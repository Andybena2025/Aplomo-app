package co.aplomo.web;

import co.aplomo.rula.*;
import co.aplomo.rula.MedidorAngulos.*;
import co.aplomo.rula.MotorRula.*;
import java.util.*;

/** Paso 13E-B: catálogo visual y adaptación al motor existente; ninguna fórmula RULA en JavaScript. */
public final class RecorridoCuerpo {
    private RecorridoCuerpo() {}
    public record Rango(String imagen,String etiqueta,double grados,int puntos) {}
    public static final Map<Segmento,List<Rango>> RANGOS=Map.of(
        Segmento.BRAZO,List.of(new Rango("brazo_extension.png","Hacia atrás más de 20°",-30,2),new Rango("brazo_neutro.png","Entre −20° y 20°",0,1),new Rango("brazo_20_45.png","20° a 45°",30,2),new Rango("brazo_45_90.png","45° a 90°",60,3),new Rango("brazo_mas_90.png","Más de 90°",110,4)),
        Segmento.ANTEBRAZO,List.of(new Rango("antebrazo_60_100.png","Entre 60° y 100°",80,1),new Rango("antebrazo_fuera.png","Menos de 60° o más de 100°",40,2)),
        Segmento.MUNECA,List.of(new Rango("muneca_neutra.png","Recta",0,1),new Rango("muneca_15.png","Doblada hasta 15°",10,2),new Rango("muneca_mas_15.png","Doblada más de 15°",25,3)),
        Segmento.CUELLO,List.of(new Rango("cuello_0_10.png","0° a 10°",5,1),new Rango("cuello_10_20.png","10° a 20°",15,2),new Rango("cuello_mas_20.png","Más de 20°",30,3),new Rango("cuello_extension.png","Hacia atrás",-15,4)),
        Segmento.TRONCO,List.of(new Rango("tronco_erguido.png","Erguido o sentado bien apoyado",0,1),new Rango("tronco_0_20.png","0° a 20°",10,2),new Rango("tronco_20_60.png","20° a 60°",40,3),new Rango("tronco_mas_60.png","Más de 60°",75,4)));
    public static final String[][] ORDEN={{"BRAZO","Brazo"},{"ANTEBRAZO","Antebrazo"},{"MUNECA","Muñeca"},{"GIRO","Giro de muñeca"},{"CUELLO","Cuello"},{"TRONCO","Tronco"},{"PIERNAS","Piernas"}};
    private static final Map<String,String[][]> AJUSTES=Map.of(
        "BRAZO",new String[][]{{"hombroElevado","Hombro levantado","brazo_hombro.png"},{"brazoSeparado","Brazo separado del cuerpo","brazo_separado.png"},{"brazoApoyado","Apoyado o persona inclinada","brazo_apoyado.png"}},
        "ANTEBRAZO",new String[][]{{"antebrazoCruza","Cruza la línea media o sale hacia el lado","antebrazo_cruza.png"}},
        "MUNECA",new String[][]{{"munecaDesviada","Doblada hacia un lado","muneca_desviada.png"}},
        "CUELLO",new String[][]{{"cuelloGirado","Girado","cuello_girado.png"},{"cuelloInclinado","Inclinado hacia un lado","cuello_inclinado.png"}},
        "TRONCO",new String[][]{{"troncoGirado","Girado","tronco_girado.png"},{"troncoInclinado","Inclinado hacia un lado","tronco_inclinado.png"}});

    /** Solo lee el componente que se está observando. Nunca emite un puntaje final incompleto. */
    public static String parcial(Map<String,String> f) {
        String seg=ValidacionEntrada.segmento(f.get("segmento"));
        if(seg.equals("GIRO")||seg.equals("PIERNAS")) {
            // Estos dos componentes son independientes de los ángulos y demás ajustes.
            // Se proyecta únicamente el componente pedido, nunca los valores auxiliares ni un total.
            var giro=seg.equals("GIRO")?PuntuadorSegmentos.Giro.valueOf(co.aplomo.servicio.Validacion.opcion(f.get("respuesta"),"el giro","MEDIO","EXTREMO")):PuntuadorSegmentos.Giro.MEDIO;
            boolean piernas=seg.equals("PIERNAS")?si(f,"respuesta"):true;
            var puntos=PuntuadorSegmentos.puntuar(new Angulos(0,0,0,0,0),new PuntuadorSegmentos.Ajustes(false,false,false,false,false,giro,false,false,false,false,piernas,false));
            return respuesta(seg,null,null,seg.equals("GIRO")?puntos.giro():puntos.piernas(),true,false);
        }
        var segmento=Segmento.valueOf(seg);var m=DatosAnalisis.medicion(f,segmento);
        int ancho=co.aplomo.servicio.Validacion.entero(f.get("ancho"),"ancho de la foto",1,Integer.MAX_VALUE),alto=co.aplomo.servicio.Validacion.entero(f.get("alto"),"alto de la foto",1,Integer.MAX_VALUE);
        var lado=Lado.valueOf(co.aplomo.servicio.Validacion.opcion(f.get("lado"),"el lado","DERECHO","IZQUIERDO"));
        if(m.puntos().size()!=0&&m.puntos().size()!=3)throw new IllegalArgumentException("Cada segmento necesita sus tres puntos completos.");
        for(var p:m.puntos())if(p.x()<0||p.x()>1||p.y()<0||p.y()>1)throw new IllegalArgumentException("Los puntos deben estar dentro de la foto.");
        double grados;
        if(m.manual()!=null)grados=m.manual();
        else {
            if(m.puntos().size()!=3)throw new IllegalArgumentException("Marca los tres puntos o elige un mosaico por observación directa.");
            var ps=m.puntos().stream().map(p->new Punto(p.x()*ancho,p.y()*alto)).toList();
            grados=switch(segmento){case BRAZO->MedidorAngulos.brazo(ps.get(0),ps.get(1),ps.get(2),lado);case ANTEBRAZO->MedidorAngulos.antebrazo(ps.get(0),ps.get(1),ps.get(2));case MUNECA->MedidorAngulos.muneca(ps.get(0),ps.get(1),ps.get(2));case CUELLO->MedidorAngulos.cuello(ps.get(0),ps.get(1),ps.get(2),lado);case TRONCO->MedidorAngulos.tronco(ps.get(0),ps.get(1),lado);};
        }
        int base=puntuar(segmento,grados,Map.of(),false);List<Rango> rangos=RANGOS.get(segmento);
        Rango rango=rangos.stream().filter(r->r.puntos()==base&&(segmento!=Segmento.BRAZO||base!=2||(grados<0)==(r.grados()<0))).findFirst().orElseThrow();
        boolean confirmado="si".equals(f.get("ajustes_confirmados"));
        return respuesta(seg,grados,rango.grados(),confirmado?puntuar(segmento,grados,f,true):null,confirmado,m.manual()!=null);
    }
    private static String respuesta(String seg,Double grados,Double rango,Integer puntos,boolean confirmado,boolean manual){
        return "<span data-parcial=\""+seg+"\" data-grados=\""+(grados==null?"":grados)+"\" data-rango=\""+(rango==null?"":rango)+"\" data-puntos=\""+(puntos==null?"":puntos)+"\" data-confirmado=\""+confirmado+"\" data-manual=\""+manual+"\">"+(puntos==null?"Medición pendiente de confirmar":puntos+(puntos==1?" punto":" puntos"))+"</span>";
    }
    private static int puntuar(Segmento s,double g,Map<String,String> f,boolean ajustes){return switch(s){
        case BRAZO->PuntuadorSegmentos.brazo(g,ajustes&&si(f,"hombroElevado"),ajustes&&si(f,"brazoSeparado"),ajustes&&si(f,"brazoApoyado"));
        case ANTEBRAZO->PuntuadorSegmentos.antebrazo(g,ajustes&&si(f,"antebrazoCruza"));
        case MUNECA->PuntuadorSegmentos.muneca(g,ajustes&&si(f,"munecaDesviada"));
        case CUELLO->PuntuadorSegmentos.cuello(g,ajustes&&si(f,"cuelloGirado"),ajustes&&si(f,"cuelloInclinado"));
        case TRONCO->PuntuadorSegmentos.tronco(g,ajustes&&si(f,"troncoGirado"),ajustes&&si(f,"troncoInclinado"),false);};}
    private static boolean si(Map<String,String> f,String clave){return co.aplomo.servicio.Validacion.opcion(f.get(clave),"la respuesta observada","si","no").equals("si");}
    private static final class ValidacionEntrada {static String segmento(String v){return co.aplomo.servicio.Validacion.opcion(v,"el segmento",Arrays.stream(ORDEN).map(a->a[0]).toArray(String[]::new));}}

    /** Reutiliza el editor y el esfuerzo actuales; la secuencia y sus respuestas las dirige recorrido.js. */
    public static String pasos(){
        StringBuilder h=new StringBuilder("<section class=\"card stack tour-pregunta\" data-paso=\"7\" hidden><h2>¿Qué lado tiene más carga?</h2><div class=\"ayuda-lado\" data-ayuda-lado><p>¿Cómo saber qué lado tiene más carga? Observa varios ciclos de la tarea:</p><ul><li>¿Con qué mano sostiene la herramienta, la pieza o la carga?</li><li>¿Qué brazo se levanta más o se aleja más del cuerpo?</li><li>¿Qué brazo repite más el movimiento?</li><li>¿Qué muñeca se dobla o gira más?</li></ul><p>Si no está claro, elige «Ambos».</p></div><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Lado que vas a evaluar\">");
        for(var lado:new String[][]{{"DERECHO","Derecho","lado_derecho.png"},{"IZQUIERDO","Izquierdo","lado_izquierdo.png"},{"AMBOS","Ambos","lado_ambos.png"}})h.append(opcion("lado_recorrido",lado[0],lado[1],lado[2],false,""));
        h.append("</div></section>").append(PaginasAnalisis.pasos().replace("data-paso=\"4\"","data-paso=\"8\"").replace("data-paso=\"5\"","data-controles-anteriores").replace("data-paso=\"6\"","data-paso=\"11\""));
        h.append(esfuerzo());
        h.append("<div data-cuerpo-cabecera hidden><div class=\"cuerpo-pildoras\" aria-label=\"Segmentos del cuerpo\">");
        for(var seg:ORDEN)h.append("<span class=\"tag\" data-pildora=\"").append(seg[0]).append("\">").append(seg[1]).append("</span>");
        h.append("</div><p class=\"hint\" data-tarea-cuerpo></p><div class=\"row\"><span class=\"tag\" role=\"status\" data-puntaje-parcial>Puntaje parcial · Pendiente</span><span class=\"hint\" data-final-pendiente>Puntaje final: Pendiente</span></div></div><div data-cuerpo-rangos hidden class=\"stack cuerpo-rangos\"><p role=\"status\" class=\"hint\" data-sugerencia-foto></p>");
        for(var seg:ORDEN){h.append("<div data-rangos=\"").append(seg[0]).append("\" class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Rango de ").append(seg[1].toLowerCase(Locale.ROOT)).append("\" hidden>");
            if(segmento(seg[0])!=null)for(var rango:RANGOS.get(segmento(seg[0])))h.append(opcion("rango_"+seg[0],""+rango.grados(),rango.etiqueta(),rango.imagen(),false,"data-rango-segmento=\""+seg[0]+"\""));
            else if(seg[0].equals("GIRO")){h.append(opcion("rango_GIRO","MEDIO","Rango medio","giro_medio.png",false,"data-rango-segmento=\"GIRO\""));h.append(opcion("rango_GIRO","EXTREMO","Cerca del extremo","giro_extremo.png",false,"data-rango-segmento=\"GIRO\""));}
            else {h.append(opcion("rango_PIERNAS","si","Bien apoyadas y equilibradas","piernas_apoyadas.png",false,"data-rango-segmento=\"PIERNAS\""));h.append(opcion("rango_PIERNAS","no","Sin buen apoyo o desequilibradas","piernas_no_apoyadas.png",false,"data-rango-segmento=\"PIERNAS\""));}
            h.append("</div>");
        }
        h.append("<button type=\"button\" class=\"link\" data-corregir-cuerpo>Corregir medición</button><div class=\"field\" data-manual-cuerpo hidden><label for=\"angulo-cuerpo\">Ángulo corregido a mano</label><input id=\"angulo-cuerpo\" type=\"number\" class=\"input\" min=\"-180\" max=\"180\" step=\"0.01\"><button type=\"button\" class=\"link\" data-restaurar-foto>Usar los puntos de la foto</button></div><button type=\"button\" class=\"link\" data-no-foto>No se ve en la foto</button></div><dialog class=\"hoja-cuerpo\" aria-modal=\"true\" role=\"dialog\" aria-labelledby=\"titulo-hoja-cuerpo\"><div class=\"stack\"><div class=\"row between\"><h2 id=\"titulo-hoja-cuerpo\">¿Algo más en el segmento?</h2><button type=\"button\" class=\"btn\" data-cerrar-hoja aria-label=\"Cerrar ajustes\">Cerrar</button></div>");
        for(var seg:ORDEN)if(AJUSTES.containsKey(seg[0])){
            h.append("<div class=\"tour-opciones\" data-hoja-segmento=\"").append(seg[0]).append("\" role=\"group\" aria-label=\"Ajustes de ").append(seg[1].toLowerCase(Locale.ROOT)).append("\" hidden>");
            for(var a:AJUSTES.get(seg[0]))h.append(opcion("ajuste_"+a[0],a[0],a[1],a[2],true,"data-cuerpo-ajuste=\""+a[0]+"\""));
            String img=seg[0].equals("CUELLO")?"cuello_ninguno.png":seg[0].equals("TRONCO")?"tronco_ninguno.png":"brazo_ninguno.png";
            h.append(opcion("ninguno_"+seg[0],"NINGUNO","Ninguno",img,true,"data-cuerpo-ninguno")).append("</div>");
        }
        return h.append("<p class=\"tour-pendiente\" role=\"status\" data-aviso-hoja hidden>Responde para continuar.</p><button type=\"button\" class=\"btn p\" data-listo-hoja aria-disabled=\"true\">Listo</button></div></dialog>").toString();
    }
    /** RF-019: correspondencia fija uno a uno con el enum del motor existente. */
    public static final String[][] CARGAS={{"MENOS_2KG_INTERMITENTE","Menos de 2 kg, a ratos","carga_1.png"},{"DE_2_A_10KG_INTERMITENTE","2 a 10 kg, a ratos","carga_2.png"},{"DE_2_A_10KG_ESTATICA_O_REPETITIVA","2 a 10 kg, sostenido o repetido","carga_3.png"},{"MAS_10KG_INTERMITENTE","Más de 10 kg, a ratos","carga_3.png"},{"MAS_10KG_ESTATICA_O_REPETITIVA","Más de 10 kg, sostenido o repetido","carga_4.png"},{"GOLPES_O_FUERZAS_BRUSCAS","Golpes o fuerzas bruscas","carga_4.png"}};
    private static String esfuerzo(){
        StringBuilder h=new StringBuilder("<section class=\"card stack tour-pregunta\" data-paso=\"9\" hidden><p class=\"hint\" data-tarea-esfuerzo></p><h2>¿La postura se mantiene más de 1 minuto o se repite más de 4 veces por minuto?</h2><p class=\"hint\" data-recuerdo-esfuerzo hidden>Respuesta del puesto anterior: revísala y pulsa Continuar para confirmarla.</p><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Actividad muscular\">");
        for(var a:new String[][]{{"ESTATICA","Se mantiene más de 1 minuto","esfuerzo_estatica.png"},{"REPETITIVA","Se repite más de 4 veces por minuto","esfuerzo_repetitiva.png"},{"NINGUNA","Ninguna de las dos","esfuerzo_ninguno.png"}})h.append(opcion("esfuerzo_opcion",a[0],a[1],a[2],false,"data-esfuerzo-opcion"));
        h.append("</div><p class=\"hint\">Puntaje final: Pendiente</p></section><section class=\"card stack tour-pregunta\" data-paso=\"10\" hidden><p class=\"hint\" data-tarea-esfuerzo></p><h2>¿Cuánto pesa lo que maneja?</h2><p class=\"hint\" data-recuerdo-esfuerzo hidden>Respuesta del puesto anterior: revísala y pulsa Continuar para confirmarla.</p><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Carga o fuerza\">");
        for(var c:CARGAS)h.append(opcion("carga_opcion",c[0],c[1],c[2],false,"data-carga-opcion"));
        return h.append("</div><p class=\"hint\">Una resma de papel ≈ 2,5 kg · un galón de agua ≈ 4 kg</p><p class=\"hint\">Puntaje final: Pendiente</p></section>").toString();
    }
    private static Segmento segmento(String s){try{return Segmento.valueOf(s);}catch(IllegalArgumentException e){return null;}}
    private static String opcion(String nombre,String valor,String etiqueta,String imagen,boolean multiple,String extra){
        String img=RecorridoCuerpo.class.getResource("/static/img/tour/"+imagen)==null?"":"<img src=\"/static/img/tour/"+imagen+"\" alt=\"\" loading=\"lazy\">";
        return "<label class=\"tour-opcion\"><input type=\""+(multiple?"checkbox":"radio")+"\" name=\""+nombre+"\" value=\""+valor+"\" "+extra+"><span>"+img+"<span>"+Html.escapar(etiqueta)+"</span></span></label>";
    }
}
