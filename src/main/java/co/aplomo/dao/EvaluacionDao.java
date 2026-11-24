package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.servicio.Evaluaciones.Datos;
import co.aplomo.modelo.Puesto;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * RF-014 a RF-016: persistencia de evaluación, condiciones y fotografías sin modificar el esquema.
 */
public final class EvaluacionDao {

    /**
     * Metadatos de una foto; su ruta nunca se entrega a un Usuario. RNF-011.
     */
    public record Foto(int id, int evaluacion, String nombre, String vista, String descripcion) {
    }

    /**
     * Evaluación guardada y sus campos aprobados para la consulta administrativa. RNF-011.
     */
    public record Ficha(int id, int visita, int puesto, int numero, LocalDate fecha, Datos datos, String responsable,
                        String tipoPuesto) {
    }

    /**
     * Crea el acceso JDBC de evaluación. RNF-011.
     */
    public EvaluacionDao() {
    }

    /**
     * Lista puestos activos del salón reservado y su jerarquía activa. RNF-011.
     */
    public List<Puesto> puestos(Connection con, int salon) throws SQLException {
        return new PuestoDao().buscarTodos(con, salon, 0).stream().filter(Puesto::activo).toList();
    }

    /**
     * Guarda todos los campos aprobados de la evaluación. RNF-011.
     */
    public int crear(Connection con, int visita, Datos evaluacion2, String motivo,
            LocalDate fecha) throws SQLException {
        return Sql.insertar(con, "INSERT INTO evaluacion (id_visita,id_puesto,fecha,postura,criterio,motivo,mi" +
                        "nutos_jornada,persona_acepto,registro_fotografico,trabajador_genero,trabajador_edad," +
                        "trabajador_antiguedad) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", visita,
                evaluacion2.puesto(), fecha, evaluacion2.postura(), evaluacion2.criterio(), motivo,
                evaluacion2.minutos(), evaluacion2.aceptaFotos(),
                evaluacion2.sinFotos() ? "SIN_REGISTRO_FOTOGRAFICO" : "CON_FOTOS", evaluacion2.trabajador().genero(),
                evaluacion2.trabajador().edad(), evaluacion2.trabajador().antiguedad());
    }

    /**
     * Guarda las seis condiciones y las observaciones de RF-015.
     */
    public void condiciones(Connection con, int id, Datos evaluacion2) throws SQLException {
        Sql.actualizar(con, "INSERT INTO condiciones_puesto (id_evaluacion,escritorio_muy_alto,pantalla_m" +
                        "uy_alta,pies_colgando,tiene_apoyapies,silla_con_apoyabrazos,silla_regulable," +
                        "otras_observaciones) VALUES (?,?,?,?,?,?,?,?)", id, evaluacion2.escritorio(),
                evaluacion2.pantalla(),
                evaluacion2.pies(), evaluacion2.apoyapies(), evaluacion2.apoyabrazos(), evaluacion2.regulable(),
                evaluacion2.observaciones());
    }

    /**
     * Guarda metadatos y la ruta relativa aleatoria; el contenido queda fuera de MySQL. RNF-011.
     */
    public int foto(Connection con, int evaluacion, String nombre, String vista, String descripcion,
            int bytes) throws SQLException {
        return Sql.insertar(con, "INSERT INTO fotografia (id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,de" +
                        "scripcion) VALUES (?,?,?,?,?,?)", evaluacion, nombre, nombre, (bytes + 1023) / 1024, vista,
                descripcion);
    }

    /**
     * Consulta una fotografía para servirla solo después de comprobar el permiso. RNF-011.
     */
    public Foto foto(Connection con, int id) throws SQLException {
        return Sql.uno(con,
                "SELECT id_foto,id_evaluacion,ruta,vista,descripcion FROM fotografia WHERE id" + "_foto = ?",
                fila -> new Foto(fila.getInt(1), fila.getInt(2), fila.getString(3), fila.getString(4),
                        fila.getString(5)), id);
    }

    /**
     * Consulta los datos y condiciones de una evaluación con su responsable de creación. RNF-011.
     */
    public Ficha consultar(Connection con, int id) throws SQLException {
        return Sql.uno(con, "SELECT e.*,c.*,p.numero,p.tipo AS tipo_puesto,(SELECT q.nombre FROM bitacora" +
                        " b JOIN cuenta q ON q.id_cuenta=b.id_cuenta WHERE b.tabla='evaluacion' AND b" +
                        ".id_registro=e.id_evaluacion AND b.accion='CREAR' ORDER BY b.id_bitacora LIM" +
                        "IT 1) AS responsable FROM evaluacion e JOIN condiciones_puesto c USING(id_ev" +
                        "aluacion) JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto WHERE e.id_evalua" + "cion=?",
                fila -> new Ficha(fila.getInt("id_evaluacion"), fila.getInt("id_visita"), fila.getInt("id_puesto"),
                        fila.getInt("numero"), fila.getDate("fecha").toLocalDate(),
                        new Datos(fila.getInt("id_puesto"), fila.getString("postura"), fila.getString("criterio"),
                                fila.getString("motivo"), fila.getInt("minutos_jornada"),
                                fila.getBoolean("escritorio_muy_alto"), fila.getBoolean("pantalla_muy_alta"),
                                fila.getBoolean("pies_colgando"), fila.getBoolean("tiene_apoyapies"),
                                fila.getBoolean("silla_con_apoyabrazos"), fila.getBoolean("silla_regulable"),
                                fila.getString("otras_observaciones"), fila.getBoolean("persona_acepto"),
                                fila.getString("registro_fotografico").equals("SIN_REGISTRO_FOTOGRAFICO"),
                                new co.aplomo.modelo.Trabajador(fila.getString("trabajador_genero"),
                                        Sql.enteroOpcional(fila, "trabajador_edad"),
                                        fila.getString("trabajador_antiguedad"))),
                        fila.getString("responsable"), fila.getString("tipo_puesto")), id);
    }

    /**
     * Lista las fotos de una evaluación después de la comprobación administrativa del servicio. RNF-011.
     */
    public List<Foto> fotos(Connection con, int evaluacion) throws SQLException {
        return Sql.listar(con, "SELECT id_foto,id_evaluacion,ruta,vista,descripcion FROM fotografia WHERE id" +
                        "_evaluacion=? ORDER BY id_foto",
                fila -> new Foto(fila.getInt(1), fila.getInt(2), fila.getString(3), fila.getString(4),
                        fila.getString(5)), evaluacion);
    }
}
