# APLOMO con Docker

Abre Docker Desktop. Trabaja desde la raíz de este repositorio. Prepara un archivo local `.env` usando `.env.ejemplo` como guía, con `MYSQL_ROOT_PASSWORD` y `APLOMO_DB_CLAVE`. Ese archivo se conserva solo en tu computador; no entra a Git ni a la imagen.

Antes de levantar Docker, detén la app que corre desde IntelliJ o la terminal: ambas usan el puerto 8080. MySQL local puede seguir encendido; Docker usa su propio MySQL y no publica su puerto 3306.

## Los tres comandos

```bash
docker compose up -d --build          # Construir y levantar
docker compose logs -f --tail=100     # Ver los registros; Ctrl+C deja la app encendida
docker compose down                  # Apagar conservando base y fotos
```

Espera a que MySQL termine de prepararse y abre [Iniciar sesión](http://localhost:8080/login). El primer arranque crea las 21 tablas y sus datos semilla. El acceso inicial y el cambio obligatorio de contraseña están explicados en [INSTALAR.md](../INSTALAR.md).

## Si el puerto está ocupado

Si aparece «port is already allocated» o «address already in use», cierra la app local desde IntelliJ o la terminal. Si la ocupa otra copia de APLOMO en Docker, apágala desde Docker Desktop. Repite el comando para levantar. No necesitas reinstalar MySQL ni borrar bases o volúmenes.

## Dónde quedan los datos

Compose conserva MySQL en `datos_mysql` y las fotografías en `fotos_aplomo`. Los nombres completos incluyen el nombre del proyecto de Compose. Levanta siempre desde la misma carpeta y con el mismo nombre de proyecto para reutilizarlos.

`docker compose down` conserva esos volúmenes. No añadas `-v` ni uses opciones para borrar volúmenes. El SQL de arranque solo se ejecuta al preparar un volumen nuevo; no lo ejecutes manualmente sobre una base que ya tenga información.

Para volver a la app normal, apaga Docker con el tercer comando y sigue el arranque local de [INSTALAR.md](../INSTALAR.md). Sus datos y su configuración son independientes.

## Variables para desplegar la imagen

- `APLOMO_DB_URL`: conexión JDBC de MySQL del despliegue.
- `APLOMO_DB_USUARIO` y `APLOMO_DB_CLAVE`: cuenta de esa base.
- `APLOMO_CARPETA_FOTOS`: carpeta persistente; en Compose es `/app/fotos`.
- Puerto: primero `PORT` si la plataforma lo proporciona; si no existe, `APLOMO_PUERTO`; sin ambos, 8080. El puerto debe estar entre 1 y 65535.

La imagen se ejecuta con el usuario `aplomo`, sin permisos de administrador. Compose entrega las claves al arrancar; no las guarda dentro de la imagen. La cuenta de base de datos de la aplicación solo tiene SELECT, INSERT y UPDATE.
