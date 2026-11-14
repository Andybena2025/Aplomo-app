# Evidencia del Paso 14 final

Capturas reales del navegador integrado, con datos ficticios solo en `rula_ergonomia_pruebas`. El servidor de QA se cerró con ROLLBACK. No son capturas de una evaluación clínica ni de un iPad físico.

- `contexto-*`, `lado-*`, `revision-*`, `esfuerzo-*`, `resultado-*`: pantallas del recorrido.
- `segundo-lado-sin-heredar.jpg`: el segundo lado empieza sin mediciones ni respuestas del primero.
- `medidas-finales.json`: 80 registros, 10 estados × 2 modos × 4 ventanas; valores CSS reales.
- `conteo-antes.json` y `conteo-despues.json`: recorridos completos medidos desde Evaluar ahora hasta guardar. El nombre `toques` del registro significa acciones de interfaz realizadas con click/check, no eventos táctiles reales.
- `comprobaciones-adicionales.json`: segundo lado, respaldo manual, fotos de apoyo y Añadir análisis.

No se ejecutó hasTouch/tap ni emulación de reduced-motion: no hay navegadores Playwright instalados y Andy indicó no descargar. No se certifica menos de diez minutos sin un ensayo con evaluadores nuevos.

Cada celda es **desplazamiento vertical de página / lateral / interno de lista**, en píxeles; máximo de Oficina y Planta. El desplazamiento interno está permitido por el encargo, con confirmación fija abajo.

| Pantalla/estado | 1366×950 | 1024×1290 | 1180×740 | 820×1100 |
|---|---:|---:|---:|---:|
| Consentimiento | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Contexto | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Condiciones | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Lado | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Revisión sin foto | 0 / 0 / 92 | 0 / 0 / 0 | 0 / 0 / 302 | 0 / 0 / 0 |
| Revisión | 0 / 0 / 77 | 0 / 0 / 0 | 0 / 0 / 287 | 0 / 0 / 0 |
| Revisión con apoyo | 0 / 0 / 77 | 0 / 0 / 0 | 0 / 0 / 287 | 0 / 0 / 0 |
| Revisión abierta | 0 / 0 / 1034 | 0 / 0 / 1015 | 0 / 0 / 1260 | 0 / 0 / 1311 |
| Esfuerzo | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
| Resultado | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |
