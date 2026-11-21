package co.aplomo.web;

import co.aplomo.dao.VisitaDao;
import co.aplomo.db.Sql;
import co.aplomo.servicio.Reservas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 27A: límites de roles, fechas y contexto de informe con sesiones HTTP reales.
 */
class HttpFunciones27Test extends HttpPrueba {
    @Test
    void administradorConsultaPeroNoReservaIniciaVisitaNiPractica() throws Exception {
        var salon = crearSalon();
        var reservas = new Reservas(tx, reloj);
        int reserva = reservas.guardar(evaluadorTrabajo(), 0, solicitud(salon));
        reservas.aceptar(evaluadorTrabajo(), reserva);
        entrar(admin);
        var inicio = get("/");
        assertEquals(200, inicio.statusCode());
        assertFalse(inicio.body().contains("href=\"/reservas/nueva\""));
        assertFalse(inicio.body().contains("href=\"/demo/evaluacion\""));
        assertEquals(403, get("/reservas/nueva").statusCode());
        assertEquals(403, post("/reservas", Map.of("csrf", csrf(inicio))).statusCode());
        assertEquals(403, post("/reservas/" + reserva + "/iniciar-visita", Map.of("csrf", csrf(inicio))).statusCode());
        assertEquals(403, get("/demo/evaluacion").statusCode());
        assertEquals(200, get("/reservas/" + reserva).statusCode());
    }

    @Test
    void horasLibresExcluyenElCruceYSoloLasConsultaEvaluador() throws Exception {
        var salon = crearSalon();
        var datos = solicitud(salon);
        new Reservas(tx, reloj).guardar(evaluadorTrabajo(), 0, datos);
        entrar(evaluadorTrabajo().id());
        var libres = get("/reservas/horas-libres?fecha=" + datos.fecha());
        assertEquals(200, libres.statusCode());
        assertFalse(libres.body().contains("data-hora-libre=\"09:00\""));
        assertFalse(libres.body().contains("data-hora-libre=\"09:30\""));
        assertTrue(libres.body().contains("data-hora-libre=\"10:00\""));
        assertEquals(400, get("/reservas/horas-libres?fecha=31/02/2026").statusCode());
        post("/logout", Map.of("csrf", csrf(get("/"))));
        entrar(admin);
        assertEquals(403, get("/reservas/horas-libres?fecha=" + datos.fecha()).statusCode());
    }

    @Test
    void unCampoPropioNoPermiteCambiarRolAunqueSeEnvieATravesDelFormulario() throws Exception {
        var actor = evaluadorTrabajo();
        entrar(actor.id());
        var formulario = get("/cuenta/nombre");
        assertEquals(200, formulario.statusCode());
        assertTrue(formulario.body().contains("name=\"nombre\""));
        assertFalse(formulario.body().contains("name=\"telefono\""));
        var respuesta = post("/cuenta/nombre",
                Map.of("csrf", csrf(formulario), "nombre", "Nombre actualizado", "rol", "ADMINISTRADOR", "correo",
                        "otro@prueba.invalid"));
        assertEquals(303, respuesta.statusCode());
        assertEquals(actor.rol(), cuenta(actor.id()).rol());
        assertEquals(actor.correo(), cuenta(actor.id()).correo());
        assertEquals("Nombre actualizado", cuenta(actor.id()).nombre());
    }

    @Test
    void informeResaltaVisitaDeSuEmpresaYRechazaUnaAjena() throws Exception {
        var salon = crearSalon();
        var reservas = new Reservas(tx, reloj);
        int reserva = reservas.guardar(evaluadorTrabajo(), 0, solicitud(salon));
        reservas.aceptar(evaluadorTrabajo(), reserva);
        int visita = new VisitaDao().iniciar(con, reserva, reloj.instant().atZone(reloj.getZone()).toLocalDateTime());
        Sql.actualizar(con, "UPDATE visita SET estado='FINALIZADA',hora_real_fin=hora_real_inicio WHERE id_visita=?",
                visita);
        entrar(admin);
        var informe = get("/informes/empresa/" + empresa + "?visita=" + visita);
        assertEquals(200, informe.statusCode());
        assertTrue(informe.body().contains("data-visita=\"" + visita + "\" data-visita-actual"));
        assertTrue(informe.body().contains("Visita actual:"));
        assertEquals(403, get("/informes/empresa/" + empresa + "?visita=2147483647").statusCode());
    }
}
