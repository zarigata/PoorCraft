package com.poorcraft.core;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

/**
 * Simple timing utility for delta time calculation.
 * 
 * Delta time = time elapsed since last frame.
 * Used for frame-rate independent movement and physics.
 * 
 * Fun fact: Without delta time, your game runs at different speeds on different PCs.
 * Ask me how I know. (Spoiler: I learned the hard way)
 */
public class Timer {
    
    private double lastTime;
    private double deltaTime;
    
    /**
     * Creates a new timer and initializes it to current time.
     */
    public Timer() {
        this.lastTime = glfwGetTime();
        this.deltaTime = 0.0;
    }
    
    /**
     * Updates the timer and calculates delta time since last update.
     * Call this once per frame at the start of your game loop.
     */
    public void update() {
        double currentTime = glfwGetTime();
        deltaTime = currentTime - lastTime;
        lastTime = currentTime;
    }
    
    /**
     * Returns delta time in seconds (double precision).
     * 
     * @return Time elapsed since last frame in seconds
     */
    public double getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * Returns delta time as float for convenience.
     * Most game math uses floats, so this saves casting everywhere.
     * 
     * @return Time elapsed since last frame in seconds (float)
     */
    public float getDeltaTimeFloat() {
        return (float) deltaTime;
    }
    
    /**
     * Returns total elapsed time since GLFW initialization.
     * Useful for animations and time-based effects.
     * 
     * @return Total time in seconds
     */
    public double getTime() {
        return glfwGetTime();
    }
}
