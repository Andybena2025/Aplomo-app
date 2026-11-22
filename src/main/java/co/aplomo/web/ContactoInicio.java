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
            boolean correoValido = correo != null &&
                    correo.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
            if (correoValido) html.append("<p class=\"contacto-dato\">").append(Html.escapar(correo)).append("</p>");
            if (!numero.isEmpty()) html.append("<p>").append(Html.escapar(telefono)).append("</p>");
            String wa = numero.isEmpty() ? "" : persona.principal() ? numero(principal.whatsapp()) : numero;
            Contacto ficha = new Contacto(nombre, cargo, correoValido ? correo : "",
                    numero.isEmpty() ? "" : telefono, wa, false);
            html.append(ContactoAyuda.botones(ficha, persona.id())).append("</div></div>");
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
