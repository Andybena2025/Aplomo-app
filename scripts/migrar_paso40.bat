@echo off
setlocal
cd /d "%~dp0.."
set "DB_NOMBRE=%~1"
if "%DB_NOMBRE%"=="" set "DB_NOMBRE=rula_ergonomia"
if "%DB_NOMBRE%"=="rula_ergonomia" goto base_valida
if "%DB_NOMBRE%"=="rula_ergonomia_pruebas" goto base_valida
echo Usa rula_ergonomia o rula_ergonomia_pruebas.
exit /b 1
:base_valida
rem Paso 40: los datos y claves quedan fuera de Git. La clave se pide por cada herramienta.
set /p "DB_ADMIN=Usuario administrador de MySQL (Enter = root): "
if "%DB_ADMIN%"=="" set DB_ADMIN=root
if not exist config\respaldos mkdir config\respaldos
set BACKUP=config\respaldos\antes_paso40_%DB_NOMBRE%_%RANDOM%_%RANDOM%.sql
mysqldump -u "%DB_ADMIN%" -p --single-transaction --set-gtid-purged=OFF "%DB_NOMBRE%" > "%BACKUP%"
if errorlevel 1 exit /b 1
mysql -u "%DB_ADMIN%" -p "%DB_NOMBRE%" < docs\base_de_datos\migracion_paso40.sql
if errorlevel 1 exit /b 1
echo Base actualizada. Respaldo privado: %BACKUP%
endlocal
