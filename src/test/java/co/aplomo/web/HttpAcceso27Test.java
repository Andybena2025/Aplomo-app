package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.servicio.SolicitudesAcceso;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-001/002/003: formulario público y aprobación por HTTP con CSRF, sin guardar claves en evidencias.
 */
class HttpAcceso27Test extends HttpPrueba {
    private Map<String, String> formulario() throws Exception {
        return new HashMap<>(
                Map.of("csrf", csrf(get("/solicitar-acceso")), "nombre", "Contacto <script>", "cargo", "Coordinación",
                        "correo", UUID.randomUUID() + "@prueba.invalid", "telefono", "3005556789", "empresa",
                        "Empresa de acceso", "nit", UUID.randomUUID().toString().substring(0, 20), "ciudad", "Armenia",
                        "sector", "Educación"));
    }

    @Test
    void publicoValidoRecibeConfirmacionSinCrearUsuario() throws Exception {
        assertTrue(get("/login").body().contains("/solicitar-acceso"));
        var datos = formulario();
        var respuesta = post("/solicitar-acceso", datos);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Recibimos tu solicitud"));
        assertNull(dao.porCorreo(con, datos.get("correo")));
        entrar(admin);
        var bandeja = get("/avisos");
        assertTrue(bandeja.body().contains("Revisar solicitud"));
        assertTrue(bandeja.body().contains("Contacto &lt;script&gt;"));
    }

    @Test
    void invalidoConservaDatosYCsrfEsObligatorio() throws Exception {
        var datos = formulario();
        datos.put("correo", "no-es-correo");
        var respuesta = post("/solicitar-acceso", datos);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Contacto &lt;script&gt;"));
        assertTrue(respuesta.body().contains("correo válido"));
        datos.remove("csrf");
        assertEquals(403, post("/solicitar-acceso", datos).statusCode());
    }

    @Test
    void sextoEnvioDevuelve429SinCrearOtraSolicitud() throws Exception {
        for (int i = 0; i < 5; i++) assertEquals(200, post("/solicitar-acceso", formulario()).statusCode());
        assertEquals(429, post("/solicitar-acceso", formulario()).statusCode());
    }

    @Test
    void soloAdministradorPuedeRevisarYDecidirTambienPorDireccionDirecta() throws Exception {
        var datos = formulario();
        post("/solicitar-acceso", datos);
        int id = Sql.uno(con, "SELECT id_solicitud FROM solicitud_acceso WHERE correo=?", f -> f.getInt(1),
                datos.get("correo"));
        entrar(evaluadorTrabajo().id());
        assertEquals(403, get("/solicitudes-acceso/" + id).statusCode());
        assertEquals(403, post("/solicitudes-acceso/" + id + "/aprobar", Map.of("csrf", csrf(get("/")))).statusCode());
        post("/logout", Map.of("csrf", csrf(get("/"))));
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        entrar(usuario);
        assertEquals(403, get("/solicitudes-acceso").statusCode());
        assertEquals(403, post("/solicitudes-acceso/" + id + "/rechazar",
                Map.of("csrf", csrf(get("/")), "motivo", "Motivo")).statusCode());
    }

    @Test
    void aprobarMuestraClaveSoloEnEsaRespuestaYPrimerIngresoExigeCambio() throws Exception {
        var datos = formulario();
        post("/solicitar-acceso", datos);
        int id = Sql.uno(con, "SELECT id_solicitud FROM solicitud_acceso WHERE correo=?", f -> f.getInt(1),
                datos.get("correo"));
        entrar(admin);
        var detalle = get("/solicitudes-acceso/" + id);
        var respuesta = post("/solicitudes-acceso/" + id + "/aprobar", Map.of("csrf", csrf(detalle)));
        assertEquals(200, respuesta.statusCode());
        var clave = java.util.regex.Pattern.compile("data-clave-temporal value=\"([^\"]+)\"").matcher(respuesta.body());
        assertTrue(clave.find());
        String temporal = clave.group(1);
        assertTrue(respuesta.body().contains("Enviar por WhatsApp"));
        assertFalse(get("/solicitudes-acceso/" + id).body().contains("data-clave-temporal"));
        assertEquals(400, post("/solicitudes-acceso/" + id + "/aprobar", Map.of("csrf", csrf(detalle))).statusCode());
        post("/logout", Map.of("csrf", csrf(get("/"))));
        var login = get("/login");
        var ingreso = post("/login", Map.of("csrf", csrf(login), "correo", datos.get("correo"), "clave", temporal));
        assertEquals(303, ingreso.statusCode());
        assertEquals("/cuenta/contrasena", ingreso.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void empresaNuevaDelEvaluadorEnviaContactoPendienteSinDarAcceso() throws Exception {
        entrar(evaluadorTrabajo().id());
        var pagina = get("/empresas/nueva");
        assertTrue(pagina.body().contains("name=\"cargo_contacto\""));
        var datos = new HashMap<String, String>();
        datos.put("csrf", csrf(pagina));
        datos.put("nombre", "Empresa del evaluador");
        datos.put("nit", UUID.randomUUID().toString().substring(0, 20));
        datos.put("persona_contacto", "Contacto de empresa");
        datos.put("correo", UUID.randomUUID() + "@prueba.invalid");
        datos.put("telefono", "3005556789");
        datos.put("sector", "Carpintería");
        datos.put("cargo_contacto", "Coordinación");
        datos.put("ciudad_contacto", "Armenia");
        assertEquals(303, post("/empresas", datos).statusCode());
        assertNull(dao.porCorreo(con, datos.get("correo")));
        assertEquals("PENDIENTE",
                Sql.uno(con, "SELECT estado FROM solicitud_acceso WHERE correo=?", f -> f.getString(1),
                        datos.get("correo")));
    }

}
