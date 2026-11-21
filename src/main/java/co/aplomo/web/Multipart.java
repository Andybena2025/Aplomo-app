package co.aplomo.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RF-016, RNF-004: lectura multipart acotada, sin confiar en nombres, extensión ni tipo declarados.
 */
public final class Multipart {
    private Multipart() {
    }

    /**
     * Parte recibida; solo el servicio de fotos decide si los bytes son una imagen. RNF-011.
     */
    public record Parte(String nombre, String archivo, byte[] bytes) {
    }

    public static final int LIMITE = 56 * 1024 * 1024;

    /**
     * Lee hasta diez imágenes más campos; rechaza formatos, cabeceras y peticiones excesivos. RNF-011.
     */
    public static List<Parte> leer(InputStream entrada, String tipo) throws IOException {
        if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("multipart/form-data;")) throw error();
        Matcher elemento = Pattern.compile("(?:^|;)\\s*boundary=(?:\"([^\"]+)\"|([^;\\s]+))").matcher(tipo);
        if (!elemento.find()) throw error();
        String boundary = elemento.group(1) != null ? elemento.group(1) : elemento.group(2);
        if (!boundary.matches("[A-Za-z0-9'()+_,./:=?-]{1,70}")) throw error();
        byte[] body = entrada.readNBytes(LIMITE + 1);
        if (body.length > LIMITE)
            throw new IllegalArgumentException("El envío supera el tamaño permitido: hasta 10 fotos de 5 MB.");
        byte[] marca = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        byte[] separador = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);
        List<Parte> partes = new ArrayList<>();
        Set<String> nombres = new HashSet<>();
        int pos = 0;
        if (!empieza(body, pos, marca)) throw error();
        pos += marca.length;
        while (true) {
            if (empieza(body, pos, new byte[]{45, 45})) break;
            if (!empieza(body, pos, new byte[]{13, 10})) throw error();
            int finCabecera = indice(body, new byte[]{13, 10, 13, 10}, pos + 2);
            if (finCabecera < 0 || finCabecera - pos > 8192) throw error();
            String cabecera = new String(body, pos + 2, finCabecera - pos - 2, StandardCharsets.ISO_8859_1);
            Matcher elemento2 = Pattern.compile("(?im)^Content-Disposition: form-data;([^\\r\\n]+)$").matcher(cabecera);
            if (!elemento2.find()) throw error();
            Matcher elemento3 = Pattern.compile("(?:^|;)\\s*name=\"([A-Za-z0-9_]+)\"").matcher(elemento2.group(1));
            if (!elemento3.find()) throw error();
            String nombre = elemento3.group(1);
            Matcher elemento4 = Pattern.compile("(?:^|;)\\s*filename=\"([^\"]*)\"").matcher(elemento2.group(1));
            String archivo = elemento4.find() ? elemento4.group(1) : null;
            if (!nombres.add(nombre) || partes.size() >= 80) throw error();
            int inicio = finCabecera + 4, fin = indice(body, separador, inicio);
            if (fin < 0) throw error();
            if (archivo == null && fin - inicio > 16384)
                throw new IllegalArgumentException("Un campo del formulario es demasiado largo.");
            partes.add(new Parte(nombre, archivo, Arrays.copyOfRange(body, inicio, fin)));
            pos = fin + separador.length;
        }
        return List.copyOf(partes);
    }

    private static int indice(byte[] texto, byte[] patron, int inicio) {
        for (int i = inicio; i <= texto.length - patron.length; i++) if (empieza(texto, i, patron)) return i;
        return -1;
    }

    private static boolean empieza(byte[] texto, int i, byte[] patron) {
        if (i < 0 || i + patron.length > texto.length) return false;
        for (int j = 0; j < patron.length; j++) if (texto[i + j] != patron[j]) return false;
        return true;
    }

    private static IllegalArgumentException error() {
        return new IllegalArgumentException(
                "No pudimos leer el formulario de fotos. Vuelve a enviarlo desde la página.");
    }
}
