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
        if (!ruta.equals("/solicitar-visita") && !ruta.startsWith("/solicitudes-visita/")) return false;
        if (sesion.cuenta().temporal()) throw new SecurityException("Cambia tu contraseña para continuar.");
        boolean post = intercambio.getRequestMethod().equals("POST");
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
        var sedes = lugares.stream().collect(java.util.stream.Collectors.toMap(SolicitudesVisita.Lugar::sede,
                l -> l.empresaNombre() + " · " + l.sedeNombre(), (a, b) -> a, java.util.LinkedHashMap::new));
        int sede = numero(datos.get("sede"));
        if (sede == 0 && !sedes.isEmpty()) sede = sedes.keySet().iterator().next();
        int elegida = sede;
        var salones = lugares.stream().filter(l -> l.sede() == elegida).toList();
        int empresa = salones.isEmpty() ? 0 : salones.get(0).empresa();
        var ingenieros =
                empresa == 0 ? List.<SolicitudesVisita.Ingeniero>of() : servicio.ingenieros(sesion.cuenta(), empresa);
        var seleccion = new java.util.HashMap<>(datos);
        seleccion.put("sede", Integer.toString(sede));
        StringBuilder html = new StringBuilder(Html.aviso(error, false));
        html.append("<form class=\"card stack solicitar-visita\" method=\"post\" action=\"/solicitar-visita\" ")
                .append("data-solicitar-visita>").append(Html.csrf(sesion))
                .append(Formularios.selector(seleccion, "sede", "Sede",
                        sedes.entrySet().stream().map(e -> new Formularios.Opcion(e.getKey().toString(), e.getValue()))
                                .toList(), true))
                .append(Formularios.selector(datos, "salon", "Salón", opcionesSalones(salones), true));
        html.append(
                        "<div class=\"field\"><label for=\"ingeniero\">Ingeniero</label><select id=\"ingeniero\" " +
                                "class=\"input\" name=\"ingeniero\" required>")
                .append("<option value=\"\">Elige un ingeniero</option><optgroup label=\"De tu empresa\">");
        for (boolean asignado : new boolean[]{true, false}) {
            if (!asignado) html.append("</optgroup><optgroup label=\"Otro ingeniero\">");
            for (var ingeniero : ingenieros)
                if (ingeniero.asignado() == asignado) {
                    html.append("<option value=\"").append(ingeniero.id()).append("\" data-color=\"")
                        .append(ingeniero.color() != null &&
                                co.aplomo.servicio.ColoresAgenda.PALETA.contains(ingeniero.color()) ?
                                ingeniero.color() : "#C9CCD2").append("\"")
                            .append(Integer.toString(ingeniero.id()).equals(datos.get("ingeniero")) ? " selected" : "")
                            .append(">").append(Html.escapar(ingeniero.nombre())).append("</option>");
                }
        }
        html.append("</optgroup></select><span class=\"small muted\" data-ingeniero-color></span></div>" +
                "<div class=\"grid3\" data-fecha-hora>")
                .append(Formularios.campo(datos, "fecha", "Fecha", "date", 10, true))
                .append(Formularios.campo(datos, "inicio", "Hora de inicio", "time", 8, true))
                .append(Formularios.campo(datos, "fin", "Hora de fin", "time", 8, true)).append("</div>")
                .append(Formularios.texto(datos, "motivo", "Motivo (opcional)", 255, false));
        if (cruce) html.append(alternativas(sesion, datos, ingenieros));
        html.append("<button class=\"btn p\">Solicitar una visita</button></form>")
                .append("<script src=\"/static/js/solicitar-visita.js\" defer></script>");
        return Html.pagina("Solicitar una visita", html.toString(), sesion, "Reservas · Solicitud", "");
    }

    private static List<Formularios.Opcion> opcionesSalones(List<SolicitudesVisita.Lugar> lugares) {
        var opciones = new ArrayList<Formularios.Opcion>();
        opciones.add(new Formularios.Opcion("", "Elige un salón"));
        lugares.forEach(l -> opciones.add(new Formularios.Opcion(Integer.toString(l.id()), l.nombre())));
        return opciones;
    }

    private String alternativas(Sesion sesion, Map<String, String> datos,
            List<SolicitudesVisita.Ingeniero> ingenieros) throws SQLException {
        var fecha = fecha(datos);
        var inicio = hora(datos, "inicio");
        var fin = hora(datos, "fin");
        int id = entero(datos, "ingeniero");
        long minutos = java.time.Duration.between(inicio, fin).toMinutes();
        StringBuilder html = new StringBuilder("<details><summary class=\"btn\">Elegir otra hora</summary>" +
                "<p class=\"hint\">Horas libres para la misma duración.</p><div class=\"chips\">");
        int libres = 0;
        for (LocalTime hora = LocalTime.of(8, 0); !hora.plusMinutes(minutos).isAfter(LocalTime.of(18, 0)) &&
                hora.isBefore(LocalTime.of(18, 0)); hora = hora.plusMinutes(30)) {
            if (servicio.libre(sesion.cuenta(), id, fecha, hora, hora.plusMinutes(minutos))) {
                html.append("<button class=\"chip\" type=\"button\" data-elegir-hora=\"").append(hora)
                        .append("\" data-fin=\"").append(hora.plusMinutes(minutos)).append("\">").append(hora)
                        .append("</button>");
                libres++;
            }
        }
        if (libres == 0) html.append("No hay horas libres este día.");
        html.append("</div></details><details><summary class=\"btn\">Elegir otro ingeniero</summary>" +
                "<div class=\"chips\">");
        libres = 0;
        for (var ingeniero : ingenieros) {
            if (ingeniero.id() != id && servicio.libre(sesion.cuenta(), ingeniero.id(), fecha, inicio, fin)) {
                html.append("<button class=\"chip\" type=\"button\" data-elegir-ingeniero=\"").append(ingeniero.id())
                        .append("\">").append(Html.escapar(ingeniero.nombre())).append("</button>");
                libres++;
            }
        }
        if (libres == 0) html.append("No hay otro ingeniero libre a esta hora.");
        return html.append("</div></details>").toString();
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
