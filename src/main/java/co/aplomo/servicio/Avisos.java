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
     * RF-002 (propuesta): una respuesta por petición de información, protegida por bloqueo transaccional.
     */
    public void responder(Cuenta actor, int id, String texto) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            Mensaje mensaje = dao.exigir(con, id, actual.id());
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
}
