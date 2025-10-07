/**
 * Core game systems and main game loop.
 * 
 * <p>This package contains the fundamental systems that drive the game:</p>
 * <ul>
 *   <li>{@link com.poorcraft.core.Game} - Main game class with update/render loop</li>
 *   <li>{@link com.poorcraft.core.Window} - GLFW window management</li>
 *   <li>{@link com.poorcraft.core.Timer} - Frame timing and delta time</li>
 *   <li>{@link com.poorcraft.core.PlayerController} - Player movement and physics</li>
 *   <li>{@link com.poorcraft.core.MiningSystem} - Block breaking mechanics</li>
 *   <li>{@link com.poorcraft.core.GameMode} - Game mode enumeration</li>
 * </ul>
 * 
 * <h2>Game Loop:</h2>
 * <p>The game runs an uncapped update/render loop (vsync optional):</p>
 * <pre>
 * while (running) {
 *     timer.update();
 *     inputHandler.update();
 *     update(deltaTime);
 *     render();
 *     window.update();
 * }
 * </pre>
 * 
 * <h2>Subsystem Orchestration:</h2>
 * <p>The {@link com.poorcraft.core.Game} class coordinates all major subsystems:
 * world generation, chunk management, rendering, UI, networking, and modding.</p>
 * 
 * @see com.poorcraft.core.Game
 * @since 1.0.0
 */
package com.poorcraft.core;
