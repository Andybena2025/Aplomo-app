package co.aplomo.seguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class ContrasenasTest {
    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings={"abc123","sinumeros","12345678","Clave123\0"})
    void rechazaClavesInvalidas(String clave) { assertThrows(IllegalArgumentException.class,()->Contrasenas.cifrar(clave)); }

    @Test void bcryptConSalCostoYCompatibilidad() {
        var a=Contrasenas.cifrar("Valida2026"); var b=Contrasenas.cifrar("Valida2026");
        assertNotEquals(a,b); assertTrue(a.startsWith("$2a$10$"));
        assertTrue(Contrasenas.verificar("Valida2026",a));
        assertTrue(Contrasenas.verificar("Valida2026",a.replace("$2a$","$2b$")));
        assertFalse(Contrasenas.verificar("Otra2026",a));
        assertFalse(Contrasenas.verificar(null,a)); assertFalse(Contrasenas.verificar("Valida2026","hash inválido"));
    }

    @Test void evitaTruncamientoBcryptEnBytes() {
        String valida="a1"+"é".repeat(35);
        Contrasenas.validar(valida);
        assertThrows(IllegalArgumentException.class,()->Contrasenas.validar(valida+"é"));
        assertFalse(Contrasenas.verificar(valida+"é",Contrasenas.cifrar(valida)));
    }
}
