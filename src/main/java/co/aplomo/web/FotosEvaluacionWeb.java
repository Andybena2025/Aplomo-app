package co.aplomo.web;

import co.aplomo.servicio.*;
import co.aplomo.seguridad.*;
import co.aplomo.seguridad.Sesiones.Sesion;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Administración de fotos activas; nunca borra ni altera el cálculo RULA. */
final class FotosEvaluacionWeb {
 private FotosEvaluacionWeb(){}
 static void cambiar(HttpExchange x,Sesion s,FotosEvaluacion servicio,int evaluacion)throws IOException,SQLException {
  Permisos.trabajo(s.cuenta());var partes=Multipart.leer(x.getRequestBody(),x.getRequestHeaders().getFirst("Content-Type"));Map<String,String> f=new HashMap<>();var archivos=new ArrayList<Multipart.Parte>();
  for(var p:partes)if(p.archivo()==null)f.put(p.nombre(),new String(p.bytes(),StandardCharsets.UTF_8));else if(!p.archivo().isEmpty()||p.bytes().length>0)archivos.add(p);
  if(!Sesiones.csrfValido(s,f.get("csrf"))){Router.responder(x,403,"text/plain; charset=UTF-8","El formulario venció. Abre otra pestaña, inicia sesión y vuelve a intentarlo.");return;}
  if(archivos.size()>1)throw new IllegalArgumentException("Elige una foto a la vez.");
  Integer anterior=f.containsKey("anterior")?Validacion.entero(f.get("anterior"),"foto anterior",1,Integer.MAX_VALUE):null;
  Fotos.Entrada entrada=archivos.isEmpty()?null:new Fotos.Entrada(archivos.get(0).bytes(),f.get("vista"),f.get("descripcion"));
  try{var cambio=servicio.cambiar(s.cuenta(),evaluacion,anterior,entrada);String json="{\"foto\":"+cambio.foto()+",\"anterior\":"+cambio.anterior()+"}";Router.responder(x,200,"application/json; charset=UTF-8",json);}
  catch(IllegalArgumentException e){Router.responder(x,400,"text/plain; charset=UTF-8",e.getMessage());}
  catch(SQLException|IOException e){Router.responder(x,503,"text/plain; charset=UTF-8","No pudimos cambiar la foto. La anterior se conserva; intenta de nuevo.");}
 }
}
