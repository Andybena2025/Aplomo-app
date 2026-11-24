package co.aplomo.web;

/** RF-024, RNF-007: presentación agrupada sobre filas ya autorizadas. */
final class VerPor {
    private VerPor() { }

    /** RF-024: botones de presentación; no cambian permisos ni parámetros de consulta. */
    static String mostrar(String defecto, String[][] opciones) {
        StringBuilder html = new StringBuilder("<div class=\"ver-por\"><span>Ver por</span>" +
                "<div role=\"group\" aria-label=\"Ver por\" class=\"ver-por-opciones\" data-ver-por-defecto=\"")
                .append(defecto).append("\">");
        for (var opcion : opciones) html.append("<button type=\"button\" data-ver-por=\"")
                .append(opcion[0]).append("\" aria-pressed=\"").append(opcion[0].equals(defecto))
                .append("\">").append(opcion[1]).append("</button>");
        return html.append("</div></div>").toString();
    }
}
