package com.poorcraft.world.chunk;

import com.poorcraft.world.block.BlockType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compressed storage for chunk block data using a palette + RLE strategy.
 * <p>
 * Data flow:
 * <ul>
 *     <li>Random access APIs ({@link #get(int)}, {@link #set(int, BlockType)}) operate on a cached array.</li>
 *     <li>{@link #compress()} converts the cached array into a palette of unique block types and
 *     a run-length encoded stream of palette indices. Runs are stored as parallel arrays of lengths and palette indices.</li>
 *     <li>{@link #decompress()} rebuilds a fresh array from the compressed representation.</li>
 * </ul>
 * <p>
 * This structure keeps memory low for distant chunks while still allowing occasional direct edits.
 */
public class CompressedChunkData {

    private final int length;

    private BlockType[] palette;
    private int[] runLengths;
    private byte[] runValues;
    private byte bitsPerEntry;
    private int compressedSizeBytes;

    private BlockType[] cache;
    private boolean cacheValid;
    private boolean dirty;

    public CompressedChunkData(int length) {
        this.length = length;
        this.cache = new BlockType[length];
        Arrays.fill(this.cache, BlockType.AIR);
        this.cacheValid = true;
        this.dirty = true;
        this.palette = new BlockType[]{BlockType.AIR};
        this.runLengths = new int[]{length};
        this.runValues = new byte[]{0};
        this.bitsPerEntry = 4;
        this.compressedSizeBytes = estimateSize();
    }

    public int length() {
        return length;
    }

    public BlockType get(int index) {
        ensureCache();
        return cache[index];
    }

    public void set(int index, BlockType type) {
        ensureCache();
        BlockType previous = cache[index];
        if (!Objects.equals(previous, type)) {
            cache[index] = type;
            dirty = true;
        }
    }

    public void fill(BlockType type) {
        ensureCache();
        Arrays.fill(cache, type);
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isCacheResident() {
        return cacheValid && cache != null;
    }

    public void compress() {
        ensureCache();
        if (!dirty) {
            return;
        }
        if (length == 0) {
            palette = new BlockType[0];
            runLengths = new int[0];
            runValues = new byte[0];
            bitsPerEntry = 4;
            compressedSizeBytes = 0;
            dirty = false;
            return;
        }

        Map<BlockType, Integer> paletteMap = new LinkedHashMap<>();
        int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            BlockType block = cache[i];
            Integer id = paletteMap.get(block);
            if (id == null) {
                id = paletteMap.size();
                paletteMap.put(block, id);
            }
            indices[i] = id;
        }

        palette = paletteMap.keySet().toArray(new BlockType[0]);
        bitsPerEntry = (byte) (palette.length <= 16 ? 4 : 8);

        List<Integer> lengthBuffer = new ArrayList<>();
        List<Byte> valueBuffer = new ArrayList<>();

        int currentValue = indices[0];
        int currentLength = 1;
        for (int i = 1; i < indices.length; i++) {
            int value = indices[i];
            if (value == currentValue && currentLength < Integer.MAX_VALUE) {
                currentLength++;
            } else {
                flushRun(lengthBuffer, valueBuffer, currentValue, currentLength);
                currentValue = value;
                currentLength = 1;
            }
        }
        flushRun(lengthBuffer, valueBuffer, currentValue, currentLength);

        runLengths = lengthBuffer.stream().mapToInt(Integer::intValue).toArray();
        runValues = new byte[valueBuffer.size()];
        for (int i = 0; i < valueBuffer.size(); i++) {
            runValues[i] = valueBuffer.get(i);
        }

        compressedSizeBytes = estimateSize();
        dirty = false;
    }

    public BlockType[] decompress() {
        ensureCache();
        return cache.clone();
    }

    public BlockType[] getCacheView() {
        ensureCache();
        return cache;
    }

    public void discardCache() {
        if (!dirty) {
            cache = null;
            cacheValid = false;
        }
    }

    public int getCompressedSizeBytes() {
        return compressedSizeBytes;
    }

    public byte getBitsPerEntry() {
        return bitsPerEntry;
    }

    public BlockType[] getPaletteSnapshot() {
        return palette.clone();
    }

    private void flushRun(List<Integer> lengthBuffer, List<Byte> valueBuffer, int value, int runLength) {
        int remaining = runLength;
        while (remaining > 0) {
            int chunk = Math.min(remaining, 1 << 16); // cap at 65536 per run
            lengthBuffer.add(chunk);
            valueBuffer.add((byte) value);
            remaining -= chunk;
        }
    }

    public void ensureCache() {
        if (cacheValid && cache != null) {
            return;
        }
        cache = new BlockType[length];
        if (runLengths == null || runValues == null || palette == null || runLengths.length == 0) {
            Arrays.fill(cache, BlockType.AIR);
            cacheValid = true;
            return;
        }
        int cursor = 0;
        for (int i = 0; i < runLengths.length && cursor < length; i++) {
            int runLength = runLengths[i];
            BlockType block = palette[Byte.toUnsignedInt(runValues[i])];
            for (int j = 0; j < runLength && cursor < length; j++) {
                cache[cursor++] = block;
            }
        }
        while (cursor < length) {
            cache[cursor++] = BlockType.AIR;
        }
        cacheValid = true;
    }

    private int estimateSize() {
        int paletteBytes = palette.length * Integer.BYTES;
        int runBytes = runValues.length + runLengths.length * Integer.BYTES;
        return paletteBytes + runBytes;
    }
}
