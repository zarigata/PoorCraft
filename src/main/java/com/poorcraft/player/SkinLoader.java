package com.poorcraft.player;

import com.poorcraft.resources.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.nio.file.StandardCopyOption;

/**
 * Utility responsible for loading player skins from user directories and bundled resources.
 */
public class SkinLoader {

    private static final long MAX_SKIN_BYTES = 1024 * 1024; // 1 MB safety limit

    private final AssetManager assetManager;
    private final Map<Path, PlayerSkin> cache;

    public SkinLoader() {
        this.assetManager = AssetManager.getInstance();
        this.cache = new HashMap<>();
    }

    public PlayerSkin loadFromFile(Path filePath, boolean isDefault) {
        Objects.requireNonNull(filePath, "filePath");
        if (cache.containsKey(filePath)) {
            return cache.get(filePath);
        }
        try {
            validateSize(filePath);
            String fileName = filePath.getFileName().toString();
            String skinId = fileName.toLowerCase().endsWith(".png")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;
            PlayerSkin skin = new PlayerSkin(skinId, filePath, isDefault, !isDefault);
            if (skin.isValid()) {
                cache.put(filePath, skin);
                return skin;
            }
        } catch (Exception e) {
            System.err.println("[SkinLoader] Failed to load skin " + filePath + ": " + e.getMessage());
        }
        return null;
    }

    public PlayerSkin loadFromClasspath(String resourcePath, String skinId) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        Objects.requireNonNull(skinId, "skinId");
        Path destination = assetManager.getDefaultSkinPath(skinId);
        try (InputStream stream = assetManager.openBundledResource(resourcePath)) {
            if (stream == null) {
                System.err.println("[SkinLoader] Bundled resource missing: " + resourcePath);
                return null;
            }
            Files.createDirectories(destination.getParent());
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
            cache.remove(destination);
            return loadFromFile(destination, true);
        } catch (IOException e) {
            System.err.println("[SkinLoader] Failed to load resource skin " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    public List<PlayerSkin> loadAllUserSkins() {
        List<Path> files = assetManager.listSkins();
        if (files.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlayerSkin> skins = new ArrayList<>();
        for (Path file : files) {
            PlayerSkin skin = loadFromFile(file, false);
            if (skin != null) {
                skins.add(skin);
            }
        }
        return skins;
    }

    public List<PlayerSkin> loadAllDefaultSkins(List<String> defaultIds) {
        if (defaultIds == null || defaultIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlayerSkin> skins = new ArrayList<>();
        for (String id : defaultIds) {
            Path path = assetManager.getDefaultSkinPath(id);
            if (Files.exists(path)) {
                PlayerSkin skin = loadFromFile(path, true);
                if (skin != null) {
                    skins.add(skin);
                }
            }
        }
        return skins;
    }

    public PlayerSkin importSkin(Path source, String targetName) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetName, "targetName");
        validateSize(source);
        Path destination = assetManager.getSkinPath(targetName);
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        cache.remove(destination);
        return loadFromFile(destination, false);
    }

    public void clearCache() {
        cache.values().forEach(skin -> skin.setTextureId(-1));
        cache.clear();
    }

    private void validateSize(Path path) throws IOException {
        long size = Files.size(path);
        if (size <= 0 || size > MAX_SKIN_BYTES) {
            throw new IOException("Skin file size out of bounds: " + size + " bytes");
        }
    }
}
