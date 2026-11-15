# Paso 20 · Sugerencia de apoyo de los pies

15 de noviembre de 2026. Pendiente de revisión de Andy. Sin capturas por encargo.

## Qué cambió

Al detectar puntos con IA en un perfil de Planta, la app puede preseleccionar «Pies sin buen apoyo». Aparecen la pastilla «Según la foto» y la ayuda: «Parece que está sobre una sola pierna. Revisa si los pies están bien apoyados.» El evaluador puede quitarla con un toque.

Una elección humana no se reemplaza al detectar de nuevo ni cambiar la foto de esa postura. Si la IA no tiene una sugerencia, no marca ni desmarca. En manual no se aplica ninguna sugerencia nueva y no aparece la pastilla. El estado de revisión vive por foto/postura; no se preselecciona por defecto en una evaluación nueva.

### Regla y constantes

En `deteccion.js`, `sugerirPiesSinApoyo` usa los landmarks **23, 24, 27 y 28** originales de MediaPipe, antes de redondearlos para diagnóstico. Exige visibilidad **≥0,5**, tomada de `PERFIL.visibilidad`, y coordenadas normalizadas válidas.

- `DIFERENCIA_RELATIVA_MINIMA = 0.15`: desnivel vertical entre tobillos dividido entre la distancia vertical de sus promedios respecto al promedio de las caderas. Debe ser mayor, no igual, al 15 %.
- `ALTURA_CADERA_TOBILLOS_MINIMA = 0.22`: descarta separaciones inferiores al 22 % de la imagen como posible foto sentada. Es una heurística visual fácil de cambiar, no un umbral del método RULA.
- `CONTEXTOS_EXCLUIDOS = ['OFICINA']`: Oficina no recibe sugerencias, aunque los puntos aparenten un desnivel.
- `MARGEN_NUMERICO = 1e-9`: evita que el redondeo binario de JavaScript convierta el 15 % exacto en un valor mayor.

El filtro sentado se probó inicialmente con 25 %, pero descartaba S3 (23,41 %). Se ajustó a 22 % al probar la imagen real. Esta es una elección de implementación; no se atribuye a Andy. La prueba de separación sentada del 20 % sigue sin sugerencia.

En «Añadir análisis», se conserva el tipo del puesto leyendo su campo existente. Antes se reconstruía el puesto con tipo vacío; ahora Oficina queda excluida también en esa continuación. No se modificó ninguna tabla ni valor guardado.

## Pruebas y resultados

| Comprobación | Resultado |
|---|---|
| `TZ=America/Bogota ./mvnw test` | **949/949**, 0 fallos, 0 errores, 0 omitidas; 1 min 27 s. |
| `node --test scripts/qa_*.cjs` | **103/103**, 0 fallos, 0 canceladas, 0 omitidas. |
| Casos nuevos | **16 JS + 1 HTTP**. |
| QA del formulario | S3 preselecciona y muestra pastilla/ayuda; desmarcar las oculta; redetectar y reemplazar conservan la decisión; manual sigue sin sugerencia. |
| Consola del formulario | **0 errores capturados** en ese recorrido. |

Los casos JS cubren tobillos a igual altura, desnivel del 20 %, 15 % exacto y menor, visibilidad baja en cada uno de los cuatro puntos, umbral de visibilidad exacto, Oficina, posible foto sentada de Planta, puntos ausentes/inválidos, Frente/Detalle, preselección IA, decisión humana marcada/desmarcada, retiro de pastilla, ausencia de sugerencia sin desmarcar, manual, cambio de foto y constantes. También se reproduce la geometría observada en S3. La prueba HTTP comprueba el contexto Oficina, ayuda accesible y ausencia de preselección inicial.

### MediaPipe real con S3

Se ejecutó el modelo real en el navegador integrado con `S3_alcance_lejano_una_pierna_perfil.jpg`, contexto Planta, sin sustituir la respuesta del modelo.

| Medida, aproximada por redondeo del diagnóstico | Valor |
|---|---:|
| Promedio cadera–promedio tobillos / alto de imagen | 0,2341 |
| Desnivel de tobillos / alto de imagen | 0,1513 |
| Desnivel / altura cadera–tobillos | **64,63 %** |
| Menor visibilidad de los cuatro puntos | **0,86** |
| Sugerencia | **true** |
| Revisión de perfil | Sin advertencia |

Además del diagnóstico aislado, se eligió S3 en la galería del formulario real de práctica y se verificaron los controles indicados arriba. QA en `rula_ergonomia_pruebas`, dentro de una transacción sin confirmar: **ROLLBACK confirmado** al cerrar. No se guardaron resultados de este recorrido ni se usó la base real para pruebas.

## Requisitos y comprobación

| Requisito del encargo / RF–RNF relacionado | Archivo y función | Prueba |
|---|---|---|
| Regla de sugerencia visual; apoyo a RF-017 | `deteccion.js`: `sugerirPiesSinApoyo`, `detectar` | `qa_apoyo_pies.cjs`: geometría, visibilidad, Oficina/sentada, S3; modelo real. |
| Decisión humana, reintento y foto nueva | `posturas-campo.js`: `nuevoApoyoPies`, `proponerApoyoPies`, `tocarApoyoPies`; `conversacion-evaluacion.js`: `estadoPies`, `recibir` | Casos JS de estado y recorrido real de casilla/redetección/reemplazo. |
| Solo IA, no manual | `analisis.js`: `autoDetectar`; evento `aplomo:puntos-ia` | Caso JS manual y continuación manual del formulario. |
| RNF-007 · Mensaje sencillo y estado visible | `ConversacionEvaluacion.chip`; `pintarPies`; CSS de ayuda | HTTP: texto y `aria-describedby`; DOM real: pastilla y ayuda desaparecen al tocar. |
| Exclusión Oficina en Añadir análisis | `EvaluacionDao.consultar`, `ConversacionEvaluacion.continuar` | `HttpAnalisisTest.agregarAnalisisConservaOficinaYLaAyudaDePiesSinPreseleccion`. |
| RF-018/019 · Cálculo del servidor conservado | `trasladar` y envío existente de `piernasApoyadas`, sin cambios de formato | Suite RULA y HTTP existentes, incluidos los diez resultados y rechazo de puntaje adulterado. |
| Permisos de Administrador conservados | Servicios y rutas existentes, sin cambios | `HttpAnalisisTest.puntajeAdulteradoSeIgnoraYElHistorialQuedaProtegido`: Usuario recibe 403 al escribir direcciones de análisis y calcular/guardar. |

## Archivos tocados

1. `src/main/resources/static/js/deteccion.js`: regla visual y constantes.
2. `src/main/resources/static/js/posturas-campo.js`: estado de sugerencia y elección humana.
3. `src/main/resources/static/js/analisis.js`: contexto y evento tras IA válida.
4. `src/main/resources/static/js/conversacion-evaluacion.js`: casilla, pastilla y memoria al cambiar foto.
5. `src/main/resources/static/css/aplomo.css`: ayuda en línea propia.
6. `src/main/java/co/aplomo/web/ConversacionEvaluacion.java`: ayuda accesible y contexto de continuación.
7. `src/main/java/co/aplomo/dao/EvaluacionDao.java`: lectura del tipo existente del puesto.
8. `scripts/qa_apoyo_pies.cjs`: 16 casos JS.
9. `src/test/java/co/aplomo/web/HttpAnalisisTest.java`: caso HTTP nuevo.
10. `docs/informe_paso20.md`: este informe.
11. `AGENTS.md`: estado.
12. `README.md`: fila del Paso 20 y contador.
13. `INSTALAR.md`: contador de pruebas actualizado; instalación intacta.

## Checklist y límites

- ✅ Maven 949/949 y JS 103/103.
- ✅ Modelo real con S3 y formulario real con controles de la sugerencia.
- ✅ Texto exacto en español, tildes y ayuda asociada a la casilla.
- ✅ Usuario bloqueado por el servidor en las pruebas HTTP de acceso por dirección.
- ✅ Cero errores de consola capturados en el recorrido descrito.
- ✅ Sin cambios en `co.aplomo.rula`, `POST /evaluaciones/calcular`, esquema ni formato de `piernasApoyadas`; sin dependencias nuevas.
- ✅ Sin borrar datos ni fotos; QA revertida. No se agregan claves ni archivos de fotos al commit. App nativa reiniciada con su configuración habitual: login 200 en 8080 y ambos scripts servidos coinciden por SHA-256 con los archivos actualizados.
- ❌ Comparación visual con mock/capturas: no realizada; Andy pidió no tomar capturas. Se mantuvieron los controles y estilos existentes.
- ❌ Matriz 390/810/1080/1366 y tacto físico: no repetidos en este paso; comprobación funcional en navegador integrado de 1280×720, sin certificar iPad físico.

Implementación pendiente del encargo: **nada**. No se asegura que esta heurística reconozca todas las posturas sentadas o todas las fotos: por eso sigue siendo una propuesta revisable y exige los cuatro puntos visibles.

## Cómo probarlo

1. Como Administrador, abre Probar evaluación → Planta; acepta el consentimiento y el registro con fotos.
2. Completa los datos del puesto y elige la foto de ejemplo «Alcance lejano con un pie de apoyo», mirando a la derecha (S3).
3. Tras detectar, comprueba «Pies sin buen apoyo», la pastilla y su ayuda.
4. Desmárcala: desaparecen pastilla y ayuda. Pulsa Detectar puntos: permanece desmarcada.
5. Cambia por S3 de nuevo: conserva tu decisión. Calcular manualmente no agrega la sugerencia.
6. En una evaluación de Oficina no debe aparecer esta propuesta. Las selecciones manuales siguen disponibles.

## Git

Base: `41dc068`. Entrega: «Paso 20: sugerencia de apoyo de pies desde la foto». Hash y confirmación de push a `origin main` se informan al cerrar, tras verificar el repositorio.

Esperar el OK de Andy antes de comenzar otro paso.
