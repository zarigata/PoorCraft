# PoorCraft Modding Examples

Step-by-step tutorials for common modding tasks.

## Table of Contents

1. [Hello World](#hello-world)
2. [Block Logger](#block-logger)
3. [Custom Ore Generator](#custom-ore-generator)
4. [Player Tracker](#player-tracker)
5. [Terrain Modifier](#terrain-modifier)
6. [Anti-Grief Protection](#anti-grief-protection)

## Hello World

The simplest possible mod.

### Step 1: Create Directory

```bash
mkdir gamedata/mods/hello_world
cd gamedata/mods/hello_world
```

### Step 2: Create mod.json

```json
{
  "id": "hello_world",
  "name": "Hello World",
  "version": "1.0.0",
  "description": "My first mod",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true
}
```

### Step 3: Create main.lua

```lua
local mod = {}

function mod.init()
    api.log("Hello, PoorCraft!")
end

return mod
```

### Step 4: Run

Start PoorCraft and check console for "Hello, PoorCraft!" message.

---

## Block Logger

Logs all block placements and breaks.

### Complete Code

**mod.json**:
```json
{
  "id": "block_logger",
  "name": "Block Logger",
  "version": "1.0.0",
  "description": "Logs all block changes",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true,
  "config": {
    "log_to_file": false
  }
}
```

**main.lua**:
```lua
local mod = {}

function mod.init()
    api.log("Block Logger initialized")
    
    -- Register event handlers
    api.register_event("block_place", function(event)
        local block_name = event.block_type_id  -- Block type ID
        api.log("[PLACE] Block " .. block_name .. " at (" .. event.x .. ", " .. event.y .. ", " .. event.z .. ")")
        if event.player_id ~= -1 then
            api.log("  by player " .. event.player_id)
        end
    end)
    
    api.register_event("block_break", function(event)
        local block_name = event.block_type_id  -- Block type ID
        api.log("[BREAK] Block " .. block_name .. " at (" .. event.x .. ", " .. event.y .. ", " .. event.z .. ")")
        if event.player_id ~= -1 then
            api.log("  by player " .. event.player_id)
        end
    end)
end

return mod
```

### What It Does

- Logs every block placement with coordinates and block type
- Logs every block break with coordinates and block type
- Shows player ID if player-initiated
- Uses Lua event registration with `api.register_event()`

---

## Custom Ore Generator

Adds custom ore veins during chunk generation.

### Complete Code

**mod.json**:
```json
{
  "id": "ore_generator",
  "name": "Custom Ore Generator",
  "version": "1.0.0",
  "description": "Adds custom ore veins",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true,
  "config": {
    "ore_type": 2,
    "veins_per_chunk": 5,
    "vein_size": 8,
    "min_height": 5,
    "max_height": 50
  }
}
```

**main.lua**:
```lua
local mod = {}
local config = {}

function mod.init()
    config = api.get_mod_config() or {}
    api.log("Ore Generator initialized")
    
    -- Register chunk generation event
    api.register_event("chunk_generate", function(event)
        local veins_per_chunk = config.veins_per_chunk or 5
        
        -- Generate multiple ore veins
        for i = 1, veins_per_chunk do
            -- Random starting position
            local start_x = math.random(0, 15)
            local start_y = math.random(config.min_height or 5, config.max_height or 50)
            local start_z = math.random(0, 15)
            
            -- Generate vein
            local vein_size = config.vein_size or 8
            local ore_type = config.ore_type or 1  -- STONE type
            
            for j = 1, vein_size do
                -- Random walk from starting position
                local x = start_x + math.random(-2, 2)
                local y = start_y + math.random(-1, 1)
                local z = start_z + math.random(-2, 2)
                
                -- Bounds check
                if x >= 0 and x < 16 and y >= 0 and y < 256 and z >= 0 and z < 16 then
                    -- Only replace stone (type 1)
                    local current_block = api.get_block(event.chunk_x * 16 + x, y, event.chunk_z * 16 + z)
                    if current_block == 1 then
                        api.set_block(event.chunk_x * 16 + x, y, event.chunk_z * 16 + z, ore_type)
                    end
                end
            end
        end
    end)
end

return mod
```

### What It Does

- Generates ore veins during chunk generation
- Configurable ore type, vein count, size, and height range
- Uses random walk algorithm for natural-looking veins
- Only replaces stone blocks (preserves caves, etc.)

---

## Player Tracker

Tracks player join/leave and maintains online player list.

### Complete Code

**mod.json**:
```json
{
  "id": "player_tracker",
  "name": "Player Tracker",
  "version": "1.0.0",
  "description": "Tracks online players",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true,
  "server_only": true
}
```

**main.lua**:
```lua
local mod = {}
local online_players = {}

function mod.init()
    api.log("Player Tracker initialized")
    
    -- Register player join event
    api.register_event("player_join", function(event)
        local player_data = {
            username = event.username,
            join_time = os.time(),
            spawn_pos = {x = event.x, y = event.y, z = event.z}
        }
        online_players[event.player_id] = player_data
        
        api.log("Player " .. event.username .. " joined")
        api.log("  ID: " .. event.player_id)
        api.log("  Spawn: (" .. event.x .. ", " .. event.y .. ", " .. event.z .. ")")
        
        local count = 0
        for _ in pairs(online_players) do count = count + 1 end
        api.log("  Online players: " .. count)
    end)
    
    -- Register player leave event
    api.register_event("player_leave", function(event)
        if online_players[event.player_id] then
            local player_data = online_players[event.player_id]
            local session_duration = os.time() - player_data.join_time
            
            api.log("Player " .. event.username .. " left")
            api.log("  Session duration: " .. session_duration .. " seconds")
            
            online_players[event.player_id] = nil
        end
        
        local count = 0
        for _ in pairs(online_players) do count = count + 1 end
        api.log("  Online players: " .. count)
    end)
end

function mod.get_online_count()
    local count = 0
    for _ in pairs(online_players) do count = count + 1 end
    return count
end

function mod.get_online_players()
    local players = {}
    for _, data in pairs(online_players) do
        table.insert(players, data.username)
    end
    return players
end

return mod
```

### What It Does

- Tracks all online players in a dictionary
- Records join time and spawn position
- Calculates session duration on leave
- Provides functions to query online player count and list
- Server-only (doesn't run on clients)

---

## Terrain Modifier

Modifies terrain during generation to create custom features.

### Complete Code

**mod.json**:
```json
{
  "id": "terrain_modifier",
  "name": "Terrain Modifier",
  "version": "1.0.0",
  "description": "Adds custom terrain features",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true,
  "config": {
    "add_pillars": true,
    "pillar_chance": 0.1,
    "flatten_spawn": true
  }
}
```

**main.lua**:
```lua
local mod = {}
local config = {}
local world_spawn = {x = 0, z = 0}

function mod.init()
    config = api.get_mod_config() or {}
    api.log("Terrain Modifier initialized")
    
    -- Register world load event
    api.register_event("world_load", function(event)
        world_spawn = {x = 0, z = 0}  -- Default spawn
        api.log("World spawn set")
    end)
    
    -- Register chunk generation event
    api.register_event("chunk_generate", function(event)
        local chunk_x = event.chunk_x
        local chunk_z = event.chunk_z
        
        -- Flatten spawn area
        if config.flatten_spawn ~= false then
            if chunk_x == 0 and chunk_z == 0 then
                flatten_spawn_area(event)
            end
        end
        
        -- Add random stone pillars
        if config.add_pillars ~= false then
            if math.random() < (config.pillar_chance or 0.1) then
                add_pillar(event)
            end
        end
    end)
end

function flatten_spawn_area(event)
    -- Flatten center of spawn chunk
    for x = 6, 9 do
        for z = 6, 9 do
            local world_x = event.chunk_x * 16 + x
            local world_z = event.chunk_z * 16 + z
            
            -- Set to grass at Y=70
            for y = 0, 255 do
                if y < 70 then
                    api.set_block(world_x, y, world_z, 1)  -- STONE
                elseif y == 70 then
                    api.set_block(world_x, y, world_z, 2)  -- GRASS
                else
                    api.set_block(world_x, y, world_z, 0)  -- AIR
                end
            end
        end
    end
end

function add_pillar(event)
    -- Add a stone pillar at random position
    local x = math.random(0, 15)
    local z = math.random(0, 15)
    local world_x = event.chunk_x * 16 + x
    local world_z = event.chunk_z * 16 + z
    
    -- Find surface
    local surface_y = 70  -- Default
    for y = 255, 1, -1 do
        if api.get_block(world_x, y, world_z) ~= 0 then  -- Not AIR
            surface_y = y
            break
        end
    end
    
    -- Build pillar
    local height = math.random(10, 30)
    for y = surface_y + 1, surface_y + height do
        if y < 256 then
            api.set_block(world_x, y, world_z, 1)  -- STONE
        end
    end
end

return mod
```

### What It Does

- Flattens spawn area for easier building
- Adds random stone pillars to terrain
- Configurable pillar frequency
- Finds surface height automatically
- Demonstrates chunk modification during generation

---

## Anti-Grief Protection

Prevents certain destructive actions.

### Complete Code

**mod.json**:
```json
{
  "id": "anti_grief",
  "name": "Anti-Grief Protection",
  "version": "1.0.0",
  "description": "Prevents griefing",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true,
  "server_only": true,
  "config": {
    "protect_bedrock": true,
    "prevent_lava": false,
    "protect_spawn_radius": 50
  }
}
```

**main.lua**:
```lua
local mod = {}
local config = {}
local world_spawn = {x = 0, y = 70, z = 0}

function mod.init()
    config = api.get_mod_config() or {}
    api.log("Anti-Grief Protection initialized")
    
    -- Register block break event
    api.register_event("block_break", function(event)
        -- Protect bedrock
        if config.protect_bedrock ~= false then
            if event.block_type_id == 7 then  -- BEDROCK type ID
                event.cancelled = true
                api.log("Prevented bedrock break at (" .. event.x .. ", " .. event.y .. ", " .. event.z .. ")")
                return
            end
        end
        
        -- Protect spawn area
        if is_in_spawn_protection(event.x, event.z) then
            event.cancelled = true
            api.log("Prevented block break in spawn protection")
        end
    end)
    
    -- Register block place event
    api.register_event("block_place", function(event)
        -- Prevent lava placement (if configured)
        if config.prevent_lava then
            -- Check for lava block type (example)
            -- if event.block_type_id == LAVA_TYPE then
            --     event.cancelled = true
            -- end
        end
        
        -- Protect spawn area
        if is_in_spawn_protection(event.x, event.z) then
            event.cancelled = true
            api.log("Prevented block place in spawn protection")
        end
    end)
end

function is_in_spawn_protection(x, z)
    -- Check if coordinates are in spawn protection radius
    local radius = config.protect_spawn_radius or 50
    local dx = x - world_spawn.x
    local dz = z - world_spawn.z
    local distance = math.sqrt(dx * dx + dz * dz)
    return distance < radius
end

return mod
```

### What It Does

- Prevents bedrock from being broken
- Protects spawn area from modifications
- Configurable protection radius
- Can prevent dangerous block placement (lava, TNT, etc.)
- Server-only for security

---

## Next Steps

- Study the official mods (`block_texture_generator`, `ai_npc`) for advanced examples
- Read the [API Reference](API_REFERENCE.md) for complete API documentation
- Check the [Event Catalog](EVENT_CATALOG.md) for all available events
- Experiment with combining multiple events and features
- Share your mods with the community!
