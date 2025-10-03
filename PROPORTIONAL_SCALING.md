# Proportional Button Scaling - 75% Width

## ✅ What Changed

Buttons now scale **proportionally** with screen size at ALL resolutions.

### Old System (Fixed Limits):
```java
buttonWidth = clamp(windowWidth * 0.40f, 400f, 600f);
buttonHeight = 40f;  // Fixed
```
- Small screens: 400px (minimum)
- Large screens: 600px (maximum)
- **Problem**: Not proportional - same size at different resolutions

### New System (Proportional):
```java
buttonWidth = windowWidth * 0.75f;   // 75% of screen width
buttonHeight = windowHeight * 0.055f; // 5.5% of screen height
buttonSpacing = buttonHeight * 0.25f; // 25% of button height
```
- **ALL screens**: 75% of width
- **ALL screens**: 5.5% of height
- **Result**: Perfect scaling at any resolution!

## 📊 Button Sizes at Different Resolutions

| Resolution  | Button Width | Button Height | % of Screen |
|-------------|--------------|---------------|-------------|
| 800x600     | 600px        | 33px          | 75% wide    |
| 1280x720    | 960px        | 39.6px        | 75% wide    |
| 1920x1080   | 1440px       | 59.4px        | 75% wide    |
| 2560x1440   | 1920px       | 79.2px        | 75% wide    |
| 3840x2160   | 2880px       | 118.8px       | 75% wide    |

**The buttons maintain the SAME proportions at every resolution!**

## 🎯 Visual Representation

### At 1280x720:
```
┌────────────────────────────────────────────────────────┐
│                                                        │
│                    PoorCraft                           │
│                  RETRO EDITION                         │
│                                                        │
│  ┌────────────────────────────────────────────────┐  │
│  │          SINGLEPLAYER (960px = 75%)            │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │          MULTIPLAYER                           │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │          SETTINGS                              │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │          QUIT                                  │  │
│  └────────────────────────────────────────────────┘  │
│                                                        │
└────────────────────────────────────────────────────────┘
   ↑──────────────── 75% of width ──────────────────↑
```

### At 1920x1080 (Same proportions, bigger screen):
```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│                         PoorCraft                                    │
│                       RETRO EDITION                                  │
│                                                                      │
│   ┌──────────────────────────────────────────────────────────────┐ │
│   │              SINGLEPLAYER (1440px = 75%)                     │ │
│   └──────────────────────────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────────────────────────┐ │
│   │              MULTIPLAYER                                     │ │
│   └──────────────────────────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────────────────────────┐ │
│   │              SETTINGS                                        │ │
│   └──────────────────────────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────────────────────────┐ │
│   │              QUIT                                            │ │
│   └──────────────────────────────────────────────────────────────┘ │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
    ↑────────────────── 75% of width ──────────────────────↑
```

**Notice**: The button takes up the SAME percentage of screen space!

## 🧮 Math Behind It

### Width Calculation:
```
buttonWidth = windowWidth * 0.75
```
- 800px screen → 600px button (75%)
- 1920px screen → 1440px button (75%)
- 3840px screen → 2880px button (75%)

### Height Calculation:
```
buttonHeight = windowHeight * 0.055
```
- 600px screen → 33px button (5.5%)
- 1080px screen → 59px button (5.5%)
- 2160px screen → 119px button (5.5%)

### Spacing Calculation:
```
buttonSpacing = buttonHeight * 0.25
```
- Scales with button height
- Always 25% of button height
- Maintains proportions

## ✅ Benefits

1. **Perfect Scaling**: Buttons look the same at ANY resolution
2. **No Weird Sizes**: Always fills 75% of screen width
3. **Consistent UX**: Same visual experience everywhere
4. **Future-Proof**: Works on tiny and massive displays
5. **Simple Math**: Easy to adjust (just change 0.75 to different %)

## 🎮 Test It

```powershell
.\run-poorcraft.bat
```

### Resize Test:
1. Start the game
2. Resize window to TINY (like 800x600)
3. Buttons fill 75% of width ✅
4. Resize to HUGE (like fullscreen 4K)
5. Buttons STILL fill 75% of width ✅
6. Everything scales perfectly!

### Console Output:
```
[MenuButton] Rendering 'SINGLEPLAYER' at 160.0,285.0 size 960.0x39.6
```
- Position and size change with window
- But proportions stay the same

## 🔧 Customization

Want different button sizes? Change these values:

```java
// In MainMenuScreen.java and PauseScreen.java
float buttonWidth = windowWidth * 0.75f;   // Change 0.75 for different width %
float buttonHeight = windowHeight * 0.055f; // Change 0.055 for different height %
```

**Examples**:
- 50% width: `windowWidth * 0.50f`
- 90% width: `windowWidth * 0.90f`
- Bigger height: `windowHeight * 0.08f`

## 📝 Files Modified

1. ✅ `MainMenuScreen.java` - Proportional button sizing
2. ✅ `PauseScreen.java` - Proportional button sizing

## 🎯 Result

**Buttons now scale perfectly with screen size!**
- ✅ Resize detection works
- ✅ Buttons are clickable
- ✅ Proportional scaling (75% width)
- ✅ Same visual appearance at ALL resolutions
- ✅ No more fixed min/max limits

---

**The UI is now truly responsive and proportional! 🎮**
