# Building PoorCraft Refactor

Complete guide to building PoorCraft Refactor from source, including native image compilation.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Build](#quick-build)
- [Development Build](#development-build)
- [Native Image Build](#native-image-build)
- [Cross-Platform Builds](#cross-platform-builds)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required

- **Java Development Kit (JDK) 17 or later**
  - Download: [Adoptium](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
  - Verify: `java -version`

- **Git** (for cloning repository)
  - Download: [git-scm.com](https://git-scm.com/)

### Optional (for native builds)

- **GraalVM 17+ with native-image**
  - Download: [GraalVM Downloads](https://www.graalvm.org/downloads/)
  - Required for creating native executables

## Quick Build

### Clone Repository

```powershell
git clone https://github.com/yourrepo/poorcraft-refactor.git
cd poorcraft-refactor
```

### Build All Modules

```powershell
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

This will:
- Compile all Kotlin source code
- Run all unit tests
- Create JAR files in `build/libs/`

### Run from Source

```powershell
# Windows
.\gradlew.bat :launcher:run

# Linux/Mac
./gradlew :launcher:run
```

## Development Build

### IDE Setup

#### IntelliJ IDEA (Recommended)

1. **Open Project**: File → Open → Select `poorcraft-refactor` directory
2. **Import Gradle**: IDEA will auto-detect and import Gradle project
3. **Set JDK**: File → Project Structure → Project SDK → Select JDK 17+
4. **Build**: Build → Build Project

#### VS Code

1. **Install Extensions**:
   - Kotlin Language
   - Gradle for Java

2. **Open Folder**: Open `poorcraft-refactor` directory

3. **Build**: Run Gradle task from command palette

#### Eclipse

1. **Import Project**: File → Import → Gradle → Existing Gradle Project
2. **Select Directory**: Choose `poorcraft-refactor`
3. **Build**: Project → Build Project

### Gradle Tasks

```powershell
# Build specific module
.\gradlew.bat :engine:build
.\gradlew.bat :launcher:build

# Run tests
.\gradlew.bat test

# Run specific test
.\gradlew.bat :engine:test --tests "BlockRegistryTest"

# Clean build
.\gradlew.bat clean build

# Run with arguments
.\gradlew.bat :launcher:run --args="--dev-mode"
.\gradlew.bat :launcher:runDev
.\gradlew.bat :launcher:runPortable
.\gradlew.bat :launcher:runHeadless
```

### Hot Reload Development

```powershell
# Terminal 1: Run in dev mode
.\gradlew.bat :launcher:runDev

# Terminal 2: Auto-rebuild on changes
.\gradlew.bat -t :engine:classes :launcher:classes
```

## Native Image Build

### Install GraalVM

#### Windows

1. **Download GraalVM**:
   ```powershell
   # Download from https://www.graalvm.org/downloads/
   # Choose Java 17 or 21, Windows x64
   ```

2. **Extract**: Extract to `C:\Program Files\GraalVM\`

3. **Set Environment Variables**:
   ```powershell
   # PowerShell (Admin)
   [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\GraalVM\graalvm-jdk-17', 'Machine')
   [System.Environment]::SetEnvironmentVariable('PATH', $env:PATH + ';C:\Program Files\GraalVM\graalvm-jdk-17\bin', 'Machine')
   ```

4. **Install native-image**:
   ```powershell
   gu install native-image
   ```

5. **Install Visual Studio Build Tools** (required for Windows):
   - Download: [Visual Studio Build Tools](https://visualstudio.microsoft.com/downloads/)
   - Install: "Desktop development with C++" workload

#### Linux

```bash
# Download and extract GraalVM
wget https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-17.0.9/graalvm-community-jdk-17.0.9_linux-x64_bin.tar.gz
tar -xzf graalvm-community-jdk-17.0.9_linux-x64_bin.tar.gz
sudo mv graalvm-community-openjdk-17.0.9 /usr/lib/jvm/

# Set environment
export JAVA_HOME=/usr/lib/jvm/graalvm-community-openjdk-17.0.9
export PATH=$JAVA_HOME/bin:$PATH

# Install native-image
gu install native-image

# Install build dependencies
sudo apt-get install build-essential zlib1g-dev
```

#### macOS

```bash
# Install via Homebrew
brew install --cask graalvm/tap/graalvm-ce-java17

# Set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java17-XX.X.X/Contents/Home

# Install native-image
gu install native-image
```

### Build Native Image

```powershell
# Windows
.\gradlew.bat :launcher:nativeCompile

# Linux/Mac
./gradlew :launcher:nativeCompile
```

**Output**: `launcher/build/native/nativeCompile/PoorCraftRefactor.exe` (Windows) or `PoorCraftRefactor` (Linux/Mac)

### Native Image Configuration

The native image build is configured in `launcher/build.gradle.kts`:

```kotlin
graalvmNative {
    binaries {
        named("main") {
            imageName.set("PoorCraftRefactor")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=org.slf4j,ch.qos.logback")
            buildArgs.add("--initialize-at-run-time=org.lwjgl")
            // ... resource includes
        }
    }
}
```

### Native Image Optimization

For smaller executable size:

```powershell
.\gradlew.bat :launcher:nativeCompile -Dgraalvm.native-image.args="--no-fallback,-O3,--gc=G1"
```

Options:
- `-O3`: Maximum optimization
- `--gc=G1`: Use G1 garbage collector
- `--static`: Static linking (Linux only)

## Cross-Platform Builds

### Building for Windows on Linux

Requires cross-compilation setup (complex). Recommended: Use Windows VM or CI.

### Building for Linux on Windows

Use WSL2 (Windows Subsystem for Linux):

```powershell
# In WSL2
./gradlew :launcher:nativeCompile
```

### CI/CD Builds

GitHub Actions workflow (`.github/workflows/ci.yml`) builds for multiple platforms:

```yaml
- Ubuntu (Linux native image)
- Windows (optional, requires Windows runner)
```

## Troubleshooting

### Build Fails: "Could not find Java"

**Solution**:
```powershell
# Verify Java installation
java -version

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
```

### Build Fails: "Gradle daemon disappeared"

**Solution**:
```powershell
# Increase Gradle memory
$env:GRADLE_OPTS = "-Xmx4g"
.\gradlew.bat build
```

### Native Image Build Fails

**Common Issues**:

1. **Missing Visual Studio Build Tools (Windows)**:
   - Install Visual Studio Build Tools with C++ workload
   - Run build from "x64 Native Tools Command Prompt"

2. **Out of Memory**:
   ```powershell
   .\gradlew.bat :launcher:nativeCompile -Dorg.gradle.jvmargs=-Xmx8g
   ```

3. **Reflection Errors**:
   - Add reflection configuration in `launcher/src/main/resources/META-INF/native-image/`

4. **Resource Not Found**:
   - Verify resources are included in build args
   - Check `buildArgs.add("-H:IncludeResources=...")`

### Tests Fail

**Solution**:
```powershell
# Run tests with more info
.\gradlew.bat test --info

# Run specific test
.\gradlew.bat :engine:test --tests "ChunkTest"

# Skip tests
.\gradlew.bat build -x test
```

### LWJGL Native Library Not Found

**Solution**:
```powershell
# Verify natives are included
.\gradlew.bat dependencies --configuration runtimeClasspath

# Clean and rebuild
.\gradlew.bat clean build
```

### Gradle Wrapper Not Found

**Solution**:
```powershell
# Re-download wrapper
gradle wrapper --gradle-version 8.5
```

## Build Artifacts

After successful build:

```
poorcraft-refactor/
├── engine/build/libs/
│   └── engine-0.1.2.jar
├── launcher/build/libs/
│   └── launcher-0.1.2.jar
├── launcher/build/native/nativeCompile/
│   └── PoorCraftRefactor.exe
└── tools/atlas-packer/build/libs/
    └── atlas-packer-0.1.2.jar
```

## Performance Benchmarks

Typical build times (on i7-10700K, 32GB RAM):

| Task | Time |
|------|------|
| Clean build | ~30s |
| Incremental build | ~5s |
| Run tests | ~10s |
| Native image (Windows) | ~5-10 min |
| Native image (Linux) | ~3-5 min |

## Advanced Configuration

### Custom Build Properties

Create `gradle.properties` in project root:

```properties
# Increase memory
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g

# Enable parallel builds
org.gradle.parallel=true

# Enable caching
org.gradle.caching=true

# Use daemon
org.gradle.daemon=true
```

### Custom Native Image Args

```powershell
.\gradlew.bat :launcher:nativeCompile `
  -Dgraalvm.native-image.args="--verbose,--no-fallback,-H:+TraceClassInitialization"
```

### Profile Build

```powershell
.\gradlew.bat build --profile
# Report: build/reports/profile/
```

## Continuous Integration

### GitHub Actions

Workflow automatically:
- Builds on push/PR
- Runs all tests
- Creates native image artifacts
- Uploads build artifacts

### Local CI Simulation

```powershell
# Simulate CI build
.\gradlew.bat clean build test :launcher:nativeCompile
```

## Distribution

### Creating Release Package

```powershell
# Build native image
.\gradlew.bat :launcher:nativeCompile

# Create distribution directory
mkdir dist
cp launcher/build/native/nativeCompile/PoorCraftRefactor.exe dist/
cp README.md dist/
cp LICENSE dist/

# Create ZIP
Compress-Archive -Path dist/* -DestinationPath PoorCraftRefactor-v0.1.2-windows.zip
```

### Installer (Future)

jpackage support planned:

```powershell
.\gradlew.bat :launcher:jpackage
```

## Support

- **Build Issues**: Check GitHub Issues
- **Questions**: GitHub Discussions
- **Documentation**: See README.md and docs/
