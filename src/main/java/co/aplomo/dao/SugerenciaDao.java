package co.aplomo.dao;

import co.aplomo.seguridad.AccesoDatos.Alcance;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Sugerencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-023a/b, RF-002: reglas y sugerencias parametrizadas, sin eliminar registros ni exponer fotos.
 */
public final class SugerenciaDao {

    /**
     * Contexto autorizado de una evaluación, sin rutas de fotografías. RNF-011.
     */
    public record Contexto(int evaluacion, int puesto, int numero, int visita, int empresa, int sede, int salon,
                           String empresaNombre, String sedeNombre, String salonNombre, String postura, Integer puntaje,
                           String color, int nivel) {
    }

    /**
     * Sugerencia acompañada de su puesto y empresa para la consulta. RNF-011.
     */
    public record Ficha(Sugerencia sugerencia, Contexto contexto) {
    }

    /**
     * Regla configurable en la tabla aprobada; su condición se interpreta por lista blanca. RNF-011.
     */
    public record Regla(int id, String segmento, int minimo, String condicion, String texto) {
    }

    private static final String CONTEXTO =
            "SELECT e.id_evaluacion,e.id_puesto,e.id_visita,e.postura,p.numero,s.id_empre" +
                    "sa,l.id_sede,l.id_salon,em.nombre empresa_nombre,s.nombre sede_nombre,l.nomb" +
                    "re salon_nombre,v.estado visita_estado,r.puntaje,n.color,n.nivel_oficial_rul" +
                    "a FROM evaluacion e JOIN puesto_trabajo p USING(id_puesto) JOIN visita v USI" +
                    "NG(id_visita) JOIN salon l ON l.id_salon=p.id_salon JOIN sede s USING(id_sed" +
                    "e) JOIN empresa em USING(id_empresa) LEFT JOIN (SELECT f.id_evaluacion,MAX(a" +
                    ".puntaje_final) puntaje FROM fotografia f JOIN analisis_rula a USING(id_foto" +
                    ") GROUP BY f.id_evaluacion) r ON r.id_evaluacion=e.id_evaluacion LEFT JOIN n" +
                    "ivel_riesgo n ON r.puntaje BETWEEN n.puntaje_minimo AND n.puntaje_maximo";

    /**
     * Crea el acceso a sugerencias con JDBC (RNF-011).
     */
    public SugerenciaDao() {
    }

    /**
     * Consulta el alcance y, al cambiar, bloquea primero la evaluación para serializar sus sugerencias. RNF-011.
     */
    public Contexto contexto(Connection conexion, int id, int empresa, boolean finalizadas,
            boolean bloquear) throws SQLException {
        return contexto(conexion, id, Alcance.exacta(empresa), finalizadas, bloquear);
    }

    /**
     * RNF-011: operación del componente.
     */
    public Contexto contexto(Connection conexion, int id, Alcance empresa, boolean finalizadas,
            boolean bloquear) throws SQLException {
        if (bloquear) Sql.uno(conexion, "SELECT id_evaluacion FROM evaluacion WHERE id_evaluacion=? FOR UPDATE",
                fila -> fila.getInt(1), id);
        var contexto = Sql.uno(conexion, CONTEXTO + " WHERE e.id_evaluacion=? AND " + empresa.sql("s.id_empresa") +
                        " AND (?=FALSE OR v.estado='FINALIZADA')", SugerenciaDao::contexto, id, empresa.todas(),
                empresa.valor(), finalizadas);
        if (!bloquear || contexto == null) return contexto;
        var resultado = Sql.uno(conexion,
                "SELECT a.puntaje_final,a.id_nivel FROM analisis_rula a WHERE a.id_foto IN (S" +
                        "ELECT id_foto FROM fotografia WHERE id_evaluacion=?) ORDER BY a.puntaje_fina" +
                        "l DESC,a.id_analisis DESC LIMIT 1 FOR UPDATE",
                fila2 -> new int[]{fila2.getInt(1), fila2.getInt(2)}, id);
        if (resultado == null) return contexto;
        var nivel = Sql.uno(conexion, "SELECT color,nivel_oficial_rula FROM nivel_riesgo WHERE id_nivel=?",
                fila3 -> new String[]{fila3.getString(1), fila3.getString(2)}, resultado[1]);
        var sugerencia = contexto;
        return new Contexto(sugerencia.evaluacion(), sugerencia.puesto(), sugerencia.numero(), sugerencia.visita(),
                sugerencia.empresa(), sugerencia.sede(), sugerencia.salon(), sugerencia.empresaNombre(),
                sugerencia.sedeNombre(), sugerencia.salonNombre(), sugerencia.postura(), resultado[0], nivel[0],
                Integer.parseInt(nivel[1]));
    }

    /**
     * Lista solo sugerencias activas dentro del alcance; evaluación cero significa todas. RNF-011.
     */
    public List<Ficha> listar(Connection conexion, int empresa, boolean finalizadas,
            int evaluacion) throws SQLException {
        return listar(conexion, Alcance.exacta(empresa), finalizadas, evaluacion);
    }

    /**
     * RF-002: consulta parametrizada en el alcance vigente.
     */
    public List<Ficha> listar(Connection conexion, Alcance empresa, boolean finalizadas,
            int evaluacion) throws SQLException {
        var contextos = Sql.listar(conexion, CONTEXTO + (evaluacion == 0 ?
                        " WHERE TRUE" + co.aplomo.modelo.Demostracion.filtro("s.id_empresa") + " AND" : " WHERE") +
                        " " +
                        empresa.sql("s.id_empresa") +
                        " AND (?=FALSE OR v.estado='FINALIZADA') AND (?=0 OR e.id_evaluacion=?) ORDER" +
                        " BY e.id_evaluacion DESC", SugerenciaDao::contexto, empresa.todas(), empresa.valor(),
                finalizadas,
                evaluacion, evaluacion);
        List<Ficha> salida = new ArrayList<>();
        for (var sugerencia : contextos)
            for (var sugerencia2 : porEvaluacion(conexion, sugerencia.evaluacion(), true))
                salida.add(new Ficha(sugerencia2, sugerencia));
        return List.copyOf(salida);
    }

    /**
     * Lee una fila; el servicio comprueba después su empresa y visita. RNF-011.
     */
    public Sugerencia buscar(Connection conexion, int id) throws SQLException {
        return Sql.uno(conexion, "SELECT * FROM sugerencia WHERE id_sugerencia=?", SugerenciaDao::leer, id);
    }

    /**
     * Lee la fila vigente bajo el bloqueo previo de la evaluación. RNF-011.
     */
    public Sugerencia buscarActual(Connection conexion, int id) throws SQLException {
        return Sql.uno(conexion, "SELECT * FROM sugerencia WHERE id_sugerencia=? FOR UPDATE", SugerenciaDao::leer, id);
    }

    /**
     * Lee sugerencias vigentes incluso si se esperó a otra operación concurrente. RNF-011.
     */
    public List<Sugerencia> actuales(Connection conexion, int evaluacion) throws SQLException {
        return Sql.listar(conexion, "SELECT * FROM sugerencia WHERE id_evaluacion=? ORDER BY id_sugerencia FOR UPDATE",
                SugerenciaDao::leer, evaluacion);
    }

    /**
     * Conserva también las filas quitadas al comprobar si una regla ya fue propuesta. RNF-011.
     */
    public List<Sugerencia> porEvaluacion(Connection conexion, int id, boolean activas) throws SQLException {
        return Sql.listar(conexion, "SELECT * FROM sugerencia WHERE id_evaluacion=? AND (?=FALSE OR activo=TRUE) " +
                "ORDER BY id_sugerencia", SugerenciaDao::leer, id, activas);
    }

    /**
     * Lee reglas configuradas, sin constantes de umbrales en Java. RNF-011.
     */
    public List<Regla> reglas(Connection conexion) throws SQLException {
        return Sql.listar(conexion, "SELECT * FROM regla_sugerencia ORDER BY id_regla",
                fila -> new Regla(fila.getInt("id_regla"), fila.getString("segmento"), fila.getInt("puntaje_minimo"),
                        fila.getString("condicion"), fila.getString("texto_sugerido")));
    }

    /**
     * Inserta una sugerencia del sistema o del Administrador. RNF-011.
     */
    public int crear(Connection conexion, Sugerencia sugerencia) throws SQLException {
        return Sql.insertar(conexion, "INSERT INTO sugerencia (id_evaluacion,id_regla,titulo,descripcion,tipo,prior" +
                        "idad,estado,fecha_limite,origen) VALUES (?,?,?,?,?,?,?,?,?)", sugerencia.evaluacion(),
                sugerencia.regla(), sugerencia.titulo(), sugerencia.descripcion(), sugerencia.tipo(),
                sugerencia.prioridad(), sugerencia.estado(), sugerencia.limite(), sugerencia.origen());
    }

    /**
     * Edita únicamente los campos permitidos; conserva origen, regla y estado. RNF-011.
     */
    public void editar(Connection conexion, Sugerencia sugerencia) throws SQLException {
        Sql.actualizar(conexion, "UPDATE sugerencia SET titulo=?,descripcion=?,tipo=?,prioridad=?,fecha_limite" +
                        "=? WHERE id_sugerencia=?", sugerencia.titulo(), sugerencia.descripcion(), sugerencia.tipo(),
                sugerencia.prioridad(), sugerencia.limite(), sugerencia.id());
    }

    /**
     * Cambia el estado sin sobrescribir la descripción ni el origen. RNF-011.
     */
    public void estado(Connection conexion, int id, String estado) throws SQLException {
        Sql.actualizar(conexion, "UPDATE sugerencia SET estado=? WHERE id_sugerencia=?", estado, id);
    }

    /**
     * Quita de las listas sin borrar la fila ni su relación con la regla. RNF-011.
     */
    public void quitar(Connection conexion, int id) throws SQLException {
        Sql.actualizar(conexion, "UPDATE sugerencia SET activo=FALSE WHERE id_sugerencia=?", id);
    }

    /**
     * Comprueba todas las sugerencias activas del puesto para ofrecer su Reevaluación. RNF-011.
     */
    public boolean todasHechas(Connection conexion, int puesto) throws SQLException {
        return Boolean.TRUE.equals(Sql.uno(conexion,
                "SELECT COUNT(*)>0 AND SUM(s.estado<>'HECHA')=0 FROM sugerencia s JOIN evalua" +
                        "cion e USING(id_evaluacion) WHERE e.id_puesto=? AND s.activo=TRUE", fila -> fila.getBoolean(1),
                puesto));
    }

    /** RF-023/RNF-005: identidad estable de mejoras sin regla, aunque luego cambien título o descripción. */
    public java.util.Map<Integer, String> marcasPlan(Connection con, int evaluacion) throws SQLException {
        var filas = Sql.listar(con, "SELECT b.id_registro,b.detalle FROM bitacora b JOIN sugerencia s " +
                "ON s.id_sugerencia=b.id_registro WHERE b.tabla='sugerencia' AND b.accion='CREAR' " +
                "AND s.id_evaluacion=? AND s.origen='SISTEMA' AND s.id_regla IS NULL " +
                "AND b.detalle LIKE 'Plan de atención · %' ORDER BY b.id_bitacora",
                f -> java.util.Map.entry(f.getInt(1), f.getString(2)), evaluacion);
        java.util.Map<Integer, String> marcas = new java.util.HashMap<>();
        filas.forEach(f -> marcas.putIfAbsent(f.getKey(), f.getValue()));
        return java.util.Map.copyOf(marcas);
    }

    private static Contexto contexto(ResultSet fila) throws SQLException {
        return new Contexto(fila.getInt("id_evaluacion"), fila.getInt("id_puesto"), fila.getInt("numero"),
                fila.getInt("id_visita"), fila.getInt("id_empresa"), fila.getInt("id_sede"), fila.getInt("id_salon"),
                fila.getString("empresa_nombre"), fila.getString("sede_nombre"), fila.getString("salon_nombre"),
                fila.getString("postura"), Sql.enteroOpcional(fila, "puntaje"), fila.getString("color"),
                fila.getInt("nivel_oficial_rula"));
    }

    private static Sugerencia leer(ResultSet fila) throws SQLException {
        var elemento = fila.getDate("fecha_limite");
        return new Sugerencia(fila.getInt("id_sugerencia"), fila.getInt("id_evaluacion"),
                Sql.enteroOpcional(fila, "id_regla"), fila.getString("titulo"), fila.getString("descripcion"),
                fila.getString("tipo"), fila.getString("prioridad"), fila.getString("estado"),
                elemento == null ? null : elemento.toLocalDate(), fila.getString("origen"), fila.getBoolean("activo"));
    }
}
