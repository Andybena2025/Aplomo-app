# Paso 13F-3 · Visita guiada más simple

Entrega del 14 nov 2026. Base `56f4fd2` (801 pruebas). Se ejecutó `git pull --rebase origin main` antes de comenzar: actualizado. Tras el corte se revisaron `git status` y `git diff`, conservando la implementación y continuando únicamente las verificaciones y ajustes pendientes. No se descargaron navegadores.

## Qué se hizo (puntos 1 a 7)

1. **Postura según el puesto.** Oficina presenta seis opciones: Sentado en computador, Sentado sin computador, Inclinado hacia adelante, Brazos por encima de los hombros, Con herramienta en mano y Otra postura. Planta presenta siete: De pie en mesa o línea, Levantando o cargando, Inclinado hacia adelante, Brazos por encima de los hombros, Con herramienta en mano, Agachado o arrodillado y Otra postura. El modo sigue dependiendo del tipo de puesto. Editar descripción solo edita la postura: Andy lo aclaró expresamente. Otra postura abre el campo existente, obligatorio y de 150 caracteres; no guarda el literal OTRA, sino lo que escribió la persona. No continúa sin texto ni avanza automáticamente mientras se escribe.
2. **Momento sencillo y contextual.** ¿Qué vas a fotografiar?, Lo más difícil del puesto / Lo que más dura, con las pistas exactas y una sola frase común. Los valores siguen siendo MAS_INADECUADA / MAYOR_EXPOSICION. Según la elección cambian título de Tiempo, instrucción de Perfil y Frente, ayuda corta del cuerpo y resumen final. Atrás conserva las respuestas. No se cambió el motivo que genera el servidor ni sus reglas de guardado.
3. **Fotos una por una.** Permiso → Perfil obligatorio → Frente opcional → Detalle opcional → resumen. Muestra ocupa el marco completo y su etiqueta está dentro de la imagen; una foto elegida la reemplaza, con Cambiar foto y Quitar. Frente y Detalle se pueden omitir expresamente. La pantalla final reúne miniaturas y Agregar otra foto, hasta diez. No se piden descripción ni uso en las nuevas tomas. Perfil/Frente asignan MEDIR y sus nombres; Detalle y extras asignan APOYO y los textos pedidos. Los campos enviados y el formato de análisis siguen siendo los existentes; no se creó una columna uso: como antes, MEDIR genera la solicitud de análisis y APOYO conserva solo la foto. Perfil se exige también al guardar un recorrido guiado con fotos en el servidor. No permite fotos mantiene el guardado sin archivos ni puntaje. La carga conserva JPG/PNG reales, 5 MB, máximo diez y los mensajes que conservan lo escrito. El título de cada toma recibe el foco; todas las muestras tienen texto alternativo.
4. **Zoom.** Control de contorno 1× / 1.5× / 2× / 3×. La caja se desplaza por dentro al acercar, con ratón o puntero táctil; un toque marca y arrastrar cerca de un punto lo corrige. Los puntos se normalizan usando el rectángulo real del lienzo transformado; no se cambian grados ni puntajes en el navegador. Se marcan los mismos tres puntos de ensayo a 1× y a 2×: Java devuelve **47,5° en ambos casos**. Desplazar 60 px horizontal/vertical no modifica el ángulo. A 1× no aparecen barras ni marcos laterales. No se cambió el viewport de producción ni se bloqueó el zoom general de la página.
5. **iPad.** Se corrigieron los tamaños y mínimos de las columnas, las ayudas laterales y la pastilla. Los textos y las imágenes permanecen completos; no se ocultó el desbordamiento del documento para aprobar las medidas. Tronco a 1366×950 termina en cero, incluida la corrección del píxel residual señalado. Los cuatro tamaños exactos se comprobaron en ambos modos.
6. **Pruebas y entrega.** Diez casos HTTP nuevos, suite completa, recorridos reales con guardado y evidencia. El motor y sus pruebas siguen intactos. Se reutilizaron las fotos S6 de perfil, frente y detalle del repositorio.
7. **Imágenes que llenan su espacio.** Revisadas las **56 imágenes** actuales de `static/img/tour/`, visibles en `galeria-revisada.png`. Se quitaron el relleno y el plato oscuro de las fichas; cover y proporciones por tipo/tamaño llenan el ancho. Se usan posiciones específicas para posturas y dibujos cuyo cuerpo necesita desplazar el recorte horizontal. Las fotos de muestra y de la persona conservan proporción real dentro de un marco ajustado a la imagen, sin barras laterales a 1×. No se modificaron los archivos de imagen ni las coordenadas para lograrlo. La foto de puntos es mayor y queda junto a sus opciones; las ayudas se distribuyen al lado cuando hace falta para conservar todo visible. Hay capturas anteriores y posteriores de Postura, Brazo y la foto.

## Aclaraciones explícitas de Andy

- No añadir cambio de modo: Oficina/Planta depende del puesto; Editar descripción solo cambia la postura.
- Conservar los recuerdos de 13F preseleccionados, con Del puesto anterior, aviso y Continuar explícito; ninguna respuesta nueva aparece seleccionada por defecto. Las propuestas de la foto tras Detectar puntos se mantienen. Una postura ajena a la lista del modo no se incorpora como opción oculta ni se convierte silenciosamente en Otra postura.
- Perfil y Frente del otro lado también son MEDIR, con descripción Perfil · otro lado / Frente · otro lado. Detalle y extras son APOYO. Se reutilizan las mismas tres pantallas para el otro lado; no se duplicó el recorrido de tomas en el DOM.

La simplificación de carga se aplica a las nuevas tomas después del permiso de fotos. Añadir análisis conserva la selección de uso para archivos ya guardados y comparte el zoom y el texto contextual del criterio guardado. No se implementó el nuevo botón del Paso 13F-2; se conserva la hoja de ajustes existente.

## Requisitos → archivo y método → prueba

| Requisito / encargo | Archivo y método | Comprobación |
|---|---|---|
| RF-014 · listas por puesto y Otra postura | `PaginasEvaluacion.nueva`, `posturasDelModo`; `evaluacion.js.actualizarModo`, `mostrarDescripcion`, `respuestaCompleta` | Dos casos `oficinaYPlantaMuestranListasExactasDistintas`; `otraPosturaExigeTextoYConservaSus150Caracteres`; campo vacío y texto válido comprobados en navegador. |
| RF-014 · momento, valores conservados | `PaginasEvaluacion.nueva`; `evaluacion.js.adaptarMomento`; `analisis.js.paso6` | Dos casos `momentoCambiaEtiquetaYConservaValorGuardado`; títulos e instrucciones comprobados con ambas elecciones en los cuatro tamaños; resumen final y guardado de ambos recorridos. |
| RF-015 · condiciones preservadas | Campos y validación existentes, sin cambios | `HttpRecorridoTest` conserva rechazo de condiciones vacías/contradictorias y nota de 255; suite completa en verde. |
| RF-016 · tomas, límites y metadatos | `FotosGuiadas.pantallas`; `fotos-guiadas.js.metadata`, `recibir`, `avanzar`; `EvaluacionesWeb.atender` (guardado) | `fotosUsanUnSoloRecorridoSinDescripcionNiClasificacionManuales`; dos casos `perfilEsObligatorioAunqueLleguenOtrasTomas`; `metadatosAutomaticosYFotosDeApoyoSeGuardanSinAnalisis`; omisión y No permite fotos comprobados en navegador. |
| RF-017 · zoom sin alterar medida | `PaginasAnalisis.pasos`; `analisis.js.ajustarZoom`, `posicion`, eventos de puntero | `zoomTieneControlesAccesiblesSinCamposDeCalculoNuevos`; marcado real 1×/2× = 47,5°, `zoom.json` y sus dos capturas; desplazamiento local sin mover puntos. |
| RF-018 / RF-019 / RF-020 · motor y resultado conservados | Motor existente, endpoint de cálculo idéntico; `analisis.js.paso6` solo añade etiqueta del momento | Suite RULA sin cambios; recorrido Planta y Oficina guardado con dos análisis y resultado Rojo 7; Detalle no genera análisis. |
| RNF-002 · pasos claros y adaptación | `FotosGuiadas`, `evaluacion.js.mostrar`, progreso; CSS bloque 13F-3 | 96 medidas completas sin desbordamiento y sin tarjetas recortadas; foco en cada nuevo título; 390/810/1080 adicionales sin desplazamiento lateral. |
| RNF-003 · rendimiento preservado | Endpoint existente y prueba de carga existente | `HttpRendimientoTest`, 1.000 registros en 16 tablas, 20 repeticiones por acción; P95 listado 89,4 ms, guardado 12,1 ms, informe 145,6 ms. |
| RNF-004 / RNF-006 · permisos, archivos, CSRF | Rutas y servicios existentes; nueva comprobación de Perfil después del permiso | Usuario por dirección directa: Acceso denegado. Suite conserva CSRF, consentimiento, archivos falsos, tamaños, límite y rutas protegidas. |
| RNF-007 · mensajes y textos | `FotosGuiadas.pantallas`, `fotos-guiadas.js.recibir`; `evaluacion.js` | Pistas literales y textos de las dos variantes; errores conservan fotos previas; pruebas de validación y capturas en español. |
| RNF-008 / accesibilidad visual del encargo | CSS, controles nativos y zoom etiquetado | Chromium integrado, Playwright y puntero CUA; nombres accesibles y foco. No se presenta como prueba de dedo físico ni de todos los navegadores. |

## Resultado de las pruebas

`TZ=America/Bogota ./mvnw -Daplomo.evidencia.rendimiento=docs/evidencia_paso13f3/rendimiento.json test`

**811 ejecutadas y aprobadas, 0 fallos, 0 errores y 0 omitidas; BUILD SUCCESS.** Duración 1:11 min; final 14 nov 2026 06:08:20 -05. Base 801 + diez casos nuevos. Se actualizan las expectativas de imágenes de las dos pruebas que antes exigían mostrar ambos modos a la vez; ahora comprueban inclusión y exclusión según el puesto. No se quitaron aserciones de seguridad ni validaciones. `HttpEvaluacionesTest` sigue pasando sin necesitar cambios de texto. Ninguna prueba del motor RULA cambió.

Los diez casos nuevos son dos de lista exacta por modo, uno de Otra postura (vacío/151 rechazados, 150 conservados), dos de criterio y tiempo guardados, uno de recorrido de fotos sin campos manuales, dos de perfil obligatorio, uno de metadatos/uso de seis fotos y uno de controles accesibles de zoom. La comprobación del ángulo al remarcar se ejecutó en el navegador real, adicional a Maven: coordenadas normalizadas equivalentes, respuesta del servidor 47,5° a ambas escalas. `zoom.json` conserva los datos; las capturas muestran ambos estados.

Las 70 pruebas dirigidas también terminaron en verde. Durante el desarrollo se corrigieron un dato de prueba de 151 en vez de 150 caracteres y una búsqueda que incluía el textarea de observaciones ajeno a Fotos. Una tanda coincidió por error con el servidor comparativo y sufrió dos bloqueos de la base de pruebas; se cerró y se repitió sin solapar. La suite final se ejecutó con todos los servidores de QA cerrados. Tras la interrupción hubo que abrir una pestaña y sesión nuevas para continuar; los recorridos finales se comprobaron con sesión vigente. No se ocultaron fallos ni se cambiaron las reglas del cálculo para aprobar pruebas.

## Medidas finales

Cada celda indica **Planta / Oficina**, en px: `document.documentElement.scrollHeight - innerHeight`. Todas las medidas laterales también son cero. Los tamaños son `innerWidth`/`innerHeight` efectivos, comprobados tras ajustar la ventana del navegador integrado; no se atribuyen a un iPad físico.

| Pantalla | 1366×950 | 1024×1290 | 1180×740 | 820×1100 |
|---|---:|---:|---:|---:|
| Postura | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Momento | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Tiempo | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Perfil-antes | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Perfil-después | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Frente-antes | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Frente-después | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Detalle-antes | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Detalle-después | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Fotos-final | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Brazo | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Tronco | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |

Además de cero desplazamiento, se verificaron tarjetas sin recorte interno, botones Continuar dentro de la ventana e imágenes cargadas. Las tomas se midieron antes y después de elegir el archivo. El zoom a 2× también dejó el documento sin desplazamiento: el desplazamiento permitido se queda dentro de la foto.

Los controles adicionales de 390×847, 810×1080 y 1080×863 no tienen desbordamiento lateral; fueron controles puntuales del cuerpo. El cero vertical corresponde únicamente a las cuatro ventanas y pantallas de la tabla, no a celular ni a cualquier altura arbitraria.

## Evidencia visual

En `docs/evidencia_paso13f3/`:

- Postura, Momento, Tiempo (ambas variantes), Perfil antes/después de elegir archivo, Fotos final, Brazo y Tronco: capturas a 1366×950 de ambos modos.
- [Postura anterior](evidencia_paso13f3/postura-antes-1366x950.png) / [Postura nueva](evidencia_paso13f3/Postura-Planta-1366x950.png).
- [Brazo anterior](evidencia_paso13f3/brazo-antes-1366x950.png) / [Brazo nuevo](evidencia_paso13f3/brazo-después-1366x950.png).
- [Foto anterior](evidencia_paso13f3/persona-antes-1366x950.png) / [Foto nueva](evidencia_paso13f3/persona-después-1366x950.png).
- [Brazo con zoom 2×](evidencia_paso13f3/Brazo-zoom-2x-Planta-1366x950.png), marcado a ambas escalas, desplazamiento, las tomas del otro lado y extra, opcionales omitidas, resultados guardados y Usuario denegado.
- [Medidas completas](evidencia_paso13f3/mediciones.json), [comparación del zoom](evidencia_paso13f3/zoom.json), rendimiento y revisión de las 56 imágenes originales.

Las vistas anteriores se obtuvieron del commit base en un servidor temporal separado, no simulando CSS viejo encima del nuevo. Las posteriores son la app modificada. Se reemplazó una captura de Perfil de Oficina tomada antes de la actualización visual; la final muestra la foto S6 elegida y vuelve a comprobar cero desplazamiento. Todas usan datos ficticios en `rula_ergonomia_pruebas` y las imágenes de prueba generadas con IA del repositorio.

## Checklist / lo que no se hizo

- ✅ 811 pruebas aprobadas, compilación incluida; sintaxis de JavaScript y `git diff --check` correctos.
- ✅ Los puntos 1 a 7 están implementados y comprobados; las capturas siguen la paleta y componentes del mock con las modificaciones expresas del encargo.
- ✅ 96 medidas finales sin desbordamiento; anchos adicionales 390/810/1080 y principal 1366 sin deslizar de lado.
- ✅ Zoom y arrastre/desplazamiento con puntero real, sin cambiar la respuesta de Java; los controles nuevos tienen nombres accesibles y tamaño táctil por CSS.
- ❌ Prueba con dedo/pellizco en iPad físico: pendiente. La revisión se hizo en ventanas Chromium; no se descargaron navegadores ni se certificaron Safari, Edge y Firefox en esta entrega.
- ✅ Usuario denegado al escribir la dirección del asistente; permisos de servidor intactos.
- ✅ Tildes y mensajes en español; foco en los títulos nuevos; recuerdos explícitos conservados.
- ✅ Cero errores de consola registrados en los recorridos finales.
- ✅ Sin credenciales, `config/db.properties`, fotos de personas reales ni archivos temporales de QA en Git.
- ✅ Sin cambios en esquema, SQL, motor, tablas RULA, NEUTRO=5°, endpoint de cálculo, dependencias, colores de botones o reglas de puntuación.

**Pendiente funcional del encargo:** nada. Quedan la revisión de Andy y las comprobaciones físicas/de otros navegadores ya diferenciadas arriba. **No se implementó el Paso 13F-2.** No se cambiaron los archivos de imágenes originales, el motivo guardado ni los tiempos. La única nueva condición del guardado es exigir el Perfil solicitado en el recorrido guiado con fotos.

Los tres servidores de QA de este paso cerraron con `QA13E: ROLLBACK completado`. No se escribieron datos de ensayo en la base real, no se borraron datos ni documentos, y las fotos de QA solo estuvieron en su carpeta temporal. Se cerraron las pestañas de ensayo y se restauró el tamaño del navegador.

## Cómo lo prueba Andy

1. Abre la app como Administrador. En una visita En curso, abre primero un puesto de Oficina y después uno de Planta: sus listas son distintas. El botón Editar descripción no cambia el modo.
2. Elige Otra postura: Continuar exige texto y admite hasta 150 caracteres. Vuelve y elige una foto de postura si prefieres.
3. Alterna Lo más difícil del puesto / Lo que más dura con Atrás. Comprueba la pregunta de Tiempo, la instrucción de Perfil/Frente, la frase del cuerpo y la etiqueta del resultado; el criterio sigue guardándose con los mismos dos valores.
4. Permite fotos. Sube Perfil; omite Frente y Detalle, o súbelos. Se sustituyen las muestras y no se piden descripciones ni uso. La pantalla final siempre permite continuar con Perfil. Quitar el Perfil vuelve a exigirlo.
5. Prueba Otro lado del cuerpo y una foto adicional: las dos primeras vistas del otro lado se miden; el detalle y las extras quedan de apoyo. Se mantiene el límite de diez archivos y 5 MB por foto.
6. En Brazo, acerca a 1.5×, 2× y 3×; desplaza la imagen y corrige un punto. El zoom no cambia las medidas existentes. Confirma cada segmento, sus ajustes, actividad y carga; guarda y revisa las fotos y resultados.
7. Guarda otra evaluación con No permite fotos: conserva el contexto sin archivos ni puntaje. En el siguiente puesto se mantienen los recuerdos compatibles con su lista, con aviso y confirmación.
8. Como Usuario, la dirección directa de creación debe mostrar Acceso denegado. Para repetir la suite desde la raíz: `TZ=America/Bogota ./mvnw test`.

## Archivos y Git

Lista completa en [archivos_cambiados.txt](evidencia_paso13f3/archivos_cambiados.txt). Cambian cuatro Java de presentación/adaptación, tres JS (uno nuevo), CSS y dos clases HTTP; se actualizan flujo, estado y contadores, con informe y evidencia. Los contadores del README/INSTALAR son 811; del README solo se modifica la fila 13 y ese contador.

Origin comprobado: `https://github.com/Andybena2025/Aplomo-app.git`, rama `main`. Commit autorizado: `Paso 13F-3: visita guiada más simple, momento que adapta los textos, fotos una por una y zoom`. El hash definitivo y la confirmación del push se entregan en el mensaje de cierre y pueden verificarse con `git log -1 --oneline` y `git status -sb`. Se espera el OK de Andy antes de otro paso.
