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
7. **Ángulos (puente provisional)**: lienzo actual, puntos/detección/arrastre o ángulos manuales y selector de lado actual.
8. **Ajustes y carga (puente provisional)**: preguntas existentes, sin valores preseleccionados; ajustes requiere alguno o Ninguno. Giro, piernas, actividad y carga requieren respuesta explícita.
9. **Resultado (puente provisional)**: cálculo del servidor, revisión y guardado como antes; un análisis por foto elegida para medir, pendientes rojos y sugerencias existentes.

Andy indicó conservar este puente para seguir calculando entre fases. **Pendientes de OK para B:** pantalla Derecho/Izquierdo/Ambos, recorrido corporal, mosaicos de rango y hoja inferior. **Pendientes de C:** esfuerzo por pantalla, resultado reorganizado, siguiente puesto, respuestas recordadas y recorrido corporal compartido al añadir análisis. No se presentan como funciones ya implementadas.

Al terminar las evaluaciones, **Finalizar visita** (RF-013b): requiere al menos una evaluación guardada; pide notas; calcula la calificación (promedio) y pasa la reserva a Finalizada.

## Listas (RF-024)

Cada sección muestra una tabla que se puede **ordenar, buscar y filtrar** (por empresa, área, cargo, sede, salón, número de puesto, fecha, puntaje, nivel de riesgo y estado) e indica cuántos resultados hay. Con JavaScript puro alcanza: filtra en el navegador si son pocos datos, o con parámetros `?q=&color=&orden=` en el servidor.

## Mensajes (RNF-007)

Notificaciones en español: las confirmaciones se ven de 3 a 5 segundos; los avisos y errores se quedan hasta que la persona los cierra. Nunca mostrar trazas, códigos SQL ni nombres de tablas. Si falla subir una foto o guardar, no se pierde lo escrito.
