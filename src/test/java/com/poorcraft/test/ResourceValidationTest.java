package com.poorcraft.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poorcraft.resources.ResourceManager;
import com.poorcraft.test.util.TestReportGenerator;
import com.poorcraft.test.util.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates resource integrity without booting the full engine.
 */
class ResourceValidationTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();
    private static final Path SHADER_DIRECTORY = Paths.get("src", "main", "resources", "shaders");
    private static final Path CONFIG_DIRECTORY = Paths.get("src", "main", "resources", "config");
    private static final Path MODS_DIRECTORY = Paths.get("gamedata", "mods");
    private static final Path FONT_PATH = Paths.get("src", "main", "resources", "fonts", "Silkscreen-Regular.ttf");

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Working Directory", Paths.get("").toAbsolutePath().toString());
        REPORT.addSystemInfo("Java Version", System.getProperty("java.version", "unknown"));
    }

    private ShaderSource loadShaderSource(String shader) {
        String classpathLocation = "shaders/" + shader;
        byte[] data = readClasspathResource(classpathLocation);
        if (data != null) {
            return new ShaderSource(shader, "classpath:" + classpathLocation, new String(data, StandardCharsets.UTF_8));
        }

        Path filesystemPath = SHADER_DIRECTORY.resolve(shader);
        if (Files.exists(filesystemPath)) {
            try {
                String content = Files.readString(filesystemPath, StandardCharsets.UTF_8);
                return new ShaderSource(shader, filesystemPath.toString(), content);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private void validateShaderContent(String shaderName, ShaderSource source, List<String> errors) {
        String content = source.content();
        if (!content.contains("#version")) {
            errors.add(shaderName + " missing #version directive (" + source.origin() + ")");
        }
        if (!content.contains("void main")) {
            errors.add(shaderName + " missing main() function (" + source.origin() + ")");
        }

        long openBraces = content.chars().filter(ch -> ch == '{').count();
        long closeBraces = content.chars().filter(ch -> ch == '}').count();
        if (openBraces != closeBraces) {
            errors.add(shaderName + " has unbalanced braces (" + source.origin() + ")");
        }
    }

    private byte[] readClasspathResource(String resource) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ResourceValidationTest.class.getClassLoader();
        }
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private record ShaderSource(String name, String origin, String content) {
    }

    @AfterAll
    static void afterAll() {
        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();
    }

    @Test
    @DisplayName("Shader files exist")
    void testShaderFilesExist() {
        ResourceManager resourceManager = ResourceManager.getInstance();
        List<String> missing = new ArrayList<>();
        List<String> resolved = new ArrayList<>();
        String[] shaderFiles = {
            "block.vert", "block.frag",
            "sky.vert", "sky.frag",
            "ui.vert", "ui.frag",
            "blur.vert", "blur.frag",
            "block_overlay.vert", "block_overlay.frag",
            "item_drop.vert", "item_drop.frag"
        };
        for (String shader : shaderFiles) {
            String classpathLocation = "/shaders/" + shader;
            if (resourceManager.resourceExists(classpathLocation)) {
                resolved.add(shader + " (classpath)");
                continue;
            }
            Path filesystemPath = SHADER_DIRECTORY.resolve(shader);
            if (Files.exists(filesystemPath)) {
                resolved.add(shader + " (filesystem:" + filesystemPath + ")");
            } else {
                missing.add(shader);
            }
        }
        boolean passed = missing.isEmpty();
        String message = passed
            ? "Shaders resolved: " + String.join(", ", resolved)
            : "Missing shaders: " + String.join(", ", missing);
        REPORT.addTestResult("Resources", "testShaderFilesExist", passed, message);
        assertTrue(missing.isEmpty(), "Missing shader files: " + missing);
    }

    @Test
    @DisplayName("Shader syntax validation")
    void testShaderSyntax() {
        List<String> errors = new ArrayList<>();
        String[] shaderFiles = {
            "block.vert", "block.frag",
            "sky.vert", "sky.frag",
            "ui.vert", "ui.frag",
            "blur.vert", "blur.frag",
            "block_overlay.vert", "block_overlay.frag",
            "item_drop.vert", "item_drop.frag"
        };

        for (String shader : shaderFiles) {
            ShaderSource source = loadShaderSource(shader);
            if (source == null) {
                errors.add("Unable to locate shader " + shader + " on classpath or filesystem");
                continue;
            }
            validateShaderContent(shader, source, errors);
        }
        boolean passed = errors.isEmpty();
        REPORT.addTestResult("Resources", "testShaderSyntax", passed, passed
            ? "All shaders passed syntax checks"
            : String.join("; ", errors));
        assertTrue(passed, () -> "Shader syntax issues: " + String.join(", ", errors));
    }

    @Test
    @DisplayName("Configuration files present and valid JSON")
    void testConfigFilesExist() throws IOException {
        Path defaultSettings = CONFIG_DIRECTORY.resolve("default_settings.json");
        List<String> errors = new ArrayList<>();
        Optional<String> validationError = TestUtils.validateJsonFile(defaultSettings.toString());
        validationError.ifPresent(errors::add);
        if (Files.exists(defaultSettings)) {
            String json = Files.readString(defaultSettings, StandardCharsets.UTF_8);
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                errors.add("default_settings.json is not a JSON object");
            } else {
                JsonObject obj = element.getAsJsonObject();
                assertAll(
                    () -> assertTrue(obj.has("window"), "Settings missing window section"),
                    () -> assertTrue(obj.has("graphics"), "Settings missing graphics section"),
                    () -> assertTrue(obj.has("audio"), "Settings missing audio section"),
                    () -> assertTrue(obj.has("controls"), "Settings missing controls section")
                );
            }
        } else {
            errors.add("Missing configuration file: " + defaultSettings.toAbsolutePath());
        }
        boolean passed = errors.isEmpty();
        REPORT.addTestResult("Resources", "testConfigFilesExist", passed,
            passed ? "default_settings.json validated" : String.join("; ", errors));
        assertTrue(passed, () -> String.join("; ", errors));
    }

    @Test
    @DisplayName("Lua mod structure validation")
    void testModStructure() throws IOException {
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(MODS_DIRECTORY)) {
            problems.add("Mods directory missing: " + MODS_DIRECTORY.toAbsolutePath());
        } else {
            try (Stream<Path> mods = Files.list(MODS_DIRECTORY)) {
                List<Path> modDirs = mods.filter(Files::isDirectory).collect(Collectors.toList());
                for (Path modDir : modDirs) {
                    Path modJson = modDir.resolve("mod.json");
                    if (!Files.exists(modJson)) {
                        problems.add("Mod missing mod.json: " + modDir.getFileName());
                        continue;
                    }
                    Optional<String> jsonValidation = TestUtils.validateJsonFile(modJson.toString());
                    jsonValidation.ifPresent(problems::add);
                    if (jsonValidation.isEmpty()) {
                        String json = Files.readString(modJson, StandardCharsets.UTF_8);
                        JsonObject meta = JsonParser.parseString(json).getAsJsonObject();
                        assertTrue(meta.has("id"), "Mod missing id: " + modDir.getFileName());
                        assertTrue(meta.has("main"), "Mod missing main script: " + modDir.getFileName());
                        if (meta.has("main")) {
                            Path mainLua = modDir.resolve(meta.get("main").getAsString());
                            if (!Files.exists(mainLua)) {
                                problems.add("Main Lua script not found for mod " + modDir.getFileName());
                            } else {
                                Optional<String> luaError = validateLuaSyntax(mainLua);
                                luaError.ifPresent(problems::add);
                            }
                        }
                    }
                }
            }
        }
        boolean passed = problems.isEmpty();
        REPORT.addTestResult("Resources", "testModStructure", passed,
            passed ? "All mods passed structure checks" : String.join("; ", problems));
        assertTrue(passed, () -> String.join("; ", problems));
    }

    @Test
    @DisplayName("Font assets available")
    void testFontFilesExist() throws IOException {
        List<String> issues = new ArrayList<>();
        String resolution;
        byte[] fontBytes = readClasspathResource("fonts/Silkscreen-Regular.ttf");
        if (fontBytes != null) {
            resolution = "classpath";
        } else if (Files.exists(FONT_PATH)) {
            fontBytes = Files.readAllBytes(FONT_PATH);
            resolution = "filesystem:" + FONT_PATH.toAbsolutePath();
        } else {
            issues.add("Font missing: " + FONT_PATH.toAbsolutePath());
            resolution = "unresolved";
        }

        if (fontBytes != null && fontBytes.length < 10_000) {
            issues.add("Font data too small (likely corrupt): " + fontBytes.length + " bytes");
        }

        boolean passed = issues.isEmpty();
        REPORT.addTestResult("Resources", "testFontFilesExist", passed,
            passed ? "Font validated via " + resolution : String.join("; ", issues));
        assertTrue(passed, () -> String.join("; ", issues));
    }

    @Test
    @DisplayName("Directory layout checks")
    void testDirectoryStructure() {
        String[] directories = {
            "gamedata/mods",
            "gamedata/worlds",
            "gamedata/screenshots",
            "gamedata/skins",
            "gamedata/config",
            "assets/ui",
            "assets/scripts"
        };
        List<String> missing = new ArrayList<>();
        for (String directory : directories) {
            Optional<String> result = TestUtils.validateDirectoryExists(directory);
            result.ifPresent(missing::add);
        }
        boolean passed = missing.isEmpty();
        REPORT.addTestResult("Resources", "testDirectoryStructure", passed,
            passed ? "All required directories present" : String.join("; ", missing));
        assertTrue(passed, () -> String.join("; ", missing));
    }

    private Optional<String> validateLuaSyntax(Path luaFile) {
        try {
            String source = Files.readString(luaFile, StandardCharsets.UTF_8);
            int balance = 0;
            for (String line : source.split("\\R")) {
                String trimmed = line.trim().toLowerCase();
                if (trimmed.startsWith("function")) {
                    balance++;
                }
                if (trimmed.equals("end") || trimmed.startsWith("end ")) {
                    balance--;
                }
            }
            if (balance != 0) {
                return Optional.of("Unbalanced function/end in " + luaFile.getFileName());
            }
        } catch (IOException e) {
            return Optional.of("Failed to read Lua file " + luaFile + ": " + e.getMessage());
        }
        return Optional.empty();
    }
}
