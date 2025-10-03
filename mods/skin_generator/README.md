# Procedural Skin Generator Mod

Generates up to 256 unique 16×16 block textures with biome-specific palettes and noise-based variation. Perfect for making PoorCraft feel a little less like a repeating wallpaper.

## Features
- **Procedural Generation**: Perlin-style noise, random palettes, and block-aware patterns.
- **Biome Palettes**: Desert, Snow, Jungle, and Plains color sets keep blocks on-theme.
- **Multiple Variations**: Each block can spawn several variants (`stone`, `stone_1`, etc.).
- **Pattern System**: Speckles on stone, veins on leaves, blade gradients on grass, and more.
- **Fully Configurable**: Tweak palettes, noise scale, variation, and pattern toggles via JSON.

## Configuration

Edit `mods/skin_generator/config.json` to adjust generation:

- `enabled`: Master on/off switch (default: `true`).
- `max_textures`: Cap on generated textures (1–256, default: 256).
- `texture_size`: Texture dimension (currently forced to 16 for atlas compatibility).
- `randomize_on_startup`: Regenerate every boot (default: `true`).
- `variations_per_block`: Variants per base texture (default: 3).
- `noise_scale`: Higher = smoother patterns, lower = more chaos (default: 4.0).
- `color_variation`: Brightness/hue wobble (0.0–0.5, default: 0.15).
- `biome_palettes`: Hex colors for each biome group.
- `block_patterns`: Enable/disable pattern layers per block type.

Example snippet:
```json
{
  "variations_per_block": 2,
  "color_variation": 0.1,
  "block_patterns": {
    "stone": {"speckles": false, "cracks": true}
  }
}
```

## How It Works
1. `init()` loads config and kicks off `generate_textures()` if enabled.
2. For each base block texture, we create several variations using:
   - Palette-driven base colors.
   - Smooth noise for shading and highlights.
   - Block-specific pattern overlays (cracks, blades, ripples, etc.).
3. Each texture is sent to Java using `add_procedural_texture()`.
4. `ChunkRenderer` grabs the textures and bakes them into its atlas before rendering starts.

## Texture Naming
- The first variant keeps the original name: `stone`.
- Additional variants are suffixed: `stone_1`, `stone_2`, etc.
- Names must match what the renderer expects (`TextureAtlas.getUVsForFace()`).

## Troubleshooting
- **No textures?** Check console logs, make sure Pillow/numpy are installed, verify JSON syntax.
- **Atlas overflow?** Keep `max_textures` ≤ 256 (engine limit).
- **Weird colors?** Lower `color_variation` or adjust palettes.
- **Performance hiccups?** Reduce `variations_per_block` or disable heavy patterns like cracks.

## Fun Facts
- Uses numpy’s RNG so every boot feels like those wild beta days.
- There’s a hardcoded “I don’t know what is going on here but it’s working” log, because nostalgia.
- All patterns were tuned while humming old Minecraft menu music. Totally not required, but recommended.
