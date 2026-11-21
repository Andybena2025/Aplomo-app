package co.aplomo.web;

import co.aplomo.dao.PendienteDao.Cambio;
import co.aplomo.dao.PendienteDao.Ficha;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.modelo.Cuenta.Rol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/**
 * RF-022, RNF-002/007: tablas, tarjetas, etiquetas y formularios del mock adaptados a pendientes.
 */
public final class PaginasPendientes {
    private PaginasPendientes() {
    }

    /**
     * Lista ordenable y filtrable; el Usuario recibe solo filas autorizadas y no ve controles de cambio. RNF-011.
     */
    public static String lista(Sesion sesion, List<Ficha> lista, String estado, String orden, String aviso) {
        StringBuilder contenido2 = new StringBuilder(Html.aviso(aviso, true)).append(
                enlace("/sugerencias", "Sugerencias", false));
        contenido2.append("<p class=\"muted\">Los resultados rojos generan un pendiente por evaluación." +
                        " Los anteriores se conservan.</p><form class=\"row filtros-pendientes\" meth" +
                        "od=\"get\" action=\"/pendientes\">")
                .append(Formularios.selector(Map.of("estado", estado), "estado", "Mostrar",
                        List.of(new Formularios.Opcion("ABIERTO", "Abiertos"),
                                new Formularios.Opcion("ARREGLADO", "Arreglados"),
                                new Formularios.Opcion("TODOS", "Todos")), true))
                .append(Formularios.selector(Map.of("orden", orden), "orden", "Ordenar por",
                        List.of(new Formularios.Opcion("prioridad", "Prioridad"),
                                new Formularios.Opcion("fecha", "Fecha de registro"),
                                new Formularios.Opcion("limite", "Fecha límite")), true))
                .append("<button class=\"btn\">Aplicar</button></form><p class=\"muted\">")
                .append(Html.plural(lista.size(), "pendiente")).append("</p>");
        if (lista.isEmpty()) contenido2.append(Html.vacio("Aún no hay pendientes con este filtro.", ""));
        else {
            contenido2.append("<div class=\"tablewrap\"><table class=\"resp pendientes-table\"><thead><tr><" +
                    "th>Puesto y ubicación</th><th>Resultado</th><th>Seguimiento</th><th>Estado</" +
                    "th><th>Detalle</th></tr></thead><tbody>");
            for (var elemento : lista) {
                var elemento2 = elemento.pendiente();
                contenido2.append("<tr>");
                celda(contenido2, "Puesto",
                        "<b>Puesto " + elemento.numero() + "</b><br>" + escapar(elemento.empresaNombre()) +
                                "<div class=\"muted small\">" + escapar(elemento.sedeNombre()) + " · " +
                                escapar(elemento.salonNombre()) + "</div>");
                celda(contenido2, "Resultado", Html.riesgo(elemento2.puntaje(), elemento.color(), elemento.nivel()));
                celda(contenido2, "Seguimiento",
                        escapar(elemento2.prioridad() == null ? "Sin prioridad" : Formularios.etiqueta(
                                elemento2.prioridad())) + "<div class=\"small num\">Límite: " +
                                (elemento2.limite() == null ? "Sin definir" : Html.fecha(elemento2.limite())) +
                                "</div><div class=\"muted small num\">Registro: " + Html.fecha(elemento2.registro()) +
                                "</div>");
                celda(contenido2, "Estado", estado(elemento2.estado()));
                celda(contenido2, "Detalle", enlace("/pendientes/" + elemento2.id(), "Ver pendiente", false));
                contenido2.append("</tr>");
            }
            contenido2.append("</tbody></table></div>");
        }
        return pagina("Pendientes de arreglo", contenido2.toString(), sesion);
    }

    /**
     * Detalla el seguimiento y sus acciones históricas sin mostrar fotos. RNF-011.
     */
    public static String detalle(Sesion sesion, Ficha elemento, List<Cambio> cambios, String aviso, boolean exito) {
        var elemento2 = elemento.pendiente();
        StringBuilder contenido2 = new StringBuilder(enlace("/pendientes", "← Pendientes", false)).append(
                Html.aviso(aviso, exito));
        contenido2.append("<section class=\"card stack\"><div class=\"row between\"><h2>Puesto ")
                .append(elemento.numero()).append("</h2>").append(estado(elemento2.estado())).append("</div><p>")
                .append(escapar(elemento.empresaNombre())).append(" · ").append(escapar(elemento.sedeNombre()))
                .append(" · ").append(escapar(elemento.salonNombre())).append("</p>")
                .append(Html.riesgo(elemento2.puntaje(), elemento.color(), elemento.nivel()))
                .append("<dl class=\"datos-cuenta\">").append(dato("Qué hay que arreglar",
                        elemento2.descripcion() == null ? "Sin descripción" : elemento2.descripcion())).append(dato(
                                "Prioridad",
                        elemento2.prioridad() == null ? "Sin definir" : Formularios.etiqueta(elemento2.prioridad())))
                .append(dato("Fecha límite",
                        elemento2.limite() == null ? "Sin definir" : Html.fecha(elemento2.limite())))
                .append(dato("Fecha de registro", Html.fecha(elemento2.registro()))).append("</dl>")
                .append(enlace("/visitas/" + elemento.visita(), "Ver visita", false));
        if (admin(sesion)) {
            contenido2.append(enlace("/pendientes/" + elemento2.id() + "/editar", "Editar seguimiento", true));
            if (elemento2.estado().equals("ABIERTO"))
                contenido2.append("<form method=\"post\" action=\"/pendientes/").append(elemento2.id())
                        .append("/arreglado\">").append(Html.csrf(sesion))
                        .append("<p class=\"hint\">Márcalo solo cuando la empresa haya corregido el puesto. E" +
                                "sto no cambia el puntaje RULA guardado.</p><button class=\"btn\">Marcar como" +
                                " arreglado</button></form>");
        }
        contenido2.append("</section><section class=\"stack\"><h2>Historial</h2>");
        if (cambios.isEmpty()) contenido2.append("<p class=\"muted\">Sin acciones registradas.</p>");
        for (var elemento3 : cambios)
            contenido2.append("<div class=\"card stack\"><span class=\"eyebrow\">")
                    .append(Html.fechaHora(elemento3.fecha())).append("</span><b>")
                    .append(escapar(elemento3.responsable())).append("</b><span>")
                    .append(escapar(Formularios.etiqueta(elemento3.accion())))
                    .append(elemento3.nuevo() == null ? "" : " · " + escapar(Formularios.etiqueta(elemento3.nuevo())))
                    .append("</span><p class=\"small\">").append(escapar(elemento3.detalle())).append("</p></div>");
        return pagina("Pendiente de arreglo", contenido2.append("</section>").toString(), sesion);
    }

    /**
     * Conserva todos los campos cuando falla validación o escritura. RNF-011.
     */
    public static String editar(Sesion sesion, Ficha elemento, Map<String, String> enviados, String error) {
        var elemento2 = elemento.pendiente();
        Map<String, String> parametros = new HashMap<>();
        parametros.put("descripcion", elemento2.descripcion() == null ? "" : elemento2.descripcion());
        parametros.put("prioridad", elemento2.prioridad() == null ? "" : elemento2.prioridad());
        parametros.put("fecha", elemento2.limite() == null ? "" : elemento2.limite().toString());
        parametros.putAll(enviados);
        String texto2 =
                enlace("/pendientes/" + elemento2.id(), "← Volver al pendiente", false) + Html.aviso(error, false) +
                        "<form data-pendiente-form class=\"card stack cuenta-form\" method=\"post\" a" +
                        "ction=\"/pendientes/" + elemento2.id() + "\"><h2>Seguimiento · Puesto " + elemento.numero() +
                        "</h2><p class=\"muted small\">Completa los datos de seguimiento. El resultad" +
                        "o y el estado se conservan.</p>" + Html.csrf(sesion) +
                        Formularios.texto(parametros, "descripcion", "Qué hay que arreglar", 255, false) +
                        "<span class=\"hint\">Máximo 255 caracteres.</span><div class=\"grid2\">" +
                        Formularios.selector(parametros, "prioridad", "Prioridad",
                                List.of(new Formularios.Opcion("", "Sin definir"),
                                        new Formularios.Opcion("ALTA", "Alta"),
                                        new Formularios.Opcion("MEDIA", "Media"),
                                        new Formularios.Opcion("BAJA", "Baja")), false) +
                        Formularios.campo(parametros, "fecha", "Fecha límite (opcional)", "date", 10, false) +
                        "</div><div class=\"aviso\" role=\"alert\" data-error-pendiente hidden><span>" +
                        "</span><button type=\"button\" class=\"link\" data-cerrar-error aria-label=" +
                        "\"Cerrar aviso\">Cerrar</button></div><button class=\"btn p\" type=\"submit" +
                        "\">Guardar seguimiento</button></form><script src=\"/static/js/pendientes.js" +
                        "\" defer></script>";
        return pagina("Editar seguimiento", texto2, sesion);
    }

    /**
     * Nombre de seguimiento derivado del color ya calculado; no altera ni calcula RULA. RNF-011.
     */
    public static String clasificacion(String color) {
        return switch (color) {
            case "VERDE" -> "Aceptable";
            case "AMARILLO" -> "En observación";
            default -> "Requiere arreglo";
        };
    }

    private static String pagina(String titulo, String contenido, Sesion sesion) {
        return Html.pagina(titulo,
                "<div class=\"pagehead\"><div><span class=\"eyebrow\">Seguimiento · Pendiente" + "s</span><h1>" +
                        escapar(titulo) + "</h1></div></div>" + contenido, sesion, false);
    }

    private static boolean admin(Sesion sesion) {
        return sesion.cuenta().rol() != Rol.USUARIO;
    }

    private static String estado(String texto) {
        return "<span class=\"tag\">" + (texto.equals("ARREGLADO") ? "Arreglado" : "Abierto") + "</span>";
    }

    private static String dato(String nombre, String valor) {
        return "<div><dt>" + escapar(nombre) + "</dt><dd class=\"texto-notas\">" + escapar(valor) + "</dd></div>";
    }

    private static void celda(StringBuilder contenido2, String nombre, String html) {
        contenido2.append("<td data-label=\"").append(nombre).append("\"><div class=\"cell-value\">").append(html)
                .append("</div></td>");
    }
}
