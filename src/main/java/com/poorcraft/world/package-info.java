/**
 * World generation, chunk management, and block systems.
 * 
 * <p>This package handles all aspects of the voxel world:</p>
 * <ul>
 *   <li>{@link com.poorcraft.world.World} - Main world container and block access</li>
 *   <li>{@link com.poorcraft.world.ChunkManager} - Dynamic chunk loading/unloading</li>
 *   <li>{@link com.poorcraft.world.block} - Block types and properties</li>
 *   <li>{@link com.poorcraft.world.chunk} - Chunk data structures</li>
 *   <li>{@link com.poorcraft.world.generation} - Procedural terrain generation</li>
 *   <li>{@link com.poorcraft.world.entity} - World entities (item drops, etc.)</li>
 * </ul>
 * 
 * <h2>World Structure:</h2>
 * <p>The world is divided into 16x128x16 chunks that are generated on-demand.
 * Chunks are loaded/unloaded based on player position to manage memory usage.</p>
 * 
 * <h2>Terrain Generation:</h2>
 * <p>Procedural generation uses Simplex noise for height maps and biome distribution.
 * Four biomes are supported: Desert, Snow, Jungle, and Plains. Each biome has
 * unique terrain features (trees, cacti, snow layers).</p>
 * 
 * <h2>Block System:</h2>
 * <p>Blocks are identified by numeric IDs (0-255). The world provides get/set
 * methods for block access at world coordinates, automatically handling chunk
 * boundaries and coordinate conversion.</p>
 * 
 * @see com.poorcraft.world.World
 * @see com.poorcraft.world.ChunkManager
 * @since 1.0.0
 */
package com.poorcraft.world;
