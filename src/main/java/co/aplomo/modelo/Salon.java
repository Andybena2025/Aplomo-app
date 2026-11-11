package co.aplomo.modelo;

/** RF-004 a RF-010, RF-028: datos persistentes de salon, con estado conservado. */
public record Salon(
        int id,
        int sede,
        String nombre,
        String descripcion,
        int piso,
        int capacidad,
        boolean activo) {}
