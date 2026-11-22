package co.aplomo.web;

import co.aplomo.dao.MensajeDao.Mensaje;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Agenda.Conversacion;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RF-033: panel de cita y conversación, con texto escapado y sin destinatarios editables.
 */
public final class PanelMensajesAgenda {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM '·' HH:mm");

    private PanelMensajesAgenda() {
    }

    /**
     * RF-033: presenta únicamente la cita y los mensajes previamente autorizados por el servicio.
     */
    public static String cita(Sesion sesion, Conversacion datos) {
        var ficha = datos.ficha();
        var reserva = ficha.reserva();
        Cuenta evaluador = datos.evaluador();
        String titulo = sesion.cuenta().rol() == Cuenta.Rol.ADMINISTRADOR ? "Mensaje para " + (evaluador ==
                null ? "el evaluador" : nombre(evaluador.nombre())) : "Responder al Administrador";
        boolean puede = evaluador != null && evaluador.rol() == Cuenta.Rol.EVALUADOR &&
                evaluador.estado() == Cuenta.Estado.ACTIVA;
        String ruta = "/agenda/citas/" + reserva.id();
        String accion = sesion.cuenta().rol() == Cuenta.Rol.ADMINISTRADOR ?
                "/reservas/" + reserva.id() + "/nota-administrador" : ruta + "/responder";
        String contenido =
                "<section class=\"stack\" data-agenda-csrf=\"" + Html.escapar(sesion.csrf()) + "\" data-agenda-url=\"" +
                        ruta + "\"><div>" + "<p class=\"small muted\">" + Html.fecha(reserva.fecha()) + " · " +
                        reserva.inicio() + "–" + reserva.fin() + "</p><h2>" + Html.escapar(ficha.empresaNombre()) +
                        "</h2>" + "<p class=\"muted\">" +
                        Html.escapar(ficha.sedeNombre() + " · " + ficha.salonNombre()) +
                        "</p><p class=\"small muted\">Área: " +
                        Html.escapar(datos.areas().isBlank() ? "Sin área registrada" : datos.areas()) + "</p>" +
                        SistemaVisual.estado(reserva.estado()) + "</div>" +
                        (evaluador == null ? "<p class=\"muted\">Sin evaluador asignado</p>" : TextoLista.persona(
                                evaluador.nombre(), evaluador.colorAgenda())) + "<a class=\"btn\" href=\"/reservas/" +
                        reserva.id() + "\">Ver reserva completa</a>" + "<h3>Mensajes de esta cita</h3>" +
                        mensajes(sesion, datos.mensajes()) + (puede ? formulario(sesion, accion,
                        titulo) : "<p class=\"hint\">Asigna un evaluador activo para enviar mensajes.</p>") +
                        "</section>";
        return contenido;
    }

    /**
     * RF-033: reutiliza el panel para un aviso general al Evaluador validado, sin id_reserva.
     */
    public static String general(Sesion sesion, Cuenta evaluador, List<Mensaje> mensajes) {
        String titulo = "Mensaje para " + nombre(evaluador.nombre());
        return "<section class=\"stack\" data-agenda-csrf=\"" + Html.escapar(sesion.csrf()) +
                "\" data-agenda-url=\"/agenda/evaluadores/" + evaluador.id() + "\">" + "<h2>" + Html.escapar(titulo) +
                "</h2>" + TextoLista.persona(evaluador.nombre(), evaluador.colorAgenda()) +
                "<p class=\"muted\">Mensaje general · Sin cita</p>" + mensajes(sesion, mensajes) +
                formulario(sesion, "/agenda/evaluadores/" + evaluador.id(), titulo) + "</section>";
    }

    private static String mensajes(Sesion sesion, List<Mensaje> mensajes) {
        StringBuilder html = new StringBuilder("<ol class=\"cita-mensajes\" data-cita-mensajes " +
                "aria-label=\"Conversación\" aria-live=\"polite\" aria-relevant=\"additions\">");
        if (mensajes.isEmpty()) html.append("<li class=\"muted\">Aún no hay mensajes.</li>");
        for (Mensaje mensaje : mensajes) {
            html.append("<li class=\"cita-burbuja ").append(mensaje.remitente() == sesion.cuenta().id() ? "mia" : "")
                    .append("\" data-cita-mensaje=\"").append(mensaje.id()).append("\"><div class=\"small muted\">")
                    .append(Html.escapar(nombre(mensaje.nombre()))).append(" · ").append(mensaje.creado().format(FECHA))
                    .append("</div><p>").append(Html.escapar(mensaje.texto())).append("</p></li>");
        }
        return html.append("</ol>").toString();
    }

    private static String formulario(Sesion sesion, String accion, String titulo) {
        String campo = accion.endsWith("nota-administrador") ? "nota" : "texto";
        return "<form class=\"stack\" method=\"post\" action=\"" + accion + "\" data-agenda-enviar>" +
                Html.csrf(sesion) + "<label class=\"field\">" + Html.escapar(titulo) +
                "<textarea class=\"input\" name=\"" + campo + "\" maxlength=\"500\" rows=\"3\" required " +
                "aria-describedby=\"agenda-contador\" data-agenda-texto></textarea></label>" +
                (sesion.cuenta().rol() == Cuenta.Rol.ADMINISTRADOR ?
                        "<div class=\"chips\">" + chip("Llevar EPP: ") + chip("Cambio de hora: ") +
                                chip("Llama al contacto antes de ir") + "</div>" : "") +
                "<small class=\"muted\" id=\"agenda-contador\" data-agenda-contador>0 / 500</small>" +
                "<p class=\"hint\" role=\"alert\" data-agenda-error></p>" +
                "<button class=\"btn p\" type=\"submit\">Enviar</button></form>";
    }

    private static String chip(String texto) {
        return "<button class=\"chip\" type=\"button\" data-agenda-plantilla=\"" + Html.escapar(texto) + "\">" +
                Html.escapar(texto) + "</button>";
    }

    private static String nombre(String nombre) {
        return Html.nombreSinTitulo(nombre).split("\\s+")[0];
    }
}
