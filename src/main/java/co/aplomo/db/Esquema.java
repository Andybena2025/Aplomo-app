package co.aplomo.db;
import java.sql.*;
/** RF-002, Paso 24: comprobación no destructiva antes de abrir el puerto. */
public final class Esquema {
 public static final String FALTA="Falta actualizar la base de datos. Corre scripts/migrar_paso24.sh (Mac) o migrar_paso24.bat (Windows).";
 private Esquema(){}
 /** Comprueba tabla, ENUM y columnas sin modificar ni instalar nada. */
 public static void verificar(Connection c)throws SQLException{
  int tabla=Sql.uno(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='cuenta_empresa'",r->r.getInt(1));
  String rol=Sql.uno(c,"SELECT column_type FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cuenta' AND column_name='rol'",r->r.getString(1));
  int notas=Sql.uno(c,"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reserva' AND column_name IN ('nota_administrador','nota_vista')",r->r.getInt(1));
  if(tabla!=1||rol==null||!rol.contains("'EVALUADOR'")||notas!=2)throw new IllegalStateException(FALTA);
 }
}
