# Official PoorCraft Mods

PoorCraft ships with two showcase mods demonstrating the Python modding workflow: a procedural block texture generator and an AI companion system. Both live in the `mods/` directory and can be toggled or customized independently.

## Procedural Block Texture Generator (`mods/block_texture_generator/`)

- **Purpose**: Generate up to 256 procedural block textures using biome-specific color palettes and pattern rules.
- **Key Features**:
  - Perlin-style noise, palette-driven shading, and pattern overlays (cracks, blades, grain).
  - Texture variations stay compatible with `TextureAtlas.getUVsForFace()` naming conventions.
  - Uses Pillow and numpy; results are pushed to Java via `add_procedural_texture()` before the renderer boots.
- **Configuration**: `mods/block_texture_generator/config.json` allows tuning of texture counts, variation intensity, palettes, and pattern toggles.
- **Docs**: See `mods/block_texture_generator/README.md` for detailed usage and troubleshooting tips and notes on block texture generation.

## AI NPC System (`mods/ai_npc/`)

- **Purpose**: Spawn conversational NPC companions that respond using LLM providers.
- **Key Features**:
  - Provider detection for Ollama, Gemini, OpenRouter, and OpenAI with graceful fallback.
  - Personality-aware prompts, conversation memoization, and async worker thread for API calls.
  - Hooks into player join/leave events to manage NPC lifecycle.
- **Configuration**: `mods/ai_npc/config.json` defines provider credentials, spawn behavior, response timeouts, and personality list.
- **Docs**: `mods/ai_npc/README.md` covers setup, provider notes, and integration guidelines.

## Managing Mods

- **Enable/Disable**: Set `enabled` to `false` in a mod's `config.json` or `mod.json`.
- **Customization**: Adjust JSON configs or extend the Python modules for deeper changes.
- **Learning**: These mods double as reference implementations for procedural content and external API integration through the PoorCraft ModAPI.
