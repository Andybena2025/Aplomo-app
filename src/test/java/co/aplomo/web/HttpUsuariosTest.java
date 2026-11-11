package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-003: formularios HTTP completos, acceso directo y sesiones invalidadas al editar permisos. */
class HttpUsuariosTest extends co.aplomo.soporte.HttpPrueba {
    private Map<String,String> datos(String csrf,String correo) {
        return new HashMap<>(Map.of("csrf",csrf,"nombre","Contacto <script>alert(1)</script>","correo",correo,
                "telefono","123","cargo","Contacto","rol","USUARIO","empresa",Integer.toString(empresa),"temporal","Temporal2026"));
    }

    @Test void crearEditarDesactivarYReactivarDesdeHttp() throws Exception {
        entrar(admin); var nuevo=get("/usuarios/nuevo"); assertEquals(200,nuevo.statusCode());
        assertTrue(nuevo.body().contains("Empresa de prueba sin confirmar"));
        String correo=UUID.randomUUID()+"@prueba.invalid";
        var form=datos(csrf(nuevo),correo);
        assertEquals(303,post("/usuarios",form).statusCode());
        Cuenta c=dao.porCorreo(con,correo); assertNotNull(c); assertTrue(c.temporal()); assertEquals(empresa,c.empresa());
        var lista=get("/usuarios"); assertFalse(lista.body().contains(c.hash()));
        assertTrue(lista.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(lista.body().contains("Temporal2026"));
        var editar=get("/usuarios/"+c.id()+"/editar");
        assertFalse(editar.body().contains(c.hash())); assertFalse(editar.body().contains("value=\"Temporal2026\""));
        form.put("csrf",csrf(editar)); form.put("temporal",""); form.put("nombre","Contacto editado");
        assertEquals(303,post("/usuarios/"+c.id()+"/editar",form).statusCode());
        assertEquals("Contacto editado",cuenta(c.id()).nombre()); assertEquals(c.hash(),cuenta(c.id()).hash());
        assertEquals(303,post("/usuarios/"+c.id()+"/desactivar",Map.of("csrf",csrf(lista))).statusCode());
        assertEquals(Estado.INACTIVA,cuenta(c.id()).estado());
        assertEquals(303,post("/usuarios/"+c.id()+"/activar",Map.of("csrf",csrf(lista))).statusCode());
        assertEquals(Estado.ACTIVA,cuenta(c.id()).estado());
    }

    @Test void erroresConservanCamposSinVolverAMostrarLaClave() throws Exception {
        entrar(admin); var pagina=get("/usuarios/nuevo");
        Map<String,String> form=datos(csrf(pagina),cuenta(usuario).correo());
        var repetido=post("/usuarios",form); assertEquals(400,repetido.statusCode());
        assertTrue(repetido.body().contains("ya pertenece")); assertTrue(repetido.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(repetido.body().contains("value=\"Temporal2026\""));
        form.put("empresa",""); assertEquals(400,post("/usuarios",form).statusCode());
        form.put("rol","EVALUADOR"); assertEquals(400,post("/usuarios",form).statusCode());
        assertEquals(403,post("/usuarios",Map.of("nombre","Sin token")).statusCode());
    }

    @Test void noPermiteEditarPorGetNiInventarIds() throws Exception {
        entrar(admin); var inicio=get("/");
        assertEquals(404,get("/usuarios/"+usuario+"/desactivar").statusCode());
        assertEquals(Estado.ACTIVA,cuenta(usuario).estado());
        assertEquals(400,get("/usuarios/2147483647/editar").statusCode());
        assertEquals(404,post("/usuarios/0/activar",Map.of("csrf",csrf(inicio))).statusCode());
    }

    @Test void cambiarElPropioRolCierraLaSesionYElNuevoRolRestringeAcceso() throws Exception {
        entrar(admin); var edicion=get("/usuarios/"+admin+"/editar");
        var form=datos(csrf(edicion),cuenta(admin).correo()); form.put("temporal","");
        insertar("ADMINISTRADOR",null,cuenta(admin).hash(),false);
        // Otro administrador fixture garantiza el relevo sin depender de cuentas reales.
        assertEquals(303,post("/usuarios/"+admin+"/editar",form).statusCode());
        assertEquals(303,get("/usuarios").statusCode());
        assertEquals(Rol.USUARIO,cuenta(admin).rol());
        entrar(admin); assertEquals(403,get("/usuarios").statusCode());
        assertFalse(get("/").body().contains("href=\"/usuarios\""));
    }
}
