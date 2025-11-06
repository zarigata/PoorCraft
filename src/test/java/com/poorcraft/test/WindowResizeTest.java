package com.poorcraft.test;

import com.poorcraft.core.Game;
import com.poorcraft.core.Window;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.config.Settings;
import com.poorcraft.ui.Button;
import com.poorcraft.ui.FontRenderer;
import com.poorcraft.ui.GameState;
import com.poorcraft.ui.MainMenuScreen;
import com.poorcraft.ui.MenuBackground;
import com.poorcraft.ui.MenuButton;
import com.poorcraft.ui.MenuWorldRenderer;
import com.poorcraft.ui.Label;
import com.poorcraft.ui.UIManager;
import com.poorcraft.ui.UIRenderer;
import com.poorcraft.ui.UIScreen;
import com.poorcraft.ui.UIComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
@Tag("resize")
class WindowResizeTest {

    private HeadlessGameContext context;
    private Game game;
    private UIManager uiManager;

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
        context.initializeSubsystem("game");
        game = context.getGame();
        assertNotNull(game, "Game should be initialised for resize tests");
        uiManager = game.getUIManager();
        assertNotNull(uiManager, "UIManager should be available");
        if (context.getSettings() != null && context.getSettings().ai == null) {
            context.getSettings().ai = new Settings.AISettings();
        }
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @DisplayName("Single resize processes after debounce")
    void testSingleResize() throws Exception {
        uiManager.onResize(1920, 1080);
        assertTrue(uiManager.isResizePending(), "Resize should be pending immediately after event");

        waitForDebounce();
        uiManager.update(0.016f);

        assertFalse(uiManager.isResizePending(), "Resize should be processed after debounce interval");
        assertEquals(1920, uiManager.getPendingResizeWidth(), "Stored width should match final resize");
        assertEquals(1080, uiManager.getPendingResizeHeight(), "Stored height should match final resize");
    }

    @Test
    @DisplayName("Rapid resize events are coalesced")
    void testRapidResizeEvents() throws Exception {
        for (int i = 0; i < 5; i++) {
            uiManager.onResize(800 + i * 50, 600 + i * 25);
            Thread.sleep(20);
        }

        waitForDebounce();
        uiManager.update(0.016f);

        assertEquals(800 + 4 * 50, uiManager.getPendingResizeWidth(), "Only final width should apply");
        assertEquals(600 + 4 * 25, uiManager.getPendingResizeHeight(), "Only final height should apply");
        assertFalse(uiManager.isResizePending(), "Resize should have been processed");
    }

    @Test
    @DisplayName("Resize not processed before debounce elapses")
    void testResizeDebounceDelay() throws Exception {
        uiManager.onResize(1280, 720);
        uiManager.update(0.016f);
        assertTrue(uiManager.isResizePending(), "Resize should remain pending before debounce");

        Thread.sleep(Math.max(10L, uiManager.getResizeDebounceMillis() / 2));
        uiManager.update(0.016f);
        assertTrue(uiManager.isResizePending(), "Resize should still be pending before full interval");

        waitForDebounce();
        uiManager.update(0.016f);
        assertFalse(uiManager.isResizePending(), "Resize should be processed after full wait");
    }

    @Test
    @DisplayName("Resize processed when transitioning states")
    void testResizeDuringStateTransition() throws Exception {
        uiManager.onResize(1024, 768);
        waitForDebounce();
        uiManager.setState(GameState.SETTINGS_MENU);
        uiManager.update(0.016f);
        assertFalse(uiManager.isResizePending(), "State transition should not leave resize pending");
    }

    @Test
    @DisplayName("Game blur targets follow resize debounce")
    void testBlurFramebufferResize() throws Exception {
        Window window = game.getWindow();
        assertNotNull(window, "Window should be available");

        Field callbackField = Window.class.getDeclaredField("resizeCallback");
        callbackField.setAccessible(true);
        Object callback = callbackField.get(window);
        assertNotNull(callback, "Resize callback should be registered");

        Method onResize = callback.getClass().getMethod("onResize", int.class, int.class);
        onResize.setAccessible(true);
        onResize.invoke(callback, 1600, 900);

        assertTrue(isBlurResizePending(game), "Blur resize should be pending after callback");
        waitForDebounce();

        invokeProcessBlurDebounce(game);
        assertFalse(isBlurResizePending(game), "Blur resize should have processed after debounce");
    }

    @Test
    @DisplayName("Maximising window updates layout")
    void testMaximizeWindow() throws Exception {
        uiManager.onResize(2560, 1440);
        waitForDebounce();
        uiManager.update(0.016f);

        assertEquals(2560, uiManager.getPendingResizeWidth());
        assertEquals(1440, uiManager.getPendingResizeHeight());
    }

    @Test
    @DisplayName("Minimising window updates layout")
    void testMinimizeWindow() throws Exception {
        uiManager.onResize(640, 480);
        waitForDebounce();
        uiManager.update(0.016f);

        assertEquals(640, uiManager.getPendingResizeWidth());
        assertEquals(480, uiManager.getPendingResizeHeight());
    }

    @Test
    @DisplayName("Resize handled while updating")
    void testConcurrentResizeAndUpdate() throws Exception {
        uiManager.onResize(1100, 700);
        for (int i = 0; i < 3; i++) {
            uiManager.update(0.016f);
            Thread.sleep(30);
        }
        waitForDebounce();
        uiManager.update(0.016f);
        assertFalse(uiManager.isResizePending(), "Resize should resolve even with frequent updates");
    }

    @Test
    @DisplayName("MenuWorldRenderer receives resize after debounce")
    void testMenuWorldRendererResize() throws Exception {
        uiManager.setState(GameState.MAIN_MENU);
        MenuWorldRenderer renderer = uiManager.getMenuWorldRenderer();
        assertNotNull(renderer, "Menu renderer should be initialised in main menu");

        uiManager.onResize(1600, 900);
        waitForDebounce();
        uiManager.update(0.016f);

        assertEquals(1600, getRendererDimension(renderer, "viewportWidth"));
        assertEquals(900, getRendererDimension(renderer, "viewportHeight"));
    }

    @Test
    @DisplayName("Main menu components are not recreated on resize")
    void testMainMenuComponentReuseOnResize() throws Exception {
        uiManager.setState(GameState.MAIN_MENU);
        UIScreen screen = getScreen(GameState.MAIN_MENU);
        assertNotNull(screen, "Main menu screen should be available");

        List<UIComponent> componentsBefore = getComponents(screen);
        Object buttonBefore = componentsBefore.stream()
            .filter(component -> component instanceof MenuButton)
            .findFirst()
            .orElse(null);
        assertNotNull(buttonBefore, "Expected at least one menu button");
        MenuButton primaryButton = (MenuButton) buttonBefore;
        float widthBefore = primaryButton.getWidth();
        float xBefore = primaryButton.getX();

        uiManager.onResize(1400, 900);
        waitForDebounce();
        uiManager.update(0.016f);

        List<UIComponent> componentsAfter = getComponents(screen);
        Object buttonAfter = componentsAfter.stream()
            .filter(component -> component instanceof MenuButton)
            .findFirst()
            .orElse(null);
        assertNotNull(buttonAfter, "Menu button should still be present after resize");
        assertSame(buttonBefore, buttonAfter, "Menu buttons should not be recreated on resize");
        float widthAfter = primaryButton.getWidth();
        float xAfter = primaryButton.getX();
        assertTrue(widthAfter != widthBefore || xAfter != xBefore,
            "Button layout should be recalculated after resize");
    }

    @Test
    @DisplayName("MenuWorldRenderer instance reused across resizes")
    void testMenuWorldRendererReuse() throws Exception {
        uiManager.setState(GameState.MAIN_MENU);
        MenuWorldRenderer initialRenderer = uiManager.getMenuWorldRenderer();
        assertNotNull(initialRenderer, "Menu renderer should exist after entering main menu");

        uiManager.onResize(1920, 1080);
        waitForDebounce();
        uiManager.update(0.016f);

        MenuWorldRenderer rendererAfter = uiManager.getMenuWorldRenderer();
        assertSame(initialRenderer, rendererAfter, "MenuWorldRenderer shouldn't be recreated on resize");
        assertEquals(1920, getRendererDimension(rendererAfter, "viewportWidth"));
        assertEquals(1080, getRendererDimension(rendererAfter, "viewportHeight"));
    }

    @Test
    @DisplayName("Rapid resize events apply only once to screens")
    void testResizeProcessedOnceAfterDebounce() throws Exception {
        AtomicInteger resizeCalls = new AtomicInteger();
        AtomicInteger lastWidth = new AtomicInteger();
        AtomicInteger lastHeight = new AtomicInteger();

        UIScreen instrumentedScreen = new UIScreen(uiManager.getPendingResizeWidth(), uiManager.getPendingResizeHeight()) {
            @Override
            public void init() {
            }

            @Override
            public void onResize(int width, int height) {
                resizeCalls.incrementAndGet();
                lastWidth.set(width);
                lastHeight.set(height);
            }
        };

        uiManager.registerScreen(GameState.SKIN_MANAGER, instrumentedScreen);
        uiManager.setState(GameState.SKIN_MANAGER);

        for (int i = 0; i < 10; i++) {
            uiManager.onResize(800 + i, 600 + i);
            Thread.sleep(5);
        }

        waitForDebounce();
        uiManager.update(0.016f);

        assertEquals(1, resizeCalls.get(), "Resize should have been applied only once after debounce");
        assertEquals(809, lastWidth.get(), "Final width should match last resize event");
        assertEquals(609, lastHeight.get(), "Final height should match last resize event");
    }

    @Test
    @DisplayName("Resize processing stays within performance budget")
    void testResizeProcessingPerformanceBudget() throws Exception {
        uiManager.onResize(1920, 1080);
        waitForDebounce();
        Instant start = Instant.now();
        uiManager.update(0.016f);
        long elapsed = Duration.between(start, Instant.now()).toMillis();

        assertTrue(elapsed < 500, "Resize processing should complete within 500ms");
    }

    @Test
    @DisplayName("Main menu renders with expected opacity and background behaviour")
    void testMainMenuBackgroundOpacityAndRendererBehaviour() throws Exception {
        uiManager.setState(GameState.MAIN_MENU);
        waitForDebounce();
        uiManager.update(0.016f);

        MainMenuScreen mainMenuScreen = (MainMenuScreen) getScreen(GameState.MAIN_MENU);
        assertNotNull(mainMenuScreen, "Main menu should be active");

        float panelOpacity = getPanelOpacity();
        assertTrue(panelOpacity < 0.5f, "Panel opacity should be semi-transparent (<0.5)");

        MenuWorldRenderer renderer = getWorldRenderer(mainMenuScreen);
        if (renderer != null) {
            renderer.update(0.016f);

            uiManager.onResize(1800, 1000);
            waitForDebounce();
            uiManager.update(0.016f);

            MenuWorldRenderer rendererAfter = getWorldRenderer(mainMenuScreen);
            assertSame(renderer, rendererAfter, "Renderer should persist across resize");

            assertEquals(1800, getRendererDimension(rendererAfter, "viewportWidth"));
            assertEquals(1000, getRendererDimension(rendererAfter, "viewportHeight"));
        } else {
            MenuBackground background = getMenuBackground(mainMenuScreen);
            assertNotNull(background, "Fallback background should be available");

            TestUIRenderer rendererSpy = new TestUIRenderer();
            StubFontRenderer fontRenderer = new StubFontRenderer();
            mainMenuScreen.render(rendererSpy, fontRenderer);

            assertTrue(rendererSpy.backgroundTextureRendered,
                "Fallback background should render tiled texture");
            assertEquals(panelOpacity, rendererSpy.lastPanelOpacity, 1e-5f,
                "Panel opacity should match configuration");
        }
    }

    @ParameterizedTest(name = "Screen {0} recalculates layout without recreating components")
    @MethodSource("screenStatesForLayoutValidation")
    void testScreensRecalculateLayoutOnResize(GameState state) throws Exception {
        UIScreen screen = activateScreen(state);

        UIComponent trackedComponent = selectStableComponent(screen);
        assertNotNull(trackedComponent, () -> "No stable component found for " + state);

        float widthBefore = trackedComponent.getWidth();
        float xBefore = trackedComponent.getX();

        uiManager.onResize(1600, 900);
        settleUI();

        List<UIComponent> componentsAfter = getComponents(screen);
        UIComponent matchedComponent = componentsAfter.stream()
            .filter(component -> component == trackedComponent)
            .findFirst()
            .orElse(null);
        assertNotNull(matchedComponent, () -> "Tracked component should remain attached to screen for " + state);

        float widthAfter = trackedComponent.getWidth();
        float xAfter = trackedComponent.getX();
        assertTrue(Math.abs(widthAfter - widthBefore) > 1e-3f || Math.abs(xAfter - xBefore) > 1e-3f,
            "Component layout should change after resize");
    }

    private static Stream<Arguments> screenStatesForLayoutValidation() {
        return Stream.of(
            Arguments.of(GameState.MAIN_MENU),
            Arguments.of(GameState.SETTINGS_MENU),
            Arguments.of(GameState.SKIN_MANAGER),
            Arguments.of(GameState.MULTIPLAYER_MENU),
            Arguments.of(GameState.PAUSED),
            Arguments.of(GameState.WORLD_CREATION),
            Arguments.of(GameState.SKIN_EDITOR),
            Arguments.of(GameState.HOSTING),
            Arguments.of(GameState.CONNECTING)
        );
    }

    private UIComponent selectStableComponent(UIScreen screen) throws Exception {
        List<UIComponent> components = getComponents(screen);
        if (components == null || components.isEmpty()) {
            return null;
        }
        return components.stream()
            .filter(MenuButton.class::isInstance)
            .findFirst()
            .or(() -> components.stream().filter(Button.class::isInstance).findFirst())
            .or(() -> components.stream().filter(Label.class::isInstance).findFirst())
            .orElse(components.get(0));
    }

    private UIScreen activateScreen(GameState state) throws Exception {
        ensurePrerequisiteState(state);
        uiManager.setState(state);
        settleUI();

        UIScreen screen = getScreen(state);
        assertNotNull(screen, () -> "Expected screen for state " + state);
        return screen;
    }

    private void ensurePrerequisiteState(GameState state) throws Exception {
        switch (state) {
            case PAUSED -> {
                uiManager.setState(GameState.IN_GAME);
                settleUI();
            }
            case WORLD_CREATION, SETTINGS_MENU, SKIN_MANAGER, MULTIPLAYER_MENU -> {
                uiManager.setState(GameState.MAIN_MENU);
                settleUI();
            }
            case SKIN_EDITOR -> {
                uiManager.setState(GameState.SKIN_MANAGER);
                settleUI();
            }
            case HOSTING, CONNECTING -> {
                uiManager.setState(GameState.MULTIPLAYER_MENU);
                settleUI();
            }
            default -> {
            }
        }
    }

    private void settleUI() throws Exception {
        waitForDebounce();
        uiManager.update(0.016f);
    }

    private float getPanelOpacity() throws Exception {
        Field field = MainMenuScreen.class.getDeclaredField("PANEL_OPACITY");
        field.setAccessible(true);
        return field.getFloat(null);
    }

    private MenuBackground getMenuBackground(MainMenuScreen screen) throws Exception {
        Field field = MainMenuScreen.class.getDeclaredField("background");
        field.setAccessible(true);
        return (MenuBackground) field.get(screen);
    }

    private MenuWorldRenderer getWorldRenderer(MainMenuScreen screen) throws Exception {
        Field field = MainMenuScreen.class.getDeclaredField("worldRenderer");
        field.setAccessible(true);
        return (MenuWorldRenderer) field.get(screen);
    }

    private void waitForDebounce() throws InterruptedException {
        Thread.sleep(uiManager.getResizeDebounceMillis() + 50L);
    }

    @SuppressWarnings("unchecked")
    private UIScreen getScreen(GameState state) throws Exception {
        Field screensField = UIManager.class.getDeclaredField("screens");
        screensField.setAccessible(true);
        Map<GameState, UIScreen> screens = (Map<GameState, UIScreen>) screensField.get(uiManager);
        return screens.get(state);
    }

    @SuppressWarnings("unchecked")
    private List<UIComponent> getComponents(UIScreen screen) throws Exception {
        Field componentsField = UIScreen.class.getDeclaredField("components");
        componentsField.setAccessible(true);
        return (List<UIComponent>) componentsField.get(screen);
    }

    private boolean isBlurResizePending(Game target) throws Exception {
        Field field = Game.class.getDeclaredField("blurResizePending");
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private void invokeProcessBlurDebounce(Game target) throws Exception {
        Method method = Game.class.getDeclaredMethod("processBlurResizeDebounce");
        method.setAccessible(true);
        method.invoke(target);
    }

    private int getRendererDimension(MenuWorldRenderer renderer, String fieldName) throws Exception {
        Field field = MenuWorldRenderer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(renderer);
    }

    private static class TestUIRenderer extends UIRenderer {
        private boolean backgroundTextureRendered;
        private float lastPanelOpacity = Float.NaN;

        @Override
        public void drawRect(float x, float y, float width, float height,
                             float r, float g, float b, float a) {
            // Record that base background drew
        }

        @Override
        public void drawTexturedRect(float x, float y, float width, float height, int textureId,
                                     float r, float g, float b, float a) {
            backgroundTextureRendered = true;
        }

        @Override
        public void drawOutsetPanel(float x, float y, float width, float height,
                                    float borderR, float borderG, float borderB, float opacity) {
            lastPanelOpacity = opacity;
        }

        @Override
        public void drawDropShadow(float x, float y, float width, float height,
                                   float shadowOffset, float shadowAlpha) {
            // no-op to avoid GL usage
        }
    }

    private static class StubFontRenderer extends FontRenderer {
        StubFontRenderer() {
            super(new NoopUIRenderer(), 16);
        }

        public void drawText(String text, float x, float y, float r, float g, float b, float a) {
            // no-op
        }

        public void drawText(String text, float x, float y, float scale, float r, float g, float b, float a) {
            // no-op
        }

        public void drawTextWithShadow(String text, float x, float y, float scale, float r, float g, float b, float a,
                                       float shadowOffset, float shadowAlpha) {
            // no-op
        }

        public float getTextWidth(String text) {
            return text == null ? 0f : text.length() * 8f;
        }
    }

    private static class NoopUIRenderer extends UIRenderer {
        @Override
        public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
            // no-op
        }

        @Override
        public void drawDropShadow(float x, float y, float width, float height, float shadowOffset, float shadowAlpha) {
            // no-op
        }

        @Override
        public void drawOutsetPanel(float x, float y, float width, float height,
                                    float borderR, float borderG, float borderB, float opacity) {
            // no-op
        }

        @Override
        public void drawTexturedRect(float x, float y, float width, float height, int textureId,
                                     float r, float g, float b, float a) {
            // no-op
        }
    }
}
