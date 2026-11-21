package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.servicio.Agenda.Evaluador;
import co.aplomo.servicio.Agenda.Nota;
import co.aplomo.servicio.Agenda.Aviso;
import co.aplomo.seguridad.AccesoDatos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * RF-033, RNF-011: consultas parametrizadas de agenda, separadas de reglas y HTML.
 */
public class AgendaDao {

    /**
     * RF-033: cuentas para el filtro, incluido su color persistente.
     */
    public List<Evaluador> evaluadores(Connection conexion2) throws SQLException {
        return Sql.listar(conexion2, "SELECT id_cuenta,nombre,estado,color_agenda FROM cuenta WHERE rol='EVALUADOR" +
                        "' ORDER BY nombre,id_cuenta",
                rs -> new Evaluador(rs.getInt(1), rs.getString(2), !rs.getString(3).equals("INACTIVA"),
                        rs.getString(4)));
    }

    /**
     * RF-033: Evaluador asignado con preferencia por una cuenta activa.
     */
    public Map.Entry<Integer, String> responsable(Connection conexion2, int empresa) throws SQLException {
        return Sql.uno(conexion2, "SELECT q.id_cuenta,q.nombre FROM cuenta_empresa ce JOIN cuenta q USING(id_cu" +
                "enta) WHERE ce.id_empresa=? AND ce.activo=TRUE AND q.rol='EVALUADOR' ORDER B" +
                "Y (q.estado='INACTIVA'),q.id_cuenta LIMIT 1", rs -> Map.entry(rs.getInt(1), rs.getString(2)), empresa);
    }

    /**
     * RF-033: responsable persistido en esta cita, independiente de otros asignados a la empresa.
     */
    public Map.Entry<Integer, String> responsableReserva(Connection con, int reserva) throws SQLException {
        return Sql.uno(con, "SELECT q.id_cuenta,q.nombre FROM reserva r JOIN cuenta q " +
                        "ON q.id_cuenta=r.id_evaluador WHERE r.id_reserva=?", f -> Map.entry(f.getInt(1),
                        f.getString(2)),
                reserva);
    }

    /**
     * RF-033: texto y acuse de una reserva cuyo alcance ya se comprobó.
     */
    public Nota nota(Connection conexion2, int id) throws SQLException {
        return Sql.uno(conexion2, "SELECT nota_administrador,nota_vista FROM reserva WHERE id_reserva=?",
                fila2 -> new Nota(fila2.getString(1), fila2.getBoolean(2)), id);
    }

    /**
     * RF-033: guarda nota y reinicia el acuse.
     */
    public void guardarNota(Connection conexion2, int reserva, String texto) throws SQLException {
        Sql.actualizar(conexion2, "UPDATE reserva SET nota_administrador=?,nota_vista=FALSE WHERE id_reserva=?",
                texto.isBlank() ? null : texto, reserva);
    }

    /**
     * RF-033: registra el acuse de lectura.
     */
    public void entender(Connection conexion2, int reserva) throws SQLException {
        Sql.actualizar(conexion2, "UPDATE reserva SET nota_vista=TRUE WHERE id_reserva=?", reserva);
    }

    /**
     * RF-002/033: IDs pendientes dentro del alcance.
     */
    public List<Integer> nuevas(Connection conexion2, AccesoDatos.Alcance al) throws SQLException {
        return Sql.listar(conexion2, "SELECT r.id_reserva FROM reserva r JOIN salon l USING(id_salon) JOIN sede s " +
                "USING(id_sede) WHERE " + al.sql("s.id_empresa") +
                " AND r.id_evaluador=? AND r.nota_administrador IS NOT NULL AND r.nota_vista=FALSE ORDER BY r.fech" +
                "a,r.hora_inicio,r.id_reserva", rs -> rs.getInt(1), al.todas(), al.valor(), al.cuenta());
    }

    /**
     * RF-002/033: datos mínimos del aviso privado.
     */
    public List<Aviso> avisos(Connection conexion, AccesoDatos.Alcance alcance) throws SQLException {
        return Sql.listar(conexion,
                "SELECT r.id_reserva,r.fecha,r.hora_inicio,e.nombre,r.nota_administrador FROM" + " reserva r " +
                        "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING" +
                        "(id_empresa) " + "WHERE " + alcance.sql("s.id_empresa") +
                        " AND r.id_evaluador=? AND r.nota_administrador IS NOT NULL AND r.nota_vista=FALSE ORDER BY r" +
                        ".fech" + "a,r.hora_inicio", fila -> new Aviso(fila.getInt(1), fila.getDate(2).toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        fila.getTime(3).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("H:mm")),
                        fila.getString(4), fila.getString(5)), alcance.todas(), alcance.valor(), alcance.cuenta());
    }
}
