# Paso 31B · Filtros de Agenda en una fila

21 de noviembre de 2026. Base `6c939fc` (Paso 30C), rama main sincronizada.

Se compactaron los filtros administrativos sin cambiar su lógica ni sus destinatarios: nombre corto sin Ing., nombre completo en title, punto de 8 px, selección por borde/fondo y checkbox conservado pero oculto visualmente. El sobre queda dentro del chip, fuera del label del filtro, con nombre accesible y título «Escribir a …». Abre el panel general existente del Paso 30C sin cambiar la selección. La fila no se parte: si falta ancho, desplaza su propio contenido y oculta la barra.

Inicio usa una leyenda de personas diferente, ya con nombres cortos y puntos; no utiliza este componente y no se modificó. No se cambiaron JavaScript, rutas, servicios, permisos, cálculo ni datos.

| Requisito | Archivo / método | Comprobación |
| --- | --- | --- |
| RF-033, RNF-002 · filtros y sobre | `Calendario.filtros`, `evaluador`; `aplomo.css` | Dos casos nuevos `FiltrosAgenda31BTest`; medidas DOM y apertura del panel |
| RNF-004 · texto/atributos escapados | `Calendario.evaluador` | `nombreConHtmlSeEscapaEnTextoYEnLosAtributosDelSobre` |
| RNF-008 · una fila y teclado | CSS de filtros, checkbox y foco | 1920×1080, 1180×820 y 390×844; Space oculta la cita, Todos la restaura |

## Resultados

- Maven **1129 → 1131** casos; JavaScript **161 → 161**. Todos aprobados; suites completas una vez al cierre, pruebas dirigidas durante desarrollo. Sin fallos, errores ni omitidas.
- A 1920×1080: tres chips, offsetTop **174/174/174**, alto **32/32/32 px**, puntos **8 px**, sobres **32 px**, inputs ocultos **1 px**.
- A 1180×820: offsetTop **180/180/180**, mismo alto **32 px**. La fila con Todos y los tres nombres ocupa **621 px** y no necesita desplazarse.
- A 390×844: offsetTop **224/224/224**, misma fila; ancho útil **343 px** y contenido **621 px**, con desplazamiento interno y scrollbar-width none. Desbordamiento exterior **0 px** en los tres tamaños.
- El sobre de Laura abrió «Mensaje para Laura»; al cerrar, los tres filtros seguían seleccionados. El checkbox de Andrés se operó con Space: desmarcado, cita oculta, fondo transparente, borde gris y foco solid. Todos volvió a marcar los tres y mostrar la cita.
- **0 errores de consola capturados**. QA con cuentas temporales solo en `rula_ergonomia_pruebas`, sin cargar demo; ROLLBACK confirmado al cerrar. Sin capturas.

## Lo que no se hizo

No se probaron iPad/iPhone físicos, tacto ni otros navegadores: la revisión se hizo por estructura y medidas del navegador integrado. No se repitió toda la matriz de dispositivos del proyecto; se comprobaron los dos tamaños pedidos y celular como caso de desbordamiento. Sin cambios de motor RULA, endpoint de cálculo, IA, esquema, demo, credenciales ni bitácora.

## Checklist

- ✅ Compilación, Maven 1131 y JS 161; 120 columnas.
- ✅ Chips de 32, nombres, sobre y selección comprobados sin capturas.
- ✅ 1920/1180/390 sin desplazamiento de página; ❌ 810/1080 exactos no repetidos.
- ✅ Teclado y foco visible; ❌ tacto físico no probado.
- ✅ El componente solo se presenta al Administrador; Usuario sigue sin verlo. Permisos de mensajería conservados y comprobados en `HttpPaso30CTest`.
- ✅ Texto escapado, tildes y cero errores de consola capturados.
- ✅ Sin configuración privada, fotos ni contraseñas en el commit; demo intacta.

Para Andy: refresca Agenda; los tres nombres deben quedar junto a Todos. Pulsa el sobre para escribir sin desmarcar. Pulsa el nombre o usa Tab/Espacio para filtrar; Todos restaura las citas. En celular la fila se desplaza dentro de su ancho.

Entrega: un commit «Paso 31B» y push a origin/main del repositorio verificado Andybena2025/Aplomo-app. App normal reiniciada en 8080, sin reiniciar demo; esperar revisión de Andy.
