"""
PoorCraft Entity API - Python wrappers for entity management.

This module provides classes for managing entities, players, and NPCs.
Currently a placeholder for future expansion.

Example usage:
    from poorcraft import entity
    
    # Get all online players
    players = entity.get_players()
    
    # Spawn an NPC
    npc = entity.NPC()
    npc.spawn(100, 70, 200)
"""

from poorcraft.api import get_mod_api


# ========== Entity Base Class ==========

class Entity:
    """
    Base class for all entities.
    
    Represents an entity in the game world with position and rotation.
    """
    
    def __init__(self, entity_id=None):
        """
        Creates a new entity.
        
        Args:
            entity_id (int): Unique entity ID, or None for new entity
        """
        self.entity_id = entity_id
        self.x = 0.0
        self.y = 0.0
        self.z = 0.0
        self.yaw = 0.0
        self.pitch = 0.0
    
    def get_position(self):
        """
        Gets the entity's position.
        
        Returns:
            tuple: (x, y, z) coordinates
        """
        return (self.x, self.y, self.z)
    
    def set_position(self, x, y, z):
        """
        Sets the entity's position.
        
        Args:
            x (float): X coordinate
            y (float): Y coordinate
            z (float): Z coordinate
        """
        self.x = x
        self.y = y
        self.z = z
    
    def get_rotation(self):
        """
        Gets the entity's rotation.
        
        Returns:
            tuple: (yaw, pitch) in degrees
        """
        return (self.yaw, self.pitch)
    
    def set_rotation(self, yaw, pitch):
        """
        Sets the entity's rotation.
        
        Args:
            yaw (float): Yaw rotation in degrees
            pitch (float): Pitch rotation in degrees
        """
        self.yaw = yaw
        self.pitch = pitch


# ========== Player Class ==========

class Player(Entity):
    """
    Represents a player entity.
    
    Extends Entity with player-specific properties and methods.
    """
    
    def __init__(self, player_id, username):
        """
        Creates a new player.
        
        Args:
            player_id (int): Unique player ID
            username (str): Player username
        """
        super().__init__(player_id)
        self.player_id = player_id
        self.username = username
    
    def send_message(self, message):
        """
        Sends a chat message to the player.
        
        Note: This is a placeholder for future implementation.
        
        Args:
            message (str): Message to send
        """
        # TODO: Implement when chat system is added
        print(f"[Player {self.username}] {message}")
    
    def teleport(self, x, y, z):
        """
        Teleports the player to the specified position.
        
        Note: This is a placeholder for future implementation.
        
        Args:
            x (float): X coordinate
            y (float): Y coordinate
            z (float): Z coordinate
        """
        # TODO: Implement when entity teleportation is added
        self.set_position(x, y, z)
        print(f"[Player {self.username}] Teleported to ({x}, {y}, {z})")


# ========== NPC Class ==========

class NPC(Entity):
    """
    Represents an AI NPC entity.
    
    Used by the ai_npc mod for creating AI-powered NPCs.
    """
    
    def __init__(self, personality="friendly"):
        """
        Creates a new NPC.
        
        Args:
            personality (str): NPC personality type
        """
        super().__init__()
        self.personality = personality
        self.conversation_context = {}
    
    def spawn(self, x, y, z):
        """
        Spawns the NPC at the specified position.
        
        Note: This is a placeholder for future implementation.
        
        Args:
            x (float): X coordinate
            y (float): Y coordinate
            z (float): Z coordinate
        """
        self.set_position(x, y, z)
        # TODO: Implement actual NPC spawning when entity system is added
        print(f"[NPC] Spawned at ({x}, {y}, {z}) with personality: {self.personality}")
    
    def despawn(self):
        """
        Removes the NPC from the world.
        
        Note: This is a placeholder for future implementation.
        """
        # TODO: Implement actual NPC despawning
        print(f"[NPC] Despawned")
    
    def say(self, message):
        """
        Makes the NPC say something.
        
        Note: This is a placeholder for future implementation.
        
        Args:
            message (str): Message to say
        """
        # TODO: Implement when chat/dialogue system is added
        print(f"[NPC] {message}")


# ========== Module Functions ==========

def get_players():
    """
    Gets a list of all online players.
    
    Note: This is a placeholder for future implementation.
    
    Returns:
        list: List of Player objects
    """
    # TODO: Implement when player tracking is added
    return []


def get_player_by_id(player_id):
    """
    Gets a player by their ID.
    
    Note: This is a placeholder for future implementation.
    
    Args:
        player_id (int): Player ID
    
    Returns:
        Player: Player object, or None if not found
    """
    # TODO: Implement when player tracking is added
    return None


def spawn_entity(entity_type, x, y, z):
    """
    Spawns an entity at the specified position.
    
    Note: This is a placeholder for future implementation.
    
    Args:
        entity_type (str): Type of entity to spawn
        x (float): X coordinate
        y (float): Y coordinate
        z (float): Z coordinate
    
    Returns:
        Entity: The spawned entity
    """
    # TODO: Implement when entity system is added
    entity = Entity()
    entity.set_position(x, y, z)
    print(f"[Entity] Spawned {entity_type} at ({x}, {y}, {z})")
    return entity
