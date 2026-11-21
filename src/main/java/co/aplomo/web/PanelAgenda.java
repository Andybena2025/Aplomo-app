package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.modelo.Cuenta.Rol;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RF-033, RNF-002: resumen lateral de las mismas citas autorizadas de la tabla.
 */
final class PanelAgenda {
    private PanelAgenda() {
    }

    static String mostrar(Sesion sesion, List<Fila> filas) {
        long pendientes = filas.stream().filter(f -> f.valor("estado").equals("ESPERANDO")).count();
        if (sesion.cuenta().rol() == Rol.EVALUADOR) {
            var lunes = Html.hoy().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long cantidad = filas.stream().filter(f -> f.valor("fecha").compareTo(lunes.toString()) >= 0 &&
                    f.valor("fecha").compareTo(lunes.plusWeeks(1).toString()) < 0 &&
                    !f.valor("estado").equals("CANCELADA")).count();
            return "<aside class=\"panel-agenda card stack\"><h2>Tu semana</h2><p>" +
                    Html.plural(Math.toIntExact(cantidad), "cita") + " · " + pendientes + " por responder</p></aside>";
        }
        StringBuilder html = new StringBuilder("<aside class=\"panel-agenda card stack\"><h2>").append(
                        Html.hoy().format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag(
                                "es-CO"))))
                .append("</h2><div class=\"mini-calendario\">");
        for (String dia : List.of("L", "M", "M", "J", "V", "S", "D")) html.append("<b>").append(dia).append("</b>");
        LocalDate primer = Html.hoy().withDayOfMonth(1);
        LocalDate desde = primer.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < 42; i++) {
            LocalDate dia = desde.plusDays(i);
            html.append("<span class=\"").append(dia.equals(Html.hoy()) ? "mini-hoy" : "").append("\">")
                    .append(dia.getDayOfMonth()).append("<small>");
            filas.stream()
                    .filter(f -> f.valor("fecha").equals(dia.toString()) && !f.valor("estado").equals("CANCELADA"))
                    .map(f -> TextoLista.persona(f.valor("evaluador_nombre"), f.valor("evaluador_color"))).distinct()
                    .limit(3).forEach(html::append);
            html.append("</small></span>");
        }
        return html.append("</div><h3>Requiere atención</h3><a href=\"/reservas?estado=ESPERANDO\">").append(pendientes)
                .append(" por confirmar</a><a href=\"/visitas?estado=EN_CURSO\">")
                .append(filas.stream().filter(f -> f.valor("visita_estado").equals("EN_CURSO")).count())
                .append(" visitas en curso</a></aside>").toString();
    }
}
