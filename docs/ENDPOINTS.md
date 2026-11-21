# Rutas HTTP de ALINEO.

Inventario leído del código al terminar los pasos 15-E y 16 (14 nov 2026). La app sirve HTML y formularios; no es una API REST. `App.main` registra un único contexto `createContext("/", new Router())`. `Router.atender` y los manejadores enlazados abajo distribuyen las rutas. Actualizado en los Pasos 23–25 para historial, tres roles, agenda y ayuda. No hay otra lista central de rutas.

## Arranque y administrador inicial

- Local: desde la raíz, `bash scripts/iniciar.sh` (Mac) o `scripts\iniciar.bat` (Windows), con la instalación de [INSTALAR.md](../INSTALAR.md). Entrada: http://localhost:8080.
- Docker Desktop: copiar `.env.ejemplo` a `.env`, escoger las claves locales y ejecutar `docker compose up --build -d`. Entrada: http://localhost:8080. Logs: `docker compose logs -f app`; apagar conservando datos: `docker compose down`. Ver [Con Docker](../INSTALAR.md#con-docker).

### Administrador inicial

El script original crea **admin@rula.local**, contraseña temporal **Cambiar2026**. Es la cuenta de superusuario de **ALINEO**, con rol Administrador; no es el administrador `root` de MySQL. Debe cambiar la contraseña al primer ingreso, en `/cuenta/contrasena`. Son datos semilla públicos: después del cambio se usa la contraseña nueva, no la temporal.

## Seguridad y convenciones

- **A** = Administrador vigente; **E** = Evaluador vigente en sus empresas asignadas; **U** = Usuario vigente en sus empresas asignadas. Los permisos se comprueban en el servidor y en los servicios. A administra todo; E realiza trabajo de evaluación dentro de su alcance; U solo consulta. Una cuenta temporal debe cambiar su contraseña antes de usar las rutas internas.
- U ve todas sus reservas, incluso Esperando, En cola y Canceladas. Los resultados (visitas, evaluaciones, puntajes, pendientes y sugerencias) solo se muestran si la visita está Finalizada. Una dirección escrita a mano no evita ese filtro. U nunca accede a `/fotos/{foto}`.
- Todo POST requiere el campo `csrf` de la sesión, incluido el login. Los formularios ordinarios usan `application/x-www-form-urlencoded`; las subidas usan `multipart/form-data`. No se debe enviar un puntaje calculado por el cliente: el servidor calcula lo guardado.
- Después de **5 intentos fallidos**, bloqueo de **15 minutos** (`Autenticacion`); cierre a los **30 minutos sin actividad** (`Sesiones`). La cookie `APLOMO_SESION` es HttpOnly, SameSite=Strict y Secure cuando la conexión es HTTPS. Cerrar sesión, cambiar contraseña, rol o estado invalida el acceso correspondiente.
- La aplicación **no borra registros**: desactiva, cancela o marca fotos reemplazadas y conserva historial. Los scripts de reinicio de demo requieren confirmación y administrador de MySQL; no son endpoints. `docker compose down -v` borra los volúmenes y NO equivale a apagar normalmente.
- `{id}`, `{empresa}`, `{sede}`, `{salon}`, `{evaluacion}`, `{analisis}`, `{foto}` y `{reserva}` son enteros positivos. En las tablas, **F** incluye siempre `csrf` en POST. Las relaciones padre se obtienen de la ruta y se verifican, no se confían al navegador.
- Éxito habitual: **200 HTML** para vistas y **303 Location** para formularios. Sin sesión: **303 /login** (foto privada: **403**). Datos inválidos: **400**; acceso/CSRF: **403**; inexistente: **404**; método no soportado: **405**; error de almacenamiento: **503**, sin detalles SQL. GET y POST son los únicos métodos globalmente admitidos; una combinación no implementada puede devolver 404 o 405 según su manejador. Cada fila indica su respuesta de éxito y excepciones específicas.
- `aviso`, `guardado=1`, `creado=1` y `contrasena=1` son mensajes de interfaz, no autorizaciones. No hay endpoint de borrado ni de consulta de contraseñas.

## Acceso y sesión

Fuente: [Router.atender](../src/main/java/co/aplomo/web/Router.java), [Autenticacion](../src/main/java/co/aplomo/servicio/Autenticacion.java), [Sesiones](../src/main/java/co/aplomo/seguridad/Sesiones.java).

| Método | Ruta | Para qué / quién | Parámetros o formulario | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/login` | Entrar; sin sesión | `inactividad=1` opcional | 200 HTML y cookie anónima; con sesión 303 a Inicio o cambio de contraseña | RF-001a, RNF-004/007 |
| POST | `/login` | Validar acceso; sin sesión | F: `correo`, `clave` | 303 a `/` o `/cuenta/contrasena`; 401 credenciales/bloqueo, 403 formulario vencido | RF-001a, RNF-004 |
| POST | `/logout` | Salir; A/E/U, también temporal | F | 303 `/login`, elimina sesión/cookie; GET devuelve 405 | RF-001b |
| GET | `/cuenta/contrasena` | Formulario; A/E/U, también temporal | Ninguno | 200 HTML | RF-001c |
| POST | `/cuenta/contrasena` | Cambiar clave propia; A/E/U | F: `actual`, `nueva`, `confirmacion` | 303 `/?contrasena=1`, renueva sesión; 400/503 conserva formulario sin clave | RF-001c, RNF-004 |
| GET | `/` | Inicio con conteos y atajos autorizados; A/E/U | `contrasena=1` opcional | 200 HTML; práctica y Evaluar ahora solo A | RF-002, RF-013a, RF-020 |
| GET | `/demo/evaluacion` | Explicar práctica sin escribir; A/E, modo demo activo | Ninguno | 200 HTML; demo deshabilitado: 404 | Valor agregado sin RF propio; RNF-002 |
| POST | `/demo/evaluacion` | Crear/reutilizar visita ficticia y abrir recorrido; A/E, modo demo | F: `tipo=OFICINA` o `PLANTA` | 303 `/evaluaciones/nueva?visita=…&puesto=…`; no borra intentos | Valor agregado; reutiliza RF-013a/014 |

El modo demo se controla con `-Daplomo.modoDemo=false` (activo por defecto). La práctica escribe solo bajo su empresa ficticia y lo anuncia en la página.

Atrás se omite en las pantallas principales del menú, Agenda/Mi agenda, Mis evaluaciones, Ayuda y cambio obligatorio de contraseña. Los detalles, formularios y cambio voluntario lo conservan. El botón interno `data-atras-evaluacion` se conserva.

## Consultas comunes

[ConsultasWeb.atender](../src/main/java/co/aplomo/web/ConsultasWeb.java) intercepta primero los GET de lista de [SeccionConsulta](../src/main/java/co/aplomo/modelo/SeccionConsulta.java). Devuelve **200 HTML**, tanto para A como U con su alcance, **RF-024 y RF-002**. Los manejadores de gestión conservan los POST y detalles. No confundir los filtros de consulta con los campos de escritura.

En las siguientes rutas se admiten `q` (hasta 120 caracteres), `orden` (clave de columna permitida), `direccion=asc|desc`, `estado` (enum propio o `TODOS`) y los filtros particulares de la tabla. También `visita` y `evaluacion` para acotar resultados relacionados, siempre verificando acceso. `desde` y `hasta` son fechas ISO `AAAA-MM-DD`; `puntaje` es 1–7 y `nivel` 1–4. Filtros ajenos dan 403. Identificadores, fechas, estados y orden inválidos dan 400.

| Método | Ruta | Filtros particulares | Datos / requisito adicional |
|---|---|---|---|
| GET | `/empresas` | `empresa`, `estado` | Empresas; RF-004 |
| GET | `/areas` | `empresa`, `area`, `estado` | Áreas; RF-007 |
| GET | `/cargos` | `empresa`, `cargo`, `estado` | Cargos; RF-007 |
| GET | `/sedes` | `empresa`, `sede`, `estado` | Sedes; RF-005 |
| GET | `/salones` | `empresa`, `sede`, `salon`, `estado` | Salones; RF-006 |
| GET | `/puestos` | `empresa`, `sede`, `salon`, `area`, `cargo`, `numero`, `puntaje`, `color`, `nivel`, `estado`, `puesto`, `registros` (A/E) | Puestos; RF-008 |
| GET | `/reservas` | `empresa`, `sede`, `salon`, `desde`, `hasta`, `estado` | Todas las propias para U; RF-011/012 |
| GET | `/visitas` | `empresa`, `sede`, `salon`, `desde`, `hasta`, `estado`, `registros` (A/E) | U solo Finalizadas; RF-013b |
| GET | `/evaluaciones` | `empresa`, `sede`, `salon`, `area`, `cargo`, `numero`, `desde`, `hasta`, `puntaje`, `color`, `nivel`, `estado`, `puesto`, `registros` (A/E) | U solo Finalizadas; RF-020/021 |
| GET | `/pendientes` | Los de Evaluaciones, estado `ABIERTO` (por defecto) o `ARREGLADO` | Orden adicional `prioridad`, `fecha`, `fecha_limite` (`limite` se admite como alias); RF-022 |
| GET | `/sugerencias` | Los de Evaluaciones, estado `PENDIENTE`, `EN_PROCESO`, `HECHA` | `evaluacion` abre lista contextual; RF-023b |

En `/evaluaciones`, `/puestos`, `/pendientes`, `/sugerencias` y `/visitas`, **solo A** dispone de `registros=TODOS|ACTIVOS|INACTIVOS`: Todos es el valor por defecto; los otros rótulos son «Solo activos» y «Solo inactivos». Una fila es inactiva si lo está su empresa, sede, salón o, cuando tiene uno, su puesto. No cambia el filtro de estado propio del registro. Un valor inválido da 400; U intentando usar `registros` recibe 403. `puesto` permite acotar por ID en Puestos/Evaluaciones/Pendientes/Sugerencias; se valida contra el catálogo autorizado. Los catálogos administrativos incluyen inactivos marcados, sin reactivarlos.

`estado=TODOS` incluye los estados visibles para la cuenta, no amplía sus permisos. Los estados completos se definen en `SeccionConsulta.estados()`; riesgo y colores se leen de MySQL. Usuarios tiene filtros de presentación en el navegador, descritos a continuación.

## Cuentas

Fuente: [UsuariosWeb.atender](../src/main/java/co/aplomo/web/UsuariosWeb.java). Todas requieren **A** (RF-002); la última cuenta Administrador activa está protegida.

| Método | Ruta | Para qué | Formulario / parámetros | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/usuarios` | Lista de cuentas | `q`, `rol`, `empresa` son controles de filtro local de `usuarios.js`; el servidor entrega solo cuentas autorizadas. `guardado=1`/`creado=1` muestra aviso | 200 HTML | RF-003/024 |
| GET | `/usuarios/nuevo` | Formulario de cuenta | Ninguno | 200 HTML | RF-003 |
| POST | `/usuarios` | Crear cuenta | F: `nombre`, `correo`, `telefono`, `cargo`, `rol=ADMINISTRADOR|USUARIO`, `empresa` para Usuario, `temporal` (clave) | 303 lista; 400/503 conserva campos | RF-003, RNF-004/007 |
| GET | `/usuarios/{id}/editar` | Formulario con datos existentes | ID | 200 HTML | RF-003 |
| POST | `/usuarios/{id}/editar` | Guardar datos / restablecer clave | Mismos campos; `temporal` vacía conserva clave | 303 lista o `/login` si cambia la propia; invalida sesiones de esa cuenta | RF-003, RF-002 |
| POST | `/usuarios/{id}/activar` | Reactivar cuenta | F | 303 lista o login propio | RF-003 |
| POST | `/usuarios/{id}/desactivar` | Desactivar conservando historial | F | 303 lista o login propio | RF-003, RNF-009 |

## Empresas, sedes, salones, áreas, cargos y puestos

Fuente: [EmpresasWeb.atender/guardar/detalle](../src/main/java/co/aplomo/web/EmpresasWeb.java). GET de detalle: **A/E/U**, empresa autorizada; escrituras y formularios: **A**. Las listas GET están arriba.

| Método | Ruta | Para qué | Formulario / parámetros | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/empresas/nueva` | Formulario empresa | Ninguno | 200 HTML | RF-004 |
| POST | `/empresas` | Crear empresa, revisar duplicados | F: `nombre`, `nit`, `descripcion`, `telefono`, `correo`, `persona_contacto` | 303 detalle empresa | RF-004/010 |
| GET | `/empresas/{id}` | Detalle, sedes, áreas y cargos | ID | 200 HTML | RF-004/005/007 |
| GET, POST | `/empresas/{empresa}/sedes/nuevo` | Formulario / guardar sede | GET: padre; POST F: `nombre`, `direccion`, `ciudad`, `telefono`, `horario`, `descripcion` | 200 HTML / 303 nueva sede | RF-005/010 |
| GET, POST | `/empresas/{empresa}/areas/nuevo` | Formulario / guardar área | POST F: `nombre` | 200 HTML / 303 empresa | RF-007/010 |
| GET, POST | `/empresas/{empresa}/cargos/nuevo` | Formulario / guardar cargo | POST F: `nombre`, `tipo` | 200 HTML / 303 empresa | RF-007/010 |
| GET | `/sedes/{id}` | Sede y sus salones | ID | 200 HTML | RF-005/006 |
| GET, POST | `/sedes/{sede}/salones/nuevo` | Formulario / guardar salón | POST F: `nombre`, `descripcion`, `piso`, `capacidad_puestos` | 200 HTML / 303 nuevo salón | RF-006/010 |
| GET | `/salones/{id}` | Salón y sus puestos | ID | 200 HTML | RF-006/008 |
| GET, POST | `/salones/{salon}/puestos/nuevo` | Formulario / guardar puesto | POST F: `numero`, `descripcion`, `tipo`, `ubicacion`, `id_area`, `id_cargo` opcionales | 200 HTML / 303 salón | RF-008/010 |
| GET | `/areas/{id}` | Resolver área autorizada | ID | 303 empresa | RF-007 |
| GET | `/cargos/{id}` | Resolver cargo autorizado | ID | 303 empresa | RF-007 |
| GET | `/puestos/{id}` | A/E: Historial del puesto, jerarquía/estados, área/cargo y todas sus evaluaciones por fecha e ID descendentes, con semáforo y enlace Ver. U: conserva la redirección autorizada anterior. | ID | A: 200 HTML; U: 303 salón propio; ajeno/inexistente: 403 como antes | RNF-009, RF-028/024/020, RF-002 |
| GET, POST | `/{tipo}/{id}/editar` | Formulario / guardar cambios | `tipo=empresas|sedes|salones|areas|cargos|puestos`; F con los campos de creación de ese tipo | 200 HTML / 303 detalle (empresa/sede/salón) o padre (otros) | RF-009a; áreas/cargos RF-007 |
| POST | `/{tipo}/{id}/activar` | Reactivar registro | Mismos seis tipos; F | 303 padre con aviso | RF-028 |
| POST | `/{tipo}/{id}/desactivar` | Desactivar, conservar historial | Mismos seis tipos; F | 303 padre con aviso | RF-028, RNF-009 |

Las tres últimas filas son familias de seis rutas cada una: por ejemplo `/sedes/{id}/editar`, `/puestos/{id}/activar` y `/empresas/{id}/desactivar`. No existen POST `/sedes` ni `/empresas/nueva`: se guarda en las rutas concretas indicadas. Errores de formulario: 400/503 HTML con campos conservados (RNF-007).

## Reservas y visitas

Fuentes: [ReservasWeb](../src/main/java/co/aplomo/web/ReservasWeb.java), [VisitasWeb](../src/main/java/co/aplomo/web/VisitasWeb.java). Consultas **A/E/U**, alcance propio; gestión **A**.

| Método | Ruta | Para qué / quién | Formulario / parámetros | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/reservas/nueva` | Formulario de reserva; A/E | `empresa`, `sede`, `salon` opcionales para contexto | 200 HTML | RF-011 |
| GET | `/reservas/catalogo` | Actualizar selectores autorizados; A/E | `empresa`, `sede`, `salon` | 200 fragmento HTML | RF-011 |
| POST | `/reservas/crear-empresa` | Alta en contexto; A/E | F: `nombre`, `nit` | 200 **application/x-www-form-urlencoded**, `id` y `aviso`; 400/503 mismo formato con aviso | RF-004/010/011 |
| POST | `/reservas/crear-sede` | Alta en contexto; A/E | F: `empresa`, `nombre`, `direccion`, `ciudad`, `telefono`, `horario`, `descripcion` | Igual al alta anterior | RF-005/010/011 |
| POST | `/reservas/crear-salon` | Alta en contexto; A/E | F: `sede`, `nombre`, `descripcion`, `piso`, `capacidad_puestos` | Igual al alta anterior | RF-006/010/011 |
| POST | `/reservas` | Guardar reserva; A/E | F: `empresa`, `sede`, `salon`, `fecha`, `inicio`, `fin`, `categoria`, `descripcion`, `notas` | 303 detalle | RF-011 |
| GET | `/reservas/{id}` | Detalle, incluso motivo de cancelación; A/E/U | ID | 200 HTML | RF-011/012/024 |
| GET, POST | `/reservas/{id}/editar` | Formulario / actualizar reserva editable; A | Mismos campos de reserva; empresa/sede/salón vienen de la reserva existente | 200 HTML / 303 detalle | RF-009b |
| POST | `/reservas/{id}/aceptar` | Pasar Esperando a En cola; A/E | F | 303 detalle | RF-012 |
| GET, POST | `/reservas/{id}/cancelar` | Formulario / cancelar si no tiene visita; A | POST F: `motivo` obligatorio | 200 HTML / 303 detalle | RF-012, RNF-009 |
| POST | `/reservas/{reserva}/iniciar-visita` | Iniciar desde En cola; A/E | F | 303 `/visitas/{id}` | RF-013a |
| GET | `/visitas/{id}` | Detalle; A/E/U (U solo Finalizada) | ID | 200 HTML con datos autorizados | RF-013a/013b/024 |
| GET, POST | `/visitas/{id}/finalizar` | Formulario / cerrar visita En curso; A | POST F: `notas` | 200 HTML / 303 detalle; 400 si ya Finalizada | RF-013b |

Las escrituras con validación fallida devuelven 400 HTML; fallo de almacenamiento 503 HTML y conservan campos. Las fechas y horas se validan en el servidor; la hora real de inicio/final no la envía el navegador.

## Evaluaciones: conversación, fotos y cálculo

Fuentes: [EvaluacionesWeb.atender](../src/main/java/co/aplomo/web/EvaluacionesWeb.java), [DatosAnalisis](../src/main/java/co/aplomo/web/DatosAnalisis.java), [FotosEvaluacionWeb.cambiar](../src/main/java/co/aplomo/web/FotosEvaluacionWeb.java). Todo es **A** salvo el GET de lista y el detalle seguro de U indicado abajo. Las pantallas de la conversación son estados del navegador, no rutas distintas.

| Método | Ruta | Para qué / quién | Formulario / parámetros | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/evaluaciones/nueva` | Abrir recorrido en visita En curso; A/E | `visita` obligatorio, `puesto` opcional, siempre activo y de esa sede | 200 HTML | RF-014/015/016 |
| POST | `/evaluaciones` | Guardar contexto, fotos y análisis; A/E | F multipart, campos descritos debajo | **201 text/plain** con `Location` al detalle, visita o siguiente puesto; 400/403/503 texto claro | RF-014/015/016/021, RNF-005/006 |
| GET | `/evaluaciones/{evaluacion}` | Ver guardado; A/E/U | ID, `aviso` opcional | 200 HTML; U solo propia Finalizada y **sin fotos**, mediante ConsultasWeb | RF-020/021/024, RNF-006 |
| GET | `/evaluaciones/{evaluacion}/analizar` | Añadir análisis con fotos activas existentes; A/E | ID; visita En curso | 200 HTML con el recorrido | RF-017/021 |
| POST | `/evaluaciones/{evaluacion}/analisis` | Guardar hasta 20 análisis; A/E | F: `analisis_0`…`analisis_19`, `destino_guardado` opcional | **201 text/plain** y `Location`; 400/403/503 texto | RF-021, RNF-010 |
| GET | `/evaluaciones/{evaluacion}/analisis/{analisis}` | Resultado guardado con semáforo y sugerencias; A/E | Ambos IDs verificados | 200 HTML | RF-020/021/023a |
| POST | `/evaluaciones/calcular` | Previsualizar segmento o resultado, sin guardar; A/E | F URL-encoded: `visita` y datos RULA; o `segmento` y sus datos parciales | **200 fragmento HTML**, nunca JSON; 400/403/503 texto | RF-017/018/019/020/027, RNF-010 |
| POST | `/evaluaciones/{evaluacion}/fotos` | Añadir/cambiar/quitar lógicamente una foto; A/E, visita En curso | F multipart: `anterior` opcional, un archivo, `vista`, `descripcion`; sin archivo y con `anterior` marca reemplazada | **200 JSON** `{"foto":123,"anterior":45}` (pueden ser `null`); errores de servicio 400/503 texto, permiso 403 | RF-016, RNF-005/006/009 |
| GET | `/fotos/{foto}` | Leer fotografía privada; solo A/E, incluso histórica | ID foto, sin ruta física pública | 200 archivo `image/jpeg` o `image/png`, inline y CSP sandbox; no disponible 404; U/sin sesión 403 | RF-016, RNF-006 |

### Campos de evaluación y fotografías

- Contexto: `visita`, `puesto`, `postura`, `criterio`, `motivo`, `minutos`, `observaciones` (hasta 255), `acepta=si` y `registro=CON_FOTOS|SIN_REGISTRO_FOTOGRAFICO`. La aceptación escrita del consentimiento es independiente del permiso de fotos.
- Condiciones: `escritorio`, `pantalla`, `pies`, `apoyapies`, `apoyabrazos`, `regulable` (`si` indica marcada). En el recorrido `campo`/`guiado`: `condiciones_respondidas=NINGUNA|SELECCION`, coherente con esas seis casillas. `recorrido=campo` admite minutos 60/180/300/420/540; `guiado` admite 30/90/180/300. El formulario genérico admite 1–720.
- Archivos: por cada parte de archivo `foto_i`, campos `foto_i_vista` y `foto_i_descripcion` (hasta 200). Vistas de servidor `PERFIL`, `FRENTE`, `OTRA` (la toma de detalle/apoyo usa `OTRA`). De 1 a 10 fotos JPG/PNG de hasta **5 × 1024 × 1024 bytes cada una**, tipo real validado, nombre aleatorio; la base guarda solo su ruta. El modo sin registro rechaza archivos. El recorrido con fotos requiere perfil. Un PDF renombrado JPG no pasa.
- `asistente_completo=si` incorpora `analisis_i` al guardar; cada valor es un formulario URL-encoded con `foto` y los datos RULA. `destino_guardado=SIGUIENTE|VOLVER` es opcional; sin él vuelve al detalle guardado. Dos posturas se envían por separado y conservan acuses; no hay transacción conjunta entre ambos envíos.
- Cambiar o quitar en una evaluación guardada usa `/evaluaciones/{evaluacion}/fotos`. Marca la descripción anterior con `Reemplazada · `, conserva el archivo, análisis y bitácora, y excluye esa foto de nuevos análisis. Requiere consentimiento con fotos y visita En curso. El historial no se borra.

### Campos de cálculo y análisis

`DatosAnalisis.leer` recibe `lado=DERECHO|IZQUIERDO`, `ancho`, `alto`; para cada segmento `BRAZO`, `ANTEBRAZO`, `MUNECA`, `CUELLO`, `TRONCO`: `{SEGMENTO}_modo=PUNTOS|MANUAL`, `{SEGMENTO}_puntos=x,y;x,y;x,y` normalizados, y `{SEGMENTO}_grados` cuando es manual. Recibe todos los booleanos `si|no`: `hombroElevado`, `brazoSeparado`, `brazoApoyado`, `antebrazoCruza`, `munecaDesviada`, `cuelloGirado`, `cuelloInclinado`, `troncoGirado`, `troncoInclinado`, `piernasApoyadas`, `actividad`; además `giro=MEDIO|EXTREMO` y `carga` con uno de los seis nombres del enum `MotorRula.Carga`. Ambos lados se envían como dos análisis, no como `lado=AMBOS` al motor.

La variante parcial de `/evaluaciones/calcular` añade `segmento` (los cinco anteriores o `GIRO`/`PIERNAS`). Para segmentos angulares: sus campos, lado, dimensiones, `ajustes_confirmados=si` y ajustes de ese segmento; para GIRO/PIERNAS, `respuesta=MEDIO|EXTREMO` o `si|no`. Devuelve un `<span data-parcial>` con medición/puntos del segmento, **sin total final**. El resultado completo puede incluir las seis condiciones para previsualizar sugerencias. Todo puntaje guardado vuelve a calcularse en el servidor con tablas MySQL y NEUTRO=5°; no se altera `POST /evaluaciones/calcular` en este paso.

La detección automática corre en el navegador, no en un endpoint ALINEO: descarga el modelo MediaPipe desde CDN y procesa la foto localmente. Las fotos de ejemplo son recursos estáticos y luego entran por la misma validación de subida.

## Pendientes y sugerencias

Fuentes: [PendientesWeb](../src/main/java/co/aplomo/web/PendientesWeb.java), [SugerenciasWeb](../src/main/java/co/aplomo/web/SugerenciasWeb.java). GET de lista en ConsultasWeb. Detalles **A/E/U** con alcance de empresa/visita Finalizada; acciones y formularios **A**. Escrituras requieren F.

| Método | Ruta | Para qué | Parámetros / formulario | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/pendientes/{id}` | Detalle e historial | ID, `aviso` opcional | 200 HTML | RF-022/024 |
| GET | `/pendientes/{id}/editar` | Formulario de seguimiento | ID | 200 HTML | RF-022 |
| POST | `/pendientes/{id}` | Guardar seguimiento | F: `descripcion`, `prioridad`, `fecha` límite opcional | 303 detalle (*) | RF-022 |
| POST | `/pendientes/{id}/arreglado` | Marcar Arreglado idempotentemente | F | 303 detalle (*) | RF-022, RNF-009 |
| GET | `/sugerencias/nueva` | Formulario manual | `evaluacion` obligatorio | 200 HTML | RF-023b |
| POST | `/sugerencias` | Guardar sugerencia manual | Query `evaluacion`; F: `titulo`, `descripcion`, `tipo`, `prioridad`, `fecha` opcional | 303 nuevo detalle (*) | RF-023b |
| POST | `/sugerencias/nueva` | Alias de guardado admitido por el mismo manejador | Query `evaluacion`, mismo F | 303 nuevo detalle (*) | RF-023b; alias sin RF propio |
| GET | `/sugerencias/{id}` | Detalle e historial | ID, `aviso` opcional | 200 HTML | RF-023b/024 |
| GET | `/sugerencias/{id}/editar` | Formulario de sugerencia activa | ID | 200 HTML | RF-023b |
| POST | `/sugerencias/{id}` | Guardar edición | Mismos campos de sugerencia; evaluación/origen se obtienen del servidor | 303 detalle (*) | RF-023b |
| POST | `/sugerencias/{id}/quitar` | Quitar lógicamente conservando historial | F | 303 detalle (*) | RF-023b, RNF-009 |
| POST | `/sugerencias/{id}/estado` | Cambiar estado | F: `estado=PENDIENTE|EN_PROCESO|HECHA` | 303 detalle (*) | RF-023b |

(*) Si `Accept` es exactamente `text/plain`, el éxito es **201 text/plain con Location** en lugar de 303. Validación: 400 HTML; fallo de guardado: 503 HTML. Prioridad `ALTA|MEDIA|BAJA`, tipo de sugerencia `MOBILIARIO|POSTURA|DESCANSOS|CARGA|OTRA`, fechas ISO. Las automáticas se crean al guardar análisis, no hay endpoint público para generarlas por separado. No se permite quitar la última activa en Amarillo/Rojo; cuando todas están Hechas se ofrece registrar Reevaluación por `/reservas/nueva` (no se crea silenciosamente).

## Calificaciones e informe de la empresa

Fuente: [CalificacionesWeb.atender](../src/main/java/co/aplomo/web/CalificacionesWeb.java). Solo consulta, **A/E/U** (U únicamente sus empresas asignadas), con visitas Finalizadas.

| Método | Ruta | Para qué | Parámetros | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/calificaciones` | Elegir empresa o mostrar calificaciones/cobertura | `empresa` opcional para A; por defecto su empresa para U | 200 HTML | RF-025, RF-002 |
| GET | `/informes/empresa/{empresa}` | Informe general imprimible | ID empresa | 200 HTML con CSS de impresión; el navegador imprime o guarda PDF, el servidor no devuelve un PDF binario | RF-026, RF-025 |

La sede muestra «Promedio histórico de sus visitas Finalizadas»; la general solo se calcula con al menos dos sedes calificadas, indicando faltantes. Las estadísticas actuales de cada puesto usan su última evaluación Finalizada; los promedios históricos de visita se conservan. «Generado el» usa America/Bogota. POST a calificaciones/informes devuelve 405; informe no reconocido devuelve 404.

## Recursos estáticos

GET `/favicon.ico` y GET `/favicon.svg`: icono A con plomada, sin sesión; respuesta 200 `image/svg+xml`. POST devuelve 405. Identidad visual, RNF-002/011.


Fuente: [Router.estatico](../src/main/java/co/aplomo/web/Router.java).

| Método | Ruta | Para qué / quién | Parámetros | Respuesta | Requisito |
|---|---|---|---|---|---|
| GET | `/static/*` | CSS, JS e imágenes públicas de la app; sin sesión | Ruta de classpath terminada en `.css`, `.js`, `.jpg` o `.png`; sin `..` | 200 archivo con tipo correcto; 404 si no permitido/no existe; POST 405 | RNF-002/008/011, soporte de RF-017 |

Incluye `/static/img/galeria/*` (ejemplos), `/static/img/tour/*`, `/static/css/aplomo.css` y `/static/js/*`. Las fotografías privadas se leen por `/fotos/{foto}` y **nunca** desde esta ruta. No se sirve `config/db.properties`, `.env` ni archivos arbitrarios del disco.

## Trazabilidad y alcance de la comprobación

El único endpoint de valor agregado sin requisito funcional propio es `/demo/evaluacion`; reutiliza creación de visita/evaluación y RNF-002. `/sugerencias/nueva` por POST es un alias de RF-023b. Los recursos estáticos y Docker son soporte de RNF, no requisitos funcionales nuevos. No se inventa un RF para ellos.

`EndpointsDocumentoTest` comprueba los contextos literales registrados en App, todas las secciones de `SeccionConsulta`, las rutas literales comparadas con `ruta.equals` en los manejadores y la presencia de las familias dinámicas. No depende de espacios, números de línea ni títulos visuales. Es una alarma de documentación; no reemplaza las pruebas HTTP de roles, CSRF, cálculo y fotos. Este inventario no añade ni cambia rutas.

Paso 17: la empresa DEMO-APLOMO-PRACTICA se excluye de listas, catálogos, contadores y calificaciones; conserva el acceso dedicado de práctica. El cargador completo se ejecuta por CLI o perfil Docker demo, no mediante una ruta HTTP.

## Historial de registros inactivos (Paso 23)

Solo A recibe los avisos con «Ver evaluaciones» en detalles inactivos de empresa/sede/salón; el enlace conserva `empresa`, `sede` o `salon` como filtro. Toda empresa, activa o no, ofrece al Administrador «Ver evaluaciones de la empresa». Cada tarjeta de puesto del salón ofrece «Ver historial». Abrir estos GET no cambia estados ni reactiva registros. Calificaciones e informe general continúan disponibles para una empresa inactiva. U conserva empresa propia, resultados solo Finalizados y fotos denegadas.

## Paso 24 · permisos A / E / U

A = Administrador; E = Evaluador en empresa asignada; U = Usuario en empresas asignadas, resultados Finalizados y sin fotos.

| Método | Ruta | Permisos | Comportamiento |
|---|---|---|---|
| GET | `/agenda` | A / E | Semana/mes, vista/fecha/evaluador/canceladas; evaluador solo A, incluye inactivos |
| GET | `/mis-evaluaciones` | E | Solo autor CREAR de bitácora; conserva histórico sin escritura al retirar empresa |
| POST | `/reservas/{id}/nota-administrador` | A | nota máx. 500, CSRF; marca no vista y bitácora EDITAR |
| POST | `/reservas/{id}/entendido` | E | Solo su empresa, CSRF; confirma lectura |

Las rutas de crear/editar sedes, salones, áreas, cargos y puestos; crear/editar/aceptar/cancelar reservas; iniciar/finalizar visitas; evaluaciones, fotos y análisis; pendientes y sugerencias; informes y práctica pasan de A a A/E, con alcance en el servidor. POST /evaluaciones/calcular mantiene exactamente su contrato y cálculo; solo la autorización exterior admite E. GET /puestos/{id} historial es A/E; U conserva su redirección propia. Consultas/calificaciones son A/E/U; informes A/E. /usuarios (todos los métodos), crear empresa y activar/desactivar jerarquía siguen solo A. Usuarios pueden tener varias empresas; reservas no exigen Finalizada, resultados sí.

## Paso 25 · Presentación pública y Centro de ayuda

| Método | Ruta | Permisos | Respuesta |
|---|---|---|---|
| GET | `/nosotros` | Público | 200 HTML, marca animada y tres ideas, sin crear sesión |
| GET | `/ayuda` | Público / A / E / U | 200 HTML; visitante solo ingreso/contacto; A todos los temas, E sin cuentas, U 1/2/5/6/8/10 |

El contacto sale de config/contacto.properties, nunca de datos del navegador. Sin el archivo o con campos incompletos aparece el respaldo. El semáforo de Ayuda se consulta en nivel_riesgo. El buscador actúa sobre el contenido que el servidor ya autorizó, sin habilitar temas por JavaScript.


## Paso 26 · perfil, Archivo y calendario

| Método | Ruta | Permiso y comportamiento |
|---|---|---|
| GET | `/cuenta` | Titular vigente; correo, rol y empresas de solo lectura |
| POST | `/cuenta` | Sus datos personales; CSRF y bitácora EDITAR |
| POST | `/cuenta/clave` | Clave actual y confirmación; rota sesiones |
| GET | `/archivo` | Empresas con historial dentro del alcance de cada rol |
| GET | `/archivo?empresa={id}` | Árbol sede/salón/puesto; Evaluador solo autoría propia |
| GET | `/puestos/{id}` | Línea de tiempo autorizada; conserva gestión cuando no hay historial |
| GET | `/mis-evaluaciones` | 303 hacia Archivo |
| GET | `/notas/nuevas` | JSON privado del Evaluador: reserva, fecha, hora, empresa, texto; A/U 403 |
| GET | `/agenda` | Mes por defecto; `vista=semana`, `fecha`, `evaluador` y `canceladas` |
| GET | `/nosotros` | 200 HTML público con marca animada, sin crear sesión |

El sondeo de notas no renueva la actividad de sesión. Los acuses mantienen el POST con CSRF existente.

## Paso 27A · Propiedad y presentación (21/11/2026)

- Crear o modificar reservas, iniciar visitas y abrir la práctica requiere **E**; **A** conserva la consulta.
- E puede crear empresa; el servidor fija `empresa.id_creador` y le asigna acceso en la misma transacción.
- Si el dueño es E, solo él edita y desactiva empresa/sedes/salones/áreas/cargos/puestos. Los demás E asignados
  y A consultan. En una empresa creada por A se conservan las capacidades anteriores.
- No se permite quitar el acceso al dueño ni cambiar su rol antes de transferir su empresa.

| Método y ruta | Acceso y comportamiento |
| --- | --- |
| POST `/empresas/{id}/transferir` | A, CSRF. `evaluador`: dueño anterior E Inactivo y destino E Activo. Cambio y asignación con bitácora en una transacción. |
| GET `/cuenta/nombre`, `/cuenta/telefono`, `/cuenta/cargo` | A/E/U vigentes. Un único campo propio. |
| POST en esas mismas rutas | CSRF y solo el campo indicado por la ruta; rol, correo y relaciones enviados de más se ignoran. |
| GET `/cuenta/clave` | Formulario propio de contraseña; el POST ya existente conserva su contrato. |
| GET `/reservas/horas-libres?fecha=yyyy-mm-dd&omitir={id}` | E. Intervalos libres de 30 minutos; si se omite una reserva se comprueba su alcance. HTML sin datos privados de otras citas. |
| GET `/informes/empresa/{id}?visita={visita}` | A/E autorizados. Valida que la visita pertenezca a esa empresa; resalta una Finalizada e identifica una En curso sin cambiar los promedios. |

La presentación visible usa Por confirmar/Aceptada, pero la base conserva ESPERANDO/EN_COLA.
El selector colombiano de fecha y hora conserva los valores ISO de los formularios existentes.
El motor, las tablas RULA y el bloque de POST `/evaluaciones/calcular` se conservan sin cambios.

GET `/reservas/horas-libres`: recibe `fecha` y `omitir` según la tabla del Paso 27A.

### Paso 27B · avisos y solicitudes (propuesta aprobada por Andy)

| Ruta | Permiso y comportamiento |
| --- | --- |
| GET `/avisos` | A/E/U. Bandeja propia, filtro `leidos=1`; panel con cabecera `X-Alineo-Panel: 1`. |
| GET `/avisos/nuevos` | A/E/U. JSON de avisos propios sin leer; no renueva inactividad, 401 si venció la sesión. |
| POST `/avisos/{id}/entendido` | Destinatario, CSRF. Acuse explícito, sin borrar. |
| POST `/avisos/{id}/responder` | U destinatario de PEDIR_INFORMACION; CSRF, texto de 500, una respuesta por mensaje. |
| GET/POST `/solicitar-visita` | U vigente. Sede/salón de su empresa, ingeniero activo, fecha/horas y motivo opcional. Cruce validado otra vez en el POST. |
| GET `/solicitudes-visita/{id}` | E responsable de esa petición, acceso mínimo al contacto antes de asignación. |
| POST `/solicitudes-visita/{id}/aceptar` | E responsable, CSRF. EN_COLA y asignación de empresa en una transacción. |
| POST `/solicitudes-visita/{id}/llamada` | E responsable, CSRF. Avisa al contacto sin cambiar el estado. |
| POST `/solicitudes-visita/{id}/informacion` | E responsable, CSRF. Texto obligatorio, máximo 500. |
| POST `/solicitudes-visita/{id}/rechazar` | E responsable, CSRF. Motivo obligatorio, máximo 255; CANCELADA. |

`/notas/nuevas` conserva el contrato privado legado para E; ahora filtra por `reserva.id_evaluador`.
La bandeja nueva usa `/avisos/nuevos`. Consultar no marca leído. Varias notas idénticas tienen IDs distintos.
Un E asignado a la empresa consulta otras citas, pero no las edita ni inicia por otro ingeniero.
Las solicitudes creadas por U solo las ve su propio contacto; las citas legadas de la empresa siguen visibles.

### Paso 27C · solicitud de acceso (propuesta aprobada)

| Método y ruta | Acceso y comportamiento |
| --- | --- |
| GET `/solicitar-acceso` | Público. Sesión anónima para CSRF; no registro libre. |
| POST `/solicitar-acceso` | Público, CSRF y cinco envíos por dirección remota/hora. Campos validados; no crea empresa ni cuenta. HTTP 429 al superar el límite. |
| GET `/solicitudes-acceso` | A vigente, lista de solicitudes Pendientes. E/U: 403. |
| GET `/solicitudes-acceso/{id}` | A vigente, contacto y formulario de decisión. |
| POST `/solicitudes-acceso/{id}/aprobar` | A vigente, CSRF. Empresa + Usuario temporal + asignación opcional + APROBADA, una transacción. Clave solo en esta respuesta. |
| POST `/solicitudes-acceso/{id}/rechazar` | A vigente, CSRF y motivo obligatorio de hasta 255; conserva solicitud, sin cuenta nueva. |

Al crear una empresa como E con persona de contacto/correo, se solicitan cargo y ciudad del contacto;
se guarda su solicitud Pendiente en la misma transacción, sin cuenta hasta aprobación. Si E no agrega
contacto, conserva el alta de empresa sin solicitud. No cambia el dueño al aprobar el contacto.
El enlace WhatsApp prepara el mensaje para revisión y envío manual; nunca envía automáticamente.
Las solicitudes se enlazan a mensajes por una entrada CREAR de bitácora `ACCESO:{id}`; no hay credenciales
ni información de otros destinatarios en el sondeo. El sistema usa una cuenta receptora para la FK de
remitente de solicitudes externas, y la interfaz lo rotula «Solicitud recibida · Acceso por revisar».

### Paso 28A · Mensaje al contacto principal

| Método | Ruta | Alcance y respuesta |
|---|---|---|
| GET | `/avisos/nuevo` | Cuenta vigente con sesión; formulario privado dirigido al contacto principal configurado. |
| POST | `/avisos/nuevo` | Misma cuenta y CSRF; destinatario resuelto en servidor, texto de hasta 500 caracteres. Respuesta HTML o panel de bandeja. |

Guardar una nota de reserva conserva el detalle y muestra confirmación; permisos y sesión siguen vigentes.

Paso 28C: `GET /static/img/logo_alineo.svg` se sirve como `image/svg+xml`; la marca usada en páginas
se incorpora en línea desde `Marca.logo()` con las marcas de regla generadas en Java.
`POST /avisos/{id}/entendido` con `X-Alineo-Panel: 1` y CSRF devuelve 200 JSON con `ok` y `nuevos`;
sin ese encabezado conserva la redirección de formularios. Solo el destinatario vigente puede confirmar.
