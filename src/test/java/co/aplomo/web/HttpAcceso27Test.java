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


    private Map<String, String> formularioEvaluador() throws Exception {
        return new HashMap<>(Map.of("csrf", csrf(get("/solicitar-evaluador")), "nombre", "Ana <script>",
                "cargo", "Ingeniera industrial", "correo", UUID.randomUUID() + "@prueba.invalid", "telefono",
                "3005556789", "ciudad", "Armenia", "empresa", "", "necesidad", "Quiero evaluar"));
    }

    private int solicitudEvaluador(Map<String, String> datos) throws Exception {
        assertEquals(200, post("/solicitar-evaluador", datos).statusCode());
        return Sql.uno(con, "SELECT id_solicitud FROM solicitud_acceso WHERE correo=?", f -> f.getInt(1),
                datos.get("correo"));
    }

    @Test
    void formularioEvaluadorEsGemeloDoradoSinNitNiSectorYLoginLoEnlaza() throws Exception {
        assertTrue(get("/login").body().contains("href=\"/solicitar-evaluador\""));
        var pagina = get("/solicitar-evaluador");
        assertEquals(200, pagina.statusCode());
        String html = pagina.body();
        assertTrue(html.contains("card stack solicitar-visita tarjeta-dorada"));
        assertTrue(html.contains("Quiero ser evaluador"));
        assertTrue(html.contains("Evaluadores · Solicitud"));
        assertTrue(html.contains("Cuéntanos de ti. La Administradora revisará"));
        assertTrue(html.contains("← Volver a iniciar sesión"));
        assertTrue(html.contains("Cargo o profesión"));
        assertTrue(html.contains("Empresa u organización (opcional)"));
        assertTrue(html.contains("¿Por qué quieres evaluar con ALINEO? (opcional)"));
        assertFalse(html.contains("name=\"nit\""));
        assertFalse(html.contains("name=\"sector\""));
        assertTrue(html.indexOf("name=\"ciudad\"") < html.indexOf("name=\"empresa\""));
        assertTrue(html.contains("maxlength=\"500\""));
        assertTrue(get("/solicitar-acceso").body().contains("name=\"nit\""));
    }

    @Test
    void evaluadorPublicoNoObtieneCuentaYElListadoMuestraRolEscapado() throws Exception {
        var datos = formularioEvaluador();
        datos.put("tipo", "EMPRESA");
        int id = solicitudEvaluador(datos);
        assertNull(dao.porCorreo(con, datos.get("correo")));
        assertEquals("EVALUADOR", Sql.uno(con,
                "SELECT tipo FROM solicitud_acceso WHERE id_solicitud=?", f -> f.getString(1), id));
        entrar(admin);
        String listado = get("/solicitudes-acceso").body();
        assertTrue(listado.contains("rol-pill rol-pill-evaluador"));
        assertTrue(listado.contains("<strong>Ana &lt;script&gt;</strong>"));
        assertTrue(listado.contains("Ingeniera industrial · Armenia"));
        assertFalse(listado.contains("<script>Ana"));
        String detalle = get("/solicitudes-acceso/" + id).body();
        assertFalse(detalle.contains("Ingeniero (opcional)"));
        assertFalse(detalle.contains("name=\"evaluador\""));
        assertFalse(detalle.contains("null"));
        assertTrue(detalle.contains("Pedir una llamada"));
        assertTrue(detalle.contains("Motivo de rechazo"));
    }

    @Test
    void aprobarEvaluadorPorHttpExigeCambioDeClaveYNoAsignaEmpresas() throws Exception {
        var datos = formularioEvaluador();
        int id = solicitudEvaluador(datos);
        entrar(admin);
        var detalle = get("/solicitudes-acceso/" + id);
        var respuesta = post("/solicitudes-acceso/" + id + "/aprobar",
                Map.of("csrf", csrf(detalle), "evaluador", "valor-forzado"));
        assertEquals(200, respuesta.statusCode());
        var clave = java.util.regex.Pattern.compile("data-clave-temporal value=\"([^\"]+)\"")
                .matcher(respuesta.body());
        assertTrue(clave.find());
        var cuenta = dao.porCorreo(con, datos.get("correo"));
        assertEquals(co.aplomo.modelo.Cuenta.Rol.EVALUADOR, cuenta.rol());
        assertTrue(new co.aplomo.dao.CuentaEmpresaDao().ids(con, cuenta.id()).isEmpty());
        assertTrue(respuesta.body().contains("Enviar por WhatsApp"));
        assertFalse(get("/solicitudes-acceso/" + id).body().contains("data-clave-temporal"));
        post("/logout", Map.of("csrf", csrf(get("/"))));
        var login = get("/login");
        var ingreso = post("/login", Map.of("csrf", csrf(login), "correo", datos.get("correo"),
                "clave", clave.group(1)));
        assertEquals(303, ingreso.statusCode());
        assertEquals("/cuenta/contrasena", ingreso.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void solicitudEvaluadorRechazaErroresCsrfYSextoEnvioSinCrearCuentas() throws Exception {
        var datos = formularioEvaluador();
        datos.put("csrf", "falso");
        assertEquals(403, post("/solicitar-evaluador", datos).statusCode());
        datos = formularioEvaluador();
        datos.put("cargo", "");
        var invalido = post("/solicitar-evaluador", datos);
        assertEquals(400, invalido.statusCode());
        assertTrue(invalido.body().contains("Ana &lt;script&gt;"));
        for (int i = 0; i < 4; i++) solicitudEvaluador(formularioEvaluador());
        assertEquals(429, post("/solicitar-evaluador", formularioEvaluador()).statusCode());
    }

    @Test
    void evaluadorYUsuarioNoPuedenLeerNiDecidirSolicitudesDeEvaluador() throws Exception {
        int id = solicitudEvaluador(formularioEvaluador());
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        for (int actor : new int[]{evaluadorTrabajo().id(), usuario}) {
            entrar(actor);
            assertEquals(403, get("/solicitudes-acceso").statusCode());
            assertEquals(403, get("/solicitudes-acceso/" + id).statusCode());
            String csrf = csrf(get("/"));
            assertEquals(403, post("/solicitudes-acceso/" + id + "/aprobar", Map.of("csrf", csrf)).statusCode());
            assertEquals(403, post("/solicitudes-acceso/" + id + "/rechazar",
                    Map.of("csrf", csrf, "motivo", "No aprobado")).statusCode());
            post("/logout", Map.of("csrf", csrf));
        }
    }

    @Test
    void rechazoEvaluadorExigeMotivoPorHttp() throws Exception {
        var datos = formularioEvaluador();
        int id = solicitudEvaluador(datos);
        entrar(admin);
        String ruta = "/solicitudes-acceso/" + id;
        String csrf = csrf(get(ruta));
        assertEquals(400, post(ruta + "/rechazar", Map.of("csrf", csrf, "motivo", "")).statusCode());
        assertEquals(303, post(ruta + "/rechazar", Map.of("csrf", csrf, "motivo", "Falta experiencia")).statusCode());
        assertNull(dao.porCorreo(con, datos.get("correo")));
    }

    @Test
    void falloDeBaseDevuelve503YConservaFormularioSinDetallesInternos() throws Exception {
        var fallida = new co.aplomo.db.Transacciones(() -> {
            throw new java.sql.SQLException("detalle interno SQL que no debe mostrarse");
        });
        servidor.removeContext("/");
        servidor.createContext("/", new Router(auth, new co.aplomo.seguridad.Sesiones(reloj), servicioUsuarios(),
                new co.aplomo.servicio.Empresas(fallida), new co.aplomo.servicio.Reservas(tx, reloj),
                new co.aplomo.servicio.Visitas(tx, reloj), new co.aplomo.servicio.Evaluaciones(tx, reloj,
                        new co.aplomo.servicio.Fotos(carpetaFotos), new co.aplomo.dao.BitacoraDao())));
        var datos = formularioEvaluador();
        var respuesta = post("/solicitar-evaluador", datos);
        assertEquals(503, respuesta.statusCode());
        assertTrue(respuesta.body().contains("No pudimos recibir la solicitud"));
        assertTrue(respuesta.body().contains("Ana &lt;script&gt;"));
        assertTrue(respuesta.body().contains("tarjeta-dorada"));
        assertFalse(respuesta.body().contains("detalle interno SQL"));
    }

    @Test
    void esquemaFaltanteIndicaLaMigracion38SinModificarTablas() throws Exception {
        assertDoesNotThrow(() -> co.aplomo.db.Esquema.verificar(con));
        var sinTipo = (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{java.sql.Connection.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("prepareStatement") && args[0] instanceof String sql &&
                            sql.contains("table_name='solicitud_acceso' AND column_name='tipo'")) {
                        return con.prepareStatement("SELECT 0");
                    }
                    try {
                        return metodo.invoke(con, args);
                    } catch (java.lang.reflect.InvocationTargetException fallo) {
                        throw fallo.getCause();
                    }
                });
        var error = assertThrows(IllegalStateException.class, () -> co.aplomo.db.Esquema.verificar(sinTipo));
        assertEquals("Falta actualizar la base. Ejecuta scripts/migrar_paso38.sh " +
                "(Mac) o migrar_paso38.bat (Windows).", error.getMessage());
    }
}
