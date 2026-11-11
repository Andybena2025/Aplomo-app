package co.aplomo.web;

import co.aplomo.servicio.*;
import co.aplomo.modelo.Puesto;
import co.aplomo.db.Sql;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** RF-014/015/016, RNF-006/007: petición HTTP real con multipart y acceso directo a fotografías. */
class HttpEvaluacionesTest extends co.aplomo.soporte.HttpPrueba {
    int visita,puesto;
    @BeforeEach void visita() throws Exception {
        var salon=crearSalon();puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Prueba","ESCRITORIO","Centro",null,true)).id();
        var r=new Reservas(tx,reloj);int id=r.guardar(cuenta(admin),0,solicitud(salon));r.aceptar(cuenta(admin),id);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),id);
    }
    private Map<String,String> campos(String token) {
        var f=new LinkedHashMap<String,String>();f.put("csrf",token);f.put("visita",""+visita);f.put("puesto",""+puesto);f.put("postura","Tarea <script>inerte</script>");f.put("criterio","MAS_INADECUADA");f.put("motivo","Se mantiene en la jornada");f.put("minutos","120");f.put("observaciones","Nota de prueba");f.put("registro","CON_FOTOS");f.put("acepta","si");return f;
    }
    private byte[] png() throws Exception {var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(10,10,1),"png",b);return b.toByteArray();}
    private HttpResponse<String> enviar(Map<String,String> f,List<byte[]> fotos,String nombre) throws Exception {
        var b=new ByteArrayOutputStream();String boundary="AplomoLimiteMultipart123";
        for(var p:f.entrySet())b.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+p.getKey()+"\"\r\n\r\n"+p.getValue()+"\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for(int i=0;i<fotos.size();i++) {
            for(var p:Map.of("foto_"+i+"_vista","PERFIL","foto_"+i+"_descripcion","Perfil").entrySet())b.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+p.getKey()+"\"\r\n\r\n"+p.getValue()+"\r\n").getBytes());
            b.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"foto_"+i+"\"; filename=\""+nombre+"\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes());b.write(fotos.get(i));b.write("\r\n".getBytes());
        }
        b.write(("--"+boundary+"--\r\n").getBytes());
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/evaluaciones")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(b.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void fotoSinSesionRespondeAccesoDenegadoSinRedirigir() throws Exception {
        var r=get("/fotos/1");assertEquals(403,r.statusCode());assertTrue(r.body().contains("Acceso denegado"));
        assertFalse(r.headers().firstValue("Location").isPresent());
    }
    @Test void guardaConMultipartYFotoProtegidaSeAbreSoloComoAdministrador() throws Exception {
        entrar(admin);var pagina=get("/evaluaciones/nueva?visita="+visita);assertEquals(200,pagina.statusCode());
        assertTrue(pagina.body().contains("data-paso=\"3\""));assertTrue(pagina.body().contains("Escritorio muy alto"));assertFalse(pagina.body().contains("Altura de la superficie"));
        var r=enviar(campos(csrf(pagina)),List.of(png()),"../../nombre_original.jpg");assertEquals(201,r.statusCode(),r.body());
        int id=Sql.uno(con,"SELECT f.id_foto FROM fotografia f JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",rs->rs.getInt(1),visita);
        int evaluacion=Sql.uno(con,"SELECT id_evaluacion FROM evaluacion WHERE id_visita=?",rs->rs.getInt(1),visita);
        var detalle=get("/evaluaciones/"+evaluacion);assertEquals(200,detalle.statusCode());assertTrue(detalle.body().contains("/fotos/"+id));assertTrue(detalle.body().contains("&lt;script&gt;inerte&lt;/script&gt;"));assertFalse(detalle.body().contains("nombre_original"));
        var imagen=cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/fotos/"+id)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,imagen.statusCode());assertEquals("image/png",imagen.headers().firstValue("Content-Type").orElseThrow());assertArrayEquals(png(),imagen.body());
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);
        cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);
        var denegada=get("/fotos/"+id);assertEquals(403,denegada.statusCode());assertTrue(denegada.body().contains("Acceso denegado"));
        assertEquals(403,get("/evaluaciones/nueva?visita="+visita).statusCode());
        assertEquals(403,get("/evaluaciones/"+evaluacion).statusCode());
        assertEquals(403,enviar(campos(csrf(get("/"))),List.of(png()),"foto.jpg").statusCode());
    }
    @ParameterizedTest @ValueSource(strings={"once","pdf","renombrado","seisMB","sinConsentimiento","sinRegistroConFoto"})
    void rechazaArchivosInvalidosDesdeHttpSinGuardarYConMensajeClaro(String caso) throws Exception {
        entrar(admin);var f=campos(csrf(get("/evaluaciones/nueva?visita="+visita)));List<byte[]> fotos=List.of(png());String nombre="foto.png";
        switch(caso){case "once"->fotos=Collections.nCopies(11,png());case "pdf"->{fotos=List.of("%PDF-1.4 prueba".getBytes());nombre="prueba.pdf";}case "renombrado"->{fotos=List.of("%PDF-1.4 prueba".getBytes());nombre="prueba.jpg";}case "seisMB"->fotos=List.of(new byte[6*1024*1024]);case "sinConsentimiento"->f.remove("acepta");case "sinRegistroConFoto"->{f.put("registro","SIN_REGISTRO_FOTOGRAFICO");f.remove("acepta");}}
        var r=enviar(f,fotos,nombre);assertEquals(400,r.statusCode(),r.body());assertTrue(r.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"));assertFalse(r.body().contains("SQLException"));
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",rs->rs.getInt(1),visita));
    }
    @Test void sinFotosEsExplicitoYNoRequiereAceptarLasFotosQueNoExisten() throws Exception {
        entrar(admin);var f=campos(csrf(get("/evaluaciones/nueva?visita="+visita)));f.put("registro","SIN_REGISTRO_FOTOGRAFICO");f.remove("acepta");
        assertEquals(201,enviar(f,List.of(),"foto.jpg").statusCode());
        assertFalse(Sql.<Boolean>uno(con,"SELECT persona_acepto FROM evaluacion WHERE id_visita=?",r->r.getBoolean(1),visita));
    }
    @Test void sinTokenNoGuardaNadaYLaPaginaTieneElJavaScriptDeConservacion() throws Exception {
        entrar(admin);var f=campos("incorrecto");assertEquals(403,enviar(f,List.of(png()),"foto.jpg").statusCode());
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",r->r.getInt(1),visita));
        assertEquals(200,get("/static/js/evaluacion.js").statusCode());
        assertEquals(404,get("/static/../../config/db.properties").statusCode());
    }
}
