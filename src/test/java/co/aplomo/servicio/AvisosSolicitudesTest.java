package co.aplomo.servicio;

import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-002/011/033: prueba real de MySQL, íntegramente revertida.
 */
class AvisosSolicitudesTest extends BasePrueba {
    private int ingeniero;
    private int segundo;
    private int salon;
    private SolicitudesVisita solicitudes;
    private Avisos avisos;

    @BeforeEach
    void prepararSolicitudes() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        ingeniero = insertar("EVALUADOR", null, Contrasenas.cifrar(clave), false);
        segundo = insertar("EVALUADOR", null, Contrasenas.cifrar(clave), false);
        salon = crearSalon().id();
        solicitudes = new SolicitudesVisita(tx, reloj);
        avisos = new Avisos(tx, reloj);
    }

    private int solicitar(int evaluador, int hora) throws Exception {
        return solicitudes.solicitar(cuenta(usuario), salon, evaluador, LocalDate.of(2026, 12, 1),
                LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0), "Revisar la mesa");
    }

    @Test
    void libreCreaReservaYMensajeSoloAlIngeniero() throws Exception {
        int id = solicitar(ingeniero, 9);
        assertEquals("ESPERANDO",
                Sql.uno(con, "SELECT estado FROM reserva WHERE id_reserva=?", f -> f.getString(1), id));
        assertEquals(Integer.valueOf(ingeniero),
                Sql.<Integer>uno(con, "SELECT id_evaluador FROM reserva WHERE id_reserva=?", f -> f.getInt(1), id));
        assertEquals(1, avisos.listar(cuenta(ingeniero)).size());
        assertTrue(avisos.listar(cuenta(segundo)).isEmpty());
        assertTrue(avisos.listar(cuenta(admin)).isEmpty());
        assertTrue(avisos.listar(cuenta(usuario)).isEmpty());
    }

    @Test
    void cruceEsPorIngenieroYNoPorEmpresa() throws Exception {
        solicitar(ingeniero, 9);
        assertThrows(SolicitudesVisita.Cruce.class, () -> solicitar(ingeniero, 9));
        assertFalse(solicitudes.libre(cuenta(usuario), ingeniero, LocalDate.of(2026, 12, 1), LocalTime.of(9, 0),
                LocalTime.of(10, 0)));
        assertTrue(solicitudes.libre(cuenta(usuario), segundo, LocalDate.of(2026, 12, 1), LocalTime.of(9, 0),
                LocalTime.of(10, 0)));
        assertTrue(solicitudes.libre(cuenta(usuario), ingeniero, LocalDate.of(2026, 12, 1), LocalTime.of(10, 0),
                LocalTime.of(11, 0)));
    }

    @Test
    void aceptarAsignaEmpresaYSostieneVariosIngenieros() throws Exception {
        int id = solicitar(ingeniero, 9);
        solicitudes.actuar(cuenta(ingeniero), id, "aceptar", null);
        new CuentaEmpresaDao().validar(con, segundo, Cuenta.Rol.EVALUADOR, List.of(empresa), true);
        new CuentaEmpresaDao().sincronizar(con, admin, segundo, List.of(empresa), false);
        assertTrue(new CuentaEmpresaDao().ids(con, ingeniero).contains(empresa));
        assertTrue(new CuentaEmpresaDao().ids(con, segundo).contains(empresa));
        assertEquals("EN_COLA", new Reservas(tx, reloj).consultar(cuenta(usuario), id).reserva().estado());
        assertEquals("Tu visita fue aceptada.", avisos.listar(cuenta(usuario)).get(0).texto());
        assertThrows(SecurityException.class, () -> solicitudes.actuar(cuenta(segundo), id, "aceptar", null));
    }

    @Test
    void pedirInformacionPermiteSoloUnaRespuestaYAcusePrivado() throws Exception {
        int id = solicitar(ingeniero, 9);
        solicitudes.actuar(cuenta(ingeniero), id, "informacion", "¿Cuántos puestos hay?");
        int mensaje = avisos.listar(cuenta(usuario)).get(0).id();
        assertThrows(SecurityException.class, () -> avisos.entender(cuenta(segundo), mensaje));
        avisos.responder(cuenta(usuario), mensaje, "Son tres puestos.");
        assertTrue(avisos.listar(cuenta(usuario)).get(0).leido());
        assertTrue(avisos.listar(cuenta(usuario)).get(0).respondido());
        assertThrows(IllegalArgumentException.class, () -> avisos.responder(cuenta(usuario), mensaje, "Otra"));
        assertTrue(avisos.listar(cuenta(ingeniero)).stream().anyMatch(m -> m.tipo().equals("RESPUESTA")));
    }

    @Test
    void rechazarConMotivoConservaLaReserva() throws Exception {
        int id = solicitar(ingeniero, 9);
        assertThrows(IllegalArgumentException.class, () -> solicitudes.actuar(cuenta(ingeniero), id, "rechazar", ""));
        solicitudes.actuar(cuenta(ingeniero), id, "rechazar", "No hay disponibilidad este día.");
        assertEquals("CANCELADA", new Reservas(tx, reloj).consultar(cuenta(usuario), id).reserva().estado());
        assertTrue(avisos.listar(cuenta(usuario)).get(0).texto().contains("disponibilidad"));
    }

    @Test
    void usuarioNoSolicitaSalonAjenoNiVeSolicitudesDeOtroContacto() throws Exception {
        int id = solicitar(ingeniero, 9);
        int otro = insertar("USUARIO", empresa, Contrasenas.cifrar(clave), false);
        assertThrows(SecurityException.class, () -> new Reservas(tx, reloj).consultar(cuenta(otro), id));
        Sql.actualizar(con, "UPDATE cuenta_empresa SET activo=FALSE WHERE id_cuenta=?", usuario);
        assertThrows(SecurityException.class, () -> solicitar(ingeniero, 11));
    }

    @Test
    void notasIgualesVuelvenAAvisarYLeerSinAcuseNoMarcaNada() throws Exception {
        int id = solicitar(ingeniero, 9);
        solicitudes.actuar(cuenta(ingeniero), id, "aceptar", null);
        Agenda agenda = new Agenda(tx, reloj);
        agenda.guardarNota(cuenta(admin), id, "Revisa el puesto de entrada.");
        var mensaje = avisos.listar(cuenta(ingeniero)).get(0);
        assertEquals("NOTA_ADMIN", mensaje.tipo());
        assertFalse(mensaje.leido());
        assertFalse(avisos.listar(cuenta(ingeniero)).get(0).leido());
        avisos.entender(cuenta(ingeniero), mensaje.id());
        assertTrue(agenda.nota(cuenta(ingeniero), id).vista());
        agenda.guardarNota(cuenta(admin), id, "Revisa el puesto de entrada.");
        assertEquals(2, avisos.listar(cuenta(ingeniero)).stream().filter(m -> m.tipo().equals("NOTA_ADMIN")).count());
        assertEquals(1,
                avisos.listar(cuenta(ingeniero)).stream().filter(m -> m.tipo().equals("NOTA_ADMIN") && !m.leido())
                        .count());
    }

    @Test
    void reservaAnteriorRecibeIngenieroSinCambiarConteos() throws Exception {
        new CuentaEmpresaDao().sincronizar(con, admin, ingeniero, List.of(empresa), false);
        int id = solicitar(ingeniero, 9);
        Sql.actualizar(con, "UPDATE reserva SET id_evaluador=NULL WHERE id_reserva=?", id);
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM reserva", f -> f.getInt(1));
        String script = java.nio.file.Files.readString(
                java.nio.file.Path.of("docs/base_de_datos/migracion_paso27.sql"));
        int inicio = script.indexOf("UPDATE reserva r JOIN salon");
        String actualizar = script.substring(inicio, script.indexOf(';', inicio));
        for (int intento = 0; intento < 2; intento++) {
            Sql.actualizar(con, actualizar);
            Integer responsable = Sql.uno(con, "SELECT id_evaluador FROM reserva WHERE id_reserva=?", f -> f.getInt(1),
                    id);
            assertEquals(Integer.valueOf(ingeniero), responsable);
            assertEquals(Integer.valueOf(antes),
                    Sql.<Integer>uno(con, "SELECT COUNT(*) FROM reserva", f -> f.getInt(1)));
        }
    }

    @Test
    void calendarioCuentaSoloLosAvisosDeSuDestinatario() throws Exception {
        int id = solicitar(ingeniero, 9);
        solicitudes.actuar(cuenta(ingeniero), id, "aceptar", null);
        var agenda = new Agenda(tx, reloj);
        var vista = agenda.consultar(cuenta(ingeniero), java.util.Map.of("fecha", "2026-12-01"));
        assertEquals(1,
                vista.citas().stream().filter(c -> c.ficha().reserva().id() == id).findFirst().orElseThrow().nuevos());
        assertTrue(agenda.consultar(cuenta(segundo), java.util.Map.of("fecha", "2026-12-01")).citas().isEmpty());
        int mensaje = avisos.listar(cuenta(ingeniero)).get(0).id();
        avisos.entender(cuenta(ingeniero), mensaje);
        vista = agenda.consultar(cuenta(ingeniero), java.util.Map.of("fecha", "2026-12-01"));
        assertEquals(0, vista.citas().get(0).nuevos());
    }

    @Test
    void otroAsignadoConsultaReservaSinNotaPeroNoPuedeEditarNiIniciar() throws Exception {
        int id = solicitar(ingeniero, 9);
        solicitudes.actuar(cuenta(ingeniero), id, "aceptar", null);
        new CuentaEmpresaDao().sincronizar(con, admin, segundo, List.of(empresa), false);
        Agenda agenda = new Agenda(tx, reloj);
        agenda.guardarNota(cuenta(admin), id, "Solo para el ingeniero de esta cita.");
        assertNull(agenda.nota(cuenta(segundo), id));
        assertThrows(SecurityException.class, () -> agenda.entender(cuenta(segundo), id));
        assertThrows(SecurityException.class, () -> new Reservas(tx, reloj).cancelar(cuenta(segundo), id, "Motivo"));
        assertThrows(SecurityException.class, () -> new Visitas(tx, reloj).iniciar(cuenta(segundo), id));
    }

    @Test
    void aceptarDesdeDetalleAnteriorTambienNotificaAlContacto() throws Exception {
        new CuentaEmpresaDao().sincronizar(con, admin, ingeniero, List.of(empresa), false);
        int id = solicitar(ingeniero, 9);
        new Reservas(tx, reloj).aceptar(cuenta(ingeniero), id);
        assertEquals("Tu visita fue aceptada.", avisos.listar(cuenta(usuario)).get(0).texto());
    }

}
