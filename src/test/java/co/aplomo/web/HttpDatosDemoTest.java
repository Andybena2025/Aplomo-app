package co.aplomo.web;
import co.aplomo.db.Sql;
import co.aplomo.rula.*;
import co.aplomo.soporte.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** El SQL opcional se prueba completo dos veces y con ediciones, siempre ROLLBACK. */
class HttpDatosDemoTest extends HttpPrueba {
 private Map<String,Integer> cantidades()throws Exception{
  Map<String,Integer> n=new TreeMap<>();
  for(String tabla:List.of("empresa","sede","salon","area","cargo","puesto_trabajo","reserva","visita","evaluacion","condiciones_puesto","fotografia","analisis_rula","medicion_angulo","pendiente_arreglo","sugerencia","bitacora"))n.put(tabla,Sql.uno(con,"SELECT COUNT(*) FROM "+tabla,r->r.getInt(1)));
  return n;
 }
 @Test void demoEsIdempotenteConservaEdicionesYCubreLosTresColoresEnElInforme()throws Exception{
  DatosDemoPrueba.cargar(con);var primera=cantidades();DatosDemoPrueba.cargar(con);assertEquals(primera,cantidades());
  int universidad=Sql.uno(con,"SELECT id_empresa FROM empresa WHERE nit=?",r->r.getInt(1),"DEMO-APLOMO-E3");
  assertEquals(3,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM sede WHERE id_empresa=?",r->r.getInt(1),universidad));
  assertEquals(3,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM empresa WHERE nit LIKE 'DEMO-APLOMO-E%'",r->r.getInt(1)));
  var filas=Sql.listar(con,"SELECT a.id_analisis,a.puntaje_a,a.puntaje_b,a.puntaje_final FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) WHERE r.codigo=? ORDER BY a.puntaje_final",r->new int[]{r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4)},"DEMO-FIN-01");
  assertEquals(3,filas.size());var tablas=new TablasRula();tablas.cargar(con);var motor=new MotorRula(tablas);
  int[] casos={4,2,8};
  for(int i=0;i<3;i++){
   var calculo=motor.calcular(CasosRulaPrueba.todos().get(casos[i]-1).entrada());var esperado=calculo.resultado();var fila=filas.get(i);
   assertArrayEquals(new int[]{esperado.puntajeA(),esperado.puntajeB(),esperado.puntajeFinal()},new int[]{fila[1],fila[2],fila[3]});
   var medidas=new co.aplomo.dao.AnalisisDao().mediciones(con,fila[0]);assertEquals(5,medidas.size());
   for(var m:calculo.mediciones()){var guardada=medidas.stream().filter(x->x.segmento()==m.segmento()).findFirst().orElseThrow();assertEquals(m.grados(),guardada.grados(),.001);assertEquals(m.puntos(),guardada.puntos());assertTrue(guardada.manual());}
  }
  int pendiente=Sql.uno(con,"SELECT p.id_pendiente FROM pendiente_arreglo p JOIN evaluacion e USING(id_evaluacion) JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) WHERE r.codigo=?",r->r.getInt(1),"DEMO-FIN-01");
  Sql.actualizar(con,"UPDATE pendiente_arreglo SET estado='ARREGLADO',descripcion=? WHERE id_pendiente=?","Edición conservada",pendiente);
  DatosDemoPrueba.cargar(con);assertEquals(primera,cantidades());assertEquals("ARREGLADO",Sql.uno(con,"SELECT estado FROM pendiente_arreglo WHERE id_pendiente=?",r->r.getString(1),pendiente));
  entrar(admin);var informe=get("/informes/empresa/"+universidad);assertEquals(200,informe.statusCode());
  for(String etiqueta:List.of("1 · Verde · Nivel 1","3 · Amarillo · Nivel 2","7 · Rojo · Nivel 4","Sede Alcázar","Sede Anova (Medicina)"))assertTrue(informe.body().contains(etiqueta),etiqueta);
  assertTrue(informe.body().contains("2 de 3 sedes calificadas · falta Sede Anova (Medicina)"));
  assertFalse(informe.body().contains("Sin calificación general"));
  var segunda=Sql.uno(con,"SELECT a.id_analisis FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) WHERE r.codigo=?",r->r.getInt(1),"DEMO-FIN-02");
  var esperado=motor.calcular(CasosRulaPrueba.todos().get(3).entrada());
  assertEquals(esperado.resultado().puntajeFinal(),Sql.<Integer>uno(con,"SELECT puntaje_final FROM analisis_rula WHERE id_analisis=?",r->r.getInt(1),segunda));
  var medidasAlcazar=new co.aplomo.dao.AnalisisDao().mediciones(con,segunda);assertEquals(5,medidasAlcazar.size());
  for(var m:esperado.mediciones()){var guardada=medidasAlcazar.stream().filter(x->x.segmento()==m.segmento()).findFirst().orElseThrow();assertEquals(m.grados(),guardada.grados(),.001);assertEquals(m.puntos(),guardada.puntos());}
  assertEquals(2,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM visita v JOIN reserva r USING(id_reserva) WHERE r.codigo IN ('DEMO-FIN-01','DEMO-FIN-02') AND v.estado='FINALIZADA'",r->r.getInt(1)));
 }
}
