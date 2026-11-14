package co.aplomo.web;

import co.aplomo.servicio.Evaluaciones.Contexto;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.rula.MotorRula.Segmento;
import java.util.*;
import static co.aplomo.web.Html.escapar;

/** RF-014 a RF-020: revisión explícita, sin cambiar la entrada ni las fórmulas del motor. */
final class RevisionEvaluacion {
    private RevisionEvaluacion(){}
    static String nueva(Sesion s,Contexto c,int seleccionado){
        return construir(s,c.puestos(),seleccionado,c.visita().visita().id(),c.visita().empresaNombre(),c.visita().salonNombre());
    }
    private static String construir(Sesion s,List<co.aplomo.modelo.Puesto> puestos,int seleccionado,int visita,String empresa,String salon){
        var p=puestos.stream().filter(x->x.id()==seleccionado).findFirst().orElse(null);
        if(seleccionado!=0&&p==null)throw new IllegalArgumentException("Elige un puesto activo del salón de esta visita.");
        StringBuilder h=new StringBuilder("<div class=\"pagehead\"><div><span class=\"eyebrow\">Evaluaciones · Nueva</span><h1>Nueva evaluación</h1><p class=\"muted\">")
            .append(escapar(empresa)).append(" · ").append(escapar(salon)).append("</p><p class=\"hint\">")
            .append(p==null?"Elige un puesto":"Puesto "+p.numero()+" · "+escapar(p.descripcion())).append("</p></div><span class=\"tag\" data-modo>")
            .append(p==null||p.tipo().isEmpty()?"":p.tipo().equals("ESCRITORIO")?"Oficina":"Planta").append("</span></div><form data-evaluacion data-revision=\"si\" data-tour=\"revision\" data-fases=\"7\" action=\"/evaluaciones\" method=\"post\" enctype=\"multipart/form-data\" novalidate>")
            .append(Html.csrf(s)).append("<input type=\"hidden\" name=\"cuenta_recuerdo\" value=\"").append(s.cuenta().id()).append("\"><input type=\"hidden\" name=\"visita\" value=\"").append(visita)
            .append("\"><input type=\"hidden\" name=\"recorrido\" value=\"guiado\"><input type=\"hidden\" name=\"motivo\"><input type=\"hidden\" name=\"condiciones_respondidas\">");
        if(p==null){
            h.append("<div class=\"field\"><label for=\"puesto\">Puesto de trabajo</label><select class=\"input\" name=\"puesto\" id=\"puesto\"><option value=\"\">Elige un puesto</option>");
            for(var x:puestos)h.append("<option value=\"").append(x.id()).append("\" data-tipo=\"").append(escapar(x.tipo())).append("\" data-tarea=\"").append(escapar(x.descripcion())).append("\">Puesto ").append(x.numero()).append(" · ").append(escapar(x.descripcion())).append("</option>");
            h.append("</select></div>");
        }else h.append("<input type=\"hidden\" name=\"puesto\" value=\"").append(p.id()).append("\" data-tipo=\"").append(escapar(p.tipo())).append("\">");
        h.append("<div class=\"tour-progreso\"><div class=\"row between\"><button class=\"btn\" type=\"button\" data-atras-evaluacion>← Atrás</button><span id=\"pasoActual\" role=\"status\">Paso 1 de 7</span></div><progress max=\"7\" value=\"1\" aria-label=\"Avance de la evaluación\"></progress></div>")
            .append("<section class=\"card stack tour-pregunta\" data-paso=\"1\"><h2>Consentimiento y permiso de fotos</h2>")
            .append(op("acepta","si","La persona firmó en papel el consentimiento para ser evaluada y quedó archivado.","",true,"required","Este permiso es para observar y evaluar su puesto. Las fotos son un permiso aparte."))
            .append("<div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Permiso de fotos\">")
            .append(op("registro","CON_FOTOS","Sí, permite fotos","",false,"","La persona permite fotografiar su postura para esta evaluación."))
            .append(op("registro","SIN_REGISTRO_FOTOGRAFICO","No permite fotos","",false,"","Se guarda la evaluación sin fotos ni puntaje RULA."))
            .append("</div></section><section class=\"card stack tour-pregunta contexto-revision\" data-paso=\"2\" hidden><h2>Contexto de la evaluación</h2><div class=\"stack\"><h3>Tarea</h3><div class=\"row\"><strong data-tarea-visible>")
            .append(escapar(p==null?"Elige un puesto":p.descripcion())).append("</strong><button type=\"button\" class=\"link\" data-cambiar-tarea>Cambiar</button></div><div class=\"field\" data-editar-tarea hidden><label for=\"postura\">Descripción de la postura</label><input class=\"input\" name=\"postura\" id=\"postura\" maxlength=\"150\" value=\"")
            .append(escapar(p==null?"":p.descripcion())).append("\" required></div><div class=\"row\" data-atajos-tarea>");
        for(String t:List.of("Levantar una caja","Trabajar inclinado","Brazos arriba","Con herramienta","Agachado","Otra"))h.append("<button class=\"btn sm\" type=\"button\" data-tarea=\"").append(t).append("\">").append(t).append("</button>");
        h.append("</div></div><div class=\"stack\"><h3>Momento</h3><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Momento de la tarea\">")
            .append(op("criterio","MAS_INADECUADA","Es el momento de mayor esfuerzo postural","",false,"","Aunque dure poco: por ejemplo, al alcanzar, inclinarse o sostener una carga."))
            .append(op("criterio","MAYOR_EXPOSICION","Es la postura que mantiene durante más tiempo","",false,"","Por ejemplo, trabajar inclinado sobre una mesa durante gran parte de la tarea."))
            .append("</div><details><summary>Cómo elegir</summary><p class=\"hint\">Al colocar una caja arriba, puedes evaluar ese instante exigente; si observas una mesa de trabajo, puedes elegir la postura que se mantiene más tiempo. Observa varios ciclos antes de elegir.</p></details></div><div class=\"stack\"><h3>Tiempo por jornada</h3><p class=\"hint\">Suma todos los ratos del turno en que la persona está en esta postura.</p><div class=\"tour-opciones tiempo-revision\" role=\"radiogroup\" aria-label=\"Tiempo por jornada\">");
        for(var t:new String[][]{{"30","Menos de 1 h"},{"90","1–2 h"},{"180","2–4 h"},{"300","Más de 4 h"}})h.append(op("minutos",t[0],t[1],"",false,"",""));
        h.append("</div></div></section><section class=\"card stack tour-pregunta\" data-paso=\"3\" hidden><h2>¿Qué ves en el puesto?</h2><div class=\"tour-opciones\" role=\"group\" aria-label=\"Condiciones del puesto\">");
        for(var x:PaginasEvaluacion.CONDICIONES)h.append(op(x[0],"si",x[1],"",true,"",x[2]));
        h.append(op("ninguna","si","Ninguna","",true,"","No ves ninguna de estas condiciones.")).append("</div>")
            .append(Formularios.texto(Map.of(),"observaciones","Otras observaciones (opcional)",255,false))
            .append("</section><section class=\"card stack tour-pregunta\" data-paso=\"4\" hidden><h2>¿Qué lado tiene más carga?</h2><div class=\"lado-revision\"><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Lado que vas a evaluar\">");
        for(var x:new String[][]{{"DERECHO","Derecho","lado_derecho.png"},{"IZQUIERDO","Izquierdo","lado_izquierdo.png"}})h.append(op("lado_recorrido",x[0],x[1],x[2],false,"","Evalúa primero este lado."));
        h.append("</div><div class=\"ayuda-lado\"><p>Observa varios ciclos de la tarea:</p><ul><li>¿Con qué mano sostiene la herramienta, la pieza o la carga?</li><li>¿Qué brazo se levanta más o se aleja más del cuerpo?</li><li>¿Qué brazo repite más el movimiento?</li><li>¿Qué muñeca se dobla o gira más?</li></ul><p>Si no está claro, elige Ambos.</p></div>")
            .append(op("lado_recorrido","AMBOS","Ambos","lado_ambos.png",false,"","Revisa primero el derecho; después podrás revisar el izquierdo."))
            .append("</div></section><section class=\"card stack tour-pregunta revision-mediciones\" data-paso=\"5\" hidden><h2>Foto de perfil y revisión</h2><p class=\"hint\" data-momento-revision></p><strong data-lado-titulo></strong><div class=\"revision-layout\"><div class=\"stack revision-foto\"><div class=\"row\" data-subir-perfil><button type=\"button\" class=\"btn p\" data-perfil>Subir foto de perfil</button><input hidden type=\"file\" accept=\"image/jpeg,image/png\" data-archivo-perfil></div><div data-foto-revision></div><div class=\"row\" data-evidencias></div><details data-agregar-foto><summary>Agregar foto</summary><div class=\"row\">");
        for(var x:new String[][]{{"FRENTE","Frente"},{"OTRA","Detalle de mano y muñeca"},{"EXTRA","Extra"}})h.append("<button type=\"button\" class=\"btn\" data-agregar=\"").append(x[0]).append("\">").append(x[1]).append("</button>");
        h.append("</div><input hidden type=\"file\" accept=\"image/jpeg,image/png\" data-archivo-extra><p class=\"hint\">Frente: revisa separación del brazo, giro e inclinación. Detalle: revisa la muñeca. Las fotos corresponden a la misma postura.</p></details><p class=\"hint\" role=\"status\" data-estado-evidencia></p></div><div class=\"revision-lista\" tabindex=\"0\" aria-label=\"Revisión de los siete segmentos\">");
        for(var seg:RecorridoCuerpo.ORDEN){
            String k=seg[0];h.append("<details class=\"revision-fila\" data-revision-segmento=\"").append(k).append("\"><summary><strong>").append(seg[1]).append("</strong><span data-resumen-segmento>Sin medición</span><span class=\"tag\" data-revisar>Revisar</span><span class=\"tag\" data-fuente>Perfil</span></summary><div class=\"stack revision-opciones\"><h3>").append(seg[1]).append("</h3><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Rango de ").append(seg[1].toLowerCase(Locale.ROOT)).append("\">");
            if(!k.equals("GIRO")&&!k.equals("PIERNAS"))for(var r:RecorridoCuerpo.RANGOS.get(Segmento.valueOf(k)))h.append(op("revision_"+k,""+r.grados(),r.etiqueta(),r.imagen(),false,"data-rango=\""+k+"\"",r.ayuda()));
            else for(var x:k.equals("GIRO")?new String[][]{{"MEDIO","Rango medio","giro_medio.png"},{"EXTREMO","Cerca del extremo","giro_extremo.png"}}:new String[][]{{"si","Bien apoyadas y equilibradas","piernas_apoyadas.png"},{"no","Sin buen apoyo o desequilibradas","piernas_no_apoyadas.png"}})h.append(op("revision_"+k,x[0],x[1],x[2],false,"data-rango=\""+k+"\"",k.equals("GIRO")?(x[0].equals("MEDIO")?"La palma gira a medias, sin forzar.":"La palma gira casi al máximo, hacia arriba o hacia abajo."):(x[0].equals("si")?"Los dos pies apoyados y el peso repartido.":"Los pies no apoyan bien o el peso está desequilibrado.")));
            h.append("</div>");
            var ajustes=RecorridoCuerpo.AJUSTES.get(k);
            if(ajustes!=null){h.append("<div class=\"revision-ajustes\"><span class=\"eyebrow\">Opcional</span><div class=\"tour-opciones\">");for(var x:ajustes)h.append(op("revision_ajuste_"+x[0],"si",x[1],x[2],true,"data-ajuste-revision=\""+x[0]+"\"",x[3]));h.append("</div></div>");}
            h.append("<button type=\"button\" class=\"link\" data-volver-foto=\"").append(k).append("\">Volver a la medición de la foto</button><button type=\"button\" class=\"link\" data-no-visible=\"").append(k).append("\">No se ve en la foto</button><p class=\"hint\" data-ayuda-no-visible hidden>Agrega otra foto como evidencia o elige un rango por observación directa del puesto.</p><button type=\"button\" class=\"btn\" data-resolver=\"").append(k).append("\">Usar esta medición</button></div></details>");
        }
        h.append("</div></div><p class=\"hint\" role=\"status\" data-respaldo hidden>No se encontró una persona en la foto. Marca los puntos a mano o elige un rango. Revisa un segmento a la vez.</p><button type=\"button\" class=\"btn p\" data-confirmar-mediciones>Confirmar mediciones</button></section><section class=\"card stack tour-pregunta esfuerzo-revision\" data-paso=\"6\" hidden><h2>Actividad y carga</h2><strong data-lado-esfuerzo></strong><div class=\"stack\"><h3>¿La postura se mantiene más de 1 minuto o se repite más de 4 veces por minuto?</h3><p class=\"hint\">Observa 1 minuto con el reloj</p><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Actividad muscular\">");
        for(var x:new String[][]{{"ESTATICA","Se mantiene más de 1 minuto","esfuerzo_estatica.png"},{"REPETITIVA","Se repite más de 4 veces por minuto","esfuerzo_repetitiva.png"},{"NINGUNA","Ninguna de las dos","esfuerzo_ninguno.png"}})h.append(op("esfuerzo_opcion",x[0],x[1],x[2],false,"",x[0].equals("ESTATICA")?"Sostiene la misma postura sin moverse más de 1 minuto.":x[0].equals("REPETITIVA")?"Repite el mismo movimiento más de 4 veces por minuto.":"Se mueve con libertad, sin sostener ni repetir."));
        h.append("</div></div><div class=\"stack\"><h3>¿Cuánto pesa lo que maneja?</h3><p class=\"hint\">Pregunta al trabajador o pesa la pieza</p><div class=\"tour-opciones carga-revision\" role=\"radiogroup\" aria-label=\"Carga o fuerza\">");
        for(var x:RecorridoCuerpo.CARGAS)h.append(op("carga_opcion",x[0],x[1],x[2],false,"",x[3]));
        h.append("</div></div><p class=\"hint\" data-recuerdo-esfuerzo hidden>Del puesto anterior: revísala y pulsa Continuar para confirmarla.</p></section><section class=\"card stack tour-pregunta\" data-paso=\"7\" hidden><h2>Resultado</h2><div data-resultados></div><button type=\"button\" class=\"btn\" data-evaluar-otro>Evaluar también el otro lado</button></section><div class=\"aviso\" role=\"alert\" data-error-evaluacion hidden><span></span><button type=\"button\" class=\"link\" data-cerrar-error>Cerrar</button></div><div class=\"wizfoot\"><button type=\"button\" class=\"btn p\" data-siguiente>Continuar →</button>")
            .append(PaginasAnalisis.botonesGuardar()).append("</div><div data-editor-compatibilidad hidden>");
        String editor=PaginasAnalisis.pasos();editor=editor.substring(0,editor.indexOf("<section class=\"card stack\" data-paso=\"6\""));
        h.append(editor.replace("data-paso=","data-editor-paso=")).append("</div></form>").append(PaginasAnalisis.scripts());
        return Html.pagina("Nueva evaluación",PaginasEvaluacion.fases(h.toString()),s,false,false);
    }
    /** Añade análisis a fotos existentes sin crear otra evaluación ni alterar su contexto guardado. */
    static String continuar(Sesion s,co.aplomo.servicio.Evaluaciones.Consulta c){
        var f=c.ficha();var d=f.datos();
        var p=new co.aplomo.modelo.Puesto(f.puesto(),0,null,null,f.numero(),d.postura(),"","",null,true);
        String html=construir(s,List.of(p),p.id(),f.visita(),"Evaluación existente","Puesto "+f.numero());
        html=html.replace("action=\"/evaluaciones\"","data-continuar=\""+f.id()+"\" action=\"/evaluaciones/"+f.id()+"/analisis\"");
        StringBuilder fotos=new StringBuilder("<div data-fotos-existentes hidden>");
        for(var foto:c.fotos())fotos.append("<span data-foto-id=\"").append(foto.id()).append("\" data-vista=\"").append(foto.vista()).append("\" data-descripcion=\"").append(escapar(foto.descripcion())).append("\" data-src=\"/fotos/").append(foto.id()).append("\"></span>");
        html=html.replace("</form>",fotos.append("</div></form>").toString());
        return html;
    }
    private static String op(String n,String v,String t,String i,boolean m,String a,String ayuda){return PaginasEvaluacion.opcion(n,v,t,i,m,a,ayuda);}
}
