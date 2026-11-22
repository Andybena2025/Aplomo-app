# Paso 30 · Encabezado único y Centro de ayuda

21/11/2026. Base 67b025a. Dos commits autorizados, con push de 30A antes de comenzar 30B.
Sin capturas, carpetas de evidencia, descargas, migraciones ni cambios del motor, cálculo, IA, imágenes o demo.
La demo real fue reiniciada en el Paso 29 con la frase exacta de Andy; no se reinicia en este paso.

## 30A · Partes 1–4

| Punto / RF-RNF | Archivo y método | Comprobación |
|---|---|---|
| 1 · RF-002, RNF-002/007 | Html.pagina/cabecera + CabeceraPagina.extraer trasladan el título y acciones a una franja estática. InicioWeb.saludo conserva fecha y primer nombre; etiqueta de rol o empresa única U, obtenida del servicio autorizado en Router y transportada como contexto de presentación. Sesiones mantiene tokens/caducidad originales. Enfoque no cambia. Aviso de 40 px debajo. | HttpPaso30ATest en A/E/U; SesionesTest. DOM: un header antes de main, título y menú dentro; una fila en los cinco tamaños; título/acciones conservados. |
| 2 · RNF-002 | aplomo.css: ambas marcas laterales height/longitud min(520px,52vh), centro compartido; marca completa en franja debajo de 1440. | A 1920×1080: izquierda 520 px y derecha 520 px, diferencia 0 %. Logo de franja 84 px en iPad/portátil, 72 en celular; rieles ocultos allí. |
| 3 · RF-024, RNF-002/007 | CSS desactiva archivo-identidad::after solo en tablas; sistema-visual conserva el clic de fila. CuentaWeb usa Html.iniciales y círculo 72 px. TextoLista.persona abrevia a nombre/apellido y conserva completo en title. PaginasConsultas: Todas en Empresa/Sede. TablasTrabajo evita En curso vacío/contador doble. Pendientes: 34 % y separación de 16 px. | HttpPaso30ATest y HTTP de consultas/cuenta. Clic real en flecha de primera fila abre su propio id; Inactivas deja una fila en /archivo sin abrir detalle. El Usuario conserva Sugerencias y consulta de estados porque sí tiene permiso de consulta en el servidor. |
| 4 · RF-013a/013b/024 | PaginasVisitas.detalle agrupa por puesto y máximo guardado, muestra número/posturas en gris y enlace separado hacia las posturas de esa visita/puesto; sin botón ni fotos para U. Horario real compacto y sin calificación técnica visible; promedio del servicio intacto. Inicio E vacío compacto y títulos alineados. Mis reservas: una cabecera por sección y próxima cita no repetida; cuenta en chips. Inicio U sin tarjeta redundante y gris Puesto N · posturas. | HttpPaso30ATest, HttpVisitasTest, HttpInicio29Test, HttpListas29Test. qa_filtros_visual: cita destacada contada pero no repetida, búsqueda por nombre corto o completo. |

### Medidas de 30A

- Navegador integrado con datos ficticios en rula_ergonomia_pruebas, transacción revertida al cerrar.
- 40 combinaciones iniciales Admin: Inicio, Empresas, Reservas, Visitas, Pendientes, Archivo, Mi cuenta y detalle.
- 50 combinaciones E/U: Inicio, Reservas, Pendientes, Mi cuenta y detalle.
- Ventanas: 1920/1180/1024/820/390 × 1080. Encabezados: uno; título dentro: uno; menú: una fila.
  Desbordamiento exterior y de tablas: 0 px. Corregidos y vueltos a medir los cortes de Estado/Prioridad/Resultado,
  incluidos dos últimos ajustes de E en 820 px. Títulos y líneas largas conservan elipsis con texto completo.
- Archivo: destino esperado y abierto /archivo?empresa=<id de la primera fila>, no el de otra fila.
  Filtro Inactivas: una fila, mismo /archivo, pseudo-elemento content none.
- Usuario de una empresa: nombre de empresa junto al saludo, sin tarjeta Tu empresa. Avatar AB.
- Cuatro acciones Admin con peso 600; botones 44/36, chips 32; escala del Paso 29 conservada.
- Cero errores de consola recogidos. Sin comprobación física de iPad, tacto, Safari/Chrome/Edge/Firefox o Windows.

### Pruebas y cierre de 30A

Maven 1105 → 1109: suite completa única 1109/1109, 0 fallos, errores u omitidas (3 min 03 s).
JS 142 → 144 casos distintos verificados: suite completa única 143/143 y un caso posterior de nombre corto,
comprobado junto a su archivo completo 12/12. No se repitieron suites completas. Promedio de visita, bloqueo,
sesiones, alcance y resultados finales siguen pasando. P95 listas 821,38 ms, guardar 12,31 ms e informe 243,20 ms.
Commit «Paso 30A» y push autorizados antes de iniciar B.

### Lo que NO se hizo en 30A

- Nada de las partes 5–6 antes del push de A. El Centro de ayuda continúa autorizado para B.
- No se tocaron tablas/esquema, datos de demo, motor, endpoint protegido, IA, imágenes ni contraseñas.
- No se borra ningún registro. La agrupación de visita presenta sus posturas sin modificar datos guardados.
- No se certifica hardware físico ni navegadores externos; no se tomaron capturas por instrucción expresa.

### Checklist 30A y revisión manual

- ✅ Un encabezado y títulos coherentes; menú sin recortes y marcas con igual altura medida.
- ✅ Archivo abre su fila y no se abre automáticamente al filtrar a un resultado.
- ✅ Mi cuenta con dos iniciales; visita por puesto; U sin fotos ni botón de análisis.
- ✅ Escapado de HTML, permisos y promedio del servidor conservados y probados.
- ✅ QA reversible; no datos sensibles en el commit.
- ✅ Suites finales verificadas; commit/push A antes de continuar B.

Para revisar: refrescar tras la entrega final; abrir Archivo y pulsar la primera empresa, volver y filtrar Inactivas;
abrir Mi cuenta; abrir una visita finalizada con dos posturas del mismo puesto; entrar como Usuario y confirmar que
solo ve sus resultados finalizados, sin fotos. En Mis reservas revisar próxima cita y lista sin repetición.
