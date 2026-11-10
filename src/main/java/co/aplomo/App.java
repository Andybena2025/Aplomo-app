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

    /** Muestra la pagina de login con foto de portada y dibujo de medicion, igual al mock. */
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
                    <div class="login">
                        <!-- Lado izquierdo: foto de portada con dibujo de medicion -->
                        <section class="hero" style="background-image:url('/static/img/portada.jpg')" aria-label="Portada">
                            <!-- SVG de medicion (puntos, angulos, plomada) pre-calculado del mock -->
                            <div class="pz" id="pzOverlay">
                                <svg class="pz-svg" viewBox="0 0 1400 933" aria-hidden="true">
                                    <line class="plumb" x1="1020" y1="20" x2="1020" y2="800"/>
                                    <path class="bob" d="M1014,800 L1026,800 L1020,816 Z"/>
                                    <line class="sk" pathLength="1" x1="1020" y1="410" x2="1024" y2="182"/>
                                    <line class="sk" pathLength="1" x1="1024" y1="182" x2="1066" y2="32"/>
                                    <line class="sk" pathLength="1" x1="1024" y1="182" x2="1052" y2="314"/>
                                    <line class="sk" pathLength="1" x1="1052" y1="314" x2="1122" y2="322"/>
                                    <line class="sk" pathLength="1" x1="1122" y1="322" x2="1160" y2="314"/>
                                    <line class="sk2" pathLength="1" x1="1020" y1="410" x2="1024" y2="598"/>
                                    <line class="sk2" pathLength="1" x1="1024" y1="598" x2="1012" y2="770"/>
                                    <line class="gd" pathLength="1" x1="1052" y1="314" x2="1061.5" y2="359"/>
                                    <line class="gd" pathLength="1" x1="1122" y1="322" x2="1155.8" y2="325.9"/>
                                    <path class="arc" pathLength="1" d="M1024.8,138.0 A44,44 0 0 1 1035.9,139.6"/>
                                    <path class="arc" pathLength="1" d="M1022.9,246.0 A64,64 0 0 0 1037.3,244.6"/>
                                    <path class="arc" pathLength="1" d="M1058.6,345.3 A32,32 0 0 0 1083.8,317.6"/>
                                    <path class="arc" pathLength="1" d="M1143.9,324.5 A22,22 0 0 0 1143.5,317.5"/>
                                    <g class="lb" transform="translate(966,82)"><text class="lt" text-anchor="end">CUELLO</text><text class="lv" y="17" text-anchor="end">15\u00b0</text></g>
                                    <g class="lb" transform="translate(950,246)"><text class="lt" text-anchor="end">BRAZO</text><text class="lv" y="17" text-anchor="end">13\u00b0</text></g>
                                    <g class="lb" transform="translate(1002,354)"><text class="lt" text-anchor="end">ANTEBRAZO</text><text class="lv" y="17" text-anchor="end">108\u00b0</text></g>
                                    <g class="lb" transform="translate(1178,348)"><text class="lt" text-anchor="start">MU\u00d1ECA</text><text class="lv" y="17" text-anchor="start">162\u00b0</text></g>
                                    <g class="lb" transform="translate(996,470)"><text class="lt" text-anchor="end">TRONCO</text><text class="lv" y="17" text-anchor="end">1\u00b0</text></g>
                                    <circle class="jt" cx="1066" cy="32" r="5"/>
                                    <circle class="jt" cx="1024" cy="182" r="5"/>
                                    <circle class="jt" cx="1052" cy="314" r="5"/>
                                    <circle class="jt" cx="1122" cy="322" r="5"/>
                                    <circle class="jt" cx="1160" cy="314" r="5"/>
                                    <circle class="jt" cx="1020" cy="410" r="5"/>
                                    <circle class="jt" cx="1024" cy="598" r="5"/>
                                    <circle class="jt" cx="1012" cy="770" r="5"/>
                                </svg>
                            </div>
                            <!-- Tarjeta de puntaje flotante -->
                            <div class="pz-card">
                                <div class="k">PUNTAJE RULA</div>
                                <div class="n">4</div>
                                <div class="nv"><i></i> Amarillo &middot; Nivel 2</div>
                            </div>
                            <!-- Logo y lema sobre la foto -->
                            <div class="in">
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
                                <div class="tagline">Ergonomia con precision</div>
                                <p class="muted" style="max-width:420px">Evaluacion ergonomica de puestos de trabajo con el metodo RULA: fotos, angulos, puntaje y resultados en un solo lugar.</p>
                            </div>
                        </section>
                        <!-- Lado derecho: formulario de login -->
                        <section class="form">
                            <div class="stack" style="gap:6px">
                                <span class="eyebrow">Software real &middot; Proyecto Nuclear</span>
                                <h2>Iniciar sesion</h2>
                            </div>
                            <form class="stack" novalidate>
                                <div class="field">
                                    <label>CORREO</label>
                                    <input class="input" type="email" placeholder="admin@rula.local" disabled>
                                </div>
                                <div class="field">
                                    <label>CONTRASE\u00d1A</label>
                                    <input class="input" type="password" placeholder="********" disabled>
                                </div>
                                <button class="btn p" disabled>Entrar</button>
                                <p class="hint" style="text-align:center">
                                    Proyecto base listo. La sesion se activa en la Fase 3.
                                </p>
                            </form>
                        </section>
                    </div>
                    <script>
                    // Ajustar el SVG de medicion para que cubra la foto (como montarPortada del mock)
                    (function(){
                        var hero=document.querySelector('.login .hero');
                        var pz=document.getElementById('pzOverlay');
                        if(!hero||!pz)return;
                        var PW=1400,PH=933,POSX=0.76;
                        function fit(){
                            var W=hero.clientWidth,H=hero.clientHeight;
                            var k=Math.max(W/PW,H/PH);
                            var iw=PW*k,ih=PH*k;
                            pz.style.width=iw+'px';
                            pz.style.height=ih+'px';
                            pz.style.left=((W-iw)*POSX)+'px';
                            pz.style.top=((H-ih)*0.5)+'px';
                        }
                        fit();
                        new ResizeObserver(fit).observe(hero);
                    })();
                    </script>
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
