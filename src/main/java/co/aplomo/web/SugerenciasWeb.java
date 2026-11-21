package co.aplomo.web;

import co.aplomo.servicio.Sugerencias;
import co.aplomo.servicio.Validacion;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RF-023a/b: consulta y gestión protegidas por sesión, alcance y CSRF del Router.
 */
public final class SugerenciasWeb {
    private final Sugerencias servicio;

    /**
     * Recibe el servicio compartido con el guardado del análisis. RNF-011.
     */
    public SugerenciasWeb(Sugerencias servicio) {
        this.servicio = servicio;
    }

    /**
     * Resuelve formularios, estados y consultas sin aceptar origen, regla o empresa desde campos ocultos. RNF-011.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> parametros) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        var parametros2 = Router.consulta(intercambio);
        if (ruta.equals("/sugerencias") && !post) {
            int cantidad = parametros2.containsKey("evaluacion") ? id(parametros2.get("evaluacion")) : 0;
            Router.enviar(intercambio, 200, PaginasSugerencias.lista(sesion, servicio.listar(sesion.cuenta(), cantidad),
                    cantidad == 0 ? null : servicio.contexto(sesion.cuenta(), cantidad)));
            return;
        }
        if (post || ruta.endsWith("/editar") || ruta.equals("/sugerencias/nueva")) Permisos.trabajo(sesion.cuenta());
        if (ruta.equals("/sugerencias/nueva") || ruta.equals("/sugerencias")) {
            int cantidad2 = id(parametros2.get("evaluacion"));
            var sugerencia = servicio.contexto(sesion.cuenta(), cantidad2);
            if (!post) {
                Router.enviar(intercambio, 200,
                        PaginasSugerencias.formulario(sesion, sugerencia, null, Map.of(), null));
                return;
            }
            guardar(intercambio, sesion, cantidad2, 0, sugerencia, null, parametros);
            return;
        }
        if (!ruta.matches("/sugerencias/[1-9][0-9]*(/(editar|quitar|estado))?")) {
            Router.enviar(intercambio, 404, "Página no disponible.");
            return;
        }
        var partes = ruta.split("/");
        int id = id(partes[2]);
        String accion = partes.length > 3 ? partes[3] : "";
        var sugerencia2 = servicio.consultar(sesion.cuenta(), id);
        if (!post && accion.isEmpty()) {
            detalle(intercambio, sesion, id, parametros2.get("aviso"), true, 200);
            return;
        }
        if (!post && accion.equals("editar")) {
            if (!sugerencia2.sugerencia().activo())
                throw new IllegalArgumentException("La sugerencia fue quitada y conserva su historial.");
            Router.enviar(intercambio, 200,
                    PaginasSugerencias.formulario(sesion, sugerencia2.contexto(), sugerencia2.sugerencia(), Map.of(),
                            null));
            return;
        }
        if (post && accion.isEmpty()) {
            guardar(intercambio, sesion, sugerencia2.contexto().evaluacion(), id, sugerencia2.contexto(),
                    sugerencia2.sugerencia(), parametros);
            return;
        }
        if (post && (accion.equals("quitar") || accion.equals("estado"))) {
            try {
                if (accion.equals("quitar")) servicio.quitar(sesion.cuenta(), id);
                else servicio.estado(sesion.cuenta(), id, parametros.get("estado"));
                destino(intercambio, id,
                        accion.equals("quitar") ? "Sugerencia quitada. Su historial se conserva." : "Estado guardado.");
            } catch (IllegalArgumentException error) {
                detalle(intercambio, sesion, id, error.getMessage(), false, 400);
            } catch (SQLException error2) {
                detalle(intercambio, sesion, id, "No pudimos guardar el cambio. Inténtalo de nuevo.", false, 503);
            }
            return;
        }
        intercambio.getResponseHeaders()
                .set("Allow", accion.equals("quitar") || accion.equals("estado") ? "POST" : "GET");
        Router.enviar(intercambio, 405, "Usa los botones de la página para realizar esta acción.");
    }

    private void guardar(HttpExchange intercambio, Sesion sesion, int evaluacion, int id,
            co.aplomo.dao.SugerenciaDao.Contexto sugerencia, co.aplomo.modelo.Sugerencia sugerencia2,
            Map<String, String> parametros) throws IOException, SQLException {
        try {
            int nuevo = servicio.guardar(sesion.cuenta(), evaluacion, id, parametros.get("titulo"),
                    parametros.get("descripcion"), parametros.get("tipo"), parametros.get("prioridad"),
                    parametros.get("fecha"));
            destino(intercambio, nuevo, "Sugerencia guardada.");
        } catch (IllegalArgumentException error) {
            Router.enviar(intercambio, 400,
                    PaginasSugerencias.formulario(sesion, sugerencia, sugerencia2, parametros, error.getMessage()));
        } catch (SQLException error2) {
            Router.enviar(intercambio, 503, PaginasSugerencias.formulario(sesion, sugerencia, sugerencia2, parametros,
                    "No pudimos guardar. Tus campos siguen escritos; inténtalo de nuevo."));
        }
    }

    private void detalle(HttpExchange intercambio, Sesion sesion, int id, String aviso, boolean exito,
            int codigo) throws IOException, SQLException {
        var sugerencia = servicio.consultar(sesion.cuenta(), id);
        Router.enviar(intercambio, codigo,
                PaginasSugerencias.detalle(sesion, sugerencia, servicio.historial(sesion.cuenta(), id),
                        servicio.todasHechas(sesion.cuenta(), sugerencia.contexto().evaluacion()), aviso, exito));
    }

    private static int id(String texto) {
        return Validacion.entero(texto, "evaluación o sugerencia", 1, Integer.MAX_VALUE);
    }

    private static void destino(HttpExchange intercambio, int id, String aviso) throws IOException {
        String ruta = "/sugerencias/" + id + "?aviso=" + URLEncoder.encode(aviso, StandardCharsets.UTF_8);
        if ("text/plain".equals(intercambio.getRequestHeaders().getFirst("Accept"))) {
            intercambio.getResponseHeaders().set("Location", ruta);
            Router.responder(intercambio, 201, "text/plain; charset=UTF-8", aviso);
        } else Router.redirigir(intercambio, ruta);
    }
}
