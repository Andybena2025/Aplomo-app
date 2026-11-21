package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.servicio.SolicitudesVisita;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-002/033: rutas reales, privacidad, CSRF y alternativas de una solicitud ocupada.
 */
class HttpAvisos27Test extends HttpPrueba {
    @Test
    void sondeoSinSesionResponde401SinRedireccion() throws Exception {
        assertEquals(401, get("/avisos/nuevos").statusCode());
        assertEquals(401, get("/notas/nuevas").statusCode());
    }

    @Test
    void todosLosRolesTienenBandejaYNoVenMensajesAjenos() throws Exception {
        int ev = evaluadorTrabajo().id();
        new co.aplomo.dao.MensajeDao().enviar(con, null, admin, ev, "AVISO", "Mensaje privado <script>",
                LocalDate.of(2026, 11, 10).atStartOfDay());
        entrar(admin);
        assertEquals("[]", get("/avisos/nuevos").body());
        assertTrue(get("/").body().contains("data-abrir-avisos"));
        assertEquals(303, post("/logout", Map.of("csrf", csrf(get("/")))).statusCode());
        entrar(ev);
        assertTrue(get("/avisos/nuevos").body().contains("Mensaje privado"));
        assertTrue(get("/avisos").body().contains("&lt;script&gt;"));
        assertFalse(get("/avisos").body().contains("Mensaje privado <script>"));
    }

    @Test
    void solicitarConCruceConservaCamposYDaLasDosAlternativas() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        int salon = crearSalon().id();
        int ev = evaluadorTrabajo().id();
        new SolicitudesVisita(tx, reloj).solicitar(cuenta(usuario), salon, ev, LocalDate.of(2026, 12, 1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Primera");
        entrar(usuario);
        var pagina = get("/solicitar-visita");
        assertEquals(200, pagina.statusCode());
        var datos = Map.of("csrf", csrf(pagina), "salon", "" + salon, "ingeniero", "" + ev, "fecha", "2026-12-01",
                "inicio", "09:00", "fin", "10:00", "motivo", "Quiero revisar la mesa");
        var respuesta = post("/solicitar-visita", datos);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Elegir otra hora"));
        assertTrue(respuesta.body().contains("Elegir otro ingeniero"));
        assertTrue(respuesta.body().contains("Quiero revisar la mesa"));
        assertEquals(403, post("/solicitar-visita", Map.of("salon", "" + salon)).statusCode());
    }

    @Test
    void usuarioAjenoEs403AntesDeCrearYNoPuedeDecidirLaSolicitud() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        int salon = crearSalon().id();
        int ev = evaluadorTrabajo().id();
        var solicitudes = new SolicitudesVisita(tx, reloj);
        int id = solicitudes.solicitar(cuenta(usuario), salon, ev, LocalDate.of(2026, 12, 1), LocalTime.of(9, 0),
                LocalTime.of(10, 0), "");
        entrar(usuario);
        assertEquals(403, get("/solicitudes-visita/" + id).statusCode());
        var pagina = get("/solicitar-visita");
        assertEquals(403, post("/solicitudes-visita/" + id + "/aceptar", Map.of("csrf", csrf(pagina))).statusCode());
        assertEquals(403, post("/solicitar-visita",
                Map.of("csrf", csrf(pagina), "salon", "2147483647", "ingeniero", "" + ev, "fecha", "2026-12-01",
                        "inicio", "11:00", "fin", "12:00")).statusCode());
    }
}
