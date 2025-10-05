#!/usr/bin/env pwsh
# PoorCraft Refactor - Build and Run Script
# This script provides convenient commands for building and running the project

param(
    [Parameter(Position=0)]
    [ValidateSet('run', 'build', 'test', 'native', 'clean', 'dev', 'portable', 'headless', 'atlas', 'help')]
    [string]$Command = 'help',
    
    [Parameter(Position=1)]
    [string]$CustomArgs = ''
)

$ErrorActionPreference = "Stop"

$ScriptRoot = Split-Path -Path $MyInvocation.MyCommand.Path -Parent
$GradleWrapperBat = Join-Path $ScriptRoot "gradlew.bat"
$GradleWrapperSh = Join-Path $ScriptRoot "gradlew"

function Invoke-GradleCommand {
    param(
        [Parameter(Mandatory = $false)]
        [string[]] $Arguments = @()
    )

    $wrapper = if (Test-Path $GradleWrapperBat) {
        $GradleWrapperBat
    } elseif (Test-Path $GradleWrapperSh) {
        $GradleWrapperSh
    } else {
        throw "Gradle wrapper not found in $ScriptRoot"
    }

    Push-Location $ScriptRoot
    try {
        & $wrapper @Arguments
        return $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host " $Text" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Text)
    Write-Host "✓ $Text" -ForegroundColor Green
}

function Write-Info {
    param([string]$Text)
    Write-Host "→ $Text" -ForegroundColor Yellow
}

function Write-Error-Message {
    param([string]$Text)
    Write-Host "✗ $Text" -ForegroundColor Red
}

function Test-Java {
    try {
        $javaVersion = java -version 2>&1 | Select-Object -First 1
        Write-Success "Java found: $javaVersion"
        return $true
    } catch {
        Write-Error-Message "Java not found. Please install JDK 17 or later."
        Write-Info "Download from: https://adoptium.net/"
        return $false
    }
}

function Test-GraalVM {
    try {
        $graalVersion = java -version 2>&1 | Select-String "GraalVM"
        if ($graalVersion) {
            Write-Success "GraalVM detected"
            return $true
        } else {
            Write-Info "GraalVM not detected (required for native builds)"
            return $false
        }
    } catch {
        return $false
    }
}

function Show-Help {
    Write-Header "PoorCraft Refactor - Build Script"
    
    Write-Host "USAGE:" -ForegroundColor Yellow
    Write-Host "  .\build-and-run.ps1 <command> [args]"
    Write-Host ""
    
    Write-Host "COMMANDS:" -ForegroundColor Yellow
    Write-Host "  run        - Build and run from source"
    Write-Host "  build      - Build all modules"
    Write-Host "  test       - Run all tests"
    Write-Host "  native     - Build native Windows EXE (requires GraalVM)"
    Write-Host "  clean      - Clean build artifacts"
    Write-Host "  dev        - Run in development mode (live reload)"
    Write-Host "  portable   - Run in portable mode"
    Write-Host "  headless   - Run in headless mode (testing)"
    Write-Host "  atlas      - Build texture atlas"
    Write-Host "  help       - Show this help message"
    Write-Host ""
    
    Write-Host "EXAMPLES:" -ForegroundColor Yellow
    Write-Host "  .\build-and-run.ps1 run"
    Write-Host "  .\build-and-run.ps1 build"
    Write-Host "  .\build-and-run.ps1 test"
    Write-Host "  .\build-and-run.ps1 native"
    Write-Host "  .\build-and-run.ps1 dev"
    Write-Host ""
}

function Invoke-Run {
    Write-Header "Running PoorCraft Refactor"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    Write-Info "Building and running..."
    $exitCode = Invoke-GradleCommand @(':launcher:run')
    
    if ($exitCode -eq 0) {
        Write-Success "Execution completed"
    } else {
        Write-Error-Message "Execution failed"
        exit $exitCode
    }
}

function Invoke-Build {
    Write-Header "Building PoorCraft Refactor"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    Write-Info "Building all modules..."
    $exitCode = Invoke-GradleCommand @('build')
    
    if ($exitCode -eq 0) {
        Write-Success "Build completed successfully"
        Write-Info "Artifacts:"
        Write-Host "  - engine/build/libs/engine-0.1.2.jar"
        Write-Host "  - launcher/build/libs/launcher-0.1.2.jar"
    } else {
        Write-Error-Message "Build failed"
        exit $exitCode
    }
}

function Invoke-Test {
    Write-Header "Running Tests"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    Write-Info "Running all tests..."
    $exitCode = Invoke-GradleCommand @('test')
    
    if ($exitCode -eq 0) {
        Write-Success "All tests passed"
        Write-Info "Test reports: build/reports/tests/test/index.html"
    } else {
        Write-Error-Message "Tests failed"
        Write-Info "Check test reports for details"
        exit $exitCode
    }
}

function Invoke-Native {
    Write-Header "Building Native Image"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    if (-not (Test-GraalVM)) {
        Write-Error-Message "GraalVM is required for native image builds"
        Write-Info "Download from: https://www.graalvm.org/downloads/"
        Write-Info "Install native-image: gu install native-image"
        exit 1
    }
    
    Write-Info "Building native Windows EXE..."
    Write-Info "This may take 5-10 minutes..."
    $exitCode = Invoke-GradleCommand @(':launcher:nativeCompile')
    
    if ($exitCode -eq 0) {
        Write-Success "Native image built successfully"
        Write-Info "Executable: launcher/build/native/nativeCompile/PoorCraftRefactor.exe"
        
        $exePath = "launcher\build\native\nativeCompile\PoorCraftRefactor.exe"
        if (Test-Path $exePath) {
            $size = (Get-Item $exePath).Length / 1MB
            Write-Info "Size: $([math]::Round($size, 2)) MB"
        }
    } else {
        Write-Error-Message "Native image build failed"
        exit $exitCode
    }
}

function Invoke-Clean {
    Write-Header "Cleaning Build Artifacts"
    
    Write-Info "Cleaning..."
    $exitCode = Invoke-GradleCommand @('clean')
    
    if ($exitCode -eq 0) {
        Write-Success "Clean completed"
    } else {
        Write-Error-Message "Clean failed"
        exit $exitCode
    }
}

function Invoke-Dev {
    Write-Header "Running in Development Mode"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    Write-Info "Starting in dev mode (live reload enabled)..."
    Invoke-GradleCommand @(':launcher:runDev') | Out-Null
}

function Invoke-Portable {
    Write-Header "Running in Portable Mode"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    Write-Info "Starting in portable mode..."
    Invoke-GradleCommand @(':launcher:runPortable') | Out-Null
}

function Invoke-Headless {
    Write-Header "Running in Headless Mode"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    Write-Info "Starting in headless mode (no window)..."
    Invoke-GradleCommand @(':launcher:runHeadless') | Out-Null
}

function Invoke-Atlas {
    Write-Header "Building Texture Atlas"
    
    if (-not (Test-Java)) {
        exit 1
    }
    
    $inputDir = if ($Args) { $Args } else { "skins/default" }
    
    Write-Info "Packing textures from: $inputDir"
    $exitCode = Invoke-GradleCommand @(':tools:atlas-packer:run', "--args=$inputDir")
    
    if ($exitCode -eq 0) {
        Write-Success "Atlas built successfully"
    } else {
        Write-Error-Message "Atlas build failed"
        exit $exitCode
    }
}

# Main execution
switch ($Command) {
    'run' { Invoke-Run }
    'build' { Invoke-Build }
    'test' { Invoke-Test }
    'native' { Invoke-Native }
    'clean' { Invoke-Clean }
    'dev' { Invoke-Dev }
    'portable' { Invoke-Portable }
    'headless' { Invoke-Headless }
    'atlas' { Invoke-Atlas }
    'help' { Show-Help }
    default { Show-Help }
}
