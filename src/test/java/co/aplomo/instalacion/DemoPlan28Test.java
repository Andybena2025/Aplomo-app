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
        var distribucion = Sql.listar(con, "SELECT ultimo_puntaje,COUNT(*) FROM puesto_trabajo p " +
                        "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa) " +
                        "WHERE em.nit IN " + nits() + " GROUP BY ultimo_puntaje ORDER BY ultimo_puntaje",
                f -> List.of(f.getInt(1), f.getInt(2)));
        int verdes = 0, amarillos = 0, rojos = 0, sietes = 0;
        for (var grupo : distribucion) {
            if (grupo.get(0) <= 2) verdes += grupo.get(1);
            else if (grupo.get(0) <= 4) amarillos += grupo.get(1);
            else rojos += grupo.get(1);
            if (grupo.get(0) == 7) sietes += grupo.get(1);
        }
        assertEquals(16, verdes);
        assertEquals(14, amarillos);
        assertEquals(10, rojos);
        assertTrue(sietes <= 2);
        System.out.println("DEMO28: 40 puestos; 16 Verde, 14 Amarillo, 10 Rojo; " + sietes +
                " con 7; 5 pendientes abiertos; 24 citas mensuales.");
    }

    private int contar(String sql) throws Exception {
        return Sql.uno(con, sql, f -> f.getInt(1));
    }

    private String nits() {
        return "('900.512.384-1','901.638.205-4','012345','900.782.156-2','901.428.603-7'," +
                "'900.315.928-5','901.267.840-3')";
    }
}
