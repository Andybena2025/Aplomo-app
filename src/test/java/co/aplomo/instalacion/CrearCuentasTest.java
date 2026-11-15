package co.aplomo.instalacion;

import co.aplomo.servicio.*;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Importa por Usuarios en MySQL de pruebas, todo dentro del ROLLBACK del fixture. */
class CrearCuentasTest extends HttpPrueba {
    Usuarios usuarios(){return new Usuarios(tx,dao,new EmpresaDao());}
    Empresas empresas(){return new Empresas(tx);}
    List<CrearCuentas.Fila> ejemplo()throws Exception{
        String nit=empresas().empresa(cuenta(admin),empresa).nit();
        return CrearCuentas.leer(new StringReader("nombre,correo,rol,nit_empresa\nCompañera QA,equipo.admin@prueba.invalid,ADMINISTRADOR,\nCompañero QA,equipo.usuario@prueba.invalid,USUARIO,"+nit+"\n"));
    }
    String ejecutar(List<CrearCuentas.Fila> filas)throws Exception{
        var out=new StringWriter();assertEquals(0,CrearCuentas.crear(usuarios(),empresas(),cuenta(admin),filas,f->new CrearCuentas.Contacto("123456","Pruebas"),new PrintWriter(out)));return out.toString();
    }
    Map<String,String> claves(String tabla){var claves=new HashMap<String,String>();for(String linea:tabla.split("\n")){String[] p=linea.split(" \\| ",2);if(p.length==2&&p[0].endsWith("@prueba.invalid")&&!p[1].equals("ya existe"))claves.put(p[0],p[1]);}return claves;}
    @Test void csvUtf8PermiteComasComillasYNormalizaElCorreo()throws Exception{
        var filas=CrearCuentas.leer(new StringReader("\uFEFFnombre,correo,rol,nit_empresa\n\"Ana, \"\"QA\"\"\",ANA@PRUEBA.INVALID,ADMINISTRADOR,\n"));
        assertEquals("Ana, \"QA\"",filas.get(0).nombre());assertEquals("ana@prueba.invalid",filas.get(0).correo());
    }
    @ParameterizedTest @ValueSource(strings={"nombre,correo,rol\nAna,a@prueba.invalid,ADMINISTRADOR", "nombre,correo,rol,nit_empresa\nAna,a@prueba.invalid,OTRO,", "nombre,correo,rol,nit_empresa\nAna,a@prueba.invalid,USUARIO,", "nombre,correo,rol,nit_empresa\nAna,a@prueba.invalid,ADMINISTRADOR,NIT", "nombre,correo,rol,nit_empresa\nAna,correo-mal,ADMINISTRADOR,", "nombre,correo,rol,nit_empresa\nAna,a@prueba.invalid,ADMINISTRADOR,\nAna,A@PRUEBA.INVALID,ADMINISTRADOR,", "nombre,correo,rol,nit_empresa\n\"Ana,a@prueba.invalid,ADMINISTRADOR,"})
    void csvInvalidoSeRechazaAntesDeCrear(String csv){assertThrows(IllegalArgumentException.class,()->CrearCuentas.leer(new StringReader(csv)));}
    @Test void temporalEsLegibleLargaYConLetraYNumero(){
        var vistas=new HashSet<String>();for(int i=0;i<100;i++){String clave=CrearCuentas.temporal();assertEquals(16,clave.length());assertFalse(clave.matches(".*[0Ool1].*"));assertDoesNotThrow(()->Contrasenas.validar(clave));assertTrue(vistas.add(clave));}
    }
    @Test void creaAmbosRolesPorElServicioConBcryptYUnaSolaTabla()throws Exception{
        String tabla=ejecutar(ejemplo());var claves=claves(tabla);assertEquals(2,claves.size());
        for(var fila:ejemplo()){
            var cuenta=dao.porCorreo(con,fila.correo());String clave=claves.get(fila.correo());
            assertNotNull(cuenta);assertEquals(fila.rol(),cuenta.rol());assertEquals(fila.rol()==Rol.USUARIO?empresa:null,cuenta.empresa());
            assertTrue(cuenta.temporal());assertTrue(cuenta.hash().startsWith("$2a$10$"));assertTrue(Contrasenas.verificar(clave,cuenta.hash()));
            assertNotEquals(clave,cuenta.hash());assertEquals(1,tabla.split(java.util.regex.Pattern.quote(clave),-1).length-1);
        }
        assertEquals(1,tabla.split("Correo \\| Contraseña temporal / resultado",-1).length-1);
    }
    @Test void repetirNoDuplicaNiCambiaDatosClavesOEstados()throws Exception{
        var filas=ejemplo();ejecutar(filas);usuarios().estado(cuenta(admin),dao.porCorreo(con,"equipo.usuario@prueba.invalid").id(),false);var antes=new HashMap<String,co.aplomo.modelo.Cuenta>();for(var f:filas)antes.put(f.correo(),dao.porCorreo(con,f.correo()));
        int cantidad=usuarios().listar(cuenta(admin)).size();var out=new StringWriter();
        assertEquals(0,CrearCuentas.crear(usuarios(),empresas(),cuenta(admin),filas,f->{fail("No debe pedir datos de una cuenta existente");return null;},new PrintWriter(out)));
        assertEquals(cantidad,usuarios().listar(cuenta(admin)).size());for(var f:filas)assertEquals(antes.get(f.correo()),dao.porCorreo(con,f.correo()));
        assertEquals(2,out.toString().split("ya existe",-1).length-1);assertTrue(claves(out.toString()).isEmpty());
    }
    @Test void primerIngresoObligaACambiarLaClaveTemporal()throws Exception{
        var claves=claves(ejecutar(ejemplo()));var login=get("/login");var r=post("/login",Map.of("csrf",csrf(login),"correo","equipo.admin@prueba.invalid","clave",claves.get("equipo.admin@prueba.invalid")));
        assertEquals(303,r.statusCode());assertEquals("/cuenta/contrasena",r.headers().firstValue("Location").orElseThrow());
        assertEquals("/cuenta/contrasena",get("/").headers().firstValue("Location").orElseThrow());
    }
    @Test void usuarioNoPuedeImportar()throws Exception{
        assertThrows(SecurityException.class,()->CrearCuentas.crear(usuarios(),empresas(),cuenta(usuario),ejemplo(),f->null,new PrintWriter(new StringWriter())));
    }
    @Test void administradorTemporalDebeCambiarLaClaveAntesDeImportar()throws Exception{
        var filas=ejemplo();dao.contrasena(con,admin,cuenta(admin).hash(),true);
        var error=assertThrows(IllegalArgumentException.class,()->CrearCuentas.crear(usuarios(),empresas(),cuenta(admin),filas,f->null,new PrintWriter(new StringWriter())));
        assertTrue(error.getMessage().contains("Primero cambia tu contraseña temporal"));assertNull(dao.porCorreo(con,filas.get(0).correo()));
    }
    @Test void empresaInexistenteNoCreaNiPideContacto()throws Exception{
        var fila=new CrearCuentas.Fila("Prueba","sin.empresa@prueba.invalid",Rol.USUARIO,"NO-EXISTE-22");var out=new StringWriter();
        assertEquals(1,CrearCuentas.crear(usuarios(),empresas(),cuenta(admin),List.of(fila),f->{fail("No debe pedir contacto para una empresa inexistente");return null;},new PrintWriter(out)));
        assertNull(dao.porCorreo(con,fila.correo()));assertTrue(out.toString().contains("empresa no existe o está inactiva"));
    }
    @Test void telefonoYCargoObligatoriosNoSeInventan()throws Exception{
        var f=ejemplo().get(0);var out=new StringWriter();assertEquals(1,CrearCuentas.crear(usuarios(),empresas(),cuenta(admin),List.of(f),x->new CrearCuentas.Contacto("",""),new PrintWriter(out)));
        assertNull(dao.porCorreo(con,f.correo()));assertTrue(out.toString().contains("no creada"));
    }
}
