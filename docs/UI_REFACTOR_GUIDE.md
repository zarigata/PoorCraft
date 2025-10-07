# Minecraft-Style UI Refactor Guide

## Overview
This guide documents the Minecraft-inspired UI refactor applied across PoorCraft's menu system. The goals are:

- **Consistency**: standardize layout math through `LayoutUtils.java`.
- **Readability**: improve text contrast with shadow-aware rendering in `FontRenderer.java` and `Label.java`.
- **Polish**: introduce reusable drop shadow and panel helpers in `UIRenderer.java`.
- **Depth**: update `MenuButton.java`, `MainMenuScreen.java`, and `PauseScreen.java` to mimic classic Minecraft visual depth while remaining responsive.

## Layout System
`src/main/java/com/poorcraft/ui/LayoutUtils.java` now exposes Minecraft-style constants and helpers.

- **Constants**
  - `MINECRAFT_BUTTON_WIDTH`
  - `MINECRAFT_BUTTON_HEIGHT`
  - `MINECRAFT_BUTTON_SPACING`
  - `MINECRAFT_PANEL_PADDING`
  - `MINECRAFT_TITLE_SCALE`
  - `MINECRAFT_SUBTITLE_SCALE`
  - `MINECRAFT_LABEL_SCALE`

- **Helpers**
  - `getMinecraftButtonWidth(int windowWidth, float uiScale)` (clamped 180-400)
  - `getMinecraftButtonHeight(int windowHeight, float uiScale)` (clamped 40-80)
  - `getMinecraftButtonSpacing(float buttonHeight)` (30% of height)
  - `getMinecraftPanelWidth(int windowWidth, float uiScale)` (50% width, clamped 400-600)
  - `getMinecraftPanelHeight(int windowHeight, float uiScale)` (60% height, clamped 400-700)
  - `getMinecraftPanelPadding(float panelWidth)` (8% of width, clamped 24-48)
  - `calculateButtonStackHeight(int buttonCount, float buttonHeight, float spacing)`
  - `centerButtonStack(int windowHeight, float stackHeight)`

### Usage Example
```java
float uiScale = settings.graphics.uiScale;
float panelWidth = LayoutUtils.getMinecraftPanelWidth(windowWidth, uiScale);
float panelHeight = LayoutUtils.getMinecraftPanelHeight(windowHeight, uiScale);
float panelX = LayoutUtils.centerHorizontally(windowWidth, panelWidth);
float panelY = LayoutUtils.centerVertically(windowHeight, panelHeight);
```

## Visual Polish Helpers
`src/main/java/com/poorcraft/ui/UIRenderer.java` now includes reusable drawing utilities:

- **`drawDropShadow(x, y, width, height, offset, alpha)`** for soft panel/button shadows.
- **`drawInsetPanel(...)`** for recessed panels with darker top/left borders.
- **`drawOutsetPanel(...)`** for raised panels with lighter top/left borders.
- **`drawBorderedRect(...)`** for consistent border rendering.

These helpers wrap existing `drawRect()` calls to keep OpenGL flow unchanged while standardizing appearance. Use them whenever you need Minecraft-style depth cues.

## Text Rendering Enhancements
`src/main/java/com/poorcraft/ui/FontRenderer.java` gains `drawTextWithShadow` overloads:

```java
fontRenderer.drawTextWithShadow(
    "New World", labelX, labelY, textScale,
    1f, 1f, 1f, 1f, 2.5f, 0.6f
);
```

Defaults (offset `2.0f`, alpha `0.6f`) match Minecraft's drop shadow look. `Label.java` exposes `setUseTextShadow`, `setShadowOffset`, and `setShadowAlpha` to toggle these effects per component.

## Button Enhancements
`src/main/java/com/poorcraft/ui/MenuButton.java` updates deliver:

- Hover scale animation (max 1.02x) with pressed dampening.
- Conditional drop shadows via `UIRenderer.drawDropShadow()`.
- Refined border math with brighter highlights and darker shadows.
- Procedural corner pixels and gradients when no texture is available.
- Text drawn through `FontRenderer.drawTextWithShadow` for consistent readability.

Existing button APIs remain unchanged for compatibility.

## Screen Refactors
### `MainMenuScreen.java`
- Uses layout helpers for panel and button sizing.
- Applies standardized title/subtitle/tagline scales.
- Centers button stack using `LayoutUtils.calculateButtonStackHeight`.
- Renders panel with `drawDropShadow` + `drawOutsetPanel`.
- Enables text shadows on all labels.

### `PauseScreen.java`
- Scales panel to 85% width / 80% height of defaults for a tighter layout.
- Centers buttons using the helper stack height math with top-padding fallback.
- Adds stronger overlay opacity when blur is disabled for readability.
- Layers `drawOutsetPanel`, `drawBorderedRect`, and an interior border for depth.
- Updates footer hint to use shadowed text.

## Migration Guide
1. **Panel Setup**: Replace manual width/height math with `LayoutUtils` helpers.
2. **Button Stack**: Compute width/height/spacing via helpers and center accordingly.
3. **Labels**: Switch to `Label.setUseTextShadow(true)` where text overlaps dynamic backgrounds.
4. **Rendering**: Swap manual panel borders for `UIRenderer.drawOutsetPanel` or `drawInsetPanel`. Add `drawDropShadow` before main panel draw.
5. **Buttons**: Instantiate `MenuButton` with new defaults; no API changes required.

### Checklist
- [ ] Use `LayoutUtils` for all menu panel/button calculations.
- [ ] Call `drawDropShadow` before rendering panels or dialogs.
- [ ] Prefer `drawOutsetPanel`/`drawInsetPanel` over bespoke border code.
- [ ] Enable text shadows on headings, subtitles, and footers.
- [ ] Verify hover/pressed animations remain smooth.

## Best Practices
- **Consistency**: Always pull spacing/scale numbers from `LayoutUtils` rather than hardcoding.
- **Readability**: Enable text shadows for labels over animated or low-contrast backgrounds.
- **Opacity**: Use alpha ≥ 0.94 for pause menus, 0.90-0.94 for main menus.
- **Spacing**: Maintain at least 30% button spacing to avoid crowding on small screens.
- **Testing**: Validate layouts at UI scales 0.75x, 1.0x, and 1.5x.

## Examples
### Basic Menu Panel
```java
float uiScale = settings.graphics.uiScale;
float panelWidth = LayoutUtils.getMinecraftPanelWidth(width, uiScale);
float panelHeight = LayoutUtils.getMinecraftPanelHeight(height, uiScale);
float panelX = LayoutUtils.centerHorizontally(width, panelWidth);
float panelY = LayoutUtils.centerVertically(height, panelHeight);

renderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 8f, 0.45f);
renderer.drawOutsetPanel(panelX, panelY, panelWidth, panelHeight,
    0.08f, 0.09f, 0.12f, 0.94f);
```

### Shadowed Label
```java
Label title = new Label(centerX, panelY + padding, "SETTINGS", 1f, 1f, 1f, 1f);
title.setCentered(true);
title.setScale(LayoutUtils.MINECRAFT_TITLE_SCALE * uiScale);
title.setUseTextShadow(true);
addComponent(title);
```

### Button Stack Placement
```java
float buttonHeight = LayoutUtils.getMinecraftButtonHeight(height, uiScale);
float spacing = LayoutUtils.getMinecraftButtonSpacing(buttonHeight);
float stackHeight = LayoutUtils.calculateButtonStackHeight(buttonCount, buttonHeight, spacing);
float startY = panelY + LayoutUtils.centerButtonStack((int) panelHeight, stackHeight);
```

## Troubleshooting
- **Text clips panel edges**: Reduce `scale` or increase padding via `LayoutUtils.getMinecraftPanelPadding`.
- **Buttons overflow**: Lower `uiScale`, reduce button count, or downscale `buttonWidth` multiplier.
- **Panel appears flat**: Ensure `drawDropShadow` precedes `drawOutsetPanel`; confirm alpha ≥ 0.4.
- **Shadows too dark/light**: Adjust shadow alpha in `drawDropShadow` or `drawTextWithShadow` to match backdrop.
- **Low-resolution layout**: Verify `getMinecraftButtonWidth` clamp ranges suit target minimum resolution; tweak clamps if necessary.
