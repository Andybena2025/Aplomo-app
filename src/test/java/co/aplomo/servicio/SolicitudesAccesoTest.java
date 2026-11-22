package co.aplomo.servicio;

import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-001/002/003: solicitudes y decisiones atómicas en MySQL de pruebas, con ROLLBACK.
 */
class SolicitudesAccesoTest extends BasePrueba {
    private SolicitudesAcceso servicio() {
        return new SolicitudesAcceso(tx, reloj);
    }

    private SolicitudesAcceso.Datos datos() {
        return new SolicitudesAcceso.Datos("Persona solicitante", "Coordinación", UUID.randomUUID() + "@prueba.invalid",
                "3005556789", "Empresa nueva de ensayo", UUID.randomUUID().toString().substring(0, 20), "Armenia",
                "Educación", "Revisar los puestos de trabajo");
    }

    @Test
    void solicitarNoCreaCuentaNiEmpresaYNotificaSoloAdministradores() throws Exception {
        var datos = datos();
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        int id = servicio().solicitar(datos);
        assertNull(dao.porCorreo(con, datos.correo()));
        assertEquals(Integer.valueOf(antes), Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
        assertEquals("PENDIENTE", servicio().consultar(cuenta(admin), id).estado());
        assertTrue(new Avisos(tx, reloj).listar(cuenta(admin)).stream()
                .anyMatch(m -> Integer.valueOf(id).equals(m.solicitudAcceso())));
    }

    @Test
    void aprobarCreaEmpresaUsuarioTemporalSectorYCiudadSinInventarSedes() throws Exception {
        var datos = datos();
        int id = servicio().solicitar(datos);
        var aprobacion = servicio().aprobar(cuenta(admin), id, null);
        Cuenta cuenta = dao.porCorreo(con, datos.correo());
        assertEquals(Cuenta.Rol.USUARIO, cuenta.rol());
        assertTrue(cuenta.temporal());
        assertTrue(aprobacion.temporal().length() >= 12);
        assertDoesNotThrow(() -> Contrasenas.validar(aprobacion.temporal()));
        assertTrue(Contrasenas.verificar(aprobacion.temporal(), cuenta.hash()));
        assertFalse(aprobacion.toString().contains(aprobacion.temporal()));
        assertEquals("APROBADA", servicio().consultar(cuenta(admin), id).estado());
        assertEquals("Educación", new Empresas(tx).empresa(cuenta(admin), aprobacion.empresa()).sector());
        assertTrue(new Empresas(tx).resumen(cuenta(admin)).stream()
                .anyMatch(r -> r.empresa().id() == aprobacion.empresa() && r.ciudad().equals("Armenia")));
        assertEquals(Integer.valueOf(0),
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM sede WHERE id_empresa=?", f -> f.getInt(1),
                        aprobacion.empresa()));
        assertThrows(IllegalArgumentException.class, () -> servicio().aprobar(cuenta(admin), id, null));
    }

    @Test
    void aprobarConIngenieroAsignaSinQuitarSusOtrasEmpresas() throws Exception {
        int evaluador = evaluadorTrabajo().id();
        int id = servicio().solicitar(datos());
        var aprobacion = servicio().aprobar(cuenta(admin), id, evaluador);
        assertTrue(new CuentaEmpresaDao().ids(con, evaluador).contains(empresa));
        assertTrue(new CuentaEmpresaDao().ids(con, evaluador).contains(aprobacion.empresa()));
    }

    @Test
    void rechazarNoCreaNadaYExigeMotivo() throws Exception {
        var datos = datos();
        int id = servicio().solicitar(datos);
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        assertThrows(IllegalArgumentException.class, () -> servicio().rechazar(cuenta(admin), id, ""));
        servicio().rechazar(cuenta(admin), id, "Faltan datos para contactar a la empresa.");
        assertEquals("RECHAZADA", servicio().consultar(cuenta(admin), id).estado());
        assertNull(dao.porCorreo(con, datos.correo()));
        assertEquals(Integer.valueOf(antes), Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
    }

    @Test
    void correoDuplicadoRevierteEmpresaYResolucion() throws Exception {
        var original = datos();
        var datos = new SolicitudesAcceso.Datos(original.nombre(), original.cargo(), cuenta(admin).correo(),
                original.telefono(), original.empresa(), original.nit(), original.ciudad(), original.sector(), "");
        int id = servicio().solicitar(datos);
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        assertThrows(IllegalArgumentException.class, () -> servicio().aprobar(cuenta(admin), id, null));
        assertEquals(Integer.valueOf(antes), Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
        assertEquals("PENDIENTE", servicio().consultar(cuenta(admin), id).estado());
    }

    @Test
    void usuarioYEvaluadorNoConsultanNiApruebanNiRechazan() throws Exception {
        int id = servicio().solicitar(datos());
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        for (Cuenta actor : new Cuenta[]{cuenta(usuario), evaluadorTrabajo()}) {
            assertThrows(SecurityException.class, () -> servicio().consultar(actor, id));
            assertThrows(SecurityException.class, () -> servicio().aprobar(actor, id, null));
            assertThrows(SecurityException.class, () -> servicio().rechazar(actor, id, "Motivo"));
        }
    }

    @Test
    void contactoDelDuenoQuedaPendienteSinPoderIngresar() throws Exception {
        var datos = datos();
        var evaluador = evaluadorTrabajo();
        var guardado = servicio().guardarEmpresa(evaluador,
                new Empresa(0, 0, datos.empresa(), datos.nit(), "", datos.telefono(), datos.correo(), datos.nombre(),
                        true, null, datos.sector()), datos.cargo(), datos.ciudad());
        assertNull(dao.porCorreo(con, datos.correo()));
        assertNull(auth.ingresar(datos.correo(), "Cualquier2026"));
        var s = servicio().pendientes(cuenta(admin)).stream().filter(sol -> sol.correo().equals(datos.correo()))
                .findFirst().orElseThrow();
        assertEquals(Integer.valueOf(guardado.id()), s.empresaId());
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        var aprobacion = servicio().aprobar(cuenta(admin), s.id(), null);
        assertEquals(guardado.id(), aprobacion.empresa());
        assertEquals(Integer.valueOf(antes), Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
        assertTrue(dao.porCorreo(con, datos.correo()).temporal());
        assertEquals(Integer.valueOf(evaluador.id()), new Empresas(tx).empresa(evaluador, guardado.id()).creador());
    }

    @Test
    void contactoIncompletoRevierteTambienLaEmpresaDelEvaluador() throws Exception {
        var datos = datos();
        var evaluador = evaluadorTrabajo();
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        assertThrows(IllegalArgumentException.class, () -> servicio().guardarEmpresa(evaluador,
                new Empresa(0, 0, datos.empresa(), datos.nit(), "", datos.telefono(), datos.correo(), datos.nombre(),
                        true, null, datos.sector()), "", datos.ciudad()));
        assertEquals(Integer.valueOf(antes), Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
    }

    @Test
    void limiteAdmiteCincoPorHoraYSeLiberaSinEsperar() {
        var limite = new LimiteSolicitudes(reloj);
        for (int i = 0; i < 5; i++) assertDoesNotThrow(() -> limite.comprobar("conexion-a"));
        assertThrows(LimiteSolicitudes.Limite.class, () -> limite.comprobar("conexion-a"));
        assertDoesNotThrow(() -> limite.comprobar("conexion-b"));
        reloj.avanzar(Duration.ofHours(1));
        assertDoesNotThrow(() -> limite.comprobar("conexion-a"));
    }

    @Test
    void formularioRechazaCorreoTelefonoYCamposObligatorios() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitudesAcceso.Datos("", "Cargo", "a@b.co", "3005556789", "Empresa", "NIT", "Armenia", "",
                        ""));
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitudesAcceso.Datos("Ana", "Cargo", "correo", "3005556789", "Empresa", "NIT", "Armenia",
                        "", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new SolicitudesAcceso.Datos("Ana", "Cargo", "a@b.co", "javascript:123", "Empresa", "NIT",
                        "Armenia", "", ""));
    }
    @Test
    void correoDeEmpresaSinContactoNoCreaSolicitudDeCuenta() throws Exception {
        var d = datos();
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM solicitud_acceso", f -> f.getInt(1));
        var empresa = servicio().guardarEmpresa(evaluadorTrabajo(), new Empresa(0, 0, d.empresa(), d.nit(), "",
                d.telefono(), d.correo(), "", true, null, d.sector()), "", "");
        assertTrue(empresa.id() > 0);
        assertNull(dao.porCorreo(con, d.correo()));
        assertEquals(Integer.valueOf(antes), Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM solicitud_acceso", f -> f.getInt(1)));
    }


    private SolicitudesAcceso.Datos evaluador(String correo, String organizacion) {
        return new SolicitudesAcceso.Datos("Ingeniera <script>", "Ingeniería industrial", correo, "3005556789",
                organizacion, "NIT ignorado", "Armenia", "Sector ignorado", "Evaluar posturas",
                co.aplomo.dao.SolicitudAccesoDao.Tipo.EVALUADOR);
    }

    @Test
    void evaluadorSolicitaSinCrearCuentaYNotificaConTipoNombreYCorreo() throws Exception {
        var datos = evaluador(UUID.randomUUID() + "@prueba.invalid", "");
        int empresas = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        int id = servicio().solicitar(datos);
        assertNull(dao.porCorreo(con, datos.correo()));
        var solicitud = servicio().consultar(cuenta(admin), id);
        assertEquals(co.aplomo.dao.SolicitudAccesoDao.Tipo.EVALUADOR, solicitud.tipo());
        assertNull(solicitud.nit());
        assertNull(solicitud.empresa());
        assertEquals(empresas, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
        assertTrue(new Avisos(tx, reloj).listar(cuenta(admin)).stream().anyMatch(m ->
                Integer.valueOf(id).equals(m.solicitudAcceso()) && m.texto().equals(
                        "Solicitud de evaluador · " + datos.nombre() + " · " + datos.correo())));
    }

    @Test
    void aprobarEvaluadorCreaCuentaActivaTemporalSinEmpresasYConColorAutomatico() throws Exception {
        var datos = evaluador(UUID.randomUUID() + "@prueba.invalid", "Organización solicitante");
        int empresas = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        int id = servicio().solicitar(datos);
        var aprobada = servicio().aprobar(cuenta(admin), id, usuario);
        var nueva = dao.porCorreo(con, datos.correo());
        assertEquals(Cuenta.Rol.EVALUADOR, nueva.rol());
        assertEquals(Cuenta.Estado.ACTIVA, nueva.estado());
        assertTrue(nueva.temporal());
        assertNull(nueva.empresa());
        assertNull(aprobada.empresa());
        assertTrue(new CuentaEmpresaDao().ids(con, nueva.id()).isEmpty());
        assertTrue(ColoresAgenda.PALETA.contains(nueva.colorAgenda()));
        assertTrue(aprobada.temporal().length() >= 12);
        assertDoesNotThrow(() -> Contrasenas.validar(aprobada.temporal()));
        assertNotNull(auth.ingresar(datos.correo(), aprobada.temporal()));
        assertEquals("APROBADA", servicio().consultar(cuenta(admin), id).estado());
        assertEquals(empresas, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
        assertEquals(1, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM bitacora WHERE tabla='cuenta' " +
                "AND id_registro=? AND accion='CREAR'", f -> f.getInt(1), nueva.id()));
        assertThrows(IllegalArgumentException.class, () -> servicio().aprobar(cuenta(admin), id, null));
    }

    @Test
    void correoRepetidoDeEvaluadorConservaCuentaYDejaSolicitudPendiente() throws Exception {
        var anterior = cuenta(admin);
        int cuentas = Sql.uno(con, "SELECT COUNT(*) FROM cuenta", f -> f.getInt(1));
        int empresas = Sql.uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1));
        int id = servicio().solicitar(evaluador(anterior.correo(), null));
        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio().aprobar(cuenta(admin), id, null));
        assertTrue(error.getMessage().contains("correo"));
        assertEquals(anterior, cuenta(admin));
        assertEquals("PENDIENTE", servicio().consultar(cuenta(admin), id).estado());
        assertEquals(cuentas, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM cuenta", f -> f.getInt(1)));
        assertEquals(empresas, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", f -> f.getInt(1)));
    }

    @Test
    void rechazarEvaluadorExigeMotivoYNoCreaCuenta() throws Exception {
        var datos = evaluador(UUID.randomUUID() + "@prueba.invalid", "");
        int id = servicio().solicitar(datos);
        assertThrows(IllegalArgumentException.class, () -> servicio().rechazar(cuenta(admin), id, " "));
        assertEquals("PENDIENTE", servicio().consultar(cuenta(admin), id).estado());
        servicio().rechazar(cuenta(admin), id, "Falta confirmar la experiencia.");
        assertEquals("RECHAZADA", servicio().consultar(cuenta(admin), id).estado());
        assertNull(dao.porCorreo(con, datos.correo()));
        assertEquals("Falta confirmar la experiencia.", Sql.uno(con,
                "SELECT motivo_rechazo FROM solicitud_acceso WHERE id_solicitud=?", f -> f.getString(1), id));
    }

    @Test
    void solicitudEvaluadorValidaCamposYLimiteDelMotivo() {
        assertThrows(IllegalArgumentException.class, () -> evaluador("correo-invalido", ""));
        assertThrows(IllegalArgumentException.class, () -> new SolicitudesAcceso.Datos("Ana", "Ingeniera",
                "ana@prueba.invalid", "3005556789", "", null, "Armenia", null, "x".repeat(501),
                co.aplomo.dao.SolicitudAccesoDao.Tipo.EVALUADOR));
        assertThrows(IllegalArgumentException.class, () -> new SolicitudesAcceso.Datos("Ana", "",
                "ana@prueba.invalid", "3005556789", "", null, "Armenia", null, "",
                co.aplomo.dao.SolicitudAccesoDao.Tipo.EVALUADOR));
        assertThrows(IllegalArgumentException.class, () -> new SolicitudesAcceso.Datos("Ana", "Ingeniera",
                "ana@prueba.invalid", "3005556789", "", null, "", null, "",
                co.aplomo.dao.SolicitudAccesoDao.Tipo.EVALUADOR));
    }
}
