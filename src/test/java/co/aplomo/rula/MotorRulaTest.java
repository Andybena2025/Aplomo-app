package co.aplomo.rula;

import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import co.aplomo.rula.MedidorAngulos.Angulos;
import co.aplomo.rula.PuntuadorSegmentos.*;
import co.aplomo.rula.MotorRula.Carga;
import static org.junit.jupiter.api.Assertions.*;

/** RF-018 a RF-020, RNF-010: diez cálculos manuales con resultados esperados explícitos. */
class MotorRulaTest {
    private static final Ajustes SIN = Ajustes.sinAjustes();
    private static final Ajustes HOMBRO = new Ajustes(true, true, false, false, false, Giro.MEDIO,
            false, false, false, false, true, false);
    private static final Ajustes MAX = new Ajustes(true, true, false, true, true, Giro.EXTREMO,
            true, true, true, true, false, false);
    private static final Angulos NEUTRO = new Angulos(0, 90, 0, 0, 0);
    private static final Angulos EJEMPLO = new Angulos(61, 47, 0, 37, 17);

    record Caso(String nombre, Angulos g, Ajustes ajustes, boolean actividad, Carga carga,
                Puntos puntos, int a, int b, int ajustadoA, int ajustadoB, int fin, String color, int nivel) {}

    @TestFactory
    Stream<DynamicTest> diezCasosManuales() throws Exception {
        var motor = new MotorRula(DatosPrueba.tablasMySQL());
        return Stream.of(
            new Caso("1. Documento: verde", new Angulos(1,85,9,10,2), SIN, false, Carga.MENOS_2KG_INTERMITENTE,
                new Puntos(1,1,2,1,1,1,1),2,1,2,1,2,"VERDE",1),
            new Caso("2. Documento: amarillo", EJEMPLO, SIN, false, Carga.MENOS_2KG_INTERMITENTE,
                new Puntos(3,2,1,1,3,2,1),3,3,3,3,3,"AMARILLO",2),
            new Caso("3. Documento: hombro elevado y separado", EJEMPLO, HOMBRO, false, Carga.MENOS_2KG_INTERMITENTE,
                new Puntos(5,2,1,1,3,2,1),5,3,5,3,4,"AMARILLO",2),
            new Caso("4. Neutral", NEUTRO, SIN, false, Carga.MENOS_2KG_INTERMITENTE,
                new Puntos(1,1,1,1,1,1,1),1,1,1,1,1,"VERDE",1),
            new Caso("5. Actividad común", NEUTRO, SIN, true, Carga.MENOS_2KG_INTERMITENTE,
                new Puntos(1,1,1,1,1,1,1),1,1,2,2,2,"VERDE",1),
            new Caso("6. Carga intermitente 2–10", NEUTRO, SIN, false, Carga.DE_2_A_10KG_INTERMITENTE,
                new Puntos(1,1,1,1,1,1,1),1,1,2,2,2,"VERDE",1),
            new Caso("7. Carga estática 2–10", EJEMPLO, SIN, false, Carga.DE_2_A_10KG_ESTATICA_O_REPETITIVA,
                new Puntos(3,2,1,1,3,2,1),3,3,5,5,6,"ROJO",3),
            new Caso("8. Carga intermitente mayor de 10", EJEMPLO, SIN, true, Carga.MAS_10KG_INTERMITENTE,
                new Puntos(3,2,1,1,3,2,1),3,3,6,6,7,"ROJO",4),
            new Caso("9. Carga estática mayor de 10", NEUTRO, SIN, false, Carga.MAS_10KG_ESTATICA_O_REPETITIVA,
                new Puntos(1,1,1,1,1,1,1),1,1,4,4,4,"AMARILLO",2),
            new Caso("10. Máximos y fuerzas bruscas", new Angulos(120,20,30,-20,70), MAX, true, Carga.GOLPES_O_FUERZAS_BRUSCAS,
                new Puntos(6,3,4,2,6,6,2),9,9,13,13,7,"ROJO",4)
        ).map(c -> DynamicTest.dynamicTest(c.nombre(), () -> {
            var r = motor.calcular(c.g(), c.ajustes(), c.actividad(), c.carga());
            assertAll(() -> assertEquals(c.puntos(), r.segmentos()),
                    () -> assertEquals(c.a(), r.tablaA()), () -> assertEquals(c.b(), r.tablaB()),
                    () -> assertEquals(c.actividad() ? 1 : 0, r.actividad()),
                    () -> assertEquals(c.ajustadoA() - c.a() - r.actividad(), r.carga()),
                    () -> assertEquals(c.ajustadoA(), r.puntajeA()), () -> assertEquals(c.ajustadoB(), r.puntajeB()),
                    () -> assertEquals(c.fin(), r.puntajeFinal()), () -> assertEquals(c.color(), r.nivel().color()),
                    () -> assertEquals(c.nivel(), r.nivel().nivelOficialRula()));
        }));
    }

    @Test
    void noCalculaSiFaltanDatos() throws Exception {
        var motor = new MotorRula(DatosPrueba.tablasMySQL());
        assertThrows(NullPointerException.class, () -> motor.calcular(null, SIN, false, Carga.MENOS_2KG_INTERMITENTE));
        assertThrows(NullPointerException.class, () -> motor.calcular(NEUTRO, null, false, Carga.MENOS_2KG_INTERMITENTE));
        assertThrows(NullPointerException.class, () -> motor.calcular(NEUTRO, SIN, false, null));
        assertThrows(IllegalStateException.class, () -> new MotorRula(new TablasRula())
                .calcular(NEUTRO, SIN, false, Carga.MENOS_2KG_INTERMITENTE));
    }
}
