package co.aplomo.web;

import co.aplomo.servicio.Avisos.ContactoEquipo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ContactoLogin34Test {
    @TempDir
    java.nio.file.Path carpeta;
    String anterior;

    @BeforeEach
    void prepararContacto() throws Exception {
        anterior = System.getProperty("aplomo.contacto.archivo");
        var archivo = carpeta.resolve("contacto.properties");
        Files.writeString(archivo, "contacto.nombre=Laura Romero\ncontacto.correo=admin34@prueba.invalid\n" +
                "contacto.telefono=+57 321 222 3333\ncontacto.whatsapp=573212223333\n");
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
    }

    @AfterEach
    void restaurar() {
        if (anterior == null) System.clearProperty("aplomo.contacto.archivo");
        else System.setProperty("aplomo.contacto.archivo", anterior);
    }

    @Test
    void svgTienePuntosGuiasYAngulosExactos() throws Exception {
        String html;
        try (var entrada = getClass().getResourceAsStream("/plantillas/login.html")) {
            assertNotNull(entrada);
            html = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        int inicio = html.indexOf("<svg class=\"pz-svg\"", html.indexOf("id=\"pzOverlay\""));
        String svg = html.substring(inicio, html.indexOf("</svg>", inicio) + 6);
        for (var caso : List.of(new Object[]{"lb", 5}, new Object[]{"ld", 5},
                new Object[]{"ldp", 5}, new Object[]{"jt", 8})) {
            assertEquals((int) caso[1], Pattern.compile("class=\"" + caso[0] + "\"").matcher(svg).results().count());
        }
        var valores = Pattern.compile("<text class=\"lv\"[^>]*>([^<]+)</text>").matcher(svg)
                .results().map(m -> m.group(1)).toList();
        assertEquals(List.of("21°", "22°", "65°", "19°", "4°"), valores);
        assertFalse(svg.contains("108°"));
        assertFalse(svg.contains("162°"));
        assertTrue(svg.contains("cx=\"1043\" cy=\"90\""));
        assertTrue(svg.contains("x1=\"988\" y1=\"20\" x2=\"988\" y2=\"800\""));
    }

    @Test
    void evaluadorSinNumeroTieneCorreoYMensajeConSuIdSinWhatsApp() {
        String html = ficha(false, "0000000000");
        assertTrue(html.contains("<p class=\"contacto-dato\">evaluador34@prueba.invalid</p>"));
        assertTrue(html.contains("href=\"mailto:evaluador34@prueba.invalid?subject=Ayuda%20con%20ALINEO\">Correo</a>"));
        assertTrue(html.contains("href=\"/avisos/nuevo?para=34\">Enviar mensaje en ALINEO</a>"));
        assertFalse(html.contains("WhatsApp</a>"));
        assertFalse(html.contains(">Escribir</a>"));
        assertTrue(html.contains("class=\"row contacto-botones\""));
    }

    @Test
    void numeroValidoAgregaWhatsAppYEscapaNombre() {
        String html = ficha(false, "321 222 3333");
        assertTrue(html.contains("Persona &lt;script&gt;"));
        assertFalse(html.contains("Persona <script>"));
        assertTrue(html.contains("https://wa.me/573212223333"));
        assertTrue(html.contains(">Correo</a>"));
        assertTrue(html.contains(">Enviar mensaje en ALINEO</a>"));
    }

    @Test
    void administradoraUsaSuContactoYSuIdConLosMismosBotones() {
        String html = ficha(true, "0000000000");
        assertTrue(html.contains("mailto:admin34@prueba.invalid?subject=Ayuda%20con%20ALINEO"));
        assertTrue(html.contains("https://wa.me/573212223333"));
        assertTrue(html.contains("/avisos/nuevo?para=34\">Enviar mensaje en ALINEO"));
        assertFalse(html.contains(">Escribir</a>"));
    }

    private static String ficha(boolean principal, String telefono) {
        return ContactoInicio.tarjeta(List.of(new ContactoEquipo(34, "Persona <script>", "Evaluadora RULA",
                "evaluador34@prueba.invalid", telefono, "#FF4F9A", principal)));
    }
}
