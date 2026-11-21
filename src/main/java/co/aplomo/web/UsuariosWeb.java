package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Usuarios;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RF-003: rutas de cuentas detrás de sesión, cambio obligatorio, permisos y CSRF del Router.
 */
public final class UsuariosWeb {
    private final Usuarios usuarios;
    private final Sesiones sesiones;
    private static final Pattern CUENTA = Pattern.compile("/usuarios/([1-9][0-9]*)/(editar|activar|desactivar)");

    /**
     * Recibe el servicio autorizado y el almacén de sesiones que se deben invalidar (RF-002).
     */
    public UsuariosWeb(Usuarios usuarios, Sesiones sesiones) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
    }

    /**
     * Gestiona lista, alta, edición y estado sin confiar en rol ni estado enviados en el formulario. RNF-011.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> parametros) throws SQLException, IOException {
        Permisos.administrador(sesion.cuenta());
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/usuarios") && !post) {
            String texto3 = intercambio.getRequestURI().getRawQuery();
            String aviso = "guardado=1".equals(texto3) ? "Cuenta actualizada." : "creado=1".equals(
                    texto3) ? "Cuenta creada." : null;
            Router.enviar(intercambio, 200,
                    PaginasUsuarios.lista(sesion, usuarios.listar(sesion.cuenta()), usuarios.empresas(sesion.cuenta()),
                            usuarios.asignaciones(sesion.cuenta()), aviso));
            return;
        }
        if (ruta.equals("/usuarios/nuevo") && !post) {
            Router.enviar(intercambio, 200,
                    PaginasUsuarios.formulario(sesion, null, usuarios.empresas(sesion.cuenta()), Map.of(), null));
            return;
        }
        var elemento = CUENTA.matcher(ruta);
        boolean edicion = elemento.matches() && elemento.group(2).equals("editar");
        int id = elemento.matches() ? id(elemento.group(1)) : 0;
        if (edicion && !post) {
            Router.enviar(intercambio, 200, PaginasUsuarios.formulario(sesion, usuarios.consultar(sesion.cuenta(), id),
                    usuarios.empresas(sesion.cuenta()), seleccion(sesion, id), null));
            return;
        }
        if (post && (ruta.equals("/usuarios") || edicion)) {
            Cuenta anterior = edicion ? usuarios.consultar(sesion.cuenta(), id) : null;
            var opciones = usuarios.empresas(sesion.cuenta());
            try {
                Usuarios.Datos datos = datos(parametros);
                if (edicion) {
                    usuarios.editar(sesion.cuenta(), id, datos, parametros.get("temporal"));
                    sesiones.invalidarCuenta(id);
                } else usuarios.crear(sesion.cuenta(), datos, parametros.get("temporal"));
            } catch (IllegalArgumentException error) {
                Router.enviar(intercambio, 400,
                        PaginasUsuarios.formulario(sesion, anterior, opciones, parametros, error.getMessage()));
                return;
            } catch (SQLException error2) {
                Router.enviar(intercambio, 503, PaginasUsuarios.formulario(sesion, anterior, opciones, parametros,
                        "No pudimos guardar la cuenta. Tus datos siguen en el formulario; inténtalo d" + "e nuevo."));
                return;
            }
            Router.redirigir(intercambio, edicion && id == sesion.cuenta().id() ? "/login" :
                    "/usuarios?" + (edicion ? "guardado=1" : "creado=1"));
            return;
        }
        if (post && elemento.matches() && !edicion) {
            try {
                usuarios.estado(sesion.cuenta(), id, elemento.group(2).equals("activar"));
            } catch (IllegalArgumentException error3) {
                Router.enviar(intercambio, 400, Html.pagina("No se cambió la cuenta",
                        Html.aviso(error3.getMessage(), false) + "<a href=\"/usuarios\">Volver a usuarios</a>",
                        sesion));
                return;
            }
            sesiones.invalidarCuenta(id);
            Router.redirigir(intercambio, id == sesion.cuenta().id() ? "/login" : "/usuarios?guardado=1");
            return;
        }
        Router.enviar(intercambio, 404,
                Html.pagina("Página no disponible", "<a href=\"/usuarios\">Volver a usuarios</a>", sesion));
    }

    private Map<String, String> seleccion(Sesion sesion, int id) throws SQLException {
        var texto2 = new java.util.HashMap<String, String>();
        for (int cantidad : usuarios.asignaciones(sesion.cuenta()).getOrDefault(id, java.util.List.of()))
            texto2.put("empresa_" + cantidad, "on");
        return texto2;
    }

    private static int id(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("La cuenta no existe. Vuelve a la lista de usuarios.");
        }
    }

    private static Usuarios.Datos datos(Map<String, String> parametros) {
        Rol rol;
        try {
            rol = Rol.valueOf(parametros.getOrDefault("rol", ""));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Elige un rol válido.");
        }
        Integer empresa = null;
        if (!parametros.getOrDefault("empresa", "").isBlank()) empresa = id(parametros.get("empresa"));
        var elegidas = parametros.keySet().stream()
                .filter(clave -> clave.startsWith("empresa_") && parametros.get(clave).equals("on"))
                .map(clave -> id(clave.substring(8))).toList();
        var asignaciones = elegidas.isEmpty() ? (
                empresa == null ? java.util.List.<Integer>of() : java.util.List.of(empresa)) : elegidas;
        return new Usuarios.Datos(parametros.get("nombre"), parametros.get("correo"), parametros.get("telefono"),
                parametros.get("cargo"), rol, empresa, asignaciones, parametros.get("colorAgenda"));
    }
}
