package co.aplomo.servicio;

import co.aplomo.modelo.Trabajador;
import co.aplomo.rula.*;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.soporte.CasosRulaPrueba;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** RF-018/027: proyección independiente de datos personales y del cálculo actual guardado. */
class PlanAtencion40Test extends BasePrueba {
    private MotorRula motor;
    private PlanAtencion40 plan;

    private void cargar() throws Exception {
        var tablas = new TablasRula(); tablas.cargar(con);
        motor = new MotorRula(tablas); plan = new PlanAtencion40(tablas);
    }

    @Test
    void seisBajaConMetasPeroConservaActividadCargaYGiro() throws Exception {
        cargar(); var entrada = CasosRulaPrueba.todos().get(6).entrada();
        var actual = motor.calcular(entrada); var proyectado = plan.proyectar(actual);
        assertEquals(6, actual.resultado().puntajeFinal());
        assertEquals(3, proyectado.resultado().puntajeFinal());
        assertEquals(entrada.actividad(), proyectado.entrada().actividad());
        assertEquals(entrada.carga(), proyectado.entrada().carga());
        assertEquals(entrada.ajustes().giro(), proyectado.entrada().ajustes().giro());
        assertEquals(6, motor.calcular(entrada).resultado().puntajeFinal());
        assertEquals(1, proyectado.resultado().segmentos().brazo());
        assertEquals(1, proyectado.resultado().segmentos().cuello());
    }

    @Test
    void minimoNoCambiaYMetasCoincidenConMotorExacto() throws Exception {
        cargar(); var c = motor.calcular(CasosRulaPrueba.todos().get(3).entrada());
        assertEquals(c.resultado().puntajeFinal(), plan.proyectar(c).resultado().puntajeFinal());
        assertEquals(c.resultado().segmentos(), plan.proyectar(c).resultado().segmentos());
        assertTrue(PlanAtencion40.METAS.get("MUNECA").rango().contains("5°"));
        assertTrue(PlanAtencion40.METAS.get("TRONCO").rango().contains("5°"));
        assertTrue(PlanAtencion40.orden(c).stream().allMatch(k -> PlanAtencion40.color(c, k).equals("VERDE")));
    }

    @Test
    void ordenPorPesoYHastaTresRojasPorEncimaDelMinimo() throws Exception {
        cargar(); var c = motor.calcular(CasosRulaPrueba.todos().get(9).entrada());
        var orden = PlanAtencion40.orden(c); var puntos = PlanAtencion40.puntos(c);
        for (int i = 1; i < orden.size(); i++) assertTrue(
                (double) puntos.get(orden.get(i - 1)) / PlanAtencion40.METAS.get(orden.get(i - 1)).maximo() >=
                (double) puntos.get(orden.get(i)) / PlanAtencion40.METAS.get(orden.get(i)).maximo());
        assertEquals(3, orden.stream().filter(k -> PlanAtencion40.color(c, k).equals("ROJO")).count());
        assertTrue(orden.stream().anyMatch(k -> PlanAtencion40.color(c, k).equals("AMARILLO")));
    }

    @Test
    void proyeccionHistoricaRestauraTambienSegmentosQueYaEstabanEnElMinimo() throws Exception {
        cargar(); var c = motor.calcular(CasosRulaPrueba.todos().get(6).entrada());
        var e = c.entrada();
        var historico = motor.restaurar(new MotorRula.Entrada(e.lado(), 1, 1, Map.of(), e.ajustes(),
                e.actividad(), e.carga()), c.mediciones(), c.resultado().puntajeA(),
                c.resultado().puntajeB(), c.resultado().puntajeFinal());
        assertEquals(3, plan.proyectar(historico).resultado().puntajeFinal());
        assertEquals(1, plan.proyectar(historico).resultado().segmentos().muneca());
    }

    @Test
    void edadesYEnumeradosInvalidosSeRechazanSinValoresSupuestos() {
        assertEquals(new Trabajador(null, null, null), Trabajador.leer(Map.of()));
        for (String valor : List.of("13", "100", "2.5", "abc", "-1")) assertThrows(IllegalArgumentException.class,
                () -> Trabajador.leer(Map.of("trabajador_edad", valor)));
        for (int edad : List.of(14, 99)) assertEquals(edad,
                Trabajador.leer(Map.of("trabajador_edad", "" + edad)).edad());
        assertThrows(IllegalArgumentException.class, () -> new Trabajador("inventado", 30, "MENOS_1"));
        assertThrows(IllegalArgumentException.class, () -> new Trabajador("OTRO", 30, "inventado"));
    }

    @Test
    void datosDelTrabajadorNuncaCambianPuntajeNiProyeccion() throws Exception {
        // RNF-010: el contrato del motor tampoco admite contexto personal como entrada.
        assertTrue(java.util.Arrays.stream(MotorRula.Entrada.class.getRecordComponents())
                .noneMatch(c -> c.getType() == Trabajador.class || c.getName().startsWith("trabajador")));
        cargar(); var entrada = CasosRulaPrueba.todos().get(6).entrada();
        for (var trabajador : List.of(new Trabajador("FEMENINO", 14, "MENOS_1"),
                new Trabajador("MASCULINO", 99, "MAS_10"), new Trabajador(null, null, null))) {
            assertNotNull(trabajador);
            var c = motor.calcular(entrada);
            assertEquals(6, c.resultado().puntajeFinal());
            assertEquals(3, plan.proyectar(c).resultado().puntajeFinal());
        }
    }
}
