package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-002/013b/024, RNF-002: encabezado único, archivo y visita agrupada sin revelar fotos al Usuario.
 */
class HttpPaso30ATest extends HttpPrueba {
    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void unaFranjaYUnaFilaPorPuestoConPermisosVigentes(Cuenta.Rol rol) throws Exception {
        var datos = new EscenarioConsultas(this, carpetaFotos);
        var evaluador = evaluadorTrabajo(empresa);
        Sql.actualizar(con, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                "SET r.id_evaluador=? WHERE s.id_empresa=?", evaluador.id(), empresa);
        Sql.actualizar(con, "UPDATE evaluacion SET id_puesto=? WHERE id_evaluacion=?", datos.puesto1, datos.evalRojo);
        int actor = rol == Cuenta.Rol.ADMINISTRADOR ? admin : rol == Cuenta.Rol.USUARIO ? usuario : evaluador.id();
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Rep. Andrés Benavides' WHERE id_cuenta=?", actor);
        entrar(actor);
        for (String ruta : List.of("/", "/empresas", "/visitas", "/visitas/" + datos.visitaFinal, "/cuenta")) {
            String html = get(ruta).body();
            String antesDeMain = html.substring(0, html.indexOf("<main"));
            assertEquals(1, Pattern.compile("<header\\b").matcher(antesDeMain).results().count(), ruta);
            assertTrue(antesDeMain.contains("<h1>") || antesDeMain.contains("cabecera-seccion"), ruta);
            assertTrue(antesDeMain.contains("aria-label=\"Menú principal\""), ruta);
            assertFalse(html.contains("Administración de ALINEO"));
            assertFalse(html.contains("Tu jornada de evaluación"));
            if (rol == Cuenta.Rol.USUARIO && ruta.equals("/"))
                assertFalse(antesDeMain.contains("class=\"cabecera-rol\""));
        }
        String cuenta = get("/cuenta").body();
        assertTrue(cuenta.contains("class=\"iniciales\">AB</span>"));
        String visita = get("/visitas/" + datos.visitaFinal).body();
        assertEquals(1, visita.split("data-puesto-visita=", -1).length - 1);
        assertTrue(visita.contains("Puesto 1 · 2 posturas"));
        assertEquals(rol != Cuenta.Rol.USUARIO, visita.contains(">Ver evaluación</a>"));
        if (rol == Cuenta.Rol.USUARIO) assertFalse(visita.contains("/fotos/"));
        assertFalse(visita.contains("<dt>Calificación</dt>"));
        String archivo = get("/archivo?filtro_lista=INACTIVO").body();
        assertTrue(archivo.contains("data-seccion=\"ARCHIVO\""));
        assertFalse(archivo.contains("archivo-indicadores"));
    }

    @Test
    void enlaceDeArchivoNoCubreLaTablaYNombresCortosConservanElCompleto() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/aplomo.css"));
        assertTrue(css.contains(".alineo-table .archivo-identidad::after{content:none!important}"));
        String persona = TextoLista.persona("Ing. Laura Gabriella Romero", "#C9CCD2");
        assertTrue(persona.contains("title=\"Laura Gabriella Romero\""));
        assertTrue(persona.contains(">Laura Romero</span>"));
    }
}
