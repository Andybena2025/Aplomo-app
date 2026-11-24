package co.aplomo.web;

import co.aplomo.servicio.Consultas;
import co.aplomo.servicio.Sugerencias;
import co.aplomo.servicio.Validacion;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.SQLException;

/**
 * RF-024: solo rutas GET; las acciones existentes conservan sus permisos y CSRF.
 */
public final class ConsultasWeb {
    private final Consultas consultas;
    private final Sugerencias sugerencias;
    private final co.aplomo.servicio.Resultados40 resultados;

    /**
     * Comparte servicios autorizados y transacciones con Evaluaciones. RNF-011.
     */
    public ConsultasWeb(Consultas consultas, Sugerencias sugerencias) {
        this(consultas, sugerencias, null);
    }

    /** RF-002/020: integra el resultado explicado en la misma consulta segura del Usuario. */
    public ConsultasWeb(Consultas consultas, Sugerencias sugerencias,
            co.aplomo.servicio.Resultados40 resultados) {
        this.consultas = consultas; this.sugerencias = sugerencias; this.resultados = resultados;
    }

    /**
     * Devuelve true únicamente cuando atendió una lista o el detalle seguro del Usuario. RNF-011.
     */
    public boolean atender(HttpExchange intercambio, Sesion sesion) throws SQLException, IOException {
        if (!intercambio.getRequestMethod().equals("GET")) return false;
        String ruta = intercambio.getRequestURI().getPath();
        if (ruta.equals("/mis-evaluaciones")) {
            var elemento = consultas.propias(sesion.cuenta(), Router.consulta(intercambio));
            String texto = PaginasConsultas.lista(sesion, elemento, null)
                    .replace("href=\"/evaluaciones?", "href=\"/mis-evaluaciones?")
                    .replace("action=\"/evaluaciones\"", "action=\"/mis-evaluaciones\"")
                    .replace("<h1>Evaluaciones</h1>", "<h1>Mis evaluaciones</h1>")
                    .replace("<title>Evaluaciones ·", "<title>Mis evaluaciones ·");
            Router.enviar(intercambio, 200, texto);
            return true;
        }
        var seccion = SeccionConsulta.deRuta(ruta);
        boolean detalle = sesion.cuenta().rol() == Cuenta.Rol.USUARIO && ruta.matches("/evaluaciones/[1-9][0-9]*");
        if (seccion == null && !detalle) return false;
        if (sesion.cuenta().temporal()) {
            Router.redirigir(intercambio, "/cuenta/contrasena");
            return true;
        }
        if (detalle) {
            int id = Validacion.entero(ruta.substring(14), "evaluación", 1, Integer.MAX_VALUE);
            String pagina = PaginasConsultas.evaluacion(sesion, consultas.evaluacion(sesion.cuenta(), id));
            if (resultados != null) pagina = Resultado40Web.pagina(sesion, resultados, id, pagina);
            Router.enviar(intercambio, 200, pagina);
            return true;
        }
        var parametros = Router.consulta(intercambio);
        var elemento2 = consultas.listar(sesion.cuenta(), seccion, parametros);
        var contexto = seccion == SeccionConsulta.SUGERENCIAS &&
                elemento2.filtros().containsKey("evaluacion") ? sugerencias.contexto(sesion.cuenta(),
                Integer.parseInt(elemento2.filtros().get("evaluacion"))) : null;
        Router.enviar(intercambio, 200, PaginasConsultas.lista(sesion, elemento2, contexto));
        return true;
    }
}
