package co.aplomo.seguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContrasenasTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc123", "sinumeros", "12345678", "Clave123\0"})
    void rechazaClavesInvalidas(String clave) {
        assertThrows(IllegalArgumentException.class, () -> Contrasenas.cifrar(clave));
    }

    @Test
    void bcryptConSalCostoYCompatibilidad() {
        var texto = Contrasenas.cifrar("Valida2026");
        var texto2 = Contrasenas.cifrar("Valida2026");
        assertNotEquals(texto, texto2);
        assertTrue(texto.startsWith("$2a$10$"));
        assertTrue(Contrasenas.verificar("Valida2026", texto));
        assertTrue(Contrasenas.verificar("Valida2026", texto.replace("$2a$", "$2b$")));
        assertFalse(Contrasenas.verificar("Otra2026", texto));
        assertFalse(Contrasenas.verificar(null, texto));
        assertFalse(Contrasenas.verificar("Valida2026", "hash inválido"));
    }

    @Test
    void evitaTruncamientoBcryptEnBytes() {
        String valida = "a1" + "é".repeat(35);
        Contrasenas.validar(valida);
        assertThrows(IllegalArgumentException.class, () -> Contrasenas.validar(valida + "é"));
        assertFalse(Contrasenas.verificar(valida + "é", Contrasenas.cifrar(valida)));
    }
}
