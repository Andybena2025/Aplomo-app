package co.aplomo.modelo;

/**
 * RF-004/028, Paso 27: datos de empresa y dueño, sin inferir su sector.
 */
public record Empresa(int id, int administrador, String nombre, String nit, String descripcion, String telefono,
                      String correo, String contacto, boolean activo, Integer creador, String sector) {
    /**
     * RNF-011: conserva clientes anteriores; el servicio determina el creador real.
     */
    public Empresa(int id, int administrador, String nombre, String nit, String descripcion, String telefono,
            String correo, String contacto, boolean activo) {
        this(id, administrador, nombre, nit, descripcion, telefono, correo, contacto, activo, null, null);
    }
}
