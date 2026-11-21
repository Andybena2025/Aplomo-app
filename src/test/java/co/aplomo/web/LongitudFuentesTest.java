package co.aplomo.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-011: evita que vuelvan las líneas Java difíciles de revisar.
 */
class LongitudFuentesTest {
    @Test
    void fuentesPrincipalesRespetanCientoVeinteColumnas() throws Exception {
        List<String> incumplimientos = new ArrayList<>();
        try (var archivos = Files.walk(Path.of("src/main/java"))) {
            for (Path archivo : archivos.filter(ruta -> ruta.toString().endsWith(".java")).toList()) {
                // Exclusión aprobada por Andy: código protegido, RNF-010; no se reformatea el motor RULA.
                if (archivo.toString().replace('\\', '/').contains("/co/aplomo/rula/")) continue;
                revisar(archivo, incumplimientos);
            }
        }
        assertTrue(incumplimientos.isEmpty(), String.join("\n", incumplimientos));
    }

    private static void revisar(Path archivo, List<String> incumplimientos) throws Exception {
        List<String> lineas = Files.readAllLines(archivo);
        boolean calculoProtegido = false;
        int marcas = 0;
        for (int indice = 0; indice < lineas.size(); indice++) {
            String linea = lineas.get(indice);
            // Exclusión aprobada por Andy: código protegido, RNF-010; solo este POST conserva su formato.
            if (archivo.endsWith("EvaluacionesWeb.java") && linea.contains("RNF-010: inicio del cálculo protegido")) {
                calculoProtegido = true;
                marcas++;
            }
            if (!calculoProtegido && linea.length() > 120) {
                incumplimientos.add(archivo + ":" + (indice + 1) + " · " + linea.length() + " columnas");
            }
            if (archivo.endsWith("EvaluacionesWeb.java") && linea.contains("RNF-010: fin del cálculo protegido")) {
                calculoProtegido = false;
                marcas++;
            }
        }
        if (archivo.endsWith("EvaluacionesWeb.java")) {
            assertEquals(2, marcas, "La exclusión debe delimitar únicamente el bloque protegido.");
            assertTrue(!calculoProtegido, "La exclusión no puede extenderse al resto del archivo.");
        }
    }
}
