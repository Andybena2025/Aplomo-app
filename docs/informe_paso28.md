# Paso 28 · Pulido de ALINEO para la presentación

Fecha: 21/11/2026. Alcance autorizado: tres bloques A/B/C con commit y push separados.
No se toman capturas ni se descargan navegadores. Todas las pruebas usan `rula_ergonomia_pruebas`.

## 28A · Errores y listas

| Punto | Resultado |
|---|---|
| 1 | Diálogo cerrado oculto; una cabecera en Archivo; sin Atrás en pestañas; contadores sin duplicación; estados con una marca; iniciales compartidas; texto de práctica y riesgo legibles; explicación de calificaciones sin notas internas. |
| 2 | Mostrar/Ocultar dentro de todos los campos de contraseña existentes, con botón de teclado y estado accesible. Solicitar acceso no pide contraseña; no se añadió una. |
| 3 | Tablas con encabezados, búsqueda, estados, personas, título descriptivo, riesgo suave con cifra y palabra, prioridad escrita y vencimiento. Se conserva el historial de registros inactivos. |
| 4 | Reservas por día y panel mensual; próxima cita y respuesta para Evaluador; visitas en curso con progreso; Archivo con resumen y jerarquía; Empresas A-Z/recientes; Usuarios por rol/empresa. Evaluador: listas de su trabajo; Usuario: alcance empresarial y resultados finalizados. |
| 5 | Contacto desde configuración privada: nombre/cargo y botones solo cuando existen datos; mensaje interno dirigido a la cuenta principal activa. |
| 6 | Guardar una nota regresa al detalle de la reserva con confirmación; no abre una página separada. |

**Decisión expresa de Andy:** Pendientes muestra Abiertos / Arreglados / Todos, con contador. No existe «En proceso»
en ese esquema y no se añadió. «En proceso» se conserva en Sugerencias.

### Verificación A

- Base anterior: 1090 casos Maven y 136 JS verificados en el Paso 27.
- Suite completa A ejecutada una vez: 1096 casos Maven. Detectó 27 fallos de regresión de presentación; se corrigieron
  los avisos de inactivos y metadatos históricos y se adaptaron las aserciones de tarjetas a filas de tabla.
  Se repiten únicamente las clases afectadas, más la regla de longitud. Resultado final: 1096 casos distintos aprobados; 85 casos dirigidos repasados, y la única longitud restante corregida y aprobada aparte.
- JS: 137 casos de suite completa aprobados; un caso dirigido adicional de contador, 138 casos distintos aprobados.
- Navegador integrado: 20 medidas de listas de Administrador a 390/820/1180/1600 px, más seis medidas de corrección
  y ocho de Evaluador/Usuario. Resultado final: 0 px de desbordamiento exterior en las combinaciones revisadas.
  Las tablas anchas tienen desplazamiento interno. Menú en una fila a 1180/1600 px; en 390 px se conserva el menú
  adaptable existente. Diálogo cerrado: ancho 0, display none; figura derecha visible a 1600 px.
- Login: control real button, type button, texto «👁 Mostrar», aria-pressed false; no se guarda la contraseña.
- Rendimiento: 1000 registros en 16 tablas, 20 repeticiones por acción. P95: lista 793,70 ms; guardar 15,54 ms;
  informe 197,79 ms, todos por debajo de 5000 ms.
- QA temporal revertida (ROLLBACK). La app normal y sus datos se conservaron.
- Sin nueva comprobación física en iPad/iPhone, Windows, Chrome/Edge/Firefox ni impresión real.

## 28B · Demo y pendiente por puesto

- RF-022: la primera postura roja crea el pendiente del puesto/visita; las siguientes actualizan solo su máximo.
  Se conservan descripción, prioridad, fecha y estado. Otra visita crea un registro distinto. El detalle reúne
  todas las posturas; las duplicaciones antiguas se agrupan en la consulta sin borrar registros.
- Cargador nuevo: 7 empresas, 10 sedes, 10 salones y 40 puestos, cada salón con 4 puestos de Oficina/Planta.
  Nombres de cargo y descripción del sector, sin duplicados dentro de cada sede. Logística queda inactiva.
- Distribución comprobada: 16 Verde (40 %), 14 Amarillo (35 %), 10 Rojo (25 %); 2 puestos con 7 (5 %).
  Todos los resultados salen del motor existente y se guardan mediante Evaluaciones/Análisis/Visitas.
- 5 pendientes abiertos; históricos arreglados por separado. 24 citas de noviembre: 8 por Evaluador,
  2 canceladas, 3 por confirmar en el periodo futuro y citas del 27 a 8:00 / 9:30 / 11:00.
- Pruebas numéricas: minutos 0/30, sin solapes, sin repetir día y hora en semanas consecutivas,
  nombres únicos, mezcla de riesgos y máximo de pendientes. Repetición conserva cuentas, claves e intentos.
- Reiniciar demo conserva la confirmación escrita y MySQL administrador; ahora abarca las siete identidades
  marcadas, limpia sus vínculos y mensajes asociados a reservas y rechaza solicitudes/cuentas incompatibles.
  No se ejecutó ese reinicio contra la base real. Andy debe ejecutarlo para sustituir las cargas anteriores.
- `datos_demo.sql` conserva utf8mb4 y remite al cargador; se retiraron las inserciones históricas que generaban
  duplicados y puntajes escritos a mano. No se cambió esquema ni se añadió migración.
- Calendario: sin checks en días pasados; nota leída como icono dentro de la cita (integrado en A).

- Cierre B: 1098 casos Maven distintos verificados; suite completa una vez, una aserción histórica de tres sedes
  ajustada a las dos sedes aprobadas y comprobada de nuevo. 138/138 JS aprobados. Motor recalculado contra
  todos los análisis de Humboldt; mediciones manuales y puntajes A/B/final coinciden.

## 28C

En curso; se cerrará con el siguiente commit de este mismo encargo.
