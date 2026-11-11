package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** RNF-007, RNF-011: HTML común; todos los datos variables pasan por escapar. */
public final class Html {
    private Html() {}

    /** Zona horaria del sistema (RNF-007). */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private static final String[] MESES = {"ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic"};

    /** Formato "20 nov 2026" como en el mock (RNF-007). */
    public static String fecha(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.getDayOfMonth() + " " + MESES[fecha.getMonthValue() - 1] + " " + fecha.getYear();
    }

    /** Formato "20 nov 2026 14:00" para marcas de tiempo (RNF-007). */
    public static String fechaHora(LocalDateTime valor) {
        if (valor == null) return "—";
        return fecha(valor.toLocalDate()) + " " + String.format("%02d:%02d", valor.getHour(), valor.getMinute());
    }

    /** Hoy en la zona horaria del sistema (RNF-007). */
    public static LocalDate hoy() { return LocalDate.now(ZONA); }

    /** SVG del ícono A-plomada del logo (identidad visual del mock). */
    private static final String LOGO_SVG =
            "<svg width=\"%d\" height=\"%d\" viewBox=\"0 0 100 100\" style=\"margin-bottom:%dpx;margin-right:%dpx\" aria-hidden=\"true\">"
            + "<line x1=\"50\" y1=\"10\" x2=\"16\" y2=\"90\" stroke=\"currentColor\" stroke-width=\"4\" stroke-linecap=\"round\"/>"
            + "<line x1=\"50\" y1=\"10\" x2=\"84\" y2=\"90\" stroke=\"currentColor\" stroke-width=\"4\" stroke-linecap=\"round\"/>"
            + "<path d=\"M31.2,54.2 A48,48 0 0 0 68.8,54.2\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.2\" stroke-dasharray=\"3.6 3.6\" opacity=\".85\"/>"
            + "<line x1=\"50\" y1=\"10\" x2=\"50\" y2=\"64\" stroke=\"currentColor\" stroke-width=\"1.6\"/>"
            + "<path d=\"M47.84,64 h4.32 v2.16 L55.04,70.48 L50,82.72 L44.96,70.48 L47.84,66.16 Z\" fill=\"currentColor\"/>"
            + "<circle cx=\"50\" cy=\"10\" r=\"3.6\" fill=\"currentColor\"/></svg>";

    /** Logo con ícono y texto como en el mock, a la altura indicada en píxeles. */
    public static String logo(int altura) {
        int svgH = (int)(altura * 0.78);
        int marginB = (int)(altura * 0.19);
        int marginR = (int)(altura * 0.03);
        return "<span class=\"lockup\" style=\"font-size:" + altura + "px\" aria-label=\"APLOMO.\">"
                + String.format(LOGO_SVG, svgH, svgH, marginB, marginR) + "<span>PLOMO.</span></span>";
    }

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

    /** Genera una página con cabecera igual al mock: atrás, logo, pestañas, nombre+avatar, salir (RF-002). */
    public static String pagina(String titulo, String contenido, Sesion s) {
        return pagina(titulo, contenido, s, true);
    }

    /** Versión con control del pagehead para páginas que definen su propio encabezado (como Inicio). */
    public static String pagina(String titulo, String contenido, Sesion s, boolean conPagehead) {
        String cabecera = "";
        if (s != null && s.cuenta() != null) {
            Cuenta c = s.cuenta();
            String iniciales = iniciales(c.nombre());
            String rolTexto = c.rol() == Rol.ADMINISTRADOR ? "Administrador" : "Usuario";
            // Pestañas de texto con la activa resaltada, como en el mock
            String[][] tabs = c.rol() == Rol.ADMINISTRADOR
                    ? new String[][]{{"Inicio","/"}, {"Empresas","/empresas"}, {"Reservas","/reservas"}, {"Visitas","/visitas"}, {"Usuarios","/usuarios"}}
                    : new String[][]{{"Inicio","/"}, {"Empresas","/empresas"}, {"Reservas","/reservas"}, {"Visitas","/visitas"}};
            StringBuilder nav = new StringBuilder();
            if (!c.temporal()) {
                for (String[] tab : tabs) {
                    nav.append("<a class=\"nav-tab\" href=\"").append(tab[1]).append("\">").append(tab[0]).append("</a>");
                }
            }
            cabecera = "<header class=\"top\"><div class=\"wrap\">"
                    // Botón Atrás + logo con ícono
                    + "<div class=\"row\" style=\"gap:10px;flex-wrap:nowrap\">"
                    + "<button class=\"btn sm\" onclick=\"history.back()\" aria-label=\"Atrás\">← <span class=\"hideSm\">Atrás</span></button>"
                    + "<a href=\"/\" style=\"text-decoration:none\">" + logo(26) + "</a></div>"
                    // Pestañas de navegación
                    + "<nav class=\"nav\" aria-label=\"Menú principal\">" + nav + "</nav>"
                    // Nombre + rol + avatar + Salir (Cambiar contraseña dentro del menú de cuenta)
                    + "<div class=\"who\"><div class=\"meta\" style=\"text-align:right\"><div>"
                    + escapar(c.nombre()) + "</div><div class=\"muted small\">" + escapar(rolTexto) + "</div></div>"
                    + "<a href=\"/cuenta/contrasena\" class=\"avatar\" title=\"Cambiar contraseña\">" + escapar(iniciales) + "</a>"
                    + "<form method=\"post\" action=\"/logout\" style=\"margin:0\">" + csrf(s)
                    + "<button class=\"btn sm\">Salir</button></form></div>"
                    + "</div></header>";
        }
        String pagehead = conPagehead
                ? "<div class=\"pagehead\"><div><span class=\"eyebrow\">Ergonomía con precisión</span><h1>" + escapar(titulo) + "</h1></div></div>"
                : "";
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<title>" + escapar(titulo) + " · APLOMO.</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Instrument+Serif&family=Inter:wght@300;400;500;600&display=swap\" rel=\"stylesheet\">"
                + "<link rel=\"stylesheet\" href=\"/static/css/aplomo.css\">"
                + "<script src=\"/static/js/cuenta.js\" defer></script><script src=\"/static/js/reservas.js\" defer></script></head><body>" + cabecera
                + "<main class=\"wrap\">" + pagehead + contenido + "</main></body></html>";
    }

    /** Iniciales del nombre para el avatar circular del mock (máximo 2 letras). */
    private static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] partes = nombre.strip().split("\\s+");
        StringBuilder ini = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) ini.appendCodePoint(partes[i].codePointAt(0));
        }
        return ini.toString().toUpperCase(Locale.ROOT);
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
