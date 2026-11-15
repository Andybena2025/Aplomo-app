# Instalar y correr APLOMO. en tu computador

Guía para el equipo. La primera vez toma unos **15 minutos**; después, arrancar la app toma segundos.


## Con Docker

Necesitas **Docker Desktop** instalado y abierto. No necesitas instalar Java ni MySQL. Desde la raíz del repositorio:

```sh
cp .env.ejemplo .env
```

Abre `.env` y cambia las dos claves de ejemplo por claves distintas. Ese archivo está ignorado por Git. Luego:

```sh
docker compose up --build -d
```

Abre **http://localhost:8080**. Cuenta inicial: **admin@rula.local**, contraseña temporal **Cambiar2026**; la app exige cambiarla en el primer ingreso. Es el Administrador de APLOMO, no el usuario root de MySQL. Si otra copia de APLOMO está usando 8080, detén esa copia antes de levantar Docker.

```sh
docker compose logs -f app
docker compose restart app
docker compose down
```

MySQL y las fotos se guardan en volúmenes con nombre; reiniciar o apagar con `down` los conserva. El SQL aprobado se ejecuta únicamente al inicializar un volumen vacío. La cuenta `app_rula` puede conectarse desde la red interna con SELECT, INSERT y UPDATE; no tiene DELETE. No se publica el puerto de MySQL en el equipo.

**Solo si quieres reiniciar desde cero:** `docker compose down -v` **borra los datos y las fotos de esos volúmenes**. No lo uses para actualizar la app.

La instalación sin Docker sigue igual: `bash scripts/iniciar.sh` en Mac o `scripts\iniciar.bat` en Windows. Las variables `APLOMO_DB_URL`, `APLOMO_DB_USUARIO`, `APLOMO_DB_CLAVE`, `APLOMO_PUERTO` y `APLOMO_CARPETA_FOTOS` permiten configurar la app; si faltan variables de conexión se conserva el respaldo de `config/db.properties`. Las sustituciones `-Daplomo.db.url`, `-Daplomo.puerto` y `-Daplomo.fotos` siguen disponibles, especialmente para aislar las pruebas.

### Demostración opcional en Docker

Primero inicia sesión y cambia la contraseña temporal del Administrador. Después carga el demo completo con el mismo motor y fotos que la instalación local:

```sh
docker compose --profile demo run --build --rm demo
```

La carga es voluntaria e idempotente: tres empresas ficticias, tres colores, doce puestos con dos posturas, fotos, pendientes, sugerencias, visitas Finalizadas y reservas futuras. No crea cuentas, no cambia contraseñas ni configuración. Las fotos se conservan en el volumen de la app. La empresa de práctica rápida es independiente y queda fuera de los contadores y consultas normales.


## Qué necesitas

| Programa | Para qué | ¿Se instala solo? |
|---|---|---|
| **Java 21** (JDK) | Ejecuta la aplicación | Con el script `instalar` |
| **MySQL 8** | Guarda los datos | Mac: con el script · Windows: a mano (5 min, abajo) |
| Maven | Compila y descarga las librerías | No hace falta: el proyecto trae `mvnw` |
| Librerías (MySQL Connector, jBCrypt, JUnit) | Conexión, contraseñas y pruebas | Sí, Maven las descarga solo desde `pom.xml` |
| Navegador | Usar la app | Chrome, Edge o Firefox |
| IntelliJ IDEA | Editar el código | Opcional |

Necesitas **internet** la primera vez (descarga Maven y las librerías).

---

## 1. Descargar el proyecto

- **Con Git:** `git clone https://github.com/Andybena2025/Aplomo-app.git`
- **Sin Git:** en GitHub, botón verde **Code → Download ZIP**, y descomprímelo donde quieras.

## 2. Instalar Java y MySQL (solo una vez)

### Mac

Abre **Terminal** dentro de la carpeta del proyecto y escribe:

```bash
bash scripts/instalar_mac.sh
```

El script usa Homebrew. Si no lo tienes, te muestra la línea para instalarlo. Puede pedirte la contraseña del Mac.

### Windows (10 u 11)

1. Doble clic en **`scripts\instalar_windows.bat`**. Instala Java 21 con `winget`.
2. Si te dice que **falta MySQL**, se abre la página de descarga:
   1. Descarga **MySQL Installer for Windows** (el archivo grande sirve sin internet).
   2. Elige **Server only**.
   3. Deja el puerto **3306**.
   4. Escribe una contraseña para **root** y **guárdala** (la vas a necesitar).
   5. Deja marcado **Configure MySQL Server as a Windows Service** y termina.
3. **Cierra la ventana** y abre una nueva para que Windows reconozca Java.

## 3. Arrancar la app

| Mac | Windows |
|---|---|
| `bash scripts/iniciar.sh` | Doble clic en `scripts\iniciar.bat` |

El script:

1. Crea `config/db.properties` con la conexión de ejemplo (este archivo nunca se sube a GitHub).
2. Te pide la contraseña de **root** de MySQL (en Mac, si nunca le pusiste una, solo presiona Enter).
3. **Solo la primera vez** crea la base `rula_ergonomia` con las 21 tablas, las tablas RULA, la cuenta del Administrador y el usuario de la app. Si la base ya existe, no la toca.
4. Carga las 11 reglas de sugerencias (no duplica nada).
5. Arranca la app.

Abre **http://localhost:8080**. Para apagarla: **Control + C** en la ventana.

¿Otro puerto? `bash scripts/iniciar.sh 8081` o `scripts\iniciar.bat 8081`.

## 4. Primer ingreso

- Correo: **`admin@rula.local`**
- Contraseña temporal: **`Cambiar2026`**

La app te pide cambiarla. Usa al menos 8 caracteres, con letras y números. Para probar el rol **Usuario**, crea una empresa y luego una cuenta de Usuario para ella desde **Usuarios**.

## 5. Usarla desde el celular o el iPad

Con la app corriendo, conecta el celular al **mismo Wi-Fi** que el computador y abre `http://IP-DEL-COMPUTADOR:8080`. El script de Mac te muestra esa dirección. En Windows, escribe `ipconfig` en una terminal y busca "Dirección IPv4".

---

## Práctica rápida desde la app

Como Administrador, abre **Inicio → Probar evaluación → Abrir evaluación de prueba** y elige **Probar Planta** u **Oficina**. También puedes abrir `/demo/evaluacion`. No necesitas cargar el SQL demo ni usar root de MySQL: se prepara una empresa ficticia `DEMO-APLOMO-PRACTICA` en la base configurada, con una visita En curso reutilizable. Los intentos se guardan allí; si finalizas esa visita, el acceso abre otra sin borrar la anterior. Con `-Daplomo.modoDemo=false` se oculta y bloquea el acceso. El reinicio de las tres empresas demo anteriores no borra esta empresa de práctica.

## Datos de demostración completos (opcional)

Primero entra como Administrador y cambia su contraseña temporal. Desde la raíz del proyecto:

- Mac: `bash scripts/cargar_demo.sh`.
- Windows: `scripts\cargar_demo.bat`.
- Docker: `docker compose --profile demo run --build --rm demo`.

El cargador prepara Ensambles del Quindío, Muebles Cafeteros y Humboldt con datos ficticios, al menos dos sedes por empresa, tres puestos con dos posturas en cada empresa, resultados Verde/Amarillo/Rojo calculados por el motor, pendientes abiertos/arreglados, sugerencias pendientes/hechas, visitas Finalizadas y reservas futuras. Copia automáticamente las fotos originales de muestra con nombres únicos; la base conserva solo la ruta. Los ángulos son casos manuales simulados de docs/03, no una valoración real de las fotos. Repetirlo no duplica evaluaciones ni restablece estados editados.

No cargues `datos_demo.sql` a mano para obtener resultados: ahora contiene solo estructura ficticia. Los análisis se generan desde Java con las tablas RULA de MySQL. La práctica rápida `DEMO-APLOMO-PRACTICA` está separada de estas tres empresas y no aparece en sus consultas ni indicadores.

Para **reiniciar solo las tres empresas DEMO**: Mac `bash scripts/reiniciar_demo.sh`; Windows `scripts\reiniciar_demo.bat`. El script exige administrador de MySQL y escribir `REINICIAR DEMO` antes de borrar los registros vinculados exclusivamente a DEMO-APLOMO-E1, E2 y E3. Si hay cuentas vinculadas, cancela. Conserva las cuentas, la práctica rápida, las demás empresas y la configuración. Cierra la app antes y vuelve a abrirla al terminar. Es una herramienta voluntaria de desarrollo.

## Pruebas (opcional)

Las pruebas usan una base aparte, **`rula_ergonomia_pruebas`**, para no ensuciar la real.

```bash
bash scripts/crear_base_pruebas.sh   # Mac; en Windows usa "Git Bash" con el mismo comando
./mvnw test                           # Windows: mvnw.cmd test
```

Deben aprobar **942 pruebas**.

---

## Si algo falla

| Mensaje o problema | Solución |
|---|---|
| `java` no se reconoce | Cierra y abre la terminal. En Windows reinicia si sigue igual. |
| "No pude entrar a MySQL" | Revisa la contraseña de root. Mac: `brew services start mysql`. Windows: abre **Servicios**, busca **MySQL** y pulsa **Iniciar**. |
| "No se pudo leer config/db.properties" | Corre de nuevo el script `iniciar`; lo crea solo. |
| `Access denied for user 'app_rula'` | La base se creó con otra clave. Revisa que `config/db.properties` diga `db.clave=CambiarEstaClave2026`. |
| "Address already in use" o el puerto está ocupado | Ya hay una copia corriendo, o usa otro puerto: `iniciar 8081`. |
| La primera vez se queda "pensando" | Está descargando Maven y las librerías (1 a 3 minutos). |
| Quiero empezar de cero | Pídeselo a Andy: borrar la base no se hace desde la app (nada se borra). |

**Importante:** nunca vuelvas a correr `Script_MySQL_RULA.sql` sobre una base que ya tiene datos, porque empieza borrando la base. Los scripts `iniciar` ya lo evitan.
