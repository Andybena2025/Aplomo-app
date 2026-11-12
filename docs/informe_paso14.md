# Paso 14 · Detección automática de puntos

12 nov 2026 · America/Bogota. Andy autorizó explícitamente este paso opcional en el chat. No se cierra ni se aprueba el Paso 13 general interrumpido.

## Qué se hizo

Se adaptó el bloque «Detección automática de postura» del mock a los estados por foto del asistente existente. MediaPipe Pose se carga bajo demanda desde `https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.5.1675469404/`. El código y el modelo se descargan; la imagen se procesa en el navegador. No se incorporan servicios de análisis remotos, dependencias Maven, rutas ni cambios de esquema.

La foto de perfil sin puntos inicia la detección al abrirse, una sola vez por foto; Detectar puntos permite reintentar. Se copian la elección por visibilidad, el promedio índice/meñique y `CORONILLA={arriba:1.2,atras:0.9}`. `cabeza` del mock se adapta al nombre `coronilla` del editor actual. Si cambia la orientación, se informa «Lado ajustado a …». La etiqueta pide revisar los puntos y el arrastre sigue disponible.

Como en el mock, la foto frontal conserva los 33 puntos auxiliares en memoria del navegador; no se convierte en una medición lateral automática. Se probó su detección con el botón, activando Analizar esta foto. La muestra de detalle corresponde a `OTRA`: no dispara el detector y conserva el flujo manual. No se automatizan ajustes, actividad, carga ni puntuaciones.

Se respetan 15 s para el script, 45 s para inicializar y 60 s para la operación de detección. Las inferencias comparten una cola; se permiten reintentos tras error o tiempo límite. Una revisión del estado evita sustituir puntos o ángulos editados mientras llega una respuesta. Solo se desactiva el botón Detectar durante su solicitud: la IA no es una condición para guardar.

## Pedido y requisitos relacionados

| Pedido / requisito | Archivo y punto | Comprobación |
|---|---|---|
| 1–2 · MediaPipe local y carga / valor agregado, RNF-003 y RNF-006 | `deteccion.js`: MP_URL, cargarScript, poseLista, conTiempo | CDN fijado; script solo bajo demanda; ocho escenarios reproducibles en `verificar-detector.mjs`; detección real en Chromium integrado y Chrome for Testing 156 |
| 3 · Seis puntos, lado y revisión / RF-017 | `deteccion.js`: detectar; `analisis.js`: autoDetectar, cargarFoto, sincronizar | Perfil: seis puntos y ángulos del servidor. Arrastrar codo cambió Brazo de 2.25° a 8.84°. Reintento desde Izquierdo informó ajuste a Derecho |
| 4 · Botón y disparo / RNF-002 | `PaginasAnalisis.pasos`; `analisis.js`: autoDetectar, mostrarDeteccion | Perfil automático; frontal por botón; detalle desactivado. Los tres botones miden 44 px en los cinco tamaños |
| 5 · Fallo y salida manual / RNF-007 | `analisis.js`: falloDeteccion, autoDetectar | Chrome en Offline, sin modelo cargado y con caché desactivada: mensaje exacto y marcado manual pasó de Cadera a Hombro. Otra comprobación bloqueó solo el CDN y permitió guardar los cinco ángulos manuales: 1 · Verde · Nivel 1 |
| 6 · Servidor y RULA intactos / RF-018–021 | Solo se añade presentación en `PaginasAnalisis` y `PaginasEvaluacion`; clases de cálculo sin cambios | Suite existente; NEUTRO=5° y tablas MySQL conservados; no se guarda una puntuación calculada por la IA |
| 7 · Accesibilidad | `PaginasAnalisis.pasos`: estado con role=status y aria-live; botón .btn existente | Mensaje persistente, sin animación nueva ni desplazamiento automático. Revisión de código; no se cambiaron preferencias del sistema |
| Privacidad y permisos / RNF-004, RNF-006 | Permisos existentes del servidor, sin modificaciones | Usuario ficticio: direcciones directas de fotografía y editor muestran Acceso denegado |

## Verificación

- `TZ=America/Bogota ./mvnw test`: resultado final en `evidencia_paso14/verificacion.txt`.
- La suite del estado de trabajo contiene **716 pruebas**: incluye una prueba de rendimiento del Paso 13 que ya estaba pendiente, sin commit. Este paso no la incorpora. La base versionada conserva sus **715** pruebas; por eso no se altera ese número en README/INSTALAR.
- Ocho escenarios aislados del detector: geometría/coronilla; lado visible; cola y reutilización; ausencia de persona; fallo de red y reintento; tiempo límite del modelo y reintento; tiempo límite de detección y reintento; navegador declarado offline sin solicitudes.
- Tres imágenes reales de muestra de `/static/img`, sin fotos particulares: perfil detectado; frontal detectado como apoyo; detalle manual.
- Offline se activó únicamente en la pestaña temporal de Chrome, no en la conexión del Mac. Se restauraron No throttling y la caché antes de cerrar esa ventana. Si el modelo ya está cargado, puede seguir procesando localmente sin descargarlo otra vez; se comprobó aparte el fallo sin precarga.
- Para probar guardar tras un fallo de IA sin bloquear el servidor local, la vista QA bloqueó solo los recursos externos mediante una cabecera temporal. Esa cabecera no está en el código del producto. Se llegó a Resultado y Guardar análisis, con confirmación de guardado.
- Todas las filas y la evaluación/análisis guardados en QA pertenecieron a `rula_ergonomia_pruebas`, dentro de una transacción reversible. Se cerró la vista con ROLLBACK. Las copias temporales de imágenes se retiraron al cerrar; nada de la base real se modificó.

## Checklist

- ✅ Suite Maven en verde; número y estado detallados arriba y en verificacion.txt.
- ✅ Mismo componente del mock: medición, barra de pasos, puntos, arrastre y botones; capturas en `docs/evidencia_paso14/`.
- ✅ 390 / 810 / 1080 / 1366 / 1920 px sin desplazamiento horizontal; tres botones de 44 px. `medidas.json`.
- ❌ Dedo físico no comprobado. Se conservan los eventos pointer/touch y las áreas táctiles existentes; sí se comprobó arrastre con mouse.
- ✅ Usuario sin acceso a foto ni editor, también por dirección escrita a mano.
- ✅ Textos en español con tildes y errores sin detalles técnicos.
- ✅ Sin errores de consola en el recorrido normal revisado en Chromium integrado. Los fallos de red/CSP de la prueba sin conexión son intencionales.
- ✅ Sin credenciales, fotos privadas, `config/db.properties` ni archivos de `fotos/` añadidos al commit.
- ✅ Sin nuevas rutas, consultas del producto, tablas o librerías Maven. No se modificó el motor RULA.

## Lo que no se hizo

No se amplió el alcance a inferir ajustes, actividad o carga. No se sustituyó la revisión del Administrador ni se declaró precisión clínica. No se repitió una matriz completa de versiones de navegadores ni una auditoría WCAG. No se retomó el cierre del Paso 13 interrumpido. No se hizo push.

## Cómo probarlo

1. Reinicia la app para cargar los cambios de presentación: `./mvnw compile exec:java`. Ingresa como Administrador y abre una visita En curso.
2. Inicia una evaluación con consentimiento y fotos de muestra. En medición, abre Perfil: aparece Detectando puntos… y luego los seis puntos con la etiqueta de revisión.
3. Arrastra un punto; verifica que cambian los ángulos devueltos por Java. Prueba Detectar puntos otra vez. Cambia el lado y vuelve a detectar para comprobar el aviso si corresponde.
4. Frente sirve de apoyo: no se lanza automáticamente. Si activas Analizar esta foto, el botón permite comprobar la detección; sus ángulos continúan siendo manuales. Detalle continúa manual.
5. Para la prueba offline, abre primero una foto frontal y activa el editor antes de detectar. En una pestaña de prueba, activa Offline en las herramientas del navegador y desactiva caché; pulsa Detectar puntos. Debe mostrarse el aviso y permitir marcar. Restablece la conexión antes de guardar, porque guardar sigue requiriendo el servidor.
6. Para simular solo un CDN inaccesible conservando el servidor disponible, bloquea el dominio del CDN en una pestaña de prueba. Marca o escribe los ángulos, continúa por Ajustes y Resultado y guarda normalmente.
7. Como Usuario, intenta las direcciones de la fotografía y del editor: Acceso denegado.

## Git

Commit solicitado: `Paso 14: detección automática de puntos con MediaPipe`, posterior a `13a9aa5 Paso 13A: correcciones del profesor Arle`. Solo se incluyen los cambios y evidencias del Paso 14. El trabajo previo del Paso 13 permanece sin commit. **Sin push.** El hash nuevo se entrega en el resumen final.

Referencia técnica consultada: [MediaPipe Pose, documentación del proyecto](https://chuoling.github.io/mediapipe/solutions/pose.html). La referencia de comportamiento fue el bloque de detección del mock local.
