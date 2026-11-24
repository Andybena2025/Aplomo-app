package co.aplomo.web;

import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.dao.EvaluacionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.regex.Pattern;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-024, RF-002, RNF-004/006: listas y direcciones directas de dos empresas y un Rojo En curso.
 */
class HttpConsultasTest extends HttpPrueba {
    EscenarioConsultas escenario;

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioConsultas(this, carpetaFotos);
        entrar(usuario);
    }

    private int idAjeno(SeccionConsulta elemento) {
        return switch (elemento) {
            case EMPRESAS -> escenario.empresaB;
            case SEDES -> escenario.sedeB;
            case SALONES -> escenario.salonB;
            case AREAS -> escenario.areaB;
            case CARGOS -> escenario.cargoB;
            case PUESTOS -> escenario.puestoB;
            case RESERVAS -> escenario.reservaB;
            case VISITAS -> escenario.visitaB;
            case EVALUACIONES -> escenario.evalB;
            case PENDIENTES -> escenario.pendienteB;
            case SUGERENCIAS -> escenario.sugerenciaB;
        };
    }

    @ParameterizedTest
    @EnumSource(SeccionConsulta.class)
    void listasDelUsuarioNoExponenEmpresaAjenaNiAcciones(SeccionConsulta elemento) throws Exception {
        var respuesta = get(elemento.ruta());
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertFalse(respuesta.body().contains("Aplicar filtros"));
        assertTrue(respuesta.body().contains("data-lista-buscar"));
        if (elemento == SeccionConsulta.PENDIENTES)
            assertTrue(respuesta.body().contains("<span class=\"eyebrow\">Gestión · Pendientes</span>"));
        assertFalse(respuesta.body().contains("Empresa ajena QA"));
        assertFalse(respuesta.body().contains("Área ajena QA"));
        assertFalse(respuesta.body().contains("Cargo ajeno QA"));
        for (String texto : List.of("Nueva empresa", "Nueva reserva", "Escribir sugerencia", "Editar sugerencia",
                "Quitar sugerencia", "Marcar como arreglado"))
            assertFalse(respuesta.body().contains(texto));
        assertFalse(respuesta.body().contains("/fotos/"));
        assertEquals(403, get(elemento.ruta() + "?empresa=" + escenario.empresaB).statusCode());
    }

    @ParameterizedTest
    @EnumSource(SeccionConsulta.class)
    void direccionesDirectasDeOtraEmpresaSeDeniegan(SeccionConsulta elemento) throws Exception {
        var respuesta = get(elemento.ruta() + "/" + idAjeno(elemento));
        assertEquals(403, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Acceso denegado") || respuesta.body().contains("no está disponible"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"visita", "evaluacion", "pendiente", "sugerencia", "foto"})
    void rojoEnCursoSeDeniegaPorDireccionDirecta(String tipo) throws Exception {
        String ruta = switch (tipo) {
            case "visita" -> "/visitas/" + escenario.visitaAbierta;
            case "evaluacion" -> "/evaluaciones/" + escenario.evalAbierta;
            case "pendiente" -> "/pendientes/" + escenario.pendienteAbierto;
            case "sugerencia" -> "/sugerencias/" + escenario.sugerenciaAbierta;
            default -> "/fotos/" + new EvaluacionDao().fotos(con, escenario.evalAbierta).get(0).id();
        };
        assertEquals(403, get(ruta).statusCode());
    }

    @Test
    void evaluacionFinalizadaAbreSinFotosYLosPuntajesAbiertosNoSeFiltran() throws Exception {
        var respuesta = get("/evaluaciones/" + escenario.evalVerde);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("2 · Verde · Nivel 1"));
        assertFalse(respuesta.body().contains("/fotos/"));
        assertFalse(respuesta.body().contains("Evaluar puesto"));
        assertFalse(respuesta.body().contains("Guardar análisis"));
        var puestos = get("/puestos");
        String uno = fila(puestos.body(), escenario.puesto1), dos = fila(puestos.body(), escenario.puesto2);
        assertTrue(uno.contains("2 · Verde · Nivel 1"));
        assertFalse(uno.contains("7 · Rojo"));
        assertTrue(dos.contains("Sin resultado"));
        assertFalse(dos.contains("7 · Rojo"));
        assertTrue(get("/salones/" + escenario.salonA).body().contains("2 · Verde · Nivel 1"));
        assertEquals(403, get("/evaluaciones?visita=" + escenario.visitaAbierta).statusCode());
    }

    @Test
    void reservasPropiasSeConsultanEnTodosLosEstadosIncluidoMotivo() throws Exception {
        for (int id : new int[]{escenario.reservaFinal, escenario.reservaAbierta, escenario.reservaEsperando,
                escenario.reservaCancelada}) {
            var respuesta = get("/reservas/" + id);
            assertEquals(200, respuesta.statusCode());
            assertFalse(respuesta.body().contains("Editar reserva"));
            assertFalse(respuesta.body().contains("Aceptar reserva"));
            assertFalse(respuesta.body().contains("Cancelar reserva"));
            assertFalse(respuesta.body().contains("Iniciar visita"));
        }
        assertTrue(get("/reservas/" + escenario.reservaCancelada).body().contains("La empresa solicitó otro horario."));
        assertFalse(
                get("/reservas/" + escenario.reservaAbierta).body().contains("/visitas/" + escenario.visitaAbierta));
        assertEquals(1, contarFilas(get("/reservas?estado=CANCELADA").body()));
    }

    @Test
    void filtrosCombinadosYOrdenDeCabeceraConservanLaConsulta() throws Exception {
        String ruta = "/evaluaciones?empresa=" + escenario.empresaA + "&sede=" + escenario.sedeA + "&salon=" +
                escenario.salonA + "&area=" + escenario.areaA + "&cargo=" + escenario.cargoA +
                "&numero=3&puntaje=7&color=ROJO&nivel=4&desde=2026-11-10&hasta=2026-11-10&est" +
                "ado=FINALIZADA&q=ensamble";
        var respuesta = get(ruta);
        assertEquals(200, respuesta.statusCode());
        assertEquals(1, contarFilas(respuesta.body()));
        assertTrue(respuesta.body().contains("data-registro=\"" + escenario.evalRojo + "\""));
        assertTrue(respuesta.body().contains("filtros-linea"));
        assertFalse(respuesta.body().contains("Más filtros"));
        assertTrue(respuesta.body().contains("value=\"ensamble\""));
        assertTrue(respuesta.body().contains("name=\"empresa\""));
        assertEquals(200, get(ruta + "&orden=fecha&direccion=desc").statusCode());
        assertEquals(1, contarFilas(get(ruta + "&orden=fecha&direccion=desc").body()));
    }

    @Test
    void sinCoincidenciasMuestraCeroYTextoHtmlSeEscapa() throws Exception {
        var respuesta = get(
                "/empresas?q=" + URLEncoder.encode("<script>texto</script>", java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("data-lista-contador"));
        assertTrue(respuesta.body().contains("data-lista-vacia"));
        assertTrue(respuesta.body().contains("&lt;script&gt;texto&lt;/script&gt;"));
        assertFalse(respuesta.body().contains("<script>texto"));
        assertEquals(400, get("/evaluaciones?orden=nombre%3BDELETE").statusCode());
        assertEquals(400, get("/evaluaciones?desde=incorrecta").statusCode());
    }

    @Test
    void listaDeUnaEvaluacionOcultaPuestoYLaGeneralLoConserva() throws Exception {
        var particular = get("/sugerencias?evaluacion=" + escenario.evalRojo);
        assertEquals(200, particular.statusCode());
        assertFalse(particular.body().contains("data-puesto"));
        assertTrue(get("/sugerencias").body().contains("data-puesto"));
        // RF-023, Paso 40: incluye la mejora de apoyo de piernas, aunque no tenga una regla inicial.
        assertTrue(particular.body().contains("12 sugerencias"));
    }

    @Test
    void administradorVeRojoAbiertoYConservaSusAcciones() throws Exception {
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(admin);
        assertEquals(200, get("/evaluaciones/" + escenario.evalAbierta).statusCode());
        assertTrue(get("/evaluaciones/" + escenario.evalAbierta).body().contains("/fotos/"));
        assertTrue(get("/empresas").body().contains("Nueva empresa"));
        assertTrue(get("/sugerencias?evaluacion=" + escenario.evalRojo).body().contains("Escribir sugerencia"));
        assertTrue(get("/pendientes/" + escenario.pendienteAbierto).body().contains("Marcar como arreglado"));
        assertTrue(get("/evaluaciones?estado=EN_CURSO&empresa=" + escenario.empresaA).body()
                .contains("7 · Rojo · Nivel 4"));
    }

    private static String fila(String texto2, int id) {
        var elemento = Pattern.compile("<tr\\b(?=[^>]*data-registro=\"" + id + "\")[^>]*>(.*?)</tr>", Pattern.DOTALL)
                .matcher(texto2);
        assertTrue(elemento.find());
        return elemento.group(1);
    }

    private static int contarFilas(String texto2) {
        return (int) Pattern.compile("<tr\\b(?![^>]*\\bhidden)(?=[^>]*\\bdata-registro=)[^>]*>").matcher(texto2)
                .results().count();
    }
}
