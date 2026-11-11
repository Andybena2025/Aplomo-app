package co.aplomo.db;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** RNF-001: comprobación real de JDBC, solo lectura y sin modificar la base. */
class ConexionTest {
    @Test
    void abreConexionesIndependientesAMySQL() throws Exception {
        try (var primera = Conexion.obtener(); var segunda = Conexion.obtener()) {
            assertNotSame(primera, segunda);
            assertEquals("MySQL", primera.getMetaData().getDatabaseProductName());
            assertTrue(primera.isValid(3));
            assertTrue(segunda.isValid(3));
            try (var consulta = primera.prepareStatement("SELECT COUNT(*) FROM valor_tabla_rula");
                 var filas = consulta.executeQuery()) {
                assertTrue(filas.next());
                assertEquals(272, filas.getInt(1));
            }
        }
    }
}
