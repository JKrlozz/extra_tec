@echo off
rem Instala la base de datos extra_tec y deja lista la API. Doble clic o ejecutar desde cmd.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "database\instalar.ps1" %*
echo.
pause
