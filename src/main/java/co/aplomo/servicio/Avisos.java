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
            enviarEquipo(con, actual, destino, "AVISO", texto);
            return null;
        });
    }

    /**
     * RF-033: opción mínima de destinatario; nunca se entrega la cuenta ni su contraseña al navegador.
     */
    public record Destinatario(int id, String nombre, boolean principal) {
    }

    /**
     * RF-033: destinos activos por rol; Usuario ve sus Evaluadores y la Administradora principal.
     */
    public List<Destinatario> destinatarios(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            Cuenta principal = actual.rol() == Cuenta.Rol.ADMINISTRADOR ? null : principalEn(con);
            if (actual.rol() == Cuenta.Rol.USUARIO) {
                var destinos = new java.util.ArrayList<Destinatario>(co.aplomo.db.Sql.listar(con,
                        "SELECT DISTINCT q.id_cuenta,q.nombre FROM cuenta q JOIN cuenta_empresa ev " +
                                "ON ev.id_cuenta=q.id_cuenta JOIN cuenta_empresa u ON u.id_empresa=ev.id_empresa " +
                                "WHERE u.id_cuenta=? AND u.activo=TRUE AND ev.activo=TRUE " +
                                "AND q.rol='EVALUADOR' AND q.estado='ACTIVA' ORDER BY q.nombre,q.id_cuenta",
                        f -> new Destinatario(f.getInt(1), f.getString(2), false), actual.id()));
                destinos.add(new Destinatario(principal.id(), principal.nombre(), true));
                return List.copyOf(destinos);
            }
            String rol = actual.rol() == Cuenta.Rol.ADMINISTRADOR ? "EVALUADOR" : "ADMINISTRADOR";
            int primero = principal == null ? 0 : principal.id();
            return co.aplomo.db.Sql.listar(con, "SELECT id_cuenta,nombre FROM cuenta WHERE estado='ACTIVA' AND rol=? " +
                            "ORDER BY (id_cuenta=?) DESC,nombre,id_cuenta",
                    f -> new Destinatario(f.getInt(1), f.getString(2), f.getInt(1) == primero), rol, primero);
        });
    }

    /**
     * RF-033/002: datos de contacto mínimos, exclusivamente para sus destinos autorizados.
     */
    public record ContactoEquipo(int id, String nombre, String cargo, String correo, String telefono,
                                 String color, boolean principal) {
    }

    /**
     * RF-033/002: solo Usuario vigente; jamás se leen contactos de Evaluadores de otra empresa.
     */
    public List<ContactoEquipo> contactos(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            if (actual.rol() != Cuenta.Rol.USUARIO) throw new SecurityException("Acceso denegado.");
            var contactos = new java.util.ArrayList<ContactoEquipo>();
            for (Destinatario destino : destinatarios(actual)) {
                ContactoEquipo contacto = co.aplomo.db.Sql.uno(con,
                        "SELECT id_cuenta,nombre,cargo,correo,telefono,color_agenda FROM cuenta " +
                                "WHERE id_cuenta=? AND estado='ACTIVA'", f -> new ContactoEquipo(f.getInt(1),
                                f.getString(2), f.getString(3), f.getString(4), f.getString(5),
                                f.getString(6), destino.principal()), destino.id());
                if (contacto != null) contactos.add(contacto);
            }
            return List.copyOf(contactos);
        });
    }

    /**
     * RF-033: mensaje nuevo con destino validado por rol y asignación vigente del Usuario.
     */
    public Cuenta enviarNuevo(Cuenta actor, Integer para, String texto) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            Cuenta destino;
            if (actual.rol() == Cuenta.Rol.USUARIO) {
                if (para == null || destinatarios(actual).stream().noneMatch(d -> d.id() == para))
                    throw new SecurityException("El destinatario no está disponible.");
                destino = new co.aplomo.dao.CuentaDao().porId(con, para, true);
                if (destino == null || destino.estado() != Cuenta.Estado.ACTIVA)
                    throw new SecurityException("El destinatario no está disponible.");
            } else if (actual.rol() == Cuenta.Rol.ADMINISTRADOR) {
                if (para == null) throw new SecurityException("Elige un evaluador activo.");
                destino = evaluadorEn(con, para);
            } else {
                destino = para == null ? null : new co.aplomo.dao.CuentaDao().porId(con, para, true);
                if (destino == null || destino.rol() != Cuenta.Rol.ADMINISTRADOR ||
                        destino.estado() != Cuenta.Estado.ACTIVA)
                    throw new SecurityException("El destinatario no está disponible.");
            }
            enviarEquipo(con, actual, destino, "AVISO", texto);
            return destino;
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
                        "ORDER BY (correo=?) DESC,(correo='gabby@rula.local') DESC, " +
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
            if (actual.rol() != Cuenta.Rol.USUARIO || mensaje.reserva() == null &&
                    List.of("AVISO", "RESPUESTA").contains(mensaje.tipo())) {
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
        boolean parejaUsuario = mensaje.reserva() == null && List.of("AVISO", "RESPUESTA").contains(mensaje.tipo()) &&
                ((actor.rol() == Cuenta.Rol.USUARIO &&
                        destinatarios(actor).stream().anyMatch(d -> d.id() == origen.id())) ||
                        (actor.rol() == Cuenta.Rol.EVALUADOR && origen.rol() == Cuenta.Rol.USUARIO &&
                                destinatarios(origen).stream().anyMatch(d -> d.id() == actor.id())) ||
                        (actor.rol() == Cuenta.Rol.ADMINISTRADOR && origen.rol() == Cuenta.Rol.USUARIO &&
                                principalEn(con).id() == actor.id()));
        if (parejaUsuario) {
            if (origen.estado() != Cuenta.Estado.ACTIVA) throw new SecurityException("Acceso denegado.");
            enviarEquipo(con, actor, origen, "RESPUESTA", texto);
            dao.entender(con, mensaje);
            return;
        }
        boolean evaluador = actor.rol() == Cuenta.Rol.EVALUADOR && origen.rol() == Cuenta.Rol.ADMINISTRADOR &&
                (mensaje.tipo().equals("AVISO") || mensaje.tipo().equals("NOTA_ADMIN"));
        boolean administrador = actor.rol() == Cuenta.Rol.ADMINISTRADOR && origen.rol() == Cuenta.Rol.EVALUADOR &&
                (mensaje.tipo().equals("RESPUESTA") || mensaje.tipo().equals("AVISO") && mensaje.reserva() == null);
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
