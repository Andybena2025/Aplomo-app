package co.aplomo.dao;
import co.aplomo.seguridad.AccesoDatos.Alcance;

import co.aplomo.modelo.Area;
import co.aplomo.db.Sql;
import java.sql.*;
import java.util.*;

/** RF-004 a RF-010, RF-028: consultas parametrizadas de area. */
public final class AreaDao {
    /** Crea el acceso JDBC al catálogo de area. */
    public AreaDao() {}

    /** Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. */
    public Area buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {return buscar(con,id,Alcance.exacta(empresaPermitida),bloquear);}
    /** RF-002: consulta parametrizada en el alcance vigente. */
    public Area buscar(Connection con, int id, Alcance empresaPermitida, boolean bloquear) throws SQLException {
        String consulta = "SELECT r.* FROM area r WHERE r.id_area = ? AND "+empresaPermitida.sql("r.id_empresa")+"" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, AreaDao::leer, id, empresaPermitida.todas(),empresaPermitida.valor());
    }

    /** Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002). */
    public List<Area> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {return buscarTodos(con,padre,Alcance.exacta(empresaPermitida));}
    /** RF-002: consulta parametrizada en el alcance vigente. */
    public List<Area> buscarTodos(Connection con, int padre, Alcance empresaPermitida) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM area r WHERE r.id_empresa = ? AND "+empresaPermitida.sql("r.id_empresa")+" ORDER BY r.nombre, r.id_area",
                AreaDao::leer, padre, empresaPermitida.todas(),empresaPermitida.valor());
    }

    /** Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. */
    public int guardar(Connection con, Area dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO area (id_empresa, nombre) VALUES (?, ?)",
                    dato.empresa(), dato.nombre());
        }
        Sql.actualizar(con, "UPDATE area SET nombre = ? WHERE id_area = ?",
                dato.nombre(), dato.id());
        return dato.id();
    }

    /** Desactiva o reactiva una fila sin borrar su historial (RF-028). */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE area SET activo = ? WHERE id_area = ?", activo, id);
    }

    private static Area leer(ResultSet fila) throws SQLException {
        return new Area(
                fila.getInt("id_area"),
                fila.getInt("id_empresa"),
                fila.getString("nombre"),
                fila.getBoolean("activo"));
    }
}
