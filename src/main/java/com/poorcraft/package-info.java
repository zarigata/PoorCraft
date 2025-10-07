/**
 * PoorCraft - An open-source voxel sandbox game with Lua modding.
 * 
 * <p>PoorCraft is a Minecraft-inspired voxel game built with Java, LWJGL3, and Lua.
 * It features infinite procedurally generated worlds, multiplayer support, and a
 * powerful Lua-based modding system.</p>
 * 
 * <h2>Main Packages:</h2>
 * <ul>
 *   <li>{@link com.poorcraft.core} - Core game loop and systems</li>
 *   <li>{@link com.poorcraft.world} - World generation and management</li>
 *   <li>{@link com.poorcraft.render} - Rendering systems (chunks, sky, UI)</li>
 *   <li>{@link com.poorcraft.network} - Multiplayer networking</li>
 *   <li>{@link com.poorcraft.modding} - Lua modding system</li>
 *   <li>{@link com.poorcraft.ui} - User interface and menus</li>
 * </ul>
 * 
 * <h2>Architecture:</h2>
 * <p>The game follows a component-based architecture with clear separation of concerns.
 * The main game loop orchestrates all subsystems (world, rendering, input, networking).
 * Mods can hook into the event system to extend gameplay without modifying core code.</p>
 * 
 * <h2>Getting Started:</h2>
 * <p>Entry point: {@link com.poorcraft.Main#main(String[])}</p>
 * <p>Main game class: {@link com.poorcraft.core.Game}</p>
 * 
 * @see com.poorcraft.Main
 * @see com.poorcraft.core.Game
 * @version 2.0.0
 * @since 1.0.0
 */
package com.poorcraft;
