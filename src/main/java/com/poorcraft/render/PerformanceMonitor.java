package com.poorcraft.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks rendering and world performance metrics to power the debug overlay and adaptive systems.
 */
public class PerformanceMonitor {

    private static final int FRAME_HISTORY = 120;
    private static final int FRAME_AVERAGE_COUNT = 60;

    private final Deque<Float> frameTimes = new ArrayDeque<>();
    private final Deque<Float> frameTimeHistory = new ArrayDeque<>();

    private float smoothedDelta;
    private float accumulatedTime;
    private float fps;
    private float frameTimeMs;
    private float frameTimeMsMin = Float.MAX_VALUE;
    private float frameTimeMsMax = 0f;
    private float frameTime1PercentLow = 0f;

    private final AtomicInteger chunksRendered = new AtomicInteger();
    private final AtomicInteger chunksCulled = new AtomicInteger();
    private final AtomicInteger chunksTotal = new AtomicInteger();

    private final AtomicLong verticesRendered = new AtomicLong();
    private final AtomicLong drawCalls = new AtomicLong();
    private final AtomicLong bufferUploads = new AtomicLong();
    private final AtomicLong bufferUploadBytes = new AtomicLong();
    private final AtomicLong textureBinds = new AtomicLong();
    private final AtomicLong stateChanges = new AtomicLong();

    private final AtomicInteger chunksLoaded = new AtomicInteger();
    private final AtomicInteger chunksCompressed = new AtomicInteger();
    private final AtomicInteger pendingChunkLoads = new AtomicInteger();
    private final AtomicLong chunkMemoryUsage = new AtomicLong();
    private final AtomicLong chunkMemoryBudget = new AtomicLong(512L * 1024L * 1024L);
    private final AtomicInteger chunkLoadQueueSize = new AtomicInteger();
    private final AtomicInteger chunkLoadCancellations = new AtomicInteger();
    private final AtomicInteger chunkLoadBudgetLast = new AtomicInteger();
    private final AtomicInteger chunkLoadCandidateCount = new AtomicInteger();
    private volatile float chunkLoadAveragePriority;

    private final Map<String, Zone> zones = new ConcurrentHashMap<>();
    private final Deque<ZoneInstance> zoneStack = new ArrayDeque<>();

    public PerformanceMonitor() {
        this.smoothedDelta = 0f;
        this.accumulatedTime = 0f;
    }

    public void update(float deltaTime) {
        if (deltaTime <= 0f) {
            return;
        }

        smoothedDelta = smoothedDelta == 0f ? deltaTime : (smoothedDelta * 0.9f + deltaTime * 0.1f);

        float deltaMs = deltaTime * 1000f;
        frameTimes.addLast(deltaMs);
        frameTimeHistory.addLast(deltaMs);
        accumulatedTime += deltaTime;

        if (frameTimes.size() > FRAME_AVERAGE_COUNT) {
            frameTimes.removeFirst();
        }
        if (frameTimeHistory.size() > FRAME_HISTORY) {
            frameTimeHistory.removeFirst();
        }

        float sum = 0f;
        for (float time : frameTimes) {
            sum += time;
        }
        float averageDeltaMs = sum / frameTimes.size();
        frameTimeMs = averageDeltaMs;
        fps = averageDeltaMs > 0f ? 1000f / averageDeltaMs : 0f;

        frameTimeMsMin = Math.min(frameTimeMsMin, deltaMs);
        frameTimeMsMax = Math.max(frameTimeMsMax, deltaMs);

        updateOnePercentLow();

        if (accumulatedTime >= 1f) {
            resetPerSecondCounters();
        }
    }

    private void updateOnePercentLow() {
        if (frameTimeHistory.isEmpty()) {
            frameTime1PercentLow = 0f;
            return;
        }
        List<Float> sorted = new ArrayList<>(frameTimeHistory);
        Collections.sort(sorted);
        int count = sorted.size();
        int index = Math.max(0, (int) (count * 0.99f) - 1);
        frameTime1PercentLow = sorted.get(index);
    }

    private void resetPerSecondCounters() {
        accumulatedTime = 0f;
        bufferUploads.set(0);
        bufferUploadBytes.set(0);
        textureBinds.set(0);
        stateChanges.set(0);
        chunksRendered.set(0);
        chunksCulled.set(0);
        verticesRendered.set(0);
        drawCalls.set(0);
    }

    public void recordRenderStats(int rendered, int culled, int total, long vertices, long draws) {
        chunksRendered.addAndGet(rendered);
        chunksCulled.addAndGet(culled);
        chunksTotal.set(total);
        verticesRendered.addAndGet(vertices);
        drawCalls.addAndGet(draws);
    }

    public void recordBufferUpload(long bytes) {
        bufferUploads.incrementAndGet();
        bufferUploadBytes.addAndGet(bytes);
    }

    public void recordTextureBind() {
        textureBinds.incrementAndGet();
    }

    public void recordStateChange() {
        stateChanges.incrementAndGet();
    }

    public void setChunkMemoryUsage(long usedBytes) {
        chunkMemoryUsage.set(usedBytes);
    }

    public void setChunkMemoryBudget(long budgetBytes) {
        chunkMemoryBudget.set(budgetBytes);
    }

    public void setChunksLoaded(int count) {
        chunksLoaded.set(count);
    }

    public void setChunksCompressed(int count) {
        chunksCompressed.set(count);
    }

    public void setPendingChunkLoads(int count) {
        pendingChunkLoads.set(count);
    }

    public void setChunkLoadQueueSize(int count) {
        chunkLoadQueueSize.set(count);
    }

    public void setChunkLoadCancellations(int count) {
        chunkLoadCancellations.set(count);
    }

    public void setChunkLoadBudgetLast(int budget) {
        chunkLoadBudgetLast.set(budget);
    }

    public void setChunkLoadAveragePriority(float priority) {
        chunkLoadAveragePriority = priority;
    }

    public void setChunkLoadCandidateCount(int count) {
        chunkLoadCandidateCount.set(count);
    }

    public void beginZone(String name) {
        Zone zone = zones.computeIfAbsent(name, Zone::new);
        ZoneInstance instance = new ZoneInstance(zone, System.nanoTime());
        zoneStack.push(instance);
    }

    public void endZone() {
        if (zoneStack.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        ZoneInstance instance = zoneStack.pop();
        long duration = now - instance.startTime;
        instance.zone.addSample(duration);
    }

    public float getFPS() {
        return fps;
    }

    public float getFrameTimeMs() {
        return frameTimeMs;
    }

    public float getFrameTimeMsMin() {
        return frameTimeMsMin == Float.MAX_VALUE ? 0f : frameTimeMsMin;
    }

    public float getFrameTimeMsMax() {
        return frameTimeMsMax;
    }

    public float getFrameTime1PercentLow() {
        return frameTime1PercentLow;
    }

    public long getChunksRendered() {
        return chunksRendered.get();
    }

    public long getChunksCulled() {
        return chunksCulled.get();
    }

    public int getChunksTotal() {
        return chunksTotal.get();
    }

    public long getVerticesRendered() {
        return verticesRendered.get();
    }

    public long getDrawCalls() {
        return drawCalls.get();
    }

    public long getBufferUploads() {
        return bufferUploads.get();
    }

    public long getBufferUploadBytes() {
        return bufferUploadBytes.get();
    }

    public long getTextureBinds() {
        return textureBinds.get();
    }

    public long getStateChanges() {
        return stateChanges.get();
    }

    public int getChunksLoaded() {
        return chunksLoaded.get();
    }

    public int getChunksCompressed() {
        return chunksCompressed.get();
    }

    public int getPendingChunkLoads() {
        return pendingChunkLoads.get();
    }

    public long getChunkMemoryUsage() {
        return chunkMemoryUsage.get();
    }

    public long getChunkMemoryBudget() {
        return chunkMemoryBudget.get();
    }

    public float getChunkLoadAveragePriority() {
        return chunkLoadAveragePriority;
    }

    public int getChunkLoadQueueSize() {
        return chunkLoadQueueSize.get();
    }

    public int getChunkLoadCancellations() {
        return chunkLoadCancellations.get();
    }

    public int getChunkLoadBudgetLast() {
        return chunkLoadBudgetLast.get();
    }

    public int getChunkLoadCandidateCount() {
        return chunkLoadCandidateCount.get();
    }

    public List<Zone> getZonesSorted() {
        List<Zone> list = new ArrayList<>(zones.values());
        list.sort((a, b) -> Float.compare(b.averageMs, a.averageMs));
        return list;
    }

    public float getSmoothedDelta() {
        return smoothedDelta;
    }

    public FrameStats getStats() {
        return new FrameStats();
    }

    public void shutdown() {
        zoneStack.clear();
        zones.clear();
    }

    public class FrameStats {
        public float fps() {
            return getFPS();
        }

        public float frameTimeMs() {
            return getFrameTimeMs();
        }

        public float frameTimeMinMs() {
            return getFrameTimeMsMin();
        }

        public float frameTimeMaxMs() {
            return getFrameTimeMsMax();
        }

        public float frameTime1PercentLowMs() {
            return getFrameTime1PercentLow();
        }

        public long chunksRendered() {
            return PerformanceMonitor.this.getChunksRendered();
        }

        public long chunksCulled() {
            return PerformanceMonitor.this.getChunksCulled();
        }

        public int chunksTotal() {
            return PerformanceMonitor.this.getChunksTotal();
        }

        public long verticesRendered() {
            return PerformanceMonitor.this.getVerticesRendered();
        }

        public long drawCalls() {
            return PerformanceMonitor.this.getDrawCalls();
        }

        public long bufferUploads() {
            return PerformanceMonitor.this.getBufferUploads();
        }

        public long bufferUploadBytes() {
            return PerformanceMonitor.this.getBufferUploadBytes();
        }

        public long textureBinds() {
            return PerformanceMonitor.this.getTextureBinds();
        }

        public long stateChanges() {
            return PerformanceMonitor.this.getStateChanges();
        }

        public int chunksLoaded() {
            return PerformanceMonitor.this.getChunksLoaded();
        }

        public int chunksCompressed() {
            return PerformanceMonitor.this.getChunksCompressed();
        }

        public int pendingChunkLoads() {
            return PerformanceMonitor.this.getPendingChunkLoads();
        }

        public long chunkMemoryUsageBytes() {
            return PerformanceMonitor.this.getChunkMemoryUsage();
        }

        public long chunkMemoryBudgetBytes() {
            return PerformanceMonitor.this.getChunkMemoryBudget();
        }

        public float chunkLoadAveragePriority() {
            return PerformanceMonitor.this.getChunkLoadAveragePriority();
        }

        public int chunkLoadQueueSize() {
            return PerformanceMonitor.this.getChunkLoadQueueSize();
        }

        public int chunkLoadCancellations() {
            return PerformanceMonitor.this.getChunkLoadCancellations();
        }

        public int chunkLoadBudgetLast() {
            return PerformanceMonitor.this.getChunkLoadBudgetLast();
        }

        public int chunkLoadCandidateCount() {
            return PerformanceMonitor.this.getChunkLoadCandidateCount();
        }

        public List<Zone> zones() {
            return PerformanceMonitor.this.getZonesSorted();
        }
    }

    public static class Zone {
        private final String name;
        private float averageMs;
        private float peakMs;

        private static final float SMOOTHING = 0.9f;

        Zone(String name) {
            this.name = Objects.requireNonNull(name);
        }

        void addSample(long durationNs) {
            float milliseconds = durationNs / 1_000_000f;
            averageMs = averageMs == 0f ? milliseconds : averageMs * SMOOTHING + milliseconds * (1f - SMOOTHING);
            peakMs = Math.max(peakMs, milliseconds);
        }

        public String name() {
            return name;
        }

        public float averageMs() {
            return averageMs;
        }

        public float peakMs() {
            return peakMs;
        }
    }

    private static class ZoneInstance {
        private final Zone zone;
        private final long startTime;

        ZoneInstance(Zone zone, long startTime) {
            this.zone = zone;
            this.startTime = startTime;
        }
    }
}
