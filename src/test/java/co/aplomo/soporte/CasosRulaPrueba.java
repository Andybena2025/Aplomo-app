package co.aplomo.soporte;

import co.aplomo.rula.MotorRula.Carga;
import co.aplomo.rula.MotorRula.Entrada;
import co.aplomo.rula.MotorRula.Medicion;
import co.aplomo.rula.MotorRula.Segmento;
import co.aplomo.rula.MedidorAngulos.Lado;
import co.aplomo.rula.PuntuadorSegmentos.Ajustes;
import co.aplomo.rula.PuntuadorSegmentos.Giro;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diez entradas manuales independientes del motor, para HTTP, persistencia y evidencia visual.
 */
public final class CasosRulaPrueba {
    private CasosRulaPrueba() {
    }

    /**
     * Caso manual con final esperado explícito.
     */
    public record Caso(int numero, Entrada entrada, int esperado) {
    }

    /**
     * Entrega los diez casos de docs/03 con NEUTRO=5°, sin obtener sus esperados del propio cálculo.
     */
    public static List<Caso> todos() {
        var sin = Ajustes.sinAjustes();
        var hombro = new Ajustes(true, true, false, false, false, Giro.MEDIO, false, false, false, false, true, false);
        var max = new Ajustes(true, true, false, true, true, Giro.EXTREMO, true, true, true, true, false, false);
        return List.of(caso(1, new double[]{1, 85, 9, 10, 2}, sin, false, Carga.MENOS_2KG_INTERMITENTE, 2),
                caso(2, new double[]{61, 47, 0, 37, 17}, sin, false, Carga.MENOS_2KG_INTERMITENTE, 3),
                caso(3, new double[]{61, 47, 0, 37, 17}, hombro, false, Carga.MENOS_2KG_INTERMITENTE, 4),
                caso(4, new double[]{0, 90, 0, 0, 0}, sin, false, Carga.MENOS_2KG_INTERMITENTE, 1),
                caso(5, new double[]{0, 90, 0, 0, 0}, sin, true, Carga.MENOS_2KG_INTERMITENTE, 2),
                caso(6, new double[]{0, 90, 0, 0, 0}, sin, false, Carga.DE_2_A_10KG_INTERMITENTE, 2),
                caso(7, new double[]{61, 47, 0, 37, 17}, sin, false, Carga.DE_2_A_10KG_ESTATICA_O_REPETITIVA, 6),
                caso(8, new double[]{61, 47, 0, 37, 17}, sin, true, Carga.MAS_10KG_INTERMITENTE, 7),
                caso(9, new double[]{0, 90, 0, 0, 0}, sin, false, Carga.MAS_10KG_ESTATICA_O_REPETITIVA, 4),
                caso(10, new double[]{120, 20, 30, -20, 70}, max, true, Carga.GOLPES_O_FUERZAS_BRUSCAS, 7));
    }

    private static Caso caso(int cantidad, double[] gs, Ajustes elemento, boolean actividad, Carga carga, int fin) {
        var parametros = new EnumMap<Segmento, Medicion>(Segmento.class);
        for (var elemento2 : Segmento.values())
            parametros.put(elemento2, new Medicion(List.of(), gs[elemento2.ordinal()]));
        return new Caso(cantidad, new Entrada(Lado.DERECHO, 200, 400, parametros, elemento, actividad, carga), fin);
    }

    /**
     * Codifica las entradas como lo hace el formulario web; no transmite puntajes esperados.
     */
    public static Map<String, String> campos(Entrada elemento) {
        Map<String, String> parametros2 = new LinkedHashMap<>();
        parametros2.put("lado", elemento.lado().name());
        parametros2.put("ancho", "" + elemento.ancho());
        parametros2.put("alto", "" + elemento.alto());
        for (var elemento2 : Segmento.values()) {
            var elemento3 = elemento.mediciones().get(elemento2);
            parametros2.put(elemento2 + "_modo", elemento3.manual() == null ? "PUNTOS" : "MANUAL");
            if (elemento3.manual() != null) parametros2.put(elemento2 + "_grados", "" + elemento3.manual());
            parametros2.put(elemento2 + "_puntos",
                    elemento3.puntos().stream().map(elemento4 -> elemento4.x() + "," + elemento4.y())
                            .collect(java.util.stream.Collectors.joining(";")));
        }
        var elemento5 = elemento.ajustes();
        String[] ks = {"hombroElevado", "brazoSeparado", "brazoApoyado", "antebrazoCruza", "munecaDesviada",
                "cuelloGirado", "cuelloInclinado", "troncoGirado", "troncoInclinado", "piernasApoyadas", "actividad"};
        boolean[] vs = {elemento5.hombroElevado(), elemento5.brazoSeparado(), elemento5.brazoApoyado(),
                elemento5.antebrazoCruza(), elemento5.munecaDesviada(), elemento5.cuelloGirado(),
                elemento5.cuelloInclinado(), elemento5.troncoGirado(), elemento5.troncoInclinado(),
                elemento5.piernasApoyadas(), elemento.actividad()};
        for (int i = 0; i < ks.length; i++) parametros2.put(ks[i], vs[i] ? "si" : "no");
        parametros2.put("giro", elemento5.giro().name());
        parametros2.put("carga", elemento.carga().name());
        return parametros2;
    }

    /**
     * Codifica una entrada por foto, conservando UTF-8 y los separadores.
     */
    public static String codificar(Map<String, String> campos) {
        return campos.entrySet().stream().map(parametros2 ->
                        java.net.URLEncoder.encode(parametros2.getKey(), java.nio.charset.StandardCharsets.UTF_8) +
                                "=" +
                                java.net.URLEncoder.encode(parametros2.getValue(),
                                        java.nio.charset.StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
    }
}
