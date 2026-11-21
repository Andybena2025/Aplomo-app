package co.aplomo.web;

import co.aplomo.db.Sql;

import co.aplomo.modelo.Cuenta;
import co.aplomo.servicio.Usuarios;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 27A: las familias GET de páginas conservan diseño compartido y sus límites de rol.
 */
class HttpAuditoriaVisual27Test extends HttpPrueba {
    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void familiasDePaginasUsanElSistemaVisualSinSaltarPermisos(Cuenta.Rol rol) throws Exception {
        var datos = new EscenarioConsultas(this, carpetaFotos);
        int evaluador = servicioUsuarios().crear(cuenta(admin),
                new Usuarios.Datos("Evaluadora QA", "visual27@prueba.invalid", "123", "QA", Cuenta.Rol.EVALUADOR, null,
                        List.of(empresa)), clave);
        dao.contrasena(con, evaluador, cuenta(evaluador).hash(), false);
        Sql.actualizar(con, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                "SET r.id_evaluador=? WHERE s.id_empresa=?", evaluador, empresa);
        int actor = rol == Cuenta.Rol.ADMINISTRADOR ? admin : rol == Cuenta.Rol.EVALUADOR ? evaluador : usuario;
        entrar(actor);
        List<String> comunes = new ArrayList<>(
                List.of("/", "/ayuda", "/cuenta", "/cuenta/nombre", "/cuenta/telefono", "/cuenta/cargo",
                        "/cuenta/clave", "/cuenta/contrasena", "/empresas", "/reservas", "/visitas", "/evaluaciones",
                        "/pendientes", "/sugerencias", "/puestos", "/archivo", "/empresas/" + empresa,
                        "/sedes/" + datos.sedeA, "/salones/" + datos.salonA, "/reservas/" + datos.reservaFinal,
                        "/reservas/" + datos.reservaCancelada, "/visitas/" + datos.visitaFinal,
                        "/evaluaciones/" + datos.evalRojo, "/calificaciones"));
        for (String ruta : comunes) pagina(ruta);
        if (rol == Cuenta.Rol.EVALUADOR) {
            assertEquals(403, get("/archivo?empresa=" + empresa).statusCode());
        } else pagina("/archivo?empresa=" + empresa);
        List<String> gestion = List.of("/empresas/nueva", "/empresas/" + empresa + "/editar",
                "/empresas/" + empresa + "/sedes/nuevo", "/empresas/" + empresa + "/areas/nuevo",
                "/empresas/" + empresa + "/cargos/nuevo", "/sedes/" + datos.sedeA + "/editar",
                "/sedes/" + datos.sedeA + "/salones/nuevo", "/salones/" + datos.salonA + "/editar",
                "/salones/" + datos.salonA + "/puestos/nuevo", "/areas/" + datos.areaA + "/editar",
                "/cargos/" + datos.cargoA + "/editar", "/puestos/" + datos.puesto1 + "/editar");
        for (String ruta : gestion) {
            if (rol == Cuenta.Rol.USUARIO || rol == Cuenta.Rol.EVALUADOR && ruta.endsWith(empresa + "/editar")) {
                assertEquals(403, get(ruta).statusCode(), ruta);
            } else pagina(ruta);
        }
        for (String ruta : List.of("/usuarios", "/usuarios/nuevo", "/usuarios/" + usuario + "/editar")) {
            if (rol == Cuenta.Rol.ADMINISTRADOR) pagina(ruta);
            else assertEquals(403, get(ruta).statusCode(), ruta);
        }
        for (String ruta : List.of("/reservas/nueva", "/reservas/" + datos.reservaEsperando + "/editar",
                "/reservas/" + datos.reservaEsperando + "/cancelar", "/demo/evaluacion")) {
            if (rol == Cuenta.Rol.EVALUADOR) pagina(ruta);
            else assertEquals(403, get(ruta).statusCode(), ruta);
        }
        for (String ruta : List.of("/agenda", "/visitas/" + datos.visitaAbierta,
                "/visitas/" + datos.visitaAbierta + "/finalizar",
                "/evaluaciones/nueva?visita=" + datos.visitaAbierta + "&puesto=" + datos.puesto2,
                "/evaluaciones/" + datos.evalAbierta + "/analizar", "/pendientes/" + datos.pendienteAbierto,
                "/pendientes/" + datos.pendienteAbierto + "/editar",
                "/sugerencias/nueva?evaluacion=" + datos.evalAbierta, "/sugerencias/" + datos.sugerenciaAbierta,
                "/sugerencias/" + datos.sugerenciaAbierta + "/editar", "/informes/empresa/" + empresa)) {
            if (rol == Cuenta.Rol.USUARIO) assertEquals(403, get(ruta).statusCode(), ruta);
            else pagina(ruta);
        }
        for (String ruta : List.of("/empresas", "/reservas", "/visitas", "/pendientes", "/sugerencias", "/evaluaciones",
                "/archivo")) {
            String html = get(ruta).body();
            assertFalse(html.contains(">Ver reserva</a>"), ruta);
            assertFalse(html.contains(">Ver empresa</a>"), ruta);
            assertFalse(html.contains(">Ver pendiente</a>"), ruta);
            assertFalse(html.contains("class=\"semaforo"), ruta);
        }
        assertEquals(404, get("/pagina-inexistente-qa").statusCode());
        assertTrue(get("/pagina-inexistente-qa").body().contains("ALINEO"));
        assertEquals(403, get("/empresas/2147483647").statusCode());
        post("/logout", Map.of("csrf", csrf(get("/"))));
        assertEquals(200, get("/login").statusCode());
        assertEquals(200, get("/ayuda").statusCode());
    }

    private void pagina(String ruta) throws Exception {
        var respuesta = get(ruta);
        assertEquals(200, respuesta.statusCode(),
                ruta + ": " + respuesta.body().substring(0, Math.min(100, respuesta.body().length())));
        assertTrue(respuesta.body().contains("/static/css/aplomo.css"), ruta);
        assertTrue(respuesta.body().contains("Saltar al contenido"), ruta);
        assertFalse(respuesta.body().contains("class=\"semaforo"), ruta);
    }
}
