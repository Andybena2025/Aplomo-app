# QA de dispositivos — 11 nov 2026

Se revisó el commit inicial `7d8f041` y se corrigieron los fallos observados. La revisión se realizó viendo capturas reales del navegador integrado y midiendo el DOM; no se usaron imágenes generadas ni HTML reconstruido. Son tamaños de viewport, no pruebas en dispositivos físicos ni certificación de Chrome, Edge o Firefox.

## Resultado por tamaño

| Viewport | Comprobado |
|---|---|
| 390 × 844 | Cabecera en dos filas: Atrás, logo, avatar y Salir arriba; las cinco pestañas completas abajo. Reservas y Visitas como tarjetas con empresa/sede, fechas, estado y botón legibles. Controles revisados con área mínima 44 × 44. Login sin líneas sobre el texto. |
| 810 × 1080 | Todas las pestañas, incluida Usuarios, sin deslizar horizontalmente; nombre/rol ocultos. Inicio con cuatro KPI en 2 × 2. Reservas y Visitas conservan tablas dentro del ancho disponible. Login en una columna. |
| 1080 × 810 | Cabecera completa con nombre, rol y avatar; cuatro KPI en una fila. Columnas de Inicio alineadas. Tablas y login de dos columnas dentro del ancho. |
| 1366 × 768 | Contenido centrado al máximo de 1080 px, cabecera completa, cuatro KPI en fila, tablas y login de dos columnas sin desbordamiento. |

En las pantallas Inicio, Reservas, Visitas y Login revisadas no se observó desplazamiento horizontal. Las mediciones de tablas y navegación muestran `scrollWidth == clientWidth`. El ancho del documento puede ser menor que el viewport cuando hay barra vertical del navegador.

## Correcciones y trazabilidad

- **RNF-002:** `aplomo.css`, reglas móviles `.nav` y `.nav-tab`: las cinco pestañas caben en 390 px. Controles de móvil/tablet con tamaño mínimo 44 px.
- **RNF-002 / RNF-007:** `PaginasReservas.lista`, `PaginasVisitas.lista` y `PaginasVisitas.detalle`: contenido de cada celda agrupado en `.cell-value`. La cuadrícula móvil separa etiqueta y valor; los saltos de empresa/sede y fecha/horario no forman columnas accidentales. La etiqueta Puntaje se conserva.
- **RNF-002:** `aplomo.css`, `.grid2 > section.stack + section.stack`: elimina el margen que desalineaba columnas de Inicio, conservando separación al apilarse.
- **RNF-002:** `plantillas/login.html`, función `fit`: recorta el dibujo por encima del texto en vista de una columna y lo recalcula al redimensionar. Se conservan foto, logo y animaciones del mock. La condición móvil usa el ancho del viewport, no el del panel de foto; foto y dibujo comparten posición horizontal (92% móvil, 76% escritorio).
- **RNF-008:** evidencias de adaptación a los cuatro tamaños; queda pendiente la comprobación completa en los tres navegadores exigidos por el plan.

Las capturas finales de cada pantalla están en [evidencia_qa_dispositivos](evidencia_qa_dispositivos/). `mediciones.json` conserva las dimensiones obtenidas del DOM. Los nombres de archivo indican el ancho del viewport confirmado por `innerWidth`; algunas capturas excluyen los 15 px de la barra vertical y por eso su imagen mide 375 px para el viewport de 390 px.

## Pruebas y datos

`mvn compile test`: **495 pruebas ejecutadas, 495 aprobadas, 0 fallos, 0 errores, 0 omitidas**. Surefire usa `rula_ergonomia_pruebas` y `America/Bogota`. La vista de QA también se lanzó con esa base explícita; usó cuentas, empresa, salón, reservas, visita y evaluaciones ficticias dentro de una transacción revertida al cerrarla. No hubo cambios de esquema ni escrituras de pruebas en la base real.

## Cómo repetirlo

1. Con MySQL funcionando y la configuración local existente, ejecutar `mvn compile test` desde la raíz.
2. Arrancar la aplicación con `mvn -Daplomo.puerto=8081 compile exec:java` y abrir `http://localhost:8081`.
3. En el modo responsive del navegador elegir, sucesivamente, 390×844, 810×1080, 1080×810 y 1366×768; recargar en cada tamaño.
4. Revisar Login y, con sesión de Administrador, Inicio, Reservas y Visitas. Usar registros de demostración en la base de pruebas para comprobar filas pobladas; una lista vacía no demuestra el diseño de tarjetas.
5. En 390 px abrir Usuarios desde su pestaña y abrir Ver reserva / Ver visita. Comprobar que las cinco pestañas están completas, y que estado, fecha y botón se leen sin deslizar lateralmente.
6. En 810 px verificar KPI en 2×2; en 1080 y 1366 px comprobar KPI en fila y columnas alineadas. Comparar con las capturas guardadas.

## Pendiente

No se inició el Paso 7. Se mantiene la instrucción de Andy del chat: seis condiciones RF-015 con diseño `.opt`, ayudas de RF-015 y Otras observaciones de máximo 255 caracteres; sin ocho casillas, sin altura y sin migración. Las tarjetas definitivas de Evaluaciones y Puestos en rojo corresponden al Paso 8; este QA conserva los contadores actuales de Inicio. No se verificó el acceso desde un iPad/celular físico por la red local ni gestos táctiles reales.


## Actualización · Paso 13B · 12 nov 2026

El registro anterior describe el estado de los Pasos 6/QA del 11 nov; su apartado «Pendiente» y su ancho máximo de 1080 px son históricos. El avance actual figura en AGENTS.md. El Paso 13A ya amplió `.wrap` a 1240 px; no se repitieron sus correcciones ni las pruebas del Paso 14.

### Recorrido real en los tres navegadores

Se reutilizaron las copias oficiales temporales existentes, **sin descargar ni instalar navegadores**. Versiones verificadas en los metadatos de cada aplicación; los nombres antiguos de sus carpetas no indican la versión actual. Servidor temporal local 8087, clases reales de la aplicación, reloj real de America/Bogota, conexión explícita a `rula_ergonomia_pruebas`. Se cargó el SQL demo dentro de una transacción reversible y se usaron fotos de muestra generadas con IA en una carpeta temporal. Nada de este ensayo se cargó en la base real.

| Navegador | Recorrido comprobado el 12 nov | Resultado |
|---|---|---|
| Chrome for Testing 156.0.8078.80 | Login → Empresas → universidad y sus tres sedes → nueva reserva 11:00–12:00 → aceptar → iniciar visita → nueva evaluación → condiciones → consentimiento → subir JPG de perfil → detección de seis puntos → ajustes → resultado → guardar → informe de empresa → Salir | ✅ Evaluación nueva guardada una vez; resultado 3 · Amarillo · Nivel 2. Informe disponible y salida vuelve al login. |
| Microsoft Edge 157.0.4341.46 | Mismo recorrido con reserva 10:00–11:00 y evaluación nueva con JPG | ✅ Detección de seis puntos, resultado 3 · Amarillo · Nivel 2, guardado, informe y salida correctos. |
| Firefox 160.0 | Mismo recorrido con reserva 09:00–10:00; ángulos manuales 61/47/0/37/17, actividad marcada y carga menor de 2 kg | ✅ Detección inicial también disponible; resultado manual 4 · Amarillo · Nivel 2, guardado, informe y salida correctos. |

Las tres visitas nuevas se dejaron En curso en la transacción de ensayo. El informe mostró la visita Finalizada del SQL demo y sus tres colores; **no se afirmó que mostrara resultados de las visitas En curso**. La selección de carga en Firefox requirió recuperar el foco de su menú nativo; la evaluación se guardó con el valor observado, sin atribuirle el caso Rojo. No se añadieron términos con HTML al título de las posturas del recorrido.

Evidencias: [Chrome resultado](evidencia_paso13b/chrome-resultado.png), [Edge resultado](evidencia_paso13b/edge-resultado.png), [Firefox resultado](evidencia_paso13b/firefox-resultado.png), e informes `chrome-informe.png`, `edge-informe.png`, `firefox-informe.png` en la misma carpeta. En el informe se vio «Generado el 12 nov 2026 … · America/Bogota», con la hora real.

### Carga, errores y permisos

- Se añadieron demoras **solo al servidor temporal de QA**, nunca al producto: 8 segundos al guardar evaluación / abrir informe y 4 al filtrar reservas. En los tres navegadores Guardando… quedó desactivado hasta responder. Generar informe cambió a Cargando… y se protegió contra otra activación. El filtro mostró Cargando… desactivado. Capturas `*-guardando.png`, `chrome-informe-cargando.png` y `filtros-cargando.png`.
- Revisión de avisos de formularios, sesión, fotos, evaluación, pendientes, sugerencias e informe. El formulario con codificación dañada ahora responde con una instrucción en español, sin el mensaje técnico de URLDecoder; prueba HTTP incluida. También se tradujeron las fallas de conexión que podían mostrar «Failed to fetch» / «NetworkError» en reservas, análisis y evaluación; el ensayo JS de red fallida comprueba conservación de campos y reintento. Las confirmaciones desaparecen en 4–5 segundos; advertencias y errores permanecen hasta cerrarlos. El trabajo previo de carga no se reimplementó.
- Se inició sesión de Usuario y se escribió a mano `/usuarios` y `/informes/empresa/24414` (empresa diferente): ambos mostraron Acceso denegado. Evidencias `usuario-acceso-denegado.png` y `usuario-otra-empresa-denegado.png`. Las pruebas HTTP existentes siguen comprobando fotos y resultados En curso protegidos.
- La consola capturada por el navegador integrado no registró errores ni avisos durante el recorrido visual de Inicio, Reservas y Usuarios. La consola visible de Chrome durante la emulación de Usuarios estaba vacía. No se afirma haber registrado automáticamente toda la consola de Edge/Firefox.

### Tamaños y tacto

Se revisaron Inicio, Reservas y Usuarios con datos, a 390, 810, 1080, 1366 y 1920 px de ancho, altura 900 px, en el navegador integrado. Los 15 pares pantalla/tamaño no desbordaron horizontalmente; [anchos.json](evidencia_paso13b/anchos.json) conserva `innerWidth` y el ancho real del documento. Las capturas tienen nombres `inicio-390.png`, `reservas-810.png`, `usuarios-1920.png`, etc. Se conserva el diseño del Paso 13A: cabecera, marfil, tipografías, tarjetas de Inicio y tabla responsive de Usuarios.

Además, Chrome DevTools en modo **Mobile**, a 390 y 810 px, activó el puntero táctil emulado. Se abrieron Usuarios y sus filtros; los botones de tabla, navegación, opciones, avatar y controles pequeños reciben un mínimo 44 × 44 px bajo `@media(pointer:coarse)`; campos y selects ya tienen mínimo 44 px. Capturas `chrome-tactil-390.png`, `chrome-tactil-usuarios-390.png` y `chrome-tactil-usuarios-810.png`. Se restauró el tamaño del navegador integrado y se cerraron las ventanas privadas de ensayo.

**Límites:** esta es emulación, no una prueba con el dedo sobre un celular o tableta físicos. RNF-008 pide las dos últimas versiones estables de cada navegador: este recorrido comprueba exactamente las tres versiones indicadas, no certifica las seis combinaciones ni su condición de «últimas» versiones. No se descargaron otras para ampliar esa cobertura. La revisión de impresión/PDF ya realizada en el Paso 12 no se repitió aquí.

### Pruebas, rendimiento y limpieza

`TZ=America/Bogota ./mvnw test`: **718 aprobadas; 0 fallos, 0 errores, 0 omitidas**, 66 segundos, terminado el 12 nov 2026 a las 15:25:40 -05. Incluye el rendimiento pendiente del Paso 13: 20 repeticiones de listado / guardado / informe, 1.000 filas en cada una de 16 tablas operativas; P95 de 172,77 / 38,47 / 164,00 ms, respectivamente. Los catálogos oficiales RULA conservaron su tamaño y contenido. [Datos de rendimiento](evidencia_paso13b/rendimiento.json).

El cierre del servidor temporal confirmó **ROLLBACK completado** y retiró únicamente su carpeta temporal de fotos. Las empresas, reservas, visitas, evaluaciones y cuentas de este recorrido no persisten. Las fotos reales y `config/db.properties` permanecieron fuera de Git. No hubo cambios de esquema, borrado de documentos ni push.


## Correcciones · Paso 13C · 13 nov 2026

Reservas, Usuarios, detalle de evaluación e informe revisados visualmente a 1440/1024/390 px en el navegador integrado. Sin desbordamiento horizontal; «Ver reserva» en una línea y 36 px de alto en escritorio; 44 px móvil. Plegables cerrados: 46 px con bordes (summary de 44 px), sin hueco; abren y cierran con clic/teclado. Fotos completas centradas y limitadas a 520/360 px. Fondo sin texto, tablas opacas y seis pestañas en una fila a 390. Comprobaciones adicionales de Reservas/Usuarios a 810/1080/1366/1920 y de Pendientes/Sugerencias a 1024/1440, sin desbordamiento.

El demo opcional ahora tiene dos visitas Finalizadas (principal y Alcázar), cuatro evaluaciones y calificación general 2,35, con «2 de 3 sedes calificadas · falta Sede Anova (Medicina)». Idempotencia y caso Verde de Alcázar comparados con MotorRula. 718 pruebas aprobadas; consola registrada sin errores antes de la prueba intencional de acceso denegado. Datos de QA y carpeta temporal de fotos revertidos al cerrar; base real sin carga demo. Informe y límites: [informe_paso13c.md](informe_paso13c.md); capturas y medidas en [evidencia_paso13c](evidencia_paso13c/). No se descargaron navegadores ni se repitió el recorrido de tres navegadores del Paso 13B.


## Correcciones · Paso 13D-1 · 13 nov 2026

Antes/después en seis páginas (Inicio, Usuarios, Reservas, detalle de reserva, detalle de empresa y Nueva empresa) a 1440/1024/390. Las 18 combinaciones posteriores muestran únicamente acciones de 48 px y encabezado de 44 px; las filas Editar/Desactivar comparten coordenada vertical. Menú de seis pestañas a 13 px en una fila a 390. Acciones junto al título, Nueva sede junto a Consultar sedes, y CTA en Inicio vacío. También se midieron las seis páginas a 810/1080/1366/1920: cero desbordamientos del documento y cero alturas fuera de 48/44.

718 pruebas aprobadas después de actualizar solo una expectativa HTML que esperaba la etiqueta anterior de Pendientes. Consola integrada sin errores antes de la prueba intencional de acceso denegado. Pruebas visuales sobre `rula_ergonomia_pruebas`, con ROLLBACK confirmado antes y después; no se cargó el demo en la base real. Fase A de Trazo vivo cancelada; no se aplicaron sus cambios de marca ni se crearon sus archivos. Ver [informe_paso13d1.md](informe_paso13d1.md) y [evidencia_paso13d1](evidencia_paso13d1/). Los límites de equipo físico y Safari/iPad real siguen documentados.
