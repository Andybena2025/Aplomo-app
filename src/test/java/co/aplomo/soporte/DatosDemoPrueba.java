package co.aplomo.soporte;

import java.nio.file.Path;
import java.sql.Connection;

/**
 * Carga los mismos servicios reales en la transacción y carpeta aisladas de pruebas.
 */
public final class DatosDemoPrueba {
    private DatosDemoPrueba() {
    }

    public static void cargar(Connection con, Path fotos) throws Exception {
        if (!con.getMetaData().getURL().contains("/rula_ergonomia_pruebas?"))
            throw new IllegalStateException("Solo se permite la base de pruebas.");
        co.aplomo.instalacion.Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
    }
}
