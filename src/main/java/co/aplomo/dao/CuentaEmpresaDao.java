package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * RF-002/003, RNF-005: asignaciones conservadas, único Evaluador activo por empresa.
 */
public final class CuentaEmpresaDao {

    /**
     * Lee las empresas asignadas, incluidas las inactivas. RNF-011.
     */
    public List<Integer> ids(Connection conexion, int cuenta) throws SQLException {
        return Sql.listar(conexion,
                "SELECT id_empresa FROM cuenta_empresa WHERE id_cuenta=? AND activo=TRUE ORDE" + "R BY id_empresa",
                fila2 -> fila2.getInt(1), cuenta);
    }

    /**
     * Mapa de asignaciones para la gestión autorizada de cuentas. RNF-011.
     */
    public Map<Integer, List<Integer>> todas(Connection conexion) throws SQLException {
        Map<Integer, List<Integer>> elementos = new HashMap<>();
        for (var par : Sql.listar(conexion,
                "SELECT id_cuenta,id_empresa FROM cuenta_empresa WHERE activo=TRUE ORDER BY i" + "d_empresa",
                fila2 -> Map.entry(fila2.getInt(1), fila2.getInt(2))))
            elementos.computeIfAbsent(par.getKey(), k -> new ArrayList<>()).add(par.getValue());
        return elementos;
    }

    /**
     * Valida y bloquea en orden para impedir dos Evaluadores simultáneos. RNF-011.
     */
    public void validar(Connection conexion, int cuenta, Cuenta.Rol rol, Collection<Integer> empresas,
            boolean activa) throws SQLException {
        for (int id : new TreeSet<>(empresas)) {
            var emp = Sql.uno(conexion, "SELECT activo FROM empresa WHERE id_empresa=? FOR UPDATE",
                    fila2 -> fila2.getBoolean(1), id);
            if (emp == null)
                throw new IllegalArgumentException("La empresa elegida no existe. Selecciona una de la lista.");
            if (!emp && !ids(conexion, cuenta).contains(id)) throw new IllegalArgumentException(
                    "La empresa está inactiva. Reactívala antes de asignar cuentas nuevas.");
            if (rol == Cuenta.Rol.EVALUADOR && activa) {
                String nombre = Sql.uno(conexion,
                        "SELECT q.nombre FROM cuenta_empresa ce JOIN cuenta q USING(id_cuenta) WHERE " +
                                "ce.id_empresa=? AND ce.activo=TRUE AND q.rol='EVALUADOR' AND q.estado<>'INAC" +
                                "TIVA' AND q.id_cuenta<>? LIMIT 1", fila3 -> fila3.getString(1), id, cuenta);
                if (nombre != null) throw new IllegalArgumentException(
                        "Esta empresa ya tiene evaluador: " + nombre + ". Quítasela primero.");
            }
        }
    }

    /**
     * Retira y reactiva sin DELETE; cada cambio conserva responsable y fecha. RNF-011.
     */
    public void sincronizar(Connection conexion, int responsable, int cuenta, Collection<Integer> deseadas,
            boolean cambiaRol) throws SQLException {
        Set<Integer> nuevas = new TreeSet<>(deseadas);
        var antes = ids(conexion, cuenta);
        var ahora = LocalDateTime.now(ZoneId.of("America/Bogota"));
        var elemento = new BitacoraDao();
        for (int id : antes)
            if (cambiaRol || !nuevas.contains(id)) {
                int fila = Sql.uno(conexion,
                        "SELECT id_cuenta_empresa FROM cuenta_empresa WHERE id_cuenta=? AND id_empresa=?",
                        fila2 -> fila2.getInt(1), cuenta, id);
                Sql.actualizar(conexion, "UPDATE cuenta_empresa SET activo=FALSE WHERE id_cuenta_empresa=?", fila);
                elemento.registrar(conexion, responsable, ahora, "cuenta_empresa", fila, "DESACTIVAR", "ACTIVO",
                        "INACTIVO", "Asignación retirada");
            }
        for (int id : nuevas)
            if (cambiaRol || !antes.contains(id)) {
                Integer fila = Sql.uno(conexion,
                        "SELECT id_cuenta_empresa FROM cuenta_empresa WHERE id_cuenta=? AND id_empresa=?",
                        fila3 -> fila3.getInt(1), cuenta, id);
                boolean nueva = fila == null;
                if (nueva) fila = Sql.insertar(conexion,
                        "INSERT INTO cuenta_empresa(id_cuenta,id_empresa,fecha_asignacion) VALUES (?,?,?)", cuenta, id,
                        ahora);
                else Sql.actualizar(conexion,
                        "UPDATE cuenta_empresa SET activo=TRUE,fecha_asignacion=? WHERE id_cuenta_empresa=?", ahora,
                        fila);
                elemento.registrar(conexion, responsable, ahora, "cuenta_empresa", fila, nueva ? "CREAR" : "REACTIVAR",
                        nueva ? null : "INACTIVO", "ACTIVO", "Asignación de empresa");
            }
    }
}
