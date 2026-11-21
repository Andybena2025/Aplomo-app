package co.aplomo.web;

import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-017–021: los diez resultados llegan en HTML y el servidor ignora puntajes adulterados.
 */
class HttpAnalisisTest extends HttpPrueba {
    int visita, puesto, eval, foto;
    Evaluaciones evaluaciones;

    @BeforeEach
    void contexto() throws Exception {
        evaluaciones = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao());
        var salon = crearSalon();
        puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Ensamble", "ESCRITORIO", "Centro", null, true)).id();
        var reservas = new Reservas(tx, reloj);
        int cantidad = reservas.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reservas.aceptar(evaluadorReserva(cantidad), cantidad);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(cantidad), cantidad);
        var evaluacion = new Evaluaciones.Datos(puesto, "Ensamble", "MAS_INADECUADA", "Trabajo mantenido", 120, false,
                false, false, false, false, false, "", true, false);
        eval = evaluaciones.guardar(cuenta(admin), visita, evaluacion,
                List.of(new Fotos.Entrada(png(), "PERFIL", "Foto completa")));
        foto = new EvaluacionDao().fotos(con, eval).get(0).id();
        entrar(admin);
    }

    @Test
    void agregarAnalisisConservaOficinaYLaAyudaDePiesSinPreseleccion() throws Exception {
        String texto = get("/evaluaciones/" + eval + "/analizar").body();
        assertTrue(texto.contains("data-contexto=\"OFICINA\""));
        assertTrue(texto.contains("data-segun-foto-pies"));
        assertTrue(texto.contains("Según la foto"));
        assertTrue(texto.contains("Parece que está sobre una sola pierna. Revisa si los pies están bien apoyados."));
        assertTrue(texto.contains("aria-describedby=\"ayuda-apoyo-pies\""));
        assertFalse(
                java.util.regex.Pattern.compile("<input[^>]*data-chip=\"pies_sin_apoyo\"[^>]*checked").matcher(texto)
                        .find());
    }

    private byte[] png() throws Exception {
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(200, 400, 1), "png", elemento);
        return elemento.toByteArray();
    }

    private Map<String, String> formulario(int caso) throws Exception {
        var parametros = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(caso - 1).entrada());
        parametros.put("csrf", csrf(get("/evaluaciones/" + eval + "/analizar")));
        parametros.put("visita", "" + visita);
        return parametros;
    }

    @TestFactory
    java.util.stream.Stream<DynamicTest> diezCasosEnLaPantallaDeResultado() {
        return CasosRulaPrueba.todos().stream()
                .map(elemento -> DynamicTest.dynamicTest("Pantalla caso " + elemento.numero(), () -> {
                    var respuesta = post("/evaluaciones/calcular", formulario(elemento.numero()));
                    assertEquals(200, respuesta.statusCode(), respuesta.body());
                    assertTrue(respuesta.body().contains("data-puntaje-final=\"" + elemento.esperado() + "\""));
                    assertTrue(respuesta.body().contains("Nivel "));
                    assertTrue(respuesta.body().contains("Tabla A · Tabla B"));
                }));
    }

    @Test
    void puntajeAdulteradoSeIgnoraYElHistorialQuedaProtegido() throws Exception {
        var parametros = formulario(8);
        parametros.put("foto", "" + foto);
        parametros.put("puntaje_final", "1");
        parametros.put("id_nivel", "1");
        var guardar = new LinkedHashMap<String, String>();
        guardar.put("csrf", parametros.get("csrf"));
        guardar.put("analisis_0", CasosRulaPrueba.codificar(parametros));
        guardar.put("puntaje_final", "1");
        var respuesta = post("/evaluaciones/" + eval + "/analisis", guardar);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        int id = Sql.uno(con, "SELECT id_analisis FROM analisis_rula WHERE id_foto=?", rs -> rs.getInt(1), foto);
        assertEquals(7,
                Sql.<Integer>uno(con, "SELECT puntaje_final FROM analisis_rula WHERE id_analisis=?", rs -> rs.getInt(1),
                        id));
        var detalle = get("/evaluaciones/" + eval + "/analisis/" + id);
        assertEquals(200, detalle.statusCode());
        assertTrue(detalle.body().contains("data-puntaje-final=\"7\""));
        assertTrue(detalle.body().contains("Registró"));
        var inicio = get("/");
        assertTrue(inicio.body().contains("Pendientes abiertos"));
        assertFalse(inicio.body().contains("color:var(--rojo)"));
        assertFalse(inicio.body().contains("Total visitas"));
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, get("/evaluaciones/" + eval + "/analizar").statusCode());
        assertEquals(403, get("/evaluaciones/" + eval + "/analisis/" + id).statusCode());
        assertEquals(403, post("/evaluaciones/calcular", parametros).statusCode());
        assertEquals(403, post("/evaluaciones/" + eval + "/analisis", guardar).statusCode());
    }

    @Test
    void visitaFinalizadaConservaElResultadoYOcultaElBotonDeAnalizar() throws Exception {
        var entrada = CasosRulaPrueba.todos().get(3).entrada();
        evaluaciones.analisis().guardar(cuenta(admin), eval, List.of(new Analisis.Solicitud(foto, entrada)));
        int id = evaluaciones.analisis().listar(cuenta(admin), eval).get(0).id();
        new Visitas(tx, reloj).finalizar(cuenta(admin), visita, "");
        var detalle = get("/evaluaciones/" + eval + "?aviso=An%C3%A1lisis+guardado.");
        assertEquals(200, detalle.statusCode());
        assertFalse(detalle.body().contains("Analizar fotos / añadir análisis"));
        assertTrue(detalle.body().contains("data-toast"));
        assertEquals(400, get("/evaluaciones/" + eval + "/analizar").statusCode());
        assertEquals(200, get("/evaluaciones/" + eval + "/analisis/" + id).statusCode());
    }

    @Test
    void csrfYEntradasInvalidasNoGuardan() throws Exception {
        var parametros = formulario(4);
        parametros.put("foto", "" + foto);
        assertEquals(403, post("/evaluaciones/" + eval + "/analisis",
                Map.of("csrf", "incorrecto", "analisis_0", CasosRulaPrueba.codificar(parametros))).statusCode());
        parametros.put("BRAZO_grados", "NaN");
        assertEquals(400, post("/evaluaciones/calcular", parametros).statusCode());
        parametros.put("BRAZO_grados", "181");
        assertEquals(400, post("/evaluaciones/calcular", parametros).statusCode());
        assertEquals(0,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM analisis_rula WHERE id_foto=?", rs -> rs.getInt(1), foto));
    }

    @Test
    void asistenteCompletoGuardaEvaluacionFotoYAnalisisEnElMismoPost() throws Exception {
        var elemento = new ByteArrayOutputStream();
        String boundary = "AplomoAsistenteCompleto123";
        var texto = new LinkedHashMap<String, String>();
        texto.put("csrf", csrf(get("/evaluaciones/nueva?visita=" + visita)));
        texto.put("visita", "" + visita);
        texto.put("puesto", "" + puesto);
        texto.put("postura", "Postura final");
        texto.put("criterio", "MAS_INADECUADA");
        texto.put("motivo", "Exposición diaria");
        texto.put("minutos", "60");
        texto.put("registro", "CON_FOTOS");
        texto.put("acepta", "si");
        texto.put("asistente_completo", "si");
        texto.put("foto_0_vista", "PERFIL");
        texto.put("foto_0_descripcion", "Vista completa");
        var entrada = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());
        entrada.put("foto", "0");
        entrada.put("puntaje_final", "1");
        texto.put("analisis_0", CasosRulaPrueba.codificar(entrada));
        for (var parametros : texto.entrySet())
            elemento.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + parametros.getKey() +
                    "\"\r\n\r\n" + parametros.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        elemento.write(
                ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"foto_0\"; filename=\"foto.png\"\r" +
                        "\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        elemento.write(png());
        elemento.write(("\r\n--" + boundary + "--\r\n").getBytes());
        var respuesta = cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(elemento.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertEquals(7, Sql.<Integer>uno(con,
                "SELECT a.puntaje_final FROM evaluacion e JOIN fotografia f USING(id_evaluaci" +
                        "on) JOIN analisis_rula a USING(id_foto) WHERE e.id_visita=? ORDER BY e.id_ev" +
                        "aluacion DESC LIMIT 1", rs -> rs.getInt(1), visita));
    }
}
