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

    /**
     * RNF-011: recibe las transacciones y el reloj del servicio de reservas.
     */
    public AgendaWeb(Agenda servicio) {
        this.servicio = servicio;
    }

    /**
     * RF-002/033: conserva los contratos de lectura y acuse de notas.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> parametros) throws SQLException, IOException {
        String ruta = intercambio.getRequestURI().getPath();
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
            if (ruta.endsWith("/entendido")) agenda.entender(sesion.cuenta(), rid);
            else agenda.guardarNota(sesion.cuenta(), rid, parametros.get("nota"));
            Router.redirigir(intercambio, "/reservas/" + rid);
            return true;
        }
        return false;
    }
}
