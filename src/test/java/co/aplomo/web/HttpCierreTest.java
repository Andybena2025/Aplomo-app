package co.aplomo.web;

import co.aplomo.dao.*;import co.aplomo.db.Sql;import co.aplomo.modelo.*;import co.aplomo.servicio.*;import co.aplomo.soporte.*;import co.aplomo.rula.MotorRula;
import org.junit.jupiter.api.*;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.*;
import java.util.*;import java.io.*;import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Cierre del recorrido: ayuda, cargas exactas, siguiente puesto real y permisos. */
class HttpCierreTest extends HttpPrueba {
    int visita,primero,segundo,evaluacion,foto;Evaluaciones servicio;
    @BeforeEach void contexto()throws Exception{
        var salon=crearSalon();var empresas=new Empresas(tx);
        primero=empresas.guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Computador","ESCRITORIO","Centro",null,true)).id();
        segundo=empresas.guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,2,"Ensamble","MESA","Centro",null,true)).id();
        int inactivo=empresas.guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,3,"Inactivo","MESA","Centro",null,true)).id();empresas.estadoPuesto(cuenta(admin),inactivo,false);
        var rs=new Reservas(tx,reloj);int reserva=rs.guardar(cuenta(admin),0,solicitud(salon));rs.aceptar(cuenta(admin),reserva);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),reserva);
        servicio=new Evaluaciones(tx,reloj,new Fotos(carpetaFotos),new BitacoraDao());
        var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(200,400,1),"png",b);
        evaluacion=servicio.guardar(cuenta(admin),visita,datos(primero),List.of(new Fotos.Entrada(b.toByteArray(),"PERFIL","Perfil completo")));
        foto=new EvaluacionDao().fotos(con,evaluacion).get(0).id();entrar(admin);
    }
    Evaluaciones.Datos datos(int p){return new Evaluaciones.Datos(p,"Ensamble","MAS_INADECUADA","",90,false,false,false,false,false,false,"",true,false);}
    Map<String,String> analisis(String destino)throws Exception{
        var r=get("/evaluaciones/"+evaluacion+"/analizar");var f=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());f.put("foto",""+foto);
        return new LinkedHashMap<>(Map.of("csrf",csrf(r),"destino_guardado",destino,"analisis_0",CasosRulaPrueba.codificar(f)));
    }
    @Test void ayudaDeLadoApareceEnLaPantallaDeLadoSinCambiarLaPregunta()throws Exception{
        String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+segundo).body();String lado=h.substring(h.indexOf("data-paso=\"7\""),h.indexOf("data-paso=\"8\""));
        assertTrue(lado.contains("<h2>¿Qué lado tiene más carga?</h2>"));assertTrue(lado.contains("data-ayuda-lado"));
        for(String texto:List.of("¿Cómo saber qué lado tiene más carga? Observa varios ciclos de la tarea:","¿Con qué mano sostiene la herramienta, la pieza o la carga?","¿Qué brazo se levanta más o se aleja más del cuerpo?","¿Qué brazo repite más el movimiento?","¿Qué muñeca se dobla o gira más?","Si no está claro, elige «Ambos»."))assertTrue(lado.contains(texto),texto);
        assertFalse(lado.contains(" checked"));
    }
    @ParameterizedTest @EnumSource(MotorRula.Carga.class)
    void lasSeisCargasTienenSuMosaicoExactoYSuPuntajeActual(MotorRula.Carga carga)throws Exception{
        var mosaico=Arrays.stream(RecorridoCuerpo.CARGAS).filter(c->c[0].equals(carga.name())).findFirst().orElseThrow();assertNotNull(getClass().getResource("/static/img/tour/"+mosaico[2]));
        var f=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());f.put("csrf",csrf(get("/evaluaciones/nueva?visita="+visita)));f.put("visita",""+visita);f.put("carga",carga.name());var r=post("/evaluaciones/calcular",f);
        assertEquals(200,r.statusCode(),r.body());assertTrue(r.body().contains("+0 · +"+carga.puntos()));assertTrue(r.body().contains("Ver desglose del cálculo"));assertFalse(r.body().contains("<details open"));assertTrue(r.body().contains("Sugerencias · aparte del puntaje"));
    }
    @Test void anadirAnalisisComparteCuerpoEsfuerzoYResultadoSinValoresIniciales()throws Exception{
        var r=get("/evaluaciones/"+evaluacion+"/analizar");assertEquals(200,r.statusCode());String h=r.body();
        for(String texto:List.of("data-tour=\"analisis\"","data-cuerpo=\"si\"","data-uso-guardada","data-ayuda-lado","esfuerzo_estatica.png","esfuerzo_repetitiva.png","esfuerzo_ninguno.png","carga_1.png","carga_4.png","data-paso=\"11\"","Guardar y evaluar siguiente puesto","Guardar y volver a la visita"))assertTrue(h.contains(texto),texto);
        assertFalse(h.contains(" checked"));assertFalse(h.contains("data-puntaje-final"));
    }
    @Test void siguienteEsActivoYPendienteDeEstaVisitaInclusoSinPuntaje()throws Exception{
        assertEquals(segundo,servicio.siguientePendiente(cuenta(admin),visita));
        servicio.guardar(cuenta(admin),visita,new Evaluaciones.Datos(segundo,"Observación","MAS_INADECUADA","",90,false,false,false,false,false,false,"",true,true),List.of());
        assertEquals(0,servicio.siguientePendiente(cuenta(admin),visita));
    }
    @Test void guardarAnalisisYSiguienteAbreElPuestoPendienteEnLaMismaVisita()throws Exception{
        var r=post("/evaluaciones/"+evaluacion+"/analisis",analisis("SIGUIENTE"));assertEquals(201,r.statusCode(),r.body());assertEquals("/evaluaciones/nueva?visita="+visita+"&puesto="+segundo,r.headers().firstValue("Location").orElseThrow());assertEquals(1,servicio.analisis().listar(cuenta(admin),evaluacion).size());
    }
    @Test void guardarYVolverVuelveALaVisitaSinCrearOtraEvaluacion()throws Exception{
        var r=post("/evaluaciones/"+evaluacion+"/analisis",analisis("VOLVER"));assertEquals(201,r.statusCode());assertTrue(r.headers().firstValue("Location").orElseThrow().startsWith("/visitas/"+visita+"?aviso="));assertEquals(1,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",rs->rs.getInt(1),visita));
    }
    @Test void ultimoPuestoVuelveConTodosEvaluados()throws Exception{
        servicio.guardar(cuenta(admin),visita,new Evaluaciones.Datos(segundo,"Observación","MAS_INADECUADA","",90,false,false,false,false,false,false,"",true,true),List.of());
        var r=post("/evaluaciones/"+evaluacion+"/analisis",analisis("SIGUIENTE"));assertEquals(201,r.statusCode());String loc=URLDecoder.decode(r.headers().firstValue("Location").orElseThrow(),StandardCharsets.UTF_8);assertEquals("/visitas/"+visita+"?aviso=Todos los puestos están evaluados",loc);
    }
    @ParameterizedTest @ValueSource(strings={"SIGUIENTE","VOLVER"})
    void nuevaEvaluacionSinFotosGuardaYRespetaElDestino(String destino)throws Exception{
        var f=new LinkedHashMap<String,String>(Map.of("csrf",csrf(get("/evaluaciones/nueva?visita="+visita+"&puesto="+segundo)),"visita",""+visita,"puesto",""+segundo,"postura","Ensamble","criterio","MAS_INADECUADA","minutos","90","registro","SIN_REGISTRO_FOTOGRAFICO","acepta","si","asistente_completo","si","destino_guardado",destino));
        String limite="CierreSinFotos123";var b=new ByteArrayOutputStream();for(var e:f.entrySet())b.write(("--"+limite+"\r\nContent-Disposition: form-data; name=\""+e.getKey()+"\"\r\n\r\n"+e.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));b.write(("--"+limite+"--\r\n").getBytes());
        var r=cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/evaluaciones")).header("Content-Type","multipart/form-data; boundary="+limite).POST(HttpRequest.BodyPublishers.ofByteArray(b.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());assertEquals(201,r.statusCode(),r.body());String loc=URLDecoder.decode(r.headers().firstValue("Location").orElseThrow(),StandardCharsets.UTF_8);assertTrue(loc.startsWith("/visitas/"+visita+"?aviso="));if(destino.equals("SIGUIENTE"))assertTrue(loc.endsWith("Todos los puestos están evaluados"));assertEquals(0,servicio.siguientePendiente(cuenta(admin),visita));
    }
    @Test void destinoManipuladoSeRechazaAntesDeGuardar()throws Exception{
        var r=post("/evaluaciones/"+evaluacion+"/analisis",analisis("https://ajeno.invalid"));assertEquals(400,r.statusCode());assertTrue(servicio.analisis().listar(cuenta(admin),evaluacion).isEmpty());
    }
    @Test void usuarioNoPuedeAbrirNiGuardarElNuevoRecorrido()throws Exception{
        var f=analisis("SIGUIENTE");dao.contrasena(con,usuario,cuenta(usuario).hash(),false);cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);
        assertEquals(403,get("/evaluaciones/"+evaluacion+"/analizar").statusCode());assertEquals(403,post("/evaluaciones/"+evaluacion+"/analisis",f).statusCode());assertThrows(SecurityException.class,()->servicio.siguientePendiente(cuenta(usuario),visita));
    }
}
