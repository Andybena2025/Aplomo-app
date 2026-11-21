package co.aplomo.web;

import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Agenda;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 * RF-002/033: aviso privado de notas, sin datos de cuentas o de otras empresas.
 */
public final class NotasWeb {
    private final Agenda agenda;

    /**
     * RNF-011: reutiliza el alcance y las transacciones del servicio de agenda.
     */
    public NotasWeb(Agenda agenda) {
        this.agenda = agenda;
    }

    /**
     * RF-033: entrega únicamente los cinco datos necesarios para el aviso.
     */
    public void nuevas(HttpExchange intercambio, Sesion sesion) throws SQLException, IOException {
        if (sesion.cuenta().rol() != Rol.EVALUADOR) {
            throw new SecurityException("Acceso denegado.");
        }
        String contenido = agenda.avisos(sesion.cuenta()).stream().map(NotasWeb::json)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        byte[] datos = contenido.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        intercambio.sendResponseHeaders(200, datos.length);
        intercambio.getResponseBody().write(datos);
    }

    private static String json(Agenda.Aviso aviso) {
        return "{\"reserva\":" + aviso.reserva() + ",\"fecha\":" + texto(aviso.fecha()) + ",\"hora\":" +
                texto(aviso.hora()) + ",\"empresa\":" + texto(aviso.empresa()) + ",\"texto\":" + texto(aviso.texto()) +
                "}";
    }

    static String texto(String valor) {
        StringBuilder texto = new StringBuilder("\"");
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '"' -> texto.append("\\\"");
                case '\\' -> texto.append("\\\\");
                case '\n' -> texto.append("\\n");
                case '\r' -> texto.append("\\r");
                case '\t' -> texto.append("\\t");
                default -> texto.append(caracter < 32 ? " " : caracter);
            }
        }
        return texto.append('"').toString();
    }
}
