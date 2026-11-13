# Requisitos v6 (fuente: Notion "Proyecto Nuclear 6.0", Matriz de requisitos v6)

Copia congelada de la matriz el 2026-11-10. Son **44 requisitos: 33 funcionales y 11 no funcionales**. Si este archivo y el código no coinciden, manda este archivo.

Los valores marcados como "(propuesta)" son del equipo y están pendientes de validar con la clienta: impleméntalos tal cual, pero déjalos fáciles de cambiar (constantes o tabla de configuración).

Índice rápido:

| ID | Nombre | Módulo | Rol | Prioridad |
|---|---|---|---|---|
| RF-001a | Iniciar sesión | Seguridad y acceso | Administrador, Usuario | Media |
| RF-001b | Cerrar sesión | Seguridad y acceso | Administrador, Usuario | Media |
| RF-001c | Cambiar contraseña | Seguridad y acceso | Administrador, Usuario | Media |
| RF-002 | Controlar permisos según el rol | Seguridad y acceso | Administrador, Usuario | Media |
| RF-003 | Administrar (crear, editar, activar y desactivar) usuarios | Administración | Administrador | Media |
| RF-004 | Crear y consultar empresas | Configuración de empresa | Administrador | Alta |
| RF-005 | Registrar las sedes de la empresa | Configuración de empresa | Administrador | Alta |
| RF-006 | Registrar los salones de cada sede | Configuración de empresa | Administrador | Alta |
| RF-007 | Registrar las áreas y los cargos de la empresa | Configuración de empresa | Administrador | Alta |
| RF-008 | Registrar los puestos de trabajo con su número | Configuración de empresa | Administrador | Alta |
| RF-009a | Editar empresas, sedes, salones y puestos | Configuración de empresa | Administrador | Alta |
| RF-009b | Editar reservas | Agenda de visitas | Administrador | Alta |
| RF-010 | Revisar si ya existe antes de crear | Configuración de empresa | Administrador | Alta |
| RF-011 | Solicitar una reserva | Agenda de visitas | Administrador | Alta |
| RF-012 | Gestionar (aceptar, finalizar o cancelar) las reservas según su estado | Agenda de visitas | Administrador | Alta |
| RF-013a | Iniciar una visita | Agenda de visitas | Administrador | Alta |
| RF-013b | Finalizar una visita | Agenda de visitas | Administrador | Alta |
| RF-014 | Elegir el puesto y la postura a estudiar | Evaluación de postura | Administrador | Alta |
| RF-015 | Anotar las condiciones del puesto | Evaluación de postura | Administrador | Media |
| RF-016 | Subir varias fotos del puesto | Registro fotográfico | Administrador | Alta |
| RF-017 | Medir los ángulos de cada foto | Medición de ángulos | Administrador | Alta |
| RF-018 | Calcular los puntos RULA de cada foto | Cálculo RULA | Administrador | Alta |
| RF-019 | Sumar puntos por actividad muscular y carga | Cálculo RULA | Administrador | Alta |
| RF-020 | Mostrar el nivel de riesgo (semáforo) | Resultados | Administrador | Alta |
| RF-021 | Guardar el análisis de cada foto | Resultados | Administrador | Alta |
| RF-022 | Marcar los puestos en Rojo como pendientes de arreglo | Seguimiento | Administrador | Alta |
| RF-023a | Proponer sugerencias del sistema | Recomendaciones | Administrador | Media |
| RF-023b | Registrar y gestionar (editar, quitar y cambiar el estado de) las sugerencias | Recomendaciones | Administrador | Media |
| RF-024 | Ver, buscar y filtrar en cada sección | Consultas | Administrador, Usuario | Alta |
| RF-025 | Calificar las sedes y la empresa | Indicadores | Administrador | Alta |
| RF-026 | Generar el informe general de la empresa en PDF | Informes | Administrador | Alta |
| RF-027 | Guardar las tablas RULA en la base de datos | Cálculo RULA | Administrador | Alta |
| RF-028 | Desactivar y reactivar registros | Configuración de empresa | Administrador | Alta |
| RNF-001 | Base de datos MySQL con tablas conectadas | Arquitectura | Todos los roles | Alta |
| RNF-002 | Facilidad de uso | Interfaz | Todos los roles | Alta |
| RNF-003 | Velocidad | Plataforma | Todos los roles | Media |
| RNF-004 | Seguridad básica | Seguridad y acceso | Administrador | Alta |
| RNF-005 | Registro de fecha, hora y responsable | Auditoría | Administrador | Media |
| RNF-006 | Privacidad de las fotos | Seguridad y cumplimiento | Administrador | Alta |
| RNF-007 | Mensajes que dicen qué pasó y qué hacer | Interfaz | Todos los roles | Alta |
| RNF-008 | Página web compatible con Chrome, Edge y Firefox | Plataforma | Todos los roles | Media |
| RNF-009 | Conservar el historial | Operación y continuidad | Administrador | Media |
| RNF-010 | Exactitud del cálculo RULA | Cálculo RULA | Desarrollador | Alta |
| RNF-011 | Mantenibilidad del código | Arquitectura | Desarrollador | Media |

---

## Requisitos funcionales

### RF-001a · Iniciar sesión

- **Módulo:** Seguridad y acceso · **Rol:** Administrador, Usuario · **Prioridad:** Media

**Descripción.** El sistema deberá permitir a un usuario activo iniciar sesión con su correo y su contraseña.

**Regla de negocio.** Solo entran usuarios activos con datos correctos. El mensaje de error no dice si falló el correo o la contraseña. Después de 5 intentos fallidos seguidos se bloquea el ingreso por 15 minutos (propuesta). Si alguien olvida su contraseña, el Administrador le asigna una temporal y la persona debe cambiarla al entrar (RF-001c). Fuente: lista acordada en clase (5-nov-2026), n.º 1.

### RF-001b · Cerrar sesión

- **Módulo:** Seguridad y acceso · **Rol:** Administrador, Usuario · **Prioridad:** Media

**Descripción.** El sistema deberá cerrar la sesión del usuario cuando este lo pida o después de 30 minutos sin actividad.

**Regla de negocio.** Al cerrar la sesión, las direcciones internas dejan de abrirse sin volver a ingresar. El cierre por inactividad es a los 30 minutos (propuesta). Fuente: lista acordada en clase (5-nov-2026), n.º 1.

### RF-001c · Cambiar contraseña

- **Módulo:** Seguridad y acceso · **Rol:** Administrador, Usuario · **Prioridad:** Media

**Descripción.** El sistema deberá permitir al usuario con sesión iniciada cambiar su contraseña escribiendo la contraseña actual y la nueva.

**Regla de negocio.** La nueva contraseña cumple las reglas de RNF-004. Una contraseña temporal obliga a cambiarla al entrar. Fuente: lista acordada en clase (5-nov-2026), n.º 1.

### RF-002 · Controlar permisos según el rol

- **Módulo:** Seguridad y acceso · **Rol:** Administrador, Usuario · **Prioridad:** Media

**Descripción.** El sistema deberá tener dos roles: Administrador (ingeniero industrial o analista), que gestiona el sistema y las cuentas de usuario y registra empresas, reservas, visitas, evaluaciones, pendientes, sugerencias e informes; y Usuario (persona de contacto de la empresa cliente), que solo inicia sesión y consulta, busca y filtra la información de su propia empresa (RF-024).

**Regla de negocio.** Los permisos se revisan en el servidor; no basta con esconder los botones. El Usuario no ve botones de crear, editar ni cambiar estado. Fuente: lista acordada en clase (5-nov-2026), n.º 2; roles según los casos de uso aprobados por el docente (CU5-01 a CU5-04).

### RF-003 · Administrar (crear, editar, activar y desactivar) usuarios

- **Módulo:** Administración · **Rol:** Administrador · **Prioridad:** Media

**Descripción.** El sistema deberá permitir al Administrador crear, editar, activar y desactivar cuentas de usuario, con nombre, correo, teléfono y cargo, y asignarles un rol.

**Regla de negocio.** Solo el Administrador activa y desactiva cuentas. Al crear una cuenta de Usuario se indica la empresa a la que pertenece. No se repiten correos (RF-010). Un usuario desactivado no entra, pero lo que hizo queda guardado. Al crear la cuenta se asigna una contraseña temporal. El sistema se instala con una cuenta de Administrador inicial, y no se puede desactivar al último Administrador activo (propuesta del equipo). Fuente: lista acordada en clase (5-nov-2026), n.º 3; caso de uso aprobado CU5-04.

### RF-004 · Crear y consultar empresas

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador crear, consultar y editar empresas cliente con nombre y NIT (obligatorios) y, de forma opcional, descripción, teléfono, correo y persona de contacto.

**Regla de negocio.** Nombre y NIT son obligatorios. Si el NIT ya existe o el nombre es igual al de otra empresa sin contar mayúsculas, tildes, espacios ni sufijos como S.A.S., Ltda. o S.A., el sistema avisa (RF-010). Las empresas no se borran: se desactivan y su historial se conserva (RF-028). Después de crear la empresa se registran sus áreas y cargos (RF-007). Fuente: lista acordada en clase (5-nov-2026), n.º 4.

### RF-005 · Registrar las sedes de la empresa

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador registrar varias sedes por empresa, cada una con nombre, dirección, ciudad, teléfono, horario y una descripción de máximo 200 caracteres.

**Regla de negocio.** Una sede siempre pertenece a una empresa. El nombre no se repite dentro de la misma empresa (RF-010). Los salones, puestos y visitas se guardan por sede. Las sedes no se borran: se desactivan (RF-028). Fuente: lista acordada en clase (5-nov-2026), n.º 5.

### RF-006 · Registrar los salones de cada sede

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador registrar los salones (o áreas físicas) de cada sede, con nombre, descripción de máximo 200 caracteres, piso y capacidad de puestos.

**Regla de negocio.** Un salón siempre pertenece a una sede. El nombre no se repite dentro de la misma sede (RF-010). Fuente: lista acordada en clase (5-nov-2026), n.º 6.

### RF-007 · Registrar las áreas y los cargos de la empresa

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador registrar las áreas de una empresa y sus cargos, indicando si cada cargo es Operativo, Administrativo u Otro.

**Regla de negocio.** Áreas y cargos pertenecen a una empresa y su nombre no se repite dentro de ella (RF-010). No se borran: se desactivan (RF-028). Son opcionales para el puesto (propuesta). No se guardan datos de personas, solo el cargo. Sirven para contar puestos por área y tipo de cargo e identificar el área con más puestos en Rojo. Fuente: lista acordada en clase (5-nov-2026), n.º 7.

### RF-008 · Registrar los puestos de trabajo con su número

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador registrar puestos de trabajo dentro de un salón, con número, descripción, tipo de puesto (escritorio, mesa, mostrador u otro), ubicación dentro del salón y, de forma opcional, el área y el cargo.

**Regla de negocio.** El número no se repite dentro del mismo salón (RF-010). El puesto guarda su último puntaje y nivel de riesgo. El área y el cargo son opcionales (propuesta). Fuente: lista acordada en clase (5-nov-2026), n.º 8.

### RF-009a · Editar empresas, sedes, salones y puestos

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador editar los datos de empresas, sedes, salones y puestos ya creados, excepto su ID.

**Regla de negocio.** En la empresa se edita todo menos su ID. Si se cambia el NIT o el nombre se vuelve a revisar que no se repitan (RF-010). Fuente: lista acordada en clase (5-nov-2026), n.º 9.

### RF-009b · Editar reservas

- **Módulo:** Agenda de visitas · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador editar la fecha, las horas, la categoría, la descripción y las notas de una reserva en estado Esperando o En cola.

**Regla de negocio.** No se editan el código, la empresa, la sede, el salón ni el estado (el estado cambia con botones, RF-012). Una reserva Finalizada o Cancelada no se edita. Fuente: lista acordada en clase (5-nov-2026), n.º 9.

### RF-010 · Revisar si ya existe antes de crear

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá verificar, antes de guardar un registro nuevo, si ya existe uno igual y avisar a quien lo registra con una notificación que indique cuál es el registro existente.

**Regla de negocio.** Se cuenta como repetido: empresa = mismo NIT (un nombre igual sin contar mayúsculas, tildes, espacios ni sufijos como S.A.S., Ltda. o S.A. solo avisa); área, cargo y sede = mismo nombre en la empresa; salón = mismo nombre en la sede; puesto = mismo número en el salón; reserva = misma sede, salón, fecha y hora de inicio; usuario = mismo correo en una cuenta activa. No importan mayúsculas, tildes ni espacios de más. Si está repetido no se guarda. Fuente: lista acordada en clase (5-nov-2026), n.º 10.

### RF-011 · Solicitar una reserva

- **Módulo:** Agenda de visitas · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador registrar una reserva de visita con empresa, sede, salón, fecha, hora de inicio, hora de fin y categoría de visita, y de forma opcional una descripción y notas, asignándole un código único.

**Regla de negocio.** La reserva empieza en estado Esperando. La fecha es hoy o posterior y la hora de fin es posterior a la de inicio. La categoría se elige de una lista fija: Evaluación inicial, Seguimiento o Reevaluación (propuesta del equipo, por validar). El código lo asigna el sistema (por ejemplo, R-0001) y no se puede cambiar. Si la empresa, la sede o el salón no existen, se pueden crear desde la misma pantalla sin perder lo escrito. La fecha se acuerda con la empresa por fuera del sistema; el Administrador solo registra lo acordado. Fuente: lista acordada en clase (5-nov-2026), n.º 11.

### RF-012 · Gestionar (aceptar, finalizar o cancelar) las reservas según su estado

- **Módulo:** Agenda de visitas · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá guardar el estado de cada reserva (Esperando, En cola, Finalizada o Cancelada) y permitir al Administrador cambiarlo solo según las transiciones permitidas.

**Regla de negocio.** Transiciones: Esperando → En cola (el Administrador la acepta); En cola → Finalizada (al finalizar la visita, RF-013b); Esperando o En cola → Cancelada (con motivo obligatorio, propuesta). Finalizada y Cancelada son estados finales. Cada cambio guarda fecha, hora y quién lo hizo. Fuente: lista acordada en clase (5-nov-2026), n.º 12.

### RF-013a · Iniciar una visita

- **Módulo:** Agenda de visitas · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador iniciar una visita a partir de una reserva En cola, guardando la hora real de inicio y dejando la visita En curso.

**Regla de negocio.** Solo hay una visita por reserva. Solo se inicia desde una reserva En cola. El cambio de estado guarda fecha, hora y quién lo hizo. Fuente: lista acordada en clase (5-nov-2026), n.º 13.

### RF-013b · Finalizar una visita

- **Módulo:** Agenda de visitas · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador finalizar una visita En curso que tenga al menos una evaluación guardada, registrando la hora real de fin, las notas y la calificación final de la visita.

**Regla de negocio.** Al finalizar se calcula la calificación final (promedio de los puntajes RULA de los puestos evaluados, propuesta por validar con la clienta) y la reserva pasa a Finalizada. Desde ese momento los resultados de la visita quedan disponibles en las consultas (RF-024). El cambio de estado guarda fecha, hora y quién lo hizo. Fuente: lista acordada en clase (5-nov-2026), n.º 13.

### RF-014 · Elegir el puesto y la postura a estudiar

- **Módulo:** Evaluación de postura · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador crear una evaluación de un puesto dentro de una visita En curso, registrando la postura elegida, el criterio de elección (más inadecuada o mayor tiempo de exposición), el motivo y el tiempo en minutos por jornada.

**Regla de negocio.** Se evalúa la postura más riesgosa o la que más tiempo se mantiene, no todas. El criterio, el motivo y el tiempo son obligatorios. El tiempo es un número entero de 1 a 720 minutos por jornada (propuesta). La evaluación solo se crea dentro de una visita En curso. Se guarda la metodología usada (RULA) para poder agregar otras después. Fuente: lista acordada en clase (5-nov-2026), n.º 14.

**Actualización aprobada por Andy, Paso 13E-A (13 nov 2026).** El nuevo recorrido elige el puesto desde la lista de activos del salón de la visita (o pregunta por él si falta en la dirección). ESCRITORIO usa modo Oficina y otros tipos modo Planta. Postura se elige entre ocho mosaicos con descripción editable de hasta 150 caracteres; momento se elige entre «La postura más exigente» y «La que más tiempo dura». El tiempo se captura como 30/90/180/300 minutos. El servidor genera motivo «Momento: … Postura: …»; la nota no se copia allí. El rango histórico 1–720 se conserva en el servicio para compatibilidad; el nuevo formulario valida sus cuatro opciones.

### RF-015 · Anotar las condiciones del puesto

- **Módulo:** Evaluación de postura · **Rol:** Administrador · **Prioridad:** Media

**Descripción.** El sistema deberá permitir al Administrador registrar con casillas Sí o No las condiciones del puesto que afectan a la persona (escritorio muy alto, pantalla muy alta, pies colgando, apoyapiés, silla con apoyabrazos, silla regulable) y un campo de otras observaciones.

**Regla de negocio.** Son observaciones del puesto y alimentan las sugerencias del sistema (RF-023a). Definiciones: escritorio muy alto = la superficie queda por encima del codo con los hombros relajados; pantalla muy alta = el borde superior queda por encima de la línea de los ojos; pies colgando = los pies no apoyan completos en el piso. Fuente: lista acordada en clase (5-nov-2026), n.º 15.

**Actualización aprobada por Andy, Paso 13E-A (13 nov 2026).** Las mismas seis condiciones se presentan en mosaicos múltiples y una opción Ninguna excluyente. Se exige respuesta explícita. Otras observaciones es opcional, hasta 255 caracteres completos, sin copiar al motivo.

### RF-016 · Subir varias fotos del puesto

- **Módulo:** Registro fotográfico · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** Antes de evaluar se exige la casilla «La persona firmó el consentimiento físico y queda archivado». Se guarda en `evaluacion.persona_acepto`. Después se elige Con fotos o Sin fotos (la persona no aceptó fotos), por separado. Esta actualización fue aprobada por Andy para el Paso 13E-A el 13 nov 2026 y sustituye la regla anterior del consentimiento fotográfico para registros nuevos; no modifica datos históricos ni esquema.

**Regla de negocio.** Con fotos: de 1 a 10 JPG/PNG de hasta 5 MB, vista y descripción de máximo 200 caracteres, nombre aleatorio fuera de MySQL y ruta protegida solo para Administrador. Por foto se elige Para medir o De apoyo; las de apoyo se conservan sin cálculo, y se requiere al menos una Para medir para continuar al análisis. Sin fotos: consentimiento físico sigue siendo obligatorio, `registro_fotografico=SIN_REGISTRO_FOTOGRAFICO`, sin archivos ni análisis; el servidor rechaza cualquier foto recibida. Los límites y la validación del tipo real se conservan. La casilla física es obligatoria en el servidor, incluso si se omite la interfaz.

### RF-017 · Medir los ángulos de cada foto

- **Módulo:** Medición de ángulos · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá calcular el ángulo de una parte del cuerpo (brazo, antebrazo, muñeca, cuello o tronco) a partir de tres puntos que el Administrador marca sobre la foto ampliada, y permitir marcar con casillas lo que la foto no muestra (hombro elevado, cabeza rotada, giro de muñeca, apoyo y piernas).

**Regla de negocio.** El ángulo se calcula con los tres puntos y se puede corregir moviendo los puntos o escribiéndolo a mano. La foto debe tomarse de frente o de perfil, con el movimiento paralelo a la cámara. RULA se aplica al lado derecho y al izquierdo por separado; si hay duda, se analizan los dos. Si falta un dato o tiene un valor imposible, no se calcula. Fuente: lista acordada en clase (5-nov-2026), n.º 17.

**Actualización del Paso 13E-B autorizada por Andy (13 nov 2026).** El nuevo formulario recorre Brazo, Antebrazo, Muñeca, Giro, Cuello, Tronco y Piernas; las mediciones/detección existentes sugieren un rango que requiere confirmación. Un rango diferente o sin puntos utiliza el ángulo representativo aprobado como corrección manual. La corrección escrita conserva su valor real. Derecho/Izquierdo/Ambos produce análisis independientes por foto y lado, sin mezclar sus respuestas. Los ajustes se confirman por segmento en una hoja inferior accesible, con Ninguno explícito.

### RF-018 · Calcular los puntos RULA de cada foto

- **Módulo:** Cálculo RULA · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá asignar puntos a cada parte del cuerpo según su ángulo y sus ajustes, obtener el puntaje del Grupo A (Tabla A) y del Grupo B (Tabla B) y, con la Tabla C, el puntaje final de 1 a 7.

**Regla de negocio.** Se usan las reglas y tablas del método RULA (Ergonautas). Ajustes: +1 hombro elevado, +1 brazo separado del cuerpo, −1 brazo apoyado, +1 antebrazo cruzando la línea media o hacia afuera, +1 muñeca desviada, +1 cuello girado, +1 cuello inclinado lateralmente, +1 tronco girado, +1 tronco inclinado lateralmente. El cálculo se muestra antes de guardar. Fuente: lista acordada en clase (5-nov-2026), n.º 18.

**Puntaje parcial, decisión de Andy (13 nov 2026).** Durante el recorrido se muestran únicamente los puntos del segmento confirmado, incluidos sus ajustes, calculados por el servidor mediante el motor existente. El puntaje final permanece Pendiente mientras falten segmentos, actividad o carga; nunca se completa con respuestas supuestas. El cálculo y las tablas MySQL no se modifican.

### RF-019 · Sumar puntos por actividad muscular y carga

- **Módulo:** Cálculo RULA · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá sumar 1 punto si la postura es estática (más de 1 minuto) o repetitiva (más de 4 veces por minuto) y de 0 a 3 puntos según la carga o fuerza, a los puntajes de los Grupos A y B antes de aplicar la Tabla C.

**Regla de negocio.** Carga menor de 2 kg intermitente = 0; de 2 a 10 kg intermitente = +1; de 2 a 10 kg estática o repetitiva = +2; más de 10 kg intermitente = +2; más de 10 kg estática o repetitiva = +3; golpes o fuerzas bruscas = +3. Fuente: lista acordada en clase (5-nov-2026), n.º 19.

### RF-020 · Mostrar el nivel de riesgo (semáforo)

- **Módulo:** Resultados · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá convertir el puntaje final en un nivel de riesgo con color, nombre y acción: Verde (no necesita cambios), Amarillo (hacer una revisión) o Rojo (intervención inmediata), y mostrar al lado el nivel de actuación oficial de RULA.

**Regla de negocio.** Rangos propuestos: Verde = 1 a 2; Amarillo = 3 a 4; Rojo = 5 a 7. Los rangos y el texto de cada nivel se guardan en la base de datos. El nivel se muestra con nombre y color, no solo con color. Junto al color se muestra el nivel de actuación oficial de RULA (Ergonautas, Tabla 18): 1–2 Nivel 1, 3–4 Nivel 2, 5–6 Nivel 3, 7 Nivel 4 (propuesta del equipo). Fuente: lista acordada en clase (5-nov-2026), n.º 20. Nivel oficial RULA: propuesta del equipo (Matriz v4, RF-016).

### RF-021 · Guardar el análisis de cada foto

- **Módulo:** Resultados · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá guardar un análisis independiente por cada foto, con el lado evaluado, los puntos de cada parte del cuerpo, el puntaje final y el nivel de riesgo.

**Regla de negocio.** El análisis de una foto no se mezcla con el de otra. Nada se reemplaza: si se analiza de nuevo, se crea un análisis nuevo. El puesto guarda el puntaje más alto de su última evaluación (propuesta). Fuente: lista acordada en clase (5-nov-2026), n.º 21.

### RF-022 · Marcar los puestos en Rojo como pendientes de arreglo

- **Módulo:** Seguimiento · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá agregar a una lista de pendientes de arreglo cada puesto cuyo análisis quede en Rojo, con descripción de lo que hay que arreglar, prioridad y fecha límite, y permitir al Administrador marcarlo como Arreglado.

**Regla de negocio.** Cada pendiente guarda el puesto, el puntaje y la fecha. Si el puesto se evalúa otra vez, el pendiente anterior no se borra. El Administrador marca el pendiente como Arreglado cuando el puesto ya fue corregido (caso de uso aprobado CU5-25). Los puestos en Amarillo no entran a pendientes: quedan En observación y se recomienda una visita de Seguimiento; los puestos en Verde quedan Aceptables. El Usuario consulta los pendientes de su empresa sin poder cambiarlos (RF-024). La lista se puede ordenar por prioridad o por fecha. Fuente: lista acordada en clase (5-nov-2026), n.º 22; caso de uso aprobado CU5-25.

### RF-023a · Proponer sugerencias del sistema

- **Módulo:** Recomendaciones · **Rol:** Administrador · **Prioridad:** Media

**Descripción.** El sistema deberá proponer sugerencias de mejora a partir de los puntos de cada parte del cuerpo y de las condiciones del puesto (RF-015).

**Regla de negocio.** Las sugerencias salen de reglas guardadas en la base de datos (sugerencias_base) y se marcan como «Sugerencia del sistema». Son un valor agregado: el análisis se entrega aunque no haya sugerencias. No reemplazan un diagnóstico médico. Fuente: lista acordada en clase (5-nov-2026), n.º 23.

### RF-023b · Registrar y gestionar (editar, quitar y cambiar el estado de) las sugerencias

- **Módulo:** Recomendaciones · **Rol:** Administrador · **Prioridad:** Media

**Descripción.** El sistema deberá permitir al Administrador editar, quitar o escribir sugerencias con título, descripción, tipo, prioridad, estado y fecha límite, y cambiar su estado.

**Regla de negocio.** Tipos: mobiliario, postura, descansos, carga u otra. Estados: Pendiente, En proceso o Hecha. La empresa ejecuta las mejoras por fuera del sistema y el Administrador actualiza el estado. Si el nivel es Amarillo o Rojo debe quedar al menos una (propuesta). Cuando las sugerencias de un puesto están Hechas, se registra una reserva de Reevaluación (RF-011). Fuente: lista acordada en clase (5-nov-2026), n.º 23.

### RF-024 · Ver, buscar y filtrar en cada sección

- **Módulo:** Consultas · **Rol:** Administrador, Usuario · **Prioridad:** Alta

**Descripción.** El sistema deberá mostrar en cada sección (empresas, áreas y cargos, sedes, salones, puestos, reservas, visitas, evaluaciones, pendientes y sugerencias) una tabla que se pueda ordenar, buscar y filtrar por empresa, área, cargo, sede, salón, número de puesto, fecha, puntaje, nivel de riesgo y estado.

**Regla de negocio.** El Usuario solo ve, busca y ordena los datos de su propia empresa, sin botones de crear ni editar. El Administrador ve todas las empresas. La tabla indica cuántos resultados hay. Fuente: lista acordada en clase (5-nov-2026), n.º 24.

### RF-025 · Calificar las sedes y la empresa

- **Módulo:** Indicadores · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá calcular la calificación RULA de cada sede a partir de sus visitas finalizadas y, si la empresa tiene dos sedes o más, una calificación general entre las sedes.

**Regla de negocio.** Puesto = su puntaje RULA más alto (1 a 7). Visita = promedio de los puestos evaluados. Sede = promedio de sus visitas finalizadas. General = promedio de las sedes; con una sola sede no se calcula. Para el color, el promedio se redondea al entero más cercano (propuesta). Los promedios son un indicador del equipo, no parte del método RULA (escala ordinal de 1 a 7); se validan con la clienta. Fuente: lista acordada en clase (5-nov-2026), n.º 25.

### RF-026 · Generar el informe general de la empresa en PDF

- **Módulo:** Informes · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá generar en PDF el informe general de una empresa con sus datos y, por cada sede, la cantidad de visitas, el detalle de los puestos, la calificación RULA con la descripción de los peligros, estadísticas, sugerencias y aspectos a mejorar, y al final la calificación general entre sedes, y permitir al Administrador generarlo y descargarlo.

**Regla de negocio.** Requiere al menos una visita finalizada (propuesta). Con una sola sede el informe indica que no hay calificación general. Las estadísticas se cuentan desde la base de datos: visitas, puestos evaluados, fotos analizadas, puestos en Verde, Amarillo y Rojo, pendientes, sugerencias por tipo, puestos por área y tipo de cargo y área con más puestos en Rojo. Muestra la empresa y la fecha de generación. Lo genera el Administrador. Imprimir se hace desde el PDF con el navegador. Debe entenderse impreso en blanco y negro. Fuente: lista acordada en clase (5-nov-2026), n.º 26; caso de uso aprobado CU5-29.

### RF-027 · Guardar las tablas RULA en la base de datos

- **Módulo:** Cálculo RULA · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá guardar en la base de datos las tablas A, B y C del método RULA y los niveles del semáforo, y consultarlas al calcular.

**Regla de negocio.** Los valores son los de Ergonautas. Se cargan una sola vez y no cambian durante el uso normal. Fuente: lista acordada en clase (5-nov-2026), n.º 27.

### RF-028 · Desactivar y reactivar registros

- **Módulo:** Configuración de empresa · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá permitir al Administrador desactivar y reactivar empresas, sedes, salones, áreas, cargos y puestos.

**Regla de negocio.** Es la alternativa a borrar que exigen RF-004, RF-005, RF-007 y RNF-009. Un registro desactivado no aparece en las listas para elegir, pero su historial, visitas y evaluaciones se conservan y se pueden consultar. Fuente: propuesta del equipo (requisito implícito en la lista de clase, n.º 4, 5, 7 y 36).

## Requisitos no funcionales

### RNF-001 · Base de datos MySQL con tablas conectadas

- **Módulo:** Arquitectura · **Rol:** Todos los roles · **Prioridad:** Alta

**Descripción.** El sistema deberá guardar todos sus datos en una base de datos MySQL cuyas tablas se relacionan mediante llaves foráneas.

**Regla de negocio.** Cada dato se guarda por sede. Un puesto siempre pertenece a un salón y un análisis siempre pertenece a una foto. No se aceptan registros sueltos (sin su registro padre). Es una restricción de diseño (ISO/IEC/IEEE 29148) definida por la guía del proyecto. Fuente: lista acordada en clase (5-nov-2026), n.º 28.

### RNF-002 · Facilidad de uso

- **Módulo:** Interfaz · **Rol:** Todos los roles · **Prioridad:** Alta

**Descripción.** El sistema deberá guiar a la persona paso a paso (empresa → sede → salón → reserva → visita → puesto → fotos → ángulos → resultado → guardar → informe), mostrando en qué paso va y permitiendo volver al anterior sin perder lo escrito.

**Regla de negocio.** Los campos obligatorios se marcan antes de enviar y cada mensaje de error dice qué campo corregir. La evaluación y la subida de fotos se usan en celular o tableta sin desplazamiento horizontal. Meta: un usuario nuevo, después de una capacitación de máximo 30 minutos, completa una evaluación (de subir la foto al resultado) en 15 minutos o menos sin ayuda (propuesta del equipo). Fuente: lista acordada en clase (5-nov-2026), n.º 29.

### RNF-003 · Velocidad

- **Módulo:** Plataforma · **Rol:** Todos los roles · **Prioridad:** Media

**Descripción.** El sistema deberá completar las acciones comunes (abrir listados, guardar y generar el informe) en 5 segundos o menos con una base de hasta 1.000 registros por tabla.

**Regla de negocio.** Meta: las acciones comunes tardan 5 segundos o menos en el 95 % de 20 repeticiones, con una base de prueba de 1.000 registros (propuesta). Si una acción tarda más de 1 segundo se muestra que está cargando y el botón se desactiva para no pulsarlo dos veces. Fuente: lista acordada en clase (5-nov-2026), n.º 30.

### RNF-004 · Seguridad básica

- **Módulo:** Seguridad y acceso · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá proteger el ingreso, las contraseñas, los datos y las fotos mediante contraseñas cifradas, consultas parametrizadas, validación en el servidor y permisos revisados en el servidor.

**Regla de negocio.** Contraseñas guardadas con hash (nadie puede leerlas). Consultas con parámetros para evitar inyección SQL. Todo lo que escribe la persona se valida otra vez en el servidor y los textos se muestran escapando el HTML (para evitar código inyectado). La aplicación se conecta a MySQL con un usuario que solo tiene permisos SELECT, INSERT y UPDATE sobre la base del sistema (sin DROP ni GRANT). HTTPS cuando se publique. Las contraseñas se guardan con hash bcrypt de costo mínimo 10 y deben tener mínimo 8 caracteres, con al menos una letra y un número (propuesta del equipo). Fuente: lista acordada en clase (5-nov-2026), n.º 31. Reglas bcrypt: propuesta del equipo (Matriz v4, RNF-005).

### RNF-005 · Registro de fecha, hora y responsable

- **Módulo:** Auditoría · **Rol:** Administrador · **Prioridad:** Media

**Descripción.** El sistema deberá guardar la fecha, la hora y el usuario responsable de cada reserva, visita, evaluación, análisis y cambio de estado.

**Regla de negocio.** Sirve para ordenar el historial y saber quién creó o cambió cada registro. Se muestra al abrir una evaluación o un análisis. Fuente: lista acordada en clase (5-nov-2026), n.º 32.

### RNF-006 · Privacidad de las fotos

- **Módulo:** Seguridad y cumplimiento · **Rol:** Administrador · **Prioridad:** Alta

**Descripción.** El sistema deberá usar las fotos solo para evaluar el puesto y mostrar cada foto únicamente a los roles con permiso.

**Regla de negocio.** Si en las fotos aparecen personas, se cumple la política de la empresa y la Ley 1581 de 2012. Una persona sin permiso que abra la dirección directa de una foto ve “Acceso denegado”. Fuente: lista acordada en clase (5-nov-2026), n.º 33.

### RNF-007 · Mensajes que dicen qué pasó y qué hacer

- **Módulo:** Interfaz · **Rol:** Todos los roles · **Prioridad:** Alta

**Descripción.** El sistema deberá mostrar notificaciones en español, sin códigos de error, nombres de tablas ni texto técnico, cuando una acción sale bien, cuando hay un aviso y cuando ocurre un error, indicando qué pasó y qué hacer.

**Regla de negocio.** Si falla subir una foto o guardar, se avisa sin cerrar la sesión y sin perder lo escrito. Las confirmaciones se ven entre 3 y 5 segundos; los avisos y errores se quedan hasta que la persona los cierra. Fuente: lista acordada en clase (5-nov-2026), n.º 34.

### RNF-008 · Página web compatible con Chrome, Edge y Firefox

- **Módulo:** Plataforma · **Rol:** Todos los roles · **Prioridad:** Media

**Descripción.** El sistema deberá funcionar como página web, sin instalar nada, en las 2 últimas versiones estables de Google Chrome, Microsoft Edge y Mozilla Firefox.

**Regla de negocio.** El flujo completo (empresa, reserva, visita, evaluación e informe) se prueba en los 3 navegadores. Fuente: lista acordada en clase (5-nov-2026), n.º 35.

### RNF-009 · Conservar el historial

- **Módulo:** Operación y continuidad · **Rol:** Administrador · **Prioridad:** Media

**Descripción.** El sistema deberá conservar el historial de evaluaciones de cada puesto y desactivar o cancelar, en lugar de borrar, las empresas, sedes, salones, puestos, reservas y visitas.

**Regla de negocio.** No se borran evaluaciones antiguas sin permiso de la organización. Se hace una copia de seguridad de la base de datos y de la carpeta de fotos al menos una vez por semana (propuesta). Fuente: lista acordada en clase (5-nov-2026), n.º 36.

### RNF-010 · Exactitud del cálculo RULA

- **Módulo:** Cálculo RULA · **Rol:** Desarrollador · **Prioridad:** Alta

**Descripción.** El sistema deberá producir, para el 100 % de las combinaciones de las Tablas A, B y C, el mismo resultado que las tablas publicadas del método RULA.

**Regla de negocio.** Las tablas se guardan en un solo lugar (RF-027) y tienen pruebas unitarias automáticas. Se comparan 10 casos RULA calculados a mano con el resultado del sistema (propuesta del equipo). Fuente: propuesta del equipo (Matriz v4, RNF-004).

### RNF-011 · Mantenibilidad del código

- **Módulo:** Arquitectura · **Rol:** Desarrollador · **Prioridad:** Media

**Descripción.** El sistema deberá organizar su código en módulos separados de datos, cálculo RULA e interfaz, con el cálculo RULA en un único módulo.

**Regla de negocio.** Cada función pública tiene un comentario que dice qué hace. Los valores de las tablas RULA y del semáforo se leen de la base de datos, no se escriben en el código (RF-027). Fuente: ISO/IEC 25010:2023 (mantenibilidad), propuesta del equipo.

---

## Visión futura (próximo semestre) · NO implementar ahora

Estos 4 requisitos vienen de un modelo de 3 roles (con Evaluador) que el docente no aprobó para este semestre. Quedan documentados, pero **no se programan** y no cuentan en los 44.

- **RF-029 · Registrarse como Evaluador Independiente:** El sistema deberá permitir que una persona se registre por su cuenta como Evaluador Independiente, con nombre, número de documento, correo, teléfono, profesión y contraseña, sin estar asociada a una empresa.
- **RF-030 · Ver los resultados de la empresa en el sistema:** El sistema deberá mostrar, en la pestaña Resultados, los puestos evaluados de una empresa con su sede, salón, puntaje RULA final (1 a 7), color del semáforo, nivel oficial de RULA, fecha de la visita, sugerencias y estado de pendiente, y permitir filtrarlos por sede, área, cargo y color.
- **RF-031 · Descargar la ficha de evaluación RULA en PDF:** El sistema deberá generar en PDF la ficha de una evaluación con empresa, sede, puesto, fecha, evaluador, ángulos medidos, puntos del Grupo A y del Grupo B, puntaje final, color del semáforo, nivel oficial de RULA y sugerencias.
- **RF-032 · Comparar el antes y el después de un puesto reevaluado:** El sistema deberá mostrar, para un puesto con visita de Reevaluación, el puntaje y el color anteriores junto al puntaje y el color nuevos, con las fechas de ambas visitas.

## Filas antiguas que se ignoran

La matriz de Notion todavía tiene dos filas de la versión 4 (RNF-011 "Cierre de sesión por inactividad" y RNF-012 "Capacidad de almacenamiento de fotografías"). **No son parte de la v6.** El cierre por inactividad ya está en RF-001b; las fotos fuera de la base de datos ya están en RF-016. El RNF-011 válido es "Mantenibilidad del código".
