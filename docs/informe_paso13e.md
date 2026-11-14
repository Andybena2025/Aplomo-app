# Paso 13E · Evaluación guiada

## Fase A · Visita, contexto y fotos · 13 nov 2026

Implementada por encargo de Andy del 13 nov 2026. Se detiene aquí: Fases B y C pendientes de su OK.

### Alcance y decisiones explícitas

Andy indicó mantener, después de las fotos, **la medición actual, ajustes, carga y resultado como continuación provisional**, para que el producto siga calculando entre fases. La pantalla Derecho / Izquierdo / Ambos y el recorrido corporal se trasladan a la Fase B; no se simulan ni se guardan elecciones de lado sin análisis. También indicó **no copiar la nota al motivo**: el motivo solo dice «Momento: … Postura: …» y Otras observaciones conserva la nota completa.

Se comprobó origin y rama `main`, y se hizo `git pull origin main` antes de trabajar: ya estaba actualizado en `417f72a`. No se descargaron ni instalaron navegadores. Se usó Safari existente y el navegador integrado. No hay cambios de esquema ni modificaciones del paquete `co.aplomo.rula`.

### Qué se implementó

La visita En curso del Administrador presenta los puestos activos del salón como tarjetas: pendiente o evaluado **en esa visita**, con el riesgo de la última evaluación de esa visita cuando tiene puntaje. Una evaluación sin fotos queda Evaluado / Sin puntaje disponible; no se convierte en Verde ni en cero. Un puntaje de otra visita no cambia el pendiente de la actual. Tocar un puesto abre la dirección solicitada, con visita y puesto; el servidor rechaza puestos ajenos o inactivos. Finalizar conserva su regla de al menos una evaluación y su cálculo existente.

Iniciar visita se desactiva si no hay puestos activos y ofrece Registrar un puesto. El servidor vuelve a comprobarlo dentro de la transacción, con bloqueo de las filas consultadas; no se crea visita ni auditoría de inicio ante el rechazo. El dato de disponibilidad se obtiene en la consulta de reservas, sin consultas individuales desde cada tarjeta.

El nuevo contexto ocupa seis pantallas: consentimiento físico, postura, momento, tiempo, condiciones y fotos. Si se entra por una dirección antigua sin `puesto`, primero aparece una pregunta adicional para elegirlo, sin elegir uno por defecto. Se calcula el modo desde el tipo real del puesto: ESCRITORIO → Oficina; otros → Planta. Oficina prioriza los dos mosaicos sentados; Planta conserva el orden del encargo empezando por De pie. Los consejos son los textos solicitados.

Postura tiene ocho opciones y descripción editable de hasta 150 caracteres. Los tiempos guardan 30/90/180/300 minutos. El servidor genera el motivo a partir de criterio y postura, ignorando el motivo enviado; no incorpora la nota. Condiciones tiene las seis opciones originales y Ninguna excluyente; las ayudas se mantienen. La nota admite 255 caracteres completos. El servidor rechaza los valores fuera de los cuatro tiempos del recorrido y una declaración de condiciones incompatible.

La casilla física es obligatoria tanto en interfaz como en servidor. Para nuevas evaluaciones, `persona_acepto=true` confirma **consentimiento físico**, incluso con SIN_REGISTRO_FOTOGRAFICO. Esto sustituye la semántica anterior del encargo de RF-016 por instrucción explícita del Paso 13E. La autorización fotográfica se expresa por separado con Con fotos / Sin fotos. No se alteraron datos históricos ni comentarios del esquema; no se infiere retrospectivamente que los registros anteriores tengan consentimiento físico firmado.

Con fotos se conservan tomas, carga local, validación real JPG/PNG, máximo 10 y 5 MB, vista y descripción de 200 caracteres. Cada foto exige Para medir / De apoyo; las de apoyo se guardan sin análisis. Al menos una debe elegirse Para medir para continuar al cálculo. Esa elección controla la selección de fotos del analizador actual; no se añadió una columna. Sin fotos, el botón cambia directamente a Guardar evaluación en la sexta pantalla y no se envían archivos ni análisis; el servidor rechaza cualquier archivo que llegue.

Se conservó el lienzo y la detección existentes como puente. Se retiraron valores automáticos de giro, piernas, actividad y carga; ajustes exige seleccionar alguno o Ninguno de estos ajustes. No se continúa al resultado con respuestas pendientes y el adaptador HTTP rechaza valores ausentes, en vez de convertirlos en No. El lado de esta pantalla sigue siendo el selector anterior, porque Andy trasladó el nuevo paso de lado a B. No se construyeron aún los segmentos por pantalla ni la hoja inferior de B.

Una pregunta de contexto queda visible a la vez. Los mosaicos usan controles nativos, grupos etiquetados, foco visible, 160 px mínimos y dos columnas en celular, tres en tableta y cuatro en ancho grande. La pregunta pendiente tiene borde y aviso ámbar; Continuar queda desactivado. Una elección única de postura/momento/tiempo avanza tras 250 ms. Con movimiento reducido no hay autoavance ni transición; la regla está implementada, sin afirmar que se cambió la preferencia física del sistema para probarla. Hay un solo Atrás del recorrido, barra de progreso y Paso X de N; la navegación permanece visible al desplazarse. Se mantienen paleta, tipografías, pie, laterales y tamaños 48/44 de la app.

Las imágenes `static/img/tour/` todavía no están en el repositorio. Se creó la carpeta con `.gitkeep`; el servidor omite la etiqueta de imagen ausente y muestra mosaico con texto y borde, sin URL rota ni petición 404. No se generaron imágenes sustitutas: Claude las incorporará después, como dice el encargo.

### Trazabilidad

| Requisito / encargo | Archivo y método | Comprobación |
|---|---|---|
| A1 · RF-013a, RF-014 | `Visitas.puestos`, `VisitasWeb.atender`, `PaginasVisitas.detalle` / `puestos` | `HttpRecorridoTest.visitaListaSoloActivosConEstadoPropioDeEstaVisita`; captura visita a 1366/1024/390; estado rojo anterior del puesto no sustituye el de la visita. |
| A2 · RF-013a | `ReservaDao.CONTEXTO` / `Ficha`, `PaginasReservas.detalle`, `Visitas.iniciar` | `sinPuestosActivosBloqueaInicioEnInterfazYServidorSinCrearVisita`: vacío, solo inactivo, reactivado, interfaz y POST; `inicio-bloqueado-1366.png`. |
| A3 · RF-014 | `PaginasEvaluacion.nueva`; `evaluacion.js`, `actualizarModo` | Prueba parametrizada Oficina/Planta; ambas pantallas observadas en navegador; captura `postura-planta-1366.png`. |
| A4.1 · RF-016 | `PaginasEvaluacion.nueva`; `Evaluaciones.validar` | Rechazo HTTP con/sin fotos sin consentimiento; Continuar desactivado antes de marcar; guardado sin fotos en Safari. |
| A4.2–4 · RF-014 | `PaginasEvaluacion.nueva` / `opcion`; `evaluacion.js`, `mostrar` / `armarMotivo`; `Evaluaciones.validar`; `EvaluacionesWeb.atender` | Cuatro tiempos por HTTP; motivo automático ignora texto manipulado; descripción personalizada; avance automático y teclado observados. |
| A4.5 · RF-015 | `CONDICIONES`, `nueva`; `evaluacion.js` manejadores de cambio; `EvaluacionesWeb.atender` | Ninguna desmarca las seis; marcar una desmarca Ninguna. Pruebas sin respuesta o contradictorias; nota de 255 intacta y de 256 rechazada por suite existente. |
| A4.6 · RF-016 / RNF-006 | `evaluacion.js`, `renderFotos` / `validarFotos` / envío; `analisis.js`, estado por foto; `Evaluaciones.guardar`; `Fotos` existente | Carga real de muestra, Para medir, cálculo y guardado observados. Suite conserva 11 fotos/PDF/PDF renombrado/6 MB, rechazo sin consentimiento y permisos de foto. Sin fotos guarda sin archivos ni puntaje. |
| A4.7 · traslado autorizado | Selector anterior de `PaginasAnalisis` conservado | Nuevo paso de lado y Ambos pendientes de B por respuesta expresa de Andy; no se afirma implementación. |
| RF-017 · puente conservado | `PaginasAnalisis.pasos`; `analisis.js` / lienzo y mediciones | Cinco ángulos manuales y foto de muestra en recorrido real; respuesta 7 en vista previa y análisis guardado. Detección mantiene su carga y revisión existentes. |
| RF-018, RF-019 · motor preservado / sin defaults | `DatosAnalisis.leer` / `si`; `analisis.js`, estado / `respondido` / `medir` / `paso6` | Cinco casos HTTP con campo ausente (giro, piernas, actividad, carga, ajuste) rechazados; 10 casos RULA en pantalla y suite del motor existentes en verde. `puenteActualSigueCalculandoYGuardandoElPuntajeDelServidor` ignora puntaje manipulado. |
| RNF-002, RNF-007 · navegación / mensajes | `aplomo.css`, `.tour-*`; `evaluacion.js`, `mostrar`, `actualizarGuia` | 28 medidas finales sin desbordamiento; mosaicos ≥160 px; una pantalla visible; Atrás a 12 px del borde superior tras desplazar 2755 px. |
| RNF-004, RNF-006 · permisos / CSRF | Router y servicios existentes, sin ampliar rol Usuario | Nuevo test por dirección ajena/inactiva; Usuario denegado en navegador al escribir la dirección, captura `usuario-denegado-390.png`; suite de permisos y CSRF en verde. |

### Pruebas

`TZ=America/Bogota ./mvnw -Daplomo.evidencia.rendimiento=docs/capturas_13e/fase_a/rendimiento.json test`: **745 ejecutadas y aprobadas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, 1:10 min, final 13 nov 2026 13:56:10 -05. Compilación incluida. También se comprobaron ambos JavaScript con `node --check` y el diff con `git diff --check`.

Se agregaron **22 casos de `HttpRecorridoTest`**. El caso anterior que exigía motivo escrito se retiró porque ahora se genera en el servidor; lo cubren los nuevos casos de motivo automático y texto manipulado. Total: 724 − 1 + 22 = 745. Se actualizaron solo las expectativas y fixtures afectados por el consentimiento físico, motivo generado y requisito de puesto activo; se conservan validaciones de archivos, permisos, transacciones y motor. Un primer intento detectó un import faltante; otro detectó dos salones de prueba con nombre duplicado y la carpeta de evidencia aún no creada. Se corrigieron las preparaciones y se repitió la suite completa; no se ocultaron fallos.

### Revisión visual

Evidencia en `docs/capturas_13e/fase_a/`. Las capturas de visita, postura, condiciones, fotos, ajustes provisionales y resultado provisional están a **1366/1024/390**. Las medidas finales incluyen visita/postura/condiciones/fotos/resultado también a **810/1080**, y ajustes a los tres tamaños principales: 28 registros. `mediciones-finales.json` registra anchos, pregunta visible, controles y alturas. Documento sin desbordamiento; botones de 48 px, encabezado de 44; mosaicos finales de 160 px o más.

El primer ensayo conservó medidas separadas en `mediciones-iniciales.json`; las capturas principales se reemplazaron por las finales tras quitar el Atrás duplicado, fijar la navegación y ampliar los mosaicos de uso de foto. `navegacion-fija.json` y PNG confirman que Atrás sigue arriba al desplazarse. Se probó seleccionar con Espacio, volver, editar descripción y conservar respuestas en el navegador integrado. No se presenta como certificación completa de accesibilidad ni como interacción con dedo físico.

**Safari 26.6.2 instalado:** recorrido Planta completo sin fotos, Ninguna, nota y guardado con 90 minutos, motivo automático y sin puntaje; capturas `safari-*.png`. La versión final se volvió a abrir: Oficina, consentimiento obligatorio, un solo Atrás y mosaicos; `safari-postura-final.png`. Las capturas de anchos exactos pertenecen al navegador integrado, no se atribuyen a Safari físico de esos tamaños.

El recorrido con foto se completó dos veces en navegador integrado: ángulos 110/40/25/30/75, Ninguno, giro extremo, piernas sin apoyo, actividad Sí y carga mayor de 10 kg estática/repetitiva; vista previa **7 · Rojo · Nivel 4**, luego mismo puntaje guardado. No hay errores de JavaScript registrados. Hay **tres avisos de MediaPipe/WebGL** (inicialización y comprobación de OpenGL), conservados en `consola-final.json`; no se ocultan como consola totalmente vacía.

Ambos servidores de QA usaron solo `rula_ergonomia_pruebas`, cuentas ficticias y carpeta propia de fotos; ambos terminaron con **QA13E: ROLLBACK completado**. No hubo cargas ni escrituras de QA en `rula_ergonomia`; no se borraron datos ni documentos. Se cerraron las ventanas privadas y pestañas creadas; se restauró el viewport. No se guardaron las contraseñas ficticias en Safari.

### Checklist y lo pendiente

- ✅ 745 pruebas en verde; motor y tablas RULA sin cambios.
- ✅ Recorrido A conforme al diseño textual aprobado, con fallback sin imágenes; continuación provisional acordada.
- ✅ 390 / 810 / 1080 / 1366 y 1024 sin desbordamiento en las combinaciones registradas.
- ✅ Botones 48/44 px, opciones ≥160 px, radios nativos, foco y teclado comprobado en navegador integrado.
- ✅ Usuario denegado por dirección directa; CSRF y validación de fotos conservados.
- ✅ Tildes y avisos en español; nota separada del motivo.
- ✅ Sin errores JS; tres avisos informativos del runtime de detección documentados.
- ✅ Sin archivos sensibles en Git; fotos y datos de ensayo revertidos.
- ❌ Prueba con dedo en iPad/celular físico: pendiente.
- ❌ Movimiento reducido: implementado por media query, no se cambió la preferencia real del equipo para comprobarlo.
- ❌ Imágenes del recorrido: pendientes de Claude; el fallback solicitado está operativo. Los ocho nombres de postura son los del encargo: `postura_de_pie.jpg`, `postura_computador.jpg`, `postura_sentado.jpg`, `postura_inclinado.jpg`, `postura_brazos_arriba.jpg`, `postura_cargando.jpg`, `postura_herramienta.jpg`, `postura_agachado.jpg`.
- ❌ B y C: pendientes de OK. Incluyen lado/Ambos, segmentos y hoja inferior, esfuerzo por pantalla, resultado reorganizado, siguiente puesto, respuestas recordadas y adaptación completa de Añadir análisis.

No se hicieron cambios de esquema, cálculo RULA, dependencias ni roles. La documentación del flujo describe solo A y el puente actual; la revisión integral y diagramas de B/C se dejan para sus fases. Chrome/Edge/Firefox no se descargaron ni se repitieron: el encargo actual pide Safari.

### Cómo lo prueba Andy

1. Entra como Administrador, abre una reserva En cola y revisa el salón. Sin puestos activos, Iniciar visita debe estar desactivado con enlace para registrar uno; intentar el POST tampoco crea visita.
2. Inicia una visita de un salón con puestos. Abre un pendiente desde su tarjeta. Solo aparecen activos del salón y su estado dentro de esa visita.
3. Sin marcar consentimiento, no puedes continuar. Márcalo, elige postura, momento y tiempo; vuelve con Atrás y comprueba que siguen elegidos. Editar descripción permite personalizar la postura.
4. En condiciones, alterna Ninguna y alguna de las seis. Escribe una nota: se conservará en observaciones, separada del motivo.
5. Sin fotos: Guarda directamente; no se crea foto ni puntaje. Con fotos: agrega 1–10 JPG/PNG válidos y elige Para medir / De apoyo en cada una; necesitas al menos una para medir.
6. Con foto, continúa por el lienzo anterior. Completa los ángulos, responde ajustes o Ninguno, giro, piernas, actividad y carga. Revisa el resultado y guarda. Lo almacenado lo calcula el servidor.
7. Abre una evaluación sin fotos y otra con resultado: revisa tiempos, motivo y nota. Prueba escribir la dirección como Usuario: debe denegarse. En celular, desplázate y comprueba Atrás arriba sin deslizar de lado.
8. Para repetir pruebas, ejecuta `TZ=America/Bogota ./mvnw test` con la base de pruebas preparada: deben aprobarse 745.

### Git y parada de fase

Commit solicitado para esta entrega: `Paso 13E-A: visita, contexto y fotos guiados`. La salida real de `git log --oneline` y el resultado del push se comunican al ejecutar ambos, en el resumen final. Solo se sube a `origin main` si origin sigue apuntando exactamente a `https://github.com/Andybena2025/Aplomo-app.git`. README cambia únicamente fila 13 y número de pruebas; INSTALAR, solo número. Estado y decisiones se actualizan en AGENTS. **Se espera el OK de Andy antes de B.**


## Fase B · Recorrido corporal · 13 nov 2026

Andy aprobó explícitamente A y autorizó B; **C sigue esperando su OK**. Antes de programar se comprobaron `origin` y `main` y se ejecutó `git pull --rebase origin main`, incorporando las imágenes de Claude de `f8b45c6` y `3fdc88a`. Se usaron sus nombres exactos, incluidos `postura_de_pie.jpg`, `postura_computador.jpg` y `muneca_desviada.png`. Las observaciones de A que decían imágenes pendientes quedan resueltas para esta entrega.

### Qué cambió

Después de fotos aparece Derecho / Izquierdo / Ambos. Cada foto Para medir tiene sus propias respuestas por lado; las De apoyo siguen sin análisis. El recorrido pregunta Brazo, Antebrazo, Muñeca, Giro de muñeca, Cuello, Tronco y Piernas, con progreso, Atrás y marcas de completado. Foto grande y mosaicos comparten pantalla en escritorio; en celular se apilan. Solo los puntos del segmento actual se resaltan; los restantes quedan tenues. Se conserva detección y arrastre. Si la detección corresponde al perfil opuesto al lado elegido, no se reutilizan sus puntos: se avisa y sigue disponible la elección por observación directa.

Los 18 rangos usan exactamente las imágenes y ángulos representativos del encargo. Confirmar el sugerido conserva el ángulo real medido; elegir otro o trabajar sin puntos guarda el representativo con corrección manual. Corregir medición conserva el número escrito, incluso si pertenece al mismo mosaico sugerido; Usar los puntos de la foto restaura la medición. No se introdujeron fórmulas de clasificación RULA en JavaScript.

Brazo, antebrazo, muñeca, cuello y tronco abren la hoja inferior nativa, con título, `role="dialog"`, `aria-modal`, foco atrapado, Escape y devolución del foco. Ajustes múltiples y Ninguno son excluyentes. **Listo confirma y avanza; Cerrar/Escape descarta los cambios sin confirmar ni avanzar.** Giro y piernas requieren elección expresa. Pies colgando llega con Sin buen apoyo sugerido, pero no cuenta como respuesta hasta confirmarlo. No se ve en la foto oculta el lienzo para ese segmento y permite observación directa.

Andy aprobó en este chat que el chip parcial muestre **solo los puntos del segmento confirmado, calculados por el servidor con sus ajustes confirmados**. Se reutiliza `/evaluaciones/calcular`, protegido por sesión, Administrador, visita En curso y CSRF; no se creó una ruta. La respuesta parcial nunca contiene el puntaje final. Los componentes aislados Giro/Piernas se proyectan desde el puntuador existente, sin usar valores auxiliares para calcular o presentar un resultado completo. El final dice **Pendiente** durante recorrido y esfuerzo; no se solicita un cálculo completo hasta que todos los análisis tienen segmentos, actividad y carga respondidos. El servidor además rechaza un cálculo completo con cualquiera de esos datos ausente.

Ambos guarda un análisis por foto y lado en la transacción existente. La validación admite hasta 20 análisis (10 fotos × 2 lados), con unicidad foto+lado por envío, conservando el mayor de la evaluación. El límite de partes multipart pasa de 64 a 80 para permitir los 20 análisis más metadatos de 10 fotos; los límites de archivos, tamaños y cuerpo permanecen. Los campos visuales del recorrido no se envían al guardar. No cambian esquema, rutas, roles, dependencias ni el paquete `co.aplomo.rula`; NEUTRO=5° y tablas leídas de MySQL permanecen intactos.

Los dos ajustes de QA de A están incluidos: llegada sin borde ámbar ni aviso; Responde para continuar solo tras intentar continuar sin respuesta. Continuar incompleto usa `aria-disabled` y el manejador bloquea el avance, permitiendo mostrar la ayuda al intento con mouse o teclado; durante operaciones en curso se desactiva también de forma nativa. Las fotos de postura llenan el ancho superior del mosaico a 180 px, con cover y esquinas superiores redondeadas; pictogramas contain.

### Trazabilidad de B y regresiones de A

| Requisito / encargo | Archivo y método | Prueba / evidencia |
|---|---|---|
| RF-014 · contexto preservado / fotos de postura | `PaginasEvaluacion.nueva`; `aplomo.css`, `.tour-postura`; `evaluacion.js`, `mostrar` | `HttpRecorridoTest` (22 casos), `HttpCuerpoTest.paginaUsaLosArchivosExactosDialogoYNingunAvisoInicial`; fotos cargadas a 180 px cover en seis anchos. |
| RF-015 · condiciones explícitas / sugerencia de piernas | `recorrido.js`, `refrescar` / `continuar`; contexto existente | Pies colgando sugiere sin apoyo, sin completar el segmento; confirmación sin cambiar la sugerencia observada en navegador. Ninguna y nota conservan pruebas de A. |
| RF-016 / RNF-006 · archivos y privacidad | `evaluacion.js`, envío; `Evaluaciones.guardar`; `Multipart.leer` | Suite de límites/tipo real/consentimiento existente; `ambosGuardaDosAnalisisPorFotoEnUnaTransaccionYConservaElMayor`, 1 y 10 fotos; Usuario denegado por dirección directa. |
| RF-017 / B2–B3 · mediciones, rangos, manual | `RecorridoCuerpo.RANGOS` / `parcial`; `DatosAnalisis.medicion`; `analisis.js`, `dibujar` / grupos / detección; `recorrido.js`, `prepararEleccion` | `RangosRecorridoTest.todosLosMosaicosProducenLosPuntosAprobados`: 18 casos independientes contra `PuntuadorSegmentos`, assets existentes. HTTP conserva 52° y rango 60°; navegador guarda 52° manual y antebrazo 85,86° medido. |
| RF-018 · puntos del segmento confirmado | `RecorridoCuerpo.parcial` / `puntuar`; `EvaluacionesWeb.atender`; `Analisis.autorizarParcial`; `recorrido.js`, `sondear` | `parcialNoMuestraUnFinalNiInventaAjustesYConservaElAnguloReal`, `observadoPuntuaSoloElComponenteConfirmado` (4), `parcialValidaPuntosYCasillasSinGuardarNiAceptarCodigoDeCliente`; Safari muestra Brazo: 4 puntos después de confirmar hombro. |
| RF-018 / RF-019 · final solo completo | `DatosAnalisis.leer`; `analisis.js`, `todoListo` / `respondido` / `medir`; `recorrido.js`, esfuerzo | `finalSoloApareceCuandoEstanTodosLosSegmentosActividadYCarga`: 9 casos retirando cada segmento, giro, piernas, actividad y carga. Respuesta 400 sin marcador final; completa 200 con marcador. Navegador Ambos: cero marcadores finales antes de esfuerzo y cuando solo un lado lo había completado. |
| RF-020 / RF-021 · Ambos y guardado del servidor | `Evaluaciones.guardar`; `Analisis.guardarVarios`; `DatosAnalisis.solicitudes`; `analisis.js`, `paso6` | 2/20 análisis y 10/100 mediciones para 1/10 fotos; mayor 7 y un pendiente. Duplicar foto+lado rechaza y revierte datos/archivos. Navegador: una foto, Derecho 3 · Amarillo · Nivel 2 e Izquierdo 7 · Rojo · Nivel 4, mismos resultados guardados. |
| RNF-002 / B1, B4–B7 · recorrido, teclado y ayuda | `RecorridoCuerpo.pasos`; `recorrido.js`, `abrirHoja` / `cerrarHoja` / foco / `continuar`; `evaluacion.js`, `actualizarGuia` | Hoja sin ajustes iniciales; Ninguno excluyente; Tab y Shift+Tab envueltos, Escape conserva la pregunta sin confirmar; sugerencia de piernas confirmada explícitamente. Safari nativo y capturas de llegada/intento. |
| RNF-003 / RNF-007 · espera y errores en español | `recorrido.js`, `sondear` / Listo; `evaluacion.js`, manejo del intento | Listo muestra Cargando… durante respuesta; errores conservan datos; llegada sin aviso y aviso solo al intento. Rendimiento completo nuevamente en verde. |
| RNF-004 / RNF-006 · permisos | `EvaluacionesWeb.atender`; `Analisis.autorizarParcial`; controles existentes | `usuarioNoObtieneMedicionesParcialesNiFotosPorDireccionManual`, CSRF incorrecto 403; captura de Usuario abriendo dirección nueva y recibiendo Acceso denegado. Las pruebas de acceso a fotos existentes siguen en verde. |

### Pruebas y correcciones comprobadas

`TZ=America/Bogota ./mvnw -Daplomo.evidencia.rendimiento=docs/capturas_13e/fase_b/rendimiento.json test`: **783 aprobadas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. Compilación incluida; resumen exacto en `fase_b/pruebas.txt`. Son 745 anteriores + 18 rangos + 20 casos HTTP. Solo cambió una expectativa de A que suponía ausente una imagen ahora incorporada por Claude. `node --check` y `git diff --check` aprobados.

El primer ensayo visual encontró que tocar el mosaico ya sugerido no abría la hoja, que Continuar podía quedar bloqueado tras confirmar piernas y que una corrección escrita podía ser reemplazada por el representativo. Se corrigieron los manejadores de click/cambio, actualización de ocupación y conservación del manual; la versión final se recorrió y guardó nuevamente. Se corrigieron también las etiquetas de Foto 1 en ambos lados y se mantuvo el final Pendiente durante esfuerzo. No se ocultaron fallos ni se aflojaron expectativas del motor.

Ensayo final integrado: Derecho con brazo 52° manual, hombro levantado y antebrazo 85,86° por puntos; Izquierdo por observación directa con 110/40/25/30/75° manuales, muñeca desviada y giro extremo; actividad y carga distintas y explícitas por lado. El resultado fue 3 y 7 respectivamente. La evaluación guardada muestra **dos análisis de la misma foto** y conserva el máximo. Safari instalado confirmó detección, sugerencia de 85,92°, hoja, Escape y brazo 60° con hombro levantado = 4 puntos; no se atribuye a Safari el guardado completo de Ambos, hecho en el navegador integrado.

### Evidencia visual y límites

`docs/capturas_13e/fase_b/`: postura, brazo sugerido, corrección, hoja, giro sin responder y tras intento, piernas sugeridas, esfuerzo pendiente, resultado Ambos, guardado, permisos y Safari. Se inspeccionaron visualmente las capturas de escritorio y celular, y la pantalla nativa de Safari.

`mediciones-finales.json` contiene **27 registros sin desbordamiento** (scrollWidth ≤ clientWidth). Postura y brazo se midieron a innerWidth 390/810/1024/1080/1366/1920; imágenes de postura 180 px cover. Altura mínima observada de mosaico 209,8 px, por encima de 160; botones 48 px y encabezado 44, con diferencias subpíxel. Las capturas se nombran por el tamaño solicitado. **Algunas capturas de hoja, esfuerzo y resultado tuvieron un ancho efectivo menor al solicitado** (el JSON conserva innerWidth real, por ejemplo 354 para la hoja nominal 390); se verificó que tampoco desbordan, pero no se presentan como mediciones exactas de esos tamaños. La cobertura exacta de 390/810/1080/1366 de esta fase corresponde a postura y brazo.

`consola-final.json`: cero errores JS y tres avisos de MediaPipe/WebGL, no una consola vacía. No se descargaron Chrome, Edge ni Firefox. No se probó dedo físico ni se cambió la preferencia real de movimiento reducido; su tratamiento está implementado en CSS/JS. No se certifica WCAG completa ni compatibilidad con dos versiones estables de cada navegador.

Los dos servidores QA de B usaron exclusivamente `rula_ergonomia_pruebas`, cuentas ficticias, fotos temporales propias y transacciones revertidas: **QA13E: ROLLBACK completado** en ambos cierres. No se guardaron sus contraseñas en Safari. Se cerraron las ventanas/pestañas creadas y se retiró el tamaño forzado del navegador. No se cargaron datos de QA ni se borró contenido de la base real.

### Checklist y lo que NO se hizo

- ✅ 783 pruebas en verde y compilación correcta; evidencia de rendimiento actualizada.
- ✅ Diseño del recorrido B aprobado por texto: siete segmentos, 18 mosaicos, hoja inferior, imágenes exactas y ajustes explícitos; capturas conservadas. Esfuerzo y resultado siguen siendo el puente existente, no se afirma que C esté implementada.
- ✅ 390 / 810 / 1080 / 1366 sin desplazamiento horizontal en postura y brazo; tamaños adicionales y otros pasos registrados con sus anchos reales.
- ✅ Opciones ≥160 px, botones 48/44, radios/checkboxes nativos, foco atrapado y Escape comprobados.
- ❌ Dedo en iPad/celular físico: pendiente.
- ✅ Usuario denegado escribiendo la dirección; validaciones de sesión/CSRF y fotos conservadas.
- ✅ Tildes y mensajes en español revisados; sin advertencias de respuesta al llegar.
- ✅ Cero errores en consola integrada; tres avisos del runtime de detección documentados.
- ✅ `config/db.properties` y `fotos/` fuera de Git; sin credenciales en evidencia, sin cambios SQL ni motor.
- ❌ Preferencia física de movimiento reducido y QA nuevo Chrome/Edge/Firefox: no realizados; sin descargas.
- ❌ Fase C: esfuerzo en dos pantallas de mosaicos, resultado reorganizado, siguiente puesto, recordar contexto/esfuerzo y adaptar Añadir análisis. Esperan el OK de Andy. Las imágenes ya están, pero esos flujos aún no se implementaron.

### Cómo lo prueba Andy

1. Entra como Administrador, abre una visita En curso y elige un puesto activo. Completa consentimiento, postura, momento, tiempo y condiciones; comprueba que ninguna pantalla recién abierta muestra aviso ámbar. Intenta continuar sin contestar: debe aparecer ayuda sin avanzar.
2. Agrega una foto lateral JPG/PNG y elige Para medir; las otras pueden ser De apoyo. Continúa y elige Derecho, Izquierdo o Ambos, sin valor inicial.
3. En Brazo revisa los tres puntos resaltados y el mosaico sugerido con ángulo real. Tócalo, confirma un ajuste o Ninguno y pulsa Listo. Escape debe cerrar conservando la pregunta sin confirmar los cambios de la hoja.
4. Prueba Corregir medición con 52°: el ángulo debe conservarse. Elige otro mosaico o No se ve en la foto: debe usar su representativo como corrección manual. Usar los puntos de la foto restaura la medición.
5. Completa los siete segmentos. Giro no tiene respuesta inicial; Pies colgando solo sugiere el apoyo de piernas y exige confirmación. El chip muestra puntos del segmento confirmado; el final dice Pendiente.
6. En Ambos repite para el otro lado, completa actividad y carga de cada foto/lado en el puente actual y revisa el resultado. Mientras falte una respuesta no debe aparecer un puntaje final; al guardar deben existir dos análisis independientes por la misma foto.
7. Como Usuario intenta escribir la dirección de Nueva evaluación: Acceso denegado. No debe ver fotos. En celular/tableta revisa mosaicos, hoja, foco y Atrás sin deslizar de lado.
8. Ejecuta `TZ=America/Bogota ./mvnw test` con la base de pruebas preparada: 783 aprobadas.

### Git y parada

Commit de esta entrega: `Paso 13E-B: recorrido corporal guiado y ajustes`; el resumen final comunica el hash real y confirma el resultado del push. Padres incorporados: `9a2a5f2` (A), `f8b45c6` y `3fdc88a` (imágenes de Claude). Se verifica origin exacto `https://github.com/Andybena2025/Aplomo-app.git` y rama main antes del push normal, sin forzar. README actualiza únicamente fila 13 y cantidad de pruebas; INSTALAR solo cantidad. **B queda para revisión de Andy; se detiene antes de C.**


## Fase C · Esfuerzo, resultado y cierre · 13 nov 2026

Andy aprobó B y autorizó C en este chat, incluyendo el bloque de ayuda de lado. Se verificaron origin exacto `https://github.com/Andybena2025/Aplomo-app.git` y rama main, con árbol limpio; `git pull --rebase origin main` indicó actualizado en `ac94c46`. No se releían ni imprimían todos los documentos del proyecto: se usó el estado y el encargo ya leído de 13E.

### Qué se hizo

Actividad y carga ahora son **dos preguntas por foto y lado**, en mosaicos con los pictogramas existentes. Actividad: esfuerzo_estatica / esfuerzo_repetitiva / esfuerzo_ninguno; las dos primeras se traducen a actividad muscular Sí y Ninguna a No. Carga usa las seis opciones exactas del enum, con carga_1/2/3/3/4/4 y la referencia de resma y galón. No hay respuesta inicial en un recorrido nuevo. Elegir avanza tras 250 ms; con movimiento reducido se conserva Continuar. Atrás vuelve sin perder respuestas entre ambas preguntas, análisis y cuerpo.

El resultado presenta primero puntaje grande, etiqueta «puntaje · color · Nivel», acción y clasificación oficiales existentes. Después viene **Ver desglose del cálculo**, cerrado, y **Sugerencias · aparte del puntaje**, separado. No se cambió el semáforo, la acción guardada en MySQL ni cómo se calculan sugerencias. Los resultados ya guardados también presentan el desglose plegado. El cálculo completo sigue exclusivamente en el servidor, con NEUTRO=5° y tablas de MySQL; nunca aparece un final mientras otro segmento o esfuerzo esté pendiente.

**Guardar y evaluar siguiente puesto** guarda y pregunta al servidor por el primer puesto activo sin evaluación de esa visita, en el orden existente de número/id. Una evaluación sin fotos cuenta como evaluada, como en la lista de visita. Si no quedan pendientes vuelve a la visita con «Todos los puestos están evaluados». **Guardar y volver a la visita** guarda y vuelve directamente. Ambos destinos sirven para nuevas evaluaciones con/sin fotos y para Añadir análisis. Destinos ajenos se rechazan antes de guardar; clientes anteriores sin el campo conservan la respuesta anterior. Si se guardó correctamente pero falla la lectura posterior del siguiente puesto, se vuelve a la visita con la confirmación de guardado, evitando sugerir un reenvío que duplicaría registros.

En el siguiente puesto de la misma visita, **postura, descripción editada, momento, tiempo y esfuerzo** se recuerdan con aviso visible para revisarlos y continuar. El almacenamiento es solo de esta pestaña/sesión, por cuenta y visita, mediante sessionStorage dentro de try/catch. Para esfuerzo se empareja el índice de foto y lado. Si no hay coincidencia, o el navegador impide guardar, se contesta de nuevo. Se conserva una lista cerrada de campos: **no se copian consentimiento, condiciones ni la elección del paso Lado, y no se almacenan fotos, puntos, ángulos, ajustes ni puntajes**. Cada recuerdo de esfuerzo lleva únicamente su índice de foto y lado como claves para emparejarlo. Las selecciones recordadas de esfuerzo no se convierten en respuestas confirmadas hasta pulsar Continuar; el final sigue Pendiente.

**Añadir análisis** utiliza las fotos existentes: se elige Para medir / De apoyo, lado, mismo recorrido corporal, dos preguntas de esfuerzo por análisis y mismo resultado. No sube otra foto ni modifica el contexto ya guardado; añade análisis y conserva el historial. Puede elegir Ambos y crear dos análisis nuevos de una misma foto. No usa recuerdos del puesto anterior.

La pregunta **¿Qué lado tiene más carga?** se mantiene intacta. Debajo aparece exactamente el texto pedido, sus cuatro preguntas y «Si no está claro, elige «Ambos».», con 13 px y var(--muted). A 390, la primera fila de mosaicos queda visible después de la ayuda; todos mantienen al menos 160 px. No se añadió inferencia de lado ni regla RULA a ese bloque.

Se corrigió una carrera observada durante QA: una sugerencia de foto recibida después del toque podía reemplazar la elección del mosaico antes de abrir la hoja. Ahora la elección explícita se conserva; restaurar puntos o modificar puntos vuelve a permitir sugerencias. Se comprobó de nuevo el brazo 110° = 4 puntos en el siguiente puesto y en Safari. Los controles anteriores permanecen ocultos solo como adaptadores del lienzo existente; no hay una pantalla de esfuerzo duplicada.

### Trazabilidad

| Requisito / encargo | Archivo y método | Prueba o evidencia |
|---|---|---|
| RF-014 · contexto y navegación entre puestos | `Evaluaciones.siguientePendiente`; `EvaluacionesWeb.destino` / `validarDestino`; `evaluacion.js`, envío | `HttpCierreTest.siguienteEsActivoYPendienteDeEstaVisitaInclusoSinPuntaje`, `guardarAnalisisYSiguienteAbreElPuestoPendienteEnLaMismaVisita`, `ultimoPuestoVuelveConTodosEvaluados`; navegador pasó de Puesto 1 a 2 y luego a Todos evaluados. |
| RF-014 · recuerdos confirmables | `memoria-evaluacion.js`, `restaurar` / `aviso` / `guardar`; `esfuerzo.js`, `mostrar` / `confirmar` | UI del siguiente puesto: postura/momento/90 min elegidos con aviso; esfuerzo repetitivo y carga anterior visibles para confirmar. Antes de medir: cinco manuales vacíos, cero ajustes y ningún segmento confirmado. Fotos/postura/esfuerzo recordados en capturas. |
| RF-015 · condiciones preservadas | Contexto existente; `recorrido.js`, `refrescar`; campos ocultos de `PaginasAnalisis.continuar` | `HttpRecorridoTest` conserva 22 casos; condiciones no se recuerdan entre puestos; Añadir análisis usa las condiciones guardadas sin modificarlas, incluidas ayudas de piernas. |
| RF-016 / RNF-006 · fotos y sin fotos | `PaginasAnalisis.continuar`; `evaluacion.js`, `validarFotos` / envío; `Evaluaciones.guardar` existente | Nueva selección explícita en fotos existentes; `nuevaEvaluacionSinFotosGuardaYRespetaElDestino` (2 casos); suite previa de tipo real, tamaño, máximo 10, consentimiento y acceso a fotos conservada. |
| RF-017 · recorrido compartido | `RecorridoCuerpo.pasos`; `PaginasAnalisis.continuar`; `recorrido.js`, `preparar`; `analisis.js`, lienzo/detección | `anadirAnalisisComparteCuerpoEsfuerzoYResultadoSinValoresIniciales`; recorrido Ambos completo y guardado; Safari con detección y ajuste de rango conservado. Los 18 casos de `RangosRecorridoTest` siguen en verde. |
| RF-018 · sin final incompleto / motor existente | `analisis.js`, `respondido` / `todoListo` / `elegirEsfuerzo`; endpoint existente | 9 casos de final incompleto de B; navegador mostró cero marcadores finales al completar solo Derecho mientras Izquierdo aún no tenía esfuerzo. Diez casos de puntaje en HTML y pruebas de motor/MySQL sin cambios. |
| RF-019 · actividad y seis cargas | `RecorridoCuerpo.esfuerzo` / `CARGAS`; `esfuerzo.js`, `confirmar`; `analisis.js`, `elegirEsfuerzo` | `lasSeisCargasTienenSuMosaicoExactoYSuPuntajeActual` (6); en UI se probaron las tres actividades: repetitiva en nueva, Ninguna y estática en Ambos. Esfuerzo y carga sin elección inicial observados en integrado y Safari. |
| RF-020 · resultado ordenado / sugerencias separadas | `PaginasAnalisis.resultado`; `EvaluacionesWeb.atender` | Seis casos verifican desglose cerrado y nuevo título; pruebas previas de sugerencias siguen comprobando regla aplicable y ausencia de inserciones en vista previa. Capturas resultado plegado/desplegado. |
| RF-021 · historial y Ambos | Servicios existentes; `DatosAnalisis.solicitudes`; `evaluacion.js`, envío | Integrado: una foto con análisis anterior Rojo 7, más Derecho Verde 1 e Izquierdo Rojo 7; los tres presentes al consultar. Safari añadió después un Amarillo 3 y volvió a la visita. B mantiene sus casos de 1/10 fotos y 2/20 análisis atómicos. |
| RNF-002 / RNF-007 · ayuda de lado, pasos y mensajes | `RecorridoCuerpo.pasos`; `.ayuda-lado` en CSS; `evaluacion.js`, `mostrar` / progreso | `ayudaDeLadoApareceEnLaPantallaDeLadoSinCambiarLaPregunta` comprueba título, bloque y los seis textos exactos, sin checked inicial. Capturas y medidas 1366/390/810/1080. Avisos solo después del intento incompleto. |
| RNF-004 / RNF-006 · permisos, CSRF y destino | Router/servicios existentes; `EvaluacionesWeb.validarDestino` | `usuarioNoPuedeAbrirNiGuardarElNuevoRecorrido`; `destinoManipuladoSeRechazaAntesDeGuardar`; Usuario por dirección directa recibe Acceso denegado en QA visual. Sin nuevas rutas ni ampliación de permisos. |
| RNF-003 / RNF-008 · carga y adaptación | `evaluacion.js`, envío; CSS; código de avance reducido existente | Botones bloqueados durante guardado; 23 medidas exactas sin desbordamiento; guardado en Safari instalado; suite de rendimiento incluida. No se presenta como certificación completa entre navegadores/dispositivos. |

### Pruebas y evidencia

`TZ=America/Bogota ./mvnw -Daplomo.evidencia.rendimiento=docs/capturas_13e/fase_c/rendimiento.json test`: **799 aprobadas; 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, compilación incluida. Resumen real en `fase_c/pruebas.txt`. Son 783 anteriores + **16 casos nuevos** de `HttpCierreTest`. También se comprobaron los cinco JavaScript con `node --check` y el diff con `git diff --check`.

La primera ejecución dirigida encontró que el fixture intentaba crear un puesto inactivo pasando false, pero el servicio crea nuevos puestos activos por diseño; se corrigió el fixture usando la operación real de desactivar. La primera suite completa falló en una expectativa del texto anterior «Sugerencias del sistema»; se actualizó solo ese título al pedido por C, conservando la comprobación de la regla, ausencia de escrituras y permisos. Se repitió la suite completa. No se rebajaron pruebas ni se cambió el motor para ajustarlo a los resultados.

**23 medidas con anchos reales iguales a los solicitados**, sin scroll horizontal: lado 1366/390/810/1080; esfuerzo, carga y resultado 1366/1024/390/810/1080; fotos de Añadir y resultado Ambos 1366/390. Mosaico mínimo observado 160 px. Evidencia en `docs/capturas_13e/fase_c/mediciones.json` y PNG de esas pantallas, recuerdos, Todos evaluados, historial, permisos y Safari. Se inspeccionaron visualmente las capturas de ayuda y resultado móvil; en escritorio y Safari se observaron las pantallas reales. Las capturas A/B ya hechas siguen en sus carpetas, sin repetir QA ni presentarlas como nuevas de C.

**Safari instalado:** selección de foto existente, ayuda de lado, recorrido corporal, detección y mosaicos de esfuerzo/carga sin respuesta inicial; resultado 3 · Amarillo · Nivel 2, desglose cerrado y guardado con vuelta a la visita. No se guardó la contraseña ficticia. Los anchos exactos son del navegador integrado, no se atribuyen a Safari físico de esas medidas. `consola.json` registra **9 avisos MediaPipe/WebGL y 0 errores JS** en integrado. No se afirma consola vacía ni se ocultan esos avisos.

La instancia QA usó exclusivamente `rula_ergonomia_pruebas`, cuentas ficticias, fotos temporales propias y transacción revertida al cerrar: **QA13E: ROLLBACK completado**. Se cerraron la ventana privada y pestaña propias y se retiró el viewport forzado. No se cargaron pruebas ni se borraron datos de `rula_ergonomia`, ni se modificó su esquema. No se descargaron navegadores.

### Checklist y lo que NO se hizo

- ✅ 799 pruebas, compilación y comprobaciones de sintaxis/diff en verde.
- ✅ Fase C según diseño textual aprobado: dos preguntas de esfuerzo, pictogramas exactos, resultado ordenado, guardar/siguiente, recuerdos y Añadir análisis; capturas conservadas.
- ✅ 390 / 810 / 1080 / 1366 y 1024 sin desbordamiento en los pasos registrados; opciones ≥160 px.
- ✅ Confirmaciones explícitas; recuerdos visibles; cero ángulos/ajustes heredados; final Pendiente mientras falte una respuesta.
- ✅ Usuario denegado por dirección directa y por POST; CSRF y fotos protegidas se mantienen.
- ✅ Mensajes con tildes, ayuda exacta y pregunta de lado intacta.
- ✅ Cero errores JS registrados; nueve avisos de MediaPipe/WebGL documentados.
- ✅ Sin cambios de motor, esquema, dependencias, rutas ni credenciales/fotos privadas en Git.
- ❌ Dedo físico en celular/iPad: pendiente. No se certifica WCAG completa ni dos versiones estables de Chrome/Edge/Firefox; no se descargaron copias.
- ❌ Preferencia física de movimiento reducido / almacenamiento bloqueado: no se cambió la configuración real para ensayarlas; ambas salidas están implementadas sin bloquear el flujo.
- ❌ Diagramas y especificación de casos de uso: a cargo de Claude según C5; se actualizaron aquí requisitos y flujo de pantalla, README/INSTALAR y estado.

No quedó funcionalidad de C a medias en el alcance implementado; los pendientes anteriores corresponden a validación física/compatibilidad y documentación asignada a Claude. El Paso 13 general conserva esos límites de QA, sin declararlo certificado por completo.

### Cómo lo prueba Andy

1. Entra como Administrador, visita En curso y puesto pendiente. Completa el contexto y agrega una foto Para medir. En lado comprueba la pregunta original y las cuatro referencias; elige un lado o Ambos.
2. Confirma los siete segmentos y sus ajustes/Ninguno. Si cambias el rango, debe conservarse aunque la detección termine después. El chip parcial muestra el segmento; el final permanece Pendiente.
3. Responde actividad con sus tres mosaicos y carga con sus seis. Vuelve con Atrás: no debe perder lo escrito. En Ambos, actividad/carga se responden por lado; no debe aparecer un puntaje mientras falte el otro.
4. Resultado: revisa puntaje, color, nivel y acción; abre Ver desglose del cálculo y vuelve a cerrarlo. Sugerencias aparece aparte.
5. Pulsa Guardar y evaluar siguiente puesto: debe guardar y abrir un pendiente activo de la misma visita. Postura, momento, tiempo y esfuerzo aparecen recordados con aviso para confirmar. Consentimiento, condiciones, lado y mediciones vuelven a contestarse; nunca se copian ángulos ni ajustes.
6. Guarda el último pendiente con el mismo botón: vuelve a la visita con Todos los puestos están evaluados. El segundo botón siempre guarda y vuelve directamente. Sin fotos, se conserva el guardado sin puntaje.
7. Abre una evaluación con fotos → Analizar fotos / añadir análisis. Elige fotos Para medir/De apoyo y repite lado, cuerpo, esfuerzo y resultado. Guarda y consulta la evaluación: los análisis anteriores deben seguir presentes junto a los nuevos.
8. Como Usuario, escribe la dirección de Añadir análisis: Acceso denegado. Repite suite con `TZ=America/Bogota ./mvnw test`: 799 aprobadas. Prueba también a 390 y con el dedo en tu dispositivo físico, que queda pendiente de esa comprobación real.

### Git y cierre

Commit de C: `Paso 13E-C: esfuerzo guiado, resultado y siguiente puesto`. El resumen final comunica el hash real y el resultado del push normal a origin/main, verificado contra `Andybena2025/Aplomo-app`. Base `ac94c46` (B aprobado por Andy); no se reescriben commits anteriores. README cambia solo fila 13 y cantidad de pruebas; INSTALAR solo cantidad; AGENTS registra la aprobación de B y el estado de C. **Se detiene para revisión de Andy, sin adelantar otro paso.**
