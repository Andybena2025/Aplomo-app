package co.aplomo.web;

import co.aplomo.servicio.*;import co.aplomo.dao.*;import co.aplomo.db.Sql;import co.aplomo.modelo.*;import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;import java.util.*;import java.io.*;import javax.imageio.ImageIO;import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** RF-017–021: los diez resultados llegan en HTML y el servidor ignora puntajes adulterados. */
class HttpAnalisisTest extends HttpPrueba {
    int visita,puesto,eval,foto;Evaluaciones evaluaciones;
    @BeforeEach void contexto()throws Exception{
        evaluaciones=new Evaluaciones(tx,reloj,new Fotos(carpetaFotos),new BitacoraDao());var salon=crearSalon();puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Ensamble","ESCRITORIO","Centro",null,true)).id();
        var reservas=new Reservas(tx,reloj);int r=reservas.guardar(cuenta(admin),0,solicitud(salon));reservas.aceptar(cuenta(admin),r);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),r);
        var d=new Evaluaciones.Datos(puesto,"Ensamble","MAS_INADECUADA","Trabajo mantenido",120,false,false,false,false,false,false,"",true,false);
        eval=evaluaciones.guardar(cuenta(admin),visita,d,List.of(new Fotos.Entrada(png(),"PERFIL","Foto completa")));foto=new EvaluacionDao().fotos(con,eval).get(0).id();entrar(admin);
    }
    private byte[] png()throws Exception{var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(200,400,1),"png",b);return b.toByteArray();}
    private Map<String,String> formulario(int caso)throws Exception{
        var f=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(caso-1).entrada());f.put("csrf",csrf(get("/evaluaciones/"+eval+"/analizar")));f.put("visita",""+visita);return f;
    }
    @TestFactory java.util.stream.Stream<DynamicTest> diezCasosEnLaPantallaDeResultado(){return CasosRulaPrueba.todos().stream().map(c->DynamicTest.dynamicTest("Pantalla caso "+c.numero(),()->{
        var r=post("/evaluaciones/calcular",formulario(c.numero()));assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("data-puntaje-final=\""+c.esperado()+"\""));assertTrue(r.body().contains("Nivel "));assertTrue(r.body().contains("Tabla A · Tabla B"));
    }));}
    @Test void puntajeAdulteradoSeIgnoraYElHistorialQuedaProtegido()throws Exception{
        var f=formulario(8);f.put("foto",""+foto);f.put("puntaje_final","1");f.put("id_nivel","1");var guardar=new LinkedHashMap<String,String>();guardar.put("csrf",f.get("csrf"));guardar.put("analisis_0",CasosRulaPrueba.codificar(f));guardar.put("puntaje_final","1");
        var r=post("/evaluaciones/"+eval+"/analisis",guardar);assertEquals(201,r.statusCode(),r.body());int id=Sql.uno(con,"SELECT id_analisis FROM analisis_rula WHERE id_foto=?",rs->rs.getInt(1),foto);
        assertEquals(7,Sql.<Integer>uno(con,"SELECT puntaje_final FROM analisis_rula WHERE id_analisis=?",rs->rs.getInt(1),id));var detalle=get("/evaluaciones/"+eval+"/analisis/"+id);assertEquals(200,detalle.statusCode());assertTrue(detalle.body().contains("data-puntaje-final=\"7\""));assertTrue(detalle.body().contains("Registró"));
        var inicio=get("/");assertTrue(inicio.body().contains("Puestos en rojo"));assertTrue(inicio.body().contains("color:var(--rojo)"));assertFalse(inicio.body().contains("Total visitas"));
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);
        assertEquals(403,get("/evaluaciones/"+eval+"/analizar").statusCode());assertEquals(403,get("/evaluaciones/"+eval+"/analisis/"+id).statusCode());assertEquals(403,post("/evaluaciones/calcular",f).statusCode());assertEquals(403,post("/evaluaciones/"+eval+"/analisis",guardar).statusCode());
    }
    @Test void visitaFinalizadaConservaElResultadoYOcultaElBotonDeAnalizar()throws Exception{
        var entrada=CasosRulaPrueba.todos().get(3).entrada();evaluaciones.analisis().guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,entrada)));
        int id=evaluaciones.analisis().listar(cuenta(admin),eval).get(0).id();new Visitas(tx,reloj).finalizar(cuenta(admin),visita,"");
        var detalle=get("/evaluaciones/"+eval+"?aviso=An%C3%A1lisis+guardado.");assertEquals(200,detalle.statusCode());assertFalse(detalle.body().contains("Analizar fotos / añadir análisis"));assertTrue(detalle.body().contains("data-confirmacion"));
        assertEquals(400,get("/evaluaciones/"+eval+"/analizar").statusCode());assertEquals(200,get("/evaluaciones/"+eval+"/analisis/"+id).statusCode());
    }
    @Test void csrfYEntradasInvalidasNoGuardan()throws Exception{
        var f=formulario(4);f.put("foto",""+foto);assertEquals(403,post("/evaluaciones/"+eval+"/analisis",Map.of("csrf","incorrecto","analisis_0",CasosRulaPrueba.codificar(f))).statusCode());
        f.put("BRAZO_grados","NaN");assertEquals(400,post("/evaluaciones/calcular",f).statusCode());f.put("BRAZO_grados","181");assertEquals(400,post("/evaluaciones/calcular",f).statusCode());
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM analisis_rula WHERE id_foto=?",rs->rs.getInt(1),foto));
    }
    @Test void asistenteCompletoGuardaEvaluacionFotoYAnalisisEnElMismoPost()throws Exception{
        var b=new ByteArrayOutputStream();String boundary="AplomoAsistenteCompleto123";var f=new LinkedHashMap<String,String>();f.put("csrf",csrf(get("/evaluaciones/nueva?visita="+visita)));f.put("visita",""+visita);f.put("puesto",""+puesto);f.put("postura","Postura final");f.put("criterio","MAS_INADECUADA");f.put("motivo","Exposición diaria");f.put("minutos","60");f.put("registro","CON_FOTOS");f.put("acepta","si");f.put("asistente_completo","si");f.put("foto_0_vista","PERFIL");f.put("foto_0_descripcion","Vista completa");var entrada=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());entrada.put("foto","0");entrada.put("puntaje_final","1");f.put("analisis_0",CasosRulaPrueba.codificar(entrada));
        for(var p:f.entrySet())b.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+p.getKey()+"\"\r\n\r\n"+p.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
        b.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"foto_0\"; filename=\"foto.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));b.write(png());b.write(("\r\n--"+boundary+"--\r\n").getBytes());
        var r=cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/evaluaciones")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(b.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());assertEquals(201,r.statusCode(),r.body());
        assertEquals(7,Sql.<Integer>uno(con,"SELECT a.puntaje_final FROM evaluacion e JOIN fotografia f USING(id_evaluacion) JOIN analisis_rula a USING(id_foto) WHERE e.id_visita=? ORDER BY e.id_evaluacion DESC LIMIT 1",rs->rs.getInt(1),visita));
    }
}
