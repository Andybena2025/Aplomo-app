package co.aplomo.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RF-013a, RF-013b: visita real, sus horas, notas y promedio de los puestos evaluados. */
public record Visita(int id, int reserva, LocalDateTime inicio, LocalDateTime fin,
                     String notas, BigDecimal calificacion, String estado) {}
