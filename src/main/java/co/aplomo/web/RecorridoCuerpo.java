package co.aplomo.web;

import co.aplomo.rula.MedidorAngulos;
import co.aplomo.rula.PuntuadorSegmentos;
import co.aplomo.rula.MedidorAngulos.Angulos;
import co.aplomo.rula.MedidorAngulos.Lado;
import co.aplomo.rula.MedidorAngulos.Punto;
import co.aplomo.rula.MotorRula.Segmento;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Paso 13E-B: catálogo visual y adaptación al motor existente; ninguna fórmula RULA en JavaScript. RNF-011.
 */
public final class RecorridoCuerpo {
    private RecorridoCuerpo() {
    }

    /**
     * RNF-011: componente de la aplicación.
     */
    public record Rango(String imagen, String etiqueta, double grados, int puntos, String ayuda) {
    }

    public static final Map<Segmento, List<Rango>> RANGOS = Map.of(Segmento.BRAZO,
            List.of(new Rango("brazo_extension.png", "Hacia atrás más de 20°", -30, 2,
                            "El codo queda por detrás del cuerpo."),
                    new Rango("brazo_neutro.png", "Entre −20° y 20°", 0, 1, "El brazo cuelga junto al cuerpo."),
                    new Rango("brazo_20_45.png", "20° a 45°", 30, 2, "El brazo va un poco hacia adelante, codo bajo."),
                    new Rango("brazo_45_90.png", "45° a 90°", 60, 3,
                            "El brazo va hacia adelante, por debajo del hombro."),
                    new Rango("brazo_mas_90.png", "Más de 90°", 110, 4, "La mano trabaja por encima del hombro.")),
            Segmento.ANTEBRAZO, List.of(new Rango("antebrazo_60_100.png", "Entre 60° y 100°", 80, 1,
                            "El codo está doblado más o menos en escuadra."),
                    new Rango("antebrazo_fuera.png", "Menos de 60° o más de 100°", 40, 2,
                            "El brazo está casi estirado o muy doblado.")), Segmento.MUNECA,
            List.of(new Rango("muneca_neutra.png", "Recta", 0, 1, "La mano sigue la línea del antebrazo."),
                    new Rango("muneca_15.png", "Doblada hasta 15°", 10, 2,
                            "La mano se dobla un poco hacia arriba o hacia abajo."),
                    new Rango("muneca_mas_15.png", "Doblada más de 15°", 25, 3,
                            "La mano se dobla mucho hacia arriba o hacia abajo.")), Segmento.CUELLO,
            List.of(new Rango("cuello_0_10.png", "0° a 10°", 5, 1, "Mira casi al frente."),
                    new Rango("cuello_10_20.png", "10° a 20°", 15, 2, "La cabeza se inclina un poco hacia abajo."),
                    new Rango("cuello_mas_20.png", "Más de 20°", 30, 3, "La cabeza se inclina mucho hacia abajo."),
                    new Rango("cuello_extension.png", "Hacia atrás", -15, 4,
                            "Mira hacia arriba, con la cabeza echada atrás.")), Segmento.TRONCO,
            List.of(new Rango("tronco_erguido.png", "Erguido o sentado bien apoyado", 0, 1,
                            "Espalda recta, o sentado con buen respaldo."),
                    new Rango("tronco_0_20.png", "0° a 20°", 10, 2, "La espalda se inclina un poco."),
                    new Rango("tronco_20_60.png", "20° a 60°", 40, 3, "La espalda se dobla claramente."),
                    new Rango("tronco_mas_60.png", "Más de 60°", 75, 4, "La espalda queda casi horizontal.")));
    public static final String[][] ORDEN = {{"BRAZO", "Brazo"}, {"ANTEBRAZO", "Antebrazo"}, {"MUNECA", "Muñeca"}, {
        "GIRO", "Giro de muñeca"}, {"CUELLO", "Cuello"}, {"TRONCO", "Tronco"}, {"PIERNAS", "Piernas"}};
    public static final Map<String, String[][]> AJUSTES = Map.of("BRAZO",
            new String[][]{{"hombroElevado", "Hombro levantado", "brazo_hombro.png",
                    "El hombro sube hacia la oreja."}, {"brazoSeparado", "Brazo separado del cuerpo",
                    "brazo_separado" +
                            ".png", "El codo se aleja del costado."}, {"brazoApoyado", "Apoyado o persona inclinada",
                    "brazo_apoyado.png",
                    "El brazo descansa en la mesa o en un apoyo, o la persona se inclina y el pes" + "o lo sostiene."}},
            "ANTEBRAZO",
            new String[][]{{"antebrazoCruza", "Cruza la línea media o sale hacia el lado", "antebrazo_cruza.png",
                    "La" + " mano trabaja al otro lado del cuerpo o muy hacia afuera."}}, "MUNECA",
            new String[][]{{"munecaDesviada", "Doblada hacia un lado", "muneca_desviada.png",
                    "La mano se dobla hacia" + " el pulgar o hacia el meñique."}}, "CUELLO",
            new String[][]{{"cuelloGirado", "Girado", "cuello_girado.png", "La cabeza voltea hacia un lado."}, {
                "cuelloInclinado", "Inclinado hacia un lado", "cuello_inclinado.png", "La oreja se acerca al hombro."}},
            "TRONCO",
            new String[][]{{"troncoGirado", "Girado", "tronco_girado.png", "Los hombros giran respecto a la cadera."}
                    , {"troncoInclinado", "Inclinado hacia un lado", "tronco_inclinado.png",
                    "El cuerpo se dobla " + "hacia un costado."}});

    /**
     * Solo lee el componente que se está observando. Nunca emite un puntaje final incompleto. RNF-011.
     */
    public static String parcial(Map<String, String> parametros) {
        String seg = ValidacionEntrada.segmento(parametros.get("segmento"));
        if (seg.equals("GIRO") || seg.equals("PIERNAS")) {

            // Estos dos componentes son independientes de los ángulos y demás ajustes.

            // Se proyecta únicamente el componente pedido, nunca los valores auxiliares ni un total.
            var giro = seg.equals("GIRO") ? PuntuadorSegmentos.Giro.valueOf(
                    co.aplomo.servicio.Validacion.opcion(parametros.get("respuesta"), "el giro", "MEDIO",
                            "EXTREMO")) : PuntuadorSegmentos.Giro.MEDIO;
            boolean piernas = seg.equals("PIERNAS") ? si(parametros, "respuesta") : true;
            var puntos = PuntuadorSegmentos.puntuar(new Angulos(0, 0, 0, 0, 0),
                    new PuntuadorSegmentos.Ajustes(false, false, false, false, false, giro, false, false, false, false,
                            piernas, false));
            return respuesta(seg, null, null, seg.equals("GIRO") ? puntos.giro() : puntos.piernas(), true, false);
        }
        var segmento = Segmento.valueOf(seg);
        var elemento = DatosAnalisis.medicion(parametros, segmento);
        int ancho = co.aplomo.servicio.Validacion.entero(parametros.get("ancho"), "ancho de la foto", 1,
                Integer.MAX_VALUE), alto = co.aplomo.servicio.Validacion.entero(parametros.get("alto"),
                "alto de la foto", 1, Integer.MAX_VALUE);
        var lado = Lado.valueOf(
                co.aplomo.servicio.Validacion.opcion(parametros.get("lado"), "el lado", "DERECHO", "IZQUIERDO"));
        if (elemento.puntos().size() != 0 && elemento.puntos().size() != 3)
            throw new IllegalArgumentException("Cada segmento necesita sus tres puntos completos.");
        for (var elemento2 : elemento.puntos())
            if (elemento2.x() < 0 || elemento2.x() > 1 || elemento2.y() < 0 || elemento2.y() > 1)
                throw new IllegalArgumentException("Los puntos deben estar dentro de la foto.");
        double grados;
        if (elemento.manual() != null) grados = elemento.manual();
        else {
            if (elemento.puntos().size() != 3)
                throw new IllegalArgumentException("Marca los tres puntos o elige un mosaico por observación directa.");
            var ps = elemento.puntos().stream().map(elemento3 -> new Punto(elemento3.x() * ancho, elemento3.y() * alto))
                    .toList();
            grados = switch (segmento) {
                case BRAZO -> MedidorAngulos.brazo(ps.get(0), ps.get(1), ps.get(2), lado);
                case ANTEBRAZO -> MedidorAngulos.antebrazo(ps.get(0), ps.get(1), ps.get(2));
                case MUNECA -> MedidorAngulos.muneca(ps.get(0), ps.get(1), ps.get(2));
                case CUELLO -> MedidorAngulos.cuello(ps.get(0), ps.get(1), ps.get(2), lado);
                case TRONCO -> MedidorAngulos.tronco(ps.get(0), ps.get(1), lado);
            };
        }
        int base = puntuar(segmento, grados, Map.of(), false);
        List<Rango> rangos = RANGOS.get(segmento);
        Rango rango = rangos.stream().filter(elemento4 -> elemento4.puntos() == base &&
                        (segmento != Segmento.BRAZO || base != 2 ||(grados < 0) ==(elemento4.grados() < 0))).
                            findFirst()
                .orElseThrow();
        boolean confirmado = "si".equals(parametros.get("ajustes_confirmados"));
        return respuesta(seg, grados, rango.grados(), confirmado ? puntuar(segmento, grados, parametros, true) : null,
                confirmado, elemento.manual() != null);
    }

    private static String respuesta(String seg, Double grados, Double rango, Integer puntos, boolean confirmado,
            boolean manual) {
        return "<span data-parcial=\"" + seg + "\" data-grados=\"" + (grados == null ? "" : grados) +
                "\" data-rango=\"" + (rango == null ? "" : rango) + "\" data-puntos=\"" +
                (puntos == null ? "" : puntos) + "\" data-confirmado=\"" + confirmado + "\" data-manual=\"" + manual +
                "\">" +
                (puntos == null ? "Medición pendiente de confirmar" : puntos + (puntos == 1 ? " punto" : " puntos")) +
                "</span>";
    }

    private static int puntuar(Segmento elemento, double valor2, Map<String, String> parametros, boolean ajustes) {
        return switch (elemento) {
            case BRAZO -> PuntuadorSegmentos.brazo(valor2, ajustes && si(parametros, "hombroElevado"),
                    ajustes && si(parametros, "brazoSeparado"), ajustes && si(parametros, "brazoApoyado"));
            case ANTEBRAZO -> PuntuadorSegmentos.antebrazo(valor2, ajustes && si(parametros, "antebrazoCruza"));
            case MUNECA -> PuntuadorSegmentos.muneca(valor2, ajustes && si(parametros, "munecaDesviada"));
            case CUELLO -> PuntuadorSegmentos.cuello(valor2, ajustes && si(parametros, "cuelloGirado"),
                    ajustes && si(parametros, "cuelloInclinado"));
            case TRONCO -> PuntuadorSegmentos.tronco(valor2, ajustes && si(parametros, "troncoGirado"),
                    ajustes && si(parametros, "troncoInclinado"), false);
        };
    }

    private static boolean si(Map<String, String> parametros, String clave) {
        return co.aplomo.servicio.Validacion.opcion(parametros.get(clave), "la respuesta observada", "si", "no")
                .equals("si");
    }

    private static final class ValidacionEntrada {
        static String segmento(String texto) {
            return co.aplomo.servicio.Validacion.opcion(texto, "el segmento",
                    Arrays.stream(ORDEN).map(texto2 -> texto2[0]).toArray(String[]::new));
        }
    }

    /**
     * Reutiliza el editor y el esfuerzo actuales; la secuencia y sus respuestas las dirige recorrido.js. RNF-011.
     */
    public static String pasos() {
        StringBuilder contenido = new StringBuilder(
                "<section class=\"card stack tour-pregunta\" data-paso=\"7\" hidden><h2>¿Qué " +
                        "lado tiene más carga?</h2><details class=\"ayuda-lado\" data-ayuda-lado><sum" +
                        "mary>Ver cómo decidir</summary><div><p>¿Cómo saber qué lado tiene más carga?" +
                        " Observa varios ciclos de la tarea:</p><ul><li>¿Con qué mano sostiene la her" +
                        "ramienta, la pieza o la carga?</li><li>¿Qué brazo se levanta más o se aleja " +
                        "más del cuerpo?</li><li>¿Qué brazo repite más el movimiento?</li><li>¿Qué mu" +
                        "ñeca se dobla o gira más?</li></ul><p>Si no está claro, elige «Ambos».</p></" +
                        "div></details><div class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"" +
                        "Lado que vas a evaluar\">");
        for (var lado : new String[][]{{"DERECHO", "Derecho", "lado_derecho.png", "El lado derecho trabaja más."}, {
            "IZQUIERDO", "Izquierdo", "lado_izquierdo.png", "El lado izquierdo trabaja más."}, {"AMBOS", "Ambos",
                "lado_ambos.png", "Trabajan parecido o no está claro."}})
            contenido.append(opcion("lado_recorrido", lado[0], lado[1], lado[2], false, "", lado[3]));
        contenido.append("</div></section>").append(PaginasAnalisis.pasos()
                .replace("class=\"card stack\" data-paso", "class=\"card stack tour-pregunta\" data-paso")
                .replace("data-paso=\"4\"", "data-paso=\"8\"").replace("data-paso=\"5\"", "data-controles-anteriores")
                .replace("data-paso=\"6\"", "data-paso=\"11\""));
        contenido.append(esfuerzo());
        contenido.append("<div data-cuerpo-cabecera hidden><div class=\"cuerpo-pildoras\" aria-label=" +
                "\"Segmentos del cuerpo\">");
        for (var seg : ORDEN)
            contenido.append("<span class=\"tag\" data-pildora=\"").append(seg[0]).append("\">").append(seg[1])
                    .append("</span>");
        contenido.append("</div><p class=\"hint\" data-tarea-cuerpo></p><div class=\"row\"><span class" +
                "=\"tag\" role=\"status\" data-puntaje-parcial>Puntaje parcial · Pendiente</s" +
                "pan><span class=\"hint\" data-final-pendiente>Puntaje final: Pendiente</span" +
                "></div></div><div data-cuerpo-rangos hidden class=\"stack cuerpo-rangos\"><p" +
                " role=\"status\" class=\"hint\" data-sugerencia-foto></p>");
        for (var seg : ORDEN) {
            contenido.append("<div data-rangos=\"").append(seg[0])
                    .append("\" class=\"tour-opciones\" role=\"radiogroup\" aria-label=\"Rango de ")
                    .append(seg[1].toLowerCase(Locale.ROOT)).append("\" hidden>");
            if (segmento(seg[0]) != null) for (var rango : RANGOS.get(segmento(seg[0])))
                contenido.append(opcion("rango_" + seg[0], "" + rango.grados(), rango.etiqueta(), rango.imagen(), false,
                        "data-rango-segmento=\"" + seg[0] + "\"", rango.ayuda()));
            else if (seg[0].equals("GIRO")) {
                contenido.append(opcion("rango_GIRO", "MEDIO", "Rango medio", "giro_medio.png", false,
                        "data-rango-segmento=\"GIRO\"", "La palma gira a medias, sin forzar."));
                contenido.append(opcion("rango_GIRO", "EXTREMO", "Cerca del extremo", "giro_extremo.png", false,
                        "data-rango-segmento=\"GIRO\"", "La palma gira casi al máximo, hacia arriba o hacia abajo."));
            } else {
                contenido.append(
                        opcion("rango_PIERNAS", "si", "Bien apoyadas y equilibradas", "piernas_apoyadas.png", false,
                                "data-rango-segmento=\"PIERNAS\"", "Los dos pies apoyados y el peso repartido."));
                contenido.append(
                        opcion("rango_PIERNAS", "no", "Sin buen apoyo o desequilibradas", "piernas_no_apoyadas.png",
                                false, "data-rango-segmento=\"PIERNAS\"",
                                "Un solo pie, de puntillas o sin apoyo estable."));
            }
            contenido.append("</div>");
        }
        contenido.append("<button type=\"button\" class=\"link\" data-corregir-cuerpo>Corregir medició" +
                "n</button><div class=\"field\" data-manual-cuerpo hidden><label for=\"angulo" +
                "-cuerpo\">Ángulo corregido a mano</label><input id=\"angulo-cuerpo\" type=\"" +
                "number\" class=\"input\" min=\"-180\" max=\"180\" step=\"0.01\"><button type" +
                "=\"button\" class=\"link\" data-restaurar-foto>Usar los puntos de la foto</b" +
                "utton></div><button type=\"button\" class=\"link\" data-no-foto>No se ve en " +
                "la foto</button></div><dialog class=\"hoja-cuerpo\" aria-modal=\"true\" role" +
                "=\"dialog\" aria-labelledby=\"titulo-hoja-cuerpo\"><div class=\"stack\"><div" +
                " class=\"row between\"><h2 id=\"titulo-hoja-cuerpo\">¿Algo más en el segment" +
                "o?</h2><button type=\"button\" class=\"btn\" data-cerrar-hoja aria-label=\"C" +
                "errar ajustes\">Cerrar</button></div>");
        for (var seg : ORDEN)
            if (AJUSTES.containsKey(seg[0])) {
                contenido.append("<div class=\"tour-opciones\" data-hoja-segmento=\"").append(seg[0])
                        .append("\" role=\"group\" aria-label=\"Ajustes de ").append(seg[1].toLowerCase(Locale.ROOT))
                        .append("\" hidden>");
                for (var texto : AJUSTES.get(seg[0]))
                    contenido.append(opcion("ajuste_" + texto[0], texto[0], texto[1], texto[2], true,
                            "data-cuerpo-ajuste=\"" + texto[0] + "\"", texto[3]));
                String img = seg[0].equals("CUELLO") ? "cuello_ninguno.png" : seg[0].equals(
                        "TRONCO") ? "tronco_ninguno.png" : "brazo_ninguno.png";
                contenido.append(opcion("ninguno_" + seg[0], "NINGUNO", "Ninguno", img, true, "data-cuerpo-ninguno",
                        "No ves ninguno de estos.")).append("</div>");
            }
        return contenido.append("<p class=\"tour-pendiente\" role=\"status\" data-aviso-hoja hidden>Responde " +
                "para continuar.</p><button type=\"button\" class=\"btn p\" data-listo-hoja a" +
                "ria-disabled=\"true\">Listo</button></div></dialog>").toString();
    }

    /**
     * RF-019: correspondencia fija uno a uno con el enum del motor existente.
     */
    public static final String[][] CARGAS = {{"MENOS_2KG_INTERMITENTE", "Menos de 2 kg, a ratos", "carga_1.png", "Por" +
            " ejemplo papeles, un mouse o una pieza pequeña."}, {"DE_2_A_10KG_INTERMITENTE", "2 a 10 kg, a ratos",
            "carga_2.png", "Por ejemplo una resma o una herramienta, de vez en cuando."}, {
        "DE_2_A_10KG_ESTATICA_O_REPETITIVA", "2 a 10 kg, sostenido o repetido", "carga_3.png",
            "Ese mismo peso, " +
                    "sostenido o levantado una y otra vez."}, {"MAS_10KG_INTERMITENTE", "Más de 10 kg, a ratos",
            "carga_3" + ".png", "Por ejemplo una caja pesada, de vez en cuando."}, {"MAS_10KG_ESTATICA_O_REPETITIVA",
            "Más de 10 " +
                    "kg, sostenido o repetido", "carga_4.png", "Carga pesada sostenida o levantada una y otra vez."},
            {"GOLPES_O_FUERZAS_BRUSCAS", "Golpes o fuerzas bruscas", "carga_4.png",
            "Martilla, golpea o hace tirones " + "fuertes."}};

    private static String esfuerzo() {
        StringBuilder contenido = new StringBuilder(
                "<section class=\"card stack tour-pregunta\" data-paso=\"9\" hidden><p class=" +
                        "\"hint\" data-tarea-esfuerzo></p><h2>¿La postura se mantiene más de 1 minuto" +
                        " o se repite más de 4 veces por minuto?</h2><p class=\"hint\" data-recuerdo-" +
                        "esfuerzo hidden>Respuesta del puesto anterior: revísala y pulsa Continuar pa" +
                        "ra confirmarla.</p><div class=\"tour-opciones\" role=\"radiogroup\" aria-lab" +
                        "el=\"Actividad muscular\">");
        for (var texto : new String[][]{{"ESTATICA", "Se mantiene más de 1 minuto", "esfuerzo_estatica.png",
                "Sostiene la misma postura sin moverse más de 1 minuto."}, {"REPETITIVA",
                "Se repite más de 4 veces " +
                        "por minuto", "esfuerzo_repetitiva.png", "Repite el mismo movimiento más de 4 veces por " +
                "minuto."}, {"NINGUNA", "Ninguna de las dos", "esfuerzo_ninguno.png",
                "Se mueve con libertad, sin sostener ni repetir" + "."}})
            contenido.append(
                    opcion("esfuerzo_opcion", texto[0], texto[1], texto[2], false, "data-esfuerzo-opcion", texto[3]));
        contenido.append("</div><p class=\"hint\">Puntaje final: Pendiente</p></section><section class" +
                "=\"card stack tour-pregunta\" data-paso=\"10\" hidden><p class=\"hint\" data" +
                "-tarea-esfuerzo></p><h2>¿Cuánto pesa lo que maneja?</h2><p class=\"hint\" da" +
                "ta-recuerdo-esfuerzo hidden>Respuesta del puesto anterior: revísala y pulsa " +
                "Continuar para confirmarla.</p><div class=\"tour-opciones\" role=\"radiogrou" +
                "p\" aria-label=\"Carga o fuerza\">");
        for (var texto2 : CARGAS)
            contenido.append(
                    opcion("carga_opcion", texto2[0], texto2[1], texto2[2], false, "data-carga-opcion", texto2[3]));
        return contenido.append("</div><p class=\"hint\">Una resma de papel ≈ 2,5 kg · un galón de agua ≈ 4 k" +
                "g</p><p class=\"hint\">Puntaje final: Pendiente</p></section>").toString();
    }

    private static Segmento segmento(String texto) {
        try {
            return Segmento.valueOf(texto);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static String opcion(String nombre, String valor, String etiqueta, String imagen, boolean multiple,
            String extra, String ayuda) {
        return PaginasEvaluacion.opcion(nombre, valor, etiqueta, imagen, multiple, extra, ayuda);
    }
}
