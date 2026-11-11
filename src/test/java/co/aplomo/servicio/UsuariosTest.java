package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UsuariosTest extends BasePrueba {
    private Usuarios usuarios;
    @BeforeEach void servicio() { usuarios=new Usuarios(tx,dao,new EmpresaDao()); }
    private Usuarios.Datos datos(String correo,Rol rol,Integer empresa) {
        return new Usuarios.Datos(" Nombre nuevo ",correo," 456 ","Contacto",rol,empresa);
    }
    private String correoNuevo() { return UUID.randomUUID()+"@prueba.invalid"; }

    @Test void creaUsuarioAsignadoConTemporalYAdministradorIndependiente() throws Exception {
        int id=usuarios.crear(cuenta(admin),datos(correoNuevo(),Rol.USUARIO,empresa),"Temporal2026");
        Cuenta c=cuenta(id); assertEquals(empresa,c.empresa()); assertTrue(c.temporal());
        assertEquals(Rol.USUARIO,c.rol()); assertEquals(Estado.ACTIVA,c.estado());
        assertEquals("Nombre nuevo",c.nombre()); assertTrue(Contrasenas.verificar("Temporal2026",c.hash()));
        int independiente=usuarios.crear(cuenta(admin),datos(correoNuevo(),Rol.ADMINISTRADOR,null),"Temporal2026");
        assertNull(cuenta(independiente).empresa()); assertEquals(Rol.ADMINISTRADOR,cuenta(independiente).rol());
    }

    @Test void noRepiteCorreoNiEnCuentaInactiva() throws Exception {
        String correo=cuenta(usuario).correo();
        for(Estado estado:List.of(Estado.ACTIVA,Estado.INACTIVA)) {
            dao.acceso(con,usuario,0,null,estado);
            assertThrows(IllegalArgumentException.class,()->usuarios.crear(cuenta(admin),datos("  "+correo.toUpperCase()+"  ",Rol.USUARIO,empresa),"Temporal2026"));
        }
        assertThrows(IllegalArgumentException.class,()->usuarios.editar(cuenta(admin),usuario,datos(cuenta(admin).correo(),Rol.USUARIO,empresa),""));
    }

    @Test void editaSinCambiarIdEstadoNiClaveYPermiteElPropioCorreo() throws Exception {
        Cuenta antes=cuenta(usuario);
        usuarios.editar(cuenta(admin),usuario,datos(antes.correo(),Rol.USUARIO,empresa),"");
        Cuenta despues=cuenta(usuario);
        assertEquals(antes.id(),despues.id()); assertEquals(antes.hash(),despues.hash());
        assertEquals(antes.estado(),despues.estado()); assertEquals("Nombre nuevo",despues.nombre());
        usuarios.editar(cuenta(admin),usuario,datos(correoNuevo(),Rol.ADMINISTRADOR,null),"");
        assertEquals(Rol.ADMINISTRADOR,cuenta(usuario).rol()); assertNull(cuenta(usuario).empresa());
    }

    @Test void desactivaSinBorrarYReactiva() throws Exception {
        Cuenta antes=cuenta(usuario);
        usuarios.estado(cuenta(admin),usuario,false);
        assertEquals(Estado.INACTIVA,cuenta(usuario).estado()); assertEquals(antes.empresa(),cuenta(usuario).empresa());
        assertNull(auth.ingresar(antes.correo(),clave));
        usuarios.estado(cuenta(admin),usuario,true);
        assertNotNull(auth.ingresar(antes.correo(),clave)); assertEquals(0,cuenta(usuario).intentos());
    }

    @Test void asignaTemporalSinConservarClaveAnterior() throws Exception {
        Cuenta antes=cuenta(usuario);
        usuarios.editar(cuenta(admin),usuario,datos(antes.correo(),Rol.USUARIO,empresa),"Restablecida2026");
        assertTrue(cuenta(usuario).temporal()); assertFalse(Contrasenas.verificar(clave,cuenta(usuario).hash()));
        assertNotNull(auth.ingresar(antes.correo(),"Restablecida2026"));
    }

    @Test void rechazaEmpresaInexistenteYRolDeUsuarioEnServicio() throws Exception {
        assertThrows(IllegalArgumentException.class,()->usuarios.crear(cuenta(admin),datos(correoNuevo(),Rol.USUARIO,Integer.MAX_VALUE),"Temporal2026"));
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);
        assertThrows(SecurityException.class,()->usuarios.listar(cuenta(usuario)));
        assertThrows(SecurityException.class,()->usuarios.crear(cuenta(usuario),datos(correoNuevo(),Rol.ADMINISTRADOR,null),"Temporal2026"));
        assertThrows(SecurityException.class,()->usuarios.editar(cuenta(usuario),admin,datos(correoNuevo(),Rol.ADMINISTRADOR,null),""));
        assertThrows(SecurityException.class,()->usuarios.estado(cuenta(usuario),admin,false));
        assertThrows(SecurityException.class,()->usuarios.empresas(cuenta(usuario)));
    }

    @Test void elServicioReleeElPermisoYNoConfiaEnElActorRecibido() throws Exception {
        Cuenta c=cuenta(usuario);
        Cuenta falso=new Cuenta(c.id(),null,c.nombre(),c.correo(),c.telefono(),c.cargo(),c.hash(),false,0,null,Estado.ACTIVA,Rol.ADMINISTRADOR);
        assertThrows(SecurityException.class,()->usuarios.listar(falso));
        Cuenta anterior=cuenta(admin); dao.acceso(con,admin,0,null,Estado.INACTIVA);
        assertThrows(SecurityException.class,()->usuarios.crear(anterior,datos(correoNuevo(),Rol.ADMINISTRADOR,null),"Temporal2026"));
    }

    @Test void ultimoAdministradorNoSeDesactivaNiSeDemueveEnTransaccion() throws Exception {
        // Simula una instalación con un único administrador mediante el conjunto de fixtures.
        // Se bloquean las filas reales con el mismo SQL; ninguna cuenta preexistente se modifica.
        CuentaDao soloFixture=new CuentaDao() {
            @Override public List<Cuenta> bloquearAdministradores(java.sql.Connection conexion) throws java.sql.SQLException {
                return super.bloquearAdministradores(conexion).stream().filter(c->c.id()==admin).toList();
            }
        };
        Usuarios limitado=new Usuarios(tx,soloFixture,new EmpresaDao());
        assertThrows(IllegalArgumentException.class,()->limitado.estado(cuenta(admin),admin,false));
        assertThrows(IllegalArgumentException.class,()->limitado.editar(cuenta(admin),admin,datos(correoNuevo(),Rol.USUARIO,empresa),""));
        assertEquals(Estado.ACTIVA,cuenta(admin).estado()); assertEquals(Rol.ADMINISTRADOR,cuenta(admin).rol());
        assertDoesNotThrow(()->Usuarios.conservarAdministrador(cuenta(admin),Rol.USUARIO,Estado.ACTIVA,2));
    }

    @ParameterizedTest @ValueSource(strings={"nombreVacio","correoInvalido","correoLargo","telefonoLargo","cargoVacio","sinEmpresa","empresaNegativa","adminConEmpresa","sinRol"})
    void validaDatosAntesDeGuardar(String fallo) {
        String nombre="Nombre",correo="a@b.co",telefono="123",cargo="Contacto"; Rol rol=Rol.USUARIO; Integer e=empresa;
        switch(fallo) {
            case "nombreVacio" -> nombre="  "; case "correoInvalido" -> correo="noesuncorreo";
            case "correoLargo" -> correo="a".repeat(121)+"@b.co"; case "telefonoLargo" -> telefono="1".repeat(21);
            case "cargoVacio" -> cargo=""; case "sinEmpresa" -> e=null; case "empresaNegativa" -> e=-1;
            case "adminConEmpresa" -> rol=Rol.ADMINISTRADOR; case "sinRol" -> rol=null;
            default -> throw new AssertionError();
        }
        String n=nombre,c=correo,t=telefono,g=cargo; Rol r=rol; Integer id=e;
        assertThrows(IllegalArgumentException.class,()->new Usuarios.Datos(n,c,t,g,r,id));
    }
}
