package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.dao.EmpresaDao.Opcion;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RF-003/024, RNF-002: cuentas con columnas y filtros, nunca hashes ni claves.
 */
final class TablaUsuarios {
    private TablaUsuarios() {
    }

    static String pagina(Sesion sesion, List<Cuenta> cuentas, List<Opcion> empresas,
            Map<Integer, List<Integer>> asignadas, String aviso) {
        var nombres = new java.util.HashMap<Integer, String>();
        empresas.forEach(e -> nombres.put(e.id(), e.nombre()));
        StringBuilder html = new StringBuilder(Html.aviso(aviso, true));
        html.append("<section data-lista data-seccion=\"USUARIOS\" data-unidad=\"cuenta\"><div class=\"chips " +
                "usuarios-roles\">");
        for (Rol rol : Rol.values()) {
            long cantidad = cuentas.stream().filter(c -> c.rol() == rol && c.estado() != Cuenta.Estado.INACTIVA)
                    .count();
            html.append("<button class=\"chip\" type=\"button\" data-rol-elegir=\"").append(rol.name())
                    .append("\" aria-pressed=\"false\"><i class=\"rol-punto\" aria-hidden=\"true\"></i>")
                    .append(Formularios.etiqueta(rol.name())).append(" · ")
                    .append(cantidad).append(" cuentas</button>");
        }
        html.append("</div><div class=\"lista-herramientas\"><label class=\"field\">Buscar" +
                "<input class=\"input\" type=\"search\" data-lista-buscar placeholder=\"Nombre o correo\"></label>" +
                "<label class=\"field\">Empresa<select class=\"input\" data-empresa-usuarios>" +
                "<option value=\"\">Todas las empresas</option>");
        for (Opcion empresa : empresas)
            html.append("<option value=\"").append(empresa.id()).append("\">").append(Html.escapar(empresa.nombre()))
                    .append("</option>");
        html.append("</select></label><a class=\"btn\" data-lista-limpiar hidden href=\"/usuarios\">Limpiar " +
                "filtros</a></div>" + "<div class=\"chips\">");
        for (var estado : List.of(Map.entry("ACTIVA", "Activas"), Map.entry("INACTIVA", "Inactivas"),
                Map.entry("TODOS", "Todas"))) {
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(estado.getKey())
                    .append("\" data-etiqueta=\"").append(estado.getValue()).append("\" aria-pressed=\"false\">")
                    .append(estado.getValue()).append(" · 0</button>");
        }
        html.append("</div><p class=\"hint\" role=\"status\" data-lista-contador></p><div class=\"table-scroll\">" +
                "<table class=\"resp alineo-table usuarios-table\"><thead><tr><th scope=\"col\">Nombre y correo</th>" +
                "<th scope=\"col\">Rol</th><th scope=\"col\">Empresa</th><th scope=\"col\">Estado</th>" +
                "<th scope=\"col\">Acciones</th></tr></thead><tbody>");
        // RNF-007: agrupadas por rol (Administrador, Evaluador, Usuario) sin cambiar el orden dentro de cada grupo.
        var ordenadas = cuentas.stream().sorted(java.util.Comparator.comparingInt(c -> c.rol().ordinal())).toList();
        for (Cuenta cuenta : ordenadas) {
            var ids = asignadas.getOrDefault(cuenta.id(), List.of());
            var nombresEmpresas = ids.stream().map(nombres::get).filter(java.util.Objects::nonNull).toList();
            String vinculadas =
                    cuenta.rol() == Rol.ADMINISTRADOR ? "Todas las empresas" : String.join(" · ", nombresEmpresas);
            String primera = cuenta.rol() ==
                    Rol.ADMINISTRADOR ? vinculadas : nombresEmpresas.isEmpty() ? "" : nombresEmpresas.get(0);
            String resumen = "<span class=\"empresas-resumen\" title=\"" + Html.escapar(vinculadas) + "\"><span>" +
                    Html.escapar(primera) + "</span>" +
                    (cuenta.rol() != Rol.ADMINISTRADOR && nombresEmpresas.size() > 1 ?
                            "<small class=\"empresas-cantidad\">+" + (nombresEmpresas.size() - 1) + "</small>" : "") +
                    "</span>";
            html.append("<tr data-registro=\"").append(cuenta.id()).append("\" data-estado=\"")
                    .append(cuenta.estado().name()).append("\" data-rol=\"").append(cuenta.rol().name())
                    .append("\" data-empresa=\"")
                    .append(ids.stream().map(Object::toString).collect(Collectors.joining(" ")))
                    .append("\" style=\"--persona:").append(TextoLista.color(cuenta.colorAgenda())).append("\">")
                    .append("<td data-label=\"Nombre y correo\"><a class=\"fila-enlace\" href=\"/usuarios/")
                    .append(cuenta.id()).append("/editar\"><strong title=\"").append(Html.escapar(cuenta.nombre()))
                    .append("\">").append(Html.escapar(cuenta.nombre())).append("</strong><small>")
                    .append(Html.escapar(cuenta.correo())).append("</small></a></td><td data-label=\"Rol\">").append(rolPill(cuenta.rol()))
                    .append("</td><td data-label=\"Empresa\">")
                    .append(resumen).append("</td><td data-label=\"Estado\">")
                    .append(SistemaVisual.estado(cuenta.estado().name())).append("</td><td data-label=\"Acciones\">")
                    .append("<a class=\"btn\" href=\"/usuarios/").append(cuenta.id())
                    .append("/editar\">Editar</a></td></tr>");
        }
        html.append("</tbody></table></div><p data-lista-vacia hidden>No hay cuentas con estos filtros.</p>" +
                ListasCortas.boton() + "</section>" + "<script src=\"/static/js/usuarios.js\" defer></script>");
        return Html.pagina("Usuarios", html.toString(), sesion, "Gestión · Usuarios",
                "<a class=\"btn p\" href=\"/usuarios/nuevo\">Nuevo usuario</a>", false);
    }

    /**
     * RF-003, RNF-007: el rol se reconoce por su color (Administrador marfil, Evaluador lila, Usuario aguamarina).
     */
    static String rolPill(Rol rol) {
        return "<span class=\"rol-pill rol-pill-" + rol.name().toLowerCase(java.util.Locale.ROOT) +
                "\"><i aria-hidden=\"true\"></i>" + Formularios.etiqueta(rol.name()) + "</span>";
    }
}
