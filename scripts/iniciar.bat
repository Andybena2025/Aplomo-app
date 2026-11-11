@echo off
chcp 65001 >nul
REM APLOMO. - Prepara la base (solo la primera vez) y arranca la aplicacion.
REM Uso: doble clic, o en la carpeta del proyecto:  scripts\iniciar.bat   (o  scripts\iniciar.bat 8081)
setlocal
cd /d "%~dp0.."
set PUERTO=%1
if "%PUERTO%"=="" set PUERTO=8080

echo.
echo APLOMO. - Iniciar
echo -----------------

java -version >nul 2>nul
if errorlevel 1 (
  echo Falta Java 17 o superior. Corre primero scripts\instalar_windows.bat
  pause & exit /b 1
)

set MYSQL=mysql
where mysql >nul 2>nul
if errorlevel 1 (
  set MYSQL=
  if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
  if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)
if "%MYSQL%"=="" (
  echo Falta MySQL. Corre primero scripts\instalar_windows.bat
  pause & exit /b 1
)

if not exist config\db.properties (
  copy config\db.properties.ejemplo config\db.properties >nul
  echo Cree config\db.properties con los datos de ejemplo.
)

set /p MYSQL_PWD=Contrasena del usuario root de MySQL: 
"%MYSQL%" -u root -e "SELECT 1" >nul 2>nul
if errorlevel 1 (
  echo No pude entrar a MySQL. Revisa la contrasena o enciende el servicio "MySQL" en Servicios de Windows.
  pause & exit /b 1
)

"%MYSQL%" -u root -N -e "SHOW DATABASES" | findstr /x /c:"rula_ergonomia" >nul
if errorlevel 1 (
  echo Creando la base rula_ergonomia (primera vez^)...
  "%MYSQL%" -u root < docs\base_de_datos\Script_MySQL_RULA.sql
  echo Base creada.
) else (
  echo La base rula_ergonomia ya existe: no la toco.
)
"%MYSQL%" -u root rula_ergonomia < docs\base_de_datos\datos_sugerencias.sql
set MYSQL_PWD=

echo.
echo Abre en este computador:  http://localhost:%PUERTO%
echo Para apagar la app: Control + C
echo La primera vez descarga Maven y las librerias (1 a 3 minutos^).
echo.
call mvnw.cmd -q compile exec:java -Daplomo.puerto=%PUERTO%
pause
