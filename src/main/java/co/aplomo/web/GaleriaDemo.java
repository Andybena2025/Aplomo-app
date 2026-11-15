package co.aplomo.web;
import java.util.*;
import static co.aplomo.web.Html.escapar;
/** Paso 18: imágenes públicas de ejemplo; una foto puede ser válida de frente y errónea en perfil. */
final class GaleriaDemo {
 static final String BANDERA="aplomo.modoDemo";
 record Foto(String archivo,String titulo,String vista,String mira,String contexto,String tipo,String aviso,boolean rotacion){}
 static final List<Foto> FOTOS=List.of(
  new Foto("S2_mesa_alta_cabeza_inclinada_perfil.jpg","Mesa alta y cabeza inclinada","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("S2_mesa_alta_cabeza_inclinada_perfil_izq.jpg","Mesa alta y cabeza inclinada","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("S2_mesa_alta_cabeza_inclinada_frente.jpg","Mesa alta y cabeza inclinada","FRENTE","","PLANTA","BUENA","",false),
  new Foto("S2_mesa_alta_cabeza_inclinada_detalle.jpg","Mesa alta y cabeza inclinada","DETALLE","","PLANTA","BUENA","",false),
  new Foto("S3_alcance_lejano_una_pierna_perfil.jpg","Alcance lejano con un pie de apoyo","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("S3_alcance_lejano_una_pierna_perfil_izq.jpg","Alcance lejano con un pie de apoyo","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("S3_alcance_lejano_una_pierna_frente.jpg","Alcance lejano con un pie de apoyo","FRENTE","","PLANTA","BUENA","",false),
  new Foto("S3_alcance_lejano_una_pierna_detalle.jpg","Alcance lejano con un pie de apoyo","DETALLE","","PLANTA","BUENA","",false),
  new Foto("S4_brazos_sobre_la_cabeza_perfil.jpg","Brazos por encima de los hombros","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("S4_brazos_sobre_la_cabeza_perfil_izq.jpg","Brazos por encima de los hombros","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("S4_brazos_sobre_la_cabeza_frente.jpg","Brazos por encima de los hombros","FRENTE","","PLANTA","BUENA","",false),
  new Foto("S4_brazos_sobre_la_cabeza_detalle.jpg","Brazos por encima de los hombros","DETALLE","","PLANTA","BUENA","",false),
  new Foto("S5_inclinada_hacia_el_carro_perfil.jpg","Inclinada hacia el carro","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("S5_inclinada_hacia_el_carro_perfil_izq.jpg","La IA no la reconoce bien: usa Calcular manualmente","PERFIL","IZQUIERDA","PLANTA","ERROR","Parece una foto de frente o de espaldas. Necesito una de lado (de perfil).",true),
  new Foto("S5_inclinada_hacia_el_carro_frente.jpg","Inclinada hacia el carro","FRENTE","","PLANTA","BUENA","",false),
  new Foto("S5_inclinada_hacia_el_carro_detalle.jpg","Inclinada hacia el carro","DETALLE","","PLANTA","BUENA","",false),
  new Foto("S6_levantando_pieza_pesada_perfil.jpg","Levantando una pieza pesada","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("S6_levantando_pieza_pesada_perfil_izq.jpg","Levantando una pieza pesada","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("S6_levantando_pieza_pesada_frente.jpg","Levantando una pieza pesada","FRENTE","","PLANTA","BUENA","",false),
  new Foto("S6_levantando_pieza_pesada_detalle.jpg","Levantando una pieza pesada","DETALLE","","PLANTA","BUENA","",false),
  new Foto("oficina_sentado_perfil_derecha.jpg","Sentado en el escritorio","PERFIL","DERECHA","OFICINA","BUENA","",false),
  new Foto("oficina_sentado_encorvado_perfil_derecha.jpg","Sentado con la espalda encorvada","PERFIL","DERECHA","OFICINA","BUENA","",false),
  new Foto("oficina_sentado_abierta_perfil_derecha.jpg","Sentada en oficina abierta","PERFIL","DERECHA","OFICINA","BUENA","",false),
  new Foto("oficina_sentada_mouse_perfil_derecha.jpg","Alcanzando el mouse","PERFIL","DERECHA","OFICINA","BUENA","",false),
  new Foto("oficina_sentado_perfil_izquierda.jpg","Sentado en el escritorio","PERFIL","IZQUIERDA","OFICINA","BUENA","",false),
  new Foto("oficina_de_pie_perfil_izquierda.jpg","De pie en el escritorio","PERFIL","IZQUIERDA","OFICINA","BUENA","",false),
  new Foto("oficina_escritorio_de_pie_perfil_izquierda.jpg","Escritorio elevable","PERFIL","IZQUIERDA","OFICINA","BUENA","",false),
  new Foto("planta_banco_inclinado_perfil_derecha.jpg","Inclinado sobre el banco","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("planta_estante_alto_perfil_derecha.jpg","Alcanzando un estante alto","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("planta_destornillador_perfil_derecha.jpg","Con destornillador en el banco","PERFIL","DERECHA","PLANTA","BUENA","",false),
  new Foto("planta_inclinado_perfil_izquierda.jpg","Inclinado hacia el puesto","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("planta_levantando_perfil_izquierda.jpg","Levantando una carga","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("planta_pieza_inclinada_perfil_izquierda.jpg","Examinando una pieza","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("planta_caja_perfil_izquierda.jpg","Sosteniendo una caja","PERFIL","IZQUIERDA","PLANTA","BUENA","",false),
  new Foto("oficina_frente.jpg","Vista de frente","FRENTE","","OFICINA","BUENA","",false),
  new Foto("oficina_frente.jpg","Vista de frente","PERFIL","","OFICINA","ERROR","Parece una foto de frente o de espaldas. Necesito una de lado (de perfil).",true),
  new Foto("planta_banco_frente.jpg","Vista de frente","FRENTE","","PLANTA","BUENA","",false),
  new Foto("planta_banco_frente.jpg","Vista de frente","PERFIL","","PLANTA","ERROR","Parece una foto de frente o de espaldas. Necesito una de lado (de perfil).",true),
  new Foto("oficina_detalle_munecas.jpg","Manos y muñecas en el teclado","DETALLE","","OFICINA","BUENA","",false),
  new Foto("oficina_detalle_munecas.jpg","Detalle sin cuerpo completo","PERFIL","","OFICINA","ERROR","No se ve una persona",false),
  new Foto("oficina_detalle_teclado.jpg","Manos y muñecas en el teclado","DETALLE","","OFICINA","BUENA","",false),
  new Foto("oficina_detalle_teclado.jpg","Detalle sin cuerpo completo","PERFIL","","OFICINA","ERROR","No se ve una persona",false),
  new Foto("error_sin_persona.jpg","Sin una persona","PERFIL","","PLANTA","ERROR","No se ve una persona",true),
  new Foto("error_cuerpo_cortado.jpg","Cuerpo fuera del encuadre","PERFIL","","PLANTA","ERROR","Se ve solo una parte del cuerpo",true),
  new Foto("error_de_espaldas.jpg","Persona de espaldas","PERFIL","","OFICINA","ERROR","Parece una foto de frente o de espaldas. Necesito una de lado (de perfil).",true),
  new Foto("oficina_sentado_inclinado_perfil_izquierda.jpg","La IA no la reconoce bien: usa Calcular manualmente","PERFIL","IZQUIERDA","OFICINA","ERROR","Se ve solo una parte del cuerpo",true));
 private GaleriaDemo(){}
 static boolean habilitada(){return Boolean.parseBoolean(System.getProperty(BANDERA,"true"));}
 static String selector(String vista){
  if(!habilitada())return "";
  String ranura=switch(vista){case "PERFIL","FRENTE"->vista;case "OTRA","DETALLE"->"DETALLE";default->throw new IllegalArgumentException("Vista de ejemplo no disponible.");};
  StringBuilder h=new StringBuilder("<details class=\"galeria-demo\" data-galeria-vista=\"").append(ranura).append("\"><summary class=\"link\">Usar foto de ejemplo</summary><p class=\"hint\">Elige una foto para probar.</p><div class=\"galeria-demo-lista\">");
  for(var f:FOTOS)if(f.vista().equals(ranura)){
   String url="/static/img/galeria/"+f.archivo();
   h.append("<button type=\"button\" hidden class=\"galeria-demo-opcion\" data-usar-ejemplo=\"").append(escapar(url)).append("\" data-vista-ejemplo=\"").append(vista.equals("DETALLE")?"OTRA":vista).append("\" data-contexto=\"").append(f.contexto()).append("\" data-mira=\"").append(f.mira()).append("\" data-tipo=\"").append(f.tipo()).append("\" data-rotacion=\"").append(f.rotacion()).append("\" data-aviso=\"").append(escapar(f.aviso())).append("\"><img src=\"").append(url).append("\" alt=\"Foto de ejemplo\" loading=\"lazy\" width=\"80\" height=\"80\"><span>").append(escapar(f.titulo())).append("<small class=\"hint\" data-mira-label hidden></small>");
   if(f.tipo().equals("ERROR"))h.append("<small class=\"hint\">Ejemplo con error: sirve para ver el aviso</small>");
   h.append("</span></button>");
  }
  return h.append("</div></details>").toString();
 }
}
