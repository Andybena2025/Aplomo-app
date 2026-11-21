package co.aplomo.web;

import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Agenda.Cita;
import co.aplomo.servicio.Agenda.Evaluador;
import co.aplomo.servicio.Agenda.Vista;
import co.aplomo.servicio.ColoresAgenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * RF-033, RNF-011: armador visual del calendario, sin consultas ni permisos propios.
 */
public final class Calendario {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm");

    private Calendario() {
    }

    /**
     * RF-033: mes o semana, con alternativa de lista en pantallas pequeñas.
     */
    public static String cuadricula(Vista vista) {
        StringBuilder html = new StringBuilder("<div class=\"calendario ").append(
                vista.mes() ? "cal-mes" : "cal-semana").append("\">");
        if (!vista.mes()) html.append("<span class=\"cal-horas-titulo\">Hora</span>");
        for (String dia : List.of("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")) {
            html.append("<div class=\"cal-dia-titulo\">").append(dia).append("</div>");
        }
        LocalDate desde = vista.desde().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate hasta = vista.mes() ? vista.hasta().minusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                                        .plusDays(1) : vista.hasta();
        if (!vista.mes()) html.append(horas());
        for (LocalDate fecha = desde; fecha.isBefore(hasta); fecha = fecha.plusDays(1)) {
            html.append(dia(vista, fecha));
        }
        return html.append("</div><section class=\"cal-dia-seleccionado stack\" hidden ")
                .append("aria-live=\"polite\"></section>").toString();
    }

    private static String dia(Vista vista, LocalDate fecha) {
        List<Cita> citas = vista.citas().stream().filter(cita -> cita.ficha().reserva().fecha().equals(fecha)).toList();
        String clases = fecha.equals(Html.hoy()) ? " cal-hoy" : "";
        if (fecha.getMonth() != vista.fecha().getMonth() || fecha.getDayOfWeek().getValue() > 5) {
            clases += " cal-oscuro";
        }
        StringBuilder html = new StringBuilder("<section class=\"cal-dia").append(clases).append("\" data-dia=\"")
                .append(Html.fecha(fecha)).append("\">")
                .append(vista.mes() ? "<button type=\"button\" class=\"cal-numero\" aria-label=\"" :
                        "<span " + "class=\"cal" + "-numero\" " + "aria-label=\"").append(Html.fecha(fecha))
                .append("\">").append(fecha.getDayOfMonth()).append(vista.mes() ? "</button>" : "</span>");
        html.append("<div class=\"cal-eventos\">");
        for (Cita cita : citas.stream().limit(vista.mes() ? 2 : Integer.MAX_VALUE).toList()) {
            html.append(pildora(cita, !vista.mes()));
        }
        if (vista.mes() && citas.size() > 2) {
            html.append("<a class=\"cal-mas\" href=\"").append(PaginasAgenda.url(vista, fecha, false)).append("\">+")
                    .append(citas.size() - 2).append(" más</a>");
        }
        return html.append("</div></section>").toString();
    }

    private static String horas() {
        StringBuilder html = new StringBuilder("<div class=\"cal-horas\">");
        for (int hora = 7; hora <= 18; hora++) {
            html.append("<span>").append(hora).append(":00</span>");
        }
        return html.append("</div>").toString();
    }

    /**
     * RF-033: píldora con color estable, nota y duración en la vista semanal.
     */
    public static String pildora(Cita cita, boolean semana) {
        var reserva = cita.ficha().reserva();
        int indice = cita.color() == null ? -1 : ColoresAgenda.PALETA.indexOf(cita.color());
        boolean pasada = reserva.fecha().isBefore(Html.hoy());
        String estilo = "";
        if (semana) {
            int inicio = reserva.inicio().toSecondOfDay() / 60 - 7 * 60;
            int duracion = (reserva.fin().toSecondOfDay() - reserva.inicio().toSecondOfDay()) / 60;
            estilo = " style=\"--inicio:" + Math.max(0, inicio) + ";--duracion:" + duracion + "\"";
        }
        String nota = cita.nota() == null || cita.nota().texto() == null ? "" : cita.nota()
                .vista() ? "<span class=\"sr-only\">Nota leída</span>" : "<i class=\"cal-nota\" title=\"Nota del " +
                                                                         "Administrador\"></i>";
        return "<a class=\"cal-cita color-" + indice + (pasada ? " pasada" : "") + "\" data-evaluador=\"" +
                cita.evaluador() + "\" href=\"/reservas/" + reserva.id() + "\"" + estilo + "><small>" +
                reserva.inicio().format(HORA) + "</small><b>" + Html.escapar(cita.ficha().empresaNombre()) + "</b>" +
                nota + "</a>";
    }

    /**
     * RF-033: filtros administrativos o próximas tres citas del Evaluador.
     */
    public static String panel(Sesion sesion, Vista vista) {
        StringBuilder html = new StringBuilder("<aside class=\"card stack cal-panel\">");
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) {
            html.append("<h2>Evaluadores</h2>");
            for (Evaluador evaluador : vista.evaluadores()) {
                if (evaluador.activo()) html.append(evaluador(evaluador, vista));
            }
            html.append(
                    "<p class=\"hint\">Solo lectura. Desde una cita puedes dejarle una nota al ev" + "aluador.</p>");
        } else {
            html.append("<h2>Próximas citas</h2>");
            List<Cita> futuras = vista.citas().stream()
                    .filter(cita -> !cita.ficha().reserva().fecha().isBefore(Html.hoy())).limit(3).toList();
            for (Cita cita : futuras) html.append(proxima(cita));
            if (futuras.isEmpty()) html.append("<p class=\"hint\">Sin citas próximas este mes.</p>");
        }
        return html.append("</aside>").toString();
    }

    private static String evaluador(Evaluador evaluador, Vista vista) {
        long cantidad = vista.citas().stream().filter(cita -> Integer.valueOf(evaluador.id()).equals(cita.evaluador()))
                .count();
        return "<label class=\"cal-evaluador color-" +
                (evaluador.color() == null ? -1 : ColoresAgenda.PALETA.indexOf(evaluador.color())) +
                "\"><input type=\"checkbox\" data-filtrar-evaluador=\"" + evaluador.id() + "\" checked><i></i><span>" +
                Html.escapar(evaluador.nombre()) + "<small>" + cantidad + " citas este mes</small></span></label>";
    }

    private static String proxima(Cita cita) {
        var ficha = cita.ficha();
        var reserva = ficha.reserva();
        String fecha =
                reserva.fecha().format(DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.forLanguageTag("es-CO"))) +
                        " · " + reserva.inicio().format(HORA);
        return "<a class=\"cal-proxima\" href=\"/reservas/" + reserva.id() + "\"><b>" + Html.escapar(fecha) +
                "</b><span>" +
                Html.escapar(ficha.empresaNombre() + " · " + ficha.sedeNombre() + " · " + ficha.salonNombre()) +
                "</span>" +
                (cita.nota() != null && cita.nota().texto() != null ? "<small>Nota del Administrador</small>" : "") +
                "</a>";
    }
}
