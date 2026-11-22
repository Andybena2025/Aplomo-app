package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.servicio.Empresas;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-009, RF-028/024/020 y RF-002: historial inactivo por HTTP, sin modificar estados al consultar.
 */
class HttpHistorialInactivosTest extends HttpPrueba {
    EscenarioConsultas escenario;
    Empresas empresas;

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioConsultas(this, carpetaFotos);
        empresas = new Empresas(tx);
        entrar(admin);
    }

    private String evaluaciones(String registros) throws Exception {
        return get("/evaluaciones?empresa=" + escenario.empresaA + "&registros=" + registros).body();
    }

    private static int filas(String html) {
        return (int) Pattern.compile("<tr\\b(?![^>]*\\bhidden)(?=[^>]*\\bdata-registro=)[^>]*>").matcher(html).results()
                .count();
    }

    private static String fila(String texto, int id) {
        var elemento = Pattern.compile("<tr\\b(?=[^>]*data-registro=\"" + id + "\")[^>]*>(.*?)</tr>", Pattern.DOTALL)
                .matcher(texto);
        assertTrue(elemento.find());
        return elemento.group(1);
    }

    private void usuario() throws Exception {
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
    }

    @Test
    void empresaInactivaConservaEvaluacionesFiltrosAvisoYElInforme() throws Exception {
        empresas.estadoEmpresa(cuenta(admin), escenario.empresaA, false);
        var detalle = get("/empresas/" + escenario.empresaA);
        assertEquals(200, detalle.statusCode());
        assertTrue(detalle.body()
                .contains("Esta empresa está desactivada. Su historial se conserva y se puede consultar aquí."));
        assertTrue(detalle.body().contains("Ver evaluaciones de la empresa"));
        assertTrue(detalle.body().contains("href=\"/evaluaciones?empresa=" + escenario.empresaA + "\""));
        assertEquals(4, filas(evaluaciones("TODOS")));
        assertEquals(4, filas(evaluaciones("INACTIVOS")));
        assertEquals(0, filas(evaluaciones("ACTIVOS")));
        String texto = evaluaciones("TODOS");
        assertTrue(fila(texto, escenario.evalVerde).contains("Inactiva"));
        assertTrue(texto.contains("(inactiva)"));
        var calificacion = get("/calificaciones?empresa=" + escenario.empresaA);
        assertEquals(200, calificacion.statusCode());
        assertTrue(calificacion.body().contains("Promedio histórico de sus visitas Finalizadas"));
        var informe = get("/informes/empresa/" + escenario.empresaA);
        assertEquals(200, informe.statusCode());
        assertTrue(informe.body().contains("Imprimir o guardar PDF"));
        assertTrue(informe.body().contains("Visita Finalizada"));
        assertFalse(empresas.empresa(cuenta(admin), escenario.empresaA).activo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"evaluaciones", "puestos", "pendientes", "sugerencias", "visitas"})
    void cincoListasIncluyenInactivosYRegistrosSoloEsAdministrativo(String lista) throws Exception {
        empresas.estadoEmpresa(cuenta(admin), escenario.empresaA, false);
        String base = "/" + lista + "?empresa=" + escenario.empresaA + "&estado=TODOS";
        var todos = get(base);
        assertEquals(200, todos.statusCode());
        assertTrue(filas(todos.body()) > 0);
        assertTrue(todos.body().contains("name=\"registros\""));
        assertTrue(todos.body().contains("Solo activos"));
        assertTrue(todos.body().contains("Solo inactivos"));
        assertTrue(todos.body().contains("Inactiva"));
        assertEquals(filas(todos.body()), filas(get(base + "&registros=INACTIVOS").body()));
        assertEquals(0, filas(get(base + "&registros=ACTIVOS").body()));
        assertEquals(400, get(base + "&registros=valor-invalido").statusCode());
        assertEquals("TODOS",
                escenario.consultas.listar(cuenta(admin), SeccionConsulta.deRuta("/" + lista), Map.of()).filtros()
                        .get("registros"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"empresa", "sede", "salon", "puesto"})
    void cualquierNivelInactivoMarcaLaFilaSinDesactivarLosDemas(String nivel) throws Exception {
        switch (nivel) {
            case "empresa" -> empresas.estadoEmpresa(cuenta(admin), escenario.empresaA, false);
            case "sede" -> empresas.estadoSede(cuenta(admin), escenario.sedeA, false);
            case "salon" -> empresas.estadoSalon(cuenta(admin), escenario.salonA, false);
            case "puesto" -> empresas.estadoPuesto(cuenta(admin), escenario.puesto1, false);
        }
        String texto = "/evaluaciones?puesto=" + escenario.puesto1;
        assertEquals(2, filas(get(texto + "&registros=INACTIVOS").body()));
        assertEquals(0, filas(get(texto + "&registros=ACTIVOS").body()));
        String texto2 = get(texto).body();
        assertTrue(fila(texto2, escenario.evalVerde).contains(
                nivel.equals("empresa") || nivel.equals("sede") ? "Inactiva</span>" : "Inactivo</span>"));
        assertTrue(empresas.puesto(cuenta(admin), escenario.puesto3).activo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sede", "salon"})
    void detallesDeSedeYSalonInactivosLlevanASusEvaluaciones(String tipo) throws Exception {
        int id = tipo.equals("sede") ? escenario.sedeA : escenario.salonA;
        if (tipo.equals("sede")) empresas.estadoSede(cuenta(admin), id, false);
        else empresas.estadoSalon(cuenta(admin), id, false);
        var respuesta = get("/" + (tipo.equals("sede") ? "sedes" : "salones") + "/" + id);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body()
                .contains(tipo.equals("sede") ? "Esta sede está desactivada." : "Este salón está desactivado."));
        assertTrue(respuesta.body().contains("href=\"/evaluaciones?" + tipo + "=" + id + "\""));
    }

    @Test
    void historialDelPuestoMuestraTodasEnOrdenEstadosYResultadosSinReactivar() throws Exception {
        empresas.estadoPuesto(cuenta(admin), escenario.puesto1, false);
        empresas.estadoSede(cuenta(admin), escenario.sedeA, false);
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM bitacora", fila2 -> fila2.getInt(1));
        var respuesta = get("/puestos/" + escenario.puesto1);
        String texto = respuesta.body();
        assertEquals(200, respuesta.statusCode());
        assertTrue(texto.contains("Historial del puesto"));
        assertTrue(texto.contains("Inactivo"));
        assertTrue(texto.contains("Inactiva"));
        assertTrue(texto.contains("Producción y ensamble"));
        assertTrue(texto.contains("Operario de precisión"));
        assertTrue(texto.contains("2 · Verde · Nivel 1"));
        assertTrue(texto.contains("7 · Rojo · Nivel 4"));
        assertTrue(texto.contains("En curso"));
        assertTrue(texto.contains("Finalizada"));
        assertTrue(texto.indexOf("data-evaluacion=\"" + escenario.evalAbierta + "\"") <
                texto.indexOf("data-evaluacion=\"" + escenario.evalVerde + "\""));
        assertEquals(2, Pattern.compile("data-evaluacion=").matcher(texto).results().count());
        assertEquals(200, get("/evaluaciones/" + escenario.evalVerde).statusCode());
        assertFalse(empresas.puesto(cuenta(admin), escenario.puesto1).activo());
        assertFalse(empresas.sede(cuenta(admin), escenario.sedeA).activo());
        assertEquals(antes, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM bitacora", rs -> rs.getInt(1)));
        assertTrue(get("/salones/" + escenario.salonA).body()
                .contains("href=\"/puestos/" + escenario.puesto1 + "\">Ver historial"));
    }

    @Test
    void historialDelMismoDiaOrdenaPorElRegistroMasReciente() throws Exception {
        Sql.actualizar(con, "UPDATE evaluacion SET fecha=(SELECT fecha FROM (SELECT fecha FROM evaluacion" +
                " WHERE id_evaluacion=?) x) WHERE id_evaluacion=?", escenario.evalVerde, escenario.evalAbierta);
        String texto = get("/puestos/" + escenario.puesto1).body();
        assertTrue(texto.indexOf("data-evaluacion=\"" + escenario.evalAbierta + "\"") <
                texto.indexOf("data-evaluacion=\"" + escenario.evalVerde + "\""));
    }

    @Test
    void puestoSinEvaluacionesTieneVacioClaroYElHtmlSeEscapa() throws Exception {
        int id = empresas.guardar(cuenta(admin),
                new Puesto(0, escenario.salonA, null, null, 9, "<b>Puesto de prueba</b>", "MESA", "Esquina", null,
                        true)).id();
        String texto = get("/puestos/" + id).body();
        assertTrue(texto.contains("Este puesto todavía no tiene evaluaciones."));
        assertTrue(texto.contains("&lt;b&gt;Puesto de prueba&lt;/b&gt;"));
        assertFalse(texto.contains("<b>Puesto de prueba</b>"));
        assertTrue(texto.contains("Sin área"));
        assertTrue(texto.contains("Sin cargo"));
    }

    @Test
    void catalogosIncluyenLosCuatroInactivosYContextoDeSugerenciasLosSenala() throws Exception {
        empresas.estadoEmpresa(cuenta(admin), escenario.empresaA, false);
        empresas.estadoSede(cuenta(admin), escenario.sedeA, false);
        empresas.estadoSalon(cuenta(admin), escenario.salonA, false);
        empresas.estadoPuesto(cuenta(admin), escenario.puesto1, false);
        var elemento = escenario.consultas.listar(cuenta(admin), SeccionConsulta.EVALUACIONES, Map.of());
        for (String k : List.of("empresa", "sede", "salon", "puesto")) {
            int id = switch (k) {
                case "empresa" -> escenario.empresaA;
                case "sede" -> escenario.sedeA;
                case "salon" -> escenario.salonA;
                default -> escenario.puesto1;
            };
            String nombre = elemento.catalogos().get(k).stream().filter(elemento2 -> elemento2.id() == id).findFirst()
                    .orElseThrow().nombre();
            assertTrue(nombre.contains(k.equals("empresa") || k.equals("sede") ? "(inactiva)" : "(inactivo)"));
        }
        String texto = get("/sugerencias?evaluacion=" + escenario.evalVerde + "&registros=ACTIVOS").body();
        assertTrue(texto.contains("Inactiva"));
        assertFalse(texto.contains("aria-label=\"Ordenar por Puesto\""));
    }

    @Test
    void usuarioConservaAlcanceYNoVeFiltrosNiAvisosAdministrativos() throws Exception {
        empresas.estadoEmpresa(cuenta(admin), escenario.empresaA, false);
        usuario();
        String texto = get("/evaluaciones").body();
        assertFalse(texto.contains("name=\"registros\""));
        assertFalse(texto.contains("(inactiva)"));
        assertTrue(texto.contains("data-registro=\"" + escenario.evalVerde + "\""));
        assertFalse(texto.contains("data-registro=\"" + escenario.evalAbierta + "\""));
        assertEquals(403, get("/evaluaciones?registros=INACTIVOS").statusCode());
        assertEquals(403, get("/evaluaciones?puesto=" + escenario.puestoB).statusCode());
        assertEquals(403, get("/puestos/" + escenario.puestoB).statusCode());
        assertEquals(403, get("/evaluaciones/" + escenario.evalAbierta).statusCode());
        var propio = get("/puestos/" + escenario.puesto1);
        assertEquals(200, propio.statusCode());
        assertFalse(propio.body().contains("/evaluaciones/" + escenario.evalAbierta + "\""));
        assertFalse(get("/empresas/" + escenario.empresaA).body()
                .contains("Su historial se conserva y se puede consultar aquí."));
    }

    @Test
    void atrasSoloEnDetallesYFormularios() throws Exception {
        for (String ruta : List.of("/", "/empresas", "/usuarios", "/reservas", "/visitas", "/pendientes",
                "/evaluaciones"))
            assertFalse(get(ruta).body().contains("aria-label=\"Atrás\""), ruta);
        assertTrue(get("/cuenta/contrasena").body().contains("aria-label=\"Atrás\""));
        for (String ruta : List.of("/puestos/" + escenario.puesto1, "/empresas/" + escenario.empresaA)) {
            String detalle = get(ruta).body();
            assertFalse(detalle.contains("aria-label=\"Atrás\""), ruta);
            assertTrue(detalle.contains("class=\"detalle-volver\""), ruta);
        }
        Sql.actualizar(con, "UPDATE empresa SET nombre=? WHERE id_empresa=?", "Empresas", escenario.empresaA);
        assertTrue(get("/empresas/" + escenario.empresaA).body().contains("class=\"detalle-volver\""));
        dao.contrasena(con, admin, cuenta(admin).hash(), true);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(admin);
        String texto = get("/cuenta/contrasena").body();
        assertFalse(texto.contains("aria-label=\"Atrás\""));
        assertTrue(texto.contains(">Salir</button>"));
    }

    @Test
    void botonAtrasInternoDeEvaluacionSeConserva() throws Exception {
        String texto = get(
                "/evaluaciones/nueva?visita=" + escenario.visitaAbierta + "&puesto=" + escenario.puesto2).body();
        assertTrue(texto.contains("data-atras-evaluacion>← Atrás</button>"));
    }
}
