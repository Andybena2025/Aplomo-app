package co.aplomo.servicio;

import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacionTest extends BasePrueba {
    @Test void ingresoRealYNormalizacionDelCorreo() throws Exception {
        var c=cuenta(admin);
        assertEquals(admin,auth.ingresar("  "+c.correo().toUpperCase()+"  ",clave).id());
        assertNull(auth.ingresar("no-existe@prueba.invalid",clave));
        assertNull(auth.ingresar("' OR 1=1 --",clave));
    }

    @Test void cincoIntentosBloqueanQuinceMinutosSinExtenderElPlazo() throws Exception {
        String correo=cuenta(admin).correo();
        for(int i=1;i<=5;i++) { assertNull(auth.ingresar(correo,"Incorrecta9")); assertEquals(i,cuenta(admin).intentos()); }
        var bloqueada=cuenta(admin); assertEquals(Estado.BLOQUEADA,bloqueada.estado());
        reloj.avanzar(Duration.ofMinutes(14)); assertNull(auth.ingresar(correo,clave));
        assertEquals(bloqueada.bloqueadoHasta(),cuenta(admin).bloqueadoHasta());
        reloj.avanzar(Duration.ofMinutes(1)); assertNotNull(auth.ingresar(correo,clave));
        assertEquals(0,cuenta(admin).intentos()); assertNull(cuenta(admin).bloqueadoHasta());
    }

    @Test void despuesDelBloqueoUnErrorEmpiezaDeNuevoEnUno() throws Exception {
        String correo=cuenta(admin).correo();
        for(int i=0;i<5;i++) auth.ingresar(correo,"Incorrecta9");
        reloj.avanzar(Duration.ofMinutes(15)); assertNull(auth.ingresar(correo,"Incorrecta9"));
        assertEquals(1,cuenta(admin).intentos()); assertEquals(Estado.ACTIVA,cuenta(admin).estado());
    }

    @Test void ingresoCorrectoReiniciaIntentosYUnaInactivaNuncaEntra() throws Exception {
        String correo=cuenta(admin).correo(); auth.ingresar(correo,"Incorrecta9");
        assertNotNull(auth.ingresar(correo,clave)); assertEquals(0,cuenta(admin).intentos());
        dao.acceso(con,admin,0,null,Estado.INACTIVA); assertNull(auth.ingresar(correo,clave));
        assertEquals(Estado.INACTIVA,cuenta(admin).estado());
    }

    @Test void cambiaTemporalVerificaActualYGuardaSoloBcrypt() throws Exception {
        var c=auth.ingresar(cuenta(usuario).correo(),clave); assertTrue(c.temporal());
        assertThrows(IllegalArgumentException.class,()->auth.cambiar(c,"Errada123","Nueva2026","Nueva2026"));
        assertThrows(IllegalArgumentException.class,()->auth.cambiar(c,clave,"Nueva2026","Distinta2026"));
        assertThrows(IllegalArgumentException.class,()->auth.cambiar(c,clave,"12345678","12345678"));
        assertThrows(IllegalArgumentException.class,()->auth.cambiar(c,clave,clave,clave));
        var cambiada=auth.cambiar(c,clave,"Nueva2026","Nueva2026");
        assertFalse(cambiada.temporal()); assertTrue(Contrasenas.verificar("Nueva2026",cuenta(usuario).hash()));
        assertNull(auth.ingresar(c.correo(),clave)); assertNotNull(auth.ingresar(c.correo(),"Nueva2026"));
        assertThrows(SecurityException.class,()->auth.cambiar(c,"Nueva2026","Otra2026","Otra2026"));
    }
}
