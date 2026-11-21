package co.aplomo.servicio;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RF-001c: editar un dato conserva los demás y no permite ampliar permisos.
 */
class CuentaCampoTest extends BasePrueba {
    @Test
    void editarNombreNoExigeTelefonoNiSobrescribeOtrosDatos() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET telefono='' WHERE id_cuenta=?", admin);
        var anterior = cuenta(admin);
        var actual = new CuentaPropia(tx, reloj).guardarCampo(anterior, "nombre", "Nombre de prueba");
        assertEquals("Nombre de prueba", actual.nombre());
        assertEquals(anterior.telefono(), actual.telefono());
        assertEquals(anterior.cargo(), actual.cargo());
        assertEquals(anterior.correo(), actual.correo());
        assertEquals(anterior.rol(), actual.rol());
    }

    @Test
    void agregaTelefonoConservandoNombreYCargo() throws Exception {
        var anterior = cuenta(admin);
        var actual = new CuentaPropia(tx, reloj).guardarCampo(anterior, "telefono", "606 123 4567");
        assertEquals("606 123 4567", actual.telefono());
        assertEquals(anterior.nombre(), actual.nombre());
        assertEquals(anterior.cargo(), actual.cargo());
    }

    @Test
    void rechazaColumnasArbitrariasYDatosInvalidos() throws Exception {
        var servicio = new CuentaPropia(tx, reloj);
        var actor = cuenta(admin);
        for (String campo : new String[]{"rol", "correo", "id_empresa", "nombre=? WHERE 1=1"}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.guardarCampo(actor, campo, "valor"));
        }
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarCampo(actor, "nombre", " "));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarCampo(actor, "cargo", "x".repeat(81)));
        assertEquals(actor, cuenta(admin));
    }
}
