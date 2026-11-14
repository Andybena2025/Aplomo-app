package co.aplomo.web;

import co.aplomo.servicio.Evaluaciones.Contexto;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.util.*;
import static co.aplomo.web.Html.escapar;

/** RF-014/015/016, RNF-002/007: primeros tres pasos con los componentes del mock. */
public final class PaginasEvaluacion {
    private PaginasEvaluacion() {}
    /** Etiquetas y ayudas de las seis condiciones aprobadas de RF-015, en el orden del esquema. */
    public static final String[][] CONDICIONES = {
            {"escritorio", "Escritorio muy alto", "La superficie queda por encima del codo con los hombros relajados."},
            {"pantalla", "Pantalla muy alta", "El borde superior queda por encima de la línea de los ojos."},
            {"pies", "Pies colgando", "Los pies no apoyan completos en el piso."},
            {"apoyapies", "Apoyapiés", "El puesto tiene apoyapiés."},
            {"apoyabrazos", "Silla con apoyabrazos", "La silla tiene apoyabrazos."},
            {"regulable", "Silla regulable", "La silla se puede regular."}};

    /** Recorrido guiado de contexto y fotos; conserva provisionalmente la medición existente. */
    public static String nueva(Sesion sesion, Contexto contexto) { return nueva(sesion,contexto,0); }

    public static String nueva(Sesion sesion, Contexto contexto, int seleccionado) {
        return RevisionEvaluacion.nueva(sesion,contexto,seleccionado);
    }

    /** Opciones con pista vinculada y nombre independiente de su descripción. */
    static String opcion(String nombre,String valor,String etiqueta,String imagen,boolean multiple,String atributos,String ayuda) {
        String img=!imagen.isEmpty() && PaginasEvaluacion.class.getResource("/static/img/tour/"+imagen)!=null ? "<img src=\"/static/img/tour/"+imagen+"\" alt=\"\" loading=\"lazy\">" : "";
        String id="tour-"+nombre+"-"+HexFormat.of().formatHex(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String pista=ayuda.isEmpty()?"":"<small class=\"tour-ayuda\" id=\""+id+"-ayuda\">"+escapar(ayuda)+"</small>";
        return "<label class=\"tour-opcion"+(imagen.startsWith("postura_")?" tour-postura":"")+"\"><input type=\""+(multiple?"checkbox":"radio")+"\" name=\""+nombre+"\" value=\""+escapar(valor)+"\" "+atributos+" aria-labelledby=\""+id+"-texto\""+(ayuda.isEmpty()?"":" aria-describedby=\""+id+"-ayuda\"")+"><span>"+img+"<span class=\"tour-franja\"><span id=\""+id+"-texto\">"+escapar(etiqueta)+"</span>"+pista+"</span></span></label>";
    }

    /** Nueve fases estables; las filas y fotos no crean fases adicionales. */
    static int fase(int paso){return Math.max(1,Math.min(9,paso));}
    static String fases(String html){return java.util.regex.Pattern.compile("data-paso=\"(\\d+)\"").matcher(html).replaceAll(m->m.group()+" data-fase=\""+fase(Integer.parseInt(m.group(1)))+"\"");}

    static List<String> posturasDelModo(String modo) {
        return modo.equals("Oficina")?List.of("computador","sentado","inclinado","brazos_arriba","herramienta","otra"):List.of("de_pie","cargando","inclinado","brazos_arriba","herramienta","agachado","otra");
    }

    /** Muestra los campos guardados y abre las fotos exclusivamente por su ruta protegida. */
    public static String detalle(Sesion sesion, co.aplomo.servicio.Evaluaciones.Consulta consulta, List<co.aplomo.dao.AnalisisDao.Resumen> analisis, String aviso) {
        var f=consulta.ficha();var d=f.datos();
        StringBuilder h=new StringBuilder(Html.aviso(aviso,true)).append(PaginasEmpresas.enlace("/visitas/"+f.visita(),"← Volver a la visita",false));
        h.append("<section class=\"card stack\"><span class=\"eyebrow\">").append(Html.fecha(f.fecha())).append(" · Registró ")
                .append(escapar(f.responsable())).append("</span><h2>").append(escapar(d.postura())).append("</h2><p>")
                .append(d.criterio().equals("MAS_INADECUADA")?"Postura más inadecuada":"Mayor tiempo de exposición").append(" · ").append(d.minutos()).append(" minutos por jornada</p><p>")
                .append(escapar(d.motivo())).append("</p><h3>Condiciones del puesto</h3><div class=\"opts\">");
        boolean[] marcadas={d.escritorio(),d.pantalla(),d.pies(),d.apoyapies(),d.apoyabrazos(),d.regulable()};
        for(int i=0;i<CONDICIONES.length;i++)h.append("<span class=\"tag\">").append(CONDICIONES[i][1]).append(": ").append(marcadas[i]?"Sí":"No").append("</span>");
        h.append("</div><p class=\"texto-notas\">").append(escapar(d.observaciones())).append("</p><span class=\"tag\">")
                .append(d.sinFotos()?"Sin registro fotográfico":"La persona aceptó las fotos").append("</span></section><section class=\"stack\"><h2>Fotos</h2>");
        if(d.sinFotos())h.append("<p class=\"muted\">Esta evaluación no tiene registro fotográfico.</p>");
        h.append("<div class=\"grid2\">");
        for(var foto:consulta.fotos())h.append("<figure class=\"card stack foto-guardada\"><img src=\"/fotos/").append(foto.id()).append("\" alt=\"")
                .append(escapar(foto.descripcion())).append("\"><figcaption>").append(Formularios.etiqueta(foto.vista())).append(" · ").append(escapar(foto.descripcion())).append("</figcaption></figure>");
        h.append("</div></section><section class=\"stack\"><h2>Análisis guardados</h2>");
        if(analisis.isEmpty())h.append("<p class=\"muted\">Todavía no hay análisis. La evaluación conserva sus datos sin puntaje.</p>");
        for(var a:analisis)h.append("<div class=\"card row between\"><div>Foto ").append(a.foto()).append(" · ").append(Formularios.etiqueta(a.lado())).append("<div class=\"hint\">").append(Html.fecha(a.fecha().toLocalDate())).append(" · Registró ").append(escapar(a.responsable())).append("</div></div><span class=\"pill ").append(a.color()).append("\">").append(a.puntaje()).append(" · ").append(Formularios.etiqueta(a.color())).append(" · Nivel ").append(a.nivel()).append("</span>").append(PaginasEmpresas.enlace("/evaluaciones/"+f.id()+"/analisis/"+a.id(),"Ver resultado",false)).append("</div>");
        h.append("</section>");
        if(consulta.enCurso()&&!consulta.fotos().isEmpty())h.append(PaginasEmpresas.enlace("/evaluaciones/"+f.id()+"/analizar","Analizar fotos / añadir análisis",true));
        return Html.pagina("Evaluación · Puesto "+f.numero(),h.toString(),sesion);
    }
}
