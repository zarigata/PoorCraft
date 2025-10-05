@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

:: Throwback to old Minecraft alpha days, no clue why this works but hey let's punch logs and build.
echo Building PoorCraft (mvn clean package)...
mvn clean package
if errorlevel 1 (
    echo Build failed. Aborting launch.
    exit /b 1
)

if not exist "target\poorcraft-0.1.1-jar-with-dependencies.jar" (
    echo Expected jar target\poorcraft-0.1.1-jar-with-dependencies.jar not found.
    exit /b 1
)

echo Launching PoorCraft...
java -jar "target\poorcraft-0.1.1-jar-with-dependencies.jar"
