package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.db.*;
import co.aplomo.modelo.*;
import co.aplomo.seguridad.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-009b a RF-012, RNF-005: agenda de visitas con estados y bitácora transaccionales. */
public final class Reservas {
    private final Transacciones tx;
    private final Clock reloj;
    private final ReservaDao reservas = new ReservaDao();
    private final BitacoraDao bitacora;

    /** Datos editables y contexto de selección; el código y el estado no vienen del formulario. */
    public record Datos(int empresa, int sede, int salon, LocalDate fecha, LocalTime inicio, LocalTime fin,
                        String categoria, String descripcion, String notas) {}

    /** Solo contiene opciones activas, con sus relaciones padre, para la selección de una reserva nueva. */
    public record Catalogo(List<Empresa> empresas, List<Sede> sedes, List<Salon> salones) {}

    /** Usa MySQL y la fecha de Colombia para la validación de la agenda. */
    public Reservas() { this(new Transacciones(), Clock.system(ZoneId.of("America/Bogota"))); }

    /** Comparte conexiones y reloj para pruebas revertidas y límites de fecha reproducibles. */
    public Reservas(Transacciones tx, Clock reloj) { this(tx, reloj, new BitacoraDao()); }

    /** Permite comprobar que una auditoría fallida revierte también el cambio de la reserva. */
    public Reservas(Transacciones tx, Clock reloj, BitacoraDao bitacora) {
        this.tx = tx; this.reloj = reloj; this.bitacora = bitacora;
    }

    /** Devuelve la fecha de negocio usada por el formulario y la validación. */
    public LocalDate hoy() { return LocalDate.now(reloj); }

    /** Lista reservas con su empresa filtrada por SQL según la cuenta vigente. */
    public List<Ficha> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> reservas.listar(con, AccesoDatos.empresaPermitida(AccesoDatos.vigente(con, actor))));
    }

    /** Consulta una reserva sin revelar las de otra empresa. */
    public Ficha consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(reservas.buscar(con, id,
                AccesoDatos.empresaPermitida(AccesoDatos.vigente(con, actor)), false)));
    }

    /** Lee los catálogos activos en tres consultas, evitando opciones bajo padres desactivados. */
    public Catalogo catalogo(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            return new Catalogo(new EmpresaDao().activas(con), new SedeDao().activas(con), new SalonDao().activos(con));
        });
    }

    /** Crea o edita una reserva conservando código, empresa, sede, salón y estado en una edición. */
    public int guardar(Cuenta actor, int id, Datos datos) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta responsable = administrador(con, actor);
            Ficha anterior = id == 0 ? null : exigir(reservas.buscar(con, id, 0, true));
            if (anterior != null && !anterior.reserva().editable()) {
                throw new IllegalArgumentException("Una reserva finalizada o cancelada no se puede editar.");
            }
            int salon = anterior == null ? datos.salon() : anterior.reserva().salon();
            Salon lugar = new SalonDao().buscar(con, salon, 0, true);
            if (lugar == null) throw new IllegalArgumentException("Selecciona un salón existente.");
            if (anterior == null) comprobarSeleccion(con, datos, lugar);
            Reserva reserva = validar(id, salon, responsable.id(), anterior, datos);
            Reserva repetida = reservas.duplicada(con, reserva);
            if (repetida != null) throw new IllegalArgumentException("Ya existe la reserva " + repetida.codigo() + " para ese salón, fecha y hora de inicio.");
            try {
                if (anterior == null) {
                    int nueva = reservas.crear(con, reserva, UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                    String codigo = String.format(Locale.ROOT, "R-%04d", nueva);
                    reservas.codigo(con, nueva, codigo);
                    bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "reserva", nueva,
                            "CREAR", null, "ESPERANDO", codigo);
                    return nueva;
                }
                reservas.editar(con, reserva);
                bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "reserva", id,
                        "EDITAR", reserva.estado(), reserva.estado(), "Actualización de los datos de la reserva.");
                return id;
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new IllegalArgumentException("Ya existe una reserva para ese salón, fecha y hora. Revisa el listado.");
            }
        });
    }

    /** Acepta exclusivamente una reserva Esperando y registra el cambio a En cola. */
    public void aceptar(Cuenta actor, int id) throws SQLException {
        cambiar(actor, id, "EN_COLA", null);
    }

    /** Cancela una reserva pendiente con motivo obligatorio y sin eliminar su historial. */
    public void cancelar(Cuenta actor, int id, String motivo) throws SQLException {
        cambiar(actor, id, "CANCELADA", Validacion.texto(motivo, "motivo de cancelación", 255));
    }

    private void cambiar(Cuenta actor, int id, String nuevo, String motivo) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta responsable = administrador(con, actor);
            Ficha ficha = exigir(reservas.buscar(con, id, 0, true));
            Reserva reserva = ficha.reserva();
            if (nuevo.equals("EN_COLA") && !reserva.estado().equals("ESPERANDO")) {
                throw new IllegalArgumentException("Solo se puede aceptar una reserva en estado Esperando.");
            }
            if (nuevo.equals("CANCELADA")) {
                if (!reserva.editable()) throw new IllegalArgumentException("Una reserva finalizada o cancelada no puede cambiar de estado.");
                if (ficha.visita() != null) {
                    throw new IllegalArgumentException("La visita ya se inició. La reserva se conserva hasta finalizarla.");
                }
            }
            reservas.estado(con, id, nuevo, motivo);
            bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "reserva", id,
                    "CAMBIAR_ESTADO", reserva.estado(), nuevo, motivo);
            return null;
        });
    }

    private Reserva validar(int id, int salon, int administrador, Ficha anterior, Datos datos) {
        if (datos.fecha() == null || datos.fecha().isBefore(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("Elige una fecha de hoy o posterior.");
        }
        if (datos.inicio() == null || datos.fin() == null || !datos.fin().withNano(0).isAfter(datos.inicio().withNano(0))) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        String categoria = Validacion.opcion(datos.categoria(), "categoría", "EVALUACION_INICIAL", "SEGUIMIENTO", "REEVALUACION");
        return new Reserva(id, salon, anterior == null ? administrador : anterior.reserva().administrador(),
                anterior == null ? "" : anterior.reserva().codigo(), datos.fecha(), datos.inicio().withNano(0), datos.fin().withNano(0),
                categoria, Validacion.opcional(datos.descripcion(), "descripción", 255), Validacion.notas(datos.notas()),
                anterior == null ? "ESPERANDO" : anterior.reserva().estado(), anterior == null ? null : anterior.reserva().motivo());
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
        Permisos.administrador(actual);
        return actual;
    }

    private static Ficha exigir(Ficha ficha) {
        if (ficha == null) throw new SecurityException("La reserva no está disponible para tu cuenta.");
        return ficha;
    }
}
