package co.aplomo.web;

import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Evaluaciones;

/**
 * RF-014 a RF-020: entrada única al recorrido vigente, sin cálculos ni persistencia.
 */
final class RevisionEvaluacion {
    private RevisionEvaluacion() {
    }

    static String nueva(Sesion sesion, Evaluaciones.Contexto evaluacion, int seleccionado) {
        return ConversacionEvaluacion.nueva(sesion, evaluacion, seleccionado);
    }

    static String continuar(Sesion sesion, Evaluaciones.Consulta evaluacion) {
        return ConversacionEvaluacion.continuar(sesion, evaluacion);
    }
}
