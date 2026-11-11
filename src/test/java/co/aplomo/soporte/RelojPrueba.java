package co.aplomo.soporte;

import java.time.*;

/** Reloj de pruebas: comprueba minutos reales sin esperas ni cambios al reloj del sistema. */
public final class RelojPrueba extends Clock {
    private Instant ahora=Instant.parse("2026-11-10T15:00:00Z");
    @Override public ZoneId getZone() { return ZoneId.of("America/Bogota"); }
    @Override public Clock withZone(ZoneId zona) { return Clock.fixed(ahora,zona); }
    @Override public Instant instant() { return ahora; }
    public void avanzar(Duration tiempo) { ahora=ahora.plus(tiempo); }
}
