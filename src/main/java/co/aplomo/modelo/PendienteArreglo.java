package co.aplomo.modelo;

import java.time.LocalDate;

/** RF-022: un pendiente por evaluación; los datos de seguimiento se completan después. */
public record PendienteArreglo(int id,int evaluacion,String descripcion,String prioridad,LocalDate limite,
                              int puntaje,LocalDate registro,String estado) {}
