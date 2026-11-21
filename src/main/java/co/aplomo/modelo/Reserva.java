package co.aplomo.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * RF-009b, RF-011, RF-012: reserva persistida con código, horario y estado.
 */
public record Reserva(int id, int salon, int administrador, String codigo, LocalDate fecha, LocalTime inicio,
                      LocalTime fin, String categoria, String descripcion, String notas, String estado, String motivo) {

    /**
     * Indica si los datos del horario pueden editarse según los estados aprobados. RNF-011.
     */
    public boolean editable() {
        return estado.equals("ESPERANDO") || estado.equals("EN_COLA");
    }
}
