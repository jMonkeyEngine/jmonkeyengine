package com.jme3.vulkan.buffers.mapping;

import com.jme3.vulkan.buffernew.GpuBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;

public class SourceBufferMapping implements BufferMapping {

    private final GpuBuffer source;
    private final ByteBuffer bytes;
    private final Runnable unmap;

    public SourceBufferMapping(GpuBuffer source, ByteBuffer bytes, Runnable unmap) {
        this.source = source;
        this.bytes = bytes;
        this.unmap = unmap;
    }

    @Override
    public void close() {
        unmap.run();
    }

    @Override
    public void stage(long offset, long size) {
        source.stage(offset, size);
    }

    @Override
    public ByteBuffer getBytes() {
        return bytes;
    }

}
