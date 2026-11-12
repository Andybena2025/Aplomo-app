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

    /** Presenta un único formulario para volver entre pasos sin perder campos ni archivos seleccionados. */
    public static String nueva(Sesion sesion, Contexto contexto) {
        var ficha = contexto.visita();
        StringBuilder html = new StringBuilder("<div class=\"pagehead\"><div><span class=\"eyebrow\">")
                .append(escapar(ficha.empresaNombre())).append(" · ").append(escapar(ficha.salonNombre()))
                .append("</span><h1>Nueva evaluación</h1></div><span class=\"tag\" id=\"pasoActual\">Paso 1 de 6</span></div>")
                .append("<div class=\"steps\" aria-label=\"Pasos de la evaluación\">");
        String[] pasos = {"Puesto y postura", "Condiciones", "Fotos", "Ángulos", "Ajustes y carga", "Resultado"};
        for (int i=0;i<pasos.length;i++) html.append("<div data-indicador=\"").append(i+1).append("\" class=\"")
                .append(i==0?"on":"").append("\"><i></i><span>").append(i+1).append(" · ").append(pasos[i]).append("</span></div>");
        html.append("</div><form data-evaluacion method=\"post\" action=\"/evaluaciones\" enctype=\"multipart/form-data\" novalidate>")
                .append(Html.csrf(sesion)).append("<input type=\"hidden\" name=\"visita\" value=\"").append(ficha.visita().id()).append("\">")
                .append("<section class=\"card stack\" data-paso=\"1\"><h2>Puesto y postura</h2><p class=\"muted small\">Elige el puesto, la postura o tarea que vas a estudiar y el tiempo de exposición. No se registran datos personales del trabajador.</p>")
                .append("<div class=\"grid2\"><div class=\"field\"><label for=\"puesto\">Puesto de trabajo</label><select class=\"input\" id=\"puesto\" name=\"puesto\" required><option value=\"\">Elige un puesto</option>");
        for (var puesto : contexto.puestos()) html.append("<option value=\"").append(puesto.id()).append("\">Puesto ")
                .append(puesto.numero()).append(" · ").append(escapar(puesto.descripcion())).append("</option>");
        html.append("</select><span class=\"hint\">Solo puestos activos del salón de esta visita.</span></div>")
                .append(Formularios.campo(Map.of(), "postura", "Postura o tarea que se observa", "text", 150, true))
                .append(Formularios.selector(Map.of(), "criterio", "Criterio de elección", List.of(new Formularios.Opcion("MAS_INADECUADA", "Postura más inadecuada"), new Formularios.Opcion("MAYOR_EXPOSICION", "Mayor tiempo de exposición")), true))
                .append("<div class=\"field\"><label for=\"minutos\">Minutos por jornada en esta postura</label><input class=\"input num\" type=\"number\" id=\"minutos\" name=\"minutos\" min=\"1\" max=\"720\" step=\"1\" required></div></div>")
                .append(Formularios.texto(Map.of(), "motivo", "Motivo de elección de la postura", 255, true));
        if (contexto.puestos().isEmpty()) html.append("<p class=\"banner\">No hay puestos activos en este salón. Registra o reactiva uno antes de evaluar.</p>");
        html.append("</section><section class=\"card stack\" data-paso=\"2\" hidden><h2>Condiciones del puesto</h2><p class=\"muted small\">Cómo es el puesto donde se trabaja. Marca lo que veas y escribe cualquier otra observación.</p><span class=\"eyebrow\">Qué hay en el puesto</span><div class=\"opts\">");
        for (String[] c:CONDICIONES) html.append("<label class=\"opt\" title=\"").append(escapar(c[2])).append("\"><input type=\"checkbox\" name=\"")
                .append(c[0]).append("\" value=\"si\"><span>").append(c[1]).append("</span></label>");
        html.append("</div><details class=\"guia\"><summary>Ayuda · cómo reconocer las condiciones</summary><ul class=\"ayuda-condiciones\">");
        for (String[] c:CONDICIONES) html.append("<li><b>").append(c[1]).append(":</b> ").append(c[2]).append("</li>");
        html.append("</ul></details>").append(Formularios.texto(Map.of(), "observaciones", "Otras observaciones", 255, false))
                .append("<span class=\"hint\">Máximo 255 caracteres.</span></section>")
                .append("<section class=\"card stack\" data-paso=\"3\" hidden><h2>Consentimiento y fotos</h2><p class=\"muted small\">Antes de tomar fotos, explica el estudio y pregunta a la persona. La empresa recibe los resultados sin fotos.</p>")
                .append("<label class=\"check\"><input type=\"radio\" name=\"registro\" value=\"CON_FOTOS\"><span><b>Con registro fotográfico</b><br><span class=\"hint\">De 1 a 10 fotos JPG o PNG, de hasta 5 MB cada una.</span></span></label>")
                .append("<label class=\"check\"><input type=\"radio\" name=\"registro\" value=\"SIN_REGISTRO_FOTOGRAFICO\"><span><b>Sin registro fotográfico</b><br><span class=\"hint\">La persona no acepta las fotos. No se sube ninguna imagen.</span></span></label>")
                .append("<div data-con-fotos hidden class=\"stack\"><label class=\"check\"><input type=\"checkbox\" name=\"acepta\" value=\"si\"><span>A la persona se le explicó el estudio y aceptó las fotos.</span></label>")
                .append("<div data-lado-principal><span class=\"eyebrow\">Tomas del puesto</span><h3>Perfil, frente y detalle</h3><p class=\"small muted\">Incluye una toma donde se vean completos los segmentos a estudiar. La de frente ayuda a revisar los ajustes y el detalle muestra la mano y la muñeca.</p></div><div class=\"slots\" data-lado-principal>")
                .append(slot("perfil", "Perfil", "PERFIL", "Cámara a 90° del costado, a la altura de la cadera y a 2–3 metros. Que se vean la cabeza y los pies.", "perfil"))
                .append(slot("frente", "Frente", "FRENTE", "Cámara de frente, a la altura de la cadera y a 2–3 metros. Mismo momento de la tarea.", "frente"))
                .append(slot("detalle", "Detalle de mano y muñeca", "OTRA", "Cámara encima de la mano, a unos 50 cm. Que se vean mano, muñeca e inicio del antebrazo.", "detalle"))
                .append("</div><button type=\"button\" class=\"otroLado\" data-otro-lado aria-expanded=\"false\"><b>+ Otro lado del cuerpo</b><br><span class=\"hint\">Si dudas cuál lado tiene más carga, puedes registrar también el otro lado.</span></button><div data-lado-extra class=\"slots\" hidden>")
                .append(slot("perfil_otro", "Perfil · otro lado", "PERFIL", "Toma el perfil del otro lado del cuerpo. La foto de frente sirve para ambos lados.", "perfil"))
                .append(slot("detalle_otro", "Detalle · otro lado", "OTRA", "Si hace falta, registra también la mano y la muñeca del otro lado.", "detalle"))
                .append("</div><div class=\"stack\" data-fotos-elegidas></div><p class=\"hint\">Las imágenes de muestra son de demostración, generadas con IA. No se guardan como fotos de la evaluación.</p></div>")
                .append("</section>").append(PaginasAnalisis.pasos()).append("<div class=\"aviso\" role=\"alert\" data-error-evaluacion hidden><span></span><button type=\"button\" class=\"link\" data-cerrar-error aria-label=\"Cerrar aviso\">Cerrar</button></div>")
                .append("<div class=\"wizfoot\"><button type=\"button\" class=\"btn\" data-atras-evaluacion>Cancelar</button><button type=\"button\" class=\"btn p\" data-siguiente>Siguiente →</button><button type=\"submit\" class=\"btn p\" data-guardar-evaluacion hidden>Guardar evaluación</button></div></form><script src=\"/static/js/analisis.js\" defer></script><script src=\"/static/js/evaluacion.js\" defer></script>");
        return Html.pagina("Nueva evaluación", html.toString(), sesion, false);
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
