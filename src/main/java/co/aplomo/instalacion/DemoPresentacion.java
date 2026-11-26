package co.aplomo.instalacion;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.db.Sql;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * RNF-011: agenda planeada, ocho citas mensuales por persona, sin recurrencias artificiales.
 */
final class DemoPresentacion {
    private static final int[][] EMPRESAS = {{2, 1}, {0, 3}, {4, 5}};
    private static final int[][] DIAS = {{3, 5, 12, 12, 18, 24, 27, 30}, {2, 6, 10, 13, 19, 25, 27, 26}, {4, 6, 11,
            16, 20, 23, 27, 25}};
    private static final int[][] MINUTOS = {{450, 600, 480, 780, 570, 840, 480, 690}, {660, 510, 780, 600, 420, 540,
            570, 840}, {480, 810, 600, 450, 660, 540, 660, 870}};
    private static final int[] DURACION = {1, 2, 2, 1, 3, 1, 2, 1};

    private DemoPresentacion() {
    }

    static List<Integer> empresas(Connection con, int admin) throws SQLException {
        return EstructuraDemo.crear(con, admin);
    }

    static void citas(Connection con, Path fotos, int admin, List<Integer> empresas, List<Integer> evaluadores,
                      LocalDate presentacion) throws SQLException, IOException {
        int existe = Sql.uno(con, "SELECT COUNT(*) FROM bitacora WHERE tabla='empresa' AND detalle='DEMO-33-PLAN'",
                f -> f.getInt(1));
        if (existe > 0) return;
        ResultadosDemo.preparar(con, fotos, admin, empresas, evaluadores);
        for (int persona = 0; persona < 3; persona++) {
            for (int turno = 0; turno < 8; turno++) {
                int empresa = empresas.get(EMPRESAS[persona][turno % 2]);
                LocalDate fecha = presentacion.withDayOfMonth(DIAS[persona][turno]);
                LocalTime inicio = LocalTime.ofSecondOfDay(MINUTOS[persona][turno] * 60L);
                int salon = Sql.uno(con, "SELECT l.id_salon FROM salon l JOIN sede s USING(id_sede) " +
                        "WHERE s.id_empresa=? ORDER BY l.id_salon LIMIT 1", f -> f.getInt(1), empresa);
                boolean cancelada = persona == 0 && turno == 1 || persona == 1 && turno == 2;
                // La cita "Esperando respuesta" nunca puede quedar en el pasado: si ya pasaron ambas fechas, no hay.
                LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
                int respuesta = presentacion.withDayOfMonth(DIAS[persona][5]).isAfter(hoy) ? 5 :
                        presentacion.withDayOfMonth(DIAS[persona][7]).isBefore(hoy) ? -1 : 7;
                boolean esperando = turno == respuesta;
                String estado = cancelada ? "CANCELADA" : esperando ? "ESPERANDO" : "EN_COLA";
                String codigo = turno == 6 ? (persona == 0 ? "V26" : "V27" + persona) +
                        presentacion.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) :
                        "D28-" + persona + "-" + turno;
                int reserva = Sql.insertar(con, "INSERT INTO reserva(id_salon,id_administrador,id_evaluador," +
                                "codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado,motivo_cancelacion) " +
                                "VALUES(?,?,?,?,?,?,?,'EVALUACION_INICIAL',?,?,?)", salon, evaluadores.get(persona),
                        evaluadores.get(persona), codigo, fecha, inicio, inicio.plusHours(DURACION[turno]),
                        "Evaluación de las tareas del turno", estado, cancelada ? "Cambio de turno" : null);
                if (!cancelada && !esperando && fecha.isBefore(hoy)) {
                    ResultadosDemo.finalizar(con, fotos, evaluadores.get(persona), reserva, salon, fecha, inicio,
                            DURACION[turno]);
                }
            }
        }
        ResultadosDemo.seguimiento(con, admin, empresas);
        Sql.actualizar(con, "UPDATE empresa SET activo=FALSE WHERE id_empresa=?", empresas.get(6));
        new BitacoraDao().registrar(con, admin, java.time.LocalDateTime.now(ZoneId.of("America/Bogota")), "empresa",
                empresas.get(0), "EDITAR", null, null, "DEMO-33-PLAN");
    }

    static void actualizarMarca(Connection con) throws SQLException {
        // Las identidades de cargas anteriores solo se sustituyen mediante el reinicio explícito y confirmado.
    }
}
