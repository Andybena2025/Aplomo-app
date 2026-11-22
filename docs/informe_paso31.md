# Paso 31 · Contacto, encabezados de detalle y últimos ajustes

21 de noviembre de 2026. Base: `fea42dc`, igual a `origin/main`; no había Paso 30C remoto.

## Qué se hizo

0. Contacto local actualizado a los cinco valores ficticios indicados por Andy. Se conservaron las dos líneas `contacto.empresa.*`; el archivo sigue ignorado y no entra al commit. `Contacto.leer` respalda cada campo vacío o de ejemplo; la ficha de Administradora usa `contacto.correo` y `contacto.telefono`, sin confundirlos con los campos de empresa. Los valores privados válidos siguen prevaleciendo; los enlaces inválidos siguen rechazados. El destinatario interno se resuelve en el servidor: correo → Administrador activo cuyo nombre contiene Gabriela/Gabriella → primer Administrador activo. Se conservan CSRF, permisos y escape; no se crea ninguna cuenta.
1. `Html.detalle` comparte el regreso, título, estado, metadatos y acciones en Archivo de empresa/puesto, Visita, Reserva, Empresa y Mi cuenta. La franja muestra la sección y navegación, sin Atrás ni acciones propias; la etiqueta del rol queda solo en Inicio. Mi cuenta regresa a Inicio, porque no existe un listado de cuentas personales. La consulta autorizada de visita añade únicamente nombre/color del evaluador mediante SELECT; no se modifican datos ni permisos. El historial del puesto mantiene activa la pestaña Archivo, sin confundirla con gestión de puestos.
2. Archivo: cuatro tarjetas, números de 32 px y etiquetas de 13 px, fecha única si coincide el periodo, evaluador trasladado a metadatos, barra y leyenda sin partir. Lugar muestra Todas; los tres filtros miden 44 px y conservan sus límites alineados con las tarjetas.
3. Usuarios muestra primera empresa con puntos suspensivos y +N; title conserva toda la lista escapada. Logo público de 56 px, 16 px debajo de Todos los temas, pista del puesto bajo su título y resultado a la derecha. Inicio administrativo oculta el segundo punto bajo 1180 px. Riel derecho: encuadre ajustado solo en el SVG lateral generado; el dibujo y la marca izquierda se centran por su contenido visible, conservando la plomada animada.

## Requisitos y pruebas

| Requisito | Archivo y método | Comprobación |
| --- | --- | --- |
| RF-002/033, RNF-007/011 · contacto útil y destinatario autorizado | `Contacto.leer`, `Avisos.principalEn`, `Avisos.escribirPrincipal` | `HttpPaso31Test.contactoVacioSeVePublicoYConSesionYRespaldaEjemplosPorCampo`, `mensajeUsaCorreoLuegoGabrielaLuegoPrimerAdministradorActivo`; envío reversible y destinatario inyectado ignorado |
| RF-024, RNF-002/007 · detalle sin duplicados y permisos | `Html.detalle`, `Html.pagina`, presentadores Archivo/Visitas/Reservas/Empresas/Cuenta; `VisitaDao.buscar` | `detallesCompartenUnTituloEnContenidoUnRegresoYAlcanceDelServidor`, tres roles; Usuario recibe 403 en otra empresa y visita En curso |
| RF-024, RNF-007 · cifras/periodo y navegación | `PaginasArchivo.indicadores`, `historial`; `cuenta.js` | `archivoTieneCuatroTarjetasUnaFechaYCuentaNumericaSinRepetirEtiqueta`; tres casos nuevos en `qa_formularios.cjs` |
| RF-003, RNF-002 · empresas asignadas compactas | `TablaUsuarios.pagina` | `usuarioConDosEmpresasTieneResumenYListaCompletaEscapada` |
| RNF-002/011 · escala y rieles | `aplomo.css`, `InicioWeb.usuario`, `CentroAyuda.inicio`, `Marca.lateral` | Medidas DOM descritas debajo; `LongitudFuentesTest` |

## Resultado de comprobaciones

- Suites completas ejecutadas una sola vez al cierre: **1121 casos Maven** (1119 aprobados inicialmente y dos corregidos y aprobados después en cierre dirigido) y **150/150 JS**, antes **1114/147**. Resultado verificado: **1121/1121 casos Maven**, sin fallos pendientes, errores u omitidas. No se repitió la suite completa. Siete casos Maven y tres JS nuevos. Las pruebas previas 30A/B se adaptaron exclusivamente a la presentación aprobada y al respaldo de contacto.
- Pruebas dirigidas durante desarrollo: contacto, avisos, encabezados, autorización y longitud. Un caso inicial de Evaluador tenía autoría de otro evaluador en la ficha de prueba: se corrigió únicamente esa preparación reversible, sin ampliar permisos de producción. En la suite completa, dos casos de historial exigían la navegación anterior o detectaron la pérdida del estado de sede: se adaptó el regreso al nuevo diseño y se restauró ese metadato, con Activo/Inactivo en el puesto. Cierre dirigido: 27/27 (historial, Paso 31 y longitud).
- 48 combinaciones DOM: Inicio Administrador, Archivo empresa, Visita, Empresa, Mi cuenta, Usuarios, Reserva y Archivo puesto, cada una a 1920×1080, 1366×950, 1180×740, 1024×1290, 820×1100 y 390×844. **0 px de desbordamiento exterior en todas**. Los seis detalles tienen un h1 en main y un solo regreso. Regreso y acciones de detalle: **44 px**. En Archivo, cuatro tarjetas en la misma fila en las cinco anchuras de escritorio/tableta; en celular se apilan. Tras ajustar la tarjeta de resultado: **0 px de desbordamiento interior** de las cuatro tarjetas en los seis tamaños.
- Rieles a 1920×1080, unión de los rectángulos de los grupos SVG visibles: izquierda **470,65 px**, derecha **474,45 px**, diferencia **0,81 %**; centros Y **540,00** y **540,01 px**. La medición no usa el alto de las cajas, que ambas conservan 520 px.
- Ayuda: logo **56 px**, margen bajo Todos los temas **16 px**. Se comprobó nombre/cargo/correo/teléfono y enlaces de Gabriela sin abrir correo ni WhatsApp.
- Inicio Administrador a 820 px: cuatro filas revisadas, punto izquierdo visible y segundo oculto. Inicio Usuario a 1366/820/390: cuatro resultados por tamaño, pista en una línea debajo del título y resultado a la derecha, **0 px** de desbordamiento de pista/página. **0 errores de consola capturados**.
- QA de navegador exclusivamente en una transacción de `rula_ergonomia_pruebas`, con ROLLBACK confirmado al cerrar. Las suites también usan esa base. No se reinició ni modificó la demo real.
- Se reinició la app normal en 8080: curl a `http://localhost:8080/ayuda` respondió **200**, con los cinco datos indicados (nombre, cargo, correo, teléfono y enlace WhatsApp). No se inició sesión ni enviaron mensajes con cuentas reales.

## Lo que no se hizo / límites

Sin cambios al motor RULA, cálculo protegido, IA, fotos/imágenes de evaluación, SQL/esquema, datos de demo, cuentas ni contraseñas. No capturas ni carpetas de evidencia, como pidió Andy. No se descargaron navegadores. No se certifica Safari/iPhone/iPad físico, tacto, Windows ni impresión PDF; tampoco se repitieron exactamente 810 y 1080 px. La revisión del navegador integrado fue por estructura y medidas DOM, sin inspección visual mediante capturas.

## Checklist

- ✅ Maven 1121 y JS 150 aprobados; compilación y regla de 120 columnas.
- ✅ Encabezados, cuatro métricas y escala pedidos comprobados por HTML/DOM; sin capturas por instrucción explícita.
- ✅ 390/820/1024/1180/1366/1920 sin desplazamiento lateral; ❌ 810/1080 exactos no repetidos.
- ❌ Prueba física con el dedo no realizada; los regresos y acciones revisados miden 44 px.
- ✅ Usuario restringido por servidor: otra empresa y visita En curso reciben 403.
- ✅ Textos españoles y contenido dinámico escapado; 0 errores de consola capturados.
- ✅ Contacto privado, conexión y fotos excluidos del commit; archivos protegidos comparados con la base.

## Cómo lo prueba Andy

1. Refresca ALINEO en el navegador; abre Ayuda y revisa la ficha de Gabriela. Sin sesión también aparece en `/ayuda`.
2. Abre Archivo y una empresa: arriba debe decir Archivo; abajo solo un título, estado, ubicación/evaluador, un regreso y acciones. Repite con un puesto, una Visita, Reserva, Empresa y Mi cuenta.
3. Revisa las cuatro métricas y los filtros. En Usuarios, una cuenta con varias empresas muestra +N y la lista completa al dejar el cursor encima.
4. En Inicio Usuario verifica la pista bajo cada puesto; en Administrador a tamaño iPad, un solo punto por fila de Equipo esta semana.

## Entrega

Un único commit `Paso 31` y push a `origin/main` del repositorio verificado `Andybena2025/Aplomo-app`. El hash y el resultado de curl se comunican al terminar. Se espera la revisión de Andy antes de continuar.
