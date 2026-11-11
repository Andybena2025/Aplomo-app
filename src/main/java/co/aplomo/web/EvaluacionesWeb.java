package co.aplomo.web;

import co.aplomo.seguridad.*;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.*;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/** RF-014/015/016, RNF-006/007: formulario multipart y fotografías con permiso de Administrador. */
public final class EvaluacionesWeb {
    private final Evaluaciones servicio;
    /** Recibe el servicio transaccional, también para las pruebas HTTP aisladas. */
    public EvaluacionesWeb(Evaluaciones servicio) { this.servicio=servicio; }

    /** Comprueba permiso antes de leer archivos; los errores AJAX dejan intacto el formulario. */
    public void atender(HttpExchange x, Sesion s) throws IOException, SQLException {
        Permisos.administrador(s.cuenta());
        String ruta=x.getRequestURI().getPath();
        boolean post=x.getRequestMethod().equals("POST");
        if (ruta.startsWith("/fotos/")) {
            if (post || !ruta.matches("/fotos/[1-9][0-9]*")) { Router.enviar(x,404,"Foto no disponible."); return; }
            try {
                var contenido=servicio.foto(s.cuenta(), entero(ruta.substring(7),"foto"));
                x.getResponseHeaders().set("Content-Type",contenido.tipo());
                x.getResponseHeaders().set("Content-Disposition","inline");
                x.getResponseHeaders().set("Content-Security-Policy","default-src 'none'; sandbox");
                x.sendResponseHeaders(200,contenido.bytes().length);x.getResponseBody().write(contenido.bytes());
            } catch (IOException e) { Router.enviar(x,404,"Foto no disponible."); }
            return;
        }
        if (ruta.equals("/evaluaciones/nueva") && !post) {
            int visita=entero(Router.consulta(x).get("visita"),"visita");
            Router.enviar(x,200,PaginasEvaluacion.nueva(s,servicio.contexto(s.cuenta(),visita))); return;
        }
        if (!post && ruta.matches("/evaluaciones/[1-9][0-9]*")) {
            Router.enviar(x,200,PaginasEvaluacion.detalle(s,servicio.consultar(s.cuenta(),entero(ruta.substring(14),"evaluación")))); return;
        }
        if (!ruta.equals("/evaluaciones") || !post) { Router.enviar(x,404,"Página no disponible.");return; }
        try {
            var partes=Multipart.leer(x.getRequestBody(),x.getRequestHeaders().getFirst("Content-Type"));
            Map<String,String> f=new HashMap<>();
            for(var p:partes) if(p.archivo()==null) f.put(p.nombre(),new String(p.bytes(),StandardCharsets.UTF_8));
            if (!Sesiones.csrfValido(s,f.get("csrf"))) { responder(x,403,"El formulario venció. Abre otra pestaña, inicia sesión y vuelve aquí para reintentar sin perder lo escrito.");return; }
            int visita=entero(f.get("visita"),"visita");
            String registro=Validacion.opcion(f.get("registro"),"registro fotográfico","CON_FOTOS","SIN_REGISTRO_FOTOGRAFICO");
            var datos=new Evaluaciones.Datos(entero(f.get("puesto"),"puesto"),f.get("postura"),f.get("criterio"),f.get("motivo"),
                    Validacion.entero(f.get("minutos"),"minutos por jornada",1,720),si(f,"escritorio"),si(f,"pantalla"),si(f,"pies"),si(f,"apoyapies"),si(f,"apoyabrazos"),si(f,"regulable"),f.get("observaciones"),si(f,"acepta"),registro.equals("SIN_REGISTRO_FOTOGRAFICO"));
            List<Fotos.Entrada> fotos=new ArrayList<>();
            for(var p:partes) if(p.archivo()!=null && !(p.archivo().isEmpty() && p.bytes().length==0))
                fotos.add(new Fotos.Entrada(p.bytes(),f.get(p.nombre()+"_vista"),f.get(p.nombre()+"_descripcion")));
            servicio.guardar(s.cuenta(),visita,datos,fotos);
            x.getResponseHeaders().set("Location","/visitas/"+visita+"?aviso=Evaluaci%C3%B3n+guardada.");
            responder(x,201,"Evaluación guardada.");
        } catch(IllegalArgumentException e) { responder(x,400,e.getMessage()); }
        catch(SQLException | IOException e) { responder(x,503,"No pudimos guardar la evaluación. Tus campos y fotos siguen seleccionados; inténtalo de nuevo."); }
    }
    private static boolean si(Map<String,String> f,String n) { return "si".equals(f.get(n)); }
    private static int entero(String v,String n) { return Validacion.entero(v,n,1,Integer.MAX_VALUE); }
    private static void responder(HttpExchange x,int estado,String texto) throws IOException { Router.responder(x,estado,"text/plain; charset=UTF-8",texto); }
}
