package co.aplomo.dao;

import co.aplomo.modelo.Puesto;
import co.aplomo.db.Sql;
import java.sql.*;
import java.util.*;

/** RF-004 a RF-010, RF-028: consultas parametrizadas de puesto_trabajo. */
public final class PuestoDao {
    /** Crea el acceso JDBC al catálogo de puesto_trabajo. */
    public PuestoDao() {}

    /** Consulta una fila con filtro de empresa y bloqueo opcional para las modificaciones. */
    public Puesto buscar(Connection con, int id, int empresaPermitida, boolean bloquear) throws SQLException {
        String consulta = seleccion(empresaPermitida>0)+" WHERE r.id_puesto = ? AND (? = 0 OR s.id_empresa = ?)" + (bloquear ? " FOR UPDATE" : "");
        return Sql.uno(con, consulta, PuestoDao::leer, id, empresaPermitida, empresaPermitida);
    }

    /** Lista el catálogo de un padre y limita los resultados a la empresa autorizada (RF-002). */
    public List<Puesto> buscarTodos(Connection con, int padre, int empresaPermitida) throws SQLException {
        return Sql.listar(con, seleccion(empresaPermitida>0)+" WHERE r.id_salon = ? AND (? = 0 OR s.id_empresa = ?) ORDER BY r.numero, r.id_puesto",
                PuestoDao::leer, padre, empresaPermitida, empresaPermitida);
    }

    /** Guarda los datos editables y conserva el ID, las relaciones y el estado existentes. */
    public int guardar(Connection con, Puesto dato) throws SQLException {
        if (dato.id() == 0) {
            return Sql.insertar(con, "INSERT INTO puesto_trabajo (id_salon, id_area, id_cargo, numero, descripcion, tipo, ubicacion) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    dato.salon(), dato.area(), dato.cargo(), dato.numero(), dato.descripcion(), dato.tipo(), dato.ubicacion());
        }
        Sql.actualizar(con, "UPDATE puesto_trabajo SET id_area = ?, id_cargo = ?, numero = ?, descripcion = ?, tipo = ?, ubicacion = ? WHERE id_puesto = ?",
                dato.area(), dato.cargo(), dato.numero(), dato.descripcion(), dato.tipo(), dato.ubicacion(), dato.id());
        return dato.id();
    }

    /** Desactiva o reactiva una fila sin borrar su historial (RF-028). */
    public void estado(Connection con, int id, boolean activo) throws SQLException {
        Sql.actualizar(con, "UPDATE puesto_trabajo SET activo = ? WHERE id_puesto = ?", activo, id);
    }

    private static String seleccion(boolean usuario){String puntaje=usuario?"(SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=(SELECT MAX(e.id_evaluacion) FROM evaluacion e JOIN visita v USING(id_visita) WHERE e.id_puesto=r.id_puesto AND v.estado='FINALIZADA'))":"r.ultimo_puntaje";return "SELECT r.*,"+puntaje+" puntaje_visible,n.color,n.nivel_oficial_rula FROM puesto_trabajo r LEFT JOIN nivel_riesgo n ON "+puntaje+" BETWEEN n.puntaje_minimo AND n.puntaje_maximo JOIN salon l ON l.id_salon=r.id_salon JOIN sede s ON s.id_sede=l.id_sede";}

    private static Puesto leer(ResultSet fila) throws SQLException {
        return new Puesto(
                fila.getInt("id_puesto"),
                fila.getInt("id_salon"),
                Sql.enteroOpcional(fila, "id_area"),
                Sql.enteroOpcional(fila, "id_cargo"),
                fila.getInt("numero"),
                fila.getString("descripcion"),
                fila.getString("tipo"),
                fila.getString("ubicacion"),
                Sql.enteroOpcional(fila, "puntaje_visible"),
                fila.getBoolean("activo"),fila.getString("color"),fila.getInt("nivel_oficial_rula"));
    }
}
