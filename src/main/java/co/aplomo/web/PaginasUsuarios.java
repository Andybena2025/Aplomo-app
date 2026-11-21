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
        StringBuilder contenido = new StringBuilder(Html.aviso(aviso, true));
        contenido.append("<form class=\"card usuarios-filtros\" data-filtros-usuarios role=\"search\">" +
                        "<div class=\"field\"><label for=\"usuarios-buscar\">Buscar</label><input cla" +
                        "ss=\"input\" id=\"usuarios-buscar\" type=\"search\" name=\"q\" aria-label=\"" +
                        "Buscar por nombre o correo\"></div>")
                .append("<div class=\"field\"><label for=\"usuarios-rol\">Rol</label><select class=\"" +
                        "input\" id=\"usuarios-rol\" name=\"rol\"><option value=\"\">Todos</option><o" +
                        "ption value=\"ADMINISTRADOR\">Administrador</option><option value=\"EVALUADO" +
                        "R\">Evaluador</option><option value=\"USUARIO\">Usuario</option></select></d" + "iv>")
                .append("<div class=\"field\"><label for=\"usuarios-empresa\">Empresa</label><select " +
                        "class=\"input\" id=\"usuarios-empresa\" name=\"empresa\"><option value=\"\">" +
                        "Todos</option><option value=\"independiente\">Independiente</option>");
        for (Opcion empresa2 : empresas)
            contenido.append(opcion(Integer.toString(empresa2.id()), empresa2.nombre(), false));
        contenido.append("</select></div><div class=\"row\"><button class=\"btn p\" type=\"submit\">Ap" +
                        "licar filtros</button><button class=\"btn\" type=\"reset\">Limpiar filtros</" + "button" +
                        "></div></form>")
                .append("<div class=\"row between usuarios-cabecera\"><p class=\"muted\" role=\"statu" +
                        "s\" aria-live=\"polite\" data-contador-usuarios>")
                .append(Html.plural(cuentas.size(), "cuenta")).append(" registrada")
                .append(cuentas.size() == 1 ? "" : "s").append("</p></div>")
                .append("<div class=\"tablewrap\"><table class=\"resp usuarios-table\"><thead><tr><th" +
                        " scope=\"col\">Nombre y correo</th><th scope=\"col\">Rol</th><th scope=\"col" +
                        "\">Empresas</th><th scope=\"col\">Estado</th><th scope=\"col\">Acciones</th>" +
                        "</tr></thead><tbody>");
        for (Cuenta cuenta : cuentas) {
            contenido.append("<tr data-cuenta data-rol=\"").append(cuenta.rol().name()).append("\" data-empresa=\"")
                    .append(cuenta.rol() == Rol.ADMINISTRADOR ? "independiente" : asignadas.getOrDefault(cuenta.id(),
                            List.of()).stream().map(Object::toString).collect(java.util.stream.Collectors.joining(" ")))
                    .append("\">")
                    .append("<td data-label=\"Nombre y correo\"><div class=\"cell-value\" data-nombre-cor" + "reo><b>")
                    .append(Html.escapar(cuenta.nombre())).append("</b><div class=\"small muted\">")
                    .append(Html.escapar(cuenta.correo())).append("</div></div></td>")
                    .append("<td data-label=\"Rol\"><div class=\"cell-value\">")
                    .append(Formularios.etiqueta(cuenta.rol().name())).append("</div></td>")
                    .append("<td data-label=\"Empresas\"><div class=\"cell-value\">").append(Html.escapar(
                            cuenta.rol() == Rol.ADMINISTRADOR ? "Todas" : asignadas.getOrDefault(cuenta.id(), List.of())
                                    .isEmpty() ? "Sin empresas asignadas" : asignadas.getOrDefault(cuenta.id(),
                                    List.of())
                                                                            .stream().map(id -> nombres.getOrDefault(id,
                                    "Empresa asignada")).collect(java.util.stream.Collectors.joining(", "))))
                    .append("</div></td>").append("<td data-label=\"Estado\"><div class=\"cell-value\"><span class=\"")
                    .append(claseEstado(cuenta.estado())).append("\">").append(estado(cuenta.estado()))
                    .append("</span></div></td>")
                    .append("<td data-label=\"Acciones\"><div class=\"cell-value row\"><a class=\"btn sm" +
                            "\" href=\"/usuarios/").append(cuenta.id()).append("/editar\">Editar</a>")
                    .append("<form method=\"post\" action=\"/usuarios/").append(cuenta.id())
                    .append(cuenta.estado() == Estado.INACTIVA ? "/activar\">" : "/desactivar\">")
                    .append(Html.csrf(sesion)).append("<button class=\"btn sm\">")
                    .append(cuenta.estado() == Estado.INACTIVA ? "Activar" : "Desactivar")
                    .append("</button></form></div></td></tr>");
        }
        contenido.append("</tbody></table></div>").append(Html.vacio(
                                "Aún no hay usuarios con estos filtros. Puedes cambiar la búsqueda o crear un" +
                                    "a " + "cuenta.",
                                "<a class=\"btn p\" href=\"/usuarios/nuevo\">Nuevo usuario</a>")
                        .replace("class=\"empty marca\"", "class=\"empty marca\" data-usuarios-vacios hidden"))
                .append("<script src=\"/static/js/usuarios.js\" defer></script>");
        return Html.pagina("Usuarios", contenido.toString(), sesion, "Gestión · Usuarios",
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
        String[] nombresColor = {"Rosado", "Azul", "Lila", "Turquesa claro", "Durazno", "Gris perla"};
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
