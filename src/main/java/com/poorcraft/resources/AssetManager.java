package com.poorcraft.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Centralized manager for user-provided and bundled asset locations.
 * Provides helpers for resolving skin paths and copying skin files.
 */
public final class AssetManager {

    public static final String SKINS_DIR = "skins";
    public static final String DEFAULT_SKINS_DIR = "skins/default";
    public static final String RESOURCEPACKS_DIR = "resourcepacks";
    public static final String SCREENSHOTS_DIR = "screenshots";
    public static final String WORLDS_DIR = "worlds";
    public static final String CONFIG_DIR = "config";

    private static final AssetManager INSTANCE = new AssetManager();

    private AssetManager() {
    }

    public static AssetManager getInstance() {
        return INSTANCE;
    }

    public Path getSkinPath(String skinName) {
        Objects.requireNonNull(skinName, "skinName");
        return resolveUnder(SKINS_DIR, ensurePngExtension(skinName));
    }

    public Path getDefaultSkinPath(String skinName) {
        Objects.requireNonNull(skinName, "skinName");
        return resolveUnder(DEFAULT_SKINS_DIR, ensurePngExtension(skinName));
    }

    public boolean skinExists(String skinName) {
        Path skinPath = getSkinPath(skinName);
        return Files.exists(skinPath);
    }

    public List<Path> listSkins() {
        Path root = Paths.get(SKINS_DIR);
        if (!Files.exists(root)) {
            return Collections.emptyList();
        }
        try {
            return Files.list(root)
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".png"))
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            System.err.println("[AssetManager] Failed to list skins: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void copySkinToUserDirectory(Path source, String targetName) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetName, "targetName");
        Path target = getSkinPath(targetName);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public InputStream openBundledResource(String classpathLocation) {
        InputStream stream = AssetManager.class.getResourceAsStream(classpathLocation);
        if (stream == null) {
            System.err.println("[AssetManager] Resource not found on classpath: " + classpathLocation);
        }
        return stream;
    }

    private Path resolveUnder(String base, String child) {
        Path basePath = Paths.get(base);
        Path resolved = basePath.resolve(child);
        if (!resolved.normalize().startsWith(basePath.normalize())) {
            throw new IllegalArgumentException("Illegal path traversal attempt for base " + base);
        }
        return resolved;
    }

    private String ensurePngExtension(String skinName) {
        if (!skinName.toLowerCase().endsWith(".png")) {
            return skinName + ".png";
        }
        return skinName;
    }
}
