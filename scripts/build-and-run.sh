#!/bin/bash

cd "$(dirname "$0")/.."

echo "========================================"
echo "PoorCraft Build and Run Script"
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
mvn clean package -q
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed! Check the output above for errors."
    exit 1
fi

echo ""
echo "========================================"
echo "Build successful! Starting PoorCraft..."
echo "========================================"
echo ""

java -jar target/PoorCraft.jar

if [ $? -ne 0 ]; then
    echo ""
    echo "Game exited with error code $?"
fi
