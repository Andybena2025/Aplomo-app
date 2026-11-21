package co.aplomo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @TempDir
    Path tmp;

    Map<String, String> entorno() {
        return new HashMap<>(Map.of("APLOMO_DB_URL", "jdbc:mysql://db/prueba", "APLOMO_DB_USUARIO", "usuario_prueba",
                "APLOMO_DB_CLAVE", "clave_ficticia"));
    }

    Path archivo() throws Exception {
        Path archivo2 = tmp.resolve("db.properties");
        Files.writeString(archivo2, "db.url=jdbc:mysql://localhost/respaldo\ndb.usuario=local\ndb.clave=solo_prueba\n");
        return archivo2;
    }

    @Test
    void entornoCompletoNoNecesitaArchivo() {
        var elemento = Config.leer(entorno(), tmp.resolve("inexistente"));
        assertEquals("jdbc:mysql://db/prueba", elemento.url());
        assertEquals("usuario_prueba", elemento.usuario());
        assertEquals("clave_ficticia", elemento.clave());
        assertEquals(8080, elemento.puerto());
        assertEquals(Path.of("fotos"), elemento.carpetaFotos());
    }

    @Test
    void archivoSigueFuncionandoSinVariables() throws Exception {
        var elemento = Config.leer(Map.of(), archivo());
        assertEquals("jdbc:mysql://localhost/respaldo", elemento.url());
        assertEquals("local", elemento.usuario());
        assertEquals("solo_prueba", elemento.clave());
    }

    @Test
    void variablesParcialesTienenPrioridadPorCampo() throws Exception {
        var elemento = Config.leer(Map.of("APLOMO_DB_USUARIO", "entorno"), archivo());
        assertEquals("entorno", elemento.usuario());
        assertEquals("solo_prueba", elemento.clave());
    }

    @Test
    void puertoYCarpetaSonConfigurables() {
        var parametros = entorno();
        parametros.put("APLOMO_PUERTO", "9090");
        parametros.put("APLOMO_CARPETA_FOTOS", "/datos/fotos");
        var elemento = Config.leer(parametros, tmp.resolve("sin-archivo"));
        assertEquals(9090, elemento.puerto());
        assertEquals(Path.of("/datos/fotos"), elemento.carpetaFotos());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "65536", "texto", ""})
    void puertoInvalidoNoArranca(String puerto) {
        var parametros = entorno();
        parametros.put("APLOMO_PUERTO", puerto);
        assertThrows(IllegalStateException.class, () -> Config.leer(parametros, tmp.resolve("sin-archivo")));
    }

    @Test
    void puertoDeHostingTienePrioridadSobreElValorDeLaImagen() {
        var parametros = entorno();
        parametros.put("PORT", "18765");
        parametros.put("APLOMO_PUERTO", "8080");
        assertEquals(18765, Config.leer(parametros, tmp.resolve("sin-archivo")).puerto());
        parametros.remove("APLOMO_PUERTO");
        assertEquals(18765, Config.leer(parametros, tmp.resolve("sin-archivo")).puerto());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "65536", "texto", ""})
    void puertoInvalidoDeHostingNoSeIgnora(String puerto) {
        var parametros = entorno();
        parametros.put("PORT", puerto);
        parametros.put("APLOMO_PUERTO", "8080");
        assertThrows(IllegalStateException.class, () -> Config.leer(parametros, tmp.resolve("sin-archivo")));
    }

    @Test
    void variableVaciaNoOcultaUnErrorConElArchivo() throws Exception {
        var parametros = entorno();
        parametros.put("APLOMO_DB_CLAVE", "");
        var fallo = assertThrows(IllegalStateException.class, () -> Config.leer(parametros, archivo()));
        assertTrue(fallo.getMessage().contains("APLOMO_DB_CLAVE"));
        assertFalse(fallo.getMessage().contains("clave_ficticia"));
    }

    @Test
    void urlDebeSerMySql() {
        var parametros = entorno();
        parametros.put("APLOMO_DB_URL", "jdbc:otro:prueba");
        assertThrows(IllegalStateException.class, () -> Config.leer(parametros, tmp.resolve("sin-archivo")));
    }

    @Test
    void representacionDeConfiguracionNuncaImprimeLaClave() {
        assertFalse(Config.leer(entorno(), tmp.resolve("sin-archivo")).toString().contains("clave_ficticia"));
    }
}
