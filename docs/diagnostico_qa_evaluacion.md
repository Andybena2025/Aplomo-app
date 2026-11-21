# Diagnóstico QA · práctica y detección de evaluación

14 nov 2026. Corrección solicitada por Andy después de 15-E/16. Se reprodujeron los fallos en el navegador integrado con datos ficticios de `rula_ergonomia_pruebas`, dentro de una transacción sin confirmar. Al cerrar se confirmó «QA revertida por ROLLBACK». Sin capturas ni descargas de navegadores.

## Fallos encontrados y correcciones

1. **Probar Planta/Oficina devolvía «Elige Oficina o Planta para practicar».** `cuenta.js` desactivaba el botón durante el evento submit. El navegador omite botones desactivados al construir el formulario, por lo que desaparecía `tipo=PLANTA|OFICINA`. Ahora conserva el nombre/valor en un campo oculto antes de desactivar; lo retira al volver con el historial. Se mantiene el bloqueo de doble envío. Ambos botones abrieron su evaluación en el navegador después del cambio.
2. **Fotos de perfil rechazadas como cuerpo incompleto.** El control exigía alta visibilidad de los dos lados del cuerpo. La oclusión del lado lejano de un perfil disparaba el mismo aviso que un recorte real. Ahora comprueba cabeza y una cadena corporal completa; baja confianza genera aviso de revisión sin ocultar los ángulos. Los recortes reales y la ausencia de persona conservan su recuperación manual. Solo se usa la geometría de hombros/caderas confiables para la advertencia de frente.
3. **Una foto heredaba el seguimiento de la anterior.** La versión JS fijada de Pose no admite `staticImageMode`. Se reutilizaba el modelo de seguimiento sin reiniciar su grafo. Se quitó esa opción ignorada y se llama `reset()` antes de cada `send()`, dentro del límite de inferencia. Se conserva una sola instancia, el modelo ligero, entrada de 1280 px, límites de carga y reintentos. Se reprodujo S6 → S2 con un falso frente antes; después se recibieron puntos y mediciones nuevos sin ese bloqueo.

Referencia técnica comprobada: [interfaz de la versión exacta de Pose](https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.5.1675469404/index.d.ts), que declara `reset()` y las opciones JS; [definición oficial de visibilidad](https://github.com/google-ai-edge/mediapipe/blob/master/docs/solutions/pose.md), que incluye oclusión. La interpretación de la causa se contrastó con los fallos reales del recorrido, no solo con documentación.

## Pruebas y números

| Comprobación | Resultado |
|---|---|
| `TZ=America/Bogota ./mvnw test` | **923/923**, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS, 1 min 22 s |
| `node --test scripts/qa_deteccion_campo.cjs scripts/qa_formularios.cjs scripts/qa_fotos_campo.cjs scripts/qa_posturas_campo.cjs scripts/qa_revision_eventos.cjs` | **69/69**, 0 fallos ni omitidas |
| Nuevas pruebas de formularios contra cuenta.js anterior (96a36b3), solo en archivos temporales | **4 fallan de 6**, como se esperaba: detectan la regresión |
| Pruebas actualizadas de detección contra deteccion.js anterior, solo temporales | **7 fallan de 27**, como se esperaba; versión corregida: todas pasan |
| Navegador: Probar Planta y Probar Oficina | Ambos abren su puesto correcto |
| Recorrido Oficina: permiso → contexto → dos posturas → resultado → guardar → salir | Completado; dos evaluaciones guardadas en la transacción QA; resultados 4 y 6; regreso a visita y cierre a login |
| Foto S6: detección y reintento | Automática, sin aceptación de falso recorte; mismos ángulos al reintentar |
| Cambiar S6 por S2 | Nuevas mediciones; sin falso frente ni ángulos de la foto anterior |
| Quitar foto → volver a cargar S6 | Vuelve a subida, Seguir indica foto faltante y la detección funciona al cargar nuevamente |
| Consola del recorrido | **0 errores** capturados; mensajes informativos WebGL del proveedor |
| Base real / datos QA | Base real sin cambios de QA; transacción QA revertida; servidores temporales cerrados |

### Galería con el modelo real (no simulado)

| Postura | Perfil | Frente |
|---|---|---|
| S2 · Mesa alta/cabeza inclinada | Puntos; sin bloqueo | Advertencia FRENTE |
| S3 · Alcance lejano/una pierna | Puntos; sin bloqueo | ANGULO_DUDOSO, revisión sin bloqueo |
| S4 · Brazos sobre la cabeza | Puntos; sin bloqueo | Advertencia FRENTE |
| S5 · Inclinada hacia el carro | Puntos; sin bloqueo | Advertencia FRENTE |
| S6 · Levantando pieza pesada | Puntos; sin bloqueo | Advertencia FRENTE |

Las cinco fotos de perfil pasaron sin falso recorte con el modelo real y el reinicio por foto. Esto comprueba la integración; no certifica precisión clínica de los puntos. Se conserva la revisión/corrección manual. S3 de frente quedó como dudosa, no se afirma clasificación perfecta.

Mediciones visibles de S6: Brazo 81,53°, Antebrazo 30,46°, Cuello −25,74°, Tronco 87,44°. Después de cambiar a S2: 27,29° / 103,27° / 18,05° / 28,1°. Son resultados del motor existente con los puntos propuestos, no una validación contra mediciones profesionales de referencia.

## Archivos y trazabilidad

| Requisito | Archivo / función | Prueba |
|---|---|---|
| RF-013a/014, RNF-003/007 | `src/main/resources/static/js/cuenta.js`, manejador submit/pageshow | `scripts/qa_formularios.cjs`: valor de ambos botones, doble envío, historial y otros formularios; clics reales en ambos botones |
| RF-017, RNF-002/007 | `src/main/resources/static/js/deteccion.js`, revisarPerfil/detectar/poseLista | `scripts/qa_deteccion_campo.cjs`: oclusión, baja confianza, recorte, frente, reset por foto, fallos/reintentos; 10 fotos con modelo real |
| RF-017, RNF-007 | `src/main/resources/static/js/analisis.js`, autoDetectar; `conversacion-evaluacion.js`, evento foto-revisada | Avisos leves en línea sin esconder ángulos; recorrido completo, cambio, retiro y reintento |
| RNF-004/006/010 | Código Java y permisos sin cambios | Suite Maven existente: acceso por rol, CSRF, fotos, cálculo y guardado |

Los seis archivos de código/pruebas anteriores, `AGENTS.md` y este informe componen la corrección. No se cambió Java, rutas, SQL, paquete RULA ni POST de cálculo. Tampoco se agregaron preguntas, dependencias o colores.

## Checklist y límites

- ✅ 923 pruebas Maven y 69 JS aprobadas; pruebas de regresión fallan con el código anterior.
- ✅ Clics reales, inferencia real y guardado completo en copia de QA reversible.
- ✅ Usuario/CSRF/fotos privadas comprobados por la suite HTTP; no se cambió el servidor.
- ✅ Texto en español, sin secretos ni fotos privadas añadidos a Git.
- ✅ Sin errores de consola del recorrido; app nativa conservada en 8080 con recursos actualizados.
- ❌ Prueba en iPad físico y Safari nativo: no realizada en esta revisión; no afirmar fallo físico reproducido/resuelto.
- ❌ Matriz 390/810/1080/1366: el control de tamaño del navegador integrado no cambió el ancho real (permaneció en 1280). Se restableció y no se cuentan esas lecturas como cuatro tamaños. En el ancho real comprobado: sin desbordamiento lateral, Detectar puntos mide 48 px.
- ❌ Capturas/comparación visual completa con mock: no realizadas, conforme al pedido previo de no tomar capturas.

## Cómo comprobarlo en la app local

1. Recarga ALINEO y abre Inicio → Probar evaluación. Entra a Probar Planta y después comprueba Probar Oficina.
2. Acepta el consentimiento/permiso de fotos en la práctica, responde contexto y elige un ejemplo de perfil.
3. Los puntos y ángulos deben aparecer; revísalos antes de seguir. Prueba Detectar puntos otra vez, Cambiar foto y Quitar foto.
4. Completa la muñeca y las dos posturas; guarda y comprueba que vuelve a la visita. Una foto realmente recortada o de frente sigue pidiendo revisión.

El navegador procesa las fotos localmente. Las correcciones del detector no cambian fórmulas ni puntajes por sí mismas. El estado del commit y del push se confirma en la respuesta final.
