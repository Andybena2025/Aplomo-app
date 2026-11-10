# Visión y alcance (resumen de Notion "Proyecto Nuclear 6.0")

## El problema

Hoy el análisis de posturas se hace a mano: los ángulos se anotan en papel, las tablas RULA se consultan a mano, cada evaluador mide distinto y no hay forma centralizada de saber qué áreas o puestos concentran más riesgo. Las posturas forzadas mantenidas durante la jornada causan trauma acumulativo y enfermedades laborales; el tiempo de exposición importa.

## La solución

Una página web que acompaña las **4 fases** del proceso actual (BPMN): Diagnóstico → Planeación → Ejecución → Cierre. Digitaliza RULA, centraliza los resultados, muestra un semáforo de riesgo, califica sedes y empresa y propone sugerencias de mejora.

## Roles (casos de uso aprobados CU5-01 a CU5-29)

| Rol | Quién es | Qué hace |
|---|---|---|
| **Administrador** | El Ingeniero Industrial / Analista. Trabaja como **evaluador independiente**: es un tercero, no pertenece a la empresa cliente. | Gestiona las cuentas y registra empresas, sedes, salones, áreas, cargos, puestos, reservas, visitas, evaluaciones, pendientes, sugerencias e informes. |
| **Usuario** | La persona de contacto de la empresa cliente. | Inicia y cierra sesión, cambia su contraseña y **consulta, busca y filtra** la información de su propia empresa. No crea ni edita nada. No ve fotos. |

El trabajador evaluado **no usa** el sistema y no se guardan sus datos personales, solo el cargo.

## Términos

- **Empresa → Sede → Salón → Puesto de trabajo** (numerado dentro del salón). Áreas y cargos pertenecen a la empresa y son opcionales para el puesto.
- **Reserva:** fecha y horario acordados para una visita. Estados: Esperando → En cola → Finalizada, o Cancelada (con motivo).
- **Visita:** la ida real a la sede, con hora real de inicio y fin. Estados: En curso → Finalizada.
- **Evaluación:** estudio de un puesto dentro de una visita (postura elegida, criterio, motivo, minutos por jornada, condiciones y fotos).
- **Análisis:** cálculo RULA de **una** foto. Cada foto tiene su propio análisis; si se analiza de nuevo, se crea uno nuevo.
- **Pendiente de arreglo:** puesto que quedó en Rojo. Tiene prioridad y fecha límite hasta que el Administrador lo marca Arreglado.

## Flujo por fases

1. **Diagnóstico:** registrar la empresa (nombre y NIT), sedes, salones, áreas, cargos y puestos. Avisar duplicados. Editar sin cambiar el ID. Desactivar en vez de borrar.
2. **Planeación:** registrar la reserva (empresa, sede, salón, fecha, horas y categoría: Evaluación inicial, Seguimiento o Reevaluación). Aceptarla (Esperando → En cola) o cancelarla con motivo.
3. **Ejecución:** iniciar la visita desde una reserva En cola → crear evaluaciones (puesto, postura, condiciones) → casilla de aceptación → subir fotos → marcar puntos y medir ángulos → ajustes, actividad y carga → resultado RULA con semáforo → guardar → finalizar la visita (requiere al menos una evaluación; guarda la hora de fin, notas y calificación = promedio).
4. **Cierre:** Verde = Aceptable; Amarillo = En observación (se recomienda Seguimiento); Rojo = pendiente de arreglo. Sugerencias del sistema, editables por el Administrador. Calificación de sedes y empresa. Informe general en PDF. Desde que la visita se finaliza, el Usuario puede consultar sus resultados.

## Pendiente de validar con la clienta (implementa la propuesta, fácil de cambiar)

- Rangos del semáforo: 3 colores (Verde 1–2, Amarillo 3–4, Rojo 5–7) con el nivel oficial RULA al lado.
- Calificaciones: puesto = su puntaje más alto; visita, sede y empresa = promedio.
- Categorías de visita: Evaluación inicial, Seguimiento, Reevaluación.
- Límites: hasta 10 fotos JPG/PNG de 5 MB; 30 minutos de inactividad; 5 intentos de ingreso y 15 minutos de bloqueo; de 1 a 720 minutos por jornada.
- Si el Usuario ve los resultados apenas se finaliza la visita (propuesta) o cuando se publique el informe.
