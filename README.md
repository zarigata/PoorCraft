# PoorCraft

![Version](https://img.shields.io/badge/version-2.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Java](https://img.shields.io/badge/java-17%2B-orange)
![Tests](https://img.shields.io/badge/tests-automated-brightgreen)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey)

![alt text](src/main/resources/images/logo.png)

A free, open-source Minecraft clone that empowers players to create, modify and share their ideas without any financial or technical barriers. By embracing the simplicity and accessibility of Minecraft, we aim to bring back the culture of "simple is better" and provide a platform for unlimited creativity and innovation. Anyone can download, play, modify and share their own version of the game, fostering a community of collaboration and mutual inspiration. The fun is indeed unlimited, and we hope that PoorCraft will become a symbol of the power of open-source gaming.

## Features

### ⛏️ World & Gameplay
- ✅ **4 Biomes** - Desert, Snow, Jungle, Plains with unique terrain generation
- ✅ **Infinite World** - Chunk-based world with seed support and dynamic loading/unloading
- ✅ **Procedural Terrain** - Simplex noise-based height maps with biome-specific features
- ✅ **Biome Features** - Trees in plains/jungle, cacti in desert, snow layers in snow biome
- ✅ **Day/Night Cycle** - Dynamic lighting and sun position (600 second cycle)
- ✅ **Block Breaking & Placing** - Full voxel manipulation

### 🎨 Graphics & Performance
- ✅ **Frustum Culling** - Only renders chunks visible in camera view
- ✅ **Greedy Meshing** - Optimized chunk rendering (60-80% triangle reduction)
- ✅ **Lighting System** - Ambient + directional lighting with normal-based shading
- ✅ **Texture Atlas** - 16x16 block textures combined into efficient atlas
- ✅ **Vaporwave UI** - Animated gradient backgrounds and retro aesthetic
- ✅ **Pause Menu Blur** - Two-pass Gaussian blur effect
- ✅ **Responsive Layout** - Adapts to any window size

### 🎮 UI & Controls
- ✅ **UI System** - Main menu, settings, world creation, in-game HUD
- ✅ **Animated Menu Backgrounds** - 3D world rendering in menus (Minecraft-style)
- ✅ **Head Bobbing** - Customizable camera bobbing during movement
- ✅ **Configuration** - JSON-based settings with in-game editor
- ✅ **Silkscreen Font** - Retro pixel font for authentic aesthetic

### 🤖 AI Companion
- ✅ **Smart Companion NPC** – Spawns alongside the player and responds to directed chat
- ✅ **Action Commands** – "follow", "stop", and resource gathering requests become in-game tasks
- ✅ **Multi-Provider Support** – Switch between local Ollama or cloud providers (Gemini, OpenRouter)
- 📘 **Companion Guide** – See [docs/AI_COMPANION_GUIDE.md](docs/AI_COMPANION_GUIDE.md) for setup and usage

### 🌐 Multiplayer
- ✅ **Client-Server Architecture** - Authoritative server model
- ✅ **14 Packet Types** - Complete networking protocol
- ✅ **Integrated Server** - Host + play in same application
- ✅ **Direct Connect** - Join via IP address
- ✅ **Chunk Streaming** - On-demand world data transmission
- ✅ **Player Synchronization** - Smooth 60 FPS interpolation from 20 TPS server

### 🔧 Modding
- ✅ **Lua-based Modding System** - Easy and powerful mod creation
- ✅ **Event System** - Hook into game events (block place/break, chunk generate, etc.)
- ✅ **Comprehensive API** - 15+ functions for world access, time control, logging
- ✅ **Example Mods** - Included demonstrations of API usage
- 🚧 **Procedural Textures** - Placeholder (requires image processing library integration)

## Requirements

### Software
- **Java**: JDK 17 or higher ([Download](https://adoptium.net/))
- **Maven**: 3.6+ (for building from source)

### System
- **OS**: Windows 10+, Ubuntu 20.04+, or macOS 11+
- **CPU**: Dual-core 2.0 GHz (Quad-core 3.0 GHz recommended)
- **RAM**: 2GB minimum (4GB recommended)
- **GPU**: OpenGL 3.3+ compatible graphics card
- **Storage**: 500MB (1GB recommended)

## Quick Start

**Unified test & run (recommended):**

- **Windows:**
  ```bat
  scripts\unified-test-and-run.bat --mode dev
  ```
- **Linux/macOS:**
  ```bash
  ./scripts/unified-test-and-run.sh --mode dev
  ```

This workflow executes the pre-flight suite, optionally runs the full test suite, builds the game, and launches it. Add `--quick-tests` to run only the pre-flight checks, `--skip-tests` to jump straight to build/launch, or `--test-only` when you just want the validation outcomes.

**Manual testing checklist:**
See `docs/MANUAL_TESTING_GUIDE.md` for detailed steps, including the resize regression scenarios introduced in the latest verification pass.

## Building from Source

See [docs/BUILDING.md](docs/BUILDING.md) for detailed build instructions.

**Quick build:**
```bash
# Clone repository
git clone https://github.com/zarigata/poorcraft.git
cd poorcraft

# Build and run (Windows)
scripts\build-and-run.bat

# Build and run (Linux/macOS)
chmod +x scripts/build-and-run.sh
scripts/build-and-run.sh

# Or manually
mvn clean package
java -jar target/PoorCraft.jar
```

## Testing

PoorCraft ships with dedicated tooling for fast validation:

- **Pre-flight suite:** `mvn -Pquick-tests test` or run the unified script with `--quick-tests` to execute `PreFlightTestSuite` and produce reports via `TestReportGenerator`.
- **Full regression:** `mvn clean verify` or omit `--quick-tests` when using the unified workflow to follow the complete Surefire + Failsafe pipeline (including resize integration tests).
- **Manual verification:** Follow the updated scenarios in `docs/MANUAL_TESTING_GUIDE.md` to confirm window resizing, menu blur, and UI responsiveness.

Additional resources:
- `docs/TESTING.md` – Automated testing guide, including workflow and pre-flight details
- `docs/MANUAL_TESTING_GUIDE.md` – Manual testing checklist
- `scripts/README.md` – Script documentation and usage examples

Continuous integration runs the suite on GitHub Actions across Ubuntu, Windows, and macOS and publishes artifacts from `target/test-reports/` for inspection.

## World Generation

PoorCraft uses a sophisticated procedural generation system:

- **Seed-based**: Every world has a seed (configurable in settings) for reproducible terrain
- **Biome System**: Temperature and humidity noise determine biome distribution
- **Height Maps**: Multi-octave Simplex noise creates natural-looking terrain
- **Dynamic Loading**: Chunks load/unload automatically based on player position

For technical details on the world generation system, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Project Structure

- **gamedata/** - Runtime game data (NEW in v2.0)
  - **gamedata/mods/** - Lua mods
  - **gamedata/resourcepacks/** - Resource packs
  - **gamedata/worlds/** - World saves
  - **gamedata/screenshots/** - Screenshots
  - **gamedata/skins/** - Player skins
  - **gamedata/config/** - Configuration files
- **assets/** - Development assets
  - **assets/ui/** - UI textures
  - **assets/scripts/** - Utility scripts
- **scripts/** - Build and run scripts
- **docs/** - Documentation and modding guides
- **changelog/** - Release notes

**Note:** Empty directories are preserved in the repository with `.gitkeep` files. The game will automatically create missing directories at runtime.

## Modding

**PoorCraft v2.0 features a complete Lua-based modding system!** 🌙

Mods live in the `gamedata/mods/` directory and can hook into events, world data, and utility helpers. The Lua modding system replaces the previous Python framework for better portability and easier single-executable distribution.

### Features
- **Event-driven architecture** - Hook into game events (block place/break, chunk generate, world load)
- **Comprehensive API** - 15+ functions for world access, time control, player position, logging
- **Cross-mod communication** - Share state with other mods via `api.set_shared_data()` / `api.get_shared_data()`
- **Isolated environments** - Each mod has its own Lua globals to prevent conflicts
- **No external dependencies** - No Python required!

### Quick Example
```lua
local mod = {}

function mod.init()
    api.log("My mod loaded!")
    api.register_event('block_place', function(event)
        api.log("Block placed at " .. event.x .. ", " .. event.y .. ", " .. event.z)
    end)
end

return mod
```

### Documentation
- **[docs/MODDING_GUIDE.md](docs/MODDING_GUIDE.md)** - Getting started with Lua modding
- **[docs/API_REFERENCE.md](docs/API_REFERENCE.md)** - Complete Lua API documentation
- **[docs/EVENT_CATALOG.md](docs/EVENT_CATALOG.md)** - All available events
- **[docs/EXAMPLES.md](docs/EXAMPLES.md)** - Step-by-step Lua tutorials
- **[docs/OFFICIAL_MODS.md](docs/OFFICIAL_MODS.md)** - Official mod documentation

### Official Mods

PoorCraft ships with example mods to demonstrate the Lua modding system:

- **Example Mod** (`gamedata/mods/example_mod/`)
  - Simple demonstration of Lua mod structure
  - Shows basic API usage and mod lifecycle
- **Procedural Block Texture Generator** (`gamedata/mods/block_texture_generator/`)
  - Placeholder for procedural texture generation (requires image processing library integration with Java)
  - Demonstrates Lua mod structure and configuration

Configuration:
- Edit each mod's `mod.json` to configure settings
- Mods can be enabled/disabled via the `enabled` flag

See `docs/OFFICIAL_MODS.md` and `docs/MODDING_GUIDE.md` for more details.

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

**Quick links:**
- [Code of Conduct](CONTRIBUTING.md#code-of-conduct)
- [Development Setup](docs/BUILDING.md)
- [Architecture Guide](docs/ARCHITECTURE.md)
- [Modding Guide](docs/MODDING_GUIDE.md)

Contributions can include:
- 🐛 Bug fixes
- ✨ New features
- 📝 Documentation improvements
- 🎨 UI/UX enhancements
- 🔧 New mods

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and release notes.

**Latest version: 2.0.0** - Major Lua modding refactor and professional reorganization

### What's New in 2.0.0
- Complete migration from Python to Lua modding
- Reorganized folder structure (gamedata/, assets/, docs/)
- Professional documentation (CONTRIBUTING.md, ARCHITECTURE.md, BUILDING.md, DEPLOYMENT.md)
- Steam-ready build configuration
- Consolidated changelog and versioning

## UI Assets

PoorCraft uses a flexible UI asset system that supports both filesystem and classpath loading:

### Directory Structure
- `assets/ui/` - UI textures loaded from filesystem (buttons, panels, etc.)
- `src/main/resources/textures/ui/` - HUD textures in classpath (hotbar, hearts, armor, XP bar)

### Required Textures
**Button Texture** (`UI_FILES/button.png`):
- 64x16 pixels, Minecraft-style button texture
- Scales to any size without quality loss
- Falls back to procedural rendering if missing

**HUD Textures** (`src/main/resources/textures/ui/`):
- `hotbar_frame.png` (800x60) - Hotbar background frame
- `hotbar_slot.png` (48x48) - Individual inventory slot
- `hotbar_selection.png` (56x56) - Selected slot highlight
- `heart_full.png` (20x20) - Full health heart
- `heart_empty.png` (20x20) - Empty health heart
- `armor_full.png` (20x20) - Full armor icon
- `armor_empty.png` (20x20) - Empty armor icon
- `xp_bar_background.png` (360x10) - XP bar background
- `xp_bar_fill.png` (360x10) - XP bar fill

### Generating Textures
UI textures should be created manually using image editing software or are already included in the project. The texture generation scripts have been removed as part of the migration to a Lua-only modding system. See the **Required Textures** section above for texture specifications.

## UI Customization

PoorCraft offers extensive UI and visual effect customization through the settings menu:

### Graphics Settings
**Animated Menu Background**
- Enable/disable 3D world rendering in menu backgrounds
- Provides a living, Minecraft-style menu experience
- Can be disabled for better performance on low-end systems

**Menu Animation Speed** (0.5 - 2.0)
- Controls the speed of camera movement in animated backgrounds
- Default: 1.0 (normal speed)

**Head Bobbing**
- Enable/disable camera bobbing during player movement
- Adds immersion and visual feedback while walking/running

**Head Bobbing Intensity** (0.0 - 2.0)
- Controls the amplitude of head bobbing effect
- Default: 1.0 (normal intensity)

**UI Scale** (0.75 - 1.5)
- Adjusts the size of all UI elements
- Useful for different screen sizes and accessibility needs
- Default: 1.0 (100% scale)

**Pause Menu Blur**
- Enable/disable blur effect on game world when paused
- Improves menu readability and visual polish

### Performance Tips
For optimal performance on low-end systems:
- Disable **Animated Menu Background** to reduce GPU load in menus
- Set **UI Scale** to 0.75 for slightly better performance
- Disable **Head Bobbing** if experiencing motion sickness
- Reduce **Menu Animation Speed** for smoother animations
## Support

Need help or have questions?

- Documentation: Check [docs/](docs/) for comprehensive guides
  - [BUILDING.md](docs/BUILDING.md) - Build instructions
  - [ARCHITECTURE.md](docs/ARCHITECTURE.md) - System design
  - [MODDING_GUIDE.md](docs/MODDING_GUIDE.md) - Mod creation
  - [DEPLOYMENT.md](docs/DEPLOYMENT.md) - Distribution guide
- Bug Reports: [Open an issue on GitHub](https://github.com/zarigata/poorcraft/issues)
- Questions: Use GitHub Discussions
- Modding Help: See [docs/MODDING_GUIDE.md](docs/MODDING_GUIDE.md) and [docs/API_REFERENCE.md](docs/API_REFERENCE.md)

## Troubleshooting

### Game Won't Start / Crashes on Launch

1. **Mod initialization failures**
   - Look for `[LuaModContainer] Error initializing mod` entries in the console
   - Set `"enabled": false` in the offending mod's `mod.json` under `gamedata/mods/<mod>/`
   - Restart the game; other mods will continue loading

2. **Lua syntax errors**
   - Check for `LuaError` messages with file names and line numbers
   - Fix the Lua script or disable the mod until corrected

3. **Missing AI configuration**
   - Ensure provider credentials and local services are configured as described in [docs/AI_COMPANION_GUIDE.md](docs/AI_COMPANION_GUIDE.md)
   - Disable experimental mods via `mod.json` if issues persist

### Disabling Mods

1. Open `gamedata/mods/<mod_name>/mod.json`
2. Change `"enabled": true` to `"enabled": false`
3. Save and restart the game

### Test Mods

- `faulty_test_mod` – Intentionally throws an error to verify resilience
- `isolation_alpha` / `isolation_beta` – Validate Lua environment isolation
- `hi_mod` – Demonstration mod that logs a greeting

All are disabled by default. Enable only when testing the modding system.

### Mod Isolation Issues

- Each mod now runs inside its own Lua globals
- Use `api.set_shared_data()` / `api.get_shared_data()` for cross-mod communication
- Avoid relying on `_G` to share state between mods

### Getting Help

1. Review console logs for detailed error output
2. Read recent entries in `CHANGELOG.md` for known issues
3. Refer to `docs/MODDING_GUIDE.md` for modding best practices
4. Include full error logs when reporting bugs

## License

PoorCraft is licensed under the [MIT License](LICENSE.txt).

You are free to use, modify, and distribute this software under the terms of the MIT License. See the [LICENSE.txt](LICENSE.txt) file for the full license text.

## Acknowledgments

PoorCraft is built with the help of amazing open-source projects:

- **[LWJGL](https://www.lwjgl.org/)** - Lightweight Java Game Library for OpenGL bindings
- **[LuaJ](http://www.luaj.org/)** - Lua scripting engine for Java
- **[Netty](https://netty.io/)** - Asynchronous networking framework
- **[JOML](https://joml-ci.github.io/JOML/)** - Java OpenGL Math Library
- **[Gson](https://github.com/google/gson)** - JSON parsing library
- **Silkscreen Font** - Retro pixel font for UI
- **Minecraft** - Inspiration for voxel sandbox gameplay

Special thanks to:
- All contributors and mod creators
- The open-source community
- Everyone who plays and enjoys PoorCraft!

---

**Made with ❤️ by Zarigata | [GitHub](https://github.com/zarigata/poorcraft) | Version 2.0.0**
