package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Agenda;
import co.aplomo.soporte.HttpPrueba;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-033, RNF-002: filtros compactos accesibles y sobres fuera del label para conservar la selección.
 */
class FiltrosAgenda31BTest extends HttpPrueba {
    @Test
    void abreviaNombresConservaCompletoYElSobreNoCambiaLaCasilla() throws Exception {
        var personas = List.of(new Agenda.Evaluador(11, "Ing. Andrés Eduardo Benavides", true, "#3D7BFF"),
                new Agenda.Evaluador(12, "Ing. Johan Andrés Zapata Quintero", true, "#16BFC9"),
                new Agenda.Evaluador(13, "Ing. Laura Gabriella Romero Gómez", true, "#9B6BFF"),
                new Agenda.Evaluador(14, "Persona Inactiva", false, null));
        LocalDate fecha = LocalDate.now(reloj);
        var vista = new Agenda.Vista(true, fecha, fecha.withDayOfMonth(1), fecha.plusMonths(1), List.of(), personas,
                "TODOS", false);
        String html = Calendario.filtros(new Sesion("id", "csrf", cuenta(admin), Instant.now()), vista);
        for (String nombre : List.of("Andrés Benavides", "Johan Zapata", "Laura Romero")) {
            assertTrue(html.contains("<span>" + nombre + "</span>"));
            assertTrue(html.contains("aria-label=\"Escribir a " + nombre + "\""));
            assertTrue(html.contains("aria-label=\"Filtrar por " + nombre + "\""));
        }
        assertTrue(html.contains("title=\"Ing. Laura Gabriella Romero Gómez\""));
        assertEquals(3, html.split("</label><a class=\"cal-escribir\"", -1).length - 1);
        assertEquals(3, html.split("type=\"checkbox\"", -1).length - 1);
        assertFalse(html.contains("Persona Inactiva"));
        assertFalse(html.contains(">Escribir</a>"));
        assertTrue(html.contains("href=\"/agenda/evaluadores/11\""));
        assertEquals("", Calendario.filtros(new Sesion("id", "csrf", cuenta(usuario), Instant.now()), vista));
    }

    @Test
    void nombreConHtmlSeEscapaEnTextoYEnLosAtributosDelSobre() throws Exception {
        var persona = new Agenda.Evaluador(21, "Ing. Ana <script>", true, null);
        LocalDate fecha = LocalDate.now(reloj);
        var vista = new Agenda.Vista(false, fecha, fecha, fecha.plusWeeks(1), List.of(), List.of(persona), "TODOS",
                false);
        String html = Calendario.filtros(new Sesion("id", "csrf", cuenta(admin), Instant.now()), vista);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("Ana &lt;script&gt;"));
        assertTrue(html.contains("title=\"Ing. Ana &lt;script&gt;\""));
    }
}
