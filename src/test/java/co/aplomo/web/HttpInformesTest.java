package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.EscenarioInformes;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-025/026: permisos por dirección, contenido completo y vista de impresión nativa.
 */
class HttpInformesTest extends HttpPrueba {
    EscenarioInformes escenario;

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioInformes(this);
        entrar(admin);
    }

    @Test
    void administradorGeneraInformeConCoberturaEstadisticasYNivelesEscritos() throws Exception {
        var respuesta = get("/informes/empresa/" + empresa);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        String texto = respuesta.body();
        for (String texto2 : List.of("Imprimir o guardar PDF", "2 de 3 sedes calificadas", "falta Sede Norte", "3,75",
                "4 · Amarillo · Nivel 2", "Fotos analizadas", "Pendientes abiertos", "Pendientes arreglados",
                "Puestos por área", "Puestos por tipo de cargo", "Sugerencias activas por tipo y estado",
                "Motivo de elección", "Pies colgando: sí", "America/Bogota"))
            assertTrue(texto.contains(texto2), texto2);
        assertFalse(texto.contains("/fotos/"));
        assertFalse(texto.contains("En curso invisible"));
        assertFalse(texto.contains("Sugerencia quitada invisible"));
        assertFalse(texto.contains("Empresa ajena para informe"));
        assertTrue(texto.contains("/static/js/informe.js"));
        assertFalse(texto.contains("html2canvas"));
        assertFalse(texto.contains("jspdf"));
    }

    @Test
    void empresaConUnaSedeExplicaQueNoHayGeneral() throws Exception {
        var respuesta = get("/informes/empresa/" + escenario.empresaSola);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Sin calificación general."));
        assertTrue(respuesta.body().contains("La empresa tiene una sola sede."));
    }

    @Test
    void empresaSinVisitasExplicaQueFaltaFinalizar() throws Exception {
        var respuesta = get("/informes/empresa/" + escenario.empresaVacia);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Finaliza al menos una visita"));
    }

    @Test
    void usuarioConsultaSoloSusCalificacionesYNoGeneraInformesPorDireccion() throws Exception {
        salir();
        entrar(usuario);
        var respuesta = get("/calificaciones?empresa=" + empresa);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertFalse(respuesta.body().contains("Generar informe"));
        assertFalse(respuesta.body().contains("/fotos/"));
        assertEquals(403, get("/calificaciones?empresa=" + escenario.empresaAjena).statusCode());
        assertEquals(403, get("/informes/empresa/" + empresa).statusCode());
        assertEquals(403, get("/informes/empresa/" + escenario.empresaAjena).statusCode());
        var detalle = get("/empresas/" + empresa);
        assertTrue(detalle.body().contains("Ver calificaciones"));
        assertFalse(detalle.body().contains("Generar informe"));
    }

    @Test
    void usuarioSinParametroVeSuEmpresaYAdministradorElige() throws Exception {
        assertTrue(get("/calificaciones").body().contains("Ver calificaciones"));
        salir();
        entrar(usuario);
        var respuesta = get("/calificaciones");
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("2 de 3 sedes calificadas"));
        assertFalse(respuesta.body().contains("Empresa ajena para informe"));
    }

    @Test
    void empresaYObservacionesSeEscapanEnElInforme() throws Exception {
        Sql.actualizar(con, "UPDATE empresa SET descripcion=? WHERE id_empresa=?", "<script>secreto</script>", empresa);
        var respuesta = get("/informes/empresa/" + empresa);
        assertTrue(respuesta.body().contains("&lt;script&gt;secreto&lt;/script&gt;"));
        assertFalse(respuesta.body().contains("<script>secreto"));
    }

    @Test
    void metodosInvalidosIdsYAccesoSinSesionProtegidos() throws Exception {
        assertEquals(400, get("/calificaciones?empresa=abc").statusCode());
        assertEquals(404, get("/informes/empresa/incorrecto").statusCode());
        String token = csrf(get("/empresas/" + empresa));
        assertEquals(405, post("/informes/empresa/" + empresa, Map.of("csrf", token)).statusCode());
        assertEquals(403, post("/informes/empresa/" + empresa, Map.of()).statusCode());
        post("/logout", Map.of("csrf", token));
        assertEquals(303, get("/informes/empresa/" + empresa).statusCode());
    }

    @Test
    void variasSedesPeroSoloUnaCalificadaMuestraFaltantesSinGeneral() throws Exception {
        Sql.actualizar(con, "UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l ON l.id_salon=" +
                "r.id_salon SET v.calificacion_final=NULL WHERE l.id_sede=?", escenario.sedeSur);
        var respuesta = get("/informes/empresa/" + empresa);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Sin calificación general."));
        assertTrue(respuesta.body().contains("1 de 3 sedes calificadas"));
        assertTrue(respuesta.body().contains("Sede Norte, Sede Sur"));
    }

    @Test
    void resumenDeEmpresaPrecedeAlDetalleConTotalesYNombresClaros() throws Exception {
        var respuesta = get("/informes/empresa/" + empresa);
        String texto = respuesta.body();
        assertEquals(200, respuesta.statusCode());
        int resumen = texto.indexOf("Resumen de la empresa"), detalle = texto.indexOf(
                "<section class=\"informe-sede\">");
        assertTrue(resumen > 0 && detalle > resumen);
        String portada = texto.substring(0, detalle);
        for (String texto2 : List.of("3,75", "2 de 3 sedes calificadas", "falta Sede Norte", "Visitas Finalizadas",
                "Puestos evaluados únicos", "Fotos analizadas", "Mobiliario: 1", "Postura: 2",
                "Área con más puestos en Rojo de la empresa"))
            assertTrue(portada.contains(texto2), texto2);
        assertTrue(texto.contains("Promedio histórico de sus visitas Finalizadas"));
        assertTrue(texto.contains("En el cuadro de imprimir, desmarca Encabezados y pies de página"));
    }

    @Test
    void singularEnCalificacionesYEnInformeDeUnaSede() throws Exception {
        String pantalla = get("/calificaciones?empresa=" + escenario.empresaSola).body(), informe = get(
                "/informes/empresa/" + escenario.empresaSola).body();
        assertTrue(pantalla.contains("1 visita Finalizada"));
        assertFalse(pantalla.contains("1 visitas Finalizadas"));
        for (String texto : List.of("1 de 1 sede calificada", "Puesto evaluado único", "Foto analizada",
                "Puesto en Amarillo"))
            assertTrue(informe.contains(texto), texto);
    }

    @Test
    void generadoElUsaRelojRealDeProduccionEnBogotaPorHttp() throws Exception {
        servidor.removeContext("/");

        // Exactamente el constructor de reloj usado por new Evaluaciones() en el Router real; solo la conexión es
        // reversible.
        servidor.createContext("/", new Router(auth, new co.aplomo.seguridad.Sesiones(reloj), servicioUsuarios(),
                new co.aplomo.servicio.Empresas(tx), new co.aplomo.servicio.Reservas(tx, reloj),
                new co.aplomo.servicio.Visitas(tx, reloj), new co.aplomo.servicio.Evaluaciones(tx)));
        entrar(admin);
        java.time.Instant antes = java.time.Instant.now();
        var respuesta = get("/informes/empresa/" + empresa);
        java.time.Instant despues = java.time.Instant.now();
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        var elemento = java.util.regex.Pattern.compile("data-generado datetime=\"([^\"]+)\"").matcher(respuesta.body());
        assertTrue(elemento.find());
        var fecha = java.time.OffsetDateTime.parse(elemento.group(1));
        assertEquals(java.time.ZoneOffset.ofHours(-5), fecha.getOffset());
        assertFalse(fecha.toInstant().isBefore(antes));
        assertFalse(fecha.toInstant().isAfter(despues));
        assertTrue(respuesta.body()
                .contains("Generado el " + Html.fechaHora(fecha.toLocalDateTime()) + " · America/Bogota"));
    }

    private void salir() throws Exception {
        post("/logout", Map.of("csrf", csrf(get("/empresas/" + empresa))));
    }
}
