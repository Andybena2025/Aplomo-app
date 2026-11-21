package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Agenda;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF-013a/024 y Paso 27: tres inicios con los mismos datos y permisos de sus listas.
 */
public final class InicioWeb {
    private final Empresas empresas;
    private final Reservas reservas;
    private final Visitas visitas;
    private final Evaluaciones evaluaciones;

    /**
     * RNF-011: comparte servicios y transacciones con el Router.
     */
    public InicioWeb(Empresas empresas, Reservas reservas, Visitas visitas, Evaluaciones evaluaciones) {
        this.empresas = empresas;
        this.reservas = reservas;
        this.visitas = visitas;
        this.evaluaciones = evaluaciones;
    }

    /**
     * RF-002/013a: compone exclusivamente el inicio permitido para el rol vigente.
     */
    public void atender(HttpExchange intercambio, Sesion sesion) throws IOException, SQLException {
        Cuenta cuenta = sesion.cuenta();
        StringBuilder html = new StringBuilder();
        if ("contrasena=1".equals(intercambio.getRequestURI().getRawQuery())) {
            html.append(Html.aviso("Contraseña actualizada.", true));
        }
        List<Empresa> companias = empresas.listar(cuenta);
        saludo(cuenta, companias, html);
        switch (cuenta.rol()) {
            case ADMINISTRADOR -> administrador(cuenta, companias, html);
            case EVALUADOR -> evaluador(cuenta, html);
            case USUARIO -> representante(cuenta, companias, html);
        }
        Router.enviar(intercambio, 200, Html.pagina("Inicio", html.toString(), sesion, false, false));
    }

    private void saludo(Cuenta cuenta, List<Empresa> companias, StringBuilder html) {
        String nombre = cuenta.nombre().replaceFirst("(?i)^(Ing\\.|Rep\\.)\\s*", "").split("\\s+")[0];
        String trato = switch (cuenta.rol()) {
            case ADMINISTRADOR -> cuenta.correo().equals("gabby@rula.local") ? "Administradora " : "Administrador ";
            case EVALUADOR -> "Ing. ";
            case USUARIO -> "Representante ";
        };
        String subtitulo = cuenta.rol() == Cuenta.Rol.ADMINISTRADOR ? "Administración de ALINEO" :
                cuenta.rol() == Cuenta.Rol.EVALUADOR ? "Tu jornada de evaluación" : companias.stream()
                                                                                    .map(Empresa::nombre)
                                                                                    .collect(Collectors.joining(" · "));
        String fecha = reservas.hoy().format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", new Locale("es", "CO")));
        html.append("<div class=\"pagehead\"><div><span class=\"eyebrow\">")
                .append(Html.escapar(fecha.toUpperCase(Locale.ROOT))).append("</span><h1>Hola, ")
                .append(Html.escapar(trato + nombre)).append("</h1><p class=\"muted\">").append(Html.escapar(subtitulo))
                .append("</p></div></div>");
    }

    private List<Fila> filas(Cuenta cuenta, SeccionConsulta seccion) throws SQLException {
        return evaluaciones.consultas().listar(cuenta, seccion, Map.of()).filas();
    }

    private void evaluador(Cuenta cuenta, StringBuilder html) throws SQLException {
        List<Ficha> citas = reservas.listar(cuenta);
        List<Fila> visitasLista = filas(cuenta, SeccionConsulta.VISITAS);
        List<Fila> evaluacionesLista = filas(cuenta, SeccionConsulta.EVALUACIONES);
        List<Fila> pendientes = filas(cuenta, SeccionConsulta.PENDIENTES);
        proxima(cuenta, citas, html);
        int puestosPendientes = Math.toIntExact(
                visitas.paraEvaluar(cuenta).stream().filter(puesto -> puesto.puesto() != null).count());
        html.append("<div class=\"inicio-indicadores\">")
                .append(kpi("Visitas en curso", contar(visitasLista, "EN_CURSO"), "/visitas?estado=EN_CURSO").replace(
                        "</a>", "<small>" + Html.plural(puestosPendientes, "puesto") + " por evaluar</small></a>"))
                .append(kpi("Por confirmar",
                        citas.stream().filter(c -> c.reserva().estado().equals("ESPERANDO")).count(),
                        "/reservas?estado=ESPERANDO"))
                .append(kpi("Evaluaciones", evaluacionesLista.size(), "/evaluaciones"))
                .append(kpi("Pendientes abiertos", contar(pendientes, "ABIERTO"), "/pendientes?estado=ABIERTO"))
                .append("</div><div class=\"inicio-columnas\"><section class=\"stack\">");
        visitasEnCurso(cuenta, html);
        html.append(InicioEvaluacion.practica(visitas.practica().enCurso(cuenta)));
        riesgos(evaluaciones.consultas().archivo().consultar(cuenta).evaluaciones(), html);
        html.append("</section><section class=\"stack\"><h2>Próximas citas</h2>");
        proximas(citas, html);
        html.append("</section></div>");
    }

    private void administrador(Cuenta cuenta, List<Empresa> companias, StringBuilder html) throws SQLException {
        Agenda.Vista semana = reservas.agenda().consultar(cuenta, Map.of("vista", "semana"));
        List<Fila> pendientes = filas(cuenta, SeccionConsulta.PENDIENTES);
        html.append("<div class=\"inicio-indicadores\">")
                .append(kpi("Empresas activas", companias.stream().filter(Empresa::activo).count(),
                        "/empresas?estado=ACTIVO")).append(kpi("Evaluadores activos",
                        semana.evaluadores().stream().filter(Agenda.Evaluador::activo).count(),
                        "/usuarios?rol=EVALUADOR&estado=ACTIVA"))
                .append(kpi("Citas esta semana", semana.citas().size(), "/agenda?vista=semana"))
                .append(kpi("Visitas en curso", contar(filas(cuenta, SeccionConsulta.VISITAS), "EN_CURSO"),
                        "/visitas?estado=EN_CURSO")).append("</div><div class=\"row inicio-acciones\">")
                .append(enlace("/empresas/nueva", "Nueva empresa", true))
                .append(enlace("/usuarios/nuevo", "Nueva cuenta", false))
                .append(enlace("/usuarios?rol=EVALUADOR", "Asignar evaluador", false))
                .append(enlace("/calificaciones", "Informe de empresa", false))
                .append("</div><div class=\"inicio-columnas\"><section class=\"stack\"><h2>Equipo esta semana</h2>");
        html.append("<div class=\"chips leyenda-personas\" aria-label=\"Evaluadores\">");
        for (var persona : semana.evaluadores()) {
            html.append(TextoLista.persona(persona.nombre(), persona.color()));
        }
        html.append("</div>");
        LocalDate anterior = null;
        for (Agenda.Cita cita : semana.citas()) {
            Ficha ficha = cita.ficha();
            if (!ficha.reserva().fecha().equals(anterior)) {
                anterior = ficha.reserva().fecha();
                html.append("<h3 class=\"lista-grupo\">").append(Html.fecha(anterior)).append("</h3>");
            }
            String color = cita.color() == null ? "#C9CCD2" : cita.color();
            html.append("<a class=\"lista-fila\" href=\"/reservas/").append(ficha.reserva().id())
                    .append("\"><span class=\"persona-color\" style=\"--persona:").append(Html.escapar(color))
                    .append("\" aria-hidden=\"true\"></span><strong>").append(hora(ficha))
                    .append("</strong><span class=\"lista-identidad\"><b title=\"")
                    .append(Html.escapar(ficha.empresaNombre())).append("\">")
                    .append(Html.escapar(ficha.empresaNombre())).append("</b><small class=\"muted\">")
                    .append(Html.escapar(ficha.sedeNombre() + " · " + ficha.salonNombre())).append("</small></span>")
                    .append(TextoLista.persona(cita.nombreEvaluador(), cita.color()))
                    .append(SistemaVisual.estado(ficha.reserva().estado())).append("</a>");
        }
        html.append("</section><section class=\"stack\"><h2>Requiere atención</h2>");
        long sinAsignar = 0;
        for (Empresa empresa : companias) {
            if (empresa.activo() && empresas.vinculadas(cuenta, empresa.id()).stream()
                    .noneMatch(c -> c.rol() == Cuenta.Rol.EVALUADOR && c.estado() == Cuenta.Estado.ACTIVA)) {
                sinAsignar++;
            }
        }
        html.append(kpi("Solicitudes de acceso", empresas.accesos().pendientes(cuenta).size(), "/solicitudes-acceso"))
                .append(kpi("Empresas sin evaluador", sinAsignar, "/empresas"))
                .append(kpi("Pendientes abiertos", contar(pendientes, "ABIERTO"), "/pendientes?estado=ABIERTO"))
                .append("</section></div>");
    }

    private void representante(Cuenta cuenta, List<Empresa> companias, StringBuilder html) throws SQLException {
        List<Fila> puestos = filas(cuenta, SeccionConsulta.PUESTOS);
        List<Fila> resultados = filas(cuenta, SeccionConsulta.EVALUACIONES);
        html.append(enlace("/solicitar-visita", "Solicitar una visita", true));
        html.append("<h2>").append(companias.size() == 1 ? "Tu empresa" : "Tus empresas").append("</h2>");
        for (Empresa empresa : companias) {
            html.append("<a class=\"lista-fila\" href=\"/empresas/").append(empresa.id()).append("\"><strong>")
                    .append(Html.escapar(empresa.nombre())).append("</strong>")
                    .append(SistemaVisual.estado(empresa.activo() ? "ACTIVO" : "INACTIVO")).append("</a>");
        }
        html.append("<div class=\"inicio-indicadores\">").append(kpi("Puestos", puestos.size(), "/puestos"));
        for (String color : List.of("VERDE", "AMARILLO", "ROJO")) {
            html.append(kpi(nombreColor(color), puestos.stream().filter(p -> p.valor("color").equals(color)).count(),
                    "/puestos?color=" + color));
        }
        html.append("</div><div class=\"inicio-columnas\"><section class=\"stack\"><h2>Últimos resultados</h2>");
        resultados.stream().sorted(Comparator.comparing((Fila fila) -> fila.valor("fecha")).reversed()).limit(4)
                .forEach(fila -> html.append("<a class=\"lista-fila\" href=\"/evaluaciones/").append(fila.id())
                        .append("\"><strong>").append(Html.escapar(fila.valor("empresa_nombre")))
                        .append("</strong><span>").append(Html.escapar(fila.valor("puesto_descripcion")))
                        .append("</span>").append(riesgo(fila)).append("</a>"));
        if (resultados.isEmpty()) html.append(Html.vacio("Aún no hay resultados de visitas Finalizadas.", ""));
        html.append("</section><section class=\"stack\"><h2>Próximas visitas</h2>");
        proximas(reservas.listar(cuenta), html);
        html.append("</section></div>");
    }

    private void visitasEnCurso(Cuenta cuenta, StringBuilder html) throws SQLException {
        var actuales = visitas.listar(cuenta).stream().filter(f -> f.visita().estado().equals("EN_CURSO")).limit(2)
                .toList();
        html.append("<div class=\"row between\"><h2>Visitas en curso</h2>")
                .append(enlace("/visitas?estado=EN_CURSO", "Ver todas", false)).append("</div>");
        if (actuales.isEmpty()) {
            html.append(
                    Html.vacio("No hay una visita en curso.", enlace("/reservas", "Iniciar desde una reserva", false)));
        }
        for (var ficha : actuales) {
            var puestos = visitas.puestos(cuenta, ficha.visita().id());
            Set<Integer> evaluados = visitas.evaluaciones(cuenta, ficha.visita().id()).stream()
                    .map(evaluacion -> evaluacion.puesto())
                    .filter(id -> puestos.stream().anyMatch(puesto -> puesto.id() == id)).collect(Collectors.toSet());
            html.append("<article class=\"card stack\"><h3>").append(Html.escapar(ficha.empresaNombre()))
                    .append("</h3><p class=\"muted\">")
                    .append(Html.escapar(ficha.sedeNombre() + " · " + ficha.salonNombre())).append("</p><p>")
                    .append(evaluados.size()).append(" de ").append(puestos.size())
                    .append(" puestos evaluados</p><progress class=\"progreso-visita\" aria-label=\"Puestos " +
                            "evaluados\" max=\"").append(Math.max(1, puestos.size())).append("\" value=\"")
                    .append(evaluados.size()).append("\"></progress><ol class=\"evaluar-puestos\">");
            for (var puesto : puestos) {
                html.append("<li><span>").append(Html.escapar(puesto.descripcion())).append("</span>")
                        .append(SistemaVisual.estado(evaluados.contains(puesto.id()) ? "EVALUADO" : "POR_EVALUAR"))
                        .append("</li>");
            }
            html.append("</ol>");
            var siguiente = puestos.stream().filter(p -> !evaluados.contains(p.id())).findFirst();
            String destino = siguiente.map(
                            p -> "/evaluaciones/nueva?visita=" + ficha.visita().id() + "&puesto=" + p.id())
                    .orElse("/visitas/" + ficha.visita().id());
            html.append(enlace(destino, siguiente.isPresent() ? "Continuar evaluación" : "Abrir visita", true))
                    .append("</article>");
        }
    }

    private void proxima(Cuenta cuenta, List<Ficha> citas, StringBuilder html) throws SQLException {
        Ficha cita = futuras(citas).stream().findFirst().orElse(null);
        boolean hoy = cita != null && cita.reserva().fecha().equals(reservas.hoy());
        html.append("<section class=\"card stack cita-hoy\"><span class=\"eyebrow\">Hoy</span>");
        if (hoy) {
            html.append("<strong class=\"cita-hora\">").append(hora(cita)).append("</strong><h2>")
                    .append(Html.escapar(cita.empresaNombre())).append("</h2><p class=\"muted\">")
                    .append(Html.escapar(cita.sedeNombre() + " · " + cita.salonNombre())).append("</p>");
            String requisito = empresas.salones(cuenta, cita.sede()).stream()
                    .filter(salon -> salon.id() == cita.reserva().salon()).map(salon -> salon.descripcion())
                    .filter(texto -> texto != null && !texto.isBlank()).findFirst().orElse("");
            if (!requisito.isEmpty()) {
                boolean exigido = requisito.toLowerCase(Locale.ROOT).startsWith("se exige");
                html.append(exigido ? "<span class=\"requisito-salon\">⚠ " : "<span class=\"muted\">")
                        .append(Html.escapar(requisito)).append("</span>");
            }
            String destino = cita.visita() == null ? "/reservas/" + cita.reserva().id() : "/visitas/" + cita.visita();
            html.append("<div class=\"row\">")
                    .append(enlace(destino, cita.visita() == null ? "Iniciar visita" : "Continuar visita", true))
                    .append(enlace("/reservas/" + cita.reserva().id(), "Ver cita", false)).append("</div>");
        } else {
            html.append("<h2>Hoy no tienes citas</h2>");
            if (cita != null) {
                html.append("<p class=\"muted\">Próxima: ").append(Html.fecha(cita.reserva().fecha())).append(" · ")
                        .append(hora(cita)).append(" · ").append(Html.escapar(cita.empresaNombre())).append(" · ")
                        .append(Html.escapar(cita.sedeNombre())).append("</p>");
            }
            html.append("<div class=\"row\">").append(enlace("/reservas/nueva", "Reservar una cita", true))
                    .append(enlace("/agenda", "Ver mi agenda", false)).append("</div>");
        }
        html.append("</section>");
    }

    private List<Ficha> futuras(List<Ficha> citas) {
        return citas.stream().filter(c -> !c.reserva().fecha().isBefore(reservas.hoy()))
                .filter(c -> !List.of("CANCELADA", "FINALIZADA").contains(c.reserva().estado()))
                .sorted(Comparator.comparing((Ficha c) -> c.reserva().fecha()).thenComparing(c -> c.reserva().inicio()))
                .toList();
    }

    private void proximas(List<Ficha> citas, StringBuilder html) {
        List<Ficha> proximas = futuras(citas);
        LocalDate anterior = null;
        for (Ficha cita : proximas.stream().limit(4).toList()) {
            if (!cita.reserva().fecha().equals(anterior)) {
                anterior = cita.reserva().fecha();
                html.append("<h3 class=\"lista-grupo\">").append(Html.fecha(anterior)).append("</h3>");
            }
            html.append("<a class=\"lista-fila\" href=\"/reservas/").append(cita.reserva().id()).append("\"><strong>")
                    .append(hora(cita)).append("</strong><span>").append(Html.escapar(cita.empresaNombre()))
                    .append("</span><span class=\"muted\">").append(Html.escapar(cita.sedeNombre())).append("</span>")
                    .append(SistemaVisual.estado(cita.reserva().estado())).append("</a>");
        }
        if (proximas.isEmpty()) html.append(Html.vacio("No hay próximas citas.", ""));
        html.append(enlace("/reservas", "Ver todas · " + proximas.size(), false));
    }

    private static String hora(Ficha cita) {
        return cita.reserva().inicio().format(DateTimeFormatter.ofPattern("H:mm"));
    }

    private static long contar(List<Fila> filas, String estado) {
        return filas.stream().filter(f -> f.valor("estado").equals(estado)).count();
    }

    private static String nombreColor(String color) {
        return switch (color) {
            case "VERDE" -> "Verde";
            case "AMARILLO" -> "Amarillo";
            default -> "Rojo";
        };
    }

    private static void riesgos(List<Fila> filas, StringBuilder html) {
        html.append("<section class=\"stack riesgo-calma\"><h2 class=\"eyebrow\">Riesgo en tus empresas</h2>")
                .append("<div class=\"row\">");
        for (String color : List.of("ROJO", "AMARILLO", "VERDE")) {
            long cantidad = filas.stream().filter(f -> f.valor("color").equals(color)).count();
            html.append("<a data-color=\"").append(color).append("\" data-cantidad=\"").append(cantidad)
                    .append("\" href=\"/archivo?color=").append(color).append("\"><span class=\"riesgo-punto ")
                    .append(color.toLowerCase(Locale.ROOT)).append("\" aria-hidden=\"true\"></span>").append(cantidad)
                    .append(" ").append(Formularios.etiqueta(color)).append(" ›</a>");
        }
        html.append("</div></section>");
    }

    private static String riesgo(Fila fila) {
        if (fila.valor("puntaje").isEmpty()) return "<span class=\"muted\">Sin evaluar</span>";
        return Html.riesgo(Integer.valueOf(fila.valor("puntaje")), fila.valor("color"),
                Integer.parseInt(fila.valor("nivel")));
    }

    private static String nombreRiesgo(String color) {
        return switch (color) {
            case "VERDE" -> "Aceptable";
            case "AMARILLO" -> "En observación";
            default -> "Pendiente";
        };
    }

    private static String kpi(String etiqueta, long numero, String destino) {
        return "<a class=\"inicio-indicador\" href=\"" + Html.escapar(destino) + "\"><strong>" + numero +
                "</strong><span>" + Html.escapar(etiqueta) + "</span></a>";
    }

    private static String enlace(String destino, String texto, boolean principal) {
        return "<a class=\"btn" + (principal ? " p" : "") + "\" href=\"" + Html.escapar(destino) + "\">" +
                Html.escapar(texto) + "</a>";
    }
}
