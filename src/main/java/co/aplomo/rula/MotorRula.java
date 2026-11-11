package co.aplomo.rula;

import java.util.Objects;
import co.aplomo.rula.MedidorAngulos.Angulos;
import co.aplomo.rula.PuntuadorSegmentos.Ajustes;
import co.aplomo.rula.PuntuadorSegmentos.Puntos;
import co.aplomo.rula.TablasRula.NivelRiesgo;

/** RF-018, RF-019, RF-020, RNF-010: cálculo puro; no guarda análisis ni altera MySQL. */
public final class MotorRula {
    private final TablasRula tablas;

    /** Recibe las tablas previamente cargadas desde MySQL (RF-027). */
    public MotorRula(TablasRula tablas) {
        this.tablas = Objects.requireNonNull(tablas, "Faltan las tablas RULA.");
    }

    /** Las seis cargas autorizadas, con los mismos nombres del enum de analisis_rula (RF-019). */
    public enum Carga {
        MENOS_2KG_INTERMITENTE(0), DE_2_A_10KG_INTERMITENTE(1),
        DE_2_A_10KG_ESTATICA_O_REPETITIVA(2), MAS_10KG_INTERMITENTE(2),
        MAS_10KG_ESTATICA_O_REPETITIVA(3), GOLPES_O_FUERZAS_BRUSCAS(3);

        private final int puntos;
        Carga(int puntos) { this.puntos = puntos; }

        /** Devuelve el incremento que se aplica a ambos grupos (RF-019). */
        public int puntos() { return puntos; }
    }

    /** Desglose previo a guardar; puntajeA y B conservan el total sin limitar a la Tabla C. */
    public record Resultado(Puntos segmentos, int tablaA, int tablaB, int actividad, int carga,
                             int puntajeA, int puntajeB, int puntajeFinal, NivelRiesgo nivel) {}

    /** Calcula segmentos, A y B, suma una actividad/carga común y consulta C y riesgo (RF-018 a RF-020). */
    public Resultado calcular(Angulos angulos, Ajustes ajustes, boolean estaticaORepetitiva, Carga carga) {
        Objects.requireNonNull(carga, "Elige la carga o fuerza.");
        Puntos p = PuntuadorSegmentos.puntuar(angulos, ajustes);
        int a = tablas.buscarA(p.brazo(), p.antebrazo(), p.muneca(), p.giro());
        int b = tablas.buscarB(p.cuello(), p.tronco(), p.piernas());
        int actividad = estaticaORepetitiva ? 1 : 0;
        int ajustadoA = a + actividad + carga.puntos();
        int ajustadoB = b + actividad + carga.puntos();
        int fin = tablas.buscarC(ajustadoA, ajustadoB);
        return new Resultado(p, a, b, actividad, carga.puntos(), ajustadoA, ajustadoB, fin, tablas.nivelDe(fin));
    }
}
