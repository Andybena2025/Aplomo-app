# Método RULA: reglas exactas del cálculo

Fuente: Ergonautas (Universidad Politécnica de Valencia), https://www.ergonautas.upv.es/metodos/rula/rula-ayuda.php, y la matriz v6 (RF-017 a RF-021, RF-027, RNF-010).

Las tablas A, B y C y los niveles del semáforo **ya están cargados** en `docs/base_de_datos/Script_MySQL_RULA.sql` (tablas `valor_tabla_rula` y `nivel_riesgo`). Ya se comprobó que coinciden con las del mock app y con Ergonautas. El código **las lee de la base**, no las copia.

## 1. Ángulos a partir de 3 puntos (RF-017)

El Administrador marca puntos sobre la foto ampliada (de perfil, con el movimiento paralelo a la cámara). Cada ángulo usa 3 puntos:

| Segmento | Puntos | Cómo se mide |
|---|---|---|
| Brazo | Cadera · Hombro · Codo | Ángulo entre la línea hombro→cadera (tronco hacia abajo) y hombro→codo. Flexión si el codo va hacia adelante; extensión si va hacia atrás. |
| Antebrazo | Hombro · Codo · Muñeca | 180° menos el ángulo en el codo (0° = brazo estirado). |
| Muñeca | Codo · Muñeca · Nudillos | 180° menos el ángulo en la muñeca (0° = recta). |
| Cuello | Cadera · Hombro · Coronilla | Ángulo entre la línea del tronco (cadera→hombro) y hombro→coronilla. Hacia adelante = flexión; hacia atrás = extensión. |
| Tronco | Cadera · Hombro · vertical | Ángulo entre cadera→hombro y la vertical. |

- Ángulo entre vectores u y v: `acos( (u·v) / (|u|·|v|) )`, en grados.
- El sentido (flexión o extensión) sale del signo del producto cruz y de hacia dónde mira el trabajador (lado Derecho o Izquierdo).
- El ángulo se puede **corregir moviendo los puntos o escribiéndolo a mano**. Si falta un dato o tiene un valor imposible, no se calcula.
- Lo que la foto no muestra se marca con casillas: hombro elevado, cabeza o cuello girado, giro de muñeca, apoyo del brazo y piernas.
- Referencia funcionando: `medirAngulos()` en `docs/referencia/mock_app/app.src.html`. Se usa la tolerancia de 5° del mock (`const NEUTRO=5`): muñeca ≤5° = 1 punto; tronco ≤5° = 1 punto; cuello y tronco con 5° o menos hacia atrás cuentan como flexión, no extensión. Se conservan decimales al medir.
- RULA se aplica al lado derecho y al izquierdo por separado; el campo `lado` del análisis guarda cuál.

## 2. Puntos por segmento (RF-018)

**Grupo A**

| Segmento | Puntos | Ajustes |
|---|---|---|
| Brazo | Extensión ≤ 20° o flexión ≤ 20° = 1 · extensión > 20° o flexión 21–45° = 2 · flexión 46–90° = 3 · flexión > 90° = 4 | +1 hombro elevado · +1 brazo separado del cuerpo · −1 brazo apoyado |
| Antebrazo | 60–100° = 1 · menos de 60° o más de 100° = 2 | +1 cruza la línea media del cuerpo o trabaja hacia afuera |
| Muñeca | 0° (neutra) = 1 · hasta 15° de flexión o extensión = 2 · más de 15° = 3 | +1 muñeca desviada (radial o cubital) |
| Giro de muñeca | Medio = 1 · extremo = 2 | Puntaje propio, entra a la Tabla A |

**Grupo B**

| Segmento | Puntos | Ajustes |
|---|---|---|
| Cuello | 0–10° = 1 · 11–20° = 2 · más de 20° = 3 · extensión = 4 | +1 girado · +1 inclinado lateralmente |
| Tronco | 0° (o sentado bien apoyado) = 1 · 1–20° = 2 · 21–60° = 3 · más de 60° = 4 | +1 girado · +1 inclinado lateralmente |
| Piernas | Bien apoyadas y equilibradas = 1 · no = 2 | — |

## 3. Tablas

- **Tabla A:** fila = `(brazo − 1) × 3 + antebrazo` (1 a 18); columna = `(muñeca − 1) × 2 + giro` (1 a 8).
- **Tabla B:** fila = cuello (1 a 6); columna = `(tronco − 1) × 2 + piernas` (1 a 12).
- **Tabla C:** fila = puntaje A ajustado (1 a 8; 8 = 8 o más); columna = puntaje B ajustado (1 a 7; 7 = 7 o más). Resultado: **puntaje final de 1 a 7**.

## 4. Actividad muscular y carga (RF-019)

Se suman al puntaje del Grupo A **y** al del Grupo B antes de la Tabla C (un solo valor por análisis, como en la tabla `analisis_rula`):

- **Actividad:** +1 si la postura es estática (más de 1 minuto) o repetitiva (más de 4 veces por minuto).
- **Carga o fuerza** (Ergonautas, Tabla 16; enum `carga` de `analisis_rula`):
  - menos de 2 kg intermitente = 0
  - de 2 a 10 kg intermitente = +1
  - de 2 a 10 kg estática o repetitiva = +2
  - más de 10 kg intermitente = +2
  - más de 10 kg estática o repetitiva = +3
  - golpes o fuerzas bruscas = +3

## 5. Semáforo y nivel oficial (RF-020)

| Puntaje final | Color | Acción (tabla `nivel_riesgo`) | Nivel oficial RULA | Qué pasa en el sistema |
|---|---|---|---|---|
| 1–2 | 🟢 Verde | No necesita cambios | 1 | Puesto Aceptable |
| 3–4 | 🟡 Amarillo | Hacer una revisión | 2 | En observación; se recomienda visita de Seguimiento; al menos una sugerencia |
| 5–6 | 🔴 Rojo | Intervención inmediata | 3 | Entra a pendientes de arreglo (RF-022) |
| 7 | 🔴 Rojo | Intervención inmediata | 4 | Entra a pendientes de arreglo (RF-022) |

El nivel se muestra **con nombre y color**, nunca solo con color.

## 6. Calificaciones (RF-013b, RF-021, RF-025) · propuesta

- Puesto = su puntaje más alto en su última evaluación.
- Visita = promedio de los puestos evaluados.
- Sede = promedio de sus visitas finalizadas.
- Empresa = promedio de las sedes (solo si tiene 2 o más).
- Para el color, el promedio se redondea al entero más cercano. Es un indicador del equipo, no parte de RULA.

## 7. Pruebas obligatorias (RNF-010)

- Pruebas unitarias JUnit que recorren **todas** las combinaciones de las Tablas A, B y C contra los valores de la base.
- **10 casos calculados a mano** comparados con el sistema. Tres casos ya verificados (puntos de segmento → tablas → final), sin actividad y con carga menor de 2 kg:
  - Brazo 1° (1), antebrazo 85° (1), muñeca 9° (2), giro medio (1), cuello 10° (1), tronco 2° (2), piernas apoyadas (1) → A = 2, B = 2 → **2 · Verde · Nivel 1**.
  - Brazo 61° (3), antebrazo 47° (2), muñeca 0° (1), giro medio (1), cuello 37° (3), tronco 17° (2), piernas apoyadas (1), sin ajustes → A = 3, B = 3 → **3 · Amarillo · Nivel 2**.
  - El mismo caso con hombro elevado (+1) y brazo separado (+1) → brazo = 5 → A = 5, B = 3 → **4 · Amarillo · Nivel 2** (es el resultado que muestra el mock app en la evaluación de ejemplo).

Los diez casos completos, su desglose manual y las instrucciones para ejecutarlos están en [pruebas_fase_2.md](pruebas_fase_2.md).
