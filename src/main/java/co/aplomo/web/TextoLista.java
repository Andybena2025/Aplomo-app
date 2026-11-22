package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * RF-024, RNF-007: texto compartido, unidades explícitas y ninguna hora con segundos.
 */
public final class TextoLista {
    private TextoLista() {
    }

    /**
     * RNF-007: conserva la hora registrada, sin segundos ni cero inicial.
     */
    public static String hora(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String tiempo = valor.contains("T") || valor.contains(" ") ? valor.substring(11) : valor;
        try {
            return LocalTime.parse(tiempo).format(DateTimeFormatter.ofPattern("H:mm"));
        } catch (RuntimeException error) {
            return "";
        }
    }

    /**
     * RNF-007: fecha colombiana a partir de DATE o DATETIME, sin convertir el día.
     */
    public static String fecha(String valor) {
        try {
            return Html.fecha(LocalDate.parse(valor.substring(0, 10)));
        } catch (RuntimeException error) {
            return "Sin fecha";
        }
    }

    /**
     * RF-024: el título identifica el trabajo; el número del puesto queda en el detalle.
     */
    public static String puesto(Fila fila) {
        return puesto(fila.valor("cargo_nombre"),
                fila.valor("puesto_descripcion").isBlank() ? fila.valor("descripcion") : fila.valor(
                        "puesto_descripcion"));
    }

    /**
     * RF-024: no inventa un cargo cuando no está registrado.
     */
    public static String puesto(String cargo, String descripcion) {
        String trabajo = descripcion == null || descripcion.isBlank() ? "Sin descripción del puesto" : descripcion;
        return cargo == null || cargo.isBlank() ? trabajo : cargo + " · " + trabajo;
    }

    /**
     * RNF-007: prioridad escrita, nunca un código de barras sin nombre.
     */
    public static String prioridad(String valor) {
        if (valor == null || valor.isBlank()) return "<span class=\"muted\">Sin prioridad</span>";
        String clave = List.of("ALTA", "MEDIA", "BAJA").contains(valor) ? valor : "BAJA";
        return "<span class=\"pill prioridad-" + clave.toLowerCase(java.util.Locale.ROOT) + "\">" +
                Formularios.etiqueta(clave) + "</span>";
    }

    /**
     * RNF-007: límite y urgencia siempre con fecha y unidad.
     */
    public static String limite(String valor) {
        if (valor == null || valor.isBlank()) return "<span class=\"muted\">Sin fecha límite</span>";
        LocalDate fecha = LocalDate.parse(valor.substring(0, 10));
        long dias = ChronoUnit.DAYS.between(Html.hoy(), fecha);
        String texto = dias < 0 ?
                "Venció hace " + Math.abs(dias) + (dias == -1 ? " día" : " días") :
                dias == 0 ? "Vence hoy" : "Vence en " + dias + (dias == 1 ? " día" : " días");
        return "<span>" + Html.fecha(fecha) + "<small class=\"" + (dias <= 7 ? "limite-urgente" : "muted") + "\">" +
                texto + "</small></span>";
    }

    /**
     * RNF-002: el punto personal tiene el nombre al lado y solo admite la paleta conocida.
     */
    public static String persona(String nombre, String color) {
        String completo = nombre == null || nombre.isBlank() ? "Sin evaluador" : Html.nombreSinTitulo(nombre);
        String[] partes = completo.split("\\s+");
        String corto = partes.length < 2 ? completo :
                partes[0] + " " + partes[partes.length > 3 ? partes.length - 2 : partes.length - 1];
        return "<span class=\"persona-lista\" title=\"" + Html.escapar(completo) + "\" style=\"--persona:" +
                color(color) + "\"><i aria-hidden=\"true\"></i><span class=\"persona-nombre\">" + Html.escapar(corto) +
                "</span></span>";
    }

    /**
     * RNF-004: los valores de estilo no admiten texto arbitrario de la base.
     */
    public static String color(String valor) {
        return valor != null && co.aplomo.servicio.ColoresAgenda.PALETA.contains(valor) ? valor : "#C9CCD2";
    }

    /**
     * RF-024: barra de conteos; no calcula puntajes ni niveles RULA.
     */
    public static String barra(List<Fila> filas) {
        Map<String, Long> conteos = filas.stream().filter(f -> !f.valor("color").isBlank()).collect(
                java.util.stream.Collectors.groupingBy(f -> f.valor("color"), java.util.stream.Collectors.counting()));
        long total = conteos.values().stream().mapToLong(Long::longValue).sum();
        StringBuilder html = new StringBuilder(
                "<span class=\"resumen-riesgo\"><span class=\"barra-riesgo\" " + "aria-hidden=\"true\">");
        for (String color : List.of("VERDE", "AMARILLO", "ROJO")) {
            long cantidad = conteos.getOrDefault(color, 0L);
            html.append("<i class=\"riesgo-").append(color.toLowerCase(java.util.Locale.ROOT))
                    .append("\" style=\"flex:").append(cantidad).append("\"></i>");
        }
        if (total == 0) html.append("<i class=\"sin-resultados\"></i>");
        html.append("</span><span class=\"leyenda-riesgo\">");
        for (String color : List.of("VERDE", "AMARILLO", "ROJO")) {
            long cantidad = conteos.getOrDefault(color, 0L);
            html.append("<span class=\"").append(cantidad == 0 ? "muted" : "riesgo-" + color.toLowerCase())
                    .append("\">● ").append(cantidad).append(" ").append(Formularios.etiqueta(color)).append("</span>");
        }
        return html.append("</span></span>").toString();
    }
}
