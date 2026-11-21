package co.aplomo.dao;

import co.aplomo.seguridad.AccesoDatos.Alcance;
import co.aplomo.modelo.Salon;
import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * RF-004 a RF-010, RF-028: consultas parametrizadas de salon.
 */
public final class SalonDao {

    /**
     * Crea el acceso JDBC al catálogo de salon. RNF-011.
     */
    public SalonDao() {
    }

    /**
     * Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. RNF-011.
     */
    public Salon buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {
        return buscar(con, id, Alcance.exacta(empresaPermitida), bloquear);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public Salon buscar(Connection con, int id, Alcance empresaPermitida, boolean bloquear) throws SQLException {
        String consulta =
                "SELECT r.* FROM salon r JOIN sede s ON s.id_sede = r.id_sede WHERE r.id_salo" + "n = ? AND " +
                        empresaPermitida.sql("s.id_empresa") + "" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, SalonDao::leer, id, empresaPermitida.todas(), empresaPermitida.valor());
    }

    /**
     * Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002).
     */
    public List<Salon> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {
        return buscarTodos(con, padre, Alcance.exacta(empresaPermitida));
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public List<Salon> buscarTodos(Connection con, int padre, Alcance empresaPermitida) throws SQLException {
        return Sql.listar(con,
                "SELECT r.* FROM salon r JOIN sede s ON s.id_sede = r.id_sede WHERE r.id_sede" + " = ? AND " +
                        empresaPermitida.sql("s.id_empresa") + " ORDER BY r.nombre, r.id_salon", SalonDao::leer, padre,
                empresaPermitida.todas(), empresaPermitida.valor());
    }

    /**
     * Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. RNF-011.
     */
    public int guardar(Connection con, Salon dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO salon (id_sede, nombre, descripcion, piso, capacidad_puestos) VA" +
                            "LUES (?, ?, ?, ?, ?)", dato.sede(), dato.nombre(), dato.descripcion(), dato.piso(),
                    dato.capacidad());
        }
        Sql.actualizar(con,
                "UPDATE salon SET nombre = ?, descripcion = ?, piso = ?, capacidad_puestos = " + "? WHERE id_salon = ?",
                dato.nombre(), dato.descripcion(), dato.piso(), dato.capacidad(), dato.id());
        return dato.id();
    }

    /**
     * Desactiva o reactiva una fila sin borrar su historial (RF-028).
     */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE salon SET activo = ? WHERE id_salon = ?", activo, id);
    }

    /**
     * RF-028: ofrece solamente opciones activas bajo una jerarquía también activa.
     */
    public List<Salon> activos(Connection con) throws SQLException {
        return activos(con, Alcance.exacta(0));
    }

    /**
     * RF-002: catálogo activo limitado por cuenta_empresa antes de devolver datos.
     */
    public List<Salon> activos(Connection con, Alcance alcance) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM salon r JOIN sede s ON s.id_sede = r.id_sede JOIN empresa e " +
                        "ON e.id_empresa = s.id_empresa WHERE r.activo = TRUE AND s.activo = TRUE AND" +
                        " e.activo = TRUE AND " + alcance.sql("s.id_empresa") +
                        co.aplomo.modelo.Demostracion.filtro("s.id_empresa") + " ORDER BY r.nombre, r.id_salon",
                SalonDao::leer,
                alcance.todas(), alcance.valor());
    }

    private static Salon leer(ResultSet fila) throws SQLException {
        return new Salon(fila.getInt("id_salon"), fila.getInt("id_sede"), fila.getString("nombre"),
                fila.getString("descripcion"), fila.getInt("piso"), fila.getInt("capacidad_puestos"),
                fila.getBoolean("activo"));
    }
}
