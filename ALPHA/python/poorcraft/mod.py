"""
PoorCraft Mod Base Class - Base class for structured mod development.

This module provides a base class that mods can extend for structured
lifecycle management and configuration access.

Example usage:
    from poorcraft import BaseMod
    
    class MyMod(BaseMod):
        def init(self):
            self.log("Initializing MyMod")
            # Register events, load resources
        
        def enable(self):
            self.log("MyMod enabled")
            # Start mod functionality
        
        def disable(self):
            self.log("MyMod disabled")
            # Stop mod functionality
"""

from poorcraft.api import log as api_log


class BaseMod:
    """
    Base class for PoorCraft mods.
    
    Provides structured lifecycle management and utility methods.
    Mods can extend this class for a consistent interface.
    """
    
    def __init__(self, mod_id, mod_name, mod_version, config=None):
        """
        Creates a new mod instance.
        
        Args:
            mod_id (str): Unique mod identifier
            mod_name (str): Display name
            mod_version (str): Semantic version (e.g., "1.0.0")
            config (dict): Configuration dictionary from mod.json
        """
        self.mod_id = mod_id
        self.mod_name = mod_name
        self.mod_version = mod_version
        self.config = config or {}
        self.enabled = False
    
    def init(self):
        """
        Called when the mod is initialized.
        
        Override this method to perform setup:
        - Register event handlers
        - Load resources
        - Initialize data structures
        
        This is called once when the mod is loaded.
        """
        pass
    
    def enable(self):
        """
        Called when the mod is enabled.
        
        Override this method to start mod functionality:
        - Start background tasks
        - Enable features
        - Connect to external services
        
        This is called after init().
        """
        self.enabled = True
    
    def disable(self):
        """
        Called when the mod is disabled.
        
        Override this method to stop mod functionality:
        - Stop background tasks
        - Unregister event handlers
        - Save data
        - Disconnect from external services
        
        This is called when the game shuts down or mod is unloaded.
        """
        self.enabled = False
    
    def get_config(self, key, default=None):
        """
        Gets a configuration value by key.
        
        Args:
            key (str): Configuration key
            default: Default value if key doesn't exist
        
        Returns:
            The configuration value, or default if not found
        """
        return self.config.get(key, default)
    
    def log(self, message):
        """
        Logs a message with the mod name prefix.
        
        Args:
            message (str): Message to log
        """
        api_log(f"[{self.mod_name}] {message}")
    
    def is_enabled(self):
        """
        Checks if the mod is enabled.
        
        Returns:
            bool: True if enabled, False otherwise
        """
        return self.enabled
