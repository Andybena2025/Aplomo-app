# Paso 17 · Arreglos, demostración y pulido

Entrega del 14 de noviembre de 2026. Pendiente de revisión de Andy. Sin capturas, por instrucción expresa.

## Qué cambió

La IA revisa primero si la foto es frontal y comprueba únicamente nariz y lado corporal más visible. La práctica se separó de las once consultas y de los indicadores operativos. Inicio tiene un solo bloque de visitas, con acceso a cada puesto pendiente. Se ordenaron filtros, ubicación, pestañas, reserva, consentimiento y detalle de visita.

«Calcular manualmente» guía cinco marcas: hombro, codo, muñeca, cabeza y cintura. Avanza al tocar, permite volver/reiniciar y conserva el arrastre. Los ángulos se obtienen por el cálculo existente del servidor; la muñeca sigue respondiéndose con Recta / Un poco doblada / Muy doblada. La foto permanece fija en pantallas amplias y vuelve a apilarse en las estrechas.

El cargador voluntario crea resultados ficticios mediante los servicios y motor reales: 24 evaluaciones con sus fotos, 12 puestos con dos posturas, cuatro visitas Finalizadas calificadas y reservas futuras. Cada empresa tiene Verde, Amarillo y Rojo. Hay cuatro pendientes Abiertos, cuatro Arreglados y 24 sugerencias: 12 Pendientes y 12 Hechas. Las fotos provienen de las muestras originales; sus descripciones explican que los ángulos son casos manuales simulados de docs/03, no mediciones de esas imágenes.

Se añadieron favicon público, errores con salida al Inicio, mensajes vacíos y distinción de empate. La corrección automática del cuello solo modifica la propuesta de IA, nunca la regla RULA.

## Decisiones expresas recibidas de Andy

- Reinicio: borrar únicamente las empresas exactas `DEMO-APLOMO-E1`, `DEMO-APLOMO-E2`, `DEMO-APLOMO-E3` y sus registros dependientes. Pedir escribir `REINICIAR DEMO`; usar administrador de MySQL, nunca la cuenta de la app. Las cuentas y otras empresas, incluida la práctica, se conservan. Si una cuenta depende de una empresa demo, cancelar antes de borrar.
- Cuello IA de −6°: proponer 0° a 10° y mostrar Revisar. Seguir confirma el rango y usa su representante de 5°; el ángulo original permanece disponible. Revisar desaparece al confirmar o cambiar el rango. La medición manual de −6° conserva cuatro puntos por extensión.

## Archivos por punto del encargo

Las rutas son relativas a la raíz del repositorio.

| Punto | Archivos y alcance |
|---|---|
| 0 · Perfil | `src/main/resources/static/js/deteccion.js`: `revisarPerfil`; `scripts/qa_deteccion_campo.cjs`. No se duplicó la revisión en el servidor ni en fotos-campo.js. |
| 1 · Botón enviador | Corrección general de `cuenta.js` ya presente en el commit base `d513e3f`: copia name/value antes de desactivar. Se conserva y se comprueba con `scripts/qa_formularios.cjs`; no se volvió a modificar. |
| 2 · Aislamiento | Nuevo `modelo/Demostracion.java`; `dao/ConsultaDao.java`, `EmpresaDao.java`, `SedeDao.java`, `SalonDao.java`, `ReservaDao.java`, `VisitaDao.java`, `PendienteDao.java`, `SugerenciaDao.java`; `servicio/Evaluaciones.java`, `Calificaciones.java`, `PracticaEvaluacion.java`; nuevo `web/Paso17Test.java` en src/test/java/co/aplomo. |
| 3 · Inicio | `web/InicioEvaluacion.java`, `Router.java`; `static/css/aplomo.css`; `HttpEvaluarAhoraTest.java`, `HttpMarcaTest.java`, `Paso17Test.java`. |
| 4 · Pestañas | `web/PaginasConsultas.java`, `static/css/aplomo.css`, `Paso17Test.java`. Sugerencias accesible desde Pendientes. |
| 5 · Listas | `web/PaginasConsultas.java`, `static/css/aplomo.css`: ubicación en dos líneas, resultado y Puesto sin corte; orden/dirección dentro de Más filtros. |
| 6 · Visita | `web/PaginasVisitas.java`, `static/css/aplomo.css`, `Paso17Test.java`: filas vacías ocultas, Ver reserva y acciones juntas, Evaluar visible. |
| 7 · Reserva | `web/PaginasReservas.java`, `static/css/aplomo.css`, `HttpMarcaTest.java`: dos filas de tres campos y Registrar un lugar compacto. |
| 8 · Consentimiento | `web/ConversacionEvaluacion.java`, `static/css/aplomo.css`: casilla/texto alineados y título sin comillas. |
| 9 · Manual | Nuevo `static/js/marcado-manual.js`; `analisis.js`, `conversacion-evaluacion.js`; `web/PaginasAnalisis.java`, `ConversacionEvaluacion.java`; CSS y nuevo `scripts/qa_paso17.cjs`. |
| 10 · Demo | `instalacion/Demo.java`, nuevo `DemoCompleta.java`, `App.java`, `docs/base_de_datos/datos_demo.sql`, `scripts/cargar_demo.sh` y `.bat`, `Dockerfile`, `.dockerignore`, `docker-compose.yml`; tres JPG en `static/img/demo/`; `DemoTest.java`, `DatosDemoPrueba.java`, `HttpDatosDemoTest.java`, `HttpMarcaTest.java`; `README.md`, `INSTALAR.md`. Los scripts de reinicio existentes conservan su confirmación y límite de alcance. |
| 11 · Pulido | `web/Html.java`, `Router.java`, `InicioEvaluacion.java`, `PaginasConsultas.java`, `PaginasUsuarios.java`, `PaginasPendientes.java`, `PaginasSugerencias.java`, `plantillas/login.html`; CSS, `analisis.js`, `conversacion-evaluacion.js`, `posturas-campo.js`; `docs/ENDPOINTS.md`, `Paso17Test.java`, `qa_paso17.cjs`. |
| 12 · Cuello IA | `static/js/posturas-campo.js`: `sugerenciaAutomatica`; `conversacion-evaluacion.js`: propuesta y confirmación; `analisis.js`: origen IA/manual; `qa_paso17.cjs` y `Paso17Test.java`. |
| Entrega | `AGENTS.md`, `README.md`, `INSTALAR.md`, este informe. |

Prefijos abreviados de la tabla: Java de producción está en `src/main/java/co/aplomo/`; JS/CSS en `src/main/resources/`; pruebas Java en `src/test/java/co/aplomo/`.

## Requisitos y comprobación

| Requisito | Archivo y función que lo respalda | Prueba |
|---|---|---|
| RF-011 · Reserva | `PaginasReservas` / formulario, `ReservaDao.listar` y catálogos | HttpReservasTest; Paso17Test; formulario real de QA |
| RF-013a/013b · Visitas y permisos | `InicioEvaluacion.tarjeta`, `PaginasVisitas`, `VisitaDao` | HttpVisitasTest, HttpEvaluarAhoraTest, Paso17Test y permisos de la suite |
| RF-014/015/016 · Contexto, consentimiento y fotos | `ConversacionEvaluacion`, flujo existente de Evaluaciones/Fotos | HttpRecorridoTest, HttpRevisionEvaluacionTest, HttpPracticaEvaluacionTest, guardado real de dos posturas en QA |
| RF-017 · Ángulos | `deteccion.revisarPerfil`, `marcado-manual`, `analisis` y cálculo existente | qa_deteccion_campo; qa_paso17; doce imágenes con modelo real; cinco marcas y continuación en navegador |
| RF-018/019/020/021/027 · RULA y resultado | Motor existente sin cambios; `DemoCompleta.entrada`, confirmación del recorrido | MotorRulaTest y demás pruebas RULA; DemoTest; cuello manual −6° sigue dando 4; guardado de dos posturas con 4/4 y empate |
| RF-022/023a/023b · Seguimiento | `DemoCompleta.cargar` usa Pendientes y Sugerencias existentes | DemoTest, HttpDatosDemoTest; conteos en Docker |
| RF-024 · Consultas | `ConsultaDao.filas/catalogos`, `PaginasConsultas.filtros` | Paso17Test con once secciones; HttpConsultasTest; filtros de Reservas en navegador |
| RF-025/026 · Calificaciones e informe | `Calificaciones.cargar`, `DemoCompleta.cargar` y servicios existentes | HttpInformesTest, CalificacionesTest, HttpDatosDemoTest; informe Humboldt en navegador |
| RNF-003/007 · Respuesta, errores y lenguaje | `cuenta.js`, `Router.enviar`, `Html.error`, mensajes y guía | qa_formularios; Paso17Test favicon/404; guardado y avisos en navegador |
| RNF-004/006/009 · Permisos, fotos e historial | Servicios existentes y filtro central de práctica; `Demo.vaciar/limpiarFotos` | Suite de permisos y fotos; DemoTest; reinicio aislado con ROLLBACK conserva empresa ajena, práctica y cuentas |
| RNF-008 · Compatibilidad | Sin nuevos frameworks ni dependencias | Navegador integrado únicamente en esta entrega; iPad físico y matriz de navegadores pendientes |

## Números comprobados

- `TZ=America/Bogota ./mvnw test`: **930 ejecutadas, 930 aprobadas, 0 fallos, 0 errores, 0 omitidas**.
- `node --test scripts/qa_*.cjs`: **72 ejecutadas, 72 aprobadas, 0 fallos**.
- QA de interfaz: navegador integrado, ventana real de **1280 × 720**. Reservas y Nueva reserva: **0 px de desbordamiento horizontal**. No se certifica una matriz de otros tamaños con esta comprobación.
- Consentimiento: texto alineado a la izquierda y separación casilla/texto de **12 px**.
- Nueva reserva: **3 columnas** para Empresa/Sede/Salón y **3** para Fecha/Inicio/Fin. Plegables cerrados de Registrar lugar: **62 px** cada uno, incluido borde y relleno.
- Guardado de práctica: primera postura marcada a mano con cinco puntos, segunda con detección. Resultado **4 y 4**, sin etiqueta de postura crítica y con «Mismo puntaje». Ambas guardadas; se regresó a la visita con «Evaluación guardada».
- Indicadores antes y después de crear y guardar práctica: **3 / 5 / 24 / 4**, sin cambios; semáforo operativo **4 / 4 / 4**. La visita de práctica solo aparece en Continuar práctica.
- Consola del recorrido comprobado: **0 errores capturados**.

### MediaPipe real, imágenes originales

| Imágenes | Resultado |
|---|---|
| 01, 02, 03, 04, 07, 08, 09 y muestra_perfil.jpg | Aceptadas sin bloqueo |
| 06 · Tres cuartos | Advertencia de ángulo dudoso, **sin bloqueo**, permite continuar |
| 05 y 10 · Frente | Bloqueadas con «Parece una foto de frente» |
| 11 · Mano | Bloqueada con «No se ve una persona» |

### Docker, base aislada

Construcción correcta y perfil `demo` ejecutado dos veces: **24 evaluaciones / 24 fotografías / 12 puestos con dos criterios / 4 visitas Finalizadas con calificación**. E1 y E2 tienen seis evaluaciones cada una; E3 tiene doce entre dos sedes. Todas las empresas tienen los tres colores. **Dos cuentas antes y dos después**; no se alteraron. No se crearon visitas Finalizadas vacías nuevas.

Reinicio comprobado en una transacción aislada con administrador de MySQL y después ROLLBACK: se quitaron únicamente E1/E2/E3 dentro de esa transacción; práctica, empresa ajena y cuentas permanecieron. Después del ROLLBACK reaparecieron las 24 evaluaciones. No se ejecutó un reinicio sobre la base nativa. Los contenedores temporales se cerraron conservando volúmenes.

## Lo que NO hice y límites

- No cambié esquema SQL original, `co.aplomo.rula` ni `EvaluacionesWeb.java`, que contiene POST `/evaluaciones/calcular`. No cambié NEUTRO ni tablas RULA.
- No añadí tolerancia al tronco: no se confirmó ese caso de ruido en esta QA. No se modificó la regla exacta para puntos manuales.
- No tomé capturas ni descargué navegadores. No se certificó iPad físico/tacto, Safari ni Chrome/Edge/Firefox en este paso.
- Los datos de navegador estuvieron solo en `rula_ergonomia_pruebas`, dentro de una transacción revertida. Docker usó su base aislada. El demo completo no se carga automáticamente en la base real al iniciar la aplicación.
- El punto 1 ya estaba resuelto y probado; se conservó sin reescribirlo. No se borraron datos reales, documentos, configuración ni cuentas.
- Las dos posturas conservan el mecanismo de guardado existente, con acuses independientes y recuperación, no una nueva transacción conjunta.

## Checklist

- ✅ Suite Maven: 930/930; JavaScript: 72/72.
- ✅ Inicio, consentimiento, foto/ángulos, resumen, visita, Empresas, Reservas, Nueva reserva e informe revisados en navegador integrado.
- ❌ Comparación visual con capturas: omitida expresamente por Andy; no afirmar igualdad exacta con el mock.
- ❌ Matriz nueva 390/810/1080/1366 y prueba táctil física: no realizadas en esta entrega. 1280 × 720 comprobado sin desborde en las listas/formulario revisados.
- ✅ Permisos de Usuario y acceso directo cubiertos por la suite completa; aislamiento de práctica nuevo cubierto en once consultas y calificación por ID. No se repitió un recorrido interactivo como Usuario.
- ✅ Textos nuevos en español con tildes; marca y botones marfil conservados.
- ✅ Cero errores de consola capturados en el recorrido comprobado.
- ✅ No se incluye `config/db.properties`, `.env`, contraseñas, fotos privadas ni `fotos/` en el commit.

## Cómo probarlo

1. Abre la app e ingresa como Administrador. Si acabas de instalar, cambia primero la contraseña temporal.
2. En Inicio, abre Probar evaluación y elige Planta u Oficina. Al volver a Inicio debe aparecer Continuar práctica; los indicadores operativos no deben aumentar.
3. En una foto de perfil, prueba Detectar puntos. Para el plan manual, pulsa Calcular manualmente y toca hombro, codo, muñeca, cabeza y cintura, en ese orden. Comprueba Anterior/Reiniciar, arrastra una marca, responde la muñeca y pulsa Seguir.
4. Completa la segunda postura y guarda. Si empatan, el resumen dice Mismo puntaje; si difieren, solo la mayor lleva Postura más crítica.
5. Revisa las pestañas y Más filtros de las listas; en la visita comprueba Ver reserva, Evaluar y las acciones de cierre.
6. Para cargar resultados ficticios completos, usa voluntariamente `bash scripts/cargar_demo.sh` en Mac o `scripts\cargar_demo.bat` en Windows. En Docker: `docker compose --profile demo run --build --rm demo`. Luego abre las empresas demo, sus visitas e informes. Repetir la carga conserva los registros existentes.
7. Solo si deseas borrar y recargar esas tres empresas demo, usa `scripts/reiniciar_demo` de tu sistema. Exige administrador de MySQL y escribir REINICIAR DEMO. Cierra la app antes. Si hay cuentas vinculadas a una empresa demo, se cancela.

## Git

Base de trabajo: `d513e3f` (ya publicado). Nuevo commit: «Paso 17: práctica aislada, guía manual y demo completa». Repositorio verificado: `https://github.com/Andybena2025/Aplomo-app.git`, rama `main`. El hash final y la confirmación de publicación se entregan al cerrar este paso; puede consultarse con `git log -1 --oneline`.

Esperar el OK de Andy antes de comenzar otro paso.
