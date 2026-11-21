package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-016, RNF-005/006: reemplazo lógico, sin borrar fotos ni sus análisis históricos.
 */
public final class FotosEvaluacion {
    public static final String REEMPLAZADA = "Reemplazada · ";

    /**
     * RNF-011: componente de la aplicación.
     */
    public record Cambio(Integer foto, Integer anterior, String vista, String descripcion) {
    }

    private final Transacciones tx;
    private final Clock reloj;
    private final Fotos archivos;
    private final BitacoraDao bitacora;

    /**
     * RNF-011: operación del componente.
     */
    public FotosEvaluacion(Transacciones tx, Clock reloj, Fotos archivos, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.archivos = archivos;
        this.bitacora = bitacora;
    }

    /**
     * Nueva imagen opcional: sin imagen, marca como reemplazada la anterior, conservando su archivo. RNF-011.
     */
    public Cambio cambiar(Cuenta actor, int evaluacion, Integer anterior,
            Fotos.Entrada entrada) throws SQLException, IOException {
        List<String> nuevos = new ArrayList<>();
        try {
            return tx.ejecutar(conexion -> {
                Cuenta admin = AccesoDatos.vigente(conexion, actor);
                Permisos.trabajo(admin);
                var dao = new EvaluacionDao();
                AccesoDatos.exigirEvaluacion(conexion, actor, evaluacion, true);
                var evaluacion2 = dao.consultar(conexion, evaluacion);
                if (evaluacion2 == null) throw new SecurityException("La evaluación no está disponible.");
                var vd = new VisitaDao();
                var visita = vd.buscar(conexion, evaluacion2.visita(), 0, false, false);
                if (visita == null) throw new SecurityException("La visita no está disponible.");
                new ReservaDao().buscar(conexion, visita.visita().reserva(), 0, true);
                visita = vd.buscar(conexion, evaluacion2.visita(), 0, false, true);
                if (!visita.visita().estado().equals("EN_CURSO"))
                    throw new IllegalArgumentException("Solo puedes cambiar fotos de una visita En curso.");
                if (evaluacion2.datos().sinFotos() || !evaluacion2.datos().aceptaFotos())
                    throw new IllegalArgumentException("Esta evaluación no tiene permiso para fotos.");
                EvaluacionDao.Foto previa = null;
                if (anterior != null) {
                    Sql.uno(conexion, "SELECT id_foto FROM fotografia WHERE id_foto=? FOR UPDATE",
                            fila -> fila.getInt(1), anterior);
                    previa = dao.foto(conexion, anterior);
                    if (previa == null || previa.evaluacion() != evaluacion)
                        throw new SecurityException("La foto no pertenece a esta evaluación.");
                }
                if (entrada == null && previa == null)
                    throw new IllegalArgumentException("Elige la foto que quieres quitar.");
                if (entrada != null && previa != null && previa.descripcion().startsWith(REEMPLAZADA))
                    throw new IllegalArgumentException("Esta foto ya fue reemplazada. Abre la foto actual.");
                Integer nueva = null;
                String vista = "", descripcion = "";
                if (entrada != null) {
                    long activas = dao.fotos(conexion, evaluacion).stream()
                            .filter(evaluacion3 -> !evaluacion3.descripcion().startsWith(REEMPLAZADA)).count();
                    if (activas - (previa == null ? 0 : 1) >= 10)
                        throw new IllegalArgumentException("Puedes guardar hasta 10 fotos activas.");
                    var imagen = archivos.validar(List.of(entrada)).get(0);
                    String nombre;
                    try {
                        nombre = archivos.guardar(imagen);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                    nuevos.add(nombre);
                    nueva = dao.foto(conexion, evaluacion, nombre, imagen.vista(), imagen.descripcion(),
                            imagen.bytes().length);
                    vista = imagen.vista();
                    descripcion = imagen.descripcion();
                    bitacora.registrar(conexion, admin.id(), LocalDateTime.now(reloj), "fotografia", nueva, "CREAR",
                            null, null, "Foto nueva · Evaluación " + evaluacion);
                }
                if (previa != null && !previa.descripcion().startsWith(REEMPLAZADA)) {
                    String marca = REEMPLAZADA + previa.descripcion();
                    marca = marca.codePoints().limit(200)
                            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                            .toString();
                    Sql.actualizar(conexion, "UPDATE fotografia SET descripcion=? WHERE id_foto=?", marca, previa.id());
                    bitacora.registrar(conexion, admin.id(), LocalDateTime.now(reloj), "fotografia", previa.id(),
                            "EDITAR", null, null,
                            "Reemplazada" + (nueva == null ? "" : " por foto " + nueva) + " · " + previa.descripcion());
                }
                return new Cambio(nueva, anterior, vista, descripcion);
            });
        } catch (SQLException | RuntimeException error2) {
            archivos.revertirNuevos(nuevos);
            if (error2 instanceof UncheckedIOException io) throw io.getCause();
            throw error2;
        }
    }
}
