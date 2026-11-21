package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Calificaciones;
import co.aplomo.servicio.Validacion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;

/**
 * RF-025/026: rutas autenticadas de consulta y de informe imprimible.
 */
public final class CalificacionesWeb {
    private final Calificaciones servicio;

    /**
     * Recibe el servicio con permisos verificados también en el servidor. RNF-011.
     */
    public CalificacionesWeb(Calificaciones servicio) {
        this.servicio = servicio;
    }

    /**
     * Atiende solo calificaciones o informes; imprimir/guardar PDF lo realiza el navegador. RNF-011.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        if (!ruta.equals("/calificaciones") && !ruta.startsWith("/informes/")) return false;
        if (!intercambio.getRequestMethod().equals("GET")) {
            intercambio.getResponseHeaders().set("Allow", "GET");
            Router.enviar(intercambio, 405, "Usa la vista de calificaciones o el botón Generar informe.");
            return true;
        }
        if (ruta.equals("/calificaciones")) {
            String id = Router.consulta(intercambio).getOrDefault("empresa", "");
            if (id.isBlank()) {
                var empresas = servicio.empresas(sesion.cuenta());
                if (sesion.cuenta().rol() == Cuenta.Rol.USUARIO && empresas.size() == 1)
                    id = Integer.toString(empresas.get(0).id());
                else {
                    Router.enviar(intercambio, 200, PaginasCalificaciones.elegir(sesion, empresas));
                    return true;
                }
            }
            Router.enviar(intercambio, 200, PaginasCalificaciones.calificaciones(sesion,
                    servicio.consultar(sesion.cuenta(), Validacion.entero(id, "empresa", 1, Integer.MAX_VALUE))));
        } else if (ruta.matches("/informes/empresa/[1-9][0-9]*")) {
            int id = Validacion.entero(ruta.substring("/informes/empresa/".length()), "empresa", 1, Integer.MAX_VALUE);
            String visita = Router.consulta(intercambio).getOrDefault("visita", "");
            var contexto = visita.isBlank() ? null : servicio.visitaInforme(sesion.cuenta(), id,
                    Validacion.entero(visita, "visita", 1, Integer.MAX_VALUE));
            Router.enviar(intercambio, 200,
                    PaginasCalificaciones.informe(sesion, servicio.informe(sesion.cuenta(), id), contexto));
        } else Router.enviar(intercambio, 404,
                Html.pagina("Informe no encontrado", Html.aviso("Abre una empresa y pulsa Generar informe.", false),
                        sesion));
        return true;
    }
}
