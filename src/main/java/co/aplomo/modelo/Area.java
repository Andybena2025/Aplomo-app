package co.aplomo.modelo;

/** RF-004 a RF-010, RF-028: datos persistentes de area, con estado conservado. */
public record Area(
        int id,
        int empresa,
        String nombre,
        boolean activo) {}
