package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.dao.VisitaDao.Evaluacion;
import co.aplomo.dao.VisitaDao.Ficha;
import co.aplomo.dao.VisitaDao.ParaEvaluar;
import co.aplomo.dao.VisitaDao.Resultado;
import co.aplomo.db.Transacciones;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RF-013a, RF-013b, RNF-005: ciclo de visita con su reserva y auditoría atómicas.
 */
public final class Visitas {
    private final Transacciones tx;
    private final Clock reloj;
    private final VisitaDao visitas = new VisitaDao();
    private final ReservaDao reservas = new ReservaDao();
    private final BitacoraDao bitacora;

    /**
     * Usa MySQL y la hora local de Colombia. RNF-011.
     */
    public Visitas() {
        this(new Transacciones(), Clock.system(ZoneId.of("America/Bogota")));
    }

    /**
     * Comparte conexiones y reloj para comprobar el ciclo sin confirmar datos de prueba. RNF-011.
     */
    public Visitas(Transacciones tx, Clock reloj) {
        this(tx, reloj, new BitacoraDao());
    }

    /**
     * Permite comprobar la reversión completa si falla el registro de auditoría. RNF-011.
     */
    public Visitas(Transacciones tx, Clock reloj, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.bitacora = bitacora;
    }

    /**
     * Lista visitas con empresa y estado filtrados según el permiso vigente. RNF-011.
     */
    public List<Ficha> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            return visitas.listar(con, AccesoDatos.empresaPermitida(actual), actual.rol() == Rol.USUARIO);
        });
    }

    /**
     * Acceso administrativo a puestos pendientes; no expone visitas en curso al Usuario. RNF-011.
     */
    public List<ParaEvaluar> paraEvaluar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            var cuenta = administrador(con, actor);
            return visitas.paraEvaluar(con, AccesoDatos.empresaPermitida(cuenta));
        });
    }

    /**
     * Práctica comparte las transacciones y el reloj, sin usar privilegios de MySQL adicionales. RNF-011.
     */
    public PracticaEvaluacion practica() {
        return new PracticaEvaluacion(tx, reloj);
    }

    /**
     * Consulta una visita; el Usuario solo puede abrir las finalizadas de su propia empresa. RNF-011.
     */
    public Ficha consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> visible(con, actor, id));
    }

    /**
     * Consulta los resúmenes de evaluaciones sin entregar fotografías ni rutas de archivos. RNF-011.
     */
    public List<Evaluacion> evaluaciones(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            visible(con, actor, id);
            return visitas.evaluaciones(con, id, false);
        });
    }

    /**
     * Puestos activos del salón; solo el Administrador puede preparar una evaluación. RNF-011.
     */
    public List<co.aplomo.modelo.Puesto> puestos(Cuenta actor, int visita) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            var ficha = visible(con, actor, visita);
            return new EvaluacionDao().puestos(con, ficha.salon());
        });
    }

    /**
     * Inicia una sola visita desde En cola y registra la hora real y el responsable. RNF-011.
     */
    public int iniciar(Cuenta actor, int reserva) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta responsable = administrador(con, actor);
            ReservaDao.Ficha ficha = reservas.buscar(con, reserva, AccesoDatos.empresaPermitida(responsable), true);
            if (ficha == null) throw new SecurityException("La reserva no está disponible para tu cuenta.");
            if (!ficha.reserva().estado().equals("EN_COLA")) {
                throw new IllegalArgumentException("Primero acepta la reserva para iniciar la visita desde En cola.");
            }
            if (ficha.visita() != null)
                throw new IllegalArgumentException("Esta reserva ya tiene una visita. Abre la existente.");
            if (Sql.listar(con, "SELECT id_puesto FROM puesto_trabajo WHERE id_salon=? AND activo=TRUE FOR UPDATE",
                    fila -> fila.getInt(1), ficha.reserva().salon()).isEmpty())
                throw new IllegalArgumentException("Este salón no tiene puestos activos");
            LocalDateTime ahora = LocalDateTime.now(reloj);
            int id;
            try {
                id = visitas.iniciar(con, reserva, ahora);
            } catch (SQLIntegrityConstraintViolationException error) {
                throw new IllegalArgumentException("Esta reserva ya tiene una visita. Abre la existente.");
            }
            bitacora.registrar(con, responsable.id(), ahora, "visita", id, "CREAR", null, "EN_CURSO",
                    "Inicio desde " + ficha.reserva().codigo());
            return id;
        });
    }

    /**
     * Finaliza con evaluaciones y guarda visita, reserva, promedio y auditoría en una sola transacción. RNF-011.
     */
    public void finalizar(Cuenta actor, int id, String notas) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta responsable = administrador(con, actor);
            AccesoDatos.exigirVisita(con, responsable, id);
            Ficha inicial = exigir(visitas.buscar(con, id, 0, false, false));

            // Orden único de bloqueo: reserva y después visita, igual que al iniciar o cancelar.
            ReservaDao.Ficha reserva = reservas.buscar(con, inicial.visita().reserva(), 0, true);
            Ficha ficha = exigir(visitas.buscar(con, id, 0, false, true));
            if (!ficha.visita().estado().equals("EN_CURSO"))
                throw new IllegalArgumentException("La visita ya está finalizada.");
            if (reserva == null || !reserva.reserva().estado().equals("EN_COLA")) {
                throw new IllegalArgumentException("La reserva debe seguir En cola para finalizar su visita.");
            }
            List<Evaluacion> evaluaciones = visitas.evaluaciones(con, id, true);
            if (evaluaciones.isEmpty())
                throw new IllegalArgumentException("Guarda al menos una evaluación antes de finalizar la visita.");
            List<Resultado> resultados = visitas.resultadosActuales(con, id);
            BigDecimal promedio = calificacion(evaluaciones, resultados);
            String observaciones = Validacion.notas(notas);
            LocalDateTime ahora = LocalDateTime.now(reloj);
            if (ahora.isBefore(ficha.visita().inicio()))
                throw new IllegalArgumentException("La hora de fin no puede ser anterior al inicio de la visita.");
            visitas.finalizar(con, id, ahora, observaciones, promedio);
            reservas.estado(con, reserva.reserva().id(), "FINALIZADA", null);
            bitacora.registrar(con, responsable.id(), ahora, "visita", id, "CAMBIAR_ESTADO", "EN_CURSO", "FINALIZADA",
                    "Finalización de visita.");
            bitacora.registrar(con, responsable.id(), ahora, "reserva", reserva.reserva().id(), "CAMBIAR_ESTADO",
                    "EN_COLA", "FINALIZADA", "Visita finalizada.");
            return null;
        });
    }

    private static BigDecimal calificacion(List<Evaluacion> evaluaciones, List<Resultado> resultados) {
        Map<Integer, Integer> ultimaEvaluacion = new HashMap<>();
        for (Evaluacion evaluacion : evaluaciones)
            ultimaEvaluacion.merge(evaluacion.puesto(), evaluacion.id(), Math::max);
        Map<Integer, Integer> puntajes = new HashMap<>();

        // RF-021: máximo de todos los análisis de la última evaluación del puesto, sin perder un lado del cuerpo.
        for (Resultado resultado : resultados) {
            if (resultado.puntaje() == null || resultado.foto() == null ||
                    !Objects.equals(ultimaEvaluacion.get(resultado.puesto()), resultado.evaluacion())) continue;
            puntajes.merge(resultado.puesto(), resultado.puntaje(), Math::max);
        }
        if (puntajes.isEmpty()) return null;
        BigDecimal suma = BigDecimal.ZERO;
        for (int puntaje : puntajes.values()) suma = suma.add(BigDecimal.valueOf(puntaje));
        return suma.divide(BigDecimal.valueOf(puntajes.size()), 1, RoundingMode.HALF_UP);
    }

    private Ficha visible(Connection con, Cuenta actor, int id) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        if (actual.rol() != Rol.USUARIO) {
            AccesoDatos.exigirVisita(con, actual, id);
            return exigir(visitas.buscar(con, id, 0, false, false));
        }
        return exigir(visitas.buscar(con, id, AccesoDatos.empresaPermitida(actual), true, false));
    }

    private static Cuenta administrador(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        Permisos.trabajo(actual);
        return actual;
    }

    private static Ficha exigir(Ficha ficha) {
        if (ficha == null) throw new SecurityException("La visita no está disponible para tu cuenta.");
        return ficha;
    }
}
