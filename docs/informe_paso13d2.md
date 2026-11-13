# Paso 13D-2 · Identidad de marca: compás y plomada

13 nov 2026 · Implementación autorizada por Andy; pendiente de su revisión del resultado.

## Resultado y alcance

Se siguió el mockup v4 de `docs/mockup_13d2/`, dando prioridad a las medidas y al CSS del encargo. Se ignoró su página de Bienvenida: el ingreso sigue llevando directamente a Inicio. El login conserva su foto y dibujo, con la etiqueta «Bienvenido». Inicio omite Atrás para ambos roles; las demás páginas lo conservan.

Las páginas con sesión comparten pie de marca, compás y plomada. Los laterales aparecen desde 1400 px, sustituyen al logo de cabecera y se ocultan al imprimir. El compás tiene las 37 marcas solicitadas; la plomada, las cinco marcas y las coordenadas del logo existente. El CSS conserva los tiempos, tamaños y puntos de corte indicados; `prefers-reduced-motion` desactiva sus animaciones. Se dejó sin uso la plantilla antigua del fondo, sin borrar documentos ni recursos.

Nueva empresa, Nueva reserva y Nuevo usuario tienen los paneles y consejos exactos del encargo; editar conserva su presentación anterior. Los estados vacíos usan el logo y texto en párrafo, conservando botones y la condición `hidden` del filtro de Usuarios. El pie permanece abajo en páginas cortas: en el estado vacío de Usuarios el documento, viewport y borde inferior del pie midieron 1000 px.

El semáforo del Administrador reutiliza el criterio de Puestos en rojo y cuenta cada color desde `nivel_riesgo` y `puesto_trabajo.ultimo_puntaje`. La ampliación de esta consulta de lectura es la excepción expresamente solicitada en el punto 5: no hay cambios de esquema, archivos SQL, rutas, permisos ni cálculo RULA. El resumen del Usuario mantiene su consulta limitada por empresa y visitas Finalizadas. La barra se omite si los tres conteos son cero.

## Requisitos y comprobaciones

| Encargo / requisito relacionado | Archivo y método / selector | Comprobación |
|---|---|---|
| 1 y 7 · presentación de producto / RNF-002 y RNF-007 | `plantillas/login.html`; `Router.atender`; sobrecarga `Html.pagina` | `HttpMarcaTest.paginasDeProductoConservanPieYNoMuestranEtiquetasDelProyecto`: login y 18 rutas con sesión sin etiquetas de proyecto; ingreso redirige a `/`. `inicioOmiteAtrasSinQuitarElBotonDelRestoNiCrearBienvenida`: ambos roles sin Atrás en Inicio. |
| 2 · pie compartido / RNF-002 | `Html.pie`, `Html.pagina`, `.pie`, `body>main` | Prueba de pie «© 2026 APLOMO»; capturas de Inicio/formularios y `safari-pie.png`; estado corto comprobado. |
| 3 y 8 · laterales, dibujo y accesibilidad visual / RNF-002 | `Html.laterales`; reglas `.riel`, `.deco`, keyframes y media queries de `aplomo.css` | `vaciosConservanTextoBotonesYDecoracionesSonSoloPresentacion`; medidas y capturas 1400/1440/1600/1920; decoración aria-hidden y sin interacción. |
| 4 · paneles de formularios / RNF-002 | `Html.conPanel`; formularios de `PaginasEmpresas`, `PaginasReservas`, `PaginasUsuarios` | `panelesSoloEnLosTresFormulariosNuevosConCamposYAccionesIntactos`: creación sí, edición no, campos y protección de formulario conservados; tres formularios revisados en Safari. |
| 5 · conteos reales / RF-024 preservado | `Evaluaciones.resumen`; `Router.semaforo` y `atender` | `semaforoLeeColoresRealesYSeActualizaConLosDatos`: demo 2 Verde / 1 Amarillo / 1 Rojo; cambio reversible a 1/2/1. `ceroPuestosNoMuestraUnaBarraProporcionalVacia`: sin barra con cero. |
| 6 · estados vacíos / RNF-002 y RNF-007 | `Html.vacio` y páginas de consultas, pendientes, sugerencias, calificaciones, usuarios e Inicio | Prueba de texto y CTA conservados; búsqueda sin resultados de Usuarios revisada en navegador; `estado-vacio-1440.png`. |
| Permisos preservados / RNF-004 y RNF-006 | Autorización existente sin cambios | Suite HTTP de permisos en verde; nuevo test comprueba que el Usuario no recibe el semáforo del Administrador. No se añadieron acciones privilegiadas. |
| 9 · verificación / RNF-008 parcial | `HttpMarcaTest` (seis pruebas nuevas), suite completa y QA visual | 724 pruebas aprobadas; Safari instalado revisado. Chrome/Edge/Firefox pendientes por indicación expresa de Andy: sin descargas; Claude realizará QA en Chromium. |

Las únicas adaptaciones al CSS proporcionado son los selectores del markup real: ocultar todo el enlace del logo en cabecera grande (evita un enlace vacío enfocable), envolver la columna del formulario y limitarla a 680 px, y respetar `hidden` en el estado vacío de Usuarios. Los conteos 2/1/1 difieren de los números ilustrativos del PDF porque proceden de datos reales del ensayo.

## Verificación y evidencia

`TZ=America/Bogota ./mvnw -Daplomo.evidencia.rendimiento=docs/capturas_13d2/rendimiento.json test`: **724 ejecutadas, 724 aprobadas; 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. Incluye las seis nuevas de `HttpMarcaTest` y la prueba de rendimiento existente. En un primer pase, una prueba nueva reutilizaba la sesión del Administrador al intentar entrar como Usuario; se corrigió su cliente de prueba para iniciar una sesión nueva. No se cambió la autenticación del producto ni se quitaron comprobaciones.

Las capturas y `mediciones.json` están en `docs/capturas_13d2/`. Son pantallas reales de la aplicación, no imágenes reconstruidas:

| Entorno / ancho | Revisado | Resultado |
|---|---|---|
| Navegador integrado · 1920 | Inicio, Nueva empresa y login | Capturas obligatorias; pie, panel y laterales visibles; sin desbordamiento. |
| Navegador integrado · 1440 y 390 | Inicio | Capturas obligatorias; a 390 sin laterales y pie apilado; sin desbordamiento. |
| Navegador integrado · 1400, 1440, 1500 y 1600 | Inicio y Nueva empresa | Geometría medida sin desbordamiento; capturas adicionales de 1400/1600 revisadas; contenido legible sobre tarjetas opacas, decoraciones sin interacción. |
| Navegador integrado · 390, 810, 1080 y 1366 | Inicio y los tres formularios nuevos | 16 combinaciones adicionales, sin desbordamiento; panel oculto en celular. |
| Safari 26.6.2 (21624.5.1.11.3), ventanas de aproximadamente 895 y 1324 px CSS | Login, Inicio, Nueva empresa, Nueva reserva, Nuevo usuario y pie | Revisión nativa con sesión de ensayo, capturas `safari-*.png`; sin problemas visuales observados. No se atribuyen las capturas de 1920/1440/390 a Safari. |

Total: **28 registros de medidas sin desbordamiento horizontal**. La consola registrada del navegador integrado no mostró errores ni advertencias. El menú móvil y tamaños de botones de 13D-1 se conservaron. La aplicación temporal y Safari usaron solo cuentas ficticias de ensayo; no se guardaron contraseñas. Se cerró la ventana privada de ensayo y la pestaña temporal, restaurando el viewport integrado.

El servidor de QA usó exclusivamente `rula_ergonomia_pruebas`, una transacción reversible y su propia carpeta temporal de fotos. Cerró con **QA13D2: ROLLBACK completado** antes de la suite final. No se cargó el demo ni se hicieron escrituras de QA en `rula_ergonomia`. `config/db.properties` y `fotos/` permanecen fuera del commit.

## Checklist y pendientes

- ✅ 724 pruebas aprobadas, compilación incluida.
- ✅ Mockup comparado con Inicio, login y Nueva empresa; paneles de reserva y usuario revisados en Safari.
- ✅ 390 / 810 / 1080 / 1366 px sin desbordamiento; también 1400–1600 y 1920.
- ✅ Decoraciones sin capturar clics, aria-hidden; controles anteriores de 48 px y encabezado de 44 px conservados.
- ✅ Permisos HTTP de la suite conservados y Usuario sin semáforo administrativo.
- ✅ Textos solicitados en español con tildes; ninguna nueva pantalla de Bienvenida.
- ✅ Consola integrada sin errores ni avisos durante la revisión.
- ✅ Nada sensible preparado para subir; ensayo revertido.
- ❌ Prueba con el dedo sobre iPad/celular físico: no realizada.
- ❌ Recorrido de esta versión en Chrome/Edge/Firefox: pendiente por instrucción de Andy de no descargar; Claude hará QA en Chromium. La revisión en Safari no sustituye los tres navegadores ni las dos versiones estables de RNF-008.
- ❌ Cambio real de la preferencia del sistema de movimiento reducido y prueba de impresión: no repetidos; reglas estáticas solicitadas presentes, sin afirmar comprobación física.

Implementación de los puntos 1–8 pendiente: **nada**. No se hicieron nuevas rutas, cambios de permisos, esquema, dependencias Maven ni lógica RULA. No se descargó software ni se cambió el navegador predeterminado.

## Cómo comprobarlo

1. Abre la app e ingresa: debe llevarte a Inicio sin Atrás. Otra página sí conserva Atrás; el login dice Bienvenido.
2. Como Administrador, compara los tres conteos con los puestos de tu base; con todos en cero no aparece la barra. Verde/Amarillo abren Evaluaciones y Rojo abre Pendientes.
3. A 1920 y entre 1400–1600 px observa el compás, la marca vertical y la plomada; el contenido permanece legible. A menos de 1400 desaparecen los laterales y regresa el logo de cabecera.
4. Abre Nueva empresa, Nueva reserva y Nuevo usuario; aparecen sus consejos. En celular se ocultan los paneles; editar no los añade.
5. Busca un nombre inexistente en Usuarios: aparece el vacío con marca; limpia el filtro y desaparece. El pie debe quedar al final, también en páginas cortas.
6. Ejecuta `TZ=America/Bogota ./mvnw test` con la base de pruebas configurada: deben pasar 724 pruebas. Usa tu navegador para confirmar movimiento reducido e impresión si necesitas completar esas comprobaciones manuales.

## Git y documentación

Se verificó `origin = https://github.com/Andybena2025/Aplomo-app.git`, rama `main`, y se hizo pull antes de implementar: `507720d` incorporó las referencias del mockup. Commit solicitado: **Paso 13D-2: identidad de marca (compás, plomada, pie, semáforo)**. Andy autorizó explícitamente terminar el resto, commit y push aun dejando los tres navegadores pendientes. El hash y resultado real del push se entregan en el resumen final, después de ejecutarlos; no se presentan aquí como acciones ya realizadas.

README actualiza únicamente la fila del Paso 13 y el número de pruebas. INSTALAR solo actualiza ese número; no cambia la instalación. AGENTS registra el estado, y `qa_dispositivos.md` diferencia Safari de las medidas integradas. Se espera el OK de Andy antes de otro paso.
