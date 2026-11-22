package co.aplomo.web;

import co.aplomo.dao.SolicitudAccesoDao.Solicitud;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.LimiteSolicitudes;
import co.aplomo.servicio.SolicitudesAcceso;
import co.aplomo.servicio.Validacion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-002/003 (propuesta): acceso solicitado, aprobado y comunicado sin registro libre.
 */
public final class SolicitudesAccesoWeb {
    private final SolicitudesAcceso servicio;
    private final Sesiones sesiones;

    /**
     * RNF-011: comparte el registro de sesiones para CSRF también en el formulario público.
     */
    public SolicitudesAccesoWeb(SolicitudesAcceso servicio, Sesiones sesiones) {
        this.servicio = servicio;
        this.sesiones = sesiones;
    }

    /**
     * RNF-004: CSRF, validación y cinco envíos por conexión antes de guardar una solicitud pública.
     */
    public boolean publica(HttpExchange intercambio, Sesion sesion) throws IOException, SQLException {
        if (!intercambio.getRequestURI().getPath().equals("/solicitar-acceso")) return false;
        if (sesion == null) {
            sesion = sesiones.crear(null);
            Router.ponerCookie(intercambio, sesion.id());
        }
        String metodo = intercambio.getRequestMethod();
        Map<String, String> datos = Map.of();
        String mensaje = "";
        int codigo = 200;
        if (metodo.equals("POST")) {
            datos = Router.formulario(intercambio);
            if (!Sesiones.csrfValido(sesion, datos.get("csrf"))) {
                codigo = 403;
                mensaje = "El formulario venció. Revisa los datos y vuelve a enviarlo.";
            } else {
                try {
                    servicio.limitar(intercambio.getRemoteAddress().getAddress().getHostAddress());
                    servicio.solicitar(
                            new SolicitudesAcceso.Datos(datos.get("nombre"), datos.get("cargo"), datos.get("correo"),
                                    datos.get("telefono"), datos.get("empresa"), datos.get("nit"), datos.get("ciudad"),
                                    datos.get("sector"), datos.get("necesidad")));
                    Router.enviar(intercambio, 200, Html.pagina("Solicitud recibida",
                            Html.aviso("Recibimos tu solicitud. Te contactaremos pronto.", true) +
                                    "<a class=\"btn\" href=\"/login\">Volver al inicio de sesión</a>", null));
                    return true;
                } catch (IllegalArgumentException error) {
                    codigo = error instanceof LimiteSolicitudes.Limite ? 429 : 400;
                    mensaje = error.getMessage();
                } catch (SQLException error) {
                    codigo = 503;
                    mensaje = "No pudimos recibir la solicitud. Tus datos siguen aquí; inténtalo de nuevo.";
                }
            }
        } else if (!metodo.equals("GET")) {
            Router.enviar(intercambio, 405, "Método no permitido.");
            return true;
        }
        Router.enviar(intercambio, codigo, formulario(sesion, datos, mensaje));
        return true;
    }

    /**
     * RF-002: la Administradora revisa; Evaluador y Usuario reciben 403 también con dirección directa.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion,
                           Map<String, String> datos) throws SQLException, IOException {
        String ruta = intercambio.getRequestURI().getPath();
        if (!ruta.equals("/solicitudes-acceso") && !ruta.startsWith("/solicitudes-acceso/")) return false;
        servicio.pendientes(sesion.cuenta());
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/solicitudes-acceso") && !post) {
            StringBuilder html = new StringBuilder("<section class=\"stack\">");
            var pendientes = servicio.pendientes(sesion.cuenta());
            if (pendientes.isEmpty()) html.append(Html.vacio("No hay solicitudes de acceso pendientes.", ""));
            for (Solicitud solicitud : pendientes) {
                html.append("<a class=\"lista-fila\" href=\"/solicitudes-acceso/").append(solicitud.id())
                        .append("\"><strong>").append(Html.escapar(solicitud.empresa())).append("</strong><span>")
                        .append(Html.escapar(solicitud.nombre() + " · " + solicitud.ciudad())).append("</span>")
                        .append(SistemaVisual.estado("PENDIENTE")).append("</a>");
            }
            html.append("</section>");
            Router.enviar(intercambio, 200,
                    Html.pagina("Solicitudes de acceso", html.toString(), sesion, "Gestión · Solicitudes de acceso",
                            ""));
            return true;
        }
        if (!ruta.matches("/solicitudes-acceso/[1-9][0-9]*(/(aprobar|rechazar))?")) return false;
        int id = Integer.parseInt(ruta.split("/")[2]);
        Solicitud solicitud = servicio.consultar(sesion.cuenta(), id);
        String error = "";
        if (post) {
            try {
                if (ruta.endsWith("/aprobar")) {
                    String elegido = datos.getOrDefault("evaluador", "");
                    Integer evaluador = elegido.isBlank() ? null : Validacion.entero(elegido, "Evaluador", 1,
                            Integer.MAX_VALUE);
                    var resultado = servicio.aprobar(sesion.cuenta(), id, evaluador);
                    Router.enviar(intercambio, 200, aprobada(sesion, resultado, origen(intercambio)));
                    return true;
                }
                if (!ruta.endsWith("/rechazar")) throw new IllegalArgumentException("Elige Aprobar o Rechazar.");
                servicio.rechazar(sesion.cuenta(), id, datos.get("motivo"));
                Router.redirigir(intercambio, "/solicitudes-acceso");
                return true;
            } catch (IllegalArgumentException fallo) {
                error = fallo.getMessage();
            } catch (SQLException fallo) {
                error = "No pudimos guardar la decisión. No se confirmó ningún cambio; inténtalo de nuevo.";
            }
        }
        Router.enviar(intercambio, error.isEmpty() ? 200 : 400, detalle(sesion, solicitud, error, datos));
        return true;
    }

    private static String formulario(Sesion sesion, Map<String, String> datos, String mensaje) {
        StringBuilder html = new StringBuilder(
                "<div class=\"acceso-volver\"><a class=\"btn\" href=\"/login\">← Volver a iniciar sesión</a></div>" +
                        Html.aviso(mensaje, false));
        html.append("<p class=\"muted\">Cuéntanos de tu empresa. La Administradora revisará la solicitud antes de " +
                "habilitar el acceso.</p><form class=\"card stack solicitar-visita tarjeta-dorada\" method=\"post\" " +
                "action=\"/solicitar-acceso\">").append(Html.csrf(sesion));
        for (String[] campo : new String[][]{{"nombre", "Nombre", "100"}, {"cargo", "Cargo", "80"}, {"correo",
                "Correo", "120"}, {"telefono", "Teléfono", "20"}, {"empresa", "Empresa", "120"}, {"nit", "NIT", "20"}
                , {"ciudad", "Ciudad", "60"}, {"sector", "Sector (opcional)", "60"}}) {
            String tipo = campo[0].equals("correo") ? "email" : campo[0].equals("telefono") ? "tel" : "text";
            html.append(Formularios.campo(datos, campo[0], campo[1], tipo, Integer.parseInt(campo[2]),
                    !campo[0].equals("sector")));
        }
        html.append(Formularios.texto(datos, "necesidad", "¿Qué necesitas? (opcional)", 500, false))
                .append("<button class=\"btn p\">Enviar solicitud</button></form>");
        return Html.pagina("Solicitar acceso", html.toString(), null, "Tu empresa · Solicitud", "");
    }

    private String detalle(Sesion sesion, Solicitud s, String error, Map<String, String> datos) throws SQLException {
        StringBuilder html = new StringBuilder(Html.aviso(error, false));
        html.append("<section class=\"card stack\"><h2>").append(Html.escapar(s.empresa())).append("</h2><p>")
                .append(Html.escapar(s.nit() + " · " + s.ciudad())).append("</p><p>").append(Html.escapar(s.sector()))
                .append("</p><p>").append(Html.escapar(s.nombre() + " · " + s.cargo() + " · " + s.correo()))
                .append("</p><p>").append(Html.escapar(s.necesidad())).append("</p>")
                .append(SistemaVisual.estado(s.estado()));
        String tel = s.telefono().replaceAll("[^0-9+]", "");
        html.append("<a class=\"btn\" href=\"tel:").append(tel).append("\">Pedir una llamada · ")
                .append(Html.escapar(s.telefono())).append("</a>");
        if (s.estado().equals("PENDIENTE")) {
            html.append("<form class=\"stack\" method=\"post\" action=\"/solicitudes-acceso/").append(s.id())
                    .append("/aprobar\">").append(Html.csrf(sesion));
            var opciones = new java.util.ArrayList<Formularios.Opcion>();
            opciones.add(new Formularios.Opcion("", "Sin ingeniero"));
            for (var e : servicio.evaluadores(sesion.cuenta())) {
                opciones.add(new Formularios.Opcion(Integer.toString(e.id()), e.nombre()));
            }
            html.append(Formularios.selector(datos, "evaluador", "Ingeniero (opcional)", opciones, false))
                    .append("<button class=\"btn p\">Aprobar</button></form>")
                    .append("<form class=\"stack\" method=\"post\" action=\"/solicitudes-acceso/").append(s.id())
                    .append("/rechazar\">").append(Html.csrf(sesion))
                    .append(Formularios.texto(datos, "motivo", "Motivo de rechazo", 255, true))
                    .append("<button class=\"btn\">Rechazar</button></form>");
        }
        return Html.pagina("Solicitud de acceso", html.append("</section>").toString(), sesion,
                "Gestión · Solicitudes de acceso", "");
    }

    private static String aprobada(Sesion sesion, SolicitudesAcceso.Aprobacion resultado, String origen) {
        String mensaje = "Bienvenido a ALINEO. Ingresa en " + origen + "/login con el correo " + resultado.correo() +
                " y la contraseña temporal " + resultado.temporal() + ". Al entrar debes cambiarla.";
        String whatsapp = "https://wa.me/" + resultado.telefono().replaceAll("[^0-9]", "") + "?text=" +
                URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        String html = Html.aviso("Acceso aprobado. Copia la contraseña ahora; se muestra una sola vez.", true) +
                "<section class=\"card stack\"><h2>Cuenta creada</h2><p>" + Html.escapar(resultado.correo()) +
                "</p><label class=\"field\">Contraseña temporal<input class=\"input\" readonly " +
                "data-clave-temporal value=\"" + Html.escapar(resultado.temporal()) + "\"></label>" +
                "<div class=\"row\"><button class=\"btn\" type=\"button\" data-copiar-temporal>Copiar</button>" +
                "<a class=\"btn\" target=\"_blank\" rel=\"noopener noreferrer\" href=\"" + Html.escapar(whatsapp) +
                "\">Enviar por WhatsApp</a></div><p class=\"hint\">El enlace abre un mensaje para que tú lo revises " +
                "y envíes. No se envía automáticamente.</p><p role=\"status\" data-copia-estado></p></section>" +
                "<script src=\"/static/js/acceso-temporal.js\" defer></script>";
        return Html.pagina("Acceso aprobado", html, sesion, "Gestión · Solicitudes de acceso", "");
    }

    private static String origen(HttpExchange intercambio) {
        String host = intercambio.getRequestHeaders().getFirst("Host");
        if (host == null || !host.matches("[A-Za-z0-9.\\[\\]:-]+")) host = "localhost:8080";
        String protocolo = "https".equalsIgnoreCase(
                intercambio.getRequestHeaders().getFirst("X-Forwarded-Proto")) ? "https" : "http";
        return protocolo + "://" + host;
    }
}
