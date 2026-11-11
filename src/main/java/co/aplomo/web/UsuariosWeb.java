package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.*;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Usuarios;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/** RF-003: rutas de cuentas detrás de sesión, cambio obligatorio, permisos y CSRF del Router. */
public final class UsuariosWeb {
    private final Usuarios usuarios;
    private final Sesiones sesiones;
    private static final Pattern CUENTA=Pattern.compile("/usuarios/([1-9][0-9]*)/(editar|activar|desactivar)");

    /** Recibe el servicio autorizado y el almacén de sesiones que se deben invalidar (RF-002). */
    public UsuariosWeb(Usuarios usuarios,Sesiones sesiones) { this.usuarios=usuarios; this.sesiones=sesiones; }

    /** Gestiona lista, alta, edición y estado sin confiar en rol ni estado enviados en el formulario. */
    public void atender(HttpExchange x,Sesion s,Map<String,String> f) throws SQLException, IOException {
        Permisos.administrador(s.cuenta());
        String ruta=x.getRequestURI().getPath(); boolean post=x.getRequestMethod().equals("POST");
        if(ruta.equals("/usuarios") && !post) {
            Router.enviar(x,200,PaginasUsuarios.lista(s,usuarios.listar(s.cuenta()),usuarios.empresas(s.cuenta()),"guardado=1".equals(x.getRequestURI().getRawQuery()))); return;
        }
        if(ruta.equals("/usuarios/nuevo") && !post) {
            Router.enviar(x,200,PaginasUsuarios.formulario(s,null,usuarios.empresas(s.cuenta()),Map.of(),null)); return;
        }
        var m=CUENTA.matcher(ruta);
        boolean edicion=m.matches() && m.group(2).equals("editar");
        int id=m.matches() ? id(m.group(1)) : 0;
        if(edicion && !post) {
            Router.enviar(x,200,PaginasUsuarios.formulario(s,usuarios.consultar(s.cuenta(),id),usuarios.empresas(s.cuenta()),Map.of(),null)); return;
        }
        if(post && (ruta.equals("/usuarios") || edicion)) {
            Cuenta anterior=edicion ? usuarios.consultar(s.cuenta(),id) : null;
            var opciones=usuarios.empresas(s.cuenta());
            try {
                Usuarios.Datos datos=datos(f);
                if(edicion) { usuarios.editar(s.cuenta(),id,datos,f.get("temporal")); sesiones.invalidarCuenta(id); }
                else usuarios.crear(s.cuenta(),datos,f.get("temporal"));
            } catch(IllegalArgumentException e) {
                Router.enviar(x,400,PaginasUsuarios.formulario(s,anterior,opciones,f,e.getMessage())); return;
            } catch(SQLException e) {
                Router.enviar(x,503,PaginasUsuarios.formulario(s,anterior,opciones,f,"No pudimos guardar la cuenta. Tus datos siguen en el formulario; inténtalo de nuevo.")); return;
            }
            Router.redirigir(x,edicion && id==s.cuenta().id() ? "/login" : "/usuarios?guardado=1"); return;
        }
        if(post && m.matches() && !edicion) {
            try { usuarios.estado(s.cuenta(),id,m.group(2).equals("activar")); }
            catch(IllegalArgumentException e) {
                Router.enviar(x,400,Html.pagina("No se cambió la cuenta",Html.aviso(e.getMessage(),false)+"<a href=\"/usuarios\">Volver a usuarios</a>",s)); return;
            }
            sesiones.invalidarCuenta(id);
            Router.redirigir(x,id==s.cuenta().id() ? "/login" : "/usuarios?guardado=1"); return;
        }
        Router.enviar(x,404,Html.pagina("Página no disponible","<a href=\"/usuarios\">Volver a usuarios</a>",s));
    }

    private static int id(String texto) {
        try { return Integer.parseInt(texto); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("La cuenta no existe. Vuelve a la lista de usuarios."); }
    }

    private static Usuarios.Datos datos(Map<String,String> f) {
        Rol rol;
        try { rol=Rol.valueOf(f.getOrDefault("rol","")); }
        catch(IllegalArgumentException e) { throw new IllegalArgumentException("Elige un rol válido."); }
        Integer empresa=null;
        if(!f.getOrDefault("empresa","").isBlank()) empresa=id(f.get("empresa"));
        return new Usuarios.Datos(f.get("nombre"),f.get("correo"),f.get("telefono"),f.get("cargo"),rol,empresa);
    }
}
