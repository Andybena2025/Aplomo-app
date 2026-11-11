package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.rula.MotorRula;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-021, RNF-005/009: análisis nuevos y mediciones independientes; nunca se reemplaza el historial. */
public final class AnalisisDao {
    /** Resumen de un análisis, sin ruta de archivo. */
    public record Resumen(int id,int foto,LocalDateTime fecha,String lado,int puntaje,String color,int nivel,String responsable) {}
    /** Crea el acceso a análisis. */
    public AnalisisDao() {}
    /** Guarda solo el resultado recién calculado por MotorRula en el servidor. */
    public int guardar(Connection c,int foto,LocalDateTime fecha,MotorRula.Calculo calculo) throws SQLException {
        var e=calculo.entrada();var a=e.ajustes();var r=calculo.resultado();
        int id=Sql.insertar(c,"INSERT INTO analisis_rula (id_foto,id_nivel,fecha_hora,lado,hombro_elevado,brazo_separado,brazo_apoyado,antebrazo_cruza_linea_media,muneca_desviada,giro_muneca,cuello_girado,cuello_inclinado,tronco_girado,tronco_inclinado,piernas_apoyadas,postura_estatica_o_repetitiva,carga,puntaje_a,puntaje_b,puntaje_final) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                foto,r.nivel().idNivel(),fecha,e.lado().name(),a.hombroElevado(),a.brazoSeparado(),a.brazoApoyado(),a.antebrazoCruza(),a.munecaDesviada(),a.giro().name(),a.cuelloGirado(),a.cuelloInclinado(),a.troncoGirado(),a.troncoInclinado(),a.piernasApoyadas(),e.actividad(),e.carga().name(),r.puntajeA(),r.puntajeB(),r.puntajeFinal());
        for(var m:calculo.mediciones()) Sql.insertar(c,"INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano) VALUES (?,?,?,?,?,?)",
                id,m.segmento().name(),m.puntosMarcados(),m.grados(),m.puntos(),m.manual());
        return id;
    }
    /** Actualiza el puesto con el máximo de su última evaluación; una evaluación vieja no pisa la nueva. */
    public void actualizarPuesto(Connection c,int puesto) throws SQLException {
        Sql.actualizar(c,"UPDATE puesto_trabajo SET ultimo_puntaje=(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=(SELECT MAX(e.id_evaluacion) FROM evaluacion e WHERE e.id_puesto=?)) WHERE id_puesto=?",puesto,puesto);
    }
    /** Lista el historial de cada foto con fecha y responsable, solo para una consulta autorizada. */
    public List<Resumen> listar(Connection c,int evaluacion) throws SQLException {
        return Sql.listar(c,"SELECT a.*,n.color,n.nivel_oficial_rula,(SELECT q.nombre FROM bitacora b JOIN cuenta q ON q.id_cuenta=b.id_cuenta WHERE b.tabla='analisis_rula' AND b.id_registro=a.id_analisis AND b.accion='CREAR' ORDER BY b.id_bitacora LIMIT 1) responsable FROM analisis_rula a JOIN fotografia f USING(id_foto) JOIN nivel_riesgo n USING(id_nivel) WHERE f.id_evaluacion=? ORDER BY a.id_analisis DESC",
                r->new Resumen(r.getInt("id_analisis"),r.getInt("id_foto"),r.getTimestamp("fecha_hora").toLocalDateTime(),r.getString("lado"),r.getInt("puntaje_final"),r.getString("color"),r.getInt("nivel_oficial_rula"),r.getString("responsable")),evaluacion);
    }
    /** Datos históricos de un análisis y su relación con la evaluación. */
    public record Ficha(int id,int evaluacion,int foto,LocalDateTime fecha,String responsable,MotorRula.Entrada entrada,int puntajeA,int puntajeB,int fin) {}
    /** Lee entradas y totales históricos sin aceptar valores del navegador. */
    public Ficha consultar(Connection c,int id) throws SQLException {
        return Sql.uno(c,"SELECT a.*,f.id_evaluacion,(SELECT q.nombre FROM bitacora b JOIN cuenta q ON q.id_cuenta=b.id_cuenta WHERE b.tabla='analisis_rula' AND b.id_registro=a.id_analisis AND b.accion='CREAR' ORDER BY b.id_bitacora LIMIT 1) responsable FROM analisis_rula a JOIN fotografia f USING(id_foto) WHERE a.id_analisis=?",r->new Ficha(r.getInt("id_analisis"),r.getInt("id_evaluacion"),r.getInt("id_foto"),r.getTimestamp("fecha_hora").toLocalDateTime(),r.getString("responsable"),
                new MotorRula.Entrada(co.aplomo.rula.MedidorAngulos.Lado.valueOf(r.getString("lado")),1,1,Map.of(),new co.aplomo.rula.PuntuadorSegmentos.Ajustes(r.getBoolean("hombro_elevado"),r.getBoolean("brazo_separado"),r.getBoolean("brazo_apoyado"),r.getBoolean("antebrazo_cruza_linea_media"),r.getBoolean("muneca_desviada"),co.aplomo.rula.PuntuadorSegmentos.Giro.valueOf(r.getString("giro_muneca")),r.getBoolean("cuello_girado"),r.getBoolean("cuello_inclinado"),r.getBoolean("tronco_girado"),r.getBoolean("tronco_inclinado"),r.getBoolean("piernas_apoyadas"),false),r.getBoolean("postura_estatica_o_repetitiva"),MotorRula.Carga.valueOf(r.getString("carga"))),r.getInt("puntaje_a"),r.getInt("puntaje_b"),r.getInt("puntaje_final")),id);
    }
    /** Lee mediciones guardadas para presentar el análisis sin recalcular su historia. */
    public List<MotorRula.Medida> mediciones(Connection c,int id) throws SQLException {
        return Sql.listar(c,"SELECT * FROM medicion_angulo WHERE id_analisis=? ORDER BY id_medicion",r->new MotorRula.Medida(MotorRula.Segmento.valueOf(r.getString("segmento")),r.getString("puntos_marcados"),r.getDouble("angulo_grados"),r.getInt("puntos"),r.getBoolean("corregido_a_mano")),id);
    }
}
