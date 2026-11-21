package co.aplomo.servicio;

import co.aplomo.dao.AnalisisDao;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.SugerenciaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.ReglasSugerenciasPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * RF-023a/b: reglas reales, condición/segmento, origen, persistencia, permisos y conservación.
 */
class SugerenciasTest extends BasePrueba {
    @TempDir
    Path fotos;
    Evaluaciones evs;
    Sugerencias servicio;
    int visita, puesto, evaluacion, foto;

    @BeforeEach
    void contexto() throws Exception {
        ReglasSugerenciasPrueba.cargar(con);
        evs = new Evaluaciones(tx, reloj, new Fotos(fotos), new BitacoraDao());
        servicio = evs.sugerencias();
        var salon = crearSalon();
        puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Ensamble", "MESA", "Centro", null, true)).id();
        var reserva2 = new Reservas(tx, reloj);
        int reserva = reserva2.guardar(cuenta(admin), 0, solicitud(salon));
        reserva2.aceptar(cuenta(admin), reserva);
        visita = new Visitas(tx, reloj).iniciar(cuenta(admin), reserva);
        evaluacion = evs.guardar(cuenta(admin), visita,
                new Evaluaciones.Datos(puesto, "Postura", "MAS_INADECUADA", "Trabajo mantenido", 120, true, true, true,
                        true, true, true, "", true, false), List.of(imagen()));
        foto = new EvaluacionDao().fotos(con, evaluacion).get(0).id();
    }

    private Fotos.Entrada imagen() throws Exception {
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(12, 12, 1), "png", elemento);
        return new Fotos.Entrada(elemento.toByteArray(), "PERFIL", "Prueba");
    }

    private void analizar(int caso) throws Exception {
        evs.analisis().guardar(cuenta(admin), evaluacion,
                List.of(new Analisis.Solicitud(foto, CasosRulaPrueba.todos().get(caso - 1).entrada())));
    }

    private int manual() throws Exception {
        return servicio.guardar(cuenta(admin), evaluacion, 0, "Pausa acordada", "Organizar descansos.", "DESCANSOS",
                "BAJA", "");
    }

    @Test
    void piesColgandoYTroncoTresProponenReposapies() throws Exception {
        analizar(10);
        var lista = servicio.listar(cuenta(admin), evaluacion);
        var sugerencia = lista.stream()
                .filter(sugerencia2 -> sugerencia2.sugerencia().descripcion().contains("reposapiés o bajar la silla"))
                .findFirst().orElseThrow();
        assertEquals("MOBILIARIO", sugerencia.sugerencia().tipo());
        assertEquals("SISTEMA", sugerencia.sugerencia().origen());
        assertEquals("ALTA", sugerencia.sugerencia().prioridad());
        assertEquals("PENDIENTE", sugerencia.sugerencia().estado());
        assertNull(sugerencia.sugerencia().limite());
        assertNotNull(sugerencia.sugerencia().regla());
        assertTrue(sugerencia.sugerencia().titulo().length() <= 120);
    }

    @Test
    void noAplicaReposapiesSiTroncoMenorQueTres() throws Exception {
        analizar(2);
        assertFalse(servicio.listar(cuenta(admin), evaluacion).stream()
                .anyMatch(sugerencia -> sugerencia.sugerencia().descripcion().contains("reposapiés")));
        assertTrue(servicio.listar(cuenta(admin), evaluacion).stream()
                .allMatch(sugerencia2 -> sugerencia2.sugerencia().prioridad().equals("MEDIA")));
    }

    @Test
    void tiposFijosNoSeAdivinanDelTextoYReglasSonIdempotentes() throws Exception {
        int cantidad = Sql.uno(con, "SELECT COUNT(*) FROM regla_sugerencia", fila -> fila.getInt(1));
        ReglasSugerenciasPrueba.cargar(con);
        assertEquals(cantidad,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM regla_sugerencia", fila2 -> fila2.getInt(1)));
        analizar(10);
        for (var sugerencia : servicio.listar(cuenta(admin), evaluacion)) {
            var sugerencia2 = new SugerenciaDao().reglas(con).stream()
                    .filter(sugerencia3 -> sugerencia3.id() == sugerencia.sugerencia().regla()).findFirst()
                    .orElseThrow();
            assertEquals(Sugerencias.TIPOS_POR_CONDICION.get(sugerencia2.condicion()), sugerencia.sugerencia().tipo());
        }
    }

    @Test
    void repetirAnalisisConservaEdicionHechaYQuitadaSinDuplicar() throws Exception {
        analizar(10);
        var lista = servicio.listar(cuenta(admin), evaluacion);
        int cantidad2 = lista.get(0).sugerencia().id(), cantidad3 = lista.get(1).sugerencia().id();
        servicio.guardar(cuenta(admin), evaluacion, cantidad2, "Personalizada", "Texto acordado", "OTRA", "BAJA",
                "2026-11-25");
        servicio.estado(cuenta(admin), cantidad2, "HECHA");
        servicio.quitar(cuenta(admin), cantidad3);
        int total = new SugerenciaDao().porEvaluacion(con, evaluacion, false).size();
        analizar(10);
        assertEquals(total, new SugerenciaDao().porEvaluacion(con, evaluacion, false).size());
        var sugerencia = servicio.consultar(cuenta(admin), cantidad2).sugerencia();
        assertEquals("Personalizada", sugerencia.titulo());
        assertEquals("HECHA", sugerencia.estado());
        assertEquals("SISTEMA", sugerencia.origen());
        assertEquals("BAJA", sugerencia.prioridad());
        assertFalse(new SugerenciaDao().buscar(con, cantidad3).activo());
    }

    @Test
    void ultimoAmarilloNoSeQuitaYEstadoNoDuplicaAuditoria() throws Exception {
        analizar(9);
        assertTrue(servicio.listar(cuenta(admin), evaluacion).isEmpty());
        int id = manual();
        assertThrows(IllegalArgumentException.class, () -> servicio.quitar(cuenta(admin), id));
        servicio.estado(cuenta(admin), id, "EN_PROCESO");
        servicio.estado(cuenta(admin), id, "HECHA");
        servicio.estado(cuenta(admin), id, "HECHA");
        assertEquals(2, servicio.historial(cuenta(admin), id).stream()
                .filter(elemento -> elemento.accion().equals("CAMBIAR_ESTADO")).count());
        assertTrue(servicio.todasHechas(cuenta(admin), evaluacion));
        assertEquals("ADMINISTRADOR", servicio.consultar(cuenta(admin), id).sugerencia().origen());
    }

    @Test
    void verdePermiteQuitarYAnalisisSeGuardaSinSugerencias() throws Exception {
        analizar(4);
        assertTrue(servicio.listar(cuenta(admin), evaluacion).isEmpty());
        assertEquals(1, new AnalisisDao().listar(con, evaluacion).size());
        int id = manual();
        servicio.quitar(cuenta(admin), id);
        assertFalse(servicio.consultar(cuenta(admin), id).sugerencia().activo());
        assertFalse(servicio.todasHechas(cuenta(admin), evaluacion));
    }

    @Test
    void usuarioSoloEmpresaFinalizadaYNuncaModifica() throws Exception {
        analizar(10);
        int id = servicio.listar(cuenta(admin), evaluacion).get(0).sugerencia().id();
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        assertTrue(servicio.listar(cuenta(usuario), 0).isEmpty());
        assertThrows(SecurityException.class, () -> servicio.consultar(cuenta(usuario), id));
        new Visitas(tx, reloj).finalizar(cuenta(admin), visita, "");
        assertFalse(servicio.listar(cuenta(usuario), 0).isEmpty());
        assertThrows(SecurityException.class, () -> servicio.estado(cuenta(usuario), id, "HECHA"));
        assertThrows(SecurityException.class, () -> servicio.quitar(cuenta(usuario), id));
        assertThrows(SecurityException.class,
                () -> servicio.guardar(cuenta(usuario), evaluacion, 0, "x", "x", "OTRA", "BAJA", ""));
        int otra = Sql.insertar(con, "INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)", admin, "Otra",
                UUID.randomUUID().toString().substring(0, 20));
        int cantidad2 = insertar("USUARIO", otra, cuenta(admin).hash(), false);
        assertTrue(servicio.listar(cuenta(cantidad2), 0).isEmpty());
        assertThrows(SecurityException.class, () -> servicio.consultar(cuenta(cantidad2), id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"titulo", "descripcion", "tipo", "prioridad", "fecha", "estado"})
    void validaSinModificar(String campo) throws Exception {
        analizar(4);
        int id = manual();
        if (campo.equals("estado"))
            assertThrows(IllegalArgumentException.class, () -> servicio.estado(cuenta(admin), id, "BORRADA"));
        else assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), evaluacion, id,
                campo.equals("titulo") ? "x".repeat(121) : "Correcto", campo.equals("descripcion") ? "" : "Texto",
                campo.equals("tipo") ? "INVALIDO" : "OTRA", campo.equals("prioridad") ? "URGENTE" : "MEDIA",
                campo.equals("fecha") ? "0001-01-01" : ""));
        assertEquals("Pausa acordada", servicio.consultar(cuenta(admin), id).sugerencia().titulo());
    }

    @Test
    void tituloCortaPalabraCompletaYPrioridadInicialEsConfigurable() {
        String texto = "mejora ".repeat(30);
        String texto2 = Sugerencias.titulo(texto);
        assertEquals("mejora ".repeat(17).strip(), texto2);
        assertEquals("ALTA", Sugerencias.prioridad("ROJO"));
        assertEquals("MEDIA", Sugerencias.prioridad("AMARILLO"));
        assertEquals("MEDIA", Sugerencias.prioridad("VERDE"));
    }

    @Test
    void nuevasReglasSeAgreganSinAlterarLasAnteriores() throws Exception {
        analizar(2);
        int anteriores = servicio.listar(cuenta(admin), evaluacion).size();
        analizar(10);
        assertTrue(servicio.listar(cuenta(admin), evaluacion).size() > anteriores);
        assertTrue(servicio.listar(cuenta(admin), evaluacion).stream()
                .anyMatch(sugerencia -> sugerencia.sugerencia().prioridad().equals("MEDIA")));
    }

    @Test
    void noSugiereMobiliarioCuandoCondicionFalsa() throws Exception {
        analizar(10);
        var elemento = evs.analisis()
                .consultar(cuenta(admin), evaluacion, new AnalisisDao().listar(con, evaluacion).get(0).id()).calculo();
        var prev = servicio.previsualizar(cuenta(admin), elemento, Map.of());
        assertTrue(prev.stream().allMatch(sugerencia -> sugerencia.condicion().equals("SIEMPRE")));
        assertEquals(5, prev.size());
    }

    @Test
    void falloDeAuditoriaRevierteSugerenciasAnalisisEvaluacionYFoto() throws Exception {
        var falla = new BitacoraDao() {
            @Override
            public void registrar(java.sql.Connection conexion, int actor, LocalDateTime fecha, String tabla, int id,
                    String accion, String anterior, String nuevo, String detalle) throws java.sql.SQLException {
                if (tabla.equals("sugerencia")) throw new java.sql.SQLException("Fallo simulado");
                super.registrar(conexion, actor, fecha, tabla, id, accion, anterior, nuevo, detalle);
            }
        };
        var conFallo = new Evaluaciones(tx, reloj, new Fotos(fotos), falla);
        assertThrows(java.sql.SQLException.class, () -> conFallo.guardar(cuenta(admin), visita,
                new Evaluaciones.Datos(puesto, "Prueba", "MAS_INADECUADA", "Trabajo", 60, true, true, true, true, true,
                        true, "", true, false), List.of(imagen()),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(9).entrada()))));
        assertEquals(1,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1),
                        visita));
        assertTrue(servicio.listar(cuenta(admin), 0).isEmpty());
        assertTrue(new AnalisisDao().listar(con, evaluacion).isEmpty());
        try (var archivo = Files.list(fotos)) {
            assertEquals(1, archivo.count());
        }
    }
}
