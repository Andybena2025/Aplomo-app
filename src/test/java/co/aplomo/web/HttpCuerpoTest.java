package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.rula.MedidorAngulos.Lado;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 13E-B: parcial sin supuestos, ambos lados atómicos y archivos existentes.
 */
class HttpCuerpoTest extends HttpPrueba {
    int visita, puesto;
    String token;

    @BeforeEach
    void contexto() throws Exception {
        var salon = crearSalon();
        puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Ensamble", "MESA", "Centro", null, true)).id();
        var rs = new Reservas(tx, reloj);
        int cantidad = rs.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        rs.aceptar(evaluadorReserva(cantidad), cantidad);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(cantidad), cantidad);
        entrar(admin);
        token = csrf(get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto));
    }

    private Map<String, String> parcial() {
        var texto = new LinkedHashMap<String, String>();
        texto.put("csrf", token);
        texto.put("visita", "" + visita);
        texto.put("segmento", "BRAZO");
        texto.put("lado", "DERECHO");
        texto.put("ancho", "200");
        texto.put("alto", "400");
        texto.put("BRAZO_modo", "MANUAL");
        texto.put("BRAZO_grados", "52");
        return texto;
    }

    @Test
    void parcialNoMuestraUnFinalNiInventaAjustesYConservaElAnguloReal() throws Exception {
        var parametros = parcial();
        var respuesta2 = post("/evaluaciones/calcular", parametros);
        assertEquals(200, respuesta2.statusCode(), respuesta2.body());
        assertTrue(respuesta2.body().contains("data-grados=\"52.0\""));
        assertTrue(respuesta2.body().contains("data-rango=\"60.0\""));
        assertTrue(respuesta2.body().contains("data-puntos=\"\""));
        assertFalse(respuesta2.body().contains("data-puntaje-final"));
        parametros.put("ajustes_confirmados", "si");
        parametros.put("hombroElevado", "si");
        parametros.put("brazoSeparado", "no");
        parametros.put("brazoApoyado", "no");
        respuesta2 = post("/evaluaciones/calcular", parametros);
        assertEquals(200, respuesta2.statusCode());
        assertTrue(respuesta2.body().contains("data-puntos=\"4\""));
        assertFalse(respuesta2.body().contains("data-puntaje-final"));
        assertEquals(0,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", rs -> rs.getInt(1), visita));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BRAZO_modo", "ANTEBRAZO_modo", "MUNECA_modo", "CUELLO_modo", "TRONCO_modo", "giro",
            "piernasApoyadas", "actividad", "carga"})
    void finalSoloApareceCuandoEstanTodosLosSegmentosActividadYCarga(String campo) throws Exception {
        var parametros = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());
        parametros.put("csrf", token);
        parametros.put("visita", "" + visita);
        String valor = parametros.remove(campo);
        var respuesta2 = post("/evaluaciones/calcular", parametros);
        assertEquals(400, respuesta2.statusCode(), respuesta2.body());
        assertFalse(respuesta2.body().contains("data-puntaje-final"));
        parametros.put(campo, valor);
        respuesta2 = post("/evaluaciones/calcular", parametros);
        assertEquals(200, respuesta2.statusCode(), respuesta2.body());
        assertTrue(respuesta2.body().contains("data-puntaje-final"));
    }

    @ParameterizedTest
    @CsvSource({"GIRO,MEDIO,1", "GIRO,EXTREMO,2", "PIERNAS,si,1", "PIERNAS,no,2"})
    void observadoPuntuaSoloElComponenteConfirmado(String segmento, String respuesta, int esperado) throws Exception {
        var texto = new HashMap<>(
                Map.of("csrf", token, "visita", "" + visita, "segmento", segmento, "respuesta", respuesta));
        var respuesta2 = post("/evaluaciones/calcular", texto);
        assertEquals(200, respuesta2.statusCode());
        assertTrue(respuesta2.body().contains("data-puntos=\"" + esperado + "\""));
        assertFalse(respuesta2.body().contains("data-puntaje-final"));
    }

    @Test
    void parcialValidaPuntosYCasillasSinGuardarNiAceptarCodigoDeCliente() throws Exception {
        var parametros = parcial();
        parametros.put("BRAZO_modo", "PUNTOS");
        parametros.put("BRAZO_puntos", "0.5,0.8;0.5,0.4;0.8,0.6");
        assertEquals(200, post("/evaluaciones/calcular", parametros).statusCode());
        parametros.put("BRAZO_puntos", "1.1,0.8;0.5,0.4;0.8,0.6");
        assertEquals(400, post("/evaluaciones/calcular", parametros).statusCode());
        parametros.put("BRAZO_modo", "MANUAL");
        parametros.put("BRAZO_puntos", "");
        parametros.put("ajustes_confirmados", "si");
        assertEquals(400, post("/evaluaciones/calcular", parametros).statusCode());
        parametros.put("csrf", "incorrecto");
        assertEquals(403, post("/evaluaciones/calcular", parametros).statusCode());
    }

    @Test
    void paginaUsaRevisionInlineArchivosExactosYNingunAvisoInicial() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        assertTrue(texto.contains("data-revision=\"si\""));
        assertFalse(texto.contains("tour-opcion tour-postura"));
        assertTrue(texto.contains("muestra_perfil.jpg"));
        assertTrue(texto.contains("muestra_frente.jpg"));
        assertTrue(texto.contains("Torcida hacia un lado"));
        assertFalse(texto.contains("<dialog"));
        assertFalse(texto.contains("role=\"dialog\""));
        assertTrue(texto.contains("data-error-evaluacion hidden"));
        assertFalse(texto.contains("data-respuesta=\"lado_recorrido\" data-valor=\"AMBOS\""));
        assertFalse(texto.contains("tour-sin-respuesta"));
        assertEquals(4, texto.split("data-medida=", -1).length - 1);
        assertEquals(200, get("/static/js/revision-evaluacion.js").statusCode());
    }

    private byte[] png() throws Exception {
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(30, 60, 1), "png", elemento);
        return elemento.toByteArray();
    }

    private HttpResponse<String> enviarAmbos(int fotos, boolean duplicado) throws Exception {
        String boundary = "AplomoAmbosLados123";
        var campos = new LinkedHashMap<String, String>();
        campos.put("csrf", token);
        campos.put("visita", "" + visita);
        campos.put("puesto", "" + puesto);
        campos.put("postura", "Ensamble");
        campos.put("criterio", "MAS_INADECUADA");
        campos.put("minutos", "30");
        campos.put("acepta", "si");
        campos.put("registro", "CON_FOTOS");
        campos.put("recorrido", "guiado");
        campos.put("condiciones_respondidas", "SELECCION");
        campos.put("asistente_completo", "si");
        for (String texto : List.of("escritorio", "pantalla", "pies", "apoyapies", "apoyabrazos", "regulable"))
            campos.put(texto, "si");
        for (int i = 0; i < fotos; i++)
            for (int lado = 0; lado < 2; lado++) {
                var parametros = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(lado == 0 ? 0 : 7).entrada());
                parametros.put("foto", "" + i);
                parametros.put("lado", lado == 0 || duplicado ? "DERECHO" : "IZQUIERDO");
                campos.put("analisis_" + (i * 2 + lado), CasosRulaPrueba.codificar(parametros));
            }
        var out = new ByteArrayOutputStream();
        for (var parametros2 : campos.entrySet()) parte(out, boundary, parametros2.getKey(), parametros2.getValue());
        for (int i = 0; i < fotos; i++) {
            parte(out, boundary, "foto_" + i + "_vista", "PERFIL");
            parte(out, boundary, "foto_" + i + "_descripcion", "Perfil " + i);
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"foto_" + i +
                    "\"; filename=\"foto.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());
            out.write(png());
            out.write("\r\n".getBytes());
        }
        out.write(("--" + boundary + "--\r\n").getBytes());
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void parte(ByteArrayOutputStream out, String texto, String k, String texto2) throws Exception {
        out.write(("--" + texto + "\r\nContent-Disposition: form-data; name=\"" + k + "\"\r\n\r\n" + texto2 +
                "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void ambosGuardaDosAnalisisPorFotoEnUnaTransaccionYConservaElMayor(int fotos) throws Exception {
        var respuesta2 = enviarAmbos(fotos, false);
        assertEquals(201, respuesta2.statusCode(), respuesta2.body());
        assertEquals(fotos * 2, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN e" +
                        "valuacion e USING(id_evaluacion) WHERE e.id_visita=?", rs -> rs.getInt(1), visita));
        assertEquals(fotos * 10, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM medicion_angulo m JOIN analisis_rula a USING(id_analisi" +
                        "s) JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) W" +
                        "HERE e.id_visita=?", rs -> rs.getInt(1), visita));
        assertEquals(7,
                Sql.<Integer>uno(con, "SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?", rs -> rs.getInt(1),
                        puesto));
        assertEquals(1, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM pendiente_arreglo p JOIN evaluacion e USING(id_evaluaci" +
                        "on) WHERE e.id_visita=?", rs -> rs.getInt(1), visita));
    }

    @Test
    void ambosNoPermiteDuplicarFotoYLadoNiDejaGuardadosParciales() throws Exception {
        assertEquals(400, enviarAmbos(1, true).statusCode());
        assertEquals(0,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", rs -> rs.getInt(1), visita));
        try (var archivos = java.nio.file.Files.list(carpetaFotos)) {
            assertEquals(0, archivos.count());
        }
    }

    @Test
    void usuarioNoObtieneMedicionesParcialesNiFotosPorDireccionManual() throws Exception {
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, post("/evaluaciones/calcular", parcial()).statusCode());
        assertEquals(403, get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).statusCode());
    }
}
