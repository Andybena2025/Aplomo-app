# Paso 40 · Resultado explicado y plan de atención

24 de noviembre de 2026. Implementado sobre main actualizado con PR #10.

## Qué se hizo

1. El lema usa Barlow, peso 500 y espaciado máximo .12em en ingreso, cabecera, pie y riel. La serif queda en el logo.
2. Resultado de ancho completo: un anillo principal, niveles de la base, posturas con anillos de 46 px y desgloses cerrados; comparación con la última evaluación Finalizada, figura humana curva con mapa de riesgo, partes ordenadas por peso, efectos posibles y plan de seis columnas. En celular el plan se convierte en tarjetas. La proyección se calcula en el servidor con el MotorRula y las tablas MySQL existentes.
3. Se cambió el texto de «sujeciones» por «soportes», incluyendo la explicación de la prensa. Solo se actualizan coincidencias exactas del texto antiguo; una sugerencia ya editada se conserva.
4. Género, edad y antigüedad son opcionales y desmarcables. Se guardan con consultas preparadas, se validan en el servidor y solo son visibles para Administrador/Evaluador según una constante. No participan en puntaje ni proyección.

## Requisitos y pruebas

| Requisito | Archivo y método | Comprobación |
| --- | --- | --- |
| RNF-007, lema y textos claros | `aplomo.css`, bloque Resultado 40; `resultado40.js`, renderizado | QA de estructura y revisión del CSS; texto nuevo validado por Migracion40Test |
| RF-027, RNF-010, cálculo y proyección del servidor | `PlanAtencion40.proyectar`, `explicar`, `orden`, `color`; `Resultados40.previsualizar` | PlanAtencion40Test: 6 casos; HttpResultado40Test: comparación de proyección y puntaje |
| RF-020, explicación del resultado | `Resultado40Web.atender/pagina`; `resultado40.js` | qa_resultado40.cjs: 7 casos; recorrido completo en navegador |
| RF-023, plan y estados de sugerencias | `Sugerencias`, `SugerenciaDao.marcasPlan`, `Resultados40.guardados` | HttpResultado40Test: guardar, editar, Hecha y reanalizar sin duplicados; HttpSugerenciasTest |
| RF-002, alcance y privacidad | `Trabajador.para`, `Resultados40.guardados`, `Resultado40Web` | Usuario propio Finalizado permitido; En curso/ajeno y previsualización denegados; HttpTresRolesTest conserva historial propio de solo consulta |
| RNF-004, validación y persistencia | `Trabajador.leer`, constructor; `EvaluacionDao`; migracion_paso40.sql | Edad/ENUM inválidos; datos distintos del trabajador no alteran cálculo; Migracion40Test |
| RF-026, informe conservado | Generación de informe existente, sin cambios | HttpInformesTest: 11 casos aprobados; datos del trabajador ausentes para Usuario |
| RNF-002, adaptación de pantalla | `aplomo.css`, bloque r40; `revision-visual.js` | Medidas DOM reales a 1440×900, 1280×800 y 390×844; estructura JS |

## Decisiones confirmadas y límites de la proyección

Andy confirmó las metas exactas del motor: «Muñeca neutra, hasta 5°»; «Tronco erguido, hasta 5° (o sentado bien apoyado)»; cuello 0°–10°, codo 60°–100°, brazo −20°–20°. Se concentran en `PlanAtencion40.METAS` para cambiarlas en un lugar.

Los colores por parte y los textos de efectos están marcados como propuesta, en funciones/mapas únicos. El orden usa puntos/máximo; hasta tres segmentos por encima del mínimo llevan «PESA MÁS».

La proyección conserva actividad, carga y giro independiente de muñeca. Cambia únicamente los segmentos del plan y sus ajustes. Si el puntaje no baja, se informa que se mantiene. Si el riesgo viene solo de actividad/carga y todos los segmentos están en su mínimo, no se inventan filas ni reducciones: se explica y se ofrece agregar sugerencia a quien tiene permiso. Es una estimación; la reevaluación confirma el resultado real.

La comparación anterior consulta la última evaluación Finalizada: el valor de caché del puesto puede ya contener un análisis En curso y no sirve para representar el resultado previo. La ficha de reserva se abre con empresa y puesto autorizados ya preparados.

Cada cambio del sistema nuevo se guarda como sugerencia real. Las marcas de bitácora conservan su identidad aunque se edite su título o descripción; se mantienen estados y no se duplican al reanalizar. En historia sin sugerencia registrada se indica ese hecho en vez de inventar un estado. Las sugerencias manuales van al final. Un Evaluador que perdió la asignación conserva su historia propia de solo consulta, sin controles de seguimiento ni reserva.

El JSON complementario `/evaluaciones/explicacion` tiene permiso y CSRF. El contrato del bloque protegido `/evaluaciones/calcular` no se cambió. La restricción de datos del trabajador se aplica en el servidor, incluyendo JSON; no depende de ocultarlos con CSS.

## Migración e instalación

- `scripts/migrar_paso40.sh` ejecutado en `rula_ergonomia` y `rula_ergonomia_pruebas`, con respaldo privado previo en `config/respaldos/`.
- Ambas bases tienen las tres columnas nuevas; edad nullable con CHECK 14–99.
- Migracion40Test ejecutó el SQL dos veces, comprobó las tres columnas y el mismo número de tablas, y verificó el reemplazo exacto del texto en reglas/sugerencias sin tocar textos editados.
- Instalación y diccionario actualizados; script Windows entregado, no ejecutado en Windows.
- No se borraron datos, fotos ni cuentas; no se reinició la demo. La base real solo recibió la migración autorizada y el reemplazo exacto de texto.

## Pruebas finales

Comandos ejecutados con `TZ=America/Bogota`:

- `mvn test`: **1194 ejecutadas, 1193 aprobadas, 1 fallida, 0 errores, 0 omitidas**, 3 min 28 s.
- Única excepción autorizada: `DemoPlan28Test.planTieneAgendaNaturalPuntajesVariadosYPocosPendientes:55`, `expected: <23> but was: <24>` (fecha de la demo).
- `node --test scripts/qa_*.cjs`: **185 ejecutadas y aprobadas; 0 fallidas**, aproximadamente 293 ms.
- Regresión dirigida posterior a las primeras correcciones: **39/39**. Se corrigieron la documentación del endpoint, una expectativa de mensaje anterior y el acceso de solo consulta al historial propio. La suite completa final ya incluye esas correcciones.
- `LongitudFuentesTest` aprobado; diferencias sin errores de espacios; paquete del motor y bloque protegido de cálculo comparados con HEAD: sin cambios.

## Revisión de interfaz sin capturas

Se recorrió una evaluación completa con dos posturas y detección real de las muestras; datos opcionales, previsualización, guardar y terminar, abrir resultado guardado y cambiar una sugerencia a Hecha. El resultado guardado conservó las dos posturas, el contexto y el estado real. Todo el recorrido se hizo en la base de pruebas y se revirtió al cerrar la QA.

| Tamaño | Desborde horizontal exterior | Textos recortados detectados | Anillos de postura | Botones del pie |
| --- | ---: | ---: | ---: | ---: |
| 1440×900 | 0 px | 0 | 46 px | 44 px |
| 1280×800 | 0 px | 0 | 46 px | 44 px |
| 390×844 | 0 px | 0 | 46 px | 44 px |

Un solo anillo principal grande; desgloses cerrados al abrir; cero errores de consola capturados. Las mediciones geométricas proceden del navegador integrado: las pruebas JS de estructura no sustituyen esas mediciones.

## Checklist y lo que no se hizo

- ❌ Maven completamente verde: queda únicamente el fallo de fecha que Andy permitió conservar; 1193 aprobadas.
- ✅ JavaScript: 185/185.
- ✅ Orden/diseño del nuevo resultado comprobado con el encargo; sin capturas por instrucción expresa.
- ✅ Sin deslizamiento lateral en los tres tamaños solicitados para este paso.
- ❌ 810/1080/1366 exactos no repetidos en este paso; no se presentan como comprobados.
- ❌ Tacto/iPad físico, navegadores externos y Windows no certificados; quedan para Andy/Claude.
- ✅ Usuario limitado a su empresa y visitas Finalizadas, sin trabajador ni fotos; accesos ajenos denegados en HTTP.
- ✅ Textos en español; mensajes escapados y datos inválidos rechazados.
- ✅ Cero errores de consola en el recorrido comprobado.
- ✅ Sin contraseñas, respaldos, configuración privada ni fotos subidos.
- ✅ Motor, tablas/textos de niveles y bloque protegido de cálculo intactos; PDF sin cambios.

No se generaron capturas, GIF ni archivos de evidencia. No se modificó la lógica de MediaPipe ni sus umbrales.

## Cómo probarlo

1. Refresca ALINEO con Cmd+Shift+R e inicia sesión como Evaluador autorizado o Administrador.
2. Abre una visita En curso y una evaluación; deja opcionalmente datos del trabajador, prueba desmarcar y vuelve a elegir.
3. Completa dos posturas; revisa que el resultado tome la más crítica y que ambos anillos pequeños midan lo mismo.
4. Abre/cierra Ver desglose; revisa el mapa corporal, el orden del plan, las metas y la proyección.
5. Guarda y termina. Abre la evaluación: cambia una sugerencia a Hecha y vuelve a abrirla para comprobar persistencia.
6. Con Usuario, consulta la misma evaluación cuando la visita esté Finalizada: no debe mostrar contexto del trabajador, fotos ni acciones de edición; otra empresa o En curso devuelve Acceso denegado.
7. Revisa el lema en login/cabecera/pie/riel y la nota nueva de soportes donde corresponda.

## Entrega Git

Un commit del Paso 40 a main, con push al origen verificado `https://github.com/Andybena2025/Aplomo-app.git`. El hash final y la confirmación de reinicio se entregan en el mensaje de cierre; no se adelanta el siguiente paso.
