package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.servicio.Avisos;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpPaso33Test extends HttpPrueba {
    @Test
    void rolDebajoDelNombreSoloEnInicioDelEquipo() throws Exception {
        for (int id : new int[]{admin, evaluadorTrabajo().id()}) {
            cambiar(id);
            String html = get("/").body();
            assertTrue(html.contains("<p class=\"cabecera-rol-texto\">" + cuenta(id).rol() + "</p>"));
            assertFalse(html.contains("class=\"cabecera-rol\""));
            assertFalse(get("/cuenta").body().contains("class=\"cabecera-rol-texto\""));
        }
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cambiar(usuario);
        assertFalse(get("/").body().contains("class=\"cabecera-rol-texto\""));
    }

    @Test
    void tendenciaEnlazaEvaluacionRealConFechaLegibleYNoAbreEmpresaAjena() throws Exception {
        var datos = new EscenarioConsultas(this, carpetaFotos);
        cambiar(usuario);
        String html = get("/").body();
        var enlace = Pattern.compile("<a class=\"tendencia-visita kpi-riesgo\" href=\"(/evaluaciones/[0-9]+)\"")
                .matcher(html);
        assertTrue(enlace.find());
        String ruta = enlace.group(1);
        assertEquals(200, get(ruta).statusCode());
        assertTrue(html.contains("10/11/2026 · última"));
        assertFalse(html.contains("2026-11-10</small>"));
        int otra = Sql.insertar(con, "INSERT INTO empresa(id_administrador,nombre,nit) VALUES(?,?,?)",
                admin, "Ajena QA", UUID.randomUUID().toString().substring(0, 20));
        int ajeno = insertar("USUARIO", otra, cuenta(admin).hash(), false);
        cambiar(ajeno);
        assertEquals(403, get(ruta).statusCode());
    }

    @Test
    void fichasSoloDeSusEvaluadoresSinTelefonoFalsoYDatosEscapados() throws Exception {
        var ev = evaluadorTrabajo();
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        Sql.actualizar(con, "UPDATE cuenta SET nombre=?,cargo=?,telefono=? WHERE id_cuenta=?",
                "Ing. Persona <script>", "Ingeniera · Evaluadora RULA", "0000000000", ev.id());
        int ajeno = insertar("EVALUADOR", null, cuenta(admin).hash(), false);
        Sql.actualizar(con, "UPDATE cuenta SET correo='ajeno33@prueba.invalid' WHERE id_cuenta=?", ajeno);
        cambiar(usuario);
        String html = get("/").body();
        assertTrue(html.contains("usuario-cabecera-grid"));
        assertTrue(html.indexOf("usuario-ayuda") < html.indexOf("inicio-indicadores"));
        assertTrue(html.contains("Nos encantaría saber de ti"));
        assertTrue(html.contains("Tu evaluadora asignada"));
        assertTrue(html.contains("Persona &lt;script&gt;"));
        assertTrue(html.contains("mailto:" + cuenta(ev.id()).correo()));
        assertFalse(html.contains("ajeno33@prueba.invalid"));
        String ficha = html.substring(html.indexOf("id=\"contacto-inicio-" + ev.id() + "\""));
        ficha = ficha.substring(0, ficha.indexOf("</div></div>"));
        assertFalse(ficha.contains("WhatsApp"));
        assertFalse(ficha.contains("tel:"));
        assertTrue(ficha.contains("/avisos/nuevo?para=" + ev.id()));
        assertFalse(html.contains("data-cerrar-ayuda-inicio"));
        assertEquals(2, new Avisos(tx, reloj).contactos(cuenta(usuario)).size());
    }

    @Test
    void ayudaEnDosColumnasAccesoConRegresoYMarcaConTm() throws Exception {
        String login = get("/login").body();
        assertTrue(login.contains("© 2026 ALINEO™ · Todos los derechos reservados."));
        String acceso = get("/solicitar-acceso").body();
        assertTrue(acceso.contains("href=\"/login\">← Volver a iniciar sesión"));
        assertTrue(acceso.contains("solicitar-visita tarjeta-dorada"));
        String publica = get("/ayuda").body();
        assertTrue(publica.contains("guia tarjeta-dorada"));
        assertTrue(publica.contains("Laura Romero"));
        cambiar(admin);
        String ayuda = get("/ayuda").body();
        assertTrue(ayuda.contains("class=\"ayuda-superior\""));
        assertTrue(ayuda.indexOf("ayuda-contacto") < ayuda.indexOf("¿Qué quieres hacer?"));
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cambiar(usuario);
        assertTrue(get("/ayuda").body().contains("<h3>Sobre ALINEO</h3>"));
    }

    private void cambiar(int id) throws Exception {
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(303, entrar(id).statusCode());
    }
}
