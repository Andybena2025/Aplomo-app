# Paso 19 · Docker comprobado

14 de noviembre de 2026. Pendiente de revisión de Andy. Sin capturas.

## Qué falló y qué se corrigió

Docker Desktop estaba abierto (motor 29.7.2). Se detuvo la app nativa antes de construir y levantar el proyecto aislado `aplomo_qa_p19`. Su MySQL es el servicio `db`: no publica 3306 y no utiliza MySQL local.

El único fallo funcional confirmado fue **PORT ignorado**. La imagen anterior seguía escuchando en 8080 pese a recibir PORT=18765. Las cinco pruebas nuevas también fallaban con ese código. Se corrigió una expresión en `Config.leer`: **PORT → APLOMO_PUERTO → 8080**. Un puerto inválido no se ignora.

Dockerfile, Compose e inicialización funcionaron y no necesitaron cambios. La construcción inicial tardó más resolviendo componentes Maven; ese tiempo se informa separado del arranque.

## Las seis comprobaciones

| Punto | Resultado real |
|---|---|
| 1 · Base automática | **21 tablas**; valores RULA A=144, B=72, C=56: **272 en total**; **4 niveles/3 colores**; un Administrador inicial activo. |
| 2 · Login | `http://localhost:8080/login`: **200**. |
| 3 · Administrador y páginas | Cuenta semilla: **303 al cambio obligatorio de contraseña**. Se cambió a una clave ficticia de QA solo en Docker. Empresas, Reservas, Visitas y Probar evaluación: **200 las cuatro**. Crear práctica: 303; guardar evaluación con foto: **201**. |
| 4 · Persistencia | Tras down/up, nuevo login y evaluación: correctos; foto autenticada **200**, mismos bytes y SHA-256. MySQL usa un volumen en `/var/lib/mysql`; fotos, otro en `/app/fotos`. |
| 5 · Variables | Conexión completa mediante APLOMO_DB_URL/USUARIO/CLAVE, sin archivo local de configuración en la imagen. APLOMO_PUERTO=18766: login **200 en 18766**. PORT=18765 junto con APLOMO_PUERTO=18766: login **200 en 18765**. Puertos temporales del host: 18087/18088. |
| 6 · Claves | .dockerignore/.gitignore revisados. JAR sin archivos locales ni valores privados de claves comprobados; entorno e historial de imagen sin claves privadas. Git no sigue .env, config/db.properties ni fotos/. No se imprimieron claves. |

La app corre con **UID 10001**, no root. La cuenta MySQL de red conserva SELECT, INSERT y UPDATE, sin DELETE/DROP/GRANT.

### Foto persistente

Foto ficticia subida por multipart al servidor real de Docker: `/fotos/1`, **60.476 bytes** antes y después. Huella SHA-256 idéntica:

`25c0ddd8398ea156b1f4c5de4c5a895cfba2f330d2f00e88392c7d5b11e092ad`

Después del reinicio y de las pruebas: **1 foto, 1 evaluación y 21 tablas** en la base de la app Docker. No se usó down -v ni se eliminaron volúmenes o datos.

## Tiempos

| Medida | Tiempo |
|---|---:|
| Primera construcción | **395,93 s**, unos 6 min 36 s |
| Primer up hasta login 200, incluido MySQL nuevo | **11,94 s** |
| Segundo up hasta login 200, reutilizando volúmenes | **6,59 s** |

## Pruebas

**948 Maven ejecutadas y aprobadas; 0 fallos, 0 errores, 0 omitidas.** La suite completa corrió con Java 17/Maven en un contenedor, desde una copia limpia sin archivos privados, apuntando a **rula_ergonomia_pruebas dentro del MySQL Docker**. No se usó MySQL local. La base de pruebas se preparó sin ejecutar DROP y separada de la base donde se guardó la foto. Las transacciones de ensayo se revierten.

**87 JavaScript ejecutadas y aprobadas; 0 fallos.** Comando: `node --test scripts/qa_*.cjs`.

ConfigTest suma cinco casos: prioridad de PORT y rechazo de 0, 65536, texto y vacío. Los cinco fallaban antes y pasan ahora; configuración queda con 16 pruebas.

## Archivos tocados

- `src/main/java/co/aplomo/config/Config.java`: prioridad de PORT en leer.
- `src/test/java/co/aplomo/config/ConfigTest.java`: cinco casos nuevos.
- `docs/DOCKER.md`: tres comandos, puerto ocupado, volúmenes y entorno.
- `docs/informe_paso19.md`: este informe.
- `AGENTS.md`: estado.
- `README.md`: fila del Paso 19 y contador.
- `INSTALAR.md`: enlace, prioridad de puerto y contador.

## Requisitos y pruebas que los respaldan

| Requisito | Implementación | Comprobación |
|---|---|---|
| RNF-001 / RF-027 · Base y tablas RULA | Compose, initdb y Config existentes | 21 tablas/272 valores por SQL; suite RULA con MySQL Docker. |
| RF-001a/001c · Acceso | Router y Autenticacion existentes | Login semilla, cambio obligatorio y cuatro rutas autenticadas por HTTP. |
| RF-016 / RNF-006/009 · Fotos e historial | Volúmenes y servicios existentes | Subida 201, reinicio y descarga autenticada 200 con huella idéntica. |
| RNF-003 · Operación | Compose y servidor | Arranques medidos y suite con prueba de rendimiento. |
| RNF-004 · Entorno y claves | Config.leer, exclusiones y usuario Docker | Cinco casos de puerto, dos puertos reales, UID y auditoría descrita. |
| RNF-007 · Instrucciones | DOCKER.md | Tres comandos y solución para puerto ocupado sin borrar datos. |

## Checklist y límites

- ✅ Construcción y arranque Docker reales, semillas y cuatro páginas comprobadas por HTTP.
- ✅ Foto y base conservadas tras apagar/levantar; entorno y puertos comprobados.
- ✅ Maven 948/948 y JavaScript 87/87; claves privadas excluidas según la auditoría.
- ✅ Motor RULA, POST /evaluaciones/calcular y esquema original intactos; sin dependencias nuevas.
- ✅ Docker de QA apagado conservando volúmenes; app nativa restaurada en 8080. No se detuvo MySQL local ni se utilizó para validar Docker.
- ➖ Sin capturas por encargo. No se hizo QA visual, táctil, matriz de tamaños ni consola del navegador: fueron comprobaciones HTTP/SQL/contenedor. No se cambiaron pantallas o permisos; la suite conserva sus pruebas de acceso.
- ➖ No se desplegó a un hosting externo. PORT se comprobó localmente en la imagen real.

No queda implementación pendiente de este encargo. La prueba de un hosting concreto queda fuera de esta validación local.

## Cómo lo prueba Andy

Sigue [DOCKER.md](DOCKER.md): detén primero la app normal, levanta Docker, abre Iniciar sesión y usa la cuenta de esa instalación. Conserva la carpeta/nombre de proyecto para reutilizar sus volúmenes. Al terminar, apaga Docker y vuelve al arranque local de INSTALAR.md.

Esta QA usó el proyecto **aplomo_qa_p19**, con datos ficticios independientes. Se conservaron sus volúmenes y la caché Maven. La contraseña cambiada en QA pertenece solo al Administrador de ese proyecto, no a la instalación normal.

## Git

Base: `1d6a04e`. Commit de entrega: «Paso 19: Docker validado y puerto de hosting». Hash y confirmación de push se entregan al cerrar; consultar git log -1 --oneline.

Esperar el OK de Andy antes de comenzar otro paso.
