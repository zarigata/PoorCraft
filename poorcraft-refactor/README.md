# PoorCraft Refactor v0.1.2

A single-file native Windows EXE game engine written in Kotlin (JVM) with LWJGL rendering, embedded Lua scripting for mods, robust event/plugin API, texture/skin system, and chunked world management.

## Features

- **Native Windows EXE**: Single executable with embedded JVM runtime (GraalVM native-image)
- **LWJGL Rendering**: OpenGL-based voxel rendering with chunk meshing and frustum culling
- **Lua Modding**: Sandboxed Lua scripting using Luaj for safe, extensible gameplay modifications
- **Event System**: Robust event bus for inter-system communication
- **Texture System**: Per-block PNG textures with automatic atlas packing
- **Chunk Management**: Asynchronous chunk loading/saving with region file storage
- **First-Run Bootstrap**: Automatic creation of user-editable directories (mods/, skins/, saves/)

## Quick Start

### Prerequisites

- **Java 17+** (for development)
- **GraalVM 17+** with native-image component (for native builds)
- **Gradle 8+** (wrapper included)

### Development Mode

```powershell
# Clone the repository
git clone <repository-url>
cd poorcraft-refactor

# Run in development mode
./gradlew :launcher:run

# Run with dev mode (live reload)
./gradlew :launcher:runDev

# Run in portable mode
./gradlew :launcher:runPortable

# Run headless (for testing)
./gradlew :launcher:runHeadless
```

### Building

```powershell
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build native image (requires GraalVM)
./gradlew :launcher:nativeCompile

# The native executable will be at:
# launcher/build/native/nativeCompile/PoorCraftRefactor.exe
```

### Packaging

#### GraalVM Native Image (Recommended)

1. Install GraalVM 17+:
   ```powershell
   # Download from https://www.graalvm.org/downloads/
   # Install native-image component
   gu install native-image
   ```

2. Build native image:
   ```powershell
   ./gradlew :launcher:nativeCompile
   ```

3. The resulting `PoorCraftRefactor.exe` is a standalone executable.

#### Alternative: jpackage

```powershell
# Create installer with bundled JRE
./gradlew :launcher:jpackage
```

## Project Structure

```
poorcraft-refactor/
├── engine/                 # Core engine module
│   ├── src/main/kotlin/
│   │   └── com/poorcraft/engine/
│   │       ├── core/       # Engine core (Engine.kt)
│   │       ├── rendering/  # OpenGL rendering
│   │       ├── world/      # Chunk management
│   │       ├── block/      # Block registry
│   │       ├── event/      # Event bus
│   │       ├── mod/        # Mod loader
│   │       └── config/     # Configuration
│   └── src/main/resources/
│       └── assets/         # Embedded default assets
├── launcher/               # Launcher module
│   └── src/main/kotlin/
│       └── com/poorcraft/launcher/
│           └── Launcher.kt # Bootstrap & entry point
├── tools/                  # Build tools
│   └── atlas-packer/       # Texture atlas packer
└── .github/workflows/      # CI/CD configuration
```

## First Run Behavior

On first run, the executable creates a data directory:

**Default Location**: `%APPDATA%\PoorCraftRefactor\`

**Portable Mode**: `<exe-folder>\PoorCraftData\` (use `--portable` flag)

**Directory Structure**:
```
PoorCraftRefactor/
├── skins/              # Block textures (user-editable)
│   └── default/        # Default texture pack
├── mods/               # Lua mods (user-editable)
│   └── example_mod/    # Example mod
├── saves/              # World saves
├── logs/               # Engine and mod logs
└── config.json         # Engine configuration
```

## Configuration

Edit `config.json` to customize engine settings:

```json
{
  "version": "0.1.2",
  "windowWidth": 1280,
  "windowHeight": 720,
  "vsync": true,
  "renderDistance": 8,
  "fov": 70.0,
  "textureSize": 16,
  "chunkSize": 16,
  "chunkHeight": 256,
  "maxLoadedChunks": 256,
  "workerThreads": 4,
  "modMemoryLimitMB": 64,
  "modTickTimeoutMs": 100,
  "enableModSandbox": true,
  "logLevel": "INFO"
}
```

## Creating Mods

See [API.md](docs/API.md) for complete API documentation.

### Basic Mod Structure

```
mods/
└── my_mod/
    ├── mod.json        # Mod manifest
    └── main.lua        # Main script
```

### Example mod.json

```json
{
  "id": "my_mod",
  "name": "My Awesome Mod",
  "version": "1.0.0",
  "author": "YourName",
  "main": "main.lua",
  "entry": "onEnable"
}
```

### Example main.lua

```lua
local engine = Engine
local log = Logger

function onLoad()
  log:info("Mod loaded")
end

function onEnable()
  log:info("Mod enabled")
  
  -- Register event listener
  engine:registerEvent("onTick", function(tick)
    if tick % 100 == 0 then
      log:info("Tick: " .. tick)
    end
  end)
end

function onDisable()
  log:info("Mod disabled")
end
```

## Creating Skins

See [SKINS.md](docs/SKINS.md) for texture creation guide.

### Texture Format

- **Format**: PNG
- **Size**: 16×16 or 32×32 pixels
- **Naming**: `<block_name>.png` (e.g., `grass.png`, `stone.png`)

### Adding Custom Textures

1. Place PNG files in `skins/default/` or create a new skin pack directory
2. Restart the game (atlas is rebuilt automatically)
3. Or run the atlas packer tool manually:
   ```powershell
   ./gradlew :tools:atlas-packer:run --args="skins/default"
   ```

## Command Line Arguments

- `--portable` - Use portable mode (data folder next to exe)
- `--dev-mode` - Enable development mode (live reload, console)
- `--headless` - Run in headless mode (no window, for testing)

## Development

### Running Tests

```powershell
# Run all tests
./gradlew test

# Run specific test
./gradlew :engine:test --tests "BlockRegistryTest"

# Run with coverage
./gradlew test jacocoTestReport
```

### Building Atlas

```powershell
# Pack textures into atlas
./gradlew :tools:atlas-packer:run --args="path/to/skins output/path"
```

### Hot Reload (Dev Mode)

```powershell
./gradlew :launcher:runDev
```

In dev mode:
- Lua mods are reloaded on file change
- Press `` ` `` (grave accent) to open console
- Logs are more verbose

## Architecture

### Engine Core

- **Engine.kt**: Main engine class, coordinates all subsystems
- **EventBus**: Pub/sub event system for decoupled communication
- **BlockRegistry**: Central registry for block types and textures
- **WorldManager**: Manages chunks, loading, saving, and generation
- **ModLoader**: Loads and sandboxes Lua mods

### Rendering Pipeline

1. **Chunk Meshing**: Builds vertex data from block data (greedy meshing)
2. **Frustum Culling**: Only renders visible chunks
3. **Texture Atlas**: All block textures in single atlas for performance
4. **Shaders**: Simple vertex/fragment shaders for voxel rendering

### Mod Sandboxing

- Lua VMs are isolated per mod
- Dangerous functions removed (`os`, `io`, `require`, etc.)
- File access restricted to mod's own directory
- CPU time limits per tick (100ms default)
- Memory limits enforced (64MB default)
- Crashes in mods don't crash engine

## Performance

- **Chunk Loading**: Asynchronous with worker thread pool
- **Rendering**: VBO-based mesh rendering with face culling
- **Memory**: Configurable chunk cache with LRU eviction
- **Mods**: Sandboxed with resource limits

## Troubleshooting

### Native Image Build Fails

- Ensure GraalVM is installed and `native-image` component is available
- Check that `JAVA_HOME` points to GraalVM installation
- Try increasing heap size: `./gradlew :launcher:nativeCompile -Dorg.gradle.jvmargs=-Xmx4g`

### Mods Not Loading

- Check `logs/engine.log` for errors
- Verify `mod.json` is valid JSON
- Ensure main script file exists and is named correctly
- Check for Lua syntax errors in mod script

### Textures Not Showing

- Verify PNG files are in `skins/default/`
- Check that atlas was built (look for `atlas.png` and `atlas.json`)
- Run atlas packer manually if needed
- Check logs for texture loading errors

### Performance Issues

- Reduce `renderDistance` in config.json
- Lower `maxLoadedChunks`
- Disable vsync if input lag is an issue
- Check mod CPU usage in logs

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass: `./gradlew test`
6. Submit a pull request

## License

[Specify your license here]

## Credits

- **LWJGL**: OpenGL bindings for Java
- **Luaj**: Pure Java Lua interpreter
- **Kotlin**: Modern JVM language
- **GraalVM**: Native image compilation

## Roadmap

- [ ] Multiplayer support
- [ ] Advanced terrain generation (Perlin noise)
- [ ] Entity system
- [ ] Inventory and crafting
- [ ] GUI mod manager
- [ ] Shader mod support
- [ ] Resource pack system
- [ ] Sound system
- [ ] Particle effects

## Support

- **Issues**: [GitHub Issues](https://github.com/yourrepo/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourrepo/discussions)
- **Wiki**: [GitHub Wiki](https://github.com/yourrepo/wiki)
