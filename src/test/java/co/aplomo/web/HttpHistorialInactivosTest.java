package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.*;
import co.aplomo.servicio.*;
import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

/** RNF-009, RF-028/024/020 y RF-002: historial inactivo por HTTP, sin modificar estados al consultar. */
class HttpHistorialInactivosTest extends HttpPrueba {
    EscenarioConsultas e;Empresas empresas;
    @BeforeEach void escenario()throws Exception{e=new EscenarioConsultas(this,carpetaFotos);empresas=new Empresas(tx);entrar(admin);}
    private String evaluaciones(String registros)throws Exception{return get("/evaluaciones?empresa="+e.empresaA+"&registros="+registros).body();}
    private static int filas(String html){return (int)Pattern.compile("<tr data-registro=").matcher(html).results().count();}
    private static String fila(String h,int id){var m=Pattern.compile("<tr data-registro=\""+id+"\">(.*?)</tr>",Pattern.DOTALL).matcher(h);assertTrue(m.find());return m.group(1);}
    private void usuario()throws Exception{cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);}
    @Test void empresaInactivaConservaEvaluacionesFiltrosAvisoYElInforme()throws Exception{
        empresas.estadoEmpresa(cuenta(admin),e.empresaA,false);
        var detalle=get("/empresas/"+e.empresaA);assertEquals(200,detalle.statusCode());
        assertTrue(detalle.body().contains("Esta empresa está desactivada. Su historial se conserva y se puede consultar aquí."));
        assertTrue(detalle.body().contains("Ver evaluaciones de la empresa"));assertTrue(detalle.body().contains("href=\"/evaluaciones?empresa="+e.empresaA+"\""));
        assertEquals(4,filas(evaluaciones("TODOS")));assertEquals(4,filas(evaluaciones("INACTIVOS")));assertEquals(0,filas(evaluaciones("ACTIVOS")));
        String h=evaluaciones("TODOS");assertTrue(fila(h,e.evalVerde).contains("<span class=\"tag\">Inactiva</span>"));assertTrue(h.contains("(inactiva)"));
        var calificacion=get("/calificaciones?empresa="+e.empresaA);assertEquals(200,calificacion.statusCode());assertTrue(calificacion.body().contains("Promedio histórico de sus visitas Finalizadas"));
        var informe=get("/informes/empresa/"+e.empresaA);assertEquals(200,informe.statusCode());assertTrue(informe.body().contains("Imprimir o guardar PDF"));assertTrue(informe.body().contains("Visita Finalizada"));
        assertFalse(empresas.empresa(cuenta(admin),e.empresaA).activo());
    }
    @ParameterizedTest @ValueSource(strings={"evaluaciones","puestos","pendientes","sugerencias","visitas"})
    void cincoListasIncluyenInactivosYRegistrosSoloEsAdministrativo(String lista)throws Exception{
        empresas.estadoEmpresa(cuenta(admin),e.empresaA,false);
        String base="/"+lista+"?empresa="+e.empresaA+"&estado=TODOS";var todos=get(base);assertEquals(200,todos.statusCode());assertTrue(filas(todos.body())>0);
        assertTrue(todos.body().contains("name=\"registros\""));assertTrue(todos.body().contains("Solo activos"));assertTrue(todos.body().contains("Solo inactivos"));
        assertTrue(todos.body().contains("<span class=\"tag\">Inactiva</span>"));assertEquals(filas(todos.body()),filas(get(base+"&registros=INACTIVOS").body()));assertEquals(0,filas(get(base+"&registros=ACTIVOS").body()));
        assertEquals(400,get(base+"&registros=valor-invalido").statusCode());assertEquals("TODOS",e.consultas.listar(cuenta(admin),SeccionConsulta.deRuta("/"+lista),Map.of()).filtros().get("registros"));
    }
    @ParameterizedTest @ValueSource(strings={"empresa","sede","salon","puesto"})
    void cualquierNivelInactivoMarcaLaFilaSinDesactivarLosDemas(String nivel)throws Exception{
        switch(nivel){case "empresa"->empresas.estadoEmpresa(cuenta(admin),e.empresaA,false);case "sede"->empresas.estadoSede(cuenta(admin),e.sedeA,false);case "salon"->empresas.estadoSalon(cuenta(admin),e.salonA,false);case "puesto"->empresas.estadoPuesto(cuenta(admin),e.puesto1,false);}
        String q="/evaluaciones?puesto="+e.puesto1;assertEquals(2,filas(get(q+"&registros=INACTIVOS").body()));assertEquals(0,filas(get(q+"&registros=ACTIVOS").body()));
        String h=get(q).body();assertTrue(fila(h,e.evalVerde).contains(nivel.equals("empresa")||nivel.equals("sede")?">Inactiva</span>":">Inactivo</span>"));
        assertTrue(empresas.puesto(cuenta(admin),e.puesto3).activo());
    }
    @ParameterizedTest @ValueSource(strings={"sede","salon"})
    void detallesDeSedeYSalonInactivosLlevanASusEvaluaciones(String tipo)throws Exception{
        int id=tipo.equals("sede")?e.sedeA:e.salonA;
        if(tipo.equals("sede"))empresas.estadoSede(cuenta(admin),id,false);else empresas.estadoSalon(cuenta(admin),id,false);
        var r=get("/"+(tipo.equals("sede")?"sedes":"salones")+"/"+id);assertEquals(200,r.statusCode());assertTrue(r.body().contains(tipo.equals("sede")?"Esta sede está desactivada.":"Este salón está desactivado."));assertTrue(r.body().contains("href=\"/evaluaciones?"+tipo+"="+id+"\""));
    }
    @Test void historialDelPuestoMuestraTodasEnOrdenEstadosYResultadosSinReactivar()throws Exception{
        empresas.estadoPuesto(cuenta(admin),e.puesto1,false);empresas.estadoSede(cuenta(admin),e.sedeA,false);
        int antes=Sql.uno(con,"SELECT COUNT(*) FROM bitacora",r->r.getInt(1));
        var r=get("/puestos/"+e.puesto1);String h=r.body();assertEquals(200,r.statusCode());assertTrue(h.contains("Historial del puesto"));assertTrue(h.contains("Inactivo"));assertTrue(h.contains("Inactiva"));
        assertTrue(h.contains("Producción y ensamble"));assertTrue(h.contains("Operario de precisión"));assertTrue(h.contains("2 · Verde · Nivel 1"));assertTrue(h.contains("7 · Rojo · Nivel 4"));assertTrue(h.contains("En curso"));assertTrue(h.contains("Finalizada"));
        assertTrue(h.indexOf("data-evaluacion=\""+e.evalAbierta+"\"")<h.indexOf("data-evaluacion=\""+e.evalVerde+"\""));
        assertEquals(2,Pattern.compile("data-evaluacion=").matcher(h).results().count());assertEquals(200,get("/evaluaciones/"+e.evalVerde).statusCode());
        assertFalse(empresas.puesto(cuenta(admin),e.puesto1).activo());assertFalse(empresas.sede(cuenta(admin),e.sedeA).activo());assertEquals(antes,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora",rs->rs.getInt(1)));
        assertTrue(get("/salones/"+e.salonA).body().contains("href=\"/puestos/"+e.puesto1+"\">Ver historial"));
    }
    @Test void historialDelMismoDiaOrdenaPorElRegistroMasReciente()throws Exception{
        Sql.actualizar(con,"UPDATE evaluacion SET fecha=(SELECT fecha FROM (SELECT fecha FROM evaluacion WHERE id_evaluacion=?) x) WHERE id_evaluacion=?",e.evalVerde,e.evalAbierta);
        String h=get("/puestos/"+e.puesto1).body();assertTrue(h.indexOf("data-evaluacion=\""+e.evalAbierta+"\"")<h.indexOf("data-evaluacion=\""+e.evalVerde+"\""));
    }
    @Test void puestoSinEvaluacionesTieneVacioClaroYElHtmlSeEscapa()throws Exception{
        int id=empresas.guardar(cuenta(admin),new Puesto(0,e.salonA,null,null,9,"<b>Puesto de prueba</b>","MESA","Esquina",null,true)).id();
        String h=get("/puestos/"+id).body();assertTrue(h.contains("Este puesto todavía no tiene evaluaciones."));assertTrue(h.contains("&lt;b&gt;Puesto de prueba&lt;/b&gt;"));assertFalse(h.contains("<b>Puesto de prueba</b>"));assertTrue(h.contains("Sin área"));assertTrue(h.contains("Sin cargo"));
    }
    @Test void catalogosIncluyenLosCuatroInactivosYContextoDeSugerenciasLosSenala()throws Exception{
        empresas.estadoEmpresa(cuenta(admin),e.empresaA,false);empresas.estadoSede(cuenta(admin),e.sedeA,false);empresas.estadoSalon(cuenta(admin),e.salonA,false);empresas.estadoPuesto(cuenta(admin),e.puesto1,false);
        var p=e.consultas.listar(cuenta(admin),SeccionConsulta.EVALUACIONES,Map.of());
        for(String k:List.of("empresa","sede","salon","puesto")){int id=switch(k){case "empresa"->e.empresaA;case "sede"->e.sedeA;case "salon"->e.salonA;default->e.puesto1;};String nombre=p.catalogos().get(k).stream().filter(o->o.id()==id).findFirst().orElseThrow().nombre();assertTrue(nombre.contains(k.equals("empresa")||k.equals("sede")?"(inactiva)":"(inactivo)"));}
        String h=get("/sugerencias?evaluacion="+e.evalVerde+"&registros=ACTIVOS").body();assertTrue(h.contains("<span class=\"tag\">Inactiva</span>"));assertFalse(h.contains("aria-label=\"Ordenar por Puesto\""));
    }
    @Test void usuarioConservaAlcanceYNoVeFiltrosNiAvisosAdministrativos()throws Exception{
        empresas.estadoEmpresa(cuenta(admin),e.empresaA,false);usuario();
        String h=get("/evaluaciones").body();assertFalse(h.contains("name=\"registros\""));assertFalse(h.contains("(inactiva)"));assertTrue(h.contains("data-registro=\""+e.evalVerde+"\""));assertFalse(h.contains("data-registro=\""+e.evalAbierta+"\""));
        assertEquals(403,get("/evaluaciones?registros=INACTIVOS").statusCode());assertEquals(403,get("/evaluaciones?puesto="+e.puestoB).statusCode());assertEquals(403,get("/puestos/"+e.puestoB).statusCode());assertEquals(403,get("/evaluaciones/"+e.evalAbierta).statusCode());
        var propio=get("/puestos/"+e.puesto1);assertEquals(303,propio.statusCode());assertEquals("/salones/"+e.salonA,propio.headers().firstValue("Location").orElseThrow());assertFalse(get("/empresas/"+e.empresaA).body().contains("Su historial se conserva y se puede consultar aquí."));
    }
    @Test void atrasSoloEnDetallesYFormularios()throws Exception{
        for(String ruta:List.of("/","/empresas","/usuarios","/reservas","/visitas","/pendientes","/evaluaciones"))assertFalse(get(ruta).body().contains("aria-label=\"Atrás\""),ruta);for(String ruta:List.of("/puestos/"+e.puesto1,"/cuenta/contrasena","/empresas/"+e.empresaA))assertTrue(get(ruta).body().contains("aria-label=\"Atrás\""),ruta);
        dao.contrasena(con,admin,cuenta(admin).hash(),true);cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(admin);
        String h=get("/cuenta/contrasena").body();assertFalse(h.contains("aria-label=\"Atrás\""));assertTrue(h.contains(">Salir</button>"));
    }
    @Test void botonAtrasInternoDeEvaluacionSeConserva()throws Exception{
        String h=get("/evaluaciones/nueva?visita="+e.visitaAbierta+"&puesto="+e.puesto2).body();assertTrue(h.contains("data-atras-evaluacion>← Atrás</button>"));
    }
}
