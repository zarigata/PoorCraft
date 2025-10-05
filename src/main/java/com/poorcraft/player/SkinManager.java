package com.poorcraft.player;

import com.poorcraft.config.Settings;
import com.poorcraft.resources.AssetManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Central manager for player skin lifecycle.
 */
public class SkinManager {

    private static final SkinManager INSTANCE = new SkinManager();
    private static final List<String> DEFAULT_SKINS = List.of("steve", "alex");

    private final Map<String, PlayerSkin> skins;
    private final SkinLoader loader;
    private final SkinAtlas atlas;
    private PlayerSkin currentSkin;
    private Settings settings;

    private SkinManager() {
        this.skins = new HashMap<>();
        this.loader = new SkinLoader();
        this.atlas = new SkinAtlas();
    }

    public static SkinManager getInstance() {
        return INSTANCE;
    }

    public void init(Settings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        loadDefaultSkins();
        loadUserSkins();
        String targetSkin = settings.player != null ? settings.player.selectedSkin : "steve";
        setCurrentSkin(targetSkin);
    }

    public void cleanup() {
        atlas.cleanup();
        loader.clearCache();
        skins.clear();
    }

    public List<PlayerSkin> getAllSkins() {
        return new ArrayList<>(skins.values());
    }

    public PlayerSkin getSkin(String name) {
        return skins.get(name);
    }

    public PlayerSkin getCurrentSkin() {
        return currentSkin;
    }

    public void setCurrentSkin(String name) {
        PlayerSkin skin = skins.get(name);
        if (skin == null) {
            skin = skins.get("steve");
        }
        if (skin == null) {
            System.err.println("[SkinManager] Unable to set skin '" + name + "' and steve fallback is missing.");
            return;
        }
        uploadSkinTexture(skin);
        currentSkin = skin;
        if (settings != null && settings.player != null) {
            settings.player.selectedSkin = skin.getName();
        }
        System.out.println("[SkinManager] Active skin set to " + skin.getName());
    }

    public PlayerSkin importSkin(Path source, String targetName) {
        try {
            PlayerSkin skin = loader.importSkin(source, targetName);
            if (skin != null) {
                registerSkin(skin);
                uploadSkinTexture(skin);
                System.out.println("[SkinManager] Imported skin " + skin.getName());
                return skin;
            }
        } catch (Exception e) {
            System.err.println("[SkinManager] Failed to import skin: " + e.getMessage());
        }
        return null;
    }

    public void removeSkin(String name) {
        PlayerSkin skin = skins.remove(name);
        if (skin == null || skin.isDefault()) {
            return;
        }
        atlas.removeSkin(name);
        try {
            Files.deleteIfExists(skin.getFilePath());
        } catch (Exception e) {
            System.err.println("[SkinManager] Failed to delete skin file: " + e.getMessage());
        }
        if (currentSkin == skin) {
            setCurrentSkin("steve");
        }
    }

    public void reloadSkin(String name) {
        PlayerSkin skin = skins.get(name);
        if (skin == null) {
            return;
        }
        skin.reload();
        uploadSkinTexture(skin);
    }

    public void reloadAllSkins() {
        skins.values().forEach(PlayerSkin::reload);
        skins.values().forEach(this::uploadSkinTexture);
    }

    public SkinAtlas getAtlas() {
        return atlas;
    }

    public List<PlayerSkin> getDefaultSkins() {
        List<PlayerSkin> defaults = new ArrayList<>();
        for (String id : DEFAULT_SKINS) {
            PlayerSkin skin = skins.get(id);
            if (skin != null && skin.isDefault()) {
                defaults.add(skin);
            }
        }
        return defaults;
    }

    private void loadDefaultSkins() {
        AssetManager assetManager = AssetManager.getInstance();
        for (String id : DEFAULT_SKINS) {
            Path path = assetManager.getDefaultSkinPath(id);
            if (!Files.exists(path)) {
                PlayerSkin loaded = loader.loadFromClasspath("/skins/default/" + id + ".png", id);
                if (loaded == null) {
                    System.err.println("[SkinManager] Failed to provision default skin: " + id);
                }
            }
        }
        List<PlayerSkin> defaultSkins = loader.loadAllDefaultSkins(DEFAULT_SKINS);
        for (PlayerSkin skin : defaultSkins) {
            registerSkin(skin);
            uploadSkinTexture(skin);
        }
    }

    private void loadUserSkins() {
        List<PlayerSkin> userSkins = loader.loadAllUserSkins();
        for (PlayerSkin skin : userSkins) {
            registerSkin(skin);
            uploadSkinTexture(skin);
        }
    }

    private void registerSkin(PlayerSkin skin) {
        skins.put(skin.getName(), skin);
    }

    private void uploadSkinTexture(PlayerSkin skin) {
        if (skin != null && skin.isValid()) {
            atlas.uploadSkin(skin);
        }
    }

    public String ensureUniqueUserSkinName(String baseName) {
        Objects.requireNonNull(baseName, "baseName");
        String candidate = baseName.trim();
        if (candidate.isEmpty()) {
            candidate = "skin";
        }
        if (candidate.length() > 32) {
            candidate = candidate.substring(0, 32);
        }

        AssetManager assetManager = AssetManager.getInstance();
        Path initialPath = assetManager.getSkinPath(candidate);
        if (!Files.exists(initialPath)) {
            return candidate;
        }

        int counter = 1;
        while (true) {
            String suffix = "_" + counter;
            int maxBaseLength = Math.max(1, 32 - suffix.length());
            String truncatedBase = candidate.length() > maxBaseLength
                ? candidate.substring(0, maxBaseLength)
                : candidate;
            String withSuffix = truncatedBase + suffix;
            Path prospective = assetManager.getSkinPath(withSuffix);
            if (!Files.exists(prospective)) {
                return withSuffix;
            }
            counter++;
        }
    }
}
