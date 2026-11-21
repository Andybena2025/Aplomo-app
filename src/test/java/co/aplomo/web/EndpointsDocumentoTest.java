package co.aplomo.web;

import co.aplomo.modelo.SeccionConsulta;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inventario documental: no depende de espacios ni de líneas de los manejadores.
 */
class EndpointsDocumentoTest {
    private static final Path RAIZ = Path.of("");

    private static String leer(String archivo) throws Exception {
        return Files.readString(RAIZ.resolve(archivo));
    }

    private static String documento() throws Exception {
        return leer("docs/ENDPOINTS.md");
    }

    @Test
    void incluyeTodosLosContextosYSeccionesDeConsulta() throws Exception {
        String doc = documento();
        var contextos = Pattern.compile("createContext\\s*\\(\\s*\"([^\"]+)\"")
                .matcher(leer("src/main/java/co/aplomo/App.java"));
        int cuantos = 0;
        while (contextos.find()) {
            cuantos++;
            assertTrue(doc.contains("`" + contextos.group(1) + "`"), contextos.group(1));
        }
        assertTrue(cuantos > 0, "Se debe revisar cualquier cambio del registro HTTP");
        for (var seccion : SeccionConsulta.values())
            assertTrue(doc.contains("`" + seccion.ruta() + "`"), seccion.ruta());
    }

    @Test
    void incluyeLasRutasLiteralesDeLosManejadores() throws Exception {
        String doc = documento();
        int rutas = 0;
        for (String nombre : List.of("Router", "UsuariosWeb", "EmpresasWeb", "ReservasWeb", "VisitasWeb",
                "EvaluacionesWeb", "PendientesWeb", "SugerenciasWeb", "CalificacionesWeb")) {
            var literales = Pattern.compile("ruta\\s*\\.\\s*equals\\s*\\(\\s*\"(/[^\"]*)\"")
                    .matcher(leer("src/main/java/co/aplomo/web/" + nombre + ".java"));
            while (literales.find()) {
                rutas++;
                assertTrue(doc.contains("`" + literales.group(1) + "`"), nombre + ": " + literales.group(1));
            }
        }
        assertTrue(rutas > 20, "No se deben perder las comparaciones del enrutador sin revisar esta prueba");
    }

    @Test
    void incluyeFamiliasDinamicasYAltasEnContexto() throws Exception {
        String doc = documento();
        for (String ruta : List.of("/usuarios/{id}/editar", "/usuarios/{id}/activar", "/usuarios/{id}/desactivar",
                "/{tipo}/{id}/editar", "/{tipo}/{id}/activar", "/{tipo}/{id}/desactivar",
                "/empresas/{empresa}/sedes/nuevo", "/empresas/{empresa}/areas/nuevo",
                "/empresas/{empresa}/cargos/nuevo", "/sedes/{sede}/salones/nuevo", "/salones/{salon}/puestos/nuevo",
                "/reservas/crear-empresa", "/reservas/crear-sede", "/reservas/crear-salon",
                "/reservas/{reserva}/iniciar-visita", "/reservas/{id}/editar", "/reservas/{id}/aceptar",
                "/reservas/{id}/cancelar", "/visitas/{id}/finalizar", "/evaluaciones/{evaluacion}/analizar",
                "/evaluaciones/{evaluacion}/analisis", "/evaluaciones/{evaluacion}/analisis/{analisis}",
                "/evaluaciones/{evaluacion}/fotos", "/fotos/{foto}", "/pendientes/{id}/arreglado",
                "/sugerencias/{id}/quitar", "/sugerencias/{id}/estado", "/informes/empresa/{empresa}", "/static/*"))
            assertTrue(doc.contains("`" + ruta + "`"), ruta);
    }
}
