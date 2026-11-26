package co.aplomo.instalacion;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-020/022 y RNF-011: contrato numérico de la demo, siempre en transacción reversible.
 */
class DemoPlan28Test extends BasePrueba {
    @TempDir
    Path fotos;

    @Test
    void planTieneAgendaNaturalPuntajesVariadosYPocosPendientes() throws Exception {
        Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
        assertEquals(40, contar("SELECT COUNT(*) FROM puesto_trabajo p JOIN salon l USING(id_salon) " +
                "JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa) WHERE em.nit IN " + nits()));
        assertEquals(0, contar("SELECT COUNT(*) FROM (SELECT p.descripcion,s.id_sede FROM puesto_trabajo p " +
                "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa) " +
                "WHERE em.nit IN " + nits() + " GROUP BY p.descripcion,s.id_sede HAVING COUNT(*)>1) repetidos"));
        assertEquals(0, contar("SELECT COUNT(*) FROM reserva WHERE codigo LIKE 'D28-%' AND " +
                "(MINUTE(hora_inicio) NOT IN(0,30) OR MINUTE(hora_fin) NOT IN(0,30))"));
        assertEquals(0, contar("SELECT COUNT(*) FROM reserva a JOIN reserva b ON a.id_evaluador=b.id_evaluador " +
                "AND a.fecha<b.fecha AND DATEDIFF(b.fecha,a.fecha)=7 AND a.hora_inicio=b.hora_inicio " +
                "WHERE a.codigo LIKE 'D28-%' AND b.codigo LIKE 'D28-%'"));
        assertEquals(0, contar("SELECT COUNT(*) FROM reserva a JOIN reserva b ON a.id_evaluador=b.id_evaluador " +
                "AND a.id_reserva<b.id_reserva AND a.fecha=b.fecha AND a.hora_inicio<b.hora_fin " +
                "AND b.hora_inicio<a.hora_fin WHERE a.codigo LIKE 'D28-%' AND b.codigo LIKE 'D28-%' " +
                "AND a.estado<>'CANCELADA' AND b.estado<>'CANCELADA'"));
        for (String correo : List.of("andres.evaluador@rula.local", "johan.evaluador@rula.local",
                "gabriella.evaluadora@rula.local")) {
            assertEquals(8, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM reserva r JOIN cuenta c " +
                    "ON c.id_cuenta=r.id_evaluador WHERE c.correo=? AND r.fecha BETWEEN '2026-11-01' AND " +
                    "'2026-11-30'", f -> f.getInt(1), correo));
        }
        assertEquals(5, contar("SELECT COUNT(*) FROM pendiente_arreglo pa JOIN evaluacion e USING(id_evaluacion) " +
                "JOIN puesto_trabajo p USING(id_puesto) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                "JOIN empresa em USING(id_empresa) WHERE em.nit IN " + nits() + " AND pa.estado='ABIERTO'"));
        var distribucion = Sql.listar(con, "SELECT MAX(a.puntaje_final) FROM visita v " +
                "JOIN reserva r USING(id_reserva) JOIN evaluacion e USING(id_visita) " +
                "JOIN fotografia f USING(id_evaluacion) JOIN analisis_rula a USING(id_foto) " +
                "WHERE v.estado='FINALIZADA' AND r.codigo LIKE 'D28-%' GROUP BY v.id_visita", f -> f.getInt(1));
        long verdes = distribucion.stream().filter(p -> p <= 2).count();
        long amarillos = distribucion.stream().filter(p -> p >= 3 && p <= 4).count();
        long rojos = distribucion.stream().filter(p -> p >= 5).count();
        // Las citas ya pasadas (según la fecha de hoy) se cargan finalizadas; por eso el total crece con los días.
        int pasadas = Sql.uno(con, "SELECT COUNT(*) FROM reserva WHERE codigo LIKE 'D28-%' AND " +
                "estado NOT IN('CANCELADA','ESPERANDO') AND fecha < ?", f -> f.getInt(1),
                java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")));
        assertEquals(pasadas, distribucion.size());
        assertTrue(distribucion.size() >= 20, distribucion.toString());
        // Paso 32: distribución por puesto/postura y progreso por fecha; ya no una mezcla fija por visita.
        assertTrue(verdes > 0, distribucion.toString());
        assertTrue(amarillos > 0, distribucion.toString());
        assertTrue(rojos > 0, distribucion.toString());
        assertTrue(distribucion.containsAll(List.of(1, 4, 7)));
        assertEquals(5,
                contar("SELECT COUNT(DISTINCT fecha_limite) FROM pendiente_arreglo " + "WHERE estado='ABIERTO'"));
        assertEquals(0, contar("SELECT COUNT(*) FROM (SELECT fecha FROM reserva WHERE fecha='2026-11-30' " +
                "AND codigo LIKE 'D28-%' GROUP BY fecha HAVING COUNT(DISTINCT id_evaluador)>1) q"));
        System.out.println("DEMO32: 23 visitas; colores variados y tendencia; " +
                "5 pendientes abiertos con fechas diferentes; 24 citas mensuales.");
    }

    private int contar(String sql) throws Exception {
        return Sql.uno(con, sql, f -> f.getInt(1));
    }

    private String nits() {
        return "('900.512.384-1','901.638.205-4','012345','900.782.156-2','901.428.603-7'," +
                "'900.315.928-5','901.267.840-3')";
    }
}
