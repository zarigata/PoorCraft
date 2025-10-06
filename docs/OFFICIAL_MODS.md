# Official PoorCraft Mods

PoorCraft ships with two showcase mods demonstrating the Lua modding workflow: a procedural block texture generator and an AI companion system. Both live in the `gamedata/mods/` directory and can be toggled or customized independently.

## Procedural Block Texture Generator (`gamedata/mods/block_texture_generator/`)

- **Purpose**: Placeholder for future procedural texture generation system.
- **Current Status**: This mod serves as a Lua implementation placeholder demonstrating mod structure and configuration. Full procedural texture generation requires image processing libraries not available in standard Lua. Future versions may implement this through Java-side image processing with Lua configuration.
- **Key Features**:
  - Demonstrates Lua mod lifecycle (`init()`, `enable()`, `disable()`)
  - Shows configuration loading via `api.get_mod_config()`
  - Template for future texture generation features
- **Configuration**: `gamedata/mods/block_texture_generator/mod.json` contains mod metadata and settings.
- **Docs**: See `gamedata/mods/block_texture_generator/README.md` for notes. Note that the README may reference the old Python implementation.

## AI NPC System (`gamedata/mods/ai_npc/`)

- **Purpose**: Placeholder for AI-powered NPC companion system.
- **Current Status**: This mod serves as a Lua implementation placeholder demonstrating NPC spawning and management. Full AI integration requires HTTP libraries not available in standard Lua. Future versions may implement this through Java-side HTTP handling with Lua scripting for NPC behavior.
- **Key Features**:
  - Demonstrates NPC spawning via `api.spawn_npc()`
  - Shows NPC lifecycle management and cleanup
  - Example of Lua mod initialization and configuration loading
  - Table-based state management for tracking spawned NPCs
- **Configuration**: `gamedata/mods/ai_npc/mod.json` defines mod metadata, spawn behavior, and personality settings.
- **Docs**: `gamedata/mods/ai_npc/README.md` covers the vision for this mod. Note that the README may reference the old Python implementation.

## Real-Time Synchronization (`gamedata/mods/realtime_sync/`)

- **Purpose**: Synchronizes in-game time with real-world time based on system clock.
- **Current Status**: Fully functional. Uses the new mod update mechanism to continuously sync game time with real-world time.
- **Key Features**:
  - Real-time synchronization using system clock
  - Configurable time scale multiplier (speed up or slow down time)
  - Adjustable sync interval for performance tuning
  - Debug logging for troubleshooting
  - Demonstrates the new `update(deltaTime)` lifecycle function
  - Placeholder configuration for future location-based and weather sync features
- **Configuration**: `gamedata/mods/realtime_sync/mod.json` contains sync settings:
  - `sync_enabled` - master toggle for time synchronization
  - `time_scale` - speed multiplier (1.0 = real-time, 2.0 = double speed)
  - `sync_interval` - seconds between sync updates (default: 60)
  - `debug_logging` - enable verbose logging
  - Future: `use_player_location`, `weather_sync_enabled`
- **Docs**: See `gamedata/mods/realtime_sync/README.md` for detailed information.
- **Technical Note**: This mod uses the new continuous update mechanism (`mod.update(deltaTime)`) introduced for mods that need per-frame execution.

## Managing Mods

- **Enable/Disable**: Set `enabled` to `false` in a mod's `mod.json`.
- **Customization**: Adjust JSON configs or extend the Lua modules for deeper changes.
- **Performance**: Some mods (like realtime_sync) use continuous updates and may have performance impact if configured with very low update intervals. Adjust `sync_interval` or similar settings if experiencing performance issues.
- **Learning**: These mods double as reference implementations for Lua mod structure and demonstrate how to integrate with the PoorCraft ModAPI using Lua.
