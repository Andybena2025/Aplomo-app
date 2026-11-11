package co.aplomo.dao;

import co.aplomo.modelo.Salon;
import co.aplomo.db.Sql;
import java.sql.*;
import java.util.*;

/** RF-004 a RF-010, RF-028: consultas parametrizadas de salon. */
public final class SalonDao {
    /** Crea el acceso JDBC al catálogo de salon. */
    public SalonDao() {}

    /** Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. */
    public Salon buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {
        String consulta = "SELECT r.* FROM salon r JOIN sede s ON s.id_sede = r.id_sede WHERE r.id_salon = ? AND (? = 0 OR s.id_empresa = ?)" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, SalonDao::leer, id, empresaPermitida, empresaPermitida);
    }

    /** Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002). */
    public List<Salon> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM salon r JOIN sede s ON s.id_sede = r.id_sede WHERE r.id_sede = ? AND (? = 0 OR s.id_empresa = ?) ORDER BY r.nombre, r.id_salon",
                SalonDao::leer, padre, empresaPermitida, empresaPermitida);
    }

    /** Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. */
    public int guardar(Connection con, Salon dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO salon (id_sede, nombre, descripcion, piso, capacidad_puestos) VALUES (?, ?, ?, ?, ?)",
                    dato.sede(), dato.nombre(), dato.descripcion(), dato.piso(), dato.capacidad());
        }
        Sql.actualizar(con, "UPDATE salon SET nombre = ?, descripcion = ?, piso = ?, capacidad_puestos = ? WHERE id_salon = ?",
                dato.nombre(), dato.descripcion(), dato.piso(), dato.capacidad(), dato.id());
        return dato.id();
    }

    /** Desactiva o reactiva una fila sin borrar su historial (RF-028). */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE salon SET activo = ? WHERE id_salon = ?", activo, id);
    }

    private static Salon leer(ResultSet fila) throws SQLException {
        return new Salon(
                fila.getInt("id_salon"),
                fila.getInt("id_sede"),
                fila.getString("nombre"),
                fila.getString("descripcion"),
                fila.getInt("piso"),
                fila.getInt("capacidad_puestos"),
                fila.getBoolean("activo"));
    }
}
