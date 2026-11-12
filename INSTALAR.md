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

## Pruebas (opcional)

Las pruebas usan una base aparte, **`rula_ergonomia_pruebas`**, para no ensuciar la real.

```bash
bash scripts/crear_base_pruebas.sh   # Mac; en Windows usa "Git Bash" con el mismo comando
./mvnw test                           # Windows: mvnw.cmd test
```

Deben aprobar **715 pruebas**.

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
