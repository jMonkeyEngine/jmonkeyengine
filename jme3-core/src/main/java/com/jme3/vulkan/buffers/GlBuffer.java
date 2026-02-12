package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.nio.Buffer;

public class GlBuffer extends NioBuffer {

    private boolean updateNeeded = true;

    public GlBuffer(MemorySize size) {
        super(size);
    }

    public GlBuffer(MemorySize size, int padding) {
        super(size, padding);
    }

    public GlBuffer(MemorySize size, int padding, boolean clearMem) {
        super(size, padding, clearMem);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        updateNeeded = true;
        return super.map(offset, size);
    }

    public void setUpdateNeeded() {
        updateNeeded = true;
    }

    public boolean pollUpdate() {
        boolean u = updateNeeded;
        updateNeeded = false;
        return u;
    }

    public boolean isUpdateNeeded() {
        return updateNeeded;
    }

}
