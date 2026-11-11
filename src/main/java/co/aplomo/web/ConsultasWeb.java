package co.aplomo.web;
import co.aplomo.servicio.*;import co.aplomo.modelo.*;import co.aplomo.seguridad.Sesiones.Sesion;import com.sun.net.httpserver.HttpExchange;import java.io.*;import java.sql.*;
/** RF-024: solo rutas GET; las acciones existentes conservan sus permisos y CSRF. */
public final class ConsultasWeb {
    private final Consultas consultas;private final Sugerencias sugerencias;
    /** Comparte servicios autorizados y transacciones con Evaluaciones. */
    public ConsultasWeb(Consultas consultas,Sugerencias sugerencias){this.consultas=consultas;this.sugerencias=sugerencias;}
    /** Devuelve true únicamente cuando atendió una lista o el detalle seguro del Usuario. */
    public boolean atender(HttpExchange x,Sesion s)throws SQLException,IOException{
        if(!x.getRequestMethod().equals("GET"))return false;String ruta=x.getRequestURI().getPath();var seccion=SeccionConsulta.deRuta(ruta);boolean detalle=s.cuenta().rol()==Cuenta.Rol.USUARIO&&ruta.matches("/evaluaciones/[1-9][0-9]*");if(seccion==null&&!detalle)return false;
        if(s.cuenta().temporal()){Router.redirigir(x,"/cuenta/contrasena");return true;}
        if(detalle){int id=Validacion.entero(ruta.substring(14),"evaluación",1,Integer.MAX_VALUE);Router.enviar(x,200,PaginasConsultas.evaluacion(s,consultas.evaluacion(s.cuenta(),id)));return true;}
        var q=Router.consulta(x);var p=consultas.listar(s.cuenta(),seccion,q);var contexto=seccion==SeccionConsulta.SUGERENCIAS&&p.filtros().containsKey("evaluacion")?sugerencias.contexto(s.cuenta(),Integer.parseInt(p.filtros().get("evaluacion"))):null;Router.enviar(x,200,PaginasConsultas.lista(s,p,contexto));return true;
    }
}
