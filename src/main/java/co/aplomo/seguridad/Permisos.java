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

    /** RF-002: trabajo reservado a Administrador o Evaluador; el servicio comprueba la empresa. */
    public static void trabajo(Cuenta c){if(c==null||c.estado()!=Estado.ACTIVA||c.temporal()||c.rol()==Rol.USUARIO)throw new SecurityException("Acceso denegado. Esta opción requiere una cuenta de evaluación.");}

    /** RF-002: autorización de empresa con fuente cuenta_empresa, nunca la relación anterior. */
    public static boolean empresa(java.sql.Connection c,Cuenta cuenta,int empresa)throws java.sql.SQLException{try{AccesoDatos.exigirEmpresa(c,cuenta,empresa);return true;}catch(SecurityException e){return false;}}
}
