package co.aplomo.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * RNF-004 (propuesta): límite de envíos por dirección remota, sin guardarla ni registrarla.
 */
public final class LimiteSolicitudes {
    public static final int MAXIMO = 5;
    private static final int MAXIMO_DIRECCIONES = 5000;
    private static final Duration VENTANA = Duration.ofHours(1);
    private final Clock reloj;
    private final Map<String, ArrayDeque<Instant>> envios = new HashMap<>();

    /**
     * RNF-011: reloj sustituible para comprobar la ventana sin esperas.
     */
    public LimiteSolicitudes(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * RNF-004: no confía en X-Forwarded-For enviado por el navegador.
     */
    public synchronized void comprobar(String direccion) {
        Instant ahora = reloj.instant();
        Instant desde = ahora.minus(VENTANA);
        envios.values().forEach(fila -> {
            while (!fila.isEmpty() && !fila.peekFirst().isAfter(desde)) fila.removeFirst();
        });
        envios.values().removeIf(ArrayDeque::isEmpty);
        if (envios.size() >= MAXIMO_DIRECCIONES && !envios.containsKey(direccion)) throw new Limite();
        var fila = envios.computeIfAbsent(direccion, clave -> new ArrayDeque<>());
        if (fila.size() >= MAXIMO) throw new Limite();
        fila.addLast(ahora);
    }

    /**
     * RNF-007: error claro y distinguible como HTTP 429, sin detalles técnicos.
     */
    public static final class Limite extends IllegalArgumentException {
        public Limite() {
            super("Ya recibimos varios envíos desde tu conexión. Inténtalo dentro de una hora.");
        }
    }
}
