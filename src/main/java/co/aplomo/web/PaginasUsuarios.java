package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.dao.EmpresaDao.Opcion;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-003, RNF-002: lista y formularios del Administrador con componentes del mock.
 */
public final class PaginasUsuarios {
    private PaginasUsuarios() {
    }

    /**
     * Lista cuentas sin mostrar hashes, claves ni contadores internos (RF-003, RNF-004).
     */
    public static String lista(Sesion sesion, List<Cuenta> cuentas, List<Opcion> empresas, String aviso) {
        return lista(sesion, cuentas, empresas, Map.of(), aviso);
    }

    /**
     * RF-003: presenta todas las empresas asignadas, sin hashes.
     */
    public static String lista(Sesion sesion, List<Cuenta> cuentas, List<Opcion> empresas,
            Map<Integer, List<Integer>> asignadas, String aviso) {
        Map<Integer, String> nombres = new HashMap<>();
        empresas.forEach(empresa -> nombres.put(empresa.id(), empresa.nombre()));
        StringBuilder html = new StringBuilder(Html.aviso(aviso, true));
        html.append("<section data-lista data-seccion=\"USUARIOS\"><div class=\"grid3 usuarios-roles\">");
        for (Rol rol : Rol.values()) {
            long cantidad = cuentas.stream().filter(cuenta -> cuenta.rol() == rol && cuenta.estado() != Estado.INACTIVA)
                    .count();
            html.append("<button class=\"card usuario-rol\" type=\"button\" data-rol-elegir=\"").append(rol.name())
                    .append("\" aria-pressed=\"false\"><strong>").append(cantidad).append("</strong><span>")
                    .append(switch (rol) {
                        case ADMINISTRADOR -> "Administradores";
                        case EVALUADOR -> "Evaluadores";
                        case USUARIO -> "Usuarios";
                    }).append("</span></button>");
        }
        html.append("</div><div class=\"lista-herramientas\"><input class=\"input\" type=\"search\" " +
                "data-lista-buscar aria-label=\"Buscar por nombre o correo\" placeholder=\"Buscar nombre o correo\">" +
                "<div class=\"chips\">");
        for (String[] filtro : List.of(new String[]{"ACTIVA", "Activas"}, new String[]{"INACTIVA", "Inactivas"},
                new String[]{"TODOS", "Todas"})) {
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(filtro[0])
                    .append("\" aria-pressed=\"false\">").append(filtro[1]).append("</button>");
        }
        html.append("</div><label class=\"field\">Empresa<select class=\"input\" data-empresa-usuarios>")
                .append("<option value=\"\">Todas las empresas</option>");
        for (Opcion empresa : empresas) {
            html.append(opcion(Integer.toString(empresa.id()), empresa.nombre(), false));
        }
        html.append("</select></label></div><p class=\"muted\" role=\"status\" data-lista-contador></p>")
                .append("<div class=\"lista-filas usuarios-lista\">");
        for (Cuenta cuenta : cuentas) {
            List<Integer> ids = asignadas.getOrDefault(cuenta.id(), List.of());
            String color = cuenta.colorAgenda() != null && co.aplomo.servicio.ColoresAgenda.PALETA.contains(
                    cuenta.colorAgenda()) ? cuenta.colorAgenda() : "#C9CCD2";
            String vinculadas = ids.stream().map(nombres::get).filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.joining(" · "));
            String empresasCuenta = cuenta.rol() == Rol.ADMINISTRADOR ? "Todas las empresas" : vinculadas;
            html.append("<a class=\"lista-fila\" href=\"/usuarios/").append(cuenta.id())
                    .append("/editar\" data-registro data-estado=\"").append(cuenta.estado().name())
                    .append("\" data-rol=\"").append(cuenta.rol().name()).append("\" data-empresa=\"")
                    .append(ids.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(" ")))
                    .append("\"><span class=\"usuario-avatar\" style=\"--persona:").append(color).append("\">")
                    .append(Html.escapar(cuenta.nombre().substring(0, 1)))
                    .append("</span><span class=\"lista-identidad\">").append("<strong title=\"")
                    .append(Html.escapar(cuenta.nombre())).append("\">").append(Html.escapar(cuenta.nombre()))
                    .append("</strong><small class=\"muted\">").append(Html.escapar(cuenta.correo()))
                    .append("</small></span><span>").append(Formularios.etiqueta(cuenta.rol().name()))
                    .append("</span><span class=\"muted\">").append(Html.escapar(empresasCuenta)).append("</span>")
                    .append(SistemaVisual.estado(cuenta.estado().name()))
                    .append("<span aria-hidden=\"true\">›</span></a>");
        }
        html.append("</div><p data-lista-vacia hidden>No hay cuentas con estos filtros.</p></section>")
                .append("<script src=\"/static/js/usuarios.js\" defer></script>");
        return Html.pagina("Usuarios", html.toString(), sesion, "Gestión · Usuarios",
                "<a class=\"btn p\" href=\"/usuarios/nuevo\">Nuevo usuario</a>", false);
    }

    private static String dato(String titulo, String valor) {
        return "<div><dt>" + Html.escapar(titulo) + "</dt><dd>" + Html.escapar(valor) + "</dd></div>";
    }

    private static String estado(Estado estado) {
        return switch (estado) {
            case ACTIVA -> "Activa";
            case INACTIVA -> "Inactiva";
            case BLOQUEADA -> "Bloqueada temporalmente";
        };
    }

    private static String claseEstado(Estado estado) {
        return switch (estado) {
            case ACTIVA -> "tag good";
            case INACTIVA -> "tag";
            case BLOQUEADA -> "tag warn";
        };
    }

    /**
     * Conserva los campos no secretos cuando falla la validación; una clave nunca se vuelve a mostrar. RNF-011.
     */
    public static String formulario(Sesion sesion, Cuenta cuenta, List<Opcion> empresas, Map<String, String> enviados,
            String error) {
        boolean nuevo = cuenta == null;
        Map<String, String> valores = new HashMap<>();
        if (cuenta != null) {
            valores.put("nombre", cuenta.nombre());
            valores.put("correo", cuenta.correo());
            valores.put("telefono", cuenta.telefono());
            valores.put("cargo", cuenta.cargo());
            valores.put("colorAgenda", cuenta.colorAgenda() == null ? "" : cuenta.colorAgenda());
            valores.put("rol", cuenta.rol().name());
            valores.put("empresa", cuenta.empresa() == null ? "" : cuenta.empresa().toString());
        } else valores.put("rol", "USUARIO");
        valores.putAll(enviados);
        String rol = valores.get("rol");
        StringBuilder form = new StringBuilder(Html.aviso(error, false));
        form.append("<form class=\"card stack cuenta-form\" method=\"post\" action=\"")
                .append(nuevo ? "/usuarios" : "/usuarios/" + cuenta.id() + "/editar").append("\">")
                .append(Html.csrf(sesion)).append(Html.campo("nombre", "Nombre", "text", valores.get("nombre"), 100))
                .append(Html.campo("correo", "Correo", "email", valores.get("correo"), 120))
                .append("<div class=\"grid2\">")
                .append(Html.campo("telefono", "Teléfono", "tel", valores.get("telefono"), 20))
                .append(Html.campo("cargo", "Cargo", "text", valores.get("cargo"), 80)).append("</div>")
                .append("<div class=\"field\"><label for=\"rol\">Rol</label><select class=\"input\" i" +
                        "d=\"rol\" name=\"rol\" required>")
                .append(opcion("ADMINISTRADOR", "Administrador", "ADMINISTRADOR".equals(rol)))
                .append(opcion("EVALUADOR", "Evaluador", "EVALUADOR".equals(rol)))
                .append(opcion("USUARIO", "Usuario", "USUARIO".equals(rol))).append("</select></div>");
        form.append("<label class=\"field\" data-color-agenda>Color de Agenda")
                .append("<select class=\"input\" name=\"colorAgenda\"><option value=\"\">Primero libr" + "e</option>");
        String[] nombresColor = {"Rosa intenso", "Azul", "Cian", "Violeta", "Gris perla", "Rosa suave"};
        for (int indice = 0; indice < co.aplomo.servicio.ColoresAgenda.PALETA.size(); indice++) {
            String color = co.aplomo.servicio.ColoresAgenda.PALETA.get(indice);
            form.append(opcion(color, nombresColor[indice], color.equals(valores.get("colorAgenda"))));
        }
        form.append("</select></label>");
        form.append("<p class=\"hint\">Administrador: gestiona todo. Evaluador: trabaja en las em" +
                "presas asignadas. Usuario: consulta resultados finalizados.</p><fieldset cla" +
                "ss=\"stack\" data-empresas><legend>Empresas</legend><p class=\"hint\">Usuari" +
                "o: elige al menos una. Evaluador: puede quedar sin empresas. Administrador: " + "ninguna.</p>");
        for (Opcion emp : empresas) {
            boolean elegida = "on".equals(valores.get("empresa_" + emp.id()));
            if (emp.activa() || elegida)
                form.append("<label class=\"opt\"><input type=\"checkbox\" name=\"empresa_").append(emp.id())
                        .append("\"").append(elegida ? " checked" : "").append("><span>")
                        .append(Html.escapar(emp.nombre() + (emp.activa() ? "" : " (inactiva)")))
                        .append("</span></label>");
        }
        form.append("</fieldset>");
        if (empresas.isEmpty()) form.append(Html.aviso(
                "Todavía no hay empresas registradas. Para crear un Usuario se necesita una e" + "mpresa existente.",
                false));
        String clave = Html.campo("temporal", nuevo ? "Contraseña temporal" : "Nueva contraseña temporal (opcional)",
                "password", "", 100);
        form.append(nuevo ? clave : clave.replace(" required", ""))
                .append("<p class=\"hint\">Al menos 8 caracteres, una letra y un número. Comunícala a" +
                        " la persona por un canal privado. ").append(nuevo ? "Se exigirá cambiarla al ingresar." :
                        "Deja el campo vacío para conservar la " + "contraseña actual.")
                .append("</p><div class=\"row\"><button class=\"btn p\">")
                .append(nuevo ? "Crear usuario" : "Guardar cambios")
                .append("</button><a class=\"btn\" href=\"/usuarios\">Volver a usuarios</a></div></form>");
        return Html.pagina(nuevo ? "Nuevo usuario" : "Editar usuario",
                nuevo ? Html.conPanel(form.toString(), "Cada empresa, solo lo suyo.",
                        "El Usuario consulta resultados finalizados de sus empresas; el Evaluador tra" +
                        "baja en las empresas que le asignan.") : form.toString(), sesion);
    }

    private static String opcion(String valor, String texto, boolean seleccionada) {
        return "<option value=\"" + Html.escapar(valor) + "\"" + (seleccionada ? " selected" : "") + ">" +
                Html.escapar(texto) + "</option>";
    }
}
