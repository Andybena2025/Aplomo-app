package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.dao.MensajeDao;
import co.aplomo.dao.SolicitudAccesoDao;
import co.aplomo.dao.SolicitudAccesoDao.Solicitud;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.PropiedadEmpresa;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * RF-002/003 (propuesta): ninguna cuenta entra sin aprobación de la Administradora.
 */
public final class SolicitudesAcceso {
    private final Transacciones tx;
    private final Clock reloj;
    private final SolicitudAccesoDao dao = new SolicitudAccesoDao();
    private final LimiteSolicitudes limite;

    /**
     * RNF-011: transacciones y reloj sustituibles; límite acotado y local al servicio.
     */
    public SolicitudesAcceso(Transacciones tx, Clock reloj) {
        this.tx = tx;
        this.reloj = reloj;
        this.limite = new LimiteSolicitudes(reloj);
    }

    /**
     * RF-003: valores reales de la persona; sin rol, estado ni empresa autorizada recibidos del cliente.
     */
    public record Datos(String nombre, String cargo, String correo, String telefono, String empresa, String nit,
                        String ciudad, String sector, String necesidad) {
        public Datos {
            nombre = Validacion.texto(nombre, "nombre", 100);
            cargo = Validacion.texto(cargo, "cargo", 80);
            correo = Validacion.correo(Validacion.texto(correo, "correo", 120));
            telefono = Validacion.texto(telefono, "teléfono", 20);
            if (!telefono.matches("[+0-9 ()-]{7,20}")) {
                throw new IllegalArgumentException("Escribe un teléfono de contacto válido.");
            }
            empresa = Validacion.texto(empresa, "empresa", 120);
            nit = Validacion.texto(nit, "NIT", 20).replaceAll("\\s+", "");
            ciudad = Validacion.texto(ciudad, "ciudad", 60);
            sector = Validacion.opcional(sector, "sector", 60);
            necesidad = Validacion.opcional(necesidad, "qué necesitas", 500);
        }
    }

    /**
     * RF-001/003: clave efímera devuelta una sola vez; nunca persiste ni aparece en toString.
     */
    public record Aprobacion(int cuenta, int empresa, String correo, String temporal, String telefono) {
        @Override
        public String toString() {
            return "Aprobacion[cuenta=" + cuenta + ", empresa=" + empresa + "]";
        }
    }

    /**
     * RNF-004: limita los POST públicos antes de procesar los campos del formulario.
     */
    public void limitar(String direccion) {
        limite.comprobar(direccion);
    }

    /**
     * RF-003: solicitud pública; no crea empresa ni cuenta hasta que se apruebe.
     */
    public int solicitar(Datos datos) throws SQLException {
        return tx.ejecutar(con -> registrar(con, datos, null, null));
    }

    /**
     * RF-003: el dueño E registra empresa y solicitud del contacto en una sola transacción.
     */
    public Empresas.Guardado guardarEmpresa(Cuenta actor, Empresa empresa, String cargo,
            String ciudad) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = AccesoDatos.vigente(con, actor);
            Datos contacto = null;
            if (actual.rol() == Cuenta.Rol.EVALUADOR && empresa.id() == 0 &&
                    empresa.contacto() != null && !empresa.contacto().isBlank()) {
                contacto = new Datos(empresa.contacto(), cargo, empresa.correo(), empresa.telefono(), empresa.nombre(),
                        empresa.nit(), ciudad, empresa.sector(), "");
            }
            var guardado = new Empresas(tx).guardarEn(con, actual, empresa);
            if (contacto != null) {
                registrar(con, contacto, guardado.id(), actual.id());
                return new Empresas.Guardado(guardado.id(),
                        "Empresa guardada. El acceso del contacto está pendiente de aprobación.");
            }
            return guardado;
        });
    }

    /**
     * RF-002: solamente Administrador vigente consulta solicitudes externas.
     */
    public List<Solicitud> pendientes(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            return dao.pendientes(con);
        });
    }

    /**
     * RF-002: ningún dato de solicitud se entrega al Evaluador ni al Usuario.
     */
    public Solicitud consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            return exigir(con, id, false);
        });
    }

    /**
     * RF-003: permite asignar un E activo al aprobar, o dejar la empresa sin ingeniero.
     */
    public List<Agenda.Evaluador> evaluadores(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            return new co.aplomo.dao.AgendaDao().evaluadores(con).stream().filter(Agenda.Evaluador::activo).toList();
        });
    }

    /**
     * RF-001/003: empresa, cuenta temporal, asignación y resolución se confirman juntas.
     */
    public Aprobacion aprobar(Cuenta actor, int id, Integer evaluador) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta actual = administrador(con, actor);
            Solicitud s = exigir(con, id, true);
            pendiente(s);
            if (evaluador != null) {
                Cuenta ingeniero = new CuentaDao().porId(con, evaluador, true);
                if (ingeniero == null || ingeniero.rol() != Cuenta.Rol.EVALUADOR ||
                        ingeniero.estado() != Cuenta.Estado.ACTIVA) {
                    throw new IllegalArgumentException("Elige un Evaluador activo o Sin ingeniero.");
                }
            }
            int empresa;
            if (s.empresaId() == null) {
                empresa = new Empresas(tx).guardarEn(con, actual,
                        new Empresa(0, actual.id(), s.empresa(), s.nit(), "", s.telefono(), s.correo(), s.nombre(),
                                true, actual.id(), s.sector())).id();
            } else {
                var existente = new EmpresaDao().buscar(con, s.empresaId(), 0, true);
                if (existente == null || !existente.activo()) {
                    throw new IllegalArgumentException("Reactiva la empresa antes de aprobar el acceso.");
                }
                empresa = existente.id();
            }
            String temporal = co.aplomo.instalacion.CrearCuentas.temporal();
            int cuenta = new Usuarios(tx, new CuentaDao(), new EmpresaDao()).crearEn(con, actual,
                    new Usuarios.Datos(s.nombre(), s.correo(), s.telefono(), s.cargo(), Cuenta.Rol.USUARIO, empresa),
                    temporal);
            if (evaluador != null) {
                var empresas = new HashSet<>(new CuentaEmpresaDao().ids(con, evaluador));
                empresas.add(empresa);
                new CuentaEmpresaDao().sincronizar(con, actual.id(), evaluador, empresas, false);
            }
            dao.resolver(con, id, actual.id(), "APROBADA", empresa, null, LocalDateTime.now(reloj));
            auditar(con, actual.id(), id, "APROBADA");
            return new Aprobacion(cuenta, empresa, s.correo(), temporal, s.telefono());
        });
    }

    /**
     * RF-003/005: rechazo obligatorio con motivo; no crea ni modifica cuentas o empresas.
     */
    public void rechazar(Cuenta actor, int id, String motivo) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta actual = administrador(con, actor);
            Solicitud s = exigir(con, id, true);
            pendiente(s);
            dao.resolver(con, id, actual.id(), "RECHAZADA", s.empresaId(),
                    Validacion.texto(motivo, "motivo de rechazo", 255), LocalDateTime.now(reloj));
            auditar(con, actual.id(), id, "RECHAZADA");
            return null;
        });
    }

    private int registrar(Connection con, Datos d, Integer empresa, Integer solicitante) throws SQLException {
        int id = dao.crear(con,
                new Solicitud(0, d.nombre(), d.cargo(), d.correo(), d.telefono(), d.empresa(), d.nit(), d.ciudad(),
                        d.sector(), d.necesidad(), "PENDIENTE", LocalDateTime.now(reloj), empresa, solicitante));
        List<Integer> administradores = Sql.listar(con,
                "SELECT id_cuenta FROM cuenta " + "WHERE rol='ADMINISTRADOR' AND estado='ACTIVA' ORDER BY id_cuenta",
                f -> f.getInt(1));
        if (administradores.isEmpty()) throw new IllegalStateException("No hay Administrador disponible para revisar.");
        int remitente = solicitante == null ? administradores.get(0) : solicitante;
        for (int administrador : administradores) {
            int mensaje = new MensajeDao().enviar(con, null, remitente, administrador, "SOLICITUD_ACCESO",
                    "Solicitud de " + d.nombre() + " · " + d.empresa() + " · " + d.correo(), LocalDateTime.now(reloj));
            new BitacoraDao().registrar(con, remitente, LocalDateTime.now(reloj), "mensaje", mensaje, "CREAR", null,
                    null, "ACCESO:" + id);
        }
        return id;
    }

    private Solicitud exigir(Connection con, int id, boolean bloquear) throws SQLException {
        Solicitud s = dao.buscar(con, id, bloquear);
        if (s == null) throw new SecurityException("La solicitud no está disponible para tu cuenta.");
        return s;
    }

    private void auditar(Connection con, int actor, int id, String estado) throws SQLException {
        new BitacoraDao().registrar(con, actor, LocalDateTime.now(reloj), "solicitud_acceso", id, "CAMBIAR_ESTADO",
                "PENDIENTE", estado, "Solicitud revisada");
    }

    private static void pendiente(Solicitud s) {
        if (!s.estado().equals("PENDIENTE")) throw new IllegalArgumentException("Esta solicitud ya fue revisada.");
    }

    private static Cuenta administrador(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        Permisos.administrador(actual);
        return actual;
    }
}
