package co.aplomo.web;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.PracticaEvaluacion;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 17: práctica separada, pantallas y permisos con datos reversibles.
 */
class Paso17Test extends HttpPrueba {
    @Test
    void practicaNoCambiaLasOnceListasNiSusCatalogosNiElSemaforo() throws Exception {
        var evs = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao());
        var consultas = evs.consultas();
        var cuenta2 = evaluadorTrabajo();
        var antes = new EnumMap<SeccionConsulta, Integer>(SeccionConsulta.class);
        for (var seccion : SeccionConsulta.values())
            antes.put(seccion, consultas.listar(cuenta2, seccion, Map.of()).filas().size());
        long[] kpi = evs.resumen(cuenta2);
        var destino = new Visitas(tx, reloj).practica().abrir(cuenta2, "PLANTA");
        byte[] png = Files.readAllBytes(
                Path.of("src/main/resources/static/img/demo/07_bodega_levantando_caja_perfil.jpg"));
        evs.guardar(cuenta2, destino.visita(),
                new Evaluaciones.Datos(destino.puesto(), "Práctica", "MAS_INADECUADA", "La postura más incómoda.", 180,
                        false, false, false, false, false, false, "", true, false),
                List.of(new Fotos.Entrada(png, "PERFIL", "Foto ficticia")),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(7).entrada())));
        assertArrayEquals(kpi, evs.resumen(cuenta2));
        int practica = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", fila -> fila.getInt(1),
                PracticaEvaluacion.NIT);
        for (var seccion : SeccionConsulta.values()) {
            var elemento = consultas.listar(cuenta2, seccion, Map.of());
            assertEquals(antes.get(seccion), elemento.filas().size(), seccion.name());
            assertTrue(elemento.catalogos().values().stream().flatMap(List::stream)
                    .noneMatch(elemento2 -> elemento2.empresa() == practica));
        }
        assertTrue(new Reservas(tx, reloj).listar(cuenta2).isEmpty());
        assertTrue(new Visitas(tx, reloj).listar(cuenta2).isEmpty());
        assertTrue(new Visitas(tx, reloj).paraEvaluar(cuenta2).isEmpty());
        assertTrue(new Reservas(tx, reloj).catalogo(cuenta2).empresas().stream()
                .noneMatch(empresa -> empresa.id() == practica));
        assertThrows(SecurityException.class, () -> evs.calificaciones().consultar(cuenta2, practica));
    }

    @Test
    void inicioTieneUnSoloBloqueDeVisitasYContinuarPracticaSeparado() throws Exception {
        var evaluador = evaluadorTrabajo();
        entrar(evaluador.id());
        String texto = get("/").body();
        assertFalse(texto.contains("Evaluar ahora"));
        var destino = new Visitas(tx, reloj).practica().abrir(evaluador, "OFICINA");
        texto = get("/").body();
        assertTrue(texto.contains("Practicar una evaluación"));
        assertTrue(texto.contains("href=\"/demo/evaluacion\""));
        assertEquals(1, texto.split("<h2>Visitas en curso</h2>", -1).length - 1);
        assertFalse(texto.contains("Todos los puestos están evaluados"));
        assertTrue(destino.visita() > 0);
    }

    @Test
    void pestañasYOrdenDentroDeMasFiltros() throws Exception {
        entrar(admin);
        String texto = get("/evaluaciones").body();
        assertFalse(texto.contains("Ver otras secciones"));
        assertTrue(texto.contains("data-lista-filtro"));
        int inicio = texto.indexOf("Más filtros"), orden = texto.indexOf("Ordenar por", inicio);
        int fin = texto.indexOf("</details>", inicio);
        assertTrue(inicio > 0 && orden > inicio && orden < fin);
        assertFalse(texto.contains("Aplicar filtros"));
    }

    @Test
    void visitaOcultaNotasYFinVaciosYOfreceEvaluar() throws Exception {
        var salon = crearSalon();
        new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Puesto", "MESA", "Aquí", null, true));
        var reservas = new Reservas(tx, reloj);
        int reserva = reservas.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reservas.aceptar(evaluadorReserva(reserva), reserva);
        int visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(reserva), reserva);
        entrar(admin);
        String texto = get("/visitas/" + visita).body();
        assertFalse(texto.contains("<dt>Fin real</dt>"));
        assertFalse(texto.contains("<dt>Notas</dt>"));
        assertTrue(texto.contains(">Ver reserva</a>"));
        assertTrue(texto.contains("class=\"btn p\">Evaluar</span>"));
    }

    @Test
    void faviconEsPublicoYErroresTienenSalidaSinDetallesTecnicos() throws Exception {
        for (String ruta : List.of("/favicon.ico", "/favicon.svg")) {
            var respuesta = get(ruta);
            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml"));
            assertTrue(respuesta.body().contains("plom") || respuesta.body().contains("<svg"));
        }
        assertTrue(get("/login").body().contains("Iniciar sesión · ALINEO."));
        entrar(admin);
        var respuesta2 = get("/no-existe");
        assertEquals(404, respuesta2.statusCode());
        assertTrue(respuesta2.body().contains("Volver al inicio"));
        assertTrue(Html.error("No pudimos abrir esta página", "Inténtalo otra vez.").contains("Volver al inicio"));
    }

    @Test
    void cuelloManualMenosSeisConservaCuatroPuntos() {
        assertEquals(4, co.aplomo.rula.PuntuadorSegmentos.cuello(-6, false, false));
    }
}
