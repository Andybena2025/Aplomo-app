# Paso 14 final · Evaluación por revisión

Encargo de Andy del 14 de noviembre de 2026. Sustituye el encargo anterior de rediseño; no sustituye el informe histórico de detección MediaPipe.

## Comprobación técnica — cinco líneas
1. Una evaluación conserva varias fotografías y análisis sin cambiar sus tablas.
2. Cada análisis pertenece a una foto y un lado; no hay una relación independiente segmento–foto.
3. Se mantiene un análisis por perfil Para medir y lado, alimentado desde la misma pantalla de revisión.
4. Frente y detalle aportan evidencia al mismo análisis; la vista frontal no se presenta como una medición lateral ni permite inferir automáticamente las torsiones.
5. Se conservan campos, valores y cálculo del servidor; las propuestas requieren confirmación explícita.

## Qué se entregó

Siete fases estables: consentimiento y permiso de fotos → contexto → condiciones → lado → perfil/revisión → actividad y carga → resultado. La barra conserva el avance máximo, incluso al revisar respuestas o añadir el otro lado. Sin fotos se conserva el recorrido válido sin puntaje.

Contexto reúne tarea visible, seis atajos de texto, momento con los textos exactos del encargo y los cuatro tiempos existentes. Solo Cambiar/Otra abre escritura. Consentimiento, fotos, momento, tiempo, condiciones y lado no se responden por defecto; los recuerdos se identifican y exigen Continuar.

La revisión reúne los siete segmentos. Los cinco ángulos geométricos y sus rangos los devuelve el servidor. Giro y piernas requieren observación explícita: no se inventan desde una foto lateral. Cada fila se abre en línea con sus ilustraciones y ajustes opcionales, sin diálogo ni ficha Ninguno. El botón conjunto confirma las mediciones; no hay confirmación individual obligatoria de las propuestas normales. Revisar señala falta de datos, menos de 3° de un borde o conflicto entre mediciones. El pulso ocurre una vez; CSS lo desactiva con reduced-motion.

El perfil es principal. Frente, detalle y extras se conservan como fotos de apoyo en la misma evaluación, con pestañas. Al consultar el frente, el evaluador puede aplicar ajustes al mismo análisis; el chip distingue el ángulo del Perfil de un ajuste observado en Frente. El detalle puede ofrecer una medición alternativa de muñeca si sus puntos son visibles, calculada en el servidor y adoptada solo por elección explícita. Si falta una persona o es otro perfil, permanece el respaldo manual, un segmento a la vez. No se adivinan ajustes ni torsiones con MediaPipe.

Actividad y carga se muestran una debajo de otra, una vez por lado, con sus pistas y valores originales. No hay avance automático. El segundo lado es opcional desde Resultado: pide su propio perfil y comienza sin mediciones del primero. Añadir análisis utiliza las fotos ya guardadas y conserva el criterio del contexto existente; se comprobó su envío y el aviso Análisis guardado. No añade archivos a una evaluación existente: conserva el contrato anterior de ese formulario.

## Pruebas

- `TZ=America/Bogota ./mvnw test`: **842 ejecutadas, 842 aprobadas, 0 fallos, 0 errores, 0 omitidas**. Resultado final: BUILD SUCCESS, 14 nov 2026 09:42, duración 1 min 16 s. El bloque A también pasó la suite base de 833.
- `node --test scripts/qa_revision_eventos.cjs`: **10 aprobadas**. Exclusión de dos acciones simultáneas, liberación tras error de red o actualización visual, límites de revisión y discrepancias. No equivalen a eventos táctiles físicos.
- Nueve casos nuevos de `HttpRevisionEvaluacionTest`: contexto y valores; siete filas/ajustes sin diálogo ni selección inicial; cuatro viñetas y esfuerzo único; cuatro variantes de contexto incompleto rechazadas; perfil + frente sin duplicar evaluación/análisis; dos análisis con esfuerzo conservado y puntaje calculado por servidor, ignorando un total falso del cliente.
- Se adaptaron las aserciones de presentación de HttpRecorridoTest, HttpCierreTest, HttpCuerpoTest y FasesEvaluacionTest. `HttpEvaluacionesTest` —nombre real, en plural— se conserva y pasa. No se modifica ninguna prueba ni archivo del paquete `co.aplomo.rula`.
- Navegador integrado: recorridos completos Oficina y Planta, detección del perfil S6, frente y detalle, confirmación, esfuerzo, resultado y guardado. Cero errores de consola en ambos.
- Adicionales: dos perfiles/dos lados con resultado y guardado; el segundo reutilizó deliberadamente la misma foto S6 para activar otroPerfil y probar el respaldo manual. Es un ensayo funcional, no una evaluación ergonómica válida del lado contrario. Se verificó que el segundo lado no muestra medidas del primero. Pestañas Frente/Detalle, ajuste en línea y Añadir análisis también comprobados.
- QA exclusivamente en `rula_ergonomia_pruebas`. Servidores temporales cerrados, ROLLBACK confirmado. No se cargó ni reinició la demo en la base real. Sin descargas de navegadores.

## Comparación medida

| Modo | Antes (51d6efb) | Después | Acciones antes → después |
|---|---|---|---:|
| Planta | 30 pantallas + 10 ventanas emergentes | 7 pantallas, ninguna ventana | 56 → 29 |
| Oficina | 30 pantallas + 10 ventanas emergentes | 7 pantallas, ninguna ventana | 56 → 29 |

Misma tarea, un lado, perfil S6 + frente + detalle, desde Evaluar ahora hasta guardar, sin memoria en cada recorrido contado. Las acciones incluyen los clics/selecciones de la app y abrir/cerrar una fila para inspección; excluyen login, espera y selección de archivos en el diálogo del sistema. Los registros usan la clave `toques`, pero se midieron con click/check en el navegador integrado, **no con tap/hasTouch**. Antes se ejecutó una copia temporal del código base, sin modificar el repositorio. No se infiere de esta comparación un tiempo garantizado para un evaluador nuevo.

## Medidas de ventanas

80 registros: diez estados × Oficina/Planta × cuatro ventanas CSS exactas. En todos: `scrollHeight − innerHeight = 0`, desbordamiento lateral = 0 y ventanas de diálogo abiertas = 0. La lista de revisión puede desplazarse internamente con Confirmar mediciones fijo abajo.

Cada celda es **página vertical / lateral / lista interna**, en píxeles; máximo de ambos modos. Datos completos: [medidas-finales.json](evidencia_paso14_revision/medidas-finales.json).

| Pantalla/estado | 1366×950 | 1024×1290 | 1180×740 | 820×1100 |
|---|---:|---:|---:|---:|
| Consentimiento | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Contexto | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Condiciones | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Lado | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Revisión sin foto | 0 / 0 / 92 | 0 / 0 / 0 | 0 / 0 / 302 | 0 / 0 / 0 |
| Revisión | 0 / 0 / 77 | 0 / 0 / 0 | 0 / 0 / 287 | 0 / 0 / 0 |
| Revisión con apoyo | 0 / 0 / 77 | 0 / 0 / 0 | 0 / 0 / 287 | 0 / 0 / 0 |
| Revisión abierta | 0 / 0 / 1034 | 0 / 0 / 1015 | 0 / 0 / 1260 | 0 / 0 / 1311 |
| Esfuerzo | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Resultado | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |

Con la fila de Brazo abierta, el desplazamiento interno llega a 1034/1015/1260/1311 px respectivamente. Este valor incluye ilustraciones y ajustes dentro de la lista; no es desplazamiento de la página. Frente/Detalle en pestaña y un ajuste abierto se comprobaron además a 1366×950 con 0 vertical y 0 lateral. El resultado de dos lados también dio 0/0 a ese tamaño. Las últimas correcciones de contexto conservado, chip de fuente y envío no cambian la distribución CSS de la matriz.

## Continuar en iPad: alcance real

**La causa del fallo observado por Andy en su iPad no se ha confirmado.** El recorrido anterior incluía temporizadores de avance automático y guardas de ocupado separadas: existían rutas en las que un evento durante transición podía ignorarse. Es una hipótesis del código, no una reproducción del dispositivo.

El nuevo recorrido elimina el avance automático, comparte un único bloqueo de acción, desactiva el botón mientras calcula y libera el estado en finally incluso si falla la petición o la actualización visual. Las pruebas JS ejercitan concurrencia y reintento, y los recorridos del navegador verifican actividad/carga con memoria y ambos lados. También se corrigieron dos fallos encontrados en QA: datos visuales del primer lado al abrir el segundo, y falta de análisis en el envío de Añadir análisis porque el permiso de fotos no estaba seleccionado en sus secciones ocultas.

**Pendiente:** la combinación solicitada de Playwright `hasTouch`, `tap`, con/sin reduced-motion y toque durante transición. El navegador integrado expone click/check y tamaños; no esos controles de emulación. Playwright está disponible localmente, pero sus navegadores no están instalados. Se respetó la instrucción de Andy de no descargar. No se afirma resuelto un fallo táctil físico ni se sustituye ese ensayo por clics. Ahora no existe avance automático en este recorrido.

## Trazabilidad de requisitos

| Requisito | Implementación | Comprobación |
|---|---|---|
| RF-014 puesto/contexto | RevisionEvaluacion.nueva/construir; revision-evaluacion.js contextoListo/motivo | HttpRevisionEvaluacionTest contexto y cuatro rechazos; HttpRecorridoTest valores/motivo/puesto ajeno |
| RF-015 condiciones | RevisionEvaluacion.construir; seis condiciones existentes, nota y Ninguna | HttpRecorridoTest persistencia/nota; suite HttpEvaluacionesTest |
| RF-016 fotos | revision-evaluacion.js recibir/evidencias; validación y guardado existentes sin cambios | HttpRevisionEvaluacionTest perfil + frente y una evaluación; HttpEvaluacionesTest tipo/tamaño/consentimiento |
| RF-017 ángulos | revision-evaluacion.js medirPropuestas/medirDetalle/refrescar; analisis.js lienzo | HttpAnalisisTest; HTTP parcial existente; perfil S6 y respaldo manual en UI |
| RF-018 cálculo | MotorRula y tablas MySQL existentes, sin modificación | Suite RULA original; HttpAnalisisTest diez casos y rechazo de total adulterado |
| RF-019 actividad/carga | RevisionEvaluacion.construir; revision-evaluacion.js esfuerzoPorLado/acción de Continuar | HttpRevisionEvaluacionTest esfuerzo conservado en dos análisis; ambos lados y memoria en UI |
| RF-020 semáforo | analisis.paso6 y PaginasAnalisis.resultado existentes | HttpAnalisisTest y dos guardados completos; resultado 7 · Rojo · Nivel 4 |
| RNF-002 uso claro | contexto compacto/revisión/confirmación conjunta | Comparación 30→7 pantallas; estudio con evaluadores nuevos pendiente |
| RNF-003 carga | accion-evaluacion.js crearAccionEvaluacion; botones; detección existente | Diez pruebas JS, recorridos completos y reintento tras fallo |
| RNF-004 seguridad | controles del servidor y CSRF existentes | HttpCierreTest.usuarioNoPuedeAbrirNiGuardarElNuevoRecorrido; HttpRecorridoTest puesto/visita ajenos; suite de seguridad |
| RNF-006 fotos privadas | rutas/autorización existentes, no modificadas | HttpEvaluacionesTest y HttpAnalisisTest, rechazo a Usuario |
| RNF-007 mensajes | RevisionEvaluacion y revision-evaluacion.js aviso/refrescar/recibir | Aserciones HTML y observación de filas/fallo/perfil contrario |
| RNF-008 compatibilidad | CSS adaptable y movimiento reducido | Matriz del navegador integrado; Safari/iPad físico y otros navegadores pendientes |
| RNF-010/011 consistencia/mantenimiento | motor y servidor únicos, revisión separada de la puntuación | Suite original intacta y 842 pruebas finales; git diff sin paquete rula |

## Lo que NO se hizo

- No se alteran esquema, rutas ni POST /evaluaciones/calcular, motor, tolerancia 5°, tablas RULA, valores enumerados, campos guardados ni colores de botones/acento.
- No se deducen torsiones/ajustes tridimensionales desde una foto plana; frente y detalle apoyan decisiones explícitas. La procedencia por segmento se muestra en pantalla; el esquema no incorpora una relación nueva segmento–foto. En fotos nuevas usadas como apoyo se añade una referencia legible a su descripción.
- No se certifica el objetivo de diez minutos: requiere un evaluador nuevo realizando el recorrido sin ayuda.
- No se completó el ensayo táctil con hasTouch/tap/reduced-motion, ni una ronda nueva en Safari, Chrome, Edge o Firefox. No se descargaron copias.
- Esta matriz corresponde a las cuatro ventanas pedidas en el encargo actual. No se repite la matriz anterior completa de 390/810/1080/1366 px.

## Checklist

- ✅ 842 pruebas Maven, 10 JS; cero fallos.
- ✅ Contexto, lado, revisión abierta/Revisar, esfuerzo y resultado observados y capturados; marca marfil conservada. El diseño del encargo actual sustituye el recorrido anterior del mock.
- ✅ 80 medidas a 1366×950, 1024×1290, 1180×740, 820×1100, sin desplazamiento de página/lateral. ❌ No se repitió toda la matriz histórica 390/810/1080/1366.
- ❌ Funciona con el dedo: sin certificación física ni hasTouch; no confundir con click/check.
- ✅ Usuario rechazado por dirección y POST en las pruebas HTTP del recorrido; fotos protegidas en suite existente.
- ✅ Textos nuevos en español, tildes, mensajes de revisión y respaldo observados.
- ✅ Cero errores de consola en los dos recorridos completos y los adicionales de la pestaña de Oficina.
- ✅ Ningún archivo de config/db.properties ni fotos/ incluido; evidencia solo de ensayo.

## Cómo probarlo

1. Con la aplicación iniciada, entrar como Administrador y abrir Evaluar ahora en una visita En curso con puestos activos.
2. Elegir un puesto; confirmar consentimiento físico y permiso de fotos. En Contexto revisar la tarea visible, elegir momento y tiempo. Elegir condiciones o Ninguna.
3. Elegir lado. Subir un perfil de la misma postura; esperar el aviso de detección. Revisar los ángulos; resolver solo las filas Revisar. Giro y piernas se eligen por observación.
4. Abrir una fila para ver rangos y ajustes opcionales. Agregar frente/detalle y consultar sus pestañas; no debe abrir otra evaluación. Confirmar mediciones una sola vez.
5. Elegir actividad y carga; Continuar debe calcular y mostrar resultado. Guardar y volver, o evaluar el otro lado con un perfil propio antes de guardar.
6. Repetir con Oficina. Comprobar los recuerdos identificados; no deben recordar ángulos ni ajustes. Abrir una evaluación guardada y Añadir análisis; el criterio original debe permanecer y el guardado añadir un análisis, no otra evaluación.
7. Ensayo táctil pendiente: repetir en iPad, reducir movimiento activado/desactivado, memoria, dos perfiles/dos lados y toques repetidos en Continuar; informar cualquier bloqueo. No descargar navegadores para este ensayo sin nueva instrucción.

## Archivos y Git

- Producción: `web/RevisionEvaluacion.java`, `web/PaginasEvaluacion.java`, `web/PaginasAnalisis.java`, `web/RecorridoCuerpo.java` (solo exposición de ilustraciones), `static/css/aplomo.css`, `static/js/revision-evaluacion.js`, `revision-reglas.js`, `accion-evaluacion.js`, `analisis.js`, `evaluacion.js`, `memoria-evaluacion.js`.
- Pruebas: `HttpRevisionEvaluacionTest`, `HttpRecorridoTest`, `HttpCierreTest`, `HttpCuerpoTest`, `FasesEvaluacionTest`; `scripts/qa_revision_eventos.cjs`.
- Documentación: este informe, `AGENTS.md`, solo fila 14 y conteo del `README.md`, conteo de `INSTALAR.md`, `docs/evidencia_paso14_revision/` (capturas, JSON y lectura de medidas).
- Commit A: `31ad27e Paso 14-A: contexto, revisión por excepción y esfuerzo por lado`.
- Commit B previsto para esta entrega: `Paso 14-B: pruebas, iPad y bloqueo del botón Continuar`. La respuesta final incluye su SHA y confirmación del push efectivo tras verificar origin/main.

Se detiene el trabajo después de esta entrega para esperar el OK de Andy.
