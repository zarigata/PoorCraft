"""
PoorCraft Python Modding API

This package provides the modding API for PoorCraft.
Mods can import from this package to access game events, world data, and entity management.

Example usage:
    from poorcraft import api, events, world
    
    @events.on_block_place
    def handle_block_place(event):
        api.log(f"Block placed at {event.x}, {event.y}, {event.z}")
"""

__version__ = '0.1.0'

# Import main modules for convenience
from . import api
from . import events
from . import world
from . import entity
from . import mod

# Export commonly used functions and classes
from .api import (
    get_mod_api,
    get_block,
    set_block,
    get_biome,
    log,
    is_server,
    set_shared_data,
    get_shared_data,
    add_procedural_texture,
    get_mod_config,
    spawn_npc,
    despawn_npc,
    npc_say,
)
from .events import on_block_place, on_block_break, on_player_join, on_player_leave, on_chunk_generate, on_world_load
from .world import BlockType, World, get_world
from .mod import BaseMod

__all__ = [
    'api',
    'events',
    'world',
    'entity',
    'mod',
    'get_mod_api',
    'get_block',
    'set_block',
    'get_biome',
    'log',
    'is_server',
    'set_shared_data',
    'get_shared_data',
    'add_procedural_texture',
    'get_mod_config',
    'spawn_npc',
    'despawn_npc',
    'npc_say',
    'on_block_place',
    'on_block_break',
    'on_player_join',
    'on_player_leave',
    'on_chunk_generate',
    'on_world_load',
    'BlockType',
    'World',
    'get_world',
    'BaseMod',
]
