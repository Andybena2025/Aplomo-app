package co.aplomo.web;

import co.aplomo.rula.MotorRula;
import co.aplomo.rula.MotorRula.Entrada;
import co.aplomo.rula.MotorRula.Medicion;
import co.aplomo.rula.MotorRula.Segmento;
import co.aplomo.rula.MedidorAngulos.Lado;
import co.aplomo.rula.MedidorAngulos.Punto;
import co.aplomo.rula.PuntuadorSegmentos.Ajustes;
import co.aplomo.rula.PuntuadorSegmentos.Giro;
import co.aplomo.servicio.Analisis;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-017, RNF-004: adapta campos acotados; no mide ángulos ni calcula puntos RULA.
 */
public final class DatosAnalisis {
    private DatosAnalisis() {
    }

    /**
     * Convierte un formulario de una foto en entradas del único motor RULA. RNF-011.
     */
    public static Entrada leer(Map<String, String> parametros) {
        var mediciones = new EnumMap<Segmento, Medicion>(Segmento.class);
        for (var elemento : Segmento.values()) {
            mediciones.put(elemento, medicion(parametros, elemento));
        }
        return new Entrada(Lado.valueOf(opcion(parametros.get("lado"), "el lado", "DERECHO", "IZQUIERDO")),
                entero(parametros.get("ancho")), entero(parametros.get("alto")), mediciones,
                new Ajustes(si(parametros, "hombroElevado"), si(parametros, "brazoSeparado"),
                        si(parametros, "brazoApoyado"), si(parametros, "antebrazoCruza"),
                        si(parametros, "munecaDesviada"),
                        Giro.valueOf(opcion(parametros.get("giro"), "el giro", "MEDIO", "EXTREMO")),
                        si(parametros, "cuelloGirado"), si(parametros, "cuelloInclinado"),
                        si(parametros, "troncoGirado"), si(parametros, "troncoInclinado"),
                        si(parametros, "piernasApoyadas"), false), si(parametros, "actividad"), MotorRula.Carga.valueOf(
                opcion(parametros.get("carga"), "la carga",
                        Arrays.stream(MotorRula.Carga.values()).map(Enum::name).toArray(String[]::new))));
    }

    /**
     * Una medición; el módulo RULA valida y mide, sin duplicar fórmulas en la pantalla. RNF-011.
     */
    public static Medicion medicion(Map<String, String> parametros, Segmento segmento) {
        List<Punto> ps = new ArrayList<>();
        String texto = parametros.getOrDefault(segmento + "_puntos", "");
        if (!texto.isBlank()) for (String par : texto.split(";", -1)) {
            String[] xy = par.split(",", -1);
            if (xy.length != 2) throw new IllegalArgumentException("Revisa los puntos de la foto.");
            ps.add(new Punto(numero(xy[0]), numero(xy[1])));
        }
        String modo = opcion(parametros.get(segmento + "_modo"), "el modo de medición", "PUNTOS", "MANUAL");
        return new Medicion(ps, modo.equals("MANUAL") ? numero(parametros.get(segmento + "_grados")) : null);
    }

    /**
     * Lee hasta veinte análisis codificados, rechazando índices y campos repetidos. RNF-011.
     */
    public static List<Analisis.Solicitud> solicitudes(Map<String, String> parametros) {
        List<Analisis.Solicitud> lista = new ArrayList<>();
        for (int i = 0; i < 20; i++)
            if (parametros.containsKey("analisis_" + i)) {
                var datos = campos(parametros.get("analisis_" + i));
                int foto;
                try {
                    foto = Integer.parseInt(datos.get("foto"));
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException("Revisa la foto seleccionada.");
                }
                lista.add(new Analisis.Solicitud(foto, leer(datos)));
            }
        for (String clave : parametros.keySet())
            if (clave.startsWith("analisis_") && !clave.matches("analisis_(?:[0-9]|1[0-9])"))
                throw new IllegalArgumentException("Guarda hasta veinte análisis a la vez.");
        return List.copyOf(lista);
    }

    /**
     * Descodifica campos de una única foto sin permitir duplicados ni entradas excesivas. RNF-011.
     */
    public static Map<String, String> campos(String texto) {
        if (texto == null || texto.length() > 12000)
            throw new IllegalArgumentException("Los datos de la foto son demasiado largos.");
        Map<String, String> parametros = new HashMap<>();
        for (String par : texto.split("&")) {
            String[] texto3 = par.split("=", 2);
            if (texto3.length != 2) throw new IllegalArgumentException("Revisa los datos del análisis.");
            String k = URLDecoder.decode(texto3[0], StandardCharsets.UTF_8), texto4 = URLDecoder.decode(texto3[1],
                    StandardCharsets.UTF_8);
            if (parametros.putIfAbsent(k, texto4) != null)
                throw new IllegalArgumentException("El análisis contiene campos repetidos.");
        }
        return parametros;
    }

    private static boolean si(Map<String, String> parametros, String k) {
        String texto3 = parametros.get(k);
        if (!"si".equals(texto3) && !"no".equals(texto3))
            throw new IllegalArgumentException("Revisa las casillas del análisis.");
        return texto3.equals("si");
    }

    private static String opcion(String texto2, String campo, String... opciones) {
        return co.aplomo.servicio.Validacion.opcion(texto2, campo, opciones);
    }

    private static int entero(String texto2) {
        return co.aplomo.servicio.Validacion.entero(texto2, "dimensiones de la foto", 1, Integer.MAX_VALUE);
    }

    private static double numero(String texto2) {
        try {
            double valor = Double.parseDouble(texto2);
            if (!Double.isFinite(valor)) throw new NumberFormatException();
            return valor;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Escribe un ángulo o una coordenada válidos.");
        }
    }
}
