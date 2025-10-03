# Pause Menu Rendering Fix

## Problem
The enhanced pause menu was initializing and rendering (confirmed by console logs), but was **not visible** on screen when pressing ESC during gameplay.

## Root Cause
**OpenGL state management issue** between 3D world rendering and 2D UI rendering.

After rendering the 3D world, several OpenGL states were left enabled that interfered with 2D UI rendering:
- **Depth Testing** - UI elements were failing depth tests against the world geometry
- **Face Culling** - Might have been culling UI quads
- **Blend Function** - Not properly set for UI transparency

## The Fix
Added explicit OpenGL state reset **before** UI rendering in `Game.java` render method:

```java
// Reset OpenGL state before UI rendering
glDisable(GL_DEPTH_TEST);  // UI doesn't need depth testing
glDisable(GL_CULL_FACE);   // Make sure back-face culling is off
glEnable(GL_BLEND);        // Need blending for transparency
glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

// Render UI on top
uiManager.render();
```

## What Changed
**File**: `src/main/java/com/poorcraft/core/Game.java`

### Before
```java
private void render() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    // Render world
    if (worldLoaded && ...) {
        chunkRenderer.render(loadedChunks, view, projection);
    }
    
    // Render UI on top
    uiManager.render();  // ❌ UI wasn't visible!
}
```

### After
```java
private void render() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    // Render world
    if (worldLoaded && ...) {
        glEnable(GL_DEPTH_TEST);  // Enable for 3D
        glDepthFunc(GL_LESS);
        
        chunkRenderer.render(loadedChunks, view, projection);
    }
    
    // Reset state for 2D UI
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    
    // Render UI on top
    uiManager.render();  // ✅ UI now visible!
}
```

## Why This Happens
This is a classic OpenGL rendering issue when mixing 3D and 2D rendering:

1. **3D world rendering** enables depth testing to handle occlusion between chunks
2. After 3D rendering, depth buffer is filled with world geometry
3. **2D UI rendering** attempts to draw at Z=0 (near plane)
4. With depth testing still enabled, UI quads fail the depth test because the world geometry is in front
5. Result: UI is rendered but immediately discarded by the GPU

## Prevention
Always explicitly manage OpenGL state when switching between:
- 3D rendering (depth testing ON)
- 2D rendering (depth testing OFF)

## Testing
Run the game and verify:
1. ✅ Press ESC during gameplay
2. ✅ Pause menu should now be **visible** with:
   - Semi-transparent dark overlay
   - Panel with cyan border
   - Quick settings sliders on the left
   - Mod list on the right
   - Four buttons at the bottom
3. ✅ Press ESC again to resume
4. ✅ All UI elements should be interactive

## Similar Issues to Watch For
This same fix applies to other UI screens rendered over the game world:
- Settings menu (when opened from pause)
- Any future in-game overlays
- Death screens
- Achievement popups

**General Rule**: Always reset OpenGL state before switching render modes!

---

**Fixed Date**: 2025-10-03  
**Status**: ✅ Fixed and Ready to Test
