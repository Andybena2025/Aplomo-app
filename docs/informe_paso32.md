# Paso 32 · Inicio, herramientas, mensajes y solicitudes

## 32A · puntos 1, 2, 4 y 5

Se reemplazó el riel derecho por el SVG exacto entregado por Andy y se añadió el monograma en la cabecera de escritorio. El riel izquierdo se conserva. Usuario elige sus Evaluadores activos o la Administradora principal y puede conversar en ambos sentidos; el servidor vuelve a comprobar destinatario, rol y asignación. Inicio incluye agradecimiento, próxima visita, semáforo, tendencia y ayuda que puede ocultarse durante la sesión.

| Requisito | Archivo y método | Comprobación |
|---|---|---|
| RNF-007/008: identidad adaptable | Html.laterales/cabecera; aplomo.css | DOM a 2240×1260, 1920×1080, 1180×820, 820×1100 y 390×844 |
| RF-033/002: mensajes autorizados | Avisos.destinatarios/enviarNuevo/responder; AvisosWeb | HttpPaso31CTest: ida/vuelta, otra empresa 403, Usuario 403, asignación retirada, escape y CSRF |
| RF-027: indicadores | InicioWeb.representante; InicioUsuario.tendencia | Consulta de niveles existente; estilos --verde/--amarillo/--rojo; InicioUsuario32Test |
| RF-013a/013b: Inicio privado | InicioUsuario.rotulo/faltan/proxima/ayuda | Reloj fijo, tendencia mejora/empeora/igual, destinos autorizados; En curso por URL devuelve 403 |
| RNF-007: ayuda opcional | inicio-usuario.js | Dos pruebas JS: ocultar y recordar; almacenamiento no disponible |

Medidas: una imagen de herramientas en 2240/1920; ninguna visible en 1180/820/390; cero `.deco-der`. Su borde izquierdo queda fuera del contenido y su borde derecho dentro de la pantalla. Desbordamiento exterior positivo: **0 px en las cinco ventanas**. A de cabecera visible en escritorio, oculta en las demás ventanas; marca anterior conservada. Cerrar ayuda y recargar conserva el ocultamiento durante la sesión. No hubo capturas.

Pruebas: base anterior **1137 Maven / 166 JS**. Cierre A: **1141 casos Maven distintos verificados / 168 JS aprobados**. Una suite completa ejecutada: 1140 casos, dos expectativas antiguas de presentación y un bloqueo de datos de QA; se corrigieron las expectativas y se repitieron solo las clases afectadas, junto con el caso adicional de otra empresa y los cambios de Inicio. La instancia visual reversible se cerró antes del cierre dirigido. No se repite toda la suite.

Checklist: ✅ comprobaciones del servidor y escape; ✅ mensajes en español; ✅ cinco tamaños sin desborde exterior; ✅ ningún secreto/configuración/foto privada preparado para Git; ✅ ensayos en rula_ergonomia_pruebas y ROLLBACK; ❌ no se certifican tacto ni Safari/iPad físicos ni otros navegadores. Sin capturas por instrucción expresa. La demo real no se reinicia en A.

Para probar: ingresar como Usuario → Inicio → Escribir al evaluador → enviar → ingresar como ese Evaluador y Responder → regresar al Usuario. Abrir Nuevo mensaje para comprobar ambos destinos. Revisar indicadores, próxima visita y cierre de ayuda. Las visitas En curso siguen fuera de los resultados.

32B continúa autorizado después del commit/push A; no se adelanta ningún paso posterior.

## 32B · puntos 3 y 6

Solicitud de visita en tres etapas en la misma página: sede única como texto, salones táctiles, Evaluadores con color y asignado primero, calendario lunes–domingo propio, duración inicial 2 h, horas libres y resumen editable. El motivo tiene cuatro atajos. POST /solicitar-visita y SolicitudesVisita.solicitar conservan su validación original. Un Cruce conserva lo escrito y abre la etapa 2; vuelve a consultar horas. Hay campos nativos de fecha/hora en noscript para enviar sin JS. La nueva consulta de horas exige Usuario vigente con lugares propios; comprueba salón si se envía y Evaluador activo; no entrega datos de las citas ocupadas.

| Requisito | Archivo y método | Prueba |
|---|---|---|
| RF-011: solicitud y cruces | SolicitudGuiada.formulario; SolicitudesVisitaWeb.atender | HttpSolicitud32Test: solicitud ESPERANDO, cruce vuelve a etapa 2, mismo POST |
| RF-011/002: disponibilidad privada | SolicitudesVisita.horas/libre | JSON excluye 09:30/10:00 cuando existe 10–12; 08/12/16 disponibles para 2 h; otro rol, salón ajeno, Evaluador inválido y Usuario sin lugares 403 |
| RNF-007/008: calendario | solicitar-visita.js | qa_solicitud_calendario: lunes–domingo, día pasado, hoy, año nuevo, bisiesto y fin con media hora |
| RF-020/022/025: demo variada | ResultadosDemo.caso/finalizar; DemoCompleta.entrada; marca DEMO-32-PLAN | DemoVariada32Test: ocho nombres, cero duplicados y seis empresas mejoran; DemoPlan28Test conserva conteos de visitas, agenda y pendientes |
| RF-024: subtítulos | InicioWeb.representante; PaginasArchivo | Puesto N · descripción registrada, sin empresa redundante |
| RNF-009: cuentas conservadas | Demo.reiniciar y VinculosDemo existentes | DemoReinicio29Test; comparación real por identidad y vínculo/NIT, sin extraer hashes ni contraseñas |

Los casos ficticios se distribuyen por fecha, índice del puesto y postura dentro de CASOS; nunca se asigna directamente un puntaje. Los dos casos verdes tienen ángulos diferentes y pueden dar el mismo puntaje; el motor decide. Se añadió únicamente una entrada ficticia al generador de demo, sin tocar co.aplomo.rula ni tablas RULA. Los ocho nombres rotan sin repetirse dentro del mismo puesto/visita. La distribución antigua fija 9/7/7 se reemplaza por comprobaciones de variedad y mejora.

### Comprobación del navegador

Recorrido real con cuenta ficticia en rula_ergonomia_pruebas: lugar → Evaluador asignado → 27/11 → 11:00–13:00 → chip Seguimiento → resumen → Solicitud enviada. A 1366×950, 1180×820, 820×1100 y 390×844: **0 px de desborde horizontal** en calendario y 0 px en resumen móvil; días de 48 px. Días anteriores desactivados y 2 h seleccionadas al llegar. Cero errores de consola capturados. La transacción de QA se revirtió antes de la suite completa. No hubo capturas ni descargas.

### Recarga real autorizada en el encargo

Ejecutada la clase equivalente de reiniciar_demo en el iMac con la confirmación REINICIAR DEMO y administrador de MySQL, dentro de una transacción con reversión ante diferencias. Base rula_ergonomia: **13 cuentas y 13 vínculos conservados**, **1 empresa ajena intacta**, **0 duplicados** por visita/puesto/postura/puntaje, **8 nombres de postura**, **6 empresas que mejoran**. La rutina existente ya desvincula/restaura por NIT; no hizo falta cambiarla. Las claves no se imprimieron ni se extrajeron para auditar. Los 184 archivos anteriores permanecen; no se hizo limpieza física de fotos. La marca de carga ahora es DEMO-32-PLAN. No se cambió config/db.properties.

### Cierre

Base 32A: 1141 Maven / 168 JS. 32B: **1144 Maven y 171 JS verificados**. Suites completas una vez al final de cada commit. Ajuste geométrico final: el contenido conserva los 1180 px del Paso 29; el borde de herramientas usa su semiancho real (590 px) + 24 px. El límite externo de ancho sigue la fórmula de 1240 indicada por Andy. Maven B encontró una expectativa antigua de los dos plegables de alternativas; se adaptó a volver a etapa 2 con calendario y horas, y se comprobó con cierre dirigido. JS B encontró una expectativa antigua que limitaba cualquier clase chip a 32 px: se acotó la regla para permitir las acciones táctiles de Solicitud a 44 px, conservando filtros de tabla a 32 px; cierre dirigido 15/15, sin repetir toda la suite.

✅ Servidor, CSRF y HTML escapado; ✅ datos QA revertidos; ✅ español; ✅ sin cambios de esquema, motor, endpoint de cálculo, IA o tablas RULA; ✅ sin archivos sensibles preparados; ✅ cuatro tamaños del formulario sin desborde. ❌ Tacto físico, Safari/iPad/iPhone físicos, Windows y otros navegadores no se certifican. El envío nativo se prueba por HTTP y el respaldo noscript está presente; no se hizo un recorrido con JavaScript desactivado en navegador.

Para Andy: refrescar → ingresar como Usuario → ver Inicio y contacto asignado → Solicitar una visita → elegir salón, Evaluador, día y hora → motivo → Enviar solicitud. Como Evaluador, revisar la solicitud en la bandeja. En Archivo revisar nombres distintos y la mejora de los resultados de demo.

Git: 32A bf2553a subido antes de B; B se entrega como un único commit Paso 32B y push a origin/main verificado. App normal reiniciada en 8080 al terminar. Se espera revisión de Andy.
