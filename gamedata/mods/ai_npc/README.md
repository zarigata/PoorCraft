# AI NPC System Mod

> **⚠️ DEPRECATION NOTICE:**
> This README documents a prior Python implementation. The current mod is a Lua placeholder; features described here are not implemented yet.

Conversational NPCs for PoorCraft powered by multiple LLM providers. Spawns buddies when players join and keeps the chat flowing using a background worker thread.

## Features
- **Multi-provider support**: Ollama (local), Gemini, OpenRouter, OpenAI.
- **Auto detection**: Checks which provider is reachable/configured on startup.
- **Personality system**: Friendly, grumpy, wise, mysterious, cheerful, merchant, guard.
- **Context-aware chat**: Remembers recent messages for coherent conversations.
- **Async requests**: Worker thread handles API calls without blocking the game.
- **Task parsing (stub)**: Simple keyword detection for follow/stay/greet commands.

## Configuration

Edit `mods/ai_npc/config.json` to customize behavior:

- `enabled`: Master toggle (default `true`).
- `ai_provider`: Preferred provider (`ollama`, `gemini`, `openrouter`, `openai`).
- Provider sections (`ollama`, `gemini`, `openrouter`, `openai`): supply URL/API key/model.
- `npc_settings`:
  - `max_npcs`: Cap on active NPCs (default 10).
  - `spawn_radius`: Distance from player for spawn (default 10 blocks).
  - `response_timeout`: API call timeout in seconds (default 30).
  - `conversation_history_limit`: Messages retained per NPC (default 5).
  - `personalities`: List of allowed personality labels.
  - `auto_spawn_on_join`: Spawn NPC automatically when player joins (default `true`).
  - `npcs_per_player`: NPCs per player (default 1).
- `task_commands`: Placeholder for future behavior toggles.

## Lifecycle
1. `init()` loads config, detects provider availability, and starts the chat worker.
2. Player joins trigger `create_npc()` if auto-spawn is enabled.
3. `handle_chat()` queues messages; worker thread fetches LLM responses.
4. Responses are relayed to the world via `npc_say()`.
5. Player leaves despawn associated NPCs.

## Provider Notes
- **Ollama**: No API key required, just ensure the URL is correct and a model is pulled (`ollama pull llama2`).
- **Gemini/OpenRouter/OpenAI**: Requires valid API key and model name. Set them in `config.json`.

## Integrating with Gameplay
- Use `handle_chat(npc_id, player_id, message)` to send new player messages from the chat system.
- `ACTIVE_NPCS` map holds NPC state for future movement/AI features.
- `parse_task_command()` returns simple directives (`follow`, `stay`, `greet`) for future automation.

## Troubleshooting
- **No NPCs spawning**: Check `enabled`, `auto_spawn_on_join`, and `max_npcs` in config.
- **No provider**: Ensure at least one provider is available (see console logs).
- **Long response times**: Increase `response_timeout` or switch to faster provider.
- **Too chatty**: Lower `conversation_history_limit` or adjust personality list.

## Fun Tidbits
- Worker thread is named `ai_npc_worker` because naming threads helps debugging.
- There’s a log line confessing “my brain just froze” to honor those glitchy beta villagers.
- We still hum classic Minecraft music when testing this. Old habits die hard.
