package co.aplomo.seguridad;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.modelo.Cuenta.Rol;

import java.util.Objects;

/**
 * RF-002: autorización en el servidor, común a rutas y servicios.
 */
public final class Permisos {
    private Permisos() {
    }

    /**
     * Rechaza operaciones de administración desde cuentas no habilitadas o temporales. RNF-011.
     */
    public static void administrador(Cuenta cuenta) {
        if (cuenta == null || cuenta.estado() != Estado.ACTIVA || cuenta.temporal() ||
                cuenta.rol() != Rol.ADMINISTRADOR) {
            throw new SecurityException("Acceso denegado. Esta opción requiere una cuenta de Administrador.");
        }
    }

    /**
     * RF-002: trabajo reservado a Administrador o Evaluador; el servicio comprueba la empresa.
     */
    public static void trabajo(Cuenta cuenta2) {
        if (cuenta2 == null || cuenta2.estado() != Estado.ACTIVA || cuenta2.temporal() || cuenta2.rol() == Rol.USUARIO)
            throw new SecurityException("Acceso denegado. Esta opción requiere una cuenta de evaluación.");
    }

    /**
     * RF-002, Paso 27: las reservas, visitas y práctica pertenecen solo al Evaluador.
     */
    public static void evaluador(Cuenta cuenta) {
        trabajo(cuenta);
        if (cuenta.rol() != Rol.EVALUADOR) {
            throw new SecurityException("Acceso denegado. Esta acción corresponde al Evaluador.");
        }
    }

    /**
     * RF-002: autorización de empresa con fuente cuenta_empresa, nunca la relación anterior.
     */
    public static boolean empresa(java.sql.Connection conexion, Cuenta cuenta,
            int empresa) throws java.sql.SQLException {
        try {
            AccesoDatos.exigirEmpresa(conexion, cuenta, empresa);
            return true;
        } catch (SecurityException error) {
            return false;
        }
    }
}
