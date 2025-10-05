package com.poorcraft.player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a player skin texture and related metadata.
 */
public class PlayerSkin {

    public enum SkinType {
        CLASSIC,
        SLIM
    }

    private final String name;
    private final String displayName;
    private final Path filePath;
    private final boolean isDefault;
    private final boolean isCustom;
    private final SkinType type;
    private BufferedImage image;
    private int textureId;

    public PlayerSkin(String name, Path filePath, boolean isDefault, boolean isCustom) {
        this.name = Objects.requireNonNull(name, "name");
        this.displayName = toDisplayName(name);
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.isDefault = isDefault;
        this.isCustom = isCustom;
        this.image = load(filePath);
        this.type = detectSkinType(image);
        this.textureId = -1;
    }

    private BufferedImage load(Path path) {
        try {
            if (!Files.exists(path)) {
                throw new IOException("Skin file does not exist: " + path);
            }
            BufferedImage loaded = ImageIO.read(path.toFile());
            validateDimensions(loaded, path);
            return loaded;
        } catch (IOException e) {
            System.err.println("[PlayerSkin] Failed to load skin " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void validateDimensions(BufferedImage image, Path path) {
        if (image == null) {
            throw new IllegalArgumentException("Skin image is null for path: " + path);
        }
        if (image.getWidth() != 64 || image.getHeight() != 64) {
            throw new IllegalArgumentException("Skin " + path + " must be 64x64 pixels");
        }
    }

    private SkinType detectSkinType(BufferedImage image) {
        if (image == null) {
            return SkinType.CLASSIC;
        }
        // Minecraft slim arms are 3 pixels wide while classic are 4.
        for (int y = 0; y < 16; y++) {
            int alpha = (image.getRGB(54, 20 + y) >>> 24) & 0xFF; // Right arm outer edge
            if (alpha != 0) {
                return SkinType.CLASSIC;
            }
        }
        return SkinType.SLIM;
    }

    private String toDisplayName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unnamed Skin";
        }
        String base = raw.endsWith(".png") ? raw.substring(0, raw.length() - 4) : raw;
        if (base.isBlank()) {
            return "Unnamed Skin";
        }
        String[] tokens = base.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1).toLowerCase());
            }
            builder.append(' ');
        }
        return builder.length() == 0 ? base : builder.toString().trim();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public SkinType getType() {
        return type;
    }

    public BufferedImage getImage() {
        return image;
    }

    public boolean isValid() {
        return image != null && image.getWidth() == 64 && image.getHeight() == 64;
    }

    public void reload() {
        this.image = load(filePath);
    }

    public String getFileName() {
        return filePath.getFileName().toString();
    }

    public int getTextureId() {
        return textureId;
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }
}
