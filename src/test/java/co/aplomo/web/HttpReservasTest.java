package co.aplomo.web;

import co.aplomo.modelo.*;
import co.aplomo.servicio.*;
import co.aplomo.dao.ReservaDao;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-009b a RF-012: formularios, permisos, estados y altas sin navegación del formulario principal. */
class HttpReservasTest extends co.aplomo.soporte.HttpPrueba {
    private Map<String,String> formulario(String csrf, Salon salon) {
        var datos = new HashMap<String,String>();
        datos.put("csrf", csrf); datos.put("empresa", Integer.toString(empresa));
        datos.put("sede", Integer.toString(salon.sede())); datos.put("salon", Integer.toString(salon.id()));
        datos.put("fecha", java.time.LocalDate.now(reloj).toString()); datos.put("inicio", "09:00"); datos.put("fin", "10:00");
        datos.put("categoria", "EVALUACION_INICIAL"); datos.put("descripcion", "Descripción <script>alert(1)</script>");
        datos.put("notas", "Nota de prueba");
        return datos;
    }

    private int id(java.net.http.HttpResponse<String> respuesta) {
        assertEquals(303, respuesta.statusCode(), respuesta.body());
        var m = java.util.regex.Pattern.compile("/reservas/([0-9]+)")
                .matcher(respuesta.headers().firstValue("Location").orElseThrow());
        assertTrue(m.find()); return Integer.parseInt(m.group(1));
    }

    @Test void crearEditarAceptarYCancelarDesdeLaInterfazHttp() throws Exception {
        Salon salon = crearSalon(); entrar(admin);
        var nueva = get("/reservas/nueva"); assertEquals(200, nueva.statusCode());
        var campos = formulario(csrf(nueva), salon);
        int id = id(post("/reservas", campos));
        var detalle = get("/reservas/" + id);
        assertTrue(detalle.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(detalle.body().contains("Aceptar reserva"));
        var r = new Reservas(tx, reloj).consultar(cuenta(admin), id).reserva();
        campos.put("inicio", "11:00"); campos.put("fin", "12:00"); campos.put("codigo", "HACKEADO");
        campos.put("salon", "2147483647"); campos.put("estado", "FINALIZADA");
        assertEquals(303, post("/reservas/" + id + "/editar", campos).statusCode());
        Reserva despues = new Reservas(tx, reloj).consultar(cuenta(admin), id).reserva();
        assertEquals(r.codigo(), despues.codigo()); assertEquals(salon.id(), despues.salon()); assertEquals("ESPERANDO", despues.estado());
        assertEquals(404, get("/reservas/" + id + "/aceptar").statusCode());
        assertEquals(403, post("/reservas/" + id + "/aceptar", Map.of()).statusCode());
        assertEquals(303, post("/reservas/" + id + "/aceptar", Map.of("csrf", csrf(detalle))).statusCode());
        assertEquals(400, post("/reservas/" + id + "/cancelar", Map.of("csrf", csrf(detalle), "motivo", "")).statusCode());
        assertEquals(303, post("/reservas/" + id + "/cancelar", Map.of("csrf", csrf(detalle), "motivo", "Cambio de horario")).statusCode());
        var cancelada = get("/reservas/" + id);
        assertTrue(cancelada.body().contains("Cambio de horario"));
        assertFalse(cancelada.body().contains("Editar reserva"));
        assertEquals(400, get("/reservas/" + id + "/editar").statusCode());
    }

    @Test void errorConservaCamposYLasNotasLargasPermitidasSeGuardanCompletas() throws Exception {
        Salon salon = crearSalon(); entrar(admin);
        var nueva = get("/reservas/nueva");
        var campos = formulario(csrf(nueva), salon);
        campos.put("fin", "08:00");
        var error = post("/reservas", campos);
        assertEquals(400, error.statusCode());
        assertTrue(error.body().contains("value=\"09:00\""));
        assertTrue(error.body().contains("Nota de prueba"));
        campos.put("fin", "10:00"); campos.put("notas", "Línea de prueba\n".repeat(1500));
        int id = id(post("/reservas", campos));
        assertEquals(campos.get("notas").strip(), new Reservas(tx, reloj).consultar(cuenta(admin), id).reserva().notas());
    }

    @Test void altasEnContextoDevuelvenCatalogosConLosNuevosLugares() throws Exception {
        entrar(admin); var nueva = get("/reservas/nueva"); String token = csrf(nueva);
        var empresa = post("/reservas/crear-empresa", Map.of("csrf", token, "nombre", "Creada en reserva", "nit", "Q" + UUID.randomUUID().toString().substring(0, 10)));
        assertEquals(200, empresa.statusCode());
        int idEmpresa = Integer.parseInt(RouterPruebaDatos.parametros(empresa.body()).get("id"));
        var sede = post("/reservas/crear-sede", Map.of("csrf", token, "empresa", Integer.toString(idEmpresa),
                "nombre", "Sede", "direccion", "Calle 1", "ciudad", "Armenia", "telefono", "123", "horario", "Mañana", "descripcion", "Descripción"));
        assertEquals(200, sede.statusCode());
        int idSede = Integer.parseInt(RouterPruebaDatos.parametros(sede.body()).get("id"));
        var salon = post("/reservas/crear-salon", Map.of("csrf", token, "sede", Integer.toString(idSede),
                "nombre", "Salón", "descripcion", "Descripción", "piso", "1", "capacidad_puestos", "10"));
        assertEquals(200, salon.statusCode());
        var catalogo = get("/reservas/catalogo?empresa=" + idEmpresa + "&sede=" + idSede);
        assertTrue(catalogo.body().contains("Creada en reserva"));
        assertTrue(catalogo.body().contains("data-padre=\"" + idEmpresa + "\""));
        assertTrue(nueva.body().contains("data-alta-reserva"));
        assertEquals(200, get("/static/js/reservas.js").statusCode());
    }

    @Test void usuarioSoloConsultaSusReservasSinBotonesDeGestion() throws Exception {
        Salon salon = crearSalon();
        int id = new Reservas(tx, reloj).guardar(cuenta(admin), 0, solicitud(salon));
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false); entrar(usuario);
        var detalle = get("/reservas/" + id);
        assertEquals(200, detalle.statusCode());
        assertFalse(detalle.body().contains("Aceptar reserva")); assertFalse(detalle.body().contains("Editar reserva"));
        assertEquals(403, get("/reservas/nueva").statusCode());
        assertEquals(403, get("/reservas/catalogo").statusCode());
        assertEquals(403, post("/reservas/" + id + "/aceptar", Map.of("csrf", csrf(detalle))).statusCode());
        assertEquals(403, post("/reservas/crear-empresa", Map.of("csrf", csrf(detalle), "nombre", "Intento", "nit", "123")).statusCode());
    }

    private static final class RouterPruebaDatos {
        static Map<String,String> parametros(String texto) {
            Map<String,String> datos = new HashMap<>();
            for (String par : texto.split("&")) {
                String[] partes = par.split("=", 2);
                datos.put(partes[0], java.net.URLDecoder.decode(partes[1], java.nio.charset.StandardCharsets.UTF_8));
            }
            return datos;
        }
    }
}
