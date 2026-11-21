package co.aplomo.dao;

import co.aplomo.seguridad.AccesoDatos.Alcance;
import co.aplomo.db.Sql;
import co.aplomo.modelo.PendienteArreglo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-022: pendientes e historial consultables por empresa y estado de visita, sin fotos.
 */
public final class PendienteDao {
    private static final String CONTEXTO =
            "SELECT n.color,n.nivel_oficial_rula,p.*,e.id_puesto,e.id_visita,pt.numero,s." +
                    "id_empresa,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_nomb" +
                    "re,v.estado visita_estado FROM pendiente_arreglo p JOIN evaluacion e ON e.id" +
                    "_evaluacion=p.id_evaluacion JOIN puesto_trabajo pt ON pt.id_puesto=e.id_pues" +
                    "to JOIN visita v ON v.id_visita=e.id_visita JOIN salon l ON l.id_salon=pt.id" +
                    "_salon JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s" +
                    ".id_empresa JOIN nivel_riesgo n ON p.puntaje BETWEEN n.puntaje_minimo AND n." + "puntaje_maximo";

    /**
     * Contexto visible sin rutas, imágenes ni datos de otras empresas. RNF-011.
     */
    public record Ficha(PendienteArreglo pendiente, int puesto, int numero, int visita, int empresa,
                        String empresaNombre, String sedeNombre, String salonNombre, String color, int nivel) {
    }

    /**
     * Acción histórica con autor y hora de Colombia. RNF-011.
     */
    public record Cambio(LocalDateTime fecha, String responsable, String accion, String anterior, String nuevo,
                         String detalle) {
    }

    /**
     * Crea el acceso parametrizado a pendientes. RNF-011.
     */
    public PendienteDao() {
    }

    /**
     * Lista el alcance permitido con orden fijo elegido por lista blanca. RNF-011.
     */
    public List<Ficha> listar(Connection conexion, int empresa, boolean finalizadas, String estado,
            String orden) throws SQLException {
        return listar(conexion, Alcance.exacta(empresa), finalizadas, estado, orden);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public List<Ficha> listar(Connection conexion, Alcance empresa, boolean finalizadas, String estado,
            String orden) throws SQLException {
        String ordenar = switch (orden) {
            case "prioridad" -> "CASE p.prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 E" +
                    "LSE 3 END,p.fecha_limite IS NULL,p.fecha_limite,p.id_pendiente DESC";
            case "limite" -> "p.fecha_limite IS NULL,p.fecha_limite,p.id_pendiente DESC";
            default -> "p.fecha_registro DESC,p.id_pendiente DESC";
        };
        return Sql.listar(conexion, CONTEXTO + " WHERE " + empresa.sql("s.id_empresa") +
                        " AND (?=FALSE OR v.estado='FINALIZADA') AND (?='TODOS' OR p.estado=?)" +
                        co.aplomo.modelo.Demostracion.filtro("s.id_empresa") + " ORDER BY " + ordenar,
                PendienteDao::ficha,
                empresa.todas(), empresa.valor(), finalizadas, estado, estado);
    }

    /**
     * Consulta dentro del alcance; bloquea solo el pendiente para no invertir bloqueos de análisis/visita. RNF-011.
     */
    public Ficha buscar(Connection conexion, int id, int empresa, boolean finalizadas,
            boolean bloquear) throws SQLException {
        return buscar(conexion, id, Alcance.exacta(empresa), finalizadas, bloquear);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public Ficha buscar(Connection conexion, int id, Alcance empresa, boolean finalizadas,
            boolean bloquear) throws SQLException {
        PendienteArreglo actual = bloquear ? Sql.uno(conexion,
                "SELECT * FROM pendiente_arreglo WHERE id_pendiente=? FOR UPDATE", PendienteDao::leer, id) : null;
        var elemento = Sql.uno(conexion, CONTEXTO + " WHERE p.id_pendiente=? AND " + empresa.sql("s.id_empresa") +
                        " AND (?=FALSE OR v.estado='FINALIZADA')", PendienteDao::ficha, id, empresa.todas(),
                empresa.valor(),
                finalizadas);
        return elemento == null ? null :
                actual == null ? elemento : new Ficha(actual, elemento.puesto(), elemento.numero(), elemento.visita(),
                        elemento.empresa(), elemento.empresaNombre(), elemento.sedeNombre(), elemento.salonNombre(),
                        elemento.color(), elemento.nivel());
    }

    /**
     * Consulta la única fila de una evaluación dentro de la transacción del análisis. RNF-011.
     */
    public PendienteArreglo porEvaluacion(Connection conexion, int evaluacion) throws SQLException {
        return Sql.uno(conexion, "SELECT * FROM pendiente_arreglo WHERE id_evaluacion=? FOR UPDATE", PendienteDao::leer,
                evaluacion);
    }

    /**
     * Crea el pendiente sin inventar descripción, prioridad ni fecha límite. RNF-011.
     */
    public int crear(Connection conexion, int evaluacion, int puntaje, LocalDate fecha) throws SQLException {
        return Sql.insertar(conexion,
                "INSERT INTO pendiente_arreglo (id_evaluacion,puntaje,fecha_registro) VALUES " + "(?,?,?)", evaluacion,
                puntaje, fecha);
    }

    /**
     * Actualiza solo el máximo, conservando seguimiento, registro y estado incluso si está Arreglado. RNF-011.
     */
    public void puntaje(Connection conexion, int id, int puntaje) throws SQLException {
        Sql.actualizar(conexion, "UPDATE pendiente_arreglo SET puntaje=? WHERE id_pendiente=?", puntaje, id);
    }

    /**
     * Guarda los datos de seguimiento sin aceptar modificaciones de estado ni puntaje. RNF-011.
     */
    public void editar(Connection conexion, int id, String descripcion, String prioridad,
            LocalDate limite) throws SQLException {
        Sql.actualizar(conexion,
                "UPDATE pendiente_arreglo SET descripcion=?,prioridad=?,fecha_limite=? WHERE " + "id_pendiente=?",
                descripcion, prioridad, limite, id);
    }

    /**
     * Marca Arreglado sin modificar el RULA ni borrar otros pendientes del puesto. RNF-011.
     */
    public void arreglar(Connection conexion, int id) throws SQLException {
        Sql.actualizar(conexion, "UPDATE pendiente_arreglo SET estado='ARREGLADO' WHERE id_pendiente=?", id);
    }

    /**
     * Lee acciones de este pendiente después de comprobar el alcance de quien consulta. RNF-011.
     */
    public List<Cambio> historial(Connection conexion, int id) throws SQLException {
        return Sql.listar(conexion, "SELECT b.*,q.nombre responsable FROM bitacora b JOIN cuenta q USING(id_cuent" +
                        "a) WHERE b.tabla='pendiente_arreglo' AND b.id_registro=? ORDER BY b.id_bitac" + "ora DESC",
                fila -> new Cambio(fila.getTimestamp("fecha_hora").toLocalDateTime(), fila.getString("responsable"),
                        fila.getString("accion"), fila.getString("estado_anterior"), fila.getString("estado_nuevo"),
                        fila.getString("detalle")), id);
    }

    private static PendienteArreglo leer(ResultSet fila) throws SQLException {
        var fecha = fila.getDate("fecha_limite");
        return new PendienteArreglo(fila.getInt("id_pendiente"), fila.getInt("id_evaluacion"),
                fila.getString("descripcion"), fila.getString("prioridad"), fecha == null ? null : fecha.toLocalDate(),
                fila.getInt("puntaje"), fila.getDate("fecha_registro").toLocalDate(), fila.getString("estado"));
    }

    private static Ficha ficha(ResultSet fila) throws SQLException {
        return new Ficha(leer(fila), fila.getInt("id_puesto"), fila.getInt("numero"), fila.getInt("id_visita"),
                fila.getInt("id_empresa"), fila.getString("empresa_nombre"), fila.getString("sede_nombre"),
                fila.getString("salon_nombre"), fila.getString("color"), fila.getInt("nivel_oficial_rula"));
    }
}
