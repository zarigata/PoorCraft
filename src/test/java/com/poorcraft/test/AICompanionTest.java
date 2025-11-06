package com.poorcraft.test;

import com.poorcraft.ai.AICompanionManager;
import com.poorcraft.ai.AIProvider;
import com.poorcraft.ai.providers.GeminiProvider;
import com.poorcraft.ai.providers.OpenRouterProvider;
import com.poorcraft.config.Settings;
import com.poorcraft.world.entity.NPCEntity;
import com.poorcraft.world.entity.NPCManager;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class AICompanionTest {

    private Settings.AISettings settings;
    private CapturingNPCManager npcManager;
    private AICompanionManager manager;

    @BeforeEach
    public void setUp() {
        settings = new Settings.AISettings();
        settings.aiEnabled = true;
        settings.aiProvider = "ollama";
        settings.companionName = "Companion";
        settings.companionSkin = "steve";
        settings.spawnOnStart = true;
        settings.followDistance = 4f;
        settings.enableActions = true;
        settings.actionCooldownSeconds = 0;
        settings.maxGatherDistance = 16;
        settings.filterReasoning = true;
        settings.logReasoning = false;
        settings.systemPrompt = "System";
        settings.apiKeys = new HashMap<>();
        settings.models = new HashMap<>();

        npcManager = new CapturingNPCManager();
        manager = new RecordingAICompanionManager();
        manager.init(settings, npcManager);
    }

    @Test
    public void providerRegistrationMatchesSettings() {
        Map<String, AIProvider> providers = manager.getProviders();
        assertEquals(3, providers.size());
        assertTrue(providers.containsKey("ollama"));
        assertTrue(providers.containsKey("gemini"));
        assertTrue(providers.containsKey("openrouter"));
    }

    @Test
    public void spawnCompanionStoresNpcId() {
        boolean spawned = manager.spawnCompanion(new Vector3f(1, 2, 3), settings);
        assertTrue(spawned);
        assertEquals(1, npcManager.spawnedNPCs.size());
        CapturingNPCManager.SpawnRecord record = npcManager.spawnedNPCs.get(0);
        assertEquals("Companion", record.name());
        assertEquals("steve", record.skin());
        assertEquals("companion", record.personality());
    }

    @Test
    public void handleChatMessageHonorsCooldownAndFiltersReasoning() throws Exception {
        RecordingProvider provider = new RecordingProvider("Reply", "Hidden");
        ((RecordingAICompanionManager) manager).injectProvider("ollama", provider);

        AtomicReference<String> reply = new AtomicReference<>();
        settings.actionCooldownSeconds = 100;

        manager.handleChatMessage("Hey Companion gather", "Player", reply::set);
        provider.awaitInvocation();
        assertEquals("Reply", reply.get());

        reply.set(null);
        manager.handleChatMessage("Hey Companion gather", "Player", reply::set);
        assertNull(reply.get(), "Cooldown should prevent second reply");
    }

    @Test
    public void parseActionsTriggersGatherTask() throws Exception {
        RecordingProvider provider = new RecordingProvider("Gather stone please", null);
        ((RecordingAICompanionManager) manager).injectProvider("ollama", provider);

        manager.handleChatMessage("Companion gather stone", "Player", msg -> {});
        provider.awaitInvocation();

        assertEquals("stone", npcManager.lastGatherResource);
        assertTrue(npcManager.lastGatherQuantity > 0);
    }

    @Test
    public void chatCallbackDispatchesReply() throws Exception {
        RecordingProvider provider = new RecordingProvider("Follow", null);
        ((RecordingAICompanionManager) manager).injectProvider("ollama", provider);

        AtomicBoolean invoked = new AtomicBoolean(false);
        manager.handleChatMessage("Companion follow", "Player", msg -> invoked.set(true));
        provider.awaitInvocation();

        assertTrue(((RecordingAICompanionManager) manager).wasReplyDispatched());
        assertTrue(invoked.get());
    }

    @Test
    public void openRouterParseResponseExtractsContentAndReasoning() {
        String payload = "{" +
            "\"choices\":[{" +
            "\"message\":{\"content\":\"Hello adventurer!\",\"reasoning\":\"Hidden thoughts\"}," +
            "\"finish_reason\":\"stop\"}]," +
            "\"usage\":{\"total_tokens\":42}}";

        OpenRouterProvider provider = new OpenRouterProvider();
        AIProvider.AIResponse response = provider.parseResponse(payload);

        assertEquals("Hello adventurer!", response.content());
        assertEquals("Hidden thoughts", response.reasoning());
        assertTrue(response.metadata().containsKey("usage"));
        assertEquals("stop", response.metadata().get("finish_reason"));
    }

    @Test
    public void geminiParseResponseExtractsFirstPart() {
        String payload = "{" +
            "\"candidates\":[{" +
            "\"content\":{\"parts\":[{\"text\":\"Gemini says hi\"}]}}]}";

        GeminiProvider provider = new GeminiProvider();
        AIProvider.AIResponse response = provider.parseResponse(payload);

        assertEquals("Gemini says hi", response.content());
        assertNull(response.reasoning());
    }

    @Test
    public void sanitizeContentRemovesReasoningBlocks() {
        RecordingAICompanionManager recordingManager = (RecordingAICompanionManager) manager;
        String input = "<reasoning>internal</reasoning>\nPlayer reply\nReasoning: nope";
        String sanitized = recordingManager.exposeSanitize(input);

        assertEquals("Player reply", sanitized);
    }

    private static final class CapturingNPCManager extends NPCManager {
        private final List<SpawnRecord> spawnedNPCs = new ArrayList<>();
        private String lastGatherResource;
        private int lastGatherQuantity;

        @Override
        public int spawnNPC(String name, float x, float y, float z, String personality, String skinName) {
            spawnedNPCs.add(new SpawnRecord(name, personality, skinName, new Vector3f(x, y, z)));
            return spawnedNPCs.size();
        }

        @Override
        public NPCEntity getNPC(int npcId) {
            return new RecordingNPC(this);
        }

        record SpawnRecord(String name, String personality, String skin, Vector3f position) {}
    }

    private static final class RecordingNPC extends NPCEntity {
        private final CapturingNPCManager owner;

        RecordingNPC(CapturingNPCManager owner) {
            super(1, "Companion", 0, 0, 0, "companion", "steve");
            this.owner = owner;
        }

        @Override
        public void setCurrentTask(NPCTask task) {
            super.setCurrentTask(task);
            if (task instanceof GatherTask gather) {
                owner.lastGatherResource = gather.getResourceType();
                owner.lastGatherQuantity = gather.getTargetQuantity();
            }
        }
    }

    private static final class RecordingAICompanionManager extends AICompanionManager {
        private volatile boolean replyDispatched;

        void injectProvider(String key, AIProvider provider) {
            registerProvider(key, provider);
        }

        boolean wasReplyDispatched() {
            return replyDispatched;
        }

        String exposeSanitize(String content) {
            return sanitizeContent(content);
        }

        @Override
        protected void dispatchReply(Consumer<String> callback, String reply) {
            replyDispatched = true;
            callback.accept(reply);
        }
    }

    private static final class RecordingProvider implements AIProvider {
        private final String content;
        private final String reasoning;
        private final AtomicBoolean invoked = new AtomicBoolean(false);

        RecordingProvider(String content, String reasoning) {
            this.content = content;
            this.reasoning = reasoning;
        }

        @Override
        public String sendRequest(String prompt, AIConfig config) {
            invoked.set(true);
            return "{}";
        }

        @Override
        public String getName() {
            return "ollama";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public AIResponse parseResponse(String jsonResponse) {
            return new AIResponse(content, reasoning, Map.of());
        }

        void awaitInvocation() throws InterruptedException {
            for (int i = 0; i < 10 && !invoked.get(); i++) {
                Thread.sleep(10);
            }
            assertTrue(invoked.get(), "Provider should have been invoked");
        }
    }
}
