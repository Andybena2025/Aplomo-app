# Paso 13G · A — contador y pantallas completas

## Qué cambió
Nueve fases estables: consentimiento, postura, momento, tiempo, condiciones, fotos, lado, cuerpo y resumen. Foto/segmento mantienen etiquetas propias. La barra conserva su máximo alcanzado al volver atrás. Ayuda de lado plegable «Ver cómo decidir»; correcciones de espacio en segmentos, actividad, carga y resultado de dos fotos. Sin esconder contenido para simular ausencia de desplazamiento.

## Requisitos y evidencia
| Requisito | Archivo / método | Comprobación |
|---|---|---|
| RF-014/017, RNF-003: progreso estable | PaginasEvaluacion.fase/fases; PaginasAnalisis.formulario; evaluacion.js actualizarProgreso | FasesEvaluacionTest: 13 casos; 241 lecturas del navegador con máximo 9; prueba-retroceso.json mantiene valor 9 al volver a fase 8 |
| RNF-006/007: espacio y ayuda comprensibles | RecorridoCuerpo.lado; aplomo.css, bloque 13G-A | Matriz completa y capturas en evidencia_paso13g |
| RF-018/019/020, RNF-010: cálculo intacto | Paquete rula y endpoint calcular sin cambios | Suite completa y recorridos con dos fotos medidas y una de apoyo |

## QA real
Navegador Chromium integrado, controlado mediante Playwright del navegador disponible. Ventanas CSS exactas, sin descargas. Oficina y Planta, todas las 21 pantallas solicitadas; además segunda foto medida, ayuda abierta y retroceso. 241 medidas finales: desplazamiento vertical y lateral 0. Consola: 0 errores. Se usaron fotos S6 de la galería local. No se guardaron estas evaluaciones exploratorias.

| Pantalla | 1366×950 | 1024×1290 | 1180×740 | 820×1100 |
|---|---:|---:|---:|---:|
| Consentimiento | 0 | 0 | 0 | 0 |
| Postura | 0 | 0 | 0 | 0 |
| Momento | 0 | 0 | 0 | 0 |
| Tiempo | 0 | 0 | 0 | 0 |
| Condiciones | 0 | 0 | 0 | 0 |
| Permiso de fotos | 0 | 0 | 0 | 0 |
| Perfil | 0 | 0 | 0 | 0 |
| Frente | 0 | 0 | 0 | 0 |
| Detalle | 0 | 0 | 0 | 0 |
| Resumen de fotos | 0 | 0 | 0 | 0 |
| Lado | 0 | 0 | 0 | 0 |
| Brazo | 0 | 0 | 0 | 0 |
| Antebrazo | 0 | 0 | 0 | 0 |
| Muñeca | 0 | 0 | 0 | 0 |
| Giro | 0 | 0 | 0 | 0 |
| Cuello | 0 | 0 | 0 | 0 |
| Tronco | 0 | 0 | 0 | 0 |
| Piernas | 0 | 0 | 0 | 0 |
| Actividad | 0 | 0 | 0 | 0 |
| Carga | 0 | 0 | 0 | 0 |
| Resultado | 0 | 0 | 0 | 0 |

Los segmentos de la segunda foto también dieron cero. Ayuda abierta en 1180×740: cero. Detalle en `evidencia_paso13g/medidas-finales.json`.

## Checklist
- ✅ Suite completa: 824 pruebas aprobadas, 0 fallos, 0 errores, 0 omitidas.
- ✅ Pantallas comparadas con el recorrido vigente y capturas de lado y resultado; sin nueva marca.
- ✅ Sin desplazamiento lateral en las cuatro ventanas del encargo; no se repitió aquí el QA móvil de 390/810/1080.
- ❌ Prueba con dedo/dispositivo físico y otros navegadores: pendientes, sin descargas.
- ✅ Permisos conservados y cubiertos por pruebas HTTP; este bloque no agrega acciones.
- ✅ Textos españoles; sin errores de consola.
- ✅ Sin config/db.properties, fotos/ ni datos privados en Git.

## Qué no se hizo
No se cambió esquema, motor, endpoint de cálculo, valores guardados ni colores. La demo y Evaluar ahora pertenecen al commit B; ningún push hasta terminar ambos. Los datos del servidor de ensayo se revierten al cerrar y la suite usa exclusivamente rula_ergonomia_pruebas.

## Cómo probar
1. Entrar como Administrador y abrir una evaluación desde una visita En curso.
2. Recorrer consentimiento, contexto y fotos: el total siempre es nueve.
3. Medir perfil y frente: permanece fase 8 durante segmentos y esfuerzo; Resultado es fase 9.
4. Volver atrás: la barra no retrocede. Abrir Ver cómo decidir y comprobar ambos modos en las cuatro ventanas.

## Git
Commit A: `Paso 13G-A: contador de pasos estable e iPad sin desplazamiento en todas las pantallas`. Push reservado al cierre de B.
