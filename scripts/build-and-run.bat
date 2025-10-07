@echo off
cd /d "%~dp0.."

echo ========================================
echo PoorCraft Build and Run Script
echo ========================================
echo.

echo [1/2] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found! Please install JDK 17 or higher.
    pause
    exit /b 1
)
echo Java found!

echo.
echo [2/2] Building with Maven...
call mvn clean package -q
if errorlevel 1 (
    echo ERROR: Maven build failed! Check the output above for errors.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build successful! Starting PoorCraft...
echo ========================================
echo.

java -jar target\PoorCraft.jar

if errorlevel 1 (
    echo.
    echo Game exited with error code %errorlevel%
    pause
)
