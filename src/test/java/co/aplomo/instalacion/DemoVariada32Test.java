package co.aplomo.instalacion;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DemoVariada32Test extends BasePrueba {
    @TempDir Path fotos;
    @Test
    void demoTieneOchoPosturasSinDuplicadosYMejoraEnDosEmpresas() throws Exception {
        Demo.cargar(con,Path.of("docs/base_de_datos/datos_demo.sql"),fotos);
        assertEquals(0,Sql.<Integer>uno(con,"SELECT COUNT(*) FROM (SELECT e.id_visita,e.id_puesto,e.postura,"+
                "a.puntaje_final FROM evaluacion e JOIN fotografia f USING(id_evaluacion) "+
                "JOIN analisis_rula a USING(id_foto) JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) "+
                "WHERE r.codigo LIKE 'D28-%' OR r.codigo LIKE 'V2%' GROUP BY e.id_visita,e.id_puesto,e.postura,"+
                "a.puntaje_final HAVING COUNT(*)>1) q",f->f.getInt(1)));
        assertTrue(Sql.<Integer>uno(con,"SELECT COUNT(DISTINCT postura) FROM evaluacion WHERE postura IN "+
                "('Alcance a la repisa alta','Digitación en pantalla','Levantamiento de caja','Inspección de pieza',"+
                "'Atención al público','Empaque en mesa','Uso del celular','Pesaje en báscula')",f->f.getInt(1))>=8);
        var filas=Sql.listar(con,"SELECT s.id_empresa,MAX(a.puntaje_final) puntos FROM visita v "+
                "JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) "+
                "JOIN evaluacion e USING(id_visita) JOIN fotografia f USING(id_evaluacion) "+
                "JOIN analisis_rula a USING(id_foto) WHERE v.estado='FINALIZADA' AND "+
                "(r.codigo LIKE 'D28-%' OR r.codigo LIKE 'V2%') GROUP BY s.id_empresa,v.id_visita,r.fecha "+
                "ORDER BY r.fecha,v.id_visita",f->new int[]{f.getInt(1),f.getInt(2)});
        Map<Integer,ArrayList<Integer>> empresas=new LinkedHashMap<>();
        for(var fila:filas)empresas.computeIfAbsent(fila[0],k->new ArrayList<>()).add(fila[1]);
        long mejoran=empresas.values().stream().filter(p->p.size()>1&&p.get(0)>p.get(p.size()-1)).count();
        assertTrue(mejoran>=2,"Empresas que mejoran: "+mejoran);
        System.out.println("DEMO32: "+mejoran+" empresas mejoran; 0 duplicados; 8 posturas.");
    }
}
