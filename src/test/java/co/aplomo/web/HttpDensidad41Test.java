package co.aplomo.web;

import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** RNF-007/008: todas las vistas comunes y el ingreso cargan la capa compacta después del estilo base. */
class HttpDensidad41Test extends HttpPrueba {
    @Test
    void densidadEstaEnIngresoPaginasPublicasYTodasLasVistasComunes() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        for (String ruta : new String[]{"/login", "/solicitar-acceso", "/solicitar-evaluador", "/ayuda"})
            comprobar(get(ruta).body());
        entrar(admin);
        for (String ruta : new String[]{"/", "/empresas", "/visitas", "/reservas", "/usuarios", "/archivo",
                "/evaluaciones/" + escenario.evalAbierta}) {
            var respuesta = get(ruta);
            assertEquals(200, respuesta.statusCode(), ruta);
            comprobar(respuesta.body());
        }
        var css = get("/static/css/densidad.css");
        assertEquals(200, css.statusCode());
        assertTrue(css.headers().firstValue("Content-Type").orElse("").contains("text/css"));
        assertTrue(css.body().contains("min-width:900px"));
        assertFalse(css.body().matches("(?s).*\\bzoom\\s*:.*"));
    }

    /** RNF-008: respeta el orden de las hojas sin introducir estilos en páginas sueltas. */
    private static void comprobar(String html) {
        int base = html.indexOf("/static/css/aplomo.css");
        int compacta = html.indexOf("/static/css/densidad.css");
        assertTrue(base >= 0 && compacta > base);
        int portada = html.indexOf("/static/css/portada-orden.css");
        assertTrue(portada < 0 || compacta > portada);
    }
}
