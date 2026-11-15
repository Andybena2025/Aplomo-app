@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
call mvnw.cmd -q -DskipTests compile org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/cuentas-classpath.txt
if errorlevel 1 exit /b 1
set /p CUENTAS_CP=<target\cuentas-classpath.txt
java -Duser.timezone=America/Bogota -cp "target\classes;%CUENTAS_CP%" co.aplomo.instalacion.CrearCuentas
exit /b %errorlevel%
