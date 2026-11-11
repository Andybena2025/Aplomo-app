package co.aplomo.soporte;
import java.nio.file.*;import java.sql.*;
/** RF-023a: carga únicamente INSERT idempotentes del catálogo, en la transacción reversible de prueba. */
public final class ReglasSugerenciasPrueba {
    private ReglasSugerenciasPrueba(){}
    /** Lee el mismo archivo que se entrega para instalación; no ejecuta DDL ni el SQL inicial. */
    public static void cargar(Connection c)throws Exception{for(String linea:Files.readAllLines(Path.of("docs/base_de_datos/datos_sugerencias.sql")))if(linea.startsWith("INSERT INTO regla_sugerencia "))try(var p=c.prepareStatement(linea)){p.executeUpdate();}}
}
