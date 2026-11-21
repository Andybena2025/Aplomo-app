package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.PendienteDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * RF-022, RNF-004/005/009: seguimiento autorizado y automático en la transacción del análisis.
 */
public final class Pendientes {
    private final Transacciones tx;
    private final Clock reloj;
    private final BitacoraDao bitacora;
    private final PendienteDao dao = new PendienteDao();

    /**
     * Usa MySQL, auditoría y hora de Colombia. RNF-011.
     */
    public Pendientes() {
        this(new Transacciones(), Clock.system(ZoneId.of("America/Bogota")), new BitacoraDao());
    }

    /**
     * Comparte transacciones y reloj para comprobar sin confirmar datos de prueba. RNF-011.
     */
    public Pendientes(Transacciones tx, Clock reloj, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.bitacora = bitacora;
    }

    /**
     * Lista por rol, empresa y visita; solo permite filtros y órdenes documentados. RNF-011.
     */
    public List<PendienteDao.Ficha> listar(Cuenta actor, String estado, String orden) throws SQLException {
        String texto2 = Validacion.opcion(estado, "estado", "ABIERTO", "ARREGLADO",
                "TODOS"), texto3 = Validacion.opcion(orden, "orden", "prioridad", "fecha", "limite");
        return tx.ejecutar(conexion -> {
            var actual = AccesoDatos.vigente(conexion, actor);
            return dao.listar(conexion, AccesoDatos.empresaPermitida(actual), actual.rol() == Cuenta.Rol.USUARIO,
                    texto2, texto3);
        });
    }

    /**
     * Abre un pendiente únicamente dentro del alcance vigente de la cuenta. RNF-011.
     */
    public PendienteDao.Ficha consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> visible(conexion, actor, id, false));
    }

    /**
     * Conserva la bitácora visible con fecha, responsable y acciones del pendiente. RNF-011.
     */
    public List<PendienteDao.Cambio> historial(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> {
            visible(conexion, actor, id, false);
            return dao.historial(conexion, id);
        });
    }

    /**
     * RF-022: las posturas solo se consultan después de verificar cuenta, empresa y estado de visita.
     */
    public List<co.aplomo.dao.ConsultaDao.Fila> posturas(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            visible(con, actor, id, false);
            return dao.posturas(con, id);
        });
    }

    /**
     * Guarda seguimiento opcional sin imponer fechas futuras que no exigen los documentos. RNF-011.
     */
    public void guardar(Cuenta actor, int id, String descripcion, String prioridad, String fecha) throws SQLException {
        tx.ejecutar(conexion -> {
            var actual = admin(conexion, actor);
            var elemento = exigir(dao.buscar(conexion, id, AccesoDatos.empresaPermitida(actual), false, true));
            String texto = Validacion.notas(descripcion);
            if (texto.codePointCount(0, texto.length()) > 255)
                throw new IllegalArgumentException("La descripción permite hasta 255 caracteres.");
            String texto2 =
                    prioridad == null || prioridad.isBlank() ? null : Validacion.opcion(prioridad, "prioridad", "ALTA",
                            "MEDIA", "BAJA");
            LocalDate limite = fecha(fecha);
            var anterior = elemento.pendiente();
            if (Objects.equals(anterior.descripcion(), texto.isEmpty() ? null : texto) &&
                    Objects.equals(anterior.prioridad(), texto2) && Objects.equals(anterior.limite(), limite))
                return null;
            dao.editar(conexion, id, texto.isEmpty() ? null : texto, texto2, limite);
            bitacora.registrar(conexion, actual.id(), LocalDateTime.now(reloj), "pendiente_arreglo", id, "EDITAR", null,
                    null, "Se actualizaron los datos de seguimiento: descripción, prioridad o fecha límite.");
            return null;
        });
    }

    /**
     * Marca solo este registro; un segundo envío no duplica el cambio ni su auditoría. RNF-011.
     */
    public boolean arreglar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> {
            var actual = admin(conexion, actor);
            var elemento = exigir(dao.buscar(conexion, id, AccesoDatos.empresaPermitida(actual), false, true));
            if (elemento.pendiente().estado().equals("ARREGLADO")) return false;
            dao.arreglar(conexion, id);
            bitacora.registrar(conexion, actual.id(), LocalDateTime.now(reloj), "pendiente_arreglo", id,
                    "CAMBIAR_ESTADO", "ABIERTO", "ARREGLADO",
                    "El Administrador confirmó que la empresa arregló el puesto.");
            return true;
        });
    }

    /**
     * RF-022: se invoca con un resultado Rojo calculado por el servidor, bajo el bloqueo de la visita.
     */
    void registrarRojo(Connection conexion, Cuenta actor, int evaluacion) throws SQLException {
        Integer maximo = Sql.uno(conexion,
                "SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_" +
                        "foto) JOIN nivel_riesgo n USING(id_nivel) WHERE f.id_evaluacion=? AND n.colo" + "r=?",
                fila -> fila.getInt(1), evaluacion, "ROJO");
        if (maximo == null || maximo == 0) return;
        var anterior = dao.porPuestoVisita(conexion, evaluacion);
        if (anterior == null) {
            int id = dao.crear(conexion, evaluacion, maximo, LocalDate.now(reloj));
            bitacora.registrar(conexion, actor.id(), LocalDateTime.now(reloj), "pendiente_arreglo", id, "CREAR", null,
                    "ABIERTO", "Pendiente creado automáticamente por un resultado Rojo.");
        } else if (maximo > anterior.puntaje()) {
            dao.puntaje(conexion, anterior.id(), maximo);
            bitacora.registrar(conexion, actor.id(), LocalDateTime.now(reloj), "pendiente_arreglo", anterior.id(),
                    "EDITAR", null, null,
                    "El puntaje máximo del puesto en la visita pasó de " + anterior.puntaje() + " a " + maximo +
                            "; se conservó su seguimiento y estado.");
        }
    }

    private PendienteDao.Ficha visible(Connection conexion, Cuenta actor, int id, boolean bloqueo) throws SQLException {
        var actual = AccesoDatos.vigente(conexion, actor);
        return exigir(dao.buscar(conexion, id, AccesoDatos.empresaPermitida(actual), actual.rol() == Cuenta.Rol.USUARIO,
                bloqueo));
    }

    private static PendienteDao.Ficha exigir(PendienteDao.Ficha elemento) {
        if (elemento == null)
            throw new SecurityException("Acceso denegado. El pendiente no está disponible para tu cuenta.");
        return elemento;
    }

    private static Cuenta admin(Connection conexion, Cuenta cuenta) throws SQLException {
        var actual = AccesoDatos.vigente(conexion, cuenta);
        Permisos.trabajo(actual);
        return actual;
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            var elemento = LocalDate.parse(texto);
            if (elemento.getYear() < 1000 || elemento.getYear() > 9999)
                throw new DateTimeParseException("Fuera de rango", texto, 0);
            return elemento;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Revisa la fecha límite: escribe una fecha válida.");
        }
    }
}
