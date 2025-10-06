# PoorCraft Modding API Reference

Complete reference for the PoorCraft Lua modding API.

## Table of Contents

1. [Core API](#core-api)
2. [Event System](#event-system)
3. [World Access](#world-access)
4. [Entity Management](#entity-management)
5. [Utility Functions](#utility-functions)

## Core API

### api (Global Lua Object)

The global `api` object provides all mod functionality. All functions are called as `api.function_name()`.

#### Functions

**`api.get_block(x, y, z)`**
- Parameters:
  - `x` (number): X coordinate
  - `y` (number): Y coordinate
  - `z` (number): Z coordinate
- Returns: number (block type ID, 0-255)
- Description: Get block type at world coordinates
- Example: `local block = api.get_block(100, 64, 200)`

**`api.set_block(x, y, z, block_type_id)`**
- Parameters:
  - `x` (number): X coordinate
  - `y` (number): Y coordinate
  - `z` (number): Z coordinate
  - `block_type_id` (number): Block type to set (0-255)
- Returns: nil
- Description: Set block type at world coordinates
- Example: `api.set_block(100, 64, 200, 2)  -- Set to STONE`

**`api.get_biome(x, z)`**
- Parameters:
  - `x` (number): X coordinate
  - `z` (number): Z coordinate
- Returns: string ("Desert", "Snow", "Jungle", or "Plains")
- Description: Get biome at coordinates
- Example: `local biome = api.get_biome(100, 200)`

**`api.get_height_at(x, z)`**
- Parameters:
  - `x` (number): X coordinate
  - `z` (number): Z coordinate
- Returns: number (Y coordinate of surface)
- Description: Get terrain height at coordinates
- Example: `local height = api.get_height_at(100, 200)`

**`api.log(message)`**
- Parameters:
  - `message` (string): Message to log
- Returns: nil
- Description: Log message to console with [MOD] prefix
- Example: `api.log("Hello from my mod!")`

**`api.is_server()`**
- Returns: boolean
- Description: Check if running on server side
- Example: `if api.is_server() then ... end`

**`api.set_shared_data(key, value)`**
- Parameters:
  - `key` (string): Data key
  - `value` (any): Data value (string, number, table, etc.)
- Returns: nil
- Description: Store data accessible to all mods
- Example: `api.set_shared_data("my_mod.counter", 42)`

**`api.get_shared_data(key)`**
- Parameters:
  - `key` (string): Data key
- Returns: any (stored value or nil)
- Description: Retrieve shared data
- Example: `local counter = api.get_shared_data("my_mod.counter")`

**`api.get_mod_config(mod_id)`**
- Parameters:
  - `mod_id` (string): Mod identifier
- Returns: string (JSON config) or nil
- Description: Get mod configuration from mod.json as JSON string (legacy)
- Example: `local config = api.get_mod_config("my_mod")`
- **Note**: Consider using `api.get_mod_config_table()` for easier access

**`api.get_mod_config_table(mod_id)`**
- Parameters:
  - `mod_id` (string): Mod identifier
- Returns: table (parsed config) or empty table
- Description: Get mod configuration as a Lua table with direct field access. Automatically handles nested objects and arrays.
- Example:
```lua
local config = api.get_mod_config_table("realtime_sync")
if config.sync_enabled then
    api.log("Sync interval: " .. config.sync_interval)
end
```

**`api.get_player_position()`**
- Parameters: none
- Returns: table with `x`, `y`, `z` fields (numbers), or nil if player not available
- Description: Get the current player's position in world coordinates. Returns nil when the player controller is not initialized (e.g., before world load).
- Example:
```lua
local pos = api.get_player_position()
if pos then
    api.log("Player at: " .. pos.x .. ", " .. pos.y .. ", " .. pos.z)
else
    api.log("Player not available")
end
```

**`api.get_game_time()`**
- Parameters: none
- Returns: number (0.0-1.0 representing time of day), or -1 if not available
- Description: Get current in-game time (0.0 = midnight, 0.25 = sunrise, 0.5 = noon, 0.75 = sunset, 1.0 = midnight)
- Example:
```lua
local time = api.get_game_time()
api.log("Game time: " .. time)
```

**`api.set_game_time(time)`**
- Parameters:
  - `time` (number): Time of day (0.0-1.0)
- Returns: nil
- Description: Set the in-game time of day. Updates lighting automatically.
- Example:
```lua
api.set_game_time(0.5)  -- Set to noon
```

**`api.set_time_control_enabled(enabled)`**
- Parameters:
  - `enabled` (boolean): true to disable automatic time progression, false to enable
- Returns: nil
- Description: Controls whether the game automatically advances time each frame. When enabled (true), automatic time progression stops, allowing mods to fully control time. When disabled (false), normal time progression resumes. Useful for mods that sync game time with real-world time or custom time systems.
- Example:
```lua
-- In mod enable():
api.set_time_control_enabled(true)  -- Take control of time

-- In mod disable():
api.set_time_control_enabled(false)  -- Restore auto progression
```

**`api.get_real_time()`**
- Parameters: none
- Returns: number (milliseconds since Unix epoch)
- Description: Get current real-world system time for synchronization purposes
- Example:
```lua
local millis = api.get_real_time()
api.log("Real time: " .. millis)
```

**`api.get_weather()`**
- Parameters: none
- Returns: string (weather status), currently always "clear"
- Description: Get current weather status. Placeholder for future weather system.
- Example:
```lua
local weather = api.get_weather()
api.log("Weather: " .. weather)
```

## Event System

### Event Registration

Use `api.register_event()` to listen for game events. Events are identified by string names.

#### Available Events

**`block_place`**
- Event Object: BlockPlaceEvent
- Description: Called when a block is placed
- Cancellable: Yes
- Example:
```lua
api.register_event('block_place', function(event)
    api.log("Block placed at " .. event.x .. ", " .. event.y .. ", " .. event.z)
end)
```

**`block_break`**
- Event Object: BlockBreakEvent
- Description: Called when a block is broken
- Cancellable: Yes
- Example:
```lua
api.register_event('block_break', function(event)
    api.log("Block broken!")
end)
```

**`player_join`**
- Event Object: PlayerJoinEvent
- Description: Called when a player joins
- Cancellable: No
- Example:
```lua
api.register_event('player_join', function(event)
    api.log(event.username .. " joined the game")
end)
```

**`player_leave`**
- Event Object: PlayerLeaveEvent
- Description: Called when a player leaves
- Cancellable: No

**`chunk_generate`**
- Event Object: ChunkGenerateEvent
- Description: Called when a chunk is generated
- Cancellable: No

**`world_load`**
- Event Object: WorldLoadEvent
- Description: Called when a world is loaded
- Cancellable: No

#### Functions

**`api.register_event(event_name, callback)`**
- Parameters:
  - `event_name` (string): Event name (see list above)
  - `callback` (function): Function to call when event fires
- Returns: nil
- Description: Register event handler
- Example:
```lua
api.register_event('block_place', function(event)
    if event.block_type_id == 1 then
        api.log("Dirt placed!")
    end
end)
```

**`api.unregister_event(event_name, callback)`**
- Parameters:
  - `event_name` (string): Event name
  - `callback` (function): Function to unregister
- Returns: nil
- Description: Unregister event handler (rarely needed)

## World Access

### Block Types

Block types are identified by numeric IDs. Define constants in your mod for readability:

```lua
local BlockType = {
    AIR = 0,
    DIRT = 1,
    STONE = 2,
    GRASS = 3,
    SAND = 4,
    SANDSTONE = 5,
    SNOW_BLOCK = 6,
    ICE = 7,
    JUNGLE_GRASS = 8,
    JUNGLE_DIRT = 9,
    WOOD = 10,
    LEAVES = 11,
    CACTUS = 12,
    SNOW_LAYER = 13,
    BEDROCK = 14
}

-- Usage
api.set_block(100, 64, 200, BlockType.STONE)
```

### World Functions

All world access is done through the global `api` object:

**Getting Blocks:**
```lua
local block_id = api.get_block(x, y, z)
if block_id == 2 then
    api.log("Found stone!")
end
```

**Setting Blocks:**
```lua
api.set_block(x, y, z, 1)  -- Place dirt
```

**Biome Information:**
```lua
local biome = api.get_biome(x, z)
api.log("Biome: " .. biome)
```

**Terrain Height:**
```lua
local surface_y = api.get_height_at(x, z)
api.log("Surface at Y: " .. surface_y)
```

### Chunk Access

Chunk manipulation is primarily done through events (see `chunk_generate` event). Direct chunk access methods may be added in future versions.

## Entity Management

### NPC Functions

NPC management is handled through the API. Entity system is primarily Java-side with Lua hooks.

**`api.spawn_npc(npc_id, name, x, y, z, personality)`**
- Parameters:
  - `npc_id` (number): Unique NPC identifier
  - `name` (string): NPC display name
  - `x`, `y`, `z` (number): Spawn position
  - `personality` (string): NPC personality/behavior type
- Returns: nil
- Description: Spawn an NPC at the specified position
- Example:
```lua
api.spawn_npc(1, "Steve", 100.0, 65.0, 100.0, "friendly villager")
```

**`api.despawn_npc(npc_id)`**
- Parameters:
  - `npc_id` (number): NPC identifier to remove
- Returns: nil
- Description: Remove an NPC from the world
- Example:
```lua
api.despawn_npc(1)
```

### Player Information

Player data is accessible through events. See `player_join` and `player_leave` events for player information.

### Future Expansion

Additional entity management functions (teleportation, inventory access, etc.) may be added in future versions. Check the event system for entity-related events.

## Mod Structure

### Lua Mod Pattern

All Lua mods should follow this structure:

```lua
-- Create mod table
local mod = {}

-- Initialize function (called when mod loads)
function mod.init()
    api.log("Initializing mod...")
    
    -- Load config
    local config = api.get_mod_config("my_mod")
    
    -- Register events
    api.register_event('block_place', function(event)
        api.log("Block placed!")
    end)
end

-- Enable function (called when mod is enabled)
function mod.enable()
    api.log("Mod enabled")
end

-- Disable function (called when mod is disabled)
function mod.disable()
    api.log("Mod disabled")
    -- Cleanup code here
end

-- Update function (optional, called every frame)
function mod.update(deltaTime)
    -- deltaTime is in seconds (e.g., 0.016 for 60 FPS)
    -- Use this for continuous logic like animations or time sync
end

-- Return mod table so functions can be called
return mod
```

### Lifecycle Methods

- **`mod.init()`**: Called once when the mod is first loaded. Use this to register events and load configuration.
- **`mod.enable()`**: Called when the mod is enabled. Use this to start mod functionality.
- **`mod.disable()`**: Called when the mod is disabled. Use this for cleanup (despawning entities, etc.).
- **`mod.update(deltaTime)`** (Optional): Called every frame if defined. The `deltaTime` parameter is the time since last frame in seconds (typically ~0.016 for 60 FPS). Use this for continuous mod logic that needs per-frame execution (animations, time synchronization, etc.). Keep update functions lightweight to avoid performance issues. Mods without an update function have zero performance overhead.

**Update Function Example**:
```lua
local timer = 0
function mod.update(deltaTime)
    timer = timer + deltaTime
    if timer >= 1.0 then  -- Run every second
        api.log("One second passed")
        timer = 0
    end
end
```

### Module Organization

For larger mods, you can organize code into multiple files:

```lua
-- main.lua
local helpers = require("my_mod.helpers")
local mod = {}

function mod.init()
    helpers.setup()
end

return mod
```

## Event Objects

### BlockPlaceEvent

Accessed in Lua as a table with these fields:

- `event.x`, `event.y`, `event.z` (number): Block coordinates
- `event.block_type_id` (number): Block type being placed (0-255)
- `event.player_id` (number): Player placing block (-1 if not player)
- `event.cancel()` (function): Call to prevent block placement

Example:
```lua
api.register_event('block_place', function(event)
    if event.block_type_id == 1 then  -- Dirt
        event.cancel()
        api.log("Dirt placement prevented!")
    end
end)
```

### BlockBreakEvent

Accessed in Lua as a table with these fields:

- `event.x`, `event.y`, `event.z` (number): Block coordinates
- `event.block_type_id` (number): Block type being broken (0-255)
- `event.player_id` (number): Player breaking block (-1 if not player)
- `event.cancel()` (function): Call to prevent block breaking

### PlayerJoinEvent

Accessed in Lua as a table with these fields:

- `event.player_id` (number): Player ID
- `event.username` (string): Player username
- `event.x`, `event.y`, `event.z` (number): Spawn position

Example:
```lua
api.register_event('player_join', function(event)
    api.log(event.username .. " joined at " .. event.x .. ", " .. event.y .. ", " .. event.z)
end)
```

### PlayerLeaveEvent

Accessed in Lua as a table with these fields:

- `event.player_id` (number): Player ID
- `event.username` (string): Player username
- `event.reason` (string): Disconnect reason

### ChunkGenerateEvent

Accessed in Lua as a table with these fields:

- `event.chunk_x`, `event.chunk_z` (number): Chunk coordinates
- `event.chunk` (userdata): Chunk object (advanced usage)

### WorldLoadEvent

Accessed in Lua as a table with these fields:

- `event.seed` (number): World seed
- `event.generate_structures` (boolean): Whether structures are enabled

## Type Reference

### Block Type IDs

| ID | Name | Description |
|----|------|-------------|
| 0 | AIR | Empty space |
| 1 | DIRT | Dirt block |
| 2 | STONE | Stone block |
| 3 | GRASS | Grass block (plains) |
| 4 | SAND | Sand block (desert) |
| 5 | SANDSTONE | Sandstone block |
| 6 | SNOW_BLOCK | Snow block |
| 7 | ICE | Ice block |
| 8 | JUNGLE_GRASS | Jungle grass |
| 9 | JUNGLE_DIRT | Jungle dirt |
| 10 | WOOD | Wood log |
| 11 | LEAVES | Tree leaves |
| 12 | CACTUS | Cactus |
| 13 | SNOW_LAYER | Snow layer |
| 14 | BEDROCK | Bedrock (indestructible) |

### Biome Names

- `"Desert"` - Hot, dry, sandy terrain
- `"Snow"` - Cold, icy, snowy terrain
- `"Jungle"` - Hot, wet, dense vegetation
- `"Plains"` - Temperate, grassy, rolling hills
