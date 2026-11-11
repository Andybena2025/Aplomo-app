package co.aplomo.web;

import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.modelo.*;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.*;
import co.aplomo.web.PaginasReservas.Contexto;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/** RF-009b a RF-012: formularios de agenda y altas en contexto sin perder la reserva escrita. */
public final class ReservasWeb {
    private static final Pattern REGISTRO = Pattern.compile("/reservas/([1-9][0-9]*)(?:/(editar|aceptar|cancelar))?");
    private final Reservas reservas;
    private final Empresas empresas;

    /** Comparte los servicios de agenda y configuración con la aplicación y las pruebas. */
    public ReservasWeb(Reservas reservas, Empresas empresas) { this.reservas = reservas; this.empresas = empresas; }

    /** Expone el servicio para que el Inicio consulte datos sin duplicar la lógica de acceso. */
    public Reservas servicio() { return reservas; }

    /** Atiende únicamente rutas y transiciones de reservas, con permisos de servidor. */
    public void atender(HttpExchange x, Sesion sesion, Map<String,String> datos) throws IOException, SQLException {
        String ruta = x.getRequestURI().getPath();
        boolean post = x.getRequestMethod().equals("POST");
        if (ruta.equals("/reservas") && !post) {
            Router.enviar(x, 200, PaginasReservas.lista(sesion, reservas.listar(sesion.cuenta())));
            return;
        }
        if (ruta.equals("/reservas/catalogo") && !post) {
            Permisos.administrador(sesion.cuenta());
            Router.enviar(x, 200, PaginasReservas.selectores(reservas.catalogo(sesion.cuenta()), Router.consulta(x)));
            return;
        }
        if (Set.of("/reservas/crear-empresa", "/reservas/crear-sede", "/reservas/crear-salon").contains(ruta)) {
            if (!post) { noDisponible(x, sesion); return; }
            Permisos.administrador(sesion.cuenta());
            alta(x, sesion, ruta.substring("/reservas/crear-".length()), datos);
            return;
        }
        if (ruta.equals("/reservas") || ruta.equals("/reservas/nueva")) {
            Permisos.administrador(sesion.cuenta());
            if (ruta.endsWith("nueva") && post) { noDisponible(x, sesion); return; }
            var contexto = new Contexto(null, reservas.catalogo(sesion.cuenta()),
                    post ? Map.of() : Router.consulta(x), reservas.hoy());
            formulario(x, sesion, contexto, datos, post);
            return;
        }
        var registro = REGISTRO.matcher(ruta);
        if (!registro.matches()) { noDisponible(x, sesion); return; }
        int id = id(registro.group(1));
        String accion = registro.group(2);
        Ficha ficha = reservas.consultar(sesion.cuenta(), id);
        if (accion == null && !post) {
            Router.enviar(x, 200, PaginasReservas.detalle(sesion, ficha, aviso(x), true));
            return;
        }
        if (accion == null) { noDisponible(x, sesion); return; }
        Permisos.administrador(sesion.cuenta());
        if (accion.equals("editar")) {
            if (!ficha.reserva().editable()) throw new IllegalArgumentException("Una reserva finalizada o cancelada no se puede editar.");
            formulario(x, sesion, new Contexto(ficha, null, campos(ficha), reservas.hoy()), datos, post);
            return;
        }
        if (accion.equals("aceptar")) {
            if (!post) { noDisponible(x, sesion); return; }
            try { reservas.aceptar(sesion.cuenta(), id); }
            catch (IllegalArgumentException e) { Router.enviar(x, 400, PaginasReservas.detalle(sesion, ficha, e.getMessage(), false)); return; }
            catch (SQLException e) { Router.enviar(x, 503, PaginasReservas.detalle(sesion, ficha, "No pudimos aceptar la reserva. Inténtalo de nuevo.", false)); return; }
            Router.redirigir(x, "/reservas/" + id + "?aviso=" + codificar("Reserva aceptada."));
            return;
        }
        if (!post) {
            if (!ficha.reserva().editable() || ficha.visita() != null) {
                throw new IllegalArgumentException("La reserva ya no se puede cancelar.");
            }
            Router.enviar(x, 200, PaginasReservas.cancelar(sesion, ficha, "", null));
            return;
        }
        try { reservas.cancelar(sesion.cuenta(), id, datos.get("motivo")); }
        catch (IllegalArgumentException e) { Router.enviar(x, 400, PaginasReservas.cancelar(sesion, ficha, datos.get("motivo"), e.getMessage())); return; }
        catch (SQLException e) { Router.enviar(x, 503, PaginasReservas.cancelar(sesion, ficha, datos.get("motivo"), "No pudimos cancelar. El motivo sigue escrito; inténtalo de nuevo.")); return; }
        Router.redirigir(x, "/reservas/" + id + "?aviso=" + codificar("Reserva cancelada. Su historial se conserva."));
    }

    private void formulario(HttpExchange x, Sesion sesion, Contexto contexto, Map<String,String> datos,
                            boolean post) throws IOException, SQLException {
        if (!post) { Router.enviar(x, 200, PaginasReservas.formulario(sesion, contexto, Map.of(), null)); return; }
        try {
            Ficha anterior = contexto.anterior();
            int empresa = anterior == null ? id(datos.get("empresa")) : anterior.empresa();
            int sede = anterior == null ? id(datos.get("sede")) : anterior.sede();
            int salon = anterior == null ? id(datos.get("salon")) : anterior.reserva().salon();
            var solicitud = new Reservas.Datos(empresa, sede, salon, fecha(datos.get("fecha")), hora(datos.get("inicio")),
                    hora(datos.get("fin")), datos.get("categoria"), datos.get("descripcion"), datos.get("notas"));
            int id = reservas.guardar(sesion.cuenta(), anterior == null ? 0 : anterior.reserva().id(), solicitud);
            Router.redirigir(x, "/reservas/" + id + "?aviso=" + codificar("Reserva guardada."));
        } catch (IllegalArgumentException e) {
            Router.enviar(x, 400, PaginasReservas.formulario(sesion, contexto, datos, e.getMessage()));
        } catch (SQLException e) {
            Router.enviar(x, 503, PaginasReservas.formulario(sesion, contexto, datos,
                    "No pudimos guardar la reserva. Tus datos siguen en el formulario; inténtalo de nuevo."));
        }
    }

    private void alta(HttpExchange x, Sesion sesion, String tipo, Map<String,String> datos) throws IOException {
        try {
            Empresas.Guardado guardado = switch (tipo) {
                case "empresa" -> empresas.guardar(sesion.cuenta(), new Empresa(0, 0, datos.get("nombre"), datos.get("nit"), "", "", "", "", true));
                case "sede" -> empresas.guardar(sesion.cuenta(), new Sede(0, id(datos.get("empresa")), datos.get("nombre"),
                        datos.get("direccion"), datos.get("ciudad"), datos.get("telefono"), datos.get("horario"), datos.get("descripcion"), true));
                case "salon" -> empresas.guardar(sesion.cuenta(), new Salon(0, id(datos.get("sede")), datos.get("nombre"), datos.get("descripcion"),
                        Validacion.entero(datos.get("piso"), "piso", -128, 127),
                        Validacion.entero(datos.get("capacidad_puestos"), "capacidad de puestos", 1, 32767), true));
                default -> throw new IllegalArgumentException("Elige un lugar válido.");
            };
            respuestaAlta(x, 200, "id=" + guardado.id() + "&aviso=" + codificar(guardado.aviso() == null ? "Lugar registrado. Puedes continuar con la reserva." : guardado.aviso()));
        } catch (IllegalArgumentException e) { respuestaAlta(x, 400, "aviso=" + codificar(e.getMessage())); }
        catch (SQLException e) { respuestaAlta(x, 503, "aviso=" + codificar("No pudimos guardar el lugar. Inténtalo de nuevo; los datos de la reserva se conservan.")); }
    }

    private static Map<String,String> campos(Ficha ficha) {
        Reserva r = ficha.reserva();
        Map<String,String> datos = new HashMap<>();
        datos.put("fecha", r.fecha().toString()); datos.put("inicio", r.inicio().toString()); datos.put("fin", r.fin().toString());
        datos.put("categoria", r.categoria()); datos.put("descripcion", r.descripcion()); datos.put("notas", r.notas());
        return datos;
    }

    private static LocalDate fecha(String texto) {
        try { return LocalDate.parse(texto == null ? "" : texto); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Escribe una fecha válida."); }
    }

    private static LocalTime hora(String texto) {
        try { return LocalTime.parse(texto == null ? "" : texto); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Escribe horas válidas de inicio y fin."); }
    }

    private static int id(String texto) { return Validacion.entero(texto, "empresa, sede o salón", 1, Integer.MAX_VALUE); }
    private static String codificar(String texto) { return URLEncoder.encode(texto, StandardCharsets.UTF_8); }
    private static String aviso(HttpExchange x) { return Router.consulta(x).get("aviso"); }
    private static void respuestaAlta(HttpExchange x, int estado, String datos) throws IOException {
        Router.responder(x, estado, "application/x-www-form-urlencoded; charset=UTF-8", datos);
    }
    private static void noDisponible(HttpExchange x, Sesion s) throws IOException {
        Router.enviar(x, 404, Html.pagina("Página no disponible", PaginasEmpresas.enlace("/reservas", "Volver a reservas", false), s));
    }
}
