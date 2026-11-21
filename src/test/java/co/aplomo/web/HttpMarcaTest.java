package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.DatosDemoPrueba;
import co.aplomo.soporte.HttpPrueba;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Identidad visual sin cambiar navegación, permisos ni resultados del motor.
 */
class HttpMarcaTest extends HttpPrueba {
    @Test
    void paginasDeProductoConservanPieYNoMuestranEtiquetasDelProyecto() throws Exception {
        var login = get("/login");
        assertTrue(login.body().contains(">Bienvenido</span>"));
        assertFalse(login.body().contains("class=\"pie\""));
        var ingreso = entrar(admin);
        assertEquals("/", ingreso.headers().firstValue("Location").orElseThrow());
        for (String ruta : List.of("/", "/empresas", "/sedes", "/salones", "/areas", "/cargos", "/puestos", "/reservas",
                "/visitas", "/evaluaciones", "/pendientes", "/sugerencias", "/usuarios", "/calificaciones",
                "/empresas/nueva", "/reservas/nueva", "/usuarios/nuevo", "/cuenta/contrasena")) {
            var respuesta = get(ruta);
            assertEquals(200, respuesta.statusCode(), ruta);
            String texto = respuesta.body();
            assertTrue(texto.contains("© 2026 ALINEO"), ruta);
            assertTrue(texto.contains("Ergonomía con precisión · Método RULA"), ruta);
            assertFalse(texto.toLowerCase(Locale.ROOT).contains("proyecto nuclear"), ruta);
            assertFalse(texto.toLowerCase(Locale.ROOT).contains("software real"), ruta);
            assertFalse(texto.contains("v1.0"), ruta);
        }
        assertFalse(login.body().contains("Proyecto Nuclear 2026-2"));
    }

    @Test
    void inicioOmiteAtrasSinQuitarElBotonDelRestoNiCrearBienvenida() throws Exception {
        entrar(admin);
        var inicio = get("/");
        assertFalse(inicio.body().contains("aria-label=\"Atrás\""));
        assertTrue(inicio.body().contains("Hola, "));
        assertTrue(inicio.body().contains("© 2026 ALINEO"));
        assertFalse(get("/usuarios").body().contains("aria-label=\"Atrás\""));
        cliente = java.net.http.HttpClient.newBuilder()
                .cookieHandler(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertFalse(get("/").body().contains("aria-label=\"Atrás\""));
        assertFalse(get("/").body().contains("Semáforo de puestos"));
    }

    @Test
    void semaforoLeeColoresRealesYSeActualizaConLosDatos() throws Exception {
        DatosDemoPrueba.cargar(con, carpetaFotos);
        entrar(admin);
        String texto = get("/").body();
        var cantidades = conteos();
        for (var entrada : cantidades.entrySet()) conteo(texto, entrada.getKey(), entrada.getValue());
        assertTrue(texto.contains("class=\"barra\""));
        int puesto = Sql.uno(con, "SELECT e.id_puesto FROM evaluacion e JOIN visita v USING(id_visita) JOIN res" +
                        "erva r USING(id_reserva) WHERE r.codigo=? ORDER BY e.id_puesto LIMIT 1",
                fila -> fila.getInt(1),
                "DEMO17-E3B");
        Sql.actualizar(con, "UPDATE puesto_trabajo SET ultimo_puntaje=? WHERE id_puesto=?", 3, puesto);
        texto = get("/").body();
        conteo(texto, "VERDE", cantidades.get("VERDE") - 1);
        conteo(texto, "AMARILLO", cantidades.get("AMARILLO") + 1);
        conteo(texto, "ROJO", cantidades.get("ROJO"));
        assertTrue(texto.indexOf("Semáforo de puestos") < texto.indexOf("id=\"visitas-en-curso\""));
    }

    @Test
    void ceroPuestosNoMuestraUnaBarraProporcionalVacia() throws Exception {
        entrar(admin);
        String texto = get("/").body();
        for (String color : List.of("VERDE", "AMARILLO", "ROJO")) conteo(texto, color, 0);
        assertFalse(texto.contains("class=\"barra\""));
    }

    @Test
    void panelesSoloEnLosTresFormulariosNuevosConCamposYAccionesIntactos() throws Exception {
        entrar(admin);
        String[] rutas = {"/empresas/nueva", "/usuarios/nuevo"};
        String[] lemas = {"Cada puesto, medido con precisión.", "Cada empresa, solo lo suyo."};
        for (int i = 0; i < rutas.length; i++) {
            String texto = get(rutas[i]).body();
            assertTrue(texto.contains("class=\"con-panel\""));
            assertTrue(texto.contains("<aside class=\"panel-marca\""));
            assertTrue(texto.contains(lemas[i]));
            assertTrue(texto.contains("Consejo RULA"));
            assertTrue(texto.contains("name=\"csrf\""));
        }
        assertFalse(get("/usuarios/" + usuario + "/editar").body().contains("class=\"panel-marca\""));
        assertFalse(get("/empresas/" + empresa + "/editar").body().contains("class=\"panel-marca\""));
    }

    @Test
    void vaciosConservanTextoBotonesYDecoracionesSonSoloPresentacion() throws Exception {
        entrar(admin);
        String texto = get("/").body();
        assertTrue(texto.contains("class=\"empty marca\""));
        assertTrue(texto.contains("Ir a Reservas"));
        assertTrue(texto.contains("class=\"riel riel-izq\" aria-hidden=\"true\""));
        assertTrue(texto.contains("viewBox=\"0 0 300 300\""));
        assertTrue(texto.contains("viewBox=\"0 0 300 380\""));
        assertFalse(texto.contains("class=\"fondo\""));
        assertTrue(get("/usuarios").body().contains("class=\"empty marca\" data-usuarios-vacios hidden"));
        assertTrue(get("/empresas?q=no-existe-qa-marca").body().contains("<p>Aún no hay empresas con estos filtros."));
    }

    private Map<String, Integer> conteos() throws Exception {
        Map<String, Integer> parametros = new HashMap<>();
        for (var parametros2 : Sql.listar(con,
                "SELECT n.color,COUNT(*) total FROM puesto_trabajo p JOIN salon l USING(id_sa" +
                        "lon) JOIN sede s USING(id_sede) JOIN nivel_riesgo n ON p.ultimo_puntaje BETW" +
                        "EEN n.puntaje_minimo AND n.puntaje_maximo WHERE TRUE" +
                        co.aplomo.modelo.Demostracion.filtro("s.id_empresa") + " GROUP BY n.color",
                fila -> Map.entry(fila.getString(1), fila.getInt(2))))
            parametros.put(parametros2.getKey(), parametros2.getValue());
        return parametros;
    }

    private static void conteo(String html, String color, int cantidad) {
        assertTrue(html.contains("data-color=\"" + color + "\" data-cantidad=\"" + cantidad + "\""),
                color + "=" + cantidad);
    }

    @org.junit.jupiter.api.Test
    void marcaDeLoginEnPanelYHeroSinTexto() throws Exception {
        String texto = get("/login").body(), hero = texto.substring(texto.indexOf("<section class=\"hero\""),
                texto.indexOf("<!-- Lado derecho"));
        assertFalse(hero.contains("class=\"in\""));
        assertFalse(hero.contains("class=\"lockup\""));
        assertTrue(texto.indexOf("class=\"login-marca\"") > texto.indexOf("id=\"contenido\""));
        assertFalse(texto.contains("Evaluación ergonómica de puestos de trabajo con el método RULA:"));
        assertTrue(texto.contains("src=\"/static/js/portada.js\""));
        var script = get("/static/js/portada.js");
        assertEquals(200, script.statusCode());
        assertTrue(script.headers().firstValue("Content-Type").orElseThrow().contains("javascript"));
    }
}
