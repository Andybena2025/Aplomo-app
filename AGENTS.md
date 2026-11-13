# AGENTS.md · Reglas de APLOMO. (léelas SIEMPRE antes de trabajar)

Este archivo es para cualquier asistente de código (Codex, Claude u otro). Complementa a `CLAUDE.md`; si algo choca, manda lo que diga Andy en el chat.

## Proyecto

APLOMO. es el software real del Proyecto Nuclear (evaluación ergonómica con el método RULA). Fecha límite del PMV: 27 de noviembre de 2026.

Fuente de verdad, en este orden:
1. Lo que Andy dice en el chat.
2. `docs/02_requisitos_v6.md` (44 requisitos).
3. `docs/03_metodo_rula.md` (reglas exactas del cálculo).
4. `docs/06_plan_de_implementacion.md` (pasos en orden).
5. `CLAUDE.md` y el resto de `docs/`.
6. El mock: `docs/referencia/mock_app/app.src.html` y `docs/referencia/mock_app/capturas/` (referencia visual y de comportamiento).

Base de datos: `docs/base_de_datos/Script_MySQL_RULA.sql` (21 tablas, base `rula_ergonomia`). No cambies la base sin permiso de Andy.

## Estado (actualízalo al terminar cada paso)

- Hechos y aprobados: Pasos 0 a 12, QA visual y QA de dispositivos. Andy aprobó el Paso 12 el 11 nov 2026.
- Correcciones previas al Paso 10 comprobadas: etiquetas «puntaje · color · Nivel», navegación en una fila desde 810 px y encabezado Seguimiento en Pendientes. Commit `6dc93c2`.
- Ajuste previo al Paso 11 comprobado: sin columna Puesto en Sugerencias de una evaluación, conservada en la lista general; espacio entre Escribir sugerencia y contador. Commit `759f5f4`.
- Paso 11 implementado y verificado: once tablas con búsqueda, filtros y orden; permisos por empresa y resultados solo Finalizados; reservas propias en sus cuatro estados. Informe: `docs/informe_paso11.md`. Aprobado por Andy el 11 nov 2026.
- Paso 12 implementado y verificado: calificaciones por sede, general con mínimo dos sedes calificadas, cobertura, informe imprimible y PDF nativo. Informe: `docs/informe_paso12.md`. Aprobado por Andy el 11 nov 2026.
- Correcciones previas al Paso 13 comprobadas: resumen de empresa en la primera página, rótulo histórico por sede, hora real de Bogotá, singular/plural y nota de impresión. Informe: `docs/correcciones_previas_paso13.md`.
- Paso 13A implementado: correcciones visuales del profesor Arle, tabla responsive de Usuarios, accesos de Inicio y accesibilidad. Ver `docs/informe_paso13a.md`. Subido a origin/main hasta `eae771e`; pendiente de su revisión individual. El Paso 13 general sigue sin cerrar.
- Paso 13 autorizado por Andy para continuar después del commit separado de correcciones. El Paso 14 (IA) solo si Andy lo pide.
- Paso 14 autorizado explícitamente por Andy el 12 nov 2026: detección MediaPipe del mock implementada, con revisión manual, carga limitada y salida manual ante fallo. Ver `docs/informe_paso14.md`; subido a origin/main hasta `eae771e` y pendiente de su revisión individual. Los avisos de carga y la prueba de rendimiento que estaban sin commit se integraron en el Paso 13B.

- Paso 13B implementado y comprobado el 12 nov 2026: carga con bloqueo de doble envío, mensajes claros, SQL demo opcional e idempotente, rendimiento con 1.000 registros en 16 tablas, 718 pruebas aprobadas y recorrido completo en Chrome 156, Edge 157 y Firefox 160. Ver `docs/informe_paso13b.md` y `docs/qa_dispositivos.md`. QA y demo solo en `rula_ergonomia_pruebas`, revertidos al cerrar. Aprobado explícitamente por Andy en el chat y subido a origin/main hasta `eae771e`. No se certifican dispositivos físicos ni las dos últimas versiones estables de cada navegador; no se descargaron nuevas copias.

- Paso 13C implementado y comprobado el 13 nov 2026: botones de consulta en una línea (36 px escritorio, 44 px móvil), plegables cerrados sin huecos, fotos limitadas y centradas, fondo sin etiquetas ni transparencia en tablas y menú móvil en una fila. Demo manual con utf8mb4 y segunda visita Finalizada en Alcázar: general 2,35 y cobertura 2 de 3 sedes. 718 pruebas aprobadas; QA reversible solo en `rula_ergonomia_pruebas`. Ver `docs/informe_paso13c.md`. Andy autorizó commit y push a `origin main` del repositorio `Andybena2025/Aplomo-app`; pendiente de su revisión del resultado. Los límites de QA físico y dos versiones estables del Paso 13B continúan documentados.

## Reglas que no se negocian

1. **Un paso de `docs/06` a la vez.** Nunca empieces el siguiente sin el "OK" de Andy.
2. **Se ve y se comporta IGUAL al mock:** colores, letras, barra superior, botones, tarjetas, etiquetas de estado con color, fechas "20 nov 2026", animaciones y textos con tildes. Si dudas, copia el HTML/CSS/JS del mock y adáptalo. No inventes diseños.
3. **Funciona sin deslizar de lado** en 390 px (celular), 810 px (iPad vertical), 1080 px (iPad horizontal), 1366 px (laptop) y 1920 px (escritorio), con mouse y con el dedo. Botones de al menos 44 px en celular.
4. **RULA:** tolerancia NEUTRO = 5° del mock (muñeca ≤5° = 1, tronco ≤5° = 1, cuello y tronco con ≤5° hacia atrás = flexión). El cálculo vive solo en `MotorRula` / `PuntuadorSegmentos` / `MedidorAngulos` / `TablasRula`; las tablas se leen de MySQL. Lo que se guarda lo calcula el servidor.
5. **Roles:** Administrador (hace todo) y Usuario (solo consulta su empresa; nunca ve fotos ni botones de crear o editar). Los permisos se revisan en el servidor, no solo escondiendo botones.
6. **Seguridad:** `PreparedStatement` siempre, HTML escapado, CSRF en cada POST, bcrypt, fotos con nombre aleatorio en `fotos/` (en `.gitignore`), nunca subir `config/db.properties`.
7. **Tecnologías:** solo Java 17+, HttpServer del JDK, JDBC, MySQL, jBCrypt, JUnit 5, HTML/CSS/JS puro. Sin frameworks ni librerías nuevas.
8. **Pruebas** en la base `rula_ergonomia_pruebas`, nunca en la real. Todo en `America/Bogota`.
9. **NUNCA escribas que "Andy decidió/confirmó/aprobó"** algo que no dijo en el chat. Si algo no está en los docs o hay contradicción, PREGUNTA antes de hacerlo.
10. **Nunca digas que algo "funciona" o "se ve igual al mock" sin haberlo comprobado.** Si no pudiste verlo, dilo.
11. **Nada se borra** de la base: se desactiva o se cancela.

## Decisiones de Andy ya tomadas

- **Condiciones del puesto (RF-015):** las 6 de la tabla `condiciones_puesto` (escritorio muy alto, pantalla muy alta, pies colgando, apoyapiés, silla con apoyabrazos, silla regulable) + "Otras observaciones" (máx. 255), con el diseño del mock (casillas `.opt` y cuadro de observaciones) y las definiciones de RF-015 como ayuda. NO las 8 casillas del mock ni el campo de altura. NO cambiar la base.
- **Consentimiento (RF-016, aclarado por Andy en el chat):** sin aceptación, `persona_acepto=false` y `registro_fotografico=SIN_REGISTRO_FOTOGRAFICO`; el servidor rechaza cualquier foto. No se pide motivo de ausencia de fotos. `evaluacion.motivo` es solo la elección de la postura. Con aceptación: `persona_acepto=true`, `CON_FOTOS` y 1–10 JPG/PNG de hasta 5 MB, cada una con vista y descripción de máximo 200 caracteres. Sin cambios de esquema.
- **Tarjetas de Inicio** (a partir del Paso 8): Visitas en curso, Reservas en cola, Evaluaciones y Puestos en rojo (número en rojo).
- **Tolerancia de 5°** del mock (ver regla 4). Andy NO aprobó quitarla.
- **Pendientes (Paso 9):** un pendiente por evaluación. Andy eligió conservar el máximo de los análisis rojos de esa evaluación y mantener descripción, prioridad, fecha límite, fecha de registro y estado, incluso Arreglado. Otra evaluación roja genera otro pendiente sin borrar el anterior.
- **Sugerencias (respuesta de Andy, 11 nov 2026):** automáticas con estado PENDIENTE, fecha límite vacía y origen SISTEMA. Prioridad inicial (propuesta): ALTA para Rojo y MEDIA para Amarillo o Verde. Tipo fijo por condición en `Sugerencias.TIPOS_POR_CONDICION`: mobiliario para las seis condiciones RF-015; POSTURA para reglas que solo dependen del segmento (`SIEMPRE`). Título del inicio de `texto_sugerido`, máximo 120 caracteres y sin cortar palabras. Sin cambiar el esquema.
- **Sugerencias e historial (Andy, 11 nov 2026):** si no hay reglas para un Amarillo/Rojo, guardar el análisis y pedir que el Administrador agregue una; no permitir quitar la última activa. Al reanalizar una evaluación, añadir solo reglas nuevas, manteniendo las editadas, Hechas o quitadas. Cuando todas las activas del puesto estén Hechas, el Administrador registra una reserva de Reevaluación mediante el formulario de RF-011 (CU5-26).
- **Correcciones visuales (Andy, 11 nov 2026):** riesgo siempre «puntaje · color · Nivel»; `.nav{flex-wrap:wrap;overflow-x:visible}` solo hasta 720 px. A 810, 1080, 1366 y 1920 px, una sola fila. Pendientes usa la etiqueta contextual Seguimiento.
- **Consultas (respuesta de Andy, 11 nov 2026):** el Usuario consulta todas las reservas de su empresa (Esperando, En cola, Finalizadas y Canceladas con su motivo), sin controles de escritura. «Solo Finalizadas» aplica a resultados: visitas, evaluaciones, puntajes, pendientes y sugerencias En curso permanecen ocultos, también por dirección directa, incluso con análisis Rojo guardado. Las fotos siguen reservadas al Administrador.
- **Calificación general (Andy, 11 nov 2026; propuesta):** promediar solo sedes con calificación y únicamente con al menos dos calificadas (`Calificaciones.MINIMO_SEDES_GENERAL`). Con cero o una, «Sin calificación general»; con una sola sede registrada, indicar que no hay general. Siempre mostrar cobertura y nombres de sedes faltantes, también en el informe.
- **Estadísticas del informe (Andy, 11 nov 2026):** visitas y fotos analizadas de todas las visitas Finalizadas; cada puesto una sola vez, máximo de los análisis de su última evaluación Finalizada, nunca máximo histórico. Una Reevaluación Verde cuenta como Verde. Mantener los promedios históricos de visita. Pendientes por su propio estado, abiertos/arreglados separados; sugerencias activas por tipo y estado.
- **Informe (Andy, 11 nov 2026):** primera página con resumen de EMPRESA y totales globales; después detalle por sede. Rótulo de sede «Promedio histórico de sus visitas Finalizadas», sin cambiar la regla. «Generado el» usa hora real de Bogotá; singular/plural correcto y nota para desmarcar Encabezados y pies de página.
- **Detección automática con IA** (MediaPipe) = Paso 14, solo si Andy lo pide.

## README e instalación (11 nov 2026)

- `README.md` es la portada del producto para el equipo y el docente. Al terminar un paso, actualiza **solo** la fila de ese paso en la tabla "Avance (hoja de ruta)" y el número de pruebas. No agregues párrafos por paso: el detalle va en `docs/informe_pasoN.md`.
- `INSTALAR.md` y `scripts/` (instalar_mac.sh, instalar_windows.bat, iniciar.sh, iniciar.bat, crear_base_pruebas.sh) son la instalación del equipo. Si cambias algo que afecte la instalación (nueva tabla de datos, puerto, cuenta inicial), actualízalos en el mismo paso.
- El proyecto trae Maven Wrapper (`./mvnw`, `mvnw.cmd`): úsalo en las instrucciones en lugar de `mvn`.

## Al terminar CADA paso entrega este informe (obligatorio)

1. Qué hiciste, en español sencillo.
2. Tabla: cada RF/RNF del paso → archivo y método donde se cumple → prueba que lo comprueba.
3. Lo que NO hiciste o quedó a medias (si no hay, escribe "nada").
4. Checklist con ✅ o ❌:
   - `mvn test` en verde (número de pruebas)
   - se ve como el mock (qué pantallas comparaste, con capturas en `docs/evidencia_*`)
   - 390 / 810 / 1080 / 1366 px sin deslizar de lado
   - funciona con el dedo
   - el Usuario no puede hacerlo (probado escribiendo la dirección a mano)
   - textos con tildes y sin errores de ortografía
   - sin errores en la consola del navegador
   - nada sensible subido (`config/db.properties`, `fotos/`)
5. Cómo lo pruebo yo, paso a paso.
6. `git log --oneline` de los commits nuevos y confirmación del push.
7. Actualiza la sección "Estado" de este archivo.

Luego ESPERA el OK de Andy.
