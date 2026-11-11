package co.aplomo.web;

import co.aplomo.modelo.*;
import co.aplomo.servicio.Calificaciones;
import co.aplomo.modelo.InformeEmpresa.*;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.math.*;
import java.util.*;
import static co.aplomo.web.Html.escapar;
import static co.aplomo.web.PaginasEmpresas.enlace;

/** RF-025/026: indicadores y hoja del mock, con nivel escrito y sin depender del color al imprimir. */
public final class PaginasCalificaciones {
    private PaginasCalificaciones(){}
    /** Muestra una selección explícita de empresa al Administrador. */
    public static String elegir(Sesion s,List<Empresa> empresas){
        var opciones=empresas.stream().map(e->new Formularios.Opcion(""+e.id(),e.nombre())).toList();
        String contenido="<form class=\"card stack cuenta-form\" method=\"get\" action=\"/calificaciones\">"
                +Formularios.selector(Map.of(),"empresa","Empresa",opciones,true)+"<button class=\"btn p\">Ver calificaciones</button></form>";
        return Html.pagina("Calificaciones",contenido,s);
    }
    /** Consulta de calificaciones de sede y empresa para ambos roles autorizados. */
    public static String calificaciones(Sesion s,InformeEmpresa i){
        StringBuilder h=new StringBuilder(encabezado(s,i,false));
        h.append("<section class=\"card stack\"><span class=\"eyebrow\">Calificación general</span>").append(general(i)).append(cobertura(i))
                .append("<p class=\"hint\">").append(metodo()).append("</p></section><section class=\"stack calificaciones-sedes\"><h2>Calificaciones por sede</h2><div class=\"grid2\">");
        for(var sede:i.sedes()){
            h.append("<article class=\"card stack\"><div class=\"row between\"><h3>").append(escapar(sede.sede().nombre()))
                    .append("</h3><span class=\"tag\">").append(sede.sede().activo()?"Activa":"Inactiva").append("</span></div><p class=\"muted small\">")
                    .append(escapar(sede.sede().ciudad())).append(" · ").append(escapar(sede.sede().direccion())).append("</p>")
                    .append(nota(sede.calificacion())).append("<p>").append(sede.visitas().size()).append(" visitas Finalizadas · ")
                    .append(sede.visitas().stream().filter(v->v.calificacion()!=null).count()).append(" con calificación</p>")
                    .append(enlace("/sedes/"+sede.sede().id(),"Ver sede",false)).append("</article>");
        }
        if(i.sedes().isEmpty())h.append("<div class=\"empty\">Esta empresa aún no tiene sedes. Registra su estructura antes de evaluar.</div>");
        return Html.pagina("Calificaciones",h.append("</div></section>").toString(),s,false);
    }
    /** Informe general: hoja clara del mock, sin bibliotecas PDF ni fotografías privadas. */
    public static String informe(Sesion s,InformeEmpresa i){
        StringBuilder h=new StringBuilder(encabezado(s,i,true));
        h.append("<p class=\"hint informe-ayuda\">Pulsa Imprimir o guardar PDF. En el diálogo del navegador elige tu impresora o Guardar como PDF.</p>")
                .append("<div class=\"sheetwrap informe-wrap\"><article class=\"sheet informe-empresa\"><header class=\"informe-marca\"><b>APLOMO.</b><div>Informe general de la empresa<br>")
                .append(Html.fechaHora(i.generado())).append(" · America/Bogota</div></header><h1>").append(escapar(i.empresa().nombre()))
                .append("</h1><p>NIT ").append(escapar(i.empresa().nit())).append("</p><p>").append(escapar(i.empresa().descripcion())).append("</p>")
                .append("<p>Contacto: ").append(texto(i.empresa().contacto(),"Sin registrar")).append(" · ").append(texto(i.empresa().telefono(),"Sin teléfono"))
                .append(" · ").append(texto(i.empresa().correo(),"Sin correo")).append("</p><p class=\"informe-metodo\">").append(metodo()).append("</p>");
        for(var sede:i.sedes())sede(h,sede);
        h.append("<section class=\"box informe-general\"><h2>Calificación general entre sedes</h2>").append(general(i)).append(cobertura(i)).append("</section>");
        h.append("<section class=\"informe-leyenda\"><h2>Cómo leer el riesgo</h2><p>El nivel y la acción están escritos para que el informe se entienda en blanco y negro.</p><ul>");
        for(var r:i.leyenda())h.append("<li>").append(r.minimo()).append(r.minimo()==r.maximo()?"":"–"+r.maximo()).append(" · ")
                .append(escapar(Formularios.etiqueta(r.color()))).append(" · Nivel ").append(r.nivel()).append(" · ").append(escapar(r.nombre())).append(": ").append(escapar(r.accion())).append("</li>");
        h.append("</ul></section><footer class=\"foot\">APLOMO. · Ergonomía con precisión · Método RULA (McAtamney y Corlett, 1993)<br>Generado el ")
                .append(Html.fechaHora(i.generado())).append(" · Resultados de visitas Finalizadas.</footer></article></div><script src=\"/static/js/informe.js\" defer></script>");
        return Html.pagina("Informe general",h.toString(),s,false);
    }
    private static String encabezado(Sesion s,InformeEmpresa i,boolean informe){
        return "<div class=\"informe-controles\">"+enlace("/empresas/"+i.empresa().id(),"← Empresa",false)
                +"<div class=\"pagehead\"><div><span class=\"eyebrow\">"+(informe?"Vista previa":"Indicadores")+"</span><h1>"+(informe?"Informe general":"Calificaciones")
                +"</h1><p class=\"muted\">"+escapar(i.empresa().nombre())+" · Solo visitas Finalizadas</p></div>"
                +(informe?"<button class=\"btn p\" type=\"button\" data-imprimir>Imprimir o guardar PDF</button>":s.cuenta().rol()==Cuenta.Rol.ADMINISTRADOR?enlace("/informes/empresa/"+i.empresa().id(),"Generar informe",true):"")
                +"</div></div>";
    }
    private static String metodo(){return "Indicadores del equipo (propuesta), no parte del método RULA; pendientes de validar con la clienta. Puesto: máximo de su última evaluación Finalizada. Visita: promedio guardado de sus puestos. Sede: promedio de visitas con calificación. General: promedio de las sedes calificadas, mínimo "+Calificaciones.MINIMO_SEDES_GENERAL+". Para el color se redondea al entero más cercano (propuesta).";}
    private static String general(InformeEmpresa i){
        if(i.sedes().size()==1)return "<p><b>No hay calificación general: la empresa tiene una sola sede.</b></p>";
        return i.general()==null?"<p><b>Sin calificación general.</b> Se necesitan al menos "+Calificaciones.MINIMO_SEDES_GENERAL+" sedes calificadas.</p>":nota(i.general());
    }
    private static String cobertura(InformeEmpresa i){
        int n=i.sedes().size()-i.sedesSinCalificacion().size();
        return "<p class=\"cobertura-sedes\">"+n+" de "+i.sedes().size()+" sedes calificadas · "+(i.sedesSinCalificacion().isEmpty()?"no faltan sedes por calificar":""+(i.sedesSinCalificacion().size()==1?"falta ":"faltan ")+String.join(", ",i.sedesSinCalificacion().stream().map(Html::escapar).toList()))+".</p>";
    }
    private static String nota(Calificacion c){
        if(c==null)return "<p class=\"sin-calificacion\">Sin calificación: no hay visitas Finalizadas con puntaje.</p>";
        return "<div class=\"nota-promedio\"><b class=\"promedio\">"+decimal(c.promedio())+"</b><span>Promedio</span>"+Html.riesgo(c.redondeado(),c.riesgo().color(),c.riesgo().nivel())
                +"<span class=\"small\">Clasificación con promedio redondeado · "+escapar(c.riesgo().accion())+"</span></div>";
    }
    private static void sede(StringBuilder h,SedeResumen s){
        h.append("<section class=\"informe-sede\"><h2>").append(escapar(s.sede().nombre())).append(s.sede().activo()?"":" · Inactiva")
                .append("</h2><p>").append(escapar(s.sede().ciudad())).append(" · ").append(escapar(s.sede().direccion())).append("</p>").append(nota(s.calificacion()));
        var e=s.estadisticas();h.append("<div class=\"informe-cifras\">");
        cifra(h,"Visitas Finalizadas",e.visitas());cifra(h,"Puestos evaluados únicos",e.puestos());cifra(h,"Puestos con puntaje",e.conPuntaje());cifra(h,"Fotos analizadas",e.fotosAnalizadas());
        for(String color:List.of("VERDE","AMARILLO","ROJO"))cifra(h,"Puestos en "+Formularios.etiqueta(color),e.colores().getOrDefault(color,0));
        cifra(h,"Puestos sin puntaje",e.puestos()-e.conPuntaje());cifra(h,"Pendientes abiertos",e.pendientes().getOrDefault("ABIERTO",0));cifra(h,"Pendientes arreglados",e.pendientes().getOrDefault("ARREGLADO",0));h.append("</div>");
        h.append("<div class=\"informe-bloque\"><h3>Visitas y calificaciones históricas</h3>");
        tabla(h,List.of("Reserva","Inicio y fin reales","Calificación"),s.visitas().stream().map(v->List.of(escapar(v.codigo()),Html.fechaHora(v.inicio())+"<br>"+Html.fechaHora(v.fin()),nota(v.calificacion()))).toList());
        h.append("</div><div class=\"informe-bloque\"><h3>Puestos y resultados actuales</h3><p>Una fila por puesto: última evaluación Finalizada; máximo de sus análisis. Una reevaluación Verde sustituye al Rojo anterior en estas cifras.</p>");
        tabla(h,List.of("Puesto y ubicación","Área y cargo","Postura y fecha","Resultado y acción"),s.puestos().stream().map(p->List.of(
                "<b>Puesto "+p.numero()+"</b><br>"+escapar(p.salon())+"<br>"+escapar(p.descripcion()),texto(p.area(),"Sin área")+"<br>"+texto(p.cargo(),"Sin cargo")+(p.tipoCargo()==null?"":"<br>"+etiqueta(p.tipoCargo())),
                escapar(p.postura())+"<br>"+Html.fecha(p.fecha()),riesgo(p.puntaje(),p.riesgo())+(p.riesgo()==null?"":"<br>"+escapar(p.riesgo().nombre())+" · "+escapar(p.riesgo().accion())))).toList());
        h.append("</div><h3>Peligros y aspectos a mejorar observados</h3>");
        if(s.puestos().isEmpty())h.append("<p>No hay evaluaciones en visitas Finalizadas.</p>");
        for(var p:s.puestos())h.append("<div class=\"box informe-observacion\"><h4>Puesto ").append(p.numero()).append(" · ").append(escapar(p.salon()))
                .append("</h4><p><b>Postura:</b> ").append(escapar(p.postura())).append("</p><p><b>Motivo de elección:</b> ").append(escapar(p.motivo()))
                .append("</p><p>").append(escapar(p.condiciones())).append("</p>")
                .append(p.riesgo()==null?"<p>Sin análisis guardado; no se asigna riesgo.</p>":"<p>"+riesgo(p.puntaje(),p.riesgo())+" · "+escapar(p.riesgo().accion())+"</p>").append("</div>");
        h.append("<div class=\"informe-bloque\"><h3>Distribución por área y tipo de cargo</h3>");conteos(h,"Puestos por área",e.areas());conteos(h,"Puestos por tipo de cargo",e.cargos());
        h.append("<p><b>Área con más puestos en Rojo:</b> ").append(e.areasMasRojo().isEmpty()?"No hay puestos en Rojo.":String.join(", ",e.areasMasRojo().stream().map(Html::escapar).toList())+" (se muestran todas si hay empate).").append("</p>");
        h.append("</div><div class=\"informe-bloque\"><h3>Pendientes de arreglo</h3>");
        tabla(h,List.of("Puesto","Resultado original","Estado y seguimiento"),s.pendientes().stream().map(p->List.of("Puesto "+p.numero()+" · "+escapar(p.salon()),riesgo(p.puntaje(),p.riesgo()),
                etiqueta(p.estado())+" · "+etiqueta(p.prioridad())+"<br>"+escapar(p.descripcion())+"<br>Límite: "+fecha(p.limite()))).toList());
        h.append("</div><div class=\"informe-bloque\"><h3>Sugerencias activas por tipo y estado</h3>");List<List<String>> filas=new ArrayList<>();
        for(var tipo:new TreeMap<>(e.sugerencias()).entrySet())for(var estado:new TreeMap<>(tipo.getValue()).entrySet())filas.add(List.of(etiqueta(tipo.getKey()),etiqueta(estado.getKey()),""+estado.getValue()));
        tabla(h,List.of("Tipo","Estado","Cantidad"),filas);h.append("</div>");
        for(var sg:s.sugerencias())h.append("<div class=\"box informe-sugerencia\"><h4>Puesto ").append(sg.numero()).append(" · ").append(escapar(sg.salon())).append(" · ").append(escapar(sg.titulo()))
                .append("</h4><p>").append(escapar(sg.descripcion())).append("</p><p>").append(etiqueta(sg.tipo())).append(" · ").append(etiqueta(sg.estado())).append(" · Prioridad ").append(etiqueta(sg.prioridad()))
                .append(" · Límite: ").append(fecha(sg.limite())).append(" · ").append(sg.origen().equals("SISTEMA")?"Sugerencia del sistema":"Escrita por el Administrador").append("</p></div>");
        if(s.sugerencias().isEmpty())h.append("<p>No hay sugerencias activas de visitas Finalizadas.</p>");h.append("</section>");
    }
    private static void cifra(StringBuilder h,String titulo,int n){h.append("<div class=\"box\"><span class=\"sm\">").append(escapar(titulo)).append("</span><b>").append(n).append("</b></div>");}
    private static void conteos(StringBuilder h,String titulo,Map<String,Integer> mapa){h.append("<h4>").append(escapar(titulo)).append("</h4>");tabla(h,List.of("Grupo","Puestos"),new TreeMap<>(mapa).entrySet().stream().map(x->List.of(etiqueta(x.getKey()),""+x.getValue())).toList());}
    private static void tabla(StringBuilder h,List<String> cabeceras,List<List<String>> filas){
        if(filas.isEmpty()){h.append("<p>Sin registros.</p>");return;}
        h.append("<div class=\"informe-tabla\"><table class=\"resp\"><thead><tr>");cabeceras.forEach(t->h.append("<th scope=\"col\">").append(escapar(t)).append("</th>"));h.append("</tr></thead><tbody>");
        for(var fila:filas){h.append("<tr>");for(int n=0;n<fila.size();n++)h.append("<td data-label=\"").append(escapar(cabeceras.get(n))).append("\"><div class=\"cell-value\">").append(fila.get(n)).append("</div></td>");h.append("</tr>");}h.append("</tbody></table></div>");
    }
    private static String riesgo(Integer p,Riesgo r){return p==null?"Sin resultado":Html.riesgo(p,r.color(),r.nivel());}
    private static String texto(String v,String vacio){return escapar(v==null||v.isBlank()?vacio:v);}
    private static String etiqueta(String v){return v==null?"Sin cargo":escapar(Formularios.etiqueta(v));}
    private static String fecha(java.time.LocalDate d){return d==null?"Sin definir":Html.fecha(d);}
    private static String decimal(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.',',');}
}
