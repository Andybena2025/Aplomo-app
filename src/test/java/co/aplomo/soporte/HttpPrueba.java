package co.aplomo.soporte;

import co.aplomo.web.Router;

import co.aplomo.seguridad.Sesiones;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Servidor HTTP y navegador de pruebas, con datos aislados por transacción. */
public abstract class HttpPrueba extends BasePrueba {
    protected HttpServer servidor;
    protected HttpClient cliente;
    protected String raiz;

    @BeforeEach void abrirServidor() throws Exception {
        servidor=HttpServer.create(new InetSocketAddress("localhost",0),0);
        servidor.createContext("/",new Router(auth,new Sesiones(reloj),servicioUsuarios()));
        servidor.start(); raiz="http://localhost:"+servidor.getAddress().getPort();
        cliente=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
    }
    @AfterEach void cerrarServidor() { if(servidor!=null) servidor.stop(0); }

    protected co.aplomo.servicio.Usuarios servicioUsuarios() {
        return new co.aplomo.servicio.Usuarios(tx,dao,new co.aplomo.dao.EmpresaDao());
    }

    protected HttpResponse<String> get(String ruta) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz+ruta)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    protected HttpResponse<String> post(String ruta, Map<String,String> datos) throws Exception {
        String cuerpo=datos.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="
                +URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz+ruta)).header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(),HttpResponse.BodyHandlers.ofString());
    }
    protected String csrf(HttpResponse<String> r) {
        var m=Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(r.body());
        assertTrue(m.find()); return m.group(1);
    }
    protected HttpResponse<String> entrar(int id) throws Exception {
        var ingreso=get("/login");
        return post("/login",Map.of("csrf",csrf(ingreso),"correo",cuenta(id).correo(),"clave",clave));
    }

}
