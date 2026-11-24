package co.aplomo.web;

import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

/** RNF-004/007: serialización limitada a datos de presentación, sin reflexión ni dependencias. */
final class Json40 {
    private Json40() { }

    /** RNF-004: escapa también HTML para incrustar JSON inerte sin cerrar su script. */
    static String escribir(Object dato) {
        if (dato == null) return "null";
        if (dato instanceof Number || dato instanceof Boolean) return dato.toString();
        if (dato instanceof Map<?, ?> mapa) return mapa.entrySet().stream()
                .map(e -> escribir(e.getKey().toString()) + ":" + escribir(e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
        if (dato instanceof Collection<?> lista) return lista.stream().map(Json40::escribir)
                .collect(Collectors.joining(",", "[", "]"));
        return NotasWeb.texto(dato.toString()).replace("<", "\\u003c").replace(">", "\\u003e")
                .replace("&", "\\u0026").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }
}
