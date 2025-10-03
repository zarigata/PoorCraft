# PoorCraft Modding API Reference

Complete reference for the PoorCraft Python modding API.

## Table of Contents

1. [Core API](#core-api)
2. [Event System](#event-system)
3. [World Access](#world-access)
4. [Entity Management](#entity-management)
5. [Utility Functions](#utility-functions)

## Core API

### poorcraft.api

Main API module for accessing game functionality.

#### Functions

**`get_mod_api()`**
- Returns: ModAPI instance
- Description: Get the main API object (rarely needed, use helper functions instead)

**`get_block(x, y, z)`**
- Parameters:
  - `x` (int): X coordinate
  - `y` (int): Y coordinate
  - `z` (int): Z coordinate
- Returns: int (block type ID, 0-255)
- Description: Get block type at world coordinates

**`set_block(x, y, z, block_type_id)`**
- Parameters:
  - `x` (int): X coordinate
  - `y` (int): Y coordinate
  - `z` (int): Z coordinate
  - `block_type_id` (int): Block type to set (0-255)
- Returns: None
- Description: Set block type at world coordinates

**`get_biome(x, z)`**
- Parameters:
  - `x` (int): X coordinate
  - `z` (int): Z coordinate
- Returns: str ("Desert", "Snow", "Jungle", or "Plains")
- Description: Get biome at coordinates

**`get_height_at(x, z)`**
- Parameters:
  - `x` (int): X coordinate
  - `z` (int): Z coordinate
- Returns: int (Y coordinate of surface)
- Description: Get terrain height at coordinates

**`log(message)`**
- Parameters:
  - `message` (str): Message to log
- Returns: None
- Description: Log message to console with [MOD] prefix

**`is_server()`**
- Returns: bool
- Description: Check if running on server side

**`set_shared_data(key, value)`**
- Parameters:
  - `key` (str): Data key
  - `value` (any): Data value
- Returns: None
- Description: Store data accessible to all mods

**`get_shared_data(key)`**
- Parameters:
  - `key` (str): Data key
- Returns: any (stored value or None)
- Description: Retrieve shared data

## Event System

### poorcraft.events

Event decorators and registration functions.

#### Decorators

**`@on_block_place`**
- Event: BlockPlaceEvent
- Description: Called when a block is placed
- Cancellable: Yes

**`@on_block_break`**
- Event: BlockBreakEvent
- Description: Called when a block is broken
- Cancellable: Yes

**`@on_player_join`**
- Event: PlayerJoinEvent
- Description: Called when a player joins
- Cancellable: No

**`@on_player_leave`**
- Event: PlayerLeaveEvent
- Description: Called when a player leaves
- Cancellable: No

**`@on_chunk_generate`**
- Event: ChunkGenerateEvent
- Description: Called when a chunk is generated
- Cancellable: No

**`@on_world_load`**
- Event: WorldLoadEvent
- Description: Called when a world is loaded
- Cancellable: No

#### Functions

**`register_event(event_name, callback)`**
- Parameters:
  - `event_name` (str): Event name
  - `callback` (callable): Function to call
- Returns: None
- Description: Register event handler manually

**`unregister_event(event_name, callback)`**
- Parameters:
  - `event_name` (str): Event name
  - `callback` (callable): Function to unregister
- Returns: None
- Description: Unregister event handler

## World Access

### poorcraft.world

World, chunk, and block access.

#### Classes

**`BlockType`**

Block type constants:
- `AIR = 0`
- `DIRT = 1`
- `STONE = 2`
- `GRASS = 3`
- `SAND = 4`
- `SANDSTONE = 5`
- `SNOW_BLOCK = 6`
- `ICE = 7`
- `JUNGLE_GRASS = 8`
- `JUNGLE_DIRT = 9`
- `WOOD = 10`
- `LEAVES = 11`
- `CACTUS = 12`
- `SNOW_LAYER = 13`
- `BEDROCK = 14`

Methods:
- `from_id(id)`: Convert ID to name
- `to_id(name)`: Convert name to ID

**`World`**

Represents the game world.

Methods:
- `get_block(x, y, z)`: Get block at coordinates
- `set_block(x, y, z, type_id)`: Set block at coordinates
- `get_biome(x, z)`: Get biome at coordinates
- `get_height_at(x, z)`: Get terrain height
- `get_chunk(chunk_x, chunk_z)`: Get chunk object

**`Chunk`**

Represents a 16×256×16 chunk.

Properties:
- `chunk_x` (int): Chunk X coordinate
- `chunk_z` (int): Chunk Z coordinate

Methods:
- `get_block(local_x, local_y, local_z)`: Get block in chunk
- `set_block(local_x, local_y, local_z, type_id)`: Set block in chunk
- `fill(type_id)`: Fill entire chunk
- `fill_layer(y, type_id)`: Fill horizontal layer

#### Functions

**`get_world()`**
- Returns: World instance
- Description: Get world singleton

## Entity Management

### poorcraft.entity

Entity access and management (placeholder for future expansion).

#### Classes

**`Entity`**

Base entity class.

Properties:
- `entity_id` (int): Unique entity ID
- `x`, `y`, `z` (float): Position
- `yaw`, `pitch` (float): Rotation

Methods:
- `get_position()`: Return (x, y, z) tuple
- `set_position(x, y, z)`: Set position
- `get_rotation()`: Return (yaw, pitch) tuple
- `set_rotation(yaw, pitch)`: Set rotation

**`Player(Entity)`**

Player entity.

Additional properties:
- `username` (str): Player username
- `player_id` (int): Player ID

Methods:
- `teleport(x, y, z)`: Teleport player

**`NPC(Entity)`**

AI NPC entity.

Additional properties:
- `personality` (str): NPC personality
- `conversation_context` (dict): Conversation state

Methods:
- `spawn(x, y, z)`: Spawn NPC
- `despawn()`: Remove NPC
- `say(message)`: Make NPC say something

#### Functions

**`get_players()`**
- Returns: list[Player]
- Description: Get all online players

**`get_player_by_id(player_id)`**
- Parameters:
  - `player_id` (int): Player ID
- Returns: Player or None
- Description: Get player by ID

**`spawn_entity(entity_type, x, y, z)`**
- Parameters:
  - `entity_type` (str): Entity type
  - `x`, `y`, `z` (float): Position
- Returns: Entity
- Description: Spawn entity at position

## Utility Functions

### poorcraft.mod

Base mod class and utilities.

#### Classes

**`BaseMod`**

Base class for mods.

Constructor:
- `__init__(mod_id, mod_name, mod_version, config)`

Methods:
- `init()`: Initialize mod (override)
- `enable()`: Enable mod (override)
- `disable()`: Disable mod (override)
- `get_config(key, default=None)`: Get config value
- `log(message)`: Log with mod name prefix

Example:
```python
from poorcraft import BaseMod

class MyMod(BaseMod):
    def init(self):
        self.log("Initializing")
    
    def enable(self):
        self.log("Enabled")
    
    def disable(self):
        self.log("Disabled")
```

## Event Objects

### BlockPlaceEvent

Properties:
- `x`, `y`, `z` (int): Block coordinates
- `block_type_id` (int): Block type being placed
- `player_id` (int): Player placing block (-1 if not player)

Methods:
- `cancel()`: Prevent block placement
- `is_cancelled()`: Check if cancelled

### BlockBreakEvent

Properties:
- `x`, `y`, `z` (int): Block coordinates
- `block_type_id` (int): Block type being broken
- `player_id` (int): Player breaking block (-1 if not player)

Methods:
- `cancel()`: Prevent block breaking
- `is_cancelled()`: Check if cancelled

### PlayerJoinEvent

Properties:
- `player_id` (int): Player ID
- `username` (str): Player username
- `x`, `y`, `z` (float): Spawn position

### PlayerLeaveEvent

Properties:
- `player_id` (int): Player ID
- `username` (str): Player username
- `reason` (str): Disconnect reason

### ChunkGenerateEvent

Properties:
- `chunk_x`, `chunk_z` (int): Chunk coordinates
- `chunk` (Chunk): Chunk object (can modify)

### WorldLoadEvent

Properties:
- `seed` (long): World seed
- `generate_structures` (bool): Whether structures are enabled

## Type Reference

### Block Type IDs

| ID | Name | Description |
|----|------|-------------|
| 0 | AIR | Empty space |
| 1 | DIRT | Dirt block |
| 2 | STONE | Stone block |
| 3 | GRASS | Grass block (plains) |
| 4 | SAND | Sand block (desert) |
| 5 | SANDSTONE | Sandstone block |
| 6 | SNOW_BLOCK | Snow block |
| 7 | ICE | Ice block |
| 8 | JUNGLE_GRASS | Jungle grass |
| 9 | JUNGLE_DIRT | Jungle dirt |
| 10 | WOOD | Wood log |
| 11 | LEAVES | Tree leaves |
| 12 | CACTUS | Cactus |
| 13 | SNOW_LAYER | Snow layer |
| 14 | BEDROCK | Bedrock (indestructible) |

### Biome Names

- `"Desert"` - Hot, dry, sandy terrain
- `"Snow"` - Cold, icy, snowy terrain
- `"Jungle"` - Hot, wet, dense vegetation
- `"Plains"` - Temperate, grassy, rolling hills
