package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-013b/024: el inicio del Usuario reúne las posturas y conserva el peor resultado del puesto.
 */
class HttpInicio29Test extends HttpPrueba {
    @Test
    void usuarioVeUnResultadoPorPuestoSinRepetirSuUnicaEmpresa() throws Exception {
        var datos = new EscenarioConsultas(this, carpetaFotos);
        Sql.actualizar(con, "UPDATE evaluacion SET id_puesto=? WHERE id_evaluacion=?", datos.puesto1, datos.evalRojo);
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Rep. Andrés Benavides' WHERE id_cuenta=?", usuario);
        entrar(usuario);
        String html = get("/").body();
        String resultados = html.substring(html.indexOf("Últimos resultados"), html.indexOf("Próximas visitas"));
        assertEquals(1, resultados.split("href=\"/evaluaciones/", -1).length - 1);
        assertTrue(resultados.contains("href=\"/evaluaciones/" + datos.evalRojo + "\""));
        assertFalse(resultados.contains("Empresa de prueba sin confirmar"));
        assertTrue(html.contains("Bienvenido, Andrés"));
        assertTrue(html.contains("usuario-proxima"));
        assertTrue(html.contains("class=\"avatar\" title=\"Rep. Andrés Benavides · Usuario\""));
        assertEquals("AB", Html.iniciales("Rep. Andrés Benavides"));
    }
}
