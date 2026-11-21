package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.AccesoDatos;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-001c, RF-002: perfil propio sin edición de permisos ni asignaciones.
 */
public final class CuentaPropia {
    private final Transacciones transacciones;
    private final Clock reloj;

    /**
     * RNF-011: comparte transacción y reloj con el servicio de cuentas.
     */
    public CuentaPropia(Transacciones transacciones, Clock reloj) {
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    /**
     * RF-002: lee únicamente las empresas asignadas al titular vigente.
     */
    public List<String> empresas(Cuenta titular) throws SQLException {
        return transacciones.ejecutar(conexion -> {
            AccesoDatos.vigente(conexion, titular);
            return Sql.listar(conexion,
                    "SELECT e.nombre FROM cuenta_empresa ce JOIN empresa e ON e.id_empresa=ce.id_" + "empresa " +
                            "WHERE ce.id_cuenta=? AND ce.activo=TRUE ORDER BY e.nombre", fila -> fila.getString(1),
                    titular.id());
        });
    }

    /**
     * RF-001c, RNF-004: guarda los tres campos propios y registra la edición.
     */
    public Cuenta guardar(Cuenta titular, String nombre, String telefono, String cargo) throws SQLException {
        String nombreValido = validar(nombre, 100, "nombre");
        String telefonoValido = validar(telefono, 20, "teléfono");
        String cargoValido = validar(cargo, 80, "cargo");
        return transacciones.ejecutar(conexion -> {
            AccesoDatos.vigente(conexion, titular);
            Sql.actualizar(conexion, "UPDATE cuenta SET nombre=?, telefono=?, cargo=? WHERE id_cuenta=?", nombreValido,
                    telefonoValido, cargoValido, titular.id());
            new BitacoraDao().registrar(conexion, titular.id(), LocalDateTime.now(reloj), "cuenta", titular.id(),
                    "EDITAR", null, null, "Actualizó sus datos personales");
            return new CuentaDao().porId(conexion, titular.id(), false);
        });
    }

    private static String validar(String valor, int limite, String campo) {
        String limpio = valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
        if (limpio.isBlank() || limpio.length() > limite || limpio.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Revisa el " + campo + ": máximo " + limite + " caracteres.");
        }
        return limpio;
    }
}
