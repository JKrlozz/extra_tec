# Instala la base de datos extra_tec, crea el usuario de la API y genera api/.env.
# Uso (desde la carpeta extra_tec):  powershell -ExecutionPolicy Bypass -File database\instalar.ps1
# Pide la contrasena de root de MySQL; no se guarda en ningun archivo.
$ErrorActionPreference = 'Stop'
$raiz  = Split-Path -Parent $PSScriptRoot
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (-not (Test-Path $mysql)) { $mysql = 'mysql' }

# Clave aleatoria para el usuario de la API y secreto para los JWT
function Nuevo-Secreto($n) { -join ((48..57 + 65..90 + 97..122) | Get-Random -Count $n | ForEach-Object { [char]$_ }) }
$claveApp = Nuevo-Secreto 24
$jwt      = Nuevo-Secreto 48

# Contrasena de root: se pide una sola vez y solo vive en esta sesion
$seguro = Read-Host 'Contrasena de root de MySQL' -AsSecureString
$env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro))

$usuarioSql = Join-Path $env:TEMP 'extra_tec_usuario.sql'
@"
DROP USER IF EXISTS 'extra_tec_app'@'localhost';
CREATE USER 'extra_tec_app'@'localhost' IDENTIFIED BY '$claveApp';
GRANT SELECT, INSERT, UPDATE, DELETE ON extra_tec.* TO 'extra_tec_app'@'localhost';
FLUSH PRIVILEGES;
"@ | Set-Content -Path $usuarioSql -Encoding ASCII

# Ejecuta un .sql redirigiendo el archivo (conserva los acentos del seed)
function Ejecutar-Sql($archivo) {
    Write-Host "Ejecutando $(Split-Path -Leaf $archivo)..."
    cmd /c "`"$mysql`" -u root --default-character-set=utf8mb4 < `"$archivo`""
    if ($LASTEXITCODE -ne 0) { throw "MySQL devolvio un error en $archivo (revisa la contrasena de root)." }
}

Push-Location (Join-Path $raiz 'database')
try {
    Ejecutar-Sql 'schema.sql'
    Ejecutar-Sql 'seed.sql'
    Ejecutar-Sql $usuarioSql
} finally {
    Pop-Location
    Remove-Item $usuarioSql -ErrorAction SilentlyContinue
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

@"
PORT=3000
DB_HOST=localhost
DB_PORT=3306
DB_USER=extra_tec_app
DB_PASSWORD=$claveApp
DB_NAME=extra_tec
JWT_SECRET=$jwt
ASISTENCIA_MINIMA=80
"@ | Set-Content -Path (Join-Path $raiz 'api\.env') -Encoding ASCII

Push-Location (Join-Path $raiz 'api')
try { npm run demo-passwords } finally { Pop-Location }

Write-Host ''
Write-Host 'Listo. Inicia la API con:  cd api; npm start   (http://localhost:3000/api/salud)'
Write-Host 'Cuentas de prueba (clave itsm1234): admin@itsm.edu.mx, cperez@itsm.edu.mx, mhernandez@itsm.edu.mx'
