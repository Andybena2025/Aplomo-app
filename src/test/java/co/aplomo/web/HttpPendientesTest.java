package co.aplomo.web;

import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.PendienteDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-022, RNF-004/007: acciones HTTP, alcance de empresa y rechazo de cambios por URL o rol incorrecto.
 */
class HttpPendientesTest extends HttpPrueba {
    Evaluaciones evals;
    int visita, eval, pendiente;

    @BeforeEach
    void contexto() throws Exception {
        evals = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao());
        var salon = crearSalon();
        int puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Ensamble", "ESCRITORIO", "Centro", null, true)).id();
        var reserva2 = new Reservas(tx, reloj);
        int reserva = reserva2.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reserva2.aceptar(evaluadorReserva(reserva), reserva);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(reserva), reserva);
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(12, 12, 1), "png", elemento);
        var evaluacion = new Evaluaciones.Datos(puesto, "Postura", "MAS_INADECUADA", "Trabajo mantenido", 60, false,
                false, false, false, false, false, "", true, false);
        eval = evals.guardar(cuenta(admin), visita, evaluacion,
                List.of(new Fotos.Entrada(elemento.toByteArray(), "PERFIL", "Perfil")),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(6).entrada())));
        pendiente = new PendienteDao().porEvaluacion(con, eval).id();
        entrar(admin);
    }

    @Test
    void editarEscapaTextoYNoAceptaCambiarPuntajeNiEstado() throws Exception {
        var editar = get("/pendientes/" + pendiente + "/editar");
        assertEquals(200, editar.statusCode());
        var texto = new HashMap<>(
                Map.of("csrf", csrf(editar), "descripcion", "<script>inocuo</script>\nAjustar mesa", "prioridad",
                        "ALTA", "fecha", "2026-11-25", "puntaje", "1", "estado", "ARREGLADO"));
        assertEquals(303, post("/pendientes/" + pendiente, texto).statusCode());
        var elemento = evals.pendientes().consultar(cuenta(admin), pendiente).pendiente();
        assertEquals(6, elemento.puntaje());
        assertEquals("ABIERTO", elemento.estado());
        var detalle = get("/pendientes/" + pendiente);
        assertTrue(detalle.body().contains("&lt;script&gt;inocuo&lt;/script&gt;"));
        assertFalse(detalle.body().contains("<script>inocuo"));
        assertTrue(detalle.body().contains("25/11/2026"));
        assertFalse(detalle.body().contains("/fotos/"));
        assertTrue(get("/pendientes?estado=TODOS&orden=limite").body().contains("data-registro="));
    }

    @Test
    void arregladoSoloPorPostConCsrfYUnaSolaAuditoria() throws Exception {
        assertEquals(405, get("/pendientes/" + pendiente + "/arreglado").statusCode());
        assertEquals(403, post("/pendientes/" + pendiente + "/arreglado", Map.of("csrf", "incorrecto")).statusCode());
        String token = csrf(get("/pendientes/" + pendiente));
        assertEquals(303, post("/pendientes/" + pendiente + "/arreglado", Map.of("csrf", token)).statusCode());
        assertEquals(303, post("/pendientes/" + pendiente + "/arreglado", Map.of("csrf", token)).statusCode());
        assertEquals(1, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='pendiente_arreglo' AND id_registr" +
                        "o=? AND accion='CAMBIAR_ESTADO'", fila -> fila.getInt(1), pendiente));
        assertFalse(get("/pendientes/" + pendiente).body().contains("Marcar como arreglado"));
    }

    @Test
    void erroresConservanCamposYExplicanQueCorregir() throws Exception {
        String token = csrf(get("/pendientes/" + pendiente + "/editar"));
        var respuesta = post("/pendientes/" + pendiente,
                Map.of("csrf", token, "descripcion", "Conservar mi descripción", "prioridad", "ALTA", "fecha",
                        "0001-01-01"));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Conservar mi descripción"));
        assertTrue(respuesta.body().contains("fecha válida"));
        assertTrue(respuesta.body().contains("value=\"ALTA\" selected"));
        assertTrue(respuesta.body().contains("0001-01-01"));
        assertFalse(respuesta.body().contains("SQLException"));
        assertEquals(400, get("/pendientes?estado=OTRO").statusCode());
        assertEquals(400, get("/pendientes?orden=xxx").statusCode());
    }

    @Test
    void ajaxGuardaYDevuelveDestinoSinRecargarElFormularioEnError() throws Exception {
        String token = csrf(get("/pendientes/" + pendiente + "/editar"));
        String cuerpo = "csrf=" + URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8) +
                "&descripcion=Conservar&prioridad=MEDIA&fecha=";
        var respuesta = cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/pendientes/" + pendiente))
                .header("Content-Type", "application/x-www-form-urlencoded").header("Accept", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respuesta.statusCode());
        assertTrue(respuesta.headers().firstValue("Location").orElseThrow().startsWith("/pendientes/" + pendiente));
        assertEquals(200, get("/static/js/pendientes.js").statusCode());
    }

    @Test
    void usuarioNoPuedeEditarNiArreglarPorDireccionDirecta() throws Exception {
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, get("/pendientes/" + pendiente).statusCode());
        new Visitas(tx, reloj).finalizar(cuenta(admin), visita, "");
        var detalle = get("/pendientes/" + pendiente);
        assertEquals(200, detalle.statusCode());
        assertFalse(detalle.body().contains("Editar seguimiento"));
        assertFalse(detalle.body().contains("Marcar como arreglado"));
        assertFalse(detalle.body().contains("/fotos/"));
        assertEquals(403, get("/pendientes/" + pendiente + "/editar").statusCode());
        String token = csrf(get("/"));
        assertEquals(403,
                post("/pendientes/" + pendiente, Map.of("csrf", token, "descripcion", "No permitido")).statusCode());
        assertEquals(403, post("/pendientes/" + pendiente + "/arreglado", Map.of("csrf", token)).statusCode());
        assertTrue(get("/pendientes").body().contains("data-registro="));
    }

    @Test
    void estadosVerdeYAmarilloSeMuestranSinCrearPendientes() throws Exception {
        for (int numero : new int[]{1, 2}) {
            var elemento = CasosRulaPrueba.todos().get(numero - 1);
            var parametros = CasosRulaPrueba.campos(elemento.entrada());
            parametros.put("csrf", csrf(get("/evaluaciones/" + eval + "/analizar")));
            parametros.put("visita", "" + visita);
            var respuesta = post("/evaluaciones/calcular", parametros);
            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.body().contains(numero == 1 ? "Aceptable" : "En observación"));
        }
        assertEquals(1, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM pendiente_arreglo WHERE id_evaluacion=?",
                fila -> fila.getInt(1), eval));
    }
}
