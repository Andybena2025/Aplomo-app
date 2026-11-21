package co.aplomo.web;

import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.FotosEvaluacion;
import co.aplomo.servicio.Validacion;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Administración de fotos activas; nunca borra ni altera el cálculo RULA.
 */
final class FotosEvaluacionWeb {
    private FotosEvaluacionWeb() {
    }

    static void cambiar(HttpExchange intercambio, Sesion sesion, FotosEvaluacion servicio,
            int evaluacion) throws IOException, SQLException {
        Permisos.trabajo(sesion.cuenta());
        var partes = Multipart.leer(intercambio.getRequestBody(),
                intercambio.getRequestHeaders().getFirst("Content-Type"));
        Map<String, String> parametros = new HashMap<>();
        var archivos = new ArrayList<Multipart.Parte>();
        for (var elemento : partes)
            if (elemento.archivo() == null)
                parametros.put(elemento.nombre(), new String(elemento.bytes(), StandardCharsets.UTF_8));
            else if (!elemento.archivo().isEmpty() || elemento.bytes().length > 0) archivos.add(elemento);
        if (!Sesiones.csrfValido(sesion, parametros.get("csrf"))) {
            Router.responder(intercambio, 403, "text/plain; charset=UTF-8",
                    "El formulario venció. Abre otra pestaña, inicia sesión y vuelve a intentarlo.");
            return;
        }
        if (archivos.size() > 1) throw new IllegalArgumentException("Elige una foto a la vez.");
        Integer anterior = parametros.containsKey("anterior") ? Validacion.entero(parametros.get("anterior"),
                "foto anterior", 1, Integer.MAX_VALUE) : null;
        Fotos.Entrada entrada = archivos.isEmpty() ? null : new Fotos.Entrada(archivos.get(0).bytes(),
                parametros.get("vista"), parametros.get("descripcion"));
        try {
            var cambio = servicio.cambiar(sesion.cuenta(), evaluacion, anterior, entrada);
            String json = "{\"foto\":" + cambio.foto() + ",\"anterior\":" + cambio.anterior() + "}";
            Router.responder(intercambio, 200, "application/json; charset=UTF-8", json);
        } catch (IllegalArgumentException error) {
            Router.responder(intercambio, 400, "text/plain; charset=UTF-8", error.getMessage());
        } catch (SQLException | IOException error2) {
            Router.responder(intercambio, 503, "text/plain; charset=UTF-8",
                    "No pudimos cambiar la foto. La anterior se conserva; intenta de nuevo.");
        }
    }
}
