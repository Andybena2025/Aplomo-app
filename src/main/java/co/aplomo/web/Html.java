package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** RNF-007, RNF-011: HTML común; todos los datos variables pasan por escapar. */
public final class Html {
    private Html() {}

    /** Escapa texto y atributos para que los datos no se ejecuten como HTML (RNF-004). */
    public static String escapar(String texto) {
        return texto == null ? "" : texto.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    /** Aviso persistente con botón de cierre; las confirmaciones duran cinco segundos (RNF-007). */
    public static String aviso(String mensaje, boolean confirmacion) {
        if (mensaje == null || mensaje.isBlank()) return "";
        return "<div class=\"aviso\" role=\"" + (confirmacion ? "status" : "alert") + "\""
                + (confirmacion ? " data-confirmacion" : "") + "><span>" + escapar(mensaje)
                + "</span><button type=\"button\" class=\"link\" data-cerrar aria-label=\"Cerrar aviso\">Cerrar</button></div>";
    }

    /** Inserta valores escapados en la portada conservada de la fase 1 (RF-001a). */
    public static String login(Sesion s, String correo, String mensaje) throws IOException {
        try (var entrada = Html.class.getResourceAsStream("/plantillas/login.html")) {
            if (entrada == null) throw new IOException("No está disponible la página de ingreso.");
            // Reemplazar solo huecos originales impide interpretar texto del usuario como plantilla.
            String plantilla = new String(entrada.readAllBytes(),StandardCharsets.UTF_8);
            var patron = java.util.regex.Pattern.compile("\\{\\{(mensaje|csrf|correo)\\}\\}");
            var matcher = patron.matcher(plantilla);
            return matcher.replaceAll(m -> java.util.regex.Matcher.quoteReplacement(switch (m.group(1)) {
                case "mensaje" -> aviso(mensaje,false);
                case "csrf" -> escapar(s.csrf());
                default -> escapar(correo);
            }));
        }
    }

    /** Genera una página con cabecera, menú permitido y formulario para salir (RF-002). */
    public static String pagina(String titulo, String contenido, Sesion s) {
        String cabecera = "";
        if (s != null && s.cuenta() != null) {
            Cuenta c = s.cuenta();
            String menu = c.temporal() ? "" : "<a class=\"btn sm\" href=\"/\">Inicio</a>"
                    + (c.rol() == Rol.ADMINISTRADOR ? "<a class=\"btn sm\" href=\"/usuarios\">Usuarios</a>" : "");
            cabecera = "<header class=\"top\"><div class=\"wrap\"><a class=\"lockup\" href=\"/\">APLOMO.</a>"
                    + "<nav class=\"row\" aria-label=\"Principal\">" + menu + "</nav><div class=\"who\">"
                    + escapar(c.nombre()) + " · " + (c.rol() == Rol.ADMINISTRADOR ? "Administrador" : "Usuario")
                    + "</div><a class=\"btn sm\" href=\"/cuenta/contrasena\">Cambiar contraseña</a>"
                    + "<form method=\"post\" action=\"/logout\">" + csrf(s)
                    + "<button class=\"btn sm\">Cerrar sesión</button></form></div></header>";
        }
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<title>" + escapar(titulo) + " · APLOMO.</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Instrument+Serif&family=Inter:wght@300;400;500;600&display=swap\" rel=\"stylesheet\">"
                + "<link rel=\"stylesheet\" href=\"/static/css/aplomo.css\">"
                + "<script src=\"/static/js/cuenta.js\" defer></script></head><body>" + cabecera
                + "<main class=\"wrap stack\"><div class=\"pagehead\"><div><span class=\"eyebrow\">Ergonomía con precisión</span><h1>"
                + escapar(titulo) + "</h1></div></div>" + contenido + "</main></body></html>";
    }

    /** Incluye el token de la sesión en un formulario POST (RNF-004). */
    public static String csrf(Sesion s) { return "<input type=\"hidden\" name=\"csrf\" value=\"" + escapar(s.csrf()) + "\">"; }

    /** Campo de texto con etiqueta accesible y valor escapado (RNF-002, RNF-004). */
    public static String campo(String nombre, String etiqueta, String tipo, String valor, int maximo) {
        return "<div class=\"field\"><label for=\"" + escapar(nombre) + "\">" + escapar(etiqueta)
                + "</label><input class=\"input\" id=\"" + escapar(nombre) + "\" name=\"" + escapar(nombre)
                + "\" type=\"" + escapar(tipo) + "\" value=\"" + escapar(valor) + "\" maxlength=\"" + maximo + "\" required"
                + (tipo.equals("password") ? " autocomplete=\"new-password\"" : "") + "></div>";
    }

    /** Formulario compartido para cambio obligatorio y voluntario (RF-001c). */
    public static String contrasena(Sesion s, String error) {
        String aviso = s.cuenta().temporal() ? aviso("Tu contraseña es temporal. Cámbiala para continuar.",false) : "";
        return pagina("Cambiar contraseña",aviso + aviso(error,false)
                + "<form class=\"card stack cuenta-form\" method=\"post\" action=\"/cuenta/contrasena\">" + csrf(s)
                + campo("actual","Contraseña actual","password","",100).replace("new-password","current-password")
                + campo("nueva","Nueva contraseña","password","",100) + campo("confirmacion","Confirmar nueva contraseña","password","",100)
                + "<p class=\"hint\">Al menos 8 caracteres, una letra y un número.</p><button class=\"btn p\">Guardar contraseña</button></form>",s);
    }
}
