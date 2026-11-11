package co.aplomo.modelo;

/** RF-004 a RF-010, RF-028: datos persistentes de puesto_trabajo, con estado conservado. */
public record Puesto(
        int id,
        int salon,
        Integer area,
        Integer cargo,
        int numero,
        String descripcion,
        String tipo,
        String ubicacion,
        Integer puntaje,
        boolean activo) {}
