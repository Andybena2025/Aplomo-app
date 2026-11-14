package co.aplomo.instalacion;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Datos reales de MySQL solo en la base de pruebas, revertidos por BasePrueba. */
class DemoTest extends BasePrueba {
    @TempDir Path archivos;
    private void cargar()throws Exception{Demo.cargar(con,Path.of("docs/base_de_datos/datos_demo.sql"));}
    @Test void cadaEmpresaTieneSedesConAmbosTiposYUnaVisitaListaSinDuplicar()throws Exception{
        cargar();cargar();
        for(int i=1;i<=3;i++){
            int empresaDemo=Sql.uno(con,"SELECT id_empresa FROM empresa WHERE nit=?",r->r.getInt(1),"DEMO-APLOMO-E"+i);
            var sedes=Sql.listar(con,"SELECT id_sede FROM sede WHERE id_empresa=?",r->r.getInt(1),empresaDemo);
            assertTrue(sedes.size()>=2);
            for(int sede:sedes){
                var tipos=Sql.listar(con,"SELECT DISTINCT p.tipo FROM puesto_trabajo p JOIN salon l USING(id_salon) WHERE l.id_sede=? AND p.activo=TRUE",r->r.getString(1),sede);
                assertTrue(tipos.containsAll(List.of("ESCRITORIO","MESA")));
            }
            var visitas=Sql.listar(con,"SELECT v.id_visita,r.id_salon FROM visita v JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE s.id_empresa=? AND v.estado='EN_CURSO'",r->new int[]{r.getInt(1),r.getInt(2)},empresaDemo);
            assertEquals(1,visitas.size());
            assertEquals(2,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM puesto_trabajo p WHERE p.id_salon=? AND p.activo=TRUE AND NOT EXISTS(SELECT 1 FROM evaluacion e WHERE e.id_visita=? AND e.id_puesto=p.id_puesto)",r->r.getInt(1),visitas.get(0)[1],visitas.get(0)[0]));
        }
    }
    @Test void sinConfirmacionNoHaceNingunaConsultaDeBorrado()throws Exception{
        assertThrows(IllegalArgumentException.class,()->Demo.vaciar(con,"sí"));
        assertNotNull(cuenta(admin));assertNotNull(cuenta(usuario));
    }
    @Test void cuentaVinculadaCancelaAntesDeBorrarYNoTocaCuentas()throws Exception{
        cargar();int demo=Sql.uno(con,"SELECT id_empresa FROM empresa WHERE nit='DEMO-APLOMO-E1'",r->r.getInt(1));
        Sql.actualizar(con,"UPDATE cuenta SET id_empresa=? WHERE id_cuenta=?",demo,usuario);
        int antes=Sql.uno(con,"SELECT COUNT(*) FROM empresa",r->r.getInt(1));
        var e=assertThrows(IllegalArgumentException.class,()->Demo.vaciar(con,Demo.CONFIRMACION));assertTrue(e.getMessage().contains("cuentas vinculadas"));
        assertEquals(antes,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM empresa",r->r.getInt(1)));
        assertEquals(demo,cuenta(usuario).empresa());assertNotNull(cuenta(admin));
    }
    @Test void usuarioDeAppNoEsAdministradorMysql()throws Exception{
        assertThrows(IllegalArgumentException.class,()->Demo.verificarAdministrador(con));
    }
    @Test void limpiezaSoloRetiraFotosEnumeradasSinReferenciasYDentroDeLaCarpeta()throws Exception{
        Path fotos=Files.createDirectory(archivos.resolve("fotos"));
        for(String nombre:List.of("vieja.jpg","compartida.jpg","ajena.jpg"))Files.writeString(fotos.resolve(nombre),"ficticio");
        Path fuera=archivos.resolve("fuera.jpg");Files.writeString(fuera,"ajena");
        Files.createSymbolicLink(fotos.resolve("enlace.jpg"),fuera);
        var salon=crearSalon();int reserva=new co.aplomo.servicio.Reservas(tx,reloj).guardar(cuenta(admin),0,solicitud(salon));
        Sql.actualizar(con,"UPDATE reserva SET estado='EN_COLA' WHERE id_reserva=?",reserva);
        int visita=Sql.insertar(con,"INSERT INTO visita(id_reserva,hora_real_inicio) VALUES (?,NOW())",reserva);
        int ev=evaluacion(visita,salon.id(),1);int foto=foto(ev);Sql.actualizar(con,"UPDATE fotografia SET ruta=? WHERE id_foto=?","compartida.jpg",foto);
        assertEquals(1,Demo.limpiarFotos(con,fotos,List.of("vieja.jpg","compartida.jpg","../fuera.jpg",fuera.toString(),"enlace.jpg")));
        assertFalse(Files.exists(fotos.resolve("vieja.jpg")));assertTrue(Files.exists(fotos.resolve("compartida.jpg")));assertTrue(Files.exists(fotos.resolve("ajena.jpg")));assertTrue(Files.exists(fuera));assertTrue(Files.isSymbolicLink(fotos.resolve("enlace.jpg")));
    }
    @Test void codigoReservadoEnEmpresaAjenaSeRechazaSinCargarDemo()throws Exception{
        var salon=crearSalon();int reserva=new co.aplomo.servicio.Reservas(tx,reloj).guardar(cuenta(admin),0,solicitud(salon));
        Sql.actualizar(con,"UPDATE reserva SET codigo='DEMO-FIN-01' WHERE id_reserva=?",reserva);
        assertThrows(IllegalArgumentException.class,this::cargar);
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM empresa WHERE nit='DEMO-APLOMO-E1'",r->r.getInt(1)));
    }
}
