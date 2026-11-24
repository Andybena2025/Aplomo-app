package co.aplomo.servicio;

import co.aplomo.dao.SugerenciaDao.Regla;
import co.aplomo.rula.MotorRula;
import co.aplomo.rula.MotorRula.Calculo;
import co.aplomo.rula.MotorRula.Segmento;
import co.aplomo.rula.PuntuadorSegmentos.Ajustes;
import co.aplomo.rula.TablasRula;
import java.util.*;

/** RF-018/023, RF-027, RNF-010: explicación y simulación; puntúa exclusivamente MotorRula. */
public final class PlanAtencion40 {
    /** RF-018: textos, mínimos, máximos con ajustes y metas en un solo lugar, según docs/03 y Andy. */
    public record Meta(String nombre, int minimo, int maximo, double grados, String rango, String cambio) { }

    /** RF-018: tabla de presentación y objetivos; no contiene tablas A, B ni C. */
    public static final Map<String, Meta> METAS = Map.of(
        "MUNECA", new Meta("Muñeca", 1, 4, 0, "Muñeca neutra, hasta 5°",
            "Ajustar la herramienta y su soporte para mantener la muñeca neutra, sin desviarla."),
        "TRONCO", new Meta("Tronco", 1, 6, 0, "Tronco erguido, hasta 5° (o sentado bien apoyado)",
            "Acercar y ajustar la altura del trabajo para mantener el tronco erguido, sin girarlo."),
        "CUELLO", new Meta("Cuello", 1, 6, 0, "Cuello de 0° a 10°",
            "Ajustar la altura y la distancia del punto de mirada, sin torcer ni inclinar el cuello."),
        "ANTEBRAZO", new Meta("Antebrazo", 1, 3, 85, "Codo entre 60° y 100°",
            "Acercar las piezas y ajustar la altura para mantener el codo entre 60° y 100°, sin cruzar el cuerpo."),
        "BRAZO", new Meta("Brazo", 1, 6, 0, "Brazo entre −20° y 20°",
            "Acercar el trabajo para mantener el brazo junto al cuerpo y el hombro relajado."),
        "PIERNAS", new Meta("Piernas", 1, 2, 0, "Pies bien apoyados y peso equilibrado",
            "Ajustar el apoyo para que los dos pies descansen de forma estable y equilibrada."));
    private static final List<String> ORDEN = List.of("MUNECA", "TRONCO", "CUELLO", "ANTEBRAZO", "BRAZO", "PIERNAS");
    private final TablasRula tablas;
    private final MotorRula motor;

    /** RF-027: recibe las mismas tablas cargadas desde MySQL que usa el cálculo normal. */
    public PlanAtencion40(TablasRula tablas) { this.tablas = tablas; this.motor = new MotorRula(tablas); }

    /** RF-018: orden por puntos/máximo; desempate estable, sin sugerir sumas de segmentos como puntaje final. */
    public static List<String> orden(Calculo calculo) {
        var puntos = puntos(calculo);
        return ORDEN.stream().sorted(Comparator.comparingDouble((String k) ->
                (double) puntos.get(k) / METAS.get(k).maximo()).reversed()).toList();
    }

    /** (propuesta) RF-020: color de cada parte; hasta tres por encima del mínimo pesan más. */
    public static String color(Calculo calculo, String clave) {
        if (puntos(calculo).get(clave) == METAS.get(clave).minimo()) return "VERDE";
        return orden(calculo).stream().filter(k -> puntos(calculo).get(k) > METAS.get(k).minimo())
                .limit(3).toList().contains(clave) ? "ROJO" : "AMARILLO";
    }

    /** RF-018: puntos reales, incluido el apoyo de piernas observado, sin duplicar el puntuador. */
    public static Map<String, Integer> puntos(Calculo c) {
        var p = c.resultado().segmentos();
        return Map.of("BRAZO", p.brazo(), "ANTEBRAZO", p.antebrazo(), "MUNECA", p.muneca(),
                "CUELLO", p.cuello(), "TRONCO", p.tronco(), "PIERNAS", p.piernas());
    }

    /** RF-027/RNF-010: cambia solo segmentos del plan; conserva actividad, carga y giro de muñeca independiente. */
    public Calculo proyectar(Calculo c) {
        var actuales = puntos(c);
        var cambios = new HashSet<String>();
        actuales.forEach((k, p) -> { if (p > METAS.get(k).minimo()) cambios.add(k); });
        var e = c.entrada(); var a = e.ajustes();
        var mediciones = new EnumMap<Segmento, MotorRula.Medicion>(Segmento.class);
        mediciones.putAll(e.mediciones());
        // RF-021: el historial conserva grados en medicion_angulo, no en Entrada.mediciones.
        for (var medida : c.mediciones()) mediciones.putIfAbsent(medida.segmento(),
                new MotorRula.Medicion(List.of(), medida.grados()));
        for (String k : cambios) if (!k.equals("PIERNAS"))
            mediciones.put(Segmento.valueOf(k), new MotorRula.Medicion(List.of(), METAS.get(k).grados()));
        var ajustes = new Ajustes(!cambios.contains("BRAZO") && a.hombroElevado(),
            !cambios.contains("BRAZO") && a.brazoSeparado(), !cambios.contains("BRAZO") && a.brazoApoyado(),
            !cambios.contains("ANTEBRAZO") && a.antebrazoCruza(), !cambios.contains("MUNECA") && a.munecaDesviada(),
            a.giro(), !cambios.contains("CUELLO") && a.cuelloGirado(),
            !cambios.contains("CUELLO") && a.cuelloInclinado(), !cambios.contains("TRONCO") && a.troncoGirado(),
            !cambios.contains("TRONCO") && a.troncoInclinado(), cambios.contains("PIERNAS") || a.piernasApoyadas(),
            !cambios.contains("TRONCO") && a.sentadoBienApoyado());
        return motor.calcular(new MotorRula.Entrada(e.lado(), e.ancho(), e.alto(), mediciones, ajustes,
                e.actividad(), e.carga()));
    }

    /** RF-020/023: JSON de presentación y proyección, sin guardar un resultado hipotético. */
    public Map<String, Object> explicar(Calculo c, List<Regla> reglas) {
        var datos = new LinkedHashMap<String, Object>(nivel(c));
        datos.put("lado", c.entrada().lado().name());
        datos.put("actividad", c.resultado().actividad()); datos.put("carga", c.resultado().carga());
        datos.put("proyeccion", nivel(proyectar(c)));
        datos.put("niveles", List.of(1, 3, 5, 7).stream().map(p -> {
            var n = tablas.nivelDe(p);
            return Map.of("min", n.puntajeMinimo(), "max", n.puntajeMaximo(), "nivel", n.nivelOficialRula(),
                    "accion", n.accion(), "color", n.color());
        }).toList());
        datos.put("segmentos", orden(c).stream().map(k -> fila(c, k, reglas)).toList());
        return datos;
    }

    /** RF-020: utiliza nombres y acciones de nivel_riesgo, sin reescribirlos. */
    private static Map<String, Object> nivel(Calculo c) {
        var n = c.resultado().nivel();
        return Map.of("puntaje", c.resultado().puntajeFinal(), "color", n.color(),
                "nivel", n.nivelOficialRula(), "accion", n.accion());
    }

    /** RF-023: conserva todas las reglas aplicables de una parte, sin inventar una regla persistida. */
    private static Map<String, Object> fila(Calculo c, String k, List<Regla> reglas) {
        var meta = METAS.get(k); var puntos = puntos(c).get(k);
        var fila = new LinkedHashMap<String, Object>();
        fila.put("clave", k); fila.put("nombre", meta.nombre()); fila.put("puntos", puntos);
        fila.put("minimo", meta.minimo()); fila.put("maximo", meta.maximo());
        fila.put("peso", (double) puntos / meta.maximo()); fila.put("color", color(c, k));
        fila.put("meta", meta.rango()); fila.put("medido", medido(c, k));
        var aplicables = reglas.stream().filter(r -> r.segmento().equals(k)).toList();
        fila.put("cambio", aplicables.isEmpty() ? meta.cambio() : aplicables.stream().map(Regla::texto)
                .collect(java.util.stream.Collectors.joining(" ")));
        fila.put("reglas", aplicables.stream().map(Regla::id).toList());
        fila.put("acciones", List.of());
        return fila;
    }

    /** RF-017/RNF-007: sentido y observaciones expresados en palabras, sin ocultar los ajustes medidos. */
    private static String medido(Calculo c, String k) {
        if (k.equals("PIERNAS")) return c.entrada().ajustes().piernasApoyadas() ?
                "Pies bien apoyados y peso equilibrado" : "Pies sin buen apoyo o peso desequilibrado";
        var medida = c.mediciones().stream().filter(m -> m.segmento().name().equals(k)).findFirst().orElseThrow();
        double grados = medida.grados();
        String texto = k.equals("ANTEBRAZO") ? "Codo a " + Math.round(grados) + "°" :
                (k.equals("MUNECA") ? "Doblada " : "Inclinado ") + Math.round(Math.abs(grados)) + "° hacia " +
                (grados < 0 ? "atrás" : "adelante");
        var a = c.entrada().ajustes();
        List<String> ajustes = switch (k) {
            case "BRAZO" -> List.of(a.hombroElevado() ? "hombro subido" : "",
                    a.brazoSeparado() ? "brazo separado" : "", a.brazoApoyado() ? "brazo apoyado" : "");
            case "ANTEBRAZO" -> List.of(a.antebrazoCruza() ? "cruza el cuerpo o sale hacia un lado" : "");
            case "MUNECA" -> List.of(a.munecaDesviada() ? "desviada hacia un lado" : "");
            case "CUELLO" -> List.of(a.cuelloGirado() ? "cabeza torcida" : "",
                    a.cuelloInclinado() ? "cabeza inclinada a un lado" : "");
            default -> List.of(a.troncoGirado() ? "espalda torcida" : "",
                    a.troncoInclinado() ? "espalda inclinada a un lado" : "");
        };
        return texto + ajustes.stream().filter(s -> !s.isEmpty()).map(s -> " · " + s)
                .collect(java.util.stream.Collectors.joining());
    }
}
