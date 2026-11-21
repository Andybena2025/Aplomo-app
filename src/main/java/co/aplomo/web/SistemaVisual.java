package co.aplomo.web;

import java.util.Locale;
import java.util.Map;

/**
 * RNF-002/007, Paso 27: un solo lenguaje visual para estados y riesgo.
 */
public final class SistemaVisual {
    private static final Map<String, String> ESTADOS = Map.ofEntries(Map.entry("ACTIVA", "Activa"),
            Map.entry("ACTIVO", "Activa"), Map.entry("INACTIVA", "Inactiva"), Map.entry("INACTIVO", "Inactiva"),
            Map.entry("ESPERANDO", "Por confirmar"), Map.entry("EN_COLA", "Aceptada"),
            Map.entry("EN_CURSO", "En curso"), Map.entry("FINALIZADA", "Finalizada"),
            Map.entry("CANCELADA", "Cancelada"), Map.entry("ABIERTO", "Abierto"), Map.entry("EN_PROCESO", "En proceso"),
            Map.entry("ARREGLADO", "Resuelto"), Map.entry("POR_EVALUAR", "Por evaluar"), Map.entry("HECHA", "Resuelto"),
            Map.entry("PENDIENTE", "Pendiente"), Map.entry("BLOQUEADA", "Bloqueada temporalmente"));

    private SistemaVisual() {
    }

    /**
     * RNF-002: el color del estado nunca representa riesgo ni la persona.
     */
    public static String estado(String estado) {
        String valor = estado == null ? "" : estado;
        String clase = switch (valor) {
            case "ACTIVA", "ACTIVO", "EN_COLA" -> "activa";
            case "INACTIVA", "INACTIVO", "CANCELADA", "BLOQUEADA" -> "inactiva";
            case "FINALIZADA", "ARREGLADO", "HECHA", "EVALUADO" -> "resuelta";
            case "ESPERANDO", "ABIERTO", "PENDIENTE" -> "esperando";
            default -> "curso";
        };
        String titulo = ESTADOS.getOrDefault(valor, valor.equals("EVALUADO") ? "Evaluado" : valor);
        return "<span class=\"pill estado-" + clase + "\"" + (valor.equals("CANCELADA") ? " data-cancelada" : "") +
                "><i aria-hidden=\"true\"></i>" + (clase.equals("resuelta") ? "✓ " : "") + Html.escapar(titulo) +
                "</span>";
    }

    /**
     * RF-020/027: clasificación recibida del servidor, presentada sin barras ni colores fuertes.
     */
    public static String riesgo(Integer puntaje, String color, int nivel) {
        if (puntaje == null) return "<span class=\"texto-secundario\">Sin resultado</span>";
        String clase = color == null ? "" : color.toLowerCase(Locale.ROOT);
        if (!java.util.List.of("verde", "amarillo", "rojo").contains(clase)) clase = "";
        String nombre = clase.isEmpty() ? "" : Character.toUpperCase(clase.charAt(0)) + clase.substring(1);
        String etiqueta = puntaje + " · " + nombre + " · Nivel " + nivel;
        return "<span class=\"riesgo-calmo riesgo-" + clase + "\" aria-label=\"" + Html.escapar(etiqueta) +
                "\"><i aria-hidden=\"true\"></i><b>" + puntaje + "</b><span>Nivel " + nivel + "</span></span>";
    }

    /**
     * RNF-007: encabezado de grupo legible sin códigos de reserva.
     */
    public static String grupo(String nombre, int cantidad) {
        return "<h2 class=\"lista-grupo\">" + Html.escapar(nombre) + "<span>" + cantidad + "</span></h2>";
    }
}
