package com.poorcraft.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * GLFW window wrapper with OpenGL context.
 * Handles window creation, event polling, and basic OpenGL setup.
 * 
 * This is basically the "hello window" tutorial but fancier.
 */
public class Window {
    
    private long windowHandle;
    private int width;
    private int height;
    private String title;
    private boolean vsync;
    
    /**
     * Creates a new window configuration.
     * Window is not created until create() is called.
     * 
     * @param width Window width in pixels
     * @param height Window height in pixels
     * @param title Window title
     * @param vsync Enable vertical sync
     */
    public Window(int width, int height, String title, boolean vsync) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.vsync = vsync;
    }
    
    /**
     * Creates and initializes the GLFW window with OpenGL context.
     * Sets up error callbacks, window hints, and OpenGL capabilities.
     * 
     * This method does a LOT. Like, seriously, it's doing all the heavy lifting.
     */
    public void create() {
        // Set up error callback - prints errors to System.err
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW. Did you install your graphics drivers?");
        }
        
        // Configure window hints
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);  // Hidden until we're ready
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);  // macOS compatibility
        
        // Create window
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create GLFW window. RIP.");
        }
        
        // Set up framebuffer size callback for window resizing
        // This ensures the viewport matches the window size when resized
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
        });
        
        // Center window on primary monitor
        // Because off-center windows are for psychopaths
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(
                windowHandle,
                (vidMode.width() - width) / 2,
                (vidMode.height() - height) / 2
            );
        }
        
        // Make OpenGL context current
        glfwMakeContextCurrent(windowHandle);
        
        // Set vsync
        glfwSwapInterval(vsync ? 1 : 0);
        
        // Show window
        glfwShowWindow(windowHandle);
        
        // Initialize OpenGL capabilities
        // This is LWJGL magic that makes OpenGL functions work
        GL.createCapabilities();
        
        // Set clear color to sky blue (like Minecraft alpha, good times)
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
        
        // Enable depth testing for 3D rendering
        glEnable(GL_DEPTH_TEST);
        
        System.out.println("[Window] Created " + width + "x" + height + " window with OpenGL " + 
                          glGetString(GL_VERSION));
    }
    
    /**
     * Updates the window by swapping buffers and polling events.
     * Call this once per frame after rendering.
     */
    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }
    
    /**
     * Checks if the window should close (user clicked X or pressed Alt+F4).
     * 
     * @return true if window should close
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    
    /**
     * Destroys the window and terminates GLFW.
     * Call this when shutting down the game.
     */
    public void destroy() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
        
        System.out.println("[Window] Destroyed");
    }
    
    /**
     * Returns the GLFW window handle.
     * Needed for input callbacks and other GLFW functions.
     * 
     * @return Window handle (long pointer)
     */
    public long getHandle() {
        return windowHandle;
    }
    
    /**
     * Returns current window width.
     * Updated automatically on resize.
     * 
     * @return Width in pixels
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Returns current window height.
     * Updated automatically on resize.
     * 
     * @return Height in pixels
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Enables or disables vertical sync.
     * 
     * @param vsync true to enable vsync, false to disable
     */
    public void setVsync(boolean vsync) {
        this.vsync = vsync;
        glfwSwapInterval(vsync ? 1 : 0);
    }
    
    /**
     * Sets the window size.
     * 
     * @param width New width in pixels
     * @param height New height in pixels
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        glfwSetWindowSize(windowHandle, width, height);
        System.out.println("[Window] Resized to " + width + "x" + height);
    }
    
    /**
     * Sets fullscreen mode.
     * 
     * @param fullscreen True for fullscreen, false for windowed
     */
    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            // Get primary monitor
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(monitor);
            
            if (vidMode != null) {
                // Set windowed fullscreen
                glfwSetWindowMonitor(windowHandle, monitor, 0, 0, 
                    vidMode.width(), vidMode.height(), vidMode.refreshRate());
                this.width = vidMode.width();
                this.height = vidMode.height();
                System.out.println("[Window] Switched to fullscreen: " + width + "x" + height);
            }
        } else {
            // Set windowed mode
            glfwSetWindowMonitor(windowHandle, NULL, 100, 100, width, height, 0);
            
            // Center window
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(
                    windowHandle,
                    (vidMode.width() - width) / 2,
                    (vidMode.height() - height) / 2
                );
            }
            System.out.println("[Window] Switched to windowed mode: " + width + "x" + height);
        }
    }
}
