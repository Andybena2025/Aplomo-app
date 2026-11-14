package co.aplomo;

import co.aplomo.web.Router;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/** RNF-008, RF-001a: arranque del servidor web del JDK, sin frameworks. */
public final class App {
    private App() {}

    /** Inicia APLOMO en el puerto 8080 y registra el control de acceso central (RF-002). */
    public static void main(String[] args) throws IOException {
        if(args.length==1&&(args[0].equals("--cargar-demo")||args[0].equals("--reiniciar-demo"))){
            co.aplomo.instalacion.Demo.ejecutar(args[0].equals("--reiniciar-demo"));return;
        }
        int puerto=Integer.getInteger("aplomo.puerto",8080);
        HttpServer servidor=HttpServer.create(new InetSocketAddress("0.0.0.0",puerto),0);
        servidor.createContext("/",new Router());
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("APLOMO. corriendo en http://0.0.0.0:"+puerto+" (accesible desde la red local)");
    }
}
