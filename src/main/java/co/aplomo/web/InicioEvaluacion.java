package co.aplomo.web;

import co.aplomo.dao.VisitaDao.ParaEvaluar;

import java.util.List;

/**
 * Atajo del Administrador: el servicio filtra visitas en curso y puestos pendientes.
 */
final class InicioEvaluacion {
    private InicioEvaluacion() {
    }

    static String practica(Integer visita) {
        if (!GaleriaDemo.habilitada()) return "";
        return "<div class=\"practica-fila row\"><a class=\"practicar-enlace\" href=\"/demo/evaluacion\">" +
                "▶ Practicar una evaluación <span>· No cuenta en tus resultados</span> ›</a>" +
                (visita == null ? "" : "<a class=\"btn\" href=\"/visitas/" + visita + "\">Continuar práctica</a>") +
                "</div>";
    }

    static String paginaPractica(co.aplomo.seguridad.Sesiones.Sesion sesion) {
        return Html.pagina("Probar evaluación",
                "<div class=\"pagehead\"><div><span class=\"eyebrow\">Demostración · Evaluaci" +
                        "ón</span><h1>Probar evaluación</h1></div></div><section class=\"card stack\"" +
                        "><p>Elige un puesto para practicar el mismo recorrido de la app.</p><p>Se pr" +
                        "epara una visita En curso en ALINEO · Práctica de evaluación. Los resultados" +
                        " son de práctica y no cuentan en tus resultados. Puedes repetir la prueba si" +
                        "n borrar intentos.</p><form method=\"post\" action=\"/demo/evaluacion\" clas" + "s=\"row\">" +
                        Html.csrf(sesion) +
                        "<button class=\"btn p\" name=\"tipo\" value=\"PLANTA\">Probar Planta</button" +
                        "><button class=\"btn\" name=\"tipo\" value=\"OFICINA\">Probar Oficina</butto" +
                        "n></form></section>", sesion, false);
    }

    static String tarjeta(List<ParaEvaluar> filas) {
        StringBuilder contenido = new StringBuilder(
                "<section class=\"card stack evaluar-ahora\" aria-labelledby=\"visitas-en-cur" +
                        "so\"><h2 id=\"visitas-en-curso\">Visitas en curso</h2>");
        if (filas.isEmpty()) return contenido.append(
                "<p>No hay una visita en curso</p><div><a class=\"btn p\" href=\"/reservas\">" +
                        "Ir a Reservas</a></div></section>").toString();
        int anterior = -1;
        for (var visita2 : filas) {
            if (visita2.visita() != anterior) {
                if (anterior != -1) contenido.append("</ul></div>");
                contenido.append("<div class=\"stack\"><h3>").append(Html.escapar(visita2.empresa()))
                        .append("</h3><p class=\"muted\">").append(Html.escapar(visita2.sede()))
                        .append("</p><a class=\"link\" href=\"/visitas/").append(visita2.visita())
                        .append("\">Ver visita</a><ul class=\"evaluar-puestos\">");
                anterior = visita2.visita();
            }
            if (visita2.puesto() != null)
                contenido.append("<li><div><strong>").append(Html.escapar(visita2.descripcion()))
                        .append("</strong><div class=\"muted small\">Puesto ").append(visita2.numero())
                        .append("</div></div><a class=\"btn p\" href=\"/evaluaciones/nueva?visita=")
                        .append(visita2.visita()).append("&amp;puesto=").append(visita2.puesto())
                        .append("\">Evaluar</a></li>");
        }
        return contenido.append("</ul></div></section>").toString();
    }
}
