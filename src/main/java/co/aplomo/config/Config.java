package co.aplomo.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lee la configuracion de conexion a MySQL desde config/db.properties.
 * Nunca guarda contrasenas en el codigo (RNF-004).
 */
public class Config {

    private Config() {}

    private static final String RUTA = "config/db.properties";
    private static final Properties props = new Properties();

    static {
        try (Reader entrada = Files.newBufferedReader(Path.of(RUTA), StandardCharsets.UTF_8)) {
            props.load(entrada);
            for (String campo : new String[]{"db.url", "db.usuario", "db.clave"}) {
                if (props.getProperty(campo) == null || props.getProperty(campo).isBlank()) {
                    throw new IllegalStateException("Falta configurar " + campo + " en " + RUTA + ".");
                }
            }
            if (!props.getProperty("db.url").startsWith("jdbc:mysql://")) {
                throw new IllegalStateException("Configura una URL JDBC de MySQL en " + RUTA + ".");
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                "No se pudo leer " + RUTA + ". Copia db.properties.ejemplo como db.properties.", e);
        }
    }

    /** Devuelve la URL JDBC (db.url). */
    public static String url() {
        return props.getProperty("db.url");
    }

    /** Devuelve el usuario de MySQL (db.usuario). */
    public static String usuario() {
        return props.getProperty("db.usuario");
    }

    /** Devuelve la clave de MySQL (db.clave). */
    public static String clave() {
        return props.getProperty("db.clave");
    }
}
