package com.poorcraft.test;

import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;
import com.poorcraft.test.util.HeadlessGameContext;
import com.poorcraft.ui.AICompanionScreen;
import com.poorcraft.ui.Checkbox;
import com.poorcraft.ui.GameState;
import com.poorcraft.ui.MenuBackground;
import com.poorcraft.ui.MenuButton;
import com.poorcraft.ui.ScrollContainer;
import com.poorcraft.ui.UIComponent;
import com.poorcraft.ui.UIManager;
import com.poorcraft.ui.UIScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AICompanionScreenTest {

    private HeadlessGameContext context;
    private Game game;
    private UIManager uiManager;

    @BeforeEach
    void setUp() {
        context = new HeadlessGameContext();
        context.initialize();
        context.initializeSubsystem("game");
        game = context.getGame();
        assertNotNull(game, "Game should be initialised for AI companion tests");
        uiManager = game.getUIManager();
        assertNotNull(uiManager, "UIManager should be available for AI companion tests");

        Settings settings = context.getSettings();
        if (settings != null && settings.ai == null) {
            settings.ai = new Settings.AISettings();
        }
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.cleanup();
        }
    }

    @Test
    @DisplayName("AI companion screen initialises and includes menu chrome")
    void testScreenInitialisation() throws Exception {
        AICompanionScreen screen = activateAICompanionScreen();
        assertEquals(GameState.AI_COMPANION_SETTINGS, uiManager.getCurrentState(),
            "State should remain on AI companion after init");

        List<UIComponent> components = getComponents(screen);
        long menuButtonCount = components.stream().filter(MenuButton.class::isInstance).count();
        assertEquals(3, menuButtonCount, "AI companion screen should expose Save, Cancel, and Test buttons");

        MenuBackground background = getBackground(screen);
        assertNotNull(background, "AI companion screen should provide menu background");

        float panelWidth = getFloatField(screen, "panelWidth");
        float panelHeight = getFloatField(screen, "panelHeight");
        assertTrue(panelWidth > 0f, "Panel width should be greater than zero after layout");
        assertTrue(panelHeight > 0f, "Panel height should be greater than zero after layout");
    }

    @Test
    @DisplayName("Save button returns to settings menu")
    void testSaveButtonReturnsToSettingsMenu() throws Exception {
        AICompanionScreen screen = activateAICompanionScreen();
        MenuButton saveButton = findButton(screen, "Save");
        assertNotNull(saveButton, "Save button should be present on AI companion screen");

        clickButton(saveButton);

        assertEquals(GameState.SETTINGS_MENU, uiManager.getCurrentState(),
            "Save should transition back to the settings menu");
    }

    @Test
    @DisplayName("Cancel button returns to settings menu")
    void testCancelButtonReturnsToSettingsMenu() throws Exception {
        AICompanionScreen screen = activateAICompanionScreen();
        MenuButton cancelButton = findButton(screen, "Cancel");
        assertNotNull(cancelButton, "Cancel button should be present on AI companion screen");

        clickButton(cancelButton);

        assertEquals(GameState.SETTINGS_MENU, uiManager.getCurrentState(),
            "Cancel should transition back to the settings menu");
    }

    @Test
    @DisplayName("Test action logs warning and tolerates missing game")
    void testTestActionHandlesMissingGame() throws Exception {
        AICompanionScreen screen = activateAICompanionScreen();
        Checkbox aiEnabled = getCheckbox(screen, "aiEnabledCheckbox");
        assertNotNull(aiEnabled, "AI enabled checkbox should be available for test action");
        if (!aiEnabled.isChecked()) {
            aiEnabled.toggle();
        }
        MenuButton testButton = findButton(screen, "Test");
        assertNotNull(testButton, "Test button should be present on AI companion screen");

        Field gameField = UIManager.class.getDeclaredField("game");
        gameField.setAccessible(true);
        Object originalGame = gameField.get(uiManager);
        ByteArrayOutputStream consoleCapture = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        gameField.set(uiManager, null);
        try {
            System.setOut(new PrintStream(consoleCapture));
            clickButton(testButton);
            assertEquals(GameState.AI_COMPANION_SETTINGS, uiManager.getCurrentState(),
                "Game-less test should not change state");
            String output = consoleCapture.toString();
            assertTrue(output.contains("[AI] Cannot test connection - game not initialized."),
                "Missing game should log warning about unavailable test context");
        } finally {
            System.setOut(originalOut);
            gameField.set(uiManager, originalGame);
        }
    }

    @Test
    @DisplayName("Resize triggers layout rebuild")
    void testResizeTriggersLayoutRebuild() throws Exception {
        AICompanionScreen screen = activateAICompanionScreen();
        ScrollContainer originalContainer = getScrollContainer(screen);
        assertNotNull(originalContainer, "Scroll container should be available after initial layout");

        screen.onResize(1920, 1080);
        assertTrue(isLayoutDirty(screen), "Resize should mark layout dirty");

        screen.update(0.016f);
        assertFalse(isLayoutDirty(screen), "Update should rebuild layout and clear dirty flag");

        ScrollContainer rebuiltContainer = getScrollContainer(screen);
        assertNotNull(rebuiltContainer, "Scroll container should be reinitialised after layout rebuild");
        assertNotSame(originalContainer, rebuiltContainer, "Layout rebuild should recreate scroll container");
    }

    private AICompanionScreen activateAICompanionScreen() throws Exception {
        uiManager.setState(GameState.SETTINGS_MENU);
        uiManager.setState(GameState.AI_COMPANION_SETTINGS);

        UIScreen screen = getScreen(GameState.AI_COMPANION_SETTINGS);
        assertNotNull(screen, "AI companion screen should be registered");
        assertTrue(screen instanceof AICompanionScreen, "Registered screen should be AICompanionScreen");
        return (AICompanionScreen) screen;
    }

    private void clickButton(MenuButton button) {
        float clickX = button.getX() + button.getWidth() / 2f;
        float clickY = button.getY() + button.getHeight() / 2f;
        button.onMouseClick(clickX, clickY, 0);
        button.onMouseRelease(clickX, clickY, 0);
    }

    private MenuButton findButton(AICompanionScreen screen, String label) throws Exception {
        return getComponents(screen).stream()
            .filter(MenuButton.class::isInstance)
            .map(MenuButton.class::cast)
            .filter(button -> label.equals(button.getText()))
            .findFirst()
            .orElse(null);
    }

    private MenuBackground getBackground(AICompanionScreen screen) throws Exception {
        Field field = AICompanionScreen.class.getDeclaredField("background");
        field.setAccessible(true);
        return (MenuBackground) field.get(screen);
    }

    private ScrollContainer getScrollContainer(AICompanionScreen screen) throws Exception {
        Field field = AICompanionScreen.class.getDeclaredField("scrollContainer");
        field.setAccessible(true);
        return (ScrollContainer) field.get(screen);
    }

    private boolean isLayoutDirty(AICompanionScreen screen) throws Exception {
        Field field = AICompanionScreen.class.getDeclaredField("layoutDirty");
        field.setAccessible(true);
        return field.getBoolean(screen);
    }

    private float getFloatField(AICompanionScreen screen, String fieldName) throws Exception {
        Field field = AICompanionScreen.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getFloat(screen);
    }

    private Checkbox getCheckbox(AICompanionScreen screen, String fieldName) throws Exception {
        Field field = AICompanionScreen.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Checkbox) field.get(screen);
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
}
