package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.seguridad.*;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Autenticacion;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/** RF-001a a RF-002, RNF-004: rutas con autorización central, CSRF y mensajes sin detalles SQL. */
public final class Router implements HttpHandler {
    private static final String COOKIE = "APLOMO_SESION";
    private final Autenticacion auth;
    private final Sesiones sesiones;
    private final UsuariosWeb usuarios;
    private final EmpresasWeb empresas;

    /** Construye las rutas con servicios reales (RNF-011). */
    public Router() { this(new Autenticacion(),new Sesiones()); }
    /** Permite comprobar HTTP con servicios y reloj controlados (RNF-011). */
    public Router(Autenticacion auth, Sesiones sesiones) { this(auth,sesiones,new co.aplomo.servicio.Usuarios()); }
    /** Comparte los servicios de cuentas para verificar todo el recorrido HTTP (RNF-011). */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios) {
        this(auth, sesiones, usuarios, new co.aplomo.servicio.Empresas());
    }
    /** Comparte los servicios transaccionales de configuración para las pruebas HTTP (RNF-011). */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
                  co.aplomo.servicio.Empresas empresas) {
        this.auth=auth; this.sesiones=sesiones; this.usuarios=new UsuariosWeb(usuarios,sesiones);
        this.empresas=new EmpresasWeb(empresas);
    }

    /** Revisa la sesión antes de atender cualquier ruta interna (RF-002). */
    @Override public void handle(HttpExchange x) throws IOException {
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        x.getResponseHeaders().set("Referrer-Policy","same-origin");
        x.getResponseHeaders().set("X-Frame-Options","DENY");
        try { atender(x); }
        catch (SecurityException e) { enviar(x,403,Html.pagina("Acceso denegado",Html.aviso(e.getMessage(),false),null)); }
        catch (IllegalArgumentException e) { enviar(x,400,Html.pagina("Revisa los datos",Html.aviso(e.getMessage(),false),null)); }
        catch (SQLException e) { enviar(x,503,Html.pagina("No pudimos completar la acción",Html.aviso("Revisa la conexión e inténtalo de nuevo. Tus datos guardados se conservan.",false),null)); }
        finally { x.close(); }
    }

    private void atender(HttpExchange x) throws IOException, SQLException {
        String ruta = x.getRequestURI().getPath(), metodo = x.getRequestMethod();
        if (!metodo.equals("GET") && !metodo.equals("POST")) {
            x.getResponseHeaders().set("Allow","GET, POST"); enviar(x,405,"Método no permitido."); return;
        }
        if (ruta.startsWith("/static/")) { estatico(x,ruta); return; }
        String id = cookie(x);
        Sesion s = sesiones.obtener(id);
        if (s != null && s.cuenta() != null) {
            Cuenta c = auth.actual(s.cuenta().id());
            if (!vigente(s.cuenta(),c)) { sesiones.cerrar(id); s=null; }
        }
        if (ruta.equals("/login")) {
            if (s != null && s.cuenta() != null) { redirigir(x,s.cuenta().temporal() ? "/cuenta/contrasena" : "/"); return; }
            if (metodo.equals("GET")) {
                if (s == null) { s=sesiones.crear(null); ponerCookie(x,s.id()); }
                String aviso = "inactividad=1".equals(x.getRequestURI().getRawQuery())
                        ? "Tu sesión se cerró por inactividad o porque cambió el acceso de tu cuenta. Vuelve a ingresar." : null;
                enviar(x,200,Html.login(s,"",aviso)); return;
            }
            Map<String,String> f = formulario(x);
            if (!Sesiones.csrfValido(s,f.get("csrf"))) {
                // RNF-004, RNF-007: rechaza el envío y permite reintentar con un formulario vigente.
                if (s == null) { s=sesiones.crear(null); ponerCookie(x,s.id()); }
                enviar(x,403,Html.login(s,f.get("correo"),
                        "El formulario venció. Vuelve a escribir tu contraseña e inténtalo de nuevo."));
                return;
            }
            Cuenta c;
            try { c=auth.ingresar(f.get("correo"),f.get("clave")); }
            catch(SQLException e) { enviar(x,503,Html.login(s,f.get("correo"),"No pudimos verificar el ingreso. Revisa la conexión e inténtalo de nuevo.")); return; }
            if (c == null) {
                enviar(x,401,Html.login(s,f.get("correo"),"Correo o contraseña incorrectos. Revisa los datos o vuelve a intentarlo en 15 minutos.")); return;
            }
            sesiones.cerrar(s.id()); s=sesiones.crear(c); ponerCookie(x,s.id());
            redirigir(x,c.temporal() ? "/cuenta/contrasena" : "/"); return;
        }
        if (s == null || s.cuenta() == null) {
            // RF-001b, RNF-007: un formulario anónimo vigente no es una sesión cerrada.
            boolean vencida = s == null && id != null;
            if (vencida) ponerCookie(x,"");
            redirigir(x,vencida ? "/login?inactividad=1" : "/login"); return;
        }
        Map<String,String> f = metodo.equals("POST") ? formulario(x) : Map.of();
        if (metodo.equals("POST")) exigirCsrf(s,f);
        if (ruta.equals("/logout")) {
            if (!metodo.equals("POST")) { enviar(x,405,"Usa el botón Cerrar sesión."); return; }
            sesiones.cerrar(s.id()); ponerCookie(x,""); redirigir(x,"/login"); return;
        }
        if (s.cuenta().temporal() && !ruta.equals("/cuenta/contrasena")) { redirigir(x,"/cuenta/contrasena"); return; }
        if (ruta.equals("/cuenta/contrasena")) {
            if (metodo.equals("GET")) { enviar(x,200,Html.contrasena(s,null)); return; }
            Cuenta cambiada;
            try { cambiada=auth.cambiar(s.cuenta(),f.get("actual"),f.get("nueva"),f.get("confirmacion")); }
            catch (IllegalArgumentException e) { enviar(x,400,Html.contrasena(s,e.getMessage())); return; }
            catch (SQLException e) { enviar(x,503,Html.contrasena(s,"No pudimos guardar la contraseña. Inténtalo de nuevo.")); return; }
            sesiones.invalidarCuenta(cambiada.id()); s=sesiones.crear(cambiada); ponerCookie(x,s.id());
            redirigir(x,"/?contrasena=1"); return;
        }
        if (ruta.equals("/usuarios") || ruta.startsWith("/usuarios/")) {
            Permisos.administrador(s.cuenta());
            usuarios.atender(x,s,f); return;
        }
        if (ruta.equals("/empresas") || ruta.startsWith("/empresas/") || ruta.startsWith("/sedes/")
                || ruta.startsWith("/salones/") || ruta.startsWith("/areas/") || ruta.startsWith("/cargos/")
                || ruta.startsWith("/puestos/")) {
            empresas.atender(x, s, f); return;
        }
        if (ruta.startsWith("/fotos/")) {
            Permisos.administrador(s.cuenta()); enviar(x,404,Html.pagina("Foto no disponible","",s)); return;
        }
        if (ruta.equals("/") && metodo.equals("GET")) {
            String aviso = "contrasena=1".equals(x.getRequestURI().getRawQuery()) ? Html.aviso("Contraseña actualizada.",true) : "";
            enviar(x,200,Html.pagina("Inicio",aviso + "<section class=\"card stack\"><h2>Bienvenido, "
                    + Html.escapar(s.cuenta().nombre()) + "</h2><p>Tu sesión está activa. Desde aquí puedes acceder a las opciones de tu cuenta.</p></section>",s)); return;
        }
        enviar(x,404,Html.pagina("Página no disponible","<a href=\"/\">Volver al inicio</a>",s));
    }

    private static boolean vigente(Cuenta antes, Cuenta ahora) {
        return ahora != null && ahora.estado() == Estado.ACTIVA && antes.hash().equals(ahora.hash())
                && antes.rol() == ahora.rol() && Objects.equals(antes.empresa(),ahora.empresa())
                && antes.temporal() == ahora.temporal();
    }

    private static void exigirCsrf(Sesion s, Map<String,String> f) {
        if (!Sesiones.csrfValido(s,f.get("csrf"))) throw new SecurityException("El formulario venció. Recarga la página e inténtalo de nuevo.");
    }

    private static String cookie(HttpExchange x) {
        for (String encabezado : x.getRequestHeaders().getOrDefault("Cookie",List.of())) {
            for (String parte : encabezado.split(";")) {
                String[] par=parte.strip().split("=",2);
                if (par.length==2 && par[0].equals(COOKIE)) return par[1];
            }
        }
        return null;
    }

    private static void ponerCookie(HttpExchange x, String valor) {
        x.getResponseHeaders().add("Set-Cookie",COOKIE+"="+valor+"; Path=/; HttpOnly; SameSite=Strict"
                + (valor.isEmpty() ? "; Max-Age=0" : "") + (x instanceof HttpsExchange ? "; Secure" : ""));
    }

    /** Lee formularios acotados sin aceptar parámetros duplicados (RNF-004). */
    public static Map<String,String> formulario(HttpExchange x) throws IOException {
        String tipo=x.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";",2)[0].equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Envía los datos desde el formulario de la página.");
        }
        byte[] bytes=x.getRequestBody().readNBytes(16385);
        if (bytes.length>16384) throw new IllegalArgumentException("El formulario es demasiado grande.");
        return parametros(new String(bytes, StandardCharsets.UTF_8));
    }

    /** Lee parámetros de consulta sin aceptar campos duplicados ni direcciones excesivas (RNF-004). */
    public static Map<String,String> consulta(HttpExchange x) {
        String texto = x.getRequestURI().getRawQuery();
        if (texto == null) return Map.of();
        if (texto.length() > 16384) throw new IllegalArgumentException("La dirección es demasiado larga.");
        return parametros(texto);
    }

    private static Map<String,String> parametros(String texto) {
        Map<String,String> f=new HashMap<>();
        for (String par : texto.split("&")) {
            if (par.isEmpty()) continue;
            String[] partes=par.split("=",2);
            String clave=URLDecoder.decode(partes[0],StandardCharsets.UTF_8);
            String valor=URLDecoder.decode(partes.length==2 ? partes[1] : "",StandardCharsets.UTF_8);
            if (f.putIfAbsent(clave,valor)!=null) throw new IllegalArgumentException("El formulario contiene campos repetidos. Recarga la página.");
        }
        return f;
    }

    /** Envía HTML en UTF-8 (RNF-007). */
    public static void enviar(HttpExchange x, int estado, String html) throws IOException {
        byte[] bytes=html.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8");
        x.sendResponseHeaders(estado,bytes.length);
        x.getResponseBody().write(bytes);
    }

    /** Redirige después de un POST para evitar repetir operaciones al recargar (RNF-002). */
    public static void redirigir(HttpExchange x, String ruta) throws IOException {
        x.getResponseHeaders().set("Location",ruta); x.sendResponseHeaders(303,-1);
    }

    private static void estatico(HttpExchange x, String ruta) throws IOException {
        if (!x.getRequestMethod().equals("GET")) { enviar(x,405,"Método no permitido."); return; }
        if (ruta.contains("..") || !(ruta.endsWith(".css") || ruta.endsWith(".js") || ruta.endsWith(".jpg") || ruta.endsWith(".png"))) {
            enviar(x,404,"Archivo no disponible."); return;
        }
        try (var entrada=Router.class.getResourceAsStream(ruta)) {
            if (entrada == null) { enviar(x,404,"Archivo no disponible."); return; }
            String tipo=ruta.endsWith(".css") ? "text/css; charset=UTF-8" : ruta.endsWith(".js") ? "text/javascript; charset=UTF-8"
                    : ruta.endsWith(".jpg") ? "image/jpeg" : "image/png";
            byte[] bytes=entrada.readAllBytes();
            x.getResponseHeaders().set("Content-Type",tipo); x.sendResponseHeaders(200,bytes.length); x.getResponseBody().write(bytes);
        }
    }
}
