package co.aplomo.rula;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import co.aplomo.rula.MedidorAngulos.Angulos;
import co.aplomo.rula.PuntuadorSegmentos.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-018, RNF-010: límites exactos, valores decimales y ajustes de todos los segmentos. */
class PuntuadorSegmentosTest {
    @ParameterizedTest
    @CsvSource({"-180,2", "-20.01,2", "-20,1", "0,1", "20,1", "20.01,2", "45,2", "45.01,3", "90,3", "90.01,4", "180,4"})
    void limitesBrazo(double g, int esperado) {
        assertEquals(esperado, PuntuadorSegmentos.brazo(g,false,false,false));
    }

    @ParameterizedTest
    @CsvSource({"0,2", "59.99,2", "60,1", "100,1", "100.01,2", "180,2"})
    void limitesAntebrazo(double g, int esperado) {
        assertEquals(esperado, PuntuadorSegmentos.antebrazo(g,false));
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0.01,2", "2,2", "4.99,2", "5,2", "15,2", "15.01,3", "180,3"})
    void limitesMunecaEnAmbosSentidos(double g, int esperado) {
        assertEquals(esperado, PuntuadorSegmentos.muneca(g,false));
        assertEquals(esperado, PuntuadorSegmentos.muneca(-g,false));
    }

    @ParameterizedTest
    @CsvSource({"-180,4", "-0.01,4", "0,1", "10,1", "10.01,2", "20,2", "20.01,3", "180,3"})
    void limitesCuello(double g, int esperado) {
        assertEquals(esperado, PuntuadorSegmentos.cuello(g,false,false));
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0.01,2", "2,2", "4.99,2", "5,2", "20,2", "20.01,3", "60,3", "60.01,4", "180,4"})
    void limitesTroncoEnAmbosSentidos(double g, int esperado) {
        assertEquals(esperado, PuntuadorSegmentos.tronco(g,false,false,false));
        assertEquals(esperado, PuntuadorSegmentos.tronco(-g,false,false,false));
    }

    @Test
    void ajustesSumanYElApoyoNoProduceCero() {
        assertEquals(1, PuntuadorSegmentos.brazo(0,false,false,true));
        assertEquals(2, PuntuadorSegmentos.brazo(0,true,true,true));
        assertEquals(5, PuntuadorSegmentos.brazo(120,true,true,true));
        assertEquals(2, PuntuadorSegmentos.antebrazo(90,true));
        assertEquals(3, PuntuadorSegmentos.antebrazo(30,true));
        assertEquals(2, PuntuadorSegmentos.muneca(0,true));
        assertEquals(4, PuntuadorSegmentos.muneca(30,true));
        assertEquals(6, PuntuadorSegmentos.cuello(-10,true,true));
        assertEquals(6, PuntuadorSegmentos.tronco(70,true,true,false));
        assertEquals(3, PuntuadorSegmentos.tronco(17,true,true,true));
        var a = new Ajustes(false,false,false,false,false,Giro.EXTREMO,false,false,false,false,false,true);
        assertEquals(new Puntos(1,1,1,2,1,1,2), PuntuadorSegmentos.puntuar(new Angulos(0,90,0,0,17),a));
    }

    @Test
    void cadaCasillaEsIndependiente() {
        assertEquals(2, PuntuadorSegmentos.brazo(0,true,false,false));
        assertEquals(2, PuntuadorSegmentos.brazo(0,false,true,false));
        assertEquals(2, PuntuadorSegmentos.cuello(0,true,false));
        assertEquals(2, PuntuadorSegmentos.cuello(0,false,true));
        assertEquals(2, PuntuadorSegmentos.tronco(0,true,false,false));
        assertEquals(2, PuntuadorSegmentos.tronco(0,false,true,false));
    }

    @ParameterizedTest
    @CsvSource({"NaN", "Infinity", "-Infinity", "180.01", "-180.01"})
    void rechazaAngulosImposibles(double g) {
        assertThrows(IllegalArgumentException.class, () -> PuntuadorSegmentos.brazo(g,false,false,false));
        assertThrows(IllegalArgumentException.class, () -> PuntuadorSegmentos.antebrazo(g,false));
        assertThrows(IllegalArgumentException.class, () -> PuntuadorSegmentos.muneca(g,false));
        assertThrows(IllegalArgumentException.class, () -> PuntuadorSegmentos.cuello(g,false,false));
        assertThrows(IllegalArgumentException.class, () -> PuntuadorSegmentos.tronco(g,false,false,false));
        for (int i = 0; i < 5; i++) {
            double[] valores = {0,90,0,0,0};
            valores[i] = g;
            assertThrows(IllegalArgumentException.class,
                    () -> new Angulos(valores[0],valores[1],valores[2],valores[3],valores[4]));
        }
    }
}
