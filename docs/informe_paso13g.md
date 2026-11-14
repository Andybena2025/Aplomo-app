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


# B — Demo lista y atajo Evaluar ahora

## Qué cambió
Las tres empresas DEMO-APLOMO-E1/E2/E3 tienen al menos dos sedes; Humboldt conserva sus tres. Cada sede incluye un salón de práctica con un puesto ESCRITORIO y otro MESA activos. Hay una visita En curso por empresa, con ambos puestos sin evaluar. La carga repetida conserva los cambios y no duplica. Si se termina la visita de demostración, cargar no la reabre: el reinicio voluntario permite empezar de cero.

Inicio del Administrador muestra arriba «Evaluar ahora», empresa, sede y los puestos pendientes con sus enlaces directos. Los inactivos y ya evaluados no tienen botón. Si termina sus puestos, muestra «Todos los puestos están evaluados.»; sin visitas, «No hay una visita en curso» e «Ir a Reservas». El Usuario no recibe la tarjeta ni puede entrar por dirección directa.

Scripts voluntarios cargar_demo y reiniciar_demo en Mac/Windows; comparten la herramienta Java sin nuevas librerías. Usan la base configurada y una cuenta administradora de MySQL: rechazan la cuenta de la app y verifican permisos administrativos globales. Reiniciar exige la frase exacta REINICIAR DEMO antes de cualquier borrado. Selecciona solo las tres identificaciones exactas, no todos los prefijos DEMO. Borra sus hijos y auditoría relacionada en orden de claves; recarga en la misma transacción. Nunca borra cuentas; una cuenta vinculada bloquea antes de borrar. Tampoco desactiva claves foráneas, crea bases o altera config/db.properties. Cualquier fallo de base revierte el reinicio completo.

La limpieza física ocurre después de confirmar la transacción y solo considera rutas de fotos de esas empresas. Conserva archivos aún referenciados, ajenos, externos y enlaces simbólicos. Las cuatro imágenes históricas ilustrativas se copian si faltan; sus resultados siguen viniendo de los casos manuales comprobados con el motor, no de medir las imágenes.

**Aclaración explícita de Andy para este paso:** usar la base actual; solo tres empresas DEMO-APLOMO y sus registros, con confirmación en terminal y administrador de MySQL; sin crear otra base ni cambiar la configuración. No se ejecutó carga ni reinicio en la base real durante este trabajo. Los scripts quedan disponibles para hacerlo voluntariamente.

## Requisitos → implementación → pruebas
| Requisito | Archivo / método | Prueba / evidencia |
|---|---|---|
| RF-004/005/008/011/013a: demo con sedes, puestos y visita iniciada | datos_demo.sql, bloque 13G-B; Demo.cargar | DemoTest.cadaEmpresaTieneSedesConAmbosTiposYUnaVisitaListaSinDuplicar; HttpDatosDemoTest conserva los casos del motor y el informe |
| RF-014, RNF-003/006: acceso directo al puesto pendiente | VisitaDao.paraEvaluar, Visitas.paraEvaluar, InicioEvaluacion.tarjeta, Router Inicio, aplomo.css | HttpEvaluarAhoraTest.administradorVePendientesActivosYElEnlaceAbreElPuestoCorrecto; recorrido UI real Oficina y Planta |
| RNF-007: estado vacío | InicioEvaluacion.tarjeta | HttpEvaluarAhoraTest.sinVisitasMuestraMensajeYReservas |
| RF-002, RNF-004/005: permisos en servidor y datos protegidos | Visitas.paraEvaluar; permisos existentes de nueva evaluación | HttpEvaluarAhoraTest.usuarioNoVeElAtajoNiPuedeEntrarPorDireccionManual; captura Usuario Acceso denegado |
| RNF-004/010: reinicio acotado, explícito y reversible | Demo.ejecutar/verificarAdministrador/vaciar/limpiarFotos; cuatro scripts | DemoTest: confirmación, cuenta vinculada, rechazo cuenta de app, rutas compartidas/externas/enlaces; reinicio-demo-qa.json con MySQL administrador, ROLLBACK final |

## Verificación completa
- `TZ=America/Bogota ./mvnw -o test`: **833 aprobadas, 0 fallos, 0 errores, 0 omitidas**, contra rula_ergonomia_pruebas. Base anterior 811 + 13 de fases + 9 de demo/Inicio. Diez pruebas dirigidas de B (incluye una existente de demo) también aprobadas.
- Reinicio real ensayado con MySQL root exclusivamente en base de pruebas: carga doble, borrado y recarga, fallo reversible, conservación de cuentas y de una empresa DEMO-APLOMO-E4 y su puesto. Cuenta vinculada bloquea sin cambios. Todo ROLLBACK; ver reinicio-demo-qa.json.
- Terminal real: pidió usuario, contraseña oculta y frase exacta; NO canceló sin cambios. Scripts Mac pasan bash -n; comando compartido ejecutado en terminal. Windows no se ejecutó en este Mac: revisión de sus envoltorios, QA en Windows pendiente.
- Chromium integrado mediante su Playwright: login → Evaluar ahora → Planta completa → guardar → Inicio → Evaluar ahora → Oficina completa → guardar. Perfil S6_levantando_pieza_pesada_perfil.jpg, más frente y detalle de S6; perfil/frente Para medir y detalle De apoyo. Dos evaluaciones guardadas, dos análisis por cada una, resultado 7 · Rojo · Nivel 4; ambos puestos Evaluados y sin botón pendiente en Inicio. Son resultados de las elecciones explícitas de QA, no un puntaje predeterminado para esos puestos.
- 60 medidas adicionales de esos recorridos con nombres demo: sin desplazamiento vertical/lateral a 1366×950. Consola de los recorridos: cero errores. Inicio: seis enlaces al principio y cero desplazamiento lateral en 390/810/1080/1366. El Usuario no ve tarjeta y escribir a mano la URL devuelve Acceso denegado.
- Servidor temporal cerrado: **QA13G: ROLLBACK completado**. Fotografías temporales eliminadas solo de la carpeta propia del ensayo. Ninguna escritura de QA en rula_ergonomia.

## Checklist final
- ✅ 833 pruebas en verde, sin omitidas.
- ✅ Recorrido vigente e identidad conservados; capturas de Inicio, Lado, resultados y visita.
- ✅ Matriz solicitada: 241 medidas finales, cero desplazamiento vertical y lateral. Inicio también 390/810/1080/1366 sin deslizar de lado.
- ❌ Dedo/dispositivo físico, otros navegadores y ejecución Windows: pendientes. No se descargó software.
- ✅ Usuario bloqueado por servidor y por dirección manual, además de no recibir tarjeta.
- ✅ Textos en español con tildes; consola sin errores.
- ✅ Motor, POST /evaluaciones/calcular, esquema, valores RULA y colores sin cambios.
- ✅ No se agregan config/db.properties ni fotos/ al commit.

## Cómo probar B
1. Cerrar la app. Opcionalmente cargar la demo: bash scripts/cargar_demo.sh (Windows: scripts\cargar_demo.bat); escribir usuario administrador y contraseña de MySQL. No se cambia la conexión configurada.
2. Abrir la app y entrar como Administrador. En Inicio, Evaluar ahora muestra empresa/sede y puestos de Oficina/Planta.
3. Pulsar Evaluar en un puesto; completar consentimiento, contexto, fotos, lado, cuerpo y esfuerzo. Comprobar Paso 1…9 y guardar. Ese puesto deja de figurar entre los pendientes.
4. Entrar como Usuario: no aparece Evaluar ahora; dirección directa de nueva evaluación: Acceso denegado.
5. Solo para empezar de nuevo con la demo: cerrar la app, bash scripts/reiniciar_demo.sh (Windows scripts\reiniciar_demo.bat). Exige administrador de MySQL y REINICIAR DEMO; otra respuesta cancela. Si una cuenta está ligada a una demo, reasignarla manualmente antes; el script no toca ninguna cuenta.

## Lo que NO se hizo
No se cargó ni reinició la demo en la base real; ambos scripts son voluntarios. No se cambió esquema, RULA, endpoint de cálculo ni permisos existentes. No se certifica iPad físico, Safari/Chrome/Edge/Firefox completos ni Windows. La demostración de 811→833 pruebas no sustituye esas comprobaciones.

## Archivos cambiados
A: PaginasEvaluacion.java, PaginasAnalisis.java, RecorridoCuerpo.java, evaluacion.js, aplomo.css, FasesEvaluacionTest.java; AGENTS.md, README.md, INSTALAR.md; este informe y evidencia_paso13g.
B: datos_demo.sql; App.java; instalacion/Demo.java; VisitaDao.java; Visitas.java; Router.java; InicioEvaluacion.java; aplomo.css; scripts/cargar_demo.sh/.bat, reiniciar_demo.sh/.bat; DemoTest.java, HttpEvaluarAhoraTest.java; AGENTS.md, README.md, INSTALAR.md; este informe y evidencia_paso13g.

## Commits y entrega
- 41fe535 — Paso 13G-A: contador de pasos estable e iPad sin desplazamiento en todas las pantallas.
- Commit B: Paso 13G-B: demo lista y atajo Evaluar ahora.
- Un solo push al terminar ambos, únicamente a main de https://github.com/Andybena2025/Aplomo-app.git (remote origin verificado). Confirmación final en la respuesta de entrega y en Git.
