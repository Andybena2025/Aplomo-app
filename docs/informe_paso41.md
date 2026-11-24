# Paso 41 · Sitio más compacto y Resultado con pestañas

24 de noviembre de 2026. Base: main `da5c74e`.

## Cambios

- Nueva hoja `static/css/densidad.css`, cargada después de `aplomo.css` en el HTML común y el ingreso. No hay `portada-orden.css` enlazada actualmente; la prueba comprueba también ese orden si aparece. La referencia versionada evita reutilizar una copia anterior del recurso.
- Densidad solo en pantalla desde 900 px: texto base de 15 px, títulos y espacios más pequeños, cabecera de 128,30 px, acciones de 42 px desde 1280 y 44 px en tableta. Textos auxiliares de al menos 13 px; el pie de 12 px es la excepción que el encargo pide expresamente.
- Pie de 34 px, sticky, con el lema a la izquierda y los derechos a la derecha. Se reserva espacio debajo del contenido; la barra de acciones del asistente queda encima del pie.
- Resultado con tres pestañas accesibles: selección única, flechas izquierda/derecha, Inicio/Fin, foco visible, relación tab/panel, persistencia por dirección en sessionStorage con try/catch y prioridad de `?pestana=plan`. Guardar estado vuelve al mismo Resultado con Plan abierto; las demás páginas de Sugerencias conservan su destino.
- Resultado compacto: anillo principal de 78 px, posturas de 46 px en el grupo derecho a partir de 1280, escala a lo ancho, trabajador y comparación anterior en una franja, plan con filas más bajas. Los textos y datos completos siguen presentes.
- Opciones del trabajador con altura automática, texto en varias líneas y mínimo 56 px, incluso en celular. No se cambia lo que se selecciona o guarda.

## Requisitos

| Requisito | Archivo / punto de cumplimiento | Prueba |
| --- | --- | --- |
| RNF-007, densidad y lectura | `densidad.css`, reglas de pantalla desde 900 px | Medidas DOM de las seis páginas, cabecera y Resultado |
| RNF-008, hoja común | `Html.pagina`, `plantillas/login.html` | `HttpDensidad41Test`, páginas públicas, seis vistas principales, evaluación y CSS servido |
| RNF-007/008, pestañas accesibles | `resultado40.js`, `render` e `iniciar`; `revision-visual.js`, `resultado` | Cinco casos JS nuevos y uso real del teclado en navegador |
| RNF-007, estado del plan | `sugerencias.js`, respuesta de guardado 201 | Guardado real en pruebas: permanece `/evaluaciones/N?pestana=plan`, estado Hecha y pestaña Plan; prueba de memoria |
| RNF-007/008, opciones legibles | `densidad.css`, `.trabajador-opciones button` | 0 botones/etiquetas recortados a 1440×900 y 1180×820 |
| RNF-010, cálculo conservado | Sin cambios en motor, servicios ni endpoint de cálculo | `HttpResultado40Test` y comparación de archivos protegidos |

## Mediciones antes → después

Se midieron las mismas páginas con el mismo escenario ficticio y el mismo número de registros en `rula_ergonomia_pruebas`. Las alturas originales del encargo corresponden a otros datos: no se usan como si fueran mediciones de esta sesión. `scrollHeight` nunca baja del alto de la ventana.

| Administrador, 1440×900 | Antes | Después | Reducción |
| --- | ---: | ---: | ---: |
| Inicio | 1209 | 944 | 265 px |
| Empresas | 975 | 900 | 75 px |
| Visitas | 1116 | 900 | 216 px |
| Reservas | 1110 | 900 | 210 px |
| Usuarios | 1192 | 942 | 250 px |
| Archivo | 900 | 900 | 0 px |

Inicio conserva 44 px de desplazamiento vertical y Usuarios 42 px en este escenario: sus listas/tarjetas completas ocupan más que la ventana. No se esconden registros ni se reducen textos para forzarlas a caber. El pie permanece visible en ambas.

| Tamaño | Pie visible en Inicio / Empresas / Visitas / Reservas / Usuarios / Archivo | Desborde horizontal |
| --- | --- | ---: |
| 1440×900 | Sí en las 6 | 0 px |
| 1180×820 | Sí en las 6 | 0 px |
| 2240×1260 | Sí en las 6 | 0 px |

El borde inferior del pie coincide con 900 / 820 / 1260 px respectivamente; la cabecera mide 128,30 px en las vistas medidas. El logo A se conserva.

### Resultado de práctica obtenido del recorrido real

Se completaron las dos posturas con fotos de ejemplo y MediaPipe real, confirmando las respuestas en la interfaz. Caso obtenido: postura más crítica 4, segunda postura 1; cuatro segmentos por encima del mínimo. No se guardó esta evaluación; el cambio de estado se probó aparte sobre una evaluación ficticia del mismo escenario.

| Tamaño | Pestaña 1, seis partes | Pestaña 3, cuatro filas | Desborde horizontal |
| --- | ---: | ---: | ---: |
| 1440×900 | 900 px | 900 px | 0 px |
| 1180×820 | 886 px | 967 px | 0 px |
| 2240×1260 | 1260 px | 1260 px | 0 px |

La meta explícita de 900 px en portátil se cumple con pasos, resultado, botones y pie. En iPad horizontal estas dos pestañas aún requieren 66 y 147 px de desplazamiento: las tarjetas de postura pasan debajo del bloque principal y los controles conservan su tamaño táctil. No se presenta ese caso como si cupiera sin bajar.

En el plan de cuatro filas no se detectaron textos recortados. Las pestañas muestran un único panel cada vez, los desgloses siguen cerrados y el teclado cambia selección y foco. Guardar estado mantiene Plan abierto incluso al recargar.

### Puesto, celular e ingreso

- Paso Puesto a 1440×900 y 1180×820: **0 botones o etiquetas** con `scrollWidth > clientWidth` o `scrollHeight > clientHeight` (tolerancia de redondeo 1 px). «Prefiere no decir» y «Menos de 1 año» completos. Tarjetas de género: 63 px; antigüedad: 56 px.
- Inicio a 390×844: **2108 → 2108 px**, diferencia 0 px. La densidad no se aplica al celular; la corrección de las tarjetas y las pestañas son los cambios explícitos que sí se aplican donde corresponden.
- Ingreso sin sesión: `login-pie` termina en 823,22 / 797,30 / 1003,22 px para las ventanas 1440×900 / 1180×820 / 2240×1260: visible sin bajar en las tres.
- Cero errores de consola capturados durante el recorrido. Sin capturas ni archivos gráficos de evidencia.

## Impresión y mejora progresiva

El render deja las tres secciones completas en el HTML, sin `hidden`, y la barra oculta antes de activar el controlador de pestañas. La activación es la que oculta paneles. En impresión se fuerzan los tres paneles visibles y se recuperan sus encabezados; la barra se oculta. El PDF de empresa y el cálculo no se modifican.

Esto no convierte el asistente existente en un cálculo sin JavaScript: generar el resultado del asistente ya requería JS antes de este paso. No se certificó un recorrido completo con JS desactivado ni el diálogo de impresión físico.

## Pruebas y límites

Comandos con `TZ=America/Bogota`:

- `mvn test`: **1195 ejecutadas, 1194 aprobadas, 1 fallida, 0 errores, 0 omitidas**, 3 min 28 s.
- Único fallo: `DemoPlan28Test.planTieneAgendaNaturalPuntajesVariadosYPocosPendientes:55`, `expected: <23> but was: <24>`, excepción de fecha permitida por Andy.
- `node --test scripts/qa_*.cjs`: **190/190**, 0 fallidas, omitidas o canceladas; 395 ms.
- Nuevos casos: 1 Maven y 5 JavaScript. Dirigidas finales: Maven 7/7, JS de resultado 12/12.

La primera prueba dirigida coincidió con la transacción abierta de QA y produjo esperas de bloqueo MySQL. Se cerró el escenario con ROLLBACK; repetida sin concurrencia: **7/7**, incluyendo HttpResultado40Test y LongitudFuentesTest. No se cambió ninguna regla para resolver esa espera.

Se usó el navegador integrado con sus controles Playwright. Playwright/Chromium independiente no estaba disponible; no se descargó nada. No se certificaron Safari/Chrome/Edge/Firefox externos, iPad físico, tacto, Windows ni impresión física.

## Checklist

- ❌ Maven completamente verde: 1194/1195; solo queda la excepción de fecha autorizada. JavaScript 190/190.
- ✅ Nuevas pruebas de pestañas, memoria, teclado, impresión y retorno a Plan.
- ✅ Resultado de cuatro filas y seis partes cabe en 900 px a 1440×900.
- ✅ Pie visible en 18 combinaciones de listas/tamaños e ingreso en los tres tamaños.
- ✅ Inicio móvil conserva su altura; sin desborde horizontal en los tamaños comprobados.
- ❌ Inicio y Usuarios no caben completos en 900 px; Resultado no cabe completo en iPad horizontal, con las cifras indicadas arriba.
- ❌ No se certificaron tacto físico ni otros navegadores; sin capturas por encargo.
- ✅ Sin cambios de base, reglas Java, motor RULA, endpoint de cálculo ni datos reales.
- ✅ Sin configuración privada, claves, fotos o respaldos subidos.

## Cómo revisarlo

1. Refresca ALINEO con Cmd+Shift+R. Abre Inicio y las cinco listas; el pie debe permanecer visible.
2. En una evaluación abre Puesto y revisa las dos etiquetas largas del trabajador.
3. Completa las dos posturas y cambia las tres pestañas con clic y con flechas.
4. Abre un resultado guardado con `?pestana=plan`, cambia un estado y guarda: debe volver al mismo plan con el estado actualizado.
5. Prueba el resultado en una ventana de 1440×900; después cambia a celular y comprueba que Inicio conserva su disposición.

## Entrega

Commit `Paso 41 · sitio más compacto y Resultado con pestañas` en main. Hash y estado final de la app en el mensaje de cierre; no se inicia el siguiente paso.
