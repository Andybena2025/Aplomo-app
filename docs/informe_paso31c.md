# Paso 31C · Nuevo mensaje desde la bandeja

21 de noviembre de 2026. Base `0ade6bf`, main sincronizada.

1. La bandeja y su panel ofrecen Nuevo mensaje como acción principal. Abre el formulario existente; el panel carga con X-Alineo-Panel y conserva el contexto al enviar. El contador funciona también en formularios cargados después de abrir la página. Los eventos se atienden antes del indicador global para evitar botones bloqueados o envíos duplicados.
2. Evaluador: lista de todos los Administradores activos, principal primero y seleccionada con su etiqueta. Usuario: contacto principal configurado, sin selector; cualquier para recibido se ignora, incluso si no es numérico. El servicio comprueba otra vez el rol/estado del destinatario, utiliza remitente de sesión y guarda AVISO con reserva NULL y bitácora. Al enviar aparece Mensaje enviado a nombre.
3. Administrador: solo Evaluadores activos. Los AVISO nuevos de Evaluador admiten Responder; la respuesta vuelve como AVISO al Evaluador, con las comprobaciones de 30C. El historial general también incluye esos AVISO iniciados por el Evaluador. El Administrador no puede enviar a Usuarios ni elegir otro Administrador desde este formulario.
4. Se comprobaron elección no principal, ida/vuelta, texto escapado, destinatarios prohibidos, cuenta desactivada entre abrir/enviar, Usuario con para manipulado, CSRF, longitud y conservación de campos ante error. No se alteró la conversación de citas ni la respuesta de Usuario a PEDIR_INFORMACION.

## Requisitos y pruebas

| Requisito | Archivo y método | Comprobación |
| --- | --- | --- |
| RF-033 · nuevo mensaje y selector por rol | `Avisos.destinatarios`, `enviarNuevo`; `AvisosWeb.atender`, `nuevo`, `panel` | `HttpPaso31CTest`: seis casos HTTP nuevos |
| RF-002/033 · destinatario activo, Usuario fijo y CSRF | Servicio anterior, middleware existente | Rechazo 403 de Usuario/Evaluador/Administrador inactivo como destino de E; A no envía a U/A; User ignora para; sin CSRF no se guarda |
| RF-033 · respuesta y conversación general | `Avisos.responderEquipoEn`, `AvisosWeb.tarjeta`, `MensajeDao.general` | E elige otro A, A lee AVISO y responde; E recibe; el historial general conserva el original |
| RNF-002/003/007 · panel, contador y mensajes claros | `notas.js`, CSS `.nuevo-aviso` | Cinco casos JS nuevos y recorrido/medidas DOM |
| RNF-004/011 · escape y trazabilidad | Presentador/servicio anteriores; `BitacoraDao` existente | Texto script escapado, bitácora CREAR por envío y regla de 120 columnas |

## Comprobaciones

- Antes **1131 Maven / 161 JS**; después **1137 Maven / 166 JS**, todos aprobados, sin fallos, errores ni omitidas. Suites completas una sola vez al final; durante desarrollo solo grupos afectados. Seis casos Maven y cinco JS nuevos.
- Se adaptaron dos comprobaciones previas al cambio aprobado: el formulario de Administrador ahora ofrece Evaluadores; la prueba del destinatario principal se realiza como Usuario, conservando su objetivo y sus comprobaciones de seguridad.
- Navegador integrado: desde la bandeja del Evaluador se abrió Nuevo mensaje, se eligió un Administrador distinto de la principal y se envió sin salir del panel. Apareció Mensaje enviado a Johan Administrador QA; esa cuenta recibió el AVISO con Responder y el Evaluador encontró la respuesta. Cuentas y mensajes temporales solo en rula_ergonomia_pruebas, ROLLBACK confirmado al cerrar; no se cargó ni reinició demo.
- Ocho combinaciones: formulario de página y panel a **1366/1080/810/390 px**, alto 950. **0 px** de desborde exterior; panel **0 px** de desborde interior. Selector **44 px**, texto **16 px**, Enviar **44 px**. Contador observado **42 / 500** para el mensaje de prueba.
- **0 errores de consola capturados**. Sin capturas ni carpetas de evidencia.

## Lo que no se hizo

No se comprobaron dispositivos físicos/tacto, Safari/iPad/iPhone ni navegadores externos: revisión por HTML/DOM del navegador integrado. Sin migración, cambios al motor RULA, cálculo, IA, imágenes o datos de demo. No se crearon cuentas reales, no se cambiaron contraseñas ni se enviaron mensajes con cuentas reales. Nada se borra; la bitácora registra la acción, sin copiar el texto del mensaje.

## Checklist

- ✅ Maven 1137 y JS 166; compilación y 120 columnas.
- ✅ Nuevo mensaje, selector, contador y confirmación comprobados; sin capturas por instrucción.
- ✅ 390/810/1080/1366 sin deslizar de lado.
- ❌ Prueba con el dedo en dispositivo físico no realizada.
- ✅ Permisos por rol y destinatario revalidados en servidor; 403 y CSRF comprobados.
- ✅ Texto escapado, tildes y cero errores de consola capturados.
- ✅ Sin conexión privada, fotos ni contraseñas en Git; demo intacta.

Para Andy: refresca, abre Avisos → Nuevo mensaje; como Evaluador elige un Administrador y escribe. Como destinatario, abre Avisos → Responder. El Usuario solo ve la principal y el Administrador solo puede elegir Evaluadores activos. La confirmación debe mostrar a quién se envió.

Entrega: un commit Paso 31C y push a main del repositorio verificado Andybena2025/Aplomo-app; app normal reiniciada en 8080 sin reiniciar demo. Esperar revisión de Andy.
