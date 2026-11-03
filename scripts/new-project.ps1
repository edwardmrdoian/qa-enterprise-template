param(
    [Parameter(Mandatory = $true)][string]$Package,
    [Parameter(Mandatory = $true)][string]$ArtifactId,
    [string]$DisplayName = $ArtifactId
)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & java scripts\NewProject.java $Package $ArtifactId $DisplayName
    if ($LASTEXITCODE -ne 0) { throw "Project initialization failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}
