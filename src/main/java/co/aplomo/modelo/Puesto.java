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
        boolean activo,String color,int nivel) {
    /** Datos editables del puesto; el color y nivel se completan al consultar MySQL (RF-020). */
    public Puesto(int id,int salon,Integer area,Integer cargo,int numero,String descripcion,String tipo,String ubicacion,Integer puntaje,boolean activo){this(id,salon,area,cargo,numero,descripcion,tipo,ubicacion,puntaje,activo,null,0);}
}
