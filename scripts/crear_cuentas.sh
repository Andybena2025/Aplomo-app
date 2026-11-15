#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export TZ=America/Bogota
./mvnw -q -DskipTests compile org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/cuentas-classpath.txt
CUENTAS_CP="$(cat target/cuentas-classpath.txt)"
exec java -Duser.timezone=America/Bogota -cp "target/classes:$CUENTAS_CP" co.aplomo.instalacion.CrearCuentas
