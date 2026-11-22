package co.aplomo.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RNF-002/007: traslada el encabezado ya escapado a la única franja común sin alterar formularios.
 */
final class CabeceraPagina {
    record Datos(String cuerpo, String rotulo, String titulo, String acciones, String etiqueta) {
    }

    private CabeceraPagina() {
    }

    static Datos extraer(String titulo, String contenido) {
        Matcher inicio = Pattern.compile("<div class=\"pagehead\"[^>]*>").matcher(contenido);
        if (!inicio.find()) return new Datos(contenido, "ALINEO.", Html.escapar(titulo), "", "");
        int fin = finalDiv(contenido, inicio.start());
        String bloque = contenido.substring(inicio.start(), fin);
        Matcher hijo = Pattern.compile("<div\\b[^>]*>").matcher(bloque);
        hijo.find();
        int interior = hijo.end();
        hijo.find();
        int finTitulo = finalDiv(bloque, hijo.start());
        String acciones = bloque.substring(finTitulo, bloque.lastIndexOf("</div>"));
        return new Datos(contenido.substring(0, inicio.start()) + contenido.substring(fin),
                grupo(bloque, "<span class=\"eyebrow\">([\\s\\S]*?)</span>", "ALINEO."),
                grupo(bloque, "<h1[^>]*>([\\s\\S]*?)</h1>", Html.escapar(titulo)), acciones,
                grupo(bloque.substring(0, interior), "data-cabecera-etiqueta=\"([^\"]*)\"", ""));
    }

    private static int finalDiv(String texto, int inicio) {
        Matcher etiquetas = Pattern.compile("<div\\b[^>]*>|</div>").matcher(texto);
        etiquetas.region(inicio, texto.length());
        int nivel = 0;
        while (etiquetas.find()) {
            nivel += etiquetas.group().startsWith("</") ? -1 : 1;
            if (nivel == 0) return etiquetas.end();
        }
        throw new IllegalArgumentException("No se pudo preparar el encabezado de la página.");
    }

    private static String grupo(String texto, String expresion, String respaldo) {
        Matcher campo = Pattern.compile(expresion).matcher(texto);
        return campo.find() ? campo.group(1) : respaldo;
    }
}
