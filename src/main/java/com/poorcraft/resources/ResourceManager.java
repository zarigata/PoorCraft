package com.poorcraft.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource manager for loading assets from classpath and filesystem.
 * 
 * Loading priority:
 * 1. Classpath resources (bundled with game) - for defaults
 * 2. Filesystem resources (user directory) - for overrides/mods
 * 
 * Includes caching for text files because loading the same shader 1000 times is dumb.
 * Trust me, I've done it. It's dumb.
 */
public class ResourceManager {
    
    private static ResourceManager instance;
    
    private final Map<String, String> textCache;
    
    /**
     * Private constructor for singleton pattern.
     * Use getInstance() to get the instance.
     */
    private ResourceManager() {
        this.textCache = new HashMap<>();
    }
    
    /**
     * Returns the singleton ResourceManager instance.
     * 
     * @return ResourceManager instance
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }
    
    /**
     * Loads a text resource (shader, config, etc.) as a String.
     * Checks cache first, then classpath, then filesystem.
     * 
     * @param path Resource path (e.g., "/shaders/vertex.glsl")
     * @return Resource content as String
     * @throws RuntimeException if resource not found
     */
    public String loadTextResource(String path) {
        // Check cache first
        if (textCache.containsKey(path)) {
            return textCache.get(path);
        }
        
        // Try classpath first (bundled resources)
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream != null) {
                byte[] bytes = stream.readAllBytes();
                String content = new String(bytes, StandardCharsets.UTF_8);
                textCache.put(path, content);
                return content;
            }
        } catch (IOException e) {
            // Fall through to filesystem attempt
        }
        
        // Try filesystem (user overrides/mods)
        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath, StandardCharsets.UTF_8);
                textCache.put(path, content);
                return content;
            }
        } catch (IOException e) {
            // Fall through to error
        }
        
        throw new RuntimeException("Resource not found: " + path + " (checked classpath and filesystem)");
    }
    
    /**
     * Loads a resource as an InputStream.
     * Does not cache (streams can only be read once).
     * 
     * @param path Resource path
     * @return InputStream for the resource
     * @throws RuntimeException if resource not found
     */
    public InputStream loadResourceStream(String path) {
        // Try classpath first
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            return stream;
        }
        
        // Try filesystem
        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                return Files.newInputStream(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource stream: " + path, e);
        }
        
        throw new RuntimeException("Resource not found: " + path);
    }
    
    /**
     * Checks if a resource exists in classpath or filesystem.
     * 
     * @param path Resource path
     * @return true if resource exists
     */
    public boolean resourceExists(String path) {
        // Check classpath
        if (getClass().getResource(path) != null) {
            return true;
        }
        
        // Check filesystem
        return Files.exists(Paths.get(path));
    }
    
    /**
     * Clears the text resource cache.
     * Useful for hot-reloading during development.
     */
    public void clearCache() {
        textCache.clear();
        System.out.println("[ResourceManager] Cache cleared");
    }
    
    /**
     * Constructs an absolute path from a relative path.
     * 
     * @param relativePath Relative path
     * @return Absolute path (or original if it starts with "/")
     */
    public String getResourcePath(String relativePath) {
        if (relativePath.startsWith("/")) {
            return relativePath;  // Already a classpath resource path
        }
        return Paths.get(relativePath).toAbsolutePath().toString();
    }
}
