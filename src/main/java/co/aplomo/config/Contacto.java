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
     * Lee UTF-8; respalda cada campo vacío o de ejemplo e impide enlaces con esquemas o consultas inesperados.
     * RNF-011.
     */
    public static Contacto leer(Path archivo) {
        var elemento = new Properties();
        if (Files.exists(archivo)) {
            try (var in = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                elemento.load(in);
            } catch (IOException | IllegalArgumentException error) {
                elemento.clear();
            }
        }
        String nombre = respaldo(texto(elemento, "nombre"), "Laura Gabriela Romero");
        String cargo = respaldo(texto(elemento, "cargo"), "Administradora principal");
        String correo = respaldo(texto(elemento, "correo"), "gabriela.romero@alineo.co");
        String telefono = respaldo(texto(elemento, "telefono"), "+57 300 000 0000");
        String wa = respaldo(texto(elemento, "whatsapp"), "573000000000");
        if (!correo.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) correo = "";
        wa = wa.replaceAll("[+\\s()\\-]", "");
        if (!wa.matches("[1-9][0-9]{6,14}")) wa = "";
        return new Contacto(nombre, cargo, correo, telefono, wa,
                List.of(nombre, cargo, correo, telefono, wa).stream().anyMatch(String::isBlank));
    }

    /**
     * RNF-007: enlace de contacto con mensaje sencillo y parámetros codificados, sin datos de la evaluación.
     */
    public String enlaceWhatsapp() {
        if (whatsapp.isBlank()) return "";
        String saludo = nombre.isBlank() ? "Hola" : "Hola " + nombre.split("\\s+")[0];
        return "https://wa.me/" + whatsapp + "?text=" +
                java.net.URLEncoder.encode(saludo + ", necesito ayuda con ALINEO.", StandardCharsets.UTF_8)
                        .replace("+", "%20");
    }

    /**
     * RNF-007: correo de ayuda con asunto, sin adjuntar datos personales ni enviar mensajes automáticamente.
     */
    public String enlaceCorreo() {
        return correo.isBlank() ? "" : "mailto:" + correo + "?subject=Ayuda%20con%20ALINEO";
    }

    private static String respaldo(String valor, String predeterminado) {
        return valor.isBlank() || List.of("Persona de ejemplo", "Responsable de ejemplo", "correo@ejemplo.com").stream()
                .anyMatch(valor::equalsIgnoreCase) ? predeterminado : valor;
    }

    private static String texto(Properties elemento, String campo) {
        String texto2 = elemento.getProperty("contacto." + campo, "").strip();
        return texto2.length() > 500 ? "" : texto2;
    }
}
