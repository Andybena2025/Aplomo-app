package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.dao.CrucesAgendaDao;
import co.aplomo.dao.MensajeDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Reserva;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * RF-002/011 (propuesta): solicitud del contacto y respuesta del ingeniero de esa cita.
 */
public final class SolicitudesVisita {
    private final Transacciones tx;
    private final Clock reloj;

    /**
     * RNF-011: comparte la transacción de cuentas, reserva y mensaje.
     */
    public SolicitudesVisita(Transacciones tx, Clock reloj) {
        this.tx = tx;
        this.reloj = reloj;
    }

    /**
     * RF-002: opción autorizada, con todos sus padres para filtrar sin revelar otras empresas.
     */
    public record Lugar(int id, int sede, int empresa, String nombre, String sedeNombre, String empresaNombre) {
    }

    /**
     * RF-002: prioridad visual de los ingenieros que ya conocen la empresa.
     */
    public record Ingeniero(int id, String nombre, String color, boolean asignado) {
    }

    /**
     * RF-002: contacto de una solicitud pendiente accesible solo al destinatario.
     */
    public record Peticion(ReservaDao.Ficha ficha, String nombre, String correo, String telefono) {
    }

    /**
     * RF-002: solo salones activos de empresas asignadas al Usuario.
     */
    public List<Lugar> lugares(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = usuario(con, actor);
            return Sql.listar(con, "SELECT l.id_salon,s.id_sede,e.id_empresa,l.nombre,s.nombre,e.nombre " +
                            "FROM salon l JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) " +
                            "JOIN cuenta_empresa ce ON ce.id_empresa=e.id_empresa " +
                            "WHERE ce.id_cuenta=? AND ce.activo=TRUE AND e.activo=TRUE AND s.activo=TRUE " +
                            "AND l.activo=TRUE ORDER BY e.nombre,s.nombre,l.nombre",
                    f -> new Lugar(f.getInt(1), f.getInt(2), f.getInt(3), f.getString(4), f.getString(5),
                            f.getString(6)), actual.id());
        });
    }

    /**
     * RF-002: cuentas activas; no se revelan sus correos ni teléfonos.
     */
    public List<Ingeniero> ingenieros(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            usuario(con, actor);
            AccesoDatos.exigirEmpresa(con, actor, empresa);
            return Sql.listar(con, "SELECT q.id_cuenta,q.nombre,q.color_agenda," +
                            "EXISTS(SELECT 1 FROM cuenta_empresa ce WHERE ce.id_cuenta=q.id_cuenta " +
                            "AND ce.id_empresa=? AND ce.activo=TRUE) asignado FROM cuenta q " +
                            "WHERE q.rol='EVALUADOR' AND q.estado='ACTIVA' ORDER BY asignado DESC,q.nombre",
                    f -> new Ingeniero(f.getInt(1), f.getString(2), f.getString(3), f.getBoolean(4)), empresa);
        });
    }

    /**
     * RF-011: cruces por ingeniero de la cita; bloqueo de la cuenta serializa solicitudes simultáneas.
     */
    public int solicitar(Cuenta actor, int salon, int ingeniero, LocalDate fecha, LocalTime inicio, LocalTime fin,
                         String motivo) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = usuario(con, actor);
            Lugar lugar = lugares(actual).stream().filter(l -> l.id() == salon).findFirst()
                    .orElseThrow(() -> new SecurityException("Acceso denegado."));
            Cuenta evaluador = new CuentaDao().porId(con, ingeniero, true);
            if (evaluador == null || evaluador.rol() != Cuenta.Rol.EVALUADOR ||
                    evaluador.estado() != Cuenta.Estado.ACTIVA) {
                throw new IllegalArgumentException("Elige un ingeniero activo.");
            }
            if (fecha == null || fecha.isBefore(LocalDate.now(reloj)) || inicio == null || fin == null ||
                    !fin.isAfter(inicio))
                throw new IllegalArgumentException("Revisa la fecha y las horas de la visita.");
            Reserva reserva = new Reserva(0, salon, actual.id(), "", fecha, inicio, fin, "EVALUACION_INICIAL",
                    Validacion.opcional(motivo, "motivo", 255), "", "ESPERANDO", null, ingeniero);
            try {
                new CrucesAgendaDao().comprobar(con, reserva);
            } catch (IllegalArgumentException error) {
                throw new Cruce(evaluador.nombre());
            }
            ReservaDao dao = new ReservaDao();
            if (dao.duplicada(con, reserva) != null) {
                throw new IllegalArgumentException("Este salón ya tiene una reserva a esa hora. Elige otra hora.");
            }
            int id = dao.crear(con, reserva, UUID.randomUUID().toString().substring(0, 12));
            dao.codigo(con, id, "R-" + String.format(java.util.Locale.ROOT, "%04d", id));
            new MensajeDao().enviar(con, id, actual.id(), ingeniero, "SOLICITUD_VISITA",
                    "Solicita una visita: " + actual.nombre() + ". " + actual.telefono() + " · " + actual.correo(),
                    LocalDateTime.now(reloj));
            new BitacoraDao().registrar(con, actual.id(), LocalDateTime.now(reloj), "reserva", id, "CREAR", null,
                    "ESPERANDO", "Solicitud de visita de " + lugar.empresaNombre());
            return id;
        });
    }

    /**
     * RF-011: alternativas sin ocupar la agenda ni guardar una solicitud rechazada por cruce.
     */
    public boolean libre(Cuenta actor, int ingeniero, LocalDate fecha, LocalTime inicio,
                         LocalTime fin) throws SQLException {
        return tx.ejecutar(con -> {
            usuario(con, actor);
            return Sql.uno(con, "SELECT COUNT(*) FROM reserva WHERE id_evaluador=? AND fecha=? " +
                            "AND estado<>'CANCELADA' AND hora_inicio<? AND hora_fin>?", f -> f.getInt(1), ingeniero,
                    fecha, fin,
                    inicio) == 0;
        });
    }

    /**
     * RF-011/002: horarios de un Evaluador activo; solo Usuario con lugares propios disponibles.
     */
    public List<LocalTime> horas(Cuenta actor, int ingeniero, LocalDate fecha, int duracion, Integer salon)
            throws SQLException {
        var lugares = lugares(actor);
        if (lugares.isEmpty() || salon != null && lugares.stream().noneMatch(l -> l.id() == salon))
            throw new SecurityException("Acceso denegado.");
        if (ingenieros(actor, lugares.get(0).empresa()).stream().noneMatch(i -> i.id() == ingeniero))
            throw new SecurityException("Acceso denegado.");
        if (fecha == null || fecha.isBefore(hoy()) || duracion < 1 || duracion > 3)
            throw new IllegalArgumentException("Revisa el día y la duración.");
        var horas = new java.util.ArrayList<LocalTime>();
        for (LocalTime inicio = LocalTime.of(8, 0); !inicio.plusHours(duracion).isAfter(LocalTime.of(18, 0));
             inicio = inicio.plusMinutes(30)) {
            if (libre(actor, ingeniero, fecha, inicio, inicio.plusHours(duracion))) horas.add(inicio);
        }
        return List.copyOf(horas);
    }

    /**
     * RF-011: fecha local del mismo reloj que valida la solicitud.
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj.withZone(java.time.ZoneId.of("America/Bogota")));
    }

    /**
     * RF-002: acceso mínimo a la petición recibida, incluso antes de asignarle la empresa.
     */
    public Peticion consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            ReservaDao.Ficha ficha = new ReservaDao().buscar(con, id, 0, false);
            if (ficha == null || !java.util.Objects.equals(ficha.reserva().evaluador(), actual.id()) ||
                    actual.rol() != Cuenta.Rol.EVALUADOR) throw new SecurityException("Acceso denegado.");
            Cuenta contacto = new CuentaDao().porId(con, ficha.reserva().administrador(), false);
            if (contacto.rol() != Cuenta.Rol.USUARIO) throw new SecurityException("Acceso denegado.");
            return new Peticion(ficha, contacto.nombre(), contacto.correo(), contacto.telefono());
        });
    }

    /**
     * RF-002/012: acepta y asigna; rechaza con motivo o solicita contacto sin alterar el estado.
     */
    public void actuar(Cuenta actor, int id, String accion, String texto) throws SQLException {
        tx.ejecutar(con -> {
            Peticion peticion = consultar(actor, id);
            Cuenta actual = AccesoDatos.vigente(con, actor);
            ReservaDao.Ficha ficha = new ReservaDao().buscar(con, id, 0, true);
            Reserva reserva = ficha.reserva();
            if (!reserva.estado().equals("ESPERANDO")) {
                throw new IllegalArgumentException("Esta solicitud ya fue atendida.");
            }
            String tipo;
            String aviso;
            switch (accion) {
                case "aceptar" -> {
                    new CrucesAgendaDao().comprobar(con, reserva);
                    var asignaciones = new java.util.HashSet<>(new CuentaEmpresaDao().ids(con, actual.id()));
                    asignaciones.add(ficha.empresa());
                    new CuentaEmpresaDao().sincronizar(con, actual.id(), actual.id(), asignaciones, false);
                    new ReservaDao().estado(con, id, "EN_COLA", null);
                    tipo = "AVISO";
                    aviso = "Tu visita fue aceptada.";
                }
                case "rechazar" -> {
                    aviso = Validacion.texto(texto, "motivo de rechazo", 255);
                    new ReservaDao().estado(con, id, "CANCELADA", aviso);
                    aviso = "Tu visita fue rechazada: " + aviso;
                    tipo = "AVISO";
                }
                case "llamada" -> {
                    tipo = "PEDIR_LLAMADA";
                    aviso = "El Ing. te llamará pronto.";
                }
                case "informacion" -> {
                    tipo = "PEDIR_INFORMACION";
                    aviso = Validacion.texto(texto, "información que necesitas", 500);
                }
                default -> throw new IllegalArgumentException("Elige una acción disponible.");
            }
            new MensajeDao().enviar(con, id, actual.id(), reserva.administrador(), tipo, aviso,
                    LocalDateTime.now(reloj));
            new BitacoraDao().registrar(con, actual.id(), LocalDateTime.now(reloj), "reserva", id, "EDITAR",
                    reserva.estado(), null, "Solicitud de visita: " + accion);
            return null;
        });
    }

    private static Cuenta usuario(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        if (actual.rol() != Cuenta.Rol.USUARIO) throw new SecurityException("Acceso denegado.");
        return actual;
    }

    /**
     * RF-011: error de negocio identificable para ofrecer ambas alternativas con los datos conservados.
     */
    public static final class Cruce extends IllegalArgumentException {
        public Cruce(String nombre) {
            super("El Ing. " + nombre + " ya tiene un compromiso a esa hora.");
        }
    }
}
