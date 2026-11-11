package co.aplomo.db;

import co.aplomo.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Entrega conexiones JDBC a la base de datos rula_ergonomia.
 * Cada peticion pide su propia conexion y la cierra al terminar (RNF-001).
 */
public class Conexion {

    private Conexion() {}

    /** Abre y devuelve una conexion nueva a MySQL. Quien la llame debe cerrarla. */
    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(Config.url(), Config.usuario(), Config.clave());
    }
}
