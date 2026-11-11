package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-025/026: permisos por dirección, contenido completo y vista de impresión nativa. */
class HttpInformesTest extends HttpPrueba {
    EscenarioInformes e;
    @BeforeEach void escenario()throws Exception{e=new EscenarioInformes(this);entrar(admin);}
    @Test void administradorGeneraInformeConCoberturaEstadisticasYNivelesEscritos()throws Exception{
        var r=get("/informes/empresa/"+empresa);assertEquals(200,r.statusCode(),r.body());String h=r.body();
        for(String t:List.of("Imprimir o guardar PDF","2 de 3 sedes calificadas","falta Sede Norte","3,75","4 · Amarillo · Nivel 2","Fotos analizadas","Pendientes abiertos","Pendientes arreglados","Puestos por área","Puestos por tipo de cargo","Sugerencias activas por tipo y estado","Motivo de elección","Pies colgando: sí","America/Bogota"))assertTrue(h.contains(t),t);
        assertFalse(h.contains("/fotos/"));assertFalse(h.contains("En curso invisible"));assertFalse(h.contains("Sugerencia quitada invisible"));assertFalse(h.contains("Empresa ajena para informe"));
        assertTrue(h.contains("/static/js/informe.js"));assertFalse(h.contains("html2canvas"));assertFalse(h.contains("jspdf"));
    }
    @Test void empresaConUnaSedeExplicaQueNoHayGeneral()throws Exception{var r=get("/informes/empresa/"+e.empresaSola);assertEquals(200,r.statusCode());assertTrue(r.body().contains("No hay calificación general: la empresa tiene una sola sede."));}
    @Test void empresaSinVisitasExplicaQueFaltaFinalizar()throws Exception{var r=get("/informes/empresa/"+e.empresaVacia);assertEquals(400,r.statusCode());assertTrue(r.body().contains("Finaliza al menos una visita"));}
    @Test void usuarioConsultaSoloSusCalificacionesYNoGeneraInformesPorDireccion()throws Exception{
        salir();entrar(usuario);var r=get("/calificaciones?empresa="+empresa);assertEquals(200,r.statusCode(),r.body());assertFalse(r.body().contains("Generar informe"));assertFalse(r.body().contains("/fotos/"));
        assertEquals(403,get("/calificaciones?empresa="+e.empresaAjena).statusCode());assertEquals(403,get("/informes/empresa/"+empresa).statusCode());assertEquals(403,get("/informes/empresa/"+e.empresaAjena).statusCode());
        var detalle=get("/empresas/"+empresa);assertTrue(detalle.body().contains("Ver calificaciones"));assertFalse(detalle.body().contains("Generar informe"));
    }
    @Test void usuarioSinParametroVeSuEmpresaYAdministradorElige()throws Exception{
        assertTrue(get("/calificaciones").body().contains("Ver calificaciones"));salir();entrar(usuario);var r=get("/calificaciones");assertEquals(200,r.statusCode());assertTrue(r.body().contains("2 de 3 sedes calificadas"));assertFalse(r.body().contains("Empresa ajena para informe"));
    }
    @Test void empresaYObservacionesSeEscapanEnElInforme()throws Exception{
        Sql.actualizar(con,"UPDATE empresa SET descripcion=? WHERE id_empresa=?","<script>secreto</script>",empresa);
        var r=get("/informes/empresa/"+empresa);assertTrue(r.body().contains("&lt;script&gt;secreto&lt;/script&gt;"));assertFalse(r.body().contains("<script>secreto"));
    }
    @Test void metodosInvalidosIdsYAccesoSinSesionProtegidos()throws Exception{
        assertEquals(400,get("/calificaciones?empresa=abc").statusCode());assertEquals(404,get("/informes/empresa/incorrecto").statusCode());
        String token=csrf(get("/empresas/"+empresa));assertEquals(405,post("/informes/empresa/"+empresa,Map.of("csrf",token)).statusCode());
        assertEquals(403,post("/informes/empresa/"+empresa,Map.of()).statusCode());
        post("/logout",Map.of("csrf",token));assertEquals(303,get("/informes/empresa/"+empresa).statusCode());
    }
    @Test void variasSedesPeroSoloUnaCalificadaMuestraFaltantesSinGeneral()throws Exception{
        Sql.actualizar(con,"UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l ON l.id_salon=r.id_salon SET v.calificacion_final=NULL WHERE l.id_sede=?",e.sedeSur);
        var r=get("/informes/empresa/"+empresa);assertEquals(200,r.statusCode());assertTrue(r.body().contains("Sin calificación general."));assertTrue(r.body().contains("1 de 3 sedes calificadas"));assertTrue(r.body().contains("Sede Norte, Sede Sur"));
    }
    private void salir()throws Exception{post("/logout",Map.of("csrf",csrf(get("/empresas/"+empresa))));}

}
