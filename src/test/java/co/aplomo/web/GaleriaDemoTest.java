package co.aplomo.web;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class GaleriaDemoTest {
 String previa;
 @BeforeEach void activar(){previa=System.getProperty(GaleriaDemo.BANDERA);System.setProperty(GaleriaDemo.BANDERA,"true");}
 @AfterEach void restaurar(){if(previa==null)System.clearProperty(GaleriaDemo.BANDERA);else System.setProperty(GaleriaDemo.BANDERA,previa);}
 @Test void cadaArchivoExisteYEsUnaImagenValidaDeMenosDeCincoMb()throws Exception{
  var archivos=new HashSet<String>();long total=0;
  for(var f:GaleriaDemo.FOTOS)if(archivos.add(f.archivo())){try(var in=getClass().getResourceAsStream("/static/img/galeria/"+f.archivo())){assertNotNull(in,f.archivo());byte[] b=in.readAllBytes();assertTrue(b.length>0&&b.length<=5*1024*1024);assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(b)));total+=b.length;}}
  assertEquals(42,archivos.size());assertTrue(total<25*1024*1024);
 }
 @Test void losQuinceOriginalesConservanExactamenteSusBytes()throws Exception{
  for(var f:GaleriaDemo.FOTOS)if(f.archivo().matches("S[2-6]_.*_(perfil|frente|detalle)\\.jpg"))assertArrayEquals(Files.readAllBytes(Path.of("fotos_de_prueba/galeria",f.archivo())),Files.readAllBytes(Path.of("src/main/resources/static/img/galeria",f.archivo())));
 }
 @ParameterizedTest @CsvSource({"OFICINA,DERECHA","OFICINA,IZQUIERDA","PLANTA,DERECHA","PLANTA,IZQUIERDA"})
 void cadaContextoYLadoTienenAlMenosTresPerfilesBuenos(String contexto,String mira){assertTrue(GaleriaDemo.FOTOS.stream().filter(f->f.contexto().equals(contexto)&&f.vista().equals("PERFIL")&&f.mira().equals(mira)&&f.tipo().equals("BUENA")).count()>=3);}
 @ParameterizedTest @CsvSource({"OFICINA","PLANTA"})
 void cadaContextoTieneFrenteDetalleYErroresParaRotar(String contexto){for(String vista:List.of("FRENTE","DETALLE"))assertTrue(GaleriaDemo.FOTOS.stream().anyMatch(f->f.contexto().equals(contexto)&&f.vista().equals(vista)&&f.tipo().equals("BUENA")));assertEquals(contexto.equals("PLANTA")?4:3,GaleriaDemo.FOTOS.stream().filter(f->f.contexto().equals(contexto)&&f.tipo().equals("ERROR")&&f.rotacion()).count());}
 @Test void piernasTapadasNoSeOfreceComoPerfilBueno(){var f=GaleriaDemo.FOTOS.stream().filter(x->x.archivo().equals("oficina_sentado_inclinado_perfil_izquierda.jpg")).findFirst().orElseThrow();assertEquals("ERROR",f.tipo());assertEquals("IZQUIERDA",f.mira());assertTrue(f.titulo().contains("usa Calcular manualmente"));assertTrue(f.aviso().contains("solo una parte"));}
 @Test void espejoS5SeOfreceSoloComoErrorRotativoYQuedanOchoPerfilesIzquierdosBuenos(){
  String archivo="S5_inclinada_hacia_el_carro_perfil_izq.jpg";
  var f=GaleriaDemo.FOTOS.stream().filter(x->x.archivo().equals(archivo)).findFirst().orElseThrow();
  assertEquals("ERROR",f.tipo());assertEquals("PLANTA",f.contexto());assertEquals("PERFIL",f.vista());assertEquals("IZQUIERDA",f.mira());assertTrue(f.rotacion());
  assertEquals("La IA no la reconoce bien: usa Calcular manualmente",f.titulo());assertTrue(f.aviso().startsWith("Parece una foto de frente o de espaldas"));
  assertFalse(GaleriaDemo.FOTOS.stream().anyMatch(x->x.archivo().equals(archivo)&&x.tipo().equals("BUENA")));
  assertEquals(8,GaleriaDemo.FOTOS.stream().filter(x->x.contexto().equals("PLANTA")&&x.vista().equals("PERFIL")&&x.mira().equals("IZQUIERDA")&&x.tipo().equals("BUENA")).count());
  String h=GaleriaDemo.selector("PERFIL");assertTrue(h.contains(archivo));assertTrue(h.contains("Ejemplo con error: sirve para ver el aviso"));
 }
 @ParameterizedTest @CsvSource({"PERFIL,PERFIL","FRENTE,FRENTE","OTRA,DETALLE"})
 void selectorIncluyeSoloSuRanuraYMetadatosParaFiltrar(String vista,String ranura){String h=GaleriaDemo.selector(vista);assertTrue(h.contains("data-galeria-vista=\""+ranura+"\""));for(var f:GaleriaDemo.FOTOS)if(f.vista().equals(ranura))assertTrue(h.contains("data-usar-ejemplo=\"/static/img/galeria/"+f.archivo()+"\""));assertEquals(GaleriaDemo.FOTOS.stream().filter(f->f.vista().equals(ranura)).count(),h.split("data-usar-ejemplo=",-1).length-1);}
 @Test void unaBanderaQuitaTodosLosEnlaces(){System.setProperty(GaleriaDemo.BANDERA,"false");for(String v:List.of("PERFIL","FRENTE","OTRA"))assertEquals("",GaleriaDemo.selector(v));}
 @Test void noInventaUnaVista(){assertThrows(IllegalArgumentException.class,()->GaleriaDemo.selector("PDF"));}
}
