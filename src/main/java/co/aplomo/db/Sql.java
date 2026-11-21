package co.aplomo.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * RNF-001, RNF-004: operaciones JDBC comunes con parámetros y recursos cerrados.
 */
public final class Sql {
    private Sql() {
    }

    /**
     * Convierte una fila de una consulta en el modelo correspondiente. RNF-011.
     */
    @FunctionalInterface
    public interface Lector<T> {
        T leer(ResultSet fila) throws SQLException;
    }

    /**
     * Lee todas las filas de una consulta parametrizada. RNF-011.
     */
    public static <T> List<T> listar(Connection con, String texto, Lector<T> lector,
            Object... valores) throws SQLException {
        try (var consulta = preparar(con, texto, false, valores); var filas = consulta.executeQuery()) {
            List<T> resultado = new ArrayList<>();
            while (filas.next()) resultado.add(lector.leer(filas));
            return List.copyOf(resultado);
        }
    }

    /**
     * Lee una fila o devuelve null si no existe dentro del alcance de la consulta. RNF-011.
     */
    public static <T> T uno(Connection con, String texto, Lector<T> lector, Object... valores) throws SQLException {
        List<T> filas = listar(con, texto, lector, valores);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * Inserta un registro y devuelve la clave asignada por MySQL. RNF-011.
     */
    public static int insertar(Connection con, String texto, Object... valores) throws SQLException {
        try (var consulta = preparar(con, texto, true, valores)) {
            consulta.executeUpdate();
            try (var claves = consulta.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se pudo identificar el registro guardado.");
                return claves.getInt(1);
            }
        }
    }

    /**
     * Ejecuta una actualización parametrizada dentro de la transacción del servicio. RNF-011.
     */
    public static void actualizar(Connection con, String texto, Object... valores) throws SQLException {
        try (var consulta = preparar(con, texto, false, valores)) {
            consulta.executeUpdate();
        }
    }

    /**
     * Conserva la diferencia entre un entero nulo y cero al leer relaciones opcionales. RNF-011.
     */
    public static Integer enteroOpcional(ResultSet fila, String columna) throws SQLException {
        int valor = fila.getInt(columna);
        return fila.wasNull() ? null : valor;
    }

    private static PreparedStatement preparar(Connection con, String texto, boolean claves,
            Object[] valores) throws SQLException {
        PreparedStatement consulta = con.prepareStatement(texto,
                claves ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        try {
            for (int i = 0; i < valores.length; i++) consulta.setObject(i + 1, valores[i]);
            return consulta;
        } catch (SQLException | RuntimeException error) {
            consulta.close();
            throw error;
        }
    }
}
