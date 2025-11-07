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

### Tree Felling & Leaf Decay
- [ ] **Basic Tree Felling** – Break the bottom log of a tall tree (4+ logs). Verify the entire trunk disappears instantly, dropping one log per segment.
- [ ] **Short Tree Handling** – Fell a 2-log sapling. Confirm only the targeted block breaks and the top log persists.
- [ ] **Mid-Trunk Break** – Break a middle log of a 4+ log tree. Confirm the entire trunk collapses, drops match trunk height, and leaves enter decay queue.
- [ ] **Top-Log Break** – Break the top log of a tall tree. Confirm the trunk still collapses fully and leaves are marked for decay.
- [ ] **Leaf Decay Timing** – After felling a tall tree, observe the canopy for 30–60 seconds. Leaves should disappear gradually, with occasional leaf drops.
- [ ] **Supported Leaves** – Place logs within 5 blocks (Chebyshev distance) of a canopy and mark leaves manually (break/re-place one). Confirm nearby leaves persist while distant ones decay.
- [ ] **Multiple Trees** – Fell one tree in a cluster of two. Ensure only the targeted tree falls and only its leaves decay. Leaves marked during felling now track a per-tree decay group, so neighbouring trunks should no longer keep felled leaves alive.
- [ ] **Chunk Boundary Trees** – Fell a tree straddling a chunk edge (x or z = 0/15). Confirm all trunk blocks vanish and no mesh artifacts appear.
- [ ] **Player-Placed Trees** – Build a custom tree from logs and leaves. Breaking the bottom log should trigger full felling and leaf decay.
- [ ] **Performance Check** – Rapidly fell 5–10 trees in succession. Confirm no noticeable frame drops during log removal or leaf decay processing.

### Player Controls
- [ ] WASD movement works
- [ ] Mouse look works smoothly
- [ ] Jump (Space) works
- [ ] Sprint (Shift) works
- [ ] Fly mode (F) toggles correctly
- [ ] Collision detection works
- [ ] Gravity works in survival mode

#### Mouse Wheel Hotbar Selection
- [ ] **In-Game** – Enter a survival world with no overlays open. Scroll the mouse wheel up/down and confirm the hotbar selector moves accordingly with no skipped slots.
- [ ] **Menus & Overlays** – Open the pause menu, inventory, chat, and console individually. Scroll the wheel and verify the hotbar does **not** change while the visible screen scrolls as expected.
- [ ] **Settings Menu** – Open Settings → Controls. Use the mouse wheel to scroll long lists and confirm list position updates smoothly without affecting gameplay hotbar selection when you return in-game.

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

### AI Companion Settings Screen

1. **Visual Style Consistency**
   - Open the settings menu (ESC) and switch to the AI tab.
   - Click **Configure AI Companion…**.
   - Confirm the screen shows the textured menu background, cyan-tinted title with shadow, and turquoise MenuButton controls.
   - Ensure the central panel has a drop shadow, outset edges, and cyan border accents like the Pause screen.

2. **Navigation – Cancel Button**
   - Modify one or two fields (e.g., toggle “Enable AI Companion”).
   - Click **Cancel**.
   - Expect an immediate return to the main settings menu with previous values intact and no freeze.

3. **Navigation – Save Button**
   - Re-enter the AI Companion settings screen.
   - Change companion name, skin, and provider, then click **Save**.
   - Confirm the settings menu appears instantly and reopening the AI screen reflects saved values.

4. **Test Connection Button**
   - Enable the AI companion and select a configured provider.
   - Use the **Test** button and watch for console feedback without UI hangs.
   - With AI disabled or the game context missing, ensure the screen logs the appropriate warning and remains responsive.

5. **Window Resize**
   - While on the AI Companion settings screen, resize the game window between typical resolutions (e.g., 1280×720 → 1920×1080).
   - Verify the panel remains centered, scroll content resizes, and all buttons stay visible/interactive.

6. **Scroll Container**
   - Scroll through all configuration sections.
   - Ensure every option is reachable, the scrollbar behaves smoothly, and there are no overlapping components.

> **Success Criteria:** Consistent visual style with other menus, reliable navigation (Save/Cancel/Test), graceful handling of missing game context, correctly scaling layout, and functional scrolling.

### AI Companion System

1. Launch the game and load a test world with AI enabled.
- [ ] Verify the AI companion spawns near the player and follows commands.

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

#### 3D Block Preview Rendering
- [ ] Open the inventory, hover items that show 3D previews, then close with `E`. Immediately open the pause menu and verify all UI elements are fully visible (no clipping or blank screens).
- [ ] Open and close the inventory repeatedly (10x). Confirm previews render consistently and the rest of the UI remains stable.
- [ ] Trigger multiple preview tooltips in different menus (inventory, crafting, creative tabs). Confirm no blue background or missing UI elements appear afterwards.

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

### Troubleshooting Fixed Regressions
- **Pause menu invisible / inventory turns blue** – Ensure you are running PoorCraft build X.X.X or later. The BlockPreviewRenderer now restores scissor and blend state correctly. If UI corruption reappears, restart the client and retest.
- **Mouse wheel fails to scroll hotbar** – Confirm you are in gameplay with chat/console closed. Scroll routing now checks the active UI state; if the issue persists, capture logs for `Game.shouldForwardScrollToUI()`.
- **Tree felling fails after breaking upper logs** – Trees now fall when any trunk segment is destroyed (minimum total height 4 logs). If a trunk remains, verify it meets height requirements and gather a reproduction world seed.

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
