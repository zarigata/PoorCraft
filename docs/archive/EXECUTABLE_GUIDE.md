# PoorCraft v2.0 - Single Executable Guide

## 🎮 What You Get

PoorCraft v2.0 can be built as a **single Windows executable** (`PoorCraft.exe`) that contains:
- ✅ The entire game engine
- ✅ All Java dependencies (LWJGL, JOML, LuaJ, etc.)
- ✅ Lua runtime for modding
- ✅ No external dependencies needed!

## 🚀 Quick Start

### For Users (Running the Game)

1. **Download** `PoorCraft.exe`
2. **Double-click** to run
3. **First run** will automatically create:
   - `gamedata/mods/` - Put your Lua mods here
   - `gamedata/worlds/` - Your world saves
   - `gamedata/screenshots/` - Screenshots
   - `gamedata/skins/` - Player skins
   - `gamedata/config/` - Configuration files
   - `assets/ui/` - Editable UI textures

4. **Check console** for "Hi Mod" message to verify mods work!

### For Developers (Building the .exe)

```bash
# Quick build
scripts\build-exe.bat           # Windows
chmod +x scripts/build-exe.sh
scripts/build-exe.sh            # Linux/Mac

# Or manually
mvn clean package

# Output:
# - target/PoorCraft.jar  (Fat JAR)
# - target/PoorCraft.exe  (Windows executable)
```

## 📁 Folder Structure After First Run

```
[Where you put PoorCraft.exe]
├── PoorCraft.exe          ← The game
├── gamedata/              ← AUTO-CREATED on first run
│   ├── mods/              ← Put Lua mods here
│   │   ├── hi_mod/        ← Test mod (says "Hi!")
│   │   ├── example_mod/   ← Example Lua mod
│   │   └── [your mods]/   ← Add your own mods
│   ├── worlds/            ← World saves
│   ├── screenshots/       ← Screenshots (F2)
│   ├── skins/             ← Player skins
│   ├── resourcepacks/     ← Resource packs
│   └── config/            ← Configuration files
└── assets/                ← AUTO-CREATED on first run
    ├── ui/                ← UI textures (editable)
    └── scripts/           ← Utility scripts
```

## 🧪 Testing Mods - The "Hi Mod"

The executable includes a test mod that runs automatically:

**What it does:**
- Displays "Hi!" message when game starts
- Runs for 5 seconds
- Proves Lua modding system works

**Console output you'll see:**
```
=======================================
        HI MOD INITIALIZED!           
=======================================

╔════════════════════════════════════════╗
║                                        ║
║         🎉  HI FROM LUA MOD! 🎉        ║
║                                        ║
║   This message will display for 5s    ║
║   Testing the Lua modding system!     ║
║                                        ║
╚════════════════════════════════════════╝

Hi Mod: Message will disappear in 5 seconds...
Hi Mod: Message will disappear in 4 seconds...
Hi Mod: Message will disappear in 3 seconds...
Hi Mod: Message will disappear in 2 seconds...
Hi Mod: Message will disappear in 1 seconds...
Hi Mod: Lua modding system is working correctly! ✓
```

**To disable Hi Mod:**
1. Open `gamedata/mods/hi_mod/mod.json`
2. Change `"enabled": true` to `"enabled": false`
3. Restart the game

## 🛠️ Creating Your Own Mods

### Step 1: Create Mod Folder
```bash
mkdir gamedata/mods/my_mod
```

### Step 2: Create mod.json
```json
{
  "id": "my_mod",
  "name": "My Cool Mod",
  "version": "1.0.0",
  "description": "My awesome mod!",
  "author": "Your Name",
  "main": "main.lua",
  "enabled": true
}
```

### Step 3: Create main.lua
```lua
local mod = {}

function mod.init()
    api.log("My mod is initializing!")
end

function mod.enable()
    api.log("My mod is enabled!")
end

function mod.disable()
    api.log("My mod is disabled!")
end

return mod
```

### Step 4: Run the game!
Your mod will load automatically.

## 📋 System Requirements

**To Run:**
- Windows 7/8/10/11
- Java 17 or higher (JDK or JRE)
- 512 MB RAM minimum (2 GB recommended)
- OpenGL 3.0+ compatible graphics

**To Build:**
- JDK 17+
- Maven 3.6+

## 🎯 Executable Features

### Auto-Initialization
On first run, the game automatically:
1. Creates `gamedata/` directory structure
2. Creates `assets/` directory structure
3. Checks for Java installation
4. Initializes Lua mod system
5. Loads all enabled mods

### Memory Configuration
Default settings:
- **Minimum**: 512 MB
- **Maximum**: 2 GB

**To change:** Edit `pom.xml` and rebuild:
```xml
<opts>
    <opt>-Xms512m</opt>   <!-- Min -->
    <opt>-Xmx2048m</opt>  <!-- Max -->
</opts>
```

### No Console Window
The .exe runs in GUI mode (no console window).

**To see console output:**
- Run from Command Prompt: `PoorCraft.exe`
- Or change `<headerType>gui</headerType>` to `<headerType>console</headerType>` in `pom.xml`

## 📦 Distribution

### Single File Distribution
Just share `PoorCraft.exe`! Users only need Java 17+.

### Complete Package
For a better experience, include:
```
PoorCraft-v2.0/
├── PoorCraft.exe
├── README.md
├── gamedata/
│   └── mods/
│       ├── hi_mod/       ← Test mod
│       ├── example_mod/  ← Example mod
│       └── [more mods]
└── assets/
    └── ui/
        └── [UI textures]
```

## 🐛 Troubleshooting

### "Java not found"
**Solution:** Install Java 17+
- Download: https://adoptium.net/
- Or: https://www.oracle.com/java/technologies/downloads/

### ".exe won't run"
1. Check Java version: `java -version` (should be 17+)
2. Try running JAR directly: `java -jar PoorCraft.jar`
3. Check Windows Event Viewer for errors

### "Folders not created"
1. Check file permissions (run as administrator if needed)
2. Ensure you have write access to the folder
3. Check console for error messages

### "Mods not loading"
1. Verify `gamedata/mods/` exists
2. Check mod structure (mod.json + main.lua)
3. Check `"enabled": true` in mod.json
4. Look for Lua errors in console

### "Hi Mod not showing"
1. Check `gamedata/mods/hi_mod/` exists
2. Verify mod.json has correct structure
3. Run from Command Prompt to see console output
4. Look for error messages

## 🔧 Advanced Configuration

### Custom Icon
1. Create/obtain a `.ico` file (Windows icon)
2. Place in `src/main/resources/images/icon.ico`
3. Uncomment this line in `pom.xml`:
   ```xml
   <icon>src/main/resources/images/icon.ico</icon>
   ```
4. Rebuild: `mvn clean package`

### Version Information
Edit `pom.xml` under `<versionInfo>`:
```xml
<fileVersion>2.0.0.0</fileVersion>
<productName>PoorCraft</productName>
<copyright>2025 Zarigata</copyright>
```

### Different Output Name
Change in `pom.xml`:
```xml
<outfile>target/MyGame.exe</outfile>
```

## 📊 File Sizes

Approximate sizes:
- **PoorCraft.jar**: ~15-20 MB
- **PoorCraft.exe**: ~15-20 MB (same content, just wrapped)
- **With mods & assets**: ~25-30 MB

## 🎓 Learn More

- **Modding Guide**: `docs/MODDING_GUIDE.md`
- **API Reference**: `docs/API_REFERENCE.md`
- **Examples**: `docs/EXAMPLES.md`
- **Migration Guide**: `MIGRATION_GUIDE_v2.0.md`

## ✨ Benefits

### For Users
- ✅ Single file to download
- ✅ No Python required
- ✅ Easy mod installation (just drop in `gamedata/mods/`)
- ✅ Portable (run from USB, cloud, anywhere)

### For Developers
- ✅ Easier distribution
- ✅ No dependency management for users
- ✅ Lua mods are simpler than Python
- ✅ Self-contained package

## 🚀 Next Steps

1. **Build**: `scripts\build-exe.bat` or `mvn package`
2. **Test**: Run `target/PoorCraft.exe`
3. **Verify**: Look for "Hi Mod" message
4. **Play**: Enjoy PoorCraft!
5. **Mod**: Create your own Lua mods!

Happy crafting! ⛏️🎮
