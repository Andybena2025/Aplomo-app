package co.aplomo.servicio;

import co.aplomo.dao.AnalisisDao;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.dao.PuestoDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.dao.SugerenciaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.rula.MotorRula;
import co.aplomo.rula.TablasRula;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RF-017 a RF-021: autorización, persistencia y auditoría; el cálculo permanece en el módulo RULA.
 */
public final class Analisis {

    /**
     * Referencia a una foto y sus entradas; no incluye ningún puntaje enviado por la pantalla. RNF-011.
     */
    public record Solicitud(int foto, MotorRula.Entrada entrada) {
    }

    private final Transacciones tx;
    private final Clock reloj;
    private final Fotos archivos;
    private final BitacoraDao bitacora;
    private final TablasRula tablas = new TablasRula();
    private final AnalisisDao dao = new AnalisisDao();

    /**
     * Comparte transacciones, fotos y reloj con las evaluaciones y las pruebas. RNF-011.
     */
    public Analisis(Transacciones tx, Clock reloj, Fotos archivos, BitacoraDao bitacora) {
        this.tx = tx;
        this.reloj = reloj;
        this.archivos = archivos;
        this.bitacora = bitacora;
    }

    /**
     * Calcula una vista previa autorizada, sin guardar ni aceptar puntajes del navegador. RNF-011.
     */
    public MotorRula.Calculo previsualizar(Cuenta actor, int visita, MotorRula.Entrada entrada) throws SQLException {
        return tx.ejecutar(conexion -> {
            admin(conexion, actor);
            abierta(conexion, actor, visita, false);
            tablas.cargar(conexion);
            return new MotorRula(tablas).calcular(entrada);
        });
    }

    /**
     * Autoriza una vista previa parcial sin completar respuestas desconocidas ni calcular un total. RNF-011.
     */
    public void autorizarParcial(Cuenta actor, int visita) throws SQLException {
        tx.ejecutar(conexion -> {
            admin(conexion, actor);
            abierta(conexion, actor, visita, false);
            return null;
        });
    }

    /**
     * Inserta un análisis dentro de la transacción de una evaluación, usando las dimensiones reales de su foto.
     */
    int guardarEnTransaccion(Connection conexion, Cuenta actor, int foto,
            MotorRula.Entrada entrada) throws SQLException, IOException {
        if (entrada == null) throw new IllegalArgumentException("Completa las mediciones de la foto.");
        var evaluacion2 = new EvaluacionDao().foto(conexion, foto);
        if (evaluacion2 == null) throw new SecurityException("La foto no está disponible.");
        if (evaluacion2.descripcion().startsWith(FotosEvaluacion.REEMPLAZADA))
            throw new IllegalArgumentException("Esta foto fue reemplazada. Selecciona una foto actual.");
        new SugerenciaDao().contexto(conexion, evaluacion2.evaluacion(), 0, false, true);
        int[] dimensiones = archivos.dimensiones(evaluacion2.nombre());
        var real = new MotorRula.Entrada(entrada.lado(), dimensiones[0], dimensiones[1], entrada.mediciones(),
                entrada.ajustes(), entrada.actividad(), entrada.carga());
        tablas.cargar(conexion);
        var calculo = new MotorRula(tablas).calcular(real);
        int id = dao.guardar(conexion, foto, LocalDateTime.now(reloj), calculo);
        bitacora.registrar(conexion, actor.id(), LocalDateTime.now(reloj), "analisis_rula", id, "CREAR", null, null,
                "Análisis nuevo · " + real.lado() + " · Foto " + foto);
        if (calculo.resultado().nivel().color().equals("ROJO"))
            new Pendientes(tx, reloj, bitacora).registrarRojo(conexion, actor, evaluacion2.evaluacion());
        new Sugerencias(tx, reloj, bitacora).proponer(conexion, actor, evaluacion2.evaluacion(), calculo);
        return id;
    }

    /**
     * Añade análisis a fotos ya guardadas; verifica relación, visita abierta y conserva todo el historial. RNF-011.
     */
    public void guardar(Cuenta actor, int evaluacion, List<Solicitud> solicitudes) throws SQLException, IOException {
        try {
            tx.ejecutar(conexion -> {
                Cuenta responsable = admin(conexion, actor);
                AccesoDatos.exigirEvaluacion(conexion, actor, evaluacion, true);
                var evaluacion2 = new EvaluacionDao().consultar(conexion, evaluacion);
                if (evaluacion2 == null) throw new SecurityException("La evaluación no está disponible.");
                var previa = abierta(conexion, actor, evaluacion2.visita(), false);
                new ReservaDao().buscar(conexion, previa.visita().reserva(), 0, true);
                abierta(conexion, actor, evaluacion2.visita(), true);
                new PuestoDao().buscar(conexion, evaluacion2.puesto(), 0, true);
                if (solicitudes == null || solicitudes.isEmpty() || solicitudes.size() > 20)
                    throw new IllegalArgumentException(
                            "Completa al menos una foto y guarda hasta veinte análisis a la vez.");
                Set<String> vistas = new HashSet<>();
                for (var elemento : solicitudes) {
                    if (elemento == null || elemento.entrada() == null || elemento.entrada().lado() == null)
                        throw new IllegalArgumentException("Completa el lado y las mediciones de la foto.");
                    var evaluacion3 = new EvaluacionDao().foto(conexion, elemento.foto());
                    if (evaluacion3 == null || evaluacion3.evaluacion() != evaluacion)
                        throw new SecurityException("La foto no pertenece a esta evaluación.");
                    if (!vistas.add(elemento.foto() + ":" + elemento.entrada().lado()))
                        throw new IllegalArgumentException(
                                "Selecciona cada foto y lado una sola vez; puedes volver a analizarlos después.");
                    try {
                        guardarEnTransaccion(conexion, responsable, elemento.foto(), elemento.entrada());
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                }
                dao.actualizarPuesto(conexion, evaluacion2.puesto());
                return null;
            });
        } catch (UncheckedIOException error2) {
            throw error2.getCause();
        }
    }

    /**
     * Consulta el historial administrativo sin entregar fotos a un Usuario. RNF-011.
     */
    public List<AnalisisDao.Resumen> listar(Cuenta actor, int evaluacion) throws SQLException {
        return tx.ejecutar(conexion -> {
            admin(conexion, actor);
            AccesoDatos.exigirEvaluacion(conexion, actor, evaluacion, false);
            return dao.listar(conexion, evaluacion);
        });
    }

    /**
     * Resultado histórico autorizado, con fecha y responsable (RF-021, RNF-005).
     */
    public record Guardado(AnalisisDao.Ficha ficha, MotorRula.Calculo calculo) {
    }

    /**
     * Abre el resultado guardado sin repetir ni reemplazar un análisis. RNF-011.
     */
    public Guardado consultar(Cuenta actor, int evaluacion, int analisis) throws SQLException {
        return tx.ejecutar(conexion -> {
            admin(conexion, actor);
            AccesoDatos.exigirEvaluacion(conexion, actor, evaluacion, false);
            var elemento = dao.consultar(conexion, analisis);
            if (elemento == null || elemento.evaluacion() != evaluacion)
                throw new SecurityException("El análisis no está disponible.");
            tablas.cargar(conexion);
            return new Guardado(elemento,
                    new MotorRula(tablas).restaurar(elemento.entrada(), dao.mediciones(conexion, analisis),
                            elemento.puntajeA(), elemento.puntajeB(), elemento.fin()));
        });
    }

    private static Cuenta admin(Connection conexion, Cuenta cuenta) throws SQLException {
        var actual = AccesoDatos.vigente(conexion, cuenta);
        Permisos.trabajo(actual);
        return actual;
    }

    private static VisitaDao.Ficha abierta(Connection conexion, Cuenta actor, int visita,
            boolean bloquear) throws SQLException {
        AccesoDatos.exigirVisita(conexion, actor, visita);
        var visita2 = new VisitaDao().buscar(conexion, visita, 0, false, bloquear);
        if (visita2 == null) throw new SecurityException("La visita no está disponible.");
        if (!visita2.visita().estado().equals("EN_CURSO"))
            throw new IllegalArgumentException("La visita ya está finalizada. Sus análisis se conservan sin cambios.");
        return visita2;
    }
}
