param([ValidateSet("entra","demo")][string]$Mode="entra")
$ErrorActionPreference="Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!(Test-Path .env)) { Copy-Item .env.example .env }
$compose=@("compose","-f","compose.yml")
if ($Mode -eq "demo") { $compose+=@("-f","compose.demo.yml") }
& docker @compose config --quiet
if ($LASTEXITCODE -ne 0) { throw "Configuración Compose inválida" }
& docker @compose up -d --build --wait --wait-timeout 420
if ($LASTEXITCODE -ne 0) { throw "Revisa docker compose ps y docker compose logs" }
Write-Host "BarrioDigital: http://localhost:4200"
