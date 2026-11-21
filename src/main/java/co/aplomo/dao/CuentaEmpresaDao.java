package co.aplomo.dao;
import co.aplomo.db.Sql;import co.aplomo.modelo.Cuenta;import java.sql.*;import java.time.*;import java.util.*;
/** RF-002/003, RNF-005: asignaciones conservadas, único Evaluador activo por empresa. */
public final class CuentaEmpresaDao {
 /** Lee las empresas asignadas, incluidas las inactivas. */
 public List<Integer> ids(Connection c,int cuenta)throws SQLException{return Sql.listar(c,"SELECT id_empresa FROM cuenta_empresa WHERE id_cuenta=? AND activo=TRUE ORDER BY id_empresa",r->r.getInt(1),cuenta);}
 /** Mapa de asignaciones para la gestión autorizada de cuentas. */
 public Map<Integer,List<Integer>> todas(Connection c)throws SQLException{
  Map<Integer,List<Integer>> m=new HashMap<>();for(var par:Sql.listar(c,"SELECT id_cuenta,id_empresa FROM cuenta_empresa WHERE activo=TRUE ORDER BY id_empresa",r->Map.entry(r.getInt(1),r.getInt(2))))m.computeIfAbsent(par.getKey(),k->new ArrayList<>()).add(par.getValue());return m;
 }
 /** Valida y bloquea en orden para impedir dos Evaluadores simultáneos. */
 public void validar(Connection c,int cuenta,Cuenta.Rol rol,Collection<Integer> empresas,boolean activa)throws SQLException{
  for(int id:new TreeSet<>(empresas)){
   var emp=Sql.uno(c,"SELECT activo FROM empresa WHERE id_empresa=? FOR UPDATE",r->r.getBoolean(1),id);if(emp==null)throw new IllegalArgumentException("La empresa elegida no existe. Selecciona una de la lista.");
   if(!emp&&!ids(c,cuenta).contains(id))throw new IllegalArgumentException("La empresa está inactiva. Reactívala antes de asignar cuentas nuevas.");
   if(rol==Cuenta.Rol.EVALUADOR&&activa){String nombre=Sql.uno(c,"SELECT q.nombre FROM cuenta_empresa ce JOIN cuenta q USING(id_cuenta) WHERE ce.id_empresa=? AND ce.activo=TRUE AND q.rol='EVALUADOR' AND q.estado<>'INACTIVA' AND q.id_cuenta<>? LIMIT 1",r->r.getString(1),id,cuenta);if(nombre!=null)throw new IllegalArgumentException("Esta empresa ya tiene evaluador: "+nombre+". Quítasela primero.");}
  }
 }
 /** Retira y reactiva sin DELETE; cada cambio conserva responsable y fecha. */
 public void sincronizar(Connection c,int responsable,int cuenta,Collection<Integer> deseadas,boolean cambiaRol)throws SQLException{
  Set<Integer> nuevas=new TreeSet<>(deseadas);var antes=ids(c,cuenta);var ahora=LocalDateTime.now(ZoneId.of("America/Bogota"));var b=new BitacoraDao();
  for(int id:antes)if(cambiaRol||!nuevas.contains(id)){int fila=Sql.uno(c,"SELECT id_cuenta_empresa FROM cuenta_empresa WHERE id_cuenta=? AND id_empresa=?",r->r.getInt(1),cuenta,id);Sql.actualizar(c,"UPDATE cuenta_empresa SET activo=FALSE WHERE id_cuenta_empresa=?",fila);b.registrar(c,responsable,ahora,"cuenta_empresa",fila,"DESACTIVAR","ACTIVO","INACTIVO","Asignación retirada");}
  for(int id:nuevas)if(cambiaRol||!antes.contains(id)){Integer fila=Sql.uno(c,"SELECT id_cuenta_empresa FROM cuenta_empresa WHERE id_cuenta=? AND id_empresa=?",r->r.getInt(1),cuenta,id);boolean nueva=fila==null;if(nueva)fila=Sql.insertar(c,"INSERT INTO cuenta_empresa(id_cuenta,id_empresa,fecha_asignacion) VALUES (?,?,?)",cuenta,id,ahora);else Sql.actualizar(c,"UPDATE cuenta_empresa SET activo=TRUE,fecha_asignacion=? WHERE id_cuenta_empresa=?",ahora,fila);b.registrar(c,responsable,ahora,"cuenta_empresa",fila,nueva?"CREAR":"REACTIVAR",nueva?null:"INACTIVO","ACTIVO","Asignación de empresa");}
 }
}
