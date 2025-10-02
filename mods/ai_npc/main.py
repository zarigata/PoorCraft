"""
AI NPC System Mod

This mod adds conversational NPCs powered by LLMs (Ollama, Gemini, OpenRouter, OpenAI).
Implementation will happen in the Official Mods phase.
"""


def init():
    """
    Initialize the AI NPC mod.
    
    This will be called when the mod is loaded. It should:
    - Load configuration from mod.json
    - Detect available AI providers
    - Register event listeners for NPC interactions
    - Initialize the selected AI provider
    """
    pass


def detect_ai_providers():
    """
    Detect which AI providers are available.
    
    This will:
    - Check if Ollama is running locally
    - Validate API keys for cloud providers (Gemini, OpenRouter, OpenAI)
    - Return list of available providers
    - Fall back to default provider if configured one is unavailable
    """
    pass


def create_npc():
    """
    Create a new AI-powered NPC.
    
    This will:
    - Generate NPC personality and backstory
    - Assign unique ID and spawn location
    - Register NPC in the world
    - Initialize conversation context
    """
    pass


def handle_chat():
    """
    Handle player chat with NPC.
    
    This will:
    - Receive player message
    - Build conversation context with NPC personality
    - Send request to AI provider
    - Parse and return NPC response
    - Handle timeout and error cases
    """
    pass
