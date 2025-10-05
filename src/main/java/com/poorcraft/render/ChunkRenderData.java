package com.poorcraft.render;

import com.poorcraft.world.chunk.ChunkMesh;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30C.glFlushMappedBufferRange;
import static org.lwjgl.opengl.GL30C.glMapBufferRange;
import static org.lwjgl.opengl.GL30C.glUnmapBuffer;
import static org.lwjgl.opengl.GL32C.GL_ALREADY_SIGNALED;
import static org.lwjgl.opengl.GL32C.GL_CONDITION_SATISFIED;
import static org.lwjgl.opengl.GL32C.GL_SYNC_FLUSH_COMMANDS_BIT;
import static org.lwjgl.opengl.GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.GL32C.GL_TIMEOUT_EXPIRED;
import static org.lwjgl.opengl.GL32C.glClientWaitSync;
import static org.lwjgl.opengl.GL32C.glDeleteSync;
import static org.lwjgl.opengl.GL32C.glFenceSync;
import static org.lwjgl.opengl.GL44C.GL_MAP_COHERENT_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL44C.glBufferStorage;

/**
 * Manages GPU resources for a single chunk mesh with vendor-aware optimisations.
 */
public class ChunkRenderData {

    private static final int STREAM_SEGMENTS = 3;
    private static final long MIN_VERTEX_SEGMENT = 8 * 1024L;
    private static final long MIN_INDEX_SEGMENT = 4 * 1024L;
    private static final int VERTEX_STRIDE = 8 * Float.BYTES;

    private final GPUCapabilities capabilities;
    private final PerformanceMonitor monitor;

    private int vao;
    private int vbo;
    private int ebo;
    private int indexCount;
    private boolean initialized;
    private int uploadedMeshVersion;
    private int usageHint = GL_STATIC_DRAW;

    private final long[] fences = new long[STREAM_SEGMENTS];
    private int currentSegment = -1;
    private boolean persistentEnabled;

    private long vertexSegmentSize;
    private long indexSegmentSize;
    private long vertexTotalSize;
    private long indexTotalSize;
    private ByteBuffer persistentVertex;
    private ByteBuffer persistentIndex;
    private long vertexStreamOffset;
    private long indexStreamOffset;
    private long indexDrawOffset;

    private long lastUploadTimeNs;
    private long totalUploadTimeNs;
    private int uploadCount;

    public ChunkRenderData(GPUCapabilities capabilities, PerformanceMonitor monitor) {
        this.capabilities = capabilities;
        this.monitor = monitor;
        this.uploadedMeshVersion = -1;
    }

    public void setUsageHint(int usage) {
        this.usageHint = usage;
    }

    public void uploadMesh(ChunkMesh mesh, int meshVersion) {
        if (mesh == null || mesh.getVertexCount() == 0 || mesh.getIndexCount() == 0) {
            cleanup();
            return;
        }

        ensureInitialized();

        int vertexBytes = mesh.getVertices().length * Float.BYTES;
        int indexBytes = mesh.getIndices().length * Integer.BYTES;

        long start = System.nanoTime();

        if (persistentEnabled) {
            ensurePersistentCapacity(vertexBytes, indexBytes);
            advanceSegment();

            vertexStreamOffset = currentSegment * vertexSegmentSize;
            indexStreamOffset = currentSegment * indexSegmentSize;

            writeFloats(persistentVertex, vertexStreamOffset, mesh.getVertices());
            writeInts(persistentIndex, indexStreamOffset, mesh.getIndices());

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glFlushMappedBufferRange(GL_ARRAY_BUFFER, vertexStreamOffset, vertexBytes);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glFlushMappedBufferRange(GL_ELEMENT_ARRAY_BUFFER, indexStreamOffset, indexBytes);

            configureAttributes(vertexStreamOffset);
            indexDrawOffset = indexStreamOffset;
        } else {
            uploadDynamic(mesh);
            vertexStreamOffset = 0L;
            indexDrawOffset = 0L;
        }

        this.indexCount = mesh.getIndexCount();
        this.uploadedMeshVersion = meshVersion;

        lastUploadTimeNs = System.nanoTime() - start;
        totalUploadTimeNs += lastUploadTimeNs;
        uploadCount++;
        if (monitor != null) {
            monitor.recordBufferUpload((long) vertexBytes + indexBytes);
        }

        if (persistentEnabled) {
            fences[currentSegment] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        }
    }

    public void updatePartialMesh(float[] vertexData, int vertexOffsetFloats, int vertexLengthFloats,
                                  int[] indexData, int indexOffsetElements, int indexLengthElements) {
        if (!initialized) {
            return;
        }

        long bytesUploaded = 0;

        if (vertexData != null && vertexLengthFloats > 0) {
            long offsetBytes = vertexStreamOffset + (long) vertexOffsetFloats * Float.BYTES;
            long lengthBytes = (long) vertexLengthFloats * Float.BYTES;
            if (persistentEnabled && persistentVertex != null) {
                writeFloats(persistentVertex, offsetBytes, vertexData, vertexLengthFloats);
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glFlushMappedBufferRange(GL_ARRAY_BUFFER, offsetBytes, lengthBytes);
            } else {
                FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexLengthFloats);
                buffer.put(vertexData, 0, vertexLengthFloats).flip();
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferSubData(GL_ARRAY_BUFFER, offsetBytes, buffer);
            }
            bytesUploaded += lengthBytes;
        }

        if (indexData != null && indexLengthElements > 0) {
            long offsetBytes = indexStreamOffset + (long) indexOffsetElements * Integer.BYTES;
            long lengthBytes = (long) indexLengthElements * Integer.BYTES;
            if (persistentEnabled && persistentIndex != null) {
                writeInts(persistentIndex, offsetBytes, indexData, indexLengthElements);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
                glFlushMappedBufferRange(GL_ELEMENT_ARRAY_BUFFER, offsetBytes, lengthBytes);
            } else {
                IntBuffer buffer = BufferUtils.createIntBuffer(indexLengthElements);
                buffer.put(indexData, 0, indexLengthElements).flip();
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
                glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, offsetBytes, buffer);
            }
            bytesUploaded += lengthBytes;
        }

        if (monitor != null && bytesUploaded > 0) {
            monitor.recordBufferUpload(bytesUploaded);
        }
    }

    public void render() {
        if (!initialized || indexCount == 0) {
            return;
        }

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, indexDrawOffset);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (persistentVertex != null) {
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glUnmapBuffer(GL_ARRAY_BUFFER);
            persistentVertex = null;
        }
        if (persistentIndex != null) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
            persistentIndex = null;
        }

        for (int i = 0; i < fences.length; i++) {
            if (fences[i] != 0L) {
                glDeleteSync(fences[i]);
                fences[i] = 0L;
            }
        }

        if (initialized) {
            glBindVertexArray(0);
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            vao = 0;
            vbo = 0;
            ebo = 0;
            indexCount = 0;
            initialized = false;
            uploadedMeshVersion = -1;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public boolean needsUpload(int currentMeshVersion) {
        return uploadedMeshVersion != currentMeshVersion;
    }

    public long getLastUploadTimeNs() {
        return lastUploadTimeNs;
    }

    public long getTotalUploadTimeNs() {
        return totalUploadTimeNs;
    }

    public int getUploadCount() {
        return uploadCount;
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        initialized = true;
        persistentEnabled = shouldUsePersistentBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        configureAttributes(0L);
        glBindVertexArray(0);
    }

    private boolean shouldUsePersistentBuffers() {
        if (!capabilities.supportsBufferStorage()) {
            return false;
        }
        if (capabilities.isIntel() || capabilities.isAMD()) {
            return false;
        }
        return capabilities.isNVIDIA();
    }

    private void ensurePersistentCapacity(int vertexBytes, int indexBytes) {
        long desiredVertexSegment = Math.max(nextPowerOfTwo(vertexBytes), MIN_VERTEX_SEGMENT);
        long desiredIndexSegment = Math.max(nextPowerOfTwo(indexBytes), MIN_INDEX_SEGMENT);

        if (vertexTotalSize == 0 || desiredVertexSegment > vertexSegmentSize) {
            createPersistentVertexBuffer(desiredVertexSegment);
        }

        if (indexTotalSize == 0 || desiredIndexSegment > indexSegmentSize) {
            createPersistentIndexBuffer(desiredIndexSegment);
        }
    }

    private void createPersistentVertexBuffer(long segmentSize) {
        resetStreamingState();

        if (persistentVertex != null) {
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glUnmapBuffer(GL_ARRAY_BUFFER);
            persistentVertex = null;
        }

        glDeleteBuffers(vbo);
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        vertexSegmentSize = segmentSize;
        vertexTotalSize = vertexSegmentSize * STREAM_SEGMENTS;

        glBufferStorage(GL_ARRAY_BUFFER, vertexTotalSize, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        persistentVertex = glMapBufferRange(GL_ARRAY_BUFFER, 0, vertexTotalSize,
            GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        persistentVertex.order(ByteOrder.nativeOrder());

        configureAttributes(0L);
        glBindVertexArray(0);
    }

    private void createPersistentIndexBuffer(long segmentSize) {
        resetStreamingState();

        if (persistentIndex != null) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
            persistentIndex = null;
        }

        glDeleteBuffers(ebo);
        ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        indexSegmentSize = segmentSize;
        indexTotalSize = indexSegmentSize * STREAM_SEGMENTS;

        glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, indexTotalSize, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        persistentIndex = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, indexTotalSize,
            GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        persistentIndex.order(ByteOrder.nativeOrder());

        glBindVertexArray(0);
    }

    private void uploadDynamic(ChunkMesh mesh) {
        glBindVertexArray(vao);

        float[] vertices = mesh.getVertices();
        int[] indices = mesh.getIndices();
        int vertexBytes = vertices.length * Float.BYTES;
        int indexBytes = indices.length * Integer.BYTES;

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBytes, usageHint);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBytes, usageHint);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, indexBuffer);

        configureAttributes(0L);
        glBindVertexArray(0);
    }

    private void configureAttributes(long baseOffset) {
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_STRIDE, baseOffset);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, VERTEX_STRIDE, baseOffset + 3L * Float.BYTES);

        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, VERTEX_STRIDE, baseOffset + 5L * Float.BYTES);

        glBindVertexArray(0);
    }

    private void advanceSegment() {
        currentSegment = (currentSegment + 1) % STREAM_SEGMENTS;
        waitForFence(currentSegment);
    }

    private void waitForFence(int segment) {
        long fence = fences[segment];
        if (fence == 0L) {
            return;
        }

        int waitResult;
        do {
            waitResult = glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000L);
        } while (waitResult == GL_TIMEOUT_EXPIRED);

        if (waitResult != GL_ALREADY_SIGNALED && waitResult != GL_CONDITION_SATISFIED) {
            glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, Long.MAX_VALUE);
        }

        glDeleteSync(fence);
        fences[segment] = 0L;
    }

    private void writeFloats(ByteBuffer target, long offset, float[] values) {
        writeFloats(target, offset, values, values.length);
    }

    private void writeFloats(ByteBuffer target, long offset, float[] values, int length) {
        if (length <= 0) {
            return;
        }

        int originalPosition = target.position();
        int originalLimit = target.limit();

        ByteBuffer slice = target.duplicate().order(target.order());
        int start = (int) offset;
        slice.position(start);
        slice.limit(Math.min(slice.capacity(), start + length * Float.BYTES));
        FloatBuffer floatView = slice.slice().order(target.order()).asFloatBuffer();
        floatView.put(values, 0, length);

        target.position(originalPosition);
        target.limit(originalLimit);
    }

    private void writeInts(ByteBuffer target, long offset, int[] values) {
        writeInts(target, offset, values, values.length);
    }

    private void writeInts(ByteBuffer target, long offset, int[] values, int length) {
        if (length <= 0) {
            return;
        }

        int originalPosition = target.position();
        int originalLimit = target.limit();

        ByteBuffer slice = target.duplicate().order(target.order());
        int start = (int) offset;
        slice.position(start);
        slice.limit(Math.min(slice.capacity(), start + length * Integer.BYTES));
        IntBuffer intView = slice.slice().order(target.order()).asIntBuffer();
        intView.put(values, 0, length);

        target.position(originalPosition);
        target.limit(originalLimit);
    }

    private void resetStreamingState() {
        for (int i = 0; i < fences.length; i++) {
            if (fences[i] != 0L) {
                glDeleteSync(fences[i]);
                fences[i] = 0L;
            }
        }
        currentSegment = -1;
        vertexStreamOffset = 0L;
        indexStreamOffset = 0L;
        indexDrawOffset = 0L;
    }

    private long nextPowerOfTwo(long value) {
        long v = value - 1;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v |= v >> 32;
        return v + 1;
    }
}
