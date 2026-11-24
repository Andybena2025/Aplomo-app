package co.aplomo.web;

/**
 * Paso 13F-3: un solo recorrido de tomas, reutilizado para el otro lado.
 */
final class FotosGuiadas {
    private FotosGuiadas() {
    }

    static String pantallas() {
        StringBuilder contenido = new StringBuilder(
                "<section class=\"card stack tour-pregunta fotos-guiadas\" data-paso=\"6\" " +
                        "data-fotos-guiadas hidden><div class=\"stack\" data-foto-pantalla=\"permiso\">")
                .append(permiso(false)).append("</div>");
        for (String[] toma : new String[][]{{"perfil", "Foto de perfil"}, {"frente", "Foto de frente"}, {"detalle",
                "Detalle de mano y muñeca"}}) {
            contenido.append("<div class=\"stack foto-pregunta\" data-foto-pantalla=\"").append(toma[0])
                    .append("\" hidden><h2>").append(toma[1])
                    .append("</h2><figure class=\"foto-muestra\"><img src=\"/static/img/muestra_").append(toma[0])
                    .append(".jpg\" alt=\"Muestra de ").append(toma[1].toLowerCase(java.util.Locale.ROOT))
                    .append("\"><figcaption>Muestra</figcaption></figure><p class=\"hint\" data-instrucci" +
                            "on-foto></p><div class=\"row foto-acciones\"><button type=\"button\" class=" +
                            "\"btn\" data-foto-tomar>Tomar foto</button><button type=\"button\" class=\"b" +
                            "tn p\" data-foto-subir>Subir foto</button><button type=\"button\" class=\"bt" +
                            "n\" data-foto-cambiar hidden>Cambiar foto</button><button type=\"button\" cl" +
                            "ass=\"btn\" data-foto-quitar hidden>Quitar</button><button type=\"button\" c" +
                            "lass=\"btn\" data-foto-omitir").append(toma[0].equals("perfil") ? " hidden" : "")
                    .append(">Omitir esta foto</button></div><input type=\"file\" accept=\"image/jpeg,ima" +
                            "ge/png\" capture=\"environment\" data-foto-archivo=\"camara\" hidden><input " +
                            "type=\"file\" accept=\"image/jpeg,image/png\" data-foto-archivo=\"archivo\" " +
                            "hidden><p class=\"hint\">JPG o PNG · máximo 5 MB por foto</p></div>");
        }
        return contenido.append("<div class=\"stack\" data-foto-pantalla=\"resumen\" hidden><h2>Fotos de la e" +
                        "valuación</h2><p class=\"hint\" data-cuenta-fotos></p><div class=\"fotos-min" +
                        "iaturas\" data-fotos-elegidas></div><div class=\"row\"><button type=\"button" +
                        "\" class=\"btn\" data-foto-agregar>Agregar otra foto</button><button type=\"" +
                        "button\" class=\"btn\" data-foto-otro-lado>+ Otro lado del cuerpo</button></" +
                        "div><p class=\"hint\">Puedes registrar también el otro lado.</p><input type=" +
                        "\"file\" accept=\"image/jpeg,image/png\" multiple data-foto-extra hidden></d" + "iv" +
                        "></section>")
                .toString();
    }

    /** RF-016: casilla original con su ayuda dentro de una tarjeta. */
    static String consentimiento() {
        return "<div class=\"permiso-pregunta\"><h3><span class=\"pregunta-numero\">1</span>" +
                "¿La persona aceptó ser evaluada?</h3>" +
                PaginasEvaluacion.opcion("acepta", "si", "Sí, firmó el consentimiento", "", true, "required",
                        "Por escrito, antes de empezar.").replace("tour-opcion", "tour-opcion permiso-consentimiento") +
                "</div>";
    }

    /** RF-016, RNF-007: dos tarjetas; los nombres y valores conservan el contrato de fotos. */
    static String permiso(boolean conversacion) {
        StringBuilder html = new StringBuilder("<div class=\"permiso-pregunta campo-pregunta\" " +
                (conversacion ? "data-pregunta=\"registro\"" : "") + "><h3 id=\"pregunta-registro\">" +
                "<span class=\"pregunta-numero\">2</span><em>¿Permite que le tomemos fotos?</em></h3>");
        if (conversacion) html.append("<input type=\"hidden\" name=\"registro\" value=\"\">" +
                "<button type=\"button\" class=\"campo-respuesta\" data-abrir=\"registro\" hidden>" +
                "<span>Elige una opción</span></button>");
        html.append("<div class=\"permiso-fotos ").append(conversacion ? "campo-lista" : "tour-opciones")
                .append("\" id=\"opciones-registro\">");
        for (boolean conFotos : new boolean[]{true, false}) {
            String valor = conFotos ? "CON_FOTOS" : "SIN_REGISTRO_FOTOGRAFICO";
            String titulo = conFotos ? "Sí, permite fotos" : "No permite fotos";
            String ayuda = conFotos ? "Medimos sobre la foto" : "Sin puntaje RULA";
            html.append(conversacion ? "<button type=\"button\" class=\"campo-opcion permiso-opcion\" " +
                    "data-respuesta=\"registro\" data-valor=\"" + valor + "\" aria-pressed=\"false\">" :
                    "<label class=\"permiso-opcion\"><input type=\"radio\" name=\"registro\" value=\"" +
                            valor + "\">")
                    .append("<i class=\"permiso-marca\" aria-hidden=\"true\"></i><div><span class=\"permiso-titulo\">")
                    .append(titulo).append("</span><small>").append(ayuda).append("</small></div>")
                    .append(camara(conFotos)).append(conversacion ? "</button>" : "</label>");
        }
        return html.append("</div><p class=\"permiso-nota\"><span aria-hidden=\"true\">ⓘ</span> " +
                "Guardamos los datos del puesto, sin fotos ni puntaje.</p></div>").toString();
    }

    /** RF-016: iconos de línea decorativos, acompañados por texto. */
    private static String camara(boolean permitida) {
        return "<svg class=\"permiso-icono\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" " +
                "stroke-width=\"1.5\" aria-hidden=\"true\"><path d=\"M3 7h5l2-3h4l2 3h5v13H3z\"/>" +
                "<circle cx=\"12\" cy=\"13\" r=\"3.5\"/>" +
                (permitida ? "" : "<path d=\"M2 2l20 20\"/>") + "</svg>";
    }
}
