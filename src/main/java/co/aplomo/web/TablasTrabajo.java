package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Consultas.Pagina;
import co.aplomo.dao.SugerenciaDao.Contexto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-024, RNF-002/007: el mismo orden y las mismas columnas en las consultas de los tres roles.
 */
final class TablasTrabajo {
    private TablasTrabajo() {
    }

    static String pagina(Sesion sesion, Pagina pagina, Contexto contexto) {
        var seccion = pagina.seccion();
        java.util.Set<Integer> visibles = pagina.filas().stream()
                .filter(f -> inicial(f, seccion, pagina.filtros().getOrDefault("estado", "TODOS"))).map(Fila::id)
                .collect(Collectors.toSet());
        boolean evaluador = sesion.cuenta().rol() == Rol.EVALUADOR;
        String titulo = seccion == SeccionConsulta.PENDIENTES ? "Pendientes" :
                seccion == SeccionConsulta.RESERVAS && evaluador ? "Mis reservas" : seccion.titulo();
        List<Fila> filas = ordenar(pagina.universo(), seccion, evaluador);
        StringBuilder html = new StringBuilder(
                Html.encabezado((seccion == SeccionConsulta.PENDIENTES ? "Gestión · " : "Consultas · ") + titulo,
                        titulo, contexto != null && sesion.cuenta().rol() != Rol.USUARIO ?
                                "<a class=\"btn p\" href=\"/sugerencias/nueva?evaluacion=" + contexto.evaluacion() +
                                "\">Escribir sugerencia</a>" : accion(sesion, seccion)));
        if (seccion == SeccionConsulta.PENDIENTES) html.append(
                "<p class=\"muted pendientes-intro\">Aquí encontrarás los resultados " +
                        "de evaluación de alto riesgo que necesitan mejoras.</p>");
        if (contexto != null) html.append(PaginasConsultas.contexto(contexto, pagina.contextoEvaluacion(),
                sesion.cuenta().rol() == Rol.ADMINISTRADOR));
        html.append("<section class=\"trabajo-lista" + (contexto != null ? " sugerencias-contexto" : "") +
                        "\" data-lista data-seccion=\"").append(seccion.name()).append("\" data-inicial=\"")
                .append(Html.escapar(pagina.filtros().getOrDefault("estado", ""))).append("\" data-unidad=\"")
                .append(Html.escapar(seccion.singular())).append("\">")
                .append(seccion == SeccionConsulta.VISITAS ? MesesVisitas.conteos(filas) : "")
                .append(herramientas(sesion, pagina, filas, contexto != null));
        if (seccion == SeccionConsulta.RESERVAS && evaluador) html.append(proxima(filas));
        if (seccion == SeccionConsulta.VISITAS) html.append(enCurso(sesion, filas));
        boolean lateral = seccion == SeccionConsulta.RESERVAS && sesion.cuenta().rol() != Rol.USUARIO;
        if (lateral) html.append("<div class=\"trabajo-con-lateral\"><div>");
        if (seccion == SeccionConsulta.RESERVAS) {
            if (evaluador) html.append("<section data-lista-grupo class=\"respuestas-reservas\">" +
                            "<h2>Necesitan tu respuesta · <span data-respuestas-contador></span></h2>")
                    .append(tabla(sesion, seccion, visibles,
                            filas.stream().filter(f -> f.valor("estado").equals("ESPERANDO")).toList()))
                    .append("</section><h2 class=\"proximas-titulo\">Próximas</h2>");
            int destacada = evaluador ? filas.stream()
                                        .filter(f -> List.of("EN_COLA", "EN_CURSO").contains(f.valor("estado")) &&
                                                     f.valor("fecha").compareTo(Html.hoy().toString()) >= 0)
                                        .mapToInt(Fila::id).findFirst().orElse(0) : 0;
            var grupos = filas.stream().filter(f -> !evaluador || !f.valor("estado").equals("ESPERANDO")).collect(
                    Collectors.groupingBy(f -> f.valor("fecha").substring(0, 10), LinkedHashMap::new,
                            Collectors.toList()));
            html.append(dias(sesion, grupos, visibles, destacada));
            if (evaluador) html.append("<details class=\"historial-reservas\"><summary>Ver historial</summary>" +
                    "<div class=\"chips\"><button class=\"chip\" type=\"button\" data-abrir-historial=\"PASADAS\">" +
                    "Pasadas</button><button class=\"chip\" type=\"button\" " +
                    "data-abrir-historial=\"CANCELADA\">Canceladas</button></div></details>");
        } else if (seccion == SeccionConsulta.VISITAS) html.append(MesesVisitas.mostrar(sesion, filas));
        else html.append(tabla(sesion, seccion, visibles, filas,
                seccion == SeccionConsulta.EVALUACIONES && pagina.filtros().containsKey("visita")));
        html.append("<p class=\"vacio-lista\" data-lista-vacia hidden>No hay resultados con estos filtros.</p>");
        if (lateral) html.append("</div>").append(PanelAgenda.mostrar(sesion, filas)).append("</div>");
        html.append(ListasCortas.boton()).append("</section>");
        String contenido = html.toString();
        if (contexto != null) contenido = contenido.replace("<th scope=\"col\">Puesto</th>", "")
                .replaceAll("<td data-puesto data-label=\"Puesto\">.*?</td>", "");
        return Html.pagina(titulo, contenido, sesion, false, false);
    }

    private static String tabla(Sesion sesion, SeccionConsulta seccion, java.util.Set<Integer> visibles,
            List<Fila> filas) {
        return tabla(sesion, seccion, visibles, filas, false);
    }

    private static String tabla(Sesion sesion, SeccionConsulta seccion, java.util.Set<Integer> visibles,
            List<Fila> filas, boolean posturas) {
        StringBuilder html = new StringBuilder(
                "<div class=\"table-scroll\"><table class=\"resp alineo-table consultas-table\">").append(
                FilasTrabajo.cabecera(seccion, sesion.cuenta().rol())).append("<tbody>");
        for (Fila fila : filas) {
            if (seccion == SeccionConsulta.VISITAS && !fila.valor("estado").equals("FINALIZADA")) continue;
            html.append(FilasTrabajo.fila(sesion, fila, seccion, visibles.contains(fila.id()), posturas));
        }
        return html.append("</tbody></table></div>").toString().replace("<th scope=\"col\">Puesto</th>",
                "<th scope=\"col\">" + (posturas ? "Postura" : "Puesto") + "</th>");
    }

    private static String herramientas(Sesion sesion, Pagina pagina, List<Fila> filas, boolean contexto) {
        StringBuilder html = new StringBuilder("<div class=\"lista-herramientas\"><label class=\"field\">" +
                "Buscar<input class=\"input\" type=\"search\" data-lista-buscar placeholder=\"Nombre o lugar\" " +
                "value=\"").append(Html.escapar(pagina.filtros().getOrDefault("q", ""))).append("\"></label>")
                .append(FiltrosLista.mostrar(sesion, pagina, contexto));
        if (pagina.seccion() == SeccionConsulta.PENDIENTES && sesion.cuenta().rol() != Rol.ADMINISTRADOR) {
            html.append("<label class=\"field\">Estado<select class=\"input\" data-incluir-estado>" +
                    "<option value=\"\">Empresas activas</option><option value=\"TODOS\">Incluir inactivas</option>" +
                    "<option value=\"INACTIVAS\">Solo inactivas</option></select></label>");
        }
        html.append("<a class=\"btn\" data-lista-limpiar hidden href=\"").append(pagina.seccion().ruta())
                .append(contexto ? "?evaluacion=" + pagina.filtros().getOrDefault("evaluacion", "") : "")
                .append("\">Limpiar filtros</a></div><div class=\"fila-chips\">").append(chips(pagina.seccion()));
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) html.append(leyenda(filas));
        return html.append("</div><p class=\"hint\" data-lista-contador role=\"status\"" +
                        (pagina.seccion() == SeccionConsulta.VISITAS ? " hidden" : "") + ">")
                .append(Html.plural(pagina.filas().size(), pagina.seccion().singular())).append("</p>").toString();
    }

    private static boolean inicial(Fila f, SeccionConsulta seccion, String estado) {
        if (!estado.equals("TODOS")) return true;
        if (seccion == SeccionConsulta.VISITAS)
            return f.valor("fecha").startsWith(Html.hoy().toString().substring(0, 7));
        if (seccion != SeccionConsulta.RESERVAS) return true;
        return List.of("EN_COLA", "EN_CURSO").contains(FilasTrabajo.estado(f)) &&
                f.valor("fecha").compareTo(Html.hoy().toString()) >= 0 &&
                f.valor("fecha").compareTo(Html.hoy().plusDays(14).toString()) <= 0;
    }

    private static String chips(SeccionConsulta seccion) {
        String[][] opciones = switch (seccion) {
            case RESERVAS ->
                    new String[][]{{"ESPERANDO", "Por confirmar"}, {"PROXIMAS", "Próximas"}, {"PASADAS", "Pasadas"},
                            {"CANCELADA", "Canceladas"}};
            case VISITAS ->
                    new String[][]{{"ESTE_MES", "Este mes"}, {"MES_ANTERIOR", "Mes anterior"}, {"TODOS", "Todas"}, {
                        "ROJO", "Con puestos en rojo"}};
            case PENDIENTES -> new String[][]{{"ABIERTO", "Abiertos"}, {"ARREGLADO", "Arreglados"}, {"TODOS", "Todos"}};
            case SUGERENCIAS ->
                    new String[][]{{"PENDIENTE", "Pendientes"}, {"EN_PROCESO", "En proceso"}, {"HECHA", "Hechas"}, {
                        "TODOS", "Todas"}};
            case EVALUACIONES ->
                    new String[][]{{"TODOS", "Todas"}, {"EN_CURSO", "En curso"}, {"FINALIZADA", "Finalizadas"}};
            default -> new String[][]{{"ACTIVO", "Activas"}, {"INACTIVO", "Inactivas"}, {"TODOS", "Todas"}};
        };
        StringBuilder html = new StringBuilder("<nav class=\"chips\" aria-label=\"Estados\">");
        for (var opcion : opciones)
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(opcion[0])
                    .append("\" data-etiqueta=\"").append(opcion[1]).append("\" aria-pressed=\"false\">")
                    .append(opcion[1]).append(" · 0</button>");
        return html.append("</nav>").toString();
    }

    static String leyenda(List<Fila> filas) {
        var personas = filas.stream().filter(f -> !f.valor("evaluador").isBlank())
                .collect(Collectors.toMap(f -> f.valor("evaluador"), f -> f, (a, b) -> a, LinkedHashMap::new));
        if (personas.isEmpty()) return "";
        StringBuilder html = new StringBuilder("<div class=\"chips leyenda-personas\" aria-label=\"Evaluadores\">");
        html.append(
                "<button type=\"button\" class=\"chip\" data-lista-persona=\"\" aria-pressed=\"true\">Todos</button>");
        personas.forEach((id, fila) -> html.append("<button type=\"button\" class=\"chip\" data-lista-persona=\"")
                .append(Html.escapar(id)).append("\" aria-pressed=\"false\">")
                .append(TextoLista.persona(fila.valor("evaluador_nombre"), fila.valor("evaluador_color")))
                .append("</button>"));
        return html.append("</div>").toString();
    }

    private static List<Fila> ordenar(List<Fila> filas, SeccionConsulta seccion, boolean evaluador) {
        Comparator<Fila> orden = Comparator.comparing(f -> f.valor("fecha"));
        if (seccion == SeccionConsulta.PENDIENTES) {
            orden = Comparator.comparing(
                    (Fila f) -> f.valor("fecha_limite").isBlank() ? "9999" : f.valor("fecha_limite")).thenComparing(
                    Comparator.comparingInt((Fila f) -> Integer.parseInt(f.valor("puntaje"))).reversed());
        }
        if (seccion == SeccionConsulta.RESERVAS) orden = orden.thenComparing(f -> f.valor("inicio"));
        return filas.stream().sorted(orden).toList();
    }

    private static String proxima(List<Fila> filas) {
        var proxima = filas.stream().filter(f -> List.of("EN_COLA", "EN_CURSO").contains(f.valor("estado")) &&
                f.valor("fecha").compareTo(Html.hoy().toString()) >= 0).findFirst();
        if (proxima.isEmpty()) return "";
        Fila f = proxima.get();
        long dias = java.time.temporal.ChronoUnit.DAYS.between(Html.hoy(), LocalDate.parse(f.valor("fecha")));
        String cuando = dias == 0 ? "hoy" : "en " + dias + (dias == 1 ? " día" : " días");
        String mapa = f.valor("direccion").isBlank() ? "" : "<a class=\"btn\" target=\"_blank\" rel=\"noopener\" " +
                                                            "href=\"https://www.google.com/maps/search/?api=1&amp;" +
                                                            "query=" +
                java.net.URLEncoder.encode(f.valor("direccion"), java.nio.charset.StandardCharsets.UTF_8) +
                                                            "\">Cómo llegar</a>";
        return "<article class=\"card proxima-cita stack\" style=\"--persona:" +
                TextoLista.color(f.valor("evaluador_color")) + "\"><h2>Tu próxima cita · " + cuando +
                "</h2><p class=\"fecha-grande\">" + TextoLista.fecha(f.valor("fecha")) + " · " +
                TextoLista.hora(f.valor("inicio")) + "</p><p>" + Html.escapar(
                f.valor("empresa_nombre") + " · " + f.valor("sede_nombre") + " · " + f.valor("salon_nombre")) +
                "</p><div class=\"row\">" + mapa + "<a class=\"btn\" href=\"/reservas/" + f.id() +
                "\">Ver detalles</a></div></article>";
    }

    private static String enCurso(Sesion sesion, List<Fila> filas) {
        var abiertas = filas.stream().filter(f -> f.valor("estado").equals("EN_CURSO")).toList();
        StringBuilder html = new StringBuilder("<section class=\"visitas-destacadas\" id=\"en-curso\">" +
                (abiertas.isEmpty() ? "" : "<h2>En curso</h2>"));
        int indice = 0;
        for (Fila f : abiertas) {
            int total = Integer.parseInt(f.valor("puestos_total"));
            int hechos = Integer.parseInt(f.valor("puestos_evaluados"));
            html.append("<article class=\"visita-en-curso\" data-visita-curso data-estado=\"EN_CURSO\" ")
                    .append("data-fecha=\"").append(Html.escapar(f.valor("fecha")))
                    .append("\" data-nivel=\"").append(Html.escapar(f.valor("nivel")))
                    .append("\" data-puntaje=\"").append(Html.escapar(f.valor("puntaje")))
                    .append("\" data-color=\"").append(Html.escapar(f.valor("color")))
                    .append("\" data-evaluador=\"").append(Html.escapar(f.valor("evaluador")))
                    .append("\" data-busqueda=\"").append(Html.escapar(f.valor("empresa_nombre") + " " +
                            f.valor("sede_nombre") + " " + f.valor("salon_nombre")))
                    .append("\" data-en-curso").append(indice++ < 3 ? "" : " hidden")
                    .append("><div><strong title=\"").append(Html.escapar(f.valor("empresa_nombre"))).append("\">")
                    .append(Html.escapar(f.valor("empresa_nombre"))).append("</strong><small>")
                    .append(Html.escapar(f.valor("sede_nombre") + " · " + f.valor("salon_nombre")))
                    .append(" · desde las ").append(TextoLista.hora(f.valor("fecha"))).append("</small></div><span>")
                    .append(hechos).append(" de ").append(total).append(" puestos</span>").append("<progress max=\"")
                    .append(Math.max(1, total)).append("\" value=\"").append(hechos)
                    .append("\" aria-label=\"Avance de puestos\"></progress><a class=\"btn p\" href=\"/visitas/")
                    .append(f.id()).append("\">")
                    .append(sesion.cuenta().rol() == Rol.EVALUADOR ? "Continuar evaluación ›" : "Ver visita")
                    .append("</a></article>");
        }
        if (abiertas.size() > 3)
            html.append("<button class=\"btn\" type=\"button\" data-ver-curso>" + "Ver todas las en curso (")
                    .append(abiertas.size()).append(")</button>");
        return html.append("</section>").toString();
    }

    /** RF-024, RNF-007: cada fecha se presenta como una tarjeta independiente. */
    private static String dias(Sesion sesion, Map<String, List<Fila>> grupos,
            java.util.Set<Integer> visibles, int destacada) {
        StringBuilder html = new StringBuilder("<div class=\"reservas-dias\">");
        var idioma = java.util.Locale.forLanguageTag("es-CO");
        for (var grupo : grupos.entrySet()) {
            LocalDate fecha = LocalDate.parse(grupo.getKey());
            boolean hoy = fecha.equals(Html.hoy());
            String etiqueta = hoy ? "Hoy" : fecha.equals(Html.hoy().plusDays(1)) ? "Mañana" : "";
            String titulo = fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", idioma));
            titulo = titulo.substring(0, 1).toUpperCase(idioma) + titulo.substring(1);
            html.append("<section data-lista-grupo data-dia-reservas class=\"dia-tarjeta")
                    .append(hoy ? " es-hoy" : "").append("\"><header class=\"dia-cabecera\">")
                    .append("<div class=\"dia-cuadro\"><small>")
                    .append(fecha.format(DateTimeFormatter.ofPattern("EEE", idioma)).toUpperCase(idioma))
                    .append("</small><strong>").append(fecha.getDayOfMonth()).append("</strong></div><h2>")
                    .append(Html.escapar(titulo)).append("</h2>");
            if (!etiqueta.isEmpty()) html.append("<span class=\"dia-etiqueta\">").append(etiqueta).append("</span>");
            html.append("<span data-dia-contador>").append(Html.plural(grupo.getValue().size(), "reserva"))
                    .append("</span></header><div class=\"table-scroll\"><table class=\"resp alineo-table ")
                    .append("consultas-table\">").append(FilasTrabajo.cabecera(SeccionConsulta.RESERVAS,
                            sesion.cuenta().rol())).append("<tbody>");
            for (Fila fila : grupo.getValue()) {
                String registro = FilasTrabajo.fila(sesion, fila, SeccionConsulta.RESERVAS,
                        visibles.contains(fila.id()) && fila.id() != destacada);
                if (fila.id() == destacada) registro = registro.replace("<tr ", "<tr data-destacada=\"true\" ");
                html.append(registro);
            }
            html.append("</tbody></table></div></section>");
        }
        return html.append("</div>").toString();
    }

    private static String accion(Sesion sesion, SeccionConsulta seccion) {
        if (seccion == SeccionConsulta.RESERVAS) return sesion.cuenta().rol() ==
                Rol.EVALUADOR ? "<a class=\"btn p\" href=\"/reservas/nueva\">Reservar una cita</a>" :
                sesion.cuenta().rol() ==
                        Rol.USUARIO ? "<a class=\"btn p\" href=\"/solicitar-visita\">Solicitar una visita</a>" : "";
        return seccion == SeccionConsulta.PENDIENTES ? "<a class=\"btn\" href=\"/sugerencias\">Sugerencias</a>" : "";
    }
}
