package co.aplomo.web;

import co.aplomo.dao.VisitaDao.Evaluacion;
import co.aplomo.dao.VisitaDao.Ficha;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/**
 * RF-013a, RF-013b: visitas y evaluaciones consultables sin entregar fotografías.
 */
public final class PaginasVisitas {
    private PaginasVisitas() {
    }

    /**
     * Lista las visitas permitidas por el servicio para cada rol. RNF-011.
     */
    public static String lista(Sesion sesion, List<Ficha> visitas) {
        StringBuilder html = new StringBuilder("<p class=\"muted\">").append(Html.plural(visitas.size(), "visita"))
                .append("</p>");
        if (visitas.isEmpty()) {
            html.append("<section class=\"card stack\"><h2>Tus visitas</h2><p class=\"muted\">Todavía" +
                    " no hay visitas disponibles.</p>");
            if (admin(sesion)) html.append(enlace("/reservas", "Ver reservas", false));
            html.append("</section>");
        } else {
            html.append("<div class=\"tablewrap\"><table class=\"resp\"><thead><tr><th>Reserva</th><t" +
                    "h>Empresa y lugar</th><th>Inicio real</th><th>Estado</th><th>Calificación</t" +
                    "h><th>Detalle</th></tr></thead><tbody>");
            for (Ficha ficha : visitas) {
                html.append("<tr><td data-label=\"Reserva\"><div class=\"cell-value\">")
                        .append(escapar(ficha.codigoReserva()))
                        .append("</div></td><td data-label=\"Empresa\"><div class=\"cell-value\">")
                        .append(escapar(ficha.empresaNombre())).append("<br><span class=\"muted small\">")
                        .append(escapar(ficha.sedeNombre())).append(" · ").append(escapar(ficha.salonNombre()))
                        .append("</span></div></td><td data-label=\"Inicio\" class=\"num\"><div class=\"cell-" +
                                "value\">").append(fecha(ficha.visita().inicio()))
                        .append("</div></td><td data-label=\"Estado\"><div class=\"cell-value\"><span class=\"")
                        .append(claseEstado(ficha.visita().estado())).append("\">")
                        .append(estado(ficha.visita().estado()))
                        .append("</span></div></td><td data-label=\"Calificación\"><div class=\"cell-value\">")
                        .append(calificacion(ficha)).append("</div></td><td><div class=\"cell-value\">")
                        .append(enlace("/visitas/" + ficha.visita().id(), "Ver visita", false))
                        .append("</div></td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        return Html.pagina("Visitas", html.toString(), sesion, true, false);
    }

    /**
     * Detalla la visita, su horario real y las evaluaciones guardadas. RNF-011.
     */
    public static String detalle(Sesion sesion, Ficha ficha, List<Evaluacion> evaluaciones,
            List<co.aplomo.modelo.Puesto> puestos, String aviso, boolean exito) {
        StringBuilder html = new StringBuilder(enlace("/visitas", "← Visitas", false));
        html.append(Html.aviso(aviso, exito)).append("<section class=\"card stack\"><div class=\"row between\"><h2>")
                .append(escapar(ficha.empresaNombre())).append("</h2><span class=\"")
                .append(claseEstado(ficha.visita().estado())).append("\">").append(estado(ficha.visita().estado()))
                .append("</span></div><p class=\"muted\">").append(escapar(ficha.sedeNombre())).append(" · ")
                .append(escapar(ficha.salonNombre())).append("</p><dl class=\"datos-cuenta\">")
                .append(dato("Horario real", Html.fecha(ficha.visita().inicio().toLocalDate()) + " · " +
                        TextoLista.hora(ficha.visita().inicio().toString()) +
                        (ficha.visita().fin() == null ? "" : " a " + TextoLista.hora(ficha.visita().fin().toString()))))
                .append(ficha.visita().notas() == null || ficha.visita().notas().isBlank() ? "" : dato("Notas",
                        ficha.visita().notas())).append("</dl>")
                .append(enlace("/reservas/" + ficha.visita().reserva(), "Ver reserva", false)).append("</section>")
                .append("<section class=\"stack\"><div class=\"row between\"><h2>" + (admin(sesion) &&
                        ficha.visita().estado().equals("EN_CURSO") ? "Puestos de esta visita" : "Evaluaciones") +
                        "</h2><span class=\"muted\">").append(Html.plural(evaluaciones.size(), "registrada"))
                .append("</span></div>");
        boolean enCursoAdmin = admin(sesion) && ficha.visita().estado().equals("EN_CURSO");
        if (enCursoAdmin) html.append(puestos(ficha, puestos, evaluaciones));
        html.append("<div class=\"row visita-acciones\">")
                .append(enlace("/evaluaciones?visita=" + ficha.visita().id(), "Consultar evaluaciones", false));
        if (enCursoAdmin && !evaluaciones.isEmpty())
            html.append(enlace("/visitas/" + ficha.visita().id() + "/finalizar", "Finalizar visita", true));
        html.append("</div>");
        if (evaluaciones.isEmpty())
            html.append("<p class=\"muted\">Todavía no hay evaluaciones registradas en esta visita.</p>");
        else if (!enCursoAdmin) {
            html.append("<div class=\"table-scroll\"><table class=\"resp alineo-table visita-evaluaciones\">" +
                    "<thead><tr><th>Puesto</th><th>Fecha</th><th>Resultado</th>" +
                    (admin(sesion) ? "<th>Acciones</th>" : "") + "</tr></thead><tbody>");
            var grupos = evaluaciones.stream().collect(
                    java.util.stream.Collectors.groupingBy(Evaluacion::puesto, java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.toList()));
            for (var grupo : grupos.values()) {
                Evaluacion evaluacion = grupo.stream()
                        .max(java.util.Comparator.comparingInt(e -> e.puntaje() == null ? -1 : e.puntaje()))
                        .orElseThrow();
                html.append("<tr data-puesto-visita=\"").append(evaluacion.puesto())
                        .append("\"><td data-label=\"Puesto\"><div class=\"fila-enlace\"><strong title=\"")
                        .append(escapar(TextoLista.puesto(evaluacion.cargo(), evaluacion.descripcion()))).append("\">")
                        .append(escapar(TextoLista.puesto(evaluacion.cargo(), evaluacion.descripcion())))
                        .append("</strong><small>Puesto ").append(evaluacion.numero()).append(" · ")
                        .append(Html.plural(grupo.size(), "postura")).append("</small></div></td>")
                        .append("<td data-label=\"Fecha\">").append(Html.fecha(evaluacion.fecha())).append("</td>")
                        .append("<td data-label=\"Resultado\">")
                        .append(evaluacion.puntaje() == null ? "Sin puntaje disponible" : Html.riesgo(
                                evaluacion.puntaje(), evaluacion.color(), evaluacion.nivel())).append("</td>");
                if (admin(sesion))
                    html.append("<td data-label=\"Acciones\"><a class=\"btn sm\" href=\"/evaluaciones?visita=")
                            .append(ficha.visita().id()).append("&amp;puesto=").append(evaluacion.puesto())
                            .append("\">Ver evaluación</a></td>");
                html.append("</tr>");
            }
            html.append("</tbody></table></div>");
        }
        html.append("</section>");
        if (admin(sesion) && ficha.visita().estado().equals("EN_CURSO")) {
            if (evaluaciones.isEmpty()) html.append(
                    "<p class=\"hint\">Guarda al menos una evaluación para poder finalizar la vis" + "ita.</p>");
        }
        return Html.pagina("Visita", html.toString(), sesion);
    }

    private static String puestos(Ficha ficha, List<co.aplomo.modelo.Puesto> puestos, List<Evaluacion> evaluaciones) {
        Map<Integer, Evaluacion> ultima = new HashMap<>();
        for (Evaluacion evaluacion2 : evaluaciones)
            ultima.merge(evaluacion2.puesto(), evaluacion2, (evaluacion3, evaluacion4) ->
                    (evaluacion3.puntaje() == null ? -1 : evaluacion3.puntaje()) >=
                            (evaluacion4.puntaje() == null ? -1 : evaluacion4.puntaje()) ? evaluacion3 : evaluacion4);
        StringBuilder contenido = new StringBuilder(
                "<div class=\"tour-puestos\" aria-label=\"Puestos de esta visita\">");
        for (var elemento : puestos) {
            var evaluacion5 = ultima.get(elemento.id());
            contenido.append("<a class=\"card tour-puesto\" href=\"/evaluaciones/nueva?visita=")
                    .append(ficha.visita().id()).append("&amp;puesto=").append(elemento.id()).append("\"><strong>")
                    .append(escapar(TextoLista.puesto(elemento.cargoNombre(), elemento.descripcion())))
                    .append("</strong><span>Puesto ").append(elemento.numero()).append("</span><span class=\"tag\">")
                    .append(evaluacion5 == null ? "○ Pendiente" : "✓ Evaluado").append("</span>");
            if (evaluacion5 == null) contenido.append("<span class=\"btn p\">Evaluar</span>");
            if (evaluacion5 != null) contenido.append(
                    evaluacion5.puntaje() == null ? "<span class=\"hint\">Sin puntaje disponible</span>" : Html.riesgo(
                            evaluacion5.puntaje(), evaluacion5.color(), evaluacion5.nivel()));
            contenido.append("</a>");
        }
        if (puestos.isEmpty()) contenido.append(Html.vacio("Este salón no tiene puestos activos",
                enlace("/salones/" + ficha.salon() + "/puestos/nuevo", "Registrar un puesto", false)));
        return contenido.append("</div>").toString();
    }

    /**
     * Conserva las notas y explica que la finalización requiere al menos una evaluación guardada. RNF-011.
     */
    public static String finalizar(Sesion sesion, Ficha ficha, List<Evaluacion> evaluaciones, String notas,
            String error) {
        boolean sinEvaluaciones = evaluaciones.isEmpty();
        return Html.pagina("Finalizar visita",
                enlace("/visitas/" + ficha.visita().id(), "← Volver a la visita", false) + Html.aviso(error, false) +
                        (sinEvaluaciones ? Html.aviso("Guarda al menos una evaluación antes de finalizar la visita.",
                                false) : "") +
                        "<form class=\"card stack cuenta-form\" method=\"post\" action=\"/visitas/" +
                        ficha.visita().id() + "/finalizar\">" + Html.csrf(sesion) +
                        "<p>Al finalizar, la reserva también quedará finalizada y los resultados esta" +
                        "rán disponibles para la empresa.</p>" +
                        Formularios.texto(Map.of("notas", notas == null ? "" : notas), "notas", "Notas de la visita",
                                65535, false) + "<button class=\"btn p\"" + (sinEvaluaciones ? " disabled" : "") +
                        ">Finalizar visita</button> " +
                        enlace("/informes/empresa/" + ficha.empresa() + "?visita=" + ficha.visita().id(),
                                "Exportar PDF", false) + "</form>", sesion);
    }

    private static String fecha(LocalDateTime valor) {
        return Html.fechaHora(valor);
    }

    private static String estado(String valor) {
        return valor.equals("EN_CURSO") ? "En curso" : "Finalizada";
    }

    private static String claseEstado(String valor) {
        return valor.equals("EN_CURSO") ? "tag warn" : "tag good";
    }

    private static String calificacion(Ficha ficha) {
        return ficha.visita().calificacion() == null ? "Sin calificación disponible" : ficha.visita().calificacion()
                                                                                       .toPlainString();
    }

    private static String dato(String titulo, String valor) {
        return "<div><dt>" + escapar(titulo) + "</dt><dd class=\"texto-notas\">" + escapar(valor) + "</dd></div>";
    }

    private static boolean admin(Sesion sesion) {
        return sesion.cuenta().rol() != Rol.USUARIO;
    }
}
