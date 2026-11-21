package co.aplomo.servicio;
import co.aplomo.dao.*;import co.aplomo.db.*;import co.aplomo.modelo.Cuenta;import co.aplomo.seguridad.*;import java.sql.*;import java.time.*;import java.util.*;
/** RF-033 (propuesta), RF-002, RNF-005: agenda y notas privadas con alcance renovado por petición. */
public final class Agenda {
 private final Transacciones tx;private final Clock reloj;
 /** Usa las mismas transacciones y fecha de negocio que Reservas. */
 public Agenda(Transacciones tx,Clock reloj){this.tx=tx;this.reloj=reloj;}
 /** Evaluador en el filtro, incluidas cuentas inactivas. */
 public record Evaluador(int id,String nombre,boolean activo,String color){
 public Evaluador(int id,String nombre,boolean activo){this(id,nombre,activo,null);}
}
 /** Cita autorizada con su nota y visita; sin controles de modificación. */
 public record Cita(ReservaDao.Ficha ficha,Integer evaluador,String nombreEvaluador,Nota nota,boolean cruce,String color){
 public Cita(ReservaDao.Ficha ficha,Integer evaluador,String nombreEvaluador,Nota nota,boolean cruce){
 this(ficha,evaluador,nombreEvaluador,nota,cruce,null);}
}
 /** Texto privado visible únicamente a Administrador y Evaluador asignado. */
 public record Nota(String texto,boolean vista){}
 /** Rango normalizado; la semana comienza el lunes. */
 public record Vista(boolean mes,LocalDate fecha,LocalDate desde,LocalDate hasta,List<Cita> citas,List<Evaluador> evaluadores,String filtro,boolean canceladas){}
 /** Normaliza fechas y vistas inválidas a la semana actual de Bogotá. */
 public Vista consultar(Cuenta actor,Map<String,String> q)throws SQLException{
  return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);Permisos.trabajo(a);LocalDate fecha=LocalDate.now(reloj);boolean mes=true;
   try{String v=q.getOrDefault("vista","mes");if(!Set.of("semana","mes").contains(v))throw new IllegalArgumentException();fecha=LocalDate.parse(q.getOrDefault("fecha",fecha.toString()));if(fecha.getYear()<1900||fecha.getYear()>9998)throw new IllegalArgumentException();mes=v.equals("mes");}catch(RuntimeException ex){fecha=LocalDate.now(reloj);mes=true;}
   var evaluadores=Sql.listar(c,"SELECT id_cuenta,nombre,estado,color_agenda FROM cuenta WHERE rol='EVALUADOR' ORDER BY nombre,id_cuenta",rs->new Evaluador(rs.getInt(1),rs.getString(2),!rs.getString(3).equals("INACTIVA"),rs.getString(4)));
   String filtro=q.getOrDefault("evaluador","TODOS");if(a.rol()!=Cuenta.Rol.ADMINISTRADOR&&q.containsKey("evaluador"))throw new SecurityException("Acceso denegado.");
   if(!filtro.equals("TODOS")&&!filtro.equals("SIN_ASIGNAR")&&evaluadores.stream().noneMatch(v->Integer.toString(v.id()).equals(filtro)))throw new IllegalArgumentException("Elige un evaluador de la lista.");
   LocalDate desde=mes?fecha.withDayOfMonth(1):fecha.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));LocalDate hasta=mes?desde.plusMonths(1):desde.plusWeeks(1);boolean canceladas=q.getOrDefault("canceladas","").equals("1");
   var al=AccesoDatos.empresaPermitida(a);var bases=new ReservaDao().listar(c,al);List<Cita> citas=new ArrayList<>();
   for(var f:bases){var res=f.reserva();if(res.fecha().isBefore(desde)||!res.fecha().isBefore(hasta)||!canceladas&&res.estado().equals("CANCELADA"))continue;
    var responsable=Sql.uno(c,"SELECT q.id_cuenta,q.nombre FROM cuenta_empresa ce JOIN cuenta q USING(id_cuenta) WHERE ce.id_empresa=? AND ce.activo=TRUE AND q.rol='EVALUADOR' ORDER BY (q.estado='INACTIVA'),q.id_cuenta LIMIT 1",rs->Map.entry(rs.getInt(1),rs.getString(2)),f.empresa());Integer id=responsable==null?null:responsable.getKey();
    if(filtro.equals("SIN_ASIGNAR")&&id!=null||!Set.of("TODOS","SIN_ASIGNAR").contains(filtro)&&!Objects.equals(id,Integer.valueOf(filtro)))continue;
    citas.add(new Cita(f,id,responsable==null?"Sin evaluador asignado":responsable.getValue(),notaEn(c,res.id()),false,evaluadores.stream().filter(e->Objects.equals(e.id(),id))
      .map(Evaluador::color).filter(Objects::nonNull).findFirst().orElse(null)));
   }
   citas.sort(Comparator.comparing((Cita cita)->cita.ficha().reserva().fecha())
       .thenComparing(cita->cita.ficha().reserva().inicio())
       .thenComparingInt(cita->cita.ficha().reserva().id()));
   return new Vista(mes,fecha,desde,hasta,List.copyOf(citas),
       a.rol()==Cuenta.Rol.ADMINISTRADOR?evaluadores:List.of(),filtro,canceladas);
  });
 }
 /** Consulta la nota solo tras autorizar la reserva; el Usuario nunca la recibe. */
 public Nota nota(Cuenta actor,int reserva)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);if(a.rol()==Cuenta.Rol.USUARIO)return null;Permisos.trabajo(a);exigir(c,a,reserva);return notaEn(c,reserva);});}
 /** Guarda nota privada y reinicia el acuse aun si su texto es igual. */
 public void guardarNota(Cuenta actor,int reserva,String texto)throws SQLException{tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);Permisos.administrador(a);exigir(c,a,reserva);String t=Validacion.opcional(texto,"nota para el evaluador",500);Sql.actualizar(c,"UPDATE reserva SET nota_administrador=?,nota_vista=FALSE WHERE id_reserva=?",t.isBlank()?null:t,reserva);new BitacoraDao().registrar(c,a.id(),LocalDateTime.now(reloj),"reserva",reserva,"EDITAR",null,null,"Nota del Administrador");return null;});}
 /** Acuse con permiso de Evaluador asignado; el Administrador no lo marca por él. */
 public void entender(Cuenta actor,int reserva)throws SQLException{tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);if(a.rol()!=Cuenta.Rol.EVALUADOR)throw new SecurityException("Acceso denegado.");exigir(c,a,reserva);Sql.actualizar(c,"UPDATE reserva SET nota_vista=TRUE WHERE id_reserva=?",reserva);return null;});}
 /** Cantidad y primera reserva con nota pendiente, solo dentro del alcance vigente. */
 public List<Integer> nuevas(Cuenta actor)throws SQLException{return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);if(a.rol()!=Cuenta.Rol.EVALUADOR)return List.of();var al=AccesoDatos.empresaPermitida(a);return Sql.listar(c,"SELECT r.id_reserva FROM reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE "+al.sql("s.id_empresa")+" AND r.nota_administrador IS NOT NULL AND r.nota_vista=FALSE ORDER BY r.fecha,r.hora_inicio,r.id_reserva",rs->rs.getInt(1),al.todas(),al.valor());});}
 /** RF-033: datos mínimos del aviso privado. */
 public record Aviso(int reserva,String fecha,String hora,String empresa,String texto) {}
 /** RF-002/033: notas pendientes, incluidas las de otros meses, solo para su Evaluador. */
 public List<Aviso> avisos(Cuenta actor)throws SQLException {
  return tx.ejecutar(conexion->{
   Cuenta cuenta=AccesoDatos.vigente(conexion,actor);
   if(cuenta.rol()!=Cuenta.Rol.EVALUADOR)throw new SecurityException("Acceso denegado.");
   var alcance=AccesoDatos.empresaPermitida(cuenta);
   return Sql.listar(conexion,
       "SELECT r.id_reserva,r.fecha,r.hora_inicio,e.nombre,r.nota_administrador FROM reserva r "
       +"JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) "
       +"WHERE "+alcance.sql("s.id_empresa")
       +" AND r.nota_administrador IS NOT NULL AND r.nota_vista=FALSE ORDER BY r.fecha,r.hora_inicio",
       fila->new Aviso(fila.getInt(1),fila.getDate(2).toLocalDate()
           .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
           fila.getTime(3).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("H:mm")),
           fila.getString(4),fila.getString(5)),alcance.todas(),alcance.valor());
  });
 }
 private static void exigir(Connection c,Cuenta a,int id)throws SQLException{if(new ReservaDao().buscar(c,id,AccesoDatos.empresaPermitida(a),false)==null)throw new SecurityException("La reserva no está disponible para tu cuenta.");}
 private static Nota notaEn(Connection c,int id)throws SQLException{return Sql.uno(c,"SELECT nota_administrador,nota_vista FROM reserva WHERE id_reserva=?",r->new Nota(r.getString(1),r.getBoolean(2)),id);}
}
