package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.PendienteDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.soporte.CasosRulaPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-022: generación por color real, historial, máximo aprobado, alcance y reversión completa.
 */
class PendientesTest extends BasePrueba {
    @TempDir
    Path carpeta;
    Evaluaciones evals;
    Pendientes pendientes;
    int visita, puesto, eval, foto;

    @BeforeEach
    void contexto() throws Exception {
        evals = new Evaluaciones(tx, reloj, new Fotos(carpeta), new BitacoraDao());
        pendientes = evals.pendientes();
        var salon = crearSalon();
        puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Ensamble", "ESCRITORIO", "Centro", null, true)).id();
        var reserva2 = new Reservas(tx, reloj);
        int reserva = reserva2.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reserva2.aceptar(evaluadorReserva(reserva), reserva);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(reserva), reserva);
        eval = evals.guardar(cuenta(admin), visita, datos(), List.of(imagen()));
        foto = new EvaluacionDao().fotos(con, eval).get(0).id();
    }

    private Evaluaciones.Datos datos() {
        return new Evaluaciones.Datos(puesto, "Ensamble", "MAS_INADECUADA", "Postura mantenida", 60, false, false,
                false, false, false, false, "", true, false);
    }

    private Fotos.Entrada imagen() throws Exception {
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(12, 12, 1), "png", elemento);
        return new Fotos.Entrada(elemento.toByteArray(), "PERFIL", "Perfil de prueba");
    }

    private void analizar(int caso) throws Exception {
        evals.analisis().guardar(cuenta(admin), eval,
                List.of(new Analisis.Solicitud(foto, CasosRulaPrueba.todos().get(caso - 1).entrada())));
    }

    private int id() throws Exception {
        return new PendienteDao().porEvaluacion(con, eval).id();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void soloLosResultadosRojosCreanPendiente(int caso) throws Exception {
        analizar(caso);
        var elemento = new PendienteDao().porEvaluacion(con, eval);
        boolean rojo = caso == 7 || caso == 8 || caso == 10;
        assertEquals(rojo, elemento != null);
        if (rojo) {
            assertEquals(CasosRulaPrueba.todos().get(caso - 1).esperado(), elemento.puntaje());
            assertNull(elemento.descripcion());
            assertNull(elemento.prioridad());
            assertNull(elemento.limite());
            assertEquals("ABIERTO", elemento.estado());
        }
    }

    @Test
    void maximoConservaDatosFechaYArregladoYNoDuplica() throws Exception {
        analizar(7);
        int id = id();
        LocalDate registro = pendientes.consultar(cuenta(admin), id).pendiente().registro();
        String descripcion = "Ajustar la mesa\nSin borrar historia";
        pendientes.guardar(cuenta(admin), id, descripcion, "ALTA", "2026-11-25");
        assertTrue(pendientes.arreglar(cuenta(admin), id));
        reloj.avanzar(Duration.ofDays(2));
        analizar(8);
        analizar(7);
        analizar(8);
        var elemento = pendientes.consultar(cuenta(admin), id).pendiente();
        assertEquals(7, elemento.puntaje());
        assertEquals(descripcion, elemento.descripcion());
        assertEquals("ALTA", elemento.prioridad());
        assertEquals(LocalDate.of(2026, 11, 25), elemento.limite());
        assertEquals(registro, elemento.registro());
        assertEquals("ARREGLADO", elemento.estado());
        assertEquals(1, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM pendiente_arreglo WHERE id_evaluacion=?",
                fila -> fila.getInt(1), eval));
        assertEquals(1, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='pendiente_arreglo' AND id_registr" +
                        "o=? AND accion='CREAR'", fila2 -> fila2.getInt(1), id));
    }

    @Test
    void otraEvaluacionGeneraOtroPendienteYUnVerdeNoBorraLosAnteriores() throws Exception {
        analizar(8);
        int primero = id();
        int segundo = evals.guardar(cuenta(admin), visita, datos(), List.of(imagen()),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(7).entrada())));
        int otro = new PendienteDao().porEvaluacion(con, segundo).id();
        pendientes.arreglar(cuenta(admin), primero);
        assertEquals("ABIERTO", pendientes.consultar(cuenta(admin), otro).pendiente().estado());
        evals.guardar(cuenta(admin), visita, datos(), List.of(imagen()),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(3).entrada())));
        assertNotNull(pendientes.consultar(cuenta(admin), primero));
        assertNotNull(pendientes.consultar(cuenta(admin), otro));
        assertEquals(1, Sql.<Integer>uno(con, "SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",
                fila -> fila.getInt(1), puesto));
    }

    @Test
    void usuarioSoloConsultaSuEmpresaYVisitasFinalizadas() throws Exception {
        analizar(8);
        int id = id();
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        assertTrue(pendientes.listar(cuenta(usuario), "TODOS", "prioridad").isEmpty());
        assertThrows(SecurityException.class, () -> pendientes.consultar(cuenta(usuario), id));
        new Visitas(tx, reloj).finalizar(cuenta(admin), visita, "");
        assertEquals(1, pendientes.listar(cuenta(usuario), "TODOS", "fecha").size());
        assertEquals(id, pendientes.consultar(cuenta(usuario), id).pendiente().id());
        assertThrows(SecurityException.class, () -> pendientes.guardar(cuenta(usuario), id, "Cambiar", "BAJA", ""));
        assertThrows(SecurityException.class, () -> pendientes.arreglar(cuenta(usuario), id));
        int empresaB = Sql.insertar(con, "INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)", admin,
                "Otra empresa", UUID.randomUUID().toString().substring(0, 20));
        int usuarioB = insertar("USUARIO", empresaB, cuenta(admin).hash(), false);
        assertTrue(pendientes.listar(cuenta(usuarioB), "TODOS", "fecha").isEmpty());
        assertThrows(SecurityException.class, () -> pendientes.consultar(cuenta(usuarioB), id));
    }

    @Test
    void seguimientoOpcionalLimitesYAuditoriaSinCambiarRula() throws Exception {
        analizar(7);
        int id = id();
        pendientes.guardar(cuenta(admin), id, "á".repeat(255), "MEDIA", "1999-01-01");
        assertEquals(255, pendientes.consultar(cuenta(admin), id).pendiente().descripcion().length());
        assertThrows(IllegalArgumentException.class,
                () -> pendientes.guardar(cuenta(admin), id, "a".repeat(256), "MEDIA", ""));
        pendientes.guardar(cuenta(admin), id, "", "", "");
        var elemento = pendientes.consultar(cuenta(admin), id).pendiente();
        assertNull(elemento.descripcion());
        assertNull(elemento.prioridad());
        assertNull(elemento.limite());
        assertTrue(pendientes.arreglar(cuenta(admin), id));
        assertFalse(pendientes.arreglar(cuenta(admin), id));
        assertEquals(6, Sql.<Integer>uno(con, "SELECT ultimo_puntaje FROM puesto_trabajo WHERE id_puesto=?",
                fila -> fila.getInt(1), puesto));
        var elementos = pendientes.historial(cuenta(admin), id);
        assertEquals(1, elementos.stream().filter(elemento2 -> elemento2.accion().equals("CAMBIAR_ESTADO")).count());
        String nombre = cuenta(admin).nombre();
        assertTrue(elementos.stream().allMatch(elemento3 -> elemento3.responsable().equals(nombre)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prioridad", "fecha", "fuera"})
    void rechazaDatosInvalidosSinAlterarElSeguimiento(String caso) throws Exception {
        analizar(8);
        int id = id();
        assertThrows(IllegalArgumentException.class,
                () -> pendientes.guardar(cuenta(admin), id, "Conservar", caso.equals("prioridad") ? "URGENTE" : "ALTA",
                        caso.equals("fecha") ? "invalida" : caso.equals("fuera") ? "0001-01-01" : ""));
        assertNull(pendientes.consultar(cuenta(admin), id).pendiente().descripcion());
        assertThrows(IllegalArgumentException.class, () -> pendientes.listar(cuenta(admin), "TODOS", "x; DROP TABLE"));
    }

    @Test
    void ordenarPorPrioridadRegistroYLimiteEsDeterminista() throws Exception {
        analizar(8);
        int uno = id();
        pendientes.guardar(cuenta(admin), uno, "", "BAJA", "2026-12-01");
        reloj.avanzar(Duration.ofDays(1));
        int segunda = evals.guardar(cuenta(admin), visita, datos(), List.of(imagen()),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(7).entrada())));
        int dos = new PendienteDao().porEvaluacion(con, segunda).id();
        pendientes.guardar(cuenta(admin), dos, "", "ALTA", "2026-11-20");
        assertEquals(dos, pendientes.listar(cuenta(admin), "ABIERTO", "prioridad").get(0).pendiente().id());
        assertEquals(dos, pendientes.listar(cuenta(admin), "ABIERTO", "limite").get(0).pendiente().id());
        assertEquals(dos, pendientes.listar(cuenta(admin), "ABIERTO", "fecha").get(0).pendiente().id());
    }

    @Test
    void falloAlCrearPendienteRevierteEvaluacionAnalisisYFotoNuevos() throws Exception {
        var falla = new BitacoraDao() {
            @Override
            public void registrar(Connection conexion, int actor, LocalDateTime fecha, String tabla, int id,
                    String accion, String texto, String texto2, String detalle) throws SQLException {
                if (tabla.equals("pendiente_arreglo")) throw new SQLException("Fallo simulado");
                super.registrar(conexion, actor, fecha, tabla, id, accion, texto, texto2, detalle);
            }
        };
        var evaluacion = new Evaluaciones(tx, reloj, new Fotos(carpeta), falla);
        assertThrows(SQLException.class, () -> evaluacion.guardar(cuenta(admin), visita, datos(), List.of(imagen()),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(7).entrada()))));
        assertEquals(1,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1),
                        visita));
        assertNull(new PendienteDao().porEvaluacion(con, eval));
        try (var fs = Files.list(carpeta)) {
            assertEquals(1, fs.count());
        }
    }
}
