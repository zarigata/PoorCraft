package com.poorcraft.player;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Procedurally generates 64x64 Minecraft-style player skins with light noise and colour variation.
 */
public final class SkinGenerator {

    private static final int SKIN_SIZE = 64;
    private static final int ARM_HEIGHT = 12;
    private static final int LEG_HEIGHT = 12;
    private static final int HAND_HEIGHT = 4;
    private static final int BOOT_HEIGHT = 4;

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private static final List<Color> SKIN_TONES = List.of(
        new Color(238, 190, 172),
        new Color(226, 171, 141),
        new Color(208, 155, 124),
        new Color(182, 130, 98),
        new Color(153, 108, 76),
        new Color(126, 86, 60)
    );

    private static final List<Color> HAIR_COLORS = List.of(
        new Color(82, 58, 32),
        new Color(118, 84, 48),
        new Color(156, 112, 66),
        new Color(42, 36, 40),
        new Color(188, 56, 48),
        new Color(210, 180, 124)
    );

    private static final List<Color> EYE_COLORS = List.of(
        new Color(76, 120, 200),
        new Color(40, 158, 124),
        new Color(52, 92, 168),
        new Color(110, 70, 40)
    );

    private static final List<Color> SHIRT_COLORS = List.of(
        new Color(52, 118, 196),
        new Color(82, 148, 68),
        new Color(176, 64, 54),
        new Color(202, 156, 64),
        new Color(96, 78, 196),
        new Color(196, 108, 164)
    );

    private static final List<Color> PANTS_COLORS = List.of(
        new Color(52, 68, 120),
        new Color(60, 68, 82),
        new Color(94, 76, 52),
        new Color(40, 88, 112)
    );

    private SkinGenerator() {
    }

    public static BufferedImage generateDefaultSkin(String skinId, Path targetPath) {
        String id = (skinId == null || skinId.isBlank()) ? "steve" : skinId.toLowerCase();
        ArmModel armModel = id.equals("alex") ? ArmModel.SLIM : ArmModel.CLASSIC;
        BufferedImage skin = generateSkin(id, armModel);
        if (skin != null && targetPath != null) {
            saveSkin(skin, targetPath);
        }
        if (skin != null) {
            System.out.println("[SkinGenerator] Generated default skin: " + id);
        }
        return skin;
    }

    private static BufferedImage generateSkin(String skinId, ArmModel armModel) {
        try {
            Random rng = new Random(hashSeed(skinId));

            Color skinTone = jitter(pick(rng, SKIN_TONES), 6, rng);
            Color hair = jitter(pick(rng, HAIR_COLORS), 10, rng);
            Color eyes = pick(rng, EYE_COLORS);
            Color shirt = jitter(pick(rng, SHIRT_COLORS), 12, rng);
            Color pants = jitter(pick(rng, PANTS_COLORS), 10, rng);
            Color accent = jitter(lighten(shirt, rng.nextInt(18) - 9), 8, rng);

            BufferedImage image = new BufferedImage(SKIN_SIZE, SKIN_SIZE, BufferedImage.TYPE_INT_ARGB);
            clearImage(image);

            paintHeadBase(image, skinTone, hair, rng);
            paintFaceDetails(image, skinTone, eyes);
            paintBodyBase(image, skinTone, shirt, accent, rng);
            paintRightArmBase(image, skinTone, shirt, armModel, rng);
            paintRightLegBase(image, pants, rng);

            mirrorBaseLimbs(image, armModel, rng);

            paintHeadOverlay(image, hair, rng);
            paintBodyOverlay(image, shirt, accent, rng);
            paintRightArmOverlay(image, skinTone, shirt, armModel, rng);
            paintRightLegOverlay(image, pants, rng);

            mirrorOverlayLimbs(image, armModel, rng);
            addFaceHighlights(image, skinTone, rng);

            return image;
        } catch (Exception ex) {
            System.err.println("[SkinGenerator] Failed to generate skin '" + skinId + "': " + ex.getMessage());
            return null;
        }
    }

    private static void paintHeadBase(BufferedImage image, Color skinTone, Color hair, Random rng) {
        Color hairHighlight = lighten(hair, 14);
        Color hairShadow = darken(hair, 14);

        fillNoiseRect(image, 8, 8, 8, 8, skinTone, 6, rng);
        fillNoiseRect(image, 0, 8, 8, 8, hairShadow, 8, rng);
        fillNoiseRect(image, 16, 8, 8, 8, hairShadow, 8, rng);
        fillNoiseRect(image, 24, 8, 8, 8, darken(hair, 20), 10, rng);
        fillNoiseRect(image, 8, 0, 8, 8, hairHighlight, 12, rng);
        fillNoiseRect(image, 16, 0, 8, 8, darken(skinTone, 10), 4, rng);
    }

    private static void paintFaceDetails(BufferedImage image, Color skinTone, Color eyeColor) {
        Color sclera = new Color(240, 248, 255);
        fillSolid(image, 10, 11, 2, 2, sclera);
        fillSolid(image, 14, 11, 2, 2, sclera);
        setPixel(image, 11, 12, eyeColor);
        setPixel(image, 15, 12, eyeColor);
        setPixel(image, 12, 12, darken(eyeColor, 28));
        setPixel(image, 14, 12, darken(eyeColor, 28));

        Color lip = darken(skinTone, 24);
        fillSolid(image, 11, 14, 6, 1, lip);
        fillSolid(image, 11, 15, 6, 1, darken(lip, 8));
    }

    private static void paintBodyBase(BufferedImage image, Color skinTone, Color shirt, Color accent, Random rng) {
        fillNoiseRect(image, 20, 20, 8, 12, shirt, 10, rng);
        fillNoiseRect(image, 32, 20, 8, 12, darken(shirt, 24), 8, rng);
        fillNoiseRect(image, 16, 20, 4, 12, darken(shirt, 14), 8, rng);
        fillNoiseRect(image, 28, 20, 4, 12, darken(shirt, 14), 8, rng);
        fillNoiseRect(image, 20, 16, 8, 4, lighten(shirt, 16), 6, rng);
        fillNoiseRect(image, 28, 16, 8, 4, darken(shirt, 30), 4, rng);

        fillSolid(image, 22, 21, 4, 2, lighten(accent, 12));
        fillSolid(image, 24, 23, 2, 5, accent);
        fillSolid(image, 20, 30, 8, 2, darken(shirt, 18));

        fillNoiseRect(image, 16, 16, 4, 4, skinTone, 4, rng);
        fillNoiseRect(image, 32, 16, 4, 4, skinTone, 4, rng);
    }

    private static void paintBodyOverlay(BufferedImage image, Color shirt, Color accent, Random rng) {
        fillNoiseRect(image, 20, 36, 8, 12, withAlpha(lighten(shirt, 16), 220), 10, rng);
        fillNoiseRect(image, 32, 36, 8, 12, withAlpha(darken(shirt, 18), 210), 8, rng);
        fillNoiseRect(image, 16, 36, 4, 12, withAlpha(darken(shirt, 10), 215), 8, rng);
        fillNoiseRect(image, 28, 36, 4, 12, withAlpha(darken(shirt, 10), 215), 8, rng);
        fillNoiseRect(image, 20, 32, 8, 4, withAlpha(lighten(shirt, 24), 230), 6, rng);
        fillNoiseRect(image, 28, 32, 8, 4, withAlpha(darken(shirt, 36), 220), 4, rng);

        fillSolid(image, 23, 38, 2, 8, withAlpha(lighten(accent, 16), 235));
    }

    private static void paintHeadOverlay(BufferedImage image, Color hair, Random rng) {
        fillNoiseRect(image, 40, 8, 8, 8, withAlpha(lighten(hair, 12), 228), 10, rng);
        fillNoiseRect(image, 32, 8, 8, 8, withAlpha(darken(hair, 8), 226), 10, rng);
        fillNoiseRect(image, 48, 8, 8, 8, withAlpha(darken(hair, 8), 226), 10, rng);
        fillNoiseRect(image, 56, 8, 8, 8, withAlpha(darken(hair, 18), 224), 10, rng);
        fillNoiseRect(image, 40, 0, 8, 8, withAlpha(lighten(hair, 10), 232), 12, rng);
        fillNoiseRect(image, 48, 0, 8, 8, withAlpha(darken(hair, 18), 228), 12, rng);

        for (int i = 0; i < 8; i++) {
            int drop = 2 + rng.nextInt(3);
            for (int y = 0; y < drop; y++) {
                setPixel(image, 40 + i, 8 + y, withAlpha(lighten(hair, 14), 236));
            }
        }
    }

    private static void paintRightArmBase(BufferedImage image, Color skinTone, Color sleeve, ArmModel model, Random rng) {
        fillArmCap(image, 44, 16, model, lighten(sleeve, 12), rng);
        fillArmCap(image, 48, 16, model, darken(skinTone, 6), rng);

        fillArmFace(image, 40, 20, model, darken(sleeve, 16), skinTone, rng);
        fillArmFace(image, 44, 20, model, sleeve, skinTone, rng);
        fillArmFace(image, 48, 20, model, lighten(sleeve, 10), skinTone, rng);
        fillArmFace(image, 52, 20, model, darken(sleeve, 24), skinTone, rng);
        if (model == ArmModel.SLIM) {
            maskSlimRightArmSource(image, 20);
        }
    }

    private static void paintRightArmOverlay(BufferedImage image, Color skinTone, Color sleeve, ArmModel model, Random rng) {
        fillArmCapOverlay(image, 44, 32, model, withAlpha(lighten(sleeve, 18), 228), rng);
        fillArmCapOverlay(image, 48, 32, model, withAlpha(skinTone, 220), rng);

        fillArmFaceOverlay(image, 40, 36, model, withAlpha(darken(sleeve, 8), 220), skinTone, rng);
        fillArmFaceOverlay(image, 44, 36, model, withAlpha(lighten(sleeve, 12), 225), skinTone, rng);
        fillArmFaceOverlay(image, 48, 36, model, withAlpha(lighten(sleeve, 4), 225), skinTone, rng);
        fillArmFaceOverlay(image, 52, 36, model, withAlpha(darken(sleeve, 20), 215), skinTone, rng);
        if (model == ArmModel.SLIM) {
            maskSlimRightArmSource(image, 36);
        }
    }

    private static void paintRightLegBase(BufferedImage image, Color pants, Random rng) {
        fillNoiseRect(image, 4, 16, 4, 4, lighten(pants, 12), 4, rng);
        fillNoiseRect(image, 8, 16, 4, 4, darken(pants, 24), 4, rng);

        fillLegFace(image, 0, 20, darken(pants, 12), rng);
        fillLegFace(image, 4, 20, pants, rng);
        fillLegFace(image, 8, 20, darken(pants, 12), rng);
        fillLegFace(image, 12, 20, darken(pants, 24), rng);

        fillSolid(image, 4, 28, 4, BOOT_HEIGHT, darken(pants, 40));
        fillSolid(image, 8, 28, 4, BOOT_HEIGHT, darken(pants, 40));
    }

    private static void paintRightLegOverlay(BufferedImage image, Color pants, Random rng) {
        fillNoiseRect(image, 4, 36, 4, 4, withAlpha(lighten(pants, 16), 220), 6, rng);
        fillNoiseRect(image, 8, 36, 4, 4, withAlpha(darken(pants, 16), 210), 6, rng);

        fillNoiseRect(image, 0, 40, 4, LEG_HEIGHT, withAlpha(darken(pants, 8), 210), 8, rng);
        fillNoiseRect(image, 4, 40, 4, LEG_HEIGHT, withAlpha(lighten(pants, 12), 220), 8, rng);
        fillNoiseRect(image, 8, 40, 4, LEG_HEIGHT, withAlpha(darken(pants, 8), 210), 8, rng);
        fillNoiseRect(image, 12, 40, 4, LEG_HEIGHT, withAlpha(darken(pants, 24), 205), 8, rng);

        fillSolid(image, 4, 48, 4, BOOT_HEIGHT, withAlpha(darken(pants, 44), 230));
        fillSolid(image, 8, 48, 4, BOOT_HEIGHT, withAlpha(darken(pants, 44), 230));
    }

    private static void mirrorBaseLimbs(BufferedImage image, ArmModel model, Random rng) {
        copyRegion(image, 0, 16, 16, 16, 16, 48);
        copyRegion(image, 40, 16, 16, 16, 32, 48);

        toneRegion(image, 16, 48, 16, 16, 6, rng);
        if (model == ArmModel.SLIM) {
            maskSlimPadding(image, 32, 48);
        }
    }

    private static void mirrorOverlayLimbs(BufferedImage image, ArmModel model, Random rng) {
        copyRegion(image, 0, 32, 16, 16, 0, 48);
        copyRegion(image, 40, 32, 16, 16, 48, 48);

        toneRegion(image, 0, 48, 16, 16, 6, rng);
        if (model == ArmModel.SLIM) {
            maskSlimPadding(image, 48, 48);
        }
    }

    private static void addFaceHighlights(BufferedImage image, Color skinTone, Random rng) {
        Color highlight = lighten(skinTone, 18);
        setPixel(image, 9, 13, highlight);
        setPixel(image, 16, 13, highlight);
        if (rng.nextBoolean()) {
            setPixel(image, 12, 13, lighten(skinTone, 24));
        }
    }

    private static void fillArmFace(BufferedImage image, int startX, int startY, ArmModel model, Color sleeveColor, Color skinTone, Random rng) {
        int width = model.textureWidth();
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < ARM_HEIGHT; dy++) {
                int px = startX + dx;
                int py = startY + dy;
                if (dx >= width) {
                    setPixel(image, px, py, TRANSPARENT);
                    continue;
                }
                boolean isSleeve = dy < ARM_HEIGHT - HAND_HEIGHT;
                Color base = isSleeve ? sleeveColor : skinTone;
                int shading = dx == 0 ? -8 : dx == width - 1 ? 8 : 0;
                base = adjust(base, shading);
                setPixel(image, px, py, jitter(base, isSleeve ? 8 : 4, rng));
            }
        }
    }

    private static void fillArmFaceOverlay(BufferedImage image, int startX, int startY, ArmModel model, Color sleeveColor, Color skinTone, Random rng) {
        int width = model.textureWidth();
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < ARM_HEIGHT; dy++) {
                int px = startX + dx;
                int py = startY + dy;
                if (dx >= width) {
                    setPixel(image, px, py, TRANSPARENT);
                    continue;
                }
                boolean isSleeve = dy < ARM_HEIGHT - HAND_HEIGHT;
                Color base = isSleeve ? sleeveColor : withAlpha(skinTone, sleeveColor.getAlpha());
                int shading = dx == 0 ? -6 : dx == width - 1 ? 6 : 0;
                base = adjust(base, shading);
                setPixel(image, px, py, jitter(base, isSleeve ? 6 : 4, rng));
            }
        }
    }

    private static void fillArmCap(BufferedImage image, int startX, int startY, ArmModel model, Color color, Random rng) {
        int width = model.textureWidth();
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                int px = startX + dx;
                int py = startY + dy;
                if (dx >= width) {
                    setPixel(image, px, py, TRANSPARENT);
                    continue;
                }
                setPixel(image, px, py, jitter(color, 8, rng));
            }
        }
    }

    private static void fillArmCapOverlay(BufferedImage image, int startX, int startY, ArmModel model, Color color, Random rng) {
        int width = model.textureWidth();
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                int px = startX + dx;
                int py = startY + dy;
                if (dx >= width) {
                    setPixel(image, px, py, TRANSPARENT);
                    continue;
                }
                setPixel(image, px, py, jitter(color, 6, rng));
            }
        }
    }

    private static void fillLegFace(BufferedImage image, int startX, int startY, Color pantsColor, Random rng) {
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < LEG_HEIGHT; dy++) {
                int px = startX + dx;
                int py = startY + dy;
                boolean isPants = dy < LEG_HEIGHT - BOOT_HEIGHT;
                Color base = isPants ? pantsColor : darken(pantsColor, 28);
                int shading = dx == 0 ? -10 : dx == 3 ? 10 : 0;
                base = adjust(base, shading);
                setPixel(image, px, py, jitter(base, isPants ? 8 : 4, rng));
            }
        }
    }

    private static void fillNoiseRect(BufferedImage image, int x, int y, int width, int height, Color base, int variance, Random rng) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                setPixel(image, x + dx, y + dy, jitter(base, variance, rng));
            }
        }
    }

    private static void fillSolid(BufferedImage image, int x, int y, int width, int height, Color color) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                setPixel(image, x + dx, y + dy, color);
            }
        }
    }

    private static void copyRegion(BufferedImage image, int srcX, int srcY, int width, int height, int destX, int destY) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int argb = image.getRGB(srcX + dx, srcY + dy);
                image.setRGB(destX + dx, destY + dy, argb);
            }
        }
    }

    private static void toneRegion(BufferedImage image, int x, int y, int width, int height, int variance, Random rng) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int px = x + dx;
                int py = y + dy;
                Color c = new Color(image.getRGB(px, py), true);
                if (c.getAlpha() == 0) {
                    continue;
                }
                image.setRGB(px, py, jitter(c, variance, rng).getRGB());
            }
        }
    }

    private static void maskSlimPadding(BufferedImage image, int x, int y) {
        for (int dy = 0; dy < 16; dy++) {
            image.setRGB(x + 3, y + dy, TRANSPARENT.getRGB());
            image.setRGB(x + 7, y + dy, TRANSPARENT.getRGB());
        }
    }

    private static void maskSlimRightArmSource(BufferedImage image, int startY) {
        for (int dy = 0; dy < ARM_HEIGHT; dy++) {
            image.setRGB(54, startY + dy, TRANSPARENT.getRGB());
        }
    }

    private static void clearImage(BufferedImage image) {
        for (int y = 0; y < SKIN_SIZE; y++) {
            for (int x = 0; x < SKIN_SIZE; x++) {
                image.setRGB(x, y, TRANSPARENT.getRGB());
            }
        }
    }

    private static void setPixel(BufferedImage image, int x, int y, Color color) {
        if (x < 0 || x >= SKIN_SIZE || y < 0 || y >= SKIN_SIZE) {
            return;
        }
        image.setRGB(x, y, color.getRGB());
    }

    private static Color jitter(Color base, int variance, Random rng) {
        int r = clamp(base.getRed() + rng.nextInt(variance * 2 + 1) - variance);
        int g = clamp(base.getGreen() + rng.nextInt(variance * 2 + 1) - variance);
        int b = clamp(base.getBlue() + rng.nextInt(variance * 2 + 1) - variance);
        return new Color(r, g, b, base.getAlpha());
    }

    private static Color lighten(Color base, int amount) {
        return adjust(base, Math.abs(amount));
    }

    private static Color darken(Color base, int amount) {
        return adjust(base, -Math.abs(amount));
    }

    private static Color adjust(Color base, int delta) {
        return new Color(
            clamp(base.getRed() + delta),
            clamp(base.getGreen() + delta),
            clamp(base.getBlue() + delta),
            base.getAlpha()
        );
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clamp(alpha));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static <T> T pick(Random rng, List<T> values) {
        return values.get(rng.nextInt(values.size()));
    }

    private static void saveSkin(BufferedImage image, Path target) {
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(image, "PNG", target.toFile());
        } catch (IOException e) {
            System.err.println("[SkinGenerator] Failed to save skin to " + target + ": " + e.getMessage());
        }
    }

    private static long hashSeed(String seed) {
        return Objects.hash(seed, 0xC0FFEEBABEL);
    }

    private enum ArmModel {
        CLASSIC(4),
        SLIM(3);

        private final int width;

        ArmModel(int width) {
            this.width = width;
        }

        int textureWidth() {
            return width;
        }
    }
}
