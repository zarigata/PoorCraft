/**
 * Rendering systems for 3D world and 2D UI.
 * 
 * <p>This package contains all rendering components:</p>
 * <ul>
 *   <li>{@link com.poorcraft.render.ChunkRenderer} - Voxel world rendering with greedy meshing</li>
 *   <li>{@link com.poorcraft.render.SkyRenderer} - Skybox and day/night cycle</li>
 *   <li>{@link com.poorcraft.render.BlockHighlightRenderer} - Block selection outline</li>
 *   <li>{@link com.poorcraft.render.ItemDropRenderer} - Dropped item rendering</li>
 *   <li>{@link com.poorcraft.render.BlurRenderer} - Post-processing blur effect</li>
 *   <li>{@link com.poorcraft.render.Shader} - GLSL shader management</li>
 *   <li>{@link com.poorcraft.render.TextureAtlas} - Block texture atlas</li>
 * </ul>
 * 
 * <h2>Chunk Rendering:</h2>
 * <p>The {@link com.poorcraft.render.ChunkRenderer} uses greedy meshing to reduce
 * triangle count by 60-80%. Only visible faces are rendered (face culling), and
 * frustum culling skips off-screen chunks entirely.</p>
 * 
 * <h2>Lighting System:</h2>
 * <p>Lighting combines ambient and directional sun light. The sun position
 * changes based on time of day (600-second cycle). Block faces are shaded
 * based on their normal direction for depth perception.</p>
 * 
 * <h2>Texture System:</h2>
 * <p>All block textures (16x16) are combined into a single texture atlas to
 * minimize texture switches. The atlas is shared across all chunks for
 * efficient GPU memory usage.</p>
 * 
 * <h2>Shaders:</h2>
 * <p>GLSL 330 shaders handle vertex transformation and fragment shading.
 * Shaders are located in {@code src/main/resources/shaders/}:</p>
 * <ul>
 *   <li>block.vert/frag - Chunk rendering</li>
 *   <li>sky.vert/frag - Skybox rendering</li>
 *   <li>ui.vert/frag - 2D UI rendering</li>
 *   <li>blur.vert/frag - Post-processing blur</li>
 * </ul>
 * 
 * <h2>Performance Optimizations:</h2>
 * <ul>
 *   <li>Greedy meshing reduces geometry</li>
 *   <li>Frustum culling skips invisible chunks</li>
 *   <li>GPU instancing for repeated geometry</li>
 *   <li>Texture atlas reduces state changes</li>
 *   <li>Mesh caching (only rebuild on changes)</li>
 * </ul>
 * 
 * @see com.poorcraft.render.ChunkRenderer
 * @see com.poorcraft.render.SkyRenderer
 * @since 1.0.0
 */
package com.poorcraft.render;
