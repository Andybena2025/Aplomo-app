package co.aplomo.web;

import co.aplomo.config.Contacto;
import co.aplomo.servicio.Avisos.ContactoEquipo;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * RF-033/002: fichas en línea con los contactos previamente autorizados por el servidor.
 */
final class ContactoInicio {
    private ContactoInicio() {
    }

    static String tarjeta(List<ContactoEquipo> contactos) {
        Contacto principal =
                Contacto.leer(Path.of(System.getProperty("aplomo.contacto.archivo", "config/contacto.properties")));
        StringBuilder html = new StringBuilder("<section class=\"card stack usuario-ayuda\" data-ayuda-inicio>")
                .append("<h2>Nos encantaría saber de ti</h2>")
                .append("<p>Si tienes alguna pregunta o inquietud, no dudes en escribirnos.</p>");
        for (ContactoEquipo persona : contactos) {
            boolean administradora = principal.cargo().startsWith("Administradora");
            boolean evaluadora = persona.cargo().toLowerCase(Locale.ROOT).contains("evaluadora");
            String rol = persona.principal() ? administradora ? "Tu administradora" : "Tu administrador" :
                    evaluadora ? "Tu evaluadora asignada" : "Tu evaluador asignado";
            String nombre = persona.principal() ? principal.nombre() : Html.nombreSinTitulo(persona.nombre());
            String cargo = persona.principal() ? principal.cargo() : persona.cargo();
            String correo = persona.principal() ? principal.correo() : persona.correo();
            String telefono = persona.principal() ? principal.telefono() : persona.telefono();
            String numero = numero(telefono);
            String id = "contacto-inicio-" + persona.id();
            html.append("<div class=\"inicio-contacto\"><small class=\"muted\">").append(rol)
                    .append("</small><button type=\"button\" class=\"contacto-nombre\" data-contacto-inicio ")
                    .append("aria-expanded=\"false\" aria-controls=\"").append(id).append("\">");
            if (!persona.principal())
                html.append("<span class=\"persona-color\" aria-hidden=\"true\" style=\"--persona:")
                        .append(TextoLista.color(persona.color())).append("\"></span>");
            html.append(Html.escapar(nombre)).append("</button><div class=\"contacto-ficha stack\" id=\"")
                    .append(id).append("\" hidden><strong>").append(Html.escapar(nombre)).append("</strong><small>")
                    .append(Html.escapar(cargo)).append("</small>");
            if (correo != null && correo.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"))
                html.append("<a href=\"mailto:").append(Html.escapar(correo)).append("\">")
                        .append(Html.escapar(correo)).append("</a>");
            if (!numero.isEmpty()) {
                html.append("<a href=\"tel:+").append(numero).append("\">").append(Html.escapar(telefono)).append(
                        "</a>");
                String wa = persona.principal() ? numero(principal.whatsapp()) : numero;
                if (!wa.isEmpty()) html.append("<a class=\"btn\" target=\"_blank\" rel=\"noopener noreferrer\" ")
                        .append("href=\"https://wa.me/").append(wa).append("\">WhatsApp</a>");
            }
            html.append("<a class=\"btn\" href=\"/avisos/nuevo?para=").append(persona.id())
                    .append("\">Escribir</a></div></div>");
        }
        return html.append("<p class=\"small muted\">Siempre estamos aquí para ayudarte.</p>")
                .append("<a class=\"small\" href=\"/ayuda\">Centro de ayuda</a></section>").toString();
    }

    private static String numero(String telefono) {
        if (telefono == null) return "";
        String numero = telefono.replaceAll("[^0-9]", "");
        if (!numero.matches("[1-9][0-9]{6,14}")) return "";
        return numero.length() == 10 ? "57" + numero : numero;
    }
}
