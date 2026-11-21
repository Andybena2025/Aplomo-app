package co.aplomo.web;

import co.aplomo.servicio.Visitas;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-002, RNF-011: entrada a la práctica, separada del Router.
 */
public final class DemoWeb {
    private final Visitas visitas;

    /**
     * RNF-011: comparte el servicio de visitas ya autorizado.
     */
    public DemoWeb(Visitas visitas) {
        this.visitas = visitas;
    }

    /**
     * RF-002: conserva el permiso, el modo demostración y el flujo de práctica existente.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> parametros) throws SQLException, IOException {
        Permisos.evaluador(sesion.cuenta());
        if (!GaleriaDemo.habilitada()) {
            Router.enviar(intercambio, 404, Html.pagina("Demostración no disponible", "", sesion));
            return;
        }
        if (intercambio.getRequestMethod().equals("GET")) {
            Router.enviar(intercambio, 200, InicioEvaluacion.paginaPractica(sesion));
            return;
        }
        var destino = visitas.practica().abrir(sesion.cuenta(), parametros.get("tipo"));
        Router.redirigir(intercambio, "/evaluaciones/nueva?visita=" + destino.visita() + "&puesto=" + destino.puesto());
        return;
    }
}
