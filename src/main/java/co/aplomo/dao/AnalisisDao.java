package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.rula.MotorRula;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * RF-021, RNF-005/009: análisis nuevos y mediciones independientes; nunca se reemplaza el historial.
 */
public final class AnalisisDao {

    /**
     * Resumen de un análisis, sin ruta de archivo. RNF-011.
     */
    public record Resumen(int id, int foto, LocalDateTime fecha, String lado, int puntaje, String color, int nivel,
                          String responsable) {
    }

    /**
     * Crea el acceso a análisis. RNF-011.
     */
    public AnalisisDao() {
    }

    /**
     * Guarda solo el resultado recién calculado por MotorRula en el servidor. RNF-011.
     */
    public int guardar(Connection conexion, int foto, LocalDateTime fecha,
            MotorRula.Calculo calculo) throws SQLException {
        var elemento = calculo.entrada();
        var elemento2 = elemento.ajustes();
        var elemento3 = calculo.resultado();
        int id = Sql.insertar(conexion, "INSERT INTO analisis_rula (id_foto,id_nivel,fecha_hora,lado,hombro_elevado,b" +
                        "razo_separado,brazo_apoyado,antebrazo_cruza_linea_media,muneca_desviada,giro" +
                        "_muneca,cuello_girado,cuello_inclinado,tronco_girado,tronco_inclinado,pierna" +
                        "s_apoyadas,postura_estatica_o_repetitiva,carga,puntaje_a,puntaje_b,puntaje_f" +
                        "inal) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", foto, elemento3.nivel().idNivel(),
                fecha,
                elemento.lado().name(), elemento2.hombroElevado(), elemento2.brazoSeparado(), elemento2.brazoApoyado(),
                elemento2.antebrazoCruza(), elemento2.munecaDesviada(), elemento2.giro().name(),
                elemento2.cuelloGirado(), elemento2.cuelloInclinado(), elemento2.troncoGirado(),
                elemento2.troncoInclinado(), elemento2.piernasApoyadas(), elemento.actividad(), elemento.carga().name(),
                elemento3.puntajeA(), elemento3.puntajeB(), elemento3.puntajeFinal());
        for (var elemento4 : calculo.mediciones())
            Sql.insertar(conexion, "INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_gra" +
                            "dos,puntos,corregido_a_mano) VALUES (?,?,?,?,?,?)", id, elemento4.segmento().name(),
                    elemento4.puntosMarcados(), elemento4.grados(), elemento4.puntos(), elemento4.manual());
        return id;
    }

    /**
     * Actualiza el puesto con el máximo de su última evaluación; una evaluación vieja no pisa la nueva. RNF-011.
     */
    public void actualizarPuesto(Connection conexion, int puesto) throws SQLException {
        Sql.actualizar(conexion, "UPDATE puesto_trabajo SET ultimo_puntaje=(SELECT MAX(a.puntaje_final) FROM f" +
                        "otografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=(SELEC" +
                        "T MAX(e.id_evaluacion) FROM evaluacion e WHERE e.id_puesto=?)) WHERE id_pues" + "to=?", puesto,
                puesto);
    }

    /**
     * Lista el historial de cada foto con fecha y responsable, solo para una consulta autorizada. RNF-011.
     */
    public List<Resumen> listar(Connection conexion, int evaluacion) throws SQLException {
        return Sql.listar(conexion, "SELECT a.*,n.color,n.nivel_oficial_rula,(SELECT q.nombre FROM bitacora b JOI" +
                        "N cuenta q ON q.id_cuenta=b.id_cuenta WHERE b.tabla='analisis_rula' AND b.id" +
                        "_registro=a.id_analisis AND b.accion='CREAR' ORDER BY b.id_bitacora LIMIT 1)" +
                        " responsable FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN nive" +
                        "l_riesgo n USING(id_nivel) WHERE f.id_evaluacion=? ORDER BY a.id_analisis DE" + "SC",
                fila -> new Resumen(fila.getInt("id_analisis"), fila.getInt("id_foto"),
                        fila.getTimestamp("fecha_hora").toLocalDateTime(), fila.getString("lado"),
                        fila.getInt("puntaje_final"), fila.getString("color"), fila.getInt("nivel_oficial_rula"),
                        fila.getString("responsable")), evaluacion);
    }

    /**
     * Datos históricos de un análisis y su relación con la evaluación. RNF-011.
     */
    public record Ficha(int id, int evaluacion, int foto, LocalDateTime fecha, String responsable,
                        MotorRula.Entrada entrada, int puntajeA, int puntajeB, int fin) {
    }

    /**
     * Lee entradas y totales históricos sin aceptar valores del navegador. RNF-011.
     */
    public Ficha consultar(Connection conexion, int id) throws SQLException {
        return Sql.uno(conexion, "SELECT a.*,f.id_evaluacion,(SELECT q.nombre FROM bitacora b JOIN cuenta q ON" +
                        " q.id_cuenta=b.id_cuenta WHERE b.tabla='analisis_rula' AND b.id_registro=a.i" +
                        "d_analisis AND b.accion='CREAR' ORDER BY b.id_bitacora LIMIT 1) responsable " +
                        "FROM analisis_rula a JOIN fotografia f USING(id_foto) WHERE a.id_analisis=?",
                fila -> new Ficha(fila.getInt("id_analisis"), fila.getInt("id_evaluacion"), fila.getInt("id_foto"),
                        fila.getTimestamp("fecha_hora").toLocalDateTime(), fila.getString("responsable"),
                        new MotorRula.Entrada(co.aplomo.rula.MedidorAngulos.Lado.valueOf(fila.getString("lado")), 1, 1,
                                Map.of(),
                                new co.aplomo.rula.PuntuadorSegmentos.Ajustes(fila.getBoolean("hombro_elevado"),
                                        fila.getBoolean("brazo_separado"), fila.getBoolean("brazo_apoyado"),
                                        fila.getBoolean("antebrazo_cruza_linea_media"),
                                        fila.getBoolean("muneca_desviada"),
                                        co.aplomo.rula.PuntuadorSegmentos.Giro.valueOf(fila.getString("giro_muneca")),
                                        fila.getBoolean("cuello_girado"), fila.getBoolean("cuello_inclinado"),
                                        fila.getBoolean("tronco_girado"), fila.getBoolean("tronco_inclinado"),
                                        fila.getBoolean("piernas_apoyadas"), false),
                                fila.getBoolean("postura_estatica_o_repetitiva"),
                                MotorRula.Carga.valueOf(fila.getString("carga"))), fila.getInt("puntaje_a"),
                        fila.getInt("puntaje_b"), fila.getInt("puntaje_final")), id);
    }

    /**
     * Lee mediciones guardadas para presentar el análisis sin recalcular su historia. RNF-011.
     */
    public List<MotorRula.Medida> mediciones(Connection conexion, int id) throws SQLException {
        return Sql.listar(conexion, "SELECT * FROM medicion_angulo WHERE id_analisis=? ORDER BY id_medicion",
                fila -> new MotorRula.Medida(MotorRula.Segmento.valueOf(fila.getString("segmento")),
                        fila.getString("puntos_marcados"), fila.getDouble("angulo_grados"), fila.getInt("puntos"),
                        fila.getBoolean("corregido_a_mano")), id);
    }
}
