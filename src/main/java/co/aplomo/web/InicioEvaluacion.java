package co.aplomo.web;

import co.aplomo.dao.VisitaDao.ParaEvaluar;
import java.util.List;

/** Atajo del Administrador: el servicio filtra visitas en curso y puestos pendientes. */
final class InicioEvaluacion {
    private InicioEvaluacion() {}
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
