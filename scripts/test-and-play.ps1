param(
    [switch]$SkipTests,
    [switch]$TestOnly,
    [switch]$Verbose,
    [switch]$OpenReports
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

function Write-Warn($Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-ErrorMsg($Message) {
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Invoke-Step($Title, [ScriptBlock]$Action) {
    Write-Info $Title
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & $Action
        $stopwatch.Stop()
        Write-Success "$Title completed in $($stopwatch.Elapsed.ToString())"
    } catch {
        $stopwatch.Stop()
        Write-ErrorMsg "$Title failed after $($stopwatch.Elapsed.ToString())"
        throw
    }
}

function Test-Prerequisites {
    Write-Info "Checking prerequisites..."
    foreach ($cmd in @("java", "mvn")) {
        if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
            Write-ErrorMsg "$cmd not found in PATH."
            throw "Missing prerequisite: $cmd"
        }
        Write-Success "$cmd detected."
    }
}

function Invoke-Build {
    $mavenBuildArgs = @("clean", "package", "-DskipTests")
    if ($Verbose) { $mavenBuildArgs += "-X" }
    & mvn @mavenBuildArgs
}

function Invoke-Tests {
    $mavenTestArgs = @("test")
    if ($Verbose) { $mavenTestArgs += "-X" }
    & mvn @mavenTestArgs
    if ($OpenReports) {
        $reportPath = Join-Path $script:RootDir "target\surefire-reports"
        if (Test-Path $reportPath) {
            Write-Info "Opening test reports in file explorer."
            Start-Process explorer.exe $reportPath
        } else {
            Write-Warn "Test reports not found at $reportPath"
        }
    }
}

function Start-Game {
    $jarPath = Join-Path $script:RootDir "target\PoorCraft.jar"
    if (-not (Test-Path $jarPath)) {
        throw "Game JAR not found: $jarPath"
    }
    Write-Info "Launching PoorCraft..."
    & java -jar $jarPath
}

try {
    Write-Info "========================================"
    Write-Info "      PoorCraft - Test & Play"
    Write-Info "========================================"

    Test-Prerequisites

    Invoke-Step "Step 1/3: Build" { Invoke-Build }

    if (-not $SkipTests) {
        Invoke-Step "Step 2/3: Tests" { Invoke-Tests }
    } else {
        Write-Warn "Tests skipped (--SkipTests)."
    }

    if (-not $TestOnly) {
        Invoke-Step "Step 3/3: Launch" { Start-Game }
    } else {
        Write-Info "Test-only mode complete. Game launch skipped."
    }

    Write-Success "All steps finished. Enjoy PoorCraft!"
} catch {
    Write-ErrorMsg $_
    if ($Verbose) {
        Write-ErrorMsg $_.ScriptStackTrace
    }
    Write-Warn "Script failed. Press Enter to exit."
    [void][System.Console]::ReadLine()
    exit 1
}
