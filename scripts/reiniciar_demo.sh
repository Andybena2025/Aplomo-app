#!/usr/bin/env bash
# Voluntario: usa la base configurada; nunca crea una base ni cambia la configuración.
set -euo pipefail
cd "$(dirname "$0")/.."
export TZ=America/Bogota
./mvnw -q compile exec:java -Dexec.args=--reiniciar-demo
