package co.aplomo.seguridad;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import java.util.Objects;

/** RF-002: autorización en el servidor, común a rutas y servicios. */
public final class Permisos {
    private Permisos() {}

    /** Rechaza operaciones de administración desde cuentas no habilitadas o temporales. */
    public static void administrador(Cuenta cuenta) {
        if (cuenta == null || cuenta.estado() != Estado.ACTIVA || cuenta.temporal() || cuenta.rol() != Rol.ADMINISTRADOR) {
            throw new SecurityException("Acceso denegado. Esta opción requiere una cuenta de Administrador.");
        }
    }

    /** Indica si la cuenta puede consultar la empresa; el DAO también debe filtrar por su ID. */
    public static boolean empresa(Cuenta cuenta, int empresa) {
        return cuenta != null && cuenta.estado() == Estado.ACTIVA && !cuenta.temporal()
                && (cuenta.rol() == Rol.ADMINISTRADOR || Objects.equals(cuenta.empresa(),empresa));
    }
}
