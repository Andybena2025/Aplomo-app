package co.aplomo.web;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
class FasesEvaluacionTest {
 @ParameterizedTest @CsvSource({"0,1","1,1","2,2","3,3","4,4","5,5","6,6","7,7","8,8","9,9","10,9","11,9"})
 void fasesSonEstablesSinDependerDeFotosOSegmentos(int paso,int fase){assertEquals(fase,PaginasEvaluacion.fase(paso));assertEquals("<section data-paso=\""+paso+"\" data-fase=\""+fase+"\"></section>",PaginasEvaluacion.fases("<section data-paso=\""+paso+"\"></section>"));}
 @Test void lasSieteFasesNoDependenDelNumeroDeFotosNiDelLado(){String html=PaginasEvaluacion.fases("<section data-paso=\"5\"></section><section data-paso=\"6\"></section><section data-paso=\"7\"></section>");for(int paso:new int[]{5,6,7})assertTrue(html.contains("data-paso=\""+paso+"\" data-fase=\""+paso+"\""));assertFalse(html.contains("data-fase=\"8\""));}
}
