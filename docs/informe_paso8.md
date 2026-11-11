# Informe del Paso 8 · APLOMO.

Fecha: 11 de noviembre de 2026. Zona: America/Bogota.

## Qué se hizo

Se continúa el asistente aprobado del Paso 7 hasta el resultado:

- **Paso 4 · Ángulos:** foto ampliada, seis puntos anatómicos, líneas y arcos del mock, marcado y arrastre con mouse; control con Pointer Events, `touch-action:none` y radio de agarre de 44 px para el dedo. Cada segmento admite corrección manual; se conserva si vino de puntos o se escribió a mano. Se elige el lado derecho o izquierdo.
- **Paso 5 · Ajustes y carga:** casillas observadas por el Administrador, giro de muñeca, apoyo de piernas, una actividad muscular y una carga comunes a ambos grupos. Incluye las seis cargas autorizadas y vista previa del servidor.
- **Paso 6 · Resultado:** puntaje grande, color y nombre, acción y nivel oficial, tabla de ángulos y puntos, Tablas A/B, incrementos de actividad y carga y totales ajustados. Se ve antes de guardar.
- **Guardar:** en una evaluación nueva se guardan evaluación, condiciones, fotos, análisis, mediciones y bitácora en una sola transacción. Si falla, se revierten los datos y se retiran solo los archivos nuevos de esa operación. También se puede continuar una evaluación ya creada en el Paso 7.
- **Historial:** cada foto analizada tiene sus cinco mediciones y su lado. Volver a analizar crea un registro nuevo; conserva el anterior. El puesto toma el máximo de su última evaluación. Una evaluación anterior no pisa el resultado de una más reciente. Una visita finalizada conserva la consulta y no permite añadir análisis.
- **Inicio:** Visitas en curso, Reservas en cola, Evaluaciones y Puestos en rojo, con el número de esta última tarjeta en rojo. Los datos del Usuario se limitan a su empresa; sus resultados solo cuentan visitas finalizadas.

Las fotos de frente o detalle pueden conservarse como apoyo sin inventar ángulos que no muestran. Cada foto seleccionada para analizar tiene datos independientes; se requiere al menos una con medición completa. Las evaluaciones **Sin registro fotográfico** conservan su recorrido sin fotos ni puntaje; no se inventa una fotografía para cumplir la relación de análisis → foto del esquema. Se mantiene intacta la aclaración de consentimiento del Paso 7 y no se pide motivo de ausencia de fotos.

## Cálculo y datos

El cálculo vive exclusivamente en `MotorRula`, `PuntuadorSegmentos`, `MedidorAngulos` y `TablasRula`:

- Las tablas A/B/C y el semáforo se leen de MySQL una vez, con validación de integridad. No se copian en JavaScript.
- **NEUTRO = 5°**: muñeca y tronco de hasta 5° tienen base 1; cuello y tronco con hasta 5° hacia atrás cuentan como flexión. La regla también cubre ángulos manuales.
- El navegador envía puntos normalizados, ángulos manuales y casillas, nunca un puntaje confiable. El servidor calcula para la vista previa y vuelve a calcular al guardar; ignora puntajes e IDs de nivel adulterados.
- Al guardar se leen las dimensiones de la foto real del servidor; las dimensiones declaradas por el navegador no pueden cambiar el puntaje guardado. Se contempla la orientación EXIF de fotos JPG y PNG del teléfono sin modificar sus bytes.
- Se guardan coordenadas, grados, puntos por segmento y la marca de corrección manual. Los grados se muestran con decimales; la columna existente los conserva con dos decimales. La consulta histórica usa los puntos y totales guardados para no volver a puntuar un ángulo redondeado.
- Se corrigió el texto antiguo de `pruebas_fase_2.md` que atribuía a Andy quitar la tolerancia. También se corrigió el desglose del primer ejemplo: con tronco 2°, NEUTRO=5° da tronco=1, Tabla B=1 y final=2.

No se cambió el esquema, no se añadieron librerías ni frameworks y no se borraron datos ni documentos.

## Requisitos → archivo y método → prueba

Los archivos Java de implementación están en `src/main/java/co/aplomo/`; los tests, en `src/test/java/co/aplomo/`.

| Requisito | Archivo y método | Comprobación |
|---|---|---|
| RF-017 · medir y corregir ángulos | `rula/MotorRula.java`: `calcular(Entrada)`; `rula/MedidorAngulos.java`: `brazo`, `antebrazo`, `muneca`, `cuello`, `tronco`; `web/DatosAnalisis.java`: `leer`; `static/js/analisis.js`: marcado, arrastre y corrección | `AnalisisTest.calculaPuntosConDimensionesRealesAunqueElClienteMienta`, `noCalculaEntradasImposibles` (5 casos), pruebas geométricas existentes y revisión visual marcando, arrastrando y escribiendo los cinco ángulos |
| RF-018 · puntos y Tablas A/B/C | `rula/PuntuadorSegmentos.java`: `puntuar`; `rula/MotorRula.java`: ambos `calcular`; `rula/TablasRula.java`: `buscarA/B/C`; `web/PaginasAnalisis.java`: `resultado` | Diez casos de `MotorRulaTest`, `AnalisisTest.diezCasosConVistaPreviaGuardadoYLectura`, `HttpAnalisisTest.diezCasosEnLaPantallaDeResultado`; 272 combinaciones de tablas existentes |
| RF-019 · actividad y carga | `MotorRula.calcular(Angulos, Ajustes, boolean, Carga)`; `PaginasAnalisis.pasos`; `DatosAnalisis.leer` | Los diez casos cubren las seis cargas, actividad común y saturación de C; casos 5–10 comprobados por HTTP, persistencia y pantalla |
| RF-020 · semáforo y nivel oficial | `TablasRula.nivelDe`; `PaginasAnalisis.resultado`, `guardado` | Diez capturas con puntaje, color, acción y niveles oficiales 1–4; HTML HTTP y lectura posterior coinciden con el esperado |
| RF-021 · análisis por foto e historial | `servicio/Analisis.java`: `guardar`, `guardarEnTransaccion`, `consultar`; `dao/AnalisisDao.java`: `guardar`, `actualizarPuesto`, `listar`, `mediciones`; `servicio/Evaluaciones.java`: sobrecarga `guardar` | `diezFotosGuardanCincuentaMedicionesSinMezclarse`, `ladosIndependientesYResponsable`, `repetirNoReemplazaYLaUltimaEvaluacionManda`, `fallaTodoSinDejarEvaluacionFotoNiArchivoNuevos`; guardado completo por HTTP |
| RF-027 · tablas desde MySQL | `TablasRula.cargar(Connection)` | Pruebas existentes comparan las 272 combinaciones contra MySQL y el SQL aprobado; no se añadieron tablas en el navegador |
| RF-024 · tarjetas solicitadas de Inicio | `Evaluaciones.resumen`; `web/Router.java`: Inicio y `kpi` | `tarjetasNoRevelanResultadosDeVisitasEnCursoAlUsuario`; prueba HTTP de títulos y número rojo; pantalla con 10 evaluaciones y 3 puestos rojos |
| RF-002, RNF-004/006 · permisos y privacidad | `Router.atender`; `EvaluacionesWeb.atender`; `Analisis.admin/guardar/consultar`; `DatosAnalisis.leer/campos` | `HttpAnalisisTest.puntajeAdulteradoSeIgnoraYElHistorialQuedaProtegido`, `csrfYEntradasInvalidasNoGuardan`; Usuario denegado por direcciones directas de foto, asistente y resultado |
| RNF-005/009 · responsable e historial | `AnalisisDao.guardar/listar/consultar`; `Analisis.guardarEnTransaccion`; `PaginasAnalisis.guardado` | `ladosIndependientesYResponsable`, `falloDeBitacoraRevierteAnalisisYMediciones`, `visitaFinalizadaConservaElResultadoYOcultaElBotonDeAnalizar`; captura de dos análisis de una foto |
| RNF-002/007 · recorrido y mensajes | `PaginasAnalisis.pasos`; `static/js/evaluacion.js`: `mostrar`, envío y recuperación; `analisis.js`: datos por foto | Recorrido real 1→6, volver 6→5→4 sin perder valores, guardar y abrir resultado; errores HTTP en español sin datos SQL; confirmación de cinco segundos y controles de envío bloqueados |
| RNF-010/011 · exactitud y separación | Cuatro clases `rula/`; datos en DAO, permisos/transacciones en servicio e interfaz en `web/` | Suite completa, pruebas de límites de cuello a −5° y −5.01°, diez casos explícitos sin obtener los esperados del propio motor; sin dependencias nuevas |

## Pruebas y diez casos obligatorios

`mvn compile test`: **573 ejecutadas, 573 aprobadas, 0 fallos, 0 errores, 0 omitidas**.

La ejecución final se repitió en el proyecto de IntelliJ el 11 de noviembre a las 11:07 (America/Bogota). El resumen está en [verificacion-final.txt](evidencia_paso8/verificacion-final.txt).

Son **41 casos adicionales** frente a las 532 del Paso 7: 25 en `AnalisisTest`, 14 en `HttpAnalisisTest` y dos límites adicionales de cuello. Incluyen diez fotos guardadas con cincuenta mediciones sin mezclarse, permisos GET/POST, CSRF, puntajes manipulados, dimensiones reales, orientación EXIF, historial, visitas cerradas y reversión completa por fallo de auditoría.

Los diez casos se comprobaron en el motor, vista previa HTTP, guardado/lectura y **pantalla real**. Las capturas están en `docs/evidencia_paso8/caso-1.jpg` a `caso-10.jpg`, con el registro de lo mostrado en `diez-casos.json`.

| Caso | A / B | Actividad / carga | Final esperado | Final en pantalla | Semáforo / nivel |
|---|---|---|---:|---:|---|
| 1 · 1°,85°,9°,10°,2° | 2 / 1 | 0 / 0 | 2 | 2 | Verde / 1 |
| 2 · Ejemplo | 3 / 3 | 0 / 0 | 3 | 3 | Amarillo / 2 |
| 3 · Ejemplo + hombro elevado y separado | 5 / 3 | 0 / 0 | 4 | 4 | Amarillo / 2 |
| 4 · Neutro | 1 / 1 | 0 / 0 | 1 | 1 | Verde / 1 |
| 5 · Neutro + actividad | 1 / 1 | 1 / 0 | 2 | 2 | Verde / 1 |
| 6 · Neutro + carga intermitente 2–10 kg | 1 / 1 | 0 / 1 | 2 | 2 | Verde / 1 |
| 7 · Ejemplo + carga estática 2–10 kg | 3 / 3 | 0 / 2 | 6 | 6 | Rojo / 3 |
| 8 · Ejemplo + actividad y carga >10 kg intermitente | 3 / 3 | 1 / 2 | 7 | 7 | Rojo / 4 |
| 9 · Neutro + carga >10 kg estática | 1 / 1 | 0 / 3 | 4 | 4 | Amarillo / 2 |
| 10 · Máximo + actividad y golpes | 9 / 9 | 1 / 3 | 7 | 7 | Rojo / 4 |

Caso 10: los totales ajustados siguen siendo 13/13; solo al consultar C se limita a su fila 8 y columna 7. Los ángulos, casillas y desglose manual de los diez casos están en `pruebas_fase_2.md` y en las entradas explícitas de `CasosRulaPrueba`.

## Checklist de AGENTS.md

- ✅ `mvn test`: 573 aprobadas.
- ✅ Comparación con el mock: paso 4 con foto, puntos y arcos; paso 5 con casillas y carga; paso 6 con círculo, semáforo, nivel oficial y desglose. Se respetan las diferencias aprobadas: dos roles, ajustes manuales, carga común, sin IA ni ficha PDF futura.
- ✅ 390 / 810 / 1080 / 1366 px sin deslizar de lado. También 1920 px; se midieron documento y navegación y se guardaron capturas.
- ❌ Prueba con el dedo en un dispositivo físico: no se pudo realizar desde esta sesión. Sí se comprobó el mouse y el arrastre real, controles móviles de 44 px y el código táctil del mock adaptado con Pointer Events y radio de agarre de 44 px. Esto no sustituye tocar un teléfono o iPad real.
- ✅ Usuario bloqueado al abrir directamente foto, asistente y resultado; permisos y CSRF comprobados también por HTTP.
- ✅ Tildes y textos revisados en los pasos, ángulos, ajustes, carga, resultado e historial.
- ✅ Sin errores de JavaScript ni advertencias registradas en los recorridos válidos revisados; rechazos 400/403 de pruebas negativas son deliberados.
- ✅ Sin credenciales ni fotografías privadas en Git. `config/db.properties` y `fotos/` siguen ignorados. Las capturas muestran únicamente datos ficticios e imágenes públicas de demostración.

| Ancho | Paso 4 | Paso 5 | Paso 6 | Desplazamiento horizontal |
|---|---|---|---|---|
| 390 px | Lienzo y controles apilados; botones de 44 px | Casillas se ajustan al ancho | Círculo y tabla adaptados | No |
| 810 px | Rejilla se convierte en una columna | Campos y casillas dentro de sus tarjetas | Resultado y tabla legibles | No |
| 1080 px | Foto y segmentos en dos columnas | Casillas distribuidas sin salir de la tarjeta | Desglose completo | No |
| 1366 px | Distribución de laptop del mock | Tarjetas, casillas y vista previa | Círculo y desglose como referencia | No |
| 1920 px | Contenido centrado con ancho acotado | Misma proporción visual | Resultado sin estirar el contenido | No |

Las medidas están en `evidencia_paso8/mediciones.json`. Las capturas pueden excluir la barra vertical; el nombre indica el viewport en CSS px.

## Lo que NO se hizo o quedó pendiente

- Del alcance funcional del Paso 8: **nada a medias**.
- Pendiente de comprobación: dedo y cámara en dispositivo físico, y recorrido completo en Chrome, Edge y Firefox. La revisión visual se hizo en el navegador integrado; no se presenta como prueba de los tres navegadores.
- **No se empezó el Paso 9:** el resultado rojo actualiza el puesto y la tarjeta de Inicio, pero todavía no crea pendientes de arreglo.
- No se implementaron sugerencias del Paso 10, consultas completas del Paso 11 ni informe del Paso 12.
- No se añadió IA del Paso 14 ni ficha PDF por evaluación (visión futura).

## Protección de la base y revisión visual

La revisión se ejecutó exclusivamente en `rula_ergonomia_pruebas`, con datos ficticios en una transacción sin confirmar. Al cerrar el servidor temporal se comprobó **ROLLBACK confirmado** y se retiraron solo sus fotos temporales. No se ejecutó el SQL de instalación ni se cambió el esquema.

La auditoría de solo lectura posterior a la revisión y las pruebas encontró **0** empresas llamadas **“Empresa de prueba sin confirmar”** en `rula_ergonomia` y **0** en `rula_ergonomia_pruebas`. No se borraron datos de la base real.

Las fechas del entorno de prueba están fijadas al 10 de noviembre para los datos ficticios; el servidor normal conserva la hora de America/Bogota. Las rutas de QA y el lanzador temporal no forman parte del producto ni del commit.

## Cómo probarlo paso a paso

1. Mantener MySQL encendido, abrir `http://localhost:8081` y entrar como Administrador. Actualizar una pestaña que siguiera abierta con la versión anterior.
2. Abrir una visita **En curso → Evaluar puesto**. Completar puesto, postura, criterio, motivo, minutos y las seis condiciones.
3. En Fotos, elegir Con registro fotográfico, marcar aceptación y seleccionar JPG/PNG válidos con vista y descripción. Continuar.
4. En Ángulos, elegir el lado y marcar cadera, hombro, codo, muñeca, nudillos y coronilla. Arrastrar un punto y comprobar que Java devuelve otra medición.
5. Probar Escribir ángulo a mano en un segmento. Para reproducir el ejemplo, escribir brazo 61°, antebrazo 47°, muñeca 0°, cuello 37° y tronco 17°.
6. Continuar a Ajustes. Sin ajustes, sin actividad y con menos de 2 kg intermitente, el ejemplo da 3. Marcar Hombro levantado y Brazo separado: da **4 · Amarillo · Nivel 2**.
7. Continuar a Resultado. Revisar puntaje y desglose. Volver a los pasos anteriores: se conservan puntos, correcciones, fotos y casillas. Guardar y abrir Ver resultado.
8. Pulsar Analizar fotos / añadir análisis para crear otro registro de esa foto. Elegir el lado correspondiente, medir y guardar: el análisis anterior permanece. Una visita finalizada conserva los resultados y oculta esta acción.
9. Probar varias fotos con entradas diferentes y cambiar entre sus pestañas. No se mezclan sus mediciones. Si una imagen es solo de apoyo, desmarcar Analizar esta foto; al menos una debe tener datos completos.
10. Revisar Inicio: sus cuatro tarjetas se actualizan desde MySQL y Puestos en rojo muestra el número en rojo.
11. Con Usuario, abrir directamente las direcciones del asistente, resultado administrativo y foto: debe aparecer Acceso denegado. Sus datos consultables pertenecen a su empresa y sus resultados a visitas finalizadas.
12. Con Sin registro fotográfico se conserva la evaluación sin fotos ni puntaje, sin pedir un motivo adicional.
13. Repetir `mvn compile test`: debe aparecer BUILD SUCCESS y 573 pruebas aprobadas. Comprobar el dedo y la cámara en un dispositivo físico antes de considerar aprobada esa parte.

## Capturas

| Ancho | Ángulos | Ajustes | Resultado |
|---|---|---|---|
| 390 px | [Paso 4](evidencia_paso8/390-paso4.jpg) | [Paso 5](evidencia_paso8/390-paso5.jpg) | [Paso 6](evidencia_paso8/390-paso6.jpg) |
| 810 px | [Paso 4](evidencia_paso8/810-paso4.jpg) | [Paso 5](evidencia_paso8/810-paso5.jpg) | [Paso 6](evidencia_paso8/810-paso6.jpg) |
| 1080 px | [Paso 4](evidencia_paso8/1080-paso4.jpg) | [Paso 5](evidencia_paso8/1080-paso5.jpg) | [Paso 6](evidencia_paso8/1080-paso6.jpg) |
| 1366 px | [Paso 4](evidencia_paso8/1366-paso4.jpg) | [Paso 5](evidencia_paso8/1366-paso5.jpg) | [Paso 6](evidencia_paso8/1366-paso6.jpg) |
| 1920 px | [Paso 4](evidencia_paso8/1920-paso4.jpg) | [Paso 5](evidencia_paso8/1920-paso5.jpg) | [Paso 6](evidencia_paso8/1920-paso6.jpg) |

- [Inicio con las cuatro tarjetas](evidencia_paso8/inicio.jpg).
- [Puntos y arcos](evidencia_paso8/lienzo-con-arcos.jpg), [punto arrastrado](evidencia_paso8/punto-arrastrado.jpg) y [valores conservados al volver](evidencia_paso8/datos-conservados.jpg).
- [Resultado guardado](evidencia_paso8/resultado-guardado.jpg) e [historial de dos lados de una foto](evidencia_paso8/historial-dos-lados.jpg).
- [Usuario: asistente denegado](evidencia_paso8/usuario-asistente-denegado.jpg), [resultado denegado](evidencia_paso8/usuario-resultado-denegado.jpg) y [foto denegada](evidencia_paso8/usuario-foto-denegada.jpg).
- Casos manuales: [1](evidencia_paso8/caso-1.jpg), [2](evidencia_paso8/caso-2.jpg), [3](evidencia_paso8/caso-3.jpg), [4](evidencia_paso8/caso-4.jpg), [5](evidencia_paso8/caso-5.jpg), [6](evidencia_paso8/caso-6.jpg), [7](evidencia_paso8/caso-7.jpg), [8](evidencia_paso8/caso-8.jpg), [9](evidencia_paso8/caso-9.jpg), [10](evidencia_paso8/caso-10.jpg).

## Git y punto de parada

Commit del paso: `Paso 8: análisis RULA, resultado e Inicio`.

Para ver los commits nuevos: `git log --oneline 45b76d0..HEAD`. El identificador definitivo y la confirmación del push se entregan en el chat después de ejecutarlos. Se actualizó Estado de `AGENTS.md` para registrar la aprobación del Paso 7 y dejar el Paso 8 pendiente de aprobación.

**Esperar el OK de Andy antes del Paso 9.**
