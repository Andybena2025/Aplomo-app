package co.aplomo.web;

import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.dao.MensajeDao;
import co.aplomo.db.Sql;
import co.aplomo.servicio.Agenda;
import co.aplomo.servicio.Avisos;
import co.aplomo.servicio.Reservas;
import co.aplomo.soporte.HttpPrueba;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-033: conversaciones reales HTTP/MySQL, con acuse explícito, permisos y transacción revertida.
 */
class HttpPaso30CTest extends HttpPrueba {
    int evaluador;
    int otro;
    int reserva;
    Agenda agenda;
    Avisos avisos;

    @BeforeEach
    void prepararCita() throws Exception {
        var salon = crearSalon();
        evaluador = evaluadorTrabajo().id();
        otro = insertar("EVALUADOR", null, cuenta(admin).hash(), false);
        new CuentaEmpresaDao().sincronizar(con, admin, otro, List.of(empresa), false);
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Ing. Andrés Ejemplo' WHERE id_cuenta=?", evaluador);
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        reserva = new Reservas(tx, reloj).guardar(cuenta(evaluador), 0, solicitud(salon));
        agenda = new Agenda(tx, reloj);
        avisos = new Avisos(tx, reloj);
        cambiar(admin);
    }

    @Test
    void citaIdaYVueltaConAvisoCalendarioEscapeYLecturaSinRecargar() throws Exception {
        String token = csrf(get("/"));
        var enviada = panel("/reservas/" + reserva + "/nota-administrador",
                Map.of("csrf", token, "nota", "Llevar EPP: <script>alert(1)</script>", "destinatario", "" + otro));
        assertEquals(200, enviada.statusCode());
        assertFalse(enviada.body().contains("<main"));
        assertTrue(enviada.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(enviada.body().contains("Mensaje para Andrés"));
        int nota = ultimo("NOTA_ADMIN", evaluador);
        assertEquals(0, avisos.listar(cuenta(otro)).size());
        assertFalse(avisos.listar(cuenta(evaluador)).get(0).leido());
        cambiar(evaluador);
        String calendario = get("/agenda?fecha=" + LocalDate.now(reloj)).body();
        assertTrue(calendario.contains("Notas nuevas"));
        assertTrue(calendario.contains("Nota nueva"));
        var abierta = panel("/agenda/citas/" + reserva, null);
        assertEquals(200, abierta.statusCode());
        assertTrue(abierta.body().contains("Responder al Administrador"));
        assertTrue(abierta.body().contains("Ver reserva completa"));
        assertFalse(leido(nota));
        token = csrf(abierta);
        assertEquals(200, panel("/agenda/citas/" + reserva + "/leidos", Map.of("csrf", token)).statusCode());
        assertTrue(leido(nota));
        assertTrue(agenda.nota(cuenta(evaluador), reserva).vista());
        assertEquals(200, panel("/agenda/citas/" + reserva + "/responder",
                Map.of("csrf", token, "texto", "De acuerdo", "destinatario", "" + otro)).statusCode());
        int respuesta = ultimo("RESPUESTA", admin);
        cambiar(admin);
        abierta = panel("/agenda/citas/" + reserva, null);
        assertTrue(abierta.body().contains("De acuerdo"));
        assertTrue(abierta.body().indexOf("Llevar EPP:") < abierta.body().indexOf("De acuerdo"));
        assertFalse(leido(respuesta));
        assertEquals(200, panel("/agenda/citas/" + reserva + "/leidos", Map.of("csrf", csrf(abierta))).statusCode());
        assertTrue(leido(respuesta));
        assertTrue(get("/avisos?leidos=1").body().contains("/reservas/" + reserva));
        assertTrue(Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='mensaje' " + "AND id_registro IN (?,?) AND accion='CREAR'",
                f -> f.getInt(1), nota, respuesta) >= 2);
    }

    @Test
    void evaluadorAjenoAunqueComparteEmpresaYUsuarioNoLeenNiEnvian() throws Exception {
        agenda.guardarNota(cuenta(admin), reserva, "Privado");
        for (int actor : List.of(otro, usuario)) {
            cambiar(actor);
            String token = csrf(get("/"));
            assertEquals(403, panel("/agenda/citas/" + reserva, null).statusCode());
            assertEquals(403, panel("/agenda/citas/" + reserva + "/leidos", Map.of("csrf", token)).statusCode());
            assertEquals(403, panel("/agenda/citas/" + reserva + "/responder",
                    Map.of("csrf", token, "texto", "No permitido")).statusCode());
            assertEquals(403, panel("/reservas/" + reserva + "/nota-administrador",
                    Map.of("csrf", token, "nota", "No permitido")).statusCode());
            assertEquals(403, panel("/agenda/evaluadores/" + evaluador, null).statusCode());
            assertEquals(403, panel("/agenda/evaluadores/" + evaluador,
                    Map.of("csrf", token, "texto", "No permitido")).statusCode());
            assertEquals(403,
                    panel("/agenda/evaluadores/" + evaluador + "/leidos", Map.of("csrf", token)).statusCode());
        }
        assertEquals(1, avisos.listar(cuenta(evaluador)).size());
    }

    @Test
    void avisoGeneralYRespuestasBidireccionalesNoMezclanCitasNiOtrosAdministradores() throws Exception {
        String ruta = "/agenda/evaluadores/" + evaluador;
        assertEquals(200, panel(ruta, Map.of("csrf", csrf(get("/")), "texto", "Aviso general")).statusCode());
        int mensaje = ultimo("AVISO", evaluador);
        assertEquals(null, avisos.listar(cuenta(evaluador)).get(0).reserva());
        cambiar(evaluador);
        assertTrue(get("/avisos").body().contains(">Responder</summary>"));
        assertEquals(303, post("/avisos/" + mensaje + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Respuesta general")).statusCode());
        int respuesta = ultimo("RESPUESTA", admin);
        cambiar(admin);
        assertTrue(get("/avisos").body().contains(">Responder</summary>"));
        assertEquals(303, post("/avisos/" + respuesta + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Recibido")).statusCode());
        assertEquals(2, avisos.listar(cuenta(evaluador)).stream().filter(m -> m.tipo().equals("AVISO")).count());
        String html = panel(ruta, null).body();
        assertTrue(html.contains("Aviso general"));
        assertTrue(html.contains("Respuesta general"));
        assertTrue(html.contains("Recibido"));
        assertFalse(panel("/agenda/citas/" + reserva, null).body().contains("Respuesta general"));
        int segundoAdmin = insertar("ADMINISTRADOR", null, cuenta(admin).hash(), false);
        cambiar(segundoAdmin);
        assertFalse(panel(ruta, null).body().contains("Aviso general"));
        assertEquals(403, post("/avisos/" + respuesta + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Ajeno")).statusCode());
    }

    @Test
    void responderCitaUsaUltimoAdministradorYSinNotaUsaPrincipal() throws Exception {
        cambiar(evaluador);
        int principal = avisos.principal(cuenta(evaluador)).id();
        assertEquals(200, panel("/agenda/citas/" + reserva + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Primera consulta")).statusCode());
        assertTrue(ultimo("RESPUESTA", principal) > 0);
        int segundoAdmin = insertar("ADMINISTRADOR", null, cuenta(admin).hash(), false);
        agenda.guardarNota(cuenta(admin), reserva, "Nota primera");
        agenda.guardarNota(cuenta(segundoAdmin), reserva, "Nota más reciente");
        assertEquals(200, panel("/agenda/citas/" + reserva + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Respuesta reciente")).statusCode());
        int id = ultimo("RESPUESTA", segundoAdmin);
        assertEquals("Respuesta reciente", new MensajeDao().exigir(con, id, segundoAdmin).texto());
        cambiar(admin);
        String propia = panel("/agenda/citas/" + reserva, null).body();
        assertTrue(propia.contains("Nota primera"));
        assertFalse(propia.contains("Nota más reciente"));
        assertFalse(propia.contains("Respuesta reciente"));
        assertFalse(get("/avisos").body().contains("Respuesta reciente"));
    }

    @Test
    void validaCsrfLongitudTipoDeCuentaYRevocacionEnCadaEnvio() throws Exception {
        String token = csrf(get("/"));
        assertEquals(403, panel("/agenda/evaluadores/" + evaluador, Map.of("texto", "Sin token")).statusCode());
        assertEquals(403, panel("/agenda/citas/" + reserva + "/leidos", Map.of()).statusCode());
        for (String texto : List.of("", "x".repeat(501))) {
            assertEquals(400,
                    panel("/agenda/evaluadores/" + evaluador, Map.of("csrf", token, "texto", texto)).statusCode());
        }
        assertEquals(403, panel("/agenda/evaluadores/" + usuario, Map.of("csrf", token, "texto", "No")).statusCode());
        assertEquals(403, panel("/agenda/evaluadores/" + admin, Map.of("csrf", token, "texto", "No")).statusCode());
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", evaluador);
        assertEquals(403, panel("/agenda/evaluadores/" + evaluador, Map.of("csrf", token, "texto", "No")).statusCode());
        assertEquals(400, panel("/reservas/" + reserva + "/nota-administrador",
                Map.of("csrf", token, "nota", "No")).statusCode());
        assertEquals(0, avisos.listar(cuenta(admin)).size());
    }

    @Test
    void bandejaRespondeNotasConFlujoActualYRevalidaAsignacion() throws Exception {
        agenda.guardarNota(cuenta(admin), reserva, "Nota original");
        int nota = ultimo("NOTA_ADMIN", evaluador);
        cambiar(evaluador);
        assertEquals(303, post("/avisos/" + nota + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Respondo desde bandeja")).statusCode());
        int respuesta = ultimo("RESPUESTA", admin);
        cambiar(admin);
        assertEquals(303, post("/avisos/" + respuesta + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Nueva nota")).statusCode());
        assertEquals("Nueva nota", agenda.nota(cuenta(evaluador), reserva).texto());
        Sql.actualizar(con, "UPDATE reserva SET id_evaluador=? WHERE id_reserva=?", otro, reserva);
        assertEquals(403, post("/avisos/" + respuesta + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Destinatario anterior")).statusCode());
        cambiar(evaluador);
        assertEquals(403, panel("/agenda/citas/" + reserva + "/responder",
                Map.of("csrf", csrf(get("/")), "texto", "Sin asignación")).statusCode());
    }

    @Test
    void agendaConservaEnlacesMesYSemanaYOfreceEscribirSoloAdministrador() throws Exception {
        for (String vista : List.of("mes", "semana")) {
            String html = get("/agenda?vista=" + vista + "&fecha=" + LocalDate.now(reloj)).body();
            assertTrue(html.contains("href=\"/reservas/" + reserva + "\""));
            assertTrue(html.contains("data-agenda-evaluador=\"" + evaluador + "\""));
            assertTrue(html.contains("data-agenda-panel"));
            assertTrue(html.contains("agenda-mensajes.js"));
        }
        cambiar(evaluador);
        String html = get("/agenda").body();
        assertTrue(html.contains("data-agenda-panel"));
        assertFalse(html.contains("data-agenda-evaluador"));
        cambiar(usuario);
        assertFalse(get("/reservas/" + reserva).body().contains("data-agenda-panel"));
    }

    @Test
    void sondeoDeConversacionYAcuseNoProlonganLaSesion() throws Exception {
        cambiar(evaluador);
        String token = csrf(get("/"));
        reloj.avanzar(java.time.Duration.ofMinutes(20));
        assertEquals(200, panel("/agenda/citas/" + reserva, null, true).statusCode());
        assertEquals(200, panel("/agenda/citas/" + reserva + "/leidos", Map.of("csrf", token), true).statusCode());
        reloj.avanzar(java.time.Duration.ofMinutes(11));
        var vencida = get("/agenda");
        assertEquals(303, vencida.statusCode());
        assertTrue(vencida.headers().firstValue("Location").orElse("").contains("inactividad=1"));
    }

    private boolean leido(int id) throws Exception {
        return Sql.uno(con, "SELECT leido FROM mensaje WHERE id_mensaje=?", f -> f.getBoolean(1), id);
    }

    private int ultimo(String tipo, int destinatario) throws Exception {
        return Sql.uno(con, "SELECT MAX(id_mensaje) FROM mensaje WHERE tipo=? AND id_destinatario=?", f -> f.getInt(1),
                tipo, destinatario);
    }

    private void cambiar(int id) throws Exception {
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(303, entrar(id).statusCode());
    }

    private HttpResponse<String> panel(String ruta, Map<String, String> datos) throws Exception {
        return panel(ruta, datos, false);
    }

    private HttpResponse<String> panel(String ruta, Map<String, String> datos, boolean sondeo) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create(raiz + ruta)).header("X-Alineo-Agenda", "1");
        if (sondeo) peticion.header("X-Alineo-Sondeo", "1");
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
