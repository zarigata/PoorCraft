Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location -Path (Join-Path -Path $PSScriptRoot -ChildPath "..")

# ok kinda messy but i swear it's like minecraft beta when redstone just randomly behaved
echo "Building PoorCraft (mvn clean package)..."
mvn clean package

if ($LASTEXITCODE -ne 0) {
    echo "Build failed. Aborting launch."
    exit 1
}

$jarPath = "target/PoorCraft.jar"

if (-not (Test-Path -Path $jarPath)) {
    echo "Expected jar $jarPath not found."
    exit 1
}

# i dont know whats going on but its working like that first minecraft pig spawner bug :)
echo "Launching PoorCraft..."
java -jar $jarPath
