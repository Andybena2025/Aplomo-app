package co.aplomo.web;
import co.aplomo.servicio.*;import co.aplomo.seguridad.*;import co.aplomo.seguridad.Sesiones.Sesion;import com.sun.net.httpserver.HttpExchange;
import java.io.*;import java.sql.*;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;import java.util.*;
/** RF-023a/b: consulta y gestión protegidas por sesión, alcance y CSRF del Router. */
public final class SugerenciasWeb {
    private final Sugerencias servicio;
    /** Recibe el servicio compartido con el guardado del análisis. */
    public SugerenciasWeb(Sugerencias servicio){this.servicio=servicio;}
    /** Resuelve formularios, estados y consultas sin aceptar origen, regla o empresa desde campos ocultos. */
    public void atender(HttpExchange x,Sesion s,Map<String,String>d)throws IOException,SQLException{
        String ruta=x.getRequestURI().getPath();boolean post=x.getRequestMethod().equals("POST");var q=Router.consulta(x);
        if(ruta.equals("/sugerencias")&&!post){int e=q.containsKey("evaluacion")?id(q.get("evaluacion")):0;Router.enviar(x,200,PaginasSugerencias.lista(s,servicio.listar(s.cuenta(),e),e==0?null:servicio.contexto(s.cuenta(),e)));return;}
        if(post||ruta.endsWith("/editar")||ruta.equals("/sugerencias/nueva"))Permisos.administrador(s.cuenta());
        if(ruta.equals("/sugerencias/nueva")||ruta.equals("/sugerencias")){
            int e=id(q.get("evaluacion"));var c=servicio.contexto(s.cuenta(),e);
            if(!post){Router.enviar(x,200,PaginasSugerencias.formulario(s,c,null,Map.of(),null));return;}
            guardar(x,s,e,0,c,null,d);return;
        }
        if(!ruta.matches("/sugerencias/[1-9][0-9]*(/(editar|quitar|estado))?")){Router.enviar(x,404,"Página no disponible.");return;}
        var partes=ruta.split("/");int id=id(partes[2]);String accion=partes.length>3?partes[3]:"";var f=servicio.consultar(s.cuenta(),id);
        if(!post&&accion.isEmpty()){detalle(x,s,id,q.get("aviso"),true,200);return;}
        if(!post&&accion.equals("editar")){if(!f.sugerencia().activo())throw new IllegalArgumentException("La sugerencia fue quitada y conserva su historial.");Router.enviar(x,200,PaginasSugerencias.formulario(s,f.contexto(),f.sugerencia(),Map.of(),null));return;}
        if(post&&accion.isEmpty()){guardar(x,s,f.contexto().evaluacion(),id,f.contexto(),f.sugerencia(),d);return;}
        if(post&&(accion.equals("quitar")||accion.equals("estado"))){try{if(accion.equals("quitar"))servicio.quitar(s.cuenta(),id);else servicio.estado(s.cuenta(),id,d.get("estado"));destino(x,id,accion.equals("quitar")?"Sugerencia quitada. Su historial se conserva.":"Estado guardado.");}catch(IllegalArgumentException e){detalle(x,s,id,e.getMessage(),false,400);}catch(SQLException e){detalle(x,s,id,"No pudimos guardar el cambio. Inténtalo de nuevo.",false,503);}return;}
        x.getResponseHeaders().set("Allow",accion.equals("quitar")||accion.equals("estado")?"POST":"GET");Router.enviar(x,405,"Usa los botones de la página para realizar esta acción.");
    }
    private void guardar(HttpExchange x,Sesion s,int evaluacion,int id,co.aplomo.dao.SugerenciaDao.Contexto c,co.aplomo.modelo.Sugerencia a,Map<String,String>d)throws IOException,SQLException{try{int nuevo=servicio.guardar(s.cuenta(),evaluacion,id,d.get("titulo"),d.get("descripcion"),d.get("tipo"),d.get("prioridad"),d.get("fecha"));destino(x,nuevo,"Sugerencia guardada.");}catch(IllegalArgumentException e){Router.enviar(x,400,PaginasSugerencias.formulario(s,c,a,d,e.getMessage()));}catch(SQLException e){Router.enviar(x,503,PaginasSugerencias.formulario(s,c,a,d,"No pudimos guardar. Tus campos siguen escritos; inténtalo de nuevo."));}}
    private void detalle(HttpExchange x,Sesion s,int id,String aviso,boolean exito,int codigo)throws IOException,SQLException{var f=servicio.consultar(s.cuenta(),id);Router.enviar(x,codigo,PaginasSugerencias.detalle(s,f,servicio.historial(s.cuenta(),id),servicio.todasHechas(s.cuenta(),f.contexto().evaluacion()),aviso,exito));}
    private static int id(String s){return Validacion.entero(s,"evaluación o sugerencia",1,Integer.MAX_VALUE);}
    private static void destino(HttpExchange x,int id,String aviso)throws IOException{String ruta="/sugerencias/"+id+"?aviso="+URLEncoder.encode(aviso,StandardCharsets.UTF_8);if("text/plain".equals(x.getRequestHeaders().getFirst("Accept"))){x.getResponseHeaders().set("Location",ruta);Router.responder(x,201,"text/plain; charset=UTF-8",aviso);}else Router.redirigir(x,ruta);}
}
