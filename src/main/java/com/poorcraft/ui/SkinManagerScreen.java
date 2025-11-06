package com.poorcraft.ui;

import com.poorcraft.config.Settings;
import com.poorcraft.player.PlayerSkin;
import com.poorcraft.player.SkinManager;
import com.poorcraft.ui.GameState;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkinManagerScreen extends UIScreen {

    private final UIManager uiManager;
    private final SkinManager skinManager;
    private final Settings settings;

    private PlayerSkin highlightedSkin;
    private List<PlayerSkin> sortedSkins;

    private final Tooltip tooltip;
    private final Map<UIComponent, String> tooltipTexts = new HashMap<>();
    private final Map<PlayerSkin, Rect> thumbnailBounds = new HashMap<>();
    private UIComponent hoveredComponent;

    private boolean componentsInitialized;
    private boolean layoutDirty;
    private float selectionPulse;

    private Label titleLabel;
    private Label subtitleLabel;
    private MenuButton gridBackground;
    private MenuButton previewBackground;

    private final List<SkinTile> skinTiles = new ArrayList<>();

    private Label previewPlaceholderLabel;
    private Label previewNameLabel;
    private Label previewTypeLabel;
    private Label previewPathLabel;
    private Label previewFlagsLabel;
    private UIComponent previewBackdrop;
    private UIComponent previewFrame;
    private SkinPreviewComponent previewSkinComponent;

    private MenuButton selectButton;
    private MenuButton importButton;
    private MenuButton createButton;
    private MenuButton deleteButton;
    private MenuButton backButton;

    private static final class Rect {
        float x;
        float y;
        float width;
        float height;

        Rect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final class SkinTile {
        final PlayerSkin skin;
        final MenuButton tileButton;
        final UIComponent gradientBackdrop;
        final SkinPreviewComponent previewComponent;
        final UIComponent highlightFrame;
        final Label nameLabel;
        final BadgeOverlay badgeOverlay;
        final CheckmarkOverlay checkmarkOverlay;

        SkinTile(PlayerSkin skin,
                 MenuButton tileButton,
                 UIComponent gradientBackdrop,
                 SkinPreviewComponent previewComponent,
                 UIComponent highlightFrame,
                 Label nameLabel,
                 BadgeOverlay badgeOverlay,
                 CheckmarkOverlay checkmarkOverlay) {
            this.skin = skin;
            this.tileButton = tileButton;
            this.gradientBackdrop = gradientBackdrop;
            this.previewComponent = previewComponent;
            this.highlightFrame = highlightFrame;
            this.nameLabel = nameLabel;
            this.badgeOverlay = badgeOverlay;
            this.checkmarkOverlay = checkmarkOverlay;
        }
    }

    private interface OnTopLayer { }

    private final class SkinPreviewComponent extends UIComponent {

        private String skinId;

        SkinPreviewComponent(float x, float y, float width, float height, String skinId) {
            super(x, y, width, height);
            this.skinId = skinId;
        }

        @Override
        public void render(UIRenderer renderer, FontRenderer fontRenderer) {
            SkinManager.getInstance().getAtlas().renderOrPlaceholder(renderer, fontRenderer, x, y, width, height, skinId);
        }

        @Override
        public void update(float deltaTime) {
        }

        void setSkin(String skinId) {
            this.skinId = skinId;
        }
    }

    private final class BadgeOverlay extends UIComponent implements OnTopLayer {

        private String badgeText = "";
        private float badgeTextR;
        private float badgeTextG;
        private float badgeTextB;
        private float norm;

        BadgeOverlay(float x, float y, float norm) {
            super(x, y, 0f, 0f);
            this.norm = norm;
        }

        void setBadge(String text, float r, float g, float b) {
            this.badgeText = text;
            this.badgeTextR = r;
            this.badgeTextG = g;
            this.badgeTextB = b;
        }

        void setNorm(float norm) {
            this.norm = norm;
        }

        @Override
        public void render(UIRenderer renderer, FontRenderer fontRenderer) {
            if (badgeText == null || badgeText.isEmpty()) {
                return;
            }

            float textWidth = fontRenderer.getTextWidth(badgeText) * 1.1f * norm;
            float textHeight = fontRenderer.getTextHeight() * 1.1f * norm;
            float padding = scaleDimension(6f);
            float panelWidth = textWidth + padding * 2f;
            float panelHeight = textHeight + padding * 1.5f;

            renderer.drawRect(x, y, panelWidth, panelHeight, 0.1f, 0.1f, 0.15f, 0.85f);
            renderer.drawBorderedRect(x, y, panelWidth, panelHeight, 1.5f,
                new float[]{0.1f, 0.1f, 0.15f, 0.85f},
                new float[]{badgeTextR * 0.7f, badgeTextG * 0.7f, badgeTextB * 0.7f, 0.95f});

            fontRenderer.drawTextWithShadow(badgeText, x + padding, y + padding * 0.75f,
                1.1f * norm, badgeTextR, badgeTextG, badgeTextB, 1.0f, 1.5f, 0.7f);
        }

        @Override
        public void update(float deltaTime) {
        }
    }

    private final class CheckmarkOverlay extends UIComponent implements OnTopLayer {

        CheckmarkOverlay(float x, float y, float width, float height) {
            super(x, y, width, height);
        }

        @Override
        public void render(UIRenderer renderer, FontRenderer fontRenderer) {
            float[] bgColor = {0.2f, 0.7f, 0.3f, 0.95f};
            float[] borderColor = {0.15f, 0.55f, 0.25f, 1.0f};
            renderer.drawBorderedRect(x, y, width, height, 2f, bgColor, borderColor);

            float scale = Math.min(width, height) / 20f;
            float textWidth = fontRenderer.getTextWidth("✓") * scale;
            float textHeight = fontRenderer.getTextHeight() * scale;
            float cx = x + (width - textWidth) / 2f;
            float cy = y + (height - textHeight) / 2f;
            fontRenderer.drawTextWithShadow("✓", cx, cy, scale, 1.0f, 1.0f, 1.0f, 1.0f, 1.5f, 0.8f);
        }

        @Override
        public void update(float deltaTime) {
        }
    }

    public SkinManagerScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.skinManager = SkinManager.getInstance();
        this.settings = uiManager.getSettings();
        this.tooltip = new Tooltip(0f, 0f, "");
        this.selectionPulse = 0.9f;
        this.layoutDirty = true;
        this.componentsInitialized = false;
    }

    @Override
    public void init() {
        tooltipTexts.clear();
        thumbnailBounds.clear();

        if (!componentsInitialized) {
            clearComponents();
            skinTiles.clear();
        }

        if (!componentsInitialized) {
            buildComponents();
            componentsInitialized = true;
        }

        recalculateLayout();
        layoutDirty = false;
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        layoutDirty = true;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Render components in passes for proper layering:
        // Pass 1: Background components (MenuButton backdrops)
        // Pass 2: Thumbnails and preview components
        // Pass 3: Selection glow (after thumbnails, before overlays)
        // Pass 4: Text labels, badges, checkmarks (OnTopLayer components)
        // Pass 5: Tooltip (on top of everything)
        
        List<UIComponent> allComponents = new ArrayList<>(components);
        
        // Pass 1 & 2: Render backgrounds and skin previews (exclude Tooltip, Label, and OnTopLayer)
        for (UIComponent component : allComponents) {
            if (component.isVisible() && !(component instanceof Tooltip) && !(component instanceof Label) && !(component instanceof OnTopLayer)) {
                component.render(renderer, fontRenderer);
            }
        }
        
        // Pass 3: Draw selection glow using stored bounds
        if (highlightedSkin != null && thumbnailBounds.containsKey(highlightedSkin)) {
            Rect bounds = thumbnailBounds.get(highlightedSkin);
            float glowOffset = scaleDimension(8f);
            float glowIntensity = 0.9f * selectionPulse;
            float[] glowColor = {0.2f, 0.8f, 1.0f, 0.85f};
            
            renderer.drawGlowBorder(bounds.x - glowOffset, bounds.y - glowOffset, 
                bounds.width + glowOffset * 2f, bounds.height + glowOffset * 2f, 
                glowOffset, glowIntensity, glowColor);
        }
        
        // Pass 4: Render labels and OnTopLayer components (badges, checkmarks)
        for (UIComponent component : allComponents) {
            if (component.isVisible() && (component instanceof Label || component instanceof OnTopLayer)) {
                component.render(renderer, fontRenderer);
            }
        }
        
        // Pass 5: Render tooltip last (on top of everything)
        if (tooltip != null && tooltip.isVisible()) {
            tooltip.render(renderer, fontRenderer);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        if (layoutDirty && componentsInitialized) {
            recalculateLayout();
            layoutDirty = false;
        }

        // Update all components
        super.update(deltaTime);
        
        // Update selection pulse animation
        selectionPulse = 0.9f + 0.1f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        
        // Update tooltip
        if (tooltip != null) {
            tooltip.update(deltaTime);
        }
    }
    
    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        super.onMouseMove(mouseX, mouseY);
        
        // Update tooltip based on hovered component
        UIComponent newHovered = null;
        for (UIComponent component : components) {
            if (component.isMouseOver(mouseX, mouseY) && tooltipTexts.containsKey(component)) {
                newHovered = component;
                break;
            }
        }
        
        if (newHovered != hoveredComponent) {
            hoveredComponent = newHovered;
            if (hoveredComponent != null) {
                String tooltipText = tooltipTexts.get(hoveredComponent);
                tooltip.setText(tooltipText);
                tooltip.setPosition(mouseX + 12f, mouseY + 12f);
                tooltip.show();
            } else {
                tooltip.hide();
            }
        } else if (hoveredComponent != null) {
            tooltip.setPosition(mouseX + 12f, mouseY + 12f);
        }
    }

    private void buildComponents() {
        skinTiles.clear();
        sortedSkins = new ArrayList<>(skinManager.getAllSkins());
        sortedSkins.sort(Comparator.comparing(PlayerSkin::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        if (highlightedSkin == null && skinManager.getCurrentSkin() != null) {
            highlightedSkin = skinManager.getCurrentSkin();
        }

        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());

        titleLabel = new Label(0f, 0f, "SKIN MANAGER", 0.92f, 0.88f, 0.99f, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);

        subtitleLabel = new Label(0f, 0f, "Select, import, or create a skin", 0.7f, 0.82f, 0.95f, 0.88f);
        subtitleLabel.setCentered(true);
        addComponent(subtitleLabel);

        gridBackground = new MenuButton(0f, 0f, 0f, 0f, "", null);
        gridBackground.setEnabled(false);
        addComponent(gridBackground);

        previewBackground = new MenuButton(0f, 0f, 0f, 0f, "", null);
        previewBackground.setEnabled(false);
        addComponent(previewBackground);

        for (PlayerSkin skin : sortedSkins) {
            skinTiles.add(createSkinTile(skin, norm));
        }

        previewBackdrop = new UIComponent(0f, 0f, 0f, 0f) {
            @Override
            public void render(UIRenderer renderer, FontRenderer fontRenderer) {
                float[] topColor = {0.88f, 0.90f, 0.95f, 1.0f};
                float[] bottomColor = {0.78f, 0.82f, 0.90f, 1.0f};
                renderer.drawGradientRect(x, y, width, height, topColor, bottomColor);
            }

            @Override
            public void update(float deltaTime) {
            }
        };
        addComponent(previewBackdrop);

        previewSkinComponent = new SkinPreviewComponent(0f, 0f, 0f, 0f,
            highlightedSkin != null ? highlightedSkin.getName() : "");
        addComponent(previewSkinComponent);

        previewFrame = new UIComponent(0f, 0f, 0f, 0f) {
            @Override
            public void render(UIRenderer renderer, FontRenderer fontRenderer) {
                float[] frameColor = {0.7f, 0.8f, 0.95f, 0.95f};
                renderer.drawHighlightFrame(x, y, width, height, 4f, frameColor);
            }

            @Override
            public void update(float deltaTime) {
            }
        };
        addComponent(previewFrame);

        previewPlaceholderLabel = new Label(0f, 0f, "Select a skin to preview", 0.7f, 0.78f, 0.9f, 0.9f);
        previewPlaceholderLabel.setCentered(true);
        addComponent(previewPlaceholderLabel);

        previewNameLabel = new Label(0f, 0f, "", 0.95f, 0.92f, 1.0f, 1.0f);
        previewNameLabel.setCentered(true);
        addComponent(previewNameLabel);

        previewTypeLabel = new Label(0f, 0f, "", 0.78f, 0.88f, 0.95f, 0.84f);
        previewTypeLabel.setCentered(true);
        addComponent(previewTypeLabel);

        previewPathLabel = new Label(0f, 0f, "", 0.7f, 0.78f, 0.88f, 0.9f);
        previewPathLabel.setCentered(true);
        addComponent(previewPathLabel);

        previewFlagsLabel = new Label(0f, 0f, "", 0.78f, 0.9f, 0.92f, 0.85f);
        previewFlagsLabel.setCentered(true);
        addComponent(previewFlagsLabel);

        selectButton = new MenuButton(0f, 0f, 0f, 0f, "SELECT", () -> {
            if (highlightedSkin != null) {
                skinManager.setCurrentSkin(highlightedSkin.getName());
                uiManager.getConfigManager().saveSettings(settings);
                updateSkinTilesActiveState();
                updateButtonStates();
                layoutDirty = true;
            }
        });
        addComponent(selectButton);
        tooltipTexts.put(selectButton, "Apply this skin to your player");

        importButton = new MenuButton(0f, 0f, 0f, 0f, "IMPORT", this::importSkin);
        addComponent(importButton);
        tooltipTexts.put(importButton, "Import a skin from a PNG file");

        createButton = new MenuButton(0f, 0f, 0f, 0f, "CREATE NEW",
            () -> uiManager.setState(GameState.SKIN_EDITOR));
        addComponent(createButton);
        tooltipTexts.put(createButton, "Open the skin editor to create a new skin");

        deleteButton = new MenuButton(0f, 0f, 0f, 0f, "DELETE", this::deleteSkin);
        addComponent(deleteButton);
        tooltipTexts.put(deleteButton, "Delete this skin (cannot delete default skins)");

        backButton = new MenuButton(0f, 0f, 0f, 0f, "BACK",
            () -> uiManager.setState(uiManager.getPreviousState() != null
                ? uiManager.getPreviousState() : GameState.MAIN_MENU));
        addComponent(backButton);
        tooltipTexts.put(backButton, "Return to the previous menu");

        addComponent(tooltip);

        updateSkinTilesActiveState();
        updateButtonStates();
    }

    private SkinTile createSkinTile(PlayerSkin skin, float norm) {
        MenuButton tileButton = new MenuButton(0f, 0f, 0f, 0f, "", () -> {
            highlightedSkin = skin;
            updateButtonStates();
            layoutDirty = true;
        });
        addComponent(tileButton);
        tooltipTexts.put(tileButton, skin.getDisplayName() + " • " + skin.getType() + "\n" + skin.getFilePath());

        UIComponent gradientBackdrop = new UIComponent(0f, 0f, 0f, 0f) {
            @Override
            public void render(UIRenderer renderer, FontRenderer fontRenderer) {
                float[] topColor = {0.88f, 0.90f, 0.95f, 0.95f};
                float[] bottomColor = {0.80f, 0.84f, 0.92f, 0.95f};
                renderer.drawGradientRect(x, y, width, height, topColor, bottomColor);
            }

            @Override
            public void update(float deltaTime) {}
        };
        addComponent(gradientBackdrop);

        SkinPreviewComponent previewComponent = new SkinPreviewComponent(0f, 0f, 0f, 0f, skin.getName());
        addComponent(previewComponent);

        UIComponent highlightFrame = new UIComponent(0f, 0f, 0f, 0f) {
            @Override
            public void render(UIRenderer renderer, FontRenderer fontRenderer) {
                float[] frameColor = {0.7f, 0.8f, 0.95f, 0.85f};
                renderer.drawHighlightFrame(x, y, width, height, 3f, frameColor);
            }

            @Override
            public void update(float deltaTime) {}
        };
        addComponent(highlightFrame);

        Label nameLabel = new Label(0f, 0f, skin.getDisplayName(), 0.82f, 0.92f, 1.0f, 0.9f);
        nameLabel.setCentered(true);
        nameLabel.setUseTextShadow(true);
        addComponent(nameLabel);

        float badgeR;
        float badgeG;
        float badgeB;
        String badgeText;
        if (skin.isDefault()) {
            badgeR = 0.95f;
            badgeG = 0.85f;
            badgeB = 0.3f;
            badgeText = "DEFAULT";
        } else if (skin.isCustom()) {
            badgeR = 0.3f;
            badgeG = 0.85f;
            badgeB = 0.95f;
            badgeText = "CUSTOM";
        } else {
            badgeR = 0.4f;
            badgeG = 0.9f;
            badgeB = 0.5f;
            badgeText = "USER";
        }

        BadgeOverlay badgeOverlay = new BadgeOverlay(0f, 0f, norm);
        badgeOverlay.setBadge(badgeText, badgeR, badgeG, badgeB);
        addComponent(badgeOverlay);

        CheckmarkOverlay checkmarkOverlay = new CheckmarkOverlay(0f, 0f, 0f, 0f);
        checkmarkOverlay.setVisible(skinManager.getCurrentSkin() == skin);
        addComponent(checkmarkOverlay);

        return new SkinTile(skin, tileButton, gradientBackdrop, previewComponent, highlightFrame,
            nameLabel, badgeOverlay, checkmarkOverlay);
    }

    private void recalculateLayout() {
        float panelPadding = scaleDimension(40f);
        float headerY = panelPadding;
        float headerX = windowWidth / 2f;
        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());

        if (titleLabel != null) {
            titleLabel.setScale(2.8f * norm);
            titleLabel.setPosition(headerX, headerY);
        }
        if (subtitleLabel != null) {
            subtitleLabel.setScale(1.6f * norm);
            subtitleLabel.setPosition(headerX, headerY + scaleDimension(46f));
        }

        float gridTop = headerY + scaleDimension(120f);
        float gridHeight = Math.max(scaleDimension(240f), windowHeight - gridTop - scaleDimension(140f));
        float gridWidth = windowWidth * 0.58f;
        float gridLeft = panelPadding;

        if (gridBackground != null) {
            gridBackground.setBounds(gridLeft, gridTop, gridWidth, gridHeight);
        }

        int columns = Math.max(3, (int) (gridWidth / scaleDimension(180f)));
        float cellWidth = gridWidth / columns;
        float cellHeight = Math.max(scaleDimension(160f), gridHeight / 3f);
        float padding = scaleDimension(18f);
        float backdropPadding = scaleDimension(6f);

        thumbnailBounds.clear();

        for (int i = 0; i < skinTiles.size(); i++) {
            SkinTile tile = skinTiles.get(i);
            PlayerSkin skin = tile.skin;

            int row = i / columns;
            int col = i % columns;

            float cellX = gridLeft + col * cellWidth + padding * 0.5f;
            float cellY = gridTop + row * cellHeight + padding * 0.5f;
            float thumbSize = Math.min(cellWidth, cellHeight) - padding;

            tile.tileButton.setBounds(cellX, cellY, thumbSize, thumbSize);

            float bgSize = thumbSize * 0.88f;
            float bgX = cellX + (thumbSize - bgSize) / 2f;
            float bgY = cellY + (thumbSize - bgSize) / 2f;
            float backdropSize = bgSize + backdropPadding * 2f;

            tile.gradientBackdrop.setBounds(bgX - backdropPadding, bgY - backdropPadding, backdropSize, backdropSize);
            tile.previewComponent.setBounds(bgX, bgY, bgSize, bgSize);
            tile.highlightFrame.setBounds(bgX, bgY, bgSize, bgSize);

            boolean isCurrent = skinManager.getCurrentSkin() == skin;
            String labelText = skin.getDisplayName() + (isCurrent ? " (Active)" : "");
            tile.nameLabel.setScale(1.4f * norm);
            tile.nameLabel.setPosition(cellX + thumbSize / 2f, cellY + thumbSize + scaleDimension(22f));
            tile.nameLabel.setText(labelText);

            float badgeX = cellX + scaleDimension(12f);
            float badgeY = cellY + scaleDimension(22f);
            tile.badgeOverlay.setPosition(badgeX, badgeY);
            tile.badgeOverlay.setNorm(norm);

            float checkSize = scaleDimension(24f);
            float checkX = cellX + thumbSize - checkSize - scaleDimension(8f);
            float checkY = cellY + scaleDimension(8f);
            tile.checkmarkOverlay.setBounds(checkX, checkY, checkSize, checkSize);
            tile.checkmarkOverlay.setVisible(isCurrent);

            thumbnailBounds.put(skin, new Rect(cellX, cellY, thumbSize, thumbSize));
        }

        float previewWidth = Math.max(scaleDimension(300f), windowWidth - gridLeft - gridWidth - panelPadding);
        float previewLeft = gridLeft + gridWidth + scaleDimension(30f);
        float previewTop = gridTop;
        float previewHeight = gridHeight;

        if (previewBackground != null) {
            previewBackground.setBounds(previewLeft, previewTop, previewWidth, previewHeight);
        }

        updatePreviewSection(norm, previewLeft, previewTop, previewWidth, previewHeight);

        float buttonBarY = gridTop + gridHeight + scaleDimension(40f);
        float totalWidth = gridWidth + previewWidth + scaleDimension(30f);
        float spacing = scaleDimension(18f);
        float buttonCount = 5f;
        float buttonWidth = (totalWidth - spacing * (buttonCount - 1)) / buttonCount;
        float buttonHeight = scaleDimension(70f);

        if (selectButton != null) {
            selectButton.setBounds(gridLeft, buttonBarY, buttonWidth, buttonHeight);
        }
        if (importButton != null) {
            importButton.setBounds(gridLeft + (buttonWidth + spacing) * 1f, buttonBarY, buttonWidth, buttonHeight);
        }
        if (createButton != null) {
            createButton.setBounds(gridLeft + (buttonWidth + spacing) * 2f, buttonBarY, buttonWidth, buttonHeight);
        }
        if (deleteButton != null) {
            deleteButton.setBounds(gridLeft + (buttonWidth + spacing) * 3f, buttonBarY, buttonWidth, buttonHeight);
        }
        if (backButton != null) {
            backButton.setBounds(gridLeft + (buttonWidth + spacing) * 4f, buttonBarY, buttonWidth, buttonHeight);
        }

        updateButtonStates();
    }

    private void updateSkinTilesActiveState() {
        for (SkinTile tile : skinTiles) {
            boolean isCurrent = skinManager.getCurrentSkin() == tile.skin;
            tile.checkmarkOverlay.setVisible(isCurrent);
        }
    }

    private void updatePreviewSection(float norm, float previewLeft, float previewTop, float previewWidth, float previewHeight) {
        if (highlightedSkin == null) {
            if (previewPlaceholderLabel != null) {
                previewPlaceholderLabel.setVisible(true);
                previewPlaceholderLabel.setScale(1.6f * norm);
                previewPlaceholderLabel.setPosition(previewLeft + previewWidth / 2f, previewTop + previewHeight / 2f);
            }
            if (previewBackdrop != null) {
                previewBackdrop.setVisible(false);
            }
            if (previewSkinComponent != null) {
                previewSkinComponent.setVisible(false);
            }
            if (previewFrame != null) {
                previewFrame.setVisible(false);
            }
            if (previewNameLabel != null) {
                previewNameLabel.setVisible(false);
            }
            if (previewTypeLabel != null) {
                previewTypeLabel.setVisible(false);
            }
            if (previewPathLabel != null) {
                previewPathLabel.setVisible(false);
            }
            if (previewFlagsLabel != null) {
                previewFlagsLabel.setVisible(false);
            }
            return;
        }

        if (previewPlaceholderLabel != null) {
            previewPlaceholderLabel.setVisible(false);
        }

        float previewSize = Math.min(previewWidth * 0.80f, previewHeight * 0.58f);
        float previewX = previewLeft + (previewWidth - previewSize) / 2f;
        float previewY = previewTop + previewHeight * 0.2f;
        float backdropPadding = scaleDimension(12f);

        if (previewBackdrop != null) {
            previewBackdrop.setVisible(true);
            previewBackdrop.setBounds(previewX - backdropPadding, previewY - backdropPadding,
                previewSize + backdropPadding * 2f, previewSize + backdropPadding * 2f);
        }

        if (previewSkinComponent != null) {
            previewSkinComponent.setVisible(true);
            previewSkinComponent.setBounds(previewX, previewY, previewSize, previewSize);
            previewSkinComponent.setSkin(highlightedSkin.getName());
        }

        if (previewFrame != null) {
            previewFrame.setVisible(true);
            previewFrame.setBounds(previewX, previewY, previewSize, previewSize);
        }

        if (previewNameLabel != null) {
            previewNameLabel.setVisible(true);
            previewNameLabel.setScale(2.2f * norm);
            previewNameLabel.setText(highlightedSkin.getDisplayName().toUpperCase(Locale.ENGLISH));
            previewNameLabel.setPosition(previewLeft + previewWidth / 2f, previewTop + scaleDimension(32f));
        }

        if (previewTypeLabel != null) {
            previewTypeLabel.setVisible(true);
            previewTypeLabel.setScale(1.4f * norm);
            previewTypeLabel.setText(highlightedSkin.getType().name() + (highlightedSkin.isDefault() ? " • Default" : ""));
            previewTypeLabel.setPosition(previewLeft + previewWidth / 2f, previewTop + scaleDimension(68f));
        }

        if (previewPathLabel != null) {
            previewPathLabel.setVisible(true);
            previewPathLabel.setScale(1.2f * norm);
            previewPathLabel.setText(highlightedSkin.getFilePath().toString());
            previewPathLabel.setPosition(previewLeft + previewWidth / 2f,
                previewY + previewSize + scaleDimension(40f));
        }

        if (previewFlagsLabel != null) {
            previewFlagsLabel.setVisible(true);
            previewFlagsLabel.setScale(1.3f * norm);
            previewFlagsLabel.setText((highlightedSkin.isDefault() ? "Bundled skin" : "User skin")
                + " • " + highlightedSkin.getType());
            previewFlagsLabel.setPosition(previewLeft + previewWidth / 2f,
                previewY + previewSize + scaleDimension(70f));
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = highlightedSkin != null;
        boolean isDefault = hasSelection && highlightedSkin.isDefault();
        boolean isActive = hasSelection && skinManager.getCurrentSkin() == highlightedSkin;

        if (selectButton != null) {
            selectButton.setEnabled(hasSelection && !isActive);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(hasSelection && !isDefault);
        }
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
            componentsInitialized = false;
            layoutDirty = true;
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
        componentsInitialized = false;
        layoutDirty = true;
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
