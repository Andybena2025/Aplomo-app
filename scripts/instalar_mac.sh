#!/usr/bin/env bash
# APLOMO. · Instala lo que el computador necesita (solo una vez): Java 21 y MySQL.
# Uso: abre Terminal en la carpeta del proyecto y escribe:  bash scripts/instalar_mac.sh
set -e

echo ""
echo "APLOMO. · Instalación en Mac"
echo "----------------------------"

if ! command -v brew >/dev/null 2>&1; then
  echo "Falta Homebrew (el instalador de programas para Mac)."
  echo "Pega esta línea en Terminal, sigue sus instrucciones y vuelve a correr este script:"
  echo ""
  echo '  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
  echo ""
  exit 1
fi

if java -version 2>&1 | grep -Eq 'version "(1[7-9]|2[0-9])'; then
  echo "✓ Java ya está instalado."
else
  echo "→ Instalando Java 21 (te puede pedir la contraseña del Mac)..."
  brew install --cask temurin@21
fi

if command -v mysql >/dev/null 2>&1; then
  echo "✓ MySQL ya está instalado."
else
  echo "→ Instalando MySQL..."
  brew install mysql
fi

echo "→ Encendiendo MySQL (queda encendido aunque reinicies el Mac)..."
brew services start mysql >/dev/null 2>&1 || true

echo ""
echo "Listo. Ahora corre:  bash scripts/iniciar.sh"
echo "(Maven no hace falta instalarlo: el proyecto trae ./mvnw y descarga lo demás solo.)"
