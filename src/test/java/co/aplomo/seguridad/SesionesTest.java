package co.aplomo.seguridad;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.soporte.RelojPrueba;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SesionesTest {
    private final RelojPrueba reloj = new RelojPrueba();
    private final Sesiones sesiones = new Sesiones(reloj);
    private final Cuenta cuenta = new Cuenta(1, 2, "Nombre", "a@b.co", "1", "Cargo", "hash", false, 0, null,
            Estado.ACTIVA, Rol.USUARIO);

    @Test
    void idsYCsrfIndependientesDe32Bytes() {
        var sesion = sesiones.crear(cuenta);
        var sesion2 = sesiones.crear(cuenta);
        assertEquals(32, Base64.getUrlDecoder().decode(sesion.id()).length);
        assertEquals(32, Base64.getUrlDecoder().decode(sesion.csrf()).length);
        assertNotEquals(sesion.id(), sesion.csrf());
        assertNotEquals(sesion.id(), sesion2.id());
        assertTrue(Sesiones.csrfValido(sesion, sesion.csrf()));
        assertFalse(Sesiones.csrfValido(sesion, sesion2.csrf()));
        assertFalse(Sesiones.csrfValido(null, sesion.csrf()));
        assertFalse(Sesiones.csrfValido(sesion, null));
    }

    @Test
    void expiraExactamenteALos30Minutos() {
        var sesion = sesiones.crear(cuenta);
        reloj.avanzar(Duration.ofMinutes(30));
        assertNull(sesiones.obtener(sesion.id()));
    }

    @Test
    void actividadRenuevaElTiempo() {
        var sesion = sesiones.crear(cuenta);
        reloj.avanzar(Duration.ofMinutes(29));
        assertNotNull(sesiones.obtener(sesion.id()));
        reloj.avanzar(Duration.ofMinutes(29));
        assertNotNull(sesiones.obtener(sesion.id()));
    }

    @Test
    void cerrarEInvalidarTodasLasSesiones() {
        var sesion = sesiones.crear(cuenta);
        var sesion2 = sesiones.crear(cuenta);
        sesiones.cerrar(sesion.id());
        assertNull(sesiones.obtener(sesion.id()));
        sesiones.invalidarCuenta(cuenta.id());
        assertNull(sesiones.obtener(sesion2.id()));
    }

    @Test
    void permisosPorRolEmpresaYTemporal() {
        assertThrows(SecurityException.class, () -> Permisos.administrador(cuenta));
        assertThrows(SecurityException.class, () -> Permisos.trabajo(cuenta));
        assertThrows(SecurityException.class, () -> Permisos.trabajo(null));
        var admin = new Cuenta(2, null, "A", "a@b.co", "1", "C", "hash", false, 0, null, Estado.ACTIVA,
                Rol.ADMINISTRADOR);
        assertDoesNotThrow(() -> Permisos.administrador(admin));
        assertDoesNotThrow(() -> Permisos.trabajo(admin));
        var temporal = new Cuenta(2, null, "A", "a@b.co", "1", "C", "hash", true, 0, null, Estado.ACTIVA,
                Rol.ADMINISTRADOR);
        assertThrows(SecurityException.class, () -> Permisos.administrador(temporal));
        assertThrows(SecurityException.class, () -> Permisos.trabajo(temporal));
    }
}
