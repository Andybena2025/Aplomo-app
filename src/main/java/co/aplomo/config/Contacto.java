package co.aplomo.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Paso 25: datos de contacto opcionales, locales y privados. Nunca escribe ni registra su contenido. RNF-011.
 */
public record Contacto(String nombre, String cargo, String correo, String telefono, String whatsapp,
                       boolean incompleto) {

    /**
     * Lee UTF-8; omite campos ausentes e impide enlaces con esquemas o consultas inesperados. RNF-011.
     */
    public static Contacto leer(Path archivo) {
        var elemento = new Properties();
        try (var in = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            elemento.load(in);
        } catch (IOException | IllegalArgumentException error) {
            return new Contacto("", "", "", "", "", true);
        }
        String nombre = texto(elemento, "nombre"), cargo = texto(elemento, "cargo"), correo = texto(elemento,
                "empresa.correo").isBlank() ? texto(elemento, "correo") : texto(elemento,
                "empresa.correo"), telefono = texto(elemento, "empresa.telefono").isBlank() ? texto(elemento,
                "telefono") : texto(elemento, "empresa.telefono"), wa = texto(elemento, "whatsapp");
        if (!correo.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) correo = "";
        wa = wa.replaceAll("[+\\s()\\-]", "");
        if (!wa.matches("[1-9][0-9]{6,14}")) wa = "";
        return new Contacto(nombre, cargo, correo, telefono, wa,
                List.of(nombre, cargo, correo, telefono, wa).stream().anyMatch(String::isBlank));
    }

    private static String texto(Properties elemento, String campo) {
        String texto2 = elemento.getProperty("contacto." + campo, "").strip();
        return texto2.length() > 500 ? "" : texto2;
    }
}
