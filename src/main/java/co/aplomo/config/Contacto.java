package co.aplomo.config;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;
/** Paso 25: datos de contacto opcionales, locales y privados. Nunca escribe ni registra su contenido. */
public record Contacto(String nombre,String cargo,String correo,String telefono,String whatsapp,boolean incompleto){
 /** Lee UTF-8; omite campos ausentes e impide enlaces con esquemas o consultas inesperados. */
 public static Contacto leer(Path archivo){var p=new Properties();try(var in=Files.newBufferedReader(archivo,StandardCharsets.UTF_8)){p.load(in);}catch(IOException|IllegalArgumentException e){return new Contacto("","","","","",true);}
  String nombre=texto(p,"nombre"),cargo=texto(p,"cargo"),correo=texto(p,"empresa.correo").isBlank()?texto(p,"correo"):texto(p,"empresa.correo"),telefono=texto(p,"empresa.telefono").isBlank()?texto(p,"telefono"):texto(p,"empresa.telefono"),wa=texto(p,"whatsapp");
  if(!correo.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"))correo="";
  wa=wa.replaceAll("[+\\s()\\-]","");if(!wa.matches("[1-9][0-9]{6,14}"))wa="";
  return new Contacto(nombre,cargo,correo,telefono,wa,List.of(nombre,cargo,correo,telefono,wa).stream().anyMatch(String::isBlank));
 }
 private static String texto(Properties p,String campo){String v=p.getProperty("contacto."+campo,"").strip();return v.length()>500?"":v;}
}
