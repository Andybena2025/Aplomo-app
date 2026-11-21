@echo off
setlocal
cd /d "%~dp0.."
rem Paso 24: los datos y claves quedan fuera de Git. La clave se pide por cada herramienta.
set /p DB_ADMIN=Usuario administrador de MySQL (Enter = root): 
if "%DB_ADMIN%"=="" set DB_ADMIN=root
if not exist config\respaldos mkdir config\respaldos
set BACKUP=config\respaldos\antes_paso24_%RANDOM%_%RANDOM%.sql
mysqldump -u "%DB_ADMIN%" -p --single-transaction --set-gtid-purged=OFF rula_ergonomia > "%BACKUP%"
if errorlevel 1 exit /b 1
mysql -u "%DB_ADMIN%" -p rula_ergonomia < docs\base_de_datos\migracion_paso24.sql
if errorlevel 1 exit /b 1
echo Base actualizada. Respaldo privado: %BACKUP%
endlocal
