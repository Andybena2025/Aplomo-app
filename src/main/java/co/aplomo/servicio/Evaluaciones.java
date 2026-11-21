package co.aplomo.servicio;

import co.aplomo.dao.AnalisisDao;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.PuestoDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Puesto;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RF-014, RF-015, RF-016, RNF-005/006: guarda la evaluación completa en una visita abierta.
 */
public final class Evaluaciones {

    /**
     * Datos aprobados de postura, seis condiciones y consentimiento; sin datos del trabajador. RNF-011.
     */
    public record Datos(int puesto, String postura, String criterio, String motivo, int minutos, boolean escritorio,
                        boolean pantalla, boolean pies, boolean apoyapies, boolean apoyabrazos, boolean regulable,
                        String observaciones, boolean aceptaFotos, boolean sinFotos) {
    }

    /**
     * Contexto permitido al Administrador para iniciar el asistente. RNF-011.
     */
    public record Contexto(VisitaDao.Ficha visita, List<Puesto> puestos) {
    }

    /**
     * Contenido autorizado y tipo real para la respuesta HTTP de una fotografía. RNF-011.
     */
    public record Contenido(byte[] bytes, String tipo) {
    }

    /**
     * Consulta administrativa sin exponer fotografías a cuentas Usuario. RNF-011.
     */
    public record Consulta(EvaluacionDao.Ficha ficha, List<EvaluacionDao.Foto> fotos, boolean enCurso) {
    }

    private final Transacciones tx;
    private final Clock reloj;
    private final Fotos archivos;
    private final BitacoraDao bitacora;
    private final Analisis analisis;
    private final EvaluacionDao dao = new EvaluacionDao();

    /**
     * Usa MySQL, la carpeta fotos/ y la hora de Colombia. RNF-011.
     */
    public Evaluaciones() {
        this(new Transacciones());
    }

    /**
     * Usa el reloj REAL de producción en Bogotá; solo sustituye la conexión para las pruebas reversibles. RNF-011.
     */
    public Evaluaciones(Transacciones tx) {
        this(tx, Clock.system(ZoneId.of("America/Bogota")), new Fotos(), new BitacoraDao());
    }

    /**
     * Comparte conexión, reloj y carpeta aislada con las pruebas. RNF-011.
     */
    public Evaluaciones(Transacciones tx, Clock reloj, Fotos archivos, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.archivos = archivos;
        this.bitacora = bitacora;
        this.analisis = new Analisis(tx, reloj, archivos, bitacora);
    }

    /**
     * Conserva el mismo almacenamiento y auditoría para cambios de fotos. RNF-011.
     */
    public FotosEvaluacion fotosEvaluacion() {
        return new FotosEvaluacion(tx, reloj, archivos, bitacora);
    }

    /**
     * Comprueba permisos y visita abierta antes de ofrecer puestos activos del salón reservado. RNF-011.
     */
    public Contexto contexto(Cuenta actor, int visita) throws SQLException {
        return tx.ejecutar(con -> {
            admin(con, actor);
            var ficha = abierta(con, actor, visita, false);
            return new Contexto(ficha, dao.puestos(con, ficha.salon()));
        });
    }

    /**
     * Siguiente pendiente real de esta visita, en el orden de puestos activos del salón. RNF-011.
     */
    public int siguientePendiente(Cuenta actor, int visita) throws SQLException {
        return tx.ejecutar(con -> {
            admin(con, actor);
            var ficha = abierta(con, actor, visita, false);
            var evaluados = new HashSet<Integer>();
            for (var evaluacion : new VisitaDao().evaluaciones(con, visita, false))
                evaluados.add(evaluacion.puesto());
            return dao.puestos(con, ficha.salon()).stream().filter(elemento -> !evaluados.contains(elemento.id()))
                    .mapToInt(Puesto::id).findFirst().orElse(0);
        });
    }

    /**
     * Valida todo, guarda evaluación, condiciones, fotos y auditoría; revierte archivos nuevos ante fallos. RNF-011.
     */
    public int guardar(Cuenta actor, int visita, Datos entrada,
            List<Fotos.Entrada> fotos) throws SQLException, IOException {
        return guardar(actor, visita, entrada, fotos, null);
    }

    /**
     * Guarda el asistente completo de forma atómica; cualquier fallo revierte también fotos y análisis (RF-021).
     */
    public int guardar(Cuenta actor, int visita, Datos entrada, List<Fotos.Entrada> fotos,
            List<Analisis.Solicitud> solicitudes) throws SQLException, IOException {
        List<String> nuevos = new ArrayList<>();
        try {
            return tx.ejecutar(con -> {
                Cuenta responsable = admin(con, actor);

                // Bloquea primero la reserva para compartir el orden con la finalización de visitas.
                var previa = new VisitaDao().buscar(con, visita, 0, false, false);
                if (previa == null) throw new SecurityException("La visita no está disponible.");
                new ReservaDao().buscar(con, previa.visita().reserva(), 0, true);
                var ficha = abierta(con, actor, visita, true);
                Datos evaluacion = validar(entrada);
                Puesto puesto = new PuestoDao().buscar(con, evaluacion.puesto(), 0, true);
                if (puesto == null || !puesto.activo() || puesto.salon() != ficha.salon())
                    throw new IllegalArgumentException("Elige un puesto activo del salón de esta visita.");
                boolean jerarquia = Boolean.TRUE.equals(Sql.uno(con,
                        "SELECT (e.activo AND s.activo AND l.activo) FROM salon l JOIN sede s ON s.id" +
                                "_sede=l.id_sede JOIN empresa e ON e.id_empresa=s.id_empresa WHERE l.id_salon" + "=?",
                        fila2 -> fila2.getBoolean(1), ficha.salon()));
                if (!jerarquia) throw new IllegalArgumentException(
                        "La empresa, sede o salón está desactivado. Revisa su estado antes de evaluar.");
                List<Fotos.Imagen> imagenes;
                if (evaluacion.sinFotos()) {
                    if (fotos != null && !fotos.isEmpty())
                        throw new IllegalArgumentException("Sin registro fotográfico no se aceptan fotos.");
                    imagenes = List.of();
                } else {
                    imagenes = archivos.validar(fotos);
                }
                int id = dao.crear(con, visita, evaluacion, evaluacion.motivo(), LocalDate.now(reloj));
                dao.condiciones(con, id, evaluacion);
                List<Integer> idsFotos = new ArrayList<>();
                for (var imagen : imagenes) {
                    String nombre;
                    try {
                        nombre = archivos.guardar(imagen);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                    nuevos.add(nombre);
                    idsFotos.add(
                            dao.foto(con, id, nombre, imagen.vista(), imagen.descripcion(), imagen.bytes().length));
                }
                bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "evaluacion", id, "CREAR", null,
                        null, "Evaluación del puesto " + puesto.numero() + " · Consentimiento escrito confirmado" +
                                (evaluacion.sinFotos() ? " · Sin registro fotográfico" : " · Con fotos"));
                if (solicitudes != null) {
                    if (evaluacion.sinFotos() && !solicitudes.isEmpty())
                        throw new IllegalArgumentException("Sin fotos no se puede guardar un análisis RULA.");
                    if (!evaluacion.sinFotos() && (solicitudes.isEmpty() || solicitudes.size() > 20))
                        throw new IllegalArgumentException("Mide al menos una foto antes de guardar el resultado.");
                    Set<String> usadas = new HashSet<>();
                    for (var solicitud : solicitudes) {
                        if (solicitud == null || solicitud.entrada() == null || solicitud.entrada().lado() == null)
                            throw new IllegalArgumentException("Completa el lado y las mediciones de la foto.");
                        if (solicitud.foto() < 0 || solicitud.foto() >= idsFotos.size() ||
                                !usadas.add(solicitud.foto() + ":" + solicitud.entrada().lado()))
                            throw new IllegalArgumentException("Revisa la selección de fotos de los análisis.");
                        try {
                            analisis.guardarEnTransaccion(con, responsable, idsFotos.get(solicitud.foto()),
                                    solicitud.entrada());
                        } catch (IOException error2) {
                            throw new UncheckedIOException(error2);
                        }
                    }
                }
                new AnalisisDao().actualizarPuesto(con, evaluacion.puesto());
                return id;
            });
        } catch (SQLException | RuntimeException fallo) {
            try {
                archivos.revertirNuevos(nuevos);
            } catch (IOException limpieza) {
                fallo.addSuppressed(limpieza);
            }
            if (fallo instanceof UncheckedIOException error3) throw error3.getCause();
            throw fallo;
        }
    }

    /**
     * Autoriza antes de leer la ruta y entregar una foto; un Usuario nunca recibe contenido ni metadatos. RNF-011.
     */
    public Contenido foto(Cuenta actor, int id) throws SQLException, IOException {
        var foto = tx.ejecutar(con -> {
            admin(con, actor);
            var evaluacion = dao.foto(con, id);
            if (evaluacion != null) AccesoDatos.exigirEvaluacion(con, actor, evaluacion.evaluacion(), false);
            return evaluacion;
        });
        if (foto == null) throw new FileNotFoundException("Foto no disponible.");
        return new Contenido(archivos.leer(foto.nombre()), foto.nombre().endsWith(".png") ? "image/png" : "image/jpeg");
    }

    /**
     * Abre la evaluación guardada con sus condiciones y fotografías solo para Administrador. RNF-011.
     */
    public Consulta consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            admin(con, actor);
            AccesoDatos.exigirEvaluacion(con, actor, id, false);
            var ficha = dao.consultar(con, id);
            if (ficha == null) throw new SecurityException("La evaluación no está disponible.");
            var visita = new VisitaDao().buscar(con, ficha.visita(), 0, false, false);
            return new Consulta(ficha, dao.fotos(con, id),
                    visita != null && visita.visita().estado().equals("EN_CURSO") && asignada(con, actor, id));
        });
    }

    /**
     * RF-002: distingue el historial propio retirado de una empresa asignada actualmente.
     */
    public boolean permiteTrabajo(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(conexion -> {
            try {
                AccesoDatos.exigirEvaluacion(conexion, actor, id, true);
                return true;
            } catch (SecurityException error) {
                return false;
            }
        });
    }

    /**
     * Comparte el servicio de análisis para vista previa y nuevos análisis de fotos existentes. RNF-011.
     */
    public Analisis analisis() {
        return analisis;
    }

    /**
     * Comparte seguimiento, transacciones y reloj con las rutas del Paso 9. RNF-011.
     */
    public Pendientes pendientes() {
        return new Pendientes(tx, reloj, bitacora);
    }

    /**
     * RF-023a/b: comparte reglas, permisos y auditoría con las rutas de sugerencias.
     */
    public Sugerencias sugerencias() {
        return new Sugerencias(tx, reloj, bitacora);
    }

    /**
     * RF-024: consultas de solo lectura que comparten la transacción autorizada.
     */
    public Consultas consultas() {
        return new Consultas(tx);
    }

    /**
     * Paso 25: ayuda por rol con niveles de la misma base. RNF-011.
     */
    public Ayuda ayuda() {
        return new Ayuda(tx);
    }

    /**
     * RF-025/026: comparte transacciones y fecha de generación con los indicadores y el informe.
     */
    public Calificaciones calificaciones() {
        return new Calificaciones(tx, reloj);
    }

    /**
     * Cuenta evaluaciones y colores de puestos para Inicio; el Usuario conserva su alcance anterior (RF-024).
     */
    public long[] resumen(Cuenta actor) throws SQLException {
        return tx.ejecutar(conexion -> {
            Cuenta actual = AccesoDatos.vigente(conexion, actor);
            var empresa = AccesoDatos.empresaPermitida(actual);
            boolean usuario = actual.rol() == Cuenta.Rol.USUARIO;
            long evaluaciones = Sql.uno(conexion,
                    "SELECT COUNT(*) FROM evaluacion e JOIN visita v USING(id_visita) JOIN puesto" +
                            "_trabajo p USING(id_puesto) JOIN salon l USING(id_salon) JOIN sede s USING(i" +
                            "d_sede) WHERE " + empresa.sql("s.id_empresa") + " AND (?=FALSE OR v.estado='FINALIZADA')" +
                            co.aplomo.modelo.Demostracion.filtro("s.id_empresa"), fila2 -> fila2.getLong(1),
                    empresa.todas(), empresa.valor(), usuario);
            long rojos;
            if (!usuario) {
                Map<String, Long> colores = new HashMap<>();
                for (var fila : Sql.listar(conexion,
                        "SELECT n.color,COUNT(*) FROM puesto_trabajo p JOIN salon l USING(id_salon) J" +
                                "OIN sede s USING(id_sede) JOIN nivel_riesgo n ON p.ultimo_puntaje BETWEEN n." +
                                "puntaje_minimo AND n.puntaje_maximo WHERE " + empresa.sql("s.id_empresa") +
                                co.aplomo.modelo.Demostracion.filtro("s.id_empresa") + " GROUP BY n.color",
                        fila3 -> Map.entry(fila3.getString(1), fila3.getLong(2)), empresa.todas(), empresa.valor()))
                    colores.put(fila.getKey(), fila.getValue());
                return new long[]{evaluaciones, colores.getOrDefault("ROJO", 0L), colores.getOrDefault("VERDE",
                        0L), colores.getOrDefault("AMARILLO", 0L)};
            } else rojos = Sql.uno(conexion,
                    "SELECT COUNT(*) FROM puesto_trabajo p JOIN salon l USING(id_salon) JOIN sede" +
                            " s USING(id_sede) WHERE " + empresa.sql("s.id_empresa") + "" +
                            co.aplomo.modelo.Demostracion.filtro("s.id_empresa") +
                            " AND EXISTS (SELECT 1 FROM nivel_riesgo n WHERE n.color=? AND (SELECT MAX(a." +
                            "puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f" +
                            ".id_evaluacion=(SELECT MAX(e.id_evaluacion) FROM evaluacion e JOIN visita v " +
                            "USING(id_visita) WHERE e.id_puesto=p.id_puesto AND v.estado='FINALIZADA')) B" +
                            "ETWEEN n.puntaje_minimo AND n.puntaje_maximo)", fila4 -> fila4.getLong(1), empresa.todas(),
                    empresa.valor(), "ROJO");
            return new long[]{evaluaciones, rojos};
        });
    }

    private static boolean asignada(Connection conexion, Cuenta cuenta, int cantidad) throws SQLException {
        try {
            AccesoDatos.exigirEvaluacion(conexion, cuenta, cantidad, true);
            return true;
        } catch (SecurityException error) {
            return false;
        }
    }

    private static Datos validar(Datos evaluacion) {
        if (evaluacion == null) throw new IllegalArgumentException("Completa los datos de la evaluación.");
        String postura = Validacion.texto(evaluacion.postura(), "postura o tarea", 150);
        String criterio = Validacion.opcion(evaluacion.criterio(), "criterio de elección", "MAS_INADECUADA",
                "MAYOR_EXPOSICION");
        if (!evaluacion.aceptaFotos()) throw new IllegalArgumentException(
                "Confirma que la persona firmó en papel el consentimiento para ser evaluada.");
        String momento = criterio.equals("MAS_INADECUADA") ? "La postura más exigente" : "La que más tiempo dura";
        boolean campo = Set.of("El momento que señala el trabajador.", "La postura que más repite.")
                .contains(evaluacion.motivo() == null ? "" : evaluacion.motivo());
        String motivo = "La postura más incómoda.".equals(evaluacion.motivo()) &&
                criterio.equals("MAS_INADECUADA") ? evaluacion.motivo() :
                "La postura que más tiempo mantiene.".equals(evaluacion.motivo()) &&
                        criterio.equals("MAYOR_EXPOSICION") ? evaluacion.motivo() :
                "También es la que más tiempo se mantiene".equals(evaluacion.motivo()) &&
                        criterio.equals("MAS_INADECUADA") ? evaluacion.motivo() : campo ? (criterio.equals(
                        "MAS_INADECUADA") ? "El momento que señala el trabajador." : "La postura que más repite.") :
                                                                                  "Momento: " + momento +
                                                                                  ". Postura: " + postura + ".";
        Validacion.entero(Integer.toString(evaluacion.minutos()), "minutos por jornada", 1, 720);
        String obs = Validacion.notas(evaluacion.observaciones());
        if (obs.codePointCount(0, obs.length()) > 255)
            throw new IllegalArgumentException("Otras observaciones permite hasta 255 caracteres.");
        return new Datos(evaluacion.puesto(), postura, criterio, motivo, evaluacion.minutos(), evaluacion.escritorio(),
                evaluacion.pantalla(), evaluacion.pies(), evaluacion.apoyapies(), evaluacion.apoyabrazos(),
                evaluacion.regulable(), obs, evaluacion.aceptaFotos(), evaluacion.sinFotos());
    }

    private static Cuenta admin(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        Permisos.trabajo(actual);
        return actual;
    }

    private static VisitaDao.Ficha abierta(Connection con, Cuenta actor, int id, boolean bloquear) throws SQLException {
        AccesoDatos.exigirVisita(con, actor, id);
        var ficha = new VisitaDao().buscar(con, id, 0, false, bloquear);
        if (ficha == null) throw new SecurityException("La visita no está disponible.");
        if (!ficha.visita().estado().equals("EN_CURSO"))
            throw new IllegalArgumentException("Solo puedes evaluar dentro de una visita En curso.");
        return ficha;
    }
}
