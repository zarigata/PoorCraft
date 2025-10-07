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

## AI NPC Companion (`gamedata/mods/ai_npc/`)

- **Purpose**: Interactive AI companion that responds to chat messages and comments on biome changes.
- **Current Status**: Fully functional with chat integration. Uses simple pattern matching for responses. Future versions may integrate with external AI services (Ollama, OpenAI, etc.).
- **Key Features**:
  - **Chat Interaction**: Responds to common greetings and questions ("hello", "help", "weather", etc.)
  - **Biome Awareness**: Automatically comments when player enters a new biome
  - **Smart Cooldown**: Prevents spam by limiting responses to once every 5 seconds
  - **Pattern Matching**: Uses keyword-based response system (extensible to external AI APIs)
  - **Update Loop**: Demonstrates continuous mod update mechanism for real-time biome tracking
  - **Chat API**: Showcases `api.register_chat_listener()` and `api.send_chat_message()`
- **Usage**:
  - Enable the mod in your mod list
  - Press `T` to open chat in-game
  - Type messages like "hello", "help", or "weather" to interact
  - Explore different biomes to trigger biome-specific responses
- **Configuration**: `gamedata/mods/ai_npc/mod.json` defines mod metadata and future AI provider settings:
  - `ai_provider` - "ollama" (planned for future)
  - `ollama_url` - "http://localhost:11434" (planned)
  - `model` - "llama2" (planned)
  - `max_npcs` - Maximum concurrent NPCs (planned)
  - `npc_response_timeout` - Response timeout in seconds (planned)
- **Technical Implementation**:
  - Uses `api.register_chat_listener()` to receive player messages
  - Tracks last biome with `api.get_current_biome()` in update loop
  - Sends responses via `api.send_chat_message()`
  - Implements cooldown timer to prevent message flooding
- **Future Plans**:
  - Integration with external AI services (Ollama, OpenAI, Gemini)
  - Voice-to-text support
  - Persistent conversation memory
  - Multiple NPC personalities
  - Quest system integration
- **Example Code**:
```lua
api.register_chat_listener(function(msg)
    if not msg.is_system then
        -- Process player message
        api.send_chat_message("Response message")
    end
end)

-- Track biome changes
local last_biome = api.get_current_biome()
function mod.update(delta_time)
    local current_biome = api.get_current_biome()
    if current_biome ~= last_biome then
        api.send_chat_message("Welcome to " .. current_biome .. "!")
        last_biome = current_biome
    end
end
```

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
