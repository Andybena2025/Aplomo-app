package co.aplomo.dao;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import java.sql.*;
import java.time.LocalDateTime;

/** RF-001a, RF-001c: solo SQL parametrizado; no borra ni expone contraseñas. */
public class CuentaDao {
    /** Crea el acceso a cuentas (RNF-011). */
    public CuentaDao() {}

    /** Busca por correo y bloquea la fila durante el ingreso para no perder intentos (RF-001a). */
    public Cuenta porCorreo(Connection con, String correo) throws SQLException {
        try (var ps = con.prepareStatement("SELECT * FROM cuenta WHERE correo = ? FOR UPDATE")) {
            ps.setString(1,correo);
            try (var rs = ps.executeQuery()) { return rs.next() ? leer(rs) : null; }
        }
    }

    /** Lee una cuenta por ID; opcionalmente bloquea su fila para actualizarla (RF-002). */
    public Cuenta porId(Connection con, int id, boolean bloquear) throws SQLException {
        try (var ps = con.prepareStatement("SELECT * FROM cuenta WHERE id_cuenta = ?" + (bloquear ? " FOR UPDATE" : ""))) {
            ps.setInt(1,id);
            try (var rs = ps.executeQuery()) { return rs.next() ? leer(rs) : null; }
        }
    }

    /** Guarda contador, estado y vencimiento de bloqueo en la misma transacción (RF-001a). */
    public void acceso(Connection con, int id, int intentos, LocalDateTime hasta, Estado estado) throws SQLException {
        try (var ps = con.prepareStatement("UPDATE cuenta SET intentos_fallidos = ?, bloqueado_hasta = ?, estado = ? WHERE id_cuenta = ?")) {
            ps.setInt(1,intentos);
            ps.setTimestamp(2,hasta == null ? null : Timestamp.valueOf(hasta));
            ps.setString(3,estado.name()); ps.setInt(4,id); ps.executeUpdate();
        }
    }

    /** Guarda únicamente el hash y si la contraseña debe cambiarse al entrar (RF-001c). */
    public void contrasena(Connection con, int id, String hash, boolean temporal) throws SQLException {
        try (var ps = con.prepareStatement("UPDATE cuenta SET contrasena_hash = ?, contrasena_temporal = ? WHERE id_cuenta = ?")) {
            ps.setString(1,hash); ps.setBoolean(2,temporal); ps.setInt(3,id); ps.executeUpdate();
        }
    }

    /** Lista cuentas para la administración; la autorización se comprueba en el servicio (RF-003). */
    public java.util.List<Cuenta> listar(Connection con) throws SQLException {
        try (var ps=con.prepareStatement("SELECT * FROM cuenta ORDER BY nombre, id_cuenta"); var rs=ps.executeQuery()) {
            var cuentas=new java.util.ArrayList<Cuenta>();
            while(rs.next()) cuentas.add(leer(rs));
            return java.util.List.copyOf(cuentas);
        }
    }

    /** Bloquea todos los Administradores en orden estable antes de cambiar roles o estados (RF-003). */
    public java.util.List<Cuenta> bloquearAdministradores(Connection con) throws SQLException {
        try (var ps=con.prepareStatement("SELECT * FROM cuenta WHERE rol = ? ORDER BY id_cuenta FOR UPDATE")) {
            ps.setString(1,Rol.ADMINISTRADOR.name());
            try(var rs=ps.executeQuery()) {
                var cuentas=new java.util.ArrayList<Cuenta>();
                while(rs.next()) cuentas.add(leer(rs));
                return java.util.List.copyOf(cuentas);
            }
        }
    }

    /** Inserta una cuenta activa con contraseña temporal y devuelve su ID (RF-003). */
    public int crear(Connection con, String nombre, String correo, String telefono, String cargo,
                     Rol rol, Integer empresa, String hash) throws SQLException {
        try(var ps=con.prepareStatement("INSERT INTO cuenta (nombre,correo,telefono,cargo,rol,id_empresa,contrasena_hash,contrasena_temporal) VALUES (?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,nombre); ps.setString(2,correo); ps.setString(3,telefono); ps.setString(4,cargo);
            ps.setString(5,rol.name()); ps.setObject(6,empresa); ps.setString(7,hash); ps.setBoolean(8,true);
            ps.executeUpdate();
            try(var claves=ps.getGeneratedKeys()) {
                if(!claves.next()) throw new SQLException("No se pudo identificar la cuenta creada.");
                return claves.getInt(1);
            }
        }
    }

    /** Edita datos de la cuenta sin cambiar su ID, estado ni contraseña (RF-003). */
    public void editar(Connection con, int id, String nombre, String correo, String telefono, String cargo,
                       Rol rol, Integer empresa) throws SQLException {
        try(var ps=con.prepareStatement("UPDATE cuenta SET nombre = ?, correo = ?, telefono = ?, cargo = ?, rol = ?, id_empresa = ? WHERE id_cuenta = ?")) {
            ps.setString(1,nombre); ps.setString(2,correo); ps.setString(3,telefono); ps.setString(4,cargo);
            ps.setString(5,rol.name()); ps.setObject(6,empresa); ps.setInt(7,id); ps.executeUpdate();
        }
    }

    private Cuenta leer(ResultSet rs) throws SQLException {
        int empresa = rs.getInt("id_empresa");
        Integer idEmpresa = rs.wasNull() ? null : empresa;
        var hasta = rs.getTimestamp("bloqueado_hasta");
        return new Cuenta(rs.getInt("id_cuenta"),idEmpresa,rs.getString("nombre"),rs.getString("correo"),
                rs.getString("telefono"),rs.getString("cargo"),rs.getString("contrasena_hash"),
                rs.getBoolean("contrasena_temporal"),rs.getInt("intentos_fallidos"),hasta == null ? null : hasta.toLocalDateTime(),
                Estado.valueOf(rs.getString("estado")),Rol.valueOf(rs.getString("rol")));
    }
}
