package co.aplomo.modelo;

import java.util.Map;
import java.util.Set;

/** RF-014, RF-002: contexto voluntario, separado de las entradas del motor RULA. */
public record Trabajador(String genero, Integer edad, String antiguedad) {
    /** (propuesta) RF-002: única política de visibilidad de los datos del trabajador. */
    public static final Set<Cuenta.Rol> ROLES_VISIBLES = Set.of(Cuenta.Rol.ADMINISTRADOR, Cuenta.Rol.EVALUADOR);

    /** RF-014: valida también las entradas construidas fuera de la pantalla. */
    public Trabajador {
        genero = opcion(genero, Set.of("FEMENINO", "MASCULINO", "OTRO", "NO_DICE"), "género");
        antiguedad = opcion(antiguedad, Set.of("MENOS_1", "DE_1_A_5", "DE_6_A_10", "MAS_10"), "antigüedad");
        if (edad != null && (edad < 14 || edad > 99))
            throw new IllegalArgumentException("La edad debe estar entre 14 y 99 años.");
    }

    /** RF-014: campos vacíos siguen siendo ausencia de dato, nunca valores supuestos. */
    public static Trabajador leer(Map<String, String> f) {
        String texto = f.getOrDefault("trabajador_edad", "").strip();
        Integer edad = null;
        if (!texto.isEmpty()) {
            try { edad = Integer.valueOf(texto); }
            catch (NumberFormatException error) {
                throw new IllegalArgumentException("La edad debe ser un número entero entre 14 y 99 años.");
            }
        }
        return new Trabajador(f.get("trabajador_genero"), edad, f.get("trabajador_antiguedad"));
    }

    /** RF-002: redacta el contexto antes de cualquier respuesta al Usuario. */
    public Trabajador para(Cuenta actor) {
        return ROLES_VISIBLES.contains(actor.rol()) ? this : new Trabajador(null, null, null);
    }

    /** RF-014: descarta solamente espacios opcionales y rechaza ENUM ajenos. */
    private static String opcion(String valor, Set<String> permitidas, String nombre) {
        if (valor == null || valor.isBlank()) return null;
        if (!permitidas.contains(valor))
            throw new IllegalArgumentException("Elige una opción válida de " + nombre + ".");
        return valor;
    }
}
