package co.aplomo.web;

import co.aplomo.servicio.*;import co.aplomo.dao.*;import co.aplomo.db.Sql;import co.aplomo.modelo.*;import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;import java.io.*;import java.util.*;import java.net.*;import java.net.http.*;import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** RF-022, RNF-004/007: acciones HTTP, alcance de empresa y rechazo de cambios por URL o rol incorrecto. */
class HttpPendientesTest extends HttpPrueba {
    Evaluaciones evals;int visita,eval,pendiente;
    @BeforeEach void contexto()throws Exception{
        evals=new Evaluaciones(tx,reloj,new Fotos(carpetaFotos),new BitacoraDao());var salon=crearSalon();int puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Ensamble","ESCRITORIO","Centro",null,true)).id();
        var r=new Reservas(tx,reloj);int reserva=r.guardar(cuenta(admin),0,solicitud(salon));r.aceptar(cuenta(admin),reserva);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),reserva);
        var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(12,12,1),"png",b);
        var d=new Evaluaciones.Datos(puesto,"Postura","MAS_INADECUADA","Trabajo mantenido",60,false,false,false,false,false,false,"",true,false);
        eval=evals.guardar(cuenta(admin),visita,d,List.of(new Fotos.Entrada(b.toByteArray(),"PERFIL","Perfil")),List.of(new Analisis.Solicitud(0,CasosRulaPrueba.todos().get(6).entrada())));pendiente=new PendienteDao().porEvaluacion(con,eval).id();entrar(admin);
    }
    @Test void editarEscapaTextoYNoAceptaCambiarPuntajeNiEstado()throws Exception{
        var editar=get("/pendientes/"+pendiente+"/editar");assertEquals(200,editar.statusCode());
        var f=new HashMap<>(Map.of("csrf",csrf(editar),"descripcion","<script>inocuo</script>\nAjustar mesa","prioridad","ALTA","fecha","2026-11-25","puntaje","1","estado","ARREGLADO"));
        assertEquals(303,post("/pendientes/"+pendiente,f).statusCode());var p=evals.pendientes().consultar(cuenta(admin),pendiente).pendiente();assertEquals(6,p.puntaje());assertEquals("ABIERTO",p.estado());
        var detalle=get("/pendientes/"+pendiente);assertTrue(detalle.body().contains("&lt;script&gt;inocuo&lt;/script&gt;"));assertFalse(detalle.body().contains("<script>inocuo"));assertTrue(detalle.body().contains("25 nov 2026"));
        assertFalse(detalle.body().contains("/fotos/"));assertTrue(get("/pendientes?estado=TODOS&orden=limite").body().contains("Ver pendiente"));
    }
    @Test void arregladoSoloPorPostConCsrfYUnaSolaAuditoria()throws Exception{
        assertEquals(405,get("/pendientes/"+pendiente+"/arreglado").statusCode());assertEquals(403,post("/pendientes/"+pendiente+"/arreglado",Map.of("csrf","incorrecto")).statusCode());
        String token=csrf(get("/pendientes/"+pendiente));assertEquals(303,post("/pendientes/"+pendiente+"/arreglado",Map.of("csrf",token)).statusCode());assertEquals(303,post("/pendientes/"+pendiente+"/arreglado",Map.of("csrf",token)).statusCode());
        assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora WHERE tabla='pendiente_arreglo' AND id_registro=? AND accion='CAMBIAR_ESTADO'",r->r.getInt(1),pendiente));assertFalse(get("/pendientes/"+pendiente).body().contains("Marcar como arreglado"));
    }
    @Test void erroresConservanCamposYExplicanQueCorregir()throws Exception{
        String token=csrf(get("/pendientes/"+pendiente+"/editar"));var r=post("/pendientes/"+pendiente,Map.of("csrf",token,"descripcion","Conservar mi descripción","prioridad","ALTA","fecha","0001-01-01"));assertEquals(400,r.statusCode());assertTrue(r.body().contains("Conservar mi descripción"));assertTrue(r.body().contains("fecha válida"));assertTrue(r.body().contains("value=\"ALTA\" selected"));assertTrue(r.body().contains("0001-01-01"));assertFalse(r.body().contains("SQLException"));
        assertEquals(400,get("/pendientes?estado=OTRO").statusCode());assertEquals(400,get("/pendientes?orden=xxx").statusCode());
    }
    @Test void ajaxGuardaYDevuelveDestinoSinRecargarElFormularioEnError()throws Exception{
        String token=csrf(get("/pendientes/"+pendiente+"/editar"));String cuerpo="csrf="+URLEncoder.encode(token,java.nio.charset.StandardCharsets.UTF_8)+"&descripcion=Conservar&prioridad=MEDIA&fecha=";
        var r=cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/pendientes/"+pendiente)).header("Content-Type","application/x-www-form-urlencoded").header("Accept","text/plain").POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(201,r.statusCode());assertTrue(r.headers().firstValue("Location").orElseThrow().startsWith("/pendientes/"+pendiente));assertEquals(200,get("/static/js/pendientes.js").statusCode());
    }
    @Test void usuarioNoPuedeEditarNiArreglarPorDireccionDirecta()throws Exception{
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);
        assertEquals(403,get("/pendientes/"+pendiente).statusCode());new Visitas(tx,reloj).finalizar(cuenta(admin),visita,"");var detalle=get("/pendientes/"+pendiente);assertEquals(200,detalle.statusCode());assertFalse(detalle.body().contains("Editar seguimiento"));assertFalse(detalle.body().contains("Marcar como arreglado"));assertFalse(detalle.body().contains("/fotos/"));
        assertEquals(403,get("/pendientes/"+pendiente+"/editar").statusCode());String token=csrf(get("/"));assertEquals(403,post("/pendientes/"+pendiente,Map.of("csrf",token,"descripcion","No permitido")).statusCode());assertEquals(403,post("/pendientes/"+pendiente+"/arreglado",Map.of("csrf",token)).statusCode());
        assertTrue(get("/pendientes").body().contains("Ver pendiente"));
    }
    @Test void estadosVerdeYAmarilloSeMuestranSinCrearPendientes()throws Exception{
        for(int numero:new int[]{1,2}){var c=CasosRulaPrueba.todos().get(numero-1);var f=CasosRulaPrueba.campos(c.entrada());f.put("csrf",csrf(get("/evaluaciones/"+eval+"/analizar")));f.put("visita",""+visita);var r=post("/evaluaciones/calcular",f);assertEquals(200,r.statusCode());assertTrue(r.body().contains(numero==1?"Aceptable":"En observación"));}
        assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM pendiente_arreglo WHERE id_evaluacion=?",r->r.getInt(1),eval));
    }
}
