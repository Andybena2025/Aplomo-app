package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.modelo.*;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-009b a RF-012, RNF-005: agenda real y auditoría, sin confirmar datos de prueba. */
class ReservasTest extends BasePrueba {
    private Reservas servicio;
    private Salon salon;

    @BeforeEach void prepararAgenda() throws Exception {
        servicio = new Reservas(tx, reloj);
        salon = crearSalon();
    }

    @Test void creaCodigoUnicoEstadoEsperandoYBitacoraEnLaMismaTransaccion() throws Exception {
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        Reserva r = servicio.consultar(cuenta(admin), id).reserva();
        assertEquals(String.format(Locale.ROOT, "R-%04d", id), r.codigo());
        assertTrue(r.codigo().length() <= 12);
        assertEquals("ESPERANDO", r.estado());
        assertEquals(salon.id(), r.salon());
        assertEquals(admin, r.administrador());
        assertTrue(r.notas().contains("\n"));
        try (var ps = con.prepareStatement("SELECT * FROM bitacora WHERE tabla = ? AND id_registro = ?")) {
            ps.setString(1, "reserva"); ps.setInt(2, id);
            try (var filas = ps.executeQuery()) {
                assertTrue(filas.next()); assertEquals(admin, filas.getInt("id_cuenta"));
                assertEquals(LocalDateTime.now(reloj), filas.getTimestamp("fecha_hora").toLocalDateTime());
                assertEquals("CREAR", filas.getString("accion")); assertEquals("ESPERANDO", filas.getString("estado_nuevo"));
                assertFalse(filas.next());
            }
        }
    }

    @Test void editaSoloHorarioCategoriaDescripcionYNotas() throws Exception {
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        Reserva antes = servicio.consultar(cuenta(admin), id).reserva();
        servicio.aceptar(cuenta(admin), id);
        var cambio = new Reservas.Datos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                LocalDate.now(reloj).plusDays(1), LocalTime.of(11, 0), LocalTime.of(12, 0), "SEGUIMIENTO", "Editada", "Notas nuevas");
        servicio.guardar(cuenta(admin), id, cambio);
        Reserva despues = servicio.consultar(cuenta(admin), id).reserva();
        assertEquals(antes.codigo(), despues.codigo()); assertEquals(antes.salon(), despues.salon());
        assertEquals(antes.administrador(), despues.administrador()); assertEquals("EN_COLA", despues.estado());
        assertEquals("SEGUIMIENTO", despues.categoria()); assertEquals("Notas nuevas", despues.notas());
    }

    @Test void evitaDuplicadosAunqueLaReservaAnteriorEsteCancelada() throws Exception {
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), 0, solicitud(salon)));
        servicio.cancelar(cuenta(admin), id, "Cambio acordado con la empresa");
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), 0, solicitud(salon)));
        assertEquals("CANCELADA", servicio.consultar(cuenta(admin), id).reserva().estado());
    }

    @Test void soloAceptaEsperandoYLosEstadosFinalesNoSeEditanNiReabren() throws Exception {
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        servicio.aceptar(cuenta(admin), id);
        assertThrows(IllegalArgumentException.class, () -> servicio.aceptar(cuenta(admin), id));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelar(cuenta(admin), id, " "));
        servicio.cancelar(cuenta(admin), id, "Motivo válido");
        assertThrows(IllegalArgumentException.class, () -> servicio.aceptar(cuenta(admin), id));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelar(cuenta(admin), id, "Otra vez"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), id, solicitud(salon)));
        try (var ps = con.prepareStatement("SELECT accion, estado_anterior, estado_nuevo FROM bitacora WHERE tabla = 'reserva' AND id_registro = ? ORDER BY id_bitacora")) {
            ps.setInt(1, id);
            try (var filas = ps.executeQuery()) {
                assertTrue(filas.next()); assertEquals("CREAR", filas.getString(1));
                assertTrue(filas.next()); assertEquals("ESPERANDO", filas.getString(2)); assertEquals("EN_COLA", filas.getString(3));
                assertTrue(filas.next()); assertEquals("EN_COLA", filas.getString(2)); assertEquals("CANCELADA", filas.getString(3));
                assertFalse(filas.next());
            }
        }
    }

    @Test void unaVisitaIniciadaProtegeSuReservaDeCancelacion() throws Exception {
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        servicio.aceptar(cuenta(admin), id);
        Sql.insertar(con, "INSERT INTO visita (id_reserva, hora_real_inicio) VALUES (?, ?)", id, LocalDateTime.now(reloj));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelar(cuenta(admin), id, "Intento"));
        assertEquals("EN_COLA", servicio.consultar(cuenta(admin), id).reserva().estado());
    }

    @ParameterizedTest @ValueSource(strings = {"ayer", "sinFecha", "sinHora", "igual", "anterior", "categoria", "empresa", "sede"})
    void rechazaFechasHorasCategoriasYRelacionesInvalidas(String caso) throws Exception {
        var base = solicitud(salon);
        var dato = new Reservas.Datos(caso.equals("empresa") ? Integer.MAX_VALUE : base.empresa(),
                caso.equals("sede") ? Integer.MAX_VALUE : base.sede(), base.salon(),
                caso.equals("sinFecha") ? null : caso.equals("ayer") ? base.fecha().minusDays(1) : base.fecha(),
                caso.equals("sinHora") ? null : base.inicio(),
                caso.equals("igual") ? base.inicio() : caso.equals("anterior") ? base.inicio().minusMinutes(1) : base.fin(),
                caso.equals("categoria") ? "NO_APROBADA" : base.categoria(), base.descripcion(), base.notas());
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), 0, dato));
    }

    @Test void catalogoNoOfreceJerarquiasInactivasYElServidorTambienLasRechaza() throws Exception {
        assertTrue(servicio.catalogo(cuenta(admin)).salones().stream().anyMatch(s -> s.id() == salon.id()));
        new Empresas(tx).estadoEmpresa(cuenta(admin), empresa, false);
        assertFalse(servicio.catalogo(cuenta(admin)).empresas().stream().anyMatch(e -> e.id() == empresa));
        assertFalse(servicio.catalogo(cuenta(admin)).sedes().stream().anyMatch(s -> s.id() == salon.sede()));
        assertFalse(servicio.catalogo(cuenta(admin)).salones().stream().anyMatch(s -> s.id() == salon.id()));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), 0, solicitud(salon)));
    }

    @Test void usuarioConsultaSuReservaPeroNoCreaNiCambiaEstados() throws Exception {
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        Empresas empresas = new Empresas(tx);
        int otraEmpresa = empresas.guardar(cuenta(admin), new Empresa(0, 0, "Otra empresa",
                "R" + UUID.randomUUID().toString().substring(0, 10), "", "", "", "", true)).id();
        int otraSede = empresas.guardar(cuenta(admin), new Sede(0, otraEmpresa, "Otra sede", "Calle 1",
                "Armenia", "123", "Mañana", "Descripción", true)).id();
        int otroSalon = empresas.guardar(cuenta(admin), new Salon(0, otraSede, "Otro salón", "Descripción", 1, 10, true)).id();
        var ajena = new Reservas.Datos(otraEmpresa, otraSede, otroSalon, solicitud(salon).fecha(),
                solicitud(salon).inicio(), solicitud(salon).fin(), "EVALUACION_INICIAL", "", "");
        int idAjena = servicio.guardar(cuenta(admin), 0, ajena);
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        Cuenta contacto = cuenta(usuario);
        assertEquals(List.of(id), servicio.listar(contacto).stream().map(f -> f.reserva().id()).toList());
        assertEquals(id, servicio.consultar(contacto, id).reserva().id());
        assertThrows(SecurityException.class, () -> servicio.consultar(contacto, idAjena));
        assertThrows(SecurityException.class, () -> servicio.catalogo(contacto));
        assertThrows(SecurityException.class, () -> servicio.guardar(contacto, 0, solicitud(salon)));
        assertThrows(SecurityException.class, () -> servicio.aceptar(contacto, id));
        assertThrows(SecurityException.class, () -> servicio.cancelar(contacto, id, "Motivo"));
        assertThrows(SecurityException.class, () -> servicio.consultar(contacto, Integer.MAX_VALUE));
    }

    @Test void auditoriaFallidaRevierteLaCreacionYLaAceptacion() throws Exception {
        BitacoraDao fallada = new BitacoraDao() {
            @Override public void registrar(Connection c, int actor, LocalDateTime fecha, String tabla, int registro,
                                             String accion, String anterior, String nuevo, String detalle) throws SQLException {
                throw new SQLException("Fallo de prueba");
            }
        };
        Reservas defectuosa = new Reservas(tx, reloj, fallada);
        assertThrows(SQLException.class, () -> defectuosa.guardar(cuenta(admin), 0, solicitud(salon)));
        assertTrue(servicio.listar(cuenta(admin)).stream().noneMatch(f -> f.reserva().salon() == salon.id()));
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon));
        assertThrows(SQLException.class, () -> defectuosa.aceptar(cuenta(admin), id));
        assertEquals("ESPERANDO", servicio.consultar(cuenta(admin), id).reserva().estado());
    }
}
