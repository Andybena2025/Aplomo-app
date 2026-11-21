package co.aplomo.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Configuración local o por entorno; nunca imprime claves. */
public final class Config {
    private Config(){}
    public record Datos(String url,String usuario,String clave,int puerto,Path carpetaFotos){@Override public String toString(){return "Configuración de ALINEO (clave oculta)";}}
    private static final class Actual {static final Datos VALORES=leer(System.getenv(),Path.of("config/db.properties"));}
    /** Env por campo; abre el archivo solo cuando falta alguna variable de conexión. */
    static Datos leer(Map<String,String> entorno,Path archivo){
        Properties p=new Properties();
        if(!entorno.keySet().containsAll(Set.of("APLOMO_DB_URL","APLOMO_DB_USUARIO","APLOMO_DB_CLAVE"))){
            try(Reader r=Files.newBufferedReader(archivo,StandardCharsets.UTF_8)){p.load(r);}catch(IOException e){throw new IllegalStateException("No se pudo leer "+archivo+". Configura las variables APLOMO_DB o copia db.properties.ejemplo como db.properties.",e);}
        }
        String url=valor(entorno,p,"APLOMO_DB_URL","db.url"),usuario=valor(entorno,p,"APLOMO_DB_USUARIO","db.usuario"),clave=valor(entorno,p,"APLOMO_DB_CLAVE","db.clave");
        if(!url.startsWith("jdbc:mysql://"))throw new IllegalStateException("Configura una URL JDBC de MySQL.");
        int puerto=puerto(entorno.getOrDefault("PORT",entorno.getOrDefault("APLOMO_PUERTO","8080")));String carpeta=entorno.getOrDefault("APLOMO_CARPETA_FOTOS","fotos");if(carpeta.isBlank())throw new IllegalStateException("Configura APLOMO_CARPETA_FOTOS.");
        return new Datos(url,usuario,clave,puerto,Path.of(carpeta));
    }
    private static String valor(Map<String,String> e,Properties p,String variable,String propiedad){String v=e.containsKey(variable)?e.get(variable):p.getProperty(propiedad);if(v==null||v.isBlank())throw new IllegalStateException("Falta configurar "+variable+" o "+propiedad+".");return v;}
    private static int puerto(String valor){try{int p=Integer.parseInt(valor);if(p>=1&&p<=65535)return p;}catch(NumberFormatException ignored){}throw new IllegalStateException("El puerto de ALINEO debe estar entre 1 y 65535.");}
    /** Se conserva la sustitución de las pruebas para no usar la base real. */
    public static String url(){return System.getProperty("aplomo.db.url",Actual.VALORES.url());}
    public static String usuario(){return Actual.VALORES.usuario();}
    public static String clave(){return Actual.VALORES.clave();}
    public static int puerto(){return System.getProperty("aplomo.puerto")==null?Actual.VALORES.puerto():puerto(System.getProperty("aplomo.puerto"));}
    public static Path carpetaFotos(){return System.getProperty("aplomo.fotos")==null?Actual.VALORES.carpetaFotos():Path.of(System.getProperty("aplomo.fotos"));}
}
