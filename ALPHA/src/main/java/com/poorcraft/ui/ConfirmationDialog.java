package com.poorcraft.ui;

/**
 * Modal confirmation dialog with Yes/No buttons.
 * Used for critical actions that need user confirmation.
 */
public class ConfirmationDialog extends UIComponent {
    
    private String message;
    private Runnable onConfirm;
    private Runnable onCancel;
    private boolean visible;
    
    private float dialogX;
    private float dialogY;
    private float dialogWidth;
    private float dialogHeight;
    
    private Button yesButton;
    private Button noButton;
    
    /**
     * Creates a confirmation dialog.
     * 
     * @param message Message to display
     * @param onConfirm Callback when user confirms
     * @param onCancel Callback when user cancels
     */
    public ConfirmationDialog(String message, Runnable onConfirm, Runnable onCancel) {
        super(0, 0, 0, 0);
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.visible = false;
    }
    
    /**
     * Shows the dialog centered on screen.
     * 
     * @param windowWidth Window width
     * @param windowHeight Window height
     */
    public void show(int windowWidth, int windowHeight) {
        this.visible = true;
        
        // Center dialog
        dialogWidth = 400;
        dialogHeight = 180;
        dialogX = (windowWidth - dialogWidth) / 2.0f;
        dialogY = (windowHeight - dialogHeight) / 2.0f;
        
        // Create buttons
        float buttonWidth = 120;
        float buttonHeight = 40;
        float buttonY = dialogY + dialogHeight - buttonHeight - 20;
        float buttonSpacing = 20;
        float centerX = dialogX + dialogWidth / 2.0f;
        
        yesButton = new Button(
            centerX - buttonWidth - buttonSpacing / 2, buttonY,
            buttonWidth, buttonHeight,
            "Yes",
            () -> {
                visible = false;
                if (onConfirm != null) {
                    onConfirm.run();
                }
            }
        );
        
        noButton = new Button(
            centerX + buttonSpacing / 2, buttonY,
            buttonWidth, buttonHeight,
            "No",
            () -> {
                visible = false;
                if (onCancel != null) {
                    onCancel.run();
                }
            }
        );
    }
    
    /**
     * Hides the dialog.
     */
    public void hide() {
        this.visible = false;
    }
    
    /**
     * Checks if dialog is visible.
     */
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (!visible) {
            return;
        }
        
        // Semi-transparent overlay
        renderer.drawRect(0, 0, 10000, 10000, 0.0f, 0.0f, 0.0f, 0.6f);
        
        // Dialog background
        renderer.drawRect(dialogX, dialogY, dialogWidth, dialogHeight,
            0.12f, 0.08f, 0.15f, 0.98f);
        
        // Border
        float border = 2.0f;
        renderer.drawRect(dialogX, dialogY, dialogWidth, border,
            0.0f, 0.95f, 0.95f, 0.9f);
        renderer.drawRect(dialogX, dialogY + dialogHeight - border, dialogWidth, border,
            0.0f, 0.95f, 0.95f, 0.9f);
        renderer.drawRect(dialogX, dialogY, border, dialogHeight,
            0.0f, 0.95f, 0.95f, 0.9f);
        renderer.drawRect(dialogX + dialogWidth - border, dialogY, border, dialogHeight,
            0.0f, 0.95f, 0.95f, 0.9f);
        
        // Message text
        float textY = dialogY + 40;
        fontRenderer.drawText(message, dialogX + dialogWidth / 2.0f, textY,
            1.0f, 1.0f, 1.0f, 1.0f);
        
        // Buttons
        if (yesButton != null) {
            yesButton.render(renderer, fontRenderer);
        }
        if (noButton != null) {
            noButton.render(renderer, fontRenderer);
        }
    }
    
    @Override
    public void onMouseMove(float mouseX, float mouseY) {
        if (!visible) {
            return;
        }
        if (yesButton != null) {
            yesButton.onMouseMove(mouseX, mouseY);
        }
        if (noButton != null) {
            noButton.onMouseMove(mouseX, mouseY);
        }
    }
    
    @Override
    public void onMouseClick(float mouseX, float mouseY, int button) {
        if (!visible) {
            return;
        }
        if (yesButton != null) {
            yesButton.onMouseClick(mouseX, mouseY, button);
        }
        if (noButton != null) {
            noButton.onMouseClick(mouseX, mouseY, button);
        }
    }
    
    @Override
    public void onMouseRelease(float mouseX, float mouseY, int button) {
        if (!visible) {
            return;
        }
        if (yesButton != null) {
            yesButton.onMouseRelease(mouseX, mouseY, button);
        }
        if (noButton != null) {
            noButton.onMouseRelease(mouseX, mouseY, button);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // No animation needed
    }
}
