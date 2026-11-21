package co.aplomo.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Configuración local o por entorno; nunca imprime claves. RNF-011.
 */
public final class Config {
    private Config() {
    }

    /**
     * RNF-011: componente de la aplicación.
     */
    public record Datos(String url, String usuario, String clave, int puerto, Path carpetaFotos) {
        /**
         * RNF-011: operación del componente.
         */
        @Override
        public String toString() {
            return "Configuración de ALINEO (clave oculta)";
        }
    }

    private static final class Actual {
        static final Datos VALORES = leer(System.getenv(), Path.of("config/db.properties"));
    }

    /**
     * Env por campo; abre el archivo solo cuando falta alguna variable de conexión.
     */
    static Datos leer(Map<String, String> entorno, Path archivo) {
        Properties elemento = new Properties();
        if (!entorno.keySet().containsAll(Set.of("APLOMO_DB_URL", "APLOMO_DB_USUARIO", "APLOMO_DB_CLAVE"))) {
            try (Reader elemento2 = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                elemento.load(elemento2);
            } catch (IOException error) {
                throw new IllegalStateException("No se pudo leer " + archivo +
                        ". Configura las variables APLOMO_DB o copia db.properties.ejemplo como db.pr" + "operties.",
                        error);
            }
        }
        String url = valor(entorno, elemento, "APLOMO_DB_URL", "db.url"), usuario = valor(entorno, elemento,
                "APLOMO_DB_USUARIO", "db.usuario"), clave = valor(entorno, elemento, "APLOMO_DB_CLAVE", "db.clave");
        if (!url.startsWith("jdbc:mysql://")) throw new IllegalStateException("Configura una URL JDBC de MySQL.");
        int puerto = puerto(entorno.getOrDefault("PORT", entorno.getOrDefault("APLOMO_PUERTO", "8080")));
        String carpeta = entorno.getOrDefault("APLOMO_CARPETA_FOTOS", "fotos");
        if (carpeta.isBlank()) throw new IllegalStateException("Configura APLOMO_CARPETA_FOTOS.");
        return new Datos(url, usuario, clave, puerto, Path.of(carpeta));
    }

    private static String valor(Map<String, String> parametros, Properties elemento, String variable,
            String propiedad) {
        String texto2 = parametros.containsKey(variable) ? parametros.get(variable) : elemento.getProperty(propiedad);
        if (texto2 == null || texto2.isBlank())
            throw new IllegalStateException("Falta configurar " + variable + " o " + propiedad + ".");
        return texto2;
    }

    private static int puerto(String valor) {
        try {
            int cantidad = Integer.parseInt(valor);
            if (cantidad >= 1 && cantidad <= 65535) return cantidad;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalStateException("El puerto de ALINEO debe estar entre 1 y 65535.");
    }

    /**
     * Se conserva la sustitución de las pruebas para no usar la base real. RNF-011.
     */
    public static String url() {
        return System.getProperty("aplomo.db.url", Actual.VALORES.url());
    }

    /**
     * RNF-011: operación del componente.
     */
    public static String usuario() {
        return Actual.VALORES.usuario();
    }

    /**
     * RNF-011: operación del componente.
     */
    public static String clave() {
        return Actual.VALORES.clave();
    }

    /**
     * RNF-011: operación del componente.
     */
    public static int puerto() {
        return System.getProperty("aplomo.puerto") == null ? Actual.VALORES.puerto() : puerto(
                System.getProperty("aplomo.puerto"));
    }

    /**
     * RNF-011: operación del componente.
     */
    public static Path carpetaFotos() {
        return System.getProperty("aplomo.fotos") == null ? Actual.VALORES.carpetaFotos() : Path.of(
                System.getProperty("aplomo.fotos"));
    }
}
