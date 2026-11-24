package co.aplomo.web;

import co.aplomo.modelo.Trabajador;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Validacion;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/** RF-020/023/027: complemento JSON de la previsualización; cálculo protegido sin cambios. */
final class Resultado40Web {
    private Resultado40Web() { }

    /** RF-002/004: CSRF, alcance, entradas validadas y errores en español; no acepta puntajes del navegador. */
    static void atender(HttpExchange x, Sesion s, Evaluaciones servicio) throws IOException, SQLException {
        var f = Router.formulario(x);
        if (!Sesiones.csrfValido(s, f.get("csrf"))) {
            Router.responder(x, 403, "text/plain; charset=UTF-8", "El formulario venció. Vuelve a intentarlo.");
            return;
        }
        try {
            Map<String, Boolean> condiciones = Map.of("escritorio_muy_alto", si(f, "escritorio"),
                "pantalla_muy_alta", si(f, "pantalla"), "pies_colgando", si(f, "pies"),
                "tiene_apoyapies", si(f, "apoyapies"), "silla_con_apoyabrazos", si(f, "apoyabrazos"),
                "silla_regulable", si(f, "regulable"));
            var vista = servicio.resultados().previsualizar(s.cuenta(), numero(f, "visita"), numero(f, "puesto"),
                    DatosAnalisis.leer(f), condiciones, Trabajador.leer(f),
                    Validacion.entero(f.getOrDefault("minutos", "0"), "minutos por jornada", 0, 720));
            agregarDesglose(vista.datos(), vista.calculo());
            Router.responder(x, 200, "application/json; charset=UTF-8", Json40.escribir(vista.datos()));
        } catch (IllegalArgumentException error) {
            Router.responder(x, 400, "text/plain; charset=UTF-8", error.getMessage());
        } catch (SQLException error) {
            Router.responder(x, 503, "text/plain; charset=UTF-8", "No pudimos preparar el plan. Inténtalo de nuevo.");
        }
    }

    /** RF-021: reutiliza la tabla de desglose actual, cerrada al presentarla. */
    static void agregarDesglose(Map<String, Object> datos, co.aplomo.rula.MotorRula.Calculo calculo) {
        String html = PaginasAnalisis.resultado(calculo);
        datos.put("desglose", html.substring(html.indexOf("<table"), html.indexOf("</table>") + 8));
    }

    /** RF-020/023: ficha guardada con el mismo resultado explicado y detalles existentes plegados. */
    static String pagina(Sesion sesion, Evaluaciones servicio, int id, String anterior) throws SQLException {
        return pagina(sesion, servicio.resultados(), id, anterior);
    }

    /** RF-002/020: comparte presentación sin ampliar el acceso ni entregar contexto privado al Usuario. */
    static String pagina(Sesion sesion, co.aplomo.servicio.Resultados40 servicio, int id,
            String anterior) throws SQLException {
        var vistas = servicio.guardados(sesion.cuenta(), id);
        if (vistas.isEmpty()) return anterior;
        var posturas = vistas.stream().map(v -> {
            agregarDesglose(v.datos(), v.calculo());
            return Map.of("titulo", v.datos().get("titulo"), "analisis", List.of(v.datos()));
        }).toList();
        int inicio = anterior.indexOf('>', anterior.indexOf("<main")) + 1;
        int fin = anterior.indexOf("</main>", inicio);
        String detalle = anterior.substring(inicio, fin).replace("<h1>", "<h2>").replace("</h1>", "</h2>");
        String contenido = Html.encabezado("Evaluaciones · Resultado", "Resultado del puesto",
                "<a class=\"btn\" href=\"/evaluaciones/" + id + "\">Volver a la evaluación</a>") +
                "<div data-resultado40></div><script type=\"application/json\" data-resultado40-json data-csrf=\"" +
                Html.escapar(sesion.csrf()) + "\">" + Json40.escribir(posturas) + "</script>" +
                "<details class=\"guia\"><summary>Datos del puesto, fotos y análisis</summary>" + detalle +
                "</details><script src=\"/static/js/resultado40.js\" defer></script>" +
                "<script src=\"/static/js/sugerencias.js\" defer></script>";
        return Html.pagina("Resultado de la evaluación", contenido, sesion);
    }

    /** RF-014: solo reconoce las casillas ya autorizadas. */
    private static boolean si(Map<String, String> f, String clave) { return "si".equals(f.get(clave)); }
    /** RF-002: los identificadores recibidos siempre se validan y se comprueban en el servicio. */
    private static int numero(Map<String, String> f, String clave) {
        return Validacion.entero(f.get(clave), clave, 1, Integer.MAX_VALUE);
    }
}
