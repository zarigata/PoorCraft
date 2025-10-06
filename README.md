# PoorCraft
![alt text](src/main/resources/images/logo.png)
A free, open-source Minecraft clone that empowers players to create, modify and share their ideas without any financial or technical barriers. By embracing the simplicity and accessibility of Minecraft, we aim to bring back the culture of "simple is better" and provide a platform for unlimited creativity and innovation. Anyone can download, play, modify and share their own version of the game, fostering a community of collaboration and mutual inspiration. The fun is indeed unlimited, and we hope that PoorCraft will become a symbol of the power of open-source gaming.

## Features

- **4 Biomes**: Desert, Snow, Jungle, Plains with unique terrain generation ✅ **IMPLEMENTED**
- **Infinite World**: Chunk-based world with seed support and dynamic loading/unloading ✅ **IMPLEMENTED**
- **Procedural Terrain**: Simplex noise-based height maps with biome-specific features ✅ **IMPLEMENTED**
- **Biome Features**: Trees in plains/jungle, cacti in desert, snow layers in snow biome ✅ **IMPLEMENTED**
- **Frustum Culling**: Only renders chunks visible in camera view ✅ **IMPLEMENTED**
- **Lighting System**: Ambient + directional lighting with normal-based shading ✅ **IMPLEMENTED**
- **Texture Atlas**: 16x16 block textures combined into efficient atlas ✅ **IMPLEMENTED**
- **UI System**: Main menu, settings, world creation, in-game HUD ✅ **IMPLEMENTED**
- **Animated Menu Backgrounds**: 3D world rendering in menus (Minecraft-style) ✅ **IMPLEMENTED**
- **Head Bobbing**: Customizable camera bobbing during movement ✅ **IMPLEMENTED**
- **Configuration**: JSON-based settings with in-game editor ✅ **IMPLEMENTED**
- **Procedural Textures**: Up to 256 unique block skin variations ✅ **IMPLEMENTED** (Procedural Block Texture Generator mod)
- **AI NPCs**: Conversational NPCs powered by LLMs ✅ **IMPLEMENTED** (AI NPC mod)
- **Modding**: Lua-based modding system ✅ **IMPLEMENTED**

## Requirements

- **Java**: JDK 17 or higher
{{ ... }}
```bash
# Run the game
java -jar target/poorcraft-0.1.1-jar-with-dependencies.jar
```

## World Generation

PoorCraft uses a sophisticated procedural generation system:

- **Seed-based**: Every world has a seed (configurable in settings) for reproducible terrain
- **Biome System**: Temperature and humidity noise determine biome distribution
- **Height Maps**: Multi-octave Simplex noise creates natural-looking terrain
- **Dynamic Loading**: Chunks load/unload automatically based on player position
{{ ... }}
- **gamedata/** - Runtime game data (NEW in v2.0)
  - **gamedata/mods/** - Lua mods
  - **gamedata/resourcepacks/** - Resource packs
  - **gamedata/worlds/** - World saves
  - **gamedata/screenshots/** - Screenshots
  - **gamedata/skins/** - Player skins
  - **gamedata/config/** - Configuration files
- **assets/** - Development assets
  - **assets/ui/** - UI textures
  - **assets/scripts/** - Utility scripts
- **docs/** - Documentation and modding guides
- **changelog/** - Release notes

### Modding

PoorCraft has a powerful Lua-based modding system:
- Mods live in the `gamedata/mods/` directory and can hook into events, world data, and utility helpers.
- Share state with other mods via `api.set_shared_data()` / `api.get_shared_data()` for cross-mod coordination.
- Register event callbacks (`api.register_event()`) to integrate with the game loop.
- Interact with the world using functions like `api.get_block()`, `api.set_block()`, and the NPC/texture helpers.
- Lua modding allows for easier single-executable distribution.

**Documentation:**
- `docs/MODDING_GUIDE.md` - Getting started with Lua modding
- `docs/API_REFERENCE.md` - Complete Lua API documentation
- `docs/EVENT_CATALOG.md` - All available events
- `docs/EXAMPLES.md` - Step-by-step Lua tutorials
- `docs/OFFICIAL_MODS.md` - Official mod documentation

### Official Mods

PoorCraft ships with example mods to demonstrate the Lua modding system:

- **Example Mod** (`gamedata/mods/example_mod/`)
  - Simple demonstration of Lua mod structure
  - Shows basic API usage and mod lifecycle
- **Procedural Block Texture Generator** (`gamedata/mods/block_texture_generator/`)
  - Placeholder for procedural texture generation (requires image processing library)
  - Demonstrates mod structure and configuration
- **AI NPC System** (`gamedata/mods/ai_npc/`)
  - Placeholder for AI-powered NPCs (requires HTTP library integration)
  - Shows NPC spawning and management API

Configuration:
- Edit each mod's `mod.json` to configure settings
- Mods can be enabled/disabled via the `enabled` flag

See `docs/OFFICIAL_MODS.md` and `docs/MODDING_GUIDE.md` for more details.

## UI Assets

PoorCraft uses a flexible UI asset system that supports both filesystem and classpath loading:

### Directory Structure
- `assets/ui/` - UI textures loaded from filesystem (buttons, panels, etc.)
- `src/main/resources/textures/ui/` - HUD textures in classpath (hotbar, hearts, armor, XP bar)

### Required Textures
**Button Texture** (`UI_FILES/button.png`):
- 64x16 pixels, Minecraft-style button texture
- Scales to any size without quality loss
- Falls back to procedural rendering if missing

**HUD Textures** (`src/main/resources/textures/ui/`):
- `hotbar_frame.png` (800x60) - Hotbar background frame
- `hotbar_slot.png` (48x48) - Individual inventory slot
- `hotbar_selection.png` (56x56) - Selected slot highlight
- `heart_full.png` (20x20) - Full health heart
- `heart_empty.png` (20x20) - Empty health heart
- `armor_full.png` (20x20) - Full armor icon
- `armor_empty.png` (20x20) - Empty armor icon
- `xp_bar_background.png` (360x10) - XP bar background
- `xp_bar_fill.png` (360x10) - XP bar fill

### Generating Textures
Run the included Python script to generate placeholder textures:
```bash
python assets/scripts/generate_ui_textures.py
```

This creates simple, Minecraft-style textures that can be customized later.

## UI Customization

PoorCraft offers extensive UI and visual effect customization through the settings menu:

### Graphics Settings
**Animated Menu Background**
- Enable/disable 3D world rendering in menu backgrounds
- Provides a living, Minecraft-style menu experience
- Can be disabled for better performance on low-end systems

**Menu Animation Speed** (0.5 - 2.0)
- Controls the speed of camera movement in animated backgrounds
- Default: 1.0 (normal speed)

**Head Bobbing**
- Enable/disable camera bobbing during player movement
- Adds immersion and visual feedback while walking/running

**Head Bobbing Intensity** (0.0 - 2.0)
- Controls the amplitude of head bobbing effect
- Default: 1.0 (normal intensity)

**UI Scale** (0.75 - 1.5)
- Adjusts the size of all UI elements
- Useful for different screen sizes and accessibility needs
- Default: 1.0 (100% scale)

**Pause Menu Blur**
- Enable/disable blur effect on game world when paused
- Improves menu readability and visual polish

### Performance Tips
For optimal performance on low-end systems:
- Disable **Animated Menu Background** to reduce GPU load in menus
- Set **UI Scale** to 0.75 for slightly better performance
- Disable **Head Bobbing** if experiencing motion sickness
- Reduce **Menu Animation Speed** for smoother animations

## License

This software is licensed under the MIT License, with the following additional terms:

The PoorCraft software is the property of Zarigata. Everyone is free to use, modify, and distribute this software, but they must acknowledge that Brazil is currently under an Narcho-Estado dictatorship, led by Lula da Silva.

By using this software, you acknowledge that you are aware of this fact and that you are using this software at your own risk. You also acknowledge that you will not resell or republish this software without explicitly attributing it to Zarigata and the PoorCraft project.

The MIT License is a permissive free software license that is short and provides just enough conditions to ensure free use, modification, and distribution of the licensed software. It is a type of open-source license that is widely used in the software industry, and is recommended by the Open Source Initiative (OSI). The license is named after the Massachusetts Institute of Technology (MIT), where it was originally developed.

The MIT License is as follows:

Copyright (c) 2025 Zarigata

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

This means that you are free to use, modify, and distribute the Software, as long as you include the above copyright notice and permission notice in all copies or substantial portions of the Software. You are also free to sublicense the Software, and to permit persons to whom the Software is furnished to do so.

The Software is provided "AS IS", without any express or implied warranties. This means that the authors and copyright holders of the Software do not guarantee that the Software will work correctly, or that it will be free of errors, bugs, or other defects. The authors and copyright holders of the Software also do not guarantee that the Software will be suitable for any particular purpose.

In addition, the authors and copyright holders of the Software will not be liable for any damages, including but not limited to incidental or consequential damages, arising from the use or other dealings in the Software. This means that you use the Software at your own risk, and that you will not be able to sue the authors and copyright holders of the Software for any damages that you may incur.

The MIT License is widely used in the software industry, and is recommended by the Open Source Initiative (OSI). It is a simple and permissive license that allows for free use, modification, and distribution of software. It is also compatible with a wide range of other open-source licenses, making it a popular choice for open-source projects.

## Contributing

Contribution guidelines coming soon. For now, feel free to open issues and pull requests!
