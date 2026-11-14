package co.aplomo.web;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class GaleriaDemoTest {
 String previa;
 @BeforeEach void activar(){previa=System.getProperty(GaleriaDemo.BANDERA);System.setProperty(GaleriaDemo.BANDERA,"true");}
 @AfterEach void restaurar(){if(previa==null)System.clearProperty(GaleriaDemo.BANDERA);else System.setProperty(GaleriaDemo.BANDERA,previa);}
 @ParameterizedTest @CsvSource({"PERFIL,perfil","FRENTE,frente","OTRA,detalle"})
 void lasCincoPosturasUsanLaVistaCorrectaYSonLosMismosJpgSinAlterar(String vista,String tipo)throws Exception{
  String h=GaleriaDemo.selector(vista);assertTrue(h.contains("Usar foto de ejemplo"));assertEquals(5,h.split("data-usar-ejemplo=",-1).length-1);assertEquals(5,h.split("data-vista-ejemplo=\""+vista+"\"",-1).length-1);
  for(var postura:GaleriaDemo.POSTURAS){String archivo=postura.archivo()+"_"+tipo+".jpg",ruta="/static/img/galeria/"+archivo;assertTrue(h.contains("data-usar-ejemplo=\""+ruta+"\""));assertTrue(h.contains("src=\""+ruta+"\""));try(var in=getClass().getResourceAsStream(ruta)){assertNotNull(in,ruta);byte[] bytes=in.readAllBytes();assertTrue(bytes.length>0&&bytes.length<=5*1024*1024);assertArrayEquals(Files.readAllBytes(Path.of("fotos_de_prueba/galeria",archivo)),bytes);assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(bytes)));}}
 }
 @Test void unaBanderaQuitaTodosLosEnlaces(){System.setProperty(GaleriaDemo.BANDERA,"false");for(String vista:new String[]{"PERFIL","FRENTE","OTRA"})assertEquals("",GaleriaDemo.selector(vista));}
 @Test void noInventaUnaVista(){assertThrows(IllegalArgumentException.class,()->GaleriaDemo.selector("PDF"));}
}
