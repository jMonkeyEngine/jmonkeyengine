package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

@Deprecated
public class UpdateFlagBuffer extends NioBuffer {

    private boolean updateNeeded = true;

    public UpdateFlagBuffer(MemorySize size) {
        super(size);
    }

    public UpdateFlagBuffer(MemorySize size, int padding) {
        super(size, padding);
    }

    public UpdateFlagBuffer(MemorySize size, int padding, boolean clearMem) {
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
