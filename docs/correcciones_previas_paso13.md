# Correcciones previas al Paso 13

11 nov 2026 · America/Bogota. Andy aprobó el Paso 12 y pidió estas cinco correcciones en un commit separado antes del Paso 13.

| Corrección | Implementación | Comprobación |
|---|---|---|
| Primera página: resumen de toda la empresa | `Calificaciones.cargar` obtiene `InformeEmpresa.totales` con los mismos puestos únicos actuales y el historial Finalizado; `PaginasCalificaciones.resumen` y `.informe-portada` | Prueba `totalesDeEmpresaSumanHistorialYEstadoActualSinDuplicarPuestos`; PDF real: la página 1 contiene general/cobertura, visitas, puestos, fotos, Verde/Amarillo/Rojo, pendientes separados, sugerencias activas por tipo y área con más Rojo; el detalle por sede comienza en la página 2 |
| Área con más Rojo de toda la empresa | Se calcula sobre todos los puestos, sin sumar solo las áreas ganadoras de cada sede | Prueba `areaConMasRojoSeCalculaEnTodaLaEmpresaNoDesdeGanadorasDeCadaSede`: Producción gana globalmente aunque las ganadoras locales sean Logística y Oficina |
| Rótulo del promedio de sede | `PaginasCalificaciones.calificaciones/sede`: «Promedio histórico de sus visitas Finalizadas» | Pantalla y PDF revisados; ninguna modificación a la regla ni a los promedios guardados |
| Hora real de generación | `Evaluaciones()` y `Evaluaciones(Transacciones)` comparten `Clock.system(America/Bogota)`; el constructor explícito de reloj sigue reservado a fixtures | `generadoElUsaRelojRealDeProduccionEnBogotaPorHttp`: timestamp HTTP entre el instante anterior y posterior a la petición, offset −05:00 y texto visible coincidente. Solo se sustituye la conexión por una reversible, no el reloj de producción. QA usa este mismo constructor real: PDF «Generado el 11 nov 2026 18:56 · America/Bogota» |
| Singular/plural | Etiquetas de cifras y contador de visitas; cobertura de una sola sede | `singularEnCalificacionesYEnInformeDeUnaSede`: «1 visita Finalizada», «1 de 1 sede calificada», «Puesto evaluado único» y «Foto analizada» |
| Nota de impresión | Junto al botón: «En el cuadro de imprimir, desmarca Encabezados y pies de página» | Prueba HTTP y revisión visual; PDF guardado con esa opción desmarcada |

RF-025/RF-026 y requisitos transversales RF-002, RNF-002, RNF-004, RNF-006, RNF-007 y RNF-011 conservados. Se mantienen los permisos: informe solo Administrador, calificaciones propias para Usuario y fotos privadas. No se cambian RULA, NEUTRO = 5°, esquema, instalación ni datos existentes.

## Verificación

- ✅ Compilación y **715 pruebas aprobadas**, 0 fallos/errores/omitidas: 710 anteriores y cinco nuevas de totales, área global, contenido, singular y hora real HTTP.
- ✅ QA exclusivamente en `rula_ergonomia_pruebas`, con ROLLBACK confirmado al cerrar el servidor temporal.
- ✅ Quince observaciones de informe, calificaciones e informe de una sede a 390/810/1080/1366/1920 px. Sin desplazamiento lateral; barra en una fila desde 810 px y controles móviles ≥44 px.
- ✅ PDF A4 guardado desde Safari, ocho páginas revisadas. Primera página con resumen de empresa completo; niveles escritos legibles en blanco y negro.
- ✅ Capturas y PDF ficticios en `docs/evidencia_previo13/`.
- ✅ Configuración privada y fotos excluidas de Git; sin borrar información.
- ❌ Prueba táctil física e impresión en papel: no realizadas.
- La revisión de Chrome, Edge y Firefox corresponde al Paso 13, que empieza después de este commit. Andy autorizó copias temporales oficiales para probarlos.

El «10:00» de las primeras capturas del Paso 12 correspondía al reloj fijo de esos fixtures. Las horas históricas de visitas ficticias siguen siendo las de su fixture; la fecha de **Generado el** ahora se comprueba también con el reloj real, sin cambiar el comportamiento de producción.

## Cómo probarlo

1. Ejecuta `./mvnw compile test` (Windows: `mvnw.cmd compile test`): 715 aprobadas.
2. En `http://localhost:8081`, abre una empresa → Generar informe. La cabecera muestra la hora de Bogotá del momento de la petición.
3. Revisa el resumen de empresa, su cobertura y los totales. Imprimir o guardar PDF → A4 → desmarca Encabezados y pies de página: la primera página es el resumen y luego comienza el detalle por sede.
4. Abre Ver calificaciones: cada sede lleva el rótulo del promedio histórico. Con una sola visita, se lee «1 visita Finalizada».

Commit: **Correcciones previas al Paso 13**. El hash se incluye en el informe del Paso 13 y en el cierre, junto con la confirmación del push.
