package co.aplomo.web;

import co.aplomo.servicio.Empresas.Resumen;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * RF-004/024: empresas ordenadas y filtros locales combinables, dentro del alcance autorizado.
 */
final class TablaEmpresas {
    private TablaEmpresas() {
    }

    static String pagina(Sesion sesion, List<Resumen> empresas, String aviso, boolean exito,
            Map<String, String> filtros) {
        String accion = sesion.cuenta().rol() ==
                Rol.USUARIO ? "" : "<a class=\"btn p\" href=\"/empresas/nueva\">Nueva empresa</a>";
        StringBuilder html = new StringBuilder(Html.aviso(aviso, exito));
        html.append("<section data-lista data-seccion=\"EMPRESAS\" data-inicial=\"ACTIVO\" data-unidad=\"empresa\">")
                .append("<div class=\"lista-herramientas\"><label class=\"field\">Buscar" +
                        "<input class=\"input\" type=\"search\" data-lista-buscar placeholder=\"Nombre, NIT o " +
                        "ciudad\" value=\"" + Html.escapar(filtros.getOrDefault("q", "")) + "\"></label>")
                .append(selector("ciudad", "Ciudad",
                        empresas.stream().map(Resumen::ciudad).distinct().sorted().toList()))
                .append(selector("sector", "Sector",
                        empresas.stream().map(r -> r.empresa().sector()).filter(java.util.Objects::nonNull)
                                .filter(s -> !s.isBlank()).distinct().sorted().toList()))
                .append("<a class=\"btn\" data-lista-limpiar hidden href=\"/empresas\">Limpiar filtros</a></div>")
                .append(VerPor.mostrar("EVALUADOR", new String[][]{{"EVALUADOR", "Evaluador"},
                        {"PROXIMA", "Próxima visita"}, {"CIUDAD", "Ciudad"}, {"AZ", "A-Z"}}))
                .append("<nav class=\"chips\" aria-label=\"Estado\">");
        for (var estado : List.of(Map.entry("ACTIVO", "Activas"), Map.entry("INACTIVO", "Inactivas"),
                Map.entry("TODOS", "Todas"))) {
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(estado.getKey())
                    .append("\" data-etiqueta=\"").append(estado.getValue()).append("\" aria-pressed=\"false\">")
                    .append(estado.getValue()).append(" · 0</button>");
        }
        html.append("</nav>");
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) html.append(personas(empresas));
        html.append(metadatos(empresas));
        html.append("<p class=\"hint\" data-lista-contador role=\"status\"></p><div data-ver-por-grupos></div>" +
                "<div class=\"table-scroll\" data-tabla-base>" +
                "<table class=\"resp alineo-table\"><thead><tr><th scope=\"col\">Empresa</th>" +
                (sesion.cuenta().rol() == Rol.EVALUADOR ? "" : "<th scope=\"col\">Evaluador(es)</th>") +
                "<th scope=\"col\">Próxima visita</th><th scope=\"col\">Estado</th>" +
                "<th scope=\"col\"></th></tr></thead><tbody>");
        for (Resumen r : empresas.stream()
                .sorted(java.util.Comparator.comparing(x -> x.empresa().nombre(), String.CASE_INSENSITIVE_ORDER))
                .toList())
            html.append(fila(r, sesion.cuenta().rol() != Rol.EVALUADOR));
        html.append("</tbody></table></div><p data-lista-vacia hidden>No hay empresas con estos filtros.</p>" +
                ListasCortas.boton() + "</section>");
        return Html.pagina("Empresas", html.toString(), sesion, "Gestión · Empresas", accion, false);
    }

    private static String fila(Resumen r, boolean mostrarEvaluador) {
        var e = r.empresa();
        String lugar = r.ciudad() +
                (e.sector() == null || e.sector().isBlank() ? "" : (r.ciudad().isBlank() ? "" : " · ") + e.sector());
        String ids = r.equipo().stream().map(p -> Integer.toString(p.id()))
                .collect(java.util.stream.Collectors.joining(","));
        StringBuilder html = new StringBuilder("<tr data-registro=\"").append(e.id()).append("\" data-estado=\"")
                .append(e.activo() ? "ACTIVO" : "INACTIVO").append("\" data-evaluadores=\"").append(ids)
                .append("\" data-evaluador=\"").append(ids.isBlank() ? "SIN_ASIGNAR" : "").append("\" data-ciudad=\"")
                .append(Html.escapar(r.ciudad())).append("\" data-sector=\"").append(Html.escapar(e.sector()))
                 .append("\" data-proxima=\"").append(r.proximaVisita() == null ? "" : r.proximaVisita().toString())
                .append("\" data-busqueda=\"").append(Html.escapar(e.nombre() + " " + e.nit() + " " + lugar))
                .append("\" style=\"--persona:")
                .append(r.equipo().isEmpty() ? "#C9CCD2" : TextoLista.color(r.equipo().get(0).color())).append("\">")
                .append("<td data-label=\"Empresa\"><a class=\"fila-enlace\" href=\"/empresas/").append(e.id())
                .append("\"><strong title=\"").append(Html.escapar(e.nombre())).append("\">")
                .append(Html.escapar(e.nombre())).append("</strong><small>").append(Html.escapar(lugar))
                .append("</small></a></td><td data-label=\"Evaluador(es)\">");
        if (r.equipo().isEmpty()) html.append("<span class=\"muted\">Sin evaluador</span>");
        for (var persona : r.equipo()) html.append(TextoLista.persona(persona.nombre(), persona.color()));
        if (!mostrarEvaluador) {
            int inicio = html.indexOf("<td data-label=\"Evaluador(es)\">");
            html.delete(inicio, html.length());
        } else html.append("</td>");
        return html.append("<td data-label=\"Próxima visita\">").append(proxima(r))
                .append("</td><td data-label=\"Estado\">")
                .append(SistemaVisual.estado(e.activo() ? "ACTIVO" : "INACTIVO"))
                .append("</td><td aria-hidden=\"true\">›</td></tr>").toString();
    }

    /** RF-002/024: fecha de la próxima reserva que el servicio ya autorizó. */
    private static String proxima(Resumen resumen) {
        var fecha = resumen.proximaVisita();
        if (fecha == null) return "<span class=\"muted\">Sin visita programada</span>";
        String dia = fecha.toLocalDate().equals(Html.hoy().plusDays(1)) ? "Mañana" : fecha.format(
                java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM", java.util.Locale.forLanguageTag("es")));
        return "<small class=\"muted\">Próxima visita</small><strong class=\"proxima-empresa\">" +
                Html.escapar(dia + " · " + fecha.format(java.time.format.DateTimeFormatter.ofPattern("H:mm"))) +
                "</strong>";
    }

    /** RF-024: identidades públicas para agrupar; nunca contacto ni credenciales. */
    private static String metadatos(List<Resumen> empresas) {
        var personas = new TreeMap<Integer, co.aplomo.servicio.Empresas.Persona>();
        empresas.forEach(e -> e.equipo().forEach(p -> personas.put(p.id(), p)));
        StringBuilder html = new StringBuilder("<div hidden data-personas-grupo>");
        personas.forEach((id, p) -> html.append("<span data-persona-id=\"").append(id)
                .append("\" data-nombre=\"").append(Html.escapar(p.nombre())).append("\" data-color=\"")
                .append(TextoLista.color(p.color())).append("\"></span>"));
        return html.append("</div>").toString();
    }

    private static String personas(List<Resumen> empresas) {
        var equipo = new TreeMap<Integer, co.aplomo.servicio.Empresas.Persona>();
        empresas.forEach(r -> r.equipo().forEach(p -> equipo.put(p.id(), p)));
        StringBuilder html = new StringBuilder("<div class=\"chips\" aria-label=\"Evaluadores\">" +
                "<button class=\"chip\" type=\"button\" data-lista-persona=\"\">Todos los evaluadores</button>");
        equipo.forEach((id, p) -> html.append("<button class=\"chip\" type=\"button\" data-lista-persona=\"").append(id)
                .append("\">").append(TextoLista.persona(p.nombre(), p.color())).append("</button>"));
        return html.append("<button class=\"chip\" type=\"button\" data-lista-persona=\"SIN_ASIGNAR\">" +
                "Sin evaluador</button></div>").toString();
    }

    private static String selector(String clave, String etiqueta, List<String> valores) {
        StringBuilder html = new StringBuilder("<label class=\"field\">").append(etiqueta)
                .append("<select class=\"input\" data-filtro-local=\"").append(clave)
                .append("\"><option value=\"\">Todas</option>");
        for (String valor : valores)
            if (valor != null && !valor.isBlank())
                html.append("<option value=\"").append(Html.escapar(valor)).append("\">").append(Html.escapar(valor))
                        .append("</option>");
        return html.append("</select></label>").toString();
    }
}
