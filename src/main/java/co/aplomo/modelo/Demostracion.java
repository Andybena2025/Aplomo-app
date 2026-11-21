package co.aplomo.modelo;

/**
 * Paso 17: separa la práctica repetible de las consultas de operación, sin borrar datos. RNF-011.
 */
public final class Demostracion {
    public static final String NIT_PRACTICA = "DEMO-APLOMO-PRACTICA";

    private Demostracion() {
    }

    /**
     * Fragmento SQL constante; la columna proviene exclusivamente del código del DAO. RNF-011.
     */
    public static String filtro(String columna) {
        return " AND " + columna + " IN (SELECT id_empresa FROM empresa WHERE nit <> '" + NIT_PRACTICA + "')";
    }
}
