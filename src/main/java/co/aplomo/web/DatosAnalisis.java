package co.aplomo.web;

import co.aplomo.rula.*;
import co.aplomo.rula.MotorRula.*;
import co.aplomo.rula.MedidorAngulos.*;
import co.aplomo.rula.PuntuadorSegmentos.*;
import co.aplomo.servicio.Analisis;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** RF-017, RNF-004: adapta campos acotados; no mide ángulos ni calcula puntos RULA. */
public final class DatosAnalisis {
    private DatosAnalisis() {}
    /** Convierte un formulario de una foto en entradas del único motor RULA. */
    public static Entrada leer(Map<String,String> f) {
        var mediciones=new EnumMap<Segmento,Medicion>(Segmento.class);
        for(var s:Segmento.values()) {
            List<Punto> ps=new ArrayList<>();String texto=f.getOrDefault(s+"_puntos","");
            if(!texto.isBlank())for(String par:texto.split(";",-1)) {
                String[] xy=par.split(",",-1);if(xy.length!=2)throw new IllegalArgumentException("Revisa los puntos de la foto.");
                ps.add(new Punto(numero(xy[0]),numero(xy[1])));
            }
            String modo=opcion(f.get(s+"_modo"),"el modo de medición","PUNTOS","MANUAL");
            mediciones.put(s,new Medicion(ps,modo.equals("MANUAL")?numero(f.get(s+"_grados")):null));
        }
        return new Entrada(Lado.valueOf(opcion(f.get("lado"),"el lado","DERECHO","IZQUIERDO")),entero(f.get("ancho")),entero(f.get("alto")),mediciones,
                new Ajustes(si(f,"hombroElevado"),si(f,"brazoSeparado"),si(f,"brazoApoyado"),si(f,"antebrazoCruza"),si(f,"munecaDesviada"),Giro.valueOf(opcion(f.get("giro"),"el giro","MEDIO","EXTREMO")),si(f,"cuelloGirado"),si(f,"cuelloInclinado"),si(f,"troncoGirado"),si(f,"troncoInclinado"),si(f,"piernasApoyadas"),false),si(f,"actividad"),MotorRula.Carga.valueOf(opcion(f.get("carga"),"la carga",Arrays.stream(MotorRula.Carga.values()).map(Enum::name).toArray(String[]::new))));
    }
    /** Lee hasta diez análisis codificados, rechazando índices y campos repetidos. */
    public static List<Analisis.Solicitud> solicitudes(Map<String,String> f) {
        List<Analisis.Solicitud> lista=new ArrayList<>();
        for(int i=0;i<10;i++)if(f.containsKey("analisis_"+i)) {
            var datos=campos(f.get("analisis_"+i));int foto;
            try{foto=Integer.parseInt(datos.get("foto"));}catch(RuntimeException x){throw new IllegalArgumentException("Revisa la foto seleccionada.");}
            lista.add(new Analisis.Solicitud(foto,leer(datos)));
        }
        for(String clave:f.keySet())if(clave.startsWith("analisis_")&&!clave.matches("analisis_[0-9]"))throw new IllegalArgumentException("Guarda hasta diez análisis a la vez.");
        return List.copyOf(lista);
    }
    /** Descodifica campos de una única foto sin permitir duplicados ni entradas excesivas. */
    public static Map<String,String> campos(String texto) {
        if(texto==null||texto.length()>12000)throw new IllegalArgumentException("Los datos de la foto son demasiado largos.");
        Map<String,String> f=new HashMap<>();
        for(String par:texto.split("&")){
            String[] p=par.split("=",2);if(p.length!=2)throw new IllegalArgumentException("Revisa los datos del análisis.");
            String k=URLDecoder.decode(p[0],StandardCharsets.UTF_8),v=URLDecoder.decode(p[1],StandardCharsets.UTF_8);
            if(f.putIfAbsent(k,v)!=null)throw new IllegalArgumentException("El análisis contiene campos repetidos.");
        }
        return f;
    }
    private static boolean si(Map<String,String> f,String k){String v=f.getOrDefault(k,"no");if(!v.equals("si")&&!v.equals("no"))throw new IllegalArgumentException("Revisa las casillas del análisis.");return v.equals("si");}
    private static String opcion(String v,String campo,String... opciones){return co.aplomo.servicio.Validacion.opcion(v,campo,opciones);}
    private static int entero(String v){return co.aplomo.servicio.Validacion.entero(v,"dimensiones de la foto",1,Integer.MAX_VALUE);}
    private static double numero(String v){try{double n=Double.parseDouble(v);if(!Double.isFinite(n))throw new NumberFormatException();return n;}catch(RuntimeException x){throw new IllegalArgumentException("Escribe un ángulo o una coordenada válidos.");}}
}
