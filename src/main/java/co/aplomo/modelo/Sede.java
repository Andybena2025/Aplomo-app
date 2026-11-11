package co.aplomo.modelo;

/** RF-004 a RF-010, RF-028: datos persistentes de sede, con estado conservado. */
public record Sede(
        int id,
        int empresa,
        String nombre,
        String direccion,
        String ciudad,
        String telefono,
        String horario,
        String descripcion,
        boolean activo) {}
