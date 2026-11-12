package co.aplomo.web;

import co.aplomo.soporte.*;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** RNF-003: 20 repeticiones HTTP por acción con 1.000 filas en las 16 tablas operativas, siempre ROLLBACK. */
class HttpRendimientoTest extends HttpPrueba {
    @Test void listadosGuardadoEInformeCumplenCincoSegundosEnPercentil95()throws Exception{
        var tamanos=CargaMilRegistros.preparar(this);assertTrue(tamanos.values().stream().allMatch(n->n==1000));entrar(admin);
        String token=csrf(get("/empresas/nueva"));List<Double> listados=new ArrayList<>(),guardados=new ArrayList<>(),informes=new ArrayList<>();
        for(int n=0;n<20;n++){
            long inicio=System.nanoTime();var lista=get("/evaluaciones");listados.add(ms(inicio));assertEquals(200,lista.statusCode());assertTrue(lista.body().contains("1000 evaluaciones"));
            var punto=con.setSavepoint();inicio=System.nanoTime();
            var guardado=post("/empresas",Map.of("csrf",token,"nombre","Rendimiento "+n,"nit","998877"+n,"descripcion","Carga reversible","telefono","123","correo","qa@prueba.invalid","persona_contacto","QA"));
            guardados.add(ms(inicio));assertEquals(303,guardado.statusCode(),guardado.body());con.rollback(punto);
            inicio=System.nanoTime();var informe=get("/informes/empresa/"+empresa);informes.add(ms(inicio));assertEquals(200,informe.statusCode(),informe.body().substring(0,Math.min(300,informe.body().length())));
        }
        String resultado="{\"zona\":\"America/Bogota\",\"base\":\"rula_ergonomia_pruebas\",\"repeticiones_por_accion\":20,\"registros_por_tabla\":1000,\"tablas\":"+tamanos.size()+",\"listados_ms\":"+listados+",\"guardados_ms\":"+guardados+",\"informes_ms\":"+informes+",\"p95_listados\":"+p95(listados)+",\"p95_guardados\":"+p95(guardados)+",\"p95_informes\":"+p95(informes)+"}";
        System.out.println("RNF-003: "+resultado);String destino=System.getProperty("aplomo.evidencia.rendimiento");if(destino!=null)Files.writeString(Path.of(destino),resultado);
        assertTrue(p95(listados)<=5000,"P95 del listado supera 5 segundos");assertTrue(p95(guardados)<=5000,"P95 de guardar supera 5 segundos");assertTrue(p95(informes)<=5000,"P95 del informe supera 5 segundos");
    }
    private static double ms(long inicio){return (System.nanoTime()-inicio)/1_000_000.0;}
    private static double p95(List<Double> tiempos){return tiempos.stream().sorted().toList().get(18);}
}
