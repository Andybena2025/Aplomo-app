package co.aplomo.config;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ConfigTest {
 @TempDir Path tmp;
 Map<String,String> entorno(){return new HashMap<>(Map.of("APLOMO_DB_URL","jdbc:mysql://db/prueba","APLOMO_DB_USUARIO","usuario_prueba","APLOMO_DB_CLAVE","clave_ficticia"));}
 Path archivo()throws Exception{Path p=tmp.resolve("db.properties");Files.writeString(p,"db.url=jdbc:mysql://localhost/respaldo\ndb.usuario=local\ndb.clave=solo_prueba\n");return p;}
 @Test void entornoCompletoNoNecesitaArchivo(){var d=Config.leer(entorno(),tmp.resolve("inexistente"));assertEquals("jdbc:mysql://db/prueba",d.url());assertEquals("usuario_prueba",d.usuario());assertEquals("clave_ficticia",d.clave());assertEquals(8080,d.puerto());assertEquals(Path.of("fotos"),d.carpetaFotos());}
 @Test void archivoSigueFuncionandoSinVariables()throws Exception{var d=Config.leer(Map.of(),archivo());assertEquals("jdbc:mysql://localhost/respaldo",d.url());assertEquals("local",d.usuario());assertEquals("solo_prueba",d.clave());}
 @Test void variablesParcialesTienenPrioridadPorCampo()throws Exception{var d=Config.leer(Map.of("APLOMO_DB_USUARIO","entorno"),archivo());assertEquals("entorno",d.usuario());assertEquals("solo_prueba",d.clave());}
 @Test void puertoYCarpetaSonConfigurables(){var e=entorno();e.put("APLOMO_PUERTO","9090");e.put("APLOMO_CARPETA_FOTOS","/datos/fotos");var d=Config.leer(e,tmp.resolve("sin-archivo"));assertEquals(9090,d.puerto());assertEquals(Path.of("/datos/fotos"),d.carpetaFotos());}
 @ParameterizedTest @ValueSource(strings={"0","65536","texto",""}) void puertoInvalidoNoArranca(String puerto){var e=entorno();e.put("APLOMO_PUERTO",puerto);assertThrows(IllegalStateException.class,()->Config.leer(e,tmp.resolve("sin-archivo")));}
 @Test void variableVaciaNoOcultaUnErrorConElArchivo()throws Exception{var e=entorno();e.put("APLOMO_DB_CLAVE","");var fallo=assertThrows(IllegalStateException.class,()->Config.leer(e,archivo()));assertTrue(fallo.getMessage().contains("APLOMO_DB_CLAVE"));assertFalse(fallo.getMessage().contains("clave_ficticia"));}
 @Test void urlDebeSerMySql(){var e=entorno();e.put("APLOMO_DB_URL","jdbc:otro:prueba");assertThrows(IllegalStateException.class,()->Config.leer(e,tmp.resolve("sin-archivo")));}
 @Test void representacionDeConfiguracionNuncaImprimeLaClave(){assertFalse(Config.leer(entorno(),tmp.resolve("sin-archivo")).toString().contains("clave_ficticia"));}
}
