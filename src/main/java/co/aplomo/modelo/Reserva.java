package co.aplomo.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * RF-009b, RF-011, RF-012: reserva persistida con código, horario y estado.
 */
public record Reserva(int id, int salon, int administrador, String codigo, LocalDate fecha, LocalTime inicio,
                      LocalTime fin, String categoria, String descripcion, String notas, String estado, String motivo,
                      Integer evaluador) {

    /**
     * RNF-011: compatibilidad con reservas legadas sin asignación directa.
     */
    public Reserva(int id, int salon, int administrador, String codigo, LocalDate fecha, LocalTime inicio,
            LocalTime fin, String categoria, String descripcion, String notas, String estado, String motivo) {
        this(id, salon, administrador, codigo, fecha, inicio, fin, categoria, descripcion, notas, estado, motivo, null);
    }

    /**
     * Indica si los datos del horario pueden editarse según los estados aprobados. RNF-011.
     */
    public boolean editable() {
        return estado.equals("ESPERANDO") || estado.equals("EN_COLA");
    }
}
