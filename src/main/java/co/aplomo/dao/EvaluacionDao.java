package co.aplomo.dao;

import co.aplomo.db.Sql;
import co.aplomo.servicio.Evaluaciones.Datos;
import co.aplomo.modelo.Puesto;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-014 a RF-016: persistencia de evaluación, condiciones y fotografías sin modificar el esquema. */
public final class EvaluacionDao {
    /** Metadatos de una foto; su ruta nunca se entrega a un Usuario. */
    public record Foto(int id, int evaluacion, String nombre, String vista, String descripcion) {}
    /** Evaluación guardada y sus campos aprobados para la consulta administrativa. */
    public record Ficha(int id, int visita, int puesto, int numero, LocalDate fecha, Datos datos, String responsable) {}
    /** Crea el acceso JDBC de evaluación. */
    public EvaluacionDao() {}
    /** Lista puestos activos del salón reservado y su jerarquía activa. */
    public List<Puesto> puestos(Connection con, int salon) throws SQLException {
        return new PuestoDao().buscarTodos(con, salon, 0).stream().filter(Puesto::activo).toList();
    }
    /** Guarda todos los campos aprobados de la evaluación. */
    public int crear(Connection con, int visita, Datos d, String motivo, LocalDate fecha) throws SQLException {
        return Sql.insertar(con, "INSERT INTO evaluacion (id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,persona_acepto,registro_fotografico) VALUES (?,?,?,?,?,?,?,?,?)",
                visita, d.puesto(), fecha, d.postura(), d.criterio(), motivo, d.minutos(), d.aceptaFotos(), d.sinFotos() ? "SIN_REGISTRO_FOTOGRAFICO" : "CON_FOTOS");
    }
    /** Guarda las seis condiciones y las observaciones de RF-015. */
    public void condiciones(Connection con, int id, Datos d) throws SQLException {
        Sql.actualizar(con, "INSERT INTO condiciones_puesto (id_evaluacion,escritorio_muy_alto,pantalla_muy_alta,pies_colgando,tiene_apoyapies,silla_con_apoyabrazos,silla_regulable,otras_observaciones) VALUES (?,?,?,?,?,?,?,?)",
                id, d.escritorio(), d.pantalla(), d.pies(), d.apoyapies(), d.apoyabrazos(), d.regulable(), d.observaciones());
    }
    /** Guarda metadatos y la ruta relativa aleatoria; el contenido queda fuera de MySQL. */
    public int foto(Connection con, int evaluacion, String nombre, String vista, String descripcion, int bytes) throws SQLException {
        return Sql.insertar(con, "INSERT INTO fotografia (id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,descripcion) VALUES (?,?,?,?,?,?)",
                evaluacion, nombre, nombre, (bytes + 1023) / 1024, vista, descripcion);
    }
    /** Consulta una fotografía para servirla solo después de comprobar el permiso. */
    public Foto foto(Connection con, int id) throws SQLException {
        return Sql.uno(con, "SELECT id_foto,id_evaluacion,ruta,vista,descripcion FROM fotografia WHERE id_foto = ?",
                r -> new Foto(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4), r.getString(5)), id);
    }

    /** Consulta los datos y condiciones de una evaluación con su responsable de creación. */
    public Ficha consultar(Connection con, int id) throws SQLException {
        return Sql.uno(con, "SELECT e.*,c.*,p.numero,(SELECT q.nombre FROM bitacora b JOIN cuenta q ON q.id_cuenta=b.id_cuenta WHERE b.tabla='evaluacion' AND b.id_registro=e.id_evaluacion AND b.accion='CREAR' ORDER BY b.id_bitacora LIMIT 1) AS responsable FROM evaluacion e JOIN condiciones_puesto c USING(id_evaluacion) JOIN puesto_trabajo p ON p.id_puesto=e.id_puesto WHERE e.id_evaluacion=?",
                r -> new Ficha(r.getInt("id_evaluacion"),r.getInt("id_visita"),r.getInt("id_puesto"),r.getInt("numero"),r.getDate("fecha").toLocalDate(),
                        new Datos(r.getInt("id_puesto"),r.getString("postura"),r.getString("criterio"),r.getString("motivo"),r.getInt("minutos_jornada"),r.getBoolean("escritorio_muy_alto"),r.getBoolean("pantalla_muy_alta"),r.getBoolean("pies_colgando"),r.getBoolean("tiene_apoyapies"),r.getBoolean("silla_con_apoyabrazos"),r.getBoolean("silla_regulable"),r.getString("otras_observaciones"),r.getBoolean("persona_acepto"),r.getString("registro_fotografico").equals("SIN_REGISTRO_FOTOGRAFICO")),r.getString("responsable")),id);
    }
    /** Lista las fotos de una evaluación después de la comprobación administrativa del servicio. */
    public List<Foto> fotos(Connection con, int evaluacion) throws SQLException {
        return Sql.listar(con,"SELECT id_foto,id_evaluacion,ruta,vista,descripcion FROM fotografia WHERE id_evaluacion=? ORDER BY id_foto",
                r->new Foto(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5)),evaluacion);
    }
}
