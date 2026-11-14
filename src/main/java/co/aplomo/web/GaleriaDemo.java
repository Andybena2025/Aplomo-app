package co.aplomo.web;

import java.util.List;

/** Paso 15-B: catálogo de fotos de demostración; nunca contiene fotos privadas. */
final class GaleriaDemo {
 static final String BANDERA="aplomo.modoDemo";
 record Postura(String archivo,String titulo){}
 static final List<Postura> POSTURAS=List.of(
  new Postura("S2_mesa_alta_cabeza_inclinada","Mesa alta y cabeza inclinada"),
  new Postura("S3_alcance_lejano_una_pierna","Alcance lejano con un pie de apoyo"),
  new Postura("S4_brazos_sobre_la_cabeza","Brazos por encima de los hombros"),
  new Postura("S5_inclinada_hacia_el_carro","Inclinada hacia el carro"),
  new Postura("S6_levantando_pieza_pesada","Levantando una pieza pesada"));
 private GaleriaDemo(){}
 /** Una sola bandera JVM; activa por ahora, se retira con -Daplomo.modoDemo=false. */
 static boolean habilitada(){return Boolean.parseBoolean(System.getProperty(BANDERA,"true"));}
 static String selector(String vista){
  if(!habilitada())return "";
  String tipo=switch(vista){case "PERFIL"->"perfil";case "FRENTE"->"frente";case "OTRA"->"detalle";default->throw new IllegalArgumentException("Vista de ejemplo no disponible.");};
  StringBuilder h=new StringBuilder("<details class=\"galeria-demo\"><summary class=\"link\">Usar foto de ejemplo</summary><p class=\"hint\">Elige una postura para probar.</p><div class=\"galeria-demo-lista\">");
  for(var p:POSTURAS){String url="/static/img/galeria/"+p.archivo()+"_"+tipo+".jpg";h.append("<button type=\"button\" class=\"galeria-demo-opcion\" data-usar-ejemplo=\"").append(url).append("\" data-vista-ejemplo=\"").append(vista).append("\"><img src=\"").append(url).append("\" alt=\"Ejemplo de ").append(tipo).append("\" loading=\"lazy\" width=\"80\" height=\"80\"><span>").append(p.titulo()).append("</span></button>");}
  return h.append("</div></details>").toString();
 }
}
