# Paso 30C · Mensajes rápidos desde Agenda

21 de noviembre de 2026. Implementado sobre `b344140` (Paso 31), que ya incluía 30A/B.

## Qué cambió

1. En el mes y la semana, una cita abre un panel de 420 px a la derecha. Por debajo de 900 px ocupa el ancho útil completo, sin recorte por la barra de desplazamiento. Conserva la dirección `/reservas/N` para otra pestaña, modificadores y navegación sin JavaScript. Muestra fecha/hora, empresa, sede, salón, áreas registradas de sus puestos, estado, evaluador y reserva completa. Las áreas se consultan después de autorizar la cita; no se agrega un campo ni se inventa un área si falta. El historial ordena por fecha e ID, con mensajes propios a la derecha, nombres cortos, fecha y texto escapado. Hay contador 0–500 y tres plantillas editables para Administrador.
2. El Administrador escribe desde la leyenda a un Evaluador activo: AVISO sin reserva. El Evaluador contesta desde su bandeja y el Administrador puede contestarle de vuelta. El panel general conserva el historial de esas dos cuentas. En la cita, el Administrador sigue usando `Agenda.guardarNota` y NOTA_ADMIN; el Evaluador responde al último Administrador que le escribió allí o al principal si todavía no existe una nota. Los mensajes nuevos se guardan en la tabla existente y cada envío tiene bitácora.
3. Todos los servicios renuevan el alcance antes de operar. El Evaluador de otra cita recibe 403 aunque esté asignado a la misma empresa; el Usuario no accede a los endpoints nuevos. Los destinatarios de cita se calculan en servidor. El Administrador solo envía a Evaluadores activos; no puede reutilizar una respuesta antigua para escribirle a quien ya no está asignado a la cita. Cada Administrador ve su conversación con el Evaluador; no recibe los mensajes privados de otro Administrador. El Evaluador ve los mensajes que le corresponden. No se alteran las respuestas de Usuario a PEDIR_INFORMACION.
4. Abrir consulta primero sin marcar leído y luego acusa por POST con CSRF únicamente lo dirigido al actor: NOTA_ADMIN para el Evaluador, RESPUESTA para el Administrador. Se conserva nota_vista y el aviso de calendario. Envío por fetch sin salir de Agenda, con botón desactivado y borrador conservado ante fallo. El sondeo del panel cada 15 segundos conserva el borrador y usa `sesiones.consultar`, también para su acuse; no prolonga los 30 minutos de inactividad. Cerrar descarta respuestas tardías. Un fallo de acuse no se presenta como fallo de un envío ya guardado.

## Requisitos y comprobaciones

| Requisito | Archivo y método | Prueba |
| --- | --- | --- |
| RF-033 · cita, avisos y respuesta | `Agenda.guardarNota`, `conversacion`, `responder`, `MensajeDao.conversacion` | `HttpPaso30CTest.citaIdaYVueltaConAvisoCalendarioEscapeYLecturaSinRecargar`, `responderCitaUsaUltimoAdministradorYSinNotaUsaPrincipal` |
| RF-033 · aviso general y conversación de vuelta | `Avisos.escribirEvaluador`, `conversacionEvaluador`, `responder`; `MensajeDao.general` | `avisoGeneralYRespuestasBidireccionalesNoMezclanCitasNiOtrosAdministradores` |
| RF-002/033 · permisos, destinatario, CSRF, 500 y actividad | Servicios anteriores, `AgendaWeb.atender`, `Router` | `evaluadorAjenoAunqueComparteEmpresaYUsuarioNoLeenNiEnvian`, `validaCsrfLongitudTipoDeCuentaYRevocacionEnCadaEnvio`, `bandejaRespondeNotasConFlujoActualYRevalidaAsignacion`, `sondeoDeConversacionYAcuseNoProlonganLaSesion` |
| RF-033, RNF-002/003/007 · panel y navegación alterna | `PanelMensajesAgenda.cita/general`, `PaginasAgenda.pagina`, `Calendario.filtros`; CSS y `agenda-mensajes.js` | `agendaConservaEnlacesMesYSemanaYOfreceEscribirSoloAdministrador`; diez casos JS y medidas DOM |
| RF-033 · formulario de respuesta sin modal superpuesto | `AvisosWeb.tarjeta`, `notas.js` | Caso nuevo en `qa_notas.cjs` y recorrido de navegador |

## Pruebas y medidas

- Antes: **1121 Maven y 150 JS**. Después: **1129 casos Maven verificados y 161/161 JS**. La suite Maven única aprobó 1128 casos y detectó una preparación antigua con Evaluador inactivo; se corrigió únicamente ese dato de prueba y el cierre dirigido aprobó 22/22 (Paso 26, Paso 30C y longitud). Sin fallos pendientes ni omitidas. Las suites completas se ejecutaron una sola vez al final; durante desarrollo y la corrección solo se ejecutaron grupos afectados. Ocho casos Maven nuevos y once JS nuevos.
- Prueba HTTP contra MySQL real de pruebas: ida/vuelta, aviso en bandeja y calendario, acuses, orden, destinatario ignorado del navegador, escape de script, otro Evaluador de la misma empresa, Usuario, otro Administrador, cuenta inactiva, reatribución de cita, vacío/501 caracteres, CSRF y vencimiento de sesión. Los servicios anteriores de solicitudes/avisos también se comprobaron de forma dirigida.
- Navegador integrado: Administrador envió nota con plantilla editada y aviso general; Evaluador abrió la cita semanal, respondió sin navegar y contestó el aviso desde la bandeja. Administrador encontró ambas respuestas en sus paneles respectivos. Todo con cuentas temporales en una transacción de `rula_ergonomia_pruebas`, sin cargar ni reiniciar la demo, y ROLLBACK confirmado al cerrar.
- 14 medidas: panel de cita y panel general a **1920, 1366, 1080, 900, 899, 810 y 390 px** (alto 950). Resultado: **0 px** de desbordamiento de página y panel en todos. Desde 900: **420 px**; a 899/810: **899/810 px**; a 390: **375 px útiles**, dejando intacta la barra del navegador y sin salir de los límites. Botones del panel **44 px**; Escribir **36 px**.
- En celular se comprobó tocar el día y abrir la cita de la lista: se restauró hora/empresa en esa lista, que heredaba la ocultación de texto del mes. Se corrigieron dos interferencias: Responder ya no abre el modal de lectura encima y los eventos del panel se atienden antes del indicador global para que Escribir no quede en Cargando ni la cita bloqueada. Reapertura consecutiva de Escribir y de cita comprobada sin recargar: texto Escribir conservado, sin data-navegando y ruta `/agenda` intacta.
- **0 errores de consola capturados**. Sin capturas ni carpetas de evidencia.

## Lo que no se hizo / límites

Sin migraciones, dependencias ni cambios al esquema, motor RULA, cálculo, IA, imágenes, datos de demo, cuentas reales o contraseñas. No se enviaron mensajes con cuentas reales. No hay mensajes borrados. No se descargaron navegadores. No se certifican Safari/iPhone/iPad físico, tacto ni Windows; la comprobación fue por estructura y medidas del navegador integrado, sin capturas.

## Checklist

- ✅ Compilación y suites Maven/JS; regla de 120 columnas, con exclusiones protegidas existentes.
- ✅ Panel 420 px / ancho completo, escala 18/15/13 y controles 44/36; tamaños y flujo comprobados sin capturas.
- ✅ 390/810/1080/1366/1920 sin desbordamiento; también 899/900.
- ❌ Prueba con el dedo en un dispositivo físico no realizada.
- ✅ Usuario y Evaluador no asignado: 403 por lectura/envío y direcciones directas.
- ✅ Textos españoles, contenido dinámico escapado y sin errores de consola capturados.
- ✅ Configuración privada/fotos excluidas; no se reinició la demo.

## Cómo lo prueba Andy

1. Refresca la app y abre Agenda como Administrador. Toca una cita, elige Llevar EPP, completa el mensaje y pulsa Enviar. La burbuja aparece sin salir de la agenda.
2. Entra como su Evaluador: abre esa cita y responde. El Administrador puede leer la respuesta en la cita y en Avisos.
3. En la leyenda de Agenda, pulsa Escribir junto a un Evaluador. Envía un aviso; desde su bandeja, el Evaluador despliega Responder. La respuesta llega al Administrador, que también puede responder.
4. Para abrir la reserva completa usa el enlace del panel o abre el enlace original de la cita en otra pestaña. En celular, primero toca el día y luego la cita.

## Entrega

Un commit `Paso 30C`, push a `origin/main` de `https://github.com/Andybena2025/Aplomo-app.git`, app normal recompilada/reiniciada en 8080 sin reiniciar demo. El hash se comunica en la entrega. Esperar la revisión de Andy.
