# Instala la base de datos extra_tec, crea el usuario de la API y genera api/.env.
# Uso desde la carpeta extra_tec:  powershell -ExecutionPolicy Bypass -File database\instalar.ps1
# Con -SoloComprobar solo revisa los requisitos y no toca la base.
# La contrasena de root se pide en pantalla y no se guarda en ningun archivo.
param([switch]$SoloComprobar)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

function Titulo($texto) {
    Write-Host ''
    Write-Host "== $texto ==" -ForegroundColor Cyan
}

function Fallo($texto) {
    Write-Host ''
    Write-Host "ERROR: $texto" -ForegroundColor Red
    exit 1
}

function Nuevo-Secreto($largo) {
    -join ((48..57 + 65..90 + 97..122) | Get-Random -Count $largo | ForEach-Object { [char]$_ })
}

# ---------- Requisitos ----------
Titulo 'Revisando requisitos'

# Busca mysql.exe en cualquier version instalada de MySQL Server
$candidatos = @(Get-ChildItem 'C:\Program Files\MySQL' -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    ForEach-Object { Join-Path $_.FullName 'bin\mysql.exe' } |
    Where-Object { Test-Path $_ })
if ($candidatos.Count -gt 0) { $mysql = $candidatos[0] }
else {
    $enRuta = Get-Command mysql -ErrorAction SilentlyContinue
    if ($enRuta) { $mysql = $enRuta.Source } else { $mysql = $null }
}
if (-not $mysql) {
    Fallo 'No se encontro MySQL. Instala MySQL Server 8.0 (https://dev.mysql.com/downloads/mysql/) y vuelve a ejecutar este instalador.'
}
Write-Host "MySQL:   $mysql"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Fallo 'No se encontro Node.js. Instalalo (version LTS) desde https://nodejs.org y vuelve a ejecutar este instalador.'
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Fallo 'No se encontro npm. Reinstala Node.js (version LTS) desde https://nodejs.org.'
}
Write-Host ("Node.js: " + (node --version))

$servicio = Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($servicio -and $servicio.Status -ne 'Running') {
    Fallo "El servicio $($servicio.Name) esta detenido. Abre PowerShell como administrador y ejecuta: Start-Service $($servicio.Name)"
}
if ($servicio) { Write-Host "Servicio: $($servicio.Name) ($($servicio.Status))" }

foreach ($archivo in 'schema.sql', 'seed.sql') {
    if (-not (Test-Path (Join-Path $raiz "database\$archivo"))) { Fallo "Falta database\$archivo. Descarga el proyecto completo." }
}
if (-not (Test-Path (Join-Path $raiz 'api\package.json'))) { Fallo 'Falta la carpeta api. Descarga el proyecto completo.' }

if ($SoloComprobar) {
    Write-Host ''
    Write-Host 'Requisitos correctos. No se modifico nada (modo -SoloComprobar).' -ForegroundColor Green
    exit 0
}

# ---------- Contrasena de root ----------
Titulo 'Conexion con MySQL'
$conectado = $false
for ($intento = 1; $intento -le 3 -and -not $conectado; $intento++) {
    $seguro = Read-Host 'Contrasena de root de MySQL' -AsSecureString
    $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro))
    cmd /c "`"$mysql`" -u root -e `"SELECT 1`" >nul 2>nul"
    if ($LASTEXITCODE -eq 0) { $conectado = $true }
    else { Write-Host "Contrasena incorrecta o MySQL no responde (intento $intento de 3)." -ForegroundColor Yellow }
}
if (-not $conectado) {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    Fallo 'No se pudo entrar a MySQL como root. Si olvidaste la contrasena, restablecela y vuelve a intentar.'
}
Write-Host 'Conexion correcta.' -ForegroundColor Green

# ---------- Aviso si la base ya existe ----------
$existe = cmd /c "`"$mysql`" -u root -N -B -e `"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='extra_tec'`" 2>nul"
if (($existe | Select-Object -First 1) -eq '1') {
    Write-Host ''
    Write-Host 'La base extra_tec YA EXISTE. Continuar la borra y vuelve a crearla con los datos de prueba.' -ForegroundColor Yellow
    $respuesta = Read-Host 'Escribe SI para continuar (cualquier otra cosa cancela)'
    if ($respuesta -ne 'SI') {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
        Write-Host 'Cancelado. No se modifico nada.'
        exit 0
    }
}

# ---------- Base de datos ----------
Titulo 'Creando la base de datos'
$claveApp = Nuevo-Secreto 24
$jwt = Nuevo-Secreto 48

$usuarioSql = Join-Path $env:TEMP 'extra_tec_usuario.sql'
@"
DROP USER IF EXISTS 'extra_tec_app'@'localhost';
CREATE USER 'extra_tec_app'@'localhost' IDENTIFIED BY '$claveApp';
GRANT SELECT, INSERT, UPDATE, DELETE ON extra_tec.* TO 'extra_tec_app'@'localhost';
FLUSH PRIVILEGES;
"@ | Set-Content -Path $usuarioSql -Encoding ASCII

# Ejecuta un .sql redirigiendo el archivo, para conservar los acentos del seed
function Ejecutar-Sql($archivo) {
    Write-Host "Ejecutando $(Split-Path -Leaf $archivo)..."
    cmd /c "`"$mysql`" -u root --default-character-set=utf8mb4 < `"$archivo`""
    if ($LASTEXITCODE -ne 0) { throw "MySQL devolvio un error en $archivo" }
}

Push-Location (Join-Path $raiz 'database')
try {
    Ejecutar-Sql 'schema.sql'
    Ejecutar-Sql 'seed.sql'
    Ejecutar-Sql $usuarioSql
} catch {
    Pop-Location
    Remove-Item $usuarioSql -ErrorAction SilentlyContinue
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    Fallo $_.Exception.Message
}
Pop-Location
Remove-Item $usuarioSql -ErrorAction SilentlyContinue
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue

# ---------- Configuracion de la API ----------
Titulo 'Configurando la API'
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
Write-Host 'Archivo api\.env creado.'

Push-Location (Join-Path $raiz 'api')
try {
    if (-not (Test-Path 'node_modules')) {
        Write-Host 'Instalando dependencias de la API (npm install)...'
        npm install --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'npm install fallo. Revisa tu conexion a internet.' }
    }
    npm run demo-passwords
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar las contrasenas de prueba.' }
} catch {
    Pop-Location
    Fallo $_.Exception.Message
}
Pop-Location

# ---------- Final ----------
Titulo 'Listo'
Write-Host 'La base de datos y la API quedaron instaladas.' -ForegroundColor Green
Write-Host ''
Write-Host 'Para encender la API:   doble clic en iniciar-api.bat   (o: cd api ; npm start)'
Write-Host 'Para comprobarla:       http://localhost:3000/api/salud'
Write-Host ''
Write-Host 'Cuentas de prueba (contrasena itsm1234):'
Write-Host '  admin@itsm.edu.mx       (admin)'
Write-Host '  cperez@itsm.edu.mx      (docente)'
Write-Host '  mhernandez@itsm.edu.mx  (alumno)'
