package com.jme3.vulkan.buffers;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public interface BufferGenerator <T extends MappableBuffer> {

    T createBuffer(MemorySize size, Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage);

    default MappableBuffer createByteBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, byte... values) {
        MappableBuffer buffer = createBuffer(MemorySize.bytes(values.length), bufUsage, dataUsage);
        buffer.mapBytes().put(values);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

    default MappableBuffer createShortBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, short... values) {
        MappableBuffer buffer = createBuffer(MemorySize.shorts(values.length), bufUsage, dataUsage);
        buffer.mapShorts().put(values);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

    default MappableBuffer createIntBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, int... values) {
        MappableBuffer buffer = createBuffer(MemorySize.ints(values.length), bufUsage, dataUsage);
        buffer.mapInts().put(values);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

    default MappableBuffer createFloatBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, float... values) {
        MappableBuffer buffer = createBuffer(MemorySize.floats(values.length), bufUsage, dataUsage);
        buffer.mapFloats().put(values);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

    default MappableBuffer createDoubleBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, double... values) {
        MappableBuffer buffer = createBuffer(MemorySize.doubles(values.length), bufUsage, dataUsage);
        buffer.mapDoubles().put(values);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

    default MappableBuffer createLongBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, long... values) {
        MappableBuffer buffer = createBuffer(MemorySize.longs(values.length), bufUsage, dataUsage);
        buffer.mapLongs().put(values);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

}
