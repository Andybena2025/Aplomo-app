package co.aplomo.web;

import co.aplomo.dao.SugerenciaDao.Contexto;
import co.aplomo.dao.SugerenciaDao.Ficha;
import co.aplomo.dao.PendienteDao.Cambio;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Sugerencia;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/**
 * RF-023a/b, RNF-002/007: componentes de sugerencias, tarjetas y formularios del mock.
 */
public final class PaginasSugerencias {
    private PaginasSugerencias() {
    }

    /**
     * Lista consultable por ambos roles; el servicio ya filtró empresa y visita. RNF-011.
     */
    public static String lista(Sesion sesion, List<Ficha> filas, Contexto contexto) {
        StringBuilder contenido = new StringBuilder(enlace("/pendientes", "← Pendientes", false));
        if (contexto != null) {
            contenido.append(contexto(contexto)).append(avisoMinimo(contexto, filas.size(), admin(sesion)));
            if (admin(sesion)) contenido.append("<div class=\"row acciones-sugerencias\">")
                    .append(enlace("/sugerencias/nueva?evaluacion=" + contexto.evaluacion(), "Escribir sugerencia",
                            true)).append("</div>");
        }
        contenido.append("<p class=\"muted\">").append(Html.plural(filas.size(), "sugerencia")).append("</p>");
        if (filas.isEmpty()) contenido.append(Html.vacio(
                "Aún no hay sugerencias disponibles. El análisis se conserva aunque no haya r" + "eglas que apliquen.",
                ""));
        else {
            contenido.append("<div class=\"tablewrap\"><table class=\"resp sugerencias-table\"><thead><tr>" +
                    (contexto == null ? "<th>Puesto</th>" : "") +
                    "<th>Sugerencia</th><th>Origen</th><th>Prioridad</th><th>Estado</th><th>Detal" +
                    "le</th></tr></thead><tbody>");
            for (var sugerencia : filas) {
                var sugerencia2 = sugerencia.sugerencia();
                var sugerencia3 = sugerencia.contexto();
                contenido.append("<tr>");
                if (contexto == null) celda(contenido, "Puesto",
                        "Puesto " + sugerencia3.numero() + "<div class=\"muted small\">" +
                                escapar(sugerencia3.empresaNombre()) + " · " + escapar(sugerencia3.sedeNombre()) +
                                " · " + escapar(sugerencia3.salonNombre()) + "</div>");
                celda(contenido, "Sugerencia", escapar(sugerencia2.titulo()) + "<div class=\"muted small\">" +
                        Formularios.etiqueta(sugerencia2.tipo()) + "</div>");
                celda(contenido, "Origen", origen(sugerencia2));
                celda(contenido, "Prioridad", Formularios.etiqueta(sugerencia2.prioridad()));
                celda(contenido, "Estado", estado(sugerencia2.estado()));
                celda(contenido, "Detalle", enlace("/sugerencias/" + sugerencia2.id(), "Ver sugerencia", false));
                contenido.append("</tr>");
            }
            contenido.append("</tbody></table></div>");
        }
        return pagina("Sugerencias", contenido.toString(), sesion);
    }

    /**
     * Detalla la mejora, su avance y la reserva de Reevaluación cuando todas están Hechas. RNF-011.
     */
    public static String detalle(Sesion sesion, Ficha sugerencia, List<Cambio> historia, boolean hechas, String aviso,
            boolean exito) {
        var sugerencia2 = sugerencia.sugerencia();
        var sugerencia3 = sugerencia.contexto();
        StringBuilder contenido = new StringBuilder(
                enlace("/sugerencias?evaluacion=" + sugerencia3.evaluacion(), "← Sugerencias de la evaluación",
                        false)).append(Html.aviso(aviso, exito)).append(contexto(sugerencia3));
        contenido.append("<section class=\"card stack\"><div class=\"row between\"><h2>")
                .append(escapar(sugerencia2.titulo())).append("</h2>")
                .append(sugerencia2.activo() ? estado(sugerencia2.estado()) : "<span class=\"tag\">Quitada</span>")
                .append("</div><span class=\"eyebrow\">").append(origen(sugerencia2))
                .append("</span><div class=\"sug texto-notas\">").append(escapar(sugerencia2.descripcion()))
                .append("</div><dl class=\"datos-cuenta\">")
                .append(dato("Tipo", Formularios.etiqueta(sugerencia2.tipo())))
                .append(dato("Prioridad", Formularios.etiqueta(sugerencia2.prioridad()))).append(dato("Fecha límite",
                        sugerencia2.limite() == null ? "Sin definir" : Html.fecha(sugerencia2.limite()))).append(
                                "</dl>");
        if (admin(sesion) && sugerencia2.activo()) contenido.append("<div class=\"row\">")
                .append(enlace("/sugerencias/" + sugerencia2.id() + "/editar", "Editar sugerencia", true))
                .append("<form data-sugerencia-form method=\"post\" action=\"/sugerencias/").append(sugerencia2.id())
                .append("/quitar\">").append(Html.csrf(sesion))
                .append("<button type=\"submit\" class=\"btn\">Quitar sugerencia</button><div class=" +
                        "\"aviso\" role=\"alert\" data-error-sugerencia hidden><span></span><button t" +
                        "ype=\"button\" class=\"link\" data-cerrar-error aria-label=\"Cerrar aviso\">" +
                        "Cerrar</button></div></form></div><form data-sugerencia-form class=\"row\" m" +
                        "ethod=\"post\" action=\"/sugerencias/").append(sugerencia2.id()).append("/estado\">")
                .append(Html.csrf(sesion))
                .append(Formularios.selector(Map.of("estado", sugerencia2.estado()), "estado", "Avance de la mejora",
                        opciones("PENDIENTE", "EN_PROCESO", "HECHA"), true))
                .append("<button type=\"submit\" class=\"btn\">Guardar estado</button><div class=\"av" +
                        "iso\" role=\"alert\" data-error-sugerencia hidden><span></span><button type=" +
                        "\"button\" class=\"link\" data-cerrar-error aria-label=\"Cerrar aviso\">Cerr" +
                        "ar</button></div></form>");
        contenido.append("</section><p class=\"hint\">Las sugerencias orientan mejoras del puesto; no " +
                "reemplazan un diagnóstico médico.</p>");
        if (hechas) contenido.append("<section class=\"card stack\"><h2>Mejoras completadas</h2><p>Todas las suger" +
                        "encias activas de este puesto están Hechas. El Administrador registra una re" +
                        "serva de Reevaluación con la fecha y el horario acordados con la empresa.</p" + ">")
                .append(admin(sesion) ? enlace(reevaluacion(sugerencia3), "Registrar reevaluación", true) : "")
                .append("</section>");
        contenido.append("<section class=\"stack\"><h2>Historial</h2>");
        for (var elemento : historia)
            contenido.append("<div class=\"card stack\"><span class=\"eyebrow\">")
                    .append(Html.fechaHora(elemento.fecha())).append("</span><b>")
                    .append(escapar(elemento.responsable())).append("</b><span>")
                    .append(escapar(Formularios.etiqueta(elemento.accion())))
                    .append(elemento.nuevo() == null ? "" : " · " + Formularios.etiqueta(elemento.nuevo()))
                    .append("</span><p>").append(escapar(elemento.detalle())).append("</p></div>");
        return pagina("Sugerencia",
                contenido.append("</section><script src=\"/static/js/sugerencias.js\" defer></script>").toString(),
                sesion);
    }

    /**
     * Conserva los campos al crear y editar, con los mismos controles del mock. RNF-011.
     */
    public static String formulario(Sesion sesion, Contexto sugerencia, Sugerencia sugerencia2,
            Map<String, String> enviados, String error) {
        Map<String, String> parametros = new HashMap<>();
        parametros.put("titulo", sugerencia2 == null ? "" : sugerencia2.titulo());
        parametros.put("descripcion", sugerencia2 == null ? "" : sugerencia2.descripcion());
        parametros.put("tipo", sugerencia2 == null ? "" : sugerencia2.tipo());
        parametros.put("prioridad", sugerencia2 == null ? "" : sugerencia2.prioridad());
        parametros.put("fecha",
                sugerencia2 == null || sugerencia2.limite() == null ? "" : sugerencia2.limite().toString());
        parametros.putAll(enviados);
        String ruta = sugerencia2 == null ?
                "/sugerencias?evaluacion=" + sugerencia.evaluacion() : "/sugerencias/" + sugerencia2.id();
        String texto2 = enlace("/sugerencias?evaluacion=" + sugerencia.evaluacion(), "← Volver a sugerencias", false) +
                contexto(sugerencia) + Html.aviso(error, false) +
                "<form data-sugerencia-form class=\"card stack cuenta-form\" method=\"post\" " + "action=\"" + ruta +
                "\">" + Html.csrf(sesion) + Formularios.campo(parametros, "titulo", "Título", "text", 120, true) +
                Formularios.texto(parametros, "descripcion", "Descripción", 65535, true) + "<div class=\"grid2\">" +
                Formularios.selector(parametros, "tipo", "Tipo",
                        opciones("MOBILIARIO", "POSTURA", "DESCANSOS", "CARGA", "OTRA"), true) +
                Formularios.selector(parametros, "prioridad", "Prioridad", opciones("ALTA", "MEDIA", "BAJA"), true) +
                "</div>" + Formularios.campo(parametros, "fecha", "Fecha límite (opcional)", "date", 10, false) +
                "<div class=\"aviso\" role=\"alert\" data-error-sugerencia hidden><span></spa" +
                "n><button type=\"button\" class=\"link\" data-cerrar-error aria-label=\"Cerr" +
                "ar aviso\">Cerrar</button></div><button class=\"btn p\" type=\"submit\">Guar" +
                "dar sugerencia</button></form><script src=\"/static/js/sugerencias.js\" defe" + "r></script>";
        return pagina(sugerencia2 == null ? "Escribir sugerencia" : "Editar sugerencia", texto2, sesion);
    }

    /**
     * RF-023a: muestra las propuestas guardadas con la clase .sug de la referencia.
     */
    public static String resumen(List<Ficha> lista, Contexto sugerencia) {
        StringBuilder contenido = new StringBuilder(
                "<section class=\"card stack\"><span class=\"eyebrow\">Sugerencias del sistem" + "a</span>");
        for (var sugerencia2 : lista)
            contenido.append("<div class=\"sug\"><div><span class=\"tag\">").append(origen(sugerencia2.sugerencia()))
                    .append("</span><p>").append(escapar(sugerencia2.sugerencia().descripcion()))
                    .append("</p></div></div>");
        return contenido.append(avisoMinimo(sugerencia, lista.size(), true))
                .append(enlace("/sugerencias?evaluacion=" + sugerencia.evaluacion(), "Ver y gestionar sugerencias",
                        false)).append("</section>").toString();
    }

    private static String avisoMinimo(Contexto sugerencia, int cantidad, boolean administrador) {
        return cantidad == 0 && !"VERDE".equals(sugerencia.color()) ? Html.aviso(administrador ?
                                                                                 "El análisis está guardado. Este " +
                                                                                 "resultado requiere al menos una " +
                                                                                 "sugerencia: " +
                                                                                 "escribe una para completar el " +
                                                                                 "seguimiento." :
                                                                                 "El análisis está guardado. El " +
                                                                                 "Administrador debe completar las " +
                                                                                 "sugerencias d" + "e este resultado.",
                false) : "";
    }

    /**
     * RF-024: tarjeta de contexto reutilizada al consultar una sola evaluación.
     */
    public static String contexto(Contexto sugerencia) {
        return "<section class=\"card stack\"><div class=\"row between\"><h2>Puesto " + sugerencia.numero() + "</h2>" +
                Html.riesgo(sugerencia.puntaje(), sugerencia.color(), sugerencia.nivel()) + "</div><p>" +
                escapar(sugerencia.empresaNombre()) + " · " + escapar(sugerencia.sedeNombre()) + " · " +
                escapar(sugerencia.salonNombre()) + "</p><p class=\"muted\">" + escapar(sugerencia.postura()) + "</p>" +
                enlace("/visitas/" + sugerencia.visita(), "Ver visita", false) + "</section>";
    }

    private static String reevaluacion(Contexto sugerencia) {
        return "/reservas/nueva?empresa=" + sugerencia.empresa() + "&sede=" + sugerencia.sede() + "&salon=" +
                sugerencia.salon() + "&categoria=REEVALUACION";
    }

    private static String origen(Sugerencia sugerencia) {
        return sugerencia.origen().equals("SISTEMA") ? "Sugerencia del sistema" : "Escrita por el Administrador";
    }

    private static String estado(String texto) {
        return "<span class=\"tag\">" +
                (texto.equals("HECHA") ? "Hecha" : texto.equals("EN_PROCESO") ? "En proceso" : "Pendiente") + "</span>";
    }

    private static List<Formularios.Opcion> opciones(String... val) {
        return Arrays.stream(val).map(texto -> new Formularios.Opcion(texto, Formularios.etiqueta(texto))).toList();
    }

    private static String dato(String texto, String texto2) {
        return "<div><dt>" + texto + "</dt><dd>" + escapar(texto2) + "</dd></div>";
    }

    private static String pagina(String texto, String texto2, Sesion sesion) {
        return Html.pagina(texto,
                "<div class=\"pagehead\"><div><span class=\"eyebrow\">Mejoras · Sugerencias</" + "span><h1>" +
                        escapar(texto) + "</h1></div></div>" + texto2, sesion, false);
    }

    private static void celda(StringBuilder contenido, String texto, String texto2) {
        contenido.append("<td data-label=\"").append(texto).append("\"><div class=\"cell-value\">").append(texto2)
                .append("</div></td>");
    }

    private static boolean admin(Sesion sesion) {
        return sesion.cuenta().rol() != Cuenta.Rol.USUARIO;
    }
}
