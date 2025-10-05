# PoorCraft Event Catalog

Complete catalog of all events available to mods.

## Event Categories

- [Block Events](#block-events)
- [Player Events](#player-events)
- [World Events](#world-events)
- [Chunk Events](#chunk-events)

## Block Events

### block_place

**Decorator**: `@on_block_place`

**When**: Fired when a block is about to be placed

**Cancellable**: Yes

**Properties**:
- `x` (int): Block X coordinate
- `y` (int): Block Y coordinate (0-255)
- `z` (int): Block Z coordinate
- `block_type_id` (int): Block type being placed (0-255)
- `player_id` (int): Player placing block (-1 if not player-initiated)

**Example**:
```python
@on_block_place
def prevent_bedrock_placement(event):
    if event.block_type_id == 14:  # Bedrock
        event.cancel()
        log("Cannot place bedrock!")
```

**Use Cases**:
- Prevent certain blocks from being placed
- Log block placements
- Trigger custom effects when blocks are placed
- Modify block type before placement

---

### block_break

**Decorator**: `@on_block_break`

**When**: Fired when a block is about to be broken

**Cancellable**: Yes

**Properties**:
- `x` (int): Block X coordinate
- `y` (int): Block Y coordinate (0-255)
- `z` (int): Block Z coordinate
- `block_type_id` (int): Block type being broken (0-255)
- `player_id` (int): Player breaking block (-1 if not player-initiated)

**Example**:
```python
@on_block_break
def protect_bedrock(event):
    if event.block_type_id == 14:  # Bedrock
        event.cancel()
        log("Cannot break bedrock!")
```

**Use Cases**:
- Protect certain blocks from being broken
- Drop custom items when blocks are broken
- Track block destruction
- Implement custom mining mechanics

---

## Player Events

### player_join

**Decorator**: `@on_player_join`

**When**: Fired when a player successfully joins the server

**Cancellable**: No

**Properties**:
- `player_id` (int): Unique player ID
- `username` (str): Player username
- `x` (float): Spawn X coordinate
- `y` (float): Spawn Y coordinate
- `z` (float): Spawn Z coordinate

**Example**:
```python
@on_player_join
def welcome_player(event):
    log(f"Welcome {event.username}!")
    log(f"Spawned at ({event.x}, {event.y}, {event.z})")
```

**Use Cases**:
- Send welcome messages
- Initialize player data
- Spawn NPCs for player
- Track player logins
- Custom spawn logic

---

### player_leave

**Decorator**: `@on_player_leave`

**When**: Fired when a player disconnects from the server

**Cancellable**: No

**Properties**:
- `player_id` (int): Player ID
- `username` (str): Player username
- `reason` (str): Disconnect reason ("Quit", "Timeout", "Kicked", etc.)

**Example**:
```python
@on_player_leave
def goodbye_player(event):
    log(f"{event.username} left: {event.reason}")
```

**Use Cases**:
- Save player data
- Despawn player NPCs
- Track player activity
- Broadcast leave messages
- Cleanup player resources

---

## World Events

### world_load

**Decorator**: `@on_world_load`

**When**: Fired when a world is created or loaded

**Cancellable**: No

**Properties**:
- `seed` (long): World seed
- `generate_structures` (bool): Whether structures are enabled

**Example**:
```python
@on_world_load
def init_world_data(event):
    log(f"World loaded with seed: {event.seed}")
    if event.generate_structures:
        log("Structures enabled")
```

**Use Cases**:
- Initialize world-specific data
- Log world information
- Set up custom world features
- Prepare mod state for new world

---

## Chunk Events

### chunk_generate

**Decorator**: `@on_chunk_generate`

**When**: Fired after terrain generation but before features (trees, etc.)

**Cancellable**: No

**Properties**:
- `chunk_x` (int): Chunk X coordinate
- `chunk_z` (int): Chunk Z coordinate
- `chunk` (Chunk): Chunk object (can modify blocks)

**Example**:
```python
@on_chunk_generate
def add_custom_ore(event):
    chunk = event.chunk
    # Add ore at random positions
    import random
    for _ in range(10):
        x = random.randint(0, 15)
        y = random.randint(5, 50)
        z = random.randint(0, 15)
        chunk.set_block(x, y, z, BlockType.STONE)  # Placeholder for ore
```

**Use Cases**:
- Add custom ores or resources
- Modify terrain generation
- Add custom structures
- Implement custom biome features
- Generate dungeons or caves

---

## Event Timing

### Execution Order

1. **World Load** - When world is created
2. **Chunk Generate** - For each chunk as it's generated
3. **Player Join** - When player connects
4. **Block Place/Break** - During gameplay
5. **Player Leave** - When player disconnects

### Cancellation

Cancellable events can be stopped by calling `event.cancel()`:

```python
@on_block_place
def prevent_action(event):
    if some_condition:
        event.cancel()  # Prevents block placement
```

Once cancelled, the event stops propagating to other handlers.

### Multiple Handlers

Multiple handlers can be registered for the same event:

```python
@on_block_place
def handler1(event):
    log("Handler 1")

@on_block_place
def handler2(event):
    log("Handler 2")
```

Handlers are called in registration order.

## Best Practices

### Error Handling

Always handle errors in event handlers:

```python
@on_block_place
def safe_handler(event):
    try:
        # Your code
        pass
    except Exception as e:
        log(f"Error: {e}")
```

### Performance

Avoid expensive operations in frequently-fired events:

```python
# Bad: Expensive operation in block_place
@on_block_place
def slow_handler(event):
    # This fires for EVERY block placement!
    for i in range(1000000):
        pass

# Good: Use chunk_generate for bulk operations
@on_chunk_generate
def efficient_handler(event):
    # This fires once per chunk
    for i in range(1000000):
        pass
```

### Server-Only Events

Some events only fire on server:

```python
from poorcraft import is_server

@on_player_join
def server_only_handler(event):
    if not is_server():
        return  # Skip on client
    # Server-only code
```

## Future Events

Planned events for future releases:

- `entity_spawn` - When an entity spawns
- `entity_death` - When an entity dies
- `chat_message` - When a chat message is sent
- `inventory_change` - When inventory changes
- `damage_taken` - When entity takes damage
- `item_use` - When an item is used

Check the changelog for updates!
