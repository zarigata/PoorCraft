# Building PoorCraft.exe

## Overview
PoorCraft v2.0 can be built as a single Windows executable (.exe) that contains the entire game, including all dependencies and the Lua runtime.

## Prerequisites

1. **JDK 17 or higher** - Required to compile Java code
2. **Maven** - Build automation tool
3. **Launch4j** - Automatically handled by Maven plugin

## Build Steps

### Option 1: Quick Build (Recommended)

Run the build script:

```bash
# Windows
mvn clean package

# This will create:
# - target/PoorCraft.jar (fat JAR with all dependencies)
# - target/PoorCraft.exe (Windows executable)
```

### Option 2: Manual Build

```bash
# Clean previous builds
mvn clean

# Compile and package
mvn package

# The .exe will be in: target/PoorCraft.exe
```

## What Happens When You Run PoorCraft.exe

### First Run
When you run `PoorCraft.exe` for the first time, it will automatically create:

```
PoorCraft.exe (your location)
├── gamedata/
│   ├── mods/              # Put your Lua mods here
│   ├── worlds/            # World saves
│   ├── screenshots/       # Screenshots
│   ├── skins/            # Player skins
│   ├── resourcepacks/    # Resource packs
│   └── config/           # Configuration files
└── assets/
    ├── ui/               # UI textures (editable)
    └── scripts/          # Utility scripts
```

### Included Mods
The executable comes with example mods:
- **hi_mod** - Test mod that displays "Hi!" for 5 seconds on startup
- **example_mod** - Simple example showing mod structure
- **block_texture_generator** - Placeholder for texture generation
- **ai_npc** - Placeholder for AI NPC system

## Testing the Build

### 1. Build the executable:
```bash
mvn clean package
```

### 2. Run the executable:
```bash
cd target
./PoorCraft.exe
```

Or double-click `PoorCraft.exe` in Windows Explorer.

### 3. Check for the "Hi Mod" message:
Look in the console output for:
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
```

## Distribution

### What to Distribute
You can distribute just the **PoorCraft.exe** file! Users need:
1. Windows OS
2. Java 17+ installed (the .exe will check and prompt if missing)

### Complete Package (Recommended)
For a complete distribution, include:
```
PoorCraft-v2.0/
├── PoorCraft.exe          # The game
├── README.md              # Game information
├── gamedata/              # Pre-populated with example mods
│   └── mods/
│       ├── hi_mod/
│       ├── example_mod/
│       └── ...
└── assets/                # Optional: pre-made UI textures
    └── ui/
```

## Executable Configuration

The .exe is configured with:
- **Min Memory**: 512 MB
- **Max Memory**: 2048 MB (2 GB)
- **Java Version**: Requires JDK 17+
- **Type**: GUI application (no console window by default)

### To See Console Output
Run from Command Prompt:
```cmd
PoorCraft.exe
```

Or change `<headerType>` in pom.xml to `console` and rebuild.

## Customization

### Memory Settings
Edit `pom.xml` to change memory allocation:
```xml
<jre>
    <minVersion>17</minVersion>
    <opts>
        <opt>-Xms512m</opt>   <!-- Min memory: 512 MB -->
        <opt>-Xmx2048m</opt>  <!-- Max memory: 2 GB -->
    </opts>
</jre>
```

### Custom Icon
Replace `src/main/resources/images/icon.ico` with your own .ico file.

If you don't have an icon, remove this line from pom.xml:
```xml
<icon>src/main/resources/images/icon.ico</icon>
```

## Troubleshooting

### "Java not found" Error
Users need to install JDK 17 or higher:
- Download from: https://adoptium.net/

### .exe Won't Run
1. Check if Java 17+ is installed: `java -version`
2. Try running the JAR directly: `java -jar PoorCraft.jar`
3. Check for error messages in console

### Mods Not Loading
1. Check that `gamedata/mods/` exists
2. Verify mod structure (mod.json and main.lua)
3. Check console for error messages

### "Hi Mod" Not Showing
1. Check `gamedata/mods/hi_mod/` exists
2. Verify `mod.json` has `"enabled": true`
3. Look for Lua error messages in console

## File Sizes

Approximate sizes:
- **PoorCraft.jar**: ~15-20 MB (with all dependencies)
- **PoorCraft.exe**: ~15-20 MB (same, just wrapped)
- **Full package**: ~25-30 MB (with assets and example mods)

## Benefits of Single Executable

✅ **Easy Distribution**: Just one file to share
✅ **No Python Required**: All modding in Lua (embedded)
✅ **Portable**: Can run from anywhere (USB drive, cloud, etc.)
✅ **Auto-Setup**: Creates folders on first run
✅ **Self-Contained**: All dependencies included

## Next Steps

1. Build: `mvn clean package`
2. Test: Run `target/PoorCraft.exe`
3. Verify: Check console for "Hi Mod" message
4. Distribute: Share the .exe file!

Happy crafting! 🎮⛏️
