package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Reserva;
import java.sql.*;
import java.util.*;

/** RF-009b a RF-012: reservas con contexto de empresa y consultas filtradas en SQL. */
public final class ReservaDao {
    private static final String CONTEXTO = "SELECT r.*, s.id_empresa, l.id_sede, e.nombre AS empresa_nombre, "
            + "s.nombre AS sede_nombre, l.nombre AS salon_nombre, v.id_visita, v.estado AS visita_estado "
            + "FROM reserva r JOIN salon l ON l.id_salon = r.id_salon JOIN sede s ON s.id_sede = l.id_sede "
            + "JOIN empresa e ON e.id_empresa = s.id_empresa LEFT JOIN visita v ON v.id_reserva = r.id_reserva";

    /** Reserva y etiquetas necesarias para mostrar su contexto sin consultas por cada fila. */
    public record Ficha(Reserva reserva, int empresa, int sede, String empresaNombre, String sedeNombre,
                        String salonNombre, Integer visita, String estadoVisita) {}

    /** Crea el acceso JDBC a las reservas. */
    public ReservaDao() {}

    /** Lista reservas de todas las empresas para Administrador o únicamente de la empresa permitida. */
    public List<Ficha> listar(Connection con, int empresa) throws SQLException {
        return Sql.listar(con, CONTEXTO + " WHERE (? = 0 OR s.id_empresa = ?) ORDER BY r.fecha DESC, r.hora_inicio DESC, r.id_reserva DESC",
                ReservaDao::leer, empresa, empresa);
    }

    /** Consulta y opcionalmente bloquea la reserva, siempre dentro del alcance autorizado. */
    public Ficha buscar(Connection con, int id, int empresa, boolean bloquear) throws SQLException {
        return Sql.uno(con, CONTEXTO + " WHERE r.id_reserva = ? AND (? = 0 OR s.id_empresa = ?)" + (bloquear ? " FOR UPDATE" : ""),
                ReservaDao::leer, id, empresa, empresa);
    }

    /** Inserta la reserva con un código provisional visible únicamente dentro de la transacción. */
    public int crear(Connection con, Reserva reserva, String provisional) throws SQLException {
        return Sql.insertar(con, "INSERT INTO reserva (id_salon, id_administrador, codigo, fecha, hora_inicio, hora_fin, categoria, descripcion, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reserva.salon(), reserva.administrador(), provisional, reserva.fecha(), reserva.inicio(), reserva.fin(),
                reserva.categoria(), reserva.descripcion(), reserva.notas());
    }

    /** Asigna el código definitivo derivado del ID automático dentro de la misma transacción. */
    public void codigo(Connection con, int id, String codigo) throws SQLException {
        Sql.actualizar(con, "UPDATE reserva SET codigo = ? WHERE id_reserva = ?", codigo, id);
    }

    /** Actualiza exclusivamente los campos editables; nunca código, salón, creador o estado. */
    public void editar(Connection con, Reserva reserva) throws SQLException {
        Sql.actualizar(con, "UPDATE reserva SET fecha = ?, hora_inicio = ?, hora_fin = ?, categoria = ?, descripcion = ?, notas = ? WHERE id_reserva = ?",
                reserva.fecha(), reserva.inicio(), reserva.fin(), reserva.categoria(), reserva.descripcion(), reserva.notas(), reserva.id());
    }

    /** Guarda un cambio de estado autorizado por el servicio y su motivo cuando corresponde. */
    public void estado(Connection con, int id, String estado, String motivo) throws SQLException {
        Sql.actualizar(con, "UPDATE reserva SET estado = ?, motivo_cancelacion = ? WHERE id_reserva = ?", estado, motivo, id);
    }

    /** Busca duplicados de salón, fecha y hora de inicio, incluidos los estados finales. */
    public Reserva duplicada(Connection con, Reserva reserva) throws SQLException {
        return Sql.uno(con, "SELECT * FROM reserva WHERE id_salon = ? AND fecha = ? AND hora_inicio = ? AND id_reserva <> ?",
                ReservaDao::reserva, reserva.salon(), reserva.fecha(), reserva.inicio(), reserva.id());
    }

    private static Ficha leer(ResultSet fila) throws SQLException {
        return new Ficha(reserva(fila), fila.getInt("id_empresa"), fila.getInt("id_sede"),
                fila.getString("empresa_nombre"), fila.getString("sede_nombre"), fila.getString("salon_nombre"),
                Sql.enteroOpcional(fila, "id_visita"), fila.getString("visita_estado"));
    }

    private static Reserva reserva(ResultSet fila) throws SQLException {
        return new Reserva(fila.getInt("id_reserva"), fila.getInt("id_salon"), fila.getInt("id_administrador"),
                fila.getString("codigo"), fila.getDate("fecha").toLocalDate(), fila.getTime("hora_inicio").toLocalTime(),
                fila.getTime("hora_fin").toLocalTime(), fila.getString("categoria"), fila.getString("descripcion"),
                fila.getString("notas"), fila.getString("estado"), fila.getString("motivo_cancelacion"));
    }
}
