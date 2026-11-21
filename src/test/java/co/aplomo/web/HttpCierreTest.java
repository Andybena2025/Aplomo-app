package co.aplomo.web;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.rula.MotorRula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cierre del recorrido: ayuda, cargas exactas, siguiente puesto real y permisos.
 */
class HttpCierreTest extends HttpPrueba {
    int visita, primero, segundo, evaluacion, foto;
    Evaluaciones servicio;

    @BeforeEach
    void contexto() throws Exception {
        var salon = crearSalon();
        var empresas = new Empresas(tx);
        primero = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Computador", "ESCRITORIO", "Centro", null, true)).id();
        segundo = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 2, "Ensamble", "MESA", "Centro", null, true)).id();
        int inactivo = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 3, "Inactivo", "MESA", "Centro", null, true)).id();
        empresas.estadoPuesto(cuenta(admin), inactivo, false);
        var rs = new Reservas(tx, reloj);
        int reserva = rs.guardar(cuenta(admin), 0, solicitud(salon));
        rs.aceptar(cuenta(admin), reserva);
        visita = new Visitas(tx, reloj).iniciar(cuenta(admin), reserva);
        servicio = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao());
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(200, 400, 1), "png", elemento);
        evaluacion = servicio.guardar(cuenta(admin), visita, datos(primero),
                List.of(new Fotos.Entrada(elemento.toByteArray(), "PERFIL", "Perfil completo")));
        foto = new EvaluacionDao().fotos(con, evaluacion).get(0).id();
        entrar(admin);
    }

    Evaluaciones.Datos datos(int cantidad2) {
        return new Evaluaciones.Datos(cantidad2, "Ensamble", "MAS_INADECUADA", "", 90, false, false, false, false,
                false, false, "", true, false);
    }

    Map<String, String> analisis(String destino) throws Exception {
        var respuesta = get("/evaluaciones/" + evaluacion + "/analizar");
        var parametros = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());
        parametros.put("foto", "" + foto);
        return new LinkedHashMap<>(Map.of("csrf", csrf(respuesta), "destino_guardado", destino, "analisis_0",
                CasosRulaPrueba.codificar(parametros)));
    }

    @Test
    void todasLasOpcionesTienenPistaVinculadaYNoHayRespuestasMarcadasEnElPrimerPuesto() throws Exception {
        String texto2 = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + segundo).body();
        var opciones = java.util.regex.Pattern.compile("<label class=\"tour-opcion[^\"]*\">(.*?)</label>",
                java.util.regex.Pattern.DOTALL).matcher(texto2);
        Set<String> ids = new HashSet<>();
        int cantidad = 0, tiempos = 0;
        while (opciones.find()) {
            String opcion = opciones.group(1);
            cantidad++;
            assertFalse(opcion.matches("(?s).*\\bchecked(?:=|[ >]).*"));
            if (opcion.contains("name=\"minutos\"")) {
                tiempos++;
                continue;
            }
            assertFalse(opcion.contains("<small class=\"tour-ayuda\""));
        }
        assertEquals(8, cantidad);
        assertEquals(0, tiempos);
        assertFalse(texto2.contains("Suma los ratos de este trabajo durante el día."));
        assertFalse(texto2.contains("Ayuda · cómo reconocer las condiciones"));
        assertTrue(texto2.contains("Consentimiento"));
        assertTrue(texto2.contains("Permiso de fotos"));
        assertTrue(texto2.contains("Sí, permite fotos"));
        assertTrue(texto2.contains("No permite fotos"));
    }

    @Test
    void lasPistasEscapanHtmlYLasFotosExistentesTambienLasIncluyen() throws Exception {
        String texto2 = PaginasEvaluacion.opcion("prueba", "valor", "Etiqueta", "", false, "",
                "<script>texto & prueba</script>");
        assertFalse(texto2.contains("<script>"));
        assertTrue(texto2.contains("&lt;script&gt;texto &amp; prueba&lt;/script&gt;"));
        texto2 = get("/evaluaciones/" + evaluacion + "/analizar").body();
        assertTrue(texto2.contains("data-foto-id=\"" + foto + "\""));
        assertTrue(texto2.contains("data-revision=\"si\""));
        assertTrue(texto2.contains("Agregar otra foto"));
        assertFalse(texto2.contains(" checked"));
    }

    @Test
    void ayudaDeLadoApareceEnLaPantallaDeLadoSinCambiarLaPregunta() throws Exception {
        String texto2 = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + segundo).body();
        assertFalse(texto2.contains("name=\"lado_recorrido\""));
        assertTrue(texto2.contains("data-lado-campo=\"DERECHO\""));
        assertTrue(texto2.contains("data-lado-campo=\"IZQUIERDO\""));
        assertTrue(texto2.contains("data-lado-campo=\"AMBOS\""));
    }

    @ParameterizedTest
    @EnumSource(MotorRula.Carga.class)
    void lasSeisCargasTienenSuMosaicoExactoYSuPuntajeActual(MotorRula.Carga carga) throws Exception {
        var mosaico = Arrays.stream(RecorridoCuerpo.CARGAS).filter(texto2 -> texto2[0].equals(carga.name())).findFirst()
                .orElseThrow();
        assertNotNull(getClass().getResource("/static/img/tour/" + mosaico[2]));
        var parametros = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());
        parametros.put("csrf", csrf(get("/evaluaciones/nueva?visita=" + visita)));
        parametros.put("visita", "" + visita);
        parametros.put("carga", carga.name());
        var respuesta = post("/evaluaciones/calcular", parametros);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.body().contains("+0 · +" + carga.puntos()));
        assertTrue(respuesta.body().contains("Ver desglose del cálculo"));
        assertFalse(respuesta.body().contains("<details open"));
        assertTrue(respuesta.body().contains("Sugerencias · aparte del puntaje"));
    }

    @Test
    void anadirAnalisisComparteCuerpoEsfuerzoYResultadoSinValoresIniciales() throws Exception {
        var respuesta = get("/evaluaciones/" + evaluacion + "/analizar");
        assertEquals(200, respuesta.statusCode());
        String texto2 = respuesta.body();
        for (String texto : List.of("data-tour=\"campo\"", "data-conversacion=\"si\"", "data-foto-id",
                "campo-dos-columnas", "data-pregunta=\"carga_campo\"", "data-pregunta=\"tirones_campo\"",
                "data-paso=\"5\"", "Guardar y terminar", "Agregar otra postura"))
            assertTrue(texto2.contains(texto), texto);
        assertFalse(texto2.contains(" checked"));
        assertFalse(texto2.contains("data-puntaje-final"));
    }

    @Test
    void siguienteEsActivoYPendienteDeEstaVisitaInclusoSinPuntaje() throws Exception {
        assertEquals(segundo, servicio.siguientePendiente(cuenta(admin), visita));
        servicio.guardar(cuenta(admin), visita,
                new Evaluaciones.Datos(segundo, "Observación", "MAS_INADECUADA", "", 90, false, false, false, false,
                        false, false, "", true, true), List.of());
        assertEquals(0, servicio.siguientePendiente(cuenta(admin), visita));
    }

    @Test
    void guardarAnalisisYSiguienteAbreElPuestoPendienteEnLaMismaVisita() throws Exception {
        var respuesta = post("/evaluaciones/" + evaluacion + "/analisis", analisis("SIGUIENTE"));
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertEquals("/evaluaciones/nueva?visita=" + visita + "&puesto=" + segundo,
                respuesta.headers().firstValue("Location").orElseThrow());
        assertEquals(1, servicio.analisis().listar(cuenta(admin), evaluacion).size());
    }

    @Test
    void guardarYVolverVuelveALaVisitaSinCrearOtraEvaluacion() throws Exception {
        var respuesta = post("/evaluaciones/" + evaluacion + "/analisis", analisis("VOLVER"));
        assertEquals(201, respuesta.statusCode());
        assertTrue(
                respuesta.headers().firstValue("Location").orElseThrow().startsWith("/visitas/" + visita + "?aviso="));
        assertEquals(1,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", rs -> rs.getInt(1), visita));
    }

    @Test
    void ultimoPuestoVuelveConTodosEvaluados() throws Exception {
        servicio.guardar(cuenta(admin), visita,
                new Evaluaciones.Datos(segundo, "Observación", "MAS_INADECUADA", "", 90, false, false, false, false,
                        false, false, "", true, true), List.of());
        var respuesta = post("/evaluaciones/" + evaluacion + "/analisis", analisis("SIGUIENTE"));
        assertEquals(201, respuesta.statusCode());
        String loc = URLDecoder.decode(respuesta.headers().firstValue("Location").orElseThrow(),
                StandardCharsets.UTF_8);
        assertEquals("/visitas/" + visita + "?aviso=Todos los puestos están evaluados", loc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIGUIENTE", "VOLVER"})
    void nuevaEvaluacionSinFotosGuardaYRespetaElDestino(String destino) throws Exception {
        var texto2 = new LinkedHashMap<String, String>(
                Map.of("csrf", csrf(get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + segundo)), "visita",
                        "" + visita, "puesto", "" + segundo, "postura", "Ensamble", "criterio", "MAS_INADECUADA",
                        "minutos", "90", "registro", "SIN_REGISTRO_FOTOGRAFICO", "acepta", "si", "asistente_completo",
                        "si", "destino_guardado", destino));
        String limite = "CierreSinFotos123";
        var elemento = new ByteArrayOutputStream();
        for (var parametros : texto2.entrySet())
            elemento.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + parametros.getKey() +
                    "\"\r\n\r\n" + parametros.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        elemento.write(("--" + limite + "--\r\n").getBytes());
        var respuesta = cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones"))
                        .header("Content-Type", "multipart/form-data; boundary=" + limite)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(elemento.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        String loc = URLDecoder.decode(respuesta.headers().firstValue("Location").orElseThrow(),
                StandardCharsets.UTF_8);
        assertTrue(loc.startsWith("/visitas/" + visita + "?aviso="));
        if (destino.equals("SIGUIENTE")) assertTrue(loc.endsWith("Todos los puestos están evaluados"));
        assertEquals(0, servicio.siguientePendiente(cuenta(admin), visita));
    }

    @Test
    void destinoManipuladoSeRechazaAntesDeGuardar() throws Exception {
        var respuesta = post("/evaluaciones/" + evaluacion + "/analisis", analisis("https://ajeno.invalid"));
        assertEquals(400, respuesta.statusCode());
        assertTrue(servicio.analisis().listar(cuenta(admin), evaluacion).isEmpty());
    }

    @Test
    void usuarioNoPuedeAbrirNiGuardarElNuevoRecorrido() throws Exception {
        var parametros = analisis("SIGUIENTE");
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, get("/evaluaciones/" + evaluacion + "/analizar").statusCode());
        assertEquals(403, post("/evaluaciones/" + evaluacion + "/analisis", parametros).statusCode());
        assertThrows(SecurityException.class, () -> servicio.siguientePendiente(cuenta(usuario), visita));
    }
}
