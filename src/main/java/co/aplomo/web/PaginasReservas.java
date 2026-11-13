package co.aplomo.web;

import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.modelo.*;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Reservas.Catalogo;
import java.time.LocalDate;
import java.util.*;
import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;
import static co.aplomo.web.Formularios.*;

/** RF-009b a RF-012: agenda y reserva con el diseño visual del mock. */
public final class PaginasReservas {
    private PaginasReservas() {}

    /** Contexto obtenido antes de guardar, para conservar datos y opciones si la base falla. */
    public record Contexto(Ficha anterior, Catalogo catalogo, Map<String,String> datos, LocalDate hoy) {}

    /** Lista las reservas ya filtradas por empresa, sin acciones de gestión para Usuario. */
    public static String lista(Sesion sesion, List<Ficha> reservas) {
        StringBuilder html = new StringBuilder("<div class=\"row between\"><p class=\"muted\">")
                .append(Html.plural(reservas.size(), "reserva")).append("</p>");
        if (admin(sesion)) html.append(enlace("/reservas/nueva", "Nueva reserva", true));
        html.append("</div>");
        if (reservas.isEmpty()) {
            html.append("<section class=\"card stack\"><h2>Tu agenda de visitas</h2><p class=\"muted\">Todavía no hay reservas registradas.</p></section>");
        } else {
            html.append("<div class=\"tablewrap\"><table class=\"resp\"><thead><tr><th>Reserva</th><th>Empresa y lugar</th><th>Fecha y horario</th><th>Categoría</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>");
            for (Ficha ficha : reservas) {
                Reserva r = ficha.reserva();
                html.append("<tr><td data-label=\"Reserva\"><div class=\"cell-value\">").append(escapar(r.codigo())).append("</div></td><td data-label=\"Empresa\"><div class=\"cell-value\">")
                        .append(escapar(ficha.empresaNombre())).append("<br><span class=\"muted small\">")
                        .append(escapar(ficha.sedeNombre())).append(" · ").append(escapar(ficha.salonNombre()))
                        .append("</span></div></td><td data-label=\"Fecha\" class=\"num\"><div class=\"cell-value\">").append(Html.fecha(r.fecha())).append("<br>")
                        .append(escapar(r.inicio().toString())).append("–").append(escapar(r.fin().toString()))
                        .append("</div></td><td data-label=\"Categoría\"><div class=\"cell-value\">").append(escapar(categoria(r.categoria()))).append("</div></td><td data-label=\"Estado\"><div class=\"cell-value\"><span class=\"")
                        .append(claseEstado(r.estado())).append("\">").append(escapar(estado(r.estado()))).append("</span></div></td><td><div class=\"cell-value\">")
                        .append(enlace("/reservas/" + r.id(), "Ver reserva", false)).append("</div></td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        return Html.pagina("Reservas", html.toString(), sesion);
    }

    /** Detalla la reserva y muestra solo las transiciones que siguen disponibles. */
    public static String detalle(Sesion sesion, Ficha ficha, String aviso, boolean exito) {
        Reserva r = ficha.reserva();
        StringBuilder html = new StringBuilder(enlace("/reservas", "← Reservas", false));
        html.append(Html.aviso(aviso, exito)).append("<section class=\"card stack\"><div class=\"row between\"><h2>")
                .append(escapar(ficha.empresaNombre())).append("</h2><span class=\"").append(claseEstado(r.estado())).append("\">").append(escapar(estado(r.estado())))
                .append("</span></div><p class=\"muted\">").append(escapar(ficha.sedeNombre())).append(" · ")
                .append(escapar(ficha.salonNombre())).append("</p><dl class=\"datos-cuenta\">")
                .append(dato("Fecha", Html.fecha(r.fecha()))).append(dato("Horario", r.inicio() + "–" + r.fin()))
                .append(dato("Categoría", categoria(r.categoria()))).append(dato("Descripción", r.descripcion()))
                .append(dato("Notas", r.notas()));
        if (r.motivo() != null) html.append(dato("Cancelación", r.motivo()));
        html.append("</dl>");
        if (admin(sesion) && r.editable()) {
            html.append("<div class=\"row\">").append(enlace("/reservas/" + r.id() + "/editar", "Editar reserva", false));
            if (r.estado().equals("ESPERANDO")) {
                html.append("<form method=\"post\" action=\"/reservas/").append(r.id()).append("/aceptar\">")
                        .append(Html.csrf(sesion)).append("<button class=\"btn p\">Aceptar reserva</button></form>");
            }
            if (r.estado().equals("EN_COLA") && ficha.visita() == null) {
                html.append("<form method=\"post\" action=\"/reservas/").append(r.id()).append("/iniciar-visita\">")
                        .append(Html.csrf(sesion)).append("<button class=\"btn p\">Iniciar visita</button></form>");
            }
            if (ficha.visita() == null) html.append(enlace("/reservas/" + r.id() + "/cancelar", "Cancelar reserva", false));
            else html.append("<p class=\"hint\">La visita ya se inició; su reserva se conserva hasta finalizarla.</p>");
            html.append("</div>");
        }
        if (ficha.visita() != null && (admin(sesion) || "FINALIZADA".equals(ficha.estadoVisita()))) {
            html.append(enlace("/visitas/" + ficha.visita(), "Ver visita", false));
        }
        html.append("</section>");
        return Html.pagina(r.codigo(), html.toString(), sesion,"Reservas · Detalle","");
    }

    /** Presenta el formulario de alta o edición manteniendo todos los datos no secretos. */
    public static String formulario(Sesion sesion, Contexto contexto, Map<String,String> enviados, String error) {
        Map<String,String> datos = new HashMap<>(contexto.datos());
        datos.putAll(enviados);
        boolean nueva = contexto.anterior() == null;
        String accion = nueva ? "/reservas" : "/reservas/" + contexto.anterior().reserva().id() + "/editar";
        StringBuilder html = new StringBuilder(enlace(nueva ? "/reservas" : "/reservas/" + contexto.anterior().reserva().id(), "← Volver", false));
        html.append(Html.aviso(error, false)).append("<div role=\"status\" aria-live=\"polite\" data-aviso-reserva></div>")
                .append("<form class=\"card stack\" method=\"post\" action=\"").append(accion).append("\"")
                .append(nueva ? " data-reserva" : "").append(">").append(Html.csrf(sesion));
        if (nueva) {
            html.append(selectores(contexto.catalogo(), datos))
                    .append("<p class=\"hint\">Si falta un lugar, puedes registrarlo abajo sin perder los datos de esta reserva.</p>");
        } else {
            Ficha ficha = contexto.anterior();
            html.append("<dl class=\"datos-cuenta\">").append(dato("Empresa", ficha.empresaNombre()))
                    .append(dato("Sede", ficha.sedeNombre())).append(dato("Salón", ficha.salonNombre())).append("</dl>");
        }
        html.append("<div class=\"grid3\">")
                .append(campo(datos, "fecha", "Fecha", "date", 10, true).replace(" required", " min=\"" + contexto.hoy() + "\" required"))
                .append(campo(datos, "inicio", "Hora de inicio", "time", 8, true))
                .append(campo(datos, "fin", "Hora de fin", "time", 8, true)).append("</div>")
                .append(selector(datos, "categoria", "Categoría", List.of(new Opcion("EVALUACION_INICIAL", "Evaluación inicial"),
                        new Opcion("SEGUIMIENTO", "Seguimiento"), new Opcion("REEVALUACION", "Reevaluación")), true))
                .append(texto(datos, "descripcion", "Descripción (opcional)", 255, false))
                .append(texto(datos, "notas", "Notas (opcional)", 65535, false))
                .append("<div class=\"row\"><button class=\"btn p\">Guardar reserva</button>")
                .append(enlace("/reservas", "Volver a reservas", false)).append("</div></form>");
        if (nueva) html.append(altas(sesion));
        return Html.pagina(nueva ? "Nueva reserva" : "Editar " + contexto.anterior().reserva().codigo(), nueva?Html.conPanel(html.toString(),"Mide la jornada real.","Agenda la visita en el horario real de trabajo: RULA evalúa la postura que el trabajador mantiene más tiempo."):html.toString(), sesion);
    }

    /** Fragmento de catálogos que se renueva al crear un lugar sin recargar el formulario principal. */
    public static String selectores(Catalogo catalogo, Map<String,String> datos) {
        StringBuilder html = new StringBuilder("<div class=\"grid3\" data-catalogo-reserva>");
        html.append("<div class=\"field\"><label for=\"empresa\">Empresa</label><select class=\"input\" id=\"empresa\" name=\"empresa\" required>")
                .append("<option value=\"\">Selecciona una empresa</option>");
        for (Empresa empresa : catalogo.empresas()) html.append(opcion(empresa.id(), empresa.nombre(), null, datos.get("empresa"), false));
        html.append("</select></div><div class=\"field\"><label for=\"sede\">Sede</label><select class=\"input\" id=\"sede\" name=\"sede\" required>")
                .append("<option value=\"\">Selecciona una sede</option>");
        for (Sede sede : catalogo.sedes()) {
            html.append(opcion(sede.id(), sede.nombre(), sede.empresa(), datos.get("sede"), !Integer.toString(sede.empresa()).equals(datos.get("empresa"))));
        }
        html.append("</select></div><div class=\"field\"><label for=\"salon\">Salón</label><select class=\"input\" id=\"salon\" name=\"salon\" required>")
                .append("<option value=\"\">Selecciona un salón</option>");
        for (Salon salon : catalogo.salones()) {
            html.append(opcion(salon.id(), salon.nombre(), salon.sede(), datos.get("salon"), !Integer.toString(salon.sede()).equals(datos.get("sede"))));
        }
        return html.append("</select></div></div>").toString();
    }

    /** Pide el motivo antes de cancelar y lo conserva si hay un error al guardar. */
    public static String cancelar(Sesion sesion, Ficha ficha, String motivo, String error) {
        return Html.pagina("Cancelar " + ficha.reserva().codigo(), enlace("/reservas/" + ficha.reserva().id(), "← Volver", false)
                + Html.aviso(error, false) + "<form class=\"card stack cuenta-form\" method=\"post\" action=\"/reservas/"
                + ficha.reserva().id() + "/cancelar\">" + Html.csrf(sesion)
                + texto(Map.of("motivo", motivo == null ? "" : motivo), "motivo", "Motivo de cancelación", 255, true)
                + "<button class=\"btn p\">Confirmar cancelación</button></form>", sesion);
    }

    /** Traduce los estados internos a los nombres aprobados de la interfaz. */
    public static String estado(String valor) {
        return switch (valor) {
            case "ESPERANDO" -> "Esperando"; case "EN_COLA" -> "En cola";
            case "FINALIZADA" -> "Finalizada"; case "CANCELADA" -> "Cancelada";
            default -> "No disponible";
        };
    }

    /** Clase CSS del tag según el estado, como en el mock: En cola = good, Esperando = warn. */
    public static String claseEstado(String valor) {
        return switch (valor) {
            case "EN_COLA" -> "tag good"; case "ESPERANDO" -> "tag warn";
            case "FINALIZADA" -> "tag good"; default -> "tag";
        };
    }

    /** Traduce las categorías de visita sin mostrar códigos del enum. */
    public static String categoria(String valor) {
        return switch (valor) {
            case "EVALUACION_INICIAL" -> "Evaluación inicial"; case "SEGUIMIENTO" -> "Seguimiento";
            case "REEVALUACION" -> "Reevaluación"; default -> "No disponible";
        };
    }

    private static String altas(Sesion sesion) {
        Map<String,String> vacio = Map.of();
        String empresa = campo(vacio, "nombre", "Nombre de la empresa", "text", 120, true)
                + campo(vacio, "nit", "NIT", "text", 20, true);
        String sede = "<input type=\"hidden\" name=\"empresa\">" + campo(vacio, "nombre", "Nombre de la sede", "text", 100, true)
                + campo(vacio, "direccion", "Dirección", "text", 150, true) + campo(vacio, "ciudad", "Ciudad", "text", 60, true)
                + campo(vacio, "telefono", "Teléfono", "tel", 20, true) + campo(vacio, "horario", "Horario", "text", 100, true)
                + texto(vacio, "descripcion", "Descripción", 200, true);
        String salon = "<input type=\"hidden\" name=\"sede\">" + campo(vacio, "nombre", "Nombre del salón", "text", 100, true)
                + texto(vacio, "descripcion", "Descripción", 200, true) + campo(vacio, "piso", "Piso", "number", 4, true)
                + campo(vacio, "capacidad_puestos", "Capacidad de puestos", "number", 5, true);
        return "<section class=\"stack\"><h2>Registrar un lugar</h2>"
                + alta(sesion, "empresa", "Registrar empresa", empresa)
                + alta(sesion, "sede", "Registrar sede en la empresa seleccionada", sede)
                + alta(sesion, "salon", "Registrar salón en la sede seleccionada", salon) + "</section>";
    }

    private static String alta(Sesion sesion, String tipo, String titulo, String campos) {
        String unicos = campos.replace("id=\"", "id=\"alta-" + tipo + "-").replace("for=\"", "for=\"alta-" + tipo + "-");
        return "<details class=\"card stack\"><summary>" + escapar(titulo) + "</summary><form class=\"stack\" method=\"post\" action=\"/reservas/crear-"
                + tipo + "\" data-alta-reserva=\"" + tipo + "\">" + Html.csrf(sesion) + unicos
                + "<div role=\"alert\" data-error-alta></div><button class=\"btn\">Guardar "
                + (tipo.equals("salon") ? "salón" : tipo) + "</button></form></details>";
    }

    private static String opcion(int id, String nombre, Integer padre, String elegida, boolean oculta) {
        return "<option value=\"" + id + "\"" + (padre == null ? "" : " data-padre=\"" + padre + "\"")
                + (Integer.toString(id).equals(elegida) ? " selected" : "") + (oculta ? " hidden disabled" : "")
                + ">" + escapar(nombre) + "</option>";
    }

    private static String dato(String titulo, String valor) {
        return "<div><dt>" + escapar(titulo) + "</dt><dd class=\"texto-notas\">" + escapar(valor) + "</dd></div>";
    }

    private static boolean admin(Sesion sesion) { return sesion.cuenta().rol() == Rol.ADMINISTRADOR; }
}
