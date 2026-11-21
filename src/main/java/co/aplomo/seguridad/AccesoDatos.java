package co.aplomo.seguridad;

import co.aplomo.dao.CuentaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.modelo.Cuenta.Rol;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * RF-002 (Paso 24): alcance único, consultado en cada petición, sin caché de asignaciones.
 */
public final class AccesoDatos {
    private AccesoDatos() {
    }

    /**
     * Relee rol, estado, temporal y hash; la relación histórica id_empresa no autoriza. RNF-011.
     */
    public static Cuenta vigente(Connection con, Cuenta actor) throws SQLException {
        if (actor == null) throw new SecurityException("Inicia sesión para continuar.");
        var cuenta2 = new CuentaDao().porId(con, actor.id(), false);
        if (cuenta2 == null || cuenta2.estado() != Estado.ACTIVA || cuenta2.temporal() ||
                !cuenta2.hash().equals(actor.hash()) || cuenta2.rol() != actor.rol())
            throw new SecurityException("Tu acceso cambió. Vuelve a iniciar sesión.");
        return cuenta2;
    }

    /**
     * Alcance parametrizado: todas o asignaciones activas. empresaExacta solo restringe relaciones internas. RNF-011.
     */
    public record Alcance(boolean todas, int cuenta, Integer empresaExacta, boolean finalizadas) {

        /**
         * Fragmento con alias del código, nunca del navegador (RNF-004).
         */
        public String sql(String columna) {
            if (!columna.matches("[a-z]+\\.id_empresa"))
                throw new IllegalArgumentException("Columna de alcance inválida.");
            return empresaExacta != null ?
                    "(? = TRUE OR " + columna + " = ?)" :
                    "(? = TRUE OR " + columna + " IN (SELECT ce.id_empresa FROM cuenta_empresa ce WHERE ce" +
                    ".id_cuenta = ? AND " + "ce.activo = TRUE))";
        }

        /**
         * Segundo parámetro de la condición; el primero es todas. RNF-011.
         */
        public int valor() {
            return empresaExacta == null ? cuenta : empresaExacta;
        }

        /**
         * RF-002/024: una lista de trabajo del Evaluador muestra solo sus citas.
         */
        public int responsableLista() {
            return !todas && !finalizadas && empresaExacta == null ? cuenta : 0;
        }

        /**
         * Restricción de un padre interno; no usa cuenta.id_empresa como permiso. RNF-011.
         */
        public static Alcance exacta(int empresa) {
            return new Alcance(empresa == 0, 0, empresa, false);
        }
    }

    /**
     * Crea el alcance de la cuenta vigente; cero asignaciones produce listas vacías. RNF-011.
     */
    public static Alcance empresaPermitida(Cuenta cuenta2) {
        return new Alcance(cuenta2.rol() == Rol.ADMINISTRADOR, cuenta2.id(), null, cuenta2.rol() == Rol.USUARIO);
    }

    /**
     * Rechaza una empresa ajena, salvo práctica propia autorizada por su reserva creadora. RNF-011.
     */
    public static void exigirEmpresa(Connection conexion, Cuenta cuenta2, int empresa) throws SQLException {
        var cuenta3 = vigente(conexion, cuenta2);
        var al = empresaPermitida(cuenta3);
        if (!Boolean.TRUE.equals(
                Sql.uno(conexion, "SELECT TRUE FROM empresa e WHERE e.id_empresa=? AND " + al.sql("e.id_empresa"),
                        fila -> fila.getBoolean(1), empresa, al.todas(), al.valor())))
            throw new SecurityException("El registro no está disponible para tu cuenta.");
    }

    /**
     * Autoriza una visita normal dentro del alcance o la práctica privada del Evaluador. RNF-011.
     */
    public static void exigirVisita(Connection conexion, Cuenta cuenta2, int visita) throws SQLException {
        var cuenta3 = vigente(conexion, cuenta2);
        var al = empresaPermitida(cuenta3);
        String sql = "SELECT TRUE FROM visita v JOIN reserva r USING(id_reserva) JOIN salon l USIN" +
                "G(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) WHER" +
                "E v.id_visita=? AND ((e.nit<>? AND " + al.sql("s.id_empresa") +
                ") OR (e.nit=? AND (?=TRUE OR r.id_administrador=?)))";
        if (!Boolean.TRUE.equals(
                Sql.uno(conexion, sql, fila -> fila.getBoolean(1), visita, co.aplomo.modelo.Demostracion.NIT_PRACTICA,
                        al.todas(), al.valor(), co.aplomo.modelo.Demostracion.NIT_PRACTICA,
                        cuenta3.rol() == Rol.ADMINISTRADOR, cuenta3.id())))
            throw new SecurityException("El registro no está disponible para tu cuenta.");
    }

    /**
     * Lectura histórica excepcional: solo la evaluación creada por este Evaluador (RNF-009).
     */
    public static boolean propia(Connection conexion, Cuenta cuenta2, int evaluacion) throws SQLException {
        return cuenta2.rol() == Rol.EVALUADOR && java.util.Objects.equals(cuenta2.id(), Sql.uno(conexion,
                "SELECT id_cuenta FROM bitacora WHERE tabla='evaluacion' AND accion='CREAR' A" +
                        "ND id_registro=? ORDER BY id_bitacora LIMIT 1", fila -> fila.getInt(1), evaluacion));
    }

    /**
     * Autoriza lectura histórica propia o escritura dentro del alcance actual. RNF-011.
     */
    public static void exigirEvaluacion(Connection conexion, Cuenta cuenta2, int id,
            boolean escritura) throws SQLException {
        var evaluacion2 = new co.aplomo.dao.EvaluacionDao().consultar(conexion, id);
        if (evaluacion2 == null) throw new SecurityException("La evaluación no está disponible.");
        if (!escritura && propia(conexion, cuenta2, id)) return;
        exigirVisita(conexion, cuenta2, evaluacion2.visita());
        if (escritura && cuenta2.rol() == Rol.EVALUADOR && !Boolean.TRUE.equals(Sql.uno(conexion,
                "SELECT (p.activo AND l.activo AND s.activo AND em.activo) FROM puesto_trabaj" +
                        "o p JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em " +
                        "USING(id_empresa) WHERE p.id_puesto=?", fila -> fila.getBoolean(1), evaluacion2.puesto())))
            throw new SecurityException(
                    "Esta evaluación es de solo lectura porque su puesto o empresa está desactivado.");
    }
}
