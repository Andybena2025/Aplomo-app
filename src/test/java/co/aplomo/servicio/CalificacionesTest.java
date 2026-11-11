package co.aplomo.servicio;

import co.aplomo.db.Sql;
import co.aplomo.modelo.InformeEmpresa;
import co.aplomo.modelo.InformeEmpresa.*;
import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-025/026: promedios por nivel, cobertura, historial, privacidad y lectura sin modificaciones. */
class CalificacionesTest extends BasePrueba {
    EscenarioInformes e;Calificaciones servicio;
    @BeforeEach void escenario()throws Exception{e=new EscenarioInformes(this);servicio=e.calificaciones;}
    @Test void promediaVisitasPorSedeYSedesSinPonderarPorCantidadDePuestos()throws Exception{
        var i=servicio.consultar(cuenta(admin),empresa);var centro=sede(i,e.sedeCentro);
        assertEquals(new BigDecimal("6.0"),centro.visitas().get(0).calificacion().promedio());
        assertEquals(new BigDecimal("3.0"),centro.visitas().get(1).calificacion().promedio());
        assertEquals(0,new BigDecimal("4.5").compareTo(centro.calificacion().promedio()));
        assertEquals(5,centro.calificacion().redondeado());assertEquals(3,centro.calificacion().riesgo().nivel());
        assertEquals(0,new BigDecimal("3.75").compareTo(i.general().promedio()));assertEquals(4,i.general().redondeado());assertEquals(2,i.general().riesgo().nivel());
        assertEquals(List.of("Sede Norte"),i.sedesSinCalificacion());assertEquals(3,i.sedes().size());
    }
    @Test void reevaluacionVerdeSustituyeRojoHistoricoYEnCursoNoInterviene()throws Exception{
        var centro=sede(servicio.consultar(cuenta(usuario),empresa),e.sedeCentro);
        assertEquals(2,centro.puestos().size());var p=centro.puestos().stream().filter(x->x.id()==e.puesto1).findFirst().orElseThrow();
        assertEquals(e.evalActual,p.evaluacion());assertEquals(2,p.puntaje());assertEquals("VERDE",p.riesgo().color());
        assertEquals(4,centro.estadisticas().fotosAnalizadas());assertEquals(1,centro.estadisticas().colores().get("VERDE"));assertEquals(1,centro.estadisticas().colores().get("AMARILLO"));assertEquals(0,centro.estadisticas().colores().getOrDefault("ROJO",0));
        assertEquals(2,centro.visitas().size());assertFalse(centro.visitas().stream().anyMatch(v->v.id()==e.visitaAbierta));
    }
    @Test void cuentaPendientesPorEstadoYSugerenciasActivasPorTipoYEstado()throws Exception{
        var s=sede(servicio.informe(cuenta(admin),empresa),e.sedeCentro);var es=s.estadisticas();
        assertEquals(Map.of("ABIERTO",1,"ARREGLADO",1),es.pendientes());assertEquals(2,s.pendientes().size());
        assertEquals(3,s.sugerencias().size());assertEquals(Map.of("HECHA",1),es.sugerencias().get("MOBILIARIO"));
        assertEquals(Map.of("EN_PROCESO",1,"PENDIENTE",1),es.sugerencias().get("POSTURA"));assertFalse(es.sugerencias().containsKey("OTRA"));
        assertEquals(Map.of("Producción",2),es.areas());assertEquals(Map.of("OPERATIVO",2),es.cargos());assertTrue(es.areasMasRojo().isEmpty());
    }
    @Test void unaSolaSedeNoTieneGeneral()throws Exception{var i=servicio.informe(cuenta(admin),e.empresaSola);assertEquals(1,i.sedes().size());assertNotNull(i.sedes().get(0).calificacion());assertNull(i.general());}
    @ParameterizedTest @ValueSource(ints={0,1}) void menosDeDosSedesCalificadasNoGeneraGeneral(int calificadas)throws Exception{
        Sql.actualizar(con,"UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l ON l.id_salon=r.id_salon SET v.calificacion_final=NULL WHERE l.id_sede=?",e.sedeSur);
        if(calificadas==0)Sql.actualizar(con,"UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l ON l.id_salon=r.id_salon SET v.calificacion_final=NULL WHERE l.id_sede=?",e.sedeCentro);
        var i=servicio.consultar(cuenta(admin),empresa);assertNull(i.general());assertEquals(3-calificadas,i.sedesSinCalificacion().size());
    }
    @Test void sedeSinDatosNoSeCuentaComoCeroNiOcultaSedesInactivas()throws Exception{
        Sql.actualizar(con,"UPDATE sede SET activo=FALSE WHERE id_sede=?",e.sedeNorte);var i=servicio.consultar(cuenta(admin),empresa);
        assertEquals(3,i.sedes().size());assertNull(sede(i,e.sedeNorte).calificacion());assertEquals(0,i.general().promedio().compareTo(new BigDecimal("3.75")));
    }
    @Test void empresaSinVisitasNoGeneraInformeYConsultarNoEscribe()throws Exception{
        int antes=Sql.uno(con,"SELECT COUNT(*) FROM informe_empresa",r->r.getInt(1));int bitacora=Sql.uno(con,"SELECT COUNT(*) FROM bitacora",r->r.getInt(1));
        assertThrows(IllegalArgumentException.class,()->servicio.informe(cuenta(admin),e.empresaVacia));
        servicio.consultar(cuenta(usuario),empresa);servicio.informe(cuenta(admin),empresa);
        assertEquals(antes,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM informe_empresa",r->r.getInt(1)));assertEquals(bitacora,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora",r->r.getInt(1)));
    }
    @Test void usuarioSoloSuEmpresaNuncaGeneraInformeYCuentaVigente()throws Exception{
        assertThrows(SecurityException.class,()->servicio.consultar(cuenta(usuario),e.empresaAjena));
        assertThrows(SecurityException.class,()->servicio.informe(cuenta(usuario),empresa));
        var actor=cuenta(usuario);Sql.actualizar(con,"UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?",usuario);
        assertThrows(SecurityException.class,()->servicio.consultar(actor,empresa));
    }
    @ParameterizedTest @CsvSource({"2.4,2.5,2.5,2.475,2,1","2.5,2.5,2.5,2.5,3,2","4.4,4.5,4.5,4.475,4,2","4.5,4.5,4.5,4.5,5,3","6.4,6.5,6.5,6.475,6,3","6.5,6.5,6.5,6.5,7,4"})
    void colorUsaPromedioRealSinRedondearAntesLasSedes(String anterior,String reevaluacion,String sur,String esperado,int puntaje,int nivel)throws Exception{
        // Promedios históricos de una décima; el promedio de sedes conserva precisión antes del color.
        Sql.actualizar(con,"UPDATE visita SET calificacion_final=? WHERE id_visita=?",new BigDecimal(anterior),e.visitaAnterior);
        Sql.actualizar(con,"UPDATE visita SET calificacion_final=? WHERE id_visita=?",new BigDecimal(reevaluacion),e.visitaReevaluacion);
        Sql.actualizar(con,"UPDATE visita SET calificacion_final=? WHERE id_visita=?",new BigDecimal(sur),e.visitaSur);
        var i=servicio.consultar(cuenta(admin),empresa);assertEquals(0,new BigDecimal(esperado).compareTo(i.general().promedio()));
        assertEquals(puntaje,i.general().redondeado());assertEquals(nivel,i.general().riesgo().nivel());
    }
    @Test void ultimaEvaluacionSinAnalisisNoRecuperaUnRojoAnteriorYFotoSinAnalisisNoSeCuenta()throws Exception{
        int ultima=evaluacion(e.visitaReevaluacion,e.salonCentro,1);foto(ultima);
        var centro=sede(servicio.consultar(cuenta(admin),empresa),e.sedeCentro);
        var p=centro.puestos().stream().filter(x->x.id()==e.puesto1).findFirst().orElseThrow();
        assertEquals(ultima,p.evaluacion());assertNull(p.puntaje());assertNull(p.riesgo());assertEquals(4,centro.estadisticas().fotosAnalizadas());
    }
    private SedeResumen sede(InformeEmpresa i,int id){return i.sedes().stream().filter(s->s.sede().id()==id).findFirst().orElseThrow();}
}
