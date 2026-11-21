package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.CasosRulaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 13E-A: recorrido HTTP y reglas definitivas de contexto, consentimiento y visita.
 */
class HttpRecorridoTest extends co.aplomo.soporte.HttpPrueba {
    Salon salon;
    int visita, reserva, puesto, planta, inactivo;

    @BeforeEach
    void contexto() throws Exception {
        salon = crearSalon();
        var empresas = new Empresas(tx);
        puesto = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Computador", "ESCRITORIO", "Centro", null, true)).id();
        planta = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 2, "Ensamble", "MESA", "Centro", null, true)).id();
        inactivo = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 3, "No mostrar", "OTRO", "Centro", null, true)).id();
        empresas.estadoPuesto(cuenta(admin), inactivo, false);
        var reservas = new Reservas(tx, reloj);
        reserva = reservas.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reservas.aceptar(evaluadorReserva(reserva), reserva);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(reserva), reserva);
        entrar(admin);
    }

    private Map<String, String> campos() throws Exception {
        var texto2 = new LinkedHashMap<String, String>();
        texto2.put("csrf", csrf(get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto)));
        texto2.put("visita", "" + visita);
        texto2.put("puesto", "" + puesto);
        texto2.put("recorrido", "guiado");
        texto2.put("acepta", "si");
        texto2.put("postura", "Sentado en computador");
        texto2.put("criterio", "MAS_INADECUADA");
        texto2.put("minutos", "30");
        texto2.put("motivo", "No confiar en este texto enviado");
        texto2.put("condiciones_respondidas", "NINGUNA");
        texto2.put("registro", "SIN_REGISTRO_FOTOGRAFICO");
        texto2.put("asistente_completo", "si");
        return texto2;
    }

    private HttpResponse<String> enviar(Map<String, String> parametros, byte[] foto) throws Exception {
        return enviarVarias(parametros, foto == null ? List.of() : List.of(foto));
    }

    private HttpResponse<String> enviarVarias(Map<String, String> parametros, List<byte[]> fotos) throws Exception {
        String texto3 = "AplomoRecorridoGuiado123";
        var out = new ByteArrayOutputStream();
        for (var parametros2 : parametros.entrySet())
            out.write(("--" + texto3 + "\r\nContent-Disposition: form-data; name=\"" + parametros2.getKey() +
                    "\"\r\n\r\n" + parametros2.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < fotos.size(); i++) {
            out.write(("--" + texto3 + "\r\nContent-Disposition: form-data; name=\"foto_" + i +
                    "\"; filename=\"foto.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(fotos.get(i));
            out.write("\r\n".getBytes());
        }
        out.write(("--" + texto3 + "--\r\n").getBytes());
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones"))
                        .header("Content-Type", "multipart/form-data; boundary=" + texto3)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private int cantidad() throws Exception {
        return Sql.uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1), visita);
    }

    @Test
    void visitaListaSoloActivosConEstadoPropioDeEstaVisita() throws Exception {
        int cantidad2 = evaluacion(visita, salon.id(), 1);
        analisis(foto(cantidad2), 7);
        var pagina = get("/visitas/" + visita);
        String texto2 = pagina.body();
        assertEquals(200, pagina.statusCode());
        assertTrue(texto2.contains("✓ Evaluado"));
        assertTrue(texto2.contains("7 · Rojo · Nivel 4"));
        assertTrue(texto2.contains("○ Pendiente"));
        assertTrue(texto2.contains("&amp;puesto=" + planta));
        assertFalse(texto2.contains("&amp;puesto=" + inactivo));
        assertTrue(texto2.contains("Finalizar visita"));
        Sql.actualizar(con, "UPDATE puesto_trabajo SET ultimo_puntaje=7 WHERE id_puesto=?", planta);
        String tile = get("/visitas/" + visita).body().split("&amp;puesto=" + planta + "\"")[1].split("</a>")[0];
        assertTrue(tile.contains("○ Pendiente"));
        assertFalse(tile.contains("✓ Evaluado"));
    }

    @Test
    void sinPuestosActivosBloqueaInicioEnInterfazYServidorSinCrearVisita() throws Exception {
        var otro = new Empresas(tx).salon(cuenta(admin),
                new Empresas(tx).guardar(cuenta(admin), new Salon(0, salon.sede(), "Otro salón", "Prueba", 1, 20, true))
                        .id());
        var rs = new Reservas(tx, reloj);
        reloj.avanzar(java.time.Duration.ofDays(1));
        int id = rs.guardar(evaluadorTrabajo((solicitud(otro)).empresa()), 0, solicitud(otro));
        rs.aceptar(evaluadorReserva(id), id);
        entrar(evaluadorTrabajo().id());
        var pagina = get("/reservas/" + id);
        assertTrue(pagina.body().contains("disabled aria-describedby=\"sin-puestos\""));
        assertTrue(pagina.body().contains("Este salón no tiene puestos activos"));
        assertTrue(pagina.body().contains("/salones/" + otro.id() + "/puestos/nuevo"));
        assertEquals(400, post("/reservas/" + id + "/iniciar-visita", Map.of("csrf", csrf(pagina))).statusCode());
        assertNull(rs.consultar(cuenta(admin), id).visita());
        int cantidad2 = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, otro.id(), null, null, 1, "Inactivo", "MESA", "Centro", null, true)).id();
        new Empresas(tx).estadoPuesto(cuenta(admin), cantidad2, false);
        assertThrows(IllegalArgumentException.class, () -> new Visitas(tx, reloj).iniciar(evaluadorReserva(id), id));
        assertNull(rs.consultar(cuenta(admin), id).visita());
        new Empresas(tx).estadoPuesto(cuenta(admin), cantidad2, true);
        assertFalse(get("/reservas/" + id).body().contains("id=\"sin-puestos\""));
        assertTrue(new Visitas(tx, reloj).iniciar(evaluadorReserva(id), id) > 0);
    }

    @ParameterizedTest
    @CsvSource({"1,Oficina", "2,Planta"})
    void contextoDelPuestoDeterminaModoYNoMarcaRespuestas(int numero, String modo) throws Exception {
        int id = numero == 1 ? puesto : planta;
        String texto2 = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + id).body();
        assertTrue(texto2.contains("data-modo>" + modo));
        assertTrue(texto2.contains("name=\"puesto\" value=\"" + id + "\""));
        assertTrue(texto2.contains("Sí, firmó el consentimiento"));
        assertTrue(texto2.contains("class=\"campo-respuesta\""));
        assertFalse(texto2.contains("data-cambiar-tarea"));
        assertTrue(texto2.contains("name=\"ninguna\""));
        assertTrue(texto2.contains("data-paso=\"5\""));
        assertFalse(texto2.contains(" checked"));
        assertTrue(texto2.contains("data-giro><option value=\"\""));
        assertTrue(texto2.contains("data-carga><option value=\"\""));
        assertTrue(texto2.contains("muestra_perfil.jpg"));
        assertTrue(texto2.contains("data-tarea=\"" + (numero == 1 ? "Computador" : "Ensamble") + "\""));
    }

    @Test
    void puestoAjenoOInactivoNoPuedeEntrarPorDireccionManual() throws Exception {
        var otro = new Empresas(tx).salon(cuenta(admin),
                new Empresas(tx).guardar(cuenta(admin), new Salon(0, salon.sede(), "Otro salón", "Prueba", 1, 20, true))
                        .id());
        int ajeno = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, otro.id(), null, null, 1, "Ajeno", "MESA", "Centro", null, true)).id();
        assertEquals(400, get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + ajeno).statusCode());
        assertEquals(400, get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + inactivo).statusCode());
        assertEquals(200, get("/evaluaciones/nueva?visita=" + visita).statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"30", "90", "180", "300"})
    void sinFotosGuardaMinutosMotivoAutomaticoYNotaIntegra(String minutos) throws Exception {
        var parametros = campos();
        parametros.put("minutos", minutos);
        parametros.put("observaciones", "á".repeat(255));
        parametros.put("postura", "Tarea <b>personalizada</b>");
        var respuesta = enviar(parametros, null);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        var datos = Sql.uno(con, "SELECT e.*,c.otras_observaciones FROM evaluacion e JOIN condiciones_puesto c" +
                        " USING(id_evaluacion) WHERE e.id_visita=?",
                fila -> List.of(fila.getString("motivo"), fila.getString("otras_observaciones"),
                        fila.getString("registro_fotografico"), fila.getString("minutos_jornada"),
                        fila.getString("persona_acepto")), visita);
        assertEquals("Momento: La postura más exigente. Postura: Tarea <b>personalizada</b>.", datos.get(0));
        assertEquals("á".repeat(255), datos.get(1));
        assertEquals("SIN_REGISTRO_FOTOGRAFICO", datos.get(2));
        assertEquals(minutos, datos.get(3));
        assertEquals("1", datos.get(4));
        assertEquals(0, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM fotografia f JOIN evaluacion e USING(id_evaluacion) WHE" + "RE e.id_visita=?",
                fila2 -> fila2.getInt(1), visita));
        assertEquals(0, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN e" +
                        "valuacion e USING(id_evaluacion) WHERE e.id_visita=?", fila3 -> fila3.getInt(1), visita));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CON_FOTOS", "SIN_REGISTRO_FOTOGRAFICO"})
    void sinConsentimientoFisicoNoGuardaAunqueSeSalteLaInterfaz(String registro) throws Exception {
        var parametros = campos();
        parametros.put("registro", registro);
        parametros.remove("acepta");
        var respuesta = enviar(parametros, null);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("consentimiento para ser evaluada"));
        assertEquals(0, cantidad());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tiempo", "sinCondiciones", "ningunaYCondicion", "fotoSinRegistro"})
    void rechazaContextoIncompletoOContradictorioSinPerderValidaciones(String caso) throws Exception {
        var parametros = campos();
        byte[] foto = null;
        switch (caso) {
            case "tiempo" -> parametros.put("minutos", "120");
            case "sinCondiciones" -> parametros.remove("condiciones_respondidas");
            case "ningunaYCondicion" -> parametros.put("pies", "si");
            case "fotoSinRegistro" -> foto = new byte[]{1, 2, 3};
        }
        assertEquals(400, enviar(parametros, foto).statusCode());
        assertEquals(0, cantidad());
    }

    @ParameterizedTest
    @ValueSource(strings = {"giro", "piernasApoyadas", "actividad", "carga", "hombroElevado"})
    void analisisProvisionalNoInventaRespuestasAusentes(String campo) throws Exception {
        var parametros = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());
        parametros.put("csrf", csrf(get("/evaluaciones/nueva?visita=" + visita)));
        parametros.put("visita", "" + visita);
        parametros.remove(campo);
        assertEquals(400, post("/evaluaciones/calcular", parametros).statusCode());
        assertEquals(0, cantidad());
    }

    @Test
    void puenteActualSigueCalculandoYGuardandoElPuntajeDelServidor() throws Exception {
        var parametros = campos();
        parametros.put("registro", "CON_FOTOS");
        parametros.put("foto_0_vista", "PERFIL");
        parametros.put("foto_0_descripcion", "Perfil de ensayo");
        var parametros2 = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());
        parametros2.put("foto", "0");
        parametros.put("analisis_0", CasosRulaPrueba.codificar(parametros2));
        parametros.put("puntaje_final", "1");
        var out = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(40, 80, 1), "png", out);
        var respuesta = enviar(parametros, out.toByteArray());
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertEquals(7, Sql.<Integer>uno(con,
                "SELECT a.puntaje_final FROM analisis_rula a JOIN fotografia f USING(id_foto)" +
                        " JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?", fila -> fila.getInt(1), visita));
    }

    @Test
    void usuarioNoPuedeAbrirNiEnviarRecorridoNiConsultarPuestosDeVisitaEnCurso() throws Exception {
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, get("/visitas/" + visita).statusCode());
        assertEquals(403, get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).statusCode());
        assertThrows(SecurityException.class, () -> new Visitas(tx, reloj).puestos(cuenta(usuario), visita));
        assertEquals(0, cantidad());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void oficinaYPlantaTienenAtajosDeTareaSinGaleriaNiSelecciones(int numero) throws Exception {
        String texto2 = get(
                "/evaluaciones/nueva?visita=" + visita + "&puesto=" + (numero == 1 ? puesto : planta)).body();
        var elemento = java.util.regex.Pattern.compile("data-tarea=\"([^\"]+)\"").matcher(texto2);
        List<String> claves = new ArrayList<>();
        while (elemento.find()) claves.add(elemento.group(1));
        assertEquals(List.of(numero == 1 ? "Computador" : "Ensamble"), claves);
        assertFalse(texto2.contains("data-cambiar-tarea"));
        assertTrue(texto2.contains("name=\"postura\""));
        assertFalse(texto2.contains(" checked"));
        assertFalse(texto2.contains("class=\"tour-opcion tour-postura\""));
    }

    @Test
    void otraPosturaExigeTextoYConservaSus150Caracteres() throws Exception {
        var parametros = campos();
        parametros.put("postura", "");
        assertEquals(400, enviar(parametros, null).statusCode());
        assertEquals(0, cantidad());
        parametros.put("postura", "á".repeat(151));
        assertEquals(400, enviar(parametros, null).statusCode());
        assertEquals(0, cantidad());
        String texto = "Evaluación personalizada " + "á".repeat(125);
        assertEquals(150, texto.length());
        parametros.put("postura", texto);
        var respuesta2 = enviar(parametros, null);
        assertEquals(201, respuesta2.statusCode(), respuesta2.body());
        assertEquals(texto,
                Sql.<String>uno(con, "SELECT postura FROM evaluacion WHERE id_visita=?", rs -> rs.getString(1),
                        visita));
    }

    @ParameterizedTest
    @CsvSource({"MAS_INADECUADA,Es el momento de mayor esfuerzo postural",
            "MAYOR_EXPOSICION,Es la postura que " + "mantiene durante más tiempo"})
    void momentoCambiaEtiquetaYConservaValorGuardado(String criterio, String etiqueta) throws Exception {
        String texto2 = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        assertTrue(texto2.contains("Datos del puesto"));
        assertFalse(texto2.contains("momento_campo"));
        assertFalse(texto2.contains("No sabe, evalúo la postura que más repite"));
        assertFalse(texto2.contains("¿Qué vas a fotografiar?"));
        var parametros = campos();
        parametros.put("criterio", criterio);
        parametros.put("minutos", "180");
        var respuesta2 = enviar(parametros, null);
        assertEquals(201, respuesta2.statusCode(), respuesta2.body());
        assertEquals(criterio,
                Sql.<String>uno(con, "SELECT criterio FROM evaluacion WHERE id_visita=?", rs -> rs.getString(1),
                        visita));
        assertEquals(180,
                Sql.<Integer>uno(con, "SELECT minutos_jornada FROM evaluacion WHERE id_visita=?", rs -> rs.getInt(1),
                        visita));
    }

    @Test
    void fotosUsanUnSoloRecorridoSinDescripcionNiClasificacionManuales() throws Exception {
        String texto2 = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        assertEquals(4, texto2.split("data-medida=", -1).length - 1);
        assertTrue(texto2.contains("Agregar otra foto"));
        assertTrue(texto2.contains("data-perfil"));
        assertTrue(texto2.contains("data-lado-campo=\"AMBOS\""));
        assertFalse(texto2.contains("data-foto-pantalla"));
        assertFalse(texto2.replaceAll("<dialog class=\"(?:avisos-panel|aviso-modal)[^>]*>[\\s\\S]*?</dialog>", "")
                .contains("<dialog"));
        assertTrue(texto2.contains("data-medidas-compactas hidden"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FRENTE", "OTRA"})
    void perfilEsObligatorioAunqueLleguenOtrasTomas(String vista) throws Exception {
        var parametros = campos();
        parametros.put("registro", "CON_FOTOS");
        parametros.put("foto_0_vista", vista);
        parametros.put("foto_0_descripcion", vista);
        var out = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(40, 80, 1), "png", out);
        var respuesta2 = enviar(parametros, out.toByteArray());
        assertEquals(400, respuesta2.statusCode(), respuesta2.body());
        assertTrue(respuesta2.body().contains("foto de perfil"));
        assertEquals(0, cantidad());
    }

    @Test
    void metadatosAutomaticosYFotosDeApoyoSeGuardanSinAnalisis() throws Exception {
        var parametros = campos();
        parametros.put("registro", "CON_FOTOS");
        String[] vistas = {"PERFIL", "FRENTE", "OTRA", "PERFIL", "FRENTE", "OTRA"}, descripciones = {"Perfil",
                "Frente", "Detalle de mano y muñeca", "Perfil · otro lado", "Frente · otro lado", "Foto adicional 1"};
        var out = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(40, 80, 1), "png", out);
        List<byte[]> archivos = new ArrayList<>();
        int indice = 0;
        for (int i = 0; i < vistas.length; i++) {
            parametros.put("foto_" + i + "_vista", vistas[i]);
            parametros.put("foto_" + i + "_descripcion", descripciones[i]);
            archivos.add(out.toByteArray());
            if (!vistas[i].equals("OTRA")) {
                var parametros2 = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());
                parametros2.put("foto", "" + i);
                parametros.put("analisis_" + (indice++), CasosRulaPrueba.codificar(parametros2));
            }
        }
        var respuesta2 = enviarVarias(parametros, archivos);
        assertEquals(201, respuesta2.statusCode(), respuesta2.body());
        var filas = Sql.listar(con, "SELECT f.vista,f.descripcion,(SELECT COUNT(*) FROM analisis_rula a WHERE a.i" +
                        "d_foto=f.id_foto) FROM fotografia f JOIN evaluacion e USING(id_evaluacion) W" +
                        "HERE e.id_visita=? ORDER BY f.id_foto",
                rs -> List.of(rs.getString(1), rs.getString(2), rs.getString(3)), visita);
        assertEquals(6, filas.size());
        for (int i = 0; i < 6; i++)
            assertEquals(List.of(vistas[i], descripciones[i], vistas[i].equals("OTRA") ? "0" : "1"), filas.get(i));
    }

    @Test
    void zoomTieneControlesAccesiblesSinCamposDeCalculoNuevos() throws Exception {
        String texto2 = get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + puesto).body();
        for (String texto : List.of("data-zoom-menos", "data-zoom-nivel", "data-zoom-mas", "aria-label=\"Alejar foto\"",
                "aria-label=\"Acercar foto\""))
            assertTrue(texto2.contains(texto));
        assertFalse(texto2.contains("name=\"zoom\""));
        assertTrue(texto2.contains("class=\"zoom-lamina\""));
    }
}
