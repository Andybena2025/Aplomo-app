package co.aplomo.seguridad;

import java.nio.charset.StandardCharsets;
import org.mindrot.jbcrypt.BCrypt;

/** RNF-004: bcrypt de costo 10 y validación compartida por creación y cambio. */
public final class Contrasenas {
    private Contrasenas() {}
    public static final int COSTO = 10;

    /** Exige ocho caracteres, letra y número; evita truncamiento bcrypt por encima de 72 bytes. */
    public static void validar(String clave) {
        if (clave == null || clave.codePointCount(0,clave.length()) < 8
                || !clave.codePoints().anyMatch(Character::isLetter)
                || !clave.codePoints().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres, una letra y un número.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > 72 || clave.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("La contraseña es demasiado larga o contiene caracteres no permitidos.");
        }
    }

    /** Genera un hash con sal nueva; no guarda la contraseña original (RNF-004). */
    public static String cifrar(String clave) {
        validar(clave);
        return BCrypt.hashpw(clave,BCrypt.gensalt(COSTO));
    }

    /** Verifica también los hashes $2b$ existentes sin modificar el valor guardado (RNF-004). */
    public static boolean verificar(String clave, String hash) {
        if (clave == null || hash == null || clave.indexOf('\0') >= 0
                || clave.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        String compatible = hash.startsWith("$2b$") ? "$2a$" + hash.substring(4) : hash;
        try { return BCrypt.checkpw(clave,compatible); }
        catch (IllegalArgumentException e) { return false; }
    }
}
