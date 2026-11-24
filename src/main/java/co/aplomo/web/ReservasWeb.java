package co.aplomo.web;

import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Reserva;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Validacion;
import co.aplomo.web.PaginasReservas.Contexto;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * RF-009b a RF-012: formularios de agenda y altas en contexto sin perder la reserva escrita.
 */
public final class ReservasWeb {
    private static final Pattern REGISTRO = Pattern.compile("/reservas/([1-9][0-9]*)(?:/(editar|aceptar|cancelar))?");
    private final Reservas reservas;
    private final Empresas empresas;

    /**
     * Comparte los servicios de agenda y configuración con la aplicación y las pruebas. RNF-011.
     */
    public ReservasWeb(Reservas reservas, Empresas empresas) {
        this.reservas = reservas;
        this.empresas = empresas;
    }

    /**
     * Expone el servicio para que el Inicio consulte datos sin duplicar la lógica de acceso. RNF-011.
     */
    public Reservas servicio() {
        return reservas;
    }

    /**
     * Atiende únicamente rutas y transiciones de reservas, con permisos de servidor. RNF-011.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> datos) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/reservas/horas-libres") && !post) {
            Permisos.evaluador(sesion.cuenta());
            Map<String, String> consulta = Router.consulta(intercambio);
            java.time.LocalDate fecha;
            try {
                fecha = java.time.LocalDate.parse(consulta.getOrDefault("fecha", ""));
            } catch (java.time.format.DateTimeParseException error) {
                Router.enviar(intercambio, 400, "Elige una fecha válida para ver las horas libres.");
                return;
            }
            int omitir = co.aplomo.servicio.Validacion.entero(consulta.getOrDefault("omitir", "0"), "reserva", 0,
                    Integer.MAX_VALUE);
            if (omitir > 0) reservas.consultar(sesion.cuenta(), omitir);
            StringBuilder horas = new StringBuilder();
            for (java.time.LocalTime hora : reservas.horasLibres(sesion.cuenta(), fecha, omitir)) {
                horas.append("<button class=\"chip\" type=\"button\" data-hora-libre=\"").append(hora).append("\">")
                        .append(hora.format(java.time.format.DateTimeFormatter.ofPattern("H:mm"))).append("</button>");
            }
            Router.enviar(intercambio, 200,
                    horas.isEmpty() ? "No hay horas libres entre las 8:00 y las 16:00." : horas.toString());
            return;
        }
        if (ruta.equals("/reservas/catalogo") && !post) {
            Permisos.evaluador(sesion.cuenta());
            Router.enviar(intercambio, 200,
                    PaginasReservas.selectores(reservas.catalogo(sesion.cuenta()), Router.consulta(intercambio)));
            return;
        }
        if (Set.of("/reservas/crear-empresa", "/reservas/crear-sede", "/reservas/crear-salon").contains(ruta)) {
            if (!post) {
                noDisponible(intercambio, sesion);
                return;
            }
            Permisos.evaluador(sesion.cuenta());
            alta(intercambio, sesion, ruta.substring("/reservas/crear-".length()), datos);
            return;
        }
        if (ruta.equals("/reservas") || ruta.equals("/reservas/nueva")) {
            Permisos.evaluador(sesion.cuenta());
            if (ruta.endsWith("nueva") && post) {
                noDisponible(intercambio, sesion);
                return;
            }
            var preseleccion = new java.util.HashMap<>(post ? Map.<String, String>of() : Router.consulta(intercambio));
            if (!post && preseleccion.containsKey("puesto")) preseleccion.putAll(
                    reservas.paraReevaluar(sesion.cuenta(), id(preseleccion.get("puesto"))));
            var contexto = new Contexto(null, reservas.catalogo(sesion.cuenta()), preseleccion, reservas.hoy());
            formulario(intercambio, sesion, contexto, datos, post);
            return;
        }
        var registro = REGISTRO.matcher(ruta);
        if (!registro.matches()) {
            noDisponible(intercambio, sesion);
            return;
        }
        int id = id(registro.group(1));
        String accion = registro.group(2);
        Ficha ficha = reservas.consultar(sesion.cuenta(), id);
        if (accion == null && !post) {
            Router.enviar(intercambio, 200, PaginasReservas.detalle(sesion, ficha, aviso(intercambio), true,
                    reservas.agenda().nota(sesion.cuenta(), id)));
            return;
        }
        if (accion == null) {
            noDisponible(intercambio, sesion);
            return;
        }
        Permisos.evaluador(sesion.cuenta());
        if (accion.equals("editar")) {
            if (!ficha.reserva().editable())
                throw new IllegalArgumentException("Una reserva finalizada o cancelada no se puede editar.");
            formulario(intercambio, sesion, new Contexto(ficha, null, campos(ficha), reservas.hoy()), datos, post);
            return;
        }
        if (accion.equals("aceptar")) {
            if (!post) {
                noDisponible(intercambio, sesion);
                return;
            }
            try {
                reservas.aceptar(sesion.cuenta(), id);
            } catch (IllegalArgumentException error) {
                Router.enviar(intercambio, 400, PaginasReservas.detalle(sesion, ficha, error.getMessage(), false));
                return;
            } catch (SQLException error2) {
                Router.enviar(intercambio, 503,
                        PaginasReservas.detalle(sesion, ficha, "No pudimos aceptar la reserva. Inténtalo de nuevo.",
                                false));
                return;
            }
            Router.redirigir(intercambio, "/reservas/" + id + "?aviso=" + codificar("Reserva aceptada."));
            return;
        }
        if (!post) {
            if (!ficha.reserva().editable() || ficha.visita() != null) {
                throw new IllegalArgumentException("La reserva ya no se puede cancelar.");
            }
            Router.enviar(intercambio, 200, PaginasReservas.cancelar(sesion, ficha, "", null));
            return;
        }
        try {
            reservas.cancelar(sesion.cuenta(), id, datos.get("motivo"));
        } catch (IllegalArgumentException error3) {
            Router.enviar(intercambio, 400,
                    PaginasReservas.cancelar(sesion, ficha, datos.get("motivo"), error3.getMessage()));
            return;
        } catch (SQLException error4) {
            Router.enviar(intercambio, 503, PaginasReservas.cancelar(sesion, ficha, datos.get("motivo"),
                    "No pudimos cancelar. El motivo sigue escrito; inténtalo de nuevo."));
            return;
        }
        Router.redirigir(intercambio,
                "/reservas/" + id + "?aviso=" + codificar("Reserva cancelada. Su historial se conserva."));
    }

    private void formulario(HttpExchange intercambio, Sesion sesion, Contexto contexto, Map<String, String> datos,
            boolean post) throws IOException, SQLException {
        if (!post) {
            Router.enviar(intercambio, 200, PaginasReservas.formulario(sesion, contexto, Map.of(), null));
            return;
        }
        try {
            Ficha anterior = contexto.anterior();
            int empresa = anterior == null ? id(datos.get("empresa")) : anterior.empresa();
            int sede = anterior == null ? id(datos.get("sede")) : anterior.sede();
            int salon = anterior == null ? id(datos.get("salon")) : anterior.reserva().salon();
            var solicitud = new Reservas.Datos(empresa, sede, salon, fecha(datos.get("fecha")),
                    hora(datos.get("inicio")), hora(datos.get("fin")), datos.get("categoria"), datos.get("descripcion"),
                    datos.get("notas"));
            int id = reservas.guardar(sesion.cuenta(), anterior == null ? 0 : anterior.reserva().id(), solicitud);
            Router.redirigir(intercambio, "/reservas/" + id + "?aviso=" + codificar("Reserva guardada."));
        } catch (IllegalArgumentException error) {
            Router.enviar(intercambio, 400, PaginasReservas.formulario(sesion, contexto, datos, error.getMessage()));
        } catch (SQLException error2) {
            Router.enviar(intercambio, 503, PaginasReservas.formulario(sesion, contexto, datos,
                    "No pudimos guardar la reserva. Tus datos siguen en el formulario; inténtalo " + "de nuevo."));
        }
    }

    private void alta(HttpExchange intercambio, Sesion sesion, String tipo,
            Map<String, String> datos) throws IOException {
        try {
            Empresas.Guardado guardado = switch (tipo) {
                case "empresa" -> empresas.guardar(sesion.cuenta(),
                        new Empresa(0, 0, datos.get("nombre"), datos.get("nit"), "", "", "", "", true));
                case "sede" -> empresas.guardar(sesion.cuenta(),
                        new Sede(0, id(datos.get("empresa")), datos.get("nombre"), datos.get("direccion"),
                                datos.get("ciudad"), datos.get("telefono"), datos.get("horario"),
                                datos.get("descripcion"), true));
                case "salon" -> empresas.guardar(sesion.cuenta(),
                        new Salon(0, id(datos.get("sede")), datos.get("nombre"), datos.get("descripcion"),
                                Validacion.entero(datos.get("piso"), "piso", -128, 127),
                                Validacion.entero(datos.get("capacidad_puestos"), "capacidad de puestos", 1, 32767),
                                true));
                default -> throw new IllegalArgumentException("Elige un lugar válido.");
            };
            respuestaAlta(intercambio, 200, "id=" + guardado.id() + "&aviso=" + codificar(guardado.aviso() ==
                    null ? "Lugar registrado. Puedes continuar con la reserva." : guardado.aviso()));
        } catch (IllegalArgumentException error) {
            respuestaAlta(intercambio, 400, "aviso=" + codificar(error.getMessage()));
        } catch (SQLException error2) {
            respuestaAlta(intercambio, 503, "aviso=" + codificar(
                    "No pudimos guardar el lugar. Inténtalo de nuevo; los datos de la reserva se " + "conservan."));
        }
    }

    private static Map<String, String> campos(Ficha ficha) {
        Reserva reserva = ficha.reserva();
        Map<String, String> datos = new HashMap<>();
        datos.put("fecha", reserva.fecha().toString());
        datos.put("inicio", reserva.inicio().toString());
        datos.put("fin", reserva.fin().toString());
        datos.put("categoria", reserva.categoria());
        datos.put("descripcion", reserva.descripcion());
        datos.put("notas", reserva.notas());
        return datos;
    }

    private static LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto == null ? "" : texto);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Escribe una fecha válida.");
        }
    }

    private static LocalTime hora(String texto) {
        try {
            return LocalTime.parse(texto == null ? "" : texto);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Escribe horas válidas de inicio y fin.");
        }
    }

    private static int id(String texto) {
        return Validacion.entero(texto, "empresa, sede o salón", 1, Integer.MAX_VALUE);
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static String aviso(HttpExchange intercambio) {
        return Router.consulta(intercambio).get("aviso");
    }

    private static void respuestaAlta(HttpExchange intercambio, int estado, String datos) throws IOException {
        Router.responder(intercambio, estado, "application/x-www-form-urlencoded; charset=UTF-8", datos);
    }

    private static void noDisponible(HttpExchange intercambio, Sesion sesion2) throws IOException {
        Router.enviar(intercambio, 404,
                Html.pagina("Página no disponible", PaginasEmpresas.enlace("/reservas", "Volver a reservas", false),
                        sesion2));
    }
}
