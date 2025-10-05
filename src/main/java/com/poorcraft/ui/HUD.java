package com.poorcraft.ui;

import com.poorcraft.core.Game;
import com.poorcraft.inventory.Inventory;
import com.poorcraft.inventory.ItemStack;
import com.poorcraft.render.PerformanceMonitor;
import com.poorcraft.render.Texture;
import com.poorcraft.world.block.BlockType;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

/**
 * In-game HUD overlay.
 * 
 * Displays crosshair, hotbar, and optional F3 debug information.
 * Always visible during gameplay (IN_GAME and PAUSED states).
 * 
 * The HUD is where you see all the important info. FPS, position, what you're looking at.
 * F3 debug overlay is a Minecraft tradition. Gotta have it.
 */
public class HUD extends UIScreen {
    
    private final Game game;  // Reference to game instance
    private boolean debugVisible;

    private static Texture hotbarFrameTexture;
    private static Texture hotbarSlotTexture;
    private static Texture hotbarSelectionTexture;
    private static Texture heartFullTexture;
    private static Texture heartEmptyTexture;
    private static Texture armorFullTexture;
    private static Texture armorEmptyTexture;
    private static Texture xpBarBackgroundTexture;
    private static Texture xpBarFillTexture;

    private float lastHotbarFrameY;
    private float lastHotbarScale;
    
    /**
     * Creates the HUD.
     *
     * @param windowWidth Window width
     * @param windowHeight Window height
     * @param game Game instance for accessing stats
     */
    public HUD(int windowWidth, int windowHeight, Object game) {
        super(windowWidth, windowHeight);
        this.game = (game instanceof Game) ? (Game) game : null;
        this.debugVisible = false;
    }

    @Override
    public void init() {
        ensureTexturesLoaded();
    }
    
    @Override
    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }
    
    @Override
    public void render(UIRenderer renderer, FontRenderer fontRenderer) {
        // Draw crosshair
        drawCrosshair(renderer);
        
        // Draw hotbar
        drawHotbar(renderer, fontRenderer);

        // Draw health/armor/xp bars
        drawPlayerStats(renderer, fontRenderer);
        
        // Draw debug info if visible
        if (debugVisible) {
            drawDebugInfo(renderer, fontRenderer);
        }
    }
    
    /**
     * Draws the crosshair at screen center.
     */
    private void drawCrosshair(UIRenderer renderer) {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        float size = 10;
        float thickness = 2;
        
        // Horizontal line
        renderer.drawRect(centerX - size, centerY - thickness / 2, 
            size * 2, thickness, 
            1.0f, 1.0f, 1.0f, 0.8f);
        
        // Vertical line
        renderer.drawRect(centerX - thickness / 2, centerY - size, 
            thickness, size * 2, 
            1.0f, 1.0f, 1.0f, 0.8f);
    }
    
    /**
     * Draws the hotbar at bottom center.
     */
    private void drawHotbar(UIRenderer renderer, FontRenderer fontRenderer) {
        if (game == null) {
            return;
        }

        Inventory inventory = game.getInventory();
        if (inventory == null) {
            return;
        }

        final int hotbarSlots = 16;
        float baseSlotSize = 48f;
        float baseSlotSpacing = 4f;
        float baseHotbarWidth = hotbarSlots * baseSlotSize + (hotbarSlots - 1) * baseSlotSpacing;
        float targetHotbarWidth = Math.min(windowWidth * 0.82f, baseHotbarWidth * 1.25f);
        float scale = Math.max(0.6f, Math.min(1.4f, targetHotbarWidth / baseHotbarWidth));

        float slotSize = baseSlotSize * scale;
        float slotSpacing = baseSlotSpacing * scale;
        float totalWidth = hotbarSlots * slotSize + (hotbarSlots - 1) * slotSpacing;
        float framePadding = 12f * scale;

        float frameWidth = totalWidth + framePadding * 2f;
        float frameHeight = (hotbarFrameTexture != null ? hotbarFrameTexture.getHeight() * scale : (slotSize + framePadding * 2f));
        float frameX = windowWidth / 2.0f - frameWidth / 2.0f;
        float frameY = windowHeight - frameHeight - 24f * scale;

        if (hotbarFrameTexture != null) {
            renderer.drawTexturedRect(frameX, frameY, frameWidth, frameHeight, hotbarFrameTexture.getId());
        } else {
            renderer.drawRect(frameX, frameY, frameWidth, frameHeight, 0.1f, 0.1f, 0.1f, 0.75f);
        }

        int selectedSlot = game.getSelectedHotbarSlot();
        float slotY = frameY + frameHeight - framePadding - slotSize;

        float selectionSize = (hotbarSelectionTexture != null ? hotbarSelectionTexture.getWidth() * scale : slotSize + 8f * scale);
        float selectionOffset = (selectionSize - slotSize) / 2f;

        for (int i = 0; i < hotbarSlots; i++) {
            float slotX = frameX + framePadding + i * (slotSize + slotSpacing);

            if (i == selectedSlot && hotbarSelectionTexture != null) {
                renderer.drawTexturedRect(slotX - selectionOffset, slotY - selectionOffset,
                    selectionSize, selectionSize, hotbarSelectionTexture.getId());
            }

            if (hotbarSlotTexture != null) {
                renderer.drawTexturedRect(slotX, slotY, slotSize, slotSize, hotbarSlotTexture.getId());
            } else {
                renderer.drawRect(slotX, slotY, slotSize, slotSize, 0.2f, 0.2f, 0.25f, 0.9f);
            }

            ItemStack stack = inventory.getSlot(i);
            if (stack != null && !stack.isEmpty()) {
                BlockType blockType = stack.getBlockType();
                String label = formatBlockLabel(blockType);
                if (!label.isEmpty()) {
                    float labelScale = 0.5f * scale;
                    float labelWidth = fontRenderer.getTextWidth(label) * labelScale;
                    float labelX = slotX + (slotSize - labelWidth) / 2f;
                    float labelY = slotY + slotSize - 14f * scale;
                    fontRenderer.drawText(label, labelX, labelY, labelScale, 0.95f, 0.95f, 0.95f, 1.0f);
                }

                int count = stack.getCount();
                if (count > 0) {
                    String countText = formatCount(count);
                    float countScale = 0.45f * scale;
                    float countWidth = fontRenderer.getTextWidth(countText) * countScale;
                    float countX = slotX + slotSize - countWidth - 6f * scale;
                    float countY = slotY + slotSize - 6f * scale;
                    fontRenderer.drawText(countText, countX, countY, countScale, 1f, 1f, 1f, 1f);
                }
            }
        }

        lastHotbarFrameY = frameY;
        lastHotbarScale = scale;
    }

    private void drawPlayerStats(UIRenderer renderer, FontRenderer fontRenderer) {
        float scale = lastHotbarScale > 0 ? lastHotbarScale : Math.max(0.7f, Math.min(1.2f, windowWidth / 1920f));

        // Placeholder values until player stats are wired in
        int maxHearts = 10;
        int filledHearts = maxHearts;
        int maxArmorIcons = 10;
        int filledArmor = 0;
        float xpProgress = 0.0f;
        int xpLevel = 0;

        float heartWidth = (heartFullTexture != null ? heartFullTexture.getWidth() : 20f) * scale;
        float heartHeight = (heartFullTexture != null ? heartFullTexture.getHeight() : 20f) * scale;
        float heartSpacing = 4f * scale;
        float heartsWidth = maxHearts * heartWidth + (maxHearts - 1) * heartSpacing;
        float heartsX = windowWidth / 2f - heartsWidth / 2f;
        float heartsY = lastHotbarFrameY - heartHeight - 14f * scale;

        for (int i = 0; i < maxHearts; i++) {
            Texture texture = (i < filledHearts) ? heartFullTexture : heartEmptyTexture;
            float x = heartsX + i * (heartWidth + heartSpacing);
            if (texture != null) {
                renderer.drawTexturedRect(x, heartsY, heartWidth, heartHeight, texture.getId());
            } else {
                renderer.drawRect(x, heartsY, heartWidth, heartHeight, 0.8f, 0.2f, 0.2f, i < filledHearts ? 0.9f : 0.25f);
            }
        }

        float armorWidth = (armorFullTexture != null ? armorFullTexture.getWidth() : 20f) * scale;
        float armorHeight = (armorFullTexture != null ? armorFullTexture.getHeight() : 20f) * scale;
        float armorSpacing = 4f * scale;
        float armorTotalWidth = maxArmorIcons * armorWidth + (maxArmorIcons - 1) * armorSpacing;
        float armorX = windowWidth / 2f - armorTotalWidth / 2f;
        float armorY = heartsY - armorHeight - 6f * scale;

        for (int i = 0; i < maxArmorIcons; i++) {
            Texture texture = (i < filledArmor) ? armorFullTexture : armorEmptyTexture;
            float x = armorX + i * (armorWidth + armorSpacing);
            if (texture != null) {
                renderer.drawTexturedRect(x, armorY, armorWidth, armorHeight, texture.getId());
            } else {
                renderer.drawRect(x, armorY, armorWidth, armorHeight, 0.5f, 0.5f, 0.6f, i < filledArmor ? 0.9f : 0.3f);
            }
        }

        if (xpBarBackgroundTexture != null) {
            float xpWidth = xpBarBackgroundTexture.getWidth() * scale;
            float xpHeight = xpBarBackgroundTexture.getHeight() * scale;
            float xpX = windowWidth / 2f - xpWidth / 2f;
            float xpY = armorY - xpHeight - 10f * scale;

            renderer.drawTexturedRect(xpX, xpY, xpWidth, xpHeight, xpBarBackgroundTexture.getId());

            if (xpProgress > 0f && xpBarFillTexture != null) {
                float fillWidth = Math.max(4f * scale, xpWidth * Math.min(1f, xpProgress));
                renderer.drawTexturedRect(xpX + 2f * scale, xpY + 2f * scale, fillWidth - 4f * scale,
                    xpHeight - 4f * scale, xpBarFillTexture.getId());
            }

            if (xpLevel > 0) {
                float textWidth = fontRenderer.getTextWidth(Integer.toString(xpLevel)) * 0.7f;
                float textX = windowWidth / 2f - textWidth / 2f;
                float textY = xpY + xpHeight - fontRenderer.getTextHeight() * 0.6f;
                fontRenderer.drawText(Integer.toString(xpLevel), textX, textY, 0.7f, 0.9f, 0.9f, 0.4f, 1f);
            }
        }
    }

    private void ensureTexturesLoaded() {
        if (hotbarFrameTexture != null) {
            return;
        }

        hotbarFrameTexture = loadTextureSafe("/textures/ui/hotbar_frame.png");
        hotbarSlotTexture = loadTextureSafe("/textures/ui/hotbar_slot.png");
        hotbarSelectionTexture = loadTextureSafe("/textures/ui/hotbar_selection.png");
        heartFullTexture = loadTextureSafe("/textures/ui/heart_full.png");
        heartEmptyTexture = loadTextureSafe("/textures/ui/heart_empty.png");
        armorFullTexture = loadTextureSafe("/textures/ui/armor_full.png");
        armorEmptyTexture = loadTextureSafe("/textures/ui/armor_empty.png");
        xpBarBackgroundTexture = loadTextureSafe("/textures/ui/xp_bar_background.png");
        xpBarFillTexture = loadTextureSafe("/textures/ui/xp_bar_fill.png");
    }

    private Texture loadTextureSafe(String path) {
        try {
            Texture texture = Texture.loadFromResource(path);
            Objects.requireNonNull(texture);
            return texture;
        } catch (Exception ex) {
            System.err.println("[HUD] Failed to load UI texture " + path + ": " + ex.getMessage());
            return null;
        }
    }

    private String formatBlockLabel(BlockType blockType) {
        if (blockType == null || blockType == BlockType.AIR) {
            return "";
        }
        String name = blockType.name().replace('_', ' ').toLowerCase();
        if (name.length() <= 10) {
            return capitalizeWords(name);
        }
        String[] parts = name.split(" ");
        StringBuilder acronym = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                acronym.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return acronym.toString();
    }

    private String capitalizeWords(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                builder.append(c);
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            return (count / 1000) + "k";
        }
        return Integer.toString(count);
    }
    
    /**
     * Draws F3 debug information.
     */
    private void drawDebugInfo(UIRenderer renderer, FontRenderer fontRenderer) {
        List<String> lines = new ArrayList<>();

        float textScale = 1.0f;
        float lineHeight = fontRenderer.getTextHeight() * textScale + 5f;

        if (game != null) {
            PerformanceMonitor pm = game.getPerformanceMonitor();
            if (pm != null) {
                PerformanceMonitor.FrameStats stats = pm.getStats();
                float onePercentLowMs = stats.frameTime1PercentLowMs();
                float onePercentLowFps = onePercentLowMs > 0f ? 1000f / onePercentLowMs : 0f;

                lines.add(String.format(Locale.US, "FPS: %.0f (1%% low %.0f)", stats.fps(), onePercentLowFps));
                lines.add(String.format(Locale.US, "Frame Time: %.2f ms (min %.2f / max %.2f)",
                    stats.frameTimeMs(), stats.frameTimeMinMs(), stats.frameTimeMaxMs()));
                lines.add(String.format(Locale.US, "Chunks: %s rendered, %s culled, %s total",
                    formatNumber(stats.chunksRendered()),
                    formatNumber(stats.chunksCulled()),
                    formatNumber(stats.chunksTotal())));
                lines.add(String.format(Locale.US, "Draw Calls: %s, Vertices: %s",
                    formatNumber(stats.drawCalls()),
                    formatNumber(stats.verticesRendered())));

                float uploadKB = stats.bufferUploadBytes() / 1024f;
                lines.add(String.format(Locale.US, "Buffer Uploads: %s (%.1f KB)",
                    formatNumber(stats.bufferUploads()), uploadKB));

                lines.add(String.format(Locale.US, "Chunk Memory: %s / %s (compressed %s)",
                    formatBytes(stats.chunkMemoryUsageBytes()),
                    formatBytes(stats.chunkMemoryBudgetBytes()),
                    formatNumber(stats.chunksCompressed())));

                List<PerformanceMonitor.Zone> zones = stats.zones();
                if (!zones.isEmpty()) {
                    lines.add("Top Zones:");
                    int zoneCount = Math.min(3, zones.size());
                    for (int i = 0; i < zoneCount; i++) {
                        PerformanceMonitor.Zone zone = zones.get(i);
                        lines.add(String.format(Locale.US, "  %s: %.2f ms", zone.name(), zone.averageMs()));
                    }
                }
            } else {
                lines.add("Performance stats unavailable");
            }

            var camera = game.getCamera();
            if (camera != null) {
                Vector3f position = camera.getPosition();
                lines.add(String.format(Locale.US, "Position: %.2f, %.2f, %.2f", position.x, position.y, position.z));

                int chunkX = (int) Math.floor(position.x / 16f);
                int chunkZ = (int) Math.floor(position.z / 16f);
                lines.add(String.format(Locale.US, "Chunk: %d, %d", chunkX, chunkZ));

                Vector3f front = camera.getFront();
                lines.add("Facing: " + describeFacing(front));
            }
        } else {
            lines.add("Debug info unavailable");
        }

        lines.add("F3: Toggle debug");

        float padding = 10f;
        float maxLineWidth = 0f;
        for (String line : lines) {
            float width = fontRenderer.getTextWidth(line) * textScale;
            if (width > maxLineWidth) {
                maxLineWidth = width;
            }
        }

        float bgWidth = maxLineWidth + padding * 2f;
        float bgHeight = lines.size() * lineHeight + padding * 2f;
        float bgX = 10f;
        float bgY = 10f;

        renderer.drawRect(bgX, bgY, bgWidth, bgHeight, 0.0f, 0.0f, 0.0f, 0.6f);

        float textX = bgX + padding;
        float textY = bgY + padding;
        for (String line : lines) {
            fontRenderer.drawText(line, textX, textY, textScale, 1.0f, 1.0f, 1.0f, 1.0f);
            textY += lineHeight;
        }
    }

    private String describeFacing(Vector3f direction) {
        if (direction == null) {
            return "N/A";
        }
        float absX = Math.abs(direction.x);
        float absZ = Math.abs(direction.z);
        if (absX < 1e-3f && absZ < 1e-3f) {
            return "N/A";
        }
        if (absX > absZ) {
            return direction.x > 0 ? "East" : "West";
        } else if (absZ > absX) {
            return direction.z > 0 ? "South" : "North";
        } else {
            String horizontal = direction.x > 0 ? "East" : "West";
            String vertical = direction.z > 0 ? "South" : "North";
            return horizontal + "-" + vertical;
        }
    }

    private String formatNumber(long value) {
        long abs = Math.abs(value);
        if (abs >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fB", value / 1_000_000_000f);
        }
        if (abs >= 1_000_000L) {
            return String.format(Locale.US, "%.1fM", value / 1_000_000f);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.US, "%.1fk", value / 1_000f);
        }
        return Long.toString(value);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return String.format(Locale.US, "%.0f %s", value, units[unitIndex]);
        }
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex]);
    }
    
    /**
     * Toggles debug info visibility.
     */
    public void toggleDebug() {
        debugVisible = !debugVisible;
        System.out.println("[HUD] Debug info " + (debugVisible ? "enabled" : "disabled"));
    }
    
    /**
     * Gets debug visibility state.
     * 
     * @return True if debug info is visible
     */
    public boolean isDebugVisible() {
        return debugVisible;
    }
}
