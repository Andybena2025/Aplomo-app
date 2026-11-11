package co.aplomo.seguridad;

import co.aplomo.dao.CuentaDao;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import java.sql.*;
import java.util.Objects;

/** RF-002: verifica el actor actual también al llamar directamente a los servicios. */
public final class AccesoDatos {
    private AccesoDatos() {}

    /** Relee cuenta, contraseña, estado, empresa y rol antes de permitir una operación. */
    public static Cuenta vigente(Connection con, Cuenta actor) throws SQLException {
        if (actor == null) throw new SecurityException("Inicia sesión para continuar.");
        Cuenta actual = new CuentaDao().porId(con, actor.id(), false);
        if (actual == null || actual.estado() != Estado.ACTIVA || actual.temporal()
                || !actual.hash().equals(actor.hash()) || actual.rol() != actor.rol()
                || !Objects.equals(actual.empresa(), actor.empresa())) {
            throw new SecurityException("Tu acceso cambió. Vuelve a iniciar sesión.");
        }
        return actual;
    }

    /** Devuelve el filtro de empresa: cero solo para el Administrador, nunca para un Usuario sin empresa. */
    public static int empresaPermitida(Cuenta actor) {
        if (actor.rol() == Rol.ADMINISTRADOR) return 0;
        if (actor.empresa() == null) throw new SecurityException("Tu cuenta no tiene una empresa asignada.");
        return actor.empresa();
    }
}
