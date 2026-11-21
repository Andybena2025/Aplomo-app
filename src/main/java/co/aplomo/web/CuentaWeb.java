package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Autenticacion;
import co.aplomo.servicio.CuentaPropia;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * RF-001c, RF-002: edición del perfil autenticado, con CSRF verificado por Router.
 */
public final class CuentaWeb {
    private final CuentaPropia perfil;
    private final Autenticacion autenticacion;
    private final Sesiones sesiones;

    /**
     * RNF-011: recibe servicios y sesiones, sin abrir conexiones propias.
     */
    public CuentaWeb(CuentaPropia perfil, Autenticacion autenticacion, Sesiones sesiones) {
        this.perfil = perfil;
        this.autenticacion = autenticacion;
        this.sesiones = sesiones;
    }

    /**
     * RF-001c: atiende datos y clave propios; nunca recibe un identificador de otra cuenta.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> formulario) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        if (!java.util.Set.of("/cuenta", "/cuenta/clave", "/cuenta/nombre", "/cuenta/telefono", "/cuenta/cargo")
                .contains(ruta)) {
            return false;
        }
        if (intercambio.getRequestMethod().equals("GET")) {
            Router.enviar(intercambio, 200, vista(sesion, ruta, Map.of(),
                    "1".equals(Router.consulta(intercambio).get("guardado")) ? "Datos guardados." : null));
            return true;
        }
        try {
            Cuenta cuenta = ruta.equals("/cuenta/clave") ? autenticacion.cambiar(sesion.cuenta(),
                    formulario.get("actual"), formulario.get("nueva"), formulario.get("confirmacion")) : ruta.equals(
                    "/cuenta") ? perfil.guardar(sesion.cuenta(), formulario.get("nombre"), formulario.get("telefono"),
                    formulario.get("cargo")) : perfil.guardarCampo(sesion.cuenta(), ruta.substring("/cuenta/".length()),
                    formulario.get(ruta.substring("/cuenta/".length())));
            sesiones.invalidarCuenta(cuenta.id());
            Sesion nueva = sesiones.crear(cuenta);
            Router.ponerCookie(intercambio, nueva.id());
            Router.redirigir(intercambio, "/cuenta?guardado=1");
        } catch (IllegalArgumentException error) {
            Router.enviar(intercambio, 400,
                    ruta.equals("/cuenta") ? corregirPerfil(sesion, formulario, error.getMessage()) : vista(sesion,
                            ruta, formulario, error.getMessage()));
        }
        return true;
    }

    private String corregirPerfil(Sesion sesion, Map<String, String> valores, String mensaje) {
        String formulario =
                "<form class=\"card stack cuenta-editar\" method=\"post\" action=\"/cuenta\">" + Html.csrf(sesion) +
                        Html.aviso(mensaje, false) + campo("nombre", "Nombre", valores.get("nombre"), 100) +
                        campo("telefono", "Teléfono", valores.get("telefono"), 20) +
                        campo("cargo", "Cargo", valores.get("cargo"), 80) +
                        "<button class=\"btn p\">Guardar</button></form>";
        return Html.pagina("Revisar datos personales", formulario, sesion, "Mi cuenta · Datos personales", "");
    }

    private String vista(Sesion sesion, String ruta, Map<String, String> valores, String mensaje) throws SQLException {
        if (ruta.equals("/cuenta")) return pagina(sesion, mensaje);
        if (ruta.equals("/cuenta/clave")) {
            return Html.pagina("Cambiar contraseña", Html.aviso(mensaje, false) + clave(sesion), sesion,
                    "Mi cuenta · Seguridad", "");
        }
        String nombre = ruta.substring("/cuenta/".length());
        String etiqueta = switch (nombre) {
            case "nombre" -> "Nombre";
            case "telefono" -> "Teléfono";
            default -> "Cargo";
        };
        String actual = switch (nombre) {
            case "nombre" -> sesion.cuenta().nombre();
            case "telefono" -> sesion.cuenta().telefono();
            default -> sesion.cuenta().cargo();
        };
        int limite = nombre.equals("nombre") ? 100 : nombre.equals("telefono") ? 20 : 80;
        String formulario = "<form class=\"card stack cuenta-editar\" method=\"post\" action=\"" + ruta + "\">" +
                Html.csrf(sesion) + Html.aviso(mensaje, false) +
                campo(nombre, etiqueta, valores.getOrDefault(nombre, actual), limite) +
                "<div class=\"row\"><a class=\"btn\" href=\"/cuenta\">Cancelar</a>" +
                "<button class=\"btn p\">Guardar</button></div></form>";
        return Html.pagina("Editar " + etiqueta.toLowerCase(java.util.Locale.ROOT), formulario, sesion,
                "Mi cuenta · Datos personales", "");
    }

    private String pagina(Sesion sesion, String mensaje) throws SQLException {
        Cuenta cuenta = sesion.cuenta();
        List<String> empresas = perfil.empresas(cuenta);
        String inicial = cuenta.nombre().isBlank() ? "" : cuenta.nombre().substring(0, 1);
        StringBuilder html = new StringBuilder(Html.aviso(mensaje, "Datos guardados.".equals(mensaje)));
        html.append("<div class=\"cuenta-ajustes\"><section class=\"card cuenta-perfil\"><span class=\"iniciales\">")
                .append(Html.escapar(inicial)).append("</span><div><h2>").append(Html.escapar(cuenta.nombre()))
                .append("</h2><p class=\"muted\">").append(Formularios.etiqueta(cuenta.rol().name())).append("</p><p>")
                .append(Html.escapar(cuenta.correo())).append("</p></div></section>")
                .append("<section class=\"card stack\"><h2>Datos personales</h2>")
                .append(ajuste("nombre", "Nombre", cuenta.nombre()))
                .append(ajuste("telefono", "Teléfono", cuenta.telefono()))
                .append(ajuste("cargo", "Cargo", cuenta.cargo())).append("</section>")
                .append("<section class=\"card stack\"><h2>Seguridad</h2><div " +
                        "class=\"ajuste-fila\"><span>Correo</span><span>").append(Html.escapar(cuenta.correo()))
                .append("<small class=\"muted\">Lo cambia un Administrador</small>").append("</span></div>")
                .append("<a class=\"ajuste-fila\" href=\"/cuenta/clave\"><span>Cambiar " +
                        "contraseña</span><span>›</span></a>")
                .append("</section><section class=\"card stack\"><h2>Accesos</h2><p>").append(Html.escapar(
                        cuenta.rol() == Cuenta.Rol.ADMINISTRADOR ? "Todas las empresas" : String.join(" · ", empresas)))
                .append("</p></section><form method=\"post\" action=\"/logout\">").append(Html.csrf(sesion))
                .append("<button class=\"btn\">Cerrar sesión</button></form></div>");
        return Html.pagina("Mi cuenta", html.toString(), sesion, "Cuenta · Ajustes", "");
    }

    private static String ajuste(String nombre, String etiqueta, String valor) {
        return "<a class=\"ajuste-fila\" href=\"/cuenta/" + nombre + "\"><span><strong>" + etiqueta +
                "</strong><small class=\"muted\">" + Html.escapar(valor) + "</small></span><span>" +
                (valor == null || valor.isBlank() ? "Agregar" : "Editar") + " ›</span></a>";
    }

    private static String campo(String nombre, String etiqueta, String valor, int limite) {
        return "<label class=\"field\">" + etiqueta + "<input class=\"input\" name=\"" + nombre + "\" value=\"" +
                Html.escapar(valor) + "\" maxlength=\"" + limite + "\" required></label>";
    }

    private static String clave(Sesion sesion) {
        StringBuilder formulario = new StringBuilder(
                "<form class=\"card stack\" method=\"post\" action=\"/cuenta/clave\"><h2>Cont" + "raseña</h2>");
        formulario.append(Html.csrf(sesion));
        for (String[] campo : List.of(new String[]{"actual", "Contraseña actual"},
                new String[]{"nueva", "Nueva contraseña"}, new String[]{"confirmacion", "Confirmar contraseña"})) {
            formulario.append("<label class=\"field\">").append(campo[1])
                    .append("<input class=\"input\" type=\"password\" name=\"").append(campo[0])
                    .append("\" required autocomplete=\"new-password\"></label>");
        }
        return formulario.append("<button class=\"btn primary\">Cambiar contraseña</button></form>").toString();
    }
}
