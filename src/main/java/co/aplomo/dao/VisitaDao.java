package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Visita;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** RF-013a, RF-013b: persistencia de visitas y lectura de sus evaluaciones ya guardadas. */
public final class VisitaDao {
    private static final String CONTEXTO = "SELECT v.*, r.codigo AS reserva_codigo, s.id_empresa, l.id_sede, l.id_salon, "
            + "e.nombre AS empresa_nombre, s.nombre AS sede_nombre, l.nombre AS salon_nombre "
            + "FROM visita v JOIN reserva r ON r.id_reserva = v.id_reserva JOIN salon l ON l.id_salon = r.id_salon "
            + "JOIN sede s ON s.id_sede = l.id_sede JOIN empresa e ON e.id_empresa = s.id_empresa";

    /** Contexto de la visita sin fotografías ni información de otra empresa. */
    public record Ficha(Visita visita, int empresa, int sede, int salon, String codigoReserva,
                        String empresaNombre, String sedeNombre, String salonNombre) {}

    /** Resumen consultable de una evaluación; no contiene rutas de fotografías. */
    public record Evaluacion(int id, int puesto, int numero, LocalDate fecha, String postura, Integer puntaje,String color,int nivel) {}

    /** Datos de cálculo de la calificación; los nulos representan fotos o análisis aún inexistentes. */
    public record Resultado(int evaluacion, int puesto, Integer foto, Integer analisis, Integer puntaje) {}

    /** Crea el acceso JDBC a las visitas. */
    public VisitaDao() {}

    /** Lista todas las visitas para Administrador o solo las finalizadas de la empresa del Usuario. */
    public List<Ficha> listar(Connection con, int empresa, boolean soloFinalizadas) throws SQLException {
        return Sql.listar(con, CONTEXTO + " WHERE (? = 0 OR s.id_empresa = ?) AND (? = FALSE OR v.estado = 'FINALIZADA') ORDER BY v.hora_real_inicio DESC, v.id_visita DESC",
                VisitaDao::leer, empresa, empresa, soloFinalizadas);
    }

    /** Consulta una visita dentro del alcance de la cuenta y permite bloquearla para finalizar. */
    public Ficha buscar(Connection con, int id, int empresa, boolean soloFinalizadas, boolean bloquear) throws SQLException {
        return Sql.uno(con, CONTEXTO + " WHERE v.id_visita = ? AND (? = 0 OR s.id_empresa = ?) AND (? = FALSE OR v.estado = 'FINALIZADA')"
                        + (bloquear ? " FOR UPDATE" : ""), VisitaDao::leer, id, empresa, empresa, soloFinalizadas);
    }

    /** Inicia una visita; la restricción única impide más de una por reserva. */
    public int iniciar(Connection con, int reserva, LocalDateTime cuando) throws SQLException {
        return Sql.insertar(con, "INSERT INTO visita (id_reserva, hora_real_inicio) VALUES (?, ?)", reserva, cuando);
    }

    /** Guarda fin, notas y calificación junto con el estado final de la visita. */
    public void finalizar(Connection con, int id, LocalDateTime cuando, String notas, BigDecimal calificacion) throws SQLException {
        Sql.actualizar(con, "UPDATE visita SET hora_real_fin = ?, notas = ?, calificacion_final = ?, estado = 'FINALIZADA' WHERE id_visita = ?",
                cuando, notas, calificacion, id);
    }

    /** Lee las evaluaciones y el máximo de sus análisis guardados, incluyendo ambos lados del cuerpo. */
    public List<Evaluacion> evaluaciones(Connection con, int visita, boolean bloquear) throws SQLException {
        String consulta = "SELECT e.id_evaluacion, e.id_puesto, p.numero, e.fecha, e.postura, (SELECT n.color FROM nivel_riesgo n WHERE (SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=e.id_evaluacion) BETWEEN n.puntaje_minimo AND n.puntaje_maximo) color, (SELECT n.nivel_oficial_rula FROM nivel_riesgo n WHERE (SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=e.id_evaluacion) BETWEEN n.puntaje_minimo AND n.puntaje_maximo) nivel, "
                + "(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a ON a.id_foto = f.id_foto "
                + "WHERE f.id_evaluacion = e.id_evaluacion) AS puntaje "
                + "FROM evaluacion e JOIN puesto_trabajo p ON p.id_puesto = e.id_puesto WHERE e.id_visita = ? "
                + "ORDER BY e.id_evaluacion DESC" + (bloquear ? " FOR UPDATE" : "");
        return Sql.listar(con, consulta, fila -> new Evaluacion(fila.getInt("id_evaluacion"), fila.getInt("id_puesto"),
                fila.getInt("numero"), fila.getDate("fecha").toLocalDate(), fila.getString("postura"),
                Sql.enteroOpcional(fila, "puntaje"),fila.getString("color"),fila.getInt("nivel")), visita);
    }

    /** Obtiene y bloquea los resultados actuales al finalizar, incluidos registros sin análisis. */
    public List<Resultado> resultadosActuales(Connection con, int visita) throws SQLException {
        String consulta = "SELECT e.id_evaluacion, e.id_puesto, f.id_foto, a.id_analisis, a.puntaje_final "
                + "FROM evaluacion e LEFT JOIN fotografia f ON f.id_evaluacion = e.id_evaluacion "
                + "LEFT JOIN analisis_rula a ON a.id_foto = f.id_foto WHERE e.id_visita = ? "
                + "ORDER BY a.id_analisis DESC FOR UPDATE";
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
                fila.getString("reserva_codigo"), fila.getString("empresa_nombre"), fila.getString("sede_nombre"), fila.getString("salon_nombre"));
    }
}
