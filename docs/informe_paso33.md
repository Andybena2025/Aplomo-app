# Paso 33 · retoques de Andy

## Qué se hizo

1. Saludo del equipo sin píldora: rol en texto de 13 px, mayúsculas y champán debajo del nombre; Usuario sin rol visible.
2. Botones principales a 700 y secundarios a 600. Los cuatro accesos de Inicio conservan alto, padding y letra iguales.
3. Tendencia enlaza la evaluación Finalizada con peor puntaje de cada visita; fecha dd/MM/yyyy. No se enlaza el ID de la visita como si fuera una evaluación. Se conservan filtros de empresa y ausencia de fotos para Usuario.
4. Ayuda arriba junto a próxima visita, con las mismas columnas que los indicadores. Contactos autorizados, género por cargo, ficha en línea de una en una y botón nativo con aria-expanded/aria-controls. Sin teléfono válido no hay tel/WhatsApp; 0000000000 se considera inválido. Contacto principal usa el archivo privado/respaldo. No hay X ni sessionStorage de ocultamiento.
5. Centro de ayuda con buscador/chips y contacto arriba; botones en una fila, temas en cuatro tarjetas de igual alto y Sobre ALINEO cuando ese grupo solo tiene Acerca.
6. Copyright con ALINEO™ y Todos los derechos reservados en login y pie compartido. Sin ®.
7. Volver a iniciar sesión desde Solicitar acceso, enlace de ayuda destacado y tarjetas doradas solo en los lugares pedidos. CSS de brillo entregado por Andy conservado, una ejecución; movimiento reducido/impresión con las reglas indicadas.
8. Gabby por correo/rol: Laura Romero; Ing. Gabriela Romero; Rep. Laura Gabriela Romero. Cargos por correo y alias viejos aceptados. Respaldo Laura Romero / laura.romero@alineo.co. Los créditos reales de Quiénes somos/Proyecto Nuclear no cambian. Principal puede resolver gabby@rula.local después del correo configurado, para que el nuevo nombre no desvíe los mensajes a otro Administrador.

## Requisitos y pruebas

| Requisito | Archivo / método | Comprobación |
|---|---|---|
| RNF-007: saludo y jerarquía | Html.cabecera; aplomo.css | HttpPaso33Test.rolDebajoDelNombreSoloEnInicioDelEquipo; DOM Admin |
| RNF-007/008: botones | aplomo.css | DOM: cuatro botones 44 px, 15 px, padding 10/22; pesos 700/600/600/600 |
| RF-013b/002: tendencia privada | InicioUsuario.tendencia; InicioWeb.representante | enlace real 200 para dueño y 403 para otra empresa; fecha 10/11/2026; clic en navegador |
| RF-033/002: contactos | Avisos.contactos; ContactoInicio.tarjeta | solo Evaluadores activos asignados + principal; otro Evaluador no aparece; sin teléfono falso; HTML escapado |
| RNF-007/008: interacción accesible | inicio-usuario.js | dos JS: abre/cierra, solo una abierta; Enter y cambio de ficha comprobados en navegador |
| RF-033, RNF-007: Centro de ayuda | CentroAyuda.inicio; ContactoAyuda | contacto antes de tareas, Sobre ALINEO, botones 44 px en una fila, temas iguales en escritorio |
| RF-003/002, RNF-007: público | SolicitudesAccesoWeb.formulario; AyudaPublica; login.html | regreso, marca ™, tarjetas doradas, contacto público sin datos privados de Evaluadores |
| RF-003, RNF-009: demo compatible | Demo.cargar; DemoRoles.cuenta/cargo | DemoNombres33Test: alias anteriores, mismo ID/correo, ingreso con misma clave ficticia antes/después; conserva visitas/evaluaciones al pasar de marca 32 a 33 |

## QA sin capturas

- Inicio U: 1920×1080, 1366×950, 1180×820, 1080×820, 1024×820, 1023×820, 810×1080 y 390×844. Todas con 0 px de desborde exterior.
- Desde 1024: diferencia de alturas visita/ayuda **0 px**, diferencia X ayuda/Rojo **0 px**. Por debajo: ayuda debajo de visita. Se corrigió un margen heredado de 32 px entre secciones hermanas.
- Ayuda autenticada: 1920/1366/1080/810/390, 0 px de desborde; contacto alineado con tercera tarea en escritorio; botones en la misma fila. Cuatro temas de 248 px en escritorio. Rejilla con filas uniformes también en tamaños pequeños.
- Público: login, ayuda y solicitud a 1366/810/390, 0 px de desborde. Enlace de ayuda 16 px; campos dorados 13 px. Botones públicos a 810: 44/44/44 px, misma Y y sin corte de texto después del ajuste final de anchos.
- Una ficha abierta a la vez; Enter abre; cambiar de contacto cierra la anterior. Evaluadora sin número no muestra WhatsApp. Tendencia abre resultado real autorizado y sin fotos. Cero errores de consola capturados.
- Brillo: animation-iteration-count = 1 en ayuda pública y solicitud; CSS exacto con reduced-motion y print. No se alteró la preferencia del sistema ni se certificó Safari/iPad físico.
- Instancia QA solo en rula_ergonomia_pruebas, con ROLLBACK confirmado antes de la suite completa. Sin capturas ni descargas.

## Pruebas y límites

Base: **1144 Maven / 171 JS**. Suite completa única del Paso 33: **1149/1149 Maven y 171/171 JS**, 0 fallos/errores/omitidas. Cinco casos Maven nuevos; los dos JS de ocultar ayuda se sustituyeron por los de fichas. La compatibilidad final de recarga sin borrados se comprueba además por ejecución dirigida, sin repetir suites completas. Cierre final dirigido: 6/6 aprobados.

La revisión automática rechazó una prueba que comparaba hashes. Se reemplazó por ingreso real de cuentas ficticias antes/después de recargar, sin leer ni guardar hashes. No quedaron comprobaciones bloqueadas por esa revisión.

Pendiente: tacto y Safari/iPad/iPhone físicos, Windows y otros navegadores. No se certifica WCAG completa ni se hizo una nueva impresión física. Se mantiene el alcance del encargo; no cambian esquema, co.aplomo.rula, POST /evaluaciones/calcular, IA ni tablas RULA.

## Contacto y demo real

El archivo privado del iMac tenía Gabriel en el nombre: actualizado a Laura Romero y laura.romero@alineo.co; otras propiedades conservadas. Sigue ignorado por Git.

La recarga usa la marca DEMO-33-PLAN y reconoce la del Paso 32 sin reiniciar sus registros: añade una marca de auditoría, conserva la anterior y vuelve a aplicar los nombres. No borra visitas, evaluaciones ni fotos. Recarga real confirmada en rula_ergonomia: 13 cuentas, 13 vínculos y 397 registros de visitas/evaluaciones/fotos conservados por ID; 0 duplicados de demo. Nombres verificados: Laura Romero / Ing. Gabriela Romero / Rep. Laura Gabriela Romero. No se leyó ni guardó ningún hash o contraseña real.

## Checklist

✅ Pruebas y permisos de servidor; ✅ español y escape de texto; ✅ tamaños medidos sin desborde; ✅ teclado de fichas; ✅ sin secretos preparados para Git; ✅ esquema/motor/cálculo/IA intactos. ❌ Hardware físico y navegadores externos pendientes. Capturas omitidas por instrucción expresa.

## Cómo probar

Ingresar como Administrador → comprobar rol debajo del saludo y Nueva empresa. Ingresar como Usuario → abrir una tarjeta de tendencia → volver → abrir cada contacto y comprobar que solo queda una ficha. Ayuda → contacto arriba y cuatro temas. Cerrar sesión → Centro de ayuda → Solicitar acceso → Volver a iniciar sesión. Revisar las tres cuentas de Gabby sin cambiar sus correos ni claves.

Un commit Paso 33 y push a origin/main del repositorio verificado. App normal en 8080 al entregar; esperar revisión de Andy.
