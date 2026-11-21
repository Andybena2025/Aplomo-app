package co.aplomo.instalacion;

import co.aplomo.dao.MensajeDao;
import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * RF-033 (propuesta): avisos de presentación repetibles, solo en empresas demo conocidas.
 */
final class DemoAvisos {
    private DemoAvisos() {
    }

    static void cargar(Connection con, int administrador, List<Integer> evaluadores) throws SQLException {
        Sql.actualizar(con, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                "JOIN empresa e USING(id_empresa) SET r.id_evaluador=(SELECT q.id_cuenta " +
                "FROM cuenta_empresa ce JOIN cuenta q USING(id_cuenta) WHERE ce.id_empresa=e.id_empresa " +
                "AND ce.activo=TRUE AND q.rol='EVALUADOR' AND q.estado='ACTIVA' " +
                "ORDER BY (q.id_cuenta=r.id_administrador) DESC,q.id_cuenta LIMIT 1) " +
                "WHERE r.id_evaluador IS NULL AND e.nit IN ('900.512.384-1','901.638.205-4','012345'," +
                "'900.782.156-2','901.428.603-7','900.315.928-5','901.267.840-3')");
        for (int evaluador : evaluadores) {
            for (boolean leido : new boolean[]{false, true}) {
                String texto = leido ? "Gracias por confirmar la visita de la semana pasada." :
                        "Antes de la visita, " + "revisa con el " + "contacto cuáles " + "puestos necesitan " +
                        "atención.";
                Integer existe = Sql.uno(con, "SELECT id_mensaje FROM mensaje WHERE id_destinatario=? " +
                                "AND tipo='AVISO' AND texto=? LIMIT 1", f -> f.getInt(1), evaluador, texto);
                if (existe != null) continue;
                Integer reserva = Sql.uno(con, "SELECT r.id_reserva FROM reserva r JOIN salon l USING(id_salon) " +
                                "JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) WHERE r.id_evaluador=? " +
                                "AND e.nit IN ('900.512.384-1','901.638.205-4','012345','900.782.156-2'," +
                                "'901.428.603-7','900.315.928-5','901.267.840-3') " + "ORDER BY r.fecha DESC LIMIT 1",
                        f -> f.getInt(1), evaluador);
                int mensaje = new MensajeDao().enviar(con, reserva, administrador, evaluador, "AVISO", texto,
                        LocalDateTime.now(ZoneId.of("America/Bogota")));
                if (leido) Sql.actualizar(con, "UPDATE mensaje SET leido=TRUE WHERE id_mensaje=?", mensaje);
            }
        }
    }
}
