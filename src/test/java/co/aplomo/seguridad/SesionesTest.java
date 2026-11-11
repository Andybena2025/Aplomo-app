package co.aplomo.seguridad;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import co.aplomo.soporte.RelojPrueba;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class SesionesTest {
    private final RelojPrueba reloj=new RelojPrueba();
    private final Sesiones sesiones=new Sesiones(reloj);
    private final Cuenta c=new Cuenta(1,2,"Nombre","a@b.co","1","Cargo","hash",false,0,null,Estado.ACTIVA,Rol.USUARIO);

    @Test void idsYCsrfIndependientesDe32Bytes() {
        var a=sesiones.crear(c); var b=sesiones.crear(c);
        assertEquals(32,Base64.getUrlDecoder().decode(a.id()).length);
        assertEquals(32,Base64.getUrlDecoder().decode(a.csrf()).length);
        assertNotEquals(a.id(),a.csrf()); assertNotEquals(a.id(),b.id());
        assertTrue(Sesiones.csrfValido(a,a.csrf())); assertFalse(Sesiones.csrfValido(a,b.csrf()));
        assertFalse(Sesiones.csrfValido(null,a.csrf())); assertFalse(Sesiones.csrfValido(a,null));
    }

    @Test void expiraExactamenteALos30Minutos() {
        var s=sesiones.crear(c); reloj.avanzar(Duration.ofMinutes(30));
        assertNull(sesiones.obtener(s.id()));
    }

    @Test void actividadRenuevaElTiempo() {
        var s=sesiones.crear(c); reloj.avanzar(Duration.ofMinutes(29));
        assertNotNull(sesiones.obtener(s.id())); reloj.avanzar(Duration.ofMinutes(29));
        assertNotNull(sesiones.obtener(s.id()));
    }

    @Test void cerrarEInvalidarTodasLasSesiones() {
        var a=sesiones.crear(c); var b=sesiones.crear(c);
        sesiones.cerrar(a.id()); assertNull(sesiones.obtener(a.id()));
        sesiones.invalidarCuenta(c.id()); assertNull(sesiones.obtener(b.id()));
    }

    @Test void permisosPorRolEmpresaYTemporal() {
        assertThrows(SecurityException.class,()->Permisos.administrador(c));
        assertTrue(Permisos.empresa(c,2)); assertFalse(Permisos.empresa(c,3)); assertFalse(Permisos.empresa(null,2));
        var admin=new Cuenta(2,null,"A","a@b.co","1","C","hash",false,0,null,Estado.ACTIVA,Rol.ADMINISTRADOR);
        assertDoesNotThrow(()->Permisos.administrador(admin)); assertTrue(Permisos.empresa(admin,99));
        var temporal=new Cuenta(2,null,"A","a@b.co","1","C","hash",true,0,null,Estado.ACTIVA,Rol.ADMINISTRADOR);
        assertThrows(SecurityException.class,()->Permisos.administrador(temporal));
        assertFalse(Permisos.empresa(temporal,99));
    }
}
