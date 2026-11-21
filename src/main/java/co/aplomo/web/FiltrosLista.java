package co.aplomo.web;

import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Consultas.Pagina;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RF-024: conserva filtros avanzados y orden sin un botón Aplicar filtros.
 */
final class FiltrosLista {
    private FiltrosLista() {
    }

    static String mostrar(Sesion sesion, Pagina pagina, boolean contexto) {
        StringBuilder html = new StringBuilder("<details class=\"filtros-extra\"><summary>Más filtros</summary>" +
                "<form method=\"get\" class=\"grid3\" data-filtros-vivos action=\"").append(pagina.seccion().ruta())
                .append("\">");
        for (String clave : pagina.seccion().filtros()) {
            if (clave.equals("estado") || clave.equals("registros") && sesion.cuenta().rol() != Rol.ADMINISTRADOR) {
                continue;
            }
            if (clave.equals("evaluador") && !pagina.catalogos().containsKey(clave)) continue;
            if (contexto && !Set.of("desde", "hasta", "registros").contains(clave)) continue;
            html.append(PaginasConsultas.filtro(pagina, clave));
        }
        var ordenes = new ArrayList<>(
                pagina.seccion().columnas().stream().filter(columna -> !contexto || !columna.clave().equals("numero"))
                        .map(columna -> new Formularios.Opcion(columna.clave(), columna.titulo())).toList());
        if (pagina.seccion() == SeccionConsulta.PENDIENTES) {
            ordenes.addAll(List.of(new Formularios.Opcion("prioridad", "Prioridad"),
                    new Formularios.Opcion("fecha_limite", "Fecha límite")));
        }
        html.append(Formularios.selector(pagina.filtros(), "orden", "Ordenar por", ordenes, true))
                .append(Formularios.selector(pagina.filtros(), "direccion", "Dirección",
                        List.of(new Formularios.Opcion("asc", "Ascendente"),
                                new Formularios.Opcion("desc", "Descendente")), true));
        for (String clave : List.of("evaluacion", "visita", "q", "estado")) {
            if (!pagina.filtros().containsKey(clave)) continue;
            html.append("<input type=\"hidden\" name=\"").append(clave).append("\" value=\"")
                    .append(Html.escapar(pagina.filtros().get(clave))).append("\">");
        }
        return html.append("<p class=\"hint\">Los filtros se actualizan al elegir una opción.</p></form></details>")
                .toString();
    }
}
