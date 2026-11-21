@echo off
chcp 65001 >nul
setlocal
rem Paso 17: datos completos calculados por el motor RULA.
cd /d "%~dp0.."
set "FECHA=%~1"
if "%FECHA%"=="" set "FECHA=2026-11-27"
call mvnw.cmd -q compile exec:java "-Dexec.args=--cargar-demo %FECHA%"
if errorlevel 1 exit /b 1
