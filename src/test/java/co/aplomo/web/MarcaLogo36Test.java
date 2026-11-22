package co.aplomo.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-002: logo final del Paso 36 (I limpia, ángulo en la L, ejes X y Y, trazo de la O) y su secuencia.
 */
class MarcaLogo36Test {
    @Test
    void logoNoTienePlomadaEnLaIYTraeLosCuatroGruposPunteados() {
        String logo = Marca.logo();
        assertFalse(logo.contains("lg-iplomo"));
        for (String grupo : List.of("lg-sec-a", "lg-angulo", "lg-ejes", "lg-trazo")) {
            assertTrue(logo.contains(grupo), grupo);
        }
        assertTrue(logo.contains("M31.2,54.2 A48,48 0 0 0 68.8,54.2"), "la A original sigue igual");
        assertEquals(92, Pattern.compile("style=\"--i:").matcher(logo).results().count());
    }

    @Test
    void secuenciaEmpiezaEnLaAYTerminaEnElArcoDeLaO() {
        String logo = Marca.logo();
        int a = maximo(seccion(logo, "lg-sec-a"));
        int l = minimo(seccion(logo, "lg-angulo"));
        int ejes = minimo(seccion(logo, "lg-ejes"));
        int trazo = minimo(seccion(logo, "lg-trazo"));
        assertEquals(0, minimo(seccion(logo, "lg-sec-a")));
        assertTrue(a < l && l < ejes && ejes < trazo, "orden A, L, ejes, O");
        assertTrue(maximo(logo) <= 3100, "la secuencia dura unos 3 segundos");
    }

    @Test
    void monogramaNoLlevaLosTrazosNuevos() {
        String icono = Marca.icono();
        for (String grupo : List.of("lg-sec", "lg-angulo", "lg-ejes", "lg-trazo")) {
            assertFalse(icono.contains(grupo), grupo);
        }
        assertEquals(21, Pattern.compile("style=\"--i:").matcher(icono).results().count());
    }

    private static String seccion(String svg, String clase) {
        int desde = svg.indexOf("class=\"lg-sec " + clase);
        if (desde < 0) desde = svg.indexOf("class=\"lg-sec lg-sec-a");
        int hasta = svg.indexOf("<g class=\"lg-sec ", desde + 10);
        if (hasta < 0) hasta = svg.indexOf("<g class=\"lg-texto", desde);
        return svg.substring(desde, hasta);
    }

    private static int minimo(String texto) {
        return tiempos(texto).stream().min(Integer::compare).orElseThrow();
    }

    private static int maximo(String texto) {
        return tiempos(texto).stream().max(Integer::compare).orElseThrow();
    }

    private static List<Integer> tiempos(String texto) {
        Matcher m = Pattern.compile("--t:(\\d+)").matcher(texto);
        return m.results().map(r -> Integer.parseInt(r.group(1))).toList();
    }
}
