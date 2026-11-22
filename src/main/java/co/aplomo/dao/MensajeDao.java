package co.aplomo.dao;

import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-002/033 (propuesta): bandeja privada por destinatario, sin borrados.
 */
public final class MensajeDao {
    /**
     * RF-002: datos mínimos de un aviso y su cita, nunca fotos ni resultados.
     */
    public record Mensaje(int id, Integer reserva, int remitente, int destinatario, String tipo, String texto,
                          LocalDateTime creado, boolean leido, String nombre, String rol, String cita, String color,
                          boolean respondido, Integer solicitudAcceso) {
    }

    private static final String SELECT = "SELECT m.*,q.nombre,q.rol,ev.color_agenda," +
            "CONCAT(DATE_FORMAT(r.fecha,'%d/%m/%Y'),' · ',TIME_FORMAT(r.hora_inicio,'%H:%i'),' · '," +
            "e.nombre,' · ',s.nombre,' · ',l.nombre) cita," +
            "EXISTS(SELECT 1 FROM bitacora b WHERE b.tabla='mensaje' AND b.id_registro=m.id_mensaje " +
            "AND b.accion='EDITAR' AND b.detalle='Respuesta enviada') respondido," +
            "(SELECT CAST(SUBSTRING(b.detalle,8) AS UNSIGNED) FROM bitacora b WHERE b.tabla='mensaje' " +
            "AND b.id_registro=m.id_mensaje AND b.accion='CREAR' AND b.detalle LIKE 'ACCESO:%' " +
            "ORDER BY b.id_bitacora LIMIT 1) solicitud_acceso " +
            "FROM mensaje m JOIN cuenta q ON q.id_cuenta=m.id_remitente " +
            "LEFT JOIN reserva r ON r.id_reserva=m.id_reserva LEFT JOIN salon l ON l.id_salon=r.id_salon " +
            "LEFT JOIN sede s ON s.id_sede=l.id_sede LEFT JOIN empresa e ON e.id_empresa=s.id_empresa " +
            "LEFT JOIN cuenta ev ON ev.id_cuenta=r.id_evaluador ";

    /**
     * RF-002: filtra antes de devolver cualquier dato.
     */
    public List<Mensaje> listar(Connection con, int destinatario) throws SQLException {
        return Sql.listar(con, SELECT + "WHERE m.id_destinatario=? ORDER BY m.creado DESC,m.id_mensaje DESC",
                MensajeDao::leer, destinatario);
    }

    /**
     * RF-002: bloquea únicamente el mensaje del destinatario.
     */
    public Mensaje exigir(Connection con, int id, int destinatario) throws SQLException {
        Mensaje mensaje = Sql.uno(con, SELECT + "WHERE m.id_mensaje=? AND m.id_destinatario=? FOR UPDATE",
                MensajeDao::leer, id, destinatario);
        if (mensaje == null) throw new SecurityException("Acceso denegado.");
        return mensaje;
    }

    /**
     * RF-033: guarda en la misma transacción de la acción que origina el aviso.
     */
    public int enviar(Connection con, Integer reserva, int remitente, int destinatario, String tipo, String texto,
            LocalDateTime fecha) throws SQLException {
        return Sql.insertar(con, "INSERT INTO mensaje(id_reserva,id_remitente,id_destinatario,tipo,texto,creado) " +
                "VALUES(?,?,?,?,?,?)", reserva, remitente, destinatario, tipo, texto, fecha);
    }

    /**
     * RF-033: solo se marca leído después del acuse explícito.
     */
    public void entender(Connection con, Mensaje mensaje) throws SQLException {
        Sql.actualizar(con, "UPDATE mensaje SET leido=TRUE WHERE id_mensaje=? AND id_destinatario=?", mensaje.id(),
                mensaje.destinatario());
        if (mensaje.tipo().equals("NOTA_ADMIN") && mensaje.reserva() != null) {
            Sql.actualizar(con, "UPDATE reserva r SET nota_vista=TRUE WHERE r.id_reserva=? " +
                    "AND NOT EXISTS(SELECT 1 FROM mensaje WHERE id_reserva=r.id_reserva " +
                    "AND tipo='NOTA_ADMIN' AND leido=FALSE)", mensaje.reserva());
        }
    }

    /**
     * RF-033: conversación cronológica del actor con el Evaluador actual, nunca de otros destinatarios.
     */
    public List<Mensaje> conversacion(Connection con, int reserva, int actor, int evaluador) throws SQLException {
        return Sql.listar(con, SELECT + "WHERE m.id_reserva=? AND (m.id_remitente=? OR m.id_destinatario=?) " +
                "AND ((m.tipo='NOTA_ADMIN' AND q.rol='ADMINISTRADOR' AND m.id_destinatario=?) " +
                "OR (m.tipo='RESPUESTA' AND q.rol='EVALUADOR' AND m.id_remitente=? " +
                "AND EXISTS(SELECT 1 FROM cuenta a WHERE a.id_cuenta=m.id_destinatario AND a.rol='ADMINISTRADOR'))) " +
                "ORDER BY m.creado,m.id_mensaje", MensajeDao::leer, reserva, actor, actor, evaluador, evaluador);
    }

    /**
     * RF-033: historial general de dos cuentas concretas; no incluye avisos del Usuario ni otras citas.
     */
    public List<Mensaje> general(Connection con, int administrador, int evaluador) throws SQLException {
        return Sql.listar(con, SELECT + "WHERE m.id_reserva IS NULL AND " +
                "((m.tipo='AVISO' AND m.id_remitente=? AND m.id_destinatario=?) OR " +
                "(m.tipo IN ('RESPUESTA','AVISO') AND m.id_remitente=? AND m.id_destinatario=?)) " +
                "ORDER BY m.creado,m.id_mensaje", MensajeDao::leer, administrador, evaluador, evaluador, administrador);
    }

    private static Mensaje leer(java.sql.ResultSet fila) throws SQLException {
        return new Mensaje(fila.getInt("id_mensaje"), Sql.enteroOpcional(fila, "id_reserva"),
                fila.getInt("id_remitente"), fila.getInt("id_destinatario"), fila.getString("tipo"),
                fila.getString("texto"), fila.getTimestamp("creado").toLocalDateTime(), fila.getBoolean("leido"),
                fila.getString("nombre"), fila.getString("rol"), fila.getString("cita"), fila.getString("color_agenda"),
                fila.getBoolean("respondido"), Sql.enteroOpcional(fila, "solicitud_acceso"));
    }
}
