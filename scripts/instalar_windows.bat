@echo off
chcp 65001 >nul
REM APLOMO. - Instala Java 21 (solo una vez) y revisa si MySQL ya está.
REM Uso: doble clic en este archivo, o en la carpeta del proyecto:  scripts\instalar_windows.bat

echo.
echo APLOMO. - Instalacion en Windows
echo --------------------------------

where winget >nul 2>nul
if errorlevel 1 (
  echo Falta "winget". Actualiza "Instalador de aplicacion" desde la Microsoft Store y vuelve a intentarlo.
  echo O instala Java 21 a mano desde https://adoptium.net/es/temurin/releases/?version=21
  pause
  exit /b 1
)

java -version >nul 2>nul
if errorlevel 1 (
  echo Instalando Java 21...
  winget install -e --id EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
) else (
  echo Java ya esta instalado.
)

where mysql >nul 2>nul
if errorlevel 1 (
  if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    echo MySQL ya esta instalado.
  ) else if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    echo MySQL ya esta instalado.
  ) else (
    echo.
    echo FALTA MySQL. Instalalo a mano una sola vez (son 5 minutos^):
    echo   1. Abre https://dev.mysql.com/downloads/installer/ y descarga "MySQL Installer for Windows".
    echo   2. Elige "Server only". Deja el puerto 3306.
    echo   3. Escribe una contrasena para el usuario root y GUARDALA.
    echo   4. Deja marcado "Configure MySQL Server as a Windows Service".
    echo   5. Termina la instalacion y vuelve a correr este archivo.
    start https://dev.mysql.com/downloads/installer/
  )
) else (
  echo MySQL ya esta instalado.
)

echo.
echo Cierra esta ventana y abre una nueva para que Windows reconozca Java.
echo Luego corre:  scripts\iniciar.bat
pause
