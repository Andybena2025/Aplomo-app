package co.aplomo.servicio;

import java.text.Normalizer;
import java.util.Locale;

/** RNF-004: validaciones del servidor compartidas por configuración y agenda. */
public final class Validacion {
    private Validacion() {}

    /** Limpia espacios y comprueba un campo obligatorio dentro del límite del esquema. */
    public static String texto(String valor, String campo, int maximo) {
        String limpio = opcional(valor, campo, maximo);
        if (limpio.isEmpty()) throw new IllegalArgumentException("Completa el campo " + campo + ".");
        return limpio;
    }

    /** Valida texto opcional sin permitir controles ni superar la longitud de MySQL. */
    public static String opcional(String valor, String campo, int maximo) {
        String limpio = valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
        if (limpio.codePointCount(0, limpio.length()) > maximo
                || limpio.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Revisa " + campo + ": permite hasta " + maximo + " caracteres.");
        }
        return limpio;
    }

    /** Valida un correo opcional con la misma longitud autorizada en el esquema. */
    public static String correo(String valor) {
        String limpio = opcional(valor, "correo", 120).toLowerCase(Locale.ROOT);
        if (!limpio.isEmpty() && !limpio.matches("[^\\s@<>\\\"]+@[^\\s@<>\\\"]+\\.[^\\s@<>\\\"]+")) {
            throw new IllegalArgumentException("Escribe un correo válido.");
        }
        return limpio;
    }

    /** Comprueba un entero y sus límites antes de enviar el dato a MySQL. */
    public static int entero(String texto, String campo, int minimo, int maximo) {
        try {
            int numero = Integer.parseInt(texto);
            if (numero >= minimo && numero <= maximo) return numero;
        } catch (NumberFormatException e) { /* Se devuelve el mismo aviso para entradas inválidas. */ }
        throw new IllegalArgumentException("Revisa " + campo + ": usa un número entre " + minimo + " y " + maximo + ".");
    }

    /** Comprueba una opción contra las alternativas aprobadas, sin aceptar valores arbitrarios. */
    public static String opcion(String valor, String campo, String... opciones) {
        for (String opcion : opciones) if (opcion.equals(valor)) return valor;
        throw new IllegalArgumentException("Elige una opción válida para " + campo + ".");
    }

    /** Conserva los párrafos de las notas y respeta el límite real de bytes del campo TEXT. */
    public static String notas(String valor) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65535
                || texto.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
            throw new IllegalArgumentException("Las notas superan el tamaño permitido o contienen caracteres no válidos.");
        }
        return texto;
    }

    /** RF-010: compara nombres sin mayúsculas, tildes ni espacios repetidos. */
    public static String nombre(String valor) {
        return Normalizer.normalize(valor.strip().replaceAll("\\s+", " "), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** RF-010: elimina el sufijo societario y los espacios para avisar de empresas similares. */
    public static String nombreEmpresa(String valor) {
        return nombre(valor).replaceFirst("[ ,]+(?:s\\.?\\s*a\\.?\\s*s\\.?|ltda\\.?|s\\.?\\s*a\\.?)$", "")
                .replaceAll("\\s+", "");
    }
}
