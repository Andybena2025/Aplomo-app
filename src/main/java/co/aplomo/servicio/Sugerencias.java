package co.aplomo.servicio;
import co.aplomo.dao.*;import co.aplomo.dao.SugerenciaDao.*;import co.aplomo.db.*;import co.aplomo.modelo.*;import co.aplomo.rula.MotorRula;import co.aplomo.seguridad.*;
import java.sql.*;import java.time.*;import java.util.*;
/** RF-023a/b: propuestas de MySQL y gestión autorizada, sin duplicar reglas ni borrar historia. */
public final class Sugerencias {
    /** (propuesta) Andy, 11 nov 2026: correspondencia fija; nunca se adivina el tipo a partir del texto. */
    public static final Map<String,String> TIPOS_POR_CONDICION=Map.of("SIEMPRE","POSTURA","escritorio_muy_alto","MOBILIARIO","pantalla_muy_alta","MOBILIARIO","pies_colgando","MOBILIARIO","tiene_apoyapies","MOBILIARIO","silla_con_apoyabrazos","MOBILIARIO","silla_regulable","MOBILIARIO");
    private final Transacciones tx;private final Clock reloj;private final BitacoraDao bitacora;private final SugerenciaDao dao=new SugerenciaDao();
    /** Comparte transacciones, auditoría y hora de Colombia (RNF-005/011). */
    public Sugerencias(Transacciones tx,Clock reloj,BitacoraDao bitacora){this.tx=tx;this.reloj=reloj;this.bitacora=bitacora;}
    /** Lista únicamente sugerencias activas autorizadas (RF-002). */
    public List<Ficha> listar(Cuenta actor,int evaluacion)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);if(evaluacion>0)visible(c,a,evaluacion,false);return dao.listar(c,AccesoDatos.empresaPermitida(a),a.rol()==Cuenta.Rol.USUARIO,evaluacion);});}
    /** Consulta una evaluación con análisis, sin entregar fotos al Usuario. */
    public Contexto contexto(Cuenta actor,int id)throws SQLException{return tx.ejecutar(c->visible(c,AccesoDatos.vigente(c,actor),id,false));}
    /** Detalla sugerencias quitadas solo para Administrador; la empresa conserva consulta de activas. */
    public Ficha consultar(Cuenta actor,int id)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);var s=exigir(dao.buscar(c,id));var e=visible(c,a,s.evaluacion(),false);if(!s.activo()&&a.rol()!=Cuenta.Rol.ADMINISTRADOR)throw new SecurityException("Acceso denegado. La sugerencia no está disponible.");return new Ficha(s,e);});}
    /** Muestra fecha, responsable y cambios sin perder el historial de una sugerencia quitada. */
    public List<PendienteDao.Cambio> historial(Cuenta actor,int id)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);var s=exigir(dao.buscar(c,id));visible(c,a,s.evaluacion(),false);if(!s.activo()&&a.rol()!=Cuenta.Rol.ADMINISTRADOR)throw new SecurityException("Acceso denegado.");return Sql.listar(c,"SELECT b.*,q.nombre responsable FROM bitacora b JOIN cuenta q USING(id_cuenta) WHERE b.tabla='sugerencia' AND b.id_registro=? ORDER BY b.id_bitacora DESC",r->new PendienteDao.Cambio(r.getTimestamp("fecha_hora").toLocalDateTime(),r.getString("responsable"),r.getString("accion"),r.getString("estado_anterior"),r.getString("estado_nuevo"),r.getString("detalle")),id);});}
    /** Crea o edita título, descripción, tipo, prioridad y fecha; no acepta origen ni regla desde el navegador. */
    public int guardar(Cuenta actor,int evaluacion,int id,String titulo,String descripcion,String tipo,String prioridad,String fecha)throws SQLException{
        return tx.ejecutar(c->{var a=admin(c,actor);visible(c,a,evaluacion,true);var anterior=id==0?null:editable(c,id,evaluacion);
            String t=Validacion.texto(titulo,"título",120),d=Validacion.notas(descripcion);if(d.isEmpty())throw new IllegalArgumentException("Completa la descripción de la sugerencia.");
            String tip=Validacion.opcion(tipo,"tipo","MOBILIARIO","POSTURA","DESCANSOS","CARGA","OTRA"),p=Validacion.opcion(prioridad,"prioridad","ALTA","MEDIA","BAJA");
            var nueva=new Sugerencia(id,evaluacion,anterior==null?null:anterior.regla(),t,d,tip,p,anterior==null?"PENDIENTE":anterior.estado(),fecha(fecha),anterior==null?"ADMINISTRADOR":anterior.origen(),true);
            int guardado;if(id==0){guardado=dao.crear(c,nueva);auditar(c,a,guardado,"CREAR",null,"PENDIENTE","Sugerencia escrita por el Administrador.");}else{guardado=id;if(!nueva.equals(anterior)){dao.editar(c,nueva);auditar(c,a,id,"EDITAR",null,null,"Se editaron los datos de la sugerencia; su origen y estado se conservan.");}}return guardado;});
    }
    /** Cambia entre los tres estados aprobados; repetir el mismo estado no duplica auditoría. */
    public void estado(Cuenta actor,int id,String estado)throws SQLException{tx.ejecutar(c->{var a=admin(c,actor);var primera=exigir(dao.buscar(c,id));visible(c,a,primera.evaluacion(),true);var s=editable(c,id,primera.evaluacion());String e=Validacion.opcion(estado,"estado","PENDIENTE","EN_PROCESO","HECHA");if(!s.estado().equals(e)){dao.estado(c,id,e);auditar(c,a,id,"CAMBIAR_ESTADO",s.estado(),e,"El Administrador actualizó el avance de la mejora.");}return null;});}
    /** Quita sin borrar; Amarillo/Rojo conserva al menos una sugerencia activa (Andy, 11 nov 2026). */
    public void quitar(Cuenta actor,int id)throws SQLException{tx.ejecutar(c->{var a=admin(c,actor);var primera=exigir(dao.buscar(c,id));var e=visible(c,a,primera.evaluacion(),true);var s=dao.buscarActual(c,id);if(!s.activo())return null;if(!"VERDE".equals(e.color())&&dao.actuales(c,e.evaluacion()).stream().filter(Sugerencia::activo).count()<=1)throw new IllegalArgumentException("Este resultado requiere al menos una sugerencia. Agrega otra antes de quitar esta.");dao.quitar(c,id);auditar(c,a,id,"DESACTIVAR",s.estado(),s.estado(),"Sugerencia quitada de la lista; su historial se conserva.");return null;});}
    /** Indica que ya se puede registrar una reserva de Reevaluación para este puesto (CU5-26). */
    public boolean todasHechas(Cuenta actor,int evaluacion)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);var e=visible(c,a,evaluacion,false);return dao.todasHechas(c,e.puesto());});}
    /** RF-023a: propuestas antes de guardar, sin insertar filas y con el cálculo ya autorizado. */
    public List<Regla> previsualizar(Cuenta actor,MotorRula.Calculo calculo,Map<String,Boolean> condiciones)throws SQLException{return tx.ejecutar(c->{admin(c,actor);return aplicables(calculo,condiciones,dao.reglas(c));});}
    private static List<Regla> aplicables(MotorRula.Calculo calculo,Map<String,Boolean> condiciones,List<Regla> reglas){return reglas.stream().filter(r->TIPOS_POR_CONDICION.containsKey(r.condicion())&&(r.condicion().equals("SIEMPRE")||Boolean.TRUE.equals(condiciones.get(r.condicion())))).filter(r->calculo.mediciones().stream().anyMatch(m->m.segmento().name().equals(r.segmento())&&m.puntos()>=r.minimo())).filter(r->!r.texto().isBlank()).toList();}
    /** RF-023a: aplica reglas configuradas al resultado del servidor, conservando las propuestas previas. */
    void proponer(Connection c,Cuenta actor,int evaluacion,MotorRula.Calculo calculo)throws SQLException{
        dao.contexto(c,evaluacion,0,false,true);var ev=new EvaluacionDao().consultar(c,evaluacion);var d=ev.datos();
        Map<String,Boolean> condiciones=Map.of("SIEMPRE",true,"escritorio_muy_alto",d.escritorio(),"pantalla_muy_alta",d.pantalla(),"pies_colgando",d.pies(),"tiene_apoyapies",d.apoyapies(),"silla_con_apoyabrazos",d.apoyabrazos(),"silla_regulable",d.regulable());
        Set<Integer> propuestas=new HashSet<>();for(var s:dao.actuales(c,evaluacion))if(s.regla()!=null)propuestas.add(s.regla());
        for(var r:aplicables(calculo,condiciones,dao.reglas(c))){
            String tipo=TIPOS_POR_CONDICION.get(r.condicion());boolean puntos=calculo.mediciones().stream().anyMatch(m->m.segmento().name().equals(r.segmento())&&m.puntos()>=r.minimo());
            if(tipo==null||!Boolean.TRUE.equals(condiciones.get(r.condicion()))||!puntos||propuestas.contains(r.id()))continue;
            String texto=r.texto().strip();if(texto.isEmpty())continue;
            var s=new Sugerencia(0,evaluacion,r.id(),titulo(texto),texto,tipo,prioridad(calculo.resultado().nivel().color()),"PENDIENTE",null,"SISTEMA",true);
            int id=dao.crear(c,s);auditar(c,actor,id,"CREAR",null,"PENDIENTE","Sugerencia del sistema · regla "+r.id()+".");propuestas.add(r.id());
        }
    }
    /** (propuesta) Andy, 11 nov 2026: prioridad inicial en un solo lugar, fácil de cambiar. */
    public static String prioridad(String color){return "ROJO".equals(color)?"ALTA":"MEDIA";}
    /** Título del texto original, hasta 120 caracteres y sin cortar una palabra (Andy, 11 nov 2026). */
    public static String titulo(String texto){String limpio=texto.strip().replaceAll("\\s+"," ");if(limpio.codePointCount(0,limpio.length())<=120)return limpio;int corte=limpio.offsetByCodePoints(0,120);if(!Character.isWhitespace(limpio.codePointAt(corte))){int espacio=limpio.lastIndexOf(' ',corte);if(espacio<0)throw new IllegalArgumentException("La regla necesita un título con palabras de hasta 120 caracteres.");corte=espacio;}return limpio.substring(0,corte).stripTrailing();}
    private Contexto visible(Connection c,Cuenta a,int id,boolean bloqueo)throws SQLException{var e=dao.contexto(c,id,AccesoDatos.empresaPermitida(a),a.rol()==Cuenta.Rol.USUARIO,bloqueo);if(e==null)throw new SecurityException("Acceso denegado. La evaluación no está disponible para tu cuenta.");if(e.puntaje()==null)throw new IllegalArgumentException("Guarda un análisis antes de registrar sugerencias.");return e;}
    private Sugerencia editable(Connection c,int id,int evaluacion)throws SQLException{var s=exigir(dao.buscarActual(c,id));if(s.evaluacion()!=evaluacion||!s.activo())throw new SecurityException("Acceso denegado. La sugerencia no está disponible para editar.");return s;}
    private static Sugerencia exigir(Sugerencia s){if(s==null)throw new SecurityException("Acceso denegado. La sugerencia no está disponible.");return s;}
    private static Cuenta admin(Connection c,Cuenta actor)throws SQLException{var a=AccesoDatos.vigente(c,actor);Permisos.administrador(a);return a;}
    private void auditar(Connection c,Cuenta a,int id,String accion,String anterior,String nuevo,String detalle)throws SQLException{bitacora.registrar(c,a.id(),LocalDateTime.now(reloj),"sugerencia",id,accion,anterior,nuevo,detalle);}
    private static LocalDate fecha(String s){if(s==null||s.isBlank())return null;try{var d=LocalDate.parse(s);if(d.getYear()<1000||d.getYear()>9999)throw new IllegalArgumentException();return d;}catch(RuntimeException e){throw new IllegalArgumentException("Revisa la fecha límite: escribe una fecha válida.");}}
}
