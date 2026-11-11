package co.aplomo.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** RNF-001, RNF-004: una conexión por operación; confirma todo o revierte todo. */
public final class Transacciones {
    /** Proporciona una conexión nueva; permite verificar con transacciones aisladas. */
    @FunctionalInterface public interface Fuente { Connection abrir() throws SQLException; }
    /** Operación que se ejecuta dentro de la misma transacción. */
    @FunctionalInterface public interface Trabajo<T> { T ejecutar(Connection con) throws SQLException; }
    private final Fuente fuente;

    /** Usa la conexión local configurada sin copiar credenciales (RNF-004). */
    public Transacciones() { this(Conexion::obtener); }
    /** Recibe el proveedor de conexiones (RNF-011). */
    public Transacciones(Fuente fuente) { this.fuente = Objects.requireNonNull(fuente); }

    /** Ejecuta y confirma la operación; revierte errores y cierra su conexión (RNF-001). */
    public <T> T ejecutar(Trabajo<T> trabajo) throws SQLException {
        try (Connection con = fuente.abrir()) {
            con.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
                throw e;
            }
        }
    }
}
