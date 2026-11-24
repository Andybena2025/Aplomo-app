package co.aplomo.web;

import co.aplomo.soporte.HttpPrueba;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Retroalimentación de la docente (nov 2026): títulos con una fuente técnica y "Quiénes somos" más corto.
 */
class HttpTipografiaTest extends HttpPrueba {
    @Test
    void quienesSomosMuestraSoloLosTresTitulosSinTextoDebajo() throws Exception {
        for (String ruta : new String[]{"/login", "/nosotros"}) {
            String html = get(ruta).body();
            String ideas = html.substring(html.indexOf("login-ideas"));
            ideas = ideas.substring(0, ideas.indexOf("</section>"));
            assertTrue(ideas.contains("Fotografía el puesto"), ruta);
            assertTrue(ideas.contains("Mide los ángulos"), ruta);
            assertTrue(ideas.contains("Conoce el riesgo"), ruta);
            assertFalse(ideas.contains("<small>"), ruta);
            assertFalse(html.contains("Una foto por postura"), ruta);
            assertFalse(html.contains("Guiado, sobre la foto"), ruta);
            assertFalse(html.contains("Semáforo y sugerencias"), ruta);
        }
    }

    @Test
    void titulosUsanBarlowPropiaYLaMarcaConservaSuLetra() throws Exception {
        String css = get("/static/css/aplomo.css").body();
        assertTrue(css.contains("font-family:'Barlow'"));
        assertTrue(css.contains("h1,h2,h3{font-family:var(--display)"));
        assertTrue(css.contains(".lockup{display:inline-flex;align-items:flex-end;font-family:var(--serif)"));
        assertFalse(css.contains("'Instrument Serif',serif"));
        assertFalse(css.contains("\"Instrument Serif\",serif"));
        assertFalse(css.contains("'Instrument Serif', serif"));
    }

    @Test
    void laFuenteSeSirveDesdeElServidorSinInternet() throws Exception {
        for (int peso : new int[]{400, 500}) {
            var respuesta = cliente.send(HttpRequest.newBuilder(URI.create(raiz +
                    "/static/fonts/barlow-latin-" + peso + "-normal.woff2")).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, respuesta.statusCode());
            assertEquals("font/woff2", respuesta.headers().firstValue("Content-Type").orElse(""));
            assertEquals("wOF2", new String(respuesta.body(), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        }
        assertEquals(404, get("/static/fonts/../css/aplomo.css.woff2").statusCode());
    }
}
