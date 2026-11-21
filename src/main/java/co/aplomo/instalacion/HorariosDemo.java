package co.aplomo.instalacion;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Paso 26: concilia horarios heredados ficticios, sin borrar historia ni tocar empresas reales.
 */
final class HorariosDemo {
    private HorariosDemo() {
    }

    private record Cita(int id, int evaluador, LocalDate fecha, LocalTime inicio, LocalTime fin) {
    }

    static void conciliar(Connection conexion, int administrador) throws SQLException {
        List<Cita> anteriores = Sql.listar(conexion,
                "SELECT r.id_reserva,q.id_cuenta,r.fecha,r.hora_inicio,r.hora_fin FROM reserva r " +
                        "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING" +
                        "(id_empresa) " + "JOIN cuenta_empresa ce ON ce.id_empresa=e.id_empresa AND ce.activo=TRUE " +
                        "JOIN cuenta q ON q.id_cuenta=ce.id_cuenta AND q.rol='EVALUADOR' AND q.estado" + "='ACTIVA' " +
                        "WHERE e.nit IN ('900.512.384-1','901.638.205-4','012345') " +
                        "AND (r.codigo LIKE 'DEMO-%' OR r.codigo LIKE 'DEMO17-%' OR r.codigo LIKE 'DE" + "MO24-%' " +
                        "OR r.codigo LIKE 'D24-%') AND r.estado<>'CANCELADA' ORDER BY r.fecha,r.id_reserva",
                fila -> new Cita(fila.getInt(1), fila.getInt(2), fila.getDate(3).toLocalDate(),
                        fila.getTime(4).toLocalTime(), fila.getTime(5).toLocalTime()));
        LocalDate presentacion = LocalDate.parse(System.getProperty("aplomo.demo.fecha", "2026-11-27"));
        LocalDate semana = presentacion.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (Cita cita : anteriores) {
            boolean periodoNuevo =
                    !cita.fecha().isBefore(semana.minusWeeks(4)) && cita.fecha().isBefore(semana.plusWeeks(4));
            if (periodoNuevo) {
                reubicar(conexion, administrador, cita, semana.minusWeeks(6));
                continue;
            }
            if (cita.fecha().getDayOfWeek().getValue() <= 5 &&
                    !cruce(conexion, cita, cita.fecha(), cita.inicio(), cita.fin())) continue;
            reubicar(conexion, administrador, cita, cita.fecha());
        }
    }

    private static void reubicar(Connection conexion, int administrador, Cita cita,
            LocalDate desde) throws SQLException {
        long duracion = Duration.between(cita.inicio(), cita.fin()).toMinutes();
        for (LocalDate fecha = desde; fecha.isBefore(desde.plusDays(30)); fecha = fecha.plusDays(1)) {
            if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (int hora = 8; hora < 16; hora++) {
                LocalTime inicio = LocalTime.of(hora, 0);
                LocalTime fin = inicio.plusMinutes(duracion);
                if (fin.isAfter(LocalTime.of(16, 0)) || cruce(conexion, cita, fecha, inicio, fin)) continue;
                Sql.actualizar(conexion, "UPDATE reserva SET fecha=?,hora_inicio=?,hora_fin=? WHERE id_reserva=?",
                        fecha, inicio, fin, cita.id());
                new BitacoraDao().registrar(conexion, administrador, LocalDateTime.now(), "reserva", cita.id(),
                        "EDITAR", null, null, "Demo ALINEO.: conciliación de horario ficticio anterior");
                return;
            }
        }
        throw new IllegalArgumentException("No se encontró un horario libre para una cita antigua de demostración.")
                ;
    }

    private static boolean cruce(Connection conexion, Cita cita, LocalDate fecha, LocalTime inicio,
            LocalTime fin) throws SQLException {
        return Sql.<Integer>uno(conexion,
                "SELECT COUNT(*) FROM reserva r JOIN salon l USING(id_salon) JOIN sede s USIN" + "G(id_sede) " +
                        "JOIN cuenta_empresa ce ON ce.id_empresa=s.id_empresa AND ce.activo=TRUE " +
                        "WHERE ce.id_cuenta=? AND r.id_reserva<>? AND r.fecha=? AND r.estado<>'CANCELADA' " +
                        "AND r.hora_inicio<? AND r.hora_fin>?", fila -> fila.getInt(1), cita.evaluador(), cita.id(),
                fecha, fin, inicio) > 0;
    }
}
