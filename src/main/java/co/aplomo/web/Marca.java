package co.aplomo.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * RNF-002/011: identidad IV aprobada por Andy; una sola fuente SVG y marcas generadas en Java.
 */
public final class Marca {
    private static final String PLANTILLA = cargar();
    private static final String COMPLETO = PLANTILLA.replace("<!-- marcas -->", marcas(327));
    private static final String MONOGRAMA = monograma();

    private Marca() {
    }

    /**
     * RNF-002: logo horizontal completo en línea, con 92 marcas graduadas.
     */
    public static String logo() {
        return COMPLETO;
    }

    /**
     * RNF-002: la A original con base corta para cabecera móvil, riel y favicon.
     */
    public static String icono() {
        return MONOGRAMA;
    }

    /**
     * RNF-002: solo oscila la plomada de la A, no el cuerpo del monograma.
     */
    public static String lateral() {
        String inicio = "<line x1=\"50\" y1=\"10\" x2=\"50\" y2=\"64\"";
        String fin = "<circle cx=\"50\" cy=\"10\"";
        return MONOGRAMA.replace("viewBox=\"0 0 108 180\"", "viewBox=\"0 43.02 90 100\"")
                .replace(inicio, "<g class=\"lg-a-plomo\">" + inicio).replace(fin, "</g>" + fin);
    }

    private static String monograma() {
        int desde = PLANTILLA.indexOf("<g class=\"lg-a\"");
        int hasta = PLANTILLA.indexOf("</g>", desde) + 4;
        return "<svg class=\"logo-monograma\" viewBox=\"0 0 108 180\" role=\"img\" aria-label=\"ALINEO.\" " +
                "xmlns=\"http://www.w3.org/2000/svg\">" + PLANTILLA.substring(desde, hasta) +
                "<g class=\"lg-regla\" stroke=\"currentColor\"><line x1=\"8.4\" y1=\"133\" x2=\"78.6\" " +
                "y2=\"133\" stroke-width=\"1\"/>" + marcas(78.6) + "</g></svg>";
    }

    private static String marcas(double fin) {
        StringBuilder svg = new StringBuilder();
        for (int numero = 0; 8.4 + numero * 3.5 <= fin; numero++) {
            double x = 8.4 + numero * 3.5;
            double y = numero % 10 == 0 ? 141 : numero % 5 == 0 ? 138.5 : 136;
            String opacidad = numero % 5 == 0 ? ".95" : ".6";
            svg.append(String.format(Locale.ROOT,
                    "<line x1=\"%.1f\" y1=\"133\" x2=\"%.1f\" y2=\"%.1f\" stroke-width=\".8\" " +
                            "opacity=\"%s\" style=\"--i:%d\"/>", x, x, y, opacidad, numero));
        }
        return svg.toString();
    }

    private static String cargar() {
        try (var entrada = Marca.class.getResourceAsStream("/static/img/logo_alineo.svg")) {
            if (entrada == null) throw new IllegalStateException("Falta el logo de ALINEO.");
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo abrir el logo de ALINEO.", error);
        }
    }
}
