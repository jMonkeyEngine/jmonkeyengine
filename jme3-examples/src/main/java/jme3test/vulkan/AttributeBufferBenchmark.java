package jme3test.vulkan;

import com.jme3.vulkan.formats.Format;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Benchmarks interaction with ByteBuffers through {@link Format.ComponentFormat
 * Component} compatibility, which ensure that whatever primitives are provided
 * are correctly put into the buffer, and vise versa. Components will be critical
 * for ensuring that Meshes don't care what MeshDescription they are using.
 *
 * <p>From the tests, it seems that the extra overhead Components use for puts
 * is minimal. Component put versus direct put pretty much take the same time.</p>
 */
public class AttributeBufferBenchmark {

    public static final Format format = Format.RGBA8_SRGB;
    public static final int verts = 5000;

    private static long startNanos;

    public static void main(String[] args) {

        ByteBuffer buffer = MemoryUtil.memCalloc(format.getTotalBytes() * verts);

        for (int t = 0; t < 10; t++) {
            fillBufferByComponent(buffer);
            fillBufferRaw(buffer);
            System.out.println();
        }

        MemoryUtil.memFree(buffer);

    }

    public static void fillBufferByComponent(ByteBuffer buffer) {
        float value = getTestValue();
        start();
        for (int i = 0; i < verts; i++) {
            int p = i * format.getTotalBytes();
            for (Format.ComponentFormat c : format) {
                // The advantage of Component put is that we can provide
                // any primitive type, and the Component will automatically
                // convert to the correct type.
                c.putFloat(buffer, p, value);
            }
        }
        end("Fill buffer by component");
    }

    public static void fillBufferRaw(ByteBuffer buffer) {
        float value = getTestValue();
        start();
        for (int i = 0; i < verts; i++) {
            int p = i * format.getTotalBytes();
            for (Format.ComponentFormat c : format) {
                buffer.put(p + c.getOffset(), (byte)value);
            }
        }
        end("Fill buffer raw");
    }

    public static void start() {
        startNanos = System.nanoTime();
    }

    public static void end(String task) {
        long end = System.nanoTime();
        System.out.println(task + ": " + ((double)Math.abs(end - startNanos) / 1_000_000) + "ms");
    }

    public static float getTestValue() {
        return (float)Math.random();
    }

}
