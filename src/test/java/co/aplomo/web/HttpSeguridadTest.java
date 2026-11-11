package co.aplomo.web;

import co.aplomo.soporte.BasePrueba;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.modelo.Cuenta.Estado;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** HTTP real con cookies y MySQL; todo dato de prueba se revierte al terminar. */
public class HttpSeguridadTest extends co.aplomo.soporte.HttpPrueba {
    @Test void anonimoNoEntraYElLoginConservaElEstilo() throws Exception {
        assertEquals("/login",get("/").headers().firstValue("Location").orElseThrow());
        assertEquals(303,get("/usuarios").statusCode());
        assertEquals(303,get("/cuenta/contrasena").statusCode());
        var login=get("/login"); assertEquals(200,login.statusCode());
        assertTrue(login.body().contains("pz-svg")); assertTrue(login.body().contains("name=\"correo\""));
        assertFalse(login.body().contains(" disabled"));
        String cookie=login.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly")); assertTrue(cookie.contains("SameSite=Strict"));
        assertEquals(200,get("/static/css/aplomo.css").statusCode());
        assertEquals(404,get("/static/../plantillas/login.html").statusCode());
    }

    @Test void csrfProtegeIngresoYSalidaYSalirInvalidaLaSesion() throws Exception {
        assertEquals(403,post("/login",Map.of("correo",cuenta(admin).correo(),"clave",clave)).statusCode());
        assertEquals(303,entrar(admin).statusCode());
        var inicio=get("/"); assertEquals(200,inicio.statusCode());
        assertEquals(403,post("/logout",Map.of("csrf","invalido")).statusCode());
        assertEquals(200,get("/").statusCode());
        assertEquals(303,post("/logout",Map.of("csrf",csrf(inicio))).statusCode());
        assertEquals(303,get("/").statusCode());
    }

    @Test void anonimoYSalidaVoluntariaNoMuestranAvisoDeInactividad() throws Exception {
        get("/login");
        assertEquals("/login", get("/usuarios").headers().firstValue("Location").orElseThrow());
        assertEquals(303, entrar(admin).statusCode());
        var inicio = get("/");
        assertEquals(303, post("/logout", Map.of("csrf", csrf(inicio))).statusCode());
        var login = get("/login");
        assertFalse(login.body().contains("Tu sesión se cerró por inactividad"));
        assertEquals("/login", get("/usuarios").headers().firstValue("Location").orElseThrow());
    }

    @Test void formularioVencidoPermiteReintentarSinPerderElCorreo() throws Exception {
        var login = get("/login");
        reloj.avanzar(Duration.ofMinutes(30));
        var vencido = post("/login", Map.of("csrf", csrf(login),
                "correo", cuenta(admin).correo(), "clave", clave));
        assertEquals(403, vencido.statusCode());
        assertTrue(vencido.body().contains("El formulario venció"));
        assertTrue(vencido.body().contains("value=\"" + cuenta(admin).correo() + "\""));
        assertTrue(vencido.body().contains("<form"));
        assertFalse(vencido.body().contains(clave));
        assertNotEquals(csrf(login), csrf(vencido));
        assertEquals(303, post("/login", Map.of("csrf", csrf(vencido),
                "correo", cuenta(admin).correo(), "clave", clave)).statusCode());
    }

    @Test void errorDeIngresoEsGenericoYEscapado() throws Exception {
        var login=get("/login");
        var incorrecto=post("/login",Map.of("csrf",csrf(login),"correo",cuenta(admin).correo(),"clave","Mal2026"));
        var inexistente=post("/login",Map.of("csrf",csrf(login),"correo","<script>@prueba.invalid","clave","Mal2026"));
        assertEquals(401,incorrecto.statusCode()); assertEquals(401,inexistente.statusCode());
        assertTrue(incorrecto.body().contains("Correo o contraseña incorrectos"));
        assertTrue(inexistente.body().contains("Correo o contraseña incorrectos"));
        assertTrue(inexistente.body().contains("&lt;script&gt;@prueba.invalid"));
        assertFalse(inexistente.body().contains("<script>@prueba.invalid"));
        assertFalse(inexistente.body().contains("Mal2026"));
    }

    @Test void temporalNoAbreOtrasRutasYCambiaSoloConDatosValidos() throws Exception {
        assertEquals("/cuenta/contrasena",entrar(usuario).headers().firstValue("Location").orElseThrow());
        assertEquals("/cuenta/contrasena",get("/").headers().firstValue("Location").orElseThrow());
        assertEquals("/cuenta/contrasena",get("/usuarios").headers().firstValue("Location").orElseThrow());
        var cambio=get("/cuenta/contrasena");
        assertTrue(cambio.body().contains("Tu contraseña es temporal"));
        assertEquals(400,post("/cuenta/contrasena",Map.of("csrf",csrf(cambio),"actual","Mal2026","nueva","Nueva2026","confirmacion","Nueva2026")).statusCode());
        var respuesta=post("/cuenta/contrasena",Map.of("csrf",csrf(cambio),"actual",clave,"nueva","Nueva2026","confirmacion","Nueva2026"));
        assertEquals(303,respuesta.statusCode()); assertFalse(cuenta(usuario).temporal());
        var inicio=get("/"); assertEquals(200,inicio.statusCode());
        assertFalse(inicio.body().contains("href=\"/usuarios\""));
        assertTrue(inicio.body().contains("&lt;script&gt;sin ejecutar&lt;/script&gt;"));
    }

    @Test void usuarioNoPuedeAdministrarAunqueEscribaLaDireccion() throws Exception {
        dao.contrasena(con,usuario,cuenta(usuario).hash(),false);
        entrar(usuario); var inicio=get("/");
        assertEquals(403,get("/usuarios").statusCode());
        assertEquals(403,post("/usuarios",Map.of("csrf",csrf(inicio),"nombre","Ataque")).statusCode());
        assertEquals(403,get("/fotos/1").statusCode());
        assertFalse(inicio.body().contains("href=\"/usuarios\""));
    }

    @Test void administradorAccedeYUnaCookieViejaNoSobreviveAlIngreso() throws Exception {
        var login=get("/login"); String antigua=login.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        var ingreso=post("/login",Map.of("csrf",csrf(login),"correo",cuenta(admin).correo(),"clave",clave));
        assertNotEquals(antigua,ingreso.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0]);
        assertEquals(200,get("/usuarios").statusCode());
        var viejo=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(raiz+"/"))
                .header("Cookie",antigua).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(303,viejo.statusCode());
    }

    @Test void expiraTrasInactividadYUnaCuentaDesactivadaPierdeAcceso() throws Exception {
        entrar(admin); reloj.avanzar(Duration.ofMinutes(29)); assertEquals(200,get("/").statusCode());
        reloj.avanzar(Duration.ofMinutes(30)); assertEquals(303,get("/").statusCode());
        assertTrue(get("/login?inactividad=1").body().contains("por inactividad"));
        entrar(admin); dao.acceso(con,admin,0,null,Estado.INACTIVA);
        assertEquals(303,get("/").statusCode());
    }

    @Test void rechazaParametrosDuplicadosYCuerpoExcesivo() throws Exception {
        get("/login");
        for(String cuerpo:List.of("csrf=a&csrf=b","correo="+"a".repeat(16400))) {
            var r=cliente.send(HttpRequest.newBuilder(URI.create(raiz+"/login"))
                    .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(400,r.statusCode());
        }
    }
}
