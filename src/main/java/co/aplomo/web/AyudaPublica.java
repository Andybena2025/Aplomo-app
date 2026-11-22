package co.aplomo.web;

import co.aplomo.servicio.Ayuda.Contenido;
import co.aplomo.seguridad.Sesiones.Sesion;

/**
 * RF-001/002, RNF-007: ayuda de acceso sin crear sesión ni mostrar datos privados.
 */
final class AyudaPublica {
    private AyudaPublica() {
    }

    static String pagina(Sesion sesion, Contenido contenido) {
        String html =
                "<header class=\"ayuda-publica-barra\"><a class=\"ayuda-logo-publico\" href=\"/login\" " +
                        "aria-label=\"ALINEO.\">" +
                        Marca.logo() + "</a><a class=\"btn\" href=\"/login\">← Volver a iniciar sesión</a></header>" +
                        "<div class=\"ayuda-publica-grid\"><section id=\"entrar\"><span class=\"eyebrow\">Centro de " +
                        "ayuda</span>" +
                        "<h1>¿Problemas para entrar?</h1><div class=\"ayuda-preguntas-publicas\">" +
                        pregunta("Olvidé mi contraseña",
                                "La Administradora te da una contraseña temporal. Escríbele por WhatsApp o correo.") +
                        pregunta("Mi cuenta está bloqueada",
                                "Tras 5 intentos fallidos, tu cuenta se bloquea 15 minutos. Espera y vuelve a " +
                                        "intentar.") +
                        pregunta("Me pide cambiar la contraseña",
                                "Es una contraseña temporal. Crea una nueva de mínimo 8 caracteres, con una letra y " +
                                        "un número.") +
                        "<details class=\"guia tarjeta-dorada\"><summary>Mi empresa no tiene cuenta</summary><div " +
                        "class=\"ayuda-respuesta\">" +
                        "<p>Solicita acceso con los datos de tu empresa y su contacto.</p><a class=\"btn\" " +
                        "href=\"/solicitar-acceso\">Solicitar acceso</a>" +
                        "</div></details></div></section><aside class=\"stack\">" +
                        ContactoAyuda.tarjeta(contenido.contacto(), sesion) +
                        "<section id=\"acerca\" class=\"card stack tarjeta-dorada\"><h2>¿Qué es ALINEO?</h2>" +
                        "<p>Evaluamos posturas de trabajo con el método RULA.<br>Te guiamos desde la observación " +
                        "hasta las mejoras.</p>" +
                        "<a href=\"/nosotros\">Conoce ALINEO ›</a><p class=\"hint\">Proyecto Nuclear " +
                        "2026-2</p></section></aside></div>" +
                        "<footer class=\"ayuda-pie\">Ergonomía con precisión · Método RULA</footer>";
        return Html.pagina("¿Problemas para entrar?", html, null, false, false);
    }

    private static String pregunta(String titulo, String texto) {
        return "<details class=\"guia tarjeta-dorada\"><summary>" + Html.escapar(titulo) +
                "</summary><div class=\"ayuda-respuesta\"><p>" + Html.escapar(texto) + "</p></div></details>";
    }
}
