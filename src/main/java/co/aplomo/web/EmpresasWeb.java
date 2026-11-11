package co.aplomo.web;

import co.aplomo.modelo.*;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Validacion;
import co.aplomo.web.PaginasEmpresas.Contexto;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/** RF-004 a RF-010, RF-028: navegación de la jerarquía y formularios protegidos por el Router. */
public final class EmpresasWeb {
    private static final Pattern REGISTRO = Pattern.compile("/(empresas|sedes|salones|areas|cargos|puestos)/([1-9][0-9]*)(?:/(editar|activar|desactivar))?");
    private static final Pattern NUEVO = Pattern.compile("/(empresas|sedes|salones)/([1-9][0-9]*)/(sedes|areas|cargos|salones|puestos)/nuevo");
    private final Empresas empresas;

    /** Recibe el servicio de configuración para compartirlo con las pruebas de HTTP. */
    public EmpresasWeb(Empresas empresas) { this.empresas = empresas; }

    /** Atiende consultas y cambios, sin aceptar IDs ni relaciones padre desde campos editables. */
    public void atender(HttpExchange x, Sesion sesion, Map<String,String> datos) throws SQLException, IOException {
        String ruta = x.getRequestURI().getPath();
        boolean post = x.getRequestMethod().equals("POST");
        if (ruta.equals("/empresas") && !post) {
            Router.enviar(x, 200, PaginasEmpresas.lista(sesion, empresas.listar(sesion.cuenta()), aviso(x)));
            return;
        }
        if (ruta.equals("/empresas") || ruta.equals("/empresas/nueva")) {
            Permisos.administrador(sesion.cuenta());
            if (post && ruta.endsWith("nueva")) { noDisponible(x, sesion); return; }
            atenderFormulario(x, sesion, preparar(sesion, "empresas", 0, 0), datos, post);
            return;
        }
        var nuevo = NUEVO.matcher(ruta);
        if (nuevo.matches()) {
            Permisos.administrador(sesion.cuenta());
            String padre = nuevo.group(1), tipo = nuevo.group(3);
            boolean relacion = padre.equals("empresas") && Set.of("sedes", "areas", "cargos").contains(tipo)
                    || padre.equals("sedes") && tipo.equals("salones")
                    || padre.equals("salones") && tipo.equals("puestos");
            if (!relacion) { noDisponible(x, sesion); return; }
            atenderFormulario(x, sesion, preparar(sesion, tipo, 0, id(nuevo.group(2))), datos, post);
            return;
        }
        var registro = REGISTRO.matcher(ruta);
        if (!registro.matches()) { noDisponible(x, sesion); return; }
        String tipo = registro.group(1), accion = registro.group(3);
        int id = id(registro.group(2));
        if (accion == null && !post) { detalle(x, sesion, tipo, id); return; }
        if (accion == null || !post && !accion.equals("editar")) { noDisponible(x, sesion); return; }
        Permisos.administrador(sesion.cuenta());
        Contexto contexto = preparar(sesion, tipo, id, 0);
        if (accion.equals("editar")) {
            atenderFormulario(x, sesion, contexto, datos, post);
            return;
        }
        boolean activo = accion.equals("activar");
        switch (tipo) {
            case "empresas" -> empresas.estadoEmpresa(sesion.cuenta(), id, activo);
            case "sedes" -> empresas.estadoSede(sesion.cuenta(), id, activo);
            case "salones" -> empresas.estadoSalon(sesion.cuenta(), id, activo);
            case "areas" -> empresas.estadoArea(sesion.cuenta(), id, activo);
            case "cargos" -> empresas.estadoCargo(sesion.cuenta(), id, activo);
            case "puestos" -> empresas.estadoPuesto(sesion.cuenta(), id, activo);
            default -> throw new IllegalArgumentException("Registro no disponible.");
        }
        Router.redirigir(x, contexto.volver() + "?aviso=" + codificar(activo ? "Registro reactivado." : "Registro desactivado. Su historial se conserva."));
    }

    private void atenderFormulario(HttpExchange x, Sesion sesion, Contexto contexto,
                                   Map<String,String> datos, boolean post) throws SQLException, IOException {
        if (!post) {
            Router.enviar(x, 200, PaginasEmpresas.formulario(sesion, contexto, Map.of(), null));
            return;
        }
        Empresas.Guardado guardado;
        try { guardado = guardar(sesion, contexto, datos); }
        catch (IllegalArgumentException e) {
            Router.enviar(x, 400, PaginasEmpresas.formulario(sesion, contexto, datos, e.getMessage()));
            return;
        } catch (SQLException e) {
            Router.enviar(x, 503, PaginasEmpresas.formulario(sesion, contexto, datos,
                    "No pudimos guardar. Tus datos siguen en el formulario; inténtalo de nuevo."));
            return;
        }
        String destino = Set.of("empresas", "sedes", "salones").contains(contexto.tipo())
                ? "/" + contexto.tipo() + "/" + guardado.id() : contexto.volver();
        Router.redirigir(x, destino + "?aviso=" + codificar(guardado.aviso() == null ? "Datos guardados." : guardado.aviso()));
    }

    private Empresas.Guardado guardar(Sesion s, Contexto c, Map<String,String> f) throws SQLException {
        return switch (c.tipo()) {
            case "empresas" -> empresas.guardar(s.cuenta(), new Empresa(c.id(), 0, f.get("nombre"), f.get("nit"),
                    f.get("descripcion"), f.get("telefono"), f.get("correo"), f.get("persona_contacto"), true));
            case "sedes" -> empresas.guardar(s.cuenta(), new Sede(c.id(), c.padre(), f.get("nombre"), f.get("direccion"),
                    f.get("ciudad"), f.get("telefono"), f.get("horario"), f.get("descripcion"), true));
            case "salones" -> empresas.guardar(s.cuenta(), new Salon(c.id(), c.padre(), f.get("nombre"), f.get("descripcion"),
                    Validacion.entero(f.get("piso"), "piso", -128, 127),
                    Validacion.entero(f.get("capacidad_puestos"), "capacidad de puestos", 1, 32767), true));
            case "areas" -> empresas.guardar(s.cuenta(), new Area(c.id(), c.padre(), f.get("nombre"), true));
            case "cargos" -> empresas.guardar(s.cuenta(), new Cargo(c.id(), c.padre(), f.get("nombre"), f.get("tipo"), true));
            case "puestos" -> empresas.guardar(s.cuenta(), new Puesto(c.id(), c.padre(), opcional(f.get("id_area")),
                    opcional(f.get("id_cargo")), Validacion.entero(f.get("numero"), "número del puesto", 1, 32767),
                    f.get("descripcion"), f.get("tipo"), f.get("ubicacion"), null, true));
            default -> throw new IllegalArgumentException("Formulario no disponible.");
        };
    }

    private Contexto preparar(Sesion sesion, String tipo, int id, int padre) throws SQLException {
        Map<String,String> datos = new LinkedHashMap<>();
        String volver;
        List<Area> areas = List.of();
        List<Cargo> cargos = List.of();
        switch (tipo) {
            case "empresas" -> {
                volver = id == 0 ? "/empresas" : "/empresas/" + id;
                if (id > 0) {
                    Empresa e = empresas.empresa(sesion.cuenta(), id);
                    datos = datos("nombre", e.nombre(), "nit", e.nit(), "descripcion", e.descripcion(), "telefono", e.telefono(),
                            "correo", e.correo(), "persona_contacto", e.contacto());
                }
            }
            case "sedes" -> {
                if (id > 0) {
                    Sede e = empresas.sede(sesion.cuenta(), id);
                    padre = e.empresa();
                    datos = datos("nombre", e.nombre(), "direccion", e.direccion(), "ciudad", e.ciudad(),
                            "telefono", e.telefono(), "horario", e.horario(), "descripcion", e.descripcion());
                }
                empresas.empresa(sesion.cuenta(), padre);
                volver = "/empresas/" + padre;
            }
            case "salones" -> {
                if (id > 0) {
                    Salon e = empresas.salon(sesion.cuenta(), id);
                    padre = e.sede();
                    datos = datos("nombre", e.nombre(), "descripcion", e.descripcion(), "piso", e.piso(), "capacidad_puestos", e.capacidad());
                }
                empresas.sede(sesion.cuenta(), padre);
                volver = "/sedes/" + padre;
            }
            case "areas" -> {
                if (id > 0) {
                    Area e = empresas.area(sesion.cuenta(), id);
                    padre = e.empresa(); datos = datos("nombre", e.nombre());
                }
                empresas.empresa(sesion.cuenta(), padre);
                volver = "/empresas/" + padre;
            }
            case "cargos" -> {
                if (id > 0) {
                    Cargo e = empresas.cargo(sesion.cuenta(), id);
                    padre = e.empresa(); datos = datos("nombre", e.nombre(), "tipo", e.tipo());
                }
                empresas.empresa(sesion.cuenta(), padre);
                volver = "/empresas/" + padre;
            }
            case "puestos" -> {
                if (id > 0) {
                    Puesto e = empresas.puesto(sesion.cuenta(), id);
                    padre = e.salon();
                    datos = datos("numero", e.numero(), "descripcion", e.descripcion(), "tipo", e.tipo(), "ubicacion", e.ubicacion(),
                            "id_area", e.area(), "id_cargo", e.cargo());
                }
                Salon salon = empresas.salon(sesion.cuenta(), padre);
                Sede sede = empresas.sede(sesion.cuenta(), salon.sede());
                areas = empresas.areas(sesion.cuenta(), sede.empresa());
                cargos = empresas.cargos(sesion.cuenta(), sede.empresa());
                volver = "/salones/" + padre;
            }
            default -> throw new IllegalArgumentException("Formulario no disponible.");
        }
        return new Contexto(tipo, id, padre, volver, datos, areas, cargos);
    }

    private void detalle(HttpExchange x, Sesion s, String tipo, int id) throws SQLException, IOException {
        switch (tipo) {
            case "empresas" -> Router.enviar(x, 200, PaginasEmpresas.empresa(s, empresas.empresa(s.cuenta(), id),
                    empresas.sedes(s.cuenta(), id), empresas.areas(s.cuenta(), id), empresas.cargos(s.cuenta(), id), aviso(x)));
            case "sedes" -> {
                Sede sede = empresas.sede(s.cuenta(), id);
                Router.enviar(x, 200, PaginasEmpresas.sede(s, sede, empresas.empresa(s.cuenta(), sede.empresa()),
                        empresas.salones(s.cuenta(), id), aviso(x)));
            }
            case "salones" -> {
                Salon salon = empresas.salon(s.cuenta(), id);
                Sede sede = empresas.sede(s.cuenta(), salon.sede());
                Router.enviar(x, 200, PaginasEmpresas.salon(s, salon, sede, empresas.empresa(s.cuenta(), sede.empresa()),
                        empresas.puestos(s.cuenta(), id), aviso(x)));
            }
            case "areas" -> Router.redirigir(x, "/empresas/" + empresas.area(s.cuenta(), id).empresa());
            case "cargos" -> Router.redirigir(x, "/empresas/" + empresas.cargo(s.cuenta(), id).empresa());
            case "puestos" -> Router.redirigir(x, "/salones/" + empresas.puesto(s.cuenta(), id).salon());
            default -> noDisponible(x, s);
        }
    }

    private static Map<String,String> datos(Object... pares) {
        Map<String,String> datos = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) datos.put((String) pares[i], pares[i + 1] == null ? "" : pares[i + 1].toString());
        return datos;
    }

    private static int id(String texto) { return Validacion.entero(texto, "identificador", 1, Integer.MAX_VALUE); }
    private static Integer opcional(String texto) { return texto == null || texto.isBlank() ? null : id(texto); }
    private static String codificar(String texto) { return URLEncoder.encode(texto, StandardCharsets.UTF_8); }
    private static String aviso(HttpExchange x) { return Router.consulta(x).get("aviso"); }
    private static void noDisponible(HttpExchange x, Sesion s) throws IOException {
        Router.enviar(x, 404, Html.pagina("Página no disponible", PaginasEmpresas.enlace("/empresas", "Volver a empresas", false), s));
    }
}
