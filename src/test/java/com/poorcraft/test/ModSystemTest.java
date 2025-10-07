package com.poorcraft.test;

import com.google.gson.JsonObject;
import com.poorcraft.modding.EventBus;
import com.poorcraft.modding.LuaModContainer;
import com.poorcraft.modding.LuaModLoader;
import com.poorcraft.modding.ModAPI;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.test.util.TestReportGenerator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the Lua modding system including discovery, lifecycle, API surface,
 * configuration exposure, and error handling behaviours.
 */
@Tag("mods")
class ModSystemTest {

    private static final TestReportGenerator REPORT = new TestReportGenerator();
    private static final Path MODS_DIRECTORY = Paths.get("gamedata", "mods");

    private HeadlessGameContext context;
    private final List<Path> transientMods = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        REPORT.addSystemInfo("Mods Directory", MODS_DIRECTORY.toAbsolutePath().toString());
    }

    @AfterAll
    static void afterAll() {
        REPORT.generateConsoleReport();
        REPORT.generateMarkdownReport();
        REPORT.generateHtmlReport();
    }

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.cleanup();
        }
        for (Path modPath : transientMods) {
            try {
                Files.walk(modPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
            } catch (IOException ignored) {
            }
        }
        transientMods.clear();
    }
    @Test
    @DisplayName("Mod discovery loads expected mods")
    void testModDiscovery() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        List<LuaModContainer> mods = loader.getLoadedMods();
        boolean discovered = !mods.isEmpty();
        REPORT.addTestResult("Modding", "testModDiscovery", discovered,
            "Discovered mods: " + mods.stream().map(LuaModContainer::getId).toList());
        assertTrue(discovered, "Expected at least one mod to be discovered");
    }

    @Test
    @DisplayName("Mod lifecycle reaches ENABLED state")
    void testModLoading() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        List<LuaModContainer> mods = loader.getLoadedMods();
        boolean allEnabled = mods.stream().allMatch(mod -> mod.getState() == LuaModContainer.ModState.ENABLED
            || mod.getState() == LuaModContainer.ModState.ERROR);
        REPORT.addTestResult("Modding", "testModLoading", allEnabled,
            "Mod states: " + mods.stream()
                .map(mod -> mod.getId() + "=" + mod.getState())
                .toList());
        assertTrue(allEnabled, "All mods should reach ENABLED state unless in ERROR");
    }

    @Test
    @DisplayName("Mod API surface available")
    void testModAPIAccess() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        ModAPI api = loader.getModAPI();
        assertNotNull(api, "Mod API should not be null");
        api.log("Mod API smoke test log entry");
        float time = api.getGameTime();
        REPORT.addTestResult("Modding", "testModAPIAccess", time >= -1.0f,
            "Game time reported as " + time);
    }

    @Test
    @DisplayName("Event bus registers callbacks")
    void testEventSystem() {
        context.initializeSubsystem("game");
        EventBus eventBus = context.getModLoader().getEventBus();
        assertNotNull(eventBus, "Event bus must be present");
        Object dummyCallback = new Object();
        eventBus.registerCallback("unit_test_event", dummyCallback);
        int callbackCount = eventBus.getCallbackCount("unit_test_event");
        REPORT.addTestResult("Modding", "testEventSystem", callbackCount == 1,
            "Callback count=" + callbackCount);
        assertEquals(1, callbackCount, "Event bus should contain registered callback");
    }

    @Test
    @DisplayName("Mod configuration exposed via API")
    void testModConfiguration() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        LuaModContainer container = loader.getModById("realtime_sync");
        Assumptions.assumeTrue(container != null, "realtime_sync mod not available");
        JsonObject config = container.getConfig();
        boolean hasTimeScale = config.has("time_scale");
        REPORT.addTestResult("Modding", "testModConfiguration", hasTimeScale,
            "Config keys=" + config.keySet());
        assertTrue(hasTimeScale, "realtime_sync config should expose time_scale");
    }

    @Test
    @DisplayName("Faulty mod errors do not crash loader")
    void testModErrorHandling() throws IOException {
        createFaultyMod();
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        LuaModContainer container = loader.getModById("faulty_test_mod");
        assertNotNull(container, "Faulty mod should be discovered");
        boolean errorState = container.getState() == LuaModContainer.ModState.ERROR
            || container.getState() == LuaModContainer.ModState.LOADED;
        REPORT.addTestResult("Modding", "testModErrorHandling", errorState,
            "Faulty mod state=" + container.getState());
        assertTrue(errorState, "Faulty mod should not reach ENABLED");
    }

    @Test
    @DisplayName("Realtime sync mod exposes expected behaviour")
    void testRealTimeSyncMod() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        LuaModContainer container = loader.getModById("realtime_sync");
        Assumptions.assumeTrue(container != null, "realtime_sync mod not available");
        boolean enabled = container.getState() == LuaModContainer.ModState.ENABLED;
        ModAPI api = loader.getModAPI();
        api.setTimeControlEnabled(true);
        float time = api.getGameTime();
        REPORT.addTestResult("Modding", "testRealTimeSyncMod", enabled,
            "Mod state=" + container.getState() + ", time=" + time);
        assertTrue(enabled, "Realtime sync mod should be enabled");
    }

    @Test
    @DisplayName("Lua mods maintain isolated globals")
    void testModIsolation() throws IOException {
        createLuaMod("isolation_alpha", "Isolation Alpha", "return {\n  init = function()\n    local observed = tostring(_G.isolation_probe)\n    api.set_shared_data(\"isolation_alpha_observed\", observed)\n    _G.isolation_probe = \"alpha\"\n    api.set_shared_data(\"isolation_alpha_written\", _G.isolation_probe)\n  end\n}");

        createLuaMod("isolation_beta", "Isolation Beta", "return {\n  init = function()\n    local observed = tostring(_G.isolation_probe)\n    api.set_shared_data(\"isolation_beta_observed\", observed)\n    _G.isolation_probe = \"beta\"\n    api.set_shared_data(\"isolation_beta_written\", _G.isolation_probe)\n  end\n}");

        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        ModAPI api = loader.getModAPI();

        assertEquals("alpha", api.getSharedData("isolation_alpha_written"), "Alpha mod should set probe to alpha");
        assertEquals("beta", api.getSharedData("isolation_beta_written"), "Beta mod should set probe to beta");
        assertEquals("nil", api.getSharedData("isolation_alpha_observed"), "Alpha mod should start with clean globals");
        assertEquals("nil", api.getSharedData("isolation_beta_observed"), "Beta mod should not see alpha globals");
    }

    @Test
    @DisplayName("Shared data round-trips across mods")
    void testSharedDataRoundTrip() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        ModAPI api = loader.getModAPI();

        Map<String, Object> payload = new HashMap<>();
        payload.put("int", 42);
        payload.put("float", 3.14f);
        payload.put("flag", Boolean.TRUE);
        payload.put("text", "hello");

        api.setSharedData("roundTripPayload", payload);

        Object stored = api.getSharedData("roundTripPayload");
        assertInstanceOf(Map.class, stored, "Shared data should retain map structure");
        @SuppressWarnings("unchecked")
        Map<String, Object> restored = (Map<String, Object>) stored;
        assertEquals(42, restored.get("int"));
        assertEquals(3.14f, restored.get("float"));
        assertEquals(Boolean.TRUE, restored.get("flag"));
        assertEquals("hello", restored.get("text"));
    }

    @Test
    @DisplayName("Event callbacks respect cancellation")
    void testEventCancellation() {
        context.initializeSubsystem("game");
        LuaModLoader loader = context.getModLoader();
        EventBus eventBus = loader.getEventBus();

        AtomicBoolean firstCalled = new AtomicBoolean(false);
        AtomicBoolean secondCalled = new AtomicBoolean(false);

        eventBus.registerCallback("block_place", (EventBusCallback) event -> {
            firstCalled.set(true);
            event.cancel();
        });
        eventBus.registerCallback("block_place", (EventBusCallback) event -> secondCalled.set(true));

        eventBus.fire(new com.poorcraft.modding.events.BlockPlaceEvent(0, 0, 0, 1, -1));

        assertTrue(firstCalled.get(), "First callback should run and cancel event");
        assertFalse(secondCalled.get(), "Second callback should not run after cancellation");

        eventBus.clearCallbacks("block_place");
    }

    @FunctionalInterface
    private interface EventBusCallback extends java.util.function.Consumer<com.poorcraft.modding.events.Event> {
        @Override
        void accept(com.poorcraft.modding.events.Event event);
    }

    private void createFaultyMod() throws IOException {
        Path faultyMod = MODS_DIRECTORY.resolve("faulty_test_mod");
        Files.createDirectories(faultyMod);
        JsonObject meta = new JsonObject();
        meta.addProperty("id", "faulty_test_mod");
        meta.addProperty("name", "Faulty Test Mod");
        meta.addProperty("version", "0.0.1");
        meta.addProperty("main", "main.lua");
        meta.addProperty("enabled", true);
        Files.writeString(faultyMod.resolve("mod.json"), meta.toString(), StandardCharsets.UTF_8);
        String script = "return { init = function() error('intentional failure') end }";
        Files.writeString(faultyMod.resolve("main.lua"), script, StandardCharsets.UTF_8);
        transientMods.add(faultyMod);
    }

    private void createLuaMod(String id, String name, String scriptBody) throws IOException {
        Path modDir = MODS_DIRECTORY.resolve(id);
        Files.createDirectories(modDir);
        JsonObject meta = new JsonObject();
        meta.addProperty("id", id);
        meta.addProperty("name", name);
        meta.addProperty("version", "1.0.0");
        meta.addProperty("main", "main.lua");
        meta.addProperty("enabled", true);
        Files.writeString(modDir.resolve("mod.json"), meta.toString(), StandardCharsets.UTF_8);
        Files.writeString(modDir.resolve("main.lua"), scriptBody, StandardCharsets.UTF_8);
        transientMods.add(modDir);
    }
}
