package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.PendienteDao;
import co.aplomo.dao.SugerenciaDao;
import co.aplomo.dao.SugerenciaDao.Contexto;
import co.aplomo.dao.SugerenciaDao.Ficha;
import co.aplomo.dao.SugerenciaDao.Regla;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Sugerencia;
import co.aplomo.rula.MotorRula;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RF-023a/b: propuestas de MySQL y gestión autorizada, sin duplicar reglas ni borrar historia.
 */
public final class Sugerencias {

    /**
     * (propuesta) Andy, 11 nov 2026: correspondencia fija; nunca se adivina el tipo a partir del texto.
     */
    public static final Map<String, String> TIPOS_POR_CONDICION = Map.of("SIEMPRE", "POSTURA", "escritorio_muy_alto",
            "MOBILIARIO", "pantalla_muy_alta", "MOBILIARIO", "pies_colgando", "MOBILIARIO", "tiene_apoyapies",
            "MOBILIARIO", "silla_con_apoyabrazos", "MOBILIARIO", "silla_regulable", "MOBILIARIO");
    private final Transacciones tx;
    private final Clock reloj;
    private final BitacoraDao bitacora;
    private final SugerenciaDao dao = new SugerenciaDao();

    /**
     * Comparte transacciones, auditoría y hora de Colombia (RNF-005/011).
     */
    public Sugerencias(Transacciones tx, Clock reloj, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.bitacora = bitacora;
    }

    /**
     * Lista únicamente sugerencias activas autorizadas (RF-002).
     */
    public List<Ficha> listar(Cuenta actor, int evaluacion) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            if (evaluacion > 0) visible(conexion, cuenta, evaluacion, false);
            return dao.listar(conexion, AccesoDatos.empresaPermitida(cuenta), cuenta.rol() == Cuenta.Rol.USUARIO,
                    evaluacion);
        });
    }

    /**
     * Consulta una evaluación con análisis, sin entregar fotos al Usuario. RNF-011.
     */
    public Contexto contexto(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> visible(conexion, AccesoDatos.vigente(conexion, actor), id, false));
    }

    /**
     * Detalla sugerencias quitadas solo para Administrador; la empresa conserva consulta de activas. RNF-011.
     */
    public Ficha consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            var sugerencia = exigir(dao.buscar(conexion, id));
            var sugerencia2 = visible(conexion, cuenta, sugerencia.evaluacion(), false);
            if (!sugerencia.activo() && cuenta.rol() == Cuenta.Rol.USUARIO)
                throw new SecurityException("Acceso denegado. La sugerencia no está disponible.");
            return new Ficha(sugerencia, sugerencia2);
        });
    }

    /**
     * Muestra fecha, responsable y cambios sin perder el historial de una sugerencia quitada. RNF-011.
     */
    public List<PendienteDao.Cambio> historial(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            var sugerencia = exigir(dao.buscar(conexion, id));
            visible(conexion, cuenta, sugerencia.evaluacion(), false);
            if (!sugerencia.activo() && cuenta.rol() == Cuenta.Rol.USUARIO)
                throw new SecurityException("Acceso denegado.");
            return Sql.listar(conexion, "SELECT b.*,q.nombre responsable FROM bitacora b JOIN cuenta q USING(id_cuent" +
                            "a) WHERE b.tabla='sugerencia' AND b.id_registro=? ORDER BY b.id_bitacora DES" + "C",
                    fila -> new PendienteDao.Cambio(fila.getTimestamp("fecha_hora").toLocalDateTime(),
                            fila.getString("responsable"), fila.getString("accion"), fila.getString("estado_anterior"),
                            fila.getString("estado_nuevo"), fila.getString("detalle")), id);
        });
    }

    /**
     * Crea o edita título, descripción, tipo, prioridad y fecha; no acepta origen ni regla desde el navegador.
     * RNF-011.
     */
    public int guardar(Cuenta actor, int evaluacion, int id, String titulo, String descripcion, String tipo,
            String prioridad, String fecha) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = admin(conexion, actor);
            visible(conexion, cuenta, evaluacion, true);
            var anterior = id == 0 ? null : editable(conexion, id, evaluacion);
            String texto2 = Validacion.texto(titulo, "título", 120), texto3 = Validacion.notas(descripcion);
            if (texto3.isEmpty()) throw new IllegalArgumentException("Completa la descripción de la sugerencia.");
            String tip = Validacion.opcion(tipo, "tipo", "MOBILIARIO", "POSTURA", "DESCANSOS", "CARGA",
                    "OTRA"), texto4 = Validacion.opcion(prioridad, "prioridad", "ALTA", "MEDIA", "BAJA");
            var nueva = new Sugerencia(id, evaluacion, anterior == null ? null : anterior.regla(), texto2, texto3, tip,
                    texto4, anterior == null ? "PENDIENTE" : anterior.estado(), fecha(fecha),
                    anterior == null ? "ADMINISTRADOR" : anterior.origen(), true);
            int guardado;
            if (id == 0) {
                guardado = dao.crear(conexion, nueva);
                auditar(conexion, cuenta, guardado, "CREAR", null, "PENDIENTE",
                        "Sugerencia escrita por el Administrador.");
            } else {
                guardado = id;
                if (!nueva.equals(anterior)) {
                    dao.editar(conexion, nueva);
                    auditar(conexion, cuenta, id, "EDITAR", null, null,
                            "Se editaron los datos de la sugerencia; su origen y estado se conservan.");
                }
            }
            return guardado;
        });
    }

    /**
     * Cambia entre los tres estados aprobados; repetir el mismo estado no duplica auditoría. RNF-011.
     */
    public void estado(Cuenta actor, int id, String estado) throws SQLException {
        tx.ejecutar(conexion -> {
            var cuenta = admin(conexion, actor);
            var primera = exigir(dao.buscar(conexion, id));
            visible(conexion, cuenta, primera.evaluacion(), true);
            var sugerencia = editable(conexion, id, primera.evaluacion());
            String texto2 = Validacion.opcion(estado, "estado", "PENDIENTE", "EN_PROCESO", "HECHA");
            if (!sugerencia.estado().equals(texto2)) {
                dao.estado(conexion, id, texto2);
                auditar(conexion, cuenta, id, "CAMBIAR_ESTADO", sugerencia.estado(), texto2,
                        "El Administrador actualizó el avance de la mejora.");
            }
            return null;
        });
    }

    /**
     * Quita sin borrar; Amarillo/Rojo conserva al menos una sugerencia activa (Andy, 11 nov 2026). RNF-011.
     */
    public void quitar(Cuenta actor, int id) throws SQLException {
        tx.ejecutar(conexion -> {
            var cuenta = admin(conexion, actor);
            var primera = exigir(dao.buscar(conexion, id));
            var sugerencia = visible(conexion, cuenta, primera.evaluacion(), true);
            var sugerencia2 = dao.buscarActual(conexion, id);
            if (!sugerencia2.activo()) return null;
            if (!"VERDE".equals(sugerencia.color()) &&
                    dao.actuales(conexion, sugerencia.evaluacion()).stream().filter(Sugerencia::activo).count() <= 1)
                throw new IllegalArgumentException(
                        "Este resultado requiere al menos una sugerencia. Agrega otra antes de quitar esta.");
            dao.quitar(conexion, id);
            auditar(conexion, cuenta, id, "DESACTIVAR", sugerencia2.estado(), sugerencia2.estado(),
                    "Sugerencia quitada de la lista; su historial se conserva.");
            return null;
        });
    }

    /**
     * Indica que ya se puede registrar una reserva de Reevaluación para este puesto (CU5-26). RNF-011.
     */
    public boolean todasHechas(Cuenta actor, int evaluacion) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            var sugerencia = visible(conexion, cuenta, evaluacion, false);
            return dao.todasHechas(conexion, sugerencia.puesto());
        });
    }

    /**
     * RF-023a: propuestas antes de guardar, sin insertar filas y con el cálculo ya autorizado.
     */
    public List<Regla> previsualizar(Cuenta actor, MotorRula.Calculo calculo,
            Map<String, Boolean> condiciones) throws SQLException {
        return tx.ejecutar(conexion -> {
            admin(conexion, actor);
            return aplicables(calculo, condiciones, dao.reglas(conexion));
        });
    }

    private static List<Regla> aplicables(MotorRula.Calculo calculo, Map<String, Boolean> condiciones,
            List<Regla> reglas) {
        return reglas.stream().filter(sugerencia -> TIPOS_POR_CONDICION.containsKey(sugerencia.condicion()) &&
                        (sugerencia.condicion().equals("SIEMPRE") ||
                                Boolean.TRUE.equals(condiciones.get(sugerencia.condicion()))))
                .filter(sugerencia2 -> calculo.mediciones().stream().anyMatch(
                        elemento -> elemento.segmento().name().equals(sugerencia2.segmento()) &&
                                elemento.puntos() >= sugerencia2.minimo()))
                .filter(sugerencia3 -> !sugerencia3.texto().isBlank()).toList();
    }

    /**
     * RF-023a: aplica reglas configuradas al resultado del servidor, conservando las propuestas previas.
     */
    void proponer(Connection conexion, Cuenta actor, int evaluacion, MotorRula.Calculo calculo) throws SQLException {
        dao.contexto(conexion, evaluacion, 0, false, true);
        var ev = new EvaluacionDao().consultar(conexion, evaluacion);
        var evaluacion2 = ev.datos();
        Map<String, Boolean> condiciones = Map.of("SIEMPRE", true, "escritorio_muy_alto", evaluacion2.escritorio(),
                "pantalla_muy_alta", evaluacion2.pantalla(), "pies_colgando", evaluacion2.pies(), "tiene_apoyapies",
                evaluacion2.apoyapies(), "silla_con_apoyabrazos", evaluacion2.apoyabrazos(), "silla_regulable",
                evaluacion2.regulable());
        Set<Integer> propuestas = new HashSet<>();
        for (var sugerencia : dao.actuales(conexion, evaluacion))
            if (sugerencia.regla() != null) propuestas.add(sugerencia.regla());
        for (var sugerencia2 : aplicables(calculo, condiciones, dao.reglas(conexion))) {
            String tipo = TIPOS_POR_CONDICION.get(sugerencia2.condicion());
            boolean puntos = calculo.mediciones().stream().anyMatch(
                    elemento -> elemento.segmento().name().equals(sugerencia2.segmento()) &&
                            elemento.puntos() >= sugerencia2.minimo());
            if (tipo == null || !Boolean.TRUE.equals(condiciones.get(sugerencia2.condicion())) || !puntos ||
                    propuestas.contains(sugerencia2.id())) continue;
            String texto = sugerencia2.texto().strip();
            if (texto.isEmpty()) continue;
            var sugerencia3 = new Sugerencia(0, evaluacion, sugerencia2.id(), titulo(texto), texto, tipo,
                    prioridad(calculo.resultado().nivel().color()), "PENDIENTE", null, "SISTEMA", true);
            int id = dao.crear(conexion, sugerencia3);
            auditar(conexion, actor, id, "CREAR", null, "PENDIENTE",
                    "Sugerencia del sistema · regla " + sugerencia2.id() + ".");
            propuestas.add(sugerencia2.id());
        }
    }

    /**
     * (propuesta) Andy, 11 nov 2026: prioridad inicial en un solo lugar, fácil de cambiar. RNF-011.
     */
    public static String prioridad(String color) {
        return "ROJO".equals(color) ? "ALTA" : "MEDIA";
    }

    /**
     * Título del texto original, hasta 120 caracteres y sin cortar una palabra (Andy, 11 nov 2026). RNF-011.
     */
    public static String titulo(String texto) {
        String limpio = texto.strip().replaceAll("\\s+", " ");
        if (limpio.codePointCount(0, limpio.length()) <= 120) return limpio;
        int corte = limpio.offsetByCodePoints(0, 120);
        if (!Character.isWhitespace(limpio.codePointAt(corte))) {
            int espacio = limpio.lastIndexOf(' ', corte);
            if (espacio < 0)
                throw new IllegalArgumentException("La regla necesita un título con palabras de hasta 120 caracteres.");
            corte = espacio;
        }
        return limpio.substring(0, corte).stripTrailing();
    }

    private Contexto visible(Connection conexion, Cuenta cuenta, int id, boolean bloqueo) throws SQLException {
        var sugerencia = dao.contexto(conexion, id, AccesoDatos.empresaPermitida(cuenta),
                cuenta.rol() == Cuenta.Rol.USUARIO, bloqueo);
        if (sugerencia == null)
            throw new SecurityException("Acceso denegado. La evaluación no está disponible para tu cuenta.");
        if (sugerencia.puntaje() == null)
            throw new IllegalArgumentException("Guarda un análisis antes de registrar sugerencias.");
        return sugerencia;
    }

    private Sugerencia editable(Connection conexion, int id, int evaluacion) throws SQLException {
        var sugerencia = exigir(dao.buscarActual(conexion, id));
        if (sugerencia.evaluacion() != evaluacion || !sugerencia.activo())
            throw new SecurityException("Acceso denegado. La sugerencia no está disponible para editar.");
        return sugerencia;
    }

    private static Sugerencia exigir(Sugerencia sugerencia) {
        if (sugerencia == null) throw new SecurityException("Acceso denegado. La sugerencia no está disponible.");
        return sugerencia;
    }

    private static Cuenta admin(Connection conexion, Cuenta actor) throws SQLException {
        var cuenta = AccesoDatos.vigente(conexion, actor);
        Permisos.trabajo(cuenta);
        return cuenta;
    }

    private void auditar(Connection conexion, Cuenta cuenta, int id, String accion, String anterior, String nuevo,
            String detalle) throws SQLException {
        bitacora.registrar(conexion, cuenta.id(), LocalDateTime.now(reloj), "sugerencia", id, accion, anterior, nuevo,
                detalle);
    }

    private static LocalDate fecha(String texto2) {
        if (texto2 == null || texto2.isBlank()) return null;
        try {
            var elemento = LocalDate.parse(texto2);
            if (elemento.getYear() < 1000 || elemento.getYear() > 9999) throw new IllegalArgumentException();
            return elemento;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Revisa la fecha límite: escribe una fecha válida.");
        }
    }
}
