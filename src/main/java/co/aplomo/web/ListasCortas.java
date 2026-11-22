package co.aplomo.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RF-024, RNF-002: límite inicial también en HTML; los filtros conservan todos los registros autorizados.
 */
final class ListasCortas {
    private static final Pattern FILA = Pattern.compile("<tr\\b(?=[^>]*data-registro=)[^>]*>");

    private ListasCortas() {
    }

    static String preparar(String html) {
        Matcher tablas = Pattern.compile("<table\\b[\\s\\S]*?</table>").matcher(html);
        StringBuilder compactas = new StringBuilder();
        while (tablas.find()) tablas.appendReplacement(compactas,
                Matcher.quoteReplacement(tablas.group().replaceAll("class=\"btn(?! sm)", "class=\"btn sm")));
        tablas.appendTail(compactas);
        html = compactas.toString();
        if (!html.contains("data-lista")) return html;
        Matcher filas = FILA.matcher(html);
        StringBuilder resultado = new StringBuilder();
        int visibles = 0;
        while (filas.find()) {
            String fila = filas.group();
            if (!Pattern.compile("\\bhidden\\b").matcher(fila).find() && ++visibles > 10) {
                fila = fila.substring(0, fila.length() - 1) + " hidden>";
            }
            filas.appendReplacement(resultado, Matcher.quoteReplacement(fila));
        }
        filas.appendTail(resultado);
        String pagina = resultado.toString().replace("{{quedan}}", Integer.toString(Math.max(0, visibles - 10)));
        return visibles <= 10 ? pagina.replace("data-lista-mas>", "data-lista-mas hidden>") : pagina;
    }

    static String boton() {
        return "<div class=\"lista-mas\"><button type=\"button\" class=\"btn\" data-lista-mas>" +
                "Ver 10 más · quedan {{quedan}}</button></div>";
    }
}
