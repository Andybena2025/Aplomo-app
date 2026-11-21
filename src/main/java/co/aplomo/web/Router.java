package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Autenticacion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-001a a RF-002, RNF-004: rutas con autorización central, CSRF y mensajes sin detalles SQL.
 */
public final class Router implements HttpHandler {
    private static final String COOKIE = "APLOMO_SESION";
    private final Autenticacion auth;
    private final Sesiones sesiones;
    private final UsuariosWeb usuarios;
    private final EmpresasWeb empresas;
    private final ReservasWeb reservas;
    private final AvisosWeb avisos;
    private final SolicitudesVisitaWeb solicitudesVisita;
    private final VisitasWeb visitas;
    private final EvaluacionesWeb evaluaciones;
    private final PendientesWeb pendientes;
    private final SugerenciasWeb sugerencias;
    private final ConsultasWeb consultas;
    private final CalificacionesWeb calificaciones;
    private final CuentaWeb cuenta;
    private final ArchivoWeb archivo;
    private final AccesoWeb acceso;
    private final InicioWeb inicio;
    private final AgendaWeb agenda;
    private final DemoWeb demo;

    /**
     * Construye las rutas con servicios reales (RNF-011).
     */
    public Router() {
        this(new Autenticacion(), new Sesiones());
    }

    /**
     * Permite comprobar HTTP con servicios y reloj controlados (RNF-011).
     */
    public Router(Autenticacion auth, Sesiones sesiones) {
        this(auth, sesiones, new co.aplomo.servicio.Usuarios());
    }

    /**
     * Comparte los servicios de cuentas para verificar todo el recorrido HTTP (RNF-011).
     */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios) {
        this(auth, sesiones, usuarios, new co.aplomo.servicio.Empresas());
    }

    /**
     * Comparte los servicios transaccionales de configuración para las pruebas HTTP (RNF-011).
     */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
            co.aplomo.servicio.Empresas empresas) {
        this(auth, sesiones, usuarios, empresas, new co.aplomo.servicio.Reservas());
    }

    /**
     * Comparte los servicios de configuración y agenda para las comprobaciones HTTP. RNF-011.
     */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
            co.aplomo.servicio.Empresas empresas, co.aplomo.servicio.Reservas reservas) {
        this(auth, sesiones, usuarios, empresas, reservas, new co.aplomo.servicio.Visitas());
    }

    /**
     * Comparte todos los servicios de la fase 4 con sus transacciones de prueba. RNF-011.
     */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
            co.aplomo.servicio.Empresas empresas, co.aplomo.servicio.Reservas reservas,
            co.aplomo.servicio.Visitas visitas) {
        this(auth, sesiones, usuarios, empresas, reservas, visitas, new co.aplomo.servicio.Evaluaciones());
    }

    /**
     * Comparte los servicios y la carpeta aislada del paso 7 para las pruebas HTTP. RNF-011.
     */
    public Router(Autenticacion auth, Sesiones sesiones, co.aplomo.servicio.Usuarios usuarios,
            co.aplomo.servicio.Empresas empresas, co.aplomo.servicio.Reservas reservas,
            co.aplomo.servicio.Visitas visitas, co.aplomo.servicio.Evaluaciones evaluaciones) {
        this.agenda = new AgendaWeb(reservas.agenda());
        this.avisos = new AvisosWeb(reservas.avisos());
        this.solicitudesVisita = new SolicitudesVisitaWeb(reservas.solicitudes());
        this.demo = new DemoWeb(visitas);
        this.acceso = new AccesoWeb(auth, sesiones);
        this.inicio = new InicioWeb(empresas, reservas, visitas, evaluaciones);
        this.archivo = new ArchivoWeb(evaluaciones.consultas().archivo());
        this.cuenta = new CuentaWeb(usuarios.perfil(), auth, sesiones);
        this.auth = auth;
        this.sesiones = sesiones;
        this.usuarios = new UsuariosWeb(usuarios, sesiones);
        this.empresas = new EmpresasWeb(empresas);
        this.reservas = new ReservasWeb(reservas, empresas);
        this.visitas = new VisitasWeb(visitas, reservas);
        this.evaluaciones = new EvaluacionesWeb(evaluaciones);
        this.pendientes = new PendientesWeb(evaluaciones.pendientes());
        this.sugerencias = new SugerenciasWeb(evaluaciones.sugerencias());
        this.consultas = new ConsultasWeb(evaluaciones.consultas(), evaluaciones.sugerencias());
        this.calificaciones = new CalificacionesWeb(evaluaciones.calificaciones());
    }

    /**
     * Revisa la sesión antes de atender cualquier ruta interna (RF-002).
     */
    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        intercambio.getResponseHeaders().set("Referrer-Policy", "same-origin");
        intercambio.getResponseHeaders().set("X-Frame-Options", "DENY");
        try {
            atender(intercambio);
        } catch (SecurityException error) {
            enviar(intercambio, 403, Html.pagina("Acceso denegado", Html.aviso(error.getMessage(), false), null));
        } catch (IllegalArgumentException error2) {
            enviar(intercambio, 400, Html.pagina("Revisa los datos", Html.aviso(error2.getMessage(), false), null));
        } catch (SQLException error3) {
            enviar(intercambio, 503, Html.pagina("No pudimos completar la acción",
                    Html.aviso("Revisa la conexión e inténtalo de nuevo. Tus datos guardados se conservan.", false),
                    null));
        } catch (RuntimeException error4) {
            if (intercambio.getResponseCode() == -1) enviar(intercambio, 500, Html.error("No pudimos abrir esta página",
                    "Inténtalo de nuevo o vuelve al inicio. Tus datos guardados se conservan."));
        } finally {
            intercambio.close();
        }
    }

    private void atender(HttpExchange intercambio) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath(), metodo = intercambio.getRequestMethod();
        if (!metodo.equals("GET") && !metodo.equals("POST")) {
            intercambio.getResponseHeaders().set("Allow", "GET, POST");
            enviar(intercambio, 405, "Método no permitido.");
            return;
        }
        if (ruta.equals("/favicon.ico") || ruta.equals("/favicon.svg")) {
            if (!metodo.equals("GET")) {
                enviar(intercambio, 405, "Método no permitido.");
                return;
            }
            responder(intercambio, 200, "image/svg+xml; charset=UTF-8", Html.favicon());
            return;
        }
        if (ruta.startsWith("/static/")) {
            estatico(intercambio, ruta);
            return;
        }
        if (ruta.equals("/nosotros") && metodo.equals("GET")) {
            redirigir(intercambio, "/login");
            return;
        }
        String id = cookie(intercambio);
        Sesion sesion = (ruta.equals("/notas/nuevas") || ruta.equals("/avisos/nuevos")) ? sesiones.consultar(
                id) : sesiones.obtener(id);
        if (sesion != null && sesion.cuenta() != null) {
            Cuenta cuenta2 = auth.actual(sesion.cuenta().id());
            if (!vigente(sesion.cuenta(), cuenta2)) {
                sesiones.cerrar(id);
                sesion = null;
            }
        }
        if (ruta.equals("/login")) {
            acceso.ingresar(intercambio, sesion);
            return;
        }
        if (ruta.equals("/ayuda") && metodo.equals("GET")) {
            enviar(intercambio, 200, PaginasAyuda.pagina(sesion != null && sesion.cuenta() != null ? sesion : null,
                    evaluaciones.servicio().ayuda().consultar(sesion == null ? null : sesion.cuenta())));
            return;
        }
        if (sesion == null || sesion.cuenta() == null) {

            // RF-001b, RNF-007: un formulario anónimo vigente no es una sesión cerrada.
            if (ruta.startsWith("/fotos/")) throw new SecurityException("Acceso denegado.");
            if (ruta.equals("/avisos/nuevos") || ruta.equals("/notas/nuevas")) {
                responder(intercambio, 401, "application/json; charset=UTF-8", "{\"sesion\":\"vencida\"}");
                return;
            }
            boolean vencida = sesion == null && id != null;
            if (vencida) ponerCookie(intercambio, "");
            redirigir(intercambio, vencida ? "/login?inactividad=1" : "/login");
            return;
        }
        if (metodo.equals("GET") && avisos.atender(intercambio, sesion, Map.of())) return;
        if (metodo.equals("GET") && solicitudesVisita.atender(intercambio, sesion, Map.of())) return;
        if (metodo.equals("GET") && agenda.atender(intercambio, sesion, Map.of())) return;
        if (archivo.atender(intercambio, sesion)) return;
        if (ruta.equals("/empresas") && metodo.equals("GET")) {
            if (sesion.cuenta().temporal()) redirigir(intercambio, "/cuenta/contrasena");
            else empresas.atender(intercambio, sesion, Map.of());
            return;
        }
        if (consultas.atender(intercambio, sesion)) return;
        if (ruta.startsWith("/evaluaciones") || ruta.startsWith("/fotos/")) {
            Permisos.trabajo(sesion.cuenta());
            if (sesion.cuenta().temporal()) {
                redirigir(intercambio, "/cuenta/contrasena");
                return;
            }
            evaluaciones.atender(intercambio, sesion);
            return;
        }
        Map<String, String> parametros2 = metodo.equals("POST") ? formulario(intercambio) : Map.of();
        if (metodo.equals("POST")) exigirCsrf(sesion, parametros2);
        if (avisos.atender(intercambio, sesion, parametros2)) return;
        if (solicitudesVisita.atender(intercambio, sesion, parametros2)) return;
        if (agenda.atender(intercambio, sesion, parametros2)) return;
        if (ruta.equals("/logout")) {
            acceso.cerrar(intercambio, sesion);
            return;
        }
        if (sesion.cuenta().temporal() && !ruta.equals("/cuenta/contrasena")) {
            redirigir(intercambio, "/cuenta/contrasena");
            return;
        }
        if (ruta.equals("/cuenta/contrasena")) {
            acceso.contrasena(intercambio, sesion, parametros2);
            return;
        }
        if (cuenta.atender(intercambio, sesion, parametros2)) return;
        if (ruta.equals("/demo/evaluacion")) {
            demo.atender(intercambio, sesion, parametros2);
            return;
        }
        if (calificaciones.atender(intercambio, sesion)) return;
        if (ruta.equals("/sugerencias") || ruta.startsWith("/sugerencias/")) {
            sugerencias.atender(intercambio, sesion, parametros2);
            return;
        }
        if (ruta.equals("/pendientes") || ruta.startsWith("/pendientes/")) {
            pendientes.atender(intercambio, sesion, parametros2);
            return;
        }
        if (ruta.equals("/usuarios") || ruta.startsWith("/usuarios/")) {
            Permisos.administrador(sesion.cuenta());
            usuarios.atender(intercambio, sesion, parametros2);
            return;
        }
        if (ruta.equals("/empresas") || ruta.startsWith("/empresas/") || ruta.startsWith("/sedes/") ||
                ruta.startsWith("/salones/") || ruta.startsWith("/areas/") || ruta.startsWith("/cargos/") ||
                ruta.startsWith("/puestos/")) {
            empresas.atender(intercambio, sesion, parametros2);
            return;
        }
        if (ruta.equals("/visitas") || ruta.startsWith("/visitas/") ||
                ruta.matches("/reservas/[1-9][0-9]*/iniciar-visita")) {
            visitas.atender(intercambio, sesion, parametros2);
            return;
        }
        if (ruta.equals("/reservas") || ruta.startsWith("/reservas/")) {
            reservas.atender(intercambio, sesion, parametros2);
            return;
        }
        if (ruta.startsWith("/fotos/")) {
            Permisos.administrador(sesion.cuenta());
            enviar(intercambio, 404, Html.pagina("Foto no disponible", "", sesion));
            return;
        }
        if (ruta.equals("/") && metodo.equals("GET")) {
            inicio.atender(intercambio, sesion);
            return;
        }
        enviar(intercambio, 404, Html.pagina("Página no disponible", "<a href=\"/\">Volver al inicio</a>", sesion));
    }

    private static boolean vigente(Cuenta antes, Cuenta ahora) {
        return ahora != null && ahora.estado() == Estado.ACTIVA && antes.hash().equals(ahora.hash()) &&
                antes.rol() == ahora.rol() && antes.temporal() == ahora.temporal();
    }

    private static void exigirCsrf(Sesion sesion, Map<String, String> parametros2) {
        if (!Sesiones.csrfValido(sesion, parametros2.get("csrf")))
            throw new SecurityException("El formulario venció. Recarga la página e inténtalo de nuevo.");
    }

    private static String cookie(HttpExchange intercambio) {
        for (String encabezado : intercambio.getRequestHeaders().getOrDefault("Cookie", List.of())) {
            for (String parte : encabezado.split(";")) {
                String[] par = parte.strip().split("=", 2);
                if (par.length == 2 && par[0].equals(COOKIE)) return par[1];
            }
        }
        return null;
    }

    static void ponerCookie(HttpExchange intercambio, String valor) {
        intercambio.getResponseHeaders().add("Set-Cookie",
                COOKIE + "=" + valor + "; Path=/; HttpOnly; SameSite=Strict" + (valor.isEmpty() ? "; Max-Age=0" : "") +
                        (intercambio instanceof HttpsExchange ? "; Secure" : ""));
    }

    /**
     * Lee formularios acotados sin aceptar parámetros duplicados (RNF-004).
     */
    public static Map<String, String> formulario(HttpExchange intercambio) throws IOException {
        String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Envía los datos desde el formulario de la página.");
        }
        String ruta = intercambio.getRequestURI().getPath();
        int limite = ruta.startsWith("/reservas") || ruta.startsWith("/visitas") ||
                ruta.startsWith("/evaluaciones") ? 262144 : 16384;
        byte[] bytes = intercambio.getRequestBody().readNBytes(limite + 1);
        if (bytes.length > limite) throw new IllegalArgumentException("El formulario es demasiado grande.");
        return parametros(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Lee parámetros de consulta sin aceptar campos duplicados ni direcciones excesivas (RNF-004).
     */
    public static Map<String, String> consulta(HttpExchange intercambio) {
        String texto = intercambio.getRequestURI().getRawQuery();
        if (texto == null) return Map.of();
        if (texto.length() > 16384) throw new IllegalArgumentException("La dirección es demasiado larga.");
        return parametros(texto);
    }

    private static Map<String, String> parametros(String texto) {
        Map<String, String> parametros2 = new HashMap<>();
        for (String par : texto.split("&")) {
            if (par.isEmpty()) continue;
            String[] partes = par.split("=", 2);
            String clave, valor;
            try {
                clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                valor = URLDecoder.decode(partes.length == 2 ? partes[1] : "", StandardCharsets.UTF_8);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "No pudimos leer los datos del formulario. Recarga la página e inténtalo de nuevo.");
            }
            if (parametros2.putIfAbsent(clave, valor) != null)
                throw new IllegalArgumentException("El formulario contiene campos repetidos. Recarga la página.");
        }
        return parametros2;
    }

    /**
     * Envía HTML en UTF-8 (RNF-007).
     */
    public static void enviar(HttpExchange intercambio, int estado, String html) throws IOException {
        if (java.util.Set.of(403, 404, 500).contains(estado)) {
            if (!html.contains("<html")) html = Html.error(estado == 403 ? "Acceso denegado" :
                            estado == 404 ? "Página no disponible" : "No pudimos abrir esta página",
                    estado == 403 ? "Tu cuenta no tiene acceso a esta página." :
                            estado == 404 ? "No encontramos lo que buscabas. Vuelve al inicio para continuar." :
                            "Inténtalo de " + "nuevo o " + "vuelve al " + "inicio.");
            else if (!html.contains("Volver al inicio"))
                html = html.replace("</main>", "<div><a class=\"btn p\" href=\"/\">Volver al inicio</a></div></main>");
        }
        responder(intercambio, estado, "text/html; charset=UTF-8", html);
    }

    /**
     * Envía una respuesta con el tipo explícito del contenido generado por la aplicación. RNF-011.
     */
    public static void responder(HttpExchange intercambio, int estado, String tipo,
            String contenido) throws IOException {
        byte[] bytes = contenido.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(estado, bytes.length);
        intercambio.getResponseBody().write(bytes);
    }

    /**
     * Redirige después de un POST para evitar repetir operaciones al recargar (RNF-002).
     */
    public static void redirigir(HttpExchange intercambio, String ruta) throws IOException {
        intercambio.getResponseHeaders().set("Location", ruta);
        intercambio.sendResponseHeaders(303, -1);
    }

    private static void estatico(HttpExchange intercambio, String ruta) throws IOException {
        if (!intercambio.getRequestMethod().equals("GET")) {
            enviar(intercambio, 405, "Método no permitido.");
            return;
        }
        if (ruta.contains("..") ||
                !(ruta.endsWith(".css") || ruta.endsWith(".js") || ruta.endsWith(".jpg") || ruta.endsWith(".png"))) {
            enviar(intercambio, 404, "Archivo no disponible.");
            return;
        }
        try (var entrada = Router.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                enviar(intercambio, 404, "Archivo no disponible.");
                return;
            }
            String tipo = ruta.endsWith(".css") ? "text/css; charset=UTF-8" : ruta.endsWith(
                    ".js") ? "text/javascript; charset=UTF-8" : ruta.endsWith(".jpg") ? "image/jpeg" : "image/png";
            byte[] bytes = entrada.readAllBytes();
            intercambio.getResponseHeaders().set("Content-Type", tipo);
            intercambio.sendResponseHeaders(200, bytes.length);
            intercambio.getResponseBody().write(bytes);
        }
    }
}
