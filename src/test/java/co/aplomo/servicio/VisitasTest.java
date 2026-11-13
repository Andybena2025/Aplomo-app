package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.Sql;
import co.aplomo.modelo.*;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-013a, RF-013b, RF-002, RNF-005: ciclo real y reversión total sin guardar fixtures. */
class VisitasTest extends BasePrueba {
    private Reservas reservas;
    private Visitas visitas;
    private Salon salon;
    private int reserva;

    @BeforeEach void prepararVisita() throws Exception {
        reservas = new Reservas(tx, reloj); visitas = new Visitas(tx, reloj); salon = crearSalon();
        new Empresas(tx).guardar(cuenta(admin), new Puesto(0,salon.id(),null,null,1,"Puesto disponible","ESCRITORIO","Centro",null,true));
        reserva = reservas.guardar(cuenta(admin), 0, solicitud(salon));
    }

    private int iniciar() throws Exception {
        reservas.aceptar(cuenta(admin), reserva);
        return visitas.iniciar(cuenta(admin), reserva);
    }

    @Test void iniciaUnaSolaVisitaConHoraRealYResponsable() throws Exception {
        int id = iniciar(); var ficha = visitas.consultar(cuenta(admin), id);
        assertEquals(reserva, ficha.visita().reserva()); assertEquals("EN_CURSO", ficha.visita().estado());
        assertEquals(LocalDateTime.now(reloj), ficha.visita().inicio()); assertNull(ficha.visita().fin());
        assertEquals("EN_COLA", reservas.consultar(cuenta(admin), reserva).reserva().estado());
        assertEquals(id, reservas.consultar(cuenta(admin), reserva).visita());
        assertThrows(IllegalArgumentException.class, () -> visitas.iniciar(cuenta(admin), reserva));
        assertThrows(IllegalArgumentException.class, () -> reservas.cancelar(cuenta(admin), reserva, "Intento"));
        try (var ps = con.prepareStatement("SELECT id_cuenta, fecha_hora, accion, estado_nuevo FROM bitacora WHERE tabla = 'visita' AND id_registro = ?")) {
            ps.setInt(1, id); try (var rs = ps.executeQuery()) {
                assertTrue(rs.next()); assertEquals(admin, rs.getInt(1)); assertEquals(LocalDateTime.now(reloj), rs.getTimestamp(2).toLocalDateTime());
                assertEquals("CREAR", rs.getString(3)); assertEquals("EN_CURSO", rs.getString(4)); assertFalse(rs.next());
            }
        }
    }

    @ParameterizedTest @ValueSource(strings = {"ESPERANDO", "CANCELADA", "FINALIZADA"})
    void noIniciaDesdeUnEstadoDistintoDeEnCola(String estado) throws Exception {
        if (estado.equals("CANCELADA")) reservas.cancelar(cuenta(admin), reserva, "Cambio");
        if (estado.equals("FINALIZADA")) Sql.actualizar(con, "UPDATE reserva SET estado = ? WHERE id_reserva = ?", estado, reserva);
        assertThrows(IllegalArgumentException.class, () -> visitas.iniciar(cuenta(admin), reserva));
        assertNull(reservas.consultar(cuenta(admin), reserva).visita());
    }

    @Test void cerrarSinEvaluacionesNoAlteraVisitaReservaNiBitacora() throws Exception {
        int id = iniciar();
        assertThrows(IllegalArgumentException.class, () -> visitas.finalizar(cuenta(admin), id, "No guardar"));
        var visita = visitas.consultar(cuenta(admin), id).visita();
        assertEquals("EN_CURSO", visita.estado()); assertNull(visita.fin()); assertNull(visita.notas());
        assertEquals("EN_COLA", reservas.consultar(cuenta(admin), reserva).reserva().estado());
        assertEquals(1, accionesVisita(id));
    }

    @Test void finalizaConPromedioPorPuestoUsandoElMaximoDeSuUltimaEvaluacion() throws Exception {
        int id = iniciar();
        int vieja = evaluacion(id, salon.id(), 1); analisis(foto(vieja), 7);
        int actual = evaluacion(id, salon.id(), 1); int foto = foto(actual);
        analisis(foto, 3); analisis(foto, 4); analisis(foto(actual), 2);
        int segunda = evaluacion(id, salon.id(), 2); analisis(foto(segunda), 7);
        reloj.avanzar(Duration.ofMinutes(37));
        visitas.finalizar(cuenta(admin), id, "Notas <script>inertes</script>\nSegunda línea");
        var visita = visitas.consultar(cuenta(admin), id).visita();
        assertEquals("FINALIZADA", visita.estado()); assertEquals(LocalDateTime.now(reloj), visita.fin());
        assertEquals(new BigDecimal("5.5"), visita.calificacion()); assertTrue(visita.notas().contains("\n"));
        assertEquals("FINALIZADA", reservas.consultar(cuenta(admin), reserva).reserva().estado());
        assertEquals(2, accionesVisita(id));
        assertEquals(4, visitas.evaluaciones(cuenta(admin), id).stream().filter(e -> e.id() == actual).findFirst().orElseThrow().puntaje());
        try (var ps = con.prepareStatement("SELECT tabla, estado_anterior, estado_nuevo FROM bitacora WHERE (tabla = 'visita' AND id_registro = ?) OR (tabla = 'reserva' AND id_registro = ?) ORDER BY id_bitacora DESC LIMIT 2")) {
            ps.setInt(1, id); ps.setInt(2, reserva);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next()); assertEquals("reserva", rs.getString(1)); assertEquals("EN_COLA", rs.getString(2)); assertEquals("FINALIZADA", rs.getString(3));
                assertTrue(rs.next()); assertEquals("visita", rs.getString(1)); assertEquals("EN_CURSO", rs.getString(2)); assertEquals("FINALIZADA", rs.getString(3));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> visitas.finalizar(cuenta(admin), id, "No reemplazar"));
        assertEquals(visita, visitas.consultar(cuenta(admin), id).visita());
    }

    @Test void unAnalisisMenorNoOcultaElMayorDeLaMismaFotoNiDelOtroLado() throws Exception {
        int id = iniciar(); int foto = foto(evaluacion(id, salon.id(), 1));
        analisis(foto, 7); int izquierdo = analisis(foto, 2);
        Sql.actualizar(con, "UPDATE analisis_rula SET lado = 'IZQUIERDO' WHERE id_analisis = ?", izquierdo);
        visitas.finalizar(cuenta(admin), id, "Ambos lados");
        assertEquals(new BigDecimal("7.0"), visitas.consultar(cuenta(admin), id).visita().calificacion());
        assertEquals(7, visitas.evaluaciones(cuenta(admin), id).get(0).puntaje());
    }

    @Test void sinRegistroFotograficoFinalizaSinInventarUnPuntajeCero() throws Exception {
        int id = iniciar(); evaluacion(id, salon.id(), 1);
        visitas.finalizar(cuenta(admin), id, "Sin fotos por decisión de la persona");
        assertEquals("FINALIZADA", visitas.consultar(cuenta(admin), id).visita().estado());
        assertNull(visitas.consultar(cuenta(admin), id).visita().calificacion());
    }

    @Test void puestosSinAnalisisNoReducenElPromedioDeLosQueSiTienenResultado() throws Exception {
        int id = iniciar(); int e = evaluacion(id, salon.id(), 1); analisis(foto(e), 5);
        evaluacion(id, salon.id(), 2);
        visitas.finalizar(cuenta(admin), id, "");
        assertEquals(new BigDecimal("5.0"), visitas.consultar(cuenta(admin), id).visita().calificacion());
    }

    @Test void falloEnSegundaEntradaDeBitacoraRevierteTodoElCierre() throws Exception {
        int id = iniciar(); analisis(foto(evaluacion(id, salon.id(), 1)), 3);
        BitacoraDao fallo = new BitacoraDao() {
            @Override public void registrar(Connection c, int actor, LocalDateTime fecha, String tabla, int registro,
                    String accion, String anterior, String nuevo, String detalle) throws SQLException {
                super.registrar(c, actor, fecha, tabla, registro, accion, anterior, nuevo, detalle);
                if (tabla.equals("reserva")) throw new SQLException("Fallo simulado después de escribir auditoría");
            }
        };
        assertThrows(SQLException.class, () -> new Visitas(tx, reloj, fallo).finalizar(cuenta(admin), id, "No guardar"));
        var visita = visitas.consultar(cuenta(admin), id).visita();
        assertEquals("EN_CURSO", visita.estado()); assertNull(visita.fin()); assertNull(visita.calificacion()); assertNull(visita.notas());
        assertEquals("EN_COLA", reservas.consultar(cuenta(admin), reserva).reserva().estado()); assertEquals(1, accionesVisita(id));
    }

    @Test void falloDeAuditoriaTambienRevierteElInicio() throws Exception {
        reservas.aceptar(cuenta(admin), reserva);
        BitacoraDao fallo = new BitacoraDao() {
            @Override public void registrar(Connection c, int actor, LocalDateTime fecha, String tabla, int registro,
                    String accion, String anterior, String nuevo, String detalle) throws SQLException { throw new SQLException("Fallo simulado"); }
        };
        assertThrows(SQLException.class, () -> new Visitas(tx, reloj, fallo).iniciar(cuenta(admin), reserva));
        assertNull(reservas.consultar(cuenta(admin), reserva).visita());
    }

    @Test void usuarioSoloVeFinalizadasPropiasSinPoderCambiarNada() throws Exception {
        int id = iniciar(); evaluacion(id, salon.id(), 1);
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false); var contacto = cuenta(usuario);
        assertTrue(visitas.listar(contacto).isEmpty());
        assertThrows(SecurityException.class, () -> visitas.consultar(contacto, id));
        assertThrows(SecurityException.class, () -> visitas.evaluaciones(contacto, id));
        assertThrows(SecurityException.class, () -> visitas.iniciar(contacto, reserva));
        assertThrows(SecurityException.class, () -> visitas.finalizar(contacto, id, "Intento"));
        visitas.finalizar(cuenta(admin), id, "Final");
        assertEquals(List.of(id), visitas.listar(contacto).stream().map(f -> f.visita().id()).toList());
        assertEquals(1, visitas.evaluaciones(contacto, id).size());
        int ajena = Sql.insertar(con, "INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)", admin, "Ajena", UUID.randomUUID().toString().substring(0,20));
        Sql.actualizar(con, "UPDATE cuenta SET id_empresa = ? WHERE id_cuenta = ?", ajena, usuario);
        var otroContacto = cuenta(usuario);
        assertTrue(visitas.listar(otroContacto).isEmpty());
        assertThrows(SecurityException.class, () -> visitas.consultar(otroContacto, id));
        assertThrows(SecurityException.class, () -> visitas.evaluaciones(otroContacto, id));
    }

    @Test void notasInvalidasYHoraAnteriorNoCierranLaVisita() throws Exception {
        int id = iniciar(); evaluacion(id, salon.id(), 1);
        assertThrows(IllegalArgumentException.class, () -> visitas.finalizar(cuenta(admin), id, "x".repeat(65536)));
        reloj.avanzar(Duration.ofMinutes(-1));
        assertThrows(IllegalArgumentException.class, () -> visitas.finalizar(cuenta(admin), id, "Nota"));
        assertEquals("EN_CURSO", visitas.consultar(cuenta(admin), id).visita().estado());
    }

    private int accionesVisita(int id) throws SQLException {
        return Sql.uno(con, "SELECT COUNT(*) FROM bitacora WHERE tabla = 'visita' AND id_registro = ?", r -> r.getInt(1), id);
    }
}
