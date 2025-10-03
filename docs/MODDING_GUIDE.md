# PoorCraft Modding Guide

Welcome to PoorCraft modding! This guide will help you create your first mod.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Mod Structure](#mod-structure)
3. [Creating Your First Mod](#creating-your-first-mod)
4. [Event System](#event-system)
5. [World Access](#world-access)
6. [Configuration](#configuration)
7. [Best Practices](#best-practices)
8. [Examples](#examples)

## Getting Started

### Prerequisites

- Python 3.8 or higher installed
- PoorCraft game installed
- Basic Python knowledge
- Text editor or IDE

### Setting Up Development Environment

1. Install Python dependencies:
   ```bash
   cd python/
   pip install -r requirements.txt
   ```

2. Create a new mod directory:
   ```bash
   mkdir mods/my_first_mod
   cd mods/my_first_mod
   ```

3. Create required files:
   - `mod.json` - Mod metadata
   - `main.py` - Main mod code
   - `__init__.py` - Python package marker (can be empty)

## Mod Structure

### Directory Layout

```
mods/
└── my_first_mod/
    ├── __init__.py          # Python package marker
    ├── mod.json             # Mod metadata and config
    ├── main.py              # Main mod code
    └── resources/           # Optional: textures, data files
```

### mod.json Format

```json
{
  "id": "my_first_mod",
  "name": "My First Mod",
  "version": "1.0.0",
  "description": "A simple example mod",
  "author": "Your Name",
  "main": "my_first_mod.main",
  "enabled": true,
  "server_only": false,
  "config": {
    "custom_setting": "value"
  }
}
```

**Fields**:
- `id`: Unique mod identifier (lowercase, no spaces)
- `name`: Display name
- `version`: Semantic version (major.minor.patch)
- `description`: What your mod does
- `author`: Your name
- `main`: Python module path (directory.filename without .py)
- `enabled`: Whether mod loads on startup
- `server_only`: If true, only loads on server (not clients)
- `config`: Custom configuration (accessible in Python)

## Creating Your First Mod

### Step 1: Create mod.json

Create `mods/hello_world/mod.json`:

```json
{
  "id": "hello_world",
  "name": "Hello World",
  "version": "1.0.0",
  "description": "Prints hello when blocks are placed",
  "author": "You",
  "main": "hello_world.main",
  "enabled": true
}
```

### Step 2: Create main.py

Create `mods/hello_world/main.py`:

```python
from poorcraft import log, on_block_place

def init():
    """Called when mod is loaded"""
    log("Hello World mod initialized!")

@on_block_place
def handle_block_place(event):
    """Called when a block is placed"""
    log(f"Block placed at {event.x}, {event.y}, {event.z}")
    log(f"Block type: {event.block_type_id}")
```

### Step 3: Create __init__.py

Create empty `mods/hello_world/__init__.py`

### Step 4: Run the game

Start PoorCraft and your mod will load automatically!

## Event System

### Available Events

**Block Events**:
- `@on_block_place` - When a block is placed
- `@on_block_break` - When a block is broken

**Player Events**:
- `@on_player_join` - When a player joins the server
- `@on_player_leave` - When a player leaves the server

**World Events**:
- `@on_chunk_generate` - When a chunk is generated
- `@on_world_load` - When a world is loaded

### Event Properties

**BlockPlaceEvent / BlockBreakEvent**:
- `event.x`, `event.y`, `event.z` - Block coordinates
- `event.block_type_id` - Block type (0-255)
- `event.player_id` - Player who placed/broke block (-1 if not player)
- `event.cancel()` - Prevent block placement/breaking

**PlayerJoinEvent / PlayerLeaveEvent**:
- `event.player_id` - Unique player ID
- `event.username` - Player username
- `event.x`, `event.y`, `event.z` - Player position (join only)
- `event.reason` - Disconnect reason (leave only)

**ChunkGenerateEvent**:
- `event.chunk_x`, `event.chunk_z` - Chunk coordinates
- `event.chunk` - Chunk object (can modify blocks)

**WorldLoadEvent**:
- `event.seed` - World seed
- `event.generate_structures` - Whether structures are enabled

### Cancelling Events

Some events can be cancelled to prevent the action:

```python
@on_block_place
def prevent_dirt_placement(event):
    if event.block_type_id == 1:  # Dirt
        event.cancel()
        log("Dirt placement prevented!")
```

## World Access

### Getting/Setting Blocks

```python
from poorcraft import get_block, set_block, BlockType

# Get block at position
block_id = get_block(100, 64, 200)

# Set block at position
set_block(100, 64, 200, BlockType.STONE)
```

### Block Types

```python
from poorcraft.world import BlockType

BlockType.AIR = 0
BlockType.DIRT = 1
BlockType.STONE = 2
BlockType.GRASS = 3
BlockType.SAND = 4
# ... see API reference for full list
```

### Biome Access

```python
from poorcraft import api

biome = api.get_biome(100, 200)  # Returns "Desert", "Snow", "Jungle", or "Plains"
```

### Terrain Height

```python
from poorcraft import api

height = api.get_height_at(100, 200)  # Returns Y coordinate of surface
```

## Configuration

### Accessing Config

```python
# Or use BaseMod class:
from poorcraft import BaseMod

class MyMod(BaseMod):
    def init(self):
        setting = self.get_config("custom_setting", "default_value")
        self.log(f"Setting: {setting}")
```

### Shared Data

Mods can share data with each other:

```python
from poorcraft import api

# Store data
api.set_shared_data("my_mod.counter", 42)

# Retrieve data
counter = api.get_shared_data("my_mod.counter")  # Returns 42
```

## Best Practices

### 1. Use Descriptive Names

```python
# Good
@on_block_place
def log_block_placement(event):
    pass

# Bad
@on_block_place
def func1(event):
    pass
```

### 2. Handle Errors Gracefully

```python
@on_block_place
def safe_handler(event):
    try:
        # Your code here
        pass
    except Exception as e:
        log(f"Error in handler: {e}")
```

### 3. Check Server/Client Side

```python
from poorcraft import is_server

def init():
    if is_server():
        log("Running on server")
        # Server-only code
    else:
        log("Running on client")
        # Client-only code
```

### 4. Use Lifecycle Methods

```python
def init():
    """Setup: register events, load resources"""
    pass

def enable():
    """Start: begin mod functionality"""
    pass

def disable():
    """Stop: cleanup, unregister events"""
    pass
```

### 5. Log Important Events

```python
from poorcraft import log

log("Mod initialized")
log(f"Config loaded: {config}")
log(f"Event triggered: {event}")
```

## Examples

See the official mods for complete examples:

- **Skin Generator** (`mods/skin_generator/`) - Procedural texture generation
- **AI NPC** (`mods/ai_npc/`) - AI-powered NPCs with LLM integration

### Example: Block Logger

Logs all block placements and breaks:

```python
from poorcraft import log, on_block_place, on_block_break

def init():
    log("Block Logger initialized")

@on_block_place
def log_place(event):
    log(f"Block {event.block_type_id} placed at ({event.x}, {event.y}, {event.z})")

@on_block_break
def log_break(event):
    log(f"Block {event.block_type_id} broken at ({event.x}, {event.y}, {event.z})")
```

## Next Steps

- Read the [API Reference](API_REFERENCE.md) for complete API documentation
- Check the [Event Catalog](EVENT_CATALOG.md) for all available events
- Study the official mods for advanced examples
- Join the community to share your mods!

## Troubleshooting

**Mod not loading?**
- Check mod.json syntax (use JSON validator)
- Verify main module path matches directory structure
- Check console for error messages

**Events not firing?**
- Ensure event decorator is used correctly
- Check if mod is enabled in mod.json
- Verify init() function is called

**Import errors?**
- Ensure py4j is installed: `pip install py4j==0.10.9.7`
- Check Python version (3.8+ required)
- Verify poorcraft package is in python/ directory

**Need help?**
- Check console logs for error messages
- Read the API reference documentation
- Study the official mod examples
