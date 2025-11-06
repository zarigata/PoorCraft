package com.poorcraft.render;

import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.NVXGPUMemoryInfo;

import java.nio.IntBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_EXTENSIONS;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL30C.glGetStringi;
import static org.lwjgl.opengl.GL20.GL_MAX_VERTEX_ATTRIBS;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;
import static org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BLOCK_SIZE;
import static org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS;
import static org.lwjgl.opengl.GL44.GL_MAX_VERTEX_ATTRIB_STRIDE;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Detects and stores GPU capabilities for vendor-specific optimisations.
 *
 * <p>
 * The detection happens once, after the OpenGL context has been created.
 * Capabilities are exposed through a singleton so any subsystem can make
 * decisions based on GPU vendor, driver, and feature support.
 * </p>
 */
public final class GPUCapabilities {

    private static final Map<String, GPUCapabilities> CACHE = new HashMap<>();

    private final String vendor;
    private final String renderer;
    private final String versionString;
    private final int majorVersion;
    private final int minorVersion;
    private final Set<String> extensions;

    private final boolean supportsUniformBufferObjects;
    private final boolean supportsBufferStorage;
    private final boolean supportsMultiDrawIndirect;
    private final boolean supportsComputeShaders;
    private final boolean supportsPersistentMappedBuffers;

    private final int maxTextureSize;
    private final int maxUniformBufferBindings;
    private final int maxUniformBlockSize;
    private final int maxVertexAttribs;
    private final int maxVertexAttribStride;

    private final long totalVRAMKB;
    private final long availableVRAMKB;

    private GPUCapabilities(String vendor,
                            String renderer,
                            String versionString,
                            int majorVersion,
                            int minorVersion,
                            Set<String> extensions,
                            boolean supportsUniformBufferObjects,
                            boolean supportsBufferStorage,
                            boolean supportsMultiDrawIndirect,
                            boolean supportsComputeShaders,
                            boolean supportsPersistentMappedBuffers,
                            int maxTextureSize,
                            int maxUniformBufferBindings,
                            int maxUniformBlockSize,
                            int maxVertexAttribs,
                            int maxVertexAttribStride,
                            long totalVRAMKB,
                            long availableVRAMKB) {
        this.vendor = safeString(vendor);
        this.renderer = safeString(renderer);
        this.versionString = safeString(versionString);
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.extensions = Collections.unmodifiableSet(new HashSet<>(extensions));
        this.supportsUniformBufferObjects = supportsUniformBufferObjects;
        this.supportsBufferStorage = supportsBufferStorage;
        this.supportsMultiDrawIndirect = supportsMultiDrawIndirect;
        this.supportsComputeShaders = supportsComputeShaders;
        this.supportsPersistentMappedBuffers = supportsPersistentMappedBuffers;
        this.maxTextureSize = maxTextureSize;
        this.maxUniformBufferBindings = maxUniformBufferBindings;
        this.maxUniformBlockSize = maxUniformBlockSize;
        this.maxVertexAttribs = maxVertexAttribs;
        this.maxVertexAttribStride = maxVertexAttribStride;
        this.totalVRAMKB = totalVRAMKB;
        this.availableVRAMKB = availableVRAMKB;
    }

    public static GPUCapabilities detect() {
        return CACHE.computeIfAbsent("default", key -> {
            try {
                return detectInternal();
            } catch (Exception ex) {
                System.err.println("[GPU] Detection failed, falling back to conservative defaults: " + ex.getMessage());
                ex.printStackTrace();
                return createFallbackCapabilities();
            }
        });
    }

    public static GPUCapabilities getInstance() {
        GPUCapabilities cached = CACHE.get("default");
        if (cached == null) {
            throw new IllegalStateException("GPUCapabilities.detect() must be called after OpenGL context creation");
        }
        return cached;
    }

    private static GPUCapabilities detectInternal() {
        GL.createCapabilities();

        String vendor = safeString(glGetString(GL_VENDOR));
        String renderer = safeString(glGetString(GL11.GL_RENDERER));
        String versionString = safeString(glGetString(GL_VERSION));

        int[] parsedVersion = parseVersion(versionString);
        int majorVersion = parsedVersion[0];
        int minorVersion = parsedVersion[1];

        Set<String> extensions = queryExtensions(majorVersion, minorVersion);

        boolean supportsUniformBufferObjects = isVersionAtLeast(majorVersion, minorVersion, 3, 1) || extensions.contains("GL_ARB_uniform_buffer_object");
        boolean supportsBufferStorage = isVersionAtLeast(majorVersion, minorVersion, 4, 4) || extensions.contains("GL_ARB_buffer_storage");
        boolean supportsPersistentMappedBuffers = supportsBufferStorage && extensions.contains("GL_ARB_buffer_storage");
        boolean supportsMultiDrawIndirect = isVersionAtLeast(majorVersion, minorVersion, 4, 3) || extensions.contains("GL_ARB_multi_draw_indirect");
        boolean supportsComputeShaders = isVersionAtLeast(majorVersion, minorVersion, 4, 3) || extensions.contains("GL_ARB_compute_shader");

        int maxTextureSize = glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        int maxUniformBufferBindings = supportsUniformBufferObjects ? glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS) : 0;
        int maxUniformBlockSize = supportsUniformBufferObjects ? glGetInteger(GL_MAX_UNIFORM_BLOCK_SIZE) : 0;
        int maxVertexAttribs = glGetInteger(GL_MAX_VERTEX_ATTRIBS);
        int maxVertexAttribStride = isVersionAtLeast(majorVersion, minorVersion, 4, 4) ? glGetInteger(GL_MAX_VERTEX_ATTRIB_STRIDE) : 0;

        long[] vramInfo = queryVram(extensions);

        GPUCapabilities capabilities = new GPUCapabilities(
            vendor,
            renderer,
            versionString,
            majorVersion,
            minorVersion,
            extensions,
            supportsUniformBufferObjects,
            supportsBufferStorage,
            supportsMultiDrawIndirect,
            supportsComputeShaders,
            supportsPersistentMappedBuffers,
            maxTextureSize,
            maxUniformBufferBindings,
            maxUniformBlockSize,
            maxVertexAttribs,
            maxVertexAttribStride,
            vramInfo[0],
            vramInfo[1]
        );
        capabilities.logSummary();
        return capabilities;
    }

    public static GPUCapabilities createFallbackCapabilities() {
        System.err.println("[GPU] Using fallback capabilities due to detection failure");
        return new GPUCapabilities(
            "Unknown",
            "Fallback Renderer",
            "OpenGL 3.3 Fallback",
            3,
            3,
            Collections.emptySet(),
            false,
            false,
            false,
            false,
            false,
            2048,
            0,
            0,
            16,
            0,
            0,
            0
        );
    }

    public boolean isAMD() {
        String lower = vendor.toLowerCase();
        return lower.contains("amd") || lower.contains("ati");
    }

    public boolean isNVIDIA() {
        return vendor.toLowerCase().contains("nvidia");
    }

    public boolean isIntel() {
        return vendor.toLowerCase().contains("intel");
    }

    public boolean isMesa() {
        return renderer.toLowerCase().contains("mesa");
    }

    public boolean supportsUniformBufferObjects() {
        return supportsUniformBufferObjects;
    }

    public boolean supportsBufferStorage() {
        return supportsBufferStorage;
    }

    public boolean supportsPersistentMappedBuffers() {
        return supportsPersistentMappedBuffers;
    }

    public boolean supportsMultiDrawIndirect() {
        return supportsMultiDrawIndirect;
    }

    public boolean supportsComputeShaders() {
        return supportsComputeShaders;
    }

    public String getVendor() {
        return vendor;
    }

    public String getRenderer() {
        return renderer;
    }

    public String getVersionString() {
        return versionString;
    }

    public String getOpenGLVersion() {
        return versionString;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public int getMaxTextureSize() {
        return maxTextureSize;
    }

    public int getMaxUniformBufferBindings() {
        return maxUniformBufferBindings;
    }

    public int getMaxUniformBlockSize() {
        return maxUniformBlockSize;
    }

    public int getMaxVertexAttribs() {
        return maxVertexAttribs;
    }

    public int getMaxVertexAttribStride() {
        return maxVertexAttribStride;
    }

    public long getTotalVRAMKB() {
        return totalVRAMKB;
    }

    public long getAvailableVRAMKB() {
        return availableVRAMKB;
    }

    public boolean hasExtension(String name) {
        return extensions.contains(name);
    }

    public boolean isVersionAtLeast(int testMajor, int testMinor) {
        return majorVersion > testMajor || (majorVersion == testMajor && minorVersion >= testMinor);
    }

    public void logSummary() {
        System.out.println("[GPU] Vendor    : " + vendor);
        System.out.println("[GPU] Renderer  : " + renderer);
        System.out.println("[GPU] Version   : " + versionString);
        System.out.println("[GPU] UBO       : " + supportsUniformBufferObjects);
        System.out.println("[GPU] BufferStorage: " + supportsBufferStorage);
        System.out.println("[GPU] MultiDrawIndirect: " + supportsMultiDrawIndirect);
        System.out.println("[GPU] ComputeShaders  : " + supportsComputeShaders);
        System.out.println("[GPU] MaxTextureSize  : " + maxTextureSize);
        if (supportsUniformBufferObjects) {
            System.out.println("[GPU] MaxUBOBindings  : " + maxUniformBufferBindings);
            System.out.println("[GPU] MaxUBOSize      : " + maxUniformBlockSize);
        }
        if (totalVRAMKB > 0) {
            System.out.println("[GPU] VRAM Total/Avail (KB): " + totalVRAMKB + " / " + availableVRAMKB);
        }
    }

    public static void cleanup() {
        CACHE.clear();
    }

    private static Set<String> queryExtensions(int majorVersion, int minorVersion) {
        Set<String> result = new HashSet<>();
        if (isVersionAtLeast(majorVersion, minorVersion, 3, 0)) {
            int count = glGetInteger(GL_NUM_EXTENSIONS);
            for (int i = 0; i < count; i++) {
                String ext = safeString(glGetStringi(GL_EXTENSIONS, i));
                if (!ext.isEmpty()) {
                    result.add(ext);
                }
            }
        } else {
            String extString = safeString(glGetString(GL_EXTENSIONS));
            if (!extString.isEmpty()) {
                String[] split = extString.split(" ");
                Collections.addAll(result, split);
            }
        }
        return result;
    }

    private static long[] queryVram(Set<String> extensions) {
        long total = 0;
        long available = 0;

        if (extensions.contains("GL_NVX_gpu_memory_info")) {
            try (var stack = stackPush()) {
                IntBuffer buffer = stack.mallocInt(1);
                GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX, buffer);
                total = buffer.get(0);
                buffer.rewind();
                GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, buffer);
                available = buffer.get(0);
            }
        } else if (extensions.contains("GL_ATI_meminfo")) {
            try (var stack = stackPush()) {
                IntBuffer buffer = stack.mallocInt(4);
                GL11.glGetIntegerv(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI, buffer);
                available = buffer.get(0);
                total = Math.max(total, available);
            }
        }

        return new long[]{Math.max(total, 0), Math.max(available, 0)};
    }

    private static boolean isVersionAtLeast(int majorVersion, int minorVersion, int testMajor, int testMinor) {
        return majorVersion > testMajor || (majorVersion == testMajor && minorVersion >= testMinor);
    }

    private static String safeString(String value) {
        return value == null ? "unknown" : value;
    }

    private static int[] parseVersion(String version) {
        if (version == null || version.isEmpty()) {
            return new int[]{0, 0};
        }
        String[] tokens = version.split("[ .]");
        int major = tokens.length > 0 ? parseIntSafe(tokens[0]) : 0;
        int minor = tokens.length > 1 ? parseIntSafe(tokens[1]) : 0;
        return new int[]{major, minor};
    }

    private static int parseIntSafe(String token) {
        try {
            return Integer.parseInt(token.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "GPUCapabilities{" +
            "vendor='" + vendor + '\'' +
            ", renderer='" + renderer + '\'' +
            ", version='" + versionString + '\'' +
            ", supportsUBO=" + supportsUniformBufferObjects +
            ", supportsBufferStorage=" + supportsBufferStorage +
            ", supportsMultiDrawIndirect=" + supportsMultiDrawIndirect +
            '}';
    }
}
