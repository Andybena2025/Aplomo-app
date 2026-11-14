package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.*;
import co.aplomo.servicio.*;
import co.aplomo.soporte.CasosRulaPrueba;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Paso 13E-A: recorrido HTTP y reglas definitivas de contexto, consentimiento y visita. */
class HttpRecorridoTest extends co.aplomo.soporte.HttpPrueba {
    Salon salon;int visita,reserva,puesto,planta,inactivo;
    @BeforeEach void contexto() throws Exception {
        salon=crearSalon();var empresas=new Empresas(tx);
        puesto=empresas.guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Computador","ESCRITORIO","Centro",null,true)).id();
        planta=empresas.guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,2,"Ensamble","MESA","Centro",null,true)).id();
        inactivo=empresas.guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,3,"No mostrar","OTRO","Centro",null,true)).id();
        empresas.estadoPuesto(cuenta(admin),inactivo,false);
        var reservas=new Reservas(tx,reloj);reserva=reservas.guardar(cuenta(admin),0,solicitud(salon));reservas.aceptar(cuenta(admin),reserva);
        visita=new Visitas(tx,reloj).iniciar(cuenta(admin),reserva);entrar(admin);
    }
    private Map<String,String> campos() throws Exception {
        var f=new LinkedHashMap<String,String>();f.put("csrf",csrf(get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto)));f.put("visita",""+visita);f.put("puesto",""+puesto);
        f.put("recorrido","guiado");f.put("acepta","si");f.put("postura","Sentado en computador");f.put("criterio","MAS_INADECUADA");f.put("minutos","30");f.put("motivo","No confiar en este texto enviado");f.put("condiciones_respondidas","NINGUNA");f.put("registro","SIN_REGISTRO_FOTOGRAFICO");f.put("asistente_completo","si");return f;
    }
    private HttpResponse<String> enviar(Map<String,String> f,byte[] foto) throws Exception {return enviarVarias(f,foto==null?List.of():List.of(foto));}
    private HttpResponse<String> enviarVarias(Map<String,String> f,List<byte[]> fotos) throws Exception {
        String b="AplomoRecorridoGuiado123";var out=new ByteArrayOutputStream();
        for(var e:f.entrySet())out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\""+e.getKey()+"\"\r\n\r\n"+e.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
        for(int i=0;i<fotos.size();i++){out.write(("--"+b+"\r\nContent-Disposition: form-data; name=\"foto_"+i+"\"; filename=\"foto.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(fotos.get(i));out.write("\r\n".getBytes());}
        out.write(("--"+b+"--\r\n").getBytes());
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/evaluaciones")).header("Content-Type","multipart/form-data; boundary="+b).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    private int cantidad()throws Exception{return Sql.uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",r->r.getInt(1),visita);}
    @Test void visitaListaSoloActivosConEstadoPropioDeEstaVisita() throws Exception {
        int e=evaluacion(visita,salon.id(),1);analisis(foto(e),7);
        var pagina=get("/visitas/"+visita);String h=pagina.body();assertEquals(200,pagina.statusCode());
        assertTrue(h.contains("✓ Evaluado"));assertTrue(h.contains("7 · Rojo · Nivel 4"));assertTrue(h.contains("○ Pendiente"));assertTrue(h.contains("&amp;puesto="+planta));assertFalse(h.contains("&amp;puesto="+inactivo));assertTrue(h.contains("Finalizar visita"));
        Sql.actualizar(con,"UPDATE puesto_trabajo SET ultimo_puntaje=7 WHERE id_puesto=?",planta);
        String tile=get("/visitas/"+visita).body().split("&amp;puesto="+planta+"\"")[1].split("</a>")[0];
        assertTrue(tile.contains("○ Pendiente"));assertFalse(tile.contains("✓ Evaluado"));
    }
    @Test void sinPuestosActivosBloqueaInicioEnInterfazYServidorSinCrearVisita() throws Exception {
        var otro=new Empresas(tx).salon(cuenta(admin),new Empresas(tx).guardar(cuenta(admin),new Salon(0,salon.sede(),"Otro salón","Prueba",1,20,true)).id());var rs=new Reservas(tx,reloj);int id=rs.guardar(cuenta(admin),0,solicitud(otro));rs.aceptar(cuenta(admin),id);
        var pagina=get("/reservas/"+id);assertTrue(pagina.body().contains("disabled aria-describedby=\"sin-puestos\""));assertTrue(pagina.body().contains("Este salón no tiene puestos activos"));assertTrue(pagina.body().contains("/salones/"+otro.id()+"/puestos/nuevo"));
        assertEquals(400,post("/reservas/"+id+"/iniciar-visita",Map.of("csrf",csrf(pagina))).statusCode());assertNull(rs.consultar(cuenta(admin),id).visita());
        int p=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,otro.id(),null,null,1,"Inactivo","MESA","Centro",null,true)).id();new Empresas(tx).estadoPuesto(cuenta(admin),p,false);
        assertThrows(IllegalArgumentException.class,()->new Visitas(tx,reloj).iniciar(cuenta(admin),id));assertNull(rs.consultar(cuenta(admin),id).visita());
        new Empresas(tx).estadoPuesto(cuenta(admin),p,true);assertFalse(get("/reservas/"+id).body().contains("id=\"sin-puestos\""));assertTrue(new Visitas(tx,reloj).iniciar(cuenta(admin),id)>0);
    }
    @ParameterizedTest @CsvSource({"1,Oficina","2,Planta"})
    void contextoDelPuestoDeterminaModoYNoMarcaRespuestas(int numero,String modo)throws Exception {
        int id=numero==1?puesto:planta;String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+id).body();
        assertTrue(h.contains("data-modo>"+modo));assertTrue(h.contains("name=\"puesto\" value=\""+id+"\""));assertTrue(h.contains("La persona firmó en papel el consentimiento para ser evaluada y quedó archivado."));
        assertTrue(h.contains("role=\"radiogroup\""));assertTrue(h.contains("Editar descripción"));assertTrue(h.contains("data-ninguna-condicion"));assertTrue(h.contains("data-paso=\"9\""));
        assertFalse(h.contains("data-piernas checked"));assertTrue(h.contains("data-giro><option value=\"\""));assertTrue(h.contains("data-carga><option value=\"\""));assertTrue(h.contains(numero==1?"postura_computador.jpg":"postura_de_pie.jpg"));assertFalse(h.contains(numero==1?"postura_de_pie.jpg":"postura_computador.jpg"));
    }
    @Test void puestoAjenoOInactivoNoPuedeEntrarPorDireccionManual()throws Exception {
        var otro=new Empresas(tx).salon(cuenta(admin),new Empresas(tx).guardar(cuenta(admin),new Salon(0,salon.sede(),"Otro salón","Prueba",1,20,true)).id());int ajeno=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,otro.id(),null,null,1,"Ajeno","MESA","Centro",null,true)).id();
        assertEquals(400,get("/evaluaciones/nueva?visita="+visita+"&puesto="+ajeno).statusCode());assertEquals(400,get("/evaluaciones/nueva?visita="+visita+"&puesto="+inactivo).statusCode());assertEquals(200,get("/evaluaciones/nueva?visita="+visita).statusCode());
    }
    @ParameterizedTest @ValueSource(strings={"30","90","180","300"})
    void sinFotosGuardaMinutosMotivoAutomaticoYNotaIntegra(String minutos)throws Exception {
        var f=campos();f.put("minutos",minutos);f.put("observaciones","á".repeat(255));f.put("postura","Tarea <b>personalizada</b>");
        var respuesta=enviar(f,null);assertEquals(201,respuesta.statusCode(),respuesta.body());
        var datos=Sql.uno(con,"SELECT e.*,c.otras_observaciones FROM evaluacion e JOIN condiciones_puesto c USING(id_evaluacion) WHERE e.id_visita=?",r->List.of(r.getString("motivo"),r.getString("otras_observaciones"),r.getString("registro_fotografico"),r.getString("minutos_jornada"),r.getString("persona_acepto")),visita);
        assertEquals("Momento: La postura más exigente. Postura: Tarea <b>personalizada</b>.",datos.get(0));assertEquals("á".repeat(255),datos.get(1));assertEquals("SIN_REGISTRO_FOTOGRAFICO",datos.get(2));assertEquals(minutos,datos.get(3));assertEquals("1",datos.get(4));
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM fotografia f JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",r->r.getInt(1),visita));
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",r->r.getInt(1),visita));
    }
    @ParameterizedTest @ValueSource(strings={"CON_FOTOS","SIN_REGISTRO_FOTOGRAFICO"})
    void sinConsentimientoFisicoNoGuardaAunqueSeSalteLaInterfaz(String registro)throws Exception {
        var f=campos();f.put("registro",registro);f.remove("acepta");var respuesta=enviar(f,null);assertEquals(400,respuesta.statusCode());assertTrue(respuesta.body().contains("consentimiento para ser evaluada"));assertEquals(0,cantidad());
    }
    @ParameterizedTest @ValueSource(strings={"tiempo","sinCondiciones","ningunaYCondicion","fotoSinRegistro"})
    void rechazaContextoIncompletoOContradictorioSinPerderValidaciones(String caso)throws Exception {
        var f=campos();byte[] foto=null;
        switch(caso){case "tiempo"->f.put("minutos","120");case "sinCondiciones"->f.remove("condiciones_respondidas");case "ningunaYCondicion"->f.put("pies","si");case "fotoSinRegistro"->foto=new byte[]{1,2,3};}
        assertEquals(400,enviar(f,foto).statusCode());assertEquals(0,cantidad());
    }
    @ParameterizedTest @ValueSource(strings={"giro","piernasApoyadas","actividad","carga","hombroElevado"})
    void analisisProvisionalNoInventaRespuestasAusentes(String campo)throws Exception {
        var f=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());f.put("csrf",csrf(get("/evaluaciones/nueva?visita="+visita)));f.put("visita",""+visita);f.remove(campo);
        assertEquals(400,post("/evaluaciones/calcular",f).statusCode());assertEquals(0,cantidad());
    }
    @Test void puenteActualSigueCalculandoYGuardandoElPuntajeDelServidor()throws Exception {
        var f=campos();f.put("registro","CON_FOTOS");f.put("foto_0_vista","PERFIL");f.put("foto_0_descripcion","Perfil de ensayo");var a=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(7).entrada());a.put("foto","0");f.put("analisis_0",CasosRulaPrueba.codificar(a));f.put("puntaje_final","1");
        var out=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(40,80,1),"png",out);
        var respuesta=enviar(f,out.toByteArray());assertEquals(201,respuesta.statusCode(),respuesta.body());assertEquals(7,Sql.<Integer>uno(con,"SELECT a.puntaje_final FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=?",r->r.getInt(1),visita));
    }
    @Test void usuarioNoPuedeAbrirNiEnviarRecorridoNiConsultarPuestosDeVisitaEnCurso()throws Exception {
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();entrar(usuario);
        assertEquals(403,get("/visitas/"+visita).statusCode());assertEquals(403,get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).statusCode());
        assertThrows(SecurityException.class,()->new Visitas(tx,reloj).puestos(cuenta(usuario),visita));assertEquals(0,cantidad());
    }
    @ParameterizedTest @CsvSource({"1,computador;sentado;inclinado;brazos_arriba;herramienta;otra","2,de_pie;cargando;inclinado;brazos_arriba;herramienta;agachado;otra"})
    void oficinaYPlantaMuestranListasExactasDistintas(int numero,String lista)throws Exception{
        String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+(numero==1?puesto:planta)).body();
        var m=java.util.regex.Pattern.compile("data-postura=\"([^\"]+)\"").matcher(h);List<String> claves=new ArrayList<>();while(m.find())claves.add(m.group(1));assertEquals(List.of(lista.split(";")),claves);
        String otra=h.substring(h.lastIndexOf("<label",h.indexOf("data-postura=\"otra\"")),h.indexOf("</label>",h.indexOf("data-postura=\"otra\"")));assertFalse(otra.contains("<img"));assertTrue(otra.contains("Otra postura"));assertTrue(h.contains("Descríbela con tus palabras"));assertFalse(h.contains(" checked"));
    }
    @Test void otraPosturaExigeTextoYConservaSus150Caracteres()throws Exception{
        var f=campos();f.put("postura","");assertEquals(400,enviar(f,null).statusCode());assertEquals(0,cantidad());
        f.put("postura","á".repeat(151));assertEquals(400,enviar(f,null).statusCode());assertEquals(0,cantidad());
        String texto="Evaluación personalizada "+"á".repeat(125);assertEquals(150,texto.length());f.put("postura",texto);var r=enviar(f,null);assertEquals(201,r.statusCode(),r.body());assertEquals(texto,Sql.<String>uno(con,"SELECT postura FROM evaluacion WHERE id_visita=?",rs->rs.getString(1),visita));
    }
    @ParameterizedTest @CsvSource({"MAS_INADECUADA,Lo más difícil del puesto","MAYOR_EXPOSICION,Lo que más dura"})
    void momentoCambiaEtiquetaYConservaValorGuardado(String criterio,String etiqueta)throws Exception{
        String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();assertTrue(h.contains("¿Qué vas a fotografiar?"));assertTrue(h.contains(etiqueta));assertTrue(h.contains("Solo se evalúa una postura por foto. Elige una."));assertFalse(h.contains("¿Qué momento vas a evaluar?"));
        var f=campos();f.put("criterio",criterio);f.put("minutos","180");var r=enviar(f,null);assertEquals(201,r.statusCode(),r.body());assertEquals(criterio,Sql.<String>uno(con,"SELECT criterio FROM evaluacion WHERE id_visita=?",rs->rs.getString(1),visita));assertEquals(180,Sql.<Integer>uno(con,"SELECT minutos_jornada FROM evaluacion WHERE id_visita=?",rs->rs.getInt(1),visita));
    }
    @Test void fotosUsanUnSoloRecorridoSinDescripcionNiClasificacionManuales()throws Exception{
        String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();for(String pantalla:List.of("permiso","perfil","frente","detalle","resumen"))assertEquals(1,h.split("data-foto-pantalla=\""+pantalla+"\"",-1).length-1);
        assertTrue(h.contains("Omitir esta foto"));assertTrue(h.contains("Agregar otra foto"));assertTrue(h.contains("data-foto-otro-lado"));assertFalse(h.contains("slotImg"));assertFalse(h.contains("Las imágenes de muestra son de demostración"));assertFalse(h.contains("data-uso-guardada"));String fotos=h.substring(h.indexOf("data-fotos-guiadas"),h.indexOf("data-paso=\"7\""));assertFalse(fotos.contains("<textarea"));assertFalse(fotos.contains("data-uso-guardada"));
    }
    @ParameterizedTest @ValueSource(strings={"FRENTE","OTRA"})
    void perfilEsObligatorioAunqueLleguenOtrasTomas(String vista)throws Exception{
        var f=campos();f.put("registro","CON_FOTOS");f.put("foto_0_vista",vista);f.put("foto_0_descripcion",vista);var out=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(40,80,1),"png",out);
        var r=enviar(f,out.toByteArray());assertEquals(400,r.statusCode(),r.body());assertTrue(r.body().contains("foto de perfil"));assertEquals(0,cantidad());
    }
    @Test void metadatosAutomaticosYFotosDeApoyoSeGuardanSinAnalisis()throws Exception{
        var f=campos();f.put("registro","CON_FOTOS");String[] vistas={"PERFIL","FRENTE","OTRA","PERFIL","FRENTE","OTRA"},descripciones={"Perfil","Frente","Detalle de mano y muñeca","Perfil · otro lado","Frente · otro lado","Foto adicional 1"};
        var out=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(40,80,1),"png",out);List<byte[]> archivos=new ArrayList<>();int indice=0;
        for(int i=0;i<vistas.length;i++){f.put("foto_"+i+"_vista",vistas[i]);f.put("foto_"+i+"_descripcion",descripciones[i]);archivos.add(out.toByteArray());if(!vistas[i].equals("OTRA")){var a=CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(0).entrada());a.put("foto",""+i);f.put("analisis_"+(indice++),CasosRulaPrueba.codificar(a));}}
        var r=enviarVarias(f,archivos);assertEquals(201,r.statusCode(),r.body());var filas=Sql.listar(con,"SELECT f.vista,f.descripcion,(SELECT COUNT(*) FROM analisis_rula a WHERE a.id_foto=f.id_foto) FROM fotografia f JOIN evaluacion e USING(id_evaluacion) WHERE e.id_visita=? ORDER BY f.id_foto",rs->List.of(rs.getString(1),rs.getString(2),rs.getString(3)),visita);assertEquals(6,filas.size());for(int i=0;i<6;i++)assertEquals(List.of(vistas[i],descripciones[i],vistas[i].equals("OTRA")?"0":"1"),filas.get(i));
    }
    @Test void zoomTieneControlesAccesiblesSinCamposDeCalculoNuevos()throws Exception{
        String h=get("/evaluaciones/nueva?visita="+visita+"&puesto="+puesto).body();for(String texto:List.of("data-zoom-menos","data-zoom-nivel","data-zoom-mas","aria-label=\"Alejar foto\"","aria-label=\"Acercar foto\""))assertTrue(h.contains(texto));assertFalse(h.contains("name=\"zoom\""));assertTrue(h.contains("class=\"zoom-lamina\""));
    }

}
