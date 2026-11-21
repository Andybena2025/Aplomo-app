package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Consultas.Pagina;
import co.aplomo.dao.SugerenciaDao.Contexto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-024, RNF-002/007: listas del sistema visual, con una acción clara por fila.
 */
public final class ListasConsulta {
    private ListasConsulta() {
    }

    /**
     * RF-002/024: solo recibe filas autorizadas por SQL; los chips no conceden permisos.
     */
    public static String pagina(Sesion sesion, Pagina pagina, Contexto contexto) {
        SeccionConsulta seccion = pagina.seccion();
        List<Fila> filas = ordenar(pagina.universo(), seccion);
        String titulo = seccion == SeccionConsulta.PENDIENTES ? "Pendientes" : seccion.titulo();
        String accion = contexto != null && sesion.cuenta().rol() != Rol.USUARIO ?
                "<a class=\"btn p\" href=\"/sugerencias/nueva?evaluacion=" + contexto.evaluacion() +
                "\">Escribir sugerencia</a>" : accion(sesion, seccion);
        StringBuilder html = new StringBuilder(Html.encabezado("Gestión · " + titulo, titulo, accion));
        if (contexto != null) html.append(PaginasConsultas.contexto(contexto, pagina.contextoEvaluacion(),
                sesion.cuenta().rol() == Rol.ADMINISTRADOR));
        html.append("<section class=\"consulta-lista\" data-lista data-seccion=\"").append(seccion.name())
                .append("\" data-inicial=\"").append(Html.escapar(pagina.filtros().getOrDefault("estado", "")))
                .append("\"><label class=\"buscar-lista\"><span>Buscar</span>")
                .append("<input class=\"input\" type=\"search\" data-lista-buscar value=\"")
                .append(Html.escapar(pagina.filtros().getOrDefault("q", "")))
                .append("\" placeholder=\"Buscar por nombre o lugar\"></label>").append(chips(seccion, filas))
                .append(FiltrosLista.mostrar(sesion, pagina, contexto != null))
                .append("<p class=\"hint\" data-lista-contador role=\"status\">")
                .append(Html.plural(pagina.filas().size(), seccion.singular())).append("</p>");
        var visibles = pagina.filas().stream().map(Fila::id).collect(Collectors.toSet());
        Map<String, List<Fila>> grupos = filas.stream()
                .collect(Collectors.groupingBy(fila -> grupo(fila, seccion), LinkedHashMap::new, Collectors.toList()));
        for (var grupo : grupos.entrySet()) {
            html.append("<section data-lista-grupo>")
                    .append(SistemaVisual.grupo(grupo.getKey(), grupo.getValue().size()))
                    .append("<div class=\"lista\">");
            grupo.getValue().forEach(fila -> html.append(
                    fila(fila, seccion, !visibles.contains(fila.id()), contexto != null,
                            sesion.cuenta().rol() == Rol.ADMINISTRADOR)));
            html.append("</div></section>");
        }
        html.append(
                "<p class=\"vacio-lista\" data-lista-vacia hidden>No hay resultados con estos filtros.</p></section>");
        return Html.pagina(titulo, html.toString(), sesion, false, false);
    }

    private static String accion(Sesion sesion, SeccionConsulta seccion) {
        boolean evaluador = sesion.cuenta().rol() == Rol.EVALUADOR;
        boolean administrador = sesion.cuenta().rol() == Rol.ADMINISTRADOR;
        if (seccion == SeccionConsulta.EMPRESAS && (evaluador || administrador)) {
            return "<a class=\"btn p\" href=\"/empresas/nueva\">Nueva empresa</a>";
        }
        if (seccion == SeccionConsulta.RESERVAS && sesion.cuenta().rol() == Rol.USUARIO) {
            return "<a class=\"btn p\" href=\"/solicitar-visita\">Solicitar una visita</a>";
        }
        if (seccion == SeccionConsulta.RESERVAS && evaluador) {
            return "<a class=\"btn p\" href=\"/reservas/nueva\">Nueva reserva</a>";
        }
        if (seccion == SeccionConsulta.PENDIENTES) {
            return "<a class=\"btn\" href=\"/sugerencias\">Sugerencias</a>";
        }
        return "";
    }

    private static String chips(SeccionConsulta seccion, List<Fila> filas) {
        String[][] opciones = switch (seccion) {
            case RESERVAS ->
                    new String[][]{{"PROXIMAS", "Próximas"}, {"ESPERANDO", "Por confirmar"}, {"EN_COLA", "Aceptadas"}
                            , {"EN_CURSO", "En curso"}, {"FINALIZADA", "Finalizadas"}, {"CANCELADA", "Canceladas"}};
            case VISITAS -> new String[][]{{"EN_CURSO", "En curso"}, {"FINALIZADA", "Finalizadas"}, {"TODOS", "Todas"}};
            case PENDIENTES ->
                    new String[][]{{"ABIERTOS", "Abiertos"}, {"ABIERTO", "Abierto"}, {"EN_PROCESO", "En proceso"}, {
                        "ARREGLADO", "Resueltos"}, {"TODOS", "Todos"}};
            default -> new String[][]{{"ACTIVO", "Activas"}, {"INACTIVO", "Inactivas"}, {"TODOS", "Todas"}};
        };
        if (seccion == SeccionConsulta.EVALUACIONES) {
            opciones = new String[][]{{"EN_CURSO", "En curso"}, {"FINALIZADA", "Finalizadas"}, {"TODOS", "Todas"}};
        }
        if (seccion == SeccionConsulta.SUGERENCIAS) {
            opciones = new String[][]{{"PENDIENTE", "Pendientes"}, {"EN_PROCESO", "En proceso"}, {"HECHA", "Hechas"},
                    {"TODOS", "Todas"}};
        }
        StringBuilder html = new StringBuilder("<nav class=\"chips\" aria-label=\"Filtrar resultados\">");
        for (String[] opcion : opciones) {
            long cantidad = filas.stream().filter(fila -> coincide(fila, opcion[0])).count();
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(opcion[0])
                    .append("\" aria-pressed=\"false\">").append(opcion[1]).append(" · ").append(cantidad)
                    .append("</button>");
        }
        return html.append("</nav>").toString();
    }

    private static boolean coincide(Fila fila, String filtro) {
        String estado = estado(fila);
        if (filtro.equals("TODOS")) return true;
        if (filtro.equals("ABIERTOS")) return !List.of("ARREGLADO", "HECHA").contains(estado);
        if (filtro.equals("PROXIMAS")) {
            return !List.of("FINALIZADA", "CANCELADA").contains(estado) && !fila.valor("fecha").isBlank() &&
                    !fecha(fila).isBefore(Html.hoy());
        }
        return filtro.equals(estado);
    }

    private static String estado(Fila fila) {
        return fila.valor("visita_estado").equals("EN_CURSO") ? "EN_CURSO" : fila.valor("estado");
    }

    private static List<Fila> ordenar(List<Fila> filas, SeccionConsulta seccion) {
        if (seccion == SeccionConsulta.PENDIENTES) {
            return filas.stream()
                    .sorted(Comparator.comparingInt((Fila fila) -> entero(fila.valor("puntaje"))).reversed()
                            .thenComparing(fila -> fila.valor("fecha"))).toList();
        }
        if (seccion == SeccionConsulta.RESERVAS || seccion == SeccionConsulta.VISITAS) {
            return filas.stream().sorted(Comparator.comparing((Fila fila) -> fila.valor("fecha"))
                    .thenComparing(fila -> fila.valor("inicio"))).toList();
        }
        return filas;
    }

    private static String grupo(Fila fila, SeccionConsulta seccion) {
        if (seccion == SeccionConsulta.RESERVAS || seccion == SeccionConsulta.VISITAS) {
            if (fila.valor("fecha").isBlank()) return "Sin fecha";
            return fecha(fila).format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.forLanguageTag("es-CO")))
                    .toUpperCase(Locale.ROOT);
        }
        return fila.valor("empresa_nombre").isBlank() ? "Empresas" : fila.valor("empresa_nombre");
    }

    private static String fila(Fila fila, SeccionConsulta seccion, boolean oculta, boolean contexto, boolean admin) {
        boolean cita = seccion == SeccionConsulta.RESERVAS || seccion == SeccionConsulta.VISITAS;
        String nombre = cita ? fila.valor("empresa_nombre") :
                seccion == SeccionConsulta.PENDIENTES ? "Puesto " + fila.valor("numero") : fila.valor("nombre");
        if (seccion == SeccionConsulta.EVALUACIONES) nombre = "Puesto " + fila.valor("numero");
        if (seccion == SeccionConsulta.PUESTOS) nombre = "Puesto " + fila.valor("numero");
        String ubicacion = String.join(" · ", List.of(fila.valor("sede_nombre"), fila.valor("salon_nombre")).stream()
                .filter(valor -> !valor.isBlank()).toList());
        StringBuilder html = new StringBuilder("<a class=\"lista-fila\"").append(oculta ? " hidden" : "")
                .append(" data-registro=\"").append(fila.id()).append("\" data-estado=\"")
                .append(Html.escapar(estado(fila))).append("\" data-fecha=\"").append(Html.escapar(fila.valor("fecha")))
                .append("\" data-busqueda=\"").append(Html.escapar(
                        String.join(" ", nombre, fila.valor("empresa_nombre"), ubicacion, fila.valor("descripcion"))))
                .append("\" href=\"").append(seccion.ruta()).append("/").append(fila.id()).append("\">");
        if (cita) html.append("<span class=\"lista-hora\">").append(Html.escapar(hora(fila))).append("<small>")
                .append(fila.valor("fin").isBlank() ? "" : "a " + Html.escapar(fila.valor("fin")))
                .append("</small></span>");
        if (!cita && !contexto && seccion == SeccionConsulta.SUGERENCIAS) {
            html.append("<span class=\"muted\" data-puesto>Puesto ").append(Html.escapar(fila.valor("numero")))
                    .append("</span>");
        }
        if (seccion == SeccionConsulta.PUESTOS && fila.valor("puntaje").isBlank()) {
            html.append("<span class=\"muted\">Sin resultado</span>");
        }
        if (admin) {
            for (String nivel : List.of("empresa", "sede", "salon", "puesto")) {
                String campo = nivel + (nivel.equals("empresa") || nivel.equals("sede") ? "_activa" : "_activo");
                if (fila.valor(campo).equals("0")) {
                    String etiqueta = nivel.equals("empresa") || nivel.equals("sede") ? "Inactiva" : "Inactivo";
                    html.append("<small class=\"registro-inactivo\">").append(Html.escapar(Formularios.etiqueta(nivel)))
                            .append(" · <span>").append(etiqueta).append("</span></small>");
                }
            }
        }
        html.append("<span class=\"lista-nombre\"><strong title=\"").append(Html.escapar(nombre)).append("\">")
                .append(Html.escapar(nombre)).append("</strong><small>").append(Html.escapar(ubicacion))
                .append("</small></span>");
        if (!fila.valor("puntaje").isBlank()) {
            html.append(SistemaVisual.riesgo(entero(fila.valor("puntaje")), fila.valor("color"),
                    entero(fila.valor("nivel"))));
        }
        if (seccion == SeccionConsulta.PENDIENTES) html.append(seguimiento(fila));
        return html.append(SistemaVisual.estado(estado(fila)))
                .append("<span class=\"lista-ir\" aria-hidden=\"true\">›</span></a>").toString();
    }

    private static String seguimiento(Fila fila) {
        String prioridad = fila.valor("prioridad");
        String barras = prioridad.equals("ALTA") ? "▮▮▮" : prioridad.equals("MEDIA") ? "▮▮▯" : "▮▯▯";
        String limite = fila.valor("fecha_limite").isBlank() ? "Sin fecha" : "Vence " + fila.valor("fecha_limite");
        return "<span class=\"lista-seguimiento\"><span aria-label=\"" + Html.escapar(prioridad) + "\">" + barras +
                "</span><small>" + Html.escapar(limite) + "</small></span>";
    }

    private static LocalDate fecha(Fila fila) {
        return LocalDate.parse(fila.valor("fecha").substring(0, 10));
    }

    private static String hora(Fila fila) {
        String valor = fila.valor("inicio");
        if (valor.isBlank() && fila.valor("fecha").length() > 10) valor = fila.valor("fecha").substring(11, 16);
        if (valor.length() >= 5) valor = valor.substring(0, 5).replaceFirst("^0", "");
        return valor;
    }

    private static int entero(String valor) {
        return valor.isBlank() ? 0 : Integer.parseInt(valor);
    }
}
