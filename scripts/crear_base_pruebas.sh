#!/usr/bin/env bash
# APLOMO. · Crea la base aparte para las pruebas (rula_ergonomia_pruebas). Solo hace falta para correr ./mvnw test.
# No toca la base real. Si la base de pruebas ya existe, la vuelve a crear desde cero.
set -e
cd "$(dirname "$0")/.."
MYSQL="$(command -v mysql || true)"
for d in /opt/homebrew/opt/mysql/bin /usr/local/opt/mysql/bin /usr/local/mysql/bin; do
  [ -z "$MYSQL" ] && [ -x "$d/mysql" ] && MYSQL="$d/mysql"
done
[ -z "$MYSQL" ] && { echo "Falta MySQL."; exit 1; }
echo "Contraseña del usuario root de MySQL (Enter si no tiene):"
read -r -s CLAVE; echo ""; export MYSQL_PWD="$CLAVE"
sed 's/rula_ergonomia/rula_ergonomia_pruebas/g' docs/base_de_datos/Script_MySQL_RULA.sql | "$MYSQL" -u root
unset MYSQL_PWD CLAVE
echo "✓ Base rula_ergonomia_pruebas lista. Ahora:  ./mvnw test"
