package co.aplomo.web;

import co.aplomo.servicio.SolicitudesVisita;
import co.aplomo.servicio.Validacion;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RF-002/011 (propuesta): solicitud breve, alternativas de horario y respuesta privada.
 */
public final class SolicitudesVisitaWeb {
    private final SolicitudesVisita servicio;

    /**
     * RNF-011: mantiene las decisiones de agenda en el servicio.
     */
    public SolicitudesVisitaWeb(SolicitudesVisita servicio) {
        this.servicio = servicio;
    }

    /**
     * RF-002: estas rutas preceden las consultas generales de reservas.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion,
                           Map<String, String> datos) throws SQLException, IOException {
        String ruta = intercambio.getRequestURI().getPath();
        if (!ruta.equals("/solicitar-visita") && !ruta.equals("/solicitar-visita/horas") &&
                !ruta.startsWith("/solicitudes-visita/")) return false;
        if (sesion.cuenta().temporal()) throw new SecurityException("Cambia tu contraseña para continuar.");
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/solicitar-visita/horas")) {
            if (post) {
                Router.enviar(intercambio, 405, "Método no permitido.");
                return true;
            }
            var consulta = Router.consulta(intercambio);
            servicio.lugares(sesion.cuenta());
            var horas = servicio.horas(sesion.cuenta(), entero(consulta, "ingeniero"), fecha(consulta),
                    entero(consulta, "duracion"), consulta.containsKey("salon") ? entero(consulta, "salon") : null);
            String json = horas.stream().map(h -> NotasWeb.texto(h.toString()))
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
            intercambio.getResponseHeaders().set("Cache-Control", "no-store");
            Router.responder(intercambio, 200, "application/json; charset=UTF-8", json);
            return true;
        }
        if (ruta.equals("/solicitar-visita")) {
            if (!post) datos = Router.consulta(intercambio);
            servicio.lugares(sesion.cuenta());
            String error = "";
            boolean cruce = false;
            if (post) {
                try {
                    int id = servicio.solicitar(sesion.cuenta(), entero(datos, "salon"), entero(datos, "ingeniero"),
                            fecha(datos), hora(datos, "inicio"), hora(datos, "fin"), datos.get("motivo"));
                    Router.enviar(intercambio, 200, Html.pagina("Solicitud enviada",
                            Html.aviso("Le avisamos al ingeniero. Te contactará pronto.", true) +
                                    "<a class=\"btn\" href=\"/reservas/" + id + "\">Ver solicitud</a>", sesion));
                    return true;
                } catch (IllegalArgumentException fallo) {
                    error = fallo.getMessage();
                    cruce = fallo instanceof SolicitudesVisita.Cruce;
                }
            }
            Router.enviar(intercambio, error.isEmpty() ? 200 : 400, formulario(sesion, datos, error, cruce));
            return true;
        }
        if (!ruta.matches("/solicitudes-visita/[1-9][0-9]*(/(aceptar|rechazar|llamada|informacion))?")) return false;
        int id = Integer.parseInt(ruta.split("/")[2]);
        var peticion = servicio.consultar(sesion.cuenta(), id);
        String error = "";
        if (post) {
            String[] partes = ruta.split("/");
            if (partes.length != 4) throw new IllegalArgumentException("Elige una acción disponible.");
            try {
                servicio.actuar(sesion.cuenta(), id, partes[3], datos.get("texto"));
                Router.redirigir(intercambio,
                        partes[3].equals("aceptar") ? "/reservas/" + id : "/solicitudes-visita/" + id + "?enviado=1");
                return true;
            } catch (IllegalArgumentException fallo) {
                error = fallo.getMessage();
            }
        }
        Router.enviar(intercambio, error.isEmpty() ? 200 : 400, detalle(sesion, peticion, error, datos));
        return true;
    }

    private String formulario(Sesion sesion, Map<String, String> datos, String error,
                              boolean cruce) throws SQLException {
        var lugares = servicio.lugares(sesion.cuenta());
        if (lugares.isEmpty()) {
            return Html.pagina("Solicitar una visita", Html.vacio(
                    "Tu empresa aún no tiene una sede y un salón disponibles. " +
                            "Contacta a tu ingeniero para prepararlos.",
                    "<a class=\"btn\" href=\"/ayuda\">Ver ayuda</a>"), sesion, "Reservas · Solicitud", "");
        }
        return SolicitudGuiada.formulario(sesion, servicio, lugares, datos, error, cruce);
    }

    private static String detalle(Sesion sesion, SolicitudesVisita.Peticion p, String error,
                                  Map<String, String> datos) {
        var ficha = p.ficha();
        int id = ficha.reserva().id();
        String telefono = p.telefono() == null ? "" : p.telefono().replaceAll("[^0-9+]", "");
        StringBuilder html = new StringBuilder(Html.aviso(error, false));
        html.append("<section class=\"card stack\"><h2>").append(Html.escapar(ficha.empresaNombre())).append("</h2><p>")
                .append(Html.escapar(ficha.sedeNombre() + " · " + ficha.salonNombre())).append("</p><p>")
                .append(Html.fecha(ficha.reserva().fecha())).append(" · ").append(ficha.reserva().inicio())
                .append(" – ").append(ficha.reserva().fin()).append("</p><p>")
                .append(Html.escapar(ficha.reserva().descripcion())).append("</p><p>")
                .append(Html.escapar(p.nombre() + " · " + p.correo())).append("</p><div class=\"row\">")
                .append("<a class=\"btn\" href=\"tel:").append(Html.escapar(telefono)).append("\">Llamar · ")
                .append(Html.escapar(p.telefono())).append("</a><a class=\"btn\" href=\"https://wa.me/")
                .append(telefono.replace("+", "")).append("\" rel=\"noopener\">WhatsApp</a></div>");
        if (ficha.reserva().estado().equals("ESPERANDO")) {
            for (String accion : List.of("aceptar", "llamada", "informacion", "rechazar")) {
                String titulo = switch (accion) {
                    case "aceptar" -> "Aceptar";
                    case "llamada" -> "Pedir una llamada";
                    case "informacion" -> "Pedir información";
                    default -> "Rechazar";
                };
                html.append("<form method=\"post\" action=\"/solicitudes-visita/").append(id).append("/").append(accion)
                        .append("\">").append(Html.csrf(sesion));
                if (accion.equals("informacion") || accion.equals("rechazar")) {
                    html.append(Formularios.texto(datos, "texto",
                                    accion.equals("rechazar") ? "Motivo" : "¿Qué información necesitas?",
                                    accion.equals("rechazar") ? 255 : 500, true)
                            .replace("for=\"texto\"", "for=\"texto-" + accion + "\"")
                            .replace("id=\"texto\"", "id=\"texto-" + accion + "\""));
                }
                html.append("<button class=\"btn\">").append(titulo).append("</button></form>");
            }
        }
        return Html.pagina("Solicitud de visita", html.append("</section>").toString(), sesion, "Reservas · Solicitud",
                "");
    }

    private static int entero(Map<String, String> datos, String campo) {
        return Validacion.entero(datos.get(campo), campo, 1, Integer.MAX_VALUE);
    }

    private static int numero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (RuntimeException error) {
            return 0;
        }
    }

    private static LocalDate fecha(Map<String, String> datos) {
        try {
            return LocalDate.parse(datos.get("fecha"));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Elige una fecha válida.");
        }
    }

    private static LocalTime hora(Map<String, String> datos, String campo) {
        try {
            return LocalTime.parse(datos.get(campo));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Elige una hora válida.");
        }
    }
}
