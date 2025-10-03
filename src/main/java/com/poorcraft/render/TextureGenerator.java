package com.poorcraft.render;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * Generates 16x16 placeholder textures with simple patterns when no authored art exists.
 * <p>
 * These textures are intentionally a little noisy so the world does not look like it was painted with
 * a single crayon. Think of it as Minecraft Alpha fan-art created with MSPaint at 3 AM.
 */
public final class TextureGenerator {

    public static final int TEXTURE_SIZE = 16;

    private static final Path RESOURCE_ROOT = Path.of("src", "main", "resources");
    private static final Path WORKSPACE_BLOCK_DIR = RESOURCE_ROOT.resolve(Path.of("textures", "blocks"));
    private static final Path WORKSPACE_SKIN_DIR = RESOURCE_ROOT.resolve(Path.of("textures", "skins"));
    private static final Path OUTPUT_BLOCK_DIR = Path.of("generated", "textures", "blocks");
    private static final Path OUTPUT_SKIN_DIR = Path.of("generated", "textures", "skins");
    private static final String CLASSPATH_BLOCK_DIR = "/textures/blocks/";
    private static final String CLASSPATH_SKIN_DIR = "/textures/skins/";
    private static final Map<String, TextureFactory> BLOCK_FACTORIES = new LinkedHashMap<>();
    private static final Map<String, TextureFactory> FLORA_FACTORIES = new LinkedHashMap<>();
    private static final Map<String, TextureFactory> SKIN_FACTORIES = new LinkedHashMap<>();

    private static Map<String, ByteBuffer> cachedBlockTextures;
    private static boolean auxiliaryEnsured;

    static {
        registerBlockFactories();
        registerFloraFactories();
        registerSkinFactories();
    }

    private TextureGenerator() {
    }

    /**
     * Ensures every known block texture exists, generating procedural art when necessary.
     * The resulting RGBA buffers are returned for immediate atlas construction.
     */
    public static synchronized Map<String, ByteBuffer> ensureDefaultBlockTextures() {
        if (cachedBlockTextures != null) {
            return cachedBlockTextures;
        }

        Map<String, ByteBuffer> textures = new LinkedHashMap<>();
        for (Map.Entry<String, TextureFactory> entry : BLOCK_FACTORIES.entrySet()) {
            textures.put(entry.getKey(), loadOrGenerate(entry.getKey(), entry.getValue(), WORKSPACE_BLOCK_DIR, OUTPUT_BLOCK_DIR, CLASSPATH_BLOCK_DIR));
        }

        cachedBlockTextures = Collections.unmodifiableMap(textures);
        return cachedBlockTextures;
    }

    /**
     * Generates a handful of auxiliary textures (flowers, leaves, base skins) so modders have
     * something to tweak without starting from absolute zero. These files live under generated/
     * and can be swapped out by hand-crafted PNGs later.
     */
    public static synchronized void ensureAuxiliaryTextures() {
        if (auxiliaryEnsured) {
            return;
        }

        for (Map.Entry<String, TextureFactory> entry : FLORA_FACTORIES.entrySet()) {
            loadOrGenerate(entry.getKey(), entry.getValue(), WORKSPACE_BLOCK_DIR, OUTPUT_BLOCK_DIR, CLASSPATH_BLOCK_DIR);
        }
        for (Map.Entry<String, TextureFactory> entry : SKIN_FACTORIES.entrySet()) {
            loadOrGenerate(entry.getKey(), entry.getValue(), WORKSPACE_SKIN_DIR, OUTPUT_SKIN_DIR, CLASSPATH_SKIN_DIR);
        }

        auxiliaryEnsured = true;
    }

    private static ByteBuffer loadOrGenerate(String name,
                                             TextureFactory factory,
                                             Path preferredDiskFolder,
                                             Path generatedDiskFolder,
                                             String classpathFolder) {
        BufferedImage existing = tryLoadFromClasspath(classpathFolder, name);
        if (existing == null) {
            existing = tryLoadFromDisk(preferredDiskFolder, name);
        }
        if (existing == null) {
            existing = tryLoadFromDisk(generatedDiskFolder, name);
        }

        if (existing == null && factory != null) {
            Random rng = new Random(hashSeed(name));
            existing = factory.create(name, rng);
            saveToDisk(generatedDiskFolder, name, existing);
            // Drop a copy into the workspace directory so artists can swap it out without spelunking
            saveToDisk(preferredDiskFolder, name, existing);
        }

        if (existing == null) {
            return null; // fallback to atlas placeholder later on
        }

        return toByteBuffer(existing);
    }

    private static BufferedImage tryLoadFromClasspath(String folder, String name) {
        if (folder == null) {
            return null;
        }
        String resourcePath = folder + name + ".png";
        try (InputStream stream = TextureGenerator.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                if (validateDimensions(name, image)) {
                    return image;
                }
            }
        } catch (IOException ignored) {
            // If this fails we'll fall back to generation, no need to spam logs here.
        }
        return null;
    }

    private static BufferedImage tryLoadFromDisk(Path directory, String name) {
        if (directory == null) {
            return null;
        }
        Path path = directory.resolve(name + ".png");
        if (!Files.exists(path)) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (validateDimensions(name, image)) {
                return image;
            }
        } catch (IOException ignored) {
            // Someone dropped a corrupt PNG? We'll regenerate a fresh one like it's 2010 again.
        }
        return null;
    }

    private static void saveToDisk(Path directory, String name, BufferedImage image) {
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(name + ".png");
            if (Files.exists(target)) {
                return; // Respect existing artwork.
            }
            try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                ImageIO.write(image, "PNG", output);
            }
        } catch (IOException e) {
            System.err.println("[TextureGenerator] Failed to save texture '" + name + "' to " + directory + ": " + e.getMessage());
        }
    }

    private static boolean validateDimensions(String name, BufferedImage image) {
        if (image == null) {
            return false;
        }
        if (image.getWidth() != TEXTURE_SIZE || image.getHeight() != TEXTURE_SIZE) {
            System.err.println("[TextureGenerator] Texture '" + name + "' is " + image.getWidth() + "x" + image.getHeight() +
                    " but needs to be 16x16. Ignoring it and falling back to generation.");
            return false;
        }
        return true;
    }

    private static long hashSeed(String name) {
        return Objects.hash(name, 8675309L);
    }

    private static ByteBuffer toByteBuffer(BufferedImage image) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF)); // R
                buffer.put((byte) ((argb >> 8) & 0xFF));  // G
                buffer.put((byte) (argb & 0xFF));         // B
                buffer.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();
        return buffer;
    }

    private static void registerBlockFactories() {
        BLOCK_FACTORIES.put("dirt", (name, rng) -> noisyFill(color(126, 86, 48), 18, rng));
        BLOCK_FACTORIES.put("stone", (name, rng) -> noisyFill(color(128, 130, 132), 10, rng));
        BLOCK_FACTORIES.put("bedrock", (name, rng) -> chunkyNoise(color(68, 68, 72), color(18, 18, 18), rng));
        BLOCK_FACTORIES.put("grass_top", TextureGenerator::generateGrassTop);
        BLOCK_FACTORIES.put("grass_side", TextureGenerator::generateGrassSide);
        BLOCK_FACTORIES.put("sand", (name, rng) -> noisyFill(color(220, 200, 158), 12, rng));
        BLOCK_FACTORIES.put("sandstone", TextureGenerator::generateSandstone);
        BLOCK_FACTORIES.put("cactus_top", TextureGenerator::generateCactusTop);
        BLOCK_FACTORIES.put("cactus_side", TextureGenerator::generateCactusSide);
        BLOCK_FACTORIES.put("snow_block", (name, rng) -> noisyFill(color(236, 240, 245), 8, rng));
        BLOCK_FACTORIES.put("ice", TextureGenerator::generateIce);
        BLOCK_FACTORIES.put("snow_layer", TextureGenerator::generateSnowLayer);
        BLOCK_FACTORIES.put("jungle_grass_top", TextureGenerator::generateJungleGrassTop);
        BLOCK_FACTORIES.put("jungle_grass_side", TextureGenerator::generateJungleGrassSide);
        BLOCK_FACTORIES.put("jungle_dirt", (name, rng) -> noisyFill(color(90, 63, 28), 20, rng));
        BLOCK_FACTORIES.put("wood_top", TextureGenerator::generateWoodTop);
        BLOCK_FACTORIES.put("wood_side", TextureGenerator::generateWoodSide);
        BLOCK_FACTORIES.put("leaves", TextureGenerator::generateLeaves);
    }

    private static void registerFloraFactories() {
        FLORA_FACTORIES.put("flower_red", TextureGenerator::generateRedFlower);
        FLORA_FACTORIES.put("flower_yellow", TextureGenerator::generateYellowFlower);
        FLORA_FACTORIES.put("flower_blue", TextureGenerator::generateBlueFlower);
        FLORA_FACTORIES.put("leaves_sparse", TextureGenerator::generateSparseLeaves);
    }

    private static void registerSkinFactories() {
        SKIN_FACTORIES.put("player_base", TextureGenerator::generatePlayerBaseSkin);
        SKIN_FACTORIES.put("villager_base", TextureGenerator::generateVillagerSkin);
    }

    private static BufferedImage noisyFill(Color base, int variation, Random rng) {
        BufferedImage image = newImage();
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                image.setRGB(x, y, jitter(base, variation, rng).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage chunkyNoise(Color primary, Color secondary, Random rng) {
        BufferedImage image = newImage();
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                boolean usePrimary = ((x / 2 + y / 2) % 2 == 0) ^ rng.nextBoolean();
                Color color = usePrimary ? jitter(primary, 10, rng) : jitter(secondary, 8, rng);
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }

    private static BufferedImage generateGrassTop(String name, Random rng) {
        BufferedImage image = newImage();
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int shade = 90 + rng.nextInt(60);
                int highlight = rng.nextInt(100) < 5 ? 40 : 0;
                Color color = new Color(clamp(shade - 20 + highlight), clamp(shade + 20), clamp(shade - 30 + highlight));
                image.setRGB(x, y, color.getRGB());
            }
        }
        sprinkleDetail(image, new Color(160, 200, 90), rng, 18);
        return image;
    }

    private static BufferedImage generateGrassSide(String name, Random rng) {
        BufferedImage image = noisyFill(color(126, 86, 48), 20, rng);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                Color grass = new Color(70 + rng.nextInt(70), 120 + rng.nextInt(80), 40 + rng.nextInt(40));
                image.setRGB(x, y, grass.getRGB());
            }
        }
        sprinkleDetail(image, new Color(180, 210, 90), rng, 10);
        return image;
    }

    private static BufferedImage generateSandstone(String name, Random rng) {
        BufferedImage image = noisyFill(color(214, 194, 155), 10, rng);
        for (int y = 1; y < TEXTURE_SIZE; y += 4) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if (rng.nextBoolean()) {
                    image.setRGB(x, y, color(200, 182, 140).getRGB());
                }
            }
        }
        return image;
    }

    private static BufferedImage generateCactusTop(String name, Random rng) {
        BufferedImage image = newImage();
        Color base = color(50, 140, 60);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                Color shade = jitter(base, 15, rng);
                image.setRGB(x, y, shade.getRGB());
            }
        }
        // Draw a little star of darker spikes, because cactus.
        drawPlus(image, TEXTURE_SIZE / 2, TEXTURE_SIZE / 2, color(25, 90, 35));
        return image;
    }

    private static BufferedImage generateCactusSide(String name, Random rng) {
        BufferedImage image = newImage();
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            Color base = (x % 4 == 0 || x % 4 == 1) ? color(35, 120, 50) : color(45, 160, 70);
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                Color shade = jitter(base, 18, rng);
                image.setRGB(x, y, shade.getRGB());
            }
        }
        for (int y = 2; y < TEXTURE_SIZE; y += 4) {
            for (int x = 1; x < TEXTURE_SIZE; x += 4) {
                image.setRGB(x, y, color(220, 240, 200).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage generateIce(String name, Random rng) {
        BufferedImage image = newImage();
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int alpha = 150 + rng.nextInt(70);
                Color shade = new Color(170 + rng.nextInt(20), 200 + rng.nextInt(30), 255, alpha);
                image.setRGB(x, y, shade.getRGB());
            }
        }
        drawVein(image, color(130, 170, 230, 200), rng);
        return image;
    }

    private static BufferedImage generateSnowLayer(String name, Random rng) {
        BufferedImage image = newImage();
        // Mostly transparent with little chunky snow bits
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int alpha = rng.nextInt(100) < 15 ? 220 : rng.nextInt(60);
                int tone = 230 + rng.nextInt(25);
                image.setRGB(x, y, new Color(tone, tone, tone, alpha).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage generateJungleGrassTop(String name, Random rng) {
        BufferedImage base = generateGrassTop(name, rng);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int rgb = base.getRGB(x, y);
                Color c = new Color(rgb, true);
                int r = clamp((int) (c.getRed() * 0.8));
                int g = clamp((int) (c.getGreen() * 1.1));
                int b = clamp((int) (c.getBlue() * 0.7));
                base.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        sprinkleDetail(base, new Color(30, 160, 60), rng, 10);
        return base;
    }

    private static BufferedImage generateJungleGrassSide(String name, Random rng) {
        BufferedImage image = noisyFill(color(90, 60, 32), 22, rng);
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                Color grass = new Color(20 + rng.nextInt(80), 120 + rng.nextInt(80), 30 + rng.nextInt(40));
                image.setRGB(x, y, grass.getRGB());
            }
        }
        sprinkleDetail(image, new Color(10, 140, 50), rng, 12);
        return image;
    }

    private static BufferedImage generateWoodTop(String name, Random rng) {
        BufferedImage image = newImage();
        double center = (TEXTURE_SIZE - 1) / 2.0;
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                double dx = x - center;
                double dy = y - center;
                double distance = Math.sqrt(dx * dx + dy * dy);
                int ring = (int) distance;
                Color base = (ring % 2 == 0) ? color(152, 123, 72) : color(134, 102, 58);
                image.setRGB(x, y, jitter(base, 12, rng).getRGB());
            }
        }
        drawNoiseCross(image, color(94, 69, 38), rng);
        return image;
    }

    private static BufferedImage generateWoodSide(String name, Random rng) {
        BufferedImage image = newImage();
        for (int x = 0; x < TEXTURE_SIZE; x++) {
            Color base = (x % 3 == 0) ? color(134, 102, 58) : color(152, 123, 72);
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                image.setRGB(x, y, jitter(base, 10, rng).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage generateLeaves(String name, Random rng) {
        BufferedImage image = newImage();
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                Color base = (y % 2 == 0) ? color(40, 120, 50, 210) : color(20, 100, 30, 200);
                if (rng.nextInt(100) < 10) {
                    base = color(70, 150, 60, 230);
                }
                image.setRGB(x, y, jitter(base, 18, rng).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage generateSparseLeaves(String name, Random rng) {
        BufferedImage image = newImage();
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int alpha = rng.nextInt(100) < 50 ? 200 : 40;
                Color base = color(60, 170, 80, alpha);
                image.setRGB(x, y, jitter(base, 25, rng).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage generateRedFlower(String name, Random rng) {
        return generateFlowerTexture(color(210, 25, 50), color(255, 200, 90), rng);
    }

    private static BufferedImage generateYellowFlower(String name, Random rng) {
        return generateFlowerTexture(color(240, 210, 40), color(255, 240, 180), rng);
    }

    private static BufferedImage generateBlueFlower(String name, Random rng) {
        return generateFlowerTexture(color(80, 120, 210), color(200, 220, 255), rng);
    }

    private static BufferedImage generateFlowerTexture(Color petal, Color pollen, Random rng) {
        BufferedImage image = newImage(0);
        int centerX = TEXTURE_SIZE / 2;
        int centerY = TEXTURE_SIZE / 2;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60 + rng.nextInt(10) - 5);
            int petalX = centerX + (int) Math.round(Math.cos(angle) * 4);
            int petalY = centerY + (int) Math.round(Math.sin(angle) * 4);
            drawCircle(image, petalX, petalY, 2, petal);
        }
        drawCircle(image, centerX, centerY, 2, new Color(pollen.getRed(), pollen.getGreen(), pollen.getBlue(), 240));
        drawStem(image, centerX, TEXTURE_SIZE - 1, color(40, 140, 60));
        return image;
    }

    private static BufferedImage generatePlayerBaseSkin(String name, Random rng) {
        BufferedImage image = newImage();
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(color(220, 180, 150));
            g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
            g.setColor(color(190, 150, 120));
            g.fillRect(2, 2, TEXTURE_SIZE - 4, TEXTURE_SIZE - 4);
            g.setColor(color(255, 255, 255));
            g.fillRect(4, 6, 3, 3);
            g.fillRect(9, 6, 3, 3);
            g.setColor(color(70, 100, 160));
            g.fillRect(4, 7, 3, 2);
            g.fillRect(9, 7, 3, 2);
            g.setColor(color(150, 90, 60));
            g.fillRect(5, 11, 6, 2);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage generateVillagerSkin(String name, Random rng) {
        BufferedImage image = newImage();
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(color(125, 82, 58));
            g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
            g.setColor(color(160, 110, 70));
            g.fillRect(2, 2, TEXTURE_SIZE - 4, TEXTURE_SIZE - 4);
            g.setColor(color(60, 90, 140));
            g.fillRect(4, 6, 8, 3);
            g.setColor(color(45, 60, 100));
            g.fillRect(4, 7, 8, 1);
            g.setColor(color(90, 55, 38));
            g.fillRect(6, 11, 4, 3);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void drawStem(BufferedImage image, int startX, int startY, Color color) {
        for (int y = startY; y >= 0; y--) {
            if (y % 2 == 0) {
                image.setRGB(Math.max(0, startX - 1), y, color.getRGB());
            }
            image.setRGB(startX, y, color.getRGB());
            if (y % 3 == 0) {
                image.setRGB(Math.min(TEXTURE_SIZE - 1, startX + 1), y, color.getRGB());
            }
        }
    }

    private static void drawCircle(BufferedImage image, int centerX, int centerY, int radius, Color color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (centerX + x < 0 || centerX + x >= TEXTURE_SIZE || centerY + y < 0 || centerY + y >= TEXTURE_SIZE) {
                    continue;
                }
                if (x * x + y * y <= radius * radius) {
                    image.setRGB(centerX + x, centerY + y, color.getRGB());
                }
            }
        }
    }

    private static void drawPlus(BufferedImage image, int cx, int cy, Color color) {
        for (int i = -2; i <= 2; i++) {
            if (cx + i >= 0 && cx + i < TEXTURE_SIZE) {
                image.setRGB(cx + i, cy, color.getRGB());
            }
            if (cy + i >= 0 && cy + i < TEXTURE_SIZE) {
                image.setRGB(cx, cy + i, color.getRGB());
            }
        }
    }

    private static void drawNoiseCross(BufferedImage image, Color color, Random rng) {
        for (int i = 0; i < TEXTURE_SIZE; i++) {
            if (rng.nextBoolean()) {
                image.setRGB(i, TEXTURE_SIZE / 2, jitter(color, 10, rng).getRGB());
            }
            if (rng.nextBoolean()) {
                image.setRGB(TEXTURE_SIZE / 2, i, jitter(color, 10, rng).getRGB());
            }
        }
    }

    private static void drawVein(BufferedImage image, Color color, Random rng) {
        int x = rng.nextInt(TEXTURE_SIZE);
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            image.setRGB(x, y, jitter(color, 12, rng).getRGB());
            if (rng.nextBoolean()) {
                x += rng.nextBoolean() ? 1 : -1;
                x = Math.max(1, Math.min(TEXTURE_SIZE - 2, x));
            }
        }
    }

    private static void sprinkleDetail(BufferedImage image, Color color, Random rng, int chance) {
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if (rng.nextInt(100) < chance) {
                    image.setRGB(x, y, jitter(color, 20, rng).getRGB());
                }
            }
        }
    }

    private static Color jitter(Color base, int variance, Random rng) {
        return jitter(base, variance, rng, base.getAlpha());
    }

    private static Color jitter(Color base, int variance, Random rng, int alpha) {
        int r = clamp(base.getRed() + rng.nextInt(variance * 2 + 1) - variance);
        int g = clamp(base.getGreen() + rng.nextInt(variance * 2 + 1) - variance);
        int b = clamp(base.getBlue() + rng.nextInt(variance * 2 + 1) - variance);
        return new Color(r, g, b, alpha);
    }

    private static int clamp(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }

    private static Color color(int r, int g, int b) {
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private static Color color(int r, int g, int b, int a) {
        return new Color(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    private static BufferedImage newImage() {
        return new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static BufferedImage newImage(int alphaFill) {
        BufferedImage image = newImage();
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(0, 0, 0, clamp(alphaFill)));
            g.fillRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
        } finally {
            g.dispose();
        }
        return image;
    }

    @FunctionalInterface
    private interface TextureFactory extends BiFunction<String, Random, BufferedImage> {
        @Override
        BufferedImage apply(String name, Random random);

        default BufferedImage create(String name, Random random) {
            return apply(name, random);
        }
    }
}
