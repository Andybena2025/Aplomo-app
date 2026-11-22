package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.MensajeDao;
import co.aplomo.dao.MensajeDao.Mensaje;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.AccesoDatos;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF-002/033 (propuesta): los tres roles solo acceden a sus propios avisos.
 */
public final class Avisos {
    private final Transacciones tx;
    private final Clock reloj;
    private final MensajeDao dao = new MensajeDao();

    /**
     * RNF-011: comparte transacciones y reloj con las reservas.
     */
    public Avisos(Transacciones tx, Clock reloj) {
        this.tx = tx;
        this.reloj = reloj;
    }

    /**
     * RF-002/033: el destinatario principal se resuelve en el servidor desde la configuración privada.
     */
    public Cuenta principal(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            AccesoDatos.vigente(con, actor);
            return principalEn(con);
        });
    }

    /**
     * RF-002/033: cualquier rol vigente escribe solo a la Administradora configurada.
     */
    public void escribirPrincipal(Cuenta actor, String texto) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            Cuenta destino = principalEn(con);
            dao.enviar(con, null, actual.id(), destino.id(), "AVISO", Validacion.texto(texto, "mensaje", 500),
                    LocalDateTime.now(reloj));
            return null;
        });
    }

    /**
     * RF-033: el Administrador solo abre conversación general con un Evaluador activo.
     */
    public Cuenta evaluador(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            co.aplomo.seguridad.Permisos.administrador(AccesoDatos.vigente(con, actor));
            return evaluadorEn(con, id);
        });
    }

    /**
     * RF-033: aviso general sin cita y con destinatario validado y bitácora en la misma transacción.
     */
    public void escribirEvaluador(Cuenta actor, int id, String texto) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta cuenta = AccesoDatos.vigente(con, actor);
            co.aplomo.seguridad.Permisos.administrador(cuenta);
            enviarEquipo(con, cuenta, evaluadorEn(con, id), "AVISO", texto);
            return null;
        });
    }

    /**
     * RF-033: conversación general autorizada del Administrador con ese Evaluador activo.
     */
    public List<Mensaje> conversacionEvaluador(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta cuenta = AccesoDatos.vigente(con, actor);
            co.aplomo.seguridad.Permisos.administrador(cuenta);
            evaluadorEn(con, id);
            return dao.general(con, cuenta.id(), id);
        });
    }

    /**
     * RF-033: solo acusa las respuestas generales dirigidas al Administrador que abrió el panel.
     */
    public void leerEvaluador(Cuenta actor, int id) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta cuenta = AccesoDatos.vigente(con, actor);
            co.aplomo.seguridad.Permisos.administrador(cuenta);
            evaluadorEn(con, id);
            for (Mensaje mensaje : dao.general(con, cuenta.id(), id)) {
                if (mensaje.destinatario() == cuenta.id()) dao.entender(con, mensaje);
            }
            return null;
        });
    }

    private static Cuenta evaluadorEn(java.sql.Connection con, int id) throws SQLException {
        Cuenta cuenta = new co.aplomo.dao.CuentaDao().porId(con, id, true);
        if (cuenta == null || cuenta.rol() != Cuenta.Rol.EVALUADOR || cuenta.estado() != Cuenta.Estado.ACTIVA)
            throw new SecurityException("El evaluador no está disponible.");
        return cuenta;
    }

    private void enviarEquipo(java.sql.Connection con, Cuenta actor, Cuenta destino, String tipo,
            String texto) throws SQLException {
        int id = dao.enviar(con, null, actor.id(), destino.id(), tipo, Validacion.texto(texto, "mensaje", 500),
                LocalDateTime.now(reloj));
        new BitacoraDao().registrar(con, actor.id(), LocalDateTime.now(reloj), "mensaje", id, "CREAR", null, null,
                "Mensaje del equipo");
    }

    static Cuenta principalEn(java.sql.Connection con) throws SQLException {
        var contacto = co.aplomo.config.Contacto.leer(
                java.nio.file.Path.of(System.getProperty("aplomo.contacto.archivo", "config/contacto.properties")));
        Integer id = co.aplomo.db.Sql.uno(con,
                "SELECT id_cuenta FROM cuenta WHERE rol='ADMINISTRADOR' AND estado='ACTIVA' " +
                        "ORDER BY (correo=?) DESC, " +
                        "(LOWER(nombre) LIKE ? OR LOWER(nombre) LIKE ?) DESC,id_cuenta LIMIT 1", f -> f.getInt(1),
                contacto.correo(), "%gabriela%", "%gabriella%");
        if (id == null) throw new IllegalArgumentException("No está disponible el contacto de la Administradora.");
        return new co.aplomo.dao.CuentaDao().porId(con, id, false);
    }

    /**
     * RF-002: la sesión se renueva en el servidor, nunca se acepta un destinatario del navegador.
     */
    public List<Mensaje> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> dao.listar(con, AccesoDatos.vigente(con, actor).id()));
    }

    /**
     * RF-033: lectura explícita; una consulta de sondeo nunca acusa recibo.
     */
    public void entender(Cuenta actor, int id) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            dao.entender(con, dao.exigir(con, id, actual.id()));
            return null;
        });
    }

    /**
     * RF-033/002: respuestas del equipo y una respuesta por petición de información del Usuario, bajo transacción.
     */
    public void responder(Cuenta actor, int id, String texto) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            Mensaje mensaje = dao.exigir(con, id, actual.id());
            if (actual.rol() != Cuenta.Rol.USUARIO) {
                responderEquipoEn(con, actual, mensaje, texto);
                return null;
            }
            if (actual.rol() != Cuenta.Rol.USUARIO || !mensaje.tipo().equals("PEDIR_INFORMACION")) {
                throw new SecurityException("Acceso denegado.");
            }
            if (mensaje.respondido()) throw new IllegalArgumentException("Ya respondiste este mensaje.");
            String respuesta = Validacion.texto(texto, "respuesta", 500);
            LocalDateTime ahora = LocalDateTime.now(reloj);
            dao.enviar(con, mensaje.reserva(), actual.id(), mensaje.remitente(), "RESPUESTA", respuesta, ahora);
            new BitacoraDao().registrar(con, actual.id(), ahora, "mensaje", id, "EDITAR", null, null,
                    "Respuesta enviada");
            dao.entender(con, mensaje);
            return null;
        });
    }

    private void responderEquipoEn(java.sql.Connection con, Cuenta actor, Mensaje mensaje,
            String texto) throws SQLException {
        Cuenta origen = new co.aplomo.dao.CuentaDao().porId(con, mensaje.remitente(), true);
        boolean evaluador = actor.rol() == Cuenta.Rol.EVALUADOR && origen.rol() == Cuenta.Rol.ADMINISTRADOR &&
                (mensaje.tipo().equals("AVISO") || mensaje.tipo().equals("NOTA_ADMIN"));
        boolean administrador = actor.rol() == Cuenta.Rol.ADMINISTRADOR && origen.rol() == Cuenta.Rol.EVALUADOR &&
                mensaje.tipo().equals("RESPUESTA");
        if (!evaluador && !administrador) throw new SecurityException("Acceso denegado.");
        if (mensaje.reserva() != null) {
            var agenda = new Agenda(tx, reloj);
            if (administrador) {
                var ficha = new co.aplomo.dao.ReservaDao().buscar(con, mensaje.reserva(),
                        AccesoDatos.empresaPermitida(actor), true);
                if (ficha == null || !java.util.Objects.equals(ficha.reserva().evaluador(), origen.id()))
                    throw new SecurityException("Acceso denegado.");
                agenda.guardarNotaEn(con, actor, mensaje.reserva(), texto);
            } else agenda.responderEn(con, actor, mensaje.reserva(), texto);
        } else {
            if (administrador) evaluadorEn(con, origen.id());
            enviarEquipo(con, actor, origen, administrador ? "AVISO" : "RESPUESTA", texto);
        }
        dao.entender(con, mensaje);
        new BitacoraDao().registrar(con, actor.id(), LocalDateTime.now(reloj), "mensaje", mensaje.id(), "EDITAR", null,
                null, "Respuesta enviada");
    }

}
