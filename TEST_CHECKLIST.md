# PoorCraft Test Checklist

## Build System
- [ ] Maven build completes without errors
- [ ] JAR generated in `target/`
- [ ] Python dependencies install successfully
- [ ] No Java compilation errors

## Startup
- [ ] Game window opens
- [ ] Main menu appears without crashing
- [ ] Console shows initialization progress
- [ ] Py4J bridge starts
- [ ] Mods load without errors

## Main Menu
- [ ] Title text renders
- [ ] Buttons are visible and clickable
- [ ] **Singleplayer** opens world selection
- [ ] **Multiplayer** opens server menu
- [ ] **Settings** opens configuration screen
- [ ] **Quit** exits to desktop

## Settings Menu
- [ ] Graphics tab sliders respond
- [ ] Audio tab volume controls adjust values
- [ ] Controls tab lists keybinds
- [ ] AI tab shows provider options
- [ ] Apply button saves changes
- [ ] Cancel button reverts changes

## World Creation
- [ ] World creation screen opens
- [ ] World name input works
- [ ] Seed input accepts values
- [ ] Create World generates terrain
- [ ] Transition to in-game view succeeds

## Core Gameplay
- [ ] Terrain renders with lighting
- [ ] Movement via `WASD`
- [ ] Jump (`Space`), sneak (`Shift`), sprint (`Ctrl`) operate
- [ ] Mouse look responds smoothly
- [ ] ESC opens pause menu

## World & Rendering
- [ ] Chunks load as player moves
- [ ] Chunks unload behind player
- [ ] Multiple biomes appear (desert, snow, jungle, plains)
- [ ] Placeholder textures display when PNGs missing
- [ ] Lighting works without flicker
- [ ] Frustum culling prevents off-screen rendering

## HUD & UI
- [ ] Crosshair centered
- [ ] Hotbar visible
- [ ] F3 debug overlay toggles info (FPS, position, chunk count)
- [ ] Pause menu overlays world correctly

## Multiplayer
- [ ] Host Game starts server without errors
- [ ] Client can connect via localhost
- [ ] Direct Connect input validates IP:port
- [ ] Connection attempts show status messages

## Mods
- [ ] Mods listed in console at startup
- [ ] Skin Generator reports generated textures
- [ ] AI NPC mod gracefully handles missing provider
- [ ] Mod init hooks fire without exceptions

## Shutdown
- [ ] Game exits cleanly via Quit
- [ ] Mods receive disable callbacks
- [ ] Py4J bridge closes
- [ ] No leftover processes

## Performance Targets
- [ ] Startup < 10 seconds
- [ ] World generation < 10 seconds
- [ ] 30+ FPS on mid-tier hardware (60+ ideal)
- [ ] Memory usage < 2 GB

## Known Limitations
- ❌ Block breaking/placing not implemented
- ❌ No inventory system
- ❌ No chat system
- ❌ No player models (camera only)
- ❌ No audio
- ❌ No world saving/loading
- ❌ Font rendering may fallback to hidden text

## Completion Criteria
- **Minimum viable**: startup success, world render, movement, chunk streaming, multi-biome support
- **Fully functional**: plus mods active, textures/fonts loaded, multiplayer connection, stable 60 FPS

Good luck tester — may your logs be short and your frames high.
