# PoorCraft Lua Modding Guide v2.0

Welcome to PoorCraft Lua modding! This guide will help you create your first Lua mod.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Mod Structure](#mod-structure)
3. [Creating Your First Mod](#creating-your-first-mod)
4. [Event System](#event-system)
5. [World Access](#world-access)
6. [Configuration](#configuration)
7. [Best Practices](#best-practices)
8. [Examples](#examples)

## Getting Started

### Prerequisites

- PoorCraft game installed
- Basic Lua knowledge
- Text editor or IDE (VSCode with Lua extension recommended)

### Why Lua?

PoorCraft v2.0 switched from Python to Lua for modding:
- **Lightweight**: No external dependencies required
- **Portable**: Better for single-executable distribution
- **Fast**: Lua is designed for embedding in applications
- **Industry Standard**: Used in many games (World of Warcraft, Roblox, Garry's Mod)

### Setting Up Development Environment

1. Create a new mod directory:
   ```bash
   mkdir gamedata/mods/my_first_mod
   cd gamedata/mods/my_first_mod
   ```

2. Create required files:
   - `mod.json` - Mod metadata and configuration
   - `main.lua` - Main mod code

## Mod Structure

### Directory Layout

```
gamedata/mods/
└── my_first_mod/
    ├── mod.json             # Mod metadata and config
    ├── main.lua             # Main mod code
    └── resources/           # Optional: textures, data files
```

### mod.json Format

```json
{
  "id": "my_first_mod",
  "name": "My First Mod",
  "version": "1.0.0",
  "description": "A simple example mod",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true,
  "server_only": false,
  "config": {
    "custom_setting": "value"
  }
}
```

**Fields**:
- `id`: Unique mod identifier (lowercase, no spaces)
- `name`: Display name
- `version`: Semantic version (major.minor.patch)
- `description`: What your mod does
- `author`: Your name
- `main`: Lua script filename (default: "main.lua")
- `enabled`: Whether mod loads on startup
- `server_only`: If true, only loads on server (not clients)
- `config`: Custom configuration (accessible in Lua via api.get_mod_config())

## Creating Your First Mod

### Step 1: Create mod.json

Create `gamedata/mods/hello_world/mod.json`:

```json
{
  "id": "hello_world",
  "name": "Hello World",
  "version": "1.0.0",
  "description": "Logs messages when blocks are placed",
  "author": "You",
  "main": "main.lua",
  "enabled": true
}
```

### Step 2: Create main.lua

Create `gamedata/mods/hello_world/main.lua`:

```lua
local mod = {}

function mod.init()
    api.log("Hello World mod initialized!")
    
    -- Register event handler for block placement
    api.register_event('block_place', function(event)
        api.log("Block placed at " .. event.x .. ", " .. event.y .. ", " .. event.z)
        api.log("Block type: " .. event.block_type_id)
    end)
end

function mod.enable()
    api.log("Hello World mod enabled!")
end

function mod.disable()
    api.log("Hello World mod disabled")
end

return mod
```

### Step 3: Run the game

Start PoorCraft and your mod will load automatically!

## Event System

### Available Events

**Block Events**:
- `block_place` - When a block is placed
- `block_break` - When a block is broken

**Player Events**:
- `player_join` - When a player joins the server
- `player_leave` - When a player leaves the server

**World Events**:
- `chunk_generate` - When a chunk is generated
- `world_load` - When a world is loaded

### Registering Event Handlers

Use `api.register_event()` to listen for events:

```lua
api.register_event('block_place', function(event)
    api.log("Block placed!")
end)
```

### Event Properties

**BlockPlaceEvent / BlockBreakEvent**:
- `event.x`, `event.y`, `event.z` - Block coordinates
- `event.block_type_id` - Block type (0-255)
- `event.player_id` - Player who placed/broke block (-1 if not player)
- `event.cancel()` - Prevent block placement/breaking

**PlayerJoinEvent / PlayerLeaveEvent**:
- `event.player_id` - Unique player ID
- `event.username` - Player username
- `event.x`, `event.y`, `event.z` - Player position (join only)
- `event.reason` - Disconnect reason (leave only)

**ChunkGenerateEvent**:
- `event.chunk_x`, `event.chunk_z` - Chunk coordinates
- `event.chunk` - Chunk object (can modify blocks)

**WorldLoadEvent**:
- `event.seed` - World seed
- `event.generate_structures` - Whether structures are enabled

### Cancelling Events

Some events can be cancelled to prevent the action:

```lua
api.register_event('block_place', function(event)
    if event.block_type_id == 1 then  -- Dirt
        event.cancel()
        api.log("Dirt placement prevented!")
    end
end)
```

## World Access

### Getting/Setting Blocks

```lua
-- Get block at position
local block_id = api.get_block(100, 64, 200)

-- Set block at position
api.set_block(100, 64, 200, 2)  -- 2 = STONE
```

### Block Types

Block types are identified by numeric IDs:

```lua
local AIR = 0
local DIRT = 1
local STONE = 2
local GRASS = 3
local SAND = 4
-- ... see API reference for full list
```

### Biome Access

```lua
local biome = api.get_biome(100, 200)  -- Returns "Desert", "Snow", "Jungle", or "Plains"
```

### Terrain Height

```lua
local height = api.get_height_at(100, 200)  -- Returns Y coordinate of surface
```

## Configuration

### Accessing Config

Access your mod's configuration from `mod.json`:

```lua
function mod.init()
    -- Get mod config as JSON string
    local config_json = api.get_mod_config("my_mod")
    
    if config_json then
        api.log("Config loaded successfully")
        -- Parse JSON manually or use config values directly
    end
end
```

### Shared Data

Mods can share data with each other:

```lua
-- Store data
api.set_shared_data("my_mod.counter", 42)

-- Retrieve data
local counter = api.get_shared_data("my_mod.counter")  -- Returns 42
```

## Best Practices

### 1. Use Descriptive Names

```lua
-- Good
api.register_event('block_place', function(event)
    -- Clear handler purpose
end)

-- Bad
api.register_event('block_place', function(e)
    -- Unclear purpose
end)
```

### 2. Handle Errors Gracefully

```lua
api.register_event('block_place', function(event)
    local success, err = pcall(function()
        -- Your code here
    end)
    
    if not success then
        api.log("Error in handler: " .. tostring(err))
    end
end)
```

### 3. Check Server/Client Side

```lua
function mod.init()
    if api.is_server() then
        api.log("Running on server")
        -- Server-only code
    else
        api.log("Running on client")
        -- Client-only code
    end
end
```

### 4. Use Lifecycle Methods

**Standard lifecycle:**
- `init()` - called once on mod load
- `enable()` - called when mod is enabled
- `disable()` - called when mod is disabled

**Optional continuous update:**
- `update(deltaTime)` - called every frame (optional)

```lua
function mod.init()
    -- Setup: register events, load resources
    api.log("Initializing...")
end

function mod.enable()
    -- Start: begin mod functionality
    api.log("Enabled")
end

function mod.disable()
    -- Stop: cleanup, unregister events
    api.log("Disabled")
end

-- Optional: for continuous execution
function mod.update(deltaTime)
    -- Called every frame
    -- deltaTime is in seconds (e.g., 0.016 for 60 FPS)
    -- Use for animations, time sync, periodic checks
end
```

**Update function notes:**
- Only define `update()` if you need continuous execution
- Keep it lightweight - runs every frame!
- Use timers to limit expensive operations:

```lua
local timer = 0
function mod.update(deltaTime)
    timer = timer + deltaTime
    if timer >= 1.0 then  -- Run every second
        -- Do something expensive
        timer = 0
    end
end
```

- Mods without `update()` have zero performance overhead
- See `gamedata/mods/realtime_sync/` for a complete example

### 5. Log Important Events

```lua
api.log("Mod initialized")
api.log("Config loaded: " .. tostring(config))
api.log("Event triggered: " .. event.x .. ", " .. event.y)
```

## Examples

See the official mods for complete examples:

- **Example Mod** (`gamedata/mods/example_mod/`) - Basic Lua mod structure
- **Real-Time Sync** (`gamedata/mods/realtime_sync/`) - Time synchronization with update lifecycle
- **Block Texture Generator** (`gamedata/mods/block_texture_generator/`) - Procedural texture generation placeholder
- **AI NPC** (`gamedata/mods/ai_npc/`) - AI-powered NPCs placeholder

### Example: Block Logger

Logs all block placements and breaks:

```lua
local mod = {}

function mod.init()
    api.log("Block Logger initialized")
    
    -- Register block place handler
    api.register_event('block_place', function(event)
        api.log("Block " .. event.block_type_id .. " placed at (" .. 
                event.x .. ", " .. event.y .. ", " .. event.z .. ")")
    end)
    
    -- Register block break handler
    api.register_event('block_break', function(event)
        api.log("Block " .. event.block_type_id .. " broken at (" .. 
                event.x .. ", " .. event.y .. ", " .. event.z .. ")")
    end)
end

function mod.enable()
    api.log("Block Logger enabled")
end

function mod.disable()
    api.log("Block Logger disabled")
end

return mod
```

## Next Steps

- Read the [API Reference](API_REFERENCE.md) for complete API documentation
- Check the [Event Catalog](EVENT_CATALOG.md) for all available events
- Study the official mods for advanced examples
- Join the community to share your mods!

## Troubleshooting

**Mod not loading?**
- Check mod.json syntax (use JSON validator)
- Verify `"main": "main.lua"` in mod.json
- Check console for error messages
- Ensure mod.lua returns the mod table

**Events not firing?**
- Ensure `api.register_event()` is called in `mod.init()`
- Check if mod is enabled in mod.json (`"enabled": true`)
- Verify event names are correct (lowercase, underscore-separated)

**Lua syntax errors?**
- Check for missing `end` statements
- Verify string concatenation uses `..` not `+`
- Use `local` for variables to avoid global scope pollution
- Remember Lua arrays start at index 1, not 0

**API not working?**
- Ensure you're calling `api.function_name()` not `function_name()`
- Check the API_REFERENCE.md for correct function signatures
- Verify the API function exists in your PoorCraft version

**Need help?**
- Check console logs for Lua error messages
- Read the API reference documentation
- Study the official mod examples in `gamedata/mods/`
