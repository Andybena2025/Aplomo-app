package co.aplomo.web;

import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.modelo.Puesto;
import co.aplomo.rula.MotorRula.Segmento;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static co.aplomo.web.Html.escapar;

/**
 * RF-014 a RF-020: una foto y una conversación, con los campos y motor actuales.
 */
final class ConversacionEvaluacion {
    private ConversacionEvaluacion() {
    }

    static String nueva(Sesion sesion, Evaluaciones.Contexto evaluacion, int id) {
        String aviso = evaluacion.visita().empresaNombre().equals(co.aplomo.servicio.PracticaEvaluacion.NOMBRE) ?
                "<div class=\"banner practica-fija\" " + "role=\"status\">MODO PRÁCTICA · No cuenta en" +
                " tus resultados</div>" : "";
        return pagina(sesion, evaluacion.puestos(), id, evaluacion.visita().visita().id(),
                evaluacion.visita().empresaNombre() + " · " + evaluacion.visita().sedeNombre() + " · " +
                        evaluacion.visita().salonNombre(), evaluacion.visita().empresa()).replace(
                "<form data-enfoque-lugar", aviso + "<form data-enfoque-lugar");
    }

    private static String pagina(Sesion sesion, List<Puesto> puestos, int id, int visita, String lugar, int empresa) {
        var elemento = puestos.stream().filter(elemento2 -> elemento2.id() == id).findFirst().orElse(null);
        if (id != 0 && elemento == null)
            throw new IllegalArgumentException("Elige un puesto activo del salón de esta visita.");
        StringBuilder contenido = new StringBuilder(
                "<div class=\"pagehead\"><div><span class=\"eyebrow\">Evaluaciones · Nueva</s" +
                        "pan><h1>Nueva evaluación</h1><p class=\"hint\">").append(escapar(lugar))
                .append("</p></div><span class=\"tag\" data-modo>")
                .append(elemento == null ? "" : elemento.tipo().equals("ESCRITORIO") ? "Oficina" : "Planta")
                .append("</span></div><form data-evaluacion data-revision=\"si\" data-conversacion=\"" +
                        "si\" data-tour=\"campo\" data-fases=\"5\" action=\"/evaluaciones\" method=\"" +
                        "post\" enctype=\"multipart/form-data\" novalidate>").append(Html.csrf(sesion));
        int inicioFormulario = contenido.indexOf("<form data-evaluacion");
        contenido.insert(inicioFormulario + "<form".length(),
                " data-enfoque-lugar=\"" + escapar(lugar) + "\" data-enfoque-visita=\"" + visita +
                        "\" data-enfoque-empresa=\"" + empresa + "\"");
        oculto(contenido, "visita", "" + visita);
        oculto(contenido, "recorrido", "campo");
        for (String texto2 : List.of("postura", "motivo", "criterio", "minutos", "condiciones_respondidas"))
            oculto(contenido, texto2, "");
        if (elemento != null)
            contenido.append("<input type=\"hidden\" name=\"puesto\" value=\"").append(id).append("\" data-tarea=\"")
                    .append(escapar(elemento.descripcion())).append("\" data-contexto=\"")
                    .append(elemento.tipo().isBlank() ? "" : elemento.tipo()
                            .equals("ESCRITORIO") ? "OFICINA" : "PLANTA").append("\">");
        else {
            contenido.append("<label>Puesto de trabajo<select class=\"input\" name=\"puesto\"><option valu" +
                    "e=\"\">Elige un puesto</option>");
            for (var elemento3 : puestos)
                contenido.append("<option value=\"").append(elemento3.id()).append("\" data-tarea=\"")
                        .append(escapar(elemento3.descripcion())).append("\" data-contexto=\"")
                        .append(elemento3.tipo().equals("ESCRITORIO") ? "OFICINA" : "PLANTA").append("\">")
                        .append(escapar(elemento3.descripcion())).append("</option>");
            contenido.append("</select></label>");
        }
        contenido.append("<header class=\"campo-cabecera\"><h2 data-titulo-fijo>Bienvenida y consentim" +
                "iento</h2><div class=\"row between\"><button class=\"btn\" type=\"button\" d" +
                "ata-atras-evaluacion>← Atrás</button><span id=\"pasoActual\" role=\"status\"" +
                ">Paso 1 de 5</span><strong data-lado-titulo></strong></div><ol class=\"campo" +
                "-progreso\" aria-label=\"Pasos de la evaluación\">");
        String[] nombres = {"Permiso", "Puesto", "Postura 1", "Postura 2", "Resultado"};
        for (int i = 0; i < 5; i++)
            contenido.append("<li data-etapa=\"").append(i + 1).append("\">").append(nombres[i]).append("</li>");
        contenido.append("</ol><progress max=\"5\" value=\"1\" aria-label=\"Avance de la evaluación\">" +
                "</progress></header>");
        abrir(contenido, 1, "Consentimiento");
        contenido.append(FotosGuiadas.consentimiento()).append(FotosGuiadas.permiso(true));
        contenido.append("</section>");
        abrir(contenido, 2, "Datos del puesto");
        String[][] tiempos = {{"MENOS_2", "Menos de 2 horas"}, {"DE_2_A_4", "De 2 a 4 horas"}, {"DE_4_A_6",
                "Más de 4" + " y hasta 6"}, {"DE_6_A_8", "Más de 6 y hasta 8"}, {"MAS_8", "Más de 8 horas"}};
        for (var texto3 : tiempos) texto3[0] = "" + MapeosCampo.MINUTOS.get(texto3[0]);
        pregunta(contenido, "tiempo_campo", "Pregúntale", "¿Cuántas horas al día trabaja en este puesto?", tiempos);
        contenido.append("<h3>Condiciones del puesto</h3><div class=\"campo-condiciones\">");
        for (var texto4 : PaginasEvaluacion.CONDICIONES)
            contenido.append(PaginasEvaluacion.opcion(texto4[0], "si", texto4[1], "", true, "", ""));
        contenido.append(PaginasEvaluacion.opcion("ninguna", "si", "Ninguna", "", true, "", "")).append("</div>")
                .append(Formularios.texto(Map.of(), "observaciones", "Otras observaciones (opcional)", 255, false))
                .append("</section>");
        abrir(contenido, 3, "Foto y ángulos");
        contenido.append("<div class=\"campo-dos-columnas\"><div class=\"campo-imagen\"><div data-mues" + "tra-perfil>")
                .append(muestra("perfil", ""))
                .append("</div><div class=\"stack\" data-foto-revision hidden></div><div class=\"row" +
                        "\" data-foto-controles hidden><button type=\"button\" class=\"btn\" data-cam" +
                        "biar-foto>Cambiar foto</button><button type=\"button\" class=\"btn\" data-qu" +
                        "itar-foto>Quitar foto</button><button type=\"button\" class=\"btn\" data-cal" +
                        "cular-manual>Calcular manualmente</button></div></div><aside class=\"guia-ma" +
                        "nual\" data-guia-manual hidden><h3 data-manual-instruccion role=\"status\" a" +
                        "ria-atomic=\"true\">Paso 1 de 5: toca el hombro</h3><span data-manual-contad" +
                        "or>1 de 5</span><div class=\"row\"><button type=\"button\" class=\"btn\" dat" +
                        "a-manual-anterior>Anterior</button><button type=\"button\" class=\"btn\" dat" +
                        "a-manual-reiniciar>Reiniciar</button></div></aside><div class=\"campo-panel" +
                        "\"><p class=\"hint\" role=\"status\" data-lado-ajustado hidden></p><div data" +
                        "-mismo-lado hidden><p role=\"status\" data-mismo-lado-texto></p><button type" +
                        "=\"button\" class=\"btn\" data-solo-este-brazo>Seguir solo con este brazo</b" +
                        "utton></div><div data-subir-perfil><p>Fotografía a la persona de lado, con e" +
                        "l cuerpo completo, en su postura real y con el brazo a la vista. Pon la cáma" +
                        "ra a la altura de la cadera.</p><p data-instruccion-postura>Pídele al trabaj" +
                        "ador que se ponga en la postura en la que se siente más incómodo y tómale la" +
                        " foto.</p><button class=\"btn p\" type=\"button\" data-perfil>Subir foto</bu" + "tton>" +
                        GaleriaDemo.selector("PERFIL") +
                        "<input type=\"file\" hidden accept=\"image/jpeg,image/png\" data-archivo-per" +
                        "fil></div><div class=\"campo-compacta\" data-medidas-compactas hidden>");
        for (String k : List.of("BRAZO", "ANTEBRAZO", "CUELLO", "TRONCO")) {
            String img = RecorridoCuerpo.RANGOS.get(Segmento.valueOf(k)).get(0).imagen();
            contenido.append("<div class=\"campo-medida\" data-medida=\"").append(k)
                    .append("\"><img class=\"campo-silueta\" src=\"/static/img/tour/").append(img)
                    .append("\" alt=\"\"><div><span data-angulo=\"").append(k)
                    .append("\">Sin medir</span><span class=\"tag\" data-revisar hidden>Revisar</span>");
            String[][] opciones = RecorridoCuerpo.RANGOS.get(Segmento.valueOf(k)).stream()
                    .map(elemento4 -> new String[]{"" + elemento4.grados(), elemento4.etiqueta(), elemento4.imagen()})
                    .toArray(String[][]::new);
            pregunta(contenido, "rango_" + k, "", nombre(k), opciones);
            contenido.append("</div></div>");
        }
        contenido.append("<div class=\"campo-ajustes\"><span class=\"eyebrow\">Míralo</span><h3>Marca " +
                "lo que veas</h3><div class=\"campo-chips\">");
        for (var texto5 : new String[][]{{"hombroElevado", "Hombro subido en el brazo que hace el esfuerzo"}, {
            "brazoSeparado", "Brazo que hace el esfuerzo separado del cuerpo"}, {"brazoApoyado",
                "Brazo que hace el " + "esfuerzo apoyado"}, {"antebrazoCruza",
                "Antebrazo del brazo que hace el esfuerzo cruza el cuerpo o " +
                        "sale hacia un lado"}, {"cuelloGirado", "Cabeza torcida"}, {"cuelloInclinado",
                "Cabeza inclinada " + "hacia un lado"}, {"troncoGirado", "Espalda torcida"}, {"troncoInclinado",
                "Espalda inclinada hacia " + "un lado"}, {"pies_sin_apoyo", "Pies sin buen apoyo"}})
            chip(contenido, texto5[0], texto5[1]);
        contenido.append("</div>");
        contenido.append("<p>Si no marcas nada, se cuenta como que no hay ajuste.</p>");
        contenido.append("<fieldset class=\"campo-muneca\"><legend>Muñeca</legend><input type=\"hidden" +
                "\" name=\"muneca_campo\"><div class=\"campo-chips\">");
        for (var texto6 : new String[][]{{"RECTA", "Recta", "muneca_neutra.png"},
                {"POCO", "Un poco doblada", "muneca_15.png"}, {"MUCHO", "Muy doblada", "muneca_mas_15.png"}})
            contenido.append("<button type=\"button\" class=\"campo-chip campo-opcion muneca-rango\" data-muneca=\"")
                    .append(MapeosCampo.MUNECA.get(texto6[0])).append("\" aria-pressed=\"false\"><img src=\"")
                    .append("/static/img/tour/").append(texto6[2]).append("\" alt=\"\"><span>").append(texto6[1])
                    .append("</span></button>");
        chip(contenido, "munecaDesviada", "Torcida hacia un lado");
        chip(contenido, "giro_extremo", "Giro hasta el límite");
        contenido.append("</div></fieldset></div><h3>Esfuerzo y carga</h3>");
        pregunta(contenido, "carga_campo", "", "Carga",
                new String[][]{{"CELULAR", "No aplica"}, {"GALON", "De 2 a 10 kg"}, {"BULTO", "Más de 10 kg"}});
        pregunta(contenido, "actividad_campo", "", "Esfuerzo muscular",
                new String[][]{{"NINGUNA", "No aplica"}, {"ESTATICA", "Mantiene la postura más de 1 minuto"}, {
                    "REPETITIVA", "Repite el movimiento más de 4 veces por minuto"}});
        pregunta(contenido, "tirones_campo", "", "Golpes o tirones",
                new String[][]{{"no", "No aplica"}, {"si", "Sí hay golpes o tirones"}});
        contenido.append("</div></div></div></div>").append(apoyo()).append("<div data-mapeos hidden>");
        for (String peso : List.of("CELULAR", "GALON", "BULTO"))
            for (String act : List.of("NINGUNA", "ESTATICA", "REPETITIVA"))
                for (boolean golpe : new boolean[]{false, true})
                    contenido.append("<i data-peso=\"").append(peso).append("\" data-actividad=\"").append(act)
                            .append("\" data-golpe=\"").append(golpe ? "si" : "no").append("\" data-carga=\"")
                            .append(MapeosCampo.carga(peso, act, golpe).name()).append("\"></i>");
        for (String parte : List.of("cuello", "tronco"))
            for (String tipo : List.of("GIRADA", "INCLINADA", "AMBAS")) {
                var valores = MapeosCampo.orientacion("si", tipo);
                contenido.append("<i data-orientacion=\"").append(parte).append("\" data-tipo=\"").append(tipo)
                        .append("\" data-girada=\"").append(valores[0]).append("\" data-inclinada=\"")
                        .append(valores[1]).append("\"></i>");
            }
        contenido.append("</div></section>");
        abrir(contenido, 5, "Resultado");
        contenido.append("<h3 data-resumen-titulo hidden>Resumen del puesto</h3><div data-resultados><" +
                "/div></section><div class=\"aviso\" role=\"alert\" data-error-evaluacion hid" +
                "den><h3 data-error-titulo>Revisa los datos</h3><span data-error-texto></span" +
                "><div class=\"row\" data-error-foto-acciones hidden><button type=\"button\" " +
                "class=\"btn\" data-foto-reemplazar>Subir otra</button><button type=\"button" +
                "\" class=\"btn\" data-foto-manual>Marcar a mano</button><button type=\"butto" +
                "n\" class=\"btn\" data-foto-usar>Usar esta de todos modos</button></div><but" +
                "ton type=\"button\" class=\"link\" data-cerrar-error>Cerrar</button></div><d" +
                "iv class=\"wizfoot\"><p class=\"hint\" role=\"status\" data-falta></p><span " +
                "class=\"seguir-zona\" data-seguir-zona><button type=\"button\" class=\"btn p" +
                "\" data-siguiente disabled>Seguir →</button></span><button type=\"submit\" c" +
                "lass=\"btn p\" data-guardar-volver hidden>Guardar y terminar</button><button" +
                " type=\"submit\" class=\"link\" data-agregar-postura hidden>Agregar otra pos" +
                "tura</button></div><div data-editor-compatibilidad hidden>");
        String editor = PaginasAnalisis.pasos();
        contenido.append(editor.substring(0, editor.indexOf("<section class=\"card stack\" data-paso=\"6\""))
                .replace("data-paso=", "data-editor-paso=")).append("</div></form>").append(PaginasAnalisis.scripts());
        return Html.pagina("Nueva evaluación",
                PaginasEvaluacion.fases(contenido.toString()).replace("“Permiso de fotos”", "Permiso de fotos"), sesion,
                false, false);
    }

    private static void oculto(StringBuilder contenido, String texto2, String texto3) {
        contenido.append("<input type=\"hidden\" name=\"").append(texto2).append("\" value=\"").append(escapar(texto3))
                .append("\">");
    }

    private static void abrir(StringBuilder contenido, int cantidad, String texto2) {
        contenido.append("<section class=\"card stack conversacion-pantalla\" data-paso=\"").append(cantidad)
                .append("\"").append(cantidad == 1 ? "" : " hidden").append("><div class=\"campo-titulo\"><h2>")
                .append(texto2).append("</h2></div>");
        if (cantidad == 3) contenido.append(
                "<div class=\"campo-postura-aviso\" role=\"status\" data-postura-aviso></div>" +
                        "<div class=\"campo-chips\" role=\"group\" aria-label=\"Brazo que hace el esf" +
                        "uerzo\"><span>Brazo que hace el esfuerzo:</span><button type=\"button\" clas" +
                        "s=\"campo-chip\" data-lado-campo=\"AUTO\" aria-pressed=\"true\">Automático</" +
                        "button><button type=\"button\" class=\"campo-chip\" data-lado-campo=\"DERECH" +
                        "O\" aria-pressed=\"false\">Derecho</button><button type=\"button\" class=\"c" +
                        "ampo-chip\" data-lado-campo=\"IZQUIERDO\" aria-pressed=\"false\">Izquierdo</" +
                        "button><button type=\"button\" class=\"campo-chip\" data-lado-campo=\"AMBOS" +
                        "\" aria-pressed=\"false\">Ambos</button></div><p role=\"status\" data-perfil" +
                        "-otro hidden>Falta la foto del otro brazo: sube un perfil del lado que aún n" +
                        "o subiste, de esta misma postura.</p>");
    }

    private static void chip(StringBuilder contenido, String nombre, String texto) {
        contenido.append("<label class=\"campo-chip\"><input type=\"checkbox\" data-chip=\"").append(nombre)
                .append("\"");
        if (nombre.equals("pies_sin_apoyo")) contenido.append(" aria-describedby=\"ayuda-apoyo-pies\"");
        contenido.append("><span>").append(texto).append("</span>");
        if (nombre.equals("pies_sin_apoyo")) contenido.append(
                "<small class=\"tag\" data-segun-foto-pies aria-hidden=\"true\" hidden>Según " + "la foto</small>");
        contenido.append("</label>");
        if (nombre.equals("pies_sin_apoyo")) contenido.append(
                "<p class=\"hint\" id=\"ayuda-apoyo-pies\" data-ayuda-pies role=\"status\" hi" +
                        "dden>Parece que está sobre una sola pierna. Revisa si los pies están bien ap" + "oyados.</p>");
    }

    private static void pregunta(StringBuilder contenido, String texto2, String voz, String texto3,
            String[][] opciones) {
        String defecto = Set.of("carga_campo", "actividad_campo", "tirones_campo")
                .contains(texto2) ? opciones[0][0] : "";
        contenido.append("<div class=\"campo-pregunta\" data-pregunta=\"").append(texto2).append("\">");
        if (!voz.isEmpty()) contenido.append("<span class=\"eyebrow\">").append(voz).append("</span>");
        contenido.append("<p id=\"pregunta-").append(texto2).append("\"><em>“").append(texto3).append("”</em></p>");
        contenido.append("<input type=\"hidden\" name=\"").append(texto2).append("\" value=\"").append(defecto)
                .append("\"><button class=\"campo-respuesta\" type=\"button\" aria-expanded=\"false\"" +
                        " aria-controls=\"opciones-").append(texto2).append("\" aria-labelledby=\"pregunta-")
                .append(texto2).append(" respuesta-").append(texto2).append("\" data-abrir=\"").append(texto2)
                .append("\"><span id=\"respuesta-").append(texto2).append("\">")
                .append(defecto.isEmpty() ? "Elige una opción" : "No aplica ✓")
                .append("</span><span aria-hidden=\"true\">⌄</span></button><div class=\"campo-lista" +
                        "\" id=\"opciones-").append(texto2).append("\" hidden>");
        for (var texto4 : opciones) {
            contenido.append("<button type=\"button\" class=\"campo-opcion\" data-respuesta=\"").append(texto2)
                    .append("\" data-valor=\"").append(escapar(texto4[0])).append("\" aria-pressed=\"")
                    .append(texto4[0].equals(defecto)).append("\">");
            if (texto4.length > 2 && !texto4[2].isBlank())
                contenido.append("<img src=\"/static/img/tour/").append(texto4[2]).append("\" alt=\"\">");
            contenido.append("<span>").append(escapar(texto4[1])).append("</span>");
            if (texto4.length > 3) contenido.append("<small>").append(escapar(texto4[3])).append("</small>");
            contenido.append("</button>");
        }
        contenido.append("</div></div>");
    }

    private static String muestra(String tipo, String leyenda) {
        return "<figure class=\"campo-muestra\"><img src=\"/static/img/muestra_" + tipo +
                ".jpg\" alt=\"Ejemplo de foto de " + tipo + "\">" +
                (leyenda.isEmpty() ? "" : "<figcaption>" + leyenda + "</figcaption>") + "</figure>";
    }

    private static String apoyo() {
        StringBuilder contenido = new StringBuilder(
                "<details class=\"campo-apoyo\"><summary class=\"btn agregar-foto\"><span ari" +
                        "a-hidden=\"true\">+</span> Agregar otra foto (opcional)</summary><div class=" +
                        "\"campo-chips\"><button type=\"button\" class=\"campo-chip\" data-apoyo-tipo" +
                        "=\"FRENTE\" aria-pressed=\"true\">Frente</button><button type=\"button\" cla" +
                        "ss=\"campo-chip\" data-apoyo-tipo=\"OTRA\" aria-pressed=\"false\">Detalle</b" +
                        "utton></div>");
        for (String vista : List.of("FRENTE", "OTRA")) {
            String tipo = vista.equals("FRENTE") ? "frente" : "detalle";
            contenido.append("<div data-apoyo-panel=\"").append(vista).append("\"")
                    .append(vista.equals("FRENTE") ? "" : " hidden").append(">").append(muestra(tipo,
                            vista.equals("FRENTE") ? "De frente, cuerpo completo. Solo respaldo." :
                                    "Mano y muñeca de cerca. Solo " + "respaldo."))
                    .append("<button type=\"button\" class=\"btn\" data-agregar=\"").append(vista)
                    .append("\">Subir foto</button>").append(GaleriaDemo.selector(vista))
                    .append("<input type=\"file\" hidden accept=\"image/jpeg,image/png\" data-archivo-extra=\"")
                    .append(vista).append("\"><div data-apoyo-vista=\"").append(vista).append("\"></div></div>");
        }
        return contenido.append("</details>").toString();
    }

    private static String nombre(String k) {
        return switch (k) {
            case "ANTEBRAZO" -> "Antebrazo";
            case "CUELLO" -> "Cuello";
            case "TRONCO" -> "Tronco";
            default -> "Brazo";
        };
    }

    static String continuar(Sesion sesion, Evaluaciones.Consulta evaluacion) {
        return continuar(sesion, evaluacion, null);
    }

    static String continuar(Sesion sesion, Evaluaciones.Consulta evaluacion, Evaluaciones.Contexto contexto) {
        var evaluacion2 = evaluacion.ficha();
        var evaluacion3 = evaluacion2.datos();
        var elemento = new Puesto(evaluacion2.puesto(), 0, null, null, evaluacion2.numero(), evaluacion3.postura(),
                evaluacion2.tipoPuesto(), "", null, true);
        String lugar = contexto == null ?
                "Evaluación existente · Puesto " + evaluacion2.numero() :
                contexto.visita().empresaNombre() + " · " + contexto.visita().sedeNombre() + " · " +
                        contexto.visita().salonNombre();
        String html = pagina(sesion, List.of(elemento), elemento.id(), evaluacion2.visita(), lugar,
                contexto == null ? 0 : contexto.visita().empresa());
        html = html.replaceAll("<ol class=\"campo-progreso\"[^>]*>.*?</ol>",
                        "<ol class=\"campo-progreso\" aria-label=\"Pasos de la evaluación\">" +
                                "<li data-etapa=\"1\">Revisar postura</li><li data-etapa=\"2\">Resultado</li></ol>")
                .replace("<progress max=\"5\"", "<progress max=\"2\"");
        html = html.replace("<span class=\"tag\" data-modo>Planta</span>", "");
        html = html.replace("<span class=\"tag\" data-modo>Oficina</span>", "");
        html = html.replace("action=\"/evaluaciones\"",
                "data-criterio=\"" + escapar(evaluacion3.criterio()) + "\" data-continuar=\"" + evaluacion2.id() +
                        "\" action=\"/evaluaciones/" + evaluacion2.id() + "/analisis\"");
        StringBuilder fotos = new StringBuilder("<div data-fotos-existentes hidden>");
        for (var foto : evaluacion.fotos())
            if (!foto.descripcion().startsWith(co.aplomo.servicio.FotosEvaluacion.REEMPLAZADA))
                fotos.append("<span data-foto-id=\"").append(foto.id()).append("\" data-vista=\"").append(foto.vista())
                        .append("\" data-descripcion=\"").append(escapar(foto.descripcion()))
                        .append("\" data-src=\"/fotos/").append(foto.id()).append("\"></span>");
        return html.replace("</form>", fotos.append("</div></form>").toString());
    }
}
