package co.aplomo.dao;

import java.sql.*;
import co.aplomo.modelo.Empresa;
import co.aplomo.db.Sql;
import java.util.*;

/** RF-003 a RF-004, RF-010, RF-028: persistencia de empresas y catálogo para cuentas. */
public final class EmpresaDao {
    /** Opción de empresa para el formulario del Administrador. */
    public record Opcion(int id, String nombre, boolean activa) {}

    /** Crea el lector del catálogo existente (RNF-011). */
    public EmpresaDao() {}

    /** Lee empresas para asignación; el servicio exige Administrador (RF-002). */
    public List<Opcion> listar(Connection con) throws SQLException {
        try(var ps=con.prepareStatement("SELECT id_empresa, nombre, activo FROM empresa ORDER BY nombre, id_empresa");var rs=ps.executeQuery()) {
            List<Opcion> opciones=new ArrayList<>();
            while(rs.next()) opciones.add(new Opcion(rs.getInt("id_empresa"),rs.getString("nombre"),rs.getBoolean("activo")));
            return List.copyOf(opciones);
        }
    }

    /** Verifica que la empresa asignada exista; permite conservar una asignación desactivada (RF-003). */
    public boolean existe(Connection con, int id) throws SQLException {
        try(var ps=con.prepareStatement("SELECT id_empresa FROM empresa WHERE id_empresa = ?")) {
            ps.setInt(1,id); try(var rs=ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. */
    public Empresa buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {
        String consulta = "SELECT r.* FROM empresa r WHERE r.id_empresa = ? AND (? = 0 OR r.id_empresa = ?)" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, EmpresaDao::leer, id, empresaPermitida, empresaPermitida);
    }

    /** Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002). */
    public List<Empresa> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {
        return Sql.listar(con, "SELECT r.* FROM empresa r WHERE (? = 0 OR r.id_empresa = ?) ORDER BY r.nombre, r.id_empresa",
                EmpresaDao::leer, empresaPermitida, empresaPermitida);
    }

    /** Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. */
    public int guardar(Connection con, Empresa dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO empresa (id_administrador, nombre, nit, descripcion, telefono, correo, persona_contacto) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    dato.administrador(), dato.nombre(), dato.nit(), dato.descripcion(), dato.telefono(), dato.correo(), dato.contacto());
        }
        Sql.actualizar(con, "UPDATE empresa SET nombre = ?, nit = ?, descripcion = ?, telefono = ?, correo = ?, persona_contacto = ? WHERE id_empresa = ?",
                dato.nombre(), dato.nit(), dato.descripcion(), dato.telefono(), dato.correo(), dato.contacto(), dato.id());
        return dato.id();
    }

    /** Desactiva o reactiva una fila sin borrar su historial (RF-028). */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE empresa SET activo = ? WHERE id_empresa = ?", activo, id);
    }

    private static Empresa leer(ResultSet fila) throws SQLException {
        return new Empresa(
                fila.getInt("id_empresa"),
                fila.getInt("id_administrador"),
                fila.getString("nombre"),
                fila.getString("nit"),
                fila.getString("descripcion"),
                fila.getString("telefono"),
                fila.getString("correo"),
                fila.getString("persona_contacto"),
                fila.getBoolean("activo"));
    }
}
