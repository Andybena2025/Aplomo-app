package co.aplomo.web;

import co.aplomo.dao.PendienteDao.*;import co.aplomo.seguridad.Sesiones.Sesion;import co.aplomo.modelo.Cuenta.Rol;
import java.util.*;import static co.aplomo.web.Html.escapar;import static co.aplomo.web.PaginasEmpresas.enlace;

/** RF-022, RNF-002/007: tablas, tarjetas, etiquetas y formularios del mock adaptados a pendientes. */
public final class PaginasPendientes {
    private PaginasPendientes() {}
    /** Lista ordenable y filtrable; el Usuario recibe solo filas autorizadas y no ve controles de cambio. */
    public static String lista(Sesion s,List<Ficha> lista,String estado,String orden,String aviso){
        StringBuilder h=new StringBuilder(Html.aviso(aviso,true));
        h.append("<p class=\"muted\">Los resultados rojos generan un pendiente por evaluación. Los anteriores se conservan.</p><form class=\"row filtros-pendientes\" method=\"get\" action=\"/pendientes\">")
                .append(Formularios.selector(Map.of("estado",estado),"estado","Mostrar",List.of(new Formularios.Opcion("ABIERTO","Abiertos"),new Formularios.Opcion("ARREGLADO","Arreglados"),new Formularios.Opcion("TODOS","Todos")),true))
                .append(Formularios.selector(Map.of("orden",orden),"orden","Ordenar por",List.of(new Formularios.Opcion("prioridad","Prioridad"),new Formularios.Opcion("fecha","Fecha de registro"),new Formularios.Opcion("limite","Fecha límite")),true))
                .append("<button class=\"btn\">Aplicar</button></form><p class=\"muted\">").append(Html.plural(lista.size(),"pendiente")).append("</p>");
        if(lista.isEmpty())h.append("<div class=\"empty\">No hay pendientes disponibles con este filtro.</div>");
        else{
            h.append("<div class=\"tablewrap\"><table class=\"resp pendientes-table\"><thead><tr><th>Puesto y ubicación</th><th>Resultado</th><th>Seguimiento</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>");
            for(var f:lista){var p=f.pendiente();h.append("<tr>");celda(h,"Puesto", "<b>Puesto "+f.numero()+"</b><br>"+escapar(f.empresaNombre())+"<div class=\"muted small\">"+escapar(f.sedeNombre())+" · "+escapar(f.salonNombre())+"</div>");
                celda(h,"Resultado","<span class=\"pill ROJO\">"+p.puntaje()+" · Rojo</span>");
                celda(h,"Seguimiento",escapar(p.prioridad()==null?"Sin prioridad":Formularios.etiqueta(p.prioridad()))+"<div class=\"small num\">Límite: "+(p.limite()==null?"Sin definir":Html.fecha(p.limite()))+"</div><div class=\"muted small num\">Registro: "+Html.fecha(p.registro())+"</div>");
                celda(h,"Estado",estado(p.estado()));celda(h,"Detalle",enlace("/pendientes/"+p.id(),"Ver pendiente",false));h.append("</tr>");
            }h.append("</tbody></table></div>");
        }
        return Html.pagina("Pendientes de arreglo",h.toString(),s);
    }
    /** Detalla el seguimiento y sus acciones históricas sin mostrar fotos. */
    public static String detalle(Sesion s,Ficha f,List<Cambio> cambios,String aviso,boolean exito){
        var p=f.pendiente();StringBuilder h=new StringBuilder(enlace("/pendientes","← Pendientes",false)).append(Html.aviso(aviso,exito));
        h.append("<section class=\"card stack\"><div class=\"row between\"><h2>Puesto ").append(f.numero()).append("</h2>").append(estado(p.estado())).append("</div><p>").append(escapar(f.empresaNombre())).append(" · ").append(escapar(f.sedeNombre())).append(" · ").append(escapar(f.salonNombre())).append("</p><span class=\"pill ROJO\">").append(p.puntaje()).append(" · Rojo</span><dl class=\"datos-cuenta\">")
                .append(dato("Qué hay que arreglar",p.descripcion()==null?"Sin descripción":p.descripcion())).append(dato("Prioridad",p.prioridad()==null?"Sin definir":Formularios.etiqueta(p.prioridad())))
                .append(dato("Fecha límite",p.limite()==null?"Sin definir":Html.fecha(p.limite()))).append(dato("Fecha de registro",Html.fecha(p.registro()))).append("</dl>")
                .append(enlace("/visitas/"+f.visita(),"Ver visita",false));
        if(admin(s)){
            h.append(enlace("/pendientes/"+p.id()+"/editar","Editar seguimiento",true));
            if(p.estado().equals("ABIERTO"))h.append("<form method=\"post\" action=\"/pendientes/").append(p.id()).append("/arreglado\">").append(Html.csrf(s)).append("<p class=\"hint\">Márcalo solo cuando la empresa haya corregido el puesto. Esto no cambia el puntaje RULA guardado.</p><button class=\"btn\">Marcar como arreglado</button></form>");
        }
        h.append("</section><section class=\"stack\"><h2>Historial</h2>");
        if(cambios.isEmpty())h.append("<p class=\"muted\">Sin acciones registradas.</p>");
        for(var c:cambios)h.append("<div class=\"card stack\"><span class=\"eyebrow\">").append(Html.fechaHora(c.fecha())).append("</span><b>").append(escapar(c.responsable())).append("</b><span>").append(escapar(Formularios.etiqueta(c.accion()))).append(c.nuevo()==null?"":" · "+escapar(Formularios.etiqueta(c.nuevo()))).append("</span><p class=\"small\">").append(escapar(c.detalle())).append("</p></div>");
        return Html.pagina("Pendiente de arreglo",h.append("</section>").toString(),s);
    }
    /** Conserva todos los campos cuando falla validación o escritura. */
    public static String editar(Sesion s,Ficha f,Map<String,String> enviados,String error){
        var p=f.pendiente();Map<String,String>d=new HashMap<>();d.put("descripcion",p.descripcion()==null?"":p.descripcion());d.put("prioridad",p.prioridad()==null?"":p.prioridad());d.put("fecha",p.limite()==null?"":p.limite().toString());d.putAll(enviados);
        String h=enlace("/pendientes/"+p.id(),"← Volver al pendiente",false)+Html.aviso(error,false)+"<form data-pendiente-form class=\"card stack cuenta-form\" method=\"post\" action=\"/pendientes/"+p.id()+"\"><h2>Seguimiento · Puesto "+f.numero()+"</h2><p class=\"muted small\">Completa los datos de seguimiento. El resultado y el estado se conservan.</p>"+Html.csrf(s)
                +Formularios.texto(d,"descripcion","Qué hay que arreglar",255,false)+"<span class=\"hint\">Máximo 255 caracteres.</span><div class=\"grid2\">"
                +Formularios.selector(d,"prioridad","Prioridad",List.of(new Formularios.Opcion("","Sin definir"),new Formularios.Opcion("ALTA","Alta"),new Formularios.Opcion("MEDIA","Media"),new Formularios.Opcion("BAJA","Baja")),false)
                +Formularios.campo(d,"fecha","Fecha límite (opcional)","date",10,false)+"</div><div class=\"aviso\" role=\"alert\" data-error-pendiente hidden><span></span><button type=\"button\" class=\"link\" data-cerrar-error aria-label=\"Cerrar aviso\">Cerrar</button></div><button class=\"btn p\" type=\"submit\">Guardar seguimiento</button></form><script src=\"/static/js/pendientes.js\" defer></script>";
        return Html.pagina("Editar seguimiento",h,s);
    }
    /** Nombre de seguimiento derivado del color ya calculado; no altera ni calcula RULA. */
    public static String clasificacion(String color){return switch(color){case "VERDE"->"Aceptable";case "AMARILLO"->"En observación";default->"Requiere arreglo";};}
    private static boolean admin(Sesion s){return s.cuenta().rol()==Rol.ADMINISTRADOR;}
    private static String estado(String s){return "<span class=\"tag\">"+(s.equals("ARREGLADO")?"Arreglado":"Abierto")+"</span>";}
    private static String dato(String nombre,String valor){return "<div><dt>"+escapar(nombre)+"</dt><dd class=\"texto-notas\">"+escapar(valor)+"</dd></div>";}
    private static void celda(StringBuilder h,String nombre,String html){h.append("<td data-label=\"").append(nombre).append("\"><div class=\"cell-value\">").append(html).append("</div></td>");}
}
