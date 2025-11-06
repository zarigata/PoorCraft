@echo off
setlocal EnableDelayedExpansion

rem ==============================================================
rem  PoorCraft Unified Test & Run Script
rem  Usage:
rem    unified-test-and-run.bat --mode <dev|prod> [--quick-tests] [--skip-tests] [--test-only]
rem ==============================================================

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
pushd "%ROOT_DIR%" >nul

set "MODE=dev"
set "RUN_TESTS=1"
set "RUN_FULL_SUITE=1"
set "TEST_ONLY=0"
set "SKIP_BUILD=0"

for /f %%i in ('echo prompt $E^| cmd') do set "ESC=%%i"
set "COLOR_INFO=%ESC%[96m"
set "COLOR_OK=%ESC%[92m"
set "COLOR_WARN=%ESC%[93m"
set "COLOR_ERROR=%ESC%[91m"
set "COLOR_RESET=%ESC%[0m"

:parse_args
if "%~1"=="" goto after_args
if /I "%~1"=="--mode" (
    shift
    if "%~1"=="" goto show_help
    set "MODE=%~1"
    if /I not "%MODE%"=="dev" if /I not "%MODE%"=="prod" (
        call :Error "Invalid mode '%MODE%'. Use dev or prod."
        goto fail
    )
    shift
    goto parse_args
)
if /I "%~1"=="--quick-tests" (
    set "RUN_FULL_SUITE=0"
    shift
    goto parse_args
)
if /I "%~1"=="--skip-tests" (
    set "RUN_TESTS=0"
    set "RUN_FULL_SUITE=0"
    shift
    goto parse_args
)
if /I "%~1"=="--test-only" (
    set "TEST_ONLY=1"
    shift
    goto parse_args
)
if /I "%~1"=="--skip-build" (
    set "SKIP_BUILD=1"
    shift
    goto parse_args
)
if /I "%~1"=="--help" goto show_help
call :Warn "Unknown option %~1"
shift
goto parse_args

:after_args
call :Banner
call :Info "Mode      : %MODE%"
if %RUN_TESTS%==1 (
    if %RUN_FULL_SUITE%==1 (
        call :Info "Tests     : pre-flight + full suite"
    ) else (
        call :Info "Tests     : pre-flight only"
    )
) else (
    call :Warn "Tests     : skipped (--skip-tests)"
)
if %TEST_ONLY%==1 (
    call :Info "Launch    : disabled (--test-only)"
) else (
    call :Info "Launch    : enabled"
)

call :Info "Checking prerequisites..."
call :CheckCommand java "Java"
if errorlevel 1 goto fail
call :CheckCommand mvn "Maven"
if errorlevel 1 goto fail

if %RUN_TESTS%==1 (
    call :Info "Running pre-flight tests (quick-tests profile)..."
    mvn -B -T 1C -Pquick-tests test
    if errorlevel 1 (
        call :Error "Pre-flight tests failed. See target\test-reports for details."
        goto fail
    )
    call :Success "Pre-flight suite passed."

    if %RUN_FULL_SUITE%==1 (
        call :Info "Running full test suite (clean verify)..."
        mvn -B -T 1C clean verify
        if errorlevel 1 (
            call :Error "Full test suite failed. See target\test-reports for details."
            goto fail
        )
        call :Success "Full test suite passed."
    ) else (
        call :Info "Skipping full suite (--quick-tests)."
    )
) else (
    call :Warn "Tests skipped by user request."
)

if %SKIP_BUILD%==1 (
    call :Warn "Build skipped (--skip-build)."
) else (
    if /I "%MODE%"=="dev" (
        call :Info "Building development artifacts (dev-build profile)..."
        mvn -B -T 1C package -Pdev-build -DskipTests
        if errorlevel 1 goto fail
        if not exist "target\PoorCraft.jar" (
            call :Error "target\PoorCraft.jar not found after build."
            goto fail
        )
        call :Success "Development build complete."
    ) else (
        call :Info "Building production artifacts (prod-build profile)..."
        mvn -B -T 1C package -Pprod-build -DskipTests
        if errorlevel 1 goto fail
        if not exist "target\PoorCraft.exe" (
            call :Error "target\PoorCraft.exe not found. Ensure Launch4j profile is configured."
            goto fail
        )
        call :Success "Production build complete."
    )
)

if %TEST_ONLY%==1 (
    call :Info "Test-only mode complete."
    goto success
)

if %SKIP_BUILD%==1 (
    if /I "%MODE%"=="dev" (
        if not exist "target\PoorCraft.jar" (
            call :Error "Cannot launch: target\PoorCraft.jar missing."
            goto fail
        )
    ) else (
        if not exist "target\PoorCraft.exe" (
            call :Error "Cannot launch: target\PoorCraft.exe missing."
            goto fail
        )
    )
)

call :Info "Launching PoorCraft (%MODE% mode)..."
if /I "%MODE%"=="dev" (
    start "PoorCraft" java -jar "target\PoorCraft.jar"
) else (
    start "PoorCraft" "target\PoorCraft.exe"
)
call :Success "Launch command issued."

:success
call :Success "All steps finished."
popd >nul
exit /b 0

:CheckCommand
where %1 >nul 2>nul
if errorlevel 1 (
    call :Error "%2 not found in PATH."
    exit /b 1
)
call :Success "%2 detected."
exit /b 0

:Banner
echo %COLOR_INFO%==========================================%COLOR_RESET%
echo %COLOR_INFO%   PoorCraft Unified Test & Run%COLOR_RESET%
echo %COLOR_INFO%==========================================%COLOR_RESET%
exit /b 0

:Info
echo %COLOR_INFO%[INFO] %~1%COLOR_RESET%
exit /b 0

:Success
echo %COLOR_OK%[OK] %~1%COLOR_RESET%
exit /b 0

:Warn
echo %COLOR_WARN%[WARN] %~1%COLOR_RESET%
exit /b 0

:Error
echo %COLOR_ERROR%[ERROR] %~1%COLOR_RESET%
exit /b 0

:show_help
echo Usage: unified-test-and-run.bat --mode ^<dev^|prod^> [--quick-tests] [--skip-tests] [--test-only]
echo Options:
echo   --mode dev        Build JAR and launch via java -jar (default)
echo   --mode prod       Build production artifacts (Launch4j profile)
echo   --quick-tests     Run pre-flight suite only
echo   --skip-tests      Skip all automated tests
echo   --test-only       Run tests/build without launching game
echo   --skip-build      Run tests but reuse existing artifacts
echo   --help            Show this help
popd >nul
exit /b 0

:fail
call :Error "Process failed. Review messages above."
popd >nul
exit /b 1
