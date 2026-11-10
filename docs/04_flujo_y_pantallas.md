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

Se abre desde una visita **En curso**.

1. **Puesto y postura** (RF-014): elegir el puesto del salón de la reserva; postura elegida; criterio (más inadecuada o mayor tiempo de exposición); motivo; minutos por jornada (1 a 720).
2. **Condiciones** (RF-015): 6 casillas Sí/No (escritorio muy alto, pantalla muy alta, pies colgando, apoyapiés, silla con apoyabrazos, silla regulable) y otras observaciones.
3. **Fotos** (RF-016): casilla "A la persona se le explicó el estudio y aceptó las fotos". De 1 a 10 fotos JPG/PNG de hasta 5 MB, cada una con vista (frente, perfil u otra) y descripción (máximo 200 caracteres). Si no acepta, la evaluación queda "Sin registro fotográfico".
4. **Ángulos** (RF-017): por cada foto, la foto ampliada en un `<canvas>`; el Administrador marca los puntos (cadera, hombro, codo, muñeca, nudillos y coronilla), los puede arrastrar con el mouse o con el dedo, o escribir el ángulo a mano. Se elige el lado (derecho o izquierdo). Copia del mock la lógica de marcar y arrastrar (`montarLienzo`, `dibujar`, `medirAngulos`), incluido el arreglo para pantallas táctiles.
5. **Ajustes y carga** (RF-018, RF-019): casillas de ajustes (hombro elevado, brazo separado, brazo apoyado, antebrazo cruza, muñeca desviada, giro de muñeca medio o extremo, cuello girado o inclinado, tronco girado o inclinado, piernas apoyadas), actividad (estática o repetitiva) y carga (las 6 opciones del enum).
6. **Resultado** (RF-020, RF-021): el puntaje final grande con el color y el nivel oficial; desglose del Grupo A y del Grupo B; sugerencias del sistema (RF-023a). El cálculo se muestra **antes de guardar**. Al guardar se crea un análisis por foto; si el resultado es Rojo, se crea el pendiente (RF-022).

Al terminar las evaluaciones, **Finalizar visita** (RF-013b): requiere al menos una evaluación guardada; pide notas; calcula la calificación (promedio) y pasa la reserva a Finalizada.

## Listas (RF-024)

Cada sección muestra una tabla que se puede **ordenar, buscar y filtrar** (por empresa, área, cargo, sede, salón, número de puesto, fecha, puntaje, nivel de riesgo y estado) e indica cuántos resultados hay. Con JavaScript puro alcanza: filtra en el navegador si son pocos datos, o con parámetros `?q=&color=&orden=` en el servidor.

## Mensajes (RNF-007)

Notificaciones en español: las confirmaciones se ven de 3 a 5 segundos; los avisos y errores se quedan hasta que la persona los cierra. Nunca mostrar trazas, códigos SQL ni nombres de tablas. Si falla subir una foto o guardar, no se pierde lo escrito.
