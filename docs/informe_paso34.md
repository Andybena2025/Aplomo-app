# Paso 34 · login y contactos

## Cambios

1. Reemplazado únicamente el SVG de pzOverlay por el bloque literal de Andy. Foto, ajuste fit(), textos y resto del login conservados. Añadidos los cuatro bloques CSS pedidos junto a pz-svg; estilo gd intacto. Las guías nuevas se ocultan hasta 820 px.
2. Ambas fichas reutilizan ContactoAyuda.botones: correo como texto, teléfono válido como texto y una fila WhatsApp / Correo / Enviar mensaje en ALINEO. Asunto Ayuda con ALINEO; destino para=id de cada contacto ya autorizado. Sin número válido no hay WhatsApp. Eliminado Escribir suelto. No se cambió Avisos.contactos ni ningún permiso.

| Requisito | Archivo / método | Prueba |
|---|---|---|
| RNF-007/008: guía del login | plantillas/login.html; aplomo.css | ContactoLogin34Test.svgTienePuntosGuiasYAngulosExactos; comparación literal del SVG y resto de plantilla |
| RF-033/002: contacto autorizado | ContactoInicio.tarjeta; ContactoAyuda.botones | evaluador sin número, evaluador con número/nombre escapado y administradora con su ID y datos de Contacto |
| RNF-007/008: botones coherentes | aplomo.css | vista local con DTO ficticios, sin base de datos, a siete tamaños |

## Comprobación

- SVG: **5 lb, 5 ld, 5 ldp y 8 jt**. Valores **21°, 22°, 65°, 19° y 4°**; sin 108° ni 162°. Comparación exacta con el encargo; el resto de login.html coincide byte por byte con main anterior.
- Vista de contactos sin base de datos: 1920×1080, 1366×950, 1180×820, 1080×820, 1024×820, 810×1080 y 390×844. Botones **44 px**, misma Y en cada fila, **0 px de corte vertical/horizontal** y **0 px de desborde exterior**. Ancho mínimo medido 45,56 px.
- Las fichas compactan sus rótulos a 13 px y eliminan padding lateral del marco interior para mantener toda la fila visible en la columna de ayuda. El componente de Centro de ayuda conserva su aspecto y alto existentes.
- Evaluador sin teléfono: solo Correo y Enviar mensaje en ALINEO. Administradora: mismos tres botones con su ID. Apertura/cierre existentes conservados; cero errores de consola capturados. No se pulsaron enlaces de correo/WhatsApp ni se enviaron mensajes.
- Sin capturas ni descargas. Vista temporal cerrada; ningún dato de la base modificado y ninguna recarga de demo.

## Pruebas

Base **1149 Maven / 171 JS**. Cuatro casos Maven nuevos. Pruebas dirigidas de SVG/contacto/rutas/longitud: 9/9. Suite completa única al final: **1153 Maven / 171 JS**, casos distintos verificados. La suite se ejecutó una sola vez; dos expectativas antiguas aún pedían x=1020. Se actualizaron a la plomada x=988 del SVG de Andy y se repitieron solo las clases afectadas, sin repetir toda la suite. Cierre dirigido: 17/17 aprobadas.

## Checklist y límites

✅ HTML escapado y permisos conservados; ✅ español; ✅ siete tamaños sin desborde; ✅ sin secretos, datos de demo ni esquema tocados; ✅ motor RULA, endpoint de cálculo, IA y tablas RULA intactos. ❌ No se certifican pantallas físicas, tacto, Safari ni otros navegadores. El login se valida por contenido exacto y no se afirma una nueva revisión visual de alineación sobre un dispositivo físico.

## Cómo probar

Refrescar login → revisar los cinco rótulos y puntos nuevos. Ingresar como Usuario → abrir el nombre del Evaluador → comprobar Correo y Enviar mensaje en ALINEO; WhatsApp solo con número válido. Abrir Administradora y comprobar el mismo tipo de fila. Los destinos siguen autorizados por el servidor.

Un commit Paso 34 y push a origin/main verificado; app normal reiniciada en 8080 al entregar. Esperar revisión de Andy.
