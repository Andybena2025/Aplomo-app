# Paso 15-D · Evaluación fluida y nuevo login

Encargo de Andy del 14 nov 2026. Flujo: Permiso → Puesto → Postura 1 → Postura 2 → Resultado. Cada postura agrupa foto, cuatro ángulos, ajustes, muñeca y las tres listas de esfuerzo/carga. No hay resultado intermedio ni copia de carga entre posturas. El servidor sigue calculando todo lo que se guarda. Se conserva el acceso permanente de práctica.

## Auditoría: exactamente qué se retiró

- «¿En qué parte del trabajo se siente más incómodo o más cansado?» (`momento_campo`), su lista y adaptación de textos.
- «¿Y cuál postura mantiene por más tiempo en el turno?» (`duracion_campo`), «Es la misma» y «Con una sola postura basta». Las dos posturas pasan a ser fijas.
- La pantalla/pregunta «¿Qué lado del cuerpo trabaja más?» y `lado_recorrido`: sustituida por detección automática y chips opcionales Derecho/Izquierdo/Ambos.
- Los chips combinados «Cabeza torcida o inclinada» y «Espalda torcida o inclinada», y ambas preguntas «¿Cómo está: girada, inclinada o las dos?» (`cuello_tipo`, `tronco_tipo`): ahora cuatro chips directos que usan los mismos mapeos de orientación.
- «Lo más pesado que levanta o mueve en su turno, ¿se parece más a un celular, a un galón de agua o a un bulto?» (`peso_campo`): ahora lista Carga.
- «Esa postura, ¿la mantiene quieta por más de un minuto, o repite el mismo movimiento más de cuatro veces por minuto?» (`esfuerzo_opcion`): ahora lista Esfuerzo muscular.
- «¿La herramienta o la pieza le ha dado alguna vez un tirón o un golpe seco en la mano o en el brazo?» (`golpe_campo`): ahora lista Golpes o tirones.
- La pantalla Carga, el resultado intermedio, las dos secciones opcionales separadas, los subtextos/ejemplos y las etiquetas pequeñas bajo las preguntas. Un solo plegable Frente/Detalle.
- Los campos ocultos sin uso `cuenta_recuerdo` y `carga_opcion`, y los recuerdos de respuestas del puesto anterior. Los controles internos ocultos del analizador se conservan porque aún sirven al cálculo existente.
- En login: marca y lema superpuestos a la foto y el párrafo «Evaluación ergonómica de puestos de trabajo con el método RULA: fotos, ángulos, puntaje y resultados en un solo lugar.» La marca pasa al panel a 48 px, con divisor; el script admite que no exista `.in`.

## Qué queda

Consentimiento escrito obligatorio y permiso de fotos; una pregunta al trabajador, «¿Cuántas horas al día trabaja en este puesto?»; seis condiciones RF-015 con Ninguna y observaciones opcionales. Por perfil: cuatro filas de ángulos (Brazo, Antebrazo, Cuello, Tronco), muñeca Recta/Un poco doblada/Muy doblada, y los chips observables Hombro subido, Brazo separado, Brazo apoyado, Antebrazo cruza/sale, Cabeza torcida, Cabeza inclinada, Espalda torcida, Espalda inclinada, Pies sin buen apoyo, Torcida hacia un lado y Giro hasta el límite. Quedan tres listas cortas: Carga, Esfuerzo muscular y Golpes o tirones, cada una con No aplica ya respondido. Elegir rangos manuales, ajustar el lado y añadir respaldo son opcionales. Todos los datos restantes se usan para el cálculo, el contexto guardado o el recorrido.

## Validación y decisiones de implementación

Revisión local en deteccion.js: nariz/hombros/caderas/rodillas/tobillos visibles (≥0,5) dentro del marco; perfil con hombros/torso, umbral central 45 % y margen ±10 puntos porcentuales. La franja 35–55 % avisa sin bloquear. Son heurísticas y se pueden corregir explícitamente; no certifican que una foto sea válida. Un recuadro ofrece Subir otra, Marcar a mano o Usar esta de todos modos; aceptar se ofrece solo ante dudas de IA. Formato real JPG/PNG, 5 MB y archivo decodificable se comprueban antes de detectar; la imagen pequeña usa como elección de implementación **menos de 240 px en cualquier dimensión**, configurable en MIN_LADO. No se cambian límites ni validación del servidor. Marcar a mano se desactiva si no hay una imagen utilizable. La etapa gris de fallo de IA, reintento, 1280 px y modelo ligero siguen disponibles.

Ambos pide dos perfiles por postura (cuatro en el recorrido completo). Añadir análisis conserva su contrato de fotos existentes: Ambos se ofrece cuando hay dos perfiles almacenados; no añade archivos por el endpoint de análisis. Sin permiso de fotos se conserva la regla previa: dos contextos sin fotografías ni puntaje, sin inventar mediciones. Cada postura se guarda como evaluación independiente y la crítica al final, igual que 15-C; no es una transacción conjunta de las dos.

| Requisitos | Implementación | Comprobación |
|---|---|---|
| RF-014–RF-016: contexto, consentimiento, fotos | ConversacionEvaluacion, fotos-campo.js | HttpRevisionEvaluacionTest, qa_fotos_campo.cjs y contratos existentes |
| RF-017–RF-020: puntos, ajustes, carga, resultado | conversacion-evaluacion.js, MapeosCampo, analisis.js | Casos RULA/HTTP existentes, qa_posturas_campo.cjs, recorrido real de QA |
| RNF-002/003/007: distribución, carga y mensajes | aplomo.css, login.html, deteccion.js | HttpMarcaTest, pruebas JS, medidas DOM en navegador integrado |
| RF-002/RNF-004: permisos y validación servidor | Rutas y servicios existentes conservados | Suite completa de seguridad y validación HTTP |

**902 Maven + 56 JS aprobadas**, cero fallos. Navegador integrado sin capturas: login, recorrido Planta de dos posturas, aviso de cuerpo incompleto, aviso de frente, aceptación explícita, cargas diferentes/defaults independientes, respaldo Detalle, resultado único 7/6 y puesto en 7; Oficina con Ambos (cuatro análisis) y Añadir análisis por respaldo manual, guardados en la base de pruebas. 21 mediciones a 1366/834/390 sin desbordamiento lateral y cero errores de consola. QA exclusivamente en rula_ergonomia_pruebas, ROLLBACK confirmado. No se generaron capturas ni evidencia.

Pendiente: iPad físico/tacto, otros navegadores y evaluación de sensibilidad/especificidad de las heurísticas con fotos de campo. No se afirma causa del fallo físico de iPad ni certificación de accesibilidad completa. Sin cambios de esquema, co.aplomo.rula, POST /evaluaciones/calcular o dependencias. Probar desde Inicio → Probar evaluación, responder permiso/horas/condiciones, completar ambos perfiles de postura si elige Ambos, y revisar el único resumen antes de guardar.
