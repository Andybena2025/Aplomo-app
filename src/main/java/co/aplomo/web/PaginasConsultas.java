package co.aplomo.web;

import co.aplomo.servicio.Consultas.Detalle;
import co.aplomo.servicio.Consultas.Pagina;
import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.dao.SugerenciaDao.Contexto;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/**
 * RF-024, RNF-002/007: tablas y filtros del mock, conservando los parámetros al ordenar.
 */
public final class PaginasConsultas {
    private PaginasConsultas() {
    }

    /**
     * Genera la sección con su contador, filtros y enlaces de ordenación accesibles. RNF-011.
     */
    public static String lista(Sesion sesion, Pagina pagina, Contexto contexto) {
        return ListasConsulta.pagina(sesion, pagina, contexto);
    }

    private static String listaAnterior(Sesion sesion, Pagina pagina, Contexto contexto) {
        var elemento = pagina.seccion();
        var parametros2 = pagina.filtros();
        boolean admin = sesion.cuenta().rol() != Cuenta.Rol.USUARIO;
        String etiqueta = elemento == SeccionConsulta.EMPRESAS ? "Gestión · Empresas" :
                elemento == SeccionConsulta.RESERVAS ? "Reservas · Consulta" :
                elemento == SeccionConsulta.PENDIENTES ? "Seguimiento · Pendientes" :
                "Consultas · " + elemento.titulo();
        StringBuilder contenido = new StringBuilder(Html.encabezado(etiqueta, elemento.titulo(), admin &&
                !(sesion.cuenta().rol() == Cuenta.Rol.EVALUADOR && elemento == SeccionConsulta.EMPRESAS) ? accion(
                elemento, parametros2, contexto) : ""));
        secciones(elemento, contenido);
        if (elemento == SeccionConsulta.PENDIENTES)
            contenido.append("<div class=\"row\">").append(enlace("/sugerencias", "Ver sugerencias", false))
                    .append("</div>");
        if (contexto != null) contenido.append(contexto(contexto, pagina.contextoEvaluacion(), admin));
        contenido.append(filtros(pagina, contexto != null, admin));
        String contador = contador(pagina);
        contenido.append("<p class=\"muted consultas-contador\" role=\"status\">").append(contador).append("</p>");
        if (pagina.filas().isEmpty()) contenido.append(Html.vacio(
                "Aún no hay " + elemento.titulo().toLowerCase(Locale.forLanguageTag("es")) + " con estos filtros.",
                admin ? accionVacio(elemento, parametros2, contexto) : ""));
        else tabla(pagina, sesion, contexto, admin, contenido);
        return Html.pagina(elemento.titulo(), contenido.toString(), sesion, false, false);
    }

    /**
     * Consulta finalizada del Usuario: postura y resultados propios, sin fotos ni acciones de gestión. RNF-011.
     */
    public static String evaluacion(Sesion sesion2, Detalle elemento) {
        var elemento2 = elemento.evaluacion();
        StringBuilder contenido = new StringBuilder(enlace("/evaluaciones", "← Evaluaciones", false)).append(
                        "<section class=\"card stack\"><div class=\"row between\"><h2>Puesto ")
                .append(escapar(elemento2.valor("numero"))).append("</h2>").append(riesgo(elemento2))
                .append("</div><p>").append(ubicacion(elemento2)).append("</p><p>Evaluada por: ")
                .append(escapar(elemento2.valor("evaluador_nombre"))).append("</p><h3>")
                .append(escapar(elemento2.valor("postura"))).append("</h3><p>").append(escapar(
                        elemento2.valor("criterio")
                                .equals("MAS_INADECUADA") ? "Postura más inadecuada" : "Mayor tiempo de exposición"))
                .append(" · ").append(escapar(elemento2.valor("minutos")))
                .append(" minutos por jornada</p><p class=\"texto-notas\">").append(escapar(elemento2.valor("motivo")))
                .append("</p><p>").append(fecha(elemento2.valor("fecha"))).append("</p>")
                .append(enlace("/visitas/" + elemento2.valor("visita"), "Ver visita", false))
                .append("</section><section class=\"stack\"><h2>Análisis guardados</h2>");
        int numero = 0;
        for (var elemento3 : elemento.analisis())
            contenido.append("<div class=\"card row between\"><div>Análisis ").append(++numero).append(" · ")
                    .append(escapar(Formularios.etiqueta(elemento3.lado()))).append("<div class=\"hint\">")
                    .append(Html.fechaHora(elemento3.fecha())).append(" · ").append(escapar(elemento3.responsable()))
                    .append("</div></div>")
                    .append(Html.riesgo(elemento3.puntaje(), elemento3.color(), elemento3.nivel())).append("</div>");
        if (elemento.analisis().isEmpty())
            contenido.append("<p class=\"muted\">Esta evaluación no tiene análisis guardados.</p>");
        else contenido.append(
                enlace("/sugerencias?evaluacion=" + elemento2.id(), "Sugerencias de la evaluación", false));
        contenido.append(
                enlace("/pendientes?estado=TODOS&evaluacion=" + elemento2.id(), "Pendientes de la evaluación", false));
        return Html.pagina("Evaluación · Puesto " + elemento2.valor("numero"),
                contenido.append("</section>").toString(), sesion2);
    }

    private static void secciones(SeccionConsulta elemento, StringBuilder contenido) {
        contenido.append("<nav class=\"secciones-tabs\" aria-label=\"Secciones de gestión\">");
        for (var otra : List.of(SeccionConsulta.EMPRESAS, SeccionConsulta.SEDES, SeccionConsulta.AREAS,
                SeccionConsulta.CARGOS, SeccionConsulta.SALONES, SeccionConsulta.PUESTOS))
            contenido.append("<a href=\"").append(otra.ruta()).append("\"")
                    .append(elemento == otra ? " class=\"on\" aria-current=\"page\"" : "").append(">")
                    .append(otra.titulo()).append("</a>");
        contenido.append("</nav>");
    }

    private static void tabla(Pagina pagina, Sesion sesion, Contexto contexto, boolean admin, StringBuilder contenido) {
        var elemento = pagina.seccion();
        var parametros2 = pagina.filtros();

        var columnas = new ArrayList<>(elemento.columnas().stream()
                .filter(elemento2 -> !(contexto != null && elemento2.clave().equals("numero"))).toList());
        if (elemento == SeccionConsulta.EVALUACIONES && sesion.cuenta().rol() == Cuenta.Rol.ADMINISTRADOR)
            columnas.add(new SeccionConsulta.Columna("evaluador_nombre", "Evaluador"));
        contenido.append("<div class=\"tablewrap\"><table class=\"resp consultas-table ")
                .append(elemento == SeccionConsulta.SUGERENCIAS ? "sugerencias-table" : "").append("\"><thead><tr>");
        for (var col : columnas) {
            boolean actual = parametros2.get("orden").equals(col.clave());
            Map<String, String> parametros = new LinkedHashMap<>(parametros2);
            parametros.put("orden", col.clave());
            parametros.put("direccion", actual && parametros2.get("direccion").equals("asc") ? "desc" : "asc");
            contenido.append("<th scope=\"col\" aria-sort=\"")
                    .append(actual ? (parametros2.get("direccion").equals("asc") ? "ascending" : "descending") : "none")
                    .append("\"><a class=\"orden-columna\" href=\"").append(escapar(url(elemento.ruta(), parametros)))
                    .append("\" aria-label=\"Ordenar por ").append(escapar(col.titulo())).append("\">")
                    .append(escapar(col.titulo()))
                    .append(actual ? (parametros2.get("direccion").equals("asc") ? " ↑" : " ↓") : "")
                    .append("</a></th>");
        }
        contenido.append("<th scope=\"col\">Detalle</th></tr></thead><tbody>");
        for (var fila : pagina.filas()) {
            contenido.append("<tr data-registro=\"").append(fila.id()).append("\">");
            for (var col : columnas)
                celda(contenido, col.titulo(), celda(fila, col.clave(), elemento, admin) +
                        (col.clave().equals("numero") && !fila.valor("acceso_historico")
                                .isEmpty() ? " <span class=\"tag\">Ya no asignada</span>" : ""));
            celda(contenido, "Detalle", enlace(elemento.ruta() + "/" + fila.id(), detalle(elemento), false));
            contenido.append("</tr>");
        }
        contenido.append("</tbody></table></div>");

    }

    private static String contador(Pagina pagina) {
        var seccion = pagina.seccion();
        if (pagina.filas().isEmpty()) return "0 resultados";
        if (pagina.filas().size() == 1) return "1 " + seccion.singular();
        String plural = seccion == SeccionConsulta.PENDIENTES ? "pendientes" : seccion.titulo().toLowerCase(
                Locale.forLanguageTag("es"));
        return pagina.filas().size() + " " + plural;
    }

    static String contexto(Contexto sugerencia, Fila elemento, boolean admin) {
        if (!admin || elemento == null) return PaginasSugerencias.contexto(sugerencia);
        return "<section class=\"card stack\"><div class=\"row between\"><h2>Puesto " + sugerencia.numero() +
                inactivo(elemento, "puesto", false, true) + "</h2>" +
                Html.riesgo(sugerencia.puntaje(), sugerencia.color(), sugerencia.nivel()) + "</div><div>" +
                ubicacion(elemento, true) + "</div><p class=\"muted\">" + escapar(sugerencia.postura()) + "</p>" +
                enlace("/visitas/" + sugerencia.visita(), "Ver visita", false) + "</section>";
    }

    private static String filtros(Pagina elemento, boolean contexto, boolean admin) {
        var elemento2 = elemento.seccion();
        var parametros2 = elemento.filtros();
        StringBuilder contenido = new StringBuilder(
                "<form class=\"card stack consultas-filtros\" method=\"get\" action=\"").append(elemento2.ruta())
                .append("\"><div class=\"grid3\">");
        contenido.append(Formularios.campo(parametros2, "q", "Buscar", "search", 120, false));
        contenido.append(filtro(elemento, "empresa"));
        if (elemento2 == SeccionConsulta.EVALUACIONES && elemento.catalogos().containsKey("evaluador"))
            contenido.append(filtro(elemento, "evaluador"));
        contenido.append(filtro(elemento, "estado")).append("</div>");
        var extras = elemento2.filtros().stream().filter(k -> !Set.of("empresa", "estado", "evaluador").contains(k))
                .filter(k -> admin || !Set.of("registros", "puesto").contains(k))
                .filter(k -> !contexto || Set.of("desde", "hasta", "registros").contains(k)).toList();
        boolean abiertos = extras.stream().anyMatch(
                k -> !parametros2.getOrDefault(k, "").isEmpty() && !parametros2.get(k).equals("TODOS"));
        contenido.append("<details class=\"guia filtros-extra\"").append(abiertos ? " open" : "")
                .append("><summary>Más filtros</summary><div class=\"grid3\">");
        for (String key : extras) contenido.append(filtro(elemento, key));
        if (parametros2.containsKey("evaluacion"))
            contenido.append("<input type=\"hidden\" name=\"evaluacion\" value=\"")
                    .append(escapar(parametros2.get("evaluacion"))).append("\">");
        if (parametros2.containsKey("visita")) contenido.append("<input type=\"hidden\" name=\"visita\" value=\"")
                .append(escapar(parametros2.get("visita"))).append("\">");
        List<Formularios.Opcion> ordenes = new ArrayList<>(
                elemento2.columnas().stream().filter(elemento3 -> !contexto || !elemento3.clave().equals("numero"))
                        .map(elemento4 -> new Formularios.Opcion(elemento4.clave(), elemento4.titulo())).toList());
        if (elemento2 == SeccionConsulta.PENDIENTES) ordenes.addAll(
                List.of(new Formularios.Opcion("prioridad", "Prioridad"),
                        new Formularios.Opcion("fecha", "Fecha de registro"),
                        new Formularios.Opcion("fecha_limite", "Fecha límite")));
        contenido.append(Formularios.selector(parametros2, "orden", "Ordenar por", ordenes, true))
                .append(Formularios.selector(parametros2, "direccion", "Dirección",
                        List.of(new Formularios.Opcion("asc", "Ascendente"),
                                new Formularios.Opcion("desc", "Descendente")), true))
                .append("</div></details><div class=\"row consultas-orden\"><button class=\"btn p\">A" +
                        "plicar filtros</button>");
        String limpiar = parametros2.containsKey("evaluacion") ?
                elemento2.ruta() + "?evaluacion=" + parametros2.get("evaluacion") : elemento2.ruta();
        return contenido.append(enlace(limpiar, "Limpiar filtros", false)).append("</div></form>").toString();
    }

    static String filtro(Pagina elemento, String key) {
        var parametros2 = elemento.filtros();
        String titulo = switch (key) {
            case "evaluador" -> "Evaluador";
            case "empresa" -> "Empresa";
            case "sede" -> "Sede";
            case "salon" -> "Salón";
            case "area" -> "Área";
            case "cargo" -> "Cargo";
            case "numero" -> "Número de puesto";
            case "puesto" -> "Puesto";
            case "registros" -> "Registros";
            case "puntaje" -> "Puntaje";
            case "color" -> "Semáforo";
            case "nivel" -> "Nivel oficial RULA";
            case "desde" -> elemento.seccion() == SeccionConsulta.SUGERENCIAS ? "Fecha límite desde" : "Fecha desde";
            case "hasta" -> elemento.seccion() == SeccionConsulta.SUGERENCIAS ? "Fecha límite hasta" : "Fecha hasta";
            default -> "Estado";
        };
        if (key.equals("numero")) return Formularios.campo(parametros2, key, titulo, "number", 5, false)
                .replace("type=\"number\"", "type=\"number\" min=\"1\" max=\"32767\"");
        if (key.equals("desde") || key.equals("hasta"))
            return Formularios.campo(parametros2, key, titulo, "date", 10, false);
        List<Formularios.Opcion> opciones = new ArrayList<>();
        opciones.add(new Formularios.Opcion(Set.of("estado", "registros").contains(key) ? "TODOS" : "", "Todos"));
        if (elemento.catalogos().containsKey(key)) for (var elemento2 : elemento.catalogos().get(key)) {
            if (!key.equals("empresa") && !key.equals("evaluador") &&
                    !parametros2.getOrDefault("empresa", "").isEmpty() &&
                    elemento2.empresa() != Integer.parseInt(parametros2.get("empresa"))) continue;
            if (key.equals("salon") && !parametros2.getOrDefault("sede", "").isEmpty() &&
                    elemento2.sede() != Integer.parseInt(parametros2.get("sede"))) continue;
            opciones.add(new Formularios.Opcion("" + elemento2.id(), elemento2.nombre()));
        }
        else if (key.equals("registros")) opciones.addAll(List.of(new Formularios.Opcion("ACTIVOS", "Solo activos"),
                new Formularios.Opcion("INACTIVOS", "Solo inactivos")));
        else if (key.equals("estado")) for (var texto2 : elemento.seccion().estados())
            opciones.add(new Formularios.Opcion(texto2, Formularios.etiqueta(texto2)));
        else if (key.equals("puntaje")) for (int n = 1; n <= 7; n++)
            opciones.add(new Formularios.Opcion("" + n, "" + n));
        else for (String texto3 : elemento.riesgos().getOrDefault(key, List.of()))
                opciones.add(new Formularios.Opcion(texto3,
                        key.equals("nivel") ? "Nivel " + texto3 : Formularios.etiqueta(texto3)));
        return Formularios.selector(parametros2, key, titulo, opciones, false);
    }

    private static String accion(SeccionConsulta elemento, Map<String, String> parametros2, Contexto sugerencia) {
        return switch (elemento) {
            case EMPRESAS -> enlace("/empresas/nueva", "Nueva empresa", true);
            case RESERVAS -> enlace("/reservas/nueva", "Nueva reserva", true);
            case SUGERENCIAS ->
                    sugerencia == null ? "" : enlace("/sugerencias/nueva?evaluacion=" + sugerencia.evaluacion(),
                            "Escribir sugerencia", true);
            default -> "";
        };
    }

    private static String celda(Fila elemento, String key, SeccionConsulta elemento2, boolean admin) {
        String valor = elemento.valor(key);
        return switch (key) {
            case "puntaje" -> riesgo(elemento);
            case "ubicacion" -> ubicacion(elemento, admin);
            case "asignacion" ->
                    escapar(elemento.valor("area_nombre").isEmpty() ? "Sin área" : elemento.valor("area_nombre")) +
                            "<div class=\"muted small\">" +
                            escapar(elemento.valor("cargo_nombre").isEmpty() ? "Sin cargo" : elemento.valor(
                                    "cargo_nombre")) + "</div>";
            case "numero" -> "<b class=\"no-wrap\">Puesto " + escapar(valor) + "</b>" +
                    inactivo(elemento, "puesto", false, admin) +
                    ((elemento2 == SeccionConsulta.PENDIENTES || elemento2 == SeccionConsulta.SUGERENCIAS) ?
                            "<div class=\"ubicacion-celda\">" + ubicacion(elemento, admin) + "</div>" : "");
            case "fecha" -> fecha(valor) + (elemento2 == SeccionConsulta.RESERVAS ?
                    "<div class=\"small num\">" + escapar(elemento.valor("inicio").substring(0, 5)) + "–" +
                            escapar(elemento.valor("fin").substring(0, 5)) + "</div>" : "");
            case "estado" -> "<span class=\"tag\">" + escapar(Formularios.etiqueta(valor)) + "</span>";
            case "origen" -> valor.equals("SISTEMA") ? "Sugerencia del sistema" : "Escrita por el Administrador";
            case "prioridad" -> escapar(Formularios.etiqueta(valor));
            case "tipo" -> escapar(elemento2 == SeccionConsulta.RESERVAS ? PaginasReservas.categoria(
                    valor) : Formularios.etiqueta(valor));
            case "seguimiento" ->
                    escapar(elemento.valor("prioridad").isEmpty() ? "Sin prioridad" : Formularios.etiqueta(
                            elemento.valor("prioridad"))) + "<div class=\"small\">Límite: " +
                            fecha(elemento.valor("fecha_limite")) + "</div><div class=\"muted small\">Registro: " +
                            fecha(elemento.valor("fecha")) + "</div>";
            case "nombre" -> escapar(valor) + (elemento2 == SeccionConsulta.SUGERENCIAS ?
                    "<div class=\"muted small\">" + escapar(Formularios.etiqueta(elemento.valor("tipo"))) +
                    "</div>" : "");
            default -> escapar(valor.isEmpty() ? "Sin definir" : valor);
        };
    }

    private static String ubicacion(Fila elemento) {
        return ubicacion(elemento, false);
    }

    private static String ubicacion(Fila elemento, boolean admin) {
        return "<strong>" + escapar(elemento.valor("empresa_nombre")) + "</strong>" +
                inactivo(elemento, "empresa", true, admin) + "<div class=\"muted small\">" +
                escapar(elemento.valor("sede_nombre")) + inactivo(elemento, "sede", true, admin) +
                (elemento.valor("salon_nombre").isEmpty() ? "" :
                        " · " + escapar(elemento.valor("salon_nombre")) + inactivo(elemento, "salon", false, admin)) +
                "</div>";
    }

    private static String inactivo(Fila elemento, String nivel, boolean femenino, boolean admin) {
        String clave = nivel.equals("empresa") || nivel.equals("sede") ? nivel + "_activa" : nivel + "_activo";
        return admin && elemento.valor(clave).equals("0") ? SistemaVisual.estado("INACTIVA") : "";
    }

    private static String accionVacio(SeccionConsulta elemento, Map<String, String> parametros2, Contexto sugerencia) {
        String accion = accion(elemento, parametros2, sugerencia);
        if (!accion.isEmpty()) return accion;
        return switch (elemento) {
            case VISITAS -> enlace("/reservas", "Ir a Reservas", true);
            case EVALUACIONES -> enlace("/visitas", "Ir a Visitas", true);
            case SEDES, AREAS, CARGOS, SALONES, PUESTOS -> enlace("/empresas", "Ir a Empresas", true);
            default -> enlace(elemento.ruta(), "Limpiar filtros", false);
        };
    }

    private static String riesgo(Fila elemento) {
        return Html.riesgo(elemento.valor("puntaje").isEmpty() ? null : Integer.valueOf(elemento.valor("puntaje")),
                elemento.valor("color"),
                elemento.valor("nivel").isEmpty() ? 0 : Integer.parseInt(elemento.valor("nivel")));
    }

    private static String fecha(String texto) {
        if (texto.isEmpty()) return "Sin definir";
        return texto.length() > 10 ? Html.fechaHora(LocalDateTime.parse(texto.replace(' ', 'T'))) : Html.fecha(
                LocalDate.parse(texto));
    }

    private static String detalle(SeccionConsulta elemento) {
        return switch (elemento) {
            case EMPRESAS -> "Ver empresa";
            case SEDES -> "Ver sede";
            case SALONES -> "Ver salón";
            case PUESTOS -> "Ver puesto";
            case RESERVAS -> "Ver reserva";
            case VISITAS -> "Ver visita";
            case EVALUACIONES -> "Ver evaluación";
            case PENDIENTES -> "Ver pendiente";
            case SUGERENCIAS -> "Ver sugerencia";
            default -> "Ver registro";
        };
    }

    private static void celda(StringBuilder contenido, String label, String html) {
        contenido.append("<td data-label=\"").append(escapar(label)).append("\"><div class=\"cell-value\">")
                .append(html).append("</div></td>");
    }

    private static String url(String ruta, Map<String, String> parametros2) {
        String texto2 = parametros2.entrySet().stream().filter(parametros3 -> !parametros3.getValue().isEmpty())
                .map(parametros4 -> URLEncoder.encode(parametros4.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(parametros4.getValue(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
        return ruta + (texto2.isEmpty() ? "" : "?" + texto2);
    }
}
