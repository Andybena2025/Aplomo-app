package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.InformeEmpresa.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-025/026: lecturas parametrizadas de una empresa, siempre sobre visitas Finalizadas. */
public final class CalificacionesDao {
    private static final String LUGAR = " JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto"
            + " JOIN salon l ON l.id_salon=p.id_salon JOIN sede s ON s.id_sede=l.id_sede ";
    /** Identifica la sede sin exponer información fotográfica. */
    public record PorSede<T>(int sede, T dato) {}
    /** Crea el lector de indicadores. */
    public CalificacionesDao() {}

    /** Lee las visitas históricas y sus promedios guardados, sin incluir En curso. */
    public List<PorSede<VisitaResumen>> visitas(Connection c, int empresa) throws SQLException {
        return Sql.listar(c, "SELECT v.*,r.codigo,l.id_sede FROM visita v JOIN reserva r USING(id_reserva)"
                + " JOIN salon l ON l.id_salon=r.id_salon JOIN sede s ON s.id_sede=l.id_sede"
                + " WHERE s.id_empresa=? AND v.estado='FINALIZADA' ORDER BY v.hora_real_inicio,v.id_visita",
                r -> new PorSede<>(r.getInt("id_sede"),new VisitaResumen(r.getInt("id_visita"),r.getString("codigo"),
                        r.getTimestamp("hora_real_inicio").toLocalDateTime(),r.getTimestamp("hora_real_fin").toLocalDateTime(),
                        r.getString("notas"),promedio(r.getBigDecimal("calificacion_final")))), empresa);
    }

    /** Cada puesto aparece una vez: máximo de los análisis de su última evaluación Finalizada (Andy, 11 nov 2026). */
    public List<PorSede<PuestoResumen>> puestos(Connection c, int empresa) throws SQLException {
        String sql = "SELECT p.id_puesto,p.numero,p.descripcion,l.nombre salon,s.id_sede,ar.nombre area,ca.nombre cargo,ca.tipo tipo_cargo,"
                + "e.id_evaluacion,e.id_visita,e.postura,e.motivo,e.fecha,cp.id_evaluacion condiciones_id,cp.*,"
                + "(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=e.id_evaluacion) puntaje"
                + " FROM evaluacion e JOIN visita v ON v.id_visita=e.id_visita" + LUGAR
                + " LEFT JOIN area ar ON ar.id_area=p.id_area LEFT JOIN cargo ca ON ca.id_cargo=p.id_cargo"
                + " LEFT JOIN condiciones_puesto cp ON cp.id_evaluacion=e.id_evaluacion"
                + " WHERE s.id_empresa=? AND v.estado='FINALIZADA' AND e.id_evaluacion=(SELECT MAX(e2.id_evaluacion)"
                + " FROM evaluacion e2 JOIN visita v2 ON v2.id_visita=e2.id_visita WHERE e2.id_puesto=p.id_puesto AND v2.estado='FINALIZADA')"
                + " ORDER BY s.nombre,l.nombre,p.numero,p.id_puesto";
        return Sql.listar(c,sql,r->new PorSede<>(r.getInt("id_sede"),new PuestoResumen(r.getInt("id_puesto"),
                r.getInt("id_evaluacion"),r.getInt("id_visita"),r.getInt("numero"),r.getString("salon"),r.getString("area"),
                r.getString("cargo"),r.getString("tipo_cargo"),r.getString("descripcion"),r.getString("postura"),
                r.getString("motivo"),condiciones(r),r.getDate("fecha").toLocalDate(),Sql.enteroOpcional(r,"puntaje"),null)),empresa);
    }

    /** Cuenta cada fotografía analizada una sola vez, aunque tenga varios análisis o lados. */
    public Map<Integer,Integer> fotosAnalizadas(Connection c,int empresa)throws SQLException {
        var filas=Sql.listar(c,"SELECT s.id_sede,COUNT(*) cantidad FROM fotografia f JOIN evaluacion e USING(id_evaluacion)"
                + " JOIN visita v ON v.id_visita=e.id_visita"+LUGAR+" WHERE s.id_empresa=? AND v.estado='FINALIZADA'"
                + " AND EXISTS(SELECT 1 FROM analisis_rula a WHERE a.id_foto=f.id_foto) GROUP BY s.id_sede",
                r->Map.entry(r.getInt(1),r.getInt(2)),empresa);
        Map<Integer,Integer> total=new HashMap<>();filas.forEach(x->total.put(x.getKey(),x.getValue()));return Map.copyOf(total);
    }

    /** Lee todos los pendientes Finalizados, abiertos y arreglados, sin sustituir su estado por el último color. */
    public List<PorSede<PendienteResumen>> pendientes(Connection c,int empresa)throws SQLException {
        return Sql.listar(c,"SELECT pa.*,p.numero,l.nombre salon,s.id_sede FROM pendiente_arreglo pa"
                + " JOIN evaluacion e ON e.id_evaluacion=pa.id_evaluacion JOIN visita v ON v.id_visita=e.id_visita"+LUGAR
                + " WHERE s.id_empresa=? AND v.estado='FINALIZADA' ORDER BY pa.fecha_registro,pa.id_pendiente",
                r->new PorSede<>(r.getInt("id_sede"),new PendienteResumen(r.getInt("id_pendiente"),r.getInt("id_evaluacion"),
                        r.getInt("numero"),r.getString("salon"),r.getString("descripcion"),r.getString("prioridad"),r.getString("estado"),
                        r.getDate("fecha_registro").toLocalDate(),fecha(r,"fecha_limite"),r.getInt("puntaje"),null)),empresa);
    }

    /** Lee únicamente sugerencias activas y conserva su clasificación y estado en el historial Finalizado. */
    public List<PorSede<SugerenciaResumen>> sugerencias(Connection c,int empresa)throws SQLException {
        return Sql.listar(c,"SELECT sg.*,p.numero,l.nombre salon,s.id_sede FROM sugerencia sg"
                + " JOIN evaluacion e ON e.id_evaluacion=sg.id_evaluacion JOIN visita v ON v.id_visita=e.id_visita"+LUGAR
                + " WHERE s.id_empresa=? AND v.estado='FINALIZADA' AND sg.activo=TRUE ORDER BY sg.id_sugerencia",
                r->new PorSede<>(r.getInt("id_sede"),new SugerenciaResumen(r.getInt("id_sugerencia"),r.getInt("id_evaluacion"),
                        r.getInt("numero"),r.getString("salon"),r.getString("titulo"),r.getString("descripcion"),r.getString("tipo"),
                        r.getString("estado"),r.getString("prioridad"),r.getString("origen"),fecha(r,"fecha_limite"))),empresa);
    }

    /** Obtiene rangos, nivel escrito y acciones del semáforo aprobado en MySQL. */
    public List<Riesgo> riesgos(Connection c)throws SQLException {
        return Sql.listar(c,"SELECT * FROM nivel_riesgo ORDER BY puntaje_minimo",r->new Riesgo(r.getInt("puntaje_minimo"),
                r.getInt("puntaje_maximo"),r.getString("color"),r.getInt("nivel_oficial_rula"),r.getString("nombre"),r.getString("accion")));
    }

    private static Calificacion promedio(BigDecimal p){return p==null?null:new Calificacion(p,0,null);}
    private static LocalDate fecha(ResultSet r,String nombre)throws SQLException{var f=r.getDate(nombre);return f==null?null:f.toLocalDate();}
    private static String condiciones(ResultSet r)throws SQLException {
        if(r.getObject("condiciones_id")==null)return "Sin condiciones registradas.";
        String[][] campos={{"escritorio_muy_alto","Escritorio muy alto"},{"pantalla_muy_alta","Pantalla muy alta"},
                {"pies_colgando","Pies colgando"},{"tiene_apoyapies","Apoyapiés"},{"silla_con_apoyabrazos","Silla con apoyabrazos"},
                {"silla_regulable","Silla regulable"}};
        List<String> textos=new ArrayList<>();for(var campo:campos)textos.add(campo[1]+": "+(r.getBoolean(campo[0])?"sí":"no"));
        String otras=r.getString("otras_observaciones");if(otras!=null&&!otras.isBlank())textos.add("Otras observaciones: "+otras);
        return String.join(" · ",textos);
    }
}
