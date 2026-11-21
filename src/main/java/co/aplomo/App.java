package co.aplomo;

import co.aplomo.web.Router;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/** RNF-008, RF-001a: arranque del servidor web del JDK, sin frameworks. */
public final class App {
    private App() {}

    /** Inicia ALINEO en el puerto 8080 y registra el control de acceso central (RF-002). */
    public static void main(String[] args) throws IOException {
        if((args.length==1||args.length==2)&&(args[0].equals("--cargar-demo")||args[0].equals("--reiniciar-demo"))){
            if(args.length==2)System.setProperty("aplomo.demo.fecha",java.time.LocalDate.parse(args[1]).toString());
            co.aplomo.instalacion.Demo.ejecutar(args[0].equals("--reiniciar-demo"));return;
        }
        if(args.length==1&&args[0].equals("--cargar-demo-entorno")){co.aplomo.instalacion.Demo.ejecutarEntorno();return;}
        try(var c=co.aplomo.db.Conexion.obtener()){co.aplomo.db.Esquema.verificar(c);}catch(java.sql.SQLException e){throw new IllegalStateException("No pudimos conectar con la base de datos. Revisa MySQL y la configuración.");}
        int puerto=co.aplomo.config.Config.puerto();
        HttpServer servidor=HttpServer.create(new InetSocketAddress("0.0.0.0",puerto),0);
        servidor.createContext("/",new Router());
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("ALINEO. corriendo en http://0.0.0.0:"+puerto+" (accesible desde la red local)");
    }
}
