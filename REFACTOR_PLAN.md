# PoorCraft Refactor Plan v2.0

## Objectives
1. **Clean up project structure** - Organize scattered folders into logical hierarchy
2. **Migrate from Python to Lua modding** - Remove Py4J dependency, implement LuaJ-based modding

## New Folder Structure

```
PoorCraft/
├── src/                      # Java source code (unchanged)
│   └── main/
│       ├── java/com/poorcraft/
│       └── resources/
├── gamedata/                 # Runtime game data (NEW)
│   ├── mods/                # Lua mods (was Python mods)
│   ├── resourcepacks/       # Resource packs
│   ├── worlds/              # World saves
│   ├── screenshots/         # Screenshots
│   ├── skins/               # Player skins
│   └── config/              # Configuration files
├── assets/                   # Development assets (NEW)
│   ├── ui/                  # UI textures (from UI_FILES/)
│   └── scripts/             # Utility scripts (from scripts/)
├── docs/                     # Documentation (unchanged)
├── changelog/                # Release notes (from UPDATES/)
├── pom.xml                   # Maven build configuration
├── README.md
└── build scripts...
```

## Migration Steps

### Phase 1: Folder Reorganization
- [x] Create `gamedata/` directory structure
- [ ] Move `mods/` → `gamedata/mods/`
- [ ] Move `resourcepacks/` → `gamedata/resourcepacks/`
- [ ] Move `worlds/` → `gamedata/worlds/`
- [ ] Move `screenshots/` → `gamedata/screenshots/`
- [ ] Move `skins/` → `gamedata/skins/`
- [ ] Move `config/` → `gamedata/config/`
- [ ] Create `assets/` directory
- [ ] Move `UI_FILES/` → `assets/ui/`
- [ ] Move `scripts/` → `assets/scripts/`
- [ ] Move `UPDATES/` → `changelog/`

### Phase 2: Remove Python Infrastructure
- [ ] Remove `python/` directory
- [ ] Remove Py4J dependency from `pom.xml`
- [ ] Delete Python-related Java classes:
  - `Py4JBridge.java`
  - Python-specific code in `ModLoader.java`
- [ ] Remove Python requirements.txt

### Phase 3: Implement Lua Modding
- [ ] Add LuaJ dependency to `pom.xml`
- [ ] Create new Lua modding classes:
  - `LuaModLoader.java` - Load and manage Lua mods
  - `LuaModAPI.java` - Expose game API to Lua
  - `LuaModContainer.java` - Hold Lua mod state
  - `LuaEventBridge.java` - Connect Lua to event system
- [ ] Update `ModAPI.java` to work with Lua
- [ ] Keep `EventBus.java` (language-agnostic)

### Phase 4: Port Existing Mods
- [ ] Convert `ai_npc` mod from Python to Lua
- [ ] Convert `block_texture_generator` mod from Python to Lua
- [ ] Update mod.json format (if needed)

### Phase 5: Update Documentation
- [ ] Update README.md with new structure
- [ ] Rewrite MODDING_GUIDE.md for Lua
- [ ] Rewrite API_REFERENCE.md for Lua API
- [ ] Update EXAMPLES.md with Lua examples
- [ ] Update EVENT_CATALOG.md (should be unchanged)

### Phase 6: Update Build Configuration
- [ ] Update paths in Java code to use `gamedata/`
- [ ] Update build scripts (build-and-run.bat/sh)
- [ ] Test compilation
- [ ] Test game launch
- [ ] Test mod loading

## Benefits

### Cleaner Structure
- All runtime data in one place (`gamedata/`)
- Development assets separated (`assets/`)
- Clear distinction between source code and data
- Easier to package as single executable later

### Lua Advantages
- No external Python dependency
- Faster startup (no Py4J bridge)
- Better embedding for single-file distribution
- Lighter weight and more portable
- Industry-standard for game modding

### Backward Compatibility
- Old Python mods will need conversion (document process)
- Provide conversion guide in docs
- Keep mod.json format mostly the same

## Technical Details

### Lua Integration (LuaJ)
```xml
<dependency>
    <groupId>org.luaj</groupId>
    <artifactId>luaj-jse</artifactId>
    <version>3.0.1</version>
</dependency>
```

### Mod Format Change
**Before (Python):**
```
mods/my_mod/
├── mod.json
├── main.py
├── __init__.py
└── config.json
```

**After (Lua):**
```
gamedata/mods/my_mod/
├── mod.json
├── main.lua
└── config.json
```

### API Exposure Pattern
```java
// Expose Java API to Lua
LuaValue modAPI = CoerceJavaToLua.coerce(modAPIInstance);
globals.set("api", modAPI);
```

## Timeline
- **Phase 1-2:** Clean up structure, remove Python (1-2 hours)
- **Phase 3:** Implement Lua system (2-3 hours)
- **Phase 4:** Port mods (1-2 hours)
- **Phase 5:** Update docs (1 hour)
- **Phase 6:** Testing and fixes (1-2 hours)

**Total Estimated Time:** 6-10 hours

## Risks & Mitigation
- **Risk:** Breaking existing setups
  - **Mitigation:** Keep old mods in archive, provide migration guide
- **Risk:** Lua API incompatibility
  - **Mitigation:** Design API to be similar to Python version
- **Risk:** Performance issues with LuaJ
  - **Mitigation:** LuaJ is mature and performant for scripting

## Next Steps
1. Get user approval for structure
2. Begin Phase 1 (folder reorganization)
3. Test game still runs with new paths
4. Proceed with Python removal
5. Implement Lua system
