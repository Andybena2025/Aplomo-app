# Flujo y pantallas

Toma el **aspecto** de las capturas en `docs/referencia/mock_app/capturas/` y del mock (`docs/referencia/mock_app/app.src.html`), pero con los **2 roles** del modelo aprobado.

## Navegación por rol

| Pantalla | Administrador | Usuario | Requisitos |
|---|---|---|---|
| Iniciar sesión | ✓ | ✓ | RF-001a |
| Cambiar contraseña (obligatoria si es temporal) | ✓ | ✓ | RF-001c |
| Inicio (resumen: visitas en curso, reservas, puestos en Rojo) | ✓ todas las empresas | ✓ solo su empresa, solo lectura | RF-024 |
| Usuarios | ✓ crear, editar, activar y desactivar | ✗ | RF-003 |
| Empresas → detalle (sedes, salones, áreas, cargos, puestos) | ✓ crear, editar, desactivar | ✓ solo la suya, solo lectura | RF-004 a RF-010, RF-028 |
| Reservas | ✓ crear, editar, aceptar, cancelar con motivo | ✓ consultar | RF-011, RF-009b, RF-012 |
| Visita (iniciar, ver evaluaciones, finalizar) | ✓ | ✓ consultar si está Finalizada | RF-013a, RF-013b |
| Asistente de evaluación (6 pasos) | ✓ | ✗ | RF-014 a RF-021 |
| Pendientes de arreglo | ✓ marcar Arreglado | ✓ consultar | RF-022 |
| Sugerencias | ✓ editar, quitar, escribir, cambiar estado | ✓ consultar | RF-023a, RF-023b |
| Calificación de sedes y empresa | ✓ | ✓ su empresa | RF-025 |
| Informe general (imprimir / PDF) | ✓ generar | — | RF-026 |

El Usuario **nunca** ve fotos (RNF-006) ni botones de crear o editar. Las listas solo le muestran datos de su empresa y de visitas Finalizadas.

## Asistente de evaluación (RNF-002: paso a paso, barra de pasos, botón Atrás sin perder lo escrito)

Actualización del Paso 13E-A aprobada por Andy el 13 nov 2026. Se abre desde una visita **En curso**, tocando un puesto activo en su lista (Evaluado/Pendiente en esa visita). No se inicia una visita de un salón sin puestos activos; se valida en pantalla y servidor. Finalizar conserva su regla anterior.

Una pregunta de contexto visible por pantalla, progreso y Atrás sin perder respuestas. Sin imágenes disponibles en `static/img/tour/`, el mosaico conserva su etiqueta y borde. ESCRITORIO → Oficina; otro tipo → Planta, con orden y consejo contextual. Si falta puesto en la dirección, se pregunta primero sin preseleccionar uno.

1. **Consentimiento físico** obligatorio; se guarda en persona_acepto y no se confunde con permiso para fotos.
2. **Postura**: ocho mosaicos y descripción editable (150).
3. **Momento**: postura más exigente / la que más tiempo dura.
4. **Tiempo**: menos de 1 h / 1–2 / 2–4 / más de 4, guardando 30/90/180/300 minutos.
5. **Condiciones**: las seis originales y Ninguna excluyente; observación opcional de 255 caracteres. Motivo lo arma el servidor con momento y postura; la nota solo queda en observaciones.
6. **Fotos**: Con fotos / Sin fotos. Con fotos mantiene 1–10 JPG/PNG de 5 MB, vista y descripción; cada una Para medir / De apoyo. Sin fotos pasa a Guardar, sin puntaje.
7. **Lado (13E-B autorizado por Andy):** Derecho / Izquierdo / Ambos, sin elección inicial. Ambos recorre cada lado por separado y crea un análisis por foto y lado.
8. **Recorrido corporal:** Brazo → Antebrazo → Muñeca → Giro de muñeca → Cuello → Tronco → Piernas. Foto con los puntos del segmento destacados, mosaicos sugeridos por su ángulo real y confirmación explícita. Cambiar el rango o elegir por observación directa guarda su ángulo representativo como manual. Corregir medición conserva el valor escrito; Usar los puntos de la foto permite restaurarlo. Los ajustes se confirman en una hoja inferior con Ninguno excluyente. Escape cierra sin confirmar ni avanzar. Pies colgando sugiere sin apoyo, pero exige confirmación.
9. **Actividad y carga (puente provisional):** controles anteriores por foto y lado, sin respuestas iniciales. Puntaje parcial muestra solo el segmento confirmado con sus ajustes, calculado por el servidor; el final permanece Pendiente hasta completar todos los segmentos, actividad y carga de todos los análisis.
10. **Resultado (puente provisional):** cálculo completo del servidor, revisión y guardado; Ambos guarda dos análisis de una misma foto. Se mantienen pendientes y sugerencias existentes.

El borde ámbar y Responde para continuar aparecen únicamente tras intentar continuar sin respuesta. Al llegar no se muestra error. Las fotos de postura llenan el ancho superior del mosaico con 180 px de alto; los pictogramas conservan contain.

**Pendientes de C y de su OK:** esfuerzo en dos pantallas de mosaicos, resultado reorganizado, siguiente puesto, respuestas recordadas y recorrido corporal compartido al añadir análisis. La dirección de Añadir análisis mantiene todavía el editor anterior.

Al terminar las evaluaciones, **Finalizar visita** (RF-013b): requiere al menos una evaluación guardada; pide notas; calcula la calificación (promedio) y pasa la reserva a Finalizada.

## Listas (RF-024)

Cada sección muestra una tabla que se puede **ordenar, buscar y filtrar** (por empresa, área, cargo, sede, salón, número de puesto, fecha, puntaje, nivel de riesgo y estado) e indica cuántos resultados hay. Con JavaScript puro alcanza: filtra en el navegador si son pocos datos, o con parámetros `?q=&color=&orden=` en el servidor.

## Mensajes (RNF-007)

Notificaciones en español: las confirmaciones se ven de 3 a 5 segundos; los avisos y errores se quedan hasta que la persona los cierra. Nunca mostrar trazas, códigos SQL ni nombres de tablas. Si falla subir una foto o guardar, no se pierde lo escrito.
