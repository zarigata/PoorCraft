"""
PoorCraft Event System - Decorators and event handling for Python mods.

This module provides decorators for registering event callbacks and
classes for wrapping Java event objects.

Example usage:
    from poorcraft import events
    
    @events.on_block_place
    def handle_block_place(event):
        print(f"Block placed at {event.x}, {event.y}, {event.z}")
        if event.block_type_id == 14:  # Bedrock
            event.cancel()
"""

from poorcraft.api import get_mod_api

# Global event handlers registry
_event_handlers = {}


# ========== Event Decorators ==========

def on_block_place(func):
    """
    Decorator that registers a function for block_place events.
    
    The decorated function will be called when a block is placed.
    Function should accept one argument: the event object.
    
    Example:
        @on_block_place
        def my_handler(event):
            print(f"Block {event.block_type_id} placed at ({event.x}, {event.y}, {event.z})")
    
    Args:
        func: Function to register
    
    Returns:
        The original function (decorator pattern)
    """
    register_event("block_place", func)
    return func


def on_block_break(func):
    """
    Decorator that registers a function for block_break events.
    
    The decorated function will be called when a block is broken.
    
    Args:
        func: Function to register
    
    Returns:
        The original function
    """
    register_event("block_break", func)
    return func


def on_player_join(func):
    """
    Decorator that registers a function for player_join events.
    
    The decorated function will be called when a player joins the server.
    
    Args:
        func: Function to register
    
    Returns:
        The original function
    """
    register_event("player_join", func)
    return func


def on_player_leave(func):
    """
    Decorator that registers a function for player_leave events.
    
    The decorated function will be called when a player leaves the server.
    
    Args:
        func: Function to register
    
    Returns:
        The original function
    """
    register_event("player_leave", func)
    return func


def on_chunk_generate(func):
    """
    Decorator that registers a function for chunk_generate events.
    
    The decorated function will be called when a chunk is generated.
    The event provides access to the chunk for modification.
    
    Args:
        func: Function to register
    
    Returns:
        The original function
    """
    register_event("chunk_generate", func)
    return func


def on_world_load(func):
    """
    Decorator that registers a function for world_load events.
    
    The decorated function will be called when a world is loaded.
    
    Args:
        func: Function to register
    
    Returns:
        The original function
    """
    register_event("world_load", func)
    return func


# ========== Event Registration Functions ==========

def register_event(event_name, callback):
    """
    Registers a callback for the specified event.
    
    Args:
        event_name (str): Name of the event (e.g., "block_place")
        callback (callable): Function to call when event fires
    """
    # Add to local registry
    if event_name not in _event_handlers:
        _event_handlers[event_name] = []
    _event_handlers[event_name].append(callback)
    
    # Register with Java EventBus
    api = get_mod_api()
    
    # Create a wrapper that calls the Python callback
    # This is needed because Py4J needs a callable object
    class EventCallback:
        def __init__(self, func):
            self.func = func
        
        def __call__(self, event):
            # Call the Python function with the event
            self.func(event)
        
        class Java:
            implements = ["java.util.function.Consumer"]
    
    wrapper = EventCallback(callback)
    api.registerEvent(event_name, wrapper)


def unregister_event(event_name, callback):
    """
    Unregisters a callback for the specified event.
    
    Args:
        event_name (str): Name of the event
        callback (callable): Function to unregister
    """
    # Remove from local registry
    if event_name in _event_handlers:
        if callback in _event_handlers[event_name]:
            _event_handlers[event_name].remove(callback)
    
    # Unregister from Java EventBus
    api = get_mod_api()
    api.unregisterEvent(event_name, callback)


# ========== Event Wrapper Class ==========

class EventWrapper:
    """
    Wraps Java event objects for easier Python access.
    
    Provides Pythonic property access to event fields and methods.
    """
    
    def __init__(self, java_event):
        """
        Creates a new event wrapper.
        
        Args:
            java_event: The Java event object (Py4J proxy)
        """
        self._event = java_event
    
    @property
    def x(self):
        """X coordinate (for block/position events)"""
        return self._event.getX() if hasattr(self._event, 'getX') else None
    
    @property
    def y(self):
        """Y coordinate (for block/position events)"""
        return self._event.getY() if hasattr(self._event, 'getY') else None
    
    @property
    def z(self):
        """Z coordinate (for block/position events)"""
        return self._event.getZ() if hasattr(self._event, 'getZ') else None
    
    @property
    def block_type_id(self):
        """Block type ID (for block events)"""
        return self._event.getBlockTypeId() if hasattr(self._event, 'getBlockTypeId') else None
    
    @property
    def player_id(self):
        """Player ID (for player/block events)"""
        return self._event.getPlayerId() if hasattr(self._event, 'getPlayerId') else None
    
    @property
    def username(self):
        """Player username (for player events)"""
        return self._event.getUsername() if hasattr(self._event, 'getUsername') else None
    
    @property
    def reason(self):
        """Disconnect reason (for player leave events)"""
        return self._event.getReason() if hasattr(self._event, 'getReason') else None
    
    @property
    def chunk_x(self):
        """Chunk X coordinate (for chunk events)"""
        return self._event.getChunkX() if hasattr(self._event, 'getChunkX') else None
    
    @property
    def chunk_z(self):
        """Chunk Z coordinate (for chunk events)"""
        return self._event.getChunkZ() if hasattr(self._event, 'getChunkZ') else None
    
    @property
    def chunk(self):
        """Chunk object (for chunk events)"""
        return self._event.getChunk() if hasattr(self._event, 'getChunk') else None
    
    @property
    def seed(self):
        """World seed (for world events)"""
        return self._event.getSeed() if hasattr(self._event, 'getSeed') else None
    
    @property
    def generate_structures(self):
        """Whether structures are enabled (for world events)"""
        return self._event.isGenerateStructures() if hasattr(self._event, 'isGenerateStructures') else None
    
    def cancel(self):
        """Cancels the event (if cancellable)"""
        if hasattr(self._event, 'setCancelled'):
            self._event.setCancelled(True)
    
    def is_cancelled(self):
        """
        Checks if the event is cancelled.
        
        Returns:
            bool: True if cancelled, False otherwise
        """
        if hasattr(self._event, 'isCancelled'):
            return self._event.isCancelled()
        return False
