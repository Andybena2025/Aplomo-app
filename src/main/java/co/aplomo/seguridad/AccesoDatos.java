package co.aplomo.seguridad;
import co.aplomo.dao.CuentaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import java.sql.*;
/** RF-002 (Paso 24): alcance único, consultado en cada petición, sin caché de asignaciones. */
public final class AccesoDatos {
 private AccesoDatos(){}
 /** Relee rol, estado, temporal y hash; la relación histórica id_empresa no autoriza. */
 public static Cuenta vigente(Connection con,Cuenta actor)throws SQLException{
  if(actor==null)throw new SecurityException("Inicia sesión para continuar.");
  var a=new CuentaDao().porId(con,actor.id(),false);
  if(a==null||a.estado()!=Estado.ACTIVA||a.temporal()||!a.hash().equals(actor.hash())||a.rol()!=actor.rol())throw new SecurityException("Tu acceso cambió. Vuelve a iniciar sesión.");
  return a;
 }
 /** Alcance parametrizado: todas o asignaciones activas. empresaExacta solo restringe relaciones internas. */
 public record Alcance(boolean todas,int cuenta,Integer empresaExacta,boolean finalizadas){
  /** Fragmento con alias del código, nunca del navegador (RNF-004). */
  public String sql(String columna){
   if(!columna.matches("[a-z]+\\.id_empresa"))throw new IllegalArgumentException("Columna de alcance inválida.");
   return empresaExacta!=null?"(? = TRUE OR "+columna+" = ?)":"(? = TRUE OR "+columna+" IN (SELECT ce.id_empresa FROM cuenta_empresa ce WHERE ce.id_cuenta = ? AND ce.activo = TRUE))";
  }
  /** Segundo parámetro de la condición; el primero es todas. */
  public int valor(){return empresaExacta==null?cuenta:empresaExacta;}
  /** Restricción de un padre interno; no usa cuenta.id_empresa como permiso. */
  public static Alcance exacta(int empresa){return new Alcance(empresa==0,0,empresa,false);}
 }
 /** Crea el alcance de la cuenta vigente; cero asignaciones produce listas vacías. */
 public static Alcance empresaPermitida(Cuenta a){return new Alcance(a.rol()==Rol.ADMINISTRADOR,a.id(),null,a.rol()==Rol.USUARIO);}
 /** Rechaza una empresa ajena, salvo práctica propia autorizada por su reserva creadora. */
 public static void exigirEmpresa(Connection c,Cuenta a,int empresa)throws SQLException{
  var v=vigente(c,a);var al=empresaPermitida(v);
  if(!Boolean.TRUE.equals(Sql.uno(c,"SELECT TRUE FROM empresa e WHERE e.id_empresa=? AND "+al.sql("e.id_empresa"),r->r.getBoolean(1),empresa,al.todas(),al.valor())))throw new SecurityException("El registro no está disponible para tu cuenta.");
 }
 /** Autoriza una visita normal dentro del alcance o la práctica privada del Evaluador. */
 public static void exigirVisita(Connection c,Cuenta a,int visita)throws SQLException{
  var v=vigente(c,a);var al=empresaPermitida(v);
  String sql="SELECT TRUE FROM visita v JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) WHERE v.id_visita=? AND ((e.nit<>? AND "+al.sql("s.id_empresa")+") OR (e.nit=? AND (?=TRUE OR r.id_administrador=?)))";
  if(!Boolean.TRUE.equals(Sql.uno(c,sql,r->r.getBoolean(1),visita,co.aplomo.modelo.Demostracion.NIT_PRACTICA,al.todas(),al.valor(),co.aplomo.modelo.Demostracion.NIT_PRACTICA,v.rol()==Rol.ADMINISTRADOR,v.id())))throw new SecurityException("El registro no está disponible para tu cuenta.");
 }
 /** Lectura histórica excepcional: solo la evaluación creada por este Evaluador (RNF-009). */
 public static boolean propia(Connection c,Cuenta a,int evaluacion)throws SQLException{
  return a.rol()==Rol.EVALUADOR&&Boolean.TRUE.equals(Sql.uno(c,"SELECT TRUE FROM bitacora WHERE tabla='evaluacion' AND accion='CREAR' AND id_registro=? AND id_cuenta=? LIMIT 1",r->r.getBoolean(1),evaluacion,a.id()));
 }
 /** Autoriza lectura histórica propia o escritura dentro del alcance actual. */
 public static void exigirEvaluacion(Connection c,Cuenta a,int id,boolean escritura)throws SQLException{
  var e=new co.aplomo.dao.EvaluacionDao().consultar(c,id);if(e==null)throw new SecurityException("La evaluación no está disponible.");
  if(!escritura&&propia(c,a,id))return;
  exigirVisita(c,a,e.visita());
  if(escritura&&a.rol()==Rol.EVALUADOR&&!Boolean.TRUE.equals(Sql.uno(c,"SELECT (p.activo AND l.activo AND s.activo AND em.activo) FROM puesto_trabajo p JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa) WHERE p.id_puesto=?",r->r.getBoolean(1),e.puesto())))throw new SecurityException("Esta evaluación es de solo lectura porque su puesto o empresa está desactivado.");
 }
}
