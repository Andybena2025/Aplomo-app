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

    /** Comparte el servicio autorizado con el panel de Inicio. */
    public Evaluaciones servicio(){return servicio;}

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
        if(ruta.equals("/evaluaciones/calcular") && post) {
            try {
                var f=Router.formulario(x);
                if(!Sesiones.csrfValido(s,f.get("csrf"))){responder(x,403,"El formulario venció. Inicia sesión en otra pestaña y vuelve a intentarlo aquí.");return;}
                if(f.containsKey("segmento")) {
                    servicio.analisis().autorizarParcial(s.cuenta(),entero(f.get("visita"),"visita"));
                    Router.enviar(x,200,RecorridoCuerpo.parcial(f));return;
                }
                var calculo=servicio.analisis().previsualizar(s.cuenta(),entero(f.get("visita"),"visita"),DatosAnalisis.leer(f));
                Map<String,Boolean> condiciones=Map.of("escritorio_muy_alto",si(f,"escritorio"),"pantalla_muy_alta",si(f,"pantalla"),"pies_colgando",si(f,"pies"),"tiene_apoyapies",si(f,"apoyapies"),"silla_con_apoyabrazos",si(f,"apoyabrazos"),"silla_regulable",si(f,"regulable"));
                var propuestas=servicio.sugerencias().previsualizar(s.cuenta(),calculo,condiciones);String resultado=PaginasAnalisis.resultado(calculo);StringBuilder html=new StringBuilder(resultado.substring(0,resultado.length()-6)).append("<div class=\"stack\"><h3>Sugerencias · aparte del puntaje</h3>");
                for(var regla:propuestas)html.append("<div class=\"sug\">").append(Html.escapar(regla.texto())).append("</div>");
                if(propuestas.isEmpty())html.append("<p class=\"hint\">No hay reglas que apliquen. El análisis se puede guardar; si es Amarillo o Rojo, agrega después al menos una sugerencia.</p>");
                Router.enviar(x,200,html.append("</div></div>").toString());
            }catch(IllegalArgumentException e){responder(x,400,e.getMessage());}
            catch(SQLException e){responder(x,503,"No pudimos calcular. Revisa la conexión e inténtalo de nuevo; tus puntos se conservan.");}
            return;
        }
        if(ruta.matches("/evaluaciones/[1-9][0-9]*/analisis/[1-9][0-9]*")&&!post) {
            String[] ids=ruta.split("/");var guardado=servicio.analisis().consultar(s.cuenta(),entero(ids[2],"evaluación"),entero(ids[4],"análisis"));
            var c=servicio.sugerencias().contexto(s.cuenta(),guardado.ficha().evaluacion()); var lista=servicio.sugerencias().listar(s.cuenta(),c.evaluacion());
            Router.enviar(x,200,PaginasAnalisis.guardado(s,guardado).replace("</main>",PaginasSugerencias.resumen(lista,c)+"</main>"));return;
        }
        if(ruta.matches("/evaluaciones/[1-9][0-9]*/analizar")&&!post) {
            int id=entero(ruta.split("/")[2],"evaluación");var consulta=servicio.consultar(s.cuenta(),id);
            servicio.contexto(s.cuenta(),consulta.ficha().visita());
            Router.enviar(x,200,PaginasAnalisis.continuar(s,consulta));return;
        }
        if(ruta.matches("/evaluaciones/[1-9][0-9]*/analisis")&&post) {
            try {
                var f=Router.formulario(x);if(!Sesiones.csrfValido(s,f.get("csrf"))){responder(x,403,"El formulario venció. Inicia sesión en otra pestaña y vuelve a intentarlo aquí.");return;}
                int id=entero(ruta.split("/")[2],"evaluación");validarDestino(f);var consulta=servicio.consultar(s.cuenta(),id);servicio.analisis().guardar(s.cuenta(),id,DatosAnalisis.solicitudes(f));
                x.getResponseHeaders().set("Location",destino(s,consulta.ficha().visita(),id,f,"Análisis guardado."));responder(x,201,"Análisis guardado.");
            }catch(IllegalArgumentException e){responder(x,400,e.getMessage());}
            catch(SQLException|IOException e){responder(x,503,"No pudimos guardar. Tus ángulos y ajustes se conservan; inténtalo de nuevo.");}
            return;
        }
        if (ruta.equals("/evaluaciones/nueva") && !post) {
            int visita=entero(Router.consulta(x).get("visita"),"visita");
            var consulta=Router.consulta(x);
            int puesto=consulta.containsKey("puesto")?entero(consulta.get("puesto"),"puesto"):0;
            Router.enviar(x,200,PaginasEvaluacion.nueva(s,servicio.contexto(s.cuenta(),visita),puesto)); return;
        }
        if (!post && ruta.matches("/evaluaciones/[1-9][0-9]*")) {
            int id=entero(ruta.substring(14),"evaluación");var analisis=servicio.analisis().listar(s.cuenta(),id);String pagina=PaginasEvaluacion.detalle(s,servicio.consultar(s.cuenta(),id),analisis,Router.consulta(x).get("aviso"));
            if(!analisis.isEmpty())pagina=pagina.replace("</main>",PaginasSugerencias.resumen(servicio.sugerencias().listar(s.cuenta(),id),servicio.sugerencias().contexto(s.cuenta(),id))+"</main>");Router.enviar(x,200,pagina);return;
        }
        if (!ruta.equals("/evaluaciones") || !post) { Router.enviar(x,404,"Página no disponible.");return; }
        try {
            var partes=Multipart.leer(x.getRequestBody(),x.getRequestHeaders().getFirst("Content-Type"));
            Map<String,String> f=new HashMap<>();
            for(var p:partes) if(p.archivo()==null) f.put(p.nombre(),new String(p.bytes(),StandardCharsets.UTF_8));
            if (!Sesiones.csrfValido(s,f.get("csrf"))) { responder(x,403,"El formulario venció. Abre otra pestaña, inicia sesión y vuelve aquí para reintentar sin perder lo escrito.");return; }
            int visita=entero(f.get("visita"),"visita");
            String registro=Validacion.opcion(f.get("registro"),"registro fotográfico","CON_FOTOS","SIN_REGISTRO_FOTOGRAFICO");
            if("guiado".equals(f.get("recorrido"))) {
                Validacion.opcion(f.get("minutos"),"el tiempo de la jornada","30","90","180","300");
                Validacion.opcion(f.get("condiciones_respondidas"),"las condiciones del puesto","NINGUNA","SELECCION");
                boolean alguna=List.of("escritorio","pantalla","pies","apoyapies","apoyabrazos","regulable").stream().anyMatch(k->si(f,k));
                if(alguna != "SELECCION".equals(f.get("condiciones_respondidas"))) throw new IllegalArgumentException("Revisa las condiciones del puesto o elige Ninguna.");
            }
            var datos=new Evaluaciones.Datos(entero(f.get("puesto"),"puesto"),f.get("postura"),f.get("criterio"),f.get("motivo"),
                    Validacion.entero(f.get("minutos"),"minutos por jornada",1,720),si(f,"escritorio"),si(f,"pantalla"),si(f,"pies"),si(f,"apoyapies"),si(f,"apoyabrazos"),si(f,"regulable"),f.get("observaciones"),si(f,"acepta"),registro.equals("SIN_REGISTRO_FOTOGRAFICO"));
            List<Fotos.Entrada> fotos=new ArrayList<>();
            for(var p:partes) if(p.archivo()!=null && !(p.archivo().isEmpty() && p.bytes().length==0))
                fotos.add(new Fotos.Entrada(p.bytes(),f.get(p.nombre()+"_vista"),f.get(p.nombre()+"_descripcion")));
            if("guiado".equals(f.get("recorrido"))&&registro.equals("CON_FOTOS")&&si(f,"acepta")&&fotos.stream().noneMatch(foto->"PERFIL".equals(foto.vista())))throw new IllegalArgumentException("Sube una foto de perfil para continuar.");
            validarDestino(f);
            int id=servicio.guardar(s.cuenta(),visita,datos,fotos,"si".equals(f.get("asistente_completo"))?DatosAnalisis.solicitudes(f):null);
            x.getResponseHeaders().set("Location",destino(s,visita,id,f,"Evaluación guardada."));
            responder(x,201,"Evaluación guardada.");
        } catch(IllegalArgumentException e) { responder(x,400,e.getMessage()); }
        catch(SQLException | IOException e) { responder(x,503,"No pudimos guardar la evaluación. Tus campos y fotos siguen seleccionados; inténtalo de nuevo."); }
    }
    private static void validarDestino(Map<String,String> f){if(f.containsKey("destino_guardado"))Validacion.opcion(f.get("destino_guardado"),"el destino al guardar","SIGUIENTE","VOLVER");}
    private String destino(Sesion s,int visita,int evaluacion,Map<String,String> f,String aviso)throws SQLException {
        if(!f.containsKey("destino_guardado"))return "/evaluaciones/"+evaluacion+"?aviso="+java.net.URLEncoder.encode(aviso,StandardCharsets.UTF_8);
        if("SIGUIENTE".equals(f.get("destino_guardado"))){try{int puesto=servicio.siguientePendiente(s.cuenta(),visita);if(puesto!=0)return "/evaluaciones/nueva?visita="+visita+"&puesto="+puesto;aviso="Todos los puestos están evaluados";}catch(SQLException|IllegalArgumentException fallo){/* Ya se guardó: volver a la visita evita invitar a duplicar el envío. */}}
        return "/visitas/"+visita+"?aviso="+java.net.URLEncoder.encode(aviso,StandardCharsets.UTF_8);
    }
    private static boolean si(Map<String,String> f,String n) { return "si".equals(f.get(n)); }
    private static int entero(String v,String n) { return Validacion.entero(v,n,1,Integer.MAX_VALUE); }
    private static void responder(HttpExchange x,int estado,String texto) throws IOException { Router.responder(x,estado,"text/plain; charset=UTF-8",texto); }
}
