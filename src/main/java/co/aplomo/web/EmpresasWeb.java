package co.aplomo.web;

import co.aplomo.modelo.Area;
import co.aplomo.modelo.Cargo;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * RF-004 a RF-010, RF-028: navegación de la jerarquía y formularios protegidos por el Router.
 */
public final class EmpresasWeb {
    private static final Pattern REGISTRO = Pattern.compile(
            "/(empresas|sedes|salones|areas|cargos|puestos)/([1-9][0-9]*)(?:/(editar|acti" + "var|desactivar))?");
    private static final Pattern NUEVO = Pattern.compile(
            "/(empresas|sedes|salones)/([1-9][0-9]*)/(sedes|areas|cargos|salones|puestos)/nuevo");
    private final Empresas empresas;

    /**
     * Recibe el servicio de configuración para compartirlo con las pruebas de HTTP. RNF-011.
     */
    public EmpresasWeb(Empresas empresas) {
        this.empresas = empresas;
    }

    /**
     * Servicio de empresas con autorización renovada (RF-002).
     */
    public Empresas servicio() {
        return empresas;
    }

    /**
     * Atiende consultas y cambios, sin aceptar IDs ni relaciones padre desde campos editables. RNF-011.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> datos) throws SQLException, IOException {
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        if (ruta.equals("/empresas") && !post) {
            Router.enviar(intercambio, 200,
                    PaginasEmpresas.lista(sesion, empresas.listar(sesion.cuenta()), aviso(intercambio)));
            return;
        }
        if (ruta.equals("/empresas") || ruta.equals("/empresas/nueva")) {
            Permisos.administrador(sesion.cuenta());
            if (post && ruta.endsWith("nueva")) {
                noDisponible(intercambio, sesion);
                return;
            }
            atenderFormulario(intercambio, sesion, preparar(sesion, "empresas", 0, 0), datos, post);
            return;
        }
        var nuevo = NUEVO.matcher(ruta);
        if (nuevo.matches()) {
            Permisos.trabajo(sesion.cuenta());
            String padre = nuevo.group(1), tipo = nuevo.group(3);
            boolean relacion = padre.equals("empresas") && Set.of("sedes", "areas", "cargos").contains(tipo) ||
                    padre.equals("sedes") && tipo.equals("salones") ||
                    padre.equals("salones") && tipo.equals("puestos");
            if (!relacion) {
                noDisponible(intercambio, sesion);
                return;
            }
            atenderFormulario(intercambio, sesion, preparar(sesion, tipo, 0, id(nuevo.group(2))), datos, post);
            return;
        }
        var registro = REGISTRO.matcher(ruta);
        if (!registro.matches()) {
            noDisponible(intercambio, sesion);
            return;
        }
        String tipo = registro.group(1), accion = registro.group(3);
        int id = id(registro.group(2));
        if (accion == null && !post) {
            detalle(intercambio, sesion, tipo, id);
            return;
        }
        if (accion == null || !post && !accion.equals("editar")) {
            noDisponible(intercambio, sesion);
            return;
        }
        if (accion.equals("editar")) Permisos.trabajo(sesion.cuenta());
        else Permisos.administrador(sesion.cuenta());
        Contexto contexto = preparar(sesion, tipo, id, 0);
        if (accion.equals("editar")) {
            atenderFormulario(intercambio, sesion, contexto, datos, post);
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
        Router.redirigir(intercambio, contexto.volver() + "?aviso=" +
                codificar(activo ? "Registro reactivado." : "Registro desactivado. Su historial se conserva."));
    }

    private void atenderFormulario(HttpExchange intercambio, Sesion sesion, Contexto contexto,
            Map<String, String> datos, boolean post) throws SQLException, IOException {
        if (!post) {
            Router.enviar(intercambio, 200, PaginasEmpresas.formulario(sesion, contexto, Map.of(), null));
            return;
        }
        Empresas.Guardado guardado;
        try {
            guardado = guardar(sesion, contexto, datos);
        } catch (IllegalArgumentException error) {
            Router.enviar(intercambio, 400, PaginasEmpresas.formulario(sesion, contexto, datos, error.getMessage()));
            return;
        } catch (SQLException error2) {
            Router.enviar(intercambio, 503, PaginasEmpresas.formulario(sesion, contexto, datos,
                    "No pudimos guardar. Tus datos siguen en el formulario; inténtalo de nuevo."));
            return;
        }
        String destino = Set.of("empresas", "sedes", "salones").contains(contexto.tipo()) ?
                "/" + contexto.tipo() + "/" + guardado.id() : contexto.volver();
        Router.redirigir(intercambio,
                destino + "?aviso=" + codificar(guardado.aviso() == null ? "Datos guardados." : guardado.aviso()));
    }

    private Empresas.Guardado guardar(Sesion sesion2, Contexto empresa,
            Map<String, String> parametros) throws SQLException {
        return switch (empresa.tipo()) {
            case "empresas" -> empresas.guardar(sesion2.cuenta(),
                    new Empresa(empresa.id(), 0, parametros.get("nombre"), parametros.get("nit"),
                            parametros.get("descripcion"), parametros.get("telefono"), parametros.get("correo"),
                            parametros.get("persona_contacto"), true));
            case "sedes" -> empresas.guardar(sesion2.cuenta(),
                    new Sede(empresa.id(), empresa.padre(), parametros.get("nombre"), parametros.get("direccion"),
                            parametros.get("ciudad"), parametros.get("telefono"), parametros.get("horario"),
                            parametros.get("descripcion"), true));
            case "salones" -> empresas.guardar(sesion2.cuenta(),
                    new Salon(empresa.id(), empresa.padre(), parametros.get("nombre"), parametros.get("descripcion"),
                            Validacion.entero(parametros.get("piso"), "piso", -128, 127),
                            Validacion.entero(parametros.get("capacidad_puestos"), "capacidad de puestos", 1, 32767),
                            true));
            case "areas" -> empresas.guardar(sesion2.cuenta(),
                    new Area(empresa.id(), empresa.padre(), parametros.get("nombre"), true));
            case "cargos" -> empresas.guardar(sesion2.cuenta(),
                    new Cargo(empresa.id(), empresa.padre(), parametros.get("nombre"), parametros.get("tipo"), true));
            case "puestos" -> empresas.guardar(sesion2.cuenta(),
                    new Puesto(empresa.id(), empresa.padre(), opcional(parametros.get("id_area")),
                            opcional(parametros.get("id_cargo")),
                            Validacion.entero(parametros.get("numero"), "número del puesto", 1, 32767),
                            parametros.get("descripcion"), parametros.get("tipo"), parametros.get("ubicacion"), null,
                            true));
            default -> throw new IllegalArgumentException("Formulario no disponible.");
        };
    }

    private Contexto preparar(Sesion sesion, String tipo, int id, int padre) throws SQLException {
        Map<String, String> datos = new LinkedHashMap<>();
        String volver;
        List<Area> areas = List.of();
        List<Cargo> cargos = List.of();
        switch (tipo) {
            case "empresas" -> {
                volver = id == 0 ? "/empresas" : "/empresas/" + id;
                if (id > 0) {
                    Empresa empresa = empresas.empresa(sesion.cuenta(), id);
                    datos = datos("nombre", empresa.nombre(), "nit", empresa.nit(), "descripcion",
                            empresa.descripcion(), "telefono", empresa.telefono(), "correo", empresa.correo(),
                            "persona_contacto", empresa.contacto());
                }
            }
            case "sedes" -> {
                if (id > 0) {
                    Sede elemento = empresas.sede(sesion.cuenta(), id);
                    padre = elemento.empresa();
                    datos = datos("nombre", elemento.nombre(), "direccion", elemento.direccion(), "ciudad",
                            elemento.ciudad(), "telefono", elemento.telefono(), "horario", elemento.horario(),
                            "descripcion", elemento.descripcion());
                }
                empresas.empresa(sesion.cuenta(), padre);
                volver = "/empresas/" + padre;
            }
            case "salones" -> {
                if (id > 0) {
                    Salon elemento2 = empresas.salon(sesion.cuenta(), id);
                    padre = elemento2.sede();
                    datos = datos("nombre", elemento2.nombre(), "descripcion", elemento2.descripcion(), "piso",
                            elemento2.piso(), "capacidad_puestos", elemento2.capacidad());
                }
                empresas.sede(sesion.cuenta(), padre);
                volver = "/sedes/" + padre;
            }
            case "areas" -> {
                if (id > 0) {
                    Area elemento3 = empresas.area(sesion.cuenta(), id);
                    padre = elemento3.empresa();
                    datos = datos("nombre", elemento3.nombre());
                }
                empresas.empresa(sesion.cuenta(), padre);
                volver = "/empresas/" + padre;
            }
            case "cargos" -> {
                if (id > 0) {
                    Cargo elemento4 = empresas.cargo(sesion.cuenta(), id);
                    padre = elemento4.empresa();
                    datos = datos("nombre", elemento4.nombre(), "tipo", elemento4.tipo());
                }
                empresas.empresa(sesion.cuenta(), padre);
                volver = "/empresas/" + padre;
            }
            case "puestos" -> {
                if (id > 0) {
                    Puesto elemento5 = empresas.puesto(sesion.cuenta(), id);
                    padre = elemento5.salon();
                    datos = datos("numero", elemento5.numero(), "descripcion", elemento5.descripcion(), "tipo",
                            elemento5.tipo(), "ubicacion", elemento5.ubicacion(), "id_area", elemento5.area(),
                            "id_cargo", elemento5.cargo());
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

    private void detalle(HttpExchange intercambio, Sesion sesion2, String tipo,
            int id) throws SQLException, IOException {
        switch (tipo) {
            case "empresas" -> Router.enviar(intercambio, 200,
                    PaginasEmpresas.empresa(sesion2, empresas.empresa(sesion2.cuenta(), id),
                            empresas.sedes(sesion2.cuenta(), id), empresas.areas(sesion2.cuenta(), id),
                            empresas.cargos(sesion2.cuenta(), id), aviso(intercambio),
                            sesion2.cuenta().rol() == Cuenta.Rol.ADMINISTRADOR ? empresas.vinculadas(sesion2.cuenta(),
                                    id) : List.of()));
            case "sedes" -> {
                Sede sede = empresas.sede(sesion2.cuenta(), id);
                Router.enviar(intercambio, 200,
                        PaginasEmpresas.sede(sesion2, sede, empresas.empresa(sesion2.cuenta(), sede.empresa()),
                                empresas.salones(sesion2.cuenta(), id), aviso(intercambio)));
            }
            case "salones" -> {
                Salon salon = empresas.salon(sesion2.cuenta(), id);
                Sede sede = empresas.sede(sesion2.cuenta(), salon.sede());
                Router.enviar(intercambio, 200,
                        PaginasEmpresas.salon(sesion2, salon, sede, empresas.empresa(sesion2.cuenta(), sede.empresa()),
                                empresas.puestos(sesion2.cuenta(), id), aviso(intercambio)));
            }
            case "areas" -> Router.redirigir(intercambio, "/empresas/" + empresas.area(sesion2.cuenta(), id).empresa());
            case "cargos" ->
                    Router.redirigir(intercambio, "/empresas/" + empresas.cargo(sesion2.cuenta(), id).empresa());
            case "puestos" -> {

                // RF-002/024: el Usuario conserva la redirección autorizada anterior; no recibe el historial
                // administrativo.
                Puesto puesto = empresas.puesto(sesion2.cuenta(), id);
                if (sesion2.cuenta().rol() != Cuenta.Rol.USUARIO) Router.enviar(intercambio, 200,
                        PaginasEmpresas.historialPuesto(sesion2, empresas.historialPuesto(sesion2.cuenta(), id)));
                else Router.redirigir(intercambio, "/salones/" + puesto.salon());
            }
            default -> noDisponible(intercambio, sesion2);
        }
    }

    private static Map<String, String> datos(Object... pares) {
        Map<String, String> datos = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2)
            datos.put((String) pares[i], pares[i + 1] == null ? "" : pares[i + 1].toString());
        return datos;
    }

    private static int id(String texto) {
        return Validacion.entero(texto, "identificador", 1, Integer.MAX_VALUE);
    }

    private static Integer opcional(String texto) {
        return texto == null || texto.isBlank() ? null : id(texto);
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static String aviso(HttpExchange intercambio) {
        return Router.consulta(intercambio).get("aviso");
    }

    private static void noDisponible(HttpExchange intercambio, Sesion sesion2) throws IOException {
        Router.enviar(intercambio, 404,
                Html.pagina("Página no disponible", PaginasEmpresas.enlace("/empresas", "Volver a empresas", false),
                        sesion2));
    }
}
