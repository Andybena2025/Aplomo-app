package co.aplomo.servicio;
import co.aplomo.db.Transacciones;import co.aplomo.dao.CalificacionesDao;import co.aplomo.modelo.*;import co.aplomo.config.Contacto;import co.aplomo.seguridad.AccesoDatos;import java.nio.file.*;import java.sql.*;import java.util.*;
/** Paso 25, RF-002/027, RNF-007/011: ayuda por rol; semáforo leído de MySQL, sin copiar reglas. */
public final class Ayuda {
 private final Transacciones tx;
 /** Comparte la conexión con el resto de consultas y sus pruebas. */
 public Ayuda(Transacciones tx){this.tx=tx;}
 /** Contenido autorizado; visitante no recibe niveles ni datos de operación. */
 public record Contenido(Set<Integer> temas,List<InformeEmpresa.Riesgo> riesgos,Contacto contacto){}
 /** Público: solo ingreso/contacto; cuentas temporales también reciben solo esa ayuda. */
 public Contenido consultar(Cuenta actor)throws SQLException{
  var contacto=Contacto.leer(Path.of(System.getProperty("aplomo.contacto.archivo","config/contacto.properties")));
  if(actor==null||actor.temporal())return new Contenido(Set.of(1,10),List.of(),contacto);
  return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);Set<Integer> temas=switch(a.rol()){case ADMINISTRADOR->Set.of(1,2,3,4,5,6,7,8,9,10);case EVALUADOR->Set.of(1,2,3,4,5,6,7,8,10);case USUARIO->Set.of(1,2,5,6,8,10);};return new Contenido(temas,new CalificacionesDao().riesgos(c),contacto);});
 }
}
