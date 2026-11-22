#!/usr/bin/env bash
# Paso 38B: respaldo primero; migración aditiva en la base real. Nada se borra.
set -euo pipefail
cd "$(dirname "$0")/.."
db_nombre=${1:-rula_ergonomia}
case "$db_nombre" in
  rula_ergonomia|rula_ergonomia_pruebas) ;;
  *) echo "Usa rula_ergonomia o rula_ergonomia_pruebas."; exit 1 ;;
esac
command -v mysql >/dev/null || { echo "Falta mysql en la ruta de comandos."; exit 1; }
command -v mysqldump >/dev/null || { echo "Falta mysqldump en la ruta de comandos."; exit 1; }
read -r -p "Usuario administrador de MySQL (Enter = root): " db_admin
db_admin=${db_admin:-root}
read -r -s -p "Contraseña de MySQL (no se muestra): " db_clave; echo
export MYSQL_PWD="$db_clave"
trap 'unset MYSQL_PWD db_clave' EXIT
umask 077
mkdir -p config/respaldos
respaldo="config/respaldos/antes_paso38_${db_nombre}_$(date +%Y%m%d_%H%M%S)_${RANDOM}.sql"
mysqldump -u "$db_admin" --single-transaction --set-gtid-purged=OFF "$db_nombre" > "$respaldo"
mysql -u "$db_admin" "$db_nombre" < docs/base_de_datos/migracion_paso38.sql
echo "Base actualizada. Respaldo privado: $respaldo"
