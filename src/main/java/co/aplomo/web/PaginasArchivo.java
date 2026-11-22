package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Archivo.Datos;
import co.aplomo.servicio.Archivo.Empresa;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * RF-024, RNF-002/007: archivo por empresa con columnas y estructura real del trabajo.
 */
public final class PaginasArchivo {
    private PaginasArchivo() {
    }

    /**
     * RF-002/024: empresas y resultados ya autorizados, sin fotos ni enlaces de escritura para U.
     */
    public static String pagina(Sesion sesion, Datos datos, Map<String, String> parametros) {
        String elegida = parametros.getOrDefault("empresa", "");
        String color = parametros.getOrDefault("color", "");
        if (!color.isBlank() && !List.of("VERDE", "AMARILLO", "ROJO").contains(color)) {
            throw new IllegalArgumentException("Elige un color de resultado válido.");
        }
        if (!color.isBlank()) {
            var filtradas = datos.evaluaciones().stream().filter(f -> f.valor("color").equals(color)).toList();
            datos = new Datos(datos.empresas().stream()
                    .filter(e -> filtradas.stream().anyMatch(f -> f.valor("empresa").equals(Integer.toString(e.id()))))
                    .toList(), filtradas, datos.riesgos(), datos.empresasInforme());
        }
        if (elegida.isBlank()) return empresas(sesion, datos);
        Empresa empresa = datos.empresas().stream().filter(e -> Integer.toString(e.id()).equals(elegida)).findFirst()
                .orElseThrow(() -> new SecurityException("La empresa no está disponible para tu cuenta."));
        List<Fila> todas = datos.evaluaciones().stream().filter(f -> f.valor("empresa").equals(elegida)).toList();
        var visibles = filtrar(todas, parametros, sesion.cuenta().rol());
        String acciones = cambiarEmpresa(datos, empresa.id()) + (datos.empresasInforme().contains(empresa.id()) ?
                "<a class=\"btn p\" href=\"/informes/empresa/" + empresa.id() + "\">Informe PDF</a>" : "");
        String lugar = empresa.ciudad() + (empresa.sector() == null || empresa.sector().isBlank() ? "" :
                (empresa.ciudad().isBlank() ? "" : " · ") + empresa.sector());
        StringBuilder html = new StringBuilder("<p class=\"migas\"><a href=\"/archivo\">‹ Archivo</a> / ").append(
                        Html.escapar(empresa.nombre())).append("</p>")
                .append(Html.encabezado("Consultas · Archivo", empresa.nombre(), acciones))
                .append("<div class=\"row\">").append(SistemaVisual.estado(empresa.activa() ? "ACTIVO" : "INACTIVO"))
                .append("<span class=\"muted\">").append(Html.escapar(lugar)).append("</span></div>")
                .append(indicadores(visibles)).append(filtros(sesion, todas, parametros, empresa.id()))
                .append(arbol(sesion, visibles));
        return Html.pagina("Archivo", html.toString(), sesion, false, true);
    }

    private static String empresas(Sesion sesion, Datos datos) {
        StringBuilder html = new StringBuilder(Html.encabezado("Consultas · Archivo", "Archivo", ""));
        html.append(
                "<p class=\"muted\">Todo lo evaluado, por empresa. Toca una empresa para ver sus sedes y puestos.</p>" +
                        "<section data-lista data-seccion=\"ARCHIVO\" data-unidad=\"empresa\"><div " +
                        "class=\"lista-herramientas\">" +
                        "<label class=\"field\">Buscar<input class=\"input\" type=\"search\" data-lista-buscar " +
                        "placeholder=\"Buscar empresa\"></label><a class=\"btn\" data-lista-limpiar hidden " +
                        "href=\"/archivo\">Limpiar filtros</a></div><div class=\"chips\">");
        for (var estado : List.of(Map.entry("ACTIVO", "Activas"), Map.entry("INACTIVO", "Inactivas"),
                Map.entry("TODOS", "Todas"))) {
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(estado.getKey())
                    .append("\" data-etiqueta=\"").append(estado.getValue()).append("\" aria-pressed=\"false\">")
                    .append(estado.getValue()).append(" · 0</button>");
        }
        html.append("</div>");
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) html.append(TablasTrabajo.leyenda(datos.evaluaciones()));
        html.append("<p class=\"hint\" data-lista-contador role=\"status\"></p><div class=\"table-scroll\">" +
                "<table class=\"resp alineo-table archivo-table\"><thead><tr><th scope=\"col\">Empresa</th>");
        if (sesion.cuenta().rol() != Rol.EVALUADOR) html.append("<th scope=\"col\">Evaluador</th>");
        html.append(
                "<th scope=\"col\">Evaluaciones</th><th scope=\"col\">Resultados</th><th scope=\"col\">Estado</th>" +
                        "<th scope=\"col\"></th></tr></thead><tbody>");
        for (Empresa empresa : datos.empresas()) {
            var filas = datos.evaluaciones().stream()
                    .filter(f -> f.valor("empresa").equals(Integer.toString(empresa.id()))).toList();
            String ids = filas.stream().map(f -> f.valor("evaluador")).distinct().collect(Collectors.joining(","));
            Fila ultima = filas.isEmpty() ? null : ordenadas(filas).get(0);
            html.append("<tr data-registro=\"").append(empresa.id()).append("\" data-estado=\"")
                    .append(empresa.activa() ? "ACTIVO" : "INACTIVO").append("\" data-evaluadores=\"").append(ids)
                    .append("\" data-busqueda=\"").append(Html.escapar(empresa.nombre() + " " + empresa.ciudad()))
                    .append("\" style=\"--persona:")
                    .append(ultima == null ? "#C9CCD2" : TextoLista.color(ultima.valor("evaluador_color")))
                    .append("\"><td data-label=\"Empresa\"><a class=\"fila-enlace archivo-identidad\" " +
                            "href=\"/archivo?empresa=").append(empresa.id()).append("\"><span class=\"iniciales\">")
                    .append(Html.escapar(Html.iniciales(empresa.nombre()))).append("</span><strong title=\"")
                    .append(Html.escapar(empresa.nombre())).append("\">").append(Html.escapar(empresa.nombre()))
                    .append("</strong><small>").append(Html.escapar(empresa.ciudad() +
                            (empresa.sector() == null || empresa.sector().isBlank() ? "" : " · " + empresa.sector())))
                    .append("</small></a></td>");
            if (sesion.cuenta().rol() != Rol.EVALUADOR)
                html.append("<td data-label=\"Evaluador\">").append(personas(filas)).append("</td>");
            html.append("<td data-label=\"Evaluaciones\">").append(Html.plural(filas.size(), "evaluación"))
                    .append("<small>")
                    .append(ultima == null ? "Sin evaluaciones" : "Última " + TextoLista.fecha(ultima.valor("fecha")))
                    .append("</small></td><td data-label=\"Resultados\">").append(TextoLista.barra(filas))
                    .append("</td><td data-label=\"Estado\">")
                    .append(SistemaVisual.estado(empresa.activa() ? "ACTIVO" : "INACTIVO"))
                    .append("</td><td aria-hidden=\"true\">›</td></tr>");
        }
        html.append("</tbody></table></div><p data-lista-vacia hidden>No hay empresas con estos filtros.</p>" +
                ListasCortas.boton() + "</section>");
        return Html.pagina("Archivo", html.toString(), sesion, false, false);
    }

    private static String indicadores(List<Fila> filas) {
        String periodo = filas.isEmpty() ? "Sin evaluaciones" :
                TextoLista.fecha(ordenadas(filas).get(filas.size() - 1).valor("fecha")) + " – " +
                        TextoLista.fecha(ordenadas(filas).get(0).valor("fecha"));
        return "<div class=\"archivo-indicadores\">" +
                indicador("Evaluaciones", Html.plural(filas.size(), "evaluación")) +
                indicador("Puestos evaluados", Html.plural(agrupar(filas, "puesto").size(), "puesto")) +
                indicador("Periodo", periodo) + indicador("Evaluador(a)", personas(filas)) +
                indicador("Resultado", TextoLista.barra(filas)) + "</div>";
    }

    private static String indicador(String titulo, String contenido) {
        return "<article class=\"card\"><span class=\"eyebrow\">" + titulo + "</span><div>" + contenido +
                "</div></article>";
    }

    private static String cambiarEmpresa(Datos datos, int seleccionada) {
        StringBuilder html = new StringBuilder(
                "<details class=\"cambiar-empresa\"><summary class=\"btn\">Cambiar empresa ▾</summary>");
        for (Empresa e : datos.empresas())
            html.append("<a href=\"/archivo?empresa=").append(e.id()).append("\"")
                    .append(e.id() == seleccionada ? " aria-current=\"page\"" : "").append(">")
                    .append(Html.escapar(e.nombre())).append("</a>");
        return html.append("</details>").toString();
    }

    private static String filtros(Sesion sesion, List<Fila> filas, Map<String, String> parametros, int empresa) {
        StringBuilder html = new StringBuilder(
                "<form method=\"get\" class=\"archivo-filtros grid3\" data-auto-filtros>" +
                        "<input type=\"hidden\" name=\"empresa\" value=\"" + empresa + "\">");
        var lugares = new LinkedHashMap<String, String>();
        for (Fila f : filas) {
            lugares.put("sede:" + f.valor("sede"), f.valor("sede_nombre"));
            lugares.put("salon:" + f.valor("salon"), f.valor("sede_nombre") + " · " + f.valor("salon_nombre"));
            lugares.put("puesto:" + f.valor("puesto"),
                    f.valor("sede_nombre") + " · " + f.valor("salon_nombre") + " · " + TextoLista.puesto(f));
        }
        html.append(selector("lugar", "Lugar", lugares, parametros));
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) {
            var personas = new LinkedHashMap<String, String>();
            filas.forEach(f -> personas.put(f.valor("evaluador"), f.valor("evaluador_nombre")));
            html.append(selector("evaluador", "Evaluador", personas, parametros));
        }
        html.append(selector("estado", "Estado", Map.of("ACTIVOS", "Activos", "INACTIVOS", "Inactivos"), parametros))
                .append("<noscript><button class=\"btn\">Ver resultados</button></noscript></form>");
        return html.toString();
    }

    private static String selector(String nombre, String etiqueta, Map<String, String> opciones,
            Map<String, String> datos) {
        StringBuilder html = new StringBuilder("<label class=\"field\">").append(etiqueta)
                .append("<select class=\"input\" name=\"").append(nombre)
                .append("\"><option value=\"\">Todos</option>");
        opciones.forEach((id, titulo) -> html.append("<option value=\"").append(Html.escapar(id)).append("\"")
                .append(id.equals(datos.get(nombre)) ? " selected" : "").append(">").append(Html.escapar(titulo))
                .append("</option>"));
        return html.append("</select></label>").toString();
    }

    private static List<Fila> filtrar(List<Fila> filas, Map<String, String> datos, Rol rol) {
        String lugar = datos.getOrDefault("lugar", "");
        String persona = rol == Rol.ADMINISTRADOR ? datos.getOrDefault("evaluador", "") : "";
        String estado = datos.getOrDefault("estado", "");
        String buscar = datos.getOrDefault("buscar", "").toLowerCase(java.util.Locale.ROOT);
        if (!lugar.isBlank() && !lugar.matches("(sede|salon|puesto):[1-9][0-9]*")) {
            throw new IllegalArgumentException("Elige un lugar de esta empresa.");
        }
        return filas.stream().filter(f -> lugar.isBlank() || f.valor(lugar.split(":")[0]).equals(lugar.split(":")[1]))
                .filter(f -> persona.isBlank() || f.valor("evaluador").equals(persona))
                .filter(f -> estado.isBlank() || estado.equals("TODOS") ||
                        f.valor("puesto_activo").equals(estado.equals("ACTIVOS") ? "1" : "0"))
                .filter(f -> (TextoLista.puesto(f) + " " + f.valor("sede_nombre") + " " +
                        f.valor("salon_nombre")).toLowerCase(java.util.Locale.ROOT).contains(buscar)).toList();
    }

    private static String arbol(Sesion sesion, List<Fila> filas) {
        StringBuilder html = new StringBuilder("<div class=\"archivo-sedes stack\">");
        for (var sede : agrupar(filas, "sede").values()) {
            html.append("<section class=\"card stack\"><h2>").append(Html.escapar(sede.get(0).valor("sede_nombre")))
                    .append("</h2><p class=\"muted\">").append(Html.plural(agrupar(sede, "salon").size(), "salón"))
                    .append(" · ").append(Html.plural(agrupar(sede, "puesto").size(), "puesto")).append(" · ")
                    .append(Html.plural(sede.size(), "evaluación")).append(" · última ")
                    .append(TextoLista.fecha(ordenadas(sede).get(0).valor("fecha"))).append("</p>");
            for (var salon : agrupar(sede, "salon").values()) {
                html.append("<h3 class=\"muted\">").append(Html.escapar(salon.get(0).valor("salon_nombre")))
                        .append("</h3><div class=\"table-scroll\"><table class=\"resp alineo-table\"><thead><tr>" +
                                "<th scope=\"col\">Puesto</th><th scope=\"col\">Último resultado</th>" +
                                "<th scope=\"col\">Última evaluación</th>");
                if (sesion.cuenta().rol() != Rol.EVALUADOR) html.append("<th scope=\"col\">Evaluador</th>");
                html.append("<th scope=\"col\"></th></tr></thead><tbody>");
                for (var puesto : agrupar(salon, "puesto").values()) {
                    Fila f = ordenadas(puesto).get(0);
                    html.append("<tr data-registro=\"").append(f.id()).append("\" style=\"--persona:")
                            .append(TextoLista.color(f.valor("evaluador_color")))
                            .append("\"><td data-label=\"Puesto\">").append("<a class=\"fila-enlace\" href=\"/puestos/")
                            .append(f.valor("puesto")).append("\" title=\"Ver historial\"><strong title=\"")
                            .append(Html.escapar(TextoLista.puesto(f))).append("\">")
                            .append(Html.escapar(TextoLista.puesto(f))).append("</strong><small>Puesto ")
                            .append(Html.escapar(f.valor("numero"))).append(" · ")
                            .append(Html.plural(puesto.size(), "evaluación"))
                            .append("</small></a></td><td data-label=\"Último resultado\">").append(resultado(f))
                            .append("</td><td data-label=\"Última evaluación\">Última ")
                            .append(TextoLista.fecha(f.valor("fecha"))).append("</td>");
                    if (sesion.cuenta().rol() != Rol.EVALUADOR) html.append("<td data-label=\"Evaluador\">")
                            .append(TextoLista.persona(f.valor("evaluador_nombre"), f.valor("evaluador_color")))
                            .append("</td>");
                    html.append("<td aria-hidden=\"true\">›</td></tr>");
                }
                html.append("</tbody></table></div>");
            }
            html.append("</section>");
        }
        if (filas.isEmpty()) html.append(Html.vacio("No hay evaluaciones con estos filtros.", ""));
        return html.append("</div>").toString();
    }

    /**
     * RF-024: historial del puesto autorizado, con su descripción y cada evaluación conservada.
     */
    public static String historial(Sesion sesion, List<Fila> filas) {
        if (filas.isEmpty()) throw new SecurityException("El puesto no está disponible para tu cuenta.");
        Fila puesto = ordenadas(filas).get(0);
        StringBuilder html = new StringBuilder("<p class=\"migas\"><a href=\"/archivo?empresa=").append(
                        puesto.valor("empresa")).append("\">‹ Archivo</a></p>")
                .append(Html.encabezado("Archivo · Puesto", TextoLista.puesto(puesto), ""))
                .append("<p class=\"hint\">Historial del puesto · ")
                .append(Html.escapar(puesto.valor("area_nombre") + " · " + puesto.valor("cargo_nombre"))).append(" · ")
                .append(puesto.valor("puesto_activo").equals("0") ? "Inactivo" : "Activo").append(" · Sede ")
                .append(puesto.valor("sede_activa").equals("0") ? "Inactiva" : "Activa")
                .append("</p><ol class=\"archivo-tiempo\">");
        for (Fila f : ordenadas(filas)) {
            html.append("<li class=\"card row between\" data-evaluacion=\"").append(f.id()).append("\"><span>")
                    .append(TextoLista.fecha(f.valor("fecha"))).append(" · ").append(resultado(f)).append(" · ")
                    .append(Html.escapar(f.valor("evaluador_nombre"))).append(" · ")
                    .append(f.valor("estado").equals("EN_CURSO") ? "En curso" : "Finalizada").append("</span>")
                    .append("<a class=\"btn\" href=\"/evaluaciones/").append(f.id()).append("\">Ver</a></li>");
        }
        return Html.pagina("Historial del puesto", html.append("</ol>").toString(), sesion, false, true);
    }

    private static Map<String, List<Fila>> agrupar(List<Fila> filas, String clave) {
        return filas.stream()
                .collect(Collectors.groupingBy(f -> f.valor(clave), LinkedHashMap::new, Collectors.toList()));
    }

    private static List<Fila> ordenadas(List<Fila> filas) {
        return filas.stream().sorted(Comparator.comparing((Fila f) -> f.valor("fecha")).reversed()
                .thenComparing(Comparator.comparingInt(Fila::id).reversed())).toList();
    }

    private static String personas(List<Fila> filas) {
        var equipo = new LinkedHashMap<String, Fila>();
        filas.forEach(f -> equipo.putIfAbsent(f.valor("evaluador"), f));
        return equipo.isEmpty() ? "<span class=\"muted\">Sin evaluador</span>" : equipo.values().stream()
                                                                                 .map(f -> TextoLista.persona(
                                                                                         f.valor("evaluador_nombre"),
                                                                                         f.valor("evaluador_color")))
                                                                                 .collect(Collectors.joining(" "));
    }

    private static String resultado(Fila f) {
        return f.valor("puntaje").isBlank() ? "<span class=\"muted\">Sin resultado</span>" : SistemaVisual.riesgo(
                Integer.valueOf(f.valor("puntaje")), f.valor("color"), Integer.parseInt(f.valor("nivel")));
    }
}
