package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.InformeEmpresa;
import co.aplomo.servicio.Calificaciones;
import co.aplomo.modelo.InformeEmpresa.Calificacion;
import co.aplomo.modelo.InformeEmpresa.Riesgo;
import co.aplomo.modelo.InformeEmpresa.SedeResumen;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/**
 * RF-025/026: indicadores y hoja del mock, con nivel escrito y sin depender del color al imprimir.
 */
public final class PaginasCalificaciones {
    private PaginasCalificaciones() {
    }

    /**
     * Muestra una selección explícita de empresa al Administrador. RNF-011.
     */
    public static String elegir(Sesion sesion, List<Empresa> empresas) {
        var opciones = empresas.stream().map(empresa -> new Formularios.Opcion("" + empresa.id(), empresa.nombre()))
                .toList();
        String contenido = "<form class=\"card stack cuenta-form\" method=\"get\" action=\"/calificaciones\">" +
                Formularios.selector(Map.of(), "empresa", "Empresa", opciones, true) +
                "<button class=\"btn p\">Ver calificaciones</button></form>";
        return Html.pagina("Calificaciones", contenido, sesion);
    }

    /**
     * Consulta de calificaciones de sede y empresa para ambos roles autorizados. RNF-011.
     */
    public static String calificaciones(Sesion sesion, InformeEmpresa i) {
        StringBuilder contenido2 = new StringBuilder(encabezado(sesion, i, false));
        contenido2.append("<section class=\"card stack\"><span class=\"eyebrow\">Calificación general</span>")
                .append(general(i)).append(cobertura(i)).append(ayudaCalculo())
                .append("</section><section class=\"stack calificaciones-sedes\"><h2>Calificacion" +
                        "es por sede</h2><div class=\"grid2\">");
        for (var sede : i.sedes()) {
            contenido2.append("<article class=\"card stack\"><div class=\"row between\"><h3>")
                    .append(escapar(sede.sede().nombre())).append("</h3><span class=\"tag\">")
                    .append(sede.sede().activo() ? "Activa" : "Inactiva")
                    .append("</span></div><p class=\"muted small\">").append(escapar(sede.sede().ciudad()))
                    .append(" · ").append(escapar(sede.sede().direccion())).append("</p>")
                    .append("<p class=\"etiqueta-promedio-sede\">Promedio histórico de sus visitas Finali" +
                            "zadas</p>").append(nota(sede.calificacion())).append("<p>")
                    .append(cantidad(sede.visitas().size(), "visita Finalizada", "visitas Finalizadas")).append(" · ")
                    .append(sede.visitas().stream().filter(visita -> visita.calificacion() != null).count())
                    .append(" con calificación</p>").append(enlace("/sedes/" + sede.sede().id(), "Ver sede", false))
                    .append("</article>");
        }
        if (i.sedes().isEmpty()) contenido2.append(
                Html.vacio("Esta empresa aún no tiene sedes. Registra su estructura antes de evaluar.", ""));
        return Html.pagina("Calificaciones", contenido2.append("</div></section>").toString(), sesion, false);
    }

    /**
     * Informe general: hoja clara del mock, sin bibliotecas PDF ni fotografías privadas. RNF-011.
     */
    public static String informe(Sesion sesion, InformeEmpresa i) {
        return informe(sesion, i, null);
    }

    /**
     * RF-026: resalta una visita autorizada sin cambiar promedios ni incluir resultados sin finalizar.
     */
    public static String informe(Sesion sesion, InformeEmpresa i, co.aplomo.dao.VisitaDao.Ficha visitaActual) {
        StringBuilder contenido2 = new StringBuilder(encabezado(sesion, i, true));
        contenido2.append("<p class=\"hint informe-ayuda\">Pulsa Imprimir o guardar PDF. En el diálogo " +
                        "del navegador elige tu impresora o Guardar como PDF.</p>")
                .append("<div class=\"sheetwrap informe-wrap\"><article class=\"sheet informe-empresa" +
                        "\"><section class=\"informe-portada\"><header class=\"informe-marca\"><b>ALI" +
                        "NEO.</b><div>Informe general de la empresa<br>").append(generado(i))
                .append("</div></header><h1>").append(escapar(i.empresa().nombre())).append("</h1><p>NIT ")
                .append(escapar(i.empresa().nit())).append("</p><p>").append(escapar(i.empresa().descripcion()))
                .append("</p>").append("<p>Contacto: ").append(texto(i.empresa().contacto(), "Sin registrar"))
                .append(" · ").append(texto(i.empresa().telefono(), "Sin teléfono")).append(" · ")
                .append(texto(i.empresa().correo(), "Sin correo")).append("</p>");
        if (visitaActual != null) {
            contenido2.append("<p class=\"visita-destacada\">Visita actual: ")
                    .append(escapar(visitaActual.sedeNombre())).append(" · ")
                    .append(escapar(visitaActual.salonNombre())).append(" · ")
                    .append(visitaActual.visita().estado().equals("EN_CURSO") ?
                            "En curso. El informe incluye solo resultados de visitas " +
                            "Finalizadas." : "Finalizada. Resaltada en el detalle de su sede.").append("</p>");
        }
        resumen(contenido2, i);
        contenido2.append("<p class=\"informe-metodo\">").append(metodo()).append("</p></section>");
        for (var sede : i.sedes()) sede(contenido2, sede);
        contenido2.append("<section class=\"box informe-general\"><h2>Calificación general entre sedes</h2>")
                .append(general(i)).append(cobertura(i)).append("</section>");
        contenido2.append("<section class=\"informe-leyenda\"><h2>Cómo leer el riesgo</h2><p>El nivel y" +
                " la acción están escritos para que el informe se entienda en blanco y negro." + "</p><ul>");
        for (var empresa : i.leyenda())
            contenido2.append("<li>").append(empresa.minimo())
                    .append(empresa.minimo() == empresa.maximo() ? "" : "–" + empresa.maximo()).append(" · ")
                    .append(escapar(Formularios.etiqueta(empresa.color()))).append(" · Nivel ").append(empresa.nivel())
                    .append(" · ").append(escapar(empresa.nombre())).append(": ").append(escapar(empresa.accion()))
                    .append("</li>");
        contenido2.append("</ul></section><footer class=\"foot\">ALINEO. · Ergonomía con precisión · Mé" +
                        "todo RULA (McAtamney y Corlett, 1993)<br>Generado el ").append(Html.fechaHora(i.generado()))
                .append(" · Resultados de visitas Finalizadas.</footer></article></div><script src=\"" +
                        "/static/js/informe.js\" defer></script>");
        return Html.pagina("Informe general", resaltar(contenido2.toString(), visitaActual), sesion, false);
    }

    private static String resaltar(String html, co.aplomo.dao.VisitaDao.Ficha visita) {
        if (visita == null) return html;
        return html.replace("data-visita=\"" + visita.visita().id() + "\"",
                "data-visita=\"" + visita.visita().id() + "\" data-visita-actual");
    }

    private static String encabezado(Sesion sesion, InformeEmpresa i, boolean informe) {
        return "<div class=\"informe-controles\">" + enlace("/empresas/" + i.empresa().id(), "← Empresa", false) +
                "<div class=\"pagehead\"><div><span class=\"eyebrow\">" +
                (informe ? "Informes · Empresa" : "Consultas · Calificaciones") + "</span><h1>" +
                (informe ? "Informe general" : "Calificaciones") + "</h1><p class=\"muted\">" +
                escapar(i.empresa().nombre()) + " · Solo visitas Finalizadas</p></div>" + (informe ?
                "<div class=\"stack informe-imprimir\"><button class=\"btn p\" type=\"button" +
                "\" data-imprimir>Imprimir o guardar PDF</button><p class=\"hint\">En el cuad" +
                "ro de imprimir, desmarca Encabezados y pies de página</p></div>" :
                sesion.cuenta().rol() != Cuenta.Rol.USUARIO ? enlace("/informes/empresa/" + i.empresa().id(),
                        "Generar informe", true) : "") + "</div></div>";
    }

    private static String metodo() {
        return "Puesto: su peor resultado. Visita: promedio de sus puestos. " +
                "Sede: promedio de sus visitas Finalizadas. General: promedio de las sedes, necesita dos o más.";
    }

    private static String ayudaCalculo() {
        return "<details class=\"calculo-ayuda\"><summary>¿Cómo se calcula?</summary><dl>" +
                "<dt>Puesto</dt><dd>Su peor resultado</dd><dt>Visita</dt><dd>Promedio de sus puestos</dd>" +
                "<dt>Sede</dt><dd>Promedio de sus visitas</dd><dt>General</dt>" +
                "<dd>Promedio de las sedes (necesita 2 o más)</dd></dl></details>";
    }

    private static String general(InformeEmpresa i) {
        if (i.sedes().size() == 1) return "<p><b>Sin calificación general.</b> La empresa tiene una sola sede.</p>";
        return i.general() == null ?
                "<p><b>Sin calificación general.</b> Se necesitan al menos " + Calificaciones.MINIMO_SEDES_GENERAL +
                " sedes calificadas.</p>" : nota(i.general());
    }

    private static String cobertura(InformeEmpresa i) {
        int cantidad2 = i.sedes().size() - i.sedesSinCalificacion().size();
        return "<p class=\"cobertura-sedes\">" + cantidad2 + " de " + i.sedes().size() +
                (i.sedes().size() == 1 ? " sede calificada · " : " sedes calificadas · ") +
                (i.sedesSinCalificacion().isEmpty() ? "no falta ninguna sede por calificar" :
                        "" + (i.sedesSinCalificacion().size() == 1 ? "falta " : "faltan ") +
                                String.join(", ", i.sedesSinCalificacion().stream().map(Html::escapar).toList())) +
                ".</p>";
    }

    private static String nota(Calificacion empresa) {
        if (empresa == null)
            return "<p class=\"sin-calificacion\">Sin calificación: no hay visitas Finalizadas c" + "on puntaje.</p>";
        return "<div class=\"nota-promedio\"><b class=\"promedio\">" + decimal(empresa.promedio()) +
                "</b><span>Promedio</span>" +
                Html.riesgo(empresa.redondeado(), empresa.riesgo().color(), empresa.riesgo().nivel()) +
                "<span class=\"small\">Clasificación con promedio redondeado · " + escapar(empresa.riesgo().accion()) +
                "</span></div>";
    }

    private static void sede(StringBuilder contenido2, SedeResumen empresa) {
        contenido2.append("<section class=\"informe-sede\"><h2>").append(escapar(empresa.sede().nombre()))
                .append(empresa.sede().activo() ? "" : " · Inactiva").append("</h2><p>")
                .append(escapar(empresa.sede().ciudad())).append(" · ").append(escapar(empresa.sede().direccion()))
                .append("</p><p class=\"etiqueta-promedio-sede\">Promedio histórico de sus visitas Fi" +
                        "nalizadas</p>").append(nota(empresa.calificacion()));
        var empresa2 = empresa.estadisticas();
        contenido2.append("<div class=\"informe-cifras\">");
        cifra(contenido2, "Visita Finalizada", "Visitas Finalizadas", empresa2.visitas());
        cifra(contenido2, "Puesto evaluado único", "Puestos evaluados únicos", empresa2.puestos());
        cifra(contenido2, "Puesto con puntaje", "Puestos con puntaje", empresa2.conPuntaje());
        cifra(contenido2, "Foto analizada", "Fotos analizadas", empresa2.fotosAnalizadas());
        for (String color : List.of("VERDE", "AMARILLO", "ROJO"))
            cifra(contenido2, "Puesto en " + Formularios.etiqueta(color), "Puestos en " + Formularios.etiqueta(color),
                    empresa2.colores().getOrDefault(color, 0));
        cifra(contenido2, "Puesto sin puntaje", "Puestos sin puntaje", empresa2.puestos() - empresa2.conPuntaje());
        cifra(contenido2, "Pendiente abierto", "Pendientes abiertos", empresa2.pendientes().getOrDefault("ABIERTO", 0));
        cifra(contenido2, "Pendiente arreglado", "Pendientes arreglados",
                empresa2.pendientes().getOrDefault("ARREGLADO", 0));
        contenido2.append("</div>");
        contenido2.append("<div class=\"informe-bloque\"><h3>Visitas y calificaciones históricas</h3>");
        tabla(contenido2, List.of("Reserva", "Inicio y fin reales", "Calificación"), empresa.visitas().stream()
                .map(visita -> List.of(
                        "<span class=\"hint\" data-visita=\"" + visita.id() + "\">" + escapar(visita.codigo()) +
                                "</span>", Html.fechaHora(visita.inicio()) + "<br>" + Html.fechaHora(visita.fin()),
                        nota(visita.calificacion()))).toList());
        contenido2.append("</div><div class=\"informe-bloque\"><h3>Puestos y resultados actuales</h3><p" +
                ">Una fila por puesto: última evaluación Finalizada; máximo de sus análisis. " +
                "Una reevaluación Verde sustituye al Rojo anterior en estas cifras.</p>");
        tabla(contenido2, List.of("Puesto y ubicación", "Área y cargo", "Postura y fecha", "Resultado y acción"),
                empresa.puestos().stream().map(empresa3 -> List.of(
                        "<b>" + escapar(TextoLista.puesto(empresa3.cargo(), empresa3.descripcion())) + "</b><br>" +
                                escapar(empresa3.salon()) + " · Puesto " + empresa3.numero(),
                        texto(empresa3.area(), "Sin área") + "<br>" + texto(empresa3.cargo(), "Sin cargo") +
                                (empresa3.tipoCargo() == null ? "" : "<br>" + etiqueta(empresa3.tipoCargo())),
                        escapar(empresa3.postura()) + "<br>" + Html.fecha(empresa3.fecha()),
                        riesgo(empresa3.puntaje(), empresa3.riesgo()) + (empresa3.riesgo() == null ? "" :
                                "<br>" + escapar(empresa3.riesgo().nombre()) + " · " +
                                        escapar(empresa3.riesgo().accion())))).toList());
        contenido2.append("</div><h3>Peligros y aspectos a mejorar observados</h3>");
        if (empresa.puestos().isEmpty()) contenido2.append("<p>No hay evaluaciones en visitas Finalizadas.</p>");
        for (var empresa4 : empresa.puestos())
            contenido2.append("<div class=\"box informe-observacion\"><h4>Puesto ").append(empresa4.numero())
                    .append(" · ").append(escapar(empresa4.salon())).append("</h4><p><b>Postura:</b> ")
                    .append(escapar(empresa4.postura())).append("</p><p><b>Motivo de elección:</b> ")
                    .append(escapar(empresa4.motivo())).append("</p><p>").append(escapar(empresa4.condiciones()))
                    .append("</p>")
                    .append(empresa4.riesgo() == null ? "<p>Sin análisis guardado; no se asigna riesgo.</p>" :
                            "<p>" + riesgo(empresa4.puntaje(), empresa4.riesgo()) + " · " +
                                    escapar(empresa4.riesgo().accion()) + "</p>").append("</div>");
        contenido2.append("<div class=\"informe-bloque\"><h3>Distribución por área y tipo de cargo</h3>");
        conteos(contenido2, "Puestos por área", empresa2.areas());
        conteos(contenido2, "Puestos por tipo de cargo", empresa2.cargos());
        contenido2.append("<p><b>Área con más puestos en Rojo:</b> ")
                .append(empresa2.areasMasRojo().isEmpty() ? "No hay puestos en Rojo." :
                        String.join(", ", empresa2.areasMasRojo().stream().map(Html::escapar).toList()) +
                        " (se muestran todas si hay empate).").append("</p>");
        contenido2.append("</div><div class=\"informe-bloque\"><h3>Pendientes de arreglo</h3>");
        tabla(contenido2, List.of("Puesto", "Resultado original", "Estado y seguimiento"), empresa.pendientes().stream()
                .map(empresa5 -> List.of("Puesto " + empresa5.numero() + " · " + escapar(empresa5.salon()),
                        riesgo(empresa5.puntaje(), empresa5.riesgo()),
                        etiqueta(empresa5.estado()) + " · " + etiqueta(empresa5.prioridad()) + "<br>" +
                                escapar(empresa5.descripcion()) + "<br>Límite: " + fecha(empresa5.limite()))).toList());
        contenido2.append("</div><div class=\"informe-bloque\"><h3>Sugerencias activas por tipo y estado</h3>");
        List<List<String>> filas = new ArrayList<>();
        for (var tipo : new TreeMap<>(empresa2.sugerencias()).entrySet())
            for (var estado : new TreeMap<>(tipo.getValue()).entrySet())
                filas.add(List.of(etiqueta(tipo.getKey()), etiqueta(estado.getKey()), "" + estado.getValue()));
        tabla(contenido2, List.of("Tipo", "Estado", "Cantidad"), filas);
        contenido2.append("</div>");
        for (var sg : empresa.sugerencias())
            contenido2.append("<div class=\"box informe-sugerencia\"><h4>Puesto ").append(sg.numero()).append(" · ")
                    .append(escapar(sg.salon())).append(" · ").append(escapar(sg.titulo())).append("</h4><p>")
                    .append(escapar(sg.descripcion())).append("</p><p>").append(etiqueta(sg.tipo())).append(" · ")
                    .append(etiqueta(sg.estado())).append(" · Prioridad ").append(etiqueta(sg.prioridad()))
                    .append(" · Límite: ").append(fecha(sg.limite())).append(" · ")
                    .append(sg.origen().equals("SISTEMA") ? "Sugerencia del sistema" : "Escrita por el Administrador")
                    .append("</p></div>");
        if (empresa.sugerencias().isEmpty())
            contenido2.append("<p>No hay sugerencias activas de visitas Finalizadas.</p>");
        contenido2.append("</section>");
    }

    private static void resumen(StringBuilder contenido2, InformeEmpresa i) {
        var empresa = i.totales();
        contenido2.append("<section class=\"informe-resumen\"><h2>Resumen de la empresa</h2><h3>Calific" + "ación " +
                "general</h3>").append(general(i)).append(cobertura(i));
        contenido2.append("<div class=\"informe-cifras resumen-cifras\">");
        cifra(contenido2, "Visita Finalizada", "Visitas Finalizadas", empresa.visitas());
        cifra(contenido2, "Puesto evaluado único", "Puestos evaluados únicos", empresa.puestos());
        cifra(contenido2, "Foto analizada", "Fotos analizadas", empresa.fotosAnalizadas());
        for (String color : List.of("VERDE", "AMARILLO", "ROJO"))
            cifra(contenido2, "Puesto en " + Formularios.etiqueta(color), "Puestos en " + Formularios.etiqueta(color),
                    empresa.colores().getOrDefault(color, 0));
        cifra(contenido2, "Pendiente abierto", "Pendientes abiertos", empresa.pendientes().getOrDefault("ABIERTO", 0));
        cifra(contenido2, "Pendiente arreglado", "Pendientes arreglados",
                empresa.pendientes().getOrDefault("ARREGLADO", 0));
        cifra(contenido2, "Sugerencia activa", "Sugerencias activas",
                empresa.sugerencias().values().stream().flatMap(parametros -> parametros.values().stream())
                        .mapToInt(Integer::intValue).sum());
        contenido2.append("</div><h3>Sugerencias activas por tipo</h3><p class=\"resumen-sugerencias\">");
        List<String> tipos = new ArrayList<>();
        for (String tipo : List.of("MOBILIARIO", "POSTURA", "DESCANSOS", "CARGA", "OTRA"))
            tipos.add(etiqueta(tipo) + ": " +
                    empresa.sugerencias().getOrDefault(tipo, Map.of()).values().stream().mapToInt(Integer::intValue)
                            .sum());
        contenido2.append(String.join(" · ", tipos))
                .append("</p><p><b>Área con más puestos en Rojo de la empresa:</b> ")
                .append(empresa.areasMasRojo().isEmpty() ? "No hay puestos en Rojo." : String.join(", ",
                        empresa.areasMasRojo().stream().map(Html::escapar).toList())).append("</p></section>");
    }

    private static String cantidad(int cantidad2, String singular, String plural) {
        return cantidad2 + " " + (cantidad2 == 1 ? singular : plural);
    }

    private static String generado(InformeEmpresa i) {
        return "<time data-generado datetime=\"" +
                i.generado().atZone(java.time.ZoneId.of("America/Bogota")).toOffsetDateTime() + "\">Generado el " +
                Html.fechaHora(i.generado()) + " · America/Bogota</time>";
    }

    private static void cifra(StringBuilder contenido2, String singular, String plural, int cantidad2) {
        String titulo = cantidad2 == 1 ? singular : plural;
        contenido2.append("<div class=\"box\"><span class=\"sm\">").append(escapar(titulo)).append("</span><b>")
                .append(cantidad2).append("</b></div>");
    }

    private static void conteos(StringBuilder contenido2, String titulo, Map<String, Integer> mapa) {
        contenido2.append("<h4>").append(escapar(titulo)).append("</h4>");
        tabla(contenido2, List.of("Grupo", "Puestos"), new TreeMap<>(mapa).entrySet().stream()
                .map(parametros -> List.of(etiqueta(parametros.getKey()), "" + parametros.getValue())).toList());
    }

    private static void tabla(StringBuilder contenido2, List<String> cabeceras, List<List<String>> filas) {
        if (filas.isEmpty()) {
            contenido2.append("<p>Sin registros.</p>");
            return;
        }
        contenido2.append("<div class=\"informe-tabla\"><table class=\"resp\"><thead><tr>");
        cabeceras.forEach(texto2 -> contenido2.append("<th scope=\"col\">").append(escapar(texto2)).append("</th>"));
        contenido2.append("</tr></thead><tbody>");
        for (var fila : filas) {
            contenido2.append("<tr>");
            for (int n = 0; n < fila.size(); n++)
                contenido2.append("<td data-label=\"").append(escapar(cabeceras.get(n)))
                        .append("\"><div class=\"cell-value\">").append(fila.get(n)).append("</div></td>");
            contenido2.append("</tr>");
        }
        contenido2.append("</tbody></table></div>");
    }

    private static String riesgo(Integer elemento, Riesgo empresa) {
        return elemento == null ? "Sin resultado" : Html.riesgo(elemento, empresa.color(), empresa.nivel());
    }

    private static String texto(String texto2, String vacio) {
        return escapar(texto2 == null || texto2.isBlank() ? vacio : texto2);
    }

    private static String etiqueta(String texto2) {
        return texto2 == null ? "Sin cargo" : escapar(Formularios.etiqueta(texto2));
    }

    private static String fecha(java.time.LocalDate elemento) {
        return elemento == null ? "Sin definir" : Html.fecha(elemento);
    }

    private static String decimal(BigDecimal elemento) {
        return elemento.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
