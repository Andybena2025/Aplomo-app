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
    private final ReservasWeb reservas;
    private final VisitasWeb visitas;
    private final EvaluacionesWeb evaluaciones;
    private final PendientesWeb pendientes;
    private final SugerenciasWeb sugerencias;
    private final ConsultasWeb consultas;
    private final CalificacionesWeb calificaciones;

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
        this(auth, sesiones, usuarios, empresas, new co.aplomo.servicio.Reservas());
    }
    /** Comparte los servicios de configuración y agenda para las comprobaciones HTTP. */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
                  co.aplomo.servicio.Empresas empresas, co.aplomo.servicio.Reservas reservas) {
        this(auth, sesiones, usuarios, empresas, reservas, new co.aplomo.servicio.Visitas());
    }
    /** Comparte todos los servicios de la fase 4 con sus transacciones de prueba. */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
                  co.aplomo.servicio.Empresas empresas, co.aplomo.servicio.Reservas reservas,
                  co.aplomo.servicio.Visitas visitas) {
        this(auth, sesiones, usuarios, empresas, reservas, visitas, new co.aplomo.servicio.Evaluaciones());
    }
    /** Comparte los servicios y la carpeta aislada del paso 7 para las pruebas HTTP. */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
                  co.aplomo.servicio.Empresas empresas, co.aplomo.servicio.Reservas reservas,
                  co.aplomo.servicio.Visitas visitas, co.aplomo.servicio.Evaluaciones evaluaciones) {
        this.auth=auth; this.sesiones=sesiones; this.usuarios=new UsuariosWeb(usuarios,sesiones);
        this.empresas=new EmpresasWeb(empresas);
        this.reservas=new ReservasWeb(reservas, empresas);
        this.visitas=new VisitasWeb(visitas, reservas);
        this.evaluaciones=new EvaluacionesWeb(evaluaciones);
        this.pendientes=new PendientesWeb(evaluaciones.pendientes());
        this.sugerencias=new SugerenciasWeb(evaluaciones.sugerencias());
        this.consultas=new ConsultasWeb(evaluaciones.consultas(),evaluaciones.sugerencias());
        this.calificaciones=new CalificacionesWeb(evaluaciones.calificaciones());
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
            if (ruta.startsWith("/fotos/")) throw new SecurityException("Acceso denegado.");
            boolean vencida = s == null && id != null;
            if (vencida) ponerCookie(x,"");
            redirigir(x,vencida ? "/login?inactividad=1" : "/login"); return;
        }
        if(consultas.atender(x,s))return;
        if (ruta.startsWith("/evaluaciones") || ruta.startsWith("/fotos/")) {
            Permisos.administrador(s.cuenta());
            if (s.cuenta().temporal()) { redirigir(x,"/cuenta/contrasena"); return; }
            evaluaciones.atender(x,s); return;
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
        if(calificaciones.atender(x,s))return;
        if(ruta.equals("/sugerencias")||ruta.startsWith("/sugerencias/")){sugerencias.atender(x,s,f);return;}
        if(ruta.equals("/pendientes")||ruta.startsWith("/pendientes/")){pendientes.atender(x,s,f);return;}
        if (ruta.equals("/usuarios") || ruta.startsWith("/usuarios/")) {
            Permisos.administrador(s.cuenta());
            usuarios.atender(x,s,f); return;
        }
        if (ruta.equals("/empresas") || ruta.startsWith("/empresas/") || ruta.startsWith("/sedes/")
                || ruta.startsWith("/salones/") || ruta.startsWith("/areas/") || ruta.startsWith("/cargos/")
                || ruta.startsWith("/puestos/")) {
            empresas.atender(x, s, f); return;
        }
        if (ruta.equals("/visitas") || ruta.startsWith("/visitas/") || ruta.matches("/reservas/[1-9][0-9]*/iniciar-visita")) {
            visitas.atender(x, s, f); return;
        }
        if (ruta.equals("/reservas") || ruta.startsWith("/reservas/")) {
            reservas.atender(x, s, f); return;
        }
        if (ruta.startsWith("/fotos/")) {
            Permisos.administrador(s.cuenta()); enviar(x,404,Html.pagina("Foto no disponible","",s)); return;
        }
        if (ruta.equals("/") && metodo.equals("GET")) {
            String aviso = "contrasena=1".equals(x.getRequestURI().getRawQuery()) ? Html.aviso("Contraseña actualizada.",true) : "";
            Cuenta c = s.cuenta();
            String nombre = c.nombre().contains(" ") ? c.nombre().substring(0, c.nombre().indexOf(' ')) : c.nombre();
            // Consultar datos reales para el panel de inicio (RF-013a)
            var listaReservas = reservas.servicio().listar(c);
            var listaVisitas = visitas.servicio().listar(c);
            long enCurso = listaVisitas.stream().filter(v -> v.visita().estado().equals("EN_CURSO")).count();
            long enCola = listaReservas.stream().filter(r -> r.reserva().estado().equals("EN_COLA")).count();
            long[] resumen=evaluaciones.servicio().resumen(c);
            StringBuilder inicio = new StringBuilder(aviso);
            // Saludo con fecha como en el mock
            inicio.append("<div class=\"pagehead\"><div><span class=\"eyebrow\">")
                    .append(Html.fecha(Html.hoy()).toUpperCase(java.util.Locale.ROOT)).append("</span><h1>Hola, ")
                    .append(Html.escapar(nombre)).append("</h1></div></div>");
            // 4 tarjetas KPI como en el mock
            inicio.append("<div class=\"grid4\" style=\"margin-bottom:24px\">")
                    .append(kpi("Visitas en curso", String.valueOf(enCurso), null))
                    .append(kpi("Reservas en cola", String.valueOf(enCola), null))
                    .append("<a class=\"kpi-enlace\" href=\"/evaluaciones\">").append(kpi("Evaluaciones", String.valueOf(resumen[0]), null)).append("</a>")
                    .append(kpi("Puestos en rojo", String.valueOf(resumen[1]), "color:var(--rojo)"))
                    .append("</div>");
            if (c.rol() == Cuenta.Rol.ADMINISTRADOR) {
                inicio.append("<div class=\"inicio-acciones\">");
                inicio.append(accionInicio("/reservas/nueva", "Nueva reserva", "Programa una evaluación.", "M5 7h14v13H5z M8 4v6 M16 4v6 M5 12h14"));
                inicio.append(accionInicio("/reservas", "Iniciar visita", "Continúa desde una reserva en cola.", "M9 5H5v15h14v-5 M12 4h8v8 M20 4L10 14"));
                inicio.append(accionInicio("/empresas/nueva", "Nueva empresa", "Registra la empresa y sus sedes.", "M5 21V5h10v16 M15 11h4v10 M9 9h2 M9 13h2 M9 17h2 M3 21h18"));
                inicio.append("</div>");
            }
            // Visitas en curso y reservas recientes como en el mock
            inicio.append("<div class=\"grid2\"><section class=\"stack\"><h3>Visitas en curso</h3>");
            var cursando = listaVisitas.stream().filter(v -> v.visita().estado().equals("EN_CURSO")).toList();
            if (cursando.isEmpty()) {
                inicio.append(c.rol()==Cuenta.Rol.ADMINISTRADOR ? "<div class=\"empty\">No hay visitas en curso. Inicia una desde una reserva En cola.</div>" : "<div class=\"empty\">No hay visitas en curso disponibles.</div>");
            } else {
                for (var v : cursando) {
                    inicio.append("<div class=\"card row between\"><div><div>").append(Html.escapar(v.empresaNombre()))
                            .append("</div><div class=\"muted small\">").append(Html.escapar(v.sedeNombre()))
                            .append(" · desde ").append(Html.fecha(v.visita().inicio().toLocalDate()))
                            .append("</div></div><a class=\"btn p sm\" href=\"/visitas/").append(v.visita().id())
                            .append("\">Continuar</a></div>");
                }
            }
            inicio.append("</section><section class=\"stack\"><h3>Reservas</h3>");
            var pendientes = listaReservas.stream()
                    .filter(r -> r.reserva().estado().equals("EN_COLA") || r.reserva().estado().equals("ESPERANDO")).toList();
            if (pendientes.isEmpty()) {
                inicio.append("<div class=\"empty\">No hay reservas pendientes.</div>");
            } else {
                for (var r : pendientes) {
                    inicio.append("<div class=\"card stack\" style=\"gap:10px\"><div class=\"row between\"><div><div>")
                            .append(Html.escapar(r.empresaNombre())).append("</div><div class=\"muted small\">")
                            .append(Html.escapar(r.sedeNombre())).append(" · ").append(Html.fecha(r.reserva().fecha()))
                            .append(" · ").append(r.reserva().inicio()).append("</div></div><span class=\"tag")
                            .append(r.reserva().estado().equals("EN_COLA") ? " good" : " warn").append("\">")
                            .append(r.reserva().estado().equals("EN_COLA") ? "En cola" : "Esperando")
                            .append("</span></div>");
                    if (r.reserva().descripcion() != null && !r.reserva().descripcion().isBlank()) {
                        inicio.append("<p class=\"small muted\">").append(Html.escapar(r.reserva().descripcion())).append("</p>");
                    }
                    if (r.reserva().estado().equals("EN_COLA") && r.visita() == null) {
                        inicio.append("<div class=\"row\"><a class=\"btn sm p\" href=\"/reservas/").append(r.reserva().id())
                                .append("\">Ver reserva</a></div>");
                    }
                    inicio.append("</div>");
                }
            }
            inicio.append("</section></div>");
            enviar(x,200,Html.pagina("Inicio",inicio.toString(),s,false)); return;
        }
        enviar(x,404,Html.pagina("Página no disponible","<a href=\"/\">Volver al inicio</a>",s));
    }

    private static String accionInicio(String ruta, String titulo, String ayuda, String trazo) {
        return "<a class=\"card accion-inicio\" href=\"" + ruta + "\"><svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"" + trazo
                + "\"/></svg><span><strong>" + titulo + "</strong><span class=\"small muted\">" + ayuda + "</span></span></a>";
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
        String ruta = x.getRequestURI().getPath();
        int limite = ruta.startsWith("/reservas") || ruta.startsWith("/visitas") || ruta.startsWith("/evaluaciones") ? 262144 : 16384;
        byte[] bytes=x.getRequestBody().readNBytes(limite + 1);
        if (bytes.length > limite) throw new IllegalArgumentException("El formulario es demasiado grande.");
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
        responder(x, estado, "text/html; charset=UTF-8", html);
    }

    /** Envía una respuesta con el tipo explícito del contenido generado por la aplicación. */
    public static void responder(HttpExchange x, int estado, String tipo, String contenido) throws IOException {
        byte[] bytes=contenido.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type",tipo);
        x.sendResponseHeaders(estado,bytes.length);
        x.getResponseBody().write(bytes);
    }

    /** Redirige después de un POST para evitar repetir operaciones al recargar (RNF-002). */
    public static void redirigir(HttpExchange x, String ruta) throws IOException {
        x.getResponseHeaders().set("Location",ruta); x.sendResponseHeaders(303,-1);
    }

    private static String kpi(String titulo, String valor, String estilo) {
        return "<div class=\"card kpi\"><span class=\"eyebrow\">" + Html.escapar(titulo) + "</span><b class=\"num\""
                + (estilo != null ? " style=\"" + estilo + "\"" : "") + ">" + Html.escapar(valor) + "</b></div>";
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
