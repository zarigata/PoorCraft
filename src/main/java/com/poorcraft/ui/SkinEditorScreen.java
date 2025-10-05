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

    public SkinEditorScreen(int windowWidth, int windowHeight, UIManager uiManager) {
        super(windowWidth, windowHeight);
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
        clearComponents();
        if (canvas == null) {
            canvas = createBlankCanvas();
        }
        buildLayout();
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        init();
    }

    private void buildLayout() {
        float padding = Math.max(36f, windowWidth * 0.04f);
        float titleY = padding;
        float titleX = windowWidth / 2f;

        Label title = new Label(titleX, titleY, "SKIN EDITOR",
            0.92f, 0.88f, 0.99f, 1.0f);
        title.setCentered(true);
        title.setScale(Math.max(1.9f, windowWidth / 820f));
        addComponent(title);

        Label subtitle = new Label(titleX, titleY + 46f,
            "Paint a 64×64 skin using the tools below",
            0.72f, 0.84f, 0.95f, 0.9f);
        subtitle.setCentered(true);
        subtitle.setScale(Math.max(1.0f, windowWidth / 980f));
        addComponent(subtitle);

        float canvasSize = Math.min(Math.min(windowWidth * 0.55f, windowHeight - padding * 2.5f), 720f);
        float canvasX = padding;
        float canvasY = titleY + Math.max(120f, windowHeight * 0.12f);

        canvasComponent = new PixelCanvasComponent(canvasX, canvasY, canvasSize, canvasSize);
        addComponent(canvasComponent);

        float controlsX = canvasX + canvasSize + Math.max(40f, windowWidth * 0.03f);
        float controlsWidth = windowWidth - controlsX - padding;
        float cursorY = canvasY;

        Label nameLabel = new Label(controlsX, cursorY, "SKIN NAME",
            0.72f, 0.9f, 0.95f, 1.0f);
        nameLabel.setScale(Math.max(1.0f, controlsWidth / 480f));
        addComponent(nameLabel);
        cursorY += 34f;

        float nameFieldHeight = Math.max(58f, windowHeight * 0.08f);
        nameField = new TextField(controlsX, cursorY, controlsWidth, nameFieldHeight, "Enter skin name");
        nameField.setMaxLength(32);
        nameField.setText(defaultName);
        addComponent(nameField);
        cursorY += nameFieldHeight + 38f;

        Label toolsLabel = new Label(controlsX, cursorY, "TOOLS",
            0.78f, 0.88f, 0.98f, 1.0f);
        toolsLabel.setScale(Math.max(1.0f, controlsWidth / 520f));
        addComponent(toolsLabel);
        cursorY += 32f;

        float toolButtonHeight = Math.max(60f, windowHeight * 0.08f);
        pencilButton = new MenuButton(controlsX, cursorY, (controlsWidth - 16f) / 2f, toolButtonHeight,
            "PENCIL", () -> setTool(Tool.PENCIL));
        addComponent(pencilButton);

        eraserButton = new MenuButton(controlsX + (controlsWidth + 16f) / 2f, cursorY,
            (controlsWidth - 16f) / 2f, toolButtonHeight,
            "ERASER", () -> setTool(Tool.ERASER));
        addComponent(eraserButton);
        cursorY += toolButtonHeight + 42f;

        Label pickerLabel = new Label(controlsX, cursorY, "COLOR PICKER",
            0.8f, 0.92f, 1.0f, 1.0f);
        pickerLabel.setScale(Math.max(1.0f, controlsWidth / 520f));
        addComponent(pickerLabel);
        cursorY += 32f;

        float previewSize = Math.min(controlsWidth, 140f);
        colorPreviewComponent = new ColorPreviewComponent(controlsX, cursorY, previewSize, previewSize);
        addComponent(colorPreviewComponent);

        colorValueLabel = new Label(controlsX + previewSize + 16f, cursorY + previewSize * 0.5f,
            formatColorLabel(currentColor), 0.88f, 0.92f, 0.98f, 1.0f);
        colorValueLabel.setScale(Math.max(1.0f, controlsWidth / 540f));
        addComponent(colorValueLabel);

        cursorY += previewSize + 28f;

        float sliderHeight = Math.max(54f, windowHeight * 0.07f);
        float sliderWidth = controlsWidth;

        redSlider = createColorSlider("Red", controlsX, cursorY, sliderWidth, sliderHeight,
            value -> updateCurrentColor());
        cursorY += sliderHeight + 22f;

        greenSlider = createColorSlider("Green", controlsX, cursorY, sliderWidth, sliderHeight,
            value -> updateCurrentColor());
        cursorY += sliderHeight + 22f;

        blueSlider = createColorSlider("Blue", controlsX, cursorY, sliderWidth, sliderHeight,
            value -> updateCurrentColor());
        cursorY += sliderHeight + 22f;

        alphaSlider = createColorSlider("Alpha", controlsX, cursorY, sliderWidth, sliderHeight,
            value -> updateCurrentColor());
        cursorY += sliderHeight + 36f;

        setSlidersFromColor(currentColor);

        float buttonHeight = Math.max(66f, windowHeight * 0.09f);
        float buttonSpacing = 18f;
        float buttonWidth = (controlsWidth - buttonSpacing * 2) / 3f;

        MenuButton saveButton = new MenuButton(controlsX, cursorY, buttonWidth, buttonHeight,
            "SAVE", this::saveSkin);
        addComponent(saveButton);

        MenuButton clearButton = new MenuButton(controlsX + buttonWidth + buttonSpacing, cursorY,
            buttonWidth, buttonHeight,
            "CLEAR", this::clearCanvas);
        addComponent(clearButton);

        MenuButton backButton = new MenuButton(controlsX + (buttonWidth + buttonSpacing) * 2, cursorY,
            buttonWidth, buttonHeight,
            "CANCEL", () -> uiManager.setState(GameState.SKIN_MANAGER));
        addComponent(backButton);

        cursorY += buttonHeight + 26f;

        statusLabel = new Label(controlsX, cursorY,
            "", 0.95f, 0.65f, 0.65f, 1.0f);
        statusLabel.setScale(Math.max(0.9f, controlsWidth / 680f));
        addComponent(statusLabel);

        updateToolButtons();
    }

    private Slider createColorSlider(String label, float x, float y, float width, float height,
                                     java.util.function.Consumer<Float> onChange) {
        Slider slider = new Slider(x, y, width, height, label, 0f, 255f, 255f, onChange);
        slider.setDecimalPlaces(0);
        slider.setFontScale(Math.max(0.95f, width / 720f), Math.max(0.9f, width / 780f));
        addComponent(slider);
        return slider;
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
                float border = Math.max(1.5f, cellSizeHover * 0.07f);
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
