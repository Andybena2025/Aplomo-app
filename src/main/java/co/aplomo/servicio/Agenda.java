package co.aplomo.servicio;
import co.aplomo.dao.*;import co.aplomo.db.*;import co.aplomo.modelo.Cuenta;import co.aplomo.seguridad.*;import java.sql.*;import java.time.*;import java.util.*;
/** RF-033 (propuesta), RF-002, RNF-005: agenda y notas privadas con alcance renovado por petición. */
public final class Agenda {
 private final Transacciones tx;private final Clock reloj;
 /** Usa las mismas transacciones y fecha de negocio que Reservas. */
 public Agenda(Transacciones tx,Clock reloj){this.tx=tx;this.reloj=reloj;}
 /** Evaluador en el filtro, incluidas cuentas inactivas. */
 public record Evaluador(int id,String nombre,boolean activo){}
 /** Cita autorizada con su nota y visita; sin controles de modificación. */
 public record Cita(ReservaDao.Ficha ficha,Integer evaluador,String nombreEvaluador,Nota nota,boolean cruce){}
 /** Texto privado visible únicamente a Administrador y Evaluador asignado. */
 public record Nota(String texto,boolean vista){}
 /** Rango normalizado; la semana comienza el lunes. */
 public record Vista(boolean mes,LocalDate fecha,LocalDate desde,LocalDate hasta,List<Cita> citas,List<Evaluador> evaluadores,String filtro,boolean canceladas){}
 /** Normaliza fechas y vistas inválidas a la semana actual de Bogotá. */
 public Vista consultar(Cuenta actor,Map<String,String> q)throws SQLException{
  return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);Permisos.trabajo(a);LocalDate fecha=LocalDate.now(reloj);boolean mes=false;
   try{String v=q.getOrDefault("vista","semana");if(!Set.of("semana","mes").contains(v))throw new IllegalArgumentException();fecha=LocalDate.parse(q.getOrDefault("fecha",fecha.toString()));if(fecha.getYear()<1900||fecha.getYear()>9998)throw new IllegalArgumentException();mes=v.equals("mes");}catch(RuntimeException ex){fecha=LocalDate.now(reloj);mes=false;}
   var evaluadores=Sql.listar(c,"SELECT id_cuenta,nombre,estado FROM cuenta WHERE rol='EVALUADOR' ORDER BY nombre,id_cuenta",rs->new Evaluador(rs.getInt(1),rs.getString(2),!rs.getString(3).equals("INACTIVA")));
   String filtro=q.getOrDefault("evaluador","TODOS");if(a.rol()!=Cuenta.Rol.ADMINISTRADOR&&q.containsKey("evaluador"))throw new SecurityException("Acceso denegado.");
   if(!filtro.equals("TODOS")&&!filtro.equals("SIN_ASIGNAR")&&evaluadores.stream().noneMatch(v->Integer.toString(v.id()).equals(filtro)))throw new IllegalArgumentException("Elige un evaluador de la lista.");
   LocalDate desde=mes?fecha.withDayOfMonth(1):fecha.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));LocalDate hasta=mes?desde.plusMonths(1):desde.plusWeeks(1);boolean canceladas=q.getOrDefault("canceladas","").equals("1");
   var al=AccesoDatos.empresaPermitida(a);var bases=new ReservaDao().listar(c,al);List<Cita> citas=new ArrayList<>();
   for(var f:bases){var res=f.reserva();if(res.fecha().isBefore(desde)||!res.fecha().isBefore(hasta)||!canceladas&&res.estado().equals("CANCELADA"))continue;
    var responsable=Sql.uno(c,"SELECT q.id_cuenta,q.nombre FROM cuenta_empresa ce JOIN cuenta q USING(id_cuenta) WHERE ce.id_empresa=? AND ce.activo=TRUE AND q.rol='EVALUADOR' ORDER BY (q.estado='INACTIVA'),q.id_cuenta LIMIT 1",rs->Map.entry(rs.getInt(1),rs.getString(2)),f.empresa());Integer id=responsable==null?null:responsable.getKey();
    if(filtro.equals("SIN_ASIGNAR")&&id!=null||!Set.of("TODOS","SIN_ASIGNAR").contains(filtro)&&!Objects.equals(id,Integer.valueOf(filtro)))continue;
    citas.add(new Cita(f,id,responsable==null?"Sin evaluador asignado":responsable.getValue(),notaEn(c,res.id()),false));
   }
   List<Cita> finalizadas=new ArrayList<>();for(var t:citas){var z=t.ficha().reserva();boolean cruce=t.evaluador()!=null&&!z.estado().equals("CANCELADA")&&citas.stream().anyMatch(o->{var b=o.ficha().reserva();return b.id()!=z.id()&&!b.estado().equals("CANCELADA")&&Objects.equals(t.evaluador(),o.evaluador())&&z.fecha().equals(b.fecha())&&z.inicio().isBefore(b.fin())&&b.inicio().isBefore(z.fin());});finalizadas.add(new Cita(t.ficha(),t.evaluador(),t.nombreEvaluador(),t.nota(),cruce));}
   finalizadas.sort(Comparator.comparing((Cita t)->t.ficha().reserva().fecha()).thenComparing(t->t.ficha().reserva().inicio()).thenComparingInt(t->t.ficha().reserva().id()));return new Vista(mes,fecha,desde,hasta,List.copyOf(finalizadas),a.rol()==Cuenta.Rol.ADMINISTRADOR?evaluadores:List.of(),filtro,canceladas);
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
 private static void exigir(Connection c,Cuenta a,int id)throws SQLException{if(new ReservaDao().buscar(c,id,AccesoDatos.empresaPermitida(a),false)==null)throw new SecurityException("La reserva no está disponible para tu cuenta.");}
 private static Nota notaEn(Connection c,int id)throws SQLException{return Sql.uno(c,"SELECT nota_administrador,nota_vista FROM reserva WHERE id_reserva=?",r->new Nota(r.getString(1),r.getBoolean(2)),id);}
}
