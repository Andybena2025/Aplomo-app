package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Archivo.Datos;
import co.aplomo.servicio.Archivo.Empresa;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** RF-024, RNF-002/007: archivo compacto por empresa, sede, salón y puesto. */
public final class PaginasArchivo {
    private PaginasArchivo() {}

    /** RF-024: lista de empresas o árbol histórico con filtros visibles. */
    public static String pagina(Sesion sesion, Datos datos, Map<String, String> parametros) {
        String elegida = parametros.getOrDefault("empresa", "");
        if (elegida.isEmpty()) return empresas(sesion, datos);
        Empresa empresa = datos.empresas().stream().filter(item -> Integer.toString(item.id()).equals(elegida))
                .findFirst().orElseThrow(() -> new SecurityException("La empresa no está disponible para tu cuenta."));
        List<Fila> todas = datos.evaluaciones().stream().filter(fila -> fila.valor("empresa").equals(elegida)).toList();
        List<Fila> visibles = filtrar(todas, parametros, sesion.cuenta().rol() == Rol.ADMINISTRADOR);
        StringBuilder html = new StringBuilder("<p class=\"migas\"><a href=\"/archivo\">Archivo</a> › ")
                .append(Html.escapar(empresa.nombre())).append("</p>");
        html.append(Html.encabezado("Archivo · Empresa", empresa.nombre(), selector(datos, elegida)))
                .append("<p class=\"hint\">").append(empresa.activa() ? "Activa" : "Inactiva")
                .append(" · ").append(resumen(todas)).append("</p>").append(barra(todas, datos))
                .append(filtros(sesion, todas, parametros)).append(arbol(visibles, datos));
        return Html.pagina("Archivo", html.toString(), sesion, "Archivo", "");
    }

    private static String empresas(Sesion sesion, Datos datos) {
        StringBuilder html = new StringBuilder(Html.encabezado("Consultas · Archivo", "Archivo", ""));
        html.append("<div class=\"grid2\">");
        for (Empresa empresa : datos.empresas()) {
            List<Fila> filas = datos.evaluaciones().stream()
                    .filter(fila -> fila.valor("empresa").equals(Integer.toString(empresa.id()))).toList();
            html.append("<article class=\"card stack\"><h2><a href=\"/archivo?empresa=")
                    .append(empresa.id()).append("\">").append(Html.escapar(empresa.nombre()))
                    .append("</a></h2><span class=\"hint\">").append(empresa.activa() ? "Activa" : "Inactiva")
                    .append(" · ").append(Html.plural(filas.size(), "evaluación")).append(" · ")
                    .append(ultima(filas)).append("</span>").append(barra(filas, datos)).append("</article>");
        }
        if (datos.empresas().isEmpty()) html.append(Html.vacio("Aún no hay evaluaciones en tu Archivo.", ""));
        return Html.pagina("Archivo", html.append("</div>").toString(), sesion, false, false);
    }

    private static String selector(Datos datos, String elegida) {
        StringBuilder html = new StringBuilder("<details class=\"archivo-selector\"><summary>Cambiar empresa ▾</summary>")
                .append("<input class=\"input\" type=\"search\" aria-label=\"Buscar empresa\" data-buscar-archivo ")
                .append("placeholder=\"Buscar empresa\"><div class=\"stack\">");
        for (Empresa empresa : datos.empresas()) {
            html.append("<a data-empresa-archivo href=\"/archivo?empresa=").append(empresa.id())
                    .append("\">").append(Html.escapar(empresa.nombre())).append("</a>");
        }
        return html.append("</div></details><a class=\"btn\" href=\"/calificaciones?empresa=")
                .append(elegida).append("\">Informe</a>").toString();
    }

    private static List<Fila> filtrar(List<Fila> filas, Map<String, String> parametros, boolean administrador) {
        String buscar = parametros.getOrDefault("buscar", "").strip().toLowerCase(java.util.Locale.ROOT);
        String autor = administrador ? parametros.getOrDefault("evaluador", "") : "";
        String estado = parametros.getOrDefault("estado", "TODOS");
        return filas.stream().filter(fila -> (fila.valor("sede_nombre") + " " + fila.valor("salon_nombre")
                + " " + fila.valor("numero") + " " + fila.valor("postura")).toLowerCase(java.util.Locale.ROOT)
                .contains(buscar)).filter(fila -> autor.isBlank() || fila.valor("evaluador").equals(autor))
                .filter(fila -> estado.equals("TODOS") || fila.valor("puesto_activo")
                        .equals(estado.equals("ACTIVOS") ? "1" : "0")).toList();
    }

    private static String filtros(Sesion sesion, List<Fila> filas, Map<String, String> parametros) {
        StringBuilder html = new StringBuilder("<form class=\"archivo-filtros row\" method=\"get\">")
                .append("<input type=\"hidden\" name=\"empresa\" value=\"")
                .append(Html.escapar(parametros.get("empresa"))).append("\"><label class=\"field\">Buscar sede, salón o puesto")
                .append("<input class=\"input\" name=\"buscar\" value=\"")
                .append(Html.escapar(parametros.get("buscar"))).append("\"></label>");
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) {
            html.append("<label class=\"field\">Evaluador<select class=\"input\" name=\"evaluador\">")
                    .append("<option value=\"\">Todos</option>");
            for (Fila fila : agrupar(filas, "evaluador").values().stream().map(lista -> lista.get(0)).toList()) {
                html.append(opcion(fila.valor("evaluador"), fila.valor("evaluador_nombre"), parametros.get("evaluador")));
            }
            html.append("</select></label>");
        }
        html.append("<label class=\"field\">Estado<select class=\"input\" name=\"estado\">")
                .append(opcion("TODOS", "Activos e inactivos", parametros.get("estado")))
                .append(opcion("ACTIVOS", "Solo activos", parametros.get("estado")))
                .append(opcion("INACTIVOS", "Solo inactivos", parametros.get("estado")))
                .append("</select></label><button class=\"btn\">Aplicar filtros</button></form>")
                .append("<script src=\"/static/js/archivo.js\" defer></script>");
        return html.toString();
    }

    private static String opcion(String valor, String texto, String elegido) {
        return "<option value=\"" + Html.escapar(valor) + "\"" + (valor.equals(elegido) ? " selected" : "")
                + ">" + Html.escapar(texto) + "</option>";
    }

    private static String arbol(List<Fila> filas, Datos datos) {
        StringBuilder html = new StringBuilder("<div class=\"archivo-arbol stack\">");
        boolean primera = true;
        for (List<Fila> sede : agrupar(filas, "sede").values()) {
            html.append("<details class=\"card\"").append(primera ? " open" : "").append("><summary><b>")
                    .append(Html.escapar(sede.get(0).valor("sede_nombre"))).append("</b><span class=\"hint\">")
                    .append(agrupar(sede, "salon").size()).append(" salones · ").append(sede.size())
                    .append(" evaluaciones · ").append(ultima(sede)).append("</span></summary>")
                    .append(barra(sede, datos));
            for (List<Fila> salon : agrupar(sede, "salon").values()) {
                html.append("<details class=\"archivo-salon\"><summary>")
                        .append(Html.escapar(salon.get(0).valor("salon_nombre")))
                        .append(" · ").append(Html.plural(agrupar(salon, "puesto").size(), "puesto"))
                        .append("</summary>").append(tabla(salon)).append("</details>");
            }
            html.append("</details>");
            primera = false;
        }
        if (filas.isEmpty()) html.append(Html.vacio("No hay evaluaciones con estos filtros.", ""));
        return html.append("</div>").toString();
    }

    private static String tabla(List<Fila> filas) {
        StringBuilder html = new StringBuilder("<div class=\"tablewrap\"><table class=\"resp\"><thead><tr>")
                .append("<th>Puesto</th><th>Último resultado</th><th>Fecha</th><th>Evaluador</th>")
                .append("<th>Veces</th><th>Acciones</th></tr></thead><tbody>");
        for (List<Fila> puesto : agrupar(filas, "puesto").values()) {
            Fila ultima = ordenadas(puesto).get(0);
            html.append("<tr><td data-label=\"Puesto\">").append(Html.escapar(ultima.valor("numero")))
                    .append(" · ").append(Html.escapar(ultima.valor("postura")))
                    .append("</td><td data-label=\"Último resultado\">").append(resultado(ultima))
                    .append("</td><td data-label=\"Fecha\">").append(fecha(ultima))
                    .append("</td><td data-label=\"Evaluador\">").append(Html.escapar(ultima.valor("evaluador_nombre")))
                    .append("</td><td data-label=\"Veces\">").append(puesto.size())
                    .append("</td><td data-label=\"Acciones\"><a class=\"btn\" href=\"/puestos/")
                    .append(ultima.valor("puesto")).append("\">Ver historial ›</a></td></tr>");
        }
        return html.append("</tbody></table></div>").toString();
    }

    /** RF-024: historial de un puesto, ya filtrado por autor o empresa finalizada. */
    public static String historial(Sesion sesion, List<Fila> filas) {
        Fila puesto = ordenadas(filas).get(0);
        StringBuilder html = new StringBuilder("<p class=\"migas\"><a href=\"/archivo\">Archivo</a> › ")
                .append("<a href=\"/archivo?empresa=").append(puesto.valor("empresa")).append("\">")
                .append(Html.escapar(puesto.valor("empresa_nombre"))).append("</a> › ")
                .append(Html.escapar(puesto.valor("sede_nombre") + " › " + puesto.valor("salon_nombre")))
                .append("</p>").append(Html.encabezado("Archivo · Puesto", "Puesto " + puesto.valor("numero"), ""))
                .append("<p class=\"hint\">").append(resumen(filas)).append(" · ")
                .append(Html.escapar(puesto.valor("area_nombre") + " · " + puesto.valor("cargo_nombre")))
                .append(" · ").append(puesto.valor("puesto_activo").equals("0") ? "Inactivo" : "Activo")
                .append(" · Sede ").append(puesto.valor("sede_activa").equals("0") ? "Inactiva" : "Activa")
                .append("</p><ol class=\"archivo-tiempo\">");
        for (Fila evaluacion : ordenadas(filas)) {
            html.append("<li class=\"card row between\" data-evaluacion=\"").append(evaluacion.id())
                    .append("\"><span>").append(fecha(evaluacion)).append(" · ")
                    .append(resultado(evaluacion)).append(" · ")
                    .append(Html.escapar(evaluacion.valor("evaluador_nombre")))
                    .append(" · ").append(evaluacion.valor("estado").equals("EN_CURSO") ? "En curso" : "Finalizada")
                    .append("</span>")
                    .append("<a class=\"btn\" href=\"/evaluaciones/").append(evaluacion.id())
                    .append("\">Ver</a></li>");
        }
        return Html.pagina("Historial del puesto", html.append("</ol>").toString(), sesion, "Archivo", "");
    }

    private static String barra(List<Fila> filas, Datos datos) {
        StringBuilder html = new StringBuilder("<div class=\"archivo-barra\" aria-label=\"Semáforo de resultados\">");
        for (String color : datos.riesgos().getOrDefault("color", List.of())) {
            long cantidad = filas.stream().filter(fila -> fila.valor("color").equals(color)).count();
            if (cantidad > 0) html.append("<span class=\"riesgo-").append(color.toLowerCase(java.util.Locale.ROOT))
                    .append("\" style=\"flex:").append(cantidad).append("\">").append(cantidad)
                    .append(" · ").append(Html.escapar(color)).append("</span>");
        }
        html.append("</div><p class=\"hint\">");
        filas.stream().map(PaginasArchivo::resultado).distinct().forEach(valor -> html.append(valor).append(" · "));
        return html.append("</p>").toString();
    }

    private static String resultado(Fila fila) {
        if (fila.valor("puntaje").isBlank()) return "Pendiente";
        String color = fila.valor("color");
        if (!List.of("VERDE", "AMARILLO", "ROJO").contains(color)) color = "";
        String nombreColor = color.isEmpty() ? "" : color.charAt(0)
                + color.substring(1).toLowerCase(java.util.Locale.ROOT);
        String etiqueta = fila.valor("puntaje") + " · " + nombreColor + " · Nivel " + fila.valor("nivel");
        return "<span aria-label=\"" + Html.escapar(etiqueta) + "\" class=\"archivo-resultado riesgo-"
                + color.toLowerCase(java.util.Locale.ROOT)
                + "\"><i aria-hidden=\"true\"></i><b>" + Html.escapar(fila.valor("puntaje"))
                + "</b> · " + Html.escapar(nombreColor) + " · Nivel "
                + Html.escapar(fila.valor("nivel")) + "</span>";
    }

    private static String resumen(List<Fila> filas) {
        if (filas.isEmpty()) return "Sin evaluaciones";
        List<Fila> orden = ordenadas(filas);
        return Html.plural(filas.size(), "evaluación") + " en " + Html.plural(agrupar(filas, "puesto").size(), "puesto")
                + " · " + fecha(orden.get(orden.size() - 1)) + " a " + fecha(orden.get(0))
                + " · " + Html.plural(agrupar(filas, "evaluador").size(), "evaluador");
    }

    private static String fecha(Fila fila) {
        String fecha = fila.valor("fecha");
        return fecha.length() >= 10 ? Html.fecha(java.time.LocalDate.parse(fecha.substring(0, 10))) : "Sin fecha";
    }

    private static String ultima(List<Fila> filas) {
        return filas.isEmpty() ? "Sin fecha" : fecha(ordenadas(filas).get(0));
    }

    private static List<Fila> ordenadas(List<Fila> filas) {
        return filas.stream().sorted(Comparator.comparing((Fila fila) -> fila.valor("fecha"))
                .thenComparingInt(Fila::id).reversed()).toList();
    }

    private static Map<String, List<Fila>> agrupar(List<Fila> filas, String campo) {
        return filas.stream().collect(Collectors.groupingBy(fila -> fila.valor(campo),
                LinkedHashMap::new, Collectors.toList()));
    }
}
