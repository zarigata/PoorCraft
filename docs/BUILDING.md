# Building PoorCraft

This guide provides comprehensive instructions for building PoorCraft from source on Windows, Linux, and macOS.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Manual Build Process](#manual-build-process)
- [Build Profiles](#build-profiles)
- [Platform-Specific Notes](#platform-specific-notes)
- [Build Output](#build-output)
- [Dependency Management](#dependency-management)
- [Troubleshooting](#troubleshooting)
- [Development Setup](#development-setup)
- [Build Customization](#build-customization)
- [Continuous Integration](#continuous-integration)
- [Performance Tips](#performance-tips)
- [Clean Build](#clean-build)
- [Version Information](#version-information)
- [Next Steps](#next-steps)

## Prerequisites

### Required Software

1. **JDK 17 or higher**
   - Download: [Eclipse Temurin](https://adoptium.net/) (recommended)
   - Verify installation:
     ```bash
     java -version
     ```
   - Should output Java 17 or higher

2. **Maven 3.6 or higher**
   - Download: [Apache Maven](https://maven.apache.org/download.cgi)
   - Verify installation:
     ```bash
     mvn --version
     ```

3. **Git** (for cloning repository)
   - Download: [Git SCM](https://git-scm.com/)
   - Verify:
     ```bash
     git --version
     ```

### System Requirements

- **RAM:** 4GB minimum (8GB recommended for development)
- **Storage:** 500MB for build, 1GB recommended
- **GPU:** OpenGL 3.3 compatible graphics card
- **OS:** Windows 10+, Ubuntu 20.04+, or macOS 11+

## Quick Start

### Clone and Build

```bash
# Clone repository
git clone https://github.com/zarigata/poorcraft.git
cd poorcraft

# Build and run (Windows)
scripts\build-and-run.bat

# Build and run (Linux/Mac)
chmod +x scripts/build-and-run.sh
scripts/build-and-run.sh
```

The script will:
1. Check for Java installation
2. Build the project with Maven
3. Launch the game automatically

## Manual Build Process

### Step 1: Clean Previous Builds

Remove any previous build artifacts:

```bash
mvn clean
```

This removes the `target/` directory containing old compiled classes and JARs.

### Step 2: Compile and Package

Build the project and create executable JAR:

```bash
mvn package
```

This will:
- Compile all Java source files
- Process resources
- Run tests (if any)
- Create fat JAR with all dependencies
- Generate Windows executable (if on Windows)

**Build time:** ~30-60 seconds (first build downloads dependencies)

### Step 3: Run the Game

Execute the generated JAR:

```bash
java -jar target/PoorCraft.jar
```

Or on Windows, run the executable:

```bash
target\PoorCraft.exe
```

## Build Profiles

### Development Build (Fast)

Skip tests for faster builds during development:

```bash
mvn clean package -DskipTests
```

### Release Build (Optimized)

Create optimized release build:

```bash
mvn clean package -Prelease
```

*(Note: Release profile not yet configured, will be added in future version)*

### Windows Executable Only

Build just the Windows .exe wrapper:

```bash
mvn clean package
# Output: target/PoorCraft.exe
```

The Launch4j plugin automatically creates the .exe during the package phase.

## Platform-Specific Notes

### Windows

**Build Scripts:**
- `scripts/build-and-run.bat` - Build and run in one command
- `scripts/build-exe.bat` - Build executable only

**Launch4j Executable:**
- Creates native .exe wrapper around JAR
- Checks for Java 17+ and prompts if missing
- Memory settings: 512MB min, 2GB max
- Icon: Can add custom icon (uncomment in pom.xml)

**PowerShell Execution:**
If batch files don't work, use PowerShell:
```powershell
scripts\run-poorcraft.ps1
```

**Path Considerations:**
- Use backslashes: `target\PoorCraft.jar`
- Or forward slashes work too: `target/PoorCraft.jar`

### Linux

**Build Script:**
```bash
chmod +x scripts/build-and-run.sh
scripts/build-and-run.sh
```

**LWJGL Natives:**
- Linux natives included automatically by Maven
- Platform detection handled in pom.xml

**OpenGL Drivers:**
May need to install OpenGL development libraries:

```bash
# Ubuntu/Debian
sudo apt-get install libgl1-mesa-dev

# Fedora
sudo dnf install mesa-libGL-devel

# Arch
sudo pacman -S mesa
```

**Permissions:**
Ensure shell scripts are executable:
```bash
chmod +x scripts/*.sh
```

### macOS

**Build Script:**
```bash
chmod +x scripts/build-and-run.sh
scripts/build-and-run.sh
```

**LWJGL Natives:**
- macOS natives included automatically
- Both Intel and Apple Silicon supported

**Apple Silicon (M1/M2):**
- Use native ARM JDK for best performance
- Or use Rosetta 2 with x86_64 JDK
- LWJGL includes ARM64 natives

**Security Settings:**
First run may prompt security warning:
1. Go to System Preferences → Security & Privacy
2. Click "Open Anyway" for Java

**Notarization:**
For distribution, executables must be notarized (requires Apple Developer account).

## Build Output

### Generated Files

After `mvn package`, you'll find:

```
target/
├── poorcraft-2.0.0.jar          # Standard JAR (requires dependencies)
├── PoorCraft.jar                # Fat JAR with all dependencies (~15-20MB)
└── PoorCraft.exe                # Windows executable wrapper (~15-20MB)
```

**Fat JAR:**
- Contains all dependencies (LWJGL, JOML, LuaJ, Gson, Netty)
- Single-file distribution
- Cross-platform (requires Java 17+ installed)

**Windows EXE:**
- Wrapper around PoorCraft.jar
- Checks for Java and prompts to download if missing
- Native Windows integration

### Runtime Directories

Created on first run:

```
gamedata/              # Runtime game data
├── mods/             # Lua mods
├── worlds/           # World saves
├── config/           # Configuration files
├── screenshots/      # Screenshots
└── skins/            # Player skins

assets/               # Development assets
├── ui/              # UI textures
└── scripts/         # Utility scripts
```

## Dependency Management

### Maven Dependencies

All dependencies defined in `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.lwjgl</groupId>
        <artifactId>lwjgl</artifactId>
        <version>3.3.3</version>
    </dependency>
    <!-- ... more dependencies ... -->
</dependencies>
```

**Main Dependencies:**
- **LWJGL 3.3.3** - OpenGL, GLFW, STB bindings
- **JOML 1.10.5** - 3D math library
- **LuaJ 3.0.1** - Lua scripting engine
- **Gson 2.10.1** - JSON parsing
- **Netty 4.1.100** - Networking

### Native Libraries

LWJGL requires platform-specific native libraries. Maven automatically includes natives for:
- Windows (x64)
- Linux (x64)
- macOS (x64 and ARM64)

Platform detection is automatic.

### Dependency Download

First build downloads all dependencies from Maven Central:
- Takes 30-60 seconds
- Cached in `~/.m2/repository/`
- Subsequent builds use cached dependencies (much faster)

**Force Update Dependencies:**
```bash
mvn clean install -U
```

## Troubleshooting

### Common Issues

#### "Java not found" error

**Cause:** Java not in PATH

**Solution:**
1. Install JDK 17+ from [Adoptium](https://adoptium.net/)
2. Add to PATH:
   - **Windows:** System Properties → Environment Variables → Add Java bin to PATH
   - **Linux/Mac:** Add to `~/.bashrc` or `~/.zshrc`:
     ```bash
     export PATH="/path/to/jdk/bin:$PATH"
     ```
3. Verify: `java -version`

#### "Maven not found" error

**Cause:** Maven not in PATH

**Solution:**
1. Install Maven from [apache.org](https://maven.apache.org/)
2. Add to PATH (similar to Java above)
3. Verify: `mvn --version`

#### Build fails with "package does not exist"

**Cause:** Corrupted Maven cache or missing dependencies

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/

# Clean and rebuild
mvn clean install -U
```

#### "OutOfMemoryError" during build

**Cause:** Insufficient Maven heap space

**Solution:**
```bash
# Linux/Mac
export MAVEN_OPTS="-Xmx2048m"

# Windows (CMD)
set MAVEN_OPTS=-Xmx2048m

# Windows (PowerShell)
$env:MAVEN_OPTS="-Xmx2048m"

# Then build
mvn clean package
```

#### Game won't start - "OpenGL error"

**Cause:** Outdated or incompatible graphics drivers

**Solutions:**
1. Update graphics drivers to latest version
2. Verify OpenGL 3.3+ support:
   - **Linux:** `glxinfo | grep "OpenGL version"`
   - **Windows:** Use GPU-Z or similar tool
3. Try software rendering (slow):
   ```bash
   java -Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true -jar target/PoorCraft.jar
   ```

#### Mods not loading

**Cause:** Missing or malformed mod files

**Solutions:**
1. Check `gamedata/mods/` directory exists
2. Verify mod structure:
   ```
   gamedata/mods/my_mod/
   ├── mod.json
   └── main.lua
   ```
3. Check `mod.json` has `"main": "main.lua"`
4. Look for Lua errors in console output
5. See [MODDING_GUIDE.md](MODDING_GUIDE.md) for details

#### "**Permission denied" on Linux/Mac

**Cause:** Shell scripts not executable

**Solution:**
```bash
chmod +x scripts/build-and-run.sh
chmod +x scripts/*.sh

#### IntelliJ IDEA (Recommended)

1. **Open Project:**
   - File → Open → Select `pom.xml`
   - Import as Maven project

2. **Set JDK:**
   - File → Project Structure → Project SDK → Select JDK 17+

3. **Run Configuration:**
   - Run → Edit Configurations
   - Add New → Application
   - Main class: `com.poorcraft.Main`
   - Working directory: `$PROJECT_DIR$`

4. **Maven Integration:**
   - Maven tool window (right sidebar)
   - Lifecycle: clean, package
   - Plugins: available automatically

#### Eclipse

1. **Import Project:**
   - File → Import → Existing Maven Projects
   - Select PoorCraft directory

2. **Set JDK:**
   - Project → Properties → Java Build Path
   - Libraries → Add JDK 17+

3. **Run Configuration:**
   - Run → Run Configurations
   - Java Application → New
   - Main class: `com.poorcraft.Main`

#### VS Code

1. **Install Extensions:**
   - Java Extension Pack
   - Maven for Java

2. **Open Folder:**
   - File → Open Folder → Select PoorCraft

3. **Build:**
   - Maven sidebar → Lifecycle → package

4. **Run:**
   - Java Projects sidebar → Run

### Hot Reload (Development)

**Lua Mods:**
- Edit `gamedata/mods/*/main.lua`
- Restart game to reload

**Java Code:**
- Requires full rebuild: `mvn package`
- Or use IDE's run configuration for faster iteration

**Shaders:**
- Edit `src/main/resources/shaders/*.glsl`
- Restart game to reload

**UI Textures:**
- Edit `assets/ui/*.png`
- Restart game to reload

## Build Customization

### Change Memory Settings

Edit Launch4j configuration in `pom.xml`:

```xml
<jre>
    <minVersion>17</minVersion>
    <opts>
        <opt>-Xms512m</opt>   <!-- Minimum memory -->
        <opt>-Xmx2048m</opt>  <!-- Maximum memory -->
    </opts>
</jre>
```

Increase for larger worlds or better performance.

### Add Custom Icon

1. Create `src/main/resources/images/icon.ico` (256x256 recommended)
2. Uncomment icon line in `pom.xml` Launch4j config:
   ```xml
   <icon>src/main/resources/images/icon.ico</icon>
   ```

### Skip Tests

```bash
mvn package -DskipTests
```

### Verbose Output

```bash
mvn package -X  # Debug output (very verbose)
mvn package -e  # Show error stack traces
```

### Parallel Builds

Use multiple threads for faster builds:

```bash
mvn -T 4 package  # Use 4 threads
mvn -T 1C package  # One thread per CPU core
```

### Offline Mode

If dependencies are cached:

```bash
mvn -o package  # Offline mode, no network
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Build PoorCraft

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean package -DskipTests
      
      - name: Upload JAR
        uses: actions/upload-artifact@v3
        with:
          name: PoorCraft
          path: target/PoorCraft.jar
```

## Performance Tips

### Faster Development Builds

```bash
# Skip tests
mvn package -DskipTests

# Use Maven daemon (requires mvnd)
mvnd clean package

# Parallel builds
mvn -T 4 package

# Offline mode (if dependencies cached)
mvn -o package
```

### Maven Daemon

Install `mvnd` for faster builds:
- Download from [maven.apache.org](https://github.com/apache/maven-mvnd)
- Drop-in replacement for `mvn` command
- Keeps JVM warm between builds

## Clean Build

Remove all build artifacts and caches:

```bash
# Remove target directory
mvn clean

# Or manually
rm -rf target/

# Clear Maven cache (nuclear option)
rm -rf ~/.m2/repository/com/poorcraft/
```

## Version Information

- **Current Version:** 2.0.0
- **Java Target:** 17
- **Maven Minimum:** 3.6
- **Build Time:** 30-60 seconds (first build), 10-20 seconds (incremental)

## Next Steps

After successfully building:

1. **Run the game:**
   ```bash
   java -jar target/PoorCraft.jar
   ```

2. **Read gameplay documentation:**
   - [README.md](../README.md) - Game overview and controls

3. **Try modding:**
   - [MODDING_GUIDE.md](MODDING_GUIDE.md) - Create your first mod

4. **Understand architecture:**
   - [ARCHITECTURE.md](ARCHITECTURE.md) - System design

5. **Prepare for release:**
   - [DEPLOYMENT.md](DEPLOYMENT.md) - Packaging and distribution

---

**Having issues? Check the [troubleshooting section](#troubleshooting) or open an issue on GitHub!**
