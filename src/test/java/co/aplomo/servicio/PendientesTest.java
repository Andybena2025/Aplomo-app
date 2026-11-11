package co.aplomo.servicio;

import co.aplomo.dao.*;import co.aplomo.db.Sql;import co.aplomo.modelo.*;import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;import java.sql.*;import java.time.*;import java.util.*;import java.io.*;import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** RF-022: generación por color real, historial, máximo aprobado, alcance y reversión completa. */
class PendientesTest extends BasePrueba {
    @TempDir Path carpeta;Evaluaciones evals;Pendientes pendientes;int visita,puesto,eval,foto;
    @BeforeEach void contexto()throws Exception{
        evals=new Evaluaciones(tx,reloj,new Fotos(carpeta),new BitacoraDao());pendientes=evals.pendientes();var salon=crearSalon();puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Ensamble","ESCRITORIO","Centro",null,true)).id();
        var r=new Reservas(tx,reloj);int reserva=r.guardar(cuenta(admin),0,solicitud(salon));r.aceptar(cuenta(admin),reserva);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),reserva);
        eval=evals.guardar(cuenta(admin),visita,datos(),List.of(imagen()));foto=new EvaluacionDao().fotos(con,eval).get(0).id();
    }
    private Evaluaciones.Datos datos(){return new Evaluaciones.Datos(puesto,"Ensamble","MAS_INADECUADA","Postura mantenida",60,false,false,false,false,false,false,"",true,false);}
    private Fotos.Entrada imagen()throws Exception{var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(12,12,1),"png",b);return new Fotos.Entrada(b.toByteArray(),"PERFIL","Perfil de prueba");}
    private void analizar(int caso)throws Exception{evals.analisis().guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,CasosRulaPrueba.todos().get(caso-1).entrada())));}
    private int id()throws Exception{return new PendienteDao().porEvaluacion(con,eval).id();}
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6,7,8,9,10})
    void soloLosResultadosRojosCreanPendiente(int caso)throws Exception{
        analizar(caso);var p=new PendienteDao().porEvaluacion(con,eval);boolean rojo=caso==7||caso==8||caso==10;assertEquals(rojo,p!=null);
        if(rojo){assertEquals(CasosRulaPrueba.todos().get(caso-1).esperado(),p.puntaje());assertNull(p.descripcion());assertNull(p.prioridad());assertNull(p.limite());assertEquals("ABIERTO",p.estado());}
    }
    @Test void maximoConservaDatosFechaYArregladoYNoDuplica()throws Exception{
        analizar(7);int id=id();LocalDate registro=pendientes.consultar(cuenta(admin),id).pendiente().registro();String descripcion="Ajustar la mesa\nSin borrar historia";
        pendientes.guardar(cuenta(admin),id,descripcion,"ALTA","2026-11-25");assertTrue(pendientes.arreglar(cuenta(admin),id));reloj.avanzar(Duration.ofDays(2));analizar(8);analizar(7);analizar(8);
        var p=pendientes.consultar(cuenta(admin),id).pendiente();assertEquals(7,p.puntaje());assertEquals(descripcion,p.descripcion());assertEquals("ALTA",p.prioridad());assertEquals(LocalDate.of(2026,11,25),p.limite());assertEquals(registro,p.registro());assertEquals("ARREGLADO",p.estado());
        assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM pendiente_arreglo WHERE id_evaluacion=?",r->r.getInt(1),eval));assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora WHERE tabla='pendiente_arreglo' AND id_registro=? AND accion='CREAR'",r->r.getInt(1),id));
    }
    @Test void otraEvaluacionGeneraOtroPendienteYUnVerdeNoBorraLosAnteriores()throws Exception{
        analizar(8);int primero=id();int segundo=evals.guardar(cuenta(admin),visita,datos(),List.of(imagen()),List.of(new Analisis.Solicitud(0,CasosRulaPrueba.todos().get(7).entrada())));int otro=new PendienteDao().porEvaluacion(con,segundo).id();
        pendientes.arreglar(cuenta(admin),primero);assertEquals("ABIERTO",pendientes.consultar(cuenta(admin),otro).pendiente().estado());
        evals.guardar(cuenta(admin),visita,datos(),List.of(imagen()),List.of(new Analisis.Solicitud(0,CasosRulaPrueba.todos().get(3).entrada())));
        assertNotNull(pendientes.consultar(cuenta(admin),primero));assertNotNull(pendientes.consultar(cuenta(admin),otro));assertEquals(1,Sql.<Integer>uno(con,"SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",r->r.getInt(1),puesto));
    }
    @Test void usuarioSoloConsultaSuEmpresaYVisitasFinalizadas()throws Exception{
        analizar(8);int id=id();dao.contrasena(con,usuario,cuenta(usuario).hash(),false);assertTrue(pendientes.listar(cuenta(usuario),"TODOS","prioridad").isEmpty());assertThrows(SecurityException.class,()->pendientes.consultar(cuenta(usuario),id));
        new Visitas(tx,reloj).finalizar(cuenta(admin),visita,"");assertEquals(1,pendientes.listar(cuenta(usuario),"TODOS","fecha").size());assertEquals(id,pendientes.consultar(cuenta(usuario),id).pendiente().id());
        assertThrows(SecurityException.class,()->pendientes.guardar(cuenta(usuario),id,"Cambiar","BAJA",""));assertThrows(SecurityException.class,()->pendientes.arreglar(cuenta(usuario),id));
        int empresaB=Sql.insertar(con,"INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)",admin,"Otra empresa",UUID.randomUUID().toString().substring(0,20));int usuarioB=insertar("USUARIO",empresaB,cuenta(admin).hash(),false);
        assertTrue(pendientes.listar(cuenta(usuarioB),"TODOS","fecha").isEmpty());assertThrows(SecurityException.class,()->pendientes.consultar(cuenta(usuarioB),id));
    }
    @Test void seguimientoOpcionalLimitesYAuditoriaSinCambiarRula()throws Exception{
        analizar(7);int id=id();pendientes.guardar(cuenta(admin),id,"á".repeat(255),"MEDIA","1999-01-01");assertEquals(255,pendientes.consultar(cuenta(admin),id).pendiente().descripcion().length());
        assertThrows(IllegalArgumentException.class,()->pendientes.guardar(cuenta(admin),id,"a".repeat(256),"MEDIA",""));
        pendientes.guardar(cuenta(admin),id,"","","");var p=pendientes.consultar(cuenta(admin),id).pendiente();assertNull(p.descripcion());assertNull(p.prioridad());assertNull(p.limite());assertTrue(pendientes.arreglar(cuenta(admin),id));assertFalse(pendientes.arreglar(cuenta(admin),id));
        assertEquals(6,Sql.<Integer>uno(con,"SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",r->r.getInt(1),puesto));var h=pendientes.historial(cuenta(admin),id);assertEquals(1,h.stream().filter(x->x.accion().equals("CAMBIAR_ESTADO")).count());String nombre=cuenta(admin).nombre();assertTrue(h.stream().allMatch(x->x.responsable().equals(nombre)));
    }
    @ParameterizedTest @ValueSource(strings={"prioridad","fecha","fuera"})
    void rechazaDatosInvalidosSinAlterarElSeguimiento(String caso)throws Exception{
        analizar(8);int id=id();assertThrows(IllegalArgumentException.class,()->pendientes.guardar(cuenta(admin),id,"Conservar",caso.equals("prioridad")?"URGENTE":"ALTA",caso.equals("fecha")?"invalida":caso.equals("fuera")?"0001-01-01":""));assertNull(pendientes.consultar(cuenta(admin),id).pendiente().descripcion());
        assertThrows(IllegalArgumentException.class,()->pendientes.listar(cuenta(admin),"TODOS","x; DROP TABLE"));
    }
    @Test void ordenarPorPrioridadRegistroYLimiteEsDeterminista()throws Exception{
        analizar(8);int uno=id();pendientes.guardar(cuenta(admin),uno,"","BAJA","2026-12-01");reloj.avanzar(Duration.ofDays(1));
        int segunda=evals.guardar(cuenta(admin),visita,datos(),List.of(imagen()),List.of(new Analisis.Solicitud(0,CasosRulaPrueba.todos().get(7).entrada())));int dos=new PendienteDao().porEvaluacion(con,segunda).id();pendientes.guardar(cuenta(admin),dos,"","ALTA","2026-11-20");
        assertEquals(dos,pendientes.listar(cuenta(admin),"ABIERTO","prioridad").get(0).pendiente().id());assertEquals(dos,pendientes.listar(cuenta(admin),"ABIERTO","limite").get(0).pendiente().id());assertEquals(dos,pendientes.listar(cuenta(admin),"ABIERTO","fecha").get(0).pendiente().id());
    }
    @Test void falloAlCrearPendienteRevierteEvaluacionAnalisisYFotoNuevos()throws Exception{
        var falla=new BitacoraDao(){@Override public void registrar(Connection c,int actor,LocalDateTime fecha,String tabla,int id,String accion,String a,String n,String detalle)throws SQLException{if(tabla.equals("pendiente_arreglo"))throw new SQLException("Fallo simulado");super.registrar(c,actor,fecha,tabla,id,accion,a,n,detalle);}};
        var e=new Evaluaciones(tx,reloj,new Fotos(carpeta),falla);assertThrows(SQLException.class,()->e.guardar(cuenta(admin),visita,datos(),List.of(imagen()),List.of(new Analisis.Solicitud(0,CasosRulaPrueba.todos().get(7).entrada()))));
        assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",r->r.getInt(1),visita));assertNull(new PendienteDao().porEvaluacion(con,eval));try(var fs=Files.list(carpeta)){assertEquals(1,fs.count());}
    }
}
