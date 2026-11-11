package co.aplomo.soporte;

import co.aplomo.rula.*;
import co.aplomo.rula.MotorRula.*;
import co.aplomo.rula.MedidorAngulos.*;
import co.aplomo.rula.PuntuadorSegmentos.*;
import java.util.*;

/** Diez entradas manuales independientes del motor, para HTTP, persistencia y evidencia visual. */
public final class CasosRulaPrueba {
    private CasosRulaPrueba() {}
    /** Caso manual con final esperado explícito. */
    public record Caso(int numero,Entrada entrada,int esperado) {}
    /** Entrega los diez casos de docs/03 con NEUTRO=5°, sin obtener sus esperados del propio cálculo. */
    public static List<Caso> todos(){
        var sin=Ajustes.sinAjustes();var hombro=new Ajustes(true,true,false,false,false,Giro.MEDIO,false,false,false,false,true,false);
        var max=new Ajustes(true,true,false,true,true,Giro.EXTREMO,true,true,true,true,false,false);
        return List.of(caso(1,new double[]{1,85,9,10,2},sin,false,Carga.MENOS_2KG_INTERMITENTE,2),
                caso(2,new double[]{61,47,0,37,17},sin,false,Carga.MENOS_2KG_INTERMITENTE,3),
                caso(3,new double[]{61,47,0,37,17},hombro,false,Carga.MENOS_2KG_INTERMITENTE,4),
                caso(4,new double[]{0,90,0,0,0},sin,false,Carga.MENOS_2KG_INTERMITENTE,1),
                caso(5,new double[]{0,90,0,0,0},sin,true,Carga.MENOS_2KG_INTERMITENTE,2),
                caso(6,new double[]{0,90,0,0,0},sin,false,Carga.DE_2_A_10KG_INTERMITENTE,2),
                caso(7,new double[]{61,47,0,37,17},sin,false,Carga.DE_2_A_10KG_ESTATICA_O_REPETITIVA,6),
                caso(8,new double[]{61,47,0,37,17},sin,true,Carga.MAS_10KG_INTERMITENTE,7),
                caso(9,new double[]{0,90,0,0,0},sin,false,Carga.MAS_10KG_ESTATICA_O_REPETITIVA,4),
                caso(10,new double[]{120,20,30,-20,70},max,true,Carga.GOLPES_O_FUERZAS_BRUSCAS,7));
    }
    private static Caso caso(int n,double[] gs,Ajustes a,boolean actividad,Carga carga,int fin){
        var m=new EnumMap<Segmento,Medicion>(Segmento.class);for(var s:Segmento.values())m.put(s,new Medicion(List.of(),gs[s.ordinal()]));
        return new Caso(n,new Entrada(Lado.DERECHO,200,400,m,a,actividad,carga),fin);
    }
    /** Codifica las entradas como lo hace el formulario web; no transmite puntajes esperados. */
    public static Map<String,String> campos(Entrada e){
        Map<String,String> f=new LinkedHashMap<>();f.put("lado",e.lado().name());f.put("ancho",""+e.ancho());f.put("alto",""+e.alto());
        for(var s:Segmento.values()){var m=e.mediciones().get(s);f.put(s+"_modo",m.manual()==null?"PUNTOS":"MANUAL");if(m.manual()!=null)f.put(s+"_grados",""+m.manual());f.put(s+"_puntos",m.puntos().stream().map(p->p.x()+","+p.y()).collect(java.util.stream.Collectors.joining(";")));}
        var a=e.ajustes();String[] ks={"hombroElevado","brazoSeparado","brazoApoyado","antebrazoCruza","munecaDesviada","cuelloGirado","cuelloInclinado","troncoGirado","troncoInclinado","piernasApoyadas","actividad"};
        boolean[] vs={a.hombroElevado(),a.brazoSeparado(),a.brazoApoyado(),a.antebrazoCruza(),a.munecaDesviada(),a.cuelloGirado(),a.cuelloInclinado(),a.troncoGirado(),a.troncoInclinado(),a.piernasApoyadas(),e.actividad()};
        for(int i=0;i<ks.length;i++)f.put(ks[i],vs[i]?"si":"no");f.put("giro",a.giro().name());f.put("carga",e.carga().name());return f;
    }
    /** Codifica una entrada por foto, conservando UTF-8 y los separadores. */
    public static String codificar(Map<String,String> campos){return campos.entrySet().stream().map(e->java.net.URLEncoder.encode(e.getKey(),java.nio.charset.StandardCharsets.UTF_8)+"="+java.net.URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));}
}
