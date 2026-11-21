package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Autenticacion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-001, RNF-011: ingreso, contraseña temporal y cierre separados del enrutador.
 */
public final class AccesoWeb {
    private final Autenticacion auth;
    private final Sesiones sesiones;

    /**
     * RNF-011: recibe el servicio de autenticación y el almacén de sesiones.
     */
    public AccesoWeb(Autenticacion auth, Sesiones sesiones) {
        this.auth = auth;
        this.sesiones = sesiones;
    }

    /**
     * RF-001a: conserva CSRF, bloqueo y rotación de sesión del ingreso.
     */
    public void ingresar(HttpExchange intercambio, Sesion sesion) throws IOException, SQLException {
        if (sesion != null && sesion.cuenta() != null) {
            Router.redirigir(intercambio, sesion.cuenta().temporal() ? "/cuenta/contrasena" : "/");
            return;
        }
        if (intercambio.getRequestMethod().equals("GET")) {
            if (sesion == null) {
                sesion = sesiones.crear(null);
                Router.ponerCookie(intercambio, sesion.id());
            }
            String aviso = "inactividad=1".equals(intercambio.getRequestURI().getRawQuery()) ?
                    "Tu sesión se cerró por inactividad o porque cambió el acceso de tu cuenta. V" +
                    "uelve a ingresar." : null;
            Router.enviar(intercambio, 200, Html.login(sesion, "", aviso));
            return;
        }
        Map<String, String> parametros = Router.formulario(intercambio);
        if (!Sesiones.csrfValido(sesion, parametros.get("csrf"))) {

            // RNF-004, RNF-007: rechaza el envío y permite reintentar con un formulario vigente.
            if (sesion == null) {
                sesion = sesiones.crear(null);
                Router.ponerCookie(intercambio, sesion.id());
            }
            Router.enviar(intercambio, 403, Html.login(sesion, parametros.get("correo"),
                    "El formulario venció. Vuelve a escribir tu contraseña e inténtalo de nuevo."));
            return;
        }
        Cuenta cuenta;
        try {
            cuenta = auth.ingresar(parametros.get("correo"), parametros.get("clave"));
        } catch (SQLException error) {
            Router.enviar(intercambio, 503, Html.login(sesion, parametros.get("correo"),
                    "No pudimos verificar el ingreso. Revisa la conexión e inténtalo de nuevo."));
            return;
        }
        if (cuenta == null) {
            Router.enviar(intercambio, 401,
                    Html.login(sesion, parametros.get("correo"), auth.mensajeFallo(parametros.get("correo"))));
            return;
        }
        sesiones.cerrar(sesion.id());
        sesion = sesiones.crear(cuenta);
        Router.ponerCookie(intercambio, sesion.id());
        Router.redirigir(intercambio, cuenta.temporal() ? "/cuenta/contrasena" : "/");
        return;
    }

    /**
     * RF-001c: mantiene el cambio obligatorio de contraseña temporal.
     */
    public void contrasena(HttpExchange intercambio, Sesion sesion,
            Map<String, String> parametros) throws IOException, SQLException {
        if (intercambio.getRequestMethod().equals("GET")) {
            Router.enviar(intercambio, 200, Html.contrasena(sesion, null));
            return;
        }
        Cuenta cambiada;
        try {
            cambiada = auth.cambiar(sesion.cuenta(), parametros.get("actual"), parametros.get("nueva"),
                    parametros.get("confirmacion"));
        } catch (IllegalArgumentException error) {
            Router.enviar(intercambio, 400, Html.contrasena(sesion, error.getMessage()));
            return;
        } catch (SQLException error2) {
            Router.enviar(intercambio, 503,
                    Html.contrasena(sesion, "No pudimos guardar la contraseña. Inténtalo de nuevo."));
            return;
        }
        sesiones.invalidarCuenta(cambiada.id());
        sesion = sesiones.crear(cambiada);
        Router.ponerCookie(intercambio, sesion.id());
        Router.redirigir(intercambio, "/?contrasena=1");
        return;
    }

    /**
     * RF-001b: conserva el cierre por POST y la eliminación de la cookie.
     */
    public void cerrar(HttpExchange intercambio, Sesion sesion) throws IOException {
        if (!intercambio.getRequestMethod().equals("POST")) {
            Router.enviar(intercambio, 405, "Usa el botón Cerrar sesión.");
            return;
        }
        sesiones.cerrar(sesion.id());
        Router.ponerCookie(intercambio, "");
        Router.redirigir(intercambio, "/login");
        return;
    }
}
