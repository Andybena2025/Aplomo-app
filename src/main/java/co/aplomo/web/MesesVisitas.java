package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** RF-024, RNF-007: meses plegables sobre las visitas que el servidor ya autorizó. */
final class MesesVisitas {
    private MesesVisitas() { }

    /** RF-024: cinco filas iniciales por mes; no limita los datos disponibles para filtrar. */
    static String mostrar(Sesion sesion, List<Fila> filas) {
        var meses = filas.stream().filter(f -> f.valor("estado").equals("FINALIZADA"))
                .collect(Collectors.groupingBy(f -> f.valor("fecha").substring(0, 7),
                        () -> new TreeMap<String, List<Fila>>(Comparator.reverseOrder()), Collectors.toList()));
        StringBuilder html = new StringBuilder("<div class=\"visitas-meses\">");
        boolean primero = true;
        for (var mes : meses.entrySet()) {
            String nombre = LocalDate.parse(mes.getKey() + "-01").format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO")));
            nombre = nombre.substring(0, 1).toUpperCase(Locale.forLanguageTag("es")) + nombre.substring(1);
            html.append("<details class=\"mes-visitas\" data-mes-visitas=\"").append(mes.getKey())
                    .append("\" data-nombre-mes=\"").append(Html.escapar(nombre)).append("\"")
                    .append(primero ? " open" : "").append("><summary><span class=\"mes-flecha\" ")
                    .append("aria-hidden=\"true\">›</span><h2>").append(nombre)
                    .append("</h2><span data-mes-contador>").append(Html.plural(mes.getValue().size(), "visita"))
                    .append(" ·</span><span class=\"mes-riesgos\">");
            for (String nivel : List.of("alto", "medio", "bajo")) {
                long cantidad = mes.getValue().stream().filter(f -> riesgo(f).equals(nivel)).count();
                html.append("<span class=\"mes-riesgo ").append(nivel).append("\" data-mes-riesgo=\"")
                        .append(nivel).append("\"><i aria-hidden=\"true\"></i><span>")
                        .append(cantidad).append("</span> ").append(nivel).append("</span>");
            }
            long sinResultado = mes.getValue().stream().filter(f -> riesgo(f).equals("sin")).count();
            html.append("<span data-mes-sin").append(sinResultado == 0 ? " hidden" : "").append(">")
                    .append(sinResultado).append(" sin resultado</span></span><span class=\"mes-ver\">Ver mes</span>")
                    .append("</summary><div class=\"table-scroll\"><table class=\"resp alineo-table consultas-table\">")
                    .append(FilasTrabajo.cabecera(SeccionConsulta.VISITAS, sesion.cuenta().rol())).append("<tbody>");
            var ordenadas = mes.getValue().stream().sorted(Comparator.comparing((Fila f) -> f.valor("fecha"))
                    .reversed()).toList();
            for (int i = 0; i < ordenadas.size(); i++) html.append(
                    FilasTrabajo.fila(sesion, ordenadas.get(i), SeccionConsulta.VISITAS, i < 5));
            int restantes = Math.max(0, ordenadas.size() - 5);
            html.append("</tbody></table></div><button type=\"button\" class=\"link mes-mas\" data-ver-mes")
                    .append(restantes == 0 ? " hidden" : "").append(">Ver las ").append(restantes)
                    .append(" visitas restantes de ").append(nombre).append("</button></details>");
            primero = false;
        }
        return html.append("</div>").toString();
    }

    /** RF-024: los niveles proceden del resultado máximo ya obtenido de MySQL. */
    static String riesgo(Fila f) {
        if (f.valor("puntaje").isBlank()) return "sin";
        int nivel = f.valor("nivel").isBlank() ? 0 : Integer.parseInt(f.valor("nivel"));
        if (nivel >= 3 || f.valor("color").equals("ROJO")) return "alto";
        return nivel == 2 || f.valor("color").equals("AMARILLO") ? "medio" : "bajo";
    }

    /** RF-024: indicadores de la lista autorizada, actualizables con los filtros locales. */
    static String conteos(List<Fila> filas) {
        String[][] datos = {{"curso", "En curso"}, {"mes", "Este mes"}, {"finalizadas", "Finalizadas"},
                {"alto", "Con riesgo alto"}};
        StringBuilder html = new StringBuilder("<div class=\"visitas-conteos\">");
        for (var dato : datos) {
            long n = filas.stream().filter(f -> switch (dato[0]) {
                case "curso" -> f.valor("estado").equals("EN_CURSO");
                case "mes" -> f.valor("fecha").startsWith(Html.hoy().toString().substring(0, 7));
                case "finalizadas" -> f.valor("estado").equals("FINALIZADA");
                default -> riesgo(f).equals("alto");
            }).count();
            html.append("<div><span>").append(dato[1]).append("</span><strong data-visitas-conteo=\"")
                    .append(dato[0]).append("\">").append(n).append("</strong></div>");
        }
        return html.append("</div>").toString();
    }
}
