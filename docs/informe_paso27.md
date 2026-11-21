# Paso 27 · ALINEO

Estado: 27A implementado y verificado. Los bloques 27B (bandeja y solicitudes de visita) y 27C (solicitudes de acceso)
se entregan en commits separados. No se presentan como terminados en este informe de A.

## 27A · Cambios y comprobaciones

- Sistema compartido: títulos champán, estados diferenciados, riesgo en calma, filas completas,
  filtros en vivo, fechas colombianas, alertas junto al campo y confirmaciones de cuatro segundos.
- Inicio distinto por rol; Administrador sin reservas, inicio de visitas ni práctica. Evaluador con
  continuación por puesto, progreso y práctica aislada. Contadores enlazados a sus consultas.
- Empresa creada por Evaluador: él es el dueño; otros asignados y el Administrador solo consultan.
  Solo el Administrador transfiere un dueño Inactivo a un Evaluador Activo, con bitácora.
  Sector opcional, ciudad registrada, edición por el dueño y migración repetible.
- Evaluación con cabecera de enfoque, cinco fases, barra fija, una parte corporal visible y resultado
  compacto. Se reutilizan las fotos, puntos, imágenes, propuestas y puntajes del servidor.
- Archivo, empresas, sedes, usuarios, agenda mensual de seis semanas, cuenta por campo y ayuda por rol.
  PDF general conserva sus promedios; valida y señala la visita actual sin incluir En curso en ellos.
- Demo con NIT, sectores, lugares y nombres presentables; conserva claves, estados e intentos del equipo.
  La carga repetida no duplica. Un NIT coincidente sin la marca de demo cancela antes de modificar nada.

Decisiones de propiedad y sector: respuestas expresas de Andy del 21/11/2026. No se dedujo el sector
ni se cambió el dueño desde un campo editable del navegador.

## Requisitos y prueba

| Requisito | Archivo / punto de cumplimiento | Comprobación |
| --- | --- | --- |
| RF-001c, RF-003 | `CuentaWeb`, `CuentaPropia.guardarCampo`, `PaginasUsuarios` | `CuentaCampoTest`, `HttpFunciones27Test`, `HttpPaso26Test` |
| RF-002 | `PropiedadEmpresa.exigir`, `Empresas.transferir`, servicios de reservas/visitas/práctica | `PropiedadEmpresaTest`, `HttpPropiedadEmpresaTest`, `HttpFunciones27Test` |
| RF-004–010, RF-028 | `Empresas`, `EmpresaDao`, `PaginasEmpresas`; migración de dueño/sector | Propiedad, `EmpresasTest`, `HttpEmpresasTest`, historial de inactivos |
| RF-011–013, RF-033 | `InicioWeb`, `Reservas.horasLibres`, `Calendario`, `tiempo-colombia.js` | `ReservasTest`, `HttpPaso26Test`, `qa_tiempo_colombia.cjs`; recorrido real de fecha/hora |
| RF-014–021 | `ConversacionEvaluacion`, `conversacion-evaluacion.js`, `revision-visual.js` | Pruebas HTTP de recorrido, revisión, fotos y análisis; dos posturas completas en navegador |
| RF-022–024 | `ListasConsulta`, `FiltrosLista`, `SistemaVisual`, Archivo | `HttpConsultasTest`, `HttpHistorialInactivosTest`, `HttpPendientesTest`, `HttpSugerenciasTest` |
| RF-025–027 | `CalificacionesWeb`, `PaginasCalificaciones`, `CentroAyuda` | `CalificacionesTest`, `HttpInformesTest`, `HttpAyudaTest`; contexto de informe |
| RNF-002, RNF-004, RNF-007 | CSS compartido, cabecera, filtros, alertas y ayuda | Auditoría GET por tres roles, mediciones DOM y formularios con errores conservados |
| RNF-003 | listas, guardado e informe con 1000 registros | 20 repeticiones por acción: P95 561,32 / 16,42 / 194,54 ms; límite 5000 ms |
| RNF-006, RNF-009 | alcance SQL, fotos privadas e historial | Pruebas de direcciones ajenas, Usuario sin fotos ni resultados En curso, desactivación sin borrar |
| RNF-010, RNF-011 | motor/bloque protegidos, formato y métodos de servicio | Comparación binaria de protegidos y `LongitudFuentesTest` |

## Números de QA

Suite A: 1057 casos Maven. Primera ejecución completa: 959 aprobados y 98 incidencias; se corrigieron
los fallos reales y se adaptaron expectativas de HTML/permisos retirados. Los 98 casos se comprobaron
con reintentos dirigidos, sin repetir la suite completa. JavaScript: suite inicial de 128 aprobadas y seis casos nuevos de filtros verificados después: 134 en total.
Cierre dirigido: 27 casos de páginas, propiedad, demo y formato; 26 funcionales aprobados y formato corregido/reintentado. Otros ocho casos de Inicio y permisos aprobados tras su ajuste de textos.

| Pantallas / rol | 360 | 390 | 820 | 1180 | 1366 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Login | 0 | 0 | 0 | 0 | 0 |
| Inicio, reservas, visitas, pendientes, archivo, empresas, cuenta, ayuda · E/U | 0 | 0 | 0 | 0 | 0 |
| Inicio y listas administrativas · A | 0 | 0 | 0 | 0 | 0 |
| Evaluación · Postura y Resultado: desborde horizontal | 0 | 0 | 0 | 0 | 0 |

Cifras en píxeles de desborde horizontal. Se hicieron 205 mediciones de páginas; el error inicial de
Inicio a 820 px (2 px) se corrigió y volvió a medir. La lista Usuarios requirió corregir un color nulo;
su medición final se repitió sobre la respuesta correcta. Otras 52 medidas finales, incluyendo 1024 px, dieron 0 px; filtros abiertos a 390 px y selección real de riesgo comprobados. No se tomaron capturas.

Resultado con tres sugerencias visibles: desplazamiento vertical **0 px** en 1366×768, 1440×900,
820×1180 y 1180×820. En celular conserva desplazamiento vertical normal; no se promete que quepa
sin bajar. La barra permanece fija. Dos posturas guardadas en el ensayo: puntajes oficiales 7 y 6,
con máximo 7; datos de QA revertidos por ROLLBACK. MediaPipe real funcionó en los dos ejemplos;
no se cambió su código. Calendario: 42 días / seis semanas y 0 px de desplazamiento vertical en las
ventanas de tableta y computador comprobadas. Cero errores de consola capturados en el recorrido.

## Auditoría de rutas

Se revisan 59 familias de páginas GET, sus formularios y variantes por rol. Además de las rutas
nombradas en el encargo: `/sedes`, `/salones`, `/areas`, `/cargos`, `/puestos`, sus detalles y altas/ediciones;
`/evaluaciones`, `/sugerencias`, sus detalles/ediciones y creación manual; `/visitas/{id}/finalizar`;
`/evaluaciones/{id}/analizar`, `/evaluaciones/{id}/analisis/{id}`; `/cuenta/contrasena`, `/cuenta/clave`,
`/cuenta/nombre`, `/cuenta/telefono`, `/cuenta/cargo`; práctica, ayuda pública y errores 403/404/500.
Recursos binarios, JSON de notas y fragmentos de catálogos no se cuentan como páginas rediseñadas.
`Inicio` es la ruta existente `/`, no se creó otra dirección `/inicio`.

## Lo que no se hizo

27B y 27C siguen pendientes de sus propios commits. No se cambió el motor, el bloque de cálculo,
la IA ni ninguna imagen. Sin ficha PDF individual, hosting, Spring Boot, logo nuevo, Notion o diagramas.
No se probó iPad físico, Windows ni tres equipos en la misma red; no se descargaron navegadores.
La cuenta inicial se conserva: la pregunta sobre desactivarla no está resuelta y su clave no se cambia.

## Checklist

- ✅ 1057 casos Maven verificados y 134 JS aprobados; suite completa una sola vez, reintentos dirigidos.
- ✅ Nuevo sistema visual y mediciones DOM; sin capturas por instrucción de Andy.
- ✅ Direcciones manuales y límites de empresa/rol comprobados en el servidor.
- ✅ Consentimiento, puntos, ajustes y esfuerzo se confirman en el recorrido; sin nuevos puntajes en JavaScript.
- ✅ Español, escape HTML, CSRF y conservación de campos al fallar.
- ✅ Archivos privados y fotos fuera del conjunto de archivos a versionar.
- ❌ Comprobación física con dedo / iPad, Windows y tres equipos; pendiente.

## Cómo probar A

1. Inicia sesión con cada rol: sus acciones de Inicio deben ser diferentes.
2. Como Evaluador, crea una empresa, su sector y un puesto; la empresa queda asignada a tu cuenta.
3. Intenta editar esa empresa como Administrador u otro Evaluador asignado: debe denegarlo.
4. Solo si el dueño está Inactivo, el Administrador ve la transferencia a otro Evaluador activo.
5. En Reservas prueba la fecha DD/MM/AAAA, el salto y una hora libre; los campos conservan sus datos si hay cruce.
6. Recorre dos posturas desde una visita En curso; revisa el Resultado, abre su desglose y Exportar PDF, y guarda.
7. Abre Archivo, Mi cuenta y Ayuda. Usuario solo consulta sus empresas y resultados Finalizados, sin fotos.

Commit y push: se anotan al cerrar cada bloque; no confundir esta copia de trabajo con la app que corre en 8080.


## Base y demo aplicadas

Respaldo privado creado antes de migrar. Migración ejecutada dos veces en la base actual: 22 tablas,
conteos idénticos antes/después; agrega dueño y sector sin borrar registros. La carga de demo también
se ejecutó dos veces dentro de una transacción: primera y segunda con los mismos conteos. Se conservó
el hash de cada contraseña existente del equipo y de la cuenta inicial.

Empresas 8→8; cuentas 13→13; sedes 14→14; salones 31→31; puestos 47→47; reservas 72→74 (dos respaldos
del viernes); visitas 45→45; evaluaciones 66→66; fotos 66→66; análisis 66→66. Ningún dato de ensayo
«Empresa de prueba sin confirmar» en la base real: 0. No se confirmó ningún ensayo de QA.

| Evaluador | Citas pasadas | Citas desde 21/11/2026 | Canceladas |
| --- | ---: | ---: | ---: |
| Ing. Andrés Benavides | 22 | 9 | 0 |
| Ing. Johan Zapata | 12 | 8 | 1 |
| Ing. Laura Gabriella Romero | 8 | 9 | 0 |

Nueve cuentas del equipo activas más la inicial conservada: 10. Las tres cuentas antiguas
`companero1–3` quedan Inactivas; solo `companero2` cambió de Activa a Inactiva. Las otras cuatro genéricas
no existían y no se crean. Avisos de ejemplo nuevos: pendientes de 27B. No se envió correo ni WhatsApp.
