package co.aplomo.db;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** RNF-001: comprobación real de JDBC, solo lectura y sin modificar la base. */
class ConexionTest {
    @Test
    void primeraConexionConCargadorDeHiloWeb() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process proceso = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                PrimeraConexion.class.getName()).redirectErrorStream(true).start();
        try {
            assertTrue(proceso.waitFor(20, TimeUnit.SECONDS), "La conexión no terminó a tiempo.");
            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, proceso.exitValue(), salida);
            assertTrue(salida.contains("CONEXION_OK"), salida);
        } finally {
            if (proceso.isAlive()) proceso.destroyForcibly();
        }
    }

    /** RNF-001: inicia JDBC por primera vez en otra JVM, como un hilo HTTP de Maven. */
    public static class PrimeraConexion {
        /** Comprueba la primera conexión sin depender de controladores cargados por otras pruebas. */
        public static void main(String[] args) throws Exception {
            Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());
            try (var conexion = Conexion.obtener();
                 var consulta = conexion.prepareStatement("SELECT 1");
                 var filas = consulta.executeQuery()) {
                if (!filas.next() || filas.getInt(1) != 1) {
                    throw new IllegalStateException("La conexión no respondió correctamente.");
                }
                System.out.println("CONEXION_OK");
            }
        }
    }

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
