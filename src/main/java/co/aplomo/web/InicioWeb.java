package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.servicio.Evaluaciones;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-013a, RNF-011: composición de Inicio separada del enrutamiento HTTP.
 */
public final class InicioWeb {
    private final Empresas empresas;
    private final Reservas reservas;
    private final Visitas visitas;
    private final Evaluaciones evaluaciones;

    /**
     * RNF-011: recibe los mismos servicios autorizados del Router.
     */
    public InicioWeb(Empresas empresas, Reservas reservas, Visitas visitas, Evaluaciones evaluaciones) {
        this.empresas = empresas;
        this.reservas = reservas;
        this.visitas = visitas;
        this.evaluaciones = evaluaciones;
    }

    /**
     * RF-013a: mantiene indicadores, acciones y alcance del Inicio existente.
     */
    public void atender(HttpExchange intercambio, Sesion sesion) throws IOException, SQLException {
        String aviso = "contrasena=1".equals(intercambio.getRequestURI().getRawQuery()) ? Html.aviso(
                "Contraseña actualizada.", true) : "";
        Cuenta cuenta = sesion.cuenta();
        String nombre = cuenta.nombre().contains(" ") ? cuenta.nombre()
                                                        .substring(0, cuenta.nombre().indexOf(' ')) : cuenta.nombre();

        // Consultar datos reales para el panel de inicio (RF-013a)
        var listaReservas = reservas.listar(cuenta);
        var listaVisitas = visitas.listar(cuenta);
        long enCurso = listaVisitas.stream().filter(visita -> visita.visita().estado().equals("EN_CURSO")).count();
        long enCola = listaReservas.stream().filter(reserva -> reserva.reserva().estado().equals("EN_COLA")).count();
        long[] resumen = evaluaciones.resumen(cuenta);
        StringBuilder inicio = new StringBuilder(aviso);

        saludo(cuenta, nombre, inicio);
        agendaHoy(cuenta, inicio);
        indicadores(inicio, enCurso, enCola, resumen);
        acciones(cuenta, inicio);
        visitas(cuenta, resumen, inicio);
        reservasPendientes(listaReservas, inicio);
        inicio.append("</section></div>");
        Router.enviar(intercambio, 200, Html.pagina("Inicio", inicio.toString(), sesion, false, false));
        return;
    }

    private void saludo(Cuenta cuenta, String nombre, StringBuilder inicio) throws SQLException {
        // Saludo con fecha como en el mock
        inicio.append("<div class=\"pagehead\"><div><span class=\"eyebrow\">")
                .append(Html.fecha(Html.hoy()).toUpperCase(java.util.Locale.ROOT)).append("</span><h1>Hola, ")
                .append(Html.escapar(nombre)).append("</h1></div></div>");
        if (cuenta.rol() != Cuenta.Rol.USUARIO)
            inicio.append(InicioEvaluacion.practica(visitas.practica().enCurso(cuenta)));
        if (cuenta.rol() != Cuenta.Rol.ADMINISTRADOR && empresas.listar(cuenta).isEmpty())
            inicio.append(Html.aviso("Aún no tienes empresas asignadas. Pídeselas al Administrador.", false));
    }

    private void agendaHoy(Cuenta cuenta, StringBuilder inicio) throws SQLException {
        if (cuenta.rol() == Cuenta.Rol.EVALUADOR) {
            var ag = reservas.agenda();
            var notas = ag.nuevas(cuenta);
            if (!notas.isEmpty())
                inicio.append("<p><a href=\"/reservas/").append(notas.get(0)).append("\">Tienes ").append(notas.size())
                        .append(notas.size() == 1 ? " nota nueva del Administrador</a></p>" :
                                " notas nuevas del " + "Administrador</a></p>");
            var hoy = ag.consultar(cuenta, Map.of());
            inicio.append("<section class=\"card stack\"><h2>Hoy en tu agenda</h2>");
            var citasHoy = hoy.citas().stream().filter(cita2 -> cita2.ficha().reserva().fecha().equals(reservas.hoy()))
                    .limit(3).toList();
            if (citasHoy.isEmpty()) inicio.append("<p class=\"muted\">No tienes citas hoy.</p>");
            else for (var cita : citasHoy) inicio.append(PaginasAgenda.cita(cita));
            inicio.append("<a class=\"btn\" href=\"/agenda\">Ver mi agenda</a></section>");
        }

    }

    private void indicadores(StringBuilder inicio, long enCurso, long enCola, long[] resumen) {
        // 4 tarjetas KPI como en el mock
        inicio.append("<div class=\"grid4\" style=\"margin-bottom:24px\">")
                .append(kpi("Visitas en curso", String.valueOf(enCurso), null))
                .append(kpi("Reservas en cola", String.valueOf(enCola), null))
                .append("<a class=\"kpi-enlace\" href=\"/evaluaciones\">")
                .append(kpi("Evaluaciones", String.valueOf(resumen[0]), null)).append("</a>")
                .append(kpi("Puestos en rojo", String.valueOf(resumen[1]), "color:var(--rojo)")).append("</div>");
    }

    private void acciones(Cuenta cuenta, StringBuilder inicio) {
        if (cuenta.rol() == Cuenta.Rol.ADMINISTRADOR) {
            inicio.append("<div class=\"inicio-acciones\">");
            inicio.append(accionInicio("/reservas/nueva", "Nueva reserva", "Programa una evaluación.",
                    "M5 7h14v13H5z M8 4v6 M16 4v6 M5 12h14"));
            inicio.append(accionInicio("/reservas", "Iniciar visita", "Continúa desde una reserva en cola.",
                    "M9 5H5v15h14v-5 M12 4h8v8 M20 4L10 14"));
            inicio.append(accionInicio("/empresas/nueva", "Nueva empresa", "Registra la empresa y sus sedes.",
                    "M5 21V5h10v16 M15 11h4v10 M9 9h2 M9 13h2 M9 17h2 M3 21h18"));
            inicio.append("</div>");
        }

    }

    private void visitas(Cuenta cuenta, long[] resumen, StringBuilder inicio) throws SQLException {
        // Visitas en curso y reservas recientes como en el mock
        inicio.append("<div class=\"grid2\"><section class=\"stack\">");
        if (cuenta.rol() != Cuenta.Rol.USUARIO) {
            inicio.append("<h3>Semáforo de puestos</h3><div class=\"semaforo\">")
                    .append(semaforo("VERDE", resumen[2], "Aceptable", "/evaluaciones"))
                    .append(semaforo("AMARILLO", resumen[3], "En observación", "/evaluaciones"))
                    .append(semaforo("ROJO", resumen[1], "Pendiente", "/pendientes"));
            if (resumen[1] + resumen[2] + resumen[3] > 0)
                inicio.append("<div class=\"barra\" aria-hidden=\"true\"><i style=\"flex:").append(resumen[2])
                        .append(";background:var(--verde)\"></i><i style=\"flex:").append(resumen[3])
                        .append(";background:var(--amarillo)\"></i><i style=\"flex:").append(resumen[1])
                        .append(";background:var(--rojo)\"></i></div>");
            inicio.append("</div>");
        }
        if (cuenta.rol() != Cuenta.Rol.USUARIO) inicio.append(InicioEvaluacion.tarjeta(visitas.paraEvaluar(cuenta)));
        else inicio.append("<h3>Visitas en curso</h3>").append(Html.vacio("Aún no hay visitas disponibles.", ""));
    }

    private void reservasPendientes(java.util.List<co.aplomo.dao.ReservaDao.Ficha> listaReservas,
            StringBuilder inicio) {
        inicio.append("</section><section class=\"stack\"><h3>Reservas</h3>");
        var pendientes = listaReservas.stream().filter(reserva2 -> reserva2.reserva().estado().equals("EN_COLA") ||
                reserva2.reserva().estado().equals("ESPERANDO")).toList();
        if (pendientes.isEmpty()) {
            inicio.append(Html.vacio("No hay reservas pendientes.", ""));
        } else {
            for (var reserva3 : pendientes) {
                inicio.append("<div class=\"card stack\" style=\"gap:10px\"><div class=\"row between\"><div><div>")
                        .append(Html.escapar(reserva3.empresaNombre())).append("</div><div class=\"muted small\">")
                        .append(Html.escapar(reserva3.sedeNombre())).append(" · ")
                        .append(Html.fecha(reserva3.reserva().fecha())).append(" · ")
                        .append(reserva3.reserva().inicio()).append("</div></div><span class=\"tag")
                        .append(reserva3.reserva().estado().equals("EN_COLA") ? " good" : " warn").append("\">")
                        .append(reserva3.reserva().estado().equals("EN_COLA") ? "En cola" : "Esperando")
                        .append("</span></div>");
                if (reserva3.reserva().descripcion() != null && !reserva3.reserva().descripcion().isBlank()) {
                    inicio.append("<p class=\"small muted\">").append(Html.escapar(reserva3.reserva().descripcion()))
                            .append("</p>");
                }
                if (reserva3.reserva().estado().equals("EN_COLA") && reserva3.visita() == null) {
                    inicio.append("<div class=\"row\"><a class=\"btn sm p\" href=\"/reservas/")
                            .append(reserva3.reserva().id()).append("\">Ver reserva</a></div>");
                }
                inicio.append("</div>");
            }
        }
    }

    private static String accionInicio(String ruta, String titulo, String ayuda, String trazo) {
        return "<a class=\"card accion-inicio\" href=\"" + ruta +
                "\"><svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"" + trazo + "\"/></svg><span><strong>" +
                titulo + "</strong><span class=\"small muted\">" + ayuda + "</span></span></a>";
    }

    private static String kpi(String titulo, String valor, String estilo) {
        return "<div class=\"card kpi\"><span class=\"eyebrow\">" + Html.escapar(titulo) + "</span><b class=\"num\"" +
                (estilo != null ? " style=\"" + estilo + "\"" : "") + ">" + Html.escapar(valor) + "</b></div>";
    }

    private static String semaforo(String color, long cantidad, String texto, String ruta) {
        return "<a class=\"s\" href=\"" + ruta + "\" data-color=\"" + color + "\" data-cantidad=\"" + cantidad +
                "\"><b style=\"color:var(--" + color.toLowerCase(java.util.Locale.ROOT) + ")\">" + cantidad +
                "</b><span>" + Formularios.etiqueta(color) + "<br>" + texto + "</span></a>";
    }
}
