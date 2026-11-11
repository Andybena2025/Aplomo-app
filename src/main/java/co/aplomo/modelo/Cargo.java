package co.aplomo.modelo;

/** RF-004 a RF-010, RF-028: datos persistentes de cargo, con estado conservado. */
public record Cargo(
        int id,
        int empresa,
        String nombre,
        String tipo,
        boolean activo) {}
