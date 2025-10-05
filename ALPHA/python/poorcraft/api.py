"""
PoorCraft Python API - Main module for accessing game functionality.

This module provides the main interface for Python mods to interact with the game.
It wraps the Java ModAPI with Pythonic functions.

Example usage:
    from poorcraft import api
    
    block_id = api.get_block(100, 64, 200)
    api.set_block(100, 64, 200, 1)  # Set to dirt
    api.log("Hello from Python!")
"""

from py4j.java_gateway import JavaGateway
import json

# Global gateway and API instances
_gateway = None
_mod_api = None


def _init_gateway():
    """
    Initializes the Py4J gateway connection to Java.
    
    Connects to the Java GatewayServer on port 25333 and retrieves
    the ModAPI entry point.
    
    Returns:
        JavaGateway: The gateway instance
    """
    global _gateway, _mod_api
    
    try:
        # Connect to Java gateway on port 25333
        _gateway = JavaGateway()
        
        # Get ModAPI entry point
        _mod_api = _gateway.entry_point
        
        print("[PoorCraft API] Connected to Java gateway")
        return _gateway
        
    except Exception as e:
        print(f"[PoorCraft API] Failed to connect to Java gateway: {e}")
        raise


def get_mod_api():
    """
    Gets the ModAPI instance.
    
    Initializes the gateway if not already connected.
    
    Returns:
        ModAPI: The Java ModAPI instance (Py4J proxy)
    """
    global _mod_api
    
    if _mod_api is None:
        _init_gateway()
    
    return _mod_api


# ========== World Access Functions ==========

def get_block(x, y, z):
    """
    Gets the block type at the specified world coordinates.
    
    Args:
        x (int): X coordinate
        y (int): Y coordinate (0-255)
        z (int): Z coordinate
    
    Returns:
        int: Block type ID (0-255), or -1 if world not loaded
    """
    api = get_mod_api()
    return api.getBlock(x, y, z)


def set_block(x, y, z, block_type_id):
    """
    Sets the block type at the specified world coordinates.
    
    Args:
        x (int): X coordinate
        y (int): Y coordinate (0-255)
        z (int): Z coordinate
        block_type_id (int): Block type ID to set (0-255)
    """
    api = get_mod_api()
    api.setBlock(x, y, z, block_type_id)


def get_biome(x, z):
    """
    Gets the biome at the specified coordinates.
    
    Args:
        x (int): X coordinate
        z (int): Z coordinate
    
    Returns:
        str: Biome name ("Desert", "Snow", "Jungle", "Plains")
    """
    api = get_mod_api()
    return api.getBiome(x, z)


def get_height_at(x, z):
    """
    Gets the terrain height at the specified coordinates.
    
    Args:
        x (int): X coordinate
        z (int): Z coordinate
    
    Returns:
        int: Y coordinate of the surface
    """
    api = get_mod_api()
    return api.getHeightAt(x, z)


# ========== Utility Functions ==========

def log(message):
    """
    Logs a message to the console with [MOD] prefix.
    
    Args:
        message (str): Message to log
    """
    api = get_mod_api()
    api.log(str(message))


def is_server():
    """
    Checks if running on the server side.
    
    Returns:
        bool: True if running on server, False otherwise
    """
    api = get_mod_api()
    return api.isServer()


# ========== Shared Data Functions ==========

def set_shared_data(key, value):
    """
    Stores data in the shared data map.
    Allows mods to communicate with each other.
    
    Args:
        key (str): Data key
        value: Data value (any object)
    """
    api = get_mod_api()
    api.setSharedData(key, value)


def get_shared_data(key):
    """
    Retrieves data from the shared data map.
    
    Args:
        key (str): Data key
    
    Returns:
        The stored value, or None if key doesn't exist
    """
    api = get_mod_api()
    return api.getSharedData(key)


# ========== Procedural Texture Registration ==========

def add_procedural_texture(name, rgba_bytes):
    """
    Registers a procedurally generated texture with the Java renderer.

    Args:
        name (str): Unique texture name (e.g., "stone_0")
        rgba_bytes (bytes): Raw 16x16 RGBA pixel data (1024 bytes total)

    Raises:
        ValueError: If the data is not bytes or the length is not 1024.

    Example:
        >>> add_procedural_texture("stone_variant", image.tobytes())
    """
    if not isinstance(rgba_bytes, (bytes, bytearray)):
        raise ValueError("rgba_bytes must be a bytes-like object")

    if len(rgba_bytes) != 16 * 16 * 4:
        raise ValueError("Procedural textures must be 16x16 RGBA (1024 bytes)")

    api = get_mod_api()
    api.addProceduralTexture(name, bytes(rgba_bytes))


# ========== Config Access ==========

def get_mod_config(mod_id):
    """
    Retrieves a mod's configuration JSON and returns it as a Python dictionary.

    Args:
        mod_id (str): Mod identifier from mod.json (e.g., "skin_generator")

    Returns:
        dict: Parsed configuration dictionary, or empty dict if not found.
    """
    api = get_mod_api()
    config_json = api.getModConfig(mod_id)
    if not config_json:
        return {}

    try:
        return json.loads(config_json)
    except json.JSONDecodeError:
        log(f"Failed to parse config JSON for mod '{mod_id}'")
        return {}


# ========== NPC Helpers ==========

def spawn_npc(npc_id, name, x, y, z, personality):
    """
    Announces an NPC spawn to the Java side. Entity creation is handled later.

    Args:
        npc_id (int): Unique NPC identifier
        name (str): NPC display name
        x (float): World X coordinate
        y (float): World Y coordinate
        z (float): World Z coordinate
        personality (str): Short personality description
    """
    api = get_mod_api()
    api.spawnNPC(npc_id, name, float(x), float(y), float(z), personality)


def despawn_npc(npc_id):
    """
    Removes an NPC registration from the Java side.

    Args:
        npc_id (int): Unique NPC identifier
    """
    api = get_mod_api()
    api.despawnNPC(npc_id)


def npc_say(npc_id, message):
    """
    Logs an NPC dialogue line via Java for debugging or chat forwarding.

    Args:
        npc_id (int): Unique NPC identifier
        message (str): Message spoken by NPC
    """
    api = get_mod_api()
    api.npcSay(npc_id, str(message))
