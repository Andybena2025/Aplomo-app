package co.aplomo.web;

import co.aplomo.servicio.*;import co.aplomo.seguridad.Permisos;import co.aplomo.seguridad.Sesiones.Sesion;import com.sun.net.httpserver.HttpExchange;
import java.io.*;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;import java.sql.*;import java.util.*;

/** RF-022: consulta por alcance y cambios solo para Administrador; Router verifica CSRF antes de entrar. */
public final class PendientesWeb {
    private final Pendientes servicio;
    /** Recibe el mismo servicio de seguimiento que utiliza el guardado del análisis. */
    public PendientesWeb(Pendientes servicio){this.servicio=servicio;}
    /** Presenta lista/detalle y conserva campos en los errores de guardado. */
    public void atender(HttpExchange x,Sesion s,Map<String,String> datos)throws IOException,SQLException{
        String ruta=x.getRequestURI().getPath();boolean post=x.getRequestMethod().equals("POST");
        if(ruta.equals("/pendientes")&&!post){var q=Router.consulta(x);String estado=q.getOrDefault("estado","ABIERTO"),orden=q.getOrDefault("orden","prioridad");Router.enviar(x,200,PaginasPendientes.lista(s,servicio.listar(s.cuenta(),estado,orden),estado,orden,q.get("aviso")));return;}
        if(!ruta.matches("/pendientes/[1-9][0-9]*(/(editar|arreglado))?")){Router.enviar(x,404,"Página no disponible.");return;}
        String[] partes=ruta.split("/");int id=Validacion.entero(partes[2],"pendiente",1,Integer.MAX_VALUE);String accion=partes.length>3?partes[3]:"";
        if(post||!accion.isEmpty())Permisos.administrador(s.cuenta());
        var f=servicio.consultar(s.cuenta(),id);
        if(!post&&accion.isEmpty()){Router.enviar(x,200,PaginasPendientes.detalle(s,f,servicio.historial(s.cuenta(),id),Router.consulta(x).get("aviso"),true));return;}
        if(!post&&accion.equals("editar")){Router.enviar(x,200,PaginasPendientes.editar(s,f,Map.of(),null));return;}
        if(post&&accion.equals("arreglado")){
            try{boolean cambio=servicio.arreglar(s.cuenta(),id);redirigir(x,id,cambio?"Pendiente marcado como Arreglado.":"El pendiente ya estaba Arreglado.");}
            catch(SQLException e){Router.enviar(x,503,PaginasPendientes.detalle(s,f,servicio.historial(s.cuenta(),id),"No pudimos guardar el cambio. Inténtalo de nuevo.",false));}return;
        }
        if(post&&accion.isEmpty()){
            try{servicio.guardar(s.cuenta(),id,datos.get("descripcion"),datos.get("prioridad"),datos.get("fecha"));redirigir(x,id,"Seguimiento guardado.");}
            catch(IllegalArgumentException e){Router.enviar(x,400,PaginasPendientes.editar(s,f,datos,e.getMessage()));}
            catch(SQLException e){Router.enviar(x,503,PaginasPendientes.editar(s,f,datos,"No pudimos guardar. Tus campos siguen escritos; inténtalo de nuevo."));}return;
        }
        x.getResponseHeaders().set("Allow",accion.equals("arreglado")?"POST":"GET");Router.enviar(x,405,"Usa los botones de la página para realizar esta acción.");
    }
    private static void redirigir(HttpExchange x,int id,String aviso)throws IOException{String ruta="/pendientes/"+id+"?aviso="+URLEncoder.encode(aviso,StandardCharsets.UTF_8);if("text/plain".equals(x.getRequestHeaders().getFirst("Accept"))){x.getResponseHeaders().set("Location",ruta);Router.responder(x,201,"text/plain; charset=UTF-8",aviso);}else Router.redirigir(x,ruta);}
}
