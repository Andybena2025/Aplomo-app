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
