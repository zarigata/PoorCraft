# Vaporwave Menu System Update

## Overview
Complete redesign of the PoorCraft menu system with **Vaporwave aesthetics**, **professional responsive design**, and the **Silkscreen retro font**.

## What Was Changed

### 1. **New VaporwaveButton Component**
   - File: `src/main/java/com/poorcraft/ui/VaporwaveButton.java`
   - Features:
     - Gradient backgrounds (pink/purple → cyan/blue on hover)
     - Animated glow borders with multi-layer effects
     - Smooth hover transitions
     - Pulsing animation when hovered
     - Text shadows for better readability
     - Full support for disabled state

### 2. **MainMenuScreen - Complete Redesign**
   - File: `src/main/java/com/poorcraft/ui/MainMenuScreen.java`
   - Features:
     - **Fully responsive layout** - adapts to any window size
     - Vaporwave gradient background (purple → blue)
     - Animated scanline effect for retro CRT look
     - New color scheme:
       - Title: Electric cyan
       - Subtitle: Hot pink
       - Footer: Purple tint
     - All buttons use new VaporwaveButton component
     - Smart sizing constraints (min/max bounds)

### 3. **PauseScreen - Matching Design**
   - File: `src/main/java/com/poorcraft/ui/PauseScreen.java`
   - Features:
     - Same Vaporwave aesthetic as main menu
     - Semi-transparent gradient overlay
     - Pulsing scanline animation
     - Responsive button layout
     - Helpful "Press ESC to resume" hint

### 4. **Silkscreen Font Integration**
   - Copied fonts to: `src/main/resources/fonts/`
     - Silkscreen-Regular.ttf
     - Silkscreen-Bold.ttf
   - Updated `UIManager.java` to load Silkscreen font (size 20px)
   - Retro pixelated aesthetic perfect for the Vaporwave theme

## Responsive Design Features

### Window Resizing
The menu now **automatically adapts** to any window size:

- **Buttons**: Scale between 280px-500px width based on screen size
- **Spacing**: Proportional to button height
- **Title**: Scales from 40px-80px based on window height
- **Layout**: Always centered vertically and horizontally
- **Minimum sizes**: Ensures readability on small screens
- **Maximum sizes**: Prevents awkward scaling on ultra-wide displays

### How It Works
When the window is resized:
1. `onResize()` is called with new dimensions
2. Layout is completely rebuilt with `init()`
3. All sizes and positions recalculated
4. Components maintain perfect proportions

## Color Palette

### Vaporwave Colors Used:
- **Electric Cyan**: `RGB(0, 242, 242)` - Titles, hover states
- **Hot Pink**: `RGB(250, 66, 160)` - Accents, normal buttons
- **Deep Purple**: `RGB(102, 56, 184)` - Gradients, backgrounds
- **Indigo**: `RGB(74, 0, 130)` - Dark gradients
- **Dark Purple/Blue**: Background gradients

## Testing the Menu

### To Test:
1. Run the game: `.\run-poorcraft.bat` or `.\run-poorcraft.ps1`
2. Main menu should appear with Vaporwave design
3. Try resizing the window - watch buttons adapt smoothly
4. Hover over buttons - see the gradient and glow effects
5. Start a game and press ESC - pause menu has matching design

### Expected Behavior:
✅ Buttons have gradient backgrounds  
✅ Hover effects are smooth and animated  
✅ Glow borders pulse on hover  
✅ Text uses Silkscreen retro font  
✅ Layout stays centered on resize  
✅ Background has subtle scanline animation  
✅ All buttons properly aligned and sized  

## Technical Details

### Animation System:
- `animationTime` tracks elapsed time
- Scanlines move continuously for retro effect
- Hover effects smoothly interpolate (8x speed)
- Pulse effects use sine wave

### Performance:
- Gradient backgrounds: 20-40 steps (smooth but efficient)
- Glow borders: 3 layers for blur effect
- All animations optimized for 60+ FPS

## Files Modified

1. ✅ `src/main/java/com/poorcraft/ui/VaporwaveButton.java` (NEW)
2. ✅ `src/main/java/com/poorcraft/ui/MainMenuScreen.java` (UPDATED)
3. ✅ `src/main/java/com/poorcraft/ui/PauseScreen.java` (UPDATED)
4. ✅ `src/main/java/com/poorcraft/ui/UIManager.java` (UPDATED - font path)
5. ✅ `src/main/resources/fonts/Silkscreen-Regular.ttf` (ADDED)
6. ✅ `src/main/resources/fonts/Silkscreen-Bold.ttf` (ADDED)

## Future Enhancements (Optional)

- Add VaporwaveButton to other menus (Settings, Multiplayer, etc.)
- Implement animated background with moving grid lines
- Add sound effects for button clicks (synthwave sounds?)
- Create custom cursor for full aesthetic
- Add particle effects on button hover

## Notes

- The compiler warning about JRE 17 vs 21 is harmless (project configured for Java 17)
- Font fallback is handled gracefully if Silkscreen fails to load
- All existing button functionality preserved (click callbacks, disabled states)
- Comments include retro gaming references for authenticity

---

**Enjoy your new AESTHETIC menu! 🌴🌊💜**
