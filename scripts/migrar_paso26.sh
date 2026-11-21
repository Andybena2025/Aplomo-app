#!/usr/bin/env bash
# Paso 26: respaldo primero; migración aditiva en la base real. Nada se borra.
set -euo pipefail
cd "$(dirname "$0")/.."
command -v mysql >/dev/null || { echo "Falta mysql en la ruta de comandos."; exit 1; }
command -v mysqldump >/dev/null || { echo "Falta mysqldump en la ruta de comandos."; exit 1; }
read -r -p "Usuario administrador de MySQL (Enter = root): " db_admin
db_admin=${db_admin:-root}
read -r -s -p "Contraseña de MySQL (no se muestra): " db_clave; echo
export MYSQL_PWD="$db_clave"
trap 'unset MYSQL_PWD db_clave' EXIT
umask 077
mkdir -p config/respaldos
respaldo="config/respaldos/antes_paso26_$(date +%Y%m%d_%H%M%S).sql"
mysqldump -u "$db_admin" --single-transaction --set-gtid-purged=OFF rula_ergonomia > "$respaldo"
mysql -u "$db_admin" rula_ergonomia < docs/base_de_datos/migracion_paso26.sql
echo "Base actualizada. Respaldo privado: $respaldo"
