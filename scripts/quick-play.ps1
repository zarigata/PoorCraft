param(
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"
$script:RootDir = (Split-Path -Path $PSScriptRoot -Parent)
Set-Location $script:RootDir

function Write-Info($Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-Success($Message) {
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-ErrorMsg($Message) {
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

try {
    Write-Info "========================================"
    Write-Info "      PoorCraft - Quick Play"
    Write-Info "========================================"

    Write-Info "Checking Java..."
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "Java not found in PATH. Install Java 17 or later."
    }
    Write-Success "Java detected."

    $mavenArgs = @("clean", "package", "-DskipTests")
    if ($Verbose) { $mavenArgs += "-X" }
    Write-Info "Building PoorCraft (skip tests)..."
    & mvn @mavenArgs
    Write-Success "Build complete."

    $jarPath = Join-Path $script:RootDir "target\PoorCraft.jar"
    if (-not (Test-Path $jarPath)) {
        throw "target\\PoorCraft.jar not found."
    }

    Write-Info "Launching PoorCraft..."
    & java -jar $jarPath

    Write-Success "Enjoy PoorCraft!"
} catch {
    Write-ErrorMsg $_
    if ($Verbose) {
        Write-ErrorMsg $_.ScriptStackTrace
    }
    Write-Info "Press Enter to exit."
    [void][System.Console]::ReadLine()
    exit 1
}
