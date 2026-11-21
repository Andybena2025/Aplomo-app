# Paso 25 · ALINEO., Quiénes somos y Centro de ayuda

Marca visible ALINEO., mismo ícono A y lema; conservados co.aplomo, archivos, variables, cookie, sessionStorage, repositorio y NIT de práctica. No se modifica el nombre persistido de la empresa de práctica: solo su presentación. Quiénes somos usa los cuatro textos entregados por Andy, debajo de la portada; /nosotros redirige sin crear sesión. Ayuda pública solo ingreso/contacto y temas según rol; niveles leídos de MySQL y buscador sin tildes. Sin JavaScript, todas las preguntas autorizadas permanecen disponibles.

| Requisito | Archivo / método | Comprobación |
|---|---|---|
| Identidad, RNF-007/011 | Html, login.html, PaginasCalificaciones, textos de práctica | HttpRecorridoMarcaTest: páginas de los tres roles, texto/title/aria-label sin marca anterior; identificadores conservados |
| Quiénes somos, RNF-002 | login.html, nosotros.js, Router | Textos exactos, /nosotros 303 sin cookie, volver da foco a Correo |
| RF-002, ayuda por rol | Ayuda.consultar, PaginasAyuda.pagina | Público 1/10; Usuario 1/2/5/6/8/10; Evaluador sin 9; Administrador todos |
| RF-027 | CalificacionesDao.riesgos | Cambiar nombre/acción en MySQL cambia Ayuda; HTML escapado |
| Contacto privado | Contacto.leer, .gitignore, plantilla local | Archivo completo/parcial/ausente, correo/WhatsApp seguros, datos escapados y respaldo sin inventar |
| RNF-002/007 | ayuda.js, nosotros.js, aplomo.css | 5 JS de filtro; mediciones DOM y búsqueda real con/sin coincidencias |

Pruebas antiguas ajustadas: HttpMarcaTest y Paso17Test cambian el texto de marca; HttpMarcaTest admite Proyecto Nuclear en la sección pública, quita Atrás de Usuarios y verifica el semáforo contra los datos actuales (la demo del Paso 24 agrega resultados). No se cambió ninguna expectativa del motor. El recorrido de marca comprueba por rol listas, detalles, formularios, resultado, informe, práctica, cuenta y páginas públicas; se preservan las restricciones del Usuario.

QA numérica, sin capturas ni carpetas de evidencia: 75 combinaciones finales de página/rol/ancho a 360/390/820/1024/1366 px reales, todas 0 px de desborde. Incluye login#nosotros y Ayuda pública (10), Administrador: ayuda/agenda semana/mes/usuarios/formulario/reserva con nota/historial (35), Evaluador: ayuda/Mis evaluaciones con una fila/agenda semana/mes/reserva con nota (25), Usuario: ayuda y cabecera (5). Botones, pestañas y avatar visibles medidos: mínimo 44 px. La matriz anterior del Paso 24 comprobó además 25 combinaciones, también sin desborde.

| Portada | Borde inferior de Entrar | Visible inicialmente | Desborde |
|---|---:|---|---:|
| 360×740 | 739,12 px | Sí, también después de cargar fuentes | 0 px |
| 390×844 | 765,89 px | Sí | 0 px |
| 430×932 | 806,58 px | Sí | 0 px |
| 844×390 | 455,96 px | Requiere desplazamiento vertical, como la presentación horizontal previa | 0 px |

Búsqueda real: CONTRASENA encuentra contraseña; sin coincidencias muestra el aviso. Volver a iniciar sesión enfoca #correo. 0 errores de consola capturados. Se cerró el servidor temporal de QA y se confirmó ROLLBACK. No se ensayó iPad físico, Windows, otros navegadores, movimiento reducido físico ni impresión nueva. Contacto real pendiente de completar en el archivo local: no se inventó correo/teléfono ni se creó ese archivo con datos reales.

Durante el cierre se detectaron y corrigieron tres detalles del Paso 24: empates de fecha con ID descendente; autor histórico determinado por el primer CREAR, igual en lista y acceso directo; agenda prefiere al Evaluador activo si permanece una asignación antigua de una cuenta inactiva. Tres pruebas nuevas: historia de empresa inactiva ya pasó; las otras dos fallaron antes y pasaron tras corregir. Singular de una nota nueva corregido.

Cierre conjunto: suite completa Maven ejecutada UNA vez: 1022 casos, 1021 aprobados inicialmente y un fallo en la apertura directa de calificaciones del Usuario de una sola empresa. Se conservó esa conducta anterior usando cuenta_empresa; ese caso se repitió y pasó en el proyecto final. Estado final: 1022 casos Maven verificados, 0 fallos pendientes, 0 errores y 0 omitidas; 119/119 JS aprobadas. El cierre de alcance y navegación sumó 53 comprobaciones dirigidas aprobadas; no se repitió la suite completa. No hay cambios de esquema en este paso, ni de co.aplomo.rula, el bloque POST /evaluaciones/calcular, IA o fotos de galería. Los informes anteriores se conservaron como historial.

El cierre también limita los catálogos de Mis evaluaciones a IDs de sus propias evaluaciones (incluidas empresas retiradas), aplica el alcance en SQL a catálogos activos y Evaluar ahora, y distingue Atrás por tipo de página, conservándolo incluso en una empresa llamada «Empresas». HttpHistorialInactivosTest añade esa comprobación; HttpTresRolesTest comprueba que no aparecen puestos ajenos en los filtros históricos. No cambia el contrato de los endpoints.

App normal restaurada en http://localhost:8080: /login y /ayuda 200, /nosotros 303 a /login#nosotros sin cookie, scripts nuevos 200. Base real migrada con respaldo privado en Paso 24; no se cargó demo ni se crearon cuentas reales. Contacto privado y respaldos fuera de Git. Commits anteriores: bb5f6eb y 37cb05e; el commit de este informe identifica el Paso 25.
