/**
 * Lua-based modding system with event-driven architecture.
 * 
 * <p>This package provides a complete modding API for extending PoorCraft:</p>
 * <ul>
 *   <li>{@link com.poorcraft.modding.LuaModLoader} - Loads and manages Lua mods</li>
 *   <li>{@link com.poorcraft.modding.LuaModContainer} - Holds individual mod state</li>
 *   <li>{@link com.poorcraft.modding.LuaModAPI} - Exposes game API to Lua</li>
 *   <li>{@link com.poorcraft.modding.EventBus} - Event pub/sub system</li>
 *   <li>{@link com.poorcraft.modding.events} - Event type definitions</li>
 * </ul>
 * 
 * <h2>Mod Structure:</h2>
 * <p>Mods live in {@code gamedata/mods/} with this structure:</p>
 * <pre>
 * my_mod/
 * ├── mod.json    # Metadata and configuration
 * └── main.lua    # Main mod code
 * </pre>
 * 
 * <h2>Mod Lifecycle:</h2>
 * <p>Mods implement these lifecycle methods:</p>
 * <ul>
 *   <li>{@code init()} - Called once on mod load</li>
 *   <li>{@code enable()} - Called when mod is enabled</li>
 *   <li>{@code disable()} - Called when mod is disabled</li>
 *   <li>{@code update(deltaTime)} - Called every frame (optional)</li>
 * </ul>
 * 
 * <h2>Event System:</h2>
 * <p>Mods register event handlers to hook into game events:</p>
 * <pre>
 * api.register_event('block_place', function(event)
 *     api.log("Block placed at " .. event.x .. ", " .. event.y)
 * end)
 * </pre>
 * 
 * <h2>Available Events:</h2>
 * <ul>
 *   <li>block_place, block_break - Block modifications</li>
 *   <li>player_join, player_leave - Player connections</li>
 *   <li>chunk_generate - Chunk generation</li>
 *   <li>world_load - World initialization</li>
 * </ul>
 * 
 * <h2>Lua API:</h2>
 * <p>The {@code api} global object provides 15+ functions for world access,
 * time control, player position, logging, and more. See the modding guide
 * for complete API documentation.</p>
 * 
 * @see com.poorcraft.modding.LuaModLoader
 * @see com.poorcraft.modding.LuaModAPI
 * @see com.poorcraft.modding.EventBus
 * @since 2.0.0
 */
package com.poorcraft.modding;
