package co.aplomo.web;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RNF-002, RNF-004: componentes HTML escapados para los formularios del sistema.
 */
public final class Formularios {
    private Formularios() {
    }

    /**
     * Valor y texto de una opción; sus datos nunca se insertan como HTML sin escapar. RNF-011.
     */
    public record Opcion(String valor, String texto) {
    }

    /**
     * Genera un campo obligatorio u opcional con la etiqueta enlazada al control. RNF-011.
     */
    public static String campo(Map<String, String> datos, String nombre, String etiqueta, String tipo, int maximo,
            boolean requerido) {
        String html = Html.campo(nombre, etiqueta, tipo, datos.get(nombre), maximo);
        return requerido ? html : html.replace(" required", "");
    }

    /**
     * Genera un área de texto y conserva el contenido cuando falla una validación. RNF-011.
     */
    public static String texto(Map<String, String> datos, String nombre, String etiqueta, int maximo,
            boolean requerido) {
        return "<div class=\"field\"><label for=\"" + Html.escapar(nombre) + "\">" + Html.escapar(etiqueta) +
                "</label><textarea class=\"input\" id=\"" + Html.escapar(nombre) + "\" name=\"" + Html.escapar(nombre) +
                "\" maxlength=\"" + maximo + "\"" + (requerido ? " required" : "") + ">" +
                Html.escapar(datos.get(nombre)) + "</textarea></div>";
    }

    /**
     * Genera un selector a partir del catálogo permitido por el servidor. RNF-011.
     */
    public static String selector(Map<String, String> datos, String nombre, String etiqueta, List<Opcion> opciones,
            boolean requerido) {
        StringBuilder html = new StringBuilder("<div class=\"field\"><label for=\"").append(Html.escapar(nombre))
                .append("\">").append(Html.escapar(etiqueta)).append("</label><select class=\"input\" id=\"")
                .append(Html.escapar(nombre)).append("\" name=\"").append(Html.escapar(nombre)).append("\"")
                .append(requerido ? " required" : "").append(">");
        for (Opcion opcion : opciones) {
            html.append("<option value=\"").append(Html.escapar(opcion.valor())).append("\"")
                    .append(opcion.valor().equals(datos.getOrDefault(nombre, "")) ? " selected" : "").append(">")
                    .append(Html.escapar(opcion.texto())).append("</option>");
        }
        return html.append("</select></div>").toString();
    }

    /**
     * Crea las opciones para un enum aprobado y convierte sus etiquetas a español legible. RNF-011.
     */
    public static List<Opcion> opciones(String... valores) {
        return Arrays.stream(valores).map(texto2 -> new Opcion(texto2, etiqueta(texto2))).toList();
    }

    /**
     * Muestra una opción interna con mayúscula inicial y espacios. RNF-011.
     */
    public static String etiqueta(String valor) {
        String texto = valor.toLowerCase(Locale.ROOT).replace('_', ' ');
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
