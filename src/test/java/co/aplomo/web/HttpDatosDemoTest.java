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
        int universidad = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit='012345'", f -> f.getInt(1));
        assertEquals(2, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM sede WHERE id_empresa=?",
                f -> f.getInt(1), universidad));
        var ids = Sql.listar(con, "SELECT a.id_analisis FROM analisis_rula a JOIN fotografia f USING(id_foto) " +
                "JOIN evaluacion e USING(id_evaluacion) JOIN puesto_trabajo p USING(id_puesto) " +
                "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE s.id_empresa=?",
                f -> f.getInt(1), universidad);
        assertFalse(ids.isEmpty());
        var tablas = new TablasRula();
        tablas.cargar(con);
        var motor = new MotorRula(tablas);
        var daoAnalisis = new co.aplomo.dao.AnalisisDao();
        for (int id : ids) {
            var guardado = daoAnalisis.consultar(con, id);
            var mediciones = new java.util.EnumMap<MotorRula.Segmento, MotorRula.Medicion>(MotorRula.Segmento.class);
            for (var medida : daoAnalisis.mediciones(con, id)) {
                assertTrue(medida.manual());
                mediciones.put(medida.segmento(), new MotorRula.Medicion(List.of(), medida.grados()));
            }
            assertEquals(5, mediciones.size());
            var origen = guardado.entrada();
            var calculado = motor.calcular(new MotorRula.Entrada(origen.lado(), 200, 400, mediciones,
                    origen.ajustes(), origen.actividad(), origen.carga())).resultado();
            assertArrayEquals(new int[]{calculado.puntajeA(), calculado.puntajeB(), calculado.puntajeFinal()},
                    new int[]{guardado.puntajeA(), guardado.puntajeB(), guardado.fin()});
        }
        int pendiente = Sql.uno(con, "SELECT MIN(id_pendiente) FROM pendiente_arreglo", f -> f.getInt(1));
        Sql.actualizar(con, "UPDATE pendiente_arreglo SET estado='ARREGLADO',descripcion=? WHERE id_pendiente=?",
                "Edición conservada", pendiente);
        DatosDemoPrueba.cargar(con, carpetaFotos);
        assertEquals(primera, cantidades());
        assertEquals("Edición conservada", Sql.uno(con,
                "SELECT descripcion FROM pendiente_arreglo WHERE id_pendiente=?", f -> f.getString(1), pendiente));
        entrar(admin);
        var informe = get("/informes/empresa/" + universidad);
        assertEquals(200, informe.statusCode());
        assertTrue(informe.body().contains("Sede Alcázar"));
        assertTrue(informe.body().contains("Sede principal"));
        assertTrue(informe.body().contains("2 de 2 sedes calificadas"));
        assertFalse(informe.body().contains("Sin calificación general"));
        for (String color : List.of("Verde", "Amarillo", "Rojo")) assertTrue(informe.body().contains(color));
    }
}
