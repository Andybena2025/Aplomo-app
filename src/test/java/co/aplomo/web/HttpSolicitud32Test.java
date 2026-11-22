package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.servicio.SolicitudesVisita;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpSolicitud32Test extends HttpPrueba {
    @Test
    void horasExcluyenCrucesYElEnvioConservaEstadoEsperando() throws Exception {
        var salon = crearSalon();
        var ev = evaluadorTrabajo();
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        var solicitudes = new SolicitudesVisita(tx,reloj);
        var dia = solicitudes.hoy().plusDays(1);
        solicitudes.solicitar(cuenta(usuario),salon.id(),ev.id(),dia,
                java.time.LocalTime.of(10,0),java.time.LocalTime.of(12,0),"Ocupada");
        entrar(usuario);
        String ruta = "/solicitar-visita/horas?ingeniero=" + ev.id() + "&fecha=" + dia + "&duracion=2&salon=" + salon.id();
        var respuesta = get(ruta);
        assertEquals(200,respuesta.statusCode());
        assertTrue(respuesta.body().contains("\"08:00\""));
        assertFalse(respuesta.body().contains("\"09:30\""));
        assertFalse(respuesta.body().contains("\"10:00\""));
        assertTrue(respuesta.body().contains("\"12:00\""));
        assertTrue(respuesta.body().contains("\"16:00\""));
        assertFalse(respuesta.body().contains("\"16:30\""));
        String html=get("/solicitar-visita").body();
        assertTrue(html.contains("1 Dónde"));assertTrue(html.contains("2 Cuándo y con quién"));
        assertTrue(html.contains("3 Revisar y enviar"));assertTrue(html.contains("<noscript>"));
        assertTrue(html.contains("Tu evaluador asignado"));
        assertFalse(html.contains("<select class=\"input\" name=\"sede\""));
        var envio=post("/solicitar-visita",Map.of("csrf",csrf(get("/solicitar-visita")),
                "sede",""+salon.sede(),"salon",""+salon.id(),"ingeniero",""+ev.id(),"fecha",""+dia,
                "inicio","13:00","fin","15:00","motivo","Primera evaluación"));
        assertEquals(200,envio.statusCode());assertTrue(envio.body().contains("Solicitud enviada"));
        assertEquals("ESPERANDO",Sql.uno(con,"SELECT estado FROM reserva WHERE id_salon=? AND hora_inicio='13:00'",
                f->f.getString(1),salon.id()));
        var cruce=post("/solicitar-visita",Map.of("csrf",csrf(get("/solicitar-visita")),
                "sede",""+salon.sede(),"salon",""+salon.id(),"ingeniero",""+ev.id(),"fecha",""+dia,
                "inicio","10:00","fin","12:00","motivo","Seguimiento"));
        assertEquals(400,cruce.statusCode());assertTrue(cruce.body().contains("data-paso=\"2\""));
        assertTrue(cruce.body().contains("Seguimiento"));
    }

    @Test
    void otroRolUsuarioSinLugaresYLugarAjenoNoConsultanHoras() throws Exception {
        var salon=crearSalon();var ev=evaluadorTrabajo();
        String ruta="/solicitar-visita/horas?ingeniero="+ev.id()+"&fecha=2026-11-11&duracion=2";
        entrar(admin);assertEquals(403,get(ruta).statusCode());
        cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        entrar(ev.id());assertEquals(403,get(ruta).statusCode());
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);
        cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);assertEquals(403,get(ruta+"&salon=2147483647").statusCode());
        assertEquals(403,get(ruta.replace("ingeniero="+ev.id(),"ingeniero="+admin)).statusCode());
        Sql.actualizar(con,"UPDATE cuenta_empresa SET activo=FALSE WHERE id_cuenta=?",usuario);
        assertEquals(403,get(ruta).statusCode());
    }
}
