$ErrorActionPreference = 'Stop'

$esAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $esAdmin) {
    Write-Host 'Abre PowerShell como administrador y vuelve a ejecutar este script.'
    exit 1
}

$nombre = 'ExtraTec API (red local)'
Get-NetFirewallRule -DisplayName $nombre -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $nombre -Direction Inbound -Protocol TCP -LocalPort 3000 -Action Allow -Profile Any | Out-Null

Write-Host "Regla creada: $nombre (TCP 3000, entrada)."
Write-Host 'Para quitarla: Remove-NetFirewallRule -DisplayName "ExtraTec API (red local)"'
