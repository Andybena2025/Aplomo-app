# Informe del Paso 7 · APLOMO.

Fecha: 11 de noviembre de 2026. Zona: America/Bogota.

## Qué se implementó

Desde una visita **En curso**, el Administrador puede abrir **Evaluar puesto** y recorrer los primeros tres pasos de la barra de seis del mock:

1. Puesto activo del salón reservado, postura o tarea, criterio, motivo y minutos por jornada (1–720).
2. Las **seis** condiciones de RF-015, con casillas `.opt`, ayuda y otras observaciones de máximo 255 caracteres. No hay altura ni las ocho condiciones del prototipo.
3. Consentimiento y fotografías, con tarjetas de perfil, frente y detalle, opción para el otro lado, vista previa, vista y descripción por foto, y posibilidad de quitar una selección antes de guardar.

Con aceptación se guarda `persona_acepto=true`, `CON_FOTOS` y de 1 a 10 fotos JPG/PNG de hasta 5 MB, cada una con vista y descripción obligatoria de hasta 200 caracteres. Se comprueba la firma y la decodificación real: cambiar un PDF a `.jpg` no lo convierte en una foto.

Sin aceptación se guarda `persona_acepto=false`, `SIN_REGISTRO_FOTOGRAFICO` y ninguna foto. El servidor rechaza cualquier archivo que llegue en ese caso. **No hay un campo de motivo de ausencia de fotos**; `evaluacion.motivo` corresponde exclusivamente a la elección de la postura, según la aclaración de Andy en el chat.

La base conserva los metadatos y la ruta relativa; los bytes quedan en `fotos/`, con nombre UUID y sin sobrescribir archivos. Las rutas de imágenes y el detalle administrativo comprueban permisos en el servidor. El guardado de evaluación, condiciones, metadatos y bitácora es transaccional; si falla, se revierten los cambios y se retiran únicamente los archivos nuevos de esa operación.

Los errores dejan el mismo formulario y las fotos seleccionadas. Los avisos se pueden cerrar. Si la sesión cambió, se puede iniciar sesión en otra pestaña y recuperar el formulario al reintentar, sin volver a escribir sus datos.

## Comprobación previa y protección de datos

- Se hizo `git pull --ff-only` y se leyó completo `AGENTS.md`, incorporado en `52ed67a`. También se revisaron completos `CLAUDE.md`, documentos 00–07, esquema, mock y referencias antes de implementar.
- El lanzador de las capturas del QA aprobado `c1cc829` usa explícitamente `rula_ergonomia_pruebas`.
- La auditoría de solo lectura encontró **0** empresas llamadas **“Empresa de prueba sin confirmar”** en `rula_ergonomia` y **0** en `rula_ergonomia_pruebas`, antes del Paso 7 y al cierre definitivo, después de revertir todas las revisiones visuales y ejecutar las pruebas.
- Todas las capturas de este paso usan `rula_ergonomia_pruebas`. Los datos visuales están en una transacción sin confirmar; al cerrar la revisión se ejecuta ROLLBACK. Las fotos de revisión son imágenes públicas de demostración generadas con IA, no fotografías de trabajadores reales.
- No se ejecutó el script inicial, no se cambió el esquema y no se borraron datos ni documentos de la base real.
- No se modificaron dependencias ni tecnologías. No se cambió el motor RULA ni su tolerancia de 5°.

## Requisitos → implementación → comprobación

Los archivos Java se encuentran bajo `src/main/java/co/aplomo/`; las pruebas bajo `src/test/java/co/aplomo/`.

| Requisito | Archivo y método | Prueba / evidencia |
|---|---|---|
| RF-014 · puesto y postura en una visita En curso | `servicio/Evaluaciones.java`: `contexto`, `guardar`, `validar`, `abierta`; `dao/EvaluacionDao.java`: `puestos`, `crear`; `web/PaginasEvaluacion.java`: `nueva`; `web/PaginasVisitas.java`: `detalle` | `EvaluacionesTest.validaContextoYTodosLosCamposObligatorios` (6 casos), `noEvaluaPuestoInactivoAjenoNiVisitaFinalizadaYUsuarioNoAccede`, `guardaPosturaSeisCondicionesFotosYResponsableSinAnalisisNiPuntaje`; capturas `*-paso1.jpg` |
| RF-015 · seis condiciones y observaciones ≤255 | `PaginasEvaluacion.CONDICIONES`, `nueva`; `Evaluaciones.validar`; `EvaluacionDao.condiciones` | `guardaPosturaSeisCondicionesFotosYResponsableSinAnalisisNiPuntaje`, `observacionesDe255SeConservanY256SeRechazan`; capturas `*-paso2.jpg` |
| RF-016 · consentimiento, cantidad, tamaño, tipo real, vista y descripción; sin registro | `servicio/Fotos.java`: `validar`, `guardar`; `Evaluaciones.guardar`; `EvaluacionDao.foto`; `web/Multipart.java`: `leer`; `web/EvaluacionesWeb.java`: `atender`; `static/js/evaluacion.js`: `renderFotos` y envío del formulario | `rechazaFotosInvalidasSinEscribirEvaluacionNiArchivos` (13 casos), `admiteDiezFotosConVistasYDescripcionesIndependientesYJpgReal`, `aceptaExactamenteCincoMBYDescripcionDe200Caracteres`, `sinFotosGuardaAceptacionFalsaYMotivoSoloDeLaPostura`; `HttpEvaluacionesTest.rechazaArchivosInvalidosDesdeHttpSinGuardarYConMensajeClaro` (6 casos), `sinFotosEsExplicitoYNoRequiereAceptarLasFotosQueNoExisten`; capturas de paso 3, otro lado y ambos guardados |
| RNF-006 · fotos privadas | `Evaluaciones.foto`, `consultar`, `admin`; `Fotos.leer`, `ruta`; `Router.atender`; `EvaluacionesWeb.atender` | `HttpEvaluacionesTest.guardaConMultipartYFotoProtegidaSeAbreSoloComoAdministrador`, `fotoSinSesionRespondeAccesoDenegadoSinRedirigir`; `EvaluacionesTest.noAbreRutasArbitrariasNiEnlacesSimbolicos`; captura de dirección directa de foto como Usuario |
| RNF-007 · mensajes claros y conservación de lo escrito | `EvaluacionesWeb.atender`, `responder`; `PaginasEvaluacion.nueva`; `evaluacion.js`: `aviso`, `actualizarFormulario` y envío | HTTP inválido devuelve español sin SQL; `falloDeAuditoriaRevierteDatosYRetiraSoloArchivosNuevos`; revisión visual de PDF renombrado conservando campos y selección, rechazo sin aceptación, cierre del aviso, vuelta entre pasos y recuperación tras iniciar otra sesión |

Comprobaciones de apoyo: RF-002 (permisos directos), RNF-004 (CSRF, escape HTML, consultas preparadas y rutas), RNF-005 (responsable y bitácora). El test HTTP comprueba escape de `<script>`, permiso GET/POST y ausencia de nombres originales en el detalle.

## Pruebas automatizadas

`mvn compile test`: **532 ejecutadas, 532 aprobadas, 0 fallos, 0 errores, 0 omitidas**.

La ejecución final se realizó también desde el proyecto de IntelliJ el 11 de noviembre a las 09:18 (America/Bogota). El resumen verificable está en [verificacion-final.txt](evidencia_paso7/verificacion-final.txt).

Se añaden **37 casos** a los 495 anteriores: 27 de servicio y archivos, y 10 HTTP. Se usan MySQL real en la base de pruebas, archivos temporales y ROLLBACK de los datos creados.

| Caso solicitado | Resultado |
|---|---|
| 11 fotos | Error; ninguna evaluación ni archivo guardado |
| PDF | Error de tipo real |
| Archivo de 6 MB | Error de tamaño |
| PDF renombrado `.jpg` y MIME `image/jpeg` | Error; no se confía en nombre ni MIME |
| Con fotos sin aceptación | Error; no se guarda |
| Sin registro con una foto enviada directamente | Error del servidor |
| 10 fotos JPG/PNG válidas | Aceptadas, con metadatos independientes |
| Exactamente 5 MB y descripción de 200 caracteres | Aceptados |
| Observaciones de 255 / 256 caracteres | Se conservan / se rechazan |
| Usuario abre foto, asistente, detalle o envía POST | 403, Acceso denegado |
| Sin sesión abre foto | 403, Acceso denegado, sin redirección |
| Fallo al registrar la bitácora | ROLLBACK de datos y limpieza solo de archivos nuevos |

## Checklist de AGENTS.md

- ✅ `mvn test`: 532 aprobadas.
- ✅ Comparación visual con el mock: barra de seis pasos, puesto y postura, condiciones `.opt`, observaciones, tarjetas de tomas, otro lado y botones. Se respetó la diferencia de seis condiciones autorizada. Capturas en `docs/evidencia_paso7/`.
- ✅ 390, 810, 1080 y 1366 px sin desplazamiento horizontal; también 1920 px. Se midieron el documento y la navegación, además de revisar las capturas.
- ❌ Prueba con el dedo en un dispositivo físico: no se pudo realizar desde esta sesión. Se revisaron los tamaños móviles y controles de al menos 44 px; eso no sustituye tocar un teléfono o iPad real.
- ✅ Usuario: probado abriendo directamente las direcciones de la foto y del asistente. Se ve “Acceso denegado”; también hay pruebas HTTP de GET y POST.
- ✅ Textos revisados: tildes en evaluación, fotografía, descripción, observaciones, postura y demás campos.
- ✅ Sin errores de JavaScript en los recorridos válidos ni en la recuperación del formulario; consola sin advertencias ni errores registrados. Los rechazos HTTP de pruebas negativas son deliberados.
- ✅ Sin `config/db.properties`, fotos subidas ni credenciales en los archivos del cambio. Las tres muestras públicas bajo `static/img/` son las referencias de demostración del prototipo.

### Comprobación por tamaño

| Ancho | Puesto y postura | Condiciones | Fotos | Desplazamiento horizontal |
|---|---|---|---|---|
| 390 px | Campos apilados; menú en dos filas | Casillas adaptadas al ancho y observaciones | Una tarjeta por fila; selección y guardado accesibles | No |
| 810 px | Rejilla adaptada; títulos y botones legibles | Seis casillas y ayuda sin desbordes | Tarjetas adaptadas al espacio disponible | No |
| 1080 px | Formulario y barra completos | Casillas y observaciones dentro de la tarjeta | Tomas distribuidas en rejilla | No |
| 1366 px | Distribución de laptop | Componentes del mock | Tres tomas principales y opción otro lado | No |
| 1920 px | Contenido centrado con ancho acotado | Misma proporción visual | Fotos y detalle sin estirar el contenido | No |

Las medidas de viewport son CSS px; las capturas pueden excluir el espacio de la barra vertical del navegador. Las cifras están en [mediciones.json](evidencia_paso7/mediciones.json).

## Lo que NO se hizo o quedó pendiente

- Del alcance funcional del Paso 7: **nada a medias**.
- Pendiente de comprobación física: dedo y cámara en teléfono/iPad real. El botón “Tomar foto” ofrece captura trasera donde el navegador del dispositivo la admite; en escritorio puede abrir un selector.
- No se realizó aún la comprobación completa en Chrome, Edge y Firefox; corresponde al paso de compatibilidad y no se presenta como aprobada.
- No se empezó el Paso 8: puntos, ángulos, ajustes, actividad, carga, análisis por foto, resultado RULA en pantalla y tarjetas finales de Inicio. Las evaluaciones del Paso 7 se guardan **sin puntaje**.
- No se implementaron sugerencias, informe, pendientes ni IA del Paso 14.

## Cómo probarlo manualmente

1. Mantener MySQL encendido y abrir la versión actual en `http://localhost:8081`. Ingresar con una cuenta Administrador existente.
2. Abrir una visita **En curso** que tenga puestos activos en el salón de su reserva. Si hace falta, crear el puesto en Empresas y crear/aceptar una reserva para iniciar la visita.
3. Pulsar **Evaluar puesto**. Elegir puesto, escribir postura, criterio, motivo y de 1 a 720 minutos. Continuar.
4. Marcar condiciones, abrir su ayuda y escribir observaciones. Comprobar que solo hay seis casillas. Volver y avanzar: los datos se conservan.
5. Elegir **Con registro fotográfico**. Intentar guardar sin aceptación: debe pedirla. Marcarla, subir un JPG o PNG, revisar su vista y descripción y probar **Otro lado del cuerpo**. Las muestras no se guardan.
6. Volver a condiciones y regresar a fotos: la selección permanece. Guardar. En la visita, abrir **Ver evaluación** y comprobar los datos y la foto.
7. Repetir con un PDF renombrado `.jpg`: aparece un error claro y el formulario mantiene los campos y archivos. Probar también 11 fotos y 6 MB. No debe guardarse una evaluación incompleta.
8. Crear otra evaluación y elegir **Sin registro fotográfico**. No se pide aceptación ni motivo adicional. Guardar y comprobar que el detalle no muestra fotos.
9. Copiar la dirección `/fotos/ID` de una imagen de prueba propia. Salir e ingresar como Usuario; abrir esa dirección y la del asistente: debe aparecer **Acceso denegado**. No compartir imágenes privadas.
10. Para comprobar recuperación, dejar un asistente abierto, salir e ingresar otra vez desde otra pestaña y volver al formulario anterior. Al guardar se actualiza el formulario y se pide reintentar; al hacerlo conserva los datos.
11. Repetir las pantallas en los tamaños indicados. Quedan pendientes el dedo y la cámara en dispositivos físicos y el recorrido de los tres navegadores.
12. Para repetir la comprobación automática: `mvn compile test` desde la raíz; esperar **BUILD SUCCESS** y 532 pruebas.

## Evidencia

| Tamaño | Paso 1 | Paso 2 | Paso 3 |
|---|---|---|---|
| 390 px | [Puesto y postura](evidencia_paso7/390-paso1.jpg) | [Condiciones](evidencia_paso7/390-paso2.jpg) | [Fotos](evidencia_paso7/390-paso3.jpg) |
| 810 px | [Puesto y postura](evidencia_paso7/810-paso1.jpg) | [Condiciones](evidencia_paso7/810-paso2.jpg) | [Fotos](evidencia_paso7/810-paso3.jpg) |
| 1080 px | [Puesto y postura](evidencia_paso7/1080-paso1.jpg) | [Condiciones](evidencia_paso7/1080-paso2.jpg) | [Fotos](evidencia_paso7/1080-paso3.jpg) |
| 1366 px | [Puesto y postura](evidencia_paso7/1366-paso1.jpg) | [Condiciones](evidencia_paso7/1366-paso2.jpg) | [Fotos](evidencia_paso7/1366-paso3.jpg) |
| 1920 px | [Puesto y postura](evidencia_paso7/1920-paso1.jpg) | [Condiciones](evidencia_paso7/1920-paso2.jpg) | [Fotos](evidencia_paso7/1920-paso3.jpg) |

Otras comprobaciones:

- [Sin aceptación](evidencia_paso7/sin-aceptacion.jpg).
- [PDF renombrado y error conservando el formulario](evidencia_paso7/error-pdf-renombrado.jpg).
- [Otro lado del cuerpo](evidencia_paso7/otro-lado.jpg).
- [Foto conservada al volver entre pasos](evidencia_paso7/foto-conservada.jpg).
- [Evaluación guardada y foto protegida](evidencia_paso7/evaluacion-guardada.jpg).
- [Elección sin registro](evidencia_paso7/sin-registro-fotografico.jpg) y [detalle sin fotos](evidencia_paso7/evaluacion-sin-fotos.jpg).
- [Formulario recuperado después de cambiar de sesión](evidencia_paso7/formulario-recuperado.jpg).
- [Usuario: foto denegada](evidencia_paso7/usuario-foto-denegada.jpg) y [asistente denegado](evidencia_paso7/usuario-asistente-denegado.jpg).

Algunas capturas documentan revisiones intermedias de mensajes y la corrección final del selector de fotos; las capturas por tamaño y de otro lado incluyen los botones definitivos “Tomar foto” y “Subir foto”. Todas pertenecen al mismo entorno aislado de pruebas; los IDs cambian después de cada ROLLBACK. El reloj del entorno visual fija el 10 de noviembre para los datos ficticios; el servidor normal conserva la hora real de America/Bogota.

## Git y siguiente paso

Cambio previsto: `Paso 7: evaluaciones, condiciones y fotos protegidas`.

Para listar los commits nuevos: `git log --oneline 52ed67a..HEAD`. La entrega del chat incluye el identificador definitivo y el resultado del push después de ejecutarlos. Se actualizó el Estado de `AGENTS.md` para dejar el Paso 7 pendiente de la aprobación de Andy.

**Detenerse aquí. Esperar el OK antes de empezar el Paso 8.**
