package co.aplomo.modelo;

/** RF-004 a RF-010, RF-028: datos persistentes de empresa, con estado conservado. */
public record Empresa(
        int id,
        int administrador,
        String nombre,
        String nit,
        String descripcion,
        String telefono,
        String correo,
        String contacto,
        boolean activo) {}
