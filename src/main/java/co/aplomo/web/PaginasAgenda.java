package co.aplomo.web;

import co.aplomo.modelo.Cuenta.Rol;

import static co.aplomo.web.Html.escapar;

import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Agenda.Cita;
import co.aplomo.servicio.Agenda.Nota;
import co.aplomo.servicio.Agenda.Vista;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * RF-033, RNF-002: calendario de un vistazo, con colores ajenos al riesgo RULA.
 */
public final class PaginasAgenda {
    private PaginasAgenda() {
    }

    /**
     * RF-033: compone cabecera, cuadrícula y panel según el rol autorizado.
     */
    public static String pagina(Sesion sesion, Vista vista) {
        String etiqueta = sesion.cuenta().rol() == Rol.ADMINISTRADOR ? "Agenda · Todos los evaluadores" : "Mi agenda";
        String titulo = vista.fecha().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO")));
        titulo = Character.toUpperCase(titulo.charAt(0)) + titulo.substring(1);
        String accion = sesion.cuenta().rol() ==
                Rol.EVALUADOR ? "<a class=\"btn p\" href=\"/reservas/nueva\">Nueva reserva</a>" : "";
        StringBuilder pagina = new StringBuilder(Html.encabezado(etiqueta, titulo, accion));
        pagina.append(controles(vista)).append("<div class=\"calendario-layout\"><section>")
                .append(Calendario.cuadricula(vista)).append("</section>").append(Calendario.panel(sesion, vista))
                .append("</div>").append("<script src=\"/static/js/agenda.js\" defer></script>");
        return Html.pagina(etiqueta, pagina.toString(), sesion, false, false);
    }

    private static String controles(Vista vista) {
        LocalDate anterior = vista.mes() ? vista.fecha().minusMonths(1) : vista.fecha().minusWeeks(1);
        LocalDate siguiente = vista.mes() ? vista.fecha().plusMonths(1) : vista.fecha().plusWeeks(1);
        return "<nav class=\"calendario-controles\" aria-label=\"Fechas de agenda\">" +
                link(vista, anterior, vista.mes(), "‹") + link(vista, Html.hoy(), vista.mes(), "Hoy") +
                link(vista, siguiente, vista.mes(), "›") + "<div class=\"segmentado\">" +
                link(vista, vista.fecha(), false, "Semana") + link(vista, vista.fecha(), true, "Mes") + "</div></nav>";
    }

    /**
     * RF-033: URL conserva el filtro elegido al navegar entre fechas.
     */
    public static String url(Vista vista, LocalDate fecha, boolean mes) {
        return "/agenda?vista=" + (mes ? "mes" : "semana") + "&amp;fecha=" + fecha +
                (vista.evaluadores().isEmpty() ? "" : "&amp;evaluador=" + Html.escapar(vista.filtro())) +
                (vista.canceladas() ? "&amp;canceladas=1" : "");
    }

    private static String link(Vista vista, LocalDate fecha, boolean mes, String texto) {
        return "<a class=\"btn\" href=\"" + url(vista, fecha, mes) + "\">" + texto + "</a>";
    }

    /**
     * RF-033: tarjeta reutilizada en Inicio; mantiene la nota privada sin publicar su texto.
     */
    public static String cita(Cita cita) {
        return Calendario.pildora(cita, false);
    }

    /**
     * Nota editable por Administrador; el Evaluador solo confirma su lectura. RNF-011.
     */
    public static String nota(Sesion sesion2, int reserva, Nota elemento) {
        if (sesion2.cuenta().rol() == Rol.USUARIO) return "";
        if (sesion2.cuenta().rol() == Rol.ADMINISTRADOR)
            return "<section class=\"card stack\"><h2>Nota para el evaluador</h2><form method=\"" +
                    "post\" action=\"/reservas/" + reserva + "/nota-administrador\" class=\"stack\">" +
                    Html.csrf(sesion2) +
                    "<div class=\"field\"><label for=\"nota-administrador\">Nota para el evaluado" +
                    "r</label><textarea class=\"input\" id=\"nota-administrador\" name=\"nota\" m" +
                    "axlength=\"500\">" + escapar(elemento == null ? "" : elemento.texto()) +
                    "</textarea></div><button class=\"btn\">Guardar nota</button></form></section>";
        if (elemento == null || elemento.texto() == null) return "";
        return "<section class=\"aviso stack\"><h2>Nota del Administrador</h2><p class=\"tex" + "to-notas\">" +
                escapar(elemento.texto()) + "</p>" + (elemento.vista() ? "<span class=\"hint\">Nota leída</span>" :
                "<form method=\"post\" action=\"/reservas/" + reserva + "/entendido\">" + Html.csrf(sesion2) +
                "<button class=\"btn\">Entendido</button></form>") + "</section>";
    }
}
