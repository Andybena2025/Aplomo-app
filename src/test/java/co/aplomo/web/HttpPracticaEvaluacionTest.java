package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.servicio.*;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;
import java.net.http.HttpResponse;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Práctica permanente con HTTP real y datos revertidos solo en la base de pruebas. */
class HttpPracticaEvaluacionTest extends HttpPrueba {
 int cantidad()throws Exception{return Sql.uno(con,"SELECT COUNT(*) FROM empresa WHERE nit=?",r->r.getInt(1),PracticaEvaluacion.NIT);}
 HttpResponse<String> abrir(String tipo)throws Exception{return post("/demo/evaluacion",Map.of("csrf",csrf(get("/demo/evaluacion")),"tipo",tipo));}
 @Test void inicioSiempreOfrecePracticaYSuConsultaNoCreaDatos()throws Exception{
  entrar(admin);int antes=cantidad();assertTrue(get("/").body().contains("Abrir evaluación de prueba"));String h=get("/demo/evaluacion").body();assertTrue(h.contains("Probar Planta"));assertTrue(h.contains("Probar Oficina"));assertTrue(h.contains("base actual"));assertEquals(antes,cantidad());
 }
 @Test void preparaVisitaYAmbosPuestosYAbreLaEvaluacionReal()throws Exception{
  entrar(admin);var r=abrir("PLANTA");assertEquals(303,r.statusCode());String ruta=r.headers().firstValue("Location").orElseThrow();assertTrue(ruta.matches("/evaluaciones/nueva\\?visita=\\d+&puesto=\\d+"));String h=get(ruta).body();assertTrue(h.contains("Evaluación de práctica"));assertTrue(h.contains("data-conversacion=\"si\""));assertTrue(h.contains("data-modo>Planta"));assertEquals(1,cantidad());assertEquals(7,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora WHERE id_cuenta=? AND fecha_hora=? AND detalle LIKE ?",x->x.getInt(1),admin,java.time.LocalDateTime.now(reloj),"%práctica%"));
  assertEquals(List.of("ESCRITORIO","MESA"),Sql.listar(con,"SELECT p.tipo FROM puesto_trabajo p JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) WHERE e.nit=? ORDER BY p.numero",x->x.getString(1),PracticaEvaluacion.NIT));
 }
 @Test void seRepiteTrasEvaluarSinDuplicarVisitaNiBorrarElIntento()throws Exception{
  var practica=new Visitas(tx,reloj).practica();var a=practica.abrir(cuenta(admin),"OFICINA");
  new Evaluaciones(tx,reloj,new Fotos(carpetaFotos),new co.aplomo.dao.BitacoraDao()).guardar(cuenta(admin),a.visita(),new Evaluaciones.Datos(a.puesto(),"Práctica","MAS_INADECUADA","",180,false,false,false,false,false,false,"",true,true),List.of());
  entrar(admin);var r=abrir("OFICINA");assertEquals("/evaluaciones/nueva?visita="+a.visita()+"&puesto="+a.puesto(),r.headers().firstValue("Location").orElseThrow());assertTrue(get("/").body().contains("Abrir evaluación de prueba"));assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",x->x.getInt(1),a.visita()));
 }
 @Test void visitaFinalizadaGeneraOtraSinAlterarLaAnterior()throws Exception{
  var practica=new Visitas(tx,reloj).practica();var a=practica.abrir(cuenta(admin),"PLANTA");Sql.actualizar(con,"UPDATE visita SET estado='FINALIZADA',hora_real_fin=hora_real_inicio WHERE id_visita=?",a.visita());Sql.actualizar(con,"UPDATE reserva SET estado='FINALIZADA' WHERE id_reserva=(SELECT id_reserva FROM visita WHERE id_visita=?)",a.visita());
  var b=practica.abrir(cuenta(admin),"PLANTA");assertNotEquals(a.visita(),b.visita());assertEquals(a.puesto(),b.puesto());assertEquals("FINALIZADA",Sql.<String>uno(con,"SELECT estado FROM visita WHERE id_visita=?",x->x.getString(1),a.visita()));assertEquals("EN_CURSO",Sql.<String>uno(con,"SELECT estado FROM visita WHERE id_visita=?",x->x.getString(1),b.visita()));
 }
 @Test void usuarioNoVeElAccesoNiPuedeCrearPracticaConDireccionManual()throws Exception{
  Sql.actualizar(con,"UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?",usuario);entrar(usuario);var inicio=get("/");assertFalse(inicio.body().contains("Abrir evaluación de prueba"));assertEquals(403,get("/demo/evaluacion").statusCode());assertEquals(403,post("/demo/evaluacion",Map.of("csrf",csrf(inicio),"tipo","PLANTA")).statusCode());assertEquals(0,cantidad());assertThrows(SecurityException.class,()->new Visitas(tx,reloj).practica().abrir(cuenta(usuario),"PLANTA"));
 }
 @Test void csrfYTipoInvalidoNoCreanDatos()throws Exception{
  entrar(admin);assertEquals(403,post("/demo/evaluacion",Map.of("csrf","incorrecto","tipo","PLANTA")).statusCode());assertEquals(400,abrir("OTRO").statusCode());assertEquals(0,cantidad());
 }
 @Test void banderaDeDemoOcultaYBloqueaElAcceso()throws Exception{
  entrar(admin);String token=csrf(get("/demo/evaluacion")),previo=System.getProperty(GaleriaDemo.BANDERA);try{System.setProperty(GaleriaDemo.BANDERA,"false");assertFalse(get("/").body().contains("Abrir evaluación de prueba"));assertEquals(404,get("/demo/evaluacion").statusCode());assertEquals(404,post("/demo/evaluacion",Map.of("csrf",token,"tipo","PLANTA")).statusCode());assertEquals(0,cantidad());}finally{if(previo==null)System.clearProperty(GaleriaDemo.BANDERA);else System.setProperty(GaleriaDemo.BANDERA,previo);}
 }
 @Test void noReactivaUnaEmpresaDesactivadaNiTocaUnaEmpresaAjena()throws Exception{
  crearSalon();int ajena=empresa;var practica=new Visitas(tx,reloj).practica();practica.abrir(cuenta(admin),"PLANTA");Sql.actualizar(con,"UPDATE empresa SET activo=FALSE WHERE nit=?",PracticaEvaluacion.NIT);assertThrows(IllegalArgumentException.class,()->practica.abrir(cuenta(admin),"PLANTA"));assertFalse(Sql.<Boolean>uno(con,"SELECT activo FROM empresa WHERE nit=?",x->x.getBoolean(1),PracticaEvaluacion.NIT));assertTrue(Sql.<Boolean>uno(con,"SELECT activo FROM empresa WHERE id_empresa=?",x->x.getBoolean(1),ajena));
 }
}
