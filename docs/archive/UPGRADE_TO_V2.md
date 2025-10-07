# 🎉 Welcome to PoorCraft v2.0!

Your game has been successfully upgraded with major improvements!

## ✨ What's New

### 🗂️ 1. Cleaner Folder Structure
Your game files are now better organized:

```
PoorCraft/
├── gamedata/          # All your game data in one place!
│   ├── mods/         # Lua mods (was: mods/)
│   ├── worlds/       # World saves
│   ├── screenshots/  # Screenshots
│   ├── skins/        # Player skins
│   ├── resourcepacks/# Resource packs
│   └── config/       # Configurations
├── assets/            # Development assets
│   ├── ui/           # UI textures (was: UI_FILES/)
│   └── scripts/      # Utility scripts
└── docs/              # Documentation
```

**Benefits:**
- Everything is where you'd expect it
- Easier to find and manage your content
- Ready for future features

### 🌙 2. Lua Modding System
We've switched from Python to Lua!

**Why Lua?**
- ✅ **No Python required** - One less thing to install
- ✅ **Faster startup** - Game loads quicker
- ✅ **Industry standard** - Same as WoW, Roblox, Garry's Mod
- ✅ **Smaller footprint** - Less memory usage
- ✅ **Better for distribution** - Easier to package

**Example Lua Mod:**
```lua
local mod = {}

function mod.init()
    api.log("Hello from Lua!")
end

function mod.enable()
    api.log("Mod enabled!")
end

return mod
```

### 📚 3. Updated Documentation
All guides have been rewritten for Lua:
- `docs/MODDING_GUIDE.md` - Learn Lua modding
- `MIGRATION_GUIDE_v2.0.md` - Convert Python mods to Lua
- `REFACTOR_SUMMARY.md` - Technical details of changes

## 🚀 Getting Started

### First Time Running v2.0?

1. **Build the game:**
   ```bash
   # Windows
   scripts\build-and-run.bat
   
   # Linux/Mac
   chmod +x scripts/build-and-run.sh
   scripts/build-and-run.sh
   ```

2. **Check your mods:**
   - Look in `gamedata/mods/`
   - Example mods are included
   - Old Python mods won't work (see migration guide)

3. **Everything else works the same!**
   - Controls haven't changed
   - Worlds are compatible
   - Settings are preserved

## 📖 Documentation

### For Players
- `README.md` - Game overview and features
- `docs/` - Comprehensive documentation

### For Modders
- `docs/MODDING_GUIDE.md` - Create your first Lua mod
- `docs/API_REFERENCE.md` - Complete API documentation
- `MIGRATION_GUIDE_v2.0.md` - Convert Python mods to Lua
- `docs/EXAMPLES.md` - Example mod code

## 🔧 What Happened to My Python Mods?

Python mods were backed up but need conversion to Lua.

**Quick comparison:**
| Python (v1.x) | Lua (v2.0) |
|---------------|------------|
| `mods/my_mod/main.py` | `gamedata/mods/my_mod/main.lua` |
| `from poorcraft import log` | `api.log()` |
| `def init():` | `function mod.init()` |
| `True/False` | `true/false` |

**See `MIGRATION_GUIDE_v2.0.md` for detailed conversion guide!**

## ⚠️ Known Limitations

Some features are simplified in v2.0:

1. **Procedural Texture Generation** - Placeholder (requires image library)
2. **AI NPC System** - Placeholder (requires HTTP library)

These will be fully implemented in future updates as we integrate necessary Lua libraries.

## 🐛 Having Issues?

### Common Problems

**"Game won't compile"**
- Make sure Maven is installed: `mvn --version`
- Try: `mvn clean package`

**"Mods not loading"**
- Check `gamedata/mods/` exists
- Verify `mod.json` has `"main": "main.lua"`
- Look for errors in console output

**"Missing textures"**
- Check `assets/ui/` has your UI textures
- Create UI textures manually (see README 'Required Textures') or use bundled textures. Python scripts were removed in the Lua-only migration.

### Getting Help
- Check documentation in `docs/`
- Review example mods in `gamedata/mods/`
- Open an issue on GitHub

## 📋 Technical Changes

For developers and technical users:

**Java Changes:**
- New: `LuaModLoader.java`, `LuaModContainer.java`, `LuaModAPI.java`
- Updated: `Game.java`, `ChunkRenderer.java`, `ModAPI.java`
- Dependency: Replaced Py4J with LuaJ 3.0.1

**API:**
- All functions now prefixed with `api.` in Lua
- Event registration: `api.register_event()`
- World access: `api.get_block()`, `api.set_block()`
- Full API documented in `docs/API_REFERENCE.md`

## 🎯 Next Steps

1. **Explore the new structure** - Check out `gamedata/` and `assets/`
2. **Try the example mods** - See `gamedata/mods/example_mod/`
3. **Read the docs** - Lua modding guide in `docs/`
4. **Create your own mod** - Follow the tutorial
5. **Share your creations** - Lua mods are easier to distribute!

## 💡 Future Plans

We're working on:
- More Lua API functions
- Image processing library for texture mods
- HTTP library for AI NPC integration
- Hot-reloading for mods during development
- In-game mod manager UI

## 🙏 Thank You!

Thank you for using PoorCraft! This refactor makes the game:
- More maintainable
- Easier to distribute
- More portable
- Better organized

We hope you enjoy v2.0! Happy crafting! ⛏️

---

**Questions?** Check the documentation or open an issue!

**Want to contribute?** PRs are welcome!

**Enjoying PoorCraft?** Star the repo! ⭐
