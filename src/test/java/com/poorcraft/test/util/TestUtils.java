package com.poorcraft.test.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.poorcraft.config.Settings;
import com.poorcraft.modding.LuaModLoader;
import com.poorcraft.world.World;
import com.poorcraft.world.chunk.Chunk;
import com.poorcraft.world.chunk.ChunkPos;
import org.junit.jupiter.api.Assertions;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shared helper methods for the PoorCraft automated test suite.
 * Provides resource validation, reusable assertions, and common setup utilities.
 */
public final class TestUtils {

    private static final Path TEMP_DIRECTORY = Paths.get("target", "test-temp");
    private TestUtils() {
        // Utility class
    }

    /**
     * Ensures the specified file exists.
     *
     * @param path absolute or relative path
     * @return optional error message
     */
    public static Optional<String> validateFileExists(String path) {
        Path filePath = Paths.get(path);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            return Optional.empty();
        }
        return Optional.of("File not found: " + filePath.toAbsolutePath());
    }

    /**
     * Ensures the specified directory exists.
     *
     * @param path path to directory
     * @return optional error message
     */
    public static Optional<String> validateDirectoryExists(String path) {
        Path dirPath = Paths.get(path);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            return Optional.empty();
        }
        return Optional.of("Directory not found: " + dirPath.toAbsolutePath());
    }

    /**
     * Validates JSON file syntax and returns parsed content when valid.
     *
     * @param path path to JSON file
     * @return optional error message
     */
    public static Optional<String> validateJsonFile(String path) {
        Path jsonPath = Paths.get(path);
        if (!Files.exists(jsonPath)) {
            return Optional.of("JSON file not found: " + jsonPath.toAbsolutePath());
        }
        try (BufferedReader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || element.isJsonNull()) {
                return Optional.of("JSON file is empty: " + jsonPath.toAbsolutePath());
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.of("Failed to parse JSON at " + jsonPath.toAbsolutePath() + ": " + ex.getMessage());
        }
    }

    /**
     * Performs a lightweight shader validation.
     * Ensures GLSL version directive, main() entry point, and balanced braces.
     *
     * @param path shader file path
     * @return optional error message
     */
    public static Optional<String> validateShaderFile(String path) {
        Path shaderPath = Paths.get(path);
        if (!Files.exists(shaderPath)) {
            return Optional.of("Shader file missing: " + shaderPath.toAbsolutePath());
        }

        try {
            List<String> lines = Files.readAllLines(shaderPath, StandardCharsets.UTF_8);
            if (lines.stream().noneMatch(line -> line.trim().startsWith("#version"))) {
                return Optional.of("Shader missing #version directive: " + shaderPath.toAbsolutePath());
            }
            if (lines.stream().noneMatch(line -> line.contains("void main"))) {
                return Optional.of("Shader missing main() function: " + shaderPath.toAbsolutePath());
            }

            long open = lines.stream().flatMapToInt(String::chars).filter(ch -> ch == '{').count();
            long close = lines.stream().flatMapToInt(String::chars).filter(ch -> ch == '}').count();
            if (open != close) {
                return Optional.of("Unbalanced braces in shader: " + shaderPath.toAbsolutePath());
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.of("Failed to read shader file " + shaderPath.toAbsolutePath() + ": " + e.getMessage());
        }
    }

    /**
     * Creates default headless-friendly settings for tests.
     *
     * @return configured {@link Settings}
     */
    public static Settings createTestSettings() {
        Settings settings = Settings.getDefault();
        settings.window.width = 640;
        settings.window.height = 360;
        settings.window.vsync = false;
        settings.graphics.renderDistance = Math.min(4, settings.graphics.renderDistance);
        settings.graphics.memoryBudgetMB = Math.min(256, settings.graphics.memoryBudgetMB);
        settings.graphics.adaptiveLoading = true;
        settings.multiplayer.autoConnect = false;
        return settings;
    }

    /**
     * Creates a minimal world suitable for deterministic tests.
     *
     * @param seed world seed (0 for random)
     * @return world instance
     */
    public static World createTestWorld(long seed) {
        return new World(seed, true);
    }

    /**
     * Generates or retrieves a single chunk for testing purposes.
     *
     * @param chunkX chunk coordinate X
     * @param chunkZ chunk coordinate Z
     * @return generated chunk
     */
    public static Chunk generateTestChunk(int chunkX, int chunkZ) {
        World world = createTestWorld(1234L);
        return world.getOrCreateChunk(new ChunkPos(chunkX, chunkZ));
    }

    /**
     * Asserts that a resource exists, providing context on failure.
     *
     * @param path resource location
     * @param description human-readable description
     */
    public static void assertResourceExists(String path, String description) {
        Path resource = Paths.get(path);
        Assertions.assertTrue(Files.exists(resource), () -> description + " missing at " + resource.toAbsolutePath());
    }

    /**
     * Checks that no OpenGL errors have occurred.
     */
    public static void assertNoGLErrors() {
        List<String> errors = new ArrayList<>();
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            errors.add("GL error code: " + error);
        }
        if (!errors.isEmpty()) {
            Assertions.fail("OpenGL errors detected: " + String.join(", ", errors));
        }
    }

    /**
     * Validates that the specified mod is loaded via {@link LuaModLoader}.
     *
     * @param modId mod identifier
     * @param loader mod loader instance
     */
    public static void assertModLoaded(String modId, LuaModLoader loader) {
        Assertions.assertNotNull(loader, "LuaModLoader instance must not be null");
        Assertions.assertNotNull(loader.getModById(modId), () -> "Expected mod '" + modId + "' to be loaded");
    }

    /**
     * Creates a temporary directory inside {@code target/test-temp}.
     *
     * @param prefix directory name prefix
     * @return created path
     */
    public static Path createTempDirectory(String prefix) {
        try {
            if (!Files.exists(TEMP_DIRECTORY)) {
                Files.createDirectories(TEMP_DIRECTORY);
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            return Files.createDirectories(TEMP_DIRECTORY.resolve(prefix + "-" + timestamp));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp directory", e);
        }
    }

    /**
     * Wipes temporary resources created during testing.
     */
    public static void cleanupTestResources() {
        if (!Files.exists(TEMP_DIRECTORY)) {
            return;
        }
        try {
            Files.walk(TEMP_DIRECTORY)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // swallow, we do not want cleanup to break tests
                    }
                });
        } catch (IOException ignored) {
            // ignore cleanup failures
        }
    }

    /**
     * Resets commonly mutated OpenGL state between tests.
     */
    public static void resetGLState() {
        GL20.glUseProgram(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    /**
     * Reads a resource from the classpath as a string.
     *
     * @param resource resource path
     * @return file contents
     */
    public static String readClasspathResource(String resource) {
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(TestUtils.class.getClassLoader().getResourceAsStream(resource),
                        "Missing resource: " + resource),
                StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + resource, e);
        }
    }

    /**
     * Validates multiple shader files and aggregates error messages.
     *
     * @param shaderPaths list of shader paths
     * @return list of error messages
     */
    public static List<String> validateShaders(String... shaderPaths) {
        return Arrays.stream(shaderPaths)
            .map(TestUtils::validateShaderFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
}
