package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.Sql;
import co.aplomo.modelo.*;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** RF-014/015/016, RNF-006/007: evaluación y archivos reales aislados, sin confirmar datos. */
class EvaluacionesTest extends BasePrueba {
    @TempDir Path carpeta;
    Evaluaciones servicio;
    Salon salon;
    int visita, puesto;
    @BeforeEach void datos() throws Exception {
        servicio=new Evaluaciones(tx,reloj,new Fotos(carpeta),new BitacoraDao());salon=crearSalon();
        puesto=new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Puesto","ESCRITORIO","Centro",null,true)).id();
        var reservas=new Reservas(tx,reloj);int r=reservas.guardar(cuenta(admin),0,solicitud(salon));reservas.aceptar(cuenta(admin),r);
        visita=new Visitas(tx,reloj).iniciar(cuenta(admin),r);
    }
    private Evaluaciones.Datos datos(boolean acepta,boolean sin) {
        return new Evaluaciones.Datos(puesto,"Ensamblar <script>inerte</script>","MAS_INADECUADA","Se mantiene durante la tarea",120,
                true,false,true,true,false,true,"Primera línea\nSegunda línea",acepta,sin);
    }
    static byte[] imagen(String tipo) throws Exception {
        var b=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(12,12,java.awt.image.BufferedImage.TYPE_INT_RGB),tipo,b);return b.toByteArray();
    }
    private Fotos.Entrada foto() throws Exception { return new Fotos.Entrada(imagen("png"),"PERFIL","Postura de perfil"); }
    private long archivos() throws Exception { try(var f=Files.list(carpeta)){return f.count();} }
    private int evaluaciones() throws Exception { return Sql.uno(con,"SELECT COUNT(*) FROM evaluacion WHERE id_visita=?",r->r.getInt(1),visita); }

    @Test void guardaPosturaSeisCondicionesFotosYResponsableSinAnalisisNiPuntaje() throws Exception {
        int id=servicio.guardar(cuenta(admin),visita,datos(true,false),List.of(foto()));
        try(var p=con.prepareStatement("SELECT e.*,c.* FROM evaluacion e JOIN condiciones_puesto c USING(id_evaluacion) WHERE id_evaluacion=?")) {
            p.setInt(1,id);try(var r=p.executeQuery()) {assertTrue(r.next());assertEquals(puesto,r.getInt("id_puesto"));assertEquals("CON_FOTOS",r.getString("registro_fotografico"));assertTrue(r.getBoolean("persona_acepto"));
                assertEquals("Momento: La postura más exigente. Postura: Ensamblar <script>inerte</script>.",r.getString("motivo"));assertEquals(120,r.getInt("minutos_jornada"));assertTrue(r.getBoolean("escritorio_muy_alto"));assertFalse(r.getBoolean("pantalla_muy_alta"));
                assertTrue(r.getBoolean("pies_colgando"));assertTrue(r.getBoolean("tiene_apoyapies"));assertFalse(r.getBoolean("silla_con_apoyabrazos"));assertTrue(r.getBoolean("silla_regulable"));assertTrue(r.getString("otras_observaciones").contains("\n"));}
        }
        String nombre=Sql.uno(con,"SELECT ruta FROM fotografia WHERE id_evaluacion=?",r->r.getString(1),id);
        assertTrue(nombre.matches("[a-f0-9-]{36}\\.png"));assertArrayEquals(imagen("png"),Files.readAllBytes(carpeta.resolve(nombre)));
        assertEquals(1,archivos());
        assertEquals(admin,Sql.<Integer>uno(con,"SELECT id_cuenta FROM bitacora WHERE tabla='evaluacion' AND id_registro=?",r->r.getInt(1),id));
        assertTrue(Sql.<Boolean>uno(con,"SELECT ultimo_puntaje IS NULL FROM puesto_trabajo WHERE id_puesto=?",r->r.getBoolean(1),puesto));
    }
    @Test void sinFotosGuardaConsentimientoFisicoYMotivoAutomatico() throws Exception {
        int id=servicio.guardar(cuenta(admin),visita,datos(true,true),List.of());
        assertEquals("SIN_REGISTRO_FOTOGRAFICO",Sql.uno(con,"SELECT registro_fotografico FROM evaluacion WHERE id_evaluacion=?",r->r.getString(1),id));
        assertTrue(Sql.<Boolean>uno(con,"SELECT persona_acepto FROM evaluacion WHERE id_evaluacion=?",r->r.getBoolean(1),id));
        assertEquals("Momento: La postura más exigente. Postura: Ensamblar <script>inerte</script>.",Sql.uno(con,"SELECT motivo FROM evaluacion WHERE id_evaluacion=?",r->r.getString(1),id));assertEquals(0,archivos());
    }
    @ParameterizedTest @ValueSource(strings={"once","pdf","pdfRenombrado","seisMB","vacia","sinAceptar","sinRegistroConFoto","sinConsentimientoSinRegistro","sinNinguna","gif","pngFalso","vista","descripcion"})
    void rechazaFotosInvalidasSinEscribirEvaluacionNiArchivos(String caso) throws Exception {
        var f=foto();var d=datos(true,false);List<Fotos.Entrada> fotos=List.of(f);
        switch(caso) {
            case "once" -> fotos=Collections.nCopies(11,f);
            case "pdf","pdfRenombrado" -> fotos=List.of(new Fotos.Entrada("%PDF-1.7 archivo disfrazado".getBytes(),"PERFIL","Foto"));
            case "seisMB" -> fotos=List.of(new Fotos.Entrada(new byte[6*1024*1024],"PERFIL","Foto"));
            case "vacia" -> fotos=List.of(new Fotos.Entrada(new byte[0],"PERFIL","Foto"));
            case "sinAceptar" -> d=datos(false,false);
            case "sinRegistroConFoto" -> d=datos(true,true);
            case "sinConsentimientoSinRegistro" -> {d=datos(false,true);fotos=List.of();}
            case "sinNinguna" -> fotos=List.of();
            case "gif" -> fotos=List.of(new Fotos.Entrada(imagen("gif"),"PERFIL","Foto"));
            case "pngFalso" -> fotos=List.of(new Fotos.Entrada(new byte[]{(byte)137,80,78,71,13,10,26,10,1,2,3},"PERFIL","Foto"));
            case "vista" -> fotos=List.of(new Fotos.Entrada(f.bytes(),"INVENTADA","Foto"));
            case "descripcion" -> fotos=List.of(new Fotos.Entrada(f.bytes(),"PERFIL","x".repeat(201)));
        }
        var definitivo=d;var lista=fotos;
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(cuenta(admin),visita,definitivo,lista));
        assertEquals(0,evaluaciones());assertEquals(0,archivos());
    }
    @Test void admiteDiezFotosConVistasYDescripcionesIndependientesYJpgReal() throws Exception {
        var fotos=new ArrayList<Fotos.Entrada>();for(int i=0;i<10;i++)fotos.add(new Fotos.Entrada(imagen(i%2==0?"jpg":"png"),i%2==0?"FRENTE":"OTRA","Foto "+i));
        int id=servicio.guardar(cuenta(admin),visita,datos(true,false),fotos);
        assertEquals(10,archivos());assertEquals(10,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM fotografia WHERE id_evaluacion=?",r->r.getInt(1),id));
    }
    @Test void observacionesDe255SeConservanY256SeRechazan() throws Exception {
        var d=datos(true,true);
        var limite=new Evaluaciones.Datos(d.puesto(),d.postura(),d.criterio(),d.motivo(),d.minutos(),false,false,false,false,false,false,"á".repeat(255),true,true);
        int id=servicio.guardar(cuenta(admin),visita,limite,List.of());assertEquals("á".repeat(255),Sql.uno(con,"SELECT otras_observaciones FROM condiciones_puesto WHERE id_evaluacion=?",r->r.getString(1),id));
        var excesiva=new Evaluaciones.Datos(d.puesto(),d.postura(),d.criterio(),d.motivo(),d.minutos(),false,false,false,false,false,false,"á".repeat(256),true,true);
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(cuenta(admin),visita,excesiva,List.of()));
    }
    @Test void noEvaluaPuestoInactivoAjenoNiVisitaFinalizadaYUsuarioNoAccede() throws Exception {
        Sql.actualizar(con,"UPDATE puesto_trabajo SET activo=FALSE WHERE id_puesto=?",puesto);
        assertTrue(servicio.contexto(cuenta(admin),visita).puestos().isEmpty());
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(cuenta(admin),visita,datos(true,true),List.of()));
        Sql.actualizar(con,"UPDATE puesto_trabajo SET activo=TRUE WHERE id_puesto=?",puesto);
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);
        assertThrows(SecurityException.class,()->servicio.contexto(cuenta(usuario),visita));
        assertThrows(SecurityException.class,()->servicio.guardar(cuenta(usuario),visita,datos(true,true),List.of()));
        int id=servicio.guardar(cuenta(admin),visita,datos(true,false),List.of(foto()));
        int foto=Sql.uno(con,"SELECT id_foto FROM fotografia WHERE id_evaluacion=?",r->r.getInt(1),id);
        assertThrows(SecurityException.class,()->servicio.foto(cuenta(usuario),foto));
        assertArrayEquals(imagen("png"),servicio.foto(cuenta(admin),foto).bytes());
        new Visitas(tx,reloj).finalizar(cuenta(admin),visita,"");
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(cuenta(admin),visita,datos(true,true),List.of()));
    }
    @Test void falloDeAuditoriaRevierteDatosYRetiraSoloArchivosNuevos() throws Exception {
        var fallo=new BitacoraDao(){@Override public void registrar(Connection c,int actor,LocalDateTime fecha,String tabla,int id,String accion,String a,String n,String detalle)throws SQLException{throw new SQLException("Fallo simulado");}};
        assertThrows(SQLException.class,()->new Evaluaciones(tx,reloj,new Fotos(carpeta),fallo).guardar(cuenta(admin),visita,datos(true,false),List.of(foto())));
        assertEquals(0,evaluaciones());assertEquals(0,archivos());
    }
    @Test void noAbreRutasArbitrariasNiEnlacesSimbolicos() throws Exception {
        assertThrows(IOException.class,()->new Fotos(carpeta).leer("../../config/db.properties"));
        Path externo=Files.createTempFile("aplomo-test-externo", ".png");
        try {String nombre=UUID.randomUUID()+".png";Files.createSymbolicLink(carpeta.resolve(nombre),externo);assertThrows(IOException.class,()->new Fotos(carpeta).leer(nombre));}
        finally {Files.deleteIfExists(externo);}
    }
    @Test void aceptaExactamenteCincoMBYDescripcionDe200Caracteres() throws Exception {
        byte[] original=imagen("png"),limite=Arrays.copyOf(original,Fotos.MAX_BYTES);
        int id=servicio.guardar(cuenta(admin),visita,datos(true,false),List.of(new Fotos.Entrada(limite,"PERFIL","á".repeat(200))));
        assertEquals(5120,Sql.<Integer>uno(con,"SELECT tamano_kb FROM fotografia WHERE id_evaluacion=?",r->r.getInt(1),id));
        assertEquals("á".repeat(200),servicio.consultar(cuenta(admin),id).fotos().get(0).descripcion());
    }
    @ParameterizedTest @ValueSource(strings={"puestoAjeno","sinPostura","criterio","minutos0","minutos721"})
    void validaContextoYTodosLosCamposObligatorios(String caso) throws Exception {
        var base=datos(true,true);int elegido=puesto;
        if(caso.equals("puestoAjeno")) {
            int otroSalon=Sql.insertar(con,"INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) VALUES (?,?,?,1,10)",salon.sede(),"Otro salón","Prueba");
            elegido=Sql.insertar(con,"INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) VALUES (?,1,?,'ESCRITORIO',?)",otroSalon,"Ajeno","Centro");
        }
        var d=new Evaluaciones.Datos(elegido,caso.equals("sinPostura")?"":base.postura(),caso.equals("criterio")?"OTRO":base.criterio(),caso.equals("sinMotivo")?"":base.motivo(),caso.equals("minutos0")?0:caso.equals("minutos721")?721:120,false,false,false,false,false,false,"",true,true);
        assertThrows(IllegalArgumentException.class,()->servicio.guardar(cuenta(admin),visita,d,List.of()));assertEquals(0,evaluaciones());assertEquals(0,archivos());
    }
}
