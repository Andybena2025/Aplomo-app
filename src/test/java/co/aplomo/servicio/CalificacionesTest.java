package co.aplomo.servicio;

import co.aplomo.db.Sql;
import co.aplomo.modelo.InformeEmpresa;
import co.aplomo.modelo.InformeEmpresa.SedeResumen;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.soporte.EscenarioInformes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-025/026: promedios por nivel, cobertura, historial, privacidad y lectura sin modificaciones.
 */
class CalificacionesTest extends BasePrueba {
    EscenarioInformes escenario;
    Calificaciones servicio;

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioInformes(this);
        servicio = escenario.calificaciones;
    }

    @Test
    void promediaVisitasPorSedeYSedesSinPonderarPorCantidadDePuestos() throws Exception {
        var i = servicio.consultar(cuenta(admin), empresa);
        var centro = sede(i, escenario.sedeCentro);
        assertEquals(new BigDecimal("6.0"), centro.visitas().get(0).calificacion().promedio());
        assertEquals(new BigDecimal("3.0"), centro.visitas().get(1).calificacion().promedio());
        assertEquals(0, new BigDecimal("4.5").compareTo(centro.calificacion().promedio()));
        assertEquals(5, centro.calificacion().redondeado());
        assertEquals(3, centro.calificacion().riesgo().nivel());
        assertEquals(0, new BigDecimal("3.75").compareTo(i.general().promedio()));
        assertEquals(4, i.general().redondeado());
        assertEquals(2, i.general().riesgo().nivel());
        assertEquals(List.of("Sede Norte"), i.sedesSinCalificacion());
        assertEquals(3, i.sedes().size());
    }

    @Test
    void reevaluacionVerdeSustituyeRojoHistoricoYEnCursoNoInterviene() throws Exception {
        var centro = sede(servicio.consultar(cuenta(usuario), empresa), escenario.sedeCentro);
        assertEquals(2, centro.puestos().size());
        var empresa2 = centro.puestos().stream().filter(empresa3 -> empresa3.id() == escenario.puesto1).findFirst()
                .orElseThrow();
        assertEquals(escenario.evalActual, empresa2.evaluacion());
        assertEquals(2, empresa2.puntaje());
        assertEquals("VERDE", empresa2.riesgo().color());
        assertEquals(4, centro.estadisticas().fotosAnalizadas());
        assertEquals(1, centro.estadisticas().colores().get("VERDE"));
        assertEquals(1, centro.estadisticas().colores().get("AMARILLO"));
        assertEquals(0, centro.estadisticas().colores().getOrDefault("ROJO", 0));
        assertEquals(2, centro.visitas().size());
        assertFalse(centro.visitas().stream().anyMatch(visita2 -> visita2.id() == escenario.visitaAbierta));
    }

    @Test
    void cuentaPendientesPorEstadoYSugerenciasActivasPorTipoYEstado() throws Exception {
        var empresa2 = sede(servicio.informe(cuenta(admin), empresa), escenario.sedeCentro);
        var es = empresa2.estadisticas();
        assertEquals(Map.of("ABIERTO", 1, "ARREGLADO", 1), es.pendientes());
        assertEquals(2, empresa2.pendientes().size());
        assertEquals(3, empresa2.sugerencias().size());
        assertEquals(Map.of("HECHA", 1), es.sugerencias().get("MOBILIARIO"));
        assertEquals(Map.of("EN_PROCESO", 1, "PENDIENTE", 1), es.sugerencias().get("POSTURA"));
        assertFalse(es.sugerencias().containsKey("OTRA"));
        assertEquals(Map.of("Producción", 2), es.areas());
        assertEquals(Map.of("OPERATIVO", 2), es.cargos());
        assertTrue(es.areasMasRojo().isEmpty());
    }

    @Test
    void unaSolaSedeNoTieneGeneral() throws Exception {
        var i = servicio.informe(cuenta(admin), escenario.empresaSola);
        assertEquals(1, i.sedes().size());
        assertNotNull(i.sedes().get(0).calificacion());
        assertNull(i.general());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void menosDeDosSedesCalificadasNoGeneraGeneral(int calificadas) throws Exception {
        Sql.actualizar(con, "UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l ON l.id_salon=" +
                "r.id_salon SET v.calificacion_final=NULL WHERE l.id_sede=?", escenario.sedeSur);
        if (calificadas == 0) Sql.actualizar(con,
                "UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l ON l.id_salon=" +
                        "r.id_salon SET v.calificacion_final=NULL WHERE l.id_sede=?", escenario.sedeCentro);
        var i = servicio.consultar(cuenta(admin), empresa);
        assertNull(i.general());
        assertEquals(3 - calificadas, i.sedesSinCalificacion().size());
    }

    @Test
    void sedeSinDatosNoSeCuentaComoCeroNiOcultaSedesInactivas() throws Exception {
        Sql.actualizar(con, "UPDATE sede SET activo=FALSE WHERE id_sede=?", escenario.sedeNorte);
        var i = servicio.consultar(cuenta(admin), empresa);
        assertEquals(3, i.sedes().size());
        assertNull(sede(i, escenario.sedeNorte).calificacion());
        assertEquals(0, i.general().promedio().compareTo(new BigDecimal("3.75")));
    }

    @Test
    void empresaSinVisitasNoGeneraInformeYConsultarNoEscribe() throws Exception {
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM informe_empresa", fila -> fila.getInt(1));
        int bitacora = Sql.uno(con, "SELECT COUNT(*) FROM bitacora", fila2 -> fila2.getInt(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.informe(cuenta(admin), escenario.empresaVacia));
        servicio.consultar(cuenta(usuario), empresa);
        servicio.informe(cuenta(admin), empresa);
        assertEquals(antes, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM informe_empresa", fila3 -> fila3.getInt(1)));
        assertEquals(bitacora, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM bitacora", fila4 -> fila4.getInt(1)));
    }

    @Test
    void usuarioSoloSuEmpresaNuncaGeneraInformeYCuentaVigente() throws Exception {
        assertThrows(SecurityException.class, () -> servicio.consultar(cuenta(usuario), escenario.empresaAjena));
        assertThrows(SecurityException.class, () -> servicio.informe(cuenta(usuario), empresa));
        var actor = cuenta(usuario);
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", usuario);
        assertThrows(SecurityException.class, () -> servicio.consultar(actor, empresa));
    }

    @ParameterizedTest
    @CsvSource({"2.4,2.5,2.5,2.475,2,1", "2.5,2.5,2.5,2.5,3,2", "4.4,4.5,4.5,4.475,4,2", "4.5,4.5,4.5,4.5,5,3",
            "6.4," + "6.5,6.5,6.475,6,3", "6.5,6.5,6.5,6.5,7,4"})
    void colorUsaPromedioRealSinRedondearAntesLasSedes(String anterior, String reevaluacion, String sur,
            String esperado, int puntaje, int nivel) throws Exception {

        // Promedios históricos de una décima; el promedio de sedes conserva precisión antes del color.
        Sql.actualizar(con, "UPDATE visita SET calificacion_final=? WHERE id_visita=?", new BigDecimal(anterior),
                escenario.visitaAnterior);
        Sql.actualizar(con, "UPDATE visita SET calificacion_final=? WHERE id_visita=?", new BigDecimal(reevaluacion),
                escenario.visitaReevaluacion);
        Sql.actualizar(con, "UPDATE visita SET calificacion_final=? WHERE id_visita=?", new BigDecimal(sur),
                escenario.visitaSur);
        var i = servicio.consultar(cuenta(admin), empresa);
        assertEquals(0, new BigDecimal(esperado).compareTo(i.general().promedio()));
        assertEquals(puntaje, i.general().redondeado());
        assertEquals(nivel, i.general().riesgo().nivel());
    }

    @Test
    void ultimaEvaluacionSinAnalisisNoRecuperaUnRojoAnteriorYFotoSinAnalisisNoSeCuenta() throws Exception {
        int ultima = evaluacion(escenario.visitaReevaluacion, escenario.salonCentro, 1);
        foto(ultima);
        var centro = sede(servicio.consultar(cuenta(admin), empresa), escenario.sedeCentro);
        var empresa2 = centro.puestos().stream().filter(empresa3 -> empresa3.id() == escenario.puesto1).findFirst()
                .orElseThrow();
        assertEquals(ultima, empresa2.evaluacion());
        assertNull(empresa2.puntaje());
        assertNull(empresa2.riesgo());
        assertEquals(4, centro.estadisticas().fotosAnalizadas());
    }

    @Test
    void totalesDeEmpresaSumanHistorialYEstadoActualSinDuplicarPuestos() throws Exception {
        var i = servicio.informe(cuenta(admin), empresa);
        var empresa2 = i.totales();
        assertEquals(3, empresa2.visitas());
        assertEquals(3, empresa2.puestos());
        assertEquals(5, empresa2.fotosAnalizadas());
        assertEquals(Map.of("VERDE", 1, "AMARILLO", 2), empresa2.colores());
        assertEquals(Map.of("ABIERTO", 1, "ARREGLADO", 1), empresa2.pendientes());
        assertEquals(Map.of("HECHA", 1), empresa2.sugerencias().get("MOBILIARIO"));
        assertEquals(Map.of("EN_PROCESO", 1, "PENDIENTE", 1), empresa2.sugerencias().get("POSTURA"));
    }

    @Test
    void areaConMasRojoSeCalculaEnTodaLaEmpresaNoDesdeGanadorasDeCadaSede() throws Exception {
        int produccion = Sql.uno(con, "SELECT id_area FROM area WHERE id_empresa=? AND nombre=?",
                fila -> fila.getInt(1), empresa, "Producción");
        int logistica = Sql.insertar(con, "INSERT INTO area(id_empresa,nombre) VALUES(?,?)", empresa, "Logística");
        int oficina = Sql.insertar(con, "INSERT INTO area(id_empresa,nombre) VALUES(?,?)", empresa, "Oficina");
        for (int puesto : new int[]{escenario.puesto1, escenario.puesto2, escenario.puestoSur}) {
            Sql.actualizar(con, "UPDATE puesto_trabajo SET id_area=? WHERE id_puesto=?", produccion, puesto);
            int foto = Sql.uno(con, "SELECT f.id_foto FROM fotografia f JOIN evaluacion ev USING(id_evaluacion) J" +
                    "OIN visita v USING(id_visita) WHERE ev.id_puesto=? AND v.estado='FINALIZADA'" +
                    " ORDER BY ev.id_evaluacion DESC LIMIT 1", fila2 -> fila2.getInt(1), puesto);
            analisis(foto, 5);
        }
        int salonSur = Sql.uno(con, "SELECT id_salon FROM puesto_trabajo WHERE id_puesto=?", fila3 -> fila3.getInt(1),
                escenario.puestoSur);
        agregarRojo(escenario.visitaSur, salonSur, 2, produccion);
        for (int numero = 10; numero < 13; numero++) {
            agregarRojo(escenario.visitaReevaluacion, escenario.salonCentro, numero, logistica);
            agregarRojo(escenario.visitaSur, salonSur, numero, oficina);
        }
        var i = servicio.informe(cuenta(admin), empresa);
        assertEquals(List.of("Logística"), sede(i, escenario.sedeCentro).estadisticas().areasMasRojo());
        assertEquals(List.of("Oficina"), sede(i, escenario.sedeSur).estadisticas().areasMasRojo());
        assertEquals(List.of("Producción"), i.totales().areasMasRojo());
    }

    private void agregarRojo(int visita, int salon, int numero, int area) throws Exception {
        int ev = evaluacion(visita, salon, numero);
        Sql.actualizar(con, "UPDATE puesto_trabajo p JOIN evaluacion e USING(id_puesto) SET p.id_area=? W" +
                "HERE e.id_evaluacion=?", area, ev);
        analisis(foto(ev), 5);
    }

    private SedeResumen sede(InformeEmpresa i, int id) {
        return i.sedes().stream().filter(empresa2 -> empresa2.sede().id() == id).findFirst().orElseThrow();
    }
}
