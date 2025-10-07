#!/bin/bash

cd "$(dirname "$0")/.."

echo "========================================"
echo "PoorCraft Windows Executable Builder"
echo "========================================"
echo ""

echo "[1/2] Checking Java..."
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java not found! Please install JDK 17 or higher."
    exit 1
fi
echo "Java found: $(java -version 2>&1 | head -n 1)"

echo ""
echo "[2/2] Building with Maven..."
echo "This will create PoorCraft.jar and PoorCraft.exe"
echo ""
mvn clean package
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed! Check the output above for errors."
    exit 1
fi

echo ""
echo "========================================"
echo "Build successful!"
echo "========================================"
echo ""
echo "Files created:"
echo "  - target/PoorCraft.jar (Fat JAR with all dependencies)"
echo "  - target/PoorCraft.exe (Windows executable)"
echo ""
echo "To run: cd target && ./PoorCraft.exe"
echo "Or double-click PoorCraft.exe in the target folder"
echo ""
echo "The game will automatically create gamedata/ and assets/ folders on first run."
echo "Look for the 'Hi Mod' message to verify Lua mods are working!"
echo ""
