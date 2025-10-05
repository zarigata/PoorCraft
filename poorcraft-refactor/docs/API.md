# PoorCraft Refactor - Lua Mod API Documentation

This document describes the complete Lua API available to mods in PoorCraft Refactor.

## Table of Contents

- [Mod Structure](#mod-structure)
- [Lifecycle Hooks](#lifecycle-hooks)
- [Engine API](#engine-api)
- [Logger API](#logger-api)
- [FileAPI](#fileapi)
- [Events](#events)
- [Best Practices](#best-practices)
- [Examples](#examples)

## Mod Structure

### Directory Layout

```
mods/
└── your_mod/
    ├── mod.json        # Required: Mod manifest
    ├── main.lua        # Required: Main script (or as specified in manifest)
    └── data/           # Optional: Mod data files
```

### mod.json Format

```json
{
  "id": "your_mod",
  "name": "Your Mod Name",
  "version": "1.0.0",
  "author": "Your Name",
  "main": "main.lua",
  "entry": "onEnable"
}
```

**Fields**:
- `id` (required): Unique identifier for your mod (lowercase, no spaces)
- `name` (required): Display name of your mod
- `version` (required): Semantic version (e.g., "1.0.0")
- `author` (optional): Your name or username
- `main` (optional): Main script file name (default: "main.lua")
- `entry` (optional): Entry point function name (default: "onEnable")

## Lifecycle Hooks

Mods can implement these lifecycle functions:

### onLoad()

Called when the mod is first loaded, before the engine is fully initialized.

```lua
function onLoad()
  log:info("Mod is loading")
  -- Initialize mod data structures
end
```

### onEnable()

Called when the mod is enabled. This is the main entry point for most mods.

```lua
function onEnable()
  log:info("Mod is enabled")
  -- Register event listeners
  -- Set up mod functionality
end
```

### onDisable()

Called when the mod is disabled or the engine is shutting down.

```lua
function onDisable()
  log:info("Mod is disabled")
  -- Clean up resources
  -- Save mod data
end
```

### onTick()

Called every game tick (20 times per second by default).

```lua
function onTick()
  -- Perform per-tick logic
  -- Keep this function fast!
end
```

**Warning**: Keep `onTick()` lightweight. Heavy operations should be scheduled or throttled.

## Engine API

The `Engine` global provides core engine functionality.

### Engine:registerEvent(eventName, callback)

Register a callback for a specific event.

**Parameters**:
- `eventName` (string): Name of the event to listen for
- `callback` (function): Function to call when event fires

**Returns**: nil

**Example**:
```lua
engine:registerEvent("onPlayerJoin", function(player)
  log:info("Player joined: " .. player:name())
end)
```

### Engine:schedule(delayTicks, callback)

Schedule a callback to run after a specified number of ticks.

**Parameters**:
- `delayTicks` (number): Number of ticks to wait
- `callback` (function): Function to call after delay

**Returns**: nil

**Example**:
```lua
-- Run after 3 seconds (60 ticks)
engine:schedule(60, function()
  log:info("3 seconds have passed!")
end)
```

### Engine:getTime()

Get the current game time in ticks.

**Returns**: number - Current tick count

**Example**:
```lua
local currentTick = engine:getTime()
log:info("Current tick: " .. currentTick)
```

### Engine:sendChat(player, message)

Send a chat message to a player.

**Parameters**:
- `player` (userdata): Player object
- `message` (string): Message to send

**Returns**: nil

**Example**:
```lua
engine:sendChat(player, "Welcome to the server!")
```

### Engine:createBlock(x, y, z, blockId)

Create a block at the specified world coordinates.

**Parameters**:
- `x` (number): World X coordinate
- `y` (number): World Y coordinate
- `z` (number): World Z coordinate
- `blockId` (number): Block type ID

**Returns**: nil

**Example**:
```lua
-- Place a stone block at (10, 64, 10)
engine:createBlock(10, 64, 10, 3)
```

### Engine:removeBlock(x, y, z)

Remove a block at the specified world coordinates.

**Parameters**:
- `x` (number): World X coordinate
- `y` (number): World Y coordinate
- `z` (number): World Z coordinate

**Returns**: nil

**Example**:
```lua
engine:removeBlock(10, 64, 10)
```

### Engine:getBlock(x, y, z)

Get information about a block at the specified coordinates.

**Parameters**:
- `x` (number): World X coordinate
- `y` (number): World Y coordinate
- `z` (number): World Z coordinate

**Returns**: table - Block information `{id=number, meta=table}`

**Example**:
```lua
local block = engine:getBlock(10, 64, 10)
if block.id ~= 0 then
  log:info("Block ID: " .. block.id)
end
```

## Logger API

The `Logger` global provides logging functionality.

### Logger:info(message)

Log an informational message.

**Parameters**:
- `message` (string): Message to log

**Example**:
```lua
log:info("This is an info message")
```

### Logger:warn(message)

Log a warning message.

**Parameters**:
- `message` (string): Warning message

**Example**:
```lua
log:warn("This is a warning")
```

### Logger:error(message)

Log an error message.

**Parameters**:
- `message` (string): Error message

**Example**:
```lua
log:error("Something went wrong!")
```

## FileAPI

The `FileAPI` global provides restricted file access within the mod's directory.

### FileAPI:read(path)

Read a file from the mod's directory.

**Parameters**:
- `path` (string): Relative path within mod directory

**Returns**: string - File contents, or nil on error

**Security**: Path is restricted to mod directory. Attempts to access parent directories will fail.

**Example**:
```lua
local data = FileAPI:read("data/config.txt")
if data then
  log:info("Config loaded: " .. data)
end
```

### FileAPI:write(path, data)

Write data to a file in the mod's directory.

**Parameters**:
- `path` (string): Relative path within mod directory
- `data` (string): Data to write

**Returns**: boolean - true on success, false on error

**Security**: Path is restricted to mod directory.

**Example**:
```lua
local success = FileAPI:write("data/save.txt", "player_score=100")
if success then
  log:info("Data saved")
end
```

## Events

### Available Events

#### onTick
Fired every game tick.

**Arguments**:
- `tick` (number): Current tick count

**Example**:
```lua
engine:registerEvent("onTick", function(tick)
  if tick % 20 == 0 then
    log:info("One second passed")
  end
end)
```

#### onChunkLoad
Fired when a chunk is loaded.

**Arguments**:
- `chunkX` (number): Chunk X coordinate
- `chunkZ` (number): Chunk Z coordinate

**Example**:
```lua
engine:registerEvent("onChunkLoad", function(x, z)
  log:info("Chunk loaded: " .. x .. ", " .. z)
end)
```

#### onChunkUnload
Fired when a chunk is unloaded.

**Arguments**:
- `chunkX` (number): Chunk X coordinate
- `chunkZ` (number): Chunk Z coordinate

**Example**:
```lua
engine:registerEvent("onChunkUnload", function(x, z)
  log:info("Chunk unloaded: " .. x .. ", " .. z)
end)
```

#### onBlockPlace
Fired when a block is placed.

**Arguments**:
- `x` (number): World X coordinate
- `y` (number): World Y coordinate
- `z` (number): World Z coordinate
- `blockId` (number): Block type ID

**Example**:
```lua
engine:registerEvent("onBlockPlace", function(x, y, z, blockId)
  log:info("Block placed at " .. x .. "," .. y .. "," .. z)
end)
```

#### onBlockBreak
Fired when a block is broken.

**Arguments**:
- `x` (number): World X coordinate
- `y` (number): World Y coordinate
- `z` (number): World Z coordinate
- `blockId` (number): Block type ID that was broken

**Example**:
```lua
engine:registerEvent("onBlockBreak", function(x, y, z, blockId)
  log:info("Block broken at " .. x .. "," .. y .. "," .. z)
end)
```

#### onPlayerJoin
Fired when a player joins (placeholder for future multiplayer).

**Arguments**:
- `player` (userdata): Player object

**Example**:
```lua
engine:registerEvent("onPlayerJoin", function(player)
  engine:sendChat(player, "Welcome!")
end)
```

## Best Practices

### Performance

1. **Keep onTick() Fast**: Avoid heavy computations in `onTick()`. Use `engine:schedule()` for delayed operations.

```lua
-- BAD: Heavy computation every tick
function onTick()
  for i = 1, 10000 do
    -- expensive operation
  end
end

-- GOOD: Throttle heavy operations
local tickCounter = 0
function onTick()
  tickCounter = tickCounter + 1
  if tickCounter >= 100 then
    tickCounter = 0
    -- expensive operation
  end
end
```

2. **Cache Frequently Used Values**: Don't recalculate the same values repeatedly.

```lua
-- BAD
engine:registerEvent("onTick", function(tick)
  local value = calculateExpensiveValue()
  useValue(value)
end)

-- GOOD
local cachedValue = calculateExpensiveValue()
engine:registerEvent("onTick", function(tick)
  useValue(cachedValue)
end)
```

3. **Use Local Variables**: Local variables are faster than globals in Lua.

```lua
-- GOOD
local engine = Engine
local log = Logger

function onEnable()
  log:info("Starting")
end
```

### Error Handling

Always use `pcall` for operations that might fail:

```lua
local success, result = pcall(function()
  return FileAPI:read("data/config.txt")
end)

if success then
  log:info("Config loaded: " .. result)
else
  log:error("Failed to load config: " .. result)
end
```

### Mod Data Persistence

Store mod data in files within your mod directory:

```lua
function onEnable()
  -- Load saved data
  local data = FileAPI:read("data/save.json")
  if data then
    modData = parseJSON(data)
  else
    modData = { score = 0 }
  end
end

function onDisable()
  -- Save data
  local json = toJSON(modData)
  FileAPI:write("data/save.json", json)
end
```

### Avoid Infinite Loops

The engine has a watchdog that will disable mods with infinite loops:

```lua
-- BAD: Will timeout and disable mod
function onEnable()
  while true do
    -- infinite loop
  end
end

-- GOOD: Use events and scheduling
function onEnable()
  engine:registerEvent("onTick", function()
    -- Runs every tick without blocking
  end)
end
```

## Examples

### Example 1: Welcome Message

```lua
local engine = Engine
local log = Logger

function onEnable()
  log:info("Welcome mod enabled")
  
  engine:registerEvent("onPlayerJoin", function(player)
    engine:sendChat(player, "Welcome to PoorCraft!")
    engine:schedule(60, function()
      engine:sendChat(player, "Hope you enjoy your stay!")
    end)
  end)
end
```

### Example 2: Block Counter

```lua
local engine = Engine
local log = Logger

local blocksPlaced = 0
local blocksBroken = 0

function onEnable()
  engine:registerEvent("onBlockPlace", function(x, y, z, blockId)
    blocksPlaced = blocksPlaced + 1
  end)
  
  engine:registerEvent("onBlockBreak", function(x, y, z, blockId)
    blocksBroken = blocksBroken + 1
  end)
  
  -- Report every 10 seconds
  engine:registerEvent("onTick", function(tick)
    if tick % 200 == 0 then
      log:info("Placed: " .. blocksPlaced .. ", Broken: " .. blocksBroken)
    end
  end)
end

function onDisable()
  log:info("Final stats - Placed: " .. blocksPlaced .. ", Broken: " .. blocksBroken)
end
```

### Example 3: Auto-Save

```lua
local engine = Engine
local log = Logger

function onEnable()
  log:info("Auto-save mod enabled")
  
  engine:registerEvent("onTick", function(tick)
    -- Auto-save every 5 minutes (6000 ticks)
    if tick % 6000 == 0 then
      log:info("Auto-saving world...")
      -- World save happens automatically
      log:info("World saved!")
    end
  end)
end
```

### Example 4: Chunk Logger

```lua
local engine = Engine
local log = Logger

local loadedChunks = 0

function onEnable()
  engine:registerEvent("onChunkLoad", function(x, z)
    loadedChunks = loadedChunks + 1
    log:info("Chunk loaded at " .. x .. ", " .. z .. " (Total: " .. loadedChunks .. ")")
  end)
  
  engine:registerEvent("onChunkUnload", function(x, z)
    loadedChunks = loadedChunks - 1
    log:info("Chunk unloaded at " .. x .. ", " .. z .. " (Total: " .. loadedChunks .. ")")
  end)
end
```

### Example 5: Persistent Data

```lua
local engine = Engine
local log = Logger

local playerData = {}

function onLoad()
  -- Load persistent data
  local data = FileAPI:read("data/players.txt")
  if data then
    -- Parse data (simple format: name=score)
    for line in data:gmatch("[^\r\n]+") do
      local name, score = line:match("([^=]+)=(%d+)")
      if name and score then
        playerData[name] = tonumber(score)
      end
    end
    log:info("Loaded data for " .. #playerData .. " players")
  end
end

function onDisable()
  -- Save persistent data
  local lines = {}
  for name, score in pairs(playerData) do
    table.insert(lines, name .. "=" .. score)
  end
  
  local data = table.concat(lines, "\n")
  if FileAPI:write("data/players.txt", data) then
    log:info("Player data saved")
  else
    log:error("Failed to save player data")
  end
end
```

## Sandbox Restrictions

For security, the following Lua features are **disabled**:

- `os.*` - Operating system functions
- `io.*` - File I/O (use FileAPI instead)
- `require()` - Module loading
- `dofile()` - File execution
- `loadfile()` - File loading
- `package.*` - Package management
- `module()` - Module definition

Attempting to use these will result in an error and may disable your mod.

## Debugging

### Logging

Use the Logger API extensively:

```lua
log:info("Debug: variable = " .. tostring(variable))
```

### Check Logs

Logs are written to:
- Console output (if running in dev mode)
- `logs/engine.log` - Engine logs
- `logs/mods.log` - Mod-specific logs

### Dev Mode

Run with `--dev-mode` flag for:
- More verbose logging
- Live reload on file changes
- In-game console (press `` ` ``)

```powershell
./gradlew :launcher:runDev
```

## Support

- Check `logs/engine.log` for errors
- Verify your `mod.json` is valid JSON
- Test Lua syntax with a Lua interpreter
- Join the community Discord for help
- Report bugs on GitHub Issues
