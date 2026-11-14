# Instalar y correr APLOMO. en tu computador

Guía para el equipo. La primera vez toma unos **15 minutos**; después, arrancar la app toma segundos.

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

## Datos de demostración (opcional y manual)

`docs/base_de_datos/datos_demo.sql` **no se carga al iniciar ni instalar la app**. Requiere una cuenta de Administrador activa. No crea usuarios ni contraseñas; vincula los registros a un Administrador existente. Solo inserta lo que falta: repetirlo no duplica filas ni cambia los estados ya editados.

Incluye Ensambles del Quindío, Muebles Cafeteros y la Corporación Universitaria Empresarial Alexander von Humboldt con Sede principal, Sede Alcázar y Sede Anova (Medicina). Los nombres y direcciones proceden del mock; no se verificó su vigencia. Puestos, contactos y resultados son ficticios y se identifican como demostración.

Hay seis reservas, dos visitas Finalizadas, cuatro evaluaciones (dos Verdes de puntaje 1; una Amarilla de puntaje 3; una Roja de puntaje 7), sus mediciones manuales, un pendiente abierto y sugerencias. Los resultados corresponden a los casos 4, 2 y 8 del plan de pruebas, comprobados con el motor RULA existente. **No son una valoración de las personas o lugares mostrados.**

Desde la raíz del proyecto, copia la imagen de muestra generada con IA para los cuatro registros de demostración. Se usa la misma imagen ilustrativa; los ángulos provienen de los casos manuales, no de medir esa imagen.

Mac:

```bash
mkdir -p fotos
cp -n src/main/resources/static/img/muestra_perfil.jpg fotos/de100001-493b-4bc9-a64e-d51b4ea3c901.jpg
cp -n src/main/resources/static/img/muestra_perfil.jpg fotos/de100002-493b-4bc9-a64e-d51b4ea3c902.jpg
cp -n src/main/resources/static/img/muestra_perfil.jpg fotos/de100003-493b-4bc9-a64e-d51b4ea3c903.jpg
cp -n src/main/resources/static/img/muestra_perfil.jpg fotos/de100004-493b-4bc9-a64e-d51b4ea3c904.jpg
```

Windows (PowerShell):

```powershell
New-Item -ItemType Directory -Force fotos | Out-Null
foreach ($archivoDemo in @('de100001-493b-4bc9-a64e-d51b4ea3c901.jpg','de100002-493b-4bc9-a64e-d51b4ea3c902.jpg','de100003-493b-4bc9-a64e-d51b4ea3c903.jpg','de100004-493b-4bc9-a64e-d51b4ea3c904.jpg')) {
    if (-not (Test-Path (Join-Path fotos $archivoDemo))) {
        Copy-Item src/main/resources/static/img/muestra_perfil.jpg (Join-Path fotos $archivoDemo)
    }
}
```

Carga primero en la base de pruebas. En Mac o en **Símbolo del sistema** de Windows (no PowerShell), ejecuta:

```bash
mysql --default-character-set=utf8mb4 -u root -p rula_ergonomia_pruebas < docs/base_de_datos/datos_demo.sql
```

Escribe la contraseña de MySQL cuando la pida. Para cargar voluntariamente estos datos en la base de la app, sustituye el nombre por `rula_ergonomia`. No vuelvas a ejecutar el script de creación de la base para cargar la demostración. Las fotos permanecen en la carpeta ignorada por Git y solo el Administrador puede verlas.

La demostración aparece en las listas de Empresas y Reservas. En la universidad, Sede principal tiene la visita Finalizada y los tres colores; Alcázar tiene la segunda visita Finalizada y un puesto Verde (caso manual 4). Hay calificación general con cobertura «2 de 3 sedes calificadas · falta Sede Anova (Medicina)». El SQL establece `SET NAMES utf8mb4` antes de iniciar la transacción para conservar tildes.

---

## Pruebas (opcional)

Las pruebas usan una base aparte, **`rula_ergonomia_pruebas`**, para no ensuciar la real.

```bash
bash scripts/crear_base_pruebas.sh   # Mac; en Windows usa "Git Bash" con el mismo comando
./mvnw test                           # Windows: mvnw.cmd test
```

Deben aprobar **799 pruebas**.

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
