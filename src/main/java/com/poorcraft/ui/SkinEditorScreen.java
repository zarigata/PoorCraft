package com.poorcraft.ui;

import com.poorcraft.player.PlayerSkin;
import com.poorcraft.player.SkinManager;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Minimal in-game skin editor that allows painting a 64x64 skin, choosing colors,
 * switching tools, and saving to the user skin directory.
 */
public class SkinEditorScreen extends UIScreen {

    private static final int SKIN_SIZE = 64;

    private enum Tool {
        PENCIL,
        ERASER
    }

    private final UIManager uiManager;
    private final SkinManager skinManager;

    private BufferedImage canvas;
    private String defaultName;

    private Tool currentTool = Tool.PENCIL;
    private int currentColor = 0xFFFFFFFF;

    private PixelCanvasComponent canvasComponent;
    private ColorPreviewComponent colorPreviewComponent;
    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private Slider alphaSlider;
    private Label colorValueLabel;
    private TextField nameField;
    private Label statusLabel;
    private MenuButton pencilButton;
    private MenuButton eraserButton;
    private Label titleLabel;
    private Label subtitleLabel;
    private Label nameLabel;
    private Label toolsLabel;
    private Label pickerLabel;
    private MenuButton saveButton;
    private MenuButton clearButton;
    private MenuButton backButton;
    private boolean layoutDirty;
    private boolean componentsInitialized;

    public SkinEditorScreen(int windowWidth, int windowHeight, UIManager uiManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.skinManager = SkinManager.getInstance();

        PlayerSkin baseSkin = skinManager.getCurrentSkin();
        if (baseSkin != null && baseSkin.getImage() != null && baseSkin.getImage().getWidth() == SKIN_SIZE
            && baseSkin.getImage().getHeight() == SKIN_SIZE) {
            this.canvas = copyImage(baseSkin.getImage());
            this.defaultName = sanitizeName(baseSkin.getName() + "_edit");
        } else {
            this.canvas = createBlankCanvas();
            this.defaultName = "custom_skin";
        }
    }

    @Override
    public void init() {
        if (canvas == null) {
            canvas = createBlankCanvas();
        }

        if (!componentsInitialized) {
            clearComponents();
            createComponents();
            componentsInitialized = true;
        }

        nameField.setText(defaultName);
        statusLabel.setText("");
        updateToolButtons();
        setSlidersFromColor(currentColor);
        colorValueLabel.setText(formatColorLabel(currentColor));

        layoutDirty = true;
        recalculateLayout();
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        if (!componentsInitialized) {
            init();
            return;
        }
        layoutDirty = true;
    }

    @Override
    public void update(float deltaTime) {
        if (layoutDirty) {
            recalculateLayout();
        }
        super.update(deltaTime);
    }

    private void createComponents() {
        float titleR = 0.92f;
        float titleG = 0.88f;
        float titleB = 0.99f;

        titleLabel = new Label(0f, 0f, "SKIN EDITOR", titleR, titleG, titleB, 1.0f);
        titleLabel.setCentered(true);
        addComponent(titleLabel);

        subtitleLabel = new Label(0f, 0f,
            "Paint a 64×64 skin using the tools below",
            0.72f, 0.84f, 0.95f, 0.9f);
        subtitleLabel.setCentered(true);
        addComponent(subtitleLabel);

        canvasComponent = new PixelCanvasComponent(0f, 0f, 0f, 0f);
        addComponent(canvasComponent);

        nameLabel = new Label(0f, 0f, "SKIN NAME",
            0.72f, 0.9f, 0.95f, 1.0f);
        addComponent(nameLabel);

        nameField = new TextField(0f, 0f, 0f, 0f, "Enter skin name");
        nameField.setMaxLength(32);
        addComponent(nameField);

        toolsLabel = new Label(0f, 0f, "TOOLS",
            0.78f, 0.88f, 0.98f, 1.0f);
        addComponent(toolsLabel);

        pencilButton = new MenuButton(0f, 0f, 0f, 0f,
            "PENCIL", () -> setTool(Tool.PENCIL));
        addComponent(pencilButton);

        eraserButton = new MenuButton(0f, 0f, 0f, 0f,
            "ERASER", () -> setTool(Tool.ERASER));
        addComponent(eraserButton);

        pickerLabel = new Label(0f, 0f, "COLOR PICKER",
            0.8f, 0.92f, 1.0f, 1.0f);
        addComponent(pickerLabel);

        colorPreviewComponent = new ColorPreviewComponent(0f, 0f, 0f, 0f);
        addComponent(colorPreviewComponent);

        colorValueLabel = new Label(0f, 0f,
            formatColorLabel(currentColor), 0.88f, 0.92f, 0.98f, 1.0f);
        addComponent(colorValueLabel);

        redSlider = createColorSlider("Red", value -> updateCurrentColor());
        greenSlider = createColorSlider("Green", value -> updateCurrentColor());
        blueSlider = createColorSlider("Blue", value -> updateCurrentColor());
        alphaSlider = createColorSlider("Alpha", value -> updateCurrentColor());

        saveButton = new MenuButton(0f, 0f, 0f, 0f,
            "SAVE", this::saveSkin);
        addComponent(saveButton);

        clearButton = new MenuButton(0f, 0f, 0f, 0f,
            "CLEAR", this::clearCanvas);
        addComponent(clearButton);

        backButton = new MenuButton(0f, 0f, 0f, 0f,
            "CANCEL", () -> uiManager.setState(GameState.SKIN_MANAGER));
        addComponent(backButton);

        statusLabel = new Label(0f, 0f,
            "", 0.95f, 0.65f, 0.65f, 1.0f);
        addComponent(statusLabel);

        updateToolButtons();
    }

    private void recalculateLayout() {
        if (!componentsInitialized) {
            layoutDirty = false;
            return;
        }

        float padding = scaleDimension(36f);
        float titleY = padding;
        float titleX = windowWidth / 2f;
        float norm = scaleManager.getTextScaleForFontSize(uiManager.getCurrentAtlasSize());

        titleLabel.setScale(1.9f * norm);
        titleLabel.setPosition(titleX, titleY);

        float subtitleY = titleY + scaleDimension(46f);
        subtitleLabel.setScale(1.0f * norm);
        subtitleLabel.setPosition(titleX, subtitleY);

        float canvasSize = Math.min(Math.min(windowWidth * 0.55f, windowHeight - padding * 2.5f), scaleDimension(720f));
        float canvasX = padding;
        float canvasY = titleY + scaleDimension(120f);
        canvasComponent.setBounds(canvasX, canvasY, canvasSize, canvasSize);

        float controlsX = canvasX + canvasSize + scaleDimension(40f);
        float controlsWidth = Math.max(0f, windowWidth - controlsX - padding);
        float cursorY = canvasY;

        nameLabel.setScale(1.0f * norm);
        nameLabel.setPosition(controlsX, cursorY);
        cursorY += scaleDimension(34f);

        float nameFieldHeight = scaleDimension(58f);
        nameField.setBounds(controlsX, cursorY, controlsWidth, nameFieldHeight);
        cursorY += nameFieldHeight + scaleDimension(38f);

        toolsLabel.setScale(1.0f * norm);
        toolsLabel.setPosition(controlsX, cursorY);
        cursorY += scaleDimension(32f);

        float toolButtonHeight = scaleDimension(60f);
        float buttonSpacing = scaleDimension(16f);
        float halfWidth = (controlsWidth - buttonSpacing) / 2f;

        pencilButton.setBounds(controlsX, cursorY, halfWidth, toolButtonHeight);
        eraserButton.setBounds(controlsX + (controlsWidth + buttonSpacing) / 2f, cursorY,
            halfWidth, toolButtonHeight);
        cursorY += toolButtonHeight + scaleDimension(42f);

        pickerLabel.setScale(1.0f * norm);
        pickerLabel.setPosition(controlsX, cursorY);
        cursorY += scaleDimension(32f);

        float previewSize = Math.min(controlsWidth, scaleDimension(140f));
        colorPreviewComponent.setBounds(controlsX, cursorY, previewSize, previewSize);

        float colorLabelX = controlsX + previewSize + scaleDimension(16f);
        float colorLabelY = cursorY + previewSize * 0.5f;
        colorValueLabel.setScale(1.0f * norm);
        colorValueLabel.setPosition(colorLabelX, colorLabelY);

        cursorY += previewSize + scaleDimension(28f);

        float sliderHeight = scaleDimension(54f);
        float sliderWidth = controlsWidth;
        float sliderSpacing = scaleDimension(22f);

        applySliderLayout(redSlider, controlsX, cursorY, sliderWidth, sliderHeight, norm);
        cursorY += sliderHeight + sliderSpacing;

        applySliderLayout(greenSlider, controlsX, cursorY, sliderWidth, sliderHeight, norm);
        cursorY += sliderHeight + sliderSpacing;

        applySliderLayout(blueSlider, controlsX, cursorY, sliderWidth, sliderHeight, norm);
        cursorY += sliderHeight + sliderSpacing;

        applySliderLayout(alphaSlider, controlsX, cursorY, sliderWidth, sliderHeight, norm);
        cursorY += sliderHeight + scaleDimension(36f);

        float buttonHeight = scaleDimension(66f);
        float actionButtonSpacing = scaleDimension(18f);
        float buttonWidth = controlsWidth > 0f ? (controlsWidth - actionButtonSpacing * 2) / 3f : 0f;

        saveButton.setBounds(controlsX, cursorY, buttonWidth, buttonHeight);
        clearButton.setBounds(controlsX + buttonWidth + actionButtonSpacing, cursorY,
            buttonWidth, buttonHeight);
        backButton.setBounds(controlsX + (buttonWidth + actionButtonSpacing) * 2, cursorY,
            buttonWidth, buttonHeight);

        cursorY += buttonHeight + scaleDimension(26f);

        statusLabel.setScale(0.9f * norm);
        statusLabel.setPosition(controlsX, cursorY);

        layoutDirty = false;
    }

    private Slider createColorSlider(String label, java.util.function.Consumer<Float> onChange) {
        Slider slider = new Slider(0f, 0f, 0f, 0f, label, 0f, 255f, 255f, onChange);
        slider.setDecimalPlaces(0);
        addComponent(slider);
        return slider;
    }

    private void applySliderLayout(Slider slider, float x, float y, float width, float height, float norm) {
        slider.setBounds(x, y, width, height);
        slider.setFontScale(0.95f * norm, 0.9f * norm);
    }
    private void setTool(Tool tool) {
        this.currentTool = tool;
        updateToolButtons();
    }

    private void updateToolButtons() {
        if (pencilButton != null) {
            pencilButton.setEnabled(currentTool != Tool.PENCIL);
        }
        if (eraserButton != null) {
            eraserButton.setEnabled(currentTool != Tool.ERASER);
        }
    }

    private void updateCurrentColor() {
        int r = Math.round(redSlider.getValue());
        int g = Math.round(greenSlider.getValue());
        int b = Math.round(blueSlider.getValue());
        int a = Math.round(alphaSlider.getValue());
        currentColor = ((a & 0xFF) << 24)
            | ((r & 0xFF) << 16)
            | ((g & 0xFF) << 8)
            | (b & 0xFF);
        if (colorValueLabel != null) {
            colorValueLabel.setText(formatColorLabel(currentColor));
        }
    }

    private void setSlidersFromColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (redSlider != null) {
            redSlider.setValue(r);
        }
        if (greenSlider != null) {
            greenSlider.setValue(g);
        }
        if (blueSlider != null) {
            blueSlider.setValue(b);
        }
        if (alphaSlider != null) {
            alphaSlider.setValue(a);
        }
        updateCurrentColor();
    }

    private void applyTool(int pixelX, int pixelY) {
        if (pixelX < 0 || pixelY < 0 || pixelX >= SKIN_SIZE || pixelY >= SKIN_SIZE) {
            return;
        }
        if (currentTool == Tool.PENCIL) {
            canvas.setRGB(pixelX, pixelY, currentColor);
        } else if (currentTool == Tool.ERASER) {
            canvas.setRGB(pixelX, pixelY, 0x00000000);
        }
    }

    private void saveSkin() {
        statusLabel.setText("");
        String rawName = nameField != null ? nameField.getText().trim() : "";
        if (rawName.isEmpty()) {
            rawName = defaultName;
        }
        String sanitized = sanitizeName(rawName);
        if (sanitized.isEmpty()) {
            sanitized = "custom_skin";
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("poorcraft_skin_", ".png");
            ImageIO.write(canvas, "PNG", tempFile.toFile());

            PlayerSkin imported = skinManager.importSkin(tempFile, sanitized);
            if (imported == null) {
                statusLabel.setText("Failed to save skin. See console for details.");
                return;
            }
            skinManager.setCurrentSkin(imported.getName());
            uiManager.getConfigManager().saveSettings(uiManager.getSettings());
            uiManager.setState(GameState.SKIN_MANAGER);
        } catch (IOException e) {
            statusLabel.setText("Error saving skin: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void clearCanvas() {
        for (int y = 0; y < SKIN_SIZE; y++) {
            for (int x = 0; x < SKIN_SIZE; x++) {
                canvas.setRGB(x, y, 0x00000000);
            }
        }
        statusLabel.setText("Canvas cleared");
    }

    private String sanitizeName(String fileName) {
        String base = fileName.toLowerCase(Locale.ENGLISH);
        if (base.endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }
        base = base.replaceAll("[^a-z0-9_]+", "_");
        base = base.replaceAll("_{2,}", "_");
        base = base.replaceAll("^_+|_+$", "");
        if (base.length() > 32) {
            base = base.substring(0, 32);
        }
        return base;
    }

    private String formatColorLabel(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        return String.format(Locale.ENGLISH, "RGBA %d, %d, %d, %d", r, g, b, a);
    }

    private BufferedImage createBlankCanvas() {
        return new BufferedImage(SKIN_SIZE, SKIN_SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private final class PixelCanvasComponent extends UIComponent {

        private boolean painting;
        private int hoverX = -1;
        private int hoverY = -1;

        PixelCanvasComponent(float x, float y, float width, float height) {
            super(x, y, width, height);
        }

        @Override
        public void render(UIRenderer renderer, FontRenderer fontRenderer) {
            float cellSize = width / SKIN_SIZE;
            for (int py = 0; py < SKIN_SIZE; py++) {
                for (int px = 0; px < SKIN_SIZE; px++) {
                    float cellX = x + px * cellSize;
                    float cellY = y + py * cellSize;

                    float bg = ((px + py) & 1) == 0 ? 0.78f : 0.66f;
                    renderer.drawRect(cellX, cellY, cellSize, cellSize,
                        bg, bg, bg, 1.0f);

                    int argb = canvas.getRGB(px, py);
                    float a = ((argb >> 24) & 0xFF) / 255f;
                    float r = ((argb >> 16) & 0xFF) / 255f;
                    float g = ((argb >> 8) & 0xFF) / 255f;
                    float b = (argb & 0xFF) / 255f;
                    if (a > 0f) {
                        renderer.drawRect(cellX, cellY, cellSize, cellSize,
                            r, g, b, a);
                    }
                }
            }

            if (hoverX >= 0 && hoverY >= 0) {
                float cellSizeHover = width / SKIN_SIZE;
                float hx = x + hoverX * cellSizeHover;
                float hy = y + hoverY * cellSizeHover;
                float border = Math.max(scaleDimension(1.5f), cellSizeHover * 0.07f);
                renderer.drawRect(hx, hy, cellSizeHover, border, 0.05f, 0.95f, 0.95f, 0.8f);
                renderer.drawRect(hx, hy + cellSizeHover - border, cellSizeHover, border, 0.05f, 0.95f, 0.95f, 0.8f);
                renderer.drawRect(hx, hy, border, cellSizeHover, 0.05f, 0.95f, 0.95f, 0.8f);
                renderer.drawRect(hx + cellSizeHover - border, hy, border, cellSizeHover, 0.05f, 0.95f, 0.95f, 0.8f);
            }
        }

        @Override
        public void update(float deltaTime) {
            // No animations required for canvas.
        }

        @Override
        public void onMouseMove(float mouseX, float mouseY) {
            super.onMouseMove(mouseX, mouseY);
            float cellSize = width / SKIN_SIZE;
            hoverX = (int) Math.floor((mouseX - x) / cellSize);
            hoverY = (int) Math.floor((mouseY - y) / cellSize);
            if (!isMouseOver(mouseX, mouseY)) {
                hoverX = -1;
                hoverY = -1;
            }
            if (painting && hoverX >= 0 && hoverY >= 0) {
                applyTool(hoverX, hoverY);
            }
        }

        @Override
        public void onMouseClick(float mouseX, float mouseY, int button) {
            if (button == 0 && isMouseOver(mouseX, mouseY)) {
                painting = true;
                if (hoverX >= 0 && hoverY >= 0) {
                    applyTool(hoverX, hoverY);
                }
            }
        }

        @Override
        public void onMouseRelease(float mouseX, float mouseY, int button) {
            if (button == 0) {
                painting = false;
            }
        }
    }

    private final class ColorPreviewComponent extends UIComponent {

        ColorPreviewComponent(float x, float y, float width, float height) {
            super(x, y, width, height);
        }

        @Override
        public void render(UIRenderer renderer, FontRenderer fontRenderer) {
            float cell = width / 4f;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    float cx = x + col * cell;
                    float cy = y + row * cell;
                    boolean light = ((row + col) & 1) == 0;
                    renderer.drawRect(cx, cy, cell, cell,
                        light ? 0.82f : 0.68f,
                        light ? 0.82f : 0.68f,
                        light ? 0.86f : 0.72f,
                        1.0f);
                }
            }

            float a = ((currentColor >> 24) & 0xFF) / 255f;
            float r = ((currentColor >> 16) & 0xFF) / 255f;
            float g = ((currentColor >> 8) & 0xFF) / 255f;
            float b = (currentColor & 0xFF) / 255f;
            renderer.drawRect(x, y, width, height, r, g, b, a);
        }

        @Override
        public void update(float deltaTime) {
            // Static preview.
        }
    }
}
