package co.aplomo.web;

import co.aplomo.dao.MensajeDao;
import co.aplomo.db.Sql;
import co.aplomo.servicio.Avisos;
import co.aplomo.soporte.HttpPrueba;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-033/002: nuevo mensaje por rol, destinatario activo, bandeja/panel y respuesta, solo en pruebas.
 */
class HttpPaso31CTest extends HttpPrueba {
    int evaluador;
    int otroAdmin;
    int inactivo;
    String contactoAnterior;

    @BeforeEach
    void prepararContactos() throws Exception {
        evaluador = evaluadorTrabajo().id();
        otroAdmin = insertar("ADMINISTRADOR", null, cuenta(admin).hash(), false);
        inactivo = insertar("ADMINISTRADOR", null, cuenta(admin).hash(), false);
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Laura Gabriela Romero' WHERE id_cuenta=?", admin);
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Otro administrador QA' WHERE id_cuenta=?", otroAdmin);
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", inactivo);
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        contactoAnterior = System.getProperty("aplomo.contacto.archivo");
        var archivo = carpetaFotos.resolve("contacto.properties");
        Files.writeString(archivo, "contacto.nombre=Laura Gabriela Romero\ncontacto.correo=" + cuenta(admin).correo());
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
    }

    @AfterEach
    void restaurarContacto() {
        if (contactoAnterior == null) System.clearProperty("aplomo.contacto.archivo");
        else System.setProperty("aplomo.contacto.archivo", contactoAnterior);
    }

    @Test
    void losTresRolesEncuentranNuevoMensajeEnLaPaginaYEnElPanel() throws Exception {
        for (int actor : List.of(admin, evaluador, usuario)) {
            cambiar(actor);
            assertTrue(get("/avisos").body().contains("href=\"/avisos/nuevo\">Nuevo mensaje</a>"));
            String panel = peticion("/avisos", null).body();
            assertTrue(panel.contains("href=\"/avisos/nuevo\">Nuevo mensaje</a>"));
            assertFalse(panel.contains("<main"));
        }
    }

    @Test
    void evaluadorEligeOtroAdministradorYAmbosPuedenResponderSinCita() throws Exception {
        cambiar(evaluador);
        var pagina = get("/avisos/nuevo");
        String html = pagina.body();
        assertTrue(html.contains("value=\"" + admin + "\" selected>Laura Gabriela Romero (Administradora principal)"));
        assertTrue(html.indexOf("value=\"" + admin + "\"") < html.indexOf("value=\"" + otroAdmin + "\""));
        assertFalse(html.contains("value=\"" + inactivo + "\""));
        assertFalse(html.contains("value=\"" + evaluador + "\""));
        String texto = "Consulta <script>alert(1)</script>";
        var respuesta = post("/avisos/nuevo", Map.of("csrf", csrf(pagina), "para", "" + otroAdmin, "texto", texto));
        assertEquals(303, respuesta.statusCode());
        String lugar = respuesta.headers().firstValue("Location").orElseThrow();
        assertTrue(get(lugar).body().contains("Mensaje enviado a Otro administrador QA"));
        int id = ultimo(evaluador);
        var mensaje = new MensajeDao().exigir(con, id, otroAdmin);
        assertEquals("AVISO", mensaje.tipo());
        assertEquals(null, mensaje.reserva());
        assertEquals(1, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='mensaje' " + "AND id_registro=? AND accion='CREAR'",
                f -> f.getInt(1), id));
        cambiar(otroAdmin);
        html = get("/avisos").body();
        assertTrue(html.contains("Consulta &lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("Consulta <script>"));
        assertTrue(html.contains(">Responder</summary>"));
        assertEquals(303, post("/avisos/" + id + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Respuesta al Evaluador")).statusCode());
        assertTrue(new Avisos(tx, reloj).conversacionEvaluador(cuenta(otroAdmin), evaluador).stream()
                .anyMatch(m -> m.id() == id));
        cambiar(evaluador);
        assertTrue(get("/avisos").body().contains("Respuesta al Evaluador"));
    }

    @Test
    void evaluadorNoPuedeElegirUsuarioEvaluadorInactivoNiIdInventado() throws Exception {
        cambiar(evaluador);
        String token = csrf(get("/avisos/nuevo"));
        for (String id : List.of("" + usuario, "" + evaluador, "" + inactivo, "2147483647", "no-es-un-id", "")) {
            assertEquals(403,
                    post("/avisos/nuevo", Map.of("csrf", token, "para", id, "texto", "No permitido")).statusCode(), id);
        }
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", otroAdmin);
        assertEquals(403, post("/avisos/nuevo",
                Map.of("csrf", token, "para", "" + otroAdmin, "texto", "Ya no activo")).statusCode());
        assertEquals(0, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM mensaje WHERE id_remitente=?", f -> f.getInt(1),
                evaluador));
    }

    @Test
    void usuarioIgnoraCualquierParaYConservaLaPrincipalSinDesplegable() throws Exception {
        cambiar(usuario);
        String html = get("/avisos/nuevo").body();
        assertTrue(html.contains("Para: Laura Gabriela Romero · Administradora principal"));
        assertFalse(html.contains("<select"));
        String token = csrf(get("/avisos/nuevo"));
        for (String para : List.of("" + otroAdmin, "" + evaluador, "" + usuario, "texto malicioso")) {
            assertEquals(303, post("/avisos/nuevo",
                    Map.of("csrf", token, "para", para, "texto", "Consulta de Usuario")).statusCode());
            assertEquals(admin, new MensajeDao().exigir(con, ultimo(usuario), admin).destinatario());
        }
    }

    @Test
    void administradorSoloVeYEnviaAEvaluadoresActivos() throws Exception {
        cambiar(admin);
        String html = get("/avisos/nuevo").body();
        assertTrue(html.contains("value=\"" + evaluador + "\""));
        assertFalse(html.contains("value=\"" + otroAdmin + "\""));
        assertFalse(html.contains("value=\"" + usuario + "\""));
        String token = csrf(get("/avisos/nuevo"));
        for (int id : List.of(admin, usuario, otroAdmin)) {
            assertEquals(403, post("/avisos/nuevo",
                    Map.of("csrf", token, "para", "" + id, "texto", "No permitido")).statusCode());
        }
        assertEquals(303, post("/avisos/nuevo",
                Map.of("csrf", token, "para", "" + evaluador, "texto", "Aviso al Evaluador")).statusCode());
        assertEquals("AVISO", new MensajeDao().exigir(con, ultimo(admin), evaluador).tipo());
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", evaluador);
        assertEquals(403, post("/avisos/nuevo",
                Map.of("csrf", token, "para", "" + evaluador, "texto", "No activo")).statusCode());
    }

    @Test
    void panelConservaTextoYParaEnErrorYVuelveALaBandejaConConfirmacion() throws Exception {
        cambiar(evaluador);
        String token = csrf(get("/avisos/nuevo"));
        assertEquals(403, peticion("/avisos/nuevo", Map.of("para", "" + otroAdmin, "texto", "Sin CSRF")).statusCode());
        String texto = "<script>" + "x".repeat(501);
        var invalida = peticion("/avisos/nuevo", Map.of("csrf", token, "para", "" + otroAdmin, "texto", texto));
        assertEquals(400, invalida.statusCode());
        assertTrue(invalida.body().contains("value=\"" + otroAdmin + "\" selected"));
        assertTrue(invalida.body().contains("&lt;script&gt;"));
        var valida = peticion("/avisos/nuevo",
                Map.of("csrf", token, "para", "" + otroAdmin, "texto", "Mensaje válido"));
        assertEquals(200, valida.statusCode());
        assertTrue(valida.body().contains("data-bandeja-avisos"));
        assertTrue(valida.body().contains("Mensaje enviado a Otro administrador QA"));
        assertFalse(valida.body().contains("<main"));
    }

    private int ultimo(int remitente) throws Exception {
        return Sql.uno(con, "SELECT MAX(id_mensaje) FROM mensaje WHERE id_remitente=?", f -> f.getInt(1), remitente);
    }

    private void cambiar(int id) throws Exception {
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(303, entrar(id).statusCode());
    }

    private HttpResponse<String> peticion(String ruta, Map<String, String> datos) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create(raiz + ruta)).header("X-Alineo-Panel", "1");
        if (datos == null) peticion.GET();
        else {
            String texto = datos.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
            peticion.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(texto));
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }
}
