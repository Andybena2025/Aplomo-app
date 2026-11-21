package co.aplomo.servicio;

import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.db.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-024: filtros combinados, orden, datos invisibles antes de finalizar y cero modificaciones al consultar.
 */
class ConsultasTest extends BasePrueba {
    @TempDir
    Path fotos;
    EscenarioConsultas escenario;
    Consultas consultas;

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioConsultas(this, fotos);
        consultas = escenario.consultas;
    }

    @ParameterizedTest
    @EnumSource(SeccionConsulta.class)
    void cadaSeccionSoloDevuelveEmpresaAutorizada(SeccionConsulta seccion) throws Exception {
        var elemento = consultas.listar(cuenta(usuario), seccion, Map.of());
        assertFalse(elemento.filas().isEmpty());
        assertTrue(elemento.filas().stream().allMatch(elemento2 -> elemento2.valor("empresa").equals("" + empresa)));
        assertTrue(elemento.catalogos().values().stream().flatMap(Collection::stream)
                .allMatch(elemento3 -> elemento3.empresa() == empresa));
        assertFalse(elemento.filas().toString().contains("Empresa ajena QA"));
        assertThrows(SecurityException.class,
                () -> consultas.listar(cuenta(usuario), seccion, Map.of("empresa", "" + escenario.empresaB)));
    }

    @Test
    void todosLosFiltrosDeEvaluacionSeCombinan() throws Exception {
        var texto = new HashMap<String, String>();
        texto.put("empresa", "" + escenario.empresaA);
        texto.put("sede", "" + escenario.sedeA);
        texto.put("salon", "" + escenario.salonA);
        texto.put("area", "" + escenario.areaA);
        texto.put("cargo", "" + escenario.cargoA);
        texto.put("numero", "3");
        texto.put("desde", "2026-11-10");
        texto.put("hasta", "2026-11-10");
        texto.put("puntaje", "7");
        texto.put("color", "ROJO");
        texto.put("nivel", "4");
        texto.put("estado", "FINALIZADA");
        var elemento = consultas.listar(cuenta(usuario), SeccionConsulta.EVALUACIONES, texto);
        assertEquals(1, elemento.filas().size());
        assertEquals(escenario.evalRojo, elemento.filas().get(0).id());
        texto.put("puntaje", "2");
        assertTrue(consultas.listar(cuenta(usuario), SeccionConsulta.EVALUACIONES, texto).filas().isEmpty());
    }

    @Test
    void usuarioVeTodasSusReservasConCancelacionPeroNoResultadosAbiertos() throws Exception {
        assertEquals(4, consultas.listar(cuenta(usuario), SeccionConsulta.RESERVAS, Map.of()).filas().size());
        var reserva = new Reservas(tx, reloj).consultar(cuenta(usuario), escenario.reservaCancelada);
        assertEquals("La empresa solicitó otro horario.", reserva.reserva().motivo());
        assertEquals(1, consultas.listar(cuenta(usuario), SeccionConsulta.VISITAS, Map.of()).filas().size());
        assertEquals(2, consultas.listar(cuenta(usuario), SeccionConsulta.EVALUACIONES, Map.of()).filas().size());
        assertThrows(SecurityException.class, () -> consultas.evaluacion(cuenta(usuario), escenario.evalAbierta));
        assertThrows(SecurityException.class, () -> consultas.listar(cuenta(usuario), SeccionConsulta.EVALUACIONES,
                Map.of("visita", "" + escenario.visitaAbierta)));
        assertThrows(SecurityException.class, () -> consultas.listar(cuenta(usuario), SeccionConsulta.PENDIENTES,
                Map.of("estado", "TODOS", "evaluacion", "" + escenario.evalAbierta)));
        assertThrows(SecurityException.class, () -> consultas.listar(cuenta(usuario), SeccionConsulta.SUGERENCIAS,
                Map.of("evaluacion", "" + escenario.evalAbierta)));
    }

    @Test
    void puntajeDelPuestoSeOcultaHastaFinalizarInclusoSiHayUnVerdeAnterior() throws Exception {
        var empresas = new Empresas(tx);
        assertEquals(7, empresas.puesto(cuenta(admin), escenario.puesto1).puntaje());
        assertEquals(2, empresas.puesto(cuenta(usuario), escenario.puesto1).puntaje());
        assertNull(empresas.puesto(cuenta(usuario), escenario.puesto2).puntaje());
        var elementos = consultas.listar(cuenta(usuario), SeccionConsulta.PUESTOS,
                Map.of("orden", "puntaje", "direccion", "desc")).filas();
        assertEquals(escenario.puesto3, elementos.get(0).id());
        assertEquals("2",
                elementos.stream().filter(elemento -> elemento.id() == escenario.puesto1).findFirst().orElseThrow()
                        .valor("puntaje"));
        assertEquals("", elementos.get(elementos.size() - 1).valor("puntaje"));
        new Visitas(tx, reloj).finalizar(cuenta(admin), escenario.visitaAbierta, "");
        assertEquals(7, empresas.puesto(cuenta(usuario), escenario.puesto1).puntaje());
        assertEquals(7, empresas.puesto(cuenta(usuario), escenario.puesto2).puntaje());
        assertNotNull(consultas.evaluacion(cuenta(usuario), escenario.evalAbierta));
    }

    @Test
    void busquedaIgnoraMayusculasTildesYNoInterpretaSql() throws Exception {
        var elemento = consultas.listar(cuenta(usuario), SeccionConsulta.AREAS, Map.of("q", "PRODUCCION"));
        assertEquals(1, elemento.filas().size());
        assertTrue(consultas.listar(cuenta(usuario), SeccionConsulta.EMPRESAS, Map.of("q", "' OR 1=1 --")).filas()
                .isEmpty());
        assertTrue(consultas.listar(cuenta(usuario), SeccionConsulta.RESERVAS, Map.of("q", "otro horario")).filas()
                .stream().allMatch(elemento2 -> elemento2.id() == escenario.reservaCancelada));
    }

    @Test
    void ordenaNumerosEnAmbosSentidosYConservaNulosAlFinal() throws Exception {
        var asc = consultas.listar(cuenta(usuario), SeccionConsulta.PUESTOS,
                Map.of("orden", "numero", "direccion", "asc")).filas();
        var desc = consultas.listar(cuenta(usuario), SeccionConsulta.PUESTOS,
                Map.of("orden", "numero", "direccion", "desc")).filas();
        assertEquals(List.of("1", "2", "3"), asc.stream().map(elemento -> elemento.valor("numero")).toList());
        assertEquals(List.of("3", "2", "1"), desc.stream().map(elemento2 -> elemento2.valor("numero")).toList());
        for (String direccion : List.of("asc", "desc"))
            assertEquals(escenario.puesto2, consultas.listar(cuenta(usuario), SeccionConsulta.PUESTOS,
                    Map.of("orden", "puntaje", "direccion", direccion)).filas().get(2).id());
    }

    @ParameterizedTest
    @ValueSource(strings = {"empresa", "sede", "salon", "area", "cargo"})
    void catalogosAjenosSeDenieganTambienPorParametro(String key) throws Exception {
        int id = switch (key) {
            case "empresa" -> escenario.empresaB;
            case "sede" -> escenario.sedeB;
            case "salon" -> escenario.salonB;
            case "area" -> escenario.areaB;
            default -> escenario.cargoB;
        };
        assertThrows(SecurityException.class,
                () -> consultas.listar(cuenta(usuario), SeccionConsulta.EVALUACIONES, Map.of(key, "" + id)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"orden", "direccion", "puntaje", "nivel", "numero", "desde", "estado", "color"})
    void rechazaFiltrosInvalidos(String key) throws Exception {
        String valor = switch (key) {
            case "puntaje" -> "8";
            case "nivel" -> "5";
            case "numero" -> "-1";
            case "desde" -> "no-fecha";
            default -> "x; DROP TABLE";
        };
        assertThrows(IllegalArgumentException.class,
                () -> consultas.listar(cuenta(admin), SeccionConsulta.EVALUACIONES, Map.of(key, valor)));
    }

    @Test
    void consultasNoCambianDatosYLasFechasSinCoincidenciasDevuelvenCero() throws Exception {
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM bitacora", fila -> fila.getInt(1));
        for (var elemento : SeccionConsulta.values())
            consultas.listar(cuenta(usuario), elemento, Map.of("q", "inexistente"));
        assertEquals(antes, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM bitacora", fila2 -> fila2.getInt(1)));
        assertTrue(
                consultas.listar(cuenta(usuario), SeccionConsulta.EVALUACIONES, Map.of("desde", "2026-11-11")).filas()
                        .isEmpty());
        assertThrows(IllegalArgumentException.class, () -> consultas.listar(cuenta(admin), SeccionConsulta.EVALUACIONES,
                Map.of("desde", "2026-11-12", "hasta", "2026-11-10")));
    }
}
