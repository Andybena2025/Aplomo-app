# Informe del Paso 11 · Consultas

11 nov 2026 · America/Bogota. Alcance: corrección de la lista de Sugerencias solicitada por Andy y únicamente el Paso 11 de docs/06. Esperar su OK antes del Paso 12.

## Qué se hizo

Las once secciones de RF-024 tienen tablas con búsqueda, filtros combinables, orden ascendente/descendente y contador de resultados: Empresas, Áreas, Cargos, Sedes, Salones, Puestos, Reservas, Visitas, Evaluaciones, Pendientes y Sugerencias. Al pulsar una cabecera se conserva la búsqueda y los filtros. También se puede elegir Ordenar por y Dirección, útil en celular, donde cada fila se adapta a una tarjeta. Una consulta vacía indica «0 resultados» y cómo cambiarla.

Se accede desde las secciones existentes, la tarjeta **Inicio → Evaluaciones** y **Ver otras secciones**, sin agregar otra pestaña a la barra superior. Los detalles de empresa, sede, salón y visita tienen enlaces Consultar para abrir la lista con ese contexto. Se conservan los formularios y acciones administrativas anteriores.

Las consultas son GET de solo lectura. El servidor limita primero en SQL la empresa y los resultados permitidos. Después aplica búsqueda, filtros y orden a las filas autorizadas en Java. Las opciones de empresa, área, cargo, sede y salón también se limitan en SQL; escribir un ID ajeno en la dirección se deniega. No se descargan datos ajenos para ocultarlos en el navegador. Las consultas no escriben en la base ni en la bitácora.

## Ajuste previo de Sugerencias

Se completó y verificó antes de desarrollar las consultas, con las 623 pruebas anteriores, en el commit `759f5f4 Ajuste previo al Paso 11: listas de sugerencias`.

- Lista de una evaluación: cinco columnas; se oculta Puesto porque ya aparece en la tarjeta de contexto.
- Lista general: conserva las seis columnas, incluida Puesto.
- Se añade margen alrededor de Escribir sugerencia para separarlo del contador «11 sugerencias».
- El estilo de Origen identifica su columna por etiqueta y no por posición, para que ocultar Puesto no cambie el formato.

La nueva lista del Paso 11 conserva los mismos comportamientos. Evidencia: `ajuste-sugerencias-evaluacion-810.jpg`, `ajuste-sugerencias-general-810.jpg` y las series finales `{ancho}-sugerencias-evaluacion.jpg` / `{ancho}-sugerencias.jpg`.

## Aclaración que respondió Andy el 11 nov 2026

El Usuario consulta **todas las reservas de su empresa**: Esperando, En cola, Finalizadas y Canceladas, incluidas las razones de cancelación, sin controles para crear ni cambiar datos.

La restricción «solo Finalizadas» se aplica a los **resultados**: visitas, evaluaciones, puntajes, pendientes y sugerencias de una visita En curso no se muestran hasta que esta finalice, tampoco escribiendo su dirección a mano. Las fotos siguen reservadas al Administrador incluso después de finalizar.

Se probó una visita En curso que ya tenía un análisis **7 · Rojo · Nivel 4**, pendiente y sugerencias. El Usuario no pudo abrir ninguno por ID. Además:

- Puesto 1 tenía un resultado Finalizado Verde 2 y otro Rojo 7 En curso. El Administrador veía el Rojo; el Usuario conservaba **2 · Verde · Nivel 1**.
- Puesto 2 solo tenía el Rojo En curso: el Usuario veía **Sin resultado**.
- Después de finalizar esa visita, la prueba de servicio comprobó que ambos puntajes 7 quedaban disponibles.
- Se protegieron también los puntajes de las tarjetas de puesto dentro del salón y su detalle, que antes podían tomar el último puntaje de una visita En curso.

El detalle de evaluación para el Usuario muestra postura, criterio, motivo, fecha, duración y resultados guardados; no incluye fotos, rutas de archivo ni acciones administrativas. No se cambian los análisis ni el cálculo RULA.

## Filtros disponibles

Buscar, Estado, orden por cabecera/selector y Dirección están en las once secciones. Los demás filtros corresponden a los datos de cada sección; no se muestran campos de puntaje en catálogos que no tienen análisis.

| Sección | Filtros adicionales |
|---|---|
| Empresas | Empresa |
| Áreas | Empresa, área |
| Cargos | Empresa, cargo |
| Sedes | Empresa, sede |
| Salones | Empresa, sede, salón |
| Puestos | Empresa, sede, salón, área, cargo, número, puntaje, semáforo, nivel oficial RULA |
| Reservas | Empresa, sede, salón, fecha desde/hasta |
| Visitas | Empresa, sede, salón, inicio real desde/hasta |
| Evaluaciones | Empresa, sede, salón, área, cargo, número, fecha desde/hasta, puntaje, semáforo, nivel oficial RULA |
| Pendientes | Empresa, sede, salón, área, cargo, número, registro desde/hasta, puntaje, semáforo, nivel oficial RULA |
| Sugerencias | Empresa, sede, salón, área, cargo, número, fecha límite desde/hasta, puntaje, semáforo, nivel oficial RULA |

Colores y niveles oficiales se leen de `nivel_riesgo` en MySQL. Las etiquetas usan «puntaje · color · Nivel». Los puestos sin resultado no reciben un puntaje inventado. Pendientes conserva Abiertos como filtro inicial y permite ordenar por prioridad, fecha de registro o fecha límite. Su etiqueta superior sigue siendo **Seguimiento**, como en la corrección aprobada del Paso 10.

## Requisitos → implementación → comprobación

| Requisito | Archivo y método | Prueba o evidencia |
|---|---|---|
| RF-024: tablas en las once secciones | `modelo/SeccionConsulta`; `dao/ConsultaDao.filas`; `servicio/Consultas.listar`; `web/ConsultasWeb.atender`; `PaginasConsultas.lista` | `ConsultasTest.cadaSeccionSoloDevuelveEmpresaAutorizada` (11 ejecuciones); `HttpConsultasTest.listasDelUsuarioNoExponenEmpresaAjenaNiAcciones` (11); 55 capturas de las once listas |
| RF-024: búsqueda y cero coincidencias | `Consultas.coincide`; `PaginasConsultas.filtros/lista` | `busquedaIgnoraMayusculasTildesYNoInterpretaSql`; `sinCoincidenciasMuestraCeroYTextoHtmlSeEscapa`; `busqueda-sin-resultados-810.jpg` |
| RF-024: todos los filtros combinados | `Consultas.validar/coincide`; `ConsultaDao.catalogos/riesgos`; `SeccionConsulta.filtros`; `PaginasConsultas.filtro` | `todosLosFiltrosDeEvaluacionSeCombinan`; `filtrosCombinadosYOrdenDeCabeceraConservanLaConsulta`; `filtros-combinados-810.jpg` con una coincidencia |
| RF-024: orden y contador, conservar parámetros | `Consultas.comparador/orden`; `PaginasConsultas.lista/filtros/url` | `ordenaNumerosEnAmbosSentidosYConservaNulosAlFinal`; prueba HTTP de cabeceras; `usuario-orden-descendente-810.jpg` con puestos 3, 2, 1 |
| RF-002 / RF-024: propia empresa y sin acciones de escritura | `AccesoDatos.vigente/empresaPermitida`; `ConsultaDao.filas/catalogos`; `Consultas.validar`; `ConsultasWeb.atender` | `catalogosAjenosSeDenieganTambienPorParametro` (5); `direccionesDirectasDeOtraEmpresaSeDeniegan` (11); captura `usuario-denegado-otra-empresa-810.jpg` |
| RF-013b y aclaración de Andy: resultados solo Finalizados | `ConsultaDao.filas`; `Consultas.evaluacion/listar`; `dao/PuestoDao.seleccion/leer`; `PaginasConsultas.evaluacion` | `puntajeDelPuestoSeOcultaHastaFinalizarInclusoSiHayUnVerdeAnterior`; `evaluacionFinalizadaAbreSinFotosYLosPuntajesAbiertosNoSeFiltran`; `rojoEnCursoSeDeniegaPorDireccionDirecta` (5); capturas Usuario de puestos, evaluación final y cuatro resultados denegados |
| Aclaración de Andy: todas las reservas propias, cancelación, solo consulta | `ConsultaDao.filas` caso RESERVAS; servicios y detalle existentes de Reservas; `PaginasReservas.detalle` | `usuarioVeTodasSusReservasConCancelacionPeroNoResultadosAbiertos`; `reservasPropiasSeConsultanEnTodosLosEstadosIncluidoMotivo`; `usuario-reservas-810.jpg`, `usuario-reserva-cancelada-810.jpg` |
| RNF-004: SQL parametrizado, validación y HTML escapado | `ConsultaDao.filas/opciones`; `Consultas.validar`; `Html.escapar`; `PaginasConsultas` | `rechazaFiltrosInvalidos` (8); búsqueda SQL tratada como texto; HTML escapado y direcciones inválidas 400; suite previa de CSRF/RBAC conservada |
| RNF-006: Usuario nunca ve fotos | `Consultas.Detalle/evaluacion`; `ConsultaDao.Fila/filas`; `ConsultasWeb.atender`; protección existente de `/fotos/` | Listas y evaluación final sin `/fotos/`; acceso directo a foto En curso 403; pruebas anteriores de privacidad en verde |
| RNF-007: mensajes y español | `PaginasConsultas.lista/filtro/evaluacion`; `Consultas.validar`; mensajes de Router | «0 resultados», aviso para cambiar filtros, fechas españolas y «Acceso denegado» vistos en pantalla; pruebas de fecha/orden inválidos |
| RNF-002 y AGENTS: mock y cinco tamaños | `PaginasConsultas`; `PaginasEmpresas`; `PaginasVisitas`; `static/css/aplomo.css` | 60 observaciones de las once listas y Sugerencias por evaluación; JSON de mediciones y capturas en `docs/evidencia_paso11/` |
| RNF-011: capas y una sola implementación de RULA | `SeccionConsulta`, `ConsultaDao`, `Consultas`, `ConsultasWeb`, `PaginasConsultas`; `Evaluaciones.consultas` | Compilación y suite completa. Sin cambios en MotorRula, PuntuadorSegmentos, MedidorAngulos, TablasRula, esquema SQL ni dependencias |

Rutas Java relativas a `src/main/java/co/aplomo/`; pruebas a `src/test/java/co/aplomo/`; estáticos a `src/main/resources/`.

## Verificación y datos

- **686 pruebas aprobadas**, 0 fallos, 0 errores y 0 omitidas: las 623 anteriores más 30 ejecuciones de `ConsultasTest` y 33 de `HttpConsultasTest`.
- Compilación y pruebas en `rula_ergonomia_pruebas`. `EscenarioConsultas` prepara empresas distintas y una visita En curso con Rojo, dentro de la transacción reversible de cada prueba.
- Las dos sesiones de revisión visual también usaron `rula_ergonomia_pruebas`. Se confirmó su ROLLBACK al detenerlas; únicamente se retiraron las fotos temporales creadas por esos servidores. No se usaron cuentas reales.
- Auditoría final de solo lectura: **0** empresas llamadas «Empresa de prueba sin confirmar» en `rula_ergonomia` y **0** en `rula_ergonomia_pruebas`. No se borró nada para obtener ese resultado.
- Paso 11 no instala catálogos ni modifica la base real, su esquema, contraseñas, tablas RULA o datos de configuración. Las 11 reglas legítimas del Paso 10 se conservan.
- Evidencia de comprobación final en `docs/evidencia_paso11/verificacion-final.txt`.

## Pantallas y tamaños

Se compararon tabla, filtros, contador y cabeceras con la consulta de resultados del mock y sus componentes `.card`, `.tablewrap`, `.resp`, `.btn`, `.eyebrow`, barra superior y etiquetas. El mock no tiene once pantallas administrativas distintas; se reutilizan sus componentes, sin afirmar una copia de pantallas inexistentes. La lista contextual de Sugerencias reutiliza su tarjeta de puesto.

| Ancho | Comprobación en las once listas y Sugerencias de una evaluación |
|---|---|
| 390 px | Filas como tarjetas, campos de filtros dentro del ancho, orden/dirección en una fila y Aplicar/Limpiar en la siguiente; controles medidos de al menos 44 px de alto; menú adaptado a dos filas |
| 810 px | Tabla dentro del ancho, cabeceras y enlaces visibles; barra en una fila |
| 1080 px | Tabla y controles sin desplazamiento lateral; barra en una fila |
| 1366 px | Contenido limitado al ancho del mock, tabla sin desbordamiento; barra en una fila |
| 1920 px | Márgenes del mock y tabla contenida; barra en una fila |

Las 60 mediciones finales no detectaron desbordamiento del documento, del menú ni del contenedor de tabla. Se corrigieron durante QA los selectores Ordenar por/Dirección de 390 px, que quedaban estrechos; ahora miden 148,5 px de ancho y 44 px de alto. Se actualizaron sus capturas. Se mantuvo Seguimiento en Pendientes y se repitieron sus cinco capturas.

Hay 75 capturas: 60 por tamaño y 15 de ajuste previo, filtros, búsqueda, permisos y resultados visibles. Archivos `{390,810,1080,1366,1920}-{seccion}.jpg`, serie `sugerencias-evaluacion`, `mediciones-*.json` y `permisos-direccion-manual.json`. Los identificadores ficticios pueden variar entre sesiones; las capturas de Pendientes corresponden a la segunda sesión reversible.

## Checklist

- ✅ `mvn compile test`: 686 aprobadas, 0 fallos/errores/omitidas.
- ✅ Tablas, filtros y componentes comparados con el mock; capturas en `docs/evidencia_paso11/`.
- ✅ 390 / 810 / 1080 / 1366 / 1920 px sin deslizar de lado en las doce vistas revisadas.
- ✅ Barra en una sola fila desde 810 px.
- ✅ Controles medidos de al menos 44 px en celular.
- ❌ Prueba física con el dedo en celular/iPad: no realizada. Las medidas de navegador no sustituyen esa prueba.
- ✅ Usuario no accede a otra empresa ni a resultados de una visita En curso, incluso con Rojo ya guardado y direcciones directas. Reservas propias en sus cuatro estados, sin acciones administrativas.
- ✅ Textos con tildes y fechas «10 nov 2026». El formato del selector nativo de fecha depende del navegador.
- ✅ Consola sin errores ni advertencias en los recorridos válidos. Los 403 de las comprobaciones negativas son esperados.
- ✅ Nada sensible versionado: sin `config/db.properties`, fotos privadas ni contraseñas reales. Las capturas contienen únicamente datos ficticios revertidos.
- ❌ Recorrido completo en las dos últimas versiones de Chrome, Edge y Firefox: pendiente del Paso 13. Esta revisión visual se realizó en el navegador integrado.

## Lo que NO se hizo

No quedan funciones del Paso 11 a medias. No se empezó el Paso 12 (calificaciones de sedes/empresa e informe), 13 (compatibilidad completa) ni 14 (IA). No se cambia el esquema, RULA, NEUTRO = 5°, bibliotecas, cuentas reales ni datos existentes. No se borró información. Quedan pendientes las comprobaciones físicas y de navegadores indicadas arriba; no se presentan como realizadas.

## Cómo probarlo tú

1. Mantén MySQL encendido. Desde la raíz ejecuta `mvn compile` y `mvn test`: deben pasar 686 pruebas. No ejecutes el script SQL inicial sobre una base existente.
2. Abre `http://localhost:8081` e ingresa con tu cuenta habitual. Las cuentas ficticias de las capturas ya no existen: se revirtieron.
3. Abre Empresas y **Ver otras secciones**. Revisa las once listas. También puedes entrar desde Inicio → Evaluaciones o los enlaces Consultar en empresa/sede/salón/visita.
4. Escribe una búsqueda y pulsa Aplicar filtros. Prueba una sin coincidencias: debe indicar 0 resultados. Limpiar filtros restaura la consulta.
5. En Evaluaciones, abre Más filtros y combina empresa, sede, salón, área, cargo, número, fechas, puntaje, semáforo, nivel y estado. Solo deben quedar las coincidencias y su contador.
6. Pulsa una cabecera dos veces: cambia ascendente/descendente manteniendo filtros. En celular usa Ordenar por y Dirección.
7. Desde una evaluación con análisis abre Sugerencias: no debe haber columna Puesto, sí tarjeta de contexto y espacio entre Escribir sugerencia y el contador. En la lista general sí debe aparecer Puesto.
8. En tu instancia de pruebas, entra como Usuario de empresa A: solo verás sus datos y ningún control de creación/edición. En Reservas comprueba Esperando, En cola, Finalizadas y Canceladas, y abre una cancelada para ver su motivo.
9. Con un Rojo guardado en una visita En curso, copia sus direcciones de visita, evaluación, pendiente y sugerencia desde Administrador; ábrelas como Usuario: Acceso denegado. Haz lo mismo con una dirección de empresa B y con su ID en un filtro.
10. Si el puesto tenía un resultado Finalizado anterior, el Usuario debe conservar ese puntaje; si nunca tuvo uno, debe decir Sin resultado. Finaliza la visita de pruebas y comprueba que sus resultados quedan disponibles. Las fotos continúan denegadas al Usuario.
11. Revisa las listas a 390, 810, 1080, 1366 y 1920 px. Toda la barra debe quedar en una fila desde 810 px. La comprobación táctil física aún requiere un celular/iPad.

## Git y entrega

- `759f5f4 Ajuste previo al Paso 11: listas de sugerencias`.
- Commit del paso: **Paso 11: consultas, filtros y permisos de resultados**. Su identificador concreto y la confirmación del push a `origin/main` se entregan en el mensaje de cierre después de verificar la sincronización. Se consultan con `git log --oneline 8ef9c02..HEAD`.
- AGENTS.md actualizado: Pasos 0–10 aprobados, Paso 11 implementado y pendiente del OK de Andy. **No empezar el Paso 12 sin su OK.**
