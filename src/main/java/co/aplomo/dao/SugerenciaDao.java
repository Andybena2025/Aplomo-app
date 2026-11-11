package co.aplomo.dao;
import co.aplomo.db.Sql;import co.aplomo.modelo.Sugerencia;import java.sql.*;import java.util.*;
/** RF-023a/b, RF-002: reglas y sugerencias parametrizadas, sin eliminar registros ni exponer fotos. */
public final class SugerenciaDao {
    /** Contexto autorizado de una evaluación, sin rutas de fotografías. */
    public record Contexto(int evaluacion,int puesto,int numero,int visita,int empresa,int sede,int salon,String empresaNombre,String sedeNombre,String salonNombre,String postura,Integer puntaje,String color,int nivel) {}
    /** Sugerencia acompañada de su puesto y empresa para la consulta. */
    public record Ficha(Sugerencia sugerencia,Contexto contexto) {}
    /** Regla configurable en la tabla aprobada; su condición se interpreta por lista blanca. */
    public record Regla(int id,String segmento,int minimo,String condicion,String texto) {}
    private static final String CONTEXTO="SELECT e.id_evaluacion,e.id_puesto,e.id_visita,e.postura,p.numero,s.id_empresa,l.id_sede,l.id_salon,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_nombre,v.estado visita_estado,r.puntaje,n.color,n.nivel_oficial_rula FROM evaluacion e JOIN puesto_trabajo p USING(id_puesto) JOIN visita v USING(id_visita) JOIN salon l ON l.id_salon=p.id_salon JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa) LEFT JOIN (SELECT f.id_evaluacion,MAX(a.puntaje_final) puntaje FROM fotografia f JOIN analisis_rula a USING(id_foto) GROUP BY f.id_evaluacion) r ON r.id_evaluacion=e.id_evaluacion LEFT JOIN nivel_riesgo n ON r.puntaje BETWEEN n.puntaje_minimo AND n.puntaje_maximo";
    /** Crea el acceso a sugerencias con JDBC (RNF-011). */
    public SugerenciaDao() {}
    /** Consulta el alcance y, al cambiar, bloquea primero la evaluación para serializar sus sugerencias. */
    public Contexto contexto(Connection c,int id,int empresa,boolean finalizadas,boolean bloquear)throws SQLException {
        if(bloquear)Sql.uno(c,"SELECT id_evaluacion FROM evaluacion WHERE id_evaluacion=? FOR UPDATE",r->r.getInt(1),id);
        var contexto=Sql.uno(c,CONTEXTO+" WHERE e.id_evaluacion=? AND (?=0 OR s.id_empresa=?) AND (?=FALSE OR v.estado='FINALIZADA')",SugerenciaDao::contexto,id,empresa,empresa,finalizadas);
        if(!bloquear||contexto==null)return contexto;
        var resultado=Sql.uno(c,"SELECT a.puntaje_final,a.id_nivel FROM analisis_rula a WHERE a.id_foto IN (SELECT id_foto FROM fotografia WHERE id_evaluacion=?) ORDER BY a.puntaje_final DESC,a.id_analisis DESC LIMIT 1 FOR UPDATE",r->new int[]{r.getInt(1),r.getInt(2)},id);
        if(resultado==null)return contexto;
        var nivel=Sql.uno(c,"SELECT color,nivel_oficial_rula FROM nivel_riesgo WHERE id_nivel=?",r->new String[]{r.getString(1),r.getString(2)},resultado[1]);
        var e=contexto;return new Contexto(e.evaluacion(),e.puesto(),e.numero(),e.visita(),e.empresa(),e.sede(),e.salon(),e.empresaNombre(),e.sedeNombre(),e.salonNombre(),e.postura(),resultado[0],nivel[0],Integer.parseInt(nivel[1]));
    }
    /** Lista solo sugerencias activas dentro del alcance; evaluación cero significa todas. */
    public List<Ficha> listar(Connection c,int empresa,boolean finalizadas,int evaluacion)throws SQLException {
        var contextos=Sql.listar(c,CONTEXTO+" WHERE (?=0 OR s.id_empresa=?) AND (?=FALSE OR v.estado='FINALIZADA') AND (?=0 OR e.id_evaluacion=?) ORDER BY e.id_evaluacion DESC",SugerenciaDao::contexto,empresa,empresa,finalizadas,evaluacion,evaluacion);
        List<Ficha> salida=new ArrayList<>();for(var e:contextos)for(var s:porEvaluacion(c,e.evaluacion(),true))salida.add(new Ficha(s,e));return List.copyOf(salida);
    }
    /** Lee una fila; el servicio comprueba después su empresa y visita. */
    public Sugerencia buscar(Connection c,int id)throws SQLException{return Sql.uno(c,"SELECT * FROM sugerencia WHERE id_sugerencia=?",SugerenciaDao::leer,id);}
    /** Lee la fila vigente bajo el bloqueo previo de la evaluación. */
    public Sugerencia buscarActual(Connection c,int id)throws SQLException{return Sql.uno(c,"SELECT * FROM sugerencia WHERE id_sugerencia=? FOR UPDATE",SugerenciaDao::leer,id);}
    /** Lee sugerencias vigentes incluso si se esperó a otra operación concurrente. */
    public List<Sugerencia> actuales(Connection c,int evaluacion)throws SQLException{return Sql.listar(c,"SELECT * FROM sugerencia WHERE id_evaluacion=? ORDER BY id_sugerencia FOR UPDATE",SugerenciaDao::leer,evaluacion);}
    /** Conserva también las filas quitadas al comprobar si una regla ya fue propuesta. */
    public List<Sugerencia> porEvaluacion(Connection c,int id,boolean activas)throws SQLException{return Sql.listar(c,"SELECT * FROM sugerencia WHERE id_evaluacion=? AND (?=FALSE OR activo=TRUE) ORDER BY id_sugerencia",SugerenciaDao::leer,id,activas);}
    /** Lee reglas configuradas, sin constantes de umbrales en Java. */
    public List<Regla> reglas(Connection c)throws SQLException{return Sql.listar(c,"SELECT * FROM regla_sugerencia ORDER BY id_regla",r->new Regla(r.getInt("id_regla"),r.getString("segmento"),r.getInt("puntaje_minimo"),r.getString("condicion"),r.getString("texto_sugerido")));}
    /** Inserta una sugerencia del sistema o del Administrador. */
    public int crear(Connection c,Sugerencia s)throws SQLException{return Sql.insertar(c,"INSERT INTO sugerencia (id_evaluacion,id_regla,titulo,descripcion,tipo,prioridad,estado,fecha_limite,origen) VALUES (?,?,?,?,?,?,?,?,?)",s.evaluacion(),s.regla(),s.titulo(),s.descripcion(),s.tipo(),s.prioridad(),s.estado(),s.limite(),s.origen());}
    /** Edita únicamente los campos permitidos; conserva origen, regla y estado. */
    public void editar(Connection c,Sugerencia s)throws SQLException{Sql.actualizar(c,"UPDATE sugerencia SET titulo=?,descripcion=?,tipo=?,prioridad=?,fecha_limite=? WHERE id_sugerencia=?",s.titulo(),s.descripcion(),s.tipo(),s.prioridad(),s.limite(),s.id());}
    /** Cambia el estado sin sobrescribir la descripción ni el origen. */
    public void estado(Connection c,int id,String estado)throws SQLException{Sql.actualizar(c,"UPDATE sugerencia SET estado=? WHERE id_sugerencia=?",estado,id);}
    /** Quita de las listas sin borrar la fila ni su relación con la regla. */
    public void quitar(Connection c,int id)throws SQLException{Sql.actualizar(c,"UPDATE sugerencia SET activo=FALSE WHERE id_sugerencia=?",id);}
    /** Comprueba todas las sugerencias activas del puesto para ofrecer su Reevaluación. */
    public boolean todasHechas(Connection c,int puesto)throws SQLException{return Boolean.TRUE.equals(Sql.uno(c,"SELECT COUNT(*)>0 AND SUM(s.estado<>'HECHA')=0 FROM sugerencia s JOIN evaluacion e USING(id_evaluacion) WHERE e.id_puesto=? AND s.activo=TRUE",r->r.getBoolean(1),puesto));}
    private static Contexto contexto(ResultSet r)throws SQLException{return new Contexto(r.getInt("id_evaluacion"),r.getInt("id_puesto"),r.getInt("numero"),r.getInt("id_visita"),r.getInt("id_empresa"),r.getInt("id_sede"),r.getInt("id_salon"),r.getString("empresa_nombre"),r.getString("sede_nombre"),r.getString("salon_nombre"),r.getString("postura"),Sql.enteroOpcional(r,"puntaje"),r.getString("color"),r.getInt("nivel_oficial_rula"));}
    private static Sugerencia leer(ResultSet r)throws SQLException{var d=r.getDate("fecha_limite");return new Sugerencia(r.getInt("id_sugerencia"),r.getInt("id_evaluacion"),Sql.enteroOpcional(r,"id_regla"),r.getString("titulo"),r.getString("descripcion"),r.getString("tipo"),r.getString("prioridad"),r.getString("estado"),d==null?null:d.toLocalDate(),r.getString("origen"),r.getBoolean("activo"));}
}
