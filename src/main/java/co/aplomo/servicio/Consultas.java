package co.aplomo.servicio;
import co.aplomo.dao.*;import co.aplomo.dao.ConsultaDao.*;import co.aplomo.modelo.*;import co.aplomo.db.Transacciones;import co.aplomo.seguridad.*;
import java.sql.*;import java.time.*;import java.math.BigDecimal;import java.util.*;
/** RF-024: búsqueda, filtros combinables y ordenación en el servidor, después del alcance SQL autorizado. */
public final class Consultas {
    /** Resultado de una consulta sin alterar registros ni su historial. */
    public record Pagina(SeccionConsulta seccion,List<Fila> filas,Map<String,String> filtros,Map<String,List<Opcion>> catalogos,Map<String,List<String>> riesgos) {}
    /** Detalle de evaluación consultable por el Usuario sin fotos ni rutas de archivos. */
    public record Detalle(Fila evaluacion,List<AnalisisDao.Resumen> analisis) {}
    private final Transacciones tx;private final ConsultaDao dao=new ConsultaDao();
    /** Comparte la fuente transaccional con la aplicación y sus pruebas reversibles. */ public Consultas(Transacciones tx){this.tx=tx;}
    /** Consulta y filtra solo los datos permitidos para la cuenta vigente. */
    public Pagina listar(Cuenta actor,SeccionConsulta seccion,Map<String,String> peticion)throws SQLException{
        return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);int empresa=AccesoDatos.empresaPermitida(a);boolean usuario=a.rol()==Cuenta.Rol.USUARIO;var catalogos=dao.catalogos(c,empresa);var riesgos=dao.riesgos(c);Map<String,String> filtros=validar(seccion,peticion,catalogos,riesgos);
            if(filtros.containsKey("visita")&&new VisitaDao().buscar(c,Integer.parseInt(filtros.get("visita")),empresa,usuario,false)==null)throw new SecurityException("Acceso denegado. La visita no está disponible para tu cuenta.");
            if(!filtros.getOrDefault("evaluacion","").isEmpty())exigir(dao.filas(c,SeccionConsulta.EVALUACIONES,empresa,usuario,Integer.parseInt(filtros.get("evaluacion"))));
            var filas=dao.filas(c,seccion,empresa,usuario,0).stream().filter(f->coincide(f,filtros,seccion)).sorted(comparador(filtros)).toList();return new Pagina(seccion,filas,Map.copyOf(filtros),catalogos,riesgos);});
    }
    /** Abre únicamente una evaluación visible; una visita En curso o empresa ajena se deniega también por ID. */
    public Detalle evaluacion(Cuenta actor,int id)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);var f=exigir(dao.filas(c,SeccionConsulta.EVALUACIONES,AccesoDatos.empresaPermitida(a),a.rol()==Cuenta.Rol.USUARIO,id));return new Detalle(f,new AnalisisDao().listar(c,id));});}
    private static Fila exigir(List<Fila> filas){if(filas.isEmpty())throw new SecurityException("Acceso denegado. La evaluación no está disponible para tu cuenta.");return filas.get(0);}
    private static Map<String,String> validar(SeccionConsulta s,Map<String,String> d,Map<String,List<Opcion>> catalogos,Map<String,List<String>> riesgos){
        Map<String,String> f=new LinkedHashMap<>();f.put("q",Validacion.opcional(d.get("q"),"búsqueda",120));
        for(String key:catalogos.keySet()){String v=d.getOrDefault(key,"");if(!v.isBlank()&&!v.equals("0")){int id=Validacion.entero(v,key,1,Integer.MAX_VALUE);if(catalogos.get(key).stream().noneMatch(x->x.id()==id))throw new SecurityException("Acceso denegado. Ese filtro no está disponible para tu cuenta.");if(s.filtros().contains(key))f.put(key,Integer.toString(id));}}
        for(String key:List.of("numero","puntaje","nivel","evaluacion","visita")){String v=d.getOrDefault(key,"");if(!v.isBlank()){int max=key.equals("puntaje")?7:key.equals("nivel")?4:key.equals("numero")?32767:Integer.MAX_VALUE;int n=Validacion.entero(v,key,1,max);if(s.filtros().contains(key)||key.equals("evaluacion")||key.equals("visita"))f.put(key,Integer.toString(n));}}
        if(s.filtros().contains("color")&&!d.getOrDefault("color","").isBlank())f.put("color",Validacion.opcion(d.get("color"),"semáforo",riesgos.get("color").toArray(String[]::new)));
        for(String key:List.of("desde","hasta"))if(s.filtros().contains(key)&&!d.getOrDefault(key,"").isBlank()){String v=d.get(key);try{LocalDate dia=LocalDate.parse(v);if(dia.getYear()<1000||dia.getYear()>9999)throw new IllegalArgumentException();f.put(key,v);}catch(RuntimeException e){throw new IllegalArgumentException("Revisa la fecha "+(key.equals("desde")?"inicial":"final")+": escribe una fecha válida.");}}
        if(f.containsKey("desde")&&f.containsKey("hasta")&&f.get("desde").compareTo(f.get("hasta"))>0)throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        String estado=d.getOrDefault("estado",s==SeccionConsulta.PENDIENTES?"ABIERTO":"TODOS");if(!estado.isBlank()&&!estado.equals("TODOS"))f.put("estado",Validacion.opcion(estado,"estado",s.estados().toArray(String[]::new)));else f.put("estado","TODOS");
        List<String> ordenes=new ArrayList<>(s.columnas().stream().map(SeccionConsulta.Columna::clave).toList());if(s==SeccionConsulta.PENDIENTES)ordenes.addAll(List.of("prioridad","fecha","fecha_limite"));
        String defecto=s==SeccionConsulta.PENDIENTES?"prioridad":ordenes.contains("fecha")?"fecha":ordenes.contains("nombre")?"nombre":"numero";
        String orden=d.getOrDefault("orden",defecto);if(orden.equals("limite"))orden="fecha_limite";f.put("orden",Validacion.opcion(orden,"orden",ordenes.toArray(String[]::new)));f.put("direccion",Validacion.opcion(d.getOrDefault("direccion",orden.equals("fecha")?"desc":"asc"),"dirección","asc","desc"));return f;
    }
    private static boolean coincide(Fila r,Map<String,String> f,SeccionConsulta s){
        for(String k:s.filtros()){String v=f.getOrDefault(k,"");if(v.isEmpty()||v.equals("TODOS"))continue;if(k.equals("desde")||k.equals("hasta")){String fecha=r.valor("fecha");if(fecha.length()<10)return false;String dia=fecha.substring(0,10);if(k.equals("desde")&&dia.compareTo(v)<0||k.equals("hasta")&&dia.compareTo(v)>0)return false;}else if(!r.valor(k).equals(v))return false;}
        if(f.containsKey("evaluacion")&&!r.valor("evaluacion").equals(f.get("evaluacion")))return false;
        if(f.containsKey("visita")&&!r.valor("visita").equals(f.get("visita")))return false;
        String q=f.getOrDefault("q","");if(q.isEmpty())return true;String texto=String.join(" ",r.datos().values()).replace('_',' ');if(r.valor("origen").equals("SISTEMA"))texto+=" Sugerencia del sistema";return Validacion.nombre(texto).contains(Validacion.nombre(q));
    }
    private static Comparator<Fila> comparador(Map<String,String> f){String key=f.get("orden");boolean desc=f.get("direccion").equals("desc");return (a,b)->{String x=orden(a,key),y=orden(b,key);int n;if(x.isEmpty()||y.isEmpty())n=x.isEmpty()?(y.isEmpty()?0:1):-1;else{if(List.of("numero","puntaje","nivel","piso","capacidad","calificacion").contains(key))n=new BigDecimal(x).compareTo(new BigDecimal(y));else n=Validacion.nombre(x).compareTo(Validacion.nombre(y));if(desc)n=-n;}return n==0?Integer.compare(a.id(),b.id()):n;};}
    private static String orden(Fila r,String key){return switch(key){case "ubicacion"->r.valor("empresa_nombre")+" "+r.valor("sede_nombre")+" "+r.valor("salon_nombre");case "asignacion"->r.valor("area_nombre")+" "+r.valor("cargo_nombre");case "seguimiento","prioridad"->switch(r.valor("prioridad")){case "ALTA"->"1";case "MEDIA"->"2";case "BAJA"->"3";default->"";};default->r.valor(key);};}
}
