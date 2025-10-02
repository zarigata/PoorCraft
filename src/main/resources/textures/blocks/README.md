# Block Textures

All block textures must be **16x16 pixels** in PNG format with RGBA channels.

## Naming Convention

For blocks with the same texture on all faces:
- `<blockname>.png` (e.g., `stone.png`, `dirt.png`, `sand.png`)

For blocks with different textures per face:
- `<blockname>_top.png` - Top face
- `<blockname>_bottom.png` - Bottom face
- `<blockname>_side.png` - Side faces (north, south, east, west)

## Required Textures

**Basic Blocks:**
- `dirt.png` - Dirt block (all faces)
- `stone.png` - Stone block (all faces)
- `bedrock.png` - Bedrock block (all faces)

**Plains Biome:**
- `grass_top.png` - Grass block top
- `grass_side.png` - Grass block sides
- (grass bottom uses dirt.png)

**Desert Biome:**
- `sand.png` - Sand block (all faces)
- `sandstone.png` - Sandstone block (all faces)
- `cactus_top.png` - Cactus top
- `cactus_side.png` - Cactus sides

**Snow Biome:**
- `snow_block.png` - Snow block (all faces)
- `ice.png` - Ice block (all faces, semi-transparent)
- `snow_layer.png` - Snow layer (decorative)

**Jungle Biome:**
- `jungle_grass_top.png` - Jungle grass top
- `jungle_grass_side.png` - Jungle grass sides
- `jungle_dirt.png` - Jungle dirt (all faces)

**Trees:**
- `wood_top.png` - Wood log top/bottom
- `wood_side.png` - Wood log sides
- `leaves.png` - Leaves (all faces, semi-transparent)

## Placeholder Textures

For initial development, create simple colored squares:
- Use solid colors or simple patterns
- Ensure proper alpha channel for transparent blocks (ice, leaves, snow layer)
- Missing textures will default to a magenta/black checkerboard pattern

## Future: Procedural Generation

The Skin Generator mod (Phase: Official Mods) will generate up to 256 unique variations of these textures at runtime. These base textures serve as templates.
