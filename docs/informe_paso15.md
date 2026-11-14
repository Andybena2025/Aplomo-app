# Paso 15 · Evaluación como conversación guiada

Encargo final de Andy del 14 de noviembre de 2026. Reemplaza el intento de grupos interrumpido: se descartaron únicamente sus dos archivos sin commit y se sincronizó main limpio antes de empezar.

## Qué cambió

Nueve pantallas: permiso, conversación inicial, condiciones, lado, perfil/ángulos, brazo/muñeca, cuello/tronco/piernas, carga y resultado. Conserva consentimiento físico y la salida sin fotos/sin puntaje cuando la persona no permite fotografías. Con fotos, el perfil es obligatorio; sus filas no aparecen antes de subirlo.

El componente de respuesta es propio: un botón de al menos 52 px abre opciones dentro de la página; una respuesta deja su texto y ✓ visibles, cierra su lista y presenta la siguiente. Solo una lista está abierta. Se puede editar una respuesta sin perder las otras. Hay foco visible y se comprobó Enter para abrir/elegir. Seguir aparece al completar la pantalla; antes indica la pregunta pendiente. No hay pestañas ni ventanas emergentes de preguntas; el selector de archivos del navegador es el normal del sistema.

Textos exactos de P1/P2, B1–B7, C1–C3, K1–K3; etiquetas Pregúntale/Míralo, siluetas y muestras existentes. El perfil se detecta al subirlo y las cuatro mediciones del servidor se confirman juntas. La muñeca se responde con opciones; medir sus tres puntos queda disponible. Frente opcional como referencia visual; detalle solo evidencia, sin detección ni cálculo propio. Ambos pertenecen a la misma evaluación. El lado adicional conserva respuestas separadas y su propio perfil. Añadir análisis reutiliza las fotos guardadas y no cambia el contexto existente.

Se corrigieron dos interferencias heredadas encontradas en las capturas: columnas del recorrido anterior que separaban preguntas y barra de Seguir que tapaba respuestas. Ahora la conversación siempre tiene una columna y la acción queda después de los campos. Encabezado con título, nombres del progreso y lado permanece visible. El avance nativo azul de Safari se sustituyó explícitamente por marfil, comprobado en Safari.

## Correspondencias y decisiones simples

Todas las correspondencias están en `web/MapeosCampo.java`, no en el paquete RULA. La página entrega al navegador los valores de ese catálogo; JavaScript no calcula puntajes.

| Respuesta | Valor existente |
|---|---|
| P1, cinco bandas | 60 / 180 / 300 / 420 / 540 minutos |
| P2, momento concreto | MAS_INADECUADA |
| P2, No sabe | MAYOR_EXPOSICION |
| Muñeca recta / un poco / mucho | 5° / 8° / 25° manuales; 5° conserva NEUTRO |
| B1–B4 y B6 | Sí/No a hombroElevado, brazoSeparado, brazoApoyado, antebrazoCruza, munecaDesviada |
| B7 | MEDIO / EXTREMO |
| C1/C2 | Girada, inclinada o ambas → solo los ajustes observados |
| C3 | piernasApoyadas Sí/No |
| Nada de peso o menos de 2 kg | MENOS_2KG_INTERMITENTE |
| 2–10 kg | INTERMITENTE si Ninguna; ESTATICA_O_REPETITIVA en las otras dos respuestas |
| Más de 10 kg | Mismo criterio anterior, con las dos categorías MAS_10KG |
| Golpe/tirón Sí | GOLPES_O_FUERZAS_BRUSCAS, con cualquier banda de peso |

Decisiones de implementación, sin atribuirlas a Andy:

- P2 conserva los dos textos de momento anteriores y agrega No sabe. Los dos momentos concretos usan MAS_INADECUADA, como pide el nuevo encargo; únicamente No sabe usa MAYOR_EXPOSICION.
- Un Sí a cabeza/espalda torcida o inclinada abre una aclaración en línea: Girada, Inclinada hacia un lado o Las dos. Evita sumar dos ajustes por una sola alteración. No cambia la pregunta exacta inicial.
- Nada de peso comparte la categoría de menos de 2 kg; la actividad sigue sumándose aparte por el motor.
- Se conserva la salida válida sin permiso de fotos; no obliga a fotografiar a quien no acepta ni crea un puntaje.
- La postura se genera con nombre del puesto y lado, limitada a 150 caracteres; con dos lados dice ambos lados. El servidor genera el motivo corto a partir del criterio. Los formularios anteriores conservan sus validaciones para no romper compatibilidad.
- Se recuerdan tiempo, momento y carga de la misma visita/cuenta, con etiqueta y confirmación; no ángulos ni ajustes.
- Se reutilizan las siluetas disponibles; no faltó una imagen requerida para los segmentos. En pantallas sin segmento se reutiliza la silueta neutra. No se crearon imágenes nuevas.

## IA en Safari / iPad: diagnóstico y alcance real

La versión anterior sí detectó la muestra en Safari del Mac: 2,38 s. No se reprodujo allí el fallo del iPad. El código anterior inicializaba el modelo completo en la primera foto, enviaba su tamaño original y permitía esperas largas; no había cierre al abandonar la página. Son hallazgos del código, **no una causa confirmada del fallo físico de Andy**.

Cambios comprobables: preparación anticipada después del permiso, una instancia compartida, modelo de complejidad 0, imagen de inferencia limitada a 1280 px conservando proporción y coordenadas normalizadas, ocho segundos programados para el trabajo y salida manual sin mensajes técnicos. Al fallar cierra la instancia y evita inicializaciones repetidas; una respuesta tardía no recupera un modelo cancelado. Al salir de la página también cierra el modelo. El botón Detectar puntos se oculta tras fallo de disponibilidad; las preguntas/manual siguen disponibles. El canvas muestra objetivos de 44 px y mantiene el arrastre por pointer events; en vertical usa todo el ancho de su contenedor.

Safari Mac 26.6.2 detectó con el código ajustado en 2,73 s y el diagnóstico capturó cero errores JavaScript. Se comprobaron carga de recursos del CDN, inicialización, inferencia y resultado. Además se recorrió la app en Safari hasta subir S6 y ver puntos y los cuatro ángulos devueltos por Java. El cambio de modelo no se presenta como una mejora de precisión: las propuestas deben revisarse.

Referencia técnica del modelo y complejidad: [documentación original de MediaPipe Pose](https://github.com/google-ai-edge/mediapipe/blob/master/docs/solutions/pose.md). Se mantuvo el CDN/versionado existente.

**No pude reproducir el fallo en un iPad físico, probar arrastre con su dedo ni medir allí la memoria WASM.** No se afirma causa confirmada ni certificación de ese dispositivo. Los límites/cierre/cancelación se probaron con reloj simulado y el funcionamiento visual con Safari Mac y Chromium integrado. No se descargaron navegadores ni se instalaron dependencias.

## Pruebas y evidencia

- `TZ=America/Bogota ./mvnw test`: **878 aprobadas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS. Última ejecución: 14 nov 2026, 11:33, 1 min 15 s. Los retoques posteriores de cierre del modelo y color nativo se comprobaron con las pruebas JS y Safari.
- `node --test scripts/qa_deteccion_campo.cjs scripts/qa_revision_eventos.cjs`: **15 aprobadas**. Incluyen inicialización única, entrada reducida, timeout/cancelación, modelo tardío, imagen pendiente, cierre al salir, concurrencia y liberación de acciones.
- 30 pruebas nuevas de MapeosCampo: cinco tiempos, tres muñecas, tres momentos, doce combinaciones de peso/frecuencia (también golpes), seis orientaciones y rechazo de datos ausentes.
- Seis casos HTTP nuevos comprueban los cinco tiempos guardados, postura/lado, motivo corto y rechazo del tiempo antiguo en el recorrido nuevo. Las adaptaciones de tests anteriores son de HTML, textos y orden; las aserciones de cálculo, datos, permisos y seguridad se conservan. Ningún archivo ni prueba de co.aplomo.rula cambió.
- UI: Planta guardada con cuatro fotos y dos análisis (perfil derecho, detalle y frente de apoyo, otro perfil); Oficina guardada con un perfil, omitiendo fotos opcionales. También guardados Añadir análisis y la salida sin fotos. En el ensayo del lado contrario se repitió intencionalmente S6 para activar el respaldo manual: prueba funcional, no una evaluación clínica válida de ambos lados.
- Recuerdos identificados, edición sin perder otras respuestas, teclado Enter, condicionales y máximo una lista abierta comprobados. La opción de tres puntos de muñeca también se probó: sustituyó la elección anterior por la medida actual del perfil (2,35°), con texto claro y sin errores de consola. Cero errores de consola en los recorridos del navegador integrado.
- Todo el QA de datos en `rula_ergonomia_pruebas`, con ROLLBACK al cerrar los servidores. No se cargó ni reinició la base real.

| Ventana CSS | Estados medidos | Desbordamiento lateral máximo | Campo mínimo | Listas abiertas máximo |
|---|---:|---:|---:|---:|
| 834×1194 | 18 | 0 px | 52 px | 1 |
| 1366×768 | 18 | 0 px | 52 px | 1 |
| 390×844 | 18 | 0 px | 52 px | 1 |

Hay 54 capturas de página completa y tres vistas adicionales de tamaño exacto. La conversación crece verticalmente al mostrar respuestas; el encargo actual no exige cero desplazamiento vertical. [Índice de capturas y medidas](evidencia_paso15/README.md).

## Requisitos y comprobación

| RF/RNF | Archivo/método | Evidencia |
|---|---|---|
| RF-014 | ConversacionEvaluacion.nueva/pagina; conversacion-evaluacion.js contexto | HTTP de tiempos/postura/motivo y recorrido guardado |
| RF-015 | ConversacionEvaluacion.pagina, condiciones existentes | HttpRecorridoTest y HTTP de condiciones; captura condiciones |
| RF-016 / RNF-006 | muestras/recibir; autorización y fotos del servidor existentes | Perfil obligatorio, apoyo sin otro análisis, suite de tipo/tamaño/permiso |
| RF-017 | analisis.js lienzo; conversacion-evaluacion.js medir; MapeosCampo.MUNECA/orientacion | HTTP parcial sin cambio, pruebas de mapeo, perfil detectado y respaldo |
| RF-018 / RNF-010 | MotorRula existente, sin modificación | Suite original, diez casos y rechazo de puntaje del cliente |
| RF-019 | MapeosCampo.carga y traslado por lado | Doce combinaciones, golpes, dos lados y carga recordada |
| RF-020 | Resultado existente del servidor | Guardados y capturas de resultado |
| RNF-002/007 | componente pregunta/poner/desplegar; texto de campo | Editar conserva respuestas, Enter, estado Falta responder y capturas |
| RNF-003 | deteccion.js preparar/detectarPuntos/cerrar; acción compartida | Cinco tests de IA/ciclo de vida; límite ocho segundos |
| RNF-004 | permisos/CSRF del servidor existentes | HttpCierreTest y HttpRecorridoTest rechazan Usuario por URL/POST |
| RNF-008 | aplomo.css; Safari y navegador integrado | 54 medidas y prueba WebKit Mac; iPad físico pendiente |
| RNF-011 | MapeosCampo y componentes compartidos | Mapeos centralizados; sin nuevas dependencias |

## Checklist y pendientes

- ✅ 878 pruebas Maven y 15 JS aprobadas.
- ✅ Encargo actual observado con capturas y estilo Obsidian & Ivory; reemplaza la galería/recorrido anterior del mock.
- ✅ 390 / 834 / 1366 px sin desplazamiento lateral. No se repitieron los anchos históricos 810 / 1080 en este encargo.
- ❌ Certificación física con dedo/iPad y consumo real WASM: pendiente. No sustituirla por clics de Mac.
- ✅ Usuario rechazado en pruebas HTTP de permisos; servidor sigue siendo responsable de autorizaciones.
- ✅ Textos exactos en español, foco visible y respuesta marcada con texto/✓.
- ✅ Sin errores de consola en el recorrido integrado; diagnóstico de Safari sin errores capturados. No se hizo un recorrido completo de resultado/guardado en Safari.
- ✅ Sin esquema, motor, POST /evaluaciones/calcular, colores nuevos, imágenes de producción nuevas ni dependencias añadidas. Sin credenciales/configuración/fotos privadas en Git.
- ❌ Chrome/Edge/Firefox externos e iPad físico no disponibles para esta ronda; sin descargas por la instrucción vigente.

## Cómo probarlo

1. Abrir APLOMO como Administrador y elegir un puesto desde Evaluar ahora.
2. Confirmar consentimiento y permiso de fotos; contestar horas y momento. Abrir de nuevo una respuesta y cambiarla: la otra permanece.
3. Elegir condiciones o Ninguna y el lado. Subir el perfil después de revisar la muestra. Confirmar los ángulos o corregir puntos/rangos.
4. Responder brazo/muñeca; luego cabeza/espalda/pies. Probar frente y detalle opcionales sin esperar otro cálculo del detalle.
5. Contestar peso, frecuencia y golpes; seguir al resultado y guardar. Probar el otro lado y comprobar que pide su perfil y no sus ajustes anteriores.
6. Repetir en otro puesto para revisar la memoria identificada. Abrir una evaluación guardada y Añadir análisis.
7. En iPad físico, comprobar detección o salida manual, arrastre y edición de respuestas. Ese ensayo sigue pendiente.

## Archivos y commits

Producción: web/ConversacionEvaluacion.java, MapeosCampo.java, RevisionEvaluacion.java, PaginasEvaluacion.java, PaginasAnalisis.java, EvaluacionesWeb.java; servicio/Evaluaciones.java; static/js/conversacion-evaluacion.js, revision-evaluacion.js, analisis.js, deteccion.js; static/css/aplomo.css. Pruebas: MapeosCampoTest, HttpRevisionEvaluacionTest y aserciones de presentación de FasesEvaluacionTest/HttpCierreTest/HttpCuerpoTest/HttpRecorridoTest; scripts/qa_deteccion_campo.cjs. Documentación: AGENTS.md, fila 15/conteo de README, conteo de INSTALAR, este informe y evidencia.

- `f2f320d Paso 15: detección ligera con salida manual y mapeos de campo`.
- Segundo commit: `Paso 15: conversación guiada y preguntas de campo`.
- Cierre documental: `Paso 15: evidencia y cierre de QA`. Los SHA y el push efectivo se confirman en la respuesta final.

Se espera el OK de Andy tras entregar este paso.
