package co.aplomo.web;

/** Paso 13F-3: un solo recorrido de tomas, reutilizado para el otro lado. */
final class FotosGuiadas {
    private FotosGuiadas() {}
    static String pantallas() {
        StringBuilder h=new StringBuilder("<section class=\"card stack tour-pregunta fotos-guiadas\" data-paso=\"6\" data-fotos-guiadas hidden><div class=\"stack\" data-foto-pantalla=\"permiso\"><h2>¿La persona permite que le tomes fotos?</h2><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Registro fotográfico\">");
        h.append(PaginasEvaluacion.opcion("registro","CON_FOTOS","Sí, permite fotos","",false,"","Tomas de 1 a 10 fotos. Con ellas se miden los ángulos y se calcula el puntaje."));
        h.append(PaginasEvaluacion.opcion("registro","SIN_REGISTRO_FOTOGRAFICO","No permite fotos","",false,"","Se guarda la evaluación sin fotos y sin puntaje RULA."));
        h.append("</div></div>");
        for(String[] toma:new String[][]{{"perfil","Foto de perfil"},{"frente","Foto de frente"},{"detalle","Detalle de mano y muñeca"}}) {
            h.append("<div class=\"stack foto-pregunta\" data-foto-pantalla=\"").append(toma[0]).append("\" hidden><h2>").append(toma[1]).append("</h2><figure class=\"foto-muestra\"><img src=\"/static/img/muestra_").append(toma[0]).append(".jpg\" alt=\"Muestra de ").append(toma[1].toLowerCase(java.util.Locale.ROOT)).append("\"><figcaption>Muestra</figcaption></figure><p class=\"hint\" data-instruccion-foto></p><div class=\"row foto-acciones\"><button type=\"button\" class=\"btn\" data-foto-tomar>Tomar foto</button><button type=\"button\" class=\"btn p\" data-foto-subir>Subir foto</button><button type=\"button\" class=\"btn\" data-foto-cambiar hidden>Cambiar foto</button><button type=\"button\" class=\"btn\" data-foto-quitar hidden>Quitar</button><button type=\"button\" class=\"btn\" data-foto-omitir").append(toma[0].equals("perfil")?" hidden":"").append(">Omitir esta foto</button></div><input type=\"file\" accept=\"image/jpeg,image/png\" capture=\"environment\" data-foto-archivo=\"camara\" hidden><input type=\"file\" accept=\"image/jpeg,image/png\" data-foto-archivo=\"archivo\" hidden><p class=\"hint\">JPG o PNG · máximo 5 MB por foto</p></div>");
        }
        return h.append("<div class=\"stack\" data-foto-pantalla=\"resumen\" hidden><h2>Fotos de la evaluación</h2><p class=\"hint\" data-cuenta-fotos></p><div class=\"fotos-miniaturas\" data-fotos-elegidas></div><div class=\"row\"><button type=\"button\" class=\"btn\" data-foto-agregar>Agregar otra foto</button><button type=\"button\" class=\"btn\" data-foto-otro-lado>+ Otro lado del cuerpo</button></div><p class=\"hint\">Puedes registrar también el otro lado.</p><input type=\"file\" accept=\"image/jpeg,image/png\" multiple data-foto-extra hidden></div></section>").toString();
    }
}
