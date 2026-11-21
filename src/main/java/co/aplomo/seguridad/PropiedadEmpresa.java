package co.aplomo.seguridad;

import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * RF-002/028, decisión de Andy: solo el dueño edita su empresa y sus espacios.
 */
public final class PropiedadEmpresa {
    private PropiedadEmpresa() {
    }

    /**
     * RF-002: autoriza en servidor tanto datos como estados; nunca desde valores del formulario.
     */
    public static void exigir(Connection conexion, Cuenta actor, int id, boolean raiz,
            boolean estado) throws SQLException {
        Cuenta cuenta = AccesoDatos.vigente(conexion, actor);
        Permisos.trabajo(cuenta);
        AccesoDatos.exigirEmpresa(conexion, cuenta, id);
        Empresa empresa = new EmpresaDao().buscar(conexion, id, 0, true);
        if (empresa == null) throw new SecurityException("La empresa no está disponible.");
        int creador = empresa.creador() == null ? empresa.administrador() : empresa.creador();
        Cuenta propietario = new CuentaDao().porId(conexion, creador, false);
        if (propietario != null && propietario.rol() == Cuenta.Rol.EVALUADOR) {
            if (cuenta.rol() != Cuenta.Rol.EVALUADOR || cuenta.id() != creador) {
                throw new SecurityException("Solo el Evaluador dueño puede modificar esta empresa y sus espacios.");
            }
        } else if (raiz || estado) {
            Permisos.administrador(cuenta);
        }
    }
}
