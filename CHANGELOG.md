# PoorCraft Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Mod Loading System Resilience**
  - Fixed Lua mod initialization so single failures no longer crash the game
  - Removed exception re-throwing in `LuaModContainer.init()` and `enable()` allowing graceful degradation
  - Game now continues startup even when some mods enter the ERROR state
  - Added summary logging to highlight failing mods
- **Lua Environment Isolation**
  - Implemented per-mod Lua globals to prevent shared state conflicts
  - Ensured `LuaModContainer` provisions a fresh API bridge for each mod
  - Isolation test mods now validate separate environments without interference
- **Test Mod Configuration**
  - Disabled `faulty_test_mod`, `isolation_alpha`, `isolation_beta`, and `hi_mod` by default
  - Added descriptions clarifying their testing purpose and usage guidance
- **Hi Mod Performance**
  - Removed blocking countdown loop from `hi_mod` initialization
  - Prevents main thread stalls during mod enabling
- **UI Rendering System Improvements**
  - FontRenderer now uses dedicated VAO/VBO instead of clobbering UIRenderer buffers
  - Fixed text double-scaling issue by adding `getTextScaleForFontSize()` to normalize atlas size selection
  - ChatOverlay and ConsoleOverlay now dynamically calculate max visible lines based on scaled line height
  - Settings UI Scale slider now provides live preview with instant layout refresh
  - All UI screens migrated to use UIScaleManager for consistent percentage-based sizing

### Changed
- **UI Screen Constructor API**: All UI screens now require `UIScaleManager` parameter in constructor. External instantiations must pass scaleManager for proper scaling support.

## [2.0.0] - 2025-01-XX (Major Refactor)

### Added
- **Lua-based modding system** replacing Python framework
  - LuaModLoader with LuaJ 3.0.1 integration
  - Complete Lua API with 15+ functions
  - Event-driven architecture with cancellable events
  - Isolated Lua environments per mod
  - Example mods demonstrating API usage
- **Real-time synchronization mod** for time/weather sync across game sessions
- **Professional documentation structure**
  - CONTRIBUTING.md with code style and PR guidelines
  - docs/ARCHITECTURE.md with system design and component overview
  - docs/BUILDING.md with comprehensive build instructions
  - docs/DEPLOYMENT.md with packaging and distribution guide
- **Package-info.java documentation** for major packages (core, world, network, modding, ui, render)
- **Steam-ready build configuration** with proper metadata and versioning
- **Consolidated changelog** (this file) replacing scattered documentation

### Changed
- **Reorganized folder structure** for better clarity
  - Created `gamedata/` directory for runtime data (mods, worlds, config, screenshots, skins)
  - Created `assets/` directory for development assets (UI textures, scripts)
  - Moved `UPDATES/` to `changelog/` directory
  - Archived planning documents to `docs/archive/`
- **Migrated all mods from Python to Lua**
  - example_mod, block_texture_generator, ai_npc
  - No Python installation required
  - Faster startup without Py4J bridge overhead
- **Updated all documentation for Lua modding**
  - Rewritten MODDING_GUIDE.md
  - Updated API_REFERENCE.md
  - Created MIGRATION_GUIDE_v2.0.md for Python-to-Lua conversion
- **Version numbering aligned to 2.0.0** across pom.xml, build scripts, and Launch4j config
- **Updated project metadata** in pom.xml with Steam-ready descriptions and URLs

### Removed
- **Python modding infrastructure**
  - Py4JBridge and Python-based ModLoader
  - Python requirements.txt and dependencies
  - Python utility scripts (moved to Lua or archived)
- **Scattered planning documents** (moved to docs/archive/)
  - REFACTOR_PLAN.md, REFACTOR_SUMMARY.md
  - UPGRADE_TO_V2.md, MIGRATION_GUIDE_v2.0.md
  - SINGLE_EXE_IMPLEMENTATION.md, EXECUTABLE_GUIDE.md
  - DISCORD_INTEGRATION_SUMMARY.md

## [0.1.1] - 2025-01-XX (Multiplayer & UI Polish)

### Added
- **Complete multiplayer system** with client-server architecture
  - 14 packet types for communication (handshake, login, keep-alive, chunk data, player movement, block updates)
  - Netty-based TCP networking with 4.1.100.Final
  - Authoritative server model running at 20 TPS
  - Thin client with server-authoritative world state
- **Integrated server support** (host + play in same JVM)
  - Automatic localhost connection
  - Background server threads alongside client
- **Direct connect functionality** with IP/port entry
- **Chunk streaming** from server to clients
  - On-demand chunk requests
  - Length-prefixed framing (4-byte header)
  - Custom ByteBuf serialization
- **Player synchronization** with interpolation
  - Remote player rendering
  - Smooth 60 FPS interpolation from 20 TPS server updates
  - Player spawn/despawn notifications
- **Block update synchronization**
  - Server validates all block changes
  - Broadcast to all clients in range
- **Multiplayer UI screens**
  - MultiplayerMenuScreen (server list, direct connect, host)
  - ConnectingScreen (connection loading)
  - HostingScreen (server startup loading)
- **Keep-alive system** (15s interval, 30s timeout)
- **Packet registry** with type mapping and factory

### Changed
- **Updated Game.java** for multiplayer mode support
  - Added setWorld() method for server-provided worlds
  - Multiplayer mode flag
  - Network integration
- **Enhanced Camera.java** with getYaw() and getPitch() methods for networking
- **Added multiplayer settings** to Settings.java
  - MultiplayerSettings class
  - Configuration in default_settings.json
- **Updated GameState** with MULTIPLAYER_MENU, CONNECTING, HOSTING states

### Fixed
- Player session management and cleanup
- Graceful disconnect handling
- Connection error messaging

## [0.1.0] - 2025-01-XX (UI Redesign & Features)

### Added
- **Vaporwave aesthetic menu system**
  - Gradient backgrounds (pink/purple → cyan/blue)
  - Electric cyan and hot pink color palette
  - Deep purple and indigo accents
- **VaporwaveButton component** with animated effects
  - Multi-layer glow borders with blur effect
  - Smooth hover transitions with color interpolation
  - Pulsing animation on hover (sine wave)
  - Text shadows for readability
  - Disabled state support
- **Silkscreen retro font integration** (20px base size)
  - Pixel-perfect rendering
  - STB TrueType atlas (512x512px, ASCII 32-126)
  - Fallback to system font if unavailable
- **Pause menu blur effect**
  - Two-pass Gaussian blur (horizontal + vertical)
  - BlurRenderer class for post-processing
  - Half-resolution blur buffers for performance
  - 9-tap Gaussian kernel
  - Controlled by settings.graphics.pauseMenuBlur
  - Graceful fallback to dark overlay
- **Responsive layout system** adapting to any window size
  - Buttons scale between 280px-500px width
  - Height scales between 52px-80px
  - Proportional spacing based on button size
  - Smart sizing constraints (min/max bounds)
  - Perfect centering at all resolutions
- **Animated scanline effects** for retro CRT look
  - Main menu: horizontal pink line moving down (50px/s)
  - Pause menu: pulsing cyan line (30px/s)
  - Semi-transparent overlays
- **Discord Rich Presence integration**
  - Activity display in Discord
  - Game state tracking
- **Skin manager and editor screens**
  - Player skin selection
  - Skin customization interface
- **Head bobbing camera effect**
  - Customizable intensity (0.0-2.0)
  - Enable/disable toggle in settings
- **Performance monitoring system**
  - FPS display
  - Performance metrics
- **GPU capabilities detection**
  - OpenGL version checking
  - FBO support validation
- **Animated menu background** with 3D world rendering
  - Living Minecraft-style menus
  - Configurable animation speed (0.5-2.0)
  - Optional for low-end systems

### Changed
- **Complete MainMenuScreen redesign**
  - Vaporwave aesthetic with gradient backgrounds
  - Responsive button layout
  - Title and subtitle with retro colors
  - Footer with purple tint
- **Enhanced PauseScreen** with matching Vaporwave design
  - Semi-transparent gradient overlay
  - Adjusted alpha based on blur state (0.40f with blur, 0.88f without)
  - "Press ESC to resume" hint
- **Improved button positioning and sizing logic**
  - Adaptive calculations based on window dimensions
  - Breakpoint behavior for different screen sizes
  - Consistent spacing across resolutions
- **Updated UIManager** with better state management
  - Blur resource initialization
  - Resize handling for blur targets
  - State machine for game states
- **Added UI scale setting** (0.75-1.5)
  - Adjusts all UI elements
  - Accessibility and screen size support

### Fixed
- **Window resize handling** for all UI elements
  - Proper layout recalculation
  - Blur FBO recreation on resize
  - Viewport restoration after blur passes
- **Pause menu rendering issues**
  - GL state management (depth testing, blending)
  - Framebuffer binding/unbinding
  - Proper render order (world → blur → UI → pause panel)
- **Proportional scaling** across different resolutions
  - Consistent appearance from 800x600 to 4K
  - No distortion or awkward scaling
- **Menu verification and testing issues**
  - Shader compilation error handling
  - FBO validation and fallback
  - Missing uniform logging

---

For detailed technical changes, see `changelog/archive/` for historical documentation.

Version 2.0.0 is the first Steam-ready release with professional documentation and organization.
