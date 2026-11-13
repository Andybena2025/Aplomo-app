# Paso 13D-1 · Correcciones de QA visual

13 nov 2026 · pendiente de revisión del resultado por Andy.

## Alcance y resultado

Se detuvo la Fase A de vistas previas de «Trazo vivo». Hasta su interrupción solo se habían leído archivos/documentación: **no se había creado `docs/preview_13d/` ni modificado producción**. Git estaba limpio al iniciar 13D-1; no hubo archivos temporales de aquella fase que revertir. No se añadieron panel de marca, pie, semáforo nuevo, animaciones ni otras propuestas de esa fase.

Se unificaron `.btn` y `.btn.sm` en 48 px, padding horizontal 24 px y texto 16 px; en tablas, 48 px, 20 px y 15 px. Atrás, Salir y avatar miden 44 px. Las celdas de acciones no envuelven sus botones; la tabla tiene contenedor con scroll interno si necesita más ancho. Las tarjetas mantienen sus acciones abajo a la izquierda.

Las acciones de página se agrupan en `.pagehead`: Nuevo usuario, Nueva reserva, Nueva empresa, Ver calificaciones y Generar informe. En los encabezados de Sedes/Áreas/Cargos/Salones se agrupan alta y consulta: por ejemplo, Nueva sede junto a Consultar sedes. En celular, grupos que no caben se acomodan dentro del mismo encabezado, sin generar desbordamiento.

Los encabezados usan etiquetas de sección coherentes: Gestión · Usuarios, Gestión · Empresas, Reservas · Consulta, Reservas · Detalle, Consultas · Evaluaciones, etc. Inicio conserva la fecha. Se mantienen los contextos de empresa, salón o puesto en el asistente como subtítulos, sin quitar esa información. La regla visual no participa en autorización, rutas ni cálculo.

En los dos plegables se añadió `list-style:none` y el ocultamiento de `::-webkit-details-marker`; se conserva la flecha CSS del Paso 13C. El menú a 390 px tiene seis pestañas en una fila, con texto 13 px. El estado vacío de Visitas en curso incluye «Iniciar desde una reserva» → `/reservas`, solo dentro del bloque ya reservado al Administrador.

## Requisitos y evidencia

| Requisito / encargo | Archivo y método / selector | Prueba y evidencia |
|---|---|---|
| RNF-002, punto 1: tamaños y filas de acciones | `aplomo.css`, reglas finales `:root body .btn`, botones de tablas y `.top`; `.tablewrap` y celdas de acciones | `mediciones.json`: todas las acciones renderizadas de las seis páginas miden 48 px y las del encabezado 44 px. `comprobaciones.json`: Editar/Desactivar en una sola fila a 1440/1024/390 y overflow interno configurado en auto. |
| RNF-002, punto 2: ubicación consistente | `Html.encabezado` y sobrecarga de `Html.pagina`; `PaginasUsuarios.lista`, `PaginasConsultas.lista`, `PaginasEmpresas.empresa` / `seccion`; CSS de acciones de tarjeta | Capturas antes/después; acciones junto al título. Grupos Nueva sede/Consultar sedes, Nueva área/Consultar áreas, Nuevo cargo/Consultar cargos comprobados en el DOM y capturas. |
| RNF-002 / RNF-007, punto 3: sección en eyebrow | `Html.seccion`; encabezados de consultas, pendientes, sugerencias, evaluaciones, análisis y calificaciones | Etiquetas registradas en `mediciones.json`; Inicio conserva «13 NOV 2026». `HttpConsultasTest.listasDelUsuarioNoExponenEmpresaAjenaNiAcciones` conserva todas sus comprobaciones de seguridad y actualiza únicamente el texto esperado de Pendientes. |
| RNF-002 / RNF-008, punto 4: marcador duplicado | `aplomo.css`, summary de `.consultas-secciones` / `.filtros-extra` y `::-webkit-details-marker` | Se conserva un solo indicador CSS en las capturas. `list-style:none` comprobado en la vista integrada y regla WebKit presente. No se presenta como prueba en Safari/iPad físico. |
| RNF-002, punto 5: menú móvil | `aplomo.css`, `.top .nav-tab` hasta 420 px | A 390: texto de 13 px, seis pestañas con la misma coordenada vertical, documento dentro del ancho. |
| RNF-002, punto 6: Inicio vacío | `Router.atender`, solo HTML del estado vacío del Administrador | Botón visible y href `/reservas` comprobado. Capturas Inicio; con Usuario no aparece la acción del Administrador. |
| RNF-004 / RNF-006 preservados | Misma autorización existente, sin cambios | Usuario por dirección directa a `/usuarios`: Acceso denegado; captura `usuario-denegado-390.png`. Suite completa de permisos, fotos, empresa e inactividad en verde. |

## Medidas y capturas

Se guardaron **36 capturas reales**: 18 antes y 18 después, sobre los datos del demo del Paso 13C en la base separada. Archivos `antes-{pagina}-{ancho}.png` y `despues-{pagina}-{ancho}.png`, para `inicio`, `usuarios`, `reservas`, `detalle-reserva`, `detalle-empresa` y `formulario` (Nueva empresa), y anchos 1440/1024/390. No son imágenes generadas ni HTML reconstruido. Una captura adicional comprueba el permiso denegado.

| Ancho | Seis páginas solicitadas | Alturas posteriores | Menú / tablas |
|---|---|---|---|
| 1440 | Capturadas antes y después; medidas sin desbordamiento | 48 px en acciones; 44 px encabezado | Acciones junto al título y dentro de tarjetas; Editar/Desactivar en una fila |
| 1024 | Capturadas antes y después; medidas sin desbordamiento | 48 / 44 px | Acciones de tabla en una fila; contenedor con scroll interno disponible |
| 390 | Capturadas antes y después; medidas sin desbordamiento | 48 / 44 px | Seis pestañas de 13 px en una fila; acciones de tabla sin partir |

Además, las seis páginas se midieron a 810/1080/1366/1920: **24 comprobaciones adicionales, cero desbordamientos del documento y cero alturas fuera de 48/44**. Las medidas incluyen botones renderizados debajo del primer pantallazo, no solo los visibles arriba. Las tarjetas grandes de Inicio son enlaces de tarjeta, no botones `.btn`, y conservan su diseño.

`mediciones.json` registra las 18 combinaciones anteriores, las 18 posteriores y las 24 adicionales. `comprobaciones.json` conserva filas de acciones, agrupaciones, fuente del menú y flechas. Para pseudo-elementos no soportados, el navegador integrado puede devolver el estilo del elemento al consultar `::-webkit-details-marker`; no se usa ese valor como certificación de Safari. La regla solicitada está explícitamente en el CSS.

## Verificación

`TZ=America/Bogota ./mvnw test` (con salida de rendimiento al directorio de evidencia): **718 ejecutadas y aprobadas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, 58,384 s, final 13 nov 2026 11:06:08 -05. La compilación está incluida. El primer pase detectó una única expectativa literal del eyebrow anterior «Seguimiento»; se actualizó a «Seguimiento · Pendientes», sin quitar las aserciones de empresa, acciones ni permisos, y se repitió la suite completa. No se cambió código después del pase final.

Los dos ensayos (antes/después) usaron `rula_ergonomia_pruebas`, una transacción sin confirmar y su propia carpeta temporal de imágenes de muestra. Ambos cerraron con **ROLLBACK completado**. No se cargó el demo ni se realizaron escrituras de QA en `rula_ergonomia`; no se borraron datos ni documentos. La consola registrada por el navegador integrado no tuvo errores/avisos antes de la prueba intencional de acceso denegado.

## Checklist y límites

- ✅ 718 pruebas aprobadas; solo una expectativa de HTML actualizada.
- ✅ Antes/después de las seis páginas a 1440/1024/390, con datos y medidas.
- ✅ Anchos adicionales de AGENTS.md comprobados; sin desbordamiento horizontal del documento.
- ✅ Botones de 48 px, encabezado 44 px y menú móvil 13 px; acciones de tabla en una fila.
- ✅ Estructura, paleta, tipografías y fondo del Paso 13C conservados; ninguna propuesta de marca de Trazo vivo aplicada.
- ✅ Usuario sin acceso a funciones del Administrador por dirección directa; condición de rol del nuevo CTA conservada.
- ✅ Textos en español con tildes; etiquetas de sección coherentes.
- ✅ Consola integrada sin errores en el ensayo visual normal.
- ✅ Fotos temporales revertidas; `config/db.properties` y `fotos/` fuera de Git.
- ❌ Safari/iPad físico y prueba con el dedo real: no disponibles en este paso. Se añadió la corrección CSS solicitada, sin afirmar certificación física.

Encargos 1–6 pendientes: **nada**. No se repitieron el recorrido completo en Chrome/Edge/Firefox, el PDF ni MediaPipe; conservan los límites anteriores documentados. No se descargó software ni se cambiaron lógica, servicios, DAO, SQL, rutas, permisos o cálculos. El cambio en Router es solo el HTML del estado vacío.

## Cómo comprobarlo

1. Ejecuta `TZ=America/Bogota ./mvnw test`: deben pasar 718 pruebas en la base de pruebas.
2. Abre la app y entra como Administrador. Revisa Inicio, Usuarios, Reservas, una reserva, una empresa y Nueva empresa en 1440/1024/390.
3. Comprueba los botones uniformes, Atrás/Salir/avatar más compactos y las acciones de página junto al título. En empresa, Nueva sede y Consultar sedes deben estar agrupados; las acciones de cada tarjeta quedan dentro de ella.
4. En Usuarios, Editar y Desactivar comparten fila. Si una tabla necesita más ancho, desplaza su contenedor, no la página completa.
5. A 390, comprueba las seis pestañas completas en una fila. Abre/cierra Ver otras secciones y Más filtros: debe verse un solo indicador.
6. Con un Inicio sin visitas En curso, pulsa Iniciar desde una reserva: abre Reservas. Como Usuario no aparece esa acción de Administrador; `/usuarios` sigue denegado.

## Git

Commit solicitado: `Paso 13D-1: correcciones de QA visual`. Push autorizado a `origin main` únicamente si origin coincide con `https://github.com/Andybena2025/Aplomo-app.git`. El hash y la confirmación del push se entregan tras ejecutar la comprobación. README cambia únicamente la fila del Paso 13; el número de pruebas sigue en 718. AGENTS.md registra el estado. Se espera el OK de Andy antes de otro paso.
