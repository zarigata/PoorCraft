# PoorCraft Manual Testing Guide

## 1. Introduction
PoorCraft combines automated tests with hands-on validation. Use this guide to perform structured manual testing alongside the automated suite.

### How to Use This Guide
- Work through each checklist for the areas you need to validate.
- Record findings and attach screenshots or logs when reporting issues.
- Use the provided scripts for a consistent environment.

### Quick Start Scripts
- Recommended unified workflow: `scripts/unified-test-and-run.bat --mode dev --quick-tests`
- Fast iteration: `scripts/quick-play.bat`
- Full validation: `scripts/test-and-play.bat`

## 2. Prerequisites
- Java JDK 17+
- Maven 3.6+
- GPU with OpenGL 3.3 support
- Optional: controller, microphone (if testing related features)

### Running Setup Scripts
```bat
scripts\quick-play.bat
scripts\test-and-play.bat --skip-tests
```
```powershell
./scripts/quick-play.ps1
./scripts/test-and-play.ps1 -Verbose
```

## 3. Feature Testing Checklist

### Core Systems
- [ ] Game launches successfully
- [ ] Main menu displays correctly
- [ ] Settings can be opened and modified
- [ ] Settings persist after restart

### World Generation
- [ ] Create new world with random seed
- [ ] Create world with specific seed
- [ ] World generates different biomes (plains, desert, forest, mountains)
- [ ] Terrain features generate correctly (trees, cacti, flowers)
- [ ] Chunks load smoothly as player moves
- [ ] No visible chunk boundaries or gaps

### Player Controls
- [ ] WASD movement works
- [ ] Mouse look works smoothly
- [ ] Jump (Space) works
- [ ] Sprint (Shift) works
- [ ] Fly mode (F) toggles correctly
- [ ] Collision detection works
- [ ] Gravity works in survival mode

### Block Interaction
- [ ] Left-click breaks blocks
- [ ] Right-click places blocks
- [ ] Block breaking shows progress animation
- [ ] Block highlight shows correctly
- [ ] Blocks drop items when broken
- [ ] Item drops can be collected

#### Focus Scenarios
- [ ] **Chunk neighbor updates** – Build a 3×3 column of blocks and break the center block. Confirm adjacent chunks update lighting/geometry without seams or delayed updates.
- [ ] **Block break overlay** – Begin mining a block and observe the highlight overlay. Verify both translucent fill and outline render without flicker or depth artifacts when moving the camera.

### Inventory System
- [ ] Inventory opens with E key
- [ ] Hotbar displays correctly
- [ ] Hotbar selection works (1-9 keys or scroll wheel)
- [ ] Items can be moved in inventory
- [ ] Item counts display correctly

### UI Systems
- [ ] HUD displays correctly (crosshair, hotbar, health)
- [ ] Pause menu works (ESC key)
- [ ] Debug info toggles (F3 key)
- [ ] Chat overlay opens (T key)
- [ ] Console opens (F1 or / key)
- [ ] UI scales correctly on window resize (see [Window Resize Testing](#window-resize-testing) for scenarios)
- [ ] **Settings menu scrolling** – Populate settings with multiple sections, scroll via wheel and drag the scrollbar. Confirm options stay aligned and clicks only affect visible rows.
- [ ] **Window resize handling** – Resize the window to minimum and maximum supported sizes. Ensure scroll containers and overlays reposition correctly with no clipped controls or misaligned hit targets.

### Console Commands
- [ ] `/help` shows command list
- [ ] `/time set <value>` changes time of day
- [ ] `/gamemode <mode>` changes game mode
- [ ] `/tp <x> <y> <z>` teleports player
- [ ] `/seed` displays world seed
- [ ] `/biome` shows current biome
- [ ] `/clear` clears console output

### Rendering
- [ ] Sky renders correctly with day/night cycle
- [ ] Lighting changes with time of day
- [ ] Fog renders correctly
- [ ] Block textures load correctly
- [ ] No visual glitches or z-fighting
- [ ] Performance is acceptable (check FPS in debug)

### Multiplayer (if implemented)
- [ ] Can host a server
- [ ] Can connect to server
- [ ] Other players are visible
- [ ] Block changes sync between clients
- [ ] Chat works in multiplayer
- [ ] Player movement syncs correctly

### Mod System
- [ ] Mods load on startup
- [ ] Example mods work correctly
- [ ] Mod events fire correctly
- [ ] No Lua errors in console

### Skin System
- [ ] Skin manager opens
- [ ] Can select different skins
- [ ] Skin editor works
- [ ] Custom skins can be created
- [ ] Skins persist after restart

## 4. Performance Testing
- Record FPS at low, medium, high render distances.
- Log memory usage after 10, 30, 60 minutes.
- note loading time for new world creation.
- Observe long-duration stability (>1h).

## 5. Known Issues
Document known defects from release notes and link to active tickets, then update this section after verifying fixes.

## 6. Reporting Issues
- Include steps to reproduce, expected vs. actual behavior, logs, hardware specs.
- Submit reports via the project issue tracker or community channel.

## 7. Quick Reference

### Keyboard Shortcuts
- `WASD` movement
- `Space` jump
- `Shift` sprint
- `F` toggle fly
- `E` inventory
- `ESC` pause menu
- `F3` debug info
- `T` chat
- `F1` or `/` console

### Console Commands
- `/help`
- `/time set <value>`
- `/gamemode <mode>`
- `/tp <x> <y> <z>`
- `/seed`
- `/biome`
- `/clear`

### Useful Debug Keys
- `F2` screenshot (if implemented)
- `F3 + H` advanced debug
- `F3 + B` bounding boxes

## 8. Window Resize Testing

Perform these checks after running the automated `WindowResizeTest` to validate real-world behavior. Expect debounce logs indicating coalesced resize events (~150 ms) and confirm no crashes or UI glitches throughout.

### Basic Resize
- Gradually drag the window edges to smaller and larger sizes.
- Confirm UI anchors and layout grids adapt without overlapping widgets.

### Rapid Resize
- Quickly resize the window back and forth between minimum and maximum widths.
- Observe logs for debounced "UIManager resize coalesced" messages and confirm FPS remains stable.

### Maximize Window
- Click the maximize button.
- Ensure blur overlays and menu backgrounds stretch cleanly with no desync.

### Restore Window
- Restore from maximized to a medium size.
- Verify retained layout state and absence of redundant blur recomputation.

### Fullscreen Toggle
- Use Alt+Enter (or in-game option) to toggle fullscreen.
- Ensure HUD scales appropriately and windowed mode restores previous bounds.

### Resize During Gameplay
- Enter a world, open the inventory and pause menu, then resize while these overlays are active.
- Confirm no input loss, cursor offsets, or duplicated blur effects.

### Resize in Different States
- Test resizing from main menu, settings screen, and during loading transitions.
- Verify each state respects the debounce interval and persists UI scale.

### Multi-Monitor
- Move the window between monitors of different resolutions and DPI scales.
- Check that UIManager recalculates scale factors and no rendering artifacts appear on the new display.

### Success Criteria
- UI logs show debounced resize handling without flooding.
- No crashes, hangs, or visual corruption occur.
- Gameplay input remains responsive and menus maintain alignment.
