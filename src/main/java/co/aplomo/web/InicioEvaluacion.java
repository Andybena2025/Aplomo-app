package co.aplomo.web;

import co.aplomo.dao.VisitaDao.ParaEvaluar;
import java.util.List;

/** Atajo del Administrador: el servicio filtra visitas en curso y puestos pendientes. */
final class InicioEvaluacion {
    private InicioEvaluacion() {}
    static String practica() {
        return GaleriaDemo.habilitada()?"<section class=\"card stack\" aria-labelledby=\"practicar-evaluacion\"><div><span class=\"eyebrow\">Demostración</span><h2 id=\"practicar-evaluacion\">Probar evaluación</h2></div><p>Practica el recorrido con un puesto de Oficina o Planta y fotos de ejemplo. Puedes repetirlo aunque ya hayas terminado una evaluación.</p><div><a class=\"btn p\" href=\"/demo/evaluacion\">Abrir evaluación de prueba</a></div></section>":"";
    }
    static String paginaPractica(co.aplomo.seguridad.Sesiones.Sesion s) {
        return Html.pagina("Probar evaluación","<div class=\"pagehead\"><div><span class=\"eyebrow\">Demostración · Evaluación</span><h1>Probar evaluación</h1></div></div><section class=\"card stack\"><p>Elige un puesto para practicar el mismo recorrido de la app.</p><p>Se prepara una visita En curso en APLOMO · Práctica de evaluación. Los resultados se guardan como demostración en la base actual; puedes repetir la prueba sin borrar intentos.</p><form method=\"post\" action=\"/demo/evaluacion\" class=\"row\">"+Html.csrf(s)+"<button class=\"btn p\" name=\"tipo\" value=\"PLANTA\">Probar Planta</button><button class=\"btn\" name=\"tipo\" value=\"OFICINA\">Probar Oficina</button></form></section>",s);
    }
    static String tarjeta(List<ParaEvaluar> filas) {
        StringBuilder h=new StringBuilder("<section class=\"card stack evaluar-ahora\" aria-labelledby=\"evaluar-ahora\"><h2 id=\"evaluar-ahora\">Evaluar ahora</h2>");
        if(filas.isEmpty())return h.append("<p>No hay una visita en curso</p><div><a class=\"btn p\" href=\"/reservas\">Ir a Reservas</a></div></section>").toString();
        int anterior=-1;
        for(var f:filas){
            if(f.visita()!=anterior){
                if(anterior!=-1)h.append("</ul></div>");
                h.append("<div class=\"stack\"><h3>").append(Html.escapar(f.empresa())).append("</h3><p class=\"muted\">").append(Html.escapar(f.sede())).append("</p><ul class=\"evaluar-puestos\">");anterior=f.visita();
            }
            if(f.puesto()==null)h.append("<li>Todos los puestos están evaluados.</li>");
            else h.append("<li><div><strong>Puesto ").append(f.numero()).append("</strong><div class=\"muted small\">").append(Html.escapar(f.descripcion())).append("</div></div><a class=\"btn p\" href=\"/evaluaciones/nueva?visita=").append(f.visita()).append("&amp;puesto=").append(f.puesto()).append("\">Evaluar</a></li>");
        }
        return h.append("</ul></div></section>").toString();
    }
}
