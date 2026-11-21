package co.aplomo.web;

import co.aplomo.modelo.Area;
import co.aplomo.modelo.Cargo;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.Formularios.Opcion;
import static co.aplomo.web.Formularios.campo;
import static co.aplomo.web.Formularios.etiqueta;
import static co.aplomo.web.Formularios.opciones;
import static co.aplomo.web.Formularios.selector;
import static co.aplomo.web.Formularios.texto;

/**
 * RF-004 a RF-010, RF-028: páginas con los colores, tipografías y componentes del mock.
 */
public final class PaginasEmpresas {
    private PaginasEmpresas() {
    }

    /**
     * Contexto del formulario obtenido antes de guardar, para conservarlo incluso si falla MySQL. RNF-011.
     */
    public record Contexto(String tipo, int id, int padre, String volver, Map<String, String> datos, List<Area> areas,
                           List<Cargo> cargos) {
    }

    /**
     * Lista empresas respetando el alcance ya filtrado por el servicio. RNF-011.
     */
    public static String lista(Sesion sesion, List<Empresa> empresas, String aviso) {
        return listado(sesion,
                empresas.stream().map(empresa -> new co.aplomo.servicio.Empresas.Resumen(empresa, "", List.of()))
                        .toList(), aviso);
    }

    /**
     * RF-002/004: filas completas con metadatos registrados, búsqueda y estados.
     */
    public static String listado(Sesion sesion, List<co.aplomo.servicio.Empresas.Resumen> empresas, String aviso) {
        return listado(sesion, empresas, aviso, Map.of());
    }

    /**
     * RF-024: conserva la búsqueda inicial además de la búsqueda en vivo.
     */
    public static String listado(Sesion sesion, List<co.aplomo.servicio.Empresas.Resumen> empresas, String aviso,
            Map<String, String> filtros) {
        String consulta = filtros.getOrDefault("q", "");
        String normalizada = co.aplomo.servicio.Validacion.nombre(consulta);
        long coincidencias = empresas.stream().filter(resumen -> co.aplomo.servicio.Validacion.nombre(
                        resumen.empresa().nombre() + " " + resumen.empresa().nit() + " " + resumen.ciudad())
                .contains(normalizada)).count();
        StringBuilder html = new StringBuilder(Html.aviso(aviso, aviso != null));
        html.append("<section data-lista data-seccion=\"EMPRESAS\"><div class=\"lista-herramientas\">" +
                "<input type=\"search\" class=\"input\" data-lista-buscar aria-label=\"Buscar empresa\" " +
                "placeholder=\"Buscar empresa\" value=\"").append(escapar(consulta)).append("\"><div class=\"chips\">");
        for (String[] filtro : List.of(new String[]{"ACTIVO", "Activas"}, new String[]{"INACTIVO", "Inactivas"},
                new String[]{"TODOS", "Todas"})) {
            html.append("<button class=\"chip\" type=\"button\" data-lista-filtro=\"").append(filtro[0])
                    .append("\" aria-pressed=\"false\">").append(filtro[1]).append("</button>");
        }
        html.append("</div></div><p class=\"muted\" data-lista-contador role=\"status\">").append(coincidencias)
                .append(" resultados</p><div class=\"lista-filas\">");
        for (var resumen : empresas) {
            Empresa empresa = resumen.empresa();
            String lugar = resumen.ciudad();
            if (empresa.sector() != null && !empresa.sector().isBlank()) {
                lugar += (lugar.isBlank() ? "" : " · ") + empresa.sector();
            }
            html.append("<a class=\"lista-fila empresa-fila\" href=\"/empresas/").append(empresa.id())
                    .append("\" data-registro data-estado=\"").append(empresa.activo() ? "ACTIVO" : "INACTIVO")
                    .append("\"><span class=\"lista-identidad\"><strong title=\"").append(escapar(empresa.nombre()))
                    .append("\">").append(escapar(empresa.nombre())).append("</strong><small class=\"muted\">")
                    .append(escapar(lugar)).append("</small></span><span class=\"equipo-empresa\">");
            for (var persona : resumen.equipo()) {
                String color = persona.color() != null &&
                        co.aplomo.servicio.ColoresAgenda.PALETA.contains(persona.color()) ? persona.color() : "#C9CCD2";
                html.append("<span title=\"").append(escapar(persona.nombre()))
                        .append("\"><i class=\"persona-color\" " + "style=\"--persona:").append(color)
                        .append("\" aria-hidden=\"true\"></i>").append(escapar(persona.nombre())).append("</span>");
            }
            html.append("</span>").append(SistemaVisual.estado(empresa.activo() ? "ACTIVO" : "INACTIVO"))
                    .append("<span aria-hidden=\"true\">›</span></a>");
        }
        html.append("</div><p data-lista-vacia hidden>No hay empresas con estos filtros.</p></section>");
        String acciones = admin(sesion) ? enlace("/empresas/nueva", "Nueva empresa", true) : "";
        return Html.pagina("Empresas", html.toString(), sesion, "Gestión · Empresas", acciones, false);
    }

    /**
     * Muestra la empresa y sus catálogos; el Usuario recibe únicamente enlaces de consulta. RNF-011.
     */
    public static String empresa(Sesion sesion, Empresa empresa, List<Sede> sedes, List<Area> areas, List<Cargo> cargos,
            String aviso) {
        return empresa(sesion, empresa, sedes, areas, cargos, aviso, List.of());
    }

    /**
     * RF-003: asignaciones de la empresa, sin datos privados de cuenta.
     */
    public static String empresa(Sesion sesion, Empresa empresa, List<Sede> sedes, List<Area> areas, List<Cargo> cargos,
            String aviso, List<Cuenta> cuentas) {
        return empresa(sesion, empresa, sedes, areas, cargos, aviso, cuentas, admin(sesion), admin(sesion), List.of());
    }

    /**
     * RF-002: recibe capacidades comprobadas en el servidor, sin deducirlas del rol visible.
     */
    public static String empresa(Sesion sesion, Empresa empresa, List<Sede> sedes, List<Area> areas, List<Cargo> cargos,
            String aviso, List<Cuenta> cuentas, boolean edita, boolean espacios, List<Cuenta> transferibles) {
        return empresa(sesion, empresa, sedes, areas, cargos, aviso, cuentas, edita, espacios, transferibles,
                cuentas.stream().filter(cuenta -> cuenta.rol() == Rol.EVALUADOR)
                        .map(cuenta -> new co.aplomo.servicio.Empresas.Persona(cuenta.id(), cuenta.nombre(),
                                cuenta.colorAgenda())).toList());
    }

    /**
     * RF-003/004: datos y equipo en tarjetas separadas, con capacidades comprobadas por el servidor.
     */
    public static String empresa(Sesion sesion, Empresa empresa, List<Sede> sedes, List<Area> areas, List<Cargo> cargos,
            String aviso, List<Cuenta> cuentas, boolean edita, boolean espacios, List<Cuenta> transferibles,
            List<co.aplomo.servicio.Empresas.Persona> equipo) {
        return empresa(sesion, empresa, sedes, areas, cargos, aviso, cuentas, edita, espacios, transferibles, equipo,
                Map.of());
    }

    /**
     * RF-005/008: datos, equipo y cantidades comprobadas de cada sede.
     */
    public static String empresa(Sesion sesion, Empresa empresa, List<Sede> sedes, List<Area> areas, List<Cargo> cargos,
            String aviso, List<Cuenta> cuentas, boolean edita, boolean espacios, List<Cuenta> transferibles,
            List<co.aplomo.servicio.Empresas.Persona> equipo, Map<Integer, List<Integer>> cantidades) {
        String ruta = "/empresas/" + empresa.id();
        StringBuilder html = new StringBuilder(
                Html.aviso(aviso, "Datos guardados.".equals(aviso) || "Registro reactivado.".equals(aviso)));
        html.append(avisoInactivo(sesion, empresa.activo(), "Esta empresa está desactivada.", "empresa", empresa.id()))
                .append(enlace("/empresas", "← Empresas", false))
                .append("<div class=\"grid2 empresa-paneles\"><section class=\"card stack\">")
                .append(estado(empresa.activo())).append("<dl class=\"datos-cuenta\">")
                .append(dato("Sector", empresa.sector())).append(dato("NIT", empresa.nit()))
                .append(dato("Descripción", empresa.descripcion())).append(dato("Teléfono", empresa.telefono()))
                .append(dato("Correo", empresa.correo())).append(dato("Contacto", empresa.contacto())).append("</dl>")
                .append(acciones(sesion, ruta, empresa.activo(), edita, edita)).append("</section>");

        String accionesPagina = enlace("/calificaciones?empresa=" + empresa.id(), "Ver calificaciones", false);
        if (admin(sesion)) accionesPagina += enlace("/informes/empresa/" + empresa.id(), "Generar informe", true) +
                enlace("/evaluaciones?empresa=" + empresa.id(), "Ver evaluaciones de la empresa", false);
        if (sesion.cuenta().rol() != Rol.USUARIO) {
            html.append("<section class=\"card stack\"><h2>Equipo de evaluación</h2>");
            for (var persona : equipo) {
                String color = persona.color() != null &&
                        co.aplomo.servicio.ColoresAgenda.PALETA.contains(persona.color()) ? persona.color() : "#C9CCD2";
                html.append("<p class=\"row\"><i class=\"persona-color\" style=\"--persona:").append(color)
                        .append("\" aria-hidden=\"true\"></i>").append(escapar(persona.nombre())).append("</p>");
            }
            if (equipo.isEmpty()) html.append("<p class=\"muted\">Sin Evaluador asignado.</p>");
            if (sesion.cuenta().rol() == Rol.ADMINISTRADOR) {
                long usuarios = cuentas.stream().filter(cuenta -> cuenta.rol() == Rol.USUARIO).count();
                html.append(enlace("/usuarios?empresa=" + empresa.id(), "Cuentas vinculadas · " + usuarios, false));
            }
            html.append("</section>");
        }
        html.append("</div>").append(transferencia(sesion, empresa.id(), transferibles));
        html.append(seccion("Sedes", espacios && empresa.activo() ? ruta + "/sedes/nuevo" : null, "Nueva sede",
                enlace("/sedes?empresa=" + empresa.id(), "Consultar sedes", false)));
        for (Sede sede : sedes) {
            List<Integer> totales = cantidades.getOrDefault(sede.id(), List.of(0, 0));
            html.append("<a class=\"card sede-tarjeta\" href=\"/sedes/").append(sede.id()).append("\"><h3>")
                    .append(escapar(sede.nombre())).append("</h3><p class=\"muted\">")
                    .append(escapar(sede.ciudad() + " · " + sede.direccion())).append("</p>")
                    .append("<p class=\"hint\">").append(Html.plural(totales.get(0), "salón")).append(" · ")
                    .append(Html.plural(totales.get(1), "puesto")).append(" ›</p>")
                    .append(SistemaVisual.estado(sede.activo() ? "ACTIVA" : "INACTIVA"))
                    .append("<span aria-hidden=\"true\">›</span></a>");
        }
        html.append(finSeccion(sedes.isEmpty(), "No hay sedes registradas."));
        html.append(seccion("Áreas", espacios && empresa.activo() ? ruta + "/areas/nuevo" : null, "Nueva área",
                enlace("/areas?empresa=" + empresa.id(), "Consultar áreas", false)));
        for (Area area : areas)
            html.append(catalogo(area.nombre(), area.activo(),
                    acciones(sesion, "/areas/" + area.id(), area.activo(), espacios, edita)));
        html.append(finSeccion(areas.isEmpty(), "No hay áreas registradas."));
        html.append(seccion("Cargos", espacios && empresa.activo() ? ruta + "/cargos/nuevo" : null, "Nuevo cargo",
                enlace("/cargos?empresa=" + empresa.id(), "Consultar cargos", false)));
        for (Cargo cargo : cargos)
            html.append(catalogo(cargo.nombre(), cargo.activo(),
                    acciones(sesion, "/cargos/" + cargo.id(), cargo.activo(), espacios, edita)));
        html.append(finSeccion(cargos.isEmpty(), "No hay cargos registrados."));
        return Html.pagina(empresa.nombre(), html.toString(), sesion, "Gestión · Empresas", accionesPagina);
    }

    /**
     * Muestra los datos de una sede y sus salones sin perder los registros inactivos. RNF-011.
     */
    public static String sede(Sesion sesion, Sede sede, Empresa empresa, List<Salon> salones, String aviso) {
        return sede(sesion, sede, empresa, salones, aviso, admin(sesion), sesion.cuenta().rol() == Rol.ADMINISTRADOR);
    }

    /**
     * RF-002: respeta la propiedad de la empresa también al presentar sus espacios.
     */
    public static String sede(Sesion sesion, Sede sede, Empresa empresa, List<Salon> salones, String aviso,
            boolean edita, boolean estados) {
        String ruta = "/sedes/" + sede.id();
        StringBuilder html = new StringBuilder(
                Html.aviso(aviso, "Datos guardados.".equals(aviso) || "Registro reactivado.".equals(aviso)));
        html.append(avisoInactivo(sesion, sede.activo(), "Esta sede está desactivada.", "sede", sede.id()))
                .append(enlace("/empresas/" + empresa.id(), "← " + empresa.nombre(), false))
                .append("<section class=\"card stack\">").append(estado(sede.activo()))
                .append("<dl class=\"datos-cuenta\">").append(dato("Dirección", sede.direccion()))
                .append(dato("Ciudad", sede.ciudad())).append(dato("Teléfono", sede.telefono()))
                .append(dato("Horario", sede.horario())).append(dato("Descripción", sede.descripcion())).append("</dl>")
                .append(acciones(sesion, ruta, sede.activo(), edita, estados)).append("</section>")
                .append(seccion("Salones", edita && sede.activo() && empresa.activo() ? ruta + "/salones/nuevo" : null,
                        "Nuevo " + "salón",
                        enlace("/salones?empresa=" + empresa.id() + "&sede=" + sede.id(), "Consultar salones", false)));
        for (Salon salon : salones)
            html.append(tarjeta(salon.nombre(), "Piso " + salon.piso() + " · Capacidad " + salon.capacidad(),
                    salon.activo(), enlace("/salones/" + salon.id(), "Ver salón", false)));
        html.append(finSeccion(salones.isEmpty(), "No hay salones registrados."));
        return Html.pagina(sede.nombre(), html.toString(), sesion, "Gestión · Sedes", "");
    }

    /**
     * Muestra el salón y los puestos, con el número y su último puntaje si ya existe. RNF-011.
     */
    public static String salon(Sesion sesion, Salon salon, Sede sede, Empresa empresa, List<Puesto> puestos,
            String aviso) {
        return salon(sesion, salon, sede, empresa, puestos, aviso, admin(sesion),
                sesion.cuenta().rol() == Rol.ADMINISTRADOR);
    }

    /**
     * RF-002: respeta la propiedad de la empresa también al presentar sus espacios.
     */
    public static String salon(Sesion sesion, Salon salon, Sede sede, Empresa empresa, List<Puesto> puestos,
            String aviso, boolean edita, boolean estados) {
        String ruta = "/salones/" + salon.id();
        StringBuilder html = new StringBuilder(
                Html.aviso(aviso, "Datos guardados.".equals(aviso) || "Registro reactivado.".equals(aviso)));
        html.append(avisoInactivo(sesion, salon.activo(), "Este salón está desactivado.", "salon", salon.id()))
                .append(enlace("/sedes/" + sede.id(), "← " + sede.nombre(), false))
                .append("<section class=\"card stack\">").append(estado(salon.activo()))
                .append("<dl class=\"datos-cuenta\">").append(dato("Descripción", salon.descripcion()))
                .append(dato("Piso", Integer.toString(salon.piso())))
                .append(dato("Capacidad", Integer.toString(salon.capacidad()))).append("</dl>")
                .append(acciones(sesion, ruta, salon.activo(), edita, estados)).append("</section>")
                .append(seccion("Puestos",
                        edita && salon.activo() && sede.activo() && empresa.activo() ? ruta + "/puestos/nuevo" : null,
                        "Nuevo puesto"));
        html.append(enlace("/puestos?empresa=" + empresa.id() + "&sede=" + sede.id() + "&salon=" + salon.id(),
                "Consultar puestos", false));
        for (Puesto puesto : puestos) {
            String detalle = puesto.descripcion() + " · " + etiqueta(puesto.tipo()) + " · " + puesto.ubicacion() +
                    (puesto.puntaje() == null ? " · Sin evaluar" : "");
            html.append(tarjeta("Puesto " + puesto.numero(), detalle, puesto.activo(),
                    (puesto.puntaje() == null ? "" : Html.riesgo(puesto.puntaje(), puesto.color(), puesto.nivel())) +
                            acciones(sesion, "/puestos/" + puesto.id(), puesto.activo(), edita, estados) +
                            (admin(sesion) ? enlace("/puestos/" + puesto.id(), "Ver historial", false) : "")));
        }
        html.append(finSeccion(puestos.isEmpty(), "No hay puestos registrados."));
        return Html.pagina(salon.nombre(), html.toString(), sesion, "Gestión · Salones", "");
    }

    /**
     * RNF-009, RF-028/024/020: presenta el historial sin ocultar evaluaciones por inactividad.
     */
    public static String historialPuesto(Sesion sesion2, co.aplomo.servicio.Empresas.HistorialPuesto empresa2) {
        var elemento = empresa2.puesto();
        StringBuilder html = new StringBuilder(
                "<section class=\"card stack\"><h2>Puesto " + elemento.numero() + "</h2>" + estado(elemento.activo()));
        html.append("<p>").append(enlace("/empresas/" + empresa2.empresa().id(), empresa2.empresa().nombre(), false))
                .append(estadoNivel(empresa2.empresa().activo(), true)).append(" &gt; ")
                .append(enlace("/sedes/" + empresa2.sede().id(), empresa2.sede().nombre(), false))
                .append(estadoNivel(empresa2.sede().activo(), true)).append(" &gt; ")
                .append(enlace("/salones/" + empresa2.salon().id(), empresa2.salon().nombre(), false))
                .append(estadoNivel(empresa2.salon().activo(), false)).append("</p><dl class=\"datos-cuenta\">")
                .append(dato("Descripción", elemento.descripcion())).append(dato("Tipo", etiqueta(elemento.tipo())))
                .append(dato("Ubicación", elemento.ubicacion())).append("<div><dt>Área</dt><dd>")
                .append(empresa2.area() == null ? "Sin área" :
                        escapar(empresa2.area().nombre()) + estadoNivel(empresa2.area().activo(), true))
                .append("</dd></div>").append("<div><dt>Cargo</dt><dd>").append(empresa2.cargo() == null ? "Sin cargo" :
                        escapar(empresa2.cargo().nombre()) + estadoNivel(empresa2.cargo().activo(), false))
                .append("</dd></div></dl></section>")
                .append("<section class=\"stack\"><h2>Evaluaciones del puesto</h2>");
        if (empresa2.evaluaciones().isEmpty())
            html.append("<p class=\"muted\">Este puesto todavía no tiene evaluaciones.</p>");
        else {
            html.append("<div class=\"tablewrap\"><table class=\"resp\"><thead><tr><th>Fecha</th><th>" +
                    "Visita/Reserva</th><th>Resultado</th><th>Estado de la visita</th><th>Detalle" +
                    "</th></tr></thead><tbody>");
            for (var elemento2 : empresa2.evaluaciones()) {
                html.append("<tr data-evaluacion=\"").append(elemento2.id()).append("\"><td data-label=\"Fecha\">")
                        .append(Html.fecha(java.time.LocalDate.parse(elemento2.valor("fecha")))).append("</td>")
                        .append("<td data-label=\"Visita/Reserva\">")
                        .append(enlace("/visitas/" + elemento2.valor("visita"), "Ver visita", false)).append("<div>")
                        .append(escapar(elemento2.valor("codigo"))).append("</div></td>")
                        .append("<td data-label=\"Resultado\">").append(Html.riesgo(
                                elemento2.valor("puntaje").isEmpty() ? null : Integer.valueOf(elemento2.valor(
                                        "puntaje")),
                                elemento2.valor("color"),
                                elemento2.valor("nivel").isEmpty() ? 0 : Integer.parseInt(elemento2.valor("nivel"))))
                        .append("</td>").append("<td data-label=\"Estado de la visita\">")
                        .append(escapar(Formularios.etiqueta(elemento2.valor("estado"))))
                        .append("</td><td data-label=\"Detalle\">")
                        .append(enlace("/evaluaciones/" + elemento2.id(), "Ver", false)).append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        return Html.pagina("Historial del puesto", html.append("</section>").toString(), sesion2, "Consultas · Puestos",
                "");
    }

    private static String estadoNivel(boolean activo, boolean femenino) {
        return " <span class=\"tag\">" +
                (activo ? (femenino ? "Activa" : "Activo") : (femenino ? "Inactiva" : "Inactivo")) + "</span>";
    }

    private static String avisoInactivo(Sesion sesion2, boolean activo, String texto, String filtro, int id) {
        return !admin(sesion2) || activo ? "" :
                Html.aviso(texto + " Su historial se conserva y se puede consultar aquí.", false) +
                        enlace("/evaluaciones?" + filtro + "=" + id, "Ver evaluaciones", false);
    }

    /**
     * Presenta un formulario sin hacer consultas, conservando sus campos si guardar falla. RNF-011.
     */
    public static String formulario(Sesion sesion, Contexto contexto, Map<String, String> enviados, String error) {
        Map<String, String> datos = new HashMap<>(contexto.datos());
        datos.putAll(enviados);
        String campos = switch (contexto.tipo()) {
            case "empresas" ->
                    campo(datos, "nombre", "Nombre", "text", 120, true) + campo(datos, "nit", "NIT", "text", 20, true) +
                            texto(datos, "descripcion", "Descripción (opcional)", 255, false) +
                            campo(datos, "telefono", "Teléfono (opcional)", "tel", 20, false) +
                            campo(datos, "correo", "Correo (opcional)", "email", 120, false) +
                            campo(datos, "persona_contacto", "Persona de contacto (opcional)", "text", 100, false) +
                            campo(datos, "sector", "Sector (opcional)", "text", 60, false);
            case "sedes" -> campo(datos, "nombre", "Nombre", "text", 100, true) +
                    campo(datos, "direccion", "Dirección", "text", 150, true) +
                    campo(datos, "ciudad", "Ciudad", "text", 60, true) +
                    campo(datos, "telefono", "Teléfono", "tel", 20, true) +
                    campo(datos, "horario", "Horario", "text", 100, true) +
                    texto(datos, "descripcion", "Descripción", 200, true);
            case "salones" -> campo(datos, "nombre", "Nombre", "text", 100, true) +
                    texto(datos, "descripcion", "Descripción", 200, true) + numero(datos, "piso", "Piso", -128, 127) +
                    numero(datos, "capacidad_puestos", "Capacidad de puestos", 1, 32767);
            case "areas" -> campo(datos, "nombre", "Nombre", "text", 100, true);
            case "cargos" -> campo(datos, "nombre", "Nombre", "text", 100, true) +
                    selector(datos, "tipo", "Tipo de cargo", opciones("OPERATIVO", "ADMINISTRATIVO", "OTRO"), true);
            case "puestos" -> numero(datos, "numero", "Número del puesto", 1, 32767) +
                    texto(datos, "descripcion", "Descripción", 255, true) +
                    selector(datos, "tipo", "Tipo de puesto", opciones("ESCRITORIO", "MESA", "MOSTRADOR", "OTRO"),
                            true) + campo(datos, "ubicacion", "Ubicación", "text", 120, true) +
                    selector(datos, "id_area", "Área (opcional)", opcionesAreas(contexto.areas(), datos.get("id_area")),
                            false) + selector(datos, "id_cargo", "Cargo (opcional)",
                    opcionesCargos(contexto.cargos(), datos.get("id_cargo")), false);
            default -> throw new IllegalArgumentException("Formulario no disponible.");
        };
        String accion = contexto.id() > 0 ?
                "/" + contexto.tipo() + "/" + contexto.id() + "/editar" : rutaNueva(contexto.tipo(), contexto.padre());
        String titulo = titulo(contexto.tipo(), contexto.id() > 0);
        String contenido = enlace(contexto.volver(), "← Volver", false) + Html.aviso(error, false) +
                "<form class=\"card stack cuenta-form\" method=\"post\" action=\"" + accion + "\">" +
                Html.csrf(sesion) + campos + "<div class=\"row\"><button class=\"btn p\">Guardar</button>" +
                enlace(contexto.volver(), "Cancelar", false) + "</div></form>";
        if (contexto.tipo().equals("empresas") && contexto.id() == 0)
            contenido = Html.conPanel(contenido, "Cada puesto, medido con precisión.",
                    "Usa el nombre legal y el NIT exactos: así aparecen en el informe PDF que rec" + "ibe la empresa.");
        return Html.pagina(titulo, contenido, sesion);
    }

    /**
     * Dirección de alta dentro del contexto padre, que evita relaciones editables desde el formulario. RNF-011.
     */
    public static String rutaNueva(String tipo, int padre) {
        return switch (tipo) {
            case "empresas" -> "/empresas";
            case "sedes", "areas", "cargos" -> "/empresas/" + padre + "/" + tipo + "/nuevo";
            case "salones" -> "/sedes/" + padre + "/salones/nuevo";
            case "puestos" -> "/salones/" + padre + "/puestos/nuevo";
            default -> throw new IllegalArgumentException("Formulario no disponible.");
        };
    }

    /**
     * Enlace de navegación con dirección interna y texto escapados. RNF-011.
     */
    public static String enlace(String ruta, String texto, boolean principal) {
        return "<a class=\"btn" + (principal ? " p" : "") + "\" href=\"" + escapar(ruta) + "\">" + escapar(texto) +
                "</a>";
    }

    private static String titulo(String tipo, boolean editar) {
        String nombre = switch (tipo) {
            case "empresas" -> "empresa";
            case "sedes" -> "sede";
            case "salones" -> "salón";
            case "areas" -> "área";
            case "cargos" -> "cargo";
            default -> "puesto";
        };
        if (editar) return "Editar " + nombre;
        return (Set.of("empresa", "sede", "área").contains(nombre) ? "Nueva " : "Nuevo ") + nombre;
    }

    private static String numero(Map<String, String> datos, String nombre, String etiqueta, int minimo, int maximo) {
        return campo(datos, nombre, etiqueta, "number", 8, true).replace(" required",
                " min=\"" + minimo + "\" max=\"" + maximo + "\" step=\"1\" required");
    }

    private static boolean admin(Sesion sesion) {
        return sesion.cuenta().rol() != Rol.USUARIO;
    }

    private static String estado(boolean activo) {
        return "<span class=\"tag" + (activo ? " good" : "") + "\">" + (activo ? "Activo" : "Inactivo") + "</span>";
    }

    private static String dato(String titulo, String valor) {
        return "<div><dt>" + escapar(titulo) + "</dt><dd>" + escapar(valor) + "</dd></div>";
    }

    private static String acciones(Sesion sesion, String ruta, boolean activo) {
        return acciones(sesion, ruta, activo, admin(sesion), sesion.cuenta().rol() == Rol.ADMINISTRADOR);
    }

    private static String acciones(Sesion sesion, String ruta, boolean activo, boolean edita, boolean estados) {
        if (!edita) return "";
        return "<div class=\"row\">" + enlace(ruta + "/editar", "Editar", false) + (!estados ? "" :
                "<form method=\"post\" action=\"" + ruta + (activo ? "/desactivar" : "/activar") + "\">" +
                        Html.csrf(sesion) + "<button class=\"btn\">" + (activo ? "Desactivar" : "Reactivar") +
                "</button></form>") + "</div>";
    }

    private static String transferencia(Sesion sesion, int empresa, List<Cuenta> candidatas) {
        if (candidatas.isEmpty()) return "";
        StringBuilder opciones = new StringBuilder("<option value=\"\">Elige un Evaluador activo</option>");
        for (Cuenta cuenta : candidatas) {
            opciones.append("<option value=\"").append(cuenta.id()).append("\">").append(escapar(cuenta.nombre()))
                    .append("</option>");
        }
        return "<section class=\"card stack\"><h2>Transferir empresa</h2>" +
                "<p>El dueño está inactivo. Elige al Evaluador que se hará cargo.</p>" +
                "<form method=\"post\" action=\"/empresas/" + empresa + "/transferir\">" + Html.csrf(sesion) +
                "<label class=\"field\">Nuevo dueño<select class=\"input\" name=\"evaluador\" required>" + opciones +
                "</select></label><button class=\"btn\">Transferir empresa</button></form></section>";
    }

    private static String catalogo(String nombre, boolean activo, String acciones) {
        return "<div class=\"catalogo-chip\"><span>" + escapar(nombre) + "</span>" +
                SistemaVisual.estado(activo ? "ACTIVO" : "INACTIVO") + (acciones.isEmpty() ? "" :
                "<details><summary aria-label=\"Opciones de " + escapar(nombre) + "\">⋯</summary>" + acciones +
                "</details>") + "</div>";
    }

    private static String tarjeta(String nombre, String detalle, boolean activo, String acciones) {
        return "<article class=\"card stack\"><div class=\"row between\"><h3>" + escapar(nombre) + "</h3>" +
                estado(activo) + "</div><p class=\"muted\">" + escapar(detalle) + "</p>" + acciones + "</article>";
    }

    private static String seccion(String titulo, String nueva, String etiqueta) {
        return seccion(titulo, nueva, etiqueta, "");
    }

    private static String seccion(String titulo, String nueva, String etiqueta, String consulta) {
        return "<section class=\"stack catalogo-seccion\"><div class=\"pagehead\"><h2>" + escapar(titulo) +
                "</h2><div class=\"row pagehead-acciones\">" + (nueva == null ? "" : enlace(nueva, etiqueta, false)) +
                consulta + "</div></div><div class=\"" +
                (titulo.equals("Áreas") || titulo.equals("Cargos") ? "catalogo-chips" : "grid2") + "\">";
    }

    private static String finSeccion(boolean vacio, String aviso) {
        return (vacio ? "<p class=\"muted\">" + escapar(aviso) + "</p>" : "") + "</div></section>";
    }

    private static List<Opcion> opcionesAreas(List<Area> areas, String actual) {
        List<Opcion> opciones = new ArrayList<>(List.of(new Opcion("", "Sin área asignada")));
        for (Area area : areas)
            if (area.activo() || Integer.toString(area.id()).equals(actual)) {
                opciones.add(
                        new Opcion(Integer.toString(area.id()), area.nombre() + (area.activo() ? "" : " (inactiva)")));
            }
        return opciones;
    }

    private static List<Opcion> opcionesCargos(List<Cargo> cargos, String actual) {
        List<Opcion> opciones = new ArrayList<>(List.of(new Opcion("", "Sin cargo asignado")));
        for (Cargo cargo : cargos)
            if (cargo.activo() || Integer.toString(cargo.id()).equals(actual)) {
                opciones.add(new Opcion(Integer.toString(cargo.id()),
                        cargo.nombre() + (cargo.activo() ? "" : " (inactivo)")));
            }
        return opciones;
    }
}
