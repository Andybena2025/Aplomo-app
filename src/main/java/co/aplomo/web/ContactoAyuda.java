package co.aplomo.web;

import co.aplomo.config.Contacto;
import co.aplomo.seguridad.Sesiones.Sesion;

/**
 * RF-002/033, RNF-007: ficha compartida de contacto sin envíos automáticos ni datos de una evaluación.
 */
final class ContactoAyuda {
    private ContactoAyuda() {
    }

    static String tarjeta(Contacto contacto, Sesion sesion) {
        StringBuilder html = new StringBuilder(
                "<section class=\"ayuda-contacto card stack\"><h2>Tu Administradora</h2>");
        html.append("<strong>")
                .append(Html.escapar(contacto.nombre().isBlank() ? "Administradora principal" : contacto.nombre()))
                .append("</strong>");
        if (!contacto.cargo().isBlank())
            html.append("<p class=\"hint\">").append(Html.escapar(contacto.cargo())).append("</p>");
        if (!contacto.correo().isBlank())
            html.append("<p class=\"contacto-dato\">").append(Html.escapar(contacto.correo())).append("</p>");
        if (!contacto.telefono().isBlank()) html.append("<p>").append(Html.escapar(contacto.telefono())).append("</p>");
        html.append(botones(contacto, sesion));
        if (sesion == null || sesion.cuenta() == null || sesion.cuenta().temporal()) {
            html.append("<a class=\"btn\" href=\"/solicitar-acceso\">Solicitar acceso</a>");
        }
        if (contacto.incompleto())
            html.append("<p class=\"hint\">Si falta un dato de contacto, consulta a quien te entregó la cuenta.</p>");
        return html.append("</section>").toString();
    }

    static String botones(Contacto contacto, Sesion sesion) {
        StringBuilder html = new StringBuilder("<div class=\"row contacto-botones\">");
        if (!contacto.enlaceWhatsapp().isBlank())
            html.append("<a class=\"btn\" href=\"").append(Html.escapar(contacto.enlaceWhatsapp()))
                    .append("\" target=\"_blank\" rel=\"noopener noreferrer\">WhatsApp</a>");
        if (!contacto.enlaceCorreo().isBlank())
            html.append("<a class=\"btn\" href=\"").append(Html.escapar(contacto.enlaceCorreo()))
                    .append("\">Correo</a>");
        if (sesion != null && sesion.cuenta() != null && !sesion.cuenta().temporal())
            html.append("<a class=\"btn\" data-lista-avisos href=\"/avisos/nuevo\">Enviar mensaje en ALINEO</a>");
        return html.append("</div>").toString();
    }
}
