package co.aplomo.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaleriaDemoTest {
    String previa;

    @BeforeEach
    void activar() {
        previa = System.getProperty(GaleriaDemo.BANDERA);
        System.setProperty(GaleriaDemo.BANDERA, "true");
    }

    @AfterEach
    void restaurar() {
        if (previa == null) System.clearProperty(GaleriaDemo.BANDERA);
        else System.setProperty(GaleriaDemo.BANDERA, previa);
    }

    @Test
    void cadaArchivoExisteYEsUnaImagenValidaDeMenosDeCincoMb() throws Exception {
        var archivos = new HashSet<String>();
        long total = 0;
        for (var elemento : GaleriaDemo.FOTOS)
            if (archivos.add(elemento.archivo())) {
                try (var in = getClass().getResourceAsStream("/static/img/galeria/" + elemento.archivo())) {
                    assertNotNull(in, elemento.archivo());
                    byte[] elemento2 = in.readAllBytes();
                    assertTrue(elemento2.length > 0 && elemento2.length <= 5 * 1024 * 1024);
                    assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(elemento2)));
                    total += elemento2.length;
                }
            }
        assertEquals(42, archivos.size());
        assertTrue(total < 25 * 1024 * 1024);
    }

    @Test
    void losQuinceOriginalesConservanExactamenteSusBytes() throws Exception {
        for (var elemento : GaleriaDemo.FOTOS)
            if (elemento.archivo().matches("S[2-6]_.*_(perfil|frente|detalle)\\.jpg"))
                assertArrayEquals(Files.readAllBytes(Path.of("fotos_de_prueba/galeria", elemento.archivo())),
                        Files.readAllBytes(Path.of("src/main/resources/static/img/galeria", elemento.archivo())));
    }

    @ParameterizedTest
    @CsvSource({"OFICINA,DERECHA", "OFICINA,IZQUIERDA", "PLANTA,DERECHA", "PLANTA,IZQUIERDA"})
    void cadaContextoYLadoTienenAlMenosTresPerfilesBuenos(String contexto, String mira) {
        assertTrue(GaleriaDemo.FOTOS.stream()
                .filter(elemento -> elemento.contexto().equals(contexto) && elemento.vista().equals("PERFIL") &&
                        elemento.mira().equals(mira) && elemento.tipo().equals("BUENA")).count() >= 3);
    }

    @ParameterizedTest
    @CsvSource({"OFICINA", "PLANTA"})
    void cadaContextoTieneFrenteDetalleYErroresParaRotar(String contexto) {
        for (String vista : List.of("FRENTE", "DETALLE"))
            assertTrue(GaleriaDemo.FOTOS.stream().anyMatch(
                    elemento -> elemento.contexto().equals(contexto) && elemento.vista().equals(vista) &&
                            elemento.tipo().equals("BUENA")));
        assertEquals(contexto.equals("PLANTA") ? 4 : 3, GaleriaDemo.FOTOS.stream()
                .filter(elemento2 -> elemento2.contexto().equals(contexto) && elemento2.tipo().equals("ERROR") &&
                        elemento2.rotacion()).count());
    }

    @Test
    void piernasTapadasNoSeOfreceComoPerfilBueno() {
        var elemento = GaleriaDemo.FOTOS.stream()
                .filter(elemento2 -> elemento2.archivo().equals("oficina_sentado_inclinado_perfil_izquierda.jpg"))
                .findFirst().orElseThrow();
        assertEquals("ERROR", elemento.tipo());
        assertEquals("IZQUIERDA", elemento.mira());
        assertTrue(elemento.titulo().contains("usa Calcular manualmente"));
        assertTrue(elemento.aviso().contains("solo una parte"));
    }

    @Test
    void espejoS5SeOfreceSoloComoErrorRotativoYQuedanOchoPerfilesIzquierdosBuenos() {
        String archivo = "S5_inclinada_hacia_el_carro_perfil_izq.jpg";
        var elemento = GaleriaDemo.FOTOS.stream().filter(elemento2 -> elemento2.archivo().equals(archivo)).findFirst()
                .orElseThrow();
        assertEquals("ERROR", elemento.tipo());
        assertEquals("PLANTA", elemento.contexto());
        assertEquals("PERFIL", elemento.vista());
        assertEquals("IZQUIERDA", elemento.mira());
        assertTrue(elemento.rotacion());
        assertEquals("La IA no la reconoce bien: usa Calcular manualmente", elemento.titulo());
        assertTrue(elemento.aviso().startsWith("Parece una foto de frente o de espaldas"));
        assertFalse(GaleriaDemo.FOTOS.stream()
                .anyMatch(elemento3 -> elemento3.archivo().equals(archivo) && elemento3.tipo().equals("BUENA")));
        assertEquals(8, GaleriaDemo.FOTOS.stream()
                .filter(elemento4 -> elemento4.contexto().equals("PLANTA") && elemento4.vista().equals("PERFIL") &&
                        elemento4.mira().equals("IZQUIERDA") && elemento4.tipo().equals("BUENA")).count());
        String texto = GaleriaDemo.selector("PERFIL");
        assertTrue(texto.contains(archivo));
        assertTrue(texto.contains("Ejemplo con error: sirve para ver el aviso"));
    }

    @ParameterizedTest
    @CsvSource({"PERFIL,PERFIL", "FRENTE,FRENTE", "OTRA,DETALLE"})
    void selectorIncluyeSoloSuRanuraYMetadatosParaFiltrar(String vista, String ranura) {
        String texto = GaleriaDemo.selector(vista);
        assertTrue(texto.contains("data-galeria-vista=\"" + ranura + "\""));
        for (var elemento : GaleriaDemo.FOTOS)
            if (elemento.vista().equals(ranura))
                assertTrue(texto.contains("data-usar-ejemplo=\"/static/img/galeria/" + elemento.archivo() + "\""));
        assertEquals(GaleriaDemo.FOTOS.stream().filter(elemento2 -> elemento2.vista().equals(ranura)).count(),
                texto.split("data-usar-ejemplo=", -1).length - 1);
    }

    @Test
    void unaBanderaQuitaTodosLosEnlaces() {
        System.setProperty(GaleriaDemo.BANDERA, "false");
        for (String texto : List.of("PERFIL", "FRENTE", "OTRA")) assertEquals("", GaleriaDemo.selector(texto));
    }

    @Test
    void noInventaUnaVista() {
        assertThrows(IllegalArgumentException.class, () -> GaleriaDemo.selector("PDF"));
    }
}
