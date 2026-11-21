#!/usr/bin/env bash
# Voluntario: usa la base configurada; nunca crea una base ni cambia la configuración.
set -euo pipefail
cd "$(dirname "$0")/.."
# Paso 17: incluye dos posturas por puesto, fotos y puntajes calculados por el motor.
export TZ=America/Bogota
./mvnw -q compile exec:java -Dexec.args="--cargar-demo ${1:-2026-11-27}"
