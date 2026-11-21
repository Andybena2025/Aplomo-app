package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.rula.MotorRula;
import co.aplomo.rula.TablasRula;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.DatosDemoPrueba;
import co.aplomo.soporte.HttpPrueba;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El SQL opcional se prueba completo dos veces y con ediciones, siempre ROLLBACK.
 */
class HttpDatosDemoTest extends HttpPrueba {
    private Map<String, Integer> cantidades() throws Exception {
        Map<String, Integer> parametros = new TreeMap<>();
        for (String tabla : List.of("empresa", "sede", "salon", "area", "cargo", "puesto_trabajo", "reserva", "visita",
                "evaluacion", "condiciones_puesto", "fotografia", "analisis_rula", "medicion_angulo",
                "pendiente_arreglo", "sugerencia", "bitacora"))
            parametros.put(tabla, Sql.uno(con, "SELECT COUNT(*) FROM " + tabla, fila2 -> fila2.getInt(1)));
        return parametros;
    }

    @Test
    void demoEsIdempotenteConservaEdicionesYCubreLosTresColoresEnElInforme() throws Exception {
        DatosDemoPrueba.cargar(con, carpetaFotos);
        var primera = cantidades();
        DatosDemoPrueba.cargar(con, carpetaFotos);
        assertEquals(primera, cantidades());
        int universidad = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", fila2 -> fila2.getInt(1),
                "012345");
        assertEquals(3, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM sede WHERE id_empresa=?", fila3 -> fila3.getInt(1),
                universidad));
        assertEquals(7, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM empresa WHERE nit IN ('900.512.384-1','901.638.205-4','012345','900.782.156-2'," +
                        "'901.428.603-7','900.315.928-5','901.267.840-3')",
                fila4 -> fila4.getInt(1)));
        var filas = Sql.listar(con, "SELECT a.id_analisis,a.puntaje_a,a.puntaje_b,a.puntaje_final FROM analisis_r" +
                        "ula a JOIN fotografia f USING(id_foto) JOIN evaluacion e USING(id_evaluacion" +
                        ") JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) WHERE r.co" +
                        "digo=? ORDER BY a.puntaje_final",
                fila5 -> new int[]{fila5.getInt(1), fila5.getInt(2), fila5.getInt(3), fila5.getInt(4)}, "DEMO-17-E3");
        assertEquals(6, filas.size());
        var tablas = new TablasRula();
        tablas.cargar(con);
        var motor = new MotorRula(tablas);
        int[] casos = {4, 2, 8};
        for (int i = 0; i < 6; i++) {
            var calculo = motor.calcular(CasosRulaPrueba.todos().get(casos[i / 2] - 1).entrada());
            var esperado = calculo.resultado();
            var fila = filas.get(i);
            assertArrayEquals(new int[]{esperado.puntajeA(), esperado.puntajeB(), esperado.puntajeFinal()},
                    new int[]{fila[1], fila[2], fila[3]});
            var medidas = new co.aplomo.dao.AnalisisDao().mediciones(con, fila[0]);
            assertEquals(5, medidas.size());
            for (var elemento : calculo.mediciones()) {
                var guardada = medidas.stream().filter(elemento2 -> elemento2.segmento() == elemento.segmento())
                        .findFirst().orElseThrow();
                assertEquals(elemento.grados(), guardada.grados(), .001);
                assertEquals(elemento.puntos(), guardada.puntos());
                assertTrue(guardada.manual());
            }
        }
        int pendiente = Sql.uno(con, "SELECT p.id_pendiente FROM pendiente_arreglo p JOIN evaluacion e USING(id_ev" +
                        "aluacion) JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) WH" + "ERE r" +
                        ".codigo=?",
                fila6 -> fila6.getInt(1), "DEMO-17-E3");
        Sql.actualizar(con, "UPDATE pendiente_arreglo SET estado='ARREGLADO',descripcion=? WHERE id_pendiente=?",
                "Edición conservada", pendiente);
        DatosDemoPrueba.cargar(con, carpetaFotos);
        assertEquals(primera, cantidades());
        assertEquals("ARREGLADO",
                Sql.uno(con, "SELECT estado FROM pendiente_arreglo WHERE id_pendiente=?", fila7 -> fila7.getString(1),
                        pendiente));
        entrar(admin);
        var informe = get("/informes/empresa/" + universidad);
        assertEquals(200, informe.statusCode());
        for (String etiqueta : List.of("1 · Verde · Nivel 1", "3 · Amarillo · Nivel 2", "7 · Rojo · Nivel 4",
                "Sede Alcázar", "Sede Anova (Medicina)"))
            assertTrue(informe.body().contains(etiqueta), etiqueta);
        assertTrue(informe.body().contains("2 de 3 sedes calificadas · falta Sede Anova (Medicina)"));
        assertFalse(informe.body().contains("Sin calificación general"));
        var segunda = Sql.uno(con, "SELECT a.id_analisis FROM analisis_rula a JOIN fotografia f USING(id_foto) J" +
                "OIN evaluacion e USING(id_evaluacion) JOIN visita v USING(id_visita) JOIN re" +
                "serva r USING(id_reserva) WHERE r.codigo=?", fila8 -> fila8.getInt(1), "DEMO17-E3B");
        var esperado = motor.calcular(CasosRulaPrueba.todos().get(3).entrada());
        assertEquals(esperado.resultado().puntajeFinal(),
                Sql.<Integer>uno(con, "SELECT puntaje_final FROM analisis_rula WHERE id_analisis=?",
                        fila9 -> fila9.getInt(1), segunda));
        var medidasAlcazar = new co.aplomo.dao.AnalisisDao().mediciones(con, segunda);
        assertEquals(5, medidasAlcazar.size());
        for (var elemento3 : esperado.mediciones()) {
            var guardada = medidasAlcazar.stream().filter(elemento4 -> elemento4.segmento() == elemento3.segmento())
                    .findFirst().orElseThrow();
            assertEquals(elemento3.grados(), guardada.grados(), .001);
            assertEquals(elemento3.puntos(), guardada.puntos());
        }
        assertEquals(2, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM visita v JOIN reserva r USING(id_reserva) WHERE r.codig" +
                        "o IN ('DEMO-17-E3','DEMO17-E3B') AND v.estado='FINALIZADA'", fila10 -> fila10.getInt(1)));
    }
}
