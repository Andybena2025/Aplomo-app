package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.List;

/**
 * RF-024, RNF-002: filas con columnas y enlace completo, sobre datos ya autorizados.
 */
final class FilasTrabajo {
    private FilasTrabajo() {
    }

    static String cabecera(SeccionConsulta seccion, Rol rol) {
        List<String> columnas = switch (seccion) {
            case RESERVAS -> List.of("Hora", "Empresa y lugar", "Evaluador", "Estado");
            case VISITAS -> List.of("Fecha", "Empresa y lugar", "Puestos", "Resultado más alto", "Evaluador");
            case PENDIENTES -> List.of("Puesto", "Resultado", "Prioridad", "Fecha límite", "Evaluador", "Estado");
            case PUESTOS -> List.of("Puesto", "Empresa y lugar", "Resultado", "Estado");
            case EVALUACIONES -> List.of("Puesto", "Fecha", "Resultado", "Evaluador", "Estado");
            case SUGERENCIAS -> List.of("Puesto", "Sugerencia", "Prioridad", "Fecha límite", "Evaluador", "Estado");
            default -> List.of("Nombre", "Empresa y lugar", "Estado");
        };
        StringBuilder html = new StringBuilder("<thead><tr>");
        for (String columna : columnas) {
            if (!columna.equals("Evaluador") || rol != Rol.EVALUADOR) {
                html.append("<th scope=\"col\">").append(columna).append("</th>");
            }
        }
        if (seccion == SeccionConsulta.RESERVAS && rol == Rol.EVALUADOR) {
            html.append("<th scope=\"col\">Respuesta</th>");
        }
        return html.append("<th scope=\"col\"><span class=\"sr-only\">Abrir</span></th></tr></thead>").toString();
    }

    static String fila(Sesion sesion, Fila f, SeccionConsulta seccion, boolean visible) {
        String titulo = switch (seccion) {
            case PENDIENTES, PUESTOS, EVALUACIONES -> TextoLista.puesto(f);
            case RESERVAS, VISITAS -> f.valor("empresa_nombre");
            default -> f.valor("nombre");
        };
        String detalle = String.join(" · ",
                List.of(f.valor("empresa_nombre"), f.valor("sede_nombre"), f.valor("salon_nombre")).stream()
                        .filter(t -> !t.isBlank()).toList());
        String marcas = sesion.cuenta().rol() == Rol.ADMINISTRADOR ? inactivos(f) : "";
        String enlace =
                "<a class=\"fila-enlace\" title=\"" + Html.escapar(titulo) + "\" href=\"" + seccion.ruta() + "/" +
                        f.id() + "\"><strong>" + Html.escapar(titulo) + "</strong>" + "<small>" +
                        Html.escapar(detalle) +
                        (f.valor("numero").isBlank() ? "" : " · Puesto " + Html.escapar(f.valor("numero"))) +
                        "</small>" + marcas + "</a>";
        StringBuilder html = new StringBuilder("<tr data-registro=\"").append(f.id()).append("\" data-filtro-extra=\"")
                .append(true).append("\" data-estado=\"").append(Html.escapar(estado(f))).append("\" data-fecha=\"")
                .append(Html.escapar(f.valor("fecha"))).append("\" data-evaluador=\"")
                .append(Html.escapar(f.valor("evaluador"))).append("\" data-color=\"")
                .append(Html.escapar(f.valor("color"))).append("\" data-empresa-activa=\"")
                .append(!List.of("0", "false").contains(f.valor("empresa_activa"))).append("\" data-busqueda=\"")
                .append(Html.escapar(titulo + " " + detalle + " " + f.valor("evaluador_nombre")))
                .append("\" style=\"--persona:").append(TextoLista.color(f.valor("evaluador_color"))).append("\"")
                .append(visible ? "" : " hidden").append(">");
        switch (seccion) {
            case RESERVAS -> {
                html.append(td("Hora",
                        "<span class=\"hora-lista\">" + TextoLista.hora(f.valor("inicio")) + "</span><small>a " +
                                TextoLista.hora(f.valor("fin")) + "</small>"));
                html.append(td("Empresa y lugar", enlace));
                persona(html, sesion, f);
                html.append(td("Estado", SistemaVisual.estado(estado(f))));
                if (sesion.cuenta().rol() == Rol.EVALUADOR) html.append(
                        f.valor("estado").equals("ESPERANDO") && true ? acciones(sesion, f) : td("Respuesta", ""));
            }
            case VISITAS -> {
                html.append(td("Fecha", TextoLista.fecha(f.valor("fecha")))).append(td("Empresa y lugar", enlace))
                        .append(td("Puestos", f.valor("puestos_evaluados") + " evaluados"))
                        .append(td("Resultado más alto", riesgo(f)));
                persona(html, sesion, f);
            }
            case PENDIENTES -> {
                String posturas = numero(f.valor("posturas")) > 1 ?
                        "<span class=\"pill\">" + f.valor("posturas") + " posturas</span>" : "";
                String proxima = f.valor("proxima_visita").isBlank() ? "" :
                        "<small>Próxima visita " + TextoLista.fecha(f.valor("proxima_visita")) + "</small>";
                html.append(td("Puesto", enlace + posturas + proxima)).append(td("Resultado", riesgo(f)))
                        .append(td("Prioridad", TextoLista.prioridad(f.valor("prioridad"))))
                        .append(td("Fecha límite", TextoLista.limite(f.valor("fecha_limite"))));
                persona(html, sesion, f);
                html.append(td("Estado", SistemaVisual.estado(f.valor("estado"))));
            }
            case PUESTOS -> html.append(td("Puesto", enlace)).append(td("Empresa y lugar", Html.escapar(detalle)))
                    .append(td("Resultado", riesgo(f))).append(td("Estado", SistemaVisual.estado(f.valor("estado"))));
            case EVALUACIONES -> {
                html.append(td("Puesto", enlace)).append(td("Fecha", TextoLista.fecha(f.valor("fecha"))))
                        .append(td("Resultado", riesgo(f)));
                persona(html, sesion, f);
                html.append(td("Estado", SistemaVisual.estado(f.valor("estado"))));
            }
            case SUGERENCIAS -> {
                html.append("<td data-puesto data-label=\"Puesto\">" + Html.escapar(TextoLista.puesto(f)) + "</td>")
                        .append(td("Sugerencia", enlace))
                        .append(td("Prioridad", TextoLista.prioridad(f.valor("prioridad"))))
                        .append(td("Fecha límite", TextoLista.limite(f.valor("fecha_limite"))));
                persona(html, sesion, f);
                html.append(td("Estado", SistemaVisual.estado(f.valor("estado"))));
            }
            default -> html.append(td("Nombre", enlace)).append(td("Empresa y lugar", Html.escapar(detalle)))
                    .append(td("Estado", SistemaVisual.estado(f.valor("estado"))));
        }
        return html.append("<td aria-hidden=\"true\">›</td></tr>").toString();
    }

    private static String inactivos(Fila f) {
        StringBuilder html = new StringBuilder();
        for (String nivel : List.of("empresa", "sede", "salon", "puesto")) {
            boolean femenino = nivel.equals("empresa") || nivel.equals("sede");
            if (f.valor(nivel + (femenino ? "_activa" : "_activo")).equals("0")) {
                html.append("<span class=\"registro-inactivo\" title=\"").append(nivel).append("\">")
                        .append(femenino ? "Inactiva" : "Inactivo").append("</span>");
            }
        }
        return html.toString();
    }

    private static String acciones(Sesion sesion, Fila f) {
        boolean solicitud = f.valor("registrador_rol").equals("USUARIO");
        String raiz = (solicitud ? "/solicitudes-visita/" : "/reservas/") + f.id();
        return "<td class=\"acciones-en-fila\" data-label=\"Respuesta\"><form method=\"post\" action=\"" + raiz +
                "/aceptar\">" + Html.csrf(sesion) + "<button class=\"btn p\">Aceptar</button></form>" +
                "<a class=\"btn\" href=\"" + raiz + "#llamada\">Pedir llamada</a>" + "<a class=\"btn\" href=\"" + raiz +
                "#informacion\">Pedir datos</a></td>";
    }

    private static String td(String etiqueta, String contenido) {
        return "<td data-label=\"" + etiqueta + "\">" + contenido + "</td>";
    }

    private static void persona(StringBuilder html, Sesion sesion, Fila f) {
        if (sesion.cuenta().rol() != Rol.EVALUADOR)
            html.append(td("Evaluador", TextoLista.persona(f.valor("evaluador_nombre"), f.valor("evaluador_color"))));
    }

    private static String riesgo(Fila fila) {
        if (fila.valor("puntaje").isBlank()) return "<span class=\"muted\">Sin resultado</span>";
        return SistemaVisual.riesgo(numero(fila.valor("puntaje")), fila.valor("color"), numero(fila.valor("nivel")));
    }

    static String estado(Fila f) {
        return f.valor("visita_estado").equals("EN_CURSO") ? "EN_CURSO" : f.valor("estado");
    }

    private static int numero(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (RuntimeException error) {
            return 0;
        }
    }
}
