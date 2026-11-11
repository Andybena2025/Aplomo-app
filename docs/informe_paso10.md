# Informe del Paso 10 · Sugerencias

11 nov 2026 · America/Bogota. Alcance: las tres correcciones solicitadas por Andy y el Paso 10 de `docs/06_plan_de_implementacion.md`. No se empezó el Paso 11.

## Qué se hizo

Las reglas y textos de sugerencias se leen de MySQL. Se muestran antes de guardar, dentro del resultado del asistente, con el componente `.sug` del mock; al guardar el análisis calculado por el servidor se proponen y guardan las que aplican. El Administrador puede escribir, editar, quitar sin borrar y cambiar el estado: Pendiente, En proceso o Hecha. Cada acción conserva fecha, hora y responsable en la bitácora. El Usuario consulta únicamente sugerencias activas de su empresa y de visitas finalizadas, sin fotos ni controles de gestión.

Se accede desde **Pendientes → Sugerencias**, desde el detalle de una evaluación con análisis y desde el resultado guardado. Se conserva la barra superior de seis secciones para que no se recorte ni se agregue desplazamiento lateral. La lista de una evaluación permite escribir una sugerencia para ese puesto.

Al estar Hechas todas las sugerencias activas de un puesto, se ofrece **Registrar reevaluación**. Abre el formulario existente de reservas con empresa, sede, salón y categoría Reevaluación preparados. El Administrador completa fecha y horario acordados y guarda, tal como el actor registra la reserva en CU5-26/RF-011. No se asignan fecha u horario ni se crea una reserva automáticamente. Se comprobó en pantalla el recorrido hasta una reserva de Reevaluación guardada.

## Las tres correcciones previas al Paso 10

Se compilaron, pasaron las 598 pruebas existentes y se guardaron primero en el commit `6dc93c2`.

1. **Etiquetas de riesgo:** Pendientes (lista y detalle) ahora obtiene color y nivel oficial de `nivel_riesgo` en MySQL y muestra «7 · Rojo · Nivel 4». También se completó el último resultado de los puestos en el salón y el puntaje de cada evaluación en la visita. Resultado, historial de análisis y vista previa ya mostraban el formato completo y se revisaron. El nuevo módulo Sugerencias usa el mismo formato. Se añadió `Html.riesgo` para reutilizar la presentación, sin calcular RULA ni fijar umbrales en la interfaz.
2. **Navegación:** la regla `.nav{flex-wrap:wrap;overflow-x:visible}` quedó dentro de `@media(max-width:720px)`. Se redujeron espacios en la barra de tableta y se ocultan el nombre/rol y la palabra Atrás a esos anchos para que los enlaces queden visibles. Las capturas y mediciones confirman una sola fila en 810, 1080, 1366 y 1920 px; a 390 px se adapta a varias filas. No hay contenido del menú oculto lateralmente.
3. **Etiqueta de Pendientes:** se reemplazó el lema por **Seguimiento**, usando `.eyebrow`, como las etiquetas contextuales de otras secciones del mock.

Inicio conserva sus cuatro tarjetas: Visitas en curso, Reservas en cola, Evaluaciones y Puestos en rojo. Sus números son **cantidades de registros**, no puntajes RULA, por lo que no se les asigna un nivel de riesgo. El número de Puestos en rojo conserva su color rojo. Las calificaciones promedio de visita siguen siendo indicadores numéricos; su desarrollo corresponde al Paso 12.

Capturas previas: `{390,810,1080,1366,1920}-correcciones-pendientes.jpg`, `correcciones-visita.jpg`, `correcciones-resultado.jpg` y `mediciones-correcciones.json` en `docs/evidencia_paso10/`.

## Reglas que respondió Andy el 11 nov 2026

Se implementan sin cambiar el esquema:

- Automáticas: estado **PENDIENTE**, fecha límite vacía y origen **SISTEMA**.
- Prioridad inicial **(propuesta)**, fácil de cambiar en `Sugerencias.prioridad`: **ALTA** para Rojo; **MEDIA** para Amarillo o Verde. Cambiar después el análisis no sobrescribe la prioridad de una sugerencia ya existente.
- Tipo fijo en `Sugerencias.TIPOS_POR_CONDICION`, nunca inferido del texto: **MOBILIARIO** para escritorio, pantalla, pies colgando, apoyapiés y las dos condiciones de silla; **POSTURA** para reglas que solo dependen del puntaje del segmento, identificadas con `SIEMPRE`.
- Título: inicio de `texto_sugerido`, máximo 120 caracteres, cortado en una palabra completa.
- Amarillo/Rojo sin sugerencias: el análisis se guarda y aparece un aviso persistente para agregar al menos una. No se inventa una sugerencia de relleno ni se impide entregar el análisis. No se puede quitar la última sugerencia activa de esa evaluación.
- Repetir análisis de la misma evaluación agrega únicamente reglas nuevas. No duplica las ya propuestas ni altera sus ediciones, estado Hecha, prioridad o fecha. Las quitadas no reaparecen. Una sugerencia quitada conserva su fila, origen y bitácora; no se usa DELETE.

El catálogo inicial tiene **11 reglas (propuesta)**, basadas en las ideas de `sugerencias()` del mock y el caso de aceptación de docs/06. Incluye pies colgando + tronco ≥ 3 → reposapiés o bajar la silla. Los umbrales y textos están en `docs/base_de_datos/datos_sugerencias.sql`; el código no los duplica. La tabla real aprobada se llama `regla_sugerencia`: no se creó otra tabla por la mención antigua de `sugerencias_base` en RF-023a.

## Requisitos → implementación → comprobación

| Requisito | Archivo y método | Prueba/evidencia |
|---|---|---|
| RF-023a: reglas desde MySQL y condiciones RF-015 | `dao/SugerenciaDao.reglas`; `servicio/Sugerencias.aplicables/proponer/previsualizar`; `docs/base_de_datos/datos_sugerencias.sql` | `SugerenciasTest.piesColgandoYTroncoTresProponenReposapies`, `noAplicaReposapiesSiTroncoMenorQueTres`, `noSugiereMobiliarioCuandoCondicionFalsa`, `tiposFijosNoSeAdivinanDelTextoYReglasSonIdempotentes` |
| RF-023a: mostrar antes de guardar y persistir propuestas | `servicio/Analisis.guardarEnTransaccion`; `web/EvaluacionesWeb.atender`; `web/PaginasSugerencias.resumen`; `static/js/analisis.js` | `HttpSugerenciasTest.vistaPreviaIncluyeReglaDentroDelCalculoSinInsertar`; `resultado-sugerencias-vista-previa.jpg`; lista/detalle con origen Sistema |
| RF-023a: entregar análisis si no hay sugerencias | `Sugerencias.proponer`; `PaginasSugerencias.avisoMinimo/lista/resumen` | `verdePermiteQuitarYAnalisisSeGuardaSinSugerencias`; `sinReglasAplicablesGuardaYExplicaQueFaltaUna`; `sin-reglas-analisis-conservado.jpg` |
| RF-023b: crear y editar todos los campos | `servicio/Sugerencias.guardar`; `dao/SugerenciaDao.crear/editar`; `web/SugerenciasWeb.atender`; `PaginasSugerencias.formulario` | `HttpSugerenciasTest.crearEditarYOrigenProtegidoConHtmlEscapado`; `SugerenciasTest.validaSinModificar` (6 ejecuciones); `sugerencia-manual.jpg`, `sugerencia-guardada.jpg` |
| RF-023b: quitar sin borrar, mínimo una y conservar al reanalizar | `Sugerencias.quitar/proponer`; `SugerenciaDao.quitar/actuales`; `modelo/Sugerencia` | `repetirAnalisisConservaEdicionHechaYQuitadaSinDuplicar`; `ultimoAmarilloNoSeQuitaYEstadoNoDuplicaAuditoria`; `nuevasReglasSeAgreganSinAlterarLasAnteriores`; `ultima-sugerencia-protegida.jpg`, `sugerencia-quitada-con-historial.jpg` |
| RF-023b: estados y Reevaluación con RF-011 | `Sugerencias.estado/todasHechas`; `SugerenciaDao.estado/todasHechas`; `PaginasSugerencias.detalle/reevaluacion`; formulario existente de Reservas | `estadoCsrfYMetodosProtegidos`; `hechasOfreceReservaPreparadaSinCrearlaAutomaticamente`; `hechas-ofrece-reevaluacion.jpg`, `reevaluacion-registrada.jpg` |
| RF-020 / docs/07: etiquetas completas y correcciones visuales | `web/Html.riesgo`; `dao/PendienteDao`, `PuestoDao`, `VisitaDao`; `PaginasPendientes`, `PaginasEmpresas`, `PaginasVisitas`; CSS | 598 pruebas antes de iniciar el Paso 10; `vistaPreviaIncluyeReglaDentroDelCalculoSinInsertar`; capturas de correcciones y resultado previo «5 · Rojo · Nivel 3» |
| RF-002 y RNF-004/006: permisos servidor, SQL y privacidad | `Sugerencias.visible/admin/consultar/listar`; `SugerenciaDao.contexto/listar`; `Router.atender` CSRF; `Html.escapar` | `usuarioSoloEmpresaFinalizadaYNuncaModifica`; `usuarioConsultaSinFotosYNoModificaPorDireccionDirecta`; `otraEmpresaYQuitadasNoSonVisiblesParaUsuario`; capturas Usuario con direcciones directas |
| RNF-005/009: auditoría y conservación | `Sugerencias.auditar/historial`, `SugerenciaDao.quitar`; transacción compartida con `Analisis` | `ultimoAmarilloNoSeQuitaYEstadoNoDuplicaAuditoria`; `falloDeAuditoriaRevierteSugerenciasAnalisisEvaluacionYFoto`; historial en capturas |
| RNF-007: español, validación y no perder campos | `Sugerencias.guardar/fecha`; `SugerenciasWeb.guardar`; `PaginasSugerencias.formulario`; `static/js/sugerencias.js` | `erroresConservanFormularioYUltimaSugerenciaNoSeQuita`; error de fecha en pantalla con texto y opciones conservados; botones Guardando… y reactivación al fallar |
| RNF-002 y reglas de AGENTS: cinco tamaños y componentes mock | `PaginasSugerencias`; `static/css/aplomo.css` | 15 observaciones y capturas finales de lista/detalle/editar; `mediciones-sugerencias.json` |
| RNF-011: capas y cálculo único | `modelo/Sugerencia`, `SugerenciaDao`, `Sugerencias`, `SugerenciasWeb`, `PaginasSugerencias`; `Evaluaciones.sugerencias` | Compilación y 623 pruebas. Sin cambios en MotorRula, PuntuadorSegmentos, MedidorAngulos o TablasRula, ni dependencias nuevas |

Rutas Java relativas a `src/main/java/co/aplomo/`; pruebas a `src/test/java/co/aplomo/`; estáticos a `src/main/resources/`.

## Comprobaciones y datos

- **623 pruebas ejecutadas y aprobadas**, 0 fallos, 0 errores, 0 omitidas. Son las 598 anteriores más **17 ejecuciones** de `SugerenciasTest` y **8** de `HttpSugerenciasTest`.
- Compilación y pruebas en la base `rula_ergonomia_pruebas`. El catálogo de pruebas se carga dentro de la transacción reversible de cada prueba; no se instala permanentemente allí.
- Las revisiones visuales también usaron `rula_ergonomia_pruebas`. Al detener cada servidor temporal se confirmó el ROLLBACK; se revirtieron cuentas, empresas, evaluaciones, sugerencias y la reserva de Reevaluación de prueba. Se retiraron solo las fotos temporales propias de la revisión.
- Consulta de solo lectura después del QA: **0** empresas llamadas «Empresa de prueba sin confirmar» en `rula_ergonomia` y **0** en `rula_ergonomia_pruebas`.
- **Instalación de configuración real, no de datos de QA:** el catálogo del Paso 10 añadió **11 reglas** en `rula_ergonomia` (0 → 11). Se repitió la carga y añadió **0** (11 → 11). Solo INSERT con NOT EXISTS; sin DDL, cambios de esquema, sobrescrituras ni borrados. No se ejecutó el SQL inicial destructivo.
- Los análisis anteriores no se modificaron ni recibieron sugerencias retroactivas; el Administrador puede escribirlas o añadir un nuevo análisis cuando la visita esté En curso.

## Pantallas, tamaños y límites

Se comparó el bloque de sugerencias de Resultado con `.sug` del mock; las pantallas de gestión reutilizan su barra, `.eyebrow`, `.card`, `.btn`, `.tablewrap`, tablas `.resp` y formularios. El mock no incluye una pantalla administrativa completa de Sugerencias; no se afirma una copia exacta de una pantalla inexistente.

| Ancho | Lista | Detalle | Edición | Barra |
|---|---|---|---|---|
| 390 px | Tarjetas por fila, textos y acciones accesibles | Una columna e historial legible | Campos y botón dentro del ancho | Varias filas, sin recorte lateral |
| 810 px | Tabla de seis columnas; botón sin partir palabras | Tarjetas e historial dentro del ancho | Campos legibles | Una sola fila, todos los enlaces visibles |
| 1080 px | Tabla completa | Acciones accesibles | Formulario con ancho controlado | Una sola fila |
| 1366 px | Tabla dentro del ancho del mock | Detalle y seguimiento | Formulario con ancho controlado | Una sola fila |
| 1920 px | Contenido limitado al ancho del mock | Márgenes y tarjetas consistentes | Formulario con ancho controlado | Una sola fila |

Se corrigió durante QA el corte de palabras del botón Ver sugerencia en 810 px y se repitieron las capturas. En las 15 observaciones finales el documento no excede el ancho pedido y el menú no tiene contenido lateral oculto. Los botones medidos a 390/810/1080 px tienen al menos 44 px de alto. Capturas `{ancho}-sugerencias-lista.jpg`, `{ancho}-sugerencia-detalle.jpg`, `{ancho}-sugerencia-editar.jpg` y sus JSON. Se guardó también evidencia funcional de edición/error, creación manual, estado, quitar, mínimo una, resultado previo, Reevaluación y permisos.

## Checklist

- ✅ `mvn compile test`: 623 aprobadas, 0 fallos/errores/omitidas.
- ✅ Componentes y bloque de sugerencias comparados con el mock; capturas en `docs/evidencia_paso10/`.
- ✅ 390 / 810 / 1080 / 1366 / 1920 px sin desplazamiento lateral en lista, detalle y edición.
- ✅ Barra en una sola fila a 810 / 1080 / 1366 / 1920 px.
- ✅ Botones medidos de al menos 44 px en celular y tableta hasta 1080 px.
- ❌ Prueba física con el dedo en celular/iPad: no realizada. La comprobación de tamaños en navegador no sustituye esa prueba.
- ✅ Usuario no crea, edita, quita ni cambia estados; permisos HTTP y prueba en pantalla escribiendo direcciones directas. Otra empresa denegada.
- ✅ Textos con tildes y fechas «30 nov 2026». El campo nativo de fecha usa el formato del navegador.
- ✅ Consola sin errores ni advertencias en el recorrido final válido. Los errores 400/403 de las pruebas negativas son intencionales.
- ✅ Sin `config/db.properties`, contraseñas reales ni fotos versionadas. Las capturas solo muestran datos ficticios revertidos.
- ❌ Recorrido completo en las dos últimas versiones de Chrome, Edge y Firefox: pendiente del Paso 13.

## Lo que NO se hizo

No se empezó el Paso 11 (consultas generales), 12 (calificaciones e informe), 13 (compatibilidad completa) ni 14 (IA). No se cambió el esquema, las tablas RULA, la tolerancia NEUTRO = 5° ni el cálculo del servidor. No se borró información ni se reactivan automáticamente sugerencias quitadas. Las mejoras se ejecutan fuera del sistema y el Administrador registra su avance. No quedan funciones del Paso 10 a medias; se conservan los límites de comprobación física y de navegadores indicados arriba.

## Cómo probarlo tú

1. Con MySQL encendido, ejecuta `mvn compile` y `mvn test`: deben aprobar 623 pruebas. Usa la conexión privada existente; no ejecutes el script SQL inicial.
2. Abre `http://localhost:8081` e ingresa con tu cuenta habitual. **Pendientes → Sugerencias** abre la lista. Las cuentas ficticias de las capturas ya no existen, pues se revirtieron.
3. En una instancia de pruebas, crea una evaluación desde una visita En curso, marca Pies colgando y usa una medición cuyo tronco tenga ≥ 3 puntos. El Resultado previo debe proponer reposapiés o bajar la silla. Revisa «puntaje · color · Nivel» y guarda.
4. Abre la evaluación → **Ver y gestionar sugerencias**. La propuesta tendrá origen Sistema, estado Pendiente, fecha vacía y prioridad Alta si el resultado fue Rojo; Media si fue Amarillo o Verde. Las reglas no coincidentes no deben aparecer.
5. Entra en una sugerencia y pulsa **Editar sugerencia**. Cambia título, descripción, tipo, prioridad y fecha; guarda y revisa la bitácora. Prueba una fecha inválida y comprueba que no pierdes los campos.
6. Cambia el estado a En proceso y luego Hecha. Escribe una sugerencia manual y comprueba su origen Administrador. Quita una: desaparece de la lista, conserva su fila e historial. En Amarillo/Rojo, quitar la última activa debe pedir agregar otra.
7. Añade otro análisis de la misma evaluación mientras la visita siga En curso. Las sugerencias ya editadas, Hechas o quitadas se mantienen; solo aparecen reglas nuevas que ahora apliquen. Un análisis sin reglas se guarda y pide completar al menos una si fue Amarillo/Rojo.
8. Cuando todas las sugerencias activas del puesto estén Hechas, abre una y pulsa **Registrar reevaluación**. Confirma empresa/sede/salón/categoría, completa fecha y horario acordados y guarda. Debe quedar una reserva de categoría Reevaluación, inicialmente Esperando.
9. Finaliza la visita. Como Usuario de esa empresa, consulta la sugerencia: sin fotos ni acciones de cambio. Añade `/editar` a su dirección: Acceso denegado. La dirección de otra empresa también se deniega.
10. Revisa las tres pantallas a 390, 810, 1080, 1366 y 1920 px; a partir de 810 px, comprueba que toda la barra esté en una sola fila.

Las acciones manuales crean datos de uso: usa tu instancia de pruebas si deseas mantener la real sin datos de demostración. La instalación local ya tiene el catálogo de configuración de 11 reglas; en otra instalación se carga únicamente `docs/base_de_datos/datos_sugerencias.sql` en la base elegida.

## Git y entrega

- `6dc93c2 Correcciones previas al Paso 10: riesgo, navegación y encabezado`.
- Commit del paso: **Paso 10: sugerencias del sistema y seguimiento de mejoras**. El identificador concreto y la confirmación del push a `origin/main` se entregan en el mensaje de cierre; se comprueba con `git log --oneline b6c4d92..HEAD`.
- Validación final: `docs/evidencia_paso10/verificacion-final.txt`.
- AGENTS.md actualizado. **Esperar el OK de Andy antes del Paso 11.**
