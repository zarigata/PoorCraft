#!/bin/bash

echo "========================================"
echo "PoorCraft Build and Run Script"
echo "========================================"
echo ""

echo "[1/4] Checking Java..."
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java not found! Please install JDK 17 or higher."
    exit 1
fi
echo "Java found: $(java -version 2>&1 | head -n 1)"

echo ""
echo "[2/4] Checking Python..."
if command -v python3 >/dev/null 2>&1; then
    PY_CMD="python3"
else
    if command -v python >/dev/null 2>&1; then
        PY_CMD="python"
    else
        echo "ERROR: Python not found! Please install Python 3.8 or higher."
        exit 1
    fi
fi
echo "Python found: $($PY_CMD --version)"

echo ""
echo "[3/4] Installing Python dependencies..."
( cd python && "$PY_CMD" -m pip install -r requirements.txt --quiet )
if [ $? -ne 0 ]; then
    echo "WARNING: Failed to install some Python dependencies."
    echo "The game may still run, but mods might not work."
fi

echo ""
echo "[4/4] Building with Maven..."
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

java -jar target/poorcraft-0.1.0-SNAPSHOT-jar-with-dependencies.jar

if [ $? -ne 0 ]; then
    echo ""
    echo "Game exited with error code $?"
fi
