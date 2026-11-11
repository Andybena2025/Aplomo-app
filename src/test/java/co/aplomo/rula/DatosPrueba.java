package co.aplomo.rula;

import co.aplomo.db.Conexion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** Solo pruebas: lee el SQL aprobado como oráculo; nunca lo ejecuta (contiene DROP DATABASE). */
final class DatosPrueba {
    record Celda(String tabla, int fila, int columna, int valor) {}

    static List<Celda> celdas() throws Exception {
        String sql = Files.readString(Path.of("docs/base_de_datos/Script_MySQL_RULA.sql"));
        var patron = Pattern.compile("\\('([ABC])', (\\d+), (\\d+), (\\d+)\\)");
        var matcher = patron.matcher(sql);
        List<Celda> datos = new ArrayList<>();
        while (matcher.find()) {
            datos.add(new Celda(matcher.group(1), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))));
        }
        if (datos.size() != 272) throw new IllegalStateException("Se esperaban 272 celdas en el SQL aprobado.");
        return datos;
    }

    static TablasRula tablasMySQL() throws Exception {
        var tablas = new TablasRula();
        try (var con = Conexion.obtener()) { tablas.cargar(con); }
        return tablas;
    }

    static int consultar(TablasRula tablas, Celda c) {
        return switch (c.tabla()) {
            case "A" -> tablas.buscarA((c.fila() - 1) / 3 + 1, (c.fila() - 1) % 3 + 1,
                    (c.columna() - 1) / 2 + 1, (c.columna() - 1) % 2 + 1);
            case "B" -> tablas.buscarB(c.fila(), (c.columna() - 1) / 2 + 1, (c.columna() - 1) % 2 + 1);
            case "C" -> tablas.buscarC(c.fila(), c.columna());
            default -> throw new IllegalArgumentException();
        };
    }
}
