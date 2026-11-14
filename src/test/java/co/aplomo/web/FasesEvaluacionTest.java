package co.aplomo.web;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
class FasesEvaluacionTest {
 @ParameterizedTest @CsvSource({"0,1","1,1","2,2","3,3","4,4","5,5","6,6","7,7","8,8","9,8","10,8","11,9"})
 void fasesSonEstablesSinDependerDeFotosOSegmentos(int paso,int fase){assertEquals(fase,PaginasEvaluacion.fase(paso));assertEquals("<section data-paso=\""+paso+"\" data-fase=\""+fase+"\"></section>",PaginasEvaluacion.fases("<section data-paso=\""+paso+"\"></section>"));}
 @Test void esfuerzoYCargaPertenecenACuerpoYResultadoAResumen(){String html=PaginasEvaluacion.fases(RecorridoCuerpo.pasos());for(int paso:new int[]{8,9,10})assertTrue(html.contains("data-paso=\""+paso+"\" data-fase=\"8\""));assertTrue(html.contains("data-paso=\"11\" data-fase=\"9\""));assertTrue(html.contains("Ver cómo decidir"));assertFalse(html.contains("<details open"));}
}
