package co.aplomo.web;

import co.aplomo.rula.PuntuadorSegmentos;
import co.aplomo.rula.MotorRula.Segmento;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Los 18 mosaicos aprobados se contrastan directamente con el motor sin cambiarlo. */
class RangosRecorridoTest {
    @ParameterizedTest(name="{0} {1}° → {2} puntos")
    @CsvSource({"BRAZO,-30,2,brazo_extension.png","BRAZO,0,1,brazo_neutro.png","BRAZO,30,2,brazo_20_45.png","BRAZO,60,3,brazo_45_90.png","BRAZO,110,4,brazo_mas_90.png","ANTEBRAZO,80,1,antebrazo_60_100.png","ANTEBRAZO,40,2,antebrazo_fuera.png","MUNECA,0,1,muneca_neutra.png","MUNECA,10,2,muneca_15.png","MUNECA,25,3,muneca_mas_15.png","CUELLO,5,1,cuello_0_10.png","CUELLO,15,2,cuello_10_20.png","CUELLO,30,3,cuello_mas_20.png","CUELLO,-15,4,cuello_extension.png","TRONCO,0,1,tronco_erguido.png","TRONCO,10,2,tronco_0_20.png","TRONCO,40,3,tronco_20_60.png","TRONCO,75,4,tronco_mas_60.png"})
    void todosLosMosaicosProducenLosPuntosAprobados(Segmento segmento,double grados,int esperado,String imagen){
        int real=switch(segmento){case BRAZO->PuntuadorSegmentos.brazo(grados,false,false,false);case ANTEBRAZO->PuntuadorSegmentos.antebrazo(grados,false);case MUNECA->PuntuadorSegmentos.muneca(grados,false);case CUELLO->PuntuadorSegmentos.cuello(grados,false,false);case TRONCO->PuntuadorSegmentos.tronco(grados,false,false,false);};
        assertEquals(esperado,real);
        var rango=RecorridoCuerpo.RANGOS.get(segmento).stream().filter(r->r.imagen().equals(imagen)).findFirst().orElseThrow();assertEquals(grados,rango.grados());assertEquals(esperado,rango.puntos());assertNotNull(getClass().getResource("/static/img/tour/"+imagen));
    }
}
