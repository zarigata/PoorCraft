# PoorCraft Refactor - Quick Start Guide

Get up and running with PoorCraft Refactor in 5 minutes!

## Installation

### Option 1: Download Pre-built Binary (Recommended)

1. **Download** the latest release from [GitHub Releases](https://github.com/yourrepo/poorcraft-refactor/releases)
2. **Extract** `PoorCraftRefactor-v0.1.2-windows.zip`
3. **Run** `PoorCraftRefactor.exe`

That's it! The game will create necessary directories on first run.

### Option 2: Build from Source

```powershell
# Clone repository
git clone https://github.com/yourrepo/poorcraft-refactor.git
cd poorcraft-refactor

# Build and run
.\gradlew.bat :launcher:run
```

## First Run

When you run PoorCraft for the first time:

1. **Data directory created** at `%APPDATA%\PoorCraftRefactor\`
2. **Default assets extracted**:
   - `skins/` - Block textures
   - `mods/` - Example mod
   - `saves/` - World saves
   - `logs/` - Log files
3. **Window opens** with a simple voxel world

## Basic Controls

| Key | Action |
|-----|--------|
| `ESC` | Exit game |
| `` ` `` | Toggle console (dev mode) |

**Note**: Full player controls (movement, block placement) are coming in future versions.

## Your First Mod

Let's create a simple mod that logs a message every second.

### Step 1: Create Mod Directory

```powershell
cd %APPDATA%\PoorCraftRefactor\mods
mkdir my_first_mod
cd my_first_mod
```

### Step 2: Create mod.json

Create `mod.json`:

```json
{
  "id": "my_first_mod",
  "name": "My First Mod",
  "version": "1.0.0",
  "author": "YourName",
  "main": "main.lua"
}
```

### Step 3: Create main.lua

Create `main.lua`:

```lua
local engine = Engine
local log = Logger

function onEnable()
  log:info("My first mod is running!")
  
  engine:registerEvent("onTick", function(tick)
    if tick % 20 == 0 then
      log:info("One second has passed! Tick: " .. tick)
    end
  end)
end
```

### Step 4: Restart Game

Close and restart PoorCraft. Check `logs/engine.log` to see your mod's messages!

## Customizing Textures

### Step 1: Navigate to Skins

```powershell
cd %APPDATA%\PoorCraftRefactor\skins\default
```

### Step 2: Edit a Texture

1. Open `grass.png` in your favorite image editor
2. Make changes (keep it 16×16 pixels)
3. Save the file

### Step 3: Rebuild Atlas

```powershell
cd %APPDATA%\PoorCraftRefactor
# Delete old atlas
del skins\default\atlas.png
del skins\default\atlas.json
```

### Step 4: Restart Game

The atlas will be rebuilt automatically with your new texture!

## Configuration

Edit `%APPDATA%\PoorCraftRefactor\config.json`:

```json
{
  "windowWidth": 1920,
  "windowHeight": 1080,
  "vsync": true,
  "renderDistance": 12,
  "fov": 90.0
}
```

Restart the game to apply changes.

## Command Line Options

```powershell
# Portable mode (data folder next to exe)
PoorCraftRefactor.exe --portable

# Development mode (verbose logging, console)
PoorCraftRefactor.exe --dev-mode

# Headless mode (no window, for testing)
PoorCraftRefactor.exe --headless
```

## Troubleshooting

### Game Won't Start

**Check logs**: `%APPDATA%\PoorCraftRefactor\logs\engine.log`

**Common issues**:
- Missing Java (if running from source)
- Graphics driver too old (need OpenGL 3.3+)
- Antivirus blocking executable

### Mods Not Loading

1. Check `logs/engine.log` for errors
2. Verify `mod.json` is valid JSON
3. Ensure `main.lua` exists
4. Check for Lua syntax errors

### Black Screen

1. Update graphics drivers
2. Check if OpenGL 3.3+ is supported
3. Try disabling vsync in config.json

### Poor Performance

1. Reduce `renderDistance` in config.json
2. Lower `maxLoadedChunks`
3. Disable vsync if input lag is an issue

## Next Steps

### Learn More

- **[README.md](../README.md)** - Full documentation
- **[API.md](API.md)** - Complete Lua API reference
- **[SKINS.md](SKINS.md)** - Texture creation guide
- **[BUILDING.md](BUILDING.md)** - Build from source

### Join Community

- **Discord** - Chat with other users
- **GitHub Discussions** - Ask questions
- **GitHub Issues** - Report bugs

### Example Projects

Check out `mods/example_mod/` for a complete example with:
- Event listeners
- Scheduled callbacks
- Chat messages
- Tick handling

## Common Tasks

### Change Window Size

Edit `config.json`:
```json
{
  "windowWidth": 1920,
  "windowHeight": 1080
}
```

### Increase Render Distance

Edit `config.json`:
```json
{
  "renderDistance": 16
}
```

**Note**: Higher values impact performance.

### Enable Dev Mode

Run with flag:
```powershell
PoorCraftRefactor.exe --dev-mode
```

Or create a shortcut with the flag.

### View Logs

Logs are in `%APPDATA%\PoorCraftRefactor\logs\`:
- `engine.log` - Engine logs
- `mods.log` - Mod-specific logs (if configured)

### Reset to Defaults

Delete the data directory:
```powershell
rmdir /s %APPDATA%\PoorCraftRefactor
```

Next run will recreate with defaults.

## FAQ

**Q: Can I run multiple instances?**
A: Yes, use `--portable` flag for each instance in separate directories.

**Q: Where are worlds saved?**
A: `%APPDATA%\PoorCraftRefactor\saves\world\`

**Q: Can I share mods?**
A: Yes! Zip your mod folder and share. Others can extract to their `mods/` directory.

**Q: How do I update?**
A: Download the new version and replace the exe. Your data directory is preserved.

**Q: Is multiplayer supported?**
A: Not yet. It's on the roadmap for a future version.

**Q: Can I use this in my project?**
A: Yes! See [LICENSE](../LICENSE) for details.

## Performance Tips

1. **Lower render distance** - Biggest performance impact
2. **Reduce max loaded chunks** - Saves memory
3. **Disable vsync** - If you don't mind tearing
4. **Close other programs** - Free up RAM
5. **Update graphics drivers** - Always recommended

## Keyboard Shortcuts (Dev Mode)

| Key | Action |
|-----|--------|
| `` ` `` | Toggle console |
| `F3` | Show debug info (planned) |
| `F11` | Toggle fullscreen (planned) |

## Getting Help

If you're stuck:

1. **Check logs** - Most issues are logged
2. **Search issues** - Someone may have had the same problem
3. **Ask on Discord** - Community is helpful
4. **Open an issue** - If it's a bug

## What's Next?

Now that you're set up, try:

1. **Creating a more complex mod** - See [API.md](API.md)
2. **Making a texture pack** - See [SKINS.md](SKINS.md)
3. **Contributing** - See [CONTRIBUTING.md](../CONTRIBUTING.md)
4. **Exploring the code** - It's all open source!

Happy crafting! 🎮
