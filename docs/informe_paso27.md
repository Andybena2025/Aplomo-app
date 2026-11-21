# Paso 27 · ALINEO

Estado: 27A implementado y verificado. Los bloques 27B (bandeja y solicitudes de visita) y 27C (solicitudes de acceso)
se entregan en commits separados. No se presentan como terminados en este informe de A.

## 27A · Cambios y comprobaciones

- Sistema compartido: títulos champán, estados diferenciados, riesgo en calma, filas completas,
  filtros en vivo, fechas colombianas, alertas junto al campo y confirmaciones de cuatro segundos.
- Inicio distinto por rol; Administrador sin reservas, inicio de visitas ni práctica. Evaluador con
  continuación por puesto, progreso y práctica aislada. Contadores enlazados a sus consultas.
- Empresa creada por Evaluador: él es el dueño; otros asignados y el Administrador solo consultan.
  Solo el Administrador transfiere un dueño Inactivo a un Evaluador Activo, con bitácora.
  Sector opcional, ciudad registrada, edición por el dueño y migración repetible.
- Evaluación con cabecera de enfoque, cinco fases, barra fija, una parte corporal visible y resultado
  compacto. Se reutilizan las fotos, puntos, imágenes, propuestas y puntajes del servidor.
- Archivo, empresas, sedes, usuarios, agenda mensual de seis semanas, cuenta por campo y ayuda por rol.
  PDF general conserva sus promedios; valida y señala la visita actual sin incluir En curso en ellos.
- Demo con NIT, sectores, lugares y nombres presentables; conserva claves, estados e intentos del equipo.
  La carga repetida no duplica. Un NIT coincidente sin la marca de demo cancela antes de modificar nada.

Decisiones de propiedad y sector: respuestas expresas de Andy del 21/11/2026. No se dedujo el sector
ni se cambió el dueño desde un campo editable del navegador.

## Requisitos y prueba

| Requisito | Archivo / punto de cumplimiento | Comprobación |
| --- | --- | --- |
| RF-001c, RF-003 | `CuentaWeb`, `CuentaPropia.guardarCampo`, `PaginasUsuarios` | `CuentaCampoTest`, `HttpFunciones27Test`, `HttpPaso26Test` |
| RF-002 | `PropiedadEmpresa.exigir`, `Empresas.transferir`, servicios de reservas/visitas/práctica | `PropiedadEmpresaTest`, `HttpPropiedadEmpresaTest`, `HttpFunciones27Test` |
| RF-004–010, RF-028 | `Empresas`, `EmpresaDao`, `PaginasEmpresas`; migración de dueño/sector | Propiedad, `EmpresasTest`, `HttpEmpresasTest`, historial de inactivos |
| RF-011–013, RF-033 | `InicioWeb`, `Reservas.horasLibres`, `Calendario`, `tiempo-colombia.js` | `ReservasTest`, `HttpPaso26Test`, `qa_tiempo_colombia.cjs`; recorrido real de fecha/hora |
| RF-014–021 | `ConversacionEvaluacion`, `conversacion-evaluacion.js`, `revision-visual.js` | Pruebas HTTP de recorrido, revisión, fotos y análisis; dos posturas completas en navegador |
| RF-022–024 | `ListasConsulta`, `FiltrosLista`, `SistemaVisual`, Archivo | `HttpConsultasTest`, `HttpHistorialInactivosTest`, `HttpPendientesTest`, `HttpSugerenciasTest` |
| RF-025–027 | `CalificacionesWeb`, `PaginasCalificaciones`, `CentroAyuda` | `CalificacionesTest`, `HttpInformesTest`, `HttpAyudaTest`; contexto de informe |
| RNF-002, RNF-004, RNF-007 | CSS compartido, cabecera, filtros, alertas y ayuda | Auditoría GET por tres roles, mediciones DOM y formularios con errores conservados |
| RNF-003 | listas, guardado e informe con 1000 registros | 20 repeticiones por acción: P95 561,32 / 16,42 / 194,54 ms; límite 5000 ms |
| RNF-006, RNF-009 | alcance SQL, fotos privadas e historial | Pruebas de direcciones ajenas, Usuario sin fotos ni resultados En curso, desactivación sin borrar |
| RNF-010, RNF-011 | motor/bloque protegidos, formato y métodos de servicio | Comparación binaria de protegidos y `LongitudFuentesTest` |

## Números de QA

Suite A: 1057 casos Maven. Primera ejecución completa: 959 aprobados y 98 incidencias; se corrigieron
los fallos reales y se adaptaron expectativas de HTML/permisos retirados. Los 98 casos se comprobaron
con reintentos dirigidos, sin repetir la suite completa. JavaScript: suite inicial de 128 aprobadas y seis casos nuevos de filtros verificados después: 134 en total.
Cierre dirigido: 27 casos de páginas, propiedad, demo y formato; 26 funcionales aprobados y formato corregido/reintentado. Otros ocho casos de Inicio y permisos aprobados tras su ajuste de textos.

| Pantallas / rol | 360 | 390 | 820 | 1180 | 1366 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Login | 0 | 0 | 0 | 0 | 0 |
| Inicio, reservas, visitas, pendientes, archivo, empresas, cuenta, ayuda · E/U | 0 | 0 | 0 | 0 | 0 |
| Inicio y listas administrativas · A | 0 | 0 | 0 | 0 | 0 |
| Evaluación · Postura y Resultado: desborde horizontal | 0 | 0 | 0 | 0 | 0 |

Cifras en píxeles de desborde horizontal. Se hicieron 205 mediciones de páginas; el error inicial de
Inicio a 820 px (2 px) se corrigió y volvió a medir. La lista Usuarios requirió corregir un color nulo;
su medición final se repitió sobre la respuesta correcta. Otras 52 medidas finales, incluyendo 1024 px, dieron 0 px; filtros abiertos a 390 px y selección real de riesgo comprobados. No se tomaron capturas.

Resultado con tres sugerencias visibles: desplazamiento vertical **0 px** en 1366×768, 1440×900,
820×1180 y 1180×820. En celular conserva desplazamiento vertical normal; no se promete que quepa
sin bajar. La barra permanece fija. Dos posturas guardadas en el ensayo: puntajes oficiales 7 y 6,
con máximo 7; datos de QA revertidos por ROLLBACK. MediaPipe real funcionó en los dos ejemplos;
no se cambió su código. Calendario: 42 días / seis semanas y 0 px de desplazamiento vertical en las
ventanas de tableta y computador comprobadas. Cero errores de consola capturados en el recorrido.

## Auditoría de rutas

Se revisan 59 familias de páginas GET, sus formularios y variantes por rol. Además de las rutas
nombradas en el encargo: `/sedes`, `/salones`, `/areas`, `/cargos`, `/puestos`, sus detalles y altas/ediciones;
`/evaluaciones`, `/sugerencias`, sus detalles/ediciones y creación manual; `/visitas/{id}/finalizar`;
`/evaluaciones/{id}/analizar`, `/evaluaciones/{id}/analisis/{id}`; `/cuenta/contrasena`, `/cuenta/clave`,
`/cuenta/nombre`, `/cuenta/telefono`, `/cuenta/cargo`; práctica, ayuda pública y errores 403/404/500.
Recursos binarios, JSON de notas y fragmentos de catálogos no se cuentan como páginas rediseñadas.
`Inicio` es la ruta existente `/`, no se creó otra dirección `/inicio`.

## Lo que no se hizo

27B y 27C siguen pendientes de sus propios commits. No se cambió el motor, el bloque de cálculo,
la IA ni ninguna imagen. Sin ficha PDF individual, hosting, Spring Boot, logo nuevo, Notion o diagramas.
No se probó iPad físico, Windows ni tres equipos en la misma red; no se descargaron navegadores.
La cuenta inicial se conserva: la pregunta sobre desactivarla no está resuelta y su clave no se cambia.

## Checklist

- ✅ 1057 casos Maven verificados y 134 JS aprobados; suite completa una sola vez, reintentos dirigidos.
- ✅ Nuevo sistema visual y mediciones DOM; sin capturas por instrucción de Andy.
- ✅ Direcciones manuales y límites de empresa/rol comprobados en el servidor.
- ✅ Consentimiento, puntos, ajustes y esfuerzo se confirman en el recorrido; sin nuevos puntajes en JavaScript.
- ✅ Español, escape HTML, CSRF y conservación de campos al fallar.
- ✅ Archivos privados y fotos fuera del conjunto de archivos a versionar.
- ❌ Comprobación física con dedo / iPad, Windows y tres equipos; pendiente.

## Cómo probar A

1. Inicia sesión con cada rol: sus acciones de Inicio deben ser diferentes.
2. Como Evaluador, crea una empresa, su sector y un puesto; la empresa queda asignada a tu cuenta.
3. Intenta editar esa empresa como Administrador u otro Evaluador asignado: debe denegarlo.
4. Solo si el dueño está Inactivo, el Administrador ve la transferencia a otro Evaluador activo.
5. En Reservas prueba la fecha DD/MM/AAAA, el salto y una hora libre; los campos conservan sus datos si hay cruce.
6. Recorre dos posturas desde una visita En curso; revisa el Resultado, abre su desglose y Exportar PDF, y guarda.
7. Abre Archivo, Mi cuenta y Ayuda. Usuario solo consulta sus empresas y resultados Finalizados, sin fotos.

Commit y push: se anotan al cerrar cada bloque; no confundir esta copia de trabajo con la app que corre en 8080.


## Base y demo aplicadas

Respaldo privado creado antes de migrar. Migración ejecutada dos veces en la base actual: 22 tablas,
conteos idénticos antes/después; agrega dueño y sector sin borrar registros. La carga de demo también
se ejecutó dos veces dentro de una transacción: primera y segunda con los mismos conteos. Se conservó
el hash de cada contraseña existente del equipo y de la cuenta inicial.

Empresas 8→8; cuentas 13→13; sedes 14→14; salones 31→31; puestos 47→47; reservas 72→74 (dos respaldos
del viernes); visitas 45→45; evaluaciones 66→66; fotos 66→66; análisis 66→66. Ningún dato de ensayo
«Empresa de prueba sin confirmar» en la base real: 0. No se confirmó ningún ensayo de QA.

| Evaluador | Citas pasadas | Citas desde 21/11/2026 | Canceladas |
| --- | ---: | ---: | ---: |
| Ing. Andrés Benavides | 22 | 9 | 0 |
| Ing. Johan Zapata | 12 | 8 | 1 |
| Ing. Laura Gabriella Romero | 8 | 9 | 0 |

Nueve cuentas del equipo activas más la inicial conservada: 10. Las tres cuentas antiguas
`companero1–3` quedan Inactivas; solo `companero2` cambió de Activa a Inactiva. Las otras cuatro genéricas
no existían y no se crean. Avisos de ejemplo nuevos: pendientes de 27B. No se envió correo ni WhatsApp.


## 27B · Avisos y solicitudes de visita

Implementado: responsable por cita, varias personas evaluadoras en una empresa, bandeja privada en los
3 roles y solicitud del Usuario con alternativas ante cruce. La nota del Administrador conserva su campo
anterior y genera un mensaje; también se importaron las notas anteriores sin reiniciar el acuse.
Se eligió la bitácora para enlazar la respuesta única a su petición, evitando un campo no solicitado.

| Requisito (propuesta aprobada) | Archivo / método | Prueba |
| --- | --- | --- |
| RF-002: asignaciones múltiples | CuentaEmpresaDao.validar; migracion_paso27.sql | AvisosSolicitudesTest.aceptarAsignaEmpresaYSostieneVariosIngenieros |
| RF-011/033: cita y cruces por ingeniero | Reserva.evaluador; CrucesAgendaDao.comprobar; Agenda.consultar | cruceEsPorIngenieroYNoPorEmpresa; reservaAnteriorRecibeIngenieroSinCambiarConteos |
| RF-002/033: bandeja privada y acuse | MensajeDao.exigir/entender; Avisos; AvisosWeb; notas.js | libreCreaReservaYMensajeSoloAlIngeniero; pedirInformacionPermiteSoloUnaRespuestaYAcusePrivado; HttpAvisos27Test |
| RF-002: nota privada real | Agenda.guardarNota/nota; AgendaDao; AgendaWeb | notasIgualesVuelvenAAvisarYLeerSinAcuseNoMarcaNada; otroAsignadoConsultaReservaSinNotaPeroNoPuedeEditarNiIniciar |
| RF-011: solicitud libre/ocupada | SolicitudesVisita.solicitar/libre; SolicitudesVisitaWeb.alternativas | HttpAvisos27Test.solicitarConCruceConservaCamposYDaLasDosAlternativas |
| RF-002/012: aceptar, llamada, información y rechazo | SolicitudesVisita.actuar; Avisos.responder | aceptarAsignaEmpresaYSostieneVariosIngenieros; rechazarConMotivoConservaLaReserva; pedirInformacionPermiteSoloUnaRespuestaYAcusePrivado |
| RNF-004/005: permisos, CSRF, escape y auditoría | Router; MensajeDao; Avisos; SolicitudesVisita | HttpAvisos27Test.usuarioAjenoEs403AntesDeCrearYNoPuedeDecidirLaSolicitud; todosLosRolesTienenBandejaYNoVenMensajesAjenos |
| RNF-002/007: responsive, texto y recuperación | aplomo.css; notas.js; SolicitudesVisitaWeb | 10 mediciones DOM + recorrido de solicitud/aceptación; qa_notas.cjs |

### Pruebas y migración

- Suite completa una vez: **1068 ejecutadas, 1055 aprobadas, 13 fallos, 0 errores y 0 omitidas**.
  Corregidos los fallos (expectativas antiguas y lectura de reserva ajena al responsable); reintento dirigido:
  **29/29**. Se añadieron cuatro casos más y comprobaciones finales: **1072 casos Maven distintos verificados**.
  El cierre del formulario también corrigió un color nulo legado; último reintento HTTP/longitud **5/5**.
- JavaScript: **136/136**, 0 fallos; los cinco casos de avisos se repitieron tras el último ajuste, **5/5**.
- Rendimiento de la suite, P95 lista/guardar/informe: **569,13 / 16,38 / 182,81 ms**.
- Migración de prueba dos veces, 23 tablas y conteos previos conservados. El caso de una reserva anterior
  sin ingeniero comprueba el relleno dos veces y conserva el número de reservas, con ROLLBACK.
- Migración real con respaldo privado: **22 → 23 tablas**, las 22 anteriores conservan cada conteo.
  **74 reservas: 69 con ingeniero y 5 sin asignación activa**, sin inventar responsables.
  **2 notas anteriores importadas**; segundo pase idéntico. Demo: **6 avisos nuevos (1 nuevo y 1 leído por E)**;
  total **8 mensajes**. Cargar dos veces no duplica ni cambia claves ni otros conteos.
- Citas pasadas/futuras/canceladas: Andrés **22/9/0**, Johan **12/8/1**, Gabriella **8/9/0**.
- QA de interfaz: bandeja y solicitud a **360/390/820/1180/1366 px: 0 px de desborde horizontal**.
  Usuario solicita → E recibe en bandeja → abre contacto → acepta → reserva Aceptada.
  **0 errores de consola**. Sin capturas ni descargas, por instrucción de Andy.
- El primer lanzador de QA omitió la URL de pruebas: la conexión salió sin confirmar al detectar la tabla
  ausente. Comprobación posterior en la base real: **0 empresas y 0 cuentas de prueba**. Se fijó la URL y
  se agregó un guardado de seguridad que exige rula_ergonomia_pruebas. El recorrido final se revirtió.

### Checklist y límites

- ✅ Casos Maven y JS verificados; código protegido, IA e imágenes conservados.
- ✅ Bandeja por destinatario; Usuario ajeno y CSRF rechazados por HTTP.
- ✅ Notas iguales vuelven a avisar; sondeo no lee ni prolonga sesión; pausa en pestaña oculta.
- ✅ Botón y hoja de avisos, formulario breve y ambos caminos de cruce revisados sin desborde.
- ✅ Sin claves/fotos privadas en el commit; respaldo fuera de Git.
- ❌ iPad/iPhone físico, Windows y tres equipos en red: no comprobados.
- ❌ 27C sigue pendiente en este punto; no hay aún solicitudes públicas de acceso.

### Cómo probarlo

1. Como Administrador, abre una cita con ingeniero, escribe una nota y verifica «Nota enviada a…».
2. Como ese Evaluador, abre Avisos: aparece la nota. Entendido la pasa a Leídos.
3. Como Usuario, pulsa Solicitar una visita, elige sede/salón/ingeniero, fecha y horas.
4. Si está ocupado, conserva los datos y ofrece Elegir otra hora / Elegir otro ingeniero.
5. Como E destinatario, abre la solicitud en Avisos: pide información, llama, acepta o rechaza con motivo.
6. Como Usuario, responde una sola vez a la petición y verifica el aviso de aceptación o rechazo.
7. Otro Evaluador asignado puede consultar la cita, pero no recibe su nota ni puede editarla o iniciarla.


## 27C · Acceso para empresas nuevas y cierre

La portada enlaza al formulario público. El envío crea solamente una solicitud Pendiente y un aviso
para cada Administrador activo. Nadie puede ingresar antes de aprobar. La Administradora puede llamar,
aprobar con ingeniero opcional o rechazar con motivo; no hay envíos automáticos por WhatsApp ni correo.
Al aprobar se reutilizan las reglas de Empresas y Usuarios dentro de la misma transacción; la clave
se muestra solo en esa respuesta, tiene 16 caracteres legibles y se guarda únicamente como bcrypt.
En el camino del Evaluador, crear empresa con persona de contacto pide cargo y ciudad y deja la solicitud
Pendiente; el dueño sigue siendo el Evaluador. Un correo empresarial sin persona de contacto conserva
el alta normal y no inventa una cuenta. Sin sedes/salones, Solicitar visita explica que debe prepararlos
el ingeniero. La ciudad declarada se usa como referencia aprobada hasta registrar las sedes reales.

| Requisito (propuesta aprobada) | Archivo / método | Prueba |
| --- | --- | --- |
| RF-003: solicitud pública sin registro libre | SolicitudesAccesoWeb.publica; SolicitudesAcceso.solicitar | HttpAcceso27Test.publicoValidoRecibeConfirmacionSinCrearUsuario |
| RNF-004: validación, CSRF, escape y límite | SolicitudesAcceso.Datos; LimiteSolicitudes; SolicitudesAccesoWeb | invalidoConservaDatosYCsrfEsObligatorio; sextoEnvioDevuelve429SinCrearOtraSolicitud; limiteAdmiteCincoPorHoraYSeLiberaSinEsperar |
| RF-002/003: solo A decide | SolicitudesAcceso.administrador/aprobar/rechazar | soloAdministradorPuedeRevisarYDecidirTambienPorDireccionDirecta; usuarioYEvaluadorNoConsultanNiApruebanNiRechazan |
| RF-001/003: aprobación atómica y contraseña temporal | Usuarios.crearEn; Empresas.guardarEn; SolicitudesAcceso.aprobar | correoDuplicadoRevierteEmpresaYResolucion; aprobarMuestraClaveSoloEnEsaRespuestaYPrimerIngresoExigeCambio |
| RF-004: sector y ciudad sin sedes inventadas | solicitud_acceso; Empresas.resumen | aprobarCreaEmpresaUsuarioTemporalSectorYCiudadSinInventarSedes |
| RF-003: contacto de dueño E pendiente | SolicitudesAcceso.guardarEmpresa; EmpresasWeb; PaginasEmpresas | contactoDelDuenoQuedaPendienteSinPoderIngresar; empresaNuevaDelEvaluadorEnviaContactoPendienteSinDarAcceso; correoDeEmpresaSinContactoNoCreaSolicitudDeCuenta |
| RF-003/005: rechazo conserva historial | SolicitudAccesoDao.resolver; SolicitudesAcceso.rechazar/auditar | rechazarNoCreaNadaYExigeMotivo |
| Decisión de Andy sobre la cuenta inicial | DemoRoles.desactivarInicial; Demo.cargar; DemoAvisos.cargar | DemoTest.demoDesactivaInicialSoloConTresAdminsActivosYConservaSuClave |

### Comprobaciones finales

- Suite completa final de C: **1088 ejecutadas / 1088 aprobadas / 0 fallos / 0 errores / 0 omitidas**.
- Andy resolvió después la cuenta inicial. Se agregó un caso de demo y uno de correo empresarial:
  **1090 casos Maven distintos verificados**, con reejecución de los afectados. Cierre de demo **12/12**;
  cierre de acceso/privacidad/longitud **22/22**. La prueba detectó y corrigió la duplicación de avisos al
  cambiar el Administrador registrador; la identidad de la semilla depende de destino y texto, no de quién carga.
- JavaScript: **136 ejecutadas / 136 aprobadas / 0 fallos**. Nuevo archivo de copia revisado con node --check.
- P95 lista/guardar/informe: **537,20 / 16,32 / 172,85 ms**.
- Migración C real con respaldo privado, dos pasadas: **23 → 24 tablas**; las 23 anteriores conservan sus
  conteos, **solicitud_acceso = 0** en la real. Segundo pase idéntico. Los ocho mensajes anteriores se conservan;
  no se importaron ocho notas nuevas. Continúan 74 reservas, 69 con ingeniero y cinco sin asignación activa.
- Decisión final de Andy aplicada: primero se verificaron **3/3 Administradores del equipo Activos**.
  `admin@rula.local` pasó a Inactiva, con una entrada de bitácora y **contraseña conservada**.
  **9 cuentas Activas**, 13 filas de cuenta conservadas. Demo cargada dos veces: segundo pase idéntico,
  8 mensajes, sin reactivar la cuenta inicial ni cambiar las claves existentes.
- Instalación nueva: el respaldo inicial sigue creándose; cargar_demo lo deja Inactivo cuando los tres
  Administradores del equipo están Activos. Repetir la carga puede usar el equipo, sin reactivar el respaldo.
- QA real del navegador integrado, solo base de pruebas y ROLLBACK: solicitud pública → aviso del Admin →
  aprobación con ingeniero → clave/copia/WhatsApp presentes → nueva consulta sin clave. **0 errores de consola**.
- Solicitar acceso, revisar y aprobación a **360/390/820/1180/1366**: **0 px** de desborde horizontal en las
  quince medidas. Login a 360×740, 390×844, 430×932, 820×1180, 1366×768: **0 px**, Entrar visible en las cinco.
  Sin capturas, evidencia nueva ni descargas, por instrucción de Andy.

### Checklist y lo que no se hizo

- ✅ Tres bloques implementados; suite completa de C en verde y cambios posteriores comprobados por casos afectados.
- ✅ Pruebas de permisos escribiendo la dirección a mano; CSRF, límite y respuesta única.
- ✅ Español y estados Aprobada/Rechazada; nueve cuentas activas, sin cambio de contraseñas.
- ✅ Script, migración, Diccionario TXT/PDF, ENDPOINTS, requisitos marcados propuesta, CUENTAS e instalación.
- ✅ Motor RULA, bloque protegido de cálculo, deteccion.js e imágenes idénticos a antes del Paso 27.
- ✅ Ninguna clave privada, fotos ni respaldo entran al commit. No se borraron datos.
- ❌ No se certifican iPad/iPhone físico, Windows ni tres equipos en la misma red. No se descargaron navegadores.
- ❌ No se enviaron mensajes por WhatsApp/correo; el enlace requiere revisión y envío manual.
- Límite público simple: cinco envíos por conexión/hora en memoria; tras un proxy pueden compartir dirección.
  No se agregaron servicios externos ni nuevos mecanismos de entrega. Resto funcional del encargo: nada pendiente.

### Cómo probarlo

1. En el login abre Solicitar acceso y envía datos de una empresa de práctica.
2. Como Administrador abre Avisos → Revisar solicitud, o el contador de Inicio.
3. Aprueba con ingeniero opcional; copia la clave temporal en ese momento. No reaparece al consultar de nuevo.
4. La persona inicia sesión y debe cambiar la clave; el ingeniero completa sedes, salones y puestos.
5. Como Evaluador crea una empresa con contacto, correo, cargo y ciudad: el contacto queda Pendiente y no entra.
6. Intenta abrir solicitudes-acceso como Evaluador o Usuario: Acceso denegado.
7. En Usuarios verifica las nueve cuentas del equipo Activas y admin@rula.local Inactiva. Cargar demo no la reactiva.

Commits de entrega: 27A `9c0dcfb`; 27B `36ed47c`; 27C «Paso 27C: solicitud de acceso para empresas nuevas».
Los hashes y confirmación del último push se entregan en la respuesta final; `git log -3 --oneline` los muestra.
