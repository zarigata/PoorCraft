# PoorCraft Modding Examples

Step-by-step tutorials for common modding tasks.

## Table of Contents

1. [Hello World](#hello-world)
2. [Block Logger](#block-logger)
3. [Custom Ore Generator](#custom-ore-generator)
4. [Player Tracker](#player-tracker)
5. [Terrain Modifier](#terrain-modifier)
6. [Anti-Grief Protection](#anti-grief-protection)

## Hello World

The simplest possible mod.

### Step 1: Create Directory

```bash
mkdir mods/hello_world
cd mods/hello_world
```

### Step 2: Create mod.json

```json
{
  "id": "hello_world",
  "name": "Hello World",
  "version": "1.0.0",
  "description": "My first mod",
  "author": "Your Name",
  "main": "hello_world.main",
  "enabled": true
}
```

### Step 3: Create main.py

```python
from poorcraft import log

def init():
    log("Hello, PoorCraft!")
```

### Step 4: Create __init__.py

Create empty file.

### Step 5: Run

Start PoorCraft and check console for "Hello, PoorCraft!" message.

---

## Block Logger

Logs all block placements and breaks.

### Complete Code

**mod.json**:
```json
{
  "id": "block_logger",
  "name": "Block Logger",
  "version": "1.0.0",
  "description": "Logs all block changes",
  "author": "Your Name",
  "main": "block_logger.main",
  "enabled": true,
  "config": {
    "log_to_file": false
  }
}
```

**main.py**:
```python
from poorcraft import log, on_block_place, on_block_break
from poorcraft.world import BlockType

def init():
    log("Block Logger initialized")

@on_block_place
def log_placement(event):
    block_name = BlockType.from_id(event.block_type_id)
    log(f"[PLACE] {block_name} at ({event.x}, {event.y}, {event.z})")
    if event.player_id != -1:
        log(f"  by player {event.player_id}")

@on_block_break
def log_break(event):
    block_name = BlockType.from_id(event.block_type_id)
    log(f"[BREAK] {block_name} at ({event.x}, {event.y}, {event.z})")
    if event.player_id != -1:
        log(f"  by player {event.player_id}")
```

### What It Does

- Logs every block placement with coordinates and block type
- Logs every block break with coordinates and block type
- Shows player ID if player-initiated
- Uses BlockType.from_id() to convert ID to readable name

---

## Custom Ore Generator

Adds custom ore veins during chunk generation.

### Complete Code

**mod.json**:
```json
{
  "id": "ore_generator",
  "name": "Custom Ore Generator",
  "version": "1.0.0",
  "description": "Adds custom ore veins",
  "author": "Your Name",
  "main": "ore_generator.main",
  "enabled": true,
  "config": {
    "ore_type": 2,
    "veins_per_chunk": 5,
    "vein_size": 8,
    "min_height": 5,
    "max_height": 50
  }
}
```

**main.py**:
```python
from poorcraft import log, on_chunk_generate
from poorcraft.world import BlockType
import random

config = {}

def init():
    global config
    # Config is passed by mod loader
    log("Ore Generator initialized")

@on_chunk_generate
def generate_ore_veins(event):
    chunk = event.chunk
    
    # Generate multiple ore veins
    for _ in range(config.get('veins_per_chunk', 5)):
        # Random starting position
        start_x = random.randint(0, 15)
        start_y = random.randint(
            config.get('min_height', 5),
            config.get('max_height', 50)
        )
        start_z = random.randint(0, 15)
        
        # Generate vein
        vein_size = config.get('vein_size', 8)
        ore_type = config.get('ore_type', BlockType.STONE)
        
        for i in range(vein_size):
            # Random walk from starting position
            x = start_x + random.randint(-2, 2)
            y = start_y + random.randint(-1, 1)
            z = start_z + random.randint(-2, 2)
            
            # Bounds check
            if 0 <= x < 16 and 0 <= y < 256 and 0 <= z < 16:
                # Only replace stone
                if chunk.get_block(x, y, z) == BlockType.STONE:
                    chunk.set_block(x, y, z, ore_type)
```

### What It Does

- Generates ore veins during chunk generation
- Configurable ore type, vein count, size, and height range
- Uses random walk algorithm for natural-looking veins
- Only replaces stone blocks (preserves caves, etc.)

---

## Player Tracker

Tracks player join/leave and maintains online player list.

### Complete Code

**mod.json**:
```json
{
  "id": "player_tracker",
  "name": "Player Tracker",
  "version": "1.0.0",
  "description": "Tracks online players",
  "author": "Your Name",
  "main": "player_tracker.main",
  "enabled": true,
  "server_only": true
}
```

**main.py**:
```python
from poorcraft import log, on_player_join, on_player_leave, is_server
from datetime import datetime

online_players = {}

def init():
    if not is_server():
        return
    log("Player Tracker initialized (server-only)")

@on_player_join
def track_join(event):
    player_data = {
        'username': event.username,
        'join_time': datetime.now(),
        'spawn_pos': (event.x, event.y, event.z)
    }
    online_players[event.player_id] = player_data
    
    log(f"Player {event.username} joined")
    log(f"  ID: {event.player_id}")
    log(f"  Spawn: ({event.x:.1f}, {event.y:.1f}, {event.z:.1f})")
    log(f"  Online players: {len(online_players)}")

@on_player_leave
def track_leave(event):
    if event.player_id in online_players:
        player_data = online_players[event.player_id]
        join_time = player_data['join_time']
        session_duration = datetime.now() - join_time
        
        log(f"Player {event.username} left")
        log(f"  Reason: {event.reason}")
        log(f"  Session duration: {session_duration}")
        
        del online_players[event.player_id]
    
    log(f"  Online players: {len(online_players)}")

def get_online_count():
    return len(online_players)

def get_online_players():
    return [p['username'] for p in online_players.values()]
```

### What It Does

- Tracks all online players in a dictionary
- Records join time and spawn position
- Calculates session duration on leave
- Provides functions to query online player count and list
- Server-only (doesn't run on clients)

---

## Terrain Modifier

Modifies terrain during generation to create custom features.

### Complete Code

**mod.json**:
```json
{
  "id": "terrain_modifier",
  "name": "Terrain Modifier",
  "version": "1.0.0",
  "description": "Adds custom terrain features",
  "author": "Your Name",
  "main": "terrain_modifier.main",
  "enabled": true,
  "config": {
    "add_pillars": true,
    "pillar_chance": 0.1,
    "flatten_spawn": true
  }
}
```

**main.py**:
```python
from poorcraft import log, on_chunk_generate, on_world_load
from poorcraft.world import BlockType
import random

config = {}
world_spawn = (0, 0)

def init():
    global config
    log("Terrain Modifier initialized")

@on_world_load
def save_spawn(event):
    global world_spawn
    world_spawn = (0, 0)  # Default spawn
    log(f"World spawn: {world_spawn}")

@on_chunk_generate
def modify_terrain(event):
    chunk = event.chunk
    chunk_x, chunk_z = event.chunk_x, event.chunk_z
    
    # Flatten spawn area
    if config.get('flatten_spawn', True):
        if chunk_x == 0 and chunk_z == 0:
            flatten_spawn_area(chunk)
    
    # Add random stone pillars
    if config.get('add_pillars', True):
        if random.random() < config.get('pillar_chance', 0.1):
            add_pillar(chunk)

def flatten_spawn_area(chunk):
    """Flatten center of spawn chunk"""
    for x in range(6, 10):
        for z in range(6, 10):
            # Set to grass at Y=70
            for y in range(256):
                if y < 70:
                    chunk.set_block(x, y, z, BlockType.STONE)
                elif y == 70:
                    chunk.set_block(x, y, z, BlockType.GRASS)
                else:
                    chunk.set_block(x, y, z, BlockType.AIR)

def add_pillar(chunk):
    """Add a stone pillar at random position"""
    x = random.randint(0, 15)
    z = random.randint(0, 15)
    
    # Find surface
    surface_y = 70  # Default
    for y in range(255, 0, -1):
        if chunk.get_block(x, y, z) != BlockType.AIR:
            surface_y = y
            break
    
    # Build pillar
    height = random.randint(10, 30)
    for y in range(surface_y + 1, surface_y + height + 1):
        if y < 256:
            chunk.set_block(x, y, z, BlockType.STONE)
```

### What It Does

- Flattens spawn area for easier building
- Adds random stone pillars to terrain
- Configurable pillar frequency
- Finds surface height automatically
- Demonstrates chunk modification during generation

---

## Anti-Grief Protection

Prevents certain destructive actions.

### Complete Code

**mod.json**:
```json
{
  "id": "anti_grief",
  "name": "Anti-Grief Protection",
  "version": "1.0.0",
  "description": "Prevents griefing",
  "author": "Your Name",
  "main": "anti_grief.main",
  "enabled": true,
  "server_only": true,
  "config": {
    "protect_bedrock": true,
    "prevent_lava": false,
    "protect_spawn_radius": 50
  }
}
```

**main.py**:
```python
from poorcraft import log, on_block_place, on_block_break, is_server
from poorcraft.world import BlockType
import math

config = {}
world_spawn = (0, 70, 0)

def init():
    global config
    if not is_server():
        return
    log("Anti-Grief Protection initialized")

@on_block_break
def prevent_bedrock_break(event):
    # Protect bedrock
    if config.get('protect_bedrock', True):
        if event.block_type_id == BlockType.BEDROCK:
            event.cancel()
            log(f"Prevented bedrock break at ({event.x}, {event.y}, {event.z})")
            return
    
    # Protect spawn area
    if is_in_spawn_protection(event.x, event.z):
        event.cancel()
        log(f"Prevented block break in spawn protection")

@on_block_place
def prevent_dangerous_blocks(event):
    # Prevent lava placement (if configured)
    if config.get('prevent_lava', False):
        # Placeholder: check for lava block type
        pass
    
    # Protect spawn area
    if is_in_spawn_protection(event.x, event.z):
        event.cancel()
        log(f"Prevented block place in spawn protection")

def is_in_spawn_protection(x, z):
    """Check if coordinates are in spawn protection radius"""
    radius = config.get('protect_spawn_radius', 50)
    spawn_x, spawn_y, spawn_z = world_spawn
    distance = math.sqrt((x - spawn_x)**2 + (z - spawn_z)**2)
    return distance < radius
```

### What It Does

- Prevents bedrock from being broken
- Protects spawn area from modifications
- Configurable protection radius
- Can prevent dangerous block placement (lava, TNT, etc.)
- Server-only for security

---

## Next Steps

- Study the official mods (`skin_generator`, `ai_npc`) for advanced examples
- Read the [API Reference](API_REFERENCE.md) for complete API documentation
- Check the [Event Catalog](EVENT_CATALOG.md) for all available events
- Experiment with combining multiple events and features
- Share your mods with the community!
