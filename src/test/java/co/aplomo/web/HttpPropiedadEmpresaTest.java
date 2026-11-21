package co.aplomo.web;

import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.servicio.Empresas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-002: direcciones escritas a mano, botones y transferencia con sesión real.
 */
class HttpPropiedadEmpresaTest extends HttpPrueba {
    private int dueno;
    private int otro;
    private int propia;
    private int sede;
    private int salon;

    @BeforeEach
    void prepararPropiedad() throws Exception {
        dueno = insertar("EVALUADOR", null, cuenta(admin).hash(), false);
        otro = insertar("EVALUADOR", null, cuenta(admin).hash(), false);
        Empresas servicio = new Empresas(tx);
        propia = servicio.guardar(cuenta(dueno),
                new Empresa(0, 0, "Empresa propia", UUID.randomUUID().toString().substring(0, 20), "", "", "", "", true,
                        null, "Educación")).id();
        new CuentaEmpresaDao().sincronizar(con, admin, otro, List.of(propia), false);
        sede = servicio.guardar(cuenta(dueno),
                new Sede(0, propia, "Principal", "Calle 1", "Armenia", "123", "8 a 5", "Principal", true)).id();
        salon = servicio.guardar(cuenta(dueno), new Salon(0, sede, "Taller", "Ensamble", 1, 10, true)).id();
    }

    @Test
    void adminYAsignadoNoVenBotonesNiAccedenAEdicionDirecta() throws Exception {
        for (int actor : List.of(admin, otro)) {
            assertEquals(303, entrar(actor).statusCode());
            var pagina = get("/empresas/" + propia);
            assertEquals(200, pagina.statusCode());
            assertTrue(pagina.body().contains("Educación"));
            assertFalse(pagina.body().contains("href=\"/empresas/" + propia + "/editar\""));
            assertFalse(pagina.body().contains("href=\"/empresas/" + propia + "/sedes/nuevo\""));
            for (String ruta : List.of("/empresas/" + propia + "/editar", "/sedes/" + sede + "/editar",
                    "/salones/" + salon + "/editar", "/empresas/" + propia + "/sedes/nuevo")) {
                assertEquals(403, get(ruta).statusCode(), ruta);
            }
            String csrf = csrf(pagina);
            assertEquals(403, post("/empresas/" + propia + "/desactivar", Map.of("csrf", csrf)).statusCode());
            assertEquals(403, post("/empresas/" + propia + "/editar",
                    Map.of("csrf", csrf, "nombre", "No autorizado")).statusCode());
            post("/logout", Map.of("csrf", csrf));
        }
    }

    @Test
    void duenoTieneFormularioYAdminSoloPuedeTransferirCuandoEstaInactivo() throws Exception {
        entrar(dueno);
        var pagina = get("/empresas/" + propia);
        assertTrue(pagina.body().contains("href=\"/empresas/" + propia + "/editar\""));
        assertTrue(pagina.body().contains("/empresas/" + propia + "/desactivar"));
        assertEquals(200, get("/empresas/" + propia + "/editar").statusCode());
        assertEquals(200, get("/sedes/" + sede + "/editar").statusCode());
        post("/logout", Map.of("csrf", csrf(pagina)));
        entrar(admin);
        pagina = get("/empresas/" + propia);
        assertFalse(pagina.body().contains("/transferir\""));
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", dueno);
        pagina = get("/empresas/" + propia);
        assertTrue(pagina.body().contains("action=\"/empresas/" + propia + "/transferir\""));
        var respuesta = post("/empresas/" + propia + "/transferir",
                Map.of("csrf", csrf(pagina), "evaluador", Integer.toString(otro)));
        assertEquals(303, respuesta.statusCode());
        assertEquals(otro, new Empresas(tx).empresa(cuenta(admin), propia).creador());
    }
}
