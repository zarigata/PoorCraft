package com.poorcraft.ui;

import com.poorcraft.ai.AICompanionManager;
import com.poorcraft.config.ConfigManager;
import com.poorcraft.config.Settings;
import com.poorcraft.core.Game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated settings screen for configuring the AI companion.
 */
public class AICompanionScreen extends UIScreen {

    private static final List<String> PROVIDERS = Arrays.asList("ollama", "gemini", "openrouter");
    private static final float PANEL_OPACITY = 0.85f;

    private final UIManager uiManager;
    private final ConfigManager configManager;
    private final Settings settings;

    private Settings.AISettings workingSettings;
    private ScrollContainer scrollContainer;
    private MenuBackground background;

    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float contentPadding;
    private boolean layoutDirty;

    private Checkbox aiEnabledCheckbox;
    private Dropdown providerDropdown;
    private Checkbox spawnOnStartCheckbox;
    private Checkbox enableActionsCheckbox;
    private Checkbox filterReasoningCheckbox;
    private Checkbox logReasoningCheckbox;
    private Slider followDistanceSlider;
    private Slider cooldownSlider;
    private Slider gatherDistanceSlider;
    private TextField companionNameField;
    private TextField companionSkinField;
    private TextField systemPromptField;

    private final Map<String, TextField> apiKeyFields;
    private final Map<String, TextField> modelFields;

    public AICompanionScreen(int windowWidth, int windowHeight, UIManager uiManager,
                             Settings settings, ConfigManager configManager, UIScaleManager scaleManager) {
        super(windowWidth, windowHeight, scaleManager);
        this.uiManager = uiManager;
        this.configManager = configManager;
        this.settings = settings;
        this.apiKeyFields = new HashMap<>();
        this.modelFields = new HashMap<>();
        this.background = new MenuBackground();
        this.layoutDirty = true;
    }

    @Override
    public void init() {
        ensureWorkingSettings();
        layoutDirty = true;
        rebuildLayout();
    }

    private void rebuildLayout() {
        ensureWorkingSettings();
        if (scrollContainer != null) {
            applyFieldValuesToWorkingSettings();
        }
        clearComponents();
        scrollContainer = null;
        buildLayout();
        layoutDirty = false;
    }

    private void buildLayout() {
        float uiScale = settings != null && settings.graphics != null ? settings.graphics.uiScale : 1.0f;
        float effectiveScale = scaleManager != null ? scaleManager.getEffectiveScale() : uiScale;

        panelWidth = scaleManager != null ? LayoutUtils.getScaledPanelWidth(scaleManager)
            : LayoutUtils.getMinecraftPanelWidth(windowWidth, uiScale);
        panelHeight = scaleManager != null ? LayoutUtils.getScaledPanelHeight(scaleManager)
            : LayoutUtils.getMinecraftPanelHeight(windowHeight, uiScale);

        float horizontalMargin = scaleManager != null
            ? scaleManager.scaleDimension(120f)
            : 120f * effectiveScale;
        float verticalMargin = scaleManager != null
            ? scaleManager.scaleDimension(140f)
            : 140f * effectiveScale;
        float minWidth = 340f * effectiveScale;
        float minHeight = 320f * effectiveScale;
        float maxWidth = Math.max(minWidth, windowWidth - horizontalMargin);
        float maxHeight = Math.max(minHeight, windowHeight - verticalMargin);
        panelWidth = LayoutUtils.clamp(panelWidth, minWidth, maxWidth);
        panelHeight = LayoutUtils.clamp(panelHeight, minHeight, maxHeight);

        panelX = LayoutUtils.centerHorizontally(windowWidth, panelWidth);
        panelY = LayoutUtils.centerVertically(windowHeight, panelHeight);
        contentPadding = scaleManager != null ? LayoutUtils.getScaledPadding(scaleManager)
            : LayoutUtils.getMinecraftPanelPadding(panelWidth);

        float titleY = panelY + scaleDimension(28f);
        Label titleLabel = new Label(panelX + panelWidth / 2f, titleY,
            "AI COMPANION SETTINGS", 0.02f, 0.96f, 0.96f, 1.0f);
        titleLabel.setCentered(true);
        float titleScale = LayoutUtils.MINECRAFT_TITLE_SCALE * (scaleManager != null ? scaleManager.getEffectiveScale() : uiScale) * 0.55f;
        titleLabel.setScale(titleScale);
        titleLabel.setUseTextShadow(true);
        addComponent(titleLabel);

        float containerWidth = panelWidth - (contentPadding * 2f);
        float containerX = panelX + contentPadding;
        float containerY = titleY + scaleDimension(48f);
        float containerHeight = panelHeight - (containerY - panelY) - (contentPadding * 1.2f);

        scrollContainer = new ScrollContainer(
            containerX,
            containerY,
            containerWidth,
            Math.max(scaleDimension(260f), containerHeight)
        );
        addComponent(scrollContainer);

        float contentX = containerX;
        float currentY = containerY + scaleDimension(10f);

        currentY = addGeneralSection(contentX, currentY);
        currentY = addIdentitySection(contentX, currentY + scaleDimension(20f));
        currentY = addBehaviorSection(contentX, currentY + scaleDimension(20f));
        currentY = addProviderSection(contentX, currentY + scaleDimension(20f));
        addPromptSection(contentX, currentY + scaleDimension(20f));

        scrollContainer.requestLayout();

        float buttonWidth = scaleManager != null ? LayoutUtils.getScaledButtonWidth(scaleManager)
            : LayoutUtils.getMinecraftButtonWidth(windowWidth, uiScale);
        float buttonHeight = scaleManager != null ? LayoutUtils.getScaledButtonHeight(scaleManager)
            : LayoutUtils.getMinecraftButtonHeight(windowHeight, uiScale);
        float buttonSpacing = LayoutUtils.getMinecraftButtonSpacing(buttonHeight);
        float buttonRowY = panelY + panelHeight - contentPadding - buttonHeight;
        float buttonRowWidth = (buttonWidth * 3f) + (buttonSpacing * 2f);
        float buttonStartX = panelX + (panelWidth - buttonRowWidth) / 2f;

        MenuButton saveButton = new MenuButton(buttonStartX, buttonRowY, buttonWidth, buttonHeight, "Save", this::onSave);
        addComponent(saveButton);

        MenuButton cancelButton = new MenuButton(buttonStartX + buttonWidth + buttonSpacing, buttonRowY, buttonWidth, buttonHeight, "Cancel", this::onCancel);
        addComponent(cancelButton);

        MenuButton testButton = new MenuButton(buttonStartX + (buttonWidth + buttonSpacing) * 2f, buttonRowY, buttonWidth, buttonHeight, "Test", this::onTestConnection);
        addComponent(testButton);
    }

    private float addGeneralSection(float x, float startY) {
        float currentY = addSectionHeader(x, startY, "General");

        aiEnabledCheckbox = createCheckbox(x, currentY, "Enable AI Companion", workingSettings.aiEnabled,
            checked -> workingSettings.aiEnabled = checked);
        currentY += scaleDimension(28f);

        int selectedIndex = PROVIDERS.indexOf(normalizeProvider(workingSettings.aiProvider));
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        float dropdownWidth = scrollContainer.getWidth() - (x - scrollContainer.getX());
        providerDropdown = new Dropdown(
            x,
            currentY,
            dropdownWidth,
            scaleDimension(36f),
            PROVIDERS,
            selectedIndex,
            index -> workingSettings.aiProvider = PROVIDERS.get(Math.max(0, Math.min(index, PROVIDERS.size() - 1)))
        );
        scrollContainer.addChild(providerDropdown);
        currentY += scaleDimension(50f);

        spawnOnStartCheckbox = createCheckbox(x, currentY, "Spawn companion when world loads", workingSettings.spawnOnStart,
            checked -> workingSettings.spawnOnStart = checked);
        currentY += scaleDimension(32f);

        return currentY;
    }

    private float addIdentitySection(float x, float startY) {
        float currentY = addSectionHeader(x, startY, "Identity");

        companionNameField = createTextField(x, currentY, "Companion Name", workingSettings.companionName, 64);
        currentY += scaleDimension(52f);

        companionSkinField = createTextField(x, currentY, "Companion Skin", workingSettings.companionSkin, 64);
        currentY += scaleDimension(52f);

        return currentY;
    }

    private float addBehaviorSection(float x, float startY) {
        float currentY = addSectionHeader(x, startY, "Behavior");

        followDistanceSlider = createSlider(x, currentY, "Follow Distance", 1f, 32f, workingSettings.followDistance,
            value -> workingSettings.followDistance = value);
        followDistanceSlider.setDecimalPlaces(1);
        currentY += scaleDimension(70f);

        enableActionsCheckbox = createCheckbox(x, currentY, "Enable companion actions", workingSettings.enableActions,
            checked -> workingSettings.enableActions = checked);
        currentY += scaleDimension(32f);

        cooldownSlider = createSlider(x, currentY, "Action Cooldown (seconds)", 1f, 180f, workingSettings.actionCooldownSeconds,
            value -> workingSettings.actionCooldownSeconds = Math.round(value));
        cooldownSlider.setDecimalPlaces(0);
        currentY += scaleDimension(70f);

        gatherDistanceSlider = createSlider(x, currentY, "Max Gather Distance", 1f, 128f, workingSettings.maxGatherDistance,
            value -> workingSettings.maxGatherDistance = Math.round(value));
        gatherDistanceSlider.setDecimalPlaces(0);
        currentY += scaleDimension(70f);

        filterReasoningCheckbox = createCheckbox(x, currentY, "Filter provider reasoning from chat", workingSettings.filterReasoning,
            checked -> workingSettings.filterReasoning = checked);
        currentY += scaleDimension(28f);

        logReasoningCheckbox = createCheckbox(x, currentY, "Log reasoning to console", workingSettings.logReasoning,
            checked -> workingSettings.logReasoning = checked);
        currentY += scaleDimension(32f);

        return currentY;
    }

    private float addProviderSection(float x, float startY) {
        float currentY = addSectionHeader(x, startY, "Provider Configuration");
        float indentX = x + scaleDimension(20f);

        apiKeyFields.clear();
        modelFields.clear();

        for (String provider : PROVIDERS) {
            String uppercase = provider.toUpperCase();
            Label providerLabel = new Label(indentX, currentY, uppercase, 0.85f, 0.92f, 1.0f, 1.0f);
            providerLabel.setScale(0.9f * (scaleManager != null ? scaleManager.getEffectiveScale() : 1.0f));
            scrollContainer.addChild(providerLabel);
            currentY += scaleDimension(28f);

            TextField apiKeyField = createTextField(indentX, currentY, provider + " API Key",
                workingSettings.apiKeys != null ? workingSettings.apiKeys.getOrDefault(provider, "") : "", 256);
            apiKeyFields.put(provider, apiKeyField);
            currentY += scaleDimension(52f);

            TextField modelField = createTextField(indentX, currentY, provider + " Model",
                workingSettings.models != null ? workingSettings.models.getOrDefault(provider, "") : "", 128);
            modelFields.put(provider, modelField);
            currentY += scaleDimension(58f);
        }

        return currentY;
    }

    private void addPromptSection(float x, float startY) {
        float currentY = addSectionHeader(x, startY, "System Prompt");

        systemPromptField = createTextField(x, currentY, "Prompt", workingSettings.systemPrompt, 1024);
        systemPromptField.setHeight(scaleDimension(48f));
    }

    private float addSectionHeader(float x, float y, String title) {
        Label header = new Label(x, y, title, 0.8f, 0.88f, 1.0f, 1.0f);
        header.setScale(1.0f * (scaleManager != null ? scaleManager.getEffectiveScale() : 1.0f));
        scrollContainer.addChild(header);
        return y + scaleDimension(30f);
    }

    private Checkbox createCheckbox(float x, float y, String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        Checkbox checkbox = new Checkbox(x, y, scaleDimension(22f), label, initial, onChange);
        scrollContainer.addChild(checkbox);
        return checkbox;
    }

    private TextField createTextField(float x, float y, String label, String value, int maxLength) {
        Label fieldLabel = new Label(x, y, label);
        scrollContainer.addChild(fieldLabel);

        float fieldY = y + scaleDimension(22f);
        float availableWidth = scrollContainer.getWidth() - (x - scrollContainer.getX());
        TextField field = new TextField(x, fieldY, availableWidth, scaleDimension(38f), label);
        field.setText(value != null ? value : "");
        field.setMaxLength(maxLength);
        scrollContainer.addChild(field);
        return field;
    }

    private Slider createSlider(float x, float y, String label, float min, float max, float initialValue,
                                java.util.function.Consumer<Float> onChange) {
        Label sliderLabel = new Label(x, y, label);
        scrollContainer.addChild(sliderLabel);

        float availableWidth = scrollContainer.getWidth() - (x - scrollContainer.getX());
        Slider slider = new Slider(x, y + scaleDimension(22f), availableWidth, scaleDimension(36f),
            label, min, max, initialValue, onChange);
        scrollContainer.addChild(slider);
        return slider;
    }

    private void onSave() {
        applyFieldValuesToWorkingSettings();
        settings.ai = cloneAISettings(workingSettings);
        try {
            configManager.saveSettings(settings);
        } catch (Exception e) {
            System.err.println("[AICompanionScreen] Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
        safeSetState(GameState.SETTINGS_MENU, "save AI companion settings");
    }

    private void onCancel() {
        safeSetState(GameState.SETTINGS_MENU, "cancel AI companion configuration");
    }

    private void onTestConnection() {
        applyFieldValuesToWorkingSettings();

        if (!workingSettings.aiEnabled) {
            System.out.println("[AI] Enable the AI companion before testing the connection.");
            return;
        }

        Game game = uiManager.getGame();
        if (game == null) {
            System.out.println("[AI] Cannot test connection - game not initialized.");
            return;
        }

        AICompanionManager manager = game.getAICompanionManager();
        if (manager == null) {
            System.out.println("[AI] Companion manager not initialized yet.");
            return;
        }

        manager.testConnection(cloneAISettings(workingSettings));
    }

    private void applyFieldValuesToWorkingSettings() {
        if (workingSettings == null) {
            return;
        }

        if (companionNameField != null) {
            workingSettings.companionName = sanitize(companionNameField.getText(), "Companion");
        }
        if (companionSkinField != null) {
            workingSettings.companionSkin = sanitize(companionSkinField.getText(), workingSettings.companionSkin);
        }
        if (systemPromptField != null) {
            workingSettings.systemPrompt = sanitize(systemPromptField.getText(), workingSettings.systemPrompt);
        }

        Map<String, String> updatedKeys = new HashMap<>();
        for (Map.Entry<String, TextField> entry : apiKeyFields.entrySet()) {
            updatedKeys.put(entry.getKey(), sanitize(entry.getValue().getText(), ""));
        }
        workingSettings.apiKeys = updatedKeys;

        Map<String, String> updatedModels = new HashMap<>();
        for (Map.Entry<String, TextField> entry : modelFields.entrySet()) {
            updatedModels.put(entry.getKey(), sanitize(entry.getValue().getText(), ""));
        }
        workingSettings.models = updatedModels;

        if (aiEnabledCheckbox != null) {
            workingSettings.aiEnabled = aiEnabledCheckbox.isChecked();
        }
        if (providerDropdown != null) {
            int idx = providerDropdown.getSelectedIndex();
            if (idx < 0 || idx >= PROVIDERS.size()) {
                idx = 0;
            }
            workingSettings.aiProvider = PROVIDERS.get(idx);
        } else {
            workingSettings.aiProvider = normalizeProvider(workingSettings.aiProvider);
        }
        if (spawnOnStartCheckbox != null) {
            workingSettings.spawnOnStart = spawnOnStartCheckbox.isChecked();
        }
        if (enableActionsCheckbox != null) {
            workingSettings.enableActions = enableActionsCheckbox.isChecked();
        }
        if (filterReasoningCheckbox != null) {
            workingSettings.filterReasoning = filterReasoningCheckbox.isChecked();
        }
        if (logReasoningCheckbox != null) {
            workingSettings.logReasoning = logReasoningCheckbox.isChecked();
        }
        if (followDistanceSlider != null) {
            workingSettings.followDistance = followDistanceSlider.getValue();
        }
        if (cooldownSlider != null) {
            workingSettings.actionCooldownSeconds = Math.round(cooldownSlider.getValue());
        }
        if (gatherDistanceSlider != null) {
            workingSettings.maxGatherDistance = Math.round(gatherDistanceSlider.getValue());
        }

        ensureProviderEntries(workingSettings.apiKeys);
        ensureProviderEntries(workingSettings.models);
    }

    private Settings.AISettings cloneAISettings(Settings.AISettings source) {
        Settings.AISettings copy = new Settings.AISettings();
        if (source == null) {
            return createDefaultSettings();
        }

        copy.aiEnabled = source.aiEnabled;
        copy.aiProvider = normalizeProvider(source.aiProvider);
        copy.companionName = sanitize(source.companionName, "Companion");
        copy.companionSkin = sanitize(source.companionSkin, "alex");
        copy.spawnOnStart = source.spawnOnStart;
        copy.followDistance = source.followDistance;
        copy.enableActions = source.enableActions;
        copy.actionCooldownSeconds = source.actionCooldownSeconds;
        copy.maxGatherDistance = source.maxGatherDistance;
        copy.filterReasoning = source.filterReasoning;
        copy.logReasoning = source.logReasoning;
        copy.systemPrompt = source.systemPrompt != null ? source.systemPrompt : "";
        copy.apiKeys = source.apiKeys != null ? new HashMap<>(source.apiKeys) : new HashMap<>();
        copy.models = source.models != null ? new HashMap<>(source.models) : new HashMap<>();
        ensureProviderEntries(copy.apiKeys);
        ensureProviderEntries(copy.models);
        return copy;
    }

    private void ensureWorkingSettings() {
        if (settings.ai == null) {
            settings.ai = createDefaultSettings();
        }
        if (workingSettings == null) {
            workingSettings = cloneAISettings(settings.ai);
        }
    }

    private Settings.AISettings createDefaultSettings() {
        Settings.AISettings defaults = new Settings.AISettings();
        defaults.aiEnabled = false;
        defaults.aiProvider = PROVIDERS.get(0);
        defaults.companionName = "Companion";
        defaults.companionSkin = "alex";
        defaults.spawnOnStart = false;
        defaults.followDistance = 3.0f;
        defaults.enableActions = true;
        defaults.actionCooldownSeconds = 10;
        defaults.maxGatherDistance = 20;
        defaults.filterReasoning = true;
        defaults.logReasoning = false;
        defaults.systemPrompt = "You are a helpful AI companion in a Minecraft-like world. Keep responses concise.";
        defaults.apiKeys = new HashMap<>();
        defaults.models = new HashMap<>();
        defaults.models.put("ollama", "llama2");
        defaults.models.put("gemini", "gemini-pro");
        defaults.models.put("openrouter", "openai/gpt-3.5-turbo");
        ensureProviderEntries(defaults.apiKeys);
        ensureProviderEntries(defaults.models);
        return defaults;
    }

    private String sanitize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return PROVIDERS.get(0);
        }
        String lower = provider.toLowerCase();
        return PROVIDERS.contains(lower) ? lower : PROVIDERS.get(0);
    }

    private void ensureProviderEntries(Map<String, String> map) {
        if (map == null) {
            return;
        }
        for (String provider : PROVIDERS) {
            map.putIfAbsent(provider, "");
        }
    }

    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        ensureWorkingSettings();
        layoutDirty = true;
    }

    @Override
    public void update(float deltaTime) {
        if (layoutDirty) {
            rebuildLayout();
        }
        if (background != null) {
            background.update(deltaTime);
        }
        super.update(deltaTime);
    }

    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        if (background != null) {
            background.render(renderer, windowWidth, windowHeight);
        } else {
            renderer.drawRect(0f, 0f, windowWidth, windowHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        }

        renderer.drawDropShadow(panelX, panelY, panelWidth, panelHeight, 8f, 0.5f);
        renderer.drawOutsetPanel(panelX, panelY, panelWidth, panelHeight,
            0.10f, 0.08f, 0.14f, PANEL_OPACITY);
        renderer.drawBorderedRect(panelX + 2f, panelY + 2f, panelWidth - 4f, panelHeight - 4f, 2f,
            new float[]{0f, 0f, 0f, 0f}, new float[]{0.0f, 0.95f, 0.95f, 0.4f});

        super.render(renderer, fontRenderer);
    }

    public void cleanup() {
        if (background != null) {
            background.cleanup();
        }
    }

    private void safeSetState(GameState targetState, String actionDescription) {
        try {
            uiManager.setState(targetState);
        } catch (Exception primaryError) {
            System.err.println("[AICompanionScreen] Failed to " + actionDescription + ": " + primaryError.getMessage());
            primaryError.printStackTrace();
            try {
                uiManager.setState(GameState.MAIN_MENU);
                System.err.println("[AICompanionScreen] Recovered by returning to main menu.");
            } catch (Exception fallbackError) {
                System.err.println("[AICompanionScreen] Fallback to main menu also failed: " + fallbackError.getMessage());
                fallbackError.printStackTrace();
            }
        }
    }
}
