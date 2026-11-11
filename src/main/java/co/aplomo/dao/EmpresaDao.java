package co.aplomo.dao;

import java.sql.*;
import java.util.*;

/** RF-003: consulta del catálogo existente para asignar cuentas, sin crear ni editar empresas. */
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
}
