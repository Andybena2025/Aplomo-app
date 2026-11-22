# Paso 29 · Listas cortas, escala común y reinicio de demo

21/11/2026. Base de trabajo: main ce274cb. Un commit «Paso 29» y push a main autorizados.
Sin capturas, carpetas de evidencia ni descargas. El ancho máximo de main sigue en 1180 px.

## Requisitos y comprobación

| Punto / requisito | Implementación | Prueba |
|---|---|---|
| 1 · RNF-002/007, escala | aplomo.css: filas 18/15, encabezados 13, hora/puntaje 26, menú 15; botones 44/36 y chips 32; campos 44/16. Mínimo de texto 13 salvo lema del riel de 12 expresamente pedido. | Medidas DOM; qa_filtros_visual verifica que ninguna regla de .chip declare mínimo >32. |
| 2 · RF-024, tablas | FilasTrabajo.fila/cabecera, TablaEmpresas, TablaUsuarios, PaginasArchivo y columnas CSS fijas: títulos y líneas secundarias con elipsis y title, sin repetir empresa; nombres sin Ing.; Puestos y Resultado; posturas debajo; próxima visita solo E; flecha sin encabezado. | HttpListas29Test, HttpConsultasTest y medidas DOM de celdas. |
| 3 · RF-024, filtros | FiltrosLista.mostrar, TablasTrabajo.herramientas y sistema-visual.js: buscador/selectores en fila, chips/leyenda en otra fila con desplazamiento interior; sin Más filtros ni texto de envío automático; Estado incluye inactivas; limpiar solo si hay filtro. | HTTP de consultas y qa_filtros_visual; estado, persona, búsqueda y cambios de selector conservan el límite. |
| 4 · RF-024, diez filas | ListasCortas.preparar/boton y sistema-visual.js: HTML inicial <=10; Ver 10 más; total de coincidencias completo y reinicio al filtrar. Visitas: hasta tres en curso, finalizadas del mes por defecto y mes anterior; Reservas: hoy a +14 días; Pendientes por vencimiento y luego puntaje. | HTTP con 50 visitas/50 reservas/30 pendientes en A y E; JS con 50 filas, incremento a 20 y filtro de vuelta a 10; cambio de mes real en navegador. |
| 5 · RNF-002/007, marca/Inicio | Html.cabecera/iniciales/nombreSinTitulo, InicioWeb y CSS: rieles grandes desde 1440, sin marca grande junto al saludo ni logo diminuto del pie; nombre/rol en avatar; saludo con primer nombre; cuatro acciones Admin en fila; un resultado peor por puesto en Inicio U y sin repetir su única empresa. | HttpInicio29Test y HttpListas29Test; DOM: main 1180 a 1920; acciones 4×44 en la misma fila; menú sin recortes; iniciales AB. |
| 6 · RF-020/022/025/033, demo | Calendario sin leyenda Día pasado. DemoPresentacion.DIAS cambia los dos días 30 por 26/25. ResultadosDemo usa días de octubre por persona, inicios 07:00–15:30, :00/:30 y duraciones 1–3 h. Casos simulados pasan por los servicios y el motor existentes; no se asignan resultados a mano. | DemoPlan28Test: 23 visitas, máximos 9 Verde / 7 Amarillo / 7 Rojo (39/30/30 %), puntajes 2–7; 24 citas de noviembre, ocho por E; cinco pendientes abiertos con fechas hoy−2/+3/+8/+15/+25 y prioridad Alta/Media. DemoTest comprueba horarios, idempotencia y cuentas. |
| 7 · RF-002/003, RNF-009/011, reinicio | Demo.reiniciar + VinculosDemo: savepoint, guardar enlaces por NIT, desvincular, vaciar/cargar y restaurar en la misma transacción. Conserva cuentas, claves y asignaciones activas/inactivas, incluidas asignaciones externas de las cuentas vinculadas. Mantiene confirmación escrita y MySQL administrador en el script. | DemoReinicio29Test con root solo en rula_ergonomia_pruebas: nuevo ID de empresa, mismo Usuario/hash y vínculo; empresa ajena conservada. ROLLBACK final. Reinicio REAL pendiente de la frase exacta de Andy después del commit. |
| 8 · RNF-011, regresión | HttpListas29Test, HttpInicio29Test, DemoReinicio29Test y qa_filtros_visual; adaptadas solo aserciones de presentación antiguas de Más filtros y de mezcla por puesto. | Suites completas una vez al final; resultado numérico en cierre. |

## Medidas sin capturas

Navegador integrado, datos demo generados en una transacción de `rula_ergonomia_pruebas` y revertidos al terminar.
No se inició sesión con contraseñas reales. No se comprobó Safari físico, iPad/iPhone ni Windows en este paso.

| Ancho × alto | Páginas/roles revisados | Desborde exterior | Desborde de tablas |
|---|---|---:|---:|
| 1920×1080 | Inicio y listas A/E/U | 0 px | 0 px |
| 1180×1080 | Inicio y listas A/E/U | 0 px | 0 px |
| 1024×1080 | Inicio y listas A/E/U | 0 px | 0 px |
| 390×1080 | Inicio y listas A/E/U | 0 px | 0 px (tarjetas adaptables) |

76 combinaciones iniciales: A en Inicio/Reservas/Visitas/Pendientes/Archivo/Empresas/Usuarios; E/U en las mismas,
salvo Usuarios por sus permisos. Ajustadas columnas que recortaban Resultado/Prioridad en Pendientes, Estado en
Archivo y Por confirmar en Reservas E; vueltas a medir sin cortes. Botones 44 px, tabla 36 px; chips 32 px.
Con demo: Visitas iniciales 10 de 13 de noviembre; Ver 10 más deja 13; Mes anterior vuelve a 10 fechas de octubre.
Riel izquierdo: longitud de marca 560 px; A derecha 150 px a 1920×1080. Cuatro acciones Admin de 44 px en una fila.
Menú E en una fila desde 1180; menú A medido completo a 1920. Ninguna captura tomada.

## Lo que NO se hizo

- No se cambió motor RULA, cálculo protegido, IA, fotos/imágenes de evaluación ni esquema. Sin migraciones.
- No se cambió el ancho de main, ni se borraron cuentas. No se descargaron navegadores ni se guardaron contraseñas.
- No se reinició la demo de rula_ergonomia: exige la confirmación exacta «REINICIAR DEMO» después del commit.
- No hay nuevos números de la base REAL mientras falte esa confirmación. Los 23/9/7/7 son datos de PRUEBAS.
- La paginación limita las filas visibles; conserva en el navegador el conjunto autorizado para filtrar sin recargar.
  No se añadió una API de paginación ni se cambiaron los permisos del servidor.

## Checklist y prueba manual

- ✅ 10 filas iniciales, más filas y contadores reales; filtro devuelve a 10.
- ✅ Tablas fijas y controles compactos medidos; nombres y líneas secundarias legibles con title.
- ✅ Inicio y cabecera comunes; iniciales sin títulos y resultado único por puesto.
- ✅ Demo con resultados y plazos variados; restauración de enlaces probada con ROLLBACK.
- ✅ Pruebas solo en rula_ergonomia_pruebas; sin cambios de archivos protegidos.
- ⏳ Confirmación de Andy y posterior carga de demo en real, recuento SQL y reinicio de app.
- ⏳ Revisión física de Andy; navegadores externos no comprobados.

Para revisar: abrir Visitas, comprobar el mes activo, pedir más filas, cambiar a Mes anterior y buscar; repetir filtros
por evaluador y Estado en Reservas/Pendientes/Empresas. En Usuarios revisar chips de rol y empresa. Entrar como
Usuario y comprobar una fila por puesto en Últimos resultados. El Usuario sigue sin acceder a Usuarios ni a fotos.

## Cierre

Maven: 1101 → 1105 casos distintos verificados. Suite completa ejecutada UNA vez: 1105 casos,
1104 aprobados y un fallo en HttpHistorialInactivosTest por el selector administrativo que faltaba en Pendientes.
Se repuso ese selector (sin cambiar permisos) y se repitieron solamente la clase afectada, HttpListas29Test y
LongitudFuentesTest: 22/22 aprobados, 0 fallos, errores u omitidas. JavaScript: 139 → 142; suite completa única 142/142, 0 fallos/omitidas.
Rendimiento incluido en la suite: 1000 registros/tabla, 16 tablas, 20 repeticiones; P95 lista 886,39 ms,
guardar 12,62 ms e informe 237,31 ms. No se repitió esa prueba.
 Un solo commit/push; detenerse a pedir confirmación antes de reiniciar la demo real.
