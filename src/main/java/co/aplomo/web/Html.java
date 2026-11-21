package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * RNF-007, RNF-011: HTML común; todos los datos variables pasan por escapar.
 */
public final class Html {
    private Html() {
    }

    /**
     * Zona horaria del sistema (RNF-007).
     */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov"
            , "dic"};

    /**
     * Formato "20 nov 2026" como en el mock (RNF-007).
     */
    public static String fecha(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Formato "20 nov 2026 14:00" para marcas de tiempo (RNF-007).
     */
    public static String fechaHora(LocalDateTime valor) {
        if (valor == null) return "—";
        return fecha(valor.toLocalDate()) + " " + String.format("%d:%02d", valor.getHour(), valor.getMinute());
    }

    /**
     * Hoy en la zona horaria del sistema (RNF-007).
     */
    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    /**
     * SVG del ícono A-plomada del logo (identidad visual del mock).
     */
    private static final String LOGO_SVG =
            "<svg width=\"%d\" height=\"%d\" viewBox=\"0 0 100 100\" style=\"margin-botto" +
                    "m:%dpx;margin-right:%dpx\" aria-hidden=\"true\">" +
                    "<line x1=\"50\" y1=\"10\" x2=\"16\" y2=\"90\" stroke=\"currentColor\" stroke" +
                    "-width=\"4\" stroke-linecap=\"round\"/>" +
                    "<line x1=\"50\" y1=\"10\" x2=\"84\" y2=\"90\" stroke=\"currentColor\" stroke" +
                    "-width=\"4\" stroke-linecap=\"round\"/>" +
                    "<path d=\"M31.2,54.2 A48,48 0 0 0 68.8,54.2\" fill=\"none\" stroke=\"current" +
                    "Color\" stroke-width=\"2.2\" stroke-dasharray=\"3.6 3.6\" opacity=\".85\"/>" +
                    "<line x1=\"50\" y1=\"10\" x2=\"50\" y2=\"64\" stroke=\"currentColor\" stroke" +
                    "-width=\"1.6\"/>" +
                    "<path d=\"M47.84,64 h4.32 v2.16 L55.04,70.48 L50,82.72 L44.96,70.48 L47.84,6" +
                    "6.16 Z\" fill=\"currentColor\"/>" +
                    "<circle cx=\"50\" cy=\"10\" r=\"3.6\" fill=\"currentColor\"/></svg>";

    /**
     * Favicon A con plomada, compartido con el logo de la app (Paso 17). RNF-011.
     */
    public static String favicon() {
        return icono(64).replace("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" ")
                .replace("currentColor", "#EDE6D8");
    }

    /**
     * Error legible con identidad de la app y una salida segura. RNF-011.
     */
    public static String error(String titulo, String mensaje) {
        return pagina(titulo, "<section class=\"card stack\">" + logo(42) + aviso(mensaje, false) +
                "<div><a class=\"btn p\" href=\"/\">Volver al inicio</a></div></section>", null);
    }

    /**
     * Logo con ícono y texto como en el mock, a la altura indicada en píxeles. RNF-011.
     */
    public static String logo(int altura) {
        int svgH = (int) (altura * 0.78);
        int marginB = (int) (altura * 0.19);
        int marginR = (int) (altura * 0.03);
        return "<span class=\"lockup\" style=\"font-size:" + altura + "px\" aria-label=\"ALINEO.\">" +
                String.format(LOGO_SVG, svgH, svgH, marginB, marginR) + "<span>LINEO.</span></span>";
    }

    /**
     * "1 reserva", "3 reservas": singular cuando n == 1, plural en otro caso (RNF-007).
     */
    public static String plural(int cantidad, String singular) {
        String plural = switch (singular) {
            case "evaluación" -> "evaluaciones";
            case "evaluador" -> "evaluadores";
            case "salón" -> "salones";
            default -> singular + "s";
        };
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    /**
     * Evita doble punto cuando un nombre ya termina en punto, como "S.A.S." (RNF-007).
     */
    public static String punto(String texto) {
        return texto != null && texto.endsWith(".") ? "" : ".";
    }

    /**
     * Escapa texto y atributos para que los datos no se ejecuten como HTML (RNF-004).
     */
    public static String escapar(String texto) {
        if (texto != null) texto = texto.replace("APLOMO · Práctica de evaluación", "ALINEO · Práctica de evaluación")
                .replace("Demostración APLOMO.", "Demostración ALINEO.")
                .replace("para presentar APLOMO", "para presentar ALINEO")
                .replace("calculados por APLOMO.", "calculados por ALINEO.");
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * Aviso persistente con botón de cierre; las confirmaciones duran cinco segundos (RNF-007).
     */
    public static String aviso(String mensaje, boolean confirmacion) {
        if (mensaje == null || mensaje.isBlank()) return "";
        return confirmacion ? "<div class=\"toast-exito\" role=\"status\" data-toast><b aria-hidden=\"true\">✓</b> " +
                escapar(mensaje) + "</div>" :
                "<div class=\"alerta\" role=\"alert\" data-alerta><span aria-hidden=\"true\">⚠</span> " +
                        escapar(mensaje) + "</div>";
    }

    /**
     * Inserta valores escapados en la portada conservada de la fase 1 (RF-001a).
     */
    public static String login(Sesion sesion2, String correo, String mensaje) throws IOException {
        try (var entrada = Html.class.getResourceAsStream("/plantillas/login.html")) {
            if (entrada == null) throw new IOException("No está disponible la página de ingreso.");

            // Reemplazar solo huecos originales impide interpretar texto del usuario como plantilla.
            String plantilla = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            var patron = java.util.regex.Pattern.compile("\\{\\{(mensaje|csrf|correo|contacto)\\}\\}");
            var matcher = patron.matcher(plantilla);
            return matcher.replaceAll(elemento -> java.util.regex.Matcher.quoteReplacement(switch (elemento.group(1)) {
                case "mensaje" -> aviso(mensaje, false);
                case "csrf" -> escapar(sesion2.csrf());
                case "contacto" -> contactoLogin();
                default -> escapar(correo);
            }));
        }
    }

    private static String notas(Sesion sesion) {
        if (sesion == null || sesion.cuenta() == null || sesion.cuenta().temporal()) return "";
        return "<div data-notas-csrf=\"" + escapar(sesion.csrf()) +
                "\"></div><dialog class=\"avisos-panel stack\" data-panel-avisos " +
                "aria-label=\"Avisos\"><div data-contenido-avisos></div></dialog>" +
                "<script src=\"/static/js/notas.js\" defer></script>";
    }

    private static String contactoLogin() {
        var contacto = co.aplomo.config.Contacto.leer(
                java.nio.file.Path.of(System.getProperty("aplomo.contacto.archivo", "config/contacto.properties")));
        return contacto.whatsapp().isBlank() ? "/ayuda" : "https://wa.me/" + contacto.whatsapp();
    }

    /**
     * Genera una página con cabecera igual al mock: atrás, logo, pestañas, nombre+avatar, salir (RF-002).
     */
    public static String pagina(String titulo, String contenido, Sesion sesion2) {
        return pagina(titulo, contenido, sesion2, true);
    }

    /**
     * Encabezado de sección con las acciones de página agrupadas junto al título. RNF-011.
     */
    public static String encabezado(String seccion, String titulo, String acciones) {
        return "<div class=\"pagehead\"><div><span class=\"eyebrow\">" + escapar(seccion) + "</span><h1>" +
                escapar(titulo) + "</h1></div>" +
                (acciones.isEmpty() ? "" : "<div class=\"row pagehead-acciones\">" + acciones + "</div>") + "</div>";
    }

    /**
     * Presentación con contexto explícito, conservando la cabecera y las autorizaciones existentes. RNF-011.
     */
    public static String pagina(String titulo, String contenido, Sesion sesion2, String seccion, String acciones) {
        return pagina(titulo, contenido, sesion2, seccion, acciones, true);
    }

    /**
     * RF-002: distingue listas de detalles sin depender del nombre de la empresa.
     */
    public static String pagina(String titulo, String contenido, Sesion sesion2, String seccion, String acciones,
            boolean atras) {
        return pagina(titulo, encabezado(seccion, titulo, acciones) + contenido, sesion2, false, atras);
    }

    private static String seccion(String titulo) {
        String texto2 = titulo.toLowerCase(java.util.Locale.ROOT);
        if (texto2.contains("usuario")) return "Gestión · Usuarios";
        if (texto2.contains("empresa")) return "Gestión · Empresas";
        if (texto2.contains("sede")) return "Gestión · Sedes";
        if (texto2.contains("salón")) return "Gestión · Salones";
        if (texto2.contains("área")) return "Gestión · Áreas";
        if (texto2.contains("cargo")) return "Gestión · Cargos";
        if (texto2.contains("evaluación")) return "Evaluaciones · Detalle";
        if (texto2.contains("rula") || texto2.contains("analizar")) return "Evaluaciones · Resultado";
        if (texto2.contains("reserva")) return "Reservas · Formulario";
        if (texto2.contains("visita")) return "Visitas · Detalle";
        if (texto2.contains("puesto")) return "Gestión · Puestos";
        if (texto2.contains("contraseña")) return "Cuenta · Contraseña";
        if (texto2.contains("calificaci")) return "Consultas · Calificaciones";
        return "Gestión · " + titulo;
    }

    /**
     * Versión con control del pagehead para páginas que definen su propio encabezado (como Inicio). RNF-011.
     */
    public static String pagina(String titulo, String contenido, Sesion sesion2, boolean conPagehead) {
        return pagina(titulo, contenido, sesion2, conPagehead, true);
    }

    /**
     * Permite que Inicio omita Atrás sin afectar el resto de páginas. RNF-011.
     */
    public static String pagina(String titulo, String contenido, Sesion sesion2, boolean conPagehead, boolean atras) {
        contenido += notas(sesion2);
        String rolPagina = sesion2 == null || sesion2.cuenta() == null ? "publica" : sesion2.cuenta().rol().name()
                                                                                     .toLowerCase(Locale.ROOT);
        boolean enfoque = java.util.regex.Pattern.compile("<form\\b[^>]*\\bdata-(?:conversacion|evaluacion)(?:[=>\\s])")
                .matcher(contenido).find();
        String cabecera = enfoque ? cabeceraEnfoque(contenido) : cabecera(sesion2, atras);

        // Una sola salida de navegación; los pasos internos del asistente se conservan.
        if (!cabecera.isEmpty())
            contenido = contenido.replaceAll("<a\\b[^>]*>\\s*(?:←[^<]*|Volver (?:a |al )[^<]*)</a>", "");
        String pagehead = conPagehead ? encabezado(seccion(titulo), titulo, "") : "";
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewpor" + "t-fit=cover\">" +
                "<title>" + escapar(titulo) + " · ALINEO.</title>" +
                "<link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.svg\">" +
                "<link href=\"https://fonts.googleapis.com/css2?family=Instrument+Serif&famil" +
                "y=Inter:wght@300;400;500;600&display=swap\" rel=\"stylesheet\">" +
                "<link rel=\"stylesheet\" href=\"/static/css/aplomo.css\">" +
                "<script src=\"/static/js/interfaz.js\" defer></script><script src=\"/static/" +
                "js/progreso.js\" defer></script><script src=\"/static/js/cuenta.js\" defer><" +
                "/script><script src=\"/static/js/reservas.js\" defer></script>" +
                "<script src=\"/static/js/tiempo-colombia.js\" defer></script>" +
                "<script src=\"/static/js/sistema-visual.js\" defer></script><script src=\"/static/js/mostrar-clave" +
                ".js\" defer></script></head><body class=\"alineo rol-" + rolPagina + (enfoque ? " modo-enfoque" : "") +
                "\">" + "<a class=\"saltar\" href=\"#contenido\">Saltar al contenido</a>" + cabecera +
                (cabecera.isEmpty() ? "" : laterales()) + "<main id=\"contenido\" class=\"wrap\" tabindex=\"-1\">" +
                pagehead + contenido + "</main>" + (cabecera.isEmpty() ? "" : pie()) + "</body></html>";
    }

    private static String cabeceraEnfoque(String contenido) {
        var lugar = java.util.regex.Pattern.compile("data-enfoque-lugar=\"([^\"]*)\"").matcher(contenido);
        var visita = java.util.regex.Pattern.compile("data-enfoque-visita=\"([0-9]+)\"").matcher(contenido);
        String nombre = lugar.find() ? lugar.group(1) : "Revisión de la postura";
        String volver = visita.find() ? "/visitas/" + visita.group(1) : "/visitas";
        return "<header class=\"top enfoque-cabecera\"><div class=\"wrap\"><a class=\"logo-cabecera\" href=\"/\">" +
                logo(26) + "</a><span class=\"enfoque-lugar\">Evaluación · " + nombre +
                "</span><a class=\"btn\" href=\"" + volver + "\">Salir</a></div></header>";
    }

    private static String cabecera(Sesion sesion2, boolean atras) {
        String cabecera = "";
        if (sesion2 != null && sesion2.cuenta() != null) {
            Cuenta cuenta = sesion2.cuenta();
            String iniciales = iniciales(cuenta.nombre());
            String rolTexto = Formularios.etiqueta(cuenta.rol().name());

            // Pestañas de texto con la activa resaltada, como en el mock
            String nav = menu(cuenta);
            cabecera = "<header class=\"top\"><div class=\"wrap\">"

                    // Botón Atrás + logo con ícono
                    + "<div class=\"row\" style=\"gap:10px;flex-wrap:nowrap\">" + (atras ?
                    "<button class=\"btn\" onclick=\"history.back()\" aria-label=\"Atrás\">← <spa" +
                    "n>Atrás</span></button>" : "") +
                    "<a class=\"logo-cabecera\" href=\"/\" style=\"text-decoration:none\">" + logo(26) + "</a></div>"

                    // Pestañas de navegación
                    + "<nav class=\"nav\" aria-label=\"Menú principal\">" + nav + "</nav>"

                    // Nombre + rol + avatar + Salir (Cambiar contraseña dentro del menú de cuenta)
                    + "<div class=\"who\"><div class=\"meta\" style=\"text-align:right\"><div>" +
                    escapar(cuenta.nombre()) + "</div><div class=\"muted small\">" + escapar(rolTexto) +
                    "</div></div>" + "<a class=\"btn ayuda-cabecera\" href=\"/ayuda\">Ayuda</a>" +
                    "<a class=\"avatar bandeja-boton\" href=\"/avisos\" data-abrir-avisos aria-label=\"Avisos\">" +
                    "<svg width=\"22\" height=\"22\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" " +
                    "aria-hidden=\"true\"><path d=\"M4 4h16v16H4z M4 14h5l1 3h4l1-3h5\"/></svg>" +
                    "<span class=\"avisos-contador\" data-contador-avisos hidden></span></a>" +
                    "<a href=\"/cuenta\" class=\"avatar\" title=\"Mi cuenta\">" + escapar(iniciales) + "</a>" +
                    "<form method=\"post\" action=\"/logout\" style=\"margin:0\">" + csrf(sesion2) +
                    "<button class=\"btn\">Salir</button></form></div>" + "</div></header>";
        }

        return cabecera;
    }

    private static String menu(Cuenta cuenta) {
        String[][] tabs = cuenta.rol() ==
                Rol.ADMINISTRADOR ? new String[][]{{"Inicio", "/"}, {"Agenda", "/agenda"}, {"Empresas", "/empresas"},
                {"Reservas", "/reservas"}, {"Visitas", "/visitas"}, {"Pendientes", "/pendientes"}, {"Usuarios",
                "/usuarios"}, {"Archivo", "/archivo"}} :
                cuenta.rol() ==
                        Rol.EVALUADOR ? new String[][]{{"Inicio", "/"}, {"Mi agenda", "/agenda"}, {"Empresas",
                        "/empresas"}, {"Reservas", "/reservas"}, {"Visitas", "/visitas"}, {"Pendientes", "/pendientes"
                }, {"Archivo", "/archivo"}} : new String[][]{{"Inicio", "/"}, {"Empresas", "/empresas"}, {"Reservas",
                        "/reservas"}, {"Visitas", "/visitas"}, {"Pendientes", "/pendientes"}, {"Archivo", "/archivo"}};
        StringBuilder nav = new StringBuilder();
        if (!cuenta.temporal()) {
            for (String[] tab : tabs) {
                nav.append("<a class=\"nav-tab\" href=\"").append(tab[1]).append("\">").append(tab[0]).append("</a>");
            }
        }
        return nav.toString();
    }

    private static String icono(int altura) {
        return String.format(LOGO_SVG, altura, altura, 0, 0)
                .replace(" style=\"margin-bottom:0px;margin-right:0px\"", "");
    }

    /**
     * Estado vacío de marca; conserva las acciones ya autorizadas por la página. RNF-011.
     */
    public static String vacio(String texto, String acciones) {
        return "<div class=\"empty marca\">" + icono(52) + "<p>" + escapar(texto) + "</p>" + acciones + "</div>";
    }

    /**
     * Panel cosmético junto a un formulario, sin modificar sus campos ni su envío. RNF-011.
     */
    public static String conPanel(String formulario, String lema, String consejo) {
        return "<div class=\"con-panel\"><div class=\"panel-formulario\">" + formulario +
                "</div><aside class=\"panel-marca\"><div><p class=\"lema\">" + escapar(lema) +
                "</p><div class=\"consejo\"><b>Consejo RULA</b><p>" + escapar(consejo) +
                "</p></div></div></aside></div>";
    }

    private static String pie() {
        return "<footer class=\"pie\"><div class=\"wrap\"><span class=\"marca\" role=\"img\"" +
                " aria-label=\"ALINEO.\">" + icono(20) +
                "<span>LINEO.</span></span><span class=\"lema\">Ergonomía con precisión · Mét" +
                "odo RULA</span><span>© 2026 ALINEO</span></div></footer>";
    }

    private static String laterales() {
        StringBuilder contenido2 = new StringBuilder(
                "<div class=\"riel riel-izq\" aria-hidden=\"true\"><span class=\"marca-v\">AL" +
                        "INEO.</span><span class=\"lema-v\">Ergonomía con precisión</span></div><div " +
                        "class=\"deco deco-izq\" aria-hidden=\"true\"><svg viewBox=\"0 0 300 300\"><p" +
                        "ath class=\"dc-h\" d=\"M20,150 A130,130 0 0 1 280,150\"/><g class=\"dc-h\">");
        for (int a = -90; a <= 90; a += 5) {
            double valor2 = a % 15 == 0 ? 116 : 123, valor3 = Math.toRadians(a);
            contenido2.append(
                    String.format(java.util.Locale.ROOT, "<line x1=\"%.3f\" y1=\"%.3f\" x2=\"%.3f\" y2=\"%.3f\"/>",
                            150 + valor2 * Math.sin(valor3), 150 - valor2 * Math.cos(valor3),
                            150 + 130 * Math.sin(valor3), 150 - 130 * Math.cos(valor3)));
        }
        contenido2.append("</g><line class=\"dc-p\" x1=\"150\" y1=\"0\" x2=\"150\" y2=\"300\"/><path cl" +
                "ass=\"dc-arc\" pathLength=\"1\" d=\"M150,90 A60,60 0 0 1 184.41,100.85\"/><g" +
                " class=\"dc-aguja\"><line x1=\"150\" y1=\"150\" x2=\"150\" y2=\"34\"/><circl" +
                "e class=\"dc-f\" cx=\"150\" cy=\"34\" r=\"3.2\"/></g><circle class=\"dc-f\" " +
                "cx=\"150\" cy=\"150\" r=\"4\"/></svg></div><div class=\"deco deco-der\" aria" +
                "-hidden=\"true\"><svg viewBox=\"0 0 300 380\"><line class=\"dc-p\" x1=\"150" +
                "\" y1=\"0\" x2=\"150\" y2=\"184\"/><path class=\"dc-h\" d=\"M118.81,336.72 A" +
                "150,150 0 0 0 181.19,336.72\"/><g class=\"dc-h\">");
        for (int a = -12; a <= 12; a += 6) {
            double valor4 = Math.toRadians(a);
            contenido2.append(
                    String.format(java.util.Locale.ROOT, "<line x1=\"%.3f\" y1=\"%.3f\" x2=\"%.3f\" y2=\"%.3f\"/>",
                            150 + 150 * Math.sin(valor4), 190 + 150 * Math.cos(valor4), 150 + 158 * Math.sin(valor4),
                            190 + 158 * Math.cos(valor4)));
        }
        return contenido2.append("</g><g class=\"dc-logo\" transform=\"translate(50,170) scale(2)\"><line x1=" +
                "\"50\" y1=\"10\" x2=\"16\" y2=\"90\" stroke-width=\"4\"/><line x1=\"50\" y1=" +
                "\"10\" x2=\"84\" y2=\"90\" stroke-width=\"4\"/><path d=\"M31.2,54.2 A48,48 0" +
                " 0 0 68.8,54.2\" stroke-width=\"2.2\" stroke-dasharray=\"3.6 3.6\" opacity=" +
                "\".85\"/></g><g class=\"dc-plomo\"><g class=\"dc-logo\" transform=\"translat" +
                "e(50,170) scale(2)\"><line x1=\"50\" y1=\"10\" x2=\"50\" y2=\"64\" stroke-wi" +
                "dth=\"1.6\"/><path class=\"dc-f\" d=\"M47.84,64 h4.32 v2.16 L55.04,70.48 L50" +
                ",82.72 L44.96,70.48 L47.84,66.16 Z\"/></g></g><circle class=\"dc-f\" cx=\"15" +
                "0\" cy=\"190\" r=\"7.2\"/></svg></div>").toString();
    }

    /**
     * RF-020: etiqueta completa con color y nivel leídos de MySQL, sin recalcular RULA.
     */
    public static String riesgo(Integer puntaje, String color, int nivel) {
        return SistemaVisual.riesgo(puntaje, color, nivel);
    }

    /**
     * RF-002, RNF-011: iniciales compartidas, sin títulos profesionales, máximo dos letras.
     */
    public static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String limpio = nombre.strip()
                .replaceFirst("(?iu)^(?:(?:Ing\\.?|Ingenier[oa]|Dr\\.?|Dra\\.?|Arq\\.?|Lic\\.?)\\s+)+", "");
        String[] partes = limpio.split("\\s+");
        StringBuilder ini = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) ini.appendCodePoint(partes[i].codePointAt(0));
        }
        return ini.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Incluye el token de la sesión en un formulario POST (RNF-004).
     */
    public static String csrf(Sesion sesion2) {
        return "<input type=\"hidden\" name=\"csrf\" value=\"" + escapar(sesion2.csrf()) + "\">";
    }

    /**
     * Campo de texto con etiqueta accesible y valor escapado (RNF-002, RNF-004).
     */
    public static String campo(String nombre, String etiqueta, String tipo, String valor, int maximo) {
        return "<div class=\"field\"><label for=\"" + escapar(nombre) + "\">" + escapar(etiqueta) +
                "</label><input class=\"input\" id=\"" + escapar(nombre) + "\" name=\"" + escapar(nombre) +
                "\" type=\"" + escapar(tipo) + "\" value=\"" + escapar(valor) + "\" maxlength=\"" + maximo +
                "\" required" + (tipo.equals("password") ? " autocomplete=\"new-password\"" : "") + "></div>";
    }

    /**
     * Formulario compartido para cambio obligatorio y voluntario (RF-001c).
     */
    public static String contrasena(Sesion sesion2, String error) {
        String aviso = sesion2.cuenta().temporal() ? aviso("Tu contraseña es temporal. Cámbiala para continuar.",
                false) : "";
        return pagina("Cambiar contraseña", aviso + aviso(error, false) +
                "<form class=\"card stack cuenta-form\" method=\"post\" action=\"/cuenta/cont" + "rasena\">" +
                csrf(sesion2) +
                campo("actual", "Contraseña actual", "password", "", 100).replace("new-password", "current-password") +
                campo("nueva", "Nueva contraseña", "password", "", 100) +
                campo("confirmacion", "Confirmar nueva contraseña", "password", "", 100) +
                "<p class=\"hint\">Al menos 8 caracteres, una letra y un número.</p><button c" +
                "lass=\"btn p\">Guardar contraseña</button></form>", sesion2, true, !sesion2.cuenta().temporal());
    }
}
