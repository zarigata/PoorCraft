"""Conversational AI NPC mod for PoorCraft.

Supports multiple LLM providers (Ollama, Gemini, OpenRouter, OpenAI), detects what is
available, and spawns buddy NPCs near joining players. NPC chatter happens in a worker
thread to avoid pausing the game loop. Future integrations (movement, tasks, voice)
can hook into the helper functions defined here."""

from __future__ import annotations

import json
import queue
import random
import threading
import time
from pathlib import Path
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

import requests

from poorcraft import (
    despawn_npc,
    get_mod_config,
    is_server,
    log,
    npc_say,
    on_player_join,
    on_player_leave,
    spawn_npc,
)


CONFIG_CACHE: Dict[str, object] = {}
ACTIVE_NPCS: Dict[int, "NPCState"] = {}
PLAYER_NPCS: Dict[int, List[int]] = {}

NPC_ID_COUNTER = 1
NPC_ID_LOCK = threading.Lock()

CHAT_QUEUE: "queue.Queue[ChatJob]" = queue.Queue()
WORKER_THREAD: Optional[threading.Thread] = None
WORKER_STOP = threading.Event()


@dataclass
class NPCState:
    npc_id: int
    player_id: int
    player_name: str
    npc_name: str
    personality: str
    position: Tuple[float, float, float]
    conversation_history: List[Dict[str, str]] = field(default_factory=list)


@dataclass
class ChatJob:
    npc_id: int
    player_id: int
    message: str


def init() -> None:
    """Initialize configuration, detect providers, and spin up worker thread."""

    if not is_server():
        log("[AI-NPC] Client context detected; mod disabled on client side.")
        return

    global CONFIG_CACHE
    CONFIG_CACHE = _load_config()

    if not CONFIG_CACHE.get("enabled", True):
        log("[AI-NPC] Mod disabled via config.json, skipping initialization.")
        return

    providers = detect_ai_providers()
    chosen = select_provider(CONFIG_CACHE, providers)

    if chosen is None:
        log("[AI-NPC] No AI provider available; NPC chatter disabled.")
    else:
        CONFIG_CACHE["active_provider"] = chosen
        log(f"[AI-NPC] Active provider: {chosen}")

    _start_worker_thread()

    if CONFIG_CACHE.get("npc_settings", {}).get("auto_spawn_on_join", True):
        log("[AI-NPC] Auto-spawn on player join enabled.")


def detect_ai_providers() -> Dict[str, bool]:
    """Return availability of each provider using config hints and health checks."""

    config = CONFIG_CACHE or _load_config()
    availability: Dict[str, bool] = {"ollama": False, "gemini": False, "openrouter": False, "openai": False}

    ollama_cfg = config.get("ollama", {})
    ollama_url = ollama_cfg.get("url") or "http://localhost:11434"
    try:
        resp = requests.get(f"{ollama_url}/api/tags", timeout=2)
        availability["ollama"] = resp.status_code == 200
    except Exception:
        availability["ollama"] = False

    gemini_key = (config.get("gemini") or {}).get("api_key")
    availability["gemini"] = bool(gemini_key)

    openrouter_key = (config.get("openrouter") or {}).get("api_key")
    availability["openrouter"] = bool(openrouter_key)

    openai_key = (config.get("openai") or {}).get("api_key")
    availability["openai"] = bool(openai_key)

    log(f"[AI-NPC] Provider availability: {availability}")
    return availability


def select_provider(config: Dict[str, object], availability: Dict[str, bool]) -> Optional[str]:
    """Pick the best provider based on config preference and availability."""

    preferred = (config.get("ai_provider") or "ollama").lower()
    order = [preferred, "ollama", "openai", "gemini", "openrouter"]

    for candidate in order:
        if availability.get(candidate):
            return candidate
    return None


def _start_worker_thread() -> None:
    """Start background worker thread if not already running."""

    global WORKER_THREAD
    if WORKER_THREAD and WORKER_THREAD.is_alive():
        return

    WORKER_STOP.clear()
    WORKER_THREAD = threading.Thread(target=_worker_loop, name="ai_npc_worker", daemon=True)
    WORKER_THREAD.start()
    log("[AI-NPC] Worker thread online.")


def _worker_loop() -> None:
    """Process chat jobs asynchronously to keep game loop smooth."""

    while not WORKER_STOP.is_set():
        try:
            job = CHAT_QUEUE.get(timeout=0.25)
        except queue.Empty:
            continue

        npc_state = ACTIVE_NPCS.get(job.npc_id)
        if not npc_state:
            continue

        provider_name = CONFIG_CACHE.get("active_provider")
        provider = create_ai_provider(provider_name)
        if provider is None:
            npc_say(job.npc_id, "Sorry, I'm feeling quiet right now.")
            continue

        response = provider.generate_response(npc_state, job.message)
        if response:
            npc_state.conversation_history.append({"player": job.message, "npc": response})
            _trim_history(npc_state)
            npc_say(job.npc_id, response)
        else:
            npc_say(job.npc_id, "Hmm, my brain just froze. Let's try again later.")

        CHAT_QUEUE.task_done()


def shutdown() -> None:
    """Stop worker thread and cleanup NPCs."""

    WORKER_STOP.set()
    if WORKER_THREAD and WORKER_THREAD.is_alive():
        WORKER_THREAD.join(timeout=1.0)

    for npc_id in list(ACTIVE_NPCS.keys()):
        despawn_npc(npc_id)
    ACTIVE_NPCS.clear()
    PLAYER_NPCS.clear()


@on_player_join
def _on_player_join(event) -> None:
    settings = CONFIG_CACHE.get("npc_settings", {})
    if not settings.get("auto_spawn_on_join", True):
        return

    max_per_player = max(1, int(settings.get("npcs_per_player", 1)))
    existing = PLAYER_NPCS.get(event.player_id, [])
    if len(existing) >= max_per_player:
        return

    npc_id = create_npc(event.player_id, event.username)
    if npc_id:
        PLAYER_NPCS.setdefault(event.player_id, []).append(npc_id)


@on_player_leave
def _on_player_leave(event) -> None:
    despawn_npc_for_player(event.player_id)


def create_npc(player_id: int, player_name: str) -> Optional[int]:
    """Spawn an NPC near the player with a personality and greeting."""

    global NPC_ID_COUNTER
    settings = CONFIG_CACHE.get("npc_settings", {})
    max_npcs = int(settings.get("max_npcs", 10))
    if len(ACTIVE_NPCS) >= max_npcs:
        log("[AI-NPC] Max NPC count reached, skipping spawn.")
        return None

    with NPC_ID_LOCK:
        npc_id = NPC_ID_COUNTER
        NPC_ID_COUNTER += 1

    spawn_radius = float(settings.get("spawn_radius", 10.0))
    position = _random_offset(spawn_radius)
    personality = _pick_personality(settings)
    npc_name = _generate_npc_name(personality)

    state = NPCState(
        npc_id=npc_id,
        player_id=player_id,
        player_name=player_name,
        npc_name=npc_name,
        personality=personality,
        position=position,
    )
    ACTIVE_NPCS[npc_id] = state

    spawn_npc(npc_id, npc_name, *position, personality)
    greeting = generate_greeting(personality, player_name)
    state.conversation_history.append({"player": "<join>", "npc": greeting})
    npc_say(npc_id, greeting)

    return npc_id


def despawn_npc_for_player(player_id: int) -> None:
    """Remove all NPCs associated with a player."""

    npc_ids = PLAYER_NPCS.pop(player_id, [])
    for npc_id in npc_ids:
        ACTIVE_NPCS.pop(npc_id, None)
        despawn_npc(npc_id)


def handle_chat(npc_id: int, player_id: int, message: str) -> None:
    """Queue a chat message for asynchronous processing."""

    if npc_id not in ACTIVE_NPCS:
        log(f"[AI-NPC] NPC {npc_id} not active, ignoring chat message.")
        return

    CHAT_QUEUE.put(ChatJob(npc_id=npc_id, player_id=player_id, message=message))


def _trim_history(state: NPCState) -> None:
    limit = int(CONFIG_CACHE.get("npc_settings", {}).get("conversation_history_limit", 5))
    state.conversation_history = state.conversation_history[-limit:]


def _random_offset(radius: float) -> Tuple[float, float, float]:
    angle = random.random() * 360
    distance = random.random() * radius
    x = distance * math_cos_deg(angle)
    z = distance * math_sin_deg(angle)
    y = 70  # Placeholder height until entity system handles actual terrain
    return x, y, z


def generate_greeting(personality: str, player_name: str) -> str:
    greetings = {
        "friendly": f"Hey {player_name}! Need a hand exploring?",
        "grumpy": f"{player_name}, try not to break anything, alright?",
        "wise": f"Greetings, {player_name}. Wisdom follows those who seek it.",
        "mysterious": f"The winds whispered you'd arrive, {player_name}.",
        "cheerful": f"Hi {player_name}! Isn't today block-tastic?",
        "merchant": f"Psst, {player_name}! I've got the best imaginary trades around.",
        "guard": f"Stay sharp, {player_name}. Trouble could be anywhere."
    }
    return greetings.get(personality, f"Hello there, {player_name}!")


def create_ai_provider(name: Optional[str]):
    if name is None:
        return None
    name = name.lower()
    if name == "ollama":
        return OllamaProvider(CONFIG_CACHE.get("ollama", {}))
    if name == "gemini":
        return GeminiProvider(CONFIG_CACHE.get("gemini", {}))
    if name == "openrouter":
        return OpenRouterProvider(CONFIG_CACHE.get("openrouter", {}))
    if name == "openai":
        return OpenAIProvider(CONFIG_CACHE.get("openai", {}))
    return None


class BaseAIProvider:
    def __init__(self, settings: Dict[str, object]):
        self.settings = settings or {}

    def _build_prompt(self, npc: NPCState, player_message: str) -> Dict[str, object]:
        history_limit = int(CONFIG_CACHE.get("npc_settings", {}).get("conversation_history_limit", 5))
        history = npc.conversation_history[-history_limit:]
        messages = [
            {"role": "system", "content": _personality_prompt(npc)},
        ]
        for entry in history:
            messages.append({"role": "user", "content": entry.get("player", "")})
            messages.append({"role": "assistant", "content": entry.get("npc", "")})
        messages.append({"role": "user", "content": player_message})
        return {"messages": messages}

    def generate_response(self, npc: NPCState, player_message: str) -> Optional[str]:
        raise NotImplementedError


class OllamaProvider(BaseAIProvider):
    def generate_response(self, npc: NPCState, player_message: str) -> Optional[str]:
        url = self.settings.get("url") or "http://localhost:11434"
        model = self.settings.get("model") or "llama2"
        timeout = int(CONFIG_CACHE.get("npc_settings", {}).get("response_timeout", 30))

        payload = {
            "model": model,
            "prompt": json.dumps(self._build_prompt(npc, player_message)),
            "stream": False,
        }

        try:
            resp = requests.post(f"{url}/api/generate", json=payload, timeout=timeout)
            resp.raise_for_status()
            data = resp.json()
            return data.get("response")
        except Exception as exc:
            log(f"[AI-NPC] Ollama request failed: {exc}")
            return None


class GeminiProvider(BaseAIProvider):
    def generate_response(self, npc: NPCState, player_message: str) -> Optional[str]:
        api_key = self.settings.get("api_key")
        if not api_key:
            return None

        model = self.settings.get("model") or "gemini-pro"
        timeout = int(CONFIG_CACHE.get("npc_settings", {}).get("response_timeout", 30))

        headers = {"Content-Type": "application/json"}
        params = {"key": api_key}
        payload = {
            "contents": [
                {"parts": [{"text": json.dumps(self._build_prompt(npc, player_message))}]}
            ]
        }

        try:
            resp = requests.post(
                f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
                headers=headers,
                params=params,
                json=payload,
                timeout=timeout,
            )
            resp.raise_for_status()
            data = resp.json()
            return data.get("candidates", [{}])[0].get("content", {}).get("parts", [{}])[0].get("text")
        except Exception as exc:
            log(f"[AI-NPC] Gemini request failed: {exc}")
            return None


class OpenRouterProvider(BaseAIProvider):
    def generate_response(self, npc: NPCState, player_message: str) -> Optional[str]:
        api_key = self.settings.get("api_key")
        if not api_key:
            return None

        model = self.settings.get("model") or "meta-llama/llama-2-7b-chat"
        timeout = int(CONFIG_CACHE.get("npc_settings", {}).get("response_timeout", 30))

        payload = self._build_prompt(npc, player_message)
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }

        try:
            resp = requests.post(
                "https://openrouter.ai/api/v1/chat/completions",
                json={"model": model, **payload},
                headers=headers,
                timeout=timeout,
            )
            resp.raise_for_status()
            data = resp.json()
            choices = data.get("choices") or []
            if choices:
                return choices[0].get("message", {}).get("content")
            return None
        except Exception as exc:
            log(f"[AI-NPC] OpenRouter request failed: {exc}")
            return None


class OpenAIProvider(BaseAIProvider):
    def generate_response(self, npc: NPCState, player_message: str) -> Optional[str]:
        api_key = self.settings.get("api_key")
        if not api_key:
            return None

        model = self.settings.get("model") or "gpt-3.5-turbo"
        timeout = int(CONFIG_CACHE.get("npc_settings", {}).get("response_timeout", 30))

        payload = self._build_prompt(npc, player_message)
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }

        try:
            resp = requests.post(
                "https://api.openai.com/v1/chat/completions",
                json={"model": model, **payload},
                headers=headers,
                timeout=timeout,
            )
            resp.raise_for_status()
            data = resp.json()
            choices = data.get("choices") or []
            if choices:
                return choices[0].get("message", {}).get("content")
            return None
        except Exception as exc:
            log(f"[AI-NPC] OpenAI request failed: {exc}")
            return None


def _personality_prompt(npc: NPCState) -> str:
    personalities = {
        "friendly": "You are Friendly Companion, upbeat and helpful explorer friend.",
        "grumpy": "You are Grumpy Guide, slightly annoyed but still knowledgeable.",
        "wise": "You are Wise Elder, calm and insightful with ancient knowledge.",
        "mysterious": "You are The Mysterious One, cryptic and intriguing.",
        "cheerful": "You are Cheerful Buddy, hyper-positive and energetic.",
        "merchant": "You are Merchant Milo, loves trade talk and negotiation.",
        "guard": "You are Sentinel, protective and duty-focused.",
    }
    base_prompt = personalities.get(npc.personality, "You are a curious NPC in a blocky world.")
    return (
        f"{base_prompt} Engage with player {npc.player_name}. "
        "Keep responses concise (<= 100 words) and offer helpful insights about the world."
    )


def _pick_personality(settings: Dict[str, object]) -> str:
    personalities = settings.get("personalities") or [
        "friendly",
        "grumpy",
        "wise",
        "mysterious",
        "cheerful",
    ]
    return random.choice(personalities)


def _generate_npc_name(personality: str) -> str:
    titles = {
        "friendly": "Buddy",
        "grumpy": "Curmudgeon",
        "wise": "Oracle",
        "mysterious": "Shade",
        "cheerful": "Spark",
        "merchant": "Barter",
        "guard": "Sentinel",
    }
    return f"{titles.get(personality, 'Companion')} NPC"


def parse_task_command(message: str) -> Optional[Dict[str, object]]:
    lowered = message.lower()
    if "follow" in lowered:
        return {"type": "follow"}
    if "stay" in lowered:
        return {"type": "stay"}
    if "greet" in lowered:
        return {"type": "greet"}
    return None


def math_cos_deg(angle: float) -> float:
    return math_cos(angle * (3.14159265 / 180.0))


def math_sin_deg(angle: float) -> float:
    return math_sin(angle * (3.14159265 / 180.0))


def math_cos(value: float) -> float:
    import math

    return math.cos(value)


def math_sin(value: float) -> float:
    import math

    return math.sin(value)


def _load_config() -> Dict[str, object]:
    config = get_mod_config("ai_npc") or {}
    config_path = Path(__file__).with_name("config.json")
    if config_path.exists():
        try:
            file_config = json.loads(config_path.read_text(encoding="utf-8"))
            _merge_dict(config, file_config)
        except Exception as exc:
            log(f"[AI-NPC] Failed to read config.json: {exc}")
    return config


def _merge_dict(target: Dict[str, object], override: Dict[str, object]) -> None:
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(target.get(key), dict):
            _merge_dict(target[key], value)  # type: ignore[arg-type]
        else:
            target[key] = value
