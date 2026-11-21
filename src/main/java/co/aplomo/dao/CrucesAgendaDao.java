package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Reserva;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * RF-033 (propuesta): exclusión de citas simultáneas del evaluador activo.
 */
public final class CrucesAgendaDao {

    /**
     * RF-033: bloquea la cuenta responsable y comprueba intervalos de todas sus empresas.
     */
    public void comprobar(Connection conexion, Reserva reserva) throws SQLException {
        Integer evaluador = Sql.uno(conexion,
                "SELECT q.id_cuenta FROM cuenta q JOIN cuenta_empresa ce USING(id_cuenta) " +
                        "JOIN sede s ON s.id_empresa=ce.id_empresa JOIN salon l USING(id_sede) " +
                        "WHERE l.id_salon=? AND ce.activo=TRUE AND q.rol='EVALUADOR' " +
                        "AND q.estado<>'INACTIVA' ORDER BY q.id_cuenta LIMIT 1 FOR UPDATE", fila -> fila.getInt(1),
                reserva.salon());
        if (evaluador == null) {
            return;
        }
        LocalTime[] cruce = Sql.uno(conexion,
                "SELECT r.hora_inicio,r.hora_fin FROM reserva r JOIN salon l USING(id_salon) " +
                        "JOIN sede s USING(id_sede) JOIN cuenta_empresa ce ON ce.id_empresa=s.id_empresa " +
                        "WHERE ce.id_cuenta=? AND ce.activo=TRUE AND r.estado<>'CANCELADA' " +
                        "AND r.fecha=? AND r.id_reserva<>? AND r.hora_inicio<? AND r.hora_fin>? " +
                        "ORDER BY r.hora_inicio LIMIT 1 FOR UPDATE",
                fila -> new LocalTime[]{fila.getTime(1).toLocalTime(), fila.getTime(2).toLocalTime()}, evaluador,
                reserva.fecha(), reserva.id(), reserva.fin(), reserva.inicio());
        if (cruce != null) {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("H:mm");
            throw new IllegalArgumentException(
                    "Ese evaluador ya tiene una cita de " + cruce[0].format(formato) + " a " +
                            cruce[1].format(formato) + " ese día. Elige otra hora.");
        }
    }
}
