# CLAUDE.md · APLOMO. (software real)

Lee este archivo completo antes de escribir código. Es la memoria del proyecto: aquí no hay chats anteriores, solo lo que está en este repositorio.

## Qué estamos construyendo

**APLOMO.** es un sistema web para evaluar posturas en puestos de trabajo con el método **RULA**. Es el **Proyecto Nuclear** de Ingeniería de Software (3.er semestre) de la Corporación Universitaria Empresarial Alexander von Humboldt, periodo 2026-2.

- Equipo: Laura Gabriella Romero Gómez, Johan Andrés Zapata Quintero, Andrés Eduardo Benavides (Andy).
- Clienta: docente experta en Métodos y Movimientos (Ingeniería Industrial).
- Docente que califica requisitos y programación: **Ing. Arle Morales Ortiz**. Si hay duda entre dos modelos, manda lo que él aprobó.
- Fase actual: **Implementación** (modelo en cascada: los requisitos y el diseño ya están cerrados).

## Fuente de verdad (en este orden)

1. `docs/02_requisitos_v6.md`: los **44 requisitos** (33 RF + 11 RNF) de Notion "Proyecto Nuclear 6.0".
2. `docs/base_de_datos/Script_MySQL_RULA.sql` + `Diccionario_de_Datos.txt`: la base de datos **ya diseñada** (21 tablas, 3FN). Úsala tal cual. Si necesitas cambiarla, pregunta primero y explica por qué.
3. `docs/diagramas/`: casos de uso aprobados (CU5-01 a CU5-29), clases, objetos, dominio, actividades, estados, secuencia y E-R. La especificación de los casos de uso también está en texto: `Especificacion_Casos_de_Uso_completa.txt`.
4. `docs/03_metodo_rula.md`: reglas exactas del cálculo.
5. `docs/referencia/mock_app/`: el **prototipo (mock app)** que ya se presentó. Es la referencia **visual y de flujo**, no de reglas (ver diferencias abajo).

## Decisiones cerradas (no las cambies sin preguntar)

- **Lenguaje:** Java 17 o superior. **Base de datos:** MySQL 8.
- **Sin frameworks ni ORM** (lo exige la guía del proyecto): nada de Spring, Hibernate, JPA, React ni Bootstrap. Se permite:
  - el servidor HTTP que trae el JDK (`com.sun.net.httpserver.HttpServer`);
  - JDBC con `PreparedStatement`;
  - HTML, CSS y JavaScript puro;
  - **Maven** solo para las dependencias: `mysql-connector-j`, `jbcrypt` (org.mindrot) y `junit-jupiter` para las pruebas.
- **Es una página web** (RNF-008: Chrome, Edge y Firefox), no una app de escritorio.
- **Dos roles** (casos de uso aprobados):
  - **Administrador**: el Ingeniero Industrial independiente. Hace todo: cuentas, empresas, reservas, visitas, evaluaciones, pendientes, sugerencias e informe.
  - **Usuario**: el contacto de la empresa. Solo inicia sesión, cierra sesión, cambia su contraseña y consulta, busca y filtra los datos de **su propia empresa**, sin botones de crear ni editar.
- Los **permisos se revisan en el servidor** en cada petición, no solo escondiendo botones (RF-002).
- **Nada se borra.** Se desactiva o se cancela (RF-028, RNF-009). El usuario de MySQL de la app no tiene permiso DELETE.
- Contraseñas con **bcrypt, costo ≥ 10**; mínimo 8 caracteres, con al menos una letra y un número (RNF-004).
- **Todas** las consultas SQL con `PreparedStatement`. Escapar el HTML de todo texto que viene del usuario.
- Las **tablas RULA y el semáforo se leen de la base de datos** (`valor_tabla_rula`, `nivel_riesgo`), no se escriben en el código (RF-027, RNF-011). El cálculo vive en **un solo módulo** con pruebas unitarias (RNF-010).
- Las fotos se guardan en una **carpeta del servidor** con nombre único; la base guarda solo la ruta. Solo los roles con permiso pueden abrirlas; un Usuario nunca ve fotos (RNF-006).
- Textos de la interfaz en **español**, claros y sin códigos técnicos (RNF-007).
- El informe PDF (RF-026) se hace con una página HTML preparada para imprimir y "Guardar como PDF" del navegador. No hace falta ninguna librería de PDF.
- La interfaz **se ve como el mock app**: estética "Obsidian & Ivory", modo oscuro, tipografías Instrument Serif + Inter. Copia los tokens de `docs/07_estilo_visual.md`.

## Diferencias entre el mock app y lo que hay que programar

El mock app se hizo antes de que se aprobara el modelo final. **Copia su aspecto y su flujo, pero no estas reglas:**

| Tema | Mock app (no copiar) | Software real (hacer así) |
|---|---|---|
| Roles | Administrador, Evaluador y Usuario | Solo **Administrador** y **Usuario** |
| Ingreso | Sin contraseña, botones "Entrar como" | Correo + contraseña bcrypt, bloqueo tras 5 intentos por 15 min, contraseña temporal, cierre a los 30 min sin actividad |
| Datos | En el navegador (localStorage) | En MySQL |
| Puntos del cuerpo | La IA (MediaPipe) los pone sola | El Administrador marca 3 puntos por ángulo y puede moverlos o escribir el ángulo a mano (RF-017). La IA es opcional y va al final, si sobra tiempo |
| Condiciones del puesto | 8 casillas propias | Las 6 casillas de RF-015 + observaciones |
| Fotos | 3 tomas fijas (perfil, frente, detalle) | De 1 a 10 fotos JPG/PNG de hasta 5 MB, cada una con vista y descripción; casilla de aceptación; "Sin registro fotográfico" (RF-016) |
| Actividad y carga | Separadas para los grupos A y B | Un solo valor por análisis, como en la tabla `analisis_rula` |
| Ficha PDF por evaluación | Sí | No (RF-031 es visión futura). Sí el **informe general de la empresa** (RF-026) |

## Fuera de alcance (no programar)

RF-029, RF-030, RF-031 y RF-032 (marcados "Visión futura"). Rol Evaluador. Registro libre de cuentas. Las filas viejas RNF-011 "Cierre de sesión por inactividad" y RNF-012 "Capacidad de almacenamiento" de la versión 4.

## Cómo trabajar

- Sigue el plan por pasos de `docs/06_plan_de_implementacion.md`. **Un paso a la vez**: al terminar, explica qué hiciste, cómo probarlo y espera el OK de Andy antes del siguiente.
- Explica en español sencillo, como a un estudiante de 3.er semestre: Andy quiere aprender, no solo recibir código.
- Cita el requisito en cada clase o método importante (por ejemplo `// RF-018`). Cada método público lleva un comentario que dice qué hace (RNF-011).
- Antes de decir que algo funciona, compílalo y pruébalo (`mvn test` y, si aplica, abre la página).
- Si algo de los requisitos no está claro o choca con el diseño, **pregunta**; no inventes reglas nuevas. Lo que esté marcado "(propuesta)" se implementa tal cual, pero fácil de cambiar.
- Nunca subas contraseñas reales al repositorio. La conexión va en `config/db.properties` (está en `.gitignore`); usa `config/db.properties.ejemplo` como plantilla.

## Mapa de docs

- `docs/00_LEEME.md`: índice.
- `docs/01_vision_y_alcance.md`: qué es, roles, las 4 fases y lo pendiente con la clienta.
- `docs/02_requisitos_v6.md`: los 44 requisitos completos.
- `docs/03_metodo_rula.md`: reglas de puntos, tablas, carga y semáforo.
- `docs/04_flujo_y_pantallas.md`: pantallas por rol y el asistente de evaluación.
- `docs/05_arquitectura.md`: paquetes, capas, rutas, sesiones y seguridad.
- `docs/06_plan_de_implementacion.md`: los pasos en orden, con cómo probar cada uno.
- `docs/07_estilo_visual.md`: colores, tipografías y componentes del mock.
