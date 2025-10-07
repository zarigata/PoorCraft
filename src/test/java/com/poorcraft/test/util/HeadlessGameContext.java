package com.poorcraft.test.util;

import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;
import com.poorcraft.core.Window;
import com.poorcraft.modding.LuaModLoader;
import com.poorcraft.render.ChunkRenderer;
import com.poorcraft.render.GPUCapabilities;
import com.poorcraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.glfwHideWindow;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL11.glViewport;

/**
 * Provides a reusable testing harness that boots the PoorCraft engine in a
 * headless configuration. The context can selectively initialise subsystems
 * (window, game, renderer, mods, world) and guarantees they are torn down when
 * {@link #cleanup()} is invoked.
 */
public class HeadlessGameContext implements AutoCloseable {

    private Settings settings;
    private ConfigManager configManager;
    private Window window;
    private Game game;
    private LuaModLoader modLoader;
    private ChunkRenderer chunkRenderer;
    private World world;
    private GPUCapabilities gpuCapabilities;
    private boolean initialized;
    private final Set<String> subsystems = new HashSet<>();

    /**
     * Prepares the JVM for headless execution and configures default settings.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        System.setProperty("java.awt.headless", "true");
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW for headless tests");
        }

        this.settings = TestUtils.createTestSettings();
        this.configManager = createTestingConfigManager();
        initialized = true;
    }

    /**
     * Lazily initialises one of the supported subsystems. Valid names are:
     * window, game, renderer, mods, world.
     *
     * @param name subsystem identifier (case-insensitive)
     */
    public synchronized void initializeSubsystem(String name) {
        Objects.requireNonNull(name, "Subsystem name must not be null");
        ensureInitialized();

        String key = name.toLowerCase(Locale.ROOT).trim();
        switch (key) {
            case "window":
                ensureWindow();
                break;
            case "game":
                ensureGame();
                break;
            case "renderer":
                ensureRenderer();
                break;
            case "mods":
                ensureModLoader();
                break;
            case "world":
                ensureWorld();
                break;
            default:
                throw new IllegalArgumentException("Unsupported subsystem: " + name);
        }
        subsystems.add(key);
    }

    /**
     * @return {@code true} if the subsystem with the given name has been initialised.
     */
    public synchronized boolean isSubsystemInitialized(String name) {
        return subsystems.contains(name.toLowerCase(Locale.ROOT).trim());
    }

    /**
     * Returns the active settings used by the context.
     */
    public Settings getSettings() {
        ensureInitialized();
        return settings;
    }

    /**
     * Returns the hidden testing window (when initialised).
     */
    public Window getWindow() {
        return window;
    }

    /**
     * Returns the running {@link Game} instance (when initialised).
     */
    public Game getGame() {
        return game;
    }

    /**
     * Returns the mod loader (when initialised).
     */
    public LuaModLoader getModLoader() {
        return modLoader;
    }

    /**
     * Returns the chunk renderer (when initialised).
     */
    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }

    /**
     * Returns a lightweight world instance used for deterministic tests.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Returns detected GPU capabilities once an OpenGL context is available.
     */
    public GPUCapabilities getGpuCapabilities() {
        return gpuCapabilities;
    }

    /**
     * Releases all resources associated with the context. Safe to call multiple times.
     */
    public synchronized void cleanup() {
        if (!initialized) {
            return;
        }

        try {
            if (chunkRenderer != null) {
                try {
                    chunkRenderer.cleanup();
                } catch (Exception ignored) {
                    // We do not let cleanup failures break the test shutdown sequence
                }
                chunkRenderer = null;
            }

            if (modLoader != null) {
                try {
                    modLoader.shutdown();
                } catch (Exception ignored) {
                }
                modLoader = null;
            }

            if (game != null) {
                try {
                    if (game.getWindow() != null) {
                        game.getWindow().destroy();
                    }
                } catch (Exception ignored) {
                }
                game = null;
            } else if (window != null) {
                try {
                    window.destroy();
                } catch (Exception ignored) {
                }
            } else {
            }
        } finally {
            try {
                GLFW.glfwTerminate();
            } catch (Throwable ignored) {
                // Ignore termination failures during test shutdown
            }
            try {
                GL.setCapabilities(null);
            } catch (Throwable ignored) {
                // Ignore capability reset failures
            }
            subsystems.clear();
            window = null;
            world = null;
            gpuCapabilities = null;
            initialized = false;
        }
    }

    @Override
    public void close() {
        cleanup();
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("HeadlessGameContext.initialize() must be called first");
        }
    }

    private void ensureWindow() {
        if (window != null && game == null) {
            return;
        }
        if (game != null) {
            // Game already created its own window
            window = game.getWindow();
            return;
        }

        window = new Window(settings.window.width, settings.window.height, settings.window.title, settings.window.vsync);
        window.create();
        glfwHideWindow(window.getHandle());
        glfwMakeContextCurrent(window.getHandle());
        GL.setCapabilities(null);
        GL.createCapabilities();
        glViewport(0, 0, settings.window.width, settings.window.height);
        queryGpuCapabilities();
    }

    private void ensureGame() {
        if (game != null) {
            return;
        }

        // Recreate settings to avoid mutations from earlier tests
        this.settings = TestUtils.createTestSettings();
        game = new Game(settings, configManager);
        game.init();
        if (game.getWindow() != null) {
            glfwHideWindow(game.getWindow().getHandle());
            glfwMakeContextCurrent(game.getWindow().getHandle());
            GL.setCapabilities(null);
            GL.createCapabilities();
            glViewport(0, 0, game.getWindow().getWidth(), game.getWindow().getHeight());
        }
        window = game.getWindow();
        modLoader = game.getModLoader();
        chunkRenderer = game.getChunkRenderer();
        queryGpuCapabilities();
    }

    private void ensureRenderer() {
        if (chunkRenderer != null) {
            return;
        }
        if (game != null) {
            chunkRenderer = game.getChunkRenderer();
            if (chunkRenderer != null) {
                return;
            }
        }
        if (modLoader == null) {
            ensureModLoader();
            if (chunkRenderer != null) {
                return;
            }
        }
        ensureWindow();
        chunkRenderer = new ChunkRenderer();
        chunkRenderer.setSettings(settings);
        chunkRenderer.setModLoader(modLoader);
        if (gpuCapabilities == null) {
            queryGpuCapabilities();
        }
        chunkRenderer.setGPUCapabilities(gpuCapabilities);
        chunkRenderer.init();
    }

    private void ensureModLoader() {
        if (modLoader != null) {
            return;
        }
        ensureGame();
        modLoader = game.getModLoader();
    }

    private void ensureWorld() {
        if (world != null) {
            return;
        }
        world = TestUtils.createTestWorld(1234L);
    }

    private void queryGpuCapabilities() {
        try {
            if (gpuCapabilities == null) {
                gpuCapabilities = GPUCapabilities.detect();
            }
        } catch (Throwable ignored) {
            // GPU detection may fail in some CI environments; allow tests to continue.
        }
    }

    private ConfigManager createTestingConfigManager() {
        return new ConfigManager() {
            @Override
            public Settings loadSettings() {
                // Ensure the config manager returns the same settings object used by the tests
                Settings loaded = TestUtils.createTestSettings();
                return loaded;
            }
        };
    }
}
