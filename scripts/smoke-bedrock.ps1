# Sapientia Bedrock smoke test (T-210 / 1.0.0) — PowerShell variant.
[CmdletBinding()]
param(
    [string]$Work = (Join-Path $PSScriptRoot '..\build\smoke-bedrock'),
    [string]$PaperVersion = '1.20.4'
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Write-Host "[smoke] Workspace: $Work"
New-Item -ItemType Directory -Force -Path (Join-Path $Work 'plugins') | Out-Null
Set-Location $Work

if (-not (Test-Path 'paper.jar')) {
    Write-Host "[smoke] Paper $PaperVersion not present — drop paper-$PaperVersion.jar (or symlink to paper.jar) into $Work before re-running."
    exit 1
}
if (-not (Test-Path 'plugins\floodgate.jar')) {
    Write-Host "[smoke] floodgate.jar missing. Download Floodgate-Spigot from GeyserMC and place it under plugins\."
    exit 1
}
if (-not (Test-Path 'plugins\Geyser-Spigot.jar')) {
    Write-Host "[smoke] Geyser-Spigot.jar missing. Download it and place it under plugins\."
    exit 1
}

Write-Host '[smoke] Building Sapientia plugin jar...'
Push-Location $root
& .\gradlew.bat --quiet :sapientia-core:buildPluginJar
Pop-Location

$jar = Get-ChildItem -Path (Join-Path $root 'sapientia-core\build\libs') -Filter 'sapientia-core-*.jar' |
        Where-Object { $_.Name -notmatch 'sources' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) {
    Write-Host '[smoke] Could not locate built Sapientia jar.'
    exit 1
}
Copy-Item $jar.FullName 'plugins\Sapientia.jar' -Force
Write-Host "[smoke] Sapientia jar copied: $($jar.Name)"

'eula=true' | Set-Content -Path 'eula.txt'

Write-Host '[smoke] Starting Paper. Connect with a Bedrock client to localhost:19132 and follow docs/bedrock-smoke-checklist.md.'
& java '-Xms2G' '-Xmx2G' '-jar' 'paper.jar' 'nogui'
