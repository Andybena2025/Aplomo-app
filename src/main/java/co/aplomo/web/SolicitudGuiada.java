package co.aplomo.web;

import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.SolicitudesVisita;
import co.aplomo.servicio.SolicitudesVisita.Lugar;
import co.aplomo.servicio.ColoresAgenda;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * RF-011/002: tres etapas de presentación; la validación y el envío originales se conservan.
 */
final class SolicitudGuiada {
    private SolicitudGuiada() {
    }

    static String formulario(Sesion sesion, SolicitudesVisita servicio, List<Lugar> lugares,
                             Map<String, String> datos, String error, boolean cruce) throws SQLException {
        Map<Integer, String> sedes = lugares.stream().collect(Collectors.toMap(Lugar::sede,
                l -> l.empresaNombre() + " · " + l.sedeNombre(), (a, b) -> a, LinkedHashMap::new));
        String sede = datos.getOrDefault("sede", "" + lugares.get(0).sede());
        if (!sedes.keySet().stream().anyMatch(id -> ("" + id).equals(sede)))
            throw new SecurityException("Acceso denegado.");
        var salones = lugares.stream().filter(l -> ("" + l.sede()).equals(sede)).toList();
        var personas = servicio.ingenieros(sesion.cuenta(), salones.get(0).empresa());
        StringBuilder html = new StringBuilder(Html.aviso(error, false));
        html.append("<form class=\"card stack solicitud-guiada\" method=\"post\" action=\"/solicitar-visita\" ")
                .append("data-solicitar-visita data-hoy=\"").append(servicio.hoy()).append("\" data-paso=\"")
                .append(cruce ? 2 : error.isEmpty() ? 1 : 3).append("\">").append(Html.csrf(sesion))
                .append("<ol class=\"solicitud-indicador\" aria-label=\"Pasos de solicitud\">")
                .append("<li data-indicador=\"1\">1 Dónde</li><li data-indicador=\"2\">2 Cuándo y con quién</li>")
                .append("<li data-indicador=\"3\">3 Revisar y enviar</li></ol>")
                .append("<p class=\"hint\" role=\"status\" data-solicitud-error></p>")
                .append("<section data-etapa=\"1\" class=\"stack\"><h2>Dónde</h2>");
        if (sedes.size() == 1) html.append("<input type=\"hidden\" name=\"sede\" value=\"").append(sede)
                .append("\"><p data-sede-nombre>").append(Html.escapar(sedes.get(Integer.parseInt(sede)))).append(
                        "</p>");
        else {
            html.append("<label class=\"field\">Sede<select class=\"input\" name=\"sede\">");
            for (var entrada : sedes.entrySet())
                html.append("<option value=\"").append(entrada.getKey()).append("\"")
                        .append(sede.equals("" + entrada.getKey()) ? " selected" : "").append(">")
                        .append(Html.escapar(entrada.getValue())).append("</option>");
            html.append("</select></label><noscript><p>Para cambiar de sede:</p>");
            for (var entrada : sedes.entrySet())
                html.append("<a class=\"btn\" href=\"/solicitar-visita?sede=")
                        .append(entrada.getKey()).append("\">").append(Html.escapar(entrada.getValue())).append("</a>");
            html.append("</noscript>");
        }
        html.append("<fieldset><legend>Salón</legend><div class=\"chips solicitud-opciones\">");
        for (Lugar salon : salones)
            html.append(opcion("salon", "" + salon.id(), salon.nombre(),
                    datos.get("salon"), "", ""));
        html.append("</div></fieldset></section><section data-etapa=\"2\" class=\"stack\"><h2>Cuándo y con quién</h2>")
                .append("<fieldset><legend>Evaluador</legend><div class=\"solicitud-personas\">");
        for (var persona : personas) {
            String color = persona.color() != null && ColoresAgenda.PALETA.contains(persona.color()) ?
                    persona.color() : "#C9CCD2";
            html.append(opcion("ingeniero", "" + persona.id(), Html.nombreSinTitulo(persona.nombre()),
                    datos.get("ingeniero"), persona.asignado() ? "Tu evaluador asignado" : "Otro evaluador", color));
        }
        html.append("</div></fieldset><div class=\"solicitud-calendario\" data-calendario hidden>")
                .append("<div class=\"row between\"><button type=\"button\" class=\"btn\" data-mes=\"-1\" ")
                .append("aria-label=\"Mes anterior\">←</button><strong data-mes-titulo></strong>")
                .append("<button type=\"button\" class=\"btn\" data-mes=\"1\" aria-label=\"Mes " +
                        "siguiente\">→</button></div>")
                .append("<div class=\"calendario-dias\" " +
                        "aria-hidden=\"true\"><span>L</span><span>M</span><span>X</span>")
                .append("<span>J</span><span>V</span><span>S</span><span>D</span></div><div class=\"calendario-dias\" ")
                .append("data-dias></div></div><input type=\"hidden\" name=\"fecha\" disabled value=\"")
                .append(Html.escapar(datos.getOrDefault("fecha", ""))).append("\">")
                .append("<fieldset data-duracion hidden><legend>Duración</legend><div class=\"chips " +
                        "solicitud-opciones\">");
        for (int horas : List.of(1, 2, 3))
            html.append(opcion("duracion", "" + horas, horas + " h",
                    datos.getOrDefault("duracion", "2"), "", ""));
        html.append("</div></fieldset><div class=\"stack\" data-horas-panel hidden><h3>Horas libres</h3>")
                .append("<div class=\"chips\" data-horas></div><p class=\"small muted\" role=\"status\" ")
                .append("data-horas-estado></p></div><input type=\"hidden\" name=\"inicio\" disabled value=\"")
                .append(Html.escapar(datos.getOrDefault("inicio", ""))).append(
                        "\"><input type=\"hidden\" name=\"fin\" ")
                .append("disabled value=\"").append(Html.escapar(datos.getOrDefault("fin", ""))).append("\">")
                .append("<noscript><label class=\"field\">Fecha<input class=\"input\" type=\"date\" name=\"fecha\" ")
                .append("required min=\"").append(servicio.hoy()).append("\" value=\"")
                .append(Html.escapar(datos.getOrDefault("fecha", ""))).append("\"></label>")
                .append(campoHora("inicio", "Hora de inicio", datos)).append(campoHora("fin", "Hora de fin", datos))
                .append("</noscript></section><section data-etapa=\"3\" class=\"stack\"><h2>Revisar y enviar</h2>")
                .append("<div class=\"chips solicitud-motivos\">");
        for (String motivo : List.of("Primera evaluación", "Seguimiento", "Cambió un puesto", "Molestias del equipo"))
            html.append("<button type=\"button\" class=\"chip\" data-motivo=\"").append(motivo).append("\">")
                    .append(motivo).append("</button>");
        html.append("</div>").append(Formularios.texto(datos, "motivo", "Motivo (opcional)", 255, false))
                .append("<div class=\"card stack\" data-solicitud-resumen hidden></div>")
                .append("<button class=\"btn p\" type=\"submit\">Enviar solicitud</button></section>")
                .append("<div class=\"row between\" data-solicitud-controles hidden>")
                .append("<button class=\"btn\" type=\"button\" data-anterior>Anterior</button>")
                .append("<button class=\"btn p\" type=\"button\" data-siguiente>Continuar →</button></div></form>")
                .append("<script src=\"/static/js/solicitar-visita.js\" defer></script>");
        return Html.pagina("Solicitar una visita", html.toString(), sesion, "Reservas · Solicitud", "");
    }

    private static String opcion(String nombre, String valor, String texto, String elegida, String ayuda,
                                 String color) {
        return "<label class=\"solicitud-opcion\"" + (color.isEmpty() ? "" : " style=\"--persona:" + color + "\"") +
                "><input type=\"radio\" name=\"" + nombre + "\" value=\"" + Html.escapar(valor) + "\" required" +
                (valor.equals(elegida) ? " checked" : "") + "><span>" + Html.escapar(texto) + "</span>" +
                (ayuda.isEmpty() ? "" : "<small>" + Html.escapar(ayuda) + "</small>") + "</label>";
    }

    private static String campoHora(String nombre, String etiqueta, Map<String, String> datos) {
        return "<label class=\"field\">" + etiqueta + "<input class=\"input\" type=\"time\" name=\"" + nombre +
                "\" required value=\"" + Html.escapar(datos.getOrDefault(nombre, "")) + "\"></label>";
    }
}
