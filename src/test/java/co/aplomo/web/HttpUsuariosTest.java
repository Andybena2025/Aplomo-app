package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.modelo.Cuenta.Rol;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-003: formularios HTTP completos, acceso directo y sesiones invalidadas al editar permisos.
 */
class HttpUsuariosTest extends co.aplomo.soporte.HttpPrueba {
    private Map<String, String> datos(String csrf, String correo) {
        return new HashMap<>(
                Map.of("csrf", csrf, "nombre", "Contacto <script>alert(1)</script>", "correo", correo, "telefono",
                        "123", "cargo", "Contacto", "rol", "USUARIO", "empresa", Integer.toString(empresa), "temporal",
                        "Temporal2026"));
    }

    @Test
    void crearEditarDesactivarYReactivarDesdeHttp() throws Exception {
        entrar(admin);
        var nuevo = get("/usuarios/nuevo");
        assertEquals(200, nuevo.statusCode());
        assertTrue(nuevo.body().contains("Empresa de prueba sin confirmar"));
        String correo = UUID.randomUUID() + "@prueba.invalid";
        var form = datos(csrf(nuevo), correo);
        assertEquals(303, post("/usuarios", form).statusCode());
        Cuenta cuenta2 = dao.porCorreo(con, correo);
        assertNotNull(cuenta2);
        assertTrue(cuenta2.temporal());
        assertEquals(empresa, cuenta2.empresa());
        var lista = get("/usuarios");
        assertFalse(lista.body().contains(cuenta2.hash()));
        assertTrue(lista.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(lista.body().contains("Temporal2026"));
        var editar = get("/usuarios/" + cuenta2.id() + "/editar");
        assertFalse(editar.body().contains(cuenta2.hash()));
        assertFalse(editar.body().contains("value=\"Temporal2026\""));
        form.put("csrf", csrf(editar));
        form.put("temporal", "");
        form.put("nombre", "Contacto editado");
        assertEquals(303, post("/usuarios/" + cuenta2.id() + "/editar", form).statusCode());
        assertEquals("Contacto editado", cuenta(cuenta2.id()).nombre());
        assertEquals(cuenta2.hash(), cuenta(cuenta2.id()).hash());
        assertEquals(303, post("/usuarios/" + cuenta2.id() + "/desactivar", Map.of("csrf", csrf(lista))).statusCode());
        assertEquals(Estado.INACTIVA, cuenta(cuenta2.id()).estado());
        assertEquals(303, post("/usuarios/" + cuenta2.id() + "/activar", Map.of("csrf", csrf(lista))).statusCode());
        assertEquals(Estado.ACTIVA, cuenta(cuenta2.id()).estado());
    }

    @Test
    void erroresConservanCamposSinVolverAMostrarLaClave() throws Exception {
        entrar(admin);
        var pagina = get("/usuarios/nuevo");
        Map<String, String> form = datos(csrf(pagina), cuenta(usuario).correo());
        var repetido = post("/usuarios", form);
        assertEquals(400, repetido.statusCode());
        assertTrue(repetido.body().contains("ya pertenece"));
        assertTrue(repetido.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(repetido.body().contains("value=\"Temporal2026\""));
        form.put("empresa", "");
        assertEquals(400, post("/usuarios", form).statusCode());
        form.put("rol", "EVALUADOR");
        assertEquals(400, post("/usuarios", form).statusCode());
        assertEquals(403, post("/usuarios", Map.of("nombre", "Sin token")).statusCode());
    }

    @Test
    void noPermiteEditarPorGetNiInventarIds() throws Exception {
        entrar(admin);
        var inicio = get("/");
        assertEquals(404, get("/usuarios/" + usuario + "/desactivar").statusCode());
        assertEquals(Estado.ACTIVA, cuenta(usuario).estado());
        assertEquals(400, get("/usuarios/2147483647/editar").statusCode());
        assertEquals(404, post("/usuarios/0/activar", Map.of("csrf", csrf(inicio))).statusCode());
    }

    @Test
    void cambiarElPropioRolCierraLaSesionYElNuevoRolRestringeAcceso() throws Exception {
        entrar(admin);
        var edicion = get("/usuarios/" + admin + "/editar");
        var form = datos(csrf(edicion), cuenta(admin).correo());
        form.put("temporal", "");
        insertar("ADMINISTRADOR", null, cuenta(admin).hash(), false);

        // Otro administrador fixture garantiza el relevo sin depender de cuentas reales.
        assertEquals(303, post("/usuarios/" + admin + "/editar", form).statusCode());
        assertEquals(303, get("/usuarios").statusCode());
        assertEquals(Rol.USUARIO, cuenta(admin).rol());
        entrar(admin);
        assertEquals(403, get("/usuarios").statusCode());
        assertFalse(get("/").body().contains("href=\"/usuarios\""));
    }
}
