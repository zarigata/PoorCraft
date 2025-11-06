package com.poorcraft.player;

import com.poorcraft.resources.AssetManager;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            if (!Files.exists(filePath)) {
                System.err.println("[SkinLoader] Skin file missing: " + filePath);
                return null;
            }
            if (!PlayerSkin.isValidSkinFile(filePath)) {
                return null;
            }
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
            System.err.println("[SkinLoader] Skin failed validation: " + filePath);
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
                System.err.println("[SkinLoader] Bundled resource not found: " + resourcePath + ", will attempt generation if possible.");
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
            System.out.println("[SkinLoader] Loading default skin: " + id);
            if (Files.exists(path)) {
                PlayerSkin skin = loadFromFile(path, true);
                if (skin != null) {
                    skins.add(skin);
                }
            } else {
                System.err.println("[SkinLoader] Default skin file missing: " + id + ", generation may be needed");
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

    public static boolean validateSkinImage(BufferedImage image, String skinRef) {
        if (image == null) {
            System.err.println("[SkinLoader] Skin '" + skinRef + "' image is null or unreadable");
            return false;
        }
        if (image.getWidth() != 64 || image.getHeight() != 64) {
            System.err.println("[SkinLoader] Skin '" + skinRef + "' has invalid dimensions " +
                image.getWidth() + "x" + image.getHeight() + " (expected 64x64)");
            return false;
        }
        return true;
    }
}
