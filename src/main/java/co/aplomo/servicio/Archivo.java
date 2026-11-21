package co.aplomo.servicio;

import co.aplomo.dao.ConsultaDao;
import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.AccesoDatos;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * RF-002/024: archivo histórico con autoría propia y resultados finalizados del Usuario.
 */
public final class Archivo {
    private final Transacciones transacciones;
    private final ConsultaDao consultas;

    /**
     * RNF-011: comparte el acceso de lectura con las consultas existentes.
     */
    public Archivo(Transacciones transacciones) {
        this.transacciones = transacciones;
        this.consultas = new ConsultaDao();
    }

    /**
     * RF-024: empresa visible en el selector, incluso cuando está inactiva.
     */
    public record Empresa(int id, String nombre, boolean activa, String ciudad, String sector) {
        /**
         * RF-024: compatibilidad con consumidores sin metadatos de ubicación.
         */
        public Empresa(int id, String nombre, boolean activa) {
            this(id, nombre, activa, "", "");
        }
    }

    /**
     * RF-024: conjunto ya autorizado, sin fotos ni rutas del servidor.
     */
    public record Datos(List<Empresa> empresas, List<Fila> evaluaciones, Map<String, List<String>> riesgos,
                        List<Integer> empresasInforme) {
    }

    /**
     * RF-002/024: el Evaluador conserva solo su autoría; el Usuario solo resultados finalizados.
     */
    public Datos consultar(Cuenta actor) throws SQLException {
        return transacciones.ejecutar(conexion -> {
            Cuenta cuenta = AccesoDatos.vigente(conexion, actor);
            var alcance = AccesoDatos.empresaPermitida(cuenta);
            List<Fila> evaluaciones =
                    cuenta.rol() == Cuenta.Rol.EVALUADOR ? consultas.propias(conexion, cuenta.id()) : consultas.filas(
                            conexion, SeccionConsulta.EVALUACIONES, alcance, cuenta.rol() == Cuenta.Rol.USUARIO, 0);
            List<Empresa> empresas = Sql.listar(conexion,
                    "SELECT id_empresa,nombre,activo,COALESCE((SELECT GROUP_CONCAT(DISTINCT se.ciudad " +
                            "ORDER BY se.ciudad SEPARATOR ', ') FROM sede se WHERE se.id_empresa=em.id_empresa), '') " +
                            "ciudad,sector FROM empresa em WHERE " +
                            (cuenta.rol() == Cuenta.Rol.EVALUADOR ? "(?=TRUE OR ?=0)" : alcance.sql("em.id_empresa")) +
                            co.aplomo.modelo.Demostracion.filtro("em.id_empresa") + " ORDER BY nombre",
                    fila -> new Empresa(fila.getInt(1), fila.getString(2), fila.getBoolean(3), fila.getString(4),
                            fila.getString(5)), cuenta.rol() == Cuenta.Rol.EVALUADOR || alcance.todas(),
                    cuenta.rol() == Cuenta.Rol.EVALUADOR ? 0 : alcance.valor());
            if (cuenta.rol() == Cuenta.Rol.EVALUADOR) {
                empresas = empresas.stream().filter(empresa -> evaluaciones.stream()
                        .anyMatch(fila -> fila.valor("empresa").equals(Integer.toString(empresa.id())))).toList();
            }
            List<Integer> informes =
                    cuenta.rol() == Cuenta.Rol.ADMINISTRADOR ? empresas.stream().map(Empresa::id).toList() :
                            cuenta.rol() == Cuenta.Rol.EVALUADOR ? new co.aplomo.dao.CuentaEmpresaDao().ids(conexion,
                                    cuenta.id()) : List.of();
            return new Datos(empresas, evaluaciones, consultas.riesgos(conexion), informes);
        });
    }
}
