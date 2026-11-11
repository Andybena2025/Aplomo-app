package co.aplomo.rula;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import co.aplomo.rula.MedidorAngulos.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-017, RNF-010: geometría conocida, reflejo, escalas, sentido y datos inválidos. */
class MedidorAngulosTest {
    private static final double EPS = 0.000001;
    private static final Punto CADERA = new Punto(100,300), HOMBRO = new Punto(100,200);

    @ParameterizedTest
    @CsvSource({"150,250,45", "50,250,-45", "100,250,0", "150,200,90", "150,150,135"})
    void brazoConSentidoYLadoEspejado(double x, double y, double esperado) {
        assertEquals(esperado, MedidorAngulos.brazo(CADERA,HOMBRO,new Punto(x,y),Lado.DERECHO),EPS);
        assertEquals(esperado, MedidorAngulos.brazo(CADERA,HOMBRO,new Punto(200-x,y),Lado.IZQUIERDO),EPS);
    }

    @ParameterizedTest
    @CsvSource({"150,150,45", "50,150,-45", "100,150,0", "150,200,90"})
    void cuelloConSentidoYLadoEspejado(double x, double y, double esperado) {
        assertEquals(esperado, MedidorAngulos.cuello(CADERA,HOMBRO,new Punto(x,y),Lado.DERECHO),EPS);
        assertEquals(esperado, MedidorAngulos.cuello(CADERA,HOMBRO,new Punto(200-x,y),Lado.IZQUIERDO),EPS);
    }

    @ParameterizedTest
    @CsvSource({"150,250,45", "50,250,-45", "100,250,0", "150,300,90", "150,350,135"})
    void troncoConSentidoYLadoEspejado(double x, double y, double esperado) {
        assertEquals(esperado, MedidorAngulos.tronco(CADERA,new Punto(x,y),Lado.DERECHO),EPS);
        assertEquals(esperado, MedidorAngulos.tronco(CADERA,new Punto(200-x,y),Lado.IZQUIERDO),EPS);
    }

    @ParameterizedTest
    @CsvSource({"0,100,0", "100,0,90", "100,100,45", "0,-100,180"})
    void articulacionesRectasYFlexionadas(double x, double y, double esperado) {
        Punto a = new Punto(0,-100), vertice = new Punto(0,0), fin = new Punto(x,y);
        assertEquals(esperado, MedidorAngulos.antebrazo(a,vertice,fin),EPS);
        assertEquals(esperado, MedidorAngulos.muneca(a,vertice,fin),EPS);
    }

    @Test
    void seisPuntosFormanUnAnalisisCompleto() throws Exception {
        var g = MedidorAngulos.medir(CADERA,HOMBRO,new Punto(100,250),new Punto(150,250),
                new Punto(200,250),new Punto(100,150),Lado.DERECHO);
        assertEquals(new Angulos(0,90,0,0,0),g);
        var r = new MotorRula(DatosPrueba.tablasMySQL()).calcular(g,
                PuntuadorSegmentos.Ajustes.sinAjustes(), false, MotorRula.Carga.MENOS_2KG_INTERMITENTE);
        assertEquals(1,r.puntajeFinal());
        assertEquals("VERDE",r.nivel().color());
    }

    @Test
    void escalaUniformeYTraslacionNoCambianGrados() {
        assertEquals(45,MedidorAngulos.brazo(new Punto(200,600),new Punto(200,400),new Punto(300,500),Lado.DERECHO),EPS);
        assertEquals(45,MedidorAngulos.brazo(new Punto(1100,1300),new Punto(1100,1200),new Punto(1150,1250),Lado.DERECHO),EPS);
        double g = MedidorAngulos.brazo(new Punto(1,3),new Punto(1,2),new Punto(1.01,2.9),Lado.DERECHO);
        assertTrue(g > 0 && g < 1, "Se conserva la precisión; no se redondea a neutro.");
    }

    @Test
    void medirEnUnLimiteNoCambiaElPuntajePorErrorDecimal() {
        Punto origen = new Punto(0,0), abajo = new Punto(0,100);
        for (double grados : new double[]{20,45,90}) {
            double rad = Math.toRadians(grados);
            var codo = new Punto(100*Math.sin(rad),100*Math.cos(rad));
            double medido = MedidorAngulos.brazo(abajo,origen,codo,Lado.DERECHO);
            assertEquals(PuntuadorSegmentos.brazo(grados,false,false,false),
                    PuntuadorSegmentos.brazo(medido,false,false,false));
        }
        for (double grados : new double[]{10,20,60}) {
            double rad = Math.toRadians(grados);
            var arriba = new Punto(100*Math.sin(rad),-100*Math.cos(rad));
            double cuello = MedidorAngulos.cuello(abajo,origen,arriba,Lado.DERECHO);
            double tronco = MedidorAngulos.tronco(origen,arriba,Lado.DERECHO);
            assertEquals(PuntuadorSegmentos.cuello(grados,false,false),
                    PuntuadorSegmentos.cuello(cuello,false,false));
            assertEquals(PuntuadorSegmentos.tronco(grados,false,false,false),
                    PuntuadorSegmentos.tronco(tronco,false,false,false));
        }
    }

    @Test
    void rechazaPuntosAusentesCoincidentesYNoFinitos() {
        assertThrows(NullPointerException.class, () -> MedidorAngulos.brazo(null,HOMBRO,CADERA,Lado.DERECHO));
        assertThrows(NullPointerException.class, () -> MedidorAngulos.tronco(CADERA,HOMBRO,null));
        assertThrows(IllegalArgumentException.class, () -> new Punto(Double.NaN,0));
        assertThrows(IllegalArgumentException.class, () -> new Punto(0,Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> MedidorAngulos.brazo(CADERA,HOMBRO,HOMBRO,Lado.DERECHO));
        assertThrows(IllegalArgumentException.class, () -> MedidorAngulos.antebrazo(HOMBRO,HOMBRO,CADERA));
        assertThrows(IllegalArgumentException.class, () -> MedidorAngulos.muneca(HOMBRO,CADERA,CADERA));
        assertThrows(IllegalArgumentException.class, () -> MedidorAngulos.cuello(CADERA,HOMBRO,HOMBRO,Lado.DERECHO));
        assertThrows(IllegalArgumentException.class, () -> MedidorAngulos.tronco(CADERA,CADERA,Lado.DERECHO));
    }
}
