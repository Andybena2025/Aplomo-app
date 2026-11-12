# Paso 13A · Correcciones del profesor Arle

12 nov 2026. Cambios solicitados por Andy en este chat. Este ajuste no cierra el Paso 13 general, que continúa pendiente desde su interrupción.

## Qué cambió

Se conserva Obsidian & Ivory, Instrument Serif e Inter. Fondo carbón, bordes de controles más visibles, escala de espacios, tipografía y tamaños consistentes. Cabecera con Atrás escrito también en celular y una sola navegación de regreso por página. Los pasos internos del asistente conservan su funcionamiento.

Inicio incorpora tres accesos grandes solo para Administración. Usuarios usa una tabla responsive y búsqueda por nombre/correo, rol y empresa sobre las cuentas que el servidor ya autorizó. No se agregaron rutas, consultas SQL, dependencias ni reglas de negocio. Se mantienen los POST originales, CSRF y validaciones.

El dibujo decorativo usa los mismos trazos SVG de la portada, con un encuadre adaptado. No participa en la lectura accesible, no intercepta clics y desaparece bajo 720 px. En Inicio se anima solo una vez por sesión; en otras páginas se presenta terminado. CSS desactiva las animaciones también en login si se prefiere movimiento reducido.

## Requisitos y comprobación

| Pedido / requisito relacionado | Archivo y punto de implementación | Evidencia |
|---|---|---|
| 1 · Atrás único y visible / RNF-002 | `Html.pagina`: texto sin hideSm y retirada de enlaces de regreso duplicados en el HTML presentado | Las cinco páginas muestran Atrás; detalle sin enlaces ← duplicados; `medidas.json` |
| 2, 4 · Tokens, controles y contraste / RNF-002 | `aplomo.css`: :root, .input, .field, filtros | Botones y campos ≥44 px; contraste de --line2 con los fondos en `contraste.json` |
| 3 · Dos tamaños y filas consistentes / RNF-002 | `aplomo.css`, `PaginasEmpresas.enlace/acciones`, controles de fotos y medición | Botones normales 44 px/15 px/padding 20; pequeños de Usuarios 36 px/14 px/padding 14; en celular mantienen 44 px de área táctil |
| 5 · Tipografía / RNF-002 | `aplomo.css`: body, .field label, .eyebrow, .small, .hint, th, .seg .deg | Capturas y revisión CSS; Instrument Serif se mantiene en tamaños ≥20 px |
| 6–7 · Espacios y ancho / RNF-002 | `aplomo.css`: .wrap, .usuarios-cabecera, .catalogo-seccion, .acciones-informe, .consultas-secciones | 16 px entre cabecera de Usuarios y tabla; 32 px antes de Sedes; Consultar con ancho del texto |
| 8 · Acciones de Inicio / RF-002, RNF-002 | `Router.accionInicio` y bloque HTML existente de Inicio | Tres accesos con icono, título y ayuda de una línea a 1440/1024/390; ausentes para Usuario |
| 9 · Usuarios / RF-003, RNF-002 | `PaginasUsuarios.lista`, `usuarios.js` | Tabla/cards; búsqueda por nombre y correo, filtros individuales/combinados, vacío y limpiar comprobados |
| 10 · Fondo / RNF-002 | `Html.fondo`, `plantillas/fondo.html`, `interfaz.js`, CSS .fondo | Primera pestaña: fondo animar; al volver desde Empresas: fondo; no intercepta la interfaz |
| 11 · Accesibilidad / RNF-002 | `Html.pagina`, login, `cuenta.js`, CSS .saltar/:focus-visible | Enter en Saltar al contenido enfoca main#contenido; aria-current correcto en las cinco páginas |
| Regresión y permisos / RNF-004, RNF-008 | Suite HTTP y servicios existentes sin cambios | 715 pruebas; Usuario recibe Acceso denegado escribiendo /usuarios a mano |

## Otros pares de botones normalizados

- Aplicar filtros / Limpiar filtros en las consultas y en Usuarios.
- Ver calificaciones / Generar informe en detalle de empresa.
- Editar / Desactivar o Reactivar en las fichas de configuración.
- Guardar / Cancelar en los formularios que usan el enlace compartido.
- Editar reserva / Aceptar o Iniciar visita / Cancelar reserva.
- Tomar foto / Subir foto en el asistente.
- Deshacer / Borrar puntos en la medición.
- Editar / Desactivar o Activar en la tabla de Usuarios: ambos pequeños, con el mismo tamaño.
- Atrás / Salir en la cabecera: ambos normales.

## Revisión responsive

| Ancho | Pantallas | Resultado |
|---|---|---|
| 1440 px | Inicio, Empresas, detalle de empresa, Reservas, Usuarios | Sin desbordamiento horizontal ni controles recortados. Navegación en una fila. Usuarios en tabla. |
| 1024 px | Las mismas cinco | Sin desbordamiento horizontal; navegación en una fila; ayudas de Inicio de una línea. Usuarios en tabla. |
| 390 px | Las mismas cinco | Sin desbordamiento horizontal; Atrás escrito; Usuarios y consultas con patrón de tarjetas. Dibujo oculto. |

Capturas: `docs/evidencia_paso13a/`. Navegador integrado Chromium, altura de vista 900 px. El ancho útil puede ser 15 px menor cuando hay barra vertical. Son comprobaciones de viewport, no pruebas en un teléfono físico.

## Checklist

- ✅ `TZ=America/Bogota ./mvnw test`: 715 pruebas, 0 fallos, 0 errores, 0 omitidas.
- ✅ Identidad del mock conservada con las modificaciones explícitas de este pedido; capturas de las cinco pantallas.
- ✅ 390/1024/1440 px sin deslizar de lado. 810/1080/1366 no se repitieron en este ajuste.
- ❌ Comprobación con dedo físico: no realizada; sí se conservan áreas táctiles de 44 px en celular.
- ✅ Usuario sin las tres acciones de Inicio y acceso directo /usuarios denegado.
- ✅ Textos en español con tildes; buscador acepta mayúsculas y normaliza acentos.
- ✅ Sin errores registrados en consola durante el recorrido revisado.
- ✅ Teclado: enlace Saltar al contenido; foco visible y pestaña activa accesible.
- ✅ Sin cambios de esquema, SQL, rutas ni cálculo RULA; nada borrado; datos QA sin confirmar, revertidos al cerrar.
- ✅ `config/db.properties` y `fotos/` quedan fuera del commit.
- ❌ Auditoría completa de conformidad WCAG 2.2 AA / lector de pantalla / dispositivo físico: fuera de esta verificación puntual; no se declara certificación.

## Lo que no se hizo

No se retomó el QA de versiones anteriores ni el resto del Paso 13 interrumpido. No se ejecutó el Paso 14. No se hizo push por instrucción expresa de Andy. No se cambiaron cuentas reales ni se sembraron datos de demostración permanentes.

## Cómo probarlo

1. Arranca el proyecto con tu configuración existente: `./mvnw compile exec:java`.
2. Como Administrador, abre Inicio: cuatro indicadores y tres acciones debajo. Sigue Nueva reserva, Iniciar visita y Nueva empresa para comprobar sus destinos.
3. Abre Empresas y Reservas: compara Aplicar/Limpiar filtros y revisa Ver otras secciones cerrado.
4. En una empresa, revisa Editar/Desactivar, Ver calificaciones/Generar informe, la separación antes de Sedes y los enlaces Consultar.
5. En Usuarios, busca por nombre o correo; combina rol/empresa; prueba un texto sin coincidencias y Limpiar filtros. Editar y los POST de activación mantienen sus rutas originales.
6. Con el ancho del navegador a 1440, 1024 y 390 px, revisa que no sea necesario deslizar lateralmente. En 390 Usuarios se presenta como tarjetas.
7. Pulsa Tab y Enter sobre Saltar al contenido. Abre Inicio en una pestaña nueva; visita otra sección y vuelve: el fondo ya no vuelve a animarse.
8. Como Usuario, verifica la ausencia de las acciones de Administración y el rechazo de /usuarios.

## Git

Commit solicitado: `Paso 13A: correcciones del profesor Arle`. Sin push. El hash se entrega en el resumen final; el estado de AGENTS.md refleja este ajuste y conserva el Paso 13 general pendiente.
