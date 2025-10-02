# PoorCraft

A simple yet functional Minecraft clone with AI NPCs and procedural texture generation.

## Features

- **4 Biomes**: Desert, Snow, Jungle, Plains with unique terrain generation ✅ **IMPLEMENTED**
- **Infinite World**: Chunk-based world with seed support and dynamic loading/unloading ✅ **IMPLEMENTED**
- **Procedural Terrain**: Simplex noise-based height maps with biome-specific features ✅ **IMPLEMENTED**
- **Biome Features**: Trees in plains/jungle, cacti in desert, snow layers in snow biome ✅ **IMPLEMENTED**
- **Voxel Rendering**: OpenGL-based chunk renderer with greedy meshing optimization ✅ **IMPLEMENTED**
- **Frustum Culling**: Only renders chunks visible in camera view ✅ **IMPLEMENTED**
- **Lighting System**: Ambient + directional lighting with normal-based shading ✅ **IMPLEMENTED**
- **Texture Atlas**: 16x16 block textures combined into efficient atlas ✅ **IMPLEMENTED**
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

## Project Structure

- **src/main/java/** - Java game engine (LWJGL3, OpenGL rendering, core game logic)
  - **src/main/java/com/poorcraft/world/** - World system (chunks, blocks, generation)
  - **src/main/java/com/poorcraft/render/** - Rendering system (shaders, textures, chunk rendering)
  - **src/main/java/com/poorcraft/config/** - Configuration management
- **src/main/resources/** - Game assets (textures, shaders, configs)
  - **src/main/resources/shaders/** - GLSL vertex and fragment shaders
  - **src/main/resources/textures/blocks/** - 16×16 block textures
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

To be determined (suggest MIT or similar open-source license).

## Contributing

Contribution guidelines coming soon. For now, feel free to open issues and pull requests!
