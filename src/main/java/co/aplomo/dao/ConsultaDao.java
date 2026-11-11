package co.aplomo.dao;
import co.aplomo.db.Sql;import co.aplomo.modelo.SeccionConsulta;import java.sql.*;import java.util.*;
/** RF-024, RF-002: solo SELECT parametrizados; la empresa y las visitas visibles se limitan antes de filtrar. */
public final class ConsultaDao {
    /** Fila de consulta sin fotos, rutas de archivos ni credenciales. */
    public record Fila(Map<String,String> datos){
        /** Lee una columna; los nulos de MySQL se muestran sin inventar resultados. */ public String valor(String clave){return datos.getOrDefault(clave,"");}
        /** Identificador del registro ya autorizado. */ public int id(){return Integer.parseInt(valor("id"));}
    }
    /** Opción autorizada de un catálogo, con sus relaciones para mostrar el contexto. */
    public record Opcion(int id,int empresa,int sede,String nombre) {}
    private static final String LUGAR=" JOIN salon l ON l.id_salon=p.id_salon JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s.id_empresa LEFT JOIN area ar ON ar.id_area=p.id_area LEFT JOIN cargo ca ON ca.id_cargo=p.id_cargo ";
    private static final String DATOS_LUGAR="s.id_empresa empresa,l.id_sede sede,l.id_salon salon,p.id_area area,p.id_cargo cargo,p.id_puesto puesto,p.numero,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_nombre,ar.nombre area_nombre,ca.nombre cargo_nombre,";
    private static final String MAXIMO="(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a ON a.id_foto=f.id_foto WHERE f.id_evaluacion=e.id_evaluacion)";
    /** Crea el acceso de consulta (RNF-011). */ public ConsultaDao(){}
    /** Lee una sección dentro del alcance; id cero consulta la lista completa autorizada. */
    public List<Fila> filas(Connection c,SeccionConsulta seccion,int empresa,boolean usuario,int id)throws SQLException{
        String select,desde,extra="";List<Object> params=new ArrayList<>();
        switch(seccion){
            case EMPRESAS->{select="em.id_empresa id,em.id_empresa empresa,em.nombre nombre,em.nombre empresa_nombre,em.nit,em.descripcion,"+activo("em")+" estado,NULL puntaje";desde="empresa em";}
            case AREAS->{select="ar.id_area id,ar.id_area area,em.id_empresa empresa,ar.nombre nombre,em.nombre empresa_nombre,"+activo("ar")+" estado,NULL puntaje";desde="area ar JOIN empresa em ON em.id_empresa=ar.id_empresa";}
            case CARGOS->{select="ca.id_cargo id,ca.id_cargo cargo,em.id_empresa empresa,ca.nombre nombre,ca.tipo,em.nombre empresa_nombre,"+activo("ca")+" estado,NULL puntaje";desde="cargo ca JOIN empresa em ON em.id_empresa=ca.id_empresa";}
            case SEDES->{select="s.id_sede id,s.id_sede sede,em.id_empresa empresa,s.nombre nombre,s.ciudad,s.direccion,em.nombre empresa_nombre,"+activo("s")+" estado,NULL puntaje";desde="sede s JOIN empresa em ON em.id_empresa=s.id_empresa";}
            case SALONES->{select="l.id_salon id,l.id_salon salon,l.id_sede sede,em.id_empresa empresa,l.nombre nombre,l.piso,l.capacidad_puestos capacidad,l.descripcion,em.nombre empresa_nombre,s.nombre sede_nombre,"+activo("l")+" estado,NULL puntaje";desde="salon l JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s.id_empresa";}
            case PUESTOS->{select="p.id_puesto id,"+DATOS_LUGAR+"p.descripcion nombre,p.descripcion,p.ubicacion,p.tipo,"+activo("p")+" estado,(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a ON a.id_foto=f.id_foto WHERE f.id_evaluacion=(SELECT MAX(e.id_evaluacion) FROM evaluacion e JOIN visita v ON v.id_visita=e.id_visita WHERE e.id_puesto=p.id_puesto AND (?=FALSE OR v.estado='FINALIZADA'))) puntaje";desde="puesto_trabajo p"+LUGAR;params.add(usuario);}
            case RESERVAS->{select="r.id_reserva id,s.id_empresa empresa,l.id_sede sede,l.id_salon salon,r.codigo,r.fecha,r.hora_inicio inicio,r.hora_fin fin,r.categoria tipo,r.descripcion,r.notas,r.motivo_cancelacion motivo,r.estado,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_nombre,NULL puntaje";desde="reserva r JOIN salon l ON l.id_salon=r.id_salon JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s.id_empresa";}
            case VISITAS->{select="v.id_visita id,s.id_empresa empresa,l.id_sede sede,l.id_salon salon,r.codigo,v.hora_real_inicio fecha,v.hora_real_fin fin,v.notas,v.estado,v.calificacion_final calificacion,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_nombre,NULL puntaje";desde="visita v JOIN reserva r ON r.id_reserva=v.id_reserva JOIN salon l ON l.id_salon=r.id_salon JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s.id_empresa";extra=" AND (?=FALSE OR v.estado='FINALIZADA')";}
            case EVALUACIONES->{select="e.id_evaluacion id,e.id_evaluacion evaluacion,e.id_visita visita,"+DATOS_LUGAR+"e.fecha,e.postura nombre,e.postura,e.criterio,e.motivo,e.minutos_jornada minutos,v.estado,"+MAXIMO+" puntaje";desde="evaluacion e JOIN visita v ON v.id_visita=e.id_visita JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto"+LUGAR;extra=" AND (?=FALSE OR v.estado='FINALIZADA')";}
            case PENDIENTES->{select="pa.id_pendiente id,e.id_evaluacion evaluacion,e.id_visita visita,"+DATOS_LUGAR+"pa.descripcion nombre,pa.descripcion,pa.prioridad,pa.fecha_limite,pa.fecha_registro fecha,pa.estado,pa.puntaje";desde="pendiente_arreglo pa JOIN evaluacion e ON e.id_evaluacion=pa.id_evaluacion JOIN visita v ON v.id_visita=e.id_visita JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto"+LUGAR;extra=" AND (?=FALSE OR v.estado='FINALIZADA')";}
            case SUGERENCIAS->{select="sg.id_sugerencia id,e.id_evaluacion evaluacion,e.id_visita visita,"+DATOS_LUGAR+"sg.titulo nombre,sg.descripcion,sg.tipo,sg.prioridad,sg.origen,sg.estado,sg.fecha_limite fecha,"+MAXIMO+" puntaje";desde="sugerencia sg JOIN evaluacion e ON e.id_evaluacion=sg.id_evaluacion JOIN visita v ON v.id_visita=e.id_visita JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto"+LUGAR;extra=" AND sg.activo=TRUE AND (?=FALSE OR v.estado='FINALIZADA')";}
            default->throw new IllegalArgumentException("Sección no disponible.");
        }
        String sql="SELECT q.*,n.color,n.nivel_oficial_rula nivel FROM (SELECT "+select+" FROM "+desde+" WHERE (?=0 OR em.id_empresa=?)"+extra+") q LEFT JOIN nivel_riesgo n ON q.puntaje BETWEEN n.puntaje_minimo AND n.puntaje_maximo WHERE (?=0 OR q.id=?)";
        params.add(empresa);params.add(empresa);if(!extra.isEmpty())params.add(usuario);params.add(id);params.add(id);
        return Sql.listar(c,sql,ConsultaDao::fila,params.toArray());
    }
    /** Catálogos con filtro SQL de empresa para impedir opciones ajenas en los formularios. */
    public Map<String,List<Opcion>> catalogos(Connection c,int empresa)throws SQLException{
        Map<String,List<Opcion>> mapa=new LinkedHashMap<>();
        mapa.put("empresa",opciones(c,"SELECT em.id_empresa id,em.id_empresa empresa,0 sede,em.nombre nombre FROM empresa em",empresa));
        mapa.put("sede",opciones(c,"SELECT s.id_sede id,s.id_empresa empresa,s.id_sede sede,CONCAT(s.nombre,' · ',em.nombre) nombre FROM sede s JOIN empresa em ON em.id_empresa=s.id_empresa",empresa));
        mapa.put("salon",opciones(c,"SELECT l.id_salon id,s.id_empresa empresa,l.id_sede sede,CONCAT(l.nombre,' · ',s.nombre,' · ',em.nombre) nombre FROM salon l JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=s.id_empresa",empresa));
        mapa.put("area",opciones(c,"SELECT a.id_area id,a.id_empresa empresa,0 sede,CONCAT(a.nombre,' · ',em.nombre) nombre FROM area a JOIN empresa em ON em.id_empresa=a.id_empresa",empresa));
        mapa.put("cargo",opciones(c,"SELECT a.id_cargo id,a.id_empresa empresa,0 sede,CONCAT(a.nombre,' · ',em.nombre) nombre FROM cargo a JOIN empresa em ON em.id_empresa=a.id_empresa",empresa));return Map.copyOf(mapa);
    }
    /** Niveles y colores oficiales para los filtros, desde la misma tabla MySQL. */
    public Map<String,List<String>> riesgos(Connection c)throws SQLException{return Map.of("color",Sql.listar(c,"SELECT color FROM nivel_riesgo GROUP BY color ORDER BY MIN(puntaje_minimo)",r->r.getString(1)),"nivel",Sql.listar(c,"SELECT DISTINCT nivel_oficial_rula FROM nivel_riesgo ORDER BY nivel_oficial_rula",r->r.getString(1)));}
    private static List<Opcion> opciones(Connection c,String sql,int empresa)throws SQLException{return Sql.listar(c,sql+" WHERE (?=0 OR em.id_empresa=?) ORDER BY nombre,id",r->new Opcion(r.getInt("id"),r.getInt("empresa"),r.getInt("sede"),r.getString("nombre")),empresa,empresa);}
    private static String activo(String alias){return "CASE WHEN "+alias+".activo THEN 'ACTIVO' ELSE 'INACTIVO' END";}
    private static Fila fila(ResultSet r)throws SQLException{Map<String,String> datos=new LinkedHashMap<>();var m=r.getMetaData();for(int i=1;i<=m.getColumnCount();i++){String valor=r.getString(i);datos.put(m.getColumnLabel(i),valor==null?"":valor);}return new Fila(Map.copyOf(datos));}
}
