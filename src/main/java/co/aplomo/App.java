package co.aplomo;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Punto de entrada de APLOMO.
 * Arranca un servidor HTTP en el puerto 8080 y sirve la pagina inicial
 * con el estilo Obsidian & Ivory del mock app.
 */
public class App {

    private static final int PUERTO = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(PUERTO), 0);

        // Ruta principal: pagina de inicio
        servidor.createContext("/", App::paginaInicio);

        // Archivos estaticos: CSS y JS
        servidor.createContext("/static/", App::archivoEstatico);

        servidor.setExecutor(null);
        servidor.start();
        System.out.println("APLOMO. corriendo en http://localhost:" + PUERTO);
    }

    /** Muestra la pagina de inicio con el estilo del mock. */
    private static void paginaInicio(HttpExchange intercambio) throws IOException {
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
                    <title>APLOMO. — Ergonomia con precision</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Instrument+Serif&family=Inter:wght@300;400;500;600&display=swap" rel="stylesheet">
                    <link rel="stylesheet" href="/static/css/aplomo.css">
                </head>
                <body>
                    <div class="login" style="grid-template-columns:1fr">
                        <div class="form" style="max-width:480px;margin:0 auto;text-align:center">
                            <div style="margin-bottom:12px">
                                <span class="lockup" style="font-size:64px;color:#EDE6D8" aria-label="APLOMO.">
                                    <svg width="50" height="50" viewBox="0 0 100 100"
                                         style="margin-bottom:12px;margin-right:2px" aria-hidden="true">
                                        <line x1="50" y1="10" x2="16" y2="90" stroke="#EDE6D8" stroke-width="4" stroke-linecap="round"/>
                                        <line x1="50" y1="10" x2="84" y2="90" stroke="#EDE6D8" stroke-width="4" stroke-linecap="round"/>
                                        <path d="M31.2,54.2 A48,48 0 0 0 68.8,54.2" fill="none" stroke="#EDE6D8"
                                              stroke-width="2.2" stroke-dasharray="3.6 3.6" opacity=".85"/>
                                        <line x1="50" y1="10" x2="50" y2="64" stroke="#EDE6D8" stroke-width="1.6"/>
                                        <path d="M47.84,64 h4.32 v2.16 L55.04,70.48 L50,82.72 L44.96,70.48 L47.84,66.16 Z"
                                              fill="#EDE6D8"/>
                                        <circle cx="50" cy="10" r="3.6" fill="#EDE6D8"/>
                                    </svg>
                                    <span>PLOMO.</span>
                                </span>
                            </div>
                            <div class="tagline" style="margin-bottom:32px">Ergonomia con precision</div>

                            <div class="card stack" style="padding:32px;text-align:left">
                                <h3>Iniciar sesion</h3>
                                <div class="field">
                                    <label>CORREO</label>
                                    <input class="input" type="email" placeholder="admin@rula.local" disabled>
                                </div>
                                <div class="field">
                                    <label>CONTRASENA</label>
                                    <input class="input" type="password" placeholder="********" disabled>
                                </div>
                                <button class="btn p" disabled>Entrar</button>
                                <p class="hint" style="text-align:center">
                                    Proyecto base listo. La sesion se activa en la Fase 3.
                                </p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        intercambio.sendResponseHeaders(200, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    /** Sirve archivos estaticos desde src/main/resources/static/. */
    private static void archivoEstatico(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath(); // ejemplo: /static/css/aplomo.css

        // Prevenir path traversal
        if (ruta.contains("..")) {
            intercambio.sendResponseHeaders(403, -1);
            return;
        }

        // Buscar el recurso en el classpath
        String recurso = ruta.substring(1); // quitar la / inicial -> static/css/aplomo.css
        try (InputStream entrada = App.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                intercambio.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = entrada.readAllBytes();

            // Tipo de contenido segun la extension
            String tipo = "application/octet-stream";
            if (ruta.endsWith(".css")) tipo = "text/css; charset=UTF-8";
            else if (ruta.endsWith(".js")) tipo = "application/javascript; charset=UTF-8";
            else if (ruta.endsWith(".png")) tipo = "image/png";
            else if (ruta.endsWith(".jpg") || ruta.endsWith(".jpeg")) tipo = "image/jpeg";
            else if (ruta.endsWith(".svg")) tipo = "image/svg+xml";

            intercambio.getResponseHeaders().set("Content-Type", tipo);
            intercambio.sendResponseHeaders(200, bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        }
    }
}
