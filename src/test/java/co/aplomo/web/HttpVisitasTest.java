package co.aplomo.web;

import co.aplomo.servicio.*;
import co.aplomo.modelo.Salon;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-013a, RF-013b: ciclo HTTP, tokens, campos manipulados y vistas de solo lectura. */
class HttpVisitasTest extends co.aplomo.soporte.HttpPrueba {
    private int reserva(Salon salon) throws Exception {
        var servicio = new Reservas(tx, reloj);
        int id = servicio.guardar(cuenta(admin), 0, solicitud(salon)); servicio.aceptar(cuenta(admin), id); return id;
    }

    @Test void iniciaPorPostYNoPermiteFinalizarSinEvaluacionesNiCancelarDespues() throws Exception {
        int reserva = reserva(crearSalon()); entrar(admin);
        var ficha = get("/reservas/" + reserva); String token = csrf(ficha);
        assertTrue(ficha.body().contains("Iniciar visita"));
        assertEquals(404, get("/reservas/" + reserva + "/iniciar-visita").statusCode());
        assertEquals(403, post("/reservas/" + reserva + "/iniciar-visita", Map.of()).statusCode());
        var inicio = post("/reservas/" + reserva + "/iniciar-visita", Map.of("csrf", token));
        assertEquals(303, inicio.statusCode(), inicio.body());
        int visita = new Reservas(tx, reloj).consultar(cuenta(admin), reserva).visita();
        var detalle = get("/visitas/" + visita);
        assertEquals(200, detalle.statusCode()); assertTrue(detalle.body().contains("Guarda al menos una evaluación"));
        var error = post("/visitas/" + visita + "/finalizar", Map.of("csrf", token, "notas", "Notas que conservo"));
        assertEquals(400, error.statusCode()); assertTrue(error.body().contains("Notas que conservo")); assertTrue(error.body().contains(" disabled"));
        assertEquals(400, post("/reservas/" + reserva + "/cancelar", Map.of("csrf", token, "motivo", "Intento")).statusCode());
        var actual = get("/reservas/" + reserva);
        assertFalse(actual.body().contains(">Cancelar reserva</a>")); assertTrue(actual.body().contains("Ver visita"));
    }

    @Test void finalizaConNotasEscapadasYPromedioDelServidorAunqueSeManipuleElFormulario() throws Exception {
        Salon salon = crearSalon(); int reserva = reserva(salon);
        int visita = new Visitas(tx, reloj).iniciar(cuenta(admin), reserva);
        analisis(foto(evaluacion(visita, salon.id(), 1)), 4);
        entrar(admin); var pagina = get("/visitas/" + visita + "/finalizar");
        assertEquals(200, pagina.statusCode());
        assertEquals(403, post("/visitas/" + visita + "/finalizar", Map.of("notas", "Sin token")).statusCode());
        var fin = post("/visitas/" + visita + "/finalizar", Map.of("csrf", csrf(pagina), "notas", "<script>alert(1)</script>\nSegunda línea",
                "calificacion", "0", "estado", "EN_CURSO", "reserva", "2147483647"));
        assertEquals(303, fin.statusCode(), fin.body());
        var detalle = get("/visitas/" + visita);
        assertTrue(detalle.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(detalle.body().contains("Postura &lt;b&gt;de prueba&lt;/b&gt;"));
        assertTrue(detalle.body().contains("4.0")); assertFalse(detalle.body().contains(">Finalizar visita</a>"));
        assertTrue(get("/reservas/" + reserva).body().contains("Finalizada"));
        assertEquals(400, post("/visitas/" + visita + "/finalizar", Map.of("csrf", csrf(detalle), "notas", "Reemplazar")).statusCode());
    }

    @Test void usuarioVeSoloFinalizadasSinFotosNiControlesYNoPuedeForzarAcciones() throws Exception {
        Salon salon = crearSalon(); int reserva = reserva(salon);
        var servicio = new Visitas(tx, reloj); int visita = servicio.iniciar(cuenta(admin), reserva);
        analisis(foto(evaluacion(visita, salon.id(), 1)), 3);
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false); entrar(usuario);
        assertEquals(403, get("/visitas/" + visita).statusCode());
        assertFalse(get("/visitas").body().contains("/visitas/" + visita + "\""));
        servicio.finalizar(cuenta(admin), visita, "Notas para la empresa");
        var detalle = get("/visitas/" + visita);
        assertEquals(200, detalle.statusCode()); assertTrue(detalle.body().contains("Notas para la empresa"));
        assertFalse(detalle.body().contains("Finalizar visita")); assertFalse(detalle.body().contains("no-existe/prueba.png"));
        assertFalse(detalle.body().contains("/fotos/"));
        assertTrue(get("/visitas").body().contains("/visitas/" + visita + "\""));
        assertEquals(403, get("/visitas/" + visita + "/finalizar").statusCode());
        assertEquals(403, post("/visitas/" + visita + "/finalizar", Map.of("csrf", csrf(detalle))).statusCode());
        assertEquals(403, post("/reservas/" + reserva + "/iniciar-visita", Map.of("csrf", csrf(detalle))).statusCode());
    }
}
