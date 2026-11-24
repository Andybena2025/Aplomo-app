package co.aplomo.web;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-017 · RNF-007: las fichas con dibujo de cada ángulo (brazo, cuello, tronco...) se ven completas.
 * La regla global del Paso 13D-1 fija todos los botones en 48 px; las fichas son botones,
 * así que sin height:auto quedaban aplastadas y el texto desaparecía (recorrido del 24-nov).
 */
class FichasAngulosTest {

    /** Devuelve el contenido de aplomo.css tal como lo sirve la app. */
    private static String css() throws Exception {
        try (var entrada = FichasAngulosTest.class.getResourceAsStream("/static/css/aplomo.css")) {
            assertNotNull(entrada, "falta aplomo.css");
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Devuelve las declaraciones del primer bloque que empieza exactamente con ese selector. */
    private static String bloque(String css, String selector) {
        int inicio = css.indexOf("\n" + selector + "{");
        assertTrue(inicio >= 0, "no existe la regla " + selector);
        int abre = css.indexOf('{', inicio);
        return css.substring(abre + 1, css.indexOf('}', abre));
    }

    @Test
    void lasFichasDeOpcionCrecenConSuContenido() throws Exception {
        String css = css();
        String global = ":root body button,:root body .btn,:root body .btn.sm{height:48px";
        assertTrue(css.contains(global), "la regla global de botones sigue existiendo");
        String ficha = bloque(css, "form[data-conversacion] .campo-opcion");
        assertTrue(ficha.contains("height:auto"), "la ficha debe anular la altura fija de 48 px");
        assertTrue(ficha.contains("white-space:normal"), "el texto de la ficha puede partirse en líneas");
    }

    @Test
    void elDibujoDeLaFichaTieneAlturaVisibleEnLaRevision() throws Exception {
        String img = bloque(css(), ".modo-enfoque form.revision-grupos .campo-opcion img");
        assertTrue(img.contains("height:110px"), "el dibujo usa los 110 px previstos, no los 72 px de la lista");
        assertTrue(img.contains("object-fit:contain"), "el dibujo se ve entero, sin recortes");
    }
}
