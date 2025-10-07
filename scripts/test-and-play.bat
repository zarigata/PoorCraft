@echo off
setlocal EnableDelayedExpansion

for /f "tokens=1,2 delims==" %%I in ('wmic os get Version /value ^| find "="') do set "OSVERSION=%%J"
for /f %%i in ('echo prompt $E^| cmd') do set "ESC=%%i"
set "COLOR_INFO=%ESC%[96m"
set "COLOR_OK=%ESC%[92m"
set "COLOR_WARN=%ESC%[93m"
set "COLOR_ERROR=%ESC%[91m"
set "COLOR_RESET=%ESC%[0m"

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
pushd "%ROOT_DIR%" >nul

set "RUN_TESTS=1"
set "LAUNCH_GAME=1"
set "MAVEN_FLAGS="

:ParseArgs
if "%~1"=="" goto AfterArgs
if /I "%~1"=="--skip-tests" (
    set "RUN_TESTS=0"
    shift
    goto ParseArgs
)
if /I "%~1"=="--test-only" (
    set "LAUNCH_GAME=0"
    shift
    goto ParseArgs
)
if /I "%~1"=="--help" (
    goto ShowHelp
)

call :Warn "Unknown option %~1"
shift
goto ParseArgs

:AfterArgs
call :Banner
call :Info "Checking prerequisites..."
call :CheckCommand java "Java"
if errorlevel 1 goto Fail
call :CheckCommand mvn "Maven"
if errorlevel 1 goto Fail

if %RUN_TESTS%==0 (
    set "MAVEN_FLAGS=-DskipTests=true"
) else (
    set "MAVEN_FLAGS=-DskipTests=false"
)

call :Info "Step 1/2: Running Maven verify lifecycle (mvn -B -T 1C clean verify %MAVEN_FLAGS%)"
mvn -B -T 1C clean verify %MAVEN_FLAGS%
if errorlevel 1 goto Fail
if %RUN_TESTS%==0 (
    call :Warn "Tests skipped (--skip-tests)."
) else (
    call :Success "Tests passed."
)

if not exist "target\PoorCraft.jar" (
    call :Error "target\\PoorCraft.jar not found after verify. Ensure the build completed successfully."
    goto Fail
)

if %LAUNCH_GAME%==1 (
    call :Info "Step 2/2: Launching PoorCraft"
    java -jar "target\PoorCraft.jar"
) else (
    call :Info "Step 2/2: Test-only mode complete. Game launch skipped."
)

call :Success "All steps finished. Enjoy PoorCraft!"
popd >nul
exit /b 0

:Banner
call :Info "========================================"
call :Info "      PoorCraft - Test & Play"
call :Info "========================================"
exit /b 0

:CheckCommand
where %1 >nul 2>nul
if errorlevel 1 (
    call :Error "%2 not found in PATH. Please install %2 or update PATH."
    exit /b 1
)
call :Success "%2 detected."
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

:ShowHelp
call :Info "Usage: test-and-play.bat [--skip-tests] [--test-only]"
call :Info "  --skip-tests   Build and launch without running tests."
call :Info "  --test-only    Build and run tests without launching the game."
call :Info "  --help         Display this help message."
popd >nul
exit /b 0

:Fail
call :Error "Process failed. Review the messages above."
call :Warn "Press any key to exit..."
pause >nul
popd >nul
exit /b 1
