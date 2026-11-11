package co.aplomo.modelo;

import java.time.LocalDateTime;

/** RF-001a, RF-002, RF-003: cuenta persistida; nunca se entrega el hash al navegador. */
public record Cuenta(int id, Integer empresa, String nombre, String correo, String telefono, String cargo,
                     String hash, boolean temporal, int intentos, LocalDateTime bloqueadoHasta,
                     Estado estado, Rol rol) {
    /** Roles aprobados; no existe registro libre ni rol Evaluador (RF-002). */
    public enum Rol { ADMINISTRADOR, USUARIO }
    /** Estado de acceso; el bloqueo temporal conserva su fecha de vencimiento (RF-001a). */
    public enum Estado { ACTIVA, INACTIVA, BLOQUEADA }

    /** Representa la cuenta sin exponer su hash ni datos de contacto (RNF-004). */
    @Override public String toString() { return "Cuenta[id=" + id + ", rol=" + rol + "]"; }
}
