package co.aplomo.dao;

import co.aplomo.seguridad.AccesoDatos.Alcance;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Visita;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * RF-013a, RF-013b: persistencia de visitas y lectura de sus evaluaciones ya guardadas.
 */
public final class VisitaDao {
    private static final String CONTEXTO =
            "SELECT v.*, r.codigo AS reserva_codigo, s.id_empresa, l.id_sede, l.id_salon, " +
                    "e.nombre AS empresa_nombre, s.nombre AS sede_nombre, l.nombre AS salon_nombre " +
                    "FROM visita v JOIN reserva r ON r.id_reserva = v.id_reserva JOIN salon l ON " +
                    "l.id_salon = r.id_salon " +
                    "JOIN sede s ON s.id_sede = l.id_sede JOIN empresa e ON e.id_empresa = s.id_empresa";

    /**
     * Contexto de la visita sin fotografías ni información de otra empresa. RNF-011.
     */
    public record Ficha(Visita visita, int empresa, int sede, int salon, String codigoReserva, String empresaNombre,
                        String sedeNombre, String salonNombre) {
    }

    /**
     * Resumen consultable de una evaluación; no contiene rutas de fotografías. RNF-011.
     */
    public record Evaluacion(int id, int puesto, int numero, LocalDate fecha, String postura, Integer puntaje,
                             String color, int nivel, String descripcion, String cargo) {
        /**
         * RNF-011: compatibilidad de consumidores anteriores, sin inventar nombres.
         */
        public Evaluacion(int id, int puesto, int numero, LocalDate fecha, String postura, Integer puntaje,
                String color, int nivel) {
            this(id, puesto, numero, fecha, postura, puntaje, color, nivel, "", "");
        }
    }

    /**
     * Datos de cálculo de la calificación; los nulos representan fotos o análisis aún inexistentes. RNF-011.
     */
    public record Resultado(int evaluacion, int puesto, Integer foto, Integer analisis, Integer puntaje) {
    }

    /**
     * Puesto pendiente de una visita en curso; puesto nulo si ya no quedan pendientes. RNF-011.
     */
    public record ParaEvaluar(int visita, String empresa, String sede, Integer puesto, Integer numero,
                              String descripcion) {
    }

    /**
     * Solo se invoca después de validar el Administrador vigente en el servicio. RNF-011.
     */
    public List<ParaEvaluar> paraEvaluar(Connection con) throws SQLException {
        return paraEvaluar(con, Alcance.exacta(0));
    }

    /**
     * RF-002/013: puestos pendientes en el alcance vigente, sin consultas por fila.
     */
    public List<ParaEvaluar> paraEvaluar(Connection con, Alcance alcance) throws SQLException {
        return Sql.listar(con, "SELECT v.id_visita,e.nombre empresa,s.nombre sede,p.id_puesto,p.numero," +
                        "CONCAT_WS(' · ',(SELECT ca.nombre FROM cargo ca WHERE ca.id_cargo=p.id_cargo)," +
                        "p.descripcion) descripcion " +
                        "FROM visita v JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) " +
                        "JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) " +
                        "LEFT JOIN puesto_trabajo p ON p.id_salon=l.id_salon AND p.activo=TRUE AND NO" + "T EXISTS " +
                        "(SELECT 1 FROM evaluacion ev WHERE ev.id_visita=v.id_visita AND ev.id_puesto" + "=p" +
                        ".id_puesto) " +
                        "WHERE v.estado='EN_CURSO' AND " + alcance.sql("s.id_empresa") +
                        co.aplomo.modelo.Demostracion.filtro("s.id_empresa") +
                        " AND (?=0 OR r.id_evaluador=?) ORDER BY e.nombre,s.nombre,v.id_visita,p.numero,p.id_puesto",
                fila2 -> new ParaEvaluar(fila2.getInt("id_visita"), fila2.getString("empresa"), fila2.getString("sede"),
                        Sql.enteroOpcional(fila2, "id_puesto"), Sql.enteroOpcional(fila2, "numero"),
                        fila2.getString("descripcion")), alcance.todas(), alcance.valor(), alcance.responsableLista(),
                alcance.responsableLista());
    }

    /**
     * Crea el acceso JDBC a las visitas. RNF-011.
     */
    public VisitaDao() {
    }

    /**
     * Lista todas las visitas para Administrador o solo las finalizadas de la empresa del Usuario. RNF-011.
     */
    public List<Ficha> listar(Connection con, int empresa, boolean soloFinalizadas) throws SQLException {
        return listar(con, Alcance.exacta(empresa), soloFinalizadas);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public List<Ficha> listar(Connection con, Alcance empresa, boolean soloFinalizadas) throws SQLException {
        return Sql.listar(con,
                CONTEXTO + " WHERE " + empresa.sql("s.id_empresa") + " AND (? = FALSE OR v.estado = 'FINALIZADA')" +
                        co.aplomo.modelo.Demostracion.filtro("s.id_empresa") +
                        " AND (?=0 OR r.id_evaluador=?) ORDER BY v.hora_real_inicio DESC, v.id_visita DESC",
                VisitaDao::leer, empresa.todas(), empresa.valor(), soloFinalizadas, empresa.responsableLista(),
                empresa.responsableLista());
    }

    /**
     * Consulta una visita dentro del alcance de la cuenta y permite bloquearla para finalizar. RNF-011.
     */
    public Ficha buscar(Connection con, int id, int empresa, boolean soloFinalizadas,
            boolean bloquear) throws SQLException {
        return buscar(con, id, Alcance.exacta(empresa), soloFinalizadas, bloquear);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public Ficha buscar(Connection con, int id, Alcance empresa, boolean soloFinalizadas,
            boolean bloquear) throws SQLException {
        return Sql.uno(con, CONTEXTO + " WHERE v.id_visita = ? AND " + empresa.sql("s.id_empresa") +
                        " AND (? = FALSE OR v.estado = 'FINALIZADA')" + (bloquear ? " FOR UPDATE" : ""),
                VisitaDao::leer, id,
                empresa.todas(), empresa.valor(), soloFinalizadas);
    }

    /**
     * Inicia una visita; la restricción única impide más de una por reserva. RNF-011.
     */
    public int iniciar(Connection con, int reserva, LocalDateTime cuando) throws SQLException {
        return Sql.insertar(con, "INSERT INTO visita (id_reserva, hora_real_inicio) VALUES (?, ?)", reserva, cuando);
    }

    /**
     * Guarda fin, notas y calificación junto con el estado final de la visita. RNF-011.
     */
    public void finalizar(Connection con, int id, LocalDateTime cuando, String notas,
            BigDecimal calificacion) throws SQLException {
        Sql.actualizar(con, "UPDATE visita SET hora_real_fin = ?, notas = ?, calificacion_final = ?, esta" +
                "do = 'FINALIZADA' WHERE id_visita = ?", cuando, notas, calificacion, id);
    }

    /**
     * Lee las evaluaciones y el máximo de sus análisis guardados, incluyendo ambos lados del cuerpo. RNF-011.
     */
    public List<Evaluacion> evaluaciones(Connection con, int visita, boolean bloquear) throws SQLException {
        String consulta = "SELECT e.id_evaluacion, e.id_puesto, p.numero,p.descripcion," +
                "(SELECT ca.nombre FROM cargo ca WHERE ca.id_cargo=p.id_cargo) cargo,e.fecha, e.postura, (SELECT n" +
                ".color FROM nivel_riesgo n WHERE (SELECT MAX(a.puntaje_final) FROM fotografi" +
                "a f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=e.id_evaluacio" +
                "n) BETWEEN n.puntaje_minimo AND n.puntaje_maximo) color, (SELECT n.nivel_ofi" +
                "cial_rula FROM nivel_riesgo n WHERE (SELECT MAX(a.puntaje_final) FROM fotogr" +
                "afia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=e.id_evalua" +
                "cion) BETWEEN n.puntaje_minimo AND n.puntaje_maximo) nivel, " +
                "(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a ON a.id_" + "foto = f.id_foto " +
                "WHERE f.id_evaluacion = e.id_evaluacion) AS puntaje " +
                "FROM evaluacion e JOIN puesto_trabajo p ON p.id_puesto = e.id_puesto WHERE e" + ".id_visita = ? " +
                "ORDER BY e.id_evaluacion DESC" + (bloquear ? " FOR UPDATE" : "");
        return Sql.listar(con, consulta,
                fila -> new Evaluacion(fila.getInt("id_evaluacion"), fila.getInt("id_puesto"), fila.getInt("numero"),
                        fila.getDate("fecha").toLocalDate(), fila.getString("postura"),
                        Sql.enteroOpcional(fila, "puntaje"), fila.getString("color"), fila.getInt("nivel"),
                        fila.getString("descripcion"), fila.getString("cargo")), visita);
    }

    /**
     * Obtiene y bloquea los resultados actuales al finalizar, incluidos registros sin análisis. RNF-011.
     */
    public List<Resultado> resultadosActuales(Connection con, int visita) throws SQLException {
        String consulta = "SELECT e.id_evaluacion, e.id_puesto, f.id_foto, a.id_analisis, a.puntaje_final " +
                "FROM evaluacion e LEFT JOIN fotografia f ON f.id_evaluacion = e.id_evaluacion " +
                "LEFT JOIN analisis_rula a ON a.id_foto = f.id_foto WHERE e.id_visita = ? " +
                "ORDER BY a.id_analisis DESC FOR UPDATE";
        return Sql.listar(con, consulta, fila -> new Resultado(fila.getInt("id_evaluacion"), fila.getInt("id_puesto"),
                Sql.enteroOpcional(fila, "id_foto"), Sql.enteroOpcional(fila, "id_analisis"),
                Sql.enteroOpcional(fila, "puntaje_final")), visita);
    }

    private static Ficha leer(ResultSet fila) throws SQLException {
        Timestamp fin = fila.getTimestamp("hora_real_fin");
        Visita visita = new Visita(fila.getInt("id_visita"), fila.getInt("id_reserva"),
                fila.getTimestamp("hora_real_inicio").toLocalDateTime(), fin == null ? null : fin.toLocalDateTime(),
                fila.getString("notas"), fila.getBigDecimal("calificacion_final"), fila.getString("estado"));
        return new Ficha(visita, fila.getInt("id_empresa"), fila.getInt("id_sede"), fila.getInt("id_salon"),
                fila.getString("reserva_codigo"), fila.getString("empresa_nombre"), fila.getString("sede_nombre"),
                fila.getString("salon_nombre"));
    }
}
