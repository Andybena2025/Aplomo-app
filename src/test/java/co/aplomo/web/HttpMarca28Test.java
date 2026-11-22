package co.aplomo.web;

import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-002/011: geometría aprobada, recurso público y marca compartida sin tocar la foto del login.
 */
class HttpMarca28Test extends HttpPrueba {
    @Test
    void logoTieneLaAOriginalPlomadaYNoventaYDosMarcasGeneradas() {
        String logo = Marca.logo();
        assertTrue(logo.contains("viewBox=\"0 0 352 180\""));
        assertTrue(logo.contains("translate(-2.4,44) scale(0.9)"));
        assertTrue(logo.contains("M31.2,54.2 A48,48 0 0 0 68.8,54.2"));
        assertFalse(logo.contains("129.9"));
        assertEquals(92, Pattern.compile("style=\"--i:").matcher(logo).results().count());
        assertTrue(logo.contains("x1=\"326.9\""));
        assertTrue(Marca.icono().contains("viewBox=\"0 0 108 180\""));
        assertEquals(21, Pattern.compile("style=\"--i:").matcher(Marca.icono()).results().count());
        assertFalse(Marca.icono().contains("lg-texto"));
    }

    @Test
    void loginFaviconYRecursoUsanLaMarcaNuevaYConservanLaFoto() throws Exception {
        String login = get("/login").body();
        assertTrue(login.contains("marca-llegada"));
        assertFalse(login.contains("lg-iplomo"));
        assertTrue(login.contains("lg-ejes"));
        assertTrue(login.contains("id=\"pzOverlay\""));
        assertTrue(login.contains("x1=\"988\" y1=\"20\""));
        assertTrue(login.contains("mostrar-clave.js"));
        assertFalse(login.contains("{{logo}}"));
        assertTrue(get("/favicon.svg").body().contains("#E3CDA0"));
        assertEquals(200, get("/static/img/logo_alineo.svg").statusCode());
        entrar(evaluadorTrabajo().id());
        String inicio = get("/").body();
        assertTrue(inicio.contains("data-avisos-saludo"));
        assertTrue(inicio.contains("data-aviso-dialog"));
        assertFalse(inicio.contains("deco-izq"));
    }
}
