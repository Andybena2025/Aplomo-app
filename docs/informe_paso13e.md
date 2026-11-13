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
