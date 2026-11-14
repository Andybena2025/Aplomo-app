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
        var ficha=contexto.visita();
        var elegido=contexto.puestos().stream().filter(p->p.id()==seleccionado).findFirst().orElse(null);
        if(seleccionado!=0 && elegido==null) throw new IllegalArgumentException("Elige un puesto activo del salón de esta visita.");
        String modo=elegido==null?"":elegido.tipo().equals("ESCRITORIO")?"Oficina":"Planta";
        StringBuilder h=new StringBuilder("<div class=\"pagehead\"><div><span class=\"eyebrow\">Evaluaciones · Nueva</span><h1>Nueva evaluación</h1><p class=\"muted\">")
            .append(escapar(ficha.empresaNombre())).append(" · ").append(escapar(ficha.salonNombre())).append("</p><p class=\"hint\" data-contexto-puesto>")
            .append(elegido==null?"Elige el puesto que vas a evaluar.":"Puesto "+elegido.numero()+" · "+escapar(elegido.descripcion())).append("</p></div><span class=\"tag\" data-modo>").append(modo).append("</span></div>")
            .append("<form data-evaluacion data-tour=\"contexto\" data-cuerpo=\"si\" data-inicio=\"").append(elegido==null?0:1).append("\" method=\"post\" action=\"/evaluaciones\" enctype=\"multipart/form-data\" novalidate>")
            .append("<input type=\"hidden\" name=\"cuenta_recuerdo\" value=\"").append(sesion.cuenta().id()).append("\">").append(Html.csrf(sesion)).append("<input type=\"hidden\" name=\"visita\" value=\"").append(ficha.visita().id()).append("\"><input type=\"hidden\" name=\"recorrido\" value=\"guiado\">")
            .append("<input type=\"hidden\" name=\"motivo\"><input type=\"hidden\" name=\"condiciones_respondidas\">")
            .append("<div class=\"tour-progreso\"><div class=\"row between\"><button type=\"button\" class=\"btn\" data-atras-evaluacion>← Atrás</button><span id=\"pasoActual\" role=\"status\">Paso 1 de ").append(elegido==null?18:17).append("</span></div><progress max=\"").append(elegido==null?18:17).append("\" value=\"1\" aria-label=\"Avance de la evaluación\"></progress></div>");
        if(elegido==null){
            h.append("<section class=\"card stack tour-pregunta\" data-paso=\"0\"><h2>¿Qué puesto vas a evaluar?</h2><div class=\"field\"><label for=\"puesto\">Puesto de trabajo</label><select class=\"input\" id=\"puesto\" name=\"puesto\" required><option value=\"\">Elige un puesto</option>");
            for(var p:contexto.puestos())h.append("<option value=\"").append(p.id()).append("\" data-tipo=\"").append(escapar(p.tipo())).append("\">Puesto ").append(p.numero()).append(" · ").append(escapar(p.descripcion())).append("</option>");
            h.append("</select></div>");
            if(contexto.puestos().isEmpty())h.append("<p class=\"banner\">Este salón no tiene puestos activos</p>").append(PaginasEmpresas.enlace("/salones/"+ficha.salon()+"/puestos/nuevo","Registrar un puesto",false));
            h.append("</section>");
        }else h.append("<input type=\"hidden\" name=\"puesto\" value=\"").append(elegido.id()).append("\" data-tipo=\"").append(escapar(elegido.tipo())).append("\">");
        h.append("<section class=\"card stack tour-pregunta\" data-paso=\"1\"").append(elegido==null?" hidden":"").append("><h2>Consentimiento</h2><label class=\"tour-opcion tour-consentimiento\"><input type=\"checkbox\" name=\"acepta\" value=\"si\" required><span>La persona firmó el consentimiento físico y queda archivado</span></label><p class=\"hint\">La autorización de las fotos se elige después, por separado.</p></section>")
            .append("<section class=\"card stack tour-pregunta\" data-paso=\"2\" hidden><h2>¿Qué postura vas a evaluar?</h2><p class=\"muted\" data-consejo-modo></p><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Postura que vas a evaluar\" data-posturas>");
        String[][] posturas={{"de_pie","De pie en mesa o línea"},{"computador","Sentado en computador"},{"sentado","Sentado sin computador"},{"inclinado","Inclinado hacia adelante"},{"brazos_arriba","Brazos por encima de los hombros"},{"cargando","Levantando o cargando"},{"herramienta","Con herramienta en mano"},{"agachado","Agachado o arrodillado"}};
        for(var p:posturas)h.append(opcion("postura_opcion",p[1],p[1],"postura_"+p[0]+".jpg",false,"data-postura=\""+p[0]+"\""));
        h.append("</div><button type=\"button\" class=\"link\" data-editar-postura aria-expanded=\"false\">Editar descripción</button><div class=\"field\" data-descripcion-postura hidden><label for=\"postura\">Descripción de la postura</label><input id=\"postura\" class=\"input\" name=\"postura\" maxlength=\"150\" required></div></section>")
            .append("<section class=\"card stack tour-pregunta\" data-paso=\"3\" hidden><h2>¿Qué momento vas a evaluar?</h2><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Momento de la tarea\">")
            .append(opcion("criterio","MAS_INADECUADA","La postura más exigente","",false,""))
            .append(opcion("criterio","MAYOR_EXPOSICION","La que más tiempo dura","",false,""))
            .append("</div></section><section class=\"card stack tour-pregunta\" data-paso=\"4\" hidden><h2>¿Cuánto tiempo de la jornada?</h2><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Tiempo por jornada\">");
        String[][] tiempos={{"30","Menos de 1 h"},{"90","1–2 h"},{"180","2–4 h"},{"300","Más de 4 h"}};
        for(var t:tiempos)h.append(opcion("minutos",t[0],t[1],"",false,""));
        h.append("</div></section><section class=\"card stack tour-pregunta\" data-paso=\"5\" hidden><h2>¿Qué ves en el puesto?</h2><div class=\"tour-opciones\" role=\"group\" aria-label=\"Condiciones del puesto\">");
        for(var c:CONDICIONES)h.append(opcion(c[0],"si",c[1],"",true,"aria-describedby=\"ayuda-"+c[0]+"\""));
        h.append(opcion("ninguna","si","Ninguna","",true,"data-ninguna-condicion"))
            .append("</div><details class=\"guia\"><summary>Ayuda · cómo reconocer las condiciones</summary><ul class=\"ayuda-condiciones\">");
        for(var c:CONDICIONES)h.append("<li id=\"ayuda-").append(c[0]).append("\"><b>").append(c[1]).append(":</b> ").append(c[2]).append("</li>");
        h.append("</ul></details>").append(Formularios.texto(Map.of(),"observaciones","Otras observaciones (opcional)",255,false)).append("<p class=\"hint\">Máximo 255 caracteres. La nota se guarda completa y no se añade al motivo.</p></section>")
            .append("<section class=\"card stack tour-pregunta\" data-paso=\"6\" hidden><h2>Fotos</h2><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Registro fotográfico\">")
            .append(opcion("registro","CON_FOTOS","Con fotos","",false,""))
            .append(opcion("registro","SIN_REGISTRO_FOTOGRAFICO","Sin fotos (la persona no aceptó fotos)","",false,""))
            .append("</div><div data-con-fotos hidden class=\"stack\"><p class=\"hint\">De 1 a 10 fotos JPG o PNG de hasta 5 MB. Elige para cada una si sirve para medir o de apoyo.</p><div data-lado-principal><h3>Perfil, frente y detalle</h3></div><div class=\"slots\" data-lado-principal>")
            .append(slot("perfil","Perfil","PERFIL","Cámara a 90° del costado, a la altura de la cadera y a 2–3 metros. Que se vean la cabeza y los pies.","perfil"))
            .append(slot("frente","Frente","FRENTE","Cámara de frente, a la altura de la cadera y a 2–3 metros. Mismo momento de la tarea.","frente"))
            .append(slot("detalle","Detalle de mano y muñeca","OTRA","Cámara encima de la mano, a unos 50 cm. Que se vean mano, muñeca e inicio del antebrazo.","detalle"))
            .append("</div><button type=\"button\" class=\"otroLado\" data-otro-lado aria-expanded=\"false\"><b>+ Otro lado del cuerpo</b><br><span class=\"hint\">Puedes registrar también el otro lado.</span></button><div data-lado-extra class=\"slots\" hidden>")
            .append(slot("perfil_otro","Perfil · otro lado","PERFIL","Toma el perfil del otro lado del cuerpo.","perfil"))
            .append(slot("detalle_otro","Detalle · otro lado","OTRA","Si hace falta, registra también la mano y la muñeca del otro lado.","detalle"))
            .append("</div><div class=\"stack\" data-fotos-elegidas></div><p class=\"hint\">Las imágenes de muestra son de demostración, generadas con IA. No se guardan como fotos de la evaluación.</p></div></section>")
            .append(RecorridoCuerpo.pasos())
            .append("<p class=\"tour-pendiente\" data-pregunta-pendiente role=\"status\" hidden>Responde para continuar.</p><div class=\"aviso\" role=\"alert\" data-error-evaluacion hidden><span></span><button type=\"button\" class=\"link\" data-cerrar-error aria-label=\"Cerrar aviso\">Cerrar</button></div><div class=\"wizfoot\"><button type=\"button\" class=\"btn p\" data-siguiente aria-disabled=\"true\">Continuar →</button>").append(PaginasAnalisis.botonesGuardar()).append("</div></form>").append(PaginasAnalisis.scripts()).append("");
        return Html.pagina("Nueva evaluación",h.toString(),sesion,false,false);
    }

    private static String opcion(String nombre,String valor,String etiqueta,String imagen,boolean multiple,String atributos) {
        String img=!imagen.isEmpty() && PaginasEvaluacion.class.getResource("/static/img/tour/"+imagen)!=null ? "<img src=\"/static/img/tour/"+imagen+"\" alt=\"\" loading=\"lazy\">" : "";
        return "<label class=\"tour-opcion"+(imagen.startsWith("postura_")?" tour-postura":"")+"\"><input type=\""+(multiple?"checkbox":"radio")+"\" name=\""+nombre+"\" value=\""+escapar(valor)+"\" "+atributos+"><span>"+img+"<span>"+escapar(etiqueta)+"</span></span></label>";
    }

    private static String slot(String id, String titulo, String vista, String ayuda, String muestra) {
        return "<section class=\"slot\" data-slot-id=\""+id+"\"><div class=\"slotTop\"><span class=\"eyebrow\">"+titulo+"</span><span class=\"tag\">Muestra</span></div>"
                +"<div class=\"slotImg\"><img src=\"/static/img/muestra_"+muestra+".jpg\" alt=\"Muestra de "+titulo.toLowerCase(Locale.ROOT)+"\"><span class=\"slotLbl\">Muestra</span></div>"
                +"<p class=\"small\">"+ayuda+"</p><div class=\"row\"><button class=\"btn\" type=\"button\" data-elegir-foto=\"cam_"+id+"\">Tomar foto</button><button class=\"btn p\" type=\"button\" data-elegir-foto=\"foto_"+id+"\">Subir foto</button></div>"
                +"<input id=\"cam_"+id+"\" type=\"file\" accept=\"image/jpeg,image/png\" capture=\"environment\" data-fotos data-vista=\""+vista+"\" data-titulo=\""+titulo+"\" hidden>"
                +"<input id=\"foto_"+id+"\" type=\"file\" accept=\"image/jpeg,image/png\" multiple data-fotos data-vista=\""+vista+"\" data-titulo=\""+titulo+"\" hidden><span class=\"hint\">JPG o PNG · máximo 5 MB por foto</span></section>";
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
