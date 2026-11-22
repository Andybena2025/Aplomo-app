package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InicioUsuario32Test extends HttpPrueba {
    @Test
    void saludoColombiaYDiasSonDeterministas() {
        LocalDate dia = LocalDate.of(2026, 11, 21);
        assertTrue(InicioUsuario.rotulo(dia.atTime(11, 59)).endsWith("BUENOS DÍAS"));
        assertTrue(InicioUsuario.rotulo(dia.atTime(12, 0)).endsWith("BUENAS TARDES"));
        assertTrue(InicioUsuario.rotulo(dia.atTime(19, 0)).endsWith("BUENAS NOCHES"));
        assertTrue(InicioUsuario.rotulo(dia.atTime(19, 0)).startsWith("SÁBADO 21/11/2026"));
        assertEquals("Hoy", InicioUsuario.faltan(dia, dia));
        assertEquals("Mañana", InicioUsuario.faltan(dia, dia.plusDays(1)));
        assertEquals("Faltan 6 días", InicioUsuario.faltan(dia, dia.plusDays(6)));
    }

    @Test
    void tendenciaMejoraEmpeoraOSeOcultaSinInventarResultados() {
        assertTrue(tendencia(7, 2).contains("▲ De 7 a 2"));
        assertTrue(tendencia(2, 5).contains("▼ De 2 a 5"));
        assertFalse(tendencia(2, 2).contains("tendencia-cambio"));
        StringBuilder html = new StringBuilder();
        InicioUsuario.tendencia(List.of(fila(1, 7)), List.of(evaluacion(1, 7)), html);
        assertFalse(html.toString().contains("tendencia-cambio"));
        assertTrue(html.toString().contains("· última"));
    }

    @Test
    void inicioSoloMuestraFinalizadasAyudaAutorizadaYSinFotos() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        var evaluador = evaluadorTrabajo();
        entrar(usuario);
        String html = get("/").body();
        assertTrue(html.contains("Bienvenido,"));
        assertFalse(html.contains("class=\"cabecera-rol\""));
        assertFalse(html.contains("class=\"cabecera-rol-texto\""));
        assertTrue(html.contains("data-ayuda-inicio"));
        assertTrue(html.contains("/avisos/nuevo?para=" + evaluador.id()));
        assertTrue(html.contains("/avisos/nuevo?para=" + new co.aplomo.servicio.Avisos(tx,reloj)
                .principal(cuenta(usuario)).id()));
        assertFalse(html.contains("/fotos/"));
        assertEquals(403, get("/evaluaciones/" + escenario.evalAbierta).statusCode());
        assertFalse(html.contains("href=\"/evaluaciones/" + escenario.evalAbierta + "\""));
        assertFalse(html.contains("deco-der"));
        assertTrue(html.contains("class=\"deco-herramientas\""));
        assertTrue(html.contains("class=\"marca-cabecera-a\""));
    }

    private static String tendencia(int antes, int despues) {
        StringBuilder html = new StringBuilder();
        InicioUsuario.tendencia(List.of(fila(1, antes), fila(2, despues)),
                List.of(evaluacion(1, antes), evaluacion(2, despues)), html);
        return html.toString();
    }
    private static Fila evaluacion(int visita, int puntos) {
        return new Fila(Map.of("id", "" + visita, "visita", "" + visita, "estado", "FINALIZADA",
                "puntaje", "" + puntos));
    }
    private static Fila fila(int id, int puntos) {
        return new Fila(Map.of("id", "" + id, "estado", "FINALIZADA", "fecha", "2026-11-0" + id,
                "puntaje", "" + puntos, "color", puntos > 4 ? "ROJO" : "VERDE"));
    }
}
