package co.aplomo.instalacion;

import co.aplomo.config.Config;
import co.aplomo.db.Sql;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Herramienta voluntaria de desarrollo; nunca se ejecuta al iniciar la web. */
public final class Demo {
    public static final String CONFIRMACION="REINICIAR DEMO";
    private static final String EMPRESAS="nit IN ('DEMO-APLOMO-E1','DEMO-APLOMO-E2','DEMO-APLOMO-E3')";
    private static final List<String> MUESTRAS=List.of("de100001-493b-4bc9-a64e-d51b4ea3c901.jpg","de100002-493b-4bc9-a64e-d51b4ea3c902.jpg","de100003-493b-4bc9-a64e-d51b4ea3c903.jpg","de100004-493b-4bc9-a64e-d51b4ea3c904.jpg");
    private Demo() {}

    /** Usa la base configurada y pide contraseña oculta y confirmación antes de borrar. */
    public static void ejecutar(boolean reiniciar) {
        Console terminal=System.console();
        if(terminal==null)throw new IllegalStateException("Abre este script directamente en una terminal para confirmar y escribir la contraseña de forma privada.");
        terminal.printf("APLOMO · %s de datos de demostración%nBase configurada: %s%n",reiniciar?"Reinicio":"Carga",base(Config.url()));
        String usuario=terminal.readLine("Usuario administrador de MySQL (Enter = root): ");
        if(usuario==null)throw new IllegalArgumentException("Operación cancelada.");
        usuario=usuario.isBlank()?"root":usuario.trim();
        if(usuario.equalsIgnoreCase(Config.usuario()))throw new IllegalArgumentException("Usa el administrador de MySQL, no la cuenta de la aplicación.");
        char[] clave=terminal.readPassword("Contraseña de MySQL (Enter si no tiene): ");
        if(clave==null)throw new IllegalArgumentException("Operación cancelada.");
        Properties credenciales=new Properties();credenciales.setProperty("user",usuario);credenciales.setProperty("password",new String(clave));Arrays.fill(clave,'\0');
        try(Connection con=DriverManager.getConnection(Config.url(),credenciales)){
            credenciales.remove("password");verificarAdministrador(con);
            String confirmacion="";
            if(reiniciar){
                terminal.printf("Se borrarán SOLO DEMO-APLOMO-E1, E2 y E3 y sus registros. Las cuentas nunca se borran. Si alguna está vinculada, el reinicio se cancela.%n");
                confirmacion=terminal.readLine("Escribe %s para continuar: ",CONFIRMACION);
                if(!CONFIRMACION.equals(confirmacion)){terminal.printf("Cancelado. No se cambió ningún dato.%n");return;}
            }
            con.setAutoCommit(false);
            List<String> fotos=List.of();
            try{
                if(reiniciar)fotos=vaciar(con,confirmacion);
                cargar(con,Path.of("docs/base_de_datos/datos_demo.sql"));
                con.commit();
            }catch(Exception e){con.rollback();throw e;}
            int borradas=limpiarFotos(con,Path.of("fotos"),fotos);
            copiarMuestras(Path.of("fotos"));
            terminal.printf("Demostración lista. Entra como Administrador y abre Evaluar ahora. Fotos antiguas retiradas: %d.%n",borradas);
            terminal.printf("No se modificaron otras empresas, cuentas ni config/db.properties.%n");
        }catch(Exception e){throw new IllegalStateException("No se pudo completar la demostración. Si falló la base, todos sus cambios se revirtieron. Revisa la conexión, los permisos y las cuentas vinculadas. "+(e instanceof IllegalArgumentException?e.getMessage():""),e);}
        finally{credenciales.clear();}
    }
    private static String base(String url){int fin=url.indexOf('?');String u=fin<0?url:url.substring(0,fin);return u.substring(u.lastIndexOf('/')+1);}

    /** No concede permisos a la cuenta de la app. */
    static void verificarAdministrador(Connection con)throws SQLException{
        String actual=Sql.uno(con,"SELECT CURRENT_USER()",r->r.getString(1));
        if(actual.split("@",2)[0].equalsIgnoreCase(Config.usuario()))throw new IllegalArgumentException("Usa el administrador de MySQL, no la cuenta de la aplicación.");
        var privilegios=Sql.listar(con,"SELECT PRIVILEGE_TYPE FROM information_schema.USER_PRIVILEGES WHERE GRANTEE=CONCAT(QUOTE(SUBSTRING_INDEX(CURRENT_USER(),'@',1)),'@',QUOTE(SUBSTRING_INDEX(CURRENT_USER(),'@',-1)))",r->r.getString(1));
        if(!privilegios.contains("DELETE")||!(privilegios.contains("SUPER")||privilegios.contains("CREATE USER")))throw new IllegalArgumentException("Esta operación requiere una cuenta administradora de MySQL con permisos globales.");
    }

    /** Carga idempotente en la transacción del llamador; no confirma ni crea bases. */
    public static void cargar(Connection con,Path sql)throws IOException,SQLException{
        if(con.getAutoCommit())throw new IllegalArgumentException("La demostración requiere una transacción.");
        Integer admin=Sql.uno(con,"SELECT id_cuenta FROM cuenta WHERE rol='ADMINISTRADOR' AND estado='ACTIVA' ORDER BY id_cuenta LIMIT 1",r->r.getInt(1));
        if(admin==null)throw new IllegalArgumentException("Primero debe existir un Administrador activo de APLOMO.");
        Integer ajenas=Sql.uno(con,"SELECT COUNT(*) FROM reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa) WHERE r.codigo IN ('DEMO-FIN-01','DEMO-FIN-02','DEMO-COL-01','DEMO-COL-02','DEMO-ESP-01','DEMO-ESP-02','DEMO-LST-E1','DEMO-LST-E2','DEMO-LST-E3') AND NOT ("+EMPRESAS+")",r->r.getInt(1));
        if(ajenas>0)throw new IllegalArgumentException("Hay un código demo en otra empresa. No se cambió nada.");
        String texto=Files.readAllLines(sql,StandardCharsets.UTF_8).stream().filter(l->!l.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
        for(String parte:texto.split(";")){
            String orden=parte.strip();if(orden.isEmpty()||orden.equalsIgnoreCase("START TRANSACTION")||orden.equalsIgnoreCase("COMMIT"))continue;
            try(PreparedStatement ps=con.prepareStatement(orden)){ps.execute();}
        }
    }

    /** Borra solo las tres empresas exactas; el llamador confirma o revierte. */
    public static List<String> vaciar(Connection con,String confirmacion)throws SQLException{
        if(!CONFIRMACION.equals(confirmacion))throw new IllegalArgumentException("Falta la confirmación REINICIAR DEMO.");
        if(con.getAutoCommit())throw new IllegalArgumentException("El reinicio requiere una transacción.");
        LinkedHashMap<String,List<Integer>> ids=new LinkedHashMap<>();
        ids.put("empresa",Sql.listar(con,"SELECT id_empresa FROM empresa WHERE "+EMPRESAS+" FOR UPDATE",r->r.getInt(1)));
        List<Integer> empresas=ids.get("empresa");if(empresas.isEmpty())return List.of();
        if(!hijos(con,"cuenta","id_cuenta","id_empresa",empresas).isEmpty())throw new IllegalArgumentException("Hay cuentas vinculadas a una empresa demo. Reasígnalas antes; no se borró nada.");
        ids.put("sede",hijos(con,"sede","id_sede","id_empresa",empresas));
        ids.put("salon",hijos(con,"salon","id_salon","id_sede",ids.get("sede")));
        ids.put("area",hijos(con,"area","id_area","id_empresa",empresas));
        ids.put("cargo",hijos(con,"cargo","id_cargo","id_empresa",empresas));
        ids.put("puesto_trabajo",hijos(con,"puesto_trabajo","id_puesto","id_salon",ids.get("salon")));
        ids.put("reserva",hijos(con,"reserva","id_reserva","id_salon",ids.get("salon")));
        ids.put("visita",hijos(con,"visita","id_visita","id_reserva",ids.get("reserva")));
        ids.put("evaluacion",hijos(con,"evaluacion","id_evaluacion","id_visita",ids.get("visita")));
        ids.put("condiciones_puesto",hijos(con,"condiciones_puesto","id_evaluacion","id_evaluacion",ids.get("evaluacion")));
        ids.put("fotografia",hijos(con,"fotografia","id_foto","id_evaluacion",ids.get("evaluacion")));
        ids.put("analisis_rula",hijos(con,"analisis_rula","id_analisis","id_foto",ids.get("fotografia")));
        ids.put("medicion_angulo",hijos(con,"medicion_angulo","id_medicion","id_analisis",ids.get("analisis_rula")));
        ids.put("sugerencia",hijos(con,"sugerencia","id_sugerencia","id_evaluacion",ids.get("evaluacion")));
        ids.put("pendiente_arreglo",hijos(con,"pendiente_arreglo","id_pendiente","id_evaluacion",ids.get("evaluacion")));
        ids.put("informe_empresa",hijos(con,"informe_empresa","id_informe","id_empresa",empresas));
        List<String> rutas=ids.get("fotografia").isEmpty()?List.of():Sql.listar(con,"SELECT ruta FROM fotografia WHERE id_foto IN ("+marcas(ids.get("fotografia"))+")",r->r.getString(1),ids.get("fotografia").toArray());
        for(var entrada:ids.entrySet())if(!entrada.getValue().isEmpty()){
            List<Object> parametros=new ArrayList<>();parametros.add(entrada.getKey());parametros.addAll(entrada.getValue());
            Sql.actualizar(con,"DELETE FROM bitacora WHERE tabla=? AND id_registro IN ("+marcas(entrada.getValue())+")",parametros.toArray());
        }
        Map<String,String> claves=Map.of("puesto_trabajo","id_puesto","condiciones_puesto","id_evaluacion","fotografia","id_foto","analisis_rula","id_analisis","medicion_angulo","id_medicion","pendiente_arreglo","id_pendiente","informe_empresa","id_informe");
        for(String tabla:List.of("medicion_angulo","analisis_rula","fotografia","sugerencia","pendiente_arreglo","condiciones_puesto","evaluacion","visita","reserva","puesto_trabajo","salon","sede","area","cargo","informe_empresa","empresa")){
            var lista=ids.get(tabla);if(!lista.isEmpty())Sql.actualizar(con,"DELETE FROM "+tabla+" WHERE "+claves.getOrDefault(tabla,"id_"+tabla)+" IN ("+marcas(lista)+")",lista.toArray());
        }
        return rutas;
    }
    private static List<Integer> hijos(Connection con,String tabla,String id,String fk,List<Integer> padres)throws SQLException{
        return padres.isEmpty()?List.of():Sql.listar(con,"SELECT "+id+" FROM "+tabla+" WHERE "+fk+" IN ("+marcas(padres)+") FOR UPDATE",r->r.getInt(1),padres.toArray());
    }
    private static String marcas(List<?> lista){return String.join(",",Collections.nCopies(lista.size(),"?"));}

    /** No toca archivos ajenos, compartidos, externos ni enlaces simbólicos. */
    public static int limpiarFotos(Connection con,Path carpeta,List<String> rutas)throws SQLException,IOException{
        if(!Files.isDirectory(carpeta,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(carpeta))return 0;
        Path base=carpeta.toRealPath();int n=0;
        for(String ruta:new LinkedHashSet<>(rutas)){
            if(ruta==null||ruta.isBlank()||Path.of(ruta).isAbsolute())continue;
            Path archivo=base.resolve(ruta).normalize();if(!archivo.startsWith(base)||archivo.equals(base))continue;
            boolean enlace=false;for(Path p=archivo;p!=null&&!p.equals(base);p=p.getParent())if(Files.isSymbolicLink(p)){enlace=true;break;}
            if(enlace||!Files.isRegularFile(archivo,LinkOption.NOFOLLOW_LINKS)||!archivo.toRealPath().startsWith(base))continue;
            Integer referencias=Sql.uno(con,"SELECT COUNT(*) FROM fotografia WHERE ruta=?",r->r.getInt(1),ruta);
            if(referencias==0&&Files.deleteIfExists(archivo))n++;
        }
        return n;
    }
    private static void copiarMuestras(Path carpeta)throws IOException{
        if(Files.isSymbolicLink(carpeta))throw new IOException("La carpeta de fotos no puede ser un enlace.");
        Files.createDirectories(carpeta);Path origen=Path.of("src/main/resources/static/img/muestra_perfil.jpg");
        for(String nombre:MUESTRAS){Path destino=carpeta.resolve(nombre);if(!Files.exists(destino,LinkOption.NOFOLLOW_LINKS))Files.copy(origen,destino);}
    }
}
