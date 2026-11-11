package co.aplomo.dao;

import co.aplomo.db.Sql;
import java.sql.*;
import java.time.LocalDateTime;

/** RNF-005: historial de acciones, guardado en la misma transacción de la operación. */
public class BitacoraDao {
    /** Crea el acceso al registro de auditoría. */
    public BitacoraDao() {}

    /** Guarda responsable, fecha, acción, registro afectado y cambio de estado. */
    public void registrar(Connection con, int cuenta, LocalDateTime cuando, String tabla, int registro,
                          String accion, String anterior, String nuevo, String detalle) throws SQLException {
        Sql.insertar(con, "INSERT INTO bitacora (id_cuenta, fecha_hora, tabla, id_registro, accion, estado_anterior, estado_nuevo, detalle) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                cuenta, cuando, tabla, registro, accion, anterior, nuevo, detalle);
    }
}
