# Estilo visual: "Obsidian & Ivory" (igual que el mock app)

El docente quiere que el software se vea como el prototipo. Las capturas están en `docs/referencia/mock_app/capturas/` y todo el CSS original en `docs/referencia/mock_app/app.src.html` (dentro de `<style>`). **Copia de ahí** las clases que necesites (`.top`, `.card`, `.btn`, `.btn.p`, `.input`, `.pill`, `.steps`, tablas, etc.) a `src/main/resources/static/css/aplomo.css`.

## Marca

- Nombre: **APLOMO.** (con punto). Lema: "Ergonomía con precisión".
- Logo: la "A" dibujada como un ángulo medido con plomada. Es SVG, está en la función `glyphA()` / `logo()` del mock.

## Tokens

```css
:root{
  --bg:#0B0B0C; --card:#151517; --card2:#1C1C1F; --line:#2B2B2F; --line2:#3A3A40;
  --ivory:#EDE6D8; --plat:#C9CCD2; --muted:#9E998F; --ink:#0B0B0C;
  --verde:#34C759; --amarillo:#FFCC00; --rojo:#FF453A;
  --serif:'Instrument Serif',Georgia,'Times New Roman',serif;
  --sans:'Inter',system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;
  color-scheme:dark;
}
```

- Fuentes de Google Fonts: `Instrument Serif` (títulos) e `Inter` (300 a 600, texto).
- Fondo obsidiana, texto marfil, tarjetas con borde fino y radio de 14 px.
- Botón principal: píldora marfil con texto negro (`.btn.p`). Botones secundarios: píldora con borde.
- Títulos grandes en serif; etiquetas pequeñas en mayúsculas con espaciado amplio.

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
