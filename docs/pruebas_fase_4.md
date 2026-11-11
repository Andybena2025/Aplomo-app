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

## Estado de la fase

Paso 4 comprobado. Reservas y visitas se construyen a continuación dentro de esta misma fase. La fase 5 todavía no se inicia.

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
