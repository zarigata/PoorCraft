# PoorCraft Refactor - Texture & Skin System Guide

This guide explains how to create, modify, and manage block textures (skins) in PoorCraft Refactor.

## Table of Contents

- [Overview](#overview)
- [Texture Format](#texture-format)
- [Creating Textures](#creating-textures)
- [Texture Atlas](#texture-atlas)
- [Custom Skin Packs](#custom-skin-packs)
- [Atlas Packer Tool](#atlas-packer-tool)
- [Troubleshooting](#troubleshooting)

## Overview

PoorCraft uses a **one texture per block** system. Each block type has a dedicated PNG texture file. At runtime (or build time), these textures are packed into a single texture atlas for efficient rendering.

### Key Features

- **Per-block textures**: Each block has its own PNG file
- **Automatic atlas packing**: Textures are automatically combined into an atlas
- **Hot reload**: Edit textures and restart to see changes
- **Multiple resolutions**: Support for 16×16 or 32×32 textures
- **Custom skin packs**: Create and share texture packs

## Texture Format

### Requirements

- **Format**: PNG (Portable Network Graphics)
- **Bit depth**: 32-bit RGBA (with alpha channel)
- **Dimensions**: Square (16×16 or 32×32 pixels)
- **Naming**: `<block_name>.png` (lowercase, no spaces)

### Supported Sizes

| Size | Description | Performance |
|------|-------------|-------------|
| 16×16 | Default, retro style | Best |
| 32×32 | Higher detail | Good |

**Note**: All textures in a skin pack must be the same size.

### File Naming Convention

Texture files must match block names exactly:

```
grass.png       → Grass block
dirt.png        → Dirt block
stone.png       → Stone block
wood.png        → Wood block
leaves.png      → Leaves block
sand.png        → Sand block
water.png       → Water block (supports transparency)
glass.png       → Glass block (supports transparency)
brick.png       → Brick block
planks.png      → Planks block
```

## Creating Textures

### Recommended Tools

- **Aseprite** - Pixel art editor (paid, excellent for game sprites)
- **GIMP** - Free, powerful image editor
- **Paint.NET** - Free, Windows-only
- **Piskel** - Free, web-based pixel art tool
- **Photoshop** - Professional (paid)

### Design Guidelines

#### 1. Pixel Art Style

For 16×16 textures, embrace the pixel art aesthetic:

```
✓ Clear, distinct pixels
✓ Limited color palette
✓ High contrast
✗ Anti-aliasing (usually)
✗ Gradients (use dithering instead)
```

#### 2. Tiling

Textures should tile seamlessly on all sides:

```
+-------+-------+
|       |       |
|  TEX  |  TEX  |
|       |       |
+-------+-------+
|       |       |
|  TEX  |  TEX  |
|       |       |
+-------+-------+
```

**Test tiling**: Duplicate your texture 2×2 in your editor to check seams.

#### 3. Transparency

Use alpha channel for transparent blocks:

- **Water**: Semi-transparent (alpha ~128)
- **Glass**: Mostly transparent (alpha ~64)
- **Leaves**: Opaque with transparent gaps

#### 4. Shading

Consider how lighting affects blocks:

- **Top faces**: Brightest (100% brightness)
- **Side faces**: Medium (80% brightness)
- **Bottom faces**: Darkest (50% brightness)

The engine applies automatic shading, so design for neutral lighting.

### Example: Creating a Grass Texture

1. **Create new image**: 16×16 pixels, RGBA
2. **Base color**: Fill with green (#5C8A3C)
3. **Add variation**: Add lighter/darker green pixels for texture
4. **Top edge**: Add brown/dirt color for grass-dirt transition
5. **Test tiling**: Check that edges match
6. **Save**: Export as `grass.png`

### Color Palette Suggestions

#### Natural Blocks

```
Grass:  #5C8A3C, #4A7C2E, #6B9E47
Dirt:   #8B6F47, #6E5839, #A0825D
Stone:  #7F7F7F, #6B6B6B, #939393
Sand:   #E0D5A7, #D4C89A, #EDE1B8
```

#### Wood Blocks

```
Oak:    #9C7F4F, #8A6E42, #B39560
Birch:  #D7D3C7, #C5C1B5, #E5E1D5
Dark:   #3F2817, #2E1A0B, #523620
```

#### Ores & Minerals

```
Coal:   #2B2B2B, #1A1A1A, #3C3C3C
Iron:   #D8AF93, #C49E82, #E8BFA3
Gold:   #FCEE4B, #E8D738, #FFF85E
Diamond:#5DECF5, #4ADAE4, #6EFAFF
```

## Texture Atlas

### What is a Texture Atlas?

A texture atlas combines multiple small textures into one large texture. This improves rendering performance by reducing texture switches.

```
Individual Textures:        Texture Atlas:
┌────┐ ┌────┐ ┌────┐       ┌────┬────┬────┐
│Grass│ │Dirt│ │Stone│      │Grass│Dirt│Stone│
└────┘ └────┘ └────┘       ├────┼────┼────┤
┌────┐ ┌────┐ ┌────┐       │Wood│Leaf│Sand│
│Wood│ │Leaf│ │Sand│   →   ├────┼────┼────┤
└────┘ └────┘ └────┘       │Water│Glass│Brick│
                            └────┴────┴────┘
```

### Atlas Files

The atlas system generates two files:

1. **atlas.png** - Combined texture image
2. **atlas.json** - UV coordinate mapping

Example `atlas.json`:

```json
{
  "grass": {
    "u1": 0.0,
    "v1": 0.0,
    "u2": 0.25,
    "v2": 0.25
  },
  "dirt": {
    "u1": 0.25,
    "v1": 0.0,
    "u2": 0.5,
    "v2": 0.25
  }
}
```

### Automatic Atlas Building

The atlas is automatically rebuilt when:

1. **First run**: No atlas exists
2. **Missing atlas**: `atlas.png` or `atlas.json` not found
3. **Manual rebuild**: Using the atlas packer tool

## Custom Skin Packs

### Creating a Skin Pack

1. **Create directory**: `skins/my_pack/`
2. **Add textures**: Place your PNG files
3. **Copy all blocks**: Include textures for all block types
4. **Build atlas**: Run atlas packer or restart game

### Skin Pack Structure

```
skins/
├── default/              # Default skin pack
│   ├── grass.png
│   ├── dirt.png
│   └── ...
└── my_pack/              # Custom skin pack
    ├── grass.png
    ├── dirt.png
    ├── stone.png
    └── README.txt        # Optional: Pack info
```

### Switching Skin Packs

Currently, the engine loads from `skins/default/`. To switch packs:

1. **Backup default**: Rename `skins/default/` to `skins/default_backup/`
2. **Activate pack**: Rename your pack to `skins/default/`
3. **Restart game**: Atlas will rebuild automatically

**Future**: In-game skin pack selector will be added.

### Sharing Skin Packs

To share your skin pack:

1. **Create ZIP**: Compress your skin pack directory
2. **Include README**: Add description, credits, license
3. **Share**: Upload to community sites or GitHub

Example README:

```
# Medieval Texture Pack
Version: 1.0
Author: YourName
License: CC-BY-4.0

A medieval-themed texture pack with stone and wood aesthetics.

## Installation
1. Extract to PoorCraft/skins/
2. Rename to 'default' or use skin selector
3. Restart game
```

## Atlas Packer Tool

### Manual Atlas Building

Use the atlas packer tool to manually rebuild the atlas:

```powershell
# Pack textures from skins/default/
./gradlew :tools:atlas-packer:run --args="skins/default"

# Pack to different output directory
./gradlew :tools:atlas-packer:run --args="skins/default output/path"
```

### When to Rebuild

Rebuild the atlas when:

- Adding new textures
- Modifying existing textures
- Switching skin packs
- Atlas appears corrupted

### Atlas Packer Output

```
Atlas Packer - PoorCraft Refactor
Input: skins/default
Output: skins/default
Loaded 10 textures
Atlas size: 64x64 (4 textures per row)
Wrote atlas image: skins/default/atlas.png
Wrote UV map: skins/default/atlas.json
Atlas packing complete!
```

### Verifying Atlas

Check that the atlas was built correctly:

1. **Open atlas.png**: Should show all textures in a grid
2. **Check atlas.json**: Should contain UV coords for all textures
3. **Test in-game**: Textures should display correctly

## Advanced Techniques

### Animated Textures (Future)

Animated textures are not yet supported, but planned:

```
water_0.png    # Frame 0
water_1.png    # Frame 1
water_2.png    # Frame 2
water_3.png    # Frame 3
```

### Normal Maps (Future)

Normal maps for lighting detail are planned:

```
stone.png          # Diffuse texture
stone_normal.png   # Normal map
```

### Emissive Textures (Future)

Self-illuminating textures are planned:

```
glowstone.png          # Base texture
glowstone_emit.png     # Emission map
```

## Troubleshooting

### Textures Not Showing

**Problem**: Blocks appear white or magenta

**Solutions**:
1. Check that PNG files exist in `skins/default/`
2. Verify file names match block names exactly (lowercase)
3. Rebuild atlas: `./gradlew :tools:atlas-packer:run --args="skins/default"`
4. Check logs for texture loading errors
5. Ensure textures are square (16×16 or 32×32)

### Atlas Build Fails

**Problem**: Atlas packer reports errors

**Solutions**:
1. Verify all PNG files are valid (open in image editor)
2. Check that all textures are the same size
3. Ensure PNG format is RGBA (not indexed color)
4. Remove any non-PNG files from skins directory

### Textures Look Blurry

**Problem**: Textures appear blurred or smoothed

**Solutions**:
1. Check texture filtering in config.json
2. Ensure textures are exact size (16×16 or 32×32)
3. Don't upscale small textures in image editor
4. Use "nearest neighbor" when resizing

### Seams Visible Between Blocks

**Problem**: Lines appear between blocks

**Solutions**:
1. Ensure textures tile seamlessly
2. Check for 1-pixel borders in textures
3. Verify atlas packing didn't add padding
4. Test textures in 2×2 grid before using

### Transparency Not Working

**Problem**: Transparent blocks appear opaque

**Solutions**:
1. Verify PNG has alpha channel (RGBA, not RGB)
2. Check that block is registered as non-opaque in code
3. Ensure alpha values are correct (0-255)
4. Test PNG in image editor to verify transparency

### Wrong Colors

**Problem**: Colors don't match original textures

**Solutions**:
1. Check color space (use sRGB)
2. Verify PNG bit depth (use 32-bit RGBA)
3. Disable color management in image editor
4. Check monitor calibration

## Best Practices

### Performance

- **Use 16×16**: Better performance than 32×32
- **Optimize PNGs**: Use tools like pngcrush or optipng
- **Limit colors**: Fewer colors = smaller file size
- **Avoid transparency**: Use only when necessary

### Consistency

- **Match style**: Keep all textures in same art style
- **Same size**: All textures must be same dimensions
- **Color palette**: Use consistent color scheme
- **Lighting**: Design for neutral lighting

### Organization

- **Name clearly**: Use descriptive, consistent names
- **Document**: Include README with your pack
- **Version control**: Track changes with git
- **Backup**: Keep copies of original textures

## Resources

### Learning Pixel Art

- **Lospec**: Tutorials and color palettes (lospec.com)
- **Pixel Joint**: Community and tutorials (pixeljoint.com)
- **MortMort**: YouTube tutorials

### Texture References

- **Minecraft textures**: Classic voxel game reference
- **OpenGameArt**: Free game assets
- **itch.io**: Indie game assets

### Tools

- **Aseprite**: aseprite.org
- **GIMP**: gimp.org
- **Piskel**: piskelapp.com
- **Lospec Palette List**: lospec.com/palette-list

## Example Workflow

### Creating a Complete Texture Pack

1. **Plan your theme**: Medieval, sci-fi, cartoon, etc.
2. **Choose palette**: 16-32 colors that work together
3. **Create base textures**: Start with most common blocks
4. **Test in-game**: Build atlas and check in game
5. **Iterate**: Refine based on how they look together
6. **Complete set**: Create all remaining textures
7. **Polish**: Ensure consistency and quality
8. **Document**: Write README with credits
9. **Share**: Package and upload

### Quick Edit Workflow

1. **Edit texture**: Modify PNG in image editor
2. **Save**: Overwrite existing file
3. **Rebuild atlas**: `./gradlew :tools:atlas-packer:run --args="skins/default"`
4. **Restart game**: See changes immediately

### Testing Workflow

1. **Create test world**: Generate new world
2. **Place blocks**: Build structure with all block types
3. **Check appearance**: Verify textures look correct
4. **Test lighting**: Check in different light conditions
5. **Test tiling**: Look for seams or patterns
6. **Iterate**: Make adjustments as needed

## Future Features

Planned texture system enhancements:

- [ ] In-game skin pack selector
- [ ] Animated textures
- [ ] Normal mapping
- [ ] Emissive textures
- [ ] Per-face textures (different texture per block face)
- [ ] Texture overlays
- [ ] Biome-specific textures
- [ ] Seasonal texture variants
- [ ] Hot reload without restart

## Support

- **Issues**: Report texture bugs on GitHub
- **Showcase**: Share your texture packs on Discord
- **Help**: Ask questions in community forums
