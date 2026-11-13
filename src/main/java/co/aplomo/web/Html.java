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

    /** "1 reserva", "3 reservas": singular cuando n == 1, plural en otro caso (RNF-007). */
    public static String plural(int n, String singular) {
        return n + " " + (n == 1 ? singular : singular + "s");
    }

    /** Evita doble punto cuando un nombre ya termina en punto, como "S.A.S." (RNF-007). */
    public static String punto(String texto) {
        return texto != null && texto.endsWith(".") ? "" : ".";
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

    /** Encabezado de sección con las acciones de página agrupadas junto al título. */
    public static String encabezado(String seccion, String titulo, String acciones) {
        return "<div class=\"pagehead\"><div><span class=\"eyebrow\">" + escapar(seccion)
                + "</span><h1>" + escapar(titulo) + "</h1></div>"
                + (acciones.isEmpty() ? "" : "<div class=\"row pagehead-acciones\">" + acciones + "</div>") + "</div>";
    }

    /** Presentación con contexto explícito, conservando la cabecera y las autorizaciones existentes. */
    public static String pagina(String titulo, String contenido, Sesion s, String seccion, String acciones) {
        return pagina(titulo, encabezado(seccion, titulo, acciones) + contenido, s, false);
    }

    private static String seccion(String titulo) {
        String t = titulo.toLowerCase(java.util.Locale.ROOT);
        if (t.contains("usuario")) return "Gestión · Usuarios";
        if (t.contains("empresa")) return "Gestión · Empresas";
        if (t.contains("sede")) return "Gestión · Sedes";
        if (t.contains("salón")) return "Gestión · Salones";
        if (t.contains("área")) return "Gestión · Áreas";
        if (t.contains("cargo")) return "Gestión · Cargos";
        if (t.contains("evaluación")) return "Evaluaciones · Detalle";
        if (t.contains("rula") || t.contains("analizar")) return "Evaluaciones · Resultado";
        if (t.contains("reserva")) return "Reservas · Formulario";
        if (t.contains("visita")) return "Visitas · Detalle";
        if (t.contains("puesto")) return "Gestión · Puestos";
        if (t.contains("contraseña")) return "Cuenta · Contraseña";
        if (t.contains("calificaci")) return "Consultas · Calificaciones";
        return "Gestión · " + titulo;
    }

    /** Versión con control del pagehead para páginas que definen su propio encabezado (como Inicio). */
    public static String pagina(String titulo, String contenido, Sesion s, boolean conPagehead) {
        return pagina(titulo,contenido,s,conPagehead,true);
    }

    /** Permite que Inicio omita Atrás sin afectar el resto de páginas. */
    public static String pagina(String titulo, String contenido, Sesion s, boolean conPagehead, boolean atras) {
        String cabecera = "";
        if (s != null && s.cuenta() != null) {
            Cuenta c = s.cuenta();
            String iniciales = iniciales(c.nombre());
            String rolTexto = c.rol() == Rol.ADMINISTRADOR ? "Administrador" : "Usuario";
            // Pestañas de texto con la activa resaltada, como en el mock
            String[][] tabs = c.rol() == Rol.ADMINISTRADOR
                    ? new String[][]{{"Inicio","/"}, {"Empresas","/empresas"}, {"Reservas","/reservas"}, {"Visitas","/visitas"}, {"Pendientes","/pendientes"}, {"Usuarios","/usuarios"}}
                    : new String[][]{{"Inicio","/"}, {"Empresas","/empresas"}, {"Reservas","/reservas"}, {"Visitas","/visitas"}, {"Pendientes","/pendientes"}};
            StringBuilder nav = new StringBuilder();
            if (!c.temporal()) {
                for (String[] tab : tabs) {
                    nav.append("<a class=\"nav-tab\" href=\"").append(tab[1]).append("\">").append(tab[0]).append("</a>");
                }
            }
            cabecera = "<header class=\"top\"><div class=\"wrap\">"
                    // Botón Atrás + logo con ícono
                    + "<div class=\"row\" style=\"gap:10px;flex-wrap:nowrap\">"
                    + (atras ? "<button class=\"btn\" onclick=\"history.back()\" aria-label=\"Atrás\">← <span>Atrás</span></button>" : "")
                    + "<a class=\"logo-cabecera\" href=\"/\" style=\"text-decoration:none\">" + logo(26) + "</a></div>"
                    // Pestañas de navegación
                    + "<nav class=\"nav\" aria-label=\"Menú principal\">" + nav + "</nav>"
                    // Nombre + rol + avatar + Salir (Cambiar contraseña dentro del menú de cuenta)
                    + "<div class=\"who\"><div class=\"meta\" style=\"text-align:right\"><div>"
                    + escapar(c.nombre()) + "</div><div class=\"muted small\">" + escapar(rolTexto) + "</div></div>"
                    + "<a href=\"/cuenta/contrasena\" class=\"avatar\" title=\"Cambiar contraseña\">" + escapar(iniciales) + "</a>"
                    + "<form method=\"post\" action=\"/logout\" style=\"margin:0\">" + csrf(s)
                    + "<button class=\"btn\">Salir</button></form></div>"
                    + "</div></header>";
        }
        // Una sola salida de navegación; los pasos internos del asistente se conservan.
        if (!cabecera.isEmpty()) contenido = contenido.replaceAll("<a\\b[^>]*>\\s*(?:←[^<]*|Volver (?:a |al )[^<]*)</a>", "");
        String pagehead = conPagehead
                ? encabezado(seccion(titulo), titulo, "")
                : "";
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<title>" + escapar(titulo) + " · APLOMO.</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Instrument+Serif&family=Inter:wght@300;400;500;600&display=swap\" rel=\"stylesheet\">"
                + "<link rel=\"stylesheet\" href=\"/static/css/aplomo.css\">"
                + "<script src=\"/static/js/interfaz.js\" defer></script><script src=\"/static/js/progreso.js\" defer></script><script src=\"/static/js/cuenta.js\" defer></script><script src=\"/static/js/reservas.js\" defer></script></head><body>" + "<a class=\"saltar\" href=\"#contenido\">Saltar al contenido</a>" + cabecera + (cabecera.isEmpty()?"":laterales())
                + "<main id=\"contenido\" class=\"wrap\" tabindex=\"-1\">" + pagehead + contenido + "</main>" + (cabecera.isEmpty()?"":pie()) + "</body></html>";
    }

    private static String icono(int altura) { return String.format(LOGO_SVG,altura,altura,0,0).replace(" style=\"margin-bottom:0px;margin-right:0px\"",""); }

    /** Estado vacío de marca; conserva las acciones ya autorizadas por la página. */
    public static String vacio(String texto,String acciones) {
        return "<div class=\"empty marca\">"+icono(52)+"<p>"+escapar(texto)+"</p>"+acciones+"</div>";
    }

    /** Panel cosmético junto a un formulario, sin modificar sus campos ni su envío. */
    public static String conPanel(String formulario,String lema,String consejo) {
        return "<div class=\"con-panel\"><div class=\"panel-formulario\">"+formulario+"</div><aside class=\"panel-marca\"><div><p class=\"lema\">"+escapar(lema)+"</p><div class=\"consejo\"><b>Consejo RULA</b><p>"+escapar(consejo)+"</p></div></div></aside></div>";
    }

    private static String pie() {
        return "<footer class=\"pie\"><div class=\"wrap\"><span class=\"marca\" role=\"img\" aria-label=\"APLOMO.\">"+icono(20)+"<span>PLOMO.</span></span><span class=\"lema\">Ergonomía con precisión · Método RULA</span><span>© 2026 APLOMO</span></div></footer>";
    }

    private static String laterales() {
        StringBuilder h=new StringBuilder("<div class=\"riel riel-izq\" aria-hidden=\"true\"><span class=\"marca-v\">APLOMO.</span><span class=\"lema-v\">Ergonomía con precisión</span></div><div class=\"deco deco-izq\" aria-hidden=\"true\"><svg viewBox=\"0 0 300 300\"><path class=\"dc-h\" d=\"M20,150 A130,130 0 0 1 280,150\"/><g class=\"dc-h\">");
        for(int a=-90;a<=90;a+=5){double r=a%15==0?116:123,v=Math.toRadians(a);h.append(String.format(java.util.Locale.ROOT,"<line x1=\"%.3f\" y1=\"%.3f\" x2=\"%.3f\" y2=\"%.3f\"/>",150+r*Math.sin(v),150-r*Math.cos(v),150+130*Math.sin(v),150-130*Math.cos(v)));}
        h.append("</g><line class=\"dc-p\" x1=\"150\" y1=\"0\" x2=\"150\" y2=\"300\"/><path class=\"dc-arc\" pathLength=\"1\" d=\"M150,90 A60,60 0 0 1 184.41,100.85\"/><g class=\"dc-aguja\"><line x1=\"150\" y1=\"150\" x2=\"150\" y2=\"34\"/><circle class=\"dc-f\" cx=\"150\" cy=\"34\" r=\"3.2\"/></g><circle class=\"dc-f\" cx=\"150\" cy=\"150\" r=\"4\"/></svg></div><div class=\"deco deco-der\" aria-hidden=\"true\"><svg viewBox=\"0 0 300 380\"><line class=\"dc-p\" x1=\"150\" y1=\"0\" x2=\"150\" y2=\"184\"/><path class=\"dc-h\" d=\"M118.81,336.72 A150,150 0 0 0 181.19,336.72\"/><g class=\"dc-h\">");
        for(int a=-12;a<=12;a+=6){double v=Math.toRadians(a);h.append(String.format(java.util.Locale.ROOT,"<line x1=\"%.3f\" y1=\"%.3f\" x2=\"%.3f\" y2=\"%.3f\"/>",150+150*Math.sin(v),190+150*Math.cos(v),150+158*Math.sin(v),190+158*Math.cos(v)));}
        return h.append("</g><g class=\"dc-logo\" transform=\"translate(50,170) scale(2)\"><line x1=\"50\" y1=\"10\" x2=\"16\" y2=\"90\" stroke-width=\"4\"/><line x1=\"50\" y1=\"10\" x2=\"84\" y2=\"90\" stroke-width=\"4\"/><path d=\"M31.2,54.2 A48,48 0 0 0 68.8,54.2\" stroke-width=\"2.2\" stroke-dasharray=\"3.6 3.6\" opacity=\".85\"/></g><g class=\"dc-plomo\"><g class=\"dc-logo\" transform=\"translate(50,170) scale(2)\"><line x1=\"50\" y1=\"10\" x2=\"50\" y2=\"64\" stroke-width=\"1.6\"/><path class=\"dc-f\" d=\"M47.84,64 h4.32 v2.16 L55.04,70.48 L50,82.72 L44.96,70.48 L47.84,66.16 Z\"/></g></g><circle class=\"dc-f\" cx=\"150\" cy=\"190\" r=\"7.2\"/></svg></div>").toString();
    }

    /** RF-020: etiqueta completa con color y nivel leídos de MySQL, sin recalcular RULA. */
    public static String riesgo(Integer puntaje,String color,int nivel) {
        if(puntaje==null)return "<span class=\"tag\">Sin resultado</span>";
        return "<span class=\"pill "+escapar(color)+"\">"+puntaje+" · "+escapar(Formularios.etiqueta(color))+" · Nivel "+nivel+"</span>";
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
