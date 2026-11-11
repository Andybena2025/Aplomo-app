package co.aplomo.rula;

import java.util.Objects;
import co.aplomo.rula.MedidorAngulos.Angulos;

/** RF-018, RNF-010: única conversión de grados y ajustes a puntos de segmento. */
public final class PuntuadorSegmentos {
    private PuntuadorSegmentos() {}

    /** Giro observado de muñeca; su puntaje es independiente del ángulo (RF-018). */
    public enum Giro { MEDIO, EXTREMO }

    /** Casillas de un análisis; sentado bien apoyado permite el caso explícito de docs/03. */
    public record Ajustes(boolean hombroElevado, boolean brazoSeparado, boolean brazoApoyado,
                          boolean antebrazoCruza, boolean munecaDesviada, Giro giro,
                          boolean cuelloGirado, boolean cuelloInclinado, boolean troncoGirado,
                          boolean troncoInclinado, boolean piernasApoyadas, boolean sentadoBienApoyado) {
        /** Exige indicar el giro; no se infiere de la fotografía (RF-018). */
        public Ajustes {
            Objects.requireNonNull(giro, "Indica el giro de la muñeca.");
        }

        /** Devuelve casillas sin ajustes, giro medio y piernas apoyadas (RF-018). */
        public static Ajustes sinAjustes() {
            return new Ajustes(false, false, false, false, false, Giro.MEDIO,
                    false, false, false, false, true, false);
        }
    }

    /** Puntos de los siete componentes que entran a las Tablas A y B (RF-018). */
    public record Puntos(int brazo, int antebrazo, int muneca, int giro,
                         int cuello, int tronco, int piernas) {}

    /** Puntúa los ángulos medidos o corregidos a mano con todas sus casillas (RF-018). */
    public static Puntos puntuar(Angulos g, Ajustes a) {
        Objects.requireNonNull(g, "Faltan los ángulos.");
        Objects.requireNonNull(a, "Faltan los ajustes.");
        return new Puntos(brazo(g.brazo(), a.hombroElevado(), a.brazoSeparado(), a.brazoApoyado()),
                antebrazo(g.antebrazo(), a.antebrazoCruza()), muneca(g.muneca(), a.munecaDesviada()),
                a.giro() == Giro.MEDIO ? 1 : 2, cuello(g.cuello(), a.cuelloGirado(), a.cuelloInclinado()),
                tronco(g.tronco(), a.troncoGirado(), a.troncoInclinado(), a.sentadoBienApoyado()),
                a.piernasApoyadas() ? 1 : 2);
    }

    /** Puntúa brazo; el apoyo nunca puede reducir el resultado por debajo de 1 (RF-018). */
    public static int brazo(double grados, boolean elevado, boolean separado, boolean apoyado) {
        MedidorAngulos.validar(grados, -180, 180);
        int base = grados < -20 ? 2 : grados <= 20 ? 1 : grados <= 45 ? 2 : grados <= 90 ? 3 : 4;
        return Math.max(1, base + punto(elevado) + punto(separado) - punto(apoyado));
    }

    /** Puntúa antebrazo: el intervalo 60–100° incluye ambos límites (RF-018). */
    public static int antebrazo(double grados, boolean cruza) {
        MedidorAngulos.validar(grados, 0, 180);
        return (grados >= 60 && grados <= 100 ? 1 : 2) + punto(cruza);
    }

    /** Puntúa flexión o extensión de muñeca; ≤5° es neutro como en el mock (RF-018). */
    public static int muneca(double grados, boolean desviada) {
        MedidorAngulos.validar(grados, -180, 180);
        double magnitud = Math.abs(grados);
        return (magnitud <= MedidorAngulos.NEUTRO ? 1 : magnitud <= 15 ? 2 : 3) + punto(desviada);
    }

    /** Puntúa cuello; extensión mayor de 5° tiene base 4; la tolerancia del mock también rige a mano (RF-018). */
    public static int cuello(double grados, boolean girado, boolean inclinado) {
        MedidorAngulos.validar(grados, -180, 180);
        return (grados < -MedidorAngulos.NEUTRO ? 4 : grados <= 10 ? 1 : grados <= 20 ? 2 : 3)
                + punto(girado) + punto(inclinado);
    }

    /** Puntúa tronco por magnitud; ≤5° o sentado bien apoyado tiene base 1 como en el mock (RF-018). */
    public static int tronco(double grados, boolean girado, boolean inclinado, boolean sentadoBienApoyado) {
        MedidorAngulos.validar(grados, -180, 180);
        double magnitud = Math.abs(grados);
        return (magnitud <= MedidorAngulos.NEUTRO || sentadoBienApoyado ? 1 : magnitud <= 20 ? 2 : magnitud <= 60 ? 3 : 4)
                + punto(girado) + punto(inclinado);
    }

    private static int punto(boolean marcado) {
        return marcado ? 1 : 0;
    }
}
