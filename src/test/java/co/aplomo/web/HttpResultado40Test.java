package co.aplomo.web;

import co.aplomo.dao.EvaluacionDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Trabajador;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-002/014/020/023: datos opcionales protegidos y plan enlazado a las sugerencias guardadas. */
class HttpResultado40Test extends HttpPrueba {
    EscenarioConsultas e;

    @BeforeEach
    void escenario() throws Exception { e = new EscenarioConsultas(this, carpetaFotos); entrar(admin); }

    @Test
    void previsualizacionIncluyeProyeccionServidorMetasYTrabajador() throws Exception {
        var f = new LinkedHashMap<>(CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(6).entrada()));
        f.put("csrf", csrf(get("/evaluaciones/nueva?visita=" + e.visitaAbierta + "&puesto=" + e.puesto1)));
        f.put("visita", "" + e.visitaAbierta); f.put("puesto", "" + e.puesto1);
        f.put("trabajador_genero", "FEMENINO"); f.put("trabajador_edad", "35"); f.put("minutos", "420");
        var respuesta = post("/evaluaciones/explicacion", f);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.body().contains("\"proyeccion\":"));
        assertTrue(respuesta.body().contains("Muñeca neutra, hasta 5°"));
        assertTrue(respuesta.body().contains("\"edad\":35"));
        assertTrue(respuesta.body().contains("\"anterior\":2"));
        f.put("trabajador_edad", "120");
        var error = post("/evaluaciones/explicacion", f);
        assertEquals(400, error.statusCode()); assertTrue(error.body().contains("14 y 99"));
        f.put("trabajador_edad", "35"); f.put("puesto", "" + e.puestoB);
        assertEquals(403, post("/evaluaciones/explicacion", f).statusCode());
    }

    @Test
    void usuarioNoRecibeContextoPorPaginaJsonNiInforme() throws Exception {
        Sql.actualizar(con, "UPDATE evaluacion SET trabajador_genero='OTRO',trabajador_edad=41," +
                "trabajador_antiguedad='MAS_10' WHERE id_evaluacion=?", e.evalVerde);
        ((java.net.CookieManager) cliente.cookieHandler().orElseThrow()).getCookieStore().removeAll();
        entrar(usuario);
        String detalle = get("/evaluaciones/" + e.evalVerde).body();
        assertTrue(detalle.contains("data-resultado40-json"));
        assertFalse(detalle.contains("\"trabajador\":"));
        assertEquals(403, get("/evaluaciones/" + e.evalAbierta).statusCode());
        assertEquals(403, get("/evaluaciones/" + e.evalB).statusCode());
        assertFalse(detalle.contains("trabajador_")); assertFalse(detalle.contains("MAS_10"));
        assertFalse(detalle.contains("41 años"));
        assertEquals(403, post("/evaluaciones/explicacion", Map.of()).statusCode());
        assertEquals(new Trabajador(null, null, null), new Trabajador("OTRO", 41, "MAS_10").para(cuenta(usuario)));
        String informe = get("/informes/empresa/" + e.empresaA).body();
        assertFalse(informe.contains("trabajador_")); assertFalse(informe.contains("41 años"));
    }

    @Test
    void contextoPersonalDistintoMantieneElMismoResultadoDeLaPrevisualizacion() throws Exception {
        var entrada = CasosRulaPrueba.todos().get(6).entrada();
        var primera = e.evaluaciones.resultados().previsualizar(cuenta(admin), e.visitaAbierta, e.puesto1,
                entrada, Map.of(), new Trabajador("FEMENINO", 14, "MENOS_1"), 60).datos();
        var segunda = e.evaluaciones.resultados().previsualizar(cuenta(admin), e.visitaAbierta, e.puesto1,
                entrada, Map.of(), new Trabajador("OTRO", 99, "MAS_10"), 540).datos();
        assertEquals(6, primera.get("puntaje"));
        assertEquals(primera.get("puntaje"), segunda.get("puntaje"));
        assertEquals(primera.get("proyeccion"), segunda.get("proyeccion"));
        assertNotEquals(primera.get("trabajador"), segunda.get("trabajador"));
    }

    @Test
    void reevaluacionPrellenaElPuestoYRechazaUnoAjeno() throws Exception {
        var evaluador = evaluadorTrabajo(e.empresaA);
        ((java.net.CookieManager) cliente.cookieHandler().orElseThrow()).getCookieStore().removeAll();
        entrar(evaluador.id());
        var formulario = get("/reservas/nueva?puesto=" + e.puesto1);
        assertEquals(200, formulario.statusCode(), formulario.body());
        assertTrue(formulario.body().contains("Reevaluación del Puesto 1"));
        assertTrue(formulario.body().contains("REEVALUACION\" selected"));
        assertEquals(403, get("/reservas/nueva?puesto=" + e.puestoB).statusCode());
    }

    @Test
    void guardarOpcionalesUsaFilasRealesYNoCambiaAnalisis() throws Exception {
        var original = new EvaluacionDao().consultar(con, e.evalNuevaAbierta).datos();
        var datos = new Evaluaciones.Datos(original.puesto(), original.postura(), original.criterio(),
                original.motivo(), original.minutos(), false, false, false, false, false, false, "", true, true,
                new Trabajador("NO_DICE", 25, "DE_1_A_5"));
        int id = e.evaluaciones.guardar(cuenta(admin), e.visitaAbierta, datos, List.of());
        assertEquals(datos.trabajador(), new EvaluacionDao().consultar(con, id).datos().trabajador());
        var vistas = e.evaluaciones.resultados().guardados(cuenta(admin), e.evalNuevaAbierta);
        assertFalse(vistas.isEmpty());
        var sugerencias = e.evaluaciones.sugerencias().listar(cuenta(admin), e.evalNuevaAbierta);
        assertTrue(sugerencias.stream().anyMatch(s -> s.sugerencia().titulo().equals("Plan · Piernas")));
        int mejora = sugerencias.stream().filter(s -> s.sugerencia().titulo().equals("Plan · Piernas"))
                .findFirst().orElseThrow().sugerencia().id();
        e.evaluaciones.sugerencias().estado(cuenta(admin), mejora, "HECHA");
        e.evaluaciones.sugerencias().guardar(cuenta(admin), e.evalNuevaAbierta, mejora,
                "Apoyo corregido", "Texto editado del apoyo", "POSTURA", "ALTA", null);
        int antes = e.evaluaciones.sugerencias().listar(cuenta(admin), e.evalNuevaAbierta).size();
        int foto = new EvaluacionDao().fotos(con, e.evalNuevaAbierta).get(0).id();
        e.evaluaciones.analisis().guardar(cuenta(admin), e.evalNuevaAbierta,
                List.of(new co.aplomo.servicio.Analisis.Solicitud(foto, CasosRulaPrueba.todos().get(9).entrada())));
        assertEquals(antes, e.evaluaciones.sugerencias().listar(cuenta(admin), e.evalNuevaAbierta).size());
        String pagina = get("/evaluaciones/" + e.evalNuevaAbierta).body();
        assertTrue(pagina.contains("data-resultado40-json"));
        assertTrue(pagina.contains("HECHA"));
        assertTrue(pagina.contains("Texto editado del apoyo"));
        assertFalse(pagina.contains("Qué hacer primero"));
    }
}
