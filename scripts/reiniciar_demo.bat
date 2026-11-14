@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
call mvnw.cmd -q compile exec:java -Dexec.args=--reiniciar-demo
if errorlevel 1 exit /b 1
