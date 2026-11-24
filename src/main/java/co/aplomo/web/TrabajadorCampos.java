package co.aplomo.web;

/** RF-014/RNF-007: preguntas opcionales; no usan los selectores del recorrido RULA. */
final class TrabajadorCampos {
    private TrabajadorCampos() { }

    /** RF-014: tarjetas desmarcables sin respuesta inicial. */
    static String mostrar() {
        return "<section class=\"trabajador-campos stack\"><h3>Sobre el trabajador (opcional)</h3>" +
                "<p class=\"hint\">Pregúntale. Solo da contexto al informe; no cambia el puntaje RULA.</p>" +
                opciones("trabajador_genero", "Género", new String[][]{{"FEMENINO", "Femenino"},
                    {"MASCULINO", "Masculino"}, {"OTRO", "Otro"}, {"NO_DICE", "Prefiere no decir"}}) +
                "<label class=\"field\">Edad<div class=\"row\"><input class=\"input\" type=\"number\" " +
                "name=\"trabajador_edad\" min=\"14\" max=\"99\" step=\"1\" inputmode=\"numeric\" " +
                "aria-label=\"Edad en años (opcional)\"><span>años</span></div></label>" +
                opciones("trabajador_antiguedad", "¿Hace cuánto trabaja en la empresa?",
                    new String[][]{{"MENOS_1", "Menos de 1 año"}, {"DE_1_A_5", "1 a 5 años"},
                        {"DE_6_A_10", "6 a 10 años"}, {"MAS_10", "Más de 10 años"}}) + "</section>";
    }

    /** RF-002/014: contexto también en fichas sin puntaje; nunca visible para Usuario. */
    static String resumen(co.aplomo.seguridad.Sesiones.Sesion sesion, co.aplomo.servicio.Evaluaciones.Datos datos) {
        if (!co.aplomo.modelo.Trabajador.ROLES_VISIBLES.contains(sesion.cuenta().rol())) return "";
        var t = datos.trabajador().para(sesion.cuenta());
        var generos = java.util.Map.of("FEMENINO", "Femenino", "MASCULINO", "Masculino", "OTRO", "Otro",
                "NO_DICE", "Prefiere no decir");
        var antiguedades = java.util.Map.of("MENOS_1", "Menos de 1 año", "DE_1_A_5", "1 a 5 años",
                "DE_6_A_10", "6 a 10 años", "MAS_10", "Más de 10 años");
        return "<section class=\"r40-trabajador\"><strong>Trabajador</strong><div>" +
                (t.genero() == null ? "" : "<span class=\"tag\">Género: " + generos.get(t.genero()) + "</span>") +
                (t.edad() == null ? "" : "<span class=\"tag\">Edad: " + t.edad() + " años</span>") +
                (t.antiguedad() == null ? "" : "<span class=\"tag\">En la empresa: " +
                    antiguedades.get(t.antiguedad()) + "</span>") +
                "<span class=\"tag\">Jornada: " + datos.minutos() + " minutos</span></div>" +
                "<small>Solo contexto · no cambia el puntaje RULA</small></section>";
    }

    /** RF-014: valores enviados mediante los tres campos permitidos. */
    private static String opciones(String nombre, String titulo, String[][] opciones) {
        StringBuilder html = new StringBuilder("<div class=\"trabajador-opciones\" role=\"group\" aria-label=\"")
                .append(titulo).append("\"><p>").append(titulo).append("</p><input type=\"hidden\" name=\"")
                .append(nombre).append("\" value=\"\"><div>");
        for (var opcion : opciones) html.append("<button type=\"button\" data-contexto-campo=\"")
                .append(nombre).append("\" data-contexto-valor=\"").append(opcion[0])
                .append("\" aria-pressed=\"false\">").append(opcion[1]).append("</button>");
        return html.append("</div></div>").toString();
    }
}
