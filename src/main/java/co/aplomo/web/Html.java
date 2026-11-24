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
     * RNF-002: favicon en champán, con la A y su base graduada.
     */
    public static String favicon() {
        return Marca.icono().replace("currentColor", "#E3CDA0").replace("<svg ", "<svg style=\"color:#E3CDA0\" ");
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
        return "<span class=\"marca-completa\" style=\"--marca-alto:" + altura + "px\">" + Marca.logo() +
                "<span class=\"marca-corta\">" + Marca.icono() + "</span></span>";
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
            var patron = java.util.regex.Pattern.compile(
                    "\\{\\{(mensaje|csrf|correo|contacto|contacto_botones|logo)\\}\\}");
            var matcher = patron.matcher(plantilla);
            return matcher.replaceAll(elemento -> java.util.regex.Matcher.quoteReplacement(switch (elemento.group(1)) {
                case "logo" -> Marca.logo();
                case "mensaje" -> aviso(mensaje, false);
                case "csrf" -> escapar(sesion2.csrf());
                case "contacto" -> contactoLogin();
                case "contacto_botones" -> ContactoAyuda.botones(contactoConfigurado(), null);
                default -> escapar(correo);
            }));
        }
    }

    /**
     * RNF-002: presentación pública con la misma marca y las tres ideas del login, sin sesión.
     */
    public static String nosotros() {
        String contenido = "<section class=\"card stack nosotros-marca\"><div class=\"marca-llegada\">" + Marca.logo() +
                "</div><span class=\"eyebrow\">Ergonomía con precisión</span>" +
                "<h1>Quiénes somos</h1><p>Evaluamos posturas de trabajo con el método RULA.</p>" +
                "<div class=\"login-ideas\"><div><strong>Fotografía el puesto</strong></div>" +
                "<div><strong>Mide los ángulos</strong></div><div><strong>Conoce el riesgo</strong></div></div>" +
                "<a class=\"btn p\" href=\"/login\">Iniciar sesión</a></section>";
        return pagina("Quiénes somos", contenido, null, false, false);
    }

    private static String notas(Sesion sesion) {
        if (sesion == null || sesion.cuenta() == null || sesion.cuenta().temporal()) return "";
        return "<div data-notas-csrf=\"" + escapar(sesion.csrf()) +
                "\"></div><dialog class=\"avisos-panel stack\" data-panel-avisos " +
                "aria-label=\"Avisos\"><div data-contenido-avisos></div></dialog>" +
                "<dialog class=\"aviso-modal stack\" data-aviso-dialog aria-label=\"Mensaje\">" +
                "<h2 data-aviso-titulo>Aviso</h2><p data-aviso-texto></p><p class=\"hint\" role=\"alert\" " +
                "data-aviso-error></p><button class=\"btn p\" type=\"button\" data-aviso-entendido>Entendido</button>" +
                "<button class=\"btn\" type=\"button\" data-aviso-cerrar>Cerrar</button></dialog>" +
                "<script src=\"/static/js/notas.js\" defer></script>";
    }

    private static co.aplomo.config.Contacto contactoConfigurado() {
        return co.aplomo.config.Contacto.leer(
                java.nio.file.Path.of(System.getProperty("aplomo.contacto.archivo", "config/contacto.properties")));
    }

    private static String contactoLogin() {
        String whatsapp = contactoConfigurado().enlaceWhatsapp();
        return whatsapp.isBlank() ? "/ayuda" : escapar(whatsapp);
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

    /**
     * RF-024, RNF-002/007: encabezado único de detalle con regreso al listado y acciones propias. Estado, metadatos y
     * acciones son fragmentos escapados por sus presentadores.
     */
    public static String detalle(String seccion, String volver, String titulo, String estado, String meta,
                                 String acciones) {
        String etiqueta = seccion.equals("Mi cuenta") ? "Inicio" : seccion;
        return "<section class=\"detalle-cabecera\" data-detalle-seccion=\"" + escapar(seccion) +
                "\"><a class=\"detalle-volver\" href=\"" + escapar(volver) + "\">← " + escapar(etiqueta) +
                "</a><div class=\"detalle-titulo\"><div class=\"detalle-identidad\"><h1>" + escapar(titulo) + "</h1>" +
                estado + "</div><div class=\"detalle-meta\">" + meta + "</div>" +
                (acciones.isBlank() ? "" : "<div class=\"row detalle-acciones\">" + acciones + "</div>") +
                "</div></section>";
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
        contenido = ListasCortas.preparar(contenido);
        contenido += notas(sesion2);
        String rolPagina = sesion2 == null || sesion2.cuenta() == null ? "publica" : sesion2.cuenta().rol().name()
                                                                                     .toLowerCase(Locale.ROOT);
        boolean enfoque = java.util.regex.Pattern.compile("<form\\b[^>]*\\bdata-(?:conversacion|evaluacion)(?:[=>\\s])")
                .matcher(contenido).find();
        String pagehead = conPagehead ? encabezado(seccion(titulo), titulo, "") : "";
        boolean unificada = !enfoque && sesion2 != null && sesion2.cuenta() != null;
        CabeceraPagina.Datos datos = unificada ? CabeceraPagina.extraer(titulo,
                pagehead + contenido) : new CabeceraPagina.Datos(contenido, "", escapar(titulo), "", "");
        var detalle = java.util.regex.Pattern.compile("data-detalle-seccion=\"([^\"]*)\"").matcher(contenido);
        boolean esDetalle = !enfoque && detalle.find();
        if (esDetalle) {
            datos = new CabeceraPagina.Datos(contenido, "", detalle.group(1), "", "");
        }
        String cabecera = enfoque ? cabeceraEnfoque(contenido) : cabecera(sesion2, atras && !esDetalle, datos,
                esDetalle, titulo.equals("Inicio"));
        if (unificada) {
            contenido = datos.cuerpo();
            pagehead = "";
        }

        // Una sola salida de navegación; los pasos internos del asistente se conservan.
        if (!cabecera.isEmpty() && !esDetalle)
            contenido = contenido.replaceAll("<a\\b[^>]*>\\s*(?:←[^<]*|Volver (?:a |al )[^<]*)</a>", "");
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

    private static String cabecera(Sesion sesion2, boolean atras, CabeceraPagina.Datos datos, boolean detalle,
                                   boolean inicio) {
        String cabecera = "";
        if (sesion2 != null && sesion2.cuenta() != null) {
            Cuenta cuenta = sesion2.cuenta();
            String iniciales = iniciales(cuenta.nombre());
            String rolTexto = Formularios.etiqueta(cuenta.rol().name());

            // Pestañas de texto con la activa resaltada, como en el mock
            String nav = menu(cuenta);
            String etiqueta = datos.etiqueta().isBlank() ? escapar(rolTexto) : datos.etiqueta();
            if (cuenta.rol() == Rol.USUARIO && sesion2.etiquetaEmpresa() != null) {
                etiqueta = escapar(sesion2.etiquetaEmpresa());
            }
            cabecera = "<header class=\"top cabecera-unica\"><div class=\"wrap\">" + "<div class=\"cabecera-titulo\">" +
                    (atras ? "<button class=\"btn cabecera-atras\" onclick=\"history.back()\" aria-label=\"Atrás\">← " +
                             "Atrás</button>" : "") + "<span class=\"eyebrow\">" + datos.rotulo() +
                    "</span><div class=\"titulo-con-rol\">" + (detalle ?
                    "<p class=\"cabecera-seccion\">" + datos.titulo() + "</p>" : "<h1>" + datos.titulo() + "</h1>") +
                    (inicio && cuenta.rol() != Rol.USUARIO ?
                            "<p class=\"cabecera-rol-texto\">" + escapar(cuenta.rol().name()) + "</p>" : "") +
                    "</div></div>" +
                    "<nav class=\"nav\" aria-label=\"Menú principal\">" + nav + "</nav>" +
                    "<a class=\"marca-cabecera\" href=\"/\" aria-label=\"ALINEO.\">" + Marca.logo() + "</a>" +
                    "<a class=\"marca-cabecera-a\" href=\"/\" aria-label=\"ALINEO. Inicio\">" + Marca.lateral() + "</a>"

                    // Nombre + rol + avatar + Salir (Cambiar contraseña dentro del menú de cuenta)
                    + "<div class=\"who\"><a class=\"btn ayuda-cabecera\" href=\"/ayuda\">Ayuda</a>" +
                    "<a class=\"avatar bandeja-boton\" href=\"/avisos\" data-abrir-avisos aria-label=\"Avisos\">" +
                    "<svg width=\"22\" height=\"22\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" " +
                    "aria-hidden=\"true\"><path d=\"M4 4h16v16H4z M4 14h5l1 3h4l1-3h5\"/></svg>" +
                    "<span class=\"avisos-contador\" data-contador-avisos hidden></span></a>" +
                    "<a href=\"/cuenta\" class=\"avatar\" title=\"" + escapar(cuenta.nombre() + " · " + rolTexto) +
                    "\" aria-label=\"Mi cuenta · " + escapar(cuenta.nombre() + " · " + rolTexto) + "\">" +
                    escapar(iniciales) + "</a>" + "<form method=\"post\" action=\"/logout\" style=\"margin:0\">" +
                    csrf(sesion2) + "<button class=\"btn\">Salir</button></form></div>" + datos.acciones() +
                    "</div></header>" +
                    "<aside class=\"avisos-saludo aviso-franja wrap\" data-avisos-saludo hidden aria-live=\"polite\">" +
                    "<button type=\"button\" data-ver-aviso-saludo></button>" +
                    "<button type=\"button\" data-ocultar-aviso aria-label=\"Ocultar aviso\">×</button></aside>";
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
        return Marca.icono().replace("<svg ", "<svg width=\"" + altura + "\" height=\"" + altura + "\" ");
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
        return "<footer class=\"pie\"><div class=\"wrap\"><span class=\"lema\">Ergonomía con precisión · Método " +
                "RULA</span>" + "<span>© 2026 ALINEO™ · Todos los derechos reservados.</span></div></footer>";
    }

    private static String laterales() {
        return "<div class=\"riel riel-izq\" aria-hidden=\"true\"><span class=\"marca-v\">" + Marca.logo() +
                "</span><span class=\"lema-v\">Ergonomía con precisión</span></div>" +
                "<img class=\"deco-herramientas\" src=\"/static/img/herramientas_mesa.svg\" alt=\"\" " +
                "aria-hidden=\"true\">";
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
        String limpio = nombreSinTitulo(nombre);
        String[] partes = limpio.split("\\s+");
        StringBuilder ini = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) ini.appendCodePoint(partes[i].codePointAt(0));
        }
        return ini.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * RNF-007: nombre compartido sin títulos de profesión ni de representación.
     */
    public static String nombreSinTitulo(String nombre) {
        return nombre == null ? "" : nombre.strip().replaceFirst(
                "(?iu)^(?:(?:Ing\\.?|Ingenier[oa]|Dr\\.?|Dra\\.?|Arq\\.?|Lic\\.?|Rep\\.?|Representante)\\s+)+", "");
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
