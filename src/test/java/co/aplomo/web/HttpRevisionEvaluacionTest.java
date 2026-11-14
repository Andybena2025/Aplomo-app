package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.servicio.*;
import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Paso 14: contexto, contrato guardado y seguridad en HTTP real, siempre ROLLBACK. */
class HttpRevisionEvaluacionTest extends HttpPrueba {
 int visita,puesto;String token;
 @BeforeEach void contexto()throws Exception{
  var salon=crearSalon();puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Trabajo de prueba","MESA","Centro",null,true)).id();
  var rs=new Reservas(tx,reloj);int r=rs.guardar(cuenta(admin),0,solicitud(salon));rs.aceptar(cuenta(admin),r);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),r);
  entrar(admin);token=csrf(get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto));
 }
 Map<String,String> campos(){
  return new LinkedHashMap<>(Map.of("csrf",token,"visita",""+visita,"puesto",""+puesto,"recorrido","guiado","postura","Levantar una caja","criterio","MAS_INADECUADA","minutos","90","condiciones_respondidas","NINGUNA","acepta","si","registro","CON_FOTOS"));
 }
 HttpResponse<String> guardar(Map<String,String> f,int fotos)throws Exception{
  String limite="AplomoRevision14Prueba";var out=new ByteArrayOutputStream();
  for(var e:f.entrySet())parte(out,limite,e.getKey(),e.getValue());
  var imagen=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(40,80,1),"png",imagen);
  for(int i=0;i<fotos;i++){out.write(("--"+limite+"\r\nContent-Disposition: form-data; name=\"foto_"+i+"\"; filename=\"prueba.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(imagen.toByteArray());out.write("\r\n".getBytes(StandardCharsets.UTF_8));}
  out.write(("--"+limite+"--\r\n").getBytes(StandardCharsets.UTF_8));
  return cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/evaluaciones")).header("Content-Type","multipart/form-data; boundary="+limite).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
 }
 static void parte(ByteArrayOutputStream b,String limite,String clave,String valor)throws Exception{b.write(("--"+limite+"\r\nContent-Disposition: form-data; name=\""+clave+"\"\r\n\r\n"+valor+"\r\n").getBytes(StandardCharsets.UTF_8));}
 int cantidad(String tabla)throws Exception{return Sql.uno(con,"SELECT COUNT(*) FROM "+tabla,r->r.getInt(1));}
 @Test void contextoContieneTresBloquesYLosValoresExactosEnUnaSolaFase()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();
  String contexto=h.substring(h.indexOf("data-paso=\"2\""),h.indexOf("data-paso=\"3\""));
  for(String t:List.of("¿Cuántas horas al día trabaja en este puesto?","¿En qué momento del trabajo se siente más incómodo o más cansado?","data-valor=\"60\"","data-valor=\"180\"","data-valor=\"300\"","data-valor=\"420\"","data-valor=\"540\"","No sabe, evalúo la postura que más repite"))assertTrue(contexto.contains(t),t);
  assertFalse(contexto.contains("data-tarea=\"Levantar"));assertFalse(contexto.contains(" checked"));assertEquals(9,h.split(" data-paso=",-1).length-1);
 }
 @Test void revisionTieneSieteFilasYLosAjustesSonOpcionalesSinVentanas()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();
  assertEquals(4,h.split("data-medida=",-1).length-1);
  for(String n:List.of("hombroElevado","brazoSeparado","brazoApoyado","antebrazoCruza","muneca_campo","munecaDesviada","giro_campo","cuello_campo","tronco_campo","piernas_campo"))assertTrue(h.contains("data-pregunta=\""+n+"\""));
  assertFalse(h.contains("<dialog"));assertFalse(h.contains("role=\"dialog\""));assertFalse(h.contains(" checked"));assertTrue(h.contains("data-falta"));assertTrue(h.contains("aria-expanded=\"false\""));assertTrue(h.contains("data-medidas-compactas hidden"));
 }
 @Test void ladoTieneCuatroVinetasSiemprePresentesYEsfuerzoSePreguntaUnaVez()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();
  String lado=h.substring(h.indexOf("data-paso=\"4\""),h.indexOf("data-paso=\"5\""));
  assertEquals(4,lado.split("<li>",-1).length-1);assertFalse(lado.contains("<details"));assertFalse(lado.contains("Ver cómo decidir"));assertTrue(lado.contains("Si no está claro, elige Ambos."));
  String esfuerzo=h.substring(h.indexOf("data-paso=\"8\""),h.indexOf("data-paso=\"9\""));
  assertEquals(1,esfuerzo.split("name=\"esfuerzo_opcion\"",-1).length-1);assertEquals(1,esfuerzo.split("name=\"peso_campo\"",-1).length-1);
  assertTrue(esfuerzo.contains("Puedes observar un minuto con el reloj."));assertTrue(esfuerzo.contains("Piensa en lo más pesado de esta tarea."));
 }
 @ParameterizedTest @ValueSource(strings={"postura","criterio","minutos","condiciones_respondidas"})
 void contextoObligatorioSeValidaTambienSiSeSaltaLaPantalla(String campo)throws Exception{
  var f=campos();f.remove(campo);f.put("registro","SIN_REGISTRO_FOTOGRAFICO");
  int antes=cantidad("evaluacion");assertEquals(400,guardar(f,0).statusCode());assertEquals(antes,cantidad("evaluacion"));
 }
 @Test void frenteDeApoyoNoCreaOtraEvaluacionNiOtroAnalisisYConservaLosCampos()throws Exception{
  var f=campos();f.put("asistente_completo","si");f.put("foto_0_vista","PERFIL");f.put("foto_0_descripcion","Perfil");f.put("foto_1_vista","FRENTE");f.put("foto_1_descripcion","Frente");
  var a=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());a.put("foto","0");f.put("analisis_0",CasosRulaPrueba.codificar(a));
  var r=guardar(f,2);assertEquals(201,r.statusCode(),r.body());
  assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",x->x.getInt(1),visita));
  var filas=Sql.listar(con,"SELECT f.vista,COUNT(a.id_analisis) cantidad FROM fotografia f JOIN evaluacion e USING(id_evaluacion) LEFT JOIN analisis_rula a USING(id_foto) WHERE e.id_visita=? GROUP BY f.id_foto,f.vista ORDER BY f.id_foto",x->List.of(x.getString(1),x.getString(2)),visita);
  assertEquals(List.of(List.of("PERFIL","1"),List.of("FRENTE","0")),filas);
  var guardado=Sql.uno(con,"SELECT e.postura,e.criterio,e.minutos_jornada,a.carga,a.puntaje_final FROM evaluacion e JOIN fotografia f USING(id_evaluacion) JOIN analisis_rula a USING(id_foto) WHERE e.id_visita=?",x->List.of(x.getString(1),x.getString(2),x.getString(3),x.getString(4),x.getString(5)),visita);
  assertEquals(List.of("Levantar una caja","MAS_INADECUADA","90",a.get("carga"),"7"),guardado);
  int ev=Sql.uno(con,"SELECT id_evaluacion FROM evaluacion WHERE id_visita=?",x->x.getInt(1),visita);
  String html=get("/evaluaciones/"+ev+"/analizar").body();assertTrue(html.contains("data-continuar=\""+ev+"\""));assertTrue(html.contains("data-revision=\"si\""));assertTrue(html.contains("data-criterio=\"MAS_INADECUADA\""));assertFalse(html.contains("<dialog"));
 }
 @Test void esfuerzoIgualPorLadoSeConservaEnDosAnalisisSinValoresDePuntajeDelCliente()throws Exception{
  var f=campos();f.put("asistente_completo","si");
  for(int i=0;i<2;i++){f.put("foto_"+i+"_vista","PERFIL");f.put("foto_"+i+"_descripcion","Perfil "+i);var a=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());a.put("foto",""+i);f.put("analisis_"+i,CasosRulaPrueba.codificar(a));}
  f.put("puntaje_final","1");var r=guardar(f,2);assertEquals(201,r.statusCode(),r.body());
  var resultados=Sql.listar(con,"SELECT a.lado,a.carga,a.postura_estatica_o_repetitiva,a.puntaje_final FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=? ORDER BY a.id_analisis",x->List.of(x.getString(1),x.getString(2),x.getString(3),x.getString(4)),visita);
  assertEquals(2,resultados.size());assertEquals(resultados.get(0),resultados.get(1));assertEquals("7",resultados.get(0).get(3));
 }
 @ParameterizedTest @ValueSource(ints={60,180,300,420,540})
 void campoGuardaNuevosTiemposPosturaConLadoYMotivoCorto(int minutos)throws Exception{
  var f=campos();f.put("recorrido","campo");f.put("minutos",""+minutos);f.put("postura","Trabajo de prueba · lado derecho");f.put("motivo","El momento que señala el trabajador.");f.put("registro","SIN_REGISTRO_FOTOGRAFICO");
  var r=guardar(f,0);assertEquals(201,r.statusCode(),r.body());var datos=Sql.uno(con,"SELECT postura,minutos_jornada,motivo,criterio FROM evaluacion WHERE id_visita=?",x->List.of(x.getString(1),x.getString(2),x.getString(3),x.getString(4)),visita);assertEquals(List.of("Trabajo de prueba · lado derecho",""+minutos,"El momento que señala el trabajador.","MAS_INADECUADA"),datos);
 }
 @Test void tiempoAntiguoNoEntraPorElNuevoRecorridoYNoSeGuardaNada()throws Exception{var f=campos();f.put("recorrido","campo");f.put("minutos","30");f.put("registro","SIN_REGISTRO_FOTOGRAFICO");assertEquals(400,guardar(f,0).statusCode());assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",x->x.getInt(1),visita));}

 @ParameterizedTest @ValueSource(ints={1,2,3,4})
 void primerasCuatroPantallasNoTienenFotosNiSiluetas(int paso)throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();String seccion=h.substring(h.indexOf("data-paso=\""+paso+"\""),h.indexOf("data-paso=\""+(paso+1)+"\""));assertFalse(seccion.contains("<img"));assertFalse(seccion.contains("campo-silueta"));assertFalse(seccion.contains("campo-foto"));
  if(paso==3)for(var c:PaginasEvaluacion.CONDICIONES){assertTrue(seccion.contains(c[1]));assertTrue(seccion.contains(c[2]));}
 }
 @Test void cadaSubidaTieneCincoEjemplosDeSuVistaYSeSirvenComoJpg()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();assertEquals(15,h.split("data-usar-ejemplo=",-1).length-1);assertEquals(3,h.split("Usar foto de ejemplo",-1).length-1);
  for(var par:new String[][]{{"PERFIL","perfil"},{"FRENTE","frente"},{"OTRA","detalle"}}){assertEquals(5,h.split("data-vista-ejemplo=\""+par[0]+"\"",-1).length-1);var r=get("/static/img/galeria/S6_levantando_pieza_pesada_"+par[1]+".jpg");assertEquals(200,r.statusCode());assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("image/jpeg"));}
 }

}
