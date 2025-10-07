# Menu Testing Guide

## Quick Test

1. **Run the game**:
   ```powershell
   .\run-poorcraft.bat
   ```

2. **Check main menu**:
   - Do you see 4 rectangular buttons with colored borders? ✅
   - Do you see a textured background (stone/grass pattern)? ✅
   - Hover over a button - does it change color? ✅
   - Click a button - does it work? ✅

3. **Resize test**:
   - Drag window corner to make it smaller
   - Do buttons move to stay centered? ✅
   - Try to click buttons - do they still work? ✅
   - Make window tiny (500x400) - still works? ✅
   - Make window huge (fullscreen) - still works? ✅

4. **In-game pause test**:
   - Click "SINGLEPLAYER"
   - Create a world and start the game
   - Press ESC to pause
   - Do you see the pause menu with 3 buttons? ✅
   - Can you click them? ✅
   - Resize while paused - still works? ✅

## What You Should See

### Main Menu:
```
┌───────────────────────────────────────┐
│                                       │
│        PoorCraft (cyan text)          │
│        RETRO EDITION (pink text)      │
│                                       │
│    ┌─────────────────────┐           │
│    │  SINGLEPLAYER       │  ← Purple rectangle
│    └─────────────────────┘           │
│    ┌─────────────────────┐           │
│    │  MULTIPLAYER        │           │
│    └─────────────────────┘           │
│    ┌─────────────────────┐           │
│    │  SETTINGS           │           │
│    └─────────────────────┘           │
│    ┌─────────────────────┐           │
│    │  QUIT               │           │
│    └─────────────────────┘           │
│                                       │
│  [Background: tiled texture pattern]  │
│  ~ Press buttons to navigate... ~     │
└───────────────────────────────────────┘
```

### Button States:
- **Normal**: Dark purple with pink border
- **Hover**: Lighter purple with cyan border (smooth transition)
- **Click**: Darkest purple with cyan border

### Pause Menu (press ESC in game):
```
┌───────────────────────────────────────┐
│                                       │
│  [Game world visible but dimmed]      │
│                                       │
│      GAME PAUSED (cyan text)          │
│                                       │
│      ┌──────────────┐                 │
│      │  RESUME      │                 │
│      └──────────────┘                 │
│      ┌──────────────┐                 │
│      │  SETTINGS    │                 │
│      └──────────────┘                 │
│      ┌──────────────┐                 │
│      │  SAVE & QUIT │                 │
│      └──────────────┘                 │
│                                       │
│    Press ESC to resume (gray text)    │
└───────────────────────────────────────┘
```

## Troubleshooting

### "I don't see any buttons, just text!"
**This should NOT happen anymore!** But if it does:
1. Check console output for errors
2. Make sure you compiled: `mvn clean compile`
3. Try restarting the game

### "Buttons don't respond to clicks after resize"
**This should NOT happen anymore!** But if it does:
1. Check console - you should see "[MainMenuScreen] Resize detected: WxH"
2. Check if components were rebuilt
3. Report as a bug (this is the main thing we fixed!)

### "Background is just solid color, no texture"
**Possible causes**:
1. Textures failed to load - check console for errors
2. This is expected if texture loading fails (fallback behavior)
3. As long as buttons work, this is not critical

### "Text is hard to read"
**Solutions**:
- Text has shadows for readability
- If still hard to read, report the window size
- May need font size adjustment for very small windows

## Console Output to Expect

### On Startup:
```
[MenuBackground] Loaded 5 background textures
[UIManager] Loaded Silkscreen font - looking retro!
[MainMenuScreen] Layout initialized for 1280x720
```

### On Resize:
```
[MainMenuScreen] Resize detected: 1920x1080
[MainMenuScreen] Resize complete, components rebuilt
```

### On Pause:
```
[UIManager] State transition: IN_GAME -> PAUSED
[PauseScreen] Layout initialized for 1920x1080
```

## Extreme Testing

### Tiny Window (500x400):
- Buttons should be minimum size (250x50)
- Layout should still be centered
- Everything should still work

### Huge Window (3840x2160):
- Buttons should be maximum size (450x75)
- Not too large to look weird
- Still centered and functional

### Rapid Resize:
- Drag window corner quickly back and forth
- Buttons should stay responsive
- No crashes or freezes

## Success Criteria

All of these should be true:

✅ Buttons always visible as rectangles  
✅ Buttons always clickable  
✅ Buttons respond to hover  
✅ Resize maintains functionality  
✅ Layout always centered  
✅ Text always readable  
✅ Background texture visible (or fallback color)  
✅ No errors in console  
✅ Smooth performance (60 FPS)  

## Report Issues

If anything doesn't work as described:
1. Note your window size
2. Note what you were doing (resize, click, etc.)
3. Check console output
4. Take a screenshot if possible
5. Describe what you expected vs what happened

---

**Happy Testing! 🎮**
