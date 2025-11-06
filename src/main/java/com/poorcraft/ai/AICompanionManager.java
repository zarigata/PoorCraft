package com.poorcraft.ai;

import com.google.gson.JsonSyntaxException;
import com.poorcraft.ai.AIProvider.AIConfig;
import com.poorcraft.ai.AIProvider.AIResponse;
import com.poorcraft.ai.providers.GeminiProvider;
import com.poorcraft.ai.providers.OllamaProvider;
import com.poorcraft.ai.providers.OpenRouterProvider;
import com.poorcraft.config.Settings;
import com.poorcraft.world.entity.NPCEntity;
import com.poorcraft.world.entity.NPCManager;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Coordinates the AI companion lifecycle and interactions.
 */
public class AICompanionManager {

    private static final List<String> PROVIDER_PRIORITY = List.of("ollama", "openrouter", "gemini");
    private static final Pattern FOLLOW_PATTERN = Pattern.compile("\\bfollow\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STOP_PATTERN = Pattern.compile("\\bstop\\b", Pattern.CASE_INSENSITIVE);

    private final Map<String, AIProvider> providers;
    private AIProvider currentProvider;
    private NPCManager npcManager;
    private Settings.AISettings aiSettings;
    private int companionNpcId;
    private long lastRequestTime;
    private ExecutorService aiExecutor;
    private volatile CompanionStatus companionStatus;
    private volatile String companionStatusDetail;
    private volatile long statusUpdatedAt;
    private final Queue<CompanionNotification> notificationQueue;
    private Consumer<Runnable> mainThreadPoster;

    public AICompanionManager() {
        this.providers = new HashMap<>();
        this.currentProvider = null;
        this.npcManager = null;
        this.aiSettings = null;
        this.companionNpcId = -1;
        this.lastRequestTime = 0L;
        this.aiExecutor = null;
        this.companionStatus = CompanionStatus.OFFLINE;
        this.companionStatusDetail = "Unavailable";
        this.statusUpdatedAt = System.currentTimeMillis();
        this.notificationQueue = new ConcurrentLinkedQueue<>();
        this.mainThreadPoster = Runnable::run;
    }

    public void init(Settings.AISettings aiSettings, NPCManager npcManager) {
        init(aiSettings, npcManager, Runnable::run);
    }

    public void init(Settings.AISettings aiSettings, NPCManager npcManager, Consumer<Runnable> mainThreadPoster) {
        this.aiSettings = aiSettings;
        this.npcManager = npcManager;
        this.mainThreadPoster = mainThreadPoster != null ? mainThreadPoster : Runnable::run;

        registerProviders();
        selectProvider(aiSettings != null ? aiSettings.aiProvider : null);

        ensureExecutor();

        if (aiSettings == null || !aiSettings.aiEnabled || currentProvider == null) {
            setStatus(CompanionStatus.OFFLINE, "AI disabled");
        } else {
            setStatus(CompanionStatus.IDLE, "Awaiting companion spawn");
            maybeSpawnCompanionOnStart();
        }

        System.out.println("[AI] Companion manager initialized with provider: "
            + (currentProvider != null ? currentProvider.getName() : "none"));
    }

    private void registerProviders() {
        providers.clear();
        registerProvider("ollama", new OllamaProvider());
        registerProvider("gemini", new GeminiProvider());
        registerProvider("openrouter", new OpenRouterProvider());
    }

    public void registerProvider(String key, AIProvider provider) {
        if (key == null || provider == null) {
            return;
        }
        String normalizedKey = key.toLowerCase();
        providers.put(normalizedKey, provider);

        if (currentProvider != null && currentProvider.getName().equalsIgnoreCase(normalizedKey)) {
            currentProvider = provider;
        }
    }

    private void selectProvider(String providerKey) {
        AIProvider selected = null;
        String normalizedRequestedKey = providerKey != null ? providerKey.toLowerCase() : null;

        if (normalizedRequestedKey != null) {
            AIProvider requested = providers.get(normalizedRequestedKey);
            if (requested == null) {
                System.err.println("[AI] Unknown provider: " + providerKey);
            } else if (isProviderAvailable(normalizedRequestedKey, requested)) {
                selected = requested;
            } else {
                System.err.println("[AI] Provider '" + providerKey + "' is unavailable. Attempting fallback.");
            }
        }

        if (selected == null) {
            for (String key : PROVIDER_PRIORITY) {
                if (normalizedRequestedKey != null && key.equals(normalizedRequestedKey)) {
                    continue;
                }
                AIProvider fallback = providers.get(key);
                if (fallback != null && isProviderAvailable(key, fallback)) {
                    selected = fallback;
                    System.out.println("[AI] Falling back to provider: " + fallback.getName());
                    break;
                }
            }
        }

        currentProvider = selected;

        if (currentProvider == null) {
            setStatus(CompanionStatus.OFFLINE, "No available providers");
            System.err.println("[AI] No available AI providers. Companion features are offline.");
        }
    }

    private void maybeSpawnCompanionOnStart() {
        if (aiSettings == null || npcManager == null) {
            return;
        }
        if (!aiSettings.spawnOnStart || companionNpcId >= 0) {
            return;
        }
        if (currentProvider == null) {
            return;
        }
        boolean spawned = spawnCompanion(new Vector3f(0f, 0f, 0f), aiSettings);
        if (!spawned) {
            System.err.println("[AI] Failed to auto-spawn companion despite spawnOnStart flag.");
        }
    }

    private boolean isProviderAvailable(String providerKey, AIProvider provider) {
        if (provider == null) {
            return false;
        }
        AIConfig config = buildConfig(provider.getName());
        boolean available = provider.isAvailable(config);
        if (!available) {
            String normalizedKey = providerKey != null ? providerKey : provider.getName();
            boolean missingCredentials = config.apiKey() == null || config.apiKey().isBlank();
            if (missingCredentials && requiresApiKey(normalizedKey)) {
                System.err.println("[AI] Provider '" + normalizedKey + "' unavailable: missing API key. Configure settings.ai.apiKeys.");
            } else {
                System.err.println("[AI] Provider '" + normalizedKey + "' reported as unavailable.");
            }
        }
        return available;
    }

    private boolean requiresApiKey(String providerKey) {
        if (providerKey == null) {
            return false;
        }
        return "gemini".equalsIgnoreCase(providerKey) || "openrouter".equalsIgnoreCase(providerKey);
    }

    public boolean spawnCompanion(Vector3f position, Settings.AISettings settings) {
        if (npcManager == null || aiSettings == null) {
            return false;
        }
        if (settings != null) {
            this.aiSettings = settings;
        }

        NPCEntity existing = companionNpcId >= 0 ? npcManager.getNPC(companionNpcId) : null;
        if (existing != null) {
            configureCompanion(existing, position);
            setStatus(CompanionStatus.IDLE, companionDisplayName() + " ready");
            enqueueNotification(companionDisplayName() + " is standing by.");
            return true;
        }

        float spawnX = position != null ? position.x : 0.0f;
        float spawnY = position != null ? position.y : 0.0f;
        float spawnZ = position != null ? position.z : 0.0f;

        int npcId = npcManager.spawnNPC(
            aiSettings.companionName,
            spawnX, spawnY, spawnZ,
            "companion",
            aiSettings.companionSkin
        );
        NPCEntity npc = npcManager.getNPC(npcId);
        if (npc == null) {
            return false;
        }
        companionNpcId = npcId;
        configureCompanion(npc, position);
        setStatus(CompanionStatus.IDLE, companionDisplayName() + " ready");
        enqueueNotification(companionDisplayName() + " has joined the world.");
        System.out.println("[AI] Spawned companion NPC with id " + companionNpcId);
        return true;
    }

    private void configureCompanion(NPCEntity npc, Vector3f position) {
        if (npc == null) {
            return;
        }
        if (position != null) {
            npc.teleport(position.x, position.y, position.z);
        }
        npc.setFollowDistance(aiSettings.followDistance);
    }

    public void handleChatMessage(String message, String senderName, Consumer<String> callback) {
        if (aiSettings == null || !aiSettings.aiEnabled || currentProvider == null) {
            setStatus(CompanionStatus.OFFLINE, currentProvider == null ? "No provider" : "Disabled");
            return;
        }

        if (senderName != null && aiSettings.companionName != null
            && senderName.equalsIgnoreCase(aiSettings.companionName)) {
            return;
        }

        if (!isMessageDirectedAtCompanion(message)) {
            return;
        }

        long cooldownMs = aiSettings.actionCooldownSeconds * 1000L;
        long now = System.currentTimeMillis();
        if (now - lastRequestTime < cooldownMs) {
            return;
        }

        String[] chatMessages = buildChatMessages(message, senderName);
        String systemPrompt = chatMessages[0];
        String userMessage = chatMessages[1];
        String prompt = composePrompt(systemPrompt, userMessage);
        AIConfig config = buildConfig(currentProvider.getName(), systemPrompt, userMessage);

        setStatus(CompanionStatus.WORKING, companionDisplayName() + " processing request");

        aiExecutor.submit(() -> {
            try {
                String responseJson = currentProvider.sendRequest(prompt, config);
                AIResponse response = currentProvider.parseResponse(responseJson);
                lastRequestTime = System.currentTimeMillis();

                if (aiSettings.logReasoning && response.reasoning() != null) {
                    System.out.println("[AI:Reasoning] " + response.reasoning());
                }

                String rawContent = response.content();
                String sanitizedContent = aiSettings.filterReasoning ? sanitizeContent(rawContent) : rawContent;

                List<CompanionAction> actions = parseActions(sanitizedContent);
                mainThreadPoster.accept(() -> executeActions(actions));

                if (callback != null) {
                    final String replyContent = sanitizedContent;
                    mainThreadPoster.accept(() -> dispatchReply(callback, replyContent));
                }
            } catch (IOException e) {
                System.err.println("[AI] Failed to send request: " + e.getMessage());
            } catch (JsonSyntaxException e) {
                System.err.println("[AI] Failed to parse response: " + e.getMessage());
            }
        });
    }

    private boolean isMessageDirectedAtCompanion(String message) {
        if (message == null) {
            return false;
        }
        String lowered = message.toLowerCase();
        String companionName = aiSettings.companionName != null ? aiSettings.companionName.toLowerCase() : "companion";
        return lowered.contains(companionName) || lowered.contains("@companion") || lowered.startsWith("companion");
    }

    private String composePrompt(String systemPrompt, String userMessage) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return userMessage;
        }
        return systemPrompt + "\n" + userMessage;
    }

    protected String[] buildChatMessages(String message, String senderName) {
        String systemPrompt = aiSettings != null && aiSettings.systemPrompt != null
            ? aiSettings.systemPrompt.trim()
            : "";
        String normalizedSender = senderName != null && !senderName.isBlank() ? senderName : "Player";
        String userMessage = "Player " + normalizedSender + " says: " + (message != null ? message : "");
        return new String[] {systemPrompt, userMessage};
    }

    private AIConfig buildConfig(String providerKey) {
        return buildConfig(providerKey, null, null);
    }

    private AIConfig buildConfig(String providerKey, String systemPrompt, String userMessage) {
        if (aiSettings == null) {
            return new AIConfig(null, null, null, buildMetadataHeaders(systemPrompt, userMessage));
        }
        Map<String, String> models = aiSettings.models != null ? aiSettings.models : Collections.emptyMap();
        Map<String, String> apiKeys = aiSettings.apiKeys != null ? aiSettings.apiKeys : Collections.emptyMap();
        String model = models.getOrDefault(providerKey, "");
        String apiKey = apiKeys.getOrDefault(providerKey, "");
        return new AIConfig(apiKey, model, null, buildMetadataHeaders(systemPrompt, userMessage));
    }

    private Map<String, String> buildMetadataHeaders(String systemPrompt, String userMessage) {
        boolean hasSystem = systemPrompt != null && !systemPrompt.isBlank();
        boolean hasUser = userMessage != null && !userMessage.isBlank();
        if (!hasSystem && !hasUser) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        if (hasSystem) {
            headers.put("__system_prompt", systemPrompt);
        }
        if (hasUser) {
            headers.put("__user_message", userMessage);
        }
        return headers;
    }

    private List<CompanionAction> parseActions(String content) {
        if (!aiSettings.enableActions || content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        String lower = content.toLowerCase();
        List<CompanionAction> actions = new ArrayList<>();

        if (FOLLOW_PATTERN.matcher(lower).find()) {
            actions.add(CompanionAction.follow());
        }
        if (STOP_PATTERN.matcher(lower).find()) {
            actions.add(CompanionAction.stop());
        }
        if (lower.contains("gather") || lower.contains("collect") || lower.contains("mine")) {
            String resource = detectResource(lower);
            int quantity = detectQuantity(lower);
            actions.add(CompanionAction.gather(resource, quantity));
        }

        return actions;
    }

    private String detectResource(String content) {
        String[] resourceKeywords = {"wood", "stone", "dirt", "leaves", "sand"};
        for (String keyword : resourceKeywords) {
            if (content.contains(keyword)) {
                return keyword;
            }
        }
        return "wood";
    }

    private int detectQuantity(String content) {
        String[] tokens = content.split("[^0-9]");
        for (String token : tokens) {
            if (!token.isBlank()) {
                try {
                    int value = Integer.parseInt(token);
                    if (value > 0) {
                        return Math.min(64, value);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 5;
    }

    private void executeActions(List<CompanionAction> actions) {
        if (npcManager == null || companionNpcId < 0) {
            return;
        }
        NPCEntity npc = npcManager.getNPC(companionNpcId);
        if (npc == null) {
            return;
        }

        if (actions.isEmpty()) {
            setStatus(CompanionStatus.IDLE, companionDisplayName() + " awaiting tasks");
            return;
        }

        for (CompanionAction action : actions) {
            switch (action.type()) {
                case GATHER -> {
                    npc.setCurrentTask(new NPCEntity.GatherTask(action.target(), action.quantity(), aiSettings.maxGatherDistance));
                    setStatus(CompanionStatus.WORKING, companionDisplayName() + " gathering " + action.target());
                    enqueueNotification(companionDisplayName() + " is gathering " + action.target() + "...");
                }
                case FOLLOW -> {
                    npc.setFollowDistance(aiSettings.followDistance);
                    setStatus(CompanionStatus.WORKING, companionDisplayName() + " following you");
                    enqueueNotification(companionDisplayName() + " is following you.");
                }
                case STOP -> {
                    npc.setFollowDistance(Float.MAX_VALUE);
                    setStatus(CompanionStatus.IDLE, companionDisplayName() + " standing by");
                    enqueueNotification(companionDisplayName() + " is standing by.");
                }
                default -> {
                }
            }
        }
    }

    public void update(float deltaTime) {
        // Currently no per-frame work required.
    }

    public void shutdown() {
        if (aiExecutor != null) {
            aiExecutor.shutdownNow();
            aiExecutor = null;
        }
        setStatus(CompanionStatus.OFFLINE, "AI shut down");
    }

    public AIProvider getCurrentProvider() {
        return currentProvider;
    }

    public Map<String, AIProvider> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    protected void setCurrentProvider(AIProvider provider) {
        this.currentProvider = provider;
    }

    public int getCompanionNpcId() {
        return companionNpcId;
    }

    public String getCompanionName() {
        return companionDisplayName();
    }

    protected void dispatchReply(Consumer<String> callback, String reply) {
        callback.accept(reply);
    }

    public CompanionStatusSnapshot getStatusSnapshot() {
        return new CompanionStatusSnapshot(companionStatus, companionStatusDetail, statusUpdatedAt);
    }

    public List<CompanionNotification> drainNotifications() {
        List<CompanionNotification> drained = new ArrayList<>();
        CompanionNotification notification;
        while ((notification = notificationQueue.poll()) != null) {
            drained.add(notification);
        }
        return drained;
    }

    public void testConnection(Settings.AISettings settings) {
        if (settings == null) {
            System.out.println("[AI] Cannot test connection: settings are null");
            return;
        }

        selectProvider(settings.aiProvider);
        if (currentProvider == null) {
            System.out.println("[AI] No provider selected for connection test.");
            return;
        }

        ensureExecutor();

        AIConfig config = new AIConfig(
            settings.apiKeys != null ? settings.apiKeys.getOrDefault(currentProvider.getName(), "") : "",
            settings.models != null ? settings.models.getOrDefault(currentProvider.getName(), "") : "",
            null,
            java.util.Collections.emptyMap()
        );

        aiExecutor.submit(() -> {
            try {
                currentProvider.sendRequest("ping", config);
                System.out.println("[AI] Provider " + currentProvider.getName() + " responded successfully.");
            } catch (IOException e) {
                System.err.println("[AI] Connection test failed: " + e.getMessage());
            }
        });
    }

    private void ensureExecutor() {
        if (aiExecutor == null) {
            aiExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "AICompanionWorker");
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    protected String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }

        String sanitized = content
            .replaceAll("(?is)<reasoning>.*?</reasoning>", "")
            .replaceAll("(?is)\\[reasoning\\].*?\\[/reasoning\\]", "");

        String[] lines = sanitized.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lowered = trimmed.toLowerCase();
            if (lowered.startsWith("reasoning:") || lowered.startsWith("thought:")
                || lowered.startsWith("analysis:")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }

        if (builder.length() == 0) {
            return sanitized.trim();
        }

        return builder.toString();
    }

    private void enqueueNotification(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        notificationQueue.offer(new CompanionNotification(message, System.currentTimeMillis()));
    }

    private void setStatus(CompanionStatus status, String detail) {
        this.companionStatus = status;
        this.companionStatusDetail = detail;
        this.statusUpdatedAt = System.currentTimeMillis();
    }

    private String companionDisplayName() {
        return aiSettings != null && aiSettings.companionName != null ? aiSettings.companionName : "Companion";
    }

    public enum CompanionStatus {
        OFFLINE,
        IDLE,
        WORKING
    }

    public record CompanionStatusSnapshot(CompanionStatus status, String detail, long updatedAt) {
    }

    public record CompanionNotification(String message, long timestamp) {
    }
}
