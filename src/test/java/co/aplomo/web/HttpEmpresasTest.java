package co.aplomo.web;

import co.aplomo.modelo.*;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.servicio.Empresas;
import co.aplomo.db.Transacciones;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-002, RF-004 a RF-010: recorridos HTTP con MySQL y datos revertidos. */
class HttpEmpresasTest extends co.aplomo.soporte.HttpPrueba {
    private Map<String,String> empresa(String csrf, String nit) {
        return Map.of("csrf", csrf, "nombre", "Compañía <script>ejemplo</script>", "nit", nit,
                "descripcion", "Descripción", "telefono", "123", "correo", "contacto@ejemplo.co", "persona_contacto", "Contacto");
    }

    private int creado(java.net.http.HttpResponse<String> respuesta) {
        assertEquals(303, respuesta.statusCode(), respuesta.body());
        var m = java.util.regex.Pattern.compile("/(?:empresas|sedes|salones)/([0-9]+)")
                .matcher(respuesta.headers().firstValue("Location").orElseThrow());
        assertTrue(m.find());
        return Integer.parseInt(m.group(1));
    }

    @Test void creaEmpresaSedeYSalonConFormulariosReales() throws Exception {
        entrar(admin);
        var form = get("/empresas/nueva");
        int nueva = creado(post("/empresas", empresa(csrf(form), UUID.randomUUID().toString().substring(0, 20))));
        var detalle = get("/empresas/" + nueva);
        assertEquals(200, detalle.statusCode());
        assertTrue(detalle.body().contains("&lt;script&gt;ejemplo&lt;/script&gt;"));
        assertFalse(detalle.body().contains("<script>ejemplo</script>"));
        var sede = post("/empresas/" + nueva + "/sedes/nuevo", Map.of("csrf", csrf(detalle), "nombre", "Principal",
                "direccion", "Calle 1", "ciudad", "Armenia", "telefono", "123", "horario", "8 a 5", "descripcion", "Sede principal"));
        int idSede = creado(sede);
        int idSalon = creado(post("/sedes/" + idSede + "/salones/nuevo", Map.of("csrf", csrf(detalle),
                "nombre", "Ensamble", "descripcion", "Salón", "piso", "1", "capacidad_puestos", "20")));
        assertTrue(get("/salones/" + idSalon).body().contains("Nuevo puesto"));
        var puesto = post("/salones/" + idSalon + "/puestos/nuevo", Map.of("csrf", csrf(detalle), "numero", "1",
                "descripcion", "Mesa", "tipo", "MESA", "ubicacion", "Ventana", "id_area", "", "id_cargo", ""));
        assertEquals(303, puesto.statusCode());
        assertTrue(get("/salones/" + idSalon).body().contains("Puesto 1"));
    }

    @Test void errorDeDuplicadoConservaLosCamposSinMostrarSql() throws Exception {
        entrar(admin);
        var form = get("/empresas/nueva");
        String nit = new co.aplomo.dao.EmpresaDao().buscar(con, empresa, 0, false).nit();
        var duplicado = post("/empresas", empresa(csrf(form), nit));
        assertEquals(400, duplicado.statusCode());
        assertTrue(duplicado.body().contains("NIT ya pertenece"));
        assertTrue(duplicado.body().contains("value=\"Compañía &lt;script&gt;ejemplo&lt;/script&gt;\""));
        assertFalse(duplicado.body().contains("SQLException"));
    }

    @Test void usuarioNoVeEmpresasAjenasNiAccedeAFormularios() throws Exception {
        Empresas servicio = new Empresas(tx);
        int ajena = servicio.guardar(cuenta(admin), new Empresa(0, 0, "Información privada", "N" + UUID.randomUUID().toString().substring(0, 10), "", "", "", "", true)).id();
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        entrar(usuario);
        var lista = get("/empresas");
        assertEquals(200, lista.statusCode());
        assertFalse(lista.body().contains("Información privada"));
        assertFalse(lista.body().contains("Nueva empresa"));
        assertEquals(403, get("/empresas/" + ajena).statusCode());
        assertEquals(403, get("/empresas/nueva").statusCode());
        var propia = get("/empresas/" + empresa);
        assertFalse(propia.body().contains("/editar"));
        assertFalse(propia.body().contains("/desactivar"));
        assertEquals(403, post("/empresas", empresa(csrf(propia), "123456789")).statusCode());
    }

    @Test void cambiosExigenPostYTokenYNoPermitenEditarIdNiEstado() throws Exception {
        entrar(admin);
        var detalle = get("/empresas/" + empresa);
        assertEquals(404, get("/empresas/" + empresa + "/desactivar").statusCode());
        assertEquals(403, post("/empresas/" + empresa + "/desactivar", Map.of()).statusCode());
        assertEquals(303, post("/empresas/" + empresa + "/desactivar", Map.of("csrf", csrf(detalle))).statusCode());
        assertFalse(new Empresas(tx).empresa(cuenta(admin), empresa).activo());
        Map<String,String> campos = new HashMap<>(empresa(csrf(detalle), "E" + UUID.randomUUID().toString().substring(0, 10)));
        campos.put("id_empresa", "2147483647"); campos.put("activo", "true"); campos.put("id_administrador", "2147483647");
        assertEquals(303, post("/empresas/" + empresa + "/editar", campos).statusCode());
        Empresa actual = new Empresas(tx).empresa(cuenta(admin), empresa);
        assertEquals(admin, actual.administrador());
        assertFalse(actual.activo());
        assertEquals(303, post("/empresas/" + empresa + "/activar", Map.of("csrf", csrf(detalle))).statusCode());
        assertTrue(new Empresas(tx).empresa(cuenta(admin), empresa).activo());
    }

    @Test void unFalloAlGuardarConservaFormularioSinDetallesPrivados() throws Exception {
        Empresas fallada = new Empresas(new Transacciones(() -> { throw new SQLException("Detalle privado SQL"); }));
        servidor.removeContext("/");
        servidor.createContext("/", new Router(auth, new Sesiones(reloj), servicioUsuarios(), fallada));
        entrar(admin);
        var form = get("/empresas/nueva");
        var fallo = post("/empresas", empresa(csrf(form), "12345678"));
        assertEquals(503, fallo.statusCode());
        assertTrue(fallo.body().contains("Tus datos siguen en el formulario"));
        assertTrue(fallo.body().contains("contacto@ejemplo.co"));
        assertFalse(fallo.body().contains("Detalle privado SQL"));
    }
}
