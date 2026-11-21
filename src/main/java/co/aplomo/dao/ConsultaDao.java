package co.aplomo.dao;

import co.aplomo.seguridad.AccesoDatos.Alcance;
import co.aplomo.db.Sql;
import co.aplomo.modelo.SeccionConsulta;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-024, RF-002: solo SELECT parametrizados; la empresa y las visitas visibles se limitan antes de filtrar.
 */
public final class ConsultaDao {

    /**
     * Fila de consulta sin fotos, rutas de archivos ni credenciales. RNF-011.
     */
    public record Fila(Map<String, String> datos) {

        /**
         * Lee una columna; los nulos de MySQL se muestran sin inventar resultados. RNF-011.
         */
        public String valor(String clave) {
            return datos.getOrDefault(clave, "");
        }

        /**
         * Identificador del registro ya autorizado. RNF-011.
         */
        public int id() {
            return Integer.parseInt(valor("id"));
        }
    }

    /**
     * Opción autorizada de un catálogo, con sus relaciones para mostrar el contexto. RNF-011.
     */
    public record Opcion(int id, int empresa, int sede, String nombre) {
    }

    private static final String LUGAR = " JOIN salon l ON l.id_salon=p.id_salon JOIN sede s ON s.id_sede=l.id_sede JO" +
            "IN empresa em ON em.id_empresa=s.id_empresa LEFT JOIN area ar ON ar.id_area=" +
            "p.id_area LEFT JOIN cargo ca ON ca.id_cargo=p.id_cargo ";
    private static final String DATOS_LUGAR =
            "s.id_empresa empresa,l.id_sede sede,l.id_salon salon,p.id_area area,p.id_car" +
                    "go cargo,p.id_puesto puesto,p.numero,p.descripcion puesto_descripcion,em.activo empresa_activa,s" +
                    ".activo sede_" + "activa,l.activo salon_activo,p.activo puesto_activo,em.nombre empresa_nombre" +
                    ",s.nombre sede_nombre,l.nombre salon_nombre,ar.nombre area_nombre,ca.nombre " + "cargo_nombre,";
    private static final String MAXIMO =
            "(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a ON a.id_" +
                    "foto=f.id_foto WHERE f.id_evaluacion=e.id_evaluacion)";

    /**
     * Crea el acceso de consulta (RNF-011).
     */
    public ConsultaDao() {
    }

    /**
     * Lee una sección dentro del alcance; id cero consulta la lista completa autorizada. RNF-011.
     */
    public List<Fila> filas(Connection conexion, SeccionConsulta seccion, int empresa, boolean usuario,
            int id) throws SQLException {
        return filas(conexion, seccion, Alcance.exacta(empresa), usuario, id);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public List<Fila> filas(Connection conexion, SeccionConsulta seccion, Alcance empresa, boolean usuario,
            int id) throws SQLException {
        return filas(conexion, seccion, empresa, usuario, id, 0);
    }

    /**
     * RNF-009, RF-028/024: historial completo del puesto, llamado después del permiso administrativo.
     */
    public List<Fila> historialPuesto(Connection conexion, int puesto) throws SQLException {
        return filas(conexion, SeccionConsulta.EVALUACIONES, 0, false, 0, puesto);
    }

    private List<Fila> filas(Connection conexion, SeccionConsulta seccion, int empresa, boolean usuario, int id,
            int puesto) throws SQLException {
        return filas(conexion, seccion, Alcance.exacta(empresa), usuario, id, puesto);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    private List<Fila> filas(Connection conexion, SeccionConsulta seccion, Alcance empresa, boolean usuario, int id,
            int puesto) throws SQLException {
        return filas(conexion, seccion, empresa, usuario, id, puesto, 0);
    }

    /**
     * RNF-005/009: consulta propia aun si se retiró una asignación, sin práctica.
     */
    public List<Fila> propias(Connection conexion, int cuenta) throws SQLException {
        return filas(conexion, SeccionConsulta.EVALUACIONES, Alcance.exacta(0), false, 0, 0, cuenta);
    }

    private List<Fila> filas(Connection conexion, SeccionConsulta seccion, Alcance empresa, boolean usuario, int id,
            int puesto, int autor) throws SQLException {
        String select, desde, extra = "";
        List<Object> params = new ArrayList<>();
        switch (seccion) {
            case EMPRESAS -> {
                select = "em.id_empresa id,em.id_empresa empresa,em.nombre nombre,em.nombre empresa_no" +
                        "mbre,em.nit,em.descripcion," + activo("em") + " estado,NULL puntaje";
                desde = "empresa em";
            }
            case AREAS -> {
                select = "ar.id_area id,ar.id_area area,em.id_empresa empresa,ar.nombre nombre,em.nomb" +
                        "re empresa_nombre," + activo("ar") + " estado,NULL puntaje";
                desde = "area ar JOIN empresa em ON em.id_empresa=ar.id_empresa";
            }
            case CARGOS -> {
                select = "ca.id_cargo id,ca.id_cargo cargo,em.id_empresa empresa,ca.nombre nombre,ca.t" +
                        "ipo,em.nombre empresa_nombre," + activo("ca") + " estado,NULL puntaje";
                desde = "cargo ca JOIN empresa em ON em.id_empresa=ca.id_empresa";
            }
            case SEDES -> {
                select = "s.id_sede id,s.id_sede sede,em.id_empresa empresa,s.nombre nombre,s.ciudad,s" +
                        ".direccion,em.nombre empresa_nombre," + activo("s") + " estado,NULL puntaje";
                desde = "sede s JOIN empresa em ON em.id_empresa=s.id_empresa";
            }
            case SALONES -> {
                select = "l.id_salon id,l.id_salon salon,l.id_sede sede,em.id_empresa empresa,l.nombre" +
                        " nombre,l.piso,l.capacidad_puestos capacidad,l.descripcion,em.nombre empresa" +
                        "_nombre,s.nombre sede_nombre," + activo("l") + " estado,NULL puntaje";
                desde = "salon l JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_empresa=" + "s.id_empresa";
            }
            case PUESTOS -> {
                select = "p.id_puesto id," + DATOS_LUGAR + "p.descripcion nombre,p.descripcion,p.ubicacion,p.tipo," +
                        activo("p") + " estado,(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a " +
                        "ON a.id_foto=f.id_foto WHERE f.id_evaluacion=(SELECT MAX(e.id_evaluacion) FR" +
                        "OM evaluacion e JOIN visita v ON v.id_visita=e.id_visita WHERE e.id_puesto=p" +
                        ".id_puesto AND (?=FALSE OR v.estado='FINALIZADA'))) puntaje";
                desde = "puesto_trabajo p" + LUGAR;
                params.add(usuario);
            }
            case RESERVAS -> {
                select = "r.id_reserva id,s.id_empresa empresa,l.id_sede sede,l.id_salon salon,r.codig" +
                        "o,r.fecha,r.hora_inicio inicio,r.hora_fin fin,r.categoria tipo,r.descripcion" +
                        ",r.notas,r.motivo_cancelacion motivo,r.estado,em.nombre empresa_nombre,s.nom" +
                        "bre sede_nombre,l.nombre salon_nombre,NULL puntaje";
                select += ",COALESCE((SELECT vx.estado FROM visita vx WHERE vx.id_reserva=r.id_reserva LIMIT 1), '') " +
                        "visita_estado";
                desde = "reserva r JOIN salon l ON l.id_salon=r.id_salon JOIN sede s ON s.id_sede=l.i" +
                        "d_sede JOIN empresa em ON em.id_empresa=s.id_empresa";
            }
            case VISITAS -> {
                select = "v.id_visita id,s.id_empresa empresa,l.id_sede sede,l.id_salon salon,r.codigo" +
                        ",v.hora_real_inicio fecha,v.hora_real_fin fin,v.notas,v.estado,v.calificacio" +
                        "n_final calificacion,em.activo empresa_activa,s.activo sede_activa,l.activo " +
                        "salon_activo,em.nombre empresa_nombre,s.nombre sede_nombre,l.nombre salon_no" +
                        "mbre,(SELECT MAX(ar.puntaje_final) FROM evaluacion ev JOIN fotografia ft USING" +
                        "(id_evaluacion) " +
                        "JOIN analisis_rula ar USING(id_foto) WHERE ev.id_visita=v.id_visita) puntaje";
                desde = "visita v JOIN reserva r ON r.id_reserva=v.id_reserva JOIN salon l ON l.id_sa" +
                        "lon=r.id_salon JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON em.id_e" +
                        "mpresa=s.id_empresa";
                extra = " AND (?=FALSE OR v.estado='FINALIZADA')";
            }
            case EVALUACIONES -> {
                select = "e.id_evaluacion id,e.id_evaluacion evaluacion,e.id_visita visita,v.id_reserv" +
                        "a reserva,r.codigo,COALESCE((SELECT b.id_cuenta FROM bitacora b WHERE b.tabl" +
                        "a='evaluacion' AND b.accion='CREAR' AND b.id_registro=e.id_evaluacion ORDER " +
                        "BY b.id_bitacora LIMIT 1),0) evaluador,COALESCE((SELECT q.nombre FROM bitaco" +
                        "ra b JOIN cuenta q USING(id_cuenta) WHERE b.tabla='evaluacion' AND b.accion=" +
                        "'CREAR' AND b.id_registro=e.id_evaluacion ORDER BY b.id_bitacora LIMIT 1),'S" +
                        "in registro') evaluador_nombre," + DATOS_LUGAR +
                        "e.fecha,e.postura nombre,e.postura,e.criterio,e.motivo,e.minutos_jornada min" +
                        "utos,v.estado," + MAXIMO + " puntaje";
                desde = "evaluacion e JOIN visita v ON v.id_visita=e.id_visita JOIN reserva r ON r.id" +
                        "_reserva=v.id_reserva JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto" + LUGAR;
                extra = " AND (?=FALSE OR v.estado='FINALIZADA')";
            }
            case PENDIENTES -> {
                select = "pa.id_pendiente id,e.id_evaluacion evaluacion,e.id_visita visita," + DATOS_LUGAR +
                        "pa.descripcion nombre,pa.descripcion,pa.prioridad,pa.fecha_limite,pa.fecha_r" +
                        "egistro fecha,pa.estado,(SELECT MAX(pp.puntaje) FROM pendiente_arreglo pp JOIN evaluacion ep" +
                        " " +
                        "USING(id_evaluacion) WHERE ep.id_visita=e.id_visita AND ep.id_puesto=e.id_puesto) puntaje";
                desde = "pendiente_arreglo pa JOIN evaluacion e ON e.id_evaluacion=pa.id_evaluacion J" +
                        "OIN visita v ON v.id_visita=e.id_visita JOIN puesto_trabajo p ON p.id_puesto" +
                        "=e.id_puesto" + LUGAR;
                extra = " AND (?=FALSE OR v.estado='FINALIZADA') AND NOT EXISTS " +
                        "(SELECT 1 FROM pendiente_arreglo pp JOIN evaluacion ep USING(id_evaluacion) " +
                        "WHERE ep.id_visita=e.id_visita AND ep.id_puesto=e.id_puesto " +
                        "AND pp.id_pendiente<pa.id_pendiente)";
            }
            case SUGERENCIAS -> {
                select = "sg.id_sugerencia id,e.id_evaluacion evaluacion,e.id_visita visita," + DATOS_LUGAR +
                        "sg.titulo nombre,sg.descripcion,sg.tipo,sg.prioridad,sg.origen,sg.estado,sg." +
                        "fecha_limite fecha," + MAXIMO + " puntaje";
                desde = "sugerencia sg JOIN evaluacion e ON e.id_evaluacion=sg.id_evaluacion JOIN vis" +
                        "ita v ON v.id_visita=e.id_visita JOIN puesto_trabajo p ON p.id_puesto=e.id_p" + "uesto" +
                        LUGAR;
                extra = " AND sg.activo=TRUE AND (?=FALSE OR v.estado='FINALIZADA')";
            }
            default -> throw new IllegalArgumentException("Sección no disponible.");
        }
        if (seccion == SeccionConsulta.PENDIENTES || seccion == SeccionConsulta.SUGERENCIAS) {
            desde += " JOIN reserva r ON r.id_reserva=v.id_reserva";
        }
        select += metadatos(seccion, params);
        boolean personal = !empresa.todas() && !empresa.finalizadas() && empresa.cuenta() > 0 &&
                java.util.Set.of(SeccionConsulta.RESERVAS, SeccionConsulta.VISITAS, SeccionConsulta.PENDIENTES)
                        .contains(seccion);
        String privacidadReserva = seccion == SeccionConsulta.RESERVAS && usuario ?
                " AND (r.id_administrador=" + empresa.cuenta() +
                " OR EXISTS(SELECT 1 FROM cuenta autor WHERE autor.id_cuenta=r.id_administrador " +
                "AND autor.rol<>'USUARIO'))" : "";
        String sql =
                "SELECT q.*,n.color,n.nivel_oficial_rula nivel FROM (SELECT " + select + " FROM " + desde + " WHERE " +
                        empresa.sql("em.id_empresa") + privacidadReserva +
                        (puesto > 0 ? "" : co.aplomo.modelo.Demostracion.filtro("em.id_empresa")) + extra +
                        (personal ? " AND r.id_evaluador=?" : "") + (autor > 0 ?
                        " AND (SELECT b.id_cuenta FROM bitacora b WHERE b.tabla='evaluacion' AND b.ac" +
                        "cion='CREAR' AND b.id_registro=e.id_evaluacion ORDER BY b.id_bitacora LIMIT " + "1)=?" : "") +
                        ") q LEFT JOIN nivel_riesgo n ON q.puntaje BETWEEN n.puntaje_minimo AND n.pun" +
                        "taje_maximo WHERE (?=0 OR q.id=?)" +
                        (puesto > 0 ? " AND q.puesto=? ORDER BY q.fecha DESC,q.id DESC" : "");
        params.add(empresa.todas());
        params.add(empresa.valor());
        if (!extra.isEmpty()) params.add(usuario);
        if (personal) params.add(empresa.cuenta());
        if (autor > 0) params.add(autor);
        params.add(id);
        params.add(id);
        if (puesto > 0) params.add(puesto);
        return Sql.listar(conexion, sql, ConsultaDao::fila, params.toArray());
    }

    private static String metadatos(SeccionConsulta seccion, List<Object> params) {
        String persona = "r.id_evaluador";
        if (seccion == SeccionConsulta.EVALUACIONES) {
            return ",(SELECT q.color_agenda FROM cuenta q WHERE q.id_cuenta=(SELECT b.id_cuenta " +
                    "FROM bitacora b WHERE b.tabla='evaluacion' AND b.accion='CREAR' " +
                    "AND b.id_registro=e.id_evaluacion ORDER BY b.id_bitacora LIMIT 1)) evaluador_color";
        }
        if (!java.util.Set.of(SeccionConsulta.RESERVAS, SeccionConsulta.VISITAS, SeccionConsulta.PENDIENTES,
                SeccionConsulta.SUGERENCIAS).contains(seccion)) return "";
        String extra = "," + persona + " evaluador,(SELECT q.nombre FROM cuenta q WHERE q.id_cuenta=" + persona +
                ") evaluador_nombre,(SELECT q.color_agenda FROM cuenta q WHERE q.id_cuenta=" + persona +
                ") evaluador_color";
        if (seccion == SeccionConsulta.RESERVAS) {
            return extra + ",s.direccion,(SELECT q.rol FROM cuenta q WHERE q.id_cuenta=r.id_administrador) " +
                    "registrador_rol,r.id_administrador registrador";
        }
        if (seccion == SeccionConsulta.VISITAS) {
            return extra + ",(SELECT COUNT(DISTINCT ev.id_puesto) FROM evaluacion ev WHERE ev.id_visita=v.id_visita) " +
                    "puestos_evaluados,(SELECT COUNT(*) FROM puesto_trabajo pt WHERE pt.id_salon=r.id_salon " +
                    "AND pt.activo=TRUE) puestos_total,(SELECT MAX(ar.puntaje_final) FROM evaluacion ev " +
                    "JOIN fotografia ft USING(id_evaluacion) JOIN analisis_rula ar USING(id_foto) " +
                    "WHERE ev.id_visita=v.id_visita) maximo";
        }
        if (seccion == SeccionConsulta.PENDIENTES) {
            params.add(java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")));
            return extra + ",(SELECT COUNT(*) FROM evaluacion ev WHERE ev.id_visita=e.id_visita " +
                    "AND ev.id_puesto=e.id_puesto) posturas,(SELECT MIN(rp.fecha) FROM reserva rp " +
                    "JOIN salon lp USING(id_salon) JOIN sede sp USING(id_sede) WHERE sp.id_empresa=s.id_empresa " +
                    "AND rp.id_evaluador=r.id_evaluador AND rp.fecha>=? " +
                    "AND rp.estado IN ('ESPERANDO','EN_COLA')) proxima_visita";
        }
        return extra;
    }

    /**
     * RF-024/028: incluye inactivos; marca sus nombres solo para Administrador, sin alterar el alcance.
     */
    public Map<String, List<Opcion>> catalogos(Connection conexion, int empresa, boolean admin) throws SQLException {
        return catalogos(conexion, Alcance.exacta(empresa), admin);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public Map<String, List<Opcion>> catalogos(Connection conexion, Alcance empresa,
            boolean admin) throws SQLException {
        Map<String, List<Opcion>> mapa = new LinkedHashMap<>();
        String em = nombreCatalogo("em", true, admin), sede = nombreCatalogo("s", true, admin), salon = nombreCatalogo(
                "l", false, admin);
        mapa.put("empresa", opciones(conexion,
                "SELECT em.id_empresa id,em.id_empresa empresa,0 sede," + em + " nombre FROM empresa em", empresa));
        mapa.put("sede", opciones(conexion,
                "SELECT s.id_sede id,s.id_empresa empresa,s.id_sede sede,CONCAT(" + sede + ",' · '," + em +
                        ") nombre FROM sede s JOIN empresa em ON em.id_empresa=s.id_empresa", empresa));
        mapa.put("salon", opciones(conexion,
                "SELECT l.id_salon id,s.id_empresa empresa,l.id_sede sede,CONCAT(" + salon + ",' · '," + sede +
                        ",' · '," + em +
                        ") nombre FROM salon l JOIN sede s ON s.id_sede=l.id_sede JOIN empresa em ON " +
                        "em.id_empresa=s.id_empresa", empresa));
        mapa.put("puesto", opciones(conexion,
                "SELECT p.id_puesto id,s.id_empresa empresa,l.id_sede sede,CONCAT('Puesto ',p" +
                        ".numero,' · ',p.descripcion," +
                        (admin ? "CASE WHEN p.activo THEN '' ELSE ' (inactivo)' END," : "") + "' · '," + salon +
                        ",' · '," + sede + ",' · '," + em + ") nombre FROM puesto_trabajo p" + LUGAR, empresa));
        mapa.put("area", opciones(conexion,
                "SELECT a.id_area id,a.id_empresa empresa,0 sede,CONCAT(a.nombre,' · '," + em +
                        ") nombre FROM area a JOIN empresa em ON em.id_empresa=a.id_empresa", empresa));
        mapa.put("cargo", opciones(conexion,
                "SELECT a.id_cargo id,a.id_empresa empresa,0 sede,CONCAT(a.nombre,' · '," + em +
                        ") nombre FROM cargo a JOIN empresa em ON em.id_empresa=a.id_empresa", empresa));
        mapa.put("evaluador",
                Sql.listar(conexion, "SELECT id_cuenta,nombre,estado FROM cuenta ORDER BY nombre,id_cuenta",
                        rs -> new Opcion(rs.getInt(1), 0, 0,
                                rs.getString(2) + (rs.getString(3).equals("INACTIVA") ? " (inactivo)" : ""))));
        return Map.copyOf(mapa);
    }

    private static String nombreCatalogo(String alias, boolean femenino, boolean admin) {
        return admin ? "CONCAT(" + alias + ".nombre,CASE WHEN " + alias + ".activo THEN '' ELSE ' (" +
                       (femenino ? "inactiva" : "inactivo") + ")' END)" : alias + ".nombre";
    }

    /**
     * Niveles y colores oficiales para los filtros, desde la misma tabla MySQL. RNF-011.
     */
    public Map<String, List<String>> riesgos(Connection conexion) throws SQLException {
        return Map.of("color",
                Sql.listar(conexion, "SELECT color FROM nivel_riesgo GROUP BY color ORDER BY MIN(puntaje_minimo)",
                        fila -> fila.getString(1)), "nivel",
                Sql.listar(conexion, "SELECT DISTINCT nivel_oficial_rula FROM nivel_riesgo ORDER BY nivel_oficial_rula",
                        fila2 -> fila2.getString(1)));
    }

    private static List<Opcion> opciones(Connection conexion, String sql, int empresa) throws SQLException {
        return opciones(conexion, sql, Alcance.exacta(empresa));
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    private static List<Opcion> opciones(Connection conexion, String sql, Alcance empresa) throws SQLException {
        return Sql.listar(conexion, sql + " WHERE " + empresa.sql("em.id_empresa") + "" +
                        co.aplomo.modelo.Demostracion.filtro("em.id_empresa") + " ORDER BY nombre,id",
                fila -> new Opcion(fila.getInt("id"), fila.getInt("empresa"), fila.getInt("sede"),
                        fila.getString("nombre")), empresa.todas(), empresa.valor());
    }

    private static String activo(String alias) {
        return "CASE WHEN " + alias + ".activo THEN 'ACTIVO' ELSE 'INACTIVO' END";
    }

    private static Fila fila(ResultSet fila) throws SQLException {
        Map<String, String> datos = new LinkedHashMap<>();
        var fila2 = fila.getMetaData();
        for (int i = 1; i <= fila2.getColumnCount(); i++) {
            String valor = fila.getString(i);
            datos.put(fila2.getColumnLabel(i), valor == null ? "" : valor);
        }
        datos.put("registros", List.of("empresa_activa", "sede_activa", "salon_activo", "puesto_activo").stream()
                .anyMatch(k -> "0".equals(datos.get(k))) ? "INACTIVOS" : "ACTIVOS");
        return new Fila(Map.copyOf(datos));
    }
}
