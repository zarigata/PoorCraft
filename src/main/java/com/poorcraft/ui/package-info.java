/**
 * User interface system with Vaporwave aesthetic.
 * 
 * <p>This package handles all UI rendering and state management:</p>
 * <ul>
 *   <li>{@link com.poorcraft.ui.UIManager} - Central UI controller and state machine</li>
 *   <li>{@link com.poorcraft.ui.UIRenderer} - 2D rendering with orthographic projection</li>
 *   <li>{@link com.poorcraft.ui.FontRenderer} - Text rendering with STB TrueType</li>
 *   <li>{@link com.poorcraft.ui.UIScreen} - Base class for all screens</li>
 *   <li>{@link com.poorcraft.ui.GameState} - Game state enumeration</li>
 * </ul>
 * 
 * <h2>Screen Types:</h2>
 * <ul>
 *   <li>{@link com.poorcraft.ui.MainMenuScreen} - Main menu with Vaporwave design</li>
 *   <li>{@link com.poorcraft.ui.PauseScreen} - Pause menu with blur effect</li>
 *   <li>{@link com.poorcraft.ui.SettingsScreen} - Graphics/controls/audio settings</li>
 *   <li>{@link com.poorcraft.ui.WorldCreationScreen} - World seed and options</li>
 *   <li>{@link com.poorcraft.ui.MultiplayerMenuScreen} - Server list and connect</li>
 *   <li>{@link com.poorcraft.ui.InventoryScreen} - Inventory management</li>
 *   <li>{@link com.poorcraft.ui.HUD} - In-game heads-up display</li>
 * </ul>
 * 
 * <h2>Design System:</h2>
 * <p>The UI uses a Vaporwave aesthetic with:</p>
 * <ul>
 *   <li>Gradient backgrounds (pink/purple → cyan/blue)</li>
 *   <li>Animated glow effects on buttons</li>
 *   <li>Silkscreen retro font</li>
 *   <li>Scanline animations for CRT effect</li>
 *   <li>Responsive layout (adapts to any window size)</li>
 * </ul>
 * 
 * <h2>State Machine:</h2>
 * <p>The {@link com.poorcraft.ui.UIManager} manages game state transitions:
 * MAIN_MENU → WORLD_CREATION → IN_GAME → PAUSED, etc. Each state has an
 * associated screen that handles rendering and input.</p>
 * 
 * <h2>Input Routing:</h2>
 * <p>Input events (mouse, keyboard) are routed to the active screen based on
 * the current game state. The UI manager handles cursor grabbing/releasing
 * automatically when transitioning between menu and gameplay states.</p>
 * 
 * @see com.poorcraft.ui.UIManager
 * @see com.poorcraft.ui.UIRenderer
 * @since 1.0.0
 */
package com.poorcraft.ui;
