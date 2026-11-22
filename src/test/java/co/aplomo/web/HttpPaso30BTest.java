package co.aplomo.web;

import co.aplomo.config.Contacto;
import co.aplomo.modelo.Cuenta;
import co.aplomo.soporte.HttpPrueba;

import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-002/027/033, RNF-007: contacto por defecto, artículos autorizados y contenido dinámico sin repetir colores.
 */
class HttpPaso30BTest extends HttpPrueba {
    String anterior;

    @BeforeEach
    void contactoFicticio() {
        anterior = System.getProperty("aplomo.contacto.archivo");
        System.setProperty("aplomo.contacto.archivo", carpetaFotos.resolve("ausente.properties").toString());
    }

    @AfterEach
    void restaurarContacto() {
        if (anterior == null) System.clearProperty("aplomo.contacto.archivo");
        else System.setProperty("aplomo.contacto.archivo", anterior);
    }

    @Test
    void ayudaPublicaTieneCuatroPreguntasYElArchivoPrivadoSigueMandando() throws Exception {
        var respuesta = get("/ayuda");
        String html = respuesta.body();
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.headers().allValues("Set-Cookie").isEmpty());
        assertEquals(4, Pattern.compile("<summary>").matcher(html).results().count());
        for (String texto : List.of("Laura Gabriela Romero", "Volver a iniciar sesión", "Solicitar acceso",
                "https://wa.me/573000000000?text=Hola%20Laura%2C%20necesito%20ayuda%20con%20ALINEO.",
                "mailto:gabriela.romero@alineo.co?subject=Ayuda%20con%20ALINEO"))
            assertTrue(html.contains(texto), texto);
        var archivo = carpetaFotos.resolve("privado.properties");
        Files.writeString(archivo, "contacto.nombre=Contacto local\ncontacto.correo=local@prueba.invalid\n");
        Contacto contacto = Contacto.leer(archivo);
        assertEquals("Contacto local", contacto.nombre());
        assertEquals("local@prueba.invalid", contacto.correo());
        assertEquals("573000000000", contacto.whatsapp());
    }

    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void seisTareasYTodosSusArticulosTienenConsejoYOrientacionPorRol(Cuenta.Rol rol) throws Exception {
        int actor = rol == Cuenta.Rol.ADMINISTRADOR ? admin :
                rol == Cuenta.Rol.USUARIO ? usuario : evaluadorTrabajo(empresa).id();
        dao.contrasena(con, actor, cuenta(actor).hash(), false);
        entrar(actor);
        String html = get("/ayuda").body();
        assertEquals(6, html.split("class=\"card ayuda-tarea\"", -1).length - 1);
        assertTrue(html.contains("data-ayuda-buscar"));
        assertFalse(html.contains("Entrar y contraseña"));
        assertFalse(html.contains("conserva tu acceso personal"));
        assertTrue(html.contains("Enviar mensaje en ALINEO"));
        var coincidencias = Pattern.compile("href=\"/ayuda[?]tema=([a-z-]+)\"").matcher(html);
        var temas = new LinkedHashSet<String>();
        while (coincidencias.find()) temas.add(coincidencias.group(1));
        for (String tema : temas) {
            var respuesta = get("/ayuda?tema=" + tema);
            assertEquals(200, respuesta.statusCode(), tema);
            String articulo = respuesta.body();
            assertEquals(3, articulo.split("Verás: ", -1).length - 1, tema);
            assertEquals(1, articulo.split("class=\"ayuda-consejo\"", -1).length - 1, tema);
            assertEquals(3, articulo.split("data-tema-id=", -1).length - 1, tema);
            assertFalse(articulo.contains("conserva tu acceso personal"));
            assertFalse(articulo.contains("Verde · Nivel 1 · Verde"));
            assertFalse(articulo.contains("<dt>Verde · Nivel 1</dt><dd>Verde"));
        }
        assertEquals(403, get("/ayuda?tema=entrar").statusCode());
        if (rol != Cuenta.Rol.ADMINISTRADOR) assertEquals(403, get("/ayuda?tema=cuentas").statusCode());
    }

    @Test
    void contactoFicticioAbreMensajeParaLaAdministradoraExistenteSinCrearOtraCuenta() throws Exception {
        int destinataria = dao.crear(con, "Ing. Laura Gabriella Romero", UUID.randomUUID() + "@prueba.invalid", "123",
                "Administradora principal", Cuenta.Rol.ADMINISTRADOR, null, cuenta(admin).hash());
        dao.contrasena(con, destinataria, cuenta(destinataria).hash(), false);
        entrar(admin);
        var respuesta = get("/avisos/nuevo");
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Nuevo mensaje"));
        assertTrue(respuesta.body().contains("Elige un evaluador"));
        assertTrue(respuesta.body().contains("name=\"csrf\""));
        var panel = cliente.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(raiz + "/avisos/nuevo"))
                .header("X-Alineo-Panel", "1").GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, panel.statusCode());
        assertTrue(panel.body().contains("data-cerrar-avisos>Cerrar</button>"));
    }
}
