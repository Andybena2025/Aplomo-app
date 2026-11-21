package co.aplomo.web;

import co.aplomo.servicio.Evaluaciones.Contexto;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.HexFormat;
import java.util.List;

import static co.aplomo.web.Html.escapar;

/**
 * RF-014/015/016, RNF-002/007: primeros tres pasos con los componentes del mock.
 */
public final class PaginasEvaluacion {
    private PaginasEvaluacion() {
    }

    /**
     * Etiquetas y ayudas de las seis condiciones aprobadas de RF-015, en el orden del esquema.
     */
    public static final String[][] CONDICIONES = {{"escritorio", "Escritorio muy alto", "La superficie queda por " +
            "encima del codo con los hombros relajados."}, {"pantalla", "Pantalla muy alta",
            "El borde superior queda" + " por encima de la línea de los ojos."}, {"pies", "Pies colgando",
            "Los pies no apoyan completos en el " +
                    "piso."}, {"apoyapies", "Apoyapiés", "El puesto tiene apoyapiés."}, {"apoyabrazos",
            "Silla con " + "apoyabrazos", "La silla tiene apoyabrazos."}, {"regulable", "Silla regulable",
            "La silla se puede " + "regular."}};

    /**
     * Recorrido guiado de contexto y fotos; conserva provisionalmente la medición existente. RNF-011.
     */
    public static String nueva(Sesion sesion, Contexto contexto) {
        return nueva(sesion, contexto, 0);
    }

    /**
     * RNF-011: operación del componente.
     */
    public static String nueva(Sesion sesion, Contexto contexto, int seleccionado) {
        return RevisionEvaluacion.nueva(sesion, contexto, seleccionado);
    }

    /**
     * Opciones con pista vinculada y nombre independiente de su descripción.
     */
    static String opcion(String nombre, String valor, String etiqueta, String imagen, boolean multiple,
            String atributos, String ayuda) {
        String img = !imagen.isEmpty() && PaginasEvaluacion.class.getResource("/static/img/tour/" + imagen) != null ?
                "<img src=\"/static/img/tour/" + imagen + "\" alt=\"\" loading=\"lazy\">" : "";
        String id = "tour-" + nombre + "-" +
                HexFormat.of().formatHex(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String pista = ayuda.isEmpty() ? "" :
                "<small class=\"tour-ayuda\" id=\"" + id + "-ayuda\">" + escapar(ayuda) + "</small>";
        return "<label class=\"tour-opcion" + (imagen.startsWith("postura_") ? " tour-postura" : "") +
                "\"><input type=\"" + (multiple ? "checkbox" : "radio") + "\" name=\"" + nombre + "\" value=\"" +
                escapar(valor) + "\" " + atributos + " aria-labelledby=\"" + id + "-texto\"" +
                (ayuda.isEmpty() ? "" : " aria-describedby=\"" + id + "-ayuda\"") + "><span>" + img +
                "<span class=\"tour-franja\"><span id=\"" + id + "-texto\">" + escapar(etiqueta) + "</span>" + pista +
                "</span></span></label>";
    }

    /**
     * Nueve fases estables; las filas y fotos no crean fases adicionales.
     */
    static int fase(int paso) {
        return Math.max(1, Math.min(9, paso));
    }

    static String fases(String html) {
        return java.util.regex.Pattern.compile("data-paso=\"(\\d+)\"").matcher(html).replaceAll(
                elemento -> elemento.group() + " data-fase=\"" + fase(Integer.parseInt(elemento.group(1))) + "\"");
    }

    static List<String> posturasDelModo(String modo) {
        return modo.equals("Oficina") ? List.of("computador", "sentado", "inclinado", "brazos_arriba", "herramienta",
                "otra") : List.of("de_pie", "cargando", "inclinado", "brazos_arriba", "herramienta", "agachado",
                "otra");
    }

    /**
     * Muestra los campos guardados y abre las fotos exclusivamente por su ruta protegida. RNF-011.
     */
    public static String detalle(Sesion sesion, co.aplomo.servicio.Evaluaciones.Consulta consulta,
            List<co.aplomo.dao.AnalisisDao.Resumen> analisis, String aviso) {
        var evaluacion = consulta.ficha();
        var evaluacion2 = evaluacion.datos();
        StringBuilder contenido = new StringBuilder(Html.aviso(aviso, true)).append(
                PaginasEmpresas.enlace("/visitas/" + evaluacion.visita(), "← Volver a la visita", false));
        contenido.append("<section class=\"card stack\"><span class=\"eyebrow\">")
                .append(Html.fecha(evaluacion.fecha())).append(" · Evaluada por: ")
                .append(escapar(evaluacion.responsable() == null ? "Sin registro" : evaluacion.responsable()))
                .append("</span><h2>").append(escapar(evaluacion2.postura())).append("</h2><p>")
                .append(evaluacion2.criterio()
                        .equals("MAS_INADECUADA") ? "Postura más inadecuada" : "Mayor tiempo de exposición")
                .append(" · ").append(evaluacion2.minutos()).append(" minutos por jornada</p><p>")
                .append(escapar(evaluacion2.motivo()))
                .append("</p><h3>Condiciones del puesto</h3><div class=\"opts\">");
        boolean[] marcadas = {evaluacion2.escritorio(), evaluacion2.pantalla(), evaluacion2.pies(),
                evaluacion2.apoyapies(), evaluacion2.apoyabrazos(), evaluacion2.regulable()};
        for (int i = 0; i < CONDICIONES.length; i++)
            contenido.append("<span class=\"tag\">").append(CONDICIONES[i][1]).append(": ")
                    .append(marcadas[i] ? "Sí" : "No").append("</span>");
        contenido.append("</div><p class=\"texto-notas\">").append(escapar(evaluacion2.observaciones()))
                .append("</p><span class=\"tag\">")
                .append(evaluacion2.sinFotos() ? "Sin registro fotográfico" : "La persona aceptó las fotos")
                .append("</span></section><section class=\"stack\"><h2>Fotos</h2>");
        if (evaluacion2.sinFotos())
            contenido.append("<p class=\"muted\">Esta evaluación no tiene registro fotográfico.</p>");
        contenido.append("<div class=\"grid2\">");
        for (var foto : consulta.fotos())
            contenido.append("<figure class=\"card stack foto-guardada\"><img src=\"/fotos/").append(foto.id())
                    .append("\" alt=\"").append(escapar(foto.descripcion())).append("\"><figcaption>")
                    .append(Formularios.etiqueta(foto.vista())).append(" · ").append(escapar(foto.descripcion()))
                    .append("</figcaption></figure>");
        contenido.append("</div></section><section class=\"stack\"><h2>Análisis guardados</h2>");
        if (analisis.isEmpty()) contenido.append(
                "<p class=\"muted\">Todavía no hay análisis. La evaluación conserva sus datos" + " sin puntaje.</p>");
        for (var elemento : analisis)
            contenido.append("<div class=\"card row between\"><div>Foto ").append(elemento.foto()).append(" · ")
                    .append(Formularios.etiqueta(elemento.lado())).append("<div class=\"hint\">")
                    .append(Html.fecha(elemento.fecha().toLocalDate())).append(" · Evaluada por: ")
                    .append(escapar(elemento.responsable())).append("</div></div><span class=\"pill ")
                    .append(elemento.color()).append("\">").append(elemento.puntaje()).append(" · ")
                    .append(Formularios.etiqueta(elemento.color())).append(" · Nivel ").append(elemento.nivel())
                    .append("</span>")
                    .append(PaginasEmpresas.enlace("/evaluaciones/" + evaluacion.id() + "/analisis/" + elemento.id(),
                            "Ver resultado", false)).append("</div>");
        contenido.append("</section>");
        if (consulta.enCurso() && !consulta.fotos().isEmpty()) contenido.append(
                PaginasEmpresas.enlace("/evaluaciones/" + evaluacion.id() + "/analizar",
                        "Analizar fotos / añadir análisis", true));
        return Html.pagina("Evaluación · Puesto " + evaluacion.numero(), contenido.toString(), sesion);
    }
}
