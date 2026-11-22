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
        if (ruta.equals("/avisos/nuevo")) {
            boolean panel = "1".equals(intercambio.getRequestHeaders().getFirst("X-Alineo-Panel"));
            String error = "";
            if (post) {
                try {
                    Integer para = null;
                    if (sesion.cuenta().rol() != co.aplomo.modelo.Cuenta.Rol.USUARIO) {
                        try {
                            para = Integer.valueOf(datos.getOrDefault("para", ""));
                            if (para < 1) throw new NumberFormatException();
                        } catch (NumberFormatException fallo) {
                            throw new SecurityException("El destinatario no está disponible.");
                        }
                    }
                    var destino = servicio.enviarNuevo(sesion.cuenta(), para, datos.get("texto"));
                    String aviso = "Mensaje enviado a " + destino.nombre();
                    if (panel) {
                        Router.enviar(intercambio, 200,
                                Html.aviso(aviso, true) + panel(sesion, servicio.listar(sesion.cuenta()), false));
                    } else Router.redirigir(intercambio, "/avisos?aviso=" +
                            java.net.URLEncoder.encode(aviso, java.nio.charset.StandardCharsets.UTF_8));
                    return true;
                } catch (IllegalArgumentException fallo) {
                    error = fallo.getMessage();
                }
            }
            String contenido = nuevo(sesion, servicio.destinatarios(sesion.cuenta()), datos, error, panel);
            Router.enviar(intercambio, error.isEmpty() ? 200 : 400,
                    panel ? contenido : Html.pagina("Nuevo mensaje", contenido, sesion));
            return true;
        }
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
            if (ruta.endsWith("entendido") && "1".equals(intercambio.getRequestHeaders().getFirst("X-Alineo-Panel"))) {
                long nuevos = servicio.listar(sesion.cuenta()).stream().filter(m -> !m.leido()).count();
                Router.responder(intercambio, 200, "application/json; charset=UTF-8",
                        "{\"ok\":true,\"nuevos\":" + nuevos + "}");
            } else Router.redirigir(intercambio, "/avisos?enviado=1");
            return true;
        }
        if (ruta.equals("/avisos") && !post) {
            List<Mensaje> mensajes = servicio.listar(sesion.cuenta());
            boolean leidos = Router.consulta(intercambio).getOrDefault("leidos", "0").equals("1");
            String html = Html.aviso(Router.consulta(intercambio).getOrDefault("aviso", ""), true) +
                    panel(sesion, mensajes, leidos);
            if (!"1".equals(intercambio.getRequestHeaders().getFirst("X-Alineo-Panel"))) {
                html = Html.pagina("Avisos", html, sesion, "Cuenta · Avisos", "");
            }
            Router.enviar(intercambio, 200, html);
            return true;
        }
        Router.enviar(intercambio, 405, "Método no permitido.");
        return true;
    }

    private static String nuevo(Sesion sesion, List<Avisos.Destinatario> destinos, Map<String, String> datos,
            String error, boolean panel) {
        StringBuilder html = new StringBuilder("<div class=\"row between\"><h2>Nuevo mensaje</h2>");
        if (panel) html.append("<button class=\"btn\" type=\"button\" data-cerrar-avisos>Cerrar</button>");
        html.append("</div>").append(Html.aviso(error, false))
                .append("<form class=\"stack nuevo-aviso\" method=\"post\" action=\"/avisos/nuevo\" data-nuevo-aviso>")
                .append(Html.csrf(sesion));
        if (sesion.cuenta().rol() == co.aplomo.modelo.Cuenta.Rol.USUARIO) {
            var contacto = co.aplomo.config.Contacto.leer(
                    java.nio.file.Path.of(System.getProperty("aplomo.contacto.archivo", "config/contacto.properties")));
            html.append("<p>Para: ").append(Html.escapar(contacto.nombre())).append(" · Administradora principal</p>");
        } else {
            String defecto = destinos.stream().filter(Avisos.Destinatario::principal).map(d -> Integer.toString(d.id()))
                    .findFirst().orElse("");
            String elegida = datos.getOrDefault("para", defecto);
            html.append("<label class=\"field\">Para<select class=\"input\" name=\"para\" required>");
            if (sesion.cuenta().rol() == co.aplomo.modelo.Cuenta.Rol.ADMINISTRADOR)
                html.append("<option value=\"\">Elige un evaluador</option>");
            for (var destino : destinos) {
                html.append("<option value=\"").append(destino.id()).append("\"")
                        .append(elegida.equals(Integer.toString(destino.id())) ? " selected" : "").append(">")
                        .append(Html.escapar(destino.nombre()))
                        .append(destino.principal() ? " (Administradora principal)" : "").append("</option>");
            }
            html.append("</select></label>");
            if (destinos.isEmpty()) html.append("<p class=\"hint\">No hay evaluadores activos para escribirles.</p>");
        }
        String texto = datos.getOrDefault("texto", "");
        String contador = panel ? "nuevo-aviso-contador-panel" : "nuevo-aviso-contador";
        return html.append("<label class=\"field\">Mensaje<textarea class=\"input\" name=\"texto\" maxlength=\"500\" ")
                .append("rows=\"4\" required data-nuevo-aviso-texto aria-describedby=\"").append(contador).append("\">")
                .append(Html.escapar(texto)).append("</textarea></label>").append("<small id=\"").append(contador)
                .append("\" class=\"muted\" data-nuevo-aviso-contador>").append(texto.length())
                .append(" / 500</small><p class=\"hint\" role=\"alert\" data-nuevo-aviso-error></p>")
                .append("<button class=\"btn p\" type=\"submit\"").append(destinos.isEmpty() ? " disabled" : "")
                .append(">Enviar mensaje</button></form>").toString();
    }

    private static String panel(Sesion sesion, List<Mensaje> mensajes, boolean leidos) {
        long nuevos = mensajes.stream().filter(m -> !m.leido()).count();
        StringBuilder html = new StringBuilder(
                "<div class=\"row between\" data-bandeja-avisos><h2>Avisos</h2><div class=\"row\">" +
                        "<a class=\"btn p\" data-lista-avisos href=\"/avisos/nuevo\">Nuevo mensaje</a>" +
                        "<button type=\"button\" class=\"btn\" data-cerrar-avisos aria-label=\"Cerrar " +
                        "avisos\">Cerrar</button>" +
                        "</div></div><nav class=\"chips\" aria-label=\"Mensajes\"><a class=\"chip\" data-lista-avisos" +
                        " " +
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
        html.append("<h3><button type=\"button\" class=\"aviso-ver\" data-ver-mensaje=\"").append(m.id()).append("\">")
                .append(titulo(m.tipo())).append("</button></h3><p class=\"small muted\">")
                .append(m.tipo().equals("SOLICITUD_ACCESO") ? "" : "De ")
                .append(m.tipo().equals("SOLICITUD_ACCESO") ? "Solicitud recibida" : Html.escapar(m.nombre()))
                .append(" · ")
                .append(m.tipo().equals("SOLICITUD_ACCESO") ? "Acceso por revisar" : Formularios.etiqueta(m.rol()))
                .append(" · ").append(edad(m)).append("</p><p data-texto-mensaje>").append(Html.escapar(m.texto()))
                .append("</p>");
        if (m.solicitudAcceso() != null) {
            html.append("<a class=\"btn\" href=\"/solicitudes-acceso/").append(m.solicitudAcceso())
                    .append("\">Revisar solicitud</a>");
        }
        if (m.reserva() != null) {
            String destino = (m.tipo().equals("SOLICITUD_VISITA") ||
                    m.tipo().equals("RESPUESTA") && m.rol().equals("USUARIO")) ? "/solicitudes-visita/" : "/reservas/";
            html.append("<a class=\"aviso-cita\" href=\"").append(destino).append(m.reserva()).append("\">")
                    .append(Html.escapar(m.cita())).append("</a>");
        }
        boolean equipo =
                sesion.cuenta().rol() == co.aplomo.modelo.Cuenta.Rol.EVALUADOR && m.rol().equals("ADMINISTRADOR") &&
                        (m.tipo().equals("AVISO") || m.tipo().equals("NOTA_ADMIN")) ||
                        sesion.cuenta().rol() == co.aplomo.modelo.Cuenta.Rol.ADMINISTRADOR &&
                                m.rol().equals("EVALUADOR") &&
                                (m.tipo().equals("RESPUESTA") || m.tipo().equals("AVISO") && m.reserva() == null);
        if (equipo || m.tipo().equals("PEDIR_INFORMACION") && !m.respondido()) {
            html.append("<details class=\"aviso-responder\"><summary class=\"btn\">Responder</summary>" +
                            "<form method=\"post\" action=\"/avisos/").append(m.id()).append("/responder\">")
                    .append(Html.csrf(sesion)).append("<label class=\"field\">Tu respuesta<textarea class=\"input\" " +
                            "name=\"texto\" maxlength=\"500\" required></textarea></label>" +
                            "<button class=\"btn p\">Enviar respuesta</button></form></details>");
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
