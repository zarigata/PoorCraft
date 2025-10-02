# PoorCraft

A simple yet functional Minecraft clone with AI NPCs and procedural texture generation.

## Features

- **4 Biomes**: Desert, Snow, Jungle, Plains with unique terrain generation
- **Procedural Textures**: Up to 256 unique 16x16 block skins generated at startup
- **AI NPCs**: Conversational NPCs powered by Ollama, Gemini, OpenRouter, or OpenAI
- **Multiplayer**: Client-server architecture with chunk-based world
- **Modding**: Easy Python-based modding system with comprehensive API

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

## Project Structure

- **src/main/java/** - Java game engine (LWJGL3, OpenGL rendering, core game logic)
- **python/** - Python modding framework and API
- **mods/** - Official and user-created mods
  - **mods/skin_generator/** - Procedural texture generation mod
  - **mods/ai_npc/** - AI-powered NPC mod
- **src/main/resources/** - Game assets (textures, shaders, configs)
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
