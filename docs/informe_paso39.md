# Paso 39 · Evaluación más clara y listas fáciles de leer

24 nov 2026 · America/Bogota

## Resultado

Se integraron Barlow (PR #7), imágenes completas (PR #8) y modelo local de detección (PR #9).
Router conserva los dos cambios en conflicto: tipos del modelo primero, woff2 después y caché del modelo.
Las tres pruebas previas obligatorias pasaron: 17 casos, sin fallos ni errores.

Permiso tiene dos tarjetas separadas también en celular, consentimiento cuadrado y nota sin fotos.
La revisión muestra ángulos enteros solo como presentación, partes hechas, sugerencia original,
fichas cuadradas sin recorte, ajustes cortos y controles secundarios dentro de Más.
No cambia los grados enviados ni el cálculo del servidor; Calcular manualmente sigue disponible.

Reservas usa una tarjeta por fecha con Hoy/Mañana. Visitas conserva En curso arriba y agrupa
las Finalizadas en meses plegables, cinco filas iniciales y expansión independiente.
Los indicadores incluyen las visitas En curso y respetan los filtros.
Empresas y Usuarios permiten Ver por con preferencia local protegida con try/catch; las asignaciones
múltiples aparecen en sus grupos sin duplicar los registros usados por contadores y búsqueda.
La próxima visita se obtiene con PreparedStatement y el alcance autorizado existente.

## Requisitos y pruebas

| Requisito | Archivo y método | Comprobación |
| --- | --- | --- |
| RF-016, RNF-007: permiso y consentimiento | FotosGuiadas.permiso/consentimiento; ConversacionEvaluacion; CSS | Paso39Test.permisoTieneDosTarjetasIndependientesYConservaValores; HttpCierreTest; navegador 390/1366 |
| RF-017: medida legible, sin alterar cálculo | revision-reglas.anguloTexto; conversacion-evaluacion.medir; revision-visual | qa_listas39.cjs; FichasAngulosTest; medición DOM de 56/40 px y cuadrados contain |
| RF-017: controles de puntos y salida manual | revision-visual; analisis.mostrarDeteccion | Navegador: muestra detectada y Más → Calcular manualmente inicia guía de puntos |
| RF-024: Reservas por día | TablasTrabajo.dias | Paso39Test.cadaDiaDeReservasTieneSuCajaYLaHoraNoTieneSegundos; navegador Hoy/Mañana |
| RF-024: Visitas por mes e indicadores | MesesVisitas.mostrar/conteos; sistema-visual.actualizar | Paso39Test.mesesSeAbrenDeCincoEnCincoYElMasRecienteEstaAbierto; HttpListas29Test; qa_listas39.cjs; expansión 5→7 y búsqueda |
| RF-024, RNF-007: Ver por | VerPor.mostrar; TablaEmpresas/TablaUsuarios.pagina; sistema-visual.agrupar | Paso39Test.verPorOfreceLosModosDeEmpresaYUsuarios; qa_listas39.cjs; búsqueda, cambio de grupo y recarga |
| RF-002: próxima visita propia | Empresas.resumen | ProximaEmpresa39Test: excluye pasadas/canceladas y empresa ajena |
| RNF-004: recursos estáticos limitados | Router.tipoModelo/estatico | HttpSeguridadTest; HttpTipografiaTest |
| RNF-010: Java hasta 120 columnas | Código Java nuevo | LongitudFuentesTest |

## Pruebas finales

- Una ejecución completa de `TZ=America/Bogota ./mvnw test`: **1182 casos; 1181 aprobados,
  1 fallo, 0 errores, 0 omitidos**, duración 3 min 24 s.
- Único fallo, excepción expresamente autorizada por Andy: `DemoPlan28Test.java:55`,
  `expected: <23> but was: <24>`. Depende de la fecha; no se cambió ni se repitió la suite.
- `node --test scripts/qa_*.cjs`: **178 casos, 178 aprobados, 0 fallos/omitidos**.
- Dirigidas finales: Paso39Test + ProximaEmpresa39Test + LongitudFuentesTest, 6/6.
- No hubo fallos del Paso 39 en la suite final. Dos errores iniciales del dato de prueba de
  ProximaEmpresa39Test (código demasiado largo y cancelación sin motivo) se corrigieron solo en la prueba.

## Checklist y límites

- ❌ Maven completamente verde: queda únicamente la excepción de fecha autorizada anterior.
- ✅ JavaScript completo en verde, sin omisiones.
- ✅ Reservas, Visitas, Empresas y Usuarios: 20 medidas, a 390/810/1080/1366/1920,
  con 0 px de desbordamiento horizontal exterior.
- ✅ Permiso: dos tarjetas a la misma altura; separación 16 px en 1366 y 12 px en 390.
- ✅ Ángulos: cinco fichas de Brazo en una fila en 1366, tres columnas en 390;
  imagen cuadrada, object-fit contain y padding 10 px. Medida 56 px / 40 px.
- ✅ Detección real con foto de muestra y guía manual accesible desde Más.
- ✅ Cero errores de consola capturados durante la revisión.
- ✅ Permisos existentes comprobados por suites HTTP; nueva consulta de empresa ajena cubierta.
- ✅ Textos nuevos con tildes y HTML escapado; git diff --check sin incidencias.
- ✅ Sin credenciales, config/db.properties ni fotos privadas añadidas a Git.
- ❌ Sin comparación visual por píxeles/capturas: Andy prohibió capturas; revisión mediante DOM y geometría.
- ❌ Sin prueba táctil en iPad físico ni Safari/Chrome/Edge/Firefox externos en este paso.

Lo que no se hizo: corregir el fallo de fecha ajeno al paso, cambiar esquema/datos reales/permisos,
motor RULA o POST /evaluaciones/calcular. No se añadieron dependencias ni se descargaron navegadores.
La QA transaccional usó solo rula_ergonomia_pruebas y confirmó ROLLBACK al cerrar.

## Cómo revisarlo

1. Refresca con Cmd+Shift+R e inicia sesión con tu cuenta habitual.
2. En Empresas cambia Ver por, busca y recarga: conserva el modo. Repite en Usuarios.
3. Abre Reservas: cada fecha tiene tarjeta propia. Abre Visitas y despliega un mes con más de cinco filas.
4. En una visita En curso entra a Evaluar: confirma permiso y usa una foto de ejemplo.
5. Revisa medida, sugerencia, fichas y avances; abre Más para comprobar el modo manual.

## Git

Merges locales de este paso:

```text
e9ab635 Detección sin internet (PR #9)
8b449c9 Fichas de ángulos completas (PR #8)
4788b23 Títulos con Barlow y Quiénes somos sin subtítulos (PR #7)
```

Commit final: `Paso 39 · evaluación más clara y listas por día, mes y Ver por`.
Destino verificado: https://github.com/Andybena2025/Aplomo-app.git, rama main.
El hash final y la confirmación del push y reinicio se entregan en el mensaje de cierre.
