# AI Companion Guide

The PoorCraft AI Companion is a built-in assistant that can reply to player chat, carry out simple tasks (follow, stop, gather), and surface world updates. This guide walks you through setup, configuration, and troubleshooting for each supported provider.

---

## 1. Overview

- **Where it runs:** Core game (no Lua mod required)
- **Default behaviour:** Companion automatically spawns when a world loads (configurable)
- **Supported providers:**
  - **Ollama** (local, default)
  - **Google Gemini**
  - **OpenRouter** (multi-model gateway)
- **Key features:**
  - Conversational replies addressed to the companion name (default "Companion")
  - Action parsing for "follow", "stop", and "gather <resource>" commands
  - Main-thread safe integration with UI and NPC manager

---

## 2. Requirements

| Requirement | Notes |
|-------------|-------|
| Java 17+    | Matches core game runtime |
| Internet access | Required for Gemini and OpenRouter |
| API keys | Needed for Gemini and OpenRouter; set via `settings.ai.apiKeys` |
| Local service | Ollama must be running (`ollama serve`) to use the default provider |

Provider-specific preparation:

1. **Ollama**
   - Install from [https://ollama.com](https://ollama.com)
   - Run `ollama serve`
   - Pull a compatible model (e.g. `ollama pull llama3.2`)

2. **Google Gemini**
   - Create an API key via the [Google AI Studio](https://aistudio.google.com/app/apikey)
   - Record the key for the configuration step

3. **OpenRouter**
   - Generate a key at [https://openrouter.ai/keys](https://openrouter.ai/keys)
   - Decide which upstream model to use (e.g. `openai/gpt-3.5-turbo`)

---

## 3. Enabling the Companion

1. Launch the game and open **Settings → AI Companion**.
2. Toggle **Enable AI Companion**.
3. Choose a provider:
   - `ollama` (default)
   - `gemini`
   - `openrouter`
4. Adjust optional fields:
   - Companion name & skin
   - Follow distance
   - Action cooldown (seconds)
   - Reasoning filter (strips "reasoning" blocks when enabled)
5. Confirm **Spawn on Start** is set according to your preference (default: enabled).
6. Save and return to the main menu, then load or create a world.

The companion will spawn near the player once the world initializes. If it does not, check the troubleshooting section below.

---

## 4. Configuration Reference

Settings are stored in the player configuration file (typically `gamedata/config/settings.json`). Key fields inside the `"ai"` block:

```json
"ai": {
  "aiEnabled": true,
  "aiProvider": "ollama",
  "companionName": "Companion",
  "companionSkin": "alex",
  "spawnOnStart": true,
  "followDistance": 3.0,
  "enableActions": true,
  "actionCooldownSeconds": 10,
  "maxGatherDistance": 20,
  "filterReasoning": true,
  "logReasoning": false,
  "apiKeys": {
    "ollama": "",
    "gemini": "<your-gemini-key>",
    "openrouter": "<your-openrouter-key>"
  },
  "models": {
    "ollama": "llama3.2",
    "gemini": "gemini-pro",
    "openrouter": "openai/gpt-3.5-turbo"
  }
}
```

> **Tip:** Leave unused provider keys blank. The manager will treat providers without credentials as unavailable and automatically fall back to the next option in its priority list (`ollama → openrouter → gemini`).

### Provider Notes

- **Ollama**
  - The `apiKeys` entry is optional.
  - Ensure the base URL (`http://localhost:11434`) is reachable.

- **Gemini**
  - An API key is required.
  - The default base URL is `https://generativelanguage.googleapis.com/v1beta`.

- **OpenRouter**
  - Requires an API key.
  - The guide sets helpful headers automatically, but you can supply additional headers via `ai.models`/`ai.apiKeys` if needed.

---

## 5. Using the Companion

### Directing chat to the companion
- Mention the companion name in chat (`Companion, follow me`)
- Prefix with `@companion` or start the message with `companion` (case-insensitive)

### Supported actions
| Command fragment | Result |
|------------------|--------|
| `follow` | Companion resumes following the player using the configured distance |
| `stop` | Companion stands by and stops following |
| `gather <resource>` | Starts a gather task for the resource (default quantity 5 unless a number is specified) |

### Tips
- Keep `filterReasoning` enabled for concise responses. Disable it if you need the full provider output for debugging.
- Enable `logReasoning` to print any "reasoning" text supplied by the provider to the console without showing it in chat.

---

## 6. Troubleshooting

| Symptom | Likely cause | Resolution |
|---------|--------------|------------|
| `No available providers` warning | Provider lacks credentials or is unreachable | Set API key in settings or start the Ollama service |
| Companion does not spawn | `spawnOnStart` disabled or NPC manager unavailable | Enable spawn or ensure world load completes successfully |
| Chat responses delayed | Action cooldown still active | Reduce `actionCooldownSeconds` or wait for cooldown to expire |
| Gemini/OpenRouter 4xx errors | Invalid key or rate limit | Verify key, check provider dashboard for quota status |
| UI freezes during response | Provider request still running | Requests are executed off the main thread; verify provider latency |

If issues persist:
1. Enable `logReasoning` to capture provider output in the console.
2. Check `logs/latest.log` for stack traces.
3. Temporarily switch to another provider to verify fallback behaviour.

---

## 7. Additional Resources

- [README.md](../README.md) – Feature overview
- [docs/MANUAL_TESTING_GUIDE.md](MANUAL_TESTING_GUIDE.md) – Includes AI Companion manual tests
- [docs/ARCHITECTURE.md](ARCHITECTURE.md) – High-level system design

Happy adventuring with your new companion!
