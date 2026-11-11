# Fase 4: empresas, reservas y visitas

Se implementan los pasos 4, 5 y 6, en ese orden y con un commit por paso. Se conserva la base existente y su esquema aprobado.

## Paso 4: empresas y estructura

Requisitos RF-004 a RF-010, RF-028, RNF-001, RNF-004 y RNF-009. Se añadieron registros de Empresa, Sede, Salon, Area, Cargo y Puesto, un DAO por tabla, servicios transaccionales y páginas con el estilo Obsidian & Ivory del mock.

- Administrador: crear, consultar, editar, desactivar y reactivar empresas, sedes, salones, áreas, cargos y puestos. Los identificadores, relaciones padre y creador se conservan al editar. El puntaje del puesto se conserva y no se inventa un resultado nuevo.
- Usuario: consulta su empresa y su estructura, con filtro de empresa en SQL y sin botones ni rutas de modificación autorizadas. El permiso se vuelve a comprobar en el servicio.
- NIT duplicado: no guarda y avisa cuál empresa existe. Nombre de empresa similar sin mayúsculas, tildes, espacios y sufijos societarios: avisa, pero admite un NIT diferente. Sedes, salones, áreas y cargos no repiten nombre dentro de su padre; puestos no repiten número en el salón. Los registros inactivos también conservan su identidad.
- Desactivar no borra ni cambia en cascada el estado de los hijos. La jerarquía inactiva no permite registros nuevos; la consulta del historial sigue disponible. Los catálogos inactivos no se ofrecen para asignaciones nuevas. Una asignación anterior se conserva al editar.
- Los errores de validación y de guardar conservan los campos del formulario y no exponen detalles SQL. Todos los cambios usan POST y el token de protección de la sesión.

Comprobación del paso 4: `mvn compile test`, **457 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**. Incluye las 440 anteriores y 17 nuevas de servicios y HTTP. Las cuentas y empresas de prueba permanecen dentro de transacciones revertidas, sin borrar datos ni confirmar fixtures.

Para probar:

1. Arrancar la versión actual con `mvn -Daplomo.puerto=8081 compile exec:java` y abrir `http://localhost:8081`.
2. Ingresar como Administrador y abrir Empresas → Nueva empresa. Escribir nombre y NIT y guardar.
3. Repetir el NIT: el sistema impide guardar y conserva el formulario. Usar otro NIT con nombre similar: guarda y muestra el aviso.
4. Abrir la empresa y registrar una sede, después un salón, áreas, cargos y un puesto. En el puesto, las áreas y cargos son opcionales y pertenecen a esa empresa.
5. Editar datos y comprobar que siguen en su contexto. Desactivar y reactivar: el registro continúa visible como parte del historial.
6. Crear una cuenta Usuario asignada a esa empresa. Al ingresar con contraseña definitiva solo se consulta su empresa; escribir una dirección de otra empresa o de edición devuelve acceso denegado.

Se verificaron visualmente en el navegador integrado la navegación Empresas, el listado vacío y el formulario Nueva empresa. Se ajustó el ancho del enlace Volver al estilo del mock, sin crear empresas reales durante la revisión.

## Paso 5: reservas

RF-011, RF-009b, RF-012 y RNF-005. Las reservas se guardan en MySQL con un código `R-` y el identificador automático con al menos cuatro cifras. Un código provisional existe solo dentro de la transacción y se reemplaza antes de confirmar. Los huecos por ROLLBACK son normales y no se reinician contadores.

- Fecha de hoy o posterior según Colombia; hora de fin posterior a inicio; categorías Evaluación inicial, Seguimiento y Reevaluación. La empresa, sede y salón deben corresponder entre sí y estar activos al registrar.
- Solo fecha, horas, categoría, descripción y notas se editan. Código, relaciones, creador y estado se conservan aunque alguien envíe otros valores en la petición.
- Esperando → En cola mediante Aceptar. Esperando o En cola → Cancelada con motivo obligatorio. Los estados finales no se reabren. La cancelación se bloquea después de iniciar la visita, siguiendo el enfoque recomendado a Andy para conservar coherentes la reserva y la visita.
- Creación, edición y cambios de estado registran fecha, cuenta responsable y estado en la misma transacción. Si falla la auditoría, se revierte también la operación.
- El Usuario solo consulta las reservas de su empresa, filtradas en SQL; no tiene acceso a formularios ni cambios de estado.
- Nueva reserva permite registrar empresa, sede y salón en formularios desplegables de la misma pantalla. Las listas se actualizan y seleccionan el lugar nuevo sin recargar ni guardar el borrador en el navegador. Los errores conservan los campos.

Comprobación automatizada: **477 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**. Se añadieron 20 casos que incluyen límites de fecha y horas, duplicados, contexto de empresa, permisos, estados, notas largas y reversión de una auditoría fallida.

Comprobación visual: en una instancia temporal con transacción revertida se escribieron primero fecha, horario, descripción y notas; después se crearon empresa, sede y salón desde la misma reserva. Todos los valores permanecieron y los tres lugares nuevos quedaron seleccionados. Se guardó la reserva y se aceptó, comprobando el cambio Esperando → En cola. Al cerrar la vista se ejecutó ROLLBACK, sin conservar esos datos de prueba.

Para probar este paso:

1. Abrir Reservas → Nueva reserva. Seleccionar empresa, sede y salón activos y escribir fecha y horas.
2. Si falta alguno, usar Registrar un lugar en la misma pantalla; al guardar se selecciona y el borrador se conserva.
3. Guardar y comprobar código, estado Esperando y datos. Aceptar para pasar a En cola.
4. Editar fecha o notas y comprobar que conserva código y lugar. Cancelar pide un motivo y después impide editar o cambiar de estado.

## Paso 6: visitas

RF-013a, RF-013b, RF-002, RNF-005 y RNF-009. La visita se inicia desde una reserva En cola, guarda la hora real de Colombia y registra al responsable en bitácora. Una reserva solo tiene una visita. Iniciar y finalizar usan POST con token de sesión y vuelven a comprobar los permisos en el servicio.

- El detalle muestra empresa, sede, salón, inicio, fin, notas y las evaluaciones guardadas con su número de puesto, postura, fecha y puntaje. El Usuario solo puede consultar visitas finalizadas de su empresa; el filtro se aplica en SQL. No se entregan fotos ni rutas de archivos.
- No se finaliza sin al menos una evaluación guardada. Se conservan las notas ante un error. Al finalizar, visita y reserva pasan a Finalizada, se guarda la hora real de fin y se registran ambos cambios en bitácora, todo en una sola transacción.
- La calificación usa el máximo de todos los análisis de la última evaluación de cada puesto dentro de esta visita, sin perder el lado derecho o izquierdo. Después promedia los puestos con puntaje disponible y guarda un decimal. Los análisis y evaluaciones anteriores se conservan.
- Una evaluación Sin registro fotográfico puede existir sin análisis RULA. No se inventa un cero: ese puesto no entra al promedio; si ninguno tiene puntaje, la calificación queda vacía y la pantalla dice «Sin calificación disponible».
- La cancelación queda bloqueada desde el inicio de la visita. Las visitas finalizadas no se reabren ni se sobrescriben. Si falla cualquier escritura, incluida la segunda entrada de bitácora, se revierten todos los cambios de la operación.

Comprobación del paso 6: `mvn compile test`, **493 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas**. Se añadieron 16 casos a las 477 anteriores: inicio único, estados de origen, cierre vacío, promedio de puestos con múltiples evaluaciones y fotos, ambos lados, ausencia de puntajes, notas inválidas, hora de fin, privacidad por empresa, tokens, campos manipulados y reversión de fallos parciales.

Comprobación visual en el navegador integrado: inicio desde una reserva En cola, visita vacía sin botón de finalizar, lista de evaluaciones, formulario de notas, cierre con puntajes 4 y 7 que dio **5.5**, notas con párrafos conservados y reserva Finalizada. Se usaron metadatos ficticios y una transacción temporal revertida al cerrar; no se crearon fotos ni datos reales de demostración.

Para probar este paso:

1. Crear una reserva y aceptarla. Abrir su detalle y pulsar Iniciar visita: aparece En curso con hora real.
2. Volver a la reserva: muestra Ver visita y ya no ofrece Cancelar. Repetir el inicio no crea otra visita.
3. Abrir la visita vacía: explica que requiere una evaluación y no ofrece finalizarla. El servidor también rechaza un envío directo sin evaluaciones.
4. La pantalla para crear evaluaciones corresponde a la **fase 5**. En esta fase el cierre con evaluaciones ya guardadas se comprueba con las pruebas reversibles de MySQL y la vista temporal descrita arriba; no se debe insertar información manualmente en la base real.
5. Cuando existan evaluaciones, pulsar Finalizar visita, escribir notas y confirmar. Verificar fin real, promedio, notas y estado Finalizada tanto en visita como en reserva.
6. Ingresar como Usuario de esa empresa: solo aparecen sus visitas finalizadas y no hay controles de modificación. Una dirección de otra empresa no permite consultar sus datos.

## Estado de la fase

**Fase 4 completa: pasos 4, 5 y 6.** Cada paso tiene compilación, suite completa y su propio commit. Se conserva el esquema, las cuentas, los datos y los documentos existentes; no se ejecutó el script inicial ni se agregaron dependencias. Los datos de las pruebas se revirtieron mediante ROLLBACK.

Pendiente para las fases siguientes: crear evaluaciones y fotos en fase 5; búsqueda y filtros completos, indicadores e informe en fase 6; recorrido de compatibilidad en Chrome, Edge y Firefox en fase 7. La revisión visual de esta fase se hizo en el navegador integrado. No se inicia la fase 5 hasta el OK de Andy.
