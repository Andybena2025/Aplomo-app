package co.aplomo.web;

import co.aplomo.servicio.Consultas.Pagina;
import co.aplomo.dao.SugerenciaDao.Contexto;
import co.aplomo.seguridad.Sesiones.Sesion;

/**
 * RF-024, RNF-011: entrada única de las consultas, sin conservar la lista visual retirada.
 */
public final class ListasConsulta {
    private ListasConsulta() {
    }

    /**
     * RF-002/024: únicamente recibe filas autorizadas por el servicio de consultas.
     */
    public static String pagina(Sesion sesion, Pagina pagina, Contexto contexto) {
        return TablasTrabajo.pagina(sesion, pagina, contexto);
    }
}
