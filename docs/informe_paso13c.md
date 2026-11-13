# Paso 13C · Correcciones de QA

13 nov 2026 · implementación terminada, pendiente de revisión de Andy.

## Qué cambió

1. **Botones de tablas:** `white-space:nowrap`, sin partir texto ni encoger la acción. En consultas, sugerencias y pendientes miden 36 px desde 721 px de ancho; en celular se conservan los 44 px exigidos por AGENTS.md. «Ver reserva» se midió en una sola línea a 1024 y 1440.
2. **Plegables:** los summary de «Ver otras secciones» y «Más filtros» usan flex y centrado vertical. Cerrados no reservan el contenido ni padding vertical: 44 px de línea y 2 px de bordes, total 46 px. El indicador de apertura se conserva mediante CSS. Abren y cierran con clic; Enter también cierra Más filtros.
3. **Demo opcional:** `SET NAMES utf8mb4;` antes de `START TRANSACTION`. Segunda reserva/visita Finalizada en Sede Alcázar, puesto y evaluación Verde basados en el caso manual 4 de MotorRula. Ahora son seis reservas, dos visitas Finalizadas y cuatro evaluaciones. General visible: **2,35**, clasificada **2 · Verde · Nivel 1**, con **2 de 3 sedes calificadas · falta Sede Anova (Medicina)**. Se conservó la regla de promedio; la visita principal mantiene el promedio almacenado 3,7 y Alcázar tiene 1. No se recalcularon registros existentes ni se alteró el esquema.
4. **Fotos del detalle:** `object-fit:contain`, centradas, máximo 520 px de alto; en celular máximo 360 px. Se ve la imagen completa sin recortarla.
5. **Fondo y menú:** se ocultan textos/etiquetas únicamente en `.fondo`; conserva líneas, puntos y animación previa. `.tablewrap` tiene fondo opaco y capa propia solo en pantalla, para impedir que el dibujo se transparente en sus filas. En 390 px las seis pestañas comparten una fila, debajo del encabezado, sin una pestaña huérfana.

No se tocó Java de producción ni JS, rutas, cálculo, permisos o cuentas reales. Los cambios de producción son CSS y el SQL demo autorizado. Solo se amplió la prueba existente del demo; se actualizaron sus instrucciones de instalación, README (únicamente fila 13) y estado de AGENTS.md.

## Requisitos y pruebas

| Requisito / encargo | Archivo y selector / método | Evidencia |
|---|---|---|
| RNF-002, punto 1: acciones legibles | `aplomo.css`, `:is(.consultas-table,.sugerencias-table,.pendientes-table) .btn` y media query desde 721 px | `mediciones.json`: seis «Ver reserva», cada uno 36 px, `nowrap`, un rectángulo de texto a 1024/1440; Pendientes y Sugerencias también 36 px. En móvil 44 px. |
| RNF-002, punto 2: plegables sin huecos | `aplomo.css`, `.consultas-secciones`, `.filtros-extra`, `summary` y `:not([open])` | Capturas `reservas-1440.png`, `reservas-1024.png`, `reservas-390.png`; closed 46/44 px caja/summary. Al abrir: contenido visible; al cerrar con Enter: 46 px otra vez. |
| RF-025 / RF-026, punto 3: demo de dos sedes | `datos_demo.sql`, `SET NAMES` y bloque final Alcázar; `HttpDatosDemoTest.demoEsIdempotenteConservaEdicionesYCubreLosTresColoresEnElInforme` | El SQL completo se carga repetidamente sin duplicados y conserva un pendiente editado. Comprueba dos visitas Finalizadas, cobertura, general disponible, puntaje y cinco mediciones de Alcázar contra `MotorRula.calcular` / tablas MySQL. Capturas del informe con general 2,35. |
| RF-016, RNF-002, punto 4: foto contenida | `aplomo.css`, `.foto-guardada img` y media query hasta 720 px | `evaluacion-1440.png`, `evaluacion-1024.png`, `evaluacion-390.png`: imagen cargada, `contain`, posición 50%/50%, altura 520/520/360 px. |
| RNF-002, punto 5: fondo y menú | `aplomo.css`, `.fondo :is(text,.lb)`, `.tablewrap` en pantalla y `.top .nav` / `.nav-tab` hasta 420 px | Texto de fondo visible: 0. Fondo de tablas `rgb(14,14,16)`; tablas y documento sin desbordar. Las seis pestañas tienen la misma coordenada vertical en 390. |
| RNF-004 / RNF-006 preservados | Autorización existente, sin modificaciones | Como Usuario, dirección directa a una foto: Acceso denegado. Captura `usuario-foto-denegada.png`; suite de permisos en verde. |

## Revisión visual

Se vieron capturas reales del navegador integrado, con datos demo en la base de pruebas. No se generaron mockups ni imágenes de pantallas reconstruidas.

| Ancho | Reservas | Usuarios | Evaluación | Informe |
|---|---|---|---|---|
| 1440 | Botón de 36 px en una línea; plegables cerrados de 46 px; tabla opaca | Tabla y acciones dentro del ancho | Foto completa centrada, límite 520 px | Resumen, general 2,35 y cobertura de dos sedes |
| 1024 | Mismas medidas; ningún «Ver reserva» partido | Tabla sin desbordar | Foto completa centrada, límite 520 px | General y sedes sin desbordar |
| 390 | Menú de seis pestañas en una fila; acciones de 44 px; listas como tarjetas | Filtros apilados y cuentas como tarjetas | Foto completa centrada, límite 360 px | Contenido apilado, general y niveles escritos legibles |

Además se midieron Reservas y Usuarios a 810/1080/1366/1920: sin desbordamiento del documento y navegación en una sola fila. Pendientes y Sugerencias se midieron a 1024/1440: acciones en una línea, 36 px y documento dentro del ancho. Las 24 mediciones están en `docs/evidencia_paso13c/mediciones.json`; doce capturas de las cuatro pantallas solicitadas, dos adicionales de Pendientes/Sugerencias y una de permisos en la misma carpeta.

## Verificación y privacidad

`TZ=America/Bogota ./mvnw test` (con salida de rendimiento al directorio de evidencia): **718 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, 60 segundos, final 13 nov 2026 05:35:39 -05. No se repitió la suite después de las capturas porque no hubo cambios nuevos de código. `pruebas.txt` y `rendimiento.json` conservan la evidencia.

El servidor temporal 8087 usó explícitamente **rula_ergonomia_pruebas**, una transacción sin confirmar y una carpeta temporal para las cuatro fotos de muestra generadas con IA. Se confirmó **QA13C: ROLLBACK completado** al cerrarlo. Ningún demo se cargó en la base real; no se borraron datos ni documentos. `config/db.properties` y `fotos/` se mantienen ignorados por Git.

## Checklist y pendientes

- ✅ Compilación y 718 pruebas aprobadas.
- ✅ Capturas reales de las cuatro pantallas a 1440/1024/390; paleta, tipografía y estructura del mock conservadas.
- ✅ Comprobación adicional de ancho en 810/1080/1366/1920 para Reservas y Usuarios.
- ✅ Controles móviles de al menos 44 px preservados; plegables probados con clic y teclado.
- ❌ Dedo en iPad/celular físico: no comprobado en este paso; sigue pendiente como en 13B.
- ✅ Usuario sin acceso a fotos, probado por dirección directa.
- ✅ Tildes del demo, textos originales conservados; codificación utf8mb4 explícita.
- ✅ Consola registrada sin errores/avisos antes del ensayo intencional de acceso denegado.
- ✅ Sin credenciales ni fotos privadas en el commit; datos de ensayo revertidos.

**Encargos 1–5 pendientes: nada.** No se repitió el recorrido de tres navegadores del Paso 13B, la impresión/PDF ni la detección de MediaPipe, y no se descargó software. La comprobación física y las dos versiones estables por navegador continúan como límites anteriores; no se presentan como certificadas.

## Cómo comprobarlo

1. Desde la raíz, ejecutar `TZ=America/Bogota ./mvnw test`: deben pasar 718 pruebas en la base separada.
2. Con el servidor iniciado, entrar como Administrador y revisar Reservas en 1440 y 1024: «Ver reserva» en una línea. Abrir/cerrar los dos plegables y comprobar que desaparece todo su contenido al cerrarlos.
3. Revisar Usuarios, una evaluación con foto y el informe en 1440/1024/390. La foto debe verse entera; en 390 el menú no deja una pestaña sola en otra fila.
4. Para demostrar la general, cargar voluntariamente el demo siguiendo INSTALAR.md, incluida la cuarta imagen de muestra. El script no se ejecuta solo; repetir únicamente `datos_demo.sql` no duplica datos. El informe de Humboldt debe indicar 2 de 3 sedes calificadas y general 2,35. Las fotos y resultados son ilustrativos, no evaluaciones de personas reales.
5. Con rol Usuario, intentar abrir la dirección de una foto: debe mostrar Acceso denegado.

## Git

Commit solicitado: `Paso 13C: correcciones de QA`. Push autorizado a `origin main` únicamente si origin es `https://github.com/Andybena2025/Aplomo-app.git`; se comprueba de nuevo inmediatamente antes de subir. El hash y el resultado de `git log -1 --oneline` / push se confirman en la entrega, tras crearlo. Se espera el OK de Andy antes de nuevas tareas.
