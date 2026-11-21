package co.aplomo.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * RF-002, Paso 24: comprobación no destructiva antes de abrir el puerto.
 */
public final class Esquema {
    public static final String FALTA = "Falta actualizar la base de datos. Corre scripts/migrar_paso24.sh (Mac) o mi" +
            "grar_paso24.bat (Windows).";

    private Esquema() {
    }

    /**
     * Comprueba tabla, ENUM y columnas sin modificar ni instalar nada. RNF-011.
     */
    public static void verificar(Connection conexion) throws SQLException {
        int tabla = Sql.uno(conexion, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()" +
                " AND table_name='cuenta_empresa'", fila2 -> fila2.getInt(1));
        String rol = Sql.uno(conexion, "SELECT column_type FROM information_schema.columns WHERE table_schema=DATABA" +
                "SE() AND table_name='cuenta' AND column_name='rol'", fila3 -> fila3.getString(1));
        int notas = Sql.uno(conexion, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE(" +
                        ") AND table_name='reserva' AND column_name IN ('nota_administrador','nota_vi" + "sta')",
                fila4 -> fila4.getInt(1));
        int color = Sql.uno(conexion, "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
                "AND table_name='cuenta' AND column_name='color_agenda'", fila -> fila.getInt(1));
        int empresa = Sql.uno(conexion,
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
                        "AND table_name='empresa' AND column_name IN ('id_creador','sector')", fila -> fila.getInt(1));
        int responsable = Sql.uno(conexion,
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
                        "AND table_name='reserva' AND column_name='id_evaluador'", fila -> fila.getInt(1));
        int mensajes = Sql.uno(conexion, "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema=DATABASE() AND table_name='mensaje'", fila -> fila.getInt(1));
        if (empresa != 2 || responsable != 1 || mensajes != 1) {
            throw new IllegalStateException("Falta actualizar la base. Ejecuta scripts/migrar_paso27.sh " +
                    "(Mac) o migrar_paso27.bat (Windows).");
        }
        if (color != 1) {
            throw new IllegalStateException("Falta actualizar la base de datos. Corre " +
                    "scripts/migrar_paso26.sh (Mac) o migrar_paso26.bat (Windows).");
        }
        if (tabla != 1 || rol == null || !rol.contains("'EVALUADOR'") || notas != 2)
            throw new IllegalStateException(FALTA);
    }
}
