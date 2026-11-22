package co.aplomo.instalacion;

import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-002/003, RNF-009: conserva cuentas y asignaciones por NIT durante el reinicio explícito de la demo.
 */
final class VinculosDemo {
    private record Directo(int cuenta, String nit) {
    }

    private record Asignacion(int cuenta, String nit, boolean activa, LocalDateTime fecha) {
    }

    private final List<Directo> directos;
    private final List<Asignacion> asignaciones;

    private VinculosDemo(List<Directo> directos, List<Asignacion> asignaciones) {
        this.directos = directos;
        this.asignaciones = asignaciones;
    }

    static VinculosDemo guardar(Connection con) throws SQLException {
        var empresas = Sql.listar(con, "SELECT id_empresa FROM empresa WHERE nit IN " + nits(), f -> f.getInt(1),
                DemoIdentidades.NITS.toArray());
        DemoIdentidades.exigirIdentidad(con, empresas);
        var directos = Sql.listar(con, "SELECT c.id_cuenta,e.nit FROM cuenta c JOIN empresa e " +
                        "ON e.id_empresa=c.id_empresa WHERE e.nit IN " + nits() + " FOR UPDATE",
                f -> new Directo(f.getInt(1), f.getString(2)), DemoIdentidades.NITS.toArray());
        var cuentas = Sql.listar(con, "SELECT DISTINCT ce.id_cuenta FROM cuenta_empresa ce JOIN empresa e " +
                "USING(id_empresa) WHERE e.nit IN " + nits(), f -> f.getInt(1), DemoIdentidades.NITS.toArray());
        List<Asignacion> asignaciones = cuentas.isEmpty() ? List.of() : Sql.listar(con,
                "SELECT ce.id_cuenta,e.nit,ce.activo,ce.fecha_asignacion FROM cuenta_empresa ce " +
                "JOIN empresa e USING(id_empresa) WHERE ce.id_cuenta IN (" +
                        String.join(",", java.util.Collections.nCopies(cuentas.size(), "?")) + ") FOR UPDATE",
                f -> new Asignacion(f.getInt(1), f.getString(2), f.getBoolean(3), f.getTimestamp(4).toLocalDateTime()),
                cuentas.toArray());
        return new VinculosDemo(directos, asignaciones);
    }

    void desvincular(Connection con) throws SQLException {
        for (var vinculo : directos)
            Sql.actualizar(con, "UPDATE cuenta SET id_empresa=NULL WHERE id_cuenta=?", vinculo.cuenta());
    }

    void restaurar(Connection con) throws SQLException {
        // La carga crea sus asignaciones iniciales; para las cuentas anteriores prevalece su lista guardada.
        for (int cuenta : asignaciones.stream().map(Asignacion::cuenta).distinct().toList()) {
            Object[] parametros = new Object[DemoIdentidades.NITS.size() + 1];
            parametros[0] = cuenta;
            for (int i = 0; i < DemoIdentidades.NITS.size(); i++) parametros[i + 1] = DemoIdentidades.NITS.get(i);
            Sql.actualizar(con, "UPDATE cuenta_empresa ce JOIN empresa e USING(id_empresa) SET ce.activo=FALSE " +
                    "WHERE ce.id_cuenta=? AND e.nit IN " + nits(), parametros);
        }
        for (var vinculo : directos) {
            Sql.actualizar(con, "UPDATE cuenta SET id_empresa=? WHERE id_cuenta=?", empresa(con, vinculo.nit()),
                    vinculo.cuenta());
        }
        for (var vinculo : asignaciones) {
            Sql.actualizar(con, "INSERT INTO cuenta_empresa(id_cuenta,id_empresa,activo,fecha_asignacion) " +
                            "VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE activo=VALUES(activo)," +
                            "fecha_asignacion=VALUES(fecha_asignacion)", vinculo.cuenta(), empresa(con, vinculo.nit()),
                    vinculo.activa(), vinculo.fecha());
        }
    }

    private static int empresa(Connection con, String nit) throws SQLException {
        Integer id = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", f -> f.getInt(1), nit);
        if (id == null)
            throw new IllegalArgumentException("No se encontró la empresa de una cuenta. Reinicio revertido.");
        return id;
    }

    private static String nits() {
        return "(" + String.join(",", java.util.Collections.nCopies(DemoIdentidades.NITS.size(), "?")) + ")";
    }
}
