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

- Hechos y aprobados: Pasos 0 a 10, QA visual y QA de dispositivos. Andy aprobó el Paso 10 el 11 nov 2026.
- Correcciones previas al Paso 10 comprobadas: etiquetas «puntaje · color · Nivel», navegación en una fila desde 810 px y encabezado Seguimiento en Pendientes. Commit `6dc93c2`.
- Ajuste previo al Paso 11 comprobado: sin columna Puesto en Sugerencias de una evaluación, conservada en la lista general; espacio entre Escribir sugerencia y contador. Commit `759f5f4`.
- Paso 11 implementado y verificado: once tablas con búsqueda, filtros y orden; permisos por empresa y resultados solo Finalizados; reservas propias en sus cuatro estados. Informe: `docs/informe_paso11.md`. Pendiente del OK de Andy.
- Siguiente autorizable: Paso 12 (calificaciones e informe), únicamente después del OK de Andy.
- Después: Paso 13. El Paso 14 (IA) solo si Andy lo pide.

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
