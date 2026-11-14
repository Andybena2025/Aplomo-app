# Informe · Pasos 15-E y 16 (A → B → C → D)

14 nov 2026. Encargo de cuatro partes del chat. Sin capturas ni carpeta de evidencia gráfica. No se agregaron preguntas a la evaluación.

## Qué cambió

**A. Evaluación.** Los cuatro chips del brazo usan los textos exactos solicitados. Control «Brazo que hace el esfuerzo», con lado propuesto por detección y cambio con un toque; brazo elegido numerado y el otro tenue. Cambiar foto vuelve al selector con ejemplos y reinicia mediciones; quitar vuelve a subir. Las fotos ya guardadas se marcan `Reemplazada · ` en su descripción y en bitácora, sin borrar archivos, registros ni análisis históricos. No se admiten nuevos análisis de una foto marcada. No se agregó campo ni tabla. Seguir permanece visible, gris/desactivado mientras falte algo, con el primer dato faltante y una zona de toque/teclado que lo resalta. En el resultado pasa a Guardar y terminar. Agregar otra foto es un botón plegable con borde y +.

**B. Instalación.** Configuración por entorno con respaldo por campo al archivo existente. Docker compila con Maven/Java 17 y ejecuta JRE 17 como UID 10001. MySQL 8 inicializa el SQL original y una cuenta de red con SELECT/INSERT/UPDATE, sin DELETE. Datos y fotos en volúmenes separados. Las claves locales están en `.env` ignorado; `.env.ejemplo` solo tiene marcadores. Demo voluntario con el SQL y las mismas cuatro fotos del script nativo, que no se modificó. README e INSTALAR explican inicio, cuenta temporal, logs, apagado y la diferencia destructiva de down -v.

**C. Rutas.** [ENDPOINTS.md](ENDPOINTS.md) inventaría el contexto raíz, Router, los nueve manejadores de gestión/consulta, cambio de fotos y recursos estáticos. Métodos, parámetros, alcance, formato de respuesta y RF/RNF; cuenta inicial y seguridad. Incluye familias dinámicas, alias y que el informe es HTML imprimible, no un PDF servido. Pruebas documentales de contexto, secciones, rutas literales y familias.

**D. Equipo.** Los tres nombres y el crédito académico exactos; Laura y Johan sin rol. Una comparación con `da31871` confirmó que el resto del README quedó idéntico, fuera de Docker y Equipo. El contador de pruebas anterior del README se conserva porque el encargo prohíbe cambiar las demás secciones.

## Archivos por parte

| Parte | Archivos creados o modificados |
|---|---|
| A · Java | `servicio/FotosEvaluacion.java` (nuevo), `servicio/Evaluaciones.java`, `servicio/Analisis.java`, `web/FotosEvaluacionWeb.java` (nuevo), `web/EvaluacionesWeb.java`, `web/ConversacionEvaluacion.java`, todos bajo `src/main/java/co/aplomo/` |
| A · interfaz/pruebas | `src/main/resources/static/css/aplomo.css`, `src/main/resources/static/js/analisis.js`, `conversacion-evaluacion.js`, `posturas-campo.js` (estos dos bajo el mismo directorio JS); `src/test/java/co/aplomo/web/HttpFotosCambioTest.java` (nuevo); `scripts/qa_posturas_campo.cjs` |
| B · configuración | `.dockerignore`, `.env.ejemplo`, `Dockerfile`, `docker-compose.yml`, `docker/initdb/02-app-red.sql` (nuevos); `.gitignore`, `src/main/java/co/aplomo/App.java`, `config/Config.java`, `servicio/Fotos.java`, `instalacion/Demo.java` (estos tres bajo co/aplomo); `src/test/java/co/aplomo/config/ConfigTest.java` (nuevo) |
| B · instrucciones | `README.md` (solo Con Docker) e `INSTALAR.md`; no se cambió `scripts/cargar_demo*` |
| C | `docs/ENDPOINTS.md`, `src/test/java/co/aplomo/web/EndpointsDocumentoTest.java` (nuevos) |
| D y cierre | `README.md` (solo Equipo), `AGENTS.md` (estado), este informe |

El commit B incluye además la desactivación inicial de Seguir en HTML y su aserción de A.

## Trazabilidad

| Requisito / encargo | Archivo y método | Comprobación |
|---|---|---|
| RF-014/017; RNF-002 · A1 | ConversacionEvaluacion.pagina; crearAnalizadorAplomo.elegirBrazo; AplomoPosturasCampo.puntosBrazo | HttpFotosCambioTest.textosExactosYControlesDeFotoEstanEnLaPagina; JS ambos lados |
| RF-016; RNF-005/006/009 · A2 | FotosEvaluacion.cambiar, FotosEvaluacionWeb.cambiar, Analisis.guardarEnTransaccion; recibir/retirarActual | Siete HttpFotosCambioTest: historial/archivo/análisis, idempotencia, PDF falso, CSRF/foto ajena, Usuario, visita Finalizada, controles |
| RNF-002/007 · A3/A4 | ConversacionEvaluacion.pagina/apoyo; pendiente/dibujarPreguntas/mostrarFalta; estadoSeguir | HTTP controles/disabled inicial; JS estados de Seguir y primera falta; sin inspección visual nueva |
| RNF-001/004/011 · B1–B6 | Config.leer; App.main; Fotos; Dockerfile/compose/initdb | 11 ConfigTest; construcción real; 21 tablas y permisos mínimos comprobados |
| RNF-009 · B7 persistencia | Volúmenes mysql_datos/fotos | 3 empresas, 4 evaluaciones, 4 fotos BD y 4 archivos iguales antes/después del reinicio |
| RF-002/024; RNF-004/011 · C | ENDPOINTS.md; EndpointsDocumentoTest | 3 pruebas del inventario; suite HTTP existente de permisos y formularios |
| D · crédito del equipo | README Equipo | Comparación de secciones contra da31871 y revisión de los tres nombres |

## Pruebas y comandos

| Comando | Resultado |
|---|---|
| `TZ=America/Bogota ./mvnw test` | **923 ejecutadas, 923 aprobadas; 0 fallos, 0 errores, 0 omitidas. BUILD SUCCESS. 1 min 47 s.** JDBC de pruebas fijado por Surefire a rula_ergonomia_pruebas |
| `node --test scripts/qa_deteccion_campo.cjs scripts/qa_fotos_campo.cjs scripts/qa_posturas_campo.cjs scripts/qa_revision_eventos.cjs` | **60 ejecutadas y aprobadas**, 0 fallos/omitidas |
| Comparación del bloque POST calcular entre da31871 y el árbol final | Idéntico |
| `git diff da31871 -- src/main/java/co/aplomo/rula docs/base_de_datos/Script_MySQL_RULA.sql scripts/cargar_demo.sh scripts/cargar_demo.bat` | Sin cambios |
| Comparación de README quitando únicamente Con Docker y Equipo | Resto idéntico |
| `git check-ignore .env` y lista de archivos versionados | .env ignorado/no versionado; config/db.properties y fotos/ no añadidos |
| GET nativo `http://localhost:8080/login` al terminar B/C/D | **200**, app local restaurada |

### B7 · Docker comprobado de verdad

Se usó `COMPOSE_PROJECT_NAME=aplomo_qa_15e16_b` delante de los comandos para separar estos volúmenes del resto. Docker Engine 29.7.2, Compose 5.4.0; MySQL ejecutado 8.4.13; JRE 17. La base nativa no se reinició ni se cargó con el demo.

| Comando / solicitud | Resultado observado |
|---|---|
| `docker compose up --build -d` | Código 0; imagen construida, db Healthy y app Running |
| `curl http://localhost:8080/login` (GET) | **200** |
| POST `/login` con cookie, CSRF y cuenta semilla | **303**, Location `/cuenta/contrasena`: contraseña temporal obligatoria; no se cambió la clave semilla para el ensayo |
| Consultar information_schema, `id -u` y SHOW GRANTS | **21 tablas**, app **UID 10001**, app_rula@% con **SELECT, INSERT, UPDATE**, sin DELETE |
| Carga opcional por `docker compose exec -T db …` y copia por `docker compose exec -T app …` | Demo aprobado: **3 empresas, 4 evaluaciones, 4 fotos** en BD; **4 JPEG** en volumen |
| `docker compose restart app` | Código 0; GET login vuelve a **200**; mismos conteos **3/4/4**, **4/4 huellas SHA-256 iguales** |
| `docker compose down` | Código 0; contenedores/red detenidos; volúmenes conservados. **No se ejecutó down -v** |

El primer ensayo aislado (`aplomo_qa_15e16`) mostró un problema de ejecución de un archivo .sh montado y no creó la cuenta de red; el login devolvía 503. Se sustituyó ese archivo temporal por `02-app-red.sql`, se repitió desde un volumen nuevo y pasó toda la secuencia anterior. El archivo .sh fallido no se incluyó en Git. Ambos proyectos de QA quedaron apagados, con sus volúmenes conservados para no borrar datos sin permiso. La app nativa se volvió a levantar en 8080.

## Lo que no se comprobó / límites

- No se tomaron capturas ni se hizo una nueva revisión visual de tamaños, tacto físico, consola de navegador o iPad en este encargo. Las pruebas automáticas verifican textos, estados, archivos y permisos; no certifican apariencia o interacción física.
- La prueba documental es una alarma de inventario, no un generador OpenAPI ni una prueba funcional de cada ruta. Las familias dinámicas se documentaron leyendo sus manejadores; no existe catálogo central completo.
- `/demo/evaluacion` es valor agregado de práctica, sin RF propio; se marca así. El POST `/sugerencias/nueva` es alias de RF-023b, sin requisito independiente. Docker/recursos estáticos son soporte RNF. No quedó otra ruta sin relación identificada.
- El guardado de las dos posturas continúa siendo separado, como antes. No se cambió el cálculo ni se promete transacción conjunta.

## Checklist

- ✅ 923 Maven y 60 JS aprobadas.
- ✅ Permisos/CSRF y rechazo de Usuario por dirección manual comprobados con HTTP; conservación de fotos y análisis.
- ✅ Docker B7 completo, incluidas persistencia y cuenta con permisos mínimos.
- ✅ Textos exactos y español con tildes revisados en código/pruebas.
- ✅ Sin claves locales/fotos en Git; README limitado a las dos secciones autorizadas.
- ✅ Motor, endpoint de cálculo y SQL original intactos.
- ❌ Nueva comparación visual con mock y tamaños 390/810/1080/1366: no realizada; sin capturas por instrucción.
- ❌ Tacto físico y consola del navegador: no comprobados en esta entrega.

## Cómo probarlo

1. La instalación local está disponible en http://localhost:8080 (en iPad, el nombre/IP del Mac y puerto 8080). Entra con tu cuenta actual; las credenciales semilla aplican solo a una instalación nueva.
2. Inicio → Probar evaluación → Oficina o Planta. Antes de responder, Seguir debe estar gris y señalar lo primero que falta; tocar su zona debe llevar a ese campo.
3. Con consentimiento y permiso de fotos, usa una foto de ejemplo, revisa el brazo y prueba Cambiar foto y Quitar foto. La foto nueva debe pedir/recalcular mediciones; completa muñeca para continuar. Agregar otra foto abre sus opciones en línea.
4. Para revisar una guardada, Añadir análisis permite cambiar/quitar lógicamente; visita En curso y permisos se exigen en servidor. Los archivos históricos no se borran.
5. Para instalación independiente: seguir Con Docker en INSTALAR; cambiar la contraseña temporal al entrar. Apagar con down, sin -v si se quieren conservar datos y fotos.
6. Para revisar rutas/roles/campos, abrir ENDPOINTS.md. No se necesita guardar ni alterar evaluaciones para leerlo.

## Commits

- 021ed06 — Paso 15-E A: brazo evaluado, fotos y avance guiado.
- b8ef97f — Paso 16 B: Docker con MySQL y configuración por entorno.
- 070e359 — Paso 16 C: inventario de rutas HTTP y permisos.
- Cierre D: Equipo, estado e informe, en el commit que incorpora este documento.

El estado del push se confirma en la respuesta final una vez verificado origin/main. Detenerse y esperar revisión de Andy.
