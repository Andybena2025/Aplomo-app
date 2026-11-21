package co.aplomo.web;

import co.aplomo.dao.MensajeDao.Mensaje;
import co.aplomo.servicio.Avisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * RF-002/033 (propuesta): panel de mensajes privado y acuse con CSRF.
 */
public final class AvisosWeb {
    private final Avisos servicio;

    /**
     * RNF-011: utiliza el servicio transaccional de la aplicación.
     */
    public AvisosWeb(Avisos servicio) {
        this.servicio = servicio;
    }

    /**
     * RF-002: sondeo mínimo, panel y respuesta solo para el destinatario vigente.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> datos) throws SQLException, IOException {
        String ruta = intercambio.getRequestURI().getPath();
        if (!ruta.equals("/avisos") && !ruta.startsWith("/avisos/")) return false;
        if (sesion.cuenta().temporal()) throw new SecurityException("Cambia tu contraseña para continuar.");
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/avisos/nuevos") && !post) {
            String json = servicio.listar(sesion.cuenta()).stream().filter(m -> !m.leido())
                    .map(m -> "{\"id\":" + m.id() + ",\"tipo\":" + NotasWeb.texto(titulo(m.tipo())) + ",\"texto\":" +
                            NotasWeb.texto(m.texto()) + "}")
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
            Router.responder(intercambio, 200, "application/json; charset=UTF-8", json);
            return true;
        }
        if (ruta.matches("/avisos/[1-9][0-9]*/(entendido|responder)") && post) {
            int id = Integer.parseInt(ruta.split("/")[2]);
            if (ruta.endsWith("entendido")) servicio.entender(sesion.cuenta(), id);
            else servicio.responder(sesion.cuenta(), id, datos.get("texto"));
            Router.redirigir(intercambio, "/avisos?enviado=1");
            return true;
        }
        if (ruta.equals("/avisos") && !post) {
            List<Mensaje> mensajes = servicio.listar(sesion.cuenta());
            boolean leidos = Router.consulta(intercambio).getOrDefault("leidos", "0").equals("1");
            String html = panel(sesion, mensajes, leidos);
            if (!"1".equals(intercambio.getRequestHeaders().getFirst("X-Alineo-Panel"))) {
                html = Html.pagina("Avisos", html, sesion, "Cuenta · Avisos", "");
            }
            Router.enviar(intercambio, 200, html);
            return true;
        }
        Router.enviar(intercambio, 405, "Método no permitido.");
        return true;
    }

    private static String panel(Sesion sesion, List<Mensaje> mensajes, boolean leidos) {
        long nuevos = mensajes.stream().filter(m -> !m.leido()).count();
        StringBuilder html = new StringBuilder("<div class=\"row between\"><h2>Avisos</h2>" +
                "<button type=\"button\" class=\"btn\" data-cerrar-avisos aria-label=\"Cerrar " +
                "avisos\">Cerrar</button>" +
                "</div><nav class=\"chips\" aria-label=\"Mensajes\"><a class=\"chip\" data-lista-avisos " +
                "href=\"/avisos\">Nuevos · " + nuevos + "</a><a class=\"chip\" data-lista-avisos " +
                "href=\"/avisos?leidos=1\">Leídos · " + (mensajes.size() - nuevos) + "</a></nav>");
        for (Mensaje mensaje : mensajes) {
            if (mensaje.leido() == leidos) html.append(tarjeta(sesion, mensaje));
        }
        if (mensajes.stream().noneMatch(m -> m.leido() == leidos)) {
            html.append("<p class=\"muted\">No hay avisos " + (leidos ? "leídos" : "nuevos") + ".</p>");
        }
        return html.toString();
    }

    private static String tarjeta(Sesion sesion, Mensaje m) {
        StringBuilder html = new StringBuilder(
                "<article class=\"aviso-tarjeta stack " + (m.leido() ? "" : "nuevo") + "\" data-mensaje=\"" + m.id() +
                        "\">");
        if (!m.leido()) html.append("<span class=\"eyebrow\">NUEVA</span>");
        html.append("<h3>").append(titulo(m.tipo())).append("</h3><p class=\"small muted\">")
                .append(m.tipo().equals("SOLICITUD_ACCESO") ? "" : "De ")
                .append(m.tipo().equals("SOLICITUD_ACCESO") ? "Solicitud recibida" : Html.escapar(m.nombre()))
                .append(" · ")
                .append(m.tipo().equals("SOLICITUD_ACCESO") ? "Acceso por revisar" : Formularios.etiqueta(m.rol()))
                .append(" · ").append(edad(m)).append("</p><p>").append(Html.escapar(m.texto())).append("</p>");
        if (m.solicitudAcceso() != null) {
            html.append("<a class=\"btn\" href=\"/solicitudes-acceso/").append(m.solicitudAcceso())
                    .append("\">Revisar solicitud</a>");
        }
        if (m.reserva() != null) {
            String destino = (m.tipo().equals("SOLICITUD_VISITA") ||
                    m.tipo().equals("RESPUESTA")) ? "/solicitudes-visita/" : "/reservas/";
            html.append("<a class=\"aviso-cita\" href=\"").append(destino).append(m.reserva()).append("\">")
                    .append(Html.escapar(m.cita())).append("</a>");
        }
        if (m.tipo().equals("PEDIR_INFORMACION") && !m.respondido()) {
            html.append("<form method=\"post\" action=\"/avisos/").append(m.id()).append("/responder\">")
                    .append(Html.csrf(sesion)).append("<label class=\"field\">Tu respuesta<textarea class=\"input\" " +
                            "name=\"texto\" maxlength=\"500\" required></textarea></label>" +
                            "<button class=\"btn p\">Enviar respuesta</button></form>");
        }
        if (!m.leido()) html.append("<form method=\"post\" action=\"/avisos/").append(m.id()).append("/entendido\">")
                .append(Html.csrf(sesion)).append("<button class=\"btn\">Entendido</button></form>");
        return html.append("</article>").toString();
    }

    private static String edad(Mensaje m) {
        long minutos = java.time.Duration.between(m.creado(),
                java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota"))).toMinutes();
        return minutos < 1 ? "ahora" : minutos < 60 ? "hace " + minutos + " min" : Html.fechaHora(m.creado());
    }

    private static String titulo(String tipo) {
        return switch (tipo) {
            case "NOTA_ADMIN" -> "Nota del Administrador";
            case "SOLICITUD_VISITA" -> "Solicitud de visita";
            case "PEDIR_INFORMACION" -> "Te pidieron información";
            case "PEDIR_LLAMADA" -> "Contacto de tu ingeniero";
            case "RESPUESTA" -> "Respuesta a tu mensaje";
            case "SOLICITUD_ACCESO" -> "Solicitud de acceso";
            default -> "Aviso";
        };
    }
}
