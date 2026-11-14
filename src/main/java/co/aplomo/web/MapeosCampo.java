package co.aplomo.web;
import co.aplomo.rula.MotorRula.Carga;
import java.util.Map;
/** Paso 15: correspondencias de respuestas, sin fórmulas ni puntos RULA. */
public final class MapeosCampo {
 private MapeosCampo(){}
 public static final Map<String,Integer> MINUTOS=Map.of("MENOS_2",60,"DE_2_A_4",180,"DE_4_A_6",300,"DE_6_A_8",420,"MAS_8",540);
 public static final Map<String,String> CRITERIOS=Map.of("EXIGENTE","MAS_INADECUADA","DURADERA","MAS_INADECUADA","NO_SABE","MAYOR_EXPOSICION");
 public static final Map<String,Double> MUNECA=Map.of("RECTA",5.0,"POCO",8.0,"MUCHO",25.0);
 /** RF-019: traduce peso, repetición y golpes a los valores existentes de docs/03. */
 public static Carga carga(String peso,String actividad,boolean golpe){
  if(!java.util.Set.of("NADA","CELULAR","GALON","BULTO").contains(peso)||!java.util.Set.of("NINGUNA","ESTATICA","REPETITIVA").contains(actividad))throw new IllegalArgumentException("Responde el peso y la frecuencia.");
  if(golpe)return Carga.GOLPES_O_FUERZAS_BRUSCAS;
  boolean sostenida=!actividad.equals("NINGUNA");
  return switch(peso){case "NADA","CELULAR"->Carga.MENOS_2KG_INTERMITENTE;case "GALON"->sostenida?Carga.DE_2_A_10KG_ESTATICA_O_REPETITIVA:Carga.DE_2_A_10KG_INTERMITENTE;default->sostenida?Carga.MAS_10KG_ESTATICA_O_REPETITIVA:Carga.MAS_10KG_INTERMITENTE;};
 }
 /** RF-017: distingue giro e inclinación, sin sumar ambos por una respuesta ambigua. */
 public static boolean[] orientacion(String respuesta,String detalle){
  if("no".equals(respuesta))return new boolean[]{false,false};
  if(!"si".equals(respuesta)||!java.util.Set.of("GIRADA","INCLINADA","AMBAS").contains(detalle))throw new IllegalArgumentException("Elige cómo está la cabeza o la espalda.");
  return new boolean[]{!detalle.equals("INCLINADA"),!detalle.equals("GIRADA")};
 }
}
