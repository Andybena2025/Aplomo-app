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

- Hechos y aprobados: Pasos 0 a 8, QA visual y QA de dispositivos. Andy aprobó el Paso 8 antes de autorizar el Paso 9.
- Paso 9 implementado y verificado: pendientes automáticos por evaluación roja, seguimiento editable, cierre Arreglado, historial, filtros y consulta por empresa. Informe y límites: `docs/informe_paso9.md`. Pendiente del OK de Andy.
- Siguiente autorizable: Paso 10 (sugerencias), únicamente después del OK de Andy.
- Después: Pasos 11 a 13. El Paso 14 (IA) solo si Andy lo pide.

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
- **Detección automática con IA** (MediaPipe) = Paso 14, solo si Andy lo pide.

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
