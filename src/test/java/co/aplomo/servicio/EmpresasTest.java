package co.aplomo.servicio;

import co.aplomo.modelo.*;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-004 a RF-010, RF-028: jerarquía real de MySQL, siempre revertida al terminar. */
class EmpresasTest extends BasePrueba {
    private Empresas servicio;

    @BeforeEach void crearServicio() { servicio = new Empresas(tx); }

    private Empresa nuevaEmpresa(String nombre) throws Exception {
        int id = servicio.guardar(cuenta(admin), new Empresa(0, 0, nombre, UUID.randomUUID().toString().substring(0, 20),
                "", "", "", "", true)).id();
        return servicio.empresa(cuenta(admin), id);
    }

    private Sede nuevaSede(int empresa, String nombre) throws Exception {
        int id = servicio.guardar(cuenta(admin), new Sede(0, empresa, nombre, "Calle 1", "Armenia", "123",
                "8 a 5", "Sede de pruebas", true)).id();
        return servicio.sede(cuenta(admin), id);
    }

    private Salon nuevoSalon(int sede, String nombre) throws Exception {
        int id = servicio.guardar(cuenta(admin), new Salon(0, sede, nombre, "Salón de pruebas", 1, 10, true)).id();
        return servicio.salon(cuenta(admin), id);
    }

    private Puesto nuevoPuesto(int salon, Integer area, Integer cargo) throws Exception {
        int id = servicio.guardar(cuenta(admin), new Puesto(0, salon, area, cargo, 1, "Puesto de ensamble", "MESA",
                "Junto a la ventana", null, true)).id();
        return servicio.puesto(cuenta(admin), id);
    }

    private Cuenta usuarioDefinitivo() throws Exception {
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        return cuenta(usuario);
    }

    @Test void guardaLaJerarquiaConPadresCorrectosYPuestoSinPuntajeInventado() throws Exception {
        Sede sede = nuevaSede(empresa, "Principal");
        Salon salon = nuevoSalon(sede.id(), "Producción");
        int area = servicio.guardar(cuenta(admin), new Area(0, empresa, "Ensamble", true)).id();
        int cargo = servicio.guardar(cuenta(admin), new Cargo(0, empresa, "Operario", "OPERATIVO", true)).id();
        Puesto puesto = nuevoPuesto(salon.id(), area, cargo);
        assertEquals(empresa, sede.empresa());
        assertEquals(sede.id(), salon.sede());
        assertEquals(salon.id(), puesto.salon());
        assertEquals(area, puesto.area());
        assertEquals(cargo, puesto.cargo());
        assertNull(puesto.puntaje());
        assertEquals(1, servicio.puestos(cuenta(admin), salon.id()).size());
    }

    @Test void nitRepetidoNoGuardaNiEnEmpresaInactiva() throws Exception {
        Empresa primera = nuevaEmpresa("Primera");
        servicio.estadoEmpresa(cuenta(admin), primera.id(), false);
        Empresa repetida = new Empresa(0, 0, "Otra", primera.nit().toUpperCase(), "", "", "", "", true);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), repetida));
        assertFalse(servicio.empresa(cuenta(admin), primera.id()).activo());
    }

    @Test void nombreSimilarSinTildesEspaciosNiSufijoSoloAvisa() throws Exception {
        nuevaEmpresa("Ácme S.A.S.");
        var resultado = servicio.guardar(cuenta(admin), new Empresa(0, 0, " ACME   Ltda. ", "987654321",
                "", "", "", "", true));
        assertNotNull(resultado.aviso());
        assertTrue(resultado.aviso().contains("Ácme"));
        assertEquals("ACME Ltda.", servicio.empresa(cuenta(admin), resultado.id()).nombre());
    }

    @Test void editarConservaIdCreadorEstadoYRelaciones() throws Exception {
        Empresa original = nuevaEmpresa("Original");
        servicio.estadoEmpresa(cuenta(admin), original.id(), false);
        servicio.guardar(cuenta(admin), new Empresa(original.id(), Integer.MAX_VALUE, "Editada", original.nit(),
                "Descripción", "123", "CONTACTO@EJEMPLO.CO", "Contacto", true));
        Empresa editada = servicio.empresa(cuenta(admin), original.id());
        assertEquals(original.id(), editada.id());
        assertEquals(admin, editada.administrador());
        assertFalse(editada.activo());
        assertEquals("contacto@ejemplo.co", editada.correo());
        servicio.estadoEmpresa(cuenta(admin), original.id(), true);
        Sede sede = nuevaSede(original.id(), "Sede");
        servicio.guardar(cuenta(admin), new Sede(sede.id(), empresa, "Nombre corregido", "Otra calle", "Armenia",
                "456", "Mañana", "Descripción", true));
        assertEquals(original.id(), servicio.sede(cuenta(admin), sede.id()).empresa());
    }

    @Test void evitaDuplicadosDeNombreEnCadaPadreIncluidosInactivos() throws Exception {
        Sede sede = nuevaSede(empresa, " Sede   Bogotá ");
        servicio.estadoSede(cuenta(admin), sede.id(), false);
        assertThrows(IllegalArgumentException.class, () -> nuevaSede(empresa, "SEDE BOGOTA"));
        servicio.estadoSede(cuenta(admin), sede.id(), true);
        Salon salon = nuevoSalon(sede.id(), "Ensamble");
        assertThrows(IllegalArgumentException.class, () -> nuevoSalon(sede.id(), "ensamble"));
        servicio.guardar(cuenta(admin), new Area(0, empresa, "Logística", true));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), new Area(0, empresa, "logistica", true)));
        servicio.guardar(cuenta(admin), new Cargo(0, empresa, "Técnico", "OTRO", true));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), new Cargo(0, empresa, "TECNICO", "OPERATIVO", true)));
        nuevoPuesto(salon.id(), null, null);
        assertThrows(IllegalArgumentException.class, () -> nuevoPuesto(salon.id(), null, null));
    }

    @Test void elMismoNombrePuedeExistirEnPadresDistintos() throws Exception {
        Empresa otra = nuevaEmpresa("Otra");
        Sede primera = nuevaSede(empresa, "Principal");
        Sede segunda = nuevaSede(otra.id(), "Principal");
        assertNotEquals(primera.id(), segunda.id());
        assertNotEquals(nuevoSalon(primera.id(), "Salón").id(), nuevoSalon(segunda.id(), "Salón").id());
    }

    @Test void noAceptaAreasNiCargosDeOtraEmpresa() throws Exception {
        Empresa otra = nuevaEmpresa("Otra");
        int area = servicio.guardar(cuenta(admin), new Area(0, otra.id(), "Área", true)).id();
        int cargo = servicio.guardar(cuenta(admin), new Cargo(0, otra.id(), "Cargo", "OTRO", true)).id();
        Salon salon = nuevoSalon(nuevaSede(empresa, "Sede").id(), "Salón");
        assertThrows(SecurityException.class, () -> nuevoPuesto(salon.id(), area, null));
        assertThrows(SecurityException.class, () -> nuevoPuesto(salon.id(), null, cargo));
    }

    @Test void desactivarPadresConservaHistorialYBloqueaNuevasAsignaciones() throws Exception {
        Sede sede = nuevaSede(empresa, "Principal");
        Salon salon = nuevoSalon(sede.id(), "Salón");
        Puesto puesto = nuevoPuesto(salon.id(), null, null);
        servicio.estadoEmpresa(cuenta(admin), empresa, false);
        assertThrows(IllegalArgumentException.class, () -> nuevaSede(empresa, "Nueva"));
        assertThrows(IllegalArgumentException.class, () -> nuevoSalon(sede.id(), "Otro"));
        assertThrows(IllegalArgumentException.class, () -> nuevoPuesto(salon.id(), null, null));
        assertEquals(puesto.id(), servicio.puestos(cuenta(admin), salon.id()).get(0).id());
        assertTrue(servicio.puesto(cuenta(admin), puesto.id()).activo());
        servicio.estadoEmpresa(cuenta(admin), empresa, true);
        assertDoesNotThrow(() -> nuevaSede(empresa, "Nueva"));
    }

    @Test void puestoConservaPuntajeYAsignacionInactivaAlEditar() throws Exception {
        Salon salon = nuevoSalon(nuevaSede(empresa, "Sede").id(), "Salón");
        int area = servicio.guardar(cuenta(admin), new Area(0, empresa, "Área", true)).id();
        Puesto puesto = nuevoPuesto(salon.id(), area, null);
        Sql.actualizar(con, "UPDATE puesto_trabajo SET ultimo_puntaje = ? WHERE id_puesto = ?", 6, puesto.id());
        servicio.estadoArea(cuenta(admin), area, false);
        servicio.estadoPuesto(cuenta(admin), puesto.id(), false);
        servicio.guardar(cuenta(admin), new Puesto(puesto.id(), 0, area, null, 2, "Corregido", "ESCRITORIO", "Otra ubicación", 1, true));
        Puesto editado = servicio.puesto(cuenta(admin), puesto.id());
        assertEquals(6, editado.puntaje());
        assertEquals(area, editado.area());
        assertEquals(salon.id(), editado.salon());
        assertFalse(editado.activo());
        assertThrows(IllegalArgumentException.class, () -> nuevoPuesto(salon.id(), area, null));
    }

    @Test void usuarioSoloConsultaSuEmpresaYNuncaModifica() throws Exception {
        Empresa otra = nuevaEmpresa("Privada");
        Sede propia = nuevaSede(empresa, "Propia");
        Sede ajena = nuevaSede(otra.id(), "Ajena");
        Cuenta contacto = usuarioDefinitivo();
        assertEquals(List.of(empresa), servicio.listar(contacto).stream().map(Empresa::id).toList());
        assertEquals(propia.id(), servicio.sedes(contacto, empresa).get(0).id());
        assertThrows(SecurityException.class, () -> servicio.empresa(contacto, otra.id()));
        assertThrows(SecurityException.class, () -> servicio.sede(contacto, ajena.id()));
        assertThrows(SecurityException.class, () -> servicio.sedes(contacto, otra.id()));
        assertThrows(SecurityException.class, () -> servicio.estadoEmpresa(contacto, empresa, false));
        assertThrows(SecurityException.class, () -> servicio.guardar(contacto, new Area(0, empresa, "Intento", true)));
    }

    @Test void validacionesRechazanDatosFueraDelEsquemaAntesDeGuardar() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> nuevaEmpresa(" "));
        assertThrows(IllegalArgumentException.class, () -> nuevaEmpresa("x".repeat(121)));
        Sede sede = nuevaSede(empresa, "Sede");
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), new Salon(0, sede.id(), "Salón", "Descripción", 128, 1, true)));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), new Salon(0, sede.id(), "Salón", "Descripción", 0, 0, true)));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), new Cargo(0, empresa, "Cargo", "OTRO_ROL", true)));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(cuenta(admin), new Empresa(0, 0, "Empresa", "123", "", "", "incorrecto", "", true)));
    }

    @Test void unaEmpresaInactivaNoSeAsignaACuentasNuevasPeroConservaLasAnteriores() throws Exception {
        servicio.estadoEmpresa(cuenta(admin), empresa, false);
        Usuarios usuarios = new Usuarios(tx, dao, new co.aplomo.dao.EmpresaDao());
        var nuevos = new Usuarios.Datos("Contacto", UUID.randomUUID() + "@prueba.invalid", "123", "Contacto", Rol.USUARIO, empresa);
        assertThrows(IllegalArgumentException.class, () -> usuarios.crear(cuenta(admin), nuevos, "Temporal2026"));
        Cuenta antes = cuenta(usuario);
        assertDoesNotThrow(() -> usuarios.editar(cuenta(admin), usuario,
                new Usuarios.Datos("Editado", antes.correo(), antes.telefono(), antes.cargo(), Rol.USUARIO, empresa), ""));
        assertEquals(empresa, cuenta(usuario).empresa());
    }
}
