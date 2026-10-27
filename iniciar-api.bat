@echo off
rem enciende la api; dejar abierta la ventana mientras se use la app
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
