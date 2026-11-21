package co.aplomo.servicio;

import co.aplomo.dao.AnalisisDao;
import co.aplomo.dao.ConsultaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.dao.ConsultaDao.Opcion;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.db.Transacciones;
import co.aplomo.seguridad.AccesoDatos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-024: búsqueda, filtros combinables y ordenación en el servidor, después del alcance SQL autorizado.
 */
public final class Consultas {

    /**
     * Resultado de una consulta sin alterar registros ni su historial. RNF-011.
     */
    public record Pagina(SeccionConsulta seccion, List<Fila> filas, Map<String, String> filtros,
                         Map<String, List<Opcion>> catalogos, Map<String, List<String>> riesgos,
                         Fila contextoEvaluacion) {
    }

    /**
     * Detalle de evaluación consultable por el Usuario sin fotos ni rutas de archivos. RNF-011.
     */
    public record Detalle(Fila evaluacion, List<AnalisisDao.Resumen> analisis) {
    }

    private final Transacciones tx;
    private final ConsultaDao dao = new ConsultaDao();

    /**
     * Comparte la fuente transaccional con la aplicación y sus pruebas reversibles. RNF-011.
     */
    public Consultas(Transacciones tx) {
        this.tx = tx;
    }

    /**
     * RF-024: archivo comparte la conexión y el alcance de estas consultas.
     */
    public Archivo archivo() {
        return new Archivo(tx);
    }

    /**
     * Consulta y filtra solo los datos permitidos para la cuenta vigente. RNF-011.
     */
    public Pagina listar(Cuenta actor, SeccionConsulta seccion, Map<String, String> peticion) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            var empresa = AccesoDatos.empresaPermitida(cuenta);
            boolean usuario = cuenta.rol() == Cuenta.Rol.USUARIO;
            var catalogos = new HashMap<>(dao.catalogos(conexion, empresa, !usuario));
            if (cuenta.rol() != Cuenta.Rol.ADMINISTRADOR) catalogos.remove("evaluador");
            else {
                var opciones = new ArrayList<>(catalogos.get("evaluador"));
                opciones.add(new Opcion(-1, 0, 0, "Sin registro"));
                catalogos.put("evaluador", opciones);
            }
            var riesgos = dao.riesgos(conexion);
            if (usuario && peticion.containsKey("registros")) throw new SecurityException(
                    "Acceso denegado. El filtro Registros requiere una cuenta de Administrador.");
            Map<String, String> filtros = validar(seccion, peticion, catalogos, riesgos);
            if (filtros.containsKey("visita") &&
                    new VisitaDao().buscar(conexion, Integer.parseInt(filtros.get("visita")), empresa, usuario,
                            false) == null)
                throw new SecurityException("Acceso denegado. La visita no está disponible para tu cuenta.");
            Fila contextoEvaluacion = null;
            if (!filtros.getOrDefault("evaluacion", "").isEmpty()) contextoEvaluacion = exigir(
                    dao.filas(conexion, SeccionConsulta.EVALUACIONES, empresa, usuario,
                            Integer.parseInt(filtros.get("evaluacion"))));
            if (!usuario && seccion.filtros().contains("registros")) filtros.putIfAbsent("registros", "TODOS");
            var filas = dao.filas(conexion, seccion, empresa, usuario, 0).stream()
                    .filter(elemento -> coincide(elemento, filtros, seccion)).sorted(comparador(filtros)).toList();
            return new Pagina(seccion, filas, Map.copyOf(filtros), catalogos, riesgos, contextoEvaluacion);
        });
    }

    /**
     * RNF-005/009: historial personal; filtros limitados a sus filas, sin permiso de escritura.
     */
    public Pagina propias(Cuenta actor, Map<String, String> parametros) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            if (cuenta.rol() != Cuenta.Rol.EVALUADOR) throw new SecurityException("Acceso denegado.");
            var filas = dao.propias(conexion, cuenta.id());
            Map<String, List<Opcion>> catalogos = catalogosPropios(filas);
            var riesgos = dao.riesgos(conexion);
            var filtros = validar(SeccionConsulta.EVALUACIONES, parametros, catalogos, riesgos);
            List<Fila> visibles = new ArrayList<>();
            for (var elemento : filas) {
                var texto3 = new HashMap<>(elemento.datos());
                boolean asignada;
                try {
                    AccesoDatos.exigirEmpresa(conexion, cuenta, Integer.parseInt(elemento.valor("empresa")));
                    asignada = true;
                } catch (SecurityException ex) {
                    asignada = false;
                }
                texto3.put("acceso_historico", asignada ? "" : "Ya no asignada");
                visibles.add(new Fila(Map.copyOf(texto3)));
            }
            return new Pagina(SeccionConsulta.EVALUACIONES,
                    visibles.stream().filter(elemento2 -> coincide(elemento2, filtros, SeccionConsulta.EVALUACIONES))
                            .sorted(comparador(filtros)).toList(), Map.copyOf(filtros), Map.copyOf(catalogos), riesgos,
                    null);
        });
    }

    /**
     * RF-002/024: los filtros históricos solo describen registros de evaluaciones propias.
     */
    private static Map<String, List<Opcion>> catalogosPropios(List<Fila> filas) {
        Map<String, List<Opcion>> resultado = new HashMap<>();
        for (String clave : List.of("empresa", "sede", "salon", "puesto", "area", "cargo")) {
            Map<Integer, Opcion> opciones = new HashMap<>();
            for (var fila : filas) {
                String id = fila.valor(clave);
                if (id.isEmpty()) continue;
                int empresa = Integer.parseInt(fila.valor("empresa"));
                int sede = Integer.parseInt(fila.valor("sede"));
                String nombre = clave.equals("puesto") ?
                        "Puesto " + fila.valor("numero") : fila.valor(clave + "_nombre");
                String estado = clave.equals("empresa") ? "empresa_activa" : clave.equals("sede") ? "sede_activa" :
                                                                             clave + "_activo";
                if (fila.valor(estado).equals("0"))
                    nombre += clave.equals("empresa") || clave.equals("sede") ? " (inactiva)" : " (inactivo)";
                if (!clave.equals("empresa")) nombre += " · " + fila.valor("empresa_nombre");
                opciones.put(Integer.parseInt(id), new Opcion(Integer.parseInt(id), empresa, sede, nombre));
            }
            resultado.put(clave, opciones.values().stream()
                    .sorted(Comparator.comparing(Opcion::nombre, String.CASE_INSENSITIVE_ORDER)).toList());
        }
        return Map.copyOf(resultado);
    }

    /**
     * Abre únicamente una evaluación visible; una visita En curso o empresa ajena se deniega también por ID. RNF-011.
     */
    public Detalle evaluacion(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            var elemento = exigir(
                    dao.filas(conexion, SeccionConsulta.EVALUACIONES, AccesoDatos.empresaPermitida(cuenta),
                            cuenta.rol() == Cuenta.Rol.USUARIO, id));
            return new Detalle(elemento, new AnalisisDao().listar(conexion, id));
        });
    }

    private static Fila exigir(List<Fila> filas) {
        if (filas.isEmpty())
            throw new SecurityException("Acceso denegado. La evaluación no está disponible para tu cuenta.");
        return filas.get(0);
    }

    private static Map<String, String> validar(SeccionConsulta elemento, Map<String, String> parametros,
            Map<String, List<Opcion>> catalogos, Map<String, List<String>> riesgos) {
        Map<String, String> parametros2 = new LinkedHashMap<>();
        parametros2.put("q", Validacion.opcional(parametros.get("q"), "búsqueda", 120));
        for (String key : catalogos.keySet()) {
            String texto4 = parametros.getOrDefault(key, "");
            if (!texto4.isBlank() && !texto4.equals("0")) {
                int id = Validacion.entero(texto4, key, key.equals("evaluador") ? -1 : 1, Integer.MAX_VALUE);
                if (catalogos.get(key).stream().noneMatch(elemento2 -> elemento2.id() == id))
                    throw new SecurityException("Acceso denegado. Ese filtro no está disponible para tu cuenta.");
                if (elemento.filtros().contains(key)) parametros2.put(key, Integer.toString(id));
            }
        }
        for (String key : List.of("numero", "puntaje", "nivel", "evaluacion", "visita")) {
            String texto5 = parametros.getOrDefault(key, "");
            if (!texto5.isBlank()) {
                int max = key.equals("puntaje") ? 7 : key.equals("nivel") ? 4 : key.equals(
                        "numero") ? 32767 : Integer.MAX_VALUE;
                int cantidad = Validacion.entero(texto5, key, 1, max);
                if (elemento.filtros().contains(key) || key.equals("evaluacion") || key.equals("visita"))
                    parametros2.put(key, Integer.toString(cantidad));
            }
        }
        if (elemento.filtros().contains("color") && !parametros.getOrDefault("color", "").isBlank())
            parametros2.put("color", Validacion.opcion(parametros.get("color"), "semáforo",
                    riesgos.get("color").toArray(String[]::new)));
        for (String key : List.of("desde", "hasta"))
            if (elemento.filtros().contains(key) && !parametros.getOrDefault(key, "").isBlank()) {
                String texto6 = parametros.get(key);
                try {
                    LocalDate dia = LocalDate.parse(texto6);
                    if (dia.getYear() < 1000 || dia.getYear() > 9999) throw new IllegalArgumentException();
                    parametros2.put(key, texto6);
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException(
                            "Revisa la fecha " + (key.equals("desde") ? "inicial" : "final") +
                                    ": escribe una fecha válida.");
                }
            }
        if (parametros2.containsKey("desde") && parametros2.containsKey("hasta") &&
                parametros2.get("desde").compareTo(parametros2.get("hasta")) > 0)
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        String estado = parametros.getOrDefault("estado", elemento == SeccionConsulta.PENDIENTES ? "ABIERTO" : "TODOS");
        if (!estado.isBlank() && !estado.equals("TODOS"))
            parametros2.put("estado", Validacion.opcion(estado, "estado", elemento.estados().toArray(String[]::new)));
        else parametros2.put("estado", "TODOS");
        if (elemento.filtros().contains("registros") && parametros.containsKey("registros"))
            parametros2.put("registros",
                    Validacion.opcion(parametros.get("registros"), "registros", "TODOS", "ACTIVOS", "INACTIVOS"));
        List<String> ordenes = new ArrayList<>(
                elemento.columnas().stream().map(SeccionConsulta.Columna::clave).toList());
        if (elemento == SeccionConsulta.EVALUACIONES) ordenes.add("evaluador_nombre");
        if (elemento == SeccionConsulta.PENDIENTES) ordenes.addAll(List.of("prioridad", "fecha", "fecha_limite"));
        String defecto = elemento == SeccionConsulta.PENDIENTES ? "prioridad" : ordenes.contains(
                "fecha") ? "fecha" : ordenes.contains("nombre") ? "nombre" : "numero";
        String orden = parametros.getOrDefault("orden", defecto);
        if (orden.equals("limite")) orden = "fecha_limite";
        parametros2.put("orden", Validacion.opcion(orden, "orden", ordenes.toArray(String[]::new)));
        parametros2.put("direccion",
                Validacion.opcion(parametros.getOrDefault("direccion", orden.equals("fecha") ? "desc" : "asc"),
                        "dirección", "asc", "desc"));
        return parametros2;
    }

    private static boolean coincide(Fila elemento, Map<String, String> parametros, SeccionConsulta elemento2) {
        for (String k : elemento2.filtros()) {
            String texto3 = parametros.getOrDefault(k, "");
            if (texto3.isEmpty() || texto3.equals("TODOS")) continue;
            if (k.equals("desde") || k.equals("hasta")) {
                String fecha = elemento.valor("fecha");
                if (fecha.length() < 10) return false;
                String dia = fecha.substring(0, 10);
                if (k.equals("desde") && dia.compareTo(texto3) < 0 || k.equals("hasta") && dia.compareTo(texto3) > 0)
                    return false;
            } else if (!elemento.valor(k).equals(k.equals("evaluador") && texto3.equals("-1") ? "0" : texto3))
                return false;
        }
        if (parametros.containsKey("evaluacion") && !elemento.valor("evaluacion").equals(parametros.get("evaluacion")))
            return false;
        if (parametros.containsKey("visita") && !elemento.valor("visita").equals(parametros.get("visita")))
            return false;
        String texto4 = parametros.getOrDefault("q", "");
        if (texto4.isEmpty()) return true;
        String texto = String.join(" ", elemento.datos().values()).replace('_', ' ');
        if (elemento.valor("origen").equals("SISTEMA")) texto += " Sugerencia del sistema";
        return Validacion.nombre(texto).contains(Validacion.nombre(texto4));
    }

    private static Comparator<Fila> comparador(Map<String, String> parametros) {
        String key = parametros.get("orden");
        boolean desc = parametros.get("direccion").equals("desc");
        return (elemento, elemento2) -> {
            String texto3 = orden(elemento, key), texto4 = orden(elemento2, key);
            int cantidad;
            if (texto3.isEmpty() || texto4.isEmpty()) cantidad = texto3.isEmpty() ? (texto4.isEmpty() ? 0 : 1) : -1;
            else {
                if (List.of("numero", "puntaje", "nivel", "piso", "capacidad", "calificacion").contains(key))
                    cantidad = new BigDecimal(texto3).compareTo(new BigDecimal(texto4));
                else cantidad = Validacion.nombre(texto3).compareTo(Validacion.nombre(texto4));
                if (desc) cantidad = -cantidad;
            }
            return cantidad == 0 ? (
                    key.equals("fecha") && desc ? Integer.compare(elemento2.id(), elemento.id()) : Integer.compare(
                            elemento.id(), elemento2.id())) : cantidad;
        };
    }

    private static String orden(Fila elemento, String key) {
        return switch (key) {
            case "ubicacion" -> elemento.valor("empresa_nombre") + " " + elemento.valor("sede_nombre") + " " +
                    elemento.valor("salon_nombre");
            case "asignacion" -> elemento.valor("area_nombre") + " " + elemento.valor("cargo_nombre");
            case "seguimiento", "prioridad" -> switch (elemento.valor("prioridad")) {
                case "ALTA" -> "1";
                case "MEDIA" -> "2";
                case "BAJA" -> "3";
                default -> "";
            };
            default -> elemento.valor(key);
        };
    }
}
