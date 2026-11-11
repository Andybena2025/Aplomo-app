package co.aplomo.web;

import co.aplomo.dao.VisitaDao.*;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.time.LocalDateTime;
import java.util.*;
import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/** RF-013a, RF-013b: visitas y evaluaciones consultables sin entregar fotografías. */
public final class PaginasVisitas {
    private PaginasVisitas() {}

    /** Lista las visitas permitidas por el servicio para cada rol. */
    public static String lista(Sesion sesion, List<Ficha> visitas) {
        StringBuilder html = new StringBuilder("<p class=\"muted\">").append(Html.plural(visitas.size(), "visita")).append("</p>");
        if (visitas.isEmpty()) {
            html.append("<section class=\"card stack\"><h2>Tus visitas</h2><p class=\"muted\">Todavía no hay visitas disponibles.</p>");
            if (admin(sesion)) html.append(enlace("/reservas", "Ver reservas", false));
            html.append("</section>");
        } else {
            html.append("<div class=\"tablewrap\"><table><thead><tr><th>Reserva</th><th>Empresa y lugar</th><th>Inicio real</th><th>Estado</th><th>Calificación</th><th>Detalle</th></tr></thead><tbody>");
            for (Ficha ficha : visitas) {
                html.append("<tr><td>").append(escapar(ficha.codigoReserva())).append("</td><td>")
                        .append(escapar(ficha.empresaNombre())).append("<br><span class=\"muted small\">")
                        .append(escapar(ficha.sedeNombre())).append(" · ").append(escapar(ficha.salonNombre()))
                        .append("</span></td><td class=\"num\">").append(fecha(ficha.visita().inicio()))
                        .append("</td><td><span class=\"").append(claseEstado(ficha.visita().estado())).append("\">")
                        .append(estado(ficha.visita().estado())).append("</span></td><td>")
                        .append(calificacion(ficha)).append("</td><td>").append(enlace("/visitas/" + ficha.visita().id(), "Ver visita", false))
                        .append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        return Html.pagina("Visitas", html.toString(), sesion);
    }

    /** Detalla la visita, su horario real y las evaluaciones guardadas. */
    public static String detalle(Sesion sesion, Ficha ficha, List<Evaluacion> evaluaciones, String aviso, boolean exito) {
        StringBuilder html = new StringBuilder(enlace("/visitas", "← Visitas", false));
        html.append(Html.aviso(aviso, exito)).append("<section class=\"card stack\"><div class=\"row between\"><h2>")
                .append(escapar(ficha.empresaNombre())).append("</h2><span class=\"").append(claseEstado(ficha.visita().estado())).append("\">").append(estado(ficha.visita().estado()))
                .append("</span></div><p class=\"muted\">").append(escapar(ficha.sedeNombre())).append(" · ")
                .append(escapar(ficha.salonNombre())).append("</p><dl class=\"datos-cuenta\">")
                .append(dato("Inicio real", fecha(ficha.visita().inicio()))).append(dato("Fin real", fecha(ficha.visita().fin())))
                .append(dato("Calificación", calificacion(ficha))).append(dato("Notas", ficha.visita().notas())).append("</dl>")
                .append(enlace("/reservas/" + ficha.visita().reserva(), "Ver " + ficha.codigoReserva(), false)).append("</section>")
                .append("<section class=\"stack\"><div class=\"row between\"><h2>Evaluaciones</h2><span class=\"muted\">")
                .append(Html.plural(evaluaciones.size(), "registrada")).append("</span></div>");
        if (evaluaciones.isEmpty()) html.append("<p class=\"muted\">Todavía no hay evaluaciones registradas en esta visita.</p>");
        else {
            html.append("<div class=\"tablewrap\"><table><thead><tr><th>Puesto</th><th>Fecha</th><th>Postura</th><th>Puntaje</th></tr></thead><tbody>");
            for (Evaluacion evaluacion : evaluaciones) {
                html.append("<tr><td>Puesto ").append(evaluacion.numero()).append("</td><td>")
                        .append(Html.fecha(evaluacion.fecha())).append("</td><td>")
                        .append(escapar(evaluacion.postura())).append("</td><td>")
                        .append(evaluacion.puntaje() == null ? "Sin puntaje disponible" : evaluacion.puntaje()).append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        html.append("</section>");
        if (admin(sesion) && ficha.visita().estado().equals("EN_CURSO")) {
            if (evaluaciones.isEmpty()) html.append("<p class=\"hint\">Guarda al menos una evaluación para poder finalizar la visita.</p>");
            else html.append(enlace("/visitas/" + ficha.visita().id() + "/finalizar", "Finalizar visita", true));
        }
        return Html.pagina("Visita · " + ficha.codigoReserva(), html.toString(), sesion);
    }

    /** Conserva las notas y explica que la finalización requiere al menos una evaluación guardada. */
    public static String finalizar(Sesion sesion, Ficha ficha, List<Evaluacion> evaluaciones, String notas, String error) {
        boolean sinEvaluaciones = evaluaciones.isEmpty();
        return Html.pagina("Finalizar visita · " + ficha.codigoReserva(),
                enlace("/visitas/" + ficha.visita().id(), "← Volver a la visita", false) + Html.aviso(error, false)
                        + (sinEvaluaciones ? Html.aviso("Guarda al menos una evaluación antes de finalizar la visita.", false) : "")
                        + "<form class=\"card stack cuenta-form\" method=\"post\" action=\"/visitas/" + ficha.visita().id() + "/finalizar\">"
                        + Html.csrf(sesion) + "<p>Al finalizar, la reserva también quedará finalizada y los resultados estarán disponibles para la empresa.</p>"
                        + Formularios.texto(Map.of("notas", notas == null ? "" : notas), "notas", "Notas de la visita", 65535, false)
                        + "<button class=\"btn p\"" + (sinEvaluaciones ? " disabled" : "") + ">Finalizar visita</button></form>", sesion);
    }

    private static String fecha(LocalDateTime valor) { return Html.fechaHora(valor); }
    private static String estado(String valor) { return valor.equals("EN_CURSO") ? "En curso" : "Finalizada"; }
    private static String claseEstado(String valor) { return valor.equals("EN_CURSO") ? "tag warn" : "tag good"; }
    private static String calificacion(Ficha ficha) { return ficha.visita().calificacion() == null ? "Sin calificación disponible" : ficha.visita().calificacion().toPlainString(); }
    private static String dato(String titulo, String valor) { return "<div><dt>" + escapar(titulo) + "</dt><dd class=\"texto-notas\">" + escapar(valor) + "</dd></div>"; }
    private static boolean admin(Sesion sesion) { return sesion.cuenta().rol() == Rol.ADMINISTRADOR; }
}
