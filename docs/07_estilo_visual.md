# Estilo visual: "Obsidian & Ivory" (igual que el mock app)

El docente quiere que el software se vea como el prototipo. Las capturas están en `docs/referencia/mock_app/capturas/` y todo el CSS original en `docs/referencia/mock_app/app.src.html` (dentro de `<style>`). **Copia de ahí** las clases que necesites (`.top`, `.card`, `.btn`, `.btn.p`, `.input`, `.pill`, `.steps`, tablas, etc.) a `src/main/resources/static/css/aplomo.css`.

## Marca

- Nombre: **ALINEO.** (con punto). Lema: "Ergonomía con precisión".
- Logo: la "A" dibujada como un ángulo medido con plomada. Es SVG, está en la función `glyphA()` / `logo()` del mock.

## Tokens

```css
:root{
  --bg:#0B0B0C; --card:#151517; --card2:#1C1C1F; --line:#2B2B2F; --line2:#3A3A40;
  --ivory:#EDE6D8; --plat:#C9CCD2; --muted:#9E998F; --ink:#0B0B0C;
  --verde:#34C759; --amarillo:#FFCC00; --rojo:#FF453A;
  --display:'IBM Plex Sans','Inter',system-ui,-apple-system,'Segoe UI',sans-serif; /* títulos y cifras */
  --serif:'Instrument Serif',Georgia,'Times New Roman',serif; /* solo la palabra ALINEO. */
  --sans:'Inter',system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;
  color-scheme:dark;
}
```

- Títulos y cifras grandes: `IBM Plex Sans` (400 y 500), guardada en `static/fonts/` para que funcione sin internet (licencia OFL). Texto: `Inter` (300 a 600). `Instrument Serif` queda solo para la palabra ALINEO. del logo.
- Cambio de noviembre de 2026: la docente pidió una letra menos formal y más técnica, de ingeniería industrial.
- Fondo obsidiana, texto marfil, tarjetas con borde fino y radio de 14 px.
- Botón principal: píldora marfil con texto negro (`.btn.p`). Botones secundarios: píldora con borde.
- Títulos grandes en IBM Plex Sans 500, champán; etiquetas pequeñas en mayúsculas con espaciado amplio.

## Semáforo (RF-020)

- **Solo** el semáforo usa verde, amarillo y rojo. Ningún botón ni adorno usa esos colores, para no confundir el riesgo.
- Siempre con texto: `4 · Amarillo · Nivel 2`, nunca solo el color (`.pill.VERDE`, `.pill.AMARILLO`, `.pill.ROJO`).
- El informe impreso (RF-026) usa un CSS de impresión claro (`@media print`: fondo blanco, texto negro) y debe entenderse en blanco y negro, por eso el nivel siempre va escrito.

## Pantallas táctiles

- `<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">` en todas las páginas.
- Campos de texto de 16 px en pantallas táctiles (`@media (pointer:coarse)`), para que el iPad no haga zoom.
- El canvas de puntos usa `touch-action:none` y un radio de agarre de 44 px con el dedo (ver `montarLienzo` en el mock).
- Sin desplazamiento horizontal en celular ni tableta (RNF-002).

## Variante clara (opcional, para más adelante)

Una compañera propuso una versión clara, "Perla": fondo `#F3EFE8`, tarjetas `#FCFBF8`, texto `#1E1C1A`, botones negros, y semáforo más oscuro (`#248A3D`, `#C99400`, `#D70015`) para que se lea sobre claro. Todavía no está decidida: **no la apliques** salvo que Andy lo pida.

## Paso 27 · Sistema visual vigente (21/11/2026)

Este apartado sustituye las indicaciones visuales anteriores cuando se contradigan. Conserva Obsidian & Ivory,
IBM Plex Sans (títulos), Inter (texto) e Instrument Serif (solo la marca). Los cambios de propiedad, avisos y solicitudes se validan en el servidor; el aspecto
nunca concede permisos.

| Componente | Regla vigente |
| --- | --- |
| Fondo y superficies | Carbón `#0E0E10`; tarjetas opacas; filas alternas `#18181B`. |
| Títulos | IBM Plex Sans 500, champán `#E3CDA0`; texto normal marfil y secundario `#9E998F`. La marca sigue marfil. |
| Anchos | Paneles hasta 1180 px; formularios, lectura y Mi cuenta hasta 720 px. |
| Acción principal | Marfil, texto oscuro; una acción principal por sección. |
| Acción secundaria | Fondo `#242429`, borde de 1,5 px `#CFC8BA`, texto 600. Enlaces champán subrayados. |
| Tamaño de controles | Al menos 44 px; en tabletas los botones alcanzan 56 px y las opciones 84 px. |
| Riesgo en calma | Punto de 8 px: Verde `#5E9C77`, Amarillo `#C2A35C`, Rojo `#C06A62`; cifra serif marfil y nivel gris. |
| Resultado final | Único lugar con semáforo fuerte: anillo principal de 96 px, posturas con anillo de 58 px. |
| Activa / Inactiva | Activa azul `#8FB2FF`, fondo al 16 %; Inactiva gris `#A9AEB7`. |
| Reserva | Por confirmar lila; Aceptada azul; Finalizada marfil con marca; Cancelada gris tachada. |
| Visita / seguimiento | En curso y En proceso champán; Abierto lila; Resuelto marfil. Se conservan los estados del esquema. |
| Puesto | Por evaluar champán; Evaluado marfil. |
| Aviso nuevo | Champán sólido `#F2D9A2` y texto oscuro; distinto del riesgo y del color de personas. |
| Error | Coral `#FF9A8A`, fondo al 12 %, borde de campo; mensaje español junto al dato, con foco. |
| Éxito | Toast inferior centrado, 4 segundos, `role=status`; fondo `#1E2A22`, borde `#3F6B50`, texto `#CFEBD9`. |

### Personas, no riesgo

Andrés Evaluador usa siempre `#FF4F9A`; Johan, `#3D7BFF`; Laura Gabriella, `#16BFC9`.
Los demás disponen de `#9B6BFF`, `#C9CCD2` y `#FF8FD0`. La asignación se guarda en `cuenta.color_agenda`.
Los colores sólidos del calendario identifican al Evaluador; no tienen relación con la calificación RULA.

### Listas y páginas

- Filas completas enlazadas, un nombre fuerte en una línea, elipsis y nombre completo al pasar el puntero.
- Búsqueda inmediata y chips con contadores; no hay que pulsar Aplicar filtros en las listas rediseñadas.
- Reservas y Visitas agrupadas por fecha; Pendientes por empresa, riesgo calmado, prioridad y fecha.
- Empresas y Archivo muestran ciudad registrada y sector opcional; sin sector no se añade un valor supuesto.
- Usuarios tiene tres contadores de rol, filtro por empresa y estado Activas inicial; una fila abre su edición.
- Empresa: datos y equipo separados; sedes enlazadas completas; áreas y cargos compactos.
- Mi cuenta: ajustes personales; cada dato se edita en su propia página con Cancelar y Guardar.
- Ayuda: navegación de temas del rol y artículo; sin acordeones. No se inventan teléfonos o correos de contacto.
- Inicio se adapta al rol. Los indicadores enlazan a las listas de las que proceden. La práctica es discreta y solo del Evaluador.

### Fechas, calendario y evaluación

Las fechas visibles usan DD / MM / AAAA y las horas HH : MM. El campo oculto conserva ISO para el servidor.
Se admite salto al completar, Backspace, pegado y flechas; el calendario pequeño se cierra con Escape.
Reservas ofrece intervalos libres de 30 minutos entre 8:00 y 16:00, sin restringir las horas que se escriben manualmente.
El servidor vuelve a comprobar los cruces al guardar. Este intervalo del selector es una elección de interfaz,
no una regla nueva del método ni una limitación de la agenda.

Agenda usa seis semanas iguales en el mes, superficie `#2E2E34`, cabecera `#38383F`, líneas `#45454C` y borde
`#4A4A52`. Máximo tres citas por día y enlace al resto; filtros del equipo arriba. Los días pasados tienen marca,
hoy lleva círculo champán y el 27/11/2026 un borde de referencia para la presentación. En celular se elige el día
para ver sus citas.

La evaluación tiene modo enfoque, contexto del lugar y salida a la visita. Conserva las fotos, puntos, líneas,
mediciones y valores existentes. Sus fases son Permiso, Puesto, Postura 1, Postura 2 y Resultado. La revisión de
cada foto presenta Brazo, Antebrazo, Muñeca, Cuello, Tronco y Piernas, una parte a la vez, seguida del esfuerzo.
La barra inferior permanece fija; los avisos de falta aparecen solo al intentar continuar. El análisis adicional
conserva el recorrido de la foto existente, con Revisar postura y Resultado.

El resultado resume el máximo recibido del servidor, conserva el desglose plegado y muestra primero tres
sugerencias. Exportar PDF abre el informe general autorizado de la empresa y resalta la visita si ya está
Finalizada; una visita En curso se identifica sin incorporarla a los promedios históricos.

### Accesibilidad

Foco visible, enlace Saltar al contenido, nombres accesibles de campos y botones, errores anunciados y sin
perder respuestas. El nivel está escrito y el riesgo tiene nombre accesible; el color nunca es la única señal.
Las tarjetas seleccionadas mantienen texto oscuro legible. Las animaciones respetan movimiento reducido.
El informe usa tinta oscura sobre papel claro y conserva nivel y acción al imprimir en blanco y negro.
