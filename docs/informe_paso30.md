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

## 30B · Partes 5–6

30A subido a origin/main como **008cf17** antes de empezar este bloque.

| Punto / RF-RNF | Archivo y método | Comprobación |
|---|---|---|
| 5 · RF-002/033, RNF-007/011 | Contacto.leer usa los cinco datos ficticios de Andy solo si el archivo no existe. enlaceWhatsapp/enlaceCorreo codifican el mensaje/asunto; configuración privada existente conserva prioridad, validación y escapado. ContactoAyuda comparte ficha y botones con ayuda/login. Avisos.principalEn conserva destinataria del servidor y, únicamente para el contacto ficticio, reconoce a la Administradora Laura existente por nombre normalizado Gabriella/Gabriela. No crea cuentas ni cambia datos. AvisosWeb añade Cerrar al panel nuevo. | HttpPaso30BTest: datos exactos, cuatro preguntas, configuración privada prioritaria, destinataria existente y cierre del panel; HttpAyudaTest: datos parciales, enlaces inválidos y escapado; HttpAvisos27Test: permisos y CSRF. Apertura/cierre reales del panel en QA, sin enviar mensajes. |
| 6a · RF-001/002, RNF-002/007 | AyudaPublica.pagina: marca de 36 px, volver a login, cuatro preguntas de acceso, contacto, Quiénes somos y pie. Cuentas temporales reciben esta ayuda de acceso. Sin crear sesión anónima. | HttpPaso30BTest, HttpAyudaTest y cinco medidas DOM: logo 36 px, summaries 56 px, 0 px de desborde. Pregunta abierta/cerrada y vuelta al login comprobadas. |
| 6b · RF-002, RNF-002/007 | CentroAyuda.inicio + GuiaAyuda.tareas/populares/seccion: saludo por primer nombre, búsqueda 52/18, cuatro temas populares, seis tarjetas por rol y cuatro grupos de temas. Entrar y contraseña retirado de la ayuda con sesión; sigue el contenido de acceso en ayuda pública y Seguridad/Mi cuenta. | HttpPaso30BTest recorre tareas y todos los artículos A/E/U; pruebas de búsqueda sin tildes de qa_ayuda. DOM: seis tarjetas, búsqueda 52 px, búsqueda real «asignar» y vacío con búsqueda sin coincidencias. |
| 6c · RF-002/027, RNF-007 | CentroAyuda.pagina/indice/riesgos: query tema validada en servidor contra lista del rol; navegación por sección, tres pasos con Verás, un consejo específico por tema, tres relacionados y contacto. Color/nivel leídos de MySQL una vez por fila, sin repetir el nombre del color. GuiaAyuda mantiene consejo y orientación específicos. | HttpPaso30BTest solicita cada artículo permitido, exige tres Verás/un consejo/tres relacionados y prueba 403 de temas de otro rol. HttpAyudaTest cambia nombre/acción en MySQL de pruebas y comprueba actualización/escapado. qa_centro_ayuda prueba enlaces anteriores y utilidad local. |

### Medidas y límites de 30B

- Ayuda pública: 1920/1180/1024/820/390 × 1080, 0 px de desborde; logo 36 y cuatro summaries 56 px.
- Centro y dos artículos (Resultados/Mi cuenta) en A/E/U: 45 combinaciones a esos cinco anchos; 0 px de desborde.
  Seis tarjetas en Inicio y un consejo/tres relacionados en cada artículo comprobado. Ningún texto visible <13 px
  en los artículos medidos. Buscador 52 px. Semáforo: Verde · Nivel 1 / Amarillo · Nivel 2 / Rojo · Nivel 3 / Rojo · Nivel 4.
- Confirmación adicional del riel común tras centrarlo: izquierda y derecha con centro Y=540 a 1920×1080.
- Login después de añadir contacto: 0 px de desborde a 390×844, 820×1100 y 1920×1080; Entrar termina en
  Y=779,66 / 1002,46 / 692,46 respectivamente, dentro de la ventana.
- Contacto interno abrió Mensaje para Laura Gabriella Romero y cerró con Cerrar; no se envió ningún mensaje.
  WhatsApp y correo comprobados por sus enlaces, sin contactar a nadie ni abrir servicios externos.
- Cero errores de consola durante las comprobaciones de artículos. Ensayos en rula_ergonomia_pruebas,
  revertidos al terminar. No se ejecutó ningún reinicio/carga sobre rula_ergonomia.
- Sin capturas ni descargas. Safari, Chrome/Edge/Firefox externos, iPad físico, dedo y Windows no comprobados.

### Pruebas y cierre de 30B

Maven 1109 → 1114: suite completa única 1114/1114, 0 fallos, errores u omitidas (3 min 00 s).
JavaScript 144 → 147: suite completa única 147/147, 0 fallos u omitidas. No se repitieron suites completas.
Commit «Paso 30B» y push autorizados; después se reinicia únicamente la app normal en 8080.
Comprobación real de solo lectura antes de actualizar la app: 13 cuentas, 25 visitas, 23 Finalizadas,
13 Finalizadas de noviembre y 6 pendientes abiertos; mismos conteos posteriores al reinicio del Paso 29.

### Lo que NO se hizo y checklist final

- ✅ Centro público completo, seis tareas por rol, artículos con permisos y contactos configurables.
- ✅ Datos ficticios exactos de Andy y configuración privada con prioridad; sin contraseñas en Git.
- ✅ Semáforo desde MySQL sin color duplicado; tres relacionados y un consejo específico por artículo.
- ✅ Mensaje nuevo abierto/cerrado sin enviar; destinataria resuelta en servidor y CSRF conservado.
- ✅ Pruebas en base de pruebas y QA revertida; sin motor/cálculo/IA/imágenes/demo/esquema modificados.
- ✅ Suites finales completas aprobadas; commit/push B y reinicio de app autorizados, sin reiniciar demo.
- ❌ Comprobación física de dedo, iPad, Windows y otros navegadores: no se realizó; solo navegador integrado.

Cómo revisar: sin sesión, abrir /ayuda, desplegar las cuatro preguntas y volver al login. Con cada rol, abrir Ayuda,
buscar un tema y comprobar seis tareas. Abrir Semáforo/Resultados y Mi cuenta; recorrer un artículo y sus relacionados.
Abrir Enviar mensaje en ALINEO y cerrar; no hace falta enviar. Los enlaces de WhatsApp/Correo usan los datos de
contacto privados cuando existen; los mostrados en ausencia del archivo son ficticios y no son un servicio real.
