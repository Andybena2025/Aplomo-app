package co.aplomo.servicio;

import co.aplomo.dao.*;import co.aplomo.db.Sql;import co.aplomo.modelo.*;import co.aplomo.rula.*;import co.aplomo.rula.MotorRula.*;import co.aplomo.rula.MedidorAngulos.*;import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;import java.io.*;import java.sql.*;import java.util.*;import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** RF-017–021, RNF-004/005/009/010: cálculo, persistencia, máximos e historial con MySQL real aislado. */
class AnalisisTest extends BasePrueba {
    @TempDir Path carpeta;Evaluaciones evaluaciones;Analisis servicio;int visita,puesto,eval,foto;
    @BeforeEach void contexto()throws Exception{
        evaluaciones=new Evaluaciones(tx,reloj,new Fotos(carpeta),new BitacoraDao());servicio=evaluaciones.analisis();var salon=crearSalon();
        puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Ensamble","ESCRITORIO","Centro",null,true)).id();
        var rs=new Reservas(tx,reloj);int r=rs.guardar(cuenta(admin),0,solicitud(salon));rs.aceptar(cuenta(admin),r);visita=new Visitas(tx,reloj).iniciar(cuenta(admin),r);
        eval=evaluaciones.guardar(cuenta(admin),visita,datos(false),List.of(imagen()));foto=new EvaluacionDao().fotos(con,eval).get(0).id();
    }
    private Evaluaciones.Datos datos(boolean sin){return new Evaluaciones.Datos(puesto,"Ensamble","MAS_INADECUADA","Postura mantenida",120,false,false,false,false,false,false,"",!sin,sin);}
    private Fotos.Entrada imagen()throws Exception{var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(200,400,1),"png",b);return new Fotos.Entrada(b.toByteArray(),"PERFIL","Vista completa");}
    private Entrada caso(int n){return CasosRulaPrueba.todos().get(n-1).entrada();}
    private void guardar(int n)throws Exception{servicio.guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,caso(n))));}
    private int cantidad()throws SQLException{return Sql.uno(con,"SELECT COUNT(*) FROM analisis_rula WHERE id_foto=?",r->r.getInt(1),foto);}
    @TestFactory java.util.stream.Stream<DynamicTest> diezCasosConVistaPreviaGuardadoYLectura(){return CasosRulaPrueba.todos().stream().map(c->DynamicTest.dynamicTest("Caso "+c.numero(),()->{
        var previa=servicio.previsualizar(cuenta(admin),visita,c.entrada());assertEquals(c.esperado(),previa.resultado().puntajeFinal());
        servicio.guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,c.entrada())));var historial=servicio.listar(cuenta(admin),eval);var guardado=servicio.consultar(cuenta(admin),eval,historial.get(0).id());
        assertEquals(c.esperado(),guardado.ficha().fin());assertEquals(c.esperado(),guardado.calculo().resultado().puntajeFinal());assertEquals(5,guardado.calculo().mediciones().size());assertTrue(guardado.calculo().mediciones().stream().allMatch(Medida::manual));
    }));}
    @Test void repetirNoReemplazaYLaUltimaEvaluacionManda()throws Exception{
        guardar(8);guardar(4);assertEquals(2,cantidad());assertEquals(7,Sql.<Integer>uno(con,"SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",r->r.getInt(1),puesto));
        int nueva=evaluaciones.guardar(cuenta(admin),visita,datos(false),List.of(imagen()),List.of(new Analisis.Solicitud(0,caso(4))));assertTrue(nueva>eval);
        guardar(10);assertEquals(3,cantidad());assertEquals(1,Sql.<Integer>uno(con,"SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",r->r.getInt(1),puesto));
    }
    @Test void ladosIndependientesYResponsable()throws Exception{
        guardar(4);var d=caso(8);var izquierda=new Entrada(Lado.IZQUIERDO,d.ancho(),d.alto(),d.mediciones(),d.ajustes(),d.actividad(),d.carga());
        servicio.guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,izquierda)));var h=servicio.listar(cuenta(admin),eval);assertEquals(2,h.size());assertEquals("IZQUIERDO",h.get(0).lado());assertEquals("DERECHO",h.get(1).lado());
        assertEquals(admin,Sql.<Integer>uno(con,"SELECT id_cuenta FROM bitacora WHERE tabla='analisis_rula' AND id_registro=?",r->r.getInt(1),h.get(0).id()));
    }
    @Test void validaRelacionPermisosYVisitaCerrada()throws Exception{
        int otra=evaluaciones.guardar(cuenta(admin),visita,datos(false),List.of(imagen()));int ajena=new EvaluacionDao().fotos(con,otra).get(0).id();
        assertThrows(SecurityException.class,()->servicio.guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(ajena,caso(4)))));assertEquals(0,cantidad());
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);assertThrows(SecurityException.class,()->servicio.previsualizar(cuenta(usuario),visita,caso(4)));assertThrows(SecurityException.class,()->servicio.guardar(cuenta(usuario),eval,List.of(new Analisis.Solicitud(foto,caso(4)))));
        guardar(4);int id=servicio.listar(cuenta(admin),eval).get(0).id();assertThrows(SecurityException.class,()->servicio.consultar(cuenta(usuario),eval,id));
        new Visitas(tx,reloj).finalizar(cuenta(admin),visita,"");assertThrows(IllegalArgumentException.class,()->guardar(4));assertThrows(IllegalArgumentException.class,()->servicio.previsualizar(cuenta(admin),visita,caso(4)));assertEquals(1,servicio.consultar(cuenta(admin),eval,id).ficha().fin());
    }
    @Test void fallaTodoSinDejarEvaluacionFotoNiArchivoNuevos()throws Exception{
        int antes=Sql.uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",r->r.getInt(1),visita);
        assertThrows(IllegalArgumentException.class,()->evaluaciones.guardar(cuenta(admin),visita,datos(false),List.of(imagen()),List.of(new Analisis.Solicitud(10,caso(4)))));
        assertEquals(antes,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",r->r.getInt(1),visita));try(var fs=Files.list(carpeta)){assertEquals(1,fs.count());}
        assertThrows(IllegalArgumentException.class,()->evaluaciones.guardar(cuenta(admin),visita,datos(true),List.of(),List.of(new Analisis.Solicitud(0,caso(4)))));
    }
    @Test void falloDeBitacoraRevierteAnalisisYMediciones()throws Exception{
        var falla=new BitacoraDao(){@Override public void registrar(Connection c,int actor,java.time.LocalDateTime fecha,String tabla,int id,String accion,String a,String n,String detalle)throws SQLException{throw new SQLException("Fallo simulado");}};
        assertThrows(SQLException.class,()->new Analisis(tx,reloj,new Fotos(carpeta),falla).guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,caso(4)))));assertEquals(0,cantidad());
    }
    @Test void calculaPuntosConDimensionesRealesAunqueElClienteMienta()throws Exception{
        var p=List.of(new Punto(.5,.9),new Punto(.5,.5),new Punto(.75,.75));
        var d=caso(4);var m=new EnumMap<Segmento,Medicion>(d.mediciones());m.put(Segmento.BRAZO,new Medicion(p,null));
        var entrada=new Entrada(d.lado(),1,1,m,d.ajustes(),false,d.carga());
        servicio.guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,entrada)));int id=servicio.listar(cuenta(admin),eval).get(0).id();var guardado=servicio.consultar(cuenta(admin),eval,id);
        var brazo=guardado.calculo().mediciones().stream().filter(x->x.segmento()==Segmento.BRAZO).findFirst().orElseThrow();
        assertEquals(26.57,brazo.grados(),.001);assertFalse(brazo.manual());assertTrue(brazo.puntosMarcados().contains("0.750000"));
    }
    @Test void jpgDelTelefonoRespetaLaOrientacionExifSinCambiarSusBytes()throws Exception{
        var salida=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(200,400,1),"jpg",salida);byte[] original=salida.toByteArray();
        byte[] exif=new byte[]{(byte)255,(byte)225,0,34,'E','x','i','f',0,0,'I','I',42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,6,0,0,0,0,0,0,0};
        var b=new ByteArrayOutputStream();b.write(original,0,2);b.write(exif);b.write(original,2,original.length-2);
        var fotos=new Fotos(carpeta);var validada=fotos.validar(List.of(new Fotos.Entrada(b.toByteArray(),"PERFIL","Girado"))).get(0);String nombre=fotos.guardar(validada);
        assertArrayEquals(new int[]{400,200},fotos.dimensiones(nombre));assertArrayEquals(b.toByteArray(),fotos.leer(nombre));
    }
    @Test void diezFotosGuardanCincuentaMedicionesSinMezclarse()throws Exception{
        List<Fotos.Entrada> fs=new ArrayList<>();List<Analisis.Solicitud> entradas=new ArrayList<>();
        for(var c:CasosRulaPrueba.todos()){fs.add(imagen());entradas.add(new Analisis.Solicitud(c.numero()-1,c.entrada()));}
        int id=evaluaciones.guardar(cuenta(admin),visita,datos(false),fs,entradas);var fotos=new EvaluacionDao().fotos(con,id);assertEquals(10,fotos.size());
        for(int i=0;i<10;i++)assertEquals(CasosRulaPrueba.todos().get(i).esperado(),Sql.<Integer>uno(con,"SELECT puntaje_final FROM analisis_rula WHERE id_foto=?",r->r.getInt(1),fotos.get(i).id()));
        assertEquals(50,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM medicion_angulo m JOIN analisis_rula a USING(id_analisis) JOIN fotografia f USING(id_foto) WHERE f.id_evaluacion=?",r->r.getInt(1),id));
    }
    @Test void tarjetasNoRevelanResultadosDeVisitasEnCursoAlUsuario()throws Exception{
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);long antes=evaluaciones.resumen(cuenta(admin))[1];guardar(8);
        assertTrue(evaluaciones.resumen(cuenta(admin))[0]>=1);assertEquals(antes+1,evaluaciones.resumen(cuenta(admin))[1]);assertArrayEquals(new long[]{0,0},evaluaciones.resumen(cuenta(usuario)));
        new Visitas(tx,reloj).finalizar(cuenta(admin),visita,"");assertArrayEquals(new long[]{1,1},evaluaciones.resumen(cuenta(usuario)));
    }
    @Test void previsualizarNoEscribeDatos()throws Exception{assertEquals(7,servicio.previsualizar(cuenta(admin),visita,caso(8)).resultado().puntajeFinal());assertEquals(0,cantidad());}
    @ParameterizedTest @ValueSource(strings={"falta","infinito","fuera","coinciden","angulo"})
    void noCalculaEntradasImposibles(String caso)throws Exception{
        var d=caso(4);var ms=new EnumMap<Segmento,Medicion>(d.mediciones());
        switch(caso){case "falta"->ms.remove(Segmento.BRAZO);case "infinito"->ms.put(Segmento.BRAZO,new Medicion(List.of(),Double.NaN));case "angulo"->ms.put(Segmento.ANTEBRAZO,new Medicion(List.of(),181.0));case "fuera"->ms.put(Segmento.BRAZO,new Medicion(List.of(new Punto(-.1,.1),new Punto(.2,.2),new Punto(.3,.3)),null));case "coinciden"->ms.put(Segmento.BRAZO,new Medicion(List.of(new Punto(.2,.2),new Punto(.2,.2),new Punto(.3,.3)),null));}
        var entrada=new Entrada(d.lado(),d.ancho(),d.alto(),ms,d.ajustes(),false,d.carga());assertThrows(IllegalArgumentException.class,()->servicio.guardar(cuenta(admin),eval,List.of(new Analisis.Solicitud(foto,entrada))));assertEquals(0,cantidad());
    }
}
