# Modding Guide

**Status**: Coming in Python Modding Framework phase

This document will contain:
- Getting started with mod development
- Mod structure and metadata
- Event system overview
- API reference
- Example mods walkthrough

## Current Examples

For now, refer to the two official mods in the `mods/` directory:

### Procedural Skin Generator
- **Location**: `mods/skin_generator/`
- **Purpose**: Generates up to 256 unique 16x16 block textures with biome-specific color palettes
- **Files**: 
  - `mod.json` - Mod metadata and configuration
  - `main.py` - Main mod logic (placeholder)

### AI NPC System
- **Location**: `mods/ai_npc/`
- **Purpose**: Adds conversational NPCs powered by LLMs (Ollama, Gemini, OpenRouter, OpenAI)
- **Files**:
  - `mod.json` - Mod metadata and configuration
  - `main.py` - Main mod logic (placeholder)

## Mod Structure

Each mod contains a `mod.json` file for metadata and configuration:

```json
{
  "id": "unique_mod_id",
  "name": "Display Name",
  "version": "0.1.0",
  "description": "What this mod does",
  "author": "Your Name",
  "main": "module.path.to.main",
  "enabled": true,
  "config": {
    // Mod-specific configuration
  }
}
```

## Coming Soon

Full API documentation, event system details, and step-by-step tutorials will be added in the Python Modding Framework phase.
