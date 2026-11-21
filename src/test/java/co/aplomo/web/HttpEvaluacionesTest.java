package co.aplomo.web;

import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.modelo.Puesto;
import co.aplomo.db.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-014/015/016, RNF-006/007: petición HTTP real con multipart y acceso directo a fotografías.
 */
class HttpEvaluacionesTest extends co.aplomo.soporte.HttpPrueba {
    int visita, puesto;

    @BeforeEach
    void visita() throws Exception {
        var salon = crearSalon();
        puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Prueba", "ESCRITORIO", "Centro", null, true)).id();
        var reserva = new Reservas(tx, reloj);
        int id = reserva.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reserva.aceptar(evaluadorReserva(id), id);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(id), id);
    }

    private Map<String, String> campos(String token) {
        var texto = new LinkedHashMap<String, String>();
        texto.put("csrf", token);
        texto.put("visita", "" + visita);
        texto.put("puesto", "" + puesto);
        texto.put("postura", "Tarea <script>inerte</script>");
        texto.put("criterio", "MAS_INADECUADA");
        texto.put("motivo", "Se mantiene en la jornada");
        texto.put("minutos", "120");
        texto.put("observaciones", "Nota de prueba");
        texto.put("registro", "CON_FOTOS");
        texto.put("acepta", "si");
        return texto;
    }

    private byte[] png() throws Exception {
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(10, 10, 1), "png", elemento);
        return elemento.toByteArray();
    }

    private HttpResponse<String> enviar(Map<String, String> parametros, List<byte[]> fotos,
            String nombre) throws Exception {
        var elemento = new ByteArrayOutputStream();
        String boundary = "AplomoLimiteMultipart123";
        for (var parametros2 : parametros.entrySet())
            elemento.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + parametros2.getKey() +
                    "\"\r\n\r\n" + parametros2.getValue() + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (int i = 0; i < fotos.size(); i++) {
            for (var parametros3 : Map.of("foto_" + i + "_vista", "PERFIL", "foto_" + i + "_descripcion", "Perfil")
                    .entrySet())
                elemento.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + parametros3.getKey() +
                        "\"\r\n\r\n" + parametros3.getValue() + "\r\n").getBytes());
            elemento.write(
                    ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"foto_" + i + "\"; filename=\"" +
                            nombre + "\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());
            elemento.write(fotos.get(i));
            elemento.write("\r\n".getBytes());
        }
        elemento.write(("--" + boundary + "--\r\n").getBytes());
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(elemento.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void fotoSinSesionRespondeAccesoDenegadoSinRedirigir() throws Exception {
        var respuesta = get("/fotos/1");
        assertEquals(403, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Acceso denegado"));
        assertFalse(respuesta.headers().firstValue("Location").isPresent());
    }

    @Test
    void guardaConMultipartYFotoProtegidaSeAbreSoloComoAdministrador() throws Exception {
        entrar(admin);
        var pagina = get("/evaluaciones/nueva?visita=" + visita);
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("data-paso=\"3\""));
        assertTrue(pagina.body().contains("Escritorio muy alto"));
        assertFalse(pagina.body().contains("Altura de la superficie"));
        var respuesta = enviar(campos(csrf(pagina)), List.of(png()), "../../nombre_original.jpg");
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        int id = Sql.uno(con,
                "SELECT f.id_foto FROM fotografia f JOIN evaluacion e USING(id_evaluacion) WH" + "ERE e.id_visita=?",
                rs -> rs.getInt(1), visita);
        int evaluacion = Sql.uno(con, "SELECT id_evaluacion FROM evaluacion WHERE id_visita=?", rs -> rs.getInt(1),
                visita);
        var detalle = get("/evaluaciones/" + evaluacion);
        assertEquals(200, detalle.statusCode());
        assertTrue(detalle.body().contains("/fotos/" + id));
        assertTrue(detalle.body().contains("&lt;script&gt;inerte&lt;/script&gt;"));
        assertFalse(detalle.body().contains("nombre_original"));
        var imagen = cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/fotos/" + id)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, imagen.statusCode());
        assertEquals("image/png", imagen.headers().firstValue("Content-Type").orElseThrow());
        assertArrayEquals(png(), imagen.body());
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        var denegada = get("/fotos/" + id);
        assertEquals(403, denegada.statusCode());
        assertTrue(denegada.body().contains("Acceso denegado"));
        assertEquals(403, get("/evaluaciones/nueva?visita=" + visita).statusCode());
        assertEquals(403, get("/evaluaciones/" + evaluacion).statusCode());
        assertEquals(403, enviar(campos(csrf(get("/"))), List.of(png()), "foto.jpg").statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"once", "pdf", "renombrado", "seisMB", "sinConsentimiento", "sinRegistroConFoto"})
    void rechazaArchivosInvalidosDesdeHttpSinGuardarYConMensajeClaro(String caso) throws Exception {
        entrar(admin);
        var parametros = campos(csrf(get("/evaluaciones/nueva?visita=" + visita)));
        List<byte[]> fotos = List.of(png());
        String nombre = "foto.png";
        switch (caso) {
            case "once" -> fotos = Collections.nCopies(11, png());
            case "pdf" -> {
                fotos = List.of("%PDF-1.4 prueba".getBytes());
                nombre = "prueba.pdf";
            }
            case "renombrado" -> {
                fotos = List.of("%PDF-1.4 prueba".getBytes());
                nombre = "prueba.jpg";
            }
            case "seisMB" -> fotos = List.of(new byte[6 * 1024 * 1024]);
            case "sinConsentimiento" -> parametros.remove("acepta");
            case "sinRegistroConFoto" -> {
                parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
                parametros.remove("acepta");
            }
        }
        var respuesta = enviar(parametros, fotos, nombre);
        assertEquals(400, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"));
        assertFalse(respuesta.body().contains("SQLException"));
        assertEquals(0,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", rs -> rs.getInt(1), visita));
    }

    @Test
    void sinFotosConservaConsentimientoFisicoSinAceptarFotografias() throws Exception {
        entrar(admin);
        var parametros = campos(csrf(get("/evaluaciones/nueva?visita=" + visita)));
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        assertEquals(201, enviar(parametros, List.of(), "foto.jpg").statusCode());
        assertTrue(Sql.<Boolean>uno(con, "SELECT persona_acepto FROM evaluacion WHERE id_visita=?",
                fila -> fila.getBoolean(1), visita));
    }

    @Test
    void sinTokenNoGuardaNadaYLaPaginaTieneElJavaScriptDeConservacion() throws Exception {
        entrar(admin);
        var parametros = campos("incorrecto");
        assertEquals(403, enviar(parametros, List.of(png()), "foto.jpg").statusCode());
        assertEquals(0,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1),
                        visita));
        assertEquals(200, get("/static/js/evaluacion.js").statusCode());
        assertEquals(404, get("/static/../../config/db.properties").statusCode());
    }
}
