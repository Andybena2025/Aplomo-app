# Paso 18 · Galería filtrada y dirección de los ángulos

14 de noviembre de 2026. Implementado y probado; pendiente de revisión de Andy. Sin capturas, por instrucción expresa.

## Qué hice y archivos por parte

| Parte | Cambios | Archivos |
|---|---|---|
| 1 · Fotos | 22 imágenes copiadas con los nombres exactos; cinco perfiles S2–S6 reflejados horizontalmente, JPG calidad 100. Las once variantes se revisaron antes de copiarlas: todas coinciden con las descripciones. Los quince archivos anteriores conservan exactamente sus bytes. | `src/main/resources/static/img/galeria/`: 27 archivos nuevos; `GaleriaDemoTest.java` verifica archivos, decodificación, peso y originales. |
| 2 · Catálogo | Archivo, vista, dirección, contexto, tipo y aviso. Una misma foto frontal es buena en Frente y error en Perfil. El caso de piernas tapadas se clasifica Error y lleva la ayuda manual. | `src/main/java/co/aplomo/web/GaleriaDemo.java`. |
| 2 · Selección | Filtro por ranura, Oficina/Planta y lado. Contexto desconocido permite ambos. Máximo seis buenas y, solo en Perfil, un error de la terna del contexto. Rotación al abrir de nuevo, sin ventanas flotantes. Etiquetas de dirección para Automático/Ambos. | Nuevo `src/main/resources/static/js/galeria-ejemplos.js`; `conversacion-evaluacion.js`, `fotos-campo.js`; `ConversacionEvaluacion.java`, `PaginasAnalisis.java`, `aplomo.css`; `scripts/qa_paso18.cjs`, `qa_fotos_campo.cjs`, `GaleriaDemoTest.java`, `HttpRevisionEvaluacionTest.java`. |
| 3 · Aviso | FRENTE y ANGULO_DUDOSO dicen «Parece una foto de frente o de espaldas. Necesito una de lado (de perfil).». | `src/main/resources/static/js/deteccion.js`; metadatos de `GaleriaDemo.java`. Las pruebas anteriores no citaban la frase antigua exacta. |
| 4a · Dirección | La IA usa mira de la foto, aunque contradiga el lado elegido. Se comunica el ajuste y el control/título reflejan el lado real. La marcación manual infiere orientación a partir de cabeza y muñeca respecto de cadera; si es ambigua, usa el lado elegido. El navegador no calcula ángulos ni puntajes. | `analisis.js`, `posturas-campo.js`, `conversacion-evaluacion.js`; `scripts/qa_paso18.cjs` y `HttpRevisionEvaluacionTest.java`. |
| 4a · Ambos | Si ambas fotos muestran el mismo lado, aparece el aviso exacto y Seguir espera una foto del otro lado o «Seguir solo con este brazo». Esta última opción analiza la foto actual y conserva los archivos adjuntos. | `ConversacionEvaluacion.java`, `conversacion-evaluacion.js`, `posturas-campo.js`. |
| 4b · Postura 2 | Empieza en Automático, sin heredar Ambos de la anterior. El mensaje «Falta la foto del otro brazo: sube un perfil del lado que aún no subiste, de esta misma postura.» aparece únicamente mientras se espera ese perfil en Ambos. | `ConversacionEvaluacion.java`, `conversacion-evaluacion.js`, `posturas-campo.js`; pruebas Java/JS. |
| 4c · Subida | Instrucciones y botones alineados al borde inferior de la muestra en distribución de dos columnas; debajo en pantallas estrechas. | `src/main/resources/static/css/aplomo.css`. |
| Entrega | Estado, fila del paso y contador de pruebas. | `AGENTS.md`, `README.md`, `INSTALAR.md`, este informe. |

Los JS de la tabla están en `src/main/resources/static/js/`; Java web en `src/main/java/co/aplomo/web/`; las pruebas Java en `src/test/java/co/aplomo/web/`.

## Fotos y catálogo: números

Origen indicado por Andy: `/Users/andresbenavides/Downloads/aplomo_fotos_demo/` y subcarpeta `aplomo_fotos_variantes_11 (4)/`.

- **42 archivos únicos**: 15 anteriores + 22 nuevos + 5 espejos.
- **20,79 MiB** en total, menos de 25 MB aproximados. No fue necesario reducir las imágenes nuevas.
- **46 entradas** de catálogo: algunas imágenes tienen un papel distinto según la ranura. Hay 38 entradas buenas y ocho de error; solo seis errores participan en las dos ternas de rotación indicadas por Andy.

| Contexto | Perfil derecho bueno | Perfil izquierdo bueno | Frente bueno | Detalle bueno | Errores que rotan en Perfil |
|---|---:|---:|---:|---:|---:|
| Oficina | 4 | 3 | 1 | 2 | 3 |
| Planta | 8 | 9 | 6 | 5 | 3 |

Las fotos de detalle no se muestran como buenas en Perfil. `oficina_sentado_inclinado_perfil_izquierda.jpg` tampoco aparece entre las buenas: su etiqueta es «La IA no la reconoce bien: usa Calcular manualmente».

La rotación se lleva por apertura de cada lista: recorre candidatos y cambia el subconjunto cuando hay más de seis. Cuando hay menos de seis buenas, se muestran las disponibles. El ejemplo con error no depende del lado para que siempre pueda enseñarse el aviso.

## Requisitos y pruebas

| Requisito | Archivo/método | Comprobación |
|---|---|---|
| RF-014 · Puesto y postura | `ConversacionEvaluacion.pagina`, `activar`, `modoPostura` | Contexto por tipo de puesto; segunda postura en Automático en JS y navegador. |
| RF-016 · Fotos | `GaleriaDemo.selector`, `AplomoGaleria.seleccionar/iniciar`, `AplomoFotosCampo.ejemplo/validar` | GaleriaDemoTest: 42 archivos válidos y originales intactos; HTTP sirve todos como JPG; pruebas JS de filtros y camino común de subida. |
| RF-017 · Medición | `ladoFoto`, `autoDetectar`, `cambiado`, `elegirBrazo` | JS: IA y manual en ambas direcciones, ambigüedad y aviso de lado repetido. HTTP: flexión positiva de Brazo/Cuello/Tronco en los dos perfiles usando el cálculo existente. |
| RF-018/020/021 · Cálculo, resultado y guardado | Servicios y motor existentes, sin cambios; parámetros por foto | Suite RULA completa y recorrido Oficina guardado con dos posturas: 2 Verde / 2 Verde. |
| RNF-002/007 · Interfaz y mensajes | Selector en línea, chips, avisos y CSS | Filtros reales, aviso de frente/espaldas, ajuste de dirección y aviso de otro brazo comprobados. |
| RNF-004/006 · Seguridad y fotos | Permisos, CSRF, validación y rutas existentes conservados | Suite completa de seguridad/fotos; ejemplos públicos usan las mismas validaciones que una subida. |
| RNF-008 · Compatibilidad | HTML/CSS/JS plano, sin dependencias nuevas | Navegador integrado comprobado; iPad físico y otros navegadores no certificados en esta entrega. |

## Pruebas y medidas de esta entrega

- `TZ=America/Bogota ./mvnw test`: **942 ejecutadas y aprobadas; 0 fallos, 0 errores, 0 omitidas**.
- `node --test scripts/qa_*.cjs`: **86 ejecutadas y aprobadas; 0 fallos**.
- Ventana real del navegador integrado: **1280 × 720**. Pantalla de subida: **0 px** de desbordamiento horizontal.
- Muestra y bloque de subida cerrada: ambos terminaron en **1152,59 px** del documento, diferencia **0 px** entre sus bordes inferiores.
- Oficina + Izquierdo: **3 buenas izquierdas + 1 error**; ninguna buena derecha.
- Planta + Izquierdo: **6 buenas izquierdas + 1 error**; se incluyen los espejos.
- Planta, ranura Frente: **6 buenas de frente**; ranura Detalle: **5 buenas de detalle**.
- Tres aperturas de Oficina mostraron errores diferentes: frente, espaldas y la foto que la IA no reconoce bien.
- Foto izquierda: Brazo **24,81°**, Antebrazo **65,70°**, Cuello **7,53°**, Tronco **1,35°**. Se comunicó el ajuste al lado izquierdo.
- Foto derecha, tras pulsar Izquierdo: Brazo **26,88°**, Antebrazo **68,14°**, Cuello **3,86°**, Tronco **2,74°**. Se mantuvo la dirección derecha y apareció «Esta foto muestra el lado derecho; lo ajustamos.».
- Repetir el mismo perfil en Ambos mostró el aviso exacto. «Seguir solo con este brazo» permitió avanzar; la postura 2 llegó con Automático seleccionado.
- Marcación con geometría de prueba orientada a la derecha, partiendo de Izquierdo: cinco marcas completadas; control y título cambiaron a Derecho; Brazo **74,60°**, Cuello **16,02°**, Tronco **16,60°**. Es una prueba de geometría simulada, no una valoración de la postura de esa foto.
- El ejemplo frontal de Planta provocó el nuevo mensaje completo de frente o espaldas y permitió entrar al respaldo manual.
- Dos posturas de Oficina guardadas, vuelta a visita con «Evaluación guardada» y resultado **2 / 2**, ambos Verdes.
- **0 errores de consola capturados** en el recorrido comprobado.
- QA solo en `rula_ergonomia_pruebas`; al cerrar se confirmó **ROLLBACK**. Ningún ensayo se guardó en la base real.

## Lo que no hice / límites

- No cambié `co.aplomo.rula`, `EvaluacionesWeb.java` (POST `/evaluaciones/calcular`) ni el esquema SQL. No cambié NEUTRO, reglas, puntajes ni formato de persistencia.
- No borré imágenes originales, datos, documentos ni configuración. La inferencia manual solo elige la dirección; no añade una tolerancia al método RULA. Si cabeza y muñeca apuntan en sentidos contradictorios o no hay información suficiente, conserva el lado elegido.
- La clasificación del resto de fotos según MediaPipe sigue los datos ya verificados que Andy incluyó en el encargo; no se repitió una campaña de detección real sobre los 42 archivos. Sí se probaron en esta entrega los dos perfiles de Oficina y el ejemplo frontal de Planta.
- No hice capturas ni descargué navegadores. No certifico una matriz nueva de tamaños, tacto en iPad físico ni Chrome/Edge/Firefox/Safari en esta entrega. El apilado estrecho está definido en CSS; la medición nueva fue a 1280 × 720.

## Checklist

- ✅ Maven 942/942 y JavaScript 86/86.
- ✅ Archivos, peso, tipos, tres perfiles buenos por contexto/lado, rotación y separación de errores cubiertos por pruebas.
- ✅ Misma validación de tamaño/tipo/permiso en ejemplos y subidas.
- ✅ Direcciones izquierda/derecha y manual probadas sin cambiar el motor.
- ✅ Recorrido y guardado de dos posturas comprobado; texto nuevo de error sin términos técnicos.
- ✅ Permisos de Usuario cubiertos por la suite completa; no se repitió un recorrido interactivo como Usuario.
- ✅ Cero errores de consola capturados y sin archivos sensibles preparados para Git.
- ❌ Capturas/comparación gráfica: omitidas expresamente por Andy.
- ❌ Nueva matriz 390/810/1080/1366 y prueba con dedo físico: pendientes; no confundir con la ventana integrada de 1280 px.

## Cómo lo prueba Andy

1. En Inicio, abre Probar evaluación y elige Oficina o Planta. Completa permiso y datos del puesto.
2. En Perfil, alterna Derecho, Izquierdo, Automático y Ambos. Abre/cierra Usar foto de ejemplo: aparecen hasta seis buenas y un ejemplo marcado con error; los errores cambian al abrir de nuevo.
3. Elige un perfil bueno. La dirección y los ángulos siguen la foto; si eliges un lado contrario, la app lo ajusta y lo explica. También puedes usar Calcular manualmente.
4. Con Ambos, intenta repetir el mismo lado para ver el aviso; cambia la foto o pulsa Seguir solo con este brazo.
5. Al pasar a la postura 2, comprueba que vuelve a Automático y no pide otro brazo hasta elegir Ambos y confirmar el primero de esa misma postura.
6. En Agregar otra foto, Frente ofrece frentes y Detalle ofrece detalles del contexto. Termina ambas posturas y guarda.

## Git

Base: `5f2f5f5`. Commit de esta entrega: «Paso 18: galería filtrada y dirección por foto». Destino verificado: `https://github.com/Andybena2025/Aplomo-app.git`, rama `main`. El hash y la confirmación de push se entregan al cerrar el paso; consulta `git log -1 --oneline`.

Esperar el OK de Andy antes de comenzar otro paso.
