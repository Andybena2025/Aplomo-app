package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.modelo.InformeEmpresa.Calificacion;
import co.aplomo.modelo.InformeEmpresa.PendienteResumen;
import co.aplomo.modelo.InformeEmpresa.PuestoResumen;
import co.aplomo.modelo.InformeEmpresa.Riesgo;
import co.aplomo.modelo.InformeEmpresa.SugerenciaResumen;
import co.aplomo.modelo.InformeEmpresa.VisitaResumen;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-025/026: lecturas parametrizadas de una empresa, siempre sobre visitas Finalizadas.
 */
public final class CalificacionesDao {
    private static final String LUGAR = " JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto" +
            " JOIN salon l ON l.id_salon=p.id_salon JOIN sede s ON s.id_sede=l.id_sede ";

    /**
     * Identifica la sede sin exponer información fotográfica. RNF-011.
     */
    public record PorSede<T>(int sede, T dato) {
    }

    /**
     * Crea el lector de indicadores. RNF-011.
     */
    public CalificacionesDao() {
    }

    /**
     * Lee las visitas históricas y sus promedios guardados, sin incluir En curso. RNF-011.
     */
    public List<PorSede<VisitaResumen>> visitas(Connection conexion, int empresa) throws SQLException {
        return Sql.listar(conexion, "SELECT v.*,r.codigo,l.id_sede FROM visita v JOIN reserva r USING(id_reserva)" +
                        " JOIN salon l ON l.id_salon=r.id_salon JOIN sede s ON s.id_sede=l.id_sede" +
                        " WHERE s.id_empresa=? AND v.estado='FINALIZADA' ORDER BY v.hora_real_inicio," + "v.id_visita",
                fila -> new PorSede<>(fila.getInt("id_sede"),
                        new VisitaResumen(fila.getInt("id_visita"), fila.getString("codigo"),
                                fila.getTimestamp("hora_real_inicio").toLocalDateTime(),
                                fila.getTimestamp("hora_real_fin").toLocalDateTime(), fila.getString("notas"),
                                promedio(fila.getBigDecimal("calificacion_final")))), empresa);
    }

    /**
     * Cada puesto aparece una vez: máximo de los análisis de su última evaluación Finalizada (Andy, 11 nov 2026).
     * RNF-011.
     */
    public List<PorSede<PuestoResumen>> puestos(Connection conexion, int empresa) throws SQLException {
        String sql = "SELECT p.id_puesto,p.numero,p.descripcion,l.nombre salon,s.id_sede,ar.nombre" +
                " area,ca.nombre cargo,ca.tipo tipo_cargo," +
                "e.id_evaluacion,e.id_visita,e.postura,e.motivo,e.fecha,cp.id_evaluacion cond" + "iciones_id,cp.*," +
                "(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id" +
                "_foto) WHERE f.id_evaluacion=e.id_evaluacion) puntaje" +
                " FROM evaluacion e JOIN visita v ON v.id_visita=e.id_visita" + LUGAR +
                " LEFT JOIN area ar ON ar.id_area=p.id_area LEFT JOIN cargo ca ON ca.id_cargo" + "=p.id_cargo" +
                " LEFT JOIN condiciones_puesto cp ON cp.id_evaluacion=e.id_evaluacion" +
                " WHERE s.id_empresa=? AND v.estado='FINALIZADA' AND e.id_evaluacion=(SELECT " +
                "MAX(e2.id_evaluacion)" +
                " FROM evaluacion e2 JOIN visita v2 ON v2.id_visita=e2.id_visita WHERE e2.id_" +
                "puesto=p.id_puesto AND v2.estado='FINALIZADA')" + " ORDER BY s.nombre,l.nombre,p.numero,p.id_puesto";
        return Sql.listar(conexion, sql, fila -> new PorSede<>(fila.getInt("id_sede"),
                new PuestoResumen(fila.getInt("id_puesto"), fila.getInt("id_evaluacion"), fila.getInt("id_visita"),
                        fila.getInt("numero"), fila.getString("salon"), fila.getString("area"), fila.getString("cargo"),
                        fila.getString("tipo_cargo"), fila.getString("descripcion"), fila.getString("postura"),
                        fila.getString("motivo"), condiciones(fila), fila.getDate("fecha").toLocalDate(),
                        Sql.enteroOpcional(fila, "puntaje"), null)), empresa);
    }

    /**
     * Cuenta cada fotografía analizada una sola vez, aunque tenga varios análisis o lados. RNF-011.
     */
    public Map<Integer, Integer> fotosAnalizadas(Connection conexion, int empresa) throws SQLException {
        var filas = Sql.listar(conexion,
                "SELECT s.id_sede,COUNT(*) cantidad FROM fotografia f JOIN evaluacion e USING" + "(id_evaluacion)" +
                        " JOIN visita v ON v.id_visita=e.id_visita" + LUGAR +
                        " WHERE s.id_empresa=? AND v.estado='FINALIZADA'" +
                        " AND EXISTS(SELECT 1 FROM analisis_rula a WHERE a.id_foto=f.id_foto) GROUP B" + "Y s.id_sede",
                fila -> Map.entry(fila.getInt(1), fila.getInt(2)), empresa);
        Map<Integer, Integer> total = new HashMap<>();
        filas.forEach(parametros -> total.put(parametros.getKey(), parametros.getValue()));
        return Map.copyOf(total);
    }

    /**
     * Lee todos los pendientes Finalizados, abiertos y arreglados, sin sustituir su estado por el último color.
     * RNF-011.
     */
    public List<PorSede<PendienteResumen>> pendientes(Connection conexion, int empresa) throws SQLException {
        return Sql.listar(conexion, "SELECT pa.*,p.numero,l.nombre salon,s.id_sede FROM pendiente_arreglo pa" +
                " JOIN evaluacion e ON e.id_evaluacion=pa.id_evaluacion JOIN visita v ON v.id" + "_visita=e.id_visita" +
                LUGAR + " WHERE s.id_empresa=? AND v.estado='FINALIZADA' ORDER BY pa.fecha_registro,p" +
                "a.id_pendiente", fila -> new PorSede<>(fila.getInt("id_sede"),
                new PendienteResumen(fila.getInt("id_pendiente"), fila.getInt("id_evaluacion"), fila.getInt("numero"),
                        fila.getString("salon"), fila.getString("descripcion"), fila.getString("prioridad"),
                        fila.getString("estado"), fila.getDate("fecha_registro").toLocalDate(),
                        fecha(fila, "fecha_limite"), fila.getInt("puntaje"), null)), empresa);
    }

    /**
     * Lee únicamente sugerencias activas y conserva su clasificación y estado en el historial Finalizado. RNF-011.
     */
    public List<PorSede<SugerenciaResumen>> sugerencias(Connection conexion, int empresa) throws SQLException {
        return Sql.listar(conexion, "SELECT sg.*,p.numero,l.nombre salon,s.id_sede FROM sugerencia sg" +
                " JOIN evaluacion e ON e.id_evaluacion=sg.id_evaluacion JOIN visita v ON v.id" + "_visita=e.id_visita" +
                LUGAR + " WHERE s.id_empresa=? AND v.estado='FINALIZADA' AND sg.activo=TRUE ORDER BY " +
                "sg.id_sugerencia", fila -> new PorSede<>(fila.getInt("id_sede"),
                new SugerenciaResumen(fila.getInt("id_sugerencia"), fila.getInt("id_evaluacion"), fila.getInt("numero"),
                        fila.getString("salon"), fila.getString("titulo"), fila.getString("descripcion"),
                        fila.getString("tipo"), fila.getString("estado"), fila.getString("prioridad"),
                        fila.getString("origen"), fecha(fila, "fecha_limite"))), empresa);
    }

    /**
     * Obtiene rangos, nivel escrito y acciones del semáforo aprobado en MySQL. RNF-011.
     */
    public List<Riesgo> riesgos(Connection conexion) throws SQLException {
        return Sql.listar(conexion, "SELECT * FROM nivel_riesgo ORDER BY puntaje_minimo",
                fila -> new Riesgo(fila.getInt("puntaje_minimo"), fila.getInt("puntaje_maximo"),
                        fila.getString("color"), fila.getInt("nivel_oficial_rula"), fila.getString("nombre"),
                        fila.getString("accion")));
    }

    private static Calificacion promedio(BigDecimal elemento) {
        return elemento == null ? null : new Calificacion(elemento, 0, null);
    }

    private static LocalDate fecha(ResultSet fila, String nombre) throws SQLException {
        var elemento = fila.getDate(nombre);
        return elemento == null ? null : elemento.toLocalDate();
    }

    private static String condiciones(ResultSet fila) throws SQLException {
        if (fila.getObject("condiciones_id") == null) return "Sin condiciones registradas.";
        String[][] campos = {{"escritorio_muy_alto", "Escritorio muy alto"}, {"pantalla_muy_alta", "Pantalla muy " +
                "alta"}, {"pies_colgando", "Pies colgando"}, {"tiene_apoyapies", "Apoyapiés"}, {
            "silla_con_apoyabrazos", "Silla con apoyabrazos"}, {"silla_regulable", "Silla regulable"}};
        List<String> textos = new ArrayList<>();
        for (var campo : campos) textos.add(campo[1] + ": " + (fila.getBoolean(campo[0]) ? "sí" : "no"));
        String otras = fila.getString("otras_observaciones");
        if (otras != null && !otras.isBlank()) textos.add("Otras observaciones: " + otras);
        return String.join(" · ", textos);
    }
}
