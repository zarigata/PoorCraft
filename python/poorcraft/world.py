"""
PoorCraft World API - Python wrappers for world access.

This module provides classes and functions for accessing and modifying
the game world, blocks, chunks, and biomes.

Example usage:
    from poorcraft import world
    
    # Get block at position
    block_id = world.get_world().get_block(100, 64, 200)
    
    # Set block
    world.get_world().set_block(100, 64, 200, world.BlockType.STONE)
"""

from poorcraft.api import get_mod_api


# ========== Block Type Constants ==========

class BlockType:
    """
    Block type constants matching Java BlockType enum.
    
    These IDs correspond to the block types in the game.
    """
    
    AIR = 0
    DIRT = 1
    STONE = 2
    GRASS = 3
    SAND = 4
    SANDSTONE = 5
    SNOW_BLOCK = 6
    ICE = 7
    JUNGLE_GRASS = 8
    JUNGLE_DIRT = 9
    WOOD = 10
    LEAVES = 11
    CACTUS = 12
    SNOW_LAYER = 13
    BEDROCK = 14
    
    # Block name mappings
    _ID_TO_NAME = {
        0: "AIR",
        1: "DIRT",
        2: "STONE",
        3: "GRASS",
        4: "SAND",
        5: "SANDSTONE",
        6: "SNOW_BLOCK",
        7: "ICE",
        8: "JUNGLE_GRASS",
        9: "JUNGLE_DIRT",
        10: "WOOD",
        11: "LEAVES",
        12: "CACTUS",
        13: "SNOW_LAYER",
        14: "BEDROCK"
    }
    
    _NAME_TO_ID = {v: k for k, v in _ID_TO_NAME.items()}
    
    @staticmethod
    def from_id(block_id):
        """
        Converts block ID to name.
        
        Args:
            block_id (int): Block type ID
        
        Returns:
            str: Block name, or "UNKNOWN" if invalid
        """
        return BlockType._ID_TO_NAME.get(block_id, "UNKNOWN")
    
    @staticmethod
    def to_id(name):
        """
        Converts block name to ID.
        
        Args:
            name (str): Block name
        
        Returns:
            int: Block type ID, or 0 (AIR) if invalid
        """
        return BlockType._NAME_TO_ID.get(name.upper(), 0)


# ========== Block Class ==========

class Block:
    """
    Represents a block in the world.
    
    Provides convenient access to block properties and methods.
    """
    
    def __init__(self, x, y, z):
        """
        Creates a block reference at the specified position.
        
        Args:
            x (int): X coordinate
            y (int): Y coordinate
            z (int): Z coordinate
        """
        self.x = x
        self.y = y
        self.z = z
    
    @property
    def type_id(self):
        """Gets the block type ID"""
        api = get_mod_api()
        return api.getBlock(self.x, self.y, self.z)
    
    @property
    def type_name(self):
        """Gets the block type name"""
        return BlockType.from_id(self.type_id)
    
    def get(self):
        """
        Gets the block type at this position.
        
        Returns:
            int: Block type ID
        """
        return self.type_id
    
    def set(self, type_id):
        """
        Sets the block type at this position.
        
        Args:
            type_id (int): Block type ID to set
        """
        api = get_mod_api()
        api.setBlock(self.x, self.y, self.z, type_id)
    
    def get_biome(self):
        """
        Gets the biome at this block's position.
        
        Returns:
            str: Biome name
        """
        api = get_mod_api()
        return api.getBiome(self.x, self.z)


# ========== Chunk Class ==========

class Chunk:
    """
    Represents a 16x256x16 chunk.
    
    Provides methods for accessing and modifying blocks within the chunk.
    """
    
    CHUNK_SIZE = 16
    CHUNK_HEIGHT = 256
    
    def __init__(self, chunk_x, chunk_z, java_chunk=None):
        """
        Creates a chunk reference.
        
        Args:
            chunk_x (int): Chunk X coordinate
            chunk_z (int): Chunk Z coordinate
            java_chunk: Java chunk object (Py4J proxy), optional
        """
        self.chunk_x = chunk_x
        self.chunk_z = chunk_z
        self._java_chunk = java_chunk
    
    def get_block(self, local_x, local_y, local_z):
        """
        Gets block type at local chunk coordinates.
        
        Args:
            local_x (int): Local X (0-15)
            local_y (int): Local Y (0-255)
            local_z (int): Local Z (0-15)
        
        Returns:
            int: Block type ID
        """
        if self._java_chunk:
            return self._java_chunk.getBlock(local_x, local_y, local_z).getId()
        else:
            # Convert to world coordinates
            world_x = self.chunk_x * self.CHUNK_SIZE + local_x
            world_z = self.chunk_z * self.CHUNK_SIZE + local_z
            api = get_mod_api()
            return api.getBlock(world_x, local_y, world_z)
    
    def set_block(self, local_x, local_y, local_z, type_id):
        """
        Sets block type at local chunk coordinates.
        
        Args:
            local_x (int): Local X (0-15)
            local_y (int): Local Y (0-255)
            local_z (int): Local Z (0-15)
            type_id (int): Block type ID to set
        """
        if self._java_chunk:
            # Use Java BlockType.fromId to get BlockType object
            # This is a bit tricky with Py4J, so we'll use world coordinates instead
            pass
        
        # Convert to world coordinates and use ModAPI
        world_x = self.chunk_x * self.CHUNK_SIZE + local_x
        world_z = self.chunk_z * self.CHUNK_SIZE + local_z
        api = get_mod_api()
        api.setBlock(world_x, local_y, world_z, type_id)
    
    def fill(self, type_id):
        """
        Fills the entire chunk with the specified block type.
        
        Args:
            type_id (int): Block type ID to fill with
        """
        for x in range(self.CHUNK_SIZE):
            for y in range(self.CHUNK_HEIGHT):
                for z in range(self.CHUNK_SIZE):
                    self.set_block(x, y, z, type_id)
    
    def fill_layer(self, y, type_id):
        """
        Fills a horizontal layer at the specified Y level.
        
        Args:
            y (int): Y level (0-255)
            type_id (int): Block type ID to fill with
        """
        for x in range(self.CHUNK_SIZE):
            for z in range(self.CHUNK_SIZE):
                self.set_block(x, y, z, type_id)


# ========== World Class ==========

class World:
    """
    Represents the game world.
    
    Provides methods for accessing blocks, chunks, biomes, and terrain.
    """
    
    def __init__(self):
        """Creates a world instance."""
        pass
    
    def get_block(self, x, y, z):
        """
        Gets block type at world coordinates.
        
        Args:
            x (int): X coordinate
            y (int): Y coordinate
            z (int): Z coordinate
        
        Returns:
            int: Block type ID
        """
        api = get_mod_api()
        return api.getBlock(x, y, z)
    
    def set_block(self, x, y, z, type_id):
        """
        Sets block type at world coordinates.
        
        Args:
            x (int): X coordinate
            y (int): Y coordinate
            z (int): Z coordinate
            type_id (int): Block type ID to set
        """
        api = get_mod_api()
        api.setBlock(x, y, z, type_id)
    
    def get_biome(self, x, z):
        """
        Gets biome at coordinates.
        
        Args:
            x (int): X coordinate
            z (int): Z coordinate
        
        Returns:
            str: Biome name
        """
        api = get_mod_api()
        return api.getBiome(x, z)
    
    def get_height_at(self, x, z):
        """
        Gets terrain height at coordinates.
        
        Args:
            x (int): X coordinate
            z (int): Z coordinate
        
        Returns:
            int: Y coordinate of surface
        """
        api = get_mod_api()
        return api.getHeightAt(x, z)
    
    def get_chunk(self, chunk_x, chunk_z):
        """
        Gets a chunk object.
        
        Args:
            chunk_x (int): Chunk X coordinate
            chunk_z (int): Chunk Z coordinate
        
        Returns:
            Chunk: Chunk object
        """
        return Chunk(chunk_x, chunk_z)


# ========== Module Functions ==========

# Singleton world instance
_world_instance = None


def get_world():
    """
    Gets the world singleton instance.
    
    Returns:
        World: The world instance
    """
    global _world_instance
    if _world_instance is None:
        _world_instance = World()
    return _world_instance
