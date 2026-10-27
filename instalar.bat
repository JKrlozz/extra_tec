@echo off
rem instala la base de datos y deja lista la api; doble clic o desde cmd
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "database\instalar.ps1" %*
echo.
pause
