@echo off
setlocal EnableDelayedExpansion

for /f %%i in ('echo prompt $E^| cmd') do set "ESC=%%i"
set "COLOR_INFO=%ESC%[96m"
set "COLOR_OK=%ESC%[92m"
set "COLOR_ERROR=%ESC%[91m"
set "COLOR_RESET=%ESC%[0m"

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
pushd "%ROOT_DIR%" >nul

call :Banner

call :Info "Checking Java..."
where java >nul 2>nul
if errorlevel 1 (
    call :Error "Java not found in PATH. Install Java 17 or later."
    goto Fail
)
call :Success "Java detected."

call :Info "Building PoorCraft (skip tests)..."
mvn clean package -DskipTests > build.log 2>&1
if errorlevel 1 (
    call :Error "Build failed. See build.log for details."
    goto Fail
)
call :Success "Build complete."

if not exist "target\PoorCraft.jar" (
    call :Error "target\\PoorCraft.jar not found."
    goto Fail
)

call :Info "Launching PoorCraft..."
java -jar "target\PoorCraft.jar"

popd >nul
exit /b 0

:Banner
echo %COLOR_INFO%========================================%COLOR_RESET%
echo %COLOR_INFO%      PoorCraft - Quick Play%COLOR_RESET%
echo %COLOR_INFO%========================================%COLOR_RESET%
exit /b 0

:Info
echo %COLOR_INFO%[INFO] %~1%COLOR_RESET%
exit /b 0

:Success
echo %COLOR_OK%[OK] %~1%COLOR_RESET%
exit /b 0

:Error
echo %COLOR_ERROR%[ERROR] %~1%COLOR_RESET%
exit /b 0

:Fail
call :Error "Quick play aborted."
call :Info "Press any key to exit..."
pause >nul
popd >nul
exit /b 1
