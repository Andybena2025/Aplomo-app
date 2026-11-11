# Fase 2: motor RULA (paso 1 del plan)

Se completa únicamente el paso 1 de `06_plan_de_implementacion.md`. La fase 1 fue el proyecto base (paso 0). Aún no hay pantallas de evaluación ni ingreso funcional.

## Qué se construyó

- `Config`: lee y valida `config/db.properties`, sin imprimir sus valores.
- `Conexion`: abre una conexión JDBC nueva; el llamador la cierra con `try-with-resources`.
- `TablasRula`: conserva la lectura iniciada por Claude y carga una vez las 144 celdas A, 72 B, 56 C y los niveles desde MySQL. Solo usa `PreparedStatement`. Rechaza datos incompletos, repetidos, fuera de rango o niveles sin cobertura de 1–7. Una carga fallida no publica datos parciales y permite reintentar.
- `MedidorAngulos`: recibe puntos en píxeles de la misma foto, con x hacia la derecha e y hacia abajo. Mide brazo, antebrazo, muñeca, cuello y tronco; distingue flexión y extensión en brazo, cuello y tronco según el lado. En perfil DERECHO la persona mira a la derecha, e IZQUIERDO a la izquierda. La muñeca se mide por magnitud, porque ambas direcciones puntúan igual. Rechaza puntos ausentes, coincidentes y coordenadas no finitas.
- `PuntuadorSegmentos`: convierte grados y casillas a puntos. Los ángulos manuales pasan por la misma validación. Extensión se representa con grados negativos; antebrazo usa 0–180°. El apoyo del brazo nunca deja un puntaje menor de 1. Incluye el caso sentado bien apoyado de la sección 2 del método, sin modificar el esquema de la base.
- `MotorRula`: consulta A y B, suma una actividad y una carga comunes, consulta C y devuelve el desglose y el nivel con ID, color, nombre, acción y nivel oficial. Conserva A/B ajustados completos (por ejemplo 13 y 13); los límites 8 y 7 se aplican solo al buscar en C.

Cubre RF-018, RF-019, RF-020 y RF-027 en el módulo de cálculo; RNF-001 en la conexión, RNF-010 con pruebas y RNF-011 en la separación y los comentarios del código. RF-017 queda preparado en Java; marcar, arrastrar o escribir ángulos en pantalla pertenece al paso 8. El motor no guarda análisis (RF-021), no crea pendientes y no cambia datos.

## Decisión sobre posición neutra

Andy eligió los rangos exactos del documento 03: solo **0°** es neutro para muñeca y tronco, salvo el caso explícito de tronco sentado bien apoyado. Por eso el primer caso tiene tronco = 2 y B = 2. No se aplica la tolerancia de 5° del mock. Al medir se normaliza el ruido aritmético a diez decimales para que un ángulo geométrico de 20° no se convierta en 20.00000000000001° y cambie de puntaje. Los ángulos manuales conservan su valor.

## Diez casos calculados a mano

Los casos 1–3 son los del documento 03. Los otros siete completan las pruebas obligatorias y cubren las seis cargas, actividad y saturación de C.

- **Neutro:** ángulos brazo 0°, antebrazo 90°, muñeca 0°, cuello 0°, tronco 0°; sin ajustes, giro medio y piernas apoyadas. Puntos (brazo, antebrazo, muñeca, giro; cuello, tronco, piernas) = **(1,1,1,1;1,1,1)**.
- **Ejemplo:** 61°, 47°, 0°, 37°, 17°; sin ajustes, giro medio y piernas apoyadas. Puntos = **(3,2,1,1;3,2,1)**.
- **Máximo:** 120°, 20°, 30°, cuello en extensión −20°, tronco 70°; hombro elevado, brazo separado, antebrazo cruzado, muñeca desviada, giro extremo, cuello y tronco girados e inclinados, piernas sin apoyo. Sin apoyo del brazo ni tronco sentado bien apoyado. Puntos = **(6,3,4,2;6,6,2)**.

| Caso | Entrada | Tabla A | Tabla B | Actividad | Carga | A ajustado | B ajustado | Final | Color / nivel |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 1°,85°,9°,10°,2°; sin ajustes; giro medio; piernas apoyadas | 2 | 2 | 0 | 0 | 2 | 2 | 2 | Verde / 1 |
| 2 | Ejemplo | 3 | 3 | 0 | 0 | 3 | 3 | 3 | Amarillo / 2 |
| 3 | Ejemplo + hombro elevado y brazo separado: brazo = 5 | 5 | 3 | 0 | 0 | 5 | 3 | 4 | Amarillo / 2 |
| 4 | Neutro | 1 | 1 | 0 | 0 | 1 | 1 | 1 | Verde / 1 |
| 5 | Neutro + actividad estática o repetitiva | 1 | 1 | 1 | 0 | 2 | 2 | 2 | Verde / 1 |
| 6 | Neutro + 2–10 kg intermitente | 1 | 1 | 0 | 1 | 2 | 2 | 2 | Verde / 1 |
| 7 | Ejemplo + 2–10 kg estática o repetitiva | 3 | 3 | 0 | 2 | 5 | 5 | 6 | Rojo / 3 |
| 8 | Ejemplo + actividad + más de 10 kg intermitente | 3 | 3 | 1 | 2 | 6 | 6 | 7 | Rojo / 4 |
| 9 | Neutro + más de 10 kg estática o repetitiva | 1 | 1 | 0 | 3 | 4 | 4 | 4 | Amarillo / 2 |
| 10 | Máximo + actividad + golpes o fuerzas bruscas | 9 | 9 | 1 | 3 | 13 | 13 | 7 | Rojo / 4 |

La carga 0 corresponde a menos de 2 kg intermitente. En el caso 10 se consulta C en fila 8, columna 7, después de conservar los totales 13 y 13.

## Cómo probar paso a paso

1. Abrir una terminal en la raíz del proyecto con Java 17 o superior y Maven.
2. Tener MySQL funcionando con la base **existente** `rula_ergonomia`. No volver a ejecutar el script completo: empieza con `DROP DATABASE`. Las pruebas no lo ejecutan; solo leen sus valores como referencia.
3. Si no existe `config/db.properties`, copiar `config/db.properties.ejemplo` con ese nombre y poner las credenciales locales. Si ya existe, conservarlo. La URL de ejemplo permite autenticación `caching_sha2_password` en desarrollo local sin TLS. Para un servidor remoto se debe configurar TLS verificado. La clave real nunca va al repositorio.
4. Ejecutar `mvn compile`.
5. Ejecutar `mvn test`. Debe terminar con `BUILD SUCCESS`, **389 pruebas ejecutadas, 389 aprobadas, 0 fallos, 0 errores y 0 omitidas**.
6. Para revisar solo los diez casos y la validación de datos faltantes: `mvn -Dtest=MotorRulaTest test` (11 pruebas). Para ver resultados detallados, abrir `target/surefire-reports/`.

Las pruebas de MySQL contrastan las 272 combinaciones tanto con las filas actuales como con el SQL aprobado; no basta con que el lector y una base alterada coincidan entre sí. También revisan los siete puntajes de riesgo y sus textos/ID. Otras pruebas cubren límites decimales, ajustes, medición con ambos lados, un recorrido puntos → motor → riesgo y rechazos de entradas inválidas. Los datos dañados se simulan con interfaces JDBC del JDK, sin modificar MySQL ni añadir bibliotecas.

Si MySQL no está disponible, la configuración falta o sus valores difieren del diseño, la suite falla: no omite esa comprobación ni declara éxito. Esta fase se verificó con MySQL real. En un entorno restringido puede requerirse autorización para que Maven descargue componentes y acceda a localhost.

## Pendiente y punto de parada

La fase 3 (paso 2 del plan) es autenticación y permisos. No se implementó. El ingreso sigue deshabilitado como en la fase 1. Se espera el **OK** de Andy antes de continuar.
