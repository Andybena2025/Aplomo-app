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
  for(String t:List.of("¿Cuántas horas al día trabaja en este puesto?","data-valor=\"60\"","data-valor=\"180\"","data-valor=\"300\"","data-valor=\"420\"","data-valor=\"540\"","Condiciones del puesto","name=\"ninguna\""))assertTrue(contexto.contains(t),t);assertFalse(h.contains("momento_campo"));assertFalse(h.contains("duracion_campo"));assertEquals(4,h.split(" data-paso=",-1).length-1);

 }
 @Test void revisionTieneSieteFilasYLosAjustesSonOpcionalesSinVentanas()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();
  assertEquals(4,h.split("data-medida=",-1).length-1);
  for(String n:List.of("hombroElevado","brazoSeparado","brazoApoyado","antebrazoCruza","munecaDesviada","cuelloGirado","cuelloInclinado","troncoGirado","troncoInclinado","pies_sin_apoyo","giro_extremo"))assertTrue(h.contains("data-chip=\""+n+"\""),n);assertEquals(3,h.split("data-muneca=",-1).length-1);
  assertFalse(h.contains("<dialog"));assertFalse(h.contains("role=\"dialog\""));assertFalse(h.contains(" checked"));assertTrue(h.contains("data-falta"));assertTrue(h.contains("aria-expanded=\"false\""));assertTrue(h.contains("data-medidas-compactas hidden"));
 }
 @Test void ladoTieneCuatroVinetasSiemprePresentesYEsfuerzoSePreguntaUnaVez()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();
  String postura=h.substring(h.indexOf("data-paso=\"3\""),h.indexOf("data-paso=\"5\""));assertFalse(h.contains("name=\"lado_recorrido\""));assertTrue(postura.contains("data-lado-campo=\"AMBOS\""));for(String campo:List.of("carga_campo","actividad_campo","tirones_campo"))assertEquals(1,postura.split("name=\""+campo+"\"",-1).length-1);assertFalse(postura.contains("Lo más pesado que levanta"));

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

 @ParameterizedTest @ValueSource(ints={1,2})
 void primerasCuatroPantallasNoTienenFotosNiSiluetas(int paso)throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();String seccion=h.substring(h.indexOf("data-paso=\""+paso+"\""),h.indexOf("data-paso=\""+(paso+1)+"\""));assertFalse(seccion.contains("<img"));assertFalse(seccion.contains("campo-silueta"));assertFalse(seccion.contains("campo-foto"));
  if(paso==2)for(var c:PaginasEvaluacion.CONDICIONES){assertTrue(seccion.contains(c[1]));assertFalse(seccion.contains(c[2]));}
 }
 @Test void cadaSubidaTieneCincoEjemplosDeSuVistaYSeSirvenComoJpg()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();assertEquals(15,h.split("data-usar-ejemplo=",-1).length-1);assertEquals(3,h.split("Usar foto de ejemplo",-1).length-1);
  for(var par:new String[][]{{"PERFIL","perfil"},{"FRENTE","frente"},{"OTRA","detalle"}}){assertEquals(5,h.split("data-vista-ejemplo=\""+par[0]+"\"",-1).length-1);var r=get("/static/img/galeria/S6_levantando_pieza_pesada_"+par[1]+".jpg");assertEquals(200,r.statusCode());assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("image/jpeg"));}
 }

 @Test void unaPosturaConservaConfirmacionYMotivoExactoSinFotos()throws Exception{
  var f=campos();f.put("recorrido","campo");f.put("minutos","180");f.put("motivo","También es la que más tiempo se mantiene");f.put("registro","SIN_REGISTRO_FOTOGRAFICO");
  assertEquals(201,guardar(f,0).statusCode());assertEquals(List.of("1","SIN_REGISTRO_FOTOGRAFICO","También es la que más tiempo se mantiene"),Sql.uno(con,"SELECT persona_acepto,registro_fotografico,motivo FROM evaluacion WHERE id_visita=?",x->List.of(x.getString(1),x.getString(2),x.getString(3)),visita));
  assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora WHERE tabla='evaluacion' AND id_cuenta=? AND fecha_hora IS NOT NULL AND detalle LIKE ?",x->x.getInt(1),admin,"%Consentimiento escrito confirmado%"));
 }
 @Test void dosPosturasSeGuardanSeparadasConCriteriosYPuntajeDelServidor()throws Exception{
  for(String criterio:List.of("MAS_INADECUADA","MAYOR_EXPOSICION")){
   var f=campos();f.put("recorrido","campo");f.put("minutos","180");f.put("criterio",criterio);f.put("postura","Trabajo · "+criterio+" · lado izquierdo");f.put("asistente_completo","si");f.put("foto_0_vista","PERFIL");f.put("foto_0_descripcion","Perfil");var a=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());a.put("foto","0");a.put("lado","IZQUIERDO");f.put("analisis_0",CasosRulaPrueba.codificar(a));f.put("puntaje_final","1");assertEquals(201,guardar(f,1).statusCode());
  }
  assertEquals(List.of(List.of("MAS_INADECUADA","7","IZQUIERDO"),List.of("MAYOR_EXPOSICION","7","IZQUIERDO")),Sql.listar(con,"SELECT e.criterio,a.puntaje_final,a.lado FROM evaluacion e JOIN fotografia f USING(id_evaluacion) JOIN analisis_rula a USING(id_foto) WHERE e.id_visita=? ORDER BY e.id_evaluacion",x->List.of(x.getString(1),x.getString(2),x.getString(3)),visita));
 }
 @Test void siluetasSoloEnCuatroFilasYConsentimientoSinVoz()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body(),cons=h.substring(h.indexOf("data-paso=\"1\""),h.indexOf("data-paso=\"2\""));
  assertFalse(cons.contains("Pregúntale"));assertFalse(cons.contains("Míralo"));assertTrue(cons.contains("Confirmo que la persona evaluada aceptó y firmó el consentimiento informado por escrito."));assertEquals(4,h.split("class=\"campo-silueta\"",-1).length-1);assertEquals(1,h.split("data-postura-aviso",-1).length-1);assertTrue(h.contains("Resumen del puesto"));assertTrue(h.contains("Guardar y terminar"));assertFalse(h.contains("data-evaluar-otro"));assertTrue(h.contains("campo-dos-columnas"));
 }

 @Test void ultimaPosturaMasCriticaConservaElMaximoDelPuesto()throws Exception{
  for(int caso:List.of(3,7)){var f=campos();f.put("asistente_completo","si");f.put("foto_0_vista","PERFIL");f.put("foto_0_descripcion","Perfil");var a=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(caso).entrada());a.put("foto","0");f.put("analisis_0",CasosRulaPrueba.codificar(a));assertEquals(201,guardar(f,1).statusCode());}
  assertEquals(7,Sql.<Integer>uno(con,"SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",x->x.getInt(1),puesto));
 }

 @Test void listasConNoAplicaYRecuadroDeFotoSinResultadoIntermedio()throws Exception{
  String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();for(var par:new String[][]{{"carga_campo","CELULAR"},{"actividad_campo","NINGUNA"},{"tirones_campo","no"}})assertTrue(h.contains("name=\""+par[0]+"\" value=\""+par[1]+"\""));assertEquals(1,h.split("data-resultados",-1).length-1);assertFalse(h.contains("data-postura-siguiente"));for(String t:List.of("data-error-titulo","data-foto-reemplazar","data-foto-manual","data-foto-usar","Agregar otra foto (opcional)"))assertTrue(h.contains(t));
 }
 @Test void motivoDeMayorDuracionConservaTextoCorto()throws Exception{var f=campos();f.put("criterio","MAYOR_EXPOSICION");f.put("motivo","La postura que más tiempo mantiene.");f.put("registro","SIN_REGISTRO_FOTOGRAFICO");assertEquals(201,guardar(f,0).statusCode());assertEquals("La postura que más tiempo mantiene.",Sql.<String>uno(con,"SELECT motivo FROM evaluacion WHERE id_visita=?",x->x.getString(1),visita));}

 @Test void motivoDeIncomodidadEsAutomaticoYCorto()throws Exception{var f=campos();f.put("motivo","La postura más incómoda.");f.put("registro","SIN_REGISTRO_FOTOGRAFICO");assertEquals(201,guardar(f,0).statusCode());assertEquals("La postura más incómoda.",Sql.<String>uno(con,"SELECT motivo FROM evaluacion WHERE id_visita=?",x->x.getString(1),visita));}

}
