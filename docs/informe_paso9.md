# Informe del Paso 9 · Pendientes de arreglo

Fecha: 11 nov 2026 · Zona: America/Bogota. Alcance: solo el Paso 9 de `docs/06_plan_de_implementacion.md`.

## Qué se hizo

Al guardar un análisis calculado como Rojo, se crea automáticamente un pendiente ligado a su evaluación, con el puntaje y la fecha. Se guarda en la misma transacción que el análisis: si falla el pendiente o su auditoría, se revierte la operación completa, incluida la foto recién recibida cuando corresponde.

La lista **Pendientes** permite mostrar Abiertos, Arreglados o Todos y ordenar por prioridad, fecha de registro o fecha límite. Cada detalle muestra puesto y ubicación, puntaje, descripción, prioridad, fechas y el historial con responsable. El Administrador puede completar o editar el seguimiento y marcar el pendiente como Arreglado. Repetir ese envío no duplica la auditoría. El Usuario solo consulta pendientes de su empresa cuyas visitas están finalizadas; no ve fotos ni acciones de cambio.

**Decisión explícita de Andy:** si hay más análisis rojos de la misma evaluación, se conserva su máximo. Se mantienen descripción, prioridad, fecha límite, fecha de registro y estado, incluso Arreglado. Otra evaluación roja crea otro pendiente. Un resultado posterior Verde no borra el historial. Marcar Arreglado no modifica el puntaje RULA ni cierra otros pendientes del puesto.

La descripción admite hasta 255 caracteres. Descripción, prioridad y fecha límite pueden completarse después, como permite el esquema aprobado; no se impone una fecha futura que los documentos no exigen. Los errores se muestran en español y el formulario conserva sus valores. Verde se presenta como **Aceptable** y Amarillo como **En observación**; ninguno crea pendientes.

## Requisitos y comprobación

El requisito propio del Paso 9 es RF-022. Se mantienen las siguientes reglas transversales, sin afirmar que este paso implementa de nuevo todo cada requisito.

| Requisito | Archivo y método | Prueba o evidencia |
|---|---|---|
| RF-022: creación automática solo por Rojo | `servicio/Analisis.java → guardarEnTransaccion`; `servicio/Pendientes.java → registrarRojo`; `dao/PendienteDao.java → crear` | `PendientesTest.soloLosResultadosRojosCreanPendiente`: los 10 casos RULA; `falloAlCrearPendienteRevierteEvaluacionAnalisisYFotoNuevos` |
| RF-022: máximo por evaluación y conservación | `Pendientes.registrarRojo`; `PendienteDao.porEvaluacion/puntaje` | `maximoConservaDatosFechaYArregladoYNoDuplica`; `otraEvaluacionGeneraOtroPendienteYUnVerdeNoBorraLosAnteriores`; `maximo-conserva-arreglado.jpg` |
| RF-022: descripción, prioridad y fecha límite | `Pendientes.guardar`; `PendienteDao.editar`; `web/PendientesWeb.java → atender`; `web/PaginasPendientes.java → editar` | `seguimientoOpcionalLimitesYAuditoriaSinCambiarRula`; `rechazaDatosInvalidosSinAlterarElSeguimiento`; `HttpPendientesTest.editarEscapaTextoYNoAceptaCambiarPuntajeNiEstado`; `seguimiento-guardado.jpg` |
| RF-022: marcar Arreglado y ordenar | `Pendientes.arreglar/listar`; `PendienteDao.arreglar/listar`; `PaginasPendientes.lista/detalle` | `ordenarPorPrioridadRegistroYLimiteEsDeterminista`; `arregladoSoloPorPostConCsrfYUnaSolaAuditoria`; `arreglado.jpg` |
| RF-022: Verde Aceptable, Amarillo En observación | `PaginasPendientes.clasificacion`; `web/PaginasAnalisis.java → resultado` | `HttpPendientesTest.estadosVerdeYAmarilloSeMuestranSinCrearPendientes`, más los 10 casos del servicio |
| RF-002 y consulta de RF-024 relacionada con RF-022: alcance y roles | `Pendientes.visible/admin/listar`; `PendienteDao.buscar/listar`; `PendientesWeb.atender`; `web/Router.java → atender` | `usuarioSoloConsultaSuEmpresaYVisitasFinalizadas`; `usuarioNoPuedeEditarNiArreglarPorDireccionDirecta`; capturas `usuario-*.jpg`. RF-024 completo corresponde al Paso 11 |
| RNF-004 y RNF-006: permisos, parámetros, escape y ausencia de fotos | `PendienteDao` usa `Sql` con parámetros y órdenes de lista blanca; `PaginasPendientes` usa `Html.escapar`; `Router.atender` comprueba CSRF | Pruebas HTTP de campos manipulados, HTML inyectado, CSRF inválido y Usuario; las páginas no contienen rutas de fotos; pruebas anteriores de fotos siguen verdes |
| RNF-005: fecha, hora y responsable | `Pendientes.registrarRojo/guardar/arreglar`; `dao/BitacoraDao.java → registrar`; `PendienteDao.historial`; `PaginasPendientes.detalle` | `seguimientoOpcionalLimitesYAuditoriaSinCambiarRula`; `arregladoSoloPorPostConCsrfYUnaSolaAuditoria`; detalle y capturas de historial |
| RNF-007: mensajes y campos conservados | `PendientesWeb.atender`; `PaginasPendientes.editar`; `static/js/pendientes.js`; `Html.aviso` | `erroresConservanCamposYExplicanQueCorregir`; `ajaxGuardaYDevuelveDestinoSinRecargarElFormularioEnError`; `error-campos-conservados.jpg` |
| RNF-009: historial sin borrar | `Pendientes.registrarRojo/arreglar`; `PendienteDao.historial` | `otraEvaluacionGeneraOtroPendienteYUnVerdeNoBorraLosAnteriores`; máximo y estado conservados |
| RNF-002 y reglas visuales de AGENTS | `PaginasPendientes.lista/detalle/editar`; `web/Html.java` navegación; `static/css/aplomo.css` | 15 capturas de tres pantallas en cinco tamaños, más `mediciones.json` |
| RNF-011: separación y cálculo único | `modelo/PendienteArreglo.java`, `PendienteDao`, `Pendientes`, `PendientesWeb`, `PaginasPendientes`; integración en `Evaluaciones.pendientes` | Compilación y 598 pruebas. Sin cambios en las cuatro clases del cálculo, tablas, dependencias ni esquema |

Rutas Java relativas a `src/main/java/co/aplomo/`; pruebas a `src/test/java/co/aplomo/`; archivos estáticos a `src/main/resources/`. Evidencias en `docs/evidencia_paso9/`.

## Pruebas y datos

- `mvn compile test`: **598 ejecutadas, 598 aprobadas, 0 fallos, 0 errores, 0 omitidas**.
- 25 pruebas nuevas: 19 ejecuciones de `servicio/PendientesTest` y 6 de `web/HttpPendientesTest`. Las 573 anteriores siguen pasando.
- Todas las pruebas y la revisión visual usan **rula_ergonomia_pruebas**. Los datos de QA se mantuvieron en una transacción temporal; al cerrar el servidor se confirmó el **ROLLBACK**. Las fotos temporales propias de esa revisión se retiraron; no se tocaron fotos existentes.
- Consulta de solo lectura después de QA: `Empresa de prueba sin confirmar`: **0** coincidencias en `rula_ergonomia` y **0** en `rula_ergonomia_pruebas`. No se borraron datos.
- Consulta de solo lectura en la base real: **0 evaluaciones rojas anteriores sin pendiente**. No fue necesaria una carga retroactiva ni se ejecutó una migración.
- No se ejecutó el script SQL inicial, no se cambió el esquema, ni se copiaron credenciales al repositorio.

## Revisión visual y dispositivos

Se vieron las pantallas reales del Paso 9 con datos ficticios de la base de pruebas en el navegador integrado. Se compararon los componentes de la referencia: barra y navegación, tipografía, colores Obsidian & Ivory, tablas `.tablewrap`/`.resp`, tarjetas, botones, formularios y etiquetas. **El mock no tiene una pantalla específica de Pendientes**; se reutilizaron sus componentes de listas y formularios, sin afirmar una comparación exacta con una pantalla inexistente.

| Ancho | Lista | Detalle e historial | Editar seguimiento | Navegación / desbordamiento |
|---|---|---|---|---|
| 390 px | Filas convertidas en tarjetas legibles | Una columna y botones visibles | Campos y botón dentro del ancho | Sin desplazamiento lateral; navegación en varias filas; controles medidos de 44 px |
| 810 px | Tabla y filtros dentro del ancho | Tarjeta e historial legibles | Campos dentro de la tarjeta | Sin desplazamiento lateral; navegación completa; controles medidos de 44 px |
| 1080 px | Cinco columnas visibles | Acciones e historial accesibles | Campos distribuidos dentro del formulario | Sin desplazamiento lateral; controles medidos de 44 px |
| 1366 px | Tabla completa | Detalle, seguimiento e historial | Formulario centrado | Sin desplazamiento lateral ni menú recortado |
| 1920 px | Contenido limitado al ancho del mock | Tarjeta e historial con márgenes | Formulario con ancho controlado | Sin desplazamiento lateral ni menú recortado |

Capturas: `{390,810,1080,1366,1920}-{lista,detalle,editar}.jpg`. `mediciones.json` guarda el ancho del documento, el del menú y la altura de controles visibles por pantalla. En las 15 observaciones el documento no excede el ancho solicitado y el menú no tiene contenido oculto de lado. Se corrigió el ajuste de las seis opciones del Administrador para que se envuelvan sin recortarse. El botón Cerrar del aviso de error del formulario tiene área mínima de 44 × 44 px.

Se probó con mouse: entrar, navegar, editar, provocar un error de fecha y conservar los campos, guardar y marcar Arreglado. Se verificó en el historial el caso aprobado de máximo 6 → 7 con estado Arreglado conservado. También se ingresaron directamente, como Usuario, las direcciones de editar un pendiente propio y consultar uno ajeno: ambas mostraron Acceso denegado. La consola no registró errores ni advertencias durante el recorrido válido. Las respuestas 403 de las comprobaciones negativas son esperadas.

## Checklist

- ✅ `mvn test`: 598 aprobadas; compilación correcta.
- ✅ Componentes comparados con el mock y capturas de lista, detalle y edición. Límite: no existe una pantalla específica de Pendientes en la referencia.
- ✅ 390 / 810 / 1080 / 1366 / 1920 px sin desplazamiento lateral en las tres pantallas; navegación accesible.
- ✅ Botones medidos de 44 px a 390 / 810 / 1080 px.
- ❌ Prueba física con el dedo en un celular o iPad: no realizada; no equivale a la revisión de tamaños en el navegador.
- ✅ El Usuario no puede editar ni marcar Arreglado; comprobado por HTTP y escribiendo direcciones a mano. Otra empresa denegada en pantalla.
- ✅ Textos revisados en español, con tildes y fechas como «20 nov 2026»; el selector de fecha nativo sigue el formato del navegador.
- ✅ Sin errores ni advertencias en la consola durante las acciones válidas revisadas.
- ✅ `config/db.properties` y `fotos/` excluidos de Git; no se publican cuentas ni contraseñas de QA.
- ❌ Compatibilidad completa en Chrome, Edge y Firefox: queda para el Paso 13, como indica el plan.

## Lo que NO se hizo o quedó pendiente

No se empezó el Paso 10 (sugerencias), 11 (consultas generales), 12 (informe), 13 (compatibilidad completa) ni 14 (IA). No se cambió el cálculo RULA, la tolerancia NEUTRO = 5°, las tablas ni la base. No se agregaron dependencias. No se reabren pendientes Arreglados automáticamente ni se cierran los de otras evaluaciones. No hay cambios funcionales a medias del Paso 9; quedan las comprobaciones físicas y de navegadores indicadas arriba.

## Cómo probarlo

Para repetir automáticamente, con MySQL encendido y la configuración existente: `mvn compile` y `mvn test`. Las pruebas seleccionan la base de pruebas. Para la aplicación existente abre `http://localhost:8081` e ingresa con tu cuenta habitual. El siguiente recorrido manual crea datos de uso; hazlo en tu instancia de pruebas si quieres mantener la real sin datos de demostración. Las cuentas ficticias de las capturas ya se revirtieron y no sirven para iniciar sesión.

1. Como Administrador, inicia una visita y completa una evaluación con foto y un cálculo Rojo. Guarda el análisis calculado por el servidor.
2. Abre **Pendientes**: debe aparecer automáticamente, Abierto, con puntaje y fecha. Un análisis Verde o Amarillo no genera pendiente y se muestra Aceptable o En observación en su resultado.
3. Abre **Ver pendiente → Editar seguimiento**. Escribe qué corregir, elige prioridad y fecha límite. Guarda y comprueba los datos y la acción en el historial.
4. Prueba 256 caracteres o una fecha inválida: el servidor rechaza el dato y conserva el formulario. Corrige y guarda.
5. Prueba Mostrar Abiertos/Arreglados/Todos y ordenar por prioridad, fecha de registro o fecha límite.
6. Pulsa **Marcar como arreglado** cuando corresponda. El estado cambia y queda registrada la persona y hora; el puntaje RULA permanece igual. En Todos o Arreglados puedes volver a consultarlo.
7. Antes de finalizar esa visita, guarda otro análisis Rojo de mayor puntaje para la misma evaluación. Debe mantener un solo pendiente, subir al máximo y conservar sus datos y Arreglado. Otra evaluación roja genera otro pendiente sin borrar el anterior.
8. Finaliza la visita. Como Usuario de esa empresa, consulta Pendientes: no hay botones de edición ni fotos. Copia la dirección del detalle y añade `/editar`: debe indicar Acceso denegado. La dirección de un pendiente de otra empresa también debe denegarse.
9. Repite las tres pantallas en 390, 810, 1080, 1366 y 1920 px. La prueba física con el dedo y el recorrido completo en los tres navegadores siguen pendientes del Paso 13.

## Git y cierre

Un commit del paso, con el nombre **Paso 9: pendientes de arreglo y seguimiento**. Para ver sus identificadores: `git log --oneline 56bd2de..HEAD`. El hash concreto y la confirmación del push a `origin/main` se entregan en el mensaje de cierre. El resumen de la comprobación final está en `docs/evidencia_paso9/verificacion-final.txt`.

AGENTS.md actualizado: Paso 9 entregado y pendiente del OK de Andy. **Esperar su OK antes del Paso 10.**
