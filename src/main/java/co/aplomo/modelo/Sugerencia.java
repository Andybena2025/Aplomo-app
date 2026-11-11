package co.aplomo.modelo;
import java.time.LocalDate;
/** RF-023a/b: mejora guardada, con origen e historial conservados al quitarla. */
public record Sugerencia(int id,int evaluacion,Integer regla,String titulo,String descripcion,String tipo,String prioridad,String estado,LocalDate limite,String origen,boolean activo) {}
