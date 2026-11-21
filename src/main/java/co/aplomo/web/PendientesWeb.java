package co.aplomo.web;

import co.aplomo.servicio.Pendientes;
import co.aplomo.servicio.Validacion;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-022: consulta por alcance y cambios solo para Administrador; Router verifica CSRF antes de entrar.
 */
public final class PendientesWeb {
    private final Pendientes servicio;

    /**
     * Recibe el mismo servicio de seguimiento que utiliza el guardado del análisis. RNF-011.
     */
    public PendientesWeb(Pendientes servicio) {
        this.servicio = servicio;
    }

    /**
     * Presenta lista/detalle y conserva campos en los errores de guardado. RNF-011.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> datos) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/pendientes") && !post) {
            var parametros = Router.consulta(intercambio);
            String estado = parametros.getOrDefault("estado", "ABIERTO"), orden = parametros.getOrDefault("orden",
                    "prioridad");
            Router.enviar(intercambio, 200,
                    PaginasPendientes.lista(sesion, servicio.listar(sesion.cuenta(), estado, orden), estado, orden,
                            parametros.get("aviso")));
            return;
        }
        if (!ruta.matches("/pendientes/[1-9][0-9]*(/(editar|arreglado))?")) {
            Router.enviar(intercambio, 404, "Página no disponible.");
            return;
        }
        String[] partes = ruta.split("/");
        int id = Validacion.entero(partes[2], "pendiente", 1, Integer.MAX_VALUE);
        String accion = partes.length > 3 ? partes[3] : "";
        if (post || !accion.isEmpty()) Permisos.trabajo(sesion.cuenta());
        var elemento = servicio.consultar(sesion.cuenta(), id);
        if (!post && accion.isEmpty()) {
            Router.enviar(intercambio, 200,
                    PaginasPendientes.detalle(sesion, elemento, servicio.historial(sesion.cuenta(), id),
                            Router.consulta(intercambio).get("aviso"), true));
            return;
        }
        if (!post && accion.equals("editar")) {
            Router.enviar(intercambio, 200, PaginasPendientes.editar(sesion, elemento, Map.of(), null));
            return;
        }
        if (post && accion.equals("arreglado")) {
            try {
                boolean cambio = servicio.arreglar(sesion.cuenta(), id);
                redirigir(intercambio, id,
                        cambio ? "Pendiente marcado como Arreglado." : "El pendiente ya estaba Arreglado.");
            } catch (SQLException error) {
                Router.enviar(intercambio, 503,
                        PaginasPendientes.detalle(sesion, elemento, servicio.historial(sesion.cuenta(), id),
                                "No pudimos guardar el cambio. Inténtalo de nuevo.", false));
            }
            return;
        }
        if (post && accion.isEmpty()) {
            try {
                servicio.guardar(sesion.cuenta(), id, datos.get("descripcion"), datos.get("prioridad"),
                        datos.get("fecha"));
                redirigir(intercambio, id, "Seguimiento guardado.");
            } catch (IllegalArgumentException error2) {
                Router.enviar(intercambio, 400, PaginasPendientes.editar(sesion, elemento, datos, error2.getMessage()));
            } catch (SQLException error3) {
                Router.enviar(intercambio, 503, PaginasPendientes.editar(sesion, elemento, datos,
                        "No pudimos guardar. Tus campos siguen escritos; inténtalo de nuevo."));
            }
            return;
        }
        intercambio.getResponseHeaders().set("Allow", accion.equals("arreglado") ? "POST" : "GET");
        Router.enviar(intercambio, 405, "Usa los botones de la página para realizar esta acción.");
    }

    private static void redirigir(HttpExchange intercambio, int id, String aviso) throws IOException {
        String ruta = "/pendientes/" + id + "?aviso=" + URLEncoder.encode(aviso, StandardCharsets.UTF_8);
        if ("text/plain".equals(intercambio.getRequestHeaders().getFirst("Accept"))) {
            intercambio.getResponseHeaders().set("Location", ruta);
            Router.responder(intercambio, 201, "text/plain; charset=UTF-8", aviso);
        } else Router.redirigir(intercambio, ruta);
    }
}
