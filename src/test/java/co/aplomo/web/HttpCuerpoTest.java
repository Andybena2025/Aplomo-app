package co.aplomo.web;

import co.aplomo.dao.*;import co.aplomo.db.Sql;import co.aplomo.modelo.*;import co.aplomo.servicio.*;import co.aplomo.rula.MotorRula.*;import co.aplomo.rula.MedidorAngulos.Lado;import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.*;
import java.util.*;import java.io.*;import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Paso 13E-B: parcial sin supuestos, ambos lados atómicos y archivos existentes. */
class HttpCuerpoTest extends HttpPrueba {
    int visita,puesto;String token;
    @BeforeEach void contexto()throws Exception{
        var salon=crearSalon();puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Ensamble","MESA","Centro",null,true)).id();
        var rs=new Reservas(tx,reloj);int r=rs.guardar(cuenta(admin),0,solicitud(salon));rs.aceptar(cuenta(admin),r);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),r);entrar(admin);token=csrf(get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto));
    }
    private Map<String,String> parcial(){var f=new LinkedHashMap<String,String>();f.put("csrf",token);f.put("visita",""+visita);f.put("segmento","BRAZO");f.put("lado","DERECHO");f.put("ancho","200");f.put("alto","400");f.put("BRAZO_modo","MANUAL");f.put("BRAZO_grados","52");return f;}
    @Test void parcialNoMuestraUnFinalNiInventaAjustesYConservaElAnguloReal()throws Exception{
        var f=parcial();var r=post("/evaluaciones/calcular",f);assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("data-grados=\"52.0\""));assertTrue(r.body().contains("data-rango=\"60.0\""));assertTrue(r.body().contains("data-puntos=\"\""));assertFalse(r.body().contains("data-puntaje-final"));
        f.put("ajustes_confirmados","si");f.put("hombroElevado","si");f.put("brazoSeparado","no");f.put("brazoApoyado","no");r=post("/evaluaciones/calcular",f);assertEquals(200,r.statusCode());assertTrue(r.body().contains("data-puntos=\"4\""));assertFalse(r.body().contains("data-puntaje-final"));assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",rs->rs.getInt(1),visita));
    }
    @ParameterizedTest @ValueSource(strings={"BRAZO_modo","ANTEBRAZO_modo","MUNECA_modo","CUELLO_modo","TRONCO_modo","giro","piernasApoyadas","actividad","carga"})
    void finalSoloApareceCuandoEstanTodosLosSegmentosActividadYCarga(String campo)throws Exception{
        var f=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());f.put("csrf",token);f.put("visita",""+visita);String valor=f.remove(campo);
        var r=post("/evaluaciones/calcular",f);assertEquals(400,r.statusCode(),r.body());assertFalse(r.body().contains("data-puntaje-final"));f.put(campo,valor);r=post("/evaluaciones/calcular",f);assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("data-puntaje-final"));
    }
    @ParameterizedTest @CsvSource({"GIRO,MEDIO,1","GIRO,EXTREMO,2","PIERNAS,si,1","PIERNAS,no,2"})
    void observadoPuntuaSoloElComponenteConfirmado(String segmento,String respuesta,int esperado)throws Exception{
        var f=new HashMap<>(Map.of("csrf",token,"visita",""+visita,"segmento",segmento,"respuesta",respuesta));var r=post("/evaluaciones/calcular",f);assertEquals(200,r.statusCode());assertTrue(r.body().contains("data-puntos=\""+esperado+"\""));assertFalse(r.body().contains("data-puntaje-final"));
    }
    @Test void parcialValidaPuntosYCasillasSinGuardarNiAceptarCodigoDeCliente()throws Exception{
        var f=parcial();f.put("BRAZO_modo","PUNTOS");f.put("BRAZO_puntos","0.5,0.8;0.5,0.4;0.8,0.6");assertEquals(200,post("/evaluaciones/calcular",f).statusCode());
        f.put("BRAZO_puntos","1.1,0.8;0.5,0.4;0.8,0.6");assertEquals(400,post("/evaluaciones/calcular",f).statusCode());f.put("BRAZO_modo","MANUAL");f.put("BRAZO_puntos","");f.put("ajustes_confirmados","si");assertEquals(400,post("/evaluaciones/calcular",f).statusCode());f.put("csrf","incorrecto");assertEquals(403,post("/evaluaciones/calcular",f).statusCode());
    }
    @Test void paginaUsaLosArchivosExactosDialogoYNingunAvisoInicial()throws Exception{
        String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();assertTrue(h.contains("data-cuerpo=\"si\""));assertTrue(h.contains("class=\"tour-opcion tour-postura\""));assertTrue(h.contains("/static/img/tour/postura_de_pie.jpg"));assertTrue(h.contains("/static/img/tour/postura_computador.jpg"));assertTrue(h.contains("/static/img/tour/muneca_desviada.png"));assertTrue(h.contains("aria-modal=\"true\" role=\"dialog\""));assertTrue(h.contains("data-pregunta-pendiente role=\"status\" hidden"));assertTrue(h.contains("name=\"lado_recorrido\" value=\"AMBOS\""));assertFalse(h.contains("class=\"tour-sin-respuesta\""));assertEquals(200,get("/static/js/recorrido.js").statusCode());
    }
    private byte[] png()throws Exception{var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(30,60,1),"png",b);return b.toByteArray();}
    private HttpResponse<String> enviarAmbos(int fotos,boolean duplicado)throws Exception{
        String boundary="AplomoAmbosLados123";var campos=new LinkedHashMap<String,String>();campos.put("csrf",token);campos.put("visita",""+visita);campos.put("puesto",""+puesto);campos.put("postura","Ensamble");campos.put("criterio","MAS_INADECUADA");campos.put("minutos","30");campos.put("acepta","si");campos.put("registro","CON_FOTOS");campos.put("recorrido","guiado");campos.put("condiciones_respondidas","SELECCION");campos.put("asistente_completo","si");
        for(String c:List.of("escritorio","pantalla","pies","apoyapies","apoyabrazos","regulable"))campos.put(c,"si");
        for(int i=0;i<fotos;i++)for(int lado=0;lado<2;lado++){var f=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(lado==0?0:7).entrada());f.put("foto",""+i);f.put("lado",lado==0||duplicado?"DERECHO":"IZQUIERDO");campos.put("analisis_"+(i*2+lado),CasosRulaPrueba.codificar(f));}
        var out=new ByteArrayOutputStream();for(var e:campos.entrySet())parte(out,boundary,e.getKey(),e.getValue());
        for(int i=0;i<fotos;i++){parte(out,boundary,"foto_"+i+"_vista","PERFIL");parte(out,boundary,"foto_"+i+"_descripcion","Perfil "+i);out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"foto_"+i+"\"; filename=\"foto.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());out.write(png());out.write("\r\n".getBytes());}
        out.write(("--"+boundary+"--\r\n").getBytes());return cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/evaluaciones")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    private static void parte(ByteArrayOutputStream out,String b,String k,String v)throws Exception{out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\""+k+"\"\r\n\r\n"+v+"\r\n").getBytes(StandardCharsets.UTF_8));}
    @ParameterizedTest @ValueSource(ints={1,10})
    void ambosGuardaDosAnalisisPorFotoEnUnaTransaccionYConservaElMayor(int fotos)throws Exception{
        var r=enviarAmbos(fotos,false);assertEquals(201,r.statusCode(),r.body());
        assertEquals(fotos*2,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",rs->rs.getInt(1),visita));assertEquals(fotos*10,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM medicion_angulo m JOIN analisis_rula a USING(id_analisis) JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",rs->rs.getInt(1),visita));
        assertEquals(7,Sql.<Integer>uno(con,"SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",rs->rs.getInt(1),puesto));assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM pendiente_arreglo p JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",rs->rs.getInt(1),visita));
    }
    @Test void ambosNoPermiteDuplicarFotoYLadoNiDejaGuardadosParciales()throws Exception{
        assertEquals(400,enviarAmbos(1,true).statusCode());assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",rs->rs.getInt(1),visita));try(var archivos=java.nio.file.Files.list(carpetaFotos)){assertEquals(0,archivos.count());}
    }
    @Test void usuarioNoObtieneMedicionesParcialesNiFotosPorDireccionManual()throws Exception{
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);assertEquals(403,post("/evaluaciones/calcular",parcial()).statusCode());assertEquals(403,get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).statusCode());
    }
}
