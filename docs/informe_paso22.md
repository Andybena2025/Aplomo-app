# Paso 22 · Portada móvil, tablas y cuentas del equipo

15 de noviembre de 2026. Sin capturas por encargo. Pendiente de revisión de Andy.

El Paso 21 se interrumpió durante la lectura: no se implementó su despliegue, arranque de base ni cambio de umbral. Este commit corresponde solo al Paso 22; Railway sigue pendiente y el umbral de detección permanece en 0,40.

## Parte 1 · Portada

El encuadre se aplica **solo hasta 700 px**. La franja de foto mide **46vh**, con sustitución **46dvh** cuando está disponible. Se acortan los espacios entre marca, título y campos; se conservan la tipografía, colores y tamaños de campos/botones. El logo móvil mide 40 px y sitúa el centro de su A bajo la plomada. En iPad y computador se conserva la composición anterior del login.

La foto de 1400×933 y el SVG reciben **el mismo ancho, alto y desplazamiento**. El cálculo usa plomada x=1020, final y=800 y punta y=816. Busca el mayor tamaño que conserva cabeza/punta y cubre el contenedor sin huecos; centra hasta donde permite el borde derecho de la foto. En los celulares comprobados, la plomada queda entre el 54 % y el 57 % del ancho. El degradado termina en el fondo existente, sin cambiar la animación ni las reglas de movimiento reducido.

### Medidas reales del navegador

| Tamaño | Foto, alto | Borde inferior de Entrar antes → después | Desborde horizontal final |
|---|---:|---:|---:|
| 360×740 | 340,40 px, 46 % | 783 → **718,40 px** | 0 px |
| 390×844 | 388,23 px, 46 % | 793,50 → **766,23 px** | 0 px |
| 430×932 | 428,72 px, 46 % | 859,50 → **806,72 px** | 0 px |
| 844×390 horizontal | 504 px, anterior | 456 → **456 px** | 0 px |
| 820×1180 iPad | 489 px, anterior | 1045,50 → **1045,50 px** | 0 px |
| 1366×950 computador | 950 px, anterior | 679 → **679 px** | 0 px |

Entrar queda visible sin desplazar en los **tres celulares verticales**. En horizontal se mantiene el desplazamiento vertical anterior, permitido por el encargo; no hay desbordamiento horizontal.

En vertical: cabeza dentro de la foto, a 6,59 / 7,53 / 8,00 px del borde superior; punta dentro de la franja, a unos 4 px del borde inferior. Centro de A frente a plomada: diferencia **menor de 0,01 px** en los tres tamaños. Fondo y SVG usan dimensiones coincidentes; las diferencias de lectura inferiores a 0,01 px son redondeo del navegador. No se tomaron capturas: la revisión fue geométrica/DOM, no una certificación visual en un iPhone físico.

## Parte 2 · Tablas y rótulos

Antes, en 820 px, «Ver reserva» sobresalía **28,59 px** del área visible. La columna Rol medía 86,70 px y «Administrador» ocupaba **42 px** de alto, dos líneas.

Ahora la última columna de acciones/detalle permanece visible dentro del contenedor desplazable; se usa el fondo opaco existente y se conserva el desplazamiento interno para datos que no caben. Rol tiene ancho mínimo de 9rem y no parte sus palabras. No se cambiaron botones ni colores.

| Tabla | 768 px | 820 px | 1024 px | 1180 px |
|---|---|---|---|---|
| Reservas | 0 cortados | 0 cortados | 0 cortados | 0 cortados |
| Visitas | 0 cortados | 0 cortados | 0 cortados | 0 cortados |
| Pendientes | 0 cortados | 0 cortados | 0 cortados | 0 cortados |
| Sugerencias | 0 cortados | 0 cortados | 0 cortados | 0 cortados |
| Usuarios | 0 cortados | 0 cortados | 0 cortados | 0 cortados |
| Evaluaciones de una visita Finalizada | 0 cortados | 0 cortados | 0 cortados | 0 cortados |

**24 combinaciones**, todas con **0 px de desborde de página**. Margen derecho mínimo de los botones: **15 px**. Rol: **21 px** de alto, una línea en los cuatro tamaños. Datos de QA: 15 reservas, 7 visitas, 4 pendientes, 24 sugerencias, 3 usuarios y 6 evaluaciones en la visita comprobada.

El desplazamiento horizontal **interno** se conserva: máximo 116 px a 768 y 64 px a 820, en la tabla de evaluaciones; no se confunde con desborde de toda la página. Las acciones permanecen en una fila y no quedan cortadas.

Se comprobaron además siete rótulos (Empresas, Reservas, Visitas, Pendientes, Sugerencias, Usuarios y Evaluaciones) a **360 y 390 px**: **14/14**, todos de **11 px**, una línea y sin desborde. Conservan el color/contraste previo.

## Parte 3 · Herramienta de cuentas

`scripts/crear_cuentas.sh` y `.bat` ejecutan una utilidad voluntaria que lee las cuatro columnas de `config/cuentas.csv`. El archivo está ignorado; `config/cuentas.csv.ejemplo` contiene datos ficticios y ninguna contraseña.

El Administrador se autentica con contraseña oculta en una consola real. La utilidad consulta las empresas por el servicio existente y crea cada cuenta mediante **Usuarios.crear**, sin SQL directo. Pide teléfono y cargo porque el servicio los exige y no vienen en el CSV. Los correos existentes se saltan, incluidas cuentas inactivas, sin cambios.

Cada clave temporal tiene **16 caracteres aleatorios** con letras/números legibles. Usuarios aplica bcrypt de costo 10 y el cambio obligatorio al primer ingreso. Se imprime una tabla, con cada clave una vez tras crear su cuenta; no hay archivos de claves, contraseñas en argumentos ni registro de salida en la herramienta. La repetición no recupera claves perdidas: se restablecen desde Usuarios → Editar. Guía: [CUENTAS.md](CUENTAS.md).

## Pruebas

- **965/965 Maven**, cero fallos, errores u omitidas; incluye **16 nuevas** de cuentas en MySQL de pruebas.
- **110/110 JavaScript**, cero fallos, canceladas u omitidas; incluye **6 nuevas** de encuadre.
- Cuentas: CSV UTF-8/BOM/comillas, siete entradas inválidas, generación legible de 100 claves, dos roles, empresa obligatoria, bcrypt, salida única, repetición sin duplicados ni cambios de cuenta inactiva, primer ingreso obligado a cambiar clave, rechazo de Usuario y Administrador temporal, teléfono/cargo obligatorios y NIT inexistente.
- `bash -n scripts/crear_cuentas.sh`: correcto. Compilación correcta. El script Mac también se ejecutó en una consola real: al no existir el CSV privado, mostró el mensaje previsto y terminó sin pedir claves ni crear cuentas.
- La primera suite encontró una aserción antigua que buscaba código incrustado en login.html. Se ajustó únicamente esa aserción para comprobar la carga de portada.js y su respuesta HTTP 200/tipo JavaScript, conservando las comprobaciones de marca.
- Cero errores de consola capturados en portada y consultas. App normal de 8080 conservada: login 200 y CSS/portada.js servidos con contenido idéntico a los archivos actualizados.
- QA con datos ficticios solo en **rula_ergonomia_pruebas**, transacción revertida con **ROLLBACK confirmado**; las cuentas nuevas de las pruebas también se revierten. No se crearon cuentas en la base real ni se borraron registros/fotos.

## Requisitos y pruebas que los respaldan

| Parte / requisito | Archivo y función | Comprobación |
|---|---|---|
| Portada móvil, RNF-002/008 | `portada.js`: encuadre/montar; login.html; media ≤700 de CSS | 6 pruebas geométricas + medidas de seis tamaños. |
| Tablas/rótulos legibles, RNF-002/008 | `aplomo.css`: última columna, Rol y eyebrow | 24 combinaciones de tablas y 14 rótulos medidos. |
| RF-003 / RF-010 · Crear sin duplicar | `CrearCuentas.crear`, servicio `Usuarios.crear` existente | Dos cuentas creadas y repetidas, campos/hash/estado conservados. |
| RF-001 / RNF-004 · Claves temporales | `CrearCuentas.temporal`, `Contrasenas` y `Autenticacion` existentes | Longitud/letras/números/bcrypt; login 303 a cambiar contraseña. |
| RF-002 · Permiso y empresa | `CrearCuentas.crear`, `Usuarios`/`Empresas` existentes | Usuario rechazado; Administrador temporal rechazado; NIT inexistente no crea. |
| RNF-007 · Instrucciones sencillas y privacidad | `CUENTAS.md`, mensajes de herramienta, .gitignore | CSV inválido/resultado ya existe, plantilla ficticia y lista privada ignorada. |

## Archivos tocados por parte

**Parte 1:**
- `src/main/resources/plantillas/login.html`: carga del encuadre compartido.
- `src/main/resources/static/js/portada.js`: cálculo del encuadre.
- `src/main/resources/static/css/aplomo.css`: franja y espacios móviles.
- `scripts/qa_portada.cjs`: seis pruebas geométricas.
- `src/test/java/co/aplomo/web/HttpMarcaTest.java`: conserva las comprobaciones de marca y verifica el nuevo recurso JavaScript servido por HTTP.

**Parte 2:**
- `src/main/resources/static/css/aplomo.css`: acciones visibles, Rol sin partir y rótulos móviles de una línea.

**Parte 3:**
- `src/main/java/co/aplomo/instalacion/CrearCuentas.java`: utilidad por servicios existentes.
- `scripts/crear_cuentas.sh` y `scripts/crear_cuentas.bat`: entradas Mac/Windows.
- `config/cuentas.csv.ejemplo`: plantilla ficticia.
- `.gitignore`: exclusión del CSV privado.
- `src/test/java/co/aplomo/instalacion/CrearCuentasTest.java`: dieciséis pruebas.
- `docs/CUENTAS.md`: guía.

**Documentación de entrega:** `docs/informe_paso22.md`, `AGENTS.md`, `README.md` (fila 22/contador) e `INSTALAR.md` (acceso a la herramienta/contador).

## Checklist, pendientes y cómo probarlo

- ✅ Maven 965/965 y JS 110/110; base de pruebas, sin datos reales de QA.
- ✅ 360/390/430 vertical, horizontal sin rotura lateral; medidas de iPad y computador conservadas en login.
- ✅ 24 tablas sin botones cortados y 14 rótulos en una línea; consola sin errores capturados.
- ✅ Permisos por el servicio, BCrypt y cambio obligatorio comprobados; CSV privado fuera de Git.
- ✅ Sin cambios al motor RULA, `POST /evaluaciones/calcular` ni esquema/datos de tablas existentes; sin dependencias nuevas, capturas o borrados.
- ❌ Capturas/comparación visual completa: no realizadas por encargo; mediciones reales del DOM.
- ❌ Dispositivo físico/iPad táctil y ejecución nativa Windows: no comprobados en este Mac. Se entregan scripts y se revisó su contenido, sin certificar Windows.

Pendiente de implementación del Paso 22: **nada**. Los límites físicos/Windows se indican arriba. **Paso 21/Railway sigue pendiente**; no se presenta como completado.

Para probar: abre el login en celular y confirma cabeza/punta, A y Entrar sin desplazar. Abre las seis consultas en iPad; desplaza solo el interior de tablas anchas. Para las cuentas, sigue [CUENTAS.md](CUENTAS.md): copia la plantilla local, usa datos reales de tu equipo, autentícate como Administrador y guarda en privado las claves mostradas. Repite el CSV: debe decir «ya existe» y conservar las cuentas. Cada persona debe cambiar su clave al entrar.

## Git

Base: `75524a1`. Entrega: «Paso 22: portada móvil, tablas y cuentas de equipo». Hash y confirmación del push al repositorio verificado se entregan al cerrar. Esperar el OK de Andy antes de otro paso.
