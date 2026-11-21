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

/** RF-001c, RF-002: edición del perfil autenticado, con CSRF verificado por Router. */
public final class CuentaWeb {
    private final CuentaPropia perfil;
    private final Autenticacion autenticacion;
    private final Sesiones sesiones;

    /** RNF-011: recibe servicios y sesiones, sin abrir conexiones propias. */
    public CuentaWeb(CuentaPropia perfil, Autenticacion autenticacion, Sesiones sesiones) {
        this.perfil = perfil;
        this.autenticacion = autenticacion;
        this.sesiones = sesiones;
    }

    /** RF-001c: atiende datos y clave propios; nunca recibe un identificador de otra cuenta. */
    public boolean atender(HttpExchange intercambio, Sesion sesion, Map<String, String> formulario)
            throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        if (!ruta.equals("/cuenta") && !ruta.equals("/cuenta/clave")) {
            return false;
        }
        if (intercambio.getRequestMethod().equals("GET")) {
            Router.enviar(intercambio, 200, pagina(sesion, Map.of(), null));
            return true;
        }
        try {
            Cuenta cuenta = ruta.equals("/cuenta/clave")
                    ? autenticacion.cambiar(sesion.cuenta(), formulario.get("actual"),
                            formulario.get("nueva"), formulario.get("confirmacion"))
                    : perfil.guardar(sesion.cuenta(), formulario.get("nombre"),
                            formulario.get("telefono"), formulario.get("cargo"));
            sesiones.invalidarCuenta(cuenta.id());
            Sesion nueva = sesiones.crear(cuenta);
            Router.ponerCookie(intercambio, nueva.id());
            Router.redirigir(intercambio, "/cuenta?guardado=1");
        } catch (IllegalArgumentException error) {
            Router.enviar(intercambio, 400, pagina(sesion, formulario, error.getMessage()));
        }
        return true;
    }

    private String pagina(Sesion sesion, Map<String, String> formulario, String mensaje) throws SQLException {
        Cuenta cuenta = sesion.cuenta();
        List<String> empresas = perfil.empresas(cuenta);
        StringBuilder contenido = new StringBuilder("<div class=\"pagehead\"><h1>Mi cuenta</h1></div>");
        contenido.append(Html.aviso(mensaje, false));
        contenido.append("<div class=\"grid2 cuenta-paneles\"><form class=\"card stack\" method=\"post\" ")
                .append("action=\"/cuenta\">").append(Html.csrf(sesion)).append("<h2>Tus datos</h2>");
        contenido.append(campo("nombre", "Nombre", formulario.getOrDefault("nombre", cuenta.nombre()), 100));
        contenido.append(campo("telefono", "Teléfono", formulario.getOrDefault("telefono", cuenta.telefono()), 20));
        contenido.append(campo("cargo", "Cargo", formulario.getOrDefault("cargo", cuenta.cargo()), 80));
        contenido.append("<dl><dt>Correo</dt><dd>").append(Html.escapar(cuenta.correo()))
                .append("</dd><dt>Rol</dt><dd>").append(Html.escapar(cuenta.rol().name()))
                .append("</dd><dt>Empresas asignadas</dt><dd>")
                .append(Html.escapar(cuenta.rol() == Cuenta.Rol.ADMINISTRADOR ? "Todas" : String.join(", ", empresas)))
                .append("</dd></dl><button class=\"btn primary\">Guardar cambios</button></form>");
        contenido.append(clave(sesion)).append("</div>");
        return Html.pagina("Mi cuenta", contenido.toString(), sesion, "", "");
    }

    private static String campo(String nombre, String etiqueta, String valor, int limite) {
        return "<label class=\"field\">" + etiqueta + "<input class=\"input\" name=\"" + nombre
                + "\" value=\"" + Html.escapar(valor) + "\" maxlength=\"" + limite + "\" required></label>";
    }

    private static String clave(Sesion sesion) {
        StringBuilder formulario = new StringBuilder(
                "<form class=\"card stack\" method=\"post\" action=\"/cuenta/clave\"><h2>Contraseña</h2>");
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
