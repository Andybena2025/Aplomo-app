package co.aplomo.web;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Evaluaciones;
/** RF-014 a RF-020: entrada única al recorrido vigente, sin cálculos ni persistencia. */
final class RevisionEvaluacion {
 private RevisionEvaluacion(){}
 static String nueva(Sesion s,Evaluaciones.Contexto c,int seleccionado){return ConversacionEvaluacion.nueva(s,c,seleccionado);}
 static String continuar(Sesion s,Evaluaciones.Consulta c){return ConversacionEvaluacion.continuar(s,c);}
}
