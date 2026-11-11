package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.PendienteArreglo;
import java.sql.*;import java.time.*;import java.util.*;

/** RF-022: pendientes e historial consultables por empresa y estado de visita, sin fotos. */
public final class PendienteDao {
    private static final String CONTEXTO="SELECT n.color,n.nivel_oficial_rula,p.*,e.id_puesto,e.id_visita,pt.numero,s.id_empresa,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_nombre,v.estado visita_estado FROM pendiente_arreglo p JOIN evaluacion e ON e.id_evaluacion=p.id_evaluacion JOIN puesto_trabajo pt ON pt.id_puesto=e.id_puesto JOIN visita v ON v.id_visita=e.id_visita JOIN salon l ON l.id_salon=pt.id_salon JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s.id_empresa JOIN nivel_riesgo n ON p.puntaje BETWEEN n.puntaje_minimo AND n.puntaje_maximo";
    /** Contexto visible sin rutas, imágenes ni datos de otras empresas. */
    public record Ficha(PendienteArreglo pendiente,int puesto,int numero,int visita,int empresa,String empresaNombre,String sedeNombre,String salonNombre,String color,int nivel) {}
    /** Acción histórica con autor y hora de Colombia. */
    public record Cambio(LocalDateTime fecha,String responsable,String accion,String anterior,String nuevo,String detalle) {}
    /** Crea el acceso parametrizado a pendientes. */
    public PendienteDao() {}
    /** Lista el alcance permitido con orden fijo elegido por lista blanca. */
    public List<Ficha> listar(Connection c,int empresa,boolean finalizadas,String estado,String orden)throws SQLException{
        String ordenar=switch(orden){case "prioridad"->"CASE p.prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END,p.fecha_limite IS NULL,p.fecha_limite,p.id_pendiente DESC";case "limite"->"p.fecha_limite IS NULL,p.fecha_limite,p.id_pendiente DESC";default->"p.fecha_registro DESC,p.id_pendiente DESC";};
        return Sql.listar(c,CONTEXTO+" WHERE (?=0 OR s.id_empresa=?) AND (?=FALSE OR v.estado='FINALIZADA') AND (?='TODOS' OR p.estado=?) ORDER BY "+ordenar,PendienteDao::ficha,empresa,empresa,finalizadas,estado,estado);
    }
    /** Consulta dentro del alcance; bloquea solo el pendiente para no invertir bloqueos de análisis/visita. */
    public Ficha buscar(Connection c,int id,int empresa,boolean finalizadas,boolean bloquear)throws SQLException{
        PendienteArreglo actual=bloquear?Sql.uno(c,"SELECT * FROM pendiente_arreglo WHERE id_pendiente=? FOR UPDATE",PendienteDao::leer,id):null;
        var f=Sql.uno(c,CONTEXTO+" WHERE p.id_pendiente=? AND (?=0 OR s.id_empresa=?) AND (?=FALSE OR v.estado='FINALIZADA')",PendienteDao::ficha,id,empresa,empresa,finalizadas);
        return f==null?null:actual==null?f:new Ficha(actual,f.puesto(),f.numero(),f.visita(),f.empresa(),f.empresaNombre(),f.sedeNombre(),f.salonNombre(),f.color(),f.nivel());
    }
    /** Consulta la única fila de una evaluación dentro de la transacción del análisis. */
    public PendienteArreglo porEvaluacion(Connection c,int evaluacion)throws SQLException{
        return Sql.uno(c,"SELECT * FROM pendiente_arreglo WHERE id_evaluacion=? FOR UPDATE",PendienteDao::leer,evaluacion);
    }
    /** Crea el pendiente sin inventar descripción, prioridad ni fecha límite. */
    public int crear(Connection c,int evaluacion,int puntaje,LocalDate fecha)throws SQLException{
        return Sql.insertar(c,"INSERT INTO pendiente_arreglo (id_evaluacion,puntaje,fecha_registro) VALUES (?,?,?)",evaluacion,puntaje,fecha);
    }
    /** Actualiza solo el máximo, conservando seguimiento, registro y estado incluso si está Arreglado. */
    public void puntaje(Connection c,int id,int puntaje)throws SQLException{Sql.actualizar(c,"UPDATE pendiente_arreglo SET puntaje=? WHERE id_pendiente=?",puntaje,id);}
    /** Guarda los datos de seguimiento sin aceptar modificaciones de estado ni puntaje. */
    public void editar(Connection c,int id,String descripcion,String prioridad,LocalDate limite)throws SQLException{
        Sql.actualizar(c,"UPDATE pendiente_arreglo SET descripcion=?,prioridad=?,fecha_limite=? WHERE id_pendiente=?",descripcion,prioridad,limite,id);
    }
    /** Marca Arreglado sin modificar el RULA ni borrar otros pendientes del puesto. */
    public void arreglar(Connection c,int id)throws SQLException{Sql.actualizar(c,"UPDATE pendiente_arreglo SET estado='ARREGLADO' WHERE id_pendiente=?",id);}
    /** Lee acciones de este pendiente después de comprobar el alcance de quien consulta. */
    public List<Cambio> historial(Connection c,int id)throws SQLException{
        return Sql.listar(c,"SELECT b.*,q.nombre responsable FROM bitacora b JOIN cuenta q USING(id_cuenta) WHERE b.tabla='pendiente_arreglo' AND b.id_registro=? ORDER BY b.id_bitacora DESC",r->new Cambio(r.getTimestamp("fecha_hora").toLocalDateTime(),r.getString("responsable"),r.getString("accion"),r.getString("estado_anterior"),r.getString("estado_nuevo"),r.getString("detalle")),id);
    }
    private static PendienteArreglo leer(ResultSet r)throws SQLException{var fecha=r.getDate("fecha_limite");return new PendienteArreglo(r.getInt("id_pendiente"),r.getInt("id_evaluacion"),r.getString("descripcion"),r.getString("prioridad"),fecha==null?null:fecha.toLocalDate(),r.getInt("puntaje"),r.getDate("fecha_registro").toLocalDate(),r.getString("estado"));}
    private static Ficha ficha(ResultSet r)throws SQLException{return new Ficha(leer(r),r.getInt("id_puesto"),r.getInt("numero"),r.getInt("id_visita"),r.getInt("id_empresa"),r.getString("empresa_nombre"),r.getString("sede_nombre"),r.getString("salon_nombre"),r.getString("color"),r.getInt("nivel_oficial_rula"));}
}
