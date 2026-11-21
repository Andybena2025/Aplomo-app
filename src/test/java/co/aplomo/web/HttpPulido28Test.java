package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.db.Sql;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.soporte.EscenarioConsultas;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
import java.util.Map;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-002/024, RNF-002/007: auditoría HTTP de todas las pestañas anunciadas a cada rol.
 */
class HttpPulido28Test extends HttpPrueba {
    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void pestanasTienenUnTituloSinAtrasNiTextosInternosNiSegundos(Cuenta.Rol rol) throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        int actor =
                rol == Cuenta.Rol.ADMINISTRADOR ? admin : rol == Cuenta.Rol.USUARIO ? usuario : evaluadorTrabajo().id();
        entrar(actor);
        String inicio = get("/").body();
        var menu = Pattern.compile("<nav class=\"nav\"[\\s\\S]*?</nav>").matcher(inicio);
        assertTrue(menu.find());
        var enlaces = Pattern.compile("href=\"([^\"]+)\"").matcher(menu.group());
        var rutas = new LinkedHashSet<String>();
        while (enlaces.find()) rutas.add(enlaces.group(1));
        for (String ruta : rutas) {
            var respuesta = get(ruta);
            assertEquals(200, respuesta.statusCode(), ruta);
            String html = respuesta.body();
            assertEquals(1, html.split("<h1(?:\\s|>)", -1).length - 1, ruta);
            assertFalse(html.contains("aria-label=\"Atrás\""), ruta);
            String visible = html.replaceAll("(?s)<script.*?</script>|<style.*?</style>", "")
                    .replaceAll("<[^>]+>", " ");
            assertFalse(visible.contains("(propuesta)"), ruta);
            assertFalse(Pattern.compile("\\b\\d{1,2}:\\d{2}:\\d{2}\\b").matcher(visible).find(), ruta);
            var pildoras = Pattern.compile("<span class=\"pill[^\"]*\"[^>]*>(.*?)</span>", Pattern.DOTALL)
                    .matcher(html);
            while (pildoras.find()) assertTrue(pildoras.group(1).chars().filter(c -> c == '●').count() <= 1, ruta);
        }
    }

    @Test
    void evaluadorVeSuTrabajoSinMezclarLasCitasDeOtroAsignado() throws Exception {
        var e = new EscenarioConsultas(this, carpetaFotos);
        int id = Sql.uno(con, "SELECT id_evaluador FROM reserva WHERE id_reserva=?", f -> f.getInt(1),
                e.reservaAbierta);
        Sql.actualizar(con, "UPDATE cuenta SET estado='ACTIVA' WHERE id_cuenta=?", id);
        new co.aplomo.dao.CuentaEmpresaDao().sincronizar(con, admin, id, java.util.List.of(e.empresaA), false);
        var responsable = cuenta(id);
        var otro = evaluadorTrabajo();
        var visitas = new co.aplomo.servicio.Visitas(tx, reloj);
        assertTrue(visitas.listar(responsable).stream().anyMatch(v -> v.visita().id() == e.visitaAbierta));
        assertFalse(visitas.listar(otro).stream().anyMatch(v -> v.visita().id() == e.visitaAbierta));
        var reservas = new co.aplomo.servicio.Reservas(tx, reloj);
        assertFalse(reservas.listar(otro).stream().anyMatch(r -> r.reserva().id() == e.reservaAbierta));
        assertEquals(e.visitaAbierta, visitas.consultar(otro, e.visitaAbierta).visita().id());
    }

    @Test
    void inicialesIgnoranLosTitulosEnUnUnicoMetodo() {
        assertEquals("AB", Html.iniciales("Ing. Andrés Benavides"));
        assertEquals("JZ", Html.iniciales("Ingeniero Johan Zapata"));
        assertEquals("GR", Html.iniciales("Dra. Gabriella Romero"));
        assertEquals("AB", Html.iniciales("Arq. Lic. Andrés Benavides"));
    }

    @Test
    void tablasNombranElContenidoYMuestranMostrarContrasenaEnTodasLasPaginas() throws Exception {
        new EscenarioConsultas(this, carpetaFotos);
        entrar(admin);
        var reservas = get("/reservas").body();
        assertTrue(reservas.contains("<th scope=\"col\">Hora</th>"));
        assertTrue(reservas.contains("<th scope=\"col\">Evaluador</th>"));
        assertTrue(reservas.contains("mostrar-clave.js"));
        var pendientes = get("/pendientes").body();
        assertTrue(pendientes.contains("Fecha límite"));
        assertFalse(pendientes.contains("▮"));
        assertFalse(pendientes.contains("data-lista-filtro=\"EN_PROCESO\""));
        assertTrue(pendientes.contains("data-etiqueta=\"Abiertos\""));
        assertTrue(pendientes.contains("data-etiqueta=\"Arreglados\""));
        assertTrue(pendientes.contains("data-etiqueta=\"Todos\""));
    }
}
