package co.aplomo.web;

import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Consultas.Pagina;

import java.util.List;

/**
 * RF-024: muestra los selectores principales en una fila sin un botón Aplicar filtros.
 */
final class FiltrosLista {
    private FiltrosLista() {
    }

    static String mostrar(Sesion sesion, Pagina pagina, boolean contexto) {
        StringBuilder html = new StringBuilder(
                "<form method=\"get\" class=\"filtros-linea\" data-filtros-vivos action=\"").append(
                pagina.seccion().ruta()).append("\">");
        List<String> campos = contexto ? List.of() : List.of("empresa", "sede");
        for (String clave : campos) {
            if (pagina.seccion().filtros().contains(clave)) html.append(PaginasConsultas.filtro(pagina, clave));
        }
        if (sesion.cuenta().rol() == Rol.ADMINISTRADOR &&
                pagina.seccion().filtros().contains("registros"))
            html.append(PaginasConsultas.filtro(pagina, "registros").replace(">Registros<", ">Estado<"));
        for (String clave : List.of("evaluacion", "visita", "q")) {
            if (pagina.filtros().containsKey(clave))
                html.append("<input type=\"hidden\" name=\"").append(clave).append("\" value=\"")
                        .append(Html.escapar(pagina.filtros().get(clave))).append("\">");
        }
        return html.append("</form>").toString();
    }
}
