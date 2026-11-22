package co.aplomo.dao;

import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-002/003 (propuesta): datos de contacto pendientes de revisión, sin credenciales.
 */
public final class SolicitudAccesoDao {
    /** RF-002/003: el destino de la solicitud lo determina la ruta pública, no el navegador. */
    public enum Tipo { EMPRESA, EVALUADOR }

    /**
     * RF-002: formulario original y resolución; no contiene contraseña en ningún estado.
     */
    public record Solicitud(int id, String nombre, String cargo, String correo, String telefono, String empresa,
                            String nit, String ciudad, String sector, String necesidad, String estado,
                            LocalDateTime creada, Integer empresaId, Integer solicitante, Tipo tipo) {
    }

    /**
     * RF-003: inserción previa a cualquier creación de cuenta.
     */
    public int crear(Connection con, Solicitud s) throws SQLException {
        return Sql.insertar(con, "INSERT INTO solicitud_acceso(nombre,cargo,correo,telefono,empresa,nit,ciudad," +
                        "sector,necesidad,creada,id_empresa,id_solicitante,tipo) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                s.nombre(),
                s.cargo(), s.correo(), s.telefono(), s.empresa(), s.nit(), s.ciudad(), s.sector(), s.necesidad(),
                s.creada(), s.empresaId(), s.solicitante(), s.tipo().name());
    }

    /**
     * RF-003: la aprobación bloquea esta fila para impedir dos cuentas o dos contraseñas.
     */
    public Solicitud buscar(Connection con, int id, boolean bloquear) throws SQLException {
        return Sql.uno(con, "SELECT * FROM solicitud_acceso WHERE id_solicitud=?" + (bloquear ? " FOR UPDATE" : ""),
                SolicitudAccesoDao::leer, id);
    }

    /**
     * RF-003: pendientes ordenadas para revisión de la Administradora.
     */
    public List<Solicitud> pendientes(Connection con) throws SQLException {
        return Sql.listar(con,
                "SELECT * FROM solicitud_acceso WHERE estado='PENDIENTE' " + "ORDER BY creada,id_solicitud",
                SolicitudAccesoDao::leer);
    }

    /**
     * RF-003/005: resolución conserva los datos iniciales y quién tomó la decisión.
     */
    public void resolver(Connection con, int id, int revisor, String estado, Integer empresa, String motivo,
            LocalDateTime fecha) throws SQLException {
        Sql.actualizar(con, "UPDATE solicitud_acceso SET estado=?,id_revisor=?,revisada=?," +
                "id_empresa=?,motivo_rechazo=? WHERE id_solicitud=?", estado, revisor, fecha, empresa, motivo, id);
    }

    private static Solicitud leer(java.sql.ResultSet f) throws SQLException {
        return new Solicitud(f.getInt("id_solicitud"), f.getString("nombre"), f.getString("cargo"),
                f.getString("correo"), f.getString("telefono"), f.getString("empresa"), f.getString("nit"),
                f.getString("ciudad"), f.getString("sector"), f.getString("necesidad"), f.getString("estado"),
                f.getTimestamp("creada").toLocalDateTime(), Sql.enteroOpcional(f, "id_empresa"),
                Sql.enteroOpcional(f, "id_solicitante"), Tipo.valueOf(f.getString("tipo")));
    }
}
