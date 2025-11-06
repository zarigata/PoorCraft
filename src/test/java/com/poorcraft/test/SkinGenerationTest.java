package com.poorcraft.test;

import com.poorcraft.config.Settings;
import com.poorcraft.player.PlayerSkin;
import com.poorcraft.player.SkinGenerator;
import com.poorcraft.player.SkinLoader;
import com.poorcraft.player.SkinManager;
import com.poorcraft.resources.AssetManager;
import com.poorcraft.test.util.HeadlessGameContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests covering procedural skin generation and loading behaviours.
 */
class SkinGenerationTest {

    private HeadlessGameContext context;
    private SkinManager skinManager;
    private AssetManager assetManager;
    private final List<Path> generatedFiles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
        context.initializeSubsystem("window");
        skinManager = SkinManager.getInstance();
        skinManager.cleanup();
        assetManager = AssetManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        try {
            if (skinManager != null) {
                skinManager.cleanup();
            }
        } finally {
            if (context != null) {
                context.cleanup();
            }
            cleanupGeneratedFiles();
        }
    }

    @Test
    @DisplayName("Generated skins are 64x64 with non-empty pixels")
    void generatedSkinHasCorrectDimensions() throws IOException {
        Path tempSkin = Files.createTempFile("test-generated", ".png");
        BufferedImage image = SkinGenerator.generateDefaultSkin("testGenerated", tempSkin);
        assertNotNull(image, "Generator should return an image");
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());

        long nonTransparent = countNonTransparent(image);
        assertTrue(nonTransparent > 64, "Generated skin should not be mostly transparent");
    }

    @Test
    @DisplayName("Missing default skins are generated and loaded once")
    void missingDefaultSkinGeneratesAndRegisters() {
        String skinId = "auto-default-" + System.nanoTime();
        Path skinPath = assetManager.getDefaultSkinPath(skinId);
        try {
            Files.deleteIfExists(skinPath);
        } catch (IOException ignored) {
        }

        boolean ensured = skinManager.ensureSkinExists(skinId, true);
        assertTrue(ensured, "Skin manager should generate missing default skin");

        PlayerSkin generated = skinManager.getSkin(skinId);
        assertNotNull(generated, "Generated skin should be registered");
        assertTrue(generated.isDefault(), "Generated default skin should be flagged as default");
        assertTrue(Files.exists(skinPath), "Generated skin file should exist on disk");
        assertTrue(skinManager.getAtlas().getTextureId(skinId) > 0, "Generated skin should upload to atlas");

        generatedFiles.add(skinPath);
    }

    @Test
    @DisplayName("SkinManager fallback chain handles missing skins gracefully")
    void fallbackChainHandlesMissingSkin() {
        Settings settings = Settings.getDefault();
        String missingSkin = "missing-" + System.nanoTime();
        settings.player.selectedSkin = missingSkin;

        skinManager.init(settings);

        PlayerSkin current = skinManager.getCurrentSkin();
        assertNotNull(current, "Fallback should select a skin");
        assertEquals("steve", current.getName(), "Fallback should default to steve");
        assertTrue(skinManager.getAtlas().getTextureId(current.getName()) > 0);
    }

    @Test
    @DisplayName("Corrupt PNG files are rejected without crashing")
    void corruptFilesFailGracefully(@TempDir Path tempDir) throws IOException {
        SkinLoader loader = new SkinLoader();
        Path corrupt = tempDir.resolve("skins/corrupt.png");
        Files.createDirectories(corrupt.getParent());
        Files.writeString(corrupt, "not a png");

        PlayerSkin skin = loader.loadFromFile(corrupt, false);
        assertNull(skin, "Corrupt skin should not load");
    }

    @Test
    @DisplayName("Skin directories are created automatically when generating")
    void directoriesCreatedForGeneratedSkins(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("skins/default/newskin.png");
        assertFalse(Files.exists(output.getParent()));

        BufferedImage generated = SkinGenerator.generateDefaultSkin("newskin", output);
        assertNotNull(generated);
        assertTrue(Files.exists(output));
        assertTrue(Files.exists(output.getParent()));
    }

    private static long countNonTransparent(BufferedImage image) {
        long count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private void cleanupGeneratedFiles() {
        for (Path path : generatedFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
        generatedFiles.clear();
    }
}
