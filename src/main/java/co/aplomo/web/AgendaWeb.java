package co.aplomo.web;

import co.aplomo.servicio.Agenda;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-033, RNF-011: calendario y notas privadas, sin decisiones de negocio en Router.
 */
public final class AgendaWeb {
    private final Agenda servicio;
    private final co.aplomo.servicio.Avisos avisos;

    /**
     * RF-033: recibe las transacciones y el reloj del servicio de reservas.
     */
    public AgendaWeb(Agenda servicio, co.aplomo.servicio.Avisos avisos) {
        this.servicio = servicio;
        this.avisos = avisos;
    }

    /**
     * RF-002/033: conserva los contratos de lectura y acuse de notas.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> parametros) throws SQLException, IOException {
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.matches("/agenda/citas/[1-9][0-9]*(/(leidos|responder))?") ||
                ruta.matches("/agenda/evaluadores/[1-9][0-9]*(/leidos)?")) {
            if (sesion.cuenta().temporal()) throw new SecurityException("Cambia tu contraseña para continuar.");
            co.aplomo.seguridad.Permisos.trabajo(sesion.cuenta());
            int id = Integer.parseInt(ruta.split("/")[3]);
            boolean cita = ruta.startsWith("/agenda/citas/");
            if (ruta.endsWith("/leidos")) {
                if (!post) {
                    Router.enviar(intercambio, 405, "Método no permitido.");
                    return true;
                }
                if (cita) servicio.leerConversacion(sesion.cuenta(), id);
                else avisos.leerEvaluador(sesion.cuenta(), id);
                Router.responder(intercambio, 200, "application/json; charset=UTF-8", "{\"ok\":true}");
                return true;
            }
            if (post) {
                if (cita && ruta.endsWith("/responder"))
                    servicio.responder(sesion.cuenta(), id, parametros.get("texto"));
                else if (!cita) avisos.escribirEvaluador(sesion.cuenta(), id, parametros.get("texto"));
                else {
                    Router.enviar(intercambio, 405, "Método no permitido.");
                    return true;
                }
            } else if (ruta.endsWith("/responder")) {
                Router.enviar(intercambio, 405, "Método no permitido.");
                return true;
            }
            String contenido = cita ? PanelMensajesAgenda.cita(sesion,
                    servicio.conversacion(sesion.cuenta(), id)) : PanelMensajesAgenda.general(sesion,
                    avisos.evaluador(sesion.cuenta(), id), avisos.conversacionEvaluador(sesion.cuenta(), id));
            Router.enviar(intercambio, 200,
                    esPanel(intercambio) ? contenido : Html.pagina("Mensajes de agenda", contenido, sesion));
            return true;
        }
        if (ruta.equals("/notas/nuevas") && intercambio.getRequestMethod().equals("GET")) {
            if (sesion.cuenta().temporal()) throw new SecurityException("Cambia tu contraseña para continuar.");
            new NotasWeb(servicio).nuevas(intercambio, sesion);
            return true;
        }
        if (ruta.equals("/agenda") && intercambio.getRequestMethod().equals("GET")) {
            Router.enviar(intercambio, 200,
                    PaginasAgenda.pagina(sesion, servicio.consultar(sesion.cuenta(), Router.consulta(intercambio))));
            return true;
        }
        if (ruta.matches("/reservas/[1-9][0-9]*/(nota-administrador|entendido)") &&
                intercambio.getRequestMethod().equals("POST")) {
            int rid = Integer.parseInt(ruta.split("/")[2]);
            var agenda = servicio;
            if (ruta.endsWith("/entendido")) {
                agenda.entender(sesion.cuenta(), rid);
                Router.redirigir(intercambio, "/reservas/" + rid);
            } else {
                String texto = esPanel(intercambio) ? co.aplomo.servicio.Validacion.texto(parametros.get("nota"),
                        "mensaje", 500) : parametros.get("nota");
                String nombre = agenda.guardarNota(sesion.cuenta(), rid, texto);
                if (esPanel(intercambio)) {
                    Router.enviar(intercambio, 200,
                            PanelMensajesAgenda.cita(sesion, servicio.conversacion(sesion.cuenta(), rid)));
                    return true;
                }
                String aviso = java.net.URLEncoder.encode("Nota enviada a " + nombre,
                        java.nio.charset.StandardCharsets.UTF_8);
                Router.redirigir(intercambio, "/reservas/" + rid + "?aviso=" + aviso);
            }
            return true;
        }
        return false;
    }

    private static boolean esPanel(HttpExchange intercambio) {
        return "1".equals(intercambio.getRequestHeaders().getFirst("X-Alineo-Agenda"));
    }
}
