@echo off
echo ========================================
echo PoorCraft Build and Run Script
echo ========================================
echo.

echo [1/4] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found! Please install JDK 17 or higher.
    pause
    exit /b 1
)
echo Java found!

echo.
echo [2/4] Checking Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found! Please install Python 3.8 or higher.
    pause
    exit /b 1
)
echo Python found!

echo.
echo [3/4] Installing Python dependencies...
pushd python
python -m pip install -r requirements.txt --quiet
if errorlevel 1 (
    echo WARNING: Failed to install some Python dependencies.
    echo The game may still run, but mods might not work.
)
popd

echo.
echo [4/4] Building with Maven...
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

java -jar target\poorcraft-0.1.0-SNAPSHOT-jar-with-dependencies.jar

if errorlevel 1 (
    echo.
    echo Game exited with error code %errorlevel%
    pause
)
