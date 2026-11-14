# Paso 13F · Recorrido guiado para evaluadores nuevos

13 nov 2026. Implementado por el encargo final de Andy, con base en `0daf4f8` (799 pruebas). Se conservó lo que ya cumplía de la sesión interrumpida. Antes de editar se comprobó el árbol limpio y se ejecutó `git pull --rebase origin main`: ya estaba actualizado. No se descargó ni instaló ningún navegador.

## Qué cambió

Todas las opciones del contexto, cuerpo, ajustes, esfuerzo y uso de fotos tienen las pistas exactas del encargo. Tiempo conserva una única pista bajo su título. Las definiciones de condiciones están en sus tarjetas y se retiró la ayuda plegable duplicada. Los dos constructores comparten marcado, texto escapado, nombre accesible independiente y una descripción con identificador único. Las ayudas de las fotos elegidas se construyen con `textContent`, igual que el resumen y las pastillas.

Se distingue el consentimiento en papel para evaluar del permiso para tomar fotos: «Consentimiento para la evaluación», casilla y ayuda nuevas; después «¿La persona permite que le tomes fotos?», «Sí, permite fotos» / «No permite fotos». El servicio solo cambia el mensaje de error solicitado; no cambian sus condiciones de validación ni los valores guardados.

Las tarjetas tienen contorno perla, franja inferior, selección marfil, texto fuerte y casilla con verificación. El pictograma sigue oscuro. El bloque completo está al final de `aplomo.css`, desde `/* Paso 13F · opciones */`, para poder retirarlo como una unidad. Los botones conservan sus tamaños y colores; el pie del asistente usa la posición sticky solicitada. La pastilla tiene su propio relleno para no heredar el espacio superior de un mosaico.

Cuando termina la detección, se consultan los cinco segmentos angulares en el endpoint existente, sin ajustes confirmados. Los grados y el rango salen del servidor. Se preseleccionan los cinco mosaicos únicamente si no hay elección explícita, ángulo manual o confirmación previa; no se confirma ningún segmento automáticamente. El resumen bajo la foto muestra los cinco segmentos, la opción real del catálogo, el actual resaltado y ✓ al confirmar. La sugerencia conserva «Según la foto» al elegir otra tarjeta; la fila añade «Tu elección». Un arrastre real recalculó el resumen, conservando la elección explícita. Al quitar puntos se retiran las propuestas sin base y sus etiquetas, y sigue disponible la observación manual. El otro perfil mantiene su aviso y oculta resumen/pastilla. Los ángulos se presentan con un decimal y coma en el lienzo, aviso y resumen.

**Recuerdo del puesto anterior: OPCIÓN 2 del encargo final**, que sustituye la respuesta anterior de no preseleccionar. Se conserva el comportamiento de 13E para postura, momento, tiempo y esfuerzo, con su aviso y Continuar explícito. Se añade «Del puesto anterior» a la respuesta recordada, incluso si se elige otra. No se copian puntos, ángulos ni ajustes.

Para iPad se compactaron encabezado/progreso, separaciones e imágenes según el espacio disponible; Fotos coloca tomas y metadatos en columnas, y en el vertical estrecho el uso de la foto ocupa una fila propia. Los rangos ocupan tres columnas en horizontal y también en el vertical estrecho para evitar una tercera fila demasiado alta. El lienzo conserva proporción. La hoja de ajustes conserva su desplazamiento propio. Los cambios de distribución empiezan en 721 px; celular mantiene su disposición, además del estilo de opciones/pistas y el pie sticky expresamente solicitados. No se disimula contenido con alturas recortadas ni overflow oculto del documento.

## Requisitos y comprobaciones

| Requisito / encargo | Archivo y método | Evidencia / prueba |
|---|---|---|
| RF-014 · postura, momento y tiempo claros | `PaginasEvaluacion.nueva`, `opcion`; `memoria-evaluacion.js`, `restaurar`, `marcarRecordada` | `HttpCierreTest.todasLasOpcionesTienenPistaVinculadaYNoHayRespuestasMarcadasEnElPrimerPuesto`; comprobaciones de primer puesto y siguiente en navegador. |
| RF-015 · seis condiciones y ayudas | `PaginasEvaluacion.CONDICIONES`, `nueva` | Mismo test de pistas/IDs; captura de Condiciones; validación Ninguna existente en `HttpRecorridoTest`. |
| RF-016 / RNF-006 · consentimiento, fotos y uso | `PaginasEvaluacion.nueva`, `PaginasAnalisis.continuar`; `evaluacion.js`, `renderFotos`; `Evaluaciones.validar` (solo texto) | `HttpRecorridoTest.sinConsentimientoFisicoNoGuardaAunqueSeSalteLaInterfaz`; `HttpCierreTest.lasPistasEscapanHtmlYLasFotosExistentesTambienLasIncluyen`; carga y guardado reales de ensayo. |
| RF-017 · medidas revisables | `recorrido.js`, `medirFotoCompleta`, `pintarResumen`, `pintarSugerida`; `analisis.js`, dibujo del lienzo | Cinco filas y cinco propuestas antes de confirmar; otra elección conserva pastilla original; arrastre: Antebrazo 85,9° → 99,3°; quitar puntos y otro perfil comprobados. |
| RF-018 / RF-019 · cálculo y ajustes preservados | `RecorridoCuerpo.RANGOS`, `AJUSTES`, `CARGAS` (solo ayudas); motor existente sin diff | `RangosRecorridoTest` (18 rangos); seis casos de cargas en `HttpCierreTest`; `HttpCuerpoTest`; suite RULA intacta. Un recorrido completo guardó 3 · Amarillo · Nivel 2. |
| RNF-002 · navegación y adaptación | `aplomo.css`, bloque 13F; `RecorridoCuerpo.pasos` (clase de presentación) | 76 medidas: 19 estados × cuatro tamaños, todas 0 vertical / 0 lateral; tarjetas sin texto recortado y Continuar dentro de ventana. |
| RNF-003 · respuesta | Endpoint existente usado por `recorrido.js.consultar`; prueba de rendimiento conservada | Suite `HttpRendimientoTest`: 1.000 filas en 16 tablas, 20 repeticiones por acción; P95 listado 92,4 ms, guardado 13,3 ms, informe 155,9 ms. |
| RNF-004 / RNF-006 · permisos, escape y CSRF | Rutas y servicios existentes; `opcion`, `consultar` | Prueba nueva de ayuda maliciosa escapada; `HttpCierreTest.usuarioNoPuedeAbrirNiGuardarElNuevoRecorrido`; dirección directa como Usuario: Acceso denegado en navegador. |
| RNF-007 · avisos entendibles | `Evaluaciones.validar`, `pintarResumen`, `marcarRecordada`, `evaluacion.js` | Texto nuevo del consentimiento en pruebas HTTP; avisos y pistas inspeccionados en español; captura del otro perfil y de selección distinta. |
| Accesibilidad del encargo | `opcion`, `renderFotos`, `pintarResumen`; CSS de foco/casillas/movimiento reducido | `aria-describedby` únicos; resumen `role=status`; pistas escapadas; proporción de contraste 7,56:1 sobre marfil y 6,06:1 o más sobre el gradiente oscuro. |
| RNF-008 · alcance de esta revisión | Navegador integrado Chromium, controles Playwright | Cuatro ventanas efectivas comprobadas; sin nuevas descargas. No equivale a certificar Safari/iPad físico ni todos los navegadores. |

## Pruebas

`TZ=America/Bogota ./mvnw -Daplomo.evidencia.rendimiento=docs/evidencia_paso13f/rendimiento.json test`

**801 ejecutadas y aprobadas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS.** Duración 1:10 min, final 13 nov 2026 21:13:25 -05. Compilación incluida. Se agregaron dos pruebas HTTP (799 + 2); de las existentes solo se actualizaron las dos expectativas del texto del consentimiento que habían cambiado. Ninguna prueba del motor fue modificada. Sintaxis correcta en los cinco JS modificados y en las comprobaciones de navegador; `git diff --check` correcto.

Las 16 comprobaciones de navegador y las medidas están en `evidencia_paso13f/mediciones.json`. Incluyen el primer puesto sin marcar, preselección/aviso/pastilla del siguiente en postura, momento, tiempo y esfuerzo; permanencia de la pastilla en la opción recordada al cambiar; propuestas completas sin confirmación; sustitución explícita; ✓ al confirmar; actualización al arrastrar; puntos faltantes, ruta manual, otro perfil, consola y permiso de Usuario. `comprobaciones.js` conserva las aserciones reutilizables de primer puesto, recuerdo y propuestas.

## Desplazamiento por paso

Valores en px: `document.documentElement.scrollHeight - innerHeight`. **Las 76 medidas son 0; las 76 medidas laterales también son 0.** Se verificaron además tarjetas sin recorte interno y sus límites dentro de la ventana, y los botones del pie visibles. Se recorrieron los 17 pasos completos hasta Resultado en los cuatro tamaños, con la foto de perfil de muestra y ajustes confirmados; Fotos se midió en tres estados. No se empleó la excepción de dos o más fotos para aprobar una foto.

El navegador conservaba escala 110 %: se compensó la ventana del navegador integrado y se verificó `innerWidth` / `innerHeight` reales en cada caso. Las medidas de la tabla son tamaños CSS efectivos exactos, no los tamaños solicitados a ciegas. Se restauró la ventana al terminar.

| Paso | 1366 × 950 | 1024 × 1290 | 1180 × 740 | 820 × 1100 |
|---|---:|---:|---:|---:|
| Consentimiento | 0 | 0 | 0 | 0 |
| Postura | 0 | 0 | 0 | 0 |
| Momento | 0 | 0 | 0 | 0 |
| Tiempo | 0 | 0 | 0 | 0 |
| Condiciones | 0 | 0 | 0 | 0 |
| Fotos sin selección | 0 | 0 | 0 | 0 |
| Fotos sin archivos | 0 | 0 | 0 | 0 |
| Fotos con una foto | 0 | 0 | 0 | 0 |
| Lado | 0 | 0 | 0 | 0 |
| Brazo | 0 | 0 | 0 | 0 |
| Antebrazo | 0 | 0 | 0 | 0 |
| Muñeca | 0 | 0 | 0 | 0 |
| Giro | 0 | 0 | 0 | 0 |
| Cuello | 0 | 0 | 0 | 0 |
| Tronco | 0 | 0 | 0 | 0 |
| Piernas | 0 | 0 | 0 | 0 |
| Esfuerzo | 0 | 0 | 0 | 0 |
| Carga | 0 | 0 | 0 | 0 |
| Resultado | 0 | 0 | 0 | 0 |

Controles adicionales: cuerpo a **390 × 847**, **810 × 1080** y **1080 × 863**, todos sin desplazamiento lateral. Estos fueron controles puntuales, no recorridos completos adicionales. En celular se conserva el desplazamiento vertical del diseño existente; la garantía de cero desplazamiento vertical corresponde a las cuatro ventanas del encargo.

## Capturas

Todas pertenecen a la app real con datos ficticios en `rula_ergonomia_pruebas`; no son mockups ni vistas inyectadas.

- [Momento elegido, 1366 × 950](evidencia_paso13f/momento-elegido-1366x950.png)
- [Condiciones, 1366 × 950](evidencia_paso13f/condiciones-1366x950.png)
- [Fotos con una foto, 1366 × 950](evidencia_paso13f/Fotos-con-una-foto-1366x950.png)
- [Brazo detectado: resumen y pastilla, 1366 × 950](evidencia_paso13f/Brazo-1366x950.png)
- [Carga, 1366 × 950](evidencia_paso13f/Carga-1366x950.png)
- Complementarias: recuerdo del puesto anterior, recuerdo con elección distinta, sugerencia y elección distinta, segmento confirmado, punto arrastrado, otro perfil y Usuario denegado, en la misma carpeta.

## Checklist y límites

- ✅ 801 pruebas en verde; las pruebas del motor RULA no cambian.
- ✅ Presentación conforme al encargo 13F, conservando paleta, imágenes y componentes del mock; revisadas las cinco pantallas solicitadas y todo el recorrido con foto.
- ✅ 390 / 810 / 1080 / 1366 sin desbordamiento lateral en los controles registrados; cuatro recorridos completos en las ventanas exactas del encargo, sin desplazamiento vertical.
- ✅ Mosaicos, casillas, foco, ayudas asociadas y selección verificables sin depender solo del color; movimiento reducido implementado por media query, sin cambiar preferencias del Mac.
- ❌ Prueba con el dedo en iPad/celular físico: pendiente. Se probaron ventanas de navegador, clic y arrastre reales, no un dispositivo táctil físico.
- ✅ Usuario denegado escribiendo la dirección del asistente a mano; permisos de servidor conservados.
- ✅ Textos y tildes revisados; ayudas literales del encargo.
- ✅ 0 errores de consola registrados durante los recorridos, antes de la prueba negativa de acceso.
- ✅ Sin credenciales, `config/db.properties`, fotos reales ni archivos temporales de QA en Git.
- ❌ Revisión de este cambio en Safari/iPad físico, Edge y Firefox: pendiente; no se descargaron navegadores. Esta entrega usa el Chromium integrado solicitado.

**Lo que no se hizo o quedó a medias:** no hay funciones del encargo pendientes. Quedan las comprobaciones físicas y de otros navegadores anteriores, expresamente diferenciadas de las verificaciones de esta entrega. No hubo cambios de esquema, SQL, rutas, permisos, fórmulas, tolerancia NEUTRO=5°, tecnologías, imágenes ni colores de botones. No se realizaron migraciones ni cargas de demo en la base real.

Los dos servidores temporales de esta sesión usaron únicamente la base de pruebas y cerraron con `QA13E: ROLLBACK completado`; la suite también revierte sus datos. No se borraron datos ni documentos. Se cerró la pestaña de ensayo y se restauró el viewport.

## Cómo lo prueba Andy

1. Abre la app como Administrador y entra a una visita En curso con al menos dos puestos pendientes. En una visita sin recuerdos de esta pestaña, el primer puesto no debe tener respuestas marcadas.
2. Abre un puesto: comprueba el consentimiento en papel. Marca, elige postura, momento, tiempo y condiciones; revisa las pistas y vuelve con Atrás si necesitas corregir.
3. En Fotos, elige Sí, permite fotos; sube una foto de perfil con descripción y Para medir. Si no hay permiso, No permite fotos conserva el guardado sin puntaje.
4. Elige el lado visible. Tras Detectar puntos, comprueba cinco filas, rango sugerido y «Según la foto», con el final todavía Pendiente. Revisa y confirma cada segmento y sus ajustes.
5. Toca un rango diferente: la pastilla permanece en el propuesto y aparece «Tu elección». Arrastra un punto y comprueba que cambian sus medidas. Si no se ve, usa el recorrido manual.
6. Confirma giro, piernas, actividad y carga. Revisa el resultado y guarda con Guardar y evaluar siguiente puesto.
7. En el siguiente puesto, revisa las opciones preseleccionadas, el aviso y «Del puesto anterior»; confirma expresamente. Cambiar una opción no mueve la pastilla del recuerdo. No se copian los ángulos ni los ajustes.
8. En una cuenta Usuario, escribir la dirección de creación o análisis debe mostrar Acceso denegado. Para repetir la suite: `TZ=America/Bogota ./mvnw test` desde la raíz del proyecto.

## Archivos, commit y publicación

Lista completa en [archivos_cambiados.txt](evidencia_paso13f/archivos_cambiados.txt). Producción: cuatro Java de servicio/presentación, cinco JS y el bloque CSS; pruebas: dos clases HTTP. Documentación: estado de AGENTS, fila 13 y contador del README, contador de INSTALAR, flujo e informe/evidencia de esta entrega.

Remote comprobado: `origin = https://github.com/Andybena2025/Aplomo-app.git`, rama `main`. Commit autorizado: `Paso 13F: pistas, opciones que resaltan, resumen de la IA, consentimiento claro e iPad sin desplazamiento`. Se publica con `git push origin main`, sin force. El identificador final del commit y la comprobación de publicación se entregan en la respuesta de cierre; pueden consultarse con `git log -1 --oneline` y `git status -sb`.

Durante la publicación, origin recibió `ae04965`, `8e14e46` y `ca0c440`: fotos de prueba generadas con IA y una sección del README. El primer push fue rechazado; se ejecutó `git pull --rebase origin main` y se integraron sin conflictos ni force, conservando íntegro ese trabajo. Se comprobó que `src`, Maven, scripts y SQL son idénticos al código que pasó las 801 pruebas; las incorporaciones del equipo son fotos y documentación.

Se espera el OK de Andy antes de otro paso.
