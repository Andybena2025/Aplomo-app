package co.aplomo.rula;

import co.aplomo.db.Conexion;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-027, RNF-010: JDBC real, todas las combinaciones y contraste con el SQL aprobado. */
class TablasRulaMySQLTest {
    @TestFactory
    Stream<DynamicTest> todasLasCombinacionesContraMySQLYScriptAprobado() throws Exception {
        var tablas = DatosPrueba.tablasMySQL();
        Map<String, Integer> valores = new HashMap<>();
        try (var con = Conexion.obtener();
             var ps = con.prepareStatement("SELECT tabla, fila, columna, valor FROM valor_tabla_rula");
             var rs = ps.executeQuery()) {
            while (rs.next()) valores.put(rs.getString("tabla") + ":" + rs.getInt("fila") + ":"
                    + rs.getInt("columna"), rs.getInt("valor"));
        }
        assertEquals(272, valores.size());
        return DatosPrueba.celdas().stream().map(c -> DynamicTest.dynamicTest(
                c.tabla() + " fila " + c.fila() + " columna " + c.columna(), () -> {
                    assertEquals(c.valor(), valores.get(c.tabla() + ":" + c.fila() + ":" + c.columna()));
                    assertEquals(c.valor(), DatosPrueba.consultar(tablas, c));
                }));
    }

    @TestFactory
    Stream<DynamicTest> sietePuntajesConNivelLeidoDeMySQL() throws Exception {
        var tablas = DatosPrueba.tablasMySQL();
        String[] colores = {"VERDE", "VERDE", "AMARILLO", "AMARILLO", "ROJO", "ROJO", "ROJO"};
        int[] oficiales = {1, 1, 2, 2, 3, 3, 4};
        return IntStream.rangeClosed(1, 7).mapToObj(p -> DynamicTest.dynamicTest("Riesgo de " + p, () -> {
            var n = tablas.nivelDe(p);
            assertEquals(colores[p - 1], n.color());
            assertEquals(oficiales[p - 1], n.nivelOficialRula());
            try (var con = Conexion.obtener();
                 var ps = con.prepareStatement("SELECT color, nombre, accion, nivel_oficial_rula "
                         + "FROM nivel_riesgo WHERE id_nivel = ?")) {
                ps.setInt(1, n.idNivel());
                try (var rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(rs.getString("color"), n.color());
                    assertEquals(rs.getString("nombre"), n.nombre());
                    assertEquals(rs.getString("accion"), n.accion());
                    assertEquals(rs.getInt("nivel_oficial_rula"), n.nivelOficialRula());
                    assertFalse(rs.next());
                }
            }
        }));
    }
}
