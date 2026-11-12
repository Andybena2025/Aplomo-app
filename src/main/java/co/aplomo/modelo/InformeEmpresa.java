package co.aplomo.modelo;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** RF-025/026: instantánea de resultados Finalizados, sin fotos ni rutas privadas. */
public record InformeEmpresa(Empresa empresa, LocalDateTime generado, List<SedeResumen> sedes,
                             Calificacion general, List<String> sedesSinCalificacion, List<Riesgo> leyenda, Estadisticas totales) {
    /** Promedio del equipo y su clasificación leída de MySQL; no es un nuevo cálculo RULA. */
    public record Calificacion(BigDecimal promedio, int redondeado, Riesgo riesgo) {}
    /** Rango oficial y textos de nivel_riesgo, sin umbrales duplicados. */
    public record Riesgo(int minimo, int maximo, String color, int nivel, String nombre, String accion) {}
    /** Visita histórica con su calificación guardada al finalizar. */
    public record VisitaResumen(int id, String codigo, LocalDateTime inicio, LocalDateTime fin,
                               String notas, Calificacion calificacion) {}
    /** Última evaluación Finalizada de un puesto y máximo de sus análisis. */
    public record PuestoResumen(int id, int evaluacion, int visita, int numero, String salon, String area,
                               String cargo, String tipoCargo, String descripcion, String postura,
                               String motivo, String condiciones, LocalDate fecha, Integer puntaje, Riesgo riesgo) {}
    /** Pendiente histórico cuyo estado se conserva aunque una reevaluación salga Verde. */
    public record PendienteResumen(int id, int evaluacion, int numero, String salon, String descripcion,
                                   String prioridad, String estado, LocalDate fecha, LocalDate limite,
                                   int puntaje, Riesgo riesgo) {}
    /** Sugerencia activa de una evaluación Finalizada, conservando tipo y estado. */
    public record SugerenciaResumen(int id, int evaluacion, int numero, String salon, String titulo,
                                    String descripcion, String tipo, String estado, String prioridad,
                                    String origen, LocalDate limite) {}
    /** Cifras de una sede: puestos únicos actuales; visitas/fotos de todo su historial Finalizado. */
    public record Estadisticas(int visitas, int puestos, int conPuntaje, int fotosAnalizadas,
                               Map<String,Integer> colores, Map<String,Integer> pendientes,
                               Map<String,Map<String,Integer>> sugerencias, Map<String,Integer> areas,
                               Map<String,Integer> cargos, List<String> areasMasRojo) {}
    /** Desglose de una sede, incluida si está inactiva o aún no tiene calificación. */
    public record SedeResumen(Sede sede, Calificacion calificacion, List<VisitaResumen> visitas,
                              List<PuestoResumen> puestos, List<PendienteResumen> pendientes,
                              List<SugerenciaResumen> sugerencias, Estadisticas estadisticas) {}
}
