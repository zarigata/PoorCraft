# Migration Guide: Python to Lua (v2.0)

This guide helps you migrate your Python mods to Lua for PoorCraft v2.0.

## What Changed in v2.0

### Major Changes
- **Modding Language**: Python → Lua
- **Folder Structure**: Reorganized for better clarity
- **Mod Location**: `mods/` → `gamedata/mods/`
- **Dependencies**: Removed Py4J, added LuaJ

### Why the Change?

**Benefits of Lua:**
- No external Python runtime required
- Smaller footprint and faster startup
- Easier single-executable distribution
- Industry-standard for game modding

## Folder Structure Changes

### Before (v1.x)
```
PoorCraft/
├── mods/              # Python mods
├── python/            # Python mod framework
├── UI_FILES/          # UI textures
├── scripts/           # Utility scripts
├── worlds/            # World saves
├── screenshots/       # Screenshots
├── skins/             # Player skins
└── config/            # Config files
```

### After (v2.0)
```
PoorCraft/
├── gamedata/          # Runtime game data (NEW)
│   ├── mods/         # Lua mods
│   ├── worlds/       # World saves
│   ├── screenshots/  # Screenshots
│   ├── skins/        # Player skins
│   ├── resourcepacks/# Resource packs
│   └── config/       # Config files
├── assets/            # Development assets (NEW)
│   ├── ui/           # UI textures
│   └── scripts/      # Utility scripts
└── docs/              # Documentation
```

## API Comparison

### Python (v1.x) vs Lua (v2.0)

| Function | Python (Old) | Lua (New) |
|----------|-------------|-----------|
| Get block | `get_block(x, y, z)` | `api.get_block(x, y, z)` |
| Set block | `set_block(x, y, z, id)` | `api.set_block(x, y, z, id)` |
| Get biome | `get_biome(x, z)` | `api.get_biome(x, z)` |
| Log message | `log("message")` | `api.log("message")` |
| Register event | `on_player_join(callback)` | `api.register_event("player_join", callback)` |
| Shared data | `set_shared_data(key, val)` | `api.set_shared_data(key, val)` |
| Config | `get_mod_config("mod_id")` | `api.get_mod_config("mod_id")` |
| Spawn NPC | `spawn_npc(id, name, ...)` | `api.spawn_npc(id, name, ...)` |

### Mod Structure

**Python (v1.x):**
```python
# main.py
from poorcraft import log, get_block, set_block

def init():
    log("Mod initializing...")

def enable():
    log("Mod enabled!")

def disable():
    log("Mod disabled")
```

**Lua (v2.0):**
```lua
-- main.lua
local mod = {}

function mod.init()
    api.log("Mod initializing...")
end

function mod.enable()
    api.log("Mod enabled!")
end

function mod.disable()
    api.log("Mod disabled")
end

return mod
```

### mod.json Changes

**Python (v1.x):**
```json
{
  "id": "my_mod",
  "name": "My Mod",
  "main": "my_mod.main",
  ...
}
```

**Lua (v2.0):**
```json
{
  "id": "my_mod",
  "name": "My Mod",
  "main": "main.lua",
  ...
}
```

## Migration Steps

### 1. Backup Your Mods
```bash
# Backup your Python mods
cp -r mods/ mods_backup_python/
```

### 2. Create New Lua Mod Structure
```bash
mkdir -p gamedata/mods/my_mod
cd gamedata/mods/my_mod
```

### 3. Update mod.json
- Change `"main"` from `"my_mod.main"` to `"main.lua"`
- Keep everything else the same

### 4. Convert Python Code to Lua

**Key Differences:**
- Python uses `def function():`, Lua uses `function mod.function()`
- Python uses `import`, Lua doesn't (API is global as `api`)
- Python uses `True/False`, Lua uses `true/false`
- Python uses `None`, Lua uses `nil`
- Python uses `:`, Lua uses `end`
- Python lists/dicts → Lua tables

**Example Conversion:**

Python:
```python
def on_block_place(event):
    x, y, z = event.get("x"), event.get("y"), event.get("z")
    log(f"Block placed at {x}, {y}, {z}")
    set_block(x, y + 1, z, 1)  # Place stone above
```

Lua:
```lua
function mod.on_block_place(event)
    local x, y, z = event.x, event.y, event.z
    api.log("Block placed at " .. x .. ", " .. y .. ", " .. z)
    api.set_block(x, y + 1, z, 1)  -- Place stone above
end
```

### 5. Test Your Mod
1. Start PoorCraft
2. Check console for mod loading messages
3. Look for errors in the log
4. Test mod functionality in-game

## Known Limitations

### Features Not Yet Available in Lua
- **Procedural Texture Generation**: Requires image processing library (was using PIL/numpy)
- **HTTP Requests**: For AI NPCs (was using requests library)
- **Advanced Math**: Some numpy features not available

### Workarounds
- **Textures**: Use Java-side texture generation or provide pre-made textures
- **HTTP**: Will be added in future via Lua HTTP library or Java bridge
- **Math**: Use Lua's built-in math library or implement in Java

## Common Issues

### "Module not found"
- Make sure `main.lua` exists in your mod directory
- Check mod.json `"main"` field points to correct file

### "Attempt to call nil value"
- API function might not exist yet
- Check API_REFERENCE.md for available functions
- Ensure you're using `api.function()` not just `function()`

### "Syntax error"
- Check Lua syntax (different from Python)
- Common: forgot `end`, used `:` instead of proper syntax
- Use a Lua linter in your editor

## Getting Help

- **Documentation**: Check `docs/` folder
- **Examples**: Look at `gamedata/mods/example_mod/`
- **API Reference**: See `docs/API_REFERENCE.md`
- **Community**: Open an issue on GitHub

## Future Plans

We plan to add:
- Lua image processing library for texture mods
- HTTP library for AI integration
- More API functions as requested
- Better debugging tools

## Why Not Keep Python?

While we loved Python for modding, Lua offers:
- **Portability**: No Python installation needed
- **Performance**: Faster startup, lower memory
- **Distribution**: Easier to package as single executable
- **Industry Standard**: Proven in game modding

Python mods can still be used as reference - the logic remains the same, just different syntax!
