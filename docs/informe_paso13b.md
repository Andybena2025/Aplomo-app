# Paso 13B · Pulido, carga, datos demo y navegadores

12 nov 2026 · pendiente de revisión de Andy · sin push.

## Qué quedó hecho

Se terminó e integró el trabajo local del Paso 13 interrumpido: avisos de carga, bloqueo de envíos repetidos, recuperación de botones al volver con el historial y prueba de rendimiento. Se conservó el trabajo ya comprobado de los Pasos 13A y 14; no se volvieron a construir sus pantallas ni su detector.

Los formularios normales muestran **Guardando…** al guardar y **Cargando…** al consultar. La evaluación conserva su bloqueo durante la subida de fotos y el guardado; las acciones asíncronas de reservas, análisis, pendientes y sugerencias usan el indicador común si superan un segundo. Los botones de enlace como Generar informe cambian de texto y rechazan activaciones repetidas; se restauran al regresar. El indicador accesible usa `role="status"` y `aria-live`, sin colores de éxito o error.

Se revisaron los avisos en español de formularios, sesión, fotos, cálculo, pendientes, sugerencias e informes. Se corrigió la fuga de un mensaje técnico al recibir un formulario con codificación inválida y se normalizaron las fallas de red de reservas, análisis y evaluación, que podían mostrar el texto en inglés del navegador. Las confirmaciones usan 4–5 segundos; errores y advertencias se conservan hasta cerrarlos. Se respeta movimiento reducido al desplazar el formulario hacia un error.

El título literal `Postura <b>de prueba</b>` venía de datos sintéticos de QA usados para comprobar que el HTML se muestra como texto seguro. No era un fallo tipográfico. En este recorrido se usaron nombres legibles como «Ensamble de piezas pequeñas · Firefox», manteniendo intacta la protección contra HTML inyectado.

## Requisitos y evidencia

| Requisito | Archivo y método / punto de aplicación | Comprobación |
|---|---|---|
| RNF-002 · guía y adaptación | `aplomo.css`, bloque `@media(pointer:coarse)`; `evaluacion.js`, `mostrar` / `aviso` | Inicio, Reservas y Usuarios a 390/810/1080/1366/1920 px sin desbordamiento; emulación táctil Chrome 390/810. Capturas y `anchos.json`. Se conserva el asistente del mock. |
| RNF-003 · carga y bloqueo | `progreso.js`, `iniciar`, `limpiar`, eventos submit/click/pageshow; `cuenta.js`, manejador submit; `evaluacion.js`, envío; hooks de `analisis.js`, `reservas.js`, `pendientes.js`, `sugerencias.js`; inclusión desde `Html.pagina` y login | Demoras de QA de 8 s: Guardando… desactivado en tres navegadores; Generar informe cambia a Cargando…. Filtros de reservas con demora de 4 s: botón desactivado. Evidencias `*-guardando.png`, `chrome-informe-cargando.png`, `filtros-cargando.png`. |
| RNF-003 · rendimiento (pendiente recuperado del Paso 13) | `CargaMilRegistros.preparar`; `HttpRendimientoTest.listadosGuardadoEInformeCumplenCincoSegundosEnPercentil95` | 1.000 filas en 16 tablas operativas; 20 repeticiones por acción; P95 listado 172,77 ms, guardado 38,47 ms e informe 164,00 ms, todos ≤5.000 ms. `evidencia_paso13b/rendimiento.json`. |
| RNF-007 · mensajes comprensibles | `Router.parametros`; avisos existentes de formularios, sesión y fotos; `progreso.js.confirmar` / `cuenta.js` | `HttpSeguridadTest.formularioDañadoMuestraUnMensajeClaroSinDetallesTecnicos`; suite HTTP de errores, sesión y guardado conservando campos. Recorrido visual con tildes y acciones claras. |
| RNF-008 · navegadores | Mismas clases Java, HTML/CSS/JS de producción, servidor local temporal | Login → empresa → reserva → visita → evaluación con JPG → resultado → informe → salir en Chrome 156.0.8078.80, Edge 157.0.4341.46 y Firefox 160.0. Detalle y límites en `qa_dispositivos.md`. No equivale a certificar las dos últimas versiones estables de cada uno. |
| Datos de demostración opcionales · docs/06 Paso 13 | `datos_demo.sql`, inserciones condicionadas; `INSTALAR.md`, sección manual | `HttpDatosDemoTest.demoEsIdempotenteConservaEdicionesYCubreLosTresColoresEnElInforme`: dos cargas sin duplicados, tres sedes de la universidad, mantiene un pendiente editado, tres colores presentes en informe. |
| Exactitud del demo · RF-018/RF-020/RF-027 preservados | SQL demo; motor existente `MotorRula.calcular`, `TablasRula.cargar` | La prueba compara los casos manuales 4, 2 y 8, sus 15 mediciones y los resultados 1/3/7 con el motor y las tablas MySQL. No se cambió NEUTRO=5°, motor ni tablas. |
| RNF-004 / RNF-006 · permisos y privacidad conservados | Router y autorización existentes; fotos solo en carpeta temporal del ensayo | Usuario por dirección directa a `/usuarios` e informe de otra empresa: Acceso denegado. Suite HTTP de roles, empresas, resultados En curso y fotos en verde. `config/db.properties` y `fotos/` fuera del commit. |

## Demo manual

El archivo opcional incluye Ensambles del Quindío, Muebles Cafeteros y Corporación Universitaria Empresarial Alexander von Humboldt. La universidad tiene Sede principal, Sede Alcázar y Sede Anova (Medicina). Incluye cinco reservas, una visita Finalizada, tres evaluaciones con puntajes 1/3/7, un pendiente abierto y sugerencias. Las identidades de contacto son ficticias; los nombres/direcciones del mock no se presentan como verificados hoy.

El script **solo inserta lo que falta**: no cambia el esquema, no borra, no modifica estados ya editados y no crea cuentas ni contraseñas. Requiere un Administrador activo existente. Usa identificadores de demostración para no duplicarse. `INSTALAR.md` explica copiar la imagen ilustrativa a los tres nombres de archivo y ejecutar el SQL voluntariamente. Los resultados son casos manuales de demostración; no son mediciones de la foto ilustrativa ni de personas reales.

No se añadió ejecución automática a los instaladores ni al arranque. Durante este trabajo solo se ejecutó dentro de la transacción de QA en `rula_ergonomia_pruebas`; **no se cargó en `rula_ergonomia`**.

## Pruebas y recuperación del Paso 13

`TZ=America/Bogota ./mvnw test`: **718 ejecutadas, 718 aprobadas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS, duración 66 s, final 12 nov 2026 15:25:40 -05. La compilación está incluida. Se conservaron las pruebas existentes y se añadieron tres comprobaciones: formulario mal codificado, SQL demo idempotente/exacto y rendimiento.

El rendimiento recuperado del Paso 13 sigue siendo una prueba local de una acción por vez, con conexión MySQL local y datos reversibles; no simula concurrencia ni latencia de un despliegue remoto. Las 16 tablas operativas llegan a 1.000 filas cada una; los catálogos RULA no se inflan ni se modifican. Se conserva también `evidencia_paso13/rendimiento.json`, resultado anterior que estaba sin commit, como evidencia histórica; los valores de esta entrega son los de `evidencia_paso13b/rendimiento.json`.

Para comprobar los estados de carga se usaron retardos de 4/8 s únicamente en el servidor temporal de QA. Esos retardos no forman parte del código entregado ni de las mediciones de rendimiento. El servidor temporal cerró con **ROLLBACK completado** y retiró su carpeta temporal de imágenes; no quedaron registros de los recorridos. La última revisión detectó avisos de red que podían quedar en inglés; se normalizaron en reservas, análisis y evaluación, y se repitió la suite completa con el resultado indicado. La simulación JS del formulario de reserva comprobó doble envío bloqueado, error de red en español conservando datos y recuperación para reintentar; `node src/test/js/reservas-carga.mjs` (3 escenarios).

## Qué no se hizo o quedó pendiente

- Prueba con el dedo en un dispositivo físico: no disponible. Se comprobó emulación táctil y reglas de área mínima, sin presentarlas como prueba física.
- Certificar las **dos últimas versiones estables** de cada navegador: pendiente. Se completó el flujo en las tres versiones indicadas, utilizando copias existentes, sin descargar otras. No se afirma qué versiones son hoy las últimas estables.
- Captura automática de toda la consola de Edge y Firefox: no realizada. La consola registrada del navegador integrado no tuvo errores/avisos; Chrome mostró la consola vacía durante el ensayo de Usuarios.
- No se rehízo la impresión/PDF del Paso 12 ni el detector del Paso 14, ya comprobados. Este recorrido abrió el informe en los tres navegadores.
- No se cambió lógica RULA, SQL de creación, rutas, permisos, cuentas reales, tipografía, paleta ni el ancho de diseño del Paso 13A. No se hizo push ni despliegue. El Paso 13 general queda pendiente de revisión y de la cobertura indicada, no aprobado por inferencia.

## Checklist

- ✅ Compilación y 718 pruebas en verde.
- ✅ Diseño del mock conservado: asistente, toma de fotos, semáforo e informe; Inicio/Reservas/Usuarios del Paso 13A. Capturas en `docs/evidencia_paso13b/`.
- ✅ 390 / 810 / 1080 / 1366 / 1920 px sin deslizamiento horizontal en Inicio, Reservas y Usuarios.
- ✅ Emulación Mobile de Chrome a 390/810, controles táctiles con mínimo 44 px; filtros y navegación operables.
- ❌ «Funciona con el dedo» en equipo físico: no se pudo comprobar.
- ✅ Usuario sin acceso a acciones de Administrador, por dirección directa; suite de aislamiento por empresa / fotos en verde.
- ✅ Avisos revisados en español con tildes; caso de formulario corrupto cubierto.
- ✅ Consola registrada del navegador integrado sin errores/avisos; consola visible de Chrome vacía en Usuarios.
- ❌ Registro exhaustivo de consola de Edge/Firefox y matriz de dos versiones estables por navegador: pendientes.
- ✅ Datos y fotos de QA reversibles; ROLLBACK observado; sin datos sensibles versionados.
- ✅ Un commit para recuperar lo pendiente y entregar Paso 13B; sin push por instrucción de Andy.

## Cómo probarlo

1. Mantén MySQL activo. Desde la raíz ejecuta `TZ=America/Bogota ./mvnw test`: deben pasar 718 pruebas en la base separada.
2. Para ver la app, inicia con el procedimiento de `INSTALAR.md` y abre su dirección local. Entra con tu cuenta existente; esta entrega no añade credenciales.
3. Si quieres datos demo, sigue **Datos de demostración (opcional y manual)** en `INSTALAR.md`, primero con la base de pruebas. Repite solo `datos_demo.sql`: no debe duplicar empresas/reservas/evaluaciones ni restablecer estados editados. No repitas el script que crea la base.
4. Como Administrador abre una empresa, crea y acepta una reserva, inicia visita, evalúa un puesto con foto de muestra y consentimiento. Revisa puntos/ángulos y semáforo; guarda. Durante una espera el botón queda desactivado; no debe guardarse dos veces.
5. Abre Generar informe y revisa el resumen de la empresa, cobertura de sedes y niveles escritos. Solo aparecen resultados de visitas Finalizadas. Comprueba la nota de desactivar encabezados y pies de página al imprimir.
6. Aplica filtros y vuelve con Atrás: los botones deben recuperar su texto y poder usarse de nuevo. Los errores conservan campos/fotos; las confirmaciones desaparecen y los errores se cierran a mano.
7. En modo responsive revisa 390, 810, 1080, 1366 y 1920 px. Para área táctil usa emulación Mobile; la validación con dedo real queda para un equipo físico.
8. Con rol Usuario intenta `/usuarios`, una empresa ajena y una foto: debe negar acceso. Cierra sesión y vuelve a una dirección interna: debe exigir ingresar.

## Git y estado

Commit de esta entrega: `Paso 13B: pulido, carga, datos demo y navegadores`. Se integra en el mismo commit el trabajo del Paso 13 que estaba sin confirmar. Los commits anteriores `13a9aa5` (13A), `abfac12` (14) y `3ddcc32` (correcciones previas) se conservan. El identificador del nuevo commit se entrega después de crearlo; se puede comprobar con `git log -1 --oneline`.

**Push: no realizado**, por instrucción explícita. `AGENTS.md` registra lo terminado y los límites pendientes. `README.md` cambia únicamente la fila del Paso 13 y el número de pruebas. Se espera el OK de Andy.
