package com.poorcraft.ui;

import com.poorcraft.config.Settings;
import com.poorcraft.player.PlayerSkin;
import com.poorcraft.player.SkinManager;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SkinManagerScreen extends UIScreen {

    private final UIManager uiManager;
    private final SkinManager skinManager;
    private final Settings settings;

    private PlayerSkin highlightedSkin;
    private List<PlayerSkin> sortedSkins;

    public SkinManagerScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
        this.uiManager = uiManager;
        this.skinManager = SkinManager.getInstance();
        this.settings = uiManager.getSettings();
    }

class SkinPreviewComponent extends UIComponent {

    private final String skinId;

    SkinPreviewComponent(float x, float y, float width, float height, String skinId) {
        super(x, y, width, height);
        this.skinId = skinId;
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        SkinManager.getInstance().getAtlas().renderOrPlaceholder(renderer, x, y, width, height, skinId);
    }

    @Override
    public void update(float deltaTime) {
        // Static preview
    }
}

    @Override
    public void init() {
        clearComponents();
        buildLayout();
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }

    private void buildLayout() {
        float panelPadding = Math.max(40f, windowWidth * 0.04f);
        float headerY = panelPadding;
        float headerX = windowWidth / 2f;

        Label title = new Label(headerX, headerY, "SKIN MANAGER",
            0.92f, 0.88f, 0.99f, 1.0f);
        title.setCentered(true);
        title.setScale(Math.max(1.8f, windowWidth / 720f));
        addComponent(title);

        Label subtitle = new Label(headerX, headerY + 46f,
            "Select, import, or create a skin",
            0.7f, 0.82f, 0.95f, 0.88f);
        subtitle.setCentered(true);
        subtitle.setScale(Math.max(1.0f, windowWidth / 960f));
        addComponent(subtitle);

        float gridTop = headerY + Math.max(120f, windowHeight * 0.12f);
        float gridHeight = windowHeight - gridTop - Math.max(140f, windowHeight * 0.18f);
        float gridWidth = windowWidth * 0.58f;
        float gridLeft = panelPadding;

        renderBackground(gridLeft, gridTop, gridWidth, gridHeight);
        buildSkinGrid(gridLeft, gridTop, gridWidth, gridHeight);

        float previewWidth = windowWidth - gridLeft - gridWidth - panelPadding;
        float previewLeft = gridLeft + gridWidth + Math.max(30f, windowWidth * 0.02f);

        renderBackground(previewLeft, gridTop, previewWidth, gridHeight);
        buildPreview(previewLeft, gridTop, previewWidth, gridHeight);

        float buttonBarY = gridTop + gridHeight + Math.max(40f, windowHeight * 0.04f);
        buildButtonBar(gridLeft, buttonBarY, gridWidth + previewWidth + Math.max(30f, windowWidth * 0.02f));

        updateButtonStates();
    }

    private void renderBackground(float x, float y, float width, float height) {
        MenuButton backdrop = new MenuButton(x, y, width, height, "", null);
        backdrop.setEnabled(false);
        addComponent(backdrop);
    }

    private void buildSkinGrid(float left, float top, float width, float height) {
        sortedSkins = new ArrayList<>(skinManager.getAllSkins());
        sortedSkins.sort(Comparator.comparing(PlayerSkin::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        if (highlightedSkin == null && skinManager.getCurrentSkin() != null) {
            highlightedSkin = skinManager.getCurrentSkin();
        }

        int columns = Math.max(3, (int) (width / 180f));
        float cellWidth = width / columns;
        float cellHeight = Math.max(160f, height / 3f);
        float padding = Math.max(12f, cellWidth * 0.08f);

        for (int i = 0; i < sortedSkins.size(); i++) {
            PlayerSkin skin = sortedSkins.get(i);
            int row = i / columns;
            int col = i % columns;
            float cellX = left + col * cellWidth + padding * 0.5f;
            float cellY = top + row * cellHeight + padding * 0.5f;
            float thumbSize = Math.min(cellWidth, cellHeight) - padding;

            createSkinThumbnail(skin, cellX, cellY, thumbSize, thumbSize);
        }
    }

    private void createSkinThumbnail(PlayerSkin skin, float x, float y, float width, float height) {
        MenuButton tile = new MenuButton(x, y, width, height, "", () -> {
            highlightedSkin = skin;
            updateButtonStates();
            init();
        });
        addComponent(tile);

        addComponent(new SkinPreviewComponent(x + width * 0.1f, y + width * 0.1f,
            width * 0.8f, height * 0.8f, skin.getName()));

        boolean isCurrent = skinManager.getCurrentSkin() == skin;
        String label = skin.getDisplayName();
        if (isCurrent) {
            label += " (Active)";
        }
        Label nameLabel = new Label(x + width / 2f, y + height + 22f, label,
            0.82f, 0.92f, 1.0f, 0.9f);
        nameLabel.setCentered(true);
        nameLabel.setScale(Math.max(0.9f, width / 220f));
        addComponent(nameLabel);

        Label badge = new Label(x + 12f, y + 22f,
            skin.isDefault() ? "DEFAULT" : (skin.isCustom() ? "CUSTOM" : "USER"),
            skin.isDefault() ? 0.6f : 0.9f,
            skin.isDefault() ? 0.9f : 0.5f,
            skin.isDefault() ? 1.0f : 0.8f,
            0.85f);
        badge.setScale(Math.max(0.72f, width / 260f));
        addComponent(badge);

        if (highlightedSkin == skin) {
            MenuButton outline = new MenuButton(x - 6f, y - 6f, width + 12f, height + 12f, "", null);
            outline.setEnabled(false);
            addComponent(outline);
        }
    }

    private void buildPreview(float left, float top, float width, float height) {
        if (highlightedSkin == null) {
            Label placeholder = new Label(left + width / 2f, top + height / 2f,
                "Select a skin to preview",
                0.7f, 0.78f, 0.9f, 0.9f);
            placeholder.setCentered(true);
            placeholder.setScale(Math.max(1.1f, width / 360f));
            addComponent(placeholder);
            return;
        }

        Label name = new Label(left + width / 2f, top + 32f,
            highlightedSkin.getDisplayName().toUpperCase(Locale.ENGLISH),
            0.95f, 0.92f, 1.0f, 1.0f);
        name.setCentered(true);
        name.setScale(Math.max(1.4f, width / 320f));
        addComponent(name);

        Label type = new Label(left + width / 2f, top + 68f,
            highlightedSkin.getType().name() + (highlightedSkin.isDefault() ? " • Default" : ""),
            0.78f, 0.88f, 0.95f, 0.84f);
        type.setCentered(true);
        type.setScale(Math.max(0.9f, width / 440f));
        addComponent(type);

        float previewSize = Math.min(width * 0.68f, height * 0.5f);
        float previewX = left + (width - previewSize) / 2f;
        float previewY = top + height * 0.2f;

        addComponent(new SkinPreviewComponent(previewX, previewY, previewSize, previewSize, highlightedSkin.getName()));

        Label infoPath = new Label(left + width / 2f, previewY + previewSize + 40f,
            highlightedSkin.getFilePath().toString(),
            0.7f, 0.78f, 0.88f, 0.9f);
        infoPath.setCentered(true);
        infoPath.setScale(Math.max(0.78f, width / 520f));
        addComponent(infoPath);

        Label infoFlags = new Label(left + width / 2f, previewY + previewSize + 70f,
            (highlightedSkin.isDefault() ? "Bundled skin" : "User skin") + " • " + highlightedSkin.getType(),
            0.78f, 0.9f, 0.92f, 0.85f);
        infoFlags.setCentered(true);
        infoFlags.setScale(Math.max(0.82f, width / 460f));
        addComponent(infoFlags);
    }

    private void buildButtonBar(float left, float y, float totalWidth) {
        float buttonCount = 5;
        float spacing = Math.max(18f, totalWidth * 0.01f);
        float buttonWidth = (totalWidth - spacing * (buttonCount - 1)) / buttonCount;
        float buttonHeight = Math.max(70f, windowHeight * 0.09f);

        MenuButton selectButton = createButton(left, y, buttonWidth, buttonHeight, "SELECT",
            () -> {
                if (highlightedSkin != null) {
                    skinManager.setCurrentSkin(highlightedSkin.getName());
                    uiManager.getConfigManager().saveSettings(settings);
                    updateButtonStates();
                    init();
                }
            });

        MenuButton importButton = createButton(left + (buttonWidth + spacing) * 1, y, buttonWidth, buttonHeight, "IMPORT",
            this::importSkin);

        MenuButton createButton = createButton(left + (buttonWidth + spacing) * 2, y, buttonWidth, buttonHeight, "CREATE NEW",
            () -> uiManager.setState(GameState.SKIN_EDITOR));

        MenuButton deleteButton = createButton(left + (buttonWidth + spacing) * 3, y, buttonWidth, buttonHeight, "DELETE",
            this::deleteSkin);

        MenuButton backButton = createButton(left + (buttonWidth + spacing) * 4, y, buttonWidth, buttonHeight, "BACK",
            () -> uiManager.setState(uiManager.getPreviousState() != null
                ? uiManager.getPreviousState() : GameState.MAIN_MENU));

        selectButton.setEnabled(false);
        deleteButton.setEnabled(false);
        updateButtonStates(selectButton, deleteButton);
    }

    private MenuButton createButton(float x, float y, float width, float height, String text, Runnable action) {
        MenuButton button = new MenuButton(x, y, width, height, text, action);
        addComponent(button);
        return button;
    }

    private void updateButtonStates() {
        // no-op retained for backwards compatibility
    }

    private void updateButtonStates(MenuButton selectButton, MenuButton deleteButton) {
        boolean hasSelection = highlightedSkin != null;
        boolean isDefault = hasSelection && highlightedSkin.isDefault();
        boolean isActive = hasSelection && skinManager.getCurrentSkin() == highlightedSkin;

        selectButton.setEnabled(hasSelection && !isActive);
        deleteButton.setEnabled(hasSelection && !isDefault);
    }

    private void importSkin() {
        File selectedFile = showSkinFileChooser();
        if (selectedFile == null) {
            return;
        }

        String sanitized = sanitizeName(selectedFile.getName());
        sanitized = skinManager.ensureUniqueUserSkinName(sanitized);

        PlayerSkin skin = skinManager.importSkin(selectedFile.toPath(), sanitized);
        if (skin != null) {
            highlightedSkin = skin;
            uiManager.getConfigManager().saveSettings(settings);
            init();
        } else {
            System.err.println("[SkinManagerScreen] Failed to import skin from " + selectedFile);
        }
    }

    private void deleteSkin() {
        if (highlightedSkin == null || highlightedSkin.isDefault()) {
            return;
        }
        Path path = highlightedSkin.getFilePath();
        skinManager.removeSkin(highlightedSkin.getName());
        if (sortedSkins != null) {
            sortedSkins.remove(highlightedSkin);
        }
        highlightedSkin = skinManager.getCurrentSkin();
        init();
        System.out.println("[SkinManagerScreen] Deleted skin at " + path);
    }

    private String sanitizeName(String fileName) {
        String base = fileName.toLowerCase(Locale.ENGLISH);
        if (base.endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }
        base = base.replaceAll("[^a-z0-9_]+", "_");
        base = base.replaceAll("_{2,}", "_");
        base = base.replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "skin";
        }
        if (base.length() > 32) {
            base = base.substring(0, 32);
        }
        return base;
    }

    private File showSkinFileChooser() {
        final File[] selection = new File[1];
        final int[] resultHolder = new int[]{JFileChooser.CANCEL_OPTION};

        Runnable dialogTask = () -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
            resultHolder[0] = chooser.showOpenDialog(null);
            if (resultHolder[0] == JFileChooser.APPROVE_OPTION) {
                selection[0] = chooser.getSelectedFile();
            }
        };

        Object window = resolveGameWindow();
        boolean restored = false;
        if (window != null && settings != null && settings.window != null && settings.window.fullscreen) {
            if (setWindowFullscreen(window, false)) {
                restored = true;
            }
        }

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                dialogTask.run();
            } else {
                SwingUtilities.invokeAndWait(dialogTask);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[SkinManagerScreen] File chooser interrupted");
        } catch (InvocationTargetException e) {
            System.err.println("[SkinManagerScreen] File chooser error: " + e.getCause());
        } finally {
            if (restored) {
                setWindowFullscreen(window, true);
            }
        }

        return resultHolder[0] == JFileChooser.APPROVE_OPTION ? selection[0] : null;
    }

    private Object resolveGameWindow() {
        try {
            Field gameField = uiManager.getClass().getDeclaredField("game");
            gameField.setAccessible(true);
            Object game = gameField.get(uiManager);
            if (game == null) {
                return null;
            }
            Field windowField = game.getClass().getDeclaredField("window");
            windowField.setAccessible(true);
            return windowField.get(game);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("[SkinManagerScreen] Unable to resolve game window: " + e.getMessage());
            return null;
        }
    }

    private boolean setWindowFullscreen(Object window, boolean fullscreen) {
        if (window == null) {
            return false;
        }
        try {
            Method method = window.getClass().getMethod("setFullscreen", boolean.class);
            method.invoke(window, fullscreen);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.err.println("[SkinManagerScreen] Failed to toggle fullscreen: " + e.getMessage());
            return false;
        }
    }
}
