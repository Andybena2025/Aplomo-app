# Paso 23 · Historial de inactivos, Atrás y guía manual

20 nov 2026. Pendiente de revisión de Andy. Sin capturas ni carpetas de evidencia.

## Qué cambió

- **Historial del puesto** en `/puestos/{id}` para Administrador: datos, jerarquía con estados, área/cargo y todas sus evaluaciones, fecha descendente e ID descendente para desempatar. Resultado con `Html.riesgo`, visita/reserva, estado y enlace Ver. Vacío claro si no hay evaluaciones. Los puestos del salón enlazan «Ver historial».
- Empresa, sede y salón inactivos muestran aviso administrativo y enlace a sus evaluaciones filtradas. Toda empresa ofrece «Ver evaluaciones de la empresa» al Administrador. Las cinco listas señalan los niveles inactivos y sus catálogos los incluyen marcados. **Registros** admite Todos (defecto), Solo activos y Solo inactivos; se valida en servidor y no sustituye el estado propio de la fila. Inactiva significa empresa/sede/salón o puesto desactivado; en Visitas solo los niveles que pertenecen a esa fila.
- Usuario conserva su empresa y resultados Finalizados, sin Registros ni historial administrativo; su puesto propio conserva la redirección al salón y uno ajeno sigue con 403. Consultar no reactiva registros. Calificaciones e informe HTML siguen disponibles con empresa inactiva.
- Atrás desaparece solo del **cambio obligatorio** de contraseña; Inicio sigue sin él y el cambio voluntario lo conserva. El botón interno de evaluación no cambia.
- Manual: guía alineada, instrucción «Paso 1 de 5: toca el hombro», un pulso de 0,9 s y desplazamiento suave a la foto solo si su inicio no está visible. Con movimiento reducido no se anima y el desplazamiento es inmediato. Se mantiene la altura mínima previa del bloque y, durante manual, se desactiva la fijación de la foto que interfería con la alineación.

## Requisito → archivo → prueba

| Requisito | Archivo/método principal | Comprobación |
|---|---|---|
| RNF-009, RF-028/024 | Empresas.historialPuesto, ConsultaDao.historialPuesto, EmpresasWeb/PaginasEmpresas | Empresa/puesto inactivos; historial completo y orden/desempate; estados y bitácora sin cambios. |
| RF-024, RF-002 | SeccionConsulta, Consultas.listar, ConsultaDao.catalogos, PaginasConsultas | Cinco listas, cuatro niveles, catálogos, valores inválidos y Usuario por dirección directa. |
| RF-020/025/026 | Html.riesgo; calificaciones e informe existentes, sin cambios | Verde/Rojo en historial; calificación e informe 200 de empresa inactiva. |
| RF-001c, RNF-002 | Html.contrasena | Inicio/obligatorio sin Atrás; Empresas/Usuarios/detalle/cambio voluntario con él; botón interno intacto. |
| RF-017, RNF-007 | marcado-manual.js; analisis.js: actualizarGuia/iniciarManual; conversacion-evaluacion.js; ConversacionEvaluacion; aplomo.css | Cuatro casos JS; medidas y progresión real hasta completar cinco marcas, sin guardar. |
| RNF-011 | Comentarios y docs/ENDPOINTS.md | Inventario de rutas/documentación aprobado. |

## Pruebas y medidas

**984/984 Maven** y **114/114 JS** aprobadas; **0 fallos, errores u omitidas**. Nuevas: **19 HTTP y 4 JS**. Pruebas nuevas en `HttpHistorialInactivosTest.java` y `scripts/qa_guia_manual.cjs`. Java usa `rula_ergonomia_pruebas` y `America/Bogota`. La suite completa se ejecutó una vez al final; previamente se repitió solo el caso que falló por esperar plural donde el fixture tenía una visita, corrigiendo la expectativa singular.

Desborde horizontal de toda la página, medido en navegador integrado:

| Página/lista | 360 | 390 | 820 | 1366 |
|---|---:|---:|---:|---:|
| Empresa inactiva | 0 | 0 | 0 | 0 |
| Sede inactiva | 0 | 0 | 0 | 0 |
| Salón inactivo | 0 | 0 | 0 | 0 |
| Historial del puesto | 0 | 0 | 0 | 0 |
| Evaluaciones | 0 | 0 | 0 | 0 |
| Puestos | 0 | 0 | 0 | 0 |
| Pendientes | 0 | 0 | 0 | 0 |
| Sugerencias | 0 | 0 | 0 | 0 |
| Visitas | 0 | 0 | 0 | 0 |
| Evaluación en modo manual | 0 | 0 | 0 | 0 |

Valores en **px**, 40 medidas. Se conserva el desplazamiento interno de las tablas cuando hace falta. Registros probado con controles reales: 0 evaluaciones en Solo activos y 4 en Solo inactivos para la empresa de ensayo.

Guía manual: separación bajo la imagen **7 px** en 360/390; diferencia de borde superior **0 px** en 820/1366. En 1366, foto antes/después: posición documental **595,59 → 595,59 px**, altura **514 → 514 px**; pulso observado `manualPulso`, una repetición. Cinco marcas completadas sin guardar resultados. **0 errores de consola capturados**. El ensayo fue revertido: **ROLLBACK confirmado**.

Comprobación dirigida de rendimiento, 1.000 registros en 16 tablas: P95 listado **121,32 ms**, guardado **20,06 ms**, informe **160,69 ms**. Es una medición local de ensayo.

## Checklist y límites

- ✅ Pruebas completas, permisos por dirección, HTML escapado, estados sin reactivar y textos claros.
- ✅ Motor RULA, POST `/evaluaciones/calcular`, esquema, IA y fotos de galería sin cambios. Sin dependencias nuevas, borrados ni claves privadas en Git.
- ✅ Guía y tablas medidas sin capturas. Documentados historial, Registros y Atrás; contadores/estado actualizados.
- ❌ No se probó iPad físico, tacto, Windows, otras versiones de navegadores, impresión PDF real ni la preferencia de movimiento reducido en un dispositivo físico. La rama de movimiento reducido está configurada en JS/CSS.

Pendiente de implementación de este encargo: **nada**. Railway/hosting, Spring Boot, pantalla de bitácora y cambios de IA quedan fuera de alcance.

Para probar: Administrador → Empresas → una empresa desactivada → Ver evaluaciones; alterna Registros y abre el historial de un puesto desde su salón. Comprueba que sigue inactivo, abre sus resultados e informe. En una evaluación con foto, pulsa Calcular manualmente y sigue las cinco instrucciones. Con Usuario, no deben aparecer Registros ni resultados En curso.

Base: `c50364f`. Commit de entrega: «Paso 23: historial de inactivos, Atrás fuera de Inicio y guía manual». Hash y push a main del repositorio verificado se entregan al cerrar. Esperar el OK de Andy.
