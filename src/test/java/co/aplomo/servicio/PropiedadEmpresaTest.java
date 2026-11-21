package co.aplomo.servicio;

import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Area;
import co.aplomo.modelo.Cargo;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-002/028: propiedad de Andy, con MySQL de pruebas y reversión al terminar.
 */
class PropiedadEmpresaTest extends BasePrueba {
    private Empresas servicio;
    private int propietario;
    private int asignado;
    private Empresa propia;
    private Sede sede;
    private Salon salon;
    private Puesto puesto;

    @BeforeEach
    void crearEmpresaPropia() throws Exception {
        servicio = new Empresas(tx);
        propietario = insertar("EVALUADOR", null, cuenta(admin).hash(), false);
        asignado = insertar("EVALUADOR", null, cuenta(admin).hash(), false);
        int nueva = servicio.guardar(cuenta(propietario),
                new Empresa(0, 0, "Taller de pruebas", UUID.randomUUID().toString().substring(0, 20), "", "", "", "",
                        true, null, "Carpintería")).id();
        propia = servicio.empresa(cuenta(propietario), nueva);
        new CuentaEmpresaDao().sincronizar(con, admin, asignado, List.of(nueva), false);
        int nuevaSede = servicio.guardar(cuenta(propietario),
                new Sede(0, nueva, "Principal", "Calle 1", "Armenia", "123", "8 a 5", "Sede de prueba", true)).id();
        sede = servicio.sede(cuenta(propietario), nuevaSede);
        int nuevoSalon = servicio.guardar(cuenta(propietario),
                new Salon(0, nuevaSede, "Taller", "Salón de prueba", 1, 10, true)).id();
        salon = servicio.salon(cuenta(propietario), nuevoSalon);
        int nuevoPuesto = servicio.guardar(cuenta(propietario),
                new Puesto(0, nuevoSalon, null, null, 1, "Ensamble", "MESA", "Ventana", null, true)).id();
        puesto = servicio.puesto(cuenta(propietario), nuevoPuesto);
    }

    @Test
    void registraAlCreadorYLoAsignaSinConfiarEnElFormulario() throws Exception {
        assertEquals(propietario, propia.creador());
        assertEquals("Carpintería", propia.sector());
        assertTrue(new CuentaEmpresaDao().ids(con, propietario).contains(propia.id()));
        assertTrue(servicio.puedeEditar(cuenta(propietario), propia.id(), true));
        assertTrue(servicio.puedeEditar(cuenta(propietario), propia.id(), false));
    }

    @Test
    void asignadosYAdministradorConsultanPeroNoEditan() throws Exception {
        for (int actor : List.of(admin, asignado)) {
            Cuenta cuenta = cuenta(actor);
            assertEquals(propia.id(), servicio.empresa(cuenta, propia.id()).id());
            assertEquals(salon.id(), servicio.salon(cuenta, salon.id()).id());
            assertFalse(servicio.puedeEditar(cuenta, propia.id(), true));
            assertFalse(servicio.puedeEditar(cuenta, propia.id(), false));
            assertThrows(SecurityException.class, () -> servicio.guardar(cuenta, propia));
            assertThrows(SecurityException.class, () -> servicio.guardar(cuenta, sede));
            assertThrows(SecurityException.class, () -> servicio.guardar(cuenta, salon));
            assertThrows(SecurityException.class, () -> servicio.guardar(cuenta, puesto));
            assertThrows(SecurityException.class,
                    () -> servicio.guardar(cuenta, new Area(0, propia.id(), "Otra", true)));
            assertThrows(SecurityException.class,
                    () -> servicio.guardar(cuenta, new Cargo(0, propia.id(), "Otro", "OPERATIVO", true)));
        }
    }

    @Test
    void soloElDuenoDesactivaLaEmpresaYSusEspacios() throws Exception {
        for (int actor : List.of(admin, asignado)) {
            Cuenta cuenta = cuenta(actor);
            assertThrows(SecurityException.class, () -> servicio.estadoEmpresa(cuenta, propia.id(), false));
            assertThrows(SecurityException.class, () -> servicio.estadoSede(cuenta, sede.id(), false));
            assertThrows(SecurityException.class, () -> servicio.estadoSalon(cuenta, salon.id(), false));
            assertThrows(SecurityException.class, () -> servicio.estadoPuesto(cuenta, puesto.id(), false));
        }
        servicio.estadoPuesto(cuenta(propietario), puesto.id(), false);
        servicio.estadoSalon(cuenta(propietario), salon.id(), false);
        servicio.estadoSede(cuenta(propietario), sede.id(), false);
        servicio.estadoEmpresa(cuenta(propietario), propia.id(), false);
        assertFalse(servicio.empresa(cuenta(propietario), propia.id()).activo());
        assertFalse(servicio.puesto(cuenta(propietario), puesto.id()).activo());
    }

    @Test
    void soloAdminTransfiereUnDuenoInactivoAYaAsignadoActivo() throws Exception {
        assertTrue(servicio.candidatosTransferencia(cuenta(admin), propia.id()).isEmpty());
        assertThrows(SecurityException.class, () -> servicio.transferir(cuenta(admin), propia.id(), asignado));
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", propietario);
        assertTrue(servicio.candidatosTransferencia(cuenta(admin), propia.id()).stream()
                .anyMatch(cuenta -> cuenta.id() == asignado));
        assertThrows(SecurityException.class, () -> servicio.transferir(cuenta(asignado), propia.id(), asignado));
        servicio.transferir(cuenta(admin), propia.id(), asignado);
        assertEquals(asignado, servicio.empresa(cuenta(admin), propia.id()).creador());
        assertTrue(servicio.puedeEditar(cuenta(asignado), propia.id(), true));
        assertFalse(servicio.puedeEditar(cuenta(admin), propia.id(), true));
        assertEquals(Integer.valueOf(1), Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='empresa' AND id_registro=? " +
                        "AND detalle='Transferencia de dueño'", fila -> fila.getInt(1), propia.id()));
    }

    @Test
    void noTransfiereAUsuarioNiEvaluadorInactivoYConservaDueno() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", propietario);
        assertThrows(SecurityException.class, () -> servicio.transferir(cuenta(admin), propia.id(), usuario));
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", asignado);
        assertThrows(SecurityException.class, () -> servicio.transferir(cuenta(admin), propia.id(), asignado));
        assertEquals(propietario, servicio.empresa(cuenta(admin), propia.id()).creador());
    }

    @Test
    void noSePuedeRetirarAlDuenoSuAccesoSinTransferirLaEmpresa() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new CuentaEmpresaDao().sincronizar(con, admin, propietario, List.of(), false));
        assertTrue(new CuentaEmpresaDao().ids(con, propietario).contains(propia.id()));
        assertEquals(propietario, servicio.empresa(cuenta(admin), propia.id()).creador());
    }

    @Test
    void sectorEsOpcionalYLimitaSesentaCaracteres() throws Exception {
        Empresa larga = new Empresa(propia.id(), admin, propia.nombre(), propia.nit(), "", "", "", "", true, admin,
                "x".repeat(61));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(propietario), larga));
        Empresa sinSector = new Empresa(propia.id(), admin, propia.nombre(), propia.nit(), "", "", "", "", true);
        servicio.guardar(cuenta(propietario), sinSector);
        Empresa guardada = servicio.empresa(cuenta(propietario), propia.id());
        assertEquals(propietario, guardada.creador());
        assertTrue(guardada.sector() == null || guardada.sector().isEmpty());
    }
}
