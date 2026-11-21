package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Archivo;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** RF-002/024: rutas de archivo y línea de tiempo, sin operaciones de escritura. */
public final class ArchivoWeb {
    private final Archivo archivo;

    /** RNF-011: recibe el servicio de lectura autorizado. */
    public ArchivoWeb(Archivo archivo) {
        this.archivo = archivo;
    }

    /** RF-024: atiende solo lecturas históricas; conserva los formularios de edición de puestos. */
    public boolean atender(HttpExchange intercambio, Sesion sesion) throws SQLException, IOException {
        if (!intercambio.getRequestMethod().equals("GET")) return false;
        String ruta = intercambio.getRequestURI().getPath();
        if (ruta.equals("/mis-evaluaciones")) {
            Router.redirigir(intercambio, "/archivo");
            return true;
        }
        if (!ruta.equals("/archivo") && !ruta.matches("/puestos/[1-9][0-9]*")) return false;
        if (sesion.cuenta().temporal()) {
            Router.redirigir(intercambio, "/cuenta/contrasena");
            return true;
        }
        Archivo.Datos datos = archivo.consultar(sesion.cuenta());
        if (ruta.startsWith("/puestos/")) {
            String puesto = ruta.substring(9);
            List<Fila> filas = datos.evaluaciones().stream().filter(fila -> fila.valor("puesto").equals(puesto)).toList();
            // Un puesto sin historia conserva la página de gestión autorizada existente.
            if (filas.isEmpty()) return false;
            Router.enviar(intercambio, 200, PaginasArchivo.historial(sesion, filas));
        } else {
            Router.enviar(intercambio, 200, PaginasArchivo.pagina(sesion, datos, Router.consulta(intercambio)));
        }
        return true;
    }
}
