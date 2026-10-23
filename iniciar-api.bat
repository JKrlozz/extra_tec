@echo off
rem Enciende la API. Dejar esta ventana abierta mientras se use la aplicacion.
title API extra_tec
cd /d "%~dp0api"
if not exist ".env" (
    echo Falta api\.env. Ejecuta primero instalar.bat
    pause
    exit /b 1
)
if not exist "node_modules" (
    echo Instalando dependencias...
    call npm install --no-audit --no-fund
)
call npm start
pause
