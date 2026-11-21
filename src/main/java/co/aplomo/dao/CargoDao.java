package co.aplomo.dao;
import co.aplomo.seguridad.AccesoDatos.Alcance;

import co.aplomo.modelo.Cargo;
import co.aplomo.db.Sql;
import java.sql.*;
import java.util.*;

/** RF-004 a RF-010, RF-028: consultas parametrizadas de cargo. */
public final class CargoDao {
    /** Crea el acceso JDBC al catálogo de cargo. */
    public CargoDao() {}

    /** Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. */
    public Cargo buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {return buscar(con,id,Alcance.exacta(empresaPermitida),bloquear);}
    /** RF-002: consulta parametrizada en el alcance vigente. */
    public Cargo buscar(Connection con, int id, Alcance empresaPermitida, boolean bloquear) throws SQLException {
        String consulta = "SELECT r.* FROM cargo r WHERE r.id_cargo = ? AND "+empresaPermitida.sql("r.id_empresa")+"" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, CargoDao::leer, id, empresaPermitida.todas(),empresaPermitida.valor());
    }

    /** Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002). */
    public List<Cargo> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {return buscarTodos(con,padre,Alcance.exacta(empresaPermitida));}
    /** RF-002: consulta parametrizada en el alcance vigente. */
    public List<Cargo> buscarTodos(Connection con, int padre, Alcance empresaPermitida) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM cargo r WHERE r.id_empresa = ? AND "+empresaPermitida.sql("r.id_empresa")+" ORDER BY r.nombre, r.id_cargo",
                CargoDao::leer, padre, empresaPermitida.todas(),empresaPermitida.valor());
    }

    /** Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. */
    public int guardar(Connection con, Cargo dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO cargo (id_empresa, nombre, tipo) VALUES (?, ?, ?)",
                    dato.empresa(), dato.nombre(), dato.tipo());
        }
        Sql.actualizar(con, "UPDATE cargo SET nombre = ?, tipo = ? WHERE id_cargo = ?",
                dato.nombre(), dato.tipo(), dato.id());
        return dato.id();
    }

    /** Desactiva o reactiva una fila sin borrar su historial (RF-028). */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE cargo SET activo = ? WHERE id_cargo = ?", activo, id);
    }

    private static Cargo leer(ResultSet fila) throws SQLException {
        return new Cargo(
                fila.getInt("id_cargo"),
                fila.getInt("id_empresa"),
                fila.getString("nombre"),
                fila.getString("tipo"),
                fila.getBoolean("activo"));
    }
}
