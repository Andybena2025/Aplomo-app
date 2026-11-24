package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.dao.SalonDao;
import co.aplomo.dao.SedeDao;
import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Reserva;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * RF-009b a RF-012, RNF-005: agenda de visitas con estados y bitácora transaccionales.
 */
public final class Reservas {
    private final Transacciones tx;
    private final Clock reloj;
    private final ReservaDao reservas = new ReservaDao();
    private final BitacoraDao bitacora;

    /**
     * Datos editables y contexto de selección; el código y el estado no vienen del formulario. RNF-011.
     */
    public record Datos(int empresa, int sede, int salon, LocalDate fecha, LocalTime inicio, LocalTime fin,
                        String categoria, String descripcion, String notas) {
    }

    /**
     * Solo contiene opciones activas, con sus relaciones padre, para la selección de una reserva nueva. RNF-011.
     */
    public record Catalogo(List<Empresa> empresas, List<Sede> sedes, List<Salon> salones) {
    }

    /**
     * Usa MySQL y la fecha de Colombia para la validación de la agenda. RNF-011.
     */
    public Reservas() {
        this(new Transacciones(), Clock.system(ZoneId.of("America/Bogota")));
    }

    /**
     * Comparte conexiones y reloj para pruebas revertidas y límites de fecha reproducibles. RNF-011.
     */
    public Reservas(Transacciones tx, Clock reloj) {
        this(tx, reloj, new BitacoraDao());
    }

    /**
     * Permite comprobar que una auditoría fallida revierte también el cambio de la reserva. RNF-011.
     */
    public Reservas(Transacciones tx, Clock reloj, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.bitacora = bitacora;
    }

    /**
     * RF-033: agenda comparte fecha y transacciones.
     */
    public Avisos avisos() {
        return new Avisos(tx, reloj);
    }

    /**
     * RF-002 (propuesta): solicitudes con las mismas transacciones y reloj.
     */
    public SolicitudesVisita solicitudes() {
        return new SolicitudesVisita(tx, reloj);
    }

    /**
     * RF-033: calendario de las citas asignadas.
     */
    public Agenda agenda() {
        return new Agenda(tx, reloj);
    }

    /**
     * RF-011/033: intervalos libres de media hora; el POST vuelve a comprobar el cruce.
     */
    public List<LocalTime> horasLibres(Cuenta actor, LocalDate fecha, int omitir) throws SQLException {
        return tx.ejecutar(conexion -> {
            Cuenta actual = AccesoDatos.vigente(conexion, actor);
            Permisos.evaluador(actual);
            var cruces = co.aplomo.db.Sql.listar(conexion, "SELECT DISTINCT r.hora_inicio,r.hora_fin FROM reserva r " +
                            "WHERE r.id_evaluador=? AND r.fecha=? AND r.estado<>'CANCELADA' " +
                            "AND r.id_reserva<>? ORDER BY r.hora_inicio",
                    fila -> new LocalTime[]{fila.getTime(1).toLocalTime(), fila.getTime(2).toLocalTime()}, actual.id(),
                    fecha, omitir);
            var libres = new java.util.ArrayList<LocalTime>();
            for (LocalTime hora = LocalTime.of(8, 0); hora.isBefore(LocalTime.of(16, 0)); hora = hora.plusMinutes(30)) {
                LocalTime inicio = hora;
                LocalTime fin = hora.plusMinutes(30);
                if (cruces.stream().noneMatch(cruce -> inicio.isBefore(cruce[1]) && fin.isAfter(cruce[0]))) {
                    libres.add(hora);
                }
            }
            return List.copyOf(libres);
        });
    }

    /**
     * Devuelve la fecha de negocio usada por el formulario y la validación. RNF-011.
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /**
     * RF-013a: saludo con el mismo reloj y hora de Colombia que la agenda.
     */
    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj.withZone(ZoneId.of("America/Bogota")));
    }

    /**
     * Lista reservas con su empresa filtrada por SQL según la cuenta vigente. RNF-011.
     */
    public List<Ficha> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> reservas.listar(con, AccesoDatos.empresaPermitida(AccesoDatos.vigente(con, actor))));
    }

    /**
     * Consulta una reserva sin revelar las de otra empresa. RNF-011.
     */
    public Ficha consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(
                reservas.buscar(con, id, AccesoDatos.empresaPermitida(AccesoDatos.vigente(con, actor)), false)));
    }

    /** RF-011/002: prellena la reserva del plan con un puesto activo autorizado; no agrega columnas. */
    public java.util.Map<String, String> paraReevaluar(Cuenta actor, int puesto) throws SQLException {
        return tx.ejecutar(con -> {
            var cuenta = AccesoDatos.vigente(con, actor); Permisos.evaluador(cuenta);
            var alcance = AccesoDatos.empresaPermitida(cuenta);
            var datos = co.aplomo.db.Sql.uno(con,
                "SELECT p.numero,p.descripcion,l.id_salon,l.id_sede,s.id_empresa FROM puesto_trabajo p " +
                "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa) " +
                "WHERE p.id_puesto=? AND p.activo AND l.activo AND s.activo AND em.activo AND " +
                alcance.sql("s.id_empresa"), f -> java.util.Map.of("empresa", f.getString("id_empresa"),
                    "sede", f.getString("id_sede"), "salon", f.getString("id_salon"), "categoria", "REEVALUACION",
                    "descripcion", "Reevaluación del Puesto " + f.getInt("numero")),
                puesto, alcance.todas(), alcance.valor());
            if (datos == null) throw new SecurityException("El puesto no está disponible para tu cuenta.");
            return datos;
        });
    }

    /**
     * Lee los catálogos activos en tres consultas, evitando opciones bajo padres desactivados. RNF-011.
     */
    public Catalogo catalogo(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            var cuenta = administrador(con, actor);
            var alcance = AccesoDatos.empresaPermitida(cuenta);
            return new Catalogo(new EmpresaDao().activas(con, alcance), new SedeDao().activas(con, alcance),
                    new SalonDao().activos(con, alcance));
        });
    }

    /**
     * Crea o edita una reserva conservando código, empresa, sede, salón y estado en una edición. RNF-011.
     */
    public int guardar(Cuenta actor, int id, Datos datos) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta responsable = administrador(con, actor);
            Permisos.evaluador(responsable);
            Ficha anterior =
                    id == 0 ? null : exigir(reservas.buscar(con, id, AccesoDatos.empresaPermitida(responsable), true));
            if (anterior != null && anterior.reserva().evaluador() != null &&
                    !java.util.Objects.equals(anterior.reserva().evaluador(), responsable.id())) {
                throw new SecurityException("Esta cita está asignada a otro ingeniero.");
            }
            if (anterior != null && !anterior.reserva().editable()) {
                throw new IllegalArgumentException("Una reserva finalizada o cancelada no se puede editar.");
            }
            int salon = anterior == null ? datos.salon() : anterior.reserva().salon();
            Salon lugar = new SalonDao().buscar(con, salon, AccesoDatos.empresaPermitida(responsable), true);
            if (lugar == null) throw new IllegalArgumentException("Selecciona un salón existente.");
            if (anterior == null) comprobarSeleccion(con, datos, lugar);
            Reserva reserva = validar(id, salon, responsable.id(), anterior, datos);
            new co.aplomo.dao.CrucesAgendaDao().comprobar(con, reserva);
            Reserva repetida = reservas.duplicada(con, reserva);
            if (repetida != null) throw new IllegalArgumentException(
                    "Ya existe la reserva " + repetida.codigo() + " para ese salón, fecha y hora de inicio.");
            try {
                if (anterior == null) {
                    int nueva = reservas.crear(con, reserva,
                            UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                    String codigo = String.format(Locale.ROOT, "R-%04d", nueva);
                    reservas.codigo(con, nueva, codigo);
                    bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "reserva", nueva, "CREAR", null,
                            "ESPERANDO", codigo);
                    return nueva;
                }
                reservas.editar(con, reserva);
                bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "reserva", id, "EDITAR",
                        reserva.estado(), reserva.estado(), "Actualización de los datos de la reserva.");
                return id;
            } catch (SQLIntegrityConstraintViolationException error) {
                throw new IllegalArgumentException(
                        "Ya existe una reserva para ese salón, fecha y hora. Revisa el listado.");
            }
        });
    }

    /**
     * Acepta exclusivamente una reserva Esperando y registra el cambio a En cola. RNF-011.
     */
    public void aceptar(Cuenta actor, int id) throws SQLException {
        cambiar(actor, id, "EN_COLA", null);
    }

    /**
     * Cancela una reserva pendiente con motivo obligatorio y sin eliminar su historial. RNF-011.
     */
    public void cancelar(Cuenta actor, int id, String motivo) throws SQLException {
        cambiar(actor, id, "CANCELADA", Validacion.texto(motivo, "motivo de cancelación", 255));
    }

    private void cambiar(Cuenta actor, int id, String nuevo, String motivo) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta responsable = administrador(con, actor);
            Permisos.evaluador(responsable);
            Ficha ficha = exigir(reservas.buscar(con, id, AccesoDatos.empresaPermitida(responsable), true));
            if (ficha != null && ficha.reserva().evaluador() != null &&
                    !java.util.Objects.equals(ficha.reserva().evaluador(), responsable.id())) {
                throw new SecurityException("Esta cita está asignada a otro ingeniero.");
            }
            Reserva reserva = ficha.reserva();
            if (nuevo.equals("EN_COLA") && !reserva.estado().equals("ESPERANDO")) {
                throw new IllegalArgumentException("Solo se puede aceptar una reserva en estado Esperando.");
            }
            if (nuevo.equals("CANCELADA")) {
                if (!reserva.editable()) throw new IllegalArgumentException(
                        "Una reserva finalizada o cancelada no puede cambiar de estado.");
                if (ficha.visita() != null) {
                    throw new IllegalArgumentException(
                            "La visita ya se inició. La reserva se conserva hasta finalizarla.");
                }
            }
            reservas.estado(con, id, nuevo, motivo);
            var contacto = new co.aplomo.dao.CuentaDao().porId(con, reserva.administrador(), false);
            if (contacto != null && contacto.rol() == Cuenta.Rol.USUARIO) {
                String textoAviso = nuevo.equals("EN_COLA") ? "Tu visita fue aceptada." :
                        "Tu visita fue cancelada: " + motivo;
                new co.aplomo.dao.MensajeDao().enviar(con, id, responsable.id(), contacto.id(), "AVISO",
                        textoAviso, LocalDateTime.now(reloj));
            }
            bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "reserva", id, "CAMBIAR_ESTADO",
                    reserva.estado(), nuevo, motivo);
            return null;
        });
    }

    private Reserva validar(int id, int salon, int administrador, Ficha anterior, Datos datos) {
        if (datos.fecha() == null || datos.fecha().isBefore(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("Elige una fecha de hoy o posterior.");
        }
        if (datos.inicio() == null || datos.fin() == null ||
                !datos.fin().withNano(0).isAfter(datos.inicio().withNano(0))) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        String categoria = Validacion.opcion(datos.categoria(), "categoría", "EVALUACION_INICIAL", "SEGUIMIENTO",
                "REEVALUACION");
        return new Reserva(id, salon, anterior == null ? administrador : anterior.reserva().administrador(),
                anterior == null ? "" : anterior.reserva().codigo(), datos.fecha(), datos.inicio().withNano(0),
                datos.fin().withNano(0), categoria, Validacion.opcional(datos.descripcion(), "descripción", 255),
                Validacion.notas(datos.notas()), anterior == null ? "ESPERANDO" : anterior.reserva().estado(),
                anterior == null ? null : anterior.reserva().motivo(),
                anterior == null ? administrador : anterior.reserva().evaluador());
    }

    private static void comprobarSeleccion(Connection con, Datos datos, Salon salon) throws SQLException {
        Sede sede = new SedeDao().buscar(con, salon.sede(), 0, true);
        Empresa empresa = sede == null ? null : new EmpresaDao().buscar(con, sede.empresa(), 0, true);
        if (sede == null || empresa == null || datos.sede() != sede.id() || datos.empresa() != empresa.id()) {
            throw new IllegalArgumentException("La empresa, la sede y el salón deben corresponder entre sí.");
        }
        if (!empresa.activo() || !sede.activo() || !salon.activo()) {
            throw new IllegalArgumentException("Selecciona una empresa, sede y salón activos.");
        }
    }

    private static Cuenta administrador(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        Permisos.trabajo(actual);
        return actual;
    }

    private static Ficha exigir(Ficha ficha) {
        if (ficha == null) throw new SecurityException("La reserva no está disponible para tu cuenta.");
        return ficha;
    }
}
