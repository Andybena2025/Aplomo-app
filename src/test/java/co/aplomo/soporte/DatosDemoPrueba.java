package co.aplomo.soporte;
import java.nio.file.*;
import java.sql.*;
/** Ejecuta el archivo real en la transacción de pruebas, omitiendo únicamente sus límites de transacción. */
public final class DatosDemoPrueba {
 private DatosDemoPrueba(){}
 public static void cargar(Connection con)throws Exception{
  if(!con.getMetaData().getURL().contains("/rula_ergonomia_pruebas?"))throw new IllegalStateException("Solo se permite la base de pruebas.");
  String sql=Files.readString(Path.of("docs/base_de_datos/datos_demo.sql")).lines()
    .filter(l->!l.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
  for(String texto:sql.split(";")){
   String sentencia=texto.trim();if(sentencia.isEmpty()||sentencia.equals("START TRANSACTION")||sentencia.equals("COMMIT"))continue;
   try(var consulta=con.prepareStatement(sentencia)){consulta.execute();}
  }
 }
}
