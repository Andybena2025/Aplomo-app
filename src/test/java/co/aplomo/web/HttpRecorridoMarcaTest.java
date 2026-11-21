package co.aplomo.web;

import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.dao.AnalisisDao;
import co.aplomo.modelo.Cuenta;
import co.aplomo.servicio.Usuarios;
import co.aplomo.db.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 25: inventario real de páginas por rol, texto y atributos accesibles sin la marca anterior.
 */
class HttpRecorridoMarcaTest extends HttpPrueba {
    EscenarioConsultas escenario;

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioConsultas(this, carpetaFotos);
    }

    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void recorreListasDetallesFormulariosResultadoYPortadaPorRol(Cuenta.Rol rol) throws Exception {
        verificar(get("/login").body(), "/login");
        verificar(get("/ayuda").body(), "/ayuda público");
        int actor = admin;
        if (rol == Cuenta.Rol.EVALUADOR) {
            actor = servicioUsuarios().crear(cuenta(admin),
                    new Usuarios.Datos("Evaluador QA", UUID.randomUUID() + "@prueba.invalid", "123", "QA", rol, null,
                            List.of(empresa)), clave);
            dao.contrasena(con, actor, cuenta(actor).hash(), false);
        } else if (rol == Cuenta.Rol.USUARIO) actor = usuario;
        entrar(actor);
        List<String> rutas = new ArrayList<>(
                List.of("/", "/empresas", "/sedes", "/salones", "/areas", "/cargos", "/puestos", "/reservas",
                        "/visitas", "/evaluaciones", "/pendientes", "/sugerencias", "/ayuda", "/cuenta/contrasena",
                        "/empresas/" + empresa, "/sedes/" + escenario.sedeA, "/salones/" + escenario.salonA,
                        "/puestos/" + escenario.puesto1, "/reservas/" + escenario.reservaFinal,
                        "/visitas/" + escenario.visitaFinal, "/evaluaciones/" + escenario.evalRojo,
                        "/calificaciones?empresa=" + empresa));
        if (rol != Cuenta.Rol.USUARIO) {
            rutas.addAll(List.of("/agenda", "/agenda?vista=mes", "/demo/evaluacion", "/empresas/" + empresa + "/editar",
                    "/empresas/" + empresa + "/sedes/nuevo", "/empresas/" + empresa + "/areas/nuevo",
                    "/empresas/" + empresa + "/cargos/nuevo", "/sedes/" + escenario.sedeA + "/editar",
                    "/sedes/" + escenario.sedeA + "/salones/nuevo", "/salones/" + escenario.salonA + "/editar",
                    "/salones/" + escenario.salonA + "/puestos/nuevo", "/puestos/" + escenario.puesto1 + "/editar",
                    "/reservas/nueva", "/reservas/" + escenario.reservaEsperando + "/editar",
                    "/reservas/" + escenario.reservaEsperando + "/cancelar", "/visitas/" + escenario.visitaAbierta,
                    "/visitas/" + escenario.visitaAbierta + "/finalizar",
                    "/evaluaciones/nueva?visita=" + escenario.visitaAbierta + "&puesto=" + escenario.puesto2,
                    "/evaluaciones/" + escenario.evalAbierta, "/evaluaciones/" + escenario.evalAbierta + "/analizar",
                    "/pendientes/" + escenario.pendienteAbierto, "/sugerencias/" + escenario.sugerenciaAbierta,
                    "/sugerencias/" + escenario.sugerenciaAbierta + "/editar",
                    "/sugerencias/nueva?evaluacion=" + escenario.evalAbierta, "/informes/empresa/" + empresa));
            int analisis = new AnalisisDao().listar(con, escenario.evalAbierta).get(0).id();
            rutas.add("/evaluaciones/" + escenario.evalAbierta + "/analisis/" + analisis);
        }
        if (rol == Cuenta.Rol.ADMINISTRADOR) rutas.addAll(
                List.of("/usuarios", "/usuarios/nuevo", "/usuarios/" + usuario + "/editar", "/empresas/nueva"));
        if (rol == Cuenta.Rol.EVALUADOR) rutas.add("/mis-evaluaciones");
        for (String ruta : rutas) {
            var res = get(ruta);
            boolean restringida = rol == Cuenta.Rol.ADMINISTRADOR &&
                    (ruta.equals("/demo/evaluacion") || ruta.equals("/reservas/nueva") ||
                            ruta.matches("/reservas/[0-9]+/(editar|cancelar)")) ||
                    rol == Cuenta.Rol.EVALUADOR && ruta.equals("/empresas/" + empresa + "/editar");
            if (restringida) {
                assertEquals(403, res.statusCode(), ruta);
                continue;
            }
            assertTrue(res.statusCode() == 200 || res.statusCode() == 303, ruta + " → " + res.statusCode());
            verificar(res.body(), ruta);
        }
        for (String activo : List.of("/static/css/aplomo.css", "/static/js/ayuda.js", "/static/js/nosotros.js")) {
            var res = get(activo);
            assertEquals(200, res.statusCode());
            assertFalse(res.body().contains("APLOMO"), activo);
        }
    }

    static void verificar(String html, String ruta) {
        String texto = html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style\\b[^>]*>.*?</style>", "").replaceAll("(?s)<[^>]+>", "");
        assertFalse(texto.contains("APLOMO"), ruta + " texto visible");
        var elemento = Pattern.compile("(?:title|aria-label)=\"([^\"]*)\"").matcher(html);
        while (elemento.find()) assertFalse(elemento.group(1).contains("APLOMO"), ruta + " atributo accesible");
    }
}
