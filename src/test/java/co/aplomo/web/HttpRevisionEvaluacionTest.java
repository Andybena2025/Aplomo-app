package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 14: contexto, contrato guardado y seguridad en HTTP real, siempre ROLLBACK.
 */
class HttpRevisionEvaluacionTest extends HttpPrueba {
    int visita, puesto;
    String token;

    @BeforeEach
    void contexto() throws Exception {
        var salon = crearSalon();
        puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Trabajo de prueba", "MESA", "Centro", null, true)).id();
        var rs = new Reservas(tx, reloj);
        int cantidad2 = rs.guardar(cuenta(admin), 0, solicitud(salon));
        rs.aceptar(cuenta(admin), cantidad2);
        visita = new Visitas(tx, reloj).iniciar(cuenta(admin), cantidad2);
        entrar(admin);
        token = csrf(get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto));
    }

    Map<String, String> campos() {
        return new LinkedHashMap<>(
                Map.of("csrf", token, "visita", "" + visita, "puesto", "" + puesto, "recorrido", "guiado", "postura",
                        "Levantar una caja", "criterio", "MAS_INADECUADA", "minutos", "90", "condiciones_respondidas",
                        "NINGUNA", "acepta", "si", "registro", "CON_FOTOS"));
    }

    HttpResponse<String> guardar(Map<String, String> parametros, int fotos) throws Exception {
        String limite = "AplomoRevision14Prueba";
        var out = new ByteArrayOutputStream();
        for (var parametros2 : parametros.entrySet()) parte(out, limite, parametros2.getKey(), parametros2.getValue());
        var imagen = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(40, 80, 1), "png", imagen);
        for (int i = 0; i < fotos; i++) {
            out.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"foto_" + i +
                    "\"; filename=\"prueba.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(imagen.toByteArray());
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones"))
                        .header("Content-Type", "multipart/form-data; boundary=" + limite)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    static void parte(ByteArrayOutputStream elemento, String limite, String clave, String valor) throws Exception {
        elemento.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + clave + "\"\r\n\r\n" + valor +
                "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    int cantidad(String tabla) throws Exception {
        return Sql.uno(con, "SELECT COUNT(*) FROM " + tabla, fila -> fila.getInt(1));
    }

    @Test
    void contextoContieneTresBloquesYLosValoresExactosEnUnaSolaFase() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        String contexto = texto.substring(texto.indexOf("data-paso=\"2\""), texto.indexOf("data-paso=\"3\""));
        for (String texto2 : List.of("¿Cuántas horas al día trabaja en este puesto?", "data-valor=\"60\"",
                "data-valor=\"180\"", "data-valor=\"300\"", "data-valor=\"420\"", "data-valor=\"540\"",
                "Condiciones del puesto", "name=\"ninguna\""))
            assertTrue(contexto.contains(texto2), texto2);
        assertFalse(texto.contains("momento_campo"));
        assertFalse(texto.contains("duracion_campo"));
        assertEquals(4, texto.split(" data-paso=", -1).length - 1);
    }

    @Test
    void revisionTieneSieteFilasYLosAjustesSonOpcionalesSinVentanas() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        assertEquals(4, texto.split("data-medida=", -1).length - 1);
        for (String texto2 : List.of("hombroElevado", "brazoSeparado", "brazoApoyado", "antebrazoCruza",
                "munecaDesviada", "cuelloGirado", "cuelloInclinado", "troncoGirado", "troncoInclinado",
                "pies_sin_apoyo", "giro_extremo"))
            assertTrue(texto.contains("data-chip=\"" + texto2 + "\""), texto2);
        assertEquals(3, texto.split("data-muneca=", -1).length - 1);
        assertFalse(texto.contains("<dialog"));
        assertFalse(texto.contains("role=\"dialog\""));
        assertFalse(texto.contains(" checked"));
        assertTrue(texto.contains("data-falta"));
        assertTrue(texto.contains("aria-expanded=\"false\""));
        assertTrue(texto.contains("data-medidas-compactas hidden"));
    }

    @Test
    void ladoTieneCuatroVinetasSiemprePresentesYEsfuerzoSePreguntaUnaVez() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        String postura = texto.substring(texto.indexOf("data-paso=\"3\""), texto.indexOf("data-paso=\"5\""));
        assertFalse(texto.contains("name=\"lado_recorrido\""));
        assertTrue(postura.contains("data-lado-campo=\"AMBOS\""));
        for (String campo : List.of("carga_campo", "actividad_campo", "tirones_campo"))
            assertEquals(1, postura.split("name=\"" + campo + "\"", -1).length - 1);
        assertFalse(postura.contains("Lo más pesado que levanta"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"postura", "criterio", "minutos", "condiciones_respondidas"})
    void contextoObligatorioSeValidaTambienSiSeSaltaLaPantalla(String campo) throws Exception {
        var parametros = campos();
        parametros.remove(campo);
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        int antes = cantidad("evaluacion");
        assertEquals(400, guardar(parametros, 0).statusCode());
        assertEquals(antes, cantidad("evaluacion"));
    }

    @Test
    void frenteDeApoyoNoCreaOtraEvaluacionNiOtroAnalisisYConservaLosCampos() throws Exception {
        var parametros = campos();
        parametros.put("asistente_completo", "si");
        parametros.put("foto_0_vista", "PERFIL");
        parametros.put("foto_0_descripcion", "Perfil");
        parametros.put("foto_1_vista", "FRENTE");
        parametros.put("foto_1_descripcion", "Frente");
        var parametros2 = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());
        parametros2.put("foto", "0");
        parametros.put("analisis_0", CasosRulaPrueba.codificar(parametros2));
        var respuesta = guardar(parametros, 2);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertEquals(1,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1),
                        visita));
        var filas = Sql.listar(con, "SELECT f.vista,COUNT(a.id_analisis) cantidad FROM fotografia f JOIN evaluaci" +
                        "on e USING(id_evaluacion) LEFT JOIN analisis_rula a USING(id_foto) WHERE e.i" +
                        "d_visita=? GROUP BY f.id_foto,f.vista ORDER BY f.id_foto",
                fila2 -> List.of(fila2.getString(1), fila2.getString(2)), visita);
        assertEquals(List.of(List.of("PERFIL", "1"), List.of("FRENTE", "0")), filas);
        var guardado = Sql.uno(con, "SELECT e.postura,e.criterio,e.minutos_jornada,a.carga,a.puntaje_final FROM e" +
                        "valuacion e JOIN fotografia f USING(id_evaluacion) JOIN analisis_rula a USIN" +
                        "G(id_foto) WHERE e.id_visita=?",
                fila3 -> List.of(fila3.getString(1), fila3.getString(2), fila3.getString(3), fila3.getString(4),
                        fila3.getString(5)), visita);
        assertEquals(List.of("Levantar una caja", "MAS_INADECUADA", "90", parametros2.get("carga"), "7"), guardado);
        int ev = Sql.uno(con, "SELECT id_evaluacion FROM evaluacion WHERE id_visita=?", fila4 -> fila4.getInt(1),
                visita);
        String html = get("/evaluaciones/" + ev + "/analizar").body();
        assertTrue(html.contains("data-continuar=\"" + ev + "\""));
        assertTrue(html.contains("data-revision=\"si\""));
        assertTrue(html.contains("data-criterio=\"MAS_INADECUADA\""));
        assertFalse(html.contains("<dialog"));
    }

    @Test
    void esfuerzoIgualPorLadoSeConservaEnDosAnalisisSinValoresDePuntajeDelCliente() throws Exception {
        var parametros = campos();
        parametros.put("asistente_completo", "si");
        for (int i = 0; i < 2; i++) {
            parametros.put("foto_" + i + "_vista", "PERFIL");
            parametros.put("foto_" + i + "_descripcion", "Perfil " + i);
            var parametros2 = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());
            parametros2.put("foto", "" + i);
            parametros.put("analisis_" + i, CasosRulaPrueba.codificar(parametros2));
        }
        parametros.put("puntaje_final", "1");
        var respuesta = guardar(parametros, 2);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        var resultados = Sql.listar(con,
                "SELECT a.lado,a.carga,a.postura_estatica_o_repetitiva,a.puntaje_final FROM a" +
                        "nalisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_e" +
                        "valuacion) WHERE e.id_visita=? ORDER BY a.id_analisis",
                fila -> List.of(fila.getString(1), fila.getString(2), fila.getString(3), fila.getString(4)), visita);
        assertEquals(2, resultados.size());
        assertEquals(resultados.get(0), resultados.get(1));
        assertEquals("7", resultados.get(0).get(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {60, 180, 300, 420, 540})
    void campoGuardaNuevosTiemposPosturaConLadoYMotivoCorto(int minutos) throws Exception {
        var parametros = campos();
        parametros.put("recorrido", "campo");
        parametros.put("minutos", "" + minutos);
        parametros.put("postura", "Trabajo de prueba · lado derecho");
        parametros.put("motivo", "El momento que señala el trabajador.");
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        var respuesta = guardar(parametros, 0);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        var datos = Sql.uno(con, "SELECT postura,minutos_jornada,motivo,criterio FROM evaluacion WHERE id_visita=?",
                fila -> List.of(fila.getString(1), fila.getString(2), fila.getString(3), fila.getString(4)), visita);
        assertEquals(List.of("Trabajo de prueba · lado derecho", "" + minutos, "El momento que señala el trabajador.",
                "MAS_INADECUADA"), datos);
    }

    @Test
    void tiempoAntiguoNoEntraPorElNuevoRecorridoYNoSeGuardaNada() throws Exception {
        var parametros = campos();
        parametros.put("recorrido", "campo");
        parametros.put("minutos", "30");
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        assertEquals(400, guardar(parametros, 0).statusCode());
        assertEquals(0,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1),
                        visita));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void primerasCuatroPantallasNoTienenFotosNiSiluetas(int paso) throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        String seccion = texto.substring(texto.indexOf("data-paso=\"" + paso + "\""),
                texto.indexOf("data-paso=\"" + (paso + 1) + "\""));
        assertFalse(seccion.contains("<img"));
        assertFalse(seccion.contains("campo-silueta"));
        assertFalse(seccion.contains("campo-foto"));
        if (paso == 2) for (var texto2 : PaginasEvaluacion.CONDICIONES) {
            assertTrue(seccion.contains(texto2[1]));
            assertFalse(seccion.contains(texto2[2]));
        }
    }

    @Test
    void cadaSubidaTieneCatalogoDeSuVistaYSeSirveComoJpg() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        assertEquals(GaleriaDemo.FOTOS.size(), texto.split("data-usar-ejemplo=", -1).length - 1);
        assertEquals(3, texto.split("Usar foto de ejemplo", -1).length - 1);
        for (String archivo : GaleriaDemo.FOTOS.stream().map(GaleriaDemo.Foto::archivo).distinct().toList()) {
            var respuesta = get("/static/img/galeria/" + archivo);
            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").startsWith("image/jpeg"));
        }
    }

    @Test
    void pantallaExplicaOtroBrazoYPermiteAutomaticoPorPostura() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        assertTrue(texto.contains("data-contexto=\"PLANTA\""));
        assertTrue(texto.contains("data-lado-campo=\"AUTO\" aria-pressed=\"true\""));
        assertTrue(texto.contains("Falta la foto del otro brazo: sube un perfil del lado que aún no subiste, de" +
                " esta misma postura."));
        assertTrue(texto.contains("Seguir solo con este brazo"));
        assertTrue(texto.contains("/static/js/galeria-ejemplos.js"));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"DERECHO", "IZQUIERDO"})
    void direccionRealDeLaFotoConservaFlexionPositivaEnElServidor(String lado) throws Exception {
        double[][] puntos = {{.4, .9}, {.55, .5}, {.85, .65}, {.75, .25}};
        if (lado.equals("IZQUIERDO")) for (var elemento : puntos) elemento[0] = 1 - elemento[0];
        for (String segmento : List.of("BRAZO", "CUELLO", "TRONCO")) {
            int[] ids = segmento.equals("BRAZO") ? new int[]{0, 1, 2} : segmento.equals(
                    "CUELLO") ? new int[]{0, 1, 3} : new int[]{0, 1, -1};
            var codificados = new ArrayList<String>();
            for (int id : ids) codificados.add(id < 0 ? puntos[0][0] + ",0" : puntos[id][0] + "," + puntos[id][1]);
            var texto = new HashMap<>(
                    Map.of("csrf", token, "visita", "" + visita, "segmento", segmento, "lado", lado, "ancho", "200",
                            "alto", "400", segmento + "_modo", "PUNTOS", segmento + "_puntos",
                            String.join(";", codificados)));
            var respuesta = post("/evaluaciones/calcular", texto);
            assertEquals(200, respuesta.statusCode(), respuesta.body());
            var elemento2 = java.util.regex.Pattern.compile("data-grados=\"([^\"]+)\"").matcher(respuesta.body());
            assertTrue(elemento2.find());
            assertTrue(Double.parseDouble(elemento2.group(1)) > 0, segmento + " " + lado + " " + respuesta.body());
        }
    }

    @Test
    void unaPosturaConservaConfirmacionYMotivoExactoSinFotos() throws Exception {
        var parametros = campos();
        parametros.put("recorrido", "campo");
        parametros.put("minutos", "180");
        parametros.put("motivo", "También es la que más tiempo se mantiene");
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        assertEquals(201, guardar(parametros, 0).statusCode());
        assertEquals(List.of("1", "SIN_REGISTRO_FOTOGRAFICO", "También es la que más tiempo se mantiene"),
                Sql.uno(con, "SELECT persona_acepto,registro_fotografico,motivo FROM evaluacion WHERE id_v" + "isita=?",
                        fila -> List.of(fila.getString(1), fila.getString(2), fila.getString(3)), visita));
        assertEquals(1, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='evaluacion' AND id_cuenta=? AND f" +
                        "echa_hora IS NOT NULL AND detalle LIKE ?", fila2 -> fila2.getInt(1), admin,
                "%Consentimiento escrito confirmado%"));
    }

    @Test
    void dosPosturasSeGuardanSeparadasConCriteriosYPuntajeDelServidor() throws Exception {
        for (String criterio : List.of("MAS_INADECUADA", "MAYOR_EXPOSICION")) {
            var parametros = campos();
            parametros.put("recorrido", "campo");
            parametros.put("minutos", "180");
            parametros.put("criterio", criterio);
            parametros.put("postura", "Trabajo · " + criterio + " · lado izquierdo");
            parametros.put("asistente_completo", "si");
            parametros.put("foto_0_vista", "PERFIL");
            parametros.put("foto_0_descripcion", "Perfil");
            var parametros2 = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());
            parametros2.put("foto", "0");
            parametros2.put("lado", "IZQUIERDO");
            parametros.put("analisis_0", CasosRulaPrueba.codificar(parametros2));
            parametros.put("puntaje_final", "1");
            assertEquals(201, guardar(parametros, 1).statusCode());
        }
        assertEquals(
                List.of(List.of("MAS_INADECUADA", "7", "IZQUIERDO"), List.of("MAYOR_EXPOSICION", "7", "IZQUIERDO")),
                Sql.listar(con, "SELECT e.criterio,a.puntaje_final,a.lado FROM evaluacion e JOIN fotografia f" +
                                " USING(id_evaluacion) JOIN analisis_rula a USING(id_foto) WHERE e.id_visita=" +
                                "? ORDER BY e.id_evaluacion",
                        fila -> List.of(fila.getString(1), fila.getString(2), fila.getString(3)), visita));
    }

    @Test
    void siluetasSoloEnCuatroFilasYConsentimientoSinVoz() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body(), cons = texto.substring(
                texto.indexOf("data-paso=\"1\""), texto.indexOf("data-paso=\"2\""));
        assertFalse(cons.contains("Pregúntale"));
        assertFalse(cons.contains("Míralo"));
        assertTrue(cons.contains(
                "Confirmo que la persona evaluada aceptó y firmó el consentimiento informado " + "por escrito."));
        assertEquals(4, texto.split("class=\"campo-silueta\"", -1).length - 1);
        assertEquals(1, texto.split("data-postura-aviso", -1).length - 1);
        assertTrue(texto.contains("Resumen del puesto"));
        assertTrue(texto.contains("Guardar y terminar"));
        assertFalse(texto.contains("data-evaluar-otro"));
        assertTrue(texto.contains("campo-dos-columnas"));
    }

    @Test
    void ultimaPosturaMasCriticaConservaElMaximoDelPuesto() throws Exception {
        for (int caso : List.of(3, 7)) {
            var parametros = campos();
            parametros.put("asistente_completo", "si");
            parametros.put("foto_0_vista", "PERFIL");
            parametros.put("foto_0_descripcion", "Perfil");
            var parametros2 = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(caso).entrada());
            parametros2.put("foto", "0");
            parametros.put("analisis_0", CasosRulaPrueba.codificar(parametros2));
            assertEquals(201, guardar(parametros, 1).statusCode());
        }
        assertEquals(7, Sql.<Integer>uno(con, "SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",
                fila -> fila.getInt(1), puesto));
    }

    @Test
    void listasConNoAplicaYRecuadroDeFotoSinResultadoIntermedio() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        for (var par : new String[][]{{"carga_campo", "CELULAR"}, {"actividad_campo", "NINGUNA"}, {"tirones_campo",
                "no"}})
            assertTrue(texto.contains("name=\"" + par[0] + "\" value=\"" + par[1] + "\""));
        assertEquals(1, texto.split("data-resultados", -1).length - 1);
        assertFalse(texto.contains("data-postura-siguiente"));
        for (String texto2 : List.of("data-error-titulo", "data-foto-reemplazar", "data-foto-manual", "data-foto-usar",
                "Agregar otra foto (opcional)"))
            assertTrue(texto.contains(texto2));
    }

    @Test
    void motivoDeMayorDuracionConservaTextoCorto() throws Exception {
        var parametros = campos();
        parametros.put("criterio", "MAYOR_EXPOSICION");
        parametros.put("motivo", "La postura que más tiempo mantiene.");
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        assertEquals(201, guardar(parametros, 0).statusCode());
        assertEquals("La postura que más tiempo mantiene.",
                Sql.<String>uno(con, "SELECT motivo FROM evaluacion WHERE id_visita=?", fila -> fila.getString(1),
                        visita));
    }

    @Test
    void motivoDeIncomodidadEsAutomaticoYCorto() throws Exception {
        var parametros = campos();
        parametros.put("motivo", "La postura más incómoda.");
        parametros.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        assertEquals(201, guardar(parametros, 0).statusCode());
        assertEquals("La postura más incómoda.",
                Sql.<String>uno(con, "SELECT motivo FROM evaluacion WHERE id_visita=?", fila -> fila.getString(1),
                        visita));
    }
}
