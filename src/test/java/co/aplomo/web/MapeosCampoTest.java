package co.aplomo.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapeosCampoTest {
    @ParameterizedTest
    @CsvSource({"MENOS_2,60", "DE_2_A_4,180", "DE_4_A_6,300", "DE_6_A_8,420", "MAS_8,540"})
    void horasConservanLosCincoValoresDelEncargo(String banda, int minutos) {
        assertEquals(minutos, MapeosCampo.MINUTOS.get(banda));
    }

    @ParameterizedTest
    @CsvSource({"RECTA,5", "POCO,8", "MUCHO,25"})
    void munecaUsaAngulosExistentesYLaToleranciaDeCincoGrados(String respuesta, double grados) {
        assertEquals(grados, MapeosCampo.MUNECA.get(respuesta));
    }

    @ParameterizedTest
    @CsvSource({"EXIGENTE,MAS_INADECUADA", "DURADERA,MAS_INADECUADA", "NO_SABE,MAYOR_EXPOSICION"})
    void soloNoSabeMapeaALaPosturaRepetida(String momento, String criterio) {
        assertEquals(criterio, MapeosCampo.CRITERIOS.get(momento));
    }

    @ParameterizedTest
    @CsvSource({"NADA,NINGUNA,MENOS_2KG_INTERMITENTE", "NADA,ESTATICA,MENOS_2KG_INTERMITENTE",
            "NADA,REPETITIVA," + "MENOS_2KG_INTERMITENTE", "CELULAR,NINGUNA,MENOS_2KG_INTERMITENTE",
            "CELULAR,ESTATICA," + "MENOS_2KG_INTERMITENTE", "CELULAR,REPETITIVA,MENOS_2KG_INTERMITENTE",
            "GALON,NINGUNA," + "DE_2_A_10KG_INTERMITENTE", "GALON,ESTATICA,DE_2_A_10KG_ESTATICA_O_REPETITIVA",
            "GALON,REPETITIVA," + "DE_2_A_10KG_ESTATICA_O_REPETITIVA", "BULTO,NINGUNA,MAS_10KG_INTERMITENTE",
            "BULTO,ESTATICA," + "MAS_10KG_ESTATICA_O_REPETITIVA", "BULTO,REPETITIVA,MAS_10KG_ESTATICA_O_REPETITIVA"})
    void pesoYFrecuenciaRespetanTablaDieciseis(String peso, String actividad, String esperado) {
        assertEquals(esperado, MapeosCampo.carga(peso, actividad, false).name());
        assertEquals("GOLPES_O_FUERZAS_BRUSCAS", MapeosCampo.carga(peso, actividad, true).name());
    }

    @ParameterizedTest
    @CsvSource({"si,GIRADA,true,false", "si,INCLINADA,false,true", "si,AMBAS,true,true", "no,GIRADA,false,false",
            "no,INCLINADA,false,false", "no,AMBAS,false,false"})
    void noSumaDosAjustesPorUnSoloCambio(String respuesta, String detalle, boolean giro, boolean inclinacion) {
        assertArrayEquals(new boolean[]{giro, inclinacion}, MapeosCampo.orientacion(respuesta, detalle));
    }

    @Test
    void noInventaRespuestasAusentes() {
        assertThrows(IllegalArgumentException.class, () -> MapeosCampo.carga("", "NINGUNA", false));
        assertThrows(IllegalArgumentException.class, () -> MapeosCampo.carga("GALON", "", true));
        assertThrows(IllegalArgumentException.class, () -> MapeosCampo.orientacion("si", ""));
    }
}
