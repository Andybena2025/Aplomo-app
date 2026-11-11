#!/usr/bin/env bash
# APLOMO. · Prepara la base (solo la primera vez) y arranca la aplicación.
# Uso: en la carpeta del proyecto:  bash scripts/iniciar.sh        (puerto 8080)
#                                    bash scripts/iniciar.sh 8081   (otro puerto)
set -e
cd "$(dirname "$0")/.."
PUERTO="${1:-8080}"

echo ""
echo "APLOMO. · Iniciar"
echo "-----------------"

if ! java -version 2>&1 | grep -Eq 'version "(1[7-9]|2[0-9])'; then
  echo "Falta Java 17 o superior. Corre primero:  bash scripts/instalar_mac.sh"; exit 1
fi

MYSQL="$(command -v mysql || true)"
for d in /opt/homebrew/opt/mysql/bin /usr/local/opt/mysql/bin /opt/homebrew/bin /usr/local/mysql/bin; do
  [ -z "$MYSQL" ] && [ -x "$d/mysql" ] && MYSQL="$d/mysql"
done
if [ -z "$MYSQL" ]; then echo "Falta MySQL. Corre primero:  bash scripts/instalar_mac.sh"; exit 1; fi

# 1. Conexión de la app (nunca se sube a GitHub)
if [ ! -f config/db.properties ]; then
  cp config/db.properties.ejemplo config/db.properties
  echo "✓ Creé config/db.properties con los datos de ejemplo."
fi

# 2. Base de datos: solo se crea si NO existe (el script inicial borra la base, por eso nunca se repite)
echo "Contraseña del usuario root de MySQL (si nunca le pusiste una, solo presiona Enter):"
read -r -s CLAVE_ROOT; echo ""
export MYSQL_PWD="$CLAVE_ROOT"
if ! "$MYSQL" -u root -e "SELECT 1" >/dev/null 2>&1; then
  echo "No pude entrar a MySQL con esa contraseña, o MySQL está apagado."
  echo "En Mac enciéndelo con:  brew services start mysql"; exit 1
fi
if [ -z "$("$MYSQL" -u root -N -e "SHOW DATABASES LIKE 'rula_ergonomia'")" ]; then
  echo "→ Creando la base rula_ergonomia (primera vez)..."
  "$MYSQL" -u root < docs/base_de_datos/Script_MySQL_RULA.sql
  echo "✓ Base creada con las tablas RULA y la cuenta del Administrador."
else
  echo "✓ La base rula_ergonomia ya existe: no la toco."
fi
# Reglas de sugerencias: el archivo no duplica ni borra nada, se puede correr siempre.
"$MYSQL" -u root rula_ergonomia < docs/base_de_datos/datos_sugerencias.sql
unset MYSQL_PWD CLAVE_ROOT

# 3. Arrancar
IP="$(ipconfig getifaddr en0 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}' || true)"
echo ""
echo "Abre en este computador:   http://localhost:$PUERTO"
[ -n "$IP" ] && echo "Desde el celular o iPad (mismo Wi-Fi):  http://$IP:$PUERTO"
echo "Para apagar la app: Control + C"
echo "La primera vez descarga Maven y las librerías (1 a 3 minutos)."
echo ""
chmod +x ./mvnw
./mvnw -q compile exec:java -Daplomo.puerto="$PUERTO"
