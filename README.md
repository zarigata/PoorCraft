# PoorCraft

A free, open-source Minecraft clone that empowers players to create, modify and share their ideas without any financial or technical barriers. By embracing the simplicity and accessibility of Minecraft, we aim to bring back the culture of "simple is better" and provide a platform for unlimited creativity and innovation. Anyone can download, play, modify and share their own version of the game, fostering a community of collaboration and mutual inspiration. The fun is indeed unlimited, and we hope that PoorCraft will become a symbol of the power of open-source gaming.

## Features

- **4 Biomes**: Desert, Snow, Jungle, Plains with unique terrain generation ✅ **IMPLEMENTED**
- **Infinite World**: Chunk-based world with seed support and dynamic loading/unloading ✅ **IMPLEMENTED**
- **Procedural Terrain**: Simplex noise-based height maps with biome-specific features ✅ **IMPLEMENTED**
- **Biome Features**: Trees in plains/jungle, cacti in desert, snow layers in snow biome ✅ **IMPLEMENTED**
- **Voxel Rendering**: OpenGL-based chunk renderer with greedy meshing optimization ✅ **IMPLEMENTED**
- **Frustum Culling**: Only renders chunks visible in camera view ✅ **IMPLEMENTED**
- **Lighting System**: Ambient + directional lighting with normal-based shading ✅ **IMPLEMENTED**
- **Texture Atlas**: 16x16 block textures combined into efficient atlas ✅ **IMPLEMENTED**
- **UI System**: Main menu, settings, world creation, in-game HUD ✅ **IMPLEMENTED**
- **Configuration**: JSON-based settings with in-game editor ✅ **IMPLEMENTED**
- **Procedural Textures**: Up to 256 unique block skin variations ⏳ **COMING SOON** (Skin Generator mod)
- **AI NPCs**: Conversational NPCs powered by LLMs ⏳ **COMING SOON**
- **Multiplayer**: Client-server architecture ⏳ **COMING SOON**
- **Modding**: Python-based modding system ⏳ **COMING SOON**

## Requirements

- **Java**: JDK 17 or higher
- **Python**: 3.8 or higher
- **Maven**: 3.6 or higher
- **GPU**: OpenGL 3.3+ compatible graphics card

## Building

```bash
# Clone the repository
git clone <repo-url>
cd PoorCraft

# Build Java project
mvn clean package

# Set up Python environment (Windows PowerShell/CMD)
python -m venv venv
venv\Scripts\activate
pip install -r python/requirements.txt
```

## Running

```bash
# Run the game
java -jar target/poorcraft-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

## World Generation

PoorCraft uses a sophisticated procedural generation system:

- **Seed-based**: Every world has a seed (configurable in settings) for reproducible terrain
- **Biome System**: Temperature and humidity noise determine biome distribution
- **Height Maps**: Multi-octave Simplex noise creates natural-looking terrain
- **Dynamic Loading**: Chunks load/unload automatically based on player position
- **Features**: Biome-specific structures (trees, cacti, snow) generate deterministically

Configure world generation in `config/settings.json` under the `world` section:
- `seed`: World seed (0 for random)
- `chunkLoadDistance`: How many chunks to load around player (default: 8)
- `chunkUnloadDistance`: Distance before unloading chunks (default: 10)
- `generateStructures`: Enable/disable feature generation (default: true)

## Rendering System

PoorCraft uses a modern OpenGL 3.3+ rendering pipeline:

**Greedy Meshing:**
- Optimized mesh generation that merges adjacent same-type faces into larger quads
- Reduces vertex count by 50-90% compared to naive per-block rendering
- Dramatically improves performance for large view distances

**Frustum Culling:**
- Only renders chunks visible in the camera's view frustum
- Uses Gribb-Hartmann plane extraction for efficient AABB testing
- Typically culls 50-80% of loaded chunks depending on FOV

**Lighting:**
- Ambient lighting provides base illumination (configurable color and strength)
- Directional lighting simulates sunlight with normal-based diffuse shading
- Per-vertex normals enable smooth lighting across block faces

**Texture Atlas:**
- All block textures combined into a single 256×256 atlas
- Reduces texture binding overhead (single bind per frame)
- Supports up to 256 unique 16×16 textures
- UV coordinates automatically calculated per block type and face

**Performance:**
- Efficient VAO/VBO/EBO management with automatic cleanup
- Mesh regeneration only when blocks change (dirty flag system)
- GPU resources freed immediately when chunks unload

## User Interface

PoorCraft features a complete UI system built on a custom lightweight LWJGL framework:

**Main Menu:**
- Singleplayer: Create and play worlds
- Multiplayer: Coming soon (grayed out)
- Settings: Configure all game options
- Quit: Exit game

**Settings Menu:**
- **Graphics Tab**: Resolution, VSync, FOV, render distance, max FPS
- **Audio Tab**: Master, music, and SFX volume controls
- **Controls Tab**: Mouse sensitivity, invert Y, keybind customization
- **AI Tab**: Enable AI NPCs, select provider (Ollama/Gemini/OpenRouter/OpenAI)
- Changes preview in real-time, apply or cancel

**World Creation:**
- World name input
- Seed specification (or random generation)
- Game mode selection (Creative/Survival)
- Structure generation toggle

**In-Game HUD:**
- Crosshair (center screen)
- Hotbar (9 slots, bottom center)
- F3 Debug overlay:
  - FPS counter
  - Player position and chunk coordinates
  - Current biome
  - Loaded/rendered chunk counts
  - Facing direction

**Pause Menu (ESC):**
- Resume game
- Access settings
- Save and quit to main menu

**Technical Details:**
- Custom immediate-mode UI renderer with orthographic projection
- STB TrueType font rendering with bitmap atlas
- Component-based architecture (buttons, labels, text fields, sliders, checkboxes, dropdowns)
- State machine for screen management
- Input event system with callbacks

## Project Structure

- **src/main/java/** - Java game engine (LWJGL3, OpenGL rendering, core game logic)
  - **src/main/java/com/poorcraft/world/** - World system (chunks, blocks, generation)
  - **src/main/java/com/poorcraft/render/** - Rendering system (shaders, textures, chunk rendering)
  - **src/main/java/com/poorcraft/config/** - Configuration management (Settings, ConfigManager)
  - **src/main/java/com/poorcraft/ui/** - UI system (screens, components, rendering)
- **src/main/resources/** - Game assets (textures, shaders, configs)
  - **src/main/resources/shaders/** - GLSL vertex and fragment shaders (including UI shaders)
  - **src/main/resources/textures/blocks/** - 16×16 block textures
  - **src/main/resources/fonts/** - TrueType fonts for UI text
  - **src/main/resources/config/** - Default configuration files
- **python/** - Python modding framework and API
- **mods/** - Official and user-created mods
  - **mods/skin_generator/** - Procedural texture generation mod
  - **mods/ai_npc/** - AI-powered NPC mod
- **docs/** - Documentation and modding guides

## Modding

PoorCraft features an easy-to-use Python-based modding system. Mods are placed in the `mods/` directory and can hook into game events, modify world generation, add new entities, and more.

See the two official mods as examples:
- **Skin Generator** (`mods/skin_generator/`) - Generates procedural block textures with biome-specific palettes
- **AI NPC** (`mods/ai_npc/`) - Adds conversational NPCs powered by various LLM providers

For detailed modding documentation, see `docs/MODDING_GUIDE.md` (coming soon).

## AI Configuration

The AI NPC mod supports multiple LLM providers. Configure your preferred provider in `mods/ai_npc/mod.json`:

### Ollama (Local, Free)
1. Install Ollama from https://ollama.ai
2. Run `ollama pull llama2` to download a model
3. Set `"ai_provider": "ollama"` in mod.json
4. Ensure `"ollama_url": "http://localhost:11434"` is correct

### Cloud Providers (Gemini, OpenRouter, OpenAI)
1. Obtain an API key from your chosen provider
2. Add the API key to the `"api_keys"` section in mod.json
3. Set `"ai_provider"` to `"gemini"`, `"openrouter"`, or `"openai"`
4. Configure the `"model"` field with your desired model name

**Note**: Only the server host can enable and configure the AI NPC mod (`"server_only": true`).

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
