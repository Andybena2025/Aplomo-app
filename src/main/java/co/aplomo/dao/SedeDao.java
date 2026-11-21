package co.aplomo.dao;
import co.aplomo.seguridad.AccesoDatos.Alcance;

import co.aplomo.modelo.Sede;
import co.aplomo.db.Sql;
import java.sql.*;
import java.util.*;

/** RF-004 a RF-010, RF-028: consultas parametrizadas de sede. */
public final class SedeDao {
    /** Crea el acceso JDBC al catálogo de sede. */
    public SedeDao() {}

    /** Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. */
    public Sede buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {return buscar(con,id,Alcance.exacta(empresaPermitida),bloquear);}
    /** RF-002: consulta parametrizada en el alcance vigente. */
    public Sede buscar(Connection con, int id, Alcance empresaPermitida, boolean bloquear) throws SQLException {
        String consulta = "SELECT r.* FROM sede r WHERE r.id_sede = ? AND "+empresaPermitida.sql("r.id_empresa")+"" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, SedeDao::leer, id, empresaPermitida.todas(),empresaPermitida.valor());
    }

    /** Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002). */
    public List<Sede> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {return buscarTodos(con,padre,Alcance.exacta(empresaPermitida));}
    /** RF-002: consulta parametrizada en el alcance vigente. */
    public List<Sede> buscarTodos(Connection con, int padre, Alcance empresaPermitida) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM sede r WHERE r.id_empresa = ? AND "+empresaPermitida.sql("r.id_empresa")+" ORDER BY r.nombre, r.id_sede",
                SedeDao::leer, padre, empresaPermitida.todas(),empresaPermitida.valor());
    }

    /** Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. */
    public int guardar(Connection con, Sede dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO sede (id_empresa, nombre, direccion, ciudad, telefono, horario, descripcion) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    dato.empresa(), dato.nombre(), dato.direccion(), dato.ciudad(), dato.telefono(), dato.horario(), dato.descripcion());
        }
        Sql.actualizar(con, "UPDATE sede SET nombre = ?, direccion = ?, ciudad = ?, telefono = ?, horario = ?, descripcion = ? WHERE id_sede = ?",
                dato.nombre(), dato.direccion(), dato.ciudad(), dato.telefono(), dato.horario(), dato.descripcion(), dato.id());
        return dato.id();
    }

    /** Desactiva o reactiva una fila sin borrar su historial (RF-028). */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE sede SET activo = ? WHERE id_sede = ?", activo, id);
    }

    /** RF-028: ofrece solamente opciones activas bajo una jerarquía también activa. */
    public List<Sede> activas(Connection con) throws SQLException {return activas(con,Alcance.exacta(0));}
    /** RF-002: catálogo activo limitado por cuenta_empresa antes de devolver datos. */
    public List<Sede> activas(Connection con,Alcance alcance) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM sede r JOIN empresa e ON e.id_empresa = r.id_empresa WHERE r.activo = TRUE AND e.activo = TRUE AND "+alcance.sql("r.id_empresa")+co.aplomo.modelo.Demostracion.filtro("r.id_empresa")+" ORDER BY r.nombre, r.id_sede", SedeDao::leer,alcance.todas(),alcance.valor());
    }

    private static Sede leer(ResultSet fila) throws SQLException {
        return new Sede(
                fila.getInt("id_sede"),
                fila.getInt("id_empresa"),
                fila.getString("nombre"),
                fila.getString("direccion"),
                fila.getString("ciudad"),
                fila.getString("telefono"),
                fila.getString("horario"),
                fila.getString("descripcion"),
                fila.getBoolean("activo"));
    }
}
