package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.*;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;

/** RF-025/026: rutas autenticadas de consulta y de informe imprimible. */
public final class CalificacionesWeb {
    private final Calificaciones servicio;
    /** Recibe el servicio con permisos verificados también en el servidor. */
    public CalificacionesWeb(Calificaciones servicio){this.servicio=servicio;}

    /** Atiende solo calificaciones o informes; imprimir/guardar PDF lo realiza el navegador. */
    public boolean atender(HttpExchange x,Sesion s)throws IOException,SQLException {
        String ruta=x.getRequestURI().getPath();
        if(!ruta.equals("/calificaciones")&&!ruta.startsWith("/informes/"))return false;
        if(!x.getRequestMethod().equals("GET")){x.getResponseHeaders().set("Allow","GET");Router.enviar(x,405,"Usa la vista de calificaciones o el botón Generar informe.");return true;}
        if(ruta.equals("/calificaciones")){
            String id=Router.consulta(x).getOrDefault("empresa",s.cuenta().rol()==Cuenta.Rol.USUARIO?String.valueOf(s.cuenta().empresa()):"");
            if(id.isBlank())Router.enviar(x,200,PaginasCalificaciones.elegir(s,servicio.empresas(s.cuenta())));
            else Router.enviar(x,200,PaginasCalificaciones.calificaciones(s,servicio.consultar(s.cuenta(),Validacion.entero(id,"empresa",1,Integer.MAX_VALUE))));
        }else if(ruta.matches("/informes/empresa/[1-9][0-9]*")){
            int id=Validacion.entero(ruta.substring("/informes/empresa/".length()),"empresa",1,Integer.MAX_VALUE);
            Router.enviar(x,200,PaginasCalificaciones.informe(s,servicio.informe(s.cuenta(),id)));
        }else Router.enviar(x,404,Html.pagina("Informe no encontrado",Html.aviso("Abre una empresa y pulsa Generar informe.",false),s));
        return true;
    }
}
