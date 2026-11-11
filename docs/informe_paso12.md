# Informe del Paso 12 · Calificaciones e informe general

11 nov 2026 · America/Bogota. Alcance: RF-025 y RF-026 del Paso 12 de docs/06. El Paso 11 fue aprobado por Andy. Esperar su OK antes del Paso 13.

## Qué se hizo

Primero se ejecutó `git pull --ff-only`: se incorporó `7670b93`, con README, INSTALAR.md, scripts y Maven Wrapper de Claude. Se leyeron completos AGENTS.md, incluida «README e instalación», CLAUDE.md y docs/00–07, además de CU5-28/CU5-29, el esquema y la hoja de informe del mock.

Desde una empresa se puede abrir **Ver calificaciones**. El Administrador también dispone de **Generar informe**, que abre una hoja HTML preparada para **Imprimir o guardar PDF** con el navegador. No se necesitan bibliotecas nuevas ni se descarga código de generación de PDF. El Usuario consulta las calificaciones de su propia empresa, pero no genera informes ni abre los de otra empresa por dirección directa.

La hoja conserva la apariencia clara de `.sheet` del mock: marca, empresa, fecha, tablas, tarjetas y leyenda. Añade los indicadores completos exigidos por RF-025/026, que el prototipo dejaba fuera de alcance. En pantalla conserva la aplicación oscura y su barra superior; en impresión se ocultan navegación, acciones y ayudas y se usa fondo blanco, texto negro y niveles escritos.

No se modifican RULA, NEUTRO = 5°, análisis guardados, promedios históricos, esquema, credenciales ni catálogos de configuración.

## Reglas y respuestas de Andy del 11 nov 2026

1. **Puesto:** máximo de todos los análisis de su **última evaluación Finalizada**, incluidos ambos lados. Nunca máximo histórico: si la Reevaluación salió Verde, cuenta como Verde. Los resultados En curso quedan fuera.
2. **Visita:** conserva el promedio de sus puestos guardado al finalizar, según RF-013b/RF-021. No se reemplaza por la reevaluación posterior del puesto. El sistema existente lo guarda con una décima.
3. **Sede:** promedio de sus visitas Finalizadas que tienen calificación. No se pondera por cantidad de puestos ni de fotos. Una visita sin puntaje conserva su ausencia de calificación; no se convierte en cero.
4. **General (propuesta, ajuste de Andy):** promedio de las sedes calificadas y únicamente con **al menos dos sedes calificadas**. Con cero o una: **Sin calificación general**. Si la empresa tiene una sola sede registrada, el informe dice **No hay calificación general: la empresa tiene una sola sede**.
5. Siempre se muestra la cobertura y los nombres faltantes: **2 de 3 sedes calificadas · falta Sede Norte**. Se hace en calificaciones y en el informe, sin excluir sedes inactivas de su historial.
6. Visitas y fotos analizadas se cuentan en todo el historial Finalizado. Cada fotografía se cuenta una sola vez aunque tenga varios análisis. Cada puesto aparece una sola vez con su última evaluación Finalizada; de esos puestos salen las cifras por color, área y tipo de cargo.
7. Pendientes por su **propio estado**, separados Abiertos/Arreglados, incluidos los de evaluaciones anteriores Finalizadas. Sugerencias **activas**, por tipo y estado, incluidas las Hechas. Las quitadas y los registros En curso quedan fuera.

El mínimo de sedes está centralizado en `Calificaciones.MINIMO_SEDES_GENERAL`; la pantalla toma ese mismo valor. El redondeo del color, marcado como **propuesta**, está en `Calificaciones.REDONDEO_COLOR` (entero más cercano, mitades hacia arriba). Los promedios de sedes y general conservan precisión antes de clasificarse; se presentan hasta con dos decimales. Los umbrales, nombres, acciones y niveles se leen de MySQL. Se muestra por separado el promedio y su clasificación completa, por ejemplo **3,75 · Promedio** y **4 · Amarillo · Nivel 2**. Estos promedios son indicadores del equipo, no parte del método RULA, pendientes de validar con la clienta.

## Contenido del informe

- Datos de empresa, NIT, contacto y fecha/hora de generación en America/Bogota.
- Por sede: dirección, estado, calificación y clasificación escrita, visitas Finalizadas y sus promedios históricos.
- Puestos únicos con salón, descripción, área, cargo/tipo, postura, fecha, máximo de la última evaluación Finalizada y acción del nivel.
- Peligros y aspectos observados: postura, motivo de elección, seis condiciones y otras observaciones realmente registradas, junto con la acción del nivel. No se inventan diagnósticos.
- Estadísticas por sede: visitas, puestos únicos, puestos con/sin puntaje, fotos analizadas, puestos Verde/Amarillo/Rojo, pendientes Abiertos/Arreglados, sugerencias activas por tipo/estado, puestos por área y tipo de cargo y área con más puestos en Rojo. Los empates muestran todas las áreas, sin elegir una arbitrariamente.
- Detalle de pendientes y sugerencias activas, conservando seguimiento, tipo, estado, prioridad y fecha límite.
- Calificación general al final, cobertura de sedes y leyenda con los cuatro niveles oficiales y sus acciones escritos.

Una evaluación sin análisis no recibe un puntaje inventado ni recupera un Rojo antiguo. Sin visitas Finalizadas, el servidor impide generar el informe y explica que hay que finalizar al menos una. Las calificaciones sí pueden consultarse sin datos y muestran qué falta.

La salida autorizada es HTML imprimible y el PDF que guarda el navegador. No se crea un PDF en el servidor ni se inserta una ruta ficticia en `informe_empresa`: esa tabla exige una ruta de archivo en el servidor, y el destino elegido en el navegador no se conoce. No se modifica esa tabla ni su esquema; tampoco se añade un archivo permanente ni un historial de PDFs al servidor.

## Requisitos → implementación → comprobación

| Requisito | Archivo y método | Prueba o evidencia |
|---|---|---|
| RF-025: puesto = máximo de última evaluación Finalizada | `dao/CalificacionesDao.puestos`; `servicio/Calificaciones.cargar` | `CalificacionesTest.reevaluacionVerdeSustituyeRojoHistoricoYEnCursoNoInterviene`; `ultimaEvaluacionSinAnalisisNoRecuperaUnRojoAnteriorYFotoSinAnalisisNoSeCuenta` |
| RF-025: visita histórica, promedio de visitas por sede y sedes por empresa | `CalificacionesDao.visitas`; `Calificaciones.cargar/promediar` | `promediaVisitasPorSedeYSedesSinPonderarPorCantidadDePuestos`: visitas 6 y 3 → sede 4,5; otra sede 3 → general 3,75 |
| RF-025: mínimo dos calificadas y cobertura, una sola sede sin general | `Calificaciones.MINIMO_SEDES_GENERAL/cargar`; `web/PaginasCalificaciones.general/cobertura` | `unaSolaSedeNoTieneGeneral`; `menosDeDosSedesCalificadasNoGeneraGeneral` (2); `sedeSinDatosNoSeCuentaComoCeroNiOcultaSedesInactivas`; `HttpInformesTest.empresaConUnaSedeExplicaQueNoHayGeneral`; `variasSedesPeroSoloUnaCalificadaMuestraFaltantesSinGeneral` |
| RF-025: color del promedio redondeado, rangos desde MySQL | `Calificaciones.calificar/riesgo/REDONDEO_COLOR`; `CalificacionesDao.riesgos`; `PaginasCalificaciones.nota` | `colorUsaPromedioRealSinRedondearAntesLasSedes` (6): debajo y exactamente en 2,5 / 4,5 / 6,5; pruebas del promedio 2,475 sin redondeo intermedio |
| RF-026: datos por sede, puestos, visitas y generación fechada | `modelo/InformeEmpresa`; `Calificaciones.informe`; `PaginasCalificaciones.informe/sede` | `administradorGeneraInformeConCoberturaEstadisticasYNivelesEscritos`; capturas del informe a cinco tamaños; PDF real de Safari |
| RF-026: estadísticas desde MySQL sin duplicar puestos/fotos | `CalificacionesDao.fotosAnalizadas/puestos/pendientes/sugerencias`; `Calificaciones.estadisticas` | `cuentaPendientesPorEstadoYSugerenciasActivasPorTipoYEstado`; prueba de Reevaluación Verde con 4 fotos aunque haya más análisis; distribución de área/cargo en el PDF |
| RF-026: impedir generar sin visitas Finalizadas | `Calificaciones.informe`; `CalificacionesWeb.atender` | `empresaSinVisitasNoGeneraInformeYConsultarNoEscribe`; `empresaSinVisitasExplicaQueFaltaFinalizar`; captura `sin-visitas-finalizadas-810.jpg` |
| RF-026 / docs/07: impresión y lectura en blanco y negro | `PaginasCalificaciones.informe`; `static/js/informe.js`; `static/css/aplomo.css` reglas de impresión | `informe-general-qa.pdf`, creado con el botón en Safari, A4 y color desactivado; siete PNG revisados; `verificacion-pdf.json` |
| RF-002 / RNF-004: Usuario solo su empresa; informes solo Administrador | `Calificaciones.consultar/informe/cargar`; `AccesoDatos.vigente/empresaPermitida`; `CalificacionesWeb.atender`; `Router` | `usuarioSoloSuEmpresaNuncaGeneraInformeYCuentaVigente`; `usuarioConsultaSoloSusCalificacionesYNoGeneraInformesPorDireccion`; capturas de tres direcciones denegadas |
| RNF-004: parámetros, HTML escapado y métodos protegidos | `CalificacionesDao` con `Sql`; `PaginasCalificaciones`; `Router` | `empresaYObservacionesSeEscapanEnElInforme`; `metodosInvalidosIdsYAccesoSinSesionProtegidos` (IDs, GET/POST, CSRF y cierre de sesión) |
| RNF-006: privacidad de fotos | `InformeEmpresa`; `CalificacionesDao`; vista sin rutas ni imágenes de trabajadores | Informe y calificaciones sin `/fotos/`; cifras de fotos analizadas sin abrir sus archivos; pruebas previas de fotos conservadas |
| RNF-002 / RNF-007: diseño, adaptación y avisos claros | `PaginasCalificaciones`; `PaginasEmpresas.empresa`; CSS | 15 observaciones a cinco tamaños, botones ≥44 px a 390; aviso sin Finalizadas y estado Sin calificación vistos en pantalla |
| RNF-009 / RNF-011: historial, capas y cálculo RULA único | `InformeEmpresa`, `CalificacionesDao`, `Calificaciones`, `CalificacionesWeb`, `PaginasCalificaciones`; `Evaluaciones.calificaciones` | Consultas sin escrituras, sedes inactivas conservadas, 710 pruebas; sin cambios en las cuatro clases RULA, SQL de esquema ni dependencias |

Java: rutas relativas a `src/main/java/co/aplomo/`; pruebas a `src/test/java/co/aplomo/`; estáticos a `src/main/resources/`.

## Pruebas, datos y evidencias

- **710 pruebas aprobadas**, 0 fallos, 0 errores y 0 omitidas: 686 anteriores más **16** ejecuciones de `CalificacionesTest` y **8** de `HttpInformesTest`.
- Compilación y suite completa con Maven Wrapper; `node --check` del pequeño controlador de impresión.
- Todas las pruebas y las dos sesiones de QA utilizaron **rula_ergonomia_pruebas**, con datos nuevos en transacciones reversibles. Se confirmó ROLLBACK al cerrar ambos servidores temporales.
- Control final de solo lectura: **0** empresas «Empresa de prueba sin confirmar» en `rula_ergonomia` y **0** en `rula_ergonomia_pruebas`. No se borró información para obtenerlo.
- Ejemplo comprobado: sede Centro, visitas 6 y 3 → promedio **4,5**, clasificación **5 · Rojo · Nivel 3**; sede Sur **3**; sede Norte sin datos. General **3,75**, clasificación **4 · Amarillo · Nivel 2**, cobertura **2 de 3**.
- En Centro, la reevaluación de un puesto pasa de Rojo 7 a Verde 2 y cuenta como Verde; un Rojo posterior En curso no modifica el informe. Los dos pendientes antiguos siguen contándose por sus propios estados, uno Abierto y otro Arreglado.
- Las capturas funcionales de permisos corresponden a la primera sesión; las quince capturas finales por tamaño y el PDF definitivo corresponden a la segunda. Los IDs ficticios cambian entre sesiones y no son cuentas para usar la app.

En `docs/evidencia_paso12/`: **20 capturas de pantalla**, **7 páginas PNG** del PDF, una muestra PDF real y los JSON de medidas y permisos. La verificación final de compilación, base y privacidad está en `verificacion-final.txt`.

## Revisión visual y de impresión

| Tamaño | Qué se comprobó |
|---|---|
| 390 px | Calificaciones en una columna; hoja clara dentro del ancho, tablas como tarjetas, cifras legibles y botones de al menos 44 px |
| 810 px | Tablas contenidas, controles visibles; barra superior en una fila |
| 1080 px | Hoja con ancho controlado, tablas y textos dentro del ancho; barra en una fila |
| 1366 px | Hoja centrada con márgenes del mock; barra completa en una fila |
| 1920 px | Contenido limitado al ancho del mock, sin estirar la hoja; barra en una fila |

Las quince mediciones de Calificaciones, informe general e informe de una sede no detectaron desbordamiento del documento, menú ni tablas. Se compararon con la hoja de informe y los componentes del mock, ampliados con los requisitos reales del paso.

Se abrió **Imprimir o guardar PDF** en Safari y se guardó la muestra A4 con color desactivado y sin fondos ni cabeceras automáticas del navegador. Se revisaron visualmente las **siete páginas** después de convertirlas a PNG. Se corrigieron cifras partidas y tablas separadas de sus cabeceras; se repitió la impresión. La muestra final mantiene completas cifras, tablas y observaciones y conserva niveles, acciones y cobertura escritos. También se comprobó el texto extraído: sin elementos de la barra ni textos fuera de la página. No se enviaron hojas a una impresora física.

La lectura inicial de Safari fue bloqueada por la revisión automática por posible exposición de otras pestañas. Andy autorizó expresamente ver la pestaña local y el diálogo de impresión; se completó la revisión y se cerraron las pestañas temporales creadas para ello. No quedó bloqueada la comprobación PDF.

## Checklist

- ✅ `./mvnw compile test`: 710 aprobadas, 0 fallos/errores/omitidas.
- ✅ Diseño de indicadores y hoja comparado con el mock; capturas en `docs/evidencia_paso12/`.
- ✅ 390 / 810 / 1080 / 1366 / 1920 px sin desplazamiento lateral en las tres vistas revisadas.
- ✅ Barra en una sola fila desde 810 px; controles medidos ≥44 px en celular.
- ❌ Prueba física con el dedo en celular/iPad: no realizada.
- ✅ Usuario consulta sus calificaciones; otra empresa denegada. No genera informe propio ni ajeno por dirección directa.
- ✅ Textos con tildes, fechas españolas y niveles siempre escritos.
- ✅ Consola sin errores ni advertencias en el recorrido válido; 400/403 de pruebas negativas esperados.
- ✅ PDF real guardado desde Safari y revisado visualmente en sus siete páginas en blanco y negro.
- ❌ Impresión física en papel: no realizada; se verificó el PDF del diálogo nativo.
- ✅ Sin fotos privadas, contraseñas reales ni `config/db.properties` versionados; evidencia solo ficticia.
- ❌ Compatibilidad completa en las dos últimas versiones de Chrome, Edge y Firefox: pendiente del Paso 13.

## Lo que NO se hizo

Nada funcional del Paso 12 quedó a medias. No se empezó el Paso 13 ni el 14. No se generaron fichas por evaluación (visión futura), no se publicaron informes al Usuario ni se añadieron bibliotecas PDF, almacenamiento de PDFs en el servidor o cambios de esquema. Las comprobaciones físicas y la compatibilidad completa siguen pendientes como indica el checklist.

README conserva la portada de Claude: únicamente cambian la fila del Paso 12 y el número de pruebas. INSTALAR.md solo actualiza ese número para que su comprobación coincida; no se alteran scripts, dependencias, puertos, cuenta inicial ni procedimientos de instalación.

## Cómo probarlo tú

1. Mantén MySQL encendido. Desde la raíz del proyecto ejecuta `./mvnw compile` y `./mvnw test` (Windows: `mvnw.cmd compile` y `mvnw.cmd test`). Deben aprobar 710 pruebas. No ejecutes el SQL inicial sobre tu base existente.
2. Abre `http://localhost:8081` e ingresa con tu cuenta habitual. Empresas → abre una empresa → **Ver calificaciones**.
3. Comprueba el promedio de cada sede y el general. Siempre debe aparecer cuántas sedes están calificadas y cuáles faltan. Una sola sede no tiene general; con varias sedes, hacen falta al menos dos calificadas.
4. Como Administrador, pulsa **Generar informe**. Sin visitas Finalizadas aparece un aviso; con al menos una, se abre la hoja con los datos y estadísticas por sede, detalle de puestos, pendientes y sugerencias.
5. Pulsa **Imprimir o guardar PDF**. Elige Guardar como PDF (en Safari, botón PDF) o tu impresora. Para repetir la muestra: papel A4, impresión en color desactivada; fondos y cabeceras automáticas desactivados. Comprueba niveles, acciones, tablas y cobertura en las páginas.
6. En una empresa de pruebas con una sede, busca al final «No hay calificación general: la empresa tiene una sola sede».
7. En pruebas, reevalúa un Rojo con resultado Verde y finaliza la visita: las cifras actuales deben contar Verde. La visita anterior conserva su promedio; los pendientes siguen Abiertos/Arreglados según su propio estado. Un nuevo Rojo En curso permanece fuera.
8. Como Usuario, abre las calificaciones de tu empresa: solo consulta, sin Generar informe ni fotos. Escribe `/calificaciones?empresa=ID_DE_OTRA_EMPRESA` o `/informes/empresa/ID` (propio o ajeno): Acceso denegado.
9. Revisa las tres vistas a 390, 810, 1080, 1366 y 1920 px. Las pruebas físicas y el recorrido completo de navegadores se harán cuando autorices el paso correspondiente.

## Git y entrega

- Base incorporada: `7670b93`, instalación y README de Claude.
- Commit del paso: **Paso 12: calificaciones de sedes e informe general imprimible**. El hash concreto, `git log --oneline 7670b93..HEAD` y la confirmación del push a `origin/main` se entregan en el mensaje de cierre después de verificarlos.
- AGENTS.md actualizado: Pasos 0–11 aprobados, Paso 12 implementado y pendiente del OK de Andy. **Esperar su OK antes del Paso 13.**
